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

package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cancellation contract the in-window project loading relies on
 * (feature #1359): cancelling a RUNNING run still delivers exactly one
 * completion event on the {@link JIPipeRunnableWorker}'s own emitters.
 * <p>
 * The worker's done() always emits (finished, exception-interrupted, or
 * cancelled-interrupted) - unlike the queue-level emitters, which receive
 * nothing for a run that was cancelled while running: the executor
 * unregisters itself from the worker inside cancel(), before done() emits,
 * so the forwarding to the queue listeners is gone. The project window's
 * completion handling therefore awaits events at BOTH levels (deduplicated).
 * Headless-safe: the executor is exercised directly, no Swing window.
 */
public class JIPipeRunnableWorkerCancellationTest {

    @Test
    public void cancellingRunningRunEmitsInterruptedOnWorkerEmitter() throws Exception {
        JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Test queue");
        queue.setSilent(true);
        CountDownLatch started = new CountDownLatch(1);
        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Cancellable test run";
            }

            @Override
            public void run() {
                started.countDown();
                try {
                    // Hold until cancel(true) interrupts this worker thread
                    new CountDownLatch(1).await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // Expected: cancel(true) interrupts the worker thread
                }
            }
        };

        JIPipeRunnableWorker worker = queue.enqueue(run);
        CountDownLatch interrupted = new CountDownLatch(1);
        worker.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                interrupted.countDown();
            }
        });

        assertTrue(started.await(30, TimeUnit.SECONDS), "The run should start");
        queue.cancel(run);
        assertTrue(interrupted.await(30, TimeUnit.SECONDS),
                "Cancelling a running run must still deliver the InterruptedEvent on the worker's emitter");
    }

    @Test
    public void cancellingRunningRunEmitsInterruptedOnQueueEmitter() throws Exception {
        JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Test queue");
        queue.setSilent(true);
        CountDownLatch started = new CountDownLatch(1);
        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Cancellable test run";
            }

            @Override
            public void run() {
                started.countDown();
                try {
                    // Hold until cancel(true) interrupts this worker thread
                    new CountDownLatch(1).await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // Expected: cancel(true) interrupts the worker thread
                }
            }
        };

        queue.enqueue(run);
        CountDownLatch interrupted = new CountDownLatch(1);
        queue.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                interrupted.countDown();
            }
        });

        assertTrue(started.await(30, TimeUnit.SECONDS), "The run should start");
        queue.cancel(run);
        assertTrue(interrupted.await(10, TimeUnit.SECONDS),
                "Cancelling a running run must deliver the InterruptedEvent on the queue's emitters "
                        + "(the modal run dialogs and other queue listeners depend on this)");
    }

    @Test
    public void normalFinishEmitsFinishedOnWorkerEmitter() throws Exception {
        JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Test queue");
        queue.setSilent(true);
        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Finishing test run";
            }

            @Override
            public void run() {
                // Nothing to do
            }
        };

        JIPipeRunnableWorker worker = queue.enqueue(run);
        CountDownLatch finished = new CountDownLatch(1);
        worker.getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                finished.countDown();
            }
        });

        assertTrue(finished.await(30, TimeUnit.SECONDS),
                "A normally finishing run must deliver the FinishedEvent on the worker's emitter");
    }
}
