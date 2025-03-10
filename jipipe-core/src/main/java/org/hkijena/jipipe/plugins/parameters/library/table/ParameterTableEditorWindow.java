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

package org.hkijena.jipipe.plugins.parameters.library.table;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.*;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ParameterTableEditorWindow extends JFrame {
    private static final Map<ParameterTable, ParameterTableEditorWindow> OPEN_WINDOWS = new HashMap<>();
    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final JIPipeParameterAccess parameterAccess;
    private final ParameterTable parameterTable;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JLabel emptyColumnsLabel = new JLabel("<html><strong>This table has no columns</strong><br/>Import a parameter type from an existing node or define a new column.</html>",
            UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
    private final JLabel emptyRowsLabel = new JLabel("<html><strong>This table has no rows</strong><br/>Click the 'Add' button insert rows.</html>",
            UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
    private JXTable table;
    private JIPipeDesktopFormPanel palettePanel;

    private ParameterTableEditorWindow(JIPipeDesktopWorkbench desktopWorkbench, JIPipeParameterAccess parameterAccess, ParameterTable parameterTable, JIPipeDesktopGraphCanvasUI canvasUI) {
        this.desktopWorkbench = desktopWorkbench;
        this.parameterAccess = parameterAccess;
        this.parameterTable = parameterTable;
        this.canvasUI = canvasUI;
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                OPEN_WINDOWS.remove(parameterTable);
            }
        });
        initialize();
        reload();
    }

    public static ParameterTableEditorWindow getInstance(JIPipeDesktopWorkbench workbench, Component parent, JIPipeParameterAccess parameterAccess, ParameterTable parameterTable, JIPipeDesktopGraphCanvasUI canvasUI) {
        ParameterTableEditorWindow window = OPEN_WINDOWS.getOrDefault(parameterTable, null);
        if (window == null) {
            window = new ParameterTableEditorWindow(workbench, parameterAccess, parameterTable, canvasUI);
            window.pack();
            window.setSize(1024, 768);
            window.setLocationRelativeTo(parent);
            window.setTitle(parameterAccess.getName());
            window.setVisible(true);
            window.revalidate();
            window.repaint();
            OPEN_WINDOWS.put(parameterTable, window);
            return window;
        } else {
            window.toFront();
            window.revalidate();
            window.repaint();
        }
        return window;
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        JIPipeDesktopFlexContentPanel contentPanel = new JIPipeDesktopFlexContentPanel();
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        // Create palette panel and add to sidebar
        palettePanel = new JIPipeDesktopFormPanel(MarkdownText.fromPluginResource("documentation/documentation-parameter-table.md", new HashMap<>()),
                JIPipeDesktopFormPanel.WITH_DOCUMENTATION | JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW);
        contentPanel.getSideBar().addTab("Edit value", UIUtils.getIconFromResources("actions/edit.png"), palettePanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init info labels
        emptyRowsLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        emptyColumnsLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Create table panel and set as main content
        table = new JXTable();
        table.setRowHeight(32);
        table.setCellSelectionEnabled(true);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().addListSelectionListener(e -> updateParameters());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateParameters());
        contentPanel.getContentPanel().add(UIUtils.boxVertical(emptyColumnsLabel, emptyRowsLabel, table.getTableHeader()), BorderLayout.NORTH);
        contentPanel.getContentPanel().add(new JScrollPane(table), BorderLayout.CENTER);

        // Init the ribbon
        initializeRibbon(contentPanel.getRibbon());
    }

    private void initializeRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task columnTask = ribbon.addTask("Targeted parameters (columns)");
        JIPipeDesktopRibbon.Task rowTask = ribbon.addTask("Parameter sets (rows)");

        JIPipeDesktopRibbon.Band addColumnBand = columnTask.addBand("Add");
        JIPipeDesktopRibbon.Band editColumnBand = columnTask.addBand("Edit");
        JIPipeDesktopRibbon.Band addRowBand = rowTask.addBand("Add");
        JIPipeDesktopRibbon.Band editRowBand = rowTask.addBand("Edit");
        JIPipeDesktopRibbon.Band columnSelectBand = columnTask.addBand("Select");
        JIPipeDesktopRibbon.Band rowSelectBand = rowTask.addBand("Select");


        // Select
        rowSelectBand.add(new JIPipeDesktopLargeButtonRibbonAction("Whole column", "Selects the whole column", UIUtils.getIcon32FromResources("actions/stock_select-column.png"), this::selectWholeColumn));
        columnSelectBand.add(new JIPipeDesktopLargeButtonRibbonAction("Whole row", "Selects the whole row", UIUtils.getIcon32FromResources("actions/stock_select-row.png"), this::selectWholeRow));

        // Columns
        JIPipeDesktopLargeButtonRibbonAction addFromNodeAction = new JIPipeDesktopLargeButtonRibbonAction("Add from node",
                "Imports a parameter setting from an existing node",
                UIUtils.getIcon32FromResources("actions/edit-table-insert-column-right.png"),
                this::importColumnFromAlgorithm);
        UIUtils.makeButtonHighlightedSuccess(addFromNodeAction.getButton());
        addColumnBand.add(addFromNodeAction);
        addColumnBand.add(new JIPipeDesktopLargeButtonRibbonAction("Define custom", "Defines a parameter type from scratch", UIUtils.getIcon32FromResources("actions/node-add.png"), this::addCustomColumn));
        editColumnBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete selected column(s)", "Deletes the selected columns", UIUtils.getIcon32FromResources("actions/delete.png"), this::removeSelectedColumns));

        // Rows
        JIPipeDesktopLargeButtonRibbonAction addRowButton = new JIPipeDesktopLargeButtonRibbonAction("Add parameter set",
                "Adds a new parameter set/row into the table",
                UIUtils.getIcon32FromResources("actions/edit-table-insert-row-below.png"),
                this::addRow);
        UIUtils.makeButtonHighlightedSuccess(addRowButton.getButton());
        addRowBand.add(addRowButton);
        {
            JIPipeDesktopLargeButtonRibbonAction action = new JIPipeDesktopLargeButtonRibbonAction("Generate rows", "Generates new rows", UIUtils.getIcon32FromResources("actions/insert-math-expression.png"));
            JPopupMenu menu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(action.getButton(), menu, () -> {
                int[] selectedColumns = getSelectedColumns(true);
                createGenerateMenuFor(selectedColumns, menu);
            });
            addRowBand.add(action);
        }
        {
            JIPipeDesktopLargeButtonRibbonAction action = new JIPipeDesktopLargeButtonRibbonAction("Replace selected row(s)", "Replaces the selected values by generated values", UIUtils.getIcon32FromResources("actions/edit.png"));
            JPopupMenu menu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(action.getButton(), menu, () -> {
                menu.removeAll();
                int[] selectedColumns = getSelectedColumns(true);
                if (selectedColumns.length > 0) {
                    createReplaceMenuFor(selectedColumns[0], menu);
                }
            });
            editRowBand.add(action);
        }
        editRowBand.add(new JIPipeDesktopLargeButtonRibbonAction("Delete selected row(s)", "Deletes the selected rows", UIUtils.getIcon32FromResources("actions/delete.png"), this::removeSelectedRows));

        ribbon.rebuildRibbon();

        if (parameterTable.getColumnCount() > 0) {
            ribbon.getTabPane().getTabbedPane().setSelectedIndex(1);
        }
    }

    private void reload() {
        table.setModel(new DefaultTableModel());
        table.setModel(parameterTable);
        table.packAll();
        updateParameters();
    }

    private void updateParameters() {
        palettePanel.clear();

        int[] selectedColumns = getSelectedColumns(true);
        int[] selectedRows = getSelectedRows(true);

        // Selection control
        if (selectedRows.length > 0) {
            if (selectedColumns.length > 1) {
                palettePanel.addGroupHeader("Edit multiple parameters", UIUtils.getIconFromResources("actions/document-edit.png"));
                palettePanel.addWideToForm(new JLabel("<html><strong>Please select only one column</strong><br/>Currently, you can only edit one parameter type.</html>",
                        UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT));
            } else if (selectedColumns.length == 1) {
                if (selectedRows.length == 1) {
                    palettePanel.addGroupHeader("Edit parameter", UIUtils.getIconFromResources("actions/document-edit.png"));
                    ParameterTableCellAccess access = new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                            selectedRows[0], selectedColumns[0]);
                    JIPipeDesktopParameterEditorUI editor = JIPipe.getParameterTypes().createEditorInstance(access, getDesktopWorkbench(), new JIPipeParameterTree(access), null);
                    palettePanel.addWideToForm(editor, JIPipeDesktopParameterFormPanel.generateParameterDocumentation(access, null));
                } else {
                    palettePanel.addGroupHeader("Edit multiple parameters", UIUtils.getIconFromResources("actions/document-edit.png"));
                    List<JIPipeParameterAccess> accessList = new ArrayList<>();
                    for (int row : selectedRows) {
                        accessList.add(new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                                row, selectedColumns[0]));
                    }
                    JIPipeMultiParameterAccess multiParameterAccess = new JIPipeMultiParameterAccess(accessList);
                    JIPipeDesktopParameterEditorUI editor = JIPipe.getParameterTypes().createEditorInstance(multiParameterAccess, getDesktopWorkbench(), new JIPipeParameterTree(multiParameterAccess), null);
                    palettePanel.addWideToForm(editor, JIPipeDesktopParameterFormPanel.generateParameterDocumentation(multiParameterAccess, null));
                }

                JTextField keyInfo = UIUtils.createReadonlyBorderlessTextField(parameterTable.getColumnInfo(selectedColumns[0]).getKey());
                keyInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JLabel keyInfoLabel = new JLabel("Will be written into");
                keyInfoLabel.setIcon(UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
                palettePanel.addToForm(keyInfo, keyInfoLabel, new MarkdownText(String.format("This column will have the unique ID <code>%s</code> " +
                                "and will replace the parameter of given identifier if used in a parameter input slot.",
                        parameterTable.getColumnInfo(selectedColumns[0]).getKey())));
            }
        } else {
            palettePanel.addGroupHeader("Edit parameter", UIUtils.getIconFromResources("actions/document-edit.png"));
            palettePanel.addWideToForm(new JLabel("<html><strong>Nothing selected</strong><br/>To edit a parameter value, select it in the table.</html>",
                    UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT));
        }

        emptyColumnsLabel.setVisible(table.getModel().getColumnCount() <= 0);
        emptyRowsLabel.setVisible(table.getModel().getRowCount() <= 0);

        palettePanel.addVerticalGlue();
    }

    private void createGenerateMenuFor(int[] selectedColumns, JPopupMenu generateMenu) {
        generateMenu.removeAll();
        if (parameterTable.getColumnCount() == 0) {
            return;
        }
        if (selectedColumns == null || selectedColumns.length == 0) {
            selectedColumns = new int[parameterTable.getColumnCount()];
            for (int i = 0; i < parameterTable.getColumnCount(); i++) {
                selectedColumns[i] = i;
            }
        }
        for (int selectedColumn : selectedColumns) {
            JMenu columnMenu = new JMenu(getParameterTable().getColumnName(selectedColumn));
            for (JIPipeParameterGenerator generator : JIPipe.getParameterTypes()
                    .getGeneratorsFor(parameterTable.getColumn(selectedColumn).getFieldClass())) {
                JMenuItem generateRowItem = new JMenuItem(generator.getName());
                generateRowItem.setToolTipText(generator.getDescription());
                generateRowItem.setIcon(UIUtils.getIconFromResources("actions/list-add.png"));
                generateRowItem.addActionListener(e -> generateNewRows(selectedColumn, generator));
                columnMenu.add(generateRowItem);
            }
            generateMenu.add(columnMenu);
        }
    }

    private void createReplaceMenuFor(int selectedColumn, JPopupMenu generateMenu) {
        for (JIPipeParameterGenerator generator : JIPipe.getParameterTypes()
                .getGeneratorsFor(parameterTable.getColumn(selectedColumn).getFieldClass())) {
            JMenuItem generateRowItem = new JMenuItem(generator.getName());
            generateRowItem.setToolTipText(generator.getDescription());
            generateRowItem.setIcon(UIUtils.getIconFromResources("actions/list-add.png"));
            generateRowItem.addActionListener(e -> generateAndReplaceRows(selectedColumn, generator));
            generateMenu.add(generateRowItem);
        }
    }

    private void generateAndReplaceRows(int columnIndex, JIPipeParameterGenerator generator) {
        int[] selectedRows = getSelectedRows(false);
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "There are no rows selected!",
                    "Replace parameter values",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<?> generatedObjects = generator.generate(getDesktopWorkbench(), this, parameterTable.getColumnInfo(columnIndex).getFieldClass());
        if (generatedObjects != null) {
            if (generatedObjects.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "The list of generated parameters is empty!",
                        "Generate new rows",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String strategy = "Skip remaining rows";
            if (generatedObjects.size() < selectedRows.length) {
                Object result = JOptionPane.showInputDialog(this, String.format("You have selected %d rows, but only %d values were generated.\n" +
                                "What should be done with the remaining rows?", selectedRows.length, generatedObjects.size()),
                        "Replace parameter values", JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Skip remaining rows", "Repeat last generated value", "Cycle generated values"}, strategy);
                if (result == null || Objects.equals(result, "Cancel"))
                    return;
                strategy = result.toString();
            }
            if (strategy.equals("Skip remaining rows")) {
                for (int i = 0; i < Math.min(generatedObjects.size(), selectedRows.length); i++) {
                    parameterTable.setValueAt(generatedObjects.get(i), selectedRows[i], columnIndex);
                }
            } else if (strategy.equals("Repeat last generated value")) {
                for (int i = 0; i < selectedRows.length; i++) {
                    if (i <= generatedObjects.size() - 1) {
                        parameterTable.setValueAt(generatedObjects.get(i), selectedRows[i], columnIndex);
                    } else {
                        Object lastObject = generatedObjects.get(generatedObjects.size() - 1);
                        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(lastObject.getClass());
                        Object copy = info.duplicate(lastObject);
                        parameterTable.setValueAt(copy, selectedRows[i], columnIndex);
                    }
                }
            } else if (strategy.equals("Cycle generated values")) {
                for (int i = 0; i < selectedRows.length; i++) {
                    if (i <= generatedObjects.size() - 1) {
                        parameterTable.setValueAt(generatedObjects.get(i), selectedRows[i], columnIndex);
                    } else {
                        Object lastObject = generatedObjects.get(i % generatedObjects.size());
                        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(lastObject.getClass());
                        Object copy = info.duplicate(lastObject);
                        parameterTable.setValueAt(copy, selectedRows[i], columnIndex);
                    }
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
        reload();
    }

    private void generateNewRows(int columnIndex, JIPipeParameterGenerator generator) {
        List<?> generatedObjects = generator.generate(getDesktopWorkbench(), this, parameterTable.getColumnInfo(columnIndex).getFieldClass());
        if (generatedObjects != null) {
            if (generatedObjects.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "The list of generated parameters is empty!",
                        "Generate new rows",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (Object generatedObject : generatedObjects) {
                parameterTable.addRow();
                parameterTable.setValueAt(generatedObject, parameterTable.getRowCount() - 1, columnIndex);
            }
        }
        reload();
    }

    private void importColumnFromAlgorithm() {

        Set<JIPipeGraphNode> existingNodes;

        if (canvasUI != null) {
            existingNodes = canvasUI.getVisibleNodes();
        } else {
            existingNodes = Collections.emptySet();
        }

        List<JIPipeDesktopParameterKeyPickerUI.ParameterEntry> result = JIPipeDesktopParameterKeyPickerUI.showPickerDialog(this,
                "Add columns",
                existingNodes,
                null);

        if (!result.isEmpty()) {
            for (JIPipeDesktopParameterKeyPickerUI.ParameterEntry parameterEntry : result) {
                importParameterColumn(parameterEntry.getName(),
                        parameterEntry.getDescription(),
                        parameterEntry.getKey(),
                        parameterEntry.getFieldClass(),
                        parameterEntry.getValue());
            }

            // Ensure that there is at least 1 row
            if (parameterTable.getRowCount() == 0) {
                addRow();
            }
        }
    }

    private void importParameterColumn(String name, String description, String key, Class<?> fieldClass, Object defaultValue) {
        if (parameterTable.containsColumn(key))
            return;
        ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn();
        column.setFieldClass(fieldClass);
        column.setKey(key);
        column.setName(name);
        column.setDescription(description);
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.getFieldClass());
        parameterTable.addColumn(column, defaultValue != null ? info.duplicate(defaultValue) : info.newInstance());

        reload();
    }

    private void importParameterColumn(JIPipeParameterTree.Node node, JIPipeParameterAccess importedParameter) {
        List<String> path = node.getPath();
        path.add(importedParameter.getKey());
        path.remove(0);

        String uniqueKey = String.join("/", path);
        if (parameterTable.containsColumn(uniqueKey))
            return;
        ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn();
        column.setFieldClass(importedParameter.getFieldClass());
        column.setKey(uniqueKey);
        column.setName(importedParameter.getName());
        column.setDescription(importedParameter.getDescription());
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.getFieldClass());
        parameterTable.addColumn(column, info.duplicate(importedParameter.get(Object.class)));

        reload();
    }

    private void addCustomColumn() {
        JIPipeDynamicParameterCollection collection = new JIPipeDynamicParameterCollection();
        collection.setAllowedTypes(JIPipe.getParameterTypes()
                .getRegisteredParameters().values().stream().map(JIPipeParameterTypeInfo::getFieldClass).collect(Collectors.toSet()));
        JIPipeDesktopAddParameterDialog.showDialog(desktopWorkbench, this, collection);
        for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
            if (parameterTable.containsColumn(entry.getKey()))
                continue;
            ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn();
            column.setFieldClass(entry.getValue().getFieldClass());
            column.setKey(entry.getKey());
            column.setName(entry.getValue().getName());
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.getFieldClass());
            parameterTable.addColumn(column, info.newInstance());
        }
        reload();
    }

    private void removeSelectedRows() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the selected ROW(S)?", "Delete row(s)", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            int[] selectedRows = getSelectedRows(true);
            table.setModel(new DefaultTableModel());
            for (int i = selectedRows.length - 1; i >= 0; --i) {
                parameterTable.removeRow(selectedRows[i]);
            }
            reload();
        }
    }

    private void removeSelectedColumns() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the selected COLUMN(S)?\n" +
                "All settings for the parameter will be gone!", "Delete column(s)", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            int[] selectedColumns = getSelectedColumns(true);
            table.setModel(new DefaultTableModel());
            for (int i = selectedColumns.length - 1; i >= 0; --i) {
                parameterTable.removeColumn(selectedColumns[i]);
            }
            reload();
        }
    }

    /**
     * Returns selected rows as sorted array of model indices
     *
     * @return selected rows in model indices
     */
    private int[] getSelectedRows(boolean sort) {
        int[] displayRows = table.getSelectedRows();
        for (int i = 0; i < displayRows.length; ++i) {
            displayRows[i] = table.getRowSorter().convertRowIndexToModel(displayRows[i]);
        }
        if (sort)
            Arrays.sort(displayRows);
        return displayRows;
    }

    private void addRow() {
        parameterTable.addRow();
        reload();
    }

    private void selectWholeColumn() {
        table.addRowSelectionInterval(0, parameterTable.getRowCount() - 1);
    }

    private void selectWholeRow() {
        table.addColumnSelectionInterval(0, parameterTable.getColumnCount() - 1);
    }

    /**
     * Returns selected rows as sorted array of model indices
     *
     * @return selected rows in model indices
     */
    private int[] getSelectedColumns(boolean sort) {
        int[] displayColumns = table.getSelectedColumns();
        for (int i = 0; i < displayColumns.length; ++i) {
            displayColumns[i] = table.convertColumnIndexToModel(displayColumns[i]);
        }
        if (sort)
            Arrays.sort(displayColumns);
        return displayColumns;
    }

    private JButton addActionToPalette(String name, String description, Icon icon, Runnable action) {
        JButton button = new JButton(name, icon);
        button.setToolTipText(description);
        UIUtils.setStandardButtonBorder(button);
        button.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 2));
//        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalAlignment(SwingConstants.LEFT);
//        button.setHorizontalTextPosition(SwingConstants.CENTER);
//        button.setMinimumSize(new Dimension(50, 50));
//        button.setPreferredSize(new Dimension(50,50));
        button.addActionListener(e -> action.run());
        palettePanel.addWideToForm(button, null);
//        currentPaletteGroup.add(button);
        return button;
    }

    private void addSeparatorToPalette() {
        palettePanel.addWideToForm(Box.createVerticalStrut(4), null);
//        currentPaletteGroup.add(Box.createVerticalStrut(8));
    }

    private JIPipeDesktopFormPanel.GroupHeaderPanel addPaletteGroup(String name, Icon icon) {
        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeaderPanel = palettePanel.addGroupHeader(name, icon);
//        currentPaletteGroup = new JPanel(new GridLayout(5,2));
//        currentPaletteGroup.setLayout(new ModifiedFlowLayout(FlowLayout.LEFT));
//        currentPaletteGroup.setMinimumSize(new Dimension(50,50));
//        palettePanel.addWideToForm(currentPaletteGroup, null);
        return groupHeaderPanel;
    }

    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }
}
