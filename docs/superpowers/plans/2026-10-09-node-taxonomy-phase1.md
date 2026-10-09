# Node Taxonomy Redesign — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 11-category `JIPipeNodeTypeCategory` system with the `JIPipeNodeTaxonomy*` model (function tree, framework, spatial dimensionality, derived input profile, workflow families, tags), convert all ~1,840 node registrations, migrate every consumer of the old category API, and deliver the adaptive node browser + rebuilt node finder — per the spec.

**Architecture:** Two sub-phases. **1a** builds the new taxonomy API, registries, seed content, node-database facets, and UI **in parallel with the untouched legacy system** (legacy annotation fields become `@Deprecated` but stay present, so the tree compiles at every commit). **1b** converts all node modules to the new annotations, migrates the ~15 non-node consumers to the function API (with a transition-only fallback), then removes the legacy system in one final atomic task. A startup/CI validation guard enforces taxonomy invariants throughout.

**Tech Stack:** Java 21, Maven, JUnit 5 (`jipipe-core` already has `junit-jupiter` + `junit-platform-launcher` + the `ImageJLegacyPatcherLauncherSessionListener`), JGraphT (already a dependency) for family graphs.

**Spec:** `docs/superpowers/specs/2026-10-08-node-taxonomy-redesign-design.md` — read it first; this plan implements its Phase 1 ("Data model, taxonomy, UI, conversion"). Phases 2 (rendering/DiceBear contrib module) and 3 (audit refinement) get their own plans.

**Why two sub-phases (do not flatten):** The legacy API (`JIPipeNodeInfo.getCategory()`, `getMenuPath()`, `JIPipeNodeDatabaseEntry.getCategoryIds()`) is consumed by ~15 non-node classes (cell renderers, `JIPipeDesktopGraphEditorUI` location-info, `JIPipeDesktopAlgorithmCompendiumUI`, `JIPipeGraphNode`/`JIPipeGraph` behavior flags, all 7 node-database entry classes, `JIPipeEnhancedNodeDatabaseSearch`). Removing the old annotation fields up-front would leave the tree non-compiling for ~15 tasks, breaking TDD and per-batch verification. So the old API stays (deprecated) until 1b's final rip-out, and the tree compiles at every commit.

## Global Constraints

- Branch: `1324-node-taxonomy-core` (GitLab convention `<issue-id>-<name-in-kebab-case>`); work in a git worktree off `master`.
- **6.0.0 hard break at the END of 1b.** The legacy system (`JIPipeNodeTypeCategory` + 11 classes, `menuPath()`/`nodeTypeCategory()` annotation fields, `getCategory()`/`getMenuPath()`/`getCategoryIds()`) is **removed in Task 27** — no shims, no deprecation window in the shipped 6.0.0. During 1a/1b it is `@Deprecated`-present only as a transition aid.
- **Registered IDs are stable string literals.** Function ids `"jipipe:function:…"`, framework ids `"jipipe:framework:…"`, family ids `"jipipe:family:…"` — never derived from class names (project rule, AGENTS.md).
- All new taxonomy names carry the `JIPipeNodeTaxonomy` prefix (spec decision #4).
- TDD: every API task starts with a failing test. Run with `mvn test -pl jipipe-core`.
- Never call `LegacyInjector.preinit()` from tests, never add `--add-opens` workarounds (AGENTS.md; the launcher session listener handles it).
- Commit after every task. The tree must compile at every commit in 1a and 1b (until the single Task 27 rip-out, which re-establishes compilation).

## Review Focus

Failure modes the spec implies but whose tests are easy to forget — each line is pinned into its owning task:

1. **A node annotation references an unregistered function** (function class exists but nobody registered it) → registration fails loudly at startup listing node id + function id; the node must not silently vanish. *Pinned in Task 7 + Task 8.*
2. **Function tree cyclic or has an unreachable leaf** → validation error naming the offending id. *Pinned in Task 2.*
3. **Two top-level branches declare the same hue** → visually indistinguishable branches; validation rejects. *Pinned in Task 9.*
4. **Framework abbreviation > 4 chars or duplicated** → badge overflows the title band. *Pinned in Task 11.*
5. **Input-profile derivation is not total** (a registered slot type has no provider and the path throws) → the whole plugin fails to load. Derivation must be total: missing provider ⇒ `NA`, never an exception. *Pinned in Task 1 + Task 7.*
6. **A conversion batch leaves a node with no function** → that node disappears from the node browser. The per-batch guard report must enumerate unassigned node ids for that module. *Pinned in Task 14, run in every 19.x batch.*
7. **A non-node consumer still reads `getCategory()` after the rip-out** → compile failure (good) but if missed before the rip-out, converted nodes render with the wrong (Internal/transparent) color. *Pinned in Task 21 (consumer migration, incl. the `ThemeUtils` color path) + Task 27 (the `git grep` gate).*

---

# Phase 1a — Build the new system in parallel

The legacy system is untouched (its annotation fields become `@Deprecated`). Every commit compiles.

### Task 1: Spatial dimension + input profile types

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomySpatialDimension.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyInputProfile.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/InputProfileProvider.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/InputProfileTest.java`

**Interfaces:**
- Produces:
  - `enum JIPipeNodeTaxonomySpatialDimension { SPATIAL_2D, SPATIAL_3D }`
  - `enum JIPipeNodeTaxonomyInputProfile { PLANE_2D, VOLUME_3D, ANY, NA }` (`NA` = not applicable; the total-derivation fallback, Review Focus #5)
  - `interface InputProfileProvider { JIPipeNodeTaxonomyInputProfile getProfile(Class<?> dataClass); }`

- [ ] **Step 1: Write the failing test** `InputProfileTest`:

```java
@Test void profileOfUnregisteredDataTypeIsNA() {
    InputProfileProvider empty = c -> JIPipeNodeTaxonomyInputProfile.NA;
    assertEquals(JIPipeNodeTaxonomyInputProfile.NA, empty.getProfile(JIPipeDataTable.class)); // total: never throws
}
@Test void spatialDimensionIsClosed() {
    assertArrayEquals(new JIPipeNodeTaxonomySpatialDimension[]{SPATIAL_2D, SPATIAL_3D},
        JIPipeNodeTaxonomySpatialDimension.values()); // no 4D/5D spatial exists
}
```

- [ ] **Step 2: Run** `mvn test -pl jipipe-core -Dtest=InputProfileTest` → FAIL (types missing).
- [ ] **Step 3: Implement** the three types exactly as above. `InputProfileProvider` is a functional interface; providers are registered per data class in `JIPipeNodesServiceComponent` (Task 7).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add spatial dimension and input profile taxonomy types"`.

---

### Task 2: Function interface + registry

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyFunction.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/FunctionRegistry.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/FunctionRegistryTest.java`

**Interfaces:**
- Produces:
  - `interface JIPipeNodeTaxonomyFunction` — `String getId()`, `String getName()`, `JIPipeNodeTaxonomyFunction getParent()`, `@Nullable Float getColorHue()` (top-level only; sub-branches return null and inherit), `String getDescription()`, `Icon getIcon()`, `Set<String> getTags()`, `boolean isVisibleInPipeline()`, `boolean isVisibleInCompartments()`, and the four carried-over defaults `canExtract()/userCanCreate()/userCanDelete()/isRunnable()` (all `true`).
  - `class FunctionRegistry` — `register(JIPipeNodeTaxonomyFunction)`, `JIPipeNodeTaxonomyFunction byId(String)`, `Set<…> all()`, `Set<…> topLevel()`, `List<…> children(JIPipeNodeTaxonomyFunction parent)`, `float baseHue(JIPipeNodeTaxonomyFunction f)` (nearest ancestor's non-null hue; the function's own if it has one), `int shadeDepth(JIPipeNodeTaxonomyFunction f)` (number of tree edges below that ancestor; 0 for the hue owner itself), `static float[] shadeHsb(float hue, float sat, float bri, int depth)` (implements the fixed shading rule below, verbatim, and returns `{hue, shadedSat, shadedBri}` — a pure function so the registry stays theme-independent; Task 21 composes it with the current theme style's sat/bri), `Set<String> validate()` (human-readable error strings; empty = OK).
  - **Corrected contract (controller ruling, pre-dispatch):** the original plan pinned `float effectiveHue(f)` whose pinned rule changes brightness/saturation — a hue-only float cannot carry it. `baseHue` + `shadeDepth` + `shadeHsb` replace it; the shading rule text is unchanged.
- Consumes: nothing.

- [ ] **Step 1: Write the failing tests** in `FunctionRegistryTest`:
  - `registerAndLookup`: 3-node chain (top "seg" → "cell" → "leaf"); `byId`/`children`/`topLevel` correct.
  - `cycleRejected`: two functions whose parent chain cycles ⇒ `validate()` non-empty, mentions the ids (Review Focus #2).
  - `unreachableLeafRejected`: a function whose ancestor chain ends at a function whose own parent is not registered ⇒ `validate()` non-empty. (Top-level = `getParent() == null`.)
  - `baseHueAndDepthTopLevel`: top with hue 0.5 ⇒ `baseHue` = 0.5, `shadeDepth` = 0.
  - `shadingByDepth`: child of a 0.5-hue top ⇒ `shadeDepth` = 1; depth-2 grandchild ⇒ `shadeDepth` = 2; `shadeHsb(0.5f, 0.5f, 0.5f, 1)` ≠ `shadeHsb(0.5f, 0.5f, 0.5f, 2)` (deterministic, monotonic lightness), and `shadeHsb(…, 0)` is the identity (sat/bri unchanged, hue unchanged).
  - `duplicateTopHuesReported`: two top-levels, same hue ⇒ `validate()` non-empty (Review Focus #3).
  - **Shading rule (implement exactly, do not vary):** walk up collecting the nearest ancestor's non-null `hue` and the depth `d` below it; `shadeHsb` = hue unchanged, brightness `+0.06*d` (cap 0.95), saturation `−0.04*d` (floor 0.05).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** (a plain class over `HashMap<String, JIPipeNodeTaxonomyFunction>`; validation walks parent chains with a visited set).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy function interface and registry with hue shading"`.

---

### Task 3: Framework interface + registry

**Files:**
- Create: `…/api/nodes/taxonomy/JIPipeNodeTaxonomyFramework.java`
- Create: `…/api/nodes/taxonomy/FrameworkRegistry.java`
- Test: `…/api/nodes/taxonomy/FrameworkRegistryTest.java`

**Interfaces:**
- Produces:
  - `interface JIPipeNodeTaxonomyFramework` — `String getId()`, `String getName()`, `String getAbbreviation()`, `String getPatternStyleId()`, `Color getColor()` (default impl = fixed 12-color neutral palette indexed by `Math.floorMod(id.hashCode(), 12)`; exact palette: `0x4a86c8, 0xd08a3e, 0x5aa05a, 0x8a63c0, 0xb3542e, 0x2e6db3, 0x5a7d2a, 0xc05a8a, 0x7a5ac0, 0x2ea0a0, 0xa0a02e, 0x666e7a`).
  - `class FrameworkRegistry` — `register(…)`, `byId(String)`, `Set<…> all()`, `Set<String> validate()` (errors: duplicate ids; abbreviation length > 4; empty abbreviation).
- Consumes: none.

- [ ] **Step 1: Write the failing tests** — `duplicateIdsRejected`, `abbreviationOverFourCharsRejected` (stub with `getAbbreviation()="ABCDE"`), `abbreviationEmptyRejected`, `colorStablePerId` (same id ⇒ same color across two registry instances).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy framework interface and registry"`.

---

### Task 4: Workflow families

**Files:**
- Create: `…/api/nodes/taxonomy/JIPipeNodeTaxonomyFamily.java`
- Create: `…/api/nodes/taxonomy/JIPipeNodeTaxonomyFamilyStep.java`
- Create: `…/api/nodes/taxonomy/JIPipeNodeTaxonomyFamilyStepAnnotation.java` (repeatable)
- Create: `…/api/nodes/taxonomy/FamilyAssembler.java`
- Test: `…/api/nodes/taxonomy/FamilyAssemblerTest.java`

**Interfaces:**
- Produces:
  - `interface JIPipeNodeTaxonomyFamily` — `String getId()`, `getName()`, `getDescription()`, `@Nullable JIPipeNodeTemplate getTemplate()`, `@Nullable JIPipeNodeExample getExample()`, `Set<String> getTags()`. (`JIPipeNodeTemplate` = `api/JIPipeNodeTemplate.java`; `JIPipeNodeExample` = `api/nodes/JIPipeNodeExample.java`.)
  - `class JIPipeNodeTaxonomyFamilyStep` — `String label()`, `nodeId()`, `purpose()`, `List<String> afterIds()`; a node's step id = `"familyId#nodeId"`.
  - `@Repeatable JIPipeNodeTaxonomyFamilyStepAnnotation` — `Class<? extends JIPipeNodeTaxonomyFamily> family()`, `String label()`, `Class<?>[] after() default {}` (predecessor **node classes**; empty = start step), `String purpose() default ""`.
  - `class FamilyAssembler` — `addStep(String nodeId, JIPipeNodeTaxonomyFamilyStepAnnotation a)`, `Graph<String, DefaultEdge> graph(JIPipeNodeTaxonomyFamily f)`, `Set<JIPipeNodeTaxonomyFamilyStep> steps(JIPipeNodeTaxonomyFamily f)`, `List<String> followUps(String nodeId)` (outgoing-edge targets, ordered), `Set<String> validate(Set<String> registeredNodeIds)` (error per dangling step whose node id is not registered).
- Consumes: `JIPipeNodeTemplate`, `JIPipeNodeExample` (existing).

- [ ] **Step 1: Write the failing tests** (tiny stub node classes in the test file — `DetectSpots` with `@JIPipeNodeTaxonomyFamilyStepAnnotation(family=Tm.class, label="Detect spots")`, `TrackSpots` with `after={DetectSpots.class}`):
  - `graphBuiltFromAnnotations`: 2 steps + 1 edge DetectSpots→TrackSpots.
  - `followUpsOrdered`: `followUps("detect")` = `["track"]`.
  - `danglingStepRejected`: step for an unregistered node id ⇒ `validate` non-empty, message contains the id.
  - `multiFamily`: one node annotated with two `family=` values appears in both.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** (JGraphT `SimpleDirectedGraph`).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add workflow family model and step assembler"`.

---

### Task 5: ADD taxonomy fields to `@ConfigureJIPipeNode` (+ tags, `NoFramework`) — old fields deprecated, NOT removed

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/ConfigureJIPipeNode.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipeNodeTaxonomyTags.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/NoFramework.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/ConfigureJIPipeNodeAnnotationTest.java`

**Interfaces:**
- Produces (on `@ConfigureJIPipeNode`):
  - `Class<? extends JIPipeNodeTaxonomyFunction>[] function() default { }` — first entry = primary; rest = secondary locations.
  - `JIPipeNodeTaxonomySpatialDimension[] spatialDimensions() default {}`
  - `Class<? extends JIPipeNodeTaxonomyFramework> framework() default NoFramework.class`
  - **NEW:** `@Deprecated` on `menuPath()` and `nodeTypeCategory()` (still present with their current defaults). They are removed only in Task 27.
  - `nodeClassification()`, `dataSourceMenuLocation()` unchanged.
  - New `@Repeatable JIPipeNodeTaxonomyTags` — `String[] value()`.
  - `class NoFramework implements JIPipeNodeTaxonomyFramework` — id `"jipipe:framework:none"`, name `"—"`, abbreviation `"—"`, patternStyleId `"initials"`.
- Consumes: Tasks 2, 3.

- [ ] **Step 1: Write the failing test** — a stub class annotated with the new fields; assert `function()[0]`, `spatialDimensions()`, `framework()` via `getAnnotation`; `@JIPipeNodeTaxonomyTags({"a","b"})` twice ⇒ `getAnnotationsByType` returns both, values concatenate. Also assert the OLD fields still exist (compile check) so nothing downstream breaks yet.
- [ ] **Step 2: Run** → FAIL (new fields missing).
- [ ] **Step 3: Implement** the added fields + `@Deprecated` markers + `NoFramework`.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy fields to ConfigureJIPipeNode, deprecate legacy fields"`.

Note: this task does NOT break the tree — it only adds fields and deprecates old ones. All ~1,840 node classes still compile unchanged.

---

### Task 6: ADD taxonomy getters to node info (+ extraction)

**Files:**
- Modify: `…/api/nodes/JIPipeNodeInfo.java` (interface)
- Modify: `…/api/nodes/infos/JIPipeMutableNodeInfo.java`
- Modify: `…/api/nodes/infos/JIPipeJavaNodeInfo.java`
- Test: `…/api/nodes/infos/JIPipeJavaNodeInfoTaxonomyTest.java`

**Interfaces:**
- Produces (on `JIPipeNodeInfo`, stored in `JIPipeMutableNodeInfo`):
  - `JIPipeNodeTaxonomyFunction getPrimaryFunction()` (nullable until the node is converted / resolved)
  - `Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions()`
  - `Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions()`
  - `JIPipeNodeTaxonomyFramework getFramework()` (never null; `NoFramework` default)
  - `Set<String> getTaxonomyTags()` (node-level tags only, from `@JIPipeNodeTaxonomyTags`)
  - `Set<String> getMergedTags()` (node tags ∪ tags of every function the node appears under, computed by Task 7 after registration; empty until then)
  - `JIPipeNodeTaxonomyInputProfile getInputProfile()` (derived in Task 7)
  - matching setters on `JIPipeMutableNodeInfo` (incl. `setMergedTags`).
  - `getCategory()`/`getMenuPath()` **kept** (now `@Deprecated` on the interface) so all consumers still compile through 1b.
  - **Controller ruling (pre-dispatch):** `JIPipeNodeInfo` has 5 DIRECT implementors that do NOT extend `JIPipeMutableNodeInfo` (`ImageJ2OpNodeInfo`, `CreateSpotDetectorNodeInfo`, `PlotTables2AlgorithmInfo`, `CLIJCommandNodeInfo`, `JIPipeEmptyNodeInfo`). Therefore every new getter must be a **`default` method** on the interface returning a safe value (`null` for `getPrimaryFunction`/`getSecondaryFunctions`→`Collections.emptySet()`, a `NoFramework` instance for `getFramework` (a shared instance is fine; `NoFramework` is a stateless sentinel), `Collections.emptySet()` for the spatial/tags sets, `JIPipeNodeTaxonomyInputProfile.NA` for `getInputProfile`), and `JIPipeMutableNodeInfo` OVERRIDES them with backing fields + the setters. Adding them as abstract methods would break all 5 direct implementors.
- Consumes: Tasks 2, 3, 5.

- [ ] **Step 1: Write the failing test** — stub node with `@ConfigureJIPipeNode(function={StubFuncA.class, StubFuncB.class}, spatialDimensions={SPATIAL_2D, SPATIAL_3D}, framework=StubFramework.class)` + `@JIPipeNodeTaxonomyTags({"x"})`; construct `new JIPipeJavaNodeInfo("test-node", Stub.class)`; assert primary/secondary/framework/spatial/tags correct. Also a stub using ONLY the legacy `nodeTypeCategory` ⇒ `getPrimaryFunction()` is null (unconverted), `getCategory()` still populated (dual state during transition).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** extraction in the constructor — new statics `getFunctionOf`, `getFrameworkOf`, `getSpatialOf`, `getTaxonomyTagsOf` mirroring the existing `getCategoryOf`/`getMenuPathOf`. Set new fields only when `function().length > 0`; always set framework (default `NoFramework`), spatial (default empty), tags.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: extract taxonomy from node annotations into node info"`.

---

### Task 7: Registry wiring (functions, frameworks, families, input profiles) — dual-read

**Files:**
- Modify: `…/api/service/components/JIPipeNodesServiceComponent.java`
- Create: `…/api/service/components/nodes/TaxonomyResolver.java`
- Test: `…/api/service/components/nodes/TaxonomyResolverTest.java`

**Interfaces:**
- Produces (on `JIPipeNodesServiceComponent` = `JIPipe.getNodes()`):
  - `FunctionRegistry getFunctions()`, `FrameworkRegistry getFrameworks()`, `Map<String, JIPipeNodeTaxonomyFamily> getFamilies()`
  - `void registerInputProfileProvider(InputProfileProvider p)`, `JIPipeNodeTaxonomyInputProfile inputProfileFor(Class<?> dataClass)` (first non-`NA` provider wins; all `NA` ⇒ `NA`; **never throws** — Review Focus #5)
  - `Set<String> validateTaxonomy()` — aggregates `FunctionRegistry.validate()`, `FrameworkRegistry.validate()`, family `validate()`, plus: for every node with a non-null primary function, that function and framework must be registered; a function class that exists but was never registered ⇒ error listing node id + function id (Review Focus #1). Nodes with null function are **unconverted** (expected in 1b) — they are reported separately, not as errors.
- Consumes: Tasks 2, 3, 4, 6.

- [ ] **Step 1: Write the failing tests** in `TaxonomyResolverTest` (construct a minimal `JIPipeNodesServiceComponent` the way existing service-component tests do — inspect `jipipe-core/src/test` for the pattern; if none, a no-op `JIPipeService` stub):
  - `unregisteredFunctionFailsValidation`: node info whose primary function was never registered ⇒ `validateTaxonomy()` non-empty, message has node id + function id.
  - `inputProfileTotal`: no providers ⇒ `inputProfileFor(JIPipeDataTable.class)` = `NA` (no exception).
  - `inputProfileFirstNonNAWins`: providers return `NA` then `VOLUME_3D` ⇒ `VOLUME_3D`.
  - `familyDanglingStepSurfaces`: family with a step for an unregistered node id ⇒ `validateTaxonomy()` non-empty.
  - `unconvertedNodeIsNotAnError`: node info with null function ⇒ not in `validateTaxonomy()` errors (but IS in the unassigned list — see Task 8).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.** In `register(JIPipeNodeInfo, JIPipeDependency)`: record node→function membership (node id → its primary + secondary functions) and node→framework membership; **do NOT** auto-register the function into the `FunctionRegistry` — the curated tree is registered once by the taxonomy plugin (Task 12); `validateTaxonomy()` then flags any node whose primary function is absent from that registry (the Review-Focus-#1 guard, and what makes the `unregisteredFunctionFailsValidation` test constructible). Read the node's `@JIPipeNodeTaxonomyFamilyStepAnnotation`s (repeatable, on `info.getInstanceClass()`) and `addStep` each into the `FamilyAssembler` (family id literal = `annotation.family().getId()`; the assembler's class→nodeId map is built from `getRegisteredNodeClasses()`). After `runRegistrationTasks()` completes (at the end of the settle loop, idempotently, iterating `registeredNodeInfos`): for every node info, compute `inputProfileFor` over its **first input slot's data class** (no input slots ⇒ `NA`), `setInputProfile`; if it has any function, `setMergedTags(node.getTaxonomyTags() ∪ tags of every function on its path, leaf→root, for both primary and secondary functions)` (Task 15 reads `getMergedTags()`).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: wire taxonomy registries into node service component"`.

---

### Task 8: Validation guard (startup + CI) with unassigned report

**Files:**
- Modify: `…/api/service/components/JIPipeNodesServiceComponent.java` (call the guard)
- Create: `…/api/service/components/nodes/TaxonomyValidationReport.java`
- Test: `…/api/nodes/taxonomy/TaxonomyValidationGuardTest.java`

**Interfaces:**
- Produces:
  - `class TaxonomyValidationReport` — `boolean ok()`, `List<String> errors()`, `List<String> unassignedNodeIds()` (nodes with null primary function — the "vanishes from browser" detector, Review Focus #6), `static TaxonomyValidationReport run(FunctionRegistry, FrameworkRegistry, Map<String, JIPipeNodeTaxonomyFamily>, Map<String, JIPipeNodeInfo>)`.
  - Guard: a final pass at the END of initialization runs `TaxonomyValidationReport.run(...)`; real `errors()` are added to the service's `JIPipeInitializationReport` (the existing init-failure mechanism — `JIPipeServiceDefaultInitializer.java:357` fails init when that report has errors; no new exception type exists or is needed). **During 1a** (nothing converted) `ok()` may be false only due to `unassignedNodeIds` being non-empty — that is the *expected 1a state*: log INFO, add nothing to the init report. A broken-but-partly-converted taxonomy (a `validate()` error) must never ship a wrong browser.
- Consumes: Task 7.

- [ ] **Step 1: Write the failing tests** — synthetic registries: (a) all valid + one unconverted node ⇒ `ok()` true, `unassignedNodeIds()` has that node, `errors()` empty; (b) cyclic functions ⇒ `errors()` non-empty ⇒ the startup hook path throws; (c) fully converted synthetic set ⇒ `unassignedNodeIds()` empty.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** the report + the guard. **Exception-type correction (controller ruling):** there is NO `JIPipeInitializationException` in this codebase — the established pattern is `JIPipeInitializationReport` (`api/service/JIPipeInitializationReport.java`), which the initializers check at the end of init: `JIPipeServiceDefaultInitializer.java:357` does `if (!getService().getInitializationReport().getErrors().isEmpty()) { … }` (and `JIPipeServiceNoImageJInitializer` + `JIPipeGUICommand.java:86-94` likewise). So: the guard = a final pass at the END of initialization that runs `TaxonomyValidationReport.run(...)`; real `errors()` are added to the `JIPipeInitializationReport` (which the existing init flow then fails on, exactly like other init errors); "all nodes unconverted" (only `unassignedNodeIds` non-empty) logs INFO and adds nothing. Additionally, ABSORB the Task 7 deferred concern: `JIPipeNodesServiceComponent` must STOP the per-settle-loop `validateTaxonomy()` WARN logging (Task 7's component lines ~99-102 log on every `runRegistrationTasks()` cycle — during the Task 19 conversion that re-logs the full error set repeatedly); leave a single silent `validateTaxonomy()` available for `TaxonomyValidationReport`/tests, with the one authoritative log+report happening at the final init pass. **Verified hook location:** the component's `postprocess(JIPipeProgressInfo)` override (added in Task 7) is the single final pass — it is invoked at `JIPipeServiceDefaultInitializer.java:333` (the `for(JIPipeServiceComponent component : getService().getComponents()) component.postprocess(…)` loop), which runs BEFORE the init-report error check at `:357`. So in that override: run `TaxonomyValidationReport.run(...)`, and if `errors()` is non-empty, append throwables to `getService().getInitializationReport().getErrors()` (the live list; `getErrors().add(…)` is how the initializer records init errors, e.g. `:245/:387`) so the existing `:357` block logs them. If only `unassignedNodeIds` is non-empty (the 1a "nothing converted" state), log INFO and append nothing. Do NOT touch `JIPipeServiceDefaultInitializer` itself — the hook is entirely inside the component.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy validation guard with unassigned-node report"`.

---

### Task 9: Top-level function branches

**Files:**
- Create: `…/api/nodes/taxonomy/functions/` — 13 files.
- Test: `…/api/nodes/taxonomy/functions/TopLevelBranchesTest.java`

**Interfaces:**
- Produces 13 top-level `JIPipeNodeTaxonomyFunction` impls (final classes, `getParent()==null`, explicit hue). Exact ids/names/hues (degrees; matching legacy category hues where they existed so the palette stays familiar):

| Class | id | name | hue |
|---|---|---|---|
| `DataSourcesFunction` | `jipipe:function:data` | Data Sources | 136 |
| `ImagesFunction` | `jipipe:function:images` | Images | 186 |
| `RoisFunction` | `jipipe:function:rois` | ROIs | 0 |
| `FilamentsFunction` | `jipipe:function:filaments` | Filaments | 40 |
| `TablesFunction` | `jipipe:function:tables` | Tables | 216 |
| `StatisticsFunction` | `jipipe:function:statistics` | Statistics | 60 |
| `TrackingFunction` | `jipipe:function:tracking` | Tracking | 165 |
| `AnnotationsFunction` | `jipipe:function:annotations` | Annotations | 264 |
| `DeepLearningFunction` | `jipipe:function:deep-learning` | Deep Learning | 200 |
| `PlottingFunction` | `jipipe:function:plotting` | Plotting | 155 |
| `ExportFunction` | `jipipe:function:export` | Export | 22 |
| `GraphFunction` | `jipipe:function:graph` | Graph / Project | null |
| `MiscellaneousFunction` | `jipipe:function:misc` | Miscellaneous | 50 |

`GraphFunction.getColorHue()` returns `null`, `isVisibleInPipeline()` returns **false**, and `isVisibleInCompartments()` returns **false** — mirroring legacy `InternalNodeTypeCategory` exactly (which is `false`/`false`/hue −1); the Graph/Project leaves are auto-generated scaffolding + debug utilities that the user does not manually insert, so they must NOT appear as an insertable section in the pipeline add-node browser. Consumers render the null hue using the theme's panel background (the legacy negative-hue case) — document this in the class javadoc. (Controller note: a transient "isVisibleInPipeline() stays true" reading was raised in review and retracted — the written plan + legacy precedent both give false.)

- [ ] **Step 1: Write the failing test** — instantiate all 13 via a test `all()` list; assert unique literal ids; names as above; hues exactly per the table (null for Graph); **pairwise hue uniqueness** among the 12 colored branches (Review Focus #3); all `getParent()==null`; after a register loop, all in a `FunctionRegistry`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** (icons: reuse legacy category icons where 1:1 — e.g. `actions/tool_imageeffects.png` for Images — else pick from `jipipe-core/src/main/resources/icons/actions/`).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add 13 top-level taxonomy function branches"`.

---

### Task 10: Seed function leaves

**Files:**
- Create: leaf classes under `…/functions/` in per-branch subpackages `data/ images/ rois/ filaments/ tables/ statistics/ tracking/ annotations/ deeplearning/ plotting/ export/ graph/` (class = `<Branch><Leaf>Function`, id = `jipipe:function:<leaf-name-kebab>` — the leaf's own name kebab-cased, NOT the full branch path, e.g. `ImageDataFunction`→`jipipe:function:image-data`; parent = the Task 9 branch).
- Test: `…/functions/FunctionLeavesTest.java`

**Interfaces:**
- Produces the exact leaf set (the contract the 1b conversion targets; Phase 3 may add/rename, never silently):
  - **data**: `ImageDataFunction`, `TableDataFunction`, `RoiDataFunction`, `FilamentDataFunction`, `MiscDataFunction`
  - **images**: `PreprocessingFunction`, `FilteringFunction`, `ThresholdingFunction`, `SegmentationFunction`, `SegmentationCellBasedFunction` (child of Segmentation), `MeasurementsFunction`, `ResamplingFunction`, `MergingSplittingFunction`, `ColorStackFunction`
  - **rois**: `RoiFiltersFunction`, `RoiMeasurementsFunction`, `RoiMorphologyFunction`, `RoiIntersectionsFunction`
  - **filaments**: `FilamentSmoothingFunction`, `FilamentFilteringFunction`, `FilamentQuantificationFunction`
  - **tables**: `TableFilterModifyFunction`, `TableJoinSplitFunction`, `TableAggregateFunction`, `TableTransformFunction`, `TableExportFileFunction`
  - **statistics**: `StatisticsDescriptiveFunction`, `StatisticsTestingFunction`, `StatisticsEstimationFunction`
  - **tracking**: `TrackingDetectionFunction`, `TrackingLinkingFunction`, `TrackingFilteringFunction`, `TrackingAnalysisFunction`
  - **annotations**: `AnnotationsEditFunction`, `AnnotationsConvertFunction`
  - **deeplearning**: `DeepLearningInferenceFunction`, `DeepLearningTrainingFunction`, `DeepLearningModelsFunction`
  - **plotting**: `PlotScatterFunction`, `PlotLineFunction`, `PlotBarFunction`, `PlotHeatmapFunction`, `PlotMiscFunction`
  - **export**: `ExportFileFormatsFunction`, `ExportArchiveFunction`
  - **graph**: `GraphCompartmentOutputsFunction`, `GraphWrappersFunction`, `GraphDebugFunction`
  (48 leaf classes total: 5 data + 9 images + 4 rois + 3 filaments + 5 tables + 3 statistics + 4 tracking + 2 annotations + 3 deeplearning + 5 plotting + 2 export + 3 graph. The earlier "39" count was an arithmetic slip — the named list above is the contract. Seed tags: `TrackingDetectionFunction` → `{"spot-detection"}`, `DeepLearningInferenceFunction` → `{"inference"}`, `SegmentationCellBasedFunction` → `{"cell-segmentation"}`; all others empty.)
- Consumes: Task 9.

- [ ] **Step 1: Write the failing test** — helper listing all 39; assert unique literal ids; each parent is a Task-9 top-level (or `SegmentationFunction` for the cell-based leaf); every leaf reachable via `topLevel()→…→leaf` (Review Focus #2); full set registered ⇒ `FunctionRegistry.validate()` empty.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** the 48 leaf classes.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add seed taxonomy function leaves"`.

---

### Task 11: Framework classes

**Files:**
- Create: `…/api/nodes/taxonomy/frameworks/` — 12 files.
- Test: `…/frameworks/FrameworksTest.java`

**Interfaces:**
- Produces 12 `JIPipeNodeTaxonomyFramework` impls:

| Class | id | name | abbrev | patternStyleId |
|---|---|---|---|---|
| `CoreFramework` | `jipipe:framework:core` | Core | CORE | `initials` |
| `Cellpose2Framework` | `jipipe:framework:cellpose2` | Cellpose 2 | CP2 | `blobs` |
| `Cellpose3Framework` | `jipipe:framework:cellpose3` | Cellpose 3 | CP3 | `blobs` |
| `Cellpose4Framework` | `jipipe:framework:cellpose4` | Cellpose 4 | CP4 | `blobs` |
| `TrackMateFramework` | `jipipe:framework:trackmate` | TrackMate | TM | `constellation` |
| `CometFramework` | `jipipe:framework:comet` | Comet | CMET | `constellation` |
| `JIPipeDLFramework` | `jipipe:framework:jipipedl` | JIPipeDL | JDL | `shape-grid` |
| `JFreeChartFramework` | `jipipe:framework:jfreechart` | JFreeChart | JFC | `shape-grid` |
| `OpenCVFramework` | `jipipe:framework:opencv` | OpenCV | OCV | `weave` |
| `ClijFramework` | `jipipe:framework:clij` | CLIJ | CLIJ | `weave` |
| `ImageJ1Framework` | `jipipe:framework:imagej1` | ImageJ 1 | IJ1 | `triangles` |
| `ImageJ2Framework` | `jipipe:framework:imagej2` | ImageJ 2 | IJ2 | `triangles` |

- [ ] **Step 1: Write the failing test** — all 12: unique ids; abbreviations ≤ 4 chars and unique (Review Focus #4); `FrameworkRegistry.validate()` empty when all registered; `getColor()` stable per id.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add seed taxonomy framework classes"`.

---

### Task 12: Taxonomy plugin + ImageJ input-profile provider

**Files:**
- Create: `…/plugins/taxonomy/TaxonomyPlugin.java` (extends `JIPipePrepackagedDefaultJavaPlugin`, id `jipipe:taxonomy`)
- Modify: the plugin init list where `CorePlugin` is declared (`grep -rn "new CorePlugin(" jipipe-core/src/main/java`) — add `TaxonomyPlugin` **before** node plugins so registries exist when nodes register.
- Modify: the ImageJ data-types plugin `register(...)` (`plugins/jipipe-plugin-ij/…/imagejdatatypes/` — find its plugin class) to register `ImageTypeInfoInputProfileProvider` reading `@ImageTypeInfo.numDimensions()`: 2→`PLANE_2D`, 3→`VOLUME_3D`, −1/absent→`ANY`; non-image types→`NA`.
- Test: `…/api/nodes/taxonomy/InputProfileProviderTest.java`

**Interfaces:**
- Produces:
  - `TaxonomyPlugin.register()` registers all 13 top-levels + 48 leaves + 12 frameworks into `JIPipe.getNodes()` registries.
  - `ImageTypeInfoInputProfileProvider implements InputProfileProvider` — `getProfile(Class)`: reads `@ImageTypeInfo`; unannotated ⇒ `NA`.
- Consumes: Tasks 7, 9, 10, 11.

- [ ] **Step 1: Write the failing tests** — `providerMapsNumDimensions`: stub classes `@ImageTypeInfo(numDimensions=2/3/-1)` ⇒ `PLANE_2D`/`VOLUME_3D`/`ANY`; unannotated ⇒ `NA`. `allTaxonomyRegistered`: after a `TaxonomyPlugin` register against the test service component (or full `JIPipe` init, per existing JIPipe-test precedent) ⇒ `getFunctions()` has 61 ids (13 top-levels + 48 leaves), `getFrameworks()` has 12, `validate()` empty.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy plugin and input profile provider"`.

---

### Task 13: Seed families + TrackMate step annotations

**Files:**
- Create: `…/api/nodes/taxonomy/families/` — `TrackMateFamily.java`, `Cellpose4Family.java`, `DeepLearningFamily.java`, `PlottingFamily.java` (ids `jipipe:family:trackmate` / `:cellpose4` / `:deep-learning` / `:plotting`; `getTemplate()`/`getExample()` return null for now — template wiring is Phase 3).
- Modify: `plugins/jipipe-plugin-ij-trackmate/` — add `@JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, …)` to the 18 TrackMate node classes (detection→linking→filtering→analysis; `after` = node classes in the same module).
- Test: `…/families/SeedFamiliesTest.java`

**Interfaces:**
- Produces: 4 family classes; the TrackMate step annotations; `followUps(detectSpotsNodeId)` = `[trackSpotsNodeId]`, etc.
- Consumes: Task 4.

- [ ] **Step 1: Write the failing test** — assemble `TrackMateFamily` steps from the annotated trackmate node classes (import them in the test); assert graph acyclic, no dangling step ids, `followUps` of the first step non-empty and ordered; all 4 families validate (unique ids, non-empty names).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** (read each node's purpose from its `SetJIPipeDocumentation`; chain per spec: Detect spots → Track spots → linkage (short/long) → Filter tracks → Analyze tracks).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add seed workflow families with TrackMate step annotations"`.

---

### Task 14: Conversion tooling + per-module report test

**Files:**
- Create: `utils/taxonomy-conversion/conversion-map.md` (records decisions; **committed**)
- Create: `utils/taxonomy-conversion/convert.py` (one-off rewriter; **git-ignored**)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`

**Interfaces:**
- Produces:
  - `map.tsv` format: `<module>\t<old-category-class>\t<menuPath-prefix-or->\t<FunctionLeaf>\t<FrameworkClass|->\t<spatial|->` (spatial ∈ `SPATIAL_2D`, `SPATIAL_3D`, `SPATIAL_2D,SPATIAL_3D`, `-`).
  - **Conversion rules** (recorded in `conversion-map.md`, applied in every 1b batch):
    1. `ImagesNodeTypeCategory` + menuPath → `images/` leaf by first menuPath segment ("Deep learning"→`DeepLearningInferenceFunction`, "Thresholding"→`ThresholdingFunction`, "Morphology" on ROI output→`RoiMorphologyFunction`, etc.).
    2. `ImageJNodeTypeCategory` (257 nodes) → framework `ImageJ1Framework`, function by menuPath; spatial: "3D" in name/menuPath→`SPATIAL_3D`, "2D"→`SPATIAL_2D`, else both when the slot takes unconstrained `ImagePlusData`.
    3. `RoiNodeTypeCategory` → `rois/` by menuPath. 4. `TableNodeTypeCategory` → `tables/`. 5. `AnnotationsNodeTypeCategory`→`annotations/`; `GraphAnnotationsNodeTypeCategory`→`AnnotationsEditFunction`.
    6. `DataSourceNodeTypeCategory` → `data/` leaf by output type (image→`ImageDataFunction`, `JIPipeDataTable`→`TableDataFunction`, ROI list→`RoiDataFunction`, filament→`FilamentDataFunction`, else `MiscDataFunction`).
    7. `ExportNodeTypeCategory` → `ExportFileFormatsFunction` (archive→`ExportArchiveFunction`).
    8. `FileSystemNodeTypeCategory` → data-source leaf by what it reads, else `MiscDataFunction`.
    9. `MiscellaneousNodeTypeCategory` → best-fit leaf; if truly none, `MiscellaneousFunction` (kept as a last-resort leaf).
    10. `InternalNodeTypeCategory` → `GraphDebugFunction`/`GraphWrappersFunction` by role.
    11. Framework by plugin: cellpose→`Cellpose{2,3,4}Framework` per subpackage; trackmate→`TrackMateFramework`; clij→`ClijFramework`; opencv→`OpenCVFramework`; plots→`JFreeChartFramework`; weka/ilastik/omnipose→`JIPipeDLFramework`; ij2-discovered→`ImageJ2Framework`; ij/ij-3d/ij-algorithms→`ImageJ1Framework` (unless more specific); core plugins→`CoreFramework`; else leave `NoFramework` (flagged for Phase 3).
    12. Spatial: read the node's first image input slot — `Image2DData`/`d2`⇒`SPATIAL_2D`; `d3`⇒`SPATIAL_3D`; `ImagePlusData`/`Image5DData`⇒both; non-image⇒absent (no `spatialDimensions` field).
  - `TaxonomyConversionReportTest`: reads `-Djipipe.taxonomy.report.module=<id>`, initializes JIPipe, collects `TaxonomyValidationReport` filtered to that module's node ids, **fails if any node in the module has a null function** (Review Focus #6). Passes when the module is fully converted.
- Consumes: Tasks 8, 9, 10, 11.

- [ ] **Step 1: Write `conversion-map.md`** with the 12 rules + an empty per-module status table (module, node count, status).
- [ ] **Step 2: Write `convert.py`** (argparse; `map.tsv` in; rewrites `@ConfigureJIPipeNode(...)` blocks in place — replaces `nodeTypeCategory=`/`menuPath=` with `function=` and inserts `framework=`/`spatialDimensions=` per the TSV; leaves name/descriptions/slots/other annotations untouched; `--dry-run` prints the change summary; idempotent).
- [ ] **Step 3: Write `TaxonomyConversionReportTest`** as specified.
- [ ] **Step 4: Verify tooling end-to-end** on a throwaway copy: copy one small module's source to `/tmp`, run `convert.py` with a 3-row TSV, confirm the rewrite + idempotency.
- [ ] **Step 5: Commit** — add `utils/taxonomy-conversion/convert.py` to `.gitignore` first; `git add utils/taxonomy-conversion/conversion-map.md jipipe-core/src/test/java/…/TaxonomyConversionReportTest.java && git commit -m "feat: add taxonomy conversion tooling and per-module report test"`.

---

### Task 15: Node database — ADD function facets + related/follow-ups (legacy kept)

**Files:**
- Modify: `…/api/nodes/database/JIPipeNodeDatabase.java`, `JIPipeNodeDatabaseEntry.java`, `entries/CreateNewNodeByInfoDatabaseEntry.java` (+ the other 6 entry impls), `JIPipeNodeDatabaseBuilderRun.java`
- Test: `…/api/nodes/database/NodeDatabaseTaxonomyTest.java`

**Interfaces:**
- Produces (on `JIPipeNodeDatabaseEntry`):
  - **ADD** `getFunctionIds()` (all functions the node appears under; empty until converted), `getFramework()` (never null), `getSpatialDimensions()`, `getInputProfile()`, `getTags()` (the node's merged tag set — read from `JIPipeNodeInfo.getMergedTags()`, Task 7).
  - **KEEP** `getCategoryIds()` (still populated from the legacy category for unconverted nodes) until Task 27.
  - On `JIPipeNodeDatabase`: `List<JIPipeNodeDatabaseEntry> followUps(JIPipeNodeDatabaseEntry e)` (via `FamilyAssembler.followUps` on e's node id, mapped to entries, ordered); `List<JIPipeNodeDatabaseEntry> related(JIPipeNodeDatabaseEntry e, int max)` (ranked: same-family siblings, excluding follow-ups > same-framework > shared-tag (≥2 shared before 1); de-duplicated; `JIPipeNodeClassification` down-ranking last; cached per entry, invalidated on database rebuild).
- Consumes: Tasks 7, 13.

- [ ] **Step 1: Write the failing test** — build the database over a small synthetic set of node infos (reuse Task 7's service-component construction): two `TrackMateFamily` nodes (detect→track) ⇒ `followUps(detect)` = `[track]`; a third node same framework, not in the family ⇒ `related(detect)` contains it after the family sibling; a shared-tag node ⇒ appears on tag match; cache: a second `related` call returns the same list instance.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** the new entry fields (populated in `JIPipeNodeDatabaseBuilderRun` from the node info) + `followUps`/`related`.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: add taxonomy facets and related/follow-ups to node database"`.

---

### Task 16: Adaptive node browser component (synthetic tests)

**Files:**
- Create: `…/desktop/app/grapheditor/addnodepanel/NodeBrowserModel.java` (non-Swing state)
- Create: `…/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java`
- Test: `…/addnodepanel/NodeBrowserModelTest.java`

**Interfaces:**
- Produces:
  - `class NodeBrowserModel` — `setSearch(String)`, `setFramework(@Nullable JIPipeNodeTaxonomyFramework)`, `setSpatial(Set<JIPipeNodeTaxonomySpatialDimension>)`, `setInput(Set<JIPipeNodeTaxonomyInputProfile>)`, `setTags(Set<String>)`, `setPinnedOnly(boolean)`, `setSelectionOnly(boolean)`, `setTreePath(List<JIPipeNodeTaxonomyFunction>)`, `int getTier(int widthPx)` ⇒ `widthPx < 380 ? TIER_COMPACT : TIER_COMFORTABLE` (`TIER_COMPACT=0`, `TIER_COMFORTABLE=1`), `boolean isSearchActive()` (non-empty search ⇒ flat-results mode, overrides tree).
  - `class JIPipeDesktopNodeBrowser extends JIPipeDesktopWorkbenchPanel` — ctor `(JIPipeDesktopWorkbench, JIPipeDesktopGraphEditorUI)`; renders per tier (compact: single tree list with node leaves; comfortable: tree list + results list); filter bar (own wrapping `JIPipeDesktopWrapLayout` row: framework combo, spatial 2D/3D checkboxes, input checkboxes, tags combo, ★ pinned, ⊂ selection-only); search field + AI toggle **lifted from** `JIPipeDesktopAddNodesPanel.initializeToolbar` (L615-689); follow-up strip at the bottom (collapsed when nothing selected; populated from `database.followUps`/`related`); DnD reuses `JIPipeDesktopAddNodeTransferHandler` on both tree-leaves and list rows; double-click ⇒ `entry.addToGraph(canvasUI)` (preserved from old panel L738-747); Enter in search ⇒ first result at cursor (L643); pin/unpin via existing settings key `"node-db"`/`pinned-node-ids` (L295-297).
  - **⊂ selection-only rule** (pinned, since the old panel had no equivalent): a node passes iff the canvas selection is non-empty **and** for at least one selected node's output slot there exists a candidate node's input slot whose data type is compatible with the output's data type — compatibility = the output data class is assignable to the input data class via the existing datatype-registry hierarchy (`JIPipe.getDataTypes()` assignability), the same relation JIPipe uses to allow a connection. Nodes with no input slots (templates, sources) are excluded; the rule is recomputed on selection change (listen to the existing `JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent`).
  - `JIPipeNodeDatabase.query(...)` gains an overload taking the facet state (framework/spatial/input/tags); the old overload delegates with empty facets.
- Consumes: Task 15.
- Note: during 1a the browser has no converted nodes to show; its tests use **synthetic** node infos that carry functions (the browser logic is function-driven, so this is valid).

- [ ] **Step 1: Write the failing tests** in `NodeBrowserModelTest` (pure logic, no Swing):
  - `tierBoundary`: `getTier(379)==TIER_COMPACT`, `getTier(380)==TIER_COMFORTABLE`.
  - `searchOverridesTree`: empty search + tree path ⇒ `isSearchActive()` false; non-empty ⇒ true.
  - `facetCombination`: setting framework+spatial+tags stores all three; clearing one leaves the others (no cross-reset).
  - `filterQuery`: over the Task 15 synthetic database — framework filter returns only that framework's nodes; spatial `SPATIAL_3D` returns nodes declaring it; input `PLANE_2D` returns nodes with that profile; tags intersect.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `NodeBrowserModel` first (TDD), then the Swing component (no Swing tests — spec: unit-level only).
- [ ] **Step 4: Run** → PASS (model tests).
- [ ] **Step 5: Commit** `git commit -m "feat: add adaptive node browser component"`.

---

### Task 17: Node finder dialog (three-zone)

**Files:**
- Modify: `…/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java`
- Create: `…/nodefinder/NodeFinderDetailRail.java`
- Test: none (Swing).

**Interfaces:**
- Consumes: Task 16 (reuse `NodeBrowserModel` + tree/list renderers as sub-panels).
- Produces: the Ctrl+A dialog is permanently three-zone (tree | list | detail rail); rail shows description, framework badge, tags, spatial + input profile, `related` list, `followUps` list with ▶ Insert setup (family template if present, else follow-up nodes unconnected) + Open example + docs. `AddNewNodeUIContextAction` unchanged (still opens this dialog).

- [ ] **Step 1: Rebuild** the dialog body on the browser's tree+list renderers; add `NodeFinderDetailRail`.
- [ ] **Step 2: Wire** ▶ Insert setup via the family's template or, absent, `addToGraph` per follow-up entry at the cursor.
- [ ] **Step 3:** `mvn -q compile -pl jipipe-core` → success.
- [ ] **Step 4: Manual smoke** (recorded, not automated — see Task 20 step 4 note).
- [ ] **Step 5: Commit** `git commit -m "feat: rebuild node finder dialog with detail rail and family follow-ups"`.

---

### Task 18: Search tokens — ADD taxonomy tokens (dual-read)

**Files:**
- Modify: `…/api/nodes/database/entries/CreateNewNodeByInfoDatabaseEntry.java` (+ siblings that build `WeightedTokens` — grep `getTokens()` in `api/nodes/database/`)
- Test: `…/api/nodes/database/TokenTaxonomyTest.java`

**Interfaces:**
- Produces: `getTokens()` composition = node name (highest weight, existing) **+** function path terms at decreasing weight (leaf > parent > … > top, e.g. leaf 1.0, each ancestor ×0.5) **+** framework name (1.0) **+** framework abbreviation (0.8) **+** tags (1.0 each) **+** description (existing). For **unconverted** nodes (no function), the function-path terms are empty and the existing category token (from `getCategoryIds()`) is retained — so search never regresses during 1b. Spatial/input-profile are **filter-only, not tokens** (spec).

- [ ] **Step 1: Write the failing test** — synthetic entry: name "Segment cells", function path [Images > Segmentation > Cell-based], framework Cellpose 4 (CP4), tags {cells}: token set has "segment cells" (weight ≥ all), "cell-based" > "segmentation" > "images", "cellpose 4" + "cp4" present, "cells" present; no spatial/input-profile tokens. Second case: unconverted entry (category set, no function) ⇒ category token present, no function tokens.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: feed taxonomy into node search tokens (dual-read)"`.

**End of 1a.** The new system is fully built and tested in parallel; the legacy system is intact; the tree compiles at every commit.

---

# Phase 1b — Convert all nodes, migrate consumers, rip out the legacy system

Every step keeps the tree compiling (legacy annotation fields still exist). The single Task 27 rip-out removes them and re-establishes a clean compiling tree.

**Per-batch method (Tasks 19.x):** (1) run `convert.py` with the module's TSV rows (built from Task 14 rules; record every non-obvious decision in `conversion-map.md`); (2) fix what the script can't — drop leftover `nodeTypeCategory`/`menuPath`/`@AddJIPipeNodeAlias` per node (aliases become secondary `function[]` entries), fix imports (remove `…categories.*`, add leaf/framework classes); (3) handle **programmatic `registerNodeType` nodes** in the module: `convert.py` only rewrites `@ConfigureJIPipeNode` blocks, but taxonomy extraction reads the node class's annotations regardless of registration path — so add `@ConfigureJIPipeNode(function = …, framework = …)` to those node classes by hand (list them in the module's `conversion-map.md` row); (4) `mvn -q compile -pl <module> -am` → **full success** (unconverted siblings still use the old fields, so the module compiles as a whole); (5) `mvn test -pl jipipe-core -Dtest=TaxonomyConversionReport -Djipipe.taxonomy.report.module=<module-id>` → **zero unassigned** for the module; (6) update `conversion-map.md`; (7) commit `feat: convert <module> node registrations to taxonomy`.

Module node counts (annotated classes): core 31, ij-algorithms 424, ij 80, ij-3d 62, ij-filaments 51, tables 48, filesystem 39, utils 28, annotations 29, omero 26, cellpose 21, trackmate 18, strings 15, forms 10, weka 9, python 9, scene-3d 7, opencv 6, r 5, imp 5, omnipose 5, clij 4, ij2 2, ij-multi-template-matching 1, ij-ocr 1, plots 1.

### Task 19.1: jipipe-core (31 annotated + the programmatic `registerNodeType` node classes)
**Files:** all `@ConfigureJIPipeNode` classes in `jipipe-core/src/main/java` (convert.py), **plus** the node classes registered programmatically via `registerNodeType` — `LoopStartNode`/`LoopEndNode` (`api/looping/LoopingPlugin.java`), `GraphWrapperAlgorithmInput`/`GraphWrapperAlgorithmOutput` (`api/grouping/GroupingPlugin.java`), `JIPipeProjectCompartmentOutput`/`JIPipeProjectCompartment` (`api/compartments/JIPipeCompartmentsPlugin.java`), `JIPipeDataFlowTunnel*Entrance/Exit` (`plugins/tunnels/TunnelsPlugin.java`), and the `plugins/multiparameters/` node classes (`MultiParametersPlugin`/`MultiParameterAlgorithmsPlugin`) — each gets a hand-written `@ConfigureJIPipeNode` with function/framework (rule: loops/wrappers/compartments → `GraphDebugFunction` or `GraphWrappersFunction`; tunnels → `GraphWrappersFunction`; multiparameters → `TablesFunction > TableTransformFunction` as appropriate — record each in `conversion-map.md`).
- [ ] **Steps 1–7** per the batch method. (This module defines node types other modules reference — fix ripple compile errors here.)
- [ ] **Verify:** `mvn -q compile -pl jipipe-core` + module report zero unassigned.
- [ ] **Commit.**

### Task 19.2: jipipe-plugin-ij-algorithms — batch A (classes 1–140)
**Files:** first ~140 of the 424 (split by subpackage alphabetically, `algorithms/` up through ~`filters/`).
- [ ] **Steps 1–7.** (Module still compiles — unconverted B/C use old fields.)

### Task 19.2b: jipipe-plugin-ij-algorithms — batch B (141–280)
- [ ] **Steps 1–7.**

### Task 19.2c: jipipe-plugin-ij-algorithms — batch C (281–424) + module completion
- [ ] **Steps 1–7.** Then `mvn -q compile -pl jipipe-plugin-ij-algorithms -am` full success + module report zero unassigned.

### Task 19.3: jipipe-plugin-ij (80)
Data types live here; `ImagePlusData`-based. Framework `ImageJ1Framework` (or `ImageJ2Framework` where a node wraps an IJ2 API — check imports). Spatial per slot types (rule 12).

### Task 19.4: jipipe-plugin-ij-3d (62)
`SPATIAL_3D` dominant; `ClijFramework` for the GPU-3D (clij2) subset, `ImageJ1Framework` otherwise.

### Task 19.5: jipipe-plugin-filaments + ij-filaments (51+)
`filaments/` leaves; `CoreFramework` for the filament core, `ImageJ1Framework` for ij-filaments ports.

### Task 19.6: jipipe-plugin-filesystem (39) + strings (15) + utils (28)
Filesystem: data-source leaf by read type (rule 8). Strings: `TableTransformFunction` for string-column ops, `MiscellaneousFunction` otherwise. Utils: best-fit per node.

### Task 19.7: jipipe-plugin-tables (48) + forms (10)
`tables/` leaves by operation; forms → `GraphWrappersFunction`.

### Task 19.8: jipipe-plugin-annotations (29) + omero (26)
Annotations → `annotations/`. OME-RO: decide `CoreFramework` vs `NoFramework` per node (rule 11 flags `NoFramework` for Phase 3); record the decision.

### Task 19.9: jipipe-plugin-cellpose (21) + omnipose (5)
Framework per subpackage (cp2/cp3/cp4); spatial `SPATIAL_2D, SPATIAL_3D` for inference nodes (unconstrained input slot — the Cellpose4 evidence). Add `@JIPipeNodeTaxonomyFamilyStepAnnotation(family = Cellpose4Family.class, …)` to cp4 inference/training (Load model → Predict → Post-process) and `DeepLearningFamily` steps for training nodes.

### Task 19.10: jipipe-plugin-trackmate (18)
Already annotated in Task 13; here convert the remaining `nodeTypeCategory`/`menuPath` fields to `function`/`framework` and complete.

### Task 19.11: small modules — weka (9), ilastik (8), python (9), r (5), scene-3d (7), opencv (6), clij (4), ij-multi-template-matching (1), ij-ocr (1), ij2 (2), plots (1)
Frameworks: weka/ilastik/omnipose→`JIPipeDLFramework`; python/r→`NoFramework` (scripting, Phase 3); scene-3d→`CoreFramework`; opencv→`OpenCVFramework`; clij→`ClijFramework`; ij2→`ImageJ2Framework` (the 2 annotated; the auto-discovered bulk is Task 20); plots→`JFreeChartFramework` + `@JIPipeNodeTaxonomyFamilyStepAnnotation(family = PlottingFamily.class, …)`; imp→`CoreFramework`.

---

### Task 20: IJ2/CLIJ auto-discovery assignment

**Files:** the IJ2 plugin's registration/discovery code (`grep -rn "JIPipeJavaNodeRegistrationTask\|register" plugins/jipipe-plugin-ij2/src/main/java`); same for `jipipe-plugin-clij` discovery.
**Interfaces:**
- Consumes: Task 7, Task 14 rules.
- Produces: `TaxonomyAutoAssignment` helper (in `api/nodes/taxonomy/` or the ij2 plugin) that for each auto-registered node sets: function by name/menuPath heuristics (small table: "segment"→`SegmentationFunction`, "filter"→`FilteringFunction`, …; default `MiscellaneousFunction`), framework `ImageJ2Framework`/`ClijFramework`, spatial `SPATIAL_2D, SPATIAL_3D` when the slot takes unconstrained image data (reuse the input-profile provider: `ANY` ⇒ both). **Input profile stays derived** (never heuristic — spec).
- Test: `…/api/nodes/taxonomy/TaxonomyAutoAssignmentTest.java` — stub discovered-node infos named "3D Gaussian filter" / "Median filter" / "Segment cells" ⇒ expected function leaves; unknown name ⇒ `MiscellaneousFunction`, no exception.
- [ ] **Steps:** 1 write test → 2 fail → 3 implement → 4 pass → 5 commit `feat: auto-assign taxonomy for discovered IJ2/CLIJ nodes`.

---

### Task 21: Migrate non-node consumers to the function API (transition fallback)

**Files** (the ~15 consumers found by `grep`): 
- Modify: `…/desktop/commons/components/renderers/JIPipeNodeInfoListCellRenderer.java`, `JIPipeNodeInfoOrExamplesListCellRenderer.java`, `JIPipeDesktopNodeDatabaseEntryListCellRenderer.java`
- Modify: `…/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (location-info text, L1172-1222)
- Modify: `…/desktop/app/documentation/JIPipeDesktopAlgorithmCompendiumUI.java` (grouping)
- Modify: `…/api/nodes/JIPipeGraphNode.java`, `JIPipeGraph.java` (behavior flags: `canExtract`/`isVisibleInPipeline`/etc.)
- Modify: all 7 `…/api/nodes/database/entries/*DatabaseEntry.java` (`getCategoryIds()` impls)
- Modify: `…/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java` (L259 category-id indexing)
- Modify: `…/utils/ThemeUtils.java` — `getNodeFillColor(JIPipeNodeInfo)`/`getNodeBorderColor(JIPipeNodeInfo)` (L289-331) currently read `info.getCategory().getColorHue()`; change to: if the info has a primary function ⇒ `FunctionRegistry baseHue` + `shadeDepth` (looked up via `JIPipe.getNodes().getFunctions()`; function with no hue in its chain ⇒ theme panel background, like the legacy negative-hue case; unregistered function ⇒ 0.5 base + depth 0 + WARN), then compose the fill/border via `FunctionRegistry.shadeHsb(baseHue, style.getNodeFillSaturation()/getNodeBorderSaturation(), style.getNodeFillBrightness()/getNodeBorderBrightness(), shadeDepth)` (see the Task 2 corrected contract), else fall back to the legacy `info.getCategory().getColorHue()` (unconverted nodes keep today's color). This is what makes converted nodes get their new tree-derived color on the canvas **during the 1b conversion** (Review Focus #7).
- Create: `…/api/nodes/taxonomy/TaxonomyView.java` (the transition helper)
- Test: `…/api/nodes/taxonomy/TaxonomyViewTest.java`, `…/utils/ThemeUtilsFunctionColorTest.java`

**Interfaces:**
- Produces:
  - `class TaxonomyView` — `functionDisplayName(JIPipeNodeInfo)` ⇒ primary function's name, or the legacy category name (fallback for unconverted nodes); `locationPath(JIPipeNodeInfo)` ⇒ "Function > … > node" (or legacy "Category > menuPath > node" fallback); `behaviorFlags(JIPipeNodeInfo)` ⇒ the four flags from the primary function, or from the legacy category (fallback); `searchIds(JIPipeNodeDatabaseEntry)` ⇒ function ids, else category ids. All consumers are rewired to call `TaxonomyView` instead of `getCategory()`/`getMenuPath()`/`getCategoryIds()` directly. **The fallbacks are transition-only and are deleted in Task 27** (Review Focus #7).
  - `ThemeUtils` function-hue path as above, with a pure-hue fallback for functions not yet in the registry.
- Consumes: Tasks 2, 6, 7, 15.

- [ ] **Step 1: Write the failing tests** — `TaxonomyViewTest`: a converted info (function set) ⇒ `functionDisplayName` = function name, `behaviorFlags` from function; an unconverted info (category set, function null) ⇒ `functionDisplayName` = category name (fallback), `behaviorFlags` from category; `searchIds` returns function ids when present else category ids. `ThemeUtilsFunctionColorTest`: a converted info whose primary function is a top-level hue-186/360 ⇒ `getNodeFillColor(info)` = `Color.getHSBColor(186f/360f, style.getNodeFillSaturation(), style.getNodeFillBrightness())`; a child function ⇒ shaded per the Task 2 rule; an unconverted info ⇒ legacy category hue result (no change).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `TaxonomyView` + the `ThemeUtils` dual-read, then rewire all ~15 consumers to use `TaxonomyView` (this is the bulk; keep each consumer's visible behavior identical for converted nodes, and legacy-equivalent for unconverted ones).
- [ ] **Step 4: Run** `mvn -q compile -pl jipipe-core` + the tests → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: migrate category consumers to taxonomy view (transition fallback)"`.

---

### Task 22: Full validation gate (all nodes converted)

**Files:**
- Create: `…/api/nodes/taxonomy/TaxonomyFullValidationTest.java`
- Test: this file.

**Interfaces:**
- Consumes: everything (full JIPipe init).
- Produces: a test that, after all 19.x + 20 are done, runs `TaxonomyValidationReport.run(...)` over the **entire** registry ⇒ `ok()` true AND `unassignedNodeIds()` **empty** (the whole-point check that the 1,840-site conversion is complete — the branch gate). Uses the full JIPipe init like the existing JIPipe tests.
- [ ] **Step 1: Write the test.**
- [ ] **Step 2: Run** → should PASS now (all nodes converted). If it lists remaining unassigned nodes, fix those stragglers (likely a few in small modules) and re-run.
- [ ] **Step 3: Run** → PASS.
- [ ] **Step 4: Commit** `git commit -m "test: full taxonomy validation gate (all nodes converted)"`.

---

### Task 23: Migration fixtures (5.3.0 projects load)

**Files:**
- Create: `jipipe-core/src/test/resources/projects/5.3.0-taxonomy-fixtures/` — 3 minimal project JSONs (copy real 5.3.0 projects if present: `git log --all --diff-filter=A --name-only | grep -iE "\.jipipe$"`; else hand-write minimal projects using node ids that exist post-conversion). Names: `basic-images.jipipe`, `trackmate-setup.jipipe`, `tables.jipipe`.
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/project/FixtureProjectsLoadTest.java`
- Test: this file.

**Interfaces:**
- Produces: for each fixture — load the project, assert no exception, every loaded graph node has a **non-null primary function** (the "projects unaffected" guarantee: they store only node ids, which resolve to a function now), and the project round-trips (serialize → reload → same node ids).
- [ ] **Step 1: Write the fixtures + failing test.**
- [ ] **Step 2: Run** → observe failures (should pass if conversion is complete).
- [ ] **Step 3: Implement/fix** any load issue.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "test: 5.3.0 fixture projects load under the taxonomy"`.

---

### Task 24: Wire the adaptive browser into both editors (replace the old panel)

**Files:**
- Modify: `…/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (`initializeDefaultPanels` L230-297, `DOCK_ADD_NODES` add at L251-256)
- Modify: `…/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (L190)
- Delete: `JIPipeDesktopAddNodesPanel.java`, `JIPipeDesktopAddNodePanelEntryListCellRenderer.java`
- Test: none (Swing mount; covered by Task 23 fixtures + manual smoke).

**Interfaces:**
- Consumes: Task 16.
- Produces: both editors mount `JIPipeDesktopNodeBrowser` in `DOCK_ADD_NODES`. The old panel's `initializeMenuForCategory` and `initializeAddDataSourceMenu` are referenced **only** inside the panel itself (verified: no external callers — their right-click category-button popups die with the panel, and the canvas "Add new node" context action already opens the node finder dialog, not these menus). Data-source insertion is preserved by a "Data sources" section in the browser tree (the `DataSourcesFunction` branch already lists them by data type via the existing `JIPipeNodesServiceComponent.getMenuDataSourcesFor` mechanism).
- [ ] **Step 1:** Swap the constructor call in both editors (`new JIPipeDesktopAddNodesPanel(workbench, this)` → `new JIPipeDesktopNodeBrowser(workbench, this)`); compile.
- [ ] **Step 2:** Delete the old panel + renderer + the two menu-builder methods; fix dangling references; add the data-sources section to the browser.
- [ ] **Step 3:** `mvn -q compile -pl jipipe-core` → success.
- [ ] **Step 4: Manual smoke** (recorded, not automated — no display in CI is fine; note "verified locally" or "deferred" in the commit): launch desktop, panel appears, search + drag/drop + double-click work, filter bar filters, tier switches on dock resize.
- [ ] **Step 5:** Commit `git commit -m "feat: replace add-nodes panel with adaptive taxonomy browser"`.

---

### Task 25: Remove legacy search-token + entry category fields

**Files:**
- Modify: `…/api/nodes/database/entries/*DatabaseEntry.java` (remove `getCategoryIds()` now that all nodes are converted and `JIPipeEnhancedNodeDatabaseSearch` uses function ids), `JIPipeNodeDatabaseEntry.java` (remove `getCategoryIds()`), `JIPipeEnhancedNodeDatabaseSearch.java` (L259 → function ids), `CreateNewNodeByInfoDatabaseEntry.getTokens()` (drop the legacy category-token fallback from Task 18).
- Test: `…/api/nodes/database/TokenTaxonomyTest.java` (extend: assert no category token for any node now).

**Interfaces:**
- Consumes: Tasks 18, 21.
- Produces: `getCategoryIds()` gone from the entry interface; search tokens are purely function/framework/tag-based; the unconverted-node category-token branch in `getTokens()` deleted.
- [ ] **Step 1: Extend the token test** — assert every entry's tokens contain no legacy category token.
- [ ] **Step 2: Run** → FAIL (method still present).
- [ ] **Step 3: Implement** the removals.
- [ ] **Step 4: Run** `mvn -q compile -pl jipipe-core` + tests → PASS.
- [ ] **Step 5: Commit** `git commit -m "refactor: remove legacy category ids from node database"`.

---

### Task 26: Remove `JIPipeNodeInfo.getCategory()`/`getMenuPath()` + `JIPipeNodeMenuLocation` rework

**Files:**
- Modify: `…/api/nodes/JIPipeNodeInfo.java` (remove `getCategory()`, `getMenuPath()`, `getAliases()`; `getPrimaryMenuLocation()` reworked to return the primary function), `…/api/nodes/infos/JIPipeMutableNodeInfo.java` + `JIPipeJavaNodeInfo.java` (remove the category/menuPath fields + the `getCategoryOf`/`getMenuPathOf`/`getAliasesOf` statics + `@AddJIPipeNodeAlias` extraction), `…/api/nodes/JIPipeNodeMenuLocation.java` — **disposition decided: delete it.** After Task 21, no consumer reads `JIPipeNodeMenuLocation` (its callers — `JIPipeDesktopGraphEditorUI` location-info L1196 and the old panel's menu builder — were rewired to `TaxonomyView`/`initializeFunctionMenu`; verify with `git grep JIPipeNodeMenuLocation` before deleting, and if a consumer remains, migrate it first). `@AddJIPipeNodeAlias`/`AddJIPipeNodeAliases` are removed too (aliases became secondary `function[]` entries during conversion).
- Test: `…/api/nodes/infos/JIPipeJavaNodeInfoTaxonomyTest.java` (extend: assert the removed methods are gone — compile check — and secondary functions replace aliases).

**Interfaces:**
- Consumes: Tasks 6, 21 (all consumers now use `TaxonomyView`/function API).
- Produces: `JIPipeNodeInfo` exposes only taxonomy getters; `@AddJIPipeNodeAlias` extraction removed (aliases became secondary `function[]` entries during conversion). `git grep "getMenuPath()"` (node-info sense) ⇒ nothing.
- [ ] **Step 1: Extend the test** — a node with secondary functions ⇒ `getSecondaryFunctions()` correct; assert no compile reference to `getCategory()`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** the removals + `JIPipeNodeMenuLocation` deletion; **also delete the `ThemeUtils` node-info methods' legacy `info.getCategory()` fallback branch** (it cannot compile once `getCategory()` is gone — this must land with this task, not Task 27); the `getNodeFillColor/getNodeBorderColor(JIPipeNodeTypeCategory)` overloads are removed in Task 27 with the interface. Fix any last consumer the `git grep` reveals.
- [ ] **Step 4: Run** `mvn -q compile -pl jipipe-core` + tests → PASS.
- [ ] **Step 5: Commit** `git commit -m "refactor: remove legacy category/menuPath from node info"`.

---

### Task 27: The legacy rip-out (atomic) — remove `JIPipeNodeTypeCategory` + 11 classes + deprecated annotation fields + `TaxonomyView` fallback

**Files:**
- Delete: `…/api/nodes/JIPipeNodeTypeCategory.java` + all 11 files in `…/api/nodes/categories/`
- Modify: `…/api/ConfigureJIPipeNode.java` — remove the now-deprecated `menuPath()` and `nodeTypeCategory()` fields.
- Modify: `…/api/nodes/taxonomy/TaxonomyView.java` — delete the legacy fallback branches (all nodes have functions, all consumers use the new API).
- Modify: `…/utils/ThemeUtils.java` — remove the `getNodeFillColor/getNodeBorderColor(JIPipeNodeTypeCategory)` overloads; the node-info methods keep their dual-read (task 21) but the legacy `info.getCategory()` fallback branch is deleted (all nodes converted — guard: the fallback now asserts and throws if ever reached, which cannot happen once `getCategory()` itself is gone in task 26; the test in step 1 pins the function path).
- Modify: `…/JIPipeDefaultJavaPlugin.java` — remove `registerNodeTypeCategory`.
- Test: `…/utils/ThemeUtilsFunctionColorTest.java` (already created in Task 21 — extend it)

**Interfaces:**
- Consumes: all prior tasks.
- Produces: `git grep JIPipeNodeTypeCategory` ⇒ nothing; `git grep "nodeTypeCategory()"` ⇒ nothing; `git grep "TaxonomyView"` ⇒ only the (fallback-free) class. Full `mvn -q compile` (all modules) + `mvn test -pl jipipe-core` ⇒ PASS.

- [ ] **Step 1: Extend `ThemeUtilsFunctionColorTest`** — the function-hue path (top hue, shaded child, 0.5-fallback for an unregistered function) was pinned in Task 21; add one assertion that the result no longer depends on any legacy category object (e.g. an info with both function and a legacy-Internal-equivalent category still yields the function hue — this pins that the legacy branch is truly gone after step 3).
- [ ] **Step 2: Run** → PASS (behavior unchanged; the test now also documents the removal).
- [ ] **Step 3: Perform the atomic removals** (annotation fields, category interface + 11 classes, `ThemeUtils` legacy overloads, `registerNodeTypeCategory`, `TaxonomyView` fallbacks).
- [ ] **Step 4: Run** `mvn -q compile` (all modules) + `mvn test -pl jipipe-core` → PASS. Fix any straggler the compiler surfaces (should be none — Tasks 21-26 migrated every consumer).
- [ ] **Step 5: Commit** `git commit -m "refactor: remove legacy node type category system (6.0.0 break)"`.

---

### Task 28: Final cleanup + branch review prep

**Files:**
- Modify: `AGENTS.md` (the `api/nodes/categories` / "Node categories" references are obsolete — replace with a short "Node taxonomy" section pointing at the spec + the `api/nodes/taxonomy/` package).
- Modify: user-facing strings referencing "category" in the node system (`grep -rn "node category\|nodeCategory" docs/ jipipe-core/src/main`).
- Test: `mvn -q test -pl jipipe-core` (full core suite).

- [ ] **Step 1: Update AGENTS.md.**
- [ ] **Step 2:** Full core test suite → PASS.
- [ ] **Step 3:** Gates: `git grep JIPipeNodeTypeCategory` ⇒ nothing; `git grep "menuPath()"` ⇒ nothing (the data-type menu path, if a distinct API still exists, is noted in `conversion-map.md`).
- [ ] **Step 4: Commit** `git commit -m "chore: update docs for node taxonomy (6.0.0)"`.
- [ ] **Step 5:** Push branch `1324-node-taxonomy-core`, open MR.

---

## Out of scope for this plan (own plans/specs)

- **Phase 2 — node rendering:** the DiceBear contrib module (`contrib/jipipe-avatar-dicebear/`), Batik dependency + `dist/dist-info.json` entries, `JIPipeDesktopGraphNodeUI` pattern+badge paint, theme-editor toggles. Its plan uses the framework classes' `getPatternStyleId()` (Task 11) as the stable contract.
- **Phase 3 — audit refinement:** per-node placement review of the converted ~1,840 nodes against the live tree, `WeightedTokens` re-balancing, Miscellaneous pruning, custom pattern styles, family template wiring (`getTemplate()` returns null until then — Task 13 defers it), and the `NoFramework`-remaining decisions flagged in `conversion-map.md` (rule 11).
- The 5D-stack-splitting user settings (which T/C axes to iterate) already exist in the iteration layer; nothing changes there.
