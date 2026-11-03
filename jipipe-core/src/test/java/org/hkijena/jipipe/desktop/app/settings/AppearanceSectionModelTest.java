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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link AppearanceSectionModel}, the pure (non-Swing) part of the
 * merged Appearance / Accessibility / Keyboard sections.
 * <p>
 * <b>Modification is per displayed key, relative to its default.</b> The sheet
 * bridge materializes defaults into the document (it seeds a block on attach and
 * flushes the whole sheet on every change), so "a block is present" is NOT a
 * modification signal — a freshly opened profile has every block present while
 * every value still equals its default. The only correct signal is "a displayed
 * key holds a value that differs from its default". A key the section does not
 * display belongs to the sheet's other consumers and must neither count as
 * modified nor be touched by the reset.
 * <p>
 * The defaults are read from throwaway fresh sheet instances (the same source
 * the section uses), so the tests exercise the real default values.
 */
class AppearanceSectionModelTest {

    private static final String GENERAL_UI_BLOCK = "sheet:" + AppearanceSectionModel.GENERAL_UI_SHEET_ID;
    private static final String FILE_CHOOSER_BLOCK = "sheet:" + AppearanceSectionModel.FILE_CHOOSER_SHEET_ID;

    /**
     * The defaults exactly as the section reads them: one fresh, unregistered
     * instance per composed sheet type (sheets have no constructor side
     * effects), each displayed key serialized with the settings mapper.
     */
    private static Map<String, JsonNode> freshDefaults() {
        Map<String, JsonNode> defaults = new LinkedHashMap<>();
        defaults.putAll(defaultsOf(new JIPipeGeneralUIApplicationSettings(),
                GENERAL_UI_BLOCK, AppearanceSectionModel.APPEARANCE_GENERAL_UI_PARAMETER_KEYS));
        defaults.putAll(defaultsOf(new JIPipeFileChooserApplicationSettings(),
                FILE_CHOOSER_BLOCK, AppearanceSectionModel.APPEARANCE_FILE_CHOOSER_PARAMETER_KEYS));
        return defaults;
    }

    private static Map<String, JsonNode> defaultsOf(JIPipeApplicationSettingsSheet freshSheet,
                                                    String blockPath,
                                                    List<String> keys) {
        Map<String, JIPipeParameterAccess> accesses = new JIPipeParameterTree(freshSheet).getParameters();
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (String key : keys) {
            JIPipeParameterAccess access = accesses.get(key);
            assertNotNull(access, "fresh sheet " + freshSheet.getId() + " declares '" + key + "'");
            result.put(blockPath + "/" + key, JsonUtils.getObjectMapper().valueToTree(access.get(Object.class)));
        }
        return result;
    }

    private static AppearanceSectionModel appearanceModel(SettingsDocument document) {
        return AppearanceSectionModel.forSection(document, AppearanceSectionModel.SECTION_APPEARANCE,
                freshDefaults());
    }

    private static String defaultTheme() {
        return freshDefaults().get(GENERAL_UI_BLOCK + "/theme-v2").asText();
    }

    /**
     * (a) The seeded-defaults case: the bridge materializes the full sheet into
     * the document on first open, so every block is present while every value
     * still equals its default. That is NOT a modification.
     */
    @Test
    void blockHoldingOnlyDefaultsIsNotModified() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        Map<String, JsonNode> defaults = freshDefaults();
        // What attach()/flushToDocument() leave behind: the whole sheet, defaults included.
        defaults.forEach((path, value) -> {
            int slash = path.indexOf('/');
            document.set(path.substring(0, slash), path.substring(slash + 1), value);
        });
        // Plus keys of the same sheets the section does NOT display, also at their defaults.
        document.set(GENERAL_UI_BLOCK, "validate-on-save", BooleanNode.FALSE);

        AppearanceSectionModel model = appearanceModel(document);
        assertTrue(document.hasBlock(GENERAL_UI_BLOCK), "precondition: the materialized block is present");
        assertFalse(model.isModified(), "a block holding only default values is not a modification");
    }

    /**
     * (b) One displayed key at a non-default value modifies the section;
     * writing the default back clears it.
     */
    @Test
    void aDisplayedKeyChangeModifiesTheSectionAndItsDefaultClearsIt() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        AppearanceSectionModel model = appearanceModel(document);
        SettingsBinding<String> theme = new SettingsBinding<>(document, AppearanceSectionModel.THEME_KEY);
        try {
            assertFalse(model.isModified());

            theme.set("Dark");
            assertTrue(model.isModified(), "a non-default displayed value is a modification");
            assertEquals("Dark", theme.get());

            theme.set(defaultTheme());
            assertFalse(model.isModified(), "writing the default back clears the modification");
        } finally {
            theme.close();
        }
    }

    /**
     * (c) Displayed-only scoping: a non-displayed key of a composed sheet at a
     * non-default value belongs to the sheet's other consumers and must not mark
     * the section as modified.
     */
    @Test
    void nonDisplayedKeysOfAComposedSheetDoNotCountAsModified() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        // validate-on-save's default is false; true is a genuine user change.
        document.set(GENERAL_UI_BLOCK, "validate-on-save", BooleanNode.TRUE);

        AppearanceSectionModel model = appearanceModel(document);
        assertFalse(model.isModified(), "a non-displayed key is outside this section");

        SettingsBinding<Boolean> validateOnSave = new SettingsBinding<>(document,
                JIPipeSettingKey.bool(GENERAL_UI_BLOCK, "validate-on-save", Boolean.class).defaultValue(false));
        try {
            assertEquals(true, validateOnSave.get());
        } finally {
            validateOnSave.close();
        }
    }

    /**
     * (d) The reset-clobber regression: the reset removes ONLY the displayed
     * keys; a non-displayed key of the same composed sheet keeps its stored
     * value, and other blocks are untouched.
     */
    @Test
    void resetRemovesOnlyTheDisplayedKeysAndKeepsNonDisplayedValues() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        document.set(GENERAL_UI_BLOCK, "theme-v2", TextNode.valueOf("Dark"));
        document.set(FILE_CHOOSER_BLOCK, "file-chooser-type-v2", TextNode.valueOf("Standard"));
        // Non-displayed keys of the SAME composed sheets: must survive the reset.
        document.set(GENERAL_UI_BLOCK, "validate-on-save", BooleanNode.TRUE);
        document.set(GENERAL_UI_BLOCK, "show-introduction", BooleanNode.FALSE);
        document.set(FILE_CHOOSER_BLOCK, "add-file-extension", BooleanNode.FALSE);
        // Foreign blocks: must survive too.
        document.set("sheet:some.other:section", "value", IntNode.valueOf(1));
        document.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.FALSE);
        document.set("state:recent-projects", "", JsonUtils.getObjectMapper().createArrayNode());

        AppearanceSectionModel model = appearanceModel(document);
        assertTrue(model.isModified());

        model.resetAll();

        assertFalse(model.isModified());
        assertTrue(document.get(GENERAL_UI_BLOCK, "theme-v2").isMissingNode(),
                "the displayed theme key is removed");
        assertTrue(document.get(FILE_CHOOSER_BLOCK, "file-chooser-type-v2").isMissingNode(),
                "the displayed file chooser key is removed");

        assertEquals(BooleanNode.TRUE, document.get(GENERAL_UI_BLOCK, "validate-on-save"),
                "a non-displayed key of a composed sheet is NOT clobbered by the reset");
        assertEquals(BooleanNode.FALSE, document.get(GENERAL_UI_BLOCK, "show-introduction"),
                "a second non-displayed key survives");
        assertEquals(BooleanNode.FALSE, document.get(FILE_CHOOSER_BLOCK, "add-file-extension"),
                "a non-displayed key of the file chooser sheet survives");
        assertTrue(document.hasBlock("sheet:some.other:section"), "sibling sheet block survives");
        assertTrue(document.hasBlock("registry:ui-graph-editor"), "registry block survives");
        assertTrue(document.hasBlock("state:recent-projects"), "state block survives");
    }

    /**
     * The brief's reset test: after a modified theme, the reset restores the
     * default (through a binding, exactly how the document reads it) and clears
     * the badge.
     */
    @Test
    void resetRestoresDefaultsAndClearsModified() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        AppearanceSectionModel model = appearanceModel(document);

        SettingsBinding<String> themeBinding = new SettingsBinding<>(document, AppearanceSectionModel.THEME_KEY);
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
        AppearanceSectionModel model = AppearanceSectionModel.allSections(new SettingsDocument(SettingsScope.APPLICATION),
                freshDefaults());
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
     * Resetting an unmodified section writes nothing (no spurious auto-save).
     */
    @Test
    void resetOfAnUnmodifiedSectionWritesNothing() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        Map<String, JsonNode> defaults = freshDefaults();
        defaults.forEach((path, value) -> {
            int slash = path.indexOf('/');
            document.set(path.substring(0, slash), path.substring(slash + 1), value);
        });
        AppearanceSectionModel model = appearanceModel(document);
        assertFalse(model.isModified());

        List<String> writes = new java.util.ArrayList<>();
        Consumer<JsonNode> listener = node -> writes.add("change");
        model.subscribe(listener);
        try {
            model.resetAll();
            assertTrue(writes.isEmpty(), "an unmodified section's reset must not touch the document");
        } finally {
            model.unsubscribe(listener);
        }
    }

    /**
     * The badge refresh must fire on a per-key change and on a per-key removal
     * (the reset), and stop after {@code unsubscribe}.
     */
    @Test
    void badgeListenerFiresOnPerKeyChangesAndStopsOnUnsubscribe() {
        SettingsDocument document = new SettingsDocument(SettingsScope.APPLICATION);
        AppearanceSectionModel model = appearanceModel(document);
        List<String> events = new java.util.ArrayList<>();
        Consumer<JsonNode> listener = node -> events.add(node.toString());
        model.subscribe(listener);
        try {
            document.set(GENERAL_UI_BLOCK, "theme-v2", TextNode.valueOf("Dark"));
            assertEquals(1, events.size(), "a displayed key change notifies the badge");
            assertTrue(model.isModified());

            model.resetAll();
            assertTrue(events.size() >= 2, "the per-key removal notifies the badge too");
            assertFalse(model.isModified());
        } finally {
            model.unsubscribe(listener);
        }
        int sizeAfterUnsubscribe = events.size();
        document.set(GENERAL_UI_BLOCK, "theme-v2", TextNode.valueOf("Dark"));
        assertEquals(sizeAfterUnsubscribe, events.size(), "unsubscribe releases the listeners");
    }

    /**
     * A section with no composed sheet reports no sections — the UI then shows a
     * single, non-resettable view.
     */
    @Test
    void sectionsWithoutOwnedBlocksAreNotListedAsModified() {
        AppearanceSectionModel all = AppearanceSectionModel.allSections(new SettingsDocument(SettingsScope.APPLICATION),
                freshDefaults());
        assertEquals(List.of("appearance", "accessibility", "keyboard"), all.sectionIds());
        assertFalse(all.isModified());

        AppearanceSectionModel keyboardOnly = all.forSection("keyboard");
        assertFalse(keyboardOnly.isModified());
        assertTrue(keyboardOnly.ownedBlockPaths().isEmpty());
        assertTrue(keyboardOnly.displayedKeys().isEmpty(), "a placeholder section displays nothing");
        assertEquals(List.of(AppearanceSectionModel.SECTION_KEYBOARD), keyboardOnly.sectionIds(),
                "the section is still selectable (it just has nothing to show or reset)");
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