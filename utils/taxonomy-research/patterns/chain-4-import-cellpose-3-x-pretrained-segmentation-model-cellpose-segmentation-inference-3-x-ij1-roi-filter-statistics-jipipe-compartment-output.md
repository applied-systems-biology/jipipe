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
      - "import-cellpose-3.x-pretrained-segmentation-model": "Pretrained Cellpose 3.x segmentation model"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-filter-statistics": "Filter 2D ROI by statistics"
      - "jipipe:compartment-output": "Masks and ROIs"
    edges:
      - {"from":"Pretrained Cellpose 3.x segmentation model","fromSlot":"Output","to":"Cellpose segmentation (3.x)","toSlot":"Model","dataClass":"cellpose-model-v2"}
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"ROI","to":"Filter 2D ROI by statistics","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Filter 2D ROI by statistics","fromSlot":"Output","to":"Masks and ROIs","toSlot":"Nucleus ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output"
  menuPath: "Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-3.x-pretrained-segmentation-model\",\"jipipe:graph-compartment\":\"9ee4362a-0af6-56f3-a63b-069868317760\",\"jipipe:node:name\":\"Pretrained Cellpose 3.x segmentation model\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"9ee4362a-0af6-56f3-a63b-069868317760\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"9ee4362a-0af6-56f3-a63b-069868317760\",\"jipipe:node:name\":\"Filter 2D ROI by statistics\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"9ee4362a-0af6-56f3-a63b-069868317760\",\"jipipe:node:name\":\"Masks and ROIs\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Model\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Nucleus ROI\"}]}"
---

# Pattern: Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output

Mined pattern
`chain-4-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics-jipipe-compartment-output`
(shape `chain`, support 28).

## Role sequence

| role    | node ids                                            |
|---------|-----------------------------------------------------|
| other   | `import-cellpose-3.x-pretrained-segmentation-model` |
| other   | `cellpose-segmentation-inference-3.x`               |
| other   | `ij1-roi-filter-statistics`                         |
| core-io | `jipipe:compartment-output`                         |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id                                             | name                                       |
|-----|-----------------------------------------------------|--------------------------------------------|
| n0  | `import-cellpose-3.x-pretrained-segmentation-model` | Pretrained Cellpose 3.x segmentation model |
| n1  | `cellpose-segmentation-inference-3.x`               | Cellpose segmentation (3.x)                |
| n2  | `ij1-roi-filter-statistics`                         | Filter 2D ROI by statistics                |
| n3  | `jipipe:compartment-output`                         | Masks and ROIs                             |

| from                                       | slot     | to                          | slot          | data class          |
|--------------------------------------------|----------|-----------------------------|---------------|---------------------|
| Pretrained Cellpose 3.x segmentation model | `Output` | Cellpose segmentation (3.x) | `Model`       | `cellpose-model-v2` |
| Cellpose segmentation (3.x)                | `ROI`    | Filter 2D ROI by statistics | `ROI`         | `imagej-roi`        |
| Filter 2D ROI by statistics                | `Output` | Masks and ROIs              | `Nucleus ROI` | `imagej-roi`        |

## Template

Proposed name
`Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output`,
menu path
`Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Compartment output`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-filter-statistics-jipipe-compartment-output.json`.

