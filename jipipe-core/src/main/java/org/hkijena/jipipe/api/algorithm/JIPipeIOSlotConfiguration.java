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

package org.hkijena.jipipe.api.algorithm;

import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;

/**
 * Slot configuration that always ensures 1:1 relation between input and output slots
 */
public class JIPipeIOSlotConfiguration extends JIPipeDefaultMutableSlotConfiguration {
    /**
     * Creates a new instance
     */
    public JIPipeIOSlotConfiguration() {
    }

    @Override
    public JIPipeDataSlotInfo addSlot(String name, JIPipeDataSlotInfo definition, boolean user) {
        JIPipeDataSlotInfo newSlot = super.addSlot(name, definition, user);
        if (newSlot.isInput()) {
            JIPipeDataSlotInfo sisterSlot = new JIPipeDataSlotInfo(definition.getDataClass(), JIPipeSlotType.Output, null);
            super.addSlot(name, sisterSlot, user);
        } else if (newSlot.isOutput()) {
            JIPipeDataSlotInfo sisterSlot = new JIPipeDataSlotInfo(definition.getDataClass(), JIPipeSlotType.Input, null);
            super.addSlot(name, sisterSlot, user);
        }
        return newSlot;
    }

    @Override
    public void removeInputSlot(String name, boolean user) {
        super.removeInputSlot(name, user);
        super.removeOutputSlot(name, user);
    }

    @Override
    public void removeOutputSlot(String name, boolean user) {
        super.removeOutputSlot(name, user);
        super.removeInputSlot(name, user);
    }
}
