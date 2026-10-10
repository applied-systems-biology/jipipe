# Node Taxonomy Conversion Map (Phase 1b, Task 14)

The decision record for converting all `@ConfigureJIPipeNode` registrations from the
deprecated `nodeTypeCategory` / `menuPath` system to the new taxonomy fields
`function` / `framework` / `spatialDimensions`.

Every 19.x conversion batch MUST apply the rules below verbatim and record any
non-obvious per-node decision in the per-module notes table at the bottom.

## 12 Conversion Rules

1. `ImagesNodeTypeCategory` + menuPath → `images/` leaf by first menuPath segment
   ("Deep learning"→`DeepLearningInferenceFunction`, "Thresholding"→`ThresholdingFunction`,
   "Morphology" on ROI output→`RoiMorphologyFunction`, etc.).
2. `ImageJNodeTypeCategory` (257 nodes) → framework `ImageJ1Framework`, function by
   menuPath; spatial: "3D" in name/menuPath→`SPATIAL_3D`, "2D"→`SPATIAL_2D`, else both
   when the slot takes unconstrained `ImagePlusData`.
3. `RoiNodeTypeCategory` → `rois/` by menuPath.
4. `TableNodeTypeCategory` → `tables/`.
5. `AnnotationsNodeTypeCategory`→`annotations/`; `GraphAnnotationsNodeTypeCategory`→`AnnotationsEditFunction`.
6. `DataSourceNodeTypeCategory` → `data/` leaf by output type (image→`ImageDataFunction`,
   `JIPipeDataTable`→`TableDataFunction`, ROI list→`RoiDataFunction`, filament→`FilamentDataFunction`,
   else `MiscDataFunction`).
7. `ExportNodeTypeCategory` → `ExportFileFormatsFunction` (archive→`ExportArchiveFunction`).
8. `FileSystemNodeTypeCategory` → data-source leaf by what it reads, else `MiscDataFunction`.
9. `MiscellaneousNodeTypeCategory` → best-fit leaf; if truly none, `MiscellaneousFunction`
   (kept as a last-resort leaf).
10. `InternalNodeTypeCategory` → `GraphDebugFunction`/`GraphWrappersFunction` by role.
11. Framework by plugin: cellpose→`Cellpose{2,3,4}Framework` per subpackage; trackmate→`TrackMateFramework`;
    clij→`ClijFramework`; opencv→`OpenCVFramework`; plots→`JFreeChartFramework`; weka/ilastik/omnipose→`JIPipeDLFramework`;
    ij2-discovered→`ImageJ2Framework`; ij/ij-3d/ij-algorithms→`ImageJ1Framework` (unless more specific);
    core plugins→`CoreFramework`; else leave `NoFramework` (flagged for Phase 3).
12. Spatial: read the node's first image input slot — `Image2DData`/`d2`⇒`SPATIAL_2D`;
    `d3`⇒`SPATIAL_3D`; `ImagePlusData`/`Image5DData`⇒both; non-image⇒absent (no `spatialDimensions` field).

## Tooling

- `convert.py` (git-ignored, this directory) applies a human-authored `map.tsv`
  mechanically: it rewrites `@ConfigureJIPipeNode(...)` blocks, replacing
  `nodeTypeCategory`/`menuPath` with `function = <FunctionLeaf>.class` and
  inserting `framework = <Framework>.class` / `spatialDimensions = { ... }` per
  TSV row. It is idempotent; `--dry-run` previews.
- `map.tsv` format (one row per distinct category+menuPath-prefix combo, NOT per node):

  ```
  <module>\t<old-category-class>\t<menuPath-prefix-or->\t<FunctionLeaf>\t<FrameworkClass|->\t<spatial|->
  ```

  - `<old-category-class>`: simple name, e.g. `TableNodeTypeCategory`.
  - `<menuPath-prefix-or->`: prefix of the node's `menuPath` (newline-joined submenu
    path, e.g. `Process\nFilters`), or `-` to match any/no menuPath. Specific prefixes
    win over a `-` catch-all; list most-specific first.
  - `<FunctionLeaf>` / `<FrameworkClass>`: simple class name (imports are fixed by
    hand per batch, step 2 of the per-batch method).
  - `<spatial>`: `SPATIAL_2D`, `SPATIAL_3D`, `SPATIAL_2D,SPATIAL_3D`, or `-` (omit field).

- What the script does NOT do (per-batch step 2, by hand): drop leftover
  `@AddJIPipeNodeAlias` (aliases become secondary `function[]` entries), fix
  imports (remove `...categories.*`, add leaf/framework classes), and add
  `@ConfigureJIPipeNode` to programmatic `registerNodeType` node classes (step 3).

- Gate: `mvn test -pl <module> -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=<package-prefix>` → zero unassigned. The test
  lives in jipipe-core and MUST be copy-pasted into plugin modules (core's test
  classpath only registers core's nodes).

## Transition note (read before any 19.x batch)

When a node is converted (its `nodeTypeCategory`/`menuPath` removed and
`function`/`framework` added), `JIPipeJavaNodeInfo.getCategoryOf(nodeClass)`
falls back to `new InternalNodeTypeCategory()` (the no-annotation default),
whose `isVisibleInPipeline()` is `false`. The **old** node panel is still the
mounted UI until **Task 24** and only lists `isVisibleInPipeline()` categories,
so a converted node is *absent from the old panel's per-category buttons* for
the 19.x→24 window (it stays findable via search and still insertable). This
is **intended and accepted**: 6.0.0 is a breaking change on an unmerged feature
branch with no backwards-compat requirement, the approved architecture builds
the new browser in parallel (1a) → converts (19.x) → swaps panels at Task 24,
and the branch is never released mid-conversion. Do NOT "fix" it by keeping
`menuPath`/`nodeTypeCategory` on converted nodes — that would defeat the
conversion. Task 21 rewires the non-visibility consumers (node color,
location-info text, compendium grouping) to `TaxonomyView`, so converted nodes
render correctly everywhere except the old panel's category buttons, which die
with the panel at Task 24.

## Known category inventory (from a full grep, 2026-10-09)

The brief names 11 old categories. A repo-wide grep of `nodeTypeCategory =`
found one additional live category: **`FilamentsNodeTypeCategory` (42 nodes)** —
it does not appear in the 12 rules, so 19.x filament batches map its nodes via
rule 9-style best-fit into the `filaments/` leaves (framework per rule 11).
Also present but with 0 annotated nodes at scan time: none.

| Old category (simple name) | Annotated nodes (grep 2026-10-09) |
|---|---|
| `ImagesNodeTypeCategory` | 424 |
| `ImageJNodeTypeCategory` | 257 |
| `DataSourceNodeTypeCategory` | 111 |
| `RoiNodeTypeCategory` | 103 |
| `MiscellaneousNodeTypeCategory` | 64 |
| `AnnotationsNodeTypeCategory` | 59 |
| `TableNodeTypeCategory` | 55 |
| `FilamentsNodeTypeCategory` | 42 |
| `ExportNodeTypeCategory` | 28 |
| `FileSystemNodeTypeCategory` | 25 |
| `InternalNodeTypeCategory` | (default; see note) |
| `GraphAnnotationsNodeTypeCategory` | (canvas-note nodes) |

## Per-module status

Counts are annotated `@ConfigureJIPipeNode` classes per the phase-1 plan.
Status: `pending` → `converting` → `converted` (gate green). Update after each
19.x batch.

| module | node count | status |
|---|---|---|
| jipipe-core | 31 | converted |
| jipipe-plugin-ij-algorithms | 424 | **converted (19.2a+b+c, 424 nodes); module gate zero unassigned** |
| jipipe-plugin-ij | 80 | **converted (19.3, 80 nodes); module gate zero unassigned** |
| jipipe-plugin-ij-3d | 62 | **converted (19.4, 62 nodes); module gate zero unassigned** |
| jipipe-plugin-filaments + ij-filaments | 51+ | **converted (19.5, 51 nodes — one module, `plugins/jipipe-plugin-ij-filaments`); module gate zero unassigned** |
| jipipe-plugin-tables | 48 | **converted (19.7, 48 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-filesystem | 39 | **converted (19.6, 39 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-annotations | 29 | **converted (19.8, 29 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-utils | 28 | **converted (19.6, 28 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-ij-omero | 26 | **converted (19.8, 26 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-cellpose | 21 | **converted (19.9, 21 nodes, DL frameworks by subpackage); module gate zero unassigned** |
| jipipe-plugin-ij-trackmate | 18 | **converted (19.10, 18 annotated + 14 runtime detector/tracker data sources = 32 registered; all TrackMateFramework); module gate zero unassigned** |
| jipipe-plugin-strings | 15 | **converted (19.6, 15 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-forms | 10 | **converted (19.7, 10 nodes, function-only/NoFramework); module gate zero unassigned** |
| jipipe-plugin-ij-weka | 9 | **converted (19.11a, 9 nodes, all JIPipeDLFramework); module gate zero unassigned** |
| jipipe-plugin-python | 9 | **converted (19.11c, 9 nodes, function-only/NoFramework-omitted); module gate zero unassigned** |
| jipipe-plugin-ilastik | 8 | **converted (19.11a, 8 nodes, all JIPipeDLFramework); module gate zero unassigned** |
| jipipe-plugin-scene-3d | 7 | **converted (19.11a, 7 nodes, NoFramework-omitted); module gate zero unassigned** |
| jipipe-plugin-opencv | 6 | **converted (19.11a, 6 nodes, all OpenCVFramework); module gate zero unassigned** |
| jipipe-plugin-r | 5 | **converted (19.11c, 5 nodes, function-only/NoFramework-omitted); module gate zero unassigned** |
| jipipe-plugin-imp | 5 | pending |
| jipipe-plugin-omnipose | 5 | **converted (19.9, 5 nodes, all JIPipeDLFramework); module gate zero unassigned** |
| jipipe-plugin-clij | 4 | pending |
| jipipe-plugin-ij2 | 2 | pending |
| jipipe-plugin-ij-multi-template-matching | 1 | **converted (19.11c, 1 node, function-only/NoFramework-omitted, spatial both); module gate zero unassigned** |
| jipipe-plugin-ij-ocr | 1 | **converted (19.11c, 1 node, function-only/NoFramework-omitted, spatial both); module gate zero unassigned** |
| jipipe-plugin-plots | 1 | pending |

## Per-module notes (append per batch)

### jipipe-core (Task 19.1) — converted, gate green

**Scope:** all `@ConfigureJIPipeNode` classes under `jipipe-core/src/main/java` (31) **plus** the
programmatic `registerNodeType` node classes that lacked an annotation (1: `JIPipeProjectCompartment`).
Every core node gets `framework = CoreFramework.class` (rule 11). No core node produces image data, so
**none declares `spatialDimensions`**. No core node carries `@AddJIPipeNodeAlias` (grep: 0 matches), so
there are no alias→secondary-function conversions in this batch.

**Per-node decision table** (node class → function → framework → spatial → rationale):

| Node class | function | framework | spatial | rationale |
|---|---|---|---|---|
| `LoopStartNode` (api/looping) | `GraphDebugFunction` | Core | — | Deprecated loop scaffold (R4: loops→GraphDebug); generic `JIPipeData` in/out slots |
| `LoopEndNode` (api/looping) | `GraphDebugFunction` | Core | — | Deprecated loop scaffold (R4); generic `JIPipeData` slots |
| `JIPipeNodeGroup` (api/grouping) | `GraphWrappersFunction` | Core | — | Wraps a sub-pipeline (R4: grouping/wrapper scaffold) |
| `GraphWrapperAlgorithmInput` (api/grouping) | `GraphWrappersFunction` | Core | — | Input plug of a node group (R4: wrappers) |
| `GraphWrapperAlgorithmOutput` (api/grouping) | `GraphWrappersFunction` | Core | — | Output plug of a node group (R4: wrappers) |
| `IOInterfaceAlgorithm` (api/compartments/algorithms) | `GraphDebugFunction` | Core | — | Internal pass-through node used by loops/compartments (R4: truly-internal) |
| `JIPipeProjectCompartment` (api/compartments/algorithms) | `GraphWrappersFunction` | Core | — | Compartment container; `JIPipeCompartmentOutputData` slots (R4: compartments). **Added** `@ConfigureJIPipeNode` (was programmatic, no annotation) |
| `JIPipeProjectCompartmentOutput` (api/compartments/algorithms) | `GraphWrappersFunction` | Core | — | Compartment output (R4: compartments) |
| `JIPipeCommentNode` (plugins/core/nodes) | `GraphDebugFunction` | Core | — | Annotation-less comment node, no workload (R4: internal/debug) |
| `ArrowGraphCanvasNote` (plugins/canvasnotes/nodes) | `AnnotationsEditFunction` | Core | — | Canvas note (rule 5: GraphAnnotations→AnnotationsEdit); no data slots |
| `TextBoxGraphCanvasNote` (plugins/canvasnotes/nodes) | `AnnotationsEditFunction` | Core | — | Canvas note (rule 5) |
| `GroupBoxGraphCanvasNote` (plugins/canvasnotes/nodes) | `AnnotationsEditFunction` | Core | — | Canvas note (rule 5); `@LabelAsJIPipeHidden` |
| `ImageBoxGraphCanvasNote` (plugins/canvasnotes/nodes) | `AnnotationsEditFunction` | Core | — | Canvas note (rule 5); displays an image but is a graph annotation, not image-processing |
| `FileDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | DataSource→data leaf by output type (rule 6); emits `FileData`, not image → Misc; no spatial |
| `FileListDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | Emits `FileData` list → Misc |
| `FolderDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | Emits `FolderData` → Misc |
| `FolderListDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | Emits `FolderData` list → Misc |
| `PathDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | Emits `PathData` → Misc |
| `PathListDataSource` (plugins/filesystem/datasources) | `MiscDataFunction` | Core | — | Emits `PathData` list → Misc |
| `DefineParametersAlgorithm` (plugins/multiparameters/nodes) | `TableDataFunction` | Core | — | Source of `ParametersData` (formerly DataSource category) → tables data leaf (R4: parameters are a table type) |
| `DefineParametersTableAlgorithm` (plugins/multiparameters/nodes) | `TableDataFunction` | Core | — | Source of `ParametersData` → tables data leaf |
| `DefineParametersFromGlobalParametersAlgorithm` (plugins/multiparameters/nodes) | `TableDataFunction` | Core | — | Source of `ParametersData` (built from global params + optional data) → tables data leaf |
| `GenerateParametersFromExpressionAlgorithm` (plugins/multiparameters/nodes) | `TableDataFunction` | Core | — | Source of `ParametersData` (built from expression + optional data) → tables data leaf |
| `ExtractParametersAlgorithm` (plugins/multiparameters/nodes) | `TableDataFunction` | Core | — | Source of `ParametersData` (extracted from a node) → tables data leaf |
| `DefineParametersFromTableAlgorithm` (plugins/multiparameters/nodes) | `TableTransformFunction` | Core | — | Table→parameters transform (formerly Table menuPath "Convert") → table transform leaf |
| `ConvertParametersToTableAlgorithm` (plugins/multiparameters/nodes) | `TableTransformFunction` | Core | — | Parameters→`ResultsTableData` transform (formerly Misc menuPath "Convert") → table transform leaf |
| `ParametersToAnnotationsAlgorithm` (plugins/multiparameters/nodes) | `TableTransformFunction` | Core | — | Parameters→annotations transform (formerly Annotations menuPath "For parameters"); the node operates on parameter/table data → table transform leaf (judgment call, see concerns) |
| `AnnotationsToParametersAlgorithm` (plugins/multiparameters/nodes) | `TableTransformFunction` | Core | — | Annotations→parameters transform (formerly Misc menuPath "Parameters") → table transform leaf |
| `MergeParametersAlgorithm` (plugins/multiparameters/nodes) | `TableJoinSplitFunction` | Core | — | Merges multiple `ParametersData` inputs (formerly Misc menuPath "Parameters") → join leaf |
| `MultiplyParametersAlgorithm` (plugins/multiparameters/nodes) | `TableJoinSplitFunction` | Core | — | Cross-joins all `ParametersData` inputs (formerly Misc menuPath "Parameters") → join leaf |
| `JIPipeDataFlowTunnel1Entrance` (plugins/tunnels/nodes/tunnel1) | `GraphWrappersFunction` | Core | — | Data-flow tunnel endpoint (R4: tunnels→GraphWrappers); generic `JIPipeData` slot |
| `JIPipeDataFlowTunnel1Exit` (plugins/tunnels/nodes/tunnel1) | `GraphWrappersFunction` | Core | — | Data-flow tunnel endpoint (R4); generic `JIPipeData` slot |

(32 rows: 31 originally-annotated classes + the 1 programmatic `JIPipeProjectCompartment`. Only
`tunnel1` exists — there are no `tunnel2`/`tunnel3` classes.)

**Non-obvious decisions / judgment calls:**
- **Multiparameters split into three leaves.** R4 said "`TablesFunction`/`TableTransformFunction` (or the
  most fitting tables leaf) as appropriate". All 11 operate on `ParametersData` (a table type). I applied
  the data-vs-transform-vs-join distinction: the 5 pure `ParametersData` *sources* (no table input, formerly
  `DataSourceNodeTypeCategory`) → `TableDataFunction` (a data source whose output is a table, per rule 6's
  "table→TableDataFunction" logic); the 4 conversions → `TableTransformFunction`; the 2 combining/cross-join
  nodes (Merge/Multiply) → `TableJoinSplitFunction`. This is the best fit to the 5 seed table leaves; a flat
  `TablesFunction` top-level or a single `TableTransformFunction` for all 11 would be coarser.
- **`ParametersToAnnotationsAlgorithm` → `TableTransformFunction` (not `AnnotationsConvertFunction`).** Its
  legacy category was `AnnotationsNodeTypeCategory` (menuPath "For parameters"), which rule 5 would map to an
  `annotations/` leaf. But the node's primary data is parameter/table data that it converts *into* annotations
  — it is a parameter-side converter, not an annotation-processing node. I placed it with the other
  parameter converters under `TableTransformFunction` for consistency with its sibling
  `AnnotationsToParametersAlgorithm`. Flagging for Phase 3 audit (it *could* equally live under
  `AnnotationsConvertFunction`).
- **Canvas notes → `AnnotationsEditFunction` (rule 5).** `ImageBoxGraphCanvasNote` displays an image, but it
  is a graph-canvas annotation (extends `JIPipeGraphCanvasNote`, no data slots), so it belongs in the
  annotations branch, not images. No spatial (it does not process image data).
- **No spatial anywhere in core.** No core node's slots are image-typed (`FileData`/`FolderData`/`PathData`/
  `ParametersData`/`ResultsTableData`/`JIPipeData`/`JIPipeCompartmentOutputData`), so per rule 12 none
  declares `spatialDimensions`.

**Programmatic nodes handled (R2/R3):** the only core node registered via `registerNodeType` that lacked
`@ConfigureJIPipeNode` was `JIPipeProjectCompartment` — I added `function = GraphWrappersFunction,
framework = CoreFramework` (it is a structural compartment container). The other "programmatic" classes
(`GraphWrapperAlgorithmInput/Output`, `JIPipeProjectCompartmentOutput`) already carried an *empty*
`@ConfigureJIPipeNode()` which I filled. The loops/tunnels/multiparameters all already had annotations.

**Gate result (R3):** `mvn test -pl jipipe-core -Dtest=TaxonomyConversionReportTest
-Djipipe.taxonomy.report.module=org.hkijena.jipipe` → **BUILD SUCCESS**, `Tests run: 1, Failures: 0`,
log line `JIPipeNodesServiceComponent | Node taxonomy validation passed` (zero unassigned, zero errors).
`mvn -q compile -pl jipipe-core` → exit 0.

**Classpath caveat (affects what the gate covers):** several core-*package* node classes are actually
*registered* by plugin modules, so they are not on jipipe-core's test classpath and the gate does not
enumerate them individually — they are still converted and verified by the module-wide `mvn compile`:
- `JIPipeNodeGroup`, `IOInterfaceAlgorithm`, `JIPipeCommentNode` are registered by **jipipe-plugin-utils**
  (`UtilitiesPlugin.registerNodeType("node-group"/"io-interface")`).
- The 6 `plugins/filesystem/datasources` classes are registered by **jipipe-plugin-filesystem**
  (`FilesystemPlugin.registerNodeType("import-file"/"import-folder"/...)`).
- `GroupBoxGraphCanvasNote` is not registered by any plugin (orphaned `@LabelAsJIPipeHidden` class); still
  converted for consistency.
The gate covers the nodes registered by **core's own plugins** on the core test classpath —
loops, wrappers, compartments, tunnels, multiparameters, and the canvas notes that `CanvasNotesPlugin`
registers — and all of those passed with zero unassigned. The 10 classes above are registered by **separate
plugin modules** (not on the core test classpath), so this gate does NOT enumerate them; they are still
converted + compile-verified here and are re-covered by those modules' own gates (Task 19.6 for
utils/filesystem).

**Ripple compile fixes:** none required — removing `nodeTypeCategory`/`menuPath`/`…categories.*` imports from
core node classes broke no other module (the legacy fields remain present on `@ConfigureJIPipeNode` and on
`JIPipeNodeTypeCategory` until Task 27, so consumers of core node categories still compile).

**Transition note understood:** converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategory()` (`isVisibleInPipeline` false), so they are absent from the *old* add-node panel's category
buttons until Task 24 swaps in the new taxonomy browser. This is intended (6.0.0 breaking, unmerged branch).
No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

### 19.2 — jipipe-plugin-ij-algorithms mapping (424 nodes)

**Status:** mapping complete (decision record below). Application happens in three mechanical chunks,
19.2a/b/c (assignment at the end of this section). No source files changed in 19.2 itself.

**Primary-category inventory (verified against the exact counts ruled by the controller, R1):**
Images 312 · Roi 67 · DataSource 20 · Export 10 · Table 7 · Annotations 5 · Miscellaneous 2 · ImageJ 1.
393 nodes have a `menuPath`; 31 do not (handled by category/role). The 239 `@AddJIPipeNodeAlias` entries
(e.g. 9 in `Convert`-only nodes aliased under `Image\nType`) are **not** counted — only the primary
`@ConfigureJIPipeNode` block determines category and menuPath.

**Framework (R2 / rule 11):** `ImageJ1Framework` for **all 424 nodes**. Zero nodes were classified as
`ImageJ2Framework`: three files import `net.imglib2.*` classes, but only as IJ1→imglib2 adapters
(`ImageJFunctions.wrap`/`VirtualStackAdapter.wrap` in `LabkitInferenceAlgorithm`, `Coloc2Node`;
`net.imglib2.RandomAccess` types in `Coloc2Node`) — the processing still runs on IJ1 `ImagePlus` APIs, so
the module-level rule stands. No node imports `net.imagej.*` (the SciJava/ImageJ2 context API).

**Spatial (R4 / rule 12):** read from each node's **first image input slot** (the first
`@AddJIPipeInputSlot` whose data class is image-typed; non-image first inputs like `Roi2dListData`,
`ResultsTableData`, `JIPipeData`, `FolderData`, `OMEXMLData`, `FileData` are skipped). Slot dimensionality
comes from the data class's `@ImageTypeInfo(numDimensions=…)` / package: root-level
`ImagePlusData`/`ImagePlusGreyscale*Data`/`ImagePlusColor*Data`/`OMEImageData` are **unconstrained**
(`@ImageTypeInfo` without `numDimensions`) ⇒ both; `d2/…2D…` ⇒ 2D; `d3/…3D…` ⇒ 3D.
**Corrected distribution: both 328 · SPATIAL_3D 16 · SPATIAL_2D 8 · absent 72.** Absent applies exactly
to the 72 nodes with **no image input slot** (rule 12) — the data/table/ROI-geometry/GUI/export families,
including the 22 no-image-input ROI nodes fixed in this revision (see (e) for the full bucket list).
(`StackToDimensionMerger2Algorithm` and `FastImageArithmeticsAlgorithm` carry no
`@AddJIPipeInputSlot` annotations but create image input slots **programmatically** —
`restrictInputTo(ImagePlusData.class)` / `addInputSlot(…, ImagePlusGreyscaleData.class)` — so they are
*not* absent and keep their group default `both`.) The per-node values are authoritative in
`ij-algorithms-final.json` (git-ignored, produced by the survey script); the TSV carries the **group
default** and the chunks apply the per-node values from that table (55 deviations, see (e)).

**Tooling constraint (affects the chunks, not the mapping):** `convert.py` matches rows on
**(category, menuPath-prefix)** only — it cannot express per-node function overrides, and a
`(category, None)` catch-all row is only consulted when no specific prefix matches, so it cannot shadow
specific groups the way a `-` menuPath could. Consequence: every node inside one `(category, menuPath)`
group receives the **group row's leaf** from `convert.py`; where the decided per-node leaf differs (the
overrides table below), the chunk must apply the override **by hand** in the same commit. This is already
required for step 2 of the per-batch method (dropping `@AddJIPipeNodeAlias` → secondary `function[]`,
fixing imports).

#### (a) menuPath-group → leaf decision table

`menuPath` shown with real newlines; count = nodes in the group. Framework = `ImageJ1Framework`
everywhere (omitted). Spatial = group default.

**Images (312) + ImageJ (1) — rules 1/2, by first menuPath segment:**

| menuPath (group) | n | leaf | spatial (group) | rationale for non-obvious |
|---|---|---|---|---|
| `Threshold` | 20 | `ThresholdingFunction` | both | |
| `Threshold\nLocal` | 12 | `ThresholdingFunction` | both | |
| `Threshold\nColor` | 4 | `ThresholdingFunction` | both | |
| `Labels` | 20 | `SegmentationFunction` | both | labeling/label editing = object partitioning; CCL is the segmentation primitive |
| `Labels\nFilter` | 6 | `FilteringFunction` | both | predicate removal of label objects |
| `Labels\nMeasure` | 2 | `MeasurementsFunction` | both | per-label statistics |
| `Labels\nConvert` | 3 | `ColorStackFunction` | both | label→mask/RGB/ROI **rendering** of the partition (judgment; flag Phase 3) |
| `Labkit` | 1 | `DeepLearningInferenceFunction` | both | runs the Labkit ML segmenter (flag Phase 3: no labkit framework exists) |
| `Math` | 15 | `FilteringFunction` | both | pixel-wise local transforms |
| `Math\nLocal` | 19 | `FilteringFunction` | both | local min/max/mean/variance filters, maxima/minima extraction |
| `Math\nDistance map` | 5 | `MeasurementsFunction` | both | distance = geometric measurement field (judgment; flag) |
| `Morphology` | 13 | `FilteringFunction` | both | binary/gray morphology filters |
| `Binary` | 10 | `FilteringFunction` | both | |
| `Filter` | 6 | `FilteringFunction` | both | |
| `Blur` | 5 | `FilteringFunction` | both | |
| `FFT` | 5 | `FilteringFunction` | both | frequency-domain filtering (judgment; flag) |
| `Noise` | 3 | `FilteringFunction` | both | |
| `Edges` | 3 | `FilteringFunction` | both | |
| `Sharpen` | 2 | `FilteringFunction` | both | |
| `Convolve` | 2 | `FilteringFunction` | both | |
| `Convert` | 18 | `ResamplingFunction` | both | bit-depth/type conversions; 3 overrides below |
| `Dimensions` | 17 | `ResamplingFunction` | both | resample/split/merge across dimensions |
| `Transform` | 19 | `ResamplingFunction` | both | crop/rotate/scale/warp = spatial resampling |
| `Colors` | 17 | `ColorStackFunction` | both | |
| `Colors\nConvert` | 1 | `ColorStackFunction` | both | |
| `LUT` | 6 | `ColorStackFunction` | both | |
| `Features` | 13 | `MeasurementsFunction` | both | vesselness/structure feature maps |
| `Statistics` | 8 | `MeasurementsFunction` | both | |
| `Analyze` | 5 | `MeasurementsFunction` | both | |
| `Colocalization` | 1 | `MeasurementsFunction` | both | |
| `Calibration` | 6 | `PreprocessingFunction` | both | physical calibration = prep (judgment; flag) |
| `Contrast` | 4 | `PreprocessingFunction` | both | |
| `Background` | 1 | `PreprocessingFunction` | both | |
| `Masking` | 3 | `PreprocessingFunction` | both | |
| `Montage` | 5 | `MergingSplittingFunction` | both | |
| `Optical flow` | 3 | `MergingSplittingFunction` | **3D** | aligns 3D+time stacks into one (flag) |
| `Registration` | 3 | `MergingSplittingFunction` | both | image alignment (flag) |
| `ROI` | 8 | `MergingSplittingFunction` | both | overlay/extract/ROI-mask ops on images (flag) |
| `Segment` | 3 | `SegmentationFunction` | both | |
| `Detect` | 6 | `SegmentationFunction` | both | Hough/corner detector object finding (flag) |
| `Forms` | 2 | `SegmentationFunction` | both | interactive mask/ROI drawing = manual segmentation (flag) |
| `Metadata` | 3 | `MiscellaneousFunction` | both | metadata in/out, not measurements (flag) |
| `Modify` | 1 | `MiscellaneousFunction` | both | set image properties from expressions |
| *(no menuPath)* | 3 | `MiscellaneousFunction` | both | `RunImageJMacroAlgorithm` (macro runner — spatial **absent by deliberate choice**, see note below), `ImagePlusToGUI` (GUI bridge, leaf override → misc; spatial **both** — it has an `ImagePlusData` input, per rule 12), `MergeImagesAlgorithm` |
| `Enhance` (ImageJ cat.) | 1 | `PreprocessingFunction` | both | `BleachCorrectionAlgorithm`, rule 2 by menuPath |

Images/**Convert** per-node overrides (same group, different operation): `ConvertImageToHSBAlgorithm`,
`ConvertImageToLABAlgorithm`, `ConvertImageToRGBAlgorithm` → `ColorStackFunction` (color-space
conversion); `ImageToMatrixAlgorithm`, `ImageToTableColumnAlgorithm` → spatial 2D; `MatrixToImageAlgorithm`,
`TableToImageAlgorithm`, `OMEMetadataToTableAlgorithm` → spatial absent (no image input).

**Deliberate spatial exception:** `RunImageJMacroAlgorithm` (Images, no menuPath) is a passthrough
macro runner. It *does* carry an `ImagePlusData` input (positional
`@AddJIPipeInputSlot(ImagePlusData.class)`), so the letter of rule 12 would assign it `both`; however it
only shuttles its inputs to/from the ImageJ GUI (runs a macro, re-imports the result) and never
processes image data itself, so it is deliberately left at spatial **absent**. Revisit in Phase 3 if a
policy for GUI-passthrough nodes is wanted.

**Roi (67) — rule 3:**

| menuPath | n | leaf | spatial (group) | rationale |
|---|---|---|---|---|
| `Convert` | 7 | `RoiFiltersFunction` | both | |
| `Draw` | 4 | `RoiFiltersFunction` | both | |
| `Filter` | 11 | `RoiFiltersFunction` | both | |
| `Modify` | 13 | `RoiFiltersFunction` | both | |
| `Metadata` | 4 | `RoiFiltersFunction` | both | |
| `Transform` | 2 | `RoiFiltersFunction` | both | |
| `Process` | 1 | `RoiFiltersFunction` | both | |
| `Register` | 1 | `RoiFiltersFunction` | **2D** | input `ImagePlus2DData` |
| `Merge` | 5 | `RoiIntersectionsFunction` | both | set union of ROI lists |
| `Measure` | 4 | `RoiMeasurementsFunction` | both | |
| `Outline` | 4 | `RoiMorphologyFunction` | **absent** | ROI-only geometry (outline/hull/band/interpolate) |
| `Split` | 4 | `RoiMorphologyFunction` | both | decomposition of ROI shapes |
| *(no menuPath)* | 7 | `RoiFiltersFunction` | **absent** | |

Roi per-node overrides: `ConvertRoiToTableAlgorithm`, `UnreferencedRoiToMaskAlgorithm`,
`UnreferencedRoiToRGBAlgorithm` → spatial absent (no image input); `RoiFloodFillAlgorithm`,
`SetRoiImageAlgorithm` → spatial both (image reference input); `ROIToGUI` → `MiscellaneousFunction`
(GUI bridge, like its Table/Export siblings).

**Table (7) — rule 4:** `Convert` (6) → `TableTransformFunction`, spatial absent (all inputs
`ResultsTableData`); *(no menuPath)* (1) → group row `TableTransformFunction`, but `ResultsTableToGUI`
override → `MiscellaneousFunction` (GUI bridge).

**DataSource (20) — rule 6, by output type:** group row `ImageDataFunction` (majority: image-producing
sources, incl. `ImageFromMatrix2DAlgorithm` under `Convolve`). Overrides by output:
`GenerateMissingTablesAlgorithm`, `GenerateMissingTablesAlgorithm2`, `ResultsTableFromGUI` →
`TableDataFunction`; `ROIFromGUI`, `DefineRectangularRoiAlgorithm`, `GenerateMissingRoiListsAlgorithm`,
`ReferencedDefineRectangularRoiAlgorithm` → `RoiDataFunction`; `RunImageJImporterAlgorithm` →
`MiscDataFunction` (emits `JIPipeData`). Spatial: absent for all except the three that take an image to
complete on — `OMEImageFromImagePlus`, `GenerateMissingZeroImage`, `GenerateMissingImageFromMathExpression2D`
→ both.

**Export (10) — rule 7:** `Images` (3) → `ExportFileFormatsFunction` (spatial both); `Tables` (4) →
`ExportFileFormatsFunction`; `ROI` (2) → `ExportArchiveFunction` (ROI lists are exported as `.zip` ROI
archives — `saveToRoiOrZip`); *(no menuPath)* (1) `RunImageJExporterAlgorithm` → `ExportFileFormatsFunction`.
Spatial absent for ROI/Tables/no-menuPath (no image input); `ExportImageDirectorySlotAlgorithm`
→ `ExportArchiveFunction` override (bulk directory export incl. AVI).

**Annotations (5) — rule 5:** `For images` (3) → `AnnotationsEditFunction` (spatial both);
`For ROI` (1) → `AnnotationsEditFunction` (absent); `For all data` (1) → `AnnotationsEditFunction` (absent).
No annotations node converts *between* annotation types, so `AnnotationsConvertFunction` is unused here.

**Miscellaneous (2) — rule 9:** `Convert` (1) `DataToPreviewAlgorithm` → `MiscellaneousFunction` (renders
arbitrary data to a preview image); `Meta run` (1) `RenderJIPipeProjectAlgorithm` → `PlotMiscFunction`
(a rendered visualization of the whole pipeline graph; flag Phase 3).

#### (b) Leaf distribution (all 424)

`FilteringFunction` 89 · `ResamplingFunction` 51 · `RoiFiltersFunction` 49 · `ThresholdingFunction` 36 ·
`MeasurementsFunction` 34 · `SegmentationFunction` 31 · `ColorStackFunction` 30 · `MergingSplittingFunction` 19 ·
`PreprocessingFunction` 15 · `ImageDataFunction` 12 · `MiscellaneousFunction` 10 · `RoiMorphologyFunction` 8 ·
`ExportFileFormatsFunction` 7 · `TableTransformFunction` 6 · `AnnotationsEditFunction` 5 ·
`RoiIntersectionsFunction` 5 · `RoiDataFunction` 4 · `RoiMeasurementsFunction` 4 · `TableDataFunction` 3 ·
`ExportArchiveFunction` 3 · `MiscDataFunction` 1 · `DeepLearningInferenceFunction` 1 · `PlotMiscFunction` 1.

#### (c) Chunk assignment (R7)

Split is **category-coherent** so every chunk is a contiguous, independently-compilable unit (groups are
never split across chunks, and all live in one Maven module sharing the classpath). The three image-free
categories plus Roi (the bulk of the non-image work) form 19.2c — the chunk that also runs the module gate.
Images + the single ImageJ node (313) are split into 19.2a (core image processing: thresholding,
labeling, math, morphology, binary, convert) and 19.2b (all other image analysis + the ImageJ node).
Sizes are 148 / 165 / 111 (sum 424) — within the "~140 each, close is fine" bar; 19.2c is a little light
because the ROI/table/data/export/annotation families are a naturally smaller, coherent unit.

| chunk | groups (category — menuPath) | nodes |
|---|---|---|
| **19.2a** (Images core) — **APPLIED** (148/148 converted; 91 same-leaf alias drops + 20 secondary `function[]` entries; 0 unresolved imports; contract-conformance vs `ij-algorithms-final.json` = 0 mismatches) | Images: `Threshold`(20), `Threshold\nLocal`(12), `Threshold\nColor`(4), `Labels`(20), `Labels\nFilter`(6), `Labels\nMeasure`(2), `Labels\nConvert`(3), `Labkit`(1), `Math`(15), `Math\nLocal`(19), `Math\nDistance map`(5), `Morphology`(13), `Binary`(10), `Convert`(18) | **148** |
| **19.2b** (Images analysis + ImageJ) — **APPLIED** (165/165 converted; 83 same-leaf alias drops + 31 secondary `function[]` entries (1 node — `MergeImagesAlgorithm` — carries 2); 0 unresolved imports; contract-conformance vs `ij-algorithms-final.json` = 0 mismatches) | Images: `Dimensions`(17), `Transform`(19), `Colors`(17), `Colors\nConvert`(1), `LUT`(6), `Filter`(6), `Blur`(5), `FFT`(5), `Edges`(3), `Noise`(3), `Sharpen`(2), `Convolve`(2), `Features`(13), `Statistics`(8), `Analyze`(5), `Colocalization`(1), `Calibration`(6), `Contrast`(4), `Background`(1), `Masking`(3), `Montage`(5), `Optical flow`(3), `Registration`(3), `ROI`(8), `Segment`(3), `Detect`(6), `Forms`(2), `Metadata`(3), `Modify`(1), *(no menuPath)*`(3)`, and ImageJ: `Enhance`(1) | **165** |
| **19.2c** (Roi/Table/DataSource/Export/Annotations/Misc — runs the gate) — **APPLIED** (111/111 converted; 13 same-leaf alias drops + 16 secondary `function[]` entries (16 nodes, 1 each; `TransformRoiFromExpressionsAlgorithm` alone contributes 3 same-leaf drops); 0 unresolved imports; contract-conformance vs `ij-algorithms-final.json` = 0 mismatches; full-module gate `TaxonomyConversionReportTest` (`-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.imagejalgorithms`) = zero unassigned, and skips cleanly when the property is unset) | Roi: `Convert`(7), `Draw`(4), `Filter`(11), `Measure`(4), `Merge`(5), `Metadata`(4), `Modify`(13), `Outline`(4), `Process`(1), `Register`(1), `Split`(4), `Transform`(2), *(no menuPath)*`(7)` = 67; Table: `Convert`(6), *(no menuPath)*`(1)` = 7; DataSource: `Convolve`(1), *(no menuPath)*`(19)` = 20; Export: `Images`(3), `ROI`(2), `Tables`(4), *(no menuPath)*`(1)` = 10; Annotations: `For ROI`(1), `For all data`(1), `For images`(3) = 5; Miscellaneous: `Convert`(1), `Meta run`(1) = 2 | **111** |

Each chunk compiles standalone against the same module classpath; the module gate
(`TaxonomyConversionReportTest`, `-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.imagejalgorithms`)
runs only in 19.2c, when all 424 are converted.

#### (d) Dry-run proof (R6)

`python3 utils/taxonomy-conversion/convert.py --map utils/taxonomy-conversion/ij-algorithms-map.tsv
--target plugins/jipipe-plugin-ij-algorithms/src/main/java --dry-run`
→ `Would change 424 file(s); 424 annotation(s) rewritten; 0 unmatched` (nothing was written; `--dry-run`).

#### (e) Per-node spatial deviations from the TSV group default (55 total)

convert.py writes the TSV group default; the chunks must apply the per-node values from
`ij-algorithms-final.json` (authoritative; the list below is a human-readable cross-check).
**55 nodes** deviate, in four buckets:

- **29 → absent** (no image input slot, rule 12 — including the 22 reviewer-named ROI nodes fixed in
  this revision): Roi/Filter `FilterRoi2dByNameAlgorithm`, `FilterRoi2dByOverlapAlgorithm`,
  `FilterRoi2dListsAlgorithm`, `SliceRoi2dListAlgorithm`; Roi/Merge `AppendRectangularRoiAlgorithm`,
  `MergeRoiListsOrderedAlgorithm`, `MergeRoiListsPairwiseOrAlgorithm`, `MergeRoiListsUnorderedAlgorithm`;
  Roi/Measure `CountROIAlgorithm`; Roi/Modify `ChangeRoiPropertiesAlgorithm`,
  `ChangeRoiPropertiesFromAnnotationsAlgorithm`, `ChangeRoiPropertiesFromTableAlgorithm`,
  `ColorRoiByNameAlgorithm`, `FlattenRoiAlgorithm`, `ReorderRoiDimensionsAlgorithm`,
  `RoiToCentroidAlgorithm`; Roi/Transform `ScaleRoiAlgorithm`; Roi/Metadata
  `ExtractROIMetadataAlgorithm`, `RemoveROIMetadataAlgorithm`, `SetROIMetadataFromTableAlgorithm`;
  Roi/Split `SplitMultiComponentRoi2dAlgorithm`, `SplitRoi2dIntoIndividualListsAlgorithm`; Roi/Convert
  `ConvertRoiToTableAlgorithm`, `UnreferencedRoiToMaskAlgorithm`, `UnreferencedRoiToRGBAlgorithm`;
  Images/Convert `MatrixToImageAlgorithm`, `TableToImageAlgorithm`, `OMEMetadataToTableAlgorithm`;
  plus `RunImageJMacroAlgorithm` (deliberate, see note above).
- **6 → both** (have an image input where the group default is absent): DataSource
  `OMEImageFromImagePlus`, `GenerateMissingZeroImage`, `GenerateMissingImageFromMathExpression2D`,
  `ReferencedDefineRectangularRoiAlgorithm`; Roi/no-menuPath `RoiFloodFillAlgorithm`,
  `SetRoiImageAlgorithm`.
- **13 → SPATIAL_3D** (3D-constrained first image input): `AnalyzeSkeleton2D3DAlgorithm`,
  `ConnectedComponentsLabeling3DAlgorithm`, `StackInverterAlgorithm`,
  `AnnotateWithOverlapMeasureLabelsAlgorithm`, `OverlapMeasureLabelsAlgorithm`,
  `FilterLabelsByExpression3DAlgorithm`, `ChamferDistanceMap3DAlgorithm`, `ImposeMaxima3DAlgorithm`,
  `GrayscaleAttributeFiltering3DAlgorithm`, `MorphologicalReconstruction3DAlgorithm`,
  `MorphologySkeletonize3DAlgorithm`, `ClassicWatershedSegmentationAlgorithm`,
  `SeededWatershedSegmentationAlgorithm`. (The 3 `Optical flow` nodes are also 3D, but their group
  default is already 3D, so no override is needed.)
- **7 → SPATIAL_2D** (2D-constrained first image input): `ImageToMatrixAlgorithm`,
  `ImageToTableColumnAlgorithm`, `FFT2DForwardTransform`, `FFT2DInverseTransform`,
  `FFT2DSwapQuadrants`, `InputImagesToMontage`, `MontageToStackAlgorithm`.
  (`RegisterRoiToImageByBrightnessAlgorithm` is also 2D, but its group default is already 2D.)

Consistency check: total 3D set = 13 overrides + 3 optical-flow = 16; total 2D set = 7 overrides +
1 register = 8; total absent = 29 overrides + 43 group-default-absent = 72; total both =
328. Leaf (function) overrides total **14 nodes** (3 Images/Convert→`ColorStackFunction`,
3 DataSource→`TableDataFunction`, 4 DataSource→`RoiDataFunction`, 1 DataSource→`MiscDataFunction`,
4 GUI bridges→`MiscellaneousFunction`, 1 Export→`ExportArchiveFunction` — all listed in (a)).

#### (f) Flagged for Phase 3 audit

- `Labels\nConvert` → `ColorStackFunction` (label rendering; could be its own leaf).
- `Labkit` → `DeepLearningInferenceFunction` (no labkit framework exists; framework stays ImageJ1).
- `Math\nDistance map` → `MeasurementsFunction` (could be `FilteringFunction`).
- `Calibration` → `PreprocessingFunction` (could be `MeasurementsFunction`).
- `Optical flow` / `Registration` → `MergingSplittingFunction` (no dedicated registration leaf).
- `Images/ROI` → `MergingSplittingFunction` (overlay/extract ops; partly ROI-domain).
- `Detect` / `Forms` → `SegmentationFunction` (detection & interactive drawing; could be `MeasurementsFunction` / manual-annotation leaf).
- `Images/Metadata` → `MiscellaneousFunction` (could be `MeasurementsFunction` for the extract node).
- `FFT` → `FilteringFunction` (could be `MeasurementsFunction` for transform-only nodes).
- `RenderJIPipeProjectAlgorithm` → `PlotMiscFunction` (pipeline rendering; could be `MiscellaneousFunction`).

### 19.3 — jipipe-plugin-ij mapping (80 nodes)

**Status:** converted — all 80 `@ConfigureJIPipeNode` nodes rewritten to `function`/`framework`/
`spatialDimensions`; module gate green (see Gate result below).

**Scope:** every `@ConfigureJIPipeNode` class under
`plugins/jipipe-plugin-ij/src/main/java` (package `org.hkijena.jipipe.plugins.imagejdatatypes`).
The module is **data-type wrappers + importers/exporters** of IJ1 `ImagePlus`/Bio-Formats images:
54 `ImagePlus*Data` data-type classes (2D/3D/4D/5D, color/greyscale/mask, FFT) plus 26 algorithm classes
(importers, export nodes, ROI/table sources, color-space/contrast/converter ops, macro + OME-XML
sources, 3 annotation nodes).

**Inventory (verified, matches controller R1):** 80 annotated files. Primary `nodeTypeCategory`
distribution: *(default = InternalNodeTypeCategory)* **54**, `DataSourceNodeTypeCategory` **12**,
`ImagesNodeTypeCategory` **7**, `ExportNodeTypeCategory` **4**, `AnnotationsNodeTypeCategory` **3**.
14 nodes have no `menuPath` (3 in the Images category + 11 in the DataSource category).

**Framework (R1 / rule 11):** `ImageJ1Framework` for **all 80 nodes**. **0 files import
`net.imagej.*`** (verified by grep) — the module wraps IJ1 `ImagePlus`/Bio-Formats data only; there is
no IJ2-API wrapper node here, so the module-level rule holds without exception.

**Spatial (R2 / rule 12):** from each node's first image input slot, or (for data-type wrappers and
sources, which have no image *input*) the data's **declared dimensionality** from its `@ImageTypeInfo` /
menuPath / data-class name:
- `d2/*` classes, `ImagePlusFFT2DData` ⇒ `SPATIAL_2D` (12)
- `d3/*` classes, `ImagePlusFFT3DData` ⇒ `SPATIAL_3D` (11)
- unconstrained / `d4` / `d5` image data (bare `ImagePlus*` color+greyscale, `FFT4D/5D`, `FFT`) ⇒
  **both** (dimensioned-wrapper ruling: the spatial set is closed to {2D,3D}) (51)
- **absent** (no `spatialDimensions`): table/ROI/OME-XML/macro sources + the file-only annotation node
  (no image dimensionality to declare) (6)

**Distribution:** spatial 2D=12 · 3D=11 · both=51 · absent=6. Function leaves: `ImageDataFunction` 61 ·
`MiscDataFunction` 3 · `ExportFileFormatsFunction` 3 · `ColorStackFunction` 3 · `AnnotationsEditFunction` 3 ·
`RoiDataFunction` 2 · `PreprocessingFunction` 2 · `TableDataFunction` 1 · `ResamplingFunction` 1 ·
`ExportArchiveFunction` 1.

#### (a) Data-type wrappers (54) — all `ImageDataFunction`, `ImageJ1Framework`; spatial by declared dimension

The 54 `ImagePlus*Data` classes (registered as data types via `registerImageDataType` in
`ImageJDataTypesPlugin` AND exposed as browsable nodes via their `@ConfigureJIPipeNode`
annotation — these are exactly the 54 "default/Internal" category nodes of the inventory) are pure
IJ1 `ImagePlus` image-data types. Function is the **data/ branch leaf**
`ImageDataFunction` (the "image data" locations — same as the ij-algorithms DataSource→`ImageDataFunction`
precedent); none is a table/ROI/filament type, so no `TableDataFunction`/`RoiDataFunction`/other here.
Spatial comes from the `@ImageTypeInfo(numDimensions=…)` / package (no image *input* slot exists on a
data type, so R2 uses the declared dimensionality). Grouped (full 80-row table is in the report; these
groups cover all 50):

| wrapper group | classes (n) | function | spatial | rationale |
|---|---|---|---|---|
| 2D image types (`d2`: base + Color{,HSB,LAB,RGB} + Greyscale{,8U,16U,32F,Mask}) | 10 | `ImageDataFunction` | **2D** | `@ImageTypeInfo(numDimensions=2)` / `d2` package |
| 2D FFT (`ImagePlusFFT2DData`) | 1 | `ImageDataFunction` | **2D** | `numDimensions=2`, frequency-space 2D image |
| 3D image types (`d3`: base + Color{,HSB,LAB,RGB} + Greyscale{,8U,16U,32F,Mask}) | 10 | `ImageDataFunction` | **3D** | `numDimensions=3` / `d3` package |
| 3D FFT (`ImagePlusFFT3DData`) | 1 | `ImageDataFunction` | **3D** | `numDimensions=3`, frequency-space 3D image |
| bare unconstrained color (`Color{,HSB,LAB,RGB}`, root) | 4 | `ImageDataFunction` | **both** | `@ImageTypeInfo` without `numDimensions` ⇒ unconstrained (dimensioned-wrapper ruling) |
| bare unconstrained greyscale (`Greyscale{,8U,16U,32F,Mask}`, root) | 5 | `ImageDataFunction` | **both** | unconstrained `@ImageTypeInfo` ⇒ both |
| 4D image types (`d4`: base + Color{,HSB,LAB,RGB} + Greyscale{,8U,16U,32F,Mask}) | 10 | `ImageDataFunction` | **both** | 4D (≥3) collapses to {2D,3D} — closed spatial set |
| 5D image types (`d5`: base + Color{,HSB,LAB,RGB} + Greyscale{,8U,16U,32F,Mask}) | 10 | `ImageDataFunction` | **both** | 5D (≥3) collapses to {2D,3D} |
| FFT `ImagePlusFFT4DData`, `ImagePlusFFT5DData`, `ImagePlusFFTData` | 3 | `ImageDataFunction` | **both** | 4D/5D ⇒ both; bare `FFT` unconstrained ⇒ both |

#### (b) Images-category operational nodes (7) — rule 1 / R1

| node | function | spatial | rationale |
|---|---|---|---|
| `ApplyDisplayContrastAlgorithm` | `PreprocessingFunction` | both | menuPath `Contrast`; applies brightness/contrast display range (precedent: ij-algorithms `Contrast`→`PreprocessingFunction`); first image input `ImagePlusData` (unconstrained) ⇒ both |
| `DisplayRangeCalibrationAlgorithm` | `PreprocessingFunction` | both | menuPath `Contrast`; calibrates the display range; `ImagePlusData` in ⇒ both |
| `ToRGBColorSpaceConverterAlgorithm` | `ColorStackFunction` | both | menuPath `Colors\nConvert`; color-space conversion (precedent: ij-algorithms `Colors\nConvert`→`ColorStackFunction`); `ImagePlusData` in ⇒ both |
| `ToHSBColorSpaceConverterAlgorithm` | `ColorStackFunction` | both | color-space conversion; `ImagePlusData` in ⇒ both |
| `ToLABColorSpaceConverterAlgorithm` | `ColorStackFunction` | both | color-space conversion; `ImagePlusData` in ⇒ both |
| `ImportLUTAsRGBFromFileAlgorithm` | `ImageDataFunction` | **2D** | **R1 override — judged by data class + purpose, not the Images category.** No menuPath; imports a LUT as a *2D* RGB image (source), output `ImagePlus2DColorRGBData`. It produces image data (data-source role) rather than processing it, so it is the data leaf `ImageDataFunction`, spatial from the 2D output. Flagged for Phase 3 (see (d)). |
| `ImageTypeConverter` | `ResamplingFunction` | both | **non-obvious.** No menuPath; converts an `ImagePlusData` image *into another ImagePlus data type* (bit-depth / 2D/3D / greyscale↔color) via a parameter. It is an in-place image type conversion, not a data source (it has an image input it transforms) — the ij-algorithms `Convert` group precedent is `ResamplingFunction`. `ImagePlusData` in ⇒ both. Flagged for Phase 3 (see (d)). |

#### (c) DataSource (12), Export (4), Annotations (3) — rules 5/6/7, R1

**DataSource (12) — rule 6, by OUTPUT type:**

| node | function | spatial | output (rationale) |
|---|---|---|---|
| `BioFormatsImporterAlgorithm` | `ImageDataFunction` | both | `OMEImageData` (Bio-Formats image import; OME wraps an unconstrained `ImagePlus`) |
| `ImportImageJSampleAlgorithm` | `ImageDataFunction` | both | `ImagePlusData` (ImageJ sample image; no input ⇒ source, unconstrained) |
| `ImportImagePlusAlgorithm` | `ImageDataFunction` | both | `ImagePlusData` (image from file) |
| `ImportOMEZARRFromURIAsImagePlusAlgorithm` | `ImageDataFunction` | both | `OMEImageData` (OME-Zarr from URI) |
| `ImportOMEZARRFromZipDirectoryAsImagePlusAlgorithm` | `ImageDataFunction` | both | `OMEImageData` (OME-Zarr from zip dir) |
| `NativeImageJImporterAlgorithm` | `ImageDataFunction` | both | `ImagePlusData` (ImageJ1 `IJ.open` import) |
| `ImportResultsTableAlgorithm` | `TableDataFunction` | **absent** | `ResultsTableData` (results table from file ⇒ table leaf, no image) |
| `ImportRoi2dAlgorithm` | `RoiDataFunction` | **absent** | `Roi2dListData` (ROI list from file ⇒ ROI leaf, no image) |
| `ExtractRoi2dFromOMEImageAlgorithm` | `RoiDataFunction` | **both** | `Roi2dListData` **but** it takes an `OMEImageData` *input* (extracts ROIs from the OME image) ⇒ first image input OME (unconstrained) ⇒ both. ROI leaf by output, spatial by input. |
| `BioFormatsExtractOMEXMLAlgorithm` | `MiscDataFunction` | **absent** | `OMEXMLData` (extends `XMLData`→`StringData`; OME-XML text, not image/table/ROI) |
| `DefineImageJMacroAlgorithm` | `MiscDataFunction` | **absent** | `ImageJMacroData` (extends `StringData`; a macro text, not image) |
| `ImportImageJMacroAlgorithm` | `MiscDataFunction` | **absent** | `ImageJMacroData` (macro text from file) |

**Export (4) — rule 7:**

| node | function | spatial | rationale |
|---|---|---|---|
| `BioFormatsExporterAlgorithm` | `ExportFileFormatsFunction` | both | writes `*.ome.tiff` (single file format); OME image input ⇒ both. (Deprecated sibling of `…2Algorithm`.) |
| `BioFormatsExporter2Algorithm` | `ExportFileFormatsFunction` | both | writes `*.ome.tiff`; OME image input ⇒ both |
| `SetBioFormatsExporterSettings` | `ExportFileFormatsFunction` | both | **non-obvious (flagged).** Configures the OME-TIFF exporter settings (pass-through `OMEImageData` in→out, no file written). It is not a file writer, but its whole purpose is preparing an OME-TIFF export and it sits under the `Images` export group, so it is grouped with the export-leaf family. `OMEImageData` in ⇒ both. |
| `ExportImagePlusAsOMEZARRAlgorithm` | `ExportArchiveFunction` | both | **judgment.** "Writes an OME ZARR into a directory or **ZIP** file" — a ZARR archive, so the archive leaf per rule 7 (archive→`ExportArchiveFunction`). `OMEImageData` in ⇒ both. |

**Annotations (3) — rule 5:**

| node | function | spatial | rationale |
|---|---|---|---|
| `AnnotateDataWithImagePropertiesAlgorithm` | `AnnotationsEditFunction` | both | `For all data`; adds data annotations from an image's properties; image input `ImagePlusData` ⇒ both |
| `BioFormatsAnnotatorAlgorithm` | `AnnotationsEditFunction` | **absent** | `For paths`; annotates `FileData` with OME metadata (no image data in/out) ⇒ absent |
| `ImagePropertiesToAnnotationAlgorithm` | `AnnotationsEditFunction` | both | `For images`; adds annotations from image properties; `ImagePlusData` in ⇒ both |

(All three *edit/add* annotations on existing data; none converts *between* annotation types, so
`AnnotationsConvertFunction` is unused — same outcome as ij-algorithms §19.2.)

#### (d) Aliases (12) — all dropped (alias-leaf == primary-leaf)

12 `@AddJIPipeNodeAlias` entries across 10 files. Every alias's old category maps to the **same**
function leaf as the node's primary (image import/export/contrast/ROI/table/LUT → `ImageDataFunction` /
`ExportFileFormatsFunction` / `PreprocessingFunction` / `RoiDataFunction` / `TableDataFunction` respectively,
or a same-source `DataSourceNodeTypeCategory` alias). Per the A/B/C protocol (alias-leaf == primary-leaf →
drop) **all 12 are deleted; no secondary `function[]` entries were added.**

| node | alias (old category / menuPath) | alias leaf | action |
|---|---|---|---|
| `NativeImageJImporterAlgorithm` | DataSource / — | `ImageDataFunction` | drop (== primary) |
| `NativeImageJImporterAlgorithm` | ImageJ / `File` | `ImageDataFunction` | drop (== primary) |
| `BioFormatsImporterAlgorithm` | DataSource / — | `ImageDataFunction` | drop (== primary) |
| `BioFormatsImporterAlgorithm` | ImageJ / `Plugins\nBio-Formats` | `ImageDataFunction` | drop (== primary) |
| `ImportImagePlusAlgorithm` | ImageJ / `File` | `ImageDataFunction` | drop (== primary) |
| `ImportRoi2dAlgorithm` | ImageJ / `File` | `RoiDataFunction` | drop (== primary) |
| `ImportResultsTableAlgorithm` | ImageJ / `File` | `TableDataFunction` | drop (== primary) |
| `ImportLUTAsRGBFromFileAlgorithm` | ImageJ / `File` | `ImageDataFunction` | drop (== primary) |
| `BioFormatsExporterAlgorithm` | ImageJ / `Plugins\nBio-Formats` | `ExportFileFormatsFunction` | drop (== primary) |
| `BioFormatsExporter2Algorithm` | ImageJ / `Plugins\nBio-Formats` | `ExportFileFormatsFunction` | drop (== primary) |
| `ApplyDisplayContrastAlgorithm` | ImageJ / `Image\nAdjust` | `PreprocessingFunction` | drop (== primary) |
| `DisplayRangeCalibrationAlgorithm` | ImageJ / `Image\nAdjust` | `PreprocessingFunction` | drop (== primary) |

#### (e) Flagged for Phase 3 audit

- **`ImportLUTAsRGBFromFileAlgorithm` → `ImageDataFunction`.** It is in the Images category with no
  menuPath; it *imports* a LUT as a 2D RGB image. Chosen `ImageDataFunction` (data-source/production role)
  over an images-processing leaf because it produces image data from a file rather than transforming an
  image input. Could equally be `ResamplingFunction` if the audit wants color-space importers grouped
  with converters.
- **`ImageTypeConverter` → `ResamplingFunction`.** An in-place image-type/bit-depth conversion (no file,
  parameter-driven). `ResamplingFunction` mirrors the ij-algorithms `Convert` group; `ColorStackFunction`
  or a dedicated type-conversion leaf are alternatives if the audit prefers a finer split.
- **`SetBioFormatsExporterSettings` → `ExportFileFormatsFunction`.** A settings-only pass-through (no file
  written) placed in the export family for consistency with its sibling exporters; could be
  `PreprocessingFunction`/`MiscellaneousFunction` if "not an actual exporter" is preferred.
- **`ExtractRoi2dFromOMEImageAlgorithm` → `RoiDataFunction` (both).** Leaf by its ROI output (rule 6), but
  it has an image *input* (OME) so spatial is both, not absent — consistent with the module's other
  OME-touching nodes. Flagged only because the leaf/spatial split here follows two different rules.
- **`ExportImagePlusAsOMEZARRAlgorithm` → `ExportArchiveFunction`.** Chosen because ZARR is written to a
  directory/ZIP archive; `ExportFileFormatsFunction` is a reasonable alternative if the audit treats ZARR
  as a format rather than an archive.

#### (f) Test infrastructure (R4)

- Copied `TaxonomyConversionReportTest.java` **byte-identical** into
  `plugins/jipipe-plugin-ij/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/`.
- Added `ImageJLegacyPatcherLauncherSessionListener`
  (`…/plugins/imagejdatatypes/`, `LegacyInjector.preinit()` in `launcherSessionOpened`, HKI header) — the
  gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context → the IJ1 patcher must run first.
- Added `META-INF/services/org.junit.platform.launcher.LauncherSessionListener` pointing at it.
- **`pom.xml` unchanged** — `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) were already present (Task 12); the pre-existing `ImageTypeInfoInputProfileProviderTest`
  stays green.

#### (g) Gate result (R4/R5)

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-ij -am` → exit 0.
- **Gate (enabled):** `mvn test -pl plugins/jipipe-plugin-ij -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.imagejdatatypes` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` — **zero unassigned** in the `imagejdatatypes` prefix
  and zero global taxonomy errors. (The log line "159 nodes not yet assigned a taxonomy function — expected
  during conversion" is the *global* count across the on-classpath modules — most of which are not yet
  converted — and is **not** in the prefix-filtered failure set nor in `errors()`, so the gate passes on
  this module alone, exactly as ruled.)
- **Full module (no property):** `mvn test -pl plugins/jipipe-plugin-ij` → **BUILD SUCCESS**,
  `Tests run: 2, Failures: 0, Errors: 0, Skipped: 1` — the gate **skips cleanly** (property unset) and the
  pre-existing `ImageTypeInfoInputProfileProviderTest` passes.

### 19.4 — jipipe-plugin-ij-3d mapping (62 nodes)

**Status:** converted — all 62 `@ConfigureJIPipeNode` nodes rewritten to `function`/`framework`/
`spatialDimensions`; module gate green (see Gate result below). Single-shot module conversion (mapping +
apply + gate in one commit), mirroring §19.3.

**Scope:** every `@ConfigureJIPipeNode` class under `plugins/jipipe-plugin-ij-3d/src/main/java`
(base package `org.hkijena.jipipe.plugins.ij3d` = the gate prefix). The module is the **3D ImageJ Suite
(mcib3d)** integration: Fast-3D filters, 3D segmentation/thresholding, 3D feature/measure nodes, and the
IJ3D-ROI (`Ij3dSuiteRoiListData`) measurement/filter/merge/split/convert family.

**Inventory (verified, matches controller R1):** 62 annotated files. Primary `nodeTypeCategory`
distribution: `ImagesNodeTypeCategory` **32**, `RoiNodeTypeCategory` **27**, `ExportNodeTypeCategory` **2**,
`DataSourceNodeTypeCategory` **1**. 4 nodes have no `menuPath` (the DataSource `ImportRoi3d`, and the Roi
no-menuPath `OutlineRoi3d`/`RemoveBorderRoi3d`/`Roi3dCalculator` — matches the controller's `none(4)`). `0
files import net.imagej.*` (grep) → the module framework rule is clean.

**Framework (R1 / rule 11):** `ImageJ1Framework` for **all 62 nodes** — 0 files import `net.imagej.*`
(verified), consistent with ij/ij-algorithms. (The `ClijFramework` subset named in the 19.4 brief does not
exist in this module: there is **no `clij`/`Clij`** code anywhere under `src/main`, so every node is IJ1.)

**Spatial (R2 / R3 + rule 12):** read from each node's image input slots. **Every image input in this
module is an unconstrained `ImagePlus*Data` class** — there are no `ImagePlus3DData`/`d3` (3D-constrained)
nor `ImagePlus2DData`/`d2` (2D-constrained) data types anywhere in the node slots (verified by a per-node
slot-class survey). So, per R3, no node is `SPATIAL_2D` or `SPATIAL_3D`; the module is a clean **both /
absent** split, where the split follows the source-verified ij-algorithms rule: **a node with any image
INPUT slot (required OR optional) ⇒ BOTH (`SPATIAL_2D,SPATIAL_3D`); a node with no image input slot ⇒
absent** (no `spatialDimensions` field). Output image types do not drive spatial (e.g. `RenderOverlay3D`
emits RGB but its input is `ImagePlusData`; `Roi3dToMask` emits a mask but its only image input is the
optional `Reference`).

**Distribution:** spatial **both = 45 · absent = 17 · 3D = 0 · 2D = 0** (total 62). Function leaves:
`RoiFiltersFunction` 19 · `FilteringFunction` 13 · `ThresholdingFunction` 7 · `MeasurementsFunction` 6 ·
`MergingSplittingFunction` 4 · `RoiMorphologyFunction` 4 · `ExportArchiveFunction` 2 ·
`RoiMeasurementsFunction` 2 · `RoiIntersectionsFunction` 2 · `RoiDataFunction` 1 · `SegmentationFunction` 1 ·
`ColorStackFunction` 1 (total 62).

#### (a) Images (32) — rules 1/R2: the 19.2 leaf table is the contract for shared segments

| segment (menuPath) | n | leaf | spatial | rationale for non-obvious |
|---|---|---|---|---|
| `Threshold` | 7 | `ThresholdingFunction` | both | the 7 nodes (`Constant/Difference/GaussianFit/LocalMean Spot` + `Hysteresis` + `IterativeThreshold` + `Nuclei`) all sit under the `Threshold` menu and are threshold-driven → the 19.2 `Threshold` contract (`ThresholdingFunction`). Their "spot/nuclei" naming is a 3D-suite label, not a new operation type. |
| `Math\nLocal` | 4 | `FilteringFunction` | both | Fast-3D local max/min/variance → 19.2 `Math\nLocal`→`FilteringFunction` contract |
| `Blur` | 2 | `FilteringFunction` | both | Fast-3D mean/median → 19.2 `Blur`→`FilteringFunction` |
| `Morphology` | 3 | `FilteringFunction` | both | Fast-3D open/close/top-hat → 19.2 `Morphology`→`FilteringFunction` |
| `Edges` | 2 | `FilteringFunction` | both | Fast-3D adaptive/sobel → 19.2 `Edges`→`FilteringFunction` |
| `Features` | 3 | `MeasurementsFunction` | both | `EdgeFilter3D`/`FindMaxima3D`/`SymmetryFilter3D` — feature/edge/maxima **maps** → 19.2 `Features`→`MeasurementsFunction` contract (flag Phase 3: `EdgeFilter3D`/`FindMaxima3D` read more like the 19.2 `Edges`→`FilteringFunction` group than a "measurement" — see (e)) |
| `ROI` | 4 | `MergingSplittingFunction` | both | `Extract/Remove/Render/SetOverlay3D` → 19.2 `ROI`→`MergingSplittingFunction` contract (the IJ `Image\nOverlay` menu maps to the same group, per 19.2b) |
| `Analyze` | 1 | `MeasurementsFunction` | both | `FindParticles3D` → 19.2 `Analyze`/`FindParticles2D`→`MeasurementsFunction` precedent (emits ROI + measurements, not a segmentation) |
| `Labels` | 1 | `ColorStackFunction` | both | `Roi3dFromLabels` (labels→ROI) → matches the 19.2 `Labels\nConvert`→`ColorStackFunction` row (the operative `LabelsToROI` precedent); the plain 19.2 `Labels` row is `SegmentationFunction` — judged as a labels→ROI *conversion* op (flag Phase 3) |
| `Math\nDistance map` | 2 | `MeasurementsFunction` | both | `DistanceMap3D`/`ErodedVolumeFraction3D` → 19.2 `Math\nDistance map`→`MeasurementsFunction` contract |
| `Segment` | 1 | `SegmentationFunction` | both | `Watershed3DSegmentation` → 19.2 `Segment`→`SegmentationFunction` contract |
| `Binary` | 2 | `FilteringFunction` | both | `Voronoi3D`/`Watershed3DSplitting` → 19.2 `Binary`→`FilteringFunction` contract |

#### (b) Roi (27) — rule 3 / R2 (the 19.2 Roi table is the precedent)

Grouped by `menuPath` (matches the controller inventory: Convert 6, Filter 4, Measure 3, Merge 1, Metadata 3,
Modify 5, Split 2, none 3 = 27). The `Convert`/`Filter`/`Metadata`/`Modify` rows map cleanly by the 19.2
contract; `Measure`/`Split`/no-menuPath carry per-node operation judgment (R2: Roi nodes are judged "per
what the node does").

| segment (menuPath) | n | leaf | spatial | rationale |
|---|---|---|---|---|
| `Convert` | 6 | `RoiFiltersFunction` | `Roi3dToLabels/Mask/RGB` = both (optional `Reference` image input); `Roi2dToRoi3d`/`Roi3dToRoi2d`/`Roi3dTo3DMesh` = absent (no image input) | 19.2 `Convert`→`RoiFiltersFunction`; spatial by image-input rule |
| `Filter` | 4 | `RoiFiltersFunction` | `ByOverlap`/`ByOverlapOld`/`ByStatistics` = both (optional `Reference`); `FilterRoi3dLists` = absent (no image) | 19.2 `Filter`→`RoiFiltersFunction`; source-verified: the 3 that take an (optional) reference → both, the pure-list one → absent (mirrors ij-algorithms `FilterRoi2dByOverlapOld`=both vs `FilterRoi2dByOverlap`=absent) |
| `Measure` | 3 | `ExtractRoi3dRelationStatistics` + `ExtractRoi3dStatistics` → `RoiMeasurementsFunction`; **`SplitRoi3dByStatistics` → `RoiMorphologyFunction`** (judgment, below) | all three = both (optional `Reference` image) | 19.2 `Measure`→`RoiMeasurementsFunction`; the one carve-out is a Split operation (see note) |
| `Merge` | 1 | `RoiIntersectionsFunction` | absent (no image) | 19.2 `Merge`→`RoiIntersectionsFunction` (set-union of ROI lists) |
| `Metadata` | 3 | `RoiFiltersFunction` | all absent (no image) | 19.2 `Metadata`→`RoiFiltersFunction` (precedent `ExtractROIMetadata`/`SetROIMetadataFromTable` etc. = absent, no image) |
| `Modify` | 5 | `RoiFiltersFunction` | `Change…FromExpressions`/`Color…ByStatistics` = both (optional `Reference`); `Change…(no expr)`/`Change…FromTable`/`Color…ByName` = absent | 19.2 `Modify`→`RoiFiltersFunction` (precedent: the ones with an image reference → both, the pure-list ones → absent) |
| `Split` | 2 | `RoiMorphologyFunction` | `ExplodeRoi3dList` = absent; `SplitRoi3dIntoConnectedComponents` = both (optional `Reference`) | 19.2 `Split`→`RoiMorphologyFunction` |
| *(no menuPath)* | 3 | `OutlineRoi3d` = absent → `RoiMorphologyFunction`; `RemoveBorderRoi3d` = both → `RoiFiltersFunction`; `Roi3dCalculator` = absent → `RoiIntersectionsFunction` | per row | `Outline` → 19.2 `Outline`→`RoiMorphologyFunction` (geometry-only, no image ⇒ absent); `RemoveBorder` → 19.2 `RemoveBorderRoi2d`→`RoiFiltersFunction` (required `Reference` ⇒ both); `Roi3dCalculator` → logical AND/OR/XOR (set-union) → `RoiIntersectionsFunction`, no image ⇒ absent (judgment, flag Phase 3) |

**Note on `SplitRoi3dByStatisticsAlgorithm` (the one Measure-group carve-out):** its `menuPath` is
`Measure` (so the controller's first-segment tally counts it in `Measure(3)`), but its operation is a
**split** — "Split IJ3D ROI lists by statistics" (it measures per-ROI statistics, then classifies and
emits split ROI lists; it lives in the `split/` package). Per R2's instruction to judge Roi nodes "per what
the node does" and to match the 19.2 sibling `SplitRoi2dByStatisticsAlgorithm` (`Split`→`RoiMorphologyFunction`),
it is assigned `RoiMorphologyFunction`, not the menuPath-literal `RoiMeasurementsFunction`. Flagged for
Phase 3 (see (e)).

#### (c) Export (2) + DataSource (1) — rules 6/7

| node | leaf | spatial | rationale |
|---|---|---|---|
| `ExportRoi3dAlgorithm` | `ExportArchiveFunction` | absent (no image) | writes `Ij3dSuiteRoiListData.save()` → a **ZIP** (data type is defined `glob:./*.zip`, "IJ3D Suite 3D ROI ZIP") → archive leaf (rule 7), mirroring ij-algorithms `ROI` export→`ExportArchiveFunction` |
| `ExportRoi3dAlgorithm2` | `ExportArchiveFunction` | absent (no image) | same `.zip`/`.roi3d` archive writer (single-path variant) → `ExportArchiveFunction` |
| `ImportRoi3dAlgorithm` | `RoiDataFunction` | absent (no image) | DataSource → data leaf by OUTPUT type (rule 6); emits `Ij3dSuiteRoiListData` from a `*.zip` → `RoiDataFunction`, no image input ⇒ absent (mirror of 19.3 `ImportRoi2dAlgorithm`) |

#### (d) Aliases (3) — all dropped (alias-leaf == primary-leaf)

3 `@AddJIPipeNodeAlias` entries. Per the first-segment rule (alias IJ-menu → JIPipe group → that group's
leaf): `Image\nOverlay` → 19.2 `ROI`→`MergingSplittingFunction` (== both overlay nodes' primary);
`Analyze` → ROI-measure family → `RoiMeasurementsFunction` (== `ExtractRoi3dStatistics`'s primary). All
three therefore map to a leaf identical to their primary's leaf and are **dropped** (no secondary
`function[]` entries added), exactly as in §19.3.

| node (primary leaf) | alias (old category / menuPath) | alias leaf | action |
|---|---|---|---|
| `ExtractOverlay3DAlgorithm` (MergingSplittingFunction) | ImageJ / `Image\nOverlay` | MergingSplittingFunction | drop (== primary; 19.2 `ROI`→MergingSplitting) |
| `RemoveOverlay3DAlgorithm` (MergingSplittingFunction) | ImageJ / `Image\nOverlay` | MergingSplittingFunction | drop (== primary) |
| `ExtractRoi3dStatisticsAlgorithm` (RoiMeasurementsFunction) | ImageJ / `Analyze` | RoiMeasurementsFunction | drop (== primary; `Analyze` = ROI-measure family) |

Note: the `Analyze` alias on `ExtractRoi3dStatistics` — the primary leaf is `RoiMeasurementsFunction`
(rule 3), and the IJ `Analyze` menu in the ROI context is the "measure ROIs" operation, so the alias leaf
resolves to `RoiMeasurementsFunction` (not the image-side `MeasurementsFunction`), i.e. == primary → dropped.

#### (e) Flagged for Phase 3 audit

- **`EdgeFilter3D` / `FindMaxima3D` → `MeasurementsFunction`.** Kept under the `Features` segment's 19.2
  contract (`Features`→`MeasurementsFunction`). But operationally they read like the 19.2 `Edges`→
  `FilteringFunction` / `Math\nLocal`→`FilteringFunction` families (edge/maxima **filtering**, not
  table-style measurement). If the audit groups by operation rather than menu segment, both move to
  `FilteringFunction`.
- **`SymmetryFilter3D` → `MeasurementsFunction`** — same segment-contract reasoning; its multiple outputs
  (symmetry / edges / bin / binedge) blur the filtering-vs-measurement line.
- **`FindParticles3D` → `MeasurementsFunction`** — follows the 19.2 `FindParticles2D` precedent (which
  itself is flagged). Emits an ROI list + measurements; `SegmentationFunction` is a reasonable alternative
  if the audit treats particle finding as object segmentation.
- **`Roi3dFromLabels` → `ColorStackFunction`** — follows the 19.2 `Labels`→`ColorStackFunction` contract
  (flagged there too). It produces an ROI list from a label image (labels→ROI), so `RoiDataFunction`-style
  or a labels-convert leaf are defensible alternatives.
- **`Roi3dCalculator` → `RoiIntersectionsFunction`** — the AND/OR/XOR "logical operation over the whole
  list" is set-union/intersection semantics; `RoiFiltersFunction` is the alternative if "calculator" is
  read as a generic list modifier rather than a set operation.

#### (f) Test infrastructure (R4)

The module had **no `src/test` and no JUnit deps**. Wired:
- **`pom.xml`**: added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) — mirrors `plugins/jipipe-plugin-ij/pom.xml` verbatim.
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-ij-3d/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/` (**byte-identical**,
  verified with `cmp`).
- Created `plugins/jipipe-plugin-ij-3d/src/test/java/org/hkijena/jipipe/plugins/ij3d/
  ImageJLegacyPatcherLauncherSessionListener.java` — the module-scoped mirror of the ij/ij-algorithms
  listeners (`LegacyInjector.preinit()` in `launcherSessionOpened`, HKI header, Javadoc adapted to this
  module's package).
- Created `plugins/jipipe-plugin-ij-3d/src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = the listener's FQN).

**Why the listener is needed:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A module's
test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent failure
(AGENTS.md patcher contract). The ij-3d nodes reference `ij.ImagePlus`/mcib3d heavily, so this is required.

#### (g) Gate result (R4/R5)

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-ij-3d -am` → **exit 0**.
- **Gate (enabled):** `mvn test -pl plugins/jipipe-plugin-ij-3d -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ij3d` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` — **zero unassigned** in the `org.hkijena.jipipe.plugins.ij3d`
  prefix and zero taxonomy `errors()`. The log line `JIPipeNodesServiceComponent | Node taxonomy: 619 nodes
  not yet assigned a taxonomy function — expected during conversion` is the **global** count across all
  on-classpath modules (most of which are not yet converted); it is *outside* the prefix-filtered failure
  set and is not in `errors()`, so the gate passes on this module alone, exactly as ruled (same shape as
  §19.3(g)).
- **Full module (no property):** `mvn test -pl plugins/jipipe-plugin-ij-3d` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1` — the gate **skips cleanly** (property unset).

### 19.5 — jipipe-plugin-ij-filaments mapping (51 nodes)

**Status:** converted — all 51 `@ConfigureJIPipeNode` nodes rewritten to `function`/`framework`/
`spatialDimensions`; module gate green (see Gate result below). Single-shot module conversion (mapping +
apply + gate in one commit), mirroring §19.3/§19.4. **Note:** the plan's "jipipe-plugin-filaments +
ij-filaments (51+)" is ONE module — `plugins/jipipe-plugin-ij-filaments` (there is no separate
`jipipe-plugin-filaments` maven module; the filament *data type* `Filaments3DGraphData` also lives here).

**Scope:** every `@ConfigureJIPipeNode` class under `plugins/jipipe-plugin-ij-filaments/src/main/java`
(base package `org.hkijena.jipipe.plugins.ijfilaments` = the gate prefix). The module is the **filament
graph toolkit**: TSOAX snake analysis, skeleton→filament extraction, and graph editing (filter/modify/
merge/split/measure) over `Filaments3DGraphData` ("Stores 3D filaments as graph", `extends SimpleGraph`).

**Inventory (verified, matches controller R1):** 51 annotated files. Primary `nodeTypeCategory`
distribution: `FilamentsNodeTypeCategory` **42**, `ImagesNodeTypeCategory` **6**,
`MiscellaneousNodeTypeCategory` **1** (`TSOAXResultAnalysis`), `RoiNodeTypeCategory` **1**
(`ConvertROIToFilaments`), `DataSourceNodeTypeCategory` **1** (`ImportFilamentsFromJson`). The 42
filament-category nodes' `menuPath` segments: Modify **19**, Convert **8**, Process **5**, Filter **4**,
Split **3**, Measure **3**, none **1** (`ImportFilamentsFromJson`, which is actually DataSource). **0
files import `net.imagej.*`** (grep) → framework rule is clean. **0 `@AddJIPipeNodeAlias`** in the module.

**The `FilamentsNodeTypeCategory` discovery (matches the Task-14 note in the Known category inventory):**
the class is **defined in this module** at `…/ijfilaments/FilamentsNodeTypeCategory.java`
(id `org.hkijena.jipipe:filaments`, "Operations on filaments") — it is plugin-local, not one of the 12
rules' core categories, so its 42 nodes are mapped rule-9-style **best-fit into the three `filaments/`
leaves** (the `data/` filament leaf covers the DataSource by rule 6). The class file is **not** deleted in
this task (it is a definition, not a `nodeTypeCategory =` usage; residue checks target usages only); it is
torn out in Task 27's category rip-out.

**Framework (R1 / rule 11):** `ImageJ1Framework` for **all 51 nodes** — 0 files import `net.imagej.*`
(verified), consistent with ij/ij-algorithms/ij-3d.

**Spatial (R3 / rule 12 — settled ruling):** read from each node's **first image INPUT slot only** (a
graph slot confers nothing — "first image input slot" = the first slot that is an image).
**`Filaments3DGraphData` is a graph, not an image — it confers NO spatial** (same absent-precedent as
ROI-list/table/file/string inputs in prior modules). So: 3D-constrained image class ⇒ `SPATIAL_3D`;
unconstrained `ImagePlus*` class ⇒ both; no image input ⇒ **absent**. The module has **19 image-input
nodes — all non-absent (18 both + 1 3D)**; the **32 graph/table/ROI/file-only nodes are absent — that is
CORRECT** (flagged for Phase 3: should spatial ever extend to non-image 3D data types? do NOT change the
semantics in this task).

**Distribution:** spatial **absent = 32 · both = 18 · 3D = 1 · 2D = 0** (total 51). Function leaves:
`FilamentFilteringFunction` **32** · `FilamentDataFunction` **8** · `FilamentSmoothingFunction` **7** ·
`FilamentQuantificationFunction` **3** · `MergingSplittingFunction` **1** (total 51).

#### (a) The 42 `FilamentsNodeTypeCategory` nodes → the three `filaments/` leaves (R2)

Leaf rule (per controller R2): smoothing/straightening/simplifying/resampling the **curves** ⇒
`FilamentSmoothingFunction`; filtering/extracting/selecting/splitting/joining/removing filaments or curve
pieces (structural edits on the filament set) ⇒ `FilamentFilteringFunction`; measuring/quantifying
properties, statistics, `ResultsTableData` outputs ⇒ `FilamentQuantificationFunction`.

**`FilamentSmoothingFunction` (7):**

| node (segment) | spatial | rationale |
|---|---|---|
| `SmoothFilamentsAlgorithm` (Process) | absent | "Applies a **smoothing** operation" on vertex locations — the namesake of the leaf |
| `SimplifyFilamentsAlgorithm` (Process) | absent | "Removes all intermediate vertices (degree 2)" — curve simplification per R2's explicit example ("a 'simplify' curve op = Smoothing") |
| `FixOverlapsNonBranchingAlgorithm` (Process) | **both** | "fix filaments that are **merged together** by junctions" — straightening/separating overlapping **curves** (non-branching); R2's "remove overlapping filaments = Filtering" is the alternative → **flagged (c)**; optional `ImagePlusGreyscaleMaskData` *Mask* input ⇒ both |
| `RemoveDuplicateVerticesAlgorithm` (Process) | absent | "removes all duplicates. Edges are preserved" — coalescing coincident **curve points**, not removing filament pieces; `FixOverlaps`' doc names this exact operation as its smoothing step; R2 alternative: Filtering → **flagged (c)** |
| `SubdivideFilamentEdges` (Modify) | absent | "Inserts a vertex at the center of edges. The connectivity and shape is preserved" — curve **resampling/densification** (R2's resampling bucket) |
| `FlattenFilamentsAlgorithm` (Modify) | absent | "Moves the Z of each vertex to zero … flattens 3D filaments into 2D" — 3D→2D **curve straightening** (no structural piece added/removed) |
| `GrowEndVerticesAlgorithm` (Modify) | absent | "adds vertices to end points … grows them in the direction determined by the neighbor" — extends the **curve** beyond its endpoints (smooth/straight continuation); alternative: Filtering → **flagged (c)** |

**`FilamentQuantificationFunction` (3):**

| node (segment) | spatial | rationale |
|---|---|---|
| `MeasureEdgesAlgorithm` (Measure) | absent | "Stores all available information about the edges … into a **table**" (`ResultsTableData` out) — R2's measure/table bucket |
| `MeasureFilamentsAlgorithm` (Measure) | absent | "Measures all components … produces measurements: number of vertices/edges/length/…" (`ResultsTableData` out) |
| `MeasureVerticesAlgorithm` (Measure) | absent | "Stores all available information about the vertices into a **table**" (`ResultsTableData` out) |

**`FilamentFilteringFunction` (32):**

| node (segment) | spatial | rationale |
|---|---|---|
| `FilterFilamentEdgesByProperties` (Filter) | absent | "Filters filament **edges** by various properties" — structural filtering of edge pieces |
| `FilterFilamentVerticesByProperties` (Filter) | absent | "Filters filament **vertices** by various properties" |
| `FilterFilamentsByProperties` (Filter) | absent | "Filters filament **connected components** by various properties" |
| `RemoveBorderFilaments` (Filter) | **both** | "Removes filament components near the image borders" — R2's "remove filaments" bucket (spatial from the required `ImagePlusData` reference slot) |
| `ErodeEndVerticesAlgorithm` (Modify) | absent | "Iteratively **removes** vertices that have a degree of at most 1" — removes curve pieces (opposite of `GrowEndVertices` → Smoothing; the pair is kept consistent) |
| `ConnectVerticesAlgorithm` (Modify) | **both** | "Connect existing vertices based on customizable criteria" (expression-filtered candidate edges) — joins curve pieces (optional `ImagePlusGreyscaleMaskData` *Mask* = first image input ⇒ both) |
| `ConnectVerticesFastAlgorithm` (Modify) | **both** | same operation, distance-based fast variant |
| `SplitFilamentsIntoConnectedComponentsAlgorithm` (Split) | absent | "Splits the filament graph into connected components" — R2's split bucket |
| `SplitFilamentsIntoCyclesAlgorithm` (Split) | absent | "Splits the filament graph into cycles" |
| `FilamentCyclesToComponentsAlgorithm` (Modify) | absent | "makes each cycle a component" — re-splits the graph at cycles (its doc explicitly distinguishes this from `Split … into cycles`) |
| `MergeFilamentsAlgorithm` (Split) | absent | "Merges multiple filament graphs into one" — R2's join bucket; menuPath `Split` is the old panel's group, not the operation (same carve-out pattern as §19.4's `SplitRoi3dByStatistics`) |
| `CopyFilamentsAcrossZCTAlgorithm` (Process) | **both** | "Copies a filament graph to other Z locations, channels, and frames" — duplicates curve pieces (spatial: optional `ImagePlusData` *Reference* = first image input ⇒ both; the graph slot confers nothing) |
| `ConvertFilamentsToRoiAlgorithm` (Convert) | absent | "Converts filaments into 2D line ImageJ ROI" — emits an ROI list, no image input |
| `ConvertFilamentCyclesToROIAlgorithm` (Convert) | absent | "Finds all cycles … converts each cycle into a 2D ROI" — cycle extraction + ROI output |
| `ConvertFilamentsToRoi3dAlgorithm` (Convert) | absent | "Converts filaments into a 3D ROI" (`Ij3dSuiteRoiListData` out) |
| `ConvertFilamentsToMaskAlgorithm` (Convert) | **both** | "Converts filaments into a mask" (`ImagePlusGreyscaleMaskData` out); required image input = the `ImagePlusData` *Reference* that determines mask size |
| `ConvertFilamentsToLabelsAlgorithm` (Convert) | **both** | "Converts filaments into a labels image" (legacy, 3D-ROI intermediate) |
| `ConvertFilamentsToLabels2Algorithm` (Convert) | **both** | "Converts filaments into a labels image" (non-legacy) |
| `ConvertFilamentsToRGBAlgorithm` (Convert) | **both** | "Visualizes filaments by rendering them onto an RGB image" (`ImagePlusColorRGBData` out) |
| `ConvertFilamentsTo3DMeshAlgorithm` (Convert) | absent | "Converts 3D filaments into a 3D scene" (`Scene3DData` out — a scene, not an image ⇒ no spatial per R3) |
| `ChangeFilamentEdgePropertiesExpressionAlgorithm` (Modify) | absent | "override various properties of the **edges**" using expressions — attribute edit, no curve/structure change (flagged (c): no "property editing" leaf exists) |
| `ChangeFilamentEdgePropertiesManuallyAlgorithm` (Modify) | absent | "override various properties of the **edges**" (manual) |
| `ChangeFilamentVertexPropertiesExpressionAlgorithm` (Modify) | absent | "override various properties of the **vertices**" using expressions |
| `ChangeFilamentVertexPropertiesManuallyAlgorithm` (Modify) | absent | "override various properties of the **vertices**" (manual) |
| `SetFilamentMetadataToComponentIndexAlgorithm` (Modify) | absent | "writes the component index into a metadata field" — metadata edit |
| `SetVertexMetadataFromImageAlgorithm` (Modify) | **both** | "Sets the vertex metadata … from the given input image" — writes into the graph; required `ImagePlusGreyscaleData` *Value* = first image input ⇒ both (graph slot confers nothing) |
| `SetVertexPhysicalSizeFromImageAlgorithm` (Modify) | **both** | "Sets the physical voxel sizes of all vertices from the image" — required `ImagePlusGreyscaleData` first image input ⇒ both |
| `SetVertexRadiusFromImageAlgorithm` (Modify) | **both** | "Sets the radius of each vertex from the image" — required `ImagePlusGreyscaleData` first image input ⇒ both |
| `SetVertexValueFromImageAlgorithm` (Modify) | **both** | "Sets the value/intensity of each vertex from the image" — required `ImagePlusGreyscaleData` first image input ⇒ both |
| `RemoveVertexValueBackupsAlgorithm` (Modify) | absent | "Removes value **backups** from vertices" — metadata removal (flagged (c): "removes" here targets backup metadata, not filament pieces) |
| `RestoreVertexValueBackupAlgorithm` (Modify) | absent | "Restore value backups from vertices" — metadata restoration |
| `FindLinearFilamentVertexOrderAlgorithm` (Modify) | absent | "find the order each vertex in that line … calculate the distance" — writes order/distance onto the graph (quantification-flavored but graph-in ⇒ Filtering; flagged (c)) |

**`data/FilamentDataFunction` (from the 42? no — see (b)):** `ImportFilamentsFromJson` (DataSource),
`ConvertROIToFilaments` (Roi) and the 6 Images-category nodes (except `SetOverlayFilaments`) — rule 6 /
R2: filament-**data producers** belong to the data/ filament leaf.

#### (b) The 9 non-`FilamentsNodeTypeCategory` nodes (R2)

| node (old category / segment) | leaf | spatial | rationale |
|---|---|---|---|
| `ImportFilamentsFromJsonAlgorithm` (DataSource, no menuPath) | `FilamentDataFunction` | absent | R2/rule 6: data leaf by OUTPUT = `Filaments3DGraphData` ⇒ the data/ filament leaf; `FileData` in ⇒ no spatial (mirror of §19.3 `ImportRoi2d`) |
| `ConvertROIToFilamentsAlgorithm` (Roi / Convert) | `FilamentDataFunction` | absent | R2: "converts 2D ROI into equivalent filaments" — a **filament-data producer**; the node belongs to the filament branch (its output is `Filaments3DGraphData`), so the data/ filament leaf over a rois/ leaf (flagged (c): a rois/ leaf is defensible) |
| `SkeletonToFilaments2DAlgorithm` (Images / Convert) | `FilamentDataFunction` | **both** | "converts a binary skeleton into a filament … per Z/C/T slice (2D data)" — skeleton→filament **extraction** (image in ⇒ filament graph out); R2's images-branch suggestion (`SegmentationFunction`) is the alternative for extraction ops — chosen data/ leaf because the node's identity/branch is "filament production from a mask" (flagged (c)); `ImagePlusData` in ⇒ both |
| `SkeletonToFilaments3DAlgorithm` (Images / Convert) | `FilamentDataFunction` | **both** | same node family, 3D variant ("per C/T stack"); `ImagePlusData` in ⇒ both |
| `SkeletonToSimplifiedFilamentsFijiAlgorithm` (Images / Convert) | `FilamentDataFunction` | **3D** | "Extract filaments using Analyze Skeleton 2D/3D" (Fiji) — same extraction op; first input slot is the 3D-constrained `ImagePlus3DGreyscaleMaskData` *Skeleton* ⇒ `SPATIAL_3D` (the only 3D node in the module) |
| `TSOAX2DAlgorithm` (Images / Analyze) | `FilamentDataFunction` | **both** | "Find and track filaments 2D+T (TSOAX)" — runs the TSOAX snake algorithm on a **greyscale** image to find/track filament paths (R2: arguably `FilteringFunction`/`SegmentationFunction` — flagged (c)); outputs filaments + snake table; `ImagePlusGreyscaleData` in ⇒ both |
| `TSOAX3DAlgorithm` (Images / Analyze) | `FilamentDataFunction` | **both** | 3D+T TSOAX variant; same ruling (flagged (c)); `ImagePlusGreyscaleData` in ⇒ both |
| `TSOAXResultAnalysisAlgorithm` (Miscellaneous / Analyze) | `FilamentDataFunction` | absent | "Parses existing results generated by TSOAX" — converts TSOAX's raw text output (`StringData` in) into a filament graph + table; R2's `FilamentQuantificationFunction` alternative rejected: it does not *quantify* an existing filament graph, it (re)constructs one from raw results (flagged (c)); no image input ⇒ absent |
| `SetOverlayFilamentsAlgorithm` (Images / ROI) | `MergingSplittingFunction` | **both** | "Set filaments overlay" on an image — the 19.2 `ROI`→`MergingSplittingFunction` contract (same group as §19.4's `Extract/Remove/SetOverlay3D`); `ImagePlusData` in ⇒ both (R2's `MiscellaneousFunction` alternative is the flagged escape hatch) |

#### (c) Flagged for Phase 3 audit

- **`FixOverlapsNonBranching` → `FilamentSmoothingFunction`.** "fix filaments that are merged together by
  junctions" reads as curve separation/straightening (Smoothing); R2's literal example
  ("remove overlapping filaments" = Filtering) would put it in `FilamentFilteringFunction`.
- **`RemoveDuplicateVertices` → `FilamentSmoothingFunction`.** Coalescing coincident curve points (the
  `FixOverlaps` doc names this operation as its smoothing step) vs. R2's "removing … curve pieces" =
  Filtering.
- **`GrowEndVertices` → `FilamentSmoothingFunction`.** Curve extension vs. R2's "removing … curve pieces"
  (Filtering). Its pair `ErodeEndVertices` is Filtering (removal) — if the audit prefers symmetry by
  "endpoint editing", `Grow` could move too.
- **`ChangeFilament{Edge,Vertex}Properties{,Expression,Manually}` (4) + `SetFilamentMetadataToComponentIndex`
  + `SetVertex{Metadata,PhysicalSize,Radius,Value}FromImage` (4) + `RemoveVertexValueBackups` +
  `RestoreVertexValueBackup` (11 total) → `FilamentFilteringFunction`.** Attribute/metadata edits with no
  curve or structure change; **no "property editing" leaf exists** in the filaments/ branch, so they take
  the structural-edit leaf as the best fit. If the audit adds a filaments "properties/
  metadata" leaf, all 11 move.
- **`FindLinearFilamentVertexOrder` → `FilamentFilteringFunction`.** Computes vertex order/distance and
  writes it back onto the graph (quantification-flavored) but produces no table ⇒ Filtering by
  elimination (flagged).
- **`RemoveVertexValueBackups` → `FilamentFilteringFunction`.** "Removes" refers to backup **metadata**,
  not filament pieces (recorded so a reviewer doesn't misread the name as Filtering-by-removal).
- **The 6 skeleton/TSOAX `generate/` nodes → `FilamentDataFunction`.** R2 suggested judging the
  image-side ops per `images/` leaf (`SegmentationFunction` for mask→skeleton→filament extraction;
  `FilteringFunction` for a TSOAX on a greyscale image). Chosen the **data/ filament leaf** for all six:
  every one of them is a *filament-graph producer* and the branch the node "belongs to" (R2's
  ConvertROIToFilaments criterion) is filaments, not images — the output `Filaments3DGraphData` makes the
  filament identity unambiguous, while the input-side operation (skeleton extraction / snake filtering) is
  incidental to that identity. `TSOAX2D/3D` in particular (greyscale in, no mask) read equally well as
  `images/FilteringFunction` — flagged. `SetOverlayFilaments` is the one Images node that is *not* a
  producer (it edits an image) ⇒ `MergingSplittingFunction` per the 19.2 `ROI` contract.
- **`ConvertROIToFilaments` → `FilamentDataFunction`.** R2 offered a rois/ leaf alternative; the data/
  leaf was chosen by output-branch identity (flagged).
- **`TSOAXResultAnalysis` → `FilamentDataFunction`.** Reconstructs filaments from TSOAX raw output text
  (flagged; `FilamentQuantificationFunction` was the R2 alternative if it "quantifies filaments" — it
  parses, it does not measure an existing graph).
- **Spatial for non-image 3D data types (module-wide question).** 32/51 nodes are spatial-**absent**
  because their only data inputs are `Filaments3DGraphData` (a graph — not an image) plus
  table/ROI/file/string. Per R3 this is
  settled for this task; **Phase 3 should decide whether spatial should ever extend to non-image 3D data
  types** (e.g. filaments, scenes, 3D ROI lists). Do NOT change the semantics here.
- **Spatial rule interpretation.** R3's "first IMAGE input slot" is read as "the first slot that is an
  image" (graph slots confer no spatial). Applied uniformly, **all 19 image-input nodes are non-absent
  (18 both + 1 3D)**; the 32 graph/table/ROI/file-only nodes are absent. For the 18 both-nodes whose
  image is a slot *behind* the required graph slot (e.g. `RemoveBorderFilaments`: required graph + required
  `ImagePlusData` reference ⇒ both; the `SetVertex*FromImage` nodes: required graph + required
  `ImagePlusGreyscaleData` ⇒ both) this reading is what drives the assignment. If "first input slot" were
  read literally slot-order-wise, those nodes would flip to absent — recorded so the audit sees the
  interpretation.

#### (d) Test infrastructure (R5)

The module had **no `src/test` and no JUnit deps** (first test in this module). Wired:
- **`pom.xml`**: added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) — mirrors `plugins/jipipe-plugin-ij/pom.xml`/`plugins/jipipe-plugin-ij-3d/pom.xml` verbatim.
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-ij-filaments/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/`
  (**byte-identical**, verified with `cmp`).
- Created `plugins/jipipe-plugin-ij-filaments/src/test/java/org/hkijena/jipipe/plugins/ijfilaments/
  ImageJLegacyPatcherLauncherSessionListener.java` — the module-scoped mirror of the ij/ij-3d listeners
  (`LegacyInjector.preinit()` in `launcherSessionOpened`, HKI header, Javadoc adapted: cites this
  module's ImageJ1 touchpoints — `ij.ImagePlus` + the Fiji `AnalyzeSkeleton_` plugin).
- Created `plugins/jipipe-plugin-ij-filaments/src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = the listener's FQN).

**Why the listener is needed:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent failure
(AGENTS.md patcher contract). This module's nodes/data types reference `ij.ImagePlus` and the Fiji
`AnalyzeSkeleton_` plugin heavily, so it is required.

#### (e) Gate result (R5)

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-ij-filaments -am` → **exit 0**.
- **Gate (enabled):** `mvn test -pl plugins/jipipe-plugin-ij-filaments -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ijfilaments` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` — **zero unassigned** in the
  `org.hkijena.jipipe.plugins.ijfilaments` prefix and zero taxonomy `errors()`. The log line
  `JIPipeNodesServiceComponent | Node taxonomy: 680 nodes not yet assigned a taxonomy function — expected
  during conversion` is the **global** count across all on-classpath modules (most of which are not yet
  converted); it is *outside* the prefix-filtered failure set and is not in `errors()`, so the gate passes
  on this module alone, exactly as ruled (same shape as §19.3(g)/§19.4(g)). (An unrelated
  pre-existing `Unregistered parameter found: class ij.process.AutoThresholder$Method …
  NucleiSegmentation3DAlgorithm` ERROR log line comes from the dependency `jipipe-plugin-ij-3d`'s
  converted node, not from this module.)
- **Full module (no property):** `mvn test -pl plugins/jipipe-plugin-ij-filaments` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1` — the gate **skips cleanly** (property unset).


### 19.6 — jipipe-plugin-filesystem + jipipe-plugin-strings + jipipe-plugin-utils (82 nodes)

**Status:** converted — all 82 `@ConfigureJIPipeNode` nodes across the three modules rewritten to
`function`-only; three module gates green (see Gate result below). Single-shot combined conversion
(mapping + apply + 3 gates in one commit), mirroring §19.3/§19.4/§19.5.

**Scope / gate prefixes (verified, matches controller R1):**
- `plugins/jipipe-plugin-filesystem/src/main` — base pkg `org.hkijena.jipipe.plugins.filesystem` — **39** annotated files.
- `plugins/jipipe-plugin-strings/src/main` — base pkg `org.hkijena.jipipe.plugins.strings` — **15** annotated files.
- `plugins/jipipe-plugin-utils/src/main` — base pkg `org.hkijena.jipipe.plugins.utils` — **28** annotated files.

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):**

| module | DataSource | FileSystemNodeTypeCategory | Miscellaneous | Export | Annotations | Table | total |
|---|---|---|---|---|---|---|---|
| filesystem | 10 | 16 | 1 | 6 | 5 | 1 | **39** |
| strings | 4 | — | 7 | — | 3 | 1 | **15** |
| utils | 1 | — | 24 | — | 3 | — | **28** |

(`FileSystemNodeTypeCategory` is a **core** category class at `jipipe-core/…/api/nodes/categories/
FileSystemNodeTypeCategory.java` — rule 8 — not module-local like the 19.5 `FilamentsNodeTypeCategory`.)

**Aliases (verified by grep of `@AddJIPipeNodeAlias`):** filesystem **3 refs in 2 files**
(`ProjectUserPathDataSource` ×2, `SimplePathAnnotationGenerator` ×1); utils **4 refs in 2 files**
(`MultiIterationStepCheckerAlgorithm` ×2, `SingleIterationStepCheckerAlgorithm` ×2); strings **0**.
Total **7** alias references → decisions in (d).

---

**Framework — OMITTED for all 82 (rule 11, deliberate, not an omission).** None of these three modules
is on rule 11's named list (cellpose/trackmate/clij/opencv/plots/weka/ilastik/omnipose/ij2-discovered/
ij/ij-3d/ij-algorithms), and they are **not** "core plugins" (they are standalone plugin modules, not
bundled in jipipe-core). Rule 11's final clause — "else leave `NoFramework`" — therefore applies. The
`@ConfigureJIPipeNode.framework()` default is **already** `NoFramework.class`
(`jipipe-core/…/api/ConfigureJIPipeNode.java:97`), so the attribute is **omitted entirely** — no node
here writes `framework = NoFramework.class` (this is the mirror of why the 19.1 core nodes write
`framework = CoreFramework.class` but these do not). `grep -rl "framework = " src/main` across all three
modules → **0**.

**Spatial — ABSENT for all 82 (rule 12, deliberate, not an omission).** None of the 82 nodes has an
image input slot (verified by a per-node slot survey: zero `ImagePlus*`/`Image*`/`OME*`/`ImageData*`
input values across the three modules; the closest is a generic `JIPipeData` slot, which confers no
spatial per the §19.5 graph-slot precedent). Rule 12's "non-image ⇒ absent" therefore applies to every
node: `spatialDimensions` is **omitted entirely** on all 82, and there is **no** `SPATIAL_*` static
import anywhere in the three modules. `grep -rl "spatialDimensions = " src/main` across all three → **0**.

So every converted node's annotation is exactly `@ConfigureJIPipeNode(function = <Leaf>.class)`, or
`@ConfigureJIPipeNode(function = { Primary.class, SecondaryLeaf.class })` where an alias added a
distinct secondary leaf. This is the simplest shape to date — but the `function` attribute is never
dropped (it is the whole point of the conversion).

**Leaf distribution (primary, all 82):** `MiscDataFunction` 29 · `MiscellaneousFunction` 17 ·
`AnnotationsEditFunction` 10 · `TableDataFunction` 7 · `ExportFileFormatsFunction` 6 ·
`TableFilterModifyFunction` 4 · `GraphDebugFunction` 3 · `TableJoinSplitFunction` 2 ·
`TableTransformFunction` 2 · `AnnotationsConvertFunction` 1 · `GraphWrappersFunction` 1 (total 82).

---

#### (a) jipipe-plugin-filesystem (39)

Rule 8 (`FileSystemNodeTypeCategory` → data-source leaf by what it reads, else `MiscDataFunction`);
rule 6 (DataSource → data leaf by output); rule 7 (Export); rule 5 (Annotations); rule 4 (Table).

**`MiscDataFunction` (25)** — the filesystem/path core. All read and/or emit `PathData` /
`FileData` / `FolderData` / `JIPipeData` / `URIData` (no dedicated path/file/folder data leaf exists;
`data/` = Image/Table/Roi/Filament/**Misc**). Assigned per the actual data role, not the menuPath
(`Modify`/`Convert`/`ZARR`/`Extract`/`List`/`Filter` are filesystem operations, not taxonomy ops):

| node (old segment) | rationale for non-obvious |
|---|---|
| `ConcatenatePaths`, `ConcatenateByParameter`, `ModifyPath`, `ModifyPathWithExpression`, `RenameByString`, `RelativizePaths`, `RelativizeByParameter` (Modify) | path-string edits (rule 8 "else Misc") |
| `ExtractFileName`, `ExtractParent` (Extract) | path decomposition → path/file data |
| `FilterPaths` (Filter) | path predicate removal |
| `CopyPath` | copies a file to a destination, emits the copied `PathData` (a path producer, not a "copy data" op) |
| `CreateDirectory` | emits `FolderData` |
| `ListFiles`, `ListSubfolders` (List) | emit `FileData`/`FolderData` lists |
| `ConvertDataToOutputPath` (Misc/Convert, "Get output path") | computes the run's output path, emits `FolderData` (a data producer); **flag (e)** — could be export-flavored |
| `DownloadFilesDataSource`, `OutputFolderDataSource`, `PathFromUserDataSource`, `ProjectUserPathDataSource`, `TemporaryFileDataSource`, `TemporaryFolderDataSource` (DataSource) | rule 6 by output (`FileData`/`FolderData`/`PathData`) → Misc |
| `URIListDataSource` (DataSource) | emits `URIData` (a `StringData` subclass) → Misc (no image/table/ROI) |
| `ImportDataRowFolder` (DataSource) | emits generic `JIPipeData` (one row from a row-folder) → Misc |

**`TableDataFunction` (2)** — rule 8/6 by OUTPUT type: `ListZARRDirectoryZIPDatasetsAlgorithm`,
`ListZARRURIDatasetsAlgorithm` (both `ZARR` segment) read a ZARR store and emit **`ResultsTableData`**
(the dataset/group listing as a table) → the data/ table leaf (a refinement of rule 8's "leaf by what
it reads/emits", mirroring §19.3's `ImportResultsTable`→`TableDataFunction`).

**`TableTransformFunction` (1)** — `AnnotationTableToPaths` (Table/Convert): input is
**`AnnotationTableData`** (a `ResultsTableData` subclass), output `PathData`. Rule 4 (Table category →
`tables/`) wins because it *transforms a table* into paths (the annotation table is the primary data it
consumes); **flag (e)** — MiscDataFunction is the alternative if judged by output (rule 8).

**`ExportFileFormatsFunction` (6)** — rule 7: `ExportData`, `ExportDataByParameter`,
`ExportDataByParameter2`, `ExportDataTable`, `ExportDataTableByParameter` (all write files via
JIPipe's standard exporter → the file-format export leaf; the two DataTable variants write a data-table
*directory*, still a file export, not a ZIP archive), plus `ConvertToExportedPath` ("Get export path" —
it does **not** write a file, it generates the export `PathData`; kept in the export family mirroring
§19.3's `SetBioFormatsExporterSettings`; **flag (e)**).

**`AnnotationsEditFunction` (4)** — rule 5: `AnnotateWithPathProperties` (For paths) adds property
annotations to paths (an *adder*); `SimpleFileAnnotationGenerator`, `SimpleFolderAnnotationGenerator`,
`SimplePathAnnotationGenerator` (all For paths) each **create/add** one annotation (e.g. `#Dataset`) to
files/folders/paths — annotation *editing/adding* ops (same Edit leaf as §19.3's
`AnnotateDataWithImageProperties`), not type-conversions. `SimplePathAnnotationGenerator` carries 1
alias (see (d)).

**`AnnotationsConvertFunction` (1)** — `AnnotationToPath` (Annotations/Convert): converts a data
annotation (key `#Dataset`) into `PathData` — a genuine annotation→other-data **conversion** (the one
node here that maps an annotation *type* to a different data type), hence the Convert leaf.

#### (b) jipipe-plugin-strings (15)

Rule 6 (DataSource), rule 5 (Annotations), rule 4 (Table), rule 9 (Miscellaneous best-fit). The module
wraps `StringData` and its subclasses (`XMLData`, `JsonData`, `URIData`, `TextData`…) — no image data.

**`MiscDataFunction` (4)** — the four DataSource nodes (rule 6 by output; there is **no** dedicated
string/text data leaf, so `data/` → Misc, mirroring §19.3's `DefineImageJMacroAlgorithm`/
`ImportImageJMacroAlgorithm`→`MiscDataFunction` for `StringData`-derived types): `ImportStringAlgorithm`
(`StringData`), `ImportXMLAlgorithm` (`XMLData`), `ImportJsonAlgorithm` (`JsonData`),
`StringDefinitionDataSource` (`StringData`).

**`AnnotationsEditFunction` (3)** — rule 5: `AnnotateWithJsonDataAlgorithm` (For JSON),
`AnnotateWithTextDataAlgorithm` (For text), `AnnotateWithXPathDataAlgorithm` (For XML) — each **adds
annotations to** JSON/text/XML data (annotation adders; the "For …" menuPath signals the target type,
not a conversion).

**`TableDataFunction` (3)** — the three table-**producers** that derive a `ResultsTableData` from a
*non-table* input: `ExtractJsonDataAsTableAlgorithm` (JSON→table), `ExtractXPathDataAsTableAlgorithm`
(XML→table), `TextDataToTableAlgorithm` (text→table). Rule 9 / §19.1 `TableDataFunction` precedent for
"produce a table from non-table source"; **flag (e)** — `TableTransformFunction` is the alternative if
the audit reads "as table" as a *conversion* of table data.

**`TableTransformFunction` (1)** — `TableToCSVTextAlgorithm` (Table/Convert): input **`ResultsTableData`**
→ `StringData`. A genuine table **transform** (table in, non-table out) under rule 4 — the only node
here with a table *input*, so the tables/ transform leaf (consistent with the `TableDataFunction`
producers above being table-*out* from non-table).

**`MiscellaneousFunction` (4)** — rule 9 last-resort (no text/XML-processing leaf exists):
`ProcessTextDataAlgorithm` (Text: expression-map text→text), `PrettifyXMLAlgorithm` (XML: reindent
XML→XML), `ExtractTextFromXMLAlgorithm` (XML: XPath→`StringData`), `ExtractTextFromJsonAlgorithm`
(JSON: json-path→`StringData`). These operate on the text/XML *string* itself (not annotations, not
tables), so the `Miscellaneous` branch is the honest fit.

#### (c) jipipe-plugin-utils (28)

Rule 6 (DataSource), rule 5 (Annotations), rule 9 (Miscellaneous best-fit — expected to be common here).
The brief explicitly steered: table-ish ops → `tables/`, graph/compartment ops → `graph/`, read each.

**`TableFilterModifyFunction` (4)** — the table **row filter/sort** ops (all carry generic
`JIPipeData` slots but their docs/params target data-table rows — §19.1's "tables as a table type"
precedent): `DataSlicer` ("Slices the incoming data table into a smaller table", Filter),
`SelectDataTableRowsAlgorithm` ("select a specific set of input data table rows", Filter),
`SortRowsByAnnotationsAlgorithm` + `SortRowsByExpressionAlgorithm` (Sort). **Flag (e)** — the generic
`JIPipeData` slot makes `MiscellaneousFunction` a defensible fallback if the audit weights slot-type
over doc-intent.

**`TableJoinSplitFunction` (2)** — `MergeDataToTableAlgorithm` (Data tables: merges iteration-step data
into a `JIPipeDataTable` = table *merge*, mirroring §19.1 `MergeParameters`→`TableJoinSplit`) and
`ExtractTableAlgorithm` ("Unpack data table": splits a `JIPipeDataTable` input into its rows/data on
output = table *split*). **Flag (e)** on `ExtractTable` — `TableTransformFunction` is the alternative if
"unpack" is read as a transform.

**`TableDataFunction` (2)** — the two `ParametersData` producers: `JIPipeProjectParameterDefinition`
(DataSource; extends `DefineParametersTableAlgorithm`, emits `ParametersData` — §19.1's
`DefineParametersTableAlgorithm`→`TableDataFunction` precedent) and
`PathsToJIPipeProjectParametersAlgorithm` (Meta run; non-table `PathData` in → `ParametersData` table
out, a table *producer* like `DefineParametersFromGlobalParameters`). **Flag (e)** on
`PathsToJIPipeProjectParameters` — `TableTransformFunction` is the alternative (it does consume an
input to build the table).

**`GraphDebugFunction` (3)** — the graph/pipeline **inspection & debugging** nodes:
`GetJIPipeSlotFolderAlgorithm` (Meta run: extracts a slot's output folder from a JIPipe output),
`ImportJIPipeSlotFolderAlgorithm` (Meta run: extends the above, also imports the folder's data),
`CrasherAlgorithm` (Misc, no menuPath: "Cancels the current run if data is processed. Use this node to
inform users about unwanted conditions" — a deliberate pipeline *debug/tripwire*). **Flag (e)** on the
two slot-folder nodes — `GraphWrappersFunction` is the alternative if "meta-run slot inspection" is read
as graph-wrapping rather than debugging.

**`GraphWrappersFunction` (1)** — `RunJIPipeProjectAlgorithm` (Meta run): runs an **existing JIPipe
project** (a sub-pipeline) with a given parameter set, emits its `JIPipeOutputData` — a
pipeline-wrapper/orchestration role (§19.1 `JIPipeProjectCompartment`/wrappers precedent), distinct from
the debug-flavored slot-folder/crash nodes. **Flag (e)** — `GraphDebugFunction` (the "Meta run" segment
group) is the alternative if the audit groups all Meta-run nodes together.

**`MiscellaneousFunction` (13)** — rule 9 last-resort (no specific leaf fits). Generic
`JIPipeData` pass-through / meta / distribution / process ops:
`ConverterAlgorithm` (convert any data type→type), `RunExpressionAlgorithm` (run an expression per row,
pass-through), `SleepAlgorithm` (wait), `DuplicateDataAlgorithm` (duplicate for debugging),
`UsePlaceholderForMissingDataAlgorithm` (substitute a placeholder for missing data),
`IterationStepSlicer` (merge/slice merging iteration steps; **flag (e)** — graph/iteration-flavored),
`DistributeDataRandomlyByCountAlgorithm` + `DistributeDataRandomlyByPercentageAlgorithm` (Split: split
data across output slots by count/percentage; **flag (e)**), `RunProcessIteratingAlgorithm` +
`RunProcessMergingAlgorithm` (Process: execute an OS process; **flag (e)** — no "external process"
leaf exists), `ToDataStringAlgorithm` (Convert: data → its `StringData` representation), and the two
iteration-step **checkers** `SingleIterationStepCheckerAlgorithm` + `MultiIterationStepCheckerAlgorithm`
(primary = Miscellaneous, with an `AnnotationsEditFunction` secondary each from their annotation-filter
aliases — see (d); **flag (e)**).

**`AnnotationsEditFunction` (3)** — rule 5: `AnnotateWithDataTableProperties` (For data tables: add
row/column property annotations), `PullDataTableAnnotations` (annotate tables with annotations pulled
from inside the table), `PushDataTableAnnotations` (copy the table's annotations into the table) — all
annotation add/edit ops on data tables.

#### (d) Aliases (7 references) — 5 dropped, 2 → secondary `function[]` (primary-first)

Per the A/B/C protocol: map the alias's old category+menuPath to a leaf by the same rules; alias-leaf
== primary-leaf → drop, else add as a **secondary** `function[]` entry (primary first).

| node (primary leaf) | alias (old category / menuPath) | alias leaf | action |
|---|---|---|---|
| `ProjectUserPathDataSource` (MiscData) | DataSource / "Project user directory" | `MiscDataFunction` (rule 6, `FolderData` out) | **drop** (== primary) |
| `ProjectUserPathDataSource` (MiscData) | DataSource / "Project user file" | `MiscDataFunction` (rule 6, file `PathData` out) | **drop** (== primary) |
| `SimplePathAnnotationGenerator` (AnnotationsEdit) | Annotations / "For paths" | `AnnotationsEditFunction` (rule 5, adder) | **drop** (== primary) |
| `MultiIterationStepCheckerAlgorithm` (Miscellaneous) | Miscellaneous / "Filter" ("Limit to iteration steps") | `MiscellaneousFunction` (== primary) | **drop** (== primary) |
| `MultiIterationStepCheckerAlgorithm` (Miscellaneous) | Annotations / "Filter" ("Filter multiple data by annotation") | `AnnotationsEditFunction` (rule 5: filters/annotates by annotation) | **add secondary** → `function = { MiscellaneousFunction.class, AnnotationsEditFunction.class }` |
| `SingleIterationStepCheckerAlgorithm` (Miscellaneous) | Miscellaneous / "Filter" ("Limit to iteration steps") | `MiscellaneousFunction` (== primary) | **drop** (== primary) |
| `SingleIterationStepCheckerAlgorithm` (Miscellaneous) | Annotations / "Filter" ("Filter multiple data by annotation") | `AnnotationsEditFunction` (rule 5) | **add secondary** → `function = { MiscellaneousFunction.class, AnnotationsEditFunction.class }` |

**5 dropped, 2 secondary `function[]` entries** (the two iteration-step checkers — the only two
multi-`function` nodes in this batch, primary-first per the §19.2/19.5 precedent).

#### (e) Flagged for Phase 3 audit

- **`ConvertToExportedPath` → `ExportFileFormatsFunction`.** In the Export category but writes **no**
  file — it generates the export `PathData`. Kept in the export family (mirrors §19.3
  `SetBioFormatsExporterSettings`); `MiscDataFunction` is the alternative if "not a real exporter" is
  preferred.
- **`AnnotationTableToPaths` → `TableTransformFunction`.** Has a table *input* (`AnnotationTableData`)
  so rule 4 applies, but its output is `PathData`; `MiscDataFunction` (rule 8, judged by output) is the
  alternative.
- **`ConvertDataToOutputPath` → `MiscDataFunction`.** A run-output *path producer*; export-flavored by
  intent (computes where data is stored) but emits `FolderData` → Misc.
- **`DataSlicer` / `SelectDataTableRows` / `SortRowsBy{Annotations,Expression}` → `TableFilterModifyFunction`.**
  Generic `JIPipeData` slots; assigned to the tables/ filter leaf by their data-table doc/params. If
  slot-type outranks doc-intent, all four fall to `MiscellaneousFunction`.
- **`ExtractTableAlgorithm` (Unpack) → `TableJoinSplitFunction`** (table *split* into rows) vs
  `TableTransformFunction`; **`PathsToJIPipeProjectParameters` → `TableDataFunction`** (non-table→table
  producer) vs `TableTransformFunction` (consumes a path input).
- **Strings table-producers `ExtractJsonDataAsTable` / `ExtractXPathDataAsTable` / `TextDataToTable` →
  `TableDataFunction`** (derive a table from a non-table source) vs `TableTransformFunction` (a
  "convert-to-table" reading). `TableToCSVText` (table *in*) is the contrasting `TableTransformFunction`.
- **`RunJIPipeProject` → `GraphWrappersFunction`** (runs a sub-pipeline) vs `GraphDebugFunction`
  (the "Meta run" segment group). **`GetJIPipeSlotFolder` / `ImportJIPipeSlotFolder` →
  `GraphDebugFunction`** (slot-folder inspection/debug) vs `GraphWrappersFunction`.
- **`IterationStepSlicer` → `MiscellaneousFunction`** (merging-iteration-step slicer; graph-flavored,
  no dedicated leaf). **`DistributeDataRandomly{ByCount,ByPercentage}` → `MiscellaneousFunction`**
  (data distribution across output slots; the `Split` menuPath is the old panel group, not a
  tables/join op). **`RunProcess{Iterating,Merging}` → `MiscellaneousFunction`** (OS process execution;
  no "external process" leaf). **`ToDataString` → `MiscellaneousFunction`** (data→string; no text leaf).
- **`Single`/`MultiIterationStepChecker` → primary `MiscellaneousFunction` + secondary
  `AnnotationsEditFunction`.** Primary is the generic iteration-step limiter (Misc); the secondary
  carries their annotation-filter aliases. If the checkers are read as annotation filters primarily,
  the primary could flip to `AnnotationsEditFunction`.

#### (f) Test infrastructure

All three modules had **no `src/test` and no JUnit deps** (first test in each). For EACH:
- **`pom.xml`**: added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) — mirrors `plugins/jipipe-plugin-ij/pom.xml`/`-ij-3d`/`-ij-filaments` verbatim (inserted
  before `</dependencies>`; in `strings` after the `json-path` dependency).
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/` (**byte-identical**,
  verified with `cmp`).
- Created `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/plugins/<pkg>/
  ImageJLegacyPatcherLauncherSessionListener.java` (module-scoped: filesystem→`…plugins.filesystem`,
  strings→`…plugins.strings`, utils→`…plugins.utils`; `LegacyInjector.preinit()` in
  `launcherSessionOpened`, HKI header, Javadoc mirroring the §19.3/19.4/19.5 listeners).
- Created `plugins/jipipe-plugin-<m>/src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = that module's listener FQCN).

**Why the listener is needed:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent
failure (AGENTS.md patcher contract). `net.imagej.patcher.LegacyInjector` is on the test classpath
transitively via jipipe-core → `net.imagej:imagej`.

#### (g) Gate result (R1/R2/R3)

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-filesystem,plugins/jipipe-plugin-strings,
  plugins/jipipe-plugin-utils -am` → **exit 0** (all three in one reactor).

- **Stale `~/.m2` core jar (environment fix, documented):** running the filesystem gate
  (`-pl plugins/jipipe-plugin-filesystem`, *no* `-am`) resolves `jipipe-core` from the **installed**
  `~/.m2` jar, which was dated **2026-10-09 17:32** — ~3.5 h *before* the §19.1 core conversion commit
  `910aa020d6` (2026-10-09 21:07). The stale jar's `FileDataSource` et al. still carried
  `nodeTypeCategory=DataSourceNodeTypeCategory` (no `function`), so the **first** filesystem gate run
  reported 6 unassigned: `import-file`, `import-file-list`, `import-folder`, `import-folder-list`,
  `import-path`, `import-path-list`. These six are core's `plugins/filesystem/datasources` classes —
  their package `org.hkijena.jipipe.plugins.filesystem.datasources` **starts with** the filesystem gate
  prefix and they are **registered by the filesystem module's** `FilesystemPlugin` — i.e. exactly the
  six §19.1 "re-covered by Task 19.6 for utils/filesystem" classes (the §19.1 core gate could not see
  them because they are not on the *core* test classpath). This was the **expected** stale-jar condition,
  not a conversion gap: all six already carry `function = MiscDataFunction.class, framework =
  CoreFramework.class` in source (verified). Fix: `mvn -q -B install -pl jipipe-core -DskipTests`
  (refreshed the core jar; `javap` then showed `FileDataSource` with `function=[MiscDataFunction]`).
  As a secondary cleanup, `mvn -q -B install -pl plugins/jipipe-plugin-filesystem -DskipTests` was also
  run so the utils gate's classpath (utils depends on filesystem) no longer saw a stale filesystem jar
  (its initial global "39 nodes not yet assigned" line, = filesystem's count, disappeared). **No source
  files were changed by these installs** — they only refreshed the local `~/.m2` reactor artifacts so
  the specified no-`-am` gate commands reflect the current (converted) source.

- **Gate (enabled), all three → BUILD SUCCESS, `Tests run: 1, Failures: 0`, "Node taxonomy validation
  passed", zero unassigned in the module prefix:**
  - filesystem: `-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.filesystem`
  - strings: `-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.strings`
  - utils: `-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.utils`
  (The utils module's classpath includes the now-converted filesystem + core, so no "N nodes not yet
  assigned" global line remained; had any unconverted on-classpath module existed, that global count
  would be *outside* the prefix-filtered failure set and not in `errors()`, per the §19.3/19.4/19.5
  shape.)

- **Gate (unset), all three → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the
  gate **skips cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works).

**Transition note understood:** the 82 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategory()` (`isVisibleInPipeline` false), so they are absent from the *old* add-node panel's
category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking, unmerged
branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

**Byte-identity check (reviewer contract):** all 82 node files are byte-identical to base after
stripping `import …;` lines and the `@ConfigureJIPipeNode(…)` / `@AddJIPipeNodeAlias(…)` lines
(verified by a line-filter diff across `git show HEAD:<f>` vs head) — the change is annotations-only.

### 19.7 — jipipe-plugin-tables + jipipe-plugin-forms (58 nodes)

**Status:** converted — all 58 `@ConfigureJIPipeNode` nodes across the two modules rewritten to
`function`-only; two module gates green (see (g)). Single-shot combined conversion
(mapping + apply + 2 gates in one commit), mirroring §19.6. This is the **same class of task as 19.6**:
these nodes get **only `function`** — **no `framework`, no `spatialDimensions`**.

**Scope / gate prefixes (verified by grep, matches controller R1):**
- `plugins/jipipe-plugin-tables/src/main` — base pkg `org.hkijena.jipipe.plugins.tables` (GATE PREFIX) — **48** annotated files. `TablesPlugin` registers **48** `registerNodeType` (1:1 with annotations).
- `plugins/jipipe-plugin-forms/src/main` — base pkg `org.hkijena.jipipe.plugins.forms` (GATE PREFIX) — **10** annotated files. `FormsPlugin` registers **10** `registerNodeType` (1:1 with annotations).

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):**

| module | Table | DataSource | Annotations | Miscellaneous | total |
|---|---|---|---|---|---|
| tables | 42 | 3 | 3 | — | **48** |
| forms | — | 7 | — | 3 | **10** |

First-menuPath segments (tables): none (22) · Transform (6) · Merge (5) · Filter (4) · Split (3) · Clustering (3) · Append (2) · "For tables" (2, the 2 Annotations ops — see below, the 3rd Annotations node carries "For all data") · "For all data" (1).
**Note on the tables Annotations split:** the controller's "Annotations 3" = `AnnotateByTablePropertiesAlgorithm` ("For tables"), `AnnotateDataWithTableValues` ("For all data"), `ColumnsToAnnotationsAlgorithm` ("For tables"). So "For tables" (2) + "For all data" (1) covers the 3 Annotations-category nodes; the 42 Table + 3 DataSource = 45, + 3 Annotations = 48. ✓

**Aliases (verified by grep of `@AddJIPipeNodeAlias`):** tables **5 refs in 4 files**
(`ApplyExpressionToTableByColumnAlgorithm` ×1, `RemoveColumnAlgorithm` ×1, `SortTableColumnsAlgorithm` ×1,
`GenerateColumnAlgorithm` ×1, `MeltTableAlgorithm` ×1); forms **0**. Total **5** alias references →
decisions in (d).

---

**⚠️ tables/ vs data/ distinction (the single most-likely mistake — apply as ruled).**
The `tables/` branch holds exactly **FIVE** *operation* leaves: `TableTransformFunction`,
`TableFilterModifyFunction`, `TableJoinSplitFunction`, `TableAggregateFunction`,
`TableExportFileFunction`. The table-**DATA** leaf lives in the **`data/`** branch —
`data/TableDataFunction` ("Nodes that produce or load table data"). **There is NO
`TableDataFunction` in `tables/`.** Do not confuse the table-DATA leaf (data/) with the
table-OP leaves (tables/). The 3 tables `DataSourceNodeTypeCategory` nodes are table **producers**
(output `ResultsTableData`) → rule 6 by OUTPUT → **`data/TableDataFunction`** (NOT a tables/ op leaf).
Conversely the 42 `TableNodeTypeCategory` ops are table **operations** → the 5 `tables/` leaves.

**Framework — OMITTED for all 58 (rule 11, deliberate, not an omission).** Neither module is on
rule 11's named list (cellpose/trackmate/clij/opencv/plots/weka/ilastik/omnipose/ij2-discovered/
ij/ij-3d/ij-algorithms), and they are **not** "core plugins" (standalone plugin modules, not
bundled in jipipe-core). Rule 11's final clause — "else leave `NoFramework`" — therefore applies.
The `@ConfigureJIPipeNode.framework()` default is **already** `NoFramework.class`
(`jipipe-core/…/api/ConfigureJIPipeNode.java:97`), so the attribute is **omitted entirely** — no node
writes `framework = NoFramework.class`, and **no** `NoFramework` import is added.
`grep -rl "framework = " src/main` across both modules → **0**.

**Spatial — ABSENT for all 58 (rule 12, deliberate, not an omission).** None of the 58 nodes has an
image input slot (controller-verified: zero `ImagePlus*`/`Image*`/`OME*` input values across both
modules; the slots are `ResultsTableData`/`TableColumnData`/`AnnotationTableData`/`JIPipeData`/
`FormData`). Rule 12's "non-image ⇒ absent" therefore applies to every node: `spatialDimensions` is
**omitted entirely** on all 58, and there is **no** `SPATIAL_*` static import anywhere in both modules.
`grep -rl "spatialDimensions = " src/main` across both modules → **0**.

So every converted node's annotation is exactly `@ConfigureJIPipeNode(function = <Leaf>.class)`, or
`@ConfigureJIPipeNode(function = { Primary.class, SecondaryLeaf.class })` for the one alias that added a
distinct secondary leaf (`ApplyExpressionToTableByColumnAlgorithm` — see (d)). The `function` attribute
is never dropped.

**Leaf distribution (primary, all 58):** `TableTransformFunction` 13 · `TableFilterModifyFunction` 13 ·
`TableJoinSplitFunction` 8 · `TableAggregateFunction` 8 · `MiscDataFunction` 7 ·
`TableDataFunction` 3 · `AnnotationsEditFunction` 3 · `MiscellaneousFunction` 3 ·
`TableExportFileFunction` 0 · `AnnotationsConvertFunction` 0 (total 58).
(No node here maps to the `tables/` export leaf or the `annotations/` convert leaf.)

---

#### (a) jipipe-plugin-tables — 42 `TableNodeTypeCategory` ops → the 5 `tables/` leaves

Assigned by each node's **actual operation** (read `@SetJIPipeDocumentation` + slots). MenuPath priors
verified per node (not blanket-applied): Transform→`TableTransformFunction`, Filter→`TableFilterModifyFunction`,
Merge/Append/Split→`TableJoinSplitFunction`, Clustering→`TableAggregateFunction`; the 22 no-menuPath nodes
judged individually. **Tally: Transform 13 · FilterModify 13 · JoinSplit 8 · Aggregate 8 = 42.**

**`TableTransformFunction` (13)** — transform/reshape/convert/compute-column ops (table in → reshaped table out):

| node (menuPath) | rationale for non-obvious |
|---|---|
| `AddAnnotationColumnsAlgorithm` (Append) | adds column annotations to the table **as new columns** (a table reshape/column-add, not an annotation edit — the table data is modified) |
| `ApplyExpressionToColumnsAlgorithm` (—) | compute new column values from expressions |
| `ApplyExpressionToTableByColumnAlgorithm` (—) | generate table columns from expressions (**alias** → `TableDataFunction` secondary, see (d)) |
| `ApplyExpressionPerRowAlgorithm` (—, @Deprecated) | per-row expression → new column values |
| `ApplyExpressionPerRowAlgorithm2` (—) | per-row expression → new column values (v2) |
| `ConvertAnnotationTableToAnnotatedTables` (—) | table in (`AnnotationTableData`) → table out, moving annotation columns into data annotations (a table **transform**, not an annotation-type conversion — the data type stays table) |
| `ConvertColumnsAlgorithm` (—) | apply a converting function to column values |
| `ColumnToNumericAlgorithm` (—) | convert columns to numeric |
| `ColumnToStringAlgorithm` (—) | convert columns to string |
| `GenerateColumnAlgorithm` (—) | **add/compute** a new column (**alias** dropped, see (d)) |
| `AddMissingRowsInSeriesAlgorithm` (Append) | **reshape**: synthesizes missing series rows (a table transformation, not a join of multiple tables) — kept Transform despite the "Append" segment (flag (e)) |
| `MeltTableAlgorithm` (Transform) | melt/unpivot columns↔rows (**alias** "Unpivot table" dropped, see (d)) |
| `UnMeltTableAlgorithm` (Transform) | pivot: value column → separate columns |

**`TableFilterModifyFunction` (13)** — filter/modify/select/sort (modify) ops:

| node (menuPath) | rationale for non-obvious |
|---|---|
| `FilterTableRowsAlgorithm` (Filter) | filter rows by expression |
| `FilterTablesAlgorithm` (Filter) | filter whole tables by their properties |
| `SliceTableRowsAlgorithm` (Filter) | select/slice specific rows |
| `SortTableRowsAlgorithm` (—) | sort rows (a modify op) |
| `SortTableColumnsAlgorithm` (—) | sort/reorder columns (a modify op; **alias** "Reorder table columns" dropped, see (d)) |
| `RemoveColumnAlgorithm` (Filter) | remove columns by name (a modify op; **alias** "Filter table columns" dropped, see (d)) |
| `RenameTableColumnsAlgorithm` (—, @Deprecated) | rename columns (a modify op) |
| `RenameTableColumns2Algorithm` (—) | rename columns |
| `RenameSingleColumnsAlgorithm` (—) | rename single `TableColumnData` |
| `RenameTableColumnsToAnnotationAlgorithm` (—) | rename columns to annotation values (a table column **modify** op, not an annotation edit) |
| `ModifyTableColumnNamesAlgorithm` (—) | rename/modify column names via expression |
| `ModifyTablesScript` (—) | general per-table modification via Python script (table in/out) (flag (e)) |
| `ModifyAndMergeTablesScript` (—) | modify & merge tables via Python script (table in/out) (flag (e)) |

**`TableJoinSplitFunction` (8)** — merge/append/join/split ops:

| node (menuPath) | rationale |
|---|---|
| `MergeColumnsAlgorithm` (Merge) | merge multiple `TableColumnData` into one table |
| `MergeTableColumnsSimpleAlgorithm` (Merge) | merge multiple tables' columns into one |
| `MergeTableColumnsSupplementAlgorithm` (Merge) | merge source columns into target by reference columns |
| `MergeTableColumnsSupplementMergingAlgorithm` (Merge) | merge source columns into target (merging variant) |
| `MergeTableRowsAlgorithm` (Merge) | merge multiple tables' rows into one |
| `SplitTableByColumnsAlgorithm` (Split) | split a table into sub-tables by selected columns |
| `SplitTableIntoColumnsAlgorithm` (Split) | split a table into individual `TableColumnData` |
| `SplitTableIntoRowsAlgorithm` (Split) | split a table into individual row-tables |

**`TableAggregateFunction` (8)** — grouping/aggregation/clustering ops:

| node (menuPath) | rationale |
|---|---|
| `IntegrateColumnsAlgorithm` (—) | "Summarize table" — avg/stddev/median per column (aggregation) |
| `ApplyExpressionPerLabelAlgorithm` (Transform) | "summarize all values assigned to a key into a single value" — a **grouping/aggregation** op (group-by-key) despite the "Transform" segment → Aggregate (flag (e), the debatable Transform-vs-Aggregate one) |
| `TableColumnToCountAlgorithm` (Transform) | count occurrences per value → histogram table (aggregation) |
| `TableToHistogram2Algorithm` (Transform) | histogram binning (aggregation) |
| `TableToHistogramAlgorithm` (Transform, @Deprecated) | histogram binning (aggregation) |
| `KMeansClusteringAlgorithm` (Clustering) | clustering (grouping op) |
| `FuzzyKMeansClusteringAlgorithm` (Clustering) | fuzzy clustering (grouping op) |
| `DBSCANClusteringAlgorithm` (Clustering) | density clustering (grouping op) |

#### (b) jipipe-plugin-tables — 3 `DataSourceNodeTypeCategory` → `data/TableDataFunction` (rule 6 by OUTPUT)

All three output **`ResultsTableData`** → the table-DATA leaf in `data/` (**NOT** a `tables/` op leaf —
see the callout). Mirrors §19.1's `DefineParametersTableAlgorithm`→`TableDataFunction` and §19.6's
`ListZARR*`→`TableDataFunction` producers.

| node | rationale |
|---|---|
| `DefineTablesAlgorithm` | defines one or multiple tables (output `ResultsTableData`) |
| `GenerateTableFromExpressionAlgorithm` | generates a table's rows from expressions (output `ResultsTableData`) |
| `TablesFromScript` | Python script generates a table (output `ResultsTableData`) |

#### (c) jipipe-plugin-tables — 3 `AnnotationsNodeTypeCategory` → `annotations/` leaves (rule 5 by OP)

| node (menuPath) | leaf | rationale |
|---|---|---|
| `AnnotateByTablePropertiesAlgorithm` (For tables) | `AnnotationsEditFunction` | **adds** row/column-count/column-name annotations to the table (an annotation adder) |
| `AnnotateDataWithTableValues` (For all data) | `AnnotationsEditFunction` | **adds** annotations to the incoming (generic `JIPipeData`) data from table values (an annotation adder, not a type conversion) |
| `ColumnsToAnnotationsAlgorithm` (For tables) | `AnnotationsEditFunction` | **copies** column values into annotations (adds/merges annotation entries; the data type stays a table) — *edit*, not a type conversion (flag (e)) |

All three are annotation **editing/adding** ops (the target data type is unchanged) → `AnnotationsEditFunction`.
None maps an annotation *type* to a different data type, so **no** `AnnotationsConvertFunction` is used here
(that leaf is reserved for genuine annotation→other-data-type conversions, cf. §19.6 `AnnotationToPath`).

#### (d) Aliases (5 references) — 4 dropped, 1 → secondary `function[]` (primary-first)

Per the A/B/C protocol: map the alias's old category+menuPath to a leaf by the same rules; alias-leaf
== primary-leaf → drop, else add as a **secondary** `function[]` entry (primary first).

| node (primary leaf) | alias (old category / menuPath) | alias leaf | action |
|---|---|---|---|
| `ApplyExpressionToTableByColumnAlgorithm` (TableTransform) | `DataSourceNodeTypeCategory` / (no menuPath), "Generate table columns from expression" | `TableDataFunction` (rule 6 by OUTPUT: with no input it **generates** a `ResultsTableData`) | **add secondary** → `function = { TableTransformFunction.class, TableDataFunction.class }` |
| `RemoveColumnAlgorithm` (TableFilterModify) | (default `InternalNodeTypeCategory`) / (no menuPath), "Filter table columns" | `TableFilterModifyFunction` (== primary; a column filter/modify op) | **drop** (== primary) |
| `SortTableColumnsAlgorithm` (TableFilterModify) | (default) / (no menuPath), "Reorder table columns" | `TableFilterModifyFunction` (== primary; a column modify/reorder op) | **drop** (== primary) |
| `GenerateColumnAlgorithm` (TableTransform) | `TableNodeTypeCategory` / "Append", "Add missing columns" | `TableTransformFunction` (== primary; adds/computes a column) | **drop** (== primary) |
| `MeltTableAlgorithm` (TableTransform) | `TableNodeTypeCategory` / "Transform", "Unpivot table" | `TableTransformFunction` (== primary; unpivot = melt) | **drop** (== primary) |

**4 dropped, 1 secondary `function[]` entry** (`ApplyExpressionToTableByColumnAlgorithm` — the only
multi-`function` node in this batch, primary-first per the §19.2/19.5/19.6 precedent). The secondary is
genuinely distinct: its alias is a **data-source** registration (output-driven `TableDataFunction`),
whereas the primary is a **table transform** (consumes an optional input).

#### (e) Flagged for Phase 3 audit

- **`ApplyExpressionPerLabelAlgorithm` → `TableAggregateFunction`** (its menuPath says "Transform"). Its
  doc is "summarize all values assigned to a key into a single value" — a **group-by-key aggregation**.
  Kept Aggregate by op; `TableTransformFunction` is the alternative if the audit weights the menuPath segment.
- **`AddMissingRowsInSeriesAlgorithm` → `TableTransformFunction`** (menuPath "Append"). It synthesizes missing
  series rows (a table **reshape**, not a join/merge of multiple inputs). Kept Transform; `TableJoinSplitFunction`
  is the alternative if "Append" is read as a join-family op.
- **`ConvertAnnotationTableToAnnotatedTables` → `TableTransformFunction`.** Input `AnnotationTableData`
  (a `ResultsTableData` subclass) → `ResultsTableData`; it moves annotation columns into data annotations.
  Kept Transform (table in, table out, rule 4); `AnnotationsEditFunction` is the alternative if the
  annotation-producing side is weighted.
- **`ColumnsToAnnotationsAlgorithm` → `AnnotationsEditFunction`.** Copies column values *into* annotations
  (adds/merges annotation entries; data type stays a table). Kept Edit; `AnnotationsConvertFunction` is the
  alternative if "columns↔annotations" is read as a type conversion.
- **`TableColumnToCountAlgorithm` / `TableToHistogram2Algorithm` / `TableToHistogramAlgorithm` →
  `TableAggregateFunction`** (menuPath "Transform"). Histogram/count ops are aggregations (group values into
  bins and count/accumulate). Kept Aggregate by op; `TableTransformFunction` is the alternative if the
  menuPath segment is weighted.
- **`IntegrateColumnsAlgorithm` → `TableAggregateFunction`** (no menuPath). "Summarize table columns by applying
  predefined operations like average, standard deviation, or median" — a clear aggregation.
- **`ModifyTablesScript` / `ModifyAndMergeTablesScript` → `TableFilterModifyFunction`.** General Python-script
  table modification (table in, table out). Kept FilterModify ("modify" in the name/doc); `TableTransformFunction`
  is the alternative (the "Modify & merge" variant also merges, so `TableJoinSplitFunction` is a weaker alternative).
- **`RenameTableColumnsToAnnotationAlgorithm` → `TableFilterModifyFunction`.** Renames columns to annotation
  values (a column **modify** op on the table); kept FilterModify, not Annotations, because it edits the table's
  column names (a table op), not the annotation data type.

#### (f) jipipe-plugin-forms (10)

Rule 6 (DataSource by output) for the 7 generators; rule 9 (Miscellaneous best-fit) for the 3 processors.

**`MiscDataFunction` (7)** — the form **generators** (all extend `SimpleFormGeneratorAlgorithm`/
`FormGeneratorAlgorithm`; each outputs **`FormData`**). There is **NO `FormData` data leaf in `data/`**
(data/ = ImageData / TableData / RoiData / FilamentData / MiscData), so rule 6's "else `MiscDataFunction`"
applies: `BooleanFormGeneratorAlgorithm`, `StringFormGeneratorAlgorithm`, `IntegerFormGeneratorAlgorithm`,
`DoubleFormGeneratorAlgorithm`, `EnumFormGeneratorAlgorithm`, `PathFormGeneratorAlgorithm`,
`GroupHeaderFormGeneratorAlgorithm`. (The 7 form GENERATORS are data **producers** of `FormData`, not
table/image/roi/filament data → the Misc data leaf.)

**`MiscellaneousFunction` (3)** — the **FormProcessors** (all `MiscellaneousNodeTypeCategory`, menuPath
"Forms", each `extends JIPipeAlgorithm implements JIPipeIterationStepAlgorithm`; slots = `JIPipeData` in/out
+ `FormData` in/out, role Parameters). `SimpleIteratingFormProcessorAlgorithm`, `IteratingFormProcessorAlgorithm`,
`MergingFormProcessorAlgorithm`. Each shows a UI during the run that lets a user **modify annotations** via
form elements, then passes the (annotated) data through. Rule 9 best-fit: a **genuine no-specific-leaf**
interactive-form-driven data-processing role (input `JIPipeData`+`FormData` → output `JIPipeData`+`FormData`).
**Not** a `graph/` wrapper — it does not run/substitute a pipeline or wrap a graph; it is a per-iteration
interactive annotation editor. Kept `MiscellaneousFunction` (the last-resort leaf). **Flag (e)** — if the audit
reads "modify annotations via form" as an annotation-edit op, `AnnotationsEditFunction` is the alternative;
this is a judgment call by nature.

#### (g) Test infrastructure + gate results (R1/R2/R3)

Both modules had **no `src/test` and no JUnit deps** (first test in each). For EACH (mirror §19.6 / the
`jipipe-plugin-ij` pom verbatim):
- **`pom.xml`**: added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) — inserted before `</dependencies>` (verified byte-identical to the ij block by `diff`).
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/` (**byte-identical**,
  verified with `cmp` for both).
- Created `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/plugins/<pkg>/
  ImageJLegacyPatcherLauncherSessionListener.java` (module-scoped: tables→`…plugins.tables`, forms→`…plugins.forms`;
  `LegacyInjector.preinit()` in `launcherSessionOpened`, HKI header, Javadoc mirroring the §19.6 listener) +
  `plugins/jipipe-plugin-<m>/src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener`
  (single line = that module's listener FQCN).

**Why the listener is needed:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A module's
test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher, any
`ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent failure
(AGENTS.md patcher contract). `net.imagej.patcher.LegacyInjector` is on the test classpath transitively via
jipipe-core → `net.imagej:imagej`.

**Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-tables,plugins/jipipe-plugin-forms -am` → **exit 0**
(both in one reactor; host mvn).

**Stale `~/.m2` jar (documented — the *core* gotcha was NOT hit).** The installed `jipipe-core` jar was
**2026-10-10 03:52**, i.e. *after* the §19.1 core conversion commit `910aa020d6` (2026-10-09 21:07) — **fresh**,
so no pre-19.1 core nodes surfaced as unassigned in either prefix (the §19.6 stale-core-jar condition did not
recur). However, the installed `jipipe-plugin-{tables,strings,annotations}` jars are all dated **2026-09-25**
(stale — pre-19.6, when only `tables`/`forms`/`annotations` were still unconverted among on-classpath deps).
Because the gate runs with **no `-am`**, Maven resolves those dependency jars from `~/.m2` (not the reactor), so
the *global* (unfiltered) "N nodes not yet assigned" log line reflects the **stale** dependency counts:
- **tables gate:** global line = **29** = the **`jipipe-plugin-annotations`** module (tables' direct dep,
  unconverted in this Phase; 29 `@ConfigureJIPipeNode` files). None of these 29 are under the
  `org.hkijena.jipipe.plugins.tables` prefix, so the prefix-filtered assertion saw **zero** and passed.
- **forms gate:** global line = **92** = 48 (stale `tables` jar, pre-conversion) + 15 (stale `strings` jar,
  pre-19.6) + 29 (`annotations`). None are under the `org.hkijena.jipipe.plugins.forms` prefix, so the
  prefix-filtered assertion saw **zero** and passed.
No `mvn install` was required (and none was run): the prefix-filtered failure set is what must be zero, and it was.
The global "N nodes not yet assigned" line is an **informational** SciJava log line (a different, looser metric
than the gate's per-node `unassignedNodeIds()` filter) that is *outside* the filtered set and **not** in
`errors()`, per the §19.3/19.4/19.5/19.6 shape.

**Gate (enabled), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, zero unassigned in
the module prefix:**
- tables: `mvn test -pl plugins/jipipe-plugin-tables -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.tables` → **Tests run: 1, Failures: 0, Errors: 0,
  Skipped: 0** (global "29 nodes not yet assigned" line present, outside prefix — documented above).
- forms: `mvn test -pl plugins/jipipe-plugin-forms -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.forms` → **Tests run: 1, Failures: 0, Errors: 0,
  Skipped: 0** (global "92 nodes not yet assigned" line present, outside prefix — documented above).

**Gate (unset), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the gate **skips
cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works).

**Transition note understood:** the 58 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node panel's
category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking, unmerged branch).
No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

**Byte-identity check (reviewer contract):** all 58 node files are byte-identical to base after stripping
`import …;` lines and the `@ConfigureJIPipeNode(…)` / `@AddJIPipeNodeAlias(…)` lines (verified by a line-filter
diff across `git show HEAD:<f>` vs head — 58 files, 0 non-annotation changes) — the change is annotations-only.

### 19.8 — jipipe-plugin-annotations (29) + jipipe-plugin-ij-omero (26) (55 nodes)

**Status:** converted — all 55 `@ConfigureJIPipeNode` nodes across the two modules rewritten to
`function`-only; two module gates green (see (g)). Single-shot combined conversion (mapping + apply +
2 gates in one commit), mirroring §19.6/§19.7. Same class of task as 19.6/19.7 (function-only,
non-ImageJ modules) **plus two judgment-heavy elements**: the 2-leaf `annotations/` Edit-vs-Convert
split (26 annotations nodes + 6 omero annotation nodes) and the OME-image spatial question (1 omero
node).

**Scope / gate prefixes (verified, matches controller R1):**
- `plugins/jipipe-plugin-annotations/src/main` — base pkg **`org.hkijena.jipipe.plugins.annotation`**
  (SINGULAR = GATE PREFIX) — **29** annotated files (28 `algorithms/` + 1 `datasources/`).
  `ImplicitResultsTableDataConverter` is a `JIPipeDataConverter` (not a node — no `@ConfigureJIPipeNode`),
  `AnnotationFilterExpression` is an expression class (not a node) → both correctly **excluded**.
- `plugins/jipipe-plugin-ij-omero/src/main` — base pkg **`org.hkijena.jipipe.plugins.omero`** (GATE PREFIX)
  — **26** annotated files (6 `annotate/` + 7 `datasources/` + 2 `download/` + 1 `manage/` + 8 `navigate/`
  + 2 `upload/`).

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):**

| module | Annotations | DataSource | FileSystem | Export | Miscellaneous | total |
|---|---|---|---|---|---|---|
| annotations | 26 | 2 | — | — | 1 | **29** |
| ij-omero | 6 | 9 | 9 | 2 | — | **26** |

(omero's `navigate/` = 8 nodes + `manage/OMEROCreateDataset` = **9** `FileSystemNodeTypeCategory`;
`download/` = 2 nodes + 7 `datasources/` = **9** `DataSourceNodeTypeCategory` — the controller's "9
DataSource" is by category, spanning the two sub-packages.)

**Aliases (verified by grep of `@AddJIPipeNodeAlias`):** annotations **5 refs in 2 files**
(`AnnotateByExpression` ×3, `ConvertToAnnotationTable` ×2); ij-omero **0**. Total **5** alias references
→ decisions in (e).

---

**Framework — OMITTED for all 55 (rule 11, deliberate, not an omission).** Neither module is on rule 11's
named list (cellpose/trackmate/clij/opencv/plots/weka/ilastik/omnipose/ij2-discovered/ij/ij-3d/
ij-algorithms), and they are **not** "core plugins" (standalone plugin modules, not bundled in
jipipe-core). Rule 11's final clause — "else leave `NoFramework`" — applies. The
`@ConfigureJIPipeNode.framework()` default is **already** `NoFramework.class`
(`jipipe-core/…/api/ConfigureJIPipeNode.java:97`), so the attribute is **omitted entirely** — no node
writes `framework = NoFramework.class`, and **no** `NoFramework` import is added. `grep -rl "framework = "
src/main` across both → **0**. **Note (ij-omero specifically):** the `omero` module is an **OMERO
integration** (`ome.*`/`loci.*`/Bio-Formats based), **not** an ImageJ module — **0** files import
`net.imagej.*` (verified); it has exactly **1** incidental `ij.ImagePlus` import (the OMERO image importer
wraps an `ij.ImagePlus`). `ij.ImagePlus` is an IJ1 *type*, not the IJ1 framework, and the node does not
call IJ1 processing APIs, so rule 11's "else → NoFramework" still holds (OMERO is the framework, and there
is no OMERO framework leaf — it stays `NoFramework`).

**Spatial:**
- **annotations (29) — ABSENT on all 29 (rule 12, deliberate).** 0 files import `net.imagej.*`/`ij.*`; the
  per-node slot survey shows **zero** image input slots (every input is `JIPipeData` or a non-image
  reference/table type; the only non-`JIPipeData` input is `AnnotateWithAnnotationTable`'s
  `AnnotationTableData`). Rule 12's "non-image ⇒ absent" applies to every node → `spatialDimensions`
  omitted entirely; no `SPATIAL_*` import. `grep -rl "spatialDimensions = " src/main` → **0**.
- **ij-omero (26) — ABSENT on 25, BOTH on 1 (rule 12).** The 26 nodes' inputs are
  `OMERO*ReferenceData` (metadata **references** to OMERO image/dataset/project/plate/well/group/screen/
  annotation objects — **not images**), `ResultsTableData`, and `OMEImageData` (1). `OMERO*ReferenceData`
  confers **no** spatial (absent-precedent, same as §19.5's filament-graph references and §19.6's
  path/file references). The **ONE** node with an `OMEImageData` *input* —
  `UploadOMEROImageToDatasetAlgorithm` (Export/Images, "Uploads an image into an OMERO dataset") — takes
  an **unconstrained** `OMEImageData` (no `@ImageTypeInfo(numDimensions=…)` on the class) ⇒ **both**
  (`SPATIAL_2D, SPATIAL_3D`) per rule 12. **Distribution: absent 25 · both 1.** Note the contrast:
  `DownloadOMEROImageAlgorithm` *outputs* `OMEImageData` but its only *input* is
  `OMEROImageReferenceData` (a reference) ⇒ **absent** (rule 12 reads the image *input* slot, not the
  output). So the omero spatial distribution is **absent = 25 · both = 1 · 3D = 0 · 2D = 0** (total 26).

So every converted node's annotation is exactly `@ConfigureJIPipeNode(function = <Leaf>.class)`, or
`@ConfigureJIPipeNode(function = { Primary.class, SecondaryLeaf.class })` for the one alias-collapse
(`ConvertToAnnotationTable`), or `@ConfigureJIPipeNode(function = <Leaf>.class, spatialDimensions =
{ SPATIAL_2D, SPATIAL_3D })` for the one OME-image node. The `function` attribute is never dropped.

**Leaf distribution (primary, all 55):** `AnnotationsEditFunction` **30** (24 annotations + 6 omero) ·
`MiscDataFunction` **16** (omero only: 7 DataSource + 8 navigate + 1 manage) · `TableDataFunction` **3**
(2 annotations DataSources + 1 omero DataSource `DownloadOMEROTable`) · `AnnotationsConvertFunction` **2**
(annotations module) · `ExportFileFormatsFunction` **2** (omero) · `MiscellaneousFunction` **1**
(annotations `MergeDataSlots`) · `ImageDataFunction` **1** (omero `DownloadOMEROImage`). Total **55**.
(Precise per-module per-node tables are in (a) and (b).)

---

#### (a) jipipe-plugin-annotations (29)

**JUDGMENT ELEMENT 1 — the 26 `AnnotationsNodeTypeCategory` nodes → the 2-leaf `annotations/` split.**
The `annotations/` branch has exactly **two** leaves: `AnnotationsEditFunction` ("Editing annotation data"
— add/modify/remove/filter/reorder the annotation set on data) and `AnnotationsConvertFunction`
("Converting between annotation types" — a genuine annotation **type**→other-representation conversion).
Each of the 26 is judged by its **actual operation** (read `@SetJIPipeDocumentation` + slots), NOT by the
menuPath first segment ("For all data" / "For X" signals the annotation **target**, not the op — per the
brief). Result: **24 Edit / 2 Convert**.

**`AnnotationsEditFunction` (24)** — the add/set/modify/remove/rename/filter/merge/split/reorder ops
(the annotation set on the data is edited; the data type is unchanged):

| node (menuPath) | operation (rationale) |
|---|---|
| `AnnotateByExpression` (Modify) | **sets/edits** annotation values (expressions); 3 same-leaf aliases dropped (see (e)) |
| `AnnotateByProjectPaths` (For all data) | **adds** annotations (project paths) |
| `AnnotateWithAnnotationTable` (For all data) | **merges** matching annotations from an `AnnotationTableData` into the data's annotation set (the table is the *source*, the data's annotations are edited — not a data-type conversion) |
| `AnnotateWithData` (For all data) | **adds** a data annotation pointing at the other input data |
| `AnnotateWithDataString` (For all data) | **adds** a string annotation (data→string representation) |
| `AnnotateWithSourceSlot` (For all data) | **adds** a string annotation (source-slot name/label) |
| `ExtractAndReplaceAnnotation` (Modify) | **edits** annotation values (extract substrings + replace) |
| `FilterByAnnotationIfElse` (Filter) | **filters** data by annotation value (If/Else split) |
| `FilterByAnnotation` (Filter) | **filters** data by annotation value |
| `GenerateRandomUniqueAnnotation` (For all data) | **adds** a random-unique annotation |
| `GenerateUniqueAnnotation` (For all data) | **adds** a row-index-unique annotation |
| `MergeAnnotations` (—) | **merges** the annotation sets of all inputs |
| `ModifyAnnotationScript` (Modify) | **modifies** annotations via a Python script |
| `OverwriteAnnotations` (—) | **copies/overwrites** annotations from source→target data |
| `RemoveAnnotationAlgorithm` (Remove) | **removes** annotations by name/value |
| `RemoveArrayAnnotationsAlgorithm` (Filter) | **removes** array-valued annotations/columns |
| `RemoveDataAnnotations` (Remove) | **removes** data annotations by column name |
| `RemoveNAAnnotationColumnsAlgorithm` (Filter) | **removes** annotation columns with missing values |
| `RenameAnnotation` (Modify) | **renames** annotations |
| `RenameDataAnnotation` (Modify) | **renames** data annotations |
| `SetSingleAnnotation` (Modify) | **sets** a single annotation (expression) |
| `SimplifyAnnotationsAlgorithm` (—) | **merges** iteration-step annotations into one + renames/deletes the sources (an annotation-set restructure, not a type conversion) |
| `SplitByAnnotation` (Filter) | **splits/filters** the data by annotation value |
| `SplitByAnnotationScript` (Filter) | **splits/filters** the data by annotation value (Python) |

**`AnnotationsConvertFunction` (2)** — the two nodes whose operation is a genuine annotation **type →
other representation** conversion (the annotation *representation* itself changes; the data type is
unchanged `JIPipeData` in/out):

| node (menuPath) | operation (rationale) |
|---|---|
| `DataAnnotationToStringAnnotation` (For all data) | **converts data annotations to string annotations** (data-annotation → text-annotation **type** conversion; the doc is literally "Convert data annotations to a strings and generates a string annotation") |
| `ExtractDataAnnotation` (—) | **converts a data annotation into the main data** (the doc is "Extracts a data annotation"; its `Annotate with current data` option "the current (main) data is **converted into an annotation** of the specified name") — a data↔annotation **representation** conversion. **Flag (f)** — the strongest Convert candidate is `DataAnnotationToStringAnnotation`; `ExtractDataAnnotation` is the one genuinely debatable (an "extract a column" reading would make it Edit) |

(The 3rd `ConvertToAnnotationTable` is **not** in the 26 Annotations nodes — it is a
`DataSourceNodeTypeCategory` node, see below; its 2 "Annotations to table" aliases collapse to
`AnnotationsConvertFunction` as a **secondary**, see (e).)

**The 2 `DataSourceNodeTypeCategory` nodes (output `AnnotationTableData`) → `TableDataFunction` (rule 6 by
output).** `AnnotationTableData` **is a table** (`extends ResultsTableData`), so rule 6's
"`JIPipeDataTable` → `TableDataFunction`" applies — the table-**data** leaf in `data/` (NOT a `tables/`
op leaf, per the §19.7 callout). `MiscDataFunction` (the "annotation-specific, not a generic data table"
alternative) was rejected: `AnnotationTableData` is literally a `ResultsTableData` subclass, so it is a
table. Mirrors §19.6 `AnnotationTableToPaths`' input-side and §19.3/§19.7 table producers. **Flag (f)** —
both are "annotation-specific" tables; `MiscDataFunction` is the alternative if the audit reads
annotation-tables as outside the generic table-data leaf.

| node | leaf | rationale |
|---|---|---|
| `AnnotationTableFromFile` (no menuPath) | `TableDataFunction` | rule 6 by OUTPUT: emits `AnnotationTableData` (a `ResultsTableData` subclass) → the table-data leaf; `FileData` in ⇒ no spatial |
| `ConvertToAnnotationTable` (no menuPath) | `TableDataFunction` (primary) + `AnnotationsConvertFunction` (secondary) | rule 6 by OUTPUT: emits `AnnotationTableData` → table-data leaf (primary); its 2 "Annotations to table" aliases map to `AnnotationsConvertFunction` (rule 5 / rule 9 best-fit, both the *same* leaf) → **one secondary** (see (e)) |

**The 1 `MiscellaneousNodeTypeCategory` node → `MiscellaneousFunction` (rule 9).**

| node | leaf | rationale |
|---|---|---|
| `MergeDataSlots` (no menuPath) | `MiscellaneousFunction` | a **structural** pass-through ("Structural node that indicates that all inputs are merged into a single output … not needed outside structuring the pipeline") — generic `JIPipeData` in/out, no specific leaf (not a graph/compartment wrapper, not a table op) → rule 9 last-resort |

#### (b) jipipe-plugin-ij-omero (26)

**`MiscDataFunction` (16)** — the OMERO **reference**-data producers. `OMERO*ReferenceData` is not a
table/image/ROI/filament, and there is **NO OMERO data leaf in `data/`** (data/ = ImageData / TableData /
RoiData / FilamentData / **Misc**). Rule 6 (DataSource by output) and rule 8 (FileSystem by what it
reads) both fall to `MiscDataFunction`. Spatial **absent** (reference inputs/outputs are not images).

| node (category) | rationale |
|---|---|
| `OMERODatasetReferenceDataSource`, `OMEROGroupReferenceDataSource`, `OMEROImageReferenceDataSource`, `OMEROPlateReferenceDataSource`, `OMEROProjectReferenceDataSource`, `OMEROScreenReferenceDataSource`, `OMEROWellReferenceDataSource` (DataSource, "OMERO") | rule 6 by OUTPUT: emit `OMERO*ReferenceData` (metadata refs) → no OMERO data leaf ⇒ `MiscDataFunction`; no image input ⇒ absent |
| `OMEROListProjects`, `OMEROListDatasets`, `OMEROListGroups`, `OMEROListPlates`, `OMEROListScreens`, `OMEROListWells`, `OMEROListDatasetImages`, `OMEROListWellImages` (FileSystem, "OMERO") | rule 8: list/navigate OMERO objects (ref in → ref out) → `MiscDataFunction`; refs are not images ⇒ absent |
| `OMEROCreateDataset` (FileSystem, "OMERO") | rule 8: creates a dataset from a project ref (ref in → dataset ref out) → `MiscDataFunction`; refs not images ⇒ absent |

**`TableDataFunction` (1)** — `DownloadOMEROTableAlgorithm` (DataSource, no menuPath): rule 6 by OUTPUT —
emits `ResultsTableData` ("Imports tables attached to an OMERO image as ImageJ table") → the table-data
leaf; input `OMEROImageReferenceData` (a ref, not an image) ⇒ **absent**.

**`ImageDataFunction` (1)** — `DownloadOMEROImageAlgorithm` (DataSource, no menuPath): rule 6 by OUTPUT —
emits **`OMEImageData`** ("Imports an image from OMERO into ImageJ") → the **image-data** leaf (data/ =
ImageData for images); input `OMEROImageReferenceData` (a ref, **not** an image) ⇒ **spatial absent**
(rule 12 reads the image *input*; there is none). **Note (f)** — the leaf/spatial split here follows two
different rules (leaf by image output, spatial by non-image input). This is the same *mechanism* as §19.3
`ExtractRoi2dFromOMEImage` (leaf by ROI output, spatial by its OME-image input), but that node's image
input makes it **both**, whereas DownloadOMEROImage's reference input makes it **absent**. ⚠️ **Phase-3
rule-refinement flag:** the 19.3 image-*importing* DataSources (`ImportImagePlus`/`BioFormatsImporter`/…)
are **both** (they emit an image from a non-image `FileData` input), so an image-*producing* source node
like DownloadOMEROImage is arguably **both** by that same output-based reasoning — its **absent** here is
the literal rule-12 (input-based) reading. Decide in Phase 3 whether image-producing source nodes declare
their **OUTPUT's** spatial (if so, flip this node to **both**, harmonizing with the 19.3 importers).

**`AnnotationsEditFunction` (6)** — the `annotate/` "For OMERO" nodes (JUDGMENT ELEMENT 1 continued).
Each **adds** OMERO metadata annotations to an `OMERO*ReferenceData` object (ref in → same ref out, now
annotated) — an annotation **editing/adding** op on the reference (the "For OMERO" segment is the target,
not the op). None *converts* between annotation types, so `AnnotationsConvertFunction` is **unused** in
omero. Spatial **absent** (refs are not images). Mirrors §19.3's `AnnotateDataWithImageProperties`/
`ImagePropertiesToAnnotation` → `AnnotationsEditFunction`.

| node | rationale |
|---|---|
| `AnnotateOMEROImageReference` | **adds** OMERO metadata to an image reference |
| `AnnotateOMERODatasetReference` | **adds** OMERO metadata to a dataset reference |
| `AnnotateOMEROProjectReference` | **adds** OMERO metadata to a project reference |
| `AnnotateOMEROPlateReference` | **adds** OMERO metadata to a plate reference |
| `AnnotateOMEROWellReference` | **adds** OMERO metadata to a well reference |
| `AnnotateOMEROScreenReference` | **adds** OMERO metadata to a screen reference |

**`ExportFileFormatsFunction` (2)** — rule 7 (Export category).

| node (menuPath) | leaf | spatial | rationale |
|---|---|---|---|
| `UploadOMEROImageToDataset` (Images) | `ExportFileFormatsFunction` | **both** | **the one OME-image node.** Uploads an `OMEImageData` into an OMERO dataset (ref in + **`OMEImageData` input** → image ref out). It is in the **Export** category (rule 7 → the export file-format leaf, NOT the images/ processing leaves); it is not a ZIP/archive ⇒ not `ExportArchiveFunction`. First image *input* = unconstrained `OMEImageData` ⇒ **both** (rule 12). (A "put data into OMERO" reading makes it export-flavored; see (f).) |
| `UploadOMEROTable` (Tables) | `ExportFileFormatsFunction` | absent | attaches a `ResultsTableData` to an OMERO image (table + ref in → annotation ref out); Export category ⇒ rule 7 file-format leaf; inputs are table + ref (no image) ⇒ **absent** |

**omero function tally:** `AnnotationsEditFunction` 6 · `MiscDataFunction` 16 · `TableDataFunction` 1 ·
`ImageDataFunction` 1 · `ExportFileFormatsFunction` 2 = **26**. Spatial: absent 25 · both 1.

#### (c) The 26-node annotations/ Edit-vs-Convert split — summary (JUDGMENT ELEMENT 1)

- **`AnnotationsEditFunction`: 24** (annotations module) + **6** (omero) = **30 total** — every
  add/set/modify/remove/rename/filter/merge/split/copy op on the annotation set.
- **`AnnotationsConvertFunction`: 2** (annotations module: `DataAnnotationToStringAnnotation`,
  `ExtractDataAnnotation`) — the only two nodes that change an annotation's **type/representation**
  (data-annotation→string-annotation; data-annotation↔main-data). No omero node converts between
  annotation types.
- **Debatable (flagged, not fixed):** `ExtractDataAnnotation` (see (f)) — "extract a data annotation into
  the main data" is the borderline case; a "column-extract = Edit" reading would move it to
  `AnnotationsEditFunction` (→ 25 Edit / 1 Convert). `DataAnnotationToStringAnnotation` is the
  unambiguous Convert (its doc is a type conversion). The 24 Edit nodes are unambiguous (all are
  add/modify/remove/filter/rename/merge on the annotation set with the data type unchanged).

#### (d) The 2 DataSource leaf choice (annotations)

Both `DataSourceNodeTypeCategory` nodes output `AnnotationTableData` (a `ResultsTableData` subclass) →
**`TableDataFunction`** (rule 6 by output; the table-**data** leaf in `data/`). `MiscDataFunction` (the
"annotation-specific, not a generic table" alternative) was rejected because `AnnotationTableData` is
literally a table. **Flagged (f)** for Phase 3 (both are annotation-flavored tables).

#### (e) Aliases (5 references) — 3 dropped, 2 → collapse to 1 secondary `function[]` (primary-first)

Per the A/B/C protocol: map the alias's old category+menuPath to a leaf by the same rules; alias-leaf ==
primary-leaf → drop, else add as a **secondary** `function[]` entry (primary first).

| node (primary leaf) | alias (old category / menuPath) | alias leaf | action |
|---|---|---|---|
| `AnnotateByExpression` (AnnotationsEdit) | (default `InternalNodeTypeCategory`) / "Set annotations" | `AnnotationsEditFunction` (rule 5, set/modify) | **drop** (== primary) |
| `AnnotateByExpression` (AnnotationsEdit) | (default) / "Modify annotations" | `AnnotationsEditFunction` | **drop** (== primary) |
| `AnnotateByExpression` (AnnotationsEdit) | (default) / "Edit annotations" | `AnnotationsEditFunction` | **drop** (== primary) |
| `ConvertToAnnotationTable` (TableData) | `AnnotationsNodeTypeCategory` / "Annotations to table" | `AnnotationsConvertFunction` (rule 5: a genuine annotation→table **conversion**) | **add secondary** |
| `ConvertToAnnotationTable` (TableData) | `MiscellaneousNodeTypeCategory` / "Annotations to table" | `AnnotationsConvertFunction` (rule 9 best-fit: the op is a conversion, not the Miscellaneous last-resort) | **collapses to the same secondary** |

The two `ConvertToAnnotationTable` aliases are **both** named "Annotations to table" and both map to
`AnnotationsConvertFunction` (the Annotations alias by rule 5; the Miscellaneous alias by rule 9's
best-fit, since the operation is a conversion — the `MiscellaneousFunction` last-resort does *not* apply
to a recognizable conversion op). They therefore collapse to **one** secondary:
`@ConfigureJIPipeNode(function = { TableDataFunction.class, AnnotationsConvertFunction.class })`
(primary-first, per the §19.2/19.5/19.6/19.7 precedent). **3 dropped, 1 secondary `function[]` entry**
(`ConvertToAnnotationTable` — the only multi-`function` node in this batch).

#### (f) Flagged for Phase 3 audit

- **`ExtractDataAnnotation` → `AnnotationsConvertFunction`** (the borderline Convert). "Extracts a data
  annotation" + "the current (main) data is **converted into an annotation**" reads as a
  data↔annotation **representation** conversion (Convert); a "extract a column" reading would make it
  `AnnotationsEditFunction`. This is the one genuinely debatable node in the 26-way split — if the audit
  flips it, the split becomes 25 Edit / 1 Convert.
- **`AnnotateWithAnnotationTable` → `AnnotationsEditFunction`.** It consumes an `AnnotationTableData`
  input (annotation *source*), which superficially resembles "table→annotations". But its operation is to
  **merge** matching annotations *into the data's annotation set* (the table is the source; the data is
  the target) — an annotation **edit**, not a data-type conversion. Kept Edit; `AnnotationsConvertFunction`
  is the alternative if "annotation-table → annotations" is read as a type conversion.
- **The 2 annotations DataSources (`AnnotationTableFromFile`, `ConvertToAnnotationTable` primary) →
  `TableDataFunction`.** Both emit `AnnotationTableData` (a `ResultsTableData` subclass) → the table-data
  leaf by output (rule 6). `MiscDataFunction` is the alternative if the audit treats annotation-tables as
  outside the generic table-data leaf (they are "annotation-specific").
- **`DownloadOMEROImageAlgorithm` → `ImageDataFunction`, spatial **absent**.** Leaf by its image OUTPUT
  (rule 6); spatial by its non-image INPUT (a reference) — the two rules point differently here
  (absent). Same *mechanism* as §19.3 `ExtractRoi2dFromOMEImage` (that one is **both** — it has an
  OME-image input — whereas this is **absent**). ⚠️ Cross-module rule question (Phase 3): the 19.3
  image-*importing* DataSources are **both** (image emitted from a `FileData` input), so an
  image-*producing* source node may belong as **both** by output; decide whether to harmonize.
- **`UploadOMEROImageToDatasetAlgorithm` → `ExportFileFormatsFunction` (both).** It is in the **Export**
  category, so rule 7 gives the export file-format leaf. A "put data into OMERO" (an external store)
  reading is export-flavored (kept); if the audit reads it as OMERO-side data management rather than a
  file-format export, `MiscDataFunction` is a weaker alternative. Its spatial is **both** (the only
  omero node with an image input — the unconstrained `OMEImageData`), per rule 12.
- **omero `OMERO*ReferenceData` producers → `MiscDataFunction` (16 nodes).** No OMERO data leaf exists in
  `data/`; the references fall to the Misc data leaf (rules 6/8). If the audit adds an OMERO/external-
  object data leaf in Phase 3, these 16 would move.
- **ij-omero `OMERO*ReferenceData` is not an image (spatial absent on 25/26).** Settled for this task
  (rule 12: non-image ⇒ absent); consistent with §19.5's filament-graph and §19.6's path/file reference
  precedent. Phase 3 should confirm spatial never extends to OMERO reference types.

#### (g) Test infrastructure + gate results

Both modules had **no `src/test` and no JUnit deps** (first test in each). For EACH (mirror §19.6 / the
`jipipe-plugin-ij`/`-ij-3d`/`-ij-filaments`/`-tables`/`-forms` pom verbatim):
- **`pom.xml`**: added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope) — inserted before `</dependencies>` (annotations: after the `jipipe-core` dependency;
  ij-omero: after the `com.zeroc:glacier2` dependency).
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/` (**byte-identical**,
  verified with `cmp` for both).
- Created `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/plugins/<pkg>/
  ImageJLegacyPatcherLauncherSessionListener.java` (module-scoped: annotations→`…plugins.annotation`,
  ij-omero→`…plugins.omero`; `LegacyInjector.preinit()` in `launcherSessionOpened`, HKI header, Javadoc
  mirroring §19.6; the ij-omero Javadoc additionally cites the module's `ij.ImagePlus` touchpoint) +
  `plugins/jipipe-plugin-<m>/src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener`
  (single line = that module's listener FQCN).

**Why the listener is needed:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent failure
(AGENTS.md patcher contract). `net.imagej.patcher.LegacyInjector` is on the test classpath transitively
via jipipe-core → `net.imagej:imagej`. The ij-omero gate in particular initializes the OMERO importer
(which wraps `ij.ImagePlus` via loci Bio-Formats), so the patcher must run first.

**Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-annotations,plugins/jipipe-plugin-ij-omero -am`
→ **exit 0** (both in one reactor; host mvn).

**Stale `~/.m2` jar (the *core* gotcha was NOT hit).** The installed `jipipe-core` SNAPSHOT jar is
**2026-10-10 03:52** (after the §19.1 core conversion commit `910aa020d6` (2026-10-09 21:07)) — **fresh**
(it contains the 74 taxonomy function classes and the converted core nodes), so no pre-19.1 core node
surfaced as unassigned in either prefix (the §19.6 stale-core condition did not recur). The gate prefix
filter (`org.hkijena.jipipe.plugins.annotation` / `…omero`) does **not** cover any core package
(`org.hkijena.jipipe.plugins.filesystem.datasources`, etc.), so even a stale core jar could not inflate the
prefix-filtered failure set. Stale *dependency* jars only inflate the informational global "N nodes not yet
assigned" line (see below) — no fix needed. No `mvn install` was run.

**Gate (enabled), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, zero unassigned
in the module prefix:**
- **annotations:** `mvn test -pl plugins/jipipe-plugin-annotations -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.annotation` → **Tests run: 1, Failures: 0,
  Errors: 0, Skipped: 0**, log line `JIPipeNodesServiceComponent | Node taxonomy validation passed`
  (zero unassigned, zero errors).
- **ij-omero:** `mvn test -pl plugins/jipipe-plugin-ij-omero -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.omero` → **Tests run: 1, Failures: 0,
  Errors: 0, Skipped: 0**, **zero unassigned in the `…omero` prefix**. The log line
  `JIPipeNodesServiceComponent | Node taxonomy: 116 nodes not yet assigned a taxonomy function — expected
  during conversion` is the **global** count across all on-classpath modules (the unconverted cellpose /
  trackmate / weka / ilastik / omnipose / clij / ij2 / ocr / plots / multi-template-matching / python /
  r / imp / scene-3d / opencv … modules + stale dependency jars); **none** are under the
  `org.hkijena.jipipe.plugins.omero` prefix, so the prefix-filtered assertion saw **zero** and passed
  (same shape as §19.3/19.4/19.5/19.6/19.7). The annotations classpath happens to have no unconverted
  on-classpath module, so it reported a clean "validation passed" with no global count line.

**Gate (unset), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the gate
**skips cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works).

**Transition note understood:** the 55 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node
panel's category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking,
unmerged branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

**Byte-identity check (reviewer contract):** all 55 node files are byte-identical to base after stripping
`import …;` lines and the `@ConfigureJIPipeNode(…)` / `@AddJIPipeNodeAlias(…)` lines (verified by a
line-filter diff across `git show HEAD:<f>` vs head — 55 files, 0 non-annotation changes) — the change is
annotations-only.

---

### 19.9 — jipipe-plugin-cellpose (21) + jipipe-plugin-omnipose (5) (26 nodes)

**Status:** converted — all 26 `@ConfigureJIPipeNode` nodes across the two modules rewritten to
`function` / `framework` / `spatialDimensions`; two module gates green (see (g)). Single-shot combined
conversion (mapping + apply + 2 gates in one commit), mirroring §19.6/§19.7/§19.8.

**First framework-variety task.** §19.6–§19.8 were function-only (`NoFramework`, the attribute omitted).
These two DL modules get **REAL frameworks** — the first batch to exercise rule 11's per-plugin framework
table with more than the `ImageJ1Framework`/`CoreFramework` shapes already seen in §19.2–§19.5. Two
distinctive elements: **(A)** framework is read **per node by subpackage** (cellpose) / **uniformly**
(omnipose); **(B)** spatial is read **per node by the first image input slot** (rule 12) — here the
`ij.ImagePlus`/`ImagePlus*Data` usage IS the spatial signal.

**Scope / gate prefixes (verified, matches controller R1):**
- `plugins/jipipe-plugin-cellpose/src/main` — base pkg **`org.hkijena.jipipe.plugins.cellpose`** (GATE
  PREFIX) — **21** annotated files (verified `grep -rl "@ConfigureJIPipeNode" src/main | wc -l` = 21,
  exactly one `@ConfigureJIPipeNode` per file). `CellposeUtils`/`CellposeImageInfo`/etc. are non-node
  helpers (no annotation) → correctly excluded.
- `plugins/jipipe-plugin-omnipose/src/main` — base pkg **`org.hkijena.jipipe.plugins.omnipose`** (GATE
  PREFIX) — **5** annotated files (verified = 5, one each). `Omnipose0Environment`/parameter classes are
  non-nodes → excluded.

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):**

| module | Images ("Deep learning") | DataSource | total |
|---|---|---|---|
| cellpose | 13 | 8 | **21** |
| omnipose | 4 | 1 | **5** |

**Aliases (verified by grep of `@AddJIPipeNodeAlias`):** **0** in both modules → no alias→secondary-
`function[]` conversions (step 2 of the per-batch method is a no-op here).

---

#### (A) Framework — by SUBPACKAGE (cellpose) / uniform (omnipose) — rule 11, per node

**The key check: exact per-framework counts (verified against each file's `package` line).**

| framework (cellpose) | count | nodes (by subpackage) |
|---|---|---|
| `Cellpose2Framework` | **3** | `algorithms/cp2` → `Cellpose2SegmentationInferenceAlgorithm`, `Cellpose2TrainingAlgorithm`, `ImportPretrainedCellpose2ModelAlgorithm` |
| `Cellpose3Framework` | **6** | `algorithms/cp3` → `Cellpose3SegmentationInferenceAlgorithm`, `Cellpose3SegmentationTrainingAlgorithm`, `Cellpose3DenoiseInferenceAlgorithm`, `Cellpose3DenoiseTrainingAlgorithm`, `ImportPretrainedCellpose3SegmentationModelAlgorithm`, `ImportPretrainedCellpose3DenoiseModelAlgorithm` |
| `Cellpose4Framework` | **4** | `algorithms/cp4` → `Cellpose4SegmentationInferenceAlgorithm`, `Cellpose4SegmentationTrainingAlgorithm`, `Cellpose4SegmentationTraining2Algorithm`, `ImportPretrainedCellpose4SegmentationModelAlgorithm` |
| **NoFramework (omitted)** | **8** | `algorithms` (base, 2): `ImportCellposeModelFromFileAlgorithm`, `ImportCellposeSizeModelFromFileAlgorithm` + `legacy/algorithms` (6): `Cellpose1InferenceAlgorithm`, `Cellpose1TrainingAlgorithm`, `LegacyCellpose2InferenceAlgorithm`, `LegacyCellpose2TrainingAlgorithm`, `ImportLegacyCellposeModelAlgorithm`, `ImportLegacyCellposeSizeModelAlgorithm` |
| **cellpose total** | **21** | |

| framework (omnipose) | count | nodes |
|---|---|---|
| `JIPipeDLFramework` | **5** (all) | `algorithms` (3): `Omnipose0InferenceAlgorithm`, `Omnipose0TrainingAlgorithm`, `ImportPretrainedOmnipose0ModelAlgorithm` + `legacy/algorithms` (2): `LegacyOmnipose0InferenceAlgorithm`, `LegacyOmnipose0TrainingAlgorithm` |
| **omnipose total** | **5** | |

**Rule 11 rationale (per framework):**
- **cellpose → `Cellpose{2,3,4}Framework` per subpackage** (rule 11 literal). The node files live in
  three version subpackages; the **`package` statement is authoritative** (verified per file — not the
  directory name). `cp2`→`Cellpose2Framework`, `cp3`→`Cellpose3Framework`, `cp4`→`Cellpose4Framework`.
- **cellpose base package `algorithms` (2 version-agnostic model-file importers) → NoFramework (OMIT).**
  There is **no version-agnostic cellpose framework** among the 12 (`jipipe-core/…/taxonomy/frameworks/`
  has only `Cellpose2/3/4Framework`); these two importers take/emit a version-agnostic `CellposeModelData`
  / `CellposeSizeModelData`, so no `CellposeN` framework fits → attribute **omitted** (the default is
  already `NoFramework`, `jipipe-core/…/api/ConfigureJIPipeNode.java:97`). **Flagged (f).**
- **cellpose `legacy.algorithms` (Cellpose v1 + legacy-v2, 6) → NoFramework (OMIT).** No `Cellpose1Framework`
  (or legacy framework) exists among the 12, so the 6 legacy nodes are **omitted** like the base importers.
  **Flagged (f)** — this is the most debatable framework call in the batch (see (f)).
- **omnipose → `JIPipeDLFramework` for ALL 5**, including the 2 `legacy.algorithms` (v1) nodes. Unlike
  cellpose, there is **no version split** for omnipose: rule 11 names omnipose → `JIPipeDLFramework`, and
  the general DL framework covers its legacy nodes (a `JIPipeDLFramework` "covers legacy" precedent, unlike
  cellpose where only version-specific frameworks exist). **This is a controller ruling** (the brief states
  JIPipeDL for all 5 explicitly); it is also the reading rule 11 most naturally gives (omnipose is on the
  named list; there is no omnipose-version-specific framework to fall back to). **Flagged (f)** in case the
  audit prefers the symmetry with cellpose (NoFramework for legacy).

**`ij.*` imports drive SPATIAL, NOT framework (rule 11 vs rule 12 — the CRITICAL distinction).** Both
modules' node files incidentally `import ij.*` (cellpose 13 files, omnipose 4 files — `ij.ImagePlus`,
`ij.IJ`, `ij.process.ImageProcessor`, …) for ImagePlus-level processing. **Do NOT** read this as
"the node wraps IJ1 ⇒ `ImageJ1Framework`". Rule 11's framework table is **plugin-scoped, not
import-scoped**: the framework is the *plugin's* deep-learning framework (`CellposeN`/`JIPipeDL`/
NoFramework), regardless of the incidental `ij.*` usage. The `ij.*` imports instead feed **rule 12
(spatial)** — the `ImagePlus*Data` first-image-input slot is what confers `SPATIAL_*`.
**Verified: `grep -rl "ImageJ1Framework\|NoFramework" src/main` across both modules → 0** (no `ImageJ1Framework`
assigned to any DL node; no literal `NoFramework` token — it is expressed by *omitting* the attribute).

---

#### (B) Function — the `deeplearning/` branch — by operation, per node

The three `jipipe-core/…/taxonomy/functions/deeplearning/` leaves (read `getName()`):
`DeepLearningInferenceFunction` ("Deep learning inference" — *running* a model on data),
`DeepLearningModelsFunction` ("Deep learning models" — *managing* model files), and
`DeepLearningTrainingFunction` ("Deep learning training" — *training* a model).

The split is by **operation**, not by category:
- **Run-the-model nodes** (`ImagesNodeTypeCategory` + menuPath `"Deep learning"`, produce
  segmentation/masks/labels/flows/probabilities) → `DeepLearningInferenceFunction`.
- **Model-importer nodes** (`DataSourceNodeTypeCategory`, `Import*Model`, read model FILES and output
  `CellposeModelData`/`CellposeSizeModelData`/`LegacyCellposeModelData`) → `DeepLearningModelsFunction`
  (model **management**, NOT inference).
- **Training nodes** → `DeepLearningTrainingFunction`. ⚠️ **Deviates from the brief's "no training nodes
  exist" premise — see (f):** 7 cellpose + 2 omnipose nodes genuinely train (`arguments.add("--train")`
  / `"Trains a … model"`), so per the brief's escape hatch ("if you find a node that genuinely trains,
  use it + record") they get `DeepLearningTrainingFunction`.

**Function split (verified by grep):**

| module | Inference | Training | Models | total |
|---|---|---|---|---|
| cellpose | **6** | **7** | **8** | **21** |
| omnipose | **2** | **2** | **1** | **5** |
| **total** | **8** | **9** | **9** | **26** |

Note the brief's "13 Inference (cellpose)" = **6 true inference + 7 training** lumped under the old
"Deep learning" menuPath. The correct rule-11/`deeplearning/` split separates the 7 trainers (which is
what (f) records). The "8 Models (cellpose)" and "4 Inference (omnipose)" premises are confirmed; only the
training sub-count (and the omnipose "4 Inference" = 2 inference + 2 training) is corrected.

---

#### (C) Spatial — by FIRST IMAGE INPUT slot (rule 12), per node

Every inference/training node's spatial is read from its **first image input** `@AddJIPipeInputSlot`:
- unconstrained `ImagePlusData` ⇒ **both** (`SPATIAL_2D, SPATIAL_3D`)
- `ImagePlus3DData` / `ImagePlus3DGreyscaleData` (3D-specific) ⇒ **`SPATIAL_3D`** only
- a 2D-specific class ⇒ `SPATIAL_2D`
- model-import DataSources (`Import*Model`) take **no image input** (they read model `FileData`) ⇒ **absent**
  (`spatialDimensions` omitted).

**Spatial distribution (verified by grep):**

| module | both (2D+3D) | 3D only | 2D only | absent | total |
|---|---|---|---|---|---|
| cellpose | **11** | **2** | 0 | **8** | **21** |
| omnipose | **4** | 0 | 0 | **1** | **5** |

**The 2 cellpose 3D-only cases (named):**
- `legacy/algorithms/Cellpose1InferenceAlgorithm` — first image input `ImagePlus3DGreyscaleData` ⇒ `SPATIAL_3D`.
- `legacy/algorithms/Cellpose1TrainingAlgorithm` — first image input `ImagePlus3DData` ⇒ `SPATIAL_3D`.

All 11 cellpose "both" + 4 omnipose "both" nodes take an unconstrained `ImagePlusData` first image input.
The 8 cellpose + 1 omnipose absent nodes are the `Import*Model` DataSources (first input is `FileData`,
not an image) ⇒ rule 12 "non-image ⇒ absent". **No 2D-only case exists** (no node's first image input is
a 2D-specific class).

---

#### (d) jipipe-plugin-cellpose (21) — per-node table

| node (subpackage) | function | framework | spatial | note |
|---|---|---|---|---|
| `Cellpose2SegmentationInferenceAlgorithm` (cp2) | Inference | `Cellpose2Framework` | both | first input `ImagePlusData` |
| `Cellpose2TrainingAlgorithm` (cp2) | **Training** | `Cellpose2Framework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `ImportPretrainedCellpose2ModelAlgorithm` (cp2) | Models | `Cellpose2Framework` | absent | DataSource, `CellposeModelData` out, `FileData`-free (model-file import) |
| `Cellpose3SegmentationInferenceAlgorithm` (cp3) | Inference | `Cellpose3Framework` | both | first input `ImagePlusData` |
| `Cellpose3SegmentationTrainingAlgorithm` (cp3) | **Training** | `Cellpose3Framework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `Cellpose3DenoiseInferenceAlgorithm` (cp3) | Inference | `Cellpose3Framework` | both | first input `ImagePlusData` |
| `Cellpose3DenoiseTrainingAlgorithm` (cp3) | **Training** | `Cellpose3Framework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `ImportPretrainedCellpose3SegmentationModelAlgorithm` (cp3) | Models | `Cellpose3Framework` | absent | DataSource |
| `ImportPretrainedCellpose3DenoiseModelAlgorithm` (cp3) | Models | `Cellpose3Framework` | absent | DataSource |
| `Cellpose4SegmentationInferenceAlgorithm` (cp4) | Inference | `Cellpose4Framework` | both | first input `ImagePlusData` |
| `Cellpose4SegmentationTrainingAlgorithm` (cp4) | **Training** | `Cellpose4Framework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `Cellpose4SegmentationTraining2Algorithm` (cp4) | **Training** | `Cellpose4Framework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `ImportPretrainedCellpose4SegmentationModelAlgorithm` (cp4) | Models | `Cellpose4Framework` | absent | DataSource |
| `ImportCellposeModelFromFileAlgorithm` (base) | Models | —(NoFramework) | absent | version-agnostic importer (flag f) |
| `ImportCellposeSizeModelFromFileAlgorithm` (base) | Models | —(NoFramework) | absent | version-agnostic importer (flag f) |
| `Cellpose1InferenceAlgorithm` (legacy) | Inference | —(NoFramework) | **3D** | first input `ImagePlus3DGreyscaleData` (flag f) |
| `Cellpose1TrainingAlgorithm` (legacy) | **Training** | —(NoFramework) | **3D** | first input `ImagePlus3DData` (flag f) |
| `LegacyCellpose2InferenceAlgorithm` (legacy) | Inference | —(NoFramework) | both | first input `ImagePlusData` (flag f) |
| `LegacyCellpose2TrainingAlgorithm` (legacy) | **Training** | —(NoFramework) | both | first input `ImagePlusData` (flag f) |
| `ImportLegacyCellposeModelAlgorithm` (legacy) | Models | —(NoFramework) | absent | DataSource, `LegacyCellposeModelData` out (flag f) |
| `ImportLegacyCellposeSizeModelAlgorithm` (legacy) | Models | —(NoFramework) | absent | DataSource, `LegacyCellposeSizeModelData` out (flag f) |

#### (e) jipipe-plugin-omnipose (5) — per-node table

All 5 → `JIPipeDLFramework` (rule 11, uniform — no version split; the 2 legacy nodes included by
controller ruling, flag f).

| node (subpackage) | function | framework | spatial | note |
|---|---|---|---|---|
| `Omnipose0InferenceAlgorithm` (algorithms) | Inference | `JIPipeDLFramework` | both | first input `ImagePlusData` |
| `Omnipose0TrainingAlgorithm` (algorithms) | **Training** | `JIPipeDLFramework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `LegacyOmnipose0InferenceAlgorithm` (legacy) | Inference | `JIPipeDLFramework` | both | first input `ImagePlusData` (flag f) |
| `LegacyOmnipose0TrainingAlgorithm` (legacy) | **Training** | `JIPipeDLFramework` | both | `--train`; first input `ImagePlusData` (flag f) |
| `ImportPretrainedOmnipose0ModelAlgorithm` (algorithms) | Models | `JIPipeDLFramework` | absent | DataSource, `CellposeModelData` out |

---

#### (f) Flagged for Phase 3 audit

- **⚠️ The "no training nodes exist" premise is FALSE — 9 genuine trainers were found and split out.**
  The brief's function split ("13 Inference + 8 Models (cellpose)", "4 Inference + 1 Models (omnipose)",
  "0 Training") lumps every old-menuPath-`"Deep learning"` node as Inference. In reality **7 cellpose
  nodes** (`Cellpose2TrainingAlgorithm`, `Cellpose3{Segmentation,Denoise}TrainingAlgorithm`,
  `Cellpose4SegmentationTraining{,2}Algorithm`, `Cellpose1TrainingAlgorithm`,
  `LegacyCellpose2TrainingAlgorithm`) and **2 omnipose nodes** (`Omnipose0TrainingAlgorithm`,
  `LegacyOmnipose0TrainingAlgorithm`) **train** models — verified by `arguments.add("--train")` and the
  `"Trains a … model"` docstrings. Per the brief's own escape hatch ("if you find a node that genuinely
  trains, use it + record"), they are assigned `DeepLearningTrainingFunction`, giving the corrected split
  **cellpose 6 Inference / 7 Training / 8 Models** and **omnipose 2 Inference / 2 Training / 1 Models**.
  **Decision recorded, not silently absorbed** — if the audit prefers the brief's "lump under Inference",
  revert the 9 `Training` rows to `Inference` (mechanical). This is the single material deviation.
- **cellpose base-package importers (2) → NoFramework (omitted).** No version-agnostic cellpose framework
  exists among the 12. Kept NoFramework (omitted) per rule 11's "else → NoFramework". If Phase 3 adds a
  version-agnostic `CellposeFramework`, these 2 (and arguably all 21) would move.
- **cellpose `legacy.algorithms` (6) → NoFramework (omitted).** No `Cellpose1Framework`/legacy framework
  exists. This is the **most debatable framework call**: the 6 legacy nodes (Cellpose v1 + legacy-v2) are
  omitted. An alternative reading would give them the *closest* version framework, but that would mis-state
  the version (v1 is neither 2/3/4), so NoFramework (omitted) is the honest choice. **Flag for Phase 3** —
  a `Cellpose1Framework` (or a shared legacy-DL framework) would resolve it.
- **omnipose legacy (2) → `JIPipeDLFramework` (by controller ruling).** The brief explicitly rules
  JIPipeDL for all 5 (no version split; the general DL framework covers legacy). This **breaks symmetry
  with cellpose** (whose legacy nodes go NoFramework because no version-agnostic cellpose framework
  exists). The asymmetry is defensible (omnipose *has* a version-agnostic framework on the rule-11 list;
  cellpose *doesn't*), but if the audit wants strict symmetry it would flip the 2 omnipose legacy nodes to
  NoFramework (omitted). **Flag, do not change.**
- **ij.* imports → spatial, not framework (confirmed).** 13 cellpose + 4 omnipose files import `ij.*`
  (`ij.ImagePlus` etc.). None is read as `ImageJ1Framework` (the DL framework is the plugin framework,
  rule 11 plugin-scoped). The `ij.*` usage only confers the `SPATIAL_*` by rule 12. `grep -rl
  "ImageJ1Framework\|NoFramework" src/main` → **0**.

---

#### (g) Test infrastructure + gate results

**cellpose** (base pkg `org.hkijena.jipipe.plugins.cellpose`): pom **already** had
`org.junit.jupiter:junit-jupiter` (test scope — pre-existing, for the pure-unit
`CellposeVersionUtilsTest`, which touches **no** `ij.*`) but **not** `junit-platform-launcher`.
- **`pom.xml`:** added **only** `org.junit.platform:junit-platform-launcher` (test scope) after the
  existing `junit-jupiter` dep.
- Copied `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-cellpose/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/`
  (**byte-identical**, verified with `cmp`).
- Created `plugins/jipipe-plugin-cellpose/src/test/java/org/hkijena/jipipe/plugins/cellpose/
  ImageJLegacyPatcherLauncherSessionListener.java` (`LegacyInjector.preinit()` in
  `launcherSessionOpened`, HKI header, Javadoc citing the module's `ij.ImagePlus` touchpoint) +
  `src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener`
  (single line = that FQCN). The pre-existing `CellposeVersionUtilsTest` must stay green (the module
  `mvn test` runs it + the gate) — **it does** (4 tests, green, see unset run below).

**omnipose** (base pkg `org.hkijena.jipipe.plugins.omnipose`): pom had **no** JUnit deps (first test) →
full wiring (mirror `jipipe-plugin-ij`/`-ij-3d`):
- **`pom.xml`:** added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (both test scope) after the last dependency (`jipipe-plugin-cellpose`).
- Copied the gate test → `plugins/jipipe-plugin-omnipose/src/test/java/org/hkijena/jipipe/api/nodes/
  taxonomy/` (**byte-identical**, verified with `cmp`).
- Created `plugins/jipipe-plugin-omnipose/src/test/java/org/hkijena/jipipe/plugins/omnipose/
  ImageJLegacyPatcherLauncherSessionListener.java` + `src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = that FQCN).

**Why the listener is needed (both):** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context.
A module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath. **Both modules genuinely load
`ij.*` node classes** (their inference/training algorithms use `ij.ImagePlus`), so the ImageJ2 legacy
patcher (`net.imagej.patcher.LegacyInjector`) **must** run first — otherwise the patcher fails
(`No _hooks field found in ij.IJ` / `LinkageError: duplicate class definition`), corrupting the context
so no node infos register (AGENTS.md patcher contract). `LegacyInjector` is on the test classpath
transitively via jipipe-core → `net.imagej:imagej`.

**Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-cellpose,plugins/jipipe-plugin-omnipose -am`
→ **exit 0** (both in one reactor; host mvn — the `Cellpose{2,3,4}Framework` / `JIPipeDLFramework` /
`DeepLearning{Inference,Training,Models}Function` symbols all resolve against the installed core).

**Stale `~/.m2` jar (the *core* gotcha was NOT hit).** The installed `jipipe-core` SNAPSHOT jar is
**2026-10-10 03:52**, newer than the last jipipe-core taxonomy commit (`aeab2dcacbed`, 2026-10-09 20:19)
— **fresh** (it contains the 74+ taxonomy function classes and the converted core nodes), so no pre-19.1
core node surfaced as unassigned in either prefix (the §19.6 stale-core condition did not recur). The gate
prefix filter (`org.hkijena.jipipe.plugins.cellpose` / `…omnipose`) does **not** cover any core package, so
even a stale core jar could not inflate the prefix-filtered failure set. Stale *dependency* jars only
inflate the informational global "N nodes not yet assigned" line (see below) — **no `mvn install` was run.**

**Gate (enabled), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, zero unassigned
in the module prefix:**
- **cellpose:** `mvn test -pl plugins/jipipe-plugin-cellpose -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.cellpose` → **Tests run: 1, Failures: 0,
  Errors: 0, Skipped: 0**, **zero unassigned in the `…cellpose` prefix**. Log line
  `JIPipeNodesServiceComponent | Node taxonomy: 554 nodes not yet assigned a taxonomy function — expected
  during conversion` is the **global** count across all on-classpath modules (the unconverted trackmate /
  weka / ilastik / clij / ij2 / ocr / plots / multi-template-matching / python / r / imp / scene-3d /
  opencv / omnipose-before-this-batch … modules + stale dependency jars); **none** are under the
  `org.hkijena.jipipe.plugins.cellpose` prefix, so the prefix-filtered assertion saw **zero** and passed
  (same shape as §19.3/19.4/19.5/19.6/19.7/19.8).
- **omnipose:** `mvn test -pl plugins/jipipe-plugin-omnipose -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.omnipose` → **Tests run: 1, Failures: 0,
  Errors: 0, Skipped: 0**, **zero unassigned in the `…omnipose` prefix**. Log line
  `Node taxonomy: 575 nodes not yet assigned a taxonomy function — expected during conversion` is the
  **global** count (its classpath additionally includes the on-classpath cellpose module, but all 21
  cellpose nodes are now assigned, so they do not contribute; **none** are under the
  `org.hkijena.jipipe.plugins.omnipose` prefix) → assertion **zero**, passed.

**Gate (unset), both → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the gate
**skips cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works). The
**cellpose** unset run additionally ran the pre-existing `CellposeVersionUtilsTest` → **Tests run: 4,
Failures: 0, Errors: 0, Skipped: 0** (module total `Tests run: 5, …, Skipped: 1` — the 1 skip is the
gated gate), confirming the pre-existing test stays green.

**Transition note understood:** the 26 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node
panel's category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking,
unmerged branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

**Byte-identity check (reviewer contract):** all 26 node files are byte-identical to base after stripping
`import …;` lines and the `@ConfigureJIPipeNode(…)` line (verified by a line-filter diff across
`git show HEAD:<f>` vs head — 26 files, 0 non-annotation changes) — the change is annotations-only.
(Import re-arrangement: the new taxonomy imports — static `SPATIAL_*` (where spatial present) + framework
(where framework present) + the `deeplearning/` leaf — are prepended to the import block and the
`…nodes.categories.*` import is dropped; this is the permitted import fixup, not a body change.)

### 19.10 — jipipe-plugin-ij-trackmate (18 annotated + 14 runtime = 32 registered nodes)

**Status:** converted — all 18 `@ConfigureJIPipeNode`-annotated nodes rewritten to
`function`/`framework`/`spatialDimensions`, **plus** the 14 runtime-registered TrackMate
detector/tracker data sources assigned via programmatic overrides; module gate green (see (e)).
Single-shot module conversion (mapping + apply + gate in one commit), mirroring §19.5/§19.9.
**Note:** the plan's "jipipe-plugin-trackmate" is the maven module
`plugins/jipipe-plugin-ij-trackmate` (base package `org.hkijena.jipipe.plugins.ijtrackmate` = the
gate prefix).

**The two distinctive elements (per controller):**
- **(A) Framework = `TrackMateFramework` for ALL nodes (rule 11: trackmate→`TrackMateFramework`).**
  Every node gets `TrackMateFramework` — 18 by annotation + 14 by programmatic `getFramework()`
  override. **No `ImageJ1Framework`, no `NoFramework`.** 7 files incidentally `import ij.*`
  (`ij.ImagePlus`, `ij.ImageStack`, `ij.process.*`) for ImagePlus-level rendering — that drives
  **SPATIAL (rule 12), NOT framework**: rule 11's framework is plugin-scoped, not import-scoped
  (same §19.9 `ij.*`-vs-framework distinction).
- **(B) Function = the `tracking/` branch (4 leaves) by node OP**, with judgment for the
  render/convert nodes. The four `jipipe-core/…/taxonomy/functions/tracking/` leaves (read
  `getName()`): `TrackingDetectionFunction` ("Tracking: detection"), `TrackingLinkingFunction`
  ("…linking"), `TrackingFilteringFunction` ("…filtering"), `TrackingAnalysisFunction`
  ("…analysis").

**Scope / gate prefix (verified):** `plugins/jipipe-plugin-ij-trackmate/src/main`, base pkg
`org.hkijena.jipipe.plugins.ijtrackmate` (GATE PREFIX) — **18** `@ConfigureJIPipeNode` files (verified
`grep -rl "@ConfigureJIPipeNode" src/main | wc -l` = 18, exactly one per file). **0 `@AddJIPipeNodeAlias`**
in the module → no alias→secondary-`function[]` conversions.

#### (A) Inventory — the 32 registered nodes, NOT the 18

**The module registers 32 nodes, not 18.** `TrackMatePlugin.registerNodes()` registers the **18**
annotated node classes. **Additionally**, `TrackMateUtils.registerSpotDetectors/registerSpotTrackers`
(`…/utils/TrackMateUtils.java:119-147`) iterate the on-classpath TrackMate
`SpotDetectorFactory`/`SpotTrackerFactory` plugins and, at startup, create one
`CreateSpotDetectorNodeInfo` / `CreateSpotTrackerNodeInfo` each and call
`registerNodeType(JIPipeNodeInfo, …)` — i.e. **14 more nodes** (7 detectors: DoG, Hessian, Label image,
LoG, Manual, Mask, Thresholding; 7 trackers: Manual, Simple LAP, LAP, Advanced Kalman, Kalman, Overlap,
Nearest-neighbor) registered **programmatically at runtime**. These classes implement
`JIPipeNodeInfo` **directly** (they are not `JIPipeMutableNodeInfo` and carry no
`@ConfigureJIPipeNode`), so their core `getPrimaryFunction()` default returns `null`
(`jipipe-core/…/api/nodes/JIPipeNodeInfo.java:312`) and `getInstanceClass()` returns
`CreateSpotDetectorNode`/`CreateSpotTrackerNode` (both under the prefix). The gate
(`TaxonomyConversionReportTest`) filters by `instanceClass.getName().startsWith(prefix)` and asserts
**zero** unassigned — so the 14 runtime nodes are *inside* the filtered set and **fail the gate while
unassigned**. The brief scoped this task to the 18 annotated files ("18 nodes"), but its own acceptance
criterion ("zero unassigned in THAT prefix") and rule "Do NOT leave any node without a function" cannot
both hold while the 14 stay `null`. **Decision: assign all 14** (see (c)); the module is genuinely
32-node-green, not 18. This is the one place the batch extends beyond the literal 18-file list, and it
is forced by the gate's contract, not by judgment.

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):** all 18 annotated nodes were
`ImagesNodeTypeCategory` (menuPath `Tracking` and its `\Filter`, `\Measure`, `\Split/Merge`,
`\Visualize`, `\Convert` submenus). The 14 runtime nodes are `DataSourceNodeTypeCategory`
(`CreateSpot{Detector,Tracker}NodeInfo.getCategory()`).

#### (B) The 18 annotated nodes → the four `tracking/` leaves, by OP

| node (old menuPath) | leaf | spatial | rationale |
|---|---|---|---|
| `SpotDetectorNode` (Tracking) | `TrackingDetectionFunction` | **both** | "Detect spots using TrackMate" — image + detector ⇒ `SpotsCollectionData`; the detection leaf |
| `TrackingNode` (Tracking) | `TrackingLinkingFunction` | absent | "Track spots using TrackMate" — spots + tracker ⇒ `TrackCollectionData` (links spots into tracks); the linking leaf |
| `SpotFilterNode` (Tracking\nFilter) | `TrackingFilteringFunction` | absent | "Filter TrackMate spots via expressions" — the filtering leaf |
| `TrackFilterNode` (Tracking\nFilter) | `TrackingFilteringFunction` | absent | "Filter TrackMate spots via expressions" (tracks) — the filtering leaf |
| `SplitSpotsNode` (Tracking\nSplit/Merge) | `TrackingFilteringFunction` | absent | "Creates a list for each individual spot" — splits the spot set; no `tracking/` split leaf ⇒ filtering (judgment, flagged (d)) |
| `MergeSpotsNode` (Tracking\nSplit/Merge) | `TrackingFilteringFunction` | absent | "Merges spot lists" — joins spot sets; no `tracking/` split/merge leaf ⇒ filtering (judgment, flagged (d)) |
| `SplitTracksNode` (Tracking\nSplit/Merge) | `TrackingFilteringFunction` | absent | "Creates a list for each individual track" — splits the track set ⇒ filtering (judgment, flagged (d)) |
| `MergeTracksNode` (Tracking\nSplit/Merge) | `TrackingFilteringFunction` | absent | "Merges track lists" — joins track sets ⇒ filtering (judgment, flagged (d)) |
| `MeasureSpotsNode` (Tracking\nMeasure) | `TrackingAnalysisFunction` | absent | "Measures the spots and outputs the results into a table" (`ResultsTableData` out) — the analysis leaf |
| `MeasureTracksNode` (Tracking\nMeasure) | `TrackingAnalysisFunction` | absent | "Measures the tracks and outputs the results into a table" (`ResultsTableData` out) |
| `MeasureEdgesNode` (Tracking\nMeasure) | `TrackingAnalysisFunction` | absent | "Measures the edges and outputs the results into a table" (`ResultsTableData` out) |
| `MeasureBranchesNode` (Tracking\nMeasure) | `TrackingAnalysisFunction` | absent | "Analyzes each branch of all tracks" (`ResultsTableData` out) |
| `SpotsToRGBNode` (Tracking\nVisualize) | `TrackingAnalysisFunction` | **both** | JUDGMENT — renders spots/tracks to RGB (see (c)) |
| `TracksToRGBNode` (Tracking\nVisualize) | `TrackingAnalysisFunction` | **both** | JUDGMENT — renders tracks to RGB (see (c)) |
| `TrackSchemeRendererNode` (Tracking\nVisualize) | `TrackingAnalysisFunction` | absent | JUDGMENT — renders the track scheme to an image (see (c)) |
| `FollowSpotsPerTrackNode` (Tracking\nVisualize) | `TrackingAnalysisFunction` | **both** | JUDGMENT — per-track follow images (see (c)) |
| `ConvertSpotsToRoiNode` (Tracking\nConvert) | `TrackingAnalysisFunction` | absent | JUDGMENT — spots ⇒ ROIs (see (c)) |
| `ConvertTracksToRoiNode` (Tracking\nConvert) | `TrackingAnalysisFunction` | absent | JUDGMENT — tracks ⇒ ROI lists (see (c)) |

**Function distribution (the 18 annotated):** `TrackingFilteringFunction` **6** (2 filter + 2 split +
2 merge) · `TrackingAnalysisFunction` **10** (4 measure + 6 judgment) · `TrackingDetectionFunction`
**1** · `TrackingLinkingFunction` **1**. **0 nodes without a function.**

#### (C) The 6 judgment nodes (render/convert) — no obvious `tracking/` leaf

There is **no "tracking render" or "tracking export" leaf**, so these six are genuine best-fit picks.
**All six take tracking data (`SpotsCollectionData`/`TrackCollectionData`) as their primary input and
*visualize/export the result of tracking***; none produces new tracking state. The `tracking/` branch
has no rendering/export leaf, and the cross-branch leaves (`images/` render-to-image, `rois/`
convert-to-ROI) describe the *output* artifact rather than the node's identity. The chosen reading —
**all six → `TrackingAnalysisFunction`** — is that the node *belongs to the tracking branch* and its
operation is analyzing/visualizing tracking output. **This directly mirrors §19.5's filament
precedent** (`ConvertFilamentsToRGB` → `FilamentDataFunction`, `ConvertFilamentsToRoi` →
`FilamentDataFunction`, where the source-data's branch won over the `images/`/`rois/` output leaf).
Each is **flagged for Phase 3 (d)**; the alternative cross-branch leaf is recorded per node.

| node | assigned | first image input (rule 12) | cross-branch alternative (flagged) |
|---|---|---|---|
| `SpotsToRGBNode` | `TrackingAnalysisFunction` | optional `ImagePlusColorRGBData` *Image* (2nd slot) ⇒ **both** | `images/ColorStackFunction` (render-to-image) |
| `TracksToRGBNode` | `TrackingAnalysisFunction` | optional `ImagePlusColorRGBData` *Image* (2nd slot) ⇒ **both** | `images/ColorStackFunction` (render-to-image) |
| `TrackSchemeRendererNode` | `TrackingAnalysisFunction` | none (tracks ⇒ rendered scheme image) ⇒ **absent** | `images/` render-to-image leaf |
| `FollowSpotsPerTrackNode` | `TrackingAnalysisFunction` | required `ImagePlusData` *Image* (1st slot) ⇒ **both** | `images/` render-to-image leaf |
| `ConvertSpotsToRoiNode` | `TrackingAnalysisFunction` | none (spots ⇒ `Roi2dListData`) ⇒ **absent** | `rois/RoiFiltersFunction` (convert-to-ROI) |
| `ConvertTracksToRoiNode` | `TrackingAnalysisFunction` | none (tracks ⇒ `Roi2dListData`) ⇒ **absent** | `rois/RoiFiltersFunction` (convert-to-ROI) |

**Spatial (rule 12, first image INPUT slot):** 14 of 18 take only tracking data
(`TrackCollectionData`/`SpotsCollectionData`/`SpotDetectorData`/`SpotTrackerData` — not images) ⇒
**absent** (no `spatialDimensions` field). 4 have an image input ⇒ **both** (unconstrained
`ImagePlusData`/`ImagePlusColorRGBData`): `SpotDetectorNode`, `SpotsToRGBNode`, `TracksToRGBNode`,
`FollowSpotsPerTrackNode`. **Distribution: absent = 14 · both = 4** (matches the brief's ~14/~4
expectation). The two RGB renderers' `Image` slot is `ImagePlusColorRGBData` (2nd slot, behind the
required spots/tracks slot — an image slot *confers* spatial per §19.5's reading), hence both.

#### (d) Flagged for Phase 3 audit

- **The 14 runtime `CreateSpot{Detector,Tracker}NodeInfo` → `MiscDataFunction` + `TrackMateFramework`**
  (assigned by overriding `getPrimaryFunction()`/`getFramework()` on the programmatic
  `JIPipeNodeInfo` classes — the annotation cannot reach them). Rule 6 (DataSource → data leaf by
  output type): output is `SpotDetectorData`/`SpotTrackerData` — **not** an image/table/ROI/filament
  type ⇒ the data/ **misc** leaf. **Alternative (role-based):** detectors ⇒ `TrackingDetectionFunction`,
  trackers ⇒ `TrackingLinkingFunction`, since these are the TrackMate algorithm factories the
  `SpotDetectorNode`/`TrackingNode` consume. The data/ misc leaf was chosen as the rule-6 literal; a
  role-based `tracking/` assignment is equally defensible and may be preferable if the audit groups by
  the factory's role rather than the output data type.
- **`Split{Spots,Tracks}Node` + `Merge{Spots,Tracks}Node` (4) → `TrackingFilteringFunction`.** There is
  no `tracking/` "split/merge" leaf; the four structural set-operations take the filtering leaf as best
  fit (the closest `tracking/` leaf to "select/retain a subset of the set"). If the audit adds a
  `tracking/` split/merge (or "edit") leaf, all four move.
- **The 6 render/convert judgment nodes → `TrackingAnalysisFunction`** (see (C) per-node alternatives).
  Chosen the `tracking/` branch leaf by node identity (they consume/visualize tracking output) over the
  cross-branch `images/`/`rois/` output leaf, mirroring §19.5's filaments precedent. If the audit
  prefers output-artifact grouping, `SpotsToRGB`/`TracksToRGB`/`TrackSchemeRenderer`/`FollowSpotsPerTrack`
  move to an `images/` render leaf and `ConvertSpotsToRoi`/`ConvertTracksToRoi` to
  `rois/RoiFiltersFunction`.

#### (e) Test infrastructure

The pom **already** had `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
(added by Task 13) — **left UNCHANGED** (no `pom.xml` in the commit). One pre-existing pure-unit test,
`…/nodes/TrackMateFamilyStepsTest.java` (uses `FamilyAssembler`, touches no `ij.*`, no
`ensureInstance`), was already present and **must stay green** (the module `mvn test` runs it + the gate).
Added:
- **Copied** `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyConversionReportTest.java`
  → `plugins/jipipe-plugin-ij-trackmate/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/`
  (**byte-identical**, verified with `cmp`).
- **Created** `plugins/jipipe-plugin-ij-trackmate/src/test/java/org/hkijena/jipipe/plugins/ijtrackmate/
  ImageJLegacyPatcherLauncherSessionListener.java` — the module-scoped mirror of the core/cellpose
  listeners (pkg `org.hkijena.jipipe.plugins.ijtrackmate`; `LegacyInjector.preinit()` in
  `launcherSessionOpened`, HKI header, Javadoc adapted to this module's ImageJ1 touchpoints —
  `ij.ImagePlus`/`ij.ImageStack`/`ij.process.*`).
- **Created** `plugins/jipipe-plugin-ij-trackmate/src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = the listener's FQN).

**Why the listener is required:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent
failure (AGENTS.md patcher contract). This module's render nodes reference `ij.ImagePlus`,
`ij.ImageStack`, `ij.process.ColorProcessor`/`ImageProcessor`, so it is required.

#### (f) Gate result

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-ij-trackmate -am` → **exit 0** (host mvn;
  no podman fallback needed).
- **Gate (enabled):** `mvn test -pl plugins/jipipe-plugin-ij-trackmate -Dtest=TaxonomyConversionReportTest
  -Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ijtrackmate` → **BUILD SUCCESS**,
  `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` — **zero unassigned** in the
  `org.hkijena.jipipe.plugins.ijtrackmate` prefix and zero taxonomy `errors()` (the 14 runtime nodes are
  assigned, so they do not appear). **First run was RED** — before the 14 overrides, the gate listed all
  14 `trackmate-create-spot-detector-*`/`trackmate-create-spot-tracker-*` ids as unassigned; adding the
  two `getPrimaryFunction()`/`getFramework()` overrides turned it green. The log line
  `JIPipeNodesServiceComponent | Node taxonomy: 545 nodes not yet assigned a taxonomy function — expected
  during conversion` is the **global** count across all on-classpath modules (the unconverted weka /
  ilastik / clij / ij2 / ocr / plots / multi-template-matching / python / r / imp / scene-3d / opencv …
  modules + dependency jars); **none** are under the `org.hkijena.jipipe.plugins.ijtrackmate` prefix, so
  the prefix-filtered assertion saw **zero** and passed (same shape as §19.3/…/§19.9).
- **Gate (unset) / full module:** `mvn test -pl plugins/jipipe-plugin-ij-trackmate` → **BUILD SUCCESS**,
  module total `Tests run: 6, Failures: 0, Errors: 0, Skipped: 1` — the gate **skips cleanly** (property
  unset, the 1 skip) and the pre-existing `TrackMateFamilyStepsTest` → `Tests run: 5, Failures: 0,
  Errors: 0, Skipped: 0` (**green**, module stays green). No `_hooks`/`LinkageError` (listener works).
- **Stale-jar gotcha (19.6–19.9):** **not hit.** The enabled gate never surfaced any CORE
  (`org.hkijena.jipipe.*`) node as globally-unassigned that this batch was expected to fix; the 545
  global count is purely the still-unconverted *other* modules + dependency jars and is outside the
  filtered prefix. No `mvn install -pl jipipe-core` was run and no jipipe-core source was touched.

#### (g) Family-step annotation preservation (reviewer contract)

**14 of the 18 node files carry Task-13 `@JIPipeNodeTaxonomyFamilyStepAnnotation` family-step markers**
(`SpotFilterNode`, `SpotsToRGBNode`, `MeasureSpotsNode`, `SpotDetectorNode`, `TrackingNode`,
`MeasureBranchesNode`, `FollowSpotsPerTrackNode`, `TrackFilterNode`, `TracksToRGBNode`,
`MeasureTracksNode`, `TrackSchemeRendererNode`, `MeasureEdgesNode`, `ConvertSpotsToRoiNode`,
`ConvertTracksToRoiNode`). These are **separate from `@ConfigureJIPipeNode`** and were **already
correct — left byte-identical.** Verified by line-filter `git diff`: in every one of the 18 node files
the *only* changed lines are the import block and the single `@ConfigureJIPipeNode(…)` line — **no
`@JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, …)` line appears in any diff**
(14/14 preserved). The 2 NodeInfo files changed only by the added imports + the two overridden methods
(no other line).

**Transition note understood:** the 18 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node
panel's category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking,
unmerged branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

### 19.11a — jipipe-plugin-ij-weka (9) + jipipe-plugin-ilastik (8) + jipipe-plugin-opencv (6) + jipipe-plugin-scene-3d (7) (30 nodes)

**Status:** converted — all 30 `@ConfigureJIPipeNode` nodes across the four modules rewritten to
`function`/`framework`/`spatialDimensions`; four module gates green (see (g)). Single-shot combined
conversion (mapping + apply + 4 gates in one commit), mirroring §19.6–§19.10.
**Module-name note:** the plan's "weka (9)" is the maven module `plugins/jipipe-plugin-ij-weka` (base
pkg `org.hkijena.jipipe.plugins.ijweka` = gate prefix).

**The four framework shapes (per controller ruling, rule 11):**
- **ij-weka → `JIPipeDLFramework` × 9** (rule 11: weka→JIPipeDL). WEKA is *classical* ML (Trainable
  Weka Segmentation), but rule 11 puts weka under the DL framework and the `deeplearning/` branch is
  its consistent home — **flagged (f)** as "classical ML under `deeplearning/`".
- **ilastik → `JIPipeDLFramework` × 8** (rule 11: ilastik→JIPipeDL).
- **opencv → `OpenCVFramework` × 6** (rule 11: opencv→OpenCV).
- **scene-3d → NoFramework (OMITTED) × 7** — scene-3d is **not** on rule 11's named list → "else leave
  NoFramework". The `framework()` default is already `NoFramework.class`
  (`jipipe-core/…/api/ConfigureJIPipeNode.java:97`), so the attribute is **omitted** (no `framework = `
  token, no `NoFramework` import — `grep "framework = " plugins/jipipe-plugin-scene-3d/src/main` → 0).

**`ij.*` imports drive SPATIAL, NOT framework** (the §19.9/§19.10 distinction, confirmed here): 8
ij-weka + 6 ilastik + 1 scene-3d files incidentally `import ij.*` (`ij.ImagePlus`, `ij.ImageStack`,
`ij.gui.Roi`, `ij.process.*`) for ImagePlus-level processing. None is read as `ImageJ1Framework` (the
framework is the *plugin's*, rule 11 plugin-scoped). Verified `grep -rl "ImageJ1Framework\|NoFramework"
src/main` across all four modules → **0**. opencv node files have **0** `ij.*` imports (its
IJ-bridge is in the `datatypes/` converters, not the nodes).

**Dynamic-NodeInfo check (§19.10 lesson, applied defensively):** `grep -rn "implements
JIPipeNodeInfo\|extends JIPipeDefaultNodeInfo"` across all four `src/main` trees → **NONE**. Every node
is annotation-registered by the module's own plugin (`WekaPlugin`/`IlastikPlugin`/`OpenCvPlugin`/
`Scene3DPlugin.registerNodeType`); no programmatic `JIPipeNodeInfo` implementations exist, so no
accessor overrides were needed (the 19.10 pattern did not recur). **Note (i):** the ilastik module has
2 `@ConfigureJIPipeNode` classes whose `registerNodeType` call is **commented out** in
`IlastikPlugin.register()` — see (f).

---

#### (a) jipipe-plugin-ij-weka (9) — all `JIPipeDLFramework`

Function = the `deeplearning/` branch by the node's **actual op** (rule 11 puts weka under JIPipeDL).
Spatial by first image input slot (rule 12; the controller's input mix is confirmed: 5×
`ImagePlus2DData`, 2× `ImagePlus2DGreyscaleMaskData`, 2× `ImagePlus3DData`, 1×
`ImagePlus2DGreyscaleData` [2nd slot, labels], 1× unconstrained `ImagePlusData`).

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `ImportWekaModelFromFileAlgorithm` (DataSource, —) | `DeepLearningModelsFunction` | absent | imports a `*.model`/`*.arff` **model file** ⇒ `WekaModelData` out; model *management* (not inference); first input `FileData` ⇒ no spatial |
| `WekaClassification2DAlgorithm` (Images / Weka) | `DeepLearningInferenceFunction` | **both** | *runs* a trained model on an image ⇒ `ImagePlusData` (unconstrained) first input; the 2D in the name is the op target, but the slot is unconstrained ⇒ both (rule 12) |
| `WekaClassification3DAlgorithm` (Images / Weka) | `DeepLearningInferenceFunction` | **3D** | runs a trained model on a 3D image ⇒ first input `ImagePlus3DData` ⇒ 3D |
| `WekaTrainingLabels2DAlgorithm` (Images / Weka) | `DeepLearningTrainingFunction` | **2D** | *trains* a Weka classifier on 2D image + labels ⇒ `ImagePlus2DData` first input; out `WekaModelData` |
| `WekaTrainingLabels2DAlgorithm2` (Images / Weka) | `DeepLearningTrainingFunction` | **2D** | multi-image variant of the above; first input `ImagePlus2DData` |
| `WekaTrainingMask2DAlgorithm` (Images / Weka) | `DeepLearningTrainingFunction` | **2D** | trains on 2D image + mask ⇒ `ImagePlus2DData` first input (2nd slot `ImagePlus2DGreyscaleMaskData`) |
| `WekaTrainingMask2DAlgorithm2` (Images / Weka) | `DeepLearningTrainingFunction` | **2D** | multi-image variant; first input `ImagePlus2DData` |
| `WekaTrainingRoi2dAlgorithm` (Images / Weka) | `DeepLearningTrainingFunction` | **2D** | trains on 2D image + ROIs ⇒ `ImagePlus2DData` first input (2nd slot `Roi2dListData`, non-image, skipped) |
| `WekaTrainingRoi3dAlgorithm` (Images / Weka) | `DeepLearningTrainingFunction` | **3D** | trains on 3D image + ROIs ⇒ `ImagePlus3DData` first input |

**Function split:** Inference **2** · Training **6** · Models **1** (total 9). **Spatial distribution:**
2D **5** · 3D **2** · both **1** · absent **1** (total 9). (The 5 `SPATIAL_2D` = the four 2D-training
nodes whose first input is `ImagePlus2DData` + `WekaTrainingRoi2dAlgorithm`; the 2 `SPATIAL_3D` =
`WekaTrainingRoi3dAlgorithm` + `WekaClassification3DAlgorithm`, first input `ImagePlus3DData`; the 1
`both` = `WekaClassification2DAlgorithm`, unconstrained `ImagePlusData` first input; the 1 absent =
`ImportWekaModelFromFileAlgorithm`, `FileData` first input.)

#### (b) jipipe-plugin-ilastik (8) — all `JIPipeDLFramework`

Function: the "Ilastik" menu nodes predict segmentation/features with a trained iLastik model →
`DeepLearningInferenceFunction`; the DataSource outputting `IlastikModelData` → `DeepLearningModelsFunction`;
the other DataSource → rule 6 by output; the Export node → `ExportFileFormatsFunction`.

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `IlastikPixelClassificationAlgorithm` (Images / Ilastik) | `DeepLearningInferenceFunction` | both | *runs* the trained pixel-classification workflow ⇒ segmentation/probabilities out; first input `ImagePlusData` (unconstrained) |
| `IlastikAutoContextAlgorithm` (Images / Ilastik) | `DeepLearningInferenceFunction` | both | runs the autocontext (iterative) workflow; first input `ImagePlusData` (unconstrained) — see (f) re: commented registration |
| `IlastikObjectClassificationAlgorithm` (Images / Ilastik) | `DeepLearningInferenceFunction` | both | runs the object-classification workflow; first input `ImagePlusData` — see (f) |
| `IlastikPredictionMapsObjectClassificationAlgorithm` (Images / Ilastik) | `DeepLearningInferenceFunction` | both | object classification from prediction maps; first input `ImagePlusData` — see (f) |
| `IlastikSegmentationImageObjectClassificationAlgorithm` (Images / Ilastik) | `DeepLearningInferenceFunction` | both | object classification from a segmentation image; first input `ImagePlusData` — see (f) |
| `ImportIlastikModel` (DataSource, —) | `DeepLearningModelsFunction` | absent | imports an `*.ilp` **project/model file** ⇒ `IlastikModelData` out; model *management*; first input `FileData` ⇒ no spatial |
| `ImportIlastikHDF5ImageAlgorithm` (DataSource, —) | `ImageDataFunction` | absent | rule 6 by **output**: emits `ImagePlusGreyscaleData` (an image) ⇒ the data/ **image** leaf; first input `FileData` ⇒ no spatial (it *reads* an HDF5 image, does not process an image input) |
| `ExportIlastikHDF5ImageAlgorithm` (Export / Images) | `ExportFileFormatsFunction` | both | rule 7: writes an HDF5 **file format** (not a ZIP/`ROI` archive) ⇒ the file-format leaf; first image input `ImagePlusGreyscaleData` (unconstrained) ⇒ both. Carries the module's **1 alias** (see (c)) |

**Function split:** Inference **5** · Models **1** · ImageData **1** · Export **1** (total 8).
**Spatial distribution:** both **6** · absent **2** (total 8).

#### (c) The ilastik alias (1) — DROPPED

`ExportIlastikHDF5ImageAlgorithm` carries the module's only `@AddJIPipeNodeAlias`:
`nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave"`.
Mapping the alias's old category+menuPath to a leaf by the same rules: an `ImageJNodeTypeCategory`
`File\nSave` node is a **file-format image exporter** → `ExportFileFormatsFunction`. That is **== the
primary** (`ExportFileFormatsFunction`), so per the A/B/C protocol (alias-leaf == primary-leaf → drop)
the alias line is **deleted**; **no secondary `function[]` entry** was added. (This is the same outcome
as §19.3's 12/12 dropped and the §19.2 `Convert`-only aliases — the alias is a same-leaf re-listing, not
a distinct leaf.) Verified `grep "@AddJIPipeNodeAlias" plugins/jipipe-plugin-ilastik/src/main` → **0**
after conversion; the other three modules have **0** aliases (verified).

#### (d) jipipe-plugin-opencv (6) — all `OpenCVFramework`

The 4 image ops take `OpenCvImageData` (a 2D OpenCV `Mat` — `public class OpenCvImageData implements
JIPipeData`, no `@ImageTypeInfo`; a 2D image type) ⇒ **`SPATIAL_2D`** (rule 12). The 2 kernel
generators have **no input slots** ⇒ **absent**.

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `BilateralFilterAlgorithm` (Images / Blur) | `FilteringFunction` | **2D** | bilateral **filter** (Blur menu) ⇒ the filtering leaf; first input `OpenCvImageData` (2D Mat) |
| `ConvertTypeAlgorithm` (Images / Convert) | `ResamplingFunction` | **2D** | bit-depth/type conversion (ij-algorithms `Convert`→`ResamplingFunction` precedent) |
| `ConvertDepthAlgorithm` (Images / Convert) | `ResamplingFunction` | **2D** | depth conversion (same `Convert` group) |
| `InpaintingAlgorithm` (Images / Restore) | `PreprocessingFunction` | **2D** | restore/inpaint (content-aware fill) ⇒ the **preprocessing** leaf; first input `OpenCvImageData` |
| `GaborKernelGenerator` (DataSource, —) | `ImageDataFunction` | absent | rule 6 by output: emits `OpenCvImageData` (an **image** — the Gabor kernel *as* a 2D image) ⇒ data/ image leaf (judgment, (f)); **no input slot** ⇒ absent |
| `GaussianKernelGenerator` (DataSource, —) | `ImageDataFunction` | absent | same as Gabor — emits a Gaussian kernel as a 2D `OpenCvImageData`; no input ⇒ absent |

**Function split:** Resampling **2** · Filtering **1** · Preprocessing **1** · ImageData **2** (total 6).
**Spatial distribution:** 2D **4** · absent **2** (total 6).

#### (e) jipipe-plugin-scene-3d (7) — NoFramework (omitted)

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `CreateLineMeshAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | rule 6 by output: emits `Scene3DData` — **no Scene3D data leaf exists** in `data/` (= Image/Table/Roi/Filament/**Misc**) ⇒ the data/ **misc** leaf (no-fit → misc); no image input |
| `CreateSphereMeshAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | same — emits `Scene3DData` (a sphere mesh); no-fit → misc; no image input |
| `ExportScene3DToColladaAlgorithm` (Export / 3D scenes) | `ExportFileFormatsFunction` | absent | rule 7: writes a **Collada (DAE) file format** (single file, not a ZIP archive) ⇒ file-format leaf; input `Scene3DData` (non-image) ⇒ absent |
| `ExportScene3DToColladaAlgorithm2` (Export / 3D scenes) | `ExportFileFormatsFunction` | absent | same DAE exporter (v2, relative-to-project); `Scene3DData` in ⇒ absent |
| `GroupSceneAlgorithm` (Misc / 3D Scenes) | `MiscellaneousFunction` | absent | rule 9 last-resort: groups 3D-scene objects (no specific leaf fits `Scene3DData` manipulation); `Scene3DData` in (non-image) ⇒ absent |
| `MergeScenesAlgorithm` (Misc / 3D Scenes) | `MiscellaneousFunction` | absent | rule 9: merges `Scene3DData` scenes (no scene-merge leaf); non-image in ⇒ absent |
| `MaskTo3DMeshAlgorithm` (Images / Binary) | `MiscellaneousFunction` | **3D** | **judgment (f):** a 3D-scene-from-mask **generation** op (Marching cubes: `ImagePlus3DGreyscaleMaskData` → `Scene3DData`). Old menuPath `Binary` would rule 1 → a `images/` leaf, but the node's **output is a 3D scene, not an image**, and it is a *generation* op, not a binary image filter. Assigned `MiscellaneousFunction` (the generation/last-resort leaf) with spatial **3D** (first image input `ImagePlus3DGreyscaleMaskData` ⇒ 3D). |

**Function split:** MiscData **2** · Export **2** · Miscellaneous **3** (total 7).
**Spatial distribution:** 3D **1** · absent **6** (total 7).

---

#### (f) Flagged for Phase 3 audit

- **⚠️ WEKA classical-ML nodes under the `deeplearning/` branch.** All 9 weka nodes are *classical*
  machine learning (Trainable Weka Segmentation — a random-forest/ML classifier, not a neural net), yet
  rule 11 places weka under `JIPipeDLFramework` and the `deeplearning/` function branch is their
  consistent home (Inference/Training/Models). **Flag for Phase 3**: if the audit prefers a dedicated
  "classical ML" framework or branch, all 9 move together (mechanical). Recorded, not silently absorbed.
- **⚠️ opencv kernel generators → `ImageDataFunction`.** `GaborKernelGenerator`/`GaussianKernelGenerator`
  emit `OpenCvImageData`, which *is* an image type (a 2D `Mat`), so rule 6's "image→`ImageDataFunction`"
  (the leaned choice) is used. **Alternative:** `MiscDataFunction` (they are OpenCV-specific *kernel*
  producers, not general image data). Flagged; both are `OpenCVFramework`, no spatial (no input slots).
- **⚠️ scene-3d `MaskTo3DMeshAlgorithm` → `MiscellaneousFunction` (spatial 3D).** Old menuPath `Binary`
  would point to a `images/` leaf, but the node's output is a **3D scene** (not an image) and its op is
  mesh *generation* from a mask, so the last-resort `MiscellaneousFunction` is the honest fit. If the
  audit groups by the *input* (a 3D mask) or adds a "3D-scene generation" leaf, it moves. Flagged.
- **scene-3d 2 Misc (`GroupSceneAlgorithm`/`MergeScenesAlgorithm`) → `MiscellaneousFunction`.** No
  `Scene3DData`-specific leaf exists; grouping/merging scene objects is a genuine best-fit (rule 9) with
  no narrower alternative. Not flagged as debatable beyond the note above.
- **ilastik `ImportIlastikHDF5ImageAlgorithm` → `ImageDataFunction` (rule 6 by output).** Emits
  `ImagePlusGreyscaleData` (an image) ⇒ the data/ image leaf. The controller left this to "rule 6 by
  output (read it)" — confirmed it reads an HDF5 *image file* and emits image data, not model data, so
  `ImageDataFunction` (contrast with `ImportIlastikModel` → `DeepLearningModelsFunction`, which emits
  `IlastikModelData`).
- **⚠️ ilastik: 4 of the 8 nodes are NOT registered at runtime.** `IlastikPlugin.register()` (line
  312–313) has the `registerNodeType` calls for `IlastikAutoContextAlgorithm` and
  `IlastikObjectClassificationAlgorithm` **commented out**, and `IlastikPredictionMapsObjectClassificationAlgorithm`
  / `IlastikSegmentationImageObjectClassificationAlgorithm` are **never referenced** by any
  `registerNodeType` (verified repo-wide grep: 0 live references outside their own file). So only **4 of
  the 8** ilastik nodes actually register on the gate's classpath (`ilastik-pixel-classification`,
  `import-ilastik-model`, `import-ilastik-hdf5-image`, `export-ilastik-hdf5-image`). All 8 are still
  converted (the 4 unregistered ones for consistency + the module-wide `mvn compile`), but the gate only
  enumerates the 4 registered — the 4 unregistered classes cannot surface as unassigned (they have no
  registered node info). **Not a conversion gap**; recorded so the 4-vs-8 gate count is not misread.
  (This is the inverse of §19.10's 18-annotated-vs-32-registered: here annotated > registered.)

---

#### (g) Test infrastructure + gate results

All four modules had **no `src/test` and no JUnit deps** (first test in each). For EACH
(`ijweka`/`ilastik`/`opencv`/`scene3d`):
- **`pom.xml`:** added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope), mirroring `plugins/jipipe-plugin-ij/pom.xml` verbatim (inserted before `</dependencies>`).
- **Copied** `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/
  TaxonomyConversionReportTest.java` → `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/api/
  nodes/taxonomy/` (**byte-identical**, verified with `cmp` — all 4).
- **Created** `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/plugins/<pkg>/
  ImageJLegacyPatcherLauncherSessionListener.java` (`LegacyInjector.preinit()` in
  `launcherSessionOpened`, HKI header, Javadoc adapted to the module's ImageJ1 touchpoints — weka:
  `ij.ImagePlus`/`ij.ImageStack`/`ij.gui.Roi`/`ij.process.*`; ilastik: `ij.ImagePlus` + `utils.hdf5.
  IJ1Hdf5`; opencv: `ij.ImagePlus` via the `datatypes/` IJ bridge; scene-3d: `MaskTo3DMeshAlgorithm`'s
  `ij.ImagePlus`) + `src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = that module's listener FQCN).

**Why the listener is required:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent
failure (AGENTS.md patcher contract). `net.imagej.patcher.LegacyInjector` is on the test classpath
transitively via jipipe-core → `net.imagej:imagej`.

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-ij-weka,plugins/jipipe-plugin-ilastik,
  plugins/jipipe-plugin-opencv,plugins/jipipe-plugin-scene-3d -am` → **exit 0** (all four in one reactor;
  the `JIPipeDLFramework`/`OpenCVFramework` + the `deeplearning/`/`images/`/`data/`/`export/`/
  `MiscellaneousFunction` leaves all resolve against the installed core).

- **Stale `~/.m2` core jar (the *core* gotcha was NOT hit).** The installed `jipipe-core` SNAPSHOT jar
  is dated **2026-10-10 03:52**, newer than the last jipipe-core taxonomy commit (`aeab2dcacb`,
  2026-10-09 20:19) — **fresh** (contains the taxonomy function classes and the converted core nodes), so
  no pre-19.1 core node surfaced as unassigned in any of the four prefixes (the §19.6 stale-core
  condition did not recur). The gate prefix filters (`…ijweka`/`…ilastik`/`…opencv`/`…scene3d`) cover no
  core package, so even a stale core jar could not inflate the prefix-filtered failure set. Stale
  *dependency* jars only inflate the informational global "N nodes not yet assigned" line (see below) —
  **no `mvn install -pl jipipe-core` was run and no jipipe-core source was touched.**

- **Gate (enabled), all four → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`,
  zero unassigned in the module prefix:**
  - **ij-weka:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ijweka`
  - **ilastik:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ilastik`
  - **opencv:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.opencv`
  - **scene-3d:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.scene3d`

  The log line `JIPipeNodesServiceComponent | Node taxonomy: N nodes not yet assigned a taxonomy
  function — expected during conversion` is the **global** count across all on-classpath modules (the
  still-unconverted clij / ij2 / ocr / plots / multi-template-matching / python / r / imp … modules +
  dependency jars); **none** are under any of the four prefixes, so the prefix-filtered assertion saw
  **zero** and passed (same shape as §19.3–§19.10). Observed global counts: ij-weka **545** · ilastik
  **545** · opencv **116** · scene-3d **116** (the opencv/scene-3d classpaths are smaller — no
  `jipipe-plugin-ij-algorithms` transitive dep — so their global unconverted count is lower; neither
  count is in the prefix-filtered set).

- **Gate (unset), all four → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the
  gate **skips cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works),
  module green.

**Byte-identity check (reviewer contract):** all 30 node files are byte-identical to base after
stripping `import …;` lines and the `@ConfigureJIPipeNode(…)` line (and, for the 1 alias file, the
`@AddJIPipeNodeAlias(…)` line) — verified by a line-filter diff across `git show HEAD:<f>` vs head
(30 files, 0 non-annotation changes). The only permitted change is the annotation + import fixup.

**Transition note understood:** the 30 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node
panel's category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking,
unmerged branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.

### 19.11c — jipipe-plugin-python (9) + jipipe-plugin-r (5) + jipipe-plugin-ij-multi-template-matching (1) + jipipe-plugin-ij-ocr (1) (16 nodes)

**Status:** converted — all 16 `@ConfigureJIPipeNode` nodes across the four modules rewritten to
`function` (plus `spatialDimensions` on the **2** image-input nodes only); four module gates green
(see (g)). Single-shot combined conversion (mapping + apply + 4 gates in one commit), mirroring
§19.6–§19.11a.

**These are the two remaining NON-ImageJ-function-only + two image-leaf modules of phase 1b.** Three of
the four (python, r) are pure script-execution / script-data modules → **function-only, NoFramework,
spatial absent** (the §19.6/§19.7 shape). The two image modules (ij-multi-template-matching, ij-ocr) are
**also NoFramework** (rule 11: neither is on the named list and neither is the `ij`/`ij-3d`/`ij-algorithms`
plugin — their single `ij.*` imports are incidental ImagePlus usage, §19.9/§19.10/§19.11a distinction) but
**additionally declare `spatialDimensions`** (they have an image *input* slot).

**Scope / gate prefixes (verified, matches controller R1):**
- `plugins/jipipe-plugin-python/src/main` — base pkg `org.hkijena.jipipe.plugins.python` (GATE PREFIX) — **9** annotated files.
- `plugins/jipipe-plugin-r/src/main` — base pkg `org.hkijena.jipipe.plugins.r` (GATE PREFIX) — **5** annotated files.
- `plugins/jipipe-plugin-ij-multi-template-matching/src/main` — base pkg `org.hkijena.jipipe.plugins.ijmultitemplatematching` (GATE PREFIX) — **1** annotated file.
- `plugins/jipipe-plugin-ij-ocr/src/main` — base pkg `org.hkijena.jipipe.plugins.ijocr` (GATE PREFIX) — **1** annotated file.

**Primary-category inventory (verified by grep of `nodeTypeCategory =`):**

| module | Miscellaneous | DataSource | Images | total |
|---|---|---|---|---|
| python | 7 ("Python script") | 2 | — | **9** |
| r | 2 ("R script") | 3 | — | **5** |
| ij-multi-template-matching | — | — | 1 ("Analyze") | **1** |
| ij-ocr | — | — | 1 ("OCR") | **1** |

**`ij.*` imports (verified):** python **0** files · r **0** files · ij-multi-template-matching **1** file
(`MultiTemplateMatchingAlgorithm` — `ij.ImagePlus`/`ij.gui.Roi`/`ij.process.*`) · ij-ocr **1** file
(`TesseractOCRAlgorithm` — `ij.IJ`/`ij.ImagePlus`). The `ij.*` imports are **incidental ImagePlus
processing, NOT a framework signal** (rule 11 is plugin-scoped; these modules are not `ij`/`ij-3d`/
`ij-algorithms`).

---

#### Framework — OMITTED for ALL 16 (rule 11, deliberate, not an omission)

None of these four modules is on rule 11's named list (cellpose/trackmate/clij/opencv/plots/weka/
ilastik/omnipose/ij2-discovered/ij/ij-3d/ij-algorithms), and none is a "core plugin". Rule 11's final
clause — "else leave `NoFramework`" — therefore applies to every node. The `@ConfigureJIPipeNode.
framework()` default is **already** `NoFramework.class` (`jipipe-core/…/api/ConfigureJIPipeNode.java:97`),
so the attribute is **omitted entirely** — no node writes `framework = NoFramework.class`, and there is
**no `NoFramework` import** anywhere. `grep -rl "framework = " src/main` across all four modules → **0**;
`grep -rl "NoFramework\|ImageJ1Framework" src/main` across all four → **0**. (This is the mirror of
§19.6/§19.7/§19.11a-scene-3d: function-only, NoFramework-omitted — the ijmtm/ijocr nodes get the *extra*
`spatialDimensions` attribute only.)

---

#### (a) jipipe-plugin-python (9) — function-only, NoFramework-omitted, spatial ABSENT

Rule 9 (Miscellaneous best-fit — the "Python script" menu nodes) + rule 6 (DataSource → data leaf by
output). **Spatial: absent on all 9** — no node has an image *input* slot (the generic `JIPipeData`
"Annotations" input and `FileData` input confer no spatial per the §19.5 graph-slot precedent;
controller-verified 0 image inputs).

**The 7 "Python script" Misc nodes → `MiscellaneousFunction`** (general script execution — there is no
"script" leaf; a *graph-wrapper* reading was considered and rejected per node: `GraphWrappersFunction`
is for nodes that wrap a *sub-pipeline/graph* (§19.1 `JIPipeProjectCompartment`, §19.6
`RunJIPipeProject`). These 7 **run an arbitrary Python/Jython snippet over data slots** (their
`JIPipe.*` API is data-in/data-out, not graph execution) — the `JIPipeScriptAlgorithm` family — so the
last-resort `MiscellaneousFunction` is the honest fit, exactly as the controller ruled):

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `RunPythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | runs a Python script over the data slots (dedicated interpreter; `JIPipeParameterSlotAlgorithm`) — general script execution, no graph wrapper |
| `RunIteratingPythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | per-iteration-step script; data slots in/out (`JIPipeIteratingAlgorithm`) |
| `RunMergingPythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | merging-iteration script; data slots in/out (`JIPipeMergingAlgorithm`) |
| `RunJythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | Jython (embedded interpreter) variant of the custom runner; data slots in/out |
| `RunIteratingJythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | Jython per-iteration-step variant |
| `RunMergingJythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | Jython merging variant |
| `RunSimpleIteratingJythonScriptAlgorithm` (Misc / "Python script") | `MiscellaneousFunction` | absent | Jython single-input-slice variant |

**The 2 DataSource nodes → `MiscDataFunction`** (rule 6 by **output**: both emit `PythonScriptData`,
which is a `StringData` subclass — **no python-script data leaf exists** in `data/` (= Image/Table/Roi/
Filament/**Misc**) ⇒ the data/ **misc** leaf, mirroring §19.3's `DefineImageJMacroAlgorithm`/
`ImportImageJMacroAlgorithm` → `MiscDataFunction` for `StringData`-derived types):

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `DefinePythonScriptAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | rule 6 by output: emits `PythonScriptData` (a `StringData` subclass); no-fit → misc; generic `JIPipeData` "Annotations" input (non-image) |
| `ImportPythonScriptAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | rule 6 by output: emits `PythonScriptData` (script text from a `FileData` input); no-fit → misc |

**Function split:** `MiscellaneousFunction` **7** · `MiscDataFunction` **2** (total 9). **Spatial:**
absent **9** (total 9). **Aliases:** 0 (verified `grep "@AddJIPipeNodeAlias" src/main` → 0).

#### (b) jipipe-plugin-r (5) — function-only, NoFramework-omitted, spatial ABSENT

Rule 9 (the 2 "R script" Misc nodes) + rule 6 (the 3 DataSource nodes, by **output**). **Spatial:
absent on all 5** — no image *input* slot (controller-verified 0 image inputs; the R runners' *outputs*
include an `ImagePlusColorRGBData` slot but rule 12 keys spatial off the first image **input**).

**The 2 "R script" Misc nodes → `MiscellaneousFunction`** (script execution; the graph-wrapper reading
was considered and rejected per node — these **run an R script over data slots** with the `JIPipe.*`
data-in/data-out API, not a sub-pipeline wrapper):

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `RunIteratingRScriptAlgorithm` (Misc / "R script") | `MiscellaneousFunction` | absent | runs an R script per iteration step over the data slots (in: `ResultsTableData`; out: `ImagePlusColorRGBData` + `ResultsTableData`) — general script execution, not a graph wrapper |
| `RunMergingRScriptAlgorithm` (Misc / "R script") | `MiscellaneousFunction` | absent | runs an R script over merging iteration steps; same slots — general script execution |

**The 3 DataSource nodes → rule 6 by OUTPUT:** the 2 that emit `RScriptData` (a `StringData` subclass —
**no R-script data leaf** in `data/`) → `MiscDataFunction`; the 1 that emits `ResultsTableData` →
`TableDataFunction`:

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `DefineRScriptAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | rule 6 by output: emits `RScriptData` (a `StringData` subclass); no-fit → misc (mirrors §19.3 `DefineImageJMacroAlgorithm`) |
| `ImportRScriptAlgorithm` (DataSource, —) | `MiscDataFunction` | absent | rule 6 by output: emits `RScriptData` (script text from a `FileData` input); no-fit → misc |
| `ImportRDatasetAlgorithm` (DataSource, —) | `TableDataFunction` | absent | rule 6 by output: "Imports a standard R data set (datasets package) **as table**" ⇒ emits **`ResultsTableData`** ⇒ the data/ **table** leaf (contrast the 2 `RScriptData` nodes) |

**Function split:** `MiscellaneousFunction` **2** · `MiscDataFunction` **2** · `TableDataFunction`
**1** (total 5). **Spatial:** absent **5** (total 5). **Aliases:** 0 (verified).

#### (c) jipipe-plugin-ij-multi-template-matching (1) — NoFramework-omitted, spatial **BOTH**, JUDGED leaf

`MultiTemplateMatchingAlgorithm` (old: `ImagesNodeTypeCategory`, menuPath `Analyze`; the 1 alias in the
batch — see (d)).

- **Framework: OMITTED (NoFramework).** Not on rule 11's list and not the `ij`/`ij-3d`/`ij-algorithms`
  plugin (rule 11) → else NoFramework; `ij.*` imports (`ij.ImagePlus`/`ij.gui.Roi`/`ij.process.*`) are
  incidental ImagePlus usage, driving **SPATIAL only** (rule 12), not framework.
- **Function: `MeasurementsFunction`** (JUDGED, see rationale) — a template-matching *object detector*
  that locates template matches in a grayscale image and reports them (ROI list + a "Measurements"
  results table + an assembled-templates overlay).
- **Spatial: `spatialDimensions = { SPATIAL_2D, SPATIAL_3D }` (BOTH).** Rule 12 reads the **first image
  input slot**: `@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image")` is slot #1 (the
  `ImagePlus2DData` "Template" is slot #2). The first input is **unconstrained `ImagePlusData`** ⇒
  BOTH. (Confirmed: `ImagePlusData` is slot #1, not `ImagePlus2DData`.)

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `MultiTemplateMatchingAlgorithm` (Images / "Analyze") | `MeasurementsFunction` | **both** | **JUDGED (flag (e)):** template matching **locates** template instances in a target image and **measures** each match (outputs an `Roi2dListData` "ROI" list, a `ResultsTableData` "Measurements" table — *literally named "Measurements"* — and an "Assembled templates" image). `MeasurementsFunction` is chosen over `SegmentationFunction` because (1) the node's own **old menuPath is `Analyze`**, and the §19.2 ij-algorithms precedent maps the entire `Images/Analyze` group (5 nodes) → `MeasurementsFunction` (whereas `Images/Detect` → `SegmentationFunction`); (2) it reports *located, scored matches as measurements* (a results table) rather than partitioning the whole image into a label/mask (the "Segmentation" leaf's stated description, "Partitioning images into meaningful objects"). The `SegmentationFunction` (object-detection) reading is the flagged alternative. |

**Function split:** `MeasurementsFunction` **1** (total 1). **Spatial:** both **1** (total 1).

#### (d) The 1 alias (ij-multi-template-matching) — DROPPED

`MultiTemplateMatchingAlgorithm` carries the batch's only `@AddJIPipeNodeAlias`:
`nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMulti-Template-Matching"` —
the IJ "Plugins" menu re-listing of the same node. Mapped by the A/B/C protocol (alias-leaf ==
primary-leaf → drop, else secondary `function[]`): the alias is an **ImageJ-category** node, but under
rule 11/§19.3 the *framework* (which for an IJ plugin node would be `ImageJ1Framework`) is a separate
attribute from the *function* leaf, and this node's **function** leaf is determined by its op (template
matching ⇒ `MeasurementsFunction`). The IJ `Plugins\nMulti-Template-Matching` alias does **not** point
to a *distinct function leaf* — it is the same node, same op, re-listed in the IJ Plugins menu. Its
function leaf is therefore **== the primary** (`MeasurementsFunction`) ⇒ **dropped** (no secondary
`function[]` entry). This is the same outcome as the §19.3 IJ-category aliases (12/12 dropped —
`ImageJ`/`Plugins\n…` re-listings map to the node's own primary leaf). Verified `grep
"@AddJIPipeNodeAlias" plugins/jipipe-plugin-ij-multi-template-matching/src/main` → **0** after
conversion. The other three modules: **0** aliases (verified). (Both the `ImageJNodeTypeCategory` and
`ImagesNodeTypeCategory` imports became unused and were removed with the node/alias annotations.)

**Flag (e) re the alias:** the alias is *framework*-flavored (ImageJ `Plugins` menu) rather than
*function*-flavored, so one could argue it should have contributed a secondary **framework** (IJ1)
rather than a secondary **function**. But rule 11 fixes the framework to **NoFramework** for this
module (the controller ruled it is *not* an ImageJ plugin), so there is no IJ1 framework to add anyway
— the alias is a pure function-leaf re-listing and is dropped. Recorded, not silently absorbed.

#### (e) jipipe-plugin-ij-ocr (1) — NoFramework-omitted, spatial **BOTH**, JUDGED leaf

`TesseractOCRAlgorithm` (old: `ImagesNodeTypeCategory`, menuPath `OCR`).

- **Framework: OMITTED (NoFramework).** Not on rule 11's list and not the `ij`/`ij-3d`/`ij-algorithms`
  plugin → else NoFramework; `ij.*` imports (`ij.IJ`/`ij.ImagePlus`) are incidental, driving
  **SPATIAL only**, not framework.
- **Function: `MiscellaneousFunction`** (JUDGED, see rationale) — OCR: image → text.
- **Spatial: `spatialDimensions = { SPATIAL_2D, SPATIAL_3D }` (BOTH).** Rule 12 first image input slot:
  `@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input")`. `ImagePlusGreyscaleData`
  **extends unconstrained `ImagePlusData`** (no `numDimensions` in its `@ImageTypeInfo`) ⇒ BOTH.

| node (old cat / menuPath) | function | spatial | rationale |
|---|---|---|---|
| `TesseractOCRAlgorithm` (Images / "OCR") | `MiscellaneousFunction` | **both** | **JUDGED (flag (f)):** OCR reads text out of an image (in: `ImagePlusGreyscaleData`; out: a `ResultsTableData` "Results" of the Tesseract TSV exporter). `MiscellaneousFunction` is chosen over `MeasurementsFunction` because OCR is **not** a measurement op — it does not measure geometric/intensity *properties* of objects (the `MeasurementsFunction` description, "Measuring properties of images and objects"); it **recognizes/extracts text**, which has **no clean taxonomy home** (no "OCR"/"text" leaf exists in `images/` or elsewhere). The `MeasurementsFunction` reading (it emits a "measurements" table) is the flagged alternative, but it would mislabel text extraction as image/object measurement. The honest last-resort leaf is `MiscellaneousFunction`. |

**Function split:** `MiscellaneousFunction` **1** (total 1). **Spatial:** both **1** (total 1).

---

#### (f) Flagged for Phase 3 audit (the 2 judged image nodes — main flags)

- **⚠️ `MultiTemplateMatchingAlgorithm` → `MeasurementsFunction` (spatial BOTH).** Template *matching*
  is an object-**detection** op, so `SegmentationFunction` ("detect objects"/partitioning) is the
  defensible alternative. Chose `MeasurementsFunction` on (1) the node's own old menuPath `Analyze`
  (the §19.2 `Images/Analyze`→`MeasurementsFunction` precedent, vs `Images/Detect`→`SegmentationFunction`)
  and (2) its output being a *scored-match measurements table* + ROI list (located/measured instances),
  not a whole-image label/mask. **Both defensible; the reviewer should check the `Analyze`-vs-`Detect`
  menuPath reading.** If the audit groups template matching as detection/segmentation, it moves to
  `SegmentationFunction` (the spatial + NoFramework are unchanged).
- **⚠️ `TesseractOCRAlgorithm` → `MiscellaneousFunction` (spatial BOTH).** OCR (image→text) has no
  dedicated leaf. `MeasurementsFunction` is the alternative (it emits a results/"measurements" table),
  but text *recognition* is not image/object *measurement*, so `MiscellaneousFunction` (the honest
  last-resort) was chosen. **Both defensible; the reviewer should check whether OCR should be read as
  a table-producing "measurement" op.** If so, it moves to `MeasurementsFunction` (spatial +
  NoFramework unchanged).
- **The 1 ijmtm alias dropped (framework-flavored, not function-flavored).** See (d) — the
  `ImageJ`/`Plugins\nMulti-Template-Matching` alias is the same op re-listed in the IJ menu; because
  rule 11 fixes this module's framework to NoFramework, the alias has no distinct *function* leaf and
  is dropped. Recorded so the "1 alias = 1 drop" count is not misread as a lost IJ1-framework signal.

---

#### (g) Test infrastructure + gate results

All four modules had **no `src/test` and no JUnit deps** (first test in each). For EACH
(`python`/`r`/`ijmultitemplatematching`/`ijocr`):
- **`pom.xml`:** added `org.junit.jupiter:junit-jupiter` + `org.junit.platform:junit-platform-launcher`
  (test scope), mirroring `plugins/jipipe-plugin-ij/pom.xml` verbatim (inserted before `</dependencies>`).
- **Copied** `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/
  TaxonomyConversionReportTest.java` → `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/api/
  nodes/taxonomy/` (**byte-identical**, verified with `cmp` — all 4).
- **Created** `plugins/jipipe-plugin-<m>/src/test/java/org/hkijena/jipipe/plugins/<pkg>/
  ImageJLegacyPatcherLauncherSessionListener.java` (`LegacyInjector.preinit()` in
  `launcherSessionOpened`, HKI header, Javadoc adapted to the module — ijmtm: `MultiTemplateMatchingAlgorithm`
  loads `ij.ImagePlus`/`ij.process.*`; ijocr: `TesseractOCRAlgorithm` loads `ij.IJ`/`ij.ImagePlus`;
  python/r: no `ij.*` in nodes, but the gate's `JIPipe.ensureInstance()` still creates the SciJava/
  ImageJ context, so the patcher must run first) + `src/test/resources/META-INF/services/
  org.junit.platform.launcher.LauncherSessionListener` (single line = that module's listener FQCN).

**Why the listener is required:** the gate calls `JIPipe.ensureInstance()` → ImageJ SciJava context. A
module's test classpath does not include jipipe-core's test classes, so core's
`ImageJLegacyPatcherLauncherSessionListener` is not on the classpath; without re-registering the patcher,
any `ij.*` class loaded before the context is created makes the ImageJ2 legacy patcher
(`net.imagej.patcher.LegacyInjector`) fail (`No _hooks field found in ij.IJ`), an order-dependent
failure (AGENTS.md patcher contract). `net.imagej.patcher.LegacyInjector` is on the test classpath
transitively via jipipe-core → `net.imagej:imagej`.

- **Compile:** `mvn -q -B compile -pl plugins/jipipe-plugin-python,plugins/jipipe-plugin-r,
  plugins/jipipe-plugin-ij-multi-template-matching,plugins/jipipe-plugin-ij-ocr -am` → **exit 0** (all
  four in one reactor; the `MiscellaneousFunction`/`MiscDataFunction`/`TableDataFunction`/
  `MeasurementsFunction` leaves + the `SPATIAL_*` statics all resolve against the installed core).

- **Stale `~/.m2` core jar (the *core* gotcha was NOT hit).** The installed `jipipe-core`
  6.0.0-SNAPSHOT jar is dated **2026-10-10 03:52** (verified), newer than the last jipipe-core taxonomy
  commit — **fresh** (contains the taxonomy function classes + converted core nodes). No CORE
  (`org.hkijena.jipipe.*`) node surfaced as unassigned in any of the four prefixes (the §19.6
  stale-core condition did not recur). The gate prefixes (`…python`/`…r`/`…ijmultitemplatematching`/
  `…ijocr`) cover no core package, so even a stale core jar could not inflate the prefix-filtered
  failure set. Stale *dependency* jars only inflate the informational global "N nodes not yet assigned"
  line (see below) — **no `mvn install -pl jipipe-core` was run and no jipipe-core source was touched.**

- **Gate (enabled), all four → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`,
  zero unassigned in the module prefix:**
  - **python:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.python` → log
    `INFORMATION: JIPipeNodesServiceComponent | Node taxonomy validation passed` (the python classpath
    carries no other unconverted module, so there is no global "N nodes" line at all — 0 unassigned).
  - **r:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.r` → log `Node taxonomy: 116 nodes
    not yet assigned a taxonomy function — expected during conversion` (global; none under the prefix).
  - **ij-multi-template-matching:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ijmultitemplatematching` → log `Node taxonomy: 116 nodes not yet assigned…` (global).
  - **ij-ocr:** `…-Djipipe.taxonomy.report.module=org.hkijena.jipipe.plugins.ijocr` → log `Node taxonomy: 116 nodes not yet assigned…` (global).

  The `116` global count (r/ijmtm/ijocr classpaths; python's clean classpath shows none) is the
  **global** count across all on-classpath modules (the still-unconverted clij / ij2 / plots / imp …
  modules + dependency jars); **none** are under any of the four prefixes, so the prefix-filtered
  assertion saw **zero** and passed (same shape as §19.3–§19.11a).

- **Gate (unset), all four → BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`** — the
  gate **skips cleanly** (property unset), no `_hooks`/`LinkageError` patcher failure (listener works),
  module green.

**Byte-identity check (reviewer contract):** all 16 node files are byte-identical to base after
stripping `import …;` lines and the `@ConfigureJIPipeNode(…)` / `@AddJIPipeNodeAlias(…)` lines
(verified by a line-filter diff across `git show HEAD:<f>` vs head) — the change is annotations-only.
The only permitted change is the annotation + import fixup (plus, for the 2 image nodes, the 2
`SPATIAL_*` static imports, and, for the 1 alias file, the removal of the now-unused
`ImageJNodeTypeCategory` import).

**Transition note understood:** the 16 converted nodes now fall back to `InternalNodeTypeCategory` via
`getCategoryOf(nodeClass)` (`isVisibleInPipeline` false), so they are absent from the *old* add-node
panel's category buttons until Task 24 swaps in the new taxonomy browser. Intended (6.0.0 breaking,
unmerged branch). No `menuPath`/`nodeTypeCategory` was kept to "fix" the old panel.
