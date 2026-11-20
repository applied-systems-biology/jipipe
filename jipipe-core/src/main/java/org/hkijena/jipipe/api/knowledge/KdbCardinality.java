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

package org.hkijena.jipipe.api.knowledge;

/**
 * How many values a KDB slot can carry (design spec 5.1).
 */
public enum KdbCardinality {

    /**
     * Exactly one value. A list node for a SINGLE slot is a cardinality violation.
     */
    SINGLE,

    /**
     * Any number of values, including zero. A bare scalar counts as one value.
     */
    MULTI
}
