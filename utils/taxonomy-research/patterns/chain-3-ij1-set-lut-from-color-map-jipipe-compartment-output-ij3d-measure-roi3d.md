---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> roi-3d-list -> imagej-imgplus -> imagej-results-table"
tags: [Graph / Project, Images, ROIs]
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
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Mask","toSlot":"3D labels","dataClass":"imagej-imgplus-greyscale"}
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Extract ROI 3D statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Set LUT (color map) -> Compartment output -> Extract IJ3D ROI statistics"
  menuPath: "Patterns/Images/Set LUT (color map) -> Compartment output -> Extract IJ3D ROI statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"61914bd8-4113-5f2c-a014-c8270a0020ce\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"61914bd8-4113-5f2c-a014-c8270a0020ce\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"61914bd8-4113-5f2c-a014-c8270a0020ce\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"3D labels\"},{\"source-node\":\"n1\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Set LUT (color map) -> Compartment output -> Extract IJ3D ROI statistics

Mined pattern `chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij3d-measure-roi3d` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-set-lut-from-color-map` |
| core-io | `jipipe:compartment-output` |
| other | `ij3d-measure-roi3d` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Membrane+Particles`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-set-lut-from-color-map` | Set LUT (color map) |
| n1 | `jipipe:compartment-output` | Mask |
| n2 | `ij3d-measure-roi3d` | Extract ROI 3D statistics |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Set LUT (color map) | `Output` | Mask | `3D labels` | `imagej-imgplus-greyscale` |
| Mask | `Magenta trimmed` | Extract ROI 3D statistics | `Reference` | `imagej-imgplus` |

## Template

Proposed name `Set LUT (color map) -> Compartment output -> Extract IJ3D ROI statistics`, menu path `Patterns/Images/Set LUT (color map) -> Compartment output -> Extract IJ3D ROI statistics` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-ij1-set-lut-from-color-map-jipipe-compartment-output-ij3d-measure-roi3d.json`.

