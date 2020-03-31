package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerStartedEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Queue for {@link ACAQRunnable}
 */
public class ACAQRunnerQueue {

    private static ACAQRunnerQueue instance;

    private ACAQRunWorker currentlyRunningWorker = null;
    private Queue<ACAQRunWorker> queue = new ArrayDeque<>();
    private Map<ACAQRunnable, ACAQRunWorker> assignedWorkers = new HashMap<>();
    private EventBus eventBus = new EventBus();

    private ACAQRunnerQueue() {

    }

    /**
     * Schedules a new runnable
     * @param run The runnable
     * @return The worker associated to the run
     */
    public ACAQRunWorker enqueue(ACAQRunnable run) {
        ACAQRunWorker worker = new ACAQRunWorker(run);
        worker.getEventBus().register(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
        tryDequeue();
        return worker;
    }

    /**
     * Finds the worker associated to the {@link ACAQRunnable}
     * @param run The runnable
     * @return The associated worker
     */
    public ACAQRunWorker findWorkerOf(ACAQRunnable run) {
        return assignedWorkers.getOrDefault(run, null);
    }

    /**
     * Attempts to run a scheduled run
     */
    public void tryDequeue() {
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            currentlyRunningWorker = queue.remove();
            eventBus.post(new RunUIWorkerStartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
            currentlyRunningWorker.execute();
        }
    }

    /**
     * Cancels a runnable
     * @param run The runnable
     */
    public void cancel(ACAQRunnable run) {
        if (run == null)
            return;
        ACAQRunWorker worker = findWorkerOf(run);
        if (worker != null) {
            if (currentlyRunningWorker == worker) {
                worker.cancel(true);
            } else {
                queue.remove(worker);
                eventBus.post(new RunUIWorkerInterruptedEvent(worker, new InterruptedException("Operation was cancelled.")));
            }
        }
    }

    /**
     * Triggered when a worker is finished
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
        }
        eventBus.post(event);
    }

    /**
     * Triggered when a worker is interrupted
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
        }
        eventBus.post(event);
    }

    /**
     * Triggered when a worker reports progress
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(RunUIWorkerProgressEvent event) {
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
    public ACAQRunnable getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }

    /**
     * @return Singleton instance
     */
    public static ACAQRunnerQueue getInstance() {
        if (instance == null)
            instance = new ACAQRunnerQueue();
        return instance;
    }
}
