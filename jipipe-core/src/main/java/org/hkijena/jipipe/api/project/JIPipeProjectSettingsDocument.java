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
 * <li>{@code $schema}/{@code $meta} carry the format version and the
 * bookkeeping metadata;</li>
 * <li>blocks whose sheet is not known to this version are <em>pass-through</em>:
 * they survive load→save verbatim, so a project saved by a newer JIPipe with
 * additional project sheets opens and saves without losing those sheets.</li>
 * </ul>
 * <p>
 * <b>Two on-disk shapes.</b> Historically the project {@code "settings"} member
 * is a flat object whose top-level keys are the <em>unprefixed</em> sheet ids
 * ({@code {"org.hkijena.jipipe:project-io": {...}}}) and that has no
 * {@code $schema}. {@link #fromJson(JsonNode, Predicate)} auto-detects the
 * shape through the presence of {@code $schema} and upgrades the legacy shape
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
        JsonNode source = settingsObject.has(SCHEMA_FIELD) ? settingsObject : upgradeLegacyShape((ObjectNode) settingsObject);
        JIPipeProjectSettingsDocument result = new JIPipeProjectSettingsDocument(registeredBlock);
        result.document = SettingsDocumentIO.fromJson(SettingsScope.PROJECT, source,
                Objects.requireNonNull(registeredBlock, "registeredBlock").and(JIPipeProjectSettingsDocument::isSheetPath));
        return result;
    }

    /**
     * Rewrites the legacy unprefixed shape into the new prefixed shape:
     * every top-level key other than the reserved {@code $}-fields becomes
     * {@code "sheet:<key>"}, keeping its value verbatim. Keys that are already
     * {@code sheet:}-prefixed are kept unchanged (defensive: a mixed object is
     * never produced by JIPipe, but must not gain a double prefix).
     *
     * @param legacy the legacy settings object (not modified)
     * @return a new object in the new shape, without {@code $schema}/{@code $meta}
     */
    private static ObjectNode upgradeLegacyShape(ObjectNode legacy) {
        ObjectNode upgraded = JsonUtils.getObjectMapper().createObjectNode();
        legacy.fields().forEachRemaining(entry -> {
            if (entry.getKey().startsWith("$")) {
                return; // reserved fields ($schema/$meta) are never sheet ids
            }
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
     * Writes the {@code $meta} bookkeeping fields into the document and
     * normalizes {@code $schema} to version 2.
     * <p>
     * <b>Deliberately timestamp-free.</b> Saving a project must be a fixpoint
     * (loading and re-saving a project produces the same JSON), so no wall
     * clock value may enter the project file: a {@code written-at} timestamp
     * would make every save differ from the previous one and every project a
     * permanent diff. The application settings file (one owner, written by a
     * desktop app) stamps timestamps; a project file (shared, version
     * controlled) records only who wrote it, which is stable for a given
     * JIPipe version and editor.
     *
     * @param writtenByVersion the JIPipe version that writes the project
     */
    public void stampMeta(String writtenByVersion) {
        ObjectNode meta = document.meta();
        meta.put("written-by-version", Objects.requireNonNull(writtenByVersion, "writtenByVersion"));
        if (!meta.has("writer-id")) {
            meta.put("writer-id", document.writerId());
        }
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