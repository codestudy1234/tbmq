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
package org.thingsboard.mqtt.broker.queue.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.mqtt.broker.queue.TbQueueControlledOffsetConsumer;
import org.thingsboard.mqtt.broker.queue.TbQueueMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractTbQueueConsumerTemplate<R, T extends TbQueueMsg> implements TbQueueControlledOffsetConsumer<T> {

    // TODO: use TB consumer template
    private volatile boolean subscribed;
    protected volatile boolean stopped = false;
    protected final Lock consumerLock = new ReentrantLock();

    @Getter
    private final String topic;

    public AbstractTbQueueConsumerTemplate(String topic) {
        this.topic = topic;
    }

    @Override
    public void subscribe() {
        consumerLock.lock();
        try {
            doSubscribe(topic);
            subscribed = true;
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void assignPartition(int partition) {
        consumerLock.lock();
        try {
            doAssignPartition(topic, partition);
            subscribed = true;
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void assignAllPartitions() {
        consumerLock.lock();
        try {
            doAssignAllPartitions(topic);
            subscribed = true;
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void unsubscribeAndClose() {
        stopped = true;
        consumerLock.lock();
        try {
            doUnsubscribeAndClose();
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (stopped) {
            log.error("Poll invoked but consumer stopped for topic {}.", topic);
            return Collections.emptyList();
        }
        long startNanos = System.nanoTime();
        if (!subscribed) {
            sleep(startNanos, durationInMillis);
            return Collections.emptyList();
        }

        List<R> records;
        consumerLock.lock();
        try {
            records = doPoll(durationInMillis);
        } finally {
            consumerLock.unlock();
        }

        if (records.isEmpty()) {
            sleep(startNanos, durationInMillis);
            return Collections.emptyList();
        }

        return decodeRecords(records);
    }

    private List<T> decodeRecords(List<R> records) {
        List<T> result = new ArrayList<>(records.size());
        records.forEach(record -> {
            try {
                if (record != null) {
                    result.add(decode(record));
                }
            } catch (IOException e) {
                log.error("Failed decode record: [{}]", record);
                throw new RuntimeException("Failed to decode record: ", e);
            }
        });
        return result;
    }

    private void sleep(final long startNanos, final long durationInMillis) {
        long durationNanos = TimeUnit.MILLISECONDS.toNanos(durationInMillis);
        long spentNanos = System.nanoTime() - startNanos;
        if (spentNanos < durationNanos) {
            try {
                Thread.sleep(Math.max(TimeUnit.NANOSECONDS.toMillis(durationNanos - spentNanos), 1));
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.error("Failed to wait", e);
                }
            }
        }
    }

    @Override
    public void commit() {
        consumerLock.lock();
        try {
            doCommit();
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void commit(int partition, long offset) {
        consumerLock.lock();
        try {
            doCommit(topic, partition, offset);
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public long getOffset(String topic, int partition) {
        consumerLock.lock();
        try {
            return doGetOffset(topic, partition);
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void seekToTheBeginning() {
        consumerLock.lock();
        try {
            doSeekToTheBeginning();
        } finally {
            consumerLock.unlock();
        }
    }

    abstract protected List<R> doPoll(long durationInMillis);

    abstract protected T decode(R record) throws IOException;

    abstract protected void doSubscribe(String topic);

    abstract protected void doAssignPartition(String topic, int partition);

    abstract protected void doAssignAllPartitions(String topic);

    abstract protected void doCommit();

    abstract protected void doCommit(String topic, int partition, long offset);

    abstract protected void doUnsubscribeAndClose();

    abstract protected long doGetOffset(String topic, int partition);

    abstract protected void doSeekToTheBeginning();
}
