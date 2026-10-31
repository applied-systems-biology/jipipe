# Node taxonomy consistency - frameworks, hues, auto-hue - Design

**Date:** 2026-10-22 · **Branch:** `1324-node-taxonomy-core` · **State:** approved design (user "yes" to the
consolidated plan)

## Context

After the taxonomy-service rework landed (functions/frameworks registered through
`JIPipeNodeTaxonomyServiceComponent`, weighted name inference, ID constants,
presence-based tests), an audit of the *content* - which framework each node
declares, and which hue each function branch carries - found systematic
inconsistencies:

- A **framework** = the coherent project / suite / toolkit a node wraps (external *or* JIPipe-exclusive). TrackMate and
  Cellpose are dedicated
  frameworks; other equally-coherent suites are misfiled under unrelated
  parents, untracked, or parked under a placeholder.
- **Conceptually-similar function branches should carry visually-similar
  hues** (e.g. Filaments ≈ ROIs). Today the 12 colored top-level branches
  were hand-assigned and Filaments (40°) sits far from ROIs (0°).

## Part A - framework consistency

### A.1 Principles (user rulings)

1. A framework models a **coherent tool / suite / toolkit** - it does not
   need to be external (JIPipe-exclusive suites like Filaments *are*
   frameworks), and it must not be a **hallucination**: a framework whose
   tool has no nodes in the repo and no raison d'être is dead weight and is **deleted**, not kept.
2. JIPipe's own core infrastructure (tables, filesystem, annotations,
   utils, strings, forms, ~175 core nodes) is **not** tagged with
   `CoreFramework` - core infra stays untracked (`NoFramework`). Reason:
   visual noise in the node browser's framework chips. `CoreFramework`
   remains registered as the identity of core, but no node mass-tagging.
3. JIPipeDL is Dhruv Kathri's **currently-unmerged dedicated project**; it
   is not a catch-all for third-party ML tools. ilastik/Omnipose/Weka must
   not point at it.

### A.2 New frameworks (5)

All follow the exact shape of `IlastikFramework` (final class, frozen
`public static final String ID` literal, name, abbreviation ≤ 4 chars,
pattern style id, optional description), placed in
`org.hkijena.jipipe.api.nodes.taxonomy.frameworks`:

| Class                | ID                           | Name        | Abbr   | Pattern style | Notes                                      |
|----------------------|------------------------------|-------------|--------|---------------|--------------------------------------------|
| `WekaFramework`      | `jipipe:framework:weka`      | `Weka`      | `WEKA` | `cellular`    | IJ Trainable Weka Filter / Weka toolkit    |
| `FilamentsFramework` | `jipipe:framework:filaments` | `Filaments` | `FILA` | `flow`        | JIPipe-exclusive filament suite (51 nodes) |
| `IJ3DFramework`      | `jipipe:framework:ij3d`      | `IJ3D`      | `3D`   | `hilbert`     | ImageJ 3D Suite / 3D Viewer (62 nodes)     |
| `OmeroFramework`     | `jipipe:framework:omero`     | `OMERO`     | `OMRO` | `stripes`     | OMERO data platform (26 nodes)             |
| `Scene3DFramework`   | `jipipe:framework:scene-3d`  | `Scene 3D`  | `S3D`  | `landscape`   | JIPipe-exclusive 3D-scene suite (7 nodes)  |

Pattern styles reuse the existing `NodePatternStyles` catalog (`stripes`,
`net`, `cellular`, `carpet`, `hilbert`, `landscape`, `waves`, `weave`,
`patchwork`, `loops`, `disco`, `triangles`, `blobs`, `dots`, `honeycomb`,
`ripple`, `flow`); the style per framework is fixed in the table above.
Abbreviations must stay unique across the registry (`FrameworksTest` pins uniqueness).

### A.3 Reassignments (~168 `@ConfigureJIPipeNode(framework = …)` edits)

| Node group | Count | From | To | Rationale |
|---|---|---|---|---|---|
| `jipipe-plugin-ilastik` | 8 | `JIPipeDLFramework` | `IlastikFramework` | dedicated framework exists, was unused |
| `jipipe-plugin-omnipose` | 5 | `JIPipeDLFramework` | `OmniposeFramework` | dedicated framework exists, was unused |
| `jipipe-plugin-ij-weka` | 9 | `JIPipeDLFramework` | `WekaFramework` | Weka is its own toolkit |
| `jipipe-plugin-ij-filaments` | 51 | `ImageJ1Framework` | `FilamentsFramework` | JIPipe-exclusive suite, user ruling |
| `jipipe-plugin-ij-3d` | 62 | `ImageJ1Framework` | `IJ3DFramework` | wraps the ImageJ 3D Suite (incl. 3D ROIs) |
| `jipipe-plugin-ij-omero` | 26 | *(none / `NoFramework`)* | `OmeroFramework` | OMERO is a platform, not core infra |
| `jipipe-plugin-scene-3d` | 7 | *(none)* | `Scene3DFramework` | JIPipe-exclusive 3D suite, same ruling as Filaments |

After this, `JIPipeDLFramework` has **zero** node references. It stays
registered (the unmerged project will land its nodes on it later; deleting
it would churn the id set again). `CometFramework` is **deleted** (A.4).

### A.4 Deletions

- `CometFramework` - hallucinated: no nodes, no comet tool anywhere in the
  repo (user ruling: "get rid of the hallucinated CometFramework"). Remove
  the class, its registration in `TaxonomyPlugin`, its entries in
  `FrameworksTest` (the class-literal list, any per-framework asserts) and
  in `TaxonomyPluginTest.EXPECTED_FRAMEWORK_IDS` (delete the
  `jipipe:framework:comet` literal).

### A.5 Tests

- `FrameworksTest`: update the class-literal list (16 → 20: −Comet, +Weka,
  +Filaments, +IJ3D, +Omero, +Scene3D), update the exact-size pins (16 → 20) and add the per-framework asserts (name,
  abbreviation,
  pattern style id) for the five new classes, delete the Comet asserts.
  This test pins the **single-source ship set** of core frameworks, so exact
  counts are correct here (AGENTS.md: one owner, one generator).
- `TaxonomyPluginTest` presence test: `EXPECTED_FRAMEWORK_IDS` 16 → 20
  literals (−`jipipe:framework:comet`, + the five new ids). Still a floor +
  per-id presence check - plugin-aware, not an exact-count pin on the live
  registry.
- A new plugin-aware verification (assert in a test, or a scripted probe
  compiled into the plan): every node in the ship set declares one of the
  registered frameworks - no node left on `JIPipeDLFramework` except none (zero), `CometFramework` gone. Form: a grep
  gate + the presence test
  asserting `JIPipe.getNodeTaxonomyService().frameworkById(...)` for the new
  ids.
- Full core + launcher suites green.

### A.6 Out of scope (recorded so they don't resurface)

- No `CoreFramework` mass-tagging of core infra (~175 nodes) - user ruling:
  visual noise.
- `NodePatternStyle` catalog additions - new frameworks reuse existing styles.
- IJ1 `ImageJ1Framework` keeps its 560+ nodes (ij, ij-algorithms) - correct,
  they wrap ImageJ1.
- `Comet` deletion does not extend to other zero-node frameworks;
  `CoreFramework` stays despite zero node references (it is core's identity
  in the taxonomy).

## Part B - deterministic hue auto-picking for taxonomy functions

### B.1 Principles

1. **Research-anchored categories, not mirrored branches.** The number and
   hue of the categories come from human color categorization research (Berlin & Kay basic color terms; Sturges &
   Whitfield 1995, "Locating
   basic colours in the Munsell space"): humans reliably distinguish **6 hue-only families** at the focal hues **red 0°,
   orange 30°,
   yellow 60°, green 120°, blue 240°, purple 300°**. (The 8 basic
   chromatic categories include pink and brown, which are distinguished by *lightness*, not hue - unusable as hue
   anchors because the taxonomy
   already encodes lightness as tree-depth shading.) Data-viz guidance (Munzner; Berkeley data-viz guide) independently
   caps categorical hue
   palettes at ~6. Hue acuity is non-uniform (weak green/blue, acute
   red-orange-yellow), so the families' Voronoi cells are intentionally
   uneven: wide cells (green, blue) where humans are weak, narrow cells (red, orange, yellow) where humans are acute.
2. **Determinism is the contract.** The auto-picked hue is a pure function
   of `(HueCategory, stable id)` - same input → same hue on every install,
   in every plugin configuration, in any registration order. No
   registry-state-dependent "first free slot" logic (order/install-
   dependent hues, untestable).
3. **Hue is never persisted** (verified: not in settings, node database, or
   project JSON) - computed live from the function tree. Safe to change;
   no migration.
4. **Hue is defined by top-level branches only** (existing contract):
   leaves return `null` and inherit + depth-shade via
   `FunctionRegistry.baseHue()`/`shadeDepth()`. A top-level `null` means *transparent* (theme panel background - `Graph`
   today). The
   auto-picker only ever produces top-level hues.
5. **Explicit hue wins; category is the default path.** A root that keeps
   an explicit `getColorHue()` keeps it. A root with only a category gets
   a deterministic in-cell hue. Plugin authors override `getHueCategory()`
   (one line) and get a family-coherent hue; explicit `getColorHue()`
   remains the escape hatch.

### B.2 `HueCategory` enum - 6 research-anchored categories

`org.hkijena.jipipe.api.nodes.taxonomy.HueCategory`, closed enum,
fields `(String label, float anchorDeg)`; the Voronoi cell of each
category spans from the midpoint to the previous anchor to the midpoint
to the next (wrapping):

| Constant        | Label           | Anchor          | Cell (deg) | Covers                                                                                                                          |
|-----------------|-----------------|-----------------|------------|---------------------------------------------------------------------------------------------------------------------------------|
| `GEOMETRY`      | `Geometry`      | red **0°**      | [330, 15)  | regions, shapes, ROIs, filaments, skeletons, surfaces                                                                           |
| `ANALYSIS`      | `Analysis`      | orange **30°**  | [15, 45)   | statistics, measurement, quantification, tracking/motion analysis                                                               |
| `VISUALIZATION` | `Visualization` | yellow **60°**  | [45, 90)   | plotting, charts, figures                                                                                                       |
| `EXCHANGE`      | `Exchange`      | green **120°**  | [90, 180)  | input, sources, export, data platforms, metadata transport                                                                      |
| `DATA`          | `Data`          | blue **240°**   | [180, 270) | **pixel data and tabular data** - the concept-wise smallest category, generalized from "Images" so tables fall in (user ruling) |
| `INTELLIGENCE`  | `Intelligence`  | purple **300°** | [270, 330) | machine learning, annotations, scripting, expressions, automation                                                               |

Historic color pins land as: ROI/Geometry = red (0°, exact), Inputs/Exchange
= green (Data sources 136° in-cell, Export moves in-cell), Images+Tables =
blue (186°, 216° in-cell), Annotations = purple (explicit 264° kept,
in-cell), ML/Tracking/Plotting move to their families' cells.

### B.3 `TaxonomyHue` utility

`org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyHue` (final class, private
ctor):

```java
/** Deterministically picks a hue inside {@code category}'s Voronoi cell
 *  for the function with the given stable id. */
public static Float autoHue(HueCategory category, String stableId)
```

- Input: `(category, id)`; output normalized hue `0..1`.
- Algorithm (exact, so the implementation and the test agree):
    1. `long h = fnv1a64(category.name() + "\u0000" + stableId)` - FNV-1a
       64-bit (offset basis `0xcbf29ce484222325L`, prime `0x100000001b3L`).
    2. `int slot = (int) Math.floorMod(h, SLOTS)` with `SLOTS = 24`.
    3. `float spread = (slot * 0.381966011250105f) % 1f` - golden-angle
       fraction (137.508°/360°): consecutive slots maximally spread inside
       the cell.
    4. `hue = (cellStartDeg + spread * cellWidthDeg) / 360f`, normalized to
       `0..1` (wrap `((x % 360) + 360) % 360` before dividing).
       Every id maps into its category's cell - conceptually similar functions
       are visually adjacent by construction.
- Pure static: no registry access, no state.

### B.4 Interface default methods

On `JIPipeNodeTaxonomyFunction`:

```java
/** Conceptual hue family used by the deterministic auto-picker when this
 *  function is top-level and does not override {@link #getColorHue()}.
 *  Null category = transparent (no auto hue). */
@Nullable
default HueCategory getHueCategory() {
    return null;
}

/** Top-level branches default to the deterministic category hue; leaves
 *  return null (inherit + depth-shade). Override getColorHue() for a
 *  designed hue; return null explicitly for a transparent branch. */
@Nullable
default Float getColorHue() {
    if (getParent() != null || getHueCategory() == null) return null;
    return TaxonomyHue.autoHue(getHueCategory(), getId());
}
```

Default category is **null → transparent** (a plugin author who sets
nothing gets no hue, like `Graph` - not a forced family). Setting a
category is the one-line opt-in.

### B.5 Function-class migration (90 classes)

| Root                      | Before        | After                                                                                                               |
|---------------------------|---------------|---------------------------------------------------------------------------------------------------------------------|
| `RoisFunction`            | explicit 0°   | keeps explicit **0°** + `GEOMETRY` (on-anchor)                                                                      |
| `FilamentsFunction`       | explicit 40°  | **drops explicit hue**, `GEOMETRY` → auto-pick lands in red cell, next to ROIs (user ask)                           |
| `StatisticsFunction`      | explicit 60°  | **drops explicit hue**, `ANALYSIS` (orange cell)                                                                    |
| `TrackingFunction`        | explicit 165° | **drops explicit hue**, `ANALYSIS` (orange cell - big move, approved: "for ML and tracking and plotting it's fine") |
| `PlottingFunction`        | explicit 155° | **drops explicit hue**, `VISUALIZATION` (yellow cell)                                                               |
| `DataSourcesFunction`     | explicit 136° | keeps explicit **136°** (historic green pin) + `EXCHANGE`                                                           |
| `ExportFunction`          | explicit 22°  | **drops explicit hue**, `EXCHANGE` (green cell)                                                                     |
| `ImagesFunction`          | explicit 186° | keeps explicit **186°** (historic blue pin) + `DATA`                                                                |
| `TablesFunction`          | explicit 216° | keeps explicit **216°** (historic) + `DATA` (tables fall in - user ruling)                                          |
| `MachineLearningFunction` | explicit 200° | **drops explicit hue**, `INTELLIGENCE` (purple cell - approved)                                                     |
| `AnnotationsFunction`     | explicit 264° | keeps explicit **264°** (historic purple pin) + `INTELLIGENCE` (fits with intelligence - user ruling)               |
| `MiscellaneousFunction`   | explicit 50°  | keeps explicit **50°** hard-coded, **no category** (user ruling: hard-code to historic value)                       |
| `GraphFunction`           | explicit null | keeps explicit null (transparent) - no category                                                                     |

New/changed structure: **`ScriptingFunction` is promoted to top-level**
(currently a child of Miscellaneous) with `INTELLIGENCE` category and
auto-picked hue - the first in-tree user of the auto-picker. Its four
leaves (`scripting-python`, `scripting-r`, `scripting-expression`,
`scripting-imagej-macro`) re-parent from Miscellaneous to Scripting and
inherit its hue. "Scripting is turned into a proper function" (user
ruling).

The **77 leaf overrides** (`return null;`) are **deleted** - the interface
default produces exactly that.

### B.6 Consumer impact (verified - none beyond defaults)

- `FunctionRegistry.baseHue()`/`shadeDepth()`/`validate()` - unchanged;
  `validate()`'s distinct-top-level-hue rule still guards auto-picked hues (two ids colliding on one slot → boot error →
  explicit-override escape
  hatch; ~24 slots per cell makes this rare).
- `ThemeUtils.resolveFunctionHue` - unchanged; transparent/broken
  fallbacks still correct.
- `JIPipeNodeTemplate.colorHue` - template-level baked hue, untouched.

### B.7 Tests (`TaxonomyHueTest`, new)

Non-trivial logic only:

1. Determinism: same `(category, id)` → identical hue; different ids in
   one category → (for a fixed corpus) distinct hues.
2. Cell containment: every produced hue lies in its category's Voronoi
   cell (cells computed from the enum anchors, not hardcoded).
3. Cell structure: the 6 cells are pairwise disjoint and tile the circle.
4. Interface-default integration: leaf → null; top-level with category →
   in-cell hue; top-level without category → null (transparent);
   explicit hue/category declarations win.
5. Ship-set guard: no two **auto-picked** top-level hues collide (boot-time `validate()` twin, cheap because the ship
   set is closed).

No rendering/Swing tests.

### B.8 Out of scope

- Re-tuning beyond the moves above (a wider coherence pass is a follow-up).
- Auto-hue for frameworks (PALETTE default is deterministic already).
- Persisted user hue overrides (not a feature today; not added).

## Migration order

1. Framework classes (5 new + delete Comet) + registrations - green.
2. Node re-annotations (~168) - compile + suite; the ij-3d/filaments/weka/
   ilastik/omnipose/omero/scene-3d modules compile against the new imports.
3. `HueCategory` + `TaxonomyHue` + interface default methods - green (defaults change nothing yet: every root still has
   an explicit
   `getColorHue()`).
4. Function-class migration: 13 roots get categories (per B.5 table),
   6 roots drop their explicit hue (auto-pick), 77 leaf overrides deleted,
   Scripting promoted to top-level (4 leaves re-parent), Misc keeps 50°.
   `validateFunctions().isEmpty()` guards hue collisions - green.
5. Tests: `FrameworksTest` 16→20, `TaxonomyPluginTest` framework ids
   16→20, new `TaxonomyHueTest` - full core + launcher suites green.
6. Grep gates: zero `CometFramework` refs; zero `JIPipeDLFramework` node
   refs (until Dhruv's project merges).

## Success criteria

- Every suite green at every step; no exact-count pins on live registries (FrameworksTest pins the core ship set - a
  single-source set - which is
  the sanctioned exception).
- The five new frameworks appear in the node browser with distinct chips;
  ilastik/Omnipose/Weka nodes no longer show JIPipeDL.
- Filaments renders red, adjacent to ROIs; ML purple; Tracking/Statistics
  orange; Plotting yellow; Export green - every family coherent (user-verifiable in the running app).
- A plugin author adding a top-level function overrides only
  `getHueCategory()` and gets a deterministic, category-adjacent hue;
  setting nothing yields transparent.
- No saved project breaks (hue/framework never persisted per node; ids
  only added, except Comet which nothing references).

