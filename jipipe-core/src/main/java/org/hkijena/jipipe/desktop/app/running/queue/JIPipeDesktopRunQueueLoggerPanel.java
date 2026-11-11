package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopProgressTreePanel;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Unified component that displays the status of a {@link JIPipeRunnableExecutor}.
 * <p>
 * The flat log was replaced by the v2 progress tree panel
 * ({@link JIPipeDesktopProgressTreePanel} - the Swing implementation of the approved mockup
 * {@code ui/v2-progress-panel.html}): node/phase rows with inline progress, expanded record views,
 * level filter, and a footer with the running path, live progress, record count, and data file.
 */
public class JIPipeDesktopRunQueueLoggerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeRunnableExecutor queue;

    private final JLabel titleLabel = new JLabel();
    private final JButton queueButton = new JButton("Queue", JIPipe.RESOURCES.getIcon16("actions/list-check.png"));
    private final JPanel headerButtonPanel = UIUtils.boxHorizontal();
    private final JIPipeDesktopProgressTreePanel treePanel;
    private final JPopupMenu queuePopupMenu = new JPopupMenu();
    private JIPipeRunnable targetRun;
    private Status targetRunStatus = Status.Enqueued;

    /**
     * The binding to {@link SettingsKeys#RUN_QUEUE_SHOW_LOG}: created once per
     * panel instance and released by {@link #dispose()}. The tree panel's Show log
     * checkbox is the UI surface; the binding is synced from it.
     */
    private SettingsBinding<Boolean> showLogBinding;
    private final ItemListener showLogSyncListener;

    public JIPipeDesktopRunQueueLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableExecutor queue) {
        super(desktopWorkbench);
        this.queue = queue;
        this.treePanel = new JIPipeDesktopProgressTreePanel(desktopWorkbench);
        // One binding per panel instance: the panel owns it and releases it in dispose().
        // The stored value is placed on the tree panel's checkbox BEFORE the sync listener
        // is registered, so the initial apply can never echo a write back into the binding
        // (same pattern as the previous Show log checkbox). Without a running settings
        // service the checkbox simply starts checked and keeps its value in memory.
        if (JIPipe.isInstantiated()) {
            showLogBinding = JIPipe.getSettingsService().bind(SettingsKeys.RUN_QUEUE_SHOW_LOG);
            treePanel.setShowLogSelected(showLogBinding.get());
        } else {
            treePanel.setShowLogSelected(true);
        }
        this.showLogSyncListener = e -> {
            if (showLogBinding != null) {
                showLogBinding.set(e.getStateChange() == ItemEvent.SELECTED);
            }
        };
        treePanel.getShowLogCheckBox().addItemListener(showLogSyncListener);

        initialize();

        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
        queue.getProgressEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);
        queue.getEnqueuedEventEmitter().subscribeWeak(this);

        // Show the current run's progress tree (if one is already active)
        if (queue.getCurrentRun() != null) {
            treePanel.setProgressStore(queue.getCurrentRun().getProgressInfo().getStore());
        }

        updateHeader();
    }

    /**
     * Releases the show-log binding (its document path listener) with the panel.
     * Called by {@link JIPipeDesktopRunExecuteUI} when its dialog is disposed; panels hosted
     * in a long-lived dock (the graph editor log) keep their binding for the workbench's
     * lifetime, which is bounded by one panel per editor.
     */
    public void dispose() {
        treePanel.getShowLogCheckBox().removeItemListener(showLogSyncListener);
        if (showLogBinding != null) {
            showLogBinding.close();
            showLogBinding = null;
        }
        treePanel.dispose();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        JPanel headerPanel = UIUtils.boxHorizontal(titleLabel, Box.createHorizontalGlue(), headerButtonPanel, Box.createHorizontalStrut(16), queueButton);
        headerPanel.setBorder(UIUtils.createEmptyBorder(8));
        add(headerPanel, BorderLayout.NORTH);

        // The progress panel fills the window edge-to-edge: the background island's rim reads as a
        // stray border seam between the panel and the window edge
        add(treePanel, BorderLayout.CENTER);

        UIUtils.addReloadablePopupMenuToButton(queueButton, queuePopupMenu, this::reloadQueuePopupMenu);
    }

    private void reloadQueuePopupMenu() {
        queuePopupMenu.removeAll();
        queuePopupMenu.add(UIUtils.createMenuItem("Cancel all", "Clears the queue and cancels the current run", JIPipe.RESOURCES.getIcon16("actions/gtk-cancel.png"), this::cancelAllRuns));
        queuePopupMenu.add(UIUtils.createMenuItem("Clear", "Clears the queue, but leaves the current run as-is", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::clearQueue));
        if (!queue.getQueue().isEmpty()) {
            queuePopupMenu.addSeparator();
            for (JIPipeRunnableWorker worker : queue.getQueue()) {
                queuePopupMenu.add(UIUtils.createMenuItem("Cancel '" + worker.getRun().getTaskLabel() + "'", "Removes the task from the queue", JIPipe.RESOURCES.getIcon16("actions/gtk-cancel.png"), () -> {
                    queue.cancel(worker.getRun());
                }));
            }
        }
    }

    private void clearQueue() {
        for (JIPipeRunnableWorker worker : queue.getQueue()) {
            queue.cancel(worker.getRun());
        }
    }

    private void cancelAllRuns() {
        queue.cancelAll();
    }

    private void updateHeader() {
        queueButton.setEnabled(queue.size() > 1);
        queueButton.setText("Queue (" + (queue.size() > 1 ? String.valueOf(queue.size() - 1) : "Empty") + ")");
        if (targetRun != null) {
            titleLabel.setText(targetRun.getTaskLabel());
            if (queue.getCurrentRun() == targetRun) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/run-build.png"));
            } else if (queue.isRunningOrEnqueued(targetRun)) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/hourglass-start.png"));
            } else if (targetRunStatus == Status.Interrupted) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/vcs-conflicting.png"));
            } else if (targetRunStatus == Status.Finished) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/vcs-normal.png"));
            }
        } else if (queue.getCurrentRun() != null) {
            titleLabel.setText(queue.getCurrentRun().getTaskLabel());
            titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/run-build.png"));
        } else {
            titleLabel.setText("All tasks finished");
            titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-success.png"));
        }

    }

    public void addHeaderPanelComponent(Component component) {
        headerButtonPanel.add(component);
    }

    public void addDefaultCancelButton() {
        addButton(UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/gtk-cancel.png"), this::cancelCurrentRun));
    }

    private void cancelCurrentRun() {
        if (targetRun != null) {
            queue.cancel(targetRun);
        } else {
            JIPipeRunnable currentRun = queue.getCurrentRun();
            if (currentRun != null) {
                queue.cancel(currentRun);
            }
        }
    }

    public void addButton(JButton button) {
        treePanel.addFooterComponent(button);
    }

    public JIPipeRunnable getTargetRun() {
        return targetRun;
    }

    public void setTargetRun(JIPipeRunnable targetRun) {
        this.targetRun = targetRun;
        updateHeader();
    }

    /**
     * Whether to automatically scroll the tree to the running node (also drives the graph
     * editor's dock behavior via the hosting panel).
     */
    public boolean isAutoShowProgress() {
        return treePanel.isAutoShowProgress();
    }

    public void setAutoShowProgressSelected(boolean selected) {
        treePanel.setAutoShowProgressSelected(selected);
    }

    /**
     * Semantic passthrough for run managers (auto-open results).
     */
    public boolean isAutoViewResults() {
        return treePanel.isAutoViewResults();
    }

    public void setAutoViewResultsSelected(boolean selected) {
        treePanel.setAutoViewResultsSelected(selected);
    }

    /**
     * Forwards ONE coalesced batch of a run's progress store to the tree panel. Runs on the store's
     * writer thread; all reads and UI updates are handled inside the tree panel (no per-record work
     * here). The empty final batch (fromLine &gt; toLine) is forwarded as the refresh signal - the
     * run's Finished/Interrupted events set the terminal state. Batches of other runs are ignored
     * when a target run is set (run dialogs show their own run).
     */
    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        if (targetRun != null && event.getRun() != targetRun) {
            return;
        }
        treePanel.onProgressBatch(event.getBatch());
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Finished;
        }
        updateHeader();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Interrupted;
        }
        updateHeader();
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        if (targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Running;
        }
        // A new run: show its progress tree (the store arrives with the run's first batch;
        // graph runs adopt their own store inside run()). A dialog with a target run ignores
        // the starts of unrelated runs.
        if (targetRun == null || event.getRun() == targetRun) {
            treePanel.reset();
        }
        updateHeader();
    }

    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateHeader();
    }

    public enum Status {
        Enqueued,
        Running,
        Finished,
        Interrupted
    }
}
