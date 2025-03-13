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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeDesktopCompactRunnableQueueButton extends JButton implements JIPipeDesktopWorkbenchAccess, JIPipeRunnable.StartedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.EnqeuedEventListener {

    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final JIPipeRunnableQueue runnerQueue;
    private final Icon iconInactive;
    private final Icon iconActive;
    private final JPopupMenu menu = new JPopupMenu();
    private boolean processAlreadyQueued;
    private boolean showProgress;
    private int lastProgress;
    private int lastMaxProgress;

    public JIPipeDesktopCompactRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench, String iconName) {
        this(desktopWorkbench, JIPipeRunnableQueue.getInstance(), UIUtils.getIconInvertedFromResources(iconName), UIUtils.getIconFromResources(iconName));
    }

    public JIPipeDesktopCompactRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableQueue runnerQueue, String iconName) {
        this(desktopWorkbench, runnerQueue, UIUtils.getIconInvertedFromResources(iconName), UIUtils.getIconFromResources(iconName));
    }

    public JIPipeDesktopCompactRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench, Icon iconInactive, Icon iconActive) {
        this(desktopWorkbench, JIPipeRunnableQueue.getInstance(), iconInactive, iconActive);
    }

    public JIPipeDesktopCompactRunnableQueueButton(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableQueue runnerQueue, Icon iconInactive, Icon iconActive) {
        this.desktopWorkbench = desktopWorkbench;
        this.runnerQueue = runnerQueue;
        this.iconInactive = iconInactive;
        this.iconActive = iconActive;
        setToolTipText(runnerQueue.getName());
        initialize();
        updateStatus();

        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        runnerQueue.getProgressEventEmitter().subscribeWeak(this);
        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getEnqueuedEventEmitter().subscribeWeak(this);
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return desktopWorkbench;
    }

    private void initialize() {
        setIcon(iconInactive);
        UIUtils.makeButtonFlat25x25(this);
        UIUtils.addReloadablePopupMenuToButton(this, menu, this::reloadMenu);
    }

    public JIPipeRunnableQueue getRunnerQueue() {
        return runnerQueue;
    }

    private void reloadMenu() {
        menu.removeAll();

        if (runnerQueue.size() > 0) {

            JMenuItem cancelAllItem = new JMenuItem("Cancel all tasks", UIUtils.getIcon32FromResources("actions/stock_calc-cancel.png"));
            cancelAllItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            cancelAllItem.setToolTipText("Cancels all running and queued tasks");
            cancelAllItem.addActionListener(e -> {
                runnerQueue.clearQueue();
                JIPipeRunnable currentRun = runnerQueue.getCurrentRun();
                runnerQueue.cancel(currentRun);
            });
            menu.add(cancelAllItem);

            if (runnerQueue.size() > 1) {
                JMenuItem cancelQueuedItem = new JMenuItem("Cancel only enqueued tasks", UIUtils.getIcon32FromResources("actions/rabbitvcs-clear.png"));
                cancelQueuedItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
                cancelQueuedItem.setToolTipText("Cancels enqueued tasks. Currently running operations are not cancelled.");
                cancelQueuedItem.addActionListener(e -> {
                    runnerQueue.clearQueue();
                });
                menu.add(cancelQueuedItem);
            }

            menu.addSeparator();

            JIPipeRunnableWorker currentRun = runnerQueue.getCurrentRunWorker();
            if (currentRun != null) {
                menu.add(new JIPipeDesktopRunnableQueueButtonMenuItem(runnerQueue, currentRun));
            }
            for (JIPipeRunnableWorker runWorker : runnerQueue.getQueue()) {
                menu.add(new JIPipeDesktopRunnableQueueButtonMenuItem(runnerQueue, runWorker));
            }
        } else {
            JMenuItem noTasksItem = new JMenuItem("There are currently no tasks running", UIUtils.getIcon32FromResources("emblems/vcs-normal.png"));
            noTasksItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
            menu.add(noTasksItem);
        }

//        if (workbench instanceof JIPipeProjectWorkbench && isOnGlobalRunnerQueue()) {
//            menu.addSeparator();
//            JMenuItem openLogsItem = new JMenuItem("Open logs", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"));
//            openLogsItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
//            openLogsItem.addActionListener(e -> workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG));
//            menu.add(openLogsItem);
//        }
    }

    private boolean isOnGlobalRunnerQueue() {
        return runnerQueue == JIPipeRunnableQueue.getInstance();
    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        JIPipeRunnable currentRun = runnerQueue.getCurrentRun();
        if (currentRun != null) {
            processAlreadyQueued = true;
            showProgress = true;
            lastProgress = currentRun.getProgressInfo().getProgress();
            lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
            setIcon(iconActive);
            repaint();
        } else {
            showProgress = false;
            setIcon(iconInactive);
            repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (showProgress) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(2, getHeight() - 4, getWidth() - 4, 2);
            if (lastMaxProgress > 0) {
                double perc = 1.0 * lastProgress / lastMaxProgress;
                if (perc < 0)
                    perc = 0;
                if (perc > 1)
                    perc = 1;
                g.setColor(JIPipeDesktopModernMetalTheme.PRIMARY5);
                g.fillRect(2, getHeight() - 4, (int) ((getWidth() - 4) * perc), 2);
            }
        }
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        JIPipeRunnable currentRun = event.getRun();
        lastProgress = currentRun.getProgressInfo().getProgress();
        lastMaxProgress = currentRun.getProgressInfo().getMaxProgress();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        updateStatus();
    }

    @Override
    public void onRunnableEnqueued(JIPipeRunnable.EnqueuedEvent event) {
        updateStatus();
    }

}
