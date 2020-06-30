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

package org.hkijena.acaq5.extensions.parameters.matrix;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Arrays;

/**
 * Editor for {@link Matrix2D}
 */
public class Matrix2DParameterEditorUI extends ACAQParameterEditorUI {

    private JXTable table;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public Matrix2DParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel(getParameterAccess().getName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton addRowButton = new JButton(UIUtils.getIconFromResources("add-row.png"));
        addRowButton.setToolTipText("Add row");
        addRowButton.addActionListener(e -> getParameter(Matrix2D.class).addRow());
        toolBar.add(addRowButton);

        JButton addColumnButton = new JButton(UIUtils.getIconFromResources("add-column.png"));
        addColumnButton.setToolTipText("Add column");
        addColumnButton.addActionListener(e -> getParameter(Matrix2D.class).addColumn());
        toolBar.add(addColumnButton);

        toolBar.addSeparator();

        JButton removeRowButton = new JButton(UIUtils.getIconFromResources("remove-row.png"));
        removeRowButton.setToolTipText("Remove selected row");
        removeRowButton.addActionListener(e -> removeRow());
        toolBar.add(removeRowButton);

        JButton removeColumnButton = new JButton(UIUtils.getIconFromResources("remove-column.png"));
        removeColumnButton.setToolTipText("Remove selected column");
        removeColumnButton.addActionListener(e -> removeColumn());
        toolBar.add(removeColumnButton);

        add(toolBar, BorderLayout.NORTH);

        table = new JXTable();
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
    }
}
