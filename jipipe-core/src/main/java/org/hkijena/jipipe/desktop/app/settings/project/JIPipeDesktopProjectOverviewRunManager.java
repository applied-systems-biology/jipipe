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

package org.hkijena.jipipe.desktop.app.settings.project;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopProjectOverviewUI;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.util.ArrayList;
import java.util.Collections;

public class JIPipeDesktopProjectOverviewRunManager implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private final JIPipeProject project;
    private final JIPipeDesktopDockPanel dockPanel;
    private final JIPipeDesktopProjectOverviewUI projectOverviewUI;
    private final JIPipeAlgorithm node;
    private final boolean allowChangePanels;
    private JIPipeRunnable run;
    private JIPipeDesktopDockPanel.State savedState;
    private boolean queueMode;
    private boolean restoreDockStateRequired;

    public JIPipeDesktopProjectOverviewRunManager(JIPipeProject project, JIPipeDesktopDockPanel dockPanel, JIPipeDesktopProjectOverviewUI projectOverviewUI, JIPipeAlgorithm node, boolean allowChangePanels) {
        this.project = project;
        this.dockPanel = dockPanel;
        this.projectOverviewUI = projectOverviewUI;
        this.node = node;
        this.allowChangePanels = allowChangePanels;
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    public void run(boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {
        if (run != null) {
            throw new IllegalStateException("Already scheduled a run!");
        }

        // Remember the saved state
        savedState = getDockPanel().getCurrentState();
        queueMode = alreadyHasRunEnqueued();
        restoreDockStateRequired = (getLogPanel().isAutoShowProgress() || getLogPanel().isAutoShowResults()) && allowChangePanels;

        // Validation step
        JIPipeValidationReport report = new JIPipeValidationReport();
        createValidationReport(report);
        if (!report.isEmpty()) {
            dockPanel.getPanelComponent(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, JIPipeDesktopGraphEditorErrorPanel.class).setItems(report);
            dockPanel.activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, false);
            return;
        } else {
            dockPanel.getPanelComponent(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, JIPipeDesktopGraphEditorErrorPanel.class).clearItems();
        }

        if (getLogPanel().isAutoShowProgress() && allowChangePanels) {
            getDockPanel().activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, true);
        }

        // Create an enqueue the run
        run = createRun(saveToDisk, storeIntermediateResults, excludeSelected);
        JIPipeRunnableQueue.getInstance().enqueue(run);
    }

    private boolean alreadyHasRunEnqueued() {
        JIPipeRunnable currentRun = JIPipeRunnableQueue.getInstance().getCurrentRun();
        if (currentRun instanceof JIPipeDesktopQuickRun) {
            if (((JIPipeDesktopQuickRun) currentRun).getProject() == project) {
                return true;
            }
        }
        for (JIPipeRunnableWorker worker : JIPipeRunnableQueue.getInstance().getQueue()) {
            if (worker.getRun() instanceof JIPipeDesktopQuickRun && ((JIPipeDesktopQuickRun) worker.getRun()).getProject() == project) {
                return true;
            }
        }
        return false;
    }

    private void createValidationReport(JIPipeValidationReport report) {
        node.reportValidity(new UnspecifiedValidationReportContext(), report);
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    protected JIPipeDesktopGraphEditorLogPanel getLogPanel() {
        return getDockPanel().getPanelComponent(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, JIPipeDesktopGraphEditorLogPanel.class);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == run) {

            // Restore the saved state
            if (!queueMode) {
                if (restoreDockStateRequired) {
                    getDockPanel().restoreState(savedState);
                }

                if (getLogPanel().isAutoShowResults() && allowChangePanels) {
                    showResults();
                }
            }
        }
    }

    private void showResults() {
        projectOverviewUI.showResults(node);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == run) {
            if (!queueMode) {
                // Restore the saved state
                if (restoreDockStateRequired) {
                    getDockPanel().restoreState(savedState);
                }

                if (getLogPanel().isAutoShowResults()) {
                    if (event.getException() != null) {
                        dockPanel.getPanelComponent(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, JIPipeDesktopGraphEditorErrorPanel.class).setItems(event.getException());
                        dockPanel.activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS, false);
                    } else {
                        getDockPanel().activatePanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG, false);
                    }
                }
            }
        }
    }

    private JIPipeDesktopQuickRun createRun(boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {

        // Generate settings
        JIPipeDesktopQuickRunSettings settings;
        if (saveToDisk) {
            settings = new JIPipeDesktopQuickRunSettings(getProject());
            settings.setSaveToDisk(true);
            settings.setExcludeSelected(false);
            settings.setLoadFromCache(true);
            settings.setStoreToCache(false);
            settings.setStoreIntermediateResults(storeIntermediateResults);
        } else {
            settings = new JIPipeDesktopQuickRunSettings(getProject());
            settings.setSaveToDisk(false);
            settings.setExcludeSelected(excludeSelected);
            settings.setLoadFromCache(true);
            settings.setStoreToCache(true);
            settings.setStoreIntermediateResults(storeIntermediateResults);
        }

        // Run
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(getProject(), Collections.singletonList(node), settings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());
        return run;
    }
}
