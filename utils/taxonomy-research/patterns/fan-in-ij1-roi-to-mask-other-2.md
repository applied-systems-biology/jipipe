---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-mask -> cellpose-model-v2 -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-results-table"
tags: [Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "ij1-roi-enlarge-shrink": "Enlarge/shrink 2D ROI"
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-to-mask": "Convert 2D ROI to mask"
    edges:
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"Probabilities","to":"Convert 2D ROI to mask","toSlot":"Image","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to mask fans in from 14 source ids (roles: other)"
  menuPath: "Patterns/ROIs/Convert 2D ROI to mask fans in from 14 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-enlarge-shrink\",\"jipipe:graph-compartment\":\"64ae6e91-7ab3-5af7-8abe-8c2737d0218a\",\"jipipe:node:name\":\"Enlarge/shrink 2D ROI\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"64ae6e91-7ab3-5af7-8abe-8c2737d0218a\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"64ae6e91-7ab3-5af7-8abe-8c2737d0218a\",\"jipipe:node:name\":\"Convert 2D ROI to mask\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Probabilities\",\"target-node\":\"n2\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Convert 2D ROI to mask fans in from 14 source ids (roles: other)

Mined pattern `fan-in-ij1-roi-to-mask-other-2` (shape `fan-in`, support 30).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-to-mask` |
| other | `cellpose-inference-2.x`, `cellpose-segmentation-inference-3.x`, `ij1-analyze-find-particles2d`, `ij1-blur-gaussian2d`, `ij1-color-split-channels-2`, `ij1-math-imagecalculator2d`, … (14 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-enlarge-shrink` | Enlarge/shrink 2D ROI |
| n1 | `cellpose-segmentation-inference-3.x` | Cellpose segmentation (3.x) |
| n2 | `ij1-roi-to-mask` | Convert 2D ROI to mask |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Cellpose segmentation (3.x) | `Probabilities` | Convert 2D ROI to mask | `Image` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Convert 2D ROI to mask fans in from 14 source ids (roles: other)`, menu path `Patterns/ROIs/Convert 2D ROI to mask fans in from 14 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-roi-to-mask-other-2.json`.

