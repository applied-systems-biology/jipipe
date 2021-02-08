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

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.api.compartments.JIPipeExportedCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ExportCompartmentAsJsonNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) selection.iterator().next().getNode();
        JIPipeExportedCompartment exportedCompartment = new JIPipeExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription(new HTMLText("An exported JIPipe compartment"));
        ParameterPanel metadataEditor = new ParameterPanel(canvasUI.getWorkbench(), exportedCompartment.getMetadata(),
                null,
                ParameterPanel.WITH_SCROLLING);

        if (JOptionPane.showConfirmDialog(canvasUI.getWorkbench().getWindow(), metadataEditor, "Export compartment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Path selectedPath = FileChooserSettings.saveFile(canvasUI, FileChooserSettings.KEY_PROJECT, "Save JIPipe graph compartment (*.jipc)", UIUtils.EXTENSION_FILTER_JIPC);
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
        return "Export to *.jipc";
    }

    @Override
    public String getDescription() {
        return "Exports the compartment as *.jipc file that can be imported back into the compartment graph.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/document-export.png");
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
