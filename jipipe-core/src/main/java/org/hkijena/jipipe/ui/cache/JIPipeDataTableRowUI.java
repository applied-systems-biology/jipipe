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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultCacheDisplaySettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * UI for a row
 */
public class JIPipeDataTableRowUI extends JIPipeWorkbenchPanel {
    private final Store<JIPipeDataTable> dataTableStore;
    private final int row;
    private final List<Store<JIPipeDataAnnotation>> dataAnnotationStores;
    private final List<JIPipeDataDisplayOperation> displayOperations;
    private JButton dataAnnotationsButton;
    private JButton textAnnotationsButton;

    /**
     * Creates a new instance
     *
     * @param workbench      the workbench
     * @param dataTableStore the data table store
     * @param row            the row
     */
    public JIPipeDataTableRowUI(JIPipeWorkbench workbench, Store<JIPipeDataTable> dataTableStore, int row) {
        super(workbench);
        this.dataTableStore = dataTableStore;
        this.row = row;
        this.dataAnnotationStores = new ArrayList<>();
        for (JIPipeDataAnnotation dataAnnotation : dataTableStore.get().getDataAnnotations(row)) {
            dataAnnotationStores.add(new WeakStore<>(dataAnnotation));
        }
        Class<? extends JIPipeData> dataClass = dataTableStore.get().getDataClass(row);
        String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
        this.initialize(dataTableStore.get());
    }

    public static JIPipeDataDisplayOperation getMainOperation(Class<? extends JIPipeData> dataClass) {
        String dataTypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        List<JIPipeDataDisplayOperation> displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(dataTypeId);
        if (!displayOperations.isEmpty()) {
            JIPipeDataDisplayOperation result = displayOperations.get(0);
            DynamicDataDisplayOperationIdEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDataDisplayOperation operation : displayOperations) {
                    if (Objects.equals(operation.getId(), defaultName)) {
                        result = operation;
                        break;
                    }
                }
            }
            if (result == null) {
                result = JIPipe.getDataTypes().getAllRegisteredDisplayOperations(dataTypeId).get("jipipe:show");
            }
            return result;
        }
        return null;
    }

    private void initialize(JIPipeDataTable dataTable) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (dataAnnotationStores.size() > 0) {
            dataAnnotationsButton = new JButton("Data annotations ...", UIUtils.getIconFromResources("data-types/data-annotation.png"));
            JPopupMenu menu = UIUtils.addPopupMenuToButton(dataAnnotationsButton);

            for (Store<JIPipeDataAnnotation> store : dataAnnotationStores) {
                JMenu subMenu = new JMenu(store.get().getName());
                subMenu.setIcon(JIPipe.getDataTypes().getIconFor(store.get().getDataClass()));
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(store.get().getDataClass());
                List<JIPipeDataDisplayOperation> displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
                for (JIPipeDataDisplayOperation displayOperation : displayOperations) {
                    JMenuItem item = new JMenuItem(displayOperation.getName(), displayOperation.getIcon());
                    item.setToolTipText(displayOperation.getDescription());
                    item.addActionListener(e -> {
                        runDisplayOperation(displayOperation, store.get());
                    });
                    subMenu.add(item);
                }
                menu.add(subMenu);
            }

            add(dataAnnotationsButton);
        }

        List<JIPipeTextAnnotation> textAnnotations = dataTable.getTextAnnotations(row);
        if (!textAnnotations.isEmpty()) {
            textAnnotationsButton = new JButton("Annotations ...", UIUtils.getIconFromResources("data-types/annotation.png"));
            JPopupMenu annotationMenu = UIUtils.addPopupMenuToButton(textAnnotationsButton);
            {
                JMenuItem toTableItem = new JMenuItem("Display as table", UIUtils.getIconFromResources("data-types/results-table.png"));
                toTableItem.addActionListener(e -> displayAnnotationsAsTable(textAnnotations));
                annotationMenu.add(toTableItem);
            }
            for (JIPipeTextAnnotation annotation : textAnnotations) {
                JMenu entryMenu = new JMenu(annotation.getName());
                entryMenu.setIcon(UIUtils.getIconFromResources("data-types/annotation.png"));

                JMenuItem valueItem = new JMenuItem(StringUtils.nullToEmpty(annotation.getValue()), UIUtils.getIconFromResources("actions/equals.png"));
                valueItem.setEnabled(false);
                entryMenu.add(valueItem);

                entryMenu.addSeparator();

                JMenuItem copyAnnotationNameItem = new JMenuItem("Copy name", UIUtils.getIconFromResources("actions/edit-copy.png"));
                copyAnnotationNameItem.addActionListener(e -> UIUtils.copyToClipboard(annotation.getName()));
                entryMenu.add(copyAnnotationNameItem);

                JMenuItem copyAnnotationNameAsVariableItem = new JMenuItem("Copy name as expression variable", UIUtils.getIconFromResources("actions/edit-copy.png"));
                copyAnnotationNameAsVariableItem.addActionListener(e -> UIUtils.copyToClipboard(DefaultExpressionEvaluator.escapeVariable(annotation.getName())));
                entryMenu.add(copyAnnotationNameAsVariableItem);

                JMenuItem copyAnnotationValueItem = new JMenuItem("Copy value", UIUtils.getIconFromResources("actions/edit-copy.png"));
                copyAnnotationValueItem.addActionListener(e -> UIUtils.copyToClipboard(annotation.getValue()));
                entryMenu.add(copyAnnotationValueItem);

                annotationMenu.add(entryMenu);
            }
            add(textAnnotationsButton);
        }

        JButton copyButton = new JButton("Copy string", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyButton.setToolTipText("Copies the string representation");
        copyButton.addActionListener(e -> copyString());
        add(copyButton);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        JPopupMenu exportMenu = UIUtils.addPopupMenuToButton(exportButton);

        JMenuItem exportToFolderItem = new JMenuItem("Export to folder", UIUtils.getIconFromResources("actions/browser-download.png"));
        exportToFolderItem.setToolTipText("Saves the data to a folder. If multiple files are present, the names will be generated according to the selected name.");
        exportToFolderItem.addActionListener(e -> exportToFolder());
        exportMenu.add(exportToFolderItem);

        JMenuItem exportAsFolderItem = new JMenuItem("Export as folder", UIUtils.getIconFromResources("actions/folder-new.png"));
        exportAsFolderItem.setToolTipText("Saves the data into a new folder. Files will be named according to the data type standard.");
        exportAsFolderItem.addActionListener(e -> exportAsFolder());
        exportMenu.add(exportAsFolderItem);

        add(exportButton);

        if (!displayOperations.isEmpty()) {
            JIPipeDataDisplayOperation mainOperation = getMainOperation();
            if (mainOperation == null)
                return;
            JButton mainActionButton = new JButton(mainOperation.getName(), mainOperation.getIcon());
            mainActionButton.setToolTipText(mainOperation.getDescription());
            mainActionButton.addActionListener(e -> runDisplayOperation(mainOperation));
            add(mainActionButton);

            if (displayOperations.size() > 1) {
                JButton menuButton = new JButton("Open with ...");
                menuButton.setMaximumSize(new Dimension(1, (int) mainActionButton.getPreferredSize().getHeight()));
                menuButton.setToolTipText("Shows more actions to display the data. On selecting an entry, " +
                        "it becomes the default action.");
                JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
                for (JIPipeDataDisplayOperation otherSlotAction : displayOperations) {
                    if (otherSlotAction == mainOperation)
                        continue;
                    JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                    item.setToolTipText(otherSlotAction.getDescription());
                    item.addActionListener(e -> runDisplayOperation(otherSlotAction));
                    menu.add(item);
                }
                add(menuButton);
            }
        }
    }

    private void displayAnnotationsAsTable(List<JIPipeTextAnnotation> textAnnotations) {
        if (dataTableStore.isPresent()) {
            JIPipeDataTable dataTable = dataTableStore.get();
            ResultsTableData data = new ResultsTableData();
            data.addStringColumn("Name");
            data.addStringColumn("Value");
            textAnnotations.stream().sorted(Comparator.comparing(JIPipeTextAnnotation::getName, NaturalOrderComparator.INSTANCE)).forEach(annotation -> {
                int row = data.addRow();
                data.setValueAt(annotation.getName(), row, "Name");
                data.setValueAt(annotation.getValue(), row, "Value");
            });

            String displayName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") +
                    "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row;
            TableEditor.openWindow(getWorkbench(), data, displayName + " [Annotations]");
        }
    }

    private void exportAsFolder() {
        if (dataTableStore.isPresent()) {
            JIPipeDataTable dataTable = dataTableStore.get();
            Path path = FileChooserSettings.saveDirectory(getWorkbench().getWindow(),
                    FileChooserSettings.LastDirectoryKey.Data,
                    "Export " + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") +
                            "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row);
            if (path != null) {
                try {
                    Files.createDirectories(path);
                    JIPipeRunnable runnable = new ExportAsFolderRun(row, dataTable, path);
                    JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
                } catch (Exception e) {
                    UIUtils.openErrorDialog(getWorkbench(), getWorkbench().getWindow(), e);
                }
            }
        }
    }

    private void exportToFolder() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            Path path = FileChooserSettings.saveFile(getWorkbench().getWindow(),
                    FileChooserSettings.LastDirectoryKey.Data,
                    "Export " + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") + "/"
                            + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row);
            if (path != null) {
                JIPipeRunnable runnable = new ExportToFolderRun(row, dataTable, path);
                JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
            }
        }
    }

    private void runDisplayOperation(JIPipeDataDisplayOperation operation, JIPipeDataAnnotation dataAnnotation) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            try (BusyCursor cursor = new BusyCursor(this)) {
                JIPipeData data = dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo());
                String displayName;
                String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
                String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
                if (!StringUtils.isNullOrEmpty(nodeName))
                    displayName = nodeName + "/" + slotName + "/" + row + "/$" + dataAnnotation.getName();
                else
                    displayName = slotName + "/" + row + "/$" + dataAnnotation.getName();

                operation.display(data, displayName, getWorkbench(), new JIPipeDataTableDataSource(dataTable, row, dataAnnotation.getName()));
            }
        }
    }

    private void runDisplayOperation(JIPipeDataDisplayOperation operation) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            try (BusyCursor cursor = new BusyCursor(this)) {
                operation.display(dataTable, row, getWorkbench(), true);
            }
        }
    }

    private JIPipeDataDisplayOperation getMainOperation() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            if (!displayOperations.isEmpty()) {
                JIPipeDataDisplayOperation result = displayOperations.get(0);
                String dataTypeId = JIPipe.getDataTypes().getIdOf(dataTable.getAcceptedDataType());
                DynamicDataDisplayOperationIdEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
                if (parameter != null) {
                    String defaultName = parameter.getValue();
                    for (JIPipeDataDisplayOperation operation : displayOperations) {
                        if (Objects.equals(operation.getId(), defaultName)) {
                            result = operation;
                            break;
                        }
                    }
                }
                if (result == null) {
                    result = JIPipe.getDataTypes().getAllRegisteredDisplayOperations(dataTypeId).get("jipipe:show");
                }
                return result;
            }
        }
        return null;
    }

    public JButton getDataAnnotationsButton() {
        return dataAnnotationsButton;
    }

    public JButton getTextAnnotationsButton() {
        return textAnnotationsButton;
    }

    public JIPipeDataTable getDataTable() {
        return dataTableStore.get();
    }

    public int getRow() {
        return row;
    }

    public List<Store<JIPipeDataAnnotation>> getDataAnnotationStores() {
        return dataAnnotationStores;
    }

    public List<JIPipeDataDisplayOperation> getDisplayOperations() {
        return displayOperations;
    }

    /**
     * Runs the currently set default action for this data
     */
    public void handleDefaultAction() {
        JIPipeDataDisplayOperation mainOperation = getMainOperation();
        if (mainOperation != null) {
            runDisplayOperation(mainOperation);
        }
    }

    /**
     * Runs the currently set default action for this data.
     * If the data column index is valid, the associated data annotation is displayed instead (using its appropriate standard action)
     *
     * @param dataAnnotationColumn column index of the data column in the data table. if outside the range, silently will run the default data operation instead
     */
    public void handleDefaultActionOrDisplayDataAnnotation(int dataAnnotationColumn) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            if (dataAnnotationColumn >= 0 && dataAnnotationColumn < dataTable.getDataAnnotationColumns().size()) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(getRow(), dataTable.getDataAnnotationColumns().get(dataAnnotationColumn));
                JIPipeDataDisplayOperation operation = getMainOperation(dataAnnotation.getDataClass());
                runDisplayOperation(operation, dataAnnotation);
            } else {
                handleDefaultAction();
            }
        }
    }

    private void copyString() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            String string = "" + dataTable.getData(row, JIPipeData.class, new JIPipeProgressInfo());
            StringSelection selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    private static class ExportAsFolderRun implements JIPipeRunnable {

        private final int row;
        private final Path path;
        private JIPipeDataTable dataTable;
        private JIPipeProgressInfo progressInfo;

        public ExportAsFolderRun(int row, JIPipeDataTable dataTable, Path path) {
            this.row = row;
            this.dataTable = dataTable;
            this.path = path;
            progressInfo = new JIPipeProgressInfo();
        }

        @Override
        public JIPipeProgressInfo getProgressInfo() {
            return progressInfo;
        }

        @Override
        public void setProgressInfo(JIPipeProgressInfo progressInfo) {
            this.progressInfo = progressInfo;
        }

        @Override
        public String getTaskLabel() {
            return "Export";
        }

        @Override
        public void run() {
            try {
                JIPipeData data = dataTable.getData(row, JIPipeData.class, progressInfo);
                data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path), "data", false, progressInfo);
            } finally {
                dataTable = null;
            }
        }


    }

    private static class ExportToFolderRun implements JIPipeRunnable {

        private final int row;
        private final Path path;
        private JIPipeDataTable dataTable;
        private JIPipeProgressInfo progressInfo;

        public ExportToFolderRun(int row, JIPipeDataTable dataTable, Path path) {
            this.row = row;
            this.dataTable = dataTable;
            this.path = path;
            progressInfo = new JIPipeProgressInfo();
        }

        @Override
        public JIPipeProgressInfo getProgressInfo() {
            return progressInfo;
        }

        @Override
        public void setProgressInfo(JIPipeProgressInfo progressInfo) {
            this.progressInfo = progressInfo;
        }

        @Override
        public String getTaskLabel() {
            return "Export";
        }

        @Override
        public void run() {
            try {
                JIPipeData data = dataTable.getData(row, JIPipeData.class, progressInfo);
                data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path.getParent()), path.getFileName().toString(), true, progressInfo);
            } finally {
                dataTable = null;
            }
        }


    }
}
