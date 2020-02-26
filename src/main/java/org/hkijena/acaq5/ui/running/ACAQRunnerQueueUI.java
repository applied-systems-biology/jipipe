package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerStartedEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class ACAQRunnerQueueUI extends JPanel {

    private JPanel emptyQueuePanel;
    private JPanel runningQueuePanel;
    private JProgressBar runningQueueProgress;

    public ACAQRunnerQueueUI() {
        initialize();
        updateStatus();

        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // UI for empty queue
        emptyQueuePanel = new JPanel(new BorderLayout());
        emptyQueuePanel.setOpaque(false);
        emptyQueuePanel.add(new JLabel("No processes are running", UIUtils.getIconFromResources("pause.png"), JLabel.LEFT),
                BorderLayout.EAST);
        emptyQueuePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));

        // UI for running queue
        runningQueuePanel = new JPanel();
        runningQueuePanel.setOpaque(false);
        runningQueuePanel.setLayout(new BoxLayout(runningQueuePanel, BoxLayout.X_AXIS));
        runningQueuePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        runningQueueProgress = new JProgressBar();
        runningQueuePanel.add(runningQueueProgress);
        JButton cancelButton = new JButton(UIUtils.getIconFromResources("remove.png"));
        UIUtils.makeBorderlessWithoutMargin(cancelButton);
        cancelButton.setToolTipText("Cancel");
        cancelButton.addActionListener(e -> cancelRun());
        runningQueuePanel.add(Box.createHorizontalStrut(4));
        runningQueuePanel.add(cancelButton);
    }

    private void cancelRun() {
        if(ACAQRunnerQueue.getInstance().getCurrentRun() != null)
            ACAQRunnerQueue.getInstance().cancel(ACAQRunnerQueue.getInstance().getCurrentRun());
    }

    public void updateStatus() {
        if(ACAQRunnerQueue.getInstance().getCurrentRun() != null) {
            removeAll();
            add(runningQueuePanel, BorderLayout.EAST);
            revalidate();
            repaint();
        }
        else {
            removeAll();
            add(emptyQueuePanel, BorderLayout.EAST);
            revalidate();
            repaint();
        }
    }

    @Subscribe
    public void onWorkerStarted(RunUIWorkerStartedEvent event) {
        updateStatus();
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        updateStatus();
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        updateStatus();
    }

    @Subscribe
    public void onWorkerProgress(RunUIWorkerProgressEvent event) {
        runningQueueProgress.setMaximum(event.getStatus().getMaxProgress());
        runningQueueProgress.setValue(event.getStatus().getProgress());
        if(event.getStatus().getMessage() != null)
            runningQueueProgress.setToolTipText("(" + runningQueueProgress.getValue() + "/" + runningQueueProgress.getMaximum() + ") " +
                    event.getStatus().getMessage());
    }

}
