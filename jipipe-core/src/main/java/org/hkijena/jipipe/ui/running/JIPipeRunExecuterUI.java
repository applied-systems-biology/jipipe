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
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.components.icons.JIPipeRunnerQueueThrobberIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that executes an {@link JIPipeRunnable}
 */
public class JIPipeRunExecuterUI extends JPanel {
    private JIPipeRunnable run;
    private final JIPipeRunnerQueue queue;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JButton closeButton;
    private JScrollPane logScrollPane;
    private JTextArea log;
    private JDialog dialog;

    public JIPipeRunExecuterUI(JIPipeRunnable run) {
        this(run, JIPipeRunnerQueue.getInstance());
    }

    /**
     * @param run The runnable
     */
    public JIPipeRunExecuterUI(JIPipeRunnable run, JIPipeRunnerQueue queue) {
        this.run = run;
        this.queue = queue;
        initialize();
        queue.getEventBus().register(this);
    }

    public static void runInDialog(Component parent, JIPipeRunnable run) {
        runInDialog(parent, run, JIPipeRunnerQueue.getInstance());
    }

    public static void runInDialog(Component parent, JIPipeRunnable run, JIPipeRunnerQueue queue) {
        JDialog dialog = new JDialog();
        dialog.setTitle(run.getTaskLabel());
        dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        JIPipeRunExecuterUI ui = new JIPipeRunExecuterUI(run, queue);
        ui.setDialog(dialog);
        dialog.setContentPane(ui);
        dialog.pack();
        dialog.revalidate();
        dialog.repaint();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        queue.getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
                if (event.getRun() == run)
                    dialog.setVisible(false);
            }
        });
        queue.enqueue(run);
        dialog.setVisible(true);
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

        JLabel throbberLabel = new JLabel();
        throbberLabel.setIcon(new JIPipeRunnerQueueThrobberIcon(throbberLabel));
        buttonPanel.add(throbberLabel);
        buttonPanel.add(Box.createHorizontalStrut(8));

        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> requestCancelRun());
        buttonPanel.add(cancelButton);

        closeButton = new JButton("Close", UIUtils.getIconFromResources("actions/cancel.png"));
        closeButton.addActionListener(e -> dialog.setVisible(false));
        closeButton.setVisible(false);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Starts the run
     */
    public void startRun() {
        queue.enqueue(run);
        progressBar.setString("Waiting until other processes are finished ...");
        progressBar.setIndeterminate(true);
    }

    /**
     * Cancels the run
     */
    public void requestCancelRun() {
        cancelButton.setEnabled(false);
        queue.cancel(run);
    }

    private void switchToCloseButtonIfPossible() {
        if (dialog != null) {
            cancelButton.setEnabled(false);
            cancelButton.setVisible(false);
            closeButton.setVisible(true);
            revalidate();
            repaint();
        }
    }

    /**
     * Triggered when a scheduled worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
            progressBar.setString("Finished");
        }
    }

    /**
     * Triggered when a schedules worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
            progressBar.setString("Finished");
        }
    }

    @Subscribe
    public void onWorkerStart(JIPipeRunnable.StartedEvent event) {
        if (event.getRun() == run) {
            cancelButton.setEnabled(true);
        }
    }

    /**
     * Triggered when a worker reports progress
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerProgress(JIPipeRunnable.ProgressEvent event) {
        if (event.getRun() == run) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(event.getStatus().getMaxProgress());
            progressBar.setValue(event.getStatus().getProgress());
            progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
            log.append(event.getStatus().render() + "\n");
        }
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setDialog(JDialog dialog) {
        this.dialog = dialog;
    }
}
