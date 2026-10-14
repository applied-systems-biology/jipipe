---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> cellpose-model-v2 -> clij2-image -> jipipe:data -> imagej-imgplus-greyscale-mask -> imagej-results-table"
tags: [Images, Machine learning, Miscellaneous, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 53
  projects:
    - "AIAgentTests/01-BasicWorkshop/lecture_03.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
  projectCount: 53
  exampleGraph:
    compartment: "Analysis"
    project: "AIAgentTests/01-BasicWorkshop/lecture_03.jip"
    nodes:
      - "ij1-analyze-find-particles2d": "Find particles 2D: Circ >= 0.7, no size limits"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB #1"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB #2"
    edges:
      - {"from":"Convert 2D ROI to RGB #1","fromSlot":"Output","to":"Convert 2D ROI to RGB #2","toSlot":"Image","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to RGB fans in from 27 source ids (roles: other)"
  menuPath: "Patterns/ROIs/Convert 2D ROI to RGB fans in from 27 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"538a6a7c-e617-52ba-b9b9-657e487f38f6\",\"jipipe:node:name\":\"Find particles 2D: Circ >= 0.7, no size limits\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"538a6a7c-e617-52ba-b9b9-657e487f38f6\",\"jipipe:node:name\":\"Convert 2D ROI to RGB #1\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"538a6a7c-e617-52ba-b9b9-657e487f38f6\",\"jipipe:node:name\":\"Convert 2D ROI to RGB #2\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Convert 2D ROI to RGB fans in from 27 source ids (roles: other)

Mined pattern `fan-in-ij1-roi-to-rgb-other-2` (shape `fan-in`, support 53).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-to-rgb` |
| other | `cellpose-2`, `cellpose-inference-2.x`, `cellpose-segmentation-inference-3.x`, `clij:plugin:net.haesleinhuepf.clij2.plugins.GaussianBlur2D`, `distribute-data`, `ij1-analyze-find-particles2d`, … (27 total) |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/lecture_03.jip` (relative to `/data/JIPipe`), compartment `Analysis`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-analyze-find-particles2d` | Find particles 2D: Circ >= 0.7, no size limits |
| n1 | `ij1-roi-to-rgb` | Convert 2D ROI to RGB #1 |
| n2 | `ij1-roi-to-rgb` | Convert 2D ROI to RGB #2 |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Convert 2D ROI to RGB #1 | `Output` | Convert 2D ROI to RGB #2 | `Image` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Convert 2D ROI to RGB fans in from 27 source ids (roles: other)`, menu path `Patterns/ROIs/Convert 2D ROI to RGB fans in from 27 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-roi-to-rgb-other-2.json`.

