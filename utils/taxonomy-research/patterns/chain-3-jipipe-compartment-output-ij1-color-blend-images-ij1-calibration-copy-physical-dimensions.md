---
audience: "tbd"
modality: "imagej-imgplus -> imagej-imgplus-color-rgb"
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
      - "jipipe:compartment-output": "Mask"
      - "ij1-color-blend-images": "Blend images"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"Mask","fromSlot":"3D labels","to":"Blend images","toSlot":"Cell","dataClass":"imagej-imgplus"}
      - {"from":"Blend images","fromSlot":"Output","to":"Copy physical dimensions","toSlot":"Target","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Compartment output -> Blend images -> Copy physical dimensions"
  menuPath: "Patterns/Graph / Project/Compartment output -> Blend images -> Copy physical dimensions"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"602f89d9-fa29-5075-93dd-1fd6c4512761\",\"jipipe:node:name\":\"Mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"602f89d9-fa29-5075-93dd-1fd6c4512761\",\"jipipe:node:name\":\"Blend images\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"602f89d9-fa29-5075-93dd-1fd6c4512761\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"3D labels\",\"target-node\":\"n1\",\"target-slot\":\"Cell\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Target\"}]}"
---

# Pattern: Compartment output -> Blend images -> Copy physical dimensions

Mined pattern `chain-3-jipipe-compartment-output-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions` (shape
`chain`, support 28).

## Role sequence

| role    | node ids                                   |
|---------|--------------------------------------------|
| core-io | `jipipe:compartment-output`                |
| other   | `ij1-color-blend-images`                   |
| other   | `ij1-calibration-copy-physical-dimensions` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                                    | name                     |
|-----|--------------------------------------------|--------------------------|
| n0  | `jipipe:compartment-output`                | Mask                     |
| n1  | `ij1-color-blend-images`                   | Blend images             |
| n2  | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from         | slot        | to                       | slot     | data class                 |
|--------------|-------------|--------------------------|----------|----------------------------|
| Mask         | `3D labels` | Blend images             | `Cell`   | `imagej-imgplus`           |
| Blend images | `Output`    | Copy physical dimensions | `Target` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Compartment output -> Blend images -> Copy physical dimensions`, menu path
`Patterns/Graph / Project/Compartment output -> Blend images -> Copy physical dimensions` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions.json`.

