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
