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
import org.hkijena.jipipe.api.settings.SettingsBlockPath;
import org.hkijena.jipipe.api.settings.SettingsCarryOver;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsDocumentIO;
import org.hkijena.jipipe.api.settings.SettingsDocumentMerger;
import org.hkijena.jipipe.api.settings.SettingsFileStore;
import org.hkijena.jipipe.api.settings.SettingsFileStore.FileFingerprint;
import org.hkijena.jipipe.api.settings.SettingsFileWatching;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.api.settings.SettingsSheetBridge;
import org.hkijena.jipipe.plugins.settings.SettingsScopes;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
import java.util.function.Predicate;

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
    /**
     * Backup marker and timestamp format for a corrupt foreign settings file,
     * identical to {@code SettingsFileStore}'s recovery convention
     * ({@code <name>.bad-<timestamp>}, filename-safe ISO basic format).
     */
    private static final String BAD_BACKUP_MARKER = ".bad-";
    private static final DateTimeFormatter BAD_BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

    /**
     * The profile directory that holds the settings files, when the component
     * was constructed with one explicitly ({@link #forTesting(Path)}). Used
     * as-is; the component never resolves the JIPipe user dir in that case.
     */
    private final Path userDirOverride;
    /**
     * The profile directory of the production component, resolved <em>lazily</em>
     * by {@link #userDir()} on first use and cached here.
     * <p>
     * Resolving it eagerly in the constructor would cache the real profile
     * directory: the service (and with it this component) is constructed before
     * the override user-dir base is installed on
     * {@link JIPipeServiceInitializationSettings}, so
     * {@link JIPipe#getJIPipeUserDir(boolean)} would return the developer's
     * profile while the startup migration (which resolves the directory at
     * initialization time, when the override is set) runs against the override
     * profile — the two would read and write different files. Guarded by the
     * component monitor.
     */
    private Path resolvedUserDir;
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
    /**
     * The ids of the sheets that have been attached through
     * {@link #attachSheet}, i.e. the {@code sheet:} blocks this version knows.
     * Only grows (a reload re-attaches the same sheets), and is consulted by
     * the cross-version carry-over (see {@link #isRegisteredBlock}). Guarded
     * by the component monitor like {@link #attachedBridges}.
     */
    private final Set<String> attachedBridgedSheetIds = new LinkedHashSet<>();
    /**
     * The block paths of the setting keys registered through
     * {@link #registerKey}, i.e. the {@code registry:}/{@code state:} blocks
     * this version knows. Consulted by the cross-version carry-over (see
     * {@link #isRegisteredBlock}); guarded by the component monitor.
     */
    private final Set<String> registeredKeyBlockPaths = new LinkedHashSet<>();
    /**
     * The settings blocks recovered by {@link #runCarryOver()} in this
     * session, mapped to the sibling version they came from, so the UI can
     * report "settings recovered from version X".
     */
    private final Map<String, String> recoveredBlocksByVersion = new LinkedHashMap<>();

    private final Timer swingSaveLaterTimer;
    /**
     * Whether the Swing debounce timer has a save pending (set by
     * {@link #saveLater()}, cleared when the timer fires). Tracked explicitly
     * so {@link #saveNowForTesting()} can report/run a pending save without
     * depending on a live Swing timer queue; {@link Timer#isRunning()} is not
     * reliable outside a running EDT.
     */
    private volatile boolean pendingSwingSave;
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
     * <p>
     * Monitor discipline: a plain {@link LinkedHashMap}, only read and
     * written by methods that run under the component monitor —
     * {@code document()}, {@code startWatchingFor()},
     * {@code handleForeignChange()} (the executor thread),
     * {@code recordOwnFingerprint()} (from the executor or from
     * {@code save()}) and {@code dispose()}. Never touch it from a document
     * path listener (those run on the mutating thread).
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
     * <p>
     * Monitor discipline: like {@link #watchers}, only touched by
     * component-monitor methods ({@code ensureObserved}, {@code reload},
     * {@code dispose}); the listener that is registered from here runs
     * elsewhere but only reads its own captured {@code doc}/{@code path}.
     */
    private final Map<SettingsDocument, Set<String>> watchedPaths = new LinkedHashMap<>();
    /**
     * Blocks that already have a local-change tracking listener (see
     * {@link #observeBlockWrites}), per document. Prevents duplicate
     * listeners on re-bind.
     * <p>
     * Monitor discipline: only touched under the component monitor — by
     * {@code bind()}, {@code attachSheet()} (via {@code attachBridge}),
     * the merge paths and {@code reload}/{@code dispose}.
     */
    private final Map<SettingsDocument, Set<String>> localChangeTrackedBlocks = new LinkedHashMap<>();
    /**
     * The process-wide write guard consulted by {@link #canWrite}: {@code null}
     * means "permit every write". A plain predicate rather than a policy
     * implementation, because the future #1102 runtime host owns the policy
     * (see {@link #setWriteGuard}). Volatile: read from whatever thread writes
     * a setting (typically the EDT), replaced from the runtime host's thread.
     */
    private volatile Predicate<JIPipeSettingKey<?>> writeGuard;

    private final SettingsChangedEventEmitter changedEventEmitter = new SettingsChangedEventEmitter();
    private final LegacyChangedEventEmitter legacyChangedEventEmitter = new LegacyChangedEventEmitter();
    /**
     * The last on-disk state each file-backed scope was known to be in from
     * this process's point of view: the fingerprint this process wrote, or
     * the fingerprint of a foreign state it merged. The pre-save
     * read-merge-write check ({@link #mergeForeignBeforeSave}) compares the
     * file's current fingerprint against it; any difference means another
     * writer changed the file since, and the save must merge before it
     * replaces. Recorded at a sync point (see
     * {@link #recordOwnFingerprint}), which also seeds it from the file at
     * first load. Guarded by the component monitor like every other field
     * here.
     */
    private final Map<SettingsScope, FileFingerprint> lastKnownFileStates = new LinkedHashMap<>();
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
     * Documents currently being written quiescently on this thread (see
     * {@link #beginQuiescentWrite}): document → nesting depth. Mutations of a
     * listed document emit no change event and do not count as local changes
     * for the duration. Thread-local, because only the writing thread must see
     * it; the write itself goes through the document's own lock.
     */
    private final ThreadLocal<Map<SettingsDocument, Integer>> quiescentWrites =
            ThreadLocal.withInitial(java.util.HashMap::new);
    /**
     * The thread that performed the last foreign merge, for
     * {@link #getLastMergeThreadNameForTesting()}.
     */
    private volatile String lastMergeThreadName;
    /**
     * <b>Testing only.</b> A hook invoked once inside the first merge pass,
     * right before its first affected block is resolved, so a test can
     * deterministically simulate a local edit landing <em>during</em> the
     * merge (after the locally-changed set snapshot, before the merge writes
     * the block — the window the second pass closes). {@code null} in
     * production.
     */
    private volatile Runnable midMergeHookForTesting;
    /**
     * While a merge is applying foreign values (between the snapshot of the
     * locally-changed set and the end of the second pass), local edits to a
     * block that the merge is about to overwrite are captured here by
     * block path, so the second pass can restore them. {@code null} outside
     * a merge. Written by whatever thread performs the local edit (the EDT),
     * read and cleared by the merging thread; hence concurrent.
     */
    private final Map<String, JsonNode> pendingLocalEdits = new ConcurrentHashMap<>();
    /**
     * Whether a merge is currently applying foreign values (see
     * {@link #pendingLocalEdits}). Set while the merge runs, so the
     * local-change listener only captures edits that can race the merge.
     */
    private volatile boolean mergeInProgress;

    /**
     * Creates the component for the real JVM-wide {@link JIPipeService}, using
     * the standard user directory for the settings files.
     *
     * <b>The profile directory is resolved lazily</b> (see {@link #userDir()}):
     * the override user-dir base
     * ({@link JIPipeServiceInitializationSettings#setOverrideUserDirBase(Path)},
     * the CLI's {@code --override-profile}) is installed on the service
     * <em>after</em> the service — and therefore this component — has been
     * constructed. Resolving the directory here would cache the real profile
     * directory and silently ignore the override, so the component would load
     * and save a different profile than the one the startup migration
     * prepared.
     *
     * @param service the owning service
     */
    public JIPipeSettingsServiceComponent(JIPipeService service) {
        this(service, null);
    }

    private JIPipeSettingsServiceComponent(JIPipeService service, Path userDir) {
        super(service);
        // Non-null only for the forTesting(Path) component, which pins the dir.
        this.userDirOverride = userDir == null ? null : Objects.requireNonNull(userDir, "userDir");
        this.swingDebounce = !GraphicsEnvironment.isHeadless();
        if (swingDebounce) {
            this.swingSaveLaterTimer = new Timer((int) SAVE_LATER_DEBOUNCE_MILLIS, e -> {
                pendingSwingSave = false;
                save();
            });
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
     * <b>Testing only — not part of the production API.</b> Runs the
     * post-save read-back verification
     * ({@link #verifyOwnWrite(SettingsFileStore, SettingsDocument, String)})
     * for one file-backed scope synchronously, so a test can
     * deterministically simulate a foreign write that interleaved with a
     * save. Production code runs the same logic from {@link #save()} right
     * after each successful write; this seam exists only because the
     * interleaving window (between the atomic write and the read-back) is
     * too narrow to hit reliably as a race. The tests that use it live in
     * the {@code org.hkijena.jipipe.api.service} package (same as this
     * class), so the method is {@code public} only because it is declared on
     * a {@code public final} class; it must not be called from feature code.
     * See {@code JIPipeSettingsServiceComponentSyncTest.interleavedForeignWriteIsNotSilentlyLost}.
     *
     * @param scope the file-backed scope to verify
     */
    public synchronized void verifyOwnWriteForTesting(SettingsScope scope) {
        SettingsFileStore store = stores.get(scope);
        SettingsDocument doc = documents.get(scope);
        if (store == null || doc == null) {
            return;
        }
        verifyOwnWrite(store, doc, "during a test");
    }

    /**
     * <b>Testing only.</b> The thread that performed the last foreign merge
     * (documented at {@link #applyForeignInto}). Used to prove that the
     * watcher callback only hands off and the file load + merge happen on
     * the executor thread. Not part of the production API.
     *
     * @return the name of the last merge thread, or {@code null} if no merge ran
     */
    public String getLastMergeThreadNameForTesting() {
        return lastMergeThreadName;
    }

    /**
     * <b>Testing only.</b> Installs a hook that runs once right after the
     * first merge pass, before its first block is resolved (see
     * {@link #midMergeHookForTesting}).
     *
     * @param hook the hook, or {@code null} to remove it
     */
    public void setMidMergeHookForTesting(Runnable hook) {
        this.midMergeHookForTesting = hook;
    }

    /**
     * @return all three scopes, always; the scope set is fixed by the design
     * and does not depend on what has been registered so far
     */
    public Set<SettingsScope> availableScopes() {
        return Set.of(SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME);
    }

    /**
     * Resolves the profile directory that holds the settings files.
     * <p>
     * A {@link #forTesting(Path)} component returns its injected directory
     * as-is and never touches {@link JIPipe#getJIPipeUserDir(boolean)} (it may
     * be constructed without a service and without a JIPipe instance). The
     * production component resolves the JIPipe user dir on first use and
     * caches it, so an override user-dir base installed after construction is
     * honored. The cache is monitor-guarded like the document maps; the
     * resolution itself does not need the lock for correctness, but keeping
     * the first resolution single under concurrent callers avoids two
     * {@code Files.createDirectories} races.
     *
     * @return the directory holding {@code settings.json}/{@code runtime-settings.json}
     */
    private synchronized Path userDir() {
        Path pinned = userDirOverride;
        if (pinned != null) {
            return pinned;
        }
        Path resolved = resolvedUserDir;
        if (resolved == null) {
            resolved = Objects.requireNonNull(JIPipe.getJIPipeUserDir(false), "JIPipe user dir");
            resolvedUserDir = resolved;
        }
        return resolved;
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
                return userDir().resolve("settings.json");
            case RUNTIME:
                return userDir().resolve("runtime-settings.json");
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
                // The file's current state is what both sides agree on at
                // load time: the pre-save read-merge-write check compares
                // against it, so the first save does not mistake our own
                // loaded content for a foreign write.
                lastKnownFileStates.put(scope, store.fingerprint());
            }
            documents.put(scope, doc);
            attachPathListeners(doc);
            recordSyncPoint(doc);
            startWatchingFor(scope, doc);
            if (scope == SettingsScope.APPLICATION) {
                // Sheets attached before the document existed have to be
                // seeded into (and synced with) this fresh document.
                ensureAttachedSheetsSeeded(doc);
            }
        }
        return doc;
    }

    /**
     * Returns the scope's document only if it has already been materialized,
     * without materializing it.
     * <p>
     * Materializing {@link SettingsScope#APPLICATION} for the first time
     * resolves the profile directory and loads the file. Callers that run
     * during the startup <em>before</em> the profile has been prepared (e.g.
     * the raw settings readers on the splash screen) must not trigger that:
     * they would load the profile directory at an instant when a
     * {@code --override-profile} override is not yet authoritative, and the
     * settings component would then be split-brained between the override
     * profile and the real one. Such callers read the file themselves and only
     * use this as a fallback.
     *
     * @param scope the scope
     * @return the live document for that scope, or {@code null} if it has not
     * been created yet
     */
    public synchronized SettingsDocument materializedDocument(SettingsScope scope) {
        Objects.requireNonNull(scope, "scope");
        return documents.get(scope);
    }

    /**
     * Runs the cross-version carry-over for the {@link SettingsScope#APPLICATION}
     * settings file: unknown blocks (sheet/registry/state IDs this version does
     * not know, or a block kind this version does not know) are recovered from
     * sibling version profiles, newest sibling first, and merged into this
     * version's {@code settings.json} (see {@link SettingsCarryOver}).
     * Registered blocks are never overridden or pulled from a sibling, and
     * sibling profile files are only read.
     * <p>
     * The recovered blocks are recorded in {@link #getRecoveredBlocks()} (block
     * path → sibling version) so the UI can report "settings recovered from
     * version X". A second run recovers nothing new: the blocks are present
     * locally by then.
     * <p>
     * The profile base directory is the parent of {@link #userDir()}
     * ({@code .../profiles}) and the current version is the profile directory's
     * file name, so this also works through {@link #forTesting(Path)}.
     * Must run <em>before</em> the APPLICATION document is loaded, otherwise
     * the recovered content only reaches this session through a reload.
     * <p>
     * TODO(#1345): the startup wiring (migration → carry-over → first document
     * load) is Task 13; this method is deliberately not called from
     * {@link #document(SettingsScope)}.
     *
     * @return the block paths recovered by this run (empty if none)
     */
    public synchronized Set<String> runCarryOver() {
        Path settingsFile = fileFor(SettingsScope.APPLICATION);
        Path profileBaseDir = userDir().toAbsolutePath().getParent();
        String currentVersion = userDir().toAbsolutePath().getFileName().toString();
        if (profileBaseDir == null || settingsFile == null) {
            return Set.of();
        }
        Map<String, String> recovered;
        try {
            recovered = SettingsCarryOver.recoverUnknownBlocksByVersion(settingsFile, profileBaseDir,
                    currentVersion, this::isRegisteredBlock);
        } catch (RuntimeException e) {
            // The carry-over is a recovery convenience: a failure (e.g. an
            // unwritable profile) must never prevent startup.
            logger.warn("Could not recover settings from sibling version profiles in {}", profileBaseDir, e);
            return Set.of();
        }
        if (recovered.isEmpty()) {
            return Set.of();
        }
        recoveredBlocksByVersion.putAll(recovered);
        for (Map.Entry<String, String> entry : recovered.entrySet()) {
            logger.info("Recovered settings block '{}' from version {}",
                    entry.getKey(), entry.getValue());
        }
        if (getProgressInfo() != null) {
            recovered.forEach((block, version) -> getProgressInfo()
                    .log("Recovered settings block '" + block + "' from JIPipe version " + version));
        }
        // If the document was already materialized, the recovered blocks are
        // not in it: pull the merged file in so this session sees them too.
        if (documents.containsKey(SettingsScope.APPLICATION)) {
            reload();
        }
        return new LinkedHashSet<>(recovered.keySet());
    }

    /**
     * @return the settings blocks recovered by {@link #runCarryOver()} in this
     * session, mapped to the sibling version they were recovered from; empty
     * when the carry-over recovered nothing. Read by the UI to report
     * "settings recovered from version X".
     */
    public synchronized Map<String, String> getRecoveredBlocks() {
        return new LinkedHashMap<>(recoveredBlocksByVersion);
    }

    /**
     * @param blockPath the canonical block path
     * @return whether the block is registered in this version. The component
     * registers sheet blocks through {@link #attachSheet} and setting keys
     * through {@link #registerKey}; everything else is unknown (pass-through).
     */
    private boolean isRegisteredBlock(String blockPath) {
        SettingsBlockPath path = new SettingsBlockPath(blockPath);
        if (path.kind() == null) {
            return false;
        }
        String id = path.id();
        switch (path.kind()) {
            case SHEET:
                return attachedBridgedSheetIds.contains(id);
            case REGISTRY:
            case STATE:
                return registeredKeyBlockPaths.contains(blockPath);
            default:
                return false;
        }
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
     * post-save read-back verification in
     * {@link #verifyOwnWrite(SettingsFileStore, SettingsDocument, String)}
     * remains the safety net there.
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
     * watcher's thread (the asynchronous fast path). Delegates the real work
     * to {@link #mergeForeignFile(SettingsScope, SettingsFileStore, String)}.
     * <p>
     * All access to the live document happens under this component's
     * monitor (this method is synchronized on it, as are
     * {@link #document(SettingsScope)}, {@link #save()} and
     * {@link #reload()}), so a merge can never observe a document that a
     * concurrent save or reload is in the middle of replacing.
     *
     * @param scope the scope of the changed file
     * @param file the settings file that changed
     * @param store the store that owns the file
     */
    private synchronized void handleForeignChange(SettingsScope scope, Path file, SettingsFileStore store) {
        mergeForeignFile(scope, store, "detected by the settings file watcher");
    }

    /**
     * Loads the file into a temporary foreign document and runs the per-block
     * merge against the live document, then records the sync point and
     * schedules a converging save when the outcome requires one. Shared by
     * the two detection tiers so they cannot drift apart:
     * <ul>
     * <li>the watcher's asynchronous {@link #handleForeignChange} (fast
     * path: another process wrote and we did not save);</li>
     * <li>the save-time read-merge-write in {@link #save()} (another process
     * wrote <em>before</em> our whole-document write, and its content is
     * merged before that write replaces it).</li>
     * </ul>
     * Callers must hold the component's monitor (this method is not
     * synchronized itself so the save path can use it without an extra lock
     * hop and without a re-entrancy surprise).
     * <p>
     * <b>Guarded load:</b> a corrupt or partially written foreign file must
     * never abort a save nor wipe the local document. {@code SettingsFileStore#load()}
     * would back the broken file up as {@code <name>.bad-<timestamp>} and
     * return an <em>empty</em> document, and merging that empty document
     * would delete every local block. So the file is probed first
     * ({@link #isUnreadableSettingsFile}): if it is not parseable settings
     * JSON, it is preserved as {@code <name>.bad-<timestamp>} (the store's own
     * recovery convention), its state is recorded as handled (so the watcher
     * does not re-deliver the same broken file in a loop), and the method
     * returns without merging. The local document is left unchanged.
     *
     * @param scope the scope of the changed file
     * @param store the store that owns the file
     * @param reason a human-readable reason for the log ("detected by the
     *               settings file watcher", "found before saving", …)
     */
    private void mergeForeignFile(SettingsScope scope, SettingsFileStore store, String reason) {
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
        Path file = store.file();
        if (isUnreadableSettingsFile(file)) {
            // A corrupt or partially written foreign file (e.g. another
            // process crashed mid-write): merging it would either be
            // meaningless (an empty document would delete local blocks) or
            // throw out of the save loop. Keep the local document, preserve
            // the broken content via the store's recovery (.bad-<timestamp>),
            // and record the state as handled so no watcher re-delivers it.
            logger.warn("Foreign settings file {} ({}) is not valid settings JSON; "
                    + "preserving it as a backup and keeping the local document unchanged", file, reason);
            backupCorruptForeignFile(store);
            recordOwnFingerprint(scope, store.fingerprint());
            return;
        }
        SettingsDocument foreign;
        try {
            foreign = store.load();
        } catch (RuntimeException e) {
            // SettingsFileStore#load() is documented never to throw; this is
            // belt and braces so a save loop is never aborted by a bad file.
            logger.warn("Could not load foreign settings file {} ({}); the local document is kept unchanged",
                    file, reason, e);
            recordOwnFingerprint(scope, foreignFingerprint);
            return;
        }
        applyForeignInto(scope, doc, foreign);
        if (disposed) {
            return;
        }
        recordOwnFingerprint(scope, foreignFingerprint);
    }

    /**
     * @param file the settings file
     * @return whether the file exists, has content, and is <em>not</em> a
     * parseable settings document (corrupt / partially written). An absent or
     * empty file is not "unreadable" here: an absent file is handled by the
     * caller before this check, and an empty file is a legitimate (empty)
     * settings document the store can load.
     */
    private static boolean isUnreadableSettingsFile(Path file) {
        try {
            if (!Files.isRegularFile(file) || Files.size(file) == 0) {
                return false;
            }
            JsonNode node = JsonUtils.getObjectMapper().readTree(Files.readString(file));
            return node == null || !node.isObject();
        } catch (IOException e) {
            return true; // unreadable
        } catch (RuntimeException e) {
            return true; // Jackson parse error
        }
    }

    /**
     * Preserves a corrupt foreign settings file using the store's existing
     * recovery convention ({@code <name>.bad-<timestamp>}), so the broken
     * content is not silently discarded: it can be inspected and the user's
     * data is never lost. Mirrors {@code SettingsFileStore#load()}'s backup
     * naming; the store owns its own recovery path, this only makes the
     * pre-check produce the same outcome without merging an empty document.
     */
    private void backupCorruptForeignFile(SettingsFileStore store) {
        Path file = store.file();
        try {
            Path backup = file.resolveSibling(file.getFileName() + BAD_BACKUP_MARKER
                    + BAD_BACKUP_TIMESTAMP.format(Instant.now()));
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("Could not back up the corrupt settings file {}", file, e);
        }
    }

    /**
     * Merges {@code foreign} into the live document {@code doc} using the
     * per-block {@link SettingsDocumentMerger} rules, records the new sync
     * point, updates {@link #foreignConflict}, and schedules a converging
     * save when the outcome requires one. This is the single merge
     * implementation shared by the asynchronous watcher path and the
     * save-time read-merge-write.
     * <p>
     * The blocks and sub-paths the merge is about to touch are observed
     * <em>before</em> the merge runs, with their pre-merge values as the
     * baseline for the "old" value of the emitted events; the merge's block
     * replacements then re-emit them as {@link ChangeOrigin#EXTERNAL_FILE}
     * (via the {@link #foreignMergeOrigin} thread-local). This is what makes
     * "fire per-path events for adopted/conflicted blocks" true for
     * sub-paths that no binding or bridge had observed before.
     * <p>
     * <b>Second-pass re-check (local edits racing the merge).</b> A local edit
     * reaches the document through {@code SettingsBinding.set} →
     * {@link SettingsDocument#set}, which takes only the <em>document</em>
     * lock, not this component's monitor. So an EDT edit can land after the
     * merge snapshotted the locally-changed set but before the merge reaches
     * that block; the first pass would then see "not changed locally, foreign
     * differs" and adopt the foreign value, silently discarding the edit —
     * the same loss class the save-time merge exists to prevent.
     * <p>
     * Two mechanisms close this, in this order, and only when an edit actually
     * raced (a merge with no concurrent edit does exactly one pass):
     * <ol>
     * <li><b>Capture and restore.</b> While the first pass runs, the
     * local-change listener records the edited block's value in
     * {@link #pendingLocalEdits}. After the pass, those values are written
     * back, so an adoption that overwrote an edit cannot destroy its data.</li>
     * <li><b>One bounded second pass</b> over exactly the raced blocks. Its
     * baseline is the first pass's result with every adopted block set to the
     * <em>foreign</em> value — i.e. what both sides agreed on after the first
     * pass — not the live document (which holds the local edits and would
     * make the foreign value look "changed", turning the block into a
     * conflict the foreign side would win). With that baseline the foreign
     * side counts as unchanged and the block is <b>kept</b>, so the caller
     * re-saves and the file converges on the local edit.</li>
     * </ol>
     * The sync point is recorded once, after both steps.
     * <p>
     * <b>Residual:</b> an edit that lands between the second pass's snapshot
     * and its write of the same block can still lose. The window is bounded by
     * one pass over the raced blocks and is not closed further; reaching zero
     * would require holding the document lock for the whole merge, which the
     * merge deliberately does not do (a listener must be able to run, and the
     * EDT must not be starved).
     * <p>
     * The caller holds the component's monitor and is responsible for
     * persisting the document afterwards when needed (the watcher path only
     * schedules it via {@link #saveLater()}; the save path writes it in the
     * same call).
     *
     * @param scope the scope of the document
     * @param doc the live document to merge into
     * @param foreign the document loaded from disk
     * @return the merge outcome
     */
    private SettingsDocumentMerger.MergeResult applyForeignInto(SettingsScope scope, SettingsDocument doc,
                                                               SettingsDocument foreign) {
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
        lastMergeThreadName = Thread.currentThread().getName();
        SettingsDocumentMerger.MergeResult result;
        try {
            Set<String> changedBefore = new LinkedHashSet<>(locallyChangedBlocks.getOrDefault(doc, Set.of()));
            // The testing hook fires inside the first pass, before the first
            // block is resolved — the exact window a concurrent EDT write
            // lands in (the locally-changed set was just snapshotted above).
            mergeInProgress = true;
            SettingsDocumentMerger.MergeResult first;
            try {
                first = SettingsDocumentMerger.merge(doc, foreign, changedBefore,
                        lastSyncedBaselines.getOrDefault(doc, Map.of()),
                        wrapHookWithLocalOrigin(midMergeHookForTesting));
            } finally {
                mergeInProgress = false;
            }
            // Step 1 — restore: a local edit that lost the race against the
            // first pass (the pass adopted the foreign value over it) is put
            // back from the value the local-change listener captured while
            // the pass ran. Without this the edit's value would be gone.
            Set<String> racedEdits = new LinkedHashSet<>(pendingLocalEdits.keySet());
            for (Map.Entry<String, JsonNode> edit : pendingLocalEdits.entrySet()) {
                doc.setBlock(edit.getKey(), edit.getValue());
            }
            pendingLocalEdits.clear();
            // Step 2 — second pass: re-merge with the CURRENT locally-changed
            // set and a baseline in which every block the first pass adopted
            // holds the foreign value, so the restored (or racing) local edit
            // is classified "foreign unchanged, changed locally" and KEPT.
            //
            // The baseline deliberately comes from the first pass's result,
            // not from baselineOf(doc): the document also holds the local
            // edits themselves, so reading it would make those edits the
            // baseline, the foreign value look "changed", and the block a
            // conflict (which the foreign side would then win).
            Set<String> newlyChanged = new LinkedHashSet<>(locallyChangedBlocks.getOrDefault(doc, Set.of()));
            newlyChanged.removeAll(changedBefore);
            newlyChanged.addAll(racedEdits);
            SettingsDocumentMerger.MergeResult second = null;
            if (!newlyChanged.isEmpty()) {
                logger.debug("Settings merge for scope {}: {} block(s) changed locally during the merge; "
                        + "running the second pass", scope, newlyChanged.size());
                Map<String, JsonNode> postPassBaseline = new LinkedHashMap<>(SettingsDocumentMerger.baselineOf(doc));
                for (String adoptedBlock : first.adoptedBlocks()) {
                    JsonNode adoptedValue = foreign.getBlock(adoptedBlock);
                    if (adoptedValue.isMissingNode()) {
                        postPassBaseline.remove(adoptedBlock);
                    } else {
                        postPassBaseline.put(adoptedBlock, adoptedValue.deepCopy());
                    }
                }
                // Only the raced blocks are re-judged: re-running the whole
                // merge would re-litigate blocks the first pass already
                // settled.
                second = restrictTo(SettingsDocumentMerger.merge(doc, foreign,
                        locallyChangedBlocks.getOrDefault(doc, Set.of()), postPassBaseline), newlyChanged);
            }
            result = combine(first, second);
            recordSyncPoint(doc);
            if (result.hasConflicts()) {
                foreignConflict = true;
                logger.info("Settings for scope {} were changed concurrently by another JIPipe instance; "
                        + "the on-disk values won for: {}", scope, result.conflictedBlocks());
            } else {
                foreignConflict = false;
            }
        } finally {
            foreignMergeOrigin.remove();
        }
        if (result.requiresSave()) {
            // Kept blocks are newer than the foreign file (we re-save so
            // the file converges), and a conflicted merge must write the
            // winning foreign values back so both processes end up with
            // the same content. saveLater() coalesces with pending
            // debounced saves and self-ignores its own write.
            saveLater();
        }
        return result;
    }

    /**
     * Keeps only the given blocks in each set of a merge result, so a second
     * pass that re-judges the whole document only reports on the blocks it
     * was meant to settle.
     */
    private static SettingsDocumentMerger.MergeResult restrictTo(SettingsDocumentMerger.MergeResult result,
                                                                 Set<String> blocks) {
        Set<String> adopted = new LinkedHashSet<>(result.adoptedBlocks());
        adopted.retainAll(blocks);
        Set<String> kept = new LinkedHashSet<>(result.keptBlocks());
        kept.retainAll(blocks);
        Set<String> conflicted = new LinkedHashSet<>(result.conflictedBlocks());
        conflicted.retainAll(blocks);
        return new SettingsDocumentMerger.MergeResult(adopted, kept, conflicted);
    }

    /**
     * Combines the outcome of the merge passes into one report. The sets are
     * unions with a precedence that reflects the <em>final</em> state of each
     * block: a block kept by the second pass was stolen back from the first
     * pass's adoption, so "kept" wins over both; a block the second pass
     * itself adopted or conflicted was resolved by it, so those wins over a
     * first-pass "kept". (Adopted and conflicted never contain the same
     * block, and kept blocks are never re-adopted by a later pass.)
     */
    private static SettingsDocumentMerger.MergeResult combine(SettingsDocumentMerger.MergeResult first,
                                                              SettingsDocumentMerger.MergeResult second) {
        if (second == null) {
            return first;
        }
        Set<String> adopted = new LinkedHashSet<>(first.adoptedBlocks());
        adopted.removeAll(second.keptBlocks());
        adopted.addAll(second.adoptedBlocks());
        adopted.removeAll(second.conflictedBlocks());
        Set<String> conflicted = new LinkedHashSet<>(first.conflictedBlocks());
        conflicted.removeAll(second.keptBlocks());
        conflicted.addAll(second.conflictedBlocks());
        Set<String> kept = new LinkedHashSet<>(first.keptBlocks());
        kept.removeAll(second.adoptedBlocks());
        kept.removeAll(second.conflictedBlocks());
        kept.addAll(second.keptBlocks());
        return new SettingsDocumentMerger.MergeResult(adopted, kept, conflicted);
    }

    /**
     * Records the given fingerprint as this process's own on the scope's
     * watcher (self-write suppression for the state that was just handled)
     * and as the scope's {@link #lastKnownFileStates} entry (the baseline for
     * the pre-save read-merge-write check). No-op for the watcher when there
     * is none (headless), but the last-known state is always recorded.
     */
    /**
     * <b>Testing only.</b> Wraps the mid-merge hook so the simulated local
     * edit runs with the default (local) origin, exactly like a real edit on
     * another thread, instead of inheriting the merge's
     * {@link ChangeOrigin#EXTERNAL_FILE} marker from this thread.
     */
    private Runnable wrapHookWithLocalOrigin(Runnable hook) {
        if (hook == null) {
            return null;
        }
        return () -> {
            ChangeOrigin saved = foreignMergeOrigin.get();
            foreignMergeOrigin.remove();
            try {
                hook.run();
            } finally {
                foreignMergeOrigin.set(saved);
            }
        };
    }

    private void recordOwnFingerprint(SettingsScope scope, FileFingerprint fingerprint) {
        if (fingerprint == null) {
            return;
        }
        lastKnownFileStates.put(scope, fingerprint);
        // Touched from the merge executor and from save() (both under the
        // component monitor), so the plain map lookup is safe here.
        SettingsFileWatching watch = watchers.get(scope);
        if (watch != null) {
            watch.setOwnFingerprint(fingerprint);
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
        registeredKeyBlockPaths.add(key.blockPath());
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
        // The write guard is a lambda over this component, so it always sees
        // the currently installed guard (setWriteGuard may run after bind).
        binding.setWriteGuard(this::canWrite);
        trackedBindings.add(binding);
        observeBlockWrites(doc, key.blockPath(), key.subPath());
        return binding;
    }

    /**
     * The single choke point for setting writes through the typed-key path:
     * every {@link SettingsBinding} handed out by {@link #bind(JIPipeSettingKey)}
     * consults it ({@link SettingsBinding#set(Object)} and
     * {@link SettingsBinding#reset()}) before mutating a document, and a
     * rejection throws
     * {@link org.hkijena.jipipe.api.settings.SettingsAccessDeniedException}
     * without touching the document.
     * <p>
     * <b>Sheet-bridge writes are not covered yet.</b> A
     * {@link SettingsSheetBridge} holds a sheet, not keys, and no key exists at
     * most sheet sub-paths, so the bridge cannot be routed through this check
     * without inventing one key per sheet parameter. Its writes become guarded
     * once sheet parameters are keyified (the per-key migration); until then a
     * write through the sheet bridge bypasses the guard.
     * <p>
     * <b>Today this returns {@code true} for every key</b>: the runtime
     * document is local, so there is no remote administrator that a
     * {@link SettingsAccessPolicy#RUNTIME_ADMIN} or
     * {@link SettingsAccessPolicy#READ_ONLY} policy would protect the value
     * from. The method exists so the future #1102 runtime host can override the
     * decision in one place: it installs a {@link #setWriteGuard} predicate and
     * the guard becomes the policy check — the {@link JIPipeSettingKey#access()}
     * policy is what that predicate is expected to enforce (e.g. reject
     * user-originated writes to {@code RUNTIME_ADMIN} and every write to
     * {@code READ_ONLY} while a remote runtime owns the document).
     *
     * @param key the setting about to be written
     * @return whether the write is permitted
     */
    public synchronized boolean canWrite(JIPipeSettingKey<?> key) {
        Objects.requireNonNull(key, "key");
        Predicate<JIPipeSettingKey<?>> guard = writeGuard;
        return guard == null || guard.test(key);
    }

    /**
     * Installs the process-wide write guard that {@link #canWrite} consults:
     * the seam the #1102 runtime host uses to protect settings it owns. The
     * guard receives every key whose value is about to be written through a
     * binding; returning {@code false} rejects the write with
     * a {@link org.hkijena.jipipe.api.settings.SettingsAccessDeniedException}.
     * <p>
     * There is exactly one guard per component (i.e. per settings service):
     * installing a new one replaces the previous one, and it also governs
     * bindings handed out earlier — they hold a live reference to this
     * component, not a copy of the guard.
     * <p>
     * {@code null} clears the guard, i.e. every key is writable again.
     *
     * @param guard the guard, or {@code null} to permit every write
     */
    public synchronized void setWriteGuard(Predicate<JIPipeSettingKey<?>> guard) {
        this.writeGuard = guard;
    }

    /**
     * The block → scope routing map used to migrate a legacy profile's blocks
     * into the scoped documents: the runtime-scoped blocks of the registered
     * keys (a key declared with {@code scope(RUNTIME)} makes its whole block
     * runtime-scoped) layered over the static fallback
     * {@link SettingsScopes#scopeByBlock()} for not-yet-keyified blocks.
     * Registered keys win over the static map.
     * <p>
     * Blocks absent from the result are {@link SettingsScope#APPLICATION}.
     *
     * @return an immutable routing map containing only {@link SettingsScope#RUNTIME} entries
     */
    public synchronized Map<String, SettingsScope> effectiveScopeByBlock() {
        Map<String, SettingsScope> result = new LinkedHashMap<>(SettingsScopes.scopeByBlock());
        // Iterate a defensive copy of the registered keys (the registeredKeys()
        // accessor) rather than the live map, so a concurrent reader cannot
        // observe a partially-mutated LinkedHashMap.
        for (JIPipeSettingKey<?> key : registeredKeys()) {
            if (key.scope() == SettingsScope.RUNTIME) {
                result.put(key.blockPath(), SettingsScope.RUNTIME);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Writes a whole block into the given document as bookkeeping content,
     * not as a user-visible setting change: no change event is emitted for it
     * and the block is not marked as locally changed (so a foreign merge
     * cannot treat it as a conflicting local edit). The write is still a
     * normal {@link SettingsDocument#setBlock(String, JsonNode)}, so the file
     * watcher, the merge baseline and the save path see it.
     * <p>
     * Used by the recent-projects list, whose {@link #getLegacyChangedEventEmitter()}
     * mirror already reports changes and whose per-item diff carries no
     * meaning for subscribers.
     * <p>
     * Thread-safety: the whole cycle (mark quiescent → write) holds the
     * component monitor, so a mutation from another thread (e.g. a sheet edit
     * on the EDT) can never run while the document is marked quiescent — its
     * events and local-change tracking are therefore never suppressed.
     *
     * @param doc the document to write to
     * @param blockPath the block to replace
     * @param value the new block value
     */
    public synchronized void writeQuiescently(SettingsDocument doc, String blockPath, JsonNode value) {
        beginQuiescentWrite(doc);
        try {
            doc.setBlock(blockPath, value);
        } finally {
            endQuiescentWrite(doc);
        }
    }

    /**
     * Writes (or, with a {@code null} value, removes) one document location
     * and attributes the resulting change to the given
     * {@link ChangeOrigin}.
     * <p>
     * This is the origin-carrying write seam. The origin a change is reported
     * with — and, for a local origin, whether it schedules the debounced
     * auto-save (see {@link #scheduleSaveForLocalEdit}) — is read from the
     * private {@link #foreignMergeOrigin} thread-local by the document's path
     * listeners. The merge path sets that thread-local itself; every other
     * writer (bindings, sheet bridges) keeps the default
     * {@link ChangeOrigin#LOCAL_PROGRAMMATIC}. This method exposes the same
     * mechanism for a caller that owns a distinct origin, currently the raw
     * settings editor ({@link ChangeOrigin#RAW_EDIT} for an edited value,
     * {@link ChangeOrigin#RESET} for restoring a default).
     * <p>
     * The thread-local is set for the duration of this call only and restored
     * in a {@code finally} (the same discipline as the merge hook wrapper), so
     * an exception out of a path listener cannot leak the origin to later
     * writes on this thread.
     * <p>
     * The location is made observable first (the same
     * {@link #ensureObserved} step the merge path performs before it writes a
     * block): the change event and the debounced auto-save are produced by the
     * document's path listeners (see {@link #onPathChanged} and
     * {@link #registerLocalChangeListener}), and those exist only for observed
     * locations. A raw edit may address a location no binding ever touched (an
     * unknown block), which a plain {@link SettingsDocument#set} would change
     * silently — not broadcast and, worse, never saved. ("Observable" is thus a
     * superset of what a binding observed; it broadens what is reported, never
     * what an existing write does.)
     * <p>
     * Additive: it changes no existing behaviour.
     *
     * @param scope the scope of the document to write to
     * @param blockPath the canonical block path
     * @param subPath the slash-separated sub-path within the block
     * @param value the value to store, or {@code null} to remove the location
     * @param origin the origin to attribute to the change
     */
    public synchronized void writeWithOrigin(SettingsScope scope,
                                             String blockPath,
                                             String subPath,
                                             JsonNode value,
                                             ChangeOrigin origin) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(origin, "origin");
        SettingsDocument doc = document(scope);
        // The same pre-write observation step the merge path performs.
        ensureObserved(doc, blockPath, subPath);
        ChangeOrigin saved = foreignMergeOrigin.get();
        foreignMergeOrigin.set(origin);
        try {
            doc.set(blockPath, subPath, value);
        } finally {
            foreignMergeOrigin.set(saved);
        }
    }

    /**
     * Marks the given document as being written quiescently (see
     * {@link #writeQuiescently}): until the matching {@link #endQuiescentWrite}
     * on the same thread, its mutations emit no change events and do not count
     * as local changes.
     *
     * @param doc the document
     */
    private void beginQuiescentWrite(SettingsDocument doc) {
        Map<SettingsDocument, Integer> depth = quiescentWrites.get();
        depth.merge(doc, 1, Integer::sum);
    }

    /**
     * Ends a quiescent write started with {@link #beginQuiescentWrite}.
     *
     * @param doc the document
     */
    private void endQuiescentWrite(SettingsDocument doc) {
        Map<SettingsDocument, Integer> depth = quiescentWrites.get();
        Integer current = depth.get(doc);
        if (current == null || current <= 1) {
            depth.remove(doc);
            if (depth.isEmpty()) {
                quiescentWrites.remove();
            }
        } else {
            depth.put(doc, current - 1);
        }
    }

    /**
     * @param doc the document
     * @return whether the current thread is inside a quiescent write of the document
     */
    private boolean isQuiescent(SettingsDocument doc) {
        Map<SettingsDocument, Integer> depth = quiescentWrites.get();
        return !depth.isEmpty() && depth.containsKey(doc);
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
     * <p>
     * <b>Auto-persist.</b> A genuine local write also schedules the
     * debounced save ({@link #saveLater()}). This is the trigger that makes
     * a UI parameter edit — or any feature write — durable without an
     * explicit save by the caller (see {@link #scheduleSaveForLocalEdit}).
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
            if (isQuiescent(doc)) {
                return; // bookkeeping write (e.g. the recent-projects list)
            }
            if (foreignMergeOrigin.get() == ChangeOrigin.EXTERNAL_FILE) {
                // The merge's own write of an adopted/conflicted block is not
                // a local change: marking the block locally changed would make
                // the next merge pass, and the next save, treat content that
                // came from the foreign file as this process's edit.
                return;
            }
            Set<String> changed = locallyChangedBlocks.get(doc);
            if (changed != null) {
                changed.add(blockPath);
            }
            if (mergeInProgress) {
                // A genuinely local edit landed while the merge is applying
                // foreign values. Capture the full block value: the merge may
                // adopt the foreign value over it before the second pass
                // runs, and only here is the edited value still visible.
                pendingLocalEdits.put(blockPath, doc.getBlock(blockPath).deepCopy());
            }
            scheduleSaveForLocalEdit();
        });
    }

    /**
     * Schedules the debounced save for a local write that just landed, so
     * the settings file converges without an explicit {@code save()} by the
     * caller.
     * <p>
     * <b>Origin gate.</b> Only genuinely local origins schedule the save
     * ({@link ChangeOrigin#LOCAL_EDIT}, {@link ChangeOrigin#LOCAL_PROGRAMMATIC},
     * {@link ChangeOrigin#RESET}, {@link ChangeOrigin#RAW_EDIT}). This keeps
     * the engine's own machinery out of the cycle:
     * <ul>
     * <li><b>Foreign merges</b> ({@link ChangeOrigin#EXTERNAL_FILE}) are
     * excluded — the merge path already schedules its own converging
     * {@link #saveLater()} (see {@code applyForeignInto}); a local edit and a
     * foreign change in the same window coalesce into that one save.</li>
     * <li><b>Quiescent bookkeeping writes</b> return before this method (see
     * {@link #onPathChanged} and the caller), because they manage their own
     * persistence.</li>
     * <li><b>The save itself</b> writes no settings block values (it only
     * stamps {@code $meta}, which is a reserved path, and flushes bridges
     * with values that are already in the document), so a save cannot
     * re-trigger this.</li>
     * </ul>
     * Never called from {@link #save()} or the merge executor: both run their
     * writes under an origin that this gate rejects.
     */
    private void scheduleSaveForLocalEdit() {
        if (disposed) {
            return;
        }
        if (!isLocalOrigin(foreignMergeOrigin.get())) {
            // Foreign merge (EXTERNAL_FILE): the merge path schedules its own
            // converging save. Any other origin is either a bookkeeping write
            // (already returned before the caller) or engine machinery.
            return;
        }
        // Called from the document path listener. In Swing mode saveLater()
        // restarts a Swing Timer, which must happen on the EDT; an off-EDT
        // local write (a background feature thread) hands the restart over.
        // The pending state itself is recorded here, on the writing thread,
        // so the auto-save is scheduled deterministically even when the
        // hand-off has not run yet (and saveNowForTesting() can observe it).
        if (swingSaveLaterTimer != null) {
            pendingSwingSave = true;
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                swingSaveLaterTimer.restart();
            } else {
                javax.swing.SwingUtilities.invokeLater(swingSaveLaterTimer::restart);
            }
        } else {
            saveLater(); // headless: no Swing involved, inline debounce scheduling
        }
    }

    /**
     * @param origin a change origin
     * @return whether it is a genuinely local origin that must auto-persist
     */
    private static boolean isLocalOrigin(ChangeOrigin origin) {
        return origin == ChangeOrigin.LOCAL_EDIT
                || origin == ChangeOrigin.LOCAL_PROGRAMMATIC
                || origin == ChangeOrigin.RESET
                || origin == ChangeOrigin.RAW_EDIT;
    }

    /**
     * Registers a loop-safe bridge projecting the given application settings
     * sheet onto the {@code "sheet:<id>"} block of the APPLICATION document.
     * Safe to call before the document exists: the document is materialized
     * (lazily) on demand.
     * <p>
     * <b>Document-later case.</b> When the APPLICATION document is created
     * <em>after</em> this call (a sheet registered very early in startup, or
     * a component constructed on a profile that is migrated afterwards), the
     * fresh document is seeded from the sheet's current values and the
     * bridge is re-created against it — see {@link #ensureAttachedSheetsSeeded}.
     * Without that, the sheet would hold an unsaved state the document never
     * sees.
     *
     * @param sheet the sheet to bridge
     */
    public synchronized void attachSheet(JIPipeApplicationSettingsSheet sheet) {
        Objects.requireNonNull(sheet, "sheet");
        // Materializing the document is lazy and idempotent, so this is safe
        // to call before the document exists (e.g. at sheet registration
        // time): the bridge is created against the fresh, loaded document.
        SettingsDocument doc = document(SettingsScope.APPLICATION);
        SettingsSheetBridge bridge = attachBridge(doc, sheet);
        attachedBridges.put(sheet, bridge);
        attachedBridgedSheetIds.add(sheet.getId());
    }

    /**
     * @param sheet the sheet
     * @return whether a bridge for the sheet is currently attached to the
     * live APPLICATION document
     */
    public synchronized boolean isSheetAttached(JIPipeApplicationSettingsSheet sheet) {
        return attachedBridges.containsKey(sheet);
    }

    /**
     * Attaches a sheet bridge to the live APPLICATION document. If the
     * document was created after some sheets were attached, that creation
     * re-attaches them here (see {@link #attachSheet}).
     */
    private void ensureAttachedSheetsSeeded(SettingsDocument doc) {
        for (JIPipeApplicationSettingsSheet sheet : unbridgedSheets(doc)) {
            attachedBridges.put(sheet, attachBridge(doc, sheet));
            attachedBridgedSheetIds.add(sheet.getId());
        }
    }

    /**
     * @param doc the live APPLICATION document
     * @return the sheets whose bridge is missing or points at a replaced
     * document, in registration order
     */
    private java.util.List<JIPipeApplicationSettingsSheet> unbridgedSheets(SettingsDocument doc) {
        java.util.List<JIPipeApplicationSettingsSheet> result = new java.util.ArrayList<>();
        JIPipeService service = getService();
        if (service == null) {
            return result; // testing component without a service: nothing registers sheets
        }
        for (JIPipeApplicationSettingsSheet sheet : service.getApplicationSettings().getRegisteredSheets().values()) {
            SettingsSheetBridge bridge = attachedBridges.get(sheet);
            if (bridge == null || bridge.document() != doc) {
                result.add(sheet);
            }
        }
        return result;
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
            // The disk content is what both sides agree on again: the
            // pre-save read-merge-write check starts from it.
            lastKnownFileStates.put(scope, store.fingerprint());
            if (scope == SettingsScope.APPLICATION) {
                // Re-create every bridge on the fresh document and attach it:
                // the already-bridged sheets (including ones that are not in
                // the service's registry, e.g. in unit tests) plus every
                // registered sheet that never got a bridge (e.g. attached
                // before the document existed).
                for (JIPipeApplicationSettingsSheet sheet : bridgedSheets) {
                    attachedBridges.put(sheet, attachBridge(fresh, sheet));
                }
                ensureAttachedSheetsSeeded(fresh);
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
     * <b>Read-merge-write before the blind replace.</b> A save is a
     * whole-document atomic replace, so a foreign write that landed on disk
     * <em>before</em> this save would simply be overwritten (the post-save
     * verification below can only see writes that land <em>after</em> our
     * write). Before writing, each scope therefore checks whether the
     * on-disk state still equals the state this process last wrote or merged
     * (its own fingerprint or its last-synced fingerprint). If it does not —
     * a foreign writer got there first — the foreign file is merged into the
     * live document with the same per-block rules and events as the watcher
     * path ({@link #mergeForeignFile}) <em>first</em>, and only the merged
     * result is written. If it does, the save proceeds unchanged.
     * <p>
     * <b>What this guarantees, precisely:</b> every foreign write that is
     * present on disk at the moment of the pre-write fingerprint check is
     * merged before it can be replaced. It does not make a save atomic with
     * respect to a foreign write that lands <em>between</em> that check and
     * the write — such a write is the post-save verification's job (below),
     * and a foreign write landing between the verification's own two reads is
     * undetectable by content comparison. The residual windows are
     * microsecond-scale, not zero.
     * <p>
     * Each successful save is then verified by
     * {@link #verifyOwnWrite(SettingsFileStore, SettingsDocument, String)}:
     * the file is read back and compared against the content this process
     * just wrote, closing the watcher's documented own-fingerprint
     * suppression window (a foreign write interleaved between this process's
     * write and its fingerprint read would otherwise be silently absorbed,
     * leaving the in-memory state diverged from disk with no event to
     * reconcile it — see {@code SettingsFileWatcher}).
     * <p>
     * Ordering per scope: flush bridges → pre-save check → (merge if the
     * file is foreign) → stamp → write → record sync point → record own
     * fingerprint → verify the write.
     */
    public synchronized void save() {
        if (disposed) {
            return;
        }
        for (SettingsSheetBridge bridge : attachedBridges.values()) {
            bridge.flushToDocument();
        }
        for (Map.Entry<SettingsScope, SettingsFileStore> entry : stores.entrySet()) {
            SettingsScope scope = entry.getKey();
            SettingsDocument doc = documents.get(scope);
            if (doc == null) {
                continue;
            }
            SettingsFileStore store = entry.getValue();
            // Read-merge-write: adopt a foreign write that is already on
            // disk BEFORE our blind replace would destroy it. The merge
            // records its own sync point, so the merged document is what
            // gets written below. A corrupt foreign file never aborts the
            // save: the merge path keeps the local document and logs.
            mergeForeignBeforeSave(scope, store);
            stampMeta(doc);
            try {
                store.save(doc);
            } catch (RuntimeException e) {
                logger.error("Could not save settings document for scope {}", scope, e);
                JIPipeService service = getService();
                if (service != null) {
                    service.getLogService().error("Could not save settings document for scope " + scope, e);
                }
                continue; // the write failed: no successful sync point
            }
            recordSyncPoint(doc); // a successful save is a sync point
            recordOwnFingerprint(scope, store.fingerprint());
            verifyOwnWrite(store, doc, "after saving");
        }
        if (getProgressInfo() != null) {
            getProgressInfo().log("Saved settings documents to disk");
        }
    }

    /**
     * Pre-save check of the read-merge-write protocol: when the file on disk
     * no longer matches the last state this process wrote or merged, a
     * foreign writer got there first and its content must be merged into the
     * live document before the save may replace it.
     * <p>
     * Three states are distinguished by the fingerprint alone (no content
     * read is needed): absent file (nothing to lose — write), a fingerprint
     * equal to the recorded own/last-synced one (no foreign writer — write),
     * and anything else (foreign — merge first). A file that cannot be
     * fingerprinted is treated like a foreign change and goes through the
     * merge, which is non-destructive (see the guarded load in
     * {@link #mergeForeignFile}): either the foreign content is merged, or it
     * is unreadable and the local document is kept. Either way the save then
     * proceeds with the (possibly merged) local document.
     *
     * @param scope the scope being saved
     * @param store the scope's store
     */
    private void mergeForeignBeforeSave(SettingsScope scope, SettingsFileStore store) {
        SettingsDocument doc = documents.get(scope);
        if (doc == null) {
            return;
        }
        Path file = store.file();
        if (!Files.isRegularFile(file)) {
            return; // nothing on disk (first save, or deleted): nothing to clobber
        }
        FileFingerprint onDisk = store.fingerprint();
        // The last state this process produced or accepted for this file.
        // A save records it as own; a merge records the merged foreign state
        // as own. Anything else means another writer changed the file since.
        FileFingerprint expected = lastKnownFileStates.get(scope);
        if (onDisk != null && expected != null && onDisk.equals(expected)) {
            return; // the file is exactly what we last wrote/merged: no foreign writer
        }
        logger.info("Settings file {} changed on disk before saving; merging the foreign content first",
                file);
        mergeForeignFile(scope, store, "found before saving (another instance wrote it first)");
    }

    /**
     * Self-healing check after a successful save: reads the file back and
     * compares it against the content this process actually wrote.
     * <p>
     * <b>What it covers:</b> a foreign write that lands <em>during or just
     * after</em> this process's atomic replace. Without it the watcher would
     * absorb such a write: it suppresses changes whose fingerprint equals the
     * one the owner recorded after its save, and the owner records that
     * fingerprint from the file as it is <em>at the moment of the read</em>
     * — so a foreign write that lands before that read is recorded as our
     * own and never reported. This check compares the read-back content
     * against what this process actually wrote, so that case is detected and
     * merged.
     * <p>
     * It complements (and does not replace) the pre-save read-merge-write in
     * {@link #save()}, which covers the opposite direction — a foreign write
     * that landed <em>before</em> our write would otherwise have been
     * replaced blindly.
     * <p>
     * On a mismatch the observed file content is fed through
     * {@link #mergeForeignFile} — the same merge implementation the watcher
     * path uses — so the foreign blocks are adopted/conflicted/kept with
     * events. This method runs from {@link #save()} while the component's
     * monitor is already held; {@code mergeForeignFile} deliberately does not
     * re-acquire it, so no extra lock hop or deadlock is involved.
     * <p>
     * <b>What it does not cover:</b> a byte-identical foreign write (a
     * foreign process that writes exactly the same content we did is
     * indistinguishable from our own write by any content comparison; such a
     * write is a no-op for the values anyway). A foreign write landing
     * <em>during</em> the verification read is covered: the fingerprint is
     * read first and the decision is made on the content read second, so a
     * write in between surfaces as a content mismatch and is merged.
     *
     * @param store the store the document was just saved through
     * @param doc the document that was just saved; its scope identifies the
     *            watcher to record the own fingerprint on
     * @param reason a human-readable description of when the verification ran
     *               ("after saving", "during a test"), used in the log message
     */
    private void verifyOwnWrite(SettingsFileStore store, SettingsDocument doc, String reason) {
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
                recordOwnFingerprint(doc.scope(), own);
                return;
            }
            // A foreign writer interleaved between our save and the
            // fingerprint read: the on-disk content is not ours. Treat it
            // as a foreign change instead of silently recording the
            // foreign fingerprint as our own.
            logger.info("Settings file {} was replaced by another process {}; merging it", file, reason);
            mergeForeignFile(doc.scope(), store, "detected " + reason);
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
            pendingSwingSave = true;
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
     * <b>Testing only.</b> Runs the pending debounced save (see
     * {@link #saveLater()}) synchronously, so a test never has to poll the
     * 250 ms wall clock or depend on a running Swing timer queue (a JVM with
     * a display but no live EDT would otherwise never fire the timer inside
     * a test). Mirrors the debounce in both modes:
     * <ul>
     * <li>Swing mode: the pending state tracked by {@link #pendingSwingSave}
     * is cleared, the {@link Timer} is stopped and {@link #save()} is run
     * inline.</li>
     * <li>Headless mode: the pending scheduled task is cancelled and
     * {@link #save()} is run inline.</li>
     * </ul>
     * Not part of the production API; a {@code *ForTesting} seam,
     * consistent with the existing {@code *ForTesting} methods of this
     * component.
     *
     * @return whether a save was pending before this call
     */
    public boolean saveNowForTesting() {
        if (swingSaveLaterTimer != null) {
            boolean pending = pendingSwingSave;
            pendingSwingSave = false;
            swingSaveLaterTimer.stop();
            if (pending) {
                // A start requested from a background thread may still be
                // queued on the EDT: drop it, so it cannot fire a second save
                // after this one.
                javax.swing.SwingUtilities.invokeLater(swingSaveLaterTimer::stop);
                save();
            }
            return pending;
        }
        boolean pending;
        synchronized (saveLaterLock) {
            pending = pendingSaveTask != null;
            if (pendingSaveTask != null) {
                pendingSaveTask.cancel(false);
                pendingSaveTask = null;
            }
        }
        if (pending) {
            save();
        }
        return pending;
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
        lastKnownFileStates.clear();
        localChangeTrackedBlocks.clear();
        recoveredBlocksByVersion.clear();
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
     * changes: reads the old value from the cache, updates the baseline, and
     * delivers the fine-grained and legacy events to subscribers.
     * <p>
     * <b>Delivery thread.</b> The cached old/new values are captured here, on
     * the mutating thread, so the event always reports the change that
     * actually happened. {@link #dispatchChangeEvent} then keeps the historic
     * synchronous contract for headless runs and for mutations that already
     * run on the Swing EDT (normal UI edits), and hands an <b>off-EDT</b>
     * mutation (the foreign-merge executor, or a background feature thread)
     * to the EDT via {@link javax.swing.SwingUtilities#invokeLater}, so UI
     * subscribers never run on the merge thread. The merge executor is a
     * single thread, so the events it posts keep their relative order.
     * <p>
     * The change origin is taken from {@link #foreignMergeOrigin}, so
     * mutations applied by a foreign merge are re-emitted as
     * {@link ChangeOrigin#EXTERNAL_FILE} while all other (local) mutations keep
     * the default {@link ChangeOrigin#LOCAL_PROGRAMMATIC}.
     */
    private void onPathChanged(SettingsDocument doc, String path, JsonNode newValue) {
        if (isQuiescent(doc)) {
            // Bookkeeping write: keep the "old value" baseline current, but do
            // not report it as a setting change.
            Map<String, JsonNode> cache = lastValues.get(doc);
            if (cache != null) {
                cache.put(path, newValue);
            }
            return;
        }
        Map<String, JsonNode> valueCache = lastValues.get(doc);
        if (valueCache == null) {
            return;
        }
        JsonNode oldValue = valueCache.get(path);
        if (!Objects.equals(oldValue, newValue)) {
            valueCache.put(path, newValue);
            SettingsChangedEvent event = new SettingsChangedEvent(
                    doc.scope(), path, oldValue, newValue, foreignMergeOrigin.get());
            dispatchChangeEvent(event);
        }
    }

    /**
     * Delivers one change event (and its legacy aggregate companion) to the
     * subscribers on the Swing EDT when a display is present, inline when
     * headless. See {@link #onPathChanged} for why.
     * <p>
     * Headless runs and mutations already on the EDT deliver inline and
     * synchronously (the caller's {@code binding.set(...)} still has notified
     * its subscribers on return). An off-EDT mutation is handed to the EDT.
     */
    private void dispatchChangeEvent(SettingsChangedEvent event) {
        Runnable deliver = () -> {
            changedEventEmitter.emit(event);
            legacyChangedEventEmitter.emit(new LegacyChangedEvent());
        };
        if (GraphicsEnvironment.isHeadless() || javax.swing.SwingUtilities.isEventDispatchThread()) {
            deliver.run(); // inline: headless runs, and already on the UI thread
            return;
        }
        javax.swing.SwingUtilities.invokeLater(deliver);
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
