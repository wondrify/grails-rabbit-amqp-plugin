package com.bertram.rabbitmq.util

import com.rabbitmq.client.ConnectionFactory

/**
 * Created by jsaardchit on 5/10/17.
 */
class ConnectionFactoryHelper extends ConnectionFactory {
	public void setSslProtocol(String proto) {
		super.useSslProtocol(proto)
	}
}
