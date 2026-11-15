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

package org.hkijena.jipipe.api.shortcuts;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeymapSettingsJsonTest {
    @Test void jsonRoundTrip() {
        KeymapSettings s = new KeymapSettings();
        s.setOverride("org.hkijena.jipipe.graph.tool.move", List.of(List.of(
                KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed M"))));
        s.setOverride("org.hkijena.jipipe.graph.tool.connect", List.of());   // removed, must not appear
        String json = JsonUtils.toJsonString(s);
        KeymapSettings back = JsonUtils.readFromString(json, KeymapSettings.class);
        assertEquals(2000L, back.getChordTimeoutMs());
        assertEquals(1, back.getOverrides().size());
        // KeyStroke.toString() canonical form ("ctrl", not "control"); parse accepts it again
        assertEquals(List.of(List.of("ctrl pressed T", "pressed M")),
                back.getOverrides().get("org.hkijena.jipipe.graph.tool.move"));
        assertEquals(1, back.overrideVariants("org.hkijena.jipipe.graph.tool.move").size());
    }

    /**
     * Pins the "defaults are never materialized" contract (issue #1346, Task
     * 9): the {@code overrides} map is the bean's actual content, while
     * {@code chordTimeoutMs} and {@code activeProfileId} serialize only when
     * they deviate from their default. In particular, the default
     * {@code 2000} must NOT be written, but the meaningful {@code 0}
     * (wait indefinitely) and any other value MUST be, so a stored default is
     * not materialized while a deliberate timeout of {@code 0} survives.
     */
    @Test void defaultTimeoutNotMaterializedButZeroAndCustomAre() {
        // The default: chordTimeoutMs is absent from the stored JSON.
        KeymapSettings def = new KeymapSettings();
        JsonNode defNode = JsonUtils.getObjectMapper().valueToTree(def);
        assertFalse(defNode.has("chordTimeoutMs"),
                "the default 2000 must not be materialized into the document");
        assertFalse(defNode.has("activeProfileId"),
                "a null activeProfileId must not be materialized");

        // A zero timeout is a meaningful value and must survive.
        KeymapSettings zero = new KeymapSettings();
        zero.setChordTimeoutMs(0);
        JsonNode zeroNode = JsonUtils.getObjectMapper().valueToTree(zero);
        assertTrue(zeroNode.has("chordTimeoutMs") && zeroNode.get("chordTimeoutMs").asLong() == 0L,
                "a 0 (wait indefinitely) timeout must be serialized, not dropped as the type default");
        assertEquals(0L, JsonUtils.readFromString(JsonUtils.toJsonString(zero), KeymapSettings.class).getChordTimeoutMs());

        // A custom timeout must survive.
        KeymapSettings custom = new KeymapSettings();
        custom.setChordTimeoutMs(1500);
        custom.setActiveProfileId("legacy");
        JsonNode customNode = JsonUtils.getObjectMapper().valueToTree(custom);
        assertTrue(customNode.has("chordTimeoutMs") && customNode.get("chordTimeoutMs").asLong() == 1500L);
        assertTrue(customNode.has("activeProfileId") && customNode.get("activeProfileId").asText().equals("legacy"));
        KeymapSettings customBack = JsonUtils.readFromString(JsonUtils.toJsonString(custom), KeymapSettings.class);
        assertEquals(1500L, customBack.getChordTimeoutMs());
        assertEquals("legacy", customBack.getActiveProfileId());

        // An absent field reads back as the default.
        assertEquals(KeymapSettings.DEFAULT_CHORD_TIMEOUT_MS,
                JsonUtils.readFromString("{}", KeymapSettings.class).getChordTimeoutMs());
    }

    @Test void unknownActionIdsSurvive() {
        // overrides containing "org.hkijena.jipipe.plugin.that.went.away.x" survive write+read verbatim
        KeymapSettings s = new KeymapSettings();
        s.setOverride("org.hkijena.jipipe.plugin.that.went.away.x", List.of(List.of(
                KeyStroke.getKeyStroke("control pressed P"))));
        String json = JsonUtils.toJsonString(s);
        KeymapSettings back = JsonUtils.readFromString(json, KeymapSettings.class);
        assertTrue(back.hasOverride("org.hkijena.jipipe.plugin.that.went.away.x"),
                "an override for an unknown action id must survive a round trip");
        assertEquals(List.of(List.of("ctrl pressed P")),
                back.getOverrides().get("org.hkijena.jipipe.plugin.that.went.away.x"));
    }

    @Test void malformedChordDroppedNotThrown() {
        // overrides {"x": [["not a keystroke"]]} -> overrideVariants("x") is empty, no exception
        KeymapSettings s = JsonUtils.readFromString(
                "{\"activeProfileId\":null,\"overrides\":{\"x\":[[\"not a keystroke\"]]}}", KeymapSettings.class);
        List<List<KeyStroke>> variants = assertDoesNotThrow(() -> s.overrideVariants("x"));
        assertTrue(variants.isEmpty(), "a fully malformed chord is dropped, not thrown");
    }

    @Test void currentIsNonNullWithDefaultsWhenNothingStored() {
        // Boots the live service (the module's LauncherSessionListeners isolate the
        // profile into a temp dir and pre-patch ImageJ, so this is headless-safe).
        // On a fresh profile the "registry:keymap" block is absent, so the binding
        // reports the key's null default and current() must coalesce it away.
        JIPipe.ensureInstance();
        KeymapSettings current = JIPipeShortcutSettings.current();
        assertNotNull(current, "current() must never return null on an empty store");
        assertEquals(KeymapSettings.DEFAULT_CHORD_TIMEOUT_MS, current.getChordTimeoutMs());
    }
}
