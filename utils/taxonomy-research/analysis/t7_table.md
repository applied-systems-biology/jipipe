# Task 7 — raw tables (mined patterns)

**Totals (kept, support >= 3):** chain=11,215, fan-out=240, fan-in=158, conversion=1,428, annotation-sandwich=4,
compartment-boundary=77, compartment-cycle=0; N=13,122.

## chain (top 25 of 11,215 kept)

|  # | id                                                                                                                 | shape | support | projects | occurrences | role summary                |
|---:|--------------------------------------------------------------------------------------------------------------------|-------|--------:|---------:|------------:|-----------------------------|
|  1 | `chain-3-project-user-directory-node-group-import-imagej-bioformats`                                               | chain |      54 |       54 |          56 | import -> other             |
|  2 | `chain-3-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output`                                       | chain |      52 |       52 |         226 | core-io -> other            |
|  3 | `chain-3-import-folder-list-folder-list-subfolders-path-to-annotation-simple`                                      | chain |      51 |       51 |          57 | import -> annotate          |
|  4 | `chain-3-folder-list-subfolders-path-to-annotation-simple-folder-list-files`                                       | chain |      50 |       50 |          70 | import -> annotate          |
|  5 | `chain-3-path-to-annotation-simple-folder-list-files-path-to-annotation-simple`                                    | chain |      50 |       50 |          60 | annotate -> import          |
|  6 | `chain-4-folder-list-subfolders-path-to-annotation-simple-folder-list-files-path-to-annotation-simple`             | chain |      50 |       50 |          60 | import -> annotate          |
|  7 | `chain-3-node-group-import-imagej-bioformats-image-properties-to-annotation`                                       | chain |      44 |       44 |          46 | other -> import -> annotate |
|  8 | `chain-3-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file`                              | chain |      43 |       43 |          47 | import -> annotate          |
|  9 | `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb`                                      | chain |      42 |       42 |        1034 | other -> core-io            |
| 10 | `chain-3-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d`                          | chain |      40 |       40 |          52 | other                       |
| 11 | `chain-4-project-user-directory-node-group-import-imagej-bioformats-image-properties-to-annotation`                | chain |      40 |       40 |          42 | import -> other -> annotate |
| 12 | `chain-3-folder-list-files-path-to-annotation-simple-path-filter`                                                  | chain |      38 |       38 |          44 | import -> annotate          |
| 13 | `chain-3-ij1-morph-operation2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb`                                        | chain |      38 |       38 |          38 | other                       |
| 14 | `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output`                           | chain |      38 |       38 |         270 | other -> core-io            |
| 15 | `chain-3-table-add-annotation-columns-table-merge-tables-jipipe-compartment-output`                                | chain |      38 |       38 |         192 | other -> core-io            |
| 16 | `chain-3-ij1-roi-to-mask-ij1-morph-operation2d-jipipe-compartment-output`                                          | chain |      36 |       36 |          36 | other -> core-io            |
| 17 | `chain-3-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d`                                        | chain |      36 |       36 |          36 | import -> other             |
| 18 | `chain-3-jipipe-comment-node-group-import-imagej-bioformats`                                                       | chain |      36 |       36 |          52 | other -> import             |
| 19 | `chain-3-jipipe-compartment-output-ij1-roi-filter-by-overlap-jipipe-compartment-output`                            | chain |      36 |       36 |         168 | core-io -> other            |
| 20 | `chain-4-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d-jipipe-compartment-output`              | chain |      36 |       36 |          36 | import -> other -> core-io  |
| 21 | `chain-4-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter`                        | chain |      36 |       36 |          42 | annotate -> import          |
| 22 | `chain-5-folder-list-subfolders-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter` | chain |      36 |       36 |          42 | import -> annotate          |
| 23 | `chain-3-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-calibration-copy-physical-dimensions`              | chain |      34 |       34 |          70 | other                       |
| 24 | `chain-3-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-rgb`     | chain |      34 |       34 |          42 | other                       |
| 25 | `chain-4-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb`           | chain |      34 |       34 |          34 | other                       |

## fan-out (top 25 of 240 kept)

|  # | id                                                                     | shape   | support | projects | occurrences | role summary                           |
|---:|------------------------------------------------------------------------|---------|--------:|---------:|------------:|----------------------------------------|
|  1 | `fan-out-ij1-analyze-find-particles2d-core-io-1-other-1`               | fan-out |      37 |       37 |          60 | other -> core-io                       |
|  2 | `fan-out-import-imagej-roi-from-file-core-io-1-other-1`                | fan-out |      34 |       34 |          36 | import -> core-io -> other             |
|  3 | `fan-out-ij3d-roi-convert-to-rgb-other-2`                              | fan-out |      30 |       30 |          70 | other                                  |
|  4 | `fan-out-cellpose-segmentation-inference-3-x-other-2`                  | fan-out |      28 |       28 |          36 | other                                  |
|  5 | `fan-out-ij3d-labels-to-roi-core-io-1-other-2`                         | fan-out |      28 |       28 |          46 | other -> core-io                       |
|  6 | `fan-out-jipipe-compartment-output-annotate-1-other-1`                 | fan-out |      28 |       28 |          28 | core-io -> annotate -> other           |
|  7 | `fan-out-table-merge-tables-core-io-1-export-1`                        | fan-out |      28 |       28 |          48 | other -> core-io -> export             |
|  8 | `fan-out-ij1-morph-operation2d-annotate-1-core-io-1`                   | fan-out |      26 |       26 |          26 | other -> annotate -> core-io           |
|  9 | `fan-out-ij1-roi-filter-statistics-core-io-1-other-1`                  | fan-out |      26 |       26 |          44 | other -> core-io                       |
| 10 | `fan-out-ij1-roi-filter-statistics-core-io-1-other-2`                  | fan-out |      26 |       26 |          26 | other -> core-io                       |
| 11 | `fan-out-ij1-roi-filter-statistics-other-2`                            | fan-out |      26 |       26 |          36 | other                                  |
| 12 | `fan-out-import-folder-list-import-1-other-1`                          | fan-out |      26 |       26 |          26 | import -> other                        |
| 13 | `fan-out-table-apply-expression-per-row-v2-core-io-1-export-1`         | fan-out |      26 |       26 |          48 | other -> core-io -> export             |
| 14 | `fan-out-cellpose-inference-2-x-other-2`                               | fan-out |      24 |       24 |          42 | other                                  |
| 15 | `fan-out-ij1-roi-merge-other-2`                                        | fan-out |      24 |       24 |          24 | other                                  |
| 16 | `fan-out-ij1-threshold-auto2d-other-2`                                 | fan-out |      24 |       24 |          58 | other                                  |
| 17 | `fan-out-ij3d-analyze-find-particles-core-io-1-other-2`                | fan-out |      22 |       22 |          22 | other -> core-io                       |
| 18 | `fan-out-plot-table-plot-histogram-core-io-1-export-1`                 | fan-out |      22 |       22 |          62 | plotting -> core-io -> export          |
| 19 | `fan-out-import-imagej-imgplus-from-file-other-2`                      | fan-out |      21 |       21 |          21 | import -> other                        |
| 20 | `fan-out-annotate-split-by-annotation-other-1-unknown-1`               | fan-out |      20 |       20 |          54 | annotate -> other -> unknown           |
| 21 | `fan-out-ij1-blur-median2d-other-2`                                    | fan-out |      20 |       20 |          20 | other                                  |
| 22 | `fan-out-ij1-roi-to-rgb-core-io-1-other-1`                             | fan-out |      20 |       20 |          36 | other -> core-io                       |
| 23 | `fan-out-import-imagej-imgplus-from-file-annotate-1-core-io-1-other-1` | fan-out |      20 |       20 |          20 | import -> annotate -> core-io -> other |
| 24 | `fan-out-jipipe-compartment-output-other-3`                            | fan-out |      19 |       19 |          21 | core-io -> other                       |
| 25 | `fan-out-ij1-analyze-find-particles2d-other-2`                         | fan-out |      18 |       18 |          26 | other                                  |

## fan-in (top 25 of 158 kept)

|  # | id                                                                  | shape  | support | projects | occurrences | role summary                |
|---:|---------------------------------------------------------------------|--------|--------:|---------:|------------:|-----------------------------|
|  1 | `fan-in-ij1-roi-to-rgb-core-io-1-other-1`                           | fan-in |      71 |       71 |         354 | other -> core-io            |
|  2 | `fan-in-ij1-roi-to-rgb-other-2`                                     | fan-in |      53 |       53 |         205 | other                       |
|  3 | `fan-in-jipipe-compartment-output-other-2`                          | fan-in |      44 |       44 |          64 | core-io -> other            |
|  4 | `fan-in-ij1-color-blend-images-core-io-1-other-1`                   | fan-in |      42 |       42 |         114 | other -> core-io            |
|  5 | `fan-in-jipipe-compartment-output-export-1-other-1`                 | fan-in |      42 |       42 |          70 | core-io -> export -> other  |
|  6 | `fan-in-node-group-import-1-other-1`                                | fan-in |      40 |       40 |          40 | other -> import             |
|  7 | `fan-in-cellpose-segmentation-inference-3-x-other-2`                | fan-in |      38 |       38 |         108 | other                       |
|  8 | `fan-in-ij1-roi-filter-statistics-core-io-1-other-1`                | fan-in |      37 |       37 |          74 | other -> core-io            |
|  9 | `fan-in-ij1-roi-filter-statistics-other-2`                          | fan-in |      37 |       37 |          72 | other                       |
| 10 | `fan-in-ij1-calibration-copy-physical-dimensions-core-io-1-other-1` | fan-in |      36 |       36 |         242 | other -> core-io            |
| 11 | `fan-in-ij1-roi-filter-by-overlap-core-io-1-other-1`                | fan-in |      34 |       34 |          92 | other -> core-io            |
| 12 | `fan-in-cellpose-inference-2-x-other-2`                             | fan-in |      32 |       32 |          92 | other                       |
| 13 | `fan-in-ij1-color-blend-images-other-2`                             | fan-in |      32 |       32 |          66 | other                       |
| 14 | `fan-in-ij1-roi-to-mask-other-2`                                    | fan-in |      30 |       30 |          52 | other                       |
| 15 | `fan-in-ij3d-measure-roi3d-core-io-1-other-1`                       | fan-in |      30 |       30 |          54 | other -> core-io            |
| 16 | `fan-in-jipipe-compartment-output-other-3`                          | fan-in |      28 |       28 |          30 | core-io -> other            |
| 17 | `fan-in-cellpose-inference-2-x-core-io-1-other-1`                   | fan-in |      26 |       26 |          40 | other -> core-io            |
| 18 | `fan-in-ij1-roi-statistics-core-io-1-other-1`                       | fan-in |      26 |       26 |          72 | other -> core-io            |
| 19 | `fan-in-ij1-threshold-auto2d-other-2`                               | fan-in |      26 |       26 |          26 | other                       |
| 20 | `fan-in-jipipe-compartment-output-other-4`                          | fan-in |      26 |       26 |          30 | core-io -> other            |
| 21 | `fan-in-path-filter-annotate-1-other-1`                             | fan-in |      26 |       26 |          28 | import -> annotate -> other |
| 22 | `fan-in-path-to-annotation-simple-import-1-other-1`                 | fan-in |      26 |       26 |          26 | annotate -> import -> other |
| 23 | `fan-in-ij1-roi-to-mask-import-2`                                   | fan-in |      24 |       24 |          24 | other -> import             |
| 24 | `fan-in-ij3d-roi-convert-to-rgb-other-2`                            | fan-in |      24 |       24 |         134 | other                       |
| 25 | `fan-in-jipipe-compartment-output-core-io-1-other-1`                | fan-in |      24 |       24 |          42 | core-io -> other            |

## conversion (top 25 of 1,428 kept)

|  # | id                                                                                                                | shape      | support | projects | occurrences | role summary                  |
|---:|-------------------------------------------------------------------------------------------------------------------|------------|--------:|---------:|------------:|-------------------------------|
|  1 | `conversion-project-user-directory-node-group-import-imagej-bioformats`                                           | conversion |      54 |       54 |          56 | import -> other               |
|  2 | `conversion-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output`                                   | conversion |      52 |       52 |         242 | core-io -> other              |
|  3 | `conversion-folder-list-subfolders-path-to-annotation-simple-folder-list-files`                                   | conversion |      50 |       50 |          70 | import -> annotate            |
|  4 | `conversion-path-to-annotation-simple-folder-list-files-path-to-annotation-simple`                                | conversion |      50 |       50 |          60 | annotate -> import            |
|  5 | `conversion-node-group-import-imagej-bioformats-image-properties-to-annotation`                                   | conversion |      44 |       44 |          46 | other -> import -> annotate   |
|  6 | `conversion-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file`                          | conversion |      43 |       43 |          47 | import -> annotate            |
|  7 | `conversion-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d`                      | conversion |      40 |       40 |          52 | other                         |
|  8 | `conversion-folder-list-files-path-to-annotation-simple-path-filter`                                              | conversion |      38 |       38 |          44 | import -> annotate            |
|  9 | `conversion-ij1-morph-operation2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb`                                    | conversion |      38 |       38 |          38 | other                         |
| 10 | `conversion-ij1-roi-to-mask-ij1-morph-operation2d-jipipe-compartment-output`                                      | conversion |      36 |       36 |          36 | other -> core-io              |
| 11 | `conversion-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb`                                  | conversion |      36 |       36 |         222 | other -> core-io              |
| 12 | `conversion-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d`                                    | conversion |      36 |       36 |          36 | import -> other               |
| 13 | `conversion-jipipe-comment-node-group-import-imagej-bioformats`                                                   | conversion |      36 |       36 |          52 | other -> import               |
| 14 | `conversion-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-calibration-copy-physical-dimensions`          | conversion |      34 |       34 |          70 | other                         |
| 15 | `conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-rgb` | conversion |      34 |       34 |          42 | other                         |
| 16 | `conversion-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file`                              | conversion |      32 |       32 |          32 | import -> annotate            |
| 17 | `conversion-folder-list-subfolders-path-to-annotation-simple-path-filter`                                         | conversion |      32 |       32 |          36 | import -> annotate            |
| 18 | `conversion-ij3d-labels-to-roi-jipipe-compartment-output-ij3d-measure-roi3d`                                      | conversion |      32 |       32 |         140 | other -> core-io              |
| 19 | `conversion-jipipe-compartment-output-jipipe-compartment-output-jipipe-compartment-output`                        | conversion |      32 |       32 |         204 | core-io                       |
| 20 | `conversion-path-filter-folder-list-files-path-to-annotation-simple`                                              | conversion |      32 |       32 |          68 | import -> annotate            |
| 21 | `conversion-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask`                                | conversion |      32 |       32 |          32 | annotate -> import -> other   |
| 22 | `conversion-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output`                      | conversion |      32 |       32 |          32 | annotate -> import -> core-io |
| 23 | `conversion-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-calibration-copy-physical-dimensions`        | conversion |      30 |       30 |         760 | other -> core-io              |
| 24 | `conversion-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output`                       | conversion |      30 |       30 |         314 | other -> core-io              |
| 25 | `conversion-ij1-threshold-auto2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb`                                     | conversion |      30 |       30 |          35 | other                         |

## annotation-sandwich (top 25 of 4 kept)

| # | id                                                                                    | shape               | support | projects | occurrences | role summary                  |
|--:|---------------------------------------------------------------------------------------|---------------------|--------:|---------:|------------:|-------------------------------|
| 1 | `annotation-sandwich-import-imagej-imgplus-from-file-annotate-with-data-other`        | annotation-sandwich |      18 |       18 |          18 | import -> annotate -> other   |
| 2 | `annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-other`   | annotation-sandwich |      16 |       16 |          22 | import -> annotate -> other   |
| 3 | `annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-core-io` | annotation-sandwich |       6 |        6 |           6 | import -> annotate -> core-io |
| 4 | `annotation-sandwich-import-file-list-path-to-annotation-simple-other`                | annotation-sandwich |       4 |        4 |          12 | import -> annotate -> other   |

## compartment-boundary (top 25 of 77 kept)

|  # | id                                                                                        | shape                | support | projects | occurrences | role summary        |
|---:|-------------------------------------------------------------------------------------------|----------------------|--------:|---------:|------------:|---------------------|
|  1 | `compartment-boundary-jipipe-compartment-output-ij1-roi-to-rgb`                           | compartment-boundary |      70 |       70 |         446 | core-io -> other    |
|  2 | `compartment-boundary-jipipe-compartment-output-jipipe-compartment-output`                | compartment-boundary |      66 |       66 |         518 | core-io             |
|  3 | `compartment-boundary-jipipe-compartment-output-ij1-roi-filter-by-overlap`                | compartment-boundary |      44 |       44 |         406 | core-io -> other    |
|  4 | `compartment-boundary-jipipe-compartment-output-ij1-color-blend-images`                   | compartment-boundary |      40 |       40 |         116 | core-io -> other    |
|  5 | `compartment-boundary-jipipe-compartment-output-ij1-roi-to-mask`                          | compartment-boundary |      38 |       38 |         170 | core-io -> other    |
|  6 | `compartment-boundary-jipipe-compartment-output-ij3d-measure-roi3d`                       | compartment-boundary |      38 |       38 |         290 | core-io -> other    |
|  7 | `compartment-boundary-jipipe-compartment-output-ij1-roi-filter-statistics`                | compartment-boundary |      37 |       37 |          85 | core-io -> other    |
|  8 | `compartment-boundary-jipipe-compartment-output-ij1-roi-statistics`                       | compartment-boundary |      36 |       36 |         180 | core-io -> other    |
|  9 | `compartment-boundary-jipipe-compartment-output-ij1-calibration-copy-physical-dimensions` | compartment-boundary |      32 |       32 |         204 | core-io -> other    |
| 10 | `compartment-boundary-jipipe-compartment-output-ij1-threshold-auto2d`                     | compartment-boundary |      32 |       32 |          86 | core-io -> other    |
| 11 | `compartment-boundary-jipipe-compartment-output-ij3d-roi-filter-by-overlap`               | compartment-boundary |      28 |       28 |          60 | core-io -> other    |
| 12 | `compartment-boundary-jipipe-compartment-output-cellpose-inference-2-x`                   | compartment-boundary |      26 |       26 |          50 | core-io -> other    |
| 13 | `compartment-boundary-jipipe-compartment-output-ij1-roi-to-annotations`                   | compartment-boundary |      24 |       24 |         266 | core-io -> annotate |
| 14 | `compartment-boundary-jipipe-compartment-output-annotate-data-with-table-values`          | compartment-boundary |      23 |       23 |         120 | core-io -> annotate |
| 15 | `compartment-boundary-jipipe-compartment-output-ij1-color-set-to-color`                   | compartment-boundary |      22 |       22 |          62 | core-io -> other    |
| 16 | `compartment-boundary-jipipe-compartment-output-ij1-roi-to-labels-name`                   | compartment-boundary |      22 |       22 |          24 | core-io -> other    |
| 17 | `compartment-boundary-jipipe-compartment-output-ij3d-roi-convert-to-rgb`                  | compartment-boundary |      22 |       22 |         154 | core-io -> other    |
| 18 | `compartment-boundary-jipipe-compartment-output-cellpose-2`                               | compartment-boundary |      20 |       20 |          42 | core-io -> other    |
| 19 | `compartment-boundary-jipipe-compartment-output-cellpose-segmentation-inference-3-x`      | compartment-boundary |      20 |       20 |          80 | core-io -> other    |
| 20 | `compartment-boundary-jipipe-compartment-output-ij1-morph-operation2d`                    | compartment-boundary |      20 |       20 |          28 | core-io -> other    |
| 21 | `compartment-boundary-jipipe-compartment-output-ij1-background-rollingball2d`             | compartment-boundary |      18 |       18 |          22 | core-io -> other    |
| 22 | `compartment-boundary-jipipe-compartment-output-ij1-noise-removeoutliers2d`               | compartment-boundary |      18 |       18 |          18 | core-io -> other    |
| 23 | `compartment-boundary-jipipe-compartment-output-iji-export-image-v2`                      | compartment-boundary |      17 |       17 |          44 | core-io -> export   |
| 24 | `compartment-boundary-jipipe-compartment-output-ij1-blur-gaussian3d`                      | compartment-boundary |      16 |       16 |          16 | core-io -> other    |
| 25 | `compartment-boundary-jipipe-compartment-output-ij1-math-divide-by-maximum`               | compartment-boundary |      16 |       16 |          32 | core-io -> other    |

## compartment-cycle (top 25 of 0 kept)

| # | id | shape | support | projects | occurrences | role summary |
|--:|----|-------|--------:|---------:|------------:|--------------|

## cross-check — per-doc

| doc                                        | extracted ids | in corpus | best overlap | matched pattern                                                                                                                                                                       |
|--------------------------------------------|--------------:|----------:|-------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 3d-roi-workflow.md                         |             8 |         8 |         0.62 | `fan-in-ij3d-roi-convert-to-rgb-core-io-1-other-1`                                                                                                                                    |
| cellpose-segmentation.md                   |            20 |        14 |         0.30 | (none)                                                                                                                                                                                |
| cellpose-training.md                       |             4 |         2 |         0.50 | `fan-out-distribute-data-other-3`                                                                                                                                                     |
| common-workflow-outline.md                 |            21 |        12 |         0.14 | (none)                                                                                                                                                                                |
| compartment-organization.md                |             5 |         5 |         0.60 | `chain-3-node-group-io-interface-jipipe-compartment-output`                                                                                                                           |
| conversion-chain-image-to-measurement.md   |             6 |         6 |         0.50 | `chain-3-ij1-roi-statistics-annotate-set-plot-table-plot-histogram`                                                                                                                   |
| conversion-chain-image-type-conversions.md |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| conversion-chain-mask-to-roi-to-labels.md  |             6 |         6 |         0.33 | (none)                                                                                                                                                                                |
| conversion-chain-roi-filaments-roi.md      |             3 |         2 |         0.33 | (none)                                                                                                                                                                                |
| conversion-chain-roi-labels-roi.md         |             4 |         4 |         0.50 | `chain-3-ij1-roi-to-labels-expression-clij-plugin-net-haesleinhuepf-clij2-plugins-excludelabelsonedges-ij1-labels-to-roi`                                                             |
| conversion-chain-roi-visualization.md      |             3 |         3 |         0.67 | `chain-3-annotate-set-ij1-roi-to-rgb-iji-export-image-v2`                                                                                                                             |
| filament-analysis.md                       |             6 |         6 |         0.67 | `chain-5-filaments-skeleton-to-filaments-2d-filaments-smooth-downscale-filaments-filter-components-by-properties-filaments-set-vertex-thickness-from-image-filaments-convert-to-mask` |
| high-content-screening.md                  |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| illumination-correction.md                 |             1 |         1 |         0.00 | (none)                                                                                                                                                                                |
| import-split-annotate.md                   |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| measurement.md                             |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| novel-algorithm-patterns.md                |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| python-script-integration.md               |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| roi-refinement-cascade.md                  |            13 |        13 |         0.54 | `fan-in-ij1-roi-to-rgb-other-2`                                                                                                                                                       |
| segmentation.md                            |             0 |         0 |         0.00 | (none)                                                                                                                                                                                |
| table-processing-pipeline.md               |             8 |         8 |         0.62 | `chain-5-table-rename-columns-2-table-apply-expression-per-row-v2-table-merge-columns-table-add-annotation-columns-table-merge-tables`                                                |
| trackmate-tracking.md                      |             2 |         2 |         1.00 | `chain-4-annotate-with-source-slot-jipipe-compartment-output-ij3d-measure-roi3d-plot-table-plot-histogram`                                                                            |
| visualization-and-export.md                |            13 |        13 |         0.38 | (none)                                                                                                                                                                                |
| z-stack-and-3d.md                          |             5 |         5 |         0.80 | `fan-out-ij3d-analyze-find-particles-other-2`                                                                                                                                         |

## 5b correction candidates (5)

- cellpose-segmentation.md: cellpose, cellpose-2, cellpose-denoise-inference-3.x, cellpose-denoise-training-3.x,
  cellpose-inference-2.x, cellpose-segmentation-inference-3.x, cellpose-segmentation-inference-4.x,
  cellpose-segmentation-training-3.x, cellpose-segmentation-training-4.x, cellpose-training, cellpose-training-2,
  cellpose-training-2.x, import-cellpose-2.x-pretrained-model, import-cellpose-3.x-pretrained-denoise-model,
  import-cellpose-3.x-pretrained-segmentation-model, import-cellpose-4.x-pretrained-segmentation-model,
  import-cellpose-model, import-cellpose-model-v2, import-cellpose-size-model, import-cellpose-size-model-v2 (14 in
  corpus)
- common-workflow-outline.md: annotate-data-with-table-values, annotate-set, annotate-set-single, annotation-merge,
  export-data, export-data-by-parameter-v2, export-data-table, export-data-table-by-parameter, folder-list-files,
  generate-unique-annotation, get-exported-path, import-file, import-file-list, import-folder, import-folder-list,
  import-path, import-path-list, overwrite-annotations, project-user-directory, rename-annotation, simplify-annotations
  (12 in corpus)
- conversion-chain-mask-to-roi-to-labels.md: ij1-labels-to-roi, ij1-roi-to-labels-expression, ij1-roi-to-labels-name,
  ij1-roi-to-mask, ij3d-labels-to-roi, ij3d-roi-convert-to-labels (6 in corpus)
- conversion-chain-roi-filaments-roi.md: filaments-convert-roi2d-to-filaments, filaments-convert-to-roi,
  filaments-filter-components-by-properties (2 in corpus)
- visualization-and-export.md: export-data-by-parameter-v2, ij1-calibration-copy-physical-dimensions,
  ij1-color-blend-images, ij1-roi-set-properties, ij1-roi-to-rgb, ij1-set-lut-from-color-map,
  ij3d-roi-change-properties, ij3d-roi-convert-to-rgb, iji-export-image-v2, iji-export-table-as-xlsx-v2,
  iji-export-table-v2, plot-table:plot-histogram, plot-table:plot-xy-scatter (13 in corpus)

## 5c library gaps (5,390) — support >= 5, no doc match

- fan-in-ij1-roi-to-rgb-core-io-1-other-1 (support 71)
- compartment-boundary-jipipe-compartment-output-ij1-roi-to-rgb (support 70)
- compartment-boundary-jipipe-compartment-output-jipipe-compartment-output (support 66)
- chain-3-project-user-directory-node-group-import-imagej-bioformats (support 54)
- conversion-project-user-directory-node-group-import-imagej-bioformats (support 54)
- chain-3-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output (support 52)
- conversion-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output (support 52)
- chain-3-import-folder-list-folder-list-subfolders-path-to-annotation-simple (support 51)
- chain-3-folder-list-subfolders-path-to-annotation-simple-folder-list-files (support 50)
- chain-3-path-to-annotation-simple-folder-list-files-path-to-annotation-simple (support 50)
- chain-4-folder-list-subfolders-path-to-annotation-simple-folder-list-files-path-to-annotation-simple (support 50)
- conversion-folder-list-subfolders-path-to-annotation-simple-folder-list-files (support 50)
- conversion-path-to-annotation-simple-folder-list-files-path-to-annotation-simple (support 50)
- chain-3-node-group-import-imagej-bioformats-image-properties-to-annotation (support 44)
- compartment-boundary-jipipe-compartment-output-ij1-roi-filter-by-overlap (support 44)
- conversion-node-group-import-imagej-bioformats-image-properties-to-annotation (support 44)
- fan-in-jipipe-compartment-output-other-2 (support 44)
- chain-3-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file (support 43)
- conversion-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file (support 43)
- chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb (support 42)
- fan-in-ij1-color-blend-images-core-io-1-other-1 (support 42)
- chain-3-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d (support 40)
- chain-4-project-user-directory-node-group-import-imagej-bioformats-image-properties-to-annotation (support 40)
- compartment-boundary-jipipe-compartment-output-ij1-color-blend-images (support 40)
- conversion-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d (support 40)
