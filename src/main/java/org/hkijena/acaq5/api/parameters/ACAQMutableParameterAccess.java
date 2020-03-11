package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.lang.annotation.Annotation;

public class ACAQMutableParameterAccess implements ACAQParameterAccess {
    private Object parameterHolder;
    private String key;
    private String name;
    private String description;
    private ACAQParameterVisibility visibility;
    private Class<?> fieldClass;
    private Object value;

    public ACAQMutableParameterAccess(Object parameterHolder, String key, Class<?> fieldClass) {
        this.parameterHolder = parameterHolder;
        this.key = key;
        this.fieldClass = fieldClass;
    }

    @Override
    public String getKey() {
        return key;
    }

    /**
     * For internal use only
     *
     * @param key
     */
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @JsonGetter("visibility")
    public ACAQParameterVisibility getVisibility() {
        return visibility;
    }

    @JsonSetter("visibility")
    public void setVisibility(ACAQParameterVisibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return null;
    }

    @Override
    @JsonGetter("field-class")
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    /**
     * For internal usage only
     *
     * @param fieldClass
     */
    @JsonSetter("field-class")
    public void setFieldClass(Class<?> fieldClass) {
        this.fieldClass = fieldClass;
    }

    @Override
    @JsonGetter("value")
    public <T> T get() {
        return (T) value;
    }

    @Override
    @JsonSetter("value")
    public <T> boolean set(T value) {
        this.value = value;
        return true;
    }

    @Override
    public Object getParameterHolder() {
        return parameterHolder;
    }

    /**
     * For internal usage only
     *
     * @param parameterHolder
     */
    public void setParameterHolder(Object parameterHolder) {
        this.parameterHolder = parameterHolder;
    }


}
