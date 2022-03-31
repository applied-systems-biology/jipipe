package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class JIPipeInputDataSlot extends JIPipeDataSlot {
    public JIPipeInputDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        super(info, node);
        if(info.getSlotType() != JIPipeSlotType.Input) {
            throw new IllegalArgumentException("Data slot info describes an input slot.");
        }
    }

    public JIPipeInputDataSlot(JIPipeDataSlot other, boolean shallow, JIPipeProgressInfo progressInfo) {
        super(other, shallow, progressInfo);
    }
}
