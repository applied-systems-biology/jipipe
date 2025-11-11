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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PathMetadataStorePathPrefixTest {

    @Test
    void testClearEntriesWithPathPrefix() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("settings/ui/theme"), "dark");
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);
        store.putPrimitive(JIPipePathMetadataStore.key("other/setting"), "value");

        // Verify initial state
        assertEquals(5, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("other/setting")));

        // Clear all settings entries
        store.clearEntriesWithPathPrefix("settings");

        // Verify that settings entries are removed
        assertEquals(2, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/version")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/ui/theme")));

        // Verify other entries remain
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("other/setting")));
    }

    @Test
    void testClearEntriesWithPathPrefixWithTrailingSlash() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Clear with trailing slash
        store.clearEntriesWithPathPrefix("settings/");

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/version")));

        // Verify other entries remain
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixNoMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Try to clear non-existent prefix
        store.clearEntriesWithPathPrefix("nonexistent");

        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixNullString() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");

        // Try to clear with null prefix (string version)
        store.clearEntriesWithPathPrefix((String) null);

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixEmpty() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");

        // Try to clear with empty prefix
        store.clearEntriesWithPathPrefix("");

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixExactMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings"), "root");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general"), "general");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/name"), "name");

        // Clear exact match
        store.clearEntriesWithPathPrefix("settings");

        // Verify exact match and all children are removed
        assertEquals(0, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixDeepHierarchy() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data with deep hierarchy
        store.putPrimitive(JIPipePathMetadataStore.key("a/b/c/d"), "deep");
        store.putPrimitive(JIPipePathMetadataStore.key("a/b/c/e"), "deep2");
        store.putPrimitive(JIPipePathMetadataStore.key("a/b/f"), "shallow");
        store.putPrimitive(JIPipePathMetadataStore.key("a/g"), "other");
        store.putPrimitive(JIPipePathMetadataStore.key("h"), "top");

        // Clear intermediate level
        store.clearEntriesWithPathPrefix("a/b");

        // Verify only cleared entries are removed
        assertEquals(2, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("a/b/c/d")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("a/b/c/e")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("a/b/f")));

        // Verify other entries remain
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("a/g")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("h")));
    }

    @Test
    void testClearEntriesWithPathPrefixCaseSensitive() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("Settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);

        // Clear lowercase settings - should only remove lowercase entries
        store.clearEntriesWithPathPrefix("settings");

        // Verify only lowercase entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/version")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("Settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathObject() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("settings/ui/theme"), "dark");
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Verify initial state
        assertEquals(4, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));

        // Clear all settings entries using Path object
        store.clearEntriesWithPathPrefix(JIPipePathMetadataStore.key("settings"));

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/version")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/ui/theme")));

        // Verify other entries remain
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathWithTrailingSlash() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Clear with trailing slash using Path object
        store.clearEntriesWithPathPrefix(JIPipePathMetadataStore.key("settings/"));

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertFalse(store.containsKey(JIPipePathMetadataStore.key("settings/general/version")));

        // Verify other entries remain
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathNoMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Try to clear non-existent prefix using Path object
        store.clearEntriesWithPathPrefix(JIPipePathMetadataStore.key("nonexistent"));

        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathNull() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Application");

        // Try to clear with null prefix using Path object
        store.clearEntriesWithPathPrefix((JIPipePathMetadataStore.Key) null);

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(JIPipePathMetadataStore.key("settings/general/name")));
    }

    @Test
    void testPutPrimitiveAll() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up initial data
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "Original Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("features/enhanced"), true);

        // Create another store with some overlapping and new data
        JIPipePathMetadataStore other = new JIPipePathMetadataStore();
        other.putPrimitive(JIPipePathMetadataStore.key("settings/general/name"), "New Application"); // Override existing
        other.putPrimitive(JIPipePathMetadataStore.key("settings/general/debug"), true); // New entry
        other.putPrimitive(JIPipePathMetadataStore.key("new/feature"), "added"); // New entry

        // Put all entries from other store
        store.putAll(other);

        // Verify sizes
        assertEquals(5, store.size());

        // Verify overridden entry
        assertEquals("New Application", store.getString(JIPipePathMetadataStore.key("settings/general/name"), "Default"));

        // Verify existing entries that weren't overridden
        assertEquals(1.0, store.getDouble(JIPipePathMetadataStore.key("settings/general/version"), 0.0));
        assertEquals(true, store.getBoolean(JIPipePathMetadataStore.key("features/enhanced"), false));

        // Verify new entries from other store
        assertEquals(true, store.getBoolean(JIPipePathMetadataStore.key("settings/general/debug"), false));
        assertEquals("added", store.getString(JIPipePathMetadataStore.key("new/feature"), "default"));
    }

    @Test
    void testPutPrimitiveAllNull() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();
        store.putPrimitive(JIPipePathMetadataStore.key("test/key"), "value");

        // Put with null should not change anything
        store.putAll(null);

        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(JIPipePathMetadataStore.key("test/key"), "default"));
    }

    @Test
    void testPutPrimitiveAllEmpty() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();
        store.putPrimitive(JIPipePathMetadataStore.key("test/key"), "value");

        JIPipePathMetadataStore empty = new JIPipePathMetadataStore();

        // Put with empty store should not change anything
        store.putAll(empty);

        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(JIPipePathMetadataStore.key("test/key"), "default"));
    }

    @Test
    void testJsonSerializationWithBackslashes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JIPipePathMetadataStore original = new JIPipePathMetadataStore();

        // Add paths with backslashes
        original.putPrimitive(JIPipePathMetadataStore.key("settings\\general\\name"), "Application");
        original.putPrimitive(JIPipePathMetadataStore.key("settings\\general\\version"), 1.0);

        // Serialize to JSON
        String json = mapper.writeValueAsString(original);

        // Verify JSON contains normalized paths (forward slashes)
        assertTrue(json.contains("\"settings/general/name\""));
        assertTrue(json.contains("\"settings/general/version\""));
        assertFalse(json.contains("\\\\"));

        // Deserialize back to store
        JIPipePathMetadataStore deserialized = mapper.readValue(json, JIPipePathMetadataStore.class);

        // Verify deserialized data
        assertEquals(2, deserialized.size());
        assertEquals("Application", deserialized.getString(JIPipePathMetadataStore.key("settings/general/name"), "Default"));
        assertEquals(1.0, deserialized.getDouble(JIPipePathMetadataStore.key("settings/general/version"), 0.0));
    }

    @Test
    void testJsonDeserializationWithBackslashes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create JSON with backslash paths (simulating old Windows format)
        String jsonWithBackslashes = "{\"settings\\\\general\\\\name\":\"Application\",\"settings\\\\general\\\\version\":1.0}";

        // Deserialize - should handle backslashes gracefully
        JIPipePathMetadataStore store = mapper.readValue(jsonWithBackslashes, JIPipePathMetadataStore.class);

        // Verify store has normalized paths
        assertEquals(2, store.size());
        assertEquals("Application", store.getString(JIPipePathMetadataStore.key("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(JIPipePathMetadataStore.key("settings/general/version"), 0.0));
    }

    @Test
    void testToNestedMapWithBackslashes() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Add paths with backslashes
        store.putPrimitive(JIPipePathMetadataStore.key("settings\\general\\name"), "Application");
        store.putPrimitive(JIPipePathMetadataStore.key("settings\\general\\version"), 1.0);
        store.putPrimitive(JIPipePathMetadataStore.key("ui\\theme"), "dark");

        // Convert to nested map
        Map<String, Object> nested = store.toNestedMap();

        // Verify structure is correct with forward slashes
        assertTrue(nested.containsKey("settings"));
        assertTrue(nested.containsKey("ui"));

        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) nested.get("settings");
        assertTrue(settings.containsKey("general"));

        @SuppressWarnings("unchecked")
        Map<String, Object> general = (Map<String, Object>) settings.get("general");
        assertEquals("Application", general.get("name"));
        assertEquals(1.0, general.get("version"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ui = (Map<String, Object>) nested.get("ui");
        assertEquals("dark", ui.get("theme"));
    }

    @Test
    void testMixedPathSeparatorsInStore() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Add paths with mixed separators
        store.putPrimitive(JIPipePathMetadataStore.key("settings\\general\\name"), "Application");  // Backslashes
        store.putPrimitive(JIPipePathMetadataStore.key("settings/general/version"), 1.0);          // Forward slashes
        store.putPrimitive(JIPipePathMetadataStore.key("ui\\theme"), "dark");                     // Backslashes

        // Before migration, backslash paths should not be found with forward slash paths
        assertEquals("Application", store.getString(JIPipePathMetadataStore.key("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(JIPipePathMetadataStore.key("settings/general/version"), 0.0));
        assertEquals("dark", store.getString(JIPipePathMetadataStore.key("ui/theme"), "light"));

        // Now all paths should work with forward slashes
        assertEquals("Application", store.getString(JIPipePathMetadataStore.key("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(JIPipePathMetadataStore.key("settings/general/version"), 0.0));
        assertEquals("dark", store.getString(JIPipePathMetadataStore.key("ui/theme"), "light"));
    }
}