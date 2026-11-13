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

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
