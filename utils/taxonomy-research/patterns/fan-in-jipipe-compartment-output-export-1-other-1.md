---
audience: "tbd"
modality: "jipipe:data -> folder -> imagej-imgplus -> file -> imagej-results-table -> imagej-roi"
tags: [Export, Graph / Project, Images, ROIs, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 42
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 42
  exampleGraph:
    compartment: "Cartography"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "iji-export-image-v2": "Export image"
      - "ij1-roi-relation-2d": "Extract pairwise 2D ROI statistics"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Extract pairwise 2D ROI statistics","fromSlot":"Measurements","to":"Output","toSlot":"Cell to cell distances","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Compartment output fans in from 11 source ids (roles: export, other)"
  menuPath: "Patterns/Graph / Project/Compartment output fans in from 11 source ids (roles: export, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"iji-export-image-v2\",\"jipipe:graph-compartment\":\"50fc307f-3da6-5eb2-8f39-20743964f2b7\",\"jipipe:node:name\":\"Export image\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-relation-2d\",\"jipipe:graph-compartment\":\"50fc307f-3da6-5eb2-8f39-20743964f2b7\",\"jipipe:node:name\":\"Extract pairwise 2D ROI statistics\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"50fc307f-3da6-5eb2-8f39-20743964f2b7\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Measurements\",\"target-node\":\"n2\",\"target-slot\":\"Cell to cell distances\"}]}"
---

# Pattern: Compartment output fans in from 11 source ids (roles: export, other)

Mined pattern `fan-in-jipipe-compartment-output-export-1-other-1` (shape `fan-in`, support 42).

## Role sequence

| role    | node ids                                                                                                                                                                                                 |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| core-io | `jipipe:compartment-output`                                                                                                                                                                              |
| export  | `export-data-by-parameter-v2`, `iji-export-image-v2`, `iji-export-table-v2`                                                                                                                              |
| other   | `ij1-calibration-copy-physical-dimensions`, `ij1-calibration-draw-scale-bar`, `ij1-contrast-histogram-enhancer`, `ij1-roi-relation-2d`, `table-apply-expression-per-row-v2`, `table-filter`, … (8 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Cartography`.

| key | node id                     | name                               |
|-----|-----------------------------|------------------------------------|
| n0  | `iji-export-image-v2`       | Export image                       |
| n1  | `ij1-roi-relation-2d`       | Extract pairwise 2D ROI statistics |
| n2  | `jipipe:compartment-output` | Output                             |

| from                               | slot           | to     | slot                     | data class             |
|------------------------------------|----------------|--------|--------------------------|------------------------|
| Extract pairwise 2D ROI statistics | `Measurements` | Output | `Cell to cell distances` | `imagej-results-table` |

## Template

Proposed name `Compartment output fans in from 11 source ids (roles: export, other)`, menu path
`Patterns/Graph / Project/Compartment output fans in from 11 source ids (roles: export, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-jipipe-compartment-output-export-1-other-1.json`.

