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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;

/**
 * Info for {@link Float}
 */
public class FloatPrimitiveParameterTypeInfo implements JIPipeParameterTypeInfo {
    @Override
    public Object newInstance() {
        return 0.0f;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.float";
    }

    @Override
    public Class<?> getFieldClass() {
        return float.class;
    }

    @Override
    public String getName() {
        return "Float";
    }

    @Override
    public String getDescription() {
        return "A 32-bit floating point number";
    }
}
