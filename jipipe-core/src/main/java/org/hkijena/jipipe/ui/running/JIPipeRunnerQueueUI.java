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
import org.hkijena.jipipe.ui.components.ThrobberIcon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * UI that monitors the queue
 */
public class JIPipeRunnerQueueUI extends JPanel {

    private JPanel emptyQueuePanel;
    private JPanel runningQueuePanel;
    private JProgressBar runningQueueProgress;
    private JLabel throbberLabel;
    private ThrobberIcon throbberIcon;

    /**
     * Creates new instance
     */
    public JIPipeRunnerQueueUI() {
        initialize();
        updateStatus();

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setMaximumSize(new Dimension(200, 32));
        setLayout(new BorderLayout());
        setOpaque(false);

        // UI for empty queue
        emptyQueuePanel = new JPanel(new BorderLayout());
        emptyQueuePanel.setOpaque(false);
        emptyQueuePanel.add(new JLabel("No processes are running", UIUtils.getIconFromResources("actions/media-pause.png"), JLabel.LEFT),
                BorderLayout.EAST);
        emptyQueuePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15))));

        // UI for running queue
        runningQueuePanel = new JPanel();
        runningQueuePanel.setOpaque(false);
        runningQueuePanel.setLayout(new BoxLayout(runningQueuePanel, BoxLayout.X_AXIS));
        runningQueuePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2),
                        BorderFactory.createEmptyBorder(5, 15, 5, 15))));
        throbberLabel = new JLabel();
        throbberIcon = new ThrobberIcon(throbberLabel, UIUtils.getIconFromResources("status/throbber.png"), 80, 24);
        throbberLabel.setIcon(throbberIcon);
        runningQueuePanel.add(throbberLabel);
        runningQueuePanel.add(Box.createHorizontalStrut(2));
        runningQueueProgress = new JProgressBar();
        runningQueuePanel.add(runningQueueProgress);
        JButton cancelButton = new JButton(UIUtils.getIconFromResources("actions/cancel.png"));
        UIUtils.makeBorderlessWithoutMargin(cancelButton);
        cancelButton.setToolTipText("Cancel");
        cancelButton.addActionListener(e -> cancelRun());
        runningQueuePanel.add(Box.createHorizontalStrut(4));
        runningQueuePanel.add(cancelButton);
    }

    private void cancelRun() {
        if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null)
            JIPipeRunnerQueue.getInstance().cancel(JIPipeRunnerQueue.getInstance().getCurrentRun());
    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null) {
            removeAll();
            add(runningQueuePanel, BorderLayout.EAST);
            throbberIcon.start();
            revalidate();
            repaint();
        } else {
            removeAll();
            add(emptyQueuePanel, BorderLayout.EAST);
            throbberIcon.stop();
            revalidate();
            repaint();
        }
    }

    /**
     * Triggered when a worker is started
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerStarted(RunUIWorkerStartedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(RunUIWorkerProgressEvent event) {
        runningQueueProgress.setMaximum(event.getStatus().getMaxProgress());
        runningQueueProgress.setValue(event.getStatus().getProgress());
        if (event.getStatus().getMessage() != null)
            runningQueueProgress.setToolTipText("(" + runningQueueProgress.getValue() + "/" + runningQueueProgress.getMaximum() + ") " +
                    event.getStatus().getMessage());
    }

}
