# Node Taxonomy Service Component - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Taxonomy functions/frameworks are registered through a first-class `JIPipeNodeTaxonomyServiceComponent`
(plugin-extensible, live ID resolution, weighted name inference owned by the function classes), with all 90+16 IDs
frozen as `ID` constants.

**Architecture:** A new `JIPipeServiceComponent` owns the `FunctionRegistry`/`FrameworkRegistry` (moved out of
`TaxonomyResolver`) plus a keyword→ (function,weight) inference index built from `getInferencePatterns()` on the
registered functions. `JIPipeDefaultJavaPlugin` gains `registerTaxonomyFunction`/`registerTaxonomyFramework` one-line
forwarders (the established plugin idiom); `TaxonomyPlugin` uses them. IJ2/CLIJ resolve IDs via the live service and
infer names through it; the hardcoded `functionForId`/`functionForName`/`functionForResource` are deleted.

**Tech Stack:** Java 21, Maven multi-module, JUnit 5 (jipipe-core tests only), headless `JIPipe.ensureInstance()` where
a registry boot is needed.

**Spec:** `docs/superpowers/specs/2026-10-22-node-taxonomy-service-design.md` - the plan argues from the spec; read
both.

## Global Constraints

- **Stable-ID rule (AGENTS.md):** every taxonomy ID is a frozen string literal -
  `public static final String ID = "<exact current literal>";` and `getId() { return ID; }`. The literal value must
  never change (saved projects and JSON mapping resources reference them).
- **Plugin-aware tests (AGENTS.md, 2026-10-22):** tests on live registries assert **presence** of the expected core
  IDs + structural invariants - **never exact registry size**.
- **Test policy (AGENTS.md):** no Swing tests; no tests for trivial code (getters/setters, pure delegation). What
  warrants a test: non-trivial logic, ID stability, ordering/determinism contracts, fail-closed guards, registry
  presence contracts.
- **No behavior drift in Tasks 1–2:** classification outcomes of the live system must be byte-identical after each of
  those tasks (except the one known-red count pin, fixed in Task 4). The only intentional behavior change - inference
  fallback `Miscellaneous` → `MiscellaneousUnassigned` - lands in Task 3, covered by explicit tests.
- **Soft failures:** `functionById` returns `null` for unknown ids; `inferFunction` never throws (null/blank name or
  unregistered fallback → `null` or fallback, never an exception). Callers in IJ2/CLIJ keep their existing
  fall-throughs.
- **Commits:** one per task (or per stated sub-step), message suffix `(6.0.0)`. Do not commit `.superpowers/`
  (gitignored) or `utils/taxonomy-research/registry.json` / `fresh-node-slots.json` (drift after launcher runs - restore
  with `git checkout --` if touched).
- **Suite commands:** core = `mvn -o -pl jipipe-core clean test` (expect 463/0/0/1 with 1 known exception per Task);
  launcher = `mvn -o -pl jipipe-launcher clean test` (expect 19/0/0/0).

## Review Focus

- **Unknown function id in a plugin JSON mapping** (IJ2/CLIJ `*Mapping.functionIdOf` → `functionById`) → must resolve to
  `null` and the caller falls through to inference; never throws, never crashes startup.
- **Duplicate-id registration** (a plugin re-registers a core function id) → last-wins replace, the inference index
  rebuilds, and `validateFunctions()` stays clean (no duplicate-state corruption).
- **Function whose parent is unregistered** → `validateFunctions()` reports "references unregistered parent" and
  `TaxonomyPluginTest` presence+validation catches it at boot.
- **Inference ties** (two functions score equally) → deterministic winner = lexicographically smallest function id; same
  name always yields the same function.
- **Fallback function unregistered** (inference asked before `TaxonomyPlugin` ran) → `inferFunction` returns `null`
  (node stays unassigned), no exception; IJ2/CLIJ `getPrimaryFunction()` degrades exactly like today's `functionForId`
  -miss path.

---

### Task 1: ID constants on all functions + frameworks

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/**` - the 59 function classes whose
  `getId()` returns a bare literal (31 already have the `ID` constant; among those, the few whose `getId()` still
  returns the literal, e.g. `MiscellaneousUnassignedFunction`, get `return ID;`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/*.java` - all 16 (none have the
  constant yet; `NoFramework` already conforms)

**Interfaces:**

- Consumes: nothing (pure mechanical pass).
- Produces: every function/framework class exposes `public static final String ID` and `getId() { return ID; }`. Task
  3's inference tests and Task 4's expected-IDs list reference the `ID` constants.

- [ ] **Step 1: Generate the exact edit list**

```bash
cd /data/src/jipipe-4
python3 - <<'EOF'
import re, pathlib
base = pathlib.Path("jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy")
todo = []
for p in sorted(list(base.glob("functions/**/*.java")) + list(base.glob("frameworks/*.java"))):
    src = p.read_text()
    if "JIPipeNodeTaxonomyFunction" not in src and "JIPipeNodeTaxonomyFramework" not in src:
        continue
    has_const = "public static final String ID" in src
    m = re.search(r'public String getId\(\)\s*\{\s*return\s+"([^"]+)"\s*;', src)
    bare = m is not None
    if not has_const or bare:
        lit = m.group(1) if bare else None
        todo.append((str(p), has_const, bare, lit))
print(f"{len(todo)} classes to edit")
for path, has_const, bare, lit in todo:
    print(f"{path}\tconst={has_const}\tbare={bare}\tliteral={lit}")
EOF
```

Expected: a list of ~75 classes with their current literals. This is the authoritative work list - no class is guessed.

- [ ] **Step 2: Apply the constant (one class at a time, from the list)**

For each listed class:

- If it has no `ID` constant: add
  `/** The stable registry id of this function (a string literal per the stable-ID rule). */ public static final String ID = "<literal from step 1>";`
  directly above `getId()` (same javadoc wording as `MiscellaneousUnassignedFunction`).
- Change `getId()` to `return ID;` (keep the `@Override`).

The value is the literal from step 1, verbatim - never retype it.

- [ ] **Step 3: Grep gate - no bare literal remains in any taxonomy `getId()`**

```bash
cd /data/src/jipipe-4
# every getId() body in the taxonomy packages must be exactly 'return ID;'
grep -rn -A2 "public String getId()" jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/NoFramework.java | grep 'return "' 
```

Expected: **no output**. (Any line shown = a class still returning a literal - fix it.)

```bash
# and every taxonomy member class declares the constant
for d in functions frameworks; do
  n=$(grep -rln "implements JIPipeNodeTaxonomy" jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/$d | wc -l)
  m=$(grep -rln "public static final String ID" jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/$d | wc -l)
  echo "$d: classes=$n with-constant=$m"
done
```

Expected: `classes == with-constant` for both (functions 90/90, frameworks 16/16).

- [ ] **Step 4: Compile + suite (one known red expected)**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: `Tests run: 463, Failures: 1, Errors: 0, Skipped: 1` - the ONLY failure is
`TaxonomyPluginTest.allTaxonomyRegistered` ("expected: <89> but was: <90>", pre-existing red from the 90th function;
fixed in Task 4). No new failures.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks
git commit -m "refactor: freeze taxonomy function/framework ids as ID constants (stable-ID rule) (6.0.0)"
```

---

### Task 2: `JIPipeNodeTaxonomyServiceComponent` + wiring + resolver reroute + `TaxonomyPlugin`

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodeTaxonomyServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java` (field, constructor,
  `components[]`, getter)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java` (static accessor)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java` (two convenience forwarders)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/nodes/TaxonomyResolver.java` (ctor
  params, registry references)
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodesServiceComponent.java:53,57-60`
  (ctor + resolver creation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java` (`register()` loops)

**Interfaces:**

- Consumes: Task 1's `ID` constants are not needed here; existing `FunctionRegistry`/`FrameworkRegistry` public APIs.
- Produces (exact signatures - Tasks 3–4 build on these):
    - `void registerFunction(JIPipeNodeTaxonomyFunction function)` - idempotent by id (replace), invalidates the
      inference index (index added in Task 3; until then it is a plain registry call)
    - `JIPipeNodeTaxonomyFunction unregisterFunction(String id)`
    - `JIPipeNodeTaxonomyFunction functionById(String id)`
    - `Set<JIPipeNodeTaxonomyFunction> allFunctions()` / `topLevelFunctions()`
    - `List<JIPipeNodeTaxonomyFunction> childrenOf(JIPipeNodeTaxonomyFunction parent)`
    - `float baseHue(JIPipeNodeTaxonomyFunction function)` / `int shadeDepth(JIPipeNodeTaxonomyFunction function)`
    - `Set<String> validateFunctions()`
    - `void registerFramework(JIPipeNodeTaxonomyFramework framework)` / `frameworkById(String id)` /
      `Set<JIPipeNodeTaxonomyFramework> allFrameworks()`
    - `FunctionRegistry getFunctionRegistry()` / `FrameworkRegistry getFrameworkRegistry()`
    - `JIPipe.getNodeTaxonomyService()` static
    - `JIPipeService.getNodeTaxonomyService()` instance (with `ensureInitialized()`)
    - `JIPipeDefaultJavaPlugin.registerTaxonomyFunction(JIPipeNodeTaxonomyFunction)` /
      `registerTaxonomyFramework(JIPipeNodeTaxonomyFramework)`

- [ ] **Step 1: Create the component**

`JIPipeNodeTaxonomyServiceComponent extends JIPipeServiceComponent`, package
`org.hkijena.jipipe.api.service.components`. Owns
`private final FunctionRegistry functionRegistry = new FunctionRegistry();` and
`private final FrameworkRegistry frameworkRegistry = new FrameworkRegistry();`. Every listed method delegates to the
matching registry method (1:1 with `FunctionRegistry`/`FrameworkRegistry` - e.g. `childrenOf` →
`functionRegistry.children(parent)`, `validateFunctions` → `functionRegistry.validate()`, `baseHue`/`shadeDepth` →
registry instances). Javadoc: state that this is the canonical taxonomy registry (functions + frameworks), that plugins
register their own members through it, and that `getFunctionRegistry()`/`getFrameworkRegistry()` exist for
`TaxonomyValidationReport` and the nodes-service delegates. No inference code yet (Task 3).

- [ ] **Step 2: Wire into `JIPipeService` and `JIPipe`**

In `JIPipeService`:

- field `private final JIPipeNodeTaxonomyServiceComponent nodeTaxonomy;`
- in the constructor, `nodeTaxonomy = new JIPipeNodeTaxonomyServiceComponent(this);` - **before**
  `nodes = new JIPipeNodesServiceComponent(this);`
- add `nodeTaxonomy` to the `this.components = new JIPipeServiceComponent[] { ... }` array
- getter, same shape as `getNodes()`:

```java
public JIPipeNodeTaxonomyServiceComponent getNodeTaxonomyService() {
    ensureInitialized();
    return nodeTaxonomy;
}
```

In `JIPipe`:

```java
public static JIPipeNodeTaxonomyServiceComponent getNodeTaxonomyService() {
    return instance.getNodeTaxonomyService();
}
```

(place next to `getNodes()`).

- [ ] **Step 3: Add the convenience forwarders to `JIPipeDefaultJavaPlugin`**

```java
/**
 * Registers a node taxonomy function with the taxonomy service.
 *
 * @param function the function to register (its stable id is the key)
 */
public void registerTaxonomyFunction(JIPipeNodeTaxonomyFunction function) {
    service.getNodeTaxonomyService().registerFunction(function);
}

/**
 * Registers a node taxonomy framework with the taxonomy service.
 *
 * @param framework the framework to register (its stable id is the key)
 */
public void registerTaxonomyFramework(JIPipeNodeTaxonomyFramework framework) {
    service.getNodeTaxonomyService().registerFramework(framework);
}
```

(immediately after the `registerNodeType(JIPipeNodeRegistrationTask task)` forwarder; add the two taxonomy imports).

- [ ] **Step 4: Reroute `TaxonomyResolver` to the service-owned registries**

The writer (`TaxonomyPlugin`) and the readers (everything that goes through
the resolver's registries) must switch **in the same task** - splitting them
would leave the suite booting against an empty tree.

- Delete the two fields `functionRegistry`/`frameworkRegistry`.
- Add `private final JIPipeNodeTaxonomyServiceComponent taxonomyService;` and change both constructors:

```java
public TaxonomyResolver(JIPipeNodeTaxonomyServiceComponent taxonomyService) {
    this.taxonomyService = taxonomyService;
}

public TaxonomyResolver(JIPipeNodeTaxonomyServiceComponent taxonomyService, Map<Class<?>, List<String>> classToNodeIds) {
    this.taxonomyService = taxonomyService;
    this.classToNodeIds = classToNodeIds == null ? null : new LinkedHashMap<>(classToNodeIds);
}
```

- Replace every `functionRegistry`/`frameworkRegistry` reference with `taxonomyService.getFunctionRegistry()`/
  `taxonomyService.getFrameworkRegistry()` (the resolver's `getFunctionRegistry()`/`getFrameworkRegistry()` methods keep
  their signatures - they now return the service's registries).

- [ ] **Step 5: Update the resolver construction site**

`JIPipeNodesServiceComponent`: field becomes `private final TaxonomyResolver taxonomyResolver;`, constructor takes the
taxonomy component **as a parameter** (do NOT call `service.getNodeTaxonomyService()` here - the getter's
`ensureInitialized()` would re-entrantly boot the service in the middle of `JIPipeService`'s own constructor, where
state is still `Uninitialized`; that path is fatal):

```java
public JIPipeNodesServiceComponent(JIPipeService service, JIPipeNodeTaxonomyServiceComponent taxonomyService) {
    super(service);
    this.taxonomyResolver = new TaxonomyResolver(taxonomyService);
    this.defaultIconURL = JIPipe.RESOURCES.getIcon16URL("actions/configure.png");
}
```

and `JIPipeService`'s constructor calls `nodes = new JIPipeNodesServiceComponent(this, nodeTaxonomy);` (the taxonomy
field is initialized before `nodes`). Grep the whole repo (`new TaxonomyResolver`, `new JIPipeNodesServiceComponent`,
all modules incl. tests) for other construction sites; update each - found: only the one here, verify don't assume.

- [ ] **Step 6: Point `TaxonomyPlugin.register()` at the forwarders**

Replace `var functions = JIPipe.getNodes().getFunctions();` and `var frameworks = JIPipe.getNodes().getFrameworks();`
(they feed only the registration loops) and change both loops to `registerTaxonomyFunction(function);` /
`registerTaxonomyFramework(framework);`. `registerSystemTemplates()` still reads the LIVE registry - change its
`FunctionRegistry functions = JIPipe.getNodes().getFunctions();` to
`JIPipe.getNodeTaxonomyService().getFunctionRegistry();`.

- [ ] **Step 7: Compile + full core suite**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: `Tests run: 463, Failures: 1, Errors: 0, Skipped: 1` - the known-red count pin (`TaxonomyPluginTest`, 89 vs
90) is the ONLY failure. This step proves the joined path end-to-end: `TaxonomyPlugin` registers into the service's
registries and every reader (nodes-service delegates, `TaxonomyValidationReport`, validation tests) sees the populated
tree. If validation errors appear, the routing is broken - fix before committing.

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodeTaxonomyServiceComponent.java jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/nodes/TaxonomyResolver.java jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodesServiceComponent.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java
git commit -m "feat: JIPipeNodeTaxonomyServiceComponent - registries owned by the taxonomy service, registered via the plugin forwarder idiom (6.0.0)"
```

---

### Task 3: Weighted name inference on the functions (TDD)

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyInferencePattern.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/service/components/NodeTaxonomyInferenceTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyFunction.java` (default
  method)
- Modify: the 8 pattern-bearing functions - `SegmentationFunction`, `FilteringFunction`, `ThresholdingFunction`,
  `ResamplingFunction`, `PreprocessingFunction`, `MeasurementsFunction`, `MergingSplittingFunction`,
  `ColorStackFunction`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodeTaxonomyServiceComponent.java`
  (index + inference)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java` (set the fallback)
- Modify: `plugins/jipipe-plugin-ij2/src/main/java/org/hkijena/jipipe/plugins/imagej2/ImageJ2OpNodeInfo.java:213-219`
  and `plugins/jipipe-plugin-clij/src/main/java/org/hkijena/jipipe/plugins/clij2/CLIJCommandNodeInfo.java:287-292`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyAutoAssignment.java` (delete
  `functionForId` + `functionForName` + `functionForResource`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyAutoAssignmentTest.java` (retarget the
  8 name cases, delete the id/resource cases)

**Interfaces:**

- Consumes: Task 2's service (the live registry that `TaxonomyPlugin` populates) and Task 1's `ID` constants (fallback
  reference, test ids).
- Produces:
    - `record JIPipeNodeTaxonomyInferencePattern(String keyword, float weight)` in
      `org.hkijena.jipipe.api.nodes.taxonomy`
    - `default List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns()` on the function interface (empty)
    - `JIPipeNodeTaxonomyFunction inferFunction(String name)` on the service
    - `void setInferenceFallback(JIPipeNodeTaxonomyFunction fallback)` on the service (stores the id; resolved live)
    - `TaxonomyAutoAssignment` keeps exactly: `ResourceMapping`, `TagMapping`, `CuratedNodeTags`, `spatialFor`,
      `spatialForFirstImageInput`

**The scoring algorithm** (put it on the component as a **pure static** so it is testable without any boot - the
instance `inferFunction` builds the index and calls it):

```java
/** One keyword occurrence in the aggregate index. */
public record IndexedKeyword(String keyword, String functionId, float weight) {}

/**
 * Scores {@code name} (already lowercased with Locale.ROOT, or null) against
 * the aggregate keyword index.
 *
 * @param name         the display name, or null
 * @param index        every (keyword, functionId, weight) contribution
 * @param functionById live-id lookup (returns null for unregistered ids)
 * @return the highest-scoring REGISTERED function - ties break to the
 *         lexicographically smallest function id; unregistered ids are
 *         skipped (never thrown); no matched keyword -> null
 */
public static JIPipeNodeTaxonomyFunction scoreName(String name,
        List<IndexedKeyword> index,
        Function<String, JIPipeNodeTaxonomyFunction> functionById)
```

`IndexedKeyword` is a `public` static record nested in the component (the test references it). Scoring: iterate `index`;
for each entry whose `keyword` is a substring of `name`, accumulate `weight` into that `functionId`'s score (skip ids
`functionById` resolves to `null`); winner = max score, tie → smallest id; empty accumulation → `null`.

- [ ] **Step 1: Write the failing pure-static inference tests** - `NodeTaxonomyInferenceTest`

The scorer is a **pure static** on the component (no boot needed), so the first test group feeds it a hand-built index.
The id literals below are the real ones (Task 1 constants): `jipipe:function:segmentation`, `jipipe:function:filtering`,
`jipipe:function:color-stack`, `jipipe:function:merging-splitting`.

```java
package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.MiscellaneousFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-static tests of the taxonomy name-inference scorer: no JIPipe boot,
 * no live registry - the index and the id lookup are handed in, so these pin
 * the scoring rules (summation, tie-break, soft-fail) on their own.
 */
class NodeTaxonomyInferenceTest {

    private static final List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> INDEX = buildIndex();

    private static List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> buildIndex() {
        // a slice of the real aggregate index: enough to exercise every rule
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> list = new ArrayList<>();
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("segment", "jipipe:function:segmentation", 9f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("filter", "jipipe:function:filtering", 8f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("median", "jipipe:function:filtering", 1f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("color", "jipipe:function:color-stack", 2f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("stack", "jipipe:function:merging-splitting", 3f));
        return list;
    }

    private static JIPipeNodeTaxonomyFunction lookup(String id) {
        return switch (id) {
            case "jipipe:function:segmentation" -> new SegmentationFunction();
            case "jipipe:function:filtering" -> new FilteringFunction();
            case "jipipe:function:color-stack" -> new ColorStackFunction();
            case "jipipe:function:merging-splitting" -> new MergingSplittingFunction();
            case "jipipe:function:misc" -> new MiscellaneousFunction();
            default -> null; // unregistered id -> soft fail
        };
    }

    @Test
    void singleKeywordWins() {
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("segment cells", INDEX, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:segmentation", fn.getId());
    }

    @Test
    void summedWeightsOfOneFunctionBeatNothingElse() {
        // "median filter" matches median(1) + filter(8) = filtering 9; no other
        // keyword matches, so filtering wins on the summed score
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("median filter", INDEX, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:filtering", fn.getId());
    }

    @Test
    void noMatchYieldsNull() {
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("some totally unknown operation xyz", INDEX, lookup));
    }

    @Test
    void nullAndBlankNamesYieldNull() {
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName(null, INDEX, lookup));
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("   ", INDEX, lookup));
    }

    @Test
    void tieBreaksToLexicographicallySmallestId() {
        // "color stack" matches color->color-stack(2) and stack->merging-splitting(3)?
        // No: 3 > 2, so build an explicit tie instead:
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> tieIndex = List.of(
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("x", "jipipe:function:merging-splitting", 5f),
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("x", "jipipe:function:color-stack", 5f));
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("xx", tieIndex, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:color-stack", fn.getId(), "tie -> smallest id");
    }

    @Test
    void unregisteredFunctionInIndexIsSkippedNotThrown() {
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> ghostIndex = List.of(
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("segment", "jipipe:function:ghost", 9f));
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("segment", ghostIndex, lookup));
    }
}
```

Run:
`mvn -o -pl jipipe-core test -Dtest=NodeTaxonomyInferenceTest 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run:|BUILD|ERROR" | head -5`
Expected: FAIL (compile error - `scoreName`/`IndexedKeyword` missing).

- [ ] **Step 2: Implement the record + static scorer**

`JIPipeNodeTaxonomyInferencePattern` (top-level record, javadoc: keyword is matched case-insensitively as a substring;
weight > 0; the aggregate score is the sum of matched weights). `IndexedKeyword` + `scoreName` on the component, exactly
per the algorithm above. Re-run: Expected: PASS.

- [ ] **Step 3: Declare the patterns (exact transcription of the spec table)**

Interface default on `JIPipeNodeTaxonomyFunction` (javadoc per spec §4):

```java
default List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns() {
    return List.of();
}
```

Each of the 8 functions overrides it with one `new JIPipeNodeTaxonomyInferencePattern("<keyword>", <weight>)` per
keyword, **verbatim** from the spec §4 weight table: segmentation `segment`=9; filtering `filter`=8 plus
`blur, smooth, erode, dilate, morpholog, gaussian, median`=1 each; thresholding `threshold`=7; resampling
`resample, interpolat, resize, rescale, upscale, downscale`=6 each; preprocessing
`inpaint, restore, denoise, deconvolv, preprocess, background, flatten`=5 each; measurements `measure, analyz, profile`
=4 each; merging-splitting `merge, split, stack, concatenat, combine, montage`=3 each; color-stack
`color, rgb, hsl, lut, colormap`=2 each. (`MiscellaneousUnassignedFunction` declares **no** patterns - it is the
fallback, not an inference target.)

- [ ] **Step 4: Write the failing instance-level tests** (same file, second test group)

```java
// fresh component, no boot: new JIPipeNodeTaxonomyServiceComponent(null) is
// legal for pure registry tests (the service reference is only used for
// progress logging, which these paths do not touch - verify while
// implementing; if anything touches it, boot with JIPipe.ensureInstance()
// and use the live service instead)
// (a) parity corpus - the EXACT names of the retired functionForName tests,
//     registered on the fresh component (the 8 pattern functions +
//     MiscellaneousFunction + MiscellaneousUnassignedFunction, flat -
//     childrenOf/validation are not exercised here):
//     "3D Gaussian filter" -> FilteringFunction
//     "Median filter"      -> FilteringFunction
//     "Segment cells"      -> SegmentationFunction
//     "Resample"           -> ResamplingFunction
//     "Color convert"      -> ColorStackFunction
// (b) THE BEHAVIOR CHANGE - after setInferenceFallback(new
//     MiscellaneousUnassignedFunction()):
//     "Some totally unknown operation xyz" -> MiscellaneousUnassignedFunction
//     "Watershed"                          -> MiscellaneousUnassignedFunction
//     null                                 -> MiscellaneousUnassignedFunction
//     and: with the fallback set but its function NOT registered,
//     inferFunction(unknownName) returns null (no exception)
// (c) extensibility - a "second plugin" (test code) registers a new function
//     class (test-local, patterns e.g. "clustery"=9) on the SAME fresh
//     component -> inferFunction("clustery thing") resolves it AND
//     functionById resolves it; after unregisterFunction, inference of that
//     name yields the fallback again (index rebuilt, no stale contribution)
```

Run:
`mvn -o -pl jipipe-core test -Dtest=NodeTaxonomyInferenceTest 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run:|BUILD|ERROR" | head -5`
Expected: FAIL (`inferFunction`/`setInferenceFallback` missing).

- [ ] **Step 5: Add the index + instance methods to the component**

- `setInferenceFallback`: store `fallback.getId()` (store `null` when passed `null`).
- index: `List<IndexedKeyword> keywordIndex` (cached) + `volatile boolean indexDirty`; `registerFunction`/
  `unregisterFunction` mark dirty; a private `ensureIndex()` rebuilds flat: for every registered function, every
  `getInferencePatterns()` entry →
  `new IndexedKeyword(pattern.keyword().toLowerCase(Locale.ROOT), function.getId(), pattern.weight())`.
- `inferFunction(String name)`:

```java
String lower = name == null ? null : name.toLowerCase(Locale.ROOT);
JIPipeNodeTaxonomyFunction winner = lower == null || lower.isBlank()
        ? null
        : scoreName(lower, ensureIndex(), this::functionById);
if (winner != null) {
    return winner;
}
return fallbackFunctionId == null ? null : functionById(fallbackFunctionId);
```

Run both test groups in `NodeTaxonomyInferenceTest`: Expected: PASS.

- [ ] **Step 6: Migrate IJ2 + CLIJ callers**

`ImageJ2OpNodeInfo.getPrimaryFunction()` (lines 213-219) and the identical block in `CLIJCommandNodeInfo`:

```java
// was: JIPipeNodeTaxonomyFunction function = TaxonomyAutoAssignment.functionForId(mapped);
JIPipeNodeTaxonomyFunction function = JIPipe.getNodeTaxonomyService().functionById(mapped);
...
// was: return TaxonomyAutoAssignment.functionForName(getName());
return JIPipe.getNodeTaxonomyService().inferFunction(getName());
```

Keep both fall-throughs exactly as they are (null → next step / null return). Add imports; the methods stay inside their
existing try/catch-or-lazy shape (do not add new defensive wrapping - `inferFunction`/`functionById` never throw).

- [ ] **Step 7: Set the fallback in `TaxonomyPlugin.register()`**

After the framework registrations: `setInferenceFallback(new MiscellaneousUnassignedFunction());` (via `this` - the
plugin extends `JIPipeDefaultJavaPlugin`, so also add a `setInferenceFallback`-shaped convenience? **No** - the fallback
is core-policy, not a per-plugin registration idiom; call `JIPipe.getNodeTaxonomyService().setInferenceFallback(...)`
directly here and nowhere else).

- [ ] **Step 8: Retire the old inference**

- In `TaxonomyAutoAssignmentTest`: retarget the eight `functionForName` test cases to `inferFunction` on a booted
  instance (`JIPipe.ensureInstance()` in `@BeforeAll` - the launcher listener pre-patches ImageJ, safe): the five
  positive names ("3D Gaussian filter", "Median filter", "Segment cells", "Resample", "Color convert" → their classes)
  keep their expectations; the three fallback cases (`unknownNameFallsBackToMiscellaneousWithoutThrowing`,
  `nameNotInTableFallsBackToMiscellaneous`, `nullNameFallsBackToMiscellaneousWithoutThrowing`) now expect
  `MiscellaneousUnassignedFunction.class` (rename them to say *unassigned*, not *miscellaneous*). Delete the
  `functionForIdInstantiatesTheAuditLeaves` and `resourceMappingWinsOverTheNameHeuristic` test cases (both methods are
  gone - the live-registry equivalents are covered by `NodeTaxonomyInferenceTest` (c) and Task 4's presence test).
- Delete `functionForId`, `functionForName`, and `functionForResource` from `TaxonomyAutoAssignment` + their now-unused
  imports (keep `MiscellaneousFunction` only if still referenced by remaining javadoc/code - it is not after this step,
  remove the import too).
- Compile all modules: `mvn -o clean compile -q 2>&1 | grep -E "ERROR" | head -5; echo "exit: ${PIPESTATUS[0]}"` -
  Expected: no output, exit 0 (this is the gate that IJ2/CLIJ were migrated and no caller was missed).

- [ ] **Step 9: Full core suite**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: only the known-red `TaxonomyPluginTest` count pin fails; inference + parity tests pass.

- [ ] **Step 10: Commit**

```bash
git add -A jipipe-core/src plugins/jipipe-plugin-ij2/src plugins/jipipe-plugin-clij/src
git commit -m "feat: weighted name inference on taxonomy functions via the taxonomy service; fallback = Miscellaneous unassigned (6.0.0)"
```

---

### Task 4: Presence-based `TaxonomyPluginTest` (fixes the known red) + ID-stability gate

**Files:**

- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyPluginTest.java` (rewrite
  `allTaxonomyRegistered`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyResolverTest.java` (if it constructs
  the resolver - verify and update the constructor)

**Interfaces:**

- Consumes: Task 1's `ID` constants, Task 2's service (incl. the resolver reroute that made it the live tree).
- Produces: a green core suite (462+/0/0/1 minus nothing); the expected-IDs list (90 functions + 16 frameworks) is the
  ID-stability pin.

- [ ] **Step 1: Generate the expected-IDs list**

```bash
cd /data/src/jipipe-4
python3 - <<'EOF'
import re, pathlib
base = pathlib.Path("jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy")
ids = []
for d in ("functions", "frameworks"):
    for p in sorted(base.glob(d + "/**/*.java")):
        m = re.search(r'public static final String ID\s*=\s*"([^"]+)"', p.read_text())
        if m: ids.append(m.group(1))
print(f"{len(ids)} ids")
print("\n".join('        "' + i + '",' for i in ids))
EOF
```

Expected: 106 lines (90 + 16). Paste into the test as two `List<String>` constants: `EXPECTED_FUNCTION_IDS` and
`EXPECTED_FRAMEWORK_IDS` (split at the 90th entry - the functions list is alphabetical by file path; the first 90 lines
are functions).

- [ ] **Step 2: Rewrite `allTaxonomyRegistered`**

```java
@Test
void allCoreTaxonomyRegistered() {
    // Presence, not counts: plugins may legitimately register more (AGENTS.md
    // plugin-aware test ruling). Every expected core id must be present; the
    // list itself pins the stable literals (ID-stability gate).
    var taxonomy = JIPipe.getNodeTaxonomyService();
    assertTrue(taxonomy.allFunctions().size() >= EXPECTED_FUNCTION_IDS.size(),
            "at least the " + EXPECTED_FUNCTION_IDS.size() + " core functions");
    for (String id : EXPECTED_FUNCTION_IDS) {
        assertNotNull(taxonomy.functionById(id), "function " + id);
        assertEquals(id, taxonomy.functionById(id).getId(), "getId drift for " + id);
    }
    for (String id : EXPECTED_FRAMEWORK_IDS) {
        assertNotNull(taxonomy.frameworkById(id), "framework " + id);
        assertEquals(id, taxonomy.frameworkById(id).getId(), "getId drift for " + id);
    }
    assertTrue(taxonomy.validateFunctions().isEmpty(),
            "function tree errors: " + taxonomy.validateFunctions());
    // the inference fallback is the unassigned sink (Task 3 behavior change)
    assertEquals(MiscellaneousUnassignedFunction.ID,
            taxonomy.inferFunction("zzz no keywords here at all").getId());
}
```

Delete the old `assertEquals(89, ...)` and the old family assertion on the plugin; keep/adjust the rest of the file per
what it contained. If `TaxonomyResolverTest` constructs `new TaxonomyResolver(...)`, update to the Task 2 constructor.

- [ ] **Step 3: Full suite - must be GREEN for the first time since Task 1**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: `Tests run: 462, Failures: 0, Errors: 0, Skipped: 1` (or 463 if the test-class count arithmetic differs by
one - the contract is **0 failures**), then the launcher:
`mvn -o -pl jipipe-launcher clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]+,|BUILD"` Expected:
`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/test
git commit -m "test: plugin-aware presence taxonomy test (no exact-count pins) + ID-stability gate (6.0.0)"
```

---

### Task 5: Interface cleanup - delete the dead registration-contract methods

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyFunction.java`
- (contingency) any function class overriding the two methods - verify first.

**Interfaces:**

- Consumes: Tasks 1–5 (suite green baseline).
- Produces: the final `JIPipeNodeTaxonomyFunction` - id, name, parent, hue, description, icon, tags,
  `isVisibleInPipeline`, `isVisibleInCompartments`, `canExtract`, `isRunnable`, `getInferencePatterns`.

- [ ] **Step 1: Verify nothing overrides the dead methods**

```bash
cd /data/src/jipipe-4
grep -rln "userCanCreate\|userCanDelete" jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/
```

Expected: **no output**. If any class overrides them, delete those overrides too in step 2.

- [ ] **Step 2: Delete `userCanCreate()` and `userCanDelete()` from the interface**

Remove both default methods + their "carried over from the node registration contract" javadoc. Keep `canExtract()`/
`isRunnable()` (live via `TaxonomyView.behaviorFlags`) - if the same "carried over" javadoc applies, reword it to: "Node
behavior flag read by the UI and graph operations via `TaxonomyView.behaviorFlags`."

- [ ] **Step 3: Compile + full suite (both modules)**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"` and
`mvn -o -pl jipipe-launcher clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]+,|BUILD"`
Expected: core 0 failures; launcher 19/0/0/0.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/
git commit -m "refactor: drop dead userCanCreate/userCanDelete from JIPipeNodeTaxonomyFunction (6.0.0)"
```

---

## Self-review notes (run by the plan author)

- **Spec coverage:** §1 component+API → Task 2 (inference index/fallback → Task 3); §2 plugin-idiom registration → Task
  2 Step 3 (forwarders) + Step 6 (`TaxonomyPlugin` loops); §3 resolver integration → Task 2 Steps 4–5 (merged so writer
  and reader switch atomically); §4 inference (record, patterns table, scoring, fallback, IJ2/CLIJ, deletions) → Task 3;
  §5 ID constants + interface cleanup → Task 1 + Task 5; §6 tests (parity, extensibility, presence, ID-stability) → Task
  3 + Task 4. Migration order → task order 1→5. No gaps.
- **Type consistency:** `IndexedKeyword`/`scoreName` defined in Task 3 Step 2 and used only there + by `inferFunction`;
  `registerFunction`/`functionById`/`validateFunctions`/`setInferenceFallback` names are identical in Tasks 2, 3, 4; the
  `ID` constant name used in Task 4's expected-IDs list matches Task 1.
- **Ordering hazard:** the plan never requires taxonomy to be registered before IJ2/CLIJ run - Tasks 2–3 keep the
  soft-null paths (`functionById`→null, `inferFunction`→null) and Task 2 Step 7's full boot is the proof the joined
  reader/writer path is populated.
- **Known-red bookkeeping:** exactly one failing test (`TaxonomyPluginTest` 89-vs-90) exists from Task 1 Step 4 through
  Task 3 Step 9; Task 4 Step 3 is the first all-green gate. Every intermediate suite step states the expected failure
  explicitly so an executor never "fixes" the pin by reverting work.
