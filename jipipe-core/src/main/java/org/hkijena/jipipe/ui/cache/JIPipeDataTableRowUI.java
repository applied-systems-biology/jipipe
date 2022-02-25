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
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteStorage;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultCacheDisplaySettings;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * UI for a row
 */
public class JIPipeDataTableRowUI extends JIPipeWorkbenchPanel {
    private final JIPipeDataTable dataTable;
    private final int row;
    private final List<JIPipeDataAnnotation> dataAnnotations;
    private final List<JIPipeDataDisplayOperation> displayOperations;
    private JButton dataAnnotationsButton;
    private JButton textAnnotationsButton;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     * @param dataTable      the slot
     * @param row       the row
     */
    public JIPipeDataTableRowUI(JIPipeWorkbench workbench, JIPipeDataTable dataTable, int row) {
        super(workbench);
        this.dataTable = dataTable;
        this.row = row;
        this.dataAnnotations = dataTable.getDataAnnotations(row);
        Class<? extends JIPipeData> dataClass = dataTable.getDataClass(row);
        String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
        this.initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (dataAnnotations.size() > 0) {
            dataAnnotationsButton = new JButton("Data annotations ...", UIUtils.getIconFromResources("data-types/data-annotation.png"));
            JPopupMenu menu = UIUtils.addPopupMenuToComponent(dataAnnotationsButton);

            for (JIPipeDataAnnotation dataAnnotation : dataAnnotations) {
                JMenu subMenu = new JMenu(dataAnnotation.getName());
                subMenu.setIcon(JIPipe.getDataTypes().getIconFor(dataAnnotation.getDataClass()));
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataAnnotation.getDataClass());
                List<JIPipeDataDisplayOperation> displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
                for (JIPipeDataDisplayOperation displayOperation : displayOperations) {
                    JMenuItem item = new JMenuItem(displayOperation.getName(), displayOperation.getIcon());
                    item.setToolTipText(displayOperation.getDescription());
                    item.addActionListener(e -> runDisplayOperation(displayOperation, dataAnnotation));
                    subMenu.add(item);
                }
                menu.add(subMenu);
            }

            add(dataAnnotationsButton);
        }

        if (!dataTable.getTextAnnotations(row).isEmpty()) {
            textAnnotationsButton = new JButton("Annotations ...", UIUtils.getIconFromResources("data-types/annotation.png"));
            JPopupMenu annotationMenu = UIUtils.addPopupMenuToComponent(textAnnotationsButton);
            for (JIPipeTextAnnotation annotation : dataTable.getTextAnnotations(row)) {
                JMenu entryMenu = new JMenu(annotation.getName());
                entryMenu.setIcon(UIUtils.getIconFromResources("data-types/annotation.png"));

                JMenuItem valueItem = new JMenuItem(StringUtils.nullToEmpty(annotation.getValue()), UIUtils.getIconFromResources("actions/equals.png"));
                valueItem.setEnabled(false);
                entryMenu.add(valueItem);

                entryMenu.addSeparator();

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
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

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
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
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

    private void exportAsFolder() {
        Path path = FileChooserSettings.saveDirectory(getWorkbench().getWindow(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Export " + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") +
                        "/" + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row);
        if (path != null) {
            try {
                Files.createDirectories(path);
                JIPipeRunnable runnable = new JIPipeRunnable() {
                    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

                    @Override
                    public JIPipeProgressInfo getProgressInfo() {
                        return progressInfo;
                    }

                    @Override
                    public String getTaskLabel() {
                        return "Export";
                    }

                    @Override
                    public void run() {
                        JIPipeData data = dataTable.getData(row, JIPipeData.class, progressInfo);
                        data.exportData(new JIPipeFileSystemWriteStorage(path), "data", false, progressInfo);
                    }

                    @Override
                    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
                        this.progressInfo = progressInfo;
                    }


                };
                JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
            } catch (Exception e) {
                UIUtils.openErrorDialog(getWorkbench().getWindow(), e);
            }
        }
    }

    private void exportToFolder() {
        Path path = FileChooserSettings.saveFile(getWorkbench().getWindow(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Export " + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "") + "/"
                        + dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "") + "/" + row);
        if (path != null) {
            JIPipeRunnable runnable = new JIPipeRunnable() {
                private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

                @Override
                public JIPipeProgressInfo getProgressInfo() {
                    return progressInfo;
                }

                @Override
                public String getTaskLabel() {
                    return "Export";
                }

                @Override
                public void run() {
                    JIPipeData data = dataTable.getData(row, JIPipeData.class, progressInfo);
                    data.exportData(new JIPipeFileSystemWriteStorage(path.getParent()), path.getFileName().toString(), true, progressInfo);
                }

                @Override
                public void setProgressInfo(JIPipeProgressInfo progressInfo) {
                    this.progressInfo = progressInfo;
                }


            };
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
        }
    }

    private void runDisplayOperation(JIPipeDataDisplayOperation operation, JIPipeDataAnnotation dataAnnotation) {
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

    private void runDisplayOperation(JIPipeDataDisplayOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            JIPipeData data = dataTable.getData(row, JIPipeData.class, new JIPipeProgressInfo());
            String displayName;
            String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
            String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
            if (!StringUtils.isNullOrEmpty(nodeName))
                displayName = nodeName + "/" + slotName + "/" + row;
            else
                displayName = slotName + "/" + row;
            operation.display(data, displayName, getWorkbench(), new JIPipeDataTableDataSource(dataTable, row));
            if (GeneralDataSettings.getInstance().isAutoSaveLastDisplay()) {
                String dataTypeId = JIPipe.getDataTypes().getIdOf(dataTable.getAcceptedDataType());
                DynamicDataDisplayOperationIdEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
                if (parameter != null && !Objects.equals(operation.getId(), parameter.getValue())) {
                    parameter.setValue(operation.getId());
                    DefaultResultImporterSettings.getInstance().setValue(dataTypeId, parameter);
                    JIPipe.getSettings().save();
                }
            }
        }
    }

    private JIPipeDataDisplayOperation getMainOperation() {
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
        return null;
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

    public JButton getDataAnnotationsButton() {
        return dataAnnotationsButton;
    }

    public JButton getTextAnnotationsButton() {
        return textAnnotationsButton;
    }

    public JIPipeDataTable getDataTable() {
        return dataTable;
    }

    public int getRow() {
        return row;
    }

    public List<JIPipeDataAnnotation> getDataAnnotations() {
        return dataAnnotations;
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
     * @param dataAnnotationColumn column index of the data column in the data table. if outside the range, silently will run the default data operation instead
     */
    public void handleDefaultActionOrDisplayDataAnnotation(int dataAnnotationColumn) {
        if(dataAnnotationColumn >= 0 && dataAnnotationColumn < dataTable.getDataAnnotationColumns().size()) {
            JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(getRow(), dataTable.getDataAnnotationColumns().get(dataAnnotationColumn));
            JIPipeDataDisplayOperation operation = getMainOperation(dataAnnotation.getDataClass());
            runDisplayOperation(operation, dataAnnotation);
        }
        else {
            handleDefaultAction();
        }
    }

    private void copyString() {
        String string = "" + dataTable.getData(row, JIPipeData.class, new JIPipeProgressInfo());
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
