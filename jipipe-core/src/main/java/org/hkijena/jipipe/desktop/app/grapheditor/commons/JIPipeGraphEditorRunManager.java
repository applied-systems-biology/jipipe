package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

public class JIPipeGraphEditorRunManager implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private final JIPipeProject project;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDesktopGraphNodeUI nodeUI;
    private final JIPipeDesktopDockPanel dockPanel;
    private JIPipeRunnable run;
    private JIPipeDesktopDockPanel.State savedState;

    public JIPipeGraphEditorRunManager(JIPipeProject project, JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDesktopGraphNodeUI nodeUI, JIPipeDesktopDockPanel dockPanel) {
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

        if(getLogPanel().isAutoShowProgress()) {
            getDockPanel().activatePanel(JIPipeDesktopGraphEditorUI.DOCK_LOG, true);
        }

        // Validation step
        JIPipeValidationReport report = new JIPipeValidationReport();
        getProject().reportValidity(new UnspecifiedValidationReportContext(), report, nodeUI.getNode());
        if (!report.isEmpty()) {
            System.err.println("HANDLE ERRORS");
            return;
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
        return getDockPanel().getPanel(JIPipeDesktopGraphEditorUI.DOCK_LOG, JIPipeDesktopGraphEditorLogPanel.class);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() == run) {

            // Restore the saved state
            getDockPanel().restoreState(savedState);

            if(getLogPanel().isAutoShowResults()) {
                canvasUI.selectOnly(nodeUI);
                getDockPanel().activatePanel("_RESULTS", true);
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
                getDockPanel().activatePanel(JIPipeDesktopGraphEditorUI.DOCK_LOG, false);
            }
        }
    }
}
