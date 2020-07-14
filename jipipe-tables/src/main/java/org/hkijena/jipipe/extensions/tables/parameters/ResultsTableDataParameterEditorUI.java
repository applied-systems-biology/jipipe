package org.hkijena.jipipe.extensions.tables.parameters;

import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParametersUI;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.tableanalyzer.JIPipeTableEditor;
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
    public ResultsTableDataParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editButton = new JButton("Edit table", UIUtils.getIconFromResources("edit.png"));
        editButton.addActionListener(e -> editParameters());
        add(editButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        ResultsTableData parameters = getParameter(ResultsTableData.class);
        for (DocumentTabPane.DocumentTab tab : getWorkbench().getDocumentTabPane().getTabs()) {
            if(tab.getContent() instanceof JIPipeTableEditor) {
                if(((JIPipeTableEditor) tab.getContent()).getTableModel() == parameters) {
                    getWorkbench().getDocumentTabPane().switchToContent(tab.getContent());
                    return;
                }
            }
        }
        getWorkbench().getDocumentTabPane().addTab(getParameterAccess().getName(),
                UIUtils.getIconFromResources("table.png"),
                new JIPipeTableEditor(getWorkbench(), parameters),
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
