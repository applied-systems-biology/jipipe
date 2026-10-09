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
| jipipe-core | 31 | pending |
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

- (none yet)
