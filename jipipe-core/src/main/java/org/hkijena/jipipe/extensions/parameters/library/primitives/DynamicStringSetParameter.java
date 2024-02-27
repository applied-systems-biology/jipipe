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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicEnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Set;

/**
 * Parameter that acts as dynamic enum where a set of items can be selected
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items. Alternatively, use allowedValues to supply items.
 * allowedValues is preferred. If allowedValues is null, you have to use {@link DynamicEnumParameterSettings}.
 */
public class DynamicStringSetParameter extends DynamicSetParameter<String> {
    public DynamicStringSetParameter() {
    }

    public DynamicStringSetParameter(DynamicStringSetParameter other) {
        super(other);
    }

    public DynamicStringSetParameter(Set<String> values) {
        super(values);
    }
}
