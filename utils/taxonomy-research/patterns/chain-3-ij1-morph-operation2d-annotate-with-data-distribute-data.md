---
audience: "tbd"
modality: "imagej-imgplus -> jipipe:data"
tags: [Annotations, Images, Miscellaneous]
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
      - "annotate-with-data": "Annotate with data"
      - "distribute-data": "Split data randomly (percentage)"
    edges:
      - {"from":"Morphological operation 2D","fromSlot":"Output","to":"Annotate with data","toSlot":"Annotation","dataClass":"imagej-imgplus"}
      - {"from":"Annotate with data","fromSlot":"Output","to":"Split data randomly (percentage)","toSlot":"Input","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)"
  menuPath: "Patterns/Images/Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"70bba66a-22c9-5604-9432-d578b034f6b4\",\"jipipe:node:name\":\"Morphological operation 2D\"},\"n1\":{\"jipipe:node-info-id\":\"annotate-with-data\",\"jipipe:graph-compartment\":\"70bba66a-22c9-5604-9432-d578b034f6b4\",\"jipipe:node:name\":\"Annotate with data\"},\"n2\":{\"jipipe:node-info-id\":\"distribute-data\",\"jipipe:graph-compartment\":\"70bba66a-22c9-5604-9432-d578b034f6b4\",\"jipipe:node:name\":\"Split data randomly (percentage)\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Annotation\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)

Mined pattern `chain-3-ij1-morph-operation2d-annotate-with-data-distribute-data` (shape `chain`, support 26).

## Role sequence

| role     | node ids                |
|----------|-------------------------|
| other    | `ij1-morph-operation2d` |
| annotate | `annotate-with-data`    |
| other    | `distribute-data`       |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment
`Cellpose training`.

| key | node id                 | name                             |
|-----|-------------------------|----------------------------------|
| n0  | `ij1-morph-operation2d` | Morphological operation 2D       |
| n1  | `annotate-with-data`    | Annotate with data               |
| n2  | `distribute-data`       | Split data randomly (percentage) |

| from                       | slot     | to                               | slot         | data class       |
|----------------------------|----------|----------------------------------|--------------|------------------|
| Morphological operation 2D | `Output` | Annotate with data               | `Annotation` | `imagej-imgplus` |
| Annotate with data         | `Output` | Split data randomly (percentage) | `Input`      | `jipipe:data`    |

## Template

Proposed name `Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)`, menu path
`Patterns/Images/Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)` (proposal only —
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-morph-operation2d-annotate-with-data-distribute-data.json`.

