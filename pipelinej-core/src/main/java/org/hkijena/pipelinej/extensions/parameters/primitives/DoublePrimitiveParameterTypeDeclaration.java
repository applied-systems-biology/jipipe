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

package org.hkijena.pipelinej.extensions.parameters.primitives;

import org.hkijena.pipelinej.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Double}
 */
public class DoublePrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return 0.0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.double";
    }

    @Override
    public Class<?> getFieldClass() {
        return double.class;
    }

    @Override
    public String getName() {
        return "Double";
    }

    @Override
    public String getDescription() {
        return "A 64-bit floating point number";
    }
}
