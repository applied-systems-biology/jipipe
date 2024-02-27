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
