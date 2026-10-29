---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb -> cellpose-model-v2 -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask -> imagej-results-table"
tags: [Graph / Project, Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 71
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
  projectCount: 71
  exampleGraph:
    compartment: "File Handling+Glomeruli"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    nodes:
      - "jipipe:compartment-output": "Output"
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
    edges:
      - {"from":"Find particles 2D","fromSlot":"ROI","to":"Convert ROI to RGB","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to RGB fans in from 13 source ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Convert 2D ROI to RGB fans in from 13 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"d94a4db5-4fca-5b27-bcdd-d5aac64fd333\",\"jipipe:node:name\":\"Output\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"d94a4db5-4fca-5b27-bcdd-d5aac64fd333\",\"jipipe:node:name\":\"Find particles 2D\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"d94a4db5-4fca-5b27-bcdd-d5aac64fd333\",\"jipipe:node:name\":\"Convert ROI to RGB\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Convert 2D ROI to RGB fans in from 13 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-roi-to-rgb-core-io-1-other-1` (shape `fan-in`, support 71).

## Role sequence

| role    | node ids                                                                                                                                                                                  |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| other   | `ij1-roi-to-rgb`                                                                                                                                                                          |
| core-io | `io-interface`, `jipipe:compartment-output`                                                                                                                                               |
| other   | `cellpose-inference-2.x`, `cellpose-segmentation-inference-3.x`, `ij1-analyze-find-particles2d`, `ij1-contrast-calibrate`, `ij1-labels-to-roi`, `ij1-roi-filter-by-overlap`, … (11 total) |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip` (relative to `/data/JIPipe`),
compartment `File Handling+Glomeruli`.

| key | node id                        | name               |
|-----|--------------------------------|--------------------|
| n0  | `jipipe:compartment-output`    | Output             |
| n1  | `ij1-analyze-find-particles2d` | Find particles 2D  |
| n2  | `ij1-roi-to-rgb`               | Convert ROI to RGB |

| from              | slot  | to                 | slot  | data class   |
|-------------------|-------|--------------------|-------|--------------|
| Find particles 2D | `ROI` | Convert ROI to RGB | `ROI` | `imagej-roi` |

## Template

Proposed name `Convert 2D ROI to RGB fans in from 13 source ids (roles: core-io, other)`, menu path
`Patterns/ROIs/Convert 2D ROI to RGB fans in from 13 source ids (roles: core-io, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-ij1-roi-to-rgb-core-io-1-other-1.json`.

