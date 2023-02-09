package com.kai.springkafkaerrorhandlingexample.consumer.blockingRetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BlockingRetryConsumer {

    // @KafkaListener: It is used to listen to the messages from the specified topic.
    // listen: This method is used to listen to the messages from the Retry Topic.
    @KafkaListener(topics = "blocking-products-retry", containerFactory = "kafkaBlockingRetryContainerFactory")
    public void listen(ConsumerRecord<String, String> message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        log.info("retrying message - key: {} , value: {}, at: {}, offset: {}", message.key(), message.value(), message.offset());
        throw new Exception("Exception in blocking-products-retry consumer");
    }

    // @KafkaListener: It is used to listen to the messages from the specified topic.
    // listenDLT: This method is used to listen to the messages from the Dead Letter Topic.
    @KafkaListener(topics = "products-retry.DLT")
    public void listenDLT(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        log.info("blocking-products-retry: message consumed at DLT - \nkey: {} , \nvalue: {}, \ntopic: {}",
                message, topic);
    }

}
