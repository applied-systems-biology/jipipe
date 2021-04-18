package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.testbench.JIPipeTestBench;
import org.hkijena.jipipe.api.testbench.JIPipeTestBenchSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.running.*;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Interface that allows users to control refresh to cache and update thew current item within directly within the viewed data
 */
public class JIPipeCachedDataDisplayCacheControl {

    private final JIPipeProjectWorkbench workbench;
    private final JToolBar toolBar;
    private final JIPipeGraphNode algorithm;
    private JToggleButton cacheAwareToggle;
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
            cacheAwareToggle.setVisible(true);
            runnerQueue.setVisible(false);
        } else {
            updateCacheButton.setVisible(false);
            cacheAwareToggle.setVisible(false);
            runnerQueue.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                toolBar.revalidate();
                toolBar.repaint();
            });
        }
    }

    private void initialize() {
        cacheAwareToggle = new JToggleButton("Refresh to cache", UIUtils.getIconFromResources("actions/view-refresh.png"));
        cacheAwareToggle.setSelected(true);

        updateCacheButton = new JButton(UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Update this data");

        JPopupMenu menu = UIUtils.addPopupMenuToComponent(updateCacheButton);

        JMenuItem updateCacheItem = new JMenuItem("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are discarded.");
        updateCacheItem.addActionListener(e -> runUpdateCache());
        menu.add(updateCacheItem);

        JMenuItem cacheIntermediateResultsItem = new JMenuItem("Cache intermediate results", UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are also cached.");
        cacheIntermediateResultsItem.addActionListener(e -> runCacheIntermediateResults());
        menu.add(cacheIntermediateResultsItem);

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
        JIPipeTestBenchSettings settings = new JIPipeTestBenchSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(true);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        JIPipeTestBench testBench = new JIPipeTestBench(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    private JIPipeProject getProject() {
        return workbench.getProject();
    }

    private void runUpdateCache() {
        JIPipeTestBenchSettings settings = new JIPipeTestBenchSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(false);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        JIPipeTestBench testBench = new JIPipeTestBench(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }

    public void install() {
        toolBar.add(runnerQueue, 0);
        toolBar.add(cacheAwareToggle, 0);
        toolBar.add(updateCacheButton, 0);
    }
}
