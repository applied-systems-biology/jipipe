---
audience: "tbd"
modality: "imagej-imgplus -> imagej-imgplus-3d-greyscale-mask -> imagej-imgplus-greyscale -> imagej-imgplus-color-rgb -> roi-3d-list"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 36
  projects:
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 36
  exampleGraph:
    compartment: "Visualization"
    project: "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    nodes:
      - "io-interface": "IO Interface"
      - "ij1-color-blend-images": "Blend images"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"IO Interface","fromSlot":"RL","to":"Blend images","toSlot":"RL","dataClass":"imagej-imgplus"}
      - {"from":"Blend images","fromSlot":"Output","to":"Copy physical dimensions","toSlot":"Target","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Copy physical dimensions fans in from 7 source ids (roles: core-io, other)"
  menuPath: "Patterns/Images/Copy physical dimensions fans in from 7 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"io-interface\",\"jipipe:graph-compartment\":\"ebecbe9b-dbc0-5f99-9746-3355720aa44d\",\"jipipe:node:name\":\"IO Interface\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"ebecbe9b-dbc0-5f99-9746-3355720aa44d\",\"jipipe:node:name\":\"Blend images\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"ebecbe9b-dbc0-5f99-9746-3355720aa44d\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"RL\",\"target-node\":\"n1\",\"target-slot\":\"RL\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Target\"}]}"
---

# Pattern: Copy physical dimensions fans in from 7 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-calibration-copy-physical-dimensions-core-io-1-other-1` (shape `fan-in`, support 36).

## Role sequence

| role    | node ids                                                                                                                                            |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| other   | `ij1-calibration-copy-physical-dimensions`                                                                                                          |
| core-io | `io-interface`, `jipipe:compartment-output`                                                                                                         |
| other   | `ij1-binary-connected-component-labeling-3d`, `ij1-color-blend-images`, `ij1-color-to-rgb`, `ij1-set-lut-from-color-map`, `ij3d-roi-convert-to-rgb` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip` (relative to `/data/JIPipe`), compartment
`Visualization`.

| key | node id                                    | name                     |
|-----|--------------------------------------------|--------------------------|
| n0  | `io-interface`                             | IO Interface             |
| n1  | `ij1-color-blend-images`                   | Blend images             |
| n2  | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from         | slot     | to                       | slot     | data class                 |
|--------------|----------|--------------------------|----------|----------------------------|
| IO Interface | `RL`     | Blend images             | `RL`     | `imagej-imgplus`           |
| Blend images | `Output` | Copy physical dimensions | `Target` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Copy physical dimensions fans in from 7 source ids (roles: core-io, other)`, menu path
`Patterns/Images/Copy physical dimensions fans in from 7 source ids (roles: core-io, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-ij1-calibration-copy-physical-dimensions-core-io-1-other-1.json`.

