/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "kafka")
public class TbKafkaMsgQueueService extends TbAbstractMsgQueueService {

    @Autowired
    private TbConsumerSettings consumerSettings;

    @Autowired
    private TbProducerSettings producerSettings;

    @Value("${kafka.queue.consumer.timeout}")
    private long timeout;

    @Value("${kafka.queue.producer.topic}")
    private String topic;

    @Value("${kafka.queue.consumer.topic_pattern}")
    private String topicPattern;

    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;

    @PostConstruct
    private void init() {
        initProducer();
        initConsumer();
    }

    @Override
    public void add(TbMsg msg) {
        byte[] data = TbMsg.toByteArray(msg);
        producer.send(new ProducerRecord<>(topic, msg.getOriginator().toString(), data));
    }

    private void initProducer() {
        Properties props = producerSettings.toProps();
        this.producer = new KafkaProducer<>(props);
    }

    private void initConsumer() {
        Properties props = consumerSettings.toProps();
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Pattern.compile(topicPattern));

        executor.execute(() -> {
            while (true) {
                if (isAck.get()) {
                    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(timeout));
                    if (records.count() > 0) {
                        isAck.set(false);
                        createAndSendTbMsgQueuePack(records);
                    }
                }
            }
        });
    }

    private void createAndSendTbMsgQueuePack(ConsumerRecords<String, byte[]> records) {
        UUID packId = UUID.randomUUID();
        currentPack = new TbMsgQueuePack(packId, new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicBoolean(false));
        for (ConsumerRecord<String, byte[]> record : records) {
            TbMsg msg = TbMsg.fromBytes(record.value());
            TbMsgQueueState msgQueueState = new TbMsgQueueState(
                    msg.copy(msg.getId(), packId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()),
                    new AtomicInteger(0),
                    new AtomicBoolean(false));
            currentPack.addMsg(msgQueueState);
        }
        send(currentPack);
    }

    @PreDestroy
    private void destroy() {
        executor.shutdown();
        producer.close();
        consumer.close();
    }
}
