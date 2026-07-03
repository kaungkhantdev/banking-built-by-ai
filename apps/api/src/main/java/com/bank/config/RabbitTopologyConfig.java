package com.bank.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology, declared once at startup: a topic exchange for domain
 * events, a durable queue for transfer notifications bound on
 * {@code transfer.completed}, and a dead-letter exchange for poison messages.
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "bank.events";
    public static final String DLX = "bank.events.dlx";
    public static final String TRANSFER_QUEUE = "notifications.transfer-completed";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX);
    }

    @Bean
    Queue transferQueue() {
        return QueueBuilder.durable(TRANSFER_QUEUE)
                .deadLetterExchange(DLX)
                .build();
    }

    @Bean
    Binding transferBinding(Queue transferQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(transferQueue).to(eventsExchange).with("transfer.completed");
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
