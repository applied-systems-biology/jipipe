# Spec: #1383 sub-project A - query correctness + shaping

Date: 2026-11-19
Status: DRAFT for user review
Work item: #1383 (MCP enhancements). Depends on: MCP v1 (#1369, MR !198), agentic interactions (#1379, MR !200), released-testing fixes (MR !201), all merged; master 67b0a501c8.
Source feedback: the #1383 work item (agent-driven 96-node presentation workflow, 2026-11-19); items 3, 4, 7, 8.

## 1. Intent

An agent reconstructing a 96-node project had to fight the query layer: the `query_graph` compartment filter silently did nothing, node positions bloated every response, per-node compartment attribution required a manual join, comment text took eight sequential `query_node` calls and did not surface at all for comment nodes, there was no way to search inside the open project, and parameter values arrived wrapped (`Expression: ` prefixes, value/expression envelopes).

This sub-project makes the query layer correct and cheap to consume: the filter is honored and fails loudly, annotations are one call, in-project search exists, and parameter values are normalized. It is the first of six sub-projects of #1383 (charter in `.superpowers/sdd/2026-11-19-wi1383/progress.md`); the follow-ups are F (project metadata, FAIR, user paths), E (parameter/report introspection), B (rendering + view state), C (broker ergonomics), D (pipeline map + composite describe).

## 2. Confirmed decisions

1. **Scope and order** (user, 2026-11-19): all six sub-projects, order A, F, E, B, C, D; slot editing (inside E) is gated behind its own design and an explicit user go.
2. **Sub-project A design** (user, 2026-11-19): approved as presented - filter fix + shaping, `query_annotations`, `find_nodes`, normalized parameter values.
3. **Breaking response-shape change accepted** (presented and approved): `query_node`'s `parameters` map changes from flat strings to objects. The user is the primary MCP consumer.

## 3. `query_graph`: filter fix, attribution, shaping (feedback item 3)

### 3.1 The defect

`JIPipeAutomationAPI.queryGraph` (`JIPipeAutomationAPI.java:59-112`) resolves the `compartment-id` filter through `JIPipeProject.findCompartment`, which matches UUIDs and alias ids but **not** display names. When the filter cannot be resolved, `compartmentFilter` stays null and the operation silently returns every node and connection of the project. The agent's `compartment: "Preprocessing"` (a display name) therefore returned all 96 nodes. The name-fallback resolution already exists as `AutomationOperationUtils.findCompartment` and is used by the screenshot and UI operations; `query_graph` predates it.

### 3.2 Fix

- Resolution moves into `JIPipeAutomationAPI` as a public `findCompartment(project, ref)` (UUID, alias id, and display name); `queryGraph` uses it internally, and `AutomationOperationUtils.findCompartment` delegates to it, so the semantics live in exactly one place and the EDT-side callers keep their entry point.
- An unresolvable non-blank `compartment-id` throws; `QueryGraph` catches and returns the error envelope: `Compartment not found: 'X'. Known compartments: A, B, C (see query_compartments for ids)`. Names are listed because after this fix names are resolvable; `query_compartments` remains the source for UUIDs.
- Every node object gains `compartmentId` (the compartment UUID string), so agents can attribute nodes without joining the pipeline map. Connection objects are unchanged (compartment-to-compartment wiring is sub-project D).

### 3.3 Response shaping

All optional, `additionalProperties: false`, kebab-case per house convention:

| Param | Type | Effect |
|---|---|---|
| `fields` | array of enum `id, name, typeId, compartmentId, uuid, x, y` | Whitelist for node objects; omitted fields are left out. Default: all fields. Applies to nodes only (connections keep their shape). |
| `omit-positions` | boolean, default false | Drops `x` and `y` from node objects (the largest unused payload in the observed session). |
| `connections-only` | boolean, default false | Returns the `connections` array and an empty `nodes` array. |

`omit-positions` is kept as a named convenience although `fields` can express it: the feedback asked for it by name, and it is the option agents will actually reach for.

## 4. `query_annotations` (feedback item 4)

New read-only Query operation; one call returns every comment and text box of the project.

Response: `{ "annotations": [ { id, uuid, typeId, kind, compartmentId, title, content?, x, y, width?, height? } ] }`

- `id` is the alias id (the same id `query_graph` and `navigate_to_node` use), `uuid` the node UUID.
- `kind` is `"comment"` (instanceof `JIPipeCommentNode`) or `"text-box"` (instanceof `JIPipeGraphCanvasNote`), decided by type, never by typeId string matching.
- Comment nodes carry their text as the node name (verified: `JIPipeCommentNode` has no text parameter, only colors and an icon): `title` = node name, `content` omitted, no `width`/`height` (their on-screen size is UI-determined).
- Text-box canvas notes: `title` = the `text-title` parameter, `content` = the `text-content` parameter (`DefaultTextBoxGraphCanvasNote.java:116-134`), `width`/`height` = grid size converted with the canvas grid constants (25 px per grid cell, `JIPipeDesktopGraphCanvasGrid.GRID_WIDTH/GRID_HEIGHT`), so the bbox lives in the same coordinate space as the `x`/`y` that `query_graph` reports.
- `x`/`y` come from the same source `query_graph` uses (`node.getNodeUILocationWithin(compartmentUuid)`).

Params: `compartment` (optional, resolved like `query_graph`, same fail-loud error), `kind` (optional, enum `comment` | `text-box`). No EDT requirement: all reads are data-side.

## 5. `find_nodes` (feedback item 7)

New read-only Query operation: search inside the open project. `search_nodes` searches the node-type catalog (`JIPipe.getNodes()`, `JIPipeAutomationAPI.searchNodes:368-387`); `find_nodes` searches the project's graph. Both tool descriptions get the cross-reference so agents stop confusing them.

Params (all optional, `additionalProperties: false`):

| Param | Type | Effect |
|---|---|---|
| `query` | string | Case-insensitive substring match on node name. Empty/omitted: matches all nodes. |
| `type-id` | string | Case-insensitive substring match on the registered node type id (e.g. `table-export` matches `org.hkijena.jipipe:table-export`). |
| `compartment` | string | Restricts to one compartment (UUID, alias, or name; same resolution and fail-loud error as `query_graph`). |
| `limit` | integer | Maximum matches returned, default 50. |

Response: `{ "matches": [node objects, same shape as query_graph nodes], "totalCount": N }` - `totalCount` exceeds `matches.length` when truncated, so the agent knows to narrow the query.

## 6. Normalized parameter values in `query_node` (feedback item 8)

The `parameters` map changes from `key -> "toString()"` to `key -> { "value", "type", "raw"? }`:

- `value`: the effective string. Produced with `JIPipeCustomTextDescriptionParameter.getTextDescriptionOf` (the project report's renderer, `JIPipeCustomTextDescriptionParameter.java:29-46`) when the value implements it, and `toString()` as the remaining fallback. Verified during exploration: the whole expression family already implements the interface with the intended semantics (`DefaultExpressionParameter.getTextDescription()` returns the bare expression, `:255`; `JIPipeExpressionParameter` and the data-export wrapper `DataExportExpressionParameter` inherit it), so the `"Expression: "` prefix and the value/expression wrappers disappear through the interface alone - no per-class special cases needed. Per-class encodings are nevertheless verified against the parameter classes during implementation; the classes are the authority (standing ruling).
- `type`: the parameter's field class simple name (the join key to `get_node_docs`, which already reports key, name, description, type per parameter of the node type).
- `raw`: the plain `toString()`, present only when it differs from `value`.

The `jipipe:` / `jipipe-ui:` key exclusion is unchanged. The shape change is flagged in the operation description (the confirmed breaking change, section 2.3).

## 7. Permissions

No new Ask surface: all touched and new operations are read-only (Query category, `isReadOnly() = true`), which resolves to Allow by the category default. No broker changes: none of the four tools is registry-global (all are project-bound).

## 8. Testing policy

Headless-safe focused tests in jipipe-core over programmatically built projects (same pattern as the existing automation-operation tests):

- Filter resolution: UUID, alias, and name; the fail-loud error listing known compartments; the previous silent-all-nodes behavior as an explicit regression case (a name that matches no compartment must error, not return everything).
- Shaping: `fields` whitelist, `omit-positions`, `connections-only`, and combinations; default response unchanged in shape apart from the added `compartmentId`.
- `query_annotations`: a project containing a comment node and a text-box note; kind detection, title/content sources, bbox units and coordinate space, compartment and kind filters.
- `find_nodes`: name substring case-insensitivity, type-id substring, compartment filter, limit + totalCount truncation.
- Normalization: an expression parameter and a data-export wrapper parameter (bare expression, no `Expression: ` prefix; `raw` carries the `toString()` form when it differs), a plain string parameter, and a type whose `getTextDescription` differs from `toString`.

No Swing interaction tests (standing policy); UI rendering is unaffected by this sub-project.

## 9. Expected files

- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` - queryGraph resolution + throw + `compartmentId`, annotations and findNodes API methods, queryNode normalization.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` - QueryGraph schema and params, new QueryAnnotations and FindNodes operations.
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` - register `query_annotations` and `find_nodes`.
- New test class(es) under `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/`.

## 10. Out of scope (belongs to later sub-projects or nowhere)

- Pipeline map wiring and status, `describe_project` (D).
- Project metadata fields, FAIR check, user paths (F).
- Parameter metadata listing, changed-vs-default diff, project report, set_parameter read-back, slot editing (E).
- Graph image export, view-state hygiene, UI surfaces (B).
- Broker routing, `ping` (C).
- Any change to `search_nodes` behavior (only its description gains the cross-reference).
