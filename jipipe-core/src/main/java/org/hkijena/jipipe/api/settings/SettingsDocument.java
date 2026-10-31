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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
 * Addressing is <b>type-directed and symmetric for reads and writes</b>: a
 * segment addresses an array element (its decimal index) when the current node
 * is an array, and an object field when the current node is an object. A block
 * holding a JSON array (e.g. {@code state:recent-projects}) therefore reads
 * element {@code i} as {@code get(blockPath, String.valueOf(i))} and writes it
 * as {@code set(blockPath, String.valueOf(i), value)}; a numeric segment under
 * an object still names the object field {@code "0"}. Writes grow an array
 * (with {@code null} placeholders) when the index is past its end.
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
    // Deliberately duplicated in SettingsDocumentIO (which also defines these
    // three names): each class owns the constants of the artifact version it
    // writes, so the two can be version-pinned and migrated independently
    // without one class silently inheriting the other's value.
    private static final String SCHEMA_FIELD = "$schema";
    private static final String META_FIELD = "$meta";
    private static final int DEFAULT_SCHEMA_VERSION = 2;

    private final SettingsScope scope;
    private final ObjectNode root;
    private final String writerId;
    private final Predicate<String> registeredBlock;
    private final Map<String, List<Consumer<JsonNode>>> pathListeners;
    /**
     * The owned subset of {@link #pathListeners}: listeners registered with an
     * explicit owner (identity-based) through
     * {@link #addPathListener(String, Consumer, Object)}. Only the testing seam
     * {@link #pathListenerCount(String)} reads it; it exists so a lifecycle
     * test observes the per-bind listeners and not the settings service's
     * fixed, per-location observers.
     */
    private final Map<String, Set<Consumer<JsonNode>>> ownedPathListeners;

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
        this.ownedPathListeners = new ConcurrentHashMap<>();
    }

    /**
     * Registers a listener that is notified whenever the value at the given
     * path changes. The listener receives the new value at that location, or
     * a {@link MissingNode} when the location is absent after the mutation.
     * <p>
     * {@code pathWithSubPath} is {@code "<blockPath>/<subPath>"}, using the
     * same sub-path normalization as {@link #get(String, String)}. That
     * normalization — <b>split the sub-path on "/" and drop empty segments</b>
     * — <em>is</em> the addressing contract (it is not the
     * {@code JIPipeSettingKey}-style composite-key scheme). A block path
     * without a sub-path addresses the whole block.
     * <p>
     * <b>Null handling is deliberately asymmetric:</b> the sub-path accessors
     * ({@link #get}, {@link #set}, {@link #has}, {@link #remove}) require a
     * non-{@code null} block path, while {@link #getBlock}/{@link #hasBlock}
     * tolerate {@code null} (they answer "nothing there").
     * <p>
     * Notifications are sent by {@link #set}, {@link #remove},
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
        addPathListener(pathWithSubPath, listener, null);
    }

    /**
     * Registers a path listener together with its <em>owner</em> (see
     * {@link #addPathListener(String, Consumer)}). The owner is only used to
     * attribute the listener for the testing seam
     * {@link #pathListenerCount(String)}; it is not retained beyond that.
     * {@link SettingsBinding} registers its listener with itself as owner, so
     * a test can observe exactly the per-binding listeners.
     *
     * @param pathWithSubPath the path to observe
     * @param listener the listener to fire with the new value at the path
     * @param owner the owner of the listener, or {@code null} for a
     *              service-owned observer
     */
    public synchronized void addPathListener(String pathWithSubPath, Consumer<JsonNode> listener, Object owner) {
        pathListeners
                .computeIfAbsent(Objects.requireNonNull(pathWithSubPath, "pathWithSubPath"), p -> new ArrayList<>())
                .add(Objects.requireNonNull(listener, "listener"));
        if (owner != null) {
            ownedPathListeners
                    .computeIfAbsent(pathWithSubPath, p -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(listener);
        }
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
        Set<Consumer<JsonNode>> owned = ownedPathListeners.get(pathWithSubPath);
        if (owned != null) {
            owned.remove(listener);
        }
    }

    /**
     * <b>Testing only — not part of the production API.</b> The parameter of
     * {@link #pathListenerCount(String)} for a key: the normalized
     * {@code "<blockPath>/<subPath>"} listener path of the given location.
     * Exists so lifecycle tests can observe a listener without depending on
     * the package-private normalization helper.
     *
     * @param blockPath the canonical block path
     * @param subPath   the slash-separated sub-path
     * @return the normalized listener path
     */
    public static String listenerPathForTesting(String blockPath, String subPath) {
        return listenerPath(blockPath, subPath);
    }

    /**
     * @return the scope this document belongs to
     */
    public SettingsScope scope() {
        return scope;
    }

    /**
     * <b>Testing only — not part of the production API.</b> Counts only the
     * path listeners registered with an explicit owner through
     * {@link #addPathListener(String, Consumer, Object)}, i.e. the listeners a
     * {@link SettingsBinding} adds. The settings service's own observers are
     * registered without an owner and are deliberately not counted: they are
     * one per registered location and never per user action, so they must not
     * mask the per-binding lifecycle this count exists to observe. Production
     * code must never branch on this value; it exists only as an observation
     * seam for the lifecycle tests ({@code SettingsBindingLifecycleTest}).
     *
     * @param pathWithSubPath the path in the same {@code "<blockPath>/<subPath>"}
     *                        form {@link #addPathListener} takes
     * @return the number of owned listeners registered at exactly that path
     */
    public synchronized int pathListenerCount(String pathWithSubPath) {
        List<Consumer<JsonNode>> listeners = pathListeners.get(pathWithSubPath);
        if (listeners == null) {
            return 0;
        }
        Set<Consumer<JsonNode>> owned = ownedPathListeners.getOrDefault(pathWithSubPath, Set.of());
        int count = 0;
        for (Consumer<JsonNode> listener : listeners) {
            if (owned.contains(listener)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the live, mutable root of the document
     * <p>
     * <b>Concurrency:</b> this returns the tree itself, not a copy, and the
     * read does <em>not</em> take the document lock. Another thread may be
     * mutating the same tree (a binding write on the EDT, a foreign merge on
     * the settings service's executor), so traversing or serializing this
     * node while that can happen is unsafe (it may throw or produce an
     * inconsistent tree). Use {@link #snapshotJson()} to obtain a consistent
     * deep copy, or only call this where no concurrent mutation is possible.
     */
    public ObjectNode root() {
        return root;
    }

    /**
     * Returns a consistent deep copy of the document's tree, taken while
     * holding the document lock, so concurrent writers cannot produce a torn
     * or inconsistent snapshot.
     * <p>
     * This is the safe way to serialize a live document: the returned node is
     * detached (mutating it does not affect the document) and is exactly the
     * state at one instant. Prefer this over reading {@link #root()} and
     * copying it yourself.
     *
     * @return a detached deep copy of the document's root
     */
    public synchronized ObjectNode snapshotJson() {
        return root.deepCopy();
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
     * <p>
     * <b>{@code "$schema"} and {@code "$meta"} are reserved field names and,
     * although they are not rejected here, must not be passed as a block
     * path.</b> The absence of a guard is intentional: this is the low-level
     * document primitive that {@link SettingsDocumentIO} itself uses to restore
     * {@code $meta} on load, and a raw editor may legitimately want to inspect
     * the reserved fields. Application code addresses settings blocks only.
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
            setAt(segments, value);
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
     * The {@code $meta} object already exists: the constructor creates it, so
     * this never materializes it lazily. The defensive branch below only
     * repairs a document whose {@code $meta} was replaced by a non-object
     * (e.g. by a raw {@link #setBlock}).
     *
     * @return the live {@code $meta} object node
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
            if (current.isArray() && isArrayIndex(segment)) {
                // Arrays are addressable by numeric index, so a list-valued
                // block (e.g. "state:recent-projects") can be read as
                // ("state:recent-projects", "0"). JsonNode#get(String) only
                // addresses object fields and would yield a MissingNode here.
                current = current.get(Integer.parseInt(segment));
            } else {
                current = current.get(segment);
            }
            if (current == null) {
                return MissingNode.getInstance();
            }
        }
        return current;
    }

    /**
     * @param segment a sub-path segment
     * @return whether the segment is a non-negative decimal array index
     */
    private static boolean isArrayIndex(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (segment.charAt(i) < '0' || segment.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes {@code value} at {@code segments}, using exactly the same
     * type-directed resolution as {@link #node(List)}: a numeric segment
     * addresses an array element (its index) when the current node is an
     * array, and an object field when the current node is an object.
     * <p>
     * Intermediate containers are created when missing (an object, or an array
     * when the next segment is numeric), and an array is grown with
     * {@code null} placeholders when the index is past its end. A missing
     * location whose current node is a scalar is replaced by the new
     * container, matching the historic "create intermediate objects" rule.
     */
    private synchronized void setAt(List<String> segments, JsonNode value) {
        String blockPath = segments.get(0);
        if (segments.size() == 1) {
            // A bare block path replaces the whole block.
            root.set(blockPath, value);
            return;
        }
        JsonNode block = root.get(blockPath);
        if (block == null || block.isNull() || block.isValueNode()) {
            // The first sub-path segment resolves inside the block, which is
            // an object unless it already is an array (blocks are objects).
            block = JsonUtils.getObjectMapper().createObjectNode();
            root.set(blockPath, block);
        }
        if (setInside(block, segments, 1, value) == null) {
            // The block holds an array but a named field is addressed: it has
            // no such field, so the block is replaced by an object holding it
            // (the historic object-only writer did the same).
            JsonNode rebuilt = newContainerFor(segments.get(1));
            setInside(rebuilt, segments, 2, value);
            root.set(blockPath, rebuilt);
        }
    }

    /**
     * Walks {@code segments[from..]} inside {@code container} with the same
     * type-directed rule as {@link #node(List)} and writes {@code value} at the
     * last segment. Array elements are grown as needed.
     *
     * @return the container the value was written into (so the caller can
     * replace it when the shape has to change), or {@code null} on a write
     */
    private JsonNode setInside(JsonNode container, List<String> segments, int from, JsonNode value) {
        JsonNode current = container;
        for (int i = from; i < segments.size(); i++) {
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;
            if (current instanceof ArrayNode array) {
                if (!isArrayIndex(segment)) {
                    return null; // an array has no named fields
                }
                int index = Integer.parseInt(segment);
                while (array.size() <= index) {
                    array.addNull();
                }
                if (last) {
                    array.set(index, value);
                    return current;
                }
                JsonNode next = array.get(index);
                if (next == null || next.isNull() || next.isValueNode()) {
                    next = newContainerFor(segments.get(i + 1));
                    array.set(index, next);
                }
                current = next;
            } else if (current instanceof ObjectNode object) {
                if (last) {
                    object.set(segment, value);
                    return current;
                }
                JsonNode next = object.get(segment);
                if (next == null || next.isNull() || next.isValueNode()) {
                    next = newContainerFor(segments.get(i + 1));
                    object.set(segment, next);
                }
                current = next;
            } else {
                return null; // a scalar in the middle of the path
            }
        }
        return current;
    }

    /**
     * @param nextSegment the segment that will be resolved inside the new container
     * @return a fresh array when the next segment is numeric, an object otherwise
     */
    private static JsonNode newContainerFor(String nextSegment) {
        return isArrayIndex(nextSegment)
                ? JsonUtils.getObjectMapper().createArrayNode()
                : JsonUtils.getObjectMapper().createObjectNode();
    }

    private synchronized void removeAt(List<String> segments) {
        if (segments.size() == 1) {
            root.remove(segments.get(0));
            return;
        }
        JsonNode parent = node(segments.subList(0, segments.size() - 1).toArray(new String[0]));
        String last = segments.get(segments.size() - 1);
        if (parent instanceof ObjectNode o) {
            o.remove(last);
        } else if (parent instanceof ArrayNode a && isArrayIndex(last)) {
            int index = Integer.parseInt(last);
            if (index < a.size()) {
                a.set(index, NullNode.getInstance());
            }
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
     * path itself. Public so every observer ({@link SettingsBinding} and the
     * settings service's local-change tracking) registers its listener under
     * the exact same key the document uses for dispatch, from a single
     * implementation.
     *
     * @param blockPath the canonical block path
     * @param subPath the slash-separated sub-path, or {@code null}
     * @return the normalized listener path
     */
    public static String listenerPath(String blockPath, String subPath) {
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
