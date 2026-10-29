---
audience: "tbd"
modality: "cellpose-model-v2 -> imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-imgplus-2d -> imagej-results-table -> imagej-imgplus-greyscale-mask -> imagej-imgplus-color-rgb -> imagej-imgplus-greyscale-8u -> roi-3d-list"
tags: [Graph / Project, Images, Machine learning, Miscellaneous, ROIs, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 2"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "ij1-math-imagecalculator2d": "Image calculator 2D"
      - "ij1-roi-statistics": "Extract ROI statistics"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Image calculator 2D","fromSlot":"Output","to":"Extract ROI statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
      - {"from":"Convert ROI to RGB: Cellpose results","fromSlot":"Output","to":"Output","toSlot":"Overlay","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Compartment output fans in from 23 source ids (roles: other)"
  menuPath: "Patterns/Graph / Project/Compartment output fans in from 23 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"4ca8e4b2-6768-5cea-8a08-df4cef9defa7\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-math-imagecalculator2d\",\"jipipe:graph-compartment\":\"4ca8e4b2-6768-5cea-8a08-df4cef9defa7\",\"jipipe:node:name\":\"Image calculator 2D\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-statistics\",\"jipipe:graph-compartment\":\"4ca8e4b2-6768-5cea-8a08-df4cef9defa7\",\"jipipe:node:name\":\"Extract ROI statistics\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"4ca8e4b2-6768-5cea-8a08-df4cef9defa7\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"},\"n4\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"4ca8e4b2-6768-5cea-8a08-df4cef9defa7\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"},{\"source-node\":\"n3\",\"source-slot\":\"Output\",\"target-node\":\"n4\",\"target-slot\":\"Overlay\"}]}"
---

# Pattern: Compartment output fans in from 23 source ids (roles: other)

Mined pattern `fan-in-jipipe-compartment-output-other-4` (shape `fan-in`, support 26).

## Role sequence

| role    | node ids                                                                                                                                                                                                                    |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| core-io | `jipipe:compartment-output`                                                                                                                                                                                                 |
| other   | `cellpose-denoise-inference-3.x`, `cellpose-segmentation-inference-3.x`, `cpython-script-iterating`, `ij-multi-template-matching`, `ij1-analyze-find-particles2d`, `ij1-calibration-copy-physical-dimensions`, … (23 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Apply Cellpose Model 2`.

| key | node id                      | name                                 |
|-----|------------------------------|--------------------------------------|
| n0  | `ij1-roi-filter-statistics`  | Filter ROI by statistics             |
| n1  | `ij1-math-imagecalculator2d` | Image calculator 2D                  |
| n2  | `ij1-roi-statistics`         | Extract ROI statistics               |
| n3  | `ij1-roi-to-rgb`             | Convert ROI to RGB: Cellpose results |
| n4  | `jipipe:compartment-output`  | Output                               |

| from                                 | slot     | to                     | slot        | data class                 |
|--------------------------------------|----------|------------------------|-------------|----------------------------|
| Image calculator 2D                  | `Output` | Extract ROI statistics | `Reference` | `imagej-imgplus`           |
| Convert ROI to RGB: Cellpose results | `Output` | Output                 | `Overlay`   | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Compartment output fans in from 23 source ids (roles: other)`, menu path
`Patterns/Graph / Project/Compartment output fans in from 23 source ids (roles: other)` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-jipipe-compartment-output-other-4.json`.

