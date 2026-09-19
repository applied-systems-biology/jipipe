/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAwareQueuedExecutorTest {

    static class TestService extends AbstractMicroservice {
        TestService() { super("Test"); }

        @Override
        protected void onStart() throws Exception { }
    }

    static class CountingRun extends DefaultJIPipeRunnable {
        final CountDownLatch latch;
        final AtomicInteger counter;
        final long delayMs;

        CountingRun(CountDownLatch latch, AtomicInteger counter, long delayMs) {
            this.latch = latch;
            this.counter = counter;
            this.delayMs = delayMs;
        }

        @Override
        public String getTaskLabel() { return "Counting"; }

        @Override
        public void run() {
            counter.incrementAndGet();
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            latch.countDown();
        }
    }

    @Test
    void tasksExecuteWhenServiceReady() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }

    @Test
    void tasksDoNotExecuteWhenServiceNotReady() throws Exception {
        TestService service = new TestService();
        // Do NOT start the service
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));

        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, counter.get());
    }

    @Test
    void tasksExecuteWhenServiceBecomesReady() throws Exception {
        TestService service = new TestService();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));

        // Service not ready — tasks should not execute
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, counter.get());

        // Start service — tasks should execute
        service.start();
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(2, counter.get());
    }

    @Test
    void cancelAllRemovesPendingTasks() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        // Enqueue a long-running task to block the queue
        CountDownLatch blockLatch = new CountDownLatch(1);
        exec.enqueue(new CountingRun(blockLatch, new AtomicInteger(0), 10000));

        // Enqueue tasks that should never run
        CountDownLatch pendingLatch = new CountDownLatch(2);
        AtomicInteger pendingCounter = new AtomicInteger(0);
        exec.enqueue(new CountingRun(pendingLatch, pendingCounter, 0));
        exec.enqueue(new CountingRun(pendingLatch, pendingCounter, 0));

        exec.cancelAll();

        assertFalse(pendingLatch.await(2, TimeUnit.SECONDS));
        assertEquals(0, pendingCounter.get());
    }

    @Test
    void tasksExecuteInFifoOrder() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        AtomicInteger order = new AtomicInteger(0);
        int[] results = new int[5];
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            exec.enqueue(new DefaultJIPipeRunnable() {
                @Override
                public String getTaskLabel() { return "Task " + idx; }

                @Override
                public void run() {
                    results[idx] = order.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        // Tasks should execute in order 1,2,3,4,5
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, results[i]);
        }
    }
}
