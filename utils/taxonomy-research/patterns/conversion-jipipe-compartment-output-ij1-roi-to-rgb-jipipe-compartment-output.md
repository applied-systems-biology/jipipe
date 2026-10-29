---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 52
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
  projectCount: 52
  exampleGraph:
    compartment: "File Handling+Glomeruli"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    nodes:
      - "jipipe:compartment-output": "Output #1"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Output #1","fromSlot":"Raw","to":"Convert ROI to RGB","toSlot":"Image","dataClass":"imagej-imgplus"}
      - {"from":"Convert ROI to RGB","fromSlot":"Output","to":"Output #2","toSlot":"Visualization","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Compartment output -> Convert 2D ROI to RGB -> Compartment output"
  menuPath: "Patterns/Graph / Project/Compartment output -> Convert 2D ROI to RGB -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"6e77d68a-f63e-52df-9249-92b009044a17\",\"jipipe:node:name\":\"Output #1\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"6e77d68a-f63e-52df-9249-92b009044a17\",\"jipipe:node:name\":\"Convert ROI to RGB\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"6e77d68a-f63e-52df-9249-92b009044a17\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Raw\",\"target-node\":\"n1\",\"target-slot\":\"Image\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Visualization\"}]}"
---

# Pattern: Compartment output -> Convert 2D ROI to RGB -> Compartment output

Mined pattern `conversion-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output` (shape `conversion`,
support 52).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| core-io | `jipipe:compartment-output` |
| other   | `ij1-roi-to-rgb`            |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip` (relative to `/data/JIPipe`),
compartment `File Handling+Glomeruli`.

| key | node id                     | name               |
|-----|-----------------------------|--------------------|
| n0  | `jipipe:compartment-output` | Output #1          |
| n1  | `ij1-roi-to-rgb`            | Convert ROI to RGB |
| n2  | `jipipe:compartment-output` | Output #2          |

| from               | slot     | to                 | slot            | data class                 |
|--------------------|----------|--------------------|-----------------|----------------------------|
| Output #1          | `Raw`    | Convert ROI to RGB | `Image`         | `imagej-imgplus`           |
| Convert ROI to RGB | `Output` | Output #2          | `Visualization` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Compartment output -> Convert 2D ROI to RGB -> Compartment output`, menu path
`Patterns/Graph / Project/Compartment output -> Convert 2D ROI to RGB -> Compartment output` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output.json`.

