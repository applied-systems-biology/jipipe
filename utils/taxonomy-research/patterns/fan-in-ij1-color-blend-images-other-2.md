---
audience: "tbd"
modality: "imagej-imgplus -> imagej-imgplus-color-rgb -> filaments -> imagej-imgplus-greyscale-mask -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> imagej-roi -> imagej-imgplus-greyscale-8u"
tags: [Filaments, Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Mask"
    project: "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    nodes:
      - "filaments-convert-to-mask": "Convert filaments to mask"
      - "ij1-set-lut-from-color-map": "Set LUT (color map)"
      - "ij1-color-blend-images": "Blend images"
    edges:
      - {"from":"Set LUT (color map)","fromSlot":"Output","to":"Blend images","toSlot":"Magenta STED","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Blend images fans in from 13 source ids (roles: other)"
  menuPath: "Patterns/Images/Blend images fans in from 13 source ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"filaments-convert-to-mask\",\"jipipe:graph-compartment\":\"1949c634-97d8-5f26-a806-5359bb1dcbe5\",\"jipipe:node:name\":\"Convert filaments to mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-set-lut-from-color-map\",\"jipipe:graph-compartment\":\"1949c634-97d8-5f26-a806-5359bb1dcbe5\",\"jipipe:node:name\":\"Set LUT (color map)\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"1949c634-97d8-5f26-a806-5359bb1dcbe5\",\"jipipe:node:name\":\"Blend images\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Magenta STED\"}]}"
---

# Pattern: Blend images fans in from 13 source ids (roles: other)

Mined pattern `fan-in-ij1-color-blend-images-other-2` (shape `fan-in`, support 32).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-color-blend-images` |
| other | `filaments-convert-to-mask`, `ij1-blur-median2d`, `ij1-color-blend-images`, `ij1-contrast-illumination-correction2d`, `ij1-dimensions-reorder`, `ij1-math-divide-by-maximum`, … (13 total) |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` (relative to `/data/JIPipe`), compartment `Mask`.

| key | node id | name |
|---|---|---|
| n0 | `filaments-convert-to-mask` | Convert filaments to mask |
| n1 | `ij1-set-lut-from-color-map` | Set LUT (color map) |
| n2 | `ij1-color-blend-images` | Blend images |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Set LUT (color map) | `Output` | Blend images | `Magenta STED` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Blend images fans in from 13 source ids (roles: other)`, menu path `Patterns/Images/Blend images fans in from 13 source ids (roles: other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-color-blend-images-other-2.json`.

