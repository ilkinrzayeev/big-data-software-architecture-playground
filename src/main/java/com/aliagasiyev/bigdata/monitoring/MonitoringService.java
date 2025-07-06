package com.aliagasiyev.bigdata.monitoring;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MonitoringService {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong lastJobRun = new AtomicLong(0);
    private final Object jobLock = new Object();

    public void runBackgroundJob() {
        synchronized (jobLock) {
            lastJobRun.set(System.currentTimeMillis());
        }
    }

    public void recordRequest(boolean success) {
        totalRequests.incrementAndGet();
        if (!success) failedRequests.incrementAndGet();
    }

    public double getAvailability() {
        int total = totalRequests.get();
        if (total == 0) return 100.0;
        int failed = failedRequests.get();
        return 100.0 * (total - failed) / total;
    }

    public long getLastJobRun() {
        return lastJobRun.get();
    }

    public void reset() {
        totalRequests.set(0);
        failedRequests.set(0);
        lastJobRun.set(0);
    }
}