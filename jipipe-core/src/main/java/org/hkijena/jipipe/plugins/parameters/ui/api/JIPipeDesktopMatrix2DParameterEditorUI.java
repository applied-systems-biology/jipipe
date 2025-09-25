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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.matrix.JIPipeMatrix2DParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

/**
 * Editor for {@link JIPipeMatrix2DParameter}
 */
public class JIPipeDesktopMatrix2DParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JTable table;

    public JIPipeDesktopMatrix2DParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
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

        JButton addRowButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-table-insert-row-below.png"));
        addRowButton.setToolTipText("Add row");
        addRowButton.addActionListener(e -> {
            getParameter(JIPipeMatrix2DParameter.class).addRow();
            reload();
        });
        toolBar.add(addRowButton);

        JButton addColumnButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-table-insert-column-right.png"));
        addColumnButton.setToolTipText("Add column");
        addColumnButton.addActionListener(e -> {
            getParameter(JIPipeMatrix2DParameter.class).addColumn();
            reload();
        });
        toolBar.add(addColumnButton);

        toolBar.addSeparator();

        JButton removeRowButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-table-delete-row.png"));
        removeRowButton.setToolTipText("Remove selected row");
        removeRowButton.addActionListener(e -> removeRow());
        toolBar.add(removeRowButton);

        JButton removeColumnButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-table-delete-column.png"));
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
        JIPipeMatrix2DParameter<?> parameter = getParameter(JIPipeMatrix2DParameter.class);
        for (int i = selectedColumns.length - 1; i >= 0; --i) {
            parameter.removeColumn(i);
        }
        reload();
    }

    private void removeRow() {
        int[] selectedRows = table.getSelectedRows();
        Arrays.sort(selectedRows);
        JIPipeMatrix2DParameter<?> parameter = getParameter(JIPipeMatrix2DParameter.class);
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
        JIPipeMatrix2DParameter<?> parameter = getParameter(JIPipeMatrix2DParameter.class);
        table.setModel(new DefaultTableModel());
        table.setModel(parameter);
        table.revalidate();
        table.repaint();
    }
}
