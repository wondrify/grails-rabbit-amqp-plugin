package com.bertram.rabbitmq.conf;

import java.lang.annotation.*;

/**
 * Created by jsaardchit on 6/30/14.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscriber {
	@Deprecated
    boolean autoDelete() default true;
	@Deprecated
    boolean durable() default false;
    int consumers() default 1;
	int prefetchCount() default 0;
    String conAlias() default "";
	@Deprecated
    String exchangeName() default "";
    String routingKey() default "#";
	@Deprecated
    ExchangeType exchangeType() default ExchangeType.topic;
    boolean autoStartup() default true;
    boolean multiSubscriber() default false;
	ExchangeParams exchangeParams() default @ExchangeParams();
	QueueParams queueParams() default @QueueParams();
}
