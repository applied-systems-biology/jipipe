# Schema Builder Reform (Charter H) - Design Spec

Date: 2026-11-20
Status: Draft (approach option 4 approved by user; pending document review)
Charter: #1383 charter addition H, ruled 2026-11-20 during sub-project C.
Tracking child work item: #1395.

## 1. Intent and why

Every one of the ~76 automation operations declares its input schema as a hand-written JSON text
block, parsed at call time via `AutomationSchemaUtils.parseJson("""...""")`. The user's ruling that
created this sub-project (during C): the broker must not have precognition, and the wider pattern is
"hardcoded JSON string schemas instead of intermediate builders carrying higher information content -
the same sloppy design the llm branch had to rectify."

The reform replaces every text block with a typed, intermediate schema model that is the single
source of truth, from which both projections derive:

1. **The MCP JSON schema** (the wire contract: what the registry validates against and the broker
   forwards verbatim to clients).
2. **Machine-readable parameter metadata** (a typed, enumerable `List<AutomationParameterSpec>` per
   operation - name, type, required, default, description, enum values, bounds) that future consumers
   read without parsing JSON.

The reuse story (user-approved option 4): the new JIPipe agent harness (phase 4 handoff) can express
its function parameters with the same spec classes and derive both the MCP JSON and its own tool
definitions from one source. The llm branch's `LLMFunctionDescriptor` (typed `ParamInfo` list, JSON
derived, cached) is the prior art; this reform gives the automation layer the same shape. The llm
branch dies; its types are not imported.

## 2. Goals, non-goals, anti-goals

### Goals

1. One typed source of truth per operation: a builder-expressed `AutomationSchemaSpec` (backed by
   the GENERAL JSON Schema AST) from which `getInputSchema()` derives. No hand-written JSON text
   blocks remain in the operations package.
2. A general, reusable JSON Schema AST + builder in `org.hkijena.jipipe.utils.json.schema` that
   knows nothing about automation - usable by the KDB, RO-Crate, and the phase-4 harness.
3. `AutomationOperation.getParameters()` exposes the typed parameter list (new default method,
   empty for non-migrating implementations).
4. The registry applies schema `default` values for absent optional parameters before dispatch -
   the `default` keyword becomes behavior, not documentation.
5. Wire stability: every operation's emitted JSON is structurally identical before and after the
   migration, pinned by a generated snapshot during the migration and by code review after.
6. JetBrains MCP drives the mechanics: search_text/search_regex for the inventory and the
   zero-remaining check, analyze_calls before cleanup deletions, build_project for fast per-batch
   compile feedback, run_inspection_kts_project as the post-migration regression sweep.

### Non-goals

- Rewriting `execute()` bodies to read parameters through the spec model. The bodies keep their
  current `params.has("x")` style; the registry's new default application makes several of those
  fallbacks redundant but harmless. A future cleanup may simplify them; not this project.
- An OpenAI-tool-definition converter or any harness-side adapter code. The AST and the spec classes
  are public and documented as the intended source; the adapter is phase-4 work.
- Migrating other subsystems (RO-Crate metadata, KDB slots) onto the new AST. They can adopt it later;
  this project builds the general library and brings the automation layer onto it.
- New schema vocabulary (patterns, nested objects, anyOf). The builder covers exactly today's
  vocabulary; extend it when an operation needs more.
- Changing any operation's behavior beyond the default application (same ids, same responses, same
  validation errors for genuinely invalid input).

### Anti-goals (failure even if it technically works)

- Two sources of truth: a spec list AND a hand-maintained JSON that can drift apart.
- A migration that silently changes a wire schema (wrong default, lost enum, reordered required
  array) - the snapshot scaffold exists to make that impossible to miss.
- Defaults applied to REQUIRED parameters (defaults are for absent OPTIONAL parameters only).

## 3. Constraints

- Java 21, Maven, Jackson (already present). No new dependency (networknt stays excluded: the MCP
  SDK ships 3.x, the RO-Crate module pins 1.5.7).
- House rules: no em/en dashes; stable string literals for ids; schema parameters kebab-case where
  multi-word (`entry-key`); registry-aware tests assert presence not exact counts - EXCEPT the
  snapshot scaffold, which pins the full schema set (a single-source ship set, the allowed case).
- The registry-wide gates (`AutomationRegistryValidationTest`,
  `AutomationOperationsSchemaTest.everySchemaPropertyHasADescription`) must stay green throughout.
- KDB MR !211 is auto-merging: this project branches from post-!211 master, and
  `KnowledgeOperations` (3 schemas) is part of the migration inventory.
- The July FQN-refactor lesson (deja ses_071663d4): bulk mechanical Java edits drift on import
  placement - the per-batch inspection sweep catches it.

## 4. Approach (approved)

Option 4 of four considered:

| | A. Thin JSON builder | B. Typed spec model | C. B + registry defaults | D. C + reuse contract (picked) |
|---|---|---|---|---|
| Source of truth | builder call, JSON-shaped | typed spec list | typed spec list | typed spec list, documented as the harness-facing contract |
| Wire change | none | none | defaults applied | defaults applied |
| Metadata consumer | none (parse JSON back) | any (typed list) | any | any, incl. phase-4 harness |
| Verdict | carries no more information than the JSON (fails the ruling's "higher information content") | the ruling's shape, but `default` stays documentation | the ruling plus a real runtime change | the ruling plus the reuse story the charter names |

## 5. The design

### 5.1 The general JSON schema library (new package, `org.hkijena.jipipe.utils.json.schema`)

**User ruling 2026-11-20:** the model is close to JSON Schema by nature, so it is built as a GENERAL
JSON Schema AST + builder, reusable anywhere JIPipe needs to describe or validate JSON. The automation
layer becomes one CONSUMER. No existing library fits (checked: networknt ships 3.x transitively with
the MCP SDK and is excluded because of the RO-Crate 1.5.7 pin; adopting it is a classpath-wide
decision that belongs to a separate discussion, and libraries give parsers, not ergonomic builders).

The package is a neutral AST, named after JSON Schema's own vocabulary - nothing automation-specific:

```java
/** A JSON Schema node. Sealed hierarchy; every node renders itself to a JSON object. */
public sealed interface JsonSchema permits JsonObjectSchema, JsonArraySchema,
        JsonStringSchema, JsonIntegerSchema, JsonNumberSchema, JsonBooleanSchema {
    JsonNode toJson();
}

public final class JsonObjectSchema implements JsonSchema {
    // description, list of (propertyName, JsonSchema) in declaration order,
    // required names in declaration order, additionalProperties allowed flag (default false)
    public static Builder object();
}

public final class JsonArraySchema implements JsonSchema {
    // items: JsonSchema, optional; description
}

public final class JsonStringSchema implements JsonSchema {
    // description, optional enumValues (List<String>), optional defaultValue
}

public final class JsonIntegerSchema implements JsonSchema {
    // description, optional minimum, optional defaultValue
}

// JsonNumberSchema, JsonBooleanSchema analogous.
```

**Named leaves rather than a type enum**: a `JsonSchemaString` that cannot be given a `minimum` makes
an invalid combination unrepresentable, the same way the KDB's typed slots do. Each leaf carries
exactly the keywords JSON Schema defines for it (string: enum, default; integer: minimum, default;
boolean: default).

The builder:

```java
JsonObjectSchema.object()
    .string("compartment", "Compartment whose nodes are arranged").required()
    .bool("apply", "true commits the computed positions").defaultsTo(false)
    .integer("limit", "Maximum results").defaultsTo(8).minimum(1)
    .enumeration("kind", "The note kind", "text-box", "comment").required()
    .arrayOfStrings("ids", "Nodes to move").required()
    .arrayOfObjects("authors", "Project authors", JsonObjectSchema.object()
            .string("firstName", "Author first name (required, non-blank)").required()
            .string("lastName", "Author last name (required, non-blank)").required()
            .string("orcid", "ORCID (URL or ID)")
            .bool("firstAuthor", "Marks the author as first author")
            .description("One author entry: firstName and lastName are required (non-blank), all other fields are optional")
            .build())
    .build();
```

**Nested object schemas are first-class** (builder-check item 1): `set_project_metadata` nests author
entries containing an `affiliations` array whose items are objects with their own `required` and
`additionalProperties: false`; the AST nests arbitrarily because array items and object properties are
`JsonSchema` values.

**Validation** (also general): `JsonSchemaValidator` moves/extends the existing hand-rolled logic in
`AutomationSchemaUtils.validate` into the package, working from the AST directly (type, enum,
required, properties, additionalProperties, minimum) and producing `List<String>` errors with JSON
paths. `AutomationSchemaUtils` keeps its signatures as thin delegates (existing callers and the
registry gate unchanged). The validator is not the reform's product but it belongs with the AST; a
future move to a library replaces one class.

**What the general package must NOT know:** automation, operations, MCP, parameters-as-args. It is
`toJson()` and `validate()` over a value model. This is what makes it reusable by the KDB, RO-Crate,
and the phase-4 harness.

### 5.1.1 The automation consumer layer (`org.hkijena.jipipe.api.automation`)

Thin, automation-specific, built ON the AST:

```java
/** One parameter of an automation operation: the machine-readable metadata. */
public final class AutomationParameterSpec {
    private final String name;              // stable, kebab-case when multi-word
    private final JsonSchema type;          // the general AST node (its description is the parameter description)
    private final boolean required;
    private final Object defaultValue;      // convenience mirror of the AST default; null = none
}

/** The complete input schema of an operation: the typed source of truth. */
public final class AutomationSchemaSpec {
    private final JsonObjectSchema schema;               // the AST
    private final List<AutomationParameterSpec> parameters;  // flattened top-level metadata
    public JsonNode toJsonSchema();                      // schema.toJson()
    public List<AutomationParameterSpec> getParameters();
}
```

`AutomationParameterSpec` exists because a harness consumer wants name/required/default as fields
without walking the AST - it is a typed view over one top-level property, not a second source of
truth (both are built from the same builder call; `build()` derives the views). Nested types are
reachable through `type` (`JsonSchema`), so an object's own properties are enumerable from the AST
when a consumer needs them.

Rendering rules (matching today's JSON exactly, verified by the snapshot):

- Object: `{"type": "object", "description": ..., "required": [...], "properties": {...}, "additionalProperties": false}`;
  `description` present only when set; `required` omitted when empty (matching the current blocks);
  `additionalProperties` always emitted as `false` (every block today declares it).
- Property/array nodes: `{"type": ..., "description": ...}` plus `enum` / `default` / `minimum` /
  `items` when present, and nested `properties`/`required` for objects. Key order follows the keyword
  order above; the snapshot compares structurally so order is documentation only.
- `toJson()` builds a fresh `ObjectNode` per call (no caching; the registry validates and the broker
  forwards per registration - both cheap).

### 5.2 The interface changes (`AutomationOperation`)

- New method `default AutomationSchemaSpec getSchemaSpec() { return AutomationSchemaSpec.object().build(); }`
  - the typed source of truth. Migrated operations override THIS only, returning their
  `static final AutomationSchemaSpec SPEC`.
- `getInputSchema()` becomes a **default method**: `return getSchemaSpec().toJsonSchema();`.
  Existing overrides keep working unchanged (they win over the default), so the migration is
  incremental with zero breakage.
- New `default List<AutomationParameterSpec> getParameters() { return getSchemaSpec().getParameters(); }`
  - the typed metadata view, free for every operation.
- Migrated operations delete their `getInputSchema()` override. End state: no operation in the
  operations package overrides `getInputSchema()`.
- The reuse contract (for phase 4, documented on the interface): a consumer derives tool definitions
  from `getParameters()` or the AST; `getInputSchema()` is always the derivation of `getSchemaSpec()`.
  A harness adapter needs nothing but these methods, and the general AST is usable outside automation.
- The reuse contract (for phase 4, documented on the interface): a consumer derives tool definitions
  from `getParameters()`; `getInputSchema()` is always its derivation. A future harness adapter needs
  nothing but these two methods.

### 5.3.1 Shared parameters and the `project` property (builder-check item 2)

Two sharing mechanisms, both redocumented rather than duplicated:

- **Shared description constants stay constants.** `UiInteractionOperations.PROJECT_PARAM_DESCRIPTION`
  and its `input-mode` sibling remain single String constants; each operation passes the same
  constant as the description argument (`.string("project", PROJECT_PARAM_DESCRIPTION)`). No
  builder-level "reusable parameter" feature: one constant is already the single source, and a
  parameter instance shared across specs would couple unrelated operations' lifecycles.
- **The broker injection is untouched** (wire stability): `MCPBrokerServer.withProjectArgument`
  keeps injecting a `project` property for `PROJECT_BOUND` tools exactly as today, and operations
  keep declaring their own `project` where they already do. Reconciling the two is out of scope; the
  spec records it as known duplication (the injected argument's description versus the declared
  one) so a future pass can unify them once and for all.

Today's three distinct `project` descriptions and the `schemaWithProject`/`schemaWithInputModeAndProject`
helpers move to builder composition: the helper methods become `AutomationSchemaSpec` arguments
(a base spec plus shared properties appended), preserving every current description verbatim.

### 5.3.2 Defaults and routing-like parameters (builder-check item 3)

The registry applies every schema default, including `ui_*` operations' `"input-mode": "dispatch"`.
Verified behaviorally neutral: `resolveInputMode` treats absence and `"dispatch"` identically
(`UiInteractionOperations.java:260`), and no operation's `project` property currently declares a
default, so the injected/envelope parameter is unaffected. The `get_ui_state` chain branches on
`has("project") && !isBlank()` - unaffected by default application for the same reason (no default).
Rule for the future: any parameter whose ABSENCE is semantically distinct from its default value must
not declare a schema default (declaring one is a spec violation the review catches).

### 5.4 The registry applies defaults (the one runtime change)

In `AutomationOperationRegistry.execute`, after validation succeeds:

1. For each property in the schema with a `default` keyword that is ABSENT from `params`, insert the
   default (a new `AutomationSchemaUtils.applyDefaults(schema, params)` returning the effective
   args node; the original `params` is never mutated).
2. Dispatch receives the effective args.

Rules: defaults apply to absent properties only (a present-but-invalid value still fails
validation first); required parameters are unaffected (absence still fails validation); the
effective args are what reaches `execute()` and the operation log. Reads of the JSON `default`
keyword keep the dispatch path spec-free - the registry works from the schema JSON, so the change
is one new helper plus one call site.

Known consequence, accepted: operations whose `execute()` re-applies the same fallback in code
(e.g. `params.has("limit") ? params.get("limit").asInt() : 8`) behave identically (they read the
injected value); their in-code fallback becomes redundant-but-harmless. An operation that ECHOES
the received args back to the caller would now emit keys the caller did not send; no operation in
this package echoes args (verified by builder-check item 4), so nothing changes on the wire, and a
future echo must read the caller's original args rather than the effective ones (the registry keeps
both; `execute()` receives the effective node only).

### 5.5 `getParameters()` instance contract (builder-check item 5)

Migrated operations hold a `private static final AutomationSchemaSpec SPEC = ...` field (built once
at class load) and `getParameters()` returns `SPEC.getParameters()` - the same immutable list
instance on every call. Documented on the interface as the contract for phase-4 consumers (a harness
may cache it). `AutomationSchemaSpec` freezes both its parameter list and every nested type spec at
`build()` (defensive copies, unmodifiable views).

### 5.6 The snapshot scaffold (migration safety net)

Before any migration, a generated test resource pins every registered operation's current schema:
op id -> schema JSON (produced by a small generator run against the live registry on the
pre-migration tree, committed as `AutomationSchemaSnapshotTest`'s resource). The test asserts
structural equality (Jackson `ObjectNode.equals` is key-order-independent) for every operation.
Each migration batch must keep it green; it is deleted in the final task (keeping it forever would
make every future schema edit a two-place edit - the friction the reform exists to remove; the
lasting protections are the structural gates plus the bijection gate below).

### 5.7 The bijection gate (migration completeness, permanent)

**Population (builder-check item 6):** both gates iterate the CURATED CORE operation-id set
(`AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS`, already maintained in the repo), never
the whole live registry - a third-party plugin operation is free to override `getInputSchema()`
with its own shape, and its drift must not fail the core build (house rule: plugin-aware registry
tests). The snapshot resource likewise lists only core ids.

For every core operation, `operation.getInputSchema()` must equal
`AutomationSchemas.toJsonSchema(operation.getParameters())`. An operation that still overrides
`getInputSchema()` with a hand-built schema while its parameter list is empty (or drifted) fails.
This is the enforcement that the text blocks are gone and stay gone. (Future escape hatch, if a core
operation ever needs vocabulary the spec cannot express: override `getInputSchema()` AND return
matching parameters - the gate then still passes, and the spec extension becomes the follow-up.)

**Parameterless operations (builder-check item 7):** the end state is (a) - delete the override and
let the interface default emit the empty object schema. `AutomationSchemaUtils.validate` treats
`null` args as `{}` and validates an empty schema identically, so nothing changes. `emptyObjectSchema()`
itself is kept only if it has non-schema callers (the builder decides from `analyze_calls`).

**Test stubs (builder-check item 8):** the ~15 stub operations in test files register into LOCAL
registries and are left exactly as-is. They are outside both gates' population by construction.

### 5.8 The migration (mechanics, JetBrains MCP)

Inventory first (pre-flight, not a task): `search_regex` for `parseJson\("""` across the operations
package - the exact file:line list goes in the ledger and drives the task split. Then per batch:

1. Rewrite each block as a builder chain (parameter order = the JSON's `required` order first, then
   optionals in their JSON order; the snapshot enforces faithfulness).
2. Delete the operation's `getInputSchema()` override (the interface default serves).
3. `build_project` after each file (fast feedback; catches import placement - the July lesson).
4. Run the gates + snapshot for the batch.
5. `run_inspection_kts_project` (or the compile inspection) at batch end: catches unused imports,
   unreachable code, and anything the bulk edit left dangling.

Batches by file grouping (~6 files each, 3 batches), then the cleanup task:
`analyze_calls` on `parseJson` and `emptyObjectSchema` before deleting anything (parseJson may have
non-schema callers elsewhere); `emptyObjectSchema()` either re-expresses as
`AutomationSchemaSpec.object().build()` at its call sites or stays as a one-line delegate.

### 5.9 What transfers from the KDB project's playbook

- The builder's narrowing rule (typed values, clean errors) mirrors `KdbValueType.narrow`.
- Findings-vs-exceptions: validation errors stay validation errors; the builder fails fast at
  construction for programmer errors (duplicate parameter names, enum on a non-string type).
- The bijection gate is the same shape as the KDB corpus integrity gate: structural truth asserted
  registry-wide, fail-closed.

## 6. Left to the builder

- Exact builder method names and chaining ergonomics (`bool` vs `boolean` - `boolean` is a Java
  keyword; the design sketch uses `bool`).
- The snapshot generator's exact resource format (JSON object keyed by op id; pretty-printed,
  sorted keys for stable diffs).
- The general AST's exact package layout and builder method names (`bool` vs `boolean` - `boolean`
  is a Java keyword; the design sketch uses `bool`), and whether `JsonSchemaValidator` is a static
  facade or an instance.
- How far to extend the validator while moving it (the existing behaviour is the floor; adding
  `maximum`, `minItems`, `maxItems` is optional and must not change any existing validation result).
- The batch split of the 18 files (the ledger's inventory drives it; keep file groups coherent).
- `emptyObjectSchema()`'s fate per its call graph.
