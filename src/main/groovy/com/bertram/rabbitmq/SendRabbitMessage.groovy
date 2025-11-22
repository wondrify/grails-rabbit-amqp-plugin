/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bertram.rabbitmq

import grails.artefact.Enhances
import groovy.transform.CompileStatic
import org.grails.core.DefaultGrailsControllerClass
import org.grails.core.DefaultGrailsServiceClass
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.amqp.core.MessagePostProcessor

@Enhances([DefaultGrailsServiceClass.SERVICE, DefaultGrailsControllerClass.CONTROLLER])
trait SendRabbitMessage {
	public sendRabbitMessage(Object message ) {
        grails.util.Holders.applicationContext.getBean('rabbitMQService', RabbitMQService).convertAndSend(message)
    }
    public sendRabbitMessage(String alias, Object message ) {
        grails.util.Holders.applicationContext.getBean('rabbitMQService', RabbitMQService).convertAndSend(alias, message)
    }
    public sendRabbitMessage(String alias, String routingKey, Object message ) {
        grails.util.Holders.applicationContext.getBean('rabbitMQService', RabbitMQService).convertAndSend(alias, routingKey, message)
    }
    public sendRabbitMessage(String alias, String exchange, String routingKey, Object message ) {
        grails.util.Holders.applicationContext.getBean('rabbitMQService', RabbitMQService).convertAndSend(alias, exchange, routingKey, message)
    }
    public sendRabbitMessage(String alias, String exchange, String routingKey, Object message, MessagePostProcessor processor ) {
        grails.util.Holders.applicationContext.getBean('rabbitMQService', RabbitMQService).convertAndSend(alias, exchange, routingKey, message, processor)
    }
}