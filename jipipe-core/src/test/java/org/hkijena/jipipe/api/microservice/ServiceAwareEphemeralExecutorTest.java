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

class ServiceAwareEphemeralExecutorTest {

    static class TestService extends AbstractMicroservice {
        TestService() { super("Test"); }
        @Override
        protected void onStart() throws Exception { }
    }

    @Test
    void newTaskCancelsPrevious() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch firstStarted = new CountDownLatch(1);
        AtomicInteger firstCompleted = new AtomicInteger(0);

        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "First"; }
            @Override
            public void run() {
                firstStarted.countDown();
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                firstCompleted.incrementAndGet();
            }
        });

        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

        // Enqueue second task — should cancel the first
        CountDownLatch secondDone = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Second"; }
            @Override
            public void run() {
                secondDone.countDown();
            }
        });

        assertTrue(secondDone.await(5, TimeUnit.SECONDS));
        assertEquals(0, firstCompleted.get(), "First task should have been cancelled before completing");
    }

    @Test
    void taskDoesNotExecuteWhenServiceNotReady() throws Exception {
        TestService service = new TestService();
        // Do NOT start
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void taskExecutesWhenServiceBecomesReady() throws Exception {
        TestService service = new TestService();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        assertFalse(latch.await(1, TimeUnit.SECONDS));

        service.start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void cancelAllClearsHeldTask() throws Exception {
        TestService service = new TestService();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        exec.cancelAll();
        service.start();

        assertFalse(latch.await(2, TimeUnit.SECONDS));
        assertTrue(exec.isEmpty());
    }
}
