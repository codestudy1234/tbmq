/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.mqtt.broker.queue.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.thingsboard.mqtt.broker.queue.TbQueueAdmin;
import org.thingsboard.mqtt.broker.queue.constants.QueueConstants;
import org.thingsboard.mqtt.broker.queue.kafka.settings.TbKafkaAdminSettings;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TbKafkaAdmin implements TbQueueAdmin {

    private final AdminClient client;
    private final Map<String, String> topicConfigs;
    private final Set<String> topics = ConcurrentHashMap.newKeySet();
    private final int numPartitions;

    private final short replicationFactor;

    public TbKafkaAdmin(TbKafkaAdminSettings adminSettings, Map<String, String> topicConfigs) {
        client = AdminClient.create(adminSettings.toProps());
        this.topicConfigs = topicConfigs;

        try {
            topics.addAll(client.listTopics().names().get());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get all topics.", e);
        }

        String numPartitionsStr = topicConfigs.get(QueueConstants.PARTITIONS);
        if (numPartitionsStr != null) {
            numPartitions = Integer.parseInt(numPartitionsStr);
            topicConfigs.remove(QueueConstants.PARTITIONS);
        } else {
            numPartitions = 1;
        }

        String replicationFactorStr = topicConfigs.get(QueueConstants.REPLICATION_FACTOR);
        if (replicationFactorStr != null) {
            replicationFactor = Short.parseShort(replicationFactorStr);
            topicConfigs.remove(QueueConstants.REPLICATION_FACTOR);
        } else {
            replicationFactor = 1;
        }
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        if (topics.contains(topic)) {
            return;
        }
        try {
            log.debug("[{}] Creating topic", topic);
            NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor).configs(topicConfigs);
            createTopic(newTopic).values().get(topic).get();
            topics.add(topic);
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof TopicExistsException) {
                //do nothing
            } else {
                log.warn("[{}] Failed to create topic", topic, ee);
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to create topic", topic, e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getNumberOfPartitions(String topic) {
        try {
            return client.describeTopics(Collections.singletonList(topic)).all().get().get(topic).partitions().size();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }

    public CreateTopicsResult createTopic(NewTopic topic) {
        return client.createTopics(Collections.singletonList(topic));
    }
}
