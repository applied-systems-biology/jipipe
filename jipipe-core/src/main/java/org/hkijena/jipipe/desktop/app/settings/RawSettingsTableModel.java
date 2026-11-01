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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent.ChangeOrigin;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure (non-Swing) model of the raw settings editor (modern settings system,
 * Task 20).
 * <p>
 * <b>What it lists.</b> {@link #rows(String)} returns one {@link RawRow} per
 * <em>addressable leaf</em> of the selected scope's document:
 * <ul>
 * <li><b>Documented rows:</b> every key registered in the component
 * ({@link JIPipeSettingsServiceComponent#registeredKeys()}) whose
 * {@link JIPipeSettingKey#scope()} is the model's scope. A documented row is
 * emitted whether or not a value is stored; an absent value reads as the key's
 * default and is shown as {@link #DEFAULT_PLACEHOLDER}.</li>
 * <li><b>Pass-through rows:</b> every leaf of the document that is <em>not</em> a
 * registered key — the leaves of pass-through blocks
 * ({@link SettingsDocument#isPassThrough(String)}: a block kind this version
 * does not know, or a block with no registered content) and the undocumented
 * sub-paths of registered blocks. This is the feature: unknown content stays
 * visible and editable.</li>
 * </ul>
 * Nested objects are flattened to their leaves (a nested object is not itself a
 * row); a block whose value is a scalar, an array or {@code null} is one row at
 * the block path. The two sets are disjoint by construction — a location's row
 * kind is decided by "is a key registered here?", the registration wins — so a
 * location is never emitted twice.
 * <p>
 * <b>Pending defaults are marked, not materialized.</b> The settings engine
 * never stores a value that equals its default. A reset default that differs
 * from the stored value (e.g. the reset restored {@code true} while an
 * undefined file still holds {@code false}) therefore stays pending: the location
 * remains stored. The row's {@link RawRow#valuePreview()} is then rendered as
 * {@code stored → default}, so the table never claims a value the engine will not
 * report. Once a save writes the value, the file is authoritative again and the
 * plain stored value is shown.
 * <p>
 * <b>Writes.</b> {@link #setValue(String, String)} and {@link #resetKey(String)}
 * apply immediately, through
 * {@link JIPipeSettingsServiceComponent#writeWithOrigin}:
 * <ul>
 * <li>a raw edit is a {@link ChangeOrigin#RAW_EDIT} write of the parsed JSON, so
 * it is broadcast with that origin and schedules the engine's debounced
 * auto-save (raw edits are local-origin writes);</li>
 * <li>resetting a documented key writes its registered default with
 * {@link ChangeOrigin#RESET}, or — when the key has no default to restore
 * ({@code null} default) — removes the stored location;</li>
 * <li>resetting a pass-through value removes the location with
 * {@link ChangeOrigin#RAW_EDIT} (there is no default to restore).</li>
 * </ul>
 * Values are stored as the JSON they parse to, exactly as raw editing implies:
 * no per-key type checking is performed (a documented key's stored value is
 * validated by whoever reads it through its typed binding).
 * <p>
 * The class holds no Swing types; {@link JIPipeRawSettingsTableModel} adapts it
 * to a {@code JTable}.
 */
public final class RawSettingsTableModel {

    /**
     * The value placeholder of a documented row whose location holds no stored
     * value (the key falls back to its default).
     */
    public static final String DEFAULT_PLACEHOLDER = "\u00abdefault\u00bb";
    /**
     * Separates the stored value from the pending default in the preview of a
     * location that was reset to a default differing from the stored value.
     */
    public static final String PENDING_DEFAULT_SEPARATOR = " \u2192 ";
    /**
     * The maximum number of characters of a rendered preview or default text
     * shown in the table; a longer rendering is cut and ends with
     * {@link #TRUNCATION_ELLIPSIS}. {@link #rawJson(String)} is never truncated:
     * the edit dialog needs the full value.
     */
    public static final int MAX_PREVIEW_LENGTH = 80;
    /**
     * Appended to a preview that {@link #MAX_PREVIEW_LENGTH} cut.
     */
    public static final String TRUNCATION_ELLIPSIS = "\u2026";

    private final JIPipeSettingsServiceComponent settingsService;
    private final SettingsScope scope;
    private final SettingsDocument document;

    /**
     * @param settingsService the component that owns the document and the key registry
     * @param scope the scope to list and edit
     */
    public RawSettingsTableModel(JIPipeSettingsServiceComponent settingsService, SettingsScope scope) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.document = settingsService.document(scope);
    }

    /**
     * @param path a full location path {@code "<blockPath>/<subPath>"}
     * @return the block path part (the first segment; block paths never contain a slash)
     */
    public static String blockPathOf(String path) {
        Objects.requireNonNull(path, "path");
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }

    /**
     * @param path a full location path {@code "<blockPath>/<subPath>"}
     * @return the sub-path part, or an empty string for a bare block path
     */
    public static String subPathOf(String path) {
        Objects.requireNonNull(path, "path");
        int slash = path.indexOf('/');
        return slash < 0 ? "" : path.substring(slash + 1);
    }

    /**
     * @return the scope this model lists and edits
     */
    public SettingsScope scope() {
        return scope;
    }

    /**
     * The rows of the selected scope, sorted by path.
     *
     * @param filter a case-insensitive substring of the row's path; blank or
     *               {@code null} matches every row
     * @return the matching rows, sorted by path
     */
    public List<RawRow> rows(String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        // Sorted by path (stable table order). A TreeMap also merges the two row
        // sources: documented keys are inserted first, stored leaves with
        // putIfAbsent, so a location is emitted once and a registered key wins.
        Map<String, RawRow> byPath = new TreeMap<>();
        for (JIPipeSettingKey<?> key : registeredKeysOfThisScope()) {
            String path = key.blockPath() + "/" + normalizeSubPath(key.subPath());
            byPath.put(path, documentedRow(path, key));
        }
        JsonNode root = document.snapshotJson();
        for (String blockPath : document.blockPaths()) {
            JsonNode block = root.get(blockPath);
            if (block == null) {
                continue;
            }
            if (block.isObject() && !block.isEmpty()) {
                collectLeaves(blockPath, "", block, byPath);
            } else {
                byPath.putIfAbsent(blockPath, passThroughRow(blockPath, block));
            }
        }
        List<RawRow> result = new ArrayList<>();
        for (RawRow row : byPath.values()) {
            if (needle.isEmpty() || row.path().toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * @param path a full location path
     * @return the row of that location, or {@code null} if the location is not
     * listed in this scope
     */
    public RawRow row(String path) {
        return rows(path).stream().filter(row -> row.path().equals(path)).findFirst().orElse(null);
    }

    /**
     * @param path a full location path
     * @return the stored value as compact JSON, or an empty string if the
     * location is absent
     */
    public String rawJson(String path) {
        JsonNode value = storedValue(path);
        return value.isMissingNode() ? "" : toCompactJson(value);
    }

    /**
     * @param path a full location path
     * @return whether the location is a registered (documented) key
     */
    public boolean isDocumented(String path) {
        return keyAt(path) != null;
    }

    /**
     * Stores the parsed {@link JsonNode} of {@code rawJson} at the location as a
     * raw edit ({@link ChangeOrigin#RAW_EDIT}); the engine broadcasts and
     * auto-saves the change.
     * <p>
     * <b>A JSON {@code null} is a value.</b> The text {@code null} is accepted
     * and stores a JSON null node at the location (the raw editor is honest
     * JSON: a stored null is content, distinct from an absent location — the
     * same distinction the legacy registry makes). An empty or unparseable
     * input is rejected. Removing a location is the job of
     * {@link #resetKey(String)} (a pass-through reset, or the reset of a
     * documented key without a default), never of this method.
     *
     * @param path a full location path
     * @param rawJson the new value as JSON text
     * @throws IllegalArgumentException if {@code rawJson} is not valid JSON; the
     *                                  document is not touched in that case
     */
    public void setValue(String path, String rawJson) {
        String blockPath = blockPathOf(path);
        String subPath = subPathOf(path);
        JsonNode value;
        try {
            if (rawJson == null) {
                throw new IllegalArgumentException("no value given");
            }
            value = JsonUtils.getObjectMapper().readTree(rawJson);
            if (value == null || value.isMissingNode()) {
                throw new IllegalArgumentException("no value given");
            }
        } catch (JsonProcessingException | RuntimeException e) {
            throw new IllegalArgumentException("Cannot edit '" + path + "': not a valid JSON value ("
                    + firstLine(e.getMessage()) + ")", e);
        }
        settingsService.writeWithOrigin(scope, blockPath, subPath, value, ChangeOrigin.RAW_EDIT);
    }

    /**
     * Resets the location to its default: a documented key is set to its
     * registered default ({@link ChangeOrigin#RESET}), or removed when it has
     * none; a pass-through value is removed ({@link ChangeOrigin#RAW_EDIT}).
     *
     * @param path a full location path
     */
    public void resetKey(String path) {
        JIPipeSettingKey<?> key = keyAt(path);
        if (key == null) {
            settingsService.writeWithOrigin(scope, blockPathOf(path), subPathOf(path), null, ChangeOrigin.RAW_EDIT);
            return;
        }
        JsonNode defaultValue = defaultJsonOf(key);
        settingsService.writeWithOrigin(scope, key.blockPath(), normalizeSubPath(key.subPath()),
                defaultValue != null && !defaultValue.isNull() ? defaultValue : null,
                ChangeOrigin.RESET);
    }

    // ------------------------------------------------------------- rows

    private RawRow documentedRow(String path, JIPipeSettingKey<?> key) {
        JsonNode defaultValue = defaultJsonOf(key);
        String defaultText = renderDefault(defaultValue);
        JsonNode stored = document.get(key.blockPath(), normalizeSubPath(key.subPath()));
        String valuePreview = renderPreview(stored, defaultValue);
        return new RawRow(path,
                valuePreview,
                key.type() == null ? "" : key.type().getSimpleName(),
                false,
                true,
                defaultText,
                key.name(),
                key.description());
    }

    private RawRow passThroughRow(String path, JsonNode value) {
        return new RawRow(path, truncate(render(value)), typeLabelOf(value), true, false, "", "", "");
    }

    /**
     * Flattens an object value into leaf rows. Nested objects recurse (unless
     * empty: an empty object is itself a leaf), arrays, scalars and
     * {@code null} are leaves.
     */
    private void collectLeaves(String blockPath, String prefix, JsonNode node, Map<String, RawRow> byPath) {
        boolean emptyObject = node.isObject() && node.isEmpty();
        if (!node.isObject() || emptyObject) {
            byPath.putIfAbsent(blockPath + "/" + prefix, passThroughRow(blockPath + "/" + prefix, node));
            return;
        }
        node.fields().forEachRemaining(entry ->
                collectLeaves(blockPath, prefix.isEmpty() ? entry.getKey() : prefix + "/" + entry.getKey(),
                        entry.getValue(), byPath));
    }

    /**
     * Renders the value preview of a documented location: {@code null} (no
     * stored value) is the {@link #DEFAULT_PLACEHOLDER}; a stored value shows
     * its JSON, followed by the default's JSON when the two differ.
     * <p>
     * <b>Why both are shown.</b> The engine never stores a value that equals its
     * default (a typed write removes it instead), but a raw edit or a reset
     * writes the location verbatim, and the auto-save persists that resolved
     * value. Until the next read/save resolves the location, the stored value and
     * the effective value can differ — showing both keeps the table honest about
     * what is stored <em>and</em> what the setting resolves to.
     *
     * @param stored the stored value, or {@code null} if the location is absent
     * @param defaultValue the key's default, or {@code null} if it has none
     * @return the rendered preview
     */
    public static String renderPreview(JsonNode stored, JsonNode defaultValue) {
        if (stored == null || stored.isMissingNode()) {
            return DEFAULT_PLACEHOLDER;
        }
        String storedText = render(stored);
        if (defaultValue == null || stored.equals(defaultValue)
                || storedText.length() > MAX_PREVIEW_LENGTH) {
            // Either there is nothing to compare, or the stored part alone is
            // already too long to append anything meaningfully: cut it and stop.
            return truncate(storedText);
        }
        return truncate(storedText + PENDING_DEFAULT_SEPARATOR + render(defaultValue));
    }

    /**
     * Renders the Default column, bounded like the preview.
     *
     * @param defaultValue the key's default, or {@code null} if it has none
     * @return the rendered, truncated default; an empty string for a key without
     * a default ({@code "null"} for an actual JSON null default)
     */
    public static String renderDefault(JsonNode defaultValue) {
        return defaultValue == null ? "" : truncate(render(defaultValue));
    }

    /**
     * @param rendered an already-rendered value
     * @return the rendering, cut to {@link #MAX_PREVIEW_LENGTH} characters plus
     * {@link #TRUNCATION_ELLIPSIS} when longer
     */
    public static String truncate(String rendered) {
        if (rendered == null) {
            return "";
        }
        if (rendered.length() <= MAX_PREVIEW_LENGTH) {
            return rendered;
        }
        return rendered.substring(0, MAX_PREVIEW_LENGTH) + TRUNCATION_ELLIPSIS;
    }

    /**
     * @param value a JSON value, or {@code null}
     * @return the value as compact JSON ({@code "null"} for a JSON null)
     */
    static String render(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        return value.toString();
    }

    /**
     * @param value a JSON value
     * @return the JSON type of a schema-less value, as the raw editor shows it
     */
    static String typeLabelOf(JsonNode value) {
        if (value == null || value.isMissingNode()) {
            return "";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        if (value.isNull()) {
            return "null";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isIntegralNumber()) {
            if (value.isBigInteger() || !value.canConvertToInt()) {
                return "long";
            }
            return "int";
        }
        if (value.isNumber()) {
            return "double";
        }
        return "string";
    }

    private static String toCompactJson(JsonNode value) {
        return value.toString();
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unparseable";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    /**
     * @param subPath a slash-separated sub-path as declared by a key
     * @return the sub-path with empty segments dropped (the document's addressing
     * rule, so {@code "a//b"} and {@code "a/b"} denote the same location)
     */
    static String normalizeSubPath(String subPath) {
        if (subPath == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        for (String segment : subPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append('/');
            }
            normalized.append(segment);
        }
        return normalized.toString();
    }

    private Set<JIPipeSettingKey<?>> registeredKeysOfThisScope() {
        Set<JIPipeSettingKey<?>> keys = new LinkedHashSet<>();
        List<JIPipeSettingKey<?>> sorted = new ArrayList<>(settingsService.registeredKeys());
        sorted.sort(Comparator.comparing((JIPipeSettingKey<?> key) -> key.blockPath())
                .thenComparing(key -> normalizeSubPath(key.subPath())));
        for (JIPipeSettingKey<?> key : sorted) {
            if (key.scope() == scope) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * @param path a full location path
     * @return the registered key at that exact location <em>in this model's
     * scope</em>, or {@code null}
     * <p>
     * The component's key registry is a single, scope-agnostic map keyed by
     * location, so {@code keyByBlockAndSubPath} also returns a key that was
     * registered in another scope. Rows are enumerated per scope
     * ({@link #registeredKeysOfThisScope()}), so resolution must be bounded the
     * same way: a location this scope's document holds as stored content, but
     * another scope's key describes, is pass-through content here. Without the
     * check, {@link #resetKey(String)} would take the documented branch and
     * write the <em>other</em> scope's default into this scope's document.
     */
    private JIPipeSettingKey<?> keyAt(String path) {
        JIPipeSettingKey<?> key = settingsService.keyByBlockAndSubPath(blockPathOf(path), subPathOf(path));
        return key != null && key.scope() == scope ? key : null;
    }

    private JsonNode storedValue(String path) {
        return document.get(blockPathOf(path), subPathOf(path));
    }

    /**
     * @param key the key
     * @return the key's default as JSON, or {@code null} if it has none
     */
    private static JsonNode defaultJsonOf(JIPipeSettingKey<?> key) {
        Object value = key.defaultValue();
        return value == null ? null : JsonUtils.getObjectMapper().valueToTree(value);
    }

    /**
     * One addressable setting of the raw editor.
     *
     * @param path the full location path {@code "<blockPath>/<subPath>"} (a bare
     *             block path for a whole-block value)
     * @param valuePreview the stored value, rendered ({@code "«default»"} for an
     *                     absent documented key)
     * @param typeLabel the documented key's Java type, or the stored JSON type
     *                  for a pass-through value
     * @param isPassThrough whether the value is not described by a registered key
     * @param isDocumented whether a registered key documents the location
     * @param defaultText the registered default, rendered (empty for pass-through)
     * @param name the key's display name (empty for pass-through)
     * @param description the key's description (empty for pass-through)
     */
    public record RawRow(String path,
                         String valuePreview,
                         String typeLabel,
                         boolean isPassThrough,
                         boolean isDocumented,
                         String defaultText,
                         String name,
                         String description) {
        public RawRow {
            Objects.requireNonNull(path, "path");
            valuePreview = valuePreview == null ? "" : valuePreview;
            typeLabel = typeLabel == null ? "" : typeLabel;
            defaultText = defaultText == null ? "" : defaultText;
            name = name == null ? "" : name;
            description = description == null ? "" : description;
        }
    }
}