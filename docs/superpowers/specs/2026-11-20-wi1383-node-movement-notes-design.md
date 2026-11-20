# Spec: #1383 sub-project G - node movement and canvas notes

Date: 2026-11-20
Status: DRAFT for user review
Work item: #1392 (child of #1383, MCP enhancements). Depends on: A (!204), F (!205), E (!206), B (!207), C (!208) merged; D (!209) open on a parallel branch - shared-file overlap limited to disjoint regions of `JIPipeAutomationAPI.java` and `AutomationPlugin.java` (second merger rebases). Branch base: post-C master `1a19f3f2d4`.
Source: user charter additions 2026-11-20 ("we would specifically need the ability to move nodes (visual arrangement by agent, and creating canvas notes etc)"), the user's answers to the 3 charter questions (workflow organization and annotations gaps -> G), and two design rulings made in chat (2026-11-20): the note-vs-annotation concept split and the grid-unit disclosure.

## 1. Intent

An agent arranging a workflow needs to reposition EXISTING nodes (fine-grained control beyond `arrange_nodes` auto-layout), create configured canvas notes in one call, and read the data-annotation columns of a node's data. The agent-facing naming must separate the two concepts the GUI already separates internally: NOTES (canvas furniture: comment nodes, text boxes) vs ANNOTATIONS (the data concept: text-annotation and data-annotation columns on table rows).

## 2. Confirmed decisions

1. **Design approved in chat** (user, 2026-11-20): G1 `move_node`, G2 `create_note` + the `query_annotations` rename, G3 data annotations in `query_data`.
2. **Naming ruling** (user, 2026-11-20): "annotations" is a data concept (text annotation, data annotation); the canvas concept is "note". `query_annotations` becomes `query_notes` (clean rename, no deprecated alias - the surface is young and single-user; Query/read-only ops carry no persisted permission state). After G: `query_data` = the annotation data concept, `query_notes` = the canvas-note concept.
3. **Grid units are the wire unit everywhere**: stored node locations (`setNodeUILocationWithin`), `add_node`'s x/y, `arrange_nodes`' returned positions, `query_annotations`' x/y, and the new `move_node` x/y and `create_note` width/height are all in grid cells (1 cell = 25 px at zoom 1, `JIPipeDesktopGraphCanvasGrid`). The read/write unit-mismatch trap (pixel `width`/`height` in the read response vs grid-unit `grid-width`/`grid-height` parameters on the write side) is closed by exposing `gridWidth`/`gridHeight` on the read side and documenting them as THE resize inputs.
4. **No overlap enforcement in `move_node`**: notes overlap nodes by design (the `z-order` parameter exists for exactly that); the GUI drag's overlap check (`moveToGridLocation(force=false)`) is a UI affordance, not data semantics.
5. **Arrow and image-box note kinds are OUT** (ledgered, section 8): the arrow needs extent/anchor semantics and the image box needs a binary-input flow (file path + load). `text-box` and `comment` are the v1 kinds.
6. **Live-canvas caveat accepted**: `move_node`/`create_note` write stored state headlessly exactly like the shipped `arrange_nodes`; an open canvas reflects the changes on its next refresh. No new event type (`arrange_nodes` publishes nothing for moves either).
7. **Editing/deleting notes stays on the existing ops**: `set_parameter` (text-title/text-content/grid-width/grid-height) and `remove_node` - documented in `create_note`'s description, not reimplemented.

## 3. Verified ground (master 1a19f3f2d4)

- **Creation/edit primitives already exist**: `add_node` (ModificationOperations :21 - node-type-id, compartment-id, x/y grid units; delegates to `JIPipeAutomationAPI.addNode` :546: `info.newInstance()`, `graph.insertNode(node, compartmentUuid)`, `setNodeUILocationWithin`, one `NodeAddedEvent`), `remove_node` (:560, `graph.findNode` resolves alias OR UUID), `set_parameter`, `add_connection`/`remove_connection`, `arrange_nodes` (ArrangeOperations :55 - auto-layout writing stored locations headlessly, no event, Modify/read-only-false/`requiresEdt() = true`).
- **Notes are registered node types**: `registerAnnotationNodeType` -> `registerNodeType` (`JIPipeDefaultJavaPlugin:267`), so `add_node("graph-annotation-text-box")` creates a bare note. Concrete classes: `TextBoxGraphCanvasNote` (constructor sets text-title "Text box"; parameters `text-title`, `text-content`, `shape-parameters`) extends `DefaultTextBoxGraphCanvasNote` extends `JIPipeGraphCanvasNote` (`api/nodes/annotation/JIPipeGraphCanvasNote.java`: `grid-width`/`grid-height` parameters, defaults 4/3; `z-order`; SEALED empty slot configuration via `JIPipeDefaultMutableSlotConfiguration.builder().seal().build()` - the sealed-by-default truth, JIPipeGraphNode.java:118 authority). `JIPipeCommentNode`: title IS the node name; type id `jipipe:comment` (confirmed in the suite log).
- **Stored location API**: `JIPipeGraphNode.setNodeUILocationWithin(compartment, Point)` (:533) writes nodeMetadata keys `("location", compartment, "x"/"y")`; `getNodeUILocationWithin` (:516) returns null when never set. The GUI converts grid to pixels via `JIPipeDesktopGraphCanvasGrid.gridToRealLocation` (GRID_WIDTH = GRID_HEIGHT = 25, :22-23) - the STORED value is grid units.
- **`query_annotations`** (`QueryOperations.QueryAnnotations` :165, static `JIPipeAutomationAPI.queryAnnotations` :347): returns `{annotations: [{id, uuid, typeId, kind: "comment"|"text-box", compartmentId, title, content?, x, y, width?, height?}]}`. Unit inconsistency inside one response: x/y are RAW stored locations (grid units), width/height are grid size x GRID_WIDTH (pixels). The rename keeps x/y as grid units (documented) and adds `gridWidth`/`gridHeight`.
- **`queryData`** (`JIPipeAutomationAPI:310`): columns = `["Name"]` + text-annotation names; rows carry `Name` (string representation via `slot.getDataItemStore(row).getStringRepresentation()`) + text-annotation values (`getTextAnnotationMap(row).get(col).getValue()`). Data annotations are never surfaced. The data-annotation API on the table: `getDataAnnotationColumnNames()` (`JIPipeDataTable:390`), per-row `getDataAnnotationItemStore(row, column)` -> `JIPipeDataItemStore`; `JIPipeDataAnnotation.getDataClass()` (`annotation/JIPipeDataAnnotation.java:45`) gives the column's data type; `JIPipeData.getNameOf(Class)` renders a type name (verify the id-vs-name choice at implementation; prefer the registered data type id for agent stability).
- **Events**: `NodeAddedEvent`, `NodeRemovedEvent`, `ConnectionChangedEvent`, `ParameterChangedEvent` exist (api/automation/events/); NO move event exists and `arrange_nodes` publishes none - G adds none.
- **Classification precedent for graph mutation**: `add_node`/`arrange_nodes` = Modify, `isReadOnly() = false`, `requiresEdt() = true` (house convention even though the write itself is headless-safe metadata). No `ModificationOperationsTest` exists; graph-mutating statics are tested headlessly by direct invocation (the house pattern of the sibling operation tests).
- `graph.findNode(idOrAlias)` is the single resolution entry used by `removeNode`/`addConnection`/`queryData`.

## 4. G1: `move_node` (new Modify operation)

New operation in `ModificationOperations` (beside `AddNode`), delegating to a new static `JIPipeAutomationAPI.moveNode(ctx, nodeId, x, y)`.

- Params: `node` (required, alias or UUID), `x` (required integer), `y` (required integer) - grid units.
- Resolution: `graph.findNode(node)` (null -> error "Node not found: <id>"); compartment key = the node's OWN compartment (`graph.getCompartmentUUIDOf(node)`, empty string when null - exactly the key `add_node` writes for compartment-less nodes). No compartment parameter: the location is always keyed by the node's own compartment.
- Write: `node.setNodeUILocationWithin(compartmentKey, new Point(x, y))` - the exact `arrange_nodes` write per node. No overlap check, no event.
- Response: `{ "node": <alias>, "uuid": <uuid>, "compartment": <uuid or null>, "x": x, "y": y, "previous": {"x": px, "y": py} | null }` - `previous` read via `getNodeUILocationWithin` BEFORE the write (null when the node had no stored location).
- Description (must state): grid units (1 cell = 25 px); works on any node including canvas notes; no overlap enforcement; the canvas reflects the move on its next refresh (same as arrange_nodes); use arrange_nodes for automatic topological layout.
- Classification: Modify, read-only false, `requiresEdt() = true` (house convention).

## 5. G2: `create_note` (new Modify operation) + `query_annotations` -> `query_notes`

### 5a. `create_note`

New operation in `ModificationOperations` (after `AddCompartment`/`RenameCompartment`), delegating to a new static `JIPipeAutomationAPI.createNote(ctx, compartmentRef, kind, title, content, x, y, width, height)`.

- Params: `compartment` (required, UUID/alias/name), `kind` (optional enum `text-box|comment`, default `text-box`), `title` (optional string), `content` (optional string), `x`/`y` (optional integers, default 0), `width`/`height` (optional integers, default 4/3, text-box only).
- `content` on kind comment -> error "content is only supported for kind text-box" (fail fast, not silent ignore); same for `width`/`height` on comment.
- text-box route: `JIPipe.getNodes().getInfoById("graph-annotation-text-box")` (null -> error naming the id - the canvasnotes plugin must be registered), `newInstance()`, `setTextTitle(title or "Text box")`, `setTextContent(content or "")`, `setGridWidth(width)`, `setGridHeight(height)`, `insertNode(compartmentUuid)`, `setNodeUILocationWithin(compartmentUuid, Point(x, y))`, publish ONE `NodeAddedEvent` after the parameters are applied (mirrors `addNode`'s flow; the event carries the final name).
- comment route: `getInfoById("jipipe:comment")`, `newInstance()`, `node.setName(title or "Comment")` (the comment title IS the node name), insert, location, event. No size/content.
- Unknown compartment -> error (reuse `addNode`'s `resolveCompartmentUUID` behavior/wording).
- Response: `{ "nodeId": <alias>, "uuid": <uuid>, "kind": <kind>, "title": <title>, "x": x, "y": y, "width": w (text-box), "height": h (text-box) }`.
- Description (must state): one-call creation of canvas notes; the note can then be edited with set_parameter (text-title, text-content, grid-width, grid-height) and deleted with remove_node; width/height are grid cells (1 cell = 25 px); comments carry their title as the node name.
- Classification: Modify, read-only false, `requiresEdt() = true`.

### 5b. `query_annotations` -> `query_notes`

- Registration id becomes `query_notes`; the `query_annotations` registration is REMOVED (clean break, no alias). The static `queryAnnotations` is renamed `queryNotes`; the op class `QueryAnnotations` renamed `QueryNotes`.
- Response: `{ "notes": [...] }` (key renamed from `annotations`); entry fields unchanged EXCEPT text-box entries additionally carry `"gridWidth"` and `"gridHeight"` (the raw parameter values, grid cells).
- Description (must state): lists all canvas notes (comments and text boxes) of the project; x/y are stored grid positions (1 cell = 25 px); text boxes additionally report gridWidth/gridHeight (the values of the grid-width/grid-height parameters) and the display width/height in pixels; resize an existing note with set_parameter on grid-width/grid-height; create new notes with create_note.
- `getAgentDocumentation` mirrors the same. Classification unchanged (Query, read-only).

## 6. G3: data annotations in `query_data`

Backward-compatible additions to the `queryData` response:

- `columns` gains the data-annotation column names (appended after the text-annotation names; `Name` stays first).
- New `"dataAnnotationTypes"` object: `{ <columnName>: <data type id> }` for every data-annotation column (via `JIPipeDataAnnotation.getDataClass()` -> the registered data type id; `getNameOf` only if the id is not retrievable - pin the id).
- Row values: for each data-annotation column, the row object carries the value's string representation via the row's data-annotation item store (the same convention as the `Name` column), empty string when the cell has no value.
- Description updated: names both column kinds (text annotations and data annotations), states the string-representation convention, and points at `describe_data` for full column schemas.

## 7. Testing policy (house rules; all headless - G has no by-eye items)

New test class `ModificationOperationsTest` (house pattern: `ensureJIPipe`, `createContext`, registered test node types where needed; the static methods are invoked directly):

- **move_node**: round-trip (create node via the `addNode` static at (0,0), move to (5,7) -> stored location reads (5,7); response carries node/uuid/compartment/x/y and `previous` = {0,0}); moving a node without a stored location -> `previous` null; unknown node -> error; a text-box note moves like any node.
- **create_note**: text-box with title/content/width/height/x/y -> node present in the compartment graph, text-title/text-content/grid-width/grid-height/location all stored, response shape exact; comment kind -> node name == title, no width/height in response, setting content/width on comment -> error; defaults (kind text-box, x/y 0, size 4/3, title "Text box"); unknown compartment -> error.
- **query_notes**: the renamed tests (existing `QueryAnnotations` tests updated to the new id/key) + a text box reports `gridWidth`/`gridHeight` equal to its parameter values (grid cells, NOT pixels).
- **query_data**: a slot carrying data-annotation columns (construction path verified by the implementer - e.g. `addDataAnnotationColumn` on the table API or a test algorithm producing them); assert columns include them, `dataAnnotationTypes` carries the type ids, row values are string representations, absent cells are "".
- Registry/schema gates pick up the new/renamed ops automatically; a renamed-id test asserts `query_notes` is registered and `query_annotations` is gone.

## 8. Out of scope (ledgered)

- Arrow and image-box note kinds (arrow extent/anchor semantics; image-box binary input flow - a file-path + load design of its own).
- Node reparenting across compartments (`move_node` positions within the node's own compartment; compartment assignment is creation-time via `add_node`'s compartment-id).
- A `NodeMovedEvent` (no move event exists; `arrange_nodes` publishes none).
- Canvas-refresh push for open editors (the shipped `arrange_nodes` caveat).
- Undo/history (no automation-layer concept for it exists).
