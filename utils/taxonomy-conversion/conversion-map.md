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
| jipipe-plugin-filaments + ij-filaments | 51+ | pending |
| jipipe-plugin-filesystem | 39 | pending |
| jipipe-plugin-annotations | 29 | pending |
| jipipe-plugin-utils | 28 | pending |
| jipipe-plugin-ij-omero | 26 | pending |
| jipipe-plugin-cellpose | 21 | pending |
| jipipe-plugin-ij-trackmate | 18 | pending |
| jipipe-plugin-strings | 15 | pending |
| jipipe-plugin-forms | 10 | pending |
| jipipe-plugin-ij-weka | 9 | pending |
| jipipe-plugin-python | 9 | pending |
| jipipe-plugin-ilastik | 8 | pending |
| jipipe-plugin-scene-3d | 7 | pending |
| jipipe-plugin-opencv | 6 | pending |
| jipipe-plugin-r | 5 | pending |
| jipipe-plugin-imp | 5 | pending |
| jipipe-plugin-omnipose | 5 | pending |
| jipipe-plugin-clij | 4 | pending |
| jipipe-plugin-ij2 | 2 | pending |
| jipipe-plugin-ij-multi-template-matching | 1 | pending |
| jipipe-plugin-ij-ocr | 1 | pending |
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
