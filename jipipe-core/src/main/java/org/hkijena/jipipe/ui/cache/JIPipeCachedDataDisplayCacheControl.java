package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueUI;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerStartedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Interface that allows users to control refresh to cache and update thew current item within directly within the viewed data
 */
public class JIPipeCachedDataDisplayCacheControl {

    private final JIPipeProjectWorkbench workbench;
    private final JToolBar toolBar;
    private final JIPipeGraphNode algorithm;
    private JCheckBoxMenuItem cacheAwareToggle;
    private JButton updateCacheButton;
    private JIPipeRunnerQueueUI runnerQueue;

    public JIPipeCachedDataDisplayCacheControl(JIPipeProjectWorkbench workbench, JToolBar toolBar, JIPipeGraphNode algorithm) {
        this.workbench = workbench;
        this.toolBar = toolBar;
        this.algorithm = algorithm;
        initialize();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        updateRunnerQueueStatus();
    }

    @Subscribe
    public void onRunnerFinished(RunUIWorkerFinishedEvent event) {
        updateRunnerQueueStatus();
    }

    @Subscribe
    public void onRunnerInterrupted(RunUIWorkerInterruptedEvent event) {
        updateRunnerQueueStatus();
    }

    @Subscribe
    public void onRunnerStarted(RunUIWorkerStartedEvent event) {
        updateRunnerQueueStatus();
    }

    private void updateRunnerQueueStatus() {
        if (JIPipeRunnerQueue.getInstance().isEmpty()) {
            updateCacheButton.setVisible(true);
            runnerQueue.setVisible(false);
        } else {
            updateCacheButton.setVisible(false);
            runnerQueue.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                toolBar.revalidate();
                toolBar.repaint();
            });
        }
    }

    private void initialize() {
        updateCacheButton = new JButton("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the cache, so the currently viewed data is updated.");

        JPopupMenu menu = UIUtils.addPopupMenuToComponent(updateCacheButton);

        JMenuItem updateCacheItem = new JMenuItem("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are discarded.");
        updateCacheItem.addActionListener(e -> runUpdateCache());
        menu.add(updateCacheItem);

        JMenuItem cacheIntermediateResultsItem = new JMenuItem("Cache intermediate results", UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are also cached.");
        cacheIntermediateResultsItem.addActionListener(e -> runCacheIntermediateResults());
        menu.add(cacheIntermediateResultsItem);

        menu.addSeparator();

        cacheAwareToggle = new JCheckBoxMenuItem("Keep up-to-date");
        cacheAwareToggle.setToolTipText("Keep up-to-date with cache.");
        cacheAwareToggle.setSelected(true);
        menu.add(cacheAwareToggle);

        runnerQueue = new JIPipeRunnerQueueUI();
    }

    public void installRefreshOnActivate(Runnable refreshFunction) {
        cacheAwareToggle.setSelected(true);
        cacheAwareToggle.addActionListener(e -> {
            if (cacheAwareToggle.isSelected()) {
                refreshFunction.run();
            }
        });
    }

    public boolean shouldRefreshToCache() {
        return cacheAwareToggle.isSelected();
    }

    private void runCacheIntermediateResults() {
        QuickRunSettings settings = new QuickRunSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(true);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    private JIPipeProject getProject() {
        return workbench.getProject();
    }

    private void runUpdateCache() {
        QuickRunSettings settings = new QuickRunSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(false);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }

    public void uninstall() {
        toolBar.remove(runnerQueue);
        toolBar.remove(updateCacheButton);
    }

    public void install() {
        toolBar.add(runnerQueue, 0);
        toolBar.add(updateCacheButton, 0);
    }
}
