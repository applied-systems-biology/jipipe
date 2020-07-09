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
 * Declaration for {@link Byte}
 */
public class ByteParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return (byte) 0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "byte";
    }

    @Override
    public Class<?> getFieldClass() {
        return Byte.class;
    }

    @Override
    public String getName() {
        return "Byte";
    }

    @Override
    public String getDescription() {
        return "An integral number ranging from " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE;
    }
}
