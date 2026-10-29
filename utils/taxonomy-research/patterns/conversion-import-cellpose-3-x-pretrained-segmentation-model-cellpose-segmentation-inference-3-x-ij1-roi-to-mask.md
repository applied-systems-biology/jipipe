---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask"
tags: [Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "import-cellpose-3.x-pretrained-segmentation-model": "Pretrained Cellpose 3.x segmentation model"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-to-mask": "Convert 2D ROI to mask"
    edges:
      - {"from":"Pretrained Cellpose 3.x segmentation model","fromSlot":"Output","to":"Cellpose segmentation (3.x)","toSlot":"Model","dataClass":"cellpose-model-v2"}
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"Probabilities","to":"Convert 2D ROI to mask","toSlot":"Image","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask"
  menuPath: "Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-3.x-pretrained-segmentation-model\",\"jipipe:graph-compartment\":\"b38a44a4-ffa7-5456-9c9a-fe722fc7b285\",\"jipipe:node:name\":\"Pretrained Cellpose 3.x segmentation model\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"b38a44a4-ffa7-5456-9c9a-fe722fc7b285\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"b38a44a4-ffa7-5456-9c9a-fe722fc7b285\",\"jipipe:node:name\":\"Convert 2D ROI to mask\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Model\"},{\"source-node\":\"n1\",\"source-slot\":\"Probabilities\",\"target-node\":\"n2\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask

Mined pattern
`conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-mask`
(shape `conversion`, support 26).

## Role sequence

| role  | node ids                                            |
|-------|-----------------------------------------------------|
| other | `import-cellpose-3.x-pretrained-segmentation-model` |
| other | `cellpose-segmentation-inference-3.x`               |
| other | `ij1-roi-to-mask`                                   |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id                                             | name                                       |
|-----|-----------------------------------------------------|--------------------------------------------|
| n0  | `import-cellpose-3.x-pretrained-segmentation-model` | Pretrained Cellpose 3.x segmentation model |
| n1  | `cellpose-segmentation-inference-3.x`               | Cellpose segmentation (3.x)                |
| n2  | `ij1-roi-to-mask`                                   | Convert 2D ROI to mask                     |

| from                                       | slot            | to                          | slot    | data class                 |
|--------------------------------------------|-----------------|-----------------------------|---------|----------------------------|
| Pretrained Cellpose 3.x segmentation model | `Output`        | Cellpose segmentation (3.x) | `Model` | `cellpose-model-v2`        |
| Cellpose segmentation (3.x)                | `Probabilities` | Convert 2D ROI to mask      | `Image` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask`,
menu path
`Patterns/Machine learning/Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-import-cellpose-3-x-pretrained-segmentation-model-cellpose-segmentation-inference-3-x-ij1-roi-to-mask.json`.

