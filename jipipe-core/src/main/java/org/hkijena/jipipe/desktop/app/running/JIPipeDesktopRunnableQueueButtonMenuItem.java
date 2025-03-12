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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopRunnableQueueButtonMenuItem extends JMenuItem implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener,
        JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeRunnableQueue runnerQueue;
    private final JLabel titleLabel = new JLabel("Status");
    private final JLabel iconLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton cancelButton = new JButton(UIUtils.getIcon32FromResources("actions/cancel.png"));
    private JIPipeRunnableWorker worker;

    public JIPipeDesktopRunnableQueueButtonMenuItem(JIPipeRunnableQueue runnerQueue, JIPipeRunnableWorker worker) {
        this.runnerQueue = runnerQueue;
        this.worker = worker;
        initialize();
        updateStatus(null);
        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        runnerQueue.getProgressEventEmitter().subscribeWeak(this);
        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getEnqueuedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        removeAll();
        setPreferredSize(new Dimension(300, 48));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
        setLayout(new GridBagLayout());

        titleLabel.setText(worker.getRun().getTaskLabel());
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
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

    private void updateStatus(JIPipeProgressInfo.StatusUpdatedEvent status) {
        if (worker == null) {
            return;
        }
        if (worker.isDone()) {
            if (worker.isCancelled()) {
                iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-error.png"));
                statusLabel.setText("Cancelled.");
            } else {
                iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-success.png"));
                statusLabel.setText("Done.");
            }
            progressBar.setMaximum(1);
            progressBar.setValue(1);
            progressBar.setIndeterminate(false);
            cancelButton.setEnabled(false);
            worker = null;
        } else if (runnerQueue.getCurrentRunWorker() == worker) {
            cancelButton.setEnabled(true);
            iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-insync-syncing.png"));
            if (status != null) {
                progressBar.setIndeterminate(false);
                progressBar.setMaximum(status.getMaxProgress());
                progressBar.setValue(status.getProgress());
                statusLabel.setText(status.getMessage());
            } else {
                statusLabel.setText("In progress ...");
                progressBar.setMaximum(1);
                progressBar.setValue(0);
                progressBar.setIndeterminate(true);
            }
        } else {
            iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-hourglass.png"));
            cancelButton.setEnabled(true);
            statusLabel.setText("Enqueued.");
            progressBar.setMaximum(1);
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
        }
    }

    /**
     * Triggered when a worker is started
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateStatus(null);
    }

    /**
     * Triggered when a worker is enqueued
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateStatus(null);
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        updateStatus(null);
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        updateStatus(null);
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        updateStatus(event.getStatus());
    }
}
