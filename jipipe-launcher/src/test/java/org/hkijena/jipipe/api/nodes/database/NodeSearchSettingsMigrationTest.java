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

package org.hkijena.jipipe.api.nodes.database;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeNodeSearchApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the settings migration of the "Node search" page (#1409, merged-ranking MR D):
 * <ul>
 * <li>the new sheet {@code org.hkijena.jipipe:node-search} registers and resolves with
 * its defaults (implementation Enhanced, AI contribution on, typo tolerance on);</li>
 * <li>the search implementation parameter is gone from the general UI sheet (its
 * persisted key {@code search-implementation-v2} moved to the new sheet);</li>
 * <li>{@link JIPipeNodeDatabase#getSearchImplementation()} reads the new sheet without
 * throwing and never returns null;</li>
 * <li>the compat migration adopts the old general-ui value only when the new sheet has
 * no stored value of its own - the "a user who chose Legacy keeps Legacy" contract of
 * the plan's review focus. Tested through the pure adoption function with synthetic
 * raw-sheets nodes, so no developer profile is touched.</li>
 * </ul>
 */
class NodeSearchSettingsMigrationTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @Test
    void newNodeSearchSheetResolvesWithDefaults() {
        JIPipeNodeSearchApplicationSettings settings = JIPipeNodeSearchApplicationSettings.getInstance();
        assertNotNull(settings, "The node search settings sheet must be registered and resolvable");
        assertTrue(settings.isAiContribution(), "AI contribution must default to enabled");
        assertTrue(settings.isTypoTolerance(), "Typo tolerance must default to enabled");
        assertEquals(JIPipeNodeDatabaseSearchImplementation.Enhanced, settings.getSearchImplementation(),
                "The search implementation must default to Enhanced");
    }

    @Test
    void generalUiSheetNoLongerExposesSearchImplementation() {
        JIPipeGeneralUIApplicationSettings sheet = JIPipeGeneralUIApplicationSettings.getInstance();
        assertNotNull(sheet, "The general UI sheet must still be registered and resolvable");
        assertFalse(JIPipeParameterTree.getParameters(sheet).containsKey("search-implementation-v2"),
                "The general UI sheet must no longer expose the search implementation parameter");
        assertThrows(NoSuchFieldException.class,
                () -> JIPipeGeneralUIApplicationSettings.class.getDeclaredField("searchImplementation"),
                "The searchImplementation field must be removed from the general UI sheet");
    }

    @Test
    void nodeDatabaseImplementationReadDoesNotThrowAndIsNonNull() {
        JIPipeNodeDatabase database = assertDoesNotThrow(JIPipeNodeDatabase::getInstance);
        JIPipeNodeDatabaseSearchImplementation implementation =
                assertDoesNotThrow(database::getSearchImplementation);
        assertNotNull(implementation, "The database's search implementation read must not return null");
    }

    @Test
    void compatMigrationAdoptsLegacyImplementationWhenOwnKeyIsAbsent() throws Exception {
        JsonNode rawSheets = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:general-ui\":{\"search-implementation-v2\":\"Legacy\"}}");
        assertEquals(JIPipeNodeDatabaseSearchImplementation.Legacy,
                JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(rawSheets),
                "A stored Legacy value of the old sheet must be adopted (Legacy stays Legacy)");
    }

    @Test
    void compatMigrationAdoptsEnhancedImplementationWhenOwnKeyIsAbsent() throws Exception {
        JsonNode rawSheets = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:general-ui\":{\"search-implementation-v2\":\"Enhanced\"}}");
        assertEquals(JIPipeNodeDatabaseSearchImplementation.Enhanced,
                JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(rawSheets));
    }

    @Test
    void compatMigrationPrefersTheOwnStoredValue() throws Exception {
        JsonNode rawSheets = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:general-ui\":{\"search-implementation-v2\":\"Legacy\"}," +
                        "\"org.hkijena.jipipe:node-search\":{\"search-implementation-v2\":\"Enhanced\"}}");
        assertNull(JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(rawSheets),
                "Nothing is adopted when the new sheet has a stored value of its own");
    }

    @Test
    void compatMigrationIsInertWithoutALegacyValue() throws Exception {
        assertNull(JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(
                        JsonUtils.getObjectMapper().readTree("{}")),
                "An empty raw sheets node adopts nothing");
        assertNull(JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(
                        JsonUtils.getObjectMapper().readTree(
                                "{\"org.hkijena.jipipe:general-ui\":{\"reduced-motion\":true}}")),
                "An old sheet block without the key adopts nothing");
        assertNull(JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(
                        JsonUtils.getObjectMapper().readTree(
                                "{\"org.hkijena.jipipe:general-ui\":{\"search-implementation-v2\":\"Nonsense\"}}")),
                "A value that is not a valid implementation name adopts nothing (fail-closed default)");
        assertNull(JIPipeNodeSearchApplicationSettings.adoptLegacySearchImplementation(null),
                "A null raw sheets node adopts nothing");
    }
}
