# #1383 Sub-project E - Parameter/Report Introspection + Slot Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give agents the parameter meaning (docs join), the changed-vs-default diff, write verification, the project report, and connection-safe slot editing (add/remove) under the user's explicit-disconnect protocol.

**Architecture:** All work is in the existing operation layer. `get_parameter_diff` and the docs join reuse sub-project A's `renderParameterValue` seam; slot operations wrap the `JIPipeMutableSlotConfiguration` contract with the connection-safety refusal; `get_project_report` reuses the rooted save-to machinery. `AutomationPlugin` registers four new tools.

**Tech Stack:** Java 21, jackson databind, JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-19-wi1383-parameters-report-slots-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- kebab-case operation param names; `additionalProperties: false` on every schema.
- Read-only ops (`get_parameter_diff`, `get_project_report`): `getCategory() = Query`, `isReadOnly() = true`, no `requiresEdt()` override. Mutating ops (`add_slot`, `remove_slot`): `Modify` category, `isReadOnly() = false`, `requiresEdt() = true` (the SetParameter/SetUserPath house form).
- Branch `1383-parameters-report-slots` (created from master bcec5fbc30; spec commit bbdc0b796a).
- Test command after each task: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest,SlotOperationsTest,ProjectReportOperationsTest,QueryOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot.
- House test policy: no tests that restate the implementation; no Swing interaction tests. The register-if-absent test pattern applies where a node type is unavailable on the test classpath (sub-project A precedent; ONE unconditional smoke assertion per file, the rest `assumeTrue`).
- Verified ground for slot work (spec section 6): `JIPipeDefaultMutableSlotConfiguration` defaults `allow*=true`, `sealed=false` (annotation-built configs are mutable); `JIPipe.getDataTypes().getById(id)` resolves data-type ids; `JIPipeGraph.getSlotEdges()` is the edge source.

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **`addSlot` on a sealed configuration throws `JIPipeValidationRuntimeException` from inside the configuration** (`JIPipeDefaultMutableSlotConfiguration.java:126,140`) instead of returning - the operation must gate with `is*Sealed()` BEFORE calling `addSlot`, so the agent gets a clean error envelope, never a thrown runtime exception. Pinned in Task 4 (sealed fixture).
2. **A slot edge whose endpoints live in DIFFERENT compartments touching the removed slot** - the connection check must scan `getSlotEdges()` by node identity + slot name regardless of compartment, and the refusal must render both endpoint node names (not UUIDs). Pinned in Task 4.
3. **`renderParameterValue` on a REFERENCE instance's access may return `"null"` for an optional parameter never set** - the diff's `defaultValue` for such keys is the rendered string `"null"`, NOT JSON null (JSON null is reserved for the key being absent from the reference tree). Pinned in Task 2.
4. **The report's inline cap boundary** - exactly at 100,000 characters the response must still be inline (cap is `>` not `>=`), and one character above must refuse. Pinned in Task 5 via the extracted guard seam.
5. **`remove_slot` on a slot that exists on BOTH sides with the same name** (an input and an output named identically) - the `slot-type` param must disambiguate; removing the input must not touch the output. Pinned in Task 4.

---

### Task 1: parameter docs join (`query_node` `include-docs`)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (`QueryNode` schema + param parsing)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (`queryNode` gains the flag; docs fields set from the access when requested)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ParameterIntrospectionOperationsTest.java` (new file)

**Interfaces:**
- Consumes: `JIPipeParameterAccess.getName()/getDescription()/isImportant()` (api/parameters/JIPipeParameterAccess.java:45-73); the existing `queryNode` shape (id, uuid, name, typeId, compartmentId, parameters via `renderParameterValue`, inputSlots, outputSlots).
- Produces: `public static JsonNode queryNode(AutomationContext ctx, String nodeIdStr, boolean includeDocs)` - the 2-arg form delegates with `false`. When `includeDocs`, every `parameters` entry additionally carries `name`, `description`, `important` (boolean) from the access; absent when false. Null/blank `name`/`description` render as `""` (empty-string convention); `important` is always present when docs are requested.

- [ ] **Step 1: Failing tests (new `ParameterIntrospectionOperationsTest`, house helper)**

- `queryNode_defaultShapeUnchanged`: without the param, entries carry exactly the A keys (value/type/raw?) - pin the absence of docs fields.
- `queryNode_includeDocsAddsMetadata`: a node with a known parameter -> entry has `name`, `description` (non-empty strings from the docs annotation), `important` present as a boolean.
- `queryNode_includeDocsEmptyStrings`: a parameter with no documentation annotation -> `name` equals the key-derived access name, `description` is `""` (never an omitted key).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest`
Expected: FAIL (unknown param rejected by `additionalProperties: false`; no docs fields).

- [ ] **Step 3: Implement**

- `QueryNode` schema gains `"include-docs": {"type": "boolean", "default": false, "description": ...}`; execute passes the flag into the new 3-arg API method; the 2-arg delegates.
- In the params loop, when `includeDocs`, set the three docs fields on the rendered entry object from the access.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest,QueryOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ParameterIntrospectionOperationsTest.java
git commit -m "feat(#1383): join parameter documentation into query_node on request"
```

### Task 2: changed-vs-default diff (`get_parameter_diff`)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (new `getParameterDiff`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java` (new `GetParameterDiff` operation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register `get_parameter_diff` in the query block)
- Test: `ParameterIntrospectionOperationsTest.java`

**Interfaces:**
- Consumes: `node.getInfo().newInstance()` (the reference instance; never mutate it), two `JIPipeParameterTree`s, `Objects.equals` on the rendered values, the two `jipipe:node:name`/`jipipe:node:description` exclusions (`JIPipeGraphNode.getTextDescription:1263` pattern), `renderParameterValue` for both sides.
- Produces: `public static JsonNode getParameterDiff(AutomationContext ctx, String nodeIdStr, boolean includeUnchanged)`. Response `{ "nodeId": ..., "parameters": [ { "key", "value", "type", "defaultValue", "changed" } ], "changedCount" }`. Keys iterate the CURRENT tree only; `defaultValue` is JSON null when the reference tree has no such key (Review Focus 3: a present-but-null optional renders `"null"`, absent renders null); comparison happens on the RENDERED strings (the report's `Objects.equals` on raw values is not reusable because the response is normalized text). `changedCount` counts `changed: true`; `parameters` is changed-only unless `includeUnchanged`. Unknown node -> the house `Node not found: X` error envelope.

- [ ] **Step 1: Failing tests (add to `ParameterIntrospectionOperationsTest`)**

- `getParameterDiff_unchangedNodeYieldsEmptyChangedList`: fresh node -> `changedCount` 0, `parameters` empty; with `include-unchanged: true` -> every key present with `changed: false`.
- `getParameterDiff_modifiedParameterIsChanged`: set a parameter via set_parameter -> exactly that key in the changed list, `value` the new rendering, `defaultValue` the old rendering, `changed` true.
- `getParameterDiff_optionalParameterNeverSetRendersNullString` (pin Review Focus 3): an optional parameter untouched since instantiation -> `include-unchanged` entry with `defaultValue` == `"null"` (string), not JSON null.
- `getParameterDiff_excludesNodeNameAndDescriptionKeys`: no `jipipe:node:*` keys in the response.
- `getParameterDiff_unknownNodeErrors`: `Node not found: nope` envelope.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest`
Expected: FAIL (no operation/API).

- [ ] **Step 3: Implement**

- `getParameterDiff` per the contract (reference instance is method-local; compare rendered `value` vs `defaultValue` strings for `changed`).
- `GetParameterDiff` operation (Query, read-only, no EDT), schema `{node-id` required, `include-unchanged` optional`, additionalProperties: false}`; description mentions the report machinery and that `defaultValue` mirrors a fresh node.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/QueryOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ParameterIntrospectionOperationsTest.java
git commit -m "feat(#1383): add get_parameter_diff against a fresh reference instance"
```

### Task 3: `set_parameter` read-back verification

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` (`SetParameter` response + description)
- Test: `ParameterIntrospectionOperationsTest.java`

**Interfaces:**
- Consumes: the existing `SetParameter` flow (convert -> `access.set` -> event) and `renderParameterValue`.
- Produces: the response becomes `{ "success": true, "parameter": { "value", "type", "raw"? } }` - the entry rendered from the SAME access after the set. Description gains: `The response carries the effective value after conversion - verify it matches your intent.` Failure paths (unknown node/parameter, conversion errors) unchanged.

- [ ] **Step 1: Failing tests (add to `ParameterIntrospectionOperationsTest`)**

- `setParameter_responseCarriesEffectiveValue`: set a string parameter -> response has `success: true` and `parameter.value` equal to the set string, `parameter.type` the class simple name.
- `setParameter_responseNormalizedLikeQueryNode`: set an enum parameter by name -> `parameter.value` is the normalized rendering (identical object shape as `query_node` returns for the same access); a `raw` key appears exactly when the renderings differ (same rule as query_node).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest`
Expected: FAIL (no `parameter` field).

- [ ] **Step 3: Implement**

- Render the access after the set via `renderParameterValue` and attach as `parameter`; description sentence; nothing else changes.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ParameterIntrospectionOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ParameterIntrospectionOperationsTest.java
git commit -m "feat(#1383): verify set_parameter with the effective normalized value"
```

### Task 4: slot editing (`add_slot`, `remove_slot`)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/SlotOperations.java` (`AddSlot`, `RemoveSlot`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register both)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/SlotOperationsTest.java` (new file)

**Interfaces:**
- Consumes: `node.getSlotConfiguration()` cast to `JIPipeMutableSlotConfiguration`; gates `allows*Slots()`, `is*SlotsSealed()`, `canModify*Slots()`, `canAdd*Slot()`, `canCreateCompatible*Slot(dataClass)`, `canRemove*Slot(name, true)`; `addSlot(name, new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Input/Output, name, description), true)`; `removeInputSlot/OutputSlot(name, true)`; `hasInputSlot/hasOutputSlot(name)`; `JIPipe.getDataTypes().getById(dataTypeId)` (null result = unknown id); `graph.getSlotEdges()` for the connection check (match by node identity + slot name + slot type); `graph.findNode(nodeId)` for node resolution.
- Produces:
  - `add_slot` (Modify, EDT-true): params `node-id` (required), `slot-type` (required enum `input|output`), `name` (required non-blank), `data-type` (required; unknown id refused listing known ids - the template-id refusal style), `description` (optional). Gate order, each refusal naming the condition: (1) `instanceof JIPipeMutableSlotConfiguration` else `node does not support slot editing`; (2) `allows*` and `!is*Sealed()` and `canModify*` (Review Focus 1: gate BEFORE addSlot - the configuration throws JIPipeValidationRuntimeException otherwise); (3) `canAdd*Slot()`; (4) `canCreateCompatible*Slot(dataClass)`; (5) name unused (`has*Slot`). Response `{ "added": true, "name": ..., "slotType": ... }`.
  - `remove_slot` (Modify, EDT-true): params `node-id`, `slot-type`, `name` (all required). Gates: (1) mutable; (2) the slot exists on that side - else an error listing the existing slot names of that side; (3) `canRemove*Slot(name, true)` (its failure message distinguishes not-user-modifiable vs sealed side). Then the CONNECTION CHECK (the user's ruling): collect every slot edge whose source OR target is this node's slot (match by node identity + slot name + slot side); if any, refuse `unable to remove slot '<name>': it has <n> connection(s): <source node name> (<source slot>) -> <target node name> (<target slot>); disconnect them first with remove_connection` (Review Focus 2: compartment-independent scan, node NAMES rendered). Nothing is disconnected implicitly. Response `{ "removed": true, "name": ..., "slotType": ... }`.
  - Both descriptions state the protocol: `add_connection`/`remove_connection` are the connect/disconnect commands; slot operations never disconnect anything.
  - Both operations carry `@Override public boolean requiresEdt() { return true; }` (the SetParameter house form - slot mutations notify UI listeners).

- [ ] **Step 1: Failing tests (new `SlotOperationsTest`)**

Test fixtures: the `jipipe:comment` node (register-if-absent; its annotation-built configuration is mutable by default - verified in the spec) for add/remove happy paths; a test-local fixture node class whose `getSlotConfiguration()` returns a non-mutable `JIPipeSlotConfiguration` stub (registered under a test-only id, register-if-absent) for the `does not support slot editing` refusal; two nodes + a programmatically built slot edge for the connection check.

- `addSlot_onMutableNodeAddsSlotAndListShowsIt`: add an output slot `extra` with data type id `jipipe:comment` (or another resolvable registered id - verify `getById` returns non-null for the chosen id at test-authoring time) -> response `added: true`; the node's configuration contains the slot afterwards.
- `addSlot_duplicateNameRefused`: add the same name twice -> the second call errors naming the name.
- `addSlot_unknownDataTypeRefusedListingKnown`: `data-type: "nope"` -> error contains `nope` and known data type ids.
- `addSlot_onNonMutableConfigurationRefused` (Review Focus 1 context): the fixture node -> `node does not support slot editing`.
- `addSlot_sealedConfigurationRefusedBeforeAnyMutation` (pin Review Focus 1): a mutable configuration with `outputSlotsSealed` set (achieve via the fixture: a second test-local node class wrapping a sealed `JIPipeDefaultMutableSlotConfiguration`, or set the flag through the builder if reachable) -> error naming the sealed side; the configuration is unchanged afterwards (no slot added).
- `removeSlot_existingWithoutConnectionsSucceeds`: add then remove -> removed true, slot gone.
- `removeSlot_unknownSlotListsExisting`: remove `nope` -> error listing that side's slot names.
- `removeSlot_connectedRefusesListingConnections` (pin Review Focus 2): two nodes, an edge built programmatically touching the slot -> refusal contains `unable to remove slot`, both endpoint node NAMES, the slot names, and `remove_connection`; the slot still exists and the edge still exists after the failed call.
- `removeSlot_disconnectThenRemoveSucceeds`: after `remove_connection` (the existing operation), `remove_slot` succeeds.
- `removeSlot_sameNameOnBothSidesDisambiguated` (pin Review Focus 5): an input and an output slot sharing one name -> removing the input leaves the output intact.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=SlotOperationsTest`
Expected: FAIL (no operations).

- [ ] **Step 3: Implement**

- `SlotOperations.java` per the contract; register both in AutomationPlugin (a slot-operations block in the file's grouping style). Never call `removeInputSlot/OutputSlot` before the connection check passes.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=SlotOperationsTest,ParameterIntrospectionOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/SlotOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/SlotOperationsTest.java
git commit -m "feat(#1383): add connection-safe slot editing operations"
```

### Task 5: project report (`get_project_report`)

**Files:**
- Create or Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (new `GetProjectReport`; implementer follows house grouping - a new group file is acceptable if InfoOperations is deemed too large)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register `get_project_report`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectReportOperationsTest.java` (new file)

**Interfaces:**
- Consumes: `project.getTextDescription(sb, 3)` (`api/project/JIPipeProject.java:1653`); `AutomationOperationUtils.resolveWriteTarget(project, "save-to", target)` (the screenshot ops' rooted machinery; UTF-8 write).
- Produces: `get_project_report` (Query, read-only, no EDT requirement): params `save-to` (optional). The inline cap is a package-visible static seam for testability: `static final int INLINE_CAP_CHARS = 100_000` and a package-private `static JsonNode renderReport(JIPipeProject project, String saveTo)` that measures, then either inlines (`{ "report": ..., "truncated": false }`), refuses (`error` naming the actual size and advising save-to; the cap is `>` - exactly 100,000 chars still inlines, Review Focus 4), or writes the full text UTF-8 and returns `{ "savedTo": ..., "sizeBytes": ... }` (never the inline text - the screenshot size contract). Description: heading level 3 fixed, the report mirrors the Project Report UI's text.

- [ ] **Step 1: Failing tests (new `ProjectReportOperationsTest`)**

- `getProjectReport_inlineUnderCap`: a small project -> `report` non-empty containing a compartment heading, `truncated` false.
- `getProjectReport_capBoundaryInline` (pin Review Focus 4): drive `renderReport` with a project whose text is exactly 100,000 chars (build a project large enough, or if impractical, test the guard through a package-private seam taking the measured text - the implementer extracts `guard(measuredLength)` or analogous so the boundary is pinned without a 100k-char project; state the chosen seam in the report) -> inline at exactly the cap, refusal at cap+1.
- `getProjectReport_saveToInsideRootWritesFile`: `save-to` inside the export root -> `{savedTo, sizeBytes}`, file exists with the full text, no inline `report` key.
- `getProjectReport_saveToOutsideRootRefused`: `save-to: "/tmp/../elsewhere/x.txt"` outside the roots -> error naming the roots.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectReportOperationsTest`
Expected: FAIL (no operation).

- [ ] **Step 3: Implement**

- `GetProjectReport` per the contract with the extracted guard seam; register in AutomationPlugin.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectReportOperationsTest,ParameterIntrospectionOperationsTest,SlotOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectReportOperationsTest.java
git commit -m "feat(#1383): add get_project_report with a rooted save-to and inline cap"
```
(Adjust the `git add` list if the operation lands in a new group file.)
