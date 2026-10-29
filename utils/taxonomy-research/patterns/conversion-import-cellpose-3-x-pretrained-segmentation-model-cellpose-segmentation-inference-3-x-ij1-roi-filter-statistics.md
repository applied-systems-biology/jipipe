---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale"
tags: [Machine learning, ROIs]
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
      - "import-cellpose-3.x-pretrained-segmentation-model": "Pretrained Cellpose 3.x segmentation model"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-filter-statistics": "Filter 2D ROI by statistics"
    edges:
      - {"from":"Pretrained Cellpose 3.x segmentation model","fromSlot":"Output","to":"Cellpose segmentation (3.x)","toSlot":"Model","dataClass":"cellpose-model-v2"}
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"ROI","to":"Filter 2D ROI by statistics","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics"
  menuPath: "Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-3.x-pretrained-segmentation-model\",\"jipipe:graph-compartment\":\"0708ac1a-cc56-55d7-84ed-da2b73287ff0\",\"jipipe:node:name\":\"Pretrained Cellpose 3.x segmentation model\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"0708ac1a-cc56-55d7-84ed-da2b73287ff0\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"0708ac1a-cc56-55d7-84ed-da2b73287ff0\",\"jipipe:node:name\":\"Filter 2D ROI by statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Model\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics

Mined pattern
`conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics`
(shape `conversion`, support 28).

## Role sequence

| role  | node ids                                            |
|-------|-----------------------------------------------------|
| other | `import-cellpose-3.x-pretrained-segmentation-model` |
| other | `cellpose-segmentation-inference-3.x`               |
| other | `ij1-roi-filter-statistics`                         |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id                                             | name                                       |
|-----|-----------------------------------------------------|--------------------------------------------|
| n0  | `import-cellpose-3.x-pretrained-segmentation-model` | Pretrained Cellpose 3.x segmentation model |
| n1  | `cellpose-segmentation-inference-3.x`               | Cellpose segmentation (3.x)                |
| n2  | `ij1-roi-filter-statistics`                         | Filter 2D ROI by statistics                |

| from                                       | slot     | to                          | slot    | data class          |
|--------------------------------------------|----------|-----------------------------|---------|---------------------|
| Pretrained Cellpose 3.x segmentation model | `Output` | Cellpose segmentation (3.x) | `Model` | `cellpose-model-v2` |
| Cellpose segmentation (3.x)                | `ROI`    | Filter 2D ROI by statistics | `ROI`   | `imagej-roi`        |

## Template

Proposed name
`Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics`, menu path
`Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics.json`.

