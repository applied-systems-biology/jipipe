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

package org.hkijena.jipipe.extensions.parameters.primitives;

import java.util.List;
import java.util.function.Supplier;

/**
 * Settings for dynamic enum-like parameters for non {@link Enum} data types
 */
public @interface DynamicSetParameterSettings {
    /**
     * Supplies the enum items. Class must have a standard constructor
     *
     * @return the enum items
     */
    Class<? extends Supplier<List<Object>>> supplier();
}
