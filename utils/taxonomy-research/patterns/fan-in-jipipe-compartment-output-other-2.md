---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> cellpose-model-v2 -> cellpose-model -> imagej-imgplus-greyscale-mask -> imagej-results-table -> imagej-imgplus-color-rgb -> roi-3d-list"
tags: [Graph / Project, Images, Machine learning, ROIs, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 44
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
  projectCount: 44
  exampleGraph:
    compartment: "Glomeruli"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    nodes:
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Find particles 2D","fromSlot":"Measurements","to":"Output","toSlot":"Measurements","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Compartment output fans in from 27 source ids (roles: other)"
  menuPath: "Patterns/Graph / Project/Compartment output fans in from 27 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"0fd4d6d7-c096-5138-a4b1-4d7b18860bc8\",\"jipipe:node:name\":\"Convert ROI to RGB\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"0fd4d6d7-c096-5138-a4b1-4d7b18860bc8\",\"jipipe:node:name\":\"Find particles 2D\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"0fd4d6d7-c096-5138-a4b1-4d7b18860bc8\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Measurements\",\"target-node\":\"n2\",\"target-slot\":\"Measurements\"}]}"
---

# Pattern: Compartment output fans in from 27 source ids (roles: other)

Mined pattern `fan-in-jipipe-compartment-output-other-2` (shape `fan-in`, support 44).

## Role sequence

| role    | node ids                                                                                                                                                                                             |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| core-io | `jipipe:compartment-output`                                                                                                                                                                          |
| other   | `cellpose-2`, `cellpose-segmentation-inference-3.x`, `cellpose-training-2`, `ij1-analyze-find-particles2d`, `ij1-analyze-image-statistics`, `ij1-calibration-copy-physical-dimensions`, … (27 total) |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip` (relative to `/data/JIPipe`),
compartment `Glomeruli`.

| key | node id                        | name               |
|-----|--------------------------------|--------------------|
| n0  | `ij1-roi-to-rgb`               | Convert ROI to RGB |
| n1  | `ij1-analyze-find-particles2d` | Find particles 2D  |
| n2  | `jipipe:compartment-output`    | Output             |

| from              | slot           | to     | slot           | data class             |
|-------------------|----------------|--------|----------------|------------------------|
| Find particles 2D | `Measurements` | Output | `Measurements` | `imagej-results-table` |

## Template

Proposed name `Compartment output fans in from 27 source ids (roles: other)`, menu path
`Patterns/Graph / Project/Compartment output fans in from 27 source ids (roles: other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-jipipe-compartment-output-other-2.json`.

