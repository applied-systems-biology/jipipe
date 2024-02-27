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

package org.hkijena.jipipe.extensions.parameters.api.enums;

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.utils.ReflectionUtils;

/**
 * Helper class to register {@link Enum} parameters
 */
public class EnumParameterTypeInfo implements JIPipeParameterTypeInfo {

    private final String id;
    private final Class<? extends Enum<?>> fieldClass;
    private final String name;
    private final String description;

    /**
     * @param id          the id
     * @param fieldClass  the field class
     * @param name        the name
     * @param description the description
     */
    public EnumParameterTypeInfo(String id, Class<? extends Enum<?>> fieldClass, String name, String description) {
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
