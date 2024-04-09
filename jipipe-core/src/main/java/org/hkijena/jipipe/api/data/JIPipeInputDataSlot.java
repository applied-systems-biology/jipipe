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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class JIPipeInputDataSlot extends JIPipeDataSlot {

    private boolean skipDataGathering;

    public JIPipeInputDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        super(info, node);
        if (info.getSlotType() != JIPipeSlotType.Input) {
            throw new IllegalArgumentException("Data slot info describes an input slot.");
        }
    }

    public JIPipeInputDataSlot(JIPipeDataSlot other, boolean shallow, JIPipeProgressInfo progressInfo) {
        super(other, shallow, progressInfo);
    }


    public boolean isSkipDataGathering() {
        return skipDataGathering;
    }

    public void setSkipDataGathering(boolean skipDataGathering) {
        this.skipDataGathering = skipDataGathering;
    }
}
