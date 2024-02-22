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

package org.hkijena.jipipe.extensions.parameters.api.matrix;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

/**
 * Editor for {@link Matrix2D}
 */
public class Matrix2DParameterEditorUI extends JIPipeParameterEditorUI {

    private JTable table;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public Matrix2DParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel(getParameterAccess().getName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton addRowButton = new JButton(UIUtils.getIconFromResources("actions/edit-table-insert-row-below.png"));
        addRowButton.setToolTipText("Add row");
        addRowButton.addActionListener(e -> {
            getParameter(Matrix2D.class).addRow();
            reload();
        });
        toolBar.add(addRowButton);

        JButton addColumnButton = new JButton(UIUtils.getIconFromResources("actions/edit-table-insert-column-right.png"));
        addColumnButton.setToolTipText("Add column");
        addColumnButton.addActionListener(e -> {
            getParameter(Matrix2D.class).addColumn();
            reload();
        });
        toolBar.add(addColumnButton);

        toolBar.addSeparator();

        JButton removeRowButton = new JButton(UIUtils.getIconFromResources("actions/edit-table-delete-row.png"));
        removeRowButton.setToolTipText("Remove selected row");
        removeRowButton.addActionListener(e -> removeRow());
        toolBar.add(removeRowButton);

        JButton removeColumnButton = new JButton(UIUtils.getIconFromResources("actions/edit-table-delete-column.png"));
        removeColumnButton.setToolTipText("Remove selected column");
        removeColumnButton.addActionListener(e -> removeColumn());
        toolBar.add(removeColumnButton);

        add(toolBar, BorderLayout.NORTH);

        table = new JTable();
        table.setCellSelectionEnabled(true);

        add(table, BorderLayout.CENTER);
    }

    private void removeColumn() {
        int[] selectedColumns = table.getSelectedColumns();
        Arrays.sort(selectedColumns);
        Matrix2D<?> parameter = getParameter(Matrix2D.class);
        for (int i = selectedColumns.length - 1; i >= 0; --i) {
            parameter.removeColumn(i);
        }
        reload();
    }

    private void removeRow() {
        int[] selectedRows = table.getSelectedRows();
        Arrays.sort(selectedRows);
        Matrix2D<?> parameter = getParameter(Matrix2D.class);
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameter.removeRow(i);
        }
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        Matrix2D<?> parameter = getParameter(Matrix2D.class);
        table.setModel(new DefaultTableModel());
        table.setModel(parameter);
        table.revalidate();
        table.repaint();
    }
}
