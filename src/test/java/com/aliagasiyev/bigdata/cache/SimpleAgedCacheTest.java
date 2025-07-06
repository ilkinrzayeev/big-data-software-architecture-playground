package com.aliagasiyev.bigdata.cache;

import org.junit.jupiter.api.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimpleAgedCacheTest {

    private Clock fixedClock(long millis) {
        return Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }

    @Test
    void cacheIsEmptyOnCreation() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 16);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    void putAndGetWorks() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 16);
        cache.put("foo", 42, 1000);
        assertEquals(42, cache.get("foo"));
        assertEquals(1, cache.size());
    }

    @Test
    void getReturnsNullForExpiredEntry() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC);
        SimpleAgedCache cache = new SimpleAgedCache(clock, 16);
        cache.put("foo", 42, 10);
        Clock later = Clock.fixed(Instant.ofEpochMilli(2000), ZoneOffset.UTC);
        SimpleAgedCache cache2 = new SimpleAgedCache(later, 16);
        cache2.put("foo", 42, 10);
        assertNull(cache2.get("foo"));
    }

    @Test
    void putOverwritesValueAndResetsExpiry() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 16);
        cache.put("foo", 42, 1000);
        cache.put("foo", 43, 2000);
        assertEquals(43, cache.get("foo"));
    }

    @Test
    void sizeIgnoresExpiredEntries() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC);
        SimpleAgedCache cache = new SimpleAgedCache(clock, 16);
        cache.put("foo", 42, 10);
        cache.put("bar", 43, 1000);
        cache.evictExpired();
        assertEquals(1, cache.size());
    }

    @Test
    void nullKeyOrValueThrows() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 16);
        assertThrows(NullPointerException.class, () -> cache.put(null, 1, 1000));
        assertThrows(NullPointerException.class, () -> cache.put("foo", null, 1000));
    }

    @Test
    void negativeOrZeroRetentionThrows() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 16);
        assertThrows(IllegalArgumentException.class, () -> cache.put("foo", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> cache.put("foo", 1, -1));
    }

    @Test
    void handlesHashCollisions() {
        SimpleAgedCache cache = new SimpleAgedCache(fixedClock(1000), 2);
        Object key1 = new Object() { public int hashCode() { return 1; }};
        Object key2 = new Object() { public int hashCode() { return 1; }};
        cache.put(key1, "A", 1000);
        cache.put(key2, "B", 1000);
        assertEquals("A", cache.get(key1));
        assertEquals("B", cache.get(key2));
    }

    @Test
    void evictExpiredRemovesAllExpired() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC);
        SimpleAgedCache cache = new SimpleAgedCache(clock, 16);
        cache.put("foo", 42, 10);
        cache.put("bar", 43, 10);
        cache.evictExpired();
        assertEquals(0, cache.size());
    }

    @Test
    void concurrencyPutAndGet() throws InterruptedException {
        SimpleAgedCache cache = new SimpleAgedCache(Clock.systemUTC(), 32);
        int threads = 10;
        int ops = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                for (int i = 0; i < ops; i++) {
                    String key = "k" + i;
                    cache.put(key, i, 10000);
                    Object val = cache.get(key);
                    if (val == null || !val.equals(i)) errors.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        };
        for (int t = 0; t < threads; t++) new Thread(task).start();
        latch.await();
        assertEquals(0, errors.get(), "No concurrency errors expected");
    }

    @Test
    void stressTestLargeNumberOfEntries() {
        SimpleAgedCache cache = new SimpleAgedCache(Clock.systemUTC(), 1024);
        int n = 10000;
        for (int i = 0; i < n; i++) {
            cache.put("key" + i, i, 10000);
        }
        assertEquals(n, cache.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i, cache.get("key" + i));
        }
    }

}