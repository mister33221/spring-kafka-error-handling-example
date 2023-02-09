package com.kai.springkafkaerrorhandlingexample.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.DefaultDestinationTopicResolver;
import org.springframework.kafka.retrytopic.DestinationTopicResolver;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.retrytopic.RetryTopicInternalBeanNames;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

// @EnableKafka: Enables detection of @KafkaListener annotations on Spring-managed beans
@EnableKafka
// @Configuration: Indicates that a class declares one or more @Bean methods and may be processed by the Spring container to generate bean definitions and service requests for those beans at runtime
@Configuration
public class KafkaConfig {

    // KafkaTemplate: A template for sending messages to Kafka topics
    private final KafkaTemplate<Object, Object> template;

    // ConsumerFactory: A factory for creating Kafka consumers
    private final ConsumerFactory<String, String> consumerFactory;

    public KafkaConfig(KafkaTemplate<Object, Object> template, ConsumerFactory<String, String> consumerFactory) {
        this.template = template;
        this.consumerFactory = consumerFactory;
    }

    // Container Factory containing bocking error handler
    // ConcurrentKafkaListenerContainerFactory: A factory for creating Kafka listener containers with a concurrency
    // of 1 (i.e. a single thread) and a batch size of 1 (i.e. a single record per poll)
    // kafkaBlockingRetryContainerFactory: A bean that creates a container factory for blocking retry
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaBlockingRetryContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(retryErrorHandler());
        consumerFactory.getListeners();
        return factory;
    }

    // This is a blocking retry (will move offset only when all tries are completed) error handler configured with
    // DeadLetterPublishingRecoverer which publishes event to DLT when tries are over
    // retryErrorHandler: A bean that handles errors in the listener container
    public DefaultErrorHandler retryErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 3));
    }

    // the kafka version is 2.8.3, so the bean name is RetryTopicInternalBeanNames.DESTINATION_TOPIC_CONTAINER_NAME
    // destinationTopicResolver: A bean that resolves the destination topic for a given retry topic
    @Bean(name = RetryTopicInternalBeanNames.DESTINATION_TOPIC_CONTAINER_NAME)
    public DestinationTopicResolver destinationTopicResolver(ApplicationContext context) {
        DefaultDestinationTopicResolver resolver = new DefaultDestinationTopicResolver(Clock.systemUTC(), context);
        resolver.setClassifications(Collections.emptyMap(), true);
        return resolver;
    }



}
