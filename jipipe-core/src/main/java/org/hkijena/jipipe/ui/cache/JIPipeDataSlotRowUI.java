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
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicDataDisplayOperationIdEnumParameter;
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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * UI for a row
 */
public class JIPipeDataSlotRowUI extends JIPipeWorkbenchPanel {
    private final JIPipeDataSlot slot;
    private final int row;
    private final List<JIPipeDataAnnotation> dataAnnotations;
    private List<JIPipeDataDisplayOperation> displayOperations;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     * @param slot      the slot
     * @param row       the row
     */
    public JIPipeDataSlotRowUI(JIPipeWorkbench workbench, JIPipeDataSlot slot, int row) {
        super(workbench);
        this.slot = slot;
        this.row = row;
        this.dataAnnotations = slot.getDataAnnotations(row);
        Class<? extends JIPipeData> dataClass = slot.getDataClass(row);
        String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
        this.initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (dataAnnotations.size() > 0) {
            JButton dataAnnotationButton = new JButton("Data annotations ...", UIUtils.getIconFromResources("data-types/data-annotation.png"));
            JPopupMenu menu = UIUtils.addPopupMenuToComponent(dataAnnotationButton);

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

            add(dataAnnotationButton);
        }

        if(!slot.getAnnotations(row).isEmpty()) {
            JButton annotationButton = new JButton("Annotations ...", UIUtils.getIconFromResources("data-types/annotation.png"));
            JPopupMenu annotationMenu = UIUtils.addPopupMenuToComponent(annotationButton);
            for (JIPipeAnnotation annotation : slot.getAnnotations(row)) {
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
            add(annotationButton);
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
        Path path = FileChooserSettings.saveDirectory(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.Data, "Export " + slot.getNode().getName() + "/" + slot.getName() + "/" + row);
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
                        JIPipeData data = slot.getData(row, JIPipeData.class, progressInfo);
                        data.saveTo(path, "data", false, progressInfo);
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
        Path path = FileChooserSettings.saveFile(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.Data, "Export " + slot.getNode().getName() + "/" + slot.getName() + "/" + row);
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
                    JIPipeData data = slot.getData(row, JIPipeData.class, progressInfo);
                    data.saveTo(path.getParent(), path.getFileName().toString(), true, progressInfo);
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
            if (slot.getNode() != null)
                displayName = slot.getNode().getName() + "/" + slot.getName() + "/" + row + "/$" + dataAnnotation.getName();
            else
                displayName = slot.getName() + "/" + row + "/$" + dataAnnotation.getName();
            ;
            operation.display(data, displayName, getWorkbench(), new JIPipeCacheSlotDataSource(slot, row, dataAnnotation.getName()));
        }
    }

    private void runDisplayOperation(JIPipeDataDisplayOperation operation) {
        try (BusyCursor cursor = new BusyCursor(this)) {
            JIPipeData data = slot.getData(row, JIPipeData.class, new JIPipeProgressInfo());
            String displayName;
            if (slot.getNode() != null)
                displayName = slot.getNode().getName() + "/" + slot.getName() + "/" + row;
            else
                displayName = slot.getName() + "/" + row;
            operation.display(data, displayName, getWorkbench(), new JIPipeCacheSlotDataSource(slot, row));
            if (GeneralDataSettings.getInstance().isAutoSaveLastDisplay()) {
                String dataTypeId = JIPipe.getDataTypes().getIdOf(slot.getAcceptedDataType());
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
            String dataTypeId = JIPipe.getDataTypes().getIdOf(slot.getAcceptedDataType());
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

    public void handleDefaultAction() {
        JIPipeDataDisplayOperation mainOperation = getMainOperation();
        if (mainOperation != null) {
            runDisplayOperation(mainOperation);
        }
    }

    private void copyString() {
        String string = "" + slot.getData(row, JIPipeData.class, new JIPipeProgressInfo());
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
