package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQRunExecuterUI extends JPanel {
    private ACAQRun run;
    private JProgressBar progressBar;
    private JButton cancelButton;

    public ACAQRunExecuterUI(ACAQRun run) {
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

    public void startRun() {
        ACAQRunWorker worker = ACAQRunnerQueue.getInstance().enqueue(run);
        progressBar.setString("Waiting until other processes are finished ...");
        progressBar.setIndeterminate(true);
        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            ACAQRunnerQueue.getInstance().cancel(run);
        });
    }

    public void requestCancelRun() {
        cancelButton.setEnabled(false);
        ACAQRunnerQueue.getInstance().cancel(run);
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run) {
            cancelButton.setEnabled(false);
            progressBar.setString("Finished");
        }
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run) {
            cancelButton.setEnabled(false);
            progressBar.setString("Finished");
        }
    }

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
