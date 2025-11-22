package com.bertram.rabbitmq.util

import com.bertram.rabbitmq.conf.Queue
import com.bertram.rabbitmq.conf.Subscriber
import groovy.util.logging.Commons

/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons
class ServiceInspector {
    public static final long DEFAULT_SHUTDOWN_TIMEOUT = 5000l
    /**
     * Returns a map of queue/topic listeners
     * @param service
     * @param grailsApplication
     */
    def getListenerConfigs(configs, service, grailsApplication) {
        if ( !configs.queues )
            configs.queues = []
        if ( !configs.exchanges )
            configs.exchanges = []

        // get all non synthetic methods annotated with either Queue or Subscriber
        service.getClazz().methods.findAll { !it.synthetic }.each { method ->
            def subscriberAnnotation = method.getAnnotation(Subscriber)
            def queueAnnotation = method.getAnnotation(Queue)

            if (subscriberAnnotation) {
                configs.exchanges << getSubscriberConfig(service, method, subscriberAnnotation, grailsApplication)
            }
            else if (queueAnnotation) {
                log.debug( "Found config for ${service}/${method.name}" )
                def queueConfig = getQueueConfig(service, method, queueAnnotation, grailsApplication)
                // Make sure we don't have multiple implementations consuming same queue
                if ( !configs.queues.find { item -> item.queueName == queueConfig.queueName } ) {
                    configs.queues << queueConfig
                }
            }
        }

        return configs
    }

    /**
     * Build a config map that will be used to build a a listener for a specific exchange
     * @param service
     * @param method
     * @param annotation
     * @param grailsApplication
     */
    def getSubscriberConfig( service, method, annotation, grailsApplication ) {
        def config = [:].with {
            serviceClass = service.getClazz()
            serviceName = service.logicalPropertyName
            listenerMethodName = method.name

	        // these annotation properties have been deprecated
            autoDelete = annotation.autoDelete()
            durable = annotation.durable()
	        exchangeName = annotation.exchangeName()
	        exchangeType = annotation.exchangeType()
	        /* --------------------------- */

            consumers = annotation.consumers()
	        prefetchCount = annotation.prefetchCount()
            connection = annotation.conAlias()
            routingKey = annotation.routingKey()
            multiSubscriber = annotation.multiSubscriber()
            startOnLoad = annotation.autoStartup()
            shutdownTimeout = grailsApplication.config.rabbitmq?.shutdownTimeout ?: DEFAULT_SHUTDOWN_TIMEOUT
			def ep = annotation.exchangeParams()
			exchange = [name:ep.name(), durable:ep.durable(), autoDelete:ep.autoDelete(), type:ep.exchangeType()]
			def qp = annotation.queueParams()
			queue = [name:qp.name(), durable:qp.durable(), autoDelete:qp.autoDelete(), exclusive:qp.exclusive(), autoNodeName: qp.autoNodeName()]
            it
        }

        log.debug( "Setting up subscriber config: ${config}" )
        return config
    }

    /**
     * Build a config map that will be used to builder a listener for a specific queue
     * @param service
     * @param method
     * @param annotation
     * @param grailsApplication
     */
    def getQueueConfig( service, method, annotation, grailsApplication ) {
        def config = [:].with {
            serviceClass = service.getClazz()
            serviceName = service.logicalPropertyName
            listenerMethodName = method.name
            queueName = annotation.name()
            autoDelete = annotation.autoDelete()
            durable = annotation.durable()
            exclusive = annotation.exclusive()
            consumers = annotation.consumers()
	        prefetchCount = annotation.prefetchCount()
            connection = annotation.conAlias()
            startOnLoad = annotation.autoStartup()
            shutdownTimeout = grailsApplication.config.rabbitmq?.shutdownTimeout ?: DEFAULT_SHUTDOWN_TIMEOUT
            // TODO: Add amq paramaters
            it
        }

        log.debug( "Setting up queue config: ${config}" )
        return config
    }
}
