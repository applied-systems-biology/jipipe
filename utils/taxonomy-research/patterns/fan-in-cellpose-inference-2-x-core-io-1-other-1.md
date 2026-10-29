---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> file"
tags: [Graph / Project, Machine learning]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/FindPores_DL_09.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Nodules+Preprocessing and edges"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "import-cellpose-model-v2": "Import Cellpose model from file"
      - "jipipe:compartment-output": "Raw and masked images"
      - "cellpose-inference-2.x": "Cellpose prediction (2.x): 30, small nodules"
    edges:
      - {"from":"Raw and masked images","fromSlot":"Masked raw image","to":"Cellpose prediction (2.x): 30, small nodules","toSlot":"Input","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Cellpose segmentation (2.x) fans in from 3 source ids (roles: core-io, other)"
  menuPath: "Patterns/Machine learning/Cellpose segmentation (2.x) fans in from 3 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-cellpose-model-v2\",\"jipipe:graph-compartment\":\"d5608aa2-d5e3-5065-bf68-b45a982d3792\",\"jipipe:node:name\":\"Import Cellpose model from file\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"d5608aa2-d5e3-5065-bf68-b45a982d3792\",\"jipipe:node:name\":\"Raw and masked images\"},\"n2\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"d5608aa2-d5e3-5065-bf68-b45a982d3792\",\"jipipe:node:name\":\"Cellpose prediction (2.x): 30, small nodules\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Masked raw image\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Cellpose segmentation (2.x) fans in from 3 source ids (roles: core-io, other)

Mined pattern `fan-in-cellpose-inference-2-x-core-io-1-other-1` (shape `fan-in`, support 26).

## Role sequence

| role    | node ids                                                           |
|---------|--------------------------------------------------------------------|
| other   | `cellpose-inference-2.x`                                           |
| core-io | `jipipe:compartment-output`                                        |
| other   | `import-cellpose-2.x-pretrained-model`, `import-cellpose-model-v2` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Nodules+Preprocessing and edges`.

| key | node id                     | name                                         |
|-----|-----------------------------|----------------------------------------------|
| n0  | `import-cellpose-model-v2`  | Import Cellpose model from file              |
| n1  | `jipipe:compartment-output` | Raw and masked images                        |
| n2  | `cellpose-inference-2.x`    | Cellpose prediction (2.x): 30, small nodules |

| from                  | slot               | to                                           | slot    | data class       |
|-----------------------|--------------------|----------------------------------------------|---------|------------------|
| Raw and masked images | `Masked raw image` | Cellpose prediction (2.x): 30, small nodules | `Input` | `imagej-imgplus` |

## Template

Proposed name `Cellpose segmentation (2.x) fans in from 3 source ids (roles: core-io, other)`, menu path
`Patterns/Machine learning/Cellpose segmentation (2.x) fans in from 3 source ids (roles: core-io, other)` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-cellpose-inference-2-x-core-io-1-other-1.json`.

