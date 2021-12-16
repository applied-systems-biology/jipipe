package org.hkijena.jipipe.extensions.parameters.table;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMultiParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AddDynamicParameterPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ParameterTableEditorWindow extends JFrame {
    private static final Map<ParameterTable, ParameterTableEditorWindow> OPEN_WINDOWS = new HashMap<>();
    private final JIPipeWorkbench workbench;
    private final JIPipeParameterAccess parameterAccess;
    private final ParameterTable parameterTable;
    private JXTable table;
    private FormPanel palettePanel;
    private JPanel currentPaletteGroup;

    private ParameterTableEditorWindow(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess, ParameterTable parameterTable) {
        this.workbench = workbench;
        this.parameterAccess = parameterAccess;
        this.parameterTable = parameterTable;
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
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

    private void initialize() {
        setLayout(new BorderLayout());

        // Create form panel
        palettePanel = new FormPanel(MarkdownDocument.fromPluginResource("documentation/documentation-parameter-table.md", new HashMap<>()),
                FormPanel.WITH_DOCUMENTATION | FormPanel.WITH_SCROLLING | FormPanel.DOCUMENTATION_BELOW);

        // Create table panel
        table = new JXTable();
        table.setRowHeight(32);
        table.setCellSelectionEnabled(true);
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().addListSelectionListener(e -> updateParameters());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateParameters());

        // Create split pane
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), palettePanel, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
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
                palettePanel.addGroupHeader("Please select only one column", UIUtils.getIconFromResources("emblems/warning.png"));
            } else if (selectedColumns.length == 1) {
                if (selectedRows.length == 1) {
                    palettePanel.addGroupHeader("Edit parameter", UIUtils.getIconFromResources("actions/document-edit.png"));
                    ParameterTableCellAccess access = new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                            selectedRows[0], selectedColumns[0]);
                    JIPipeParameterEditorUI editor = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access);
                    palettePanel.addWideToForm(editor, ParameterPanel.generateParameterDocumentation(access, null));
                } else {
                    palettePanel.addGroupHeader("Edit multiple parameters", UIUtils.getIconFromResources("actions/document-edit.png"));
                    List<JIPipeParameterAccess> accessList = new ArrayList<>();
                    for (int row : selectedRows) {
                        accessList.add(new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                                row, selectedColumns[0]));
                    }
                    JIPipeMultiParameterAccess multiParameterAccess = new JIPipeMultiParameterAccess(accessList);
                    JIPipeParameterEditorUI editor = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), multiParameterAccess);
                    palettePanel.addWideToForm(editor, ParameterPanel.generateParameterDocumentation(multiParameterAccess, null));
                }

                JTextField keyInfo = UIUtils.makeReadonlyBorderlessTextField(parameterTable.getColumnInfo(selectedColumns[0]).getKey());
                keyInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JLabel keyInfoLabel = new JLabel("Will be written into");
                keyInfoLabel.setIcon(UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
                palettePanel.addToForm(keyInfo, keyInfoLabel, new MarkdownDocument(String.format("This column will have the unique ID <code>%s</code> " +
                                "and will replace the parameter of given identifier if used in a parameter input slot.",
                        parameterTable.getColumnInfo(selectedColumns[0]).getKey())));
            }
        }

        // Column controls
        addPaletteGroup("Columns (" + parameterTable.getColumnCount() + ")", parameterTable.getColumnCount() == 0 ?
                UIUtils.getIconFromResources("emblems/warning.png") : UIUtils.getIconFromResources("actions/view-column.png"));
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            addActionToPalette("Import",
                    "Adds a column from an existing node.",
                    UIUtils.getIconFromResources("actions/add.png"),
                    this::importColumnFromAlgorithm);
        }
        addActionToPalette("Define",
                "Defines a custom column (advanced users).",
                UIUtils.getIconFromResources("actions/code-context.png"),
                this::addCustomColumn);
        if (selectedColumns.length > 0) {
            addSeparatorToPalette();
            addActionToPalette("Remove",
                    "Remove selected columns",
                    UIUtils.getIconFromResources("actions/edit-table-delete-column.png"),
                    this::removeSelectedColumns);
        }

        //Row controls
        addPaletteGroup("Rows (" + parameterTable.getRowCount() + ")", parameterTable.getRowCount() == 0 ?
                UIUtils.getIconFromResources("emblems/warning.png") : UIUtils.getIconFromResources("actions/object-rows.png"));

        addActionToPalette("Add",
                "Adds an empty row at the end of the table.",
                UIUtils.getIconFromResources("actions/add.png"),
                this::addRow);
        if (selectedColumns.length == 1) {
            JPopupMenu generateMenu = new JPopupMenu();
            createGenerateMenuFor(selectedColumns[0], generateMenu);
            if (generateMenu.getComponentCount() > 0) {
                JButton generateButton = addActionToPalette("Generate new",
                        "Generates new rows",
                        UIUtils.getIconFromResources("actions/tools-wizard.png"),
                        () -> {
                        });
                UIUtils.addPopupMenuToComponent(generateButton, generateMenu);
            }
        }
        if (selectedRows.length > 0 && selectedColumns.length == 1) {
            JPopupMenu generateMenu = new JPopupMenu();
            createReplaceMenuFor(selectedColumns[0], generateMenu);
            if (generateMenu.getComponentCount() > 0) {
                JButton generateButton = addActionToPalette("Replace selection by generated",
                        "Generates parameter values and replaces the selected items by these values",
                        UIUtils.getIconFromResources("actions/tools-wizard.png"),
                        () -> {
                        });
                UIUtils.addPopupMenuToComponent(generateButton, generateMenu);
            }
        }
        if (selectedRows.length > 0) {
            addSeparatorToPalette();
            addActionToPalette("Remove",
                    "Remove selected rows",
                    UIUtils.getIconFromResources("actions/edit-table-delete-row.png"),
                    this::removeSelectedRows);
        }


        // Selection controls
        addPaletteGroup("Selection", UIUtils.getIconFromResources("actions/edit-select-all.png"));
        addActionToPalette("Whole row",
                "Expands the selection to the whole row",
                UIUtils.getIconFromResources("actions/stock_select-row.png"),
                this::selectWholeRow);
        addActionToPalette("Whole column",
                "Expands the selection to the whole column",
                UIUtils.getIconFromResources("actions/stock_select-column.png"),
                this::selectWholeColumn);

        palettePanel.addVerticalGlue();
    }

    private void createGenerateMenuFor(int selectedColumn, JPopupMenu generateMenu) {
        for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipe.getParameterTypes()
                .getGeneratorsFor(parameterTable.getColumn(selectedColumn).getFieldClass())) {
            JIPipeDocumentation documentation = JIPipe.getParameterTypes().getGeneratorDocumentationFor(generator);
            JMenuItem generateRowItem = new JMenuItem(documentation.name());
            generateRowItem.setToolTipText(DocumentationUtils.getDocumentationDescription(documentation));
            generateRowItem.setIcon(UIUtils.getIconFromResources("actions/list-add.png"));
            generateRowItem.addActionListener(e -> generateNewRows(selectedColumn, generator));
            generateMenu.add(generateRowItem);
        }
    }

    private void createReplaceMenuFor(int selectedColumn, JPopupMenu generateMenu) {
        for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipe.getParameterTypes()
                .getGeneratorsFor(parameterTable.getColumn(selectedColumn).getFieldClass())) {
            JIPipeDocumentation documentation = JIPipe.getParameterTypes().getGeneratorDocumentationFor(generator);
            JMenuItem generateRowItem = new JMenuItem(documentation.name());
            generateRowItem.setToolTipText(DocumentationUtils.getDocumentationDescription(documentation));
            generateRowItem.setIcon(UIUtils.getIconFromResources("actions/list-add.png"));
            generateRowItem.addActionListener(e -> generateAndReplaceRows(selectedColumn, generator));
            generateMenu.add(generateRowItem);
        }
    }

    private void generateAndReplaceRows(int columnIndex, Class<? extends JIPipeParameterGeneratorUI> generator) {
        int[] selectedRows = getSelectedRows(false);
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "There are no rows selected!",
                    "Replace parameter values",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Object> generatedObjects = JIPipeParameterGeneratorUI.showDialog(this, getWorkbench(), generator);
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

    private void generateNewRows(int columnIndex, Class<? extends JIPipeParameterGeneratorUI> generator) {
        List<Object> generatedObjects = JIPipeParameterGeneratorUI.showDialog(this, getWorkbench(), generator);
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
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeGraph graph = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getGraph();
            JIPipeParameterTree globalTree = graph.getParameterTree(false);

            List<Object> importedParameters = ParameterTreeUI.showPickerDialog(getWorkbench().getWindow(), globalTree, "Import parameter");
            for (Object importedParameter : importedParameters) {
                if (importedParameter instanceof JIPipeParameterAccess) {
                    JIPipeParameterTree.Node node = globalTree.getSourceNode(((JIPipeParameterAccess) importedParameter).getSource());
                    importParameterColumn(node, (JIPipeParameterAccess) importedParameter);
                } else if (importedParameter instanceof JIPipeParameterTree.Node) {
                    JIPipeParameterTree.Node node = (JIPipeParameterTree.Node) importedParameter;
                    for (JIPipeParameterAccess access : node.getParameters().values()) {
                        if (!access.isHidden()) {
                            JIPipeParameterTree.Node sourceNode = globalTree.getSourceNode(access.getSource());
                            importParameterColumn(sourceNode, access);
                        }
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "There is no graph editor open.", "Error", JOptionPane.ERROR_MESSAGE);
        }
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
        AddDynamicParameterPanel.showDialog(this, collection);
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
        int[] selectedRows = getSelectedRows(true);
        table.setModel(new DefaultTableModel());
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameterTable.removeRow(selectedRows[i]);
        }
        reload();
    }

    private void removeSelectedColumns() {
        int[] selectedColumns = getSelectedColumns(true);
        table.setModel(new DefaultTableModel());
        for (int i = selectedColumns.length - 1; i >= 0; --i) {
            parameterTable.removeColumn(selectedColumns[i]);
        }
        reload();
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
        UIUtils.makeFlat(button);
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

    private FormPanel.GroupHeaderPanel addPaletteGroup(String name, Icon icon) {
        FormPanel.GroupHeaderPanel groupHeaderPanel = palettePanel.addGroupHeader(name, icon);
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

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public static ParameterTableEditorWindow getInstance(JIPipeWorkbench workbench, Component parent, JIPipeParameterAccess parameterAccess, ParameterTable parameterTable) {
        ParameterTableEditorWindow window = OPEN_WINDOWS.getOrDefault(parameterTable, null);
        if (window == null) {
            window = new ParameterTableEditorWindow(workbench, parameterAccess, parameterTable);
            window.setSize(1024, 768);
            window.setLocationRelativeTo(parent);
            window.setTitle(parameterAccess.getName());
            window.setVisible(true);
            OPEN_WINDOWS.put(parameterTable, window);
            return window;
        } else {
            window.toFront();
            window.repaint();
        }
        return window;
    }
}
