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
import org.hkijena.jipipe.api.settings.SettingsDocumentIO;
import org.hkijena.jipipe.api.settings.SettingsDocumentMerger;
import org.hkijena.jipipe.api.settings.SettingsFileStore;
import org.hkijena.jipipe.api.settings.SettingsFileStore.FileFingerprint;
import org.hkijena.jipipe.api.settings.SettingsFileWatching;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.api.settings.SettingsSheetBridge;
import org.hkijena.jipipe.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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
 * <li><b>Multi-instance sync.</b> Each file-backed scope is watched by a
 * {@link SettingsFileWatching} (started lazily with the document, inert in
 * headless JVMs). When another JIPipe process writes the file, the
 * watcher's callback — invoked under the watcher's instance monitor —
 * does nothing but hand the file off to a single-thread executor, which
 * loads the file into a temporary document and runs the per-block
 * {@link SettingsDocumentMerger#merge} against the live document under
 * this component's monitor. Adopted and conflicted blocks are re-emitted
 * as {@link SettingsChangedEvent}s with
 * {@link ChangeOrigin#EXTERNAL_FILE}; kept blocks are persisted again
 * (via {@link #saveLater()}) so the file converges, as is a conflicted
 * merge. {@link #hasForeignConflict()} reports the last collision for the
 * UI banner. Because a save can be interleaved with a foreign write in a
 * way the watcher's fingerprint suppression absorbs (its documented false
 * negative), {@link #save()} verifies each write by reading the file back
 * and comparing it against the serialized content; a mismatch is fed
 * through the same merge path instead of being silently accepted.</li>
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
     * One watcher per file-backed scope, started lazily when the scope's
     * document is first materialized (see {@link #document(SettingsScope)}).
     * Headless runs hold no live watch: {@link #startWatchingFor} does
     * nothing in that case, and the slot stays empty.
     */
    private final Map<SettingsScope, SettingsFileWatching> watchers = new LinkedHashMap<>();
    /**
     * Single-thread executor that runs foreign-change merges off the
     * watcher's worker thread: the watcher invokes its callback while
     * holding its instance monitor, so the callback must be a cheap
     * hand-off, and the file load / merge / event work happens here.
     * Daemon so it cannot keep the JVM alive.
     */
    private final ExecutorService foreignChangeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jipipe-settings-foreign-merge");
        t.setDaemon(true);
        return t;
    });
    /**
     * Block paths changed locally (through a binding, the document's
     * listeners, or a sheet bridge) since the last sync point (a
     * successful save, a reload, or a completed merge), per document.
     * Drives the per-block merge decisions (see
     * {@link SettingsDocumentMerger}) and is cleared after each of those
     * sync points.
     * <p>
     * Concurrent: the tracking listener (see {@link
     * #registerLocalChangeListener}) fires on whatever thread mutates the
     * document — typically the EDT for a {@code binding.set()} — which does
     * <em>not</em> hold the component's monitor, while the merge and save
     * paths read the set under it. The outer map and each inner set are
     * therefore concurrent; {@link #recordSyncPoint} atomically replaces
     * the whole inner set at a sync point.
     */
    private final Map<SettingsDocument, Set<String>> locallyChangedBlocks = new ConcurrentHashMap<>();
    /**
     * The value of each block at the last sync point (the document's
     * content when it was loaded from disk, after a save, or after a
     * merge), per document. The baseline that distinguishes "changed only
     * locally" from a true same-block collision in
     * {@link SettingsDocumentMerger#merge}.
     */
    private final Map<SettingsDocument, Map<String, JsonNode>> lastSyncedBaselines = new LinkedHashMap<>();
    /**
     * Last observed value per document and normalized path: the document's
     * path listeners deliver only the new value, so the old value of a change
     * is read from this cache.
     * <p>
     * Concurrent (outer and inner): {@link #onPathChanged} runs on whatever
     * thread mutates the document — the EDT for a local {@code binding.set()}
     * and the foreign-merge executor for an adopted/conflicted block — so the
     * cache is written from multiple threads, outside the component's
     * monitor.
     */
    private final Map<SettingsDocument, Map<String, JsonNode>> lastValues = new ConcurrentHashMap<>();
    /**
     * Normalized paths that already have a component listener, per document.
     */
    private final Map<SettingsDocument, Set<String>> watchedPaths = new LinkedHashMap<>();
    /**
     * Blocks that already have a local-change tracking listener (see
     * {@link #observeBlockWrites}), per document. Prevents duplicate
     * listeners on re-bind.
     */
    private final Map<SettingsDocument, Set<String>> localChangeTrackedBlocks = new LinkedHashMap<>();

    private final SettingsChangedEventEmitter changedEventEmitter = new SettingsChangedEventEmitter();
    private final LegacyChangedEventEmitter legacyChangedEventEmitter = new LegacyChangedEventEmitter();
    /**
     * Set when a merge with foreign changes found a true same-block
     * collision (the foreign value won over a local edit), cleared when a
     * later merge finds no collision. Read by the UI to show the "Settings
     * were updated by another JIPipe instance" banner.
     */
    private volatile boolean foreignConflict;
    private volatile boolean disposed;

    /**
     * The {@link ChangeOrigin} to attribute to document path changes while a
     * foreign-change merge is applying adopted/conflicted blocks, so those
     * mutations are re-emitted as {@link ChangeOrigin#EXTERNAL_FILE}
     * instead of {@link ChangeOrigin#LOCAL_PROGRAMMATIC}. Set for the
     * duration of a {@link #handleForeignChange} on its executor thread and
     * cleared afterwards; a plain (non-merge) mutation sees the default
     * {@code LOCAL_PROGRAMMATIC}.
     * <p>
     * {@code EXTERNAL_FILE} is used for every foreign change detected via
     * the file (whether by the watcher or by the post-save read-back).
     * {@code EXTERNAL_PROCESS} is reserved for a future explicit IPC
     * channel and is intentionally not produced by this task.
     */
    private final ThreadLocal<ChangeOrigin> foreignMergeOrigin = ThreadLocal.withInitial(() -> ChangeOrigin.LOCAL_PROGRAMMATIC);

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
     * <b>Testing only.</b> Runs the post-save read-back verification
     * ({@link #verifyOwnWrite}) for one file-backed scope synchronously, so
     * a test can deterministically simulate a foreign write that
     * interleaved with a save. Production code runs the same logic from
     * {@link #save()} right after each successful write; this seam exists
     * only because the interleaving window (between the atomic write and
     * the read-back) is too narrow to hit reliably in a race. See
     * {@code JIPipeSettingsServiceComponentSyncTest.interleavedForeignWriteIsNotSilentlyLost}.
     *
     * @param scope the file-backed scope to verify
     */
    public synchronized void verifyOwnWriteForTesting(SettingsScope scope) {
        SettingsFileStore store = stores.get(scope);
        SettingsDocument doc = documents.get(scope);
        if (store == null || doc == null) {
            return;
        }
        verifyOwnWrite(store, doc);
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
            recordSyncPoint(doc);
            startWatchingFor(scope, doc);
        }
        return doc;
    }

    /**
     * Records the just-loaded document as the current sync point: its
     * content is the last value both sides agreed on, so no block counts
     * as locally changed yet and the merge baseline starts from the disk
     * content.
     */
    private void recordSyncPoint(SettingsDocument doc) {
        locallyChangedBlocks.put(doc, ConcurrentHashMap.newKeySet());
        lastSyncedBaselines.put(doc, SettingsDocumentMerger.baselineOf(doc));
    }

    /**
     * Starts the foreign-change watch for a file-backed scope: the watcher
     * fires on disk changes made by other processes; its callback only
     * hands the fingerprint off to {@link #foreignChangeExecutor}, because
     * it runs under the watcher's instance monitor and must stay cheap
     * (see {@code SettingsFileWatcher}). The current file state is
     * recorded as this process's own fingerprint so the pre-existing
     * content is never reported as a foreign change.
     * <p>
     * In a headless JVM no watch is started at all: CLI runs do not spawn
     * watcher threads (the same rule as the debounce executor), and the
     * post-save read-back verification in {@link #verifyOwnWrite} remains
     * the safety net there.
     */
    private void startWatchingFor(SettingsScope scope, SettingsDocument doc) {
        if (disposed || GraphicsEnvironment.isHeadless()) {
            return;
        }
        Path file = fileFor(scope);
        SettingsFileStore store = stores.get(scope);
        if (file == null || store == null || watchers.get(scope) != null) {
            return;
        }
        try {
            watchers.put(scope, SettingsFileWatching.start(file, fp ->
                    foreignChangeExecutor.submit(() -> handleForeignChange(scope, file, store))));
        } catch (RuntimeException e) {
            // Watching is an optimization; the post-save read-back
            // verification still covers foreign interleavings.
            logger.warn("Could not start the settings file watcher for {}", file, e);
        }
    }

    /**
     * Handles a foreign change of the given settings file, off the
     * watcher's thread: reloads the file into a temporary document, runs
     * the per-block merge against the live document, records the new sync
     * point, and persists the local document when the merge outcome
     * requires it.
     * <p>
     * All access to the live document happens under this component's
     * monitor (this method is synchronized on it, as are
     * {@link #document(SettingsScope)}, {@link #save()} and
     * {@link #reload()}), so a merge can never observe a document that a
     * concurrent save or reload is in the middle of replacing.
     * <p>
     * The blocks and sub-paths the merge is about to touch are observed
     * <em>before</em> the merge runs, with their pre-merge values as the
     * baseline for the "old" value of the emitted events; the merge's
     * block replacements then re-emit them as
     * {@link ChangeOrigin#EXTERNAL_FILE} (via the
     * {@link #foreignMergeOrigin} thread-local). This is what makes
     * "fire per-path events for adopted/conflicted blocks" true for
     * sub-paths that no binding or bridge had observed before.
     *
     * @param scope the scope of the changed file
     * @param file the settings file that changed
     * @param store the store that owns the file
     */
    private synchronized void handleForeignChange(SettingsScope scope, Path file, SettingsFileStore store) {
        if (disposed) {
            return;
        }
        SettingsDocument doc = documents.get(scope);
        if (doc == null) {
            return; // the document was not (yet) materialized; nothing to merge into
        }
        // Fingerprint the foreign state before loading it: after the merge
        // this exact state is handled, so the watcher must not deliver it
        // again as a new change (a second delivery would re-merge the same
        // content against an already-updated baseline and could adopt the
        // local blocks as foreign deletions — data loss).
        FileFingerprint foreignFingerprint = store.fingerprint();
        SettingsDocument foreign = store.load();
        Set<String> affected = new LinkedHashSet<>(doc.blockPaths());
        affected.addAll(foreign.blockPaths());
        // Observe the blocks the merge may touch, and the sub-paths they
        // currently hold, before any of them is mutated: this both registers
        // listeners for paths that were unobserved (a whole-block adoption
        // of a previously empty block) and fixes the old value the event
        // will report.
        for (String blockPath : affected) {
            ensureObserved(doc, blockPath, null);
            for (String subPath : doc.subPaths(blockPath)) {
                ensureObserved(doc, blockPath, subPath);
            }
        }
        foreignMergeOrigin.set(ChangeOrigin.EXTERNAL_FILE);
        try {
            Set<String> changed = locallyChangedBlocks.getOrDefault(doc, Set.of());
            Map<String, JsonNode> baseline = lastSyncedBaselines.getOrDefault(doc, Map.of());
            SettingsDocumentMerger.MergeResult result =
                    SettingsDocumentMerger.merge(doc, foreign, changed, baseline);
            recordSyncPoint(doc);
            if (result.hasConflicts()) {
                foreignConflict = true;
                logger.info("Settings for scope {} were changed concurrently by another JIPipe instance; "
                        + "the on-disk values won for: {}", scope, result.conflictedBlocks());
            } else {
                foreignConflict = false;
            }
            if (result.requiresSave()) {
                // Kept blocks are newer than the foreign file (we re-save so
                // the file converges), and a conflicted merge must write the
                // winning foreign values back so both processes end up with
                // the same content. saveLater() coalesces with pending
                // debounced saves and self-ignores its own write.
                saveLater();
            }
        } finally {
            foreignMergeOrigin.remove();
        }
        if (foreignFingerprint != null) {
            SettingsFileWatching watch = watchers.get(scope);
            if (watch != null) {
                watch.setOwnFingerprint(foreignFingerprint);
            }
        }
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
        observeBlockWrites(doc, key.blockPath(), key.subPath());
        return binding;
    }

    /**
     * Marks the given block as locally changed whenever its content is
     * mutated at the given sub-path <em>or</em> as a whole block, so the
     * per-block merge ({@link SettingsDocumentMerger}) knows which blocks
     * this process edited since the last sync point. This is the tracking
     * hook required for every {@code set}/{@code remove} that reaches the
     * document through a binding or a sheet bridge.
     * <p>
     * Both locations are registered because the document's notification is
     * location-specific: a binding writes a sub-path
     * ({@code document.set(blockPath, subPath, …)} fires only that sub-path's
     * listeners) while a sheet bridge replaces the whole block
     * ({@code document.setBlock(blockPath, …)} fires only the block-level
     * listener). Tracking both covers both writer kinds.
     * <p>
     * The hook is attached for a location the first time a binding or a
     * bridge observes it. A raw {@code document().set(...)} that no binding
     * or bridge ever touched is not tracked; the merge's conservative rule
     * then keeps such a block (it is never destroyed), so the gap degrades
     * at worst to a missing conflict banner, not to data loss.
     *
     * @param doc the document
     * @param blockPath the canonical block path
     * @param subPath the slash-separated sub-path a writer addresses, or
     *                {@code null} for whole-block writers
     */
    private void observeBlockWrites(SettingsDocument doc, String blockPath, String subPath) {
        registerLocalChangeListener(doc, blockPath, subPath);
        registerLocalChangeListener(doc, blockPath, null);
    }

    /**
     * Registers one local-change listener for a single normalized location
     * (idempotent per document+location). The listener adds the block path
     * to the document's locally-changed set, looking the set up at fire
     * time (rather than capturing it) because a sync point replaces the set
     * in the map and only writes after the new sync point count.
     */
    private void registerLocalChangeListener(SettingsDocument doc, String blockPath, String subPath) {
        String path = location(blockPath, subPath);
        if (path.startsWith("$")) {
            return; // reserved fields are not settings blocks
        }
        if (!localChangeTrackedBlocks.computeIfAbsent(doc, d -> new LinkedHashSet<>()).add(path)) {
            return;
        }
        doc.addPathListener(path, newValue -> {
            Set<String> changed = locallyChangedBlocks.get(doc);
            if (changed != null) {
                changed.add(blockPath);
            }
        });
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
        observeBlockWrites(app, "sheet:" + sheet.getId(), null);
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
            // and merge-bookkeeping state so the cache maps do not keep it
            // (and its value tree) alive after a reload. The fresh document
            // becomes the new sync point: the disk content is what both
            // sides agree on again.
            if (old != null) {
                lastValues.remove(old);
                watchedPaths.remove(old);
                locallyChangedBlocks.remove(old);
                lastSyncedBaselines.remove(old);
                localChangeTrackedBlocks.remove(old);
            }
            attachPathListeners(fresh);
            recordSyncPoint(fresh);
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
     * <p>
     * Each successful save is immediately verified by
     * {@link #verifyOwnWrite(SettingsFileStore, SettingsDocument)}: the
     * file is read back and compared against the content this process just
     * wrote, closing the watcher's documented own-fingerprint suppression
     * window (a foreign write interleaved between this process's write and
     * its fingerprint read would otherwise be silently absorbed, leaving
     * the in-memory state diverged from disk with no event to reconcile
     * it — see {@code SettingsFileWatcher}). A verification mismatch feeds
     * the foreign content through the same merge path a watcher callback
     * would use, instead of silently accepting the observed fingerprint.
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
            SettingsFileStore store = entry.getValue();
            stampMeta(doc);
            try {
                store.save(doc);
            } catch (RuntimeException e) {
                logger.error("Could not save settings document for scope {}", entry.getKey(), e);
                JIPipeService service = getService();
                if (service != null) {
                    service.getLogService().error("Could not save settings document for scope " + entry.getKey(), e);
                }
                continue; // the write failed: no successful sync point
            }
            recordSyncPoint(doc); // a successful save is a sync point
            verifyOwnWrite(store, doc);
        }
        if (getProgressInfo() != null) {
            getProgressInfo().log("Saved settings documents to disk");
        }
    }

    /**
     * Self-healing check after a successful save: reads the file back and
     * compares it against the content this process actually wrote.
     * <p>
     * <b>What it closes:</b> the watcher suppresses changes whose
     * fingerprint equals the one the owner recorded after its save, and
     * the owner records that fingerprint from the file as it is <em>at the
     * moment of the read</em>. If a foreign process writes the file after
     * this process's atomic move but before that read — the window the
     * watcher documents as its realistic suppression case — the owner
     * records the <b>foreign</b> state as its own fingerprint and the
     * watcher never reports it: the foreign change is silently lost. This
     * check detects exactly that situation, because the read-back content
     * differs from the serialized bytes this process intended to write
     * (the store's temp-file-then-replace write makes the intended bytes
     * deterministic for the document at save time).
     * <p>
     * On a mismatch the observed file content is fed through
     * {@link #handleForeignChange} — the same merge path a watcher
     * callback uses — so the foreign blocks are adopted/conflicted/kept
     * with events, and the re-save scheduled by the merge converges the
     * file. It is called from {@link #save()} while already holding the
     * component's monitor; the (reentrant) {@code synchronized}
     * {@link #handleForeignChange} call re-enters the same lock, so no
     * deadlock is possible.
     * <p>
     * <b>What it does not cover:</b> a byte-identical foreign write (a
     * foreign process that writes exactly the same content we did is
     * indistinguishable from our own write by any content comparison; such
     * a write is a no-op for the values anyway). A foreign write landing
     * <em>during</em> the verification read is covered: the fingerprint is
     * read first and the decision is made on the content read second, so a
     * write in between surfaces as a content mismatch and is merged.
     *
     * @param store the store the document was just saved through
     * @param doc the document that was just saved; its scope identifies the
     *            watcher to record the own fingerprint on
     */
    private void verifyOwnWrite(SettingsFileStore store, SettingsDocument doc) {
        Path file = store.file();
        try {
            if (!Files.isRegularFile(file)) {
                return; // deleted concurrently; the watcher treats an absent file as no event
            }
            // Read the fingerprint first, then the content, and decide on
            // the content (the strong identity, taken after the
            // fingerprint). If a foreign write lands between the two
            // reads, the content no longer equals ours, so the mismatch
            // branch routes it through the merge instead of recording the
            // foreign fingerprint as our own (which the watcher would
            // then suppress — the loss this check exists to prevent).
            FileFingerprint own = store.fingerprint();
            String onDisk = Files.readString(file, StandardCharsets.UTF_8);
            if (onDisk.equals(SettingsDocumentIO.toPrettyString(doc))) {
                // Our write survived: record it as our own, so the watcher
                // suppresses it (normal self-write).
                if (own != null) {
                    SettingsFileWatching watch = watchers.get(doc.scope());
                    if (watch != null) {
                        watch.setOwnFingerprint(own);
                    }
                }
                return;
            }
            // A foreign writer interleaved between our save and the
            // fingerprint read: the on-disk content is not ours. Treat it
            // as a foreign change instead of silently recording the
            // foreign fingerprint as our own.
            logger.info("Settings file {} was modified by another process while saving; merging it", file);
            handleForeignChange(doc.scope(), file, store);
        } catch (IOException e) {
            // The verification is best-effort: on a read failure the next
            // watcher delivery or the next save reconciles the file.
            logger.warn("Could not verify own settings write for {}", file, e);
        } catch (RuntimeException e) {
            // The verification is best-effort: on failure the next watcher
            // delivery or the next save reconciles the file.
            logger.warn("Could not verify own settings write for {}", file, e);
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
     * @return whether the most recent merge with a foreign change found a
     * true same-block collision, i.e. a local edit was discarded in favor
     * of the value another JIPipe instance wrote. The UI shows a
     * "Settings were updated by another JIPipe instance" banner while this
     * is {@code true}; it clears when a later merge finds no collision.
     */
    public boolean hasForeignConflict() {
        return foreignConflict;
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
        for (SettingsFileWatching watch : watchers.values()) {
            try {
                watch.close();
            } catch (RuntimeException e) {
                logger.warn("Could not close settings file watcher", e);
            }
        }
        watchers.clear();
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
        foreignChangeExecutor.shutdownNow();
        documents.clear();
        stores.clear();
        lastValues.clear();
        watchedPaths.clear();
        locallyChangedBlocks.clear();
        lastSyncedBaselines.clear();
        localChangeTrackedBlocks.clear();
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
        Map<String, JsonNode> valueCache = lastValues.computeIfAbsent(doc, d -> new ConcurrentHashMap<>());
        valueCache.put(path, doc.get(blockPath, subPath));
        doc.addPathListener(path, newValue -> onPathChanged(doc, path, newValue));
    }

    /**
     * Called by a document path listener when the value at a watched path
     * changes: reads the old value from the cache, emits the fine-grained and
     * legacy events, and updates the baseline. The change origin is taken
     * from {@link #foreignMergeOrigin}, so mutations applied by a foreign
     * merge are re-emitted as {@link ChangeOrigin#EXTERNAL_FILE} while all
     * other (local) mutations keep the default
     * {@link ChangeOrigin#LOCAL_PROGRAMMATIC}.
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
                    doc.scope(), path, oldValue, newValue, foreignMergeOrigin.get()));
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
