package org.hkijena.jipipe.extensions.tables.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData}
 */
public class ResultsTableDataParameterEditorUI extends JIPipeParameterEditorUI {
    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public ResultsTableDataParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editButton = new JButton("Edit table", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.addActionListener(e -> editParameters());
        add(editButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        ResultsTableData table = getParameter(ResultsTableData.class);
        TableEditor.openWindow(getWorkbench(), table, "Edit table");
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
