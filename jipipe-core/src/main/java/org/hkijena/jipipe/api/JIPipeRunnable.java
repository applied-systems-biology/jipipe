/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.running.JIPipeRunWorker;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import java.util.function.Consumer;

/**
 * Runnable that can be scheduled, canceled, and reports progress
 */
public interface JIPipeRunnable extends Runnable {

    /**
     * The info object that allows communication with the run
     *
     * @return the info object
     */
    JIPipeProgressInfo getProgressInfo();

    /**
     * Sets the progress info of this runnable
     *
     * @param progressInfo the info object
     */
    void setProgressInfo(JIPipeProgressInfo progressInfo);

    /**
     * A name for the runnable
     *
     * @return the name
     */
    String getTaskLabel();

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run finishes
     *
     * @param function the method to execute when the run finishes (successfully or not successfully)
     */
    default void onFinished(Consumer<FinishedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerFinished(FinishedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is interrupted
     *
     * @param function the method to execute when the run is interrupted (due to error or user cancel)
     */
    default void onInterrupted(Consumer<InterruptedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerInterrupted(InterruptedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is started
     *
     * @param function the method to execute when the run starts
     */
    default void onStarted(Consumer<StartedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerStarted(StartedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is progressing
     *
     * @param function the method to execute when the run progresses
     */
    default void onProgress(Consumer<ProgressEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerStarted(ProgressEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Generated when an {@link JIPipeRunWorker} was started
     */
    class StartedEvent {

        private JIPipeRunnable run;
        private JIPipeRunWorker worker;

        /**
         * @param run    the run
         * @param worker the worker
         */
        public StartedEvent(JIPipeRunnable run, JIPipeRunWorker worker) {
            this.run = run;
            this.worker = worker;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public JIPipeRunnable getRun() {
            return run;
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} finished its work
     */
    class FinishedEvent {

        private JIPipeRunWorker worker;

        /**
         * @param worker worker that finished
         */
        public FinishedEvent(JIPipeRunWorker worker) {
            this.worker = worker;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public JIPipeRunnable getRun() {
            return worker.getRun();
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} was enqueued
     */
    class EnqueuedEvent {

        private JIPipeRunnable run;
        private JIPipeRunWorker worker;

        /**
         * @param run    the run
         * @param worker the worker
         */
        public EnqueuedEvent(JIPipeRunnable run, JIPipeRunWorker worker) {
            this.run = run;
            this.worker = worker;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public JIPipeRunnable getRun() {
            return run;
        }
    }

    /**
     * Generated when work of an {@link JIPipeRunWorker} is interrupted
     */
    class InterruptedEvent {

        private Throwable exception;
        private JIPipeRunWorker worker;

        /**
         * @param worker    the worker
         * @param exception the exception triggered when interrupted
         */
        public InterruptedEvent(JIPipeRunWorker worker, Throwable exception) {
            this.exception = exception;
            this.worker = worker;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public Throwable getException() {
            return exception;
        }

        public JIPipeRunnable getRun() {
            return worker.getRun();
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} reports progress
     */
    class ProgressEvent {
        private final JIPipeRunWorker worker;
        private final JIPipeProgressInfo.StatusUpdatedEvent status;

        /**
         * @param worker the worker
         * @param status the status
         */
        public ProgressEvent(JIPipeRunWorker worker, JIPipeProgressInfo.StatusUpdatedEvent status) {
            this.worker = worker;
            this.status = status;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public JIPipeRunnable getRun() {
            return worker.getRun();
        }

        public JIPipeProgressInfo.StatusUpdatedEvent getStatus() {
            return status;
        }
    }
}
