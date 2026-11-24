# Spec: #1383 sub-project E - parameter and report introspection (+ slot editing)

Date: 2026-11-19
Status: DRAFT for user review
Work item: #1387 (child of #1383, MCP enhancements). Depends on: sub-project A (merged, !204) whose `renderParameterValue` seam and normalized `query_node` shape this sub-project builds on; sub-project F (merged, !205) - independent file surface.
Source: user additions to #1383 (2026-11-19): "read parameters (and write them + verification), view/edit slots (if slots are editable), list parameters, list parameters that are changed compared to default (see project report), obtain project report (caution: can be long)". Slot editing was gated pending a design; the user resolved the gate on 2026-11-19 (section 6).

## 1. Intent

An agent configuring or documenting a pipeline needs, in one place: the meaning of each parameter (docs joined onto instance values), which parameters the user customized versus the node type default, verification that a parameter write produced the intended effective value, and the project report text. Slot editing becomes possible under an explicit, connection-safe protocol: connections are never dropped implicitly - the agent disconnects first (existing `remove_connection`), then edits the slot.

## 2. Confirmed decisions

1. **Design approved in chat** (user, 2026-11-19): sections 3-5 as presented.
2. **Slot-editing gate resolved** (user, 2026-11-19): "(1) ensure that we have an explicit disconnect and connect slot command (2) edit slots is verified so that it does not allow disconnecting slots (result back: unable to disconnect because of the following connections). (3) agent must explicitly disconnect; this combines safety + convenience". The existing `add_connection`/`remove_connection` operations are the explicit connect/disconnect commands; the new slot operations never drop connections and refuse with the connections named.
3. **Sub-project order** (charter): E third of six (after A, F - both merged).

## 3. E1: parameter docs join (`query_node` extension)

New optional boolean param `include-docs` (default false). When true, every entry of the `parameters` map gains `name`, `description`, and `important` - taken from the parameter access (`JIPipeParameterAccess.getName()/getDescription()/isImportant()`, `api/parameters/JIPipeParameterAccess.java:45-73`), which is the same source `get_node_docs` renders for node types. The existing `value`/`type`/`raw?` fields are unchanged; docs fields are absent when `include-docs` is false (default responses stay byte-compatible with sub-project A's shape). `additionalProperties: false` is preserved; the schema description documents that docs come from the node type's parameter documentation.

## 4. E2: changed-vs-default diff (`get_parameter_diff`, new read-only op)

New Query operation in `QueryOperations`, registered as `get_parameter_diff`. Params: `node-id` (required), `include-unchanged` (optional boolean, default false).

Machinery (the project report's own approach, `JIPipeGraphNode.getTextDescription` at `api/nodes/JIPipeGraphNode.java:1263`): instantiate a reference node via `node.getInfo().newInstance()`, build `JIPipeParameterTree` over both the live node and the reference, compare per key with `Objects.equals`, and skip the `jipipe:node:name` and `jipipe:node:description` keys (same exclusions as the report). Keys present only on one side count as changed (missing default renders as `null`).

Response: `{ "nodeId": ..., "parameters": [ { "key", "value", "type", "defaultValue", "changed" } ], "changedCount" }` - both sides rendered through the same normalizer as `query_node` (`JIPipeAutomationAPI.renderParameterValue` semantics: `getTextDescriptionOf` when implemented, else `toString()`); `defaultValue` is JSON null when the reference has no such key. Keys are iterated from the CURRENT tree only (the report's own behavior; default-side-only keys are out of scope). `changedCount` counts the changed entries; `parameters` carries changed-only unless `include-unchanged` is true. Read-only, no EDT requirement (pure data-side reads; `newInstance()` must not touch UI - the report machinery already runs in this context).

## 5. E3: `set_parameter` read-back verification

`set_parameter`'s response changes from `{ "success": true }` to `{ "success": true, "parameter": { "value", "type", "raw"? } }` - the effective value after conversion, rendered by the same `renderParameterValue` seam as `query_node` (sub-project A). The description gains: "The response carries the effective value after conversion - verify it matches your intent." Additive change (existing consumers reading `success` are unaffected).

## 6. E5: slot editing (gate lifted; explicit, connection-safe protocol)

Verified ground (corrected 2026-11-19, implementer-verified + controller-confirmed at `JIPipeGraphNode.java:118`): slot configurations are `JIPipeSlotConfiguration`; editable nodes implement `JIPipeMutableSlotConfiguration` (`api/data/JIPipeMutableSlotConfiguration.java`) with `allowsInputSlots()/allowsOutputSlots()`, `isInputSlotsSealed()/isOutputSlotsSealed()`, `canModifyInputSlots()/canModifyOutputSlots()` (= `allows* && !is*Sealed`), `canAddInputSlot()/canAddOutputSlot()`, `canCreateCompatibleInputSlot/OutputSlot(Class)`, `canRemoveInputSlot/OutputSlot(name, user)` (= `isUserModifiable() && canModifySlots`), `addSlot(name, JIPipeDataSlotInfo, user)`, `removeInputSlot/OutputSlot(name, user)`. **Annotation-built configurations are SEALED on both sides**: the automatic builder path calls `builder.seal()` (`JIPipeGraphNode.java:118`), so stock annotation-only nodes (e.g. the comment node) are refused by slot editing with the sealed-side message - consistent with the GUI, which hides slot editing for sealed configurations. Slot editing applies to nodes whose authors pass an explicit, unsealed `JIPipeDefaultMutableSlotConfiguration` to the `(info, configuration)` constructor. The non-mutable case exists for group/wrapper algorithms (`JIPipeGraphWrapperAlgorithm` implements the fixed interface directly). The GUI's slot manager casts to the mutable interface and calls exactly these (`JIPipeDesktopGraphNodeUISlotManager.java:199-206`). Slot info construction: `JIPipeDataSlotInfo(Class<? extends JIPipeData>, JIPipeSlotType, name, description)` (`api/data/JIPipeDataSlotInfo.java:100`). Data-type ids: `JIPipe.getDataTypes().getById(id)` THROWS on unknown ids (it does not return null), so the unknown-id gate reads the registered map directly; `jipipe:data` is a resolvable generic id. Graph edges: `JIPipeGraph.getSlotEdges()` (the same source `query_graph` reads).

New operations in a new `SlotOperations.java` group file, both Modify (Ask by category default):

- **`add_slot`**: params `node-id` (required), `slot-type` (required enum `input|output`), `name` (required, non-blank), `data-type` (required, a registered data type id; unknown ids are refused listing known ids - the template-id refusal style), `description` (optional). Precondition gates, checked in order, each refusal naming the offending condition: (1) the node's slot configuration implements `JIPipeMutableSlotConfiguration` - otherwise `node does not support slot editing`; (2) slots of that type are allowed and not sealed and modifiable (`allows*`, `is*Sealed`, `canModify*`); (3) `canAdd*Slot()`; (4) `canCreateCompatible*Slot(dataClass)`; (5) the name is unused on that side (`hasInputSlot/hasOutputSlot`). Response: `{ "added": true, "name": ..., "slotType": ... }`.
- **`remove_slot`**: params `node-id`, `slot-type`, `name` (all required). Gates: (1) mutable configuration; (2) the slot exists on that side - otherwise an error listing the existing slot names; (3) `canRemove*Slot(name, true)` - its refusal message names that the slot is not user-modifiable or the node seals that side. Then the **connection check (the user's ruling)**: if any graph slot edge touches this node's slot, refuse with `unable to remove slot '<name>': it has <n> connection(s): <source node> (<source slot>) -> <target node> (<target slot>); disconnect them first with remove_connection`. Connections are never dropped implicitly - the agent must call `remove_connection` (existing) for each, then `remove_slot`. Response: `{ "removed": true, "name": ..., "slotType": ... }`.

Both descriptions state the protocol explicitly: add_connection/remove_connection are the connect/disconnect commands; slot operations never disconnect anything.

## 7. E4: project report (`get_project_report`, new read-only op)

New Query operation (in `InfoOperations` or its own group file - implementer follows house grouping). Renders `JIPipeProject.getTextDescription(sb, 3)` (`api/project/JIPipeProject.java:1653`) - the Project Report UI's own text (compartments, nodes, connections, non-default parameters), heading level fixed at 3 and documented.

Size guard: the rendered text is measured; inline responses are capped at 100,000 characters - above the cap the response is an error advising the `save-to` parameter (the error states the actual size). `save-to` (optional) routes through the same rooted machinery as the screenshot operations (`AutomationOperationUtils.resolveWriteTarget`: the `jipipe-mcp-export` directory in the system temp directory, or the project work directory; targets outside are refused naming the roots), writes the full text (UTF-8) and returns `{ "savedTo": ..., "sizeBytes": ... }` - the screenshot size contract applies: with a `save-to` target only the file path and size are returned, never the inline text. Without `save-to` and under the cap: `{ "report": "<text>", "truncated": false }` (`truncated` is always false today; the field pins the contract that the cap is a hard refusal, never silent truncation).

## 8. Permissions

`get_parameter_diff`, `get_project_report`: Query, read-only, Allow by category default. `add_slot`, `remove_slot`: Modify, non-read-only, Ask by category default. `set_parameter`/`query_node` keep their existing classifications. No broker changes (all project-bound).

## 9. Testing policy

Headless-safe focused tests in jipipe-core over programmatically built projects (house pattern; the test node types `jipipe:comment` and `graph-annotation-text-box` plus a node type with configurable slots - verified available or registered-if-absent per the sub-project A pattern):

- E1: `include-docs` adds `name`/`description`/`important` per key from the access; default response unchanged (byte-shape); docs fields absent when not requested.
- E2: unchanged node -> empty changed list; modified parameter -> exactly that key changed with the correct current/default rendering; removed-optional-parameter case (present current, missing default -> changed, `defaultValue` null); `include-unchanged` full inventory; the two `jipipe:node:*` exclusions.
- E3: set_parameter response carries the effective normalized value (string param, enum param); failure paths unchanged.
- E5: the SEALED refusal is pinned by a real stock node (the comment node - annotation-built configurations are sealed, `JIPipeGraphNode.java:118`); add/remove happy paths use a programmatically unsealed configuration (`setInputSealed(false)/setOutputSealed(false)` on the obtained `JIPipeDefaultMutableSlotConfiguration`) or a test-local fixture with an explicit unsealed configuration; duplicate-name refusal; unknown data-type refusal listing known ids; remove with a connection -> refusal listing the connection endpoints (build the edge programmatically); disconnect-then-remove succeeds; remove without connections succeeds. The `node does not support slot editing` refusal is pinned with a test-local fixture node whose `getSlotConfiguration()` returns a non-mutable `JIPipeSlotConfiguration` (a minimal fixed stub registered under a test-only id) - the same register-if-absent mechanism.
- E4: fresh project report under the cap inline; a size-above-cap case is impractical to build programmatically - pin the cap logic by calling the operation with a synthetic oversized text through the extracted measure/guard seam (implementation extracts the guard for testability, same pattern as `renderParameterValue` in sub-project A); save-to happy path inside the export root; save-to outside refused naming the roots.
- No Swing interaction tests (standing policy).

## 10. Expected files

- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` - `query_node` include-docs, new `GetParameterDiff`.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` - the diff API method (reusing `renderParameterValue`), possibly the query_node docs join.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` - `SetParameter` response extension.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/SlotOperations.java` - new file (`AddSlot`, `RemoveSlot`).
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` or a new report group file - `GetProjectReport` (implementer follows house grouping).
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` - register `get_parameter_diff`, `add_slot`, `remove_slot`, `get_project_report`.
- Test class(es) under `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/`.

## 11. Out of scope

- Slot property editing beyond add/remove (rename, data type change, reorder) - not requested; the add/remove protocol establishes the pattern.
- Anything touching the permission model, broker, or `GLOBAL_TOOL_IDS`.
- Slot editing for nodes whose configuration is not `JIPipeMutableSlotConfiguration` (refused by design).
- The AI-chat presentation of the report (the report text is the contract; consumers render it).
