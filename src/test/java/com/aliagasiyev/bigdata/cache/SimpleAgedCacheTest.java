package com.aliagasiyev.bigdata.cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleAgedCacheTest {

    @Test
    void cacheIsEmptyOnCreation() {
        SimpleAgedCache cache = new SimpleAgedCache();
        assertTrue(cache.isEmpty(), "Cache should be empty on creation");
        assertEquals(0, cache.size(), "Cache size should be 0 on creation");
    }
    @Test
    void canPutAndGetEntry() {
        SimpleAgedCache cache = new SimpleAgedCache();
        cache.put("foo", 42, 1000);
        assertEquals(42, cache.get("foo"));
    }

    @Test
    void returnsNullForExpiredEntry() throws InterruptedException {
        SimpleAgedCache cache = new SimpleAgedCache();
        cache.put("foo", 42, 10); // 10 ms retention
        Thread.sleep(20);
        assertNull(cache.get("foo"));
    }

    @Test
    void sizeReflectsNumberOfValidEntries() {
        SimpleAgedCache cache = new SimpleAgedCache();
        cache.put("foo", 42, 1000);
        cache.put("bar", 43, 1000);
        assertEquals(2, cache.size());
    }
}