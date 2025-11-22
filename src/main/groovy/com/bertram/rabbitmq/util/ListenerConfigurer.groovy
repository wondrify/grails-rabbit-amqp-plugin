package com.bertram.rabbitmq.util

import com.bertram.rabbitmq.AutoQueueMessageListenerContainer
import com.bertram.rabbitmq.BasicQueueMessageListenerContainer
import grails.spring.BeanBuilder
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.AnonymousQueue
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.SimpleMessageConverter
import groovy.util.logging.Commons
/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons
class ListenerConfigurer {
    def grailsApplication

    /**
     * This method will wire together spring beans to use for a rabbit queue listener
     * @param beanBuilder
     * @param config
     * @return
     */
    def registerQueueListener( beanBuilder, config ) {
        log.debug( "Registering Queue Listener for: ${config}")
	    def adapterName = config.listenerMethodName ? "${config.serviceName}.${config.listenerMethodName}" : null

        // Build queue bean
        beanBuilder."${SpringBeanUtils.getQueueBeanName(config.queueName)}"( org.springframework.amqp.core.Queue,
                config.queueName,
                config.durable ?: true,
                config.exclusive ?: false,
                config.autoDelete ?: false,
                null // TODO: Add queue arguments
        )

        // build our service delegate if we've been configured from an annotation
	    if (adapterName) {
			beanBuilder."${SpringBeanUtils.getServiceDelegateBeanName(config.queueName)}"( MessageServiceDelegate ) {
				grailsApplication = grailsApplication
				serviceName = config.serviceName
				listenerMethod = config.listenerMethodName
				persistenceInterceptor = ref('persistenceInterceptor')
			}

			// build adapter bean
			beanBuilder."${SpringBeanUtils.getAdapterBeanName(adapterName)}"( MessageListenerAdapter ) { beanDefinition ->
				beanDefinition.constructorArgs = [ref(SpringBeanUtils.getServiceDelegateBeanName(config.queueName)), converter()]
			}
	    }

        // configure the container bean
        beanBuilder."${SpringBeanUtils.getContainerBeanName(SpringBeanUtils.getQueueBeanName(config.queueName))}"( BasicQueueMessageListenerContainer ) {
            autoStartup = false
            startOnLoad = config.startOnLoad ?: false
            errorHandler = ref('rabbitErrorHandler')
            acknowledgeMode = AcknowledgeMode.AUTO
            concurrentConsumers = config.consumers ?: 1
            channelTransacted = false
            prefetchCount = config.prefetchCount ?: BasicQueueMessageListenerContainer.DEFAULT_PREFETCH_COUNT
            shutdownTimeout = config.shutdownTimeout ?: 1000l
            queue = ref(SpringBeanUtils.getQueueBeanName(config.queueName))
            if (config.connection) {
                connectionFactory = ref(SpringBeanUtils.getConFactoryBeanName(config.connection))
                rabbitAdminBeanName = SpringBeanUtils.getAdminBeanName(config.connection)
            }
            else {
                connectionFactory = ref("rabbitMQConnectionFactory")
            }
	        if (adapterName)
				messageListener = ref(SpringBeanUtils.getAdapterBeanName(adapterName))
        }
    }

    /**
     * Register the listener that is bound to the current topic exchange
     * @param beanBuilder
     * @param config
     * @return
     */
    def registerExchangeListener( beanBuilder, config ) {
        log.debug( "Registering Exchange listener for : ${config}" )
        def nameSuffix = "${config.serviceClass.name}.${config.listenerMethodName}"
        def adapterName = config.listenerMethodName ? "${config.serviceName}.${config.listenerMethodName}" : null
        def beanQueueName = "${getQueueListener(config.exchange?.name ?: config.exchangeName)}" // NOTE: this is used for the spring bean name
        def serviceDelegateName = "${SpringBeanUtils.getServiceDelegateBeanName(beanQueueName)}"
        def containerName = "${SpringBeanUtils.getContainerBeanName(SpringBeanUtils.getExchangeBeanName(config.exchange?.name ?: config.exchangeName))}"

        if(config.multiSubscriber) {
            beanQueueName = "${beanQueueName}_${nameSuffix}"
            serviceDelegateName = "${serviceDelegateName}_${nameSuffix}"
            containerName = "${containerName}_${nameSuffix}"
        }

        log.debug "queueName : $beanQueueName"
        log.debug "serviceDelegateName: $serviceDelegateName"
        log.debug "containerName : $containerName"
        String exchangeName = config.exchange.name ?: config.exchangeName
        // build exchange bean
        beanBuilder."${SpringBeanUtils.getExchangeBeanName(config.exchange?.name ?: config.exchangeName)}"(
			config.exchange.type?.type() ?: config.exchangeType.type(), // backwards compatability for deprecation
			exchangeName,
			config.exchange.durable,
			config.exchange.autoDelete
        )


	    // build queue bean
	    if (config.queue.name) {
		    beanBuilder."${SpringBeanUtils.getQueueBeanName(beanQueueName)}"(
			    Queue,
			    config.queue.name,
			    config.queue.durable,
			    config.queue.exclusive,
			    config.queue.autoDelete
		    )
	    } else if(config.queue.autoNodeName) {
            def hostname = InetAddress.getLocalHost().getHostName()
            if(hostname == 'localhost') {
                log.error("Warning!! Local Host name determined as localhost. This can cause problems in HA environments on a fanout exchange!")
            }
            String queueName = "${exchangeName}-${hostname}".toString()
            if(!config.queue.autoDelete) {
                beanBuilder."${SpringBeanUtils.getQueueBeanName(beanQueueName)}"(
                    Queue,
                    queueName,
                    config.queue.durable,
                    config.queue.exclusive,
                    config.queue.autoDelete,
                    ['x-expires':1800000]
                )
            } else {
                beanBuilder."${SpringBeanUtils.getQueueBeanName(beanQueueName)}"(
                    Queue,
                    queueName,
                    config.queue.durable,
                    config.queue.exclusive,
                    config.queue.autoDelete
                )
            }
            
        } else {
		    beanBuilder."${SpringBeanUtils.getQueueBeanName(beanQueueName)}"(
			    AnonymousQueue,
			    ['x-expires': 1800000]
		    )
	    }

        // build adapter bean if config contains a service/method to attach to
        if (adapterName) {
            // build our service delegate
            beanBuilder."${serviceDelegateName}"( MessageServiceDelegate ) {
                grailsApplication = grailsApplication
                serviceName = config.serviceName
                listenerMethod = config.listenerMethodName
                persistenceInterceptor = ref('persistenceInterceptor')
            }

            beanBuilder."${SpringBeanUtils.getAdapterBeanName(adapterName)}"( MessageListenerAdapter ) { beanDefinition ->
                beanDefinition.constructorArgs = [ref(serviceDelegateName), converter()]
            }
        }

        // build our container
        beanBuilder."${containerName}"(AutoQueueMessageListenerContainer) {
            autoStartup = false
            startOnLoad = config.startOnLoad
            acknowledgeMode = AcknowledgeMode.AUTO
            errorHandler = ref("rabbitErrorHandler")
            channelTransacted = false
            concurrentConsumers = config.consumers
	        prefetchCount = config.prefetchCount ?: AutoQueueMessageListenerContainer.DEFAULT_PREFETCH_COUNT
            shutdownTimeout = config.shutdownTimeout
            exchangeBeanName = SpringBeanUtils.getExchangeBeanName(config.exchange?.name ?: config.exchangeName)
            routingKey = config.routingKey

	        // queue bean
	        queue = ref("${SpringBeanUtils.getQueueBeanName(beanQueueName)}")

            if (config.connection) {
                connectionFactory = ref(SpringBeanUtils.getConFactoryBeanName(config.connection))
                rabbitAdminBeanName = SpringBeanUtils.getAdminBeanName(config.connection)
            }
            else {
                connectionFactory = ref("rabbitMQConnectionFactory")
            }
	        if (adapterName)
				messageListener = ref(SpringBeanUtils.getAdapterBeanName(adapterName))

        }

    }

    /**
     * This method is a runtime utility method for creating new topic listeners (see traffic plugin for use)
     * @param config
     * @param handler
     * @return
     */
    def createTopicListener(Map config, Closure handler) {
        def bb = new BeanBuilder(grailsApplication.mainContext)
        bb."${SpringBeanUtils.getClosureDelegateBeanName(config.exchangeName)}"( MessageClosureDelegate, handler ) {
            applicationContext = grailsApplication.mainContext
            persistenceInterceptor = ref('persistenceInterceptor')
        }

        registerExchangeListener(bb, config)
        // Our new application context with wired queue and binding for this listener
        def newCtx = bb.createApplicationContext()

        // Our message handler
        def msgHandler = newCtx.getBean("${SpringBeanUtils.getClosureDelegateBeanName(config.queueName)}")
	    def adapter = new MessageListenerAdapter(msgHandler, converter())

        def container = newCtx.getBean("${SpringBeanUtils.getContainerBeanName(SpringBeanUtils.getExchangeBeanName(config.exchangeName))}")
        container.messageListener = adapter
        container.start()
    }

	/**
	 * This method allows you to create adhoc queue listeners at runtime
	 * @param config
	 * @param handler
	 */
	def createQueueListener(Map config, Closure handler) {
		def builder = new BeanBuilder(grailsApplication.mainContext)
		builder."${SpringBeanUtils.getClosureDelegateBeanName(config.queueName)}"(MessageClosureDelegate, handler) {
			applicationContext = grailsApplication.mainContext
			persistenceInterceptor = ref('persistenceInterceptor')
		}

		registerQueueListener(builder, config)

		// create our new context with this bean definition
		def newCtx = builder.createApplicationContext()

		// grab out message handler and add it to our container
		def msgHandler = newCtx.getBean("${SpringBeanUtils.getClosureDelegateBeanName(config.queueName)}")
		def adapter = new MessageListenerAdapter(msgHandler, converter())

		def container = newCtx.getBean("${SpringBeanUtils.getContainerBeanName(SpringBeanUtils.getQueueBeanName(config.queueName))}")
		container.messageListener = adapter
		if (container.startOnLoad)
			container.start()
	}


    private SimpleMessageConverter converter() {
        SimpleMessageConverter converter = new SimpleMessageConverter()
        converter.setAllowedListPatterns(List.of("java.util.*","java.lang.*","java.sql.*","org.codehaus.groovy.runtime.GStringImpl","groovy.lang.GString","java.time.*","org.springframework.session.MapSession","java.math.*","org.springframework.security.*","org.grails.web.servlet.*","org.springframework.security.core.context.*","com.morpheus.*","com.morpheusdata.model.*","grails.plugin.springsecurity.*"))
        return converter
    }

    private getQueueListener( String id ) {
        return "${InetAddress.getLocalHost().hostName}.${id}"
    }
}
