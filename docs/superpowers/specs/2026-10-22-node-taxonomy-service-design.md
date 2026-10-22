# Node Taxonomy as a First-Class Service Component — Design

Date: 2026-10-22
Issue: [RGerst/jipipe #1324](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1324)
Branch: `1324-node-taxonomy-core` (6.0.0)

## Problem

The node taxonomy (functions + frameworks) is registered through an ad-hoc
path that violates JIPipe's own service-component conventions, and its
auto-assignment layer is a purpose-built, closed utility:

1. **No proper home.** `FunctionRegistry` and `FrameworkRegistry` live inside
   `TaxonomyResolver`, which is owned by `JIPipeNodesServiceComponent`.
   `TaxonomyPlugin` populates them via `JIPipe.getNodes().getFunctions()`.
   There is no `JIPipe.getNodeTaxonomyService()` — the taxonomy is not a
   service of its own, so "registering taxonomy" is a special case instead of
   the established pattern (as nodes and datatypes are registered through
   their service components).
2. **Closed ID→function resolution.** `TaxonomyAutoAssignment.functionForId`
   is a hardcoded `switch` over ~20 function IDs. Every new function — e.g.
   the `MiscellaneousUnassignedFunction` added 2026-10-22 for auto-imported
   noise nodes — must be added to *both* the registration array in
   `TaxonomyPlugin` *and* this switch. Plugins cannot register their own
   taxonomy at all.
3. **Hardcoded name inference.** `TaxonomyAutoAssignment.functionForName` is
   an if-chain of keywords with first-match-wins priority. The knowledge lives
   in the utility class, not on the functions it classifies into, so a plugin
   function can never be inferred into, and the priority rules are not
   inspectable per function.
4. **Duplicated ID literals.** Only 31 of 90 function classes (and 1 of 17
   framework classes) carry a `public static final String ID` constant; the
   rest hardcode the literal directly in `getId()`. The stable-ID rule
   (AGENTS.md) wants one frozen literal per ID, referenceable from tests and
   mapping resources.
5. **Dead interface contract.** `JIPipeNodeTaxonomyFunction` carries
   `userCanCreate()`/`userCanDelete()` ("carried over from the node
   registration contract") that nothing reads.
6. **Fragile test.** `TaxonomyPluginTest.allTaxonomyRegistered` pins
   `assertEquals(89, ...getFunctions().all().size())` — it went red the moment
   the 90th function was added, even though nothing core broke. Exact-count
   pins on a live registry violate the plugin-aware test ruling (AGENTS.md,
   2026-10-22).

## Goal

Taxonomy registration and resolution work through established service
patterns, so that:

- `JIPipe.getNodeTaxonomyService()` is the canonical access to functions and
  frameworks; `TaxonomyPlugin` registers them into it **like any other JIPipe
  service component**, and **any plugin can register its own taxonomy
  members** (functions, frameworks, inference patterns).
- ID→function resolution is a **live-registry lookup** — adding a function is
  "declare class + register it", with no second edit anywhere.
- Inference knowledge **lives on the function classes** (weighted keyword
  patterns) and is aggregated by the service; a function that declares no
  patterns simply can't be inferred into.
- Every function and framework ID is a frozen `ID` constant.
- Taxonomy tests assert **presence** of the expected core IDs, never exact
  registry size.

## Rulings (2026-10-22, user)

- Service scope: **functions + frameworks + inference + tree validation only.**
  The resolver keeps node-driven state (membership, families, input-profile
  providers, tag enrichment) and reads the registries from the taxonomy
  service. Families do **not** move.
- Inference patterns: **weighted keyword patterns** on the functions;
  service builds an aggregate index over all registered functions;
  inference = highest total score.
- Inference fallback: **`MiscellaneousUnassignedFunction`** (the
  auto-imported noise sink), not `MiscellaneousFunction` — names with no
  pattern match land in *Miscellaneous unassigned*.
- Tests: **plugin-aware** — presence of expected IDs, never exact counts on
  live registries (codified in AGENTS.md the same day).

## Non-goals

- No UI changes (function tree, browser, finder render exactly as today).
- No change to node registration itself (`@ConfigureJIPipeNode`,
  `JIPipeJavaNodeInfo`, membership recording).
- No change to the JSON exact-key mappings (`ResourceMapping`), tag mappings
  (`TagMapping`, `CuratedNodeTags`), or the spatial-dimension derivation
  (`spatialFor`, `spatialForFirstImageInput`) — those are data-driven
  machinery, not the purpose-built switch, and stay in
  `TaxonomyAutoAssignment`.
- No persistence changes (taxonomy is code-defined; projects store node IDs
  only).
- No moving of family assembly, input-profile providers, or tag enrichment.

## Current system (context)

| Concern | Where |
|---|---|
| Function interface (id, name, parent, hue, tags, visibility flags, dead create/delete flags) | `api/nodes/taxonomy/JIPipeNodeTaxonomyFunction.java` (90 impls under `functions/`) |
| Framework interface + 16 impls | `api/nodes/taxonomy/JIPipeNodeTaxonomyFramework.java`, `frameworks/` (+ `NoFramework` sentinel) |
| In-memory registries (plain classes; hue resolution, tree validation, cycle detection) | `FunctionRegistry.java`, `FrameworkRegistry.java` |
| Registry ownership + node taxonomy state | `api/service/components/nodes/TaxonomyResolver.java` (function/framework registries, `nodeFunctions`, families, input-profile providers, `enrichNodeInfos`, `validateTaxonomy`) |
| Nodes service component | `api/service/components/JIPipeNodesServiceComponent.java` — `getFunctions()`/`getFrameworks()`/`inputProfileFor()`/`registerInputProfileProvider()` delegate to the resolver |
| Core registration | `plugins/taxonomy/TaxonomyPlugin.java` — arrays of 13 top-level + 77 leaf functions and 16 frameworks, registered into `JIPipe.getNodes().getFunctions()/getFrameworks()`; then `registerSystemTemplates()` |
| Auto-assignment utility | `api/nodes/taxonomy/TaxonomyAutoAssignment.java` (460 lines): `functionForName` (keyword if-chain), `functionForId` (~20-case switch), `functionForResource` (dead outside a test), `ResourceMapping`, `TagMapping`, `CuratedNodeTags`, `spatialFor*` |
| Consumers of ID/name resolution | `plugins/jipipe-plugin-ij2/.../ImageJ2OpNodeInfo.java` (`getPrimaryFunction`, `getSpatialDimensions`), `plugins/jipipe-plugin-clij/.../CLIJCommandNodeInfo.java` (same shape) — both **lazy** (called at UI/validation time, and during `recordNodeMembership` while plugins still register) |
| Consumers of the registries | ~19 files via `JIPipe.getNodes().getFunctions()/getFrameworks()` (desktop tree/browser, `ThemeUtils`, `JIPipeTaxonomyFunctionRef`, migration, expression syntax, tests, `TaxonomyValidationReport`) |
| Service wiring | `JIPipeService` constructor creates ~24 `JIPipeServiceComponent`s + `components[]` array; static accessors `JIPipe.getNodes()` → `instance.getNodes()` (with `ensureInitialized()`) |
| Behavior flags | `TaxonomyView.behaviorFlags(info)` reads `isVisibleInPipeline`/`isVisibleInCompartments`/`canExtract`/`isRunnable` off the **primary function** — consumed by node browser, delete context action, `JIPipeGraph` extraction, database entries |

## Design

### 1. `JIPipeNodeTaxonomyServiceComponent` (new)

`org.hkijena.jipipe.api.service.components.JIPipeNodeTaxonomyServiceComponent
extends JIPipeServiceComponent`.

**Owns** (moved out of `TaxonomyResolver`):
- `FunctionRegistry functionRegistry`
- `FrameworkRegistry frameworkRegistry`
- the inference index (below) + the inference fallback

**API** (mirrors how a node registry is addressed):

```java
// functions
void registerFunction(JIPipeNodeTaxonomyFunction function);   // idempotent by id (replace), as FunctionRegistry.register today
JIPipeNodeTaxonomyFunction unregisterFunction(String id);
JIPipeNodeTaxonomyFunction functionById(String id);           // null if unknown  (replaces TaxonomyAutoAssignment.functionForId)
Set<JIPipeNodeTaxonomyFunction> allFunctions();
Set<JIPipeNodeTaxonomyFunction> topLevelFunctions();
List<JIPipeNodeTaxonomyFunction> childrenOf(JIPipeNodeTaxonomyFunction parent);
float baseHue(JIPipeNodeTaxonomyFunction function);
int shadeDepth(JIPipeNodeTaxonomyFunction function);
Set<String> validateFunctions();                              // FunctionRegistry.validate()
JIPipeNodeTaxonomyFunction inferFunction(String name);        // see §3

// frameworks
void registerFramework(JIPipeNodeTaxonomyFramework framework);
JIPipeNodeTaxonomyFramework frameworkById(String id);
Set<JIPipeNodeTaxonomyFramework> allFrameworks();

// inference configuration
void setInferenceFallback(JIPipeNodeTaxonomyFunction fallback);  // stores the fallback's id; resolved live at inference time

// raw registries (kept for TaxonomyValidationReport + the nodes-service delegates)
FunctionRegistry getFunctionRegistry();
FrameworkRegistry getFrameworkRegistry();
```

`registerFunction`/`registerFramework` invalidate the inference index (lazy
rebuild on next `inferFunction`).

**Wiring** — `JIPipeService`:
- Constructor: `nodeTaxonomy = new JIPipeNodeTaxonomyServiceComponent(this);`
  created **before** `nodes` (construction order matters, see §2), added to the
  `components[]` array so postprocess/dispose cover it.
- Getter `getNodeTaxonomyService()` with `ensureInitialized()` (same shape as
  `getNodes()`).
- `JIPipe` static: `public static JIPipeNodeTaxonomyServiceComponent
  getNodeTaxonomyService() { return instance.getNodeTaxonomyService(); }`.

`JIPipeNodesServiceComponent` constructor gains the taxonomy component as a
parameter (its only construction site is `JIPipeService`) and forwards it to
`TaxonomyResolver`.

### 2. `TaxonomyResolver` integration

- Constructors take the `JIPipeNodeTaxonomyServiceComponent` (both overloads).
- **Removes** the `functionRegistry`/`frameworkRegistry` fields; all accesses
  go through the injected taxonomy service. Node-driven state —
  `nodeFunctions`, `familiesById`, `inputProfileProviders`,
  `classToNodeIds`, `enrichNodeInfos`, `validateTaxonomy` — is unchanged and
  keeps living here.
- `JIPipeNodesServiceComponent.getFunctions()`/`getFrameworks()` become thin
  delegates to the taxonomy service. **All ~19 existing call sites compile
  unchanged** — the nodes service stays a legitimate read facade for
  node-related taxonomy; the new accessor is the canonical one (no
  deprecation noise in a 6.0.0 branch for an internal API).

### 3. Inference on the functions

**Interface addition** — `JIPipeNodeTaxonomyFunction`:

```java
/**
 * Name-inference patterns of this function: weighted keywords matched as
 * case-insensitive substrings of a discovered node's display name. The
 * aggregate score is the sum of the weights of all matched keywords; the
 * function with the highest total wins. Functions that declare no patterns
 * cannot be inferred into. Empty by default.
 */
default List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns() {
    return List.of();
}
```

`JIPipeNodeTaxonomyInferencePattern` — new top-level record in
`api/nodes/taxonomy/`: `record JIPipeNodeTaxonomyInferencePattern(String
keyword, float weight)`.

**Service index.** The service aggregates over **all registered functions**:
for each function, each pattern contributes `(lowercased keyword, function
id, weight)` to a map. Rebuilt lazily after any register/unregister.

**`inferFunction(String name)`:**
1. `name` null/blank → fallback (§below).
2. Lowercase with `Locale.ROOT`; for every indexed keyword that is a
   substring of the name, add its weight to that function's score.
3. Winner = highest total score; **tie-break: lexicographically smallest
   function id** (deterministic).
4. No keyword matched at all → fallback.
5. Fallback = the function set via `setInferenceFallback`, resolved **by id
   through the live registry**; if none is set or it is not registered,
   return `null` (soft fail — the node simply stays unassigned, a valid and
   validated state).

**Weight migration** — the existing `functionForName` priority order (rules
1–9) maps to weights `10 − ruleIndex`, so the earliest-matching rule in the
old chain also carries the highest weight in the new scoring:

| Weight | Function | Keywords (migrated 1:1, none added/removed) |
|---|---|---|
| 9 | `SegmentationFunction` | `segment` |
| 8 | `FilteringFunction` | `filter` |
| 7 | `ThresholdingFunction` | `threshold` |
| 6 | `ResamplingFunction` | `resample`, `interpolat`, `resize`, `rescale`, `upscale`, `downscale` |
| 5 | `PreprocessingFunction` | `inpaint`, `restore`, `denoise`, `deconvolv`, `preprocess`, `background`, `flatten` |
| 4 | `MeasurementsFunction` | `measure`, `analyz`, `profile` |
| 3 | `MergingSplittingFunction` | `merge`, `split`, `stack`, `concatenat`, `combine`, `montage` |
| 2 | `ColorStackFunction` | `color`, `rgb`, `hsl`, `lut`, `colormap` |
| 1 | `FilteringFunction` | `blur`, `smooth`, `erode`, `dilate`, `morpholog`, `gaussian`, `median` |

(`MiscellaneousUnassignedFunction` declares **no** patterns — it is the
fallback, not an inference target.)

Parity note: first-match-wins and weighted-summing diverge only for
contrived multi-function keyword soup (e.g. a name stacking 6+ keywords from
a later rule against one keyword of an earlier rule); the behavior-parity
test (§5) runs the real IJ2/CLIJ name corpus and the existing unit-test
names, and any divergence found there is reviewed and either weight-tuned or
accepted as an improvement — the keyword set itself must remain an exact
migration.

**Caller migration** (IJ2 `ImageJ2OpNodeInfo.getPrimaryFunction`, CLIJ
`CLIJCommandNodeInfo` — identical shape):

```java
// was: TaxonomyAutoAssignment.functionForId(mapped)
JIPipeNodeTaxonomyFunction function = JIPipe.getNodeTaxonomyService().functionById(mapped);
...
// was: TaxonomyAutoAssignment.functionForName(getName())
return JIPipe.getNodeTaxonomyService().inferFunction(getName());
```

Both stay **soft-failing**: `functionById` returns `null` for an
unregistered id (callers keep their existing fall-through);
`inferFunction` never throws. This mirrors the already-established defensive
pattern in `getSpatialDimensions` ("registration must not crash if the node
service is not ready") and covers the registration-order hazard: IJ2/CLIJ
depend on `CorePlugin` + `ImageJDataTypes` but **not** on `TaxonomyPlugin`,
so their nodes can be resolved before the core taxonomy is registered
(usually only during startup ordering; the lazy UI reads always see the
full registry).

**`TaxonomyAutoAssignment` after the change** keeps exactly:
`ResourceMapping`, `TagMapping`, `CuratedNodeTags`, `spatialFor`,
`spatialForFirstImageInput`. Deleted: `functionForName`, `functionForId`,
`functionForResource`.

### 4. ID constants + interface cleanup

- **All 90 function classes + 16 framework classes** (plus
  `NoFramework`, done) get `public static final String ID = "<exact current
  literal>";` and `getId()` returns `ID`. The 59 functions and 16 frameworks
  that currently inline the literal need the constant added; values are
  frozen verbatim (stable-ID rule — saved projects and mapping resources
  reference them). `MiscellaneousUnassignedFunction` (user's new file) also
  gets `getId() { return ID; }`.
- `JIPipeNodeTaxonomyFunction`: **delete** `userCanCreate()` and
  `userCanDelete()` (no overrides, no readers — verified). **Keep**
  `canExtract()`, `isRunnable()`, `isVisibleInPipeline()`,
  `isVisibleInCompartments()` — live via `TaxonomyView.behaviorFlags`.
- Verification gate (plan step, scripted): `grep` that no
  function/framework `getId()` body contains a string literal, and that
  every such class declares `public static final String ID`.

### 5. Tests

Per the plugin-aware registry ruling (AGENTS.md): **no exact counts on live
registries.**

1. **`TaxonomyPluginTest` rewritten presence-based** (fixes the current red
   89-vs-90 pin): every ID in a curated expected-IDs list (the 90 core
   function id literals + 16 framework id literals, as constants via the new
   `ID` fields) is registered; `validateFunctions()` is empty. The list also
   doubles as the **ID-stability gate** (pins the exact literals). The
   `getFamilies().isEmpty()` assertion is kept but re-pointed: it asserts
   the nodes service's family *membership* state (families are
   node-driven, not plugin-contributed, so an empty-expected check remains
   valid there); tree integrity of functions/frameworks stays with
   `validateFunctions()` / `TaxonomyValidationReport`.
2. **Inference behavior parity** (`TaxonomyAutoAssignmentTest` extension or
   new `NodeTaxonomyInferenceTest`): the names used by the existing tests +
   a sample of real IJ2 op / CLIJ command names classify to the same
   function as the old `functionForName`; no-match names now yield
   `MiscellaneousUnassignedFunction` (the deliberate change, asserted
   explicitly); tie-breaks are deterministic.
3. **Service extensibility** (new test): a "second plugin" (test code
   registering through the service) adds a new function with inference
   patterns → resolvable by id, appears in `childrenOf`, is inferred into for
   a matching name, and disappears cleanly on `unregisterFunction`. Proves
   the plugin path is real, not core-to-core.
4. **`TaxonomyResolverTest`** updated for the new constructor (injects the
   service component from the booted instance); behavior assertions
   unchanged.

Untested (policy): no Swing tests; the mechanical ID-constant pass is
verified by the grep gate + compilation + the presence tests, not by
per-class tests.

## Migration order

1. **ID constants** (90 + 16 files, mechanical): no behavior change; suite
   must be green afterward (except the known-red count pin, untouched yet).
2. **Service component + wiring** (new class, `JIPipeService`, `JIPipe`,
   `TaxonomyResolver`, `JIPipeNodesServiceComponent` delegates,
   `TaxonomyPlugin`): behavior identical; `functionForId` still exists.
3. **Inference** (interface default + record, patterns on the 8 functions,
   service index + `inferFunction` + fallback, IJ2/CLIJ migration, deletion
   of `functionForName`/`functionForId`/`functionForResource`): the behavior
   change (fallback → unassigned) lands here, with the parity test.
4. **Tests** (presence rewrite, parity, extensibility, resolver ctor).
5. **Interface cleanup** (delete the two dead methods).

Each step compiles and runs the suite; steps 1–2 must leave classification
behavior byte-identical.

## Risks / notes

- **Construction order**: `JIPipeService` must create the taxonomy component
  before the nodes component (constructor injection). The `components[]`
  array and `postprocess` chain pick the taxonomy component up automatically.
- **Ordering hazard is accepted, handled soft**: core taxonomy is not
  guaranteed registered before IJ2/CLIJ `getPrimaryFunction()` runs
  (`recordNodeMembership` during startup). Null results degrade to
  "unassigned" and are corrected by the lazy UI/validation reads — the same
  shape the existing spatial-derivation code already relies on.
- **`ensureInitialized()` re-entrancy**: `TaxonomyPlugin.register()` runs
  during initialization and already calls `JIPipe.getNodes()` (same
  re-entrancy path) — `JIPipe.getNodeTaxonomyService()` is safe there.
- The 90-element expected-IDs list in the test is the one place the full
  core ID set is enumerated; it is maintained with the function classes and
  is what the presence gate checks.
