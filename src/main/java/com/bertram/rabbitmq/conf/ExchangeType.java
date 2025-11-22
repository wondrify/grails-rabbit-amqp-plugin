package com.bertram.rabbitmq.conf;

import org.springframework.amqp.core.*;

/**
 * Created by jsaardchit on 6/30/14.
 */
public enum ExchangeType {
    topic( TopicExchange.class ),
    direct( DirectExchange.class ),
    fanout( FanoutExchange.class ),
    headers( HeadersExchange.class );

    Class exchangeType;

    ExchangeType( Class et ) {
        exchangeType = et;
    }

    @SuppressWarnings("unchecked")
    Class<AbstractExchange> type() {
        return exchangeType;
    }
}
