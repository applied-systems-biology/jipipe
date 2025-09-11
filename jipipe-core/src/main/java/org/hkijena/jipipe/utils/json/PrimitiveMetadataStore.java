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

package org.hkijena.jipipe.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * A JSON-serializable map-like class that stores string keys with primitive values (String, Integer, Double, Boolean).
 * This class provides convenient methods for storing and retrieving primitive values with type-safe defaults.
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Create and populate a store
 * PrimitiveMetadataStore store = new PrimitiveMetadataStore();
 * store.put("name", "Test Example");
 * store.put("age", 25);
 * store.put("price", 19.99);
 * store.put("enabled", true);
 *
 * // Retrieve values with defaults
 * String name = store.getString("name", "Default Name");
 * int age = store.getInteger("age", 0);
 * double price = store.getDouble("price", 0.0);
 * boolean enabled = store.getBoolean("enabled", false);
 *
 * // JSON serialization
 * String jsonString = store.toJsonString();
 * PrimitiveMetadataStore fromJson = PrimitiveMetadataStore.fromJsonString(jsonString);
 * </pre>
 */
@JsonSerialize(using = PrimitiveMetadataStore.Serializer.class)
@JsonDeserialize(using = PrimitiveMetadataStore.Deserializer.class)
public class PrimitiveMetadataStore {
    private final Map<String, Object> data = new HashMap<>();

    /**
     * Creates an empty PrimitiveMetadataStore
     */
    public PrimitiveMetadataStore() {
    }

    /**
     * Creates a copy of an existing PrimitiveMetadataStore
     *
     * @param other the store to copy
     */
    public PrimitiveMetadataStore(PrimitiveMetadataStore other) {
        this.data.putAll(other.data);
    }

    /**
     * Creates a PrimitiveMetadataStore from a JSON string.
     *
     * @param json the JSON string to parse
     * @return a new PrimitiveMetadataStore instance
     */
    public static PrimitiveMetadataStore fromJsonString(String json) {
        try {
            return JsonUtils.readFromString(json, PrimitiveMetadataStore.class);
        } catch (Exception e) {
            // Return empty store on error
            return new PrimitiveMetadataStore();
        }
    }

    /**
     * Adds or updates a string value.
     *
     * @param key   the key (must not be null)
     * @param value the string value
     */
    public void put(String key, String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates an integer value.
     *
     * @param key   the key (must not be null)
     * @param value the integer value
     */
    public void put(String key, Integer value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a double value.
     *
     * @param key   the key (must not be null)
     * @param value the double value
     */
    public void put(String key, Double value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a boolean value.
     *
     * @param key   the key (must not be null)
     * @param value the boolean value
     */
    public void put(String key, Boolean value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Checks if the store contains the specified key.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(String key) {
        return key != null && data.containsKey(key);
    }

    /**
     * Returns the set of all keys in the store.
     *
     * @return an unmodifiable set of keys
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * Returns the number of entries in the store.
     *
     * @return the size of the store
     */
    public int size() {
        return data.size();
    }

    /**
     * Removes all entries from the store.
     */
    public void clear() {
        data.clear();
    }

    /**
     * Retrieves a string value with a default if not found or type mismatched.
     *
     * @param key          the key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the string value or default
     */
    public String getString(String key, String defaultValue) {
        return getTypedValue(key, defaultValue, String.class, String::valueOf);
    }

    /**
     * Retrieves an integer value with a default if not found or type mismatched.
     *
     * @param key          the key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the integer value or default
     */
    public Integer getInteger(String key, Integer defaultValue) {
        return getTypedValue(key, defaultValue, Integer.class, input -> Integer.valueOf(input.toString()));
    }

    /**
     * Retrieves a double value with a default if not found or type mismatched.
     *
     * @param key          the key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the double value or default
     */
    public Double getDouble(String key, Double defaultValue) {
        return getTypedValue(key, defaultValue, Double.class, input -> Double.valueOf(input.toString()));
    }

    /**
     * Retrieves a boolean value with a default if not found or type mismatched.
     *
     * @param key          the key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the boolean value or default
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return getTypedValue(key, defaultValue, Boolean.class, input -> {
            if (input instanceof Boolean) {
                return (Boolean) input;
            }
            if (input instanceof String) {
                return Boolean.parseBoolean((String) input);
            }
            return defaultValue;
        });
    }

    /**
     * Generic method to retrieve typed values with safe conversion.
     *
     * @param key          the key to retrieve
     * @param defaultValue the default value
     * @param expectedType the expected type
     * @param converter    conversion function from stored value to expected type
     * @param <T>          the type
     * @return the typed value or default
     */
    @SuppressWarnings("unchecked")
    private <T> T getTypedValue(String key, T defaultValue, Class<T> expectedType, Function<Object, T> converter) {
        if (key == null || !data.containsKey(key)) {
            return defaultValue;
        }

        Object storedValue = data.get(key);
        if (storedValue == null) {
            return defaultValue;
        }

        if (expectedType.isInstance(storedValue)) {
            return (T) storedValue;
        }

        try {
            return converter.apply(storedValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Converts this store to a JSON string using JsonUtils.
     *
     * @return JSON string representation of this store
     */
    public String toJsonString() {
        return JsonUtils.toJsonString(this);
    }

    /**
     * Returns an unmodifiable copy of the internal data map.
     *
     * @return the data map
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveMetadataStore that = (PrimitiveMetadataStore) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "PrimitiveMetadataStore{" +
                "data=" + data +
                '}';
    }

    /**
     * Jackson serializer for PrimitiveMetadataStore
     */
    public static class Serializer extends JsonSerializer<PrimitiveMetadataStore> {
        @Override
        public void serialize(PrimitiveMetadataStore store, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeObject(store.getData());
        }
    }

    /**
     * Jackson deserializer for PrimitiveMetadataStore
     */
    public static class Deserializer extends JsonDeserializer<PrimitiveMetadataStore> {
        @Override
        public PrimitiveMetadataStore deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            PrimitiveMetadataStore store = new PrimitiveMetadataStore();

            if (jsonParser.isExpectedStartObjectToken()) {
                com.fasterxml.jackson.databind.node.ObjectNode node = jsonParser.readValueAsTree();
                Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = node.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                    String key = field.getKey();
                    com.fasterxml.jackson.databind.JsonNode valueNode = field.getValue();

                    if (key != null && !key.isEmpty() && valueNode != null && !valueNode.isNull()) {
                        try {
                            if (valueNode.isTextual()) {
                                store.put(key, valueNode.asText());
                            } else if (valueNode.isInt()) {
                                store.put(key, valueNode.asInt());
                            } else if (valueNode.isDouble()) {
                                store.put(key, valueNode.asDouble());
                            } else if (valueNode.isBoolean()) {
                                store.put(key, valueNode.asBoolean());
                            }
                        } catch (Exception e) {
                            // Skip invalid values
                        }
                    }
                }
            }

            return store;
        }
    }
}