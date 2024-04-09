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

package org.hkijena.jipipe.plugins.parameters.api.optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * A parameter that is optional
 *
 * @param <T> the underlying parameter
 */
@JsonSerialize(using = OptionalParameter.Serializer.class)
@JsonDeserialize(using = OptionalParameter.Deserializer.class)
public abstract class OptionalParameter<T> implements JIPipeCustomTextDescriptionParameter {
    private final Class<T> contentClass;
    private boolean enabled = false;
    private T content;

    /**
     * @param contentClass the stored content
     */
    public OptionalParameter(Class<T> contentClass) {
        this.contentClass = contentClass;
        setNewInstance();
    }

    /**
     * Produces a shallow copy
     * You have to implement deep content copying yourself
     *
     * @param other the original
     */
    public OptionalParameter(OptionalParameter<T> other) {
        this.contentClass = other.contentClass;
        this.enabled = other.enabled;
        this.content = other.content;
    }

    /**
     * Adds a new instance of the content class
     * Override this method for types that cannot be default-constructed
     *
     * @return the instance
     */
    public T setNewInstance() {
        try {
            T instance = getContentClass().newInstance();
            setContent(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<T> getContentClass() {
        return contentClass;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String getTextDescription() {
        return enabled ? JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(content) : "[Disabled]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionalParameter<?> that = (OptionalParameter<?>) o;
        return enabled == that.enabled && Objects.equals(contentClass, that.contentClass) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentClass, enabled, content);
    }

    /**
     * Returns the content if enabled, otherwise the default value
     *
     * @param defaultValue the default value
     * @return the content if enabled, otherwise the default value
     */
    public T getContentOrDefault(T defaultValue) {
        return isEnabled() ? getContent() : defaultValue;
    }

    @Override
    public String toString() {
        if (enabled)
            return "" + getContent();
        else
            return "[Ignored]";
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<OptionalParameter<?>> {
        @Override
        public void serialize(OptionalParameter<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("content", objects.getContent());
            jsonGenerator.writeBooleanField("enabled", objects.isEnabled());
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<T> extends JsonDeserializer<OptionalParameter<T>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public OptionalParameter<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();

            OptionalParameter<T> parameter;
            try {
                parameter = (OptionalParameter<T>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(parameter.getContentClass());

            if (root.isObject() && root.has("enabled")) {
                parameter.setEnabled(root.get("enabled").booleanValue());
                parameter.setContent(objectReader.readValue(root.get("content")));
            } else {
                // Fallback for conversion from content to optional parameter
                parameter.setContent(objectReader.readValue(root));
            }

            return parameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Deserializer<?> deserializer = new Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
