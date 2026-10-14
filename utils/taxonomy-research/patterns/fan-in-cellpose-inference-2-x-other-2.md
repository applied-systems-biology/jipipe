---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> file"
tags: [Images, Machine learning]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Apply Cellpose Model 1"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "import-cellpose-model-v2": "Import Cellpose model from file"
      - "ij1-math-fast-image-arithmetics": "Fast image arithmetics 2D"
      - "cellpose-inference-2.x": "Cellpose segmentation (2.x)"
    edges:
      - {"from":"Fast image arithmetics 2D","fromSlot":"Output","to":"Cellpose segmentation (2.x)","toSlot":"Input","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (2.x) fans in from 8 source ids (roles: other)"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (2.x) fans in from 8 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-model-v2\",\"jipipe:graph-compartment\":\"81ed29ca-d487-55fc-985b-5dee00d242c6\",\"jipipe:node:name\":\"Import Cellpose model from file\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-math-fast-image-arithmetics\",\"jipipe:graph-compartment\":\"81ed29ca-d487-55fc-985b-5dee00d242c6\",\"jipipe:node:name\":\"Fast image arithmetics 2D\"},\"n2\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"81ed29ca-d487-55fc-985b-5dee00d242c6\",\"jipipe:node:name\":\"Cellpose segmentation (2.x)\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Cellpose segmentation (2.x) fans in from 8 source ids (roles: other)

Mined pattern `fan-in-cellpose-inference-2-x-other-2` (shape `fan-in`, support 32).

## Role sequence

| role | node ids |
|---|---|
| other | `cellpose-inference-2.x` |
| other | `cellpose-2`, `cellpose-inference-2.x`, `ij1-contrast-illumination-correction2d`, `ij1-dimensions-stacks-to-dimension`, `ij1-math-divide-by-maximum`, `ij1-math-fast-image-arithmetics`, … (8 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 1`.

| key | node id | name |
|---|---|---|
| n0 | `import-cellpose-model-v2` | Import Cellpose model from file |
| n1 | `ij1-math-fast-image-arithmetics` | Fast image arithmetics 2D |
| n2 | `cellpose-inference-2.x` | Cellpose segmentation (2.x) |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Fast image arithmetics 2D | `Output` | Cellpose segmentation (2.x) | `Input` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Cellpose segmentation (2.x) fans in from 8 source ids (roles: other)`, menu path `Patterns/Machine learning/Cellpose segmentation (2.x) fans in from 8 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-cellpose-inference-2-x-other-2.json`.

