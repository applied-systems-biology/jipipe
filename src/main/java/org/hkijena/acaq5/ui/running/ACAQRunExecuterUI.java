package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that executes an {@link ACAQRunnable}
 */
public class ACAQRunExecuterUI extends JPanel {
    private ACAQRunnable run;
    private JProgressBar progressBar;
    private JButton cancelButton;

    /**
     * @param run The runnable
     */
    public ACAQRunExecuterUI(ACAQRunnable run) {
        this.run = run;
        initialize();
        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        progressBar = new JProgressBar();
        progressBar.setString("Ready");
        progressBar.setStringPainted(true);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> requestCancelRun());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.CENTER);
    }

    /**
     * Starts the run
     */
    public void startRun() {
        ACAQRunWorker worker = ACAQRunnerQueue.getInstance().enqueue(run);
        progressBar.setString("Waiting until other processes are finished ...");
        progressBar.setIndeterminate(true);
        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            ACAQRunnerQueue.getInstance().cancel(run);
        });
    }

    /**
     * Cancels the run
     */
    public void requestCancelRun() {
        cancelButton.setEnabled(false);
        ACAQRunnerQueue.getInstance().cancel(run);
    }

    /**
     * Triggered when a scheduled worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run) {
            cancelButton.setEnabled(false);
            progressBar.setString("Finished");
        }
    }

    /**
     * Triggered when a schedules worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run) {
            cancelButton.setEnabled(false);
            progressBar.setString("Finished");
        }
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(RunUIWorkerProgressEvent event) {
        if (event.getRun() == run) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(event.getStatus().getMaxProgress());
            progressBar.setValue(event.getStatus().getMaxProgress());
            progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
        }
    }

}
