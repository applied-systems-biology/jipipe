---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-color-rgb"
tags: [Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 1"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "cellpose-inference-2.x": "Cellpose segmentation (2.x)"
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
    edges:
      - {"from":"Cellpose segmentation (2.x)","fromSlot":"ROI","to":"Filter ROI by statistics","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Convert ROI to RGB: Cellpose results","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"d7dd3d45-d5d2-5df8-92ae-48ed195bc0fc\",\"jipipe:node:name\":\"Cellpose segmentation (2.x)\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"d7dd3d45-d5d2-5df8-92ae-48ed195bc0fc\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"d7dd3d45-d5d2-5df8-92ae-48ed195bc0fc\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB

Mined pattern `chain-3-cellpose-inference-2-x-ij1-roi-filter-statistics-ij1-roi-to-rgb` (shape `chain`, support 26).

## Role sequence

| role  | node ids                    |
|-------|-----------------------------|
| other | `cellpose-inference-2.x`    |
| other | `ij1-roi-filter-statistics` |
| other | `ij1-roi-to-rgb`            |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Apply Cellpose Model 1`.

| key | node id                     | name                                 |
|-----|-----------------------------|--------------------------------------|
| n0  | `cellpose-inference-2.x`    | Cellpose segmentation (2.x)          |
| n1  | `ij1-roi-filter-statistics` | Filter ROI by statistics             |
| n2  | `ij1-roi-to-rgb`            | Convert ROI to RGB: Cellpose results |

| from                        | slot     | to                                   | slot  | data class   |
|-----------------------------|----------|--------------------------------------|-------|--------------|
| Cellpose segmentation (2.x) | `ROI`    | Filter ROI by statistics             | `ROI` | `imagej-roi` |
| Filter ROI by statistics    | `Output` | Convert ROI to RGB: Cellpose results | `ROI` | `imagej-roi` |

## Template

Proposed name `Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB`, menu path
`Patterns/Machine learning/Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB`
(proposal only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-cellpose-inference-2-x-ij1-roi-filter-statistics-ij1-roi-to-rgb.json`.

