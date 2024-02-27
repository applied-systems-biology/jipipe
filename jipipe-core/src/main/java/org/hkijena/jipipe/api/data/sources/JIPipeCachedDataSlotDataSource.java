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

package org.hkijena.jipipe.api.data.sources;

import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.UUID;

public class JIPipeCachedDataSlotDataSource extends JIPipeDataTableDataSource {

    private final UUID nodeUUID;
    private final String slotName;

    public JIPipeCachedDataSlotDataSource(JIPipeDataTable dataTable, int row, UUID nodeUUID, String slotName) {
        super(dataTable, row);
        this.nodeUUID = nodeUUID;
        this.slotName = slotName;
    }

    public JIPipeCachedDataSlotDataSource(JIPipeDataTable dataTable, int row, String dataAnnotation, UUID nodeUUID, String slotName) {
        super(dataTable, row, dataAnnotation);
        this.nodeUUID = nodeUUID;
        this.slotName = slotName;
    }

    public UUID getNodeUUID() {
        return nodeUUID;
    }

    public String getSlotName() {
        return slotName;
    }
}
