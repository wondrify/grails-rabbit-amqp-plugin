package com.bertram.rabbitmq.conf;

/**
 * Created by jsaardchit on 6/30/14.
 */
public @interface AMQPParameter {
    String name() default "";
    String value() default "";
}
