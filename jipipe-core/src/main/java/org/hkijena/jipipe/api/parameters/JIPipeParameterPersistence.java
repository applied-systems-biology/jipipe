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

package org.hkijena.jipipe.api.parameters;

/**
 * Determines how a parameter is serialized
 */
public enum JIPipeParameterPersistence {
    /**
     * The parameter is serialized as collection, meaning that sub-parameters are
     * exploded. This is equal to the beahvior of 'Object' for non {@link JIPipeParameterCollection} objects.
     */
    Collection,
    /**
     * The parameter is serialized as-is.
     */
    Object,
    /**
     * The parameter is not serialized
     */
    None
}
