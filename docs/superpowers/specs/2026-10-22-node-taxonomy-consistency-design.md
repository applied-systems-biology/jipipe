# Node taxonomy consistency — frameworks, hues, auto-hue — Design

**Date:** 2026-10-22 · **Branch:** `1324-node-taxonomy-core` · **State:** approved design (user "yes" to the consolidated plan)

## Context

After the taxonomy-service rework landed (functions/frameworks registered through
`JIPipeNodeTaxonomyServiceComponent`, weighted name inference, ID constants,
presence-based tests), an audit of the *content* — which framework each node
declares, and which hue each function branch carries — found systematic
inconsistencies:

- A **framework** = the coherent project / suite / toolkit a node wraps
  (external *or* JIPipe-exclusive). TrackMate and Cellpose are dedicated
  frameworks; other equally-coherent suites are misfiled under unrelated
  parents, untracked, or parked under a placeholder.
- **Conceptually-similar function branches should carry visually-similar
  hues** (e.g. Filaments ≈ ROIs). Today the 12 colored top-level branches
  were hand-assigned and Filaments (40°) sits far from ROIs (0°).

## Part A — framework consistency

### A.1 Principles (user rulings)

1. A framework models a **coherent tool / suite / toolkit** — it does not
   need to be external (JIPipe-exclusive suites like Filaments *are*
   frameworks), and it must not be a **hallucination**: a framework whose
   tool has no nodes in the repo and no raison d'être is dead weight and is
   **deleted**, not kept.
2. JIPipe's own core infrastructure (tables, filesystem, annotations,
   utils, strings, forms, ~175 core nodes) is **not** tagged with
   `CoreFramework` — core infra stays untracked (`NoFramework`). Reason:
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

| Class | ID | Name | Abbr | Pattern style | Notes |
|---|---|---|---|---|---|
| `WekaFramework` | `jipipe:framework:weka` | `Weka` | `WEKA` | `blobs` | IJ Trainable Weka Filter / Weka toolkit |
| `FilamentsFramework` | `jipipe:framework:filaments` | `Filaments` | `FILA` | `web` | JIPipe-exclusive filament suite (51 nodes) |
| `IJ3DFramework` | `jipipe:framework:ij3d` | `IJ3D` | `3D` | `grid` | ImageJ 3D Suite / 3D Viewer (62 nodes) |
| `OmeroFramework` | `jipipe:framework:omero` | `OMERO` | `OMRO` | `stripes` | OMERO data platform (26 nodes) |
| `Scene3DFramework` | `jipipe:framework:scene-3d` | `Scene 3D` | `S3D` | `web` | JIPipe-exclusive 3D-scene suite (7 nodes) |

Pattern styles reuse the existing `NodePatternStyle` catalog — the implementer
picks from the existing style ids (`dots`, `blobs`, `net`, `carpet`,
`patchwork`, `web`, `grid`, `stripes`, `weave`, …) to differentiate visually;
exact choice per framework is listed in the plan's task table. Abbreviations
must stay unique across the registry (`FrameworksTest` pins uniqueness).

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

- `CometFramework` — hallucinated: no nodes, no comet tool anywhere in the
  repo (user ruling: "get rid of the hallucinated CometFramework"). Remove
  the class, its registration in `TaxonomyPlugin`, its entries in
  `FrameworksTest` (the class-literal list, any per-framework asserts) and
  in `TaxonomyPluginTest.EXPECTED_FRAMEWORK_IDS` (delete the
  `jipipe:framework:comet` literal).

### A.5 Tests

- `FrameworksTest`: update the class-literal list (16 → 20: −Comet, +Weka,
  +Filaments, +IJ3D, +Omero, +Scene3D), update the exact-size pins
  (16 → 20) and add the per-framework asserts (name, abbreviation,
  pattern style id) for the five new classes, delete the Comet asserts.
  This test pins the **single-source ship set** of core frameworks, so exact
  counts are correct here (AGENTS.md: one owner, one generator).
- `TaxonomyPluginTest` presence test: `EXPECTED_FRAMEWORK_IDS` 16 → 20
  literals (−`jipipe:framework:comet`, + the five new ids). Still a floor +
  per-id presence check — plugin-aware, not an exact-count pin on the live
  registry.
- A new plugin-aware verification (assert in a test, or a scripted probe
  compiled into the plan): every node in the ship set declares one of the
  registered frameworks — no node left on `JIPipeDLFramework` except none
  (zero), `CometFramework` gone. Form: a grep gate + the presence test
  asserting `JIPipe.getNodeTaxonomyService().frameworkById(...)` for the new
  ids.
- Full core + launcher suites green.

### A.6 Out of scope (recorded so they don't resurface)

- No `CoreFramework` mass-tagging of core infra (~175 nodes) — user ruling:
  visual noise.
- `NodePatternStyle` catalog additions — new frameworks reuse existing styles.
- IJ1 `ImageJ1Framework` keeps its 560+ nodes (ij, ij-algorithms) — correct,
  they wrap ImageJ1.
- `Comet` deletion does not extend to other zero-node frameworks;
  `CoreFramework` stays despite zero node references (it is core's identity
  in the taxonomy).

## Part B — deterministic hue auto-picking for taxonomy functions

### B.1 Principles

1. **Determinism is the contract.** The hue is a pure function of
   `(HueCategory, stable id)` — same input → same hue on every install, in
   every plugin configuration, in any registration order. No
   registry-state-dependent "first free slot" logic: that would make hues
   order- and install-dependent (a plugin added in another session silently
   shifts every later hue) and untestable against the ship set.
2. **Hue is never persisted** (verified: not in settings, node database, or
   project JSON) — it is computed live from the function tree. Safe to
   change; no migration.
3. **Hue is defined by top-level branches only** (existing contract): leaves
   return `null` and inherit + depth-shade via
   `FunctionRegistry.baseHue()`/`shadeDepth()`. A top-level `null` means
   *transparent* (rendered with the theme panel background — `Graph`
   today). The auto-picker only ever produces top-level hues.
4. **The designed hue wins.** The 12 existing colored top-level branches
   keep their explicit `getColorHue()` — they are the anchors that define
   the category layout. Auto-picking is the path for **new** top-level
   branches (plugin authors override `getHueCategory()` instead of hand-
   picking a hue; explicit `getColorHue()` override remains the escape
   hatch if auto-pick collides or looks bad).
5. **Categories = the designed hue families.** `HueCategory` is a closed
   enum of conceptual families, each anchored at the hue its core branch
   uses today. A new top-level in category `ROI` lands *near* ROIs by
   construction.

### B.2 `HueCategory` enum

`org.hkijena.jipipe.api.nodes.taxonomy.HueCategory` — closed enum, one
constant per conceptual family, fields `(String label, float anchorDeg)`:

| Constant | Label | Anchor (deg) | Core branch it anchors |
|---|---|---|---|
| `ROI` | `ROI` | 0 | `RoisFunction` |
| `EXPORT` | `Export` | 22 | `ExportFunction` |
| `MISC` | `Miscellaneous` | 50 | `MiscellaneousFunction` |
| `STATISTICS` | `Statistics` | 60 | `StatisticsFunction` |
| `INPUT` | `Input` | 136 | `DataSourcesFunction` |
| `PLOT` | `Plot` | 155 | `PlottingFunction` |
| `TRACKING` | `Tracking` | 165 | `TrackingFunction` |
| `IMAGE` | `Image` | 186 | `ImagesFunction` |
| `MACHINE_LEARNING` | `Machine learning` | 200 | `MachineLearningFunction` |
| `TABLE` | `Table` | 216 | `TablesFunction` |
| `ANNOTATION` | `Annotation` | 264 | `AnnotationsFunction` |
| `FILAMENTS` | `Filaments` | 15 | `FilamentsFunction` (moved 40° → 15°, sits next to ROI) |

Anchor layout note: anchors are the current core hues, re-used as category
centers. The Voronoi cell of a category spans from the midpoint to the
previous anchor to the midpoint to the next (wrapping). With FILAMENTS at
15°, cells are: ROI [−11, 3.5), FILAMENTS [3.5, 18.5), EXPORT [18.5, 29.5),
MISC [29.5, 43), … (deg). All 12 cells are ≥ 12° wide — comfortable for
±6° of auto-pick spread around each anchor.

### B.3 `TaxonomyHue` utility

`org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyHue` (final class, private
ctor):

```java
/** Deterministically picks an unused-looking hue inside {@code category}'s
 *  window for the function with the given stable id. */
public static Float autoHue(HueCategory category, String stableId)
```

- Input: `(category, id)`; output normalized hue `0..1`.
- Algorithm (exact, so the implementation and the test agree):
  1. `long h = fnv1a64(category.name() + "\u0000" + stableId)` — FNV-1a
     64-bit (offset basis `0xcbf29ce484222325L`, prime `0x100000001b3L`).
  2. `int slot = (int) Math.floorMod(h, SLOTS)` with `SLOTS = 24`.
  3. `float spread = (slot * 0.381966011250105f) % 1f` — the golden-angle
     fraction (137.508°/360°): consecutive slots land maximally far apart
     inside the cell, so nearby ids spread across the whole window.
  4. `hue = (cellStartDeg + spread * cellWidthDeg) / 360f` where the cell
     is the category's Voronoi window (B.2), normalized to `0..1` (wrap
     `((x % 360) + 360) % 360` before dividing).
  Every id maps into its category's Voronoi cell — conceptually similar
  functions stay visually adjacent.
- Pure static: no registry access, no state — a plugin's hue is stable
  regardless of what else is installed.

### B.4 Interface default method

On `JIPipeNodeTaxonomyFunction`:

```java
/** Conceptual hue family used by the deterministic auto-picker when this
 *  function is top-level and does not override {@link #getColorHue()}. */
default HueCategory getHueCategory() {
    return HueCategory.MISC;
}

/** Top-level branches default to the deterministic category hue; leaves
 *  return null (inherit + depth-shade). Override getColorHue() for a
 *  designed hue; return null explicitly for a transparent branch. */
@Nullable
default Float getColorHue() {
    if (getParent() != null) return null;
    return TaxonomyHue.autoHue(getHueCategory(), getId());
}
```

- Leaves: default returns `null` — identical to today. The 77 leaf
  overrides (`return null;`) are **deleted** (the default replaces them).
- The 12 colored roots keep explicit `getColorHue()` (designed anchors)
  **and** each declares its `getHueCategory()` (self-documenting + keeps
  the anchor self-consistent). `Graph` keeps its explicit `return null;`
  (transparent is a deliberate choice, not auto-pickable). `Filaments`
  changes its explicit hue 40° → 15°.
- Interface javadoc updated: `getColorHue()` is now a default with the
  auto-pick path; the "hue is defined by top-level functions only"
  contract unchanged.

### B.5 Consumer impact (verified — none beyond defaults)

- `FunctionRegistry.baseHue()`/`shadeDepth()`/`validate()` — unchanged
  logic; `validate()`'s distinct-top-level-hue rule keeps guarding
  auto-picked hues (two ids hashing to the same slot → boot-time error →
  explicit override is the fix; expected frequency low, ~24 slots/cell).
- `ThemeUtils.resolveFunctionHue` — unchanged; transparent/broken fallback
  paths still correct.
- `JIPipeNodeTemplate.colorHue` — a **template-level** baked hue, not the
  taxonomy path; untouched.

### B.6 Tests (`TaxonomyHueTest`, new)

Non-trivial logic only (AGENTS.md test policy):

1. Determinism: same `(category, id)` → identical hue across calls; equal
   ids in different categories → different hues.
2. Window containment: for every category and a spread of ids, the result
   lies in the category's Voronoi cell (structural invariant computed from
   the enum, not hardcoded angles).
3. Cell structure: the 12 cells are pairwise disjoint and their union
   covers the circle (computed from the closed enum set).
4. Golden-spread sanity: for a fixed corpus of ids in one category, the
   produced hues are pairwise distinct (collisions allowed in theory,
   disallowed for this corpus) and spread across the cell (min pairwise
   distance > 0).
5. Interface-default integration: a leaf of any category defaults to
   `null` (inherit); a top-level with no explicit hue returns its
   category's window hue; `Graph`-style explicit `null` wins over
   auto-pick.

No rendering/Swing tests.

### B.7 Out of scope

- Re-tuning the 12 designed anchors beyond the Filaments move (a wider
  coherence pass is a follow-up if the user asks).
- Auto-picking hues for **frameworks** (framework colors use the
  `PALETTE[floorMod(id.hashCode(), 12)]` default — deterministic already).
- Persisted user overrides of hues (not a feature today; not added).

## Migration order

1. Framework classes (5 new + delete Comet) + registrations — green.
2. Node re-annotations (~168) — compile + suite; the ij-3d/filaments/weka/
   ilastik/omnipose/omero/scene-3d modules compile against the new imports.
3. `HueCategory` + `TaxonomyHue` + interface defaults — green.
4. Function-class migration (delete 77 leaf overrides; 12 roots keep
   explicit hue + add category; Filaments 40°→15°) — green; the presence
   test's `validateFunctions().isEmpty()` keeps guarding duplicates.
5. Tests: `FrameworksTest` 16→20, `TaxonomyPluginTest` framework ids
   16→20, new `TaxonomyHueTest` — full core + launcher suites green.
6. Grep gates: zero `CometFramework` refs; zero non-JIPipeDL-project
   `JIPipeDLFramework` refs (i.e. none at all until the project merges).

## Success criteria

- Every suite green at every step; no exact-count pins on live registries
  (FrameworksTest pins the core ship set — a single-source set — which is
  the sanctioned exception).
- The five new frameworks appear in the node browser with distinct chips;
  ilastik/Omnipose/Weka nodes no longer show JIPipeDL.
- A plugin author adding a top-level function overrides only
  `getHueCategory()` and gets a deterministic, category-adjacent hue.
- No saved project breaks (hue/framework never persisted per node; ids
  only added, except Comet which nothing references).
