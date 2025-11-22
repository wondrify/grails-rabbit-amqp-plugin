package com.bertram.rabbitmq

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Created by jsaardchit on 6/30/14.
 */
class AutoStartSimpleMessageListenerContainer extends SimpleMessageListenerContainer implements ApplicationContextAware {
    Boolean startOnLoad = true
    String rabbitAdminBeanName = "adm"
    ApplicationContext applicationContext
}
