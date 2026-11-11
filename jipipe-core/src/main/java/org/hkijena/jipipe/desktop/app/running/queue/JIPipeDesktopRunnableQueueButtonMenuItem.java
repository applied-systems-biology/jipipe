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
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeUtils;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopRunnableQueueButtonMenuItem extends JMenuItem implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener,
        JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeRunnableExecutor runnerQueue;
    private final JLabel titleLabel = new JLabel("Status");
    private final JLabel iconLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton cancelButton = new JButton(JIPipe.RESOURCES.getIcon32("actions/cancel.png"));
    private final Timer refreshTimer = new Timer(500, e -> refreshIfDirty());
    private JIPipeRunnableWorker worker;

    /**
     * The node whose state is displayed (the latest node with a progress update among the batches'
     * changed nodes) and the store it belongs to. Written by batch listeners (writer thread), read
     * by the refresh timer (EDT).
     */
    private volatile JIPipeProgressLog trackedStore;
    private volatile int trackedNodeId;
    private volatile boolean progressDirty;

    public JIPipeDesktopRunnableQueueButtonMenuItem(JIPipeRunnableExecutor runnerQueue, JIPipeRunnableWorker worker) {
        this.runnerQueue = runnerQueue;
        this.worker = worker;
        initialize();
        updateStatus();
        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        runnerQueue.getProgressEventEmitter().subscribeWeak(this);
        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getEnqueuedEventEmitter().subscribeWeak(this);
        refreshTimer.start();
    }

    private void initialize() {
        removeAll();
        setPreferredSize(new Dimension(300, 48));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
        setLayout(new GridBagLayout());

        titleLabel.setText(worker.getRun().getTaskLabel());
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeTiny()));
        cancelButton.setBorder(null);
        progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 8));
        progressBar.setBorder(null);

        add(iconLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        add(titleLabel, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        add(statusLabel, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        add(progressBar, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        add(cancelButton, new GridBagConstraints(2, 0, 1, 3, 0, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));

        cancelButton.addActionListener(e -> cancelClicked());
    }

    private void cancelClicked() {
        JPopupMenu ancestor = (JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, this);
        ancestor.setVisible(false);
        runnerQueue.cancel(worker.getRun());
    }

    /**
     * EDT: refreshes the displayed state when a batch arrived since the last refresh. The batch
     * listeners only set the dirty flag - all UI updates happen here, at most one per 500 ms.
     */
    private void refreshIfDirty() {
        if (!progressDirty) {
            return;
        }
        progressDirty = false;
        updateStatus();
    }

    /**
     * EDT: updates the displayed state from the queue and the run's progress store. The progress
     * bar and status text show the tracked node's live state (the latest node with a progress
     * update), not the root handle's counters (which stay at their initial values).
     */
    private void updateStatus() {
        if (worker == null) {
            return;
        }
        if (worker.isDone()) {
            if (worker.isCancelled()) {
                iconLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-error.png"));
                statusLabel.setText("Cancelled.");
            } else {
                iconLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-success.png"));
                statusLabel.setText("Done.");
            }
            progressBar.setMaximum(1);
            progressBar.setValue(1);
            progressBar.setIndeterminate(false);
            cancelButton.setEnabled(false);
            refreshTimer.stop();
            worker = null;
        } else if (runnerQueue.getCurrentRunWorker() == worker) {
            cancelButton.setEnabled(true);
            iconLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-insync-syncing.png"));
            JIPipeProgressLog store = trackedStore;
            JIPipeProgressNode node = store != null && trackedNodeId > 0 ? store.getNode(trackedNodeId) : null;
            if (node != null) {
                progressBar.setIndeterminate(node.getMaxProgress() <= 0);
                progressBar.setMaximum(Math.max(node.getMaxProgress(), 1));
                progressBar.setValue(node.getProgress());
                statusLabel.setText(statusText(store, node));
            } else {
                statusLabel.setText("In progress ...");
                progressBar.setMaximum(1);
                progressBar.setValue(0);
                progressBar.setIndeterminate(true);
            }
        } else {
            iconLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-hourglass.png"));
            cancelButton.setEnabled(true);
            statusLabel.setText("Enqueued.");
            progressBar.setMaximum(1);
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
        }
    }

    /**
     * The status text of a tracked node: the node chain label + progress text.
     */
    private static String statusText(JIPipeProgressLog store, JIPipeProgressNode node) {
        String chain = JIPipeProgressNodeUtils.labelChain(store, node.getId());
        String progress = JIPipeProgressNodeUtils.progressText(node);
        if (chain.isEmpty()) {
            return progress.isEmpty() ? "In progress ..." : progress;
        }
        return progress.isEmpty() ? chain : chain + " - " + progress;
    }

    /**
     * Triggered when a worker is started
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is enqueued
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker reports progress. Runs on the store's writer thread: only the dirty
     * flag and the tracked node are set - the refresh timer (EDT) does the UI work.
     *
     * @param event Generated event
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
}
