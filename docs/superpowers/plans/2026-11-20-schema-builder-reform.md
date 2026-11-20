# Schema Builder Reform (Charter H) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 59 hardcoded JSON schema text blocks across the automation operations with a general, reusable JSON Schema AST + builder, and expose typed parameter metadata; the MCP JSON schema becomes a projection of the typed spec.

**Architecture:** A general AST package (`org.hkijena.jipipe.utils.json.schema`) with named leaf schemas, a fluent object builder, and the moved validator. A thin automation consumer layer (`AutomationSchemaSpec` + `AutomationParameterSpec`) wraps the AST. `AutomationOperation` gains `getSchemaSpec()` as its single override point and defaults `getInputSchema()` / `getParameters()` from it. The registry applies schema defaults before dispatch. A pre-migration snapshot scaffold pins wire stability and is deleted at the end; a permanent bijection gate keeps the text blocks gone.

**Tech Stack:** Java 21, Maven, Jackson (`ObjectNode` / `ArrayNode`), JUnit 5, JetBrains MCP for the migration mechanics (search, build, analyze usages, inspections).

**Spec:** `docs/superpowers/specs/2026-11-20-schema-builder-reform-design.md`

## Global Constraints

- No em dashes (U+2014) or en dashes (U+2013) anywhere, including schema descriptions (descriptions are agent-facing user text).
- Stable string literals for ids; schema parameter names kebab-case when multi-word (`input-mode`, `entry-key`).
- No new dependency. `networknt` stays excluded (MCP SDK ships 3.x; the RO-Crate module pins 1.5.7).
- The general AST package must have zero imports from `org.hkijena.jipipe.api.automation` (it is reusable and automation-agnostic).
- Every schema property must keep its description verbatim during migration (the gate `everySchemaPropertyHasADescription` enforces presence; the snapshot enforces exact text).
- Wire stability: `getInputSchema()` JSON is structurally identical before and after each migration batch (`ObjectNode.equals`, key order irrelevant).
- Defaults apply to absent OPTIONAL parameters only; a required parameter's absence still fails validation.
- Commits: `type(#1383): subject`, explicit `git add`, no push unless asked.
- Registry-aware tests assert presence, never exact live-registry counts. The curated core set (`AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS`) is a single-source ship set and may be iterated exactly.
- Migration mechanics use JetBrains MCP where it fits: `search_regex` for the inventory and the zero-remaining check, `build_project` for per-file compile feedback (the July bulk-edit lesson: import placement drifts), `analyze_calls` before deleting helpers, and a project inspection at each batch end. Shell `mvn` runs remain the authority for test evidence.

## Review Focus

Failure modes most likely to bite a user, each pinned by a test in the task that owns the code:

1. **A migrated operation's wire schema silently changes** (a lost enum, a changed default, a description typo). Task 3's snapshot test compares every core operation's schema structurally to the pre-migration capture and fails on any difference.
2. **A default gets applied to a parameter whose ABSENCE is meaningful** (a caller that omits `project` means "use the dispatch window", not "use the literal default"). Task 4 pins that defaults apply only where the schema declares one, never to required or default-less parameters, and that `resolveInputMode`-style absence-vs-value semantics are unchanged.
3. **The registry mutates the caller's args node** (the broker caches or reuses request nodes). Task 4 pins that `applyDefaults` returns a new node and the input `params` is byte-identical afterward.
4. **The general AST leaks automation concepts**, coupling a would-be-general library to the operations layer. Task 1's package has no automation import and its own unit tests; Task 2's bijection between the AST and the consumer layer is proven by test.
5. **An operation that declares a parameter but never reads it** (migration typo swapping two descriptions or two names). The snapshot pins text per property name, but Task 6 (bijection gate) plus each batch's existing operation tests catch a round-trip that no longer produces the same args.
6. **The nested `set_project_metadata` schema drifts** (the only operation with a nested object containing an array of objects with their own `required`). Task 5 pins it explicitly with a dedicated schema-shape assertion in addition to the snapshot.

---

### Task 1: General JSON Schema AST

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonSchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonObjectSchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonArraySchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonStringSchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonIntegerSchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonNumberSchema.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonBooleanSchema.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaRenderingTest.java`

**Interfaces:**
- Produces: `sealed interface JsonSchema permits JsonObjectSchema, JsonArraySchema, JsonStringSchema, JsonIntegerSchema, JsonNumberSchema, JsonBooleanSchema` with `JsonNode toJson()`; static starters `JsonObjectSchema.object()`, `JsonArraySchema.array(JsonSchema items)`, `JsonStringSchema.string()`, `JsonIntegerSchema.integer()`, `JsonNumberSchema.number()`, `JsonBooleanSchema.bool()`; every leaf has `.description(String)`; `JsonStringSchema` has `.enumeration(String...)` and `.defaultsTo(String)`; `JsonIntegerSchema` has `.minimum(int)` and `.defaultsTo(int)`; the other leaves have `.defaultsTo(...)`; `JsonObjectSchema.Builder` has `.property(String name, JsonSchema schema)`, `.property(String name, JsonSchema schema, boolean required)`, and `.build()`; `JsonArraySchema` additionally exposes `.description(String)`.

- [ ] **Step 1: Write the failing rendering tests**

```java
class JsonSchemaRenderingTest {

    @Test
    void stringSchemaRendersTypeDescriptionEnumAndDefault() { /* string().description("d").enumeration("a","b").defaultsTo("a")
        -> {"type":"string","description":"d","enum":["a","b"],"default":"a"} */ }

    @Test
    void integerSchemaRendersMinimumAndDefault() { /* integer().description("d").minimum(1).defaultsTo(8)
        -> type integer, description, minimum 1, default 8 */ }

    @Test
    void objectSchemaRendersRequiredInDeclarationOrderAndAdditionalPropertiesFalse() {
        // object().property("b", string().description("B"), true).property("a", string().description("A"), true)
        // -> "required": ["b","a"] (declaration order, not sorted), additionalProperties false
    }

    @Test
    void objectSchemaOmitsRequiredWhenEmptyAndOmitsEmptyDescription() { /* object() with one optional property
        -> no "required" key, no "description" key */ }

    @Test
    void arraySchemaRendersItemsFromTheGivenType() { /* array(object().property("x", string().description("X")).build())
        -> {"type":"array","items":{...}} */ }

    @Test
    void nestedObjectsNestArbitrarily() { /* object -> array -> object -> array -> object renders all four levels */ }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest=JsonSchemaRenderingTest`
Expected: compile FAIL (no types).

- [ ] **Step 3: Implement the AST**

`JsonSchema` is a sealed interface with `JsonNode toJson()`. Each leaf is a final class with private final fields and a `toJson` that builds an `ObjectNode` in the keyword order `type, description, required, properties, additionalProperties, items, enum, default, minimum` (order is documentation only; the snapshot compares structurally). `JsonObjectSchema.object()` returns a `Builder` whose `build()` freezes two `LinkedHashMap`/`List` pairs (properties in declaration order, required names in declaration order, filtered from the properties' required flags - the `String, boolean` overload is sugar). Emit `required` only when non-empty; emit `description` only when non-null. `additionalProperties` always `false`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=JsonSchemaRenderingTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Verify the package is automation-agnostic**

Run: `grep -rn "import org.hkijena.jipipe.api.automation" jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/`
Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/ \
  jipipe-core/src/test/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaRenderingTest.java
git commit -m "feat(#1383): add the general JSON Schema AST and builder"
```

---

### Task 2: Validator on the AST, with `AutomationSchemaUtils` delegating

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaValidator.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java` (validate delegates; parseJson and emptyObjectSchema untouched until Task 7)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaValidatorTest.java`
- Test (existing, must stay green): `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtilsTest.java`

**Interfaces:**
- Produces: `JsonSchemaValidator.validate(JsonNode schemaJson, JsonNode args) : List<String>` - the same signature and message shape as today's `AutomationSchemaUtils.validate` (errors prefixed with the JSON path, `"compiled."` style child paths as today). `AutomationSchemaUtils.validate` delegates to it.
- Consumes: Task 1's AST is used for *emission*; the validator reads the JSON node (it must validate schemas that arrive as JSON from the broker or a test, not only AST-built ones).

- [ ] **Step 1: Read the existing validator and its tests**

Read `AutomationSchemaUtils.validate` (type, enum, required, properties, additionalProperties, and whatever else it handles) and `AutomationSchemaUtilsTest` (the behaviour to preserve verbatim). The new validator's contract is "the current behaviour, same messages", plus anything the current code already supports.

- [ ] **Step 2: Write the failing test that pins equivalence**

```java
class JsonSchemaValidatorTest {

    @Test
    void missingRequiredIsReportedAndValidArgsAreAccepted() { /* schema from the AST with one required string;
        empty args -> one error naming the property; args with it -> empty list */ }

    @Test
    void wrongTypeIsReported() { /* integer given "x" -> error mentioning the expected type */ }

    @Test
    void unknownPropertyIsRejectedWhenAdditionalPropertiesIsFalse() { /* ... */ }

    @Test
    void enumViolationIsReported() { /* ... */ }

    @Test
    void nestedObjectRequiredAndAdditionalAreEnforced() { /* object array items with their own required name */ }

    @Test
    void nullArgsAreTreatedAsEmptyObject() { /* validate(schema, null) -> missing-required errors only */ }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=JsonSchemaValidatorTest`
Expected: compile FAIL.

- [ ] **Step 4: Implement by moving the logic**

Move `validateNode` / `validateObject` / `matchesType` / `containsNode` / `describe` / `at` / `child` from `AutomationSchemaUtils` into `JsonSchemaValidator` unchanged. `AutomationSchemaUtils.validate` becomes `return JsonSchemaValidator.validate(schema, args);`. Do not change any message string: the existing tests and the registry's error text depend on them.

- [ ] **Step 5: Run the tests and the existing utils tests**

Run: `mvn -q test -pl jipipe-core -Dtest='JsonSchemaValidatorTest,AutomationSchemaUtilsTest'`
Expected: both PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaValidator.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/utils/json/schema/JsonSchemaValidatorTest.java
git commit -m "feat(#1383): move schema validation onto the general JSON Schema package"
```

---

### Task 3: Pre-migration snapshot scaffold

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaSnapshotTest.java`
- Create: `jipipe-core/src/test/resources/automation-schema-snapshot.json` (generated)

**Interfaces:**
- Consumes: `JIPipe.getAutomation().getRegistry().getAll()` (or the registry's equivalent enumerator; find the real accessor while writing) and `AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS` as the population.
- Produces: a structural pin of every core operation's `getInputSchema()`.

- [ ] **Step 1: Write the generator and the test together**

```java
class AutomationSchemaSnapshotTest {

    @Test
    void everyCoreOperationMatchesThePreMigrationSnapshot() { /* load the resource, compare ObjectNode.equals per id */ }

    @Test
    void snapshotAndRegistryHaveTheSameIdSet() { /* no missing, no extra ids among core operations */ }
}
```

Generate the resource once from the live registry on this commit (before any migration) with a small main or a `@Disabled` generator method that writes pretty-printed, key-sorted JSON. Commit the resource.

- [ ] **Step 2: Run to verify it fails before the resource exists**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationSchemaSnapshotTest`
Expected: FAIL (resource missing).

- [ ] **Step 3: Generate and commit the snapshot, then run it green**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationSchemaSnapshotTest`
Expected: PASS (2 tests). Sanity-check the resource holds all 76 core ids.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaSnapshotTest.java \
  jipipe-core/src/test/resources/automation-schema-snapshot.json
git commit -m "test(#1383): pin the pre-migration schema snapshot"
```

---

### Task 4: The automation spec layer, defaults in the registry, and the interface

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationParameterSpec.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaSpec.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java` (add `getSchemaSpec()`, make `getInputSchema()` and `getParameters()` defaults)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java` (add `applyDefaults`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistry.java` (apply defaults before `execute`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaSpecTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistryDefaultsTest.java`

**Interfaces:**
- Produces: `AutomationSchemaSpec` with static `object() : Builder`, `Builder.string(name, description)`, `.bool(...)`, `.integer(...)`, `.number(...)`, `.enumeration(name, description, String... values)`, `.arrayOfStrings(name, description)`, `.arrayOfObjects(name, description, JsonObjectSchema items)`, each returning a per-parameter builder with `.required()`, `.defaultsTo(...)`, and (integer) `.minimum(int)`; `.build()` returns the spec. Spec accessors: `toJsonSchema() : ObjectNode`, `getParameters() : List<AutomationParameterSpec>`. `AutomationParameterSpec` exposes `getName()`, `getType() : JsonSchema`, `isRequired()`, `getDefaultValue() : Object`.
- Produces on the interface: `default AutomationSchemaSpec getSchemaSpec()`, `default JsonNode getInputSchema()` (derives), `default List<AutomationParameterSpec> getParameters()` (derives).
- Produces: `AutomationSchemaUtils.applyDefaults(JsonNode schema, JsonNode args) : JsonNode` (new node; input untouched).

- [ ] **Step 1: Write the failing spec-layer test**

```java
class AutomationSchemaSpecTest {

    @Test
    void specRendersTheSameJsonAsTheAstDirectly() { /* build one spec, assert toJsonSchema equals the hand-built
        JsonObjectSchema.object()...toJson() */ }

    @Test
    void parametersExposeNameTypeRequiredAndDefault() { /* three params, one required, one with default */ }

    @Test
    void parameterListIsImmutableAndStable() { /* two calls return the same instance; add throws */ }
}
```

- [ ] **Step 2: Write the failing defaults test**

```java
class AutomationOperationRegistryDefaultsTest {

    @Test
    void absentOptionalGetsItsDefault() { /* schema with "default": 8; execute sees limit == 8 */ }

    @Test
    void callerArgsNodeIsNotMutated() { /* applyDefaults returns a new node; the input stays byte-identical */ }

    @Test
    void absentRequiredStillFailsValidation() { /* required without default -> IllegalArgumentException, never defaulted */ }

    @Test
    void presentValueWinsOverDefault() { /* explicit 3 stays 3 */ }

    @Test
    void parameterWithoutDefaultIsNotInjected() { /* absent optional without default stays absent (presence semantics preserved) */ }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaSpecTest,AutomationOperationRegistryDefaultsTest'`
Expected: compile FAIL.

- [ ] **Step 4: Implement the spec layer**

`AutomationSchemaSpec.Builder` accumulates `(String name, JsonSchema schema, boolean required, Object defaultValue)` and on `build()` constructs a `JsonObjectSchema` from the same data, so the AST and the parameter view cannot diverge. `AutomationParameterSpec` is a record-like final class mirroring one top-level property.

- [ ] **Step 5: Implement `applyDefaults` and the registry call**

`applyDefaults(schema, args)`: deep-copy `args` (or `{}` when null), then for each property of `schema.properties` that carries a `default` and is absent from the copy, `set` that default. Recursion into nested object/array items is NOT performed (the current schemas only use defaults at the top level; a nested default is a spec extension, ledgered if it ever appears). `AutomationOperationRegistry.execute` calls it after `validate` and passes the result to `operation.execute`.

- [ ] **Step 6: Wire the interface defaults**

`getSchemaSpec()` default returns `AutomationSchemaSpec.object().build()` (empty schema). `getInputSchema()` default returns `getSchemaSpec().toJsonSchema()`. `getParameters()` default returns `getSchemaSpec().getParameters()`. Existing overrides still win, so un-migrated operations are unaffected.

- [ ] **Step 7: Run the tests and the registry/schema gates**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaSpecTest,AutomationOperationRegistryDefaultsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest,AutomationSchemaSnapshotTest'`
Expected: all PASS. The snapshot must be unchanged: no operation has migrated yet.

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationParameterSpec.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaSpec.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistry.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaSpecTest.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistryDefaultsTest.java
git commit -m "feat(#1383): add the automation schema spec layer and registry defaults"
```

---

### Task 5: Migrate batch 1 (18 files, 59 blocks; batch 1 = the small files)

**File inventory (verified from the tree; the count after each file name is its `parseJson("""` block count):**

| Batch | Files (blocks) | Subtotal |
|---|---|---|
| 1 (this task) | `ArrangeOperations` (1), `CacheOperations` (1), `ROCrateOperations` (1), `DiagnosticsOperations` (2), `SlotOperations` (2), `UserPathOperations` (2), `KnowledgeOperations` (2) | 11 |
| 2 (Task 5b) | `UiInteractionOperations` (1), `ViewerOperations` (3), `PipelineMapOperations` (3), `InfoOperations` (3) | 10 |
| 3 (Task 5c) | `DataOperations` (4), `ProjectOperations` (4), `UiStateOperations` (4) | 12 |
| 4 (Task 5d) | `ExecutionOperations` (5), `ScreenshotOperations` (5) | 10 |
| 5 (Task 5e) | `QueryOperations` (6), `ModificationOperations` (10, the nested author/affiliation case) | 16 |
| | **total** | **59 across 18 files** |

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperations.java`, `CacheOperations.java`, `ROCrateOperations.java`, `DiagnosticsOperations.java`, `SlotOperations.java`, `UserPathOperations.java`, `KnowledgeOperations.java`
- Test: `AutomationSchemaSnapshotTest` plus each file's existing operation tests

Note the naming trap: the arrange file is `ArrangeOperations.java` (not `ArrangementOperations.java`); the plan's earlier draft said the wrong name. Confirm every file name against `ls` before editing.

**Interfaces:**
- Consumes: `AutomationSchemaSpec.object()...build()` (Task 4).
- Produces: no API change; each operation's `getSchemaSpec()` override replaces its `getInputSchema()` override.

- [ ] **Step 1: Inventory with JetBrains `search_regex`**

Search the operations package for `parseJson\("""` and list the 8 batch-1 files with their block counts. Record the list in the report.

- [ ] **Step 2: Migrate one file (ArrangeOperations), then verify**

Replace its single `getInputSchema()` with `getSchemaSpec()`. For a parameterless operation, delete the override entirely. Run `mvn -q test -pl jipipe-core -Dtest='ArrangeOperationsTest,AutomationSchemaSnapshotTest'`; expected PASS with no snapshot change. Then `build_project` via JetBrains for import placement.

- [ ] **Step 3: Migrate the remaining 6 files of batch 1 the same way**

Diagnostics, Slot, UserPath, Knowledge, Cache, ROCrate. For each: migrate, `build_project`, then the snapshot test. Same property names, same descriptions character-for-character, same required set and order, same `default`/`enum`/`minimum`/`items`.

- [ ] **Step 4: Run the batch gate**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaSnapshotTest,AutomationOperationsSchemaTest,AutomationRegistryValidationTest'` plus the batch's operation test classes.
Expected: all PASS, snapshot byte-identical.

- [ ] **Step 5: Batch inspection**

Run the JetBrains project inspection; triage anything it reports in the touched files (unused imports, redundant casts).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/
git commit -m "refactor(#1383): migrate batch 1 of the operation schemas to the spec layer"
```

---

### Task 6: Migrate batches 2 and 3 (shared-helper files and four-block files)

**Files (22 blocks across 7 files):**
- Modify: `UiInteractionOperations.java` (1 block + the `schemaWithProject` / `schemaWithInputModeAndProject` helpers, which all of its operations route through), `ViewerOperations.java` (3), `PipelineMapOperations.java` (3), `InfoOperations.java` (3), `DataOperations.java` (4), `ProjectOperations.java` (4), `UiStateOperations.java` (4)
- Test: `AutomationSchemaSnapshotTest` plus each file's operation tests

**Interfaces:**
- Consumes: Tasks 1-5. The shared `PROJECT_PARAM_DESCRIPTION` constant is reused as a builder description argument; the shared helpers become `AutomationSchemaSpec.Builder` composition.

- [ ] **Step 1: Migrate `UiInteractionOperations` and its helpers first**

`schemaWithProject(base, extra)` keeps its signature but takes and returns the spec builder path: compose the base operation's spec builder with the shared `project` property (and, for `schemaWithInputModeAndProject`, the shared `input-mode` property with its `"dispatch"` default). Every one of the three distinct `project` descriptions stays character-identical. Run `mvn -q test -pl jipipe-core -Dtest='UiInteractionOperationsTest,AutomationSchemaSnapshotTest'`; expected PASS, snapshot unchanged.

- [ ] **Step 2: Migrate the six four-or-three-block files**

Viewer, PipelineMap, Info, then Data, Project, UiState. Per file: migrate, `build_project`, snapshot test.

- [ ] **Step 3: Run the batch gate**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaSnapshotTest,AutomationOperationsSchemaTest,AutomationRegistryValidationTest'` plus the batch's operation test classes.
Expected: all PASS, snapshot byte-identical.

- [ ] **Step 4: Batch inspection**

Run the JetBrains project inspection; triage anything in the touched files.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/
git commit -m "refactor(#1383): migrate the shared-helper and four-block operation schemas"
```

---

### Task 7: Migrate batches 4 and 5 (the large files and the nested schema)

**Files (26 blocks across 3 files):**
- Modify: `ExecutionOperations.java` (5), `ScreenshotOperations.java` (5), `QueryOperations.java` (6), `ModificationOperations.java` (10, including `set_project_metadata`'s nested author/affiliation schema)
- Test: `AutomationSchemaSnapshotTest`, `ModificationOperationsTest` (new nested-shape assertion), the other operation tests

**Interfaces:**
- Consumes: Tasks 1-6. The shared nested schema becomes `private static final JsonObjectSchema AUTHOR_ENTRY = JsonObjectSchema.object()...build();`, referenced by both array parameters (replacing the `.formatted()` string interpolation).

- [ ] **Step 1: Migrate `set_project_metadata`'s nested schema first**

Build `AUTHOR_ENTRY` as a `JsonObjectSchema` constant (firstName/lastName required, `affiliations` as an array of objects with `name` required). Both `authors` and `acknowledgements` use `.arrayOfObjects(name, description, AUTHOR_ENTRY)`. Run `mvn -q test -pl jipipe-core -Dtest='ModificationOperationsTest,AutomationSchemaSnapshotTest'`; expected PASS, snapshot identical.

- [ ] **Step 2: Add the dedicated nested-shape assertion**

In `ModificationOperationsTest`, one test asserting `set_project_metadata`'s schema carries the full chain: `authors.items.required == [firstName, lastName]`, `authors.items.properties.affiliations.items.required == [name]`, and that innermost object has `additionalProperties: false`. This pins the deepest nesting independently of the snapshot.

- [ ] **Step 3: Migrate the remaining blocks in `ModificationOperations`, then Execution, Screenshot, Query**

Per file: migrate, `build_project`, snapshot test.

- [ ] **Step 4: Run the batch gate**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaSnapshotTest,AutomationOperationsSchemaTest,AutomationRegistryValidationTest'` plus the batch's operation tests.
Expected: all PASS, snapshot byte-identical.

- [ ] **Step 5: Commit**

```bash
git commit -m "refactor(#1383): migrate the large operation schemas, including the nested author schema"
```

---

### Task 8: Cleanup, delete the snapshot, install the bijection gate

**Files:**
- Delete: `jipipe-core/src/test/resources/automation-schema-snapshot.json`, `AutomationSchemaSnapshotTest.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaBijectionTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java` (retire `parseJson` if it has no remaining callers; `emptyObjectSchema` per its call graph)
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationsSchemaTest.java` (widen `EXPECTED_CORE_OPERATION_IDS` to package-private)

**Interfaces:**
- Produces: the permanent gate - for every id in `EXPECTED_CORE_OPERATION_IDS`, `operation.getInputSchema()` equals `operation.getSchemaSpec().toJsonSchema()`.

- [ ] **Step 1: Zero-remaining check with JetBrains `search_regex`**

Search the operations package (main sources) for `parseJson\("""` and `getInputSchema()`. Expected: zero `parseJson("""` hits and zero `getInputSchema` overrides.

- [ ] **Step 2: Delete the snapshot scaffold**

Remove the test and the resource. Rationale in the commit message: keeping a full-schema snapshot would make every future schema edit a two-place edit.

- [ ] **Step 3: Write the bijection gate**

```java
class AutomationSchemaBijectionTest {
    @Test
    void everyCoreOperationSchemaIsTheDerivationOfItsSpec() {
        // for each id in AutomationOperationsSchemaTest's curated set:
        // assertEquals(op.getSchemaSpec().toJsonSchema(), op.getInputSchema());
    }
}
```

Make the curated set reachable (widen `EXPECTED_CORE_OPERATION_IDS`' visibility to package-private, or duplicate the id list in a shared test helper; prefer widening - one source of truth).

- [ ] **Step 4: Retire the now-unused helpers**

`analyze_calls` on `parseJson` and `emptyObjectSchema` first. Delete each whose callers are all gone; keep `validate` (delegates to `JsonSchemaValidator`).

- [ ] **Step 5: Run the full gates and the whole automation package tests**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaBijectionTest,AutomationOperationsSchemaTest,AutomationRegistryValidationTest,org.hkijena.jipipe.api.automation.**'`
Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git commit -m "test(#1383): install the schema bijection gate and retire the snapshot scaffold"
```

---

### Task 9: Whole-branch verification

- [ ] **Step 1:** `mvn -q clean test -pl jipipe-core` and record the counts (the baseline before this branch is 1639/0/0/6 once !211 merges; the new tests raise it).
- [ ] **Step 2:** `mvn -q test -pl jipipe-launcher -am -Dsurefire.failIfNoSpecifiedTests=false`; record counts.
- [ ] **Step 3:** By-eye: start the app, open the MCP monitor's operation list, and inspect `list_operations` output for one migrated operation; compare its schema to a pre-migration capture if any UI shows it. Report what you saw.
- [ ] **Step 4:** Whole-branch review, MR, work-item sync.