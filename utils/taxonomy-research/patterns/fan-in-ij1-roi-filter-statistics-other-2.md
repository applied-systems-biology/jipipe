---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> cellpose-model-v2 -> imagej-imgplus-greyscale-mask -> imagej-results-table -> imagej-imgplus-2d -> imagej-imgplus-greyscale-8u -> jipipe:data"
tags: [Graph / Project, Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 37
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
  projectCount: 37
  exampleGraph:
    compartment: "Apply Cellpose Model 1"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "cellpose-inference-2.x": "Cellpose segmentation (2.x)"
      - "ij1-math-fast-image-arithmetics": "Fast image arithmetics 2D"
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
    edges:
      - {"from":"Fast image arithmetics 2D","fromSlot":"Output","to":"Filter ROI by statistics","toSlot":"Reference","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics fans in from 14 source ids (roles: other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics fans in from 14 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"b0a17450-848d-57af-aeff-59757687e272\",\"jipipe:node:name\":\"Cellpose segmentation (2.x)\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-math-fast-image-arithmetics\",\"jipipe:graph-compartment\":\"b0a17450-848d-57af-aeff-59757687e272\",\"jipipe:node:name\":\"Fast image arithmetics 2D\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"b0a17450-848d-57af-aeff-59757687e272\",\"jipipe:node:name\":\"Filter ROI by statistics\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Filter 2D ROI by statistics fans in from 14 source ids (roles: other)

Mined pattern `fan-in-ij1-roi-filter-statistics-other-2` (shape `fan-in`, support 37).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-filter-statistics` |
| other | `cellpose-2`, `cellpose-denoise-inference-3.x`, `cellpose-inference-2.x`, `cellpose-segmentation-inference-3.x`, `ij1-analyze-find-particles2d`, `ij1-background-rollingball2d`, … (14 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 1`.

| key | node id | name |
|---|---|---|
| n0 | `cellpose-inference-2.x` | Cellpose segmentation (2.x) |
| n1 | `ij1-math-fast-image-arithmetics` | Fast image arithmetics 2D |
| n2 | `ij1-roi-filter-statistics` | Filter ROI by statistics |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Fast image arithmetics 2D | `Output` | Filter ROI by statistics | `Reference` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Filter 2D ROI by statistics fans in from 14 source ids (roles: other)`, menu path `Patterns/ROIs/Filter 2D ROI by statistics fans in from 14 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-roi-filter-statistics-other-2.json`.

