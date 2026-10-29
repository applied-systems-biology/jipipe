---
audience: "tbd"
modality: "imagej-imgplus-greyscale"
tags: [Graph / Project, Images]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 38
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 38
  exampleGraph:
    compartment: "File handler+Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map): label"
      - "jipipe:compartment-output": "Channels"
      - "jipipe:compartment-output": "Masks and ROIs"
    edges:
      - {"from":"Set LUT (color map): label","fromSlot":"Output","to":"Channels","toSlot":"Label","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Channels","fromSlot":"Label","to":"Masks and ROIs","toSlot":"Label","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Compartment output"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"8284b7e1-9a31-5b8d-a8c0-fc6a9704fde7\",\"jipipe:node:name\":\"Set LUT (color map): label\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"8284b7e1-9a31-5b8d-a8c0-fc6a9704fde7\",\"jipipe:node:name\":\"Channels\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"8284b7e1-9a31-5b8d-a8c0-fc6a9704fde7\",\"jipipe:node:name\":\"Masks and ROIs\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Label\"},{\"source-node\":\"n1\",\"source-slot\":\"Label\",\"target-node\":\"n2\",\"target-slot\":\"Label\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Compartment output

Mined pattern `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output` (shape `chain`,
support 38).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| other   | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output`  |
| core-io | `jipipe:compartment-output`  |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment
`File handler+Segmentation`.

| key | node id                      | name                       |
|-----|------------------------------|----------------------------|
| n0  | `ij1-set-lut-from-color-map` | Set LUT (color map): label |
| n1  | `jipipe:compartment-output`  | Channels                   |
| n2  | `jipipe:compartment-output`  | Masks and ROIs             |

| from                       | slot     | to             | slot    | data class                 |
|----------------------------|----------|----------------|---------|----------------------------|
| Set LUT (color map): label | `Output` | Channels       | `Label` | `imagej-imgplus-greyscale` |
| Channels                   | `Label`  | Masks and ROIs | `Label` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Compartment output`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output.json`.

