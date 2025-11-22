package com.bertram.rabbitmq

import com.bertram.rabbitmq.util.ConnectionFactoryHelper
import com.bertram.rabbitmq.util.SpringBeanUtils
import groovy.util.logging.Commons
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.SimpleMessageConverter

/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons 
class RabbitConnectionFactoryConfigurer {

    // Constants used for spring bean construction
    public static final CONNECTION_FACTORY = 'factory'

    // Configure available connection factory properties
    private static final CONNECTION_FACTORY_PROPS = [
            ''
    ]

    def factoryConfig
    def beanBuilder
    def connectionFactories = []

    /**
     * Constructor to use for defined configuration
     * @param factoryConfig
     */
    RabbitConnectionFactoryConfigurer(factoryConfig) {
        this.factoryConfig = factoryConfig
    }

    /**
     * Configure the spring bean wiring for connection factories in config
     * @param beanBuilder
     */
    def configure(beanBuilder) {
        this.beanBuilder = beanBuilder

        def localConfig = factoryConfig.clone()

        if (localConfig.connectionFactories) {
            log.debug( "found connection Factories")
            def factoriesConfig = localConfig.connectionFactories
            if(factoriesConfig instanceof Closure) {
                factoriesConfig.delegate = this
                factoriesConfig.resolveStrategy = Closure.DELEGATE_FIRST    
                factoriesConfig()
            } else if(factoriesConfig instanceof Collection) {
                connectionFactories += factoriesConfig
            }
            

            // walk through the config and extract connection factory params
            

            // wire together the specified
            initConnectionFactoryBeans()
        }
    }

    /**
     * This method will spring wire components together for connection factories defined in connectionFactories block
     * of DSL
     * @param config
     */
    def initConnectionFactoryBeans() {
        this.connectionFactories.eachWithIndex { item, index ->
            def connectionFactoryUsername = item.username
            def connectionFactoryPassword = item.password
            def connectionFactoryVirtualHost = item.virtualHost
            def connectionFactoryHostname = item.hostname
            def connectionFactoryPort = item.port ?: 5672
            def connectionFactoryAlias = item.name
            def connectionChannelCacheSize = item.channelCacheSize ?: 10
	        def connectionHeartbeatInterval = item.heartBeatDelay ?: 580
            def connectionAddresses = item.addresses
            def connectionSslProtocol = item.sslProtocol ?: 'TLSv1.2'

            if(!connectionFactoryUsername || !connectionFactoryPassword || (!connectionFactoryHostname && !connectionAddresses)) {
                log.error 'RabbitMQ connection factory settings (factory.username, factory.password and factory.hostname) must be defined in Config.groovy'
            }
            else {
                log.debug "Connecting to rabbitmq ${connectionFactoryUsername}@${connectionFactoryHostname}"
                if(item.useSsl) {
                    log.debug "Setting ssl protocol to ${connectionSslProtocol}"
                }

	            // create connection factory
                beanBuilder."${SpringBeanUtils.getRmqConFactoryBeanName(connectionFactoryAlias)}"(ConnectionFactoryHelper) {
                    if (item.useSsl) {
                        sslProtocol = connectionSslProtocol
                    }
                }

                // create connection factory bean
                beanBuilder."${SpringBeanUtils.getConFactoryBeanName(connectionFactoryAlias)}"(CachingConnectionFactory) { beanDefinition ->
                    beanDefinition.constructorArgs = [ref(SpringBeanUtils.getRmqConFactoryBeanName(connectionFactoryAlias))]
                    username = connectionFactoryUsername
                    password = connectionFactoryPassword
	                host = connectionFactoryHostname
                    port = connectionFactoryPort
                    channelCacheSize = connectionChannelCacheSize
	                requestedHeartBeat = connectionHeartbeatInterval
                    if(connectionAddresses) {
                        addresses = connectionAddresses
                    }

                    if (connectionFactoryVirtualHost) {
                        virtualHost = connectionFactoryVirtualHost
                    }
                }

                if(index == 0) {
                   beanBuilder.springConfig.addAlias "rabbitConnectionFactory", "${SpringBeanUtils.getConFactoryBeanName(connectionFactoryAlias)}"
                }

                // create template to use for this connection
                // TODO Add ability to configure message converter inside our builder DSL
                beanBuilder."${SpringBeanUtils.getTemplateBeanName(connectionFactoryAlias)}"(RabbitTemplate) {
                    connectionFactory = ref("${SpringBeanUtils.getConFactoryBeanName(connectionFactoryAlias)}")
                    def converter = new SimpleMessageConverter()
                    converter.createMessageIds = true
                    converter.setAllowedListPatterns(List.of("java.util.*","java.lang.*","java.sql.*","org.codehaus.groovy.runtime.GStringImpl","groovy.lang.GString","java.time.*","org.springframework.session.MapSession","java.math.*","org.springframework.security.*","org.grails.web.servlet.*","org.springframework.security.core.context.*","com.morpheus.*","com.morpheusdata.model.*","grails.plugin.springsecurity.*"))

                    messageConverter = converter
                }

                // create rabbit admin instance for our connection factory
                beanBuilder."${SpringBeanUtils.getAdminBeanName(connectionFactoryAlias)}"(RabbitAdmin) { beanDefinition ->
                    beanDefinition.constructorArgs = [ref(SpringBeanUtils.getConFactoryBeanName(connectionFactoryAlias))]
                    autoStartup = false
                }

                // create error handler
                // TODO Add ability to configure message converter inside our builder DSL
                beanBuilder."${SpringBeanUtils.getErrorHandlerBeanName(connectionFactoryAlias)}"(RabbitErrorHandler)
            }
        }
    }

    /**
     * Handler for the connection factory DSL
     * @param methodName - Act only on factory method calls
     * @param args - Connection factory properties
     */
    def methodMissing(String methodName, args) {
        def argsMap = args ? args[0] : [:]
        if (methodName == CONNECTION_FACTORY) {
            connectionFactories << argsMap
        }
    }
}
