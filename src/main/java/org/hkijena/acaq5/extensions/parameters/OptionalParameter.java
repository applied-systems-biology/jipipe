package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;

/**
 * A parameter that is optional
 *
 * @param <T> the underlying parameter
 */
@JsonSerialize(using = OptionalParameter.Serializer.class)
@JsonDeserialize(using = OptionalParameter.Deserializer.class)
public abstract class OptionalParameter<T> {
    private Class<T> contentClass;
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

            parameter.setEnabled(root.get("enabled").booleanValue());
            parameter.setContent(objectReader.readValue(root.get("content")));

            return parameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            OptionalParameter.Deserializer<?> deserializer = new OptionalParameter.Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
