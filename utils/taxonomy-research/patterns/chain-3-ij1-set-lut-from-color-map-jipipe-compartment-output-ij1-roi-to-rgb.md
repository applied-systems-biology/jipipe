---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 42
  projects:
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 42
  exampleGraph:
    compartment: "New green +Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "jipipe:compartment-output": "Images and masks"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Images and masks","toSlot":"Normalized red","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Images and masks","fromSlot":"Raw green","to":"Convert 2D ROI to RGB","toSlot":"Image","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"d57c71b3-7af6-5d72-a9ac-2fead8d6873b\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"d57c71b3-7af6-5d72-a9ac-2fead8d6873b\",\"jipipe:node:name\":\"Images and masks\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"d57c71b3-7af6-5d72-a9ac-2fead8d6873b\",\"jipipe:node:name\":\"Convert 2D ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Normalized red\"},{\"source-node\":\"n1\",\"source-slot\":\"Raw green\",\"target-node\":\"n2\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB

Mined pattern `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb` (shape `chain`, support 42).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| other   | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output`  |
| other   | `ij1-roi-to-rgb`             |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip` (relative to `/data/JIPipe`), compartment
`New green +Preprocessing`.

| key | node id                      | name                  |
|-----|------------------------------|-----------------------|
| n0  | `ij1-set-lut-from-color-map` | Set LUT (color map)   |
| n1  | `jipipe:compartment-output`  | Images and masks      |
| n2  | `ij1-roi-to-rgb`             | Convert 2D ROI to RGB |

| from                | slot        | to                    | slot             | data class                 |
|---------------------|-------------|-----------------------|------------------|----------------------------|
| Set LUT (color map) | `Output`    | Images and masks      | `Normalized red` | `imagej-imgplus-greyscale` |
| Images and masks    | `Raw green` | Convert 2D ROI to RGB | `Image`          | `imagej-imgplus-greyscale` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB`, menu path
`Patterns/Images/Set LUT (color map) -> Compartment output -> Convert 2D ROI to RGB` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij1-roi-to-rgb.json`.

