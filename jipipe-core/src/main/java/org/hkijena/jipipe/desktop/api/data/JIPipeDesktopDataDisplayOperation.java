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

package org.hkijena.jipipe.desktop.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultCacheDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultResultImporterApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Objects;

/**
 * An operation that is executed on showing existing data located in memory/cache.
 * This acts as additional entry in the cache browser display menu. Must be registered.
 */
public interface JIPipeDesktopDataDisplayOperation extends JIPipeDataOperation {

    /**
     * Shows the data in the UI
     *
     * @param data             the data
     * @param displayName      the display name
     * @param desktopWorkbench the workbench that issued the command
     * @param source           optional source of the data. Can by null or any kind of object (e.g. {@link JIPipeDataSlot})
     */
    void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source);


    /**
     * Applies the operation on a data table
     *
     * @param dataTable        the data table
     * @param row              the model row
     * @param desktopWorkbench the workbench
     * @param saveAsDefault    saves the operation as new default
     */
    default void display(JIPipeDataTable dataTable, int row, JIPipeDesktopWorkbench desktopWorkbench, boolean saveAsDefault) {
        JIPipeData data = dataTable.getData(row, JIPipeData.class, new JIPipeProgressInfo());
        String displayName;
        String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
        String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
        if (!StringUtils.isNullOrEmpty(nodeName))
            displayName = nodeName + "/" + slotName + "/" + row;
        else
            displayName = slotName + "/" + row;
        display(data, displayName, desktopWorkbench, new JIPipeDataTableDataSource(dataTable, row));
        if (saveAsDefault && JIPipeGeneralDataApplicationSettings.getInstance().isAutoSaveLastDisplay()) {
            String dataTypeId = JIPipe.getDataTypes().getIdOf(dataTable.getAcceptedDataType());
            DynamicDataDisplayOperationIdEnumParameter parameter = JIPipeDefaultCacheDisplayApplicationSettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
            if (parameter != null && !Objects.equals(getId(), parameter.getValue())) {
                parameter.setValue(getId());
                JIPipeDefaultResultImporterApplicationSettings.getInstance().setValue(dataTypeId, parameter);
                if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                    JIPipe.getSettings().save();
                }
            }
        }
    }

    /**
     * Applies the operation on a data table
     *
     * @param dataTable        the data table
     * @param row              the model row
     * @param dataAnnotation   the data annotation to show
     * @param desktopWorkbench the workbench
     */
    default void displayDataAnnotation(JIPipeDataTable dataTable, int row, JIPipeDataAnnotation dataAnnotation, JIPipeDesktopWorkbench desktopWorkbench) {
        JIPipeData data = dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo());
        String displayName;
        String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
        String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
        if (!StringUtils.isNullOrEmpty(nodeName))
            displayName = nodeName + "/" + slotName + "/" + row + "/$" + dataAnnotation.getName();
        else
            displayName = slotName + "/" + row + "/$" + dataAnnotation.getName();

        display(data, displayName, desktopWorkbench, new JIPipeDataTableDataSource(dataTable, row, dataAnnotation.getName()));
    }
}
