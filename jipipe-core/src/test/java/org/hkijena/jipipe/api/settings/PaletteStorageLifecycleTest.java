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

package org.hkijena.jipipe.api.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.contextpanel.JIPipeDesktopGraphEditorContextPanelIsland;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopRegistryBackedColorPaletteUserColorStorage;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Task 16 (fix round): the user palette color storage is reconstructed on
 * every graph-editor context-panel rebuild (i.e. on every node selection while
 * a note tool is active), so its {@code SettingsBinding} must be closed when
 * the owning island is torn down — otherwise every rebuild leaks one document
 * path listener.
 * <p>
 * This pins the non-live <em>mechanism</em>: constructing and disposing the
 * storage is safe without a service (no binding), the palette path mapping
 * never throws, and the island teardown seam disposes exactly the previous
 * pass once. The live "dispose closes the binding" assertion needs a running
 * service and lives in {@code jipipe-launcher}'s
 * {@code SettingsPaletteKeysTest}. The Swing wiring (the context panel calling
 * the seam in {@code rebuild()}) is verified by code review — see
 * {@code JIPipeDesktopGraphEditorContextPanel.rebuild()}.
 */
class PaletteStorageLifecycleTest {

    private static JIPipeDesktopRegistryBackedColorPaletteUserColorStorage newStorage() {
        // The exact palette configuration the note tool panels use: flags NONE
        // (foreground + background selection, no alpha) -> the "fg" sub-path.
        JIPipeDesktopColorPaletteUI paletteUI = new JIPipeDesktopColorPaletteUI(null,
                JIPipeDesktopColorPaletteUI.NONE, JIPipeDesktopColorPalette.PASTEL);
        return new JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(paletteUI);
    }

    /**
     * Without a running settings service the storage works in memory: building
     * and disposing it (the rebuild path) must neither bind nor throw.
     */
    @Test
    void storageWorksWithoutASettingsService(@TempDir Path userDir) {
        Assumptions.assumeFalse(JIPipe.isInstantiated(), "a live service is covered by the launcher test");
        JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
        try {
            String fgPath = SettingsDocument.listenerPathForTesting("registry:ui-palette", "user-palette-colors/fg");
            String fgBgPath = SettingsDocument.listenerPathForTesting("registry:ui-palette", "user-palette-colors/fg-bg");
            for (int i = 0; i < 5; i++) {
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage storage = newStorage();
                assertEquals(0, settings.document(SettingsScope.APPLICATION).pathListenerCount(fgBgPath),
                        "without a service the storage holds no binding (no listener to leak)");
                assertEquals(0, settings.document(SettingsScope.APPLICATION).pathListenerCount(fgPath));
                storage.dispose();
                storage.dispose();
            }
        } finally {
            settings.dispose();
        }
    }

    /**
     * The palette path mapping is called from the storage's constructor, so an
     * unexpected path must fall back to the foreground key instead of throwing.
     */
    @Test
    void unknownPalettePathFallsBackToTheForegroundKey() {
        assertEquals(SettingsKeys.USER_PALETTE_COLORS_FG,
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.getKey(
                        JIPipePathMetadataStore.key("user-palette-colors", "not-a-variant")));
        assertEquals(SettingsKeys.USER_PALETTE_COLORS_FG,
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.getKey(
                        JIPipePathMetadataStore.key("something-else", "fg")));
        assertEquals(SettingsKeys.USER_PALETTE_COLORS_FG,
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.getKey(
                        JIPipePathMetadataStore.key("user-palette-colors", "fg")));
        // The four known variants are unchanged.
        assertEquals(SettingsKeys.USER_PALETTE_COLORS_FG_A,
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.getKey(
                        JIPipePathMetadataStore.key("user-palette-colors", "fg-a")));
        assertEquals(SettingsKeys.USER_PALETTE_COLORS_FG_BG_A,
                JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.getKey(
                        JIPipePathMetadataStore.key("user-palette-colors", "fg-bg-a")));
    }

    /**
     * The island teardown seam: a rebuilt context panel disposes exactly the
     * islands of the previous pass, exactly once, and starts a fresh list.
     */
    @Test
    void islandTeardownDisposesThePreviousPassOnce() {
        List<FakeIsland> islands = new ArrayList<>();
        FakeIsland a = new FakeIsland();
        FakeIsland b = new FakeIsland();
        islands.add(a);
        islands.add(b);

        List<FakeIsland> previous = JIPipeDesktopGraphEditorContextPanelIsland.takePreviousAndClear(islands);
        assertEquals(List.of(a, b), previous, "the previous pass is handed back");
        assertEquals(0, islands.size(), "the tracking list is cleared for the new pass");
        JIPipeDesktopGraphEditorContextPanelIsland.disposeAll(previous);
        assertEquals(1, a.disposeCount);
        assertEquals(1, b.disposeCount);

        // A next pass with no islands: nothing to dispose, nothing thrown.
        assertEquals(List.of(), JIPipeDesktopGraphEditorContextPanelIsland.takePreviousAndClear(islands));
        JIPipeDesktopGraphEditorContextPanelIsland.disposeAll(List.of());
    }

    /**
     * The base {@code dispose()} is a no-op; a subclass override receives
     * every call (it guards its own resources, and the storage's
     * {@code dispose()} is itself idempotent).
     */
    @Test
    void islandBaseDisposeIsIdempotent() {
        FakeIsland island = new FakeIsland();
        island.dispose();
        island.dispose();
        assertEquals(2, island.disposeCount);
    }

    /**
     * A minimal island. The base constructor reads
     * {@code graphEditorUI.getDesktopWorkbench()}, so a
     * {@link JIPipeDesktopGraphEditorUI} instance must exist; a Mockito mock
     * (whose constructor never runs) returns a {@code null} workbench without
     * constructing any Swing UI.
     */
    private static class FakeIsland extends JIPipeDesktopGraphEditorContextPanelIsland {
        private int disposeCount;

        FakeIsland() {
            super(mock(JIPipeDesktopGraphEditorUI.class));
        }

        @Override
        protected Icon getTitleIcon() {
            return null;
        }

        @Override
        protected String getTitle() {
            return "fake";
        }

        @Override
        public void dispose() {
            disposeCount++;
            super.dispose();
        }
    }
}
