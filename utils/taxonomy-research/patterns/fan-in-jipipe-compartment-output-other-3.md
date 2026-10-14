---
audience: "tbd"
modality: "imagej-imgplus -> imagej-imgplus-color-rgb -> imagej-imgplus-greyscale-mask -> imagej-roi -> imagej-results-table -> imagej-imgplus-2d -> imagej-imgplus-greyscale -> imagej-imgplus-3d-greyscale -> imagej-imgplus-greyscale-8u -> roi-3d-list"
tags: [Graph / Project, Images, ROIs, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Postprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-labels-to-rgb": "Convert labels to RGB: resutls after seeded watershed"
      - "ij1-roi-to-rgb": "Convert ROI to RGB:  results after watershed"
      - "ij1-roi-from-mask": "Mask to ROI"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Mask to ROI","fromSlot":"Output","to":"Output","toSlot":"ROIs after watershed","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Compartment output fans in from 22 source ids (roles: other)"
  menuPath: "Patterns/Graph / Project/Compartment output fans in from 22 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-labels-to-rgb\",\"jipipe:graph-compartment\":\"b80ad27a-263e-510b-95c8-6fca28e07c91\",\"jipipe:node:name\":\"Convert labels to RGB: resutls after seeded watershed\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"b80ad27a-263e-510b-95c8-6fca28e07c91\",\"jipipe:node:name\":\"Convert ROI to RGB:  results after watershed\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-from-mask\",\"jipipe:graph-compartment\":\"b80ad27a-263e-510b-95c8-6fca28e07c91\",\"jipipe:node:name\":\"Mask to ROI\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b80ad27a-263e-510b-95c8-6fca28e07c91\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"ROIs after watershed\"}]}"
---

# Pattern: Compartment output fans in from 22 source ids (roles: other)

Mined pattern `fan-in-jipipe-compartment-output-other-3` (shape `fan-in`, support 28).

## Role sequence

| role | node ids |
|---|---|
| core-io | `jipipe:compartment-output` |
| other | `ij-convert-image-to-rgb-colors`, `ij1-analyze-find-particles2d`, `ij1-blur-median2d`, `ij1-calibration-copy-physical-dimensions`, `ij1-color-blend-images`, `ij1-contrast-histogram-enhancer`, … (22 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Postprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-labels-to-rgb` | Convert labels to RGB: resutls after seeded watershed |
| n1 | `ij1-roi-to-rgb` | Convert ROI to RGB:  results after watershed |
| n2 | `ij1-roi-from-mask` | Mask to ROI |
| n3 | `jipipe:compartment-output` | Output |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Mask to ROI | `Output` | Output | `ROIs after watershed` | `imagej-roi` |

## Template

Proposed name `Compartment output fans in from 22 source ids (roles: other)`, menu path `Patterns/Graph / Project/Compartment output fans in from 22 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-jipipe-compartment-output-other-3.json`.

