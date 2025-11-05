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
 * Tests for the getList method in PathMetadataStore
 */
public class PathMetadataStoreGetListTest {

    @Test
    public void testGetListWithStringKeyAndListValue() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list directly
        List<String> features = Arrays.asList("feature1", "feature2", "feature3");
        store.putObject("app/features", features);
        
        // Retrieve using getList method
        List<String> retrievedFeatures = store.getList("app/features", String.class);
        
        assertNotNull(retrievedFeatures);
        assertEquals(3, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        assertEquals("feature3", retrievedFeatures.get(2));
        
        // Test caching - second call should return the same cached object
        List<String> cachedFeatures = store.getList("app/features", String.class);
        assertSame(retrievedFeatures, cachedFeatures);
    }

    @Test
    public void testGetListWithPathKeyAndListValue() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list directly
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        store.putObject(Paths.get("app/numbers"), numbers);
        
        // Retrieve using getList method
        List<Integer> retrievedNumbers = store.getList(Paths.get("app/numbers"), Integer.class);
        
        assertNotNull(retrievedNumbers);
        assertEquals(5, retrievedNumbers.size());
        assertEquals(1, retrievedNumbers.get(0));
        assertEquals(2, retrievedNumbers.get(1));
        assertEquals(3, retrievedNumbers.get(2));
        assertEquals(4, retrievedNumbers.get(3));
        assertEquals(5, retrievedNumbers.get(4));
        
        // Test caching - second call should return the same cached object
        List<Integer> cachedNumbers = store.getList(Paths.get("app/numbers"), Integer.class);
        assertSame(retrievedNumbers, cachedNumbers);
    }

    @Test
    public void testGetListWithJsonNodeValue() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list as JsonNode
        store.putObject("app/features", JsonUtils.toJsonNode(Arrays.asList("feature1", "feature2", "feature3")));
        
        // Retrieve using getList method
        List<String> retrievedFeatures = store.getList("app/features", String.class);
        
        assertNotNull(retrievedFeatures);
        assertEquals(3, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        assertEquals("feature3", retrievedFeatures.get(2));
        
        // Test that the original JsonNode was replaced with deserialized list
        Object storedValue = store.getData().get(Paths.get("app/features"));
        assertSame(retrievedFeatures, storedValue);
    }

    @Test
    public void testGetListWithDifferentTypes() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Test with Integer list
        List<Integer> integers = Arrays.asList(1, 2, 3);
        store.putObject("test/integers", integers);
        List<Integer> retrievedIntegers = store.getList("test/integers", Integer.class);
        assertNotNull(retrievedIntegers);
        assertEquals(3, retrievedIntegers.size());
        assertSame(integers, retrievedIntegers);
        
        // Test with Double list
        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);
        store.putObject("test/doubles", doubles);
        List<Double> retrievedDoubles = store.getList("test/doubles", Double.class);
        assertNotNull(retrievedDoubles);
        assertEquals(3, retrievedDoubles.size());
        assertSame(doubles, retrievedDoubles);
        
        // Test with Boolean list
        List<Boolean> booleans = Arrays.asList(true, false, true);
        store.putObject("test/booleans", booleans);
        List<Boolean> retrievedBooleans = store.getList("test/booleans", Boolean.class);
        assertNotNull(retrievedBooleans);
        assertEquals(3, retrievedBooleans.size());
        assertSame(booleans, retrievedBooleans);
        
        // Test with custom object list
        List<TestObject> objects = Arrays.asList(
            new TestObject("obj1", 1),
            new TestObject("obj2", 2),
            new TestObject("obj3", 3)
        );
        store.putObject("test/objects", objects);
        List<TestObject> retrievedObjects = store.getList("test/objects", TestObject.class);
        assertNotNull(retrievedObjects);
        assertEquals(3, retrievedObjects.size());
        assertSame(objects, retrievedObjects);
    }

    @Test
    public void testGetListWithNonExistentKey() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Test with non-existent key - should return null
        List<String> result = store.getList("nonexistent/key", String.class);
        assertNull(result);
        
        // Test with Path object that doesn't exist
        List<Integer> pathResult = store.getList(Paths.get("nonexistent/path"), Integer.class);
        assertNull(pathResult);
    }

    @Test
    public void testGetListWithNullKey() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Test with null string key - should return null
        List<String> result = store.getList((String) null, String.class);
        assertNull(result);
        
        // Test with null Path key - should return null
        List<Integer> pathResult = store.getList((Path) null, Integer.class);
        assertNull(pathResult);
    }

    @Test
    public void testGetListWithNullType() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list
        List<String> features = Arrays.asList("feature1", "feature2");
        store.putObject("app/features", features);
        
        // Test with null type - should return null
        List<String> result = store.getList("app/features", null);
        assertNull(result);
    }

    @Test
    public void testGetListWithWrongType() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a string value
        store.putPrimitive("test/value", "not-a-list");
        
        // Try to retrieve as List - should return null
        List<String> result = store.getList("test/value", String.class);
        assertNull(result);
        
        // Store a non-list object
        Map<String, Object> config = Map.of("key", "value");
        store.putObject("test/config", config);
        
        // Try to retrieve as List - should return null
        List<String> listResult = store.getList("test/config", String.class);
        assertNull(listResult);
    }

    @Test
    public void testGetListWithEmptyList() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store an empty list
        List<String> emptyList = Arrays.asList();
        store.putObject("app/empty", emptyList);
        
        // Retrieve using getList method
        List<String> retrievedList = store.getList("app/empty", String.class);
        
        assertNotNull(retrievedList);
        assertEquals(0, retrievedList.size());
        assertSame(emptyList, retrievedList);
    }

    @Test
    public void testGetListWithSingleElement() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a single-element list
        List<String> singleElement = Arrays.asList("single");
        store.putObject("app/single", singleElement);
        
        // Retrieve using getList method
        List<String> retrievedList = store.getList("app/single", String.class);
        
        assertNotNull(retrievedList);
        assertEquals(1, retrievedList.size());
        assertEquals("single", retrievedList.get(0));
        assertSame(singleElement, retrievedList);
    }

    @Test
    public void testGetListCacheReplacement() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list as JsonNode
        store.putObject("test/data", JsonUtils.toJsonNode(Arrays.asList("original")));
        
        // First retrieval should deserialize and cache
        List<String> firstRetrieval = store.getList("test/data", String.class);
        assertNotNull(firstRetrieval);
        assertEquals("original", firstRetrieval.get(0));
        
        // Second retrieval should return cached object
        List<String> secondRetrieval = store.getList("test/data", String.class);
        assertSame(firstRetrieval, secondRetrieval);
        
        // Now put a new list at the same path - should clear cache
        List<String> newList = Arrays.asList("new", "value");
        store.putObject("test/data", newList);
        
        // Third retrieval should return the new list, not cached one
        List<String> thirdRetrieval = store.getList("test/data", String.class);
        assertSame(newList, thirdRetrieval);
        assertNotSame(firstRetrieval, thirdRetrieval);
        assertEquals("new", thirdRetrieval.get(0));
        assertEquals("value", thirdRetrieval.get(1));
    }

    @Test
    public void testGetListCacheInvalidation() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store a list
        List<String> originalList = Arrays.asList("original");
        store.putObject("test/data", originalList);
        
        // First retrieval
        List<String> firstRetrieval = store.getList("test/data", String.class);
        assertNotNull(firstRetrieval);
        assertSame(originalList, firstRetrieval);
        
        // Clear the entire store - should invalidate cache
        store.clear();
        
        // Verify the key is gone
        assertFalse(store.containsKey(Paths.get("test/data")));
        
        // Store a new value
        List<String> newList = Arrays.asList("new");
        store.putObject("test/data", newList);
        
        // Second retrieval should work with new data
        List<String> secondRetrieval = store.getList("test/data", String.class);
        assertNotNull(secondRetrieval);
        assertEquals("new", secondRetrieval.get(0));
        assertSame(newList, secondRetrieval);
        assertNotSame(originalList, secondRetrieval);
        
        // Clear entries with prefix
        store.clearEntriesWithPathPrefix("test");
        
        // Verify the key is gone
        assertFalse(store.containsKey(Paths.get("test/data")));
    }

    @Test
    public void testGetListIntegrationWithOtherMethods() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store various types of data
        store.putPrimitive("settings/app/name", "TestApp");
        store.putPrimitive("settings/app/version", 1.0);
        store.putPrimitive("settings/app/enabled", true);
        
        List<String> features = Arrays.asList("feature1", "feature2");
        store.putObject("settings/features", features);
        
        Map<String, Object> config = Map.of("key", "value");
        store.putObject("settings/config", config);
        
        // Verify other methods still work
        assertEquals("TestApp", store.getString("settings/app/name", null));
        assertEquals(1.0, store.getDouble("settings/app/version", null));
        assertEquals(true, store.getBoolean("settings/app/enabled", null));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedConfig = store.getObject("settings/config", Map.class);
        assertNotNull(retrievedConfig);
        assertEquals("value", retrievedConfig.get("key"));
        
        // Verify getList works alongside other methods
        List<String> retrievedFeatures = store.getList("settings/features", String.class);
        assertNotNull(retrievedFeatures);
        assertEquals(2, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        
        // Verify getEntriesUnderPath works
        Map<Path, Object> settingsEntries = store.getEntriesUnderPath("settings");
        assertEquals(5, settingsEntries.size());
        assertTrue(settingsEntries.containsKey(Paths.get("settings/app/name")));
        assertTrue(settingsEntries.containsKey(Paths.get("settings/app/version")));
        assertTrue(settingsEntries.containsKey(Paths.get("settings/app/enabled")));
        assertTrue(settingsEntries.containsKey(Paths.get("settings/features")));
        assertTrue(settingsEntries.containsKey(Paths.get("settings/config")));
    }

    @Test
    public void testGetListJsonSerializationDeserialization() {
        PathMetadataStore originalStore = new PathMetadataStore();
        
        // Add various types of data including lists
        originalStore.putPrimitive("settings/app/name", "TestApp");
        originalStore.putPrimitive("settings/app/version", 1.0);
        
        List<String> features = Arrays.asList("feature1", "feature2", "feature3");
        originalStore.putObject("settings/features", features);
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        originalStore.putObject("settings/numbers", numbers);
        
        // Serialize to JSON string
        String jsonString = JsonUtils.toJsonString(originalStore);
        assertNotNull(jsonString);
        
        // Deserialize from JSON string
        PathMetadataStore deserializedStore = JsonUtils.readFromString(jsonString, PathMetadataStore.class);
        
        // Verify primitive values
        assertEquals("TestApp", deserializedStore.getString("settings/app/name", null));
        assertEquals(1.0, deserializedStore.getDouble("settings/app/version", null));
        
        // Verify list values using getList method
        List<String> retrievedFeatures = deserializedStore.getList("settings/features", String.class);
        assertNotNull(retrievedFeatures);
        assertEquals(3, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        assertEquals("feature3", retrievedFeatures.get(2));
        
        List<Integer> retrievedNumbers = deserializedStore.getList("settings/numbers", Integer.class);
        assertNotNull(retrievedNumbers);
        assertEquals(5, retrievedNumbers.size());
        assertEquals(1, retrievedNumbers.get(0));
        assertEquals(2, retrievedNumbers.get(1));
        assertEquals(3, retrievedNumbers.get(2));
        assertEquals(4, retrievedNumbers.get(3));
        assertEquals(5, retrievedNumbers.get(4));
    }

    @Test
    public void testGetListWithCrossPlatformPaths() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Store lists with Windows-style paths
        List<String> windowsFeatures = Arrays.asList("feature1", "feature2");
        store.putObject("windows\\path\\to\\features", windowsFeatures);
        
        List<Integer> windowsNumbers = Arrays.asList(1, 2, 3);
        store.putObject("windows\\path\\to\\numbers", windowsNumbers);
        
        // Verify the paths were stored with backslashes
        assertTrue(store.hasBackslashPaths());
        assertTrue(store.containsKey(Paths.get("windows\\path\\to\\features")));
        assertTrue(store.containsKey(Paths.get("windows\\path\\to\\numbers")));
        
        // Migrate paths to forward slashes
        store.migrateBackslashPaths();
        
        // Verify no more backslash paths
        assertFalse(store.hasBackslashPaths());
        assertFalse(store.containsKey(Paths.get("windows\\path\\to\\features")));
        assertFalse(store.containsKey(Paths.get("windows\\path\\to\\numbers")));
        assertTrue(store.containsKey(Paths.get("windows/path/to/features")));
        assertTrue(store.containsKey(Paths.get("windows/path/to/numbers")));
        
        // Verify the lists are still accessible
        List<String> retrievedFeatures = store.getList("windows/path/to/features", String.class);
        assertNotNull(retrievedFeatures);
        assertEquals(2, retrievedFeatures.size());
        assertEquals("feature1", retrievedFeatures.get(0));
        assertEquals("feature2", retrievedFeatures.get(1));
        
        List<Integer> retrievedNumbers = store.getList("windows/path/to/numbers", Integer.class);
        assertNotNull(retrievedNumbers);
        assertEquals(3, retrievedNumbers.size());
        assertEquals(1, retrievedNumbers.get(0));
        assertEquals(2, retrievedNumbers.get(1));
        assertEquals(3, retrievedNumbers.get(2));
    }

    @Test
    public void testGetListWithEmptyStore() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Test with empty store
        List<String> result = store.getList("any/path", String.class);
        assertNull(result);
        
        List<Integer> intResult = store.getList(Paths.get("any/path"), Integer.class);
        assertNull(intResult);
        
        // Verify store is still empty
        assertEquals(0, store.size());
    }

    @Test
    public void testGetListWithComplexNestedLists() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Create a complex nested structure with lists
        List<Object> complexList = Arrays.asList(
            Map.of("name", "item1", "values", Arrays.asList(1, 2, 3)),
            Map.of("name", "item2", "values", Arrays.asList(4, 5, 6))
        );
        
        store.putObject("app/complex", complexList);
        
        // Retrieve the complex list
        @SuppressWarnings("unchecked")
        List<Object> retrievedList = store.getList("app/complex", Object.class);
        
        assertNotNull(retrievedList);
        assertEquals(2, retrievedList.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> item1 = (Map<String, Object>) retrievedList.get(0);
        assertEquals("item1", item1.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Integer> values1 = (List<Integer>) item1.get("values");
        assertEquals(3, values1.size());
        assertEquals(1, values1.get(0));
        assertEquals(2, values1.get(1));
        assertEquals(3, values1.get(2));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> item2 = (Map<String, Object>) retrievedList.get(1);
        assertEquals("item2", item2.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Integer> values2 = (List<Integer>) item2.get("values");
        assertEquals(3, values2.size());
        assertEquals(4, values2.get(0));
        assertEquals(5, values2.get(1));
        assertEquals(6, values2.get(2));
    }

    @Test
    public void testGetListWithLargeLists() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Test with large list
        List<String> largeList = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        
        store.putObject("app/large", largeList);
        
        // Retrieve the large list
        List<String> retrievedList = store.getList("app/large", String.class);
        
        assertNotNull(retrievedList);
        assertEquals(1000, retrievedList.size());
        assertEquals("item-0", retrievedList.get(0));
        assertEquals("item-999", retrievedList.get(999));
        
        // Test caching - should return same object
        List<String> cachedList = store.getList("app/large", String.class);
        assertSame(largeList, cachedList);
    }

    // Helper class for testing custom objects in lists
    public static class TestObject {
        private String name;
        private int number;

        public TestObject() {
        }

        public TestObject(String name, int number) {
            this.name = name;
            this.number = number;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject testObject = (TestObject) o;
            return number == testObject.number &&
                    java.util.Objects.equals(name, testObject.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, number);
        }
    }
}