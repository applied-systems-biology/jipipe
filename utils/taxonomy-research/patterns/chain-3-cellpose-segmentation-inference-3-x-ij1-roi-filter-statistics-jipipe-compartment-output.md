---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale"
tags: [Graph / Project, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-filter-statistics": "Filter 2D ROI by statistics"
      - "jipipe:compartment-output": "Masks and ROIs"
    edges:
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"ROI","to":"Filter 2D ROI by statistics","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Filter 2D ROI by statistics","fromSlot":"Output","to":"Masks and ROIs","toSlot":"Nucleus ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"3ab54ae9-20f1-5b57-913a-16c59a0c8354\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"3ab54ae9-20f1-5b57-913a-16c59a0c8354\",\"jipipe:node:name\":\"Filter 2D ROI by statistics\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"3ab54ae9-20f1-5b57-913a-16c59a0c8354\",\"jipipe:node:name\":\"Masks and ROIs\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Nucleus ROI\"}]}"
---

# Pattern: Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output

Mined pattern `chain-3-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics-jipipe-compartment-output` (shape
`chain`, support 28).

## Role sequence

| role    | node ids                              |
|---------|---------------------------------------|
| other   | `cellpose-segmentation-inference-3.x` |
| other   | `ij1-roi-filter-statistics`           |
| core-io | `jipipe:compartment-output`           |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id                               | name                        |
|-----|---------------------------------------|-----------------------------|
| n0  | `cellpose-segmentation-inference-3.x` | Cellpose segmentation (3.x) |
| n1  | `ij1-roi-filter-statistics`           | Filter 2D ROI by statistics |
| n2  | `jipipe:compartment-output`           | Masks and ROIs              |

| from                        | slot     | to                          | slot          | data class   |
|-----------------------------|----------|-----------------------------|---------------|--------------|
| Cellpose segmentation (3.x) | `ROI`    | Filter 2D ROI by statistics | `ROI`         | `imagej-roi` |
| Filter 2D ROI by statistics | `Output` | Masks and ROIs              | `Nucleus ROI` | `imagej-roi` |

## Template

Proposed name `Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output`, menu path
`Patterns/Machine learning/Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics-jipipe-compartment-output.json`.

