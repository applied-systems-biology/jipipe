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

package org.hkijena.pipelinej.ui.grapheditor.contextmenu;

import org.hkijena.pipelinej.api.compartments.ACAQExportedCompartment;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.pipelinej.extensions.settings.FileChooserSettings;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ExportCompartmentAsJsonAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        ACAQProjectCompartment compartment = (ACAQProjectCompartment) selection.iterator().next().getNode();
        ACAQExportedCompartment exportedCompartment = new ACAQExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription("An exported ACAQ5 compartment");
        ParameterPanel metadataEditor = new ParameterPanel(canvasUI.getWorkbench(), exportedCompartment.getMetadata(),
                null,
                ParameterPanel.WITH_SCROLLING);

        if (JOptionPane.showConfirmDialog(canvasUI, metadataEditor, "Export compartment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Path selectedPath = FileChooserSettings.saveFile(canvasUI, FileChooserSettings.KEY_PROJECT, "Save ACAQ5 graph compartment (*.json)", null);
            if (selectedPath != null) {
                try {
                    exportedCompartment.saveToJson(selectedPath);
                    canvasUI.getWorkbench().sendStatusBarText("Exported compartment '" + compartment.getName() + "' to " + selectedPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Export to *.json";
    }

    @Override
    public String getDescription() {
        return "Exports the compartment as *.json file that can be imported back into the compartment graph.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("export.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
