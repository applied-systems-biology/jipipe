# Multi-component shipped DAG patterns — per-case review (item 3c) — RESOLVED

**Final outcome (rulings 2026-10-19/20):** 16 DROP + 32 KEEP-LARGEST, plus
the general script/tunnel policies enforced generator-wide (which also
removed 2 connected patterns — 0090, 0121 — that never appeared on this
list). Shipped DAG total: 422 → 406; System templates: 446 → 430.

- **DROP (9 mechanical)** — largest component < 3 nodes, not a real pipeline:
  `dag-mined-cluster-0003/0061/0062/0074/0099/0204`,
  `dag-derived-derived-mech-cluster-0003-3d-ddd732f1a382`,
  `dag-derived-derived-mech-cluster-0099-3d-6269bb9b90ae`,
  `dag-derived-derived-mech-cluster-0204-3d-9598f33c2700`.
- **DROP (7)** — all 7 REVIEW rows resolved by user ruling:
  - `dag-mined-cluster-0184` "Measure pairwise distances between 3D ROIs" —
    drop (cpython involvement; the scripting system is being redesigned).
  - `dag-mined-cluster-0250` + 3D derived twin "Smooth images with Gaussian
    blur" — drop (tunnel entrances/exits are not part of any pattern).
  - `dag-mined-cluster-0198/0201/0218/0140` — keep-largest per
    recommendation (accepted; recorded in dag-pattern-decisions.json).
- **KEEP-LARGEST (32)** — auto + the 4 above; disconnected strays dropped.
- **General policies encoded in the generator (fail-closed):** no JIPipe
  script-runner node (cpython-script*, python-script* [Jython], r-script*,
  define/import-*-script, tables-from-script) and no data-flow-tunnel node in
  any shipped pattern; any multi-component pattern without a decision entry
  fails the build. `external-imagej-macro` is OUT of scope (separate
  integration); the "(Script)" filter variants
  (annotate-split-by-annotation-script, ij1-roi-filter-statistics-script, …)
  are NOT script-runners and remain allowed. Decision file:
  `dag-pattern-decisions.json` (50 entries: 34 keep-largest, 16 drop).

---
The original per-case list (generated before the rulings):

Context: 1324 follow-up. After 3a (strip `graph-wrapper:input/output`) + 3b
(enable the `{Parameters}` external slot), 48 shipped DAG templates are still
Total: 48 multi-component = 32 KEEP + 7 REVIEW + 9 DROP.

**multi-component** (disconnected node groups) — a harvest (A)-step artifact
(the candidate region captured several unconnected groups; verified in the raw
`dag-candidates.json`, e.g. `dag-1518` = 6 nodes / 1 edge). Instantiated, the
non-largest components appear as floating, unconnected nodes.

Verdicts:
  * **KEEP** — keep the largest connected component, drop the rest.
  * **DROP** — the whole pattern is degenerate (largest < 3 nodes and not a real 2-step pipeline).
  * **REVIEW** — needs a human look (orphaned tunnel, or a dropped component that is
    itself a sizeable sub-pipeline that may be worth keeping as its own pattern).

## DROP — 'Filter 2D ROIs by overlap'
key: `dag-mined-cluster-0003`   ·   largest component is 2 copies of the same node — harvest artifact
components (largest first):
  [drop] (2): ij1-roi-filter-by-overlap, ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap

## DROP — 'Filter 3D ROIs by overlap'
key: `dag-derived-derived-mech-cluster-0003-3d-ddd732f1a382`   ·   largest component is 2 copies of the same node — harvest artifact
components (largest first):
  [drop] (2): ij3d-roi-filter-by-overlap, ij3d-roi-filter-by-overlap
  [drop] (1): ij3d-roi-filter-by-overlap
  [drop] (1): ij3d-roi-filter-by-overlap
  [drop] (1): ij3d-roi-filter-by-overlap
  [drop] (1): ij3d-roi-filter-by-overlap

## DROP — 'Filter data by label'
key: `dag-mined-cluster-0062`   ·   largest component is a single node — not a pattern
components (largest first):
  [drop] (1): annotate-filter-by-annotation-if-else
  [drop] (1): annotate-filter-by-annotation-if-else
  [drop] (1): annotate-filter-by-annotation-if-else

## DROP — 'Filter three inputs by annotation'
key: `dag-mined-cluster-0061`   ·   largest component is a single node — not a pattern
components (largest first):
  [drop] (1): annotate-split-by-annotation
  [drop] (1): annotate-split-by-annotation
  [drop] (1): annotate-split-by-annotation

## DROP — 'Plot tabular data with scatter, box, and line charts'
key: `dag-mined-cluster-0074`   ·   largest component is a single node — not a pattern
components (largest first):
  [drop] (1): plot-table:plot-xy-scatter
  [drop] (1): plot-table:plot-xy-line
  [drop] (1): plot-table:plot-box-and-whisker

## DROP — 'Render 2D ROIs as an RGB image'
key: `dag-mined-cluster-0099`   ·   largest component is a single node — not a pattern
components (largest first):
  [drop] (1): ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb

## DROP — 'Render 2D ROIs as an RGB image'
key: `dag-mined-cluster-0204`   ·   largest component is 2 copies of the same node — harvest artifact
components (largest first):
  [drop] (2): ij1-roi-to-rgb, ij1-roi-to-rgb
  [drop] (1): ij1-set-lut-from-color-map

## DROP — 'Render 3D ROIs as an RGB image'
key: `dag-derived-derived-mech-cluster-0099-3d-6269bb9b90ae`   ·   largest component is a single node — not a pattern
components (largest first):
  [drop] (1): ij3d-roi-convert-to-rgb
  [drop] (1): ij3d-roi-convert-to-rgb
  [drop] (1): ij3d-roi-convert-to-rgb

## DROP — 'Render 3D ROIs as an RGB image'
key: `dag-derived-derived-mech-cluster-0204-3d-9598f33c2700`   ·   largest component is 2 copies of the same node — harvest artifact
components (largest first):
  [drop] (2): ij3d-roi-convert-to-rgb, ij3d-roi-convert-to-rgb
  [drop] (1): ij1-set-lut-from-color-map

## KEEP — '2D ROI overlap counts and overlays'
key: `dag-mined-cluster-0040`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (8): ij1-roi-count, ij1-roi-filter-by-overlap, ij1-roi-filter-by-overlap, ij1-roi-to-rgb, ij1-roi-to-rgb, ij1-roi-to-rgb, ij1-roi-to-rgb, table-apply-expression-per-row-v2
  [drop] (1): ij1-roi-to-rgb

## KEEP — 'Attach 2D ROI measurements to data'
key: `dag-derived-derived-mech-cluster-0005-2d-4b8f24225885`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): annotate-data-with-table-values, ij1-roi-statistics
  [drop] (1): table-add-columns-generate

## KEEP — 'Attach 3D ROI measurements to data'
key: `dag-mined-cluster-0005`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): annotate-data-with-table-values, ij3d-measure-roi3d
  [drop] (1): table-add-columns-generate

## KEEP — 'Clean and enhance images'
key: `dag-mined-cluster-0129`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (4): ij1-background-rollingball2d, ij1-blur-median3d, ij1-contrast-histogram-enhancer, ij1-math-imagecalculator2d
  [drop] (1): ij1-noise-removeoutliers2d
  [drop] (1): ij1-background-rollingball2d

## KEEP — 'Create extended depth of focus projections from microscopy images'
key: `dag-mined-cluster-0091`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (5): annotate-set, clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection, ij1-set-lut-from-color-map, image-properties-to-annotation, import-imagej-bioformats
  [drop] (1): project-user-directory
  [drop] (1): ij1-dimensions-extended-depth-of-focus

## KEEP — 'Detect 2D particle ROIs from microscopy image stacks'
key: `dag-mined-cluster-0153`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (10): clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection, ij1-analyze-find-particles2d, ij1-blur-median2d, ij1-dimensions-expression-slicer, ij1-dimensions-reorder, ij1-dimensions-reorder, ij1-dimensions-stack-to-2d, ij1-threshold-auto2d, image-properties-to-annotation, import-imagej-bioformats
  [drop] (1): project-user-directory

## KEEP — 'Detect 2D particles and create 2D ROI overlays'
key: `dag-mined-cluster-0208`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (8): ij1-analyze-find-particles2d, ij1-background-rollingball2d, ij1-binary-dtwatershed2d, ij1-contrast-clahe, ij1-dimensions-inpput2montage, ij1-noise-removeoutliers2d, ij1-roi-to-rgb, ij1-threshold-auto2d
  [drop] (1): parameters-define-table
  [drop] (1): parameters-define-table

## KEEP — 'Detect 2D particles and extract 2D ROI statistics'
key: `dag-mined-cluster-0172`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (9): annotate-set, external-imagej-macro, ij1-analyze-find-particles2d, ij1-math-transform2d, ij1-morph-binary-fillholes2d, ij1-morph-operation2d, ij1-roi-merge, ij1-roi-statistics, ij1-roi-to-rgb
  [drop] (1): annotate-set

## KEEP — 'Detect and enhance line-like structures in 2D images'
key: `dag-mined-cluster-0036`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (8): annotate-split-by-annotation, ij1-blur-gaussian2d, ij1-contrast-histogram-enhancer, ij1-math-imagecalculator2d, ij1-segment-ridge-detector-2d, ij1-transform-crop2d, image-properties-to-annotation, import-imagej-imgplus-from-file
  [drop] (1): import-folder-list

## KEEP — 'Detect line-like structures in microscopy images'
key: `dag-mined-cluster-0179`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (6): annotate-filter-by-annotation, ij1-background-rollingball2d, ij1-color-split-channels-2, ij1-segment-ridge-detector-2d, image-properties-to-annotation, import-imagej-bioformats
  [drop] (1): import-folder-list

## KEEP — 'Enhance and blend images'
key: `dag-mined-cluster-0158`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): ij1-color-blend-images, ij1-contrast-histogram-enhancer
  [drop] (1): ij-convert-image-to-8-bit

## KEEP — 'Export 2D ROIs as a labeled image'
key: `dag-derived-derived-mech-cluster-0071-2d-b20edf1c1cf9`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (5): ij1-calibration-copy-physical-dimensions, ij1-generate-zero-image, ij1-roi-set-properties, ij1-roi-to-rgb, iji-export-image-v2
  [drop] (1): cpython-script-iterating

## KEEP — 'Export 3D ROIs as a labeled image'
key: `dag-mined-cluster-0071`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (5): ij1-calibration-copy-physical-dimensions, ij1-generate-zero-image, ij3d-roi-change-properties, ij3d-roi-convert-to-rgb, iji-export-image-v2
  [drop] (1): cpython-script-iterating

## KEEP — 'Export merged summary tables as XLSX'
key: `dag-mined-cluster-0026`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): iji-export-table-as-xlsx-v2, table-add-annotation-columns, table-merge-tables
  [drop] (1): table-merge-tables
  [drop] (1): iji-export-table-as-xlsx-v2

## KEEP — 'Filter and merge overlapping 2D ROIs'
key: `dag-mined-cluster-0194`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (4): ij1-roi-filter-by-overlap, ij1-roi-filter-by-overlap, ij1-roi-merge, ij1-roi-merge
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap
  [drop] (1): ij1-roi-filter-by-overlap

## KEEP — 'Filter and summarize 2D ROI lists'
key: `dag-mined-cluster-0078`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): annotate-set-single, ij1-roi-explode, ij1-roi-to-annotations
  [drop] (2): ij1-roi-outline, rename-annotation
  [drop] (1): ij1-roi-filter-by-overlap

## KEEP — 'Find boundary regions of selected 2D ROIs'
key: `dag-mined-cluster-0068`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): ij1-binary-connected-component-labeling-2d, ij1-labels-filter-by-overlap, ij1-labels-get-label-boundaries
  [drop] (1): ij1-roi-filter-statistics

## KEEP — 'Import and split microscopy images with color maps and metadata'
key: `dag-mined-cluster-0023`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): annotate-set, image-properties-to-annotation, import-imagej-bioformats
  [drop] (2): ij1-dimensions-expression-slicer, ij1-set-lut-from-color-map
  [drop] (2): ij1-dimensions-expression-slicer, ij1-set-lut-from-color-map
  [drop] (2): ij1-dimensions-expression-slicer, ij1-set-lut-from-color-map
  [drop] (1): project-user-directory

## KEEP — 'Import, split, and apply false colors to microscopy images'
key: `dag-mined-cluster-0007`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (8): annotate-filter-by-annotation, annotate-set, ij1-color-split-channels-2, ij1-dimensions-reorder, ij1-set-lut-from-color-map, ij1-set-lut-from-color-map, image-properties-to-annotation, import-imagej-bioformats
  [drop] (1): project-user-directory

## KEEP — 'Load and denoise images from folders'
key: `dag-derived-derived-mech-cluster-0035-3d-e780848c8feb`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (9): annotate-set, folder-list-files, folder-list-subfolders, ij1-blur-median3d, import-imagej-imgplus-from-file, path-filter, path-to-annotation-simple, path-to-annotation-simple, project-user-directory
  [drop] (1): import-folder-list

## KEEP — 'Load and denoise images from folders'
key: `dag-mined-cluster-0035`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (9): annotate-set, folder-list-files, folder-list-subfolders, ij1-blur-median2d, import-imagej-imgplus-from-file, path-filter, path-to-annotation-simple, path-to-annotation-simple, project-user-directory
  [drop] (1): import-folder-list

## KEEP — 'Load and split image stacks by slice position'
key: `dag-mined-cluster-0223`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): ij1-dimensions-stacksplitter, import-imagej-imgplus-from-file
  [drop] (1): import-file-list

## KEEP — 'Load microscopy images from folders and convert to 32-bit'
key: `dag-mined-cluster-0164`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (8): annotate-set, annotate-set, annotate-set, annotate-set, annotate-set, annotate-set, ij-convert-image-to-32-bit, import-imagej-bioformats
  [drop] (1): import-folder-list

## KEEP — 'Load three-channel images from folders and combine them into RGB'
key: `dag-mined-cluster-0187`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): ij1-color-combine-rgb, import-data-from-slot-folder
  [drop] (1): import-folder-list

## KEEP — 'Measure overlap between two mask images'
key: `dag-mined-cluster-0086`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (5): ij1-labels-overlap-statistics, iji-export-table, table-add-annotation-columns, table-add-annotation-columns, table-merge-tables
  [drop] (1): ij1-math-divide-by-maximum

## KEEP — 'Measure tracked objects and export histograms'
key: `dag-mined-cluster-0197`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (9): data-batch-slicer, data-batch-slicer, iji-export-image-v2, iji-export-image-v2, iji-export-image-v2, plot-table:plot-histogram, plot-table:plot-histogram, plot-table:plot-histogram, trackmate-measure-tracks
  [drop] (1): trackmate-measure-spots
  [drop] (1): trackmate-measure-edges

## KEEP — 'Prepare 32-bit contrast-adjusted microscopy images from folders'
key: `dag-mined-cluster-0125`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): annotate-set, ij-convert-image-to-32-bit, import-imagej-bioformats
  [drop] (2): ij1-contrast-histogram-enhancer, ij1-set-lut-from-color-map
  [drop] (1): import-folder-list
  [drop] (1): ij1-set-lut-from-color-map

## KEEP — 'Preprocess microscopy image stacks'
key: `dag-mined-cluster-0058`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (6): clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection, ij1-contrast-illumination-correction2d, ij1-dimensions-hyper-stacksplitter, ij1-dimensions-merge-2d-to-hyperstack, image-properties-to-annotation, import-imagej-bioformats
  [drop] (1): project-user-directory

## KEEP — 'RGB track visualization and branch statistics'
key: `dag-mined-cluster-0311`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (3): ij1-color-combine-rgb, ij1-color-merge-channels, trackmate-visualize-follow-spots
  [drop] (1): trackmate-measure-branches

## KEEP — 'Segment 2D objects and overlay 2D ROIs'
key: `dag-mined-cluster-0080`   ·   keep-largest (drop the disconnected strays)
components (largest first):
  [KEEP] (7): ij1-analyze-find-particles2d, ij1-blur-median2d, ij1-contrast-illumination-correction2d, ij1-roi-set-overlay, ij1-roi-to-rgb, ij1-threshold-auto2d, ij1-threshold-auto2d
  [drop] (1): graph-annotation-image

## KEEP — 'Split 3D filaments into component masks'
key: `dag-mined-cluster-0063`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): filaments-convert-to-mask, filaments-split-into-connected-components
  [drop] (1): filaments-convert-to-mask

## KEEP — 'Visualize tracked spots and trajectories'
key: `dag-mined-cluster-0056`   ·   keep-largest (a genuine 2-step pipeline)
components (largest first):
  [KEEP] (2): trackmate-visualize-spots, trackmate-visualize-tracks
  [drop] (1): trackmate-visualize-track-scheme

## REVIEW — 'Create 2D ROI segmentation image and measurements table'
key: `dag-mined-cluster-0198`   ·   keep-largest, but a DROPPED component is also >=3 nodes (secondary sub-pipeline?)
components (largest first):
  [KEEP] (7): annotate-set, annotate-split-by-annotation, annotate-split-by-annotation, ij1-roi-statistics, plot-table:plot-histogram, plot-table:plot-histogram, table-filter-2
  [drop] (3): ij1-morph-operation2d, ij1-roi-to-labels-expression, ij1-transform-add-border-2d

## REVIEW — 'Create RGB montage of 2D ROIs'
key: `dag-mined-cluster-0201`   ·   keep-largest, but a DROPPED component is also >=3 nodes (secondary sub-pipeline?)
components (largest first):
  [KEEP] (4): ij1-dimensions-inpput2montage, ij1-roi-to-rgb, ij1-roi-to-rgb, ij1-roi-to-rgb
  [drop] (3): ij1-roi-merge, ij1-roi-to-rgb, ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb
  [drop] (1): ij1-roi-to-rgb

## REVIEW — 'Export 2D ROI visualization images'
key: `dag-mined-cluster-0218`   ·   keep-largest, but a DROPPED component is also >=3 nodes (secondary sub-pipeline?)
components (largest first):
  [KEEP] (6): ij1-binary-connected-component-labeling-2d, ij1-color-blend-images, ij1-color-set-to-color, ij1-labels-to-rgb, ij1-morph-operation2d, iji-export-image-to-web
  [drop] (5): ij1-dimensions-inpput2montage, ij1-roi-render-overlay, ij1-roi-set-overlay, iji-export-image-to-web, plot-table:plot-histogram

## REVIEW — 'Measure pairwise distances between 3D ROIs'
key: `dag-mined-cluster-0184`   ·   keep-largest, but a DROPPED component is also >=3 nodes (secondary sub-pipeline?)
components (largest first):
  [KEEP] (5): cpython-script-iterating, cpython-script-iterating, cpython-script-iterating, ij3d-measure-roi3d, ij3d-measure-roi3d
  [drop] (3): ij1-binary-connected-component-labeling-3d, ij1-calibration-copy-physical-dimensions, ij3d-labels-to-roi
  [drop] (1): ij3d-measure-pairwise-roi3d
  [drop] (1): ij3d-measure-pairwise-roi3d

## REVIEW — 'Segment 2D images with edge projection and watershed'
key: `dag-mined-cluster-0140`   ·   keep-largest, but a DROPPED component is also >=3 nodes (secondary sub-pipeline?)
components (largest first):
  [KEEP] (7): annotate-filter-by-annotation, annotate-filter-by-annotation, clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection, ij1-blur-median2d, ij1-contrast-histogram-enhancer, ij1-segment-seeded-watershed, ij1-threshold-auto2d
  [drop] (5): clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection, ij1-blur-median2d, ij1-contrast-histogram-enhancer, ij1-segment-seeded-watershed, ij1-threshold-auto2d

## REVIEW — 'Smooth images with Gaussian blur'
key: `dag-derived-derived-mech-cluster-0250-3d-1fc616164f56`   ·   keep-largest part contains a data-flow-tunnel (orphaned loop plumbing)
components (largest first):
  [KEEP] (2): data-flow-tunnel-1-exit, ij1-blur-gaussian3d
  [drop] (2): data-flow-tunnel-1-entrance, data-flow-tunnel-1-exit
  [drop] (1): data-flow-tunnel-1-entrance

## REVIEW — 'Smooth images with Gaussian blur'
key: `dag-mined-cluster-0250`   ·   keep-largest part contains a data-flow-tunnel (orphaned loop plumbing)
components (largest first):
  [KEEP] (2): data-flow-tunnel-1-exit, ij1-blur-gaussian2d
  [drop] (2): data-flow-tunnel-1-entrance, data-flow-tunnel-1-exit
  [drop] (1): data-flow-tunnel-1-entrance
