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
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An in-memory, scoped container for settings blocks.
 * <p>
 * A document is a plain JSON object tree. Top-level fields that start with
 * {@code "$"} are reserved: {@code $schema} holds the schema version
 * (default {@value #DEFAULT_SCHEMA_VERSION}) and {@code $meta} holds
 * bookkeeping metadata (e.g. {@code written-by-version}, {@code written-at},
 * {@code writer-id}). All other top-level fields are <em>blocks</em>, keyed
 * by their canonical block path of the form {@code "<prefix>:<id>"}
 * (see {@link SettingsBlockPath}).
 * <p>
 * The document never materializes defaults: reading an absent sub-path
 * yields a {@link MissingNode} rather than a type default.
 * <p>
 * Values within a block are addressed by slash-separated sub-paths that are
 * normalized like {@code JIPipePathMetadataStore.key(String...)}, i.e.
 * empty segments are ignored, so {@code "a//b"} and {@code "a/b"} denote the
 * same location. An empty sub-path addresses the whole block.
 * <p>
 * A block is <em>pass-through</em> when its kind is unknown or its id is not
 * registered; pass-through blocks are stored and written back verbatim, and
 * reading or writing their sub-paths is allowed but only raw. The
 * pass-through decision is delegated to a {@link Predicate} that receives the
 * canonical block path; by default a block is pass-through exactly when its
 * kind is unknown.
 * <p>
 * This class is a pure data structure: it performs no IO and the
 * {@code $meta} values are interpolated by the caller. The only form of
 * notification is the per-path listener API ({@link #addPathListener},
 * {@link #removePathListener}), which lets in-process observers (such as
 * {@link SettingsBinding}s) be told when the value at a specific path
 * changes.
 * <p>
 * <b>Threading contract.</b> A document is shared between threads: UI and
 * feature code mutate it on the Swing EDT (bindings write through
 * {@link #set}/{@link #setBlock} without any outer lock), while the
 * settings service's foreign-change merge mutates the same document on a
 * background thread. To make that safe, every mutating operation (and the
 * listener registration it uses) is {@link Object#synchronized
 * synchronized} on the document instance, and notifications are dispatched
 * while that same lock is held. The lock is <b>reentrant</b>, so a listener
 * that writes back into the document from inside its callback is safe (the
 * change it causes is then dispatched after it returns, in the normal
 * nested-call order). Listeners therefore run on the mutating thread while
 * the document is locked: they must be short and must not block on another
 * thread that could itself touch this document (e.g. do not wait for another
 * thread to save the settings from inside a listener).
 * <p>
 * Reads ({@link #get}, {@link #has}, {@link #getBlock}, {@link #blockPaths},
 * {@link #subPaths}) are not synchronized: a {@link JsonNode} obtained from
 * a read is a live view that may be mutated or replaced by a concurrent
 * write. Read into a local value and do not mutate the returned node, or
 * perform multi-read sequences in the same thread as the writes. The
 * settings service serializes its own reads/writes with its component
 * monitor, and the document lock covers the mutation itself.
 */
public final class SettingsDocument {
    private static final String SCHEMA_FIELD = "$schema";
    private static final String META_FIELD = "$meta";
    private static final int DEFAULT_SCHEMA_VERSION = 2;

    private final SettingsScope scope;
    private final ObjectNode root;
    private final String writerId;
    private final Predicate<String> registeredBlock;
    private final Map<String, List<Consumer<JsonNode>>> pathListeners;

    /**
     * Creates a document with the default registration check, which treats a
     * block as registered whenever its kind is known.
     *
     * @param scope the scope this document belongs to
     */
    public SettingsDocument(SettingsScope scope) {
        this(scope, p -> new SettingsBlockPath(p).kind() != null);
    }

    /**
     * Creates a document with a custom registration check.
     *
     * @param scope the scope this document belongs to
     * @param registeredBlock a predicate that receives a canonical block path
     *                        (e.g. {@code "sheet:org.hkijena.jipipe:general-ui"})
     *                        and returns {@code true} when the block is registered
     */
    public SettingsDocument(SettingsScope scope, Predicate<String> registeredBlock) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.registeredBlock = Objects.requireNonNull(registeredBlock, "registeredBlock");
        this.root = JsonUtils.getObjectMapper().createObjectNode();
        this.root.put(SCHEMA_FIELD, DEFAULT_SCHEMA_VERSION);
        this.root.putObject(META_FIELD);
        this.writerId = UUID.randomUUID().toString().replace("-", "");
        this.pathListeners = new ConcurrentHashMap<>();
    }

    /**
     * Registers a listener that is notified whenever the value at the given
     * path changes. The listener receives the new value at that location, or
     * a {@link MissingNode} when the location is absent after the mutation.
     * <p>
     * {@code pathWithSubPath} is {@code "<blockPath>/<subPath>"}, using the
     * same sub-path normalization as {@link #get(String, String)} (empty
     * segments are dropped); a block path without a sub-path addresses the
     * whole block. Notifications are sent by {@link #set}, {@link #remove},
     * {@link #setBlock}, and {@link #removeBlock}: listeners registered for
     * the mutated path are always notified, and a listener registered under a
     * block whose <em>whole block</em> was replaced or removed is notified as
     * well, with the value at its location in the new block (or a
     * {@link MissingNode} when its location no longer exists). Listeners are
     * invoked synchronously on the mutating thread, after the mutation is
     * applied.
     *
     * @param pathWithSubPath the path to observe
     * @param listener the listener to fire with the new value at the path
     */
    public synchronized void addPathListener(String pathWithSubPath, Consumer<JsonNode> listener) {
        pathListeners
                .computeIfAbsent(Objects.requireNonNull(pathWithSubPath, "pathWithSubPath"), p -> new ArrayList<>())
                .add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Removes a listener previously registered with {@link #addPathListener}.
     * Removing a listener that is not registered is a no-op.
     *
     * @param pathWithSubPath the path the listener was registered for
     * @param listener the listener to remove
     */
    public synchronized void removePathListener(String pathWithSubPath, Consumer<JsonNode> listener) {
        List<Consumer<JsonNode>> listeners = pathListeners.get(pathWithSubPath);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * @return the scope this document belongs to
     */
    public SettingsScope scope() {
        return scope;
    }

    /**
     * @return the live, mutable root of the document
     */
    public ObjectNode root() {
        return root;
    }

    /**
     * @return a fixed, random 32-character lowercase hex identifier for this
     * instance, suitable for recording in {@code $meta} as {@code writer-id}
     */
    public String writerId() {
        return writerId;
    }

    /**
     * @param blockPath the canonical block path
     * @return the block's JSON, or a {@link MissingNode} if the block is absent
     */
    public JsonNode getBlock(String blockPath) {
        return node(blockPath);
    }

    /**
     * Replaces the block at the given path with the given value.
     *
     * @param blockPath the canonical block path
     * @param value the new block value, or {@code null} to remove the block
     */
    public synchronized void setBlock(String blockPath, JsonNode value) {
        if (value == null || value.isMissingNode()) {
            root.remove(blockPath);
        } else {
            root.set(blockPath, value);
        }
        notifyBlockAndDescendants(blockPath);
    }

    /**
     * @param blockPath the canonical block path
     * @return whether the block exists
     */
    public boolean hasBlock(String blockPath) {
        return root.has(blockPath);
    }

    /**
     * @param blockPath the canonical block path
     * @return whether the block is pass-through, i.e. its kind is unknown or
     * its id is not registered; pass-through blocks are stored and written
     * back verbatim
     */
    public boolean isPassThrough(String blockPath) {
        return !registeredBlock.test(new SettingsBlockPath(blockPath).path());
    }

    /**
     * Reads a value from a block by sub-path. An empty or {@code null}
     * sub-path addresses the whole block.
     *
     * @param blockPath the canonical block path
     * @param subPath slash-separated sub-path within the block, or empty for the whole block
     * @return the value at the location, or a {@link MissingNode} if the block
     * or any sub-path segment is absent; defaults are never materialized
     */
    public JsonNode get(String blockPath, String subPath) {
        return node(subSegments(blockPath, subPath));
    }

    /**
     * Writes a value into a block at the given sub-path, creating intermediate
     * object nodes as needed. An empty or {@code null} sub-path replaces the
     * whole block.
     *
     * @param blockPath the canonical block path
     * @param subPath slash-separated sub-path within the block, or empty for the whole block
     * @param value the value to store; a {@code null} or missing value removes the location
     */
    public synchronized void set(String blockPath, String subPath, JsonNode value) {
        List<String> segments = subSegments(blockPath, subPath);
        if (value == null || value.isMissingNode()) {
            removeAt(segments);
        } else {
            ObjectNode parent = objectAt(segments, segments.size() - 1);
            parent.set(segments.get(segments.size() - 1), value);
        }
        if (segments.size() == 1) {
            notifyBlockAndDescendants(blockPath);
        } else {
            notifyPathListener(listenerPath(blockPath, subPath));
        }
    }

    /**
     * @param blockPath the canonical block path
     * @param subPath slash-separated sub-path within the block, or empty for the whole block
     * @return whether a value is present at the location
     */
    public boolean has(String blockPath, String subPath) {
        JsonNode n = node(subSegments(blockPath, subPath));
        return !n.isMissingNode();
    }

    /**
     * Removes the value at the given sub-path. An empty or {@code null} sub-path
     * removes the whole block. Absent locations are left untouched.
     *
     * @param blockPath the canonical block path
     * @param subPath slash-separated sub-path within the block, or empty for the whole block
     */
    public synchronized void remove(String blockPath, String subPath) {
        String path = listenerPath(blockPath, subPath);
        removeAt(subSegments(blockPath, subPath));
        if (path.equals(blockPath)) {
            notifyBlockAndDescendants(blockPath);
        } else {
            notifyPathListener(path);
        }
    }

    /**
     * Removes the whole block at the given path. Absent blocks are left untouched.
     *
     * @param blockPath the canonical block path
     */
    public synchronized void removeBlock(String blockPath) {
        root.remove(Objects.requireNonNull(blockPath, "blockPath"));
        notifyBlockAndDescendants(blockPath);
    }

    /**
     * @return the canonical block paths present in the document, excluding
     * reserved {@code $}-prefixed fields
     */
    public synchronized Set<String> blockPaths() {
        Set<String> paths = new LinkedHashSet<>();
        root.fieldNames().forEachRemaining(f -> {
            if (!f.startsWith("$")) {
                paths.add(f);
            }
        });
        return paths;
    }

    /**
     * @param blockPath the canonical block path
     * @return all slash-joined sub-paths of nested values within the block,
     * including the full path of the block's direct fields; empty if the block
     * is absent or holds a non-object value
     */
    public synchronized Set<String> subPaths(String blockPath) {
        Set<String> result = new LinkedHashSet<>();
        collectSubPaths(node(Objects.requireNonNull(blockPath, "blockPath")), "", result);
        return result;
    }

    /**
     * @return the schema version declared in the {@code $schema} field
     */
    public int schemaVersion() {
        return node(SCHEMA_FIELD).asInt(DEFAULT_SCHEMA_VERSION);
    }

    /**
     * Sets the {@code $schema} field.
     *
     * @param version the new schema version
     */
    public synchronized void setSchemaVersion(int version) {
        root.put(SCHEMA_FIELD, version);
    }

    /**
     * @return the live {@code $meta} object node; created on first access
     */
    public synchronized ObjectNode meta() {
        return root.get(META_FIELD) instanceof ObjectNode m ? m : root.putObject(META_FIELD);
    }

    private JsonNode node(String... segments) {
        return node(List.of(segments));
    }

    private JsonNode node(List<String> segments) {
        JsonNode current = root;
        for (String segment : segments) {
            current = current.get(segment);
            if (current == null) {
                return MissingNode.getInstance();
            }
        }
        return current;
    }

    private synchronized ObjectNode objectAt(List<String> segments, int depth) {
        ObjectNode current = root;
        for (int i = 0; i < depth; i++) {
            String segment = segments.get(i);
            JsonNode next = current.get(segment);
            if (next instanceof ObjectNode o) {
                current = o;
            } else {
                ObjectNode created = JsonUtils.getObjectMapper().createObjectNode();
                current.set(segment, created);
                current = created;
            }
        }
        return current;
    }

    private synchronized void removeAt(List<String> segments) {
        if (segments.size() == 1) {
            root.remove(segments.get(0));
            return;
        }
        JsonNode parent = node(segments.subList(0, segments.size() - 1).toArray(new String[0]));
        if (parent instanceof ObjectNode o) {
            o.remove(segments.get(segments.size() - 1));
        }
    }

    private List<String> subSegments(String blockPath, String subPath) {
        List<String> segments = new ArrayList<>();
        segments.add(Objects.requireNonNull(blockPath, "blockPath"));
        if (subPath != null) {
            for (String segment : subPath.split("/")) {
                if (!segment.isEmpty()) {
                    segments.add(segment);
                }
            }
        }
        return segments;
    }

    /**
     * Normalizes a block path plus sub-path into the listener key form
     * {@code "<blockPath>/<subPath>"}, dropping empty sub-path segments, so
     * {@code ("a:b", "x//y")} yields {@code "a:b/x/y"}. An empty or
     * {@code null} sub-path addresses the whole block and yields the block
     * path itself. Package-visible so {@link SettingsBinding} registers its
     * listener under the exact same key the document uses for dispatch.
     *
     * @param blockPath the canonical block path
     * @param subPath the slash-separated sub-path, or {@code null}
     * @return the normalized listener path
     */
    static String listenerPath(String blockPath, String subPath) {
        StringBuilder normalized = new StringBuilder(Objects.requireNonNull(blockPath, "blockPath"));
        if (subPath != null) {
            for (String segment : subPath.split("/")) {
                if (!segment.isEmpty()) {
                    normalized.append('/').append(segment);
                }
            }
        }
        return normalized.toString();
    }

    private synchronized void notifyBlockAndDescendants(String blockPath) {
        String prefix = blockPath + "/";
        notifyPathListener(blockPath);
        for (String path : pathListeners.keySet()) {
            if (path.startsWith(prefix)) {
                notifyPathListener(path);
            }
        }
    }

    private synchronized void notifyPathListener(String pathWithSubPath) {
        List<Consumer<JsonNode>> listeners = pathListeners.get(pathWithSubPath);
        if (listeners == null) {
            return;
        }
        int slash = pathWithSubPath.indexOf('/');
        String blockPath = slash > 0 ? pathWithSubPath.substring(0, slash) : pathWithSubPath;
        String subPath = slash > 0 ? pathWithSubPath.substring(slash + 1) : "";
        JsonNode value = get(blockPath, subPath);
        // Iterate a snapshot: listeners may add or remove listeners for the
        // same path while they are being notified (e.g. a subscriber that
        // creates another binding on the same key).
        for (Consumer<JsonNode> listener : new ArrayList<>(listeners)) {
            listener.accept(value);
        }
    }

    private void collectSubPaths(JsonNode value, String prefix, Set<String> result) {
        if (!value.isObject()) {
            return;
        }
        String joiner = prefix.isEmpty() ? "" : prefix + "/";
        value.fieldNames().forEachRemaining(f -> {
            String child = joiner + f;
            result.add(child);
            collectSubPaths(value.get(f), child, result);
        });
    }
}
