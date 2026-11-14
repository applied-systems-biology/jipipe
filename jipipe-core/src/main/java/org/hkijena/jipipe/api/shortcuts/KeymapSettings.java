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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The persisted keymap state: which keyboard shortcut the user remapped to
 * which stroke, and for how long a chord (a sequence of key presses) stays
 * pending before it expires.
 * <p>
 * This bean is stored in the application settings document under the
 * {@code "registry:keymap"} block (see {@link JIPipeShortcutSettings}). The
 * {@link #overrides} map is keyed by the stable string-literal action id of a
 * {@link JIPipeShortcutAction}; every value is a list of chord variants, and
 * every variant is a list of keystrokes in the canonical {@link KeyStroke}
 * string form (e.g. {@code "ctrl pressed T"}). Keys are stored as strings so
 * the document stays plain JSON, and the typed accessors
 * ({@link #overrideVariants(String)} / {@link #setOverride(String, List)})
 * parse and produce {@link KeyStroke}s.
 * <p>
 * Overrides for action ids that are no longer registered survive a write+read
 * round trip verbatim: they are only cleaned up in the UI. A single malformed
 * chord entry is dropped with a warning instead of failing the whole load.
 * The field accessors use explicit {@code @JsonGetter}/{@code @JsonSetter}
 * annotations because the global {@link org.hkijena.jipipe.utils.json.JsonUtils}
 * mapper disables implicit property visibility.
 * <p>
 * <b>Defaults are never materialized.</b> The settings rule is that a stored
 * value equal to the default is not written to the document, so the
 * {@code overrides} map is the bean's actual content while the other two
 * fields serialize only when they deviate from their default:
 * {@code activeProfileId} omits the {@code null} default
 * ({@link JsonInclude.Include#NON_DEFAULT NON_DEFAULT}, correct for a
 * reference type), and {@code chordTimeoutMs} omits
 * {@value #DEFAULT_CHORD_TIMEOUT_MS}. The chord timeout uses a custom getter
 * rather than {@code NON_DEFAULT} because Jackson evaluates
 * {@code NON_DEFAULT} for primitives against the type's natural default
 * ({@code 0L}), not the field's: a plain annotation would still materialize
 * the default {@code 2000L} and - worse - silently drop the meaningful
 * value {@code 0} (wait indefinitely for the second chord stroke). The
 * getter therefore returns {@code null} exactly for the default value, which
 * {@link JsonInclude.Include#NON_NULL NON_NULL} omits; every other value,
 * including {@code 0}, is serialized. Reading falls back to
 * {@value #DEFAULT_CHORD_TIMEOUT_MS} when the field is absent (the field
 * initializer).
 */
public class KeymapSettings {
    private static final Logger logger = LoggerFactory.getLogger(KeymapSettings.class);
    /**
     * The default maximum time between two key presses of a chord, in
     * milliseconds.
     */
    public static final long DEFAULT_CHORD_TIMEOUT_MS = 2000L;

    private String activeProfileId;
    private Map<String, List<List<String>>> overrides = new LinkedHashMap<>();
    private long chordTimeoutMs = DEFAULT_CHORD_TIMEOUT_MS;

    public KeymapSettings() {
    }

    @JsonGetter("activeProfileId")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public String getActiveProfileId() {
        return activeProfileId;
    }

    @JsonSetter("activeProfileId")
    public void setActiveProfileId(String activeProfileId) {
        this.activeProfileId = activeProfileId;
    }

    @JsonGetter("overrides")
    public Map<String, List<List<String>>> getOverrides() {
        return overrides;
    }

    @JsonSetter("overrides")
    public void setOverrides(Map<String, List<List<String>>> overrides) {
        this.overrides = overrides == null ? new LinkedHashMap<>() : overrides;
    }

    /**
     * The current maximum time between two key presses of a chord, in
     * milliseconds. {@code 0} means the pending first stroke waits
     * indefinitely for the second.
     *
     * @return the chord timeout in milliseconds, never below {@code 0}
     */
    public long getChordTimeoutMs() {
        return chordTimeoutMs;
    }

    /**
     * The {@code chordTimeoutMs} <em>serialization</em> getter. Unlike
     * {@link #getChordTimeoutMs()} (which returns the raw value and is the
     * bean's API accessor), this one returns {@code null} exactly when the
     * value equals {@link #DEFAULT_CHORD_TIMEOUT_MS}, so
     * {@link JsonInclude.Include#NON_NULL NON_NULL} omits the default from
     * the stored document while every deviation - including the meaningful
     * {@code 0} (wait indefinitely) - is written. See the class javadoc for
     * why {@code NON_DEFAULT} cannot express this for a primitive
     * {@code long}.
     */
    @JsonGetter("chordTimeoutMs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long serializeChordTimeoutMs() {
        return chordTimeoutMs == DEFAULT_CHORD_TIMEOUT_MS ? null : chordTimeoutMs;
    }

    @JsonSetter("chordTimeoutMs")
    public void setChordTimeoutMs(long chordTimeoutMs) {
        this.chordTimeoutMs = chordTimeoutMs;
    }

    /**
     * Parses the stored override for the given action id into typed chords.
     * A variant whose stored keystrokes cannot all be parsed back into a
     * {@link KeyStroke} is dropped with a warning: one malformed entry must
     * not make the whole keymap unloadable.
     *
     * @param actionId the action id of the override to read
     * @return the parsed chord variants, empty when the action has no override
     */
    public List<List<KeyStroke>> overrideVariants(String actionId) {
        List<List<KeyStroke>> result = new ArrayList<>();
        for (List<String> variant : overrides.getOrDefault(Objects.requireNonNull(actionId, "actionId"), List.of())) {
            List<KeyStroke> chord = new ArrayList<>(variant.size());
            boolean valid = true;
            for (String stroke : variant) {
                KeyStroke parsed = KeyStroke.getKeyStroke(stroke);
                if (parsed == null) {
                    logger.warn("Dropping malformed keystroke '{}' of the override for action {}", stroke, actionId);
                    valid = false;
                    break;
                }
                chord.add(parsed);
            }
            if (valid) {
                result.add(chord);
            }
        }
        return result;
    }

    /**
     * Replaces the stored override for the given action id with the given
     * chord variants. The strokes are stored in the canonical
     * {@link KeyStroke#toString()} form. A {@code null} or empty variant list
     * removes the key, so an override that was just cleared is not persisted.
     *
     * @param actionId the action id of the override to write
     * @param variants the new chord variants, or {@code null}/empty to remove
     */
    public void setOverride(String actionId, List<List<KeyStroke>> variants) {
        Objects.requireNonNull(actionId, "actionId");
        if (variants == null || variants.isEmpty()) {
            overrides.remove(actionId);
            return;
        }
        List<List<String>> stored = new ArrayList<>(variants.size());
        for (List<KeyStroke> variant : variants) {
            List<String> chord = new ArrayList<>(variant.size());
            for (KeyStroke stroke : variant) {
                chord.add(stroke.toString());
            }
            stored.add(chord);
        }
        overrides.put(actionId, stored);
    }

    /**
     * Removes the stored override for the given action id, if any.
     *
     * @param actionId the action id of the override to remove
     */
    public void removeOverride(String actionId) {
        overrides.remove(Objects.requireNonNull(actionId, "actionId"));
    }

    /**
     * @param actionId the action id to check
     * @return whether an override is stored for the given action id
     */
    public boolean hasOverride(String actionId) {
        return overrides.containsKey(Objects.requireNonNull(actionId, "actionId"));
    }
}
