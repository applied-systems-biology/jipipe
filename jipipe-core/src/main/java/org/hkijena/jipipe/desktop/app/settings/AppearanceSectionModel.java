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
 * <b>Composition.</b> The three sections are registered as application sheets of
 * the category {@link #CATEGORY_APPEARANCE_AND_ACCESSIBILITY}; the <em>Appearance</em>
 * section composes <em>selected</em> parameters of two registered sheets —
 * {@code JIPipeGeneralUIApplicationSettings} (theme, theme style) and
 * {@code JIPipeFileChooserApplicationSettings} (file dialog design + fallback) —
 * the <em>Accessibility</em> section the general UI sheet's reduced motion (plus
 * the UI-scale control, which is not a settings key), the <em>Keyboard</em>
 * section is a placeholder. {@link #parametersBySheet(String)} declares the keys.
 * <p>
 * <b>Modification is per displayed key, relative to its default.</b> The sheet
 * bridge deliberately materializes defaults into the document: it seeds a block
 * from the whole sheet on attach and flushes the whole sheet on every change.
 * Block presence is therefore <em>not</em> a modification signal — a freshly
 * opened profile has every block present while every value still equals its
 * default. {@link #isModified()} answers "does any <em>displayed</em> key hold a
 * value that differs from its default?"; keys the section does not display
 * belong to the sheet's other consumers and neither count nor get touched.
 * <p>
 * <b>Reset.</b> {@link #resetAll()} removes only the displayed keys from the
 * document ({@link SettingsDocument#remove(String, String)} per key, never a
 * whole block). That cleans the stored state; the authoritative reset — driving
 * the live sheet parameters to their defaults — is the section's job, because
 * the bridge only applies keys that are <em>present</em> in the block and the
 * next save would re-flush the sheet's (unchanged) values.
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
     * shows.
     */
    public static final List<Section> SECTIONS = List.of(
            new Section(SECTION_APPEARANCE, "org.hkijena.jipipe:desktop-appearance",
                    List.of(GENERAL_UI_SHEET_ID, FILE_CHOOSER_SHEET_ID)),
            new Section(SECTION_ACCESSIBILITY, "org.hkijena.jipipe:desktop-accessibility",
                    List.of(GENERAL_UI_SHEET_ID)),
            new Section(SECTION_KEYBOARD, "org.hkijena.jipipe:desktop-keyboard",
                    List.of()));

    private final SettingsDocument document;
    /**
     * The displayed keys per section, as {@code (blockPath, keys)}. Only these
     * keys are evaluated and reset.
     */
    private final Map<String, List<Map.Entry<String, List<String>>>> displayedKeysBySection;
    /**
     * The default value of every displayed key, keyed by {@code blockPath + "/" + key}.
     */
    private final Map<String, JsonNode> defaults;
    private final List<String> sectionIds;

    private AppearanceSectionModel(SettingsDocument document,
                                   Map<String, List<Map.Entry<String, List<String>>>> displayedKeysBySection,
                                   Map<String, JsonNode> defaults,
                                   List<String> sectionIds) {
        this.document = Objects.requireNonNull(document, "document");
        this.displayedKeysBySection = displayedKeysBySection;
        this.defaults = Map.copyOf(defaults);
        this.sectionIds = List.copyOf(sectionIds);
    }

    /**
     * The id → composition of the three sections, in display order.
     *
     * @return section id → (block path → displayed keys)
     */
    public static Map<String, Map<String, List<String>>> allCompositions() {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        for (Section section : SECTIONS) {
            result.put(section.id(), parametersBySheet(section.id()));
        }
        return result;
    }

    /**
     * The catalog of the merged sections.
     *
     * @param document the application settings document
     * @param defaults the default value of every displayed key, keyed by
     *                 {@code blockPath + "/" + key} (read from throwaway fresh
     *                 sheet instances by the caller)
     * @return a model listing all three sections
     */
    public static AppearanceSectionModel allSections(SettingsDocument document, Map<String, JsonNode> defaults) {
        return new AppearanceSectionModel(document,
                toComposition(allCompositions()),
                defaults,
                List.of(SECTION_APPEARANCE, SECTION_ACCESSIBILITY, SECTION_KEYBOARD));
    }

    /**
     * Creates a model for a single section, holding only the keys that section
     * displays.
     *
     * @param document  the application settings document
     * @param sectionId one of {@link #SECTION_APPEARANCE},
     *                  {@link #SECTION_ACCESSIBILITY} or {@link #SECTION_KEYBOARD}
     * @param defaults  the default value of every displayed key, keyed by
     *                  {@code blockPath + "/" + key}
     * @return the section's model; the keyboard placeholder displays nothing
     * @throws IllegalArgumentException if the id is unknown
     */
    public static AppearanceSectionModel forSection(SettingsDocument document, String sectionId,
                                                    Map<String, JsonNode> defaults) {
        return allSections(document, defaults).forSection(sectionId);
    }

    /**
     * A model over an explicit composition (used by tests and by consumers
     * that compose their own keys).
     *
     * @param document              the settings document
     * @param displayedBySection    section id → (block path → displayed keys)
     * @param defaults              the default value per displayed key, keyed by
     *                              {@code blockPath + "/" + key}
     * @return the model
     */
    public static AppearanceSectionModel withComposition(SettingsDocument document,
                                                         Map<String, Map<String, List<String>>> displayedBySection,
                                                         Map<String, JsonNode> defaults) {
        return new AppearanceSectionModel(document, toComposition(displayedBySection), defaults,
                List.copyOf(displayedBySection.keySet()));
    }

    private static Map<String, List<Map.Entry<String, List<String>>>> toComposition(
            Map<String, Map<String, List<String>>> displayedBySection) {
        Map<String, List<Map.Entry<String, List<String>>>> result = new LinkedHashMap<>();
        displayedBySection.forEach((sectionId, bySheet) -> result.put(sectionId, List.copyOf(bySheet.entrySet())));
        return result;
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
     * @return the sheets (block paths) the section's parameters come from; empty
     * for the placeholder
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
     * @return a model restricted to one section (see {@link #forSection(String, Map)})
     */
    public AppearanceSectionModel forSection(String sectionId) {
        if (!displayedKeysBySection.containsKey(sectionId)) {
            // Unknown section in this composition (or the placeholder): nothing is displayed.
            return new AppearanceSectionModel(document, Map.of(), defaults, List.of());
        }
        Map<String, List<Map.Entry<String, List<String>>>> single = new LinkedHashMap<>();
        single.put(sectionId, displayedKeysBySection.get(sectionId));
        return new AppearanceSectionModel(document, single, defaults, List.of(sectionId));
    }

    /**
     * @return the ids of the sections this model covers (empty for a
     * single-section view, all three for the catalog)
     */
    public List<String> sectionIds() {
        return sectionIds;
    }

    /**
     * @return the block paths of the sheets the displayed keys come from
     */
    public List<String> ownedBlockPaths() {
        Set<String> result = new LinkedHashSet<>();
        displayedKeysBySection.values().forEach(entries -> entries.forEach(entry -> result.add(entry.getKey())));
        return List.copyOf(result);
    }

    /**
     * @return the displayed keys of this model, keyed by
     * {@code blockPath + "/" + key}
     */
    public Map<String, List<String>> displayedKeys() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (List<Map.Entry<String, List<String>>> entries : displayedKeysBySection.values()) {
            for (Map.Entry<String, List<String>> entry : entries) {
                result.merge(entry.getKey(), entry.getValue(), (a, b) -> {
                    List<String> merged = new ArrayList<>(a);
                    b.stream().filter(key -> !merged.contains(key)).forEach(merged::add);
                    return merged;
                });
            }
        }
        return result;
    }

    /**
     * @return whether any <em>displayed</em> key holds a value in the document
     * that differs from its default. A block holding only defaults (the state
     * the bridge materializes on first open) is not a modification; a
     * non-displayed key does not count either.
     */
    public boolean isModified() {
        for (Map.Entry<String, List<String>> entry : displayedKeys().entrySet()) {
            for (String key : entry.getValue()) {
                if (isKeyModified(entry.getKey(), key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the displayed keys whose stored value differs from the default,
     * as {@code blockPath + "/" + key}, in display order
     */
    public List<String> modifiedKeys() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : displayedKeys().entrySet()) {
            for (String key : entry.getValue()) {
                if (isKeyModified(entry.getKey(), key)) {
                    result.add(entry.getKey() + "/" + key);
                }
            }
        }
        return result;
    }

    /**
     * A displayed key is modified when the document holds a value for it that
     * differs structurally from the recorded default. An absent value reads as
     * the default, so it is not modified.
     */
    private boolean isKeyModified(String blockPath, String key) {
        JsonNode stored = document.get(blockPath, key);
        if (stored.isMissingNode()) {
            return false;
        }
        JsonNode defaultValue = defaults.get(blockPath + "/" + key);
        if (defaultValue == null) {
            // No recorded default: fall back to "a stored value is a deviation".
            return true;
        }
        return !stored.equals(defaultValue);
    }

    /**
     * Removes the section's <em>displayed</em> keys from the document (per key,
     * never a whole block), so no stale non-default value survives in the
     * stored profile. Keys the section does not display are left untouched.
     * <p>
     * This only cleans the document. The live sheets are reset by the section UI
     * (which drives each displayed parameter to its default), because the bridge
     * applies only keys that are present in the block.
     * <p>
     * Only keys that actually deviate from their default are removed: an absent
     * key already reads as the default, so resetting a section whose displayed
     * keys are all at their defaults writes nothing (no spurious auto-save).
     */
    public void resetAll() {
        for (Map.Entry<String, List<String>> entry : displayedKeys().entrySet()) {
            for (String key : entry.getValue()) {
                if (isKeyModified(entry.getKey(), key)) {
                    document.remove(entry.getKey(), key);
                }
            }
        }
    }

    /**
     * Subscribes to changes of the sheets the displayed keys come from, so the
     * "modified" badge can be re-evaluated. A block-level listener catches every
     * per-key change (including the per-key removals of {@link #resetAll()}), so
     * the callback must re-evaluate {@link #isModified()} rather than trust the
     * event. The subscription is removed by {@link #unsubscribe(Consumer)}; call
     * both with the same listener instance (store it in a field).
     *
     * @param listener invoked on every change of a composed sheet's block
     */
    public void subscribe(Consumer<JsonNode> listener) {
        Objects.requireNonNull(listener, "listener");
        for (String blockPath : ownedBlockPaths()) {
            // SettingsDocument#set(String, String, JsonNode) (the per-key write the
            // sheet bridge uses) notifies only the exact "blockPath/key" listeners,
            // so every displayed key gets its own listener on top of the
            // block-level one (which covers whole-block replacements, block
            // removal, and the per-key removals of resetAll()).
            document.addPathListener(blockPath, listener);
            for (String key : displayedKeys().getOrDefault(blockPath, List.of())) {
                document.addPathListener(blockPath + "/" + key, listener);
            }
        }
    }

    /**
     * Removes a subscription created by {@link #subscribe(Consumer)}.
     *
     * @param listener the listener passed to {@link #subscribe(Consumer)}
     */
    public void unsubscribe(Consumer<JsonNode> listener) {
        for (String blockPath : ownedBlockPaths()) {
            document.removePathListener(blockPath, listener);
            for (String key : displayedKeys().getOrDefault(blockPath, List.of())) {
                document.removePathListener(blockPath + "/" + key, listener);
            }
        }
    }

    /**
     * @return the document this model operates on
     */
    public SettingsDocument document() {
        return document;
    }

    /**
     * @return the recorded default of a displayed key, or {@code null}
     */
    public JsonNode defaultOf(String blockPath, String key) {
        return defaults.get(blockPath + "/" + key);
    }

    /**
     * One of the merged sections.
     *
     * @param id            the stable section id ({@link #SECTION_APPEARANCE}, ...)
     * @param mergedSheetId the registration id of the merged sheet hosting the
     *                      section (persisted in profiles)
     * @param sheetIds      the registration ids of the <em>composed</em> sheets
     *                      the section's parameters come from (empty for the
     *                      placeholder)
     */
    public record Section(String id, String mergedSheetId, List<String> sheetIds) {
        public Section {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(mergedSheetId, "mergedSheetId");
            sheetIds = List.copyOf(sheetIds);
        }
    }
}