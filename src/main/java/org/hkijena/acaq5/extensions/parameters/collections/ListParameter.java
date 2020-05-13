package org.hkijena.acaq5.extensions.parameters.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A parameter that is a collection of another parameter type
 */
@JsonSerialize(using = ListParameter.Serializer.class)
@JsonDeserialize(using = ListParameter.Deserializer.class)
public abstract class ListParameter<T> extends ArrayList<T> implements ACAQValidatable {
    private Class<T> contentClass;

    /**
     * @param contentClass the stored content
     */
    public ListParameter(Class<T> contentClass) {
        this.contentClass = contentClass;
    }

    public Class<T> getContentClass() {
        return contentClass;
    }

    /**
     * Adds a new instance of the content class
     *
     * @return the instance
     */
    public T addNewInstance() {
        try {
            T instance = getContentClass().newInstance();
            add(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (ACAQValidatable.class.isAssignableFrom(contentClass)) {
            for (int i = 0; i < size(); i++) {
                ACAQValidatable validatable = (ACAQValidatable) get(i);
                report.forCategory("Item #" + (i + 1)).report(validatable);
            }
        }
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<ListParameter<?>> {
        @Override
        public void serialize(ListParameter<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartArray();
            for (Object object : objects) {
                jsonGenerator.writeObject(object);
            }
            jsonGenerator.writeEndArray();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<T> extends JsonDeserializer<ListParameter<T>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public ListParameter<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();

            ListParameter<T> listParameter;
            try {
                listParameter = (ListParameter<T>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(listParameter.getContentClass());
            for (JsonNode element : ImmutableList.copyOf(root.elements())) {
                listParameter.add(objectReader.readValue(element));
            }

            return listParameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            ListParameter.Deserializer<?> deserializer = new Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
