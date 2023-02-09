package com.kai.springkafkaerrorhandlingexample.consumer.noBlockingRetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SingleTopicRetryConsumer {

    // @RetryableTopic: This annotation is used to configure the retry topic.
    // attempts: It is used to specify the number of attempts to retry the message.
    // backoff: It is used to specify the backoff delay and multiplier. The delay is the initial delay and the multiplier is used to calculate the next delay.
    // fixedDelayTopicStrategy: It is used to specify the fixed delay strategy to use. In this case, it is set to SINGLE_TOPIC.
    // fixedDelayTopicStrategy - SINGLE_TOPIC: It is used to specify that the retry topic is the same as the original topic.
    // include: It is used to specify the exception types to retry.
    // includeNames: It is used to specify the exception names to retry.
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 1.0),
        fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
        include = {ClassCastException.class},
        includeNames = "java.lang.ClassCastException")
    @KafkaListener(topics = "non-bloking-singleTopicRetryConsumer-products-retry")
    public void listen(ConsumerRecord<String, String> message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

            log.info("\nSingleTopicRetryConsumer: message consumed - \nkey: {} , \nvalue: {}, \ntopic: {}",
                message.key(),
                message.value(),
                message.topic());
            throw new ClassCastException("Exception in non-bloking-singleTopicRetryConsumer-products-retry consumer");
    }

    // @DltHandler: This annotation is used to configure the dead letter topic.
    @DltHandler
    public void dltListener(ConsumerRecord<String, String> message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("\nSingleTopicRetryConsumer: message consumed at DLT - \nkey: {} , \nvalue: {}, \ntopic: {}",
                message.key(),
                message.value(),
                message.topic());
    }

}
