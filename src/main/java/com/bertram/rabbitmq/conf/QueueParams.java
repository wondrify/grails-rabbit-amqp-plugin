package com.bertram.rabbitmq.conf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jsaardchit on 1/5/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface QueueParams {
	String name() default "";
	boolean autoNodeName() default false;
	boolean durable() default false;
	boolean autoDelete() default true;
	boolean exclusive() default true;
}
