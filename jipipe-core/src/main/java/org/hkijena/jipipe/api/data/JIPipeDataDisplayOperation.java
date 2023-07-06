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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.DefaultCacheDisplaySettings;
import org.hkijena.jipipe.extensions.settings.DefaultResultImporterSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Objects;

/**
 * An operation that is executed on showing existing data located in memory/cache.
 * This acts as additional entry in the cache browser display menu. Must be registered.
 */
public interface JIPipeDataDisplayOperation extends JIPipeDataOperation {

    /**
     * Shows the data in the UI
     *
     * @param data        the data
     * @param displayName the display name
     * @param workbench   the workbench that issued the command
     * @param source      optional source of the data. Can by null or any kind of object (e.g. {@link JIPipeDataSlot})
     */
    void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source);


    /**
     * Applies the operation on a data table
     * @param dataTable the data table
     * @param modelRow the model row
     * @param workbench the workbench
     * @param saveAsDefault saves the operation as new default
     */
    default void display(JIPipeDataTable dataTable, int modelRow, JIPipeWorkbench workbench, boolean saveAsDefault) {
        JIPipeData data = dataTable.getData(modelRow, JIPipeData.class, new JIPipeProgressInfo());
        String displayName;
        String nodeName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_NODE_NAME, "");
        String slotName = dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
        if (!StringUtils.isNullOrEmpty(nodeName))
            displayName = nodeName + "/" + slotName + "/" + modelRow;
        else
            displayName = slotName + "/" + modelRow;
        display(data, displayName, workbench, new JIPipeDataTableDataSource(dataTable, modelRow));
        if (saveAsDefault && GeneralDataSettings.getInstance().isAutoSaveLastDisplay()) {
            String dataTypeId = JIPipe.getDataTypes().getIdOf(dataTable.getAcceptedDataType());
            DynamicDataDisplayOperationIdEnumParameter parameter = DefaultCacheDisplaySettings.getInstance().getValue(dataTypeId, DynamicDataDisplayOperationIdEnumParameter.class);
            if (parameter != null && !Objects.equals(getId(), parameter.getValue())) {
                parameter.setValue(getId());
                DefaultResultImporterSettings.getInstance().setValue(dataTypeId, parameter);
                JIPipe.getSettings().save();
            }
        }
    }
}
