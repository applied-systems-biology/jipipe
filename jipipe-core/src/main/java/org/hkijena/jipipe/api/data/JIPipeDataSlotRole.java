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
