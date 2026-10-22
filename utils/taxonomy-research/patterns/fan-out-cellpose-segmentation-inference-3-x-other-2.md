---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask -> imagej-imgplus-color-rgb"
tags: [Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    nodes:
      - "cellpose-segmentation-inference-3.x": "Cellpose segmentation (3.x)"
      - "ij1-roi-to-mask": "Convert 2D ROI to mask"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB"
    edges:
      - {"from":"Cellpose segmentation (3.x)","fromSlot":"ROI","to":"Convert 2D ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (3.x) fans out to 5 target ids (roles: other)"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (3.x) fans out to 5 target ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"cellpose-segmentation-inference-3.x\",\"jipipe:graph-compartment\":\"e2b61611-4493-5e90-af1b-4c91b503bc7a\",\"jipipe:node:name\":\"Cellpose segmentation (3.x)\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"e2b61611-4493-5e90-af1b-4c91b503bc7a\",\"jipipe:node:name\":\"Convert 2D ROI to mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"e2b61611-4493-5e90-af1b-4c91b503bc7a\",\"jipipe:node:name\":\"Convert 2D ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Cellpose segmentation (3.x) fans out to 5 target ids (roles: other)

Mined pattern `fan-out-cellpose-segmentation-inference-3-x-other-2` (shape `fan-out`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `cellpose-segmentation-inference-3.x` |
| other | `cellpose-segmentation-inference-3.x`, `ij1-blur-gaussian2d`, `ij1-roi-filter-statistics`, `ij1-roi-to-mask`, `ij1-roi-to-rgb` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `cellpose-segmentation-inference-3.x` | Cellpose segmentation (3.x) |
| n1 | `ij1-roi-to-mask` | Convert 2D ROI to mask |
| n2 | `ij1-roi-to-rgb` | Convert 2D ROI to RGB |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Cellpose segmentation (3.x) | `ROI` | Convert 2D ROI to mask | `ROI` | `imagej-roi` |

## Template

Proposed name `Cellpose segmentation (3.x) fans out to 5 target ids (roles: other)`, menu path `Patterns/Machine learning/Cellpose segmentation (3.x) fans out to 5 target ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-out-cellpose-segmentation-inference-3-x-other-2.json`.

