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

package org.hkijena.pipelinej.api.history;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.data.ACAQSlotConfiguration;

public class SlotConfigurationHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {
    private final ACAQGraphNode node;
    private final ACAQSlotConfiguration slotConfiguration;
    private final String name;
    private ACAQSlotConfiguration afterSlotConfiguration;

    public SlotConfigurationHistorySnapshot(ACAQGraphNode node, String name) {
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

    public ACAQSlotConfiguration getAfterSlotConfiguration() {
        return afterSlotConfiguration;
    }

    public void setAfterSlotConfiguration(ACAQSlotConfiguration afterSlotConfiguration) {
        this.afterSlotConfiguration = afterSlotConfiguration;
    }
}
