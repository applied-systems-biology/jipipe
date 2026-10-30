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
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A typed, observable view over one setting: one sub-path of one block of
 * one {@link SettingsDocument}.
 * <p>
 * Bindings are how features read and write settings and stay in sync with
 * each other and with external writers: {@link #get()} converts the stored
 * JSON to the key's type (falling back to the key's default when the value
 * is absent or malformed), {@link #set(Object)} writes through to the
 * document, and {@link #subscribe(Consumer)} registers a callback that fires
 * on <em>every</em> change to that path — including changes made by other
 * bindings over the same document, raw document edits, and document-level
 * replacements such as an external reload.
 * <p>
 * Guarantees:
 * <ul>
 * <li><b>Defaults are never materialized.</b> Writing a value that equals
 * the key's default removes the sub-path from the document instead of
 * storing it, so {@link #isModified()} is exactly "a value is present".</li>
 * <li><b>Re-entrancy guard.</b> Subscribers fire only when the <em>effective</em>
 * value changes: a {@code set(x)} whose value already equals {@code x} fires
 * nothing, and an incoming event whose new effective value equals the last
 * effective value seen at the path is not delivered.</li>
 * <li><b>Subscribers cannot break each other or the writer.</b> A subscriber
 * that throws is caught and logged; the remaining subscribers still fire and
 * the original mutation (e.g. the caller's {@code set()}) completes normally.</li>
 * <li><b>{@link #get()} never throws.</b> Absent and malformed values —
 * including a stored JSON {@code null} — fall back to the key's default.</li>
 * </ul>
 * <p>
 * Keys of type {@code JsonNode} ({@code object(...)} keys) hand out the
 * <em>live, externally-mutable</em> node that is stored in the document:
 * mutating it changes the document's content directly and does <em>not</em>
 * fire listeners; use {@link #set(Object)} (or the document's {@code set})
 * for observable changes.
 * <p>
 * A binding is <em>attached</em> to its document while it lives: it registers
 * a path listener in its constructor. Owners that create short-lived
 * bindings over a long-lived document (a feature that observes a value
 * while a panel is open) must call {@link #close()} when done, so the
 * document releases the listener, the binding, and its subscribers.
 */
public final class SettingsBinding<T> {
    private static final Logger logger = LoggerFactory.getLogger(SettingsBinding.class);

    private final SettingsDocument document;
    private final JIPipeSettingKey<T> key;
    private final String pathWithSubPath;
    private final List<Consumer<T>> subscribers;
    /**
     * The listener registered on the document for this binding's path. Held
     * as a field so {@link #close()} can remove it again from the document.
     */
    private final Consumer<JsonNode> listener;
    /**
     * The last effective value observed at this path: initialized to the
     * current effective value when the binding is created and updated by
     * every change event on the path (so it always equals the current
     * effective value, because every mutation of the path is an event).
     * Events whose effective value equals this are suppressed — the
     * re-entrancy guard.
     */
    private volatile T lastKnown;
    /**
     * Whether {@link #close()} has been called; closed bindings detach their
     * listener and receive no further events.
     */
    private volatile boolean closed;
    /**
     * The access-policy guard consulted before every document mutation (see
     * {@link #setWriteGuard(SettingsWriteGuard)}). Read by whichever thread
     * writes; hence volatile.
     */
    private volatile SettingsWriteGuard writeGuard = SettingsWriteGuard.ALLOW_ALL;

    /**
     * Creates a binding for the given key over the given document. The
     * binding immediately registers a path listener on the document for its
     * (normalized) key path. Call {@link #close()} when the binding is no
     * longer needed and the document may outlive it.
     * <p>
     * The binding is created without a write guard (every write permitted);
     * the owning {@code JIPipeSettingsServiceComponent} installs its guard via
     * {@link #setWriteGuard(SettingsWriteGuard)} right after construction.
     *
     * @param document the settings document to read from and write to
     * @param key the typed key of the setting
     */
    public SettingsBinding(SettingsDocument document, JIPipeSettingKey<T> key) {
        this.document = Objects.requireNonNull(document, "document");
        this.key = Objects.requireNonNull(key, "key");
        this.pathWithSubPath = SettingsDocument.listenerPath(key.blockPath(), key.subPath());
        this.subscribers = new CopyOnWriteArrayList<>();
        this.listener = this::handleDocumentChange;
        this.lastKnown = get();
        document.addPathListener(pathWithSubPath, listener, this);
    }

    /**
     * Installs the access-policy guard consulted by {@link #set(Object)} and
     * {@link #reset()} before the document is mutated. A denied write throws
     * {@link SettingsAccessDeniedException} and leaves the document untouched.
     * <p>
     * Set by the owning settings component; a binding used stand-alone keeps
     * the permissive default.
     *
     * @param writeGuard the guard, or {@code null} to permit every write
     */
    public void setWriteGuard(SettingsWriteGuard writeGuard) {
        this.writeGuard = writeGuard == null ? SettingsWriteGuard.ALLOW_ALL : writeGuard;
    }

    /**
     * @return the access-policy guard consulted before every write
     */
    public SettingsWriteGuard writeGuard() {
        return writeGuard;
    }

    /**
     * @return the current effective value: the stored value converted to the
     * key's type, or the key's default when the sub-path is absent, holds a
     * JSON {@code null}, or cannot be converted to the type. This method
     * never throws. A closed binding still reads through to the document.
     */
    public T get() {
        JsonNode node = document.get(key.blockPath(), key.subPath());
        if (node.isMissingNode()) {
            return key.defaultValue();
        }
        return convert(node);
    }

    /**
     * Writes the value to the document. A value that equals the key's
     * default removes the sub-path instead of storing it, so defaults are
     * never materialized. Subscribers (including those of other bindings for
     * the same key) fire only when the effective value changes. A closed
     * binding still writes through to the document.
     * <p>
     * Keys whose default is {@code null} (e.g. {@code object(...)} keys) are
     * the only ones for which {@code set(null)} is meaningful: it stores a
     * JSON {@code null}, and {@link #get()} then falls back to that
     * {@code null} default. For keys with a non-null default,
     * {@code set(null)} is rejected with a {@link NullPointerException}.
     *
     * @param value the value to store
     */
    public void set(T value) {
        if (value == null && !Objects.equals(key.defaultValue(), null)) {
            throw new NullPointerException(
                    "Cannot set a null value for " + pathWithSubPath
                            + ": its default is non-null, so null cannot be distinguished from \"reset\"");
        }
        checkWriteAllowed();
        if (Objects.equals(value, key.defaultValue())) {
            document.remove(key.blockPath(), key.subPath());
        } else {
            document.set(key.blockPath(), key.subPath(), value == null ? null : valueToTree(value));
        }
    }

    /**
     * Removes the sub-path from the document, so the setting reads its
     * default again. Subscribers fire when the effective value changes.
     * Works on closed bindings too (it never touched the listener). A reset
     * is a write: it is rejected by the write guard like {@link #set(Object)}.
     */
    public void reset() {
        checkWriteAllowed();
        document.remove(key.blockPath(), key.subPath());
    }

    /**
     * Rejects the write with a {@link SettingsAccessDeniedException} when the
     * installed guard denies it. Called before any document mutation, so a
     * rejected write never leaves a partial change behind.
     */
    private void checkWriteAllowed() {
        if (!writeGuard.test(key)) {
            throw new SettingsAccessDeniedException(key.blockPath(), key.subPath(), key.access());
        }
    }

    /**
     * @return whether a value is present in the document for this key, i.e.
     * whether the setting deviates from its default. Works on closed
     * bindings too.
     */
    public boolean isModified() {
        return document.has(key.blockPath(), key.subPath());
    }

    /**
     * Registers a subscriber for effective value changes on this path.
     * Subscribers are invoked synchronously on the thread that caused the
     * change, and only when the effective value actually changes (re-entrancy
     * guard). A subscriber is registered at most once: re-subscribing one
     * whose {@code equals()} matches an existing registration is a no-op.
     * <p>
     * Subscribing a <em>closed</em> binding is allowed, but the binding stays
     * detached from the document: the subscriber will never fire.
     *
     * @param subscriber the callback, receiving the new effective value
     */
    public void subscribe(Consumer<T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }

    /**
     * Removes a subscriber previously registered with {@link #subscribe(Consumer)}.
     * Removing a subscriber that is not registered is a no-op. Subscribers
     * are matched by {@code equals}, so lambdas work only while the same
     * instance is passed; use fields or method references for durable
     * subscriptions.
     *
     * @param subscriber the subscriber to remove
     */
    public void unsubscribe(Consumer<T> subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Detaches this binding from its document: the path listener is removed
     * and the subscriber list is cleared. This method is idempotent.
     * <p>
     * After closing, the binding no longer receives change events; late
     * {@link #subscribe(Consumer)} calls are accepted but never fire.
     * {@link #get()}, {@link #set(Object)}, {@link #reset()} and
     * {@link #isModified()} keep working against the document.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        document.removePathListener(pathWithSubPath, listener);
        subscribers.clear();
    }

    /**
     * @return the key this binding reads and writes
     */
    public JIPipeSettingKey<T> key() {
        return key;
    }

    private void handleDocumentChange(JsonNode node) {
        T value = node.isMissingNode() ? key.defaultValue() : convert(node);
        if (Objects.equals(value, lastKnown)) {
            return;
        }
        lastKnown = value;
        // Iterate a snapshot: a subscriber may register or remove subscribers
        // (or bindings with their own document listeners) during dispatch.
        for (Consumer<T> subscriber : List.copyOf(subscribers)) {
            try {
                subscriber.accept(value);
            } catch (Exception e) {
                logger.warn("Settings subscriber for {} threw; other subscribers were still notified",
                        pathWithSubPath, e);
            }
        }
    }

    private T convert(JsonNode node) {
        try {
            if (key.type() == JsonNode.class) {
                @SuppressWarnings("unchecked") T asNode = (T) node;
                return asNode;
            }
            T value = JsonUtils.getObjectMapper().treeToValue(node, key.type());
            if (value == null) {
                // A stored JSON null converts to null; treat that as
                // malformed so callers never receive a surprise null.
                logger.warn("Stored value of setting {} is JSON null; falling back to the default", pathWithSubPath);
                return key.defaultValue();
            }
            return value;
        } catch (Exception e) {
            logger.warn("Could not convert stored value of setting {} ({}) to {}: falling back to the default",
                    pathWithSubPath, node, key.type(), e);
            return key.defaultValue();
        }
    }

    private static JsonNode valueToTree(Object value) {
        if (value instanceof JsonNode node) {
            return node;
        }
        return JsonUtils.getObjectMapper().valueToTree(value);
    }
}
