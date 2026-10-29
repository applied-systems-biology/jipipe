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
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "jipipe:compartment-output": "Mask"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Mask","toSlot":"3D labels","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Mask","fromSlot":"3D cell mask","to":"Output","toSlot":"3D cell mask","dataClass":"imagej-imgplus-greyscale-mask"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Compartment output"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"0ee18a8b-fccc-521e-ad0b-b41fc237502a\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"0ee18a8b-fccc-521e-ad0b-b41fc237502a\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"0ee18a8b-fccc-521e-ad0b-b41fc237502a\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"3D labels\"},{\"source-node\":\"n1\",\"source-slot\":\"3D cell mask\",\"target-node\":\"n2\",\"target-slot\":\"3D cell mask\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Compartment output

Mined pattern `conversion-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output` (shape
`conversion`, support 30).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| other   | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output`  |
| core-io | `jipipe:compartment-output`  |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                      | name                |
|-----|------------------------------|---------------------|
| n0  | `ij1-set-lut-from-color-map` | Set LUT (color map) |
| n1  | `jipipe:compartment-output`  | Mask                |
| n2  | `jipipe:compartment-output`  | Output              |

| from                | slot           | to     | slot           | data class                      |
|---------------------|----------------|--------|----------------|---------------------------------|
| Set LUT (color map) | `Output`       | Mask   | `3D labels`    | `imagej-imgplus-greyscale`      |
| Mask                | `3D cell mask` | Output | `3D cell mask` | `imagej-imgplus-greyscale-mask` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Compartment output`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output.json`.

