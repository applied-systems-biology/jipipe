---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-mask -> jipipe:data"
tags: [Annotations, Images, ROIs]
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
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "ij1-morph-operation2d": "Morphological operation 2D"
      - "annotate-with-data": "Annotate with data"
    edges:
      - {"from":"Convert ROI to mask","fromSlot":"Output","to":"Morphological operation 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Morphological operation 2D","fromSlot":"Output","to":"Annotate with data","toSlot":"Annotation","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data"
  menuPath: "Patterns/ROIs/Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"462dc98d-a2bd-5f9a-9d09-e4ed2ead4f20\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"462dc98d-a2bd-5f9a-9d09-e4ed2ead4f20\",\"jipipe:node:name\":\"Morphological operation 2D\"},\"n2\":{\"jipipe:node-info-id\":\"annotate-with-data\",\"jipipe:graph-compartment\":\"462dc98d-a2bd-5f9a-9d09-e4ed2ead4f20\",\"jipipe:node:name\":\"Annotate with data\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Annotation\"}]}"
---

# Pattern: Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data

Mined pattern `conversion-ij1-roi-to-mask-ij1-morph-operation2d-annotate-with-data` (shape `conversion`, support 26).

## Role sequence

| role     | node ids                |
|----------|-------------------------|
| other    | `ij1-roi-to-mask`       |
| other    | `ij1-morph-operation2d` |
| annotate | `annotate-with-data`    |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment
`Cellpose training`.

| key | node id                 | name                       |
|-----|-------------------------|----------------------------|
| n0  | `ij1-roi-to-mask`       | Convert ROI to mask        |
| n1  | `ij1-morph-operation2d` | Morphological operation 2D |
| n2  | `annotate-with-data`    | Annotate with data         |

| from                       | slot     | to                         | slot         | data class                      |
|----------------------------|----------|----------------------------|--------------|---------------------------------|
| Convert ROI to mask        | `Output` | Morphological operation 2D | `Input`      | `imagej-imgplus-greyscale-mask` |
| Morphological operation 2D | `Output` | Annotate with data         | `Annotation` | `imagej-imgplus`                |

## Template

Proposed name `Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data`, menu path
`Patterns/ROIs/Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-ij1-roi-to-mask-ij1-morph-operation2d-annotate-with-data.json`.

