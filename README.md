# Grails RabbitMQ AMQP Plugin

A RabbitMQ plugin for **Grails 7** (Java 17 / Groovy 4) that supports multiple connection factories, annotation-based consumer configuration, and exchange/topic subscriptions.

## Documentation

📚 **Full Documentation**: https://wondrify.github.io/grails-rabbit-amqp-plugin/

## Installation

Add the plugin to your `build.gradle`:

```gradle
dependencies {
    implementation 'cloud.wondrify:grails-rabbit-amqp-plugin:7.0.0'
}
```

For Grails 6.x and earlier versions, use the previous coordinates:

```gradle
dependencies {
    implementation 'com.bertramlabs.plugins:rabbit-amqp:6.x.x'
}
```

## Quick Start

Configure a connection factory:

```groovy
// application.groovy
rabbitmq {
    connectionFactories {
        factory(
            name: 'main',
            hostname: 'localhost',
            username: 'guest',
            password: 'guest'
        )
    }
}
```

Create a consumer service:

```groovy
import com.bertram.rabbitmq.conf.RabbitConsumer
import com.bertram.rabbitmq.conf.Queue

@RabbitConsumer
class MyMessageService {

    @Queue(name = 'my.queue', durable = true)
    def handleMessage(Map message) {
        println "Received: ${message}"
    }
}
```

Send messages from any service or controller (via the `SendRabbitMessage` trait):

```groovy
class MyController {

    def index() {
        sendRabbitMessage('main', 'my.queue', [hello: 'world'])
        render "Sent!"
    }
}
```

## License

Apache License 2.0
