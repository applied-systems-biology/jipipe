# Task 6 - raw tables (function-tree fit & secondary-function value)

## 4a - leaf occupancy (primary function id): all non-empty

| function id                   | name                        | nodes (primary) |
|-------------------------------|-----------------------------|----------------:|
| `filtering`                   | Filtering                   |             198 |
| `thresholding`                | Thresholding                |              94 |
| `image-arithmetic`            | Image arithmetic            |              89 |
| `image-transformations`       | Image transformations       |              65 |
| `merging-splitting`           | Merging & splitting         |              54 |
| `annotations-edit`            | Edit annotations            |              51 |
| `image-statistics`            | Image statistics            |              51 |
| `measurements`                | Measurements                |              49 |
| `roi-filters`                 | ROI filters                 |              48 |
| `image-moments`               | Image moments               |              42 |
| `image-comparison`            | Image comparison            |              38 |
| `image-projections`           | Image projections           |              37 |
| `segmentation`                | Segmentation                |              37 |
| `color-stack`                 | Color stack                 |              34 |
| `image-type-conversion`       | Image type conversion       |              33 |
| `filament-filtering`          | Filament filtering          |              32 |
| `image-labeling`              | Image labeling              |              30 |
| `image-pixel-manipulation`    | Image pixel manipulation    |              29 |
| `file-system`                 | File system                 |              28 |
| `image-data`                  | Image data                  |              27 |
| `image-neighbor-measurements` | Image neighbor measurements |              27 |
| `data-utilities`              | Data utilities              |              26 |
| `table-transform`             | Transform tables            |              22 |
| `table-data`                  | Table data                  |              19 |
| `preprocessing`               | Preprocessing               |              18 |
| `roi-properties`              | ROI properties              |              18 |
| `omero`                       | OMERO                       |              16 |
| `resampling`                  | Resampling                  |              16 |
| `table-filter-modify`         | Filter & modify tables      |              16 |
| `machine-learning-training`   | Machine learning training   |              15 |
| `misc`                        | Miscellaneous               |              14 |
| `table-join-split`            | Join & split tables         |              13 |
| `machine-learning-inference`  | Machine learning inference  |              12 |
| `roi-morphology`              | ROI morphology              |              12 |
| `machine-learning-models`     | Machine learning models     |              11 |
| `export-images`               | Images                      |              10 |
| `tracking-analysis`           | Tracking: analysis          |              10 |
| `annotations-convert`         | Convert annotations         |               9 |
| `scripting-python`            | Python                      |               9 |
| `filament-data`               | Filament data               |               8 |
| `graph-wrappers`              | Graph wrappers              |               8 |
| `roi-data`                    | ROI data                    |               8 |
| `table-aggregate`             | Aggregate tables            |               8 |
| `text-markup`                 | Text & markup               |               8 |
| `tracking-detection`          | Tracking: detection         |               8 |
| `export-tables`               | Tables                      |               7 |
| `filament-smoothing`          | Filament smoothing          |               7 |
| `forms`                       | Forms                       |               7 |
| `graph-debug`                 | Graph debugging             |               7 |
| `roi-intersections`           | ROI intersections           |               7 |
| `tracking-linking`            | Tracking: linking           |               7 |
| `roi-measurements`            | ROI measurements            |               6 |
| `tracking-filtering`          | Tracking: filtering         |               6 |
| `export-data`                 | Data                        |               5 |
| `plot-misc`                   | Other plots                 |               5 |
| `export-roi`                  | ROI                         |               4 |
| `image-properties`            | Image properties            |               4 |
| `plot-bar`                    | Bar plot                    |               4 |
| `scripting-r`                 | R                           |               4 |
| `filament-quantification`     | Filament quantification     |               3 |
| `graph-annotations`           | Graph annotations           |               3 |
| `misc-data`                   | Misc. data                  |               3 |
| `plot-line`                   | Line plot                   |               3 |
| `scripting-imagej-macro`      | ImageJ macro                |               3 |
| `export-scenes`               | 3D scenes                   |               2 |
| `scene-3d-data`               | Scene 3D data               |               2 |
| `scripting`                   | Scripting                   |               2 |
| `plot-scatter`                | Scatter plot                |               1 |
| `scripting-expression`        | Expressions                 |               1 |

## 4b - reading (i) catch-all only (primary == `misc`)

(none)

## 4b - reading (ii) whole misc branch (primary function in misc subtree)

| node id                    | name                             | primary function         |
|----------------------------|----------------------------------|--------------------------|
| `cpython-script-iterating` | Run Python script (iterating)    | `scripting-python`       |
| `data-batch-slicer`        | Iteration step slicer            | `data-utilities`         |
| `distribute-data`          | Split data randomly (percentage) | `data-utilities`         |
| `external-imagej-macro`    | Run ImageJ Macro                 | `scripting-imagej-macro` |
| `mask-to-3d-mesh`          | Mask to 3D scene                 | `data-utilities`         |
| `merge-slots`              | Merge inputs                     | `data-utilities`         |
| `parameters-define-table`  | Define multiple parameters       | `data-utilities`         |
| `tesseract-ocr`            | Find text (Tesseract OCR)        | `text-markup`            |
| `xml-prettify`             | Prettify XML                     | `text-markup`            |

## 4b - reading (ii), ruling's literal set {misc, data-utilities, scripting, text-markup}

| node id                   | name                             | primary function |
|---------------------------|----------------------------------|------------------|
| `data-batch-slicer`       | Iteration step slicer            | `data-utilities` |
| `distribute-data`         | Split data randomly (percentage) | `data-utilities` |
| `mask-to-3d-mesh`         | Mask to 3D scene                 | `data-utilities` |
| `merge-slots`             | Merge inputs                     | `data-utilities` |
| `parameters-define-table` | Define multiple parameters       | `data-utilities` |
| `tesseract-ocr`           | Find text (Tesseract OCR)        | `text-markup`    |
| `xml-prettify`            | Prettify XML                     | `text-markup`    |

## 4c - top-level branches: distinct corpus-USED node ids (primary branch)

| branch             | name             | distinct corpus-USED node ids (primary branch) | flag                                        |
|--------------------|------------------|-----------------------------------------------:|---------------------------------------------|
| `images`           | Images           |                                            132 |                                             |
| `rois`             | ROIs             |                                             40 |                                             |
| `tables`           | Tables           |                                             29 |                                             |
| `annotations`      | Annotations      |                                             27 |                                             |
| `data`             | Data Sources     |                                             22 |                                             |
| `machine-learning` | Machine learning |                                             18 |                                             |
| `filaments`        | Filaments        |                                             16 |                                             |
| `tracking`         | Tracking         |                                             15 |                                             |
| `misc`             | Miscellaneous    |                                              9 |                                             |
| `export`           | Export           |                                              7 |                                             |
| `plotting`         | Plotting         |                                              7 |                                             |
| `graph`            | Graph / Project  |                                              5 |                                             |
| `statistics`       | Statistics       |                                              0 | 0 used - strongest merge/demotion candidate |

## 4d - secondary-value candidates (all)

| # | node id                      | name                        | primary branch | best secondary branch | primaryMass | secondaryMass | known-branch neighbor endpoints |
|--:|------------------------------|-----------------------------|----------------|-----------------------|------------:|--------------:|--------------------------------:|
| 1 | `data-to-annotation-table`   | Convert to annotation table | `data`         | `annotations`         |           0 |           121 |                             242 |
| 2 | `plot-table:plot-histogram`  | JFreeChart Histogram plot   | `plotting`     | `images`              |           0 |           120 |                            1239 |
| 3 | `plot-table:plot-xy-scatter` | JFreeChart XY scatter plot  | `plotting`     | `images`              |           0 |            38 |                             416 |
| 4 | `plot-table:plot-xy-line`    | JFreeChart XY line plot     | `plotting`     | `images`              |           0 |            16 |                              77 |

Step 2 spot-check (3 named candidates, registry read)

| node id                      | verification (read from `registry.json`)                                                                                                                                                                                                                                                          |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `data-to-annotation-table`   | confirmed: claimed primary `table-data` + secondary `annotations-convert` are exactly its registry `functions[0]`/`[1]`; inputs `JIPipeData` (jipipe:data), outputs `AnnotationTableData` (annotation-table) -> plausible for the Annotations branch (its winning secondary)                      |
| `plot-table:plot-histogram`  | confirmed: claimed primary `plot-misc` + secondary `measurements` are exactly its registry `functions[0]`/`[1]`; inputs `ResultsTableData` (imagej-results-table), outputs `JFreeChartPlotData` (plot) -> plausible for the Images branch (its winning secondary) and for the Plotting primary    |
| `plot-table:plot-xy-scatter` | confirmed: claimed primary `plot-scatter` + secondary `measurements` are exactly its registry `functions[0]`/`[1]`; inputs `ResultsTableData` (imagej-results-table), outputs `JFreeChartPlotData` (plot) -> plausible for the Images branch (its winning secondary) and for the Plotting primary |

Corpus-USED registry nodes with >= 1 secondary branch (denominator context): `data-to-annotation-table`,
`ij1-roi-statistics`, `plot-table:plot-box-and-whisker`, `plot-table:plot-category-statistical-bar`,
`plot-table:plot-category-statistical-line`, `plot-table:plot-histogram`, `plot-table:plot-pie-2d`,
`plot-table:plot-xy-line`, `plot-table:plot-xy-scatter`, `table-apply-expression-to-table-by-columns`

Absent-from-registry corpus-USED node ids (unknown-branch): `clij:plugin:net.haesleinhuepf.clij2.plugins.ErodeLabels`,
`clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection`,
`clij:plugin:net.haesleinhuepf.clij2.plugins.ReduceLabelsToCentroids`,
`clij:plugin:net.haesleinhuepf.clij2.plugins.ReduceLabelsToLabelEdges`,
`clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection`, `graph-annotation-group-box`,
`plotly-export`, `plotly-table-to-animation-scatter`, `plotly-table-to-histogram`
