package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerStartedEvent;

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
            eventBus.post(new RunUIWorkerStartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
            currentlyRunningWorker.execute();
        }
    }

    public void cancel(ACAQRun run) {
        if(run == null)
            return;
        ACAQRunWorker worker = findWorkerOf(run);
        if(worker != null) {
            if(currentlyRunningWorker == worker) {
                worker.cancel(true);
            }
            else {
                queue.remove(worker);
                eventBus.post(new RunUIWorkerInterruptedEvent(worker, new InterruptedException("Operation was cancelled.")));
            }
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
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
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

    public ACAQRun getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }
}
