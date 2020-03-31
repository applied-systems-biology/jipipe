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
@JsonDeserialize(using = CollectionParameter.Deserializer.class)
public class CollectionParameter<T> extends ArrayList<T> {
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
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<CollectionParameter<?>> {
        @Override
        public void serialize(CollectionParameter<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("content-class", objects.contentClass);
            jsonGenerator.writeObjectField("entries", new ArrayList<>(objects));
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer extends JsonDeserializer<CollectionParameter<?>> {
        @Override
        public CollectionParameter<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();
            Class<?> contentClass = JsonUtils.getObjectMapper().readerFor(Class.class).readValue(root.get("content-class"));
            JavaType listType = JsonUtils.getObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, contentClass);
            ArrayList<?> entries = JsonUtils.getObjectMapper().readerFor(listType).readValue(root.get("entries"));

            CollectionParameter collectionParameter = new CollectionParameter<>(contentClass);
            collectionParameter.addAll(entries);

            return collectionParameter;
        }
    }
}
