package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.collections.Matrix2DParameter;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Editor for {@link Matrix2DParameter}
 */
public class Matrix2DParameterEditorUI extends ACAQParameterEditorUI {

    private JXTable table;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public Matrix2DParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
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
        addRowButton.addActionListener(e -> getParameterAccess().get(Matrix2DParameter.class).addRow());
        toolBar.add(addRowButton);

        JButton addColumnButton = new JButton(UIUtils.getIconFromResources("add-column.png"));
        addColumnButton.setToolTipText("Add column");
        addColumnButton.addActionListener(e -> getParameterAccess().get(Matrix2DParameter.class).addColumn());
        toolBar.add(addColumnButton);

        toolBar.addSeparator();

        JButton removeRowButton = new JButton(UIUtils.getIconFromResources("remove-row.png"));
        removeRowButton.setToolTipText("Remove selected row");
        removeRowButton.removeActionListener(e -> removeRow());
        toolBar.remove(removeRowButton);

        JButton removeColumnButton = new JButton(UIUtils.getIconFromResources("remove-column.png"));
        removeColumnButton.setToolTipText("Remove selected column");
        removeColumnButton.removeActionListener(e -> removeColumn());
        toolBar.remove(removeColumnButton);

        table = new JXTable();
        table.setCellSelectionEnabled(true);
    }

    private void removeColumn() {
        int[] selectedColumns = table.getSelectedColumns();
        Arrays.sort(selectedColumns);
        Matrix2DParameter<?> parameter = getParameterAccess().get(Matrix2DParameter.class);
        for (int i = selectedColumns.length - 1; i >= 0; --i) {
            parameter.removeColumn(i);
        }
    }

    private void removeRow() {
        int[] selectedRows = table.getSelectedRows();
        Arrays.sort(selectedRows);
        Matrix2DParameter<?> parameter = getParameterAccess().get(Matrix2DParameter.class);
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameter.removeRow(i);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        Matrix2DParameter<?> parameter = getParameterAccess().get(Matrix2DParameter.class);
        table.setModel(parameter);
    }
}
