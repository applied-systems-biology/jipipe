# Implementation Plan: Content-Addressed Backup Store (#1342)

**Spec:** `docs/superpowers/specs/2026-10-24-backup-content-addressed-store-design.md` (approved)
**Branch:** `1342-new-backup-system` (rebased on post-#753 master)
**Work item:** https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1342

## Context for an engineer who has not seen this code

### What exists today (the system being replaced)

- `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeProjectBackupServiceComponent.java`
  - Owns a Swing `Timer` (default every 7 min, `restartTimer()`), a single executor
    `private final JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Backups")`,
    `backup(window)` (cancels the in-flight run for that window via
    `queue.cancelIf(r -> r instanceof CreateBackupRun cbr && cbr.window == window)`, then enqueues),
    `getCurrentBackupPath()` (settings `customBackupPath` or `<userDir>/backups`),
    `scheduleCleanup()` (enqueues `ThinBackupsRun` on the **cleanup** queue), and
    `postprocess()` (subscribes to settings changes, starts the timer, optionally schedules cleanup).
  - Contains the **inner class `CreateBackupRun`** — the full-copy backup: resolves
    `getCurrentBackupPath()/<window.getSessionId()>/`, writes the whole project via
    `window.getProject().saveProject(targetFile, false)` (a full `.jipx`/`.jip` save), and writes
    `backup-info.json` (`JIPipeProjectBackupSessionInfo`: `project-session-id`,
    `project-storage-path`, `last-backup-date-time` — kebab-case JsonGetter/JsonSetter).
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/backups/` — `JIPipeProjectBackupItem`
  (`projectPath` Path, `originalProjectPath` String, `backupTime` LocalDateTime, `sessionId` String),
  `JIPipeProjectBackupItemCollection` (`sessionId`, `legacy` flag, item list, `renderName()`),
  `CollectBackupsRun` (scans the backups dir: session dirs via `backup-info.json` + files matching
  `JIPipeProjectBackupUtils.isBackupFile` (`.jip`/`.jipx`); loose files at the root become
  `legacy-*` items; groups by session; **ends with `PruneBackupsRun.pruneEmptySessions(...)`**),
  `ThinBackupsRun` (cascading keep-rules; deletes via `Files.delete(item.getProjectPath())`),
  `DeleteOldBackupsByAgeRun` (same shape, age-based), `PruneBackupsRun` (deletes sessions where
  `sessionHasBackups(...)` is false; **`sessionHasBackups` only looks for `isBackupFile` files —
  a snapshot-only session would be wrongly pruned**), `JIPipeProjectBackupUtils`.
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/backups/JIPipeDesktopBackupManagerPanel.java`
  — the manager UI. `restoreBackup(item)` = `Files.copy(item.getProjectPath(), path, REPLACE_EXISTING)`
  + `openProject(path, true)`. `deleteSelection()`, `removeUnnamedBackups()`/`removeNamedBackups()`
  (via `PruneBackupsRun`), `thinBackups()` (modal `runInDialog`). Read this file before Task 7.
- Settings: `plugins/settings/application/JIPipeBackupApplicationSettings.java` (ID
  `org.hkijena.jipipe:backups`) — `enableBackups`, `backupDelay` (min), `customBackupPath`
  (`OptionalPathParameter`), nested `CleanupSettings` (`AbstractJIPipeParameterCollection` with
  `@JIPipeParameter("...")` kebab-case IDs, `uiOrder`, `OptionalIntegerParameter` knobs).

### Key APIs this plan builds on (verified to exist)

- `org.hkijena.jipipe.utils.FileLocker` — **`tryWriteLock()` is a probe: it releases immediately
  after a successful acquisition. `acquireWriteLock()` retries forever (1 s interval) and HOLDS
  the lock until `releaseLock()`.** A bounded, held acquisition does not exist yet (Task 1 adds it).
- `JIPipeProject.saveProject(OutputStream)` → `new JIPipeLegacyProjectWriter().write(project, os)`
  — the stream variant. **Frozen asymmetry from #753: stream writes do NOT merge authors.** This
  is exactly what backups want (snapshot the model as-is).
- `JIPipeProjectWorkspace` (`api/project/jipx/`): `getBaseDirectory()`,
  `materializeFromModel(JIPipeProgressInfo)` (idempotent; rewrites only the 9 part files;
  payload-preserving), `readParts()`, `markSaved(Path)`, `markDirty()`.
  **Workspace lock idiom (from `JIPipeXProjectWriter`): `synchronized (workspace) { ... }` on the
  workspace instance guards all workspace-touching sequences.** The new backup code uses the same
  monitor; lock order is always workspace lock → store lock, never inverted (GC touches only the store).
- `JsonUtils.saveToFile(obj, path)` / `JsonUtils.readFromFile(path, class)` — pretty-printed JSON.
- `StringUtils.formatSize(long)` — human-readable sizes for the UI.
- `ArchiveUtils.compressDirectoryToZip(root, rootName, zip, pi)` **wraps content in a root folder
  entry — NOT usable for `.jipx` assembly** (container entries must sit at the zip root). Task 4
  writes its own flat zip.
- `PathUtils.deleteDirectoryRecursively(path, progressInfo)`.

### Conventions (project policy — follow exactly)

- **MIT license header** on every new file (copy from any file in `api/backups/`).
- **No Swing-structure tests.** UI (Task 7) is verified by the user in the app. All non-trivial
  logic (store, snapshots, dedup, GC, locking, collector) IS tested, in `jipipe-launcher`
  (`src/test/java/org/hkijena/jipipe/api/backups/store/`). Tests that need the registry use
  `@BeforeAll static void ensureInstance() { JIPipe.ensureInstance(); }`.
- **Build commands:** after jipipe-core edits: `mvn -pl jipipe-core install -DskipTests -q`.
  Before launcher test runs: `mvn -pl jipipe-launcher -am install -DskipTests -q` (stale-SNAPSHOT
  guard). Focused: `mvn -pl jipipe-launcher test -Dtest=<Class>`.
- **Revert test side effects before committing:** `git checkout -- utils/fresh-node-slots.json
  utils/taxonomy-research/registry.json` (regenerated by registry-booting tests).
- Commit messages end with `, #1342`.
- Do NOT touch the frozen #753 paths: `JIPipeXProjectWriter`, `JIPipeProjectCrate`,
  `JIPipeXManifest`, `JIPipeXProjectReader`, `JIPipeProjectParts` (the workspace's
  `materializeFromModel` is *used*, not modified).

### Canonical on-disk layout (exact — refines spec §3.1: one lock file instead of a `locks/` dir)

```
<backupPath>/                          # JIPipeProjectBackupServiceComponent.getCurrentBackupPath()
  store/
    format.json                        # {"format-version": 1}, written on first use
    lock                               # FileLocker lock file (cross-process store lock)
    objects/<first-2-hex>/<sha256-64>  # loose objects, raw bytes
  snapshots/<snapshot-id>.json         # snapshot manifests (spec §3.2)
  files-cache/<session-id>.json        # per-project-session files cache (spec §3.4)
  <session-id>/                        # unchanged per-session dirs
    backup-info.json                   # UNCHANGED schema
    snapshots.json                     # ordered list of snapshot ids (the session index)
```

---

## Task 1 — FileLocker bounded acquisition + store core

**Goal:** the content-addressed object store with atomic writes, cross-process locking with
bounded waiting, and mark-and-sweep GC.

### 1a. Extend `FileLocker` (`utils/FileLocker.java`)

Add a bounded, *held* acquisition (the existing `acquireWriteLock()` retries forever):

```java
/**
 * Acquires the write lock, giving up after the timeout. Unlike
 * {@link #tryWriteLock()} (a probe that releases immediately), the lock is
 * HELD on success until {@link #releaseLock()}.
 *
 * @param timeoutMillis maximum time to wait
 * @return true if the lock is held
 */
public boolean acquireWriteLock(long timeoutMillis) { ... }
```

Implementation: loop like `acquireWriteLock()` but bail out (return `false`, release channel)
once `System.currentTimeMillis()` passes the deadline; 1 s retry interval (a shorter first-retry
interval, e.g. 100 ms, is fine). Do not modify the existing methods.

### 1b. `JIPipeBackupObjectStore` interface + `JIPipeLocalBackupObjectStore` impl

New package `org.hkijena.jipipe.api.backups.store`.

`JIPipeBackupObjectStore.java` (interface):

```java
public interface JIPipeBackupObjectStore {
    /** Bounded cross-process write lock. Held until releaseWriteLock(). */
    boolean acquireWriteLock(long timeoutMillis);
    void releaseWriteLock();
    /** Store a file's bytes; returns the SHA-256 hex. Existing hashes are NOT re-written. */
    String put(Path file) throws IOException;
    /** Store streamed bytes of known size; returns the SHA-256 hex. */
    String put(InputStream content, long size) throws IOException;
    boolean has(String hash);
    long sizeOf(String hash);
    /** Lock-free: objects are immutable once written. Verifies length while streaming. */
    InputStream get(String hash) throws IOException;
    /** Sum of all object file sizes. */
    long totalObjectBytes() throws IOException;
    /**
     * Mark-and-sweep: deletes object files not referenced by any snapshot
     * manifest in <backupPath>/snapshots/, plus stray temp files.
     * MUST be called while holding the write lock (the method does not
     * acquire it itself).
     */
    void garbageCollect(JIPipeProgressInfo progressInfo) throws IOException;
    Path getStoreDirectory();
}
```

`JIPipeLocalBackupObjectStore.java` — constructor `JIPipeLocalBackupObjectStore(Path backupRoot)`
(derives `backupRoot/store`, `backupRoot/snapshots`, `backupRoot/files-cache`):

- **`put`:** hash while streaming (`DigestInputStream` with `MessageDigest.getInstance("SHA-256")`)
  into a temp file `objects/<hh>/.tmp-<uuid>` in the same directory, then
  `Files.move(tmp, objects/<hh>/<hash>, StandardCopyOption.ATOMIC_MOVE)` — if the target exists,
  delete the temp instead (dedup). Hex-encode lowercase. Create `format.json` on first use
  (`{"format-version": 1}` via a small Jackson-annotated record or `ObjectNode`; refuse to operate
  if an existing `format.json` has a version > 1: `IOException("Unsupported store format version")`).
- **`get`:** wrap the object file in an `InputStream` that counts bytes and throws
  `IOException("Object <hash> is truncated")` if fewer than `sizeOf` bytes arrive; callers that
  need integrity re-hash while reading (restore does; Task 4).
- **`garbageCollect`:** parse every `<backupRoot>/snapshots/*.json` (`JIPipeBackupSnapshot` from
  Task 2 — until then, parse the `entries[].hash` arrays with a raw `JsonNode` walk so Task 1 is
  independently testable; Task 2 swaps it to the typed model), collect referenced hashes; walk
  `objects/*/*`, delete files whose name is not a referenced hash (this also sweeps leftover
  `.tmp-*` files); delete empty `<hh>` dirs. Log counts via `progressInfo`.
- **Locking:** `acquireWriteLock(timeoutMillis)` delegates to a `FileLocker` on `store/lock` with
  the new bounded overload; `releaseWriteLock()` releases. `put`/`garbageCollect` are documented
  as *caller-holds-lock* (assert by convention; do not re-acquire — FileLock re-entrancy is
  platform-dependent). `get`/`has`/`sizeOf`/`totalObjectBytes` are lock-free.

### 1c. Tests — `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/backups/store/JIPipeLocalBackupObjectStoreTest.java`

Pure IO, no registry. `@TempDir`:

1. `putGetRoundTrip` — put a file, `has` true, read back via `get`, bytes equal, hash is 64 hex chars.
2. `dedupStoresOnce` — put the same content twice (different temp files); exactly one file under
   `objects/`, both calls return the same hash.
3. `putIsAtomic` — after a successful put, no `.tmp-*` files remain under `objects/`.
4. `garbageCollectSweepsUnreferencedAndKeepsReferenced` — write two snapshot manifests to
   `snapshots/` (hand-written JSON: `{"entries":[{"path":"a","size":1,"hash":"<h1>"}]}`) referencing
   one stored object; store a second unreferenced object; hold the lock, GC; referenced object
   survives, unreferenced deleted, a stray `.tmp-x` file deleted.
5. `garbageCollectIsIdempotent` — run GC twice; same state, no exceptions.
6. `truncatedObjectFailsOnRead` — corrupt an object (truncate it); `get` throws IOException naming
   the hash.
7. `crossProcessLockIsBounded` — store A `acquireWriteLock(-)` holds; a second store instance on
   the same root with `acquireWriteLock(1500)` returns `false` within a sane wall time; after
   `releaseWriteLock()` the second store acquires. Also: `get`/`has` work while the other instance
   holds the write lock (lock-free reads).
8. `formatVersionGuard` — write `{"format-version": 99}` to `store/format.json`; a `put` throws
   with "Unsupported store format".

**Verify:** core install + focused test green. **Commit:** `feat(backups): content-addressed object store core (atomic put, dedup, GC, cross-process lock), #1342`

---

## Task 2 — Snapshot model + files cache

**Goal:** the Jackson models and the speed cache.

### `JIPipeBackupSnapshot.java` (store package)

Kebab-case `@JsonGetter`/`@JsonSetter` (mirror `JIPipeProjectBackupSessionInfo`):

```java
public class JIPipeBackupSnapshot {
    private String snapshotId;        // "snapshot-id" — UUID
    private String created;           // "created" — ISO_LOCAL_DATE_TIME string
    private String projectFormat;     // "project-format" — "jip" | "jipx"
    private String projectName;       // "project-name" — original project file name
    private List<SnapshotEntry> entries = new ArrayList<>();  // "entries", sorted by path
    public long getTotalSize() { ... }        // sum of entry sizes
    public static class SnapshotEntry {
        private String path;   // "path" — workspace-relative ('project.jip' for jip)
        private long size;     // "size"
        private String hash;   // "hash"
    }
}
```

### `JIPipeBackupFilesCache.java` (store package)

Per project session, JSON at `<backupRoot>/files-cache/<session-id>.json`:
`Map<String, CachedInfo>` (path → `{"size", "mtime", "hash"}`, kebab-case getters/setters).
API: `static JIPipeBackupFilesCache load(Path file)`, `void save(Path file)` (atomic: temp +
move), `Optional<String> hashIfUnchanged(String relativePath, long size, FileTime mtime)`,
`void record(String relativePath, long size, FileTime mtime, String hash)`.
Semantics per spec §3.4 — a hit reuses the hash without reading the file; a miss/absence is
recorded after hashing.

### Tests — `JIPipeBackupSnapshotTest.java`, `JIPipeBackupFilesCacheTest.java`

1. Snapshot JSON round-trip via `JsonUtils.saveToFile`/`readFromFile` into `@TempDir`; entries
   order preserved; `getTotalSize` correct.
2. Files cache: `hashIfUnchanged` empty on miss; `record` then hit with matching size+mtime;
   miss again after size or mtime change; `load` of a nonexistent file returns an empty cache;
   `save`→`load` round-trip.

**Verify:** focused tests green. **Commit:** `feat(backups): snapshot model and files cache, #1342`

---

## Task 3 — Snapshot creation (jip + jipx) and persistence

**Goal:** turn a live project (either format) into a stored snapshot. All logic lives in a static
operations class so it is testable without the window; the run (Task 5) only wires it.

### `JIPipeBackupStoreOperations.java` (store package)

```java
public final class JIPipeBackupStoreOperations {
    /**
     * .jip: serialize the model via the stream writer (no authors merge —
     * frozen asymmetry) and store it as a single 'project.jip' entry.
     */
    public static JIPipeBackupSnapshot createJipSnapshot(
            JIPipeBackupObjectStore store, JIPipeProject project,
            String projectName, JIPipeProgressInfo progressInfo) throws IOException;

    /**
     * .jipx: under the workspace monitor, materializeFromModel (captures unsaved
     * edits; payload-preserving), then hash+put every workspace file (files cache
     * consulted first — unchanged files are not re-read). Snapshot entries list
     * every file sorted by path.
     */
    public static JIPipeBackupSnapshot createJipxSnapshot(
            JIPipeBackupObjectStore store, JIPipeProject project,
            JIPipeBackupFilesCache filesCache, String projectName,
            JIPipeProgressInfo progressInfo) throws IOException;

    /** Persist: snapshots/<id>.json + append to <session>/snapshots.json + backup-info.json. */
    public static void persistSnapshot(
            JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
            Path sessionDirectory, JIPipeProjectBackupSessionInfo sessionInfo) throws IOException;

    /** Remove a snapshot: delete snapshots/<id>.json + remove from <session>/snapshots.json. */
    public static void deleteSnapshot(Path backupRoot, String snapshotId, Path sessionDirectory,
            JIPipeProgressInfo progressInfo) throws IOException;
}
```

Semantics (spec §3.6):

- `createJipSnapshot`: hash+put via `store.put(InputStream)` fed by a `ByteArrayOutputStream`
  from `project.saveProject(OutputStream)` (memory is fine — `.jip` models are MBs, not GBs).
  One entry: path `"project.jip"`.
- `createJipxSnapshot`: `synchronized (workspace) { workspace.materializeFromModel(pi); ... }`
  and hash the files **inside** the same monitor (a concurrent save must not mutate a part file
  mid-hash). Walk `workspace.getBaseDirectory()` (`Files.walk`, regular files, relative paths with
  `/` separators, sorted). For each: `filesCache.hashIfUnchanged(...)` → hit: reuse hash;
  miss: `store.put(file)` + `filesCache.record(...)`. Entry = `{relativePath, size, hash}`.
  `snapshotId = UUID.randomUUID()`, `created = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME)`.
- `persistSnapshot`: callers (Task 5's run) hold the store lock across create+persist — document
  this on the methods ("MUST be called while holding the store write lock"). Write
  `snapshots/<id>.json` last (atomic: temp+move via a small private helper or `JsonUtils.saveToFile`
  to a temp then move). `snapshots.json` is a `List<String>` rewritten atomically (append the id).
  `backup-info.json` via `JsonUtils.saveToFile(info, sessionDir/backup-info.json)` (schema unchanged).
- `deleteSnapshot`: the inverse (used by thinning/deletion in Task 6).

### Tests — `JIPipeBackupSnapshotCreationTest.java` (needs `JIPipe.ensureInstance()`)

`@TempDir` + real `JIPipeProject` instances:

1. `jipSnapshotStoresSingleEntry` — new project, snapshot, `has(hash)` true, entry count 1,
   path `project.jip`, format `jip`; `persistSnapshot` wrote `snapshots/<id>.json` and the session
   index contains the id.
2. `jipxSnapshotDedupsPayloadAcrossSaves` — THE core test (spec §5.3): create a project, write a
   `.jipx` via `new JIPipeXProjectWriter().write(project, target, SILENT)` (workspace binds),
   open it via `JIPipeProject.loadProject(target, ...)` (workspace extracts), add a multi-MB
   payload file to `<workspace>/data/`, snapshot A (store the payload once), **mutate the
   in-memory project** (e.g. rename the project — changes the `project.json` part), snapshot B →
   assert: at least one part-file entry hash differs between A and B (the mutation must be
   proven), `totalObjectBytes()` grew by only part-sized bytes (≪ payload size; compute expected
   = bytes of changed part files), the payload entry hash is identical in both snapshots, and
   the files cache file for the session exists. `close()` both projects afterwards (temp-base
   hygiene, #753 audit lesson).
3. `jipxSnapshotCapturesUnsavedEdits` — open a `.jipx`, rename the project (in memory, do NOT
   save), snapshot, assert the `project.json` part entry hash differs from the pre-edit snapshot's.
4. `deleteSnapshotRemovesManifestAndIndexEntry` — create + persist, delete, manifest gone,
   index empty; the object remains (GC is a separate concern).

**Verify:** core install, SNAPSHOT pre-step, focused tests green. **Commit:** `feat(backups): snapshot creation for jip and jipx with files cache and dedup, #1342`

---

## Task 4 — Restore + verify

**Goal:** assemble any snapshot back into a project file, integrity-checked end to end.

### Add to `JIPipeBackupStoreOperations`:

```java
/**
 * Assemble a snapshot into a project file at the user-chosen path.
 * .jip: stream the single object to the target (hash re-verified while copying).
 * .jipx: assemble all entries into a temp dir (each object re-hashed while copying —
 * a mismatch aborts with a clear error naming path+hash), then zip the directory
 * CONTENTS flat (container entries at the zip root, sorted by path) to the target.
 */
public static void restoreSnapshot(JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
        Path targetFile, JIPipeProgressInfo progressInfo) throws IOException;

/** Verify every object of every snapshot without writing anything (dry run). */
public static JIPipeBackupVerificationReport verifyAll(JIPipeBackupObjectStore store,
        JIPipeProgressInfo progressInfo) throws IOException;
```

- The flat zip: private helper `zipDirectoryContentsFlat(Path dir, Path target)` — `ZipOutputStream`,
  walk regular files sorted by relative path, entries at the root (do NOT use
  `ArchiveUtils.compressDirectoryToZip` — it wraps in a root folder).
- Missing object during restore → `IOException("Backup object <hash> for <path> is missing — the
  backup store is damaged")`; hash mismatch → `IOException("Backup object <hash> does not match
  its checksum — the backup store is damaged")`. Both abort the restore before anything is
  written to the target (assemble into the temp dir first, zip last).
- `JIPipeBackupVerificationReport` (store package, plain class): `int snapshotsChecked`,
  `int objectsChecked`, `long bytesChecked`, `List<String> problems`. `verifyAll` enumerates
  `snapshots/*.json`, re-hashes each referenced object, accumulates the report.

### Tests — `JIPipeBackupRestoreTest.java` (registry)

1. `jipSnapshotRestoresAndLoads` — snapshot a project (with ≥1 node), restore to `@TempDir`,
   `JIPipeProject.loadProject` succeeds, node count equals the original.
2. `jipxSnapshotWithPayloadRestoresAndLoads` — continue the Task-3 scenario style: build
   container + payload + snapshot, restore, load, assert the payload round-trips only through
   the FILE (workspace of the loaded project contains the payload with identical bytes) and
   `zipContains(target, "data/<payload>")`.
3. `restoreDetectsCorruption` — snapshot, corrupt one object file on disk (flip bytes, keep size),
   restore throws with "does not match its checksum"; a missing object (delete one) throws
   "missing".
4. `verifyAllReportsProblems` — same corruption setup, `verifyAll` returns a report with the
   problem listed and the checked counts > 0; on a healthy store, `problems` is empty.

**Verify:** focused tests green. **Commit:** `feat(backups): snapshot restore with verify-on-read and store verification, #1342`

---

## Task 5 — Backup run rewrite + service wiring

**Goal:** replace the full-copy inner class with the store-backed run.

- **Delete the inner `CreateBackupRun`** from `JIPipeProjectBackupServiceComponent` and create
  the top-level class `org.hkijena.jipipe.api.backups.CreateBackupRun` (same simple name — the
  `cancelIf` instanceof check keeps working):
  ```java
  public class CreateBackupRun extends DefaultJIPipeRunnable {
      final JIPipeDesktopProjectWindow window;   // package-private (cancelIf matches on it)
      private final String projectName;
      // getTaskLabel() = "Creating backup"
      // run():
      //   1. store = JIPipe.getInstance().getProjectBackup().getStore()
      //   2. if (!store.acquireWriteLock(60_000)) { log warning "another JIPipe instance is
      //      backing up — skipping this cycle"; status-bar note; return; }
      //   3. try {
      //        sessionDir = getCurrentBackupPath()/<window.getSessionId()>; createDirectories
      //        projectIsJipx = PathUtils.isJipxPath(window.getProjectSavePath())
      //        cache = JIPipeBackupFilesCache.load(files-cache/<sessionId>.json)
      //        snapshot = projectIsJipx
      //            ? createJipxSnapshot(store, project, cache, name, pi)
      //            : createJipSnapshot(store, project, name, pi)
      //        cache.save(...)
      //        persistSnapshot(store, snapshot, sessionDir, sessionInfo)
      //        invokeLater: status bar "Saved backup <snapshot time>" (+ keep the existing tone)
      //      } finally { store.releaseWriteLock(); }
      //   4. IOException → invokeLater status-bar "Failed to save backup: ..." + IJ.handleException
  }
  ```
  Note the `.jip`/`.jipx` decision: `PathUtils.isJipxPath(window.getProjectSavePath())`; a project
  with `null` save path is `.jip` (single-stream entry — the model is what matters).
- **Service component:** add `public JIPipeBackupObjectStore getStore()` →
  `new JIPipeLocalBackupObjectStore(getCurrentBackupPath())` (fresh instance per call — the
  backup path is settings-changeable; the store is stateless beyond paths). `backup(window)`
  and the timer are otherwise unchanged.

No new tests (the run is Swing-adjacent wiring; the logic is covered by Tasks 3–4). **Verify:**
compile + existing suites still green. **Commit:** `feat(backups): store-backed CreateBackupRun with bounded cross-instance locking, #1342`

---

## Task 6 — Collector, thinning, GC integration, settings

**Goal:** snapshots become first-class backup items everywhere; deletion paths clean up manifests
and objects; GC runs after thinning.

### 6a. `JIPipeProjectBackupItem` extension (additive)

New fields + getters/setters: `String snapshotId`, `Path snapshotManifestPath`,
`String projectFormat` (`"jip"`/`"jipx"`), `long totalSizeBytes`. `boolean isSnapshot()` =
`snapshotId != null`. `projectPath` stays null for snapshot items (legacy file items keep it).
Add `void delete(JIPipeProgressInfo progressInfo)`: file item → `Files.delete(projectPath)`;
snapshot item → `JIPipeBackupStoreOperations.deleteSnapshot(...)` (backupRoot derived from
`snapshotManifestPath.getParent().getParent()`).

### 6b. `CollectBackupsRun`

In the session-dir branch, additionally read `snapshots.json` (if present): for each id, load
`snapshots/<id>.json` → item with `backupTime = LocalDateTime.parse(created)`,
`originalProjectPath`/`sessionId` from the session info, `totalSizeBytes = snapshot.getTotalSize()`,
`projectFormat`, `snapshotId`, `snapshotManifestPath`. Keep the existing file-scan branch
(legacy file items in the same session dir still work). Sort order per collection unchanged
(newest first — `backupTime` from `created`).

### 6c. `PruneBackupsRun`

Fix `sessionHasBackups`: a session also has backups if `snapshots.json` exists and is non-empty
(read the `List<String>`; a JSON parse failure counts as empty). In the session-deletion path,
before deleting a session dir, delete each snapshot manifest it references (so GC can reclaim
the objects — otherwise orphaned manifests keep objects alive forever).

### 6d. `ThinBackupsRun` + `DeleteOldBackupsByAgeRun`

Replace `Files.delete(item.getProjectPath())` with `item.delete(progressInfo)` (both classes).
At the end of `ThinBackupsRun.run()`, after deletions, if deletions occurred and
`settings.getCleanupSettings().isEnableAutoGc()`: best-effort GC —
`store.acquireWriteLock(60_000)` → `garbageCollect(pi)` → release; on lock timeout log a warning
and skip (never block the cleanup queue). Then, if `store-size-limit` is enabled and
`totalObjectBytes()` exceeds it: delete the oldest snapshot items (across all sessions, by
`backupTime`) until under the limit (each via `item.delete`), then GC again. Log a summary.

### 6e. Panel deletions (`JIPipeDesktopBackupManagerPanel`)

`deleteSelection` (and any other `Files.delete(item...)` paths in the panel): route through
`item.delete(...)`. Collection deletion = delete every item in the collection. Read the file
first; keep the interaction flow unchanged.

### 6f. Settings (`JIPipeBackupApplicationSettings.CleanupSettings`)

Add two knobs (kebab-case IDs, `uiOrder` below the existing ones, neutral wording for biologists):
- `enable-auto-gc` — name "Automatically free unused backup data", default `true`: "If enabled, data no longer used by any backup is deleted after cleanup runs."
- `store-size-limit` — `OptionalIntegerParameter(false, 0)` disabled by default — name
  "Maximum backup storage (MB)": "If enabled, the oldest backups are removed when the backup data exceeds this size."

### 6g. Tests — `JIPipeBackupIntegrationTest.java` (registry)

1. `collectListsSnapshotAndLegacyItems` — build a backup root by hand: session dir with
   `backup-info.json`, `snapshots.json` (one persisted snapshot from Task-3 ops) + one legacy
   `.jip` file; a root-level legacy `.jip`; run `CollectBackupsRun` (set `progressInfo`); assert
   the collection contains one snapshot item (sizes/format set) and the legacy items are intact.
2. `thinningDeletesSnapshotsAndGcFreesObjects` — persist 3 snapshots of the same project
   (identical content — dedup makes them share objects); `ThinBackupsRun(false)` with keep-rules
   that delete 2; assert: 2 manifests gone, session index has 1 entry, GC freed nothing (objects
   shared); then delete the last → GC removes the object.
3. `pruneKeepsSnapshotOnlySessions` — session dir with `backup-info.json` + `snapshots.json`
   (1 snapshot) and NO backup files → `pruneEmptySessions` does NOT delete it; after
   `deleteSnapshot`, it IS deleted.
4. `sizeLimitDropsOldest` — 3 snapshots with distinct content (different object sets), size
   limit set below the total, thin → oldest dropped until under the limit (assert by remaining
   manifest ids).

**Verify:** core install, SNAPSHOT pre-step, focused + full suites. **Commit:** `feat(backups): snapshot items in collector/thinning/pruning with GC integration and settings, #1342`

---

## Task 7 — Manager UI (verified by the user in the app — no tests)

`JIPipeDesktopBackupManagerPanel` (read the whole file first):

- **Restore** for snapshot items: item properties panel gains, when `isSnapshot()`, format +
  size (`StringUtils.formatSize(totalSizeBytes)`) + entry count; the Restore button calls
  `JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, chosenPath, pi)` (load the
  snapshot from `snapshotManifestPath`) inside a `runInDialog` on the backup queue (modal progress
  is correct here — the user explicitly asked to restore), then the existing
  `openProject(path, true)` flow. File items keep the `Files.copy` path. Extend the save-file
  chooser filter list by the item's format if trivial, else keep both filters as today.
- **Verify all backups** button (sidebar, next to the cleanup buttons): enqueues
  `VerifyBackupsRun` (new small `DefaultJIPipeRunnable` wrapping `verifyAll`) on the backup
  queue, **non-modal**, result via status bar + run-logs marked read (`isLogSilent()` true —
  the #753 pattern; check `JIPipeDesktopRunnableLogsCollection` wiring as done for #753:
  the collection tracks the global queue only, so push manually with
  `pushToLog(run, success)`).
- **Store stats footer** (bottom of the sidebar): total unique data
  (`formatSize(totalObjectBytes())`) and total backup size (sum of item `totalSizeBytes`) —
  label them "Backup data: X (unique data: Y)" — neutral, no jargon.
- Snapshot tree rows: append the size to the time label (e.g. `2026-10-24T15:04 — 2.1 GB`).

**Verify:** compile + full suites green. **Commit:** `feat(desktop): backup manager restore/verify UI and store stats, #1342`

---

## Task 8 — Final verification + record

1. `mvn -pl jipipe-launcher -am install -DskipTests -q` then
   `mvn -pl jipipe-core,jipipe-launcher test` — expect all green (core ~514+, launcher ~105+).
2. JIPX done-gate guard (writer untouched, must stay 358/358):
   `mvn -pl jipipe-launcher test -Dtest=JipxRoundTripFixpointTest -Djipipe.jipx.roundtrip.corpus=/data/JIPipe -Djipipe.jipx.roundtrip.report=/data/JIPipe/.roundtrip/backup-store-guard.json`
3. Capacity spot-check (evidence for the record, not a committed test): a synthetic project with
   a ~500 MB payload under `data/` — backup 1 (cold) vs. backup 2 (unchanged) vs. backup 3
   (parts mutated): record bytes written and wall times; expect backup 2 ≈ 0 new bytes.
4. Revert `utils/fresh-node-slots.json` + `utils/taxonomy-research/registry.json`.
5. Write `docs/superpowers/plans/2026-10-24-backup-content-addressed-store-verification.md`:
   suites, done-gate numbers, capacity numbers, deviations from the plan (if any).
   **Commit:** `docs: backup store verification record (#1342)`
6. Push `1342-new-backup-system` to origin (no MR until the user says the feature is finished).
