---
audience: "tbd"
modality: "imagej-imgplus -> jipipe:data"
tags: [Annotations, Graph / Project, Images]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Cellpose training"
    project: "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    nodes:
      - "ij1-morph-operation2d": "Morphological operation 2D"
      - "jipipe:compartment-output": "Output"
      - "annotate-with-data": "Annotate with data"
    edges:
      - {"from":"Morphological operation 2D","fromSlot":"Output","to":"Output","toSlot":"Mask","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Morphological operation 2D fans out to 2 target ids (roles: annotate, core-io)"
  menuPath: "Patterns/Images/Morphological operation 2D fans out to 2 target ids (roles: annotate, core-io)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"03487caa-0f94-5ec4-be03-3f31de500df1\",\"jipipe:node:name\":\"Morphological operation 2D\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"03487caa-0f94-5ec4-be03-3f31de500df1\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"annotate-with-data\",\"jipipe:graph-compartment\":\"03487caa-0f94-5ec4-be03-3f31de500df1\",\"jipipe:node:name\":\"Annotate with data\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Mask\"}]}"
---

# Pattern: Morphological operation 2D fans out to 2 target ids (roles: annotate, core-io)

Mined pattern `fan-out-ij1-morph-operation2d-annotate-1-core-io-1` (shape `fan-out`, support 26).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| other    | `ij1-morph-operation2d`     |
| annotate | `annotate-with-data`        |
| core-io  | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment
`Cellpose training`.

| key | node id                     | name                       |
|-----|-----------------------------|----------------------------|
| n0  | `ij1-morph-operation2d`     | Morphological operation 2D |
| n1  | `jipipe:compartment-output` | Output                     |
| n2  | `annotate-with-data`        | Annotate with data         |

| from                       | slot     | to     | slot   | data class       |
|----------------------------|----------|--------|--------|------------------|
| Morphological operation 2D | `Output` | Output | `Mask` | `imagej-imgplus` |

## Template

Proposed name `Morphological operation 2D fans out to 2 target ids (roles: annotate, core-io)`, menu path
`Patterns/Images/Morphological operation 2D fans out to 2 target ids (roles: annotate, core-io)` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-ij1-morph-operation2d-annotate-1-core-io-1.json`.

