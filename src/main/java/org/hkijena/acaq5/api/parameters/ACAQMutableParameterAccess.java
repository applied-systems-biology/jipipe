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

package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.scijava.Priority;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable implementation of {@link ACAQParameterAccess}
 */
@JsonDeserialize(using = ACAQMutableParameterAccess.Deserializer.class)
public class ACAQMutableParameterAccess implements ACAQParameterAccess {
    private ACAQParameterCollection parameterHolder;
    private String holderName;
    private String holderDescription;
    private String key;
    private String name;
    private String description;
    private ACAQParameterVisibility visibility = ACAQParameterVisibility.TransitiveVisible;
    private Class<?> fieldClass;
    private Object value;
    private double priority = Priority.NORMAL;
    private Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private String shortKey;
    private int uiOrder;

    /**
     * Creates a new instance
     */
    public ACAQMutableParameterAccess() {
    }

    /**
     * Creates a new independent parameter from the provided access
     *
     * @param other the parameter
     */
    public ACAQMutableParameterAccess(ACAQParameterAccess other) {
        this.parameterHolder = other.getSource();
        this.key = other.getKey();
        this.name = other.getName();
        this.description = other.getDescription();
        this.visibility = other.getVisibility();
        this.fieldClass = other.getFieldClass();
        ACAQParameterTypeDeclaration declaration = ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(fieldClass);
        this.value = declaration.duplicate(other.get(fieldClass)); // Deep copy
    }

    /**
     * Creates a new instance
     *
     * @param parameterHolder The object that holds the parameter
     * @param key             Unique parameter key
     * @param fieldClass      Parameter field type
     */
    public ACAQMutableParameterAccess(ACAQParameterCollection parameterHolder, String key, Class<?> fieldClass) {
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
        this.priority = other.priority;
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
        return (T) annotationMap.getOrDefault(klass, null);
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
    public void setFieldClass(Class<?> fieldClass) {
        this.fieldClass = fieldClass;
    }

    @JsonGetter("field-class-id")
    public String getFieldClassDeclarationId() {
        return ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(getFieldClass()).getId();
    }

    @JsonSetter("field-class-id")
    public void setFieldClassDeclarationId(String id) {
        setFieldClass(ACAQParameterTypeRegistry.getInstance().getDeclarationById(id).getFieldClass());
    }

    @JsonGetter("value")
    public Object getValue() {
        return value;
    }

    @Override

    public <T> T get(Class<T> klass) {
        return (T) value;
    }

    @Override
    @JsonSetter("value")
    public <T> boolean set(T value) {

        // Ignore non-changes
        if (this.value != value && Objects.equals(value, this.value))
            return true;

        this.value = value;

        // Trigger change in parent parameter holder
        if (parameterHolder != null)
            parameterHolder.getEventBus().post(new ParameterChangedEvent(parameterHolder, key));
        return true;
    }

    @Override
    public ACAQParameterCollection getSource() {
        return parameterHolder;
    }

    /**
     * For internal usage only
     *
     * @param parameterHolder The object that holds the parameter
     */
    public void setParameterHolder(ACAQParameterCollection parameterHolder) {
        this.parameterHolder = parameterHolder;
    }


    @Override
    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    /**
     * Mutable access to the annotations
     *
     * @return access to the annotations
     */
    public Map<Class<? extends Annotation>, Annotation> getAnnotationMap() {
        return annotationMap;
    }

    public void setAnnotationMap(Map<Class<? extends Annotation>, Annotation> annotationMap) {
        this.annotationMap = annotationMap;
    }

    @JsonGetter("short-key")
    @Override
    public String getShortKey() {
        return !StringUtils.isNullOrEmpty(shortKey) ? shortKey : getKey();
    }

    @JsonSetter("short-key")
    public void setShortKey(String shortKey) {
        this.shortKey = shortKey;
    }

    @JsonGetter("ui-order")
    @Override
    public int getUIOrder() {
        return uiOrder;
    }

    @JsonSetter("ui-order")
    public void setUIOrder(int uiOrder) {
        this.uiOrder = uiOrder;
    }

    /**
     * Deserializes {@link ACAQMutableParameterAccess}
     */
    public static class Deserializer extends JsonDeserializer<ACAQMutableParameterAccess> {
        @Override
        public ACAQMutableParameterAccess deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode jsonNode = p.readValueAsTree();
            ACAQMutableParameterAccess result = new ACAQMutableParameterAccess();
            result.setName(jsonNode.get("name").textValue());
            result.setDescription(jsonNode.get("description").textValue());
            result.setVisibility(JsonUtils.getObjectMapper().readerFor(ACAQParameterVisibility.class).readValue(jsonNode.get("visibility")));
            result.setFieldClassDeclarationId(jsonNode.get("field-class-id").textValue());
            if (jsonNode.has("value"))
                result.set(JsonUtils.getObjectMapper().readerFor(result.getFieldClass()).readValue(jsonNode.get("value")));
            if (jsonNode.has("short-key"))
                result.setShortKey(jsonNode.get("short-key").textValue());
            if (jsonNode.has("ui-order"))
                result.setUIOrder(jsonNode.get("ui-order").intValue());
            return result;
        }
    }
}
