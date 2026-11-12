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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the JSON contract of the floating map state: the exact stored shape
 * (spec section 2 of the navigator overhaul), that missing fields fall back
 * to the defaults, and that copies are deep.
 * <p>
 * Uses the same {@link org.hkijena.jipipe.utils.json.JsonUtils} mapper that
 * the settings service applies to {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#PIPELINE_MAP_STATE}
 * and {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#COMPARTMENTS_MAP_STATE}.
 */
class JIPipeDesktopMapStateTest {

    @Test
    void serializationMatchesTheSpecifiedJsonShape() throws Exception {
        JIPipeDesktopMapState state = JIPipeDesktopMapState.defaultState();
        state.setPosition(470, 16);

        String json = JsonUtils.getObjectMapper().writeValueAsString(state);

        assertEquals("{\"visible\":true,\"position\":{\"x\":470,\"y\":16}," +
                "\"size\":{\"w\":280,\"h\":200},\"locked\":false," +
                "\"anchor\":\"Auto\",\"anchorOffset\":{\"x\":0,\"y\":0}," +
                "\"layers\":{\"nodes\":true,\"edges\":true,\"notes\":true}}", json);
    }

    @Test
    void roundTripPreservesAllFields() throws Exception {
        JIPipeDesktopMapState state = JIPipeDesktopMapState.defaultState();
        state.setVisible(false);
        state.setPosition(470, 16);
        state.setSize(320, 240);
        state.setLocked(true);
        state.setAnchor(JIPipeDesktopMapState.AnchorMode.TopRight);
        state.setAnchorOffset(16, 12);
        state.setLayerNodes(false);
        state.setLayerEdges(false);
        state.setLayerNotes(false);

        String json = JsonUtils.getObjectMapper().writeValueAsString(state);
        JIPipeDesktopMapState restored = JsonUtils.getObjectMapper().readValue(json, JIPipeDesktopMapState.class);

        assertEquals(state, restored);
        assertFalse(restored.isVisible());
        assertEquals(470, restored.getX());
        assertEquals(16, restored.getY());
        assertEquals(320, restored.getWidth());
        assertEquals(240, restored.getHeight());
        assertTrue(restored.isLocked());
        assertEquals(JIPipeDesktopMapState.AnchorMode.TopRight, restored.getAnchor());
        assertEquals(16, restored.getAnchorOffset().getX());
        assertEquals(12, restored.getAnchorOffset().getY());
        assertFalse(restored.isLayerNodes());
        assertFalse(restored.isLayerEdges());
        assertFalse(restored.isLayerNotes());
    }

    @Test
    void emptyJsonFallsBackToTheDefaults() throws Exception {
        JIPipeDesktopMapState restored = JsonUtils.getObjectMapper().readValue("{}", JIPipeDesktopMapState.class);

        assertEquals(JIPipeDesktopMapState.defaultState(), restored);
        assertTrue(restored.isVisible());
        assertEquals(0, restored.getX());
        assertEquals(0, restored.getY());
        assertEquals(JIPipeDesktopMapState.MAP_DEFAULT_WIDTH, restored.getWidth());
        assertEquals(JIPipeDesktopMapState.MAP_DEFAULT_HEIGHT, restored.getHeight());
        assertFalse(restored.isLocked());
        assertEquals(JIPipeDesktopMapState.AnchorMode.Auto, restored.getAnchor());
        assertEquals(0, restored.getAnchorOffset().getX());
        assertEquals(0, restored.getAnchorOffset().getY());
        assertTrue(restored.isLayerNodes());
        assertTrue(restored.isLayerEdges());
        assertTrue(restored.isLayerNotes());
    }

    @Test
    void legacyJsonWithoutAnchorFieldsFallsBackToAuto() throws Exception {
        // The JSON shape before the anchor model existed: stored positions
        // must migrate to the default Auto mode
        JIPipeDesktopMapState restored = JsonUtils.getObjectMapper().readValue(
                "{\"visible\":true,\"position\":{\"x\":470,\"y\":16}," +
                        "\"size\":{\"w\":280,\"h\":200},\"locked\":false," +
                        "\"layers\":{\"nodes\":true,\"edges\":true,\"notes\":true}}",
                JIPipeDesktopMapState.class);

        assertEquals(JIPipeDesktopMapState.AnchorMode.Auto, restored.getAnchor());
        assertEquals(0, restored.getAnchorOffset().getX());
        assertEquals(0, restored.getAnchorOffset().getY());
        assertEquals(470, restored.getX());
        assertEquals(16, restored.getY());
    }

    @Test
    void copyConstructorDeepCopiesTheState() {
        JIPipeDesktopMapState source = JIPipeDesktopMapState.defaultState();
        source.setPosition(470, 16);
        source.setLocked(true);
        source.setAnchor(JIPipeDesktopMapState.AnchorMode.BottomRight);
        source.setAnchorOffset(8, 8);
        source.setLayerNotes(false);

        JIPipeDesktopMapState copy = new JIPipeDesktopMapState(source);
        assertEquals(source, copy);

        copy.setPosition(100, 100);
        copy.setLocked(false);
        copy.setAnchor(JIPipeDesktopMapState.AnchorMode.TopLeft);
        copy.setAnchorOffset(1, 2);
        copy.setLayerNotes(true);

        assertEquals(470, source.getX());
        assertEquals(16, source.getY());
        assertTrue(source.isLocked());
        assertEquals(JIPipeDesktopMapState.AnchorMode.BottomRight, source.getAnchor());
        assertEquals(8, source.getAnchorOffset().getX());
        assertEquals(8, source.getAnchorOffset().getY());
        assertFalse(source.isLayerNotes());
    }
}
