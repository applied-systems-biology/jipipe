# Progress System Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat in-memory `JIPipeProgressInfo` log with a tree-structured, file-backed (NDJSON) progress store with bounded heap, coalesced events, and a redesigned desktop UI, fixing the #1349-class EDT/heap pathology.

**Architecture:** Producers call the unchanged `JIPipeProgressInfo` facade, which becomes a handle `(JIPipeProgressLog store, nodeId)`. Records go lock-free into an MPSC queue drained by one async writer thread per run into `<logs-base>/<run-uuid>.ndjson` (project temp base or global temp base — never the output directory). Memory holds only a bounded tail ring + a light node index; older records are read back from the file. Listeners receive coalesced batch notifications (≤1/100 ms + final), never per-record events.

**Tech Stack:** Java 21, Jackson (`JsonUtils.toJsonString`, already in jipipe-core), `java.nio`, no new dependencies. Swing for the UI (Task 9). JUnit 5 (already configured in jipipe-core with the ImageJ-patcher `LauncherSessionListener`).

**Spec:** `docs/superpowers/specs/2026-11-04-progress-system-overhaul-design.md`
**UI design contract:** `docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/v2-progress-panel.html`

## Global Constraints

- Branch: `1352-progress-system-overhaul` (already created). Commit prefix `feat(progress):` / `test(progress):` / `refactor(progress):`.
- **No new third-party dependencies.** No SQLite/H2. NDJSON only.
- **Offline build is preferred:** `mvn -q -pl jipipe-core compile -o`. Tests: `mvn -q -pl jipipe-core test -o -Dtest=<Class>`.
- **Never emit or do I/O on the producer path:** `log()` must be O(1) enqueue. No listener calls, no lock held during encoding.
- **Record file is the contract.** Text renders (`log.txt`, `getLog()` view) are convenience only, not parseable contracts.
- **Heap bound:** in-memory state per run = tail ring (default 8,192 records or ~8 MB message text, whichever binds first) + node index. Nothing else scales with record count.
- Test policy (repo): no Swing interaction tests; no trivial getter/setter tests; tests cover non-trivial contracts (round-trips, coalescer math, scale, fail-closed guards).
- jipipe-core tests must not touch `ij.*` classes (ImageJ patcher ordering) — the tests in this plan don't.
- Coalescer default: 100 ms interval. Writer flush: ≥ 64 KB or ≥ 250 ms, final flush on close. NDJSON keys exactly per spec §Record types (`t`, `v`, `run`, `label`, `ts`, `id`, `p`, `l`, `k`, `s`, `lv`, `m`, `mx`, `c`, `st`, `h`, `d`).

## Review Focus

Failure modes the spec implies but whose tests live inside specific tasks:

1. **Verbose node floods records** (the #1349 trigger, ~22k rec/s) — run must stay responsive and heap-bounded. → Task 4 (coalescer burst math) + Task 2 (6 M-record scale test, throughput floor 25k/s).
2. **Run cancelled/killed mid-flush** — file ends with a truncated line; reader must tolerate it and return everything complete. → Task 2 (`readRecordsTruncatedFinalLineTolerated`).
3. **Run without a project or output path** (quick run, cache run) — store must land under the global temp base; nothing may be written into the output directory. → Task 6 (`resolveStorageBase` pure-function test) + Task 6 wiring note.
4. **Level filter switched while records stream** — already-visible rows must not be lost; hidden-debug counts correct. → Task 3 (`levelFilterRender` golden test).
5. **Logs-collection entry evicted or project closed** — file deleted, no NPE rendering an entry whose file is gone. → Task 8 (`evictDeletesFile`, `renderSurvivesMissingFile`).

---

### Task 1: Progress record types + log level

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeLogLevel.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressRecord.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/progress/ProgressRecordJsonTest.java`

**Interfaces:**
- Consumes: `org.hkijena.jipipe.utils.json.JsonUtils.toJsonString(Object)` (serializes a Jackson-annotated object to one line).
- Produces (used by every later task):
  - `enum JIPipeLogLevel { DEBUG(10), INFO(20), WARN(30), ERROR(40) }` with `int value()`.
  - `enum JIPipeProgressNodeKind { PARTITION, PHASE, ITEM, RUN }` and `enum JIPipeProgressNodeState { RUNNING, OK, FAILED, CANCELLED }` as two top-level files (`JIPipeProgressNodeKind.java`, `JIPipeProgressNodeState.java`, same package) so the store references them without the record file.
  - `sealed interface JIPipeProgressRecord` with **static factory methods** (all take epoch-millis `long ts` unless noted):
    - `record Head(int v, String run, String label, String app, long ts)` — `t:"head"`
    - `record Node(int id, int p, String l, String k, long ts)` — `t:"n"`, `k` = kind name
    - `record NodeFinished(int id, String s, long ts)` — `t:"N"`, `s` = `ok|fail|cancel`
    - `record Log(int id, int lv, String m, long ts)` — `t:"l"`
    - `record Progress(int id, int v, int mx, long ts)` — `t:"p"`, `mx < 0` = indeterminate
    - `record Error(int id, String c, String m, String st, long ts)` — `t:"e"`
    - `record Status(String s, long ts)` — `t:"s"`, `s` = `running|finished|failed|cancelled`
    - `record Notification(String h, String d, long ts)` — `t:"w"`
  - every record: `String type()` (the `t` value).

**Steps:**

- [ ] **Step 1: Write the failing test** `ProgressRecordJsonTest`

```java
@Test
void headRecordSerializesWithShortKeys() {
    String json = JsonUtils.toJsonString(JIPipeProgressRecord.head(1, "run-uuid", "Pipeline run", "6.0.0", 123L));
    // assert: contains "t":"head", "run":"run-uuid", "v":1, "ts":123 — exact keys, no pretty-printing
    assertFalse(json.contains("\n"), "NDJSON records must be single-line");
}

@Test
void levelEnumValuesMatchSpec() {
    assertEquals(10, JIPipeLogLevel.DEBUG.value());
    assertEquals(20, JIPipeLogLevel.INFO.value());
    assertEquals(30, JIPipeLogLevel.WARN.value());
    assertEquals(40, JIPipeLogLevel.ERROR.value());
}
```

- [ ] **Step 2: Run to verify it fails** — `mvn -q -pl jipipe-core test -o -Dtest=ProgressRecordJsonTest` → compile error, class missing.

- [ ] **Step 3: Implement** the enums + sealed record interface + factory methods. Jackson: annotate each record with `@JsonIgnoreProperties(ignoreUnknown = true)` and explicit `@JsonProperty` on every component (records serialize by component name otherwise — the short keys must win). `type()` returns the literal. `head(...)` maps to `Head(...)` and sets `t`. Keep `ts` as `long` epoch millis.

- [ ] **Step 4: Run to verify it passes** — same command → PASS.

- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(progress): NDJSON record types and log level enum"`

---

### Task 2: Progress store — async writer, node index, bounded tail, scale + truncation tests

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressNode.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressLog.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/progress/ProgressStoreTest.java`

**Interfaces:**
- Consumes: Task 1 record types, `JsonUtils.toJsonString`.
- Produces (Tasks 3–8 build on these exact signatures):

```java
public class JIPipeProgressLog implements AutoCloseable {
    public static JIPipeProgressLog silent();
    public static JIPipeProgressLog stdout();                      // prints rendered lines, no file
    public static JIPipeProgressLog create(Path file, String runUuid, String runLabel); // auto root node, file created lazily on first flush
    public static Path resolveStorageBase(Path projectTempBaseOrNull); // base.resolve("logs"), global base when null — pure, testable

    public Path getFile();                                         // null for silent
    public int beginNode(int parentId, String label, JIPipeProgressNodeKind kind); // returns nodeId, emits "n"
    public void log(int nodeId, JIPipeLogLevel level, String message);
    public void progress(int nodeId, int value, int max);          // max < 0 indeterminate
    public void error(int nodeId, Throwable e);
    public void nodeFinished(int nodeId, JIPipeProgressNodeState state); // emits "N"
    public void finish(JIPipeProgressNodeState runState);          // emits "s", final flush, close
    public void close();

    public JIPipeProgressNode getNode(int id);
    public int getRootNodeId();                                    // 1
    public int nextNodeId();                                       // peek without creating (facade tests)

    public List<JIPipeProgressRecord> readRecords(int fromLine, int limit); // fromLine 1-based, line 1 = head
    public List<JIPipeProgressRecord> readRecent(int count);             // from tail
    public void renderText(JIPipeLogLevel minLevel, java.util.function.Consumer<String> lineSink);

    public void setCoalesceIntervalMillis(long ms);   // default 100
    public void setTailLimit(int records);            // default 8192
    public ProgressBatchEventEmitter getBatchEventEmitter();  // Task 4 adds the emitter + event class

    public static class ProgressBatchEvent extends AbstractJIPipeEvent {
        public int fromLine(); public int toLine(); public Set<Integer> changedNodes(); public boolean finalBatch();
    }
}
```

`JIPipeProgressNode`: `int getId()`, `int getParentId()`, `String getLabel()`, `JIPipeProgressNodeKind getKind()`, `JIPipeProgressNodeState getState()`, `int getProgress()`, `int getMaxProgress()`, `int getDescendantNodeCount()`, `long getRecordCount()`, `int getFirstLine()`, `int getLastLine()`.

- Writer: daemon `Thread` per store, `ConcurrentLinkedQueue<Record>` drain loop, flush when buffer ≥ 64 KB or 250 ms elapsed or on `finish()`/`close()`. Encoding (`toJsonString`) happens **on the writer thread**, never on producers.
- Tail: ring buffer of `setTailLimit` records; `readRecent` serves it; `readRecords` opens the file with a **separate `RandomAccessFile`/channel** (not the writer's stream).
- `silent()` discards records (no file, no stdout). `stdout()` prints the compatibility line per record (used by CLI).

**Steps:**

- [ ] **Step 1: Write the failing tests** `ProgressStoreTest`

```java
@Test
void recordFileRoundTrip() throws Exception {
    Path file = tmp.resolve("run-1.ndjson");
    try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-1", "Pipeline run")) {
        int n = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        store.log(n, JIPipeLogLevel.INFO, "hello");
        store.progress(n, 5, 100);
        store.nodeFinished(n, JIPipeProgressNodeState.OK);
        store.finish(JIPipeProgressNodeState.OK);
    }
    List<JIPipeProgressRecord> all = readWholeFile(file); // helper: parse lines
    assertEquals(5, all.size());
    assertEquals("head", all.get(0).type());
    assertEquals("n", all.get(1).type());
    assertEquals("l", all.get(2).type());
    // "p", "N" follow; head.line 1 is the file's first line
}

@Test
void fileCreatedLazilyOnlyWhenRecordsExist() {
    JIPipeProgressLog silent = JIPipeProgressLog.silent();
    assertNull(silent.getFile());
    silent.finish(JIPipeProgressNodeState.OK);
    // no file anywhere
}

@Test
void truncatedFinalLineTolerated() throws Exception {
    // write 12 records, flush, then overwrite file cutting the last line in half
    // readRecords(1, 100) returns 11 complete records, no exception
}

@Test
void resolveStorageBaseIsPure() {
    Path projectBase = tmp.resolve("proj");
    assertEquals(projectBase.resolve("logs"), JIPipeProgressLog.resolveStorageBase(projectBase));
    // null → global base: assert result ends with "logs"
}

@Test
void scaleSixMillionRecordsThroughputAndBoundedTail() throws Exception {
    store.setTailLimit(8192);
    long start = System.nanoTime();
    for (int i = 0; i < 6_000_000; i++) store.log(node, JIPipeLogLevel.DEBUG, "cand " + i + " & filter " + (i % 7) + " --> overlap");
    store.finish(OK); long ms = (System.nanoTime() - start) / 1_000_000;
    assertTrue(6_000_000.0 / (ms / 1000.0) >= 25_000, "throughput floor 25k/s, was " + ms + "ms");
    assertEquals(8192, store.readRecent(8192).size(), "tail must be bounded");
    // read-back: readRecords(first log line, 10) equals the first 10 written (order preserved)
}
```

Run the scale test with `-Dtest=ProgressStoreTest#scaleSixMillionRecordsThroughputAndBoundedTail`; it may take a few minutes — that is expected and acceptable.

- [ ] **Step 2: Run to verify they fail** — compile error, store missing.

- [ ] **Step 3: Implement** `JIPipeProgressNode`, `JIPipeProgressLog` per the Interfaces block. Writer loop: poll the queue with 250 ms timeout; append; encode on writer thread; track line numbers; update node index (firstLine/lastLine/recordCount) and tail ring on the **writer thread** (single-writer → no contention). `beginNode`/`log`/etc. assign sequence numbers on enqueue (AtomicLong) so index updates stay in order. `readRecords`/`readRecent` may be called from any thread; parse lazily from the file (buffered read from `fromLine`).

- [ ] **Step 4: Run all store tests to verify they pass** — `mvn -q -pl jipipe-core test -o -Dtest=ProgressStoreTest`.

- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(progress): async NDJSON store with bounded tail, node index, read-back"`

---

### Task 3: Read-back level filtering + compatibility text renderer

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressLog.java` (add `renderText` implementation + `readRecords` level-filter overload)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/progress/ProgressRenderTest.java`

**Interfaces:**
- Produces: `void renderText(JIPipeLogLevel minLevel, Consumer<String> lineSink)` and `List<JIPipeProgressRecord> readRecords(int fromLine, int limit, JIPipeLogLevel minLevel)`.
- **Render contract (the golden):** for records on node path `root → "Partition 0" (PARTITION) → "Node A" (ITEM)`:
  - `l` INFO `"msg1"` with progress 5/100 on Node A → `[5/100] Partition 0 | Node A | msg1`
  - `l` WARN with message `"[WARNING] clipped"` (facade pre-formats) → `[5/100] Partition 0 | Node A | [WARNING] clipped`
  - `l` at level below `minLevel` → omitted entirely
  - `p` record with 50 % → `[5/100] Partition 0 | Node A | 50%`
  - `e` record → `<exception class>: <message>` line + each stack line indented two spaces (same as today's `log(Throwable)` output)
  - `n`/`N`/`s`/`head`/`w` records: `n` renders nothing; `w` renders `@ <heading>: <description>` (today's notification line)
  - Root-run lines (preprocessing etc.) render without the chain part: `[1/1] Preprocessing/Tunnels | Dissolving tunnels …`

**Steps:**

- [ ] **Step 1: Write the failing golden test** `ProgressRenderTest.renderKnownTreeMatchesGolden` — build the store in memory (silent or temp file), emit the exact record sequence above, collect `renderText(INFO, sink)` lines, `assertEquals(List.of(<the 5 lines above in order>), lines)`. Plus `renderText(DEBUG, sink)` includes a DEBUG record that INFO omits (the level-switch contract — Review Focus 4).
- [ ] **Step 2: Run to verify it fails.**
- [ ] **Step 3: Implement** the renderer: walk records in order (from file via `readRecords`, streaming in 10k-record pages); keep a running node-path stack from `n` records (root = id 1, label omitted); emit per contract.
- [ ] **Step 4: Run to verify it passes** — `mvn -q -pl jipipe-core test -o -Dtest=ProgressRenderTest`.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(progress): level-filtered read-back and compatibility text renderer"`

---

### Task 4: Coalescer — batch notifications

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressLog.java` (add `ProgressBatchEvent` + emitter, coalescing in the writer loop)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/progress/ProgressCoalescerTest.java`

**Interfaces:**
- Produces: `ProgressBatchEventEmitter extends JIPipeEventEmitter<ProgressBatchEvent, Listener>` (existing JIPipe event-emitter pattern, see `JIPipeProgressInfo.StatusUpdatedEventEmitter` for the shape). Contract: **≤ 1 event per `coalesceIntervalMillis`** summarizing `fromLine→toLine` + changed node ids; **the final batch (flagged `finalBatch()`) is always emitted** after the last record, even if it arrives inside the interval window.

**Steps:**

- [ ] **Step 1: Write the failing tests**

```java
@Test
void burstProducesBoundedNotifications() {
    store.setCoalesceIntervalMillis(50);
    // collect events on the test thread via subscribe
    for (int i = 0; i < 100_000; i++) store.log(node, DEBUG, "x");
    long start = System.nanoTime();
    store.finish(OK);
    long durationMs = (System.nanoTime() - start) / 1_000_000;
    int expectedMax = 1 + (int) Math.ceil(durationMs / 50.0);
    assertTrue(seen.size() <= expectedMax + 1, "burst of 100k must not produce 100k events; saw " + seen.size());
    assertTrue(seen.size() > 1, "a long-enough burst must produce more than one batch");
}

@Test
void finalBatchAlwaysDelivered() {
    // emit 3 records, finish immediately; assert last event has finalBatch()==true
    // and fromLine/toLine cover the full range (no gaps across all events)
}
```

(For determinism the tests use a 50 ms interval; the default stays 100 ms.)

- [ ] **Step 2: Run to verify they fail.**
- [ ] **Step 3: Implement** in the writer loop: track `lastEmitNanos`, pending from/to lines, changed-node set; after flushing, if ≥ interval elapsed since last emit (or records remain and the loop idles), emit; on `finish()` always emit the final batch before closing.
- [ ] **Step 4: Run to verify they pass** — `mvn -q -pl jipipe-core test -o -Dtest=ProgressCoalescerTest`.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(progress): coalesced batch notifications (<=1/interval + final)"`

---

### Task 5: `JIPipeProgressInfo` facade over the store

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipeProgressInfo.java` (rewrite internals)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipePercentageProgressInfo.java` (reimplement on `p` records)
- Delete: inner `StatusUpdatedEvent` / `StatusUpdatedEventEmitter` / `StatusUpdatedEventListener` (breaking — 6.0.0)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/ProgressInfoFacadeTest.java`

**Interfaces:**
- Consumes: Task 2 store API.
- Produces: the **entire existing public surface of `JIPipeProgressInfo` unchanged** except:
  - `getStatusUpdatedEventEmitter()` / `setStatusUpdatedEventEmitter(...)` **removed**
  - `getLog()` return type `StringBuilder` → `CharSequence` (rendered view of the tail; `clearLog()` clears that view only)
  - `SILENT` / `STDOUT` statics: `SILENT` = store `silent()`, `STDOUT` = store `stdout()`; `new JIPipeProgressInfo()` now creates a **standalone in-memory store in silent mode** (no file) so existing `new JIPipeProgressInfo()` call sites (tests, CLI helpers) keep working.
  - New: `void debug(String)`, `void log(JIPipeLogLevel, String)`, `JIPipeProgressLog getStore()`, `int getNodeId()`, `void setStorageBase(Path)` (sets `resolveStorageBase` base on the store before first flush; root-run creation point only).
- Mapping (spec §Producer API): `log` → INFO; `error`/`warn` keep `[ERROR] `/`[WARNING] ` prefix; `log(Throwable)` → `e` record; `resolve(text)` → `beginNode(current, text, ITEM)`; `resolveAndLog` → same, no extra record; `percentage(text)` → child node + `JIPipePercentageProgressInfo` emitting `p` records with the existing throttle (interval 1000 ms, always 50 %/100 %); `detachProgress()` → child node, `SUB` render flag kept on the node; progress getters read the node; cancellation and notifications unchanged (notifications also emitted as `w` records).

**Steps:**

- [ ] **Step 1: Write the failing tests**

```java
@Test
void facadeMapsToStoreRecords() {
    JIPipeProgressInfo info = new JIPipeProgressInfo();
    info.log("hello");
    JIPipeProgressInfo child = info.resolve("Node A");
    child.log("sub");
    child.percentage("P").logPercentage(50, 100);
    child.log(new RuntimeException("boom"));
    // assert via store.readRecords: INFO "hello" on root, node "Node A" created,
    // INFO "sub" on child, Progress(50), Error(RuntimeException class) records
}

@Test
void getLogViewRendersTail() {
    info.log("visible");
    assertTrue(info.getLog().toString().contains("visible"));
}

@Test
void defaultConstructorCreatesSilentStoreWithoutFile() {
    JIPipeProgressInfo info = new JIPipeProgressInfo();
    info.log("x");
    assertNull(info.getStore().getFile());
}
```

- [ ] **Step 2: Run to verify they fail.**
- [ ] **Step 3: Rewrite** the facade internals per the mapping. Keep every other method signature verbatim (307 files depend on them). `resolveAndLogForEach` family: unchanged semantics, delegates to `resolve` per item.
- [ ] **Step 4: Compile the whole repo** — `mvn -q -pl jipipe-core compile -o` then `mvn -q compile -o` (full reactor: this is the blast-radius gate; **all 307 call sites must compile untouched**). Fix only signature-level fallout (e.g. `getLog()` return-type usages — update the 9 `getLog()` caller files per the spec mapping: most only read it as a `String`/`StringBuilder`; the logs collection gets its real rework in Task 8, for now point it at the `CharSequence` view).
- [ ] **Step 5: Run the facade tests + full jipipe-core tests** — `mvn -q -pl jipipe-core test -o`.
- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat(progress): JIPipeProgressInfo is now a facade over the progress store"`

---

### Task 6: Wire `JIPipeGraphRun` — store creation, location, streamed `log.txt`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeGraphRun.java` (~148-200 run start; ~287-300 log.txt write)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeGraphRunConfiguration.java` (only if a `storeToDisk`-style flag is needed — check first; likely none)
- Test: extend `ProgressStoreTest` (location cases) or new `GraphRunLogWiringTest` if project creation is cheap; otherwise location logic is covered by `resolveStorageBaseIsPure` (Task 2) and wiring is compile-gated.

**Interfaces:**
- Consumes: `JIPipeProgressLog.create(file, runUuid, label)`, `resolveStorageBase(Path)`, `renderText`.
- Produces: `JIPipeGraphRun` owns the root store for the whole run; `getBaseRun().getProgressStore()` (new getter) exposes it to consumers (queue UI, instrumentation, logs collection).

**Steps:**

- [ ] **Step 1: Implement** at `run()` start (before the first `progressInfo.log`):
  - `Path base = (parent == null) ? JIPipeProgressLog.resolveStorageBase(project != null ? project.getTemporaryBaseDirectory() : null) : null;`
  - root run only: create `JIPipeProgressLog store = JIPipeProgressLog.create(base.resolve(runUUID + ".ndjson"), runUUID, "Pipeline run")`; build the root `JIPipeProgressInfo` handle on it; child runs (`parent != null`) share the root's store/handle (they already receive the parent's progress info).
  - Keep the ASCII banner / version logs — they become INFO records on the root (unchanged UX).
- [ ] **Step 2: Replace the end-of-run `log.txt` write** (`JIPipeGraphRun.java:291-300`): `store.renderText(INFO, line -> writer.append(line).append('\n'))` streamed to `configuration.getOutputPath().resolve("log.txt")` **while** `store.finish(...)` happens first (records complete, then render). Preserve the existing `JIPipeValidationRuntimeException` on IO failure.
- [ ] **Step 3: Compile** — `mvn -q -pl jipipe-core compile -o`; **run jipipe-core tests** — `mvn -q -pl jipipe-core test -o`.
- [ ] **Step 4: Commit** — `git add -A && git commit -m "feat(progress): graph run creates progress store, streams log.txt from it"`

---

### Task 7: Event-path rewrite — worker + all 8 consumer sites

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeRunnableWorker.java` (stop per-line `publish`; subscribe to the store's batch emitter; `ProgressEvent` payload becomes `JIPipeProgressLog.ProgressBatchEvent`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeRunnable.java` (`ProgressEvent`/`ProgressEventListener` retype or remove + replace)
- Modify (all onto batch events; compile gate): `desktop/commons/components/JIPipeDesktopProgressLoggerPanel.java` (append rendered tail delta from batch), `desktop/app/running/queue/JIPipeDesktopRunQueueLoggerPanel.java`, `desktop/app/running/queue/JIPipeDesktopRunnableQueueButtonMenuItem.java` (+ compact variant — Timer 500 ms, dirty-flag, reads node state), `desktop/app/JIPipeDesktopProjectWindow.java:775-827` (stage listener), `JIPipeRunAlgorithmCommand.java:152`, `api/instrumentation/InstrumentationRunEngine.java` (forward batch as `job_progress`; message = last rendered line of the batch), `api/service/init/JIPipeServiceDefaultInitializer.java:69`, `api/service/components/JIPipeAIServiceComponent.java` (AI log forwarding on batches)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationProgressMappingTest.java` (pure: batch → `job_progress` message extraction)

**Interfaces:**
- Consumes: Task 4 batch emitter.
- Produces: `JIPipeRunnableWorker.getProgressEventEmitter()` emits `JIPipeRunnable.ProgressEvent(JIPipeRunnableWorker, JIPipeProgressLog.ProgressBatchEvent)` — same emitter name (call sites unchanged), new payload type.

**Steps:**

- [ ] **Step 1: Write the failing instrumentation mapping test** — a batch with rendered lines `["a", "b"]` + final flag produces a `job_progress` with message `"b"`; a non-final batch with an empty tail delta produces no progress event.
- [ ] **Step 2: Run to verify it fails.**
- [ ] **Step 3: Rewrite** worker + all 8 sites per the list. Queue-menu items: `javax.swing.Timer` 500 ms that refreshes only when a batch arrived since last refresh (dirty flag) — no per-batch `setText`.
- [ ] **Step 4: Full reactor compile** — `mvn -q compile -o` (consumer files span jipipe-core; all in one module). Run `mvn -q -pl jipipe-core test -o`.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "refactor(progress): coalesced event path for worker and all consumers"`

---

### Task 8: Logs collection + viewer + export on the store

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeRunnableLogEntry.java` (holds `Path dataFile` + `String runLabel` + metadata; drop the rendered `String log`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/logs/JIPipeDesktopRunnableLogsCollection.java` (push store/file refs; eviction deletes file; project tagging)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/logs/JIPipeDesktopLogViewLogUI.java` (render from file on demand; Export writes `.ndjson` (+ optional `.txt`) via `JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/publish/JIPipeDesktopPublisherAssistant.java` (uses `getLog()` — switch to `renderText`/`CharSequence` view)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/running/logs/RunnableLogsCollectionTest.java`

**Interfaces:**
- Produces: `JIPipeRunnableLogEntry.getPath()` (nullable when silent), `setProject(JIPipeProject)`; collection removes a project's entries on project close (hook: the existing finished/interrupted listeners + a project-close subscription if one exists on `JIPipeDesktopProjectWindow`; otherwise removal happens at eviction — document whichever the codebase offers and use the simpler one).

**Steps:**

- [ ] **Step 1: Write the failing tests**

```java
@Test
void evictDeletesFile() {
    // entry with a real temp .ndjson; call the eviction path (limit 1, add 2 entries)
    assertFalse(Files.exists(firstEntry.getPath()));
}

@Test
void renderSurvivesMissingFile() {
    // entry whose file was deleted: renderer returns a "data unavailable" placeholder, no NPE
}
```

- [ ] **Step 2: Run to verify they fail.**
- [ ] **Step 3: Implement** the rework per the spec §Logs collection & viewer. `pushToLog(run, success)`: take `run`'s store file path (from the base-run getter, Task 6) instead of `getLog().toString()`.
- [ ] **Step 4: Compile + tests** — `mvn -q compile -o` (desktop classes are in jipipe-core here), `mvn -q -pl jipipe-core test -o`.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(progress): logs collection stores file refs, exports ndjson"`

---

### Task 9: Swing progress tree panel (v2 design contract)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/JIPipeDesktopProgressTreePanel.java`
- Modify: the host panel replacing `JIPipeDesktopProgressLoggerPanel` in the run window (find via `grep -rn "JIPipeDesktopProgressLoggerPanel" jipipe-core/src/main/java` — keep the old class for the interim compile in Task 7, swap the instantiation here)
- **No tests** (repo policy: no Swing interaction tests; the user verifies by eye).

**Interfaces:**
- Consumes: Task 2 store API (tail, `readRecords` paging, node index), Task 4 batches, v2 design contract.
- Implements exactly `ui/v2-progress-panel.html`: flat rows (no partition grouping; PARTITION-kind nodes squash into the dim `P0` chip), state icons + spinner for indeterminate, inline bars (`[v/mx (pct)%]`), `+N` hidden-descendants chip **only while collapsed** (recompute on expand), collapsed detail area with `Show N older records from run data…` paging (default page 1,000 records, read from file), INFO/DEBUG filter select with faint `<n> debug` chip when DEBUG records are hidden, one-line error rows with "Show stack trace" collapsible, footer (breadcrumb path · live progress · `N records · size` · data-file path), header toggles unchanged + Copy/Export.
- Rendering driver: batch events mark the model dirty; a 250 ms `Timer` rebuilds changed rows only (row = `JTree` cell or a flat `JList` of row objects — choose the flat `JList`/`JTable` route: the v2 design is a flat list, not a JTree, and laziness is trivial with `ListCellRenderer` + virtual scrolling).

**Working directory:** create a worktree for this task (`worktree_create` with a name like `1352-progress-ui`) and move the session into it (`session_move`) — it is the primary working directory for the Swing implementation. The v2 design contract travels with the branch (commit `00d3f748f8`).

**Steps:**

- [ ] **Step 1: Implement** the panel per the contract (single focused file; extract the row-renderer if it grows past ~300 lines).
- [ ] **Step 2: Swap** the run-window panel; delete nothing yet.
- [ ] **Step 3: Compile** — `mvn -q -pl jipipe-core compile -o`. **No tests.** Tell the user to run the app: open the progress panel on a real run, expand nodes, toggle the level filter, trigger an error (e.g. a node that fails), check the footer path and Export.
- [ ] **Step 4: Commit after user confirms the look** — `git add -A && git commit -m "feat(progress): v2 progress tree panel in the run window"`

---

### Task 10: Verbose-site migration + cleanup + spec alignment

**Files:**
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/roi/filter/FilterRoi2dByOverlapAlgorithm.java` (per-pair `logCandidateFilterVerbose` → `progressInfo.debug(...)`)
- Modify: other `Verbose`-gated per-item logging found by `grep -rn "Verbose" --include='*.java' jipipe-core plugins | grep -i "log("` — migrate each hit to `debug()` (list the files in the commit message)
- Delete: dead code left by Tasks 5/7 (`JIPipeDesktopProgressLoggerPanel` if fully replaced, `StatusUpdatedEvent` remnants if any reference remains)
- Modify: `AGENTS.md` — one short paragraph under the build/testing section documenting the new `api/progress` package (record types, store, facade, coalescer contract) so future agents find it

**Interfaces:** consumes `JIPipeProgressInfo.debug(String)` (Task 5).

**Steps:**

- [ ] **Step 1: Migrate** `FilterRoi2dByOverlapAlgorithm` + the audit list; **compile** `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile -o` (and any other touched plugin module).
- [ ] **Step 2: Delete** dead code; full reactor `mvn -q compile -o` + `mvn -q -pl jipipe-core test -o` (all progress tests green).
- [ ] **Step 3: Write** the AGENTS.md paragraph.
- [ ] **Step 4: Commit** — `git add -A && git commit -m "feat(progress): verbose sites to debug level, remove legacy machinery"`

---

## Self-Review Notes

- **Spec coverage:** data model (T1–T2), storage/location (T2, T6), bounded memory (T2 scale test), producer API (T5), event path (T4, T7), UI (T9), logs collection/viewer/export (T8), log.txt compat (T6), tests (T1–T4, T8), rollout phases 1–5 (T2–T3 → T4–T5 → T8 → T9 → T10; T6/T7 fill phase 2–3). The "running-log section" in the logs viewer is part of T8's viewer rework (live entries from the executor queue).
- **Step scan:** each code step carries exact signatures; the only algorithmic bodies (writer loop, renderer walk) are pinned by golden/scale tests.
- **Type consistency:** `JIPipeProgressLog` / `JIPipeProgressNode` / `JIPipeProgressRecord` / `JIPipeLogLevel` / `JIPipeProgressNodeKind` / `JIPipeProgressNodeState` / `ProgressBatchEvent` used identically in T1–T10; facade method names match the existing class verbatim (T5 gate: full-reactor compile).
- **Proportion:** 10 tasks, signatures + test assertions only — no full bodies.
