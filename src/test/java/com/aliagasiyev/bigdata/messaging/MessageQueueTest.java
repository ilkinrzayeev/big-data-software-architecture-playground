package com.aliagasiyev.bigdata.messaging;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueTest {

    @Test
    void queueIsEmptyOnCreation() {
        MessageQueue<String> queue = new MessageQueue<>(10);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void enqueueAndDequeueWorks() {
        MessageQueue<Integer> queue = new MessageQueue<>(5);
        queue.enqueue(1);
        queue.enqueue(2);
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void throwsWhenDequeuingEmpty() {
        MessageQueue<String> queue = new MessageQueue<>(2);
        assertThrows(IllegalStateException.class, queue::dequeue);
    }

    @Test
    void throwsWhenEnqueueingFull() {
        MessageQueue<Integer> queue = new MessageQueue<>(2);
        queue.enqueue(1);
        queue.enqueue(2);
        assertThrows(IllegalStateException.class, () -> queue.enqueue(3));
    }

    @Test
    void nullEnqueueThrows() {
        MessageQueue<String> queue = new MessageQueue<>(2);
        assertThrows(NullPointerException.class, () -> queue.enqueue(null));
    }

    @Test
    void orderIsPreserved() {
        MessageQueue<String> queue = new MessageQueue<>(10);
        for (int i = 0; i < 10; i++) queue.enqueue("msg" + i);
        for (int i = 0; i < 10; i++) assertEquals("msg" + i, queue.dequeue());
    }

    @Test
    void isFullAndIsEmptyWork() {
        MessageQueue<Integer> queue = new MessageQueue<>(2);
        assertTrue(queue.isEmpty());
        queue.enqueue(1);
        queue.enqueue(2);
        assertTrue(queue.isFull());
        queue.dequeue();
        assertFalse(queue.isFull());
    }

    @Test
    void stressTestEnqueueDequeue() {
        MessageQueue<Integer> queue = new MessageQueue<>(1000);
        for (int i = 0; i < 1000; i++) queue.enqueue(i);
        for (int i = 0; i < 1000; i++) assertEquals(i, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void concurrentEnqueueAndDequeue() throws InterruptedException {
        MessageQueue<Integer> queue = new MessageQueue<>(100);
        int threads = 10;
        int ops = 1000;
        CountDownLatch latch = new CountDownLatch(threads * 2);
        AtomicInteger enqCount = new AtomicInteger(0);
        AtomicInteger deqCount = new AtomicInteger(0);

        Runnable enq = () -> {
            for (int i = 0; i < ops; i++) {
                try {
                    queue.enqueue(i);
                    enqCount.incrementAndGet();
                } catch (IllegalStateException ignored) {}
            }
            latch.countDown();
        };
        Runnable deq = () -> {
            for (int i = 0; i < ops; i++) {
                try {
                    queue.dequeue();
                    deqCount.incrementAndGet();
                } catch (IllegalStateException ignored) {}
            }
            latch.countDown();
        };
        for (int t = 0; t < threads; t++) {
            new Thread(enq).start();
            new Thread(deq).start();
        }
        latch.await();
        assertTrue(queue.size() >= 0 && queue.size() <= 100);
    }

    @Test
    void handlesRapidEnqueueDequeueAlternation() {
        MessageQueue<Integer> queue = new MessageQueue<>(10);
        for (int i = 0; i < 100; i++) {
            if (!queue.isFull()) queue.enqueue(i);
            if (!queue.isEmpty()) queue.dequeue();
        }
        // No exceptions = pass
    }

    @Test
    void handlesExtremeCapacity() {
        MessageQueue<Integer> queue = new MessageQueue<>(100_000);
        for (int i = 0; i < 100_000; i++) queue.enqueue(i);
        assertTrue(queue.isFull());
        for (int i = 0; i < 100_000; i++) assertEquals(i, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

}