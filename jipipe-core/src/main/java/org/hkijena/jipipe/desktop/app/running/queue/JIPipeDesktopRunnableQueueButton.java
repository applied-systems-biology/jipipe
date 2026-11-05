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

package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeUtils;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopRunnableQueueSpinnerIcon;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeDesktopRunnableQueueButton extends JButton implements JIPipeDesktopWorkbenchAccess, JIPipeRunnable.StartedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final JIPipeRunnableExecutor runnerQueue;
    private final JPopupMenu menu = new JPopupMenu();
    private boolean processAlreadyQueued;
    private JIPipeDesktopRunnableQueueSpinnerIcon throbberIcon;
    private boolean showProgress;
    private int lastProgress;
    private int lastMaxProgress;
    private boolean flatMode = false;
    private String readyLabel = "Ready";
    private String tasksFinishedLabel = "All tasks finished";
    private String taskSingleRunningLabel = "1 task running";

    private String taskSingleEnqueuedRunningLabel = "1 task running (+ %d enqueued)";

    /**
     * The node whose state is displayed (the latest node with a progress update among the batches'
     * changed nodes) and the store it belongs to. Written by batch listeners (writer thread), read
     * by the refresh timer (EDT).
     */
    private volatile JIPipeProgressLog trackedStore;
    private volatile int trackedNodeId;
    private volatile boolean progressDirty;
    private final Timer refreshTimer = new Timer(500, e -> refreshIfDirty());

    public JIPipeDesktopRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench) {
        this(desktopWorkbench, JIPipeQueuedRunnableExecutor.getInstance());
    }

    public JIPipeDesktopRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableExecutor runnerQueue) {
        this.desktopWorkbench = desktopWorkbench;
        this.runnerQueue = runnerQueue;
        initialize();
        updateStatus();

        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        runnerQueue.getProgressEventEmitter().subscribeWeak(this);
        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getEnqueuedEventEmitter().subscribeWeak(this);
        refreshTimer.start();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return desktopWorkbench;
    }

    public String getTaskSingleRunningLabel() {
        return taskSingleRunningLabel;
    }

    public void setTaskSingleRunningLabel(String taskSingleRunningLabel) {
        this.taskSingleRunningLabel = taskSingleRunningLabel;
        updateStatus();
    }

    public String getTaskSingleEnqueuedRunningLabel() {
        return taskSingleEnqueuedRunningLabel;
    }

    public void setTaskSingleEnqueuedRunningLabel(String taskSingleEnqueuedRunningLabel) {
        this.taskSingleEnqueuedRunningLabel = taskSingleEnqueuedRunningLabel;
        updateStatus();
    }

    public String getReadyLabel() {
        return readyLabel;
    }

    public void setReadyLabel(String readyLabel) {
        this.readyLabel = readyLabel;
        updateStatus();
    }

    public String getTasksFinishedLabel() {
        return tasksFinishedLabel;
    }

    public void setTasksFinishedLabel(String tasksFinishedLabel) {
        this.tasksFinishedLabel = tasksFinishedLabel;
        updateStatus();
    }

    private void initialize() {
        setIcon(JIPipe.RESOURCES.getIcon16("actions/check-circle.png"));
        UIUtils.setStandardButtonBorder(this);

        throbberIcon = new JIPipeDesktopRunnableQueueSpinnerIcon(this, runnerQueue);
        UIUtils.addReloadablePopupMenuToButton(this, menu, this::reloadMenu);
    }

    public JIPipeDesktopRunnableQueueButton makeFlat() {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        flatMode = true;
        return this;
    }

    public JIPipeRunnableExecutor getRunnerQueue() {
        return runnerQueue;
    }

    private void reloadMenu() {
        menu.removeAll();

        if (runnerQueue.size() > 0) {

            JMenuItem cancelAllItem = new JMenuItem("Cancel all tasks", JIPipe.RESOURCES.getIcon32("actions/stock_calc-cancel.png"));
            cancelAllItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            cancelAllItem.setToolTipText("Cancels all running and queued tasks");
            cancelAllItem.addActionListener(e -> {
                runnerQueue.cancelAll();
            });
            menu.add(cancelAllItem);

            if (runnerQueue.size() > 1) {
                JMenuItem cancelQueuedItem = new JMenuItem("Cancel only enqueued tasks", JIPipe.RESOURCES.getIcon32("actions/rabbitvcs-clear.png"));
                cancelQueuedItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
                cancelQueuedItem.setToolTipText("Cancels enqueued tasks. Currently running operations are not cancelled.");
                cancelQueuedItem.addActionListener(e -> {
                    for (JIPipeRunnableWorker worker : runnerQueue.getQueue()) {
                        runnerQueue.cancel(worker.getRun());
                    }
                });
                menu.add(cancelQueuedItem);
            }

            menu.addSeparator();

            JIPipeRunnableWorker currentRun = runnerQueue.getCurrentRunWorker();
            if (currentRun != null) {
                menu.add(new JIPipeDesktopRunnableQueueButtonMenuItem(runnerQueue, currentRun));
            }
            for (JIPipeRunnableWorker runWorker : runnerQueue.getQueue()) {
                menu.add(new JIPipeDesktopRunnableQueueButtonMenuItem(runnerQueue, runWorker));
            }
        } else {
            JMenuItem noTasksItem = new JMenuItem("There are currently no tasks running", JIPipe.RESOURCES.getIcon32("emblems/vcs-normal.png"));
            noTasksItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            menu.add(noTasksItem);
        }

//        if (workbench instanceof JIPipeProjectWorkbench && isOnGlobalRunnerQueue()) {
//            menu.addSeparator();
//            JMenuItem openLogsItem = new JMenuItem("Open logs", JIPipe.RESOURCES.getIcon32("actions/rabbitvcs-show_log.png"));
//            openLogsItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
//            openLogsItem.addActionListener(e -> workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG));
//            menu.add(openLogsItem);
//        }
    }

    private boolean isOnGlobalRunnerQueue() {
        return runnerQueue == JIPipeQueuedRunnableExecutor.getInstance();
    }

    /**
     * EDT: refreshes the displayed state when a batch arrived since the last refresh. The batch
     * listeners only set the dirty flag — all UI updates happen here, at most one per 500 ms.
     */
    private void refreshIfDirty() {
        if (!progressDirty) {
            return;
        }
        progressDirty = false;
        updateStatus();
    }

    /**
     * Updates the UI status. The progress bar shows the tracked node's live state from the run's
     * progress store (the latest node with a progress update), not the root handle's counters
     * (which stay at their initial values).
     */
    public void updateStatus() {
        JIPipeRunnable currentRun = runnerQueue.getCurrentRun();
        if (currentRun != null) {
            processAlreadyQueued = true;
            showProgress = true;
            applyTrackedNodeState();
            setIcon(throbberIcon);
            int size = runnerQueue.size();
            if (size <= 1) {
                setText(taskSingleRunningLabel);
            } else {
                setText(String.format(taskSingleEnqueuedRunningLabel, (size - 1)));
            }
            repaint();
        } else {
            showProgress = false;
            setIcon(JIPipe.RESOURCES.getIcon16("actions/check-circle.png"));
            if (!processAlreadyQueued) {
                setText(readyLabel);
            } else {
                setText(tasksFinishedLabel);
            }
            setToolTipText(null);
            repaint();
        }

//        reloadMenu();
    }

    /**
     * EDT: applies the tracked node's progress state to the painted progress bar. Nodes without a
     * determinate progress show no bar fill (indeterminate state).
     */
    private void applyTrackedNodeState() {
        JIPipeProgressLog store = trackedStore;
        JIPipeProgressNode node = store != null && trackedNodeId > 0 ? store.getNode(trackedNodeId) : null;
        if (node != null && node.getMaxProgress() > 0) {
            lastProgress = node.getProgress();
            lastMaxProgress = node.getMaxProgress();
            String chain = JIPipeProgressNodeUtils.labelChain(store, node.getId());
            setToolTipText(chain.isEmpty()
                    ? node.getProgress() + "/" + node.getMaxProgress()
                    : chain + " — " + node.getProgress() + "/" + node.getMaxProgress());
        } else {
            lastProgress = 0;
            lastMaxProgress = 0;
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (showProgress) {
            if (!flatMode) {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(15, getHeight() - 6, getWidth() - 15 * 2, 2);
                if (lastMaxProgress > 0) {
                    double perc = 1.0 * lastProgress / lastMaxProgress;
                    if (perc < 0)
                        perc = 0;
                    if (perc > 1)
                        perc = 1;
                    g.setColor(ThemeUtils.getCurrentStyle().getPrimaryColor());
                    g.fillRect(15, getHeight() - 6, (int) ((getWidth() - 15 * 2) * perc), 2);
                }
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(22, getHeight() - 4, getWidth() - 22, 2);
                if (lastMaxProgress > 0) {
                    double perc = 1.0 * lastProgress / lastMaxProgress;
                    if (perc < 0)
                        perc = 0;
                    if (perc > 1)
                        perc = 1;
                    g.setColor(ThemeUtils.getCurrentStyle().getPrimaryColor());
                    g.fillRect(22, getHeight() - 4, (int) ((getWidth() - 22) * perc), 2);
                }
            }
        }
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker reports progress. Runs on the store's writer thread: only the dirty
     * flag and the tracked node are set — the refresh timer (EDT) does the UI work.
     */
    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        JIPipeProgressLog.ProgressBatchEvent batch = event.getBatch();
        if (batch.fromLine() > batch.toLine()) {
            return;
        }
        JIPipeProgressLog store = (JIPipeProgressLog) batch.getSource();
        int latestNodeId = JIPipeProgressNodeUtils.latestNodeId(store, batch.changedNodes());
        if (latestNodeId > 0) {
            trackedStore = store;
            trackedNodeId = latestNodeId;
        }
        progressDirty = true;
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateStatus();
    }

}
