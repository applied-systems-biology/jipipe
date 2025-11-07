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
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);
        store.putPrimitive(Paths.get("settings/ui/theme"), "dark");
        store.putPrimitive(Paths.get("features/enhanced"), true);
        store.putPrimitive(Paths.get("other/setting"), "value");

        // Verify initial state
        assertEquals(5, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
        assertTrue(store.containsKey(Paths.get("other/setting")));

        // Clear all settings entries
        store.clearEntriesWithPathPrefix("settings");

        // Verify that settings entries are removed
        assertEquals(2, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/name")));
        assertFalse(store.containsKey(Paths.get("settings/general/version")));
        assertFalse(store.containsKey(Paths.get("settings/ui/theme")));

        // Verify other entries remain
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
        assertTrue(store.containsKey(Paths.get("other/setting")));
    }

    @Test
    void testClearEntriesWithPathPrefixWithTrailingSlash() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Clear with trailing slash
        store.clearEntriesWithPathPrefix("settings/");

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/name")));
        assertFalse(store.containsKey(Paths.get("settings/general/version")));

        // Verify other entries remain
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixNoMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Try to clear non-existent prefix
        store.clearEntriesWithPathPrefix("nonexistent");

        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixNullString() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");

        // Try to clear with null prefix (string version)
        store.clearEntriesWithPathPrefix((String) null);

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixEmpty() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");

        // Try to clear with empty prefix
        store.clearEntriesWithPathPrefix("");

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixExactMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings"), "root");
        store.putPrimitive(Paths.get("settings/general"), "general");
        store.putPrimitive(Paths.get("settings/name"), "name");

        // Clear exact match
        store.clearEntriesWithPathPrefix("settings");

        // Verify exact match and all children are removed
        assertEquals(0, store.size());
        assertFalse(store.containsKey(Paths.get("settings")));
        assertFalse(store.containsKey(Paths.get("settings/general")));
        assertFalse(store.containsKey(Paths.get("settings/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixDeepHierarchy() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data with deep hierarchy
        store.putPrimitive(Paths.get("a/b/c/d"), "deep");
        store.putPrimitive(Paths.get("a/b/c/e"), "deep2");
        store.putPrimitive(Paths.get("a/b/f"), "shallow");
        store.putPrimitive(Paths.get("a/g"), "other");
        store.putPrimitive(Paths.get("h"), "top");

        // Clear intermediate level
        store.clearEntriesWithPathPrefix("a/b");

        // Verify only cleared entries are removed
        assertEquals(2, store.size());
        assertFalse(store.containsKey(Paths.get("a/b/c/d")));
        assertFalse(store.containsKey(Paths.get("a/b/c/e")));
        assertFalse(store.containsKey(Paths.get("a/b/f")));

        // Verify other entries remain
        assertTrue(store.containsKey(Paths.get("a/g")));
        assertTrue(store.containsKey(Paths.get("h")));
    }

    @Test
    void testClearEntriesWithPathPrefixCaseSensitive() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("Settings/general/name"), "Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);

        // Clear lowercase settings - should only remove lowercase entries
        store.clearEntriesWithPathPrefix("settings");

        // Verify only lowercase entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/version")));
        assertTrue(store.containsKey(Paths.get("Settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathObject() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);
        store.putPrimitive(Paths.get("settings/ui/theme"), "dark");
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Verify initial state
        assertEquals(4, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));

        // Clear all settings entries using Path object
        store.clearEntriesWithPathPrefix(Paths.get("settings"));

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/name")));
        assertFalse(store.containsKey(Paths.get("settings/general/version")));
        assertFalse(store.containsKey(Paths.get("settings/ui/theme")));

        // Verify other entries remain
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathWithTrailingSlash() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Clear with trailing slash using Path object
        store.clearEntriesWithPathPrefix(Paths.get("settings/"));

        // Verify that settings entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/name")));
        assertFalse(store.containsKey(Paths.get("settings/general/version")));

        // Verify other entries remain
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathNoMatch() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Try to clear non-existent prefix using Path object
        store.clearEntriesWithPathPrefix(Paths.get("nonexistent"));

        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathNull() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up test data
        store.putPrimitive(Paths.get("settings/general/name"), "Application");

        // Try to clear with null prefix using Path object
        store.clearEntriesWithPathPrefix((Path) null);

        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testPutPrimitiveAll() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Set up initial data
        store.putPrimitive(Paths.get("settings/general/name"), "Original Application");
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);
        store.putPrimitive(Paths.get("features/enhanced"), true);

        // Create another store with some overlapping and new data
        JIPipePathMetadataStore other = new JIPipePathMetadataStore();
        other.putPrimitive(Paths.get("settings/general/name"), "New Application"); // Override existing
        other.putPrimitive(Paths.get("settings/general/debug"), true); // New entry
        other.putPrimitive(Paths.get("new/feature"), "added"); // New entry

        // Put all entries from other store
        store.putAll(other);

        // Verify sizes
        assertEquals(5, store.size());

        // Verify overridden entry
        assertEquals("New Application", store.getString(Paths.get("settings/general/name"), "Default"));

        // Verify existing entries that weren't overridden
        assertEquals(1.0, store.getDouble(Paths.get("settings/general/version"), 0.0));
        assertEquals(true, store.getBoolean(Paths.get("features/enhanced"), false));

        // Verify new entries from other store
        assertEquals(true, store.getBoolean(Paths.get("settings/general/debug"), false));
        assertEquals("added", store.getString(Paths.get("new/feature"), "default"));
    }

    @Test
    void testPutPrimitiveAllNull() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();
        store.putPrimitive(Paths.get("test/key"), "value");

        // Put with null should not change anything
        store.putAll(null);

        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(Paths.get("test/key"), "default"));
    }

    @Test
    void testPutPrimitiveAllEmpty() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();
        store.putPrimitive(Paths.get("test/key"), "value");

        JIPipePathMetadataStore empty = new JIPipePathMetadataStore();

        // Put with empty store should not change anything
        store.putAll(empty);

        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(Paths.get("test/key"), "default"));
    }

    @Test
    void testPathNormalization() {
        // Test path normalization utility methods
        assertEquals("path/to/file", JIPipePathMetadataStore.normalizePathString("path\\to\\file"));
        assertEquals("path/to/file", JIPipePathMetadataStore.normalizePathString("path/to/file"));
        assertEquals(null, JIPipePathMetadataStore.normalizePathString(null));

        Path originalPath = Paths.get("settings\\general\\name");
        Path normalizedPath = JIPipePathMetadataStore.normalizePath(originalPath);
        assertEquals("settings/general/name", normalizedPath.toString());
    }

    @Test
    void testBackslashMigration() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Add paths with backslashes (simulating Windows paths)
        store.putPrimitive(Paths.get("settings\\general\\name"), "Application");
        store.putPrimitive(Paths.get("settings\\general\\version"), 1.0);
        store.putPrimitive(Paths.get("ui\\theme"), "dark");

        // Verify that backslash paths exist
        assertTrue(store.hasBackslashPaths());
        assertEquals(3, store.size());

        // Migrate backslash paths
        store.migrateBackslashPaths();

        // Verify that no backslash paths remain
        assertFalse(store.hasBackslashPaths());
        assertEquals(3, store.size());

        // Verify that values are preserved with normalized paths
        assertEquals("Application", store.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(Paths.get("settings/general/version"), 0.0));
        assertEquals("dark", store.getString(Paths.get("ui/theme"), "light"));
    }

    @Test
    void testGetNormalizedCopy() {
        JIPipePathMetadataStore original = new JIPipePathMetadataStore();

        // Add paths with mixed separators
        original.putPrimitive(Paths.get("settings\\general\\name"), "Application");
        original.putPrimitive(Paths.get("settings/general/version"), 1.0);
        original.putPrimitive(Paths.get("ui\\theme"), "dark");

        // Create normalized copy
        JIPipePathMetadataStore normalized = original.getNormalizedCopy();

        // Verify original is unchanged
        assertEquals(3, original.size());
        assertTrue(original.hasBackslashPaths());

        // Verify normalized copy has no backslashes
        assertEquals(3, normalized.size());
        assertFalse(normalized.hasBackslashPaths());

        // Verify all paths use forward slashes
        Set<String> normalizedPaths = normalized.getNormalizedPathStrings();
        assertTrue(normalizedPaths.contains("settings/general/name"));
        assertTrue(normalizedPaths.contains("settings/general/version"));
        assertTrue(normalizedPaths.contains("ui/theme"));

        // Verify values are preserved
        assertEquals("Application", normalized.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, normalized.getDouble(Paths.get("settings/general/version"), 0.0));
        assertEquals("dark", normalized.getString(Paths.get("ui/theme"), "light"));
    }

    @Test
    void testJsonSerializationWithBackslashes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JIPipePathMetadataStore original = new JIPipePathMetadataStore();

        // Add paths with backslashes
        original.putPrimitive(Paths.get("settings\\general\\name"), "Application");
        original.putPrimitive(Paths.get("settings\\general\\version"), 1.0);

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
        assertEquals("Application", deserialized.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, deserialized.getDouble(Paths.get("settings/general/version"), 0.0));
        assertFalse(deserialized.hasBackslashPaths());
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
        assertEquals("Application", store.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(Paths.get("settings/general/version"), 0.0));
        assertFalse(store.hasBackslashPaths());
    }

    @Test
    void testToNestedMapWithBackslashes() {
        JIPipePathMetadataStore store = new JIPipePathMetadataStore();

        // Add paths with backslashes
        store.putPrimitive(Paths.get("settings\\general\\name"), "Application");
        store.putPrimitive(Paths.get("settings\\general\\version"), 1.0);
        store.putPrimitive(Paths.get("ui\\theme"), "dark");

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
        store.putPrimitive(Paths.get("settings\\general\\name"), "Application");  // Backslashes
        store.putPrimitive(Paths.get("settings/general/version"), 1.0);          // Forward slashes
        store.putPrimitive(Paths.get("ui\\theme"), "dark");                     // Backslashes

        // Before migration, backslash paths should not be found with forward slash paths
        assertEquals("Default", store.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(Paths.get("settings/general/version"), 0.0));
        assertEquals("light", store.getString(Paths.get("ui/theme"), "light"));

        // Verify backslash paths exist before migration
        assertTrue(store.hasBackslashPaths());

        // After migration, all should be normalized and accessible
        store.migrateBackslashPaths();
        assertFalse(store.hasBackslashPaths());

        // Now all paths should work with forward slashes
        assertEquals("Application", store.getString(Paths.get("settings/general/name"), "Default"));
        assertEquals(1.0, store.getDouble(Paths.get("settings/general/version"), 0.0));
        assertEquals("dark", store.getString(Paths.get("ui/theme"), "light"));
    }
}