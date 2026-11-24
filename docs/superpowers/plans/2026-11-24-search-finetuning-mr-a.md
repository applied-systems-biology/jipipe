# Search Finetuning MR A (usage-aware ranking) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Feed mined + recorded node usage into the enhanced search ranking (bounded bonus, tie-break, bucket rescale) and ship the usage prior resource with its generator.

**Architecture:** A new small `NodeUsage` service (prior + per-user counts, injectable for tests) feeds a bounded score bonus and a comparator tie-break in `JIPipeEnhancedNodeDatabaseSearch`; a committed generator script mines the real-world corpus into a resource JSON; the three UI insert funnels record usage. A shared text helper (`NodeSearchText`) centralizes name-word tokenization for this MR's tests and MR B's gate.

**Tech Stack:** Java 21, Maven multi-module, Jackson, JUnit 5 (assertj in core tests), Python 3 (generator).

**Spec:** `docs/superpowers/specs/2026-11-24-search-finetuning-design.md`

## Global Constraints

- Branch `1404-search-finetuning` off the current `1404-search-ux-enhancements` HEAD (stacks on !214); MR targets master after !214 merges.
- Registered node ids are stable string literals; the prior and recorded counts key on them; unknown ids are ignored, never an error.
- The prior is registry-filtered (ids that are not registered node types are dropped at generation). Canvas notes, comments, groups, and compartment I/O ARE registered node types and stay in the prior (Ruling, Task 2 - the builder-check premise that they "carry no description" was wrong).
- Usage units are "times used" (instances); `effective = prior.instances + userCount`; bonus `W = 0.05`, `K = 10` (`bonus = W * f / (f + K)`), applied ONLY inside the coverage-scored comparator tier; the exact/name-prefix ladder stays strictly above usage.
- No em dashes (U+2014) or en dashes (U+2013) anywhere.
- The example-workflow corpus lives outside the repo; the generator takes its path as an argument; the committed artifact is the generated resource only. CI never reads the corpus.
- Tests must not write the user's real settings: `NodeUsage` persistence is injectable and stubbed in tests (the `NodeEntryKind` persistence-helper precedent).
- The registry filter at generation drops ids that are not registered node types at all. Canvas notes, comments, groups, and compartment I/O ARE registered node types and STAY in the prior (Ruling, Task 2 - the builder-check premise that they "carry no description" was wrong); see the Global Constraints bullet above.

## Review Focus

- Unknown or renamed node ids in the prior/recorded counts: loading and ranking must silently ignore them (a plugin removed between corpus mining and runtime). Test: Task 3.
- Corrupt/foreign values in the registry counts map: reset to empty, never throw. Test: Task 3.
- Non-node entries (templates, compartments, existing nodes) have null origin ids: no bonus, no NPE. Test: Task 4.
- Missing/unreadable prior resource (packaging stripped it): prior treated as empty, ranking unaffected. Test: Task 3.
- Concurrent `record` calls (UI + worker threads): no lost updates, no torn writes. Test: Task 3.

---

### Task 1: Shared text helper `NodeSearchText`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeSearchText.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java` (replace the private `SPLIT_WORD_PATTERN`/`splitWords` with the helper; behavior identical)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchTextTest.java`

**Interfaces:**
- Produces (used by Tasks 2/3 and MR B's gate):
  - `public static List<String> contentWords(String text)` - split on the search's word pattern (whitespace, `_ - / .`, camelCase), lowercase, drop stopwords (`the a an of and or to in for with is are be as on by it its this that from into if you your can will`), drop single-character and pure-digit words; order preserved, duplicates kept.
  - `public static Set<String> wordSet(String text)` - lowercase word set of arbitrary text (no stopword filtering).
  - `public static boolean coveredBy(String word, Set<String> tokens)` - the search's ladder without fuzzy: exact, then token-prefix either direction (`token.startsWith(word) || word.startsWith(token)`), then stem equality (English stemmer as in the search), then substring.

- [ ] **Step 1: Write the failing test**

```java
@Test void contentWordsSplitsCamelCaseAndStops() {
    // "to" is a stopword; "2d" is not a pure-digit word and stays; "ROI"
    // has no split point (the pattern only splits at case boundaries)
    assertEquals(List.of("convert", "2d", "roi", "rgb"),
            NodeSearchText.contentWords("Convert 2D ROI to RGB"));
}
@Test void coveredByLadder() {
    Set<String> tokens = Set.of("particles", "finds", "segmentation");
    assertTrue(NodeSearchText.coveredBy("particle", tokens));   // token prefix
    assertTrue(NodeSearchText.coveredBy("find", tokens));       // token prefix ("finds")
    assertFalse(NodeSearchText.coveredBy("threshold", tokens));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -o -Dtest=NodeSearchTextTest`
Expected: FAIL (class missing)

- [ ] **Step 3: Implement `NodeSearchText`**

Move `SPLIT_WORD_PATTERN` and `splitWords` from `JIPipeEnhancedNodeDatabaseSearch` into the helper (the search keeps using them via the helper so the token stream is bit-identical); add the stopword set and the ladder method (stem via the existing `EnglishStemmer` usage pattern from the search).

- [ ] **Step 4: Run test to verify it passes; then run the full core suite**

Run: `mvn -pl jipipe-core test -o -Dtest='NodeSearchTextTest,EnhancedNodeDatabaseSearchRankingTest'` then `mvn -pl jipipe-core test -o` (background, must stay green: the search refactor must not change tokenization)
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeSearchText.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchTextTest.java
git commit -m "feat(#1404): shared search text helper (content words, coverage ladder)"
```

### Task 2: Usage prior resource + generator

**Files:**
- Create: `utils/search-usage/generate-usage-prior.py`
- Create: `utils/search-usage/README.md`
- Create: `jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json` (GENERATED - run the script, do not hand-write)
- Keep (formalize): `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/NodeRegistryDumpToolTest.java` (already in the tree; no-ops without `-Djipipe.dump.path`; used to produce the registry dump the generator filters against)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/UsagePriorResourceTest.java`

**Interfaces:**
- Produces: the resource JSON `{"source": <string>, "generated": <ISO date>, "counts": {"<node-id>": {"instances": N, "pipelines": M}}, "corpus": {"files": N, "sha256": <hex>}}`, keys sorted lexicographically. Loaded by Task 3's `NodeUsage.loadPrior()`; pinned by this task's test.

- [ ] **Step 1: Write the generator**

`generate-usage-prior.py [--corpus DIR] [--registry-dump JSON] [--out PATH]`: walks `*.jip` files, counts `jipipe:node-info-id` occurrences (instances) and distinct files per id (pipelines); with `--registry-dump`, drops ids not present in the dump's keys; writes the JSON with `json.dump(..., indent=2, sort_keys=True, ensure_ascii=False)` + trailing newline; `corpus.sha256` = sha256 over the sorted `(filename, bytes)` pairs; `generated` = UTC date. Deterministic: same input, byte-identical output.

- [ ] **Step 2: Generate the committed resource**

Run:
```bash
mvn -q -pl jipipe-launcher -am test -o -Dtest=NodeRegistryDumpToolTest \
    -Djipipe.dump.path=/tmp/opencode/node-registry-dump.json -Dsurefire.failIfNoSpecifiedTests=false
python3 utils/search-usage/generate-usage-prior.py \
    --corpus "/data/JIPipe/Example workflows" \
    --registry-dump /tmp/opencode/node-registry-dump.json \
    --out jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json
```
Expected: resource exists; spot-check `ij1-threshold-auto2d` has `instances: 242`, `pipelines: 50`; `graph-annotation-text-box` ABSENT.

- [ ] **Step 3: Write the failing resource test**

```java
@Test void resourceIsWellFormedAndPinned() throws Exception {
    Map<String, Integer> prior = NodeUsage.loadPrior();   // Task 3's loader; create a minimal
                                                          // version first if needed (compile order)
    assertEquals(242, prior.get("ij1-threshold-auto2d"));
    assertFalse(prior.containsKey("graph-annotation-text-box"));
    assertTrue(prior.values().stream().allMatch(v -> v > 0));
}
```
(If `NodeUsage` does not exist yet, load the resource with Jackson in this test exactly the way Task 3's loader will, and Task 3 reuses that parsing.)

Run: `mvn -pl jipipe-core test -o -Dtest=UsagePriorResourceTest`
Expected: FAIL if the resource is malformed or the pins drift; PASS after generation.

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Commit**

```bash
git add utils/search-usage jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json \
        jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/NodeRegistryDumpToolTest.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/UsagePriorResourceTest.java
git commit -m "feat(#1404): usage prior generator and shipped prior resource (mined from the example-workflow corpus)"
```

### Task 3: `NodeUsage` service

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeUsage.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeUsageTest.java`

**Interfaces:**
- Consumes: Task 2's resource (via `NodeUsage.loadPrior()`), the settings registry (pattern of `NodeEntryKind`: database `"node-db"`, `JIPipePathMetadataStore.key("node-usage-counts")`).
- Produces (used by Task 4/5):
  - `public static int usage(String nodeId)` - prior + user count; 0 for null/unknown ids.
  - `public static void record(String nodeId)` - increments the user count (concurrent map) and persists write-through; no-op for null/blank ids; unknown-to-registry ids are recorded anyway (they may exist in another installation's plugin set).
  - `public static void setPriorSupplier(Supplier<Map<String, Integer>> supplier)` / `public static void setUserCountsPersister(Consumer<Map<String, Integer>> persister)` - test injection; production defaults load the resource and write the registry.
  - `public static void resetForTests()` - restores defaults and clears state.

- [ ] **Step 1: Write the failing test**

```java
@Test void usageSumsPriorAndUserCounts() {
    NodeUsage.setPriorSupplier(() -> Map.of("a", 10));
    NodeUsage.setUserCountsPersister(m -> { });
    NodeUsage.record("a"); NodeUsage.record("a"); NodeUsage.record("unknown-id");
    assertEquals(12, NodeUsage.usage("a"));
    assertEquals(1, NodeUsage.usage("unknown-id"));
    assertEquals(0, NodeUsage.usage(null));
    assertEquals(0, NodeUsage.usage("never-seen"));
}
@Test void concurrentRecordsDoNotLoseUpdates() throws Exception {
    NodeUsage.setUserCountsPersister(m -> { });
    Thread[] threads = new Thread[8];
    for (int t = 0; t < 8; t++) { threads[t] = new Thread(() -> { for (int i = 0; i < 100; i++) NodeUsage.record("x"); }); threads[t].start(); }
    for (Thread th : threads) th.join();
    assertEquals(800, NodeUsage.usage("x"));
}
@Test void corruptPriorJsonYieldsEmptyMap() {
    // package-private parse of the resource JSON: garbage -> empty, no throw
    assertTrue(NodeUsage.parsePriorJson(JsonUtils.getObjectMapper().createObjectNode().put("counts", "not-an-object")).isEmpty());
}
@Test void corruptRegistryCountsAreDropped() {
    // package-private coercion: mixed/foreign values are dropped, valid ones kept
    Map<String, Object> raw = Map.of("a", 5, "b", "not-a-number", "c", 2.0);
    Map<String, Integer> coerced = NodeUsage.coerceCounts(raw);
    assertEquals(5, coerced.get("a"));
    assertEquals(2, coerced.get("c"));
    assertFalse(coerced.containsKey("b"));
}
```
(plus a test that a persister receiving the map sees the updated counts, and `resetForTests()` clears everything)

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -o -Dtest=NodeUsageTest`
Expected: FAIL (class missing)

- [ ] **Step 3: Implement `NodeUsage`**

Static state: `volatile Supplier<Map<String,Integer>> priorSupplier` (default: `NodeUsage::loadPrior`, reading `usage-prior.json` from the classpath with Jackson, empty map on any failure), `volatile Consumer<Map<String,Integer>> persister` (default: put the map into the registry via `JIPipe.getSettings().putIntoRegistry("node-db", KEY, new TreeMap<>(counts))` + `save()`; reading happens lazily once from `getFromRegistry(..., Map.class, ...)` with tolerant number coercion), `ConcurrentHashMap<String, Integer> userCounts`. `record` bumps and then hands a snapshot copy to the persister. Cache the prior map in a volatile field after the first load.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-core test -o -Dtest='NodeUsageTest,UsagePriorResourceTest'`
Expected: PASS (no settings write in tests: the injected persister stubs it)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeUsage.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeUsageTest.java
git commit -m "feat(#1404): NodeUsage service (shipped prior + per-user counts, injectable for tests)"
```

### Task 4: Ranking changes in `JIPipeEnhancedNodeDatabaseSearch`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeUsageRankingTest.java`

**Interfaces:**
- Consumes: `NodeUsage.usage(String)` (Task 3), the fixture pattern of `EnhancedNodeDatabaseSearchRankingTest` (TestNodeDatabase + stub nodes + template entries).
- Produces: `static double usageBonus(String nodeId)` (package-private for tests: `W * f / (f + K)`, `W = 0.05`, `K = 10`, 0.0 for null) and `static int bucketFor(double coverage)` (package-private: `Math.min(1.0, coverage / 0.95)` bucketed at `0.98 / 0.90 / 0.80 / 0.70`).

- [ ] **Step 1: Write the failing tests**

```java
@Test void usageBonusFlipsNearTies() {
    // Two nodes with IDENTICAL names except the variant word, identical
    // description shape, neither name a prefix of the query "extract blobs":
    // "Extract blobs wide" and "Extract blobs thin". Prior: wide=100, thin=0.
    NodeUsage.setPriorSupplier(() -> Map.of(WIDE_ID, 100));
    List<...> results = search.query("extract blobs", Pipeline, false, true, Set.of());
    assertTrue(indexOf(results, "Extract blobs wide") < indexOf(results, "Extract blobs thin"));
}
@Test void usageCannotCrossTheLadder() {
    // "Extract blobs" node (prefix match, prior 0) vs "Make blobs from noise"
    // (coverage-only match, prior 10000): the prefix match wins.
    NodeUsage.setPriorSupplier(() -> Map.of(MAKE_ID, 10000));
    ... assertTrue(indexOf(prefixMatch) < indexOf(coverageMatch));
}
@Test void nullOriginEntriesGetNoBonusAndNoNpe() {
    // a template entry + query; must not throw; ranking deterministic
}
@Test void bucketForRescalesTheNameTokenMaximum() {
    assertEquals(0, JIPipeEnhancedNodeDatabaseSearch.bucketFor(0.95)); // full name+token, no echo
    assertEquals(0, JIPipeEnhancedNodeDatabaseSearch.bucketFor(1.00));
    assertEquals(1, JIPipeEnhancedNodeDatabaseSearch.bucketFor(0.94));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-core test -o -Dtest=NodeUsageRankingTest`
Expected: FAIL (bonus not applied / methods missing)

- [ ] **Step 3: Implement**

In `internalQuery`: compute `finalScore -= usageBonus(e.getOriginNodeId())` for scored (non-exact, non-prefix) entries; extend `baseComparator` with `.thenComparingLong(e -> -NodeUsage.usage(e.getOriginNodeId()))` between the primary-score and name keys; replace the `mainBucket` computation with `bucketFor(coverage)`.

- [ ] **Step 4: Run test to verify it passes; then the affected suites**

Run: `mvn -pl jipipe-core test -o -Dtest='NodeUsageRankingTest,EnhancedNodeDatabaseSearchRankingTest,NodeSearchTextTest,NodeUsageTest,UsagePriorResourceTest'` then full `mvn -pl jipipe-core test -o` (background)
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeUsageRankingTest.java
git commit -m "feat(#1404): usage bonus (bounded), usage tie-break, and the name+token bucket rescale in the enhanced search"
```

### Task 5: UI recording wiring

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java` (`insertAtCursor`, around line 1141)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java` (`addAndConnectEntry` line 417, `addEntryToGraph` line 457)

**Interfaces:**
- Consumes: `NodeUsage.record(String)` (Task 3). Inserted nodes are read from the `Set<JIPipeDesktopGraphNodeUI>` each `entry.addToGraph(...)` returns: `nodeUI.getNode().getInfo().getId()`.

- [ ] **Step 1: Wire the three funnels**

After each successful `addToGraph` call, iterate the returned node UIs and `NodeUsage.record(info.getId())` per node (templates therefore record every member node type). The browser's `insertAtCursor` already holds the returned set; the finder's `addAndConnectEntry` uses `.iterator().next()` - switch to the set first, then record from it and take the first element.

- [ ] **Step 2: Verify no settings write happens in tests and the module compiles**

Run: `mvn -pl jipipe-core test -o -Dtest='NodeBrowserModelTest,NodeBrowserTreeControllerTest'` (these cover the surrounding model; the recording lines are Swing-adjacent and verified by eye per the project's no-Swing-test policy)
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java
git commit -m "feat(#1404): record node usage at the browser and finder insert funnels"
```

### Task 6: Whole-branch verification and MR

- [ ] **Step 1: Full core suite**

Run: `mvn -pl jipipe-core clean test -o` (background; the clean avoids the stale `InstrumentationServerTest.class` discovery crash)
Expected: 0 failures, 0 errors

- [ ] **Step 2: Compile the launcher module (the dump tool test lives there)**

Run: `mvn -q -pl jipipe-launcher -am test -o -Dtest=NodeRegistryDumpToolTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (dumps; no assertion failures)

- [ ] **Step 3: Push and open MR A**

Target master (after !214 merges; if it has not, target `1404-search-ux-enhancements` and retarget on merge). Description: links the design doc, states the DnD deviation (below), lists the test evidence.

**Deviation from the approved design (recorded, not silent):** the design says drag-and-drop records usage; the DnD payload is a plain `stringFlavor` (graph JSON) shared with node copy/paste, so the drop side cannot attribute the source without a dedicated data flavor. This MR records the three click-driven funnels and leaves DnD unrecorded; a dedicated flavor remains possible later if the signal gap matters.
