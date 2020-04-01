package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A parameter that is a collection of another parameter type
 */
@JsonSerialize(using = CollectionParameter.Serializer.class)
public abstract class CollectionParameter<T> extends ArrayList<T> {
    private Class<T> contentClass;

    /**
     * @param contentClass the stored content
     */
    public CollectionParameter(Class<T> contentClass) {
        this.contentClass = contentClass;
    }

    public Class<T> getContentClass() {
        return contentClass;
    }

    /**
     * Adds a new instance of the content class
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

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<CollectionParameter<?>> {
        @Override
        public void serialize(CollectionParameter<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
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
    public abstract static class Deserializer<T> extends JsonDeserializer<CollectionParameter<T>> {

        public abstract Class<T> getContentClass();

        /**
         * @return New instance of the collection
         */
        public abstract CollectionParameter<T> newInstance();

        @Override
        public CollectionParameter<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();
            JavaType listType = JsonUtils.getObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, getContentClass());
            ArrayList<T> entries = JsonUtils.getObjectMapper().readerFor(listType).readValue(root);

            CollectionParameter<T> collectionParameter = newInstance();
            collectionParameter.addAll(entries);

            return collectionParameter;
        }
    }
}
