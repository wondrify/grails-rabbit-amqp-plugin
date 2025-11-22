package com.bertram.rabbitmq.util

/**
 * Created by jsaardchit on 6/30/14.
 */
class SpringBeanUtils {
    public static final String QUEUE_PREFIX = 'grails.rabbit.queue'
    public static final String EXCHANGE_PREFIX = 'grails.rabbit.exchange'
    public static final String BINDING_PREFIX = 'grails.rabbit.binding'
    public static final String ADAPTER_SUFFIX = 'RabbitAdapter'
    public static final String CONTAINER_SUFFIX = "container"
    public static final String CON_FACTORY_SUFFIX = 'rabbitMQConnectionFactory'
    public static final String CON_FACTORY_BEAN_SUFFIX = 'rabbitMQConnectionFactoryBean'
    public static final String TEMPLATE_SUFFIX = 'rabbitTemplate'
    public static final String ERROR_HANDLER_SUFFIX = 'rabbitErrorHandler'
    public static final String ADMIN_SUFFIX = 'adm'
    public static final String SERVICE_DELEGATE_SUFFIX = 'messageServiceDelegate'
    public static final String CLOSURE_DELEGATE_SUFFIX = 'messageClosureDelegate'

    // helper methods
    public static String getQueueBeanName(String name) {
        "${QUEUE_PREFIX}.${name}".toString()
    }

    public static String getExchangeBeanName(String name) {
        "${EXCHANGE_PREFIX}.${name}".toString()
    }

    public static String getBindingBeanName(String exchange, String queue) {
        "${BINDING_PREFIX}.${exchange}.${queue}".toString()
    }

    public static String getConFactoryBeanName(String prefix) {
        "${prefix}.${CON_FACTORY_SUFFIX}".toString()
    }

    public static String getRmqConFactoryBeanName(String prefix) {
        "${prefix}.${CON_FACTORY_BEAN_SUFFIX}".toString()
    }

    public static String getAdapterBeanName(String prefix) {
        "${prefix}.${ADAPTER_SUFFIX}".toString()
    }

    public static String getContainerBeanName(String prefix) {
        "${prefix}.${CONTAINER_SUFFIX}".toString()
    }

    public static String getTemplateBeanName(String prefix) {
        "${prefix}.${TEMPLATE_SUFFIX}".toString()
    }

    public static String getErrorHandlerBeanName(String prefix) {
        "${prefix}.${ERROR_HANDLER_SUFFIX}".toString()
    }

    public static String getAdminBeanName(String prefix) {
        "${prefix}.${ADMIN_SUFFIX}".toString()
    }

    public static String getServiceDelegateBeanName(String prefix) {
        "${prefix}.${SERVICE_DELEGATE_SUFFIX}".toString()
    }

    public static String getClosureDelegateBeanName(String prefix) {
        "${prefix}.${CLOSURE_DELEGATE_SUFFIX}".toString()
    }
}
