# #1383 Sub-project A - Query Correctness + Shaping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the MCP query layer correct and cheap to consume: `query_graph` honors the compartment filter (and fails loud instead of silently returning everything), responses can be shaped, `query_annotations` and `find_nodes` exist, and `query_node` parameter values are normalized.

**Architecture:** All work happens in the existing operation layer: `JIPipeAutomationAPI` gains one shared compartment finder plus new pure JSON methods; `QueryOperations` gains schema-driven operations that parse params and delegate; `AutomationPlugin` registers the two new tools. No broker, permission, or UI changes - every touched tool stays read-only.

**Tech Stack:** Java 21, jackson databind (ObjectNode/ArrayNode), JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-19-wi1383-query-shaping-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- Stable string-literal ids only; kebab-case operation param names; `additionalProperties: false` on every schema.
- Read-only Query operations: `getCategory() = Query`, `isReadOnly() = true`. No `requiresEdt()` override needed (all reads are data-side; the existing query ops set none).
- Test command: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,AutomationRegistryValidationTest` (and `-Dtest=InstrumentationAPITest` where Task 1 changes the legacy signature). Builds are sequential - one slot.
- No Swing interaction tests. Programmatic projects in tests; `assumeTrue(hasNodeType(...))` guard where a node type registration is required, mirroring `InstrumentationAPITest`.
- House test policy: no tests that restate the implementation; the tests below pin the spec'd contracts and the Review Focus edge cases only.

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **Two compartments share a display name.** Resolution must be deterministic (same compartment every call, first match in iteration order) - no uniqueness claim anywhere. Pinned in Task 1.
2. **`compartment-id` is a well-formed UUID that matches no compartment.** Must be an error, NOT a fallback to name matching. Pinned in Task 1.
3. **A connection endpoint whose alias is null.** The shaping path must not change the existing JSON-null shape. Pinned in Task 2.
4. **A text-box note with empty title and content.** `title`/`content` present as `""` (omission applies to comment nodes only). Pinned in Task 3.
5. **A parameter value whose `toString`/`getTextDescription` throws.** `query_node` must still succeed; the entry carries a rendered error string, no `raw`. Pinned in Task 5.

---

### Task 1: Shared compartment resolution + fail-loud `query_graph` + per-node `compartmentId`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryGraph` :59-112; new public `findCompartment`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/AutomationOperationUtils.java` (`findCompartment` - becomes a delegate)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryGraph.execute` - catch and format the error)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java` (new file)

**Interfaces:**
- Consumes: existing `JIPipeProject.findCompartment` (UUID/alias), `JIPipeProjectCompartment.getProjectCompartmentUUID()`, `JIPipeGraph.getCompartmentUUIDOf`, `JIPipeGraphNode.getNodeUILocationWithin(String compartmentUuidStr)`.
- Produces:
  - `public static JIPipeProjectCompartment findCompartment(JIPipeProject project, String ref)` on `JIPipeAutomationAPI` - resolution order: well-formed UUID match, then alias (via `project.findCompartment`), then case-sensitive display-name first match. Returns null when unresolvable. A well-formed UUID that matches nothing returns null (no name fallback).
  - `JIPipeAutomationAPI.queryGraph(ctx, compartmentIdStr)` throws `IllegalArgumentException("Compartment not found: '<ref>'. Known compartments: <comma list>")` for a non-blank unresolvable filter (behavior change, was silent all-nodes).
  - Every node object in the `queryGraph` response gains `compartmentId` (string, null when the node has none).
  - The `QueryGraph` operation returns `{"error": "<api message> (see query_compartments for ids)"}` for that case (the operation appends the parenthetical; the API message does not).

- [ ] **Step 1: Create the test file with the house helper, failing tests first**

New file `QueryOperationsTest.java` in `org.hkijena.jipipe.api.automation.operations`, with the same `createContext(JIPipeProject)` helper shape as `InstrumentationAPITest` (`JIPipe.ensureInstance()` in `@BeforeAll`; context id `"test"`; `new JIPipeProgressInfo()`) plus its own `hasNodeType` guard. Test node type: `jipipe:comment` (registered by the in-repo UtilitiesPlugin, available headless; insert with `project.getGraph().insertNode(info.newInstance(), compartmentUuid)` after `assumeTrue(hasNodeType("jipipe:comment"))` - comment nodes are plain graph nodes, so they insert into any compartment).

- `queryGraph_filterByNameReturnsOnlyThatCompartment`: two compartments with distinct nodes; call the **operation** `new QueryOperations.QueryGraph().execute(ctx, {"compartment-id": "<name of comp 1>"})`; assert exactly comp-1's nodes come back.
- `queryGraph_unknownFilterErrorsWithKnownCompartments`: `{"compartment-id": "Nope"}`; assert result `error` text contains `Compartment not found: 'Nope'`, contains both compartment names, and ends with `(see query_compartments for ids)`.
- `queryGraph_unknownUuidFilterDoesNotFallBackToName`: insert a real compartment named `Preprocessing`; call with a well-formed random UUID that matches no compartment; assert the `error` envelope (not all nodes).
- `queryGraph_duplicateNamesResolveStably`: two compartments both named `Dup`; call with `"Dup"`; repeat 3 times; assert the returned `compartmentId` field (see next test) is identical across calls and equals one of the two UUIDs.
- `queryGraph_nodesCarryCompartmentId`: assert every node object has `compartmentId` equal to its compartment's UUID.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest`
Expected: compile or assertion failures (name filter not honored - all nodes returned; no `compartmentId` field; error case returns nodes).

- [ ] **Step 3: Implement in `JIPipeAutomationAPI`**

- Add `public static JIPipeProjectCompartment findCompartment(JIPipeProject project, String ref)` (ref may be null/blank: return null). Order: (a) `UUID.fromString` success AND `project.getCompartments().containsKey(uuid)` -> that compartment; (b) `project.findCompartment(ref)` (alias); (c) case-sensitive name first match over `project.getCompartments().entrySet()`.
- Rework `queryGraph`: when the filter is non-blank, resolve via `findCompartment`; if null, throw `IllegalArgumentException` with the message `Compartment not found: '<ref>'. Known compartments: <names comma-joined>` (use display names; they are what the agent typed). Keep the UUID echo `data.put("compartmentId", ...)` and add `compartmentId` to each node object (`graph.getCompartmentUUIDOf(node)`).
- `AutomationOperationUtils.findCompartment` delegates to the API method (same signature, same EDT-side callers; keep its Javadoc note that it runs on the EDT where the window lookup does).
- `QueryGraph.execute`: wrap the API call in `try/catch (IllegalArgumentException e)` -> return `mapper.createObjectNode().put("error", e.getMessage() + " (see query_compartments for ids)")`.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,InstrumentationAPITest`
Expected: PASS (the legacy `queryGraph(ctx, null)` callers are unaffected; the legacy all-nodes-on-bad-filter behavior is intentionally gone).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/AutomationOperationUtils.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): honor query_graph compartment filter by name and fail loud"
```

### Task 2: `query_graph` response shaping

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryGraph` gains the overload)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryGraph` schema + param parsing)

**Interfaces:**
- Consumes: Task 1's finder and error behavior.
- Produces: `public static JsonNode queryGraph(AutomationContext ctx, String compartmentIdStr, List<String> fields, boolean omitPositions, boolean connectionsOnly)` - the full shaping implementation; the existing 2-arg `queryGraph(ctx, compartmentIdStr)` delegates with `(null, false, false)`. Node fields whitelist set (exact order of the enum): `id, name, typeId, compartmentId, uuid, x, y`. `fields` non-null -> only listed fields emitted (in the enum order, not the request order). `omitPositions` true -> no `x`/`y` even if `fields` lists them. `connectionsOnly` true -> `"nodes"` is an empty array, connections unchanged. Connection objects are otherwise byte-identical to today (including JSON-null aliases).

- [ ] **Step 1: Failing tests (add to `QueryOperationsTest`)**

- `queryGraph_fieldsWhitelist`: request `fields: ["name", "typeId"]`; assert node objects contain exactly `name` and `typeId`.
- `queryGraph_omitPositions`: assert no `x`/`y` keys; default call still has them.
- `queryGraph_connectionsOnly`: `nodes` array size 0, connections intact.
- `queryGraph_omitPositionsBeatsFields`: `fields: ["id", "x"]` + `omit-positions: true` -> `id` present, no `x`.
- `queryGraph_unknownFieldErrors`: `fields: ["id", "bogus"]` -> operation returns `error` naming the valid set.
- `queryGraph_nullAliasConnectionUnchanged`: build a node without an alias in a second compartment connected across; assert the connection's `target` is JSON null exactly as before shaping (pin Review Focus 3).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest`
Expected: FAIL (schema rejects the new params - `additionalProperties: false` - and the API overload does not exist).

- [ ] **Step 3: Implement**

- `QueryOperations.QueryGraph`: extend the schema with `fields` (array of string, `enum` items from the whitelist), `omit-positions` (boolean), `connections-only` (boolean). Parse: validate each `fields` entry against the whitelist (unknown entry -> `error` node listing the valid fields, the same refusal style as template-id refusals); call the new API overload.
- `JIPipeAutomationAPI`: move the node-emission loop into the overload; emit fields in enum order per the rules above. The 2-arg method delegates.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,InstrumentationAPITest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): add response shaping to query_graph"
```

### Task 3: `query_annotations`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `queryAnnotations`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (new `QueryAnnotations` operation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register `query_annotations` next to the other query ops, :69-72)
- Test: `QueryOperationsTest.java`

**Interfaces:**
- Consumes: Task 1's `JIPipeAutomationAPI.findCompartment`; `JIPipeCommentNode` (plugins/core), `JIPipeGraphCanvasNote` (api/nodes/annotation) with `getGridWidth()`/`getGridHeight()`; `DefaultTextBoxGraphCanvasNote` params `text-title`/`text-content` read via `JIPipeParameterTree.getParameters(node)`; `JIPipeDesktopGraphCanvasGrid.GRID_WIDTH`/`GRID_HEIGHT` (25).
- Produces: `public static JsonNode queryAnnotations(AutomationContext ctx, String compartmentRef, String kind)` on the API. Response `{"annotations": [...]}`; entry shape:
  - `id` = alias id (null -> JSON null, like query_graph), `uuid`, `typeId`, `kind` = `"comment"` | `"text-box"` (decided by `instanceof`, never typeId), `compartmentId`, `title`, `x`, `y` (same source as query_graph: `getNodeUILocationWithin`, 0 when null).
  - Comment: `title` = node name; no `content`, no `width`/`height`.
  - Text-box: `title` = `text-title` param (default `""`), `content` = `text-content` param (default `""`), `width` = `gridWidth * GRID_WIDTH`, `height` = `gridHeight * GRID_HEIGHT`.
  - `kind` filter: `"comment"`/`"text-box"`/null. `compartment` filter: same fail-loud error as query_graph (operation appends the parenthetical).
  - Node types not matching either family are skipped (defensive; the project graph today contains only these annotation families).

- [ ] **Step 1: Failing tests (add to `QueryOperationsTest`)**

Build a project (guard `assumeTrue` on `jipipe:comment` and `graph-annotation-text-box`): one compartment, a `JIPipeCommentNode` named "My note" inserted, and a `JIPipeGraphCanvasNote` created from `JIPipe.getInstance().getNodes().getNodeInfo("graph-annotation-text-box").newInstance()` with `text-title`/`text-content` set through the parameter tree and grid size 3x2 inserted.

- `queryAnnotations_returnsBothFamiliesWithFields`: 2 entries; the comment entry has `kind=comment`, `title="My note"`, no `content`/`width`/`height` keys; the text-box entry has `kind=text-box`, the set title/content, `width=75`, `height=50`.
- `queryAnnotations_kindFilter`: `kind=comment` -> 1 entry; `kind=text-box` -> 1 entry.
- `queryAnnotations_compartmentFilterByName`: second empty compartment; filter by its name -> 0 entries; unknown name -> `error` envelope with the known-compartments list.
- `queryAnnotations_emptyTextFieldsStayPresent` (pin Review Focus 4): text-box with blank title/content -> `title`/`content` keys present as `""`.
- `queryAnnotations_positionsMatchQueryGraph`: the text-box entry's `x`/`y` equal the same node's `x`/`y` in a `queryGraph` response.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest`
Expected: FAIL (no `query_annotations` operation/API).

- [ ] **Step 3: Implement**

- `JIPipeAutomationAPI.queryAnnotations` as specified (single pass over `project.getGraph().getGraphNodes()`, family check by `instanceof`, compartment filter via `findCompartment` with the same `IllegalArgumentException` contract as Task 1).
- `QueryOperations.QueryAnnotations`: schema `{"compartment": string, "kind": enum [comment, text-box]}`, `additionalProperties: false`, neither required; description: "List all comments and text boxes of the project with title, content, compartment, and bbox in one call (id, uuid, typeId, kind, compartmentId, title, content for text boxes, x, y, and width, height for text boxes)". Catch the finder's `IllegalArgumentException` like `QueryGraph`.
- Register in `AutomationPlugin` as `registerAutomationOperation("query_annotations", new QueryOperations.QueryAnnotations());` in the query block.

- [ ] **Step 4: Run tests green (including the registry gate)**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,AutomationRegistryValidationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): add query_annotations for comments and text boxes"
```

### Task 4: `find_nodes`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `findNodes`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (new `FindNodes` operation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java` (`SearchNodes` description cross-reference only)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register `find_nodes`)
- Test: `QueryOperationsTest.java`

**Interfaces:**
- Consumes: Task 1's finder (the `compartment` filter uses the same fail-loud contract); the query_graph node shape (same fields, including `compartmentId`).
- Produces: `public static JsonNode findNodes(AutomationContext ctx, String query, String typeId, String compartmentRef, int limit)` on the API. Response `{"matches": [node objects], "totalCount": N}`. Matching: case-insensitive substring on node name (`query`), case-insensitive substring on registered node type id (`type-id`), compartment restriction; all optional, all must match (AND). `limit` default 50, `totalCount` counts matches before limiting.

- [ ] **Step 1: Failing tests (add to `QueryOperationsTest`)**

- `find_nodes_nameSubstringCaseInsensitive`: nodes named "Input CSV", "Filter"; `query: "csv"` -> 1 match; also `query: "filter"` -> 1.
- `find_nodes_typeIdFilter`: `type-id: "comment"` matches comment nodes, not others.
- `find_nodes_compartmentFilter`: two compartments, one matching node each; filter -> 1.
- `find_nodes_limitAndTotalCount`: 3 matching nodes, `limit: 2` -> `matches` size 2, `totalCount` 3.
- `find_nodes_noFiltersReturnsEverythingLimited`: no params -> `totalCount` = all nodes, `matches` <= 50.
- `find_nodes_unknownCompartmentErrors`: `compartment: "Nope"` -> the operation returns the same error envelope as `QueryGraph` (`Compartment not found: 'Nope'. Known compartments: ... (see query_compartments for ids)`), not an empty match list.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest`
Expected: FAIL (no `find_nodes`).

- [ ] **Step 3: Implement**

- `JIPipeAutomationAPI.findNodes` as specified (reuse the same node-object emission as `queryGraph` - extract the node-object builder if it is the cleanest path; do not duplicate the field logic). Unresolvable `compartmentRef`: throw the `IllegalArgumentException` with the Task 1 message format; the operation catches it and appends the parenthetical, exactly like `QueryGraph`.
- `QueryOperations.FindNodes`: schema with optional `query`, `type-id`, `compartment`, `limit` (integer), `additionalProperties: false`. Description: "Search nodes inside the open project by name, node type id, and compartment (find_nodes searches the project; search_nodes searches the algorithm catalog)".
- `PipelineMapOperations.SearchNodes` description gains the mirror sentence: "(searches the algorithm catalog; use find_nodes to search inside the open project)".
- Register in `AutomationPlugin` query block.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,AutomationRegistryValidationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): add find_nodes for in-project node search"
```

### Task 5: Normalized parameter values in `query_node`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryNode` params block :127-133)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryNode` description)
- Test: `QueryOperationsTest.java`

**Interfaces:**
- Consumes: `JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(Object)` (api/parameters); `JIPipeParameterAccess.get(Object.class)` / `getFieldClass()`.
- Produces: `queryNode`'s `parameters` map becomes `key -> {"value": string, "type": string, "raw": string?}`. `value`: the interface's text description when the value implements `JIPipeCustomTextDescriptionParameter`, else `toString()`, else `"null"` for a null value. `type`: field class simple name, `"?"` when absent (the `get_node_docs` convention). `raw`: the plain `toString()` (or `"null"`), present only when it differs from `value`. A `RuntimeException` from either rendering path yields `value = "error rendering value: <ExceptionSimpleName>"` and no `raw` (pin Review Focus 5). The `jipipe:`/`jipipe-ui:` key exclusion is unchanged.

- [ ] **Step 1: Failing tests (add to `QueryOperationsTest`)**

The renderer is the extraction seam (Step 3 extracts `renderParameterValue(JIPipeParameterAccess)`), so the value-form tests call it directly with a test-local `JIPipeParameterAccess` implementation (small: `getKey()`/`get(Object.class)`/`getFieldClass()` return the canned values, everything else dummy) - deterministic, no registered node type required. A node-level test confirms the wiring:

- `renderParameterValue_stringHasNoRaw`: canned `String` value -> `{"value": "<s>", "type": "String"}`, no `raw` key.
- `renderParameterValue_expressionIsBare`: canned value `new JIPipeExpressionParameter("foo(bar)")` (public constructor, `JIPipeExpressionParameter.java:43`) -> `value` = `foo(bar)`, no `Expression: ` prefix, `raw` present (its `toString()` differs).
- `renderParameterValue_dataExportWrapperIsBare`: `new DataExportExpressionParameter("PATH_COMBINE(data_dir, \"x\")")` (`DataExportExpressionParameter.java:68`) -> same contract.
- `renderParameterValue_rawOnlyWhenDifferent`: the String case proves absence; the expression cases prove presence with `raw` equal to the `toString()` form.
- `renderParameterValue_throwingRendererDoesNotFail` (pin Review Focus 5): canned value a test-local class implementing `JIPipeCustomTextDescriptionParameter` whose `getTextDescription()` throws -> `value` starts with `error rendering value:`, no `raw`; a second entry (plain string) still renders normally.
- `renderParameterValue_nullValue`: null value -> `value` = `"null"`, no `raw`.
- `queryNode_parametersUseTheRenderer`: one inserted node type with a real parameter (use the `jipipe:comment` node: `background-color` -> `Color`), assert the `query_node` response entry is an object with `value`/`type` and no `raw` for a value whose both renderings agree, and that the `jipipe:`-prefixed keys are still excluded.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest`
Expected: FAIL (entries are flat strings today).

- [ ] **Step 3: Implement**

- Rework the `queryNode` params loop to emit the object form; extract `renderParameterValue(JIPipeParameterAccess)` as specified.
- `QueryOperations.QueryNode` description: append "Parameter values are normalized objects {value, type, raw?}: value is the effective readable string (expressions without the 'Expression: ' prefix), raw is the plain toString when it differs".

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,InstrumentationAPITest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): normalize query_node parameter values"
```
