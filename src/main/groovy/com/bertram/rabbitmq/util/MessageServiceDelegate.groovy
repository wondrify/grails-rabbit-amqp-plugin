package com.bertram.rabbitmq.util

import grails.persistence.support.PersistenceContextInterceptor
import groovy.util.logging.Commons
/**
 * Created by jsaardchit on 6/30/14.
 */

@Commons
class MessageServiceDelegate {
    // needed to bind an orm session to the service we delegate too
    PersistenceContextInterceptor persistenceInterceptor

    def grailsApplication
    def serviceName
    def listenerMethod

    MessageServiceDelegate(grailsApplication, serviceName, listenerMethod) {
        this.grailsApplication = grailsApplication
        this.persistenceInterceptor = grailsApplication.mainContext.getBean('persistenceInterceptor')
        this.serviceName = serviceName
        this.listenerMethod = listenerMethod
    }

    MessageServiceDelegate() {
        
    }

    public void handleMessage( Map msg ) {
        delegateMessage( msg )
    }

    public void handleMessage( String msg ) {
        delegateMessage( msg )
    }

    public void handleMessage( byte[] msg ) {
        delegateMessage( msg )
    }

    public void handleMessage( List msg ) {
        delegateMessage( msg )
    }

    /**
     * This method delegates the message handler to the service method configured by annotation
     * @param msg
     * @return
     */
    protected delegateMessage( msg ) {
        try {
            log.debug("Initializing a persistence context")
            persistenceInterceptor.init()
            grailsApplication.mainContext.getBean("${serviceName}Service")."${listenerMethod}"( msg )
        }
        finally {
            if (persistenceInterceptor) {
                log.debug("flushing persistence context")
                try {
                    persistenceInterceptor.flush()    
                } catch(ex) {
                    log.warn("An Error Occurred attempting to flush the persistenceInterceptor. This may be related to a previous error in the thread context ${ex}",ex)
                }
                
                persistenceInterceptor.destroy()
            }
        }
    }
}
