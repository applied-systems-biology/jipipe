---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/LauraClement_07_3channels.jip"
    - "AIAgentTests/03-ExampleWorkflows/Manuel_11.jip"
    - "AIAgentTests/03-ExampleWorkflows/Mona_04.jip"
  projectCount: 26
  exampleGraph:
    compartment: "CellPose+File handler"
    project: "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map): grey"
      - "jipipe:compartment-output": "Output #1"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: green overlay"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Set LUT (color map): grey","fromSlot":"Output","to":"Output #1","toSlot":"TL stack","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Output #1","fromSlot":"Green","to":"Convert ROI to RGB: green overlay","toSlot":"Image","dataClass":"imagej-imgplus-2d-greyscale-16u"}
      - {"from":"Convert ROI to RGB: green overlay","fromSlot":"Output","to":"Output #2","toSlot":"Green overlay","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB -> Compartment output"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"446361e3-3e11-5ee7-8800-c10de3b969e2\",\"jipipe:node:name\":\"Set LUT (color map): grey\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"446361e3-3e11-5ee7-8800-c10de3b969e2\",\"jipipe:node:name\":\"Output #1\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"446361e3-3e11-5ee7-8800-c10de3b969e2\",\"jipipe:node:name\":\"Convert ROI to RGB: green overlay\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"446361e3-3e11-5ee7-8800-c10de3b969e2\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"TL stack\"},{\"source-node\":\"n1\",\"source-slot\":\"Green\",\"target-node\":\"n2\",\"target-slot\":\"Image\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Green overlay\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB -> Compartment output

Mined pattern `chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output`
(shape `chain`, support 26).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| other   | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output`  |
| other   | `ij1-roi-to-rgb`             |
| core-io | `jipipe:compartment-output`  |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip` (relative to `/data/JIPipe`),
compartment `CellPose+File handler`.

| key | node id                      | name                              |
|-----|------------------------------|-----------------------------------|
| n0  | `ij1-set-lut-from-color-map` | Set LUT (color map): grey         |
| n1  | `jipipe:compartment-output`  | Output #1                         |
| n2  | `ij1-roi-to-rgb`             | Convert ROI to RGB: green overlay |
| n3  | `jipipe:compartment-output`  | Output #2                         |

| from                              | slot     | to                                | slot            | data class                        |
|-----------------------------------|----------|-----------------------------------|-----------------|-----------------------------------|
| Set LUT (color map): grey         | `Output` | Output #1                         | `TL stack`      | `imagej-imgplus-greyscale`        |
| Output #1                         | `Green`  | Convert ROI to RGB: green overlay | `Image`         | `imagej-imgplus-2d-greyscale-16u` |
| Convert ROI to RGB: green overlay | `Output` | Output #2                         | `Green overlay` | `imagej-imgplus-color-rgb`        |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB -> Compartment output`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB -> Compartment output` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb-jipipe-compartment-output.json`.

