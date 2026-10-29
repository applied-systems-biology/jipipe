# Redesign of Node Categorization and Design — Design

Date: 2026-10-08
Issue: [RGerst/jipipe #1324](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1324)

## Problem

JIPipe organizes its ~1,840 registered node types by a fixed set of 11 flat
`JIPipeNodeTypeCategory` classes plus a string `menuPath` that builds deep,
hard-to-navigate submenu hierarchies. Categories also own the visual aspects (authors pick a `colorHue` and icon per
category — an annoying manual task) and
install one button each into the node-list panel. As new frameworks (Cellpose 2/3/4, TrackMate, JFreeChart plotting,
JIPipeDL, …) are added, more
and more nodes are hard to categorize, hard to find, and hard to recognize on
the canvas.

## Goal

A fine-grained, low-effort **node taxonomy** that

- makes authoring new nodes/categories cheap (no per-node visual assignment),
- renames "category" to capture its *functional* meaning ("function"),
- dissolves the `menuPath` hierarchy into a browsable **function tree**,
- adds **tags** (per function and per node), **framework** (origin: Cellpose 4,
  TrackMate, JFreeChart, JIPipeDL, …), **dimensionality** (declared spatial
  2D/3D + derived input profile, see below), and **workflow families** (groups of nodes that form a setup together),
- presents the node selection in a modern, adaptive UI with contextual *related* and *follow-ups* sections, and
- renders framework identity on canvas nodes via a cached procedural
  "gravatar-like" pattern plus a badge, both user-configurable.

All existing nodes are converted to the new system as part of this change (6.0.0 is a breaking major; no legacy
compatibility is carried).

## Non-goals

- No persistence of taxonomy into project files (verified: projects store only
  node-type ids — `jipipe:algorithm-class` — never category/menu-path data).
- No user-editable functions/frameworks/families (they are code-defined;
  tags are the free-form layer).
- No changes to node execution semantics, iteration, or data flow.
- No new node UI subclasses; rendering changes are additive inside the
  existing `JIPipeDesktopGraphNodeUI` paint pipeline.
- Custom/procedural pattern *designs* beyond the bundled DiceBear styles (a later extension; the engine supports adding
  CC0 styles as resources).

## Current system (context)

| Concern                                                                    | Where                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Category interface (id, name, icon, `colorHue`, uiOrder, visibility flags) | `api/nodes/JIPipeNodeTypeCategory.java`; 11 impls in `api/nodes/categories/`                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Node declaration                                                           | `@ConfigureJIPipeNode` (`api/ConfigureJIPipeNode.java`): `nodeTypeCategory`, `menuPath`, `nodeClassification`, `dataSourceMenuLocation`; aliases via repeatable `@AddJIPipeNodeAlias`                                                                                                                                                                                                                                                                                                                       |
| Info extraction at registration                                            | `api/nodes/infos/JIPipeJavaNodeInfo.java` (reads annotations, builds `JIPipeNodeMenuLocation`s)                                                                                                                                                                                                                                                                                                                                                                                                             |
| Registry                                                                   | `api/service/components/JIPipeNodesServiceComponent.java` (`JIPipe.getNodes()`): node infos, category `BiMap`, `getNodesOfCategory`, `getMenuDataSourcesFor`, icon cache                                                                                                                                                                                                                                                                                                                                    |
| Search / node database                                                     | `api/nodes/database/` — `JIPipeNodeDatabase` builds `JIPipeNodeDatabaseEntry`s (incl. `CreateNewNodeByInfoDatabaseEntry`) with `WeightedTokens`, `getCategoryIds()`, `getFillColor/BorderColor`; several search backends incl. embedding/AI search                                                                                                                                                                                                                                                          |
| Node list panel                                                            | `desktop/app/grapheditor/addnodepanel/JIPipeDesktopAddNodesPanel.java` (967 lines): search box + AI toggle → `database.query()` → `JList`; one `JToggleButton` per visible category (right-click = nested `menuPath` `JMenu` tree via `UIUtils.createMenuTree`); optional sub-category breadcrumb from a JGraphT hierarchy; pin/unpin; DnD via `JIPipeDesktopAddNodeTransferHandler` (JSON `Transferable`); double-click inserts at cursor. Mounted in `DOCK_ADD_NODES` by pipeline and compartment editors |
| Node finder dialog                                                         | `nodefinder/JIPipeDesktopNodeFinderDialogUI.java` (Ctrl+A), opened from `commons/contextmenu/add/AddNewNodeUIContextAction.java`                                                                                                                                                                                                                                                                                                                                                                            |
| Node rendering                                                             | `desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java` (1705 lines): plain-rect node body filled with `ThemeUtils.getNodeFillColor(category.colorHue)` (hue < 0 = panel background), title band (run button, icon, name), output slot band, partition chip, striped disabled/pass-through paints, `nodeBuffer` cache; UI class chosen via `JIPipeGraphNode.getNodeUiClass()`                                                                                                                |
| Colors                                                                     | `utils/ThemeUtils.java` (HSB from hue + theme saturation/brightness); theme defaults in `desktop/commons/theme/JIPipeDesktopModernThemeStyle.java`                                                                                                                                                                                                                                                                                                                                                          |
| Search ranking class                                                       | `api/nodes/JIPipeNodeClassification.java` (Standard / AutoImport / EdgeCase) — kept, orthogonal to the new taxonomy                                                                                                                                                                                                                                                                                                                                                                                         |

## Data model (Phase 1)

New API in `api/nodes/`, class-based, instantiated at registration.
Prefix **`JIPipeNodeTaxonomy`** on all names to namespace the concept and
avoid collisions with `JIPipeNodeClassification` and the removed legacy
`JIPipeNodeTypeCategory`.

### `JIPipeNodeTaxonomyFunction`

The renamed "category", now a **tree node** (what the node *does*).

```java
public interface JIPipeNodeTaxonomyFunction {
    String getId();                                   // stable literal, e.g. "jipipe:function:segmentation"
    String getName();                                 // "Segmentation"
    JIPipeNodeTaxonomyFunction getParent();           // null = top-level branch
    @Nullable Float getColorHue();                    // set only on top-level branches;
                                                      // sub-branches auto-shade down the tree
    String getDescription();
    Icon getIcon();                                   // optional; auto-derived fallback
    Set<String> getTags();                            // tags inherited by all nodes under it
    boolean isVisibleInPipeline();
    boolean isVisibleInCompartments();
    // behavior flags carried over from the legacy category:
    default boolean canExtract() { return true; }
    default boolean userCanCreate() { return true; }
    default boolean userCanDelete() { return true; }
    default boolean isRunnable() { return true; }
}
```

Top-level branches declare one deliberate hue; deeper branches derive fill
colors deterministically (shade/tint by tree depth), so **authors never
assign node colors again**. Hue uniqueness across top branches is a
validation-checked invariant (contrast separation).

### Dimensionality: two unambiguous items

The original single `D2…D5` enum was rejected as ambiguous: it conflated a
node's **spatial math** (what it computes) with **how much stack it can be
fed** (a pipeline property). JIPipe's data model makes this precise:
spatial is bounded at 3D (x/y/z); "4D"/"5D" images are 3D-or-2D volumes
plus stack axes — `HyperstackDimension = {Depth (Z), Channel (C), Frame (T)}` —
and feeding/iteration over T/C is handled by the **iteration layer**
(`JIPipeMultiIterationStep` solvers, user-configurable at run time). So the
model splits into:

**1. `spatialDimensions` — declared per node (intrinsic, stable):**

```java
public enum JIPipeNodeTaxonomySpatialDimension { SPATIAL_2D, SPATIAL_3D }

// on @ConfigureJIPipeNode:
spatialDimensions = {SPATIAL_2D, SPATIAL_3D}   // Cellpose: adaptive, declares both
```

"2D" = operates on an x/y plane; "3D" = operates on an x/y/z volume. There is
no 4D/5D *spatial* operation, so this set is closed and unambiguous.

**2. `inputProfile` — derived at registration from the node's input slot
data types (not hand-annotated):**

```java
public enum JIPipeNodeTaxonomyInputProfile { PLANE_2D, VOLUME_3D, ANY }
```

A small provider interface (registered per data class, following the existing
data-type-operation pattern) answers "what is the native input shape of this
data type?". The ImageJ data-types plugin implements it from the existing
`@ImageTypeInfo.numDimensions()` (2 → `PLANE_2D`, 3 → `VOLUME_3D`,
unconstrained `ImagePlusData` → `ANY`). T and C are always iterable by the
solvers, so the effective maximum feedable dimensionality is: `PLANE_2D` → 4D (x/y/T/C), `VOLUME_3D`/`ANY` → 5D
(x/y/z/T/C). Non-image data types report
"N/A" and the facet is hidden for those nodes.

Both are used as **filter facets** and search metadata (filter-only, not
ranking tokens). Example: the Cellpose 4 inference node = spatial `{2D, 3D}`
(declared) + input `ANY → up to 5D` (derived) — both statements true at once,
no ambiguity.

### `JIPipeNodeTaxonomyFramework`

The node's *origin*:

```java
public interface JIPipeNodeTaxonomyFramework {
    String getId();          // stable literal, e.g. "jipipe:framework:cellpose4"
    String getName();        // "Cellpose 4"
    String getAbbreviation();// "CP4" (badge text; ≤ 4 chars, validated)
    String getPatternStyleId(); // DiceBear style id for the procedural pattern (Phase 2)
    // badge/tint color auto-derived from id (fixed hashed palette), no authoring
}
```

Assigned **explicitly per node** (no plugin-level inheritance). Initial set:
`Core` (neutral), `Cellpose 2`, `Cellpose 3`, `Cellpose 4`, `TrackMate`,
`Comet`, `JIPipeDL`, `JFreeChart`, `OpenCV`, `Clij`, `ImageJ 1`, `ImageJ 2`.

### `JIPipeNodeTaxonomyFamily` (+ `JIPipeNodeTaxonomyFamilyStep`)

Workflow families: nodes that belong together because a complete setup needs
several of them (TrackMate, deep learning, plotting).

```java
public interface JIPipeNodeTaxonomyFamily {
    String getId(); String getName(); String getDescription();
    Set<JIPipeNodeTaxonomyFamilyStep> getSteps();          // label + node-type id + one-line purpose
    Graph<String, DirectedWeightedEdge> getGraph();        // directed "after" edges between steps
    @Nullable JIPipeNodeTemplate getTemplate();            // optional wired setup fragment
    @Nullable JIPipeNodeExample getExample();              // optional example project
    Set<String> getTags();
}
```

Declared per node via a **repeatable annotation** (syntactic sugar):

```java
@JIPipeNodeTaxonomyFamilyStep(family = TrackMateFamily.class,
                              label = "Detect spots",
                              after = { /* step classes of predecessors, may be empty */ },
                              purpose = "Finds candidate spots…")
```

The registration system assembles the family graph from all step annotations (resolution deferred, consistent with the
existing scheduling system; a node
may reference a family whose other members register later). A node can
belong to multiple families. For **auto-discovered nodes** (IJ2, CLIJ) the
registration code can build family membership programmatically.

Initial families: **TrackMate setup** (Detect spots → Track spots →
linkage → Filter tracks → Analyze tracks; optional "TrackMate baseline"
template), **Deep learning setup** (Load images → Train/Load model →
Predict → Post-process), **Cellpose 4 setup** (Load stack → Segment cells →
Measure), **Plotting setup** (Prepare table → Plot → Export chart).
Cross-family *relatedness* comes from framework + shared tags, not family
edges.

### Node declaration

`@ConfigureJIPipeNode` grows (class-reference style, matching the existing
`nodeTypeCategory()` pattern):

```java
@ConfigureJIPipeNode(
    name = "Segment cells (2D)",
    function = {CellSegmentationFunction.class},   // [0] = primary
    framework = Cellpose4Framework.class,
    spatialDimensions = {SPATIAL_2D}               // the 3D variant declares {SPATIAL_3D}
)
@JIPipeNodeTaxonomyFamilyStep(family = TrackMateFamily.class, label = "Detect spots", …)
@JIPipeNodeTaxonomyTags({"spot-detection", "tracking"})   // repeatable
public class CellposeSegmentCellsNode extends JIPipeSimpleIteratingAlgorithm { … }
```

- `menuPath()` and `nodeTypeCategory()` are **removed** (6.0.0 hard break).
- `function()` is an array; the first entry is the **primary** function (determines tree position, hue, and the default
  search token set), the rest
  are secondary locations where the node additionally appears (replacing
  `@AddJIPipeNodeAlias`, whose `alternativeName` capability moves to an
  optional per-entry display name if ever needed — not in the seed scope).
- Data-source menu location (`dataSourceMenuLocation`) stays as-is.
- `nodeClassification` (Standard/AutoImport/EdgeCase) is kept unchanged.

### Registration (`JIPipeNodesServiceComponent`)

- Builds the function tree from registered function classes (once, not
  per-panel); replaces the JGraphT menuPath hierarchy machinery.
- Registers frameworks and assembles families (deferred step resolution).
- Exposes `getFunctions()`, `getFrameworks()`, `getFamilies()`,
  `getNodesOfFunction(...)`.
- **Auto-discovered nodes** (IJ2, CLIJ): function/framework/spatial
  dimensionality assigned programmatically by heuristics (name keywords →
  function leaf; "3D"/"2D" in name → spatial set; source plugin →
  framework). Input profile stays derived from the slot types in all cases,
  the only thing not subject to heuristics.
- Legacy `JIPipeNodeTypeCategory` and its 11 implementations are **removed**,
  not shimmed.

### Validation (registration guard)

Run at startup / in tests:

- every registered node resolves to a known function leaf, known framework,
  and (if declared) a valid spatial dimensionality; nodes whose input slot
  data types have no input-profile provider get "N/A" (never a hard error);
- function tree is acyclic, every leaf reachable from a top-level branch;
- family graphs contain no dangling step ids;
- top-branch hues are pairwise distinct (contrast check);
- framework ids/abbreviations are stable literals (extends the existing
  "registered IDs must be stable string literals" rule);
- function ids never derived from code (per the project's stable-ID rule).

## The taxonomy content (seed; refined in Phase 3)

Top-level function branches (each with one deliberate hue):

| Branch           | Sub-branches (dissolved from old menuPaths)                                                                                                          |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Data Sources     | Images (Tiff, OmeTiff, Czi, …), Tables, ROIs, Filaments, Misc.                                                                                       |
| Images           | Preprocessing, Filtering, Thresholding, Segmentation (Cell-based, Region-based), Measurements, Resampling, Merging/Splitting, Color/Stack ops        |
| ROIs             | ROI filters, Measurements, Morphology, Intersections                                                                                                 |
| Filaments        | Smoothing, Filtering, Quantification                                                                                                                 |
| Tables           | Filter/Modify, Join/Split, Aggregate, Transform, Export-to-file                                                                                      |
| Statistics       | Descriptive, Testing, Estimation                                                                                                                     |
| Tracking         | Detection, Linking, Filtering, Analysis (TrackMate/Comet nodes live here functionally)                                                               |
| Annotations      | Add/edit/remove, Convert                                                                                                                             |
| Machine learning | Inference, Training, Models (deep-learning *and* classical classifiers: Weka, LabelKit, ilastik RF; renamed from "Deep Learning" during 1324 review) |
| Plotting         | Scatter, Line, Bar, Heatmap, …                                                                                                                       |
| Export           | File formats, Archive                                                                                                                                |
| Graph/Project    | Compartment outputs, Wrappers, Debug/internal                                                                                                        |
| Miscellaneous    | shrinking residual                                                                                                                                   |

Cellpose/TrackMate/etc. are **not** branches — a TrackMate node lives where it
functions and carries the TrackMate framework; framework filtering crosses the
tree.

Every one of the ~1,840 registration sites is converted to the new
annotations plugin-by-plugin (this conversion *is* the "deep audit and
recategorization" of the issue; Phase 3 refines placement).

## UI (Phase 1)

One new component, `JIPipeDesktopNodeBrowser`, used in two surfaces.

### Sidebar panel (replaces `JIPipeDesktopAddNodesPanel`, same `DOCK_ADD_NODES` slot)

**Adaptive by width** (single component, three arrangements):

- **< 380px — compact:** single function tree with node leaves (icon,
  framework badge, spatial chips); filters in their own wrapping bar;
  related/follow-ups strip at the bottom.
- **≥ 380px — comfortable:** function tree | filtered node list; filter bar;
  related/follow-ups strip.
- **Filter bar** (own row, wrapping, room to grow): Framework ▾,
  Spatial (2D / 3D, multi) ▾, Input (plane / volume / hyperstack, multi) ▾,
  Tags ▾, plus toggles: ★ pinned, ⊂ selection only (nodes whose inputs are compatible with the current selection's
  outputs),
  deprecated/unstable.
- **Search** box with the existing AI toggle preserved; typing flips either
  tier into a flat ranked result list; Enter inserts first hit (preserved).
- **Drag & drop** to canvas and **double-click to add at cursor** preserved (same JSON `Transferable` mechanism; tree
  leaves and list rows draggable).
- Pin/unpin, docs, examples, "show family setup" context menu items preserved.

The old per-category buttons and their right-click `menuPath` popup menus are
removed; the tree takes over the "see what's available" role. Data-source
insertion keeps its simpler existing menu.

### Node finder dialog (rebuilt `JIPipeDesktopNodeFinderDialogUI`, Ctrl+A)

Always the **three-zone** layout: function tree | result list | **detail
rail** (description, framework, tags, spatial + input profile, *related*, *follow-ups* with ▶ Insert setup + Open
example, docs).

### Related / follow-ups (shared implementation)

Computed in `JIPipeNodeDatabase` (not the UI), cached, so the sidebar strip,
dialog rail, and any future surface share one implementation:

- `getFollowUps(entry)` → outgoing family-graph edges of the selected node's
  family steps (ordered).
- `getRelated(entry)` → ranked, de-duplicated mix of same-family siblings,
  same-framework nodes, shared-tag neighbors; framework match outranks tag
  match; existing `JIPipeNodeClassification` down-ranking still applies.

### Search upgrade

Taxonomy fields feed `WeightedTokens`: function path names at decreasing
weight (deeper terms weigh more), framework name, tags at full weight;
spatial + input-profile fields are filter-only. Structured facets replace fuzzy menuPath
substring matching; the AI/embedding search backends index the new fields as
well.

### Colors in the UI

Node list entries, dialog rows, and canvas nodes all derive fill/border from
the function tree hue (top-branch hue shaded by depth) via `ThemeUtils` —
the legacy category-hue path is replaced, not extended.

## Node rendering (Phase 2)

> **SUPERSEDED (2026-10-20):** the DiceBear port plan below was rejected
> after evaluating the real DiceBear output (centered non-plane-filling
> motifs, baked-in palettes, no organic style). Phase 2 now follows
> [`2026-10-20-node-pattern-rendering-design.md`](2026-10-20-node-pattern-rendering-design.md):
> a pure-Java2D formula engine in jipipe-core (no contrib module, no Batik,
> no SVG), color-neutral alpha masks, plane-filling enforced by tests,
> pattern + badge on canvas nodes. The sections below are kept as the
> evaluation record.

Framework identification on canvas nodes: **full-body procedural pattern +
badge** (design option A, approved).

- Node body fill = function hue (unchanged from Phase 1).
- The **pattern** is blitted over the node's main area at low opacity (default ≈ 0.15), theme-aware ink color;
  suppressed where the
  disabled/pass-through hatching paints.
- The **badge** (rounded chip, framework abbreviation, framework-specific
  hashed-palette color independent of the function hue) sits in the title
  band after icon/name; tooltip shows the full framework name.
- Minimap reuses the same tiles scaled down (framework recognizable at
  distance / zoomed out).

### Pattern engine: DiceBear port (contrib module)

This is a solved problem (identicons / procedural avatars). We port **DiceBear** (github.com/dicebear/dicebear, MIT
core, 9.8k stars) instead of
inventing an algorithm:

- Styles are plain JSON definitions rendered by a small core engine (PRNG → option resolution → SVG); all official ports
  (JS/PHP/Python/Rust/
  Go/Dart/C#) are required to be **byte-identical**, so the algorithm is fully
  pinned and verifiable.
- **42 of 63 styles are CC0 public domain** — freely bundleable.
- New module **`contrib/jipipe-avatar-dicebear-<ver>/`** (following the
  existing contrib module pattern, e.g. `contrib/jipipe-ro-crate-java-2.1.0`):
  Java port of the DiceBear core (PRNG, options descriptor, resolver,
  validator, SVG renderer) + bundled CC0 style JSONs as resources +
  attribution (MIT copyright notice kept; CC0 needs none).
- `JIPipeNodeTaxonomyFramework.getPatternStyleId()` selects the style;
  candidate minimalist styles for the seed set: **Blobs** (organic →
  Cellpose), **Constellation** (dots+lines → TrackMate/Comet), **Shape Grid /
  Weave / Loops / Triangles** (geometric → JFreeChart, OpenCV, JIPipeDL).
  Style selection is a per-framework one-liner; more styles can be bundled
  later without engine changes. Custom styles can be authored later using
  JIPipe's image inspect tool for visual evaluation.
- **Rasterization:** generated SVG → 128×128 `BufferedImage` via **Apache Batik** (Apache-2.0) once per framework,
  cached in a
  `Map<String, BufferedImage>`, invalidated on theme/framework
  re-registration; per-node cost ≈ one scaled `drawImage` (the issue's
  caching/performance requirement). Fallback if Batik is judged too heavy:
  direct `Graphics2D` rendering (same interface, no dependency).
- **Dist packaging:** the new contrib jar (s) (incl. Batik if used) are added
  to `dist/dist-info.json` (`dependencies` / `contrib-dependencies`) and
  build scripts regenerated with `python3 dist/generate-dist-scripts.py`;
  plugin-dependencies in `jipipe-distribution-files` updated accordingly.

### User configuration (theme editor, node section)

- *Show framework pattern* (default on)
- *Pattern opacity* (slider, default ≈ 0.15)
- *Show framework badge* (default on)
- *Show framework name in tooltip* (default on)

Persisted in the existing theme document. All off ⇒ today's look (plus the
new function-derived hues). Rendering changes are additive; the existing
`nodeBuffer` cache is reused.

## Migration & compatibility

- **6.0.0 hard break:** `JIPipeNodeTypeCategory` + 11 legacy classes removed;
  `@ConfigureJIPipeNode.nodeTypeCategory()/menuPath()` removed;
  `JIPipeNodeDatabaseEntry.getCategoryIds()` → `getFunctionIds()`.
- **Saved projects unaffected:** they persist node-type ids only (`jipipe:algorithm-class`); all taxonomy metadata is
  rebuilt at
  registration from annotations. No project-file migration needed.
- **Plugin API:** external plugin authors target the new annotations in
  6.0.0; no compatibility layer is provided (accepted).
- **Pinned nodes** (settings `node-db` / `pinned-node-ids`) key on node ids —
  unaffected.
- Conversion of ~1,840 registration sites is done plugin-by-plugin inside
  this change (Phase 1, task block 3), not as a later migration.

## Testing

- **Registration guard tests** (core): every registered node resolves to a
  known function leaf / framework / valid spatial set; input-profile
  derivation is total (no exception for any registered slot type — "N/A"
  allowed); function tree acyclic and fully reachable; family graphs have no
  dangling step ids.
- **Taxonomy content tests:** top-branch hue uniqueness (contrast),
  framework abbreviation length, id stability (literal, not code-derived).
- **Avatar module tests** (contrib, JUnit 5 per the existing contrib module
  pattern): **byte-identical SVG output** against the DiceBear reference test
  vectors for a fixed set of seeds — the port is verifiably correct.
- **UI tests:** unit-level only (no heavyweight Swing harness exists):
  adaptive tier switching by width, filter state handling,
  related/follow-ups ranking in `JIPipeNodeDatabase`.
- **Migration test:** 5.3.0-era project fixtures must load unchanged (category metadata rebuilt fresh).

## Phases

**Phase 1 — Data model, taxonomy, UI, conversion**

1. `JIPipeNodeTaxonomy*` API + annotations + registration (function tree,
   frameworks, families, spatial dimensionality + derived input profile
   provider, tags) + IJ2/CLIJ programmatic assignment + validation guards.
2. Remove legacy category system.
3. Taxonomy audit + conversion of all registration sites plugin-by-plugin (largest task block; mechanical per plugin,
   reviewed per node).
4. `JIPipeDesktopNodeBrowser` (adaptive tiers), filter bar, rebuilt Node
   Finder dialog, related/follow-ups, search-token upgrade, UI colors.

**Phase 2 — Node rendering**
Contrib avatar module (DiceBear port + CC0 styles + dist entries), per-
framework tile cache, pattern+badge paint in `JIPipeDesktopGraphNodeUI`,
theme-editor toggles, minimap reuse.

**Phase 3 — Audit refinement & search tuning**
With the tree live: measure search quality across backends, rebalance
`WeightedTokens`, prune Miscellaneous, fix family follow-up edges, tune
pattern styles per framework (visual review), add custom styles if desired.

Branch per phase, following the `<issue-id>-<name>` convention (e.g. `1324-node-taxonomy-core`,
`1324-node-taxonomy-rendering`,
`1324-node-taxonomy-audit`), each an MR.

## Risks & mitigations

| Risk                                       | Mitigation                                                                                                  |
|--------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Migration size (1,840 sites)               | Mechanical per plugin; registration guard tests catch stragglers at startup and in CI                       |
| Function-tree taste                        | Seed tree is a proposal; Phase 3 refines with the system live                                               |
| DiceBear port fidelity                     | Byte-identity test suite from the reference ports                                                           |
| Pattern readability                        | User-toggleable, low-opacity default; styles swappable per framework as JSON resources without code changes |
| New dependency (Batik)                     | Isolated in the contrib module; direct-Graphics2D fallback behind the same interface                        |
| Auto-discovered node heuristics (IJ2/CLIJ) | Heuristics are data (tables), overridable; Phase 3 tunes                                                    |

## Decisions log (from brainstorming)

1. Scope: full architecture spec, three-phase implementation.
2. Axes: function tree + framework + dimensionality (declared spatial 2D/3D
    + derived input profile) + workflow families + tags.
3. Dimensionality: split into `spatialDimensions` (declared, closed set
   {2D, 3D}) and a *derived* `inputProfile` from the input slot data type —
   the single `D2…D5` enum was rejected as ambiguous (Cellpose is spatial
   2D/3D; "5D" is a pipeline/iteration capability, not a node tag).
4. Naming: `JIPipeNodeTaxonomy*` prefix (avoids collisions).
5. Declaration: annotations referencing classes, instantiated at registration;
   programmatic assignment path for auto-discovered nodes.
6. Framework: explicit per-node.
7. Color: hue per top-level function branch, auto-shaded down the tree.
8. Families: directed graph of steps, repeatable per-node annotation,
   optional template; programmatic for IJ2/CLIJ.
9. UI: adaptive panel (compact < 380px / comfortable ≥ 380px), filters in
   their own wrapping bar, Node Finder dialog always three-zone with detail
   rail; DnD + double-click + AI search preserved.
10. Rendering: full-body pattern + badge (option A), each element
    user-configurable.
11. Pattern engine: DiceBear port in a new contrib module (MIT core + CC0
    styles), dist dependencies configured; minimalist styles first, custom
    styles later (judged with the image inspect tool).
12. Compatibility: 6.0.0 hard break; legacy categories removed, no shims.
