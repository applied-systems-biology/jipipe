---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-color-rgb"
tags: [Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 34
  projects:
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
  projectCount: 34
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    nodes:
      - "import-cellpose-3.x-pretrained-segmentation-model": "Pretrained Cellpose 3.x segmentation model"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB"
    edges:
      - {"from":"Pretrained Cellpose 3.x segmentation model","fromSlot":"Output","to":"Cellpose segmentation (3.x)","toSlot":"Model","dataClass":"cellpose-model-v2"}
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"ROI","to":"Convert 2D ROI to RGB","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-3.x-pretrained-segmentation-model\",\"jipipe:graph-compartment\":\"ddf3f1c9-a711-5d9d-8ee8-fa08fd28e27d\",\"jipipe:node:name\":\"Pretrained Cellpose 3.x segmentation model\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"ddf3f1c9-a711-5d9d-8ee8-fa08fd28e27d\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"ddf3f1c9-a711-5d9d-8ee8-fa08fd28e27d\",\"jipipe:node:name\":\"Convert 2D ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Model\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB

Mined pattern
`conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-rgb` (shape
`conversion`, support 34).

## Role sequence

| role  | node ids                                            |
|-------|-----------------------------------------------------|
| other | `import-cellpose-3.x-pretrained-segmentation-model` |
| other | `cellpose-segmentation-inference-3.x`               |
| other | `ij1-roi-to-rgb`                                    |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                                             | name                                       |
|-----|-----------------------------------------------------|--------------------------------------------|
| n0  | `import-cellpose-3.x-pretrained-segmentation-model` | Pretrained Cellpose 3.x segmentation model |
| n1  | `cellpose-segmentation-inference-3.x`               | Cellpose segmentation (3.x)                |
| n2  | `ij1-roi-to-rgb`                                    | Convert 2D ROI to RGB                      |

| from                                       | slot     | to                          | slot    | data class          |
|--------------------------------------------|----------|-----------------------------|---------|---------------------|
| Pretrained Cellpose 3.x segmentation model | `Output` | Cellpose segmentation (3.x) | `Model` | `cellpose-model-v2` |
| Cellpose segmentation (3.x)                | `ROI`    | Convert 2D ROI to RGB       | `ROI`   | `imagej-roi`        |

## Template

Proposed name `Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB`, menu
path
`Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB`
(proposal only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-rgb.json`.

