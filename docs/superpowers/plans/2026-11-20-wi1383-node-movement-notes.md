# #1383 Sub-project G - Node Movement and Canvas Notes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give agents `move_node`, one-call `create_note`, note-terminology `query_notes` (with grid-unit sizes), data-annotation columns in `query_data`, and make `arrange_nodes` preview-by-default and note-safe.

**Architecture:** Two new Modify operations in `ModificationOperations` delegating to new statics in `JIPipeAutomationAPI` (the `addNode`/`arrange_nodes` pattern: headless stored-location writes, one `NodeAddedEvent` where a node is created); the `query_annotations` -> `query_notes` rename in `QueryOperations` + `JIPipeAutomationAPI.queryAnnotations`; `queryData` gains data-annotation columns; `ArrangeOperations.ArrangeNodes` gains `apply` (default false) and filters notes out of the layout.

**Tech Stack:** Java 21, Jackson databind, JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-20-wi1383-node-movement-notes-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- Branch `1383-node-movement-notes` (from post-C master `1a19f3f2d4`; spec commits `64f1d5b61d`, `13267a6938`).
- Registered ids are stable string literals: new `"move_node"`, `"create_note"`, `"query_notes"`; the id `"query_annotations"` is REMOVED (clean break, no alias).
- **Grid units are the wire unit** for every position/size in this sub-project (1 cell = 25 px). Never convert to pixels except the existing display `width`/`height` fields of `query_notes`.
- Classification: `move_node`, `create_note` = Modify, read-only false, `requiresEdt() = true` (the `add_node`/`arrange_nodes` convention). `query_notes`, `query_data`, `arrange_nodes` keep Query/Modify + read-only as they are.
- No overlap enforcement in `move_node`; no new event types; no anchoring of notes.
- House test policy: headless only, no Swing interaction tests, no tests that restate the implementation. G has NO by-eye items.
- Test command per task: `mvn -q test -pl jipipe-core -Dtest=<TaskTests>,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot; wait out concurrent mvn, never kill. Full-suite verification uses `mvn -q clean test -pl jipipe-core` (standing branch-switch ruling).

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **`move_node` on a node that never had a stored location** must write cleanly and report `previous: null` (a null dereference here would break every freshly added node). Pinned in Task 1 (`moveNodeWithoutStoredLocationReportsNullPrevious`).
2. **`create_note` parameters must land before the single `NodeAddedEvent`** - an agent reading the event's node name would otherwise see the default title. Pinned in Task 2 (`createNotePublishesOneEventWithFinalTitle`).
3. **A comment note must not accept `content`/`width`/`height`** (silent acceptance would produce a note whose size is ignored). Pinned in Task 2 (`createNoteContentOnCommentFails`).
4. **`query_notes` must report grid units, not pixels** - the exact bug the user flagged. Pinned in Task 3 (`queryNotesReportsGridUnits`).
5. **`arrange_nodes` preview must not write a single stored location, and must not move notes** - the user's "agent breaks my workflow" scenario. Pinned in Task 5 (`arrangePreviewWritesNothing`, `arrangeExcludesNotes`).
6. **`query_data` on a slot WITH data annotations must not lose the existing text-annotation/Name contract**, and a slot with none must be unchanged. Pinned in Task 4 (`queryDataWithoutDataAnnotationsIsUnchanged`, `queryDataWithDataAnnotations`).

---

### Task 1: `move_node`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `moveNode` static after `removeNode`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` (new `MoveNode` class after `RemoveNode`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register after `add_connection`/in the Modification block)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ModificationOperationsTest.java` (create)

**Interfaces:**
- Consumes: `graph.findNode(idOrAlias)` -> `JIPipeGraphNode`; `graph.getUUIDOf(node)`, `graph.getAliasIdOf(node)`, `graph.getCompartmentUUIDOf(node)`; `node.getNodeUILocationWithin(String)` / `node.setNodeUILocationWithin(String, Point)`; the `addNode` static (test setup).
- Produces (later tasks: nothing; tests reuse): `JIPipeAutomationAPI.moveNode(AutomationContext ctx, String nodeId, int x, int y)` -> `ObjectNode` with keys `node`, `uuid`, `compartment`, `x`, `y`, `previous` (object with `x`/`y`, or JSON null).

- [ ] **Step 1: Write the failing tests** (`ModificationOperationsTest`; house helpers `ensureJIPipe`, `createProject`, `createContext` as in the sibling operation tests; node creation via `JIPipeAutomationAPI.addNode(ctx, compartment.getName(), "jipipe:comment", 0, 0)` or a simple registered algorithm - pick a node type guaranteed registered, e.g. `"jipipe:comment"`, and note the choice in the report)

```java
@Test void moveNodeWritesGridLocation()      // addNode at (0,0) -> moveNode(node, 5, 7) -> stored location (5,7);
                                             // response node == alias, uuid non-null, compartment == the compartment UUID string,
                                             // x == 5, y == 7, previous.x == 0, previous.y == 0
@Test void moveNodeWithoutStoredLocationReportsNullPrevious() // insert a node directly via graph.insertNode (no location),
                                             // moveNode(..., 3, 4) -> previous is JSON null, stored location (3,4)
@Test void moveNodeUnknownNodeReturnsError()  // "missing" -> {"error": "Node not found: missing"}
@Test void moveNodeMovesCanvasNotes()         // a "graph-annotation-text-box" created via addNode can be moved like any node
```

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=ModificationOperationsTest` -> COMPILATION ERROR (`moveNode`/`MoveNode` missing).

- [ ] **Step 3: Implement**

- `JIPipeAutomationAPI.moveNode`: `graph == null` -> `IllegalStateException("No graph available")` (the `addNode` convention); `findNode` null -> `mapper.createObjectNode().put("error", "Node not found: " + nodeId)`; read `previous` via `getNodeUILocationWithin(StringUtils.nullToEmpty(compartmentUuid))` BEFORE writing; write `setNodeUILocationWithin(compartmentUuid, new Point(x, y))`; build the response (`compartment` as the UUID string or JSON null when the node has no compartment).
- `ModificationOperations.MoveNode`: id `"move_node"`, `requiresEdt()` true, Modify, read-only false, schema `{"type":"object","required":["node","x","y"],"properties":{"node":{...alias or UUID...},"x":{"type":"integer","description":"Canvas column in grid cells (1 cell = 25 px)"},"y":{...same...}},"additionalProperties":false}`. Description (pin verbatim): "Move a node to a grid position (1 cell = 25 px). Works on any node, including canvas notes. No overlap checks are applied: notes may be placed on top of nodes. Use arrange_nodes for automatic topological layout. An open graph canvas shows the new position after its next refresh". Delegate to the static.
- Register `registerAutomationOperation("move_node", new ModificationOperations.MoveNode());` in the Modification block, directly after `remove_connection` (Task 2 then adds `create_note` directly after `move_node`).

- [ ] **Step 4: Run to verify pass** - same command as Step 2 plus the registry gates -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ModificationOperationsTest.java
git commit -m "feat(#1383): add the move_node operation"
```

### Task 2: `create_note`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `createNote` static after `moveNode`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` (new `CreateNote` class)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register after `move_node`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ModificationOperationsTest.java` (extend)

**Interfaces:**
- Consumes: `JIPipe.getNodes().getInfoById("graph-annotation-text-box")` / `("jipipe:comment")`; `info.newInstance()`; `TextBoxGraphCanvasNote` (`plugins/canvasnotes/nodes/`): `setTextTitle/setTextContent` (`DefaultTextBoxGraphCanvasNote`), `setGridWidth/setGridHeight` (on `JIPipeGraphCanvasNote`, inherited); `JIPipeGraphNode.setName(...)` for comments; `AutomationOperationUtils.findCompartment(project, ref)` (UUID/alias/NAME - the newer-ops resolver; `JIPipeAutomationAPI.resolveCompartmentUUID` is UUID/alias-only, discovered in Task 1, and is NOT used here); `NodeAddedEvent(projectId, compartmentId, alias, typeId, name)`.
- Produces: `JIPipeAutomationAPI.createNote(AutomationContext ctx, String compartmentRef, String kind, String title, String content, int x, int y, int width, int height)` -> `ObjectNode`.

- [ ] **Step 1: Write the failing tests** (extend `ModificationOperationsTest`)

```java
@Test void createNoteTextBoxStoresEverything()   // kind "text-box", title "T", content "C", x 2, y 3, width 6, height 5
    // -> node present in the compartment graph; text-title == "T", text-content == "C",
    //    grid-width == 6, grid-height == 5, stored location (2,3); response nodeId/uuid/kind/title/x/y/width/height exact
@Test void createNoteCommentUsesTitleAsName()    // kind "comment", title "Heads up"
    // -> node name == "Heads up"; response has NO width/height keys
@Test void createNoteDefaults()                  // compartment only -> kind text-box, title "Text box", content "",
                                                 //    x 0, y 0, width 4, height 3 (the canvas-note defaults)
@Test void createNoteContentOnCommentFails()     // kind comment + content -> error naming content and the restriction
@Test void createNoteWidthOnCommentFails()       // kind comment + width -> same kind of error
@Test void createNoteUnknownCompartmentReturnsError() // "Nope" -> error "Compartment not found: Nope"
@Test void createNotePublishesOneEventWithFinalTitle() // subscribe to the event bus before the call;
                                                 // exactly one NodeAddedEvent, its name == the final title/content default
                                                 // (NOT the constructor default "Text box" when a title was passed)
```

For the parameter assertions read via `JIPipeParameterTree.getParameters(node).get("text-title").get(String.class)` (the `queryAnnotations` test pattern) or the typed getters.

- [ ] **Step 2: Run to verify failure** - COMPILATION ERROR (`createNote`/`CreateNote` missing).

- [ ] **Step 3: Implement**

- `JIPipeAutomationAPI.createNote`: resolve compartment via `AutomationOperationUtils.findCompartment(ctx.getProject(), ref)` -> null means not found -> `{"error": "Compartment not found: " + ref}` (a blank ref also errors - the compartment is REQUIRED for a note; this deliberately differs from `add_node`'s silent compartment-less fallback); route by kind: unknown kind -> `error("Unknown kind: <k> (supported: text-box, comment)")`; comment with content/width/height -> `error("content/width/height are only supported for kind text-box")` naming the offending parameter. text-box: `getInfoById("graph-annotation-text-box")`, null -> `error("Node type not registered: graph-annotation-text-box")`, instantiate, cast to `TextBoxGraphCanvasNote`-compatible base (`DefaultTextBoxGraphCanvasNote`) for title/content, set size on `JIPipeGraphCanvasNote`, insert, set location, THEN publish `NodeAddedEvent` once with the node's final name. comment: same with `"jipipe:comment"` and `setName(title)`.
- `ModificationOperations.CreateNote`: schema `{"type":"object","required":["compartment"],"properties":{"compartment":{...},"kind":{"type":"string","enum":["text-box","comment"],"default":"text-box",...},"title":{...},"content":{"type":"string","description":"Text-box content; only valid for kind text-box"},"x":{"type":"integer","default":0,...grid cells...},"y":{...},"width":{"type":"integer","default":4,"description":"Text-box width in grid cells; only valid for kind text-box"},"height":{"type":"integer","default":3,...}},"additionalProperties":false}`. Description (pin verbatim): "Create a canvas note in one call: a text box (title, content, size, position) or a comment (title is the node name). Sizes and positions are grid cells (1 cell = 25 px). Edit the note later with set_parameter (text-title, text-content, grid-width, grid-height) and delete it with remove_node". Modify, requiresEdt true, read-only false.
- Register after `move_node`.

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=ModificationOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ModificationOperationsTest.java
git commit -m "feat(#1383): add the create_note operation"
```

### Task 3: `query_annotations` -> `query_notes` (+ grid sizes)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryAnnotations` :347 renamed `queryNotes`, key `notes`, add `gridWidth`/`gridHeight` for text boxes)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryAnnotations` -> `QueryNotes`, id, description, agent documentation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (rename the registration line)
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java` (all `QueryAnnotations` references and the `annotations` key -> `notes`)
- Test: same file (add the grid-unit test)

**Interfaces:**
- Consumes: `JIPipeGraphCanvasNote.getGridWidth/getGridHeight` (`api/nodes/annotation/JIPipeGraphCanvasNote.java:56`).
- Produces: operation id `query_notes`, response `{notes: [...]}` with text-box entries carrying `gridWidth`/`gridHeight` in addition to the existing `width`/`height` (pixels).

- [ ] **Step 1: Add the failing test** (in `QueryOperationsTest`)

```java
@Test void queryNotesReportsGridUnits() // a text box with grid-width 6, grid-height 5
    // -> its note entry: gridWidth == 6, gridHeight == 5, width == 150, height == 125 (pixels, unchanged contract)
@Test void queryNotesIsRegisteredAndAnnotationsIsGone() // JIPipe registry: getOperationRegistry().get("query_notes") != null
    // && get("query_annotations") == null (the clean break)
```

- [ ] **Step 2: Run to verify failure** - COMPILATION ERROR (`QueryNotes` missing) / the registry assertion fails.

- [ ] **Step 3: Implement the rename + fields**

Rename the static (`queryNotes`) and the class (`QueryNotes`), keep both filters (`compartment`, `kind`) and all existing fields, rename the response key to `notes`, and add `n.put("gridWidth", note.getGridWidth()); n.put("gridHeight", note.getGridHeight());` for text boxes. New description (pin verbatim): "List all canvas notes of the project (comments and text boxes) with id, uuid, typeId, kind, compartmentId, title, and content for text boxes. Positions and sizes are grid cells (1 cell = 25 px): x/y are the stored grid position; text boxes additionally report gridWidth/gridHeight (the values of the grid-width/grid-height parameters) and width/height in pixels. Resize with set_parameter on grid-width/grid-height, create with create_note". `getAgentDocumentation` adds: the same unit note and that annotation DATA (text/data annotation columns) lives in query_data, not here.

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS (all legacy query_annotations tests, updated, green).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): rename query_annotations to query_notes and report grid sizes"
```

### Task 4: data annotations in `query_data`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryData` :310)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryData.getDescription()`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java` (extend)

**Interfaces:**
- Consumes: `slot.getDataAnnotationColumnNames()` (`JIPipeDataTable:390`), `getDataAnnotationItemStore(row, column)` -> `JIPipeDataItemStore` (`getStringRepresentation()`, `:161`), `JIPipeDataAnnotation.getDataClass()` -> `JIPipeData.getNameOf(Class)` / the registered data type id (`JIPipe.getDataTypes()`; pin whichever is stable - prefer the registered id string).
- Produces: `queryData` response additions `dataAnnotationTypes` (object) + data-annotation values in `rows` and names in `columns`.

- [ ] **Step 1: Write the failing tests** (extend `QueryOperationsTest`; construct the slot data via the project cache or a direct table + `setDataAnnotationItemStore(row, "Mask", new JIPipeDataItemStore(new StringData("m-1")))` (`plugins/strings/StringData`), then assert through `query_data` against a node whose slot carries it - use whichever wiring the implementer verifies works headlessly, document it in the report)

```java
@Test void queryDataWithDataAnnotations()      // row data annotation "Mask" = StringData("m-1")
    // -> columns contains "Mask" after the text-annotation columns; dataAnnotationTypes["Mask"] is a non-blank type id;
    //    rows[0]["Mask"] == "m-1"
@Test void queryDataWithoutDataAnnotationsIsUnchanged() // text annotations only -> no dataAnnotationTypes entries,
                                                        // columns/rows exactly as before
```

- [ ] **Step 2: Run to verify failure** - `queryDataWithDataAnnotations` fails (no `dataAnnotationTypes`, no Mask column).

- [ ] **Step 3: Implement** - append `slot.getDataAnnotationColumnNames()` to `columns`; build `dataAnnotationTypes` from the first non-null item store per column's data class (registered id); per row, put the string representation for each data-annotation column (empty string when absent, the text-annotation convention). Update `QueryData.getDescription()` to (pin verbatim): "Get the data table of a node's slot: row count, column names (Name, text-annotation columns, then data-annotation columns), dataAnnotationTypes (data-annotation column to data type id), and up to limit rows with Name, text-annotation and data-annotation values as readable strings. Use describe_data for full column schemas".

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=QueryOperationsTest` -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/QueryOperationsTest.java
git commit -m "feat(#1383): surface data-annotation columns in query_data"
```

### Task 5: `arrange_nodes` guards

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperations.java` (`ArrangeNodes.execute` :82-123; schema; description)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperationsTest.java` (create)

**Interfaces:**
- Consumes: `JIPipeGraphCanvasNote`, `JIPipeCommentNode` (`instanceof` filters); the existing depth/position logic unchanged.
- Produces: `arrange_nodes` params `compartment` (required, unchanged) + `apply` (boolean, default false); response `{positions: [...], applied: <bool>}`; notes excluded.

- [ ] **Step 1: Write the failing tests** (`ArrangeOperationsTest`; build a compartment with two connected algorithm nodes placed at hand positions plus a text-box note at its own position)

```java
@Test void arrangePreviewWritesNothing()   // apply absent -> positions returned non-empty, applied == false,
                                           // every node's stored location unchanged (the hand positions)
@Test void arrangeApplyWritesPositions()   // apply true -> applied == true; each algorithm node's stored location
                                           // equals its returned position
@Test void arrangeExcludesNotes()          // the note is absent from positions AND its stored location is unchanged
                                           // after apply true
```

- [ ] **Step 2: Run to verify failure** - `arrangeApplyWritesPositions` cannot pass (no `apply` param; today apply is unconditional), `arrangeExcludesNotes` fails (the note is in positions and gets moved).

- [ ] **Step 3: Implement**

- Schema: add `"apply": {"type": "boolean", "default": false, "description": "true commits the computed positions; false (default) only returns them for inspection"}`.
- `execute`: read `apply` (default false); filter `nodes` with `!(n instanceof JIPipeGraphCanvasNote) && !(n instanceof JIPipeCommentNode)` BEFORE `computeDepths`; compute positions into the response array as today but call `setNodeUILocationWithin` ONLY when `apply`; put `applied`.
- Description (pin verbatim): "Arrange the algorithm nodes of a compartment in topological layers and return the proposed positions (grid cells, 1 cell = 25 px). PREVIEW BY DEFAULT: nothing is written unless apply is true - arranging rewrites every stored position in the compartment and is not recoverable, so inspect the proposal first. Canvas notes and comments are excluded from the layout and keep their positions".

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=ArrangeOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperations.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperationsTest.java
git commit -m "fix(#1383): make arrange_nodes preview by default and skip canvas notes"
```

### Task 6: Full-suite verification

- [ ] **Step 1: Run the whole jipipe-core suite** - `mvn -q clean test -pl jipipe-core` (clean per the standing branch-switch ruling; wait it out).
Expected: 0 failures, 0 errors; skips only the known environment-dependent classes (the 4 classes; the post-C/D baseline count moves up by this branch's new tests); zero failures in new or touched test classes.

- [ ] **Step 2: Record the numbers** in the SDD workspace ledger (`.superpowers/sdd/2026-11-20-wi1383-node-movement-notes/progress.md`): suite counts, any deferred minors, and the (empty) by-eye list.

No commit (ledger path is git-ignored).