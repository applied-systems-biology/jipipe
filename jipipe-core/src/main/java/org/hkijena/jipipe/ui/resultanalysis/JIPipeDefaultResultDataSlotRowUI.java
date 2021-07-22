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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTableRow;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public JIPipeDefaultResultDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTableRow row) {
        super(workbenchUI, slot, row);
        String datatypeId = row.getTrueDataType();
        importOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedImportOperationsFor(datatypeId);
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (getRow().getDataAnnotations().size() > 0) {
            JButton dataAnnotationButton = new JButton("Data annotations ...", UIUtils.getIconFromResources("data-types/data-annotation.png"));
            JPopupMenu menu = UIUtils.addPopupMenuToComponent(dataAnnotationButton);

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
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
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
                        JIPipeData data = JIPipe.importData(getRowStorageFolder(), JIPipe.getDataTypes().getById(getRow().getTrueDataType()));
                        data.saveTo(path, "data", false, progressInfo.resolveAndLog("Saving data"));
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
                    JIPipeData data = JIPipe.importData(getRowStorageFolder(), JIPipe.getDataTypes().getById(getRow().getTrueDataType()));
                    data.saveTo(path.getParent(), path.getFileName().toString(), true, progressInfo.resolveAndLog("Exporting data"));
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
            operation.show(getSlot(), getRow(), dataAnnotation.getName(), getSlot().getStoragePath().resolve(dataAnnotation.getRowStorageFolder()), getAlgorithmCompartmentName(), getAlgorithmName(), getDisplayName(), getWorkbench());
        }
    }

    private void runImportOperation(JIPipeDataImportOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            operation.show(getSlot(), getRow(), null, getRowStorageFolder(), getAlgorithmCompartmentName(), getAlgorithmName(), getDisplayName(), getWorkbench());
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

}
