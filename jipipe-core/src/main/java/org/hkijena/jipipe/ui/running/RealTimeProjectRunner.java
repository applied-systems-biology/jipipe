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
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.events.SlotsChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A tool that automatically runs 'update cache' when any parameter or graph property is changed
 */
public class RealTimeProjectRunner extends JIPipeProjectWorkbenchPanel {

    private final RuntimeSettings runtimeSettings = RuntimeSettings.getInstance();
    private JIPipeRun currentRun;
    private Timer timer = new Timer(RuntimeSettings.getInstance().getRealTimeRunDelay(), e -> scheduleRun());

    /**
     * @param workbenchUI The workbench UI
     */
    public RealTimeProjectRunner(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        timer.setRepeats(false);
        getProject().getGraph().getEventBus().register(this);
        refreshEventRegistrations();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    public JToggleButton createToggleButton() {
        JToggleButton button = new JToggleButton("Auto update", UIUtils.getIconFromResources("actions/quickopen-function.png"));
        button.setSelected(runtimeSettings.isRealTimeRunEnabled());
        button.setToolTipText("Enable/disable real-time update. If enabled, any changes to the graph or parameters trigger a cache update.");
        button.addActionListener(e -> {
            if(button.isSelected() != runtimeSettings.isRealTimeRunEnabled()) {
                runtimeSettings.setRealTimeRunEnabled(button.isSelected());
                runtimeSettings.getEventBus().post(new ParameterChangedEvent(runtimeSettings, "real-time-run-enabled"));
            }
        });
        return button;
    }

    private void refreshEventRegistrations() {
        for (JIPipeGraphNode node : getProject().getGraph().getNodes().values()) {
            node.getEventBus().register(this);
        }
    }

    @Subscribe
    public void onGraphChanged(GraphChangedEvent event) {
        refreshEventRegistrations();
        scheduleRunTimer();
    }

    @Subscribe
    public void onNodeSlotsChanged(SlotsChangedEvent event) {
        scheduleRunTimer();
    }

    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if("jipipe:node:name".equals(event.getKey()) || "jipipe:node:description".equals(event.getKey()))
            return;
        scheduleRunTimer();
    }

    private void scheduleRunTimer() {
        if(!runtimeSettings.isRealTimeRunEnabled()) {
            return;
        }
        timer.restart();
    }

    public void scheduleRun() {
        if(!runtimeSettings.isRealTimeRunEnabled()) {
            return;
        }
        getProject().getCache().autoClean(true, true);
        if(currentRun != null)
            JIPipeRunnerQueue.getInstance().cancel(currentRun);
        currentRun = null;
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setNumThreads(RuntimeSettings.getInstance().getDefaultTestBenchThreads());
        settings.setLoadFromCache(true);
        settings.setStoreToCache(true);
        settings.setSaveOutputs(false);
        settings.setSilent(true);
        currentRun = new JIPipeRun(getProject(), settings);
        JIPipeRunnerQueue.getInstance().enqueue(currentRun);
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() == currentRun) {
            getWorkbench().sendStatusBarText("Real-time: Update finished");
        }
    }

    @Subscribe
    public void onRunCancelled(RunUIWorkerInterruptedEvent event) {
        if(event.getRun() == currentRun) {
            getWorkbench().sendStatusBarText("Real-time: Update failed");
        }
    }

}
