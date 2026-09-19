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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * FIFO {@link JIPipeRunnableExecutor} that only dispatches work when the
 * associated {@link Microservice} is in the {@link MicroserviceState#Ready} state.
 *
 * <p>Tasks enqueued while the service is not Ready pile up in the queue.
 * When the service transitions to Ready, the executor's state-change listener
 * triggers dispatch.</p>
 *
 * <p>Cancellation removes pending tasks from the queue before they start —
 * no orphan tasks remain. Lifecycle operations (start/stop) are handled by
 * the {@link Microservice}, NOT as queue tasks.</p>
 */
public class ServiceAwareQueuedExecutor implements JIPipeRunnableExecutor,
        JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener,
        JIPipeRunnable.ProgressEventListener, MicroserviceStateChangeListener {

    private final String name;
    private final Microservice service;
    private final Queue<JIPipeRunnableWorker> queue = new ArrayDeque<>();
    private final Map<JIPipeRunnable, JIPipeRunnableWorker> assignedWorkers = new HashMap<>();
    private final JIPipeRunnable.EnqueuedEventEmitter enqueuedEventEmitter = new JIPipeRunnable.EnqueuedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();
    private final JIPipeRunnable.StartedEventEmitter startedEventEmitter = new JIPipeRunnable.StartedEventEmitter();
    private JIPipeRunnableWorker currentlyRunningWorker = null;
    private boolean silent;

    public ServiceAwareQueuedExecutor(String name, Microservice service) {
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
        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, silent);
        worker.getFinishedEventEmitter().subscribe(this);
        worker.getInterruptedEventEmitter().subscribe(this);
        worker.getProgressEventEmitter().subscribe(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
        enqueuedEventEmitter.emit(new JIPipeRunnable.EnqueuedEvent(run, worker));
        tryDequeue();
        return worker;
    }

    private void tryDequeue() {
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            if (!service.isReady()) {
                return;
            }
            currentlyRunningWorker = queue.remove();
            startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
            currentlyRunningWorker.execute();
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
            unregisterWorkerEvents(event.getWorker());
            event.getRun().onFinished(event);
        }
        finishedEventEmitter.emit(event);
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            tryDequeue();
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
            unregisterWorkerEvents(event.getWorker());
            event.getRun().onInterrupted(event);
        }
        interruptedEventEmitter.emit(event);
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            tryDequeue();
        }
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        progressEventEmitter.emit(event);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        if (event.getNewState() == MicroserviceState.Ready) {
            tryDequeue();
        }
    }

    private void registerWorkerEvents(JIPipeRunnableWorker worker) {
        worker.getFinishedEventEmitter().subscribe(this);
        worker.getInterruptedEventEmitter().subscribe(this);
        worker.getProgressEventEmitter().subscribe(this);
    }

    private void unregisterWorkerEvents(JIPipeRunnableWorker worker) {
        worker.getFinishedEventEmitter().unsubscribe(this);
        worker.getInterruptedEventEmitter().unsubscribe(this);
        worker.getProgressEventEmitter().unsubscribe(this);
    }

    @Override
    public boolean isRunningOrEnqueued(JIPipeRunnable runnable) {
        if (currentlyRunningWorker != null && currentlyRunningWorker.getRun() == runnable)
            return true;
        for (JIPipeRunnableWorker worker : queue) {
            if (worker.getRun() == runnable)
                return true;
        }
        return false;
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

    @Override
    public Queue<JIPipeRunnableWorker> getQueue() {
        return new ArrayDeque<>(queue);
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
    public boolean isEmpty() {
        return currentlyRunningWorker == null && queue.isEmpty();
    }

    @Override
    public int size() {
        return (currentlyRunningWorker != null ? 1 : 0) + queue.size();
    }

    @Override
    public JIPipeRunnableWorker findWorkerOf(JIPipeRunnable run) {
        return assignedWorkers.getOrDefault(run, null);
    }

    @Override
    public JIPipeRunnableWorker getCurrentRunWorker() {
        return currentlyRunningWorker;
    }

    @Override
    public JIPipeRunnable getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }

    @Override
    public void cancel(JIPipeRunnable run) {
        if (run == null) return;
        JIPipeRunnableWorker worker = findWorkerOf(run);
        if (worker != null) {
            if (currentlyRunningWorker == worker) {
                worker.getRun().getProgressInfo().cancel();
                worker.cancel(true);
                assignedWorkers.remove(worker.getRun());
                currentlyRunningWorker = null;
                tryDequeue();
                unregisterWorkerEvents(worker);
            } else {
                queue.remove(worker);
                interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                        new InterruptedException("Operation was cancelled.")));
                unregisterWorkerEvents(worker);
            }
        }
    }

    @Override
    public void cancelAll() {
        for (JIPipeRunnableWorker worker : ImmutableList.copyOf(queue)) {
            cancel(worker.getRun());
        }
        if (currentlyRunningWorker != null) {
            cancel(currentlyRunningWorker.getRun());
        }
    }

    @Override
    public void cancelIf(Predicate<JIPipeRunnable> predicate) {
        for (JIPipeRunnableWorker toCancel : queue.stream().filter(rw -> predicate.test(rw.getRun())).collect(Collectors.toList())) {
            cancel(toCancel.getRun());
        }
        if (currentlyRunningWorker != null && !currentlyRunningWorker.isDone() && predicate.test(currentlyRunningWorker.getRun())) {
            cancel(currentlyRunningWorker.getRun());
        }
    }
}
