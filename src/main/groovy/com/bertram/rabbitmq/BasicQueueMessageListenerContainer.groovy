package com.bertram.rabbitmq

import groovy.util.logging.Commons
import org.springframework.context.ApplicationContextAware

/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons
class BasicQueueMessageListenerContainer extends AutoStartSimpleMessageListenerContainer {
    org.springframework.amqp.core.Queue queue
    protected void doStart() {
        // First, create a broker-named, temporary queue.
        def adminBean = applicationContext.getBean(rabbitAdminBeanName)
        adminBean.declareQueue(queue)

        // Let the super class do the rest.
        super.setQueueNames(queue.name)
        super.doStart()
    }
}
