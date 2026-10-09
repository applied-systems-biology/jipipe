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
| jipipe-plugin-ij-algorithms | 424 | pending |
| jipipe-plugin-ij | 80 | pending |
| jipipe-plugin-ij-3d | 62 | pending |
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
