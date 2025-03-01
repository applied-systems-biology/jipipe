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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopRunnableQueueSpinnerIcon;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationButton;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that executes an {@link JIPipeRunnable}
 */
public class JIPipeDesktopRunExecuteUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener {
    private final JIPipeRunnableQueue queue;
    private final JIPipeRunnable run;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JButton closeButton;
    private JTextArea log;
    private JDialog dialog;

    public JIPipeDesktopRunExecuteUI(JIPipeDesktopWorkbench workbench, JIPipeRunnable run) {
        this(workbench, run, JIPipeRunnableQueue.getInstance());
    }

    /**
     * @param workbench the workbench
     * @param run       The runnable
     */
    public JIPipeDesktopRunExecuteUI(JIPipeDesktopWorkbench workbench, JIPipeRunnable run, JIPipeRunnableQueue queue) {
        super(workbench);
        this.run = run;
        this.queue = queue;
        initialize();
        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
        queue.getProgressEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);
    }

    public static void runInDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeRunnable run) {
        runInDialog(workbench, parent, run, JIPipeRunnableQueue.getInstance());
    }

    public static void runInDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeRunnable run, JIPipeRunnableQueue queue) {
        JDialog dialog = new JDialog();
        dialog.setTitle(run.getTaskLabel());
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopRunExecuteUI ui = new JIPipeDesktopRunExecuteUI(workbench, run, queue);
        ui.setDialog(dialog);
        dialog.setContentPane(ui);
        dialog.pack();
        dialog.revalidate();
        dialog.repaint();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        queue.getFinishedEventEmitter().subscribeLambdaOnce((emitter, event) -> {
            if (event.getRun() == run) {
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
        JScrollPane logScrollPane = new JScrollPane(log);
        add(logScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        progressBar = new JProgressBar();
        progressBar.setString("Ready");
        progressBar.setStringPainted(true);

        JLabel throbberLabel = new JLabel();
        throbberLabel.setIcon(new JIPipeDesktopRunnableQueueSpinnerIcon(throbberLabel));
        buttonPanel.add(throbberLabel);
        buttonPanel.add(Box.createHorizontalStrut(8));

        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        buttonPanel.add(new JIPipeDesktopGenericNotificationButton(getDesktopWorkbench(), run.getProgressInfo().getNotifications()));

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

    public JDialog getDialog() {
        return dialog;
    }

    public void setDialog(JDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        cancelButton.setEnabled(true);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
            progressBar.setString("Finished");
        }
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        if (event.getRun() == run) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(event.getStatus().getMaxProgress());
            progressBar.setValue(event.getStatus().getProgress());
            progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
            log.append(event.getStatus().render() + "\n");
        } else {
            log.append("[~] " + event.getStatus().render() + "\n");
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
            progressBar.setString("Finished");
        }
    }
}
