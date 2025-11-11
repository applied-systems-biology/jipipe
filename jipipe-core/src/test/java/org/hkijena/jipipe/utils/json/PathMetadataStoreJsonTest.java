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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JSON-serializable object support in PathMetadataStore
 */
public class PathMetadataStoreJsonTest {

    @Test
    public void testPutAndGetObjectWithPathKey() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with a simple map
        Map<String, Object> config = new HashMap<>();
        config.put("database", "localhost");
        config.put("port", 5432);
        config.put("ssl", true);

        JIPipePathMetadataStore.Key configPath = JIPipePathMetadataStore.key("database/config");
        store.putObject(configPath, config);

        // Retrieve with type safety
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedConfig = store.getObject(configPath, Map.class);

        assertNotNull(retrievedConfig);
        assertEquals("localhost", retrievedConfig.get("database"));
        assertEquals(5432, retrievedConfig.get("port"));
        assertEquals(true, retrievedConfig.get("ssl"));

        // Test caching - second call should return the same cached object
        Map<String, Object> cachedConfig = store.getObject(configPath, Map.class);
        assertSame(retrievedConfig, cachedConfig);
    }

    @Test
    public void testPutAndGetObjectWithStringKey() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with a list
        List<String> features = Arrays.asList("feature1", "feature2", "feature3");

        store.putObject("app/features", features);

        // Retrieve with type safety
        List<String> retrievedFeatures = store.getObject("app/features", List.class);

        assertNotNull(retrievedFeatures);
        assertEquals(3, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        assertEquals("feature3", retrievedFeatures.get(2));

        // Test caching - second call should return the same cached object
        List<String> cachedFeatures = store.getObject("app/features", List.class);
        assertSame(retrievedFeatures, cachedFeatures);
    }

    @Test
    public void testJsonSerializationAndDeserialization() {
        JIPipePathMetadataStore originalStore = new JIPipePathMetadataStore();

        // Add various types of data including JSON objects
        originalStore.putPrimitive("settings/app/name", "TestApp");
        originalStore.putPrimitive("settings/app/version", 1.0);
        originalStore.putPrimitive("settings/app/enabled", true);

        Map<String, Object> config = new HashMap<>();
        config.put("host", "example.com");
        config.put("timeout", 5000);
        config.put("retries", 3);
        originalStore.putObject("settings/server", config);

        List<String> plugins = Arrays.asList("plugin1", "plugin2");
        originalStore.putObject("settings/plugins", plugins);

        // Serialize to JSON string
        String jsonString = JsonUtils.toJsonString(originalStore);
        assertNotNull(jsonString);

        // Deserialize from JSON string
        JIPipePathMetadataStore deserializedStore = JsonUtils.readFromString(jsonString, JIPipePathMetadataStore.class);

        // Verify primitive values
        assertEquals("TestApp", deserializedStore.getString("settings/app/name", null));
        assertEquals(1.0, deserializedStore.getDouble("settings/app/version", null));
        assertEquals(true, deserializedStore.getBoolean("settings/app/enabled", null));

        // Verify complex objects
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedConfig = deserializedStore.getObject("settings/server", Map.class);
        assertNotNull(retrievedConfig);
        assertEquals("example.com", retrievedConfig.get("host"));
        assertEquals(5000, retrievedConfig.get("timeout"));
        assertEquals(3, retrievedConfig.get("retries"));

        List<String> retrievedPlugins = deserializedStore.getObject("settings/plugins", List.class);
        assertNotNull(retrievedPlugins);
        assertEquals(2, retrievedPlugins.size());
        assertEquals("plugin1", retrievedPlugins.get(0));
        assertEquals("plugin2", retrievedPlugins.get(1));
    }

    @Test
    public void testBackwardCompatibility() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test that existing primitive methods still work
        store.putPrimitive("test/string", "hello");
        store.putPrimitive("test/number", 42);
        store.putPrimitive("test/boolean", true);

        assertEquals("hello", store.getString("test/string", null));
        assertEquals(42, store.getInteger("test/number", null));
        assertEquals(true, store.getBoolean("test/boolean", null));

        // Test that new JSON methods work alongside primitives
        Map<String, Object> complex = new HashMap<>();
        complex.put("nested", "value");
        store.putObject("test/complex", complex);

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedComplex = store.getObject("test/complex", Map.class);
        assertEquals("value", retrievedComplex.get("nested"));
    }

    @Test
    public void testCacheReplacement() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store an object (map)
        store.putObject("test/data", JsonUtils.toJsonNode(Map.of("key", "value")));

        // First retrieval should deserialize and cache
        Map<String, Object> firstRetrieval = store.getObject("test/data", Map.class);
        assertNotNull(firstRetrieval);
        assertEquals("value", firstRetrieval.get("key"));

        // Second retrieval should return cached object
        Map<String, Object> secondRetrieval = store.getObject("test/data", Map.class);
        assertSame(firstRetrieval, secondRetrieval);

        // Verify the data map now contains the deserialized object
        Object storedValue = store.getData().get(JIPipePathMetadataStore.key("test/data"));
        assertSame(firstRetrieval, storedValue);
    }
    // Enhanced putObject() Testing

    @Test
    public void testPutObjectWithNullKey() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test that null key doesn't throw exception and doesn't store the value
        store.putObject((String) null, "test-value");

        // Verify nothing was stored
        assertEquals(0, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("test")));
    }

    @Test
    public void testPutObjectWithNullValue() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test that null value doesn't throw exception and doesn't store the value
        store.putObject("test/key", null);

        // Verify nothing was stored
        assertEquals(0, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("test/key")));
    }

    @Test
    public void testPutObjectWithCustomPOJO() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with a custom POJO object
        TestPOJO pojo = new TestPOJO("test-name", 42, true);
        store.putObject("test/pojo", pojo);

        // Verify the object was stored directly
        Object storedValue = store.getData().get(JIPipePathMetadataStore.key("test/pojo"));
        assertSame(pojo, storedValue);

        // Test retrieval
        TestPOJO retrievedPOJO = store.getObject("test/pojo", TestPOJO.class);
        assertSame(pojo, retrievedPOJO);
        assertEquals("test-name", retrievedPOJO.getName());
        assertEquals(42, retrievedPOJO.getNumber());
        assertEquals(true, retrievedPOJO.isActive());
    }

    @Test
    public void testPutObjectWithNestedComplexObjects() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Create a nested complex object structure
        Map<String, Object> nestedConfig = new HashMap<>();
        nestedConfig.put("database", new HashMap<String, Object>() {{
            put("host", "localhost");
            put("port", 5432);
            put("ssl", true);
        }});

        List<String> features = Arrays.asList("feature1", "feature2", "feature3");
        nestedConfig.put("features", features);

        TestPOJO pojo = new TestPOJO("nested-pogo", 123, false);
        nestedConfig.put("pojo", pojo);

        store.putObject("app/config", nestedConfig);

        // Verify the complex object was stored
        Object storedValue = store.getData().get(JIPipePathMetadataStore.key("app/config"));
        assertSame(nestedConfig, storedValue);

        // Test retrieval of nested objects
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedConfig = store.getObject("app/config", Map.class);
        assertNotNull(retrievedConfig);

        @SuppressWarnings("unchecked")
        Map<String, Object> dbConfig = (Map<String, Object>) retrievedConfig.get("database");
        assertEquals("localhost", dbConfig.get("host"));
        assertEquals(5432, dbConfig.get("port"));
        assertEquals(true, dbConfig.get("ssl"));

        List<String> retrievedFeatures = (List<String>) retrievedConfig.get("features");
        assertEquals(3, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));

        TestPOJO retrievedPOJO = (TestPOJO) retrievedConfig.get("pojo");
        assertEquals("nested-pogo", retrievedPOJO.getName());
        assertEquals(123, retrievedPOJO.getNumber());
        assertEquals(false, retrievedPOJO.isActive());
    }

    // Enhanced getObject() Testing

    @Test
    public void testGetObjectWithNonExistentKey() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with non-existent key - should return null
        String result = store.getObject("nonexistent/key", String.class);
        assertNull(result);

        // Test with default value using primitive method instead
        String resultWithDefault = store.getString("nonexistent/key", "default-value");
        assertEquals("default-value", resultWithDefault);
    }

    @Test
    public void testGetObjectWithTypeMismatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store a string value
        store.putPrimitive("test/value", "42");

        // Try to retrieve as Integer - fails
        Integer result = store.getObject("test/value", Integer.class);
        assertNull(result);

        // Try to retrieve as String - should work
        String stringResult = store.getObject("test/value", String.class);
        assertEquals("42", stringResult);
    }

    @Test
    public void testGetObjectWithDifferentCollectionTypes() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with Set
        Set<String> features = new HashSet<>(Arrays.asList("feature1", "feature2", "feature3"));
        store.putObject("app/features", features);

        // Retrieve as Set
        Set<String> retrievedFeatures = store.getObject("app/features", Set.class);
        assertNotNull(retrievedFeatures);
        assertEquals(3, retrievedFeatures.size());
        assertTrue(retrievedFeatures.contains("feature1"));
        assertTrue(retrievedFeatures.contains("feature2"));
        assertTrue(retrievedFeatures.contains("feature3"));

        // Test with List
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        store.putObject("app/numbers", numbers);

        // Retrieve as List
        List<Integer> retrievedNumbers = store.getObject("app/numbers", List.class);
        assertNotNull(retrievedNumbers);
        assertEquals(5, retrievedNumbers.size());
        assertEquals(1, retrievedNumbers.get(0));
        assertEquals(5, retrievedNumbers.get(4));

        // Test with custom Map
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", 123);
        store.putObject("app/config", config);

        // Retrieve as Map
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedConfig = store.getObject("app/config", Map.class);
        assertNotNull(retrievedConfig);
        assertEquals("value1", retrievedConfig.get("key1"));
        assertEquals(123, retrievedConfig.get("key2"));
    }

    // Enhanced Caching Testing

    @Test
    public void testCacheClearingOnPutObjectOperations() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store a JSON string
        store.putObject("test/data", Map.of("key", "value"));

        // First retrieval should deserialize and cache
        Map<String, Object> firstRetrieval = store.getObject("test/data", Map.class);
        assertNotNull(firstRetrieval);
        assertEquals("value", firstRetrieval.get("key"));

        // Second retrieval should return cached object
        Map<String, Object> secondRetrieval = store.getObject("test/data", Map.class);
        assertSame(firstRetrieval, secondRetrieval);

        // Now put a new object at the same path - should clear cache
        Map<String, Object> newObject = new HashMap<>();
        newObject.put("key", "new-value");
        store.putObject("test/data", newObject);

        // Third retrieval should return the new object, not cached one
        Map<String, Object> thirdRetrieval = store.getObject("test/data", Map.class);
        assertSame(newObject, thirdRetrieval);
        assertNotSame(firstRetrieval, thirdRetrieval);
        assertEquals("new-value", thirdRetrieval.get("key"));
    }

    @Test
    public void testCacheBehaviorWithMixedPrimitiveObjectOperations() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store a primitive value
        store.putPrimitive("test/primitive", "primitive-value");

        // Store a JSON object
        Map<String, Object> complexObject = new HashMap<>();
        complexObject.put("key", "complex-value");
        store.putObject("test/complex", complexObject);

        // Retrieve primitive
        String primitiveValue = store.getString("test/primitive", null);
        assertEquals("primitive-value", primitiveValue);

        // Retrieve complex object
        Map<String, Object> retrievedComplex = store.getObject("test/complex", Map.class);
        assertNotNull(retrievedComplex);
        assertEquals("complex-value", retrievedComplex.get("key"));

        // Verify they are stored as different types
        Object storedPrimitive = store.getData().get(JIPipePathMetadataStore.key("test/primitive"));
        Object storedComplex = store.getData().get(JIPipePathMetadataStore.key("test/complex"));
        assertSame(String.class, storedPrimitive.getClass());
        assertSame(HashMap.class, storedComplex.getClass());

        // Retrieve complex object again - should be cached
        Map<String, Object> cachedComplex = store.getObject("test/complex", Map.class);
        assertSame(retrievedComplex, cachedComplex);
    }

    @Test
    public void testCacheInvalidationEdgeCases() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store a JSON string
        store.putObject("test/data", Map.of("key", "value"));

        // First retrieval
        Map<String, Object> firstRetrieval = store.getObject("test/data", Map.class);
        assertNotNull(firstRetrieval);

        // Clear the entire store - should invalidate cache
        store.clear();

        // Verify the key is gone
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("test/data")));

        // Store a new value
        store.putObject("test/data", Map.of("new", "value"));

        // Second retrieval should work with new data
        Map<String, Object> secondRetrieval = store.getObject("test/data", Map.class);
        assertNotNull(secondRetrieval);
        assertEquals("value", secondRetrieval.get("new"));
        assertNotSame(firstRetrieval, secondRetrieval);

        // Clear entries with prefix
        store.clearEntriesWithPathPrefix("test");

        // Verify the key is gone
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("test/data")));
    }

    // Enhanced Integration Testing

    @Test
    public void testIntegrationWithToNestedMap() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store various types of data
        store.putPrimitive("settings/app/name", "TestApp");
        store.putPrimitive("settings/app/version", 1.0);
        store.putPrimitive("settings/app/enabled", true);

        Map<String, Object> dbConfig = new HashMap<>();
        dbConfig.put("host", "localhost");
        dbConfig.put("port", 5432);
        store.putObject("settings/database", dbConfig);

        List<String> features = Arrays.asList("feature1", "feature2");
        store.putObject("settings/features", features);

        // Convert to nested map
        Map<String, Object> nestedMap = store.toNestedMap();

        // Verify the nested structure
        assertNotNull(nestedMap);
        assertEquals(1, nestedMap.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) nestedMap.get("settings");
        assertNotNull(settings);
        assertEquals(3, settings.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> app = (Map<String, Object>) settings.get("app");
        assertNotNull(app);
        assertEquals("TestApp", app.get("name"));
        assertEquals(1.0, app.get("version"));
        assertEquals(true, app.get("enabled"));

        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) settings.get("database");
        assertNotNull(database);
        assertEquals("localhost", database.get("host"));
        assertEquals(5432, database.get("port"));

        @SuppressWarnings("unchecked")
        List<String> retrievedFeatures = (List<String>) settings.get("features");
        assertNotNull(retrievedFeatures);
        assertEquals(2, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
    }

    @Test
    public void testHierarchicalPathOperationsWithComplexObjects() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Create a hierarchical structure with complex objects
        Map<String, Object> level1 = new HashMap<>();
        level1.put("name", "level1");

        Map<String, Object> level2 = new HashMap<>();
        level2.put("name", "level2");
        level2.put("data", Arrays.asList(1, 2, 3));

        Map<String, Object> level3 = new HashMap<>();
        level3.put("name", "level3");
        level3.put("pojo", new TestPOJO("level3-pogo", 999, true));

        level2.put("nested", level3);
        level1.put("child", level2);

        store.putObject("hierarchy/level1", level1);

        // Test hierarchical retrieval
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedLevel1 = store.getObject("hierarchy/level1", Map.class);
        assertNotNull(retrievedLevel1);
        assertEquals("level1", retrievedLevel1.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedLevel2 = (Map<String, Object>) retrievedLevel1.get("child");
        assertNotNull(retrievedLevel2);
        assertEquals("level2", retrievedLevel2.get("name"));

        @SuppressWarnings("unchecked")
        List<Integer> retrievedData = (List<Integer>) retrievedLevel2.get("data");
        assertEquals(3, retrievedData.size());
        assertEquals(1, retrievedData.get(0));

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedLevel3 = (Map<String, Object>) retrievedLevel2.get("nested");
        assertNotNull(retrievedLevel3);
        assertEquals("level3", retrievedLevel3.get("name"));

        TestPOJO retrievedPOJO = (TestPOJO) retrievedLevel3.get("pojo");
        assertEquals("level3-pogo", retrievedPOJO.getName());
        assertEquals(999, retrievedPOJO.getNumber());
        assertEquals(true, retrievedPOJO.isActive());

        // Test getEntriesUnderPath
        Map<JIPipePathMetadataStore.Key, Object> hierarchyEntries = store.getEntriesUnderPath("hierarchy");
        assertEquals(1, hierarchyEntries.size());
        assertTrue(hierarchyEntries.containsKey(JIPipePathMetadataStore.key("hierarchy/level1")));
    }

    @Test
    public void testMalformedJsonHandling() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Store malformed JSON string
        store.putPrimitive("malformed/data", "{\"invalid\": json}");

        // Attempt to retrieve as Map - should return null
        Map<String, Object> result = store.getObject("malformed/data", Map.class);
        assertNull(result);

        // Verify the malformed JSON is still stored as string
        Object storedValue = store.getData().get(JIPipePathMetadataStore.key("malformed/data"));
        assertEquals("{\"invalid\": json}", storedValue);

        // Store another valid object
        Map<String, Object> validObject = new HashMap<>();
        validObject.put("key", "value");
        store.putObject("valid/data", validObject);

        // Verify valid object still works
        @SuppressWarnings("unchecked")
        Map<String, Object> validResult = store.getObject("valid/data", Map.class);
        assertNotNull(validResult);
        assertEquals("value", validResult.get("key"));
    }

    @Test
    public void testConcurrentAccessScenarios() throws InterruptedException {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Pre-populate with some data
        store.putPrimitive("shared/value", "initial");
        Map<String, Object> complexObject = new HashMap<>();
        complexObject.put("data", "test");
        store.putObject("shared/complex", complexObject);

        // Create multiple threads to access the store
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // Read operations
                    String value = store.getString("shared/value", null);
                    assertEquals("initial", value);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> complex = store.getObject("shared/complex", Map.class);
                    assertEquals("test", complex.get("data"));

                    // Write operations
                    Map<String, Object> newObject = new HashMap<>();
                    newObject.put("thread", threadId);
                    store.putObject("shared/thread-" + threadId, newObject);

                    // Verify write
                    @SuppressWarnings("unchecked")
                    Map<String, Object> retrieved = store.getObject("shared/thread-" + threadId, Map.class);
                    assertEquals(threadId, retrieved.get("thread"));

                } catch (Exception e) {
                    exceptions[threadId] = e;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Check for exceptions
        for (Exception exception : exceptions) {
            assertNull(exception, "Thread encountered an exception: " + exception);
        }

        // Verify all writes succeeded
        for (int i = 0; i < threadCount; i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> retrieved = store.getObject("shared/thread-" + i, Map.class);
            assertEquals(i, retrieved.get("thread"));
        }
    }

    @Test
    public void testBoundaryConditions() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Test with empty string paths
        store.putObject("", "empty-path-value");
        assertEquals("empty-path-value", store.getObject("", String.class));

        // Test with single character paths
        store.putObject("a", "single-char");
        assertEquals("single-char", store.getObject("a", String.class));

        // Test with very long paths
        String longPath = "a/".repeat(100) + "end";
        store.putObject(longPath, "long-path-value");
        assertEquals("long-path-value", store.getObject(longPath, String.class));

        // Test with special characters in paths
        store.putObject("special/chars/with@#$%", "special-value");
        assertEquals("special-value", store.getObject("special/chars/with@#$%", String.class));

        // Test with large objects
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        store.putObject("large/list", largeList);

        List<String> retrievedLargeList = store.getObject("large/list", List.class);
        assertEquals(1000, retrievedLargeList.size());
        assertEquals("item-0", retrievedLargeList.get(0));
        assertEquals("item-999", retrievedLargeList.get(999));

        // Test with deeply nested objects
        Map<String, Object> deepObject = new HashMap<>();
        Map<String, Object> current = deepObject;
        for (int i = 0; i < 50; i++) {
            Map<String, Object> next = new HashMap<>();
            next.put("level", i);
            current.put("nested", next);
            current = next;
        }
        store.putObject("deep/nested", deepObject);

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedDeep = store.getObject("deep/nested", Map.class);
        assertNotNull(retrievedDeep);

        // Verify the store handles all these cases without errors
        assertTrue(store.size() > 0);
    }

    // Helper class for testing custom POJO objects
    public static class TestPOJO {
        private String name;
        private int number;
        private boolean active;

        public TestPOJO() {
        }

        public TestPOJO(String name, int number, boolean active) {
            this.name = name;
            this.number = number;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestPOJO testPOJO = (TestPOJO) o;
            return number == testPOJO.number &&
                    active == testPOJO.active &&
                    Objects.equals(name, testPOJO.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, number, active);
        }
    }
}