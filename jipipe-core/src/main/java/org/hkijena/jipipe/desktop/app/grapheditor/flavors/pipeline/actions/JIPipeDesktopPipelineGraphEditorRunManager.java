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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions;

import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

public class JIPipeDesktopPipelineGraphEditorRunManager implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private final JIPipeProject project;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDesktopGraphNodeUI nodeUI;
    private final JIPipeDesktopDockPanel dockPanel;
    private JIPipeRunnable run;
    private JIPipeDesktopDockPanel.State savedState;

    public JIPipeDesktopPipelineGraphEditorRunManager(JIPipeProject project, JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDesktopGraphNodeUI nodeUI, JIPipeDesktopDockPanel dockPanel) {
        this.project = project;
        this.canvasUI = canvasUI;
        this.nodeUI = nodeUI;
        this.dockPanel = dockPanel;
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    public void run(boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {
        if(run != null) {
            throw new IllegalStateException("Already scheduled a run!");
        }

        // Remember the saved state
        savedState = getDockPanel().getCurrentState();

        // Validation step
        JIPipeValidationReport report = new JIPipeValidationReport();
        getProject().reportValidity(new UnspecifiedValidationReportContext(), report, nodeUI.getNode());
        if (!report.isEmpty()) {
            dockPanel.getPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, JIPipeDesktopGraphEditorErrorPanel.class).setItems(report);
            dockPanel.activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, false);
            return;
        }

        if(getLogPanel().isAutoShowProgress()) {
            getDockPanel().activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, true);
        }

        // Generate settings
        JIPipeDesktopQuickRunSettings settings;
        if(saveToDisk) {
            settings = new JIPipeDesktopQuickRunSettings(getProject());
            settings.setSaveToDisk(true);
            settings.setExcludeSelected(false);
            settings.setLoadFromCache(true);
            settings.setStoreToCache(false);
            settings.setStoreIntermediateResults(storeIntermediateResults);
        }
        else {
            settings = new JIPipeDesktopQuickRunSettings(getProject());
            settings.setSaveToDisk(false);
            settings.setExcludeSelected(excludeSelected);
            settings.setLoadFromCache(true);
            settings.setStoreToCache(true);
            settings.setStoreIntermediateResults(storeIntermediateResults);
        }

        // Run
        run = new JIPipeDesktopQuickRun(getProject(), nodeUI.getNode(), settings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());
        JIPipeRunnableQueue.getInstance().enqueue(run);
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }

    public JIPipeDesktopGraphNodeUI getNodeUI() {
        return nodeUI;
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    private JIPipeDesktopGraphEditorLogPanel getLogPanel() {
        return getDockPanel().getPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, JIPipeDesktopGraphEditorLogPanel.class);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() == run) {

            // Restore the saved state
            getDockPanel().restoreState(savedState);

            if(getLogPanel().isAutoShowResults()) {
                canvasUI.selectOnly(nodeUI);
                getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, true);
                getDockPanel().getPanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, JIPipeDesktopAlgorithmCacheBrowserUI.class).refreshTable();
            }
        }
    }


    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if(event.getRun() == run) {
            // Restore the saved state
            getDockPanel().restoreState(savedState);

            if(getLogPanel().isAutoShowResults()) {
                canvasUI.selectOnly(nodeUI);

                if(event.getException() != null) {
                    dockPanel.getPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, JIPipeDesktopGraphEditorErrorPanel.class).setItems(event.getException());
                    dockPanel.activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, false);
                }
                else {
                    getDockPanel().activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, false);
                }
            }
        }
    }
}
