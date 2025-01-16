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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterKeyPickerUI;
import org.hkijena.jipipe.plugins.multiparameters.nodes.DefineParametersTableAlgorithm;
import org.hkijena.jipipe.plugins.parameters.library.table.ParameterTable;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

public class AddNewParameterSetNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {

        List<JIPipeDesktopParameterKeyPickerUI.ParameterEntry> result = JIPipeDesktopParameterKeyPickerUI.showPickerDialog(canvasUI,
                "Add parameter sets node",
                canvasUI.getVisibleNodes(),
                null);

        if (!result.isEmpty()) {
            ParameterTable table = new ParameterTable();
            for (JIPipeDesktopParameterKeyPickerUI.ParameterEntry entry : result) {
                ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn(entry.getName(), entry.getKey(), entry.getFieldClass());
                table.addColumn(column, entry.getInitialValue());
            }
            table.addRow();

            DefineParametersTableAlgorithm node = JIPipe.createNode(DefineParametersTableAlgorithm.class);
            node.setParameterTable(table);
            canvasUI.getGraph().insertNode(node, canvasUI.getCompartmentUUID());
        }
    }

    @Override
    public String getName() {
        return "Add parameter sets here ...";
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true);
    }

    @Override
    public String getDescription() {
        return "Opens a dialog that allows to add a parameter set node at the specified location";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("data-types/parameters.png");
    }
}
