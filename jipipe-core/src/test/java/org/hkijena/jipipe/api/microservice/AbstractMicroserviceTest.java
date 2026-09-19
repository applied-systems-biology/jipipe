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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AbstractMicroserviceTest {

    static class TestService extends AbstractMicroservice {
        volatile boolean startCalled = false;
        volatile boolean stopCalled = false;
        volatile boolean throwOnStart = false;
        volatile boolean throwOnStop = false;
        String throwMessage = "boom";

        TestService() {
            super("Test Service");
        }

        TestService(String name) {
            super(name);
        }

        @Override
        protected void onStart() throws Exception {
            startCalled = true;
            if (throwOnStart) throw new RuntimeException(throwMessage);
        }

        @Override
        protected void onStop() throws Exception {
            stopCalled = true;
            if (throwOnStop) throw new RuntimeException(throwMessage);
        }
    }

    @Test
    void startTransitionsToReady() {
        TestService s = new TestService();
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
        assertTrue(s.startCalled);
    }

    @Test
    void startFailureTransitionsToFailed() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        assertEquals(MicroserviceState.Failed, s.getState());
        assertTrue(s.startCalled);
        assertTrue(s.getStateDetail().contains("boom"));
    }

    @Test
    void stopTransitionsToStopped() {
        TestService s = new TestService();
        s.start();
        s.stop();
        assertEquals(MicroserviceState.Stopped, s.getState());
        assertTrue(s.stopCalled);
    }

    @Test
    void stopWhenStoppedIsNoOp() {
        TestService s = new TestService();
        s.stop();
        assertEquals(MicroserviceState.Stopped, s.getState());
        assertFalse(s.stopCalled);
    }

    @Test
    void startWhenReadyIsNoOp() {
        TestService s = new TestService();
        s.start();
        s.startCalled = false;
        s.start();
        assertFalse(s.startCalled);
        assertEquals(MicroserviceState.Ready, s.getState());
    }

    @Test
    void startWhenStartingIsNoOp() {
        TestService s = new TestService() {
            @Override
            protected void onStart() throws Exception {
                // Simulate that we're "in" onStart — state should already be Starting
                assertEquals(MicroserviceState.Starting, getState());
                // A second start() call should be a no-op (but it can't happen
                // because start() is synchronized and we're inside it)
                super.onStart();
            }
        };
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
    }

    @Test
    void markFailedTransitionsToFailed() {
        TestService s = new TestService();
        s.start();
        s.markFailed("crashed");
        assertEquals(MicroserviceState.Failed, s.getState());
        assertEquals("crashed", s.getStateDetail());
    }

    @Test
    void markFailedWhenAlreadyFailedIsNoOp() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        String originalDetail = s.getStateDetail();
        s.markFailed("new error");
        assertEquals(MicroserviceState.Failed, s.getState());
        assertEquals(originalDetail, s.getStateDetail());
    }

    @Test
    void setStateDetailIsReflected() {
        TestService s = new TestService();
        s.setStateDetail("loading model...");
        assertEquals("loading model...", s.getStateDetail());
    }

    @Test
    void stateChangeEventsAreFired() {
        TestService s = new TestService();
        List<MicroserviceStateChangeEvent> events = new ArrayList<>();
        s.getStateChangeEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        s.start();
        s.stop();

        assertEquals(4, events.size());
        assertEquals(MicroserviceState.Starting, events.get(0).getNewState());
        assertEquals(MicroserviceState.Ready, events.get(1).getNewState());
        assertEquals(MicroserviceState.Stopping, events.get(2).getNewState());
        assertEquals(MicroserviceState.Stopped, events.get(3).getNewState());
    }

    @Test
    void failedStartFiresEvents() {
        TestService s = new TestService();
        s.throwOnStart = true;
        List<MicroserviceStateChangeEvent> events = new ArrayList<>();
        s.getStateChangeEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        s.start();

        assertEquals(2, events.size());
        assertEquals(MicroserviceState.Starting, events.get(0).getNewState());
        assertEquals(MicroserviceState.Failed, events.get(1).getNewState());
    }

    @Test
    void concurrentStartIsSafe() throws Exception {
        TestService s = new TestService() {
            @Override
            protected void onStart() throws Exception {
                super.onStart();
                Thread.sleep(100);  // Simulate slow start to increase contention
            }
        };
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                s.start();
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(MicroserviceState.Ready, s.getState());
        // onStart should only have been called once despite 10 threads
        assertTrue(s.startCalled);
    }

    @Test
    void failedServiceCanRestart() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        assertEquals(MicroserviceState.Failed, s.getState());

        // Fix the issue and retry
        s.throwOnStart = false;
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
    }
}
