package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;
import org.hkijena.acaq5.utils.ReflectionUtils;

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
