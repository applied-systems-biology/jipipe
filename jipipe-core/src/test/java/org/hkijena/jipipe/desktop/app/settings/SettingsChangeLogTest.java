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

package org.hkijena.jipipe.desktop.app.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.ChangeOrigin;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.SettingsChangedEvent;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SettingsChangeLog}, the pure (non-Swing) since-open audit of
 * settings changes.
 */
class SettingsChangeLogTest {

    private static final SettingsScope scope = SettingsScope.APPLICATION;
    private static final ObjectMapper MAPPER = JsonUtils.getObjectMapper();

    private static JsonNode node(int value) {
        return MAPPER.valueToTree(value);
    }

    @Test
    void recordsOnlyForeignOriginsInTheForeignList() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "registry:x/a", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.record(new SettingsChangedEvent(scope, "registry:x/b", null, node(2), ChangeOrigin.EXTERNAL_PROCESS), "2");
        assertEquals(2, log.size());
        assertEquals(1, log.entries().stream().filter(SettingsChangeLog.ChangeEntry::isForeign).count());
    }

    @Test
    void clearEmptiesEntries() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "a", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.clear();
        assertTrue(log.entries().isEmpty());
    }

    @Test
    void capsAtTwoHundredEntriesDroppingTheOldest() {
        SettingsChangeLog log = new SettingsChangeLog();
        for (int i = 0; i < SettingsChangeLog.MAX_ENTRIES + 1; i++) {
            log.record(new SettingsChangedEvent(scope, "registry:x/key" + i, null, node(i), ChangeOrigin.LOCAL_EDIT),
                    Integer.toString(i));
        }
        assertEquals(SettingsChangeLog.MAX_ENTRIES, log.size());
        assertEquals(SettingsChangeLog.MAX_ENTRIES, log.entries().size());
        assertTrue(log.entries().stream().noneMatch(entry -> entry.path().equals("registry:x/key0")),
                "the oldest entry must have been dropped");
        assertEquals("registry:x/key" + SettingsChangeLog.MAX_ENTRIES,
                log.entries().get(log.entries().size() - 1).path());
    }

    @Test
    void acknowledgeForeignRemovesOnlyThatScopesForeignEntries() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "registry:x/local", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.record(new SettingsChangedEvent(scope, "registry:x/external", null, node(2), ChangeOrigin.EXTERNAL_FILE), "2");
        log.record(new SettingsChangedEvent(SettingsScope.PROJECT, "sheet:p/external", null, node(3),
                ChangeOrigin.EXTERNAL_PROCESS), "3");
        log.record(new SettingsChangedEvent(SettingsScope.PROJECT, "sheet:p/local", null, node(4),
                ChangeOrigin.RESET), "4");

        log.acknowledgeForeign(scope);

        assertEquals(3, log.size());
        assertTrue(log.entries().stream().noneMatch(entry -> entry.scope() == scope && entry.isForeign()));
        assertTrue(log.entries().stream().anyMatch(entry -> entry.scope() == scope && entry.path().equals("registry:x/local")));
        assertTrue(log.entries().stream().anyMatch(entry -> entry.scope() == SettingsScope.PROJECT && entry.isForeign()));
        assertTrue(log.entries().stream().anyMatch(entry -> entry.scope() == SettingsScope.PROJECT && entry.path().equals("sheet:p/local")));
    }

    @Test
    void entryRendersOldToNewAndMarksARemoval() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "registry:x/a", node(1), node(2), ChangeOrigin.LOCAL_EDIT), "2");
        log.record(new SettingsChangedEvent(scope, "registry:x/b", node(3), null, ChangeOrigin.RESET), "");
        assertEquals("registry:x/a: 1 \u2192 2", log.entries().get(0).displayText());
        assertEquals("registry:x/b: 3 \u2192 ", log.entries().get(1).displayText());
    }

    @Test
    void entriesReturnsADefensiveCopy() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "registry:x/a", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.entries().clear();
        assertEquals(1, log.size());
        assertFalse(log.entries().isEmpty());
    }
}