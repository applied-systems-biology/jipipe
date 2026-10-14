---
audience: "tbd"
modality: "imagej-imgplus -> imagej-imgplus-color-rgb -> imagej-imgplus-greyscale-mask -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-8u -> roi-3d-list"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 42
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 42
  exampleGraph:
    compartment: "Quantification+Segmentation"
    project: "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    nodes:
      - "jipipe:compartment-output": "Masks and ROIs"
      - "ij1-labels-dilate-labels": "Dilate labels"
      - "ij1-color-blend-images": "Blend images: Blend two images"
    edges:
      - {"from":"Dilate labels","fromSlot":"Output","to":"Blend images: Blend two images","toSlot":"Top","dataClass":"imagej-imgplus-greyscale"}
  templateCandidate: true
template:
  name: "Blend images fans in from 8 source ids (roles: core-io, other)"
  menuPath: "Patterns/Images/Blend images fans in from 8 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"e7d24479-afc6-5ea0-abab-6ed356d32b40\",\"jipipe:node:name\":\"Masks and ROIs\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-labels-dilate-labels\",\"jipipe:graph-compartment\":\"e7d24479-afc6-5ea0-abab-6ed356d32b40\",\"jipipe:node:name\":\"Dilate labels\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"e7d24479-afc6-5ea0-abab-6ed356d32b40\",\"jipipe:node:name\":\"Blend images: Blend two images\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Top\"}]}"
---

# Pattern: Blend images fans in from 8 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-color-blend-images-core-io-1-other-1` (shape `fan-in`, support 42).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-color-blend-images` |
| core-io | `io-interface`, `jipipe:compartment-output` |
| other | `ij1-binary-connected-component-labeling-2d`, `ij1-contrast-histogram-enhancer`, `ij1-labels-dilate-labels`, `ij1-set-lut-from-color-map`, `ij1-threshold-auto2d`, `ij3d-roi-convert-to-rgb` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Paco_06.jip` (relative to `/data/JIPipe`), compartment `Quantification+Segmentation`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:compartment-output` | Masks and ROIs |
| n1 | `ij1-labels-dilate-labels` | Dilate labels |
| n2 | `ij1-color-blend-images` | Blend images: Blend two images |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Dilate labels | `Output` | Blend images: Blend two images | `Top` | `imagej-imgplus-greyscale` |

## Template

Proposed name `Blend images fans in from 8 source ids (roles: core-io, other)`, menu path `Patterns/Images/Blend images fans in from 8 source ids (roles: core-io, other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij1-color-blend-images-core-io-1-other-1.json`.

