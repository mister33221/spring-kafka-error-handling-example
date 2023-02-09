package com.kai.springkafkaerrorhandlingexample.consumer.blockingRetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class Consumer {

    private final KafkaTemplate<String, String> template;

    public Consumer(KafkaTemplate<String, String> template) {
        this.template = template;
    }

    // @KafkaListener: It is used to listen to the messages from the specified topic.
    // listen: This method is used to listen to the messages from the Main Topic.
    // ConsumerRecord: It is used to get the message from the topic.
    // @Header: It is used to get the header information from the message.
    // KafkaHeaders.RECEIVED_TOPIC: It is used to get the topic name from the header.
    @KafkaListener(topics = "products")
    public void listen(ConsumerRecord<String, String> message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {

            if (message.key().equals("1")) {
                throw new RuntimeException("Exception in main consumer");
            }
            log.info("\nmessage consumed - \nkey: {} , \nvalue: {}, \nat: {}", message.key(), message.value(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("\nfailed to consume - \nkey: {}", message.key());
            // send failed event to another retry topic
            // If you want to test no-bloking retry, you have to comment out code which are marked "For blocking retry" in KafkaConfig.java
            template.send("blocking-products-retry", message.key(), message.value());
//            template.send("non-bloking-multipleTopicRetryConsumer-products-retry", message.key(), message.value());
//            template.send("non-bloking-singleTopicRetryConsumer-products-retry", message.key(), message.value());
        }

    }

}
