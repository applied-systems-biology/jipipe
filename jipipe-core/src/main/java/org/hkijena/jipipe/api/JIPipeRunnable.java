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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.ui.running.JIPipeRunWorker;

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
     * Determines if this run is being logged silently
     * @return if logs are silent
     */
    default boolean isLogSilent() {
        return false;
    }

    /**
     * Called when this runnable finishes
     *
     * @param event the event
     */
    default void onFinished(FinishedEvent event) {

    }

    /**
     * Called when this runnable is interrupted
     *
     * @param event the event
     */
    default void onInterrupted(InterruptedEvent event) {

    }

    interface StartedEventListener {
        void onRunnableStarted(StartedEvent event);
    }

    interface FinishedEventListener {
        void onRunnableFinished(FinishedEvent event);
    }

    interface EnqeuedEventListener {
        void onRunnableEnqueued(EnqueuedEvent event);
    }

    interface InterruptedEventListener {
        void onRunnableInterrupted(InterruptedEvent event);
    }

    interface ProgressEventListener {
        void onRunnableProgress(ProgressEvent event);
    }

    /**
     * Generated when an {@link JIPipeRunWorker} was started
     */
    class StartedEvent extends AbstractJIPipeEvent {

        private JIPipeRunnable run;
        private JIPipeRunWorker worker;

        /**
         * @param run    the run
         * @param worker the worker
         */
        public StartedEvent(JIPipeRunnable run, JIPipeRunWorker worker) {
            super(worker);
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

    class StartedEventEmitter extends JIPipeEventEmitter<StartedEvent, StartedEventListener> {

        @Override
        protected void call(StartedEventListener startedEventListener, StartedEvent event) {
            startedEventListener.onRunnableStarted(event);
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} finished its work
     */
    class FinishedEvent extends AbstractJIPipeEvent {

        private JIPipeRunWorker worker;

        /**
         * @param worker worker that finished
         */
        public FinishedEvent(JIPipeRunWorker worker) {
            super(worker);
            this.worker = worker;
        }

        public JIPipeRunWorker getWorker() {
            return worker;
        }

        public JIPipeRunnable getRun() {
            return worker.getRun();
        }
    }

    class FinishedEventEmitter extends JIPipeEventEmitter<FinishedEvent, FinishedEventListener> {

        @Override
        protected void call(FinishedEventListener finishedEventListener, FinishedEvent event) {
            finishedEventListener.onRunnableFinished(event);
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} was enqueued
     */
    class EnqueuedEvent extends AbstractJIPipeEvent {

        private JIPipeRunnable run;
        private JIPipeRunWorker worker;

        /**
         * @param run    the run
         * @param worker the worker
         */
        public EnqueuedEvent(JIPipeRunnable run, JIPipeRunWorker worker) {
            super(worker);
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

    class EnqueuedEventEmitter extends JIPipeEventEmitter<EnqueuedEvent, EnqeuedEventListener> {

        @Override
        protected void call(EnqeuedEventListener enqeuedEventListener, EnqueuedEvent event) {
            enqeuedEventListener.onRunnableEnqueued(event);
        }
    }

    /**
     * Generated when work of an {@link JIPipeRunWorker} is interrupted
     */
    class InterruptedEvent extends AbstractJIPipeEvent {

        private Throwable exception;
        private JIPipeRunWorker worker;

        /**
         * @param worker    the worker
         * @param exception the exception triggered when interrupted
         */
        public InterruptedEvent(JIPipeRunWorker worker, Throwable exception) {
            super(worker);
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

    class InterruptedEventEmitter extends JIPipeEventEmitter<InterruptedEvent, InterruptedEventListener> {

        @Override
        protected void call(InterruptedEventListener interruptedEventListener, InterruptedEvent event) {
            interruptedEventListener.onRunnableInterrupted(event);
        }
    }

    /**
     * Generated when an {@link JIPipeRunWorker} reports progress
     */
    class ProgressEvent extends AbstractJIPipeEvent {
        private final JIPipeRunWorker worker;
        private final JIPipeProgressInfo.StatusUpdatedEvent status;

        /**
         * @param worker the worker
         * @param status the status
         */
        public ProgressEvent(JIPipeRunWorker worker, JIPipeProgressInfo.StatusUpdatedEvent status) {
            super(worker);
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

    class ProgressEventEmitter extends JIPipeEventEmitter<ProgressEvent, ProgressEventListener> {

        @Override
        protected void call(ProgressEventListener progressEventListener, ProgressEvent event) {
            progressEventListener.onRunnableProgress(event);
        }
    }
}
