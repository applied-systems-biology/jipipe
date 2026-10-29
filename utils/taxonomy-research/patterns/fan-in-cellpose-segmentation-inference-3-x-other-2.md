---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> cellpose-size-model-v2 -> clij2-image -> jipipe:data -> file"
tags: [Images, Machine learning, Miscellaneous]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 38
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
  projectCount: 38
  exampleGraph:
    compartment: "Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "clij:plugin:net.haesleinhuepf.clij2.plugins.GaussianBlur2D": "CLIJ2 Gaussian Blur 2D"
      - "import-cellpose-3.x-pretrained-segmentation-model": "Pretrained Cellpose 3.x segmentation model"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
    edges:
      - {"from":"Pretrained Cellpose 3.x segmentation model","fromSlot":"Output","to":"Cellpose segmentation (3.x)","toSlot":"Model","dataClass":"cellpose-model-v2"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (3.x) fans in from 9 source ids (roles: other)"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (3.x) fans in from 9 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"clij:plugin:net.haesleinhuepf.clij2.plugins.GaussianBlur2D\",\"jipipe:graph-compartment\":\"fbf90eb3-dcf1-58b2-9c16-66b67513b56c\",\"jipipe:node:name\":\"CLIJ2 Gaussian Blur 2D\"},\"n1\":{\"jipipe:node-info-id\":\"import-cellpose-3.x-pretrained-segmentation-model\",\"jipipe:graph-compartment\":\"fbf90eb3-dcf1-58b2-9c16-66b67513b56c\",\"jipipe:node:name\":\"Pretrained Cellpose 3.x segmentation model\"},\"n2\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"fbf90eb3-dcf1-58b2-9c16-66b67513b56c\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Model\"}]}"
---

# Pattern: Cellpose segmentation (3.x) fans in from 9 source ids (roles: other)

Mined pattern `fan-in-cellpose-segmentation-inference-3-x-other-2` (shape `fan-in`, support 38).

## Role sequence

| role  | node ids                                                                                                                                                                                                                           |
|-------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| other | `cellpose-segmentation-inference-3.x`                                                                                                                                                                                              |
| other | `cellpose-denoise-inference-3.x`, `cellpose-segmentation-inference-3.x`, `cellpose-segmentation-training-3.x`, `clij:plugin:net.haesleinhuepf.clij2.plugins.GaussianBlur2D`, `distribute-data`, `ij1-blur-gaussian2d`, … (9 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id                                                      | name                                       |
|-----|--------------------------------------------------------------|--------------------------------------------|
| n0  | `clij:plugin:net.haesleinhuepf.clij2.plugins.GaussianBlur2D` | CLIJ2 Gaussian Blur 2D                     |
| n1  | `import-cellpose-3.x-pretrained-segmentation-model`          | Pretrained Cellpose 3.x segmentation model |
| n2  | `cellpose-segmentation-inference-3.x`                        | Cellpose segmentation (3.x)                |

| from                                       | slot     | to                          | slot    | data class          |
|--------------------------------------------|----------|-----------------------------|---------|---------------------|
| Pretrained Cellpose 3.x segmentation model | `Output` | Cellpose segmentation (3.x) | `Model` | `cellpose-model-v2` |

## Template

Proposed name `Cellpose segmentation (3.x) fans in from 9 source ids (roles: other)`, menu path
`Patterns/Machine learning/Cellpose segmentation (3.x) fans in from 9 source ids (roles: other)` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-cellpose-segmentation-inference-3-x-other-2.json`.

