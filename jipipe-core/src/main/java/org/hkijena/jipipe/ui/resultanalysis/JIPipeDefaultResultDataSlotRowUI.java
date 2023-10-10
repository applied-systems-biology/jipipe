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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Provides a standard result slot UI that can be also further extended.
 * Override registerActions() for this
 */
public class JIPipeDefaultResultDataSlotRowUI extends JIPipeResultDataSlotRowUI {

    private final List<JIPipeDataImportOperation> importOperations;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public JIPipeDefaultResultDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row) {
        super(workbenchUI, slot, row);
        String datatypeId = row.getTrueDataType();
        importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(datatypeId);
        initialize();
    }

    public static JIPipeDataImportOperation getMainOperation(Class<? extends JIPipeData> dataClass) {
        String dataTypeId = JIPipe.getDataTypes().getIdOf(dataClass);
        List<JIPipeDataImportOperation> importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(dataTypeId);
        if (!importOperations.isEmpty()) {
            JIPipeDataImportOperation result = importOperations.get(0);
            DynamicDataImportOperationIdEnumParameter parameter = DefaultResultImporterSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDataImportOperation operation : importOperations) {
                    if (Objects.equals(operation.getId(), defaultName)) {
                        result = operation;
                        break;
                    }
                }
            }
            if (result == null) {
                result = JIPipe.getDataTypes().getAllRegisteredImportOperations(dataTypeId).get("jipipe:show");
            }
            return result;
        }
        return null;
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (getRow().getDataAnnotations().size() > 0) {
            JButton dataAnnotationButton = new JButton("Data annotations ...", UIUtils.getIconFromResources("data-types/data-annotation.png"));
            JPopupMenu menu = UIUtils.addPopupMenuToButton(dataAnnotationButton);

            for (JIPipeExportedDataAnnotation dataAnnotation : getRow().getDataAnnotations()) {
                JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(dataAnnotation.getTrueDataType());
                JMenu subMenu = new JMenu(dataAnnotation.getName());
                subMenu.setIcon(JIPipe.getDataTypes().getIconFor(dataInfo.getDataClass()));
                List<JIPipeDataImportOperation> importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(dataInfo.getId());
                for (JIPipeDataImportOperation importOperation : importOperations) {
                    JMenuItem item = new JMenuItem(importOperation.getName(), importOperation.getIcon());
                    item.setToolTipText(importOperation.getDescription());
                    item.addActionListener(e -> runImportOperation(importOperation, dataAnnotation));
                    subMenu.add(item);
                }
                menu.add(subMenu);
            }

            add(dataAnnotationButton);
        }
        if (!getRow().getTextAnnotations().isEmpty()) {
            JButton annotationButton = new JButton("Annotations ...", UIUtils.getIconFromResources("data-types/annotation.png"));
            JPopupMenu annotationMenu = UIUtils.addPopupMenuToButton(annotationButton);
            {
                JMenuItem toTableItem = new JMenuItem("Display as table", UIUtils.getIconFromResources("data-types/results-table.png"));
                toTableItem.addActionListener(e -> displayAnnotationsAsTable(getRow().getTextAnnotations()));
                annotationMenu.add(toTableItem);
            }
            for (JIPipeTextAnnotation annotation : getRow().getTextAnnotations()) {
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
            add(annotationButton);
        }

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

        if (!importOperations.isEmpty()) {
            JIPipeDataImportOperation mainOperation = getMainOperation();
            if (mainOperation == null)
                return;
            JButton mainActionButton = new JButton(mainOperation.getName(), mainOperation.getIcon());
            mainActionButton.setToolTipText(mainOperation.getDescription());
            mainActionButton.addActionListener(e -> runImportOperation(mainOperation));
            add(mainActionButton);

            if (importOperations.size() > 1) {
                JButton menuButton = new JButton("Open with ...");
                menuButton.setMaximumSize(new Dimension(1, (int) mainActionButton.getPreferredSize().getHeight()));
                menuButton.setToolTipText("Shows more actions to display the data. On selecting an entry, " +
                        "it becomes the default action.");
                JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
                for (JIPipeDataImportOperation otherSlotAction : importOperations) {
                    if (otherSlotAction == mainOperation)
                        continue;
                    JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                    item.setToolTipText(otherSlotAction.getDescription());
                    item.addActionListener(e -> runImportOperation(otherSlotAction));
                    menu.add(item);
                }
                add(menuButton);
            }
        }
    }

    private void displayAnnotationsAsTable(List<JIPipeTextAnnotation> textAnnotations) {
        ResultsTableData data = new ResultsTableData();
        data.addStringColumn("Name");
        data.addStringColumn("Value");
        textAnnotations.stream().sorted(Comparator.comparing(JIPipeTextAnnotation::getName, NaturalOrderComparator.INSTANCE)).forEach(annotation -> {
            int row = data.addRow();
            data.setValueAt(annotation.getName(), row, "Name");
            data.setValueAt(annotation.getValue(), row, "Value");
        });

        TableEditor.openWindow(getWorkbench(), data, getDisplayName() + " [Annotations]");
    }

    private void exportAsFolder() {
        Path path = FileChooserSettings.saveDirectory(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.Data, "Export " + getDisplayName());
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
                        progressInfo.log("Importing data from " + getRowStorageFolder() + " ...");
                        JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, getRowStorageFolder()),
                                JIPipe.getDataTypes().getById(getRow().getTrueDataType()), progressInfo);
                        data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path), "data", false, progressInfo.resolveAndLog("Saving data"));
                    }

                    @Override
                    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
                        this.progressInfo = progressInfo;
                    }


                };
                JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
            } catch (Exception e) {
                UIUtils.openErrorDialog(getWorkbench(), getWorkbench().getWindow(), e);
            }
        }
    }

    private void exportToFolder() {
        Path path = FileChooserSettings.saveFile(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.Data, "Export " + getDisplayName());
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
                    progressInfo.log("Importing data from " + getRowStorageFolder() + " ...");
                    JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, getRowStorageFolder()), JIPipe.getDataTypes().getById(getRow().getTrueDataType()), progressInfo);
                    data.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path.getParent()), path.getFileName().toString(), true, progressInfo.resolveAndLog("Exporting data"));
                }

                @Override
                public void setProgressInfo(JIPipeProgressInfo progressInfo) {
                    this.progressInfo = progressInfo;
                }


            };
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), runnable);
        }
    }

    private void runImportOperation(JIPipeDataImportOperation operation, JIPipeExportedDataAnnotation dataAnnotation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(),
                    getRow(),
                    dataAnnotation.getName(),
                    getSlot().getSlotStoragePath().resolve(dataAnnotation.getRowStorageFolder()),
                    getAlgorithmCompartmentName(),
                    getAlgorithmName(),
                    getDisplayName(),
                    getWorkbench(),
                    new JIPipeProgressInfo());
        }
    }

    private void runImportOperation(JIPipeDataImportOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(),
                    getRow(),
                    null,
                    getRowStorageFolder(),
                    getAlgorithmCompartmentName(),
                    getAlgorithmName(),
                    getDisplayName(),
                    getWorkbench(),
                    new JIPipeProgressInfo());
            if (GeneralDataSettings.getInstance().isAutoSaveLastImporter()) {
                String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
                DynamicDataImportOperationIdEnumParameter parameter = DefaultResultImporterSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
                if (parameter != null && !Objects.equals(operation.getId(), parameter.getValue())) {
                    parameter.setValue(operation.getId());
                    DefaultResultImporterSettings.getInstance().setValue(dataTypeId, parameter);
                    JIPipe.getSettings().save();
                }
            }
        }
    }

    private JIPipeDataImportOperation getMainOperation() {
        if (!importOperations.isEmpty()) {
            JIPipeDataImportOperation result = importOperations.get(0);
            String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
            DynamicDataImportOperationIdEnumParameter parameter = DefaultResultImporterSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDataImportOperation operation : importOperations) {
                    if (Objects.equals(operation.getId(), defaultName)) {
                        result = operation;
                        break;
                    }
                }
            }
            if (result == null) {
                result = JIPipe.getDataTypes().getAllRegisteredImportOperations(dataTypeId).get("jipipe:show");
            }
            return result;
        }
        return null;
    }

    @Override
    public void handleDefaultAction() {
        JIPipeDataImportOperation mainOperation = getMainOperation();
        if (mainOperation != null)
            SwingUtilities.invokeLater(() -> runImportOperation(mainOperation));
    }

    @Override
    public void handleDefaultActionOrDisplayDataAnnotation(int dataAnnotationColumn) {
        if (dataAnnotationColumn >= 0 && dataAnnotationColumn < getRow().getDataAnnotations().size()) {
            String dataTypeId = getRow().getDataAnnotations().get(dataAnnotationColumn).getTrueDataType();
            Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(dataTypeId);
            JIPipeDataImportOperation operation = getMainOperation(dataClass);
            if (operation != null) {
                SwingUtilities.invokeLater(() -> runImportOperation(operation, getRow().getDataAnnotations().get(dataAnnotationColumn)));
            }
        } else {
            handleDefaultAction();
        }
    }
}
