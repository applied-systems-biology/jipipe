package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class JIPipeOutputDataSlot extends JIPipeDataSlot {
    public JIPipeOutputDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        super(info, node);
        if(info.getSlotType() != JIPipeSlotType.Output) {
            throw new IllegalArgumentException("Data slot info describes an output slot.");
        }
    }

    public JIPipeOutputDataSlot(JIPipeDataSlot other, boolean shallow, JIPipeProgressInfo progressInfo) {
        super(other, shallow, progressInfo);
    }
}
