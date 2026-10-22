---
audience: "tbd"
modality: "imagej-imgplus-greyscale"
tags: [Graph / Project, Images]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Distances+Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "jipipe:compartment-output": "Mask"
      - "jipipe:compartment-output": "Output"
      - "jipipe:compartment-output": "Distances"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Mask","toSlot":"3D labels","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Mask","fromSlot":"3D cell mask","to":"Output","toSlot":"3D cell mask","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Output","fromSlot":"Green distances","to":"Distances","toSlot":"Green to green closest distances","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Compartment output -> Compartment output"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"b6771280-6aa3-580b-a615-5b2b8e378aac\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b6771280-6aa3-580b-a615-5b2b8e378aac\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b6771280-6aa3-580b-a615-5b2b8e378aac\",\"jipipe:node:name\":\"Output\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b6771280-6aa3-580b-a615-5b2b8e378aac\",\"jipipe:node:name\":\"Distances\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"3D labels\"},{\"source-node\":\"n1\",\"source-slot\":\"3D cell mask\",\"target-node\":\"n2\",\"target-slot\":\"3D cell mask\"},{\"source-node\":\"n2\",\"source-slot\":\"Green distances\",\"target-node\":\"n3\",\"target-slot\":\"Green to green closest distances\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Compartment output -> Compartment output

Mined pattern `chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output-jipipe-compartment-output` (shape `chain`, support 30).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Distances+Membrane+Particles`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-set-lut-from-color-map` | Set LUT (color map) |
| n1 | `jipipe:compartment-output` | Mask |
| n2 | `jipipe:compartment-output` | Output |
| n3 | `jipipe:compartment-output` | Distances |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Set LUT (color map) | `Output` | Mask | `3D labels` | `imagej-imgplus-greyscale` |
| Mask | `3D cell mask` | Output | `3D cell mask` | `imagej-imgplus-greyscale-mask` |
| Output | `Green distances` | Distances | `Green to green closest distances` | `imagej-results-table` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Compartment output -> Compartment output`, menu path `Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output-jipipe-compartment-output.json`.

