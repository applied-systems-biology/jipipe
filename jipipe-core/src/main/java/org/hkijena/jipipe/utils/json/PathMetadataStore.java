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
 * functionality to work with hierarchical paths (like internal config paths). This class automatically handles
 * cross-platform path separator normalization, ensuring consistent behavior across Windows (backslashes), macOS, and Linux (forward slashes).
 *
 * <h3>JSON-Serializable Object Support:</h3>
 * <p>
 * The PathMetadataStore now supports storing and retrieving complex JSON-serializable objects:
 * <ul>
 *   <li>Use {@link #putObject(Path, Object)} and {@link #putObject(String, Object)} to store JSON-serializable objects</li>
 *   <li>Use {@link #getObject(Path, Class)} and {@link #getObject(String, Class)} to retrieve objects with type safety</li>
 *   <li>Objects are cached after successful deserialization for performance</li>
 *   <li>Cache is cleared when underlying data changes to ensure consistency</li>
 * </ul>
 * </p>
 *
 * <h3>Cross-Platform Path Handling:</h3>
 * <p>
 * The PathMetadataStore automatically normalizes all paths to use forward slashes (/) for consistency:
 * <ul>
 *   <li>When saving/serializing: All paths are converted to use forward slashes</li>
 *   <li>When loading/deserializing: Backslashes from Windows paths are automatically converted to forward slashes</li>
 *   <li>When storing: Paths are stored internally with forward slashes regardless of the platform</li>
 * </ul>
 * This ensures that documents created on any platform can be read correctly on any other platform.
 * </p>
 *
 * <h3>Migration Support:</h3>
 * <p>
 * For existing documents that contain backslash paths, the class provides migration methods:
 * <ul>
 *   <li>{@link #migrateBackslashPaths()}: Migrates existing backslash paths to forward slashes</li>
 *   <li>{@link #hasBackslashPaths()}: Checks if any paths contain backslashes that need migration</li>
 *   <li>{@link #getNormalizedCopy()}: Creates a copy with all paths normalized</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Create and populate a path store
 * PathMetadataStore store = new PathMetadataStore();
 * store.put(Paths.get("settings/general/name"), "Application");
 * store.put(Paths.get("settings/general/version"), 1.0);
 * store.put(Paths.get("ui/colors/theme"), "dark");
 * store.put(Paths.get("features/enhanced-mode"), true);
 *
 * // JSON-serializable object support
 * Map<String, Object> config = new HashMap<>();
 * config.put("database", "localhost");
 * config.put("port", 5432);
 * config.put("ssl", true);
 * store.putObject(Paths.get("database/config"), config);
 *
 * // Retrieve complex objects with type safety
 * Map<String, Object> retrievedConfig = store.getObject(Paths.get("database/config"), Map.class);
 * // Result: {database="localhost", port=5432, ssl=true}
 *
 * // Using string paths for JSON objects (convenience overloads)
 * List<String> features = Arrays.asList("feature1", "feature2", "feature3");
 * store.putObject("app/features", features);
 * List<String> retrievedFeatures = store.getObject("app/features", List.class);
 *
 * // Retrieve values with defaults
 * String name = store.getString(Paths.get("settings/general/name"), "Default App");
 * int version = store.getInteger(Paths.get("settings/general/version"), 1);
 *
 * // Using string paths (convenience overloads)
 * store.put("settings/icon", "icon.png");
 * String icon = store.getString("settings/icon", "default-icon.png");
 *
 * // Get all entries under a path segment
 * Map<Path, Object> generalSettings = store.getEntriesUnderPath(Paths.get("settings/general"));
 * // Result: {settings/general/name: "Application", settings/general/version: 1.0}
 *
 * // Convert to a nested map structure
 * Map<String, Object> nested = store.toNestedMap();
 * // Result: {settings={general={name="Application", version=1.0}}, ui={colors={theme="dark"}}, features={enhanced-mode=true}, database={config={database="localhost", port=5432, ssl=true}}}
 *
 * // Merge/override entries from another store
 * PathMetadataStore other = new PathMetadataStore();
 * other.put(Paths.get("settings/general/name"), "New Application");
 * other.put(Paths.get("new/feature"), true);
 * store.putAll(other);
 * // Result: store now has "New Application" and "new/feature: true" added
 *
 * // Clear entries with a path prefix
 * store.clearEntriesWithPathPrefix("settings");
 * // Result: removes all entries under "settings/" prefix
 *
 * // Cross-platform path handling
 * store.put(Paths.get("windows\\path\\to\\file"), "value");  // Backslashes from Windows
 * store.migrateBackslashPaths();  // Convert to forward slashes
 * // Now accessible with: store.getString(Paths.get("windows/path/to/file"), "default")
 *
 * // Check for cross-platform compatibility
 * if (store.hasBackslashPaths()) {
 *     store.migrateBackslashPaths();
 * }
 * </pre>
 */
@JsonSerialize(using = PathMetadataStore.Serializer.class)
@JsonDeserialize(using = PathMetadataStore.Deserializer.class)
public class PathMetadataStore {
    private final Map<Path, Object> data = new HashMap<>();

    /**
     * Creates an empty PathMetadataStore
     */
    public PathMetadataStore() {
    }

    /**
     * Creates a copy of an existing PathMetadataStore
     *
     * @param other the store to copy
     */
    public PathMetadataStore(PathMetadataStore other) {
        this.data.putAll(other.data);
    }

    /**
     * Creates a PathMetadataStore from a PrimitiveMetadataStore by converting string keys to paths
     *
     * @param primitiveStore the store to convert
     */
    public PathMetadataStore(PrimitiveMetadataStore primitiveStore) {
        for (String key : primitiveStore.keySet()) {
            Path path = Paths.get(key);
            Object value = primitiveStore.getData().get(key);
            if (value != null) {
                this.data.put(path, value);
            }
        }
    }

    /**
     * Normalizes a path string to use forward slashes consistently across platforms.
     * This method converts backslashes (\) to forward slashes (/) to ensure
     * cross-platform compatibility.
     *
     * @param pathString the path string to normalize
     * @return the normalized path string with forward slashes, or null if input is null
     */
    public static String normalizePathString(String pathString) {
        if (pathString == null) {
            return null;
        }
        return pathString.replace('\\', '/');
    }

    /**
     * Normalizes a path to use forward slashes consistently across platforms.
     * This method creates a new Path object with a normalized string representation.
     *
     * @param path the path to normalize
     * @return a new Path object with normalized forward slashes, or null if input is null
     */
    public static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        return Paths.get(normalizePathString(path.toString()));
    }

    /**
     * Migrates existing paths that contain backslashes to use forward slashes.
     * This method should be called when loading existing documents that may have
     * been created on Windows systems with backslash separators.
     */
    public void migrateBackslashPaths() {
        Map<Path, Object> migratedData = new HashMap<>();

        for (Map.Entry<Path, Object> entry : data.entrySet()) {
            Path originalKey = entry.getKey();
            Object value = entry.getValue();

            // Normalize the path key to use forward slashes
            Path normalizedKey = normalizePath(originalKey);

            // Only add if the normalized key is different (migration needed)
            if (!originalKey.equals(normalizedKey)) {
                migratedData.put(normalizedKey, value);
            } else {
                // Keep original if no migration needed
                migratedData.put(originalKey, value);
            }
        }

        // Replace the data with migrated data
        this.data.clear();
        this.data.putAll(migratedData);
    }

    /**
     * Puts all entries from another PathMetadataStore into this store.
     * Entries from the other store will override existing entries with the same keys.
     *
     * @param other the store to put all entries from
     */
    public void putAll(PathMetadataStore other) {
        if (other != null) {
            this.data.putAll(other.data);
        }
    }

    // Put methods with Path keys

    /**
     * Adds or updates a string value using a Path key.
     *
     * @param key   the path key (must not be null)
     * @param value the string value
     */
    public void putPrimitive(Path key, String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates an integer value using a Path key.
     *
     * @param key   the path key (must not be null)
     * @param value the integer value
     */
    public void putPrimitive(Path key, Integer value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a double value using a Path key.
     *
     * @param key   the path key (must not be null)
     * @param value the double value
     */
    public void putPrimitive(Path key, Double value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a boolean value using a Path key.
     *
     * @param key   the path key (must not be null)
     * @param value the boolean value
     */
    public void putPrimitive(Path key, Boolean value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    // String overloads for convenience

    /**
     * Adds or updates a string value using a string path (converted to Path internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the string value
     */
    public void putPrimitive(String pathString, String value) {
        if (pathString != null && value != null) {
            putPrimitive(Paths.get(pathString), value);
        }
    }

    /**
     * Adds or updates an integer value using a string path (converted to Path internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the integer value
     */
    public void putPrimitive(String pathString, Integer value) {
        if (pathString != null && value != null) {
            putPrimitive(Paths.get(pathString), value);
        }
    }

    /**
     * Adds or updates a double value using a string path (converted to Path internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the double value
     */
    public void putPrimitive(String pathString, Double value) {
        if (pathString != null && value != null) {
            putPrimitive(Paths.get(pathString), value);
        }
    }

    /**
     * Adds or updates a boolean value using a string path (converted to Path internally).
     *
     * @param pathString the path string (must not be null)
     * @param value      the boolean value
     */
    public void putPrimitive(String pathString, Boolean value) {
        if (pathString != null && value != null) {
            putPrimitive(Paths.get(pathString), value);
        }
    }

    // Put methods for JSON-serializable objects

    /**
     * Adds or updates a JSON-serializable object using a Path key.
     * The object will be stored directly in the data map and serialized when needed.
     *
     * @param key   the path key (must not be null)
     * @param value the JSON-serializable object (must not be null)
     */
    public void putObject(Path key, Object value) {
        if (key != null && value != null) {
            // Store the object directly in data
            data.put(key, value);
        }
    }

    /**
     * Adds or updates a JSON-serializable object using a string path (converted to Path internally).
     * The object will be stored directly in the data map and serialized when needed.
     *
     * @param pathString the path string (must not be null)
     * @param value      the JSON-serializable object (must not be null)
     */
    public void putObject(String pathString, Object value) {
        if (pathString != null && value != null) {
            putObject(Paths.get(pathString), value);
        }
    }

    // Get methods with Path keys

    /**
     * Checks if the store contains the specified path key.
     *
     * @param key the path key to check
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(Path key) {
        return key != null && data.containsKey(key);
    }

    /**
     * Retrieves a string value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the string value or default
     */
    public String getString(Path key, String defaultValue) {
        return getTypedValue(key, defaultValue, String.class, String::valueOf);
    }

    /**
     * Retrieves an integer value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the integer value or default
     */
    public Integer getInteger(Path key, Integer defaultValue) {
        return getTypedValue(key, defaultValue, Integer.class, input -> Integer.valueOf(input.toString()));
    }

    /**
     * Retrieves a double value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the double value or default
     */
    public Double getDouble(Path key, Double defaultValue) {
        return getTypedValue(key, defaultValue, Double.class, input -> Double.valueOf(input.toString()));
    }

    /**
     * Retrieves a boolean value with a default if not found or type mismatched.
     *
     * @param key          the path key to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the boolean value or default
     */
    public Boolean getBoolean(Path key, Boolean defaultValue) {
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
        return getString(pathString != null ? Paths.get(pathString) : null, defaultValue);
    }

    /**
     * Retrieves an integer value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the integer value or default
     */
    public Integer getInteger(String pathString, Integer defaultValue) {
        return getInteger(pathString != null ? Paths.get(pathString) : null, defaultValue);
    }

    /**
     * Retrieves a double value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the double value or default
     */
    public Double getDouble(String pathString, Double defaultValue) {
        return getDouble(pathString != null ? Paths.get(pathString) : null, defaultValue);
    }

    /**
     * Retrieves a boolean value using a string path with a default if not found or type mismatched.
     *
     * @param pathString   the path string to look up
     * @param defaultValue the default value to return if key doesn't exist or type differs
     * @return the boolean value or default
     */
    public Boolean getBoolean(String pathString, Boolean defaultValue) {
        return getBoolean(pathString != null ? Paths.get(pathString) : null, defaultValue);
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
        return getList(pathString != null ? Paths.get(pathString) : null, type);
    }

    /**
     * Gets a list of the specified type
     *
     * @param key  the key
     * @param type the type
     * @param <T>  the type
     * @return the list
     */
    public <T> List<T> getList(Path key, Class<T> type) {
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
     * Retrieves a JSON-serializable object with type safety using a Path key.
     * If the object is already deserialized and stored in data, it's returned directly.
     * Otherwise, it attempts to deserialize from stored JSON string or JsonNode and
     * replaces the original value with the deserialized object in data.
     *
     * @param <T>  the type of the object to retrieve. Special cases are String and JsonNode that serialize any object independent of the type
     * @param key  the path key to look up
     * @param type the class of the object to retrieve
     * @return the deserialized object or null if not found or deserialization fails
     */
    public <T> T getObject(Path key, Class<T> type) {
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
        return getObject(pathString != null ? Paths.get(pathString) : null, type);
    }

    // Path-based collection methods

    /**
     * Returns a map of all entries where the path starts with the given prefix.
     *
     * @param prefixPath the path prefix to filter by
     * @return an unmodifiable map of entries matching the prefix
     */
    public Map<Path, Object> getEntriesUnderPath(Path prefixPath) {
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
    public Map<Path, Object> getEntriesUnderPath(String prefixString) {
        Path prefixPath = prefixString != null ? Paths.get(prefixString) : null;
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

        for (Map.Entry<Path, Object> entry : data.entrySet()) {
            Path path = entry.getKey();
            Object value = entry.getValue();

            // Use normalized path string to ensure consistent splitting
            String normalizedPathStr = normalizePathString(path.toString());
            String[] parts = normalizedPathStr.split("/");
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
     * Returns a string representation of all paths in this store using forward slashes.
     * This is useful for debugging and ensuring consistent path display.
     *
     * @return a set of normalized path strings
     */
    public Set<String> getNormalizedPathStrings() {
        Set<String> normalizedPaths = new HashSet<>();
        for (Path path : data.keySet()) {
            normalizedPaths.add(normalizePathString(path.toString()));
        }
        return Collections.unmodifiableSet(normalizedPaths);
    }

    /**
     * Checks if the store contains any paths with backslashes that need migration.
     *
     * @return true if any paths contain backslashes, false otherwise
     */
    public boolean hasBackslashPaths() {
        for (Path path : data.keySet()) {
            if (path.toString().contains("\\")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new PathMetadataStore with all paths normalized to use forward slashes.
     * This method returns a new store with migrated paths, leaving the original unchanged.
     *
     * @return a new PathMetadataStore with normalized paths
     */
    public PathMetadataStore getNormalizedCopy() {
        PathMetadataStore normalizedStore = new PathMetadataStore();

        for (Map.Entry<Path, Object> entry : data.entrySet()) {
            Path normalizedKey = normalizePath(entry.getKey());
            // Use direct access to the internal map to handle Object values
            normalizedStore.data.put(normalizedKey, entry.getValue());
        }

        return normalizedStore;
    }

    /**
     * Returns the set of all keys in the store.
     *
     * @return an unmodifiable set of path keys
     */
    public Set<Path> keySet() {
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
    public void clearEntriesWithPathPrefix(String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isEmpty()) {
            return;
        }

        // Convert string prefixes to Path objects
        Path prefixPath = Paths.get(pathPrefix);

        // Handle trailing separator normalization by creating both versions
        Path normalizedPrefixPath = Paths.get(pathPrefix);
        if (!pathPrefix.endsWith("/")) {
            normalizedPrefixPath = Paths.get(pathPrefix + "/");
        }

        // Collect all keys to remove to avoid concurrent modification
        List<Path> keysToRemove = new ArrayList<>();
        for (Path key : data.keySet()) {
            if (key != null) {
                if (isUnderPath(key, prefixPath) || isUnderPath(key, normalizedPrefixPath)) {
                    keysToRemove.add(key);
                }
            }
        }

        // Remove all collected keys
        for (Path key : keysToRemove) {
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
     * store.put(Paths.get("settings/general/name"), "Application");
     * store.put(Paths.get("settings/general/version"), 1.0);
     * store.put(Paths.get("settings/ui/theme"), "dark");
     * store.put(Paths.get("features/enhanced"), true);
     *
     * // Clear all settings entries
     * store.clearEntriesWithPathPrefix(Paths.get("settings"));
     * // Result: {"features/enhanced": true} remains
     *
     * // Clear with trailing slash
     * store.clearEntriesWithPathPrefix(Paths.get("features/"));
     * // Result: store is now empty
     *
     * // No matching prefix - nothing happens
     * store.clearEntriesWithPathPrefix(Paths.get("nonexistent"));
     * // Result: store remains unchanged
     * </pre>
     *
     * @param pathPrefix the path prefix to remove entries for (must not be null)
     */
    public void clearEntriesWithPathPrefix(Path pathPrefix) {
        if (pathPrefix == null) {
            return;
        }

        // Handle trailing separator normalization by creating both versions
        Path normalizedPrefixPath = pathPrefix;
        if (!pathPrefix.toString().endsWith("/")) {
            normalizedPrefixPath = Paths.get(pathPrefix.toString() + "/");
        }

        // Collect all keys to remove to avoid concurrent modification
        List<Path> keysToRemove = new ArrayList<>();
        for (Path key : data.keySet()) {
            if (key != null) {
                if (isUnderPath(key, pathPrefix) || isUnderPath(key, normalizedPrefixPath)) {
                    keysToRemove.add(key);
                }
            }
        }

        // Remove all collected keys
        for (Path key : keysToRemove) {
            data.remove(key);
        }
    }

    /**
     * Returns an unmodifiable copy of the internal data map.
     *
     * @return the data map
     */
    public Map<Path, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    // Helper methods

    private boolean isUnderPath(Path path, Path prefix) {
        if (path == null || prefix == null) {
            return false;
        }

        String pathStr = path.toString();
        String prefixStr = prefix.toString();

        if (pathStr.length() < prefixStr.length()) {
            return false;
        }

        if (pathStr.startsWith(prefixStr)) {
            // Check if the next character is either end of string or a path separator
            if (pathStr.length() == prefixStr.length()) {
                return true;
            }
            char nextChar = pathStr.charAt(prefixStr.length());
            return nextChar == '/';
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> T getTypedValue(Path key, T defaultValue, Class<T> expectedType, Function<Object, T> converter) {
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
        PathMetadataStore that = (PathMetadataStore) o;
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
    public static class Serializer extends JsonSerializer<PathMetadataStore> {
        @Override
        public void serialize(PathMetadataStore store, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            // Convert to string-based map for JSON serialization with normalized paths
            Map<String, Object> stringMap = new LinkedHashMap<>();
            for (Map.Entry<Path, Object> entry : store.getData().entrySet()) {
                // Use normalized path string to ensure forward slashes across platforms
                stringMap.put(normalizePathString(entry.getKey().toString()), entry.getValue());
            }
            jsonGenerator.writeObject(stringMap);
        }
    }

    /**
     * Jackson deserializer for PathMetadataStore
     */
    public static class Deserializer extends JsonDeserializer<PathMetadataStore> {
        @Override
        public PathMetadataStore deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            PathMetadataStore store = new PathMetadataStore();

            if (jsonParser.isExpectedStartObjectToken()) {
                ObjectNode node = jsonParser.readValueAsTree();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String keyStr = field.getKey();
                    JsonNode valueNode = field.getValue();

                    if (keyStr != null && !keyStr.isEmpty() && valueNode != null && !valueNode.isNull()) {
                        // Normalize the key string to handle backslashes from Windows paths
                        String normalizedKeyStr = normalizePathString(keyStr);
                        Path key = Paths.get(normalizedKeyStr);

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
}