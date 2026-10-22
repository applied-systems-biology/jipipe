---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-results-table -> imagej-imgplus-greyscale-mask -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 2"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "ij1-roi-statistics": "Extract ROI statistics"
      - "jipipe:compartment-output": "Output"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
    edges:
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Extract ROI statistics","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Extract ROI statistics","fromSlot":"Measurements","to":"Output","toSlot":"ROI results","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics fans out to 6 target ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics fans out to 6 target ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"c5d9876d-24c2-5953-a702-536cba3c8423\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-statistics\",\"jipipe:graph-compartment\":\"c5d9876d-24c2-5953-a702-536cba3c8423\",\"jipipe:node:name\":\"Extract ROI statistics\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"c5d9876d-24c2-5953-a702-536cba3c8423\",\"jipipe:node:name\":\"Output\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"c5d9876d-24c2-5953-a702-536cba3c8423\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Measurements\",\"target-node\":\"n2\",\"target-slot\":\"ROI results\"}]}"
---

# Pattern: Filter 2D ROI by statistics fans out to 6 target ids (roles: core-io, other)

Mined pattern `fan-out-ij1-roi-filter-statistics-core-io-1-other-2` (shape `fan-out`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-filter-statistics` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-roi-enlarge-shrink`, `ij1-roi-merge`, `ij1-roi-statistics`, `ij1-roi-to-mask`, `ij1-roi-to-rgb` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 2`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-filter-statistics` | Filter ROI by statistics |
| n1 | `ij1-roi-statistics` | Extract ROI statistics |
| n2 | `jipipe:compartment-output` | Output |
| n3 | `ij1-roi-to-rgb` | Convert ROI to RGB: Cellpose results |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Filter ROI by statistics | `Output` | Extract ROI statistics | `ROI` | `imagej-roi` |
| Extract ROI statistics | `Measurements` | Output | `ROI results` | `imagej-results-table` |

## Template

Proposed name `Filter 2D ROI by statistics fans out to 6 target ids (roles: core-io, other)`, menu path `Patterns/ROIs/Filter 2D ROI by statistics fans out to 6 target ids (roles: core-io, other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-out-ij1-roi-filter-statistics-core-io-1-other-2.json`.

