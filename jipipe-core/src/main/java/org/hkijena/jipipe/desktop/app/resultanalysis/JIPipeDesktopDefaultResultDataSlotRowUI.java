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

package org.hkijena.jipipe.desktop.app.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataImportOperation;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataAnnotationInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultResultImporterApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
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
import java.util.UUID;

/**
 * Provides a standard result slot UI that can be also further extended.
 * Override registerActions() for this
 */
public class JIPipeDesktopDefaultResultDataSlotRowUI extends JIPipeDesktopResultDataSlotRowUI {

    private final List<JIPipeLegacyDataImportOperation> importOperations;

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public JIPipeDesktopDefaultResultDataSlotRowUI(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeDataTableRowInfo row) {
        super(workbenchUI, slot, row);
        String datatypeId = row.getTrueDataType();
        importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(datatypeId);
        initialize();
    }

    public static JIPipeLegacyDataImportOperation getMainOperation(Class<? extends JIPipeData> dataClass) {
        String dataTypeId = JIPipe.getDataTypes().getIdOf(dataClass);
        List<JIPipeLegacyDataImportOperation> importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(dataTypeId);
        if (!importOperations.isEmpty()) {
            JIPipeLegacyDataImportOperation result = importOperations.get(0);
            DynamicDataImportOperationIdEnumParameter parameter = JIPipeDefaultResultImporterApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeLegacyDataImportOperation operation : importOperations) {
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

            for (JIPipeDataAnnotationInfo dataAnnotation : getRow().getDataAnnotations()) {
                JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(dataAnnotation.getTrueDataType());
                JMenu subMenu = new JMenu(dataAnnotation.getName());
                subMenu.setIcon(JIPipe.getDataTypes().getIconFor(dataInfo.getDataClass()));
                List<JIPipeLegacyDataImportOperation> importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(dataInfo.getId());
                for (JIPipeLegacyDataImportOperation importOperation : importOperations) {
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
                copyAnnotationNameAsVariableItem.addActionListener(e -> UIUtils.copyToClipboard(JIPipeExpressionEvaluator.escapeVariable(annotation.getName())));
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

        JMenuItem exportToFolderItem = new JMenuItem("Export to folder", UIUtils.getIconFromResources("actions/download.png"));
        exportToFolderItem.setToolTipText("Saves the data to a folder. If multiple files are present, the names will be generated according to the selected name.");
        exportToFolderItem.addActionListener(e -> exportToFolder());
        exportMenu.add(exportToFolderItem);

        JMenuItem exportAsFolderItem = new JMenuItem("Export as folder", UIUtils.getIconFromResources("actions/folder-new.png"));
        exportAsFolderItem.setToolTipText("Saves the data into a new folder. Files will be named according to the data type standard.");
        exportAsFolderItem.addActionListener(e -> exportAsFolder());
        exportMenu.add(exportAsFolderItem);

        add(exportButton);

        if (!importOperations.isEmpty()) {
            JIPipeLegacyDataImportOperation mainOperation = getMainOperation();
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
                for (JIPipeLegacyDataImportOperation otherSlotAction : importOperations) {
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

        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), data, getDisplayName() + " [Annotations]");
    }

    private void exportAsFolder() {
        Path path = JIPipeDesktop.saveDirectory(getDesktopWorkbench().getWindow(), getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export " + getDisplayName(), HTMLText.EMPTY);
        if (path != null) {
            try {
                Files.createDirectories(path);
                JIPipeRunnable runnable = new JIPipeRunnable() {
                    private final UUID uuid = UUID.randomUUID();
                    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

                    @Override
                    public UUID getRunUUID() {
                        return uuid;
                    }

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
                JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), runnable);
            } catch (Exception e) {
                UIUtils.showErrorDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), e);
            }
        }
    }

    private void exportToFolder() {
        Path path = JIPipeDesktop.saveFile(getDesktopWorkbench().getWindow(), getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export " + getDisplayName(), HTMLText.EMPTY);
        if (path != null) {
            JIPipeRunnable runnable = new JIPipeRunnable() {
                private final UUID uuid = UUID.randomUUID();
                private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

                @Override
                public UUID getRunUUID() {
                    return uuid;
                }

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
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), runnable);
        }
    }

    private void runImportOperation(JIPipeLegacyDataImportOperation operation, JIPipeDataAnnotationInfo dataAnnotation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(),
                    getRow(),
                    dataAnnotation.getName(),
                    getSlot().getSlotStoragePath().resolve(dataAnnotation.getRowStorageFolder()),
                    getAlgorithmCompartmentName(),
                    getAlgorithmName(),
                    getDisplayName(),
                    getDesktopWorkbench(),
                    new JIPipeProgressInfo());
        }
    }

    private void runImportOperation(JIPipeLegacyDataImportOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(),
                    getRow(),
                    null,
                    getRowStorageFolder(),
                    getAlgorithmCompartmentName(),
                    getAlgorithmName(),
                    getDisplayName(),
                    getDesktopWorkbench(),
                    new JIPipeProgressInfo());
            if (JIPipeGeneralDataApplicationSettings.getInstance().isAutoSaveLastImporter()) {
                String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
                DynamicDataImportOperationIdEnumParameter parameter = JIPipeDefaultResultImporterApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
                if (parameter != null && !Objects.equals(operation.getId(), parameter.getValue())) {
                    parameter.setValue(operation.getId());
                    JIPipeDefaultResultImporterApplicationSettings.getInstance().setValue(dataTypeId, parameter);
                    if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                        JIPipe.getSettings().save();
                    }
                }
            }
        }
    }

    private JIPipeLegacyDataImportOperation getMainOperation() {
        if (!importOperations.isEmpty()) {
            JIPipeLegacyDataImportOperation result = importOperations.get(0);
            String dataTypeId = JIPipe.getDataTypes().getIdOf(getSlot().getAcceptedDataType());
            DynamicDataImportOperationIdEnumParameter parameter = JIPipeDefaultResultImporterApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataImportOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeLegacyDataImportOperation operation : importOperations) {
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
        JIPipeLegacyDataImportOperation mainOperation = getMainOperation();
        if (mainOperation != null)
            SwingUtilities.invokeLater(() -> runImportOperation(mainOperation));
    }

    @Override
    public void handleDefaultActionOrDisplayDataAnnotation(int dataAnnotationColumn) {
        if (dataAnnotationColumn >= 0 && dataAnnotationColumn < getRow().getDataAnnotations().size()) {
            String dataTypeId = getRow().getDataAnnotations().get(dataAnnotationColumn).getTrueDataType();
            Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(dataTypeId);
            JIPipeLegacyDataImportOperation operation = getMainOperation(dataClass);
            if (operation != null) {
                SwingUtilities.invokeLater(() -> runImportOperation(operation, getRow().getDataAnnotations().get(dataAnnotationColumn)));
            }
        } else {
            handleDefaultAction();
        }
    }
}
