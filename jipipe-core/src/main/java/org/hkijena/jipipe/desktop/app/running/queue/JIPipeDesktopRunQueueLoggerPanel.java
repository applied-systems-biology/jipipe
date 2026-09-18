package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.LoggerPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopRunnableQueueSpinnerIcon;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;

import javax.swing.*;
import java.awt.*;

/**
 * Unified component that displays the status of a {@link JIPipeRunnableExecutor}
 */
public class JIPipeDesktopRunQueueLoggerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeRunnableExecutor queue;

    private final JLabel titleLabel = new JLabel();
    private final JButton queueButton = new JButton("Queue", JIPipe.RESOURCES.getIcon16("actions/list-check.png"));
    private final JIPipeDesktopRunnableQueueSpinnerIcon spinnerIcon;
    private final JLabel statusLabel = new JLabel("Ready ...");
    private final JProgressBar progressBar = new JProgressBar();
    private JIPipeRunnable targetRun;
    private Status targetRunStatus = Status.Enqueued;
    private final JPanel buttonPanel = UIUtils.boxHorizontal();
    private final JPanel headerButtonPanel = UIUtils.boxHorizontal();
    private final LoggerPanel loggerPanel = new LoggerPanel();
    private final JPopupMenu queuePopupMenu = new JPopupMenu();
    private final JCheckBox showLogCheckbox = new JCheckBox("Show log");
    private Component visibleLoggerComponent;

    private final StaticDebouncer progressDebouncer;
    private final StringBuilder batchedProgressText = new StringBuilder();
    private String batchedStatusText = "Ready ...";
    private int batchedProgressCurrent = 0;
    private int batchedProgressMax = 0;

    public JIPipeDesktopRunQueueLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableExecutor queue) {
        super(desktopWorkbench);
        this.queue = queue;
        this.progressDebouncer = new StaticDebouncer(250, this::updateProgress);
        this.spinnerIcon = new JIPipeDesktopRunnableQueueSpinnerIcon(this, queue, 32);
        this.showLogCheckbox.setSelected(JIPipe.getSettings().getFromRegistry("ui-general", JIPipePathMetadataStore.key("logger-panel", "show-log"), Boolean.class, Boolean.TRUE, true));

        initialize();

        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
        queue.getProgressEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);
        queue.getEnqueuedEventEmitter().subscribeWeak(this);

        // Load existing log
        if(queue.getCurrentRun() != null) {
            loggerPanel.setLogText(queue.getCurrentRun().getProgressInfo().getLog().toString());
        }

        addHeaderPanelComponent(showLogCheckbox);
        updateHeader();
        updateLoggerVisibility();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        JPanel headerPanel = UIUtils.boxHorizontal(titleLabel, Box.createHorizontalGlue(), headerButtonPanel, Box.createHorizontalStrut(16), queueButton);
        headerPanel.setBorder(UIUtils.createEmptyBorder(8));
        add(headerPanel, BorderLayout.NORTH);
        if(ThemeUtils.isUsingModernTheme()) {
            UIUtils.makeNonOpaque(loggerPanel, true);
        }

        visibleLoggerComponent = UIUtils.wrapInBackgroundIslandPanelIfNeeded(loggerPanel);
        add(visibleLoggerComponent, BorderLayout.CENTER);

        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        statusLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());

        JPanel bottomPanel = new JPanel(new BorderLayout(16,16));
        bottomPanel.setBorder(UIUtils.createEmptyBorder(8));
        bottomPanel.add(new JLabel(spinnerIcon), BorderLayout.WEST);

        JPanel statusInfoPanel = new JPanel(new GridBagLayout());
        progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 4));
        progressBar.setMinimumSize(new Dimension(32, 4));
        progressBar.setPreferredSize(new Dimension(100, 4));
        statusInfoPanel.add(progressBar, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, UIUtils.UI_PADDING, 0,0));
        statusInfoPanel.add(statusLabel, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, UIUtils.UI_PADDING, 0,0));

        bottomPanel.add(statusInfoPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        UIUtils.addReloadablePopupMenuToButton(queueButton, queuePopupMenu,  this::reloadQueuePopupMenu);

        showLogCheckbox.addActionListener(e -> {
            JIPipe.getSettings().putIntoRegistry("ui-general", JIPipePathMetadataStore.key("logger-panel", "show-log"), showLogCheckbox.isSelected());
            updateLoggerVisibility();
        });
    }

    private void updateLoggerVisibility() {
        visibleLoggerComponent.setVisible(showLogCheckbox.isSelected());
    }

    private void reloadQueuePopupMenu() {
        queuePopupMenu.removeAll();
        queuePopupMenu.add(UIUtils.createMenuItem("Cancel all", "Clears the queue and cancels the current run", JIPipe.RESOURCES.getIcon16("actions/gtk-cancel.png"), this::cancelAllRuns));
        queuePopupMenu.add(UIUtils.createMenuItem("Clear", "Clears the queue, but leaves the current run as-is", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::clearQueue));
        if(!queue.getQueue().isEmpty()) {
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
        queueButton.setText("Queue (" +  (queue.size() > 1 ? String.valueOf(queue.size() - 1) : "Empty") + ")");
        if(targetRun != null) {
            titleLabel.setText(targetRun.getTaskLabel());
            if(queue.getCurrentRun() == targetRun) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/run-build.png"));
            }
            else if(queue.isRunningOrEnqueued(targetRun)) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/hourglass-start.png"));
            }
            else if(targetRunStatus == Status.Interrupted) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/vcs-conflicting.png"));
            }
            else if(targetRunStatus == Status.Finished) {
                titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("emblems/vcs-normal.png"));
            }
        }
        else if(queue.getCurrentRun() != null) {
            titleLabel.setText(queue.getCurrentRun().getTaskLabel());
            titleLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/run-build.png"));
        }
        else {
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
        if(targetRun != null) {
            queue.cancel(targetRun);
        }
        else {
            JIPipeRunnable currentRun = queue.getCurrentRun();
            if(currentRun != null) {
                queue.cancel(currentRun);
            }
        }
    }

    public void addButton(JButton button) {
        buttonPanel.add(button);
    }

    public JIPipeRunnable getTargetRun() {
        return targetRun;
    }

    public void setTargetRun(JIPipeRunnable targetRun) {
        this.targetRun = targetRun;
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        batchedProgressText.append("\n").append(event.getStatus().getMessage());
        batchedProgressCurrent = event.getStatus().getProgress();
        batchedProgressMax = event.getStatus().getMaxProgress();
        batchedStatusText = event.getStatus().getMessage();
        progressDebouncer.debounce();
    }

    private void updateProgress() {
        if(queue.getCurrentRun() != null) {
            if(batchedProgressMax <= 1) {
                progressBar.setIndeterminate(true);
            }
            else {
                progressBar.setIndeterminate(false);
                progressBar.setMaximum(batchedProgressMax);
                progressBar.setValue(batchedProgressCurrent);
            }
        }
        else {
            progressBar.setIndeterminate(false);
        }

        statusLabel.setText(batchedStatusText);
        loggerPanel.appendLine(batchedProgressText.toString().trim());
        batchedProgressText.setLength(0);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Finished;
        }
        updateHeader();
        updateProgress();

        // Manually update UI
        batchedStatusText = "Finished.";
        statusLabel.setText(batchedStatusText);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(1);
        progressBar.setValue(1);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Interrupted;
        }
        updateHeader();
        updateProgress();

        // Manually update UI
        batchedStatusText = "Interrupted.";
        statusLabel.setText(batchedStatusText);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(1);
        progressBar.setValue(1);
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Running;
        }
        updateHeader();
        updateProgress();
    }

    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateHeader();
        updateProgress();
    }

    public enum Status {
        Enqueued,
        Running,
        Finished,
        Interrupted
    }
}
