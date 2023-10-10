package org.hkijena.jipipe.api.nodes.databatch;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;

import java.util.Map;
import java.util.Set;

public interface JIPipeDataBatch {
    /**
     * Returns the referenced indices of given input slot name
     * @param slotName the slot name
     * @return the indices
     */
    Set<Integer> getInputSlotRowIndices(String slotName);

    /**
     * Gets the names of all input slots that are referenced
     * @return the names of the input slots
     */
    Set<String> getInputSlotNames();

    /**
     * Gets the input slots that are referenced
     * @return the input slots
     */
    Set<JIPipeDataSlot> getInputSlots();
}
