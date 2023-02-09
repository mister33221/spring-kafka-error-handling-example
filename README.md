# spring-kafka-error-handling-example

{%hackmd Hy_uVFcRD %}
# Kafka 例外處理

首先，原生kafka是不支持消息重試的。但是spring kafka 2.7+ 封裝了 Retry Topic這個功能。
請各位再嘗試的時候，請注意個依賴的版本問題，小弟我啟環境的時候，試了好多遍Orz。

## 專案位置
https://github.com/mister33221/spring-kafka-error-handling-example.git

## 環境
* java: openjdk-17
* spring-boot: 2.6.6
* spring-kafka: 2.8.5
* lombok: 1.2.11

## 開始弄髒手

其實我都寫好了，應該不會弄很髒~

1. 把這個寫好的範例專案clone下來
    ```
    https://github.com/mister33221/spring-kafka-error-handling-example.git
    ```

2. 先使用內含的docker-compose.yml搭配docker將kafka、zookeeper、broker、control-center起起來，在cmd介面中輸入以下指令
    ```
    docker-compose up
    ```
   kafka的控制平台port號為9021。
   成功完成後你可以在網址輸入localhost:9021，如果出現以下畫面就是成功了。
   ![](https://i.imgur.com/uxDMFUA.png)

2. 接著就更新pom.xml把依賴載回來
3. 啟動專案
4. 完成啦!! 下課!!

## 如何測試

程式碼中我盡量都有寫上註解，以更好了解做了甚麼事情。

* 程式中有分為三種不同的處理方式
    1. blocking: 會鎖在同一個topic上，並在同一個topic上繼續retry
    2. non-blocking: 會把topic放開，並新開topic，在這上面進行retry
        1. multiple Topic: 每次retry，都新開一個topic
        2. single Topic: 第一次retry會

* 首先看到KafkaConfig.java，這裡定義了error處理的方式
    ```java
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
            // for blocking retry
            factory.setCommonErrorHandler(retryErrorHandler());
            consumerFactory.getListeners();
            return factory;
        }

        // For blocking retry
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
    ```

* 來看 Consumer.java
    ```java
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

                if (message.key().equals("product2")) {
                    throw new RuntimeException("Exception in main consumer");
                }
                log.info("message consumed - key: {} , value: {}, at: {}", message.key(), message.value());
            } catch (Exception e) {
                log.error("failed to consume - key: {}", message.key());
                // send failed event to another retry topic
                // If you want to test no-bloking retry, you have to comment out code which are marked "For blocking retry" in KafkaConfig.java
                template.send("blocking-products-retry", message.key(), message.value());
    //            template.send("non-bloking-multipleTopicRetryConsumer-products-retry", message.key(), message.value());
    //            template.send("non-bloking-singleTopicRetryConsumer-products-retry", message.key(), message.value());
            }

        }

    }
    ```
    * 倒數三行為三種不同處理方式，可透過註解來選用。如果要使用blocking的話，要到config中將我有標注for blocking retry的部分註解起來。

        1. 我們使用[Control Centeter-localhost:9021](http://localhost:9021/clusters)來進行測試
           ![](https://i.imgur.com/mle34NP.png)
        2. 點擊topic
           ![](https://i.imgur.com/AjgVwbf.png)
        3. 找到 product 這個 topic -> messages -> produce a message to this topic
        4. 在key輸入 1 或 null 就可以製造exception
           ![](https://i.imgur.com/HcsuNkA.png)


        * blocking
            我們可以看下圖，我只截了第一張，你會發現每次就噴一個exception，且每一次都是同一個原始的 topic ，因為他已經被鎖在上面了，造成後面連續的噴錯，而這個重試的預設機制為 retry 3次 ，每次間隔1秒。也可以到config的檔案中自行設定。
            ![](https://i.imgur.com/5ubC6Pl.png)

        * non-block-multipleTopicRetryConsumer
            我們可以看下面的圖，發現他每次都在 topic 名稱後加上 retry 及編號，顯示出他把上一次用的 topic 放開了，並用一個全新的 topic 來進行 retry 。
            ![](https://i.imgur.com/a3Jfeb3.png)

        * non-blok-singleTopicRetryConsumer-products-retry
            我們可以看下面的圖，發現他開始要 retry 時，在原本的 topic 後加上 retry ，並每一次的retry都使用這個新建的 topic 來進行。
            ![](https://i.imgur.com/CmebmVO.png)



其他程式碼及annotation的用法及含意，我都註解寫在code中了，
想知道的話..
![](https://i.imgur.com/VnrRrhj.png)


--------
###### tags: `kafka` `springboot`
