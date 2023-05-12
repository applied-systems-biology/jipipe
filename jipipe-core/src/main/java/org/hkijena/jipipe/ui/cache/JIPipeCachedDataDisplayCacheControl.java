package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;

/**
 * Interface that allows users to control refresh to cache and update thew current item within directly within the viewed data
 */
public class JIPipeCachedDataDisplayCacheControl implements Disposable,
        JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.StartedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeProjectWorkbench workbench;
    private final JToolBar toolBar;
    private final JIPipeGraphNode node;
    private JCheckBoxMenuItem cacheAwareToggle;

    private JCheckBoxMenuItem algorithmAwareToggle;
    private JButton updateCacheButton;
    private JIPipeRunnerQueueUI runnerQueue;

    public JIPipeCachedDataDisplayCacheControl(JIPipeProjectWorkbench workbench, JToolBar toolBar, JIPipeGraphNode node) {
        this.workbench = workbench;
        this.toolBar = toolBar;
        this.node = node;
        initialize();
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getStartedEventEmitter().subscribeWeak(this);
        node.getParameterChangedEventEmitter().subscribeWeak(this);
        updateRunnerQueueStatus();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        updateRunnerQueueStatus();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        updateRunnerQueueStatus();
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        updateRunnerQueueStatus();
    }

    @Override
    public void dispose() {
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().unsubscribe(this);
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().unsubscribe(this);
        JIPipeRunnerQueue.getInstance().getStartedEventEmitter().unsubscribe(this);
        node.getParameterChangedEventEmitter().unsubscribe(this);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent e) {
        if(!updateCacheButton.isDisplayable()) {
            return;
        }
        if (algorithmAwareToggle != null && algorithmAwareToggle.getState()) {
            workbench.runUpdateCacheLater(node);
        }
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
        updateCacheButton = new JButton("Update", UIUtils.getIconFromResources("actions/run-play.png"));
        updateCacheButton.setToolTipText("Updates the cache, so the currently viewed data is updated.");

        JPopupMenu menu = UIUtils.addPopupMenuToComponent(updateCacheButton);

        JMenuItem updateCacheItem = new JMenuItem("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are discarded.");
        updateCacheItem.addActionListener(e -> workbench.runUpdateCache(node));
        menu.add(updateCacheItem);

        JMenuItem cacheIntermediateResultsItem = new JMenuItem("Cache intermediate results", UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
        updateCacheButton.setToolTipText("Updates the node that contains this viewed data. Intermediate results are also cached.");
        cacheIntermediateResultsItem.addActionListener(e -> workbench.runCacheIntermediateResults(node));
        menu.add(cacheIntermediateResultsItem);

        menu.addSeparator();

        cacheAwareToggle = new JCheckBoxMenuItem("Auto-update from cache");
        cacheAwareToggle.setToolTipText("Keep up-to-date with cache.");
        cacheAwareToggle.setSelected(true);
        menu.add(cacheAwareToggle);

        algorithmAwareToggle = new JCheckBoxMenuItem("Update on parameter changes");
        algorithmAwareToggle.setToolTipText("If enabled, automatically update the cache when algorithm parameters change");
        menu.add(algorithmAwareToggle);

        runnerQueue = new JIPipeRunnerQueueUI(workbench);
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

    private JIPipeProject getProject() {
        return workbench.getProject();
    }

    public JIPipeGraphNode getNode() {
        return node;
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
