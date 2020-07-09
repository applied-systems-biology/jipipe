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
import org.hkijena.pipelinej.utils.ReflectionUtils;

/**
 * Helper class to register {@link Enum} parameters
 */
public class EnumParameterTypeDeclaration implements ACAQParameterTypeDeclaration {

    private String id;
    private Class<? extends Enum<?>> fieldClass;
    private String name;
    private String description;

    /**
     * @param id
     * @param fieldClass
     * @param name
     * @param description
     */
    public EnumParameterTypeDeclaration(String id, Class<? extends Enum<?>> fieldClass, String name, String description) {
        this.id = id;
        this.fieldClass = fieldClass;
        this.name = name;
        this.description = description;
    }

    @Override
    public Object newInstance() {
        try {
            return ReflectionUtils.getEnumValues(fieldClass)[0];
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
