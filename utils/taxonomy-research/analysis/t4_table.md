# Task 4 - raw tables (workflow chains & branch fragmentation)

|  # | node id                           | name                           | distinct projects | node instances |
|---:|-----------------------------------|--------------------------------|------------------:|---------------:|
|  1 | `jipipe:compartment-output`       | Compartment output             |               153 |            780 |
|  2 | `ij1-threshold-auto2d`            | Auto threshold 2D (8-bit)      |               134 |            549 |
|  3 | `ij1-roi-to-rgb`                  | Convert 2D ROI to RGB          |               110 |            707 |
|  4 | `ij1-morph-operation2d`           | Morphological operation 2D     |               109 |            534 |
|  5 | `ij1-analyze-find-particles2d`    | Find particles 2D              |               103 |            280 |
|  6 | `image-properties-to-annotation`  | Annotate with image properties |                98 |            122 |
|  7 | `project-user-directory`          | Project user path              |                98 |            110 |
|  8 | `node-group`                      | Group                          |                97 |            196 |
|  9 | `ij1-blur-median2d`               | Median filter 2D               |                92 |            216 |
| 10 | `import-imagej-bioformats`        | Bio-Formats importer           |                91 |            108 |
| 11 | `jipipe:comment`                  | Comment                        |                90 |            308 |
| 12 | `graph-annotation-text-box`       | Text box canvas note           |                87 |            931 |
| 13 | `table-merge-tables`              | Merge table rows               |                84 |            361 |
| 14 | `ij1-set-lut-from-color-map`      | Set LUT (color map)            |                83 |            541 |
| 15 | `ij1-morph-binary-fillholes2d`    | Morphological hole filling 2D  |                80 |            202 |
| 16 | `ij1-blur-gaussian2d`             | Gaussian blur 2D               |                79 |            207 |
| 17 | `iji-export-image-v2`             | Export image                   |                78 |            695 |
| 18 | `import-imagej-imgplus-from-file` | Import image                   |                78 |            114 |
| 19 | `ij1-color-blend-images`          | Blend images                   |                77 |            359 |
| 20 | `import-folder-list`              | Folder list                    |                77 |            147 |

|  # | A → B → C (node ids)                                                                                    | occurrences | projects | top-level branches (union of A∪B∪C)  | fragmented | isolated middle |
|---:|---------------------------------------------------------------------------------------------------------|------------:|---------:|--------------------------------------|-----------:|----------------:|
|  1 | `ij1-roi-filter-by-overlap` → `jipipe:compartment-output` → `ij1-roi-to-annotations`                    |        2030 |       24 | Annotations ∪ Graph / Project ∪ ROIs |        yes |             yes |
|  2 | `io-interface` → `jipipe:compartment-output` → `ij3d-measure-roi3d`                                     |        1944 |       18 | Graph / Project ∪ ROIs               |         no |              no |
|  3 | `ij1-roi-to-rgb` → `jipipe:compartment-output` → `ij1-roi-filter-by-overlap`                            |        1206 |       28 | Graph / Project ∪ ROIs               |         no |             yes |
|  4 | `jipipe:compartment-output` → `jipipe:compartment-output` → `ij1-roi-to-annotations`                    |        1158 |       24 | Annotations ∪ Graph / Project        |         no |              no |
|  5 | `ij1-roi-to-rgb` → `jipipe:compartment-output` → `jipipe:compartment-output`                            |        1152 |       26 | Graph / Project ∪ ROIs               |         no |              no |
|  6 | `iji-export-image-v2` → `io-interface` → `jipipe:compartment-output`                                    |        1124 |       26 | Export ∪ Graph / Project             |         no |              no |
|  7 | `ij3d-roi-filter-by-overlap` → `jipipe:compartment-output` → `ij3d-measure-roi3d`                       |        1080 |       18 | Graph / Project ∪ ROIs               |         no |             yes |
|  8 | `ij1-set-lut-from-color-map` → `jipipe:compartment-output` → `ij1-roi-to-rgb`                           |        1034 |       42 | Graph / Project ∪ Images ∪ ROIs      |        yes |             yes |
|  9 | `io-interface` → `jipipe:compartment-output` → `annotate-data-with-table-values`                        |         972 |       18 | Annotations ∪ Graph / Project        |         no |              no |
| 10 | `ij1-roi-filter-statistics` → `jipipe:compartment-output` → `ij1-roi-filter-by-overlap`                 |         828 |       28 | Graph / Project ∪ ROIs               |         no |             yes |
| 11 | `ij1-set-lut-from-color-map` → `jipipe:compartment-output` → `ij3d-roi-convert-to-rgb`                  |         804 |       22 | Graph / Project ∪ Images ∪ ROIs      |        yes |             yes |
| 12 | `ij1-set-lut-from-color-map` → `jipipe:compartment-output` → `ij1-calibration-copy-physical-dimensions` |         768 |       30 | Graph / Project ∪ Images             |         no |             yes |
| 13 | `ij3d-roi-convert-to-rgb` → `io-interface` → `jipipe:compartment-output`                                |         738 |       18 | Graph / Project ∪ ROIs               |         no |              no |
| 14 | `ij1-roi-filter-by-overlap` → `jipipe:compartment-output` → `ij1-roi-to-mask`                           |         720 |       14 | Graph / Project ∪ ROIs               |         no |             yes |
| 15 | `jipipe:compartment-output` → `ij1-roi-filter-by-overlap` → `jipipe:compartment-output`                 |         658 |       36 | Graph / Project ∪ ROIs               |         no |             yes |
| 16 | `jipipe:compartment-output` → `jipipe:compartment-output` → `ij3d-measure-pairwise-roi3d`               |         640 |        8 | Graph / Project ∪ ROIs               |         no |              no |
| 17 | `iji-export-table-as-xlsx-v2` → `io-interface` → `jipipe:compartment-output`                            |         632 |        8 | Export ∪ Graph / Project             |         no |              no |
| 18 | `jipipe:compartment-output` → `jipipe:compartment-output` → `ij1-roi-to-mask`                           |         604 |       16 | Graph / Project ∪ ROIs               |         no |              no |
| 19 | `jipipe:compartment-output` → `jipipe:compartment-output` → `jipipe:compartment-output`                 |         594 |       32 | Graph / Project                      |         no |              no |
| 20 | `ij1-set-lut-from-color-map` → `jipipe:compartment-output` → `jipipe:compartment-output`                |         544 |       38 | Graph / Project ∪ Images             |         no |              no |

| node id                        | name                          | typed outgoing edges (corpus) | narrow consumer branch | distinct registry consumer node ids |
|--------------------------------|-------------------------------|------------------------------:|------------------------|------------------------------------:|
| `data-to-annotation-table`     | Convert to annotation table   |                           121 | Annotations            |                                   3 |
| `ij1-roi-to-labels-expression` | 2D ROI to Labels (Expression) |                            34 | Images                 |                                  19 |
| `ij1-roi-to-labels-name`       | 2D ROI to Labels (by name)    |                            30 | Images                 |                                  19 |
