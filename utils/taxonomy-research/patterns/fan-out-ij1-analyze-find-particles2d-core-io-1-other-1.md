---
audience: "tbd"
modality: "imagej-imgplus-greyscale-mask -> imagej-roi -> imagej-results-table -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 37
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
  projectCount: 37
  exampleGraph:
    compartment: "Glomeruli"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    nodes:
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "jipipe:compartment-output": "Output"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
    edges:
      - {"from":"Find particles 2D","fromSlot":"Measurements","to":"Output","toSlot":"Measurements","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Find particles 2D fans out to 5 target ids (roles: core-io, other)"
  menuPath: "Patterns/Images/Find particles 2D fans out to 5 target ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"8e6edabf-244f-577c-b330-acdbcdcb9bee\",\"jipipe:node:name\":\"Find particles 2D\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"8e6edabf-244f-577c-b330-acdbcdcb9bee\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"8e6edabf-244f-577c-b330-acdbcdcb9bee\",\"jipipe:node:name\":\"Convert ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Measurements\",\"target-node\":\"n1\",\"target-slot\":\"Measurements\"}]}"
---

# Pattern: Find particles 2D fans out to 5 target ids (roles: core-io, other)

Mined pattern `fan-out-ij1-analyze-find-particles2d-core-io-1-other-1` (shape `fan-out`, support 37).

## Role sequence

| role    | node ids                                                                   |
|---------|----------------------------------------------------------------------------|
| other   | `ij1-analyze-find-particles2d`                                             |
| core-io | `io-interface`, `jipipe:compartment-output`                                |
| other   | `ij1-color-set-to-color`, `ij1-roi-to-mask-unreferenced`, `ij1-roi-to-rgb` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip` (relative to `/data/JIPipe`),
compartment `Glomeruli`.

| key | node id                        | name               |
|-----|--------------------------------|--------------------|
| n0  | `ij1-analyze-find-particles2d` | Find particles 2D  |
| n1  | `jipipe:compartment-output`    | Output             |
| n2  | `ij1-roi-to-rgb`               | Convert ROI to RGB |

| from              | slot           | to     | slot           | data class             |
|-------------------|----------------|--------|----------------|------------------------|
| Find particles 2D | `Measurements` | Output | `Measurements` | `imagej-results-table` |

## Template

Proposed name `Find particles 2D fans out to 5 target ids (roles: core-io, other)`, menu path
`Patterns/Images/Find particles 2D fans out to 5 target ids (roles: core-io, other)` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-ij1-analyze-find-particles2d-core-io-1-other-1.json`.

