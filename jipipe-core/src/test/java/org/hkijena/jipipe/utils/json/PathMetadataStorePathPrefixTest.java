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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathMetadataStorePathPrefixTest {

    @Test
    void testClearEntriesWithPathPrefix() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        store.put(Paths.get("settings/ui/theme"), "dark");
        store.put(Paths.get("features/enhanced"), true);
        store.put(Paths.get("other/setting"), "value");
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        store.put(Paths.get("features/enhanced"), true);
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("features/enhanced"), true);
        
        // Try to clear non-existent prefix
        store.clearEntriesWithPathPrefix("nonexistent");
        
        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixNullString() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        
        // Try to clear with null prefix (string version)
        store.clearEntriesWithPathPrefix((String) null);
        
        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixEmpty() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        
        // Try to clear with empty prefix
        store.clearEntriesWithPathPrefix("");
        
        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixExactMatch() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings"), "root");
        store.put(Paths.get("settings/general"), "general");
        store.put(Paths.get("settings/name"), "name");
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data with deep hierarchy
        store.put(Paths.get("a/b/c/d"), "deep");
        store.put(Paths.get("a/b/c/e"), "deep2");
        store.put(Paths.get("a/b/f"), "shallow");
        store.put(Paths.get("a/g"), "other");
        store.put(Paths.get("h"), "top");
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("Settings/general/name"), "Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        
        // Clear lowercase settings - should only remove lowercase entries
        store.clearEntriesWithPathPrefix("settings");
        
        // Verify only lowercase entries are removed
        assertEquals(1, store.size());
        assertFalse(store.containsKey(Paths.get("settings/general/version")));
        assertTrue(store.containsKey(Paths.get("Settings/general/name")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathObject() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        store.put(Paths.get("settings/ui/theme"), "dark");
        store.put(Paths.get("features/enhanced"), true);
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        store.put(Paths.get("features/enhanced"), true);
        
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
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        store.put(Paths.get("features/enhanced"), true);
        
        // Try to clear non-existent prefix using Path object
        store.clearEntriesWithPathPrefix(Paths.get("nonexistent"));
        
        // Verify nothing changed
        assertEquals(2, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
        assertTrue(store.containsKey(Paths.get("features/enhanced")));
    }

    @Test
    void testClearEntriesWithPathPrefixPathNull() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up test data
        store.put(Paths.get("settings/general/name"), "Application");
        
        // Try to clear with null prefix using Path object
        store.clearEntriesWithPathPrefix((Path) null);
        
        // Verify nothing changed
        assertEquals(1, store.size());
        assertTrue(store.containsKey(Paths.get("settings/general/name")));
    }

    @Test
    void testPutAll() {
        PathMetadataStore store = new PathMetadataStore();
        
        // Set up initial data
        store.put(Paths.get("settings/general/name"), "Original Application");
        store.put(Paths.get("settings/general/version"), 1.0);
        store.put(Paths.get("features/enhanced"), true);
        
        // Create another store with some overlapping and new data
        PathMetadataStore other = new PathMetadataStore();
        other.put(Paths.get("settings/general/name"), "New Application"); // Override existing
        other.put(Paths.get("settings/general/debug"), true); // New entry
        other.put(Paths.get("new/feature"), "added"); // New entry
        
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
    void testPutAllNull() {
        PathMetadataStore store = new PathMetadataStore();
        store.put(Paths.get("test/key"), "value");
        
        // Put with null should not change anything
        store.putAll(null);
        
        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(Paths.get("test/key"), "default"));
    }

    @Test
    void testPutAllEmpty() {
        PathMetadataStore store = new PathMetadataStore();
        store.put(Paths.get("test/key"), "value");
        
        PathMetadataStore empty = new PathMetadataStore();
        
        // Put with empty store should not change anything
        store.putAll(empty);
        
        // Verify original data remains
        assertEquals(1, store.size());
        assertEquals("value", store.getString(Paths.get("test/key"), "default"));
    }
}