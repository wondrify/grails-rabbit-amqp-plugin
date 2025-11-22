package com.bertram.rabbitmq.util

import grails.persistence.support.PersistenceContextInterceptor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import groovy.util.logging.Commons
/**
 * Created by jsaardchit on 6/30/14.
 */
@Commons 
class MessageClosureDelegate implements ApplicationContextAware {
    ApplicationContext applicationContext
    PersistenceContextInterceptor persistenceInterceptor
    Closure delegate

    public MessageClosureDelegate( Closure clos ) {
        super()
        this.delegate = clos
    }

    public void handleMessage(Map msg) {
        handle(msg)
    }

    public void handleMessage(String msg) {
        handle(msg)
    }

    public void handleMessage(byte[] msg) {
        handle(msg)
    }

    public void handleMessage(List msg) {
        handle(msg)
    }

    private void handle(msg) {
        try {
            log.debug("opening persistence context before delegating to message handler")
            persistenceInterceptor.init()
            delegate.call(msg)
        }
        finally {
            if (persistenceInterceptor) {
                log.debug("Flushing persistence context")
                try {
                    persistenceInterceptor.flush()    
                } catch(ex) {
                    log.warn("An Error Occurred attempting to flush the persistenceInterceptor. This may be related to a previous error in the thread context ${ex}",ex)
                }

                try {
                    persistenceInterceptor.clear()    
                } catch(ex2) {
                    log.warn("An Error Occurred attempting to clear the persistenceInterceptor. This may be related to a previous error in the thread context ${ex2}",ex2)
                }
                
                persistenceInterceptor.destroy()
            }
        }
    }
}
