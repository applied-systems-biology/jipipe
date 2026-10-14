---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-results-table -> imagej-imgplus-2d -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Anna_Slavena_11.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 1+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-merge": "Merge ROI lists"
      - "jipipe:compartment-output": "Output"
      - "ij1-roi-statistics": "Extract ROI statistics"
    edges:
      - {"from":"Output","fromSlot":"Image with ridges","to":"Extract ROI statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Extract 2D ROI statistics fans in from 10 source ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Extract 2D ROI statistics fans in from 10 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-merge\",\"jipipe:graph-compartment\":\"ae39fa29-a991-5531-a00a-a6a1d71e5e84\",\"jipipe:node:name\":\"Merge ROI lists\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"ae39fa29-a991-5531-a00a-a6a1d71e5e84\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-statistics\",\"jipipe:graph-compartment\":\"ae39fa29-a991-5531-a00a-a6a1d71e5e84\",\"jipipe:node:name\":\"Extract ROI statistics\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Image with ridges\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Extract 2D ROI statistics fans in from 10 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-roi-statistics-core-io-1-other-1` (shape `fan-in`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-statistics` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-dimensions-stack-to-2d`, `ij1-labels-to-roi`, `ij1-math-fast-image-arithmetics`, `ij1-roi-filter-statistics`, `ij1-roi-from-mask`, `ij1-roi-merge`, … (9 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 1+ROI management`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-merge` | Merge ROI lists |
| n1 | `jipipe:compartment-output` | Output |
| n2 | `ij1-roi-statistics` | Extract ROI statistics |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Output | `Image with ridges` | Extract ROI statistics | `Reference` | `imagej-imgplus` |

## Template

Proposed name `Extract 2D ROI statistics fans in from 10 source ids (roles: core-io, other)`, menu path `Patterns/ROIs/Extract 2D ROI statistics fans in from 10 source ids (roles: core-io, other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-roi-statistics-core-io-1-other-1.json`.

