package org.hkijena.acaq5.extensions.parameters.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Parameter equivalent of {@link java.util.Map.Entry}
 */
@JsonSerialize(using = KeyValuePairParameter.Serializer.class)
@JsonDeserialize(using = KeyValuePairParameter.Deserializer.class)
public abstract class KeyValuePairParameter<K, V> implements ACAQValidatable, Map.Entry<K, V> {

    private Class<K> keyClass;
    private Class<V> valueClass;
    private K key;
    private V value;

    /**
     * @param keyClass   the key class
     * @param valueClass the stored content
     */
    public KeyValuePairParameter(Class<K> keyClass, Class<V> valueClass) {
        this.keyClass = keyClass;
        this.valueClass = valueClass;
    }

    /**
     * Creates a copy.
     * No deep-copy is applied!
     *
     * @param other the original
     */
    public KeyValuePairParameter(KeyValuePairParameter<K, V> other) {
        this.keyClass = other.keyClass;
        this.valueClass = other.valueClass;
        this.key = other.key;
        this.value = other.value;
    }

    public Class<V> getValueClass() {
        return valueClass;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (key instanceof ACAQValidatable)
            report.forCategory("Key").report((ACAQValidatable) key);
        if (value instanceof ACAQValidatable)
            report.forCategory("Value").report((ACAQValidatable) value);
    }

    @Override
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        this.value = value;
        return value;
    }

    public Class<K> getKeyClass() {
        return keyClass;
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<KeyValuePairParameter<?, ?>> {
        @Override
        public void serialize(KeyValuePairParameter<?, ?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("key", objects.key);
            jsonGenerator.writeObjectField("value", objects.value);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<K, V> extends JsonDeserializer<KeyValuePairParameter<?, ?>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public KeyValuePairParameter<K, V> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();
            KeyValuePairParameter<K, V> keyValuePairParameter;
            try {
                keyValuePairParameter = (KeyValuePairParameter<K, V>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            keyValuePairParameter.key = JsonUtils.getObjectMapper().readerFor(keyValuePairParameter.getKeyClass()).readValue(root.get("key"));
            keyValuePairParameter.value = JsonUtils.getObjectMapper().readerFor(keyValuePairParameter.getKeyClass()).readValue(root.get("value"));

            return keyValuePairParameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            KeyValuePairParameter.Deserializer<K, V> deserializer = new KeyValuePairParameter.Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
