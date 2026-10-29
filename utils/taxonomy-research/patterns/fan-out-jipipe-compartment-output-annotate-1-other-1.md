---
audience: "tbd"
modality: "jipipe:data -> imagej-results-table -> cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask -> imagej-imgplus-color-rgb -> roi-3d-list"
tags: [Annotations, Graph / Project, Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Colocalization+Membrane+Z stack and colocalization"
    project: "AIAgentTests/03-ExampleWorkflows/Lukas_CFranke_09.jip"
    nodes:
      - "jipipe:compartment-output": "Raw images"
      - "overwrite-annotations": "Copy/overwrite annotations #1"
      - "overwrite-annotations": "Copy/overwrite annotations #2"
      - "cellpose-inference-2.x": "Cellpose prediction (2.x)"
    edges:
      - {"from":"Raw images","fromSlot":"Annotated with ROI","to":"Copy/overwrite annotations #1","toSlot":"Source","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Compartment output fans out to 8 target ids (roles: annotate, other)"
  menuPath: "Patterns/Graph / Project/Compartment output fans out to 8 target ids (roles: annotate, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"22bae9ed-ea52-5bfa-af16-9b4ca7e30a2f\",\"jipipe:node:name\":\"Raw images\"},\"n1\":{\"jipipe:node-info-id\":\"overwrite-annotations\",\"jipipe:graph-compartment\":\"22bae9ed-ea52-5bfa-af16-9b4ca7e30a2f\",\"jipipe:node:name\":\"Copy/overwrite annotations #1\"},\"n2\":{\"jipipe:node-info-id\":\"overwrite-annotations\",\"jipipe:graph-compartment\":\"22bae9ed-ea52-5bfa-af16-9b4ca7e30a2f\",\"jipipe:node:name\":\"Copy/overwrite annotations #2\"},\"n3\":{\"jipipe:node-info-id\":\"cellpose-inference-2.x\",\"jipipe:graph-compartment\":\"22bae9ed-ea52-5bfa-af16-9b4ca7e30a2f\",\"jipipe:node:name\":\"Cellpose prediction (2.x)\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated with ROI\",\"target-node\":\"n1\",\"target-slot\":\"Source\"}]}"
---

# Pattern: Compartment output fans out to 8 target ids (roles: annotate, other)

Mined pattern `fan-out-jipipe-compartment-output-annotate-1-other-1` (shape `fan-out`, support 28).

## Role sequence

| role     | node ids                                                                                                         |
|----------|------------------------------------------------------------------------------------------------------------------|
| core-io  | `jipipe:compartment-output`                                                                                      |
| annotate | `annotate-data-with-table-values`, `annotate-split-by-annotation`, `annotate-with-data`, `overwrite-annotations` |
| other    | `cellpose-inference-2.x`, `ij1-colocalization-coloc2`, `ij1-roi-to-rgb`, `ij3d-measure-roi3d`                    |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Lukas_CFranke_09.jip` (relative to `/data/JIPipe`), compartment
`Colocalization+Membrane+Z stack and colocalization`.

| key | node id                     | name                          |
|-----|-----------------------------|-------------------------------|
| n0  | `jipipe:compartment-output` | Raw images                    |
| n1  | `overwrite-annotations`     | Copy/overwrite annotations #1 |
| n2  | `overwrite-annotations`     | Copy/overwrite annotations #2 |
| n3  | `cellpose-inference-2.x`    | Cellpose prediction (2.x)     |

| from       | slot                 | to                            | slot     | data class    |
|------------|----------------------|-------------------------------|----------|---------------|
| Raw images | `Annotated with ROI` | Copy/overwrite annotations #1 | `Source` | `jipipe:data` |

## Template

Proposed name `Compartment output fans out to 8 target ids (roles: annotate, other)`, menu path
`Patterns/Graph / Project/Compartment output fans out to 8 target ids (roles: annotate, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-jipipe-compartment-output-annotate-1-other-1.json`.

