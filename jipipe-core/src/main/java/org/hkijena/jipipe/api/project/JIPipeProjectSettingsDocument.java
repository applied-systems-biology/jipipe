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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.settings.SettingsBlockPath;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsDocumentIO;
import org.hkijena.jipipe.api.settings.SettingsKind;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The <b>project-scope</b> settings document: the content of the top-level
 * {@code "settings"} member of a saved {@code .jip}/{@code .jipx} project.
 * <p>
 * It wraps a {@link SettingsDocument} scoped {@link SettingsScope#PROJECT}
 * with the same rules as the application document:
 * <ul>
 * <li>blocks are the project settings sheets, keyed {@code "sheet:<sheetId>"};</li>
 * <li>{@code $schema} carries the format version and {@code $meta} carries
 * {@code written-by-version} — and nothing else, so a project file is
 * save-deterministic (see {@link #stampMeta(String)});</li>
 * <li>blocks whose sheet is not known to this version are <em>pass-through</em>:
 * they survive load→save verbatim, so a project saved by a newer JIPipe with
 * additional project sheets opens and saves without losing those sheets.</li>
 * </ul>
 * <p>
 * <b>Two on-disk shapes.</b> Historically the project {@code "settings"} member
 * is a flat object whose top-level keys are the <em>unprefixed</em> sheet ids
 * ({@code {"org.hkijena.jipipe:project-io": {...}}}) and that has no
 * {@code $schema}. {@link #fromJson(JsonNode, Predicate)} auto-detects the
 * shape through the presence of {@code $schema} <em>together with</em>
 * {@code $meta} or a {@code sheet:}-prefixed key (so a legacy object that
 * merely happens to contain a literal {@code "$schema"} key is still read as
 * legacy) and upgrades the legacy shape
 * by prefixing <em>every</em> top-level key with {@code "sheet:"} — registered
 * or not, so an unknown sheet of an old project is preserved exactly like an
 * unknown sheet of a new project.
 * <p>
 * This class performs no file IO: it is a thin, shape-aware layer over
 * {@link SettingsDocumentIO} and is usable standalone (e.g. in tests or by a
 * migration tool) without a live {@link JIPipeProject}.
 */
public final class JIPipeProjectSettingsDocument {

    /**
     * The block path prefix of project settings sheets ({@link SettingsKind#SHEET}).
     */
    private static final String SHEET_PREFIX = SettingsKind.SHEET.prefix() + ":";
    private static final String SCHEMA_FIELD = "$schema";

    private SettingsDocument document;

    /**
     * Creates a new, empty project settings document.
     *
     * @param registeredBlock predicate receiving a canonical block path
     *                        ({@code "sheet:<id>"}); {@code true} when the block
     *                        is known to this version
     */
    public JIPipeProjectSettingsDocument(Predicate<String> registeredBlock) {
        this.document = new SettingsDocument(SettingsScope.PROJECT,
                Objects.requireNonNull(registeredBlock, "registeredBlock").and(JIPipeProjectSettingsDocument::isSheetPath));
    }

    private JIPipeProjectSettingsDocument(SettingsDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    /**
     * Reads a project {@code "settings"} object of either shape.
     * <p>
     * <ul>
     * <li>the node has {@code $schema} → it is a new-shape document and is
     * read as-is;</li>
     * <li>otherwise it is the legacy shape: every top-level key is a
     * unprefixed sheet id and is re-written as {@code "sheet:<id>"}
     * (registered or not — drop nothing), then the resulting object is read.</li>
     * </ul>
     * The input node is never modified; all values are deep-copied.
     *
     * @param settingsObject the project's {@code "settings"} node
     * @param registeredBlock predicate receiving a canonical block path
     *                        ({@code "sheet:<id>"}); applied to the
     *                        <em>prefixed</em> path
     * @return the read document
     * @throws IllegalArgumentException if the node is not a JSON object
     */
    public static JIPipeProjectSettingsDocument fromJson(JsonNode settingsObject, Predicate<String> registeredBlock) {
        Objects.requireNonNull(settingsObject, "settingsObject");
        Objects.requireNonNull(registeredBlock, "registeredBlock");
        if (!settingsObject.isObject()) {
            throw new IllegalArgumentException("A project settings object must be a JSON object, but got: "
                    + settingsObject.getNodeType());
        }
        JsonNode source = isNewShape(settingsObject) ? settingsObject : upgradeLegacyShape((ObjectNode) settingsObject);
        JIPipeProjectSettingsDocument result = new JIPipeProjectSettingsDocument(registeredBlock);
        result.document = SettingsDocumentIO.fromJson(SettingsScope.PROJECT, source, registeredBlock.and(JIPipeProjectSettingsDocument::isSheetPath));
        return result;
    }

    /**
     * Discriminates the two shapes. A new-shape document always carries the
     * corroborating markers JIPipe itself writes, so {@code $schema} alone is
     * not enough: a hand-edited or third-party legacy object may contain a
     * literal {@code "$schema"} key (e.g. a JSON-Schema reference), and
     * misreading it as new-shape would silently drop every sheet block.
     *
     * @param settingsObject the project's {@code "settings"} node
     * @return {@code true} iff the node has {@code $schema} <em>and</em>
     * ({@code $meta} or at least one {@code sheet:}-prefixed key)
     */
    private static boolean isNewShape(JsonNode settingsObject) {
        if (!settingsObject.has(SCHEMA_FIELD) || !settingsObject.isObject()) {
            return false;
        }
        if (settingsObject.has("$meta")) {
            return true;
        }
        Iterator<String> fields = settingsObject.fieldNames();
        while (fields.hasNext()) {
            if (fields.next().startsWith(SHEET_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rewrites the legacy unprefixed shape into the new prefixed shape:
     * <em>every</em> top-level key becomes {@code "sheet:<key>"}, keeping its
     * value verbatim. Nothing is dropped — a legacy object can only reach here
     * when it carries no {@code $meta} and no {@code sheet:} block, so any
     * {@code $}-key it does contain (e.g. a stray {@code "$schema"} reference)
     * is data, not a document field, and is preserved as a pass-through block
     * {@code "sheet:$schema"}. Keys already {@code sheet:}-prefixed are kept
     * unchanged (defensive: a mixed object is never produced by JIPipe, but
     * must not gain a double prefix).
     *
     * @param legacy the legacy settings object (not modified)
     * @return a new object in the new shape, without {@code $schema}/{@code $meta}
     */
    private static ObjectNode upgradeLegacyShape(ObjectNode legacy) {
        ObjectNode upgraded = JsonUtils.getObjectMapper().createObjectNode();
        legacy.fields().forEachRemaining(entry -> {
            String path = entry.getKey().startsWith(SHEET_PREFIX) ? entry.getKey() : SHEET_PREFIX + entry.getKey();
            upgraded.set(path, entry.getValue().deepCopy());
        });
        return upgraded;
    }

    private static boolean isSheetPath(String path) {
        SettingsBlockPath blockPath = new SettingsBlockPath(path);
        return blockPath.kind() == SettingsKind.SHEET;
    }

    /**
     * @return the wrapped project-scope document (always {@link SettingsScope#PROJECT})
     */
    public SettingsDocument document() {
        return document;
    }

    /**
     * @return the serialized document: {@code $schema}, {@code $meta} and one
     * block per sheet, keyed {@code "sheet:<id>"}
     */
    public ObjectNode toJson() {
        return SettingsDocumentIO.toJson(document);
    }

    /**
     * @param sheetId the (unprefixed) settings sheet id
     * @return the canonical block path of that sheet, {@code "sheet:<sheetId>"}
     */
    public static String blockPathOf(String sheetId) {
        return SHEET_PREFIX + Objects.requireNonNull(sheetId, "sheetId");
    }

    /**
     * @param blockPath a canonical block path
     * @return the (unprefixed) sheet id of a {@code "sheet:<id>"} block path,
     * or {@code null} if the path is not a sheet block
     */
    public static String sheetIdOf(String blockPath) {
        SettingsBlockPath path = new SettingsBlockPath(blockPath);
        return path.kind() == SettingsKind.SHEET ? path.id() : null;
    }

    /**
     * @param blockPath a canonical block path
     * @return whether the block is known to this version (i.e. not pass-through)
     */
    public boolean isRegistered(String blockPath) {
        return isSheetPath(blockPath) && !document.isPassThrough(blockPath);
    }

    /**
     * @param blockPath a canonical block path
     * @return whether the block is stored and written back verbatim
     */
    public boolean isPassThrough(String blockPath) {
        return document.isPassThrough(blockPath);
    }

    /**
     * @param blockPath a canonical block path
     * @return the block's JSON, or a {@link MissingNode} if the block is absent
     */
    public JsonNode getBlock(String blockPath) {
        return document.getBlock(blockPath);
    }

    /**
     * Replaces the block at the given path (used to re-serialize a live sheet
     * into its block on save).
     *
     * @param blockPath the canonical block path
     * @param value     the new block, or {@code null} to remove it
     */
    public void setBlock(String blockPath, JsonNode value) {
        document.setBlock(blockPath, value);
    }

    /**
     * @return the canonical block paths present in the document
     */
    public java.util.Set<String> blockPaths() {
        return document.blockPaths();
    }

    /**
     * Sets the project {@code $meta} and normalizes {@code $schema} to
     * version 2.
     * <p>
     * <b>The project {@code $meta} contract: exactly {@code written-by-version}
     * — nothing else.</b> Project settings are shared and version-controlled,
     * and saving a project must be deterministic: loading and saving a project
     * (in any version, in any process, at any time) must produce the same JSON.
     * Every other bookkeeping field would violate that:
     * <ul>
     * <li>{@code written-at} — a wall clock makes every single save a diff;</li>
     * <li>{@code writer-id} — a per-instance random id makes the <em>first</em>
     * save of every upgraded project a spurious one-line diff. That id exists
     * for multi-process conflict detection between settings files that one
     * process may write, which does not apply to a project file.</li>
     * </ul>
     * The application settings file keeps its own, richer {@code $meta}
     * (timestamps, writer id) — it is single-owner and not diffed; that
     * contract lives in the settings service, not here.
     *
     * @param writtenByVersion the JIPipe version that writes the project
     */
    public void stampMeta(String writtenByVersion) {
        ObjectNode meta = JsonUtils.getObjectMapper().createObjectNode();
        meta.put("written-by-version", Objects.requireNonNull(writtenByVersion, "writtenByVersion"));
        document.setBlock("$meta", meta);
        if (document.schemaVersion() != 2) {
            document.setSchemaVersion(2);
        }
    }

    /**
     * @return the JIPipe version recorded as the writer of the settings block:
     * always the <em>running</em> JIPipe version. It must not be the loaded
     * project's version — {@code $meta} describes who wrote the file now, and
     * copying the loaded version would change on every subsequent save (the
     * first save would stamp the old version, the second the new one),
     * breaking the save fixpoint.
     */
    static String writtenByVersion() {
        return VersionUtils.getJIPipeVersion();
    }
}
