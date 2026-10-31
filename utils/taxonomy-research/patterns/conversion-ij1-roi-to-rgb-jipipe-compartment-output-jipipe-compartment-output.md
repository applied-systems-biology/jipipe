---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Florian_HolesInPolymer_CellPose_21.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 2+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
      - "jipipe:compartment-output": "Output #1"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Convert ROI to RGB: Cellpose results","fromSlot":"Output","to":"Output #1","toSlot":"Overlay","dataClass":"imagej-imgplus-color-rgb"}
      - {"from":"Output #1","fromSlot":"ROIs","to":"Output #2","toSlot":"Model2 ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to RGB -> Compartment output -> Compartment output"
  menuPath: "Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"cfe30cfd-2011-52d6-92fa-535b9d2ffc51\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"cfe30cfd-2011-52d6-92fa-535b9d2ffc51\",\"jipipe:node:name\":\"Output #1\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"cfe30cfd-2011-52d6-92fa-535b9d2ffc51\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Overlay\"},{\"source-node\":\"n1\",\"source-slot\":\"ROIs\",\"target-node\":\"n2\",\"target-slot\":\"Model2 ROI\"}]}"
---

# Pattern: Convert 2D ROI to RGB -> Compartment output -> Compartment output

Mined pattern `conversion-ij1-roi-to-rgb-jipipe-compartment-output-jipipe-compartment-output` (shape `conversion`,
support 26).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| other   | `ij1-roi-to-rgb`            |
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Apply Cellpose Model 2+ROI management`.

| key | node id                     | name                                 |
|-----|-----------------------------|--------------------------------------|
| n0  | `ij1-roi-to-rgb`            | Convert ROI to RGB: Cellpose results |
| n1  | `jipipe:compartment-output` | Output #1                            |
| n2  | `jipipe:compartment-output` | Output #2                            |

| from                                 | slot     | to        | slot         | data class                 |
|--------------------------------------|----------|-----------|--------------|----------------------------|
| Convert ROI to RGB: Cellpose results | `Output` | Output #1 | `Overlay`    | `imagej-imgplus-color-rgb` |
| Output #1                            | `ROIs`   | Output #2 | `Model2 ROI` | `imagej-roi`               |

## Template

Proposed name `Convert 2D ROI to RGB -> Compartment output -> Compartment output`, menu path
`Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Compartment output` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-ij1-roi-to-rgb-jipipe-compartment-output-jipipe-compartment-output.json`.

