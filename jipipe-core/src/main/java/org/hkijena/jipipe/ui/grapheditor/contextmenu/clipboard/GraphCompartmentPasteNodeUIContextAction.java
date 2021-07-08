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

package org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.JIPipeExportedCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

import static org.hkijena.jipipe.utils.UIUtils.getStringFromClipboard;

public class GraphCompartmentPasteNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !StringUtils.isNullOrEmpty(UIUtils.getStringFromClipboard());
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
            return;
        try {
            String json = getStringFromClipboard();
            if (json != null) {
                JIPipeProject project = ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getProject();
                TypeReference<List<JIPipeExportedCompartment>> typeReference = new TypeReference<List<JIPipeExportedCompartment>>() {
                };
                List<JIPipeExportedCompartment> compartments = JsonUtils.getObjectMapper().readValue(json, typeReference);
                if (compartments.isEmpty()) {
                    throw new NullPointerException("Empty compartment list pasted.");
                }
                for (JIPipeExportedCompartment compartment : compartments) {
                    String newId = compartment.getSuggestedName();
                    JIPipeProjectCompartment compartmentNode = compartment.addTo(project, newId);
                    JIPipeNodeUI ui = canvasUI.getNodeUIs().getOrDefault(compartmentNode, null);
                    if (ui != null) {
                        canvasUI.autoPlaceCloseToCursor(ui);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(canvasUI.getWorkbench().getWindow(), "The current clipboard contents are no valid compartments.", "Paste compartment", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Paste";
    }

    @Override
    public String getDescription() {
        return "Copies compartments from clipboard into the current graph";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-paste.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK, true);
    }
}
