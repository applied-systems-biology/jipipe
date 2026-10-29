---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> roi-3d-list -> imagej-imgplus -> imagej-results-table"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Membrane+Particles+Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij1-set-lut-from-color-map": "Set LUT (color map): magenta"
      - "jipipe:compartment-output": "Output"
      - "jipipe:compartment-output": "Mask"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Set LUT (color map): magenta","fromSlot":"Output","to":"Output","toSlot":"Magenta raw","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Output","fromSlot":"Green trimmed","to":"Mask","toSlot":"Green trimmed","dataClass":"imagej-imgplus"}
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Extract ROI 3D statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Compartment output -> Extract IJ3D ROI statistics"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output -> Extract IJ3D ROI statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"5804e1de-b70f-557f-ae2f-17851c6dbf35\",\"jipipe:node:name\":\"Set LUT (color map): magenta\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"5804e1de-b70f-557f-ae2f-17851c6dbf35\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"5804e1de-b70f-557f-ae2f-17851c6dbf35\",\"jipipe:node:name\":\"Mask\"},\"n3\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"5804e1de-b70f-557f-ae2f-17851c6dbf35\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Magenta raw\"},{\"source-node\":\"n1\",\"source-slot\":\"Green trimmed\",\"target-node\":\"n2\",\"target-slot\":\"Green trimmed\"},{\"source-node\":\"n2\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n3\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Compartment output -> Extract IJ3D ROI statistics

Mined pattern
`chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output-ij3d-measure-roi3d` (shape
`chain`, support 26).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| other   | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output`  |
| core-io | `jipipe:compartment-output`  |
| other   | `ij3d-measure-roi3d`         |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles+Preprocessing`.

| key | node id                      | name                         |
|-----|------------------------------|------------------------------|
| n0  | `ij1-set-lut-from-color-map` | Set LUT (color map): magenta |
| n1  | `jipipe:compartment-output`  | Output                       |
| n2  | `jipipe:compartment-output`  | Mask                         |
| n3  | `ij3d-measure-roi3d`         | Extract ROI 3D statistics    |

| from                         | slot              | to                        | slot            | data class                 |
|------------------------------|-------------------|---------------------------|-----------------|----------------------------|
| Set LUT (color map): magenta | `Output`          | Output                    | `Magenta raw`   | `imagej-imgplus-greyscale` |
| Output                       | `Green trimmed`   | Mask                      | `Green trimmed` | `imagej-imgplus`           |
| Mask                         | `Magenta trimmed` | Extract ROI 3D statistics | `Reference`     | `imagej-imgplus`           |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Compartment output -> Extract IJ3D ROI statistics`, menu
path `Patterns/Images/Set LUT (color map) -> Compartment output -> Compartment output -> Extract IJ3D ROI statistics`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-ij1-set-lut-from-color-map-jipipe-compartment-output-jipipe-compartment-output-ij3d-measure-roi3d.json`.

