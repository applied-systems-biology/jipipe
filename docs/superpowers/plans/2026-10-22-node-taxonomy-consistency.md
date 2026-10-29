# Node Taxonomy Consistency — Frameworks + Research-Anchored Hues — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the taxonomy content coherent — 5 new frameworks for coherent suites (Weka/Filaments/IJ3D/OMERO/Scene3D),
delete the hallucinated CometFramework, reassign ~168 node annotations, and replace the hand-assigned function hues with
a deterministic auto-pick over 6 research-anchored hue categories (focal hues red/orange/yellow/green/blue/purple).

**Architecture:** Part A is pure data: new framework classes following the `IlastikFramework` shape +
`@ConfigureJIPipeNode(framework=…)` rewrites (frameworks are derived at registration, never persisted per node). Part B
adds a closed `HueCategory` enum (6 categories at the focal hues from color-categorization research) + a pure
`TaxonomyHue.autoHue(category, id)` (FNV-1a → 24 slots → golden-angle spread inside the category's Voronoi cell), turns
`getColorHue()` into a default method, migrates the 13 roots (4 historic pins keep explicit hues, 6 drop into their
family cells, Misc hard-coded 50°) and promotes `ScriptingFunction` to top-level under Intelligence.

**Tech Stack:** Java 21, Maven multi-module (27 modules), JUnit 5 (jipipe-core + jipipe-launcher tests).

**Spec:** `docs/superpowers/specs/2026-10-22-node-taxonomy-consistency-design.md` — the plan argues from the spec; read
both.

## Global Constraints

- **Stable-ID rule (AGENTS.md):** framework IDs are frozen string literals — the 5 new IDs are the exact spec A.2 table
  values; NO existing ID changes anywhere.
- **Framework + hue are never persisted per node/project** (verified in spec): re-annotation and hue moves cannot break
  saved projects. The only persistence-sensitive artifact is the *ship set* (framework classes registered by
  `TaxonomyPlugin`).
- **No CoreFramework mass-tagging** (user ruling): the core-infra plugins (tables, filesystem, annotations, utils,
  strings, forms) keep no framework attribute.
- **JIPipeDLFramework stays registered** but ends with zero node references (it is the unmerged Dhruv-Kathri project's
  home).
- **Ship-set test vs live-registry test (AGENTS.md):** `FrameworksTest` pins the core ship set — exact counts there are
  sanctioned (single owner, one generator). `TaxonomyPluginTest` stays presence-based — its `EXPECTED_FRAMEWORK_IDS`
  list updates 16→20, still no live-count pin.
- **Test policy (AGENTS.md):** no Swing tests; no trivial tests. Hue tests pin the algorithm's invariants (determinism,
  cell containment, tiling, spread) — not rendering.
- **Hue values are normalized 0..1** (the codebase convention `deg/360.0f`); `TaxonomyHue.autoHue` returns normalized
  floats.
- **Commits:** one per task, message suffix `(6.0.0)`. After any launcher-suite run:
  `git checkout -- utils/taxonomy-research/registry.json utils/fresh-node-slots.json` (known dumper drift, per
  AGENTS.md) — never commit those files.
- **Suite commands:** core = `mvn -o -pl jipipe-core clean test`; launcher = `mvn -o -pl jipipe-launcher clean test`;
  whole-repo compile = `mvn -o clean compile -q`. Baseline: core 478/0/0/1, launcher 19/0/0/0. The contract at every
  green step is **0 failures / 0 errors**, not an exact test count.

## Review Focus

- **Two auto-picked top-level hues collide in a dense category** (a plugin adds many branches to one category) →
  `validate()` reports "Duplicate top-level color hue" at boot, fail-closed; the fix is an explicit `getColorHue()`
  override. Pinned: Task 3 Step 1 test `duplicateHueIsReportedByValidation` (two test-local top-level functions forced
  to the same explicit hue → `FunctionRegistry.validate()` names the error).
- **Explicit hue + category both set on one function** → the explicit hue silently wins (documented default, surprise
  risk). Pinned: Task 3 Step 1 test `explicitHueBeatsCategory`.
- **A leaf whose root chain has no hue** (e.g. after the Scripting promotion if the chain were mis-wired) → `baseHue`
  throws `IllegalStateException`; `ThemeUtils` degrades to a neutral fallback (existing contract). Pinned: Task 4 Step 4
  test `scriptingLeavesInheritTheAutoPickedHue` (baseHue resolves for all 4 scripting leaves without exception and
  equals the Scripting root's hue).
- **Saved projects that reference re-annotated nodes** → load unchanged (framework derives from the class annotation,
  not the JSON). Pinned: Task 2 Step 4 diff-scope gate — the commit touches ONLY `@ConfigureJIPipeNode` annotation
  values + import lines in the 7 plugins, no node IDs, no other code.
- **Framework abbreviation uniqueness across the 20-framework ship set** (WEKA/FILA/3D/OMRO/S3D vs the 15 existing) →
  `FrameworksTest`'s existing uniqueness assertion covers the enlarged set. Pinned: Task 1 Step 5 (the test's
  abbreviation-uniqueness block runs over all 20).

---

### Task 1: Framework ship set — 5 new classes, Comet deleted, registrations + tests

**Files:**

- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/{WekaFramework,FilamentsFramework,IJ3DFramework,OmeroFramework,Scene3DFramework}.java`
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/CometFramework.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java` (registration block ~
  336-351 + imports)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/FrameworksTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyPluginTest.java` (the
  `EXPECTED_FRAMEWORK_IDS` list)

**Interfaces:**

- Consumes: the `IlastikFramework` shape (final class, frozen `ID` literal,
  `getId/getName/getAbbreviation/getPatternStyleId`, default `getColor` from the interface PALETTE).
- Produces: the 20-framework ship set; class names + exact metadata per spec A.2 (Weka/`WEKA`/`cellular`, Filaments/
  `FILA`/`flow`, IJ3D/`3D`/`hilbert`, OMERO/`OMRO`/`stripes`, Scene 3D/`S3D`/`landscape`). Task 2 imports these classes.

- [ ] **Step 1: Create the 5 framework classes**

Each mirrors `IlastikFramework` exactly (same javadoc style — one-line tool description; same constant javadoc).
IDs/names/abbreviations/styles from the spec A.2 table verbatim. Javadoc first lines: Weka = "The Weka machine-learning
toolkit (wrapped via the ImageJ Trainable Weka Filter)."; Filaments = "The JIPipe-exclusive filament skeleton processing
suite."; IJ3D = "The ImageJ 3D Suite (3D ROIs, 3D viewer, 3D filters)."; OMERO = "The OMERO data platform (remote
image/table repositories)."; Scene 3D = "The JIPipe-exclusive 3D scene / mesh suite."

- [ ] **Step 2: Delete CometFramework + its registration**

Delete the class file. In `TaxonomyPlugin`: remove the `import …CometFramework;` and the
`registerTaxonomyFramework(new CometFramework());` line; add the 5 new imports + 5
`registerTaxonomyFramework(new …Framework());` calls next to the existing block.

- [ ] **Step 3: Update `FrameworksTest`**

- `allFrameworks()`: remove `CometFramework.class`, add the 5 new classes → 20 entries.
- The three exact-size pins: `assertEquals(16, …)` for ids (~line 85), abbreviations (~line 158),
  `registry.all().size()` (~line 193) → `20`.
- Per-framework asserts block (~lines 109-121): delete the two Comet lines; add for each new framework
  `assertEquals("<Name>", new X().getName());` + `assertEquals("<style>", new X().getPatternStyleId());` per the spec
  table (Weka name `Weka`, style `cellular`; Filaments `Filaments`/`flow`; IJ3D `IJ3D`/`hilbert`; OMERO `OMERO`/
  `stripes`; Scene 3D `Scene 3D`/`landscape`).
- Class javadoc "16 seed frameworks" → reword to the core ship set (20).
- If the test asserts a per-id color, derive the 5 new entries with the same `PALETTE[floorMod(id.hashCode(), 12)]`
  default the others use.

- [ ] **Step 4: Update `TaxonomyPluginTest.EXPECTED_FRAMEWORK_IDS`**

Remove `"jipipe:framework:comet"`, add the 5 new IDs → 20 literals. Touch nothing else in that test (the function list
stays 90).

- [ ] **Step 5: Run the core suite — the abbreviation-uniqueness pin runs over all 20**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: **0 failures, 0 errors** (BUILD SUCCESS).

- [ ] **Step 6: Commit**

```bash
git add -A jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java jipipe-core/src/test
git commit -m "feat: Weka/Filaments/IJ3D/OMERO/Scene3D frameworks; delete hallucinated CometFramework (6.0.0)"
```

---

### Task 2: Node re-annotations (~168 files across 7 plugins)

**Files:** (annotation + import edits only — the diff-scope gate in Step 4 enforces this)

- `plugins/jipipe-plugin-ilastik/src/main/**` — 8 files
- `plugins/jipipe-plugin-omnipose/src/main/**` — 5 files
- `plugins/jipipe-plugin-ij-weka/src/main/**` — 9 files
- `plugins/jipipe-plugin-ij-filaments/src/main/**` — 51 files
- `plugins/jipipe-plugin-ij-3d/src/main/**` — 62 files
- `plugins/jipipe-plugin-ij-omero/src/main/**` — 26 files (framework attribute ADDED)
- `plugins/jipipe-plugin-scene-3d/src/main/**` — 7 files (framework attribute ADDED)

**Interfaces:**

- Consumes: Task 1's five new framework classes + the existing `IlastikFramework`/`OmniposeFramework`.
- Produces: zero `JIPipeDLFramework` references in any node annotation; every re-annotated node compiles (whole-repo
  compile is the gate).

- [ ] **Step 1: Re-annotate the 5 swap groups**

For each group, list the files (`grep -rl "framework = <Old>" plugins/<plugin>/src/main`), then in every file: change
the annotation value and fix imports (add the new framework import; remove the old one only if no other reference
remains in the file):

| Plugin                     | From                      | To                         |
|----------------------------|---------------------------|----------------------------|
| jipipe-plugin-ilastik      | `JIPipeDLFramework.class` | `IlastikFramework.class`   |
| jipipe-plugin-omnipose     | `JIPipeDLFramework.class` | `OmniposeFramework.class`  |
| jipipe-plugin-ij-weka      | `JIPipeDLFramework.class` | `WekaFramework.class`      |
| jipipe-plugin-ij-filaments | `ImageJ1Framework.class`  | `FilamentsFramework.class` |
| jipipe-plugin-ij-3d        | `ImageJ1Framework.class`  | `IJ3DFramework.class`      |

(If a file's `ImageJ1Framework` import is still used by another annotation in the same file, keep the import — expected:
none, the swap is whole-file.)

- [ ] **Step 2: Add the framework attribute to omero + scene-3d**

Every file in `plugins/jipipe-plugin-ij-omero/src/main` and `plugins/jipipe-plugin-scene-3d/src/main` carrying
`@ConfigureJIPipeNode(...)` gets `, framework = OmeroFramework.class` / `, framework = Scene3DFramework.class` added to
the annotation (attribute position per the annotation's existing style — after `function =`), plus the import. Gate:
file-count parity — the number of files containing `@ConfigureJIPipeNode` equals the number containing `framework = ` in
that plugin (verify with two greps per plugin; expected 26 and 7).

- [ ] **Step 3: Whole-repo compile (catches every import mistake)**

Run: `mvn -o clean compile -q 2>&1 | grep -E "ERROR" | head -5; echo "exit: ${PIPESTATUS[0]}"`
Expected: no output, exit 0.

- [ ] **Step 4: Diff-scope gate (saved-project safety)**

Run: `git diff --stat` then inspect the changed-file list — every changed file must be under the 7 plugins' `src/main`,
and within each file only `@ConfigureJIPipeNode` annotation lines + `import` lines may differ. Any other hunk (a node
ID, a method body) is a defect — fix before committing.

- [ ] **Step 5: Grep gates**

```bash
grep -rl "framework = JIPipeDLFramework" --include=*.java jipipe-core/src plugins/ contrib/   # expect: no output
grep -rl "CometFramework" --include=*.java jipipe-core/src plugins/ contrib/                   # expect: no output
```

- [ ] **Step 6: Core suite + commit**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"` → **0
failures**.

```bash
git add plugins/
git commit -m "fix: framework annotations — ilastik/omnipose/weka off JIPipeDL; filaments/ij3d off ImageJ1; omero/scene-3d now tracked (6.0.0)"
```

---

### Task 3: `HueCategory` + `TaxonomyHue` + interface defaults (TDD)

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/HueCategory.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyHue.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyHueTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/JIPipeNodeTaxonomyFunction.java` (two default
  methods + interface javadoc)

**Interfaces:**

- Consumes: nothing from Tasks 1–2 (independent).
- Produces (exact — Task 4 builds on these):
    -
    `enum HueCategory { GEOMETRY("Geometry", 0f), ANALYSIS("Analysis", 30f), VISUALIZATION("Visualization", 60f), EXCHANGE("Exchange", 120f), DATA("Data", 240f), INTELLIGENCE("Intelligence", 300f) }`
    with a `float anchorDeg` field (deg, 0..360)
    - **Voronoi cell formula (implementation AND test must use exactly this):** sort the enum anchors `a₀…a₅` ascending;
      for anchor `aᵢ` with next-anchor gap `gᵢ = (aᵢ₊₁ − aᵢ) mod 360` and previous-anchor gap
      `gᵢ₋₁ = (aᵢ − aᵢ₋₁) mod 360`: cell = `[aᵢ − gᵢ₋₁/2, aᵢ + gᵢ/2)` (wrap-aware). Result: GEOMETRY [330,15),
      ANALYSIS [15,45), VISUALIZATION [45,90), EXCHANGE [90,180), DATA [180,270), INTELLIGENCE [270,330) — the test
      recomputes this from the enum anchors (independent copy of the formula, not the implementation helper) for its
      containment + tiling assertions.
    - `static Float TaxonomyHue.autoHue(HueCategory category, String stableId)` — normalized 0..1, per the spec B.3
      algorithm (FNV-1a 64, `SLOTS = 24`, golden fraction `0.381966011250105f`, cell from the formula above,
      wrap-normalized)
    - `static` cell helper on `TaxonomyHue` (e.g. `cellStartDeg(HueCategory)` / `cellWidthDeg(HueCategory)`) so the
      implementation has one cell definition; the test still keeps its independent recomputation
    - `default HueCategory getHueCategory() { return null; }` (null = transparent, no auto-pick)
    -
    `default Float getColorHue() { if (getParent() != null || getHueCategory() == null) return null; return TaxonomyHue.autoHue(getHueCategory(), getId()); }`

- [ ] **Step 1: Write the failing test class** — `TaxonomyHueTest`

```java
// Test-local minimal functions (implement the interface's abstract methods;
// the two new defaults are left to the interface).
// (1) determinism: autoHue(GEOMETRY, "jipipe:function:x") called twice →
//     equal; autoHue(GEOMETRY, "a") != autoHue(GEOMETRY, "b")
// (2) cell containment: for EVERY category and a corpus of ~10 varied ids,
//     autoHue(category, id) lies inside the category's Voronoi cell —
//     cell computed in the test from the enum's anchors (midpoint to
//     midpoint, wrap-aware), NOT hardcoded angles
// (3) tiling: the 6 cells are pairwise disjoint and their total span is 360°
// (4) golden spread: for a fixed 8-id corpus in one category, all hues
//     pairwise distinct
// (5) explicitHueBeatsCategory: test-local top-level with BOTH an explicit
//     getColorHue() (0.99f) and getHueCategory() → getColorHue() == 0.99f
//     (i.e. the explicit override wins — the default is not called)
// (6) noCategoryNoHue: test-local top-level, no category, no explicit hue
//     → getColorHue() == null (transparent); test-local LEAF with a category
//     → getColorHue() == null (leaves always inherit)
// (7) duplicateHueIsReportedByValidation: a fresh FunctionRegistry with two
//     top-level test-local functions sharing one explicit hue → validate()
//     returns an error mentioning "Duplicate top-level color hue"
```

Run:
`mvn -o -pl jipipe-core test -Dtest=TaxonomyHueTest 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run:|BUILD|ERROR" | head -5`
Expected: FAIL (compile error — `HueCategory`/`TaxonomyHue` missing).

- [ ] **Step 2: Implement `HueCategory` + `TaxonomyHue` + the interface defaults**

Per the Produces block (anchors, algorithm, defaults — all fixed by the spec B.2/B.3/B.4; the enum carries the labels as
in the table). Interface javadoc: reword the `getColorHue`/hue paragraphs to describe the default path (top-level +
category → deterministic in-cell hue; no category → transparent; explicit override wins).

Re-run: Expected: PASS (all 7 groups).

- [ ] **Step 3: Full core suite — nothing should have moved**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: **0 failures** (every existing root still has an explicit `getColorHue()`; every leaf's explicit
`return null;` equals the new default → behavior identical).

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/ jipipe-core/src/test
git commit -m "feat: deterministic hue auto-picking — HueCategory (6 research anchors) + TaxonomyHue + getColorHue default (6.0.0)"
```

---

### Task 4: Function-class migration — categories, hue drops, Scripting promotion, leaf cleanup

**Files:**

- Modify: the 13 top-level function classes per the spec B.5 table (`RoisFunction`, `FilamentsFunction`,
  `StatisticsFunction`, `TrackingFunction`, `PlottingFunction`, `DataSourcesFunction`, `ExportFunction`,
  `ImagesFunction`, `TablesFunction`, `MachineLearningFunction`, `AnnotationsFunction`, `MiscellaneousFunction`,
  `GraphFunction`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/ScriptingFunction.java` (promoted
  to top-level)
- Modify: the 77 leaf function classes (delete their `getColorHue` overrides) — the work list is generated, not guessed:

```bash
cd /data/src/jipipe-4
# leaves = getColorHue body is exactly 'return null;' AND parent is non-null
python3 - <<'EOF'
import re, pathlib
base = pathlib.Path("jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions")
for p in sorted(base.rglob("*.java")):
    s = p.read_text()
    m = re.search(r"public Float getColorHue\(\)\s*\{\s*return null;\s*\}", s)
    par = re.search(r"getParent\(\)\s*\{\s*return (null|new )", s)
    if m and par and par.group(1) != "null":
        print(p.relative_to(base.parent))
EOF
```

- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyHueTest.java` (add the ship-set
  integration tests)

**Interfaces:**

- Consumes: Task 3's `HueCategory`/`TaxonomyHue`/defaults.
- Produces: the final hue layout — the 4 historic pins explicit (Rois 0°, DataSources 136°, Images 186°, Tables 216°,
  Annotations 264°, Misc 50°), the 6 family auto-picks (Filaments→GEOMETRY, Statistics+Tracking→ANALYSIS,
  Plotting→VISUALIZATION, Export→EXCHANGE, MachineLearning→INTELLIGENCE), Scripting top-level with INTELLIGENCE
  auto-pick, Graph transparent.

- [ ] **Step 1: Migrate the 13 roots per the spec B.5 table**

- Keep-explicit group (add `getHueCategory()` override only): RoisFunction `GEOMETRY` (keep 0°), DataSourcesFunction
  `EXCHANGE` (keep 136°), ImagesFunction `DATA` (keep 186°), TablesFunction `DATA` (keep 216°), AnnotationsFunction
  `INTELLIGENCE` (keep 264°).
- MiscellaneousFunction: keep the explicit 50°; NO category (default null) — user ruling.
- GraphFunction: keep explicit null (transparent); NO category.
- Drop-explicit group (DELETE the `getColorHue` method entirely — the default takes over — and add the
  `getHueCategory()` override): FilamentsFunction `GEOMETRY`, StatisticsFunction `ANALYSIS`, TrackingFunction
  `ANALYSIS`, PlottingFunction `VISUALIZATION`, ExportFunction `EXCHANGE`, MachineLearningFunction `INTELLIGENCE`.
- Update each touched class's class-javadoc where it describes its hue placement (e.g. Filaments "Leaf of…" wording is
  stale for roots that move; keep descriptions factual).

- [ ] **Step 2: Promote `ScriptingFunction` to top-level**

`getParent()` → `return null;`; class javadoc: now a top-level branch of the Intelligence family (first in-tree user of
the auto-picker); add `getHueCategory() { return INTELLIGENCE; }`; delete its `getColorHue` (null) override. The 4
scripting leaves (`scripting/Scripting{Expression,ImageJMacro,Python,R}Function`) already parent to Scripting — no
change to them.

- [ ] **Step 3: Delete the 77 leaf overrides**

For every file in the generated work list: remove the `getColorHue()` method (its `@Nullable` annotation and javadoc, if
any). The work list must print exactly 77 files — if it doesn't, stop and report (a miscount means the regex or the tree
changed).

- [ ] **Step 4: Add the ship-set integration tests to `TaxonomyHueTest`**

```java
// (8) scriptingLeavesInheritTheAutoPickedHue: on a fresh FunctionRegistry
//     holding ScriptingFunction + its 4 leaves: baseHue of every leaf ==
//     baseHue(ScriptingFunction), no exception; ScriptingFunction is
//     top-level (getParent() == null)
// (9) shipSetAutoPicksAreDistinct: on the booted live service
//     (JIPipe.ensureInstance() in @BeforeAll — the launcher listener
//     pre-patches ImageJ, safe), collect the hues of every TOP-LEVEL
//     function that relies on the default — detection: getHueCategory()
//     != null AND the class is NOT in the keep-explicit set
//     {RoisFunction, DataSourcesFunction, ImagesFunction, TablesFunction,
//      AnnotationsFunction} (spec B.5 "keeps explicit" group; Miscellaneous
//     and Graph have null category and are excluded automatically) —
//     expected: exactly {Filaments, Statistics, Tracking, Plotting, Export,
//     MachineLearning, Scripting}; hues pairwise distinct, each inside its
//     category's cell (the test's independent cell recomputation)
// (10) noRegisteredTopLevelHueCollides: on the same live registry,
//     FunctionRegistry.validate() reports no "Duplicate top-level color
//     hue" error (the boot-time fail-closed guard over the real ship set)
```

- [ ] **Step 5: Full core suite**

Run: `mvn -o -pl jipipe-core clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]{3},|BUILD"`
Expected: **0 failures** — `TaxonomyPluginTest.allCoreTaxonomyRegistered` (presence + `validateFunctions().isEmpty()` +
106-ID gate) is the integration proof; its function ID list is unchanged (Scripting keeps its id, only its parent
moved).

- [ ] **Step 6: Launcher suite (the whole-registry production validation)**

Run: `mvn -o -pl jipipe-launcher clean test 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -E "Tests run: [0-9]+,|BUILD"`
Expected: **0 failures** (TaxonomyFullValidationTest runs over the entire registered registry — the hue moves +
Scripting promotion must pass it). Then:
`git checkout -- utils/taxonomy-research/registry.json utils/fresh-node-slots.json` (dumper drift).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions jipipe-core/src/test
git commit -m "feat: research-anchored hue families — 6 roots auto-pick, 4 historic pins kept, Scripting promoted to top-level (6.0.0)"
```

---

## Self-review notes (run by the plan author)

- **Spec coverage:** A.2/A.3/A.4 → Tasks 1+2; A.5 → Task 1 (FrameworksTest/presence) + Task 2 (grep gates);
  B.2/B.3/B.4 → Task 3; B.5 (13-root table + Scripting + 77 leaves) → Task 4; B.7 tests → Task 3 Steps 1 + Task 4 Step
  4; migration order 1→6 → task order 1→4 with the grep gates in Task 2 Step 5 and the launcher gate in Task 4 Step 6.
  No gaps.
- **Type consistency:** `HueCategory` constants + `anchorDeg`, `TaxonomyHue.autoHue(HueCategory, String)`,
  `getHueCategory()` default null — identical in Task 3 Produces and Task 4 usage; the 5 framework class names/imports
  identical in Tasks 1 and 2.
- **Ordering hazard:** Tasks 1 and 3 are independent (parallel-safe); Task 2 needs Task 1's classes; Task 4 needs Task
  3's defaults. No task reads an unregistered hue: after Task 3 every root still has an explicit hue, so the suite is
  green before any root drops its override.
- **Known counts:** FrameworksTest 16→20 (ship-set, sanctioned); presence list 16→20; leaf-override deletion work list
  expected 77 (generator is authoritative — a different count stops the task); re-annotation counts 8/5/9/51/62/26/7
  (grep gates, not blind).
- **Proportion:** 4 tasks; the mechanical work (168 annotations, 77 override deletions) is generator-driven with count
  gates, not transcribed.
