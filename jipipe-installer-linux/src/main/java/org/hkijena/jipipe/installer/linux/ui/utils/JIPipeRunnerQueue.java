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

package org.hkijena.jipipe.installer.linux.ui.utils;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.installer.linux.api.JIPipeRunnable;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.jipipe.installer.linux.ui.events.RunUIWorkerStartedEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Queue for {@link JIPipeRunnable}
 */
public class JIPipeRunnerQueue {

    private static JIPipeRunnerQueue instance;

    private JIPipeRunWorker currentlyRunningWorker = null;
    private Queue<JIPipeRunWorker> queue = new ArrayDeque<>();
    private Map<JIPipeRunnable, JIPipeRunWorker> assignedWorkers = new HashMap<>();
    private EventBus eventBus = new EventBus();

    private JIPipeRunnerQueue() {

    }

    /**
     * @return Singleton instance
     */
    public static JIPipeRunnerQueue getInstance() {
        if (instance == null)
            instance = new JIPipeRunnerQueue();
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

    public boolean isEmpty() {
        return currentlyRunningWorker == null && queue.isEmpty();
    }

    /**
     * Schedules a new runnable
     *
     * @param run The runnable
     * @return The worker associated to the run
     */
    public JIPipeRunWorker enqueue(JIPipeRunnable run) {
        JIPipeRunWorker worker = new JIPipeRunWorker(run);
        worker.getEventBus().register(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
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
            eventBus.post(new RunUIWorkerStartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
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
                worker.cancel(true);
            } else {
                queue.remove(worker);
                eventBus.post(new RunUIWorkerInterruptedEvent(worker, new InterruptedException("Operation was cancelled.")));
            }
        }
    }

    /**
     * Triggered when a worker is finished
     *
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
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        event.getException().printStackTrace();
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
        }
        eventBus.post(event);
    }

    /**
     * Triggered when a worker reports progress
     *
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
    public JIPipeRunnable getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }
}