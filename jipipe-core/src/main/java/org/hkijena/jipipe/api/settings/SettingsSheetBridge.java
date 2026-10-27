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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection.ParameterChangedEventListener;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A two-way, loop-safe bridge between one {@link JIPipeApplicationSettingsSheet}
 * and its block ({@code "sheet:<sheetId>"}) in a {@link SettingsDocument}.
 * <p>
 * While attached, the bridge keeps both sides in sync:
 * <ul>
 * <li><b>Sheet → document:</b> the bridge subscribes to the sheet's
 * {@code ParameterChangedEvent}s; on an event it calls
 * {@link #flushToDocument()}, which <em>merges</em> the sheet's serialized
 * keys into the existing block (Jackson {@link ObjectNode#setAll}), so
 * sub-paths the sheet does not know about — e.g. knobs written by a newer
 * JIPipe version — survive a sheet round-trip instead of being clobbered.</li>
 * <li><b>Document → sheet:</b> the bridge registers a path listener on the
 * block and on every parameter key the sheet knows (via
 * {@link SettingsDocument#listenerPath(String, String)}). A change to any of
 * those locations triggers {@link #applyFromDocument()}, which deserializes
 * only the known keys (a filtered copy of the block) so unknown pass-through
 * sub-paths cannot fail sheet validation, and sets each sheet parameter one
 * at a time <em>only when the serialized value differs</em>.</li>
 * </ul>
 * <p>
 * <b>Loop safety:</b> a single re-entrancy flag ({@link #applying}) plus
 * value comparison guarantees that the echo of an applied change never
 * writes back and never re-applies:
 * <ul>
 * <li>Document → sheet: applying sets sheet parameters with the flag up, so
 * any {@code ParameterChangedEvent}s that leak out are dropped instead of
 * triggering a flush. Because only <em>changed</em> parameters fire events
 * and only <em>different</em> values are applied, re-applying an unchanged
 * block is fully inert (zero events, zero writes).</li>
 * <li>Sheet → document: a flush merges the sheet's state into the block,
 * which fires the bridge's own document listeners and triggers
 * {@link #applyFromDocument()} once; but the block now holds exactly the
 * state that was just serialized out of the sheet, so every value
 * comparison matches, no parameter is set, no change events are emitted,
 * and the cycle stops. {@code applyFromDocument} itself never writes to
 * the document, so a flush → apply round-trip cannot produce a second
 * write.</li>
 * </ul>
 * <p>
 * A fresh document without the block is seeded from the sheet's current
 * (default) values by {@link #attach()}, so a first-run profile behaves like
 * the old built-in defaults.
 */
public final class SettingsSheetBridge {
    private static final Logger logger = LoggerFactory.getLogger(SettingsSheetBridge.class);

    private final SettingsDocument document;
    private final String blockPath;
    private final JIPipeApplicationSettingsSheet sheet;
    private final Consumer<JsonNode> documentListener;
    private final ParameterChangedEventListener sheetListener;

    private boolean attached;
    /**
     * True while this bridge is copying the block into the sheet. Sheet
     * parameter changes caused by the apply are then dropped in
     * {@link #onSheetParameterChanged(ParameterChangedEvent)} so they do not
     * flush back into the document.
     */
    private boolean applying;

    /**
     * Creates a bridge between a sheet and the {@code "sheet:<sheetId>"}
     * block of a document. The bridge is inert until {@link #attach()} is
     * called; call {@link #detach()} when the sheet is disposed or the
     * document is being torn down.
     *
     * @param document the settings document that owns the block
     * @param sheetId the sheet's unique ID; the block path is {@code "sheet:" + sheetId}
     * @param sheet the settings sheet to keep in sync
     */
    public SettingsSheetBridge(SettingsDocument document, String sheetId, JIPipeApplicationSettingsSheet sheet) {
        this.document = Objects.requireNonNull(document, "document");
        this.blockPath = "sheet:" + Objects.requireNonNull(sheetId, "sheetId");
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.documentListener = node -> onDocumentChange();
        this.sheetListener = this::onSheetParameterChanged;
    }

    /**
     * Starts syncing the two sides. If the block is absent in the document,
     * the sheet's current values are serialized into it first (a fresh
     * profile behaves like the old defaults), and then the bridge subscribes
     * to both directions.
     */
    public void attach() {
        if (attached) {
            return;
        }
        attached = true;
        if (!document.hasBlock(blockPath)) {
            flushToDocument();
        }
        Map<String, JIPipeParameterAccess> parameters = parameterKeys();
        // A block-level listener covers whole-block replacements (setBlock,
        // removeBlock, external reloads); per-key listeners cover sub-path
        // mutations, because SettingsDocument.set(block, subPath, ...)
        // notifies only the exact sub-path listeners.
        document.addPathListener(blockPath, documentListener);
        for (String key : parameters.keySet()) {
            document.addPathListener(SettingsDocument.listenerPath(blockPath, key), documentListener);
        }
        sheet.getParameterChangedEventEmitter().subscribe(sheetListener);
    }

    /**
     * Stops syncing and removes every subscription: the document path
     * listeners (block level and per parameter key) and the sheet's
     * parameter-changed listener. This method is idempotent.
     */
    public void detach() {
        if (!attached) {
            return;
        }
        attached = false;
        sheet.getParameterChangedEventEmitter().unsubscribe(sheetListener);
        Map<String, JIPipeParameterAccess> parameters = parameterKeys();
        document.removePathListener(blockPath, documentListener);
        for (String key : parameters.keySet()) {
            document.removePathListener(SettingsDocument.listenerPath(blockPath, key), documentListener);
        }
    }

    /**
     * Serializes the sheet and <em>merges</em> the result into the block:
     * the sheet's keys are written onto the existing block (or a fresh block
     * when it is absent), so sub-paths unknown to the sheet survive. This is
     * the write path used by both the save flow and sheet-originated
     * changes; it is a plain document write and fires the document's path
     * listeners, including other bridges over the same block.
     */
    public void flushToDocument() {
        JsonNode sheetNode;
        try {
            sheetNode = JsonUtils.getObjectMapper().readTree(sheet.serializeToJsonString());
        } catch (Exception e) {
            logger.warn("Could not serialize settings sheet {} for {}", sheet.getClass().getName(), blockPath, e);
            return;
        }
        if (!(sheetNode instanceof ObjectNode objectNode)) {
            logger.warn("Settings sheet {} serialized to a non-object node; nothing written to {}",
                    sheet.getClass().getName(), blockPath);
            return;
        }
        JsonNode existing = document.getBlock(blockPath);
        ObjectNode block = existing instanceof ObjectNode o ? (ObjectNode) o.deepCopy()
                : JsonUtils.getObjectMapper().createObjectNode();
        block.setAll(objectNode);
        document.setBlock(blockPath, block);
    }

    /**
     * Copies the block into the sheet, firing the parameter change events
     * the UI listens to. Only the keys the sheet knows are read (a filtered
     * copy of the block), so unknown pass-through sub-paths cannot fail
     * sheet validation, and each parameter is set one at a time, only when
     * its serialized value differs from what the block holds — applying an
     * unchanged block therefore fires nothing and writes nothing back.
     */
    public void applyFromDocument() {
        JsonNode block = document.getBlock(blockPath);
        Map<String, JIPipeParameterAccess> parameters = parameterKeys();
        if (parameters.isEmpty()) {
            return;
        }
        ObjectNode filtered = JsonUtils.getObjectMapper().createObjectNode();
        for (String key : parameters.keySet()) {
            JsonNode value = block.isObject() ? block.get(key) : null;
            if (value != null && !value.isMissingNode()) {
                filtered.set(key, value);
            }
        }
        applying = true;
        try {
            for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.entrySet()) {
                JsonNode stored = filtered.get(entry.getKey());
                if (stored == null || stored.isMissingNode()) {
                    continue;
                }
                JsonNode current = serializeValue(entry.getValue().get(Object.class));
                if (current.equals(stored)) {
                    continue;
                }
                Object value;
                try {
                    value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getFieldClass()).readValue(stored);
                } catch (Exception | Error e) {
                    logger.warn("Could not apply value of '{}' from {} to sheet {}: keeping the current value",
                            entry.getKey(), blockPath, sheet.getClass().getName(), e);
                    continue;
                }
                try {
                    entry.getValue().set(value);
                } catch (Exception | Error e) {
                    logger.warn("Could not set parameter '{}' of sheet {}: keeping the current value",
                            entry.getKey(), sheet.getClass().getName(), e);
                }
            }
        } finally {
            applying = false;
        }
    }

    /**
     * Called on every parameter change event of the sheet. Events that
     * leaked out of an in-progress {@link #applyFromDocument()} are dropped
     * (re-entrancy guard); genuine sheet-originated edits trigger a merge
     * into the document via {@link #flushToDocument()}.
     */
    private void onSheetParameterChanged(ParameterChangedEvent event) {
        if (!attached || applying) {
            return;
        }
        flushToDocument();
    }

    /**
     * Called by the document's path listeners (block level and per
     * parameter key) whenever the block or one of the known keys changes.
     * The document is the source of truth in this direction; the bridge
     * copies the changed keys into the sheet and never writes back into the
     * document.
     */
    private void onDocumentChange() {
        if (!attached || applying) {
            return;
        }
        applyFromDocument();
    }

    /**
     * @return the parameters the sheet knows, keyed by the unique
     * serialization key (identical to the parameter's JSON field name)
     */
    private Map<String, JIPipeParameterAccess> parameterKeys() {
        Map<String, JIPipeParameterAccess> result = new LinkedHashMap<>();
        try {
            result.putAll(JIPipeParameterTree.getParameters(sheet));
        } catch (Exception | Error e) {
            logger.warn("Could not build the parameter tree of settings sheet {}: treating it as having no parameters",
                    sheet.getClass().getName(), e);
        }
        return result;
    }

    private static JsonNode serializeValue(Object value) {
        if (value == null) {
            return MissingNode.getInstance();
        }
        return JsonUtils.getObjectMapper().valueToTree(value);
    }
}
