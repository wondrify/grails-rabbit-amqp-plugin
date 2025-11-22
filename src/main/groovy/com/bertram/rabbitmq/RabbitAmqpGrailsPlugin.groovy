package com.bertram.rabbitmq

import grails.plugins.*
import com.bertram.rabbitmq.util.ListenerConfigurer
import com.bertram.rabbitmq.util.ServiceInspector
import org.aopalliance.aop.Advice
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptorFactoryBean
import org.springframework.amqp.rabbit.core.RabbitTemplate
//import org.springframework.amqp.rabbit.retry.MissingMessageIdAdvice
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.MapRetryContextCache
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import groovy.util.logging.Slf4j
import org.springframework.core.annotation.AnnotationUtils;


@Slf4j
class RabbitAmqpGrailsPlugin extends Plugin {
    private Map<String, Boolean>            wiredBeans      = new HashMap<String, Boolean>();

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.5 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    

    def title = "Rabbit Amqp Plugin" // Headline display name of the plugin
    def author = "Jordon Saardchit"
    def authorEmail = "jsaardchit@bcap.com"
    def description = '''A replacement for the grails-rabbitmq plugin that allows you to connect to multiple hosts, and use an annotation scheme to configure consumers as methods as opposed to an entire service '''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rabbit-amqp"

    def license = "APACHE"
    def loadAfter = ['services']
    def loadBefore = ['quartz']
    // Details of company behind the plugin (if there is one)
    def organization = "Bertram Labs"



    Closure doWithSpring() { {->
        def application = grailsApplication
        def rabbitmqConfig = grailsApplication.config.rabbitmq

        if (!rabbitmqConfig) {
            log.warn("No rabbit configuration specified, skipping configuration")
            return
        }

        // Setup all connection factories, templates, etc before anything else
        def conFactBuilder = new RabbitConnectionFactoryConfigurer(rabbitmqConfig)
        conFactBuilder.configure(delegate)

        rabbitErrorHandler(RabbitErrorHandler)

        // A container for various listener configurations
        def startTime = new Date().time
        def listenerConfigs = [:]
        def serviceInspector = new ServiceInspector()
        for(service in application.serviceClasses) {
            if (null == AnnotationUtils.findAnnotation(service.clazz, com.bertram.rabbitmq.conf.RabbitConsumer.class)) {
                continue;
            }
            serviceInspector.getListenerConfigs( listenerConfigs, service, application )
        }

        
        
        println "Scanned for Rabbit Listener Configs in ${new Date().time - startTime}ms"

        // Setup listeners
        def listenerConfigurer = new ListenerConfigurer( grailsApplication:application )
        startTime = new Date().time
        // def results = withPool( 4 ) {
            
            
        // }
        listenerConfigs.queues?.each {
                listenerConfigurer.registerQueueListener( delegate, it )
            }
        listenerConfigs.exchanges?.each {
                listenerConfigurer.registerExchangeListener( delegate, it )
        }
        println "Created Rabbit Beans in ${new Date().time - startTime}ms"

        rabbitRetryHandler(StatefulRetryOperationsInterceptorFactoryBean) {
            def retryPolicy = new SimpleRetryPolicy()
            def maxRetryAttempts = 3
            if(rabbitmqConfig?.retryPolicy?.containsKey('maxAttempts')) {
                def maxAttemptsConfigValue = rabbitmqConfig.retryPolicy.maxAttempts
                if(maxAttemptsConfigValue instanceof Integer) {
                    maxRetryAttempts = maxAttemptsConfigValue
                } else {
                    log.error "rabbitmq.retryPolicy.maxAttempts [$maxAttemptsConfigValue] of type [${maxAttemptsConfigValue.getClass().getName()}] is not an Integer and will be ignored.  The default value of [${maxRetryAttempts}] will be used"
                }
            }
            retryPolicy.maxAttempts = maxRetryAttempts

            def backOffPolicy = new FixedBackOffPolicy()
            backOffPolicy.backOffPeriod = rabbitmqConfig.retryPolicy?.backOffPeriod ?: 5000

            def retryTemplate = new RetryTemplate()
            retryTemplate.retryPolicy  = retryPolicy
            retryTemplate.backOffPolicy = backOffPolicy

            retryOperations = retryTemplate
        }
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        def containerBeans = applicationContext.getBeansOfType(AutoStartSimpleMessageListenerContainer)
        def templateBeans = applicationContext.getBeansOfType(RabbitTemplate)

        templateBeans.each { name, bean ->
            if(bean.messageConverter instanceof org.springframework.amqp.support.converter.AbstractMessageConverter) {
                bean.messageConverter.createMessageIds = true
            }
        }
        containerBeans.each { beanName, bean ->
            initialiseAdviceChain bean, applicationContext

            //Listeners are started in bootstrap, after all other plugins have initialized.
        }
    }

    void onStartup(Map<String, Object> event) {
        log.info("Starting Message Queue Listeners (RabbitMq)...")
        def containerBeans = grailsApplication.mainContext.getBeansOfType(AutoStartSimpleMessageListenerContainer)
        containerBeans?.each { beanName, bean ->
            if(bean.startOnLoad) {
                bean.start()
            }
        }
    }

    void onShutdown(Map<String, Object> event) {
        log.info("Shutting Down Message Queues (RabbitMq)...")
        def containerBeans = grailsApplication.mainContext.getBeansOfType(AutoStartSimpleMessageListenerContainer)
        containerBeans?.each { beanName, bean ->
            bean.stop();
        }
    }   

    protected initialiseAdviceChain(listenerBean, applicationContext) {
        def retryTemplate = applicationContext.rabbitRetryHandler.retryOperations
        def cache = new MapRetryContextCache()
        retryTemplate.retryContextCache = cache

//        def missingIdAdvice = new MissingMessageIdAdvice(cache)
        listenerBean.adviceChain = [applicationContext.rabbitRetryHandler] as Advice[]
    }
}
