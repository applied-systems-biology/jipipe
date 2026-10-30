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

package org.hkijena.jipipe.plugins.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteColor;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 15 of the modern settings system: the migration-safety pin for the
 * typed {@link SettingsKeys}.
 * <p>
 * The keyified consumers replaced {@code getFromRegistry}/{@code putIntoRegistry}
 * calls with {@link SettingsBinding}s. Existing profiles hold the values in
 * the shape the legacy path wrote, so every key must address the exact same
 * location and the binding round-trip must produce byte-identical JSON to
 * the legacy path for every non-primitive type. No {@code JIPipe} service is
 * booted: the component is constructed for testing over a temporary profile.
 */
class SettingsKeysTest {

    private static StringAndStringPairParameterList sampleTemplates() {
        StringAndStringPairParameterList templates = new StringAndStringPairParameterList();
        templates.add(new StringAndStringPairParameter("My layout", "{\"visibilities\":{\"log\":true},\"left-panel-width\":350}"));
        templates.add(new StringAndStringPairParameter("Wide", "{\"left-panel-width\":600}"));
        return templates;
    }

    @Test
    void keysAddressTheLegacyRegistryLocations() {
        assertEquals("registry:ui-graph-editor", SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS.blockPath());
        assertEquals("any/dock/show-labels", SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS.subPath());
        assertEquals(Boolean.class, SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS.type());
        assertEquals(true, SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS.defaultValue());

        assertEquals("registry:ui-graph-editor", SettingsKeys.PIPELINE_DOCK_STATE.blockPath());
        assertEquals("pipeline/dock-state", SettingsKeys.PIPELINE_DOCK_STATE.subPath());
        assertEquals("registry:ui-graph-editor", SettingsKeys.COMPARTMENTS_DOCK_STATE.blockPath());
        assertEquals("compartments/dock-state", SettingsKeys.COMPARTMENTS_DOCK_STATE.subPath());

        assertEquals("registry:ui-dock", SettingsKeys.DOCK_LAYOUT_TEMPLATES.blockPath());
        assertEquals("layouts", SettingsKeys.DOCK_LAYOUT_TEMPLATES.subPath());
        assertEquals(StringAndStringPairParameterList.class, SettingsKeys.DOCK_LAYOUT_TEMPLATES.type());

        assertEquals("registry:node-db", SettingsKeys.PINNED_NODE_IDS.blockPath());
        assertEquals("pinned-node-ids", SettingsKeys.PINNED_NODE_IDS.subPath());
        assertEquals("registry:node-db", SettingsKeys.NODE_BROWSER_KINDS.blockPath());
        assertEquals("node-browser-kinds", SettingsKeys.NODE_BROWSER_KINDS.subPath());

        // The registry databases are user-interface state: application scope.
        for (var key : List.of(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS, SettingsKeys.PIPELINE_DOCK_STATE,
                SettingsKeys.COMPARTMENTS_DOCK_STATE, SettingsKeys.DOCK_LAYOUT_TEMPLATES,
                SettingsKeys.PINNED_NODE_IDS, SettingsKeys.NODE_BROWSER_KINDS)) {
            assertEquals(SettingsScope.APPLICATION, key.scope());
        }
    }

    /**
     * The most exotic value type: a parameter collection. Its stored JSON
     * must not change, or every existing profile loses its saved layouts.
     */
    @Test
    void dockLayoutTemplatesRoundTripIsJsonIdenticalToTheLegacyShape(@TempDir Path userDir) throws Exception {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            StringAndStringPairParameterList templates = sampleTemplates();

            // The legacy path: putIntoRegistry wrote valueToTree(value) at the
            // same location; getFromRegistry read it back via treeToValue.
            StringAndStringPairParameterList legacy = JsonUtils.getObjectMapper().treeToValue(
                    JsonUtils.getObjectMapper().valueToTree(templates), StringAndStringPairParameterList.class);

            SettingsBinding<StringAndStringPairParameterList> binding =
                    settings.bind(SettingsKeys.DOCK_LAYOUT_TEMPLATES);
            binding.set(templates);

            JsonNode stored = settings.document(SettingsScope.APPLICATION)
                    .get("registry:ui-dock", "layouts");
            assertEquals(JsonUtils.getObjectMapper().valueToTree(templates), stored,
                    "the stored JSON must be identical to the legacy putIntoRegistry shape");
            assertEquals(JsonUtils.getObjectMapper().valueToTree(legacy), stored,
                    "the legacy read-back value must serialize to the same JSON again");

            StringAndStringPairParameterList readBack = binding.get();
            assertEquals(templates, readBack);
            assertEquals(JsonUtils.getObjectMapper().valueToTree(templates),
                    JsonUtils.getObjectMapper().valueToTree(readBack),
                    "binding.get() must serialize to the same JSON as the legacy read");
        } finally {
            settings.dispose();
        }
    }

    /**
     * An empty template list is the key's default, so it is never stored: a
     * profile written by the legacy path materialized {@code []} and both
     * shapes must read back as the same (empty) list.
     */
    @Test
    void emptyDockLayoutTemplatesReadsAsDefaultAndIsNotMaterialized(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            SettingsBinding<StringAndStringPairParameterList> binding =
                    settings.bind(SettingsKeys.DOCK_LAYOUT_TEMPLATES);
            assertTrue(binding.get().isEmpty());
            binding.set(new StringAndStringPairParameterList());
            assertFalse(binding.isModified(), "the default must not be materialized");
            assertTrue(binding.get().isEmpty());

            // A profile that still holds the legacy "[]" reads back as empty.
            SettingsDocument document = settings.document(SettingsScope.APPLICATION);
            document.set("registry:ui-dock", "layouts", JsonUtils.getObjectMapper().createArrayNode());
            assertEquals(List.of(), new ArrayList<>(binding.get()));
        } finally {
            settings.dispose();
        }
    }

    /**
     * The dock state default must be the same "always show right panel"
     * state the call sites passed as their default, so a fresh profile
     * behaves exactly like before.
     */
    @Test
    void pipelineAndCompartmentsDockStateDefaultsAlwaysShowTheRightPanel(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            assertTrue(SettingsKeys.PIPELINE_DOCK_STATE.defaultValue().isAlwaysShowRightPanel());
            assertTrue(SettingsKeys.COMPARTMENTS_DOCK_STATE.defaultValue().isAlwaysShowRightPanel());
            assertFalse(settings.bind(SettingsKeys.PIPELINE_DOCK_STATE).isModified());
        } finally {
            settings.dispose();
        }
    }

    /**
     * Node ids are stored as a JSON array of strings; the binding's list
     * type must read a legacy array and write the same shape.
     */
    @Test
    void pinnedNodeIdsRoundTrip(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            List<String> ids = List.of("a:b", "c:d");
            SettingsBinding<List<String>> binding = settings.bind(SettingsKeys.PINNED_NODE_IDS);
            binding.set(ids);
            assertEquals(JsonUtils.getObjectMapper().valueToTree(ids),
                    settings.document(SettingsScope.APPLICATION).get("registry:node-db", "pinned-node-ids"));
            assertEquals(ids, binding.get());
        } finally {
            settings.dispose();
        }
    }

    @Test
    void paletteKeysAddressTheLegacyRegistryLocations() {
        assertEquals("registry:ui-palette", SettingsKeys.USER_PALETTE_COLORS_FG.blockPath());
        assertEquals("user-palette-colors/fg", SettingsKeys.USER_PALETTE_COLORS_FG.subPath());
        assertEquals("user-palette-colors/fg-a", SettingsKeys.USER_PALETTE_COLORS_FG_A.subPath());
        assertEquals("user-palette-colors/fg-bg", SettingsKeys.USER_PALETTE_COLORS_FG_BG.subPath());
        assertEquals("user-palette-colors/fg-bg-a", SettingsKeys.USER_PALETTE_COLORS_FG_BG_A.subPath());
        for (var key : List.of(SettingsKeys.USER_PALETTE_COLORS_FG, SettingsKeys.USER_PALETTE_COLORS_FG_A,
                SettingsKeys.USER_PALETTE_COLORS_FG_BG, SettingsKeys.USER_PALETTE_COLORS_FG_BG_A)) {
            assertEquals("registry:ui-palette", key.blockPath());
            assertEquals(JsonNode.class, key.type());
            assertEquals(SettingsScope.APPLICATION, key.scope());
        }

        assertEquals("registry:ui-general", SettingsKeys.RUN_QUEUE_SHOW_LOG.blockPath());
        assertEquals("logger-panel/show-log", SettingsKeys.RUN_QUEUE_SHOW_LOG.subPath());
        assertEquals(Boolean.class, SettingsKeys.RUN_QUEUE_SHOW_LOG.type());
        assertEquals(true, SettingsKeys.RUN_QUEUE_SHOW_LOG.defaultValue());
        assertEquals(SettingsScope.APPLICATION, SettingsKeys.RUN_QUEUE_SHOW_LOG.scope());
    }

    /**
     * Migration-safety pin for the user palette colors: the stored JSON must
     * be byte-identical to the legacy {@code putIntoRegistry} shape, and the
     * consumer's read (the typed list reader) must reproduce the same JSON
     * again.
     * <p>
     * The value type is deliberately a raw {@link JsonNode}: a
     * {@code List.class} key hands out {@code LinkedHashMap} elements, whose
     * re-serialization is not the stored shape (and {@code Color} is
     * serialized as a hex string, so a map round-trip changes it).
     */
    @Test
    void userPaletteColorsRoundTripIsJsonIdenticalToTheLegacyShape(@TempDir Path userDir) throws Exception {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            // The legacy putIntoRegistry wrote valueToTree(List<JIPipeDesktopColorPaletteColor>).
            List<JIPipeDesktopColorPaletteColor> legacy = new ArrayList<>();
            legacy.add(new JIPipeDesktopColorPaletteColor(new Color(255, 0, 0)));
            legacy.add(new JIPipeDesktopColorPaletteColor(new Color(0, 255, 0), null));
            legacy.add(new JIPipeDesktopColorPaletteColor(new Color(0, 0, 255), new Color(200, 200, 200)));
            JsonNode legacyJson = JsonUtils.getObjectMapper().valueToTree(legacy);

            SettingsBinding<JsonNode> binding = settings.bind(SettingsKeys.USER_PALETTE_COLORS_FG);
            binding.set(legacyJson);
            JsonNode stored = settings.document(SettingsScope.APPLICATION)
                    .get("registry:ui-palette", "user-palette-colors/fg");
            assertEquals(legacyJson, stored, "the stored JSON must be identical to the legacy putIntoRegistry shape");
            assertEquals(JsonUtils.getObjectMapper().valueToTree(legacy),
                    JsonUtils.getObjectMapper().valueToTree(binding.get()));

            // The consumer read: the typed list reader of the concrete element type.
            List<JIPipeDesktopColorPaletteColor> readBack =
                    JsonUtils.getObjectMapper().readerForListOf(JIPipeDesktopColorPaletteColor.class).readValue(binding.get());
            assertEquals(legacy.size(), readBack.size());
            for (int i = 0; i < legacy.size(); i++) {
                assertEquals(legacy.get(i).getForeground(), readBack.get(i).getForeground());
                assertEquals(legacy.get(i).getBackground(), readBack.get(i).getBackground());
            }
            assertEquals(legacyJson, JsonUtils.getObjectMapper().valueToTree(readBack),
                    "the (opaque) colors must serialize to the same JSON again");

            // And writing the consumer's list again through the binding is byte-stable.
            binding.set(JsonUtils.getObjectMapper().valueToTree(readBack));
            assertEquals(JsonUtils.getObjectMapper().valueToTree(readBack), settings.document(SettingsScope.APPLICATION)
                    .get("registry:ui-palette", "user-palette-colors/fg"));
        } finally {
            settings.dispose();
        }
    }

    /**
     * <b>Known legacy lossiness — pinned so it does not change silently.</b>
     * {@code ColorUtils.hexStringToColor} parses the alpha digits of a
     * {@code #RRGGBBAA} hex string as a <em>decimal</em> integer, so reading
     * a stored color back through the consumer's typed list reader does not
     * reproduce translucent colors (alpha 128 → 80, i.e. 0x50). This is the
     * behavior the storage had before the keyification (the legacy
     * {@code getListFromRegistry} used the same reader on the same JSON), so
     * it is pinned here rather than changed.
     */
    @Test
    void translucentPaletteColorsAreReadWithTheLegacyAlphaParsing(@TempDir Path userDir) throws Exception {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            List<JIPipeDesktopColorPaletteColor> legacy = new ArrayList<>();
            legacy.add(new JIPipeDesktopColorPaletteColor(new Color(10, 20, 30, 128), new Color(200, 200, 200)));
            JsonNode legacyJson = JsonUtils.getObjectMapper().valueToTree(legacy);
            assertEquals("#0A141E80", legacyJson.get(0).get("foreground").asText());

            SettingsBinding<JsonNode> binding = settings.bind(SettingsKeys.USER_PALETTE_COLORS_FG_A);
            binding.set(legacyJson);
            assertEquals(legacyJson, settings.document(SettingsScope.APPLICATION)
                    .get("registry:ui-palette", "user-palette-colors/fg-a"));
            List<JIPipeDesktopColorPaletteColor> readBack =
                    JsonUtils.getObjectMapper().readerForListOf(JIPipeDesktopColorPaletteColor.class).readValue(binding.get());
            assertEquals(new Color(10, 20, 30, 80), readBack.get(0).getForeground(),
                    "legacy alpha parsing: the hex alpha digits are read as decimal (128 stored as 0x80 -> 0x50)");
        } finally {
            settings.dispose();
        }
    }

    /**
     * An absent palette setting reads back as the (null) key default; the
     * consumer treats that as "no stored colors" instead of failing.
     */
    @Test
    void absentUserPaletteColorsReadAsNull(@TempDir Path userDir) {
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            SettingsBinding<JsonNode> binding = settings.bind(SettingsKeys.USER_PALETTE_COLORS_FG_BG_A);
            assertTrue(binding.get() == null || binding.get().isNull());
            assertFalse(binding.isModified());
        } finally {
            settings.dispose();
        }
    }
}