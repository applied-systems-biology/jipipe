---
audience: "tbd"
modality: "imagej-imgplus-greyscale-8u -> imagej-imgplus-greyscale-mask -> cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> filaments -> jipipe:data -> parameters"
tags: [Filaments, Graph / Project, Images, Machine learning, Miscellaneous]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Florian_HolesInPolymer_CellPose_21.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Segmentation and colocalization"
    project: "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    nodes:
      - "filaments-convert-to-mask": "Convert filaments to mask"
      - "cellpose-inference-2.x": "Cellpose prediction (2.x)"
      - "ij1-threshold-auto2d": "Auto threshold 2D"
    edges:
      - {"from":"Cellpose prediction (2.x)","fromSlot":"Probabilities","to":"Auto threshold 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Auto threshold 2D (8-bit) fans in from 6 source ids (roles: other)"
  menuPath: "Patterns/Images/Auto threshold 2D (8-bit) fans in from 6 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"filaments-convert-to-mask\",\"jipipe:graph-compartment\":\"c2a51425-7b77-54cc-9d42-c49f390fd601\",\"jipipe:node:name\":\"Convert filaments to mask\"},\"n1\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"c2a51425-7b77-54cc-9d42-c49f390fd601\",\"jipipe:node:name\":\"Cellpose prediction (2.x)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-threshold-auto2d\",\"jipipe:graph-compartment\":\"c2a51425-7b77-54cc-9d42-c49f390fd601\",\"jipipe:node:name\":\"Auto threshold 2D\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Probabilities\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Auto threshold 2D (8-bit) fans in from 6 source ids (roles: other)

Mined pattern `fan-in-ij1-threshold-auto2d-other-2` (shape `fan-in`, support 26).

## Role sequence

| role  | node ids                                                                                                                                                  |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| other | `ij1-threshold-auto2d`                                                                                                                                    |
| other | `cellpose-inference-2.x`, `filaments-convert-to-mask`, `ij1-background-rollingball2d`, `ij1-blur-gaussian3d`, `jipipe:comment`, `parameters-define-table` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` (relative to `/data/JIPipe`),
compartment `Segmentation and colocalization`.

| key | node id                     | name                      |
|-----|-----------------------------|---------------------------|
| n0  | `filaments-convert-to-mask` | Convert filaments to mask |
| n1  | `cellpose-inference-2.x`    | Cellpose prediction (2.x) |
| n2  | `ij1-threshold-auto2d`      | Auto threshold 2D         |

| from                      | slot            | to                | slot    | data class                 |
|---------------------------|-----------------|-------------------|---------|----------------------------|
| Cellpose prediction (2.x) | `Probabilities` | Auto threshold 2D | `Input` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Auto threshold 2D (8-bit) fans in from 6 source ids (roles: other)`, menu path
`Patterns/Images/Auto threshold 2D (8-bit) fans in from 6 source ids (roles: other)` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-ij1-threshold-auto2d-other-2.json`.

