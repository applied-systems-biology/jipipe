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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A tool that automatically runs 'update cache' when any parameter or graph property is changed
 */
public class RealTimeProjectRunner extends JIPipeProjectWorkbenchPanel {

    private final RuntimeSettings runtimeSettings = RuntimeSettings.getInstance();
    private JIPipeProjectRun currentRun;
    private final Timer timer = new Timer(RuntimeSettings.getInstance().getRealTimeRunDelay(), e -> scheduleRun());

    /**
     * @param workbenchUI The workbench UI
     */
    public RealTimeProjectRunner(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        timer.setRepeats(false);
        getProject().getGraph().getEventBus().register(this);
        refreshEventRegistrations();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        runtimeSettings.getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if ("real-time-run-enabled".equals(event.getKey()) && runtimeSettings.isRealTimeRunEnabled()) {
                    scheduleRun();
                }
            }
        });
    }

    public JToggleButton createToggleButton() {
        JToggleButton button = new JToggleButton("Auto update", UIUtils.getIconFromResources("actions/quickopen-function.png"));
        button.setSelected(runtimeSettings.isRealTimeRunEnabled());
        button.setToolTipText("Enable/disable real-time update. If enabled, any changes to the graph or parameters trigger a cache update.");
        button.addActionListener(e -> {
            if (button.isSelected() != runtimeSettings.isRealTimeRunEnabled()) {
                runtimeSettings.setRealTimeRunEnabled(button.isSelected());
                runtimeSettings.getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(runtimeSettings, "real-time-run-enabled"));
            }
        });
        return button;
    }

    private void refreshEventRegistrations() {
        for (JIPipeGraphNode node : getProject().getGraph().getGraphNodes()) {
            node.getEventBus().register(this);
        }
    }

    @Subscribe
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        refreshEventRegistrations();
        scheduleRunTimer();
    }

    @Subscribe
    public void onNodeSlotsChanged(JIPipeSlotConfiguration.SlotsChangedEvent event) {
        scheduleRunTimer();
    }

    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("jipipe:node:name".equals(event.getKey()) || "jipipe:node:description".equals(event.getKey()))
            return;
        scheduleRunTimer();
    }

    private void scheduleRunTimer() {
        if (!runtimeSettings.isRealTimeRunEnabled()) {
            return;
        }
        timer.restart();
    }

    public void scheduleRun() {
        if (!runtimeSettings.isRealTimeRunEnabled()) {
            return;
        }
        getProject().getCache().clearOutdated(new JIPipeProgressInfo());
        if (currentRun != null)
            JIPipeRunnerQueue.getInstance().cancel(currentRun);
        currentRun = null;
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setOutputPath(RuntimeSettings.generateTempDirectory("real-time-run"));
        settings.setNumThreads(RuntimeSettings.getInstance().getDefaultQuickRunThreads());
        settings.setLoadFromCache(true);
        settings.setStoreToCache(true);
        settings.setSaveToDisk(false);
        settings.setSilent(true);
        currentRun = new JIPipeProjectRun(getProject(), settings);
        JIPipeRunnerQueue.getInstance().enqueue(currentRun);
    }

    @Subscribe
    public void onRunStarted(RunWorkerStartedEvent event) {
        if (event.getRun() == currentRun) {
            if (!runtimeSettings.isRealTimeRunEnabled()) {
                return;
            }
        }
    }

    @Subscribe
    public void onRunFinished(RunWorkerFinishedEvent event) {
        if (event.getRun() == currentRun) {
            getWorkbench().sendStatusBarText("Real-time: Update finished");
        }
    }

    @Subscribe
    public void onRunCancelled(RunWorkerInterruptedEvent event) {
        if (event.getRun() == currentRun) {
            getWorkbench().sendStatusBarText("Real-time: Update failed");
        }
    }

}
