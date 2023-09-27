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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.icons.JIPipeRunnerQueueThrobberIcon;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeRunnerQueueButton extends JButton implements JIPipeWorkbenchAccess, JIPipeRunnable.StartedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeWorkbench workbench;
    private final JIPipeRunnerQueue runnerQueue;
    private final JPopupMenu menu = new JPopupMenu();
    private boolean processAlreadyQueued;
    private JIPipeRunnerQueueThrobberIcon throbberIcon;
    private boolean showProgress;
    private int lastProgress;
    private int lastMaxProgress;
    private boolean flatMode = false;

    /**
     * Creates new instance
     */
    public JIPipeRunnerQueueButton(JIPipeWorkbench workbench) {
        this(workbench, JIPipeRunnerQueue.getInstance());
    }

    public JIPipeRunnerQueueButton(JIPipeWorkbench workbench, JIPipeRunnerQueue runnerQueue) {
        this.workbench = workbench;
        this.runnerQueue = runnerQueue;
        initialize();
        updateStatus();

        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        runnerQueue.getProgressEventEmitter().subscribeWeak(this);
        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getEnqueuedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setIcon(UIUtils.getIconFromResources("actions/check-circle.png"));
        UIUtils.setStandardButtonBorder(this);

        throbberIcon = new JIPipeRunnerQueueThrobberIcon(this, runnerQueue);
        UIUtils.addReloadablePopupMenuToButton(this, menu, this::reloadMenu);
    }

    public JIPipeRunnerQueueButton makeFlat() {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        flatMode = true;
        return this;
    }

    public JIPipeRunnerQueue getRunnerQueue() {
        return runnerQueue;
    }

    private void reloadMenu() {
        menu.removeAll();

        if (runnerQueue.size() > 0) {

            JMenuItem cancelAllItem = new JMenuItem("Cancel all tasks", UIUtils.getIcon32FromResources("actions/stock_calc-cancel.png"));
            cancelAllItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            cancelAllItem.setToolTipText("Cancels all running and queued tasks");
            cancelAllItem.addActionListener(e -> {
                runnerQueue.clearQueue();
                JIPipeRunnable currentRun = runnerQueue.getCurrentRun();
                runnerQueue.cancel(currentRun);
            });
            menu.add(cancelAllItem);

            if (runnerQueue.size() > 1) {
                JMenuItem cancelQueuedItem = new JMenuItem("Cancel only enqueued tasks", UIUtils.getIcon32FromResources("actions/rabbitvcs-clear.png"));
                cancelQueuedItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
                cancelQueuedItem.setToolTipText("Cancels enqueued tasks. Currently running operations are not cancelled.");
                cancelQueuedItem.addActionListener(e -> {
                    runnerQueue.clearQueue();
                });
                menu.add(cancelQueuedItem);
            }

            menu.addSeparator();

            JIPipeRunWorker currentRun = runnerQueue.getCurrentRunWorker();
            if (currentRun != null) {
                menu.add(new RunMenuItem(runnerQueue, currentRun));
            }
            for (JIPipeRunWorker runWorker : runnerQueue.getQueue()) {
                menu.add(new RunMenuItem(runnerQueue, runWorker));
            }
        } else {
            JMenuItem noTasksItem = new JMenuItem("There are currently no tasks running", UIUtils.getIcon32FromResources("emblems/vcs-normal.png"));
            noTasksItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            menu.add(noTasksItem);
        }

        if (workbench instanceof JIPipeProjectWorkbench && isOnGlobalRunnerQueue()) {
            menu.addSeparator();
            JMenuItem openLogsItem = new JMenuItem("Open logs", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"));
            openLogsItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            openLogsItem.addActionListener(e -> workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG));
            menu.add(openLogsItem);
        }
    }

    private boolean isOnGlobalRunnerQueue() {
        return runnerQueue == JIPipeRunnerQueue.getInstance();
    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        JIPipeRunnable currentRun = runnerQueue.getCurrentRun();
        if (currentRun != null) {
            processAlreadyQueued = true;
            showProgress = true;
            lastProgress = currentRun.getProgressInfo().getProgress();
            lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
            setIcon(throbberIcon);
            int size = runnerQueue.size();
            if (size <= 1) {
                setText("1 task running");
            } else {
                setText("1 task running (+" + (size - 1) + " enqueued)");
            }
            repaint();
        } else {
            showProgress = false;
            setIcon(UIUtils.getIconFromResources("actions/check-circle.png"));
            if (!processAlreadyQueued) {
                setText("Ready");
            } else {
                setText("All tasks finished");
            }
            repaint();
        }

//        reloadMenu();
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
                    g.setColor(ModernMetalTheme.PRIMARY5);
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
                    g.setColor(ModernMetalTheme.PRIMARY5);
                    g.fillRect(22, getHeight() - 4, (int) ((getWidth() - 22) * perc), 2);
                }
            }
        }
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        JIPipeRunnable currentRun = event.getRun();
        lastProgress = currentRun.getProgressInfo().getProgress();
        lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
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

    public static class RunMenuItem extends JMenuItem implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener,
            JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

        private final JIPipeRunnerQueue runnerQueue;
        private final JLabel titleLabel = new JLabel("Status");
        private final JLabel iconLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final JProgressBar progressBar = new JProgressBar();
        private final JButton cancelButton = new JButton(UIUtils.getIcon32FromResources("actions/cancel.png"));
        private JIPipeRunWorker worker;

        public RunMenuItem(JIPipeRunnerQueue runnerQueue, JIPipeRunWorker worker) {
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

}
