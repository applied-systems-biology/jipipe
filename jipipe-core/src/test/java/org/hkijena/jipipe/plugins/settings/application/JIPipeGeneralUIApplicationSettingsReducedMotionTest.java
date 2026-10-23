package org.hkijena.jipipe.plugins.settings.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Pure/headless: constructs the sheet directly, never calls JIPipe.ensureInstance().
// Pins the reduced-motion setting contract: default, serialization key, round-trip,
// and back-compat fallback for settings.json files that predate the key.
class JIPipeGeneralUIApplicationSettingsReducedMotionTest {

    @Test
    void defaultIsAnimationsOn() {
        assertFalse(new JIPipeGeneralUIApplicationSettings().isReducedMotion());
    }

    @Test
    void serializesUnderStableKey() throws Exception {
        JIPipeGeneralUIApplicationSettings s = new JIPipeGeneralUIApplicationSettings();
        s.setReducedMotion(true);
        JsonNode node = JsonUtils.getObjectMapper().readTree(s.serializeToJsonString());
        assertTrue(node.has("reduced-motion"), "expected stable key 'reduced-motion'");
        assertTrue(node.get("reduced-motion").asBoolean());
    }

    @Test
    void roundTripPreservesValue() throws Exception {
        JIPipeGeneralUIApplicationSettings s = new JIPipeGeneralUIApplicationSettings();
        s.setReducedMotion(true);
        JsonNode node = JsonUtils.getObjectMapper().readTree(s.serializeToJsonString());
        JIPipeGeneralUIApplicationSettings back = new JIPipeGeneralUIApplicationSettings();
        back.deserializeFromJsonNode(node);
        assertTrue(back.isReducedMotion());
    }

    @Test
    void missingKeyFallsBackToDefault() throws Exception {
        // settings.json predating reduced-motion: only an unrelated key is present.
        JsonNode node = JsonUtils.getObjectMapper().readTree("{\"maximize-windows\":true}");
        JIPipeGeneralUIApplicationSettings back = new JIPipeGeneralUIApplicationSettings();
        back.deserializeFromJsonNode(node);
        assertFalse(back.isReducedMotion());
    }
}
