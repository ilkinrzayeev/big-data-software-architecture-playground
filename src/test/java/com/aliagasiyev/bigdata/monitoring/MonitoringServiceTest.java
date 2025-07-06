package com.aliagasiyev.bigdata.monitoring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class MonitoringServiceTest {

    @Test
    void initialAvailabilityIs100Percent() {
        MonitoringService service = new MonitoringService();
        assertEquals(100.0, service.getAvailability());
    }

    @Test
    void recordsRequestsAndCalculatesAvailability() {
        MonitoringService service = new MonitoringService();
        service.recordRequest(true);
        service.recordRequest(false);
        service.recordRequest(true);
        assertEquals(66.66666666666666, service.getAvailability(), 0.0001);
    }

    @Test
    void backgroundJobUpdatesLastRun() throws InterruptedException {
        MonitoringService service = new MonitoringService();
        long before = System.currentTimeMillis();
        Thread.sleep(10);
        service.runBackgroundJob();
        long after = service.getLastJobRun();
        assertTrue(after >= before);
    }

    @Test
    void resetClearsAllMetrics() {
        MonitoringService service = new MonitoringService();
        service.recordRequest(false);
        service.runBackgroundJob();
        service.reset();
        assertEquals(100.0, service.getAvailability());
        assertEquals(0, service.getLastJobRun());
    }

    @Test
    void handlesHighLoadConcurrently() throws InterruptedException {
        MonitoringService service = new MonitoringService();
        int threads = 20;
        int ops = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger(0);

        Runnable task = () -> {
            for (int i = 0; i < ops; i++) {
                boolean success = (i % 10 != 0);
                service.recordRequest(success);
                if (!success) failures.incrementAndGet();
            }
            latch.countDown();
        };
        for (int t = 0; t < threads; t++) new Thread(task).start();
        latch.await();
        int total = threads * ops;
        int failed = failures.get();
        double expected = 100.0 * (total - failed) / total;
        assertEquals(expected, service.getAvailability(), 0.0001);
    }

    @Test
    void backgroundJobIsThreadSafe() throws InterruptedException {
        MonitoringService service = new MonitoringService();
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        Runnable job = () -> {
            for (int i = 0; i < 100; i++) {
                service.runBackgroundJob();
            }
            latch.countDown();
        };
        for (int t = 0; t < threads; t++) new Thread(job).start();
        latch.await();
        assertTrue(service.getLastJobRun() > 0);
    }

    @Test
    void availabilityNeverBelowZeroOrAbove100() {
        MonitoringService service = new MonitoringService();
        for (int i = 0; i < 100; i++) service.recordRequest(false);
        assertEquals(0.0, service.getAvailability());
        service.reset();
        for (int i = 0; i < 100; i++) service.recordRequest(true);
        assertEquals(100.0, service.getAvailability());
    }

    @Test
    void handlesNoRequestsGracefully() {
        MonitoringService service = new MonitoringService();
        assertEquals(100.0, service.getAvailability());
    }

    @Test
    void stressTestMetricsAccuracy() {
        MonitoringService service = new MonitoringService();
        int n = 10000;
        for (int i = 0; i < n; i++) {
            service.recordRequest(i % 2 == 0);
        }
        assertEquals(50.0, service.getAvailability(), 0.0001);
    }
}