package com.bertram.rabbitmq
import org.springframework.amqp.core.AnonymousQueue
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.Connection
import org.springframework.amqp.rabbit.connection.ConnectionListener
import org.springframework.amqp.core.Queue
import groovy.util.logging.Commons

/**
 * Created by jsaardchit on 6/30/14.
 */
/**
 * Message listener container that creates a temporary, auto-delete queue and
 * binds it to a configured exchange.
 */
@Commons
class AutoQueueMessageListenerContainer extends AutoStartSimpleMessageListenerContainer implements ConnectionListener {
    String rabbitAdminBeanName = "adm"

    /**
     * The exchange to bind the temporary queue to.
     */
    String exchangeBeanName

    /**
     * The routing key to bind the queue to the exchange with. This is
     * the 'match-all' wildcard by default: '#'.
     */
    String routingKey = '#'
	Queue queue
	Boolean hasStarted = false

	public AutoQueueMessageListenerContainer() {
	}

	@Override
    protected void doStart() {
        // Check the exchange name has been specified.
        if (!exchangeBeanName) {
            log.error "Property [exchangeBeanName] must have a value!"
            return
        }

        // setup the binding
        setupBinding()

        // Let the super class do the rest.
        super.setQueueNames(queue.name)
        super.doStart()
	    hasStarted = true
	}

	@Override
	void onClose(Connection con) {
		log.error("Lost connection to ${applicationContext.getBean(exchangeBeanName).name} - ${queue.name}")
	}

	@Override
	void onCreate(Connection con) {
		log.info("Connection created to ${applicationContext.getBean(exchangeBeanName).name} - ${queue.name}")
		def adminBean = applicationContext.getBean(rabbitAdminBeanName)
		adminBean.declareQueue(queue)
        setupBinding(adminBean)
	}

	@Override
	void start() {
		if (!hasStarted)
			this.connectionFactory.addConnectionListener(this)

		super.start()
	}

    protected void setupBinding(adminBean = null) {
        adminBean = adminBean ?: applicationContext.getBean(rabbitAdminBeanName)

        // Now bind this queue to the named exchanged. If the exchange is a
        // fanout, then we don't bind with a routing key. If it's a topic,
        // we use the 'match-all' wildcard. Other exchange types are not
        // supported.
        def exchange = applicationContext.getBean(exchangeBeanName)
        adminBean.declareExchange(exchange)

        def binding = null
        if (exchange instanceof FanoutExchange) {
            binding = BindingBuilder.bind(queue).to(exchange);
        }
        else if (exchange instanceof DirectExchange || exchange instanceof TopicExchange) {
            binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        }
        else {
            log.error "Cannot subscribe to an exchange ('${exchange.name}') that is neither a fanout nor a topic"
            return
        }

        adminBean.declareBinding(binding)
    }
}
