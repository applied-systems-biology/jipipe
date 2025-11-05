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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for getObject and putObject methods with primitive values in PathMetadataStore.
 * This class verifies that when primitives are put using putObject(), they can be retrieved
 * correctly using getObject() and the behavior matches the corresponding primitive-specific
 * getter methods (getBoolean, getDouble, getInteger, getString).
 */
public class PathMetadataStorePrimitiveObjectTest {

    @Test
    public void testPutObjectAndGetBooleanWithPathKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject
        Path key = Paths.get("test/boolean");
        store.putObject(key, true);

        Boolean result = store.getObject(key, Boolean.class);
        assertNotNull(result);
        assertTrue(result);

        // Verify behavior matches getBoolean method
        Boolean primitiveResult = store.getBoolean(key, false);
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectAndGetBooleanWithStringKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject using string key
        store.putObject("test/boolean", false);

        Boolean result = store.getObject("test/boolean", Boolean.class);
        assertNotNull(result);
        assertFalse(result);

        // Verify behavior matches getBoolean method
        Boolean primitiveResult = store.getBoolean("test/boolean", true);
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectAndGetIntegerWithPathKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject
        Path key = Paths.get("test/integer");
        store.putObject(key, 42);

        Integer result = store.getObject(key, Integer.class);
        assertNotNull(result);
        assertEquals(42, result);

        // Verify behavior matches getInteger method
        Integer primitiveResult = store.getInteger(key, 0);
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectAndGetIntegerWithStringKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject using string key
        store.putObject("test/integer", 123);

        Integer result = store.getObject("test/integer", Integer.class);
        assertNotNull(result);
        assertEquals(123, result);

        // Verify behavior matches getInteger method
        Integer primitiveResult = store.getInteger("test/integer", 0);
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectAndGetDoubleWithPathKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject
        Path key = Paths.get("test/double");
        store.putObject(key, 3.14);

        Double result = store.getObject(key, Double.class);
        assertNotNull(result);
        assertEquals(3.14, result, 0.001);

        // Verify behavior matches getDouble method
        Double primitiveResult = store.getDouble(key, 0.0);
        assertEquals(result, primitiveResult, 0.001);
    }

    @Test
    public void testPutObjectAndGetDoubleWithStringKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject using string key
        store.putObject("test/double", 2.718);

        Double result = store.getObject("test/double", Double.class);
        assertNotNull(result);
        assertEquals(2.718, result, 0.001);

        // Verify behavior matches getDouble method
        Double primitiveResult = store.getDouble("test/double", 0.0);
        assertEquals(result, primitiveResult, 0.001);
    }

    @Test
    public void testPutObjectGetStringWithPathKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject
        Path key = Paths.get("test/string");
        store.putObject(key, "test-value");

        String result = store.getObject(key, String.class);
        assertNotNull(result);
        assertEquals("test-value", result);

        // Verify behavior matches getString method
        String primitiveResult = store.getString(key, "default");
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectGetStringWithStringKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with putObject and getObject using string key
        store.putObject("test/string", "hello-world");

        String result = store.getObject("test/string", String.class);
        assertNotNull(result);
        assertEquals("hello-world", result);

        // Verify behavior matches getString method
        String primitiveResult = store.getString("test/string", "default");
        assertEquals(result, primitiveResult);
    }

    @Test
    public void testPutObjectAndGetBooleanWithDifferentValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test various boolean values
        store.putObject("test/true", true);
        store.putObject("test/false", false);

        assertEquals(true, store.getObject("test/true", Boolean.class));
        assertEquals(false, store.getObject("test/false", Boolean.class));

        // Verify behavior matches primitive methods
        assertEquals(true, store.getBoolean("test/true", false));
        assertEquals(false, store.getBoolean("test/false", true));
    }

    @Test
    public void testPutObjectAndGetIntegerWithDifferentValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test various integer values
        store.putObject("test/zero", 0);
        store.putObject("test/positive", 100);
        store.putObject("test/negative", -50);
        store.putObject("test/max", Integer.MAX_VALUE);
        store.putObject("test/min", Integer.MIN_VALUE);

        assertEquals(0, store.getObject("test/zero", Integer.class));
        assertEquals(100, store.getObject("test/positive", Integer.class));
        assertEquals(-50, store.getObject("test/negative", Integer.class));
        assertEquals(Integer.MAX_VALUE, store.getObject("test/max", Integer.class));
        assertEquals(Integer.MIN_VALUE, store.getObject("test/min", Integer.class));

        // Verify behavior matches primitive methods
        assertEquals(0, store.getInteger("test/zero", 1));
        assertEquals(100, store.getInteger("test/positive", 0));
        assertEquals(-50, store.getInteger("test/negative", 0));
        assertEquals(Integer.MAX_VALUE, store.getInteger("test/max", 0));
        assertEquals(Integer.MIN_VALUE, store.getInteger("test/min", 0));
    }

    @Test
    public void testPutObjectAndGetDoubleWithDifferentValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test various double values
        store.putObject("test/zero", 0.0);
        store.putObject("test/positive", 3.14159);
        store.putObject("test/negative", -2.718);
        store.putObject("test/max", Double.MAX_VALUE);
        store.putObject("test/min", Double.MIN_VALUE);
        store.putObject("test/pi", Math.PI);
        store.putObject("test/e", Math.E);

        assertEquals(0.0, store.getObject("test/zero", Double.class), 0.001);
        assertEquals(3.14159, store.getObject("test/positive", Double.class), 0.001);
        assertEquals(-2.718, store.getObject("test/negative", Double.class), 0.001);
        assertEquals(Double.MAX_VALUE, store.getObject("test/max", Double.class), 0.001);
        assertEquals(Double.MIN_VALUE, store.getObject("test/min", Double.class), 0.001);
        assertEquals(Math.PI, store.getObject("test/pi", Double.class), 0.001);
        assertEquals(Math.E, store.getObject("test/e", Double.class), 0.001);

        // Verify behavior matches primitive methods
        assertEquals(0.0, store.getDouble("test/zero", 1.0), 0.001);
        assertEquals(3.14159, store.getDouble("test/positive", 0.0), 0.001);
        assertEquals(-2.718, store.getDouble("test/negative", 0.0), 0.001);
        assertEquals(Double.MAX_VALUE, store.getDouble("test/max", 0.0), 0.001);
        assertEquals(Double.MIN_VALUE, store.getDouble("test/min", 0.0), 0.001);
        assertEquals(Math.PI, store.getDouble("test/pi", 0.0), 0.001);
        assertEquals(Math.E, store.getDouble("test/e", 0.0), 0.001);
    }

    @Test
    public void testPutObjectGetStringWithDifferentValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test various string values
        store.putObject("test/empty", "");
        store.putObject("test/whitespace", "   ");
        store.putObject("test/special", "!@#$%^&*()");
        store.putObject("test/unicode", "Hello 世界");
        store.putObject("test/long", "a".repeat(1000));
        store.putObject("test/numbers", "12345");
        store.putObject("test/mixed", "abc123!@#");

        assertEquals("", store.getObject("test/empty", String.class));
        assertEquals("   ", store.getObject("test/whitespace", String.class));
        assertEquals("!@#$%^&*()", store.getObject("test/special", String.class));
        assertEquals("Hello 世界", store.getObject("test/unicode", String.class));
        assertEquals("a".repeat(1000), store.getObject("test/long", String.class));
        assertEquals("12345", store.getObject("test/numbers", String.class));
        assertEquals("abc123!@#", store.getObject("test/mixed", String.class));

        // Verify behavior matches primitive methods
        assertEquals("", store.getString("test/empty", "default"));
        assertEquals("   ", store.getString("test/whitespace", "default"));
        assertEquals("!@#$%^&*()", store.getString("test/special", "default"));
        assertEquals("Hello 世界", store.getString("test/unicode", "default"));
        assertEquals("a".repeat(1000), store.getString("test/long", "default"));
        assertEquals("12345", store.getString("test/numbers", "default"));
        assertEquals("abc123!@#", store.getString("test/mixed", "default"));
    }

    @Test
    public void testPutObjectAndGetBooleanWithNullValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null values are not stored
        store.putObject("test/boolean", null);

        assertNull(store.getObject("test/boolean", Boolean.class));
        assertFalse(store.containsKey(Paths.get("test/boolean")));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetIntegerWithNullValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null values are not stored
        store.putObject("test/integer", null);

        assertNull(store.getObject("test/integer", Integer.class));
        assertFalse(store.containsKey(Paths.get("test/integer")));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetDoubleWithNullValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null values are not stored
        store.putObject("test/double", null);

        assertNull(store.getObject("test/double", Double.class));
        assertFalse(store.containsKey(Paths.get("test/double")));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectGetStringWithNullValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null values are not stored
        store.putObject("test/string", null);

        assertNull(store.getObject("test/string", String.class));
        assertFalse(store.containsKey(Paths.get("test/string")));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetBooleanWithNullKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null key is not stored
        store.putObject((Path) null, true);

        assertNull(store.getObject((Path) null, Boolean.class));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetIntegerWithNullKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null key is not stored
        store.putObject((Path) null, 42);

        assertNull(store.getObject((Path) null, Integer.class));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetDoubleWithNullKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null key is not stored
        store.putObject((Path) null, 3.14);

        assertNull(store.getObject((Path) null, Double.class));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectGetStringWithNullKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test that null key is not stored
        store.putObject((Path) null, "test");

        assertNull(store.getObject((Path) null, String.class));
        assertEquals(0, store.size());
    }

    @Test
    public void testPutObjectAndGetBooleanWithNullType() {
        PathMetadataStore store = new PathMetadataStore();

        store.putObject("test/boolean", true);

        // Test that null type returns null
        assertNull(store.getObject("test/boolean", null));
    }

    @Test
    public void testPutObjectAndGetIntegerWithNullType() {
        PathMetadataStore store = new PathMetadataStore();

        store.putObject("test/integer", 42);

        // Test that null type returns null
        assertNull(store.getObject("test/integer", null));
    }

    @Test
    public void testPutObjectAndGetDoubleWithNullType() {
        PathMetadataStore store = new PathMetadataStore();

        store.putObject("test/double", 3.14);

        // Test that null type returns null
        assertNull(store.getObject("test/double", null));
    }

    @Test
    public void testPutObjectGetStringWithNullType() {
        PathMetadataStore store = new PathMetadataStore();

        store.putObject("test/string", "test");

        // Test that null type returns null
        assertNull(store.getObject("test/string", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithNonExistentKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with non-existent key
        assertNull(store.getObject("nonexistent/boolean", Boolean.class));

        // Verify primitive method also returns null (or default)
        assertNull(store.getBoolean("nonexistent/boolean", null));
    }

    @Test
    public void testPutObjectAndGetIntegerWithNonExistentKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with non-existent key
        assertNull(store.getObject("nonexistent/integer", Integer.class));

        // Verify primitive method also returns null (or default)
        assertNull(store.getInteger("nonexistent/integer", null));
    }

    @Test
    public void testPutObjectAndGetDoubleWithNonExistentKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with non-existent key
        assertNull(store.getObject("nonexistent/double", Double.class));

        // Verify primitive method also returns null (or default)
        assertNull(store.getDouble("nonexistent/double", null));
    }

    @Test
    public void testPutObjectGetStringWithNonExistentKey() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with non-existent key
        assertNull(store.getObject("nonexistent/string", String.class));

        // Verify primitive method also returns null (or default)
        assertNull(store.getString("nonexistent/string", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithWrongType() {
        PathMetadataStore store = new PathMetadataStore();

        // Store a string value
        store.putObject("test/wrong", "true");

        // Try to retrieve as Boolean - should return null (no conversion from String to Boolean)
        assertNull(store.getObject("test/wrong", Boolean.class));

        // Verify primitive method also fails
        assertEquals(true, store.getBoolean("test/wrong", null));
    }

    @Test
    public void testPutObjectAndGetIntegerWithWrongType() {
        PathMetadataStore store = new PathMetadataStore();

        // Store a string value
        store.putObject("test/wrong", "42");

        // Try to retrieve as Integer - should return null (no conversion from String to Integer)
        assertNull(store.getObject("test/wrong", Integer.class));

        // Verify primitive method also fails
        assertEquals(42, store.getInteger("test/wrong", null));
    }

    @Test
    public void testPutObjectAndGetDoubleWithWrongType() {
        PathMetadataStore store = new PathMetadataStore();

        // Store a string value
        store.putObject("test/wrong", "3.14");

        // Try to retrieve as Double - should return null (no conversion from String to Double)
        assertNull(store.getObject("test/wrong", Double.class));

        // Verify primitive method also fails
        assertEquals(3.14, store.getDouble("test/wrong", null));
    }

    @Test
    public void testPutObjectGetStringWithWrongType() {
        PathMetadataStore store = new PathMetadataStore();

        // Store an integer value
        store.putObject("test/wrong", 123);

        // Try to retrieve as String - should work because String has special conversion handling
        String result = store.getObject("test/wrong", String.class);
        assertEquals("123", result);

        // Verify primitive method also works
        assertEquals("123", store.getString("test/wrong", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithMixedPrimitiveAndObjectOperations() {
        PathMetadataStore store = new PathMetadataStore();

        // Store some values using primitive methods
        store.putPrimitive("primitive/boolean", true);
        store.putPrimitive("primitive/integer", 42);
        store.putPrimitive("primitive/double", 3.14);
        store.putPrimitive("primitive/string", "primitive");

        // Store some values using object methods
        store.putObject("object/boolean", false);
        store.putObject("object/integer", 123);
        store.putObject("object/double", 2.718);
        store.putObject("object/string", "object");

        // Verify all values can be retrieved correctly
        assertEquals(true, store.getObject("primitive/boolean", Boolean.class));
        assertEquals(42, store.getObject("primitive/integer", Integer.class));
        assertEquals(3.14, store.getObject("primitive/double", Double.class), 0.001);
        assertEquals("primitive", store.getObject("primitive/string", String.class));

        assertEquals(false, store.getObject("object/boolean", Boolean.class));
        assertEquals(123, store.getObject("object/integer", Integer.class));
        assertEquals(2.718, store.getObject("object/double", Double.class), 0.001);
        assertEquals("object", store.getObject("object/string", String.class));

        // Verify primitive methods also work
        assertEquals(true, store.getBoolean("primitive/boolean", false));
        assertEquals(42, store.getInteger("primitive/integer", 0));
        assertEquals(3.14, store.getDouble("primitive/double", 0.0), 0.001);
        assertEquals("primitive", store.getString("primitive/string", null));

        assertEquals(false, store.getBoolean("object/boolean", true));
        assertEquals(123, store.getInteger("object/integer", 0));
        assertEquals(2.718, store.getDouble("object/double", 0.0), 0.001);
        assertEquals("object", store.getString("object/string", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithJsonSerialization() {
        PathMetadataStore originalStore = new PathMetadataStore();

        // Store primitive values using putObject
        originalStore.putObject("test/boolean", true);
        originalStore.putObject("test/integer", 42);
        originalStore.putObject("test/double", 3.14);
        originalStore.putObject("test/string", "test-value");

        // Serialize to JSON string
        String jsonString = JsonUtils.toJsonString(originalStore);
        assertNotNull(jsonString);

        // Deserialize from JSON string
        PathMetadataStore deserializedStore = JsonUtils.readFromString(jsonString, PathMetadataStore.class);

        // Verify values can be retrieved using getObject
        assertEquals(true, deserializedStore.getObject("test/boolean", Boolean.class));
        assertEquals(42, deserializedStore.getObject("test/integer", Integer.class));
        assertEquals(3.14, deserializedStore.getObject("test/double", Double.class), 0.001);
        assertEquals("test-value", deserializedStore.getObject("test/string", String.class));

        // Verify behavior matches primitive methods
        assertEquals(true, deserializedStore.getBoolean("test/boolean", false));
        assertEquals(42, deserializedStore.getInteger("test/integer", 0));
        assertEquals(3.14, deserializedStore.getDouble("test/double", 0.0), 0.001);
        assertEquals("test-value", deserializedStore.getString("test/string", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithCacheBehavior() {
        PathMetadataStore store = new PathMetadataStore();

        // Store a primitive value using putObject
        store.putObject("test/boolean", true);

        // First retrieval should work
        Boolean firstResult = store.getObject("test/boolean", Boolean.class);
        assertNotNull(firstResult);
        assertTrue(firstResult);

        // Second retrieval should return the same cached object
        Boolean secondResult = store.getObject("test/boolean", Boolean.class);
        assertSame(firstResult, secondResult);

        // Verify primitive method also works
        assertEquals(true, store.getBoolean("test/boolean", false));

        // Now overwrite the value
        store.putObject("test/boolean", false);

        // Third retrieval should return the new value
        Boolean thirdResult = store.getObject("test/boolean", Boolean.class);
        assertNotNull(thirdResult);
        assertFalse(thirdResult);
        assertNotSame(firstResult, thirdResult);

        // Verify primitive method also returns new value
        assertEquals(false, store.getBoolean("test/boolean", true));
    }

    @Test
    public void testPutObjectAndGetBooleanWithEmptyPaths() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with empty path
        store.putObject("", true);
        assertEquals(true, store.getObject("", Boolean.class));
        assertEquals(true, store.getBoolean("", false));

        // Test with single character path
        store.putObject("a", false);
        assertEquals(false, store.getObject("a", Boolean.class));
        assertEquals(false, store.getBoolean("a", true));

        // Test with special characters in path
        store.putObject("special/chars/with@#$%", true);
        assertEquals(true, store.getObject("special/chars/with@#$%", Boolean.class));
        assertEquals(true, store.getBoolean("special/chars/with@#$%", false));
    }

    @Test
    public void testPutObjectAndGetIntegerWithEmptyPaths() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with empty path
        store.putObject("", 0);
        assertEquals(0, store.getObject("", Integer.class));
        assertEquals(0, store.getInteger("", 1));

        // Test with single character path
        store.putObject("a", 999);
        assertEquals(999, store.getObject("a", Integer.class));
        assertEquals(999, store.getInteger("a", 0));

        // Test with special characters in path
        store.putObject("special/chars/with@#$%", -42);
        assertEquals(-42, store.getObject("special/chars/with@#$%", Integer.class));
        assertEquals(-42, store.getInteger("special/chars/with@#$%", 0));
    }

    @Test
    public void testPutObjectAndGetDoubleWithEmptyPaths() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with empty path
        store.putObject("", 0.0);
        assertEquals(0.0, store.getObject("", Double.class), 0.001);
        assertEquals(0.0, store.getDouble("", 1.0), 0.001);

        // Test with single character path
        store.putObject("a", 2.718);
        assertEquals(2.718, store.getObject("a", Double.class), 0.001);
        assertEquals(2.718, store.getDouble("a", 0.0), 0.001);

        // Test with special characters in path
        store.putObject("special/chars/with@#$%", -3.14);
        assertEquals(-3.14, store.getObject("special/chars/with@#$%", Double.class), 0.001);
        assertEquals(-3.14, store.getDouble("special/chars/with@#$%", 0.0), 0.001);
    }

    @Test
    public void testPutObjectGetStringWithEmptyPaths() {
        PathMetadataStore store = new PathMetadataStore();

        // Test with empty path
        store.putObject("", "empty");
        assertEquals("empty", store.getObject("", String.class));
        assertEquals("empty", store.getString("", "default"));

        // Test with single character path
        store.putObject("a", "single");
        assertEquals("single", store.getObject("a", String.class));
        assertEquals("single", store.getString("a", "default"));

        // Test with special characters in path
        store.putObject("special/chars/with@#$%", "special");
        assertEquals("special", store.getObject("special/chars/with@#$%", String.class));
        assertEquals("special", store.getString("special/chars/with@#$%", "default"));
    }

    @Test
    public void testPutObjectAndGetBooleanWithCrossPlatformPaths() {
        PathMetadataStore store = new PathMetadataStore();

        // Store values with Windows-style paths
        store.putObject("windows\\path\\to\\boolean", true);
        store.putObject("windows\\path\\to\\integer", 42);
        store.putObject("windows\\path\\to\\double", 3.14);
        store.putObject("windows\\path\\to\\string", "windows-value");

        // Verify paths were stored with backslashes
        assertTrue(store.hasBackslashPaths());

        // Migrate paths to forward slashes
        store.migrateBackslashPaths();

        // Verify no more backslash paths
        assertFalse(store.hasBackslashPaths());

        // Verify values can be retrieved using forward slash paths
        assertEquals(true, store.getObject("windows/path/to/boolean", Boolean.class));
        assertEquals(42, store.getObject("windows/path/to/integer", Integer.class));
        assertEquals(3.14, store.getObject("windows/path/to/double", Double.class), 0.001);
        assertEquals("windows-value", store.getObject("windows/path/to/string", String.class));

        // Verify behavior matches primitive methods
        assertEquals(true, store.getBoolean("windows/path/to/boolean", false));
        assertEquals(42, store.getInteger("windows/path/to/integer", 0));
        assertEquals(3.14, store.getDouble("windows/path/to/double", 0.0), 0.001);
        assertEquals("windows-value", store.getString("windows/path/to/string", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithComplexHierarchy() {
        PathMetadataStore store = new PathMetadataStore();

        // Create a complex hierarchy with primitive values
        store.putObject("app/settings/enabled", true);
        store.putObject("app/settings/version", 2);
        store.putObject("app/settings/price", 19.99);
        store.putObject("app/settings/name", "Test App");

        store.putObject("app/database/host", "localhost");
        store.putObject("app/database/port", 5432);
        store.putObject("app/database/timeout", 30.5);
        store.putObject("app/database/ssl", true);

        store.putObject("app/features/feature1/enabled", true);
        store.putObject("app/features/feature1/priority", 1);
        store.putObject("app/features/feature1/version", 1.2);
        store.putObject("app/features/feature1/description", "First feature");

        store.putObject("app/features/feature2/enabled", false);
        store.putObject("app/features/feature2/priority", 2);
        store.putObject("app/features/feature2/version", 2.0);
        store.putObject("app/features/feature2/description", "Second feature");

        // Verify all values can be retrieved correctly
        assertEquals(true, store.getObject("app/settings/enabled", Boolean.class));
        assertEquals(2, store.getObject("app/settings/version", Integer.class));
        assertEquals(19.99, store.getObject("app/settings/price", Double.class), 0.001);
        assertEquals("Test App", store.getObject("app/settings/name", String.class));

        assertEquals("localhost", store.getObject("app/database/host", String.class));
        assertEquals(5432, store.getObject("app/database/port", Integer.class));
        assertEquals(30.5, store.getObject("app/database/timeout", Double.class), 0.001);
        assertEquals(true, store.getObject("app/database/ssl", Boolean.class));

        assertEquals(true, store.getObject("app/features/feature1/enabled", Boolean.class));
        assertEquals(1, store.getObject("app/features/feature1/priority", Integer.class));
        assertEquals(1.2, store.getObject("app/features/feature1/version", Double.class), 0.001);
        assertEquals("First feature", store.getObject("app/features/feature1/description", String.class));

        assertEquals(false, store.getObject("app/features/feature2/enabled", Boolean.class));
        assertEquals(2, store.getObject("app/features/feature2/priority", Integer.class));
        assertEquals(2.0, store.getObject("app/features/feature2/version", Double.class), 0.001);
        assertEquals("Second feature", store.getObject("app/features/feature2/description", String.class));

        // Verify behavior matches primitive methods
        assertEquals(true, store.getBoolean("app/settings/enabled", false));
        assertEquals(2, store.getInteger("app/settings/version", 0));
        assertEquals(19.99, store.getDouble("app/settings/price", 0.0), 0.001);
        assertEquals("Test App", store.getString("app/settings/name", null));

        assertEquals("localhost", store.getString("app/database/host", null));
        assertEquals(5432, store.getInteger("app/database/port", 0));
        assertEquals(30.5, store.getDouble("app/database/timeout", 0.0), 0.001);
        assertEquals(true, store.getBoolean("app/database/ssl", false));

        assertEquals(true, store.getBoolean("app/features/feature1/enabled", false));
        assertEquals(1, store.getInteger("app/features/feature1/priority", 0));
        assertEquals(1.2, store.getDouble("app/features/feature1/version", 0.0), 0.001);
        assertEquals("First feature", store.getString("app/features/feature1/description", null));

        assertEquals(false, store.getBoolean("app/features/feature2/enabled", true));
        assertEquals(2, store.getInteger("app/features/feature2/priority", 0));
        assertEquals(2.0, store.getDouble("app/features/feature2/version", 0.0), 0.001);
        assertEquals("Second feature", store.getString("app/features/feature2/description", null));
    }

    @Test
    public void testPutObjectAndGetBooleanWithLargeNumberOfValues() {
        PathMetadataStore store = new PathMetadataStore();

        // Store a large number of primitive values
        for (int i = 0; i < 1000; i++) {
            store.putObject("test/boolean/" + i, i % 2 == 0);
            store.putObject("test/integer/" + i, i);
            store.putObject("test/double/" + i, i * 1.1);
            store.putObject("test/string/" + i, "value-" + i);
        }

        // Verify all values can be retrieved correctly
        for (int i = 0; i < 1000; i++) {
            assertEquals(i % 2 == 0, store.getObject("test/boolean/" + i, Boolean.class));
            assertEquals(i, store.getObject("test/integer/" + i, Integer.class));
            assertEquals(i * 1.1, store.getObject("test/double/" + i, Double.class), 0.001);
            assertEquals("value-" + i, store.getObject("test/string/" + i, String.class));

            // Verify behavior matches primitive methods
            assertEquals(i % 2 == 0, store.getBoolean("test/boolean/" + i, false));
            assertEquals(i, store.getInteger("test/integer/" + i, 0));
            assertEquals(i * 1.1, store.getDouble("test/double/" + i, 0.0), 0.001);
            assertEquals("value-" + i, store.getString("test/string/" + i, null));
        }

        // Verify store size
        assertEquals(4000, store.size());
    }
}