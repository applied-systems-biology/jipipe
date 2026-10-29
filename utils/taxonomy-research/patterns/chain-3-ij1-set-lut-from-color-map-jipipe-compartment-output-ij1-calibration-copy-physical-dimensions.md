---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> imagej-imgplus"
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
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnMicrospheresInStaticCulture_Kerstin_16.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "jipipe:compartment-output": "Mask"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Mask","toSlot":"3D labels","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Mask","fromSlot":"Green trimmed","to":"Copy physical dimensions","toSlot":"Source","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Copy physical dimensions"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Copy physical dimensions"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"a81b0acd-f5e7-5b63-b370-d78451a7469a\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a81b0acd-f5e7-5b63-b370-d78451a7469a\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"a81b0acd-f5e7-5b63-b370-d78451a7469a\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"3D labels\"},{\"source-node\":\"n1\",\"source-slot\":\"Green trimmed\",\"target-node\":\"n2\",\"target-slot\":\"Source\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Copy physical dimensions

Mined pattern `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-calibration-copy-physical-dimensions`
(shape `chain`, support 30).

## Role sequence

| role    | node ids                                   |
|---------|--------------------------------------------|
| other   | `ij1-set-lut-from-color-map`               |
| core-io | `jipipe:compartment-output`                |
| other   | `ij1-calibration-copy-physical-dimensions` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                                    | name                     |
|-----|--------------------------------------------|--------------------------|
| n0  | `ij1-set-lut-from-color-map`               | Set LUT (color map)      |
| n1  | `jipipe:compartment-output`                | Mask                     |
| n2  | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from                | slot            | to                       | slot        | data class                 |
|---------------------|-----------------|--------------------------|-------------|----------------------------|
| Set LUT (color map) | `Output`        | Mask                     | `3D labels` | `imagej-imgplus-greyscale` |
| Mask                | `Green trimmed` | Copy physical dimensions | `Source`    | `imagej-imgplus`           |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Copy physical dimensions`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Copy physical dimensions` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-calibration-copy-physical-dimensions.json`.

