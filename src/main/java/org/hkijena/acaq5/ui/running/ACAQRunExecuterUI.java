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

package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * UI that executes an {@link ACAQRunnable}
 */
public class ACAQRunExecuterUI extends JPanel {
    private ACAQRunnable run;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JScrollPane logScrollPane;
    private JTextArea log;

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

        log = new JTextArea();
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setEditable(false);
        logScrollPane = new JScrollPane(log);
        add(logScrollPane, BorderLayout.CENTER);

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

        add(buttonPanel, BorderLayout.SOUTH);
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
            progressBar.setValue(event.getStatus().getProgress());
            progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
            log.append("[" + event.getStatus().getProgress() + "/" + event.getStatus().getMaxProgress() + "] " + event.getStatus().getMessage() + "\n");
        }
    }

}
