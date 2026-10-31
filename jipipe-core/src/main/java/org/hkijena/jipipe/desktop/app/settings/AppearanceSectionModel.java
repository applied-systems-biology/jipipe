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
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pure (non-Swing) model of the merged <em>Appearance</em>,
 * <em>Accessibility</em> and <em>Keyboard</em> sections of the settings tab
 * (modern settings system, Task 19).
 * <p>
 * <b>Composition.</b> The three sections are registered as ordinary
 * application sheets of the category
 * {@link #CATEGORY_APPEARANCE_AND_ACCESSIBILITY}. The <em>Appearance</em>
 * section composes parameters of two <em>already registered</em> sheets —
 * {@code JIPipeGeneralUIApplicationSettings} (theme, theme style) and
 * {@code JIPipeFileChooserApplicationSettings} (file dialog design + fallback);
 * the <em>Accessibility</em> section shows the general UI sheet's reduced motion
 * and owns the UI-scale control (which is <em>not</em> a settings key: it is
 * stored in the jaunch config files, see
 * {@code JIPipeDesktopAppearanceSection}); the <em>Keyboard</em> section is a
 * placeholder. Which keys are shown per sheet is declared by this model and
 * read by the section UI — the UI never invents a key.
 * <p>
 * <b>Modification and reset.</b> A section owns the sheets it composes. A block
 * is present in the document exactly when its sheet deviates from its defaults
 * (the sheet bridge merges the serialized sheet into the block), so
 * {@link #isModified()} is "any owned block present" and
 * {@link #resetAll()} removes exactly those blocks — the bridge then re-applies
 * the (now absent) block into the live sheet, restoring defaults, and the
 * settings engine auto-saves the removal like any other local write.
 * <p>
 * The model is either the <em>catalog</em> ({@link #allSections(SettingsDocument)}:
 * all three sections, {@link #sectionIds()} lists them) or one section's view
 * ({@link #forSection(String)}), which is what a section header binds to.
 */
public final class AppearanceSectionModel {

    /**
     * The id of the merged appearance section.
     */
    public static final String SECTION_APPEARANCE = "appearance";
    /**
     * The id of the accessibility section.
     */
    public static final String SECTION_ACCESSIBILITY = "accessibility";
    /**
     * The id of the keyboard placeholder section.
     */
    public static final String SECTION_KEYBOARD = "keyboard";

    /**
     * The category the three merged sections are registered in.
     */
    public static final String CATEGORY_APPEARANCE_AND_ACCESSIBILITY = "Appearance & accessibility";

    /**
     * The registration id of the general UI sheet (persisted in profiles).
     */
    public static final String GENERAL_UI_SHEET_ID = "org.hkijena.jipipe:general-ui";
    /**
     * The registration id of the file chooser sheet (persisted in profiles).
     */
    public static final String FILE_CHOOSER_SHEET_ID = "org.hkijena.jipipe:file-chooser";

    /**
     * The general UI sheet's block in the document.
     */
    public static final String GENERAL_UI_BLOCK = "sheet:" + GENERAL_UI_SHEET_ID;
    /**
     * The file chooser sheet's block in the document.
     */
    public static final String FILE_CHOOSER_BLOCK = "sheet:" + FILE_CHOOSER_SHEET_ID;

    /**
     * The theme key. Declared here (not as a registered setting) only so the
     * model's tests can observe a theme change without Swing; the UI itself
     * edits the sheet parameters through the sheet's own
     * {@code ParameterChangedEvent}s and the sheet bridge.
     */
    public static final JIPipeSettingKey<String> THEME_KEY = JIPipeSettingKey.string(GENERAL_UI_BLOCK, "theme-v2", String.class)
            .defaultValue("Modern")
            .name("Theme")
            .description("The theme that is used for the user interface");

    /**
     * The general UI parameters shown in the merged appearance section (in
     * display order). Keys are the sheets' serialization keys.
     */
    public static final List<String> APPEARANCE_GENERAL_UI_PARAMETER_KEYS = List.of("theme-v2", "theme-style");

    /**
     * The file chooser parameters shown in the merged appearance section.
     */
    public static final List<String> APPEARANCE_FILE_CHOOSER_PARAMETER_KEYS = List.of("file-chooser-type-v2", "file-chooser-type-fallback");

    /**
     * All parameters shown in the merged appearance section, across both
     * composed sheets (general UI first, then the file chooser).
     */
    public static final List<String> APPEARANCE_PARAMETER_KEYS = java.util.stream.Stream.concat(
            APPEARANCE_GENERAL_UI_PARAMETER_KEYS.stream(), APPEARANCE_FILE_CHOOSER_PARAMETER_KEYS.stream()).toList();

    /**
     * The general UI parameters shown in the accessibility section.
     */
    public static final List<String> ACCESSIBILITY_PARAMETER_KEYS = List.of("reduced-motion");

    /**
     * The empty-state text of the keyboard placeholder (no keyboard settings
     * exist yet; shortcuts are tracked in issue #1346).
     */
    public static final String KEYBOARD_EMPTY_STATE = "Shortcut profiles will be available here (#1346)";

    /**
     * The three sections, in display order: the registered merged sheet that
     * hosts the section and the <em>composed</em> sheets whose parameters it
     * shows (which is what a section's reset reverts).
     */
    public static final List<Section> SECTIONS = List.of(
            new Section(SECTION_APPEARANCE, "org.hkijena.jipipe:desktop-appearance",
                    List.of(GENERAL_UI_SHEET_ID, FILE_CHOOSER_SHEET_ID)),
            new Section(SECTION_ACCESSIBILITY, "org.hkijena.jipipe:desktop-accessibility",
                    List.of(GENERAL_UI_SHEET_ID)),
            new Section(SECTION_KEYBOARD, "org.hkijena.jipipe:desktop-keyboard",
                    List.of()));

    private final SettingsDocument document;
    private final List<String> ownedBlockPaths;
    private final List<String> sectionIds;

    private AppearanceSectionModel(SettingsDocument document, List<String> ownedBlockPaths, List<String> sectionIds) {
        this.document = document;
        this.ownedBlockPaths = List.copyOf(ownedBlockPaths);
        this.sectionIds = List.copyOf(sectionIds);
    }

    /**
     * The catalog of the merged sections.
     *
     * @param document the application settings document
     * @return a model listing all three sections; performing a reset on it
     * resets every owned block of every section
     */
    public static AppearanceSectionModel allSections(SettingsDocument document) {
        List<String> blocks = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Section section : SECTIONS) {
            ids.add(section.id());
            for (String sheetId : section.sheetIds()) {
                blocks.add("sheet:" + sheetId);
            }
        }
        return new AppearanceSectionModel(document, blocks, ids);
    }

    /**
     * A model over an explicit set of owned blocks (no section ids). Used by
     * the section UI for one composed section and by tests.
     *
     * @param document        the settings document
     * @param ownedBlockPaths the block paths the section owns
     * @return the model
     */
    public static AppearanceSectionModel withOwnedBlocks(SettingsDocument document, List<String> ownedBlockPaths) {
        Objects.requireNonNull(document, "document");
        return new AppearanceSectionModel(document, ownedBlockPaths, List.of());
    }

    /**
     * Creates a model for a single section, holding only the blocks that
     * section owns.
     *
     * @param document  the application settings document
     * @param sectionId one of {@link #SECTION_APPEARANCE},
     *                  {@link #SECTION_ACCESSIBILITY} or {@link #SECTION_KEYBOARD}
     * @return the section's model; the keyboard placeholder owns no sheet
     * @throws IllegalArgumentException if the id is unknown
     */
    public static AppearanceSectionModel forSection(SettingsDocument document, String sectionId) {
        return allSections(document).forSection(sectionId);
    }

    /**
     * @param sectionId the section id
     * @return the section's definition
     * @throws IllegalArgumentException if the id is unknown
     */
    public static Section section(String sectionId) {
        for (Section section : SECTIONS) {
            if (section.id().equals(sectionId)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown settings section: " + sectionId);
    }

    /**
     * The parameters a section shows from each of its sheets. The merged
     * appearance section is composed of general UI and file chooser keys, the
     * accessibility section of general UI keys (plus its UI-scale control), the
     * keyboard placeholder of none.
     *
     * @param sectionId the section id
     * @return the sheet block path → the parameter keys shown from that sheet
     * (in display order); empty for the placeholder
     * @throws IllegalArgumentException if the id is unknown
     */
    public static Map<String, List<String>> parametersBySheet(String sectionId) {
        return switch (sectionId) {
            case SECTION_APPEARANCE -> {
                Map<String, List<String>> result = new LinkedHashMap<>();
                result.put(GENERAL_UI_BLOCK, APPEARANCE_GENERAL_UI_PARAMETER_KEYS);
                result.put(FILE_CHOOSER_BLOCK, APPEARANCE_FILE_CHOOSER_PARAMETER_KEYS);
                yield result;
            }
            case SECTION_ACCESSIBILITY -> Map.of(GENERAL_UI_BLOCK, ACCESSIBILITY_PARAMETER_KEYS);
            case SECTION_KEYBOARD -> Map.of();
            default -> throw new IllegalArgumentException("Unknown settings section: " + sectionId);
        };
    }

    /**
     * @param sectionId the section id
     * @return the sheets (block paths) the section's parameters come from; empty for the placeholder
     * @throws IllegalArgumentException if the id is unknown
     */
    public static List<String> ownedBlocksOf(String sectionId) {
        List<String> result = new ArrayList<>();
        for (String sheetId : section(sectionId).sheetIds()) {
            result.add("sheet:" + sheetId);
        }
        return result;
    }

    /**
     * @return a model restricted to one section (see {@link #forSection(String)})
     */
    public AppearanceSectionModel forSection(String sectionId) {
        List<String> blocks = ownedBlocksOf(sectionId);
        if (blocks.isEmpty()) {
            return new AppearanceSectionModel(document, List.of(), List.of());
        }
        return new AppearanceSectionModel(document, blocks, List.of(sectionId));
    }

    /**
     * @return the ids of the sections this model covers (empty for a
     * single-section view, all three for the catalog)
     */
    public List<String> sectionIds() {
        return sectionIds;
    }

    /**
     * @return the block paths of the sheets this model's sections own
     */
    public List<String> ownedBlockPaths() {
        return ownedBlockPaths;
    }

    /**
     * @return whether any owned block is present in the document, i.e. whether
     * the section holds a value that deviates from the sheet defaults
     */
    public boolean isModified() {
        for (String blockPath : ownedBlockPaths) {
            if (document.hasBlock(blockPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the owned blocks present in the document, in ownership order
     */
    public List<String> modifiedBlockPaths() {
        List<String> result = new ArrayList<>();
        for (String blockPath : ownedBlockPaths) {
            if (document.hasBlock(blockPath)) {
                result.add(blockPath);
            }
        }
        return result;
    }

    /**
     * Removes the owned blocks from the document, so every parameter of the
     * section falls back to its default. Blocks that are already absent are
     * not touched (no document change, no auto-save).
     */
    public void resetAll() {
        for (String blockPath : modifiedBlockPaths()) {
            document.removeBlock(blockPath);
        }
    }

    /**
     * Subscribes to changes of the owned blocks, so the "modified" badge can be
     * re-evaluated. A block appearing or disappearing notifies through the
     * block-level listener. The subscription is removed by
     * {@link #unsubscribe(Consumer)}; call both with the same listener instance
     * (store it in a field).
     *
     * @param listener invoked on every change of an owned block
     */
    public void subscribe(Consumer<JsonNode> listener) {
        Objects.requireNonNull(listener, "listener");
        for (String blockPath : ownedBlocks()) {
            document.addPathListener(blockPath, listener);
        }
    }

    /**
     * Removes a subscription created by {@link #subscribe(Consumer)}.
     *
     * @param listener the listener passed to {@link #subscribe(Consumer)}
     */
    public void unsubscribe(Consumer<JsonNode> listener) {
        for (String blockPath : ownedBlocks()) {
            document.removePathListener(blockPath, listener);
        }
    }

    /**
     * @return the document this model operates on
     */
    public SettingsDocument document() {
        return document;
    }

    /**
     * @return the owned blocks, distinct and in ownership order
     */
    public Set<String> ownedBlocks() {
        return new LinkedHashSet<>(ownedBlockPaths);
    }

    /**
     * One of the merged sections.
     *
     * @param id           the stable section id ({@link #SECTION_APPEARANCE}, ...)
     * @param mergedSheetId the registration id of the merged sheet hosting the
     *                      section (persisted in profiles)
     * @param sheetIds     the registration ids of the <em>composed</em> sheets
     *                     the section's parameters come from (empty for the
     *                     placeholder)
     */
    public record Section(String id, String mergedSheetId, List<String> sheetIds) {
        public Section {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(mergedSheetId, "mergedSheetId");
            sheetIds = List.copyOf(sheetIds);
        }
    }
}