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

package org.hkijena.jipipe.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopRegistryBackedColorPaletteUserColorStorage;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Task 16 of the modern settings system: the user palette colors are a typed
 * setting. A write through the key must land at the frozen legacy location
 * ({@code registry:ui-palette} / {@code user-palette-colors/fg}) in the
 * application settings document — that is what makes the palette storage a
 * real consumer of the settings document instead of a separate store.
 * <p>
 * Lives in <b>jipipe-launcher</b> because it boots the real service
 * ({@link JIPipe#ensureInstance()}) and therefore runs against the
 * developer's real profile: the original value is restored in a
 * {@code finally} block, so the test never leaves the profile mutated.
 */
class SettingsPaletteKeysTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    @Test
    void paletteStorageUsesTheDocument() {
        JIPipe.ensureInstance();
        SettingsBinding<JsonNode> b = JIPipe.getSettingsService().bind(SettingsKeys.USER_PALETTE_COLORS_FG);
        JsonNode original = JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                .get("registry:ui-palette", "user-palette-colors/fg");
        try {
            b.set(JsonUtils.toJsonNode(List.of("#ff0000")));
            assertEquals("#ff0000", JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                    .get("registry:ui-palette", "user-palette-colors/fg").get(0).asText());
        } finally {
            // Restore the developer's real profile: the stored value if there
            // was one, the default (i.e. absent) otherwise.
            if (original.isMissingNode()) {
                b.reset();
            } else {
                b.set(original);
            }
            b.close();
        }
    }

    /**
     * Fix round (Finding 1): the palette storage is reconstructed on every
     * context-panel rebuild (every node selection with a note tool active), so
     * its {@code dispose()} must close the binding it holds. Observed through
     * the Task 15 listener seam on the live application document, and pinned
     * over several construct+dispose cycles (the rebuild path).
     * <p>
     * The note tool palette is created with flags {@code NONE}, which keeps
     * background selection enabled, so its storage path is
     * {@code user-palette-colors/fg-bg}.
     */
    @Test
    void disposingThePaletteStorageClosesItsBinding() {
        JIPipe.ensureInstance();
        SettingsDocument document = JIPipe.getSettingsService().document(SettingsScope.APPLICATION);
        String path = SettingsDocument.listenerPathForTesting("registry:ui-palette", "user-palette-colors/fg-bg");
        int baseline = document.pathListenerCount(path);
        try {
            for (int i = 0; i < 5; i++) {
                JIPipeDesktopColorPaletteUI paletteUI = new JIPipeDesktopColorPaletteUI(
                        null, JIPipeDesktopColorPaletteUI.NONE, JIPipeDesktopColorPalette.PASTEL);
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage storage =
                        new JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(paletteUI);
                assertEquals(baseline + 1, document.pathListenerCount(path),
                        "a live palette storage holds exactly one path listener");
                storage.dispose();
                assertEquals(baseline, document.pathListenerCount(path),
                        "dispose() must close the binding");
                storage.dispose();
                assertEquals(baseline, document.pathListenerCount(path),
                        "dispose() must be idempotent");
            }
        } finally {
            assertEquals(baseline, document.pathListenerCount(path));
        }
    }
}
