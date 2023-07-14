package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;

public class GraphNodeSlotValidationReportEntryCause extends GraphNodeValidationReportEntryCause {

    private final String slotName;
    private final JIPipeSlotType slotType;

    public GraphNodeSlotValidationReportEntryCause(JIPipeGraphNode graphNode, String slotName, JIPipeSlotType slotType) {
        super(graphNode);
        this.slotName = slotName;
        this.slotType = slotType;
    }

    public GraphNodeSlotValidationReportEntryCause(JIPipeValidationReportEntryCause parent, JIPipeGraphNode graphNode, String slotName, JIPipeSlotType slotType) {
        super(parent, graphNode);
        this.slotName = slotName;
        this.slotType = slotType;
    }

    public String getSlotName() {
        return slotName;
    }

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    @Override
    public String renderName() {
        return super.renderName() + "/" + slotType + "/" + slotName;
    }
}
