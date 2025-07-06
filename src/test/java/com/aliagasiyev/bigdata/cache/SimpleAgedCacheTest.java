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
}