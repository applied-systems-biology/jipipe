package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ACAQRunnerQueue {

    private static ACAQRunnerQueue instance;

    private ACAQRunWorker currentlyRunningWorker = null;
    private Queue<ACAQRunWorker> queue = new ArrayDeque<>();
    private Map<ACAQRun, ACAQRunWorker> assignedWorkers = new HashMap<>();
    private EventBus eventBus = new EventBus();

    private ACAQRunnerQueue() {

    }

    public static ACAQRunnerQueue getInstance() {
        if(instance == null)
            instance = new ACAQRunnerQueue();
        return instance;
    }

    public ACAQRunWorker enqueue(ACAQRun run) {
        ACAQRunWorker worker = new ACAQRunWorker(run);
        worker.getEventBus().register(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
        tryDequeue();
        return worker;
    }

    public ACAQRunWorker findWorkerOf(ACAQRun run) {
        return assignedWorkers.getOrDefault(run, null);
    }

    public void tryDequeue() {
        if(currentlyRunningWorker == null && !queue.isEmpty()) {
            currentlyRunningWorker = queue.remove();
            currentlyRunningWorker.execute();
        }
    }

    public void cancel(ACAQRun run) {
        ACAQRunWorker worker = findWorkerOf(run);
        if(currentlyRunningWorker == worker) {
            worker.cancel(true);
        }
        else {
            queue.remove(worker);
            eventBus.post(new RunUIWorkerInterruptedEvent(run, new InterruptedException("Operation was cancelled."), worker));
        }
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if(event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
        }
        eventBus.post(event);
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerFinishedEvent event) {
        if(event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
        }
        eventBus.post(event);
    }

    @Subscribe
    public void onWorkerProgress(RunUIWorkerProgressEvent event) {
        eventBus.post(event);
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
