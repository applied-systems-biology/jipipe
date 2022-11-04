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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.icons.JIPipeRunThrobberIcon;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeRunnerQueueUI extends JButton implements JIPipeWorkbenchAccess {

    private final JIPipeWorkbench workbench;

    private boolean processAlreadyQueued;

    private JIPipeRunThrobberIcon throbberIcon;

    private boolean showProgress;

    private int lastProgress;

    private int lastMaxProgress;

    private final JPopupMenu menu = new JPopupMenu();

    /**
     * Creates new instance
     */
    public JIPipeRunnerQueueUI(JIPipeWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        updateStatus();

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setIcon(UIUtils.getIconFromResources("actions/check-circle.png"));
        UIUtils.makeFlat(this);

        throbberIcon = new JIPipeRunThrobberIcon(this);

        UIUtils.addReloadablePopupMenuToComponent(this, menu, this::reloadMenu);
    }

    private void reloadMenu() {
        menu.removeAll();

        if(JIPipeRunnerQueue.getInstance().size() > 0) {

            JMenuItem cancelAllItem = new JMenuItem("Cancel all tasks", UIUtils.getIcon32FromResources("actions/stock_calc-cancel.png"));
            cancelAllItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            cancelAllItem.setToolTipText("Cancels all running and queued tasks");
            cancelAllItem.addActionListener(e -> {
                JIPipeRunnerQueue.getInstance().clearQueue();
                JIPipeRunnable currentRun = JIPipeRunnerQueue.getInstance().getCurrentRun();
                JIPipeRunnerQueue.getInstance().cancel(currentRun);
            });
            menu.add(cancelAllItem);

            if(JIPipeRunnerQueue.getInstance().size() > 1) {
                JMenuItem cancelQueuedItem = new JMenuItem("Cancel only enqueued tasks", UIUtils.getIcon32FromResources("actions/rabbitvcs-clear.png"));
                cancelQueuedItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
                cancelQueuedItem.setToolTipText("Cancels enqueued tasks. Currently running operations are not cancelled.");
                cancelQueuedItem.addActionListener(e -> {
                    JIPipeRunnerQueue.getInstance().clearQueue();
                });
                menu.add(cancelQueuedItem);
            }

            menu.addSeparator();

            JIPipeRunWorker currentRun = JIPipeRunnerQueue.getInstance().getCurrentRunWorker();
            if(currentRun != null) {
                menu.add(new RunMenuItem(currentRun));
            }
            for (JIPipeRunWorker runWorker : JIPipeRunnerQueue.getInstance().getQueue()) {
                menu.add(new RunMenuItem(runWorker));
            }
        }
        else {
            JMenuItem noTasksItem = new JMenuItem("There are currently no tasks running", UIUtils.getIcon32FromResources("emblems/vcs-normal.png"));
            menu.add(noTasksItem);
        }

        if(workbench instanceof JIPipeProjectWorkbench) {
            menu.addSeparator();
            JMenuItem openLogsItem = new JMenuItem("Open logs", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"));
            openLogsItem.addActionListener(e -> workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG));
            menu.add(openLogsItem);
        }
    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        JIPipeRunnable currentRun = JIPipeRunnerQueue.getInstance().getCurrentRun();
        if (currentRun != null) {
            processAlreadyQueued = true;
            showProgress = true;
            lastProgress = currentRun.getProgressInfo().getProgress();
            lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
            setIcon(throbberIcon);
            int size = JIPipeRunnerQueue.getInstance().size();
            if (size <= 1) {
                setText("1 task running");
            } else {
                setText("1 task running (+" + (size - 1) + " enqueued)");
            }
            repaint();
        } else {
            showProgress = false;
            setIcon(UIUtils.getIconFromResources("actions/check-circle.png"));
            if(!processAlreadyQueued) {
                setText("Ready");
            }
            else {
                setText("All tasks finished");
            }
            repaint();
        }

        reloadMenu();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if(showProgress) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(15,getHeight() - 6,getWidth()-15*2, 2);
            if(lastMaxProgress > 0) {
                double perc = 1.0 * lastProgress / lastMaxProgress;
                if(perc < 0)
                    perc = 0;
                if(perc > 1)
                    perc = 1;
                g.setColor(ModernMetalTheme.PRIMARY5);
                g.fillRect(15,getHeight() - 6, (int) ((getWidth()-15*2) * perc), 2);
            }
        }
    }

    /**
     * Triggered when a worker is started
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerStarted(RunWorkerStartedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is enqueued
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerEnqueued(RunWorkerEnqueuedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunWorkerFinishedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(RunWorkerProgressEvent event) {
        JIPipeRunnable currentRun = event.getRun();
        lastProgress = currentRun.getProgressInfo().getProgress();
        lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public static class RunMenuItem extends JMenuItem {
        private final JIPipeRunWorker worker;
        private final JLabel titleLabel = new JLabel("Status");

        private final JLabel iconLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final JProgressBar progressBar = new JProgressBar();

        private final JButton cancelButton = new JButton(UIUtils.getIcon32FromResources("actions/cancel.png"));

        public RunMenuItem(JIPipeRunWorker worker) {
            this.worker = worker;
            initialize();
            updateStatus(null);
            JIPipeRunnerQueue.getInstance().getEventBus().register(this);
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

            add(iconLabel, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,new Insets(2,2,2,2), 0, 0));
            add(titleLabel, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2), 0, 0));
            add(statusLabel, new GridBagConstraints(1,1,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2), 0, 0));
            add(progressBar, new GridBagConstraints(1,2,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2), 0, 0));
            add(cancelButton, new GridBagConstraints(2,0,1,3,0,1,GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, new Insets(2,2,2,2), 0,0));

            cancelButton.addActionListener(e-> cancelClicked());
        }

        private void cancelClicked() {
            JPopupMenu ancestor = (JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, this);
            ancestor.setVisible(false);
            JIPipeRunnerQueue.getInstance().cancel(worker.getRun());
        }

        private void updateStatus(JIPipeProgressInfo.StatusUpdatedEvent status) {
            if(worker.isDone()) {
                if(worker.isCancelled()) {
                    iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-error.png"));
                    statusLabel.setText("Cancelled.");
                }
                else {
                    iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-success.png"));
                    statusLabel.setText("Done.");
                }
                progressBar.setMaximum(1);
                progressBar.setValue(1);
                progressBar.setIndeterminate(false);
                cancelButton.setEnabled(false);
            }
            else if(JIPipeRunnerQueue.getInstance().getCurrentRunWorker() == worker) {
                cancelButton.setEnabled(true);
                iconLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-insync-syncing.png"));
                if(status != null) {
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(status.getMaxProgress());
                    progressBar.setValue(status.getProgress());
                    statusLabel.setText(status.getMessage());
                }
                else {
                    statusLabel.setText("In progress ...");
                    progressBar.setMaximum(1);
                    progressBar.setValue(0);
                    progressBar.setIndeterminate(true);
                }
            }
            else {
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
        @Subscribe
        public void onWorkerStarted(RunWorkerStartedEvent event) {
            updateStatus(null);
        }

        /**
         * Triggered when a worker is enqueued
         *
         * @param event Generated event
         */
        @Subscribe
        public void onWorkerEnqueued(RunWorkerEnqueuedEvent event) {
            updateStatus(null);
        }

        /**
         * Triggered when a worker is finished
         *
         * @param event Generated event
         */
        @Subscribe
        public void onWorkerFinished(RunWorkerFinishedEvent event) {
            updateStatus(null);
        }

        /**
         * Triggered when a worker is interrupted
         *
         * @param event Generated event
         */
        @Subscribe
        public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
            updateStatus(null);
        }

        /**
         * Triggered when a worker reports progress
         *
         * @param event Generated event
         */
        @Subscribe
        public void onWorkerProgress(RunWorkerProgressEvent event) {
            updateStatus(event.getStatus());
        }
    }

}
