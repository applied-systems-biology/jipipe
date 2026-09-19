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

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Predicate;

import javax.swing.SwingWorker;

/**
 * Latest-wins {@link JIPipeRunnableExecutor} that gates on {@link Microservice#isReady()}.
 *
 * <p>Only one task is held at a time. Enqueuing a new task cancels any previous
 * running or held task. If the service is not Ready, the task is held but not
 * executed until the service becomes Ready. If a newer task arrives before the
 * service is Ready, the held task is cancelled and replaced.</p>
 */
public class ServiceAwareEphemeralExecutor implements JIPipeRunnableExecutor,
        MicroserviceStateChangeListener {

    private final String name;
    private final Microservice service;
    private volatile JIPipeRunnableWorker currentWorker = null;
    private volatile boolean silent = false;

    private final JIPipeRunnable.EnqueuedEventEmitter enqueuedEventEmitter = new JIPipeRunnable.EnqueuedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();
    private final JIPipeRunnable.StartedEventEmitter startedEventEmitter = new JIPipeRunnable.StartedEventEmitter();

    public ServiceAwareEphemeralExecutor(String name, Microservice service) {
        this.name = name;
        this.service = service;
        service.getStateChangeEventEmitter().subscribe(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JIPipeRunnableWorker enqueue(JIPipeRunnable run) {
        // Cancel previous worker if running
        JIPipeRunnableWorker previous = currentWorker;
        if (previous != null) {
            previous.getRun().getProgressInfo().cancel();
            previous.cancel(true);
        }

        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, silent);
        currentWorker = worker;

        worker.getFinishedEventEmitter().subscribe(this::onWorkerFinished);
        worker.getInterruptedEventEmitter().subscribe(this::onWorkerInterrupted);
        worker.getProgressEventEmitter().subscribe(this::onWorkerProgress);

        enqueuedEventEmitter.emit(new JIPipeRunnable.EnqueuedEvent(run, worker));

        // Only execute if service is ready
        if (service.isReady()) {
            startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(run, worker));
            worker.execute();
        }

        return worker;
    }

    private void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        currentWorker = null;
        finishedEventEmitter.emit(event);
    }

    private void onWorkerInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        currentWorker = null;
        interruptedEventEmitter.emit(event);
    }

    private void onWorkerProgress(JIPipeRunnable.ProgressEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        progressEventEmitter.emit(event);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        if (event.getNewState() == MicroserviceState.Ready) {
            // If we have a held worker that hasn't been executed, dispatch it now
            JIPipeRunnableWorker worker = currentWorker;
            if (worker != null && !worker.isDone()) {
                // Check if it hasn't been started yet (state is PENDING)
                if (worker.getState() == SwingWorker.StateValue.PENDING) {
                    startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(worker.getRun(), worker));
                    worker.execute();
                }
            }
        }
    }

    @Override
    public void cancelAll() {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null) {
            worker.getRun().getProgressInfo().cancel();
            worker.cancel(true);
            currentWorker = null;
            interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                    new InterruptedException("Operation was cancelled.")));
        }
    }

    @Override
    public void cancel(JIPipeRunnable run) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && worker.getRun() == run) {
            worker.getRun().getProgressInfo().cancel();
            worker.cancel(true);
            currentWorker = null;
            interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                    new InterruptedException("Operation was cancelled.")));
        }
    }

    @Override
    public void cancelIf(Predicate<JIPipeRunnable> predicate) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && predicate.test(worker.getRun())) {
            cancel(worker.getRun());
        }
    }

    @Override
    public boolean isEmpty() {
        return currentWorker == null;
    }

    @Override
    public int size() {
        return currentWorker != null ? 1 : 0;
    }

    @Override
    public boolean isRunningOrEnqueued(JIPipeRunnable runnable) {
        JIPipeRunnableWorker worker = currentWorker;
        return worker != null && worker.getRun() == runnable;
    }

    @Override
    public JIPipeRunnable getCurrentRun() {
        JIPipeRunnableWorker worker = currentWorker;
        return worker != null ? worker.getRun() : null;
    }

    @Override
    public JIPipeRunnableWorker getCurrentRunWorker() {
        return currentWorker;
    }

    @Override
    public JIPipeRunnableWorker findWorkerOf(JIPipeRunnable run) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && worker.getRun() == run) {
            return worker;
        }
        return null;
    }

    @Override
    public Queue<JIPipeRunnableWorker> getQueue() {
        return new ArrayDeque<>();
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public JIPipeRunnable.EnqueuedEventEmitter getEnqueuedEventEmitter() {
        return enqueuedEventEmitter;
    }

    @Override
    public JIPipeRunnable.FinishedEventEmitter getFinishedEventEmitter() {
        return finishedEventEmitter;
    }

    @Override
    public JIPipeRunnable.InterruptedEventEmitter getInterruptedEventEmitter() {
        return interruptedEventEmitter;
    }

    @Override
    public JIPipeRunnable.ProgressEventEmitter getProgressEventEmitter() {
        return progressEventEmitter;
    }

    @Override
    public JIPipeRunnable.StartedEventEmitter getStartedEventEmitter() {
        return startedEventEmitter;
    }
}
