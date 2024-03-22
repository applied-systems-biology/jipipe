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

package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;

import java.util.Set;

public interface JIPipeIterationStep {
    /**
     * Returns the referenced indices of given input slot name
     *
     * @param slotName the slot name
     * @return the indices
     */
    Set<Integer> getInputSlotRowIndices(String slotName);

    /**
     * Gets the names of all input slots that are referenced
     *
     * @return the names of the input slots
     */
    Set<String> getInputSlotNames();

    /**
     * Gets the input slots that are referenced
     *
     * @return the input slots
     */
    Set<JIPipeDataSlot> getInputSlots();
}
