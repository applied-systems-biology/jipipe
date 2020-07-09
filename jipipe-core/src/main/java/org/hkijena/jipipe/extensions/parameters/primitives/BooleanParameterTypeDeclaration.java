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

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeDeclaration;

/**
 * Declaration for {@link Boolean}
 */
public class BooleanParameterTypeDeclaration implements JIPipeParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return false;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "boolean";
    }

    @Override
    public Class<?> getFieldClass() {
        return Boolean.class;
    }

    @Override
    public String getName() {
        return "Boolean";
    }

    @Override
    public String getDescription() {
        return "A value that can be true or false";
    }
}
