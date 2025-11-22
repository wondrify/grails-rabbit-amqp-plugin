package com.bertram.rabbitmq

import com.bertram.rabbitmq.util.ListenerConfigurer
import com.bertram.rabbitmq.util.SpringBeanUtils
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.core.ChannelCallback
import org.springframework.amqp.rabbit.core.RabbitTemplate

/**
 * This is the class the dynamically added service methods will delegate to for rabbit message sending
 * @author jsaardchit
 */
class RabbitMQService {
    static transactional = false
    def grailsApplication

    /**
     * Base method for sending an amqp message
     * @param message
     * @return
     */
    def convertAndSend(Object message) {
        convertAndSend(null, message)
    }

    /**
     * Overloaded method for message send
     * @param connectionAlias
     * @param message
     * @return
     */
    def convertAndSend(String connectionAlias, Object message ) {
        getTemplate(connectionAlias).convertAndSend(message)
    }

    /**
     * Send message with a routing key
     * @param connectionAlias
     * @param routingKey
     * @param message
     * @return
     */
    def convertAndSend(String connectionAlias, String routingKey, Object message ) {
        getTemplate(connectionAlias).convertAndSend(routingKey, message)
    }

    /**
     * Send message to exchange
     * @param connectionAlias
     * @param exchange
     * @param routingKey
     * @param message
     * @return
     */
    def convertAndSend(String connectionAlias, String exchange, String routingKey, Object message ) {
        getTemplate(connectionAlias).convertAndSend(exchange, routingKey, message)
    }

    /**
     * Send message to exchange with post processor
     * @param connectionAlias
     * @param exchange
     * @param routingKey
     * @param message
     * @param processor
     * @return
     */
    def convertAndSend(String connectionAlias, String exchange, String routingKey, Object message, MessagePostProcessor processor ) {
        getTemplate(connectionAlias).convertAndSend(exchange, routingKey, message, processor)
    }

    /**
     * This method will stop the rabbit queue listening container that is mapped to the spring context of its app
     * @param container
     * @return
     */
    def stopRabbitQueueContainer(container) {
        def containerBean = grailsApplication.mainContext.getBean(
                SpringBeanUtils.getContainerBeanName(
                        SpringBeanUtils.getQueueBeanName(container.toString())
                )
        )
        if (containerBean && containerBean.running) {
            containerBean.stop()
        }
        else {
            throw new RuntimeException("There is no container bean for: ${container}")
        }
    }

    /**
     * This method will stop the exchange subscriber container that is mapped to the spring context of its app
     * @param container
     * @return
     */
    def stopRabbitSubscriberContainer(container) {
        def containerBean = grailsApplication.mainContext.getBean(
                SpringBeanUtils.getContainerBeanName(
                        SpringBeanUtils.getExchangeBeanName(container.toString())
                )
        )
        if (containerBean && containerBean.running) {
            containerBean.stop()
        }
        else {
            throw new RuntimeException("There is no container bean for: ${container}")
        }
    }

    /**
     * Convenience method to stop all rabbitmq containers in this spring app context
     * @return
     */
    def stopAllRabbitContainers() {
        grailsApplication.mainContext.getBeansOfType(AutoStartSimpleMessageListenerContainer).each { beanName, bean ->
            bean.stop()
        }
    }

    /**
     * This method will start a rabbit queue listening container that is mapped to the spring context of its app
     * @param container
     * @return
     */
    def startRabbitQueueContainer(container) {
        def containerBean = grailsApplication.mainContext.getBean(
                SpringBeanUtils.getContainerBeanName(
                        SpringBeanUtils.getQueueBeanName(container.toString())
                )
        )
        if (containerBean) {
            if (!containerBean.running)
                containerBean.start()
        }
        else {
            throw new RuntimeException("There is no container bean for: ${container}")
        }
    }

    /**
     * This method will start a rabbit topic subscriber container that is mapped to the spring context of its app
     * @param container
     * @return
     */
    def startRabbitSubscriberContainer(container) {
        def containerBean = grailsApplication.mainContext.getBean(
                SpringBeanUtils.getContainerBeanName(
                        SpringBeanUtils.getExchangeBeanName(container.toString())
                )
        )
        if (containerBean) {
            if (!containerBean.running)
                containerBean.start()
        }
        else {
            throw new RuntimeException("There is no container bean for: ${container}")
        }
    }

    /**
     * Convenience method to start all rmq containers in this spring application context
     * @return
     */
    def startAllRabbitContainers() {
        grailsApplication.mainContext.getBeansOfType(AutoStartSimpleMessageListenerContainer).each { beanName, bean ->
            bean.start()
        }
    }

    /**
     * Convenience method for getting all container beans of this spring context
     * @return
     */
    def getRabbitListenerContainers() {
        return grailsApplication.mainContext.getBeansOfType(AutoStartSimpleMessageListenerContainer)
    }

    /**
     * Convenience message for getting message count on a queue
     * @param conAlias
     * @param queueName
     * @return
     */
    def getQueueMessageCount(String conAlias, String queueName) {
        AMQP.Queue.DeclareOk dok = getTemplate(conAlias).execute(new ChannelCallback<AMQP.Queue.DeclareOk>() {
            public AMQP.Queue.DeclareOk doInRabbit(Channel channel) throws Exception {
                return channel.queueDeclarePassive(queueName);
            }
        })

        return dok.messageCount
    }

    /**
     * A shorthand method for creating a new topic subscriber
     * @param config
     * @param adapter
     * @return
     */
    def createSubscriber(Map config, Closure adapter) {
        new ListenerConfigurer(grailsApplication:grailsApplication).createTopicListener(config, adapter)
    }

	/**
	 * Method for creating adhoc queue consumers
	 * @param config [queueName:'myqueue', connection:'main', consumers:1, shutdownTimeout:1000l, startOnLoad:true, durable:true, autoDelete:false]
	 * @param adapter
	 * @return
	 */
	def createQueueConsumer(Map config, Closure adapter) {
		new ListenerConfigurer(grailsApplication:grailsApplication).createQueueListener(config, adapter)
	}

    /**
     * Method used to grab a rabbit template used to send a message on
     * @param alias
     * @return
     */
    private getTemplate(alias) {
        def template

        // If we have an alias get the correct template bean
        if (alias) {
            template = grailsApplication.mainContext.getBean("${SpringBeanUtils.getTemplateBeanName(alias)}")
        }
        // if no alias provided grab first available template bean
        else {
            def factories = grailsApplication.mainContext.getBeansOfType(RabbitTemplate)
            template =  factories.keySet().iterator().next()
        }

        if (!template)
            throw new RuntimeException( "There is no available RabbitTemplate for alias: ${alias}" )
        else
            return template
    }
}
