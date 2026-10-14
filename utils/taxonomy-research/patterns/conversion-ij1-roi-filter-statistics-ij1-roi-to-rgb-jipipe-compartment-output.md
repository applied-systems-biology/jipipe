---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Apply Cellpose Model 1"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Convert ROI to RGB: Cellpose results","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Convert ROI to RGB: Cellpose results","fromSlot":"Output","to":"Output","toSlot":"Overlay","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Compartment output"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"5aa1e6cc-80db-518f-b819-d884298c5ae6\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"5aa1e6cc-80db-518f-b819-d884298c5ae6\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"5aa1e6cc-80db-518f-b819-d884298c5ae6\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Overlay\"}]}"
---

# Pattern: Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Compartment output

Mined pattern `conversion-ij1-roi-filter-statistics-ij1-roi-to-rgb-jipipe-compartment-output` (shape `conversion`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-filter-statistics` |
| other | `ij1-roi-to-rgb` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 1`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-filter-statistics` | Filter ROI by statistics |
| n1 | `ij1-roi-to-rgb` | Convert ROI to RGB: Cellpose results |
| n2 | `jipipe:compartment-output` | Output |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Filter ROI by statistics | `Output` | Convert ROI to RGB: Cellpose results | `ROI` | `imagej-roi` |
| Convert ROI to RGB: Cellpose results | `Output` | Output | `Overlay` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Compartment output`, menu path `Patterns/ROIs/Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-ij1-roi-filter-statistics-ij1-roi-to-rgb-jipipe-compartment-output.json`.

