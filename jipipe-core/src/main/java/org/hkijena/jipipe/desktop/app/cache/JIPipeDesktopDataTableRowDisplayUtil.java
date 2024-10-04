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

package org.hkijena.jipipe.desktop.app.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultCacheDisplayApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * UI for a row
 */
public class JIPipeDesktopDataTableRowDisplayUtil implements JIPipeDesktopWorkbenchAccess {
    private final JIPipeDesktopWorkbench workbench;
    private final Store<JIPipeDataTable> dataTableStore;
    private final int row;
    private final List<Store<JIPipeDataAnnotation>> dataAnnotationStores;
    private final List<JIPipeDesktopDataDisplayOperation> displayOperations;

    /**
     * Creates a new instance
     *
     * @param workbench      the workbench
     * @param dataTableStore the data table store
     * @param row            the row
     */
    public JIPipeDesktopDataTableRowDisplayUtil(JIPipeDesktopWorkbench workbench, Store<JIPipeDataTable> dataTableStore, int row) {
        this.workbench = workbench;
        this.dataTableStore = dataTableStore;
        this.row = row;
        this.dataAnnotationStores = new ArrayList<>();
        for (JIPipeDataAnnotation dataAnnotation : dataTableStore.get().getDataAnnotations(row)) {
            dataAnnotationStores.add(new WeakStore<>(dataAnnotation));
        }
        Class<? extends JIPipeData> dataClass = dataTableStore.get().getDataClass(row);
        String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId);
    }

    public static JIPipeDesktopDataDisplayOperation getMainOperation(Class<? extends JIPipeData> dataClass) {
        String dataTypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
        List<JIPipeDesktopDataDisplayOperation> displayOperations = JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(dataTypeId);
        if (!displayOperations.isEmpty()) {
            JIPipeDesktopDataDisplayOperation result = displayOperations.get(0);
            DynamicDataDisplayOperationIdEnumParameter parameter = JIPipeDefaultCacheDisplayApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
            if (parameter != null) {
                String defaultName = parameter.getValue();
                for (JIPipeDesktopDataDisplayOperation operation : displayOperations) {
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

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public void runDisplayOperation(JIPipeDesktopDataDisplayOperation operation, JIPipeDataAnnotation dataAnnotation) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            try (BusyCursor cursor = new BusyCursor(getDesktopWorkbench().getWindow())) {
                JIPipeData data = dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo());
                String displayName;
                String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
                String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
                if (!StringUtils.isNullOrEmpty(nodeName))
                    displayName = nodeName + "/" + slotName + "/" + row + "/$" + dataAnnotation.getName();
                else
                    displayName = slotName + "/" + row + "/$" + dataAnnotation.getName();

                operation.display(data, displayName, getDesktopWorkbench(), new JIPipeDataTableDataSource(dataTable, row, dataAnnotation.getName()));
            }
        }
    }

    public void runDisplayOperation(JIPipeDesktopDataDisplayOperation operation) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            try (BusyCursor cursor = new BusyCursor(getDesktopWorkbench().getWindow())) {
                operation.display(dataTable, row, getDesktopWorkbench(), true);
            }
        }
    }

    public JIPipeDesktopDataDisplayOperation getMainOperation() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            if (!displayOperations.isEmpty()) {
                JIPipeDesktopDataDisplayOperation result = displayOperations.get(0);
                String dataTypeId = JIPipe.getDataTypes().getIdOf(dataTable.getAcceptedDataType());
                DynamicDataDisplayOperationIdEnumParameter parameter = JIPipeDefaultCacheDisplayApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
                if (parameter != null) {
                    String defaultName = parameter.getValue();
                    for (JIPipeDesktopDataDisplayOperation operation : displayOperations) {
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

    public JIPipeDataTable getDataTable() {
        return dataTableStore.get();
    }

    public int getRow() {
        return row;
    }

    public List<Store<JIPipeDataAnnotation>> getDataAnnotationStores() {
        return dataAnnotationStores;
    }

    public List<JIPipeDesktopDataDisplayOperation> getDisplayOperations() {
        return displayOperations;
    }

    /**
     * Runs the currently set default action for this data
     */
    public void handleDefaultAction() {
        JIPipeDesktopDataDisplayOperation mainOperation = getMainOperation();
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
            if (dataAnnotationColumn >= 0 && dataAnnotationColumn < dataTable.getDataAnnotationColumnNames().size()) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(getRow(), dataTable.getDataAnnotationColumnNames().get(dataAnnotationColumn));
                JIPipeDesktopDataDisplayOperation operation = getMainOperation(dataAnnotation.getDataClass());
                runDisplayOperation(operation, dataAnnotation);
            } else {
                handleDefaultAction();
            }
        }
    }

    public void copyString() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            String string = "" + dataTable.getData(row, JIPipeData.class, new JIPipeProgressInfo());
            StringSelection selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

}
