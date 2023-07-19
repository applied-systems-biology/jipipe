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

package org.hkijena.jipipe.extensions.parameters.api.pairs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Parameter equivalent of a map entry.
 * We suggest to use {@link org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList} if applicable, due to its greater flexibility.
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
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (key instanceof JIPipeValidatable) {
            report.report(new CustomValidationReportContext(context, "Key"), (JIPipeValidatable) key);
        }
        if (value instanceof JIPipeValidatable) {
            report.report(new CustomValidationReportContext(context, "Value"), (JIPipeValidatable) value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairParameter<?, ?> that = (PairParameter<?, ?>) o;
        return Objects.equals(keyClass, that.keyClass) && Objects.equals(valueClass, that.valueClass) && Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyClass, valueClass, key, value);
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
