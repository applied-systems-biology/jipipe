# Spec: #1383 sub-project D - pipeline map enrichment and describe_project

Date: 2026-11-20
Status: DRAFT for user review
Work item: #1390 (child of #1383, MCP enhancements). Depends on: A (!204), F (!205), E (!206), B (!207) merged; C (!208) open - file-surface disjoint from D except adjacent `AutomationPlugin` registration lines (if C merges mid-D, master is pulled into the branch). Branch base: post-B master `5bdc2b80c4`.
Source: user feedback items 5 and 11 of the #1383 charter. Design approved in chat (user, 2026-11-20).

## 1. Intent

An agent that documents or reasons about a whole pipeline needs one map that shows how compartments hand data to each other, whether each compartment has cached results or problems, and one composite call that assembles metadata, compartments, wiring, annotations, and the file-writing nodes - instead of the roughly ten calls the original workflow needed.

## 2. Confirmed decisions

1. **Design approved in chat** (user, 2026-11-20): D1 enriches `get_pipeline_map` (real edges from the compartment graph, real status, version bump); D2 adds the `describe_project(detail)` composite. Per-node status in `get_segment_detail`, run-history-based status, and `save-to` on the composite are OUT (section 9).
2. **Edges come from the compartment graph, not re-derivation**: `project.getCompartmentGraph()` (`JIPipeProject.java:910`, a `JIPipeGraph` of type ProjectCompartments) is the canonical inter-compartment wiring model - the same model the compartments overview canvas renders. Deriving links by comparing node compartments in the project graph would be a second, drift-prone source of truth.
3. **Status = validation + cache, the reproducible pair**: validation errors are graph-state (the same `project.reportValidity` call `get_errors` makes, run once per map call); cache coverage is `project.getCache().query(node, nodeUUID, silent)` (a cheap read-lock map lookup, `JIPipeLocalProjectMemoryCache.java:281-289`). Run-error records are transient progress-log state and stay out (they remain available per-run via `get_errors`/`get_job_status`).
4. **The enum stays; the semantics are documented and the counts are machine-readable**: `SegmentStatus` keeps OK/WARNING/ERROR/UNKNOWN. ERROR = validation errors in the compartment; OK = fully cached, no validation issues; WARNING = partially cached (or only validation warnings with some cache - see D1 mapping, the precise table is in section 4); UNKNOWN = nothing cached and no issues. Each compartment segment additionally carries `cachedNodes`/`totalNodes` so no consumer has to guess the enum mapping. Adding enum values would change the wire shape for no benefit; the counts are the contract.

## 3. Verified ground (master 5bdc2b80c4)

- `JIPipeAutomationAPI.getPipelineMap(ctx)` (`JIPipeAutomationAPI.java:656-676`): root segment (project name) with one child segment per compartment (id = compartment UUID string, `compartmentId` set, `nodeIds` = alias-or-name, `summary` = "N nodes"). Never touches `PipelineMap.edges`; status stays the `SegmentNode` field default `UNKNOWN`.
- `PipelineMap` (`api/automation/pipeline_map/PipelineMap.java`): fields `version` (long, always 0 today), `root`, `edges` (list of `SegmentEdge`, initialized empty). `SegmentEdge` (`SegmentEdge.java`): final `source`, `target`, `label`, `@JsonInclude(NON_NULL)`. `SegmentNode`: id, label, summary, status (default UNKNOWN), compartmentId, nodeIds, children - all `@JsonInclude(NON_NULL)`.
- **Compartment graph**: vertices are the compartments as `JIPipeProjectCompartment` nodes (`api/compartments/algorithms/JIPipeProjectCompartment.java:48`, a `JIPipeGraphNode`): exactly one input slot `"Input"` (`:61`, type `JIPipeCompartmentOutputData`, optional), plus one output slot per compartment output (mirrored from the `JIPipeProjectCompartmentOutput` nodes by `JIPipeProject.updateCompartmentOutputs`, `JIPipeProject.java:660-700`). Edges connect a source compartment's output slot to a target compartment's `Input` slot (the connect pattern `JIPipeProject.java:639` uses). Compartment identity on the vertex: `JIPipeProjectCompartment.getProjectCompartmentUUID()`.
- **Cache**: `JIPipeLocalProjectMemoryCache.query(node, nodeUUID, progressInfo)` returns `Map<String, JIPipeDataTable>` of stored outputs, non-empty = the node has cached results; read-lock map lookup, safe headless. `project.getCache()` (`JIPipeProject.java:576`).
- **Validation**: `project.reportValidity(new UnspecifiedValidationReportContext(), JIPipeValidationReportSettings.DEFAULT, report, new JIPipeProgressInfo())` - the exact call in `DiagnosticsOperations.GetErrors` (headless-safe there, same here). Report entries carry the node/compartment context `get_errors` already filters by (`addValidationEntry` resolves the compartment of an entry's node).
- **File-writing nodes**: the run write-step condition (`api/run/JIPipeGraphRunConfiguration` consumer at `JIPipeGraphRun.java:1361`): data of an output slot is written to disk when `configuration.isStoreToDisk()` AND the node is not in `disableStoreToDiskNodes` AND `outputSlot.getInfo().isStoreToDisk()` AND `!outputSlot.isSkipExport()`. The persistent, project-derived part is the last two conditions (`JIPipeDataSlotInfo.isStoreToDisk()` `:282`; `JIPipeOutputDataSlot.isSkipExport()` `:52`, a persisted per-slot state); `disableStoreToDiskNodes` is computed per run from the run scope (cache updates disable everything outside the targets), so it is project-independent and out of scope. `isStoreToDisk()`/`isSkipExport()` are read on the PROJECT graph slots (no run needed).
- **Composite ingredients**: `get_project_info` (`InfoOperations.GetProjectInfo`, response: name, path, compartments [{id, name, nodeCount}], compartmentCount, nodeCount, metadata {description, license, summary, website, citation, authors, acknowledgements, globalParameters}); `query_annotations` via `JIPipeAutomationAPI.queryAnnotations` (`:347`, returns `{annotations: [{id, uuid, typeId, kind, compartmentId, title, content?, x, y, width?, height?}]}`); `get_segment_detail`'s per-node shape (id, name, typeId).
- `GetPipelineMap` classification today: Query family behavior via `JIPipeAutomationAPI` delegation, no `requiresEdt` override (default false) - D keeps that (all reads are headless-safe).

## 4. D1: `get_pipeline_map` enrichment

`JIPipeAutomationAPI.getPipelineMap` (the single builder; `PipelineMapOperations.GetPipelineMap` keeps delegating to it) gains:

- **Edges**: iterate `project.getCompartmentGraph().edgeSet()`; for each edge resolve source slot (`getEdgeSource`) and target slot (`getEdgeTarget`), source and target node (`slot.getNode()`), and map them to compartment UUIDs via `getProjectCompartmentUUID()`. For each edge add one `SegmentEdge(source = source compartment UUID string, target = target compartment UUID string, label = source output slot name)`. The receiving side is always the single `Input` slot, so the label (the hand-off name) is the informative part. Multiple output slots produce multiple edges between the same pair, as in reality. Edge UUIDs and UI shapes are not exposed.
- **Status and counts** (computed in one pass over the compartments, after one `reportValidity` call for the whole project):
  - `errorCount` per compartment = validation report entries of severity ERROR whose node (or compartment context) belongs to the compartment (the same compartment resolution `get_errors.addValidationEntry` uses).
  - `warningCount` per compartment = the same for severity WARNING.
  - `cachedNodes` = count of the compartment's nodes with a non-empty `cache.query(...)`; `totalNodes` = the compartment's node count.
  - Mapping: `errorCount > 0` -> ERROR; else `cachedNodes == totalNodes && totalNodes > 0 && warningCount == 0` -> OK; else `cachedNodes > 0 || warningCount > 0` -> WARNING; else UNKNOWN. An empty compartment (0 nodes) is UNKNOWN. The mapping is stated verbatim in the operation description.
  - New `SegmentNode` fields: `cachedNodes`, `totalNodes` (Integer, `@JsonInclude(NON_NULL)`; set on compartment segments, null on the root). Status set via `setStatus`.
- **Version**: `PipelineMap` constructed with version 1 (the shape changed: edges + status are now real).
- The compartment loop keeps the existing nodeIds/summary behavior; `summary` becomes "N nodes, M cached" (machine-readable counts live in the new fields, the summary stays human-readable).
- Classification unchanged (Query/read-only semantics, no EDT requirement).

## 5. D2: `describe_project` composite

New operation `DescribeProject` in `PipelineMapOperations` (the pipeline-overview family), id `describe_project`, registered in `AutomationPlugin` beside the map ops. Query, read-only, no EDT requirement.

- Params: `detail` optional enum `summary|full`, default `summary`; `additionalProperties: false`.
- Response (single JSON object; sections assembled from SHARED statics in `JIPipeAutomationAPI` - `describeProject` builds on the same compartment-graph walk, status computation, and file-writer filter as `getPipelineMap`, so the two views cannot drift):
  - `name`, `path` (same resolution as `get_project_info`).
  - `metadata`: the `get_project_info` metadata block (description, license, summary, website, citation, authors, acknowledgements, globalParameters) - extracted into a shared static so it is not re-typed.
  - `compartments`: array of `{id, name, nodeCount, cachedNodes, totalNodes, status, fileWriterCount}`.
  - `wiring`: array of `{source, target, label}` - exactly D1's edges.
  - `annotations`: the `queryAnnotations` array (all kinds, no compartment filter).
  - `fileWritingNodes`: array of `{id (alias), name, typeId, compartmentId, outputSlots: [names]}` for every project-graph node with at least one output slot where `getInfo().isStoreToDisk()` is true and `isSkipExport()` is false. Documented in the description: this is the project-derived part of the run write-step; a run can additionally exclude nodes (cache-update runs limit writing to the run targets).
  - With `detail: full`, each `compartments` entry additionally carries `nodes: [{id, name, typeId}]` (the `get_segment_detail` shape).
- Description names the decomposition honestly: for per-node detail beyond `full`, deep-dive with `get_segment_detail`, `query_node`, `get_project_report`; for run errors use `get_errors`/`get_job_status`.

## 6. Permissions and broker

Both operations: Query category, read-only (Allow by category default), project-bound (no scope override - both need a project). No broker changes; C's machinery picks them up automatically.

## 7. Testing policy (house rules: headless-safe, no Swing interaction tests)

All of D is pure project-graph reading - fully headless-testable (unlike B/C, no by-eye items):

- **Edges**: a two-compartment project connected the way JIPipeProject itself connects (compartment-graph `connect` of the source compartment's output slot to the target's `Input` slot, `JIPipeProject.java:639` pattern; the test may need `updateCompartmentOutputs` to mirror the output slots first - the implementer verifies the exact setup order against `JIPipeProjectCompartment`'s slot sync): edges listed with the right source/target/label; an unconnected project yields `edges: []`.
- **Status mapping**: a pure static helper `SegmentStatusLogic` (or an equivalent named home in the automation API) mapping (errorCount, warningCount, cachedNodes, totalNodes) -> SegmentStatus gets table-driven tests for all branches including the empty-compartment case; the integration test stores cache via `project.getCache().store(node, nodeUUID, table, outputName, silent)` and asserts the counts and mapped status on the built map.
- **File-writer filter**: a pure filter helper test (slot store-to-disk true/false x skipExport true/false) + an integration assertion that the composite's `fileWritingNodes` matches the per-compartment `fileWriterCount` sums.
- **`describe_project`**: summary response has all sections with counts consistent with the individual ops (compartment counts vs `get_project_info`, annotations vs `queryAnnotations`, wiring vs the map's edges); `full` adds the node lists.
- Registry/schema validation tests pick up the new operation automatically.

## 8. Out of scope (ledgered)

- Per-node status in `get_segment_detail` (feedback item 5 asks at map level; node-level drill-down stays alias-based).
- Run-history/run-error status (transient progress-log state; not project state - `get_errors`/`get_job_status` remain the run-truth sources).
- `save-to` on `describe_project` (inline JSON only in v1; the per-piece ops cover huge projects).
- Edge UI data (uuids, shapes, control points) and any write path for the wiring (read-only map).
