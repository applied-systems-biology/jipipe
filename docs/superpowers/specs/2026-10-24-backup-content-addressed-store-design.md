# Design: New Backup System — Content-Addressed Store

**Work item:** [RGerst/jipipe#1342](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1342)
**Branch:** `1342-new-backup-system` (off `master`)
**Prerequisite:** #753 MR !174 merged (the JIPX project format — this design builds on its workspace model). Until then the JIPX-dependent sections are forward-references; the store core is independent.
**Date:** 2026-10-24

## 1. Problem

The current backup system saves a **full copy of the project file** every 7 minutes
(`CreateBackupRun` → `saveProject` → one file per backup in `backups/<session>/`).
Cascading thinning (keep ~1/h, 1/d, 4/w, 4/m, 12/y, 60-day cap) limits the *count*,
but every retained backup is a full copy.

With the JIPX project crate (#753), projects will carry **payload data**
(microscopy images, tables) in the workspace — multi-GB containers are foreseeable.
Consequences for the current system:

- **Space:** ~8 backups alive at any time (the <1 h rule keeps everything recent) →
  ~8× the payload duplicated, ~10–100 GB/day steady state for a multi-GB project,
  all in the user profile directory by default.
- **Speed:** every 7 minutes the whole payload is re-zipped and re-parsed by the
  writer's self-verify.
- **Locking:** backups run through the same `synchronized(workspace)` as manual
  saves — a long backup blocks Ctrl+S.

**Goal:** backups stay small and fast no matter how much data a project carries —
each unique byte is stored **once**, ever; snapshots reference bytes; restore
rebuilds any snapshot on demand.

## 2. Research & rejected alternatives

### 2.1 Library survey (Java)

| Candidate | License | Verdict |
|---|---|---|
| **FastCDC4J** (Maven Central, Java 14+) | GPL-3.0 | Best-in-class CDC API, but GPLv3 is viral; JIPipe is MIT and ships in bundles. **Rejected.** |
| **Farley-Chen/fastcdc-java** | MIT | Real FastCDC port; 4 stars, single contributor, low activity. A bet on maintenance, not a foundation. |
| **hashstore-java** (DataONE) | Apache-2 | Maintained CAS object store, but server-oriented (pid/cid reference files, tagging lifecycle) — more concepts than an embedded desktop store needs. |
| **JGit as a CAS** | EPL-2.0 | JGit's object database *is* a content-addressed store, but pulling in git's pack/reachability/GC model and a multi-MB dependency to solve a few-hundred-lines problem is disproportionate. |

**Conclusion: in-house store.** At our scale (whole-file granularity, local disk,
single user), a content-addressed store is a small, well-understood piece of code —
and we own the format (versioned, upgradable), which the user explicitly wanted
("future-proof, avoid re-doing the system with migrations").

### 2.2 Granularity: whole-file vs. CDC chunking

The restic/borg design literature is clear that content-defined chunking (CDC)
pays off when **large single files are modified in place** (VM images, databases).
JIPipe's payload is microscopy data — images and tables that are **added and
removed, rarely mutated byte-by-byte**. For that access pattern, **whole-file
SHA-256 addressing captures essentially all of the deduplication benefit** with
zero algorithmic risk (no rolling-hash security concerns, no chunk-boundary
stability reasoning, ~1 GB/s hash throughput).

The store format is **CDC-compatible by construction**: an object's content is
opaque to the store. A future version can store large files as a *chunk-manifest
object* (a small object listing chunk hashes) pointing at chunk objects — the same
architecture as restic's own evolution. No store-layout change, no data migration.

### 2.3 Scope: one uniform system, not two

Deliberation: keep plain-file backups for legacy `.jip` (small, human-inspectable,
directly restorable) and store-backed backups for `.jipx` only — vs. **one uniform
store-backed system for all projects** (a `.jip` backup = a snapshot with a single
whole-file entry).

**Decision: uniform.** Rationale:
- A `.jip` that changes rarely is *also* served by dedup: after the first backup,
  an unchanged `.jip` costs zero additional bytes.
- One collect / thin / GC / restore / verify code path, one UI, one mental model.
- The human-inspectability argument is weak: backups are a safety net, not a
  deliverable — if a user wants a shareable copy they *save* the project.

## 3. Design

### 3.1 On-disk layout

Everything under the existing (user-configurable) backup path
(`JIPipeProjectBackupServiceComponent.getCurrentBackupPath()`, default
`<jipipe user dir>/backups`). Versioned, self-describing:

```
<backupPath>/
  store/
    format.json              { "format-version": 1 }
    objects/
      <first 2 hex>/
        <sha256 hex, 64 chars>     # loose object = raw bytes, nothing else
    locks/                     # FileLocker instances (existing utility)
  files-cache/
    <store-session-id>.json    # path -> {size, mtime, hash} (speed, §3.4)
  snapshots/
    <snapshot-id>.json         # snapshot manifest, §3.2
  <session-id>/                # unchanged per-session directories
    backup-info.json           # UNCHANGED schema (project-session-id,
                               # project-storage-path, last-backup-date-time)
    snapshots.json             # ordered list of snapshot ids for this session
```

- **Objects** are stored once by SHA-256 of their exact bytes. No compression in v1
  (payload is usually TIFF/PNG — already compressed; the `.jipx` container keeps
  ZIP compression for the file itself). No encryption (the backup path is local
  user data, same trust level as today — and encrypting would break dedup unless
  done as convergent encryption, which is out of scope).
- **Object writes are atomic:** write to a temp file in the same directory,
  `Files.move(..., ATOMIC_MOVE)`. A crash leaves at worst an orphan temp file
  (removed by GC) — never a truncated object.
- **Snapshot writes are last:** all referenced objects exist before the
  `<snapshot-id>.json` is written (atomic rename). A crash mid-backup leaves
  orphan objects, which GC sweeps — never a snapshot referencing missing objects.
- The per-session `backup-info.json` schema is **unchanged**, so the existing
  collector/manager keep working; `snapshots.json` is the new source of the
  item list.

### 3.2 Snapshot model

`JIPipeBackupSnapshot` (new class, Jackson-mapped), `<snapshot-id>.json`:

```json
{
  "snapshot-id": "<uuid>",
  "created": "<ISO-8601 local date-time>",
  "project-format": "jip | jipx",
  "project-name": "<original project file name>",
  "entries": [
    { "path": "project/graph.json", "size": 12345, "hash": "<sha256>" },
    { "path": "data/scan001.tiff",  "size": 2147483648, "hash": "<sha256>" }
  ]
}
```

- **`.jip` backup:** exactly one entry — the whole file (`path` = `"project.jip"`
  by convention; the path is metadata only, the entry count of 1 + the format tag
  drive restore).
- **`.jipx` backup:** one entry per workspace file — the parts, the crate files,
  `jipx.json`, and **every payload** under `data/`/`graphs/`/`history/`. The
  workspace directory is exactly the container layout, which is what makes restore
  straightforward (§3.5).
- Snapshot id is a UUID (no content derivation — the same bytes in two different
  backup times are two snapshots; objects are still shared).
- `entries` sorted by path for deterministic output.

### 3.3 Store API

`JIPipeBackupObjectStore` (new class, one responsibility, final, testable without
GUI):

```java
public interface JIPipeBackupObjectStore {
    /** Store bytes; returns the hash. A hash that already exists is NOT re-written. */
    String put(InputStream content, long size) throws IOException;
    /** Stream an object by hash (verifies length on read). */
    InputStream get(String hash) throws IOException;
    boolean has(String hash);
    long sizeOf(String hash);
    /** Total bytes used by live objects (for UI stats). */
    long totalObjectBytes();
    /**
     * Mark-and-sweep garbage collection: deletes objects not referenced by any
     * snapshot file in the store root. Idempotent, crash-safe (deleting a live
     * object is prevented by the reference set; deleting twice is harmless).
     */
    void garbageCollect(JIPipeProgressInfo progressInfo) throws IOException;
}
```

- A single implementation (`JIPipeLocalBackupObjectStore`) over the directory
  layout above. The interface leaves the door open for a remote store later
  (the VFS-seam principle from the JIPX design, §5.4).
- **Concurrency:** two levels, mirroring the project temp-base discipline:
  - *Intra-process:* all backup runs are serialized by the existing single
    `"Backups"` executor queue (timer + manual) — one backup at a time per
    JIPipe instance, per process.
  - *Cross-process (multiple JIPipe instances share the backup path):* the
    store directory carries a `FileLocker` write lock (the existing
    project-temp-base utility — OS-level file locks are cross-process) held
    for the whole store *mutation*: object puts, snapshot write, files-cache
    update, GC. Snapshot reads and `get(hash)` streams are lock-free (content
    addressing makes object reads safe: an object file, once written, never
    changes).
    Lock-order discipline: workspace lock first (part materialization), store
    lock second — never inverted.
  - *GC is cross-process safe* because the store write lock is held for the
    full mark (enumerate `snapshots/*.json`) and sweep (delete unreferenced
    objects) — a concurrent instance's snapshot cannot appear mid-sweep, and
    a running backup's snapshot is written only after its objects, under the
    same lock.
  - *Livelock avoidance:* `acquireWriteLock()` blocks forever on contention;
    store mutations use **bounded waiting** (`tryWriteLock` + retry with a
    cap, e.g. 60 s) and skip the cycle with a logged warning on timeout — a
    stuck second instance must never freeze the first instance's save path.
    The backup timer simply retries next cycle.
- **GC is safe because** it only runs when the store lock is held and enumerates
  the current set of `snapshots/*.json` — a concurrent backup holds the store lock
  for its `put`s, and its snapshot file only becomes visible after the puts.

### 3.4 Files cache (the speed story)

Hashing a 2 GB payload that hasn't changed is wasted work (SHA-256 of a big file
is ~2 s — fine once, pointless 20×/day). A per-store `files-cache/*.json` maps
workspace-relative path → `{size, mtime, hash}`. At backup time:

- entry matches (size + mtime) → reuse the cached hash, **no read of the file**;
- mismatch or missing → hash the file, update the cache.

This is *safe* where the NAS-mtime lesson doesn't apply: the workspace files are
**local, JIPipe-managed** files (not user-copied project files). Worst case of a
stale cache hit (file changed with mtime+size preserved — practically impossible
for locally written files) is a *wrong hash for identical-looking metadata*, which
cannot happen because the hash is only reused when size+mtime match and the file
has not been re-materialized (materialization updates mtime). Even in the
absurd case, restore re-verifies hashes (§3.5), so a mismatch surfaces as a
clear "backup integrity error", never a silent corrupt restore.

### 3.5 Restore

Restore a snapshot = rebuild the project file from objects, **verifying every
hash while reading**:

- **`.jip` snapshot:** stream the single object to `<target>.jip` (user-chosen
  path, same chooser as today) → `openProject`.
- **`.jipx` snapshot:** reassemble the workspace directory into a temp folder
  (objects → files, re-hashing each entry and failing with a clear error on
  mismatch) → zip it into `<target>.jipx` (the JIPX writer's zip routine,
  entry order per #753) → `openProject` (the reader's own self-verify runs
  during open).

Restore is thus *integrity-checked end to end*: store read hash → assembled
container → reader self-verify → full load. A "Verify all backups" action
(manager panel) runs the same assembly in dry-run mode (hash-check only, no zip)
over every snapshot — a trust feature aimed squarely at the biologist user:
"your backups were checked, N GB in M unique objects".

### 3.6 Backup flow (replacing `CreateBackupRun`)

`JIPipeProjectBackupRun` (rewritten):

1. **`.jip` project:** serialize the model with the **stream writer variant**
   (`JIPipeLegacyProjectWriter.write(project, OutputStream)` — the frozen
   asymmetry: stream writes do NOT merge authors) and feed the bytes to
   `store.put`. One entry. No temp file, no model mutation.
2. **`.jipx` project:** capture the workspace in its current state:
   (a) `workspace.materializeFromModel(...)` under the workspace lock —
   refreshes the 9 part files from the in-memory model (so **unsaved edits are
   backed up**, like today) and is payload-preserving; then
   (b) for each workspace file, consult the files cache → `store.put` only the
   changed ones. Snapshot entries = the manifest.
   Deliberate behavior differences vs. the current `saveProject`-based backup
   (both more predictable): the backup does NOT apply the authors merge (that
   stays a save-only side effect), and the crate files in the snapshot are the
   ones from the last real save (the crate is a derived artifact, regenerated
   by the next save — the reader only requires it to parse, not to be fresh).
3. Write `<snapshot-id>.json` (last, atomic), append to `<session>/snapshots.json`
   (atomic rewrite), update `backup-info.json`'s `last-backup-date-time`.
4. Release locks. Push to the run-logs marked-read (the #753 pattern).

**Consequence:** the `.jipx` backup no longer zips anything and no longer builds
the crate or self-verifies — it's a materialize + hash pass. The blocks-Ctrl+S
problem of the status-quo disappears for the dominant case (unchanged payload:
near-instant; changed payload: part re-serialization + hash only).

### 3.7 Thinning & GC integration

- `CollectBackupsRun`: session items come from `snapshots.json` (new) *plus* the
  existing plain `.jip` legacy support (old backups created before this feature
  remain listable/restorable as today — **no migration**).
- `ThinBackupsRun` / `DeleteOldBackupsByAgeRun`: unchanged cascading rules, but
  an item is a snapshot; deleting an item deletes its `snapshot*.json` entry from
  `snapshots.json` (+ the snapshot file). **After** the thinning pass, both runs
  trigger `store.garbageCollect()` (sweeps objects no remaining snapshot uses).
- "Create backup now" (menu) → the same `JIPipeProjectBackupRun`.
- The per-session `cancelIf` pre-empt pattern stays (a new manual backup
  cancels the timer's in-flight one for that window).

### 3.8 Settings (extend `JIPipeBackupApplicationSettings`, ID `org.hkijena.jipipe:backups`)

- **Store location** (default: `<backup path>/store` — one knob, not two: moving
  the backup path moves the store; an explicit override is not worth it in v1).
- **Auto garbage collect** (default on): GC after thinning/cleanup runs.
- **Backup size limit** (optional, default off): hard cap on total store bytes;
  when exceeded, oldest snapshots beyond the thinning floor are dropped until
  under the cap. (Protects users from unbounded growth on slow disks — the
  thinning rules are time-based, a size cap is space-based; both are legitimate.)
- Existing knobs (enable, delay, cascading keep counts, max age) unchanged.

### 3.9 UI (backup manager panel)

- The tree keeps sessions → items; an item is now a snapshot: shows project path,
  time, **snapshot size** (sum of entry sizes) and **unique contribution**
  (bytes of objects only this snapshot holds — the honest "what did this backup
  cost me" number; cheap to compute, big trust value).
- **Restore** button: same chooser flow, assembles per §3.5.
- **Verify all backups** action (non-modal run, logs to the run-logs): hash-check
  every snapshot's objects; reports "N snapshots verified, M objects, K bytes".
- **Store stats** in the panel footer: total objects / total bytes / bytes freed
  by deduplication (sum of entry sizes − unique object bytes).
- Tone per the #753 ruling: neutral, concrete numbers, no jargon ("objects"
  appears only in the stats line, phrased as "unique data").

## 4. What is explicitly NOT in v1

- **CDC chunking** (reserved: the chunk-manifest-object path, §2.2).
- **Compression** of objects (payload is pre-compressed; parts are tiny; the
  `.jipx` file itself stays ZIP-compressed).
- **Encryption / convergent encryption.**
- **Remote store** (the interface seam exists; no SFTP backend).
- **Migration** of existing plain `.jip` backups (they keep working as legacy
  items until thinned away naturally).
- **Delta/dedup across different sessions** beyond object identity (objects are
  global — cross-session dedup *happens automatically*; no feature needed).

## 5. Testing (project policy: no Swing-structure tests)

Non-trivial logic only, all in `jipipe-launcher` (the module with the full
classpath), gated tests where heavy:

1. **Store unit tests:** put/get round-trip; dedup (two puts of equal bytes → one
   file on disk, same hash); atomicity (simulate crash after temp write → no
   corrupt object, GC removes the temp); GC correctness (orphaned objects
   deleted, referenced objects survive, idempotent double-GC); hash verification
   on read (corrupt an object file → clear integrity error).
2. **Snapshot round-trip (`.jip`):** create snapshot from a fixture project save →
   thin → GC → restore → `openProject` → uuid→node-info-id mapping equals the
   original (the `FixtureProjectsLoadTest` idiom).
3. **Snapshot round-trip (`.jipx` + payload):** build a container (JIPX writer),
   open it (workspace materializes), add a multi-MB payload file to the
   `data/` directory, snapshot → mutate the pipeline (parts change, payload
   doesn't) → snapshot again → **assert exactly one new object of parts-size
   (the payload hash is reused)** → GC → restore both snapshots independently →
   both open, payload bytes identical, pipeline content per-snapshot. This is
   the *measurable* proof of the dedup property (a capacity assertion, not a
   mock).
4. **Files cache:** unchanged file → no re-read (verify via a file that is
   deleted-and-recreated with same content between snapshots — cache hit must
   still be valid; and a modified file → re-hash).
5. **Thinning integration:** cascading rules operate on snapshot items; GC runs
   after thinning and frees exactly the objects of the thinned snapshots.
6. **Concurrency:** two backup runs on the same store serialize on the store
   lock (the queue-serialization test idiom from #753); **two processes**
   (two store instances on the same directory, the cross-process lock held by
   a background process) → the second skips with a warning within the bounded
   wait (never blocks indefinitely, never writes); and the GC-under-lock
   invariant (an object referenced by a snapshot written by another instance
   is never swept).
7. **Legacy interop:** a pre-feature plain `.jip` backup in the backups root is
   still listed and restorable after the new code is in place.

## 6. Task breakdown (for the implementation plan)

1. Store core: layout, `put`/`get`/`has`/`garbageCollect`, atomic writes,
   locking, integrity check. (Independent of JIPX.)
2. Snapshot model + backup run (`.jip` path only) + collector/thin/GC
   integration + legacy interop.
3. Files cache.
4. JIPX integration: workspace snapshotting, `.jipx` restore assembly.
   *(Requires #753 merged.)*
5. Restore path + verify-all run.
6. UI: manager panel (sizes, unique contribution, verify, stats) + settings.
7. Full verification + record (capacity measurement on a synthetic
   multi-GB-payload project: backup #1 vs. backup #2–N bytes + times).

## 7. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Object-store corruption from disk failure | per-object SHA-256 naming = self-verifying path; verify-on-read at restore; "Verify all backups" action surfaces damage early. |
| **Multiple JIPipe instances sharing the backup path** | cross-process `FileLocker` write lock on the store for the whole mutation window (puts, snapshot write, cache update, GC); snapshot reads and object streams are lock-free (objects are immutable once written); bounded lock waiting with skip-and-retry (never freezes an instance); GC's mark-and-sweep runs entirely under the lock, so no instance can delete another's live objects. |
| GC race deletes a live object | store lock held for mark + sweep; snapshot visibility is last-write; GC re-enumerates references at deletion time. |
| Files cache staleness → wrong backup | §3.4 analysis: local JIPipe-managed files, verify-on-read backstop. |
| Multi-GB single payload file re-hashed every backup after it changes | SHA-256 is ~1 GB/s — a 10 GB re-hash is ~10 s every 7 min in the worst (constantly-mutating) case; acceptable. CDC is the reserved fix if real data shows otherwise. |
| Store growth on a small laptop disk | size-limit setting (§3.8) + GC after every thinning pass; stats visible in the panel. |
