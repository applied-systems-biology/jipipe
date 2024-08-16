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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.actions;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorRunManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties.JIPipeDesktopCompartmentGraphEditorResultsPanel;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.util.ArrayList;

public class JIPipeDesktopCompartmentsGraphEditorRunManager extends JIPipeDesktopGraphEditorRunManager {

    public JIPipeDesktopCompartmentsGraphEditorRunManager(JIPipeProject project, JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDesktopGraphNodeUI nodeUI, JIPipeDesktopDockPanel dockPanel) {
        super(project, canvasUI, nodeUI, dockPanel);
    }

    @Override
    protected void createValidationReport(JIPipeValidationReport report) {
        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) getNodeUI().getNode();
        for (JIPipeProjectCompartmentOutput compartmentOutput : compartment.getOutputNodes().values()) {
            getProject().reportValidity(new UnspecifiedValidationReportContext(), report,compartmentOutput);
        }
    }

    @Override
    protected void showResults() {
        getDockPanel().activatePanel(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, true);
        getDockPanel().getPanel(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_NODE_CONTEXT_RESULTS, JIPipeDesktopCompartmentGraphEditorResultsPanel.class).refreshTables();
    }

    @Override
    protected JIPipeDesktopQuickRun createRun(boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {

        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) getNodeUI().getNode();

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
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(getProject(), new ArrayList<>(compartment.getSortedOutputNodes()), settings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());
        return run;
    }
}
