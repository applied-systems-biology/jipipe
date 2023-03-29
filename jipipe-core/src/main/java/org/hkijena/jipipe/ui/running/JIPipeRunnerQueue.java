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

package org.hkijena.jipipe.ui.running;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.JIPipeRunnable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Queue for {@link JIPipeRunnable}
 */
public class JIPipeRunnerQueue {

    private static JIPipeRunnerQueue instance;

    private final String name;
    private final Queue<JIPipeRunWorker> queue = new ArrayDeque<>();
    private final Map<JIPipeRunnable, JIPipeRunWorker> assignedWorkers = new HashMap<>();
    private final EventBus eventBus = new EventBus();
    private JIPipeRunWorker currentlyRunningWorker = null;

    private boolean silent;

    public JIPipeRunnerQueue(String name) {
        this.name = name;
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeRunnerQueue getInstance() {
        if (instance == null)
            instance = new JIPipeRunnerQueue("");
        return instance;
    }

    /**
     * Determines if a runnable is enqueued or running
     *
     * @param runnable the runnable
     * @return if a runnable is enqueued or running
     */
    public boolean isRunningOrEnqueued(JIPipeRunnable runnable) {
        if (currentlyRunningWorker != null && currentlyRunningWorker.getRun() == runnable)
            return true;
        for (JIPipeRunWorker worker : queue) {
            if (worker.getRun() == runnable)
                return true;
        }

        return false;
    }

    public Queue<JIPipeRunWorker> getQueue() {
        return new ArrayDeque<>(queue);
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * @return true if nothing is running and the queue is empty
     */
    public boolean isEmpty() {
        return currentlyRunningWorker == null && queue.isEmpty();
    }

    /**
     * The size of the queue (includes the currently running {@link JIPipeProjectRun}
     *
     * @return the size
     */
    public int size() {
        return (currentlyRunningWorker != null ? 1 : 0) + queue.size();
    }

    /**
     * Schedules a new runnable
     *
     * @param run The runnable
     * @return The worker associated to the run
     */
    public JIPipeRunWorker enqueue(JIPipeRunnable run) {
        JIPipeRunWorker worker = new JIPipeRunWorker(run, silent);
        worker.getEventBus().register(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
        eventBus.post(new JIPipeRunnable.EnqueuedEvent(run, worker));
        tryDequeue();
        return worker;
    }

    /**
     * Finds the worker associated to the {@link JIPipeRunnable}
     *
     * @param run The runnable
     * @return The associated worker
     */
    public JIPipeRunWorker findWorkerOf(JIPipeRunnable run) {
        return assignedWorkers.getOrDefault(run, null);
    }

    /**
     * Attempts to run a scheduled run
     */
    public void tryDequeue() {
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            currentlyRunningWorker = queue.remove();
            eventBus.post(new JIPipeRunnable.StartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
            currentlyRunningWorker.execute();
        }
    }

    /**
     * Cancels a runnable
     *
     * @param run The runnable
     */
    public void cancel(JIPipeRunnable run) {
        if (run == null)
            return;
        JIPipeRunWorker worker = findWorkerOf(run);
        if (worker != null) {
            if (currentlyRunningWorker == worker) {
                worker.getRun().getProgressInfo().cancel();
                worker.cancel(true);
            } else {
                queue.remove(worker);
                eventBus.post(new JIPipeRunnable.InterruptedEvent(worker, new InterruptedException("Operation was cancelled.")));

                worker.getEventBus().unregister(this);
            }
        }
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();

            event.getWorker().getEventBus().unregister(this);
        }
        eventBus.post(event);
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();

            event.getWorker().getEventBus().unregister(this);
        }
        eventBus.post(event);
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(JIPipeRunnable.ProgressEvent event) {
        eventBus.post(event);
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return The current run
     */
    public JIPipeRunWorker getCurrentRunWorker() {
        return currentlyRunningWorker;
    }

    /**
     * @return The current run
     */
    public JIPipeRunnable getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }

    /**
     * Removes all enqueued (but not running tasks)
     */
    public void clearQueue() {
        for (JIPipeRunWorker worker : ImmutableList.copyOf(queue)) {
            cancel(worker.getRun());
        }
    }

    public void cancelAll() {
        clearQueue();
        if (currentlyRunningWorker != null) {
            cancel(currentlyRunningWorker.getRun());
        }
    }

    public void cancelIf(Predicate<JIPipeRunnable> predicate) {
        for (JIPipeRunWorker toCancel : queue.stream().filter(rw -> predicate.test(rw.getRun())).collect(Collectors.toList())) {
            cancel(toCancel.getRun());
        }
        if (currentlyRunningWorker != null && !currentlyRunningWorker.isDone() && predicate.test(currentlyRunningWorker.getRun())) {
            cancel(currentlyRunningWorker.getRun());
        }
    }
}
