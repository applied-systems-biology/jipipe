package org.hkijena.jipipe.api.data;

/**
 * Assigns a role to the data table attached to the data slot.
 */
public enum JIPipeDataSlotRole {
    /**
     * The data table contains data that need to be iterated/merged/processed.
     * This is the default option
     */
    Data,
    /**
     * The data table contains a parametric data, i.e., they might not be considered for data batch generation.
     * Depending on the node, slots with this role are handled independently of the data role slots.
     */
    Parameters
}
