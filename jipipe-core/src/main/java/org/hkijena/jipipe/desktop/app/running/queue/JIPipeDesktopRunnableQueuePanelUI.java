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

package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that executes an {@link JIPipeRunnable}
 */
public class JIPipeDesktopRunnableQueuePanelUI extends JPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.FinishedEventListener,
        JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener {
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JScrollPane logScrollPane;
    private JTextArea log;


    public JIPipeDesktopRunnableQueuePanelUI() {
        initialize();
        // Pre-initialize log
        JIPipeRunnable currentRun = JIPipeRunnableQueue.getInstance().getCurrentRun();
        if (currentRun != null) {
            log.append(currentRun.getProgressInfo().getLog().toString());
        }
        JIPipeRunnableQueue.getInstance().getStartedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getProgressEventEmitter().subscribeWeak(this);
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
        progressBar.setStringPainted(true);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> requestCancelRun());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        if (JIPipeRunnableQueue.getInstance().getCurrentRun() != null) {
            progressBar.setString("Waiting for progress ...");
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setString("Nothing is currently running");
            progressBar.setIndeterminate(false);
            cancelButton.setEnabled(false);
        }
    }

    /**
     * Cancels the run
     */
    public void requestCancelRun() {
        cancelButton.setEnabled(false);
        if (JIPipeRunnableQueue.getInstance().getCurrentRun() != null) {
            JIPipeRunnableQueue.getInstance().cancel(JIPipeRunnableQueue.getInstance().getCurrentRun());
        }
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        cancelButton.setEnabled(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Waiting for progress ...");
    }

    /**
     * Triggered when a scheduled worker is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        cancelButton.setEnabled(false);
        progressBar.setString("Finished");
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(event.getStatus().getMaxProgress());
        progressBar.setValue(event.getStatus().getProgress());
        progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
        log.append(event.getStatus().render() + "\n");
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        cancelButton.setEnabled(false);
        progressBar.setString("Finished");
    }
}
