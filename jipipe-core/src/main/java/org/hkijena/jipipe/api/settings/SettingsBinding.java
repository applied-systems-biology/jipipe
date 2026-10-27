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
 * Two guarantees:
 * <ul>
 * <li><b>Defaults are never materialized.</b> Writing a value that equals
 * the key's default removes the sub-path from the document instead of
 * storing it, so {@link #isModified()} is exactly "a value is present".</li>
 * <li><b>Re-entrancy guard.</b> Subscribers fire only when the <em>effective</em>
 * value changes: a {@code set(x)} whose value already equals {@code x} fires
 * nothing, and an incoming event whose new effective value equals the last
 * effective value seen at the path is not delivered.</li>
 * </ul>
 *
 * @param <T> the type of the setting's value, as declared by the key
 */
public final class SettingsBinding<T> {
    private static final Logger logger = LoggerFactory.getLogger(SettingsBinding.class);

    private final SettingsDocument document;
    private final JIPipeSettingKey<T> key;
    private final String pathWithSubPath;
    private final List<Consumer<T>> subscribers;
    /**
     * The listener registered on the document for this binding's path. Held
     * as a field so the document's reference can be removed from outside
     * when the binding's owner decides the binding is no longer needed.
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
     * Creates a binding for the given key over the given document. The
     * binding immediately registers a path listener on the document for
     * {@code key.blockPath() + "/" + key.subPath()}.
     *
     * @param document the settings document to read from and write to
     * @param key the typed key of the setting
     */
    public SettingsBinding(SettingsDocument document, JIPipeSettingKey<T> key) {
        this.document = Objects.requireNonNull(document, "document");
        this.key = Objects.requireNonNull(key, "key");
        this.pathWithSubPath = key.blockPath() + "/" + key.subPath();
        this.subscribers = new CopyOnWriteArrayList<>();
        this.listener = this::handleDocumentChange;
        this.lastKnown = get();
        document.addPathListener(pathWithSubPath, listener);
    }

    /**
     * @return the current effective value: the stored value converted to the
     * key's type, or the key's default when the sub-path is absent or the
     * stored value cannot be converted to the type. This method never throws.
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
     * the same key) fire only when the effective value changes.
     *
     * @param value the value to store
     */
    public void set(T value) {
        Objects.requireNonNull(value, "value");
        if (Objects.equals(value, key.defaultValue())) {
            document.remove(key.blockPath(), key.subPath());
        } else {
            document.set(key.blockPath(), key.subPath(), valueToTree(value));
        }
    }

    /**
     * Removes the sub-path from the document, so the setting reads its
     * default again. Subscribers fire when the effective value changes.
     */
    public void reset() {
        document.remove(key.blockPath(), key.subPath());
    }

    /**
     * @return whether a value is present in the document for this key, i.e.
     * whether the setting deviates from its default
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
        for (Consumer<T> subscriber : subscribers) {
            subscriber.accept(value);
        }
    }

    private T convert(JsonNode node) {
        try {
            if (key.type() == JsonNode.class) {
                @SuppressWarnings("unchecked") T asNode = (T) node;
                return asNode;
            }
            return JsonUtils.getObjectMapper().treeToValue(node, key.type());
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
