# Progress System Overhaul — Design

Date: 2026-11-04
Issue: [RGerst/jipipe #1352](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1352)
Context: [#1349](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1349) (closed — root-cause report in `docs/investigation-1349/report.md`)

## Problem

`JIPipeProgressInfo` stores progress as **one flat, unbounded in-memory `StringBuilder` of
pre-rendered strings** shared by all sub-progress handles (copy-constructor), plus progress
counters and a per-line `StatusUpdatedEventEmitter` that fires **synchronously while holding
the write lock**.

Measured consequences (#1349):

| Symptom | Mechanism |
|---|---|
| Multi-minute UI freeze on verbose runs | 1 log line → 1 `SwingWorker.publish()` → 1 EDT runnable; ~22,000 lines/s saturate the EDT |
| Unbounded heap growth | `StringBuilder` capped only by heap; 6.03 M lines ≈ 736 MB |
| Lock contention | `log()` emits listener events **inside** the `StampedLock` write section |
| Per-event UI churn | `JIPipeDesktopRunnableQueueButtonMenuItem` does `setText()`/`setValue()` per event, undebounced |
| Run-end memory spike | `JIPipeDesktopRunnableLogsCollection.pushToLog` copies the whole buffer via `getLog().toString()`; `JIPipeGraphRun` writes `log.txt` from the buffer |
| No structure | log/percentage/error are all text lines; progress hierarchy exists only as `resolve()` string prefixes |
| Poor error display | raw stack traces dumped as log text |

The UI shows this as a flat monospace stream (`Storage: Register input: <uuid>`, `GC |
Set overlay`, …) that cannot express the tree-like structure of a run, has no native
percentage rendering, no detail-level control, and exports rendered text instead of data.

## Goal

A progress system where

- progress is a **tree** mirroring the run hierarchy (partitions → nodes → phases → items),
- the tree's event stream is **file-backed** (NDJSON) with a **hard-bounded heap
  footprint** regardless of record count (GB/TB-scale runs),
- percentage progress is a **native progress state** rendered as bars, not "%" text lines,
- log **detail levels** (debug/info) are recorded and filterable in the UI,
- errors are **structured records** rendered as collapsibles (no raw stack-trace dumps),
- the log viewer's export produces the **data file** (plus optional text render),
- the #1349 event pathology is structurally impossible (coalescing, no emit under lock,
  debounced consumers),
- the existing `JIPipeProgressInfo` API keeps compiling: ~1,420 call sites in 307 files are
  **not** migrated in this pass (6.0.0 allows breaking removals later; the facade keeps them
  working now).

## Non-goals

- No bulk migration of existing `progressInfo.log(...)` call sites (facade covers them;
  only known verbose-heavy sites move to `debug()`).
- No new third-party dependencies (no SQLite/H2; NDJSON is hand-rolled on Jackson).
- No remote/networked log transport; the WebSocket instrumentation reads the same store.
- No log rotation/compression of the NDJSON file (a setting may come later).
- No Swing interaction tests (user verifies UI by eye, per repo test policy).
- `log.txt` byte-compatibility: the text render keeps the familiar shape but is not a
  parseable contract anymore; the NDJSON file is the contract.

## Architecture overview

```
producer threads (algorithm nodes)
   │  JIPipeProgressInfo facade (existing API, unchanged signatures)
   │  log()/resolve()/percentage()/error()/... → O(1) enqueue
   ▼
JIPipeProgressLog (one per top-level run)
   ├─ lock-free MPSC record queue
   ├─ single async writer thread ──────►  <logs-base>/<run-uuid>.ndjson   (append-only)
   ├─ node index (light metadata per tree node)
   ├─ bounded tail ring buffer (recent records only)
   └─ coalescer ── batch notifications (≤1 per ~100 ms + final) ──► listeners
                                                        │
             ┌──────────────────────┬───────────────────┼─────────────────────┐
             ▼                      ▼                   ▼                     ▼
      desktop tree panel      queue menu item     project window        instrumentation
      (lazy read-back from    (dirty-flag         (stage status,        (AI agent/WebSocket,
       file on expand)         Timer refresh)      batched)              forwards records)
```

Consumers never receive per-record events on the hot path. Anything older than the tail is
read back from the NDJSON file on demand.

## Data model

New package `org.hkijena.jipipe.api.progress`:

- **`JIPipeProgressLog`** — the store **interface**, one instance per top-level run. Owns:
  node index, bounded tail, coalescer, batch event emitter, the record sink, and the
  read-back API (`readRecords(...)`), level-filtered iteration, and the compat text
  renderer. Static factories live on the interface and select the implementation:
  `silent()` → `SilentJIPipeProgressLog` (dummy sink — no file, no output; index + tail
  still maintained so `getLog()`/progress getters work), `stdout()` →
  `StdoutJIPipeProgressLog` (prints rendered lines, non-persistent), `create(Path, ...)`
  → `FileJIPipeProgressLog` (async writer → NDJSON file, full read-back). Shared tree
  logic (index, id allocation, tail, coalescing) lives in
  `AbstractJIPipeProgressLog`; implementations differ only in where records go and
  whether they can be read back: `readRecent(n)` works on all; `readRecords(fromLine,
  ...)` on non-persistent stores returns only what the tail holds (never throws) — the
  UI treats that as "no scroll-back".
- **`JIPipeProgressNode`** — light metadata only: numeric id, parent id, label, kind
  (`RUN`, `PARTITION`, `PHASE`, `ITEM`), state (`RUNNING`, `OK`, `FAILED`, `CANCELLED`), optional
  location URI (`node://<uuid>` today; free-form for future schemes), progress
  (value/max, indeterminate flag), first/last-record sequence range + record count,
  timestamps, activity flag (spinner). **Messages never live in nodes** — they are records
  in the file. The index stays small even for 6 M-record runs because nodes correspond to
  `resolve()` calls, not log lines.
- **Handles**: `JIPipeProgressInfo` becomes a thin handle `(store, currentNodeId)` — see
  Producer API.

### Record types (NDJSON, one JSON object per line)

Short keys; ~100–200 bytes per record. Discriminator field `t`. Timestamps `ts` in epoch
millis. Levels follow the log4j-style scale: `10`=DEBUG, `20`=INFO, `30`=WARN, `40`=ERROR.

| `t` | meaning | fields |
|---|---|---|
| `head` | file header, always line 1 | `v` (schema version, `1`), `run` (run UUID), `label`, `app` (JIPipe version), `ts` |
| `n` | node created | `id`, `p` (parent id, `0` = root), `l` (label), `k` (kind), `loc` (optional location URI, e.g. `node://<uuid>`; free-form, forward-extensible), `ts` |
| `N` | node finished | `id`, `s` (`ok`/`fail`/`cancel`), `ts` |
| `l` | log record | `id`, `lv`, `m` (message), `ts` |
| `p` | progress state | `id`, `v` (value), `mx` (max; negative = indeterminate), `ts` |
| `e` | error record | `id`, `c` (exception class), `m` (message), `st` (stack trace, optional), `ts` |
| `s` | run status change | `s` (`running`/`finished`/`failed`/`cancelled`), `ts` |
| `w` | notification | `h` (heading), `d` (description), `ts` |

Rules:

- The sequence number of a record is its **line number** (1-based, head = line 1). Readers
  tolerate a truncated final line (writer killed mid-flush) and unknown `t` values and
  unknown fields (forward compatibility).
- Node ids are store-local ints assigned by the index.
- A reader can rebuild the full tree from `n`/`N` records alone; `l`/`p`/`e` attach to nodes
  via `id`.

## Storage

### Location — never the output directory

Update/cache runs clear the output directory afterwards, so the primary store lives in
temporary scopes:

- **Project runs** → `<project temporary base>/logs/<run-uuid>.ndjson`. The project temp
  base is resolved via `JIPipeProject.getTemporaryBaseDirectory()` (honors
  `forceGlobalTempDirectory`, per-project temp setting, and temp-dir overrides), is
  lock-protected, and is **cleaned by `JIPipeProject.close()`** — project logs disappear
  when the user loads another project (accepted; readable on error while the project is
  open).
- **Global runs** (no project: AI/embedding runs, service initializers, artifact installs,
  quick runs without a project, …) → `<JIPipe temporary base>/logs/<run-uuid>.ndjson`,
  existing global temp lifecycle.
- **Output directory** keeps only the compat `log.txt` render at run end, subject to the
  existing cleanup settings — unchanged behavior.

The location is decided by a small settable property on the root progress handle
(`storageBasePath`, `Path`): `JIPipeGraphRun` sets it to the project's `logs` directory;
the desktop run queue sets it for non-graph runs executed in a project context; everything
else defaults to the global temp base. The file is created by the writer thread lazily on
first flush; a run that logs nothing creates no file.

### Writer

- Producers enqueue record objects into a lock-free MPSC queue and return immediately —
  **no I/O, no listener calls, no lock** on the producer path.
- One writer thread per store drains the queue, encodes JSON, and flushes the
  `BufferedWriter` in batches (≥ 64 KB or ≥ 250 ms, whichever first; final flush at run
  end before the `s:finished` record).
- `SILENT` routes to `SilentJIPipeProgressLog` (dummy sink: records discarded, no file, no
  stdout; index + tail maintained). `STDOUT` routes to `StdoutJIPipeProgressLog` (renders
  each record to `System.out`, keeps no file).

### Bounded memory

- **Tail ring buffer**: last N records kept in memory for live rendering (default 8,192
  records **or** ~8 MB of message text, whichever binds first; user-setting). Older records
  exist only in the file.
- **Node index**: one light object per tree node (metadata + record range). Node counts
  stay in the thousands even for huge runs.
- Scroll-back / node expansion reads record pages from the file on demand
  (`readRecords(fromLine, limit)` + per-node record ranges from the index).

## Producer API

`JIPipeProgressInfo` keeps its public surface. Each instance is a handle
`(store, currentNodeId)`. Mapping:

| Existing member | New behavior |
|---|---|
| `log(String)` | enqueue `l` record (INFO) on current node |
| `log(Throwable)` | enqueue `e` record (class + message + stack) |
| `error(String)` / `warn(String)` | `l` record at ERROR / WARN (text render keeps `[ERROR] `/`[WARNING] ` prefixes) |
| `aggressiveError/Warn(String...)` | several `l` records; compat shape preserved |
| `resolve(String)` / `resolve(String,int,int)` | create child node (label = text); return handle on it |
| `resolveAndLog(...)` | `resolve(...)`; the node row *is* the rendered line (no duplicate record) |
| `resolveAndLogForEach(...)` | per item: `resolve` + delegate; semantics unchanged |
| `percentage(String)` | child node; returns `JIPipePercentageProgressInfo` writing `p` records |
| `incrementProgress/addProgress/setProgress/addMaxProgress/setProgress(int,int)` | update node counters + enqueue `p` record |
| `getProgress/getMaxProgress` | read node counters |
| `detachProgress()` | child node flagged detached (rendering: `SUB` context) |
| `getLog()` | compat `CharSequence` view rendering the tail (see `getLog()` callers below) |
| `clearLog()` | clears the in-memory tail view only (file untouched) |
| `cancel/isCancelled/getCancelReason/cancellationCheck` | unchanged |
| `getNotifications/setNotifications` | unchanged; notifications additionally recorded as `w` |
| `isLogToStdOut/setLogToStdOut` | additionally print rendered lines to stdout |
| `isWithSpinner/setWithSpinner` | activity hint on the node (UI spinner; text render keeps spinner chars) |
| `getStatusUpdatedEventEmitter/setStatusUpdatedEventEmitter` | **removed** (breaking, 6.0.0) — replaced by the store's batch emitter (all 8 subscriber files are internal and rewritten) |
| `getLog()` return type | **`StringBuilder` → `CharSequence`** (breaking, 6.0.0); all 9 caller files are internal and rewritten |

New first-class API (for new code and the verbose-site migration):

- `debug(String)` — `l` record at DEBUG.
- `log(JIPipeLogLevel, String)` — explicit level.
- `beginNode(String label)` / `endNode()` — explicit node lifecycle for code that wants
  tree structure without `resolve()` scoping.
- `JIPipePercentageProgressInfo` is reimplemented on `p` records; its throttle semantics
  (notify interval, 50 %/100 % always) are preserved.

**Verbose-site migration (this pass):** known per-item log storms move to `debug()` —
starting with `FilterRoi2dByOverlapAlgorithm`'s per-(candidate × filter) logging (the #1349
trigger), plus a short audit of other `Verbose` options. Their text is preserved at DEBUG
level instead of being deleted.

## Event path

- **Coalescer contract:** the store emits a `ProgressBatchEvent` to listeners at most once
  per ~100 ms (setting) summarizing the delta (new/changed node ids, record counts, tail
  range), **plus a guaranteed final batch** at run end. A burst of any size produces at
  most `⌈duration/interval⌉ + 1` notifications.
- **`JIPipeRunnableWorker`** stops re-emitting per line; it forwards coalesced batches.
  The per-line `StatusUpdatedEvent` path and `JIPipeRunnable.ProgressEvent` are removed.
- **All internal subscribers rewritten onto the batch stream** (8 files):
  `JIPipeDesktopProgressLoggerPanel`, `JIPipeDesktopProjectWindow`, `JIPipeRunAlgorithmCommand`,
  `InstrumentationRunEngine`, `JIPipeRunnableWorker`, `JIPipeServiceDefaultInitializer`,
  `JIPipeAIServiceComponent`, and the queue UI (below).
- **Queue menu items** (`JIPipeDesktopRunnableQueueButtonMenuItem`, compact variant) switch
  from per-event `setText()`/`setValue()` to a dirty flag drained by a Swing `Timer`
  (≤ 2 updates/s) reading node state from the store.

## Desktop UI

### Design language (from the approved mockup `ui/v2-progress-panel.html`)

- **Flat list of rows — no partition grouping.** Node rows (`Find particles 2D [uuid]`)
  and phase rows (`Preparing`, `Postprocessing`) sit directly in the list with an
  expander, state icon (running spinner / done / failed / cancelled) and, when the node
  belongs to a partition, a small dim **partition chip** (`P0`). Partition nodes are
  recorded in the data as `PARTITION` kind; the renderer **squashes** them into the chip
  instead of spending a tree level on them (partitions are a rarely used feature and
  become even less prominent with #1212 — prominence is a UI decision, not data).
- **Native inline progress bars** on rows with determinate progress: `Data rows | 5/200 |
  Particle detection` renders a real bar (`5/200 (2%)`); indeterminate nodes show a
  spinner; failed rows' bars turn red.
- **`+N` badge = hidden descendants, shown only while the row is collapsed** (child nodes
  + detail records); it disappears on expansion.
- **Collapsed by default:** a node's log records are not dumped; expanding a node shows
  its records (paged). Millions of records on one node are safe: the detail view reads
  the last page (default 1,000 records) from the file and pages on demand ("Show 4 older
  records from run data…").
- **Detail-level filter** replaced by a **toggle-button group** in the panel header:
  `[debug | info | warning (count) | error (count)]` — each button toggles its category's
  visibility and carries a live per-category record count (glanceable volume). Buckets:
  debug = `l`@DEBUG, info = `l`@INFO, warning = `l`@WARN **+ notification (`w`) records**,
  error = `l`@ERROR + `e` records. DEBUG off by default, others on. Notifications render as
  warning-tinted entries (`heading: description`).
- **Location URIs:** nodes carry an optional free-form location URI (`n.loc`; graph-run
  nodes are tagged `node://<algorithm-uuid>`); rows render the faint `[uuid]` suffix
  derived from `node://` URIs. Future schemes (e.g. parameter locations) reuse the field
  without UI changes.
- **Structured errors:** error records render as a one-line summary with a collapsible
  stack trace ("Show stack trace"), not as raw dumps.
- **Footer:** active path breadcrumb + live progress + `N records · size` + the NDJSON
  data-file path. Header keeps Show log / Auto show progress / Auto view results plus
  Copy / Export (data file).

### HTML mockups (done)

The design language was iterated as plain HTML/CSS/JS mockups in
`docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/` (sample data embedded,
no build system): `v1-progress-panel.html` (partition-grouped, superseded) and
`v2-progress-panel.html` (**approved — the design contract** for the Swing
implementation: custom tree renderer with row-suffix components; lazy TreeModel fed by
batch notifications; virtualized per-node record list).

## Logs collection & viewer (Tools > Logs)

- `JIPipeRunnableLogEntry` holds a **store reference + run metadata** (task label, time,
  success, read flag) instead of a rendered `String` copy. The run-end
  `getLog().toString()` copy disappears (this was a second unbounded-memory hazard: entries
  kept full GB-scale texts).
- Entries are tagged with their owning project. When a project closes, its entries and
  store files are dropped (project temp cleanup). Global entries persist for the session;
  the existing log-limit setting bounds the list, and **evicting an entry deletes its
  store file** (disk stays bounded).
- **Running-log section:** the viewer lists currently-running runs (from the executor
  queue) — AI logs and other global runnables included — backed by the same store +
  coalescer, so live and finished logs share one rendering path.
- **Export:** saving an entry writes the `.ndjson` data file (primary), optionally plus a
  rendered `.txt`.
- `getLog()` callers re-mapped (9 files): `JIPipeDesktopProgressLoggerPanel` (reads store
  records), `JIPipeDesktopRunQueueLoggerPanel`, `JIPipeDesktopRunnableLogsCollection`,
  `JIPipeDesktopLogViewLogUI`, `JIPipeDesktopPublisherAssistant` (render view),
  `JIPipeGraphRun` (streamed `log.txt` render), `JIPipeServiceNoImageJInitializer`,
  `JIPipeServiceDefaultInitializer`, `CellposeUtils` (render view).

## Output directory compatibility

`JIPipeGraphRun` keeps writing `log.txt` at run end, **streamed** from the NDJSON file
(no full-buffer `toString()`): a tree-walk renderer that reproduces the familiar line shape
(`<p/mx> label-path | message`, `SUB` prefixes, `[ERROR]`/`[WARNING]` markers, run
header/footer). It is a convenience render, not a contract (see Non-goals).

## Performance requirements

Derived from the #1349 measurements (6.03 M lines, ~820 MB text, ~4.5 min run):

| Requirement | Target |
|---|---|
| Producer throughput | ≥ 25,000 records/s sustained (measured need ≈ 22,000/s), amortized O(1) `log()` |
| Heap | bounded: tail + node index only; independent of total record count |
| EDT load | ≤ 1 batch notification per 100 ms per store; queue-menu refresh ≤ 2/s |
| Run-end cost | no full-buffer copy; `log.txt` render streams from file |
| Read-back | opening a node with 6 M records shows its last page in < 200 ms |

## Testing

Per repo test policy (non-trivial contracts only; no Swing interaction tests):

- **Round-trip golden test:** records → file → rebuild → tree/record equality.
- **Scale test:** ~6 M synthetic records written through the store; assert bounded tail,
  writer throughput, and correct read-back pages (the #1349 magnitude).
- **Coalescer contract:** burst of N records → ≤ `⌈duration/interval⌉ + 1` notifications,
  final batch always delivered.
- **Level filter + text render:** DEBUG/INFO filtering; `log.txt` render of a known tree.
- **Storage lifecycle:** project run stores under project temp base; global run under
  global base; entry eviction deletes the file; truncated final line tolerated.
- **SILENT/STDOUT:** no file created; stdout path works.

## Rollout phases

1. **Core store** — records, NDJSON writer, node index, bounded tail, read-back, facade
   mapping, `SILENT`/`STDOUT`, unit tests (no UI change yet).
2. **Event path** — coalescer; rewire worker; rewrite the 8 consumer sites onto batches;
   remove the per-line emitter and `ProgressEvent`.
3. **Logs infra** — `JIPipeRunnableLogEntry` store reference, project tagging, eviction,
   export, streamed `log.txt`, running-log plumbing.
4. **UI** — the approved design contract is `ui/v2-progress-panel.html`; implement the
   Swing tree panel + queue-menu rewrite against it.
5. **Cleanup** — verbose-site `debug()` migration (incl. `FilterRoi2dByOverlapAlgorithm`),
   remove old `StringBuilder` machinery, documentation.

## Risks

- **NDJSON size** mirrors today's buffer size on disk instead of heap (same order of
  magnitude; acceptable — it is the data, and temp scopes clean it).
- **Windows file locking** while the viewer reads a file the writer appends to: readers use
  a separate channel and tolerate a truncated final line.
- **HTML→Swing fidelity:** the mockup fixes the design language (structure, badges, bars,
  collapsibles), not pixel-exact styling; Swing theming follows.
- **Breaking API changes** (`StatusUpdatedEvent` removal, `getLog()` → `CharSequence`) are
  acceptable in the 6.0.0 major (precedent: taxonomy redesign carried no legacy compat).
