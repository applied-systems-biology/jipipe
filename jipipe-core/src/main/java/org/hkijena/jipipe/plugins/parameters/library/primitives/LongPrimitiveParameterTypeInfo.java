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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;

/**
 * Info for {@link Long}
 */
public class LongPrimitiveParameterTypeInfo implements JIPipeParameterTypeInfo {
    @Override
    public Object newInstance() {
        return 0L;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.long";
    }

    @Override
    public Class<?> getFieldClass() {
        return long.class;
    }

    @Override
    public String getName() {
        return "Long";
    }

    @Override
    public String getDescription() {
        return "An integral number (maximum allowed range is " + Long.MIN_VALUE + " to " + Long.MAX_VALUE + ")";
    }
}
