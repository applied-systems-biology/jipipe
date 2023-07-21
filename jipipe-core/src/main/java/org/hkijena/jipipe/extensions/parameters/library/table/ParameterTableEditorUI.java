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

package org.hkijena.jipipe.extensions.parameters.library.table;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * UI for {@link ParameterTable}
 */
public class ParameterTableEditorUI extends JIPipeParameterEditorUI implements ParameterTable.ModelChangedEventListener {

    private JXTable table;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public ParameterTableEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        // Create toolbar for adding/removing rows
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(Box.createHorizontalGlue());

        JButton editButton = new JButton("Edit parameters", UIUtils.getIconFromResources("actions/document-edit.png"));
        editButton.setToolTipText("Opens a new window that allows to edit the parameter table");
        editButton.addActionListener(e -> openEditor());

        toolBar.add(editButton);

        // Create table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        table = new JXTable();
        table.setRowHeight(25);
        table.setCellSelectionEnabled(true);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(table, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void openEditor() {
        ParameterTableEditorWindow window = ParameterTableEditorWindow.getInstance(getWorkbench(),
                getWorkbench().getWindow(),
                getParameterAccess(),
                getParameter(ParameterTable.class));
        if (getParameterAccess().getSource() instanceof JIPipeGraphNode) {
            window.setTitle(((JIPipeGraphNode) getParameterAccess().getSource()).getName() + ": " + getParameterAccess().getName());
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (table.getModel() instanceof ParameterTable) {
            ((ParameterTable) table.getModel()).getModelChangedEventEmitter().unsubscribe(this);
        }

        ParameterTable parameterTable = getParameter(ParameterTable.class);
        if (parameterTable == null) {
            table.setModel(new DefaultTableModel());
        } else {
            table.setModel(parameterTable);
            parameterTable.getModelChangedEventEmitter().subscribeWeak(this);
        }
    }

    @Override
    public void onParameterTableModelChangedEvent(ParameterTable.ModelChangedEvent event) {
        TableModel model = table.getModel();
        table.setModel(new DefaultTableModel());
        table.setModel(model);
    }
}
