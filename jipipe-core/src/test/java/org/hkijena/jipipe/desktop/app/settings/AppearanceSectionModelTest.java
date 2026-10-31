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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link AppearanceSectionModel}, the pure (non-Swing) part of the
 * merged Appearance / Accessibility / Keyboard sections: which settings blocks
 * each section owns, the "modified" check and the per-section reset.
 * <p>
 * The reset removes exactly the section's owned blocks from the document, so
 * every parameter of those sheets falls back to its default (the sheet bridge
 * re-applies the document into the live sheet) while sibling blocks — other
 * sections, the runtime scope, foreign pass-through blocks — stay untouched.
 */
class AppearanceSectionModelTest {

    private static final String GENERAL_UI_BLOCK = "sheet:" + AppearanceSectionModel.GENERAL_UI_SHEET_ID;
    private static final String FILE_CHOOSER_BLOCK = "sheet:" + AppearanceSectionModel.FILE_CHOOSER_SHEET_ID;

    /**
     * The brief's model test: a modified theme (written through a binding, the
     * same way the UI writes it) is reported as modified, and the reset
     * restores the default and clears the badge.
     */
    @Test
    void resetRestoresDefaultsAndClearsModified() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        AppearanceSectionModel model = AppearanceSectionModel.withOwnedBlocks(document, List.of(GENERAL_UI_BLOCK, FILE_CHOOSER_BLOCK));

        SettingsBinding<String> themeBinding = new SettingsBinding<>(document,
                AppearanceSectionModel.THEME_KEY);
        try {
            assertFalse(model.isModified(), "an untouched document has no modified section");
            assertEquals("Modern", themeBinding.get(), "the theme default");

            themeBinding.set("Dark");
            assertTrue(model.isModified());
            assertEquals("Dark", themeBinding.get());

            model.resetAll();

            assertFalse(model.isModified());
            assertEquals("Modern", themeBinding.get());
        } finally {
            themeBinding.close();
        }
    }

    @Test
    void mergedSectionsContainThemeAndFileChooserEntries() {
        AppearanceSectionModel model = AppearanceSectionModel.allSections(new SettingsDocument(SettingsScope.APPLICATION));
        assertTrue(model.sectionIds().containsAll(List.of("appearance", "accessibility", "keyboard")));

        // The merged Appearance section composes parameters of both registered
        // sheets: the theme of the general UI sheet and the file dialog design
        // of the file chooser sheet.
        assertTrue(AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS.contains("theme-v2"));
        assertTrue(AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS.contains("file-chooser-type-v2"));
        assertTrue(AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS.contains("file-chooser-type-fallback"));
        assertTrue(AppearanceSectionModel.ACCESSIBILITY_PARAMETER_KEYS.contains("reduced-motion"));
        // Every key is offered exactly once across the merged sections.
        assertEquals(AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS.size(),
                AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS.stream().distinct().count());
        assertEquals(0, AppearanceSectionModel.ACCESSIBILITY_PARAMETER_KEYS.stream()
                .filter(AppearanceSectionModel.APPEARANCE_PARAMETER_KEYS::contains).count(),
                "a parameter must not appear in two sections");
    }

    /**
     * The reset removes exactly the owned blocks: foreign blocks (another
     * section, a pass-through registry block, an unknown-state block) and the
     * other scope stay present.
     */
    @Test
    void resetRemovesExactlyTheOwnedBlocksAndLeavesSiblingsIntact() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        document.set(GENERAL_UI_BLOCK, "theme-v2", com.fasterxml.jackson.databind.node.TextNode.valueOf("Dark"));
        document.set(FILE_CHOOSER_BLOCK, "file-chooser-type-v2", com.fasterxml.jackson.databind.node.TextNode.valueOf("Standard"));
        document.set("sheet:some.other:section", "value", com.fasterxml.jackson.databind.node.IntNode.valueOf(1));
        document.set("registry:ui-graph-editor", "any/dock/show-labels", com.fasterxml.jackson.databind.node.BooleanNode.FALSE);
        document.set("state:recent-projects", "", com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());

        AppearanceSectionModel model = AppearanceSectionModel.withOwnedBlocks(document, List.of(GENERAL_UI_BLOCK, FILE_CHOOSER_BLOCK));
        assertTrue(model.isModified());
        assertTrue(model.modifiedBlockPaths().containsAll(List.of(GENERAL_UI_BLOCK, FILE_CHOOSER_BLOCK)));

        model.resetAll();

        assertFalse(document.hasBlock(GENERAL_UI_BLOCK));
        assertFalse(document.hasBlock(FILE_CHOOSER_BLOCK));
        assertFalse(model.isModified());
        assertTrue(model.modifiedBlockPaths().isEmpty());
        assertTrue(document.hasBlock("sheet:some.other:section"), "sibling sheet block survives");
        assertTrue(document.hasBlock("registry:ui-graph-editor"), "registry block survives");
        assertTrue(document.hasBlock("state:recent-projects"), "state block survives");
    }

    /**
     * Resetting when nothing is modified is a no-op and fires no document
     * change (so no spurious auto-save).
     */
    @Test
    void resetOfAnUnmodifiedSectionWritesNothing() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        document.set("registry:ui-graph-editor", "any/dock/show-labels", com.fasterxml.jackson.databind.node.BooleanNode.TRUE);
        AppearanceSectionModel model = AppearanceSectionModel.withOwnedBlocks(document, List.of(GENERAL_UI_BLOCK, FILE_CHOOSER_BLOCK));
        assertFalse(model.isModified());

        List<String> writes = new java.util.ArrayList<>();
        document.addPathListener(GENERAL_UI_BLOCK, n -> writes.add(GENERAL_UI_BLOCK));
        document.addPathListener(FILE_CHOOSER_BLOCK, n -> writes.add(FILE_CHOOSER_BLOCK));

        model.resetAll();
        assertTrue(writes.isEmpty(), "an unmodified section's reset must not touch the document");
        assertTrue(document.hasBlock("registry:ui-graph-editor"));
    }

    /**
     * A section with no owned block reports no sections — the UI then shows a
     * single, non-resettable view.
     */
    @Test
    void sectionsWithoutOwnedBlocksAreNotListedAsModified() {
        AppearanceSectionModel all = AppearanceSectionModel.allSections(new SettingsDocument(SettingsScope.APPLICATION));
        assertEquals(List.of("appearance", "accessibility", "keyboard"), all.sectionIds());
        assertFalse(all.isModified());

        AppearanceSectionModel keyboardOnly = all.forSection("keyboard");
        assertFalse(keyboardOnly.isModified());
        assertTrue(keyboardOnly.ownedBlockPaths().isEmpty());
        assertTrue(keyboardOnly.sectionIds().isEmpty(), "a placeholder section owns no sheet");
    }

    /**
     * The UI resolves the composed sheets by the ids and keys this model
     * declares; a typo would silently drop a control. Pins the registered
     * instances (the composition must never build new sheets) and the exact
     * parameter keys of both composed sheets.
     */
    @Test
    void declaredSheetIdsAndKeysResolveAgainstTheRegisteredSheets() {
        JIPipe.ensureInstance();
        JIPipeGeneralUIApplicationSettings generalUi = JIPipe.getSettings()
                .getById(AppearanceSectionModel.GENERAL_UI_SHEET_ID, JIPipeGeneralUIApplicationSettings.class);
        JIPipeFileChooserApplicationSettings fileChooser = JIPipe.getSettings()
                .getById(AppearanceSectionModel.FILE_CHOOSER_SHEET_ID, JIPipeFileChooserApplicationSettings.class);
        assertNotNull(generalUi, "the general UI sheet is registered under the declared id");
        assertNotNull(fileChooser, "the file chooser sheet is registered under the declared id");
        assertSame(JIPipeGeneralUIApplicationSettings.getInstance(), generalUi,
                "the composition addresses the one live sheet instance");

        assertTrue(JIPipeParameterTree.getParameters(generalUi).keySet()
                        .containsAll(AppearanceSectionModel.APPEARANCE_GENERAL_UI_PARAMETER_KEYS),
                "every declared appearance key exists on the general UI sheet");
        assertTrue(JIPipeParameterTree.getParameters(fileChooser).keySet()
                        .containsAll(AppearanceSectionModel.APPEARANCE_FILE_CHOOSER_PARAMETER_KEYS),
                "every declared appearance key exists on the file chooser sheet");
        assertTrue(JIPipeParameterTree.getParameters(generalUi).keySet()
                        .containsAll(AppearanceSectionModel.ACCESSIBILITY_PARAMETER_KEYS),
                "every declared accessibility key exists on the general UI sheet");
    }

    /**
     * The three merged sections are registered application sheets of one
     * category, so they are first-class in the section tree, the scope routing
     * and the persistence — the section UI replaces their form with the
     * composition (presence check, not a registry count: plugins may add
     * sheets).
     */
    @Test
    void mergedSectionsAreRegisteredInOneCategory() {
        JIPipe.ensureInstance();
        for (AppearanceSectionModel.Section section : AppearanceSectionModel.SECTIONS) {
            JIPipeApplicationSettingsSheet sheet = JIPipe.getSettings()
                    .getById(section.mergedSheetId(), JIPipeApplicationSettingsSheet.class);
            assertNotNull(sheet, "merged section '" + section.id() + "' is a registered sheet");
            assertEquals(AppearanceSectionModel.CATEGORY_APPEARANCE_AND_ACCESSIBILITY, sheet.getCategory(),
                    "all merged sections share one category");
        }
    }
}