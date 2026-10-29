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

package org.hkijena.jipipe.api.project.jipx;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The concurrency contract the non-modal JIPX save relies on (feature #753):
 * every project window runs its saves on its own
 * {@link JIPipeQueuedRunnableExecutor} (a per-project queue, NOT the global
 * singleton). The two guarantees this locks in are:
 * <ol>
 *   <li>Runs enqueued on the <b>same</b> executor never overlap — same-project
 *       saves (manual vs. autosave) serialize.</li>
 *   <li>Runs on <b>different</b> executors are independent — a large save in
 *       project A must not stall a save in project B.</li>
 * </ol>
 * The executor is exercised directly (no Swing window), mirroring the
 * production per-project queue. Headless-safe: {@link JIPipeQueuedRunnableExecutor}
 * delegates to {@code SwingWorker}, but neither the run body nor the events
 * touch AWT components (the same pattern as
 * {@code ServiceAwareQueuedExecutorTest} in jipipe-core).
 */
class SaveQueueSerializationTest {

    @Test
    void sameExecutorSerializesTwoRuns() throws InterruptedException {
        JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Save Test Project");

        AtomicInteger active = new AtomicInteger(0);
        AtomicLong maxActive = new AtomicLong(0);
        CountDownLatch finished = new CountDownLatch(2);

        queue.enqueue(new TimedRun("save #1", 600, active, maxActive, finished, null));
        queue.enqueue(new TimedRun("save #2", 600, active, maxActive, finished, null));

        assertTrue(finished.await(10, TimeUnit.SECONDS), "Both enqueued saves must complete");
        // The single-worker queue must never run two saves at the same time.
        assertEquals(1L, maxActive.get(),
                "Two saves on the same per-project queue must never overlap; observed max concurrency: " + maxActive.get());
    }

    @Test
    void differentExecutorsRunIndependently() throws InterruptedException {
        JIPipeQueuedRunnableExecutor queueA = new JIPipeQueuedRunnableExecutor("Save Project A");
        JIPipeQueuedRunnableExecutor queueB = new JIPipeQueuedRunnableExecutor("Save Project B");

        AtomicInteger activeA = new AtomicInteger(0);
        AtomicLong maxActiveA = new AtomicLong(0);
        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch aFinished = new CountDownLatch(1);

        // A long save on project A (2s) — must NOT block project B.
        queueA.enqueue(new TimedRun("save A (long)", 2000, activeA, maxActiveA, aFinished, aStarted));

        // Project B's save must START while A is still running, proving the
        // two per-project queues are independent.
        AtomicInteger activeB = new AtomicInteger(0);
        AtomicLong maxActiveB = new AtomicLong(0);
        CountDownLatch bStarted = new CountDownLatch(1);
        CountDownLatch bFinished = new CountDownLatch(1);
        queueB.enqueue(new TimedRun("save B", 0, activeB, maxActiveB, bFinished, bStarted));

        assertTrue(aStarted.await(2, TimeUnit.SECONDS), "Project A's long save must start");
        // If the queues were shared/serialized, B would only start after A
        // finished (~2s). A short window proves independence.
        assertTrue(bStarted.await(1, TimeUnit.SECONDS),
                "Project B's save must start immediately, not wait for project A's long save");
        assertTrue(aFinished.await(10, TimeUnit.SECONDS), "Project A's save must complete");
        assertTrue(bFinished.await(10, TimeUnit.SECONDS), "Project B's save must complete");
        assertEquals(1L, maxActiveA.get(), "Project A's queue must stay serialized");
    }

    /**
     * A run that signals entry/exit and sleeps so that, if two runs ran
     * concurrently, the overlap would be observable.
     */
    private static class TimedRun extends DefaultJIPipeRunnable {
        private final String label;
        private final long delayMs;
        private final AtomicInteger active;
        private final AtomicLong maxActive;
        private final CountDownLatch finished;
        private final CountDownLatch entered;

        TimedRun(String label, long delayMs, AtomicInteger active, AtomicLong maxActive,
                 CountDownLatch finished, CountDownLatch entered) {
            this.label = label;
            this.delayMs = delayMs;
            this.active = active;
            this.maxActive = maxActive;
            this.finished = finished;
            this.entered = entered;
        }

        @Override
        public String getTaskLabel() {
            return label;
        }

        @Override
        public void run() {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            if (entered != null) {
                entered.countDown();
            }
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            active.decrementAndGet();
            finished.countDown();
        }
    }
}
