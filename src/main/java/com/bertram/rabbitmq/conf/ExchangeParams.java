package com.bertram.rabbitmq.conf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jsaardchit on 1/6/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExchangeParams {
	String name() default "";
	boolean durable() default true;
	boolean autoDelete() default false;
	ExchangeType exchangeType() default ExchangeType.topic;
}
