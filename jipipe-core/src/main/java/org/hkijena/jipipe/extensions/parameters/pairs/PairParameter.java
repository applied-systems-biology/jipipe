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

package org.hkijena.jipipe.extensions.parameters.pairs;

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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Parameter equivalent of {@link Map.Entry}
 */
@JsonSerialize(using = PairParameter.Serializer.class)
@JsonDeserialize(using = PairParameter.Deserializer.class)
public abstract class PairParameter<K, V> implements JIPipeValidatable, Map.Entry<K, V> {

    private Class<K> keyClass;
    private Class<V> valueClass;
    private K key;
    private V value;

    /**
     * @param keyClass   the key class
     * @param valueClass the stored content
     */
    public PairParameter(Class<K> keyClass, Class<V> valueClass) {
        this.keyClass = keyClass;
        this.valueClass = valueClass;
    }

    /**
     * Creates a copy.
     * No deep-copy is applied!
     *
     * @param other the original
     */
    public PairParameter(PairParameter<K, V> other) {
        this.keyClass = other.keyClass;
        this.valueClass = other.valueClass;
        this.key = other.key;
        this.value = other.value;
    }

    public Class<V> getValueClass() {
        return valueClass;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (key instanceof JIPipeValidatable)
            report.resolve("Key").report((JIPipeValidatable) key);
        if (value instanceof JIPipeValidatable)
            report.resolve("Value").report((JIPipeValidatable) value);
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

    @Override
    public String toString() {
        return getKey() + " -> " + getValue();
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<PairParameter<?, ?>> {
        @Override
        public void serialize(PairParameter<?, ?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("key", objects.key);
            jsonGenerator.writeObjectField("value", objects.value);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<K, V> extends JsonDeserializer<PairParameter<?, ?>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public PairParameter<K, V> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();
            PairParameter<K, V> pair;
            try {
                pair = (PairParameter<K, V>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            pair.key = JsonUtils.getObjectMapper().readerFor(pair.getKeyClass()).readValue(root.get("key"));
            pair.value = JsonUtils.getObjectMapper().readerFor(pair.getValueClass()).readValue(root.get("value"));

            return pair;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Deserializer<K, V> deserializer = new Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
