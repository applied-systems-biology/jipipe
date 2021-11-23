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

package org.hkijena.jipipe.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Priority;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable implementation of {@link JIPipeParameterAccess}
 */
@JsonDeserialize(using = JIPipeMutableParameterAccess.Deserializer.class)
public class JIPipeMutableParameterAccess implements JIPipeParameterAccess {
    private JIPipeParameterCollection parameterHolder;
    private String holderName;
    private String holderDescription;
    private String key;
    private String name;
    private String description;
    private boolean hidden;
    private boolean important;
    private Class<?> fieldClass;
    private Object value;
    private double priority = Priority.NORMAL;
    private Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private String shortKey;
    private int uiOrder;
    private JIPipeParameterPersistence persistence = JIPipeParameterPersistence.Collection;

    /**
     * Creates a new instance
     */
    public JIPipeMutableParameterAccess() {
    }

    /**
     * Creates a new independent parameter from the provided access
     *
     * @param other the parameter
     */
    public JIPipeMutableParameterAccess(JIPipeParameterAccess other) {
        this.parameterHolder = other.getSource();
        this.key = other.getKey();
        this.name = other.getName();
        this.description = other.getDescription();
        this.hidden = other.isHidden();
        this.fieldClass = other.getFieldClass();
        this.persistence = other.getPersistence();
        this.uiOrder = other.getUIOrder();
        this.priority = other.getPriority();
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(fieldClass);
        this.value = info.duplicate(other.get(fieldClass)); // Deep copy
    }

    /**
     * Creates a new instance
     *
     * @param parameterHolder The object that holds the parameter
     * @param key             Unique parameter key
     * @param fieldClass      Parameter field type
     */
    public JIPipeMutableParameterAccess(JIPipeParameterCollection parameterHolder, String key, Class<?> fieldClass) {
        this.parameterHolder = parameterHolder;
        this.key = key;
        this.fieldClass = fieldClass;
    }

    /**
     * Copies the parameter access
     *
     * @param other The original
     */
    public JIPipeMutableParameterAccess(JIPipeMutableParameterAccess other) {
        this.parameterHolder = other.parameterHolder;
        this.holderName = other.holderName;
        this.holderDescription = other.holderDescription;
        this.key = other.key;
        this.name = other.name;
        this.description = other.description;
        this.hidden = other.hidden;
        this.fieldClass = other.fieldClass;
        this.value = other.value;
        this.priority = other.priority;
        this.persistence = other.persistence;
    }

    @Override
    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
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

    @JsonGetter("hidden")
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @JsonSetter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return (T) annotationMap.getOrDefault(klass, null);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return annotationMap.values();
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
    public String getFieldClassInfoId() {
        return JIPipe.getParameterTypes().getInfoByFieldClass(getFieldClass()).getId();
    }

    @JsonSetter("field-class-id")
    public void setFieldClassInfoId(String id) {
        setFieldClass(JIPipe.getParameterTypes().getInfoById(id).getFieldClass());
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
            parameterHolder.getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(parameterHolder, key));
        return true;
    }

    @Override
    public JIPipeParameterCollection getSource() {
        return parameterHolder;
    }

    /**
     * For internal usage only
     *
     * @param parameterHolder The object that holds the parameter
     */
    public void setParameterHolder(JIPipeParameterCollection parameterHolder) {
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

    @Override
    public JIPipeParameterPersistence getPersistence() {
        return persistence;
    }

    public void setPersistence(JIPipeParameterPersistence persistence) {
        this.persistence = persistence;
    }

    /**
     * Deserializes {@link JIPipeMutableParameterAccess}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeMutableParameterAccess> {
        @Override
        public JIPipeMutableParameterAccess deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode jsonNode = p.readValueAsTree();
            JIPipeMutableParameterAccess result = new JIPipeMutableParameterAccess();
            result.setName(jsonNode.get("name").textValue());
            result.setDescription(jsonNode.get("description").textValue());
            result.setFieldClassInfoId(jsonNode.get("field-class-id").textValue());
            if (jsonNode.has("hidden"))
                result.setHidden(jsonNode.get("hidden").asBoolean());
            try {
                if (jsonNode.has("value"))
                    result.set(JsonUtils.getObjectMapper().readerFor(result.getFieldClass()).readValue(jsonNode.get("value")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (jsonNode.has("short-key"))
                result.setShortKey(jsonNode.get("short-key").textValue());
            if (jsonNode.has("ui-order"))
                result.setUIOrder(jsonNode.get("ui-order").intValue());
            return result;
        }
    }
}
