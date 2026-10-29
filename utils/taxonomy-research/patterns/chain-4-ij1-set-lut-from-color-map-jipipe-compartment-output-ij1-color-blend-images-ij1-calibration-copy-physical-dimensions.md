---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [Graph / Project, Images]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnMicrospheresInStaticCulture_Kerstin_16.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "jipipe:compartment-output": "Mask"
      - "ij1-color-blend-images": "Blend images"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Mask","toSlot":"3D labels","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Mask","fromSlot":"3D labels","to":"Blend images","toSlot":"Cell","dataClass":"imagej-imgplus"}
      - {"from":"Blend images","fromSlot":"Output","to":"Copy physical dimensions","toSlot":"Target","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Blend images -> Copy physical dimensions"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Blend images -> Copy physical dimensions"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"47d507b4-8859-5319-9cfa-2a106693dfd7\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"47d507b4-8859-5319-9cfa-2a106693dfd7\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"47d507b4-8859-5319-9cfa-2a106693dfd7\",\"jipipe:node:name\":\"Blend images\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"47d507b4-8859-5319-9cfa-2a106693dfd7\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"3D labels\"},{\"source-node\":\"n1\",\"source-slot\":\"3D labels\",\"target-node\":\"n2\",\"target-slot\":\"Cell\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Target\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Blend images -> Copy physical dimensions

Mined pattern
`chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions`
(shape `chain`, support 28).

## Role sequence

| role    | node ids                                   |
|---------|--------------------------------------------|
| other   | `ij1-set-lut-from-color-map`               |
| core-io | `jipipe:compartment-output`                |
| other   | `ij1-color-blend-images`                   |
| other   | `ij1-calibration-copy-physical-dimensions` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                                    | name                     |
|-----|--------------------------------------------|--------------------------|
| n0  | `ij1-set-lut-from-color-map`               | Set LUT (color map)      |
| n1  | `jipipe:compartment-output`                | Mask                     |
| n2  | `ij1-color-blend-images`                   | Blend images             |
| n3  | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from                | slot        | to                       | slot        | data class                 |
|---------------------|-------------|--------------------------|-------------|----------------------------|
| Set LUT (color map) | `Output`    | Mask                     | `3D labels` | `imagej-imgplus-greyscale` |
| Mask                | `3D labels` | Blend images             | `Cell`      | `imagej-imgplus`           |
| Blend images        | `Output`    | Copy physical dimensions | `Target`    | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Blend images -> Copy physical dimensions`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Blend images -> Copy physical dimensions` (proposal only -
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions.json`.

