package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.LoggerPanel;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.awt.*;

/**
 * Unified component that displays the status of a {@link JIPipeRunnableQueue}
 */
public class JIPipeDesktopRunQueueLoggerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener {

    private final JIPipeRunnableQueue queue;

    private final JLabel titleLabel = new JLabel();
    private final JButton queueButton = new JButton("Queue", JIPipe.RESOURCES.getIcon16("actions/list-check.png"));
    private final JLabel statusLabel = new JLabel("Ready ...");
    private final JProgressBar progressBar = new JProgressBar();
    private JIPipeRunnable targetRun;
    private Status targetRunStatus = Status.Enqueued;
    private final JPanel buttonPanel = UIUtils.boxHorizontal();
    private final JPanel headerButtonPanel = UIUtils.boxHorizontal();
    private final LoggerPanel loggerPanel = new LoggerPanel();

    private final StaticDebouncer progressDebouncer;
    private final StringBuilder batchedProgressText = new StringBuilder();
    private int batchedProgressCurrent = 0;
    private int batchedProgressMax = 0;

    public JIPipeDesktopRunQueueLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableQueue queue) {
        super(desktopWorkbench);
        this.queue = queue;
        this.progressDebouncer = new StaticDebouncer(250, this::updateProgress);
        initialize();

        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
        queue.getProgressEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);

        // Load existing log
        if(queue.getCurrentRun() != null) {
            loggerPanel.setLogText(queue.getCurrentRun().getProgressInfo().getLog().toString());
        }

        updateHeader();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        JPanel headerPanel = UIUtils.boxHorizontal(titleLabel, Box.createHorizontalGlue(), headerButtonPanel, queueButton);
        headerPanel.setBorder(UIUtils.createEmptyBorder(8));
        add(headerPanel, BorderLayout.NORTH);
        if(ThemeUtils.isUsingModernTheme()) {
            UIUtils.makeNonOpaque(loggerPanel, true);
        }
        add(UIUtils.wrapInBackgroundIslandPanelIfNeeded(loggerPanel), BorderLayout.CENTER);

        buttonPanel.setBorder(UIUtils.createEmptyBorder(8));
        buttonPanel.add(Box.createHorizontalGlue());
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateHeader() {
        queueButton.setVisible(queue.size() > 1);
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
        progressDebouncer.debounce();
    }

    private void updateProgress() {
        if(batchedProgressMax == 0) {
            progressBar.setIndeterminate(true);
        }
        else {
            progressBar.setMaximum(batchedProgressMax);
            progressBar.setValue(batchedProgressCurrent);
        }
        loggerPanel.appendLine(batchedProgressText.toString());
        batchedProgressText.setLength(0);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Finished;
        }
        updateHeader();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Interrupted;
        }
        updateHeader();
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        if(targetRun != null && event.getRun() == targetRun) {
            targetRunStatus = Status.Running;
        }
        updateHeader();
    }

    public enum Status {
        Enqueued,
        Running,
        Finished,
        Interrupted
    }
}
