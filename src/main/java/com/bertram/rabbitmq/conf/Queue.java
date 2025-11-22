package com.bertram.rabbitmq.conf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jsaardchit on 6/30/14.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Queue {
    String name() default "";
    boolean autoDelete() default false;
    boolean durable() default true;
    boolean exclusive() default true;
    int consumers() default 1;
    int prefetchCount() default 0;
    String conAlias() default "";
    boolean autoStartup() default true;
    AMQPParameter[] params() default {};
}
