package com.bertram.rabbitmq

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.ErrorHandler
import groovy.util.logging.Commons
/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons
class RabbitErrorHandler implements ErrorHandler {

    void handleError(Throwable t) {
        log.error "Rabbit service listener failed.", t
    }
}
