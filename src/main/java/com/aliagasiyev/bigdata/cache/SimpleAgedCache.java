package com.aliagasiyev.bigdata.cache;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * A production-grade, thread-safe aged cache with TTL-based eviction.
 * No built-in collections (List/Map/Set) are used.
 */
public class SimpleAgedCache {
    private final Clock clock;
    private final int capacity;
    private final ExpirableEntry[] table;
    private final Object lock = new Object();

    private static final int DEFAULT_CAPACITY = 128;

    /**
     * Inner class representing a cache entry with expiry.
     */
    static class ExpirableEntry {
        final Object key;
        Object value;
        long expiryTime;
        ExpirableEntry next; // for collision handling (chaining)

        ExpirableEntry(Object key, Object value, long expiryTime) {
            this.key = key;
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }

    public SimpleAgedCache() {
        this(Clock.systemUTC(), DEFAULT_CAPACITY);
    }

    public SimpleAgedCache(Clock clock) {
        this(clock, DEFAULT_CAPACITY);
    }

    public SimpleAgedCache(Clock clock, int capacity) {
        this.clock = clock;
        this.capacity = capacity;
        this.table = new ExpirableEntry[capacity];
    }

    private int hash(Object key) {
        return (key == null ? 0 : Math.abs(key.hashCode())) % capacity;
    }

    public void put(Object key, Object value, int retentionInMillis) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        if (retentionInMillis <= 0) throw new IllegalArgumentException("Retention must be positive");
        long expiry = clock.millis() + retentionInMillis;
        int idx = hash(key);
        synchronized (lock) {
            ExpirableEntry prev = null, curr = table[idx];
            while (curr != null) {
                if (curr.key.equals(key)) {
                    curr.value = value;
                    curr.expiryTime = expiry;
                    return;
                }
                prev = curr;
                curr = curr.next;
            }
            ExpirableEntry entry = new ExpirableEntry(key, value, expiry);
            if (prev == null) {
                table[idx] = entry;
            } else {
                prev.next = entry;
            }
        }
    }

    public Object get(Object key) {
        int idx = hash(key);
        synchronized (lock) {
            ExpirableEntry prev = null, curr = table[idx];
            while (curr != null) {
                if (curr.key.equals(key)) {
                    if (curr.expiryTime < clock.millis()) {
                        // expired, remove
                        if (prev == null) table[idx] = curr.next;
                        else prev.next = curr.next;
                        return null;
                    }
                    return curr.value;
                }
                prev = curr;
                curr = curr.next;
            }
            return null;
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        int count = 0;
        long now = clock.millis();
        synchronized (lock) {
            for (ExpirableEntry entry : table) {
                ExpirableEntry curr = entry;
                while (curr != null) {
                    if (curr.expiryTime >= now) count++;
                    curr = curr.next;
                }
            }
        }
        return count;
    }

    public void evictExpired() {
        long now = clock.millis();
        synchronized (lock) {
            for (int i = 0; i < capacity; i++) {
                ExpirableEntry prev = null, curr = table[i];
                while (curr != null) {
                    if (curr.expiryTime < now) {
                        if (prev == null) table[i] = curr.next;
                        else prev.next = curr.next;
                        curr = (prev == null) ? table[i] : prev.next;
                    } else {
                        prev = curr;
                        curr = curr.next;
                    }
                }
            }
        }
    }
}