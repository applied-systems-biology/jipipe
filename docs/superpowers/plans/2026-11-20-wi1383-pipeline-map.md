# #1383 Sub-project D - Pipeline Map Enrichment and describe_project Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `get_pipeline_map` a real map (compartment-to-compartment edges from the compartment graph, validation+cache status with machine-readable counts, version 1) and add the `describe_project(detail)` one-call composite.

**Architecture:** All reads live in shared statics of `JIPipeAutomationAPI` so `get_pipeline_map` and `describe_project` cannot drift: one map builder (edges + status + counts), one metadata builder (extracted from `GetProjectInfo`), one file-writer filter. `SegmentStatusLogic` is a pure mapping helper in the `pipeline_map` DTO package. `PipelineMapOperations.GetPipelineMap` keeps delegating; a new `DescribeProject` operation composes the statics.

**Tech Stack:** Java 21, Jackson databind, JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-20-wi1383-pipeline-map-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- Branch `1383-pipeline-map` (from post-B master `5bdc2b80c4`; spec commit `7d0a3c12f4`).
- Registered ids stay stable string literals (`"describe_project"`).
- Both operations: Query category, read-only, no `requiresEdt` override (all reads are headless-safe; keep the house default).
- No scope overrides: both ops are project-bound (C's `AutomationToolScope` default applies; do NOT add `getToolScope` overrides).
- Wire shape: `PipelineMap.version` becomes 1; `SegmentNode` gains `cachedNodes`/`totalNodes` (Integer, null on the root, omitted by the class-level `@JsonInclude(NON_NULL)`); `SegmentEdge` is NOT extended (source/target = compartment UUID strings, label = source output slot name).
- Status mapping (binding, implemented once in `SegmentStatusLogic.compute(errorCount, warningCount, cachedNodes, totalNodes)`): `errorCount > 0` -> ERROR; else `cachedNodes == totalNodes && totalNodes > 0 && warningCount == 0` -> OK; else `cachedNodes > 0 || warningCount > 0` -> WARNING; else UNKNOWN.
- House test policy: no Swing interaction tests; no tests that restate the implementation. D has NO by-eye items.
- Test command per task: `mvn -q test -pl jipipe-core -Dtest=<TaskTests>,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot; wait out concurrent mvn, never kill. Full-suite verification uses `mvn -q clean test -pl jipipe-core` (standing ruling: branch switches leave stale bytecode).

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **A compartment-graph vertex that is not a `JIPipeProjectCompartment`** (defensive; the graph normally holds only compartments) must be skipped, not crash the map. Pinned in Task 2 (`edgesSkipNonCompartmentVertices`).
2. **Two output slots on one source compartment produce TWO edges to the same target** (the real hand-off shape; a Map-based implementation would collapse them). Pinned in Task 2 (`twoOutputSlotsProduceTwoEdges`).
3. **Validation failure must not kill the map** - `reportValidity` throwing degrades to counts 0 (statuses then reflect cache only), never an error envelope. Ruling (not in the spec verbatim): catch, continue. Pinned in Task 2 via the pure-helper boundary only (the throw path itself is the integration try/catch; asserted by code review, listed here for the reviewer).
4. **`cache.query` on a node with nothing stored returns an empty map, and an empty compartment must be UNKNOWN, never OK** (the `totalNodes > 0` guard). Pinned in Task 2 (`emptyCompartmentIsUnknown`).
5. **The metadata extraction must be behavior-identical** - `get_project_info`'s response must not change while its body is refactored into the shared static. Pinned in Task 3 (`getProjectInfoResponseIsUnchangedByTheExtraction` asserting the full metadata block on a populated project).

---

### Task 1: `SegmentStatusLogic` + `SegmentNode` count fields

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentStatusLogic.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentNode.java` (two fields after `status`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentStatusLogicTest.java` (create)

**Interfaces:**
- Produces (Task 2 consumes): `SegmentStatusLogic.compute(int errorCount, int warningCount, int cachedNodes, int totalNodes)` -> `SegmentStatus`, the Global Constraints mapping verbatim; `SegmentNode.setCachedNodes(Integer)`/`setTotalNodes(Integer)` + getters.

- [ ] **Step 1: Write the failing tests** (`SegmentStatusLogicTest`, plain JUnit, no fixtures)

```java
@Test void errorsDominate()                  // (1, 0, 5, 5) -> ERROR; (2, 3, 0, 4) -> ERROR
@Test void fullCleanCacheIsOk()              // (0, 0, 4, 4) -> OK
@Test void partialCacheIsWarning()           // (0, 0, 2, 4) -> WARNING
@Test void warningsWithFullCacheAreWarning() // (0, 1, 4, 4) -> WARNING (OK requires warningCount == 0)
@Test void warningsWithoutCacheAreWarning()  // (0, 2, 0, 3) -> WARNING
@Test void nothingCachedIsUnknown()          // (0, 0, 0, 3) -> UNKNOWN
@Test void emptyCompartmentIsUnknown()       // (0, 0, 0, 0) -> UNKNOWN (totalNodes 0 guard)
```

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=SegmentStatusLogicTest` -> COMPILATION ERROR (class missing).

- [ ] **Step 3: Implement** - `SegmentStatusLogic`: public final class, private constructor, the single static `compute` implementing the Global Constraints mapping exactly (if/else in the stated order; no Swing imports). `SegmentNode`: `private Integer cachedNodes;` + `private Integer totalNodes;` with getters/setters, placed after the `status` accessors; class-level `@JsonInclude(NON_NULL)` already omits nulls.

- [ ] **Step 4: Run to verify pass** - same command -> PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentStatusLogic.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentNode.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/pipeline_map/SegmentStatusLogicTest.java
git commit -m "feat(#1383): add the segment status logic and count fields"
```

### Task 2: `getPipelineMap` enrichment (edges + status + version)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`getPipelineMap` :656-676, rebuilt in place)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java` (`GetPipelineMap.getDescription()`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperationsTest.java` (create)

**Interfaces:**
- Consumes: Task 1's `SegmentStatusLogic.compute` and `SegmentNode` setters; `project.getCompartmentGraph()` (`JIPipeProject:910`), `graph.getEdgeSource/getEdgeTarget(edge)` -> `JIPipeDataSlot`, `slot.getNode()` (`JIPipeDataSlot:139`), `JIPipeProjectCompartment.getProjectCompartmentUUID()`, `project.reportValidity(...)` + `entryNode`-style context traversal (`entry.getContext().traverse()` for `GraphNodeValidationReportContext`, the `DiagnosticsOperations:80-87` pattern), `reportEntry.getLevel()` (`JIPipeValidationReportEntryLevel.Error`/`Warning`), `project.getCache().query(node, nodeUUID, silent)` (`JIPipeLocalProjectMemoryCache:281`, empty map when nothing stored), `graph.getUUIDOf(node)`.
- Produces (Task 3 consumes): the enriched `getPipelineMap(ctx)` response (version 1, edges, per-compartment `status`/`cachedNodes`/`totalNodes`); the internal map-building is structured so Task 3 can reuse it (see Step 3 note).

- [ ] **Step 1: Write the failing tests** (`PipelineMapOperationsTest`, modeled on the sibling operations tests' `ensureJIPipe`/`createContext`/`createProject` helpers)

Test setup facts (pin these; verify against the named sources while writing):
- `project.addCompartment(name)` returns the `JIPipeProjectCompartment` (and registers it in the compartment graph, `JIPipeProject:622-631`).
- A compartment gains an output slot via `compartment.getSlotConfiguration().addOutputSlot("Out", "...", JIPipeCompartmentOutputData.class, true)` (`JIPipeDefaultMutableSlotConfiguration:277`; the slot-config-changed event then auto-runs `project.updateCompartmentOutputs` because `addCompartment` set the runtime project, `JIPipeProjectCompartment:112-120`).
- `project.connectCompartments(source, target)` connects the source's first output slot to the target's `Input` slot (`JIPipeProject:635-640`).
- A node is inserted with `project.getGraph().insertNode(node, compartmentUUID)`; cache is stored with `project.getCache().store(node, project.getGraph().getUUIDOf(node), new JIPipeDataTable(), "Out", new JIPipeProgressInfo())` (store signature `JIPipeLocalProjectMemoryCache:78`; `new JIPipeDataTable()` exists, `JIPipeDataTable:76`). Use a minimal test node the way `ParameterIntrospectionOperationsTest` defines its inner node classes.

```java
@Test void unconnectedProjectHasEmptyEdgesAndVersionOne() // 2 compartments, not connected
    // -> map.version == 1; map.edges empty; each segment has totalNodes == 0, cachedNodes == 0, status UNKNOWN

@Test void connectedCompartmentsProduceEdges() // A gains output slot "Out", connectCompartments(A, B)
    // -> exactly one edge: source == A's compartment UUID string, target == B's, label == "Out"

@Test void twoOutputSlotsProduceTwoEdges() // A gains "Out1" and "Out2", one connectCompartments call
    // -> 2 edges, both A -> B, labels {Out1, Out2}

@Test void edgesSkipNonCompartmentVertices() // construct the map from a graph where an edge's
    // endpoint node is NOT a JIPipeProjectCompartment (e.g. insert a plain test node into the
    // compartment graph and connect it) -> that edge is skipped, no exception, other edges intact

@Test void cachedCompartmentIsOkAndCountsAreRight() // compartment with 2 nodes, store cache for both
    // -> status OK, cachedNodes == 2, totalNodes == 2, summary mentions "2 nodes, 2 cached"

@Test void partiallyCachedCompartmentIsWarning() // 2 nodes, 1 stored
    // -> status WARNING, cachedNodes == 1, totalNodes == 2

@Test void emptyCompartmentIsUnknown() // compartment with 0 nodes
    // -> status UNKNOWN, cachedNodes == 0, totalNodes == 0
```

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=PipelineMapOperationsTest` -> COMPILATION/ASSERTION failures (version still 0, edges always empty, no count fields on segments).

- [ ] **Step 3: Implement in `JIPipeAutomationAPI.getPipelineMap`**

Structure the method as public entry + private helpers so Task 3 reuses the pieces without re-walking:
- `private static void fillValidationCounts(project, graph, perCompartmentErrorCount, perCompartmentWarningCount)`: one `project.reportValidity(new UnspecifiedValidationReportContext(), JIPipeValidationReportSettings.DEFAULT, report, new JIPipeProgressInfo())` in a try/catch - on exception, leave both count maps empty (ruling: a validation hiccup degrades the status to cache-only, it never fails the map). For each entry, attribute the node via the `entry.getContext().traverse()` `GraphNodeValidationReportContext` walk (the `DiagnosticsOperations:80-87` pattern, reimplemented locally - do not widen DiagnosticsOperations' private helper) and `graph.getCompartmentUUIDOf(node)`; increment `Error`/`Warning` counters by `reportEntry.getLevel()`.
- Edge loop: for each edge of `project.getCompartmentGraph().edgeSet()`, resolve source/target slots and their nodes; skip any endpoint whose node is not `instanceof JIPipeProjectCompartment`; add `SegmentEdge(sourceCompartmentUuid, targetCompartmentUuid, sourceSlot.getName())`.
- Compartment loop (existing structure kept): per compartment also compute `totalNodes`, `cachedNodes` (count of nodes whose `cache.query(node, graph.getUUIDOf(node), new JIPipeProgressInfo())` is non-empty), the two validation counts, then `segment.setStatus(SegmentStatusLogic.compute(...))` and set the count fields; `summary` = `nodes.size() + " nodes, " + cachedNodes + " cached"`.
- Build with `new PipelineMap(1, root)` (the `@JsonCreator(version, root)` constructor).

- [ ] **Step 4: Update `GetPipelineMap.getDescription()`** to the truthful catalog text (pin verbatim): "Get a hierarchical map of the pipeline: one segment per compartment with its nodes, the compartment-to-compartment wiring (edges: source and target compartment UUID, label = the source compartment's output slot name), and a status per compartment. Status mapping: ERROR = validation errors in the compartment; OK = all nodes cached and no validation issues; WARNING = some nodes cached or validation warnings; UNKNOWN = nothing cached and no issues. cachedNodes/totalNodes carry the exact cache coverage". Schema stays `emptyObjectSchema()`.

- [ ] **Step 5: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=PipelineMapOperationsTest,SegmentStatusLogicTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperationsTest.java
git commit -m "feat(#1383): populate the pipeline map with compartment wiring and real segment status"
```

### Task 3: `describe_project` composite

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `projectMetadata(project)` + `describeProject(ctx, detail)` statics; the metadata block moves out of `GetProjectInfo`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (`GetProjectInfo.execute` delegates to the shared static; `authorsToArray` moves with it)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java` (new `DescribeProject` class after `GetSegmentDetail`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java:110-111` (register after `get_segment_detail`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperationsTest.java` (extend)

**Interfaces:**
- Consumes: Task 2's map builder internals (compartment walk, edges, status/counts), `JIPipeAutomationAPI.queryAnnotations(ctx, null, null)` (`:347`), the `GetProjectInfo` metadata block (`InfoOperations:244-260`).
- Produces: operation id `describe_project`, schema `{"detail": enum summary|full, default summary}` + `additionalProperties: false`, Query/read-only/no-EDT; response sections `name`, `path`, `metadata`, `compartments`, `wiring`, `annotations`, `fileWritingNodes` (+ `nodes` per compartment when `detail: full`).

- [ ] **Step 1: Write the failing tests** (extend `PipelineMapOperationsTest`)

```java
@Test void getProjectInfoResponseIsUnchangedByTheExtraction() // project with populated metadata
    // (name, description, license, summary, website, citation, one author, one acknowledgement)
    // -> new InfoOperations.GetProjectInfo().execute(ctx, {}) carries the identical metadata
    // block as before the refactor (assert every metadata key's value; behavior-identity pin)

@Test void describeProjectSummaryHasAllSections() // 2 compartments (one connected), 3 test nodes with
    // output slots in one compartment: nodeA defaults (qualifies), nodeB skipExport=true (does not
    // qualify: slot.setSkipExport(true)), nodeC slot storeToDisk=false via getSlotConfiguration's
    // slot info (does not qualify) - the store-to-disk x skip-export matrix through the composite
    // -> name/path present; metadata has license/authors keys; compartments entries carry
    //    id/name/nodeCount/cachedNodes/totalNodes/status/fileWriterCount and nodeCount matches
    //    get_project_info's compartments; wiring matches the map's edges exactly (same
    //    source/target/label list); annotations array present; fileWritingNodes contains nodeA's
    //    alias with its slot names but NOT nodeB/nodeC; compartment fileWriterCount sums ==
    //    fileWritingNodes size

@Test void describeProjectFullAddsNodeLists() // detail: "full"
    // -> each compartments entry additionally has nodes: [{id, name, typeId}] matching the
    //    compartment's project-graph nodes

@Test void describeProjectDetailDefaultsToSummary() // no detail param
    // -> compartments entries have NO "nodes" key
```

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=PipelineMapOperationsTest` -> COMPILATION ERROR (`DescribeProject` missing; the extraction pin may pass already if written before the refactor - note the actual red set in the report).

- [ ] **Step 3: Extract `projectMetadata` and implement `describeProject`**

- `JIPipeAutomationAPI.projectMetadata(JIPipeProject project)` -> `ObjectNode`: the exact block from `GetProjectInfo.execute` (`InfoOperations:244-260`: description, license, summary, website, citation, authors, acknowledgements, globalParameters loop skipping `jipipe:`/`jipipe-ui:` keys), with `authorsToArray` moved in as a private static helper of `JIPipeAutomationAPI`. `GetProjectInfo.execute` replaces the block with `data.set("metadata", JIPipeAutomationAPI.projectMetadata(project))` - no other change to its response.
- `JIPipeAutomationAPI.describeProject(AutomationContext ctx, String detail)` -> `JsonNode`: null project -> `{"error": "No project loaded"}`; builds `name`/`path` (the `GetProjectInfo` resolution: metadata name or file name or "Untitled"), `metadata` via the new static, `compartments` (per compartment: id, name, nodeCount, cachedNodes, totalNodes, status from the same helpers Task 2 uses, `fileWriterCount` = count of the compartment's project-graph nodes matching the file-writer filter), `wiring` (the same edge list as the map), `annotations` (`queryAnnotations(ctx, null, null)`'s array re-used), `fileWritingNodes` (project-graph-wide, each `{id: alias-or-name, name, typeId, compartmentId, outputSlots: [names of qualifying slots]}`); with `detail` equal to `"full"`, each compartments entry additionally carries `nodes: [{id, name, typeId}]` (the `get_segment_detail` shape).
- File-writer filter (one private static helper): an output slot qualifies when `slot.getInfo().isStoreToDisk()` is true AND `(slot instanceof JIPipeOutputDataSlot o) && !o.isSkipExport()`. A node is a file writer when it has at least one qualifying output slot. (Ruling: input slots never qualify - `isStoreToDisk` is documented "Only valid for output slots".)

- [ ] **Step 4: Implement the `DescribeProject` operation** in `PipelineMapOperations` (after `GetSegmentDetail`): `getId()` "describe_project"; schema with `detail` (`{"type": "string", "enum": ["summary", "full"], "default": "summary", "description": "summary: sections with counts. full: additionally the node list per compartment"}`), `additionalProperties: false`; Query, read-only; `execute` validates `detail` (anything but `summary`/`full`/absent -> `error("Unknown detail: <v> (supported: summary, full)")`) and delegates to `JIPipeAutomationAPI.describeProject(ctx, detail)`. Description (pin verbatim): "Describe the whole project in one call: metadata, compartments with status and cache coverage, the compartment-to-compartment wiring, annotations, and the nodes whose output slots are exported to disk (slot store-to-disk enabled and not skipped; a run can still exclude nodes, e.g. cache updates). detail=full adds the node list per compartment. For per-node parameter detail use query_node, for the text report use get_project_report, for run errors use get_errors". Register in `AutomationPlugin` directly after `get_segment_detail`.

- [ ] **Step 5: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=PipelineMapOperationsTest,SegmentStatusLogicTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperationsTest.java
git commit -m "feat(#1383): add the describe_project composite"
```

### Task 4: Full-suite verification

- [ ] **Step 1: Run the whole jipipe-core suite** - `mvn -q clean test -pl jipipe-core` (clean per the standing branch-switch ruling; wait it out).
Expected: 0 failures, 0 errors; skips only the known environment-dependent classes (SettingsFileWatcher 3, SettingsServiceComponentSync 1, PaletteStorageLifecycle 1, TaxonomyConversionReport 1 at last baseline 1527/0/0/6); zero failures in new or touched test classes.

- [ ] **Step 2: Record the numbers** in the SDD workspace ledger (`.superpowers/sdd/2026-11-20-wi1383-pipeline-map/progress.md`): suite counts, any deferred minors, and the (empty) by-eye list - D has none.

No commit (ledger path is git-ignored).
