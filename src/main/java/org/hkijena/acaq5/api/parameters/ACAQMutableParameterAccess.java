package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;

import java.lang.annotation.Annotation;

/**
 * A mutable implementation of {@link ACAQParameterAccess}
 */
public class ACAQMutableParameterAccess implements ACAQParameterAccess {
    private ACAQParameterHolder parameterHolder;
    private String holderName;
    private String holderDescription;
    private String key;
    private String name;
    private String description;
    private ACAQParameterVisibility visibility = ACAQParameterVisibility.TransitiveVisible;
    private Class<?> fieldClass;
    private Object value;

    /**
     * Creates a new instance
     */
    public ACAQMutableParameterAccess() {
    }

    /**
     * Creates a new instance
     *
     * @param parameterHolder The object that holds the parameter
     * @param key             Unique parameter key
     * @param fieldClass      Parameter field type
     */
    public ACAQMutableParameterAccess(ACAQParameterHolder parameterHolder, String key, Class<?> fieldClass) {
        this.parameterHolder = parameterHolder;
        this.key = key;
        this.fieldClass = fieldClass;
    }

    /**
     * Copies the parameter access
     *
     * @param other The original
     */
    public ACAQMutableParameterAccess(ACAQMutableParameterAccess other) {
        this.parameterHolder = other.parameterHolder;
        this.holderName = other.holderName;
        this.holderDescription = other.holderDescription;
        this.key = other.key;
        this.name = other.name;
        this.description = other.description;
        this.visibility = other.visibility;
        this.fieldClass = other.fieldClass;
        this.value = other.value;
    }

    @Override
    public String getKey() {
        return key;
    }

    /**
     * For internal use only
     *
     * @param key key of this access
     */
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name The name
     */
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description The description
     */
    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @JsonGetter("visibility")
    public ACAQParameterVisibility getVisibility() {
        if (visibility == null)
            return ACAQParameterVisibility.TransitiveVisible;
        return visibility;
    }

    /**
     * Sets the visibility
     *
     * @param visibility The visibilities
     */
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
     * @param fieldClass The parameter class
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

        // Trigger change in parent parameter holder
        if (parameterHolder != null)
            parameterHolder.getEventBus().post(new ParameterChangedEvent(this, getKey()));
        return true;
    }

    @Override
    public ACAQParameterHolder getParameterHolder() {
        return parameterHolder;
    }

    /**
     * For internal usage only
     *
     * @param parameterHolder The object that holds the parameter
     */
    public void setParameterHolder(ACAQParameterHolder parameterHolder) {
        this.parameterHolder = parameterHolder;
    }


    @Override
    public String getHolderDescription() {
        return holderDescription;
    }

    /**
     * Sets a description that describes the parameter holder
     *
     * @param holderDescription Parameter holder description
     */
    public void setHolderDescription(String holderDescription) {
        this.holderDescription = holderDescription;
    }

    @Override
    public String getHolderName() {
        return holderName;
    }

    /**
     * Sets a name for the parameter holder
     *
     * @param holderName Parameter holder name
     */
    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }
}
