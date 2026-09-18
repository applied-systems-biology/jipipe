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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunQueueLoggerPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * UI that executes an {@link JIPipeRunnable} and shows the progress in a dialog
 */
public class JIPipeDesktopRunExecuteUI extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private static final Logger log = LoggerFactory.getLogger(JIPipeDesktopRunExecuteUI.class);
    private final JIPipeRunnableExecutor queue;
    private final JIPipeRunnable run;
    private final JIPipeDesktopRunQueueLoggerPanel loggerPanel;
    private JButton closeButton;
    private JDialog dialog;

    public JIPipeDesktopRunExecuteUI(JIPipeDesktopWorkbench workbench, JIPipeRunnable run) {
        this(workbench, run, JIPipeQueuedRunnableExecutor.getInstance());
    }

    /**
     * @param workbench the workbench
     * @param run       The runnable
     */
    public JIPipeDesktopRunExecuteUI(JIPipeDesktopWorkbench workbench, JIPipeRunnable run, JIPipeRunnableExecutor queue) {
        super(workbench);
        this.run = run;
        this.queue = queue;
        this.loggerPanel = new JIPipeDesktopRunQueueLoggerPanel(workbench, queue);
        this.loggerPanel.setTargetRun(run);
        initialize();
        queue.getFinishedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
    }

    public static void runInDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeRunnable run) {
        runInDialog(workbench, parent, run, JIPipeQueuedRunnableExecutor.getInstance(), GlobalLogMode.OnlyFailures);
    }

    public static void runInDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeRunnable run, JIPipeRunnableExecutor queue, GlobalLogMode logMode) {
        JDialog dialog = new JDialog();
        dialog.setTitle(run.getTaskLabel());
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        JIPipeDesktopRunExecuteUI ui = new JIPipeDesktopRunExecuteUI(workbench, run, queue);
        ui.setBorder(UIUtils.createEmptyBorder(8));
        ui.setDialog(dialog);
        contentPane.add(ui, BorderLayout.CENTER);
        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.revalidate();
        dialog.repaint();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        queue.getFinishedEventEmitter().subscribeLambdaOnce((emitter, event) -> {
            if (event.getRun() == run) {
                if (logMode == GlobalLogMode.Everything && queue != JIPipeQueuedRunnableExecutor.getInstance()) {
                    JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, true);
                }
                dialog.setVisible(false);
            }
        });
        if(logMode == GlobalLogMode.Everything || logMode == GlobalLogMode.OnlyFailures) {
            queue.getInterruptedEventEmitter().subscribeLambdaOnce((emitter, event) -> {
                if (queue != JIPipeQueuedRunnableExecutor.getInstance()) {
                    JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, false);
                }
            });
        }
        queue.enqueue(run);
        dialog.setVisible(true);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        add(loggerPanel, BorderLayout.CENTER);

        loggerPanel.addDefaultCancelButton();

        // Create and add close button
        closeButton = new JButton("Close", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
        closeButton.addActionListener(e -> dialog.setVisible(false));
        closeButton.setVisible(false);
        loggerPanel.addButton(closeButton);
    }

    /**
     * Starts the run
     */
    public void startRun() {
        queue.enqueue(run);
    }

    /**
     * Cancels the run
     */
    public void requestCancelRun() {
        queue.cancel(run);
    }

    private void switchToCloseButtonIfPossible() {
        if (dialog != null) {
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
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == run) {
            switchToCloseButtonIfPossible();
        }
    }

    public enum GlobalLogMode {
        None,
        OnlyFailures,
        Everything
    }
}
