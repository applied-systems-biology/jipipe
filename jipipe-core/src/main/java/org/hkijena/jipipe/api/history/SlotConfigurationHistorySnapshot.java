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

package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;

public class SlotConfigurationHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {
    private final JIPipeGraphNode node;
    private final JIPipeSlotConfiguration slotConfiguration;
    private final String name;
    private JIPipeSlotConfiguration afterSlotConfiguration;

    public SlotConfigurationHistorySnapshot(JIPipeGraphNode node, String name) {
        this.node = node;
        this.slotConfiguration = node.getSlotConfiguration().duplicate();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void undo() {
        afterSlotConfiguration = node.getSlotConfiguration().duplicate();
        node.getSlotConfiguration().setTo(slotConfiguration);
    }

    @Override
    public void redo() {
        if (afterSlotConfiguration != null) {
            node.getSlotConfiguration().setTo(afterSlotConfiguration);
        }
    }

    public JIPipeSlotConfiguration getAfterSlotConfiguration() {
        return afterSlotConfiguration;
    }

    public void setAfterSlotConfiguration(JIPipeSlotConfiguration afterSlotConfiguration) {
        this.afterSlotConfiguration = afterSlotConfiguration;
    }
}
