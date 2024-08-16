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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentGraphEditorResultsPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

public class JIPipeDesktopPipelineGraphEditorRunManager extends JIPipeDesktopGraphEditorRunManager {

    public JIPipeDesktopPipelineGraphEditorRunManager(JIPipeProject project, JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDesktopGraphNodeUI nodeUI, JIPipeDesktopDockPanel dockPanel) {
        super(project, canvasUI, nodeUI, dockPanel);
    }

    @Override
    protected void createValidationReport(JIPipeValidationReport report) {
        getProject().reportValidity(new UnspecifiedValidationReportContext(), report, getNodeUI().getNode());
    }

    @Override
    protected void showResults() {
        getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, true);
        getDockPanel().getPanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, JIPipeDesktopAlgorithmCacheBrowserUI.class).refreshTable();
    }

    @Override
    protected JIPipeRunnable createRun(boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {
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
        JIPipeRunnable run = new JIPipeDesktopQuickRun(getProject(), getNodeUI().getNode(), settings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());
        return run;
    }
}
