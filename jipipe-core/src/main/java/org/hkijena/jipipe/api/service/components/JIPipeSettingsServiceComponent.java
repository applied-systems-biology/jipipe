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

package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsFileStore;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.api.settings.SettingsSheetBridge;
import org.hkijena.jipipe.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The scoped settings service component: the single owner of the
 * {@link SettingsDocument}s for every {@link SettingsScope} and of the typed
 * setting keys that address them.
 * <p>
 * This is the integration point of the modern settings system (see
 * {@code docs/superpowers/specs/2026-10-27-modern-settings-system-design.md}):
 * <ul>
 * <li><b>One document per scope.</b> {@link #document(SettingsScope)} creates
 * (and, for file-backed scopes, loads) the document lazily on first access.
 * {@link SettingsScope#APPLICATION} is backed by
 * {@code <userDir>/settings.json}, {@link SettingsScope#RUNTIME} by
 * {@code <userDir>/runtime-settings.json}, and {@link SettingsScope#PROJECT}
 * by an in-memory document whose persistence is the project's concern (Task 12).
 * Repeated calls return the same document instance until {@link #reload()}
 * replaces a file-backed one.</li>
 * <li><b>Typed access.</b> {@link #bind(JIPipeSettingKey)} registers the key
 * and hands out a live {@link SettingsBinding}. Bindings handed out by this
 * component are tracked and closed in {@link #dispose()}; a caller may close
 * a binding early — its later close by the component is then a no-op, and
 * {@code get()}/{@code set()} on a closed binding keep working, so tracking
 * cannot break early-closed bindings.</li>
 * <li><b>Sheet projection.</b> {@link #attachSheet(JIPipeApplicationSettingsSheet)}
 * registers a loop-safe {@link SettingsSheetBridge} for the sheet's
 * {@code "sheet:<id>"} block against the APPLICATION document. Attaching is
 * safe before the document exists: the document is materialized (lazily) on
 * demand.</li>
 * <li><b>Persistence.</b> {@link #save()} flushes all attached sheet bridges
 * into the document and then atomically writes every file-backed document.
 * {@link #saveLater()} coalesces rapid changes into one save after a 250 ms
 * quiet period, matching the legacy {@code saveLater} contract.</li>
 * </ul>
 * <p>
 * <b>Swing is optional.</b> The legacy component used a
 * {@link javax.swing.Timer} for debouncing, which initializes the Swing
 * subsystem. This component does so only when an EDT is available (the
 * environment is not headless); headless/CLI runs use a
 * {@link ScheduledExecutorService} instead so saves are still coalesced
 * without pulling in Swing.
 * <p>
 * <b>Change notification.</b> Each managed document has path listeners that
 * re-emit every observed mutation as a {@link SettingsChangedEvent} on
 * {@link #getChangedEventEmitter()} (scope, path, old and new value, and a
 * {@link ChangeOrigin}) and as a {@link LegacyChangedEvent} on
 * {@link #getLegacyChangedEventEmitter()}, so pre-existing legacy subscribers
 * keep working until they are migrated (Task 13).
 */
public final class JIPipeSettingsServiceComponent extends JIPipeServiceComponent {
    private static final Logger logger = LoggerFactory.getLogger(JIPipeSettingsServiceComponent.class);

    /**
     * Debounce window for {@link #saveLater()}, in milliseconds. Matches the
     * legacy component's 250 ms value.
     */
    private static final long SAVE_LATER_DEBOUNCE_MILLIS = 250;

    private static final DateTimeFormatter META_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final Path userDir;
    /**
     * Whether the debounce timer runs on the Swing EDT. Resolved once at
     * construction: a Swing timer requires a live EDT, which headless
     * (CLI/CI) environments do not have.
     */
    private final boolean swingDebounce;

    /**
     * One document per scope, created lazily on first access.
     */
    private final Map<SettingsScope, SettingsDocument> documents = new LinkedHashMap<>();
    /**
     * One store per file-backed scope, created lazily alongside the document.
     */
    private final Map<SettingsScope, SettingsFileStore> stores = new LinkedHashMap<>();
    /**
     * Registered setting keys, keyed by their normalized
     * {@code "<blockPath>/<subPath>"} location.
     */
    private final Map<String, JIPipeSettingKey<?>> registeredKeys = new LinkedHashMap<>();
    /**
     * Bindings handed out through {@link #bind(JIPipeSettingKey)} that the
     * component still needs to close; closed in {@link #dispose()}.
     */
    private final Set<SettingsBinding<?>> trackedBindings = new LinkedHashSet<>();
    /**
     * Sheet bridges attached to the current APPLICATION document, keyed by
     * sheet. Re-created after a reload of the APPLICATION document.
     */
    private final Map<JIPipeApplicationSettingsSheet, SettingsSheetBridge> attachedBridges = new LinkedHashMap<>();

    private final Timer swingSaveLaterTimer;
    private final ScheduledExecutorService executorSaveLater;
    /**
     * The currently scheduled headless save task; replaced (and the previous
     * one cancelled) by each {@link #saveLater()} call, mirroring the
     * {@link Timer#restart()} semantics of the Swing path.
     */
    private volatile ScheduledFuture<?> pendingSaveTask;
    private final Object saveLaterLock = new Object();

    /**
     * Last observed value per document and normalized path: the document's
     * path listeners deliver only the new value, so the old value of a change
     * is read from this cache.
     */
    private final Map<SettingsDocument, Map<String, JsonNode>> lastValues = new LinkedHashMap<>();
    /**
     * Normalized paths that already have a component listener, per document.
     */
    private final Map<SettingsDocument, Set<String>> watchedPaths = new LinkedHashMap<>();

    private final SettingsChangedEventEmitter changedEventEmitter = new SettingsChangedEventEmitter();
    private final LegacyChangedEventEmitter legacyChangedEventEmitter = new LegacyChangedEventEmitter();
    private volatile boolean disposed;

    /**
     * Creates the component for the real JVM-wide {@link JIPipeService}, using
     * the standard user directory for the settings files.
     *
     * @param service the owning service
     */
    public JIPipeSettingsServiceComponent(JIPipeService service) {
        this(service, JIPipe.getJIPipeUserDir(false));
    }

    private JIPipeSettingsServiceComponent(JIPipeService service, Path userDir) {
        super(service);
        this.userDir = Objects.requireNonNull(userDir, "userDir");
        this.swingDebounce = !GraphicsEnvironment.isHeadless();
        if (swingDebounce) {
            this.swingSaveLaterTimer = new Timer((int) SAVE_LATER_DEBOUNCE_MILLIS, e -> save());
            this.swingSaveLaterTimer.setRepeats(false);
            this.executorSaveLater = null;
        } else {
            this.swingSaveLaterTimer = null;
            this.executorSaveLater = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jipipe-settings-save-later");
                t.setDaemon(true);
                return t;
            });
        }
    }

    /**
     * <b>Testing only.</b> Creates a component for unit tests: it is
     * constructed without a {@link JIPipeService} (passing {@code null}) and
     * with an injected user directory, so the full code path can be exercised
     * without initializing the service. Every service-dependent access is
     * null-guarded — {@link #getProgressInfo()} returns {@code null} instead
     * of throwing.
     *
     * @param userDir the directory the settings files are read from and written to
     * @return a testable component
     */
    public static JIPipeSettingsServiceComponent forTesting(Path userDir) {
        return new JIPipeSettingsServiceComponent(null, userDir);
    }

    /**
     * @return all three scopes, always; the scope set is fixed by the design
     * and does not depend on what has been registered so far
     */
    public Set<SettingsScope> availableScopes() {
        return Set.of(SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME);
    }

    /**
     * @param scope the scope
     * @return the file the scope's document is persisted to, or {@code null}
     * for {@link SettingsScope#PROJECT} (kept in memory; the project owns it)
     */
    public Path fileFor(SettingsScope scope) {
        Objects.requireNonNull(scope, "scope");
        switch (scope) {
            case APPLICATION:
                return userDir.resolve("settings.json");
            case RUNTIME:
                return userDir.resolve("runtime-settings.json");
            case PROJECT:
                return null;
            default:
                return null;
        }
    }

    /**
     * Returns the document for the given scope, creating (and, for
     * file-backed scopes, loading) it on first access.
     *
     * @param scope the scope
     * @return the live document for that scope
     */
    public synchronized SettingsDocument document(SettingsScope scope) {
        Objects.requireNonNull(scope, "scope");
        SettingsDocument doc = documents.get(scope);
        if (doc == null) {
            Path file = fileFor(scope);
            if (file == null) {
                doc = new SettingsDocument(scope);
            } else {
                SettingsFileStore store = new SettingsFileStore(file, scope, blockPath -> true);
                stores.put(scope, store);
                doc = store.load();
            }
            documents.put(scope, doc);
            attachPathListeners(doc);
        }
        return doc;
    }

    /**
     * Registers a setting key so it can be resolved by its block path and
     * sub-path and bound later. If the key's scope document already exists,
     * the key's block and sub-path are made observable as well, so changes
     * written before or after registration are re-emitted.
     *
     * @param key the key to register
     */
    public synchronized void registerKey(JIPipeSettingKey<?> key) {
        Objects.requireNonNull(key, "key");
        registeredKeys.put(keyLocation(key), key);
        SettingsDocument doc = documents.get(key.scope());
        if (doc != null) {
            ensureObserved(doc, key.blockPath(), null);
            if (key.subPath() != null && !key.subPath().isEmpty()) {
                ensureObserved(doc, key.blockPath(), key.subPath());
            }
        }
    }

    /**
     * @param blockPath the canonical block path
     * @param subPath the slash-separated sub-path
     * @return the registered key at that location, or {@code null} if none
     */
    public synchronized JIPipeSettingKey<?> keyByBlockAndSubPath(String blockPath, String subPath) {
        return registeredKeys.get(location(blockPath, subPath));
    }

    /**
     * @return the set of all registered keys
     */
    public synchronized Set<JIPipeSettingKey<?>> registeredKeys() {
        return new LinkedHashSet<>(registeredKeys.values());
    }

    /**
     * Registers a key and returns a live {@link SettingsBinding} on the
     * key's scope document.
     * <p>
     * The component tracks the binding and closes it in {@link #dispose()} if
     * the caller has not closed it first; {@link SettingsBinding#close()} is
     * idempotent, so early-closed bindings are unaffected.
     *
     * @param key the key to bind
     * @param <T> the value type
     * @return the live binding
     */
    public synchronized <T> SettingsBinding<T> bind(JIPipeSettingKey<T> key) {
        Objects.requireNonNull(key, "key");
        // Materialize the document first, then register: registerKey makes the
        // key's block and sub-path observable, which must happen on a
        // materialized document — otherwise the first write of a new setting
        // (block absent at bind time, the common case) would fire no change
        // event at all.
        SettingsDocument doc = document(key.scope());
        registerKey(key);
        SettingsBinding<T> binding = new SettingsBinding<>(doc, key);
        trackedBindings.add(binding);
        return binding;
    }

    /**
     * Registers a loop-safe bridge projecting the given application settings
     * sheet onto the {@code "sheet:<id>"} block of the APPLICATION document.
     * Safe to call before the document exists: the document is materialized
     * (lazily) on demand.
     *
     * @param sheet the sheet to bridge
     */
    public synchronized void attachSheet(JIPipeApplicationSettingsSheet sheet) {
        Objects.requireNonNull(sheet, "sheet");
        // Materializing the document is lazy and idempotent, so this is safe
        // to call before the document exists (e.g. at sheet registration
        // time): the bridge is created against the fresh, loaded document.
        SettingsSheetBridge bridge = attachBridge(document(SettingsScope.APPLICATION), sheet);
        attachedBridges.put(sheet, bridge);
    }

    /**
     * Creates, attaches, and aligns a bridge for one sheet: after
     * {@link SettingsSheetBridge#attach()} (which seeds an absent block from
     * the sheet's current values), the block's stored values are applied into
     * the sheet, so a pre-existing file's content reaches the sheet — matching
     * today's reload-after-registration sequence.
     */
    private SettingsSheetBridge attachBridge(SettingsDocument app, JIPipeApplicationSettingsSheet sheet) {
        SettingsSheetBridge bridge = new SettingsSheetBridge(app, sheet.getId(), sheet);
        bridge.attach();
        bridge.applyFromDocument();
        return bridge;
    }

    /**
     * Re-reads every file-backed document from disk and re-attaches all sheet
     * bridges (a bridge holds a document reference, so after the document is
     * replaced it must point at the fresh one). In-memory (PROJECT)
     * documents are left untouched.
     */
    public synchronized void reload() {
        for (SettingsScope scope : new LinkedHashSet<>(documents.keySet())) {
            SettingsFileStore store = stores.get(scope);
            if (store == null) {
                continue; // in-memory scope
            }
            Set<JIPipeApplicationSettingsSheet> bridgedSheets = null;
            if (scope == SettingsScope.APPLICATION) {
                // Detach all bridges: they hold a reference to the document
                // that is about to be replaced, and a bridge cannot be
                // re-attached to a different document.
                for (SettingsSheetBridge bridge : attachedBridges.values()) {
                    try {
                        bridge.detach();
                    } catch (RuntimeException e) {
                        logger.warn("Could not detach settings sheet bridge during reload", e);
                    }
                }
                // Remember which sheets were bridged, then clear the map so it
                // is rebuilt below against the fresh document.
                bridgedSheets = new LinkedHashSet<>(attachedBridges.keySet());
                attachedBridges.clear();
            }
            SettingsDocument old = documents.get(scope);
            SettingsDocument fresh = store.load();
            documents.put(scope, fresh);
            // The replaced document is no longer managed: drop its observer
            // state so the cache maps do not keep it (and its value tree)
            // alive after a reload.
            if (old != null) {
                lastValues.remove(old);
                watchedPaths.remove(old);
            }
            attachPathListeners(fresh);
            if (scope == SettingsScope.APPLICATION) {
                // Re-create every bridge on the fresh document and attach it.
                for (JIPipeApplicationSettingsSheet sheet : bridgedSheets) {
                    attachedBridges.put(sheet, attachBridge(fresh, sheet));
                }
            }
        }
        if (getProgressInfo() != null) {
            getProgressInfo().log("Reloaded settings documents from disk");
        }
    }

    /**
     * Flushes all attached sheet bridges into the document, then atomically
     * saves every file-backed document.
     */
    public synchronized void save() {
        if (disposed) {
            return;
        }
        for (SettingsSheetBridge bridge : attachedBridges.values()) {
            bridge.flushToDocument();
        }
        for (Map.Entry<SettingsScope, SettingsFileStore> entry : stores.entrySet()) {
            SettingsDocument doc = documents.get(entry.getKey());
            if (doc == null) {
                continue;
            }
            stampMeta(doc);
            try {
                entry.getValue().save(doc);
            } catch (RuntimeException e) {
                logger.error("Could not save settings document for scope {}", entry.getKey(), e);
                JIPipeService service = getService();
                if (service != null) {
                    service.getLogService().error("Could not save settings document for scope " + entry.getKey(), e);
                }
            }
        }
        if (getProgressInfo() != null) {
            getProgressInfo().log("Saved settings documents to disk");
        }
    }

    /**
     * Coalesces rapid changes into a single {@link #save()} after a 250 ms
     * quiet period: repeated calls within the window do not stack. Uses a
     * Swing timer when an EDT exists, a background executor otherwise, so
     * headless runs coalesce saves without initializing Swing.
     */
    public void saveLater() {
        if (disposed) {
            return;
        }
        if (swingSaveLaterTimer != null) {
            swingSaveLaterTimer.restart();
            return;
        }
        // Headless path: schedule (or reschedule) one save 250 ms out,
        // cancelling the previous task so the quiet period restarts on every
        // call — the same semantics as Swing's Timer.restart().
        synchronized (saveLaterLock) {
            ScheduledFuture<?> previous = pendingSaveTask;
            if (previous != null) {
                previous.cancel(false);
            }
            try {
                pendingSaveTask = executorSaveLater.schedule(() -> {
                    synchronized (saveLaterLock) {
                        pendingSaveTask = null;
                    }
                    save();
                }, SAVE_LATER_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                pendingSaveTask = null;
                logger.warn("Could not schedule a debounced settings save", e);
            }
        }
    }

    /**
     * @return the emitter of fine-grained {@link SettingsChangedEvent}s
     */
    public SettingsChangedEventEmitter getChangedEventEmitter() {
        return changedEventEmitter;
    }

    /**
     * @return the aggregate emitter that fires on <em>any</em> change, for
     * legacy subscribers that do not care which setting changed
     */
    public LegacyChangedEventEmitter getLegacyChangedEventEmitter() {
        return legacyChangedEventEmitter;
    }

    /**
     * The source of a settings change.
     */
    public enum ChangeOrigin {
        /** The user edited the value in the settings UI. */
        LOCAL_EDIT,
        /** A feature or node wrote the value programmatically in this process. */
        LOCAL_PROGRAMMATIC,
        /** The value was re-read from a file that changed on disk. */
        EXTERNAL_FILE,
        /** The value came from another process writing the file. */
        EXTERNAL_PROCESS,
        /** The value was carried over from a previous version's profile. */
        CARRY_OVER,
        /** The value was reset to its default. */
        RESET,
        /** The value was changed through the raw settings editor. */
        RAW_EDIT
    }

    /**
     * A single settings change: which scope and path, the old and new value,
     * and where the change originated.
     */
    public static final class SettingsChangedEvent extends AbstractJIPipeEvent {
        private final SettingsScope scope;
        private final String path;
        private final JsonNode oldValue;
        private final JsonNode newValue;
        private final ChangeOrigin origin;

        public SettingsChangedEvent(SettingsScope scope, String path, JsonNode oldValue, JsonNode newValue, ChangeOrigin origin) {
            super(JIPipeSettingsServiceComponent.class);
            this.scope = Objects.requireNonNull(scope, "scope");
            this.path = Objects.requireNonNull(path, "path");
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.origin = Objects.requireNonNull(origin, "origin");
        }

        public SettingsScope getScope() {
            return scope;
        }

        /**
         * @return the normalized {@code "<blockPath>/<subPath>"} that changed
         * (a bare block path for whole-block changes)
         */
        public String getPath() {
            return path;
        }

        public JsonNode getOldValue() {
            return oldValue;
        }

        public JsonNode getNewValue() {
            return newValue;
        }

        public ChangeOrigin getOrigin() {
            return origin;
        }
    }

    /**
     * Listener for {@link SettingsChangedEvent}.
     */
    public interface SettingsChangedEventListener {
        void onSettingsChanged(SettingsChangedEvent event);
    }

    /**
     * Emits {@link SettingsChangedEvent}s for every observed change.
     */
    public static final class SettingsChangedEventEmitter extends JIPipeEventEmitter<SettingsChangedEvent, SettingsChangedEventListener> {
        @Override
        protected void call(SettingsChangedEventListener listener, SettingsChangedEvent event) {
            listener.onSettingsChanged(event);
        }
    }

    /**
     * A no-argument aggregate change event for legacy subscribers.
     */
    public static final class LegacyChangedEvent extends AbstractJIPipeEvent {
        public LegacyChangedEvent() {
            super(JIPipeSettingsServiceComponent.class);
        }
    }

    /**
     * Listener for the aggregate {@link LegacyChangedEvent}.
     */
    public interface LegacyChangedEventListener {
        void onSettingsChanged(LegacyChangedEvent event);
    }

    /**
     * Emits {@link LegacyChangedEvent} on any change, so pre-existing legacy
     * subscribers keep working until they are migrated (Task 13).
     */
    public static final class LegacyChangedEventEmitter extends JIPipeEventEmitter<LegacyChangedEvent, LegacyChangedEventListener> {
        @Override
        protected void call(LegacyChangedEventListener listener, LegacyChangedEvent event) {
            listener.onSettingsChanged(event);
        }
    }

    @Override
    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (SettingsSheetBridge bridge : attachedBridges.values()) {
            try {
                bridge.detach();
            } catch (RuntimeException e) {
                logger.warn("Could not detach settings sheet bridge", e);
            }
        }
        attachedBridges.clear();
        for (SettingsBinding<?> binding : trackedBindings) {
            try {
                binding.close();
            } catch (RuntimeException e) {
                logger.warn("Could not close tracked settings binding", e);
            }
        }
        trackedBindings.clear();
        if (swingSaveLaterTimer != null) {
            swingSaveLaterTimer.stop();
        }
        if (executorSaveLater != null) {
            pendingSaveTask = null;
            executorSaveLater.shutdownNow();
        }
        documents.clear();
        stores.clear();
        lastValues.clear();
        watchedPaths.clear();
        changedEventEmitter.dispose();
        legacyChangedEventEmitter.dispose();
    }

    /**
     * Null-safe access to the service's progress info: the testing factory
     * constructs this component without a service, so this must not NPE.
     */
    @Override
    public JIPipeProgressInfo getProgressInfo() {
        JIPipeService service = getService();
        return service == null ? null : service.getProgressInfo();
    }

    private static String keyLocation(JIPipeSettingKey<?> key) {
        return location(key.blockPath(), key.subPath());
    }

    /**
     * Normalizes a block path plus sub-path into the listener key form
     * {@code "<blockPath>/<subPath>"}, dropping empty sub-path segments, so
     * {@code ("a:b", "x//y")} yields {@code "a:b/x/y"} and an empty sub-path
     * yields the bare block path.
     * <p>
     * TODO(#1345): this duplicates the normalization performed by
     * {@code SettingsDocument.listenerPath} (package-private in
     * {@code org.hkijena.jipipe.api.settings}); reuse it there if/when its
     * visibility is widened, instead of keeping two copies.
     */
    private static String location(String blockPath, String subPath) {
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

    /**
     * Attaches component listeners to every block and sub-path of the
     * document, so subsequent mutations of those locations are observed and
     * re-emitted.
     */
    private void attachPathListeners(SettingsDocument doc) {
        for (String blockPath : doc.blockPaths()) {
            ensureObserved(doc, blockPath, null);
            for (String subPath : doc.subPaths(blockPath)) {
                ensureObserved(doc, blockPath, subPath);
            }
        }
    }

    /**
     * Ensures a listener is attached for one block location and records its
     * current value as the baseline for the "old" value of the next change.
     * Idempotent per document+normalized path.
     *
     * @param doc the document to observe
     * @param blockPath the canonical block path
     * @param subPath the sub-path within the block, or {@code null} for the whole block
     */
    private void ensureObserved(SettingsDocument doc, String blockPath, String subPath) {
        String path = location(blockPath, subPath);
        if (path.startsWith("$")) {
            return; // reserved fields are not settings
        }
        Set<String> watched = watchedPaths.computeIfAbsent(doc, d -> new LinkedHashSet<>());
        if (!watched.add(path)) {
            return;
        }
        Map<String, JsonNode> valueCache = lastValues.computeIfAbsent(doc, d -> new LinkedHashMap<>());
        valueCache.put(path, doc.get(blockPath, subPath));
        doc.addPathListener(path, newValue -> onPathChanged(doc, path, newValue));
    }

    /**
     * Called by a document path listener when the value at a watched path
     * changes: reads the old value from the cache, emits the fine-grained and
     * legacy events, and updates the baseline.
     */
    private void onPathChanged(SettingsDocument doc, String path, JsonNode newValue) {
        Map<String, JsonNode> valueCache = lastValues.get(doc);
        if (valueCache == null) {
            return;
        }
        JsonNode oldValue = valueCache.get(path);
        if (!Objects.equals(oldValue, newValue)) {
            valueCache.put(path, newValue);
            changedEventEmitter.emit(new SettingsChangedEvent(
                    doc.scope(), path, oldValue, newValue, ChangeOrigin.LOCAL_PROGRAMMATIC));
            legacyChangedEventEmitter.emit(new LegacyChangedEvent());
        }
    }

    /**
     * Writes the {@code $meta} bookkeeping fields (written-by-version,
     * written-at, writer-id) into the document just before it is saved, so
     * the file on disk records who and when wrote it.
     */
    private void stampMeta(SettingsDocument doc) {
        doc.meta().put("written-by-version", VersionUtils.getJIPipeVersion());
        doc.meta().put("written-at", META_TIMESTAMP.format(Instant.now()));
        doc.meta().put("writer-id", doc.writerId());
    }
}
