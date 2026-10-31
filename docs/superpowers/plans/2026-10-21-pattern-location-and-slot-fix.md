# Bundled System Pattern Fix - Locations, Floating Nodes, io-Interface Drop

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]` syntax for tracking.

**Goal:** Every shipped System pattern loads with (a) resolvable, properly spaced node locations and (b) every edge
intact - no stacked nodes, no floating nodes - plus the 6 `io-interface` plumbing patterns dropped from the shipped set
(user ruling, 2026-10-21).

**Architecture:** Two defect classes, both rooted in the corpus-mining "minimal graph" format plus one curation gap.

- **Locations:** stored locations are **grid cells** (`gridToRealLocation = x·zoom·GRID_WIDTH(25)`). The 251
  `dag-mined-*` files are valid (real UUID compartments + cell coords from the user's original layout) and are LEFT
  ALONE. The 155 `dag-derived-*` files are invalid twice over: `jipipe:graph-compartment: "derived"` is a legacy *name*
  (demoted to the default compartment at load; the name is stashed in `nodeLegacyCompartmentIDs`, which no consumer
  reads, so `location/derived/*` never resolves) AND their coords are in *pixels* (`x=100+250·depth, y=100+150·rank` -
  verified: all 450 nodes match exactly). The 24 linear files have **no locations at all**. Fix: lay them out with the
  project's own **Sugiyama convention** (`SugiyamaGraphAutoLayoutImplementation` - longest-path layering; start x=1
  cell, column step = max node width + 2 cells, start y=1 cell, row step = node height + 1 cell), and rekey derived
  files to a resolvable compartment key.
- **Floating nodes (66 files / ~150 edges - NOT the 14 the first audit assumed):** the minimal-graph format strips
  `jipipe:slot-configuration` AND the slot-driving parameters from every node (`mine-dag-patterns.py:_minimal_node`
  keeps only 4 keys; verified 0/13860 candidate nodes carry a config). A fresh node then has only its
  annotation-declared slots, so `JIPipeGraph.edgesFromJson` drops every edge whose slot name is user-renamed,
  parameter-driven, or dynamic: **109** are static-type nodes with parameter-driven/renamed slots (e.g.
  `ij1-color-split-channels-2` driven by `channel-to-slot-assignments/C1`, `ij1-color-blend-images` `Top`/`Bottom`,
  `distribute-data` `Train`/`Test`), **44** are the 6 `JIPipeIOSlotConfiguration` dynamic types. The source corpus
  carries the full configs (all 318 member projects present under `corpusRoot`, verified). Fix = **per-pattern
  corpus-instance restoration**: for each affected node, find a corpus instance of the same node type whose
  `jipipe:slot-configuration` covers the node's required slot set (source edges ⊆ output slots, target edges ⊆ input
  slots); copy the instance's `jipipe:slot-configuration` + its non-JIPipe parameter keys verbatim (real data types, not
  guesses). Deterministic tie-break: lexicographically-smallest (project path, compartment key, node UUID). Generic
  `jipipe:data` reconstruction is the FALLBACK only (derived patterns with no corpus instance; unmatchable dynamic-type
  nodes). 6 of the affected patterns contain `io-interface` - dropped (B2, below).
- **B2 drop set (user-ruled; the 6 patterns containing an `io-interface` node):** `dag-mined-cluster-0013`,
  `dag-mined-cluster-0029`, `dag-mined-cluster-0076`, `dag-mined-cluster-0215`, `dag-mined-cluster-0230`,
  `dag-derived-derived-mech-cluster-0029-2d-8c541287354e`.

**Tech Stack:** Python 3 generators (deterministic, fail-closed), Java 21, JUnit 5, JGraphT (Sugiyama reference impl).

**Spec:** `docs/superpowers/specs/2026-10-20-node-taxonomy-redesign-design.md` (patterns section) +
`utils/taxonomy-research/dag-multi-component-review.md` (plumbing-exclusion precedent) + this plan's audit notes.
Research artifacts are READ-ONLY.

## Global Constraints

- **Determinism:** generators byte-stable on re-run (sorted iteration, stable JSON). `SystemPatternTemplatesTest`
  (launcher) re-pins the set end-to-end.
- **Research dir read-only:** `utils/taxonomy-research/**` - the generators read it, never write it.
- **Grid-cell units:** stored locations are cells. Valid mined coords observed: x∈[0,62], y∈[0,66].
- **Sugiyama-equivalent layout (the "do what our Sugiyama impl does" rule):** replicate
  `SugiyamaGraphAutoLayoutImplementation.rearrangeSugiyamaVertical`'s arithmetic in the generators (it is UI-coupled; a
  pure port over the raw edge list is deterministic and, for these graphs, identical to the Java impl's output):
    1. Layer = longest-path depth (BFS; sources = 0; virtual nodes for long edges, exactly like the Java code).
    2. Within a layer: index = node-key sort order (deterministic; the Java impl uses set order, which is irrelevant for
       equality of the *spacing rules*).
    3. `x(layer) = 1 + Σ_{l' < layer} (maxNodeWidthCells(l') + 2)`, where
       `maxNodeWidthCells(l) = max over nodes in layer l of ceil(estimateNodeWidth(node) / 25)`.
    4. `y(index) = 1 + Σ_{i' < index} (nodeHeightCells(i') + 1)`, `nodeHeightCells = 3` (node = 3 grid rows tall;
       virtual rows use the real nodes' heights).
    5. Node-width estimate: 7 cells for most nodes; 10 cells for nodes whose final slot count (restored
       `jipipe:slot-configuration` if present, else `registry.json` `inputSlots`+`outputSlots`) is ≥ 4 (longest registry
       slot name ≥ ~20 chars → ~550px). The width map is computed AFTER the B1 restore pass (restored configs can add
       slots), so the layout sees the final slot counts.
    6. Free-floating nodes (no edges at all in the pattern): `x = 1 + Σ all real columns`, `y = 1 + 3·(index+1)` -
       mirroring the Java impl's free-floating stack (x = maxX + 4·GRID_WIDTH, y stepping by height + GRID_HEIGHT).
       Free-floating placement is only reachable if a pattern with an isolated node ships; the multi-component curation
       should prevent it, but the rule exists so the layout function is total.
- **B1 slot restoration (replaces what the minimal format dropped), corpus-instance method (user-ruled: full fidelity
  from the source corpus):** for every node in a shipped pattern whose edge-referenced slots are not all covered by its
  annotation-declared slots (generator-side approximation: `registry.json` `inputSlots`/`outputSlots` names; the
  launcher edge-survival test is the authoritative oracle):
    - **Match:** a corpus instance (any project under `corpusRoot`, indexed by node type) whose
      `jipipe:slot-configuration` covers the node's REQUIRED slot set = every slot name on a shipped edge endpoint for
      that node (source-end slots ⊆ instance `output` map, target-end slots ⊆ instance `input` map). Mined compartment
      UUIDs are synthetic (`_candidate_compartment_uuid`), so the match is by (type, required-slot-set) - NEVER by the
      shipped compartment UUID.
    - **Tie-break (determinism):** lexicographically-smallest (corpus-relative project path, compartment key, node
      UUID). Pre-verified: 88 of 93 affected nodes match; the 5 unmatched are 4 in `dag-mined-cluster-0013` (dropped by
      B2 anyway) + 1 (`derived-0096`, static type → hard-fail, see below).
    - **Copy (full fidelity):** the instance's full `jipipe:slot-configuration` (both `input` and `output` sides - real
      data types, e.g. `imagej-imgplus` for split-channels), plus the instance's **top-level non-`jipipe:` parameter
      keys verbatim** (the slot-driving parameters, e.g. `channel-to-slot-assignments/C1: 0` - without them the restored
      config's slots are never created). EXCLUDED from the copy: everything under `jipipe:` EXCEPT
      `jipipe:slot-configuration` itself (i.e. drop `jipipe:node:name`, `jipipe:graph-compartment`,
      `jipipe:metadata-v1`, `jipipe:ui-grid-location`, `jipipe:alias-id`, `jipipe:node:*`, `jipipe:algorithm:*`,
      `jipipe:parallelization:*`, `jipipe:data-batch-generation/*`, `jipipe:parameter-slot-algorithm/*` - the
      `{Parameters}` flag is the generator's existing (3b) pass's job), `jipipe:adaptive-parameters/*`.
    - **Fallback (only when no corpus instance matches):** dynamic-slot node types (the 6 `JIPipeIOSlotConfiguration`
      ids) → emit `jipipe:data` mirror slots for the required names (contiguous `Data 1..N` fill for the "Data k"
      pattern - 0176 references only "Data 2" → emit BOTH "Data 1" and "Data 2"). Static types with no corpus match →
      **fail the build** (never ship a dangling edge silently).
    - **Do NOT restore configs for nodes whose edges all resolve** - keep the minimal format for everything else
      (file-size + dedup-signature stability; the slot config is instance data only where needed).
    - **Implementation home:** a new standalone script `utils/restore_pattern_slot_configs.py` (reads `corpusRoot` from
      `dag-candidates.json`, patches the shipped files in `OUT_DIR` AFTER both generators run, fail-closed on unmatched
      static types, idempotent + deterministic, prints a per-pattern change report). Rationale: the DAG/linear
      generators work from research artifacts that do NOT carry slot configs; the corpus is only reachable from a
      dedicated pass.
- **No (node,slot) edge-dedupe guard - removed after verification (Ruling 11):** the original "0202 duplicate" concern
  was a misread of a merge node's legitimate multi-input fan-in. Verified across all 430 files: **0** have true
  full-tuple duplicate edges; **103** have legitimate fan-out (one source slot → many targets) and **8** legitimate
  fan-in (many sources → one input slot). Any (node,slot)-key dedupe would delete real edges - do not add one.
  (Observation for the final report: those 8 fan-in files include nodes whose single input slot receives 2-3 sources -
  e.g. `cluster-0111` `n2(Input)×2` - which the loader tolerates but is execution-invalid; out of scope for this fix,
  surfaced to the user.)
- **Shipped-set pins (exact, post-fix):** count **424** (was 430). DAG generator pins: `EXPECTED_MINED_SHIP = 262 → 257`
  (drop 0013/0029/0076/0215/0230), `EXPECTED_DERIVED_SHIP = 160 → 159` (drop the derived 0029 twin) → `EXPECTED_TOTAL`
  (pre item-3c) 422 → **416**; the item-3c drop pin at line ~413 (currently `{EXPECTED_TOTAL} - 16` → 406) becomes
  **400**; + 24 linear = **424**. Launcher `EXPECTED_SYSTEM_TEMPLATES = 424`. (The launcher has NO per-branch
  distribution assertions - verified in Task 1; nothing else to change there.)
- **Width-map ordering (Ruling 12):** the generators call `restore_pattern_slot_configs.analyze` on the shipped files to
  build the width map - but the shipped files are what the generators produce. Resolution: the SHIPPED SET is stable
  across this change (same 430 files pre-B2-drop; only contents change), so a scratch regeneration first is safe: (1)
  run each generator UNMODIFIED (or with only the B2 drop + writer changes, no location pass yet) against a scratch copy
  of `OUT_DIR` (the script's `OUT_DIR` must be overridable via a `--out` argument or env var for this purpose); (2) run
  `analyze` over the scratch files → width map; (3) run the real generators (with the location pass) using that width
  map; (4) run `restore`. If the scratch files' affected-node set diverges from the real run's (it must not - same input
  artifacts), the width map is stale: re-run (2)- (4) once and diff the outputs; any difference is a fail-closed stop,
  not a silent continuation.
- **Test policy (2026-10-20 ruling):** ship-set contract tests (serialization round-trip over the bundled set,
  fail-closed guards) are the kind of test that warrants coverage. No Swing interaction tests.
- **`registry.json` drift is environmental** (`RegistryDumperTest`):
  `git checkout -- utils/taxonomy-research/registry.json` after any launcher run, before committing.
- **No pushes** - branch `1324-node-taxonomy-core`, MR !168, user reviews.

## Review Focus

- **A user-saved template with a legacy (non-UUID) compartment name** must place nodes at their stored locations, not
  stack them - pinned by the core unit test (Task 3), since the launcher test only covers bundled data.
- **A dynamic-slot node whose edges reference "Data 2" but not "Data 1"** (0176) must still load with BOTH slots -
  pinned by the launcher edge-survival test + a generator assertion that 0176's shipped `jipipe:slot-configuration`
  input contains exactly `Data 1` and `Data 2`.
- **A corpus re-mine changing the shipped set** must fail the generators (count pins + per-node location presence +
  no-orphan-edge + no- (node,slot)-duplicate guards), not ship a silently-wrong set.
- **Mined locations must be byte-untouched:** the data fix rewrites locations ONLY in the 179 invalid files (155
  derived + 24 linear). The 251 mined files' `location/*` and `jipipe:graph-compartment` must not change (verified 0
  invalid mined locations). Separately, ~66 affected DAG files (59 mined + 7 derived) gain B1 slot restoration on
  specific nodes - those diffs must show NO location/compartment changes, only added `jipipe:slot-configuration` +
  driving-parameter keys. Pinned by the Task 2 Step 5 python check.
- **The 6 dropped io-interface patterns** must leave no orphan resources (their `.json` files deleted, sidecar entries
  removed) - pinned by `SystemPatternTemplatesTest` count + the generator sweep.

---

## Task 1: Failing contract tests (launcher) - resolvable locations + surviving edges

**Files:**

- Modify: `jipipe-launcher/src/test/java/org/hkijena/jipipe/plugins/nodetemplate/SystemPatternTemplatesTest.java`

**Interfaces:**

- Consumes: the bundled resources (classpath `/org/hkijena/jipipe/plugins/nodetemplate/patterns/`); `JIPipeGraph`
  Jackson deserialization (the `JIPipeNodeTemplate.getGraph()` path -
  `JsonUtils.readFromString(json, JIPipeGraph.class)`); `JIPipeGraphNode.getNodeUILocationWithin(String)`;
  `JIPipeGraph.getCompartmentUUIDOf(node)`; `JIPipeGraph.getSlotEdges()` (cross-node edges only - Ruling 1) and the raw
  tree's `edges` array; `JIPipeNodesServiceComponent.hasNodeInfoWithId(String)` (the version-gap tolerance, Ruling 8).
- Produces: `@Test everyBundledPatternNodeHasAResolvableLocation`,
  `@Test everyBundledPatternEdgeSurvivesDeserialization`. Both RED now, GREEN after Task 2. Also change
  `EXPECTED_SYSTEM_TEMPLATES` 430 → 424 in this task (it goes red on the drop until Task 2 ships the new set - sequence:
  change the constant, keep the old test methods, add the two new ones; ALL red is expected and recorded).

- [ ] **Step 1: Add the two contract tests**

Both iterate the bundled graph files exactly as the existing tests do (sidecar `file` entries).

`everyBundledPatternNodeHasAResolvableLocation`:

```java
JsonNode tree = JsonUtils.getObjectMapper().readTree(bytes);
JIPipeGraph graph = JsonUtils.readFromString(new String(bytes, StandardCharsets.UTF_8), JIPipeGraph.class);
for (JIPipeGraphNode node : graph.getGraphNodes()) {
    String key = StringUtils.nullToEmpty(graph.getCompartmentUUIDOf(node));
    assertNotNull(node.getNodeUILocationWithin(key),
        f + ": node " + node.getName() + " has no resolvable location (compartment key " + key + ")");
}
```

(Assert resolvability only - NOT spacing/overlap: 36 of the 251 mined files have user layouts with nodes closer than any
fixed footprint, so a spacing assertion false-fails. Spacing correctness of the *generated* files is guaranteed by
construction - same longest-path layering the app's own Sugiyama impl uses - and eyeballed in-app.)

`everyBundledPatternEdgeSurvivesDeserialization`:

```java
assertEquals(tree.get("edges").size(), graph.getSlotEdges().size(),
    f + ": " + (tree.get("edges").size() - graph.getSlotEdges().size()) + " edge(s) dropped on load (missing slot / missing node)");
```

**Metric is `getSlotEdges()` (cross-node edges only), NOT `getGraph().edgeSet().size()`:** `repairGraph()` adds per-node
internal input→output edges to the JGraphT graph, so `edgeSet().size()` = cross + internal and can never equal the raw
cross-edge count (verified in `JIPipeGraph.java` - `getSlotEdges():983` skips same-node edges and is the graph's own
serializer's metric). Ruling 1.

- [ ] **Step 2: Change the count pin and the per-branch distribution pins to the post-fix values** (424; images 109,
  rois 87, export 53, plotting 20 - verify the exact assertion sites at ~lines 210-235 and adjust each; if the launcher
  asserts the distribution as a map, update all four entries).

- [ ] **Step 3: Run - expect RED, record the exact failures**

Run: `mvn -o -pl jipipe-launcher test -Dtest=SystemPatternTemplatesTest`
Expected: `everyBundledPatternNodeHasAResolvableLocation` fails (179 files: the 24 linear sort first in sidecar order -
e.g. `annotation-sandwich-import-file-list-path-to-annotation-simple-other.json` - then the 155 `dag-derived-*`);
`everyBundledPatternEdgeSurvivesDeserialization` fails (~66 files, ~153 dropped edges: parameter-driven named slots +
dynamic types); the two count tests fail (430 on disk ≠ 424 expected). Record the failing counts + first failing file +
the node-info-ids reported. Note: the pre-flight scan found NO per-branch distribution assertion in the launcher - Step
2's distribution updates are a no-op; only `EXPECTED_SYSTEM_TEMPLATES` changes.

- [ ] **Step 4: Commit the red tests**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/plugins/nodetemplate/SystemPatternTemplatesTest.java
git commit -m "test: contract pins for bundled patterns - resolvable locations, surviving edges, 424-set (red) (6.0.0)"
```

---

## Task 2: Data fix - Sugiyama layout for the 179 invalid files, B1 slot configs, B2 drop; regenerate

**Files:**

- Modify: `utils/generate-dag-system-template-metadata.py` - the mined/derived ship path (add: location validation +
  re-layout, B2 drop, updated `EXPECTED_*` pins), the §3.4 guard (~440-449)
- Modify: `utils/generate-system-template-metadata.py` - the linear write loop (~594-610): add the layout pass +
  location guard + writer preservation + io-interface fail-closed guard
- Create: `utils/sugiyama_layout.py` - the shared layout function (the Global-Constraints rule, one implementation, used
  by both generators) + `utils/sugiyama_layout_tests.py` (its unit tests)
- Create: `utils/restore_pattern_slot_configs.py` - the B1 corpus-restore pass (Global-Constraints B1 rule) +
  `utils/restore_pattern_slot_configs_tests.py`
- Regenerate: `jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/*.json` (179 rewrites + ~
  66-69 slot-restore edits on DAG files (static proxy: 59 mined + 7 derived; JVM-measured: 60 mined + 9 derived - the
  restore script's report is authoritative) + 6 deletions + sidecar rewrite; every non-affected file byte-identical)

**Interfaces:**

- Consumes: Task 1 tests as the acceptance oracle. Research inputs: `dag-candidates.json` / `dag-clusters.json` /
  `dag-derived.json` / `dag-judgments.json` (mined + derived source graphs), `registry.json` (node slot tables for width
  estimation), `patterns/templates/*.json` (linear minimal graphs). `sugiyama_layout.py` public API:
  `layout(graph: dict, node_width_cells: dict[str, int]) -> dict[node_key, (x, y)]` (graph =
  `{"nodes": {key: {...}}, "edges": [...]}`; returns grid-cell coords; deterministic).
- Produces: shipped files where every node has `location/<ownCompartmentKey>/x|y` in cells under a resolvable key;
  B1-affected nodes carry the corpus-restored `jipipe:slot-configuration` (+ driving parameters); the 6 B2 patterns are
  absent (files + sidecar).

- [ ] **Step 1: Write `utils/sugiyama_layout.py` + its tests**

`sugiyama_layout_tests.py`: plain asserts, run with `python3 utils/sugiyama_layout_tests.py` (mirrors the repo's
`*_tests.py` convention). Cases:

- linear 3-node chain → `[(1, 1), (1 + w0 + 2, 1), (1 + w0 + 2 + w1 + 2, 1)]` with width w=7 → x = 1, 10, 19, y = 1,1,1.
- diamond (a→b, a→c, b→d, c→d) → b and c same layer, different y (y = 1 and 1+3+1 = 5); d at layer 2, single row.
- long edge a→c (b in between at layer 1 via another path) → virtual node handling: c's layer = 2, x steps twice.
- isolated node + a chain → isolated node placed after the last column (free-floating rule).
- determinism: same input twice → identical output (dict equality).
  Implement `layout` per the Global-Constraints rule. Widths come in as a dict; the function itself never touches the
  registry.

- [ ] **Step 2: Run the layout tests - green before use**

Run: `python3 utils/sugiyama_layout_tests.py`
Expected: all asserts pass.

- [ ] **Step 3: Write `utils/restore_pattern_slot_configs.py` (the B1 corpus-restore pass) + its test**

Per the Global-Constraints B1 rule: index the corpus (`corpusRoot` from `utils/taxonomy-research/dag-candidates.json`)
by node type → instances with `jipipe:slot-configuration`; for each shipped DAG/linear file in `OUT_DIR`, for each node
whose required slot set (from the file's edges) exceeds its `registry.json` static slots, find the deterministic match
(type + slot-set coverage, tie-break = lex-smallest (project, compartment, uuid)) and patch the file: add
`jipipe:slot-configuration` + the instance's top-level non-`jipipe:` parameter keys (exclusions per the rule). Dynamic
types with no match → generic `jipipe:data` fallback (contiguous `Data 1..N`). Static types with no match → `fail(...)`
(the known case: `derived-0096` `ij1-roi-count` slot `Roi2dListData` - if it fires, that file must be handled by a
recorded ruling, not a silent skip). Idempotent (re-run on already-patched files = no-op: a node with a config covering
its required set is skipped). Print a per-pattern change report (file, node, matched source project, slots added).
The script MUST expose the two importable functions named in the Global-Constraints "Restore-script API" bullet -
`analyze(out_dir, corpus_root, registry)` (pure, returns per-file/per-node extra slot counts; used by the generators'
width maps) and `restore(out_dir, corpus_root, registry)` (the patching pass; `main()` runs it). Implement `analyze`
FIRST; `restore` reuses its matching logic.
`utils/restore_pattern_slot_configs_tests.py`: a synthetic mini-corpus in a temp dir (2 projects, 3 node types: one
parameter-driven, one dynamic, one unmatched-static) + a synthetic shipped file; asserts the deterministic pick, the
param-copy exclusions, the fallback, and the hard-fail. Run with `python3 utils/restore_pattern_slot_configs_tests.py`.
NOTE: the DAG/linear generator changes below (Steps 4-5) are what make the generator output correct; the B1 slot
restoration itself is the separate restore pass from Step 3, run in Step 6.

- [ ] **Step 4: Wire the layout + guards + B2 drop into the DAG generator**

In `utils/generate-dag-system-template-metadata.py`:

- **B2 drop:** after the existing item-3c drop pass, remove any record whose graph contains a node with
  `jipipe:node-info-id == "io-interface"` (the 6 patterns). Fail-closed: the drop count must equal the
  `EXPECTED_IOINTERFACE_DROP = 6` pin (a corpus re-mine that changes it fails loudly). Bump the pins:
  `EXPECTED_MINED_SHIP = 257`, `EXPECTED_DERIVED_SHIP = 159` (and the resulting staged total 416 → shipped 400 after
  item-3c; if the generator asserts an intermediate shipped-DAG total, update it to 400).
- **Location pass (per record, before writing):** a node's location is *valid* iff `jipipe:graph-compartment` is a UUID
  (or empty/`"DEFAULT"`) AND `location/<that key>/x|y` both exist. Mined nodes are valid (verified 0 invalid) → keep
  verbatim. Invalid (all derived + none mined) → re-layout the WHOLE pattern with `sugiyama_layout` and rewrite every
  node's `jipipe:metadata-v1` to `{"location/<comp>/x": x, "location/<comp>/y": y}` where `<comp>` is the node's
  existing compartment value, EXCEPT: for derived files (`comp == "derived"`) also rewrite `jipipe:graph-compartment` to
  `"DEFAULT"` (the resolvable default-compartment marker; `nodesFromJson` keeps default-compartment nodes as-is and the
  existing "DEFAULT" location migration, JIPipeGraph.java:1177, promotes the key). Width map for the layout: FINAL
  per-node slot count = `registry.json` static count + the extras `restore_pattern_slot_configs.analyze` reports for
  that node (Global-Constraints Restore-script API) - so spacing accounts for restored slots. NOTE: do NOT add any
  edge-dedupe pass (Ruling 11 - verified harmful).
- **§3.4 guard (strengthen, ~440-449):** keep the location-presence assertion; ADD: compartment key must be UUID/
  `"DEFAULT"`/empty (legacy names fail); coords must be cell-plausible (`0 <= x <= 400`, `0 <= y <= 400` - pixel-era
  1250 fails). (The no-orphan-edge condition is enforced by the RESTORE pass + the launcher edge test, not here - the
  generator cannot see final slot configs.)
- **Writer:** do NOT strip `jipipe:slot-configuration` or top-level non-`jipipe:` parameter keys if present (they are
  functional data after the restore pass - the minimalizer must preserve them; today it drops only `jipipe:node:name`).

- [ ] **Step 5: Wire the layout + guards into the linear generator**

In `utils/generate-system-template-metadata.py`'s write loop: after stripping `jipipe:node:name`, apply the SAME
location pass (all 24 linear files are invalid - no locations - so all 24 re-layout via `sugiyama_layout`; their
compartments are UUIDs → kept as-is; width map via the same `analyze` call). No edge dedupe (Ruling 11). (No linear file
contains io-interface or any dropped-edge node - verified - but the B2 drop guard ships live: any pattern with an
`io-interface` node fails the build.)

- [ ] **Step 6: Regenerate + restore + verify independently**

```bash
cd /data/src/jipipe-4
python3 utils/generate-system-template-metadata.py
python3 utils/generate-dag-system-template-metadata.py
python3 utils/restore_pattern_slot_configs.py     # B1 pass (idempotent)
python3 utils/restore_pattern_slot_configs.py     # second run MUST be a no-op (idempotence check)
```

Both must print OK (or fail loudly - do NOT weaken a guard to pass). Then:

```bash
python3 - <<'EOF'
import json, os, uuid
d = 'jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns'
def is_uuid(s):
    try: uuid.UUID(s); return True
    except Exception: return False
bad = 0; files = 0; mined_changed = []
prev = {}  # captured BEFORE regen via git
for f in sorted(os.listdir(d)):
    if not f.endswith('.json') or f == 'system-templates.json': continue
    files += 1
    g = json.load(open(os.path.join(d, f)))
    if 'io-interface' in {n['jipipe:node-info-id'] for n in g['nodes'].values()}:
        print('IO-INTERFACE SURVIVED', f); bad += 1
    for nid, n in g['nodes'].items():
        comp = n.get('jipipe:graph-compartment', '')
        md = n.get('jipipe:metadata-v1') or {}
        if not (is_uuid(comp) or comp in ('DEFAULT', '')):
            print('BAD COMPARTMENT', f, nid, comp); bad += 1
        if not (f'location/{comp}/x' in md and f'location/{comp}/y' in md):
            print('NO LOCATION', f, nid, comp); bad += 1
print('files:', files, 'bad:', bad)
EOF
git status --short jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/ | wc -l
```

Expected: `bad: 0`; the restore pass's change report lists the affected files (static proxy says 66: 59 mined + 7
derived; Task 1's JVM measurement says 69: 60 mined + 9 derived - the restore script's OWN report is the authoritative
record; investigate only if it diverges by >2 from the proxy); second restore run reports 0 changes (idempotent).
Modified paths ≈ 179 (re-layout: 155 derived + 24 linear) + ~60 (B1 slot restoration on mined files NOT already
re-laid-out; affected derived files overlap the 179) + 6 (deletions) + 1 (sidecar) ≈ **246**. Confirm via `git status` +
`git diff` that every `dag-mined-*` diff contains ONLY added `jipipe:slot-configuration` / driving-parameter keys (no
location, compartment, or node-id changes), and that every non-affected file is byte-identical.
Known-fail watch: `restore_pattern_slot_configs.py` hard-fails on unmatched STATIC types - the pre-verified case is
`dag-derived-derived-wn-cluster-0096-27-3250df34394f.json` (`ij1-roi-count`, required input slot `Roi2dListData` - a
class name, no corpus instance covers it). When it fires: that one file's edge cannot be restored faithfully. The ruling
for it is pre-made: the node's input slot is declared in code with an EMPTY name (registry:
`inputSlots: [{class: Roi2dListData, name: ""}]`) - restore by emitting a `jipipe:slot-configuration` input named
`Roi2dListData` of type `imagej-roi` (the registry `dataType` for that class) for THIS node only, via an explicit
per-file exception table in the restore script (documented, not a silent skip). If the launcher edge test still drops
that edge, the file falls back to the B2-style drop only with a fresh ruling.

- [ ] **Step 7: Run the Task 1 tests - expect GREEN**

Run: `mvn -o -pl jipipe-launcher test -Dtest=SystemPatternTemplatesTest`
Expected: all green, including the 424 count + the two new contract tests.

- [ ] **Step 8: Commit**

```bash
git checkout -- utils/taxonomy-research/registry.json 2>/dev/null
git add utils/sugiyama_layout.py utils/sugiyama_layout_tests.py utils/restore_pattern_slot_configs.py utils/restore_pattern_slot_configs_tests.py utils/generate-dag-system-template-metadata.py utils/generate-system-template-metadata.py jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/
git commit -m "fix: bundled pattern locations (Sugiyama cell layout, 179 files), restore slot configs from source corpus for 66 patterns, drop 6 io-interface patterns (430 -> 424) (6.0.0)"
```

---

## Task 3: Code defense - legacy-compartment location rekey at load

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeGraph.java` - `nodesFromJson` (~1177-1181)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/LegacyCompartmentLocationTest.java` (new)

**Interfaces:**

- Consumes: `JIPipeGraph.nodeLegacyCompartmentIDs` (populated at load for non-UUID, non-`"DEFAULT"` compartment names -
  getter at ~1826); `JIPipeGraphNode.getAllNodeUILocations()` / `setNodeUILocationWithin(String, Point)` /
  `getNodeMetadata().clearEntriesWithPathPrefix(JIPipePathMetadataStore.key("location", name))`.
- Produces: `JIPipeGraph` re-keys `location/<legacyName>/x|y` → the default-compartment key at load time for ANY legacy
  name (generalizing the existing `"DEFAULT"`-only migration). This covers user-saved templates/projects with legacy
  compartments - not just bundled data.

**Why only the loader, not the template rekey:** the rekey
(`CreateNewNodesByTemplateDatabaseEntry.rekeyTemplateGraphToCompartment`) operates on a `duplicate()` of the parsed
graph - after `nodesFromJson` has run - so the loader fix already hands it a resolvable location. Adding a second lookup
there would be dead code. (The earlier "harden the rekey" idea is superseded by this: fix the source, keep the consumer
simple.)

- [ ] **Step 1: Write the failing test**

```java
@Test
void legacyCompartmentLocationIsRekeyedToDefaultAtLoad() throws Exception {
    String json = "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"parameters-define\","
        + "\"jipipe:graph-compartment\":\"derived\","
        + "\"jipipe:metadata-v1\":{\"location/derived/x\":12,\"location/derived/y\":5}}},"
        + "\"edges\":[]}";
    JIPipeGraph graph = JsonUtils.readFromString(json, JIPipeGraph.class);
    JIPipeGraphNode node = graph.getGraphNodes().iterator().next();
    assertNull(graph.getCompartmentUUIDOf(node), "legacy name demotes to the default compartment");
    assertEquals(new Point(12, 5), node.getNodeUILocationWithin(""),
        "location must resolve under the default compartment key");
}
```

(`parameters-define` is core-registered - verified via `CorePlugin`. `JIPipe.ensureInstance()` required: add the same
`@BeforeAll` the other core graph tests use; the suite's `ImageJLegacyPatcherLauncherSessionListener` handles patcher
ordering - do NOT add your own preinit.)

- [ ] **Step 2: Run - expect FAIL** (`getNodeUILocationWithin("")` → null; location stranded under `location/derived/`).

Run: `mvn -o -pl jipipe-core test -Dtest=LegacyCompartmentLocationTest`

- [ ] **Step 3: Generalize the migration in `nodesFromJson`**

Replace the `"DEFAULT"`-only block with: after `insertNode`, read
`String legacy = nodeLegacyCompartmentIDs.get(nodeUUID)`; the migration key is `legacy != null ? legacy : "DEFAULT"`; if
`node.getAllNodeUILocations()` contains that key, `node.setNodeUILocationWithin("", point)` and clear the
`location/<key>` prefix from the node metadata. (`"DEFAULT"` stays a special case: it is NOT stored in the legacy map,
so the fallback preserves today's behavior exactly.)

- [ ] **Step 4: Run - expect PASS.**

Run: `mvn -o -pl jipipe-core test -Dtest=LegacyCompartmentLocationTest`

- [ ] **Step 5: Full core suite (merge gate)**

Run: `mvn -o -pl jipipe-core clean test`
Expected: BUILD SUCCESS, 0 failures, 0 errors, 1 known skip; count = previous baseline + 1 (the new test method).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeGraph.java jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/LegacyCompartmentLocationTest.java
git commit -m "fix: rekey legacy-compartment node locations to the default compartment at load (generalizes the DEFAULT migration) (6.0.0)"
```

---

## Task 4: Final gate - full suites + tree clean + report

**Files:** none (verification only).

- [ ] **Step 1: Launcher suite (clean)**

Run: `mvn -o -pl jipipe-launcher clean test`
Expected: BUILD SUCCESS, 0 failures/errors. Note the SystemPatternTemplatesTest count (was 16; +2 new = 18) and total.

- [ ] **Step 2: Core suite (clean, authoritative)**

Run: `mvn -o -pl jipipe-core clean test`
Expected: BUILD SUCCESS, 0 failures, 0 errors, 1 known skip.

- [ ] **Step 3: Restore environmental drift, confirm clean tree**

```bash
git checkout -- utils/taxonomy-research/registry.json 2>/dev/null
git status --short
```

Expected: clean (no modified files).

- [ ] **Step 4: Report to the user for in-app verification**

Summarize: (a) 179 files re-laid out with the Sugiyama cell convention (155 derived - rekeyed to `"DEFAULT"` AND
rescaled from pixels to cells; 24 linear - newly laid out), 251 mined files' locations untouched; (b) ~66 patterns fixed
by corpus slot-config restoration (list with display names - the parameter-driven + dynamic-type set, incl. "Add
computed annotations to data"); (c) 6 `io-interface` patterns dropped (names listed); (d) the loader defense for legacy
compartments; (e) suite evidence. Ask the user to spot-check: apply **"16-bit distance map from binary image"** (was
stacked → now two spaced nodes) and **"Add computed annotations to data"** (floating "Merge annotations" → now
connected), plus one dropped name is gone from the pattern tree.
