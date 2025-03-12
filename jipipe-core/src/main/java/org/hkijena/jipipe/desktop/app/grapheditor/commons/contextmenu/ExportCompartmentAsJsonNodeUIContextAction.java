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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.api.compartments.JIPipeExportedCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ExportCompartmentAsJsonNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) selection.iterator().next().getNode();
        JIPipeExportedCompartment exportedCompartment = new JIPipeExportedCompartment(compartment);
        exportedCompartment.getMetadata().setName(compartment.getName());
        exportedCompartment.getMetadata().setDescription(new HTMLText("An exported JIPipe compartment"));
        JIPipeDesktopParameterFormPanel metadataEditor = new JIPipeDesktopParameterFormPanel(canvasUI.getDesktopWorkbench(), exportedCompartment.getMetadata(),
                null,
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING);

        if (JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(), metadataEditor, "Export compartment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Path selectedPath = JIPipeDesktop.saveFile(canvasUI, canvasUI.getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save JIPipe graph compartment (*.jipc)", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JIPC);
            if (selectedPath != null) {
                try {
                    exportedCompartment.saveToJson(selectedPath);
                    canvasUI.getDesktopWorkbench().sendStatusBarText("Exported compartment '" + compartment.getName() + "' to " + selectedPath);
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

}
