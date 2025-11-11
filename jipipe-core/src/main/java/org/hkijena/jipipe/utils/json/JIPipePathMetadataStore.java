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
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A JSON-serializable map-like class that stores Path keys with both primitive values (String, Integer, Double, Boolean)
 * and complex JSON-serializable objects. This class extends PrimitiveMetadataStore with Path-based keys and provides
 * functionality to work with hierarchical paths (like internal config paths). This class uses the Key system for
 * automatic path normalization, ensuring consistent behavior across platforms.
 *
 * <h3>JSON-Serializable Object Support:</h3>
 * <p>
 * The PathMetadataStore supports storing and retrieving complex JSON-serializable objects:
 * <ul>
 *   <li>Use {@link #putObject(Key, Object)} and {@link #putObject(String, Object)} to store JSON-serializable objects</li>
 *   <li>Use {@link #getObject(Key, Class)} and {@link #getObject(String, Class)} to retrieve objects with type safety</li>
 *   <li>Objects are cached after successful deserialization for performance</li>
 *   <li>Cache is cleared when underlying data changes to ensure consistency</li>
 * </ul>
 * </p>
 *
 * <h3>Path Handling:</h3>
 * <p>
 * The PathMetadataStore uses the Key system for automatic path normalization:
 * <ul>
 *   <li>When saving/serializing: All paths are converted to use forward slashes</li>
 *   <li>When loading/deserializing: Backslashes from Windows paths are automatically converted to forward slashes</li>
 *   <li>Paths are stored internally using the Key system for consistent comparison and handling</li>
 * </ul>
 * This ensures that documents created on any platform can be read correctly on any other platform.
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Create and populate a path store
 * PathMetadataStore store = new PathMetadataStore();
 * store.putPrimitive("settings/general/name", "Application");
 * store.putPrimitive("settings/general/version", 1.0);
 * store.putPrimitive("ui/colors/theme", "dark");
 * store.putPrimitive("features/enhanced-mode", true);
 *
 * // JSON-serializable object support
 * Map<String, Object> config = new HashMap<>();
 * config.put("database", "localhost");
 * config.put("port", 5432);
 * config.put("ssl", true);
 * store.putObject("database/config", config);
 *
 * // Retrieve complex objects with type safety
 * Map<String, Object> retrievedConfig = store.getObject("database/config", Map.class);
 * // Result: {database="localhost", port=5432, ssl=true}
 *
 * // Using string paths for JSON objects (convenience overloads)
 * List<String> features = Arrays.asList("feature1", "feature2", "feature3");
 * store.putObject("app/features", features);
 * List<String> retrievedFeatures = store.getObject("app/features", List.class);
 *
 * // Retrieve values with defaults
 * String name = store.getString("settings/general/name", "Default App");
 * int version = store.getInteger("settings/general/version", 1);
 *
 * // Get all entries under a path segment
 * Map<Key, Object> generalSettings = store.getEntriesUnderPath("settings/general");
 * // Result: {settings/general/name: "Application", settings/general/version: 1.0}
 *
 * // Convert to a nested map structure
 * Map<String, Object> nested = store.toNestedMap();
 * // Result: {settings={general={name="Application", version=1.0}}, ui={colors={theme="dark"}}, features={enhanced-mode=true}, database={config={database="localhost", port=5432, ssl=true}}}
 *
 * // Merge/override entries from another store
 * PathMetadataStore other = new PathMetadataStore();
 * other.putPrimitive("settings/general/name", "New Application");
 * other.putPrimitive("new/feature", true);
 * store.putAll(other);
 * // Result: store now has "New Application" and "new/feature: true" added
 *
 * // Clear entries with a path prefix
 * store.clearEntriesWithPathPrefix("settings");
 * // Result: removes all entries under "settings/" prefix
 * </pre>
 */
@JsonSerialize(using = JIPipePathMetadataStore.Serializer.class)
@JsonDeserialize(using = JIPipePathMetadataStore.Deserializer.class)
public class JIPipePathMetadataStore {
    private final Map<Key, Object> data = new HashMap<>();

    /**
     * Creates an empty PathMetadataStore
     */
    public JIPipePathMetadataStore() {
    }

    /**
     * Creates a copy of an existing PathMetadataStore
     *
     * @param other the store to copy
     */
    public JIPipePathMetadataStore(JIPipePathMetadataStore other) {
        this.data.putAll(other.data);
    }

    /**
     * Creates a PathMetadataStore from a PrimitiveMetadataStore by converting string keys to paths
     *
     * @param primitiveStore the store to convert
     */
    public JIPipePathMetadataStore(PrimitiveMetadataStore primitiveStore) {
        for (String key : primitiveStore.keySet()) {
            Key pathKey = key(key.split("/"));
            Object value = primitiveStore.getData().get(key);
            if (value != null) {
                this.data.put(pathKey, value);
            }
        }
    }

    /**
     * Puts all entries from another PathMetadataStore into this store.
     * Entries from the other store will override existing entries with the same keys.
     *
     * @param other the store to put all entries from
     */
    public void putAll(JIPipePathMetadataStore other) {
        if (other != null) {
            this.data.putAll(other.data);
        }
    }

    // Put methods with Key keys

    /**
     * Adds or updates a string value using a Key key.
     *
     * @param key   the path key (must not be null)
     * @param value the string value
     */
    public void putPrimitive(Key key, String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates an integer value using a Key key.
     *
     * @param key   the path key (must not be null)
     * @param value the integer value
     */
    public void putPrimitive(Key key, Integer value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a double value using a Key key.
     *
     * @param key   the path key (must not be null)
     * @param value the double value
     */
    public void putPrimitive(Key key, Double value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a boolean value using a Key key.
     *
     * @param key   the path key (must not be null)
     * @param value the boolean value
     */
    public void putPrimitive(Key key, Boolean value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    // String overloads for convenience

    /**
     * Adds or updates a string value using a string path (converted to Key internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the string value
     */
    public void putPrimitive(String pathString, String value) {
        if (pathString != null && value != null) {
            putPrimitive(key(pathString.split("/")), value);
        }
    }

    /**
     * Adds or updates an integer value using a string path (converted to Key internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the integer value
     */
    public void putPrimitive(String pathString, Integer value) {
        if (pathString != null && value != null) {
            putPrimitive(key(pathString.split("/")), value);
        }
    }

    /**
     * Adds or updates a double value using a string path (converted to Key internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the double value
     */
    public void putPrimitive(String pathString, Double value) {
        if (pathString != null && value != null) {
            putPrimitive(key(pathString.split("/")), value);
        }
    }

    /**
     * Adds or updates a boolean value using a string path (converted to Key internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the boolean value
     */
    public void putPrimitive(String pathString, Boolean value) {
        if (pathString != null && value != null) {
            putPrimitive(key(pathString.split("/")), value);
        }
    }

    // Put methods for JSON-serializable objects

    /**
     * Adds or updates a JSON-serializable object using a Key key.
     * The object will be stored directly in the data map and serialized when needed.
     *
     * @param key   the path key (must not be null)
     * @param value the JSON-serializable object (must not be null)
     */
    public void putObject(Key key, Object value) {
        if (key != null && value != null) {
            // Store the object directly in data
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a JSON-serializable object using a string path (converted to Key internally).
     * The object will be stored directly in the data map and serialized when needed.
     *
     * @param pathString the path string (must not be null)
     * @param value      the JSON-serializable object (must not be null)
     */
    public void putObject(String pathString, Object value) {
        if (pathString != null && value != null) {
            putObject(key(pathString.split("/")), value);
        }
    }

    // Get methods with Path keys

    /**
     * Checks if the store contains the specified path key.
     *
     * @param key the path key to check
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(Key key) {
        return key != null && data.containsKey(key);
    }

    /**
     * Retrieves a string value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the string value or default
     */
    public String getString(Key key, String defaultValue) {
        return getTypedValue(key, defaultValue, String.class, String::valueOf);
    }

    /**
     * Retrieves an integer value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the integer value or default
     */
    public Integer getInteger(Key key, Integer defaultValue) {
        return getTypedValue(key, defaultValue, Integer.class, input -> Integer.valueOf(input.toString()));
    }

    /**
     * Retrieves a double value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the double value or default
     */
    public Double getDouble(Key key, Double defaultValue) {
        return getTypedValue(key, defaultValue, Double.class, input -> Double.valueOf(input.toString()));
    }

    /**
     * Retrieves a boolean value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the boolean value or default
     */
    public Boolean getBoolean(Key key, Boolean defaultValue) {
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

    // String overloads for convenience

    /**
     * Retrieves a string value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the string value or default
     */
    public String getString(String pathString, String defaultValue) {
        return getString(pathString != null ? key(pathString.split("/")) : null, defaultValue);
    }

    /**
     * Retrieves an integer value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the integer value or default
     */
    public Integer getInteger(String pathString, Integer defaultValue) {
        return getInteger(pathString != null ? key(pathString.split("/")) : null, defaultValue);
    }

    /**
     * Retrieves a double value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the double value or default
     */
    public Double getDouble(String pathString, Double defaultValue) {
        return getDouble(pathString != null ? key(pathString.split("/")) : null, defaultValue);
    }

    /**
     * Retrieves a boolean value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the boolean value or default
     */
    public Boolean getBoolean(String pathString, Boolean defaultValue) {
        return getBoolean(pathString != null ? key(pathString.split("/")) : null, defaultValue);
    }

    /**
     * Gets a list of the specified type
     *
     * @param pathString the key
     * @param type       the type
     * @param <T>        the type
     * @return the list
     */
    public <T> List<T> getList(String pathString, Class<T> type) {
        return getList(pathString != null ? key(pathString.split("/")) : null, type);
    }

    /**
     * Gets a list of the specified type
     *
     * @param key  the key
     * @param type the type
     * @param <T>  the type
     * @return the list
     */
    public <T> List<T> getList(Key key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }

        Object storedValue = data.get(key);
        if (storedValue != null) {
            try {
                List<T> deserialized;
                if (storedValue instanceof JsonNode) {
                    // Deserialize from JsonNode
                    deserialized = JsonUtils.getObjectMapper().readerForListOf(type).readValue((JsonNode) storedValue);
                } else if (storedValue instanceof List) {
                    deserialized = (List<T>) storedValue;
                } else {
                    return null;
                }
                // Replace the original value with the deserialized object for caching
                data.put(key, deserialized);
                return deserialized;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Retrieves a JSON-serializable object with type safety using a Key key.
     * If the object is already deserialized and stored in data, it's returned directly.
     * Otherwise, it attempts to deserialize from stored JSON string or JsonNode and
     * replaces the original value with the deserialized object in data.
     *
     * @param <T>  the type of the object to retrieve. Special cases are String and JsonNode that serialize any object independent of the type
     * @param key  the path key to look up
     * @param type the class of the object to retrieve
     * @return the deserialized object or null if not found or deserialization fails
     */
    public <T> T getObject(Key key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }

        Object storedValue = data.get(key);
        if (storedValue != null) {
            try {
                T deserialized;
                if (storedValue instanceof JsonNode) {
                    // Deserialize from JsonNode
                    deserialized = JsonUtils.getObjectMapper().treeToValue((JsonNode) storedValue, type);
                } else {
                    // Direct type match
                    if (type.isInstance(storedValue)) {
                        deserialized = type.cast(storedValue);
                    } else if (type == JsonNode.class) {
                        // Special case: serialize back into JSON
                        return (T) JsonUtils.getObjectMapper().convertValue(storedValue, JsonNode.class);
                    } else if (type == String.class) {
                        return (T) JsonUtils.toJsonString(storedValue);
                    } else {
                        return null;
                    }
                }
                // Replace the original value with the deserialized object for caching
                data.put(key, deserialized);
                return deserialized;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Retrieves a JSON-serializable object with type safety using a string path.
     * If the object is already deserialized and stored in data, it's returned directly.
     * Otherwise, it attempts to deserialize from stored JSON string or JsonNode and
     * replaces the original value with the deserialized object in data.
     *
     * @param <T>        the type of the object to retrieve
     * @param pathString the path string to look up
     * @param type       the class of the object to retrieve
     * @return the deserialized object or null if not found or deserialization fails
     */
    public <T> T getObject(String pathString, Class<T> type) {
        return getObject(pathString != null ? key(pathString.split("/")) : null, type);
    }

    // Path-based collection methods

    /**
     * Returns a map of all entries where the path starts with the given prefix.
     *
     * @param prefixPath the path prefix to filter by
     * @return an unmodifiable map of entries matching the prefix
     */
    public Map<Key, Object> getEntriesUnderPath(Key prefixPath) {
        if (prefixPath == null) {
            return Collections.emptyMap();
        }

        return data.entrySet().stream()
                .filter(entry -> isUnderPath(entry.getKey(), prefixPath))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    /**
     * Returns a map of all entries where the path starts with the given prefix string.
     *
     * @param prefixString the path prefix string to filter by
     * @return an unmodifiable map of entries matching the prefix
     */
    public Map<Key, Object> getEntriesUnderPath(String prefixString) {
        Key prefixPath = prefixString != null ? key(prefixString.split("/")) : null;
        return getEntriesUnderPath(prefixPath);
    }

    /**
     * Converts this store to a nested map structure that corresponds to the path hierarchy.
     * For example, a key "settings/general/name" would result in: {settings={general={name="value"}}}
     *
     * @return a nested map structure
     */
    public Map<String, Object> toNestedMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<Key, Object> entry : data.entrySet()) {
            Key key = entry.getKey();
            Object value = entry.getValue();

            // Use the key items to build the nested structure
            String[] parts = key.items;
            Map<String, Object> current = result;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Object next = current.get(part);

                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(part, newMap);
                    current = newMap;
                }
            }

            current.put(parts[parts.length - 1], value);
        }

        return result;
    }

    /**
     * Returns the set of all keys in the store.
     *
     * @return an unmodifiable set of path keys
     */
    public Set<Key> keySet() {
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
     * Removes all entries where the path starts with the given prefix.
     * This method handles pathPrefix with or without trailing separator (e.g., "path" and "path/" will both match "path/subkey").
     * If pathPrefix is null or empty, this method does nothing.
     *
     * <h3>Examples:</h3>
     * <pre>
     * PathMetadataStore store = new PathMetadataStore();
     * store.put(Paths.get("settings/general/name"), "Application");
     * store.put(Paths.get("settings/general/version"), 1.0);
     * store.put(Paths.get("settings/ui/theme"), "dark");
     * store.put(Paths.get("features/enhanced"), true);
     *
     * // Clear all settings entries
     * store.clearEntriesWithPathPrefix("settings");
     * // Result: {"features/enhanced": true} remains
     *
     * // Clear with trailing slash
     * store.clearEntriesWithPathPrefix("features/");
     * // Result: store is now empty
     *
     * // No matching prefix - nothing happens
     * store.clearEntriesWithPathPrefix("nonexistent");
     * // Result: store remains unchanged
     * </pre>
     *
     * @param pathPrefix the path prefix to remove entries for (must not be null or empty)
     */
    /**
     * Removes all entries where the path starts with the given prefix.
     * This method handles pathPrefix with or without trailing separator (e.g., "path" and "path/" will both match "path/subkey").
     * If pathPrefix is null or empty, this method does nothing.
     *
     * <h3>Examples:</h3>
     * <pre>
     * PathMetadataStore store = new PathMetadataStore();
     * store.put("settings/general/name", "Application");
     * store.put("settings/general/version", 1.0);
     * store.put("settings/ui/theme", "dark");
     * store.put("features/enhanced", true);
     *
     * // Clear all settings entries
     * store.clearEntriesWithPathPrefix("settings");
     * // Result: {"features/enhanced": true} remains
     *
     * // Clear with trailing slash
     * store.clearEntriesWithPathPrefix("features/");
     * // Result: store is now empty
     *
     * // No matching prefix - nothing happens
     * store.clearEntriesWithPathPrefix("nonexistent");
     * // Result: store remains unchanged
     * </pre>
     *
     * @param pathPrefix the path prefix to remove entries for (must not be null or empty)
     */
    public void clearEntriesWithPathPrefix(String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isEmpty()) {
            return;
        }

        // Convert string prefixes to Key objects
        Key prefixKey = key(pathPrefix.split("/"));

        // Handle trailing separator normalization by creating both versions
        Key normalizedPrefixKey = prefixKey;
        if (!pathPrefix.endsWith("/")) {
            normalizedPrefixKey = key((pathPrefix + "/").split("/"));
        }

        // Collect all keys to remove to avoid concurrent modification
        List<Key> keysToRemove = new ArrayList<>();
        for (Key key : data.keySet()) {
            if (key != null) {
                if (isUnderPath(key, prefixKey) || isUnderPath(key, normalizedPrefixKey)) {
                    keysToRemove.add(key);
                }
            }
        }

        // Remove all collected keys
        for (Key key : keysToRemove) {
            data.remove(key);
        }
    }

    /**
     * Removes all entries where the path starts with the given prefix.
     * This method handles pathPrefix with or without trailing separator (e.g., "path" and "path/" will both match "path/subkey").
     * If pathPrefix is null, this method does nothing.
     *
     * <h3>Examples:</h3>
     * <pre>
     * PathMetadataStore store = new PathMetadataStore();
     * store.put("settings/general/name", "Application");
     * store.put("settings/general/version", 1.0);
     * store.put("settings/ui/theme", "dark");
     * store.put("features/enhanced", true);
     *
     * // Clear all settings entries
     * store.clearEntriesWithPathPrefix(key("settings".split("/")));
     * // Result: {"features/enhanced": true} remains
     *
     * // Clear with trailing slash
     * store.clearEntriesWithPathPrefix(key("features/".split("/")));
     * // Result: store is now empty
     *
     * // No matching prefix - nothing happens
     * store.clearEntriesWithPathPrefix(key("nonexistent".split("/")));
     * // Result: store remains unchanged
     * </pre>
     *
     * @param pathPrefix the path prefix to remove entries for (must not be null)
     */
    public void clearEntriesWithPathPrefix(Key pathPrefix) {
        if (pathPrefix == null) {
            return;
        }

        // Handle trailing separator normalization by creating both versions
        Key normalizedPrefixKey = pathPrefix;
        if (!pathPrefix.items[pathPrefix.items.length - 1].isEmpty()) {
            String[] newItems = Arrays.copyOf(pathPrefix.items, pathPrefix.items.length + 1);
            newItems[pathPrefix.items.length] = "";
            normalizedPrefixKey = key(newItems);
        }

        // Collect all keys to remove to avoid concurrent modification
        List<Key> keysToRemove = new ArrayList<>();
        for (Key key : data.keySet()) {
            if (key != null) {
                if (isUnderPath(key, pathPrefix) || isUnderPath(key, normalizedPrefixKey)) {
                    keysToRemove.add(key);
                }
            }
        }

        // Remove all collected keys
        for (Key key : keysToRemove) {
            data.remove(key);
        }
    }

    /**
     * Returns an unmodifiable copy of the internal data map.
     *
     * @return the data map
     */
    public Map<Key, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    // Helper methods

    private boolean isUnderPath(Key path, Key prefix) {
        if (path == null || prefix == null) {
            return false;
        }

        String[] pathItems = path.items;
        String[] prefixItems = prefix.items;

        if (pathItems.length < prefixItems.length) {
            return false;
        }

        for (int i = 0; i < prefixItems.length; i++) {
            if (!pathItems[i].equals(prefixItems[i])) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private <T> T getTypedValue(Key key, T defaultValue, Class<T> expectedType, Function<Object, T> converter) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipePathMetadataStore that = (JIPipePathMetadataStore) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "PathMetadataStore{" +
                "data=" + data +
                '}';
    }

    /**
     * Jackson serializer for PathMetadataStore
     */
    public static class Serializer extends JsonSerializer<JIPipePathMetadataStore> {
        @Override
        public void serialize(JIPipePathMetadataStore store, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            // Convert to string-based map for JSON serialization with normalized paths
            Map<String, Object> stringMap = new LinkedHashMap<>();
            for (Map.Entry<Key, Object> entry : store.getData().entrySet()) {
                // Use normalized path string to ensure forward slashes across platforms
                stringMap.put(String.join("/", entry.getKey().items), entry.getValue());
            }
            jsonGenerator.writeObject(stringMap);
        }
    }

    /**
     * Jackson deserializer for PathMetadataStore
     */
    public static class Deserializer extends JsonDeserializer<JIPipePathMetadataStore> {
        @Override
        public JIPipePathMetadataStore deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipePathMetadataStore store = new JIPipePathMetadataStore();

            if (jsonParser.isExpectedStartObjectToken()) {
                ObjectNode node = jsonParser.readValueAsTree();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String keyStr = field.getKey();
                    JsonNode valueNode = field.getValue();

                    if (keyStr != null && !keyStr.isEmpty() && valueNode != null && !valueNode.isNull()) {
                        // Normalize the key string to handle backslashes from Windows paths
                        String normalizedKeyStr = keyStr.replace('\\', '/');
                        
                        // Split by both forward and backward slashes to handle cross-platform paths
                        String[] pathItems = normalizedKeyStr.split("[/\\\\]");
                        
                        // Trim away empty components (e.g., "/a//b" turns into ["a", "b"])
                        List<String> filteredItems = new ArrayList<>();
                        for (String item : pathItems) {
                            if (!item.isEmpty()) {
                                filteredItems.add(item);
                            }
                        }
                        
                        Key key = key(filteredItems.toArray(new String[0]));

                        try {
                            if (valueNode.isTextual()) {
                                store.putPrimitive(key, valueNode.asText());
                            } else if (valueNode.isInt()) {
                                store.putPrimitive(key, valueNode.asInt());
                            } else if (valueNode.isDouble()) {
                                store.putPrimitive(key, valueNode.asDouble());
                            } else if (valueNode.isBoolean()) {
                                store.putPrimitive(key, valueNode.asBoolean());
                            } else {
                                // Store complex JSON objects as JsonNode for later deserialization
                                store.data.put(key, valueNode);
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

    public static Key key(String... arr) {
        return new Key(arr);
    }

    public static final class Key {
        private final String[] items;

        public Key(String[] items) {
            this.items = items;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return Objects.deepEquals(items, key.items);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(items);
        }
    }
}