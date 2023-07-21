package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

public class GraphNodeSlotValidationReportContext extends GraphNodeValidationReportContext {

    private final String slotName;
    private final JIPipeSlotType slotType;

    public GraphNodeSlotValidationReportContext(JIPipeGraphNode graphNode, String slotName, JIPipeSlotType slotType) {
        super(graphNode);
        this.slotName = slotName;
        this.slotType = slotType;
    }

    public GraphNodeSlotValidationReportContext(JIPipeValidationReportContext parent, JIPipeGraphNode graphNode, String slotName, JIPipeSlotType slotType) {
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
