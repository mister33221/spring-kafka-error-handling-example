package com.kai.springkafkaerrorhandlingexample.consumer.noBlockingRetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MultipleTopicRetryConsumer {

    // @RetryableTopic is used to configure the retry topic for the listener.
    // The attempts attribute specifies the number of attempts to retry the message.
    // The backoff attribute specifies the backoff policy to use. In this case, the delay is 1 second and the multiplier is 1.0. The multiplier is used to calculate the next delay.
    // The autoCreateTopics attribute specifies whether to create the retry topic automatically. In this case, it is set to false.
    // The topicSuffixingStrategy attribute specifies the topic suffixing strategy to use. In this case, it is set to SUFFIX_WITH_INDEX_VALUE.
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 1.0),
            autoCreateTopics = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = "no-bloking-multipleTopicRetryConsumer-products-retry")
    public void listen(ConsumerRecord<String, String> message) {
        log.info("MultipleTopicRetryConsumer retrying message - key: {} , value: {}, at: {}, offset: {}", message.key(), message.value(), LocalDateTime.now(), message.offset());
        throw new RuntimeException("Exception in no-bloking-multipleTopicRetryConsumer-products-retry consumer");
    }

    // The DLT handler is used to handle the messages that have exceeded the maximum number of retries.
    @DltHandler
    public void multipleTopicDLT(ConsumerRecord<String, String> message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("MultipleTopicRetryConsumer: message consumed at DLT - \nkey: {} , \nvalue: {}, \ntopic: {}",
                message.key(),
                message.value(),
                message.topic());
    }


}
