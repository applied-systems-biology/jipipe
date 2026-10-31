---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Colocalization"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij3d-roi-convert-to-rgb": "Convert 3D ROI to RGB"
      - "ij1-color-blend-images": "Blend images"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"Convert 3D ROI to RGB","fromSlot":"Output","to":"Blend images","toSlot":"Magenta","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Convert IJ3D ROI to RGB fans out to 2 target ids (roles: other)"
  menuPath: "Patterns/ROIs/Convert IJ3D ROI to RGB fans out to 2 target ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-roi-convert-to-rgb\",\"jipipe:graph-compartment\":\"d74727c7-23a4-55cc-ba85-d9888e3789d5\",\"jipipe:node:name\":\"Convert 3D ROI to RGB\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"d74727c7-23a4-55cc-ba85-d9888e3789d5\",\"jipipe:node:name\":\"Blend images\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"d74727c7-23a4-55cc-ba85-d9888e3789d5\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Magenta\"}]}"
---

# Pattern: Convert IJ3D ROI to RGB fans out to 2 target ids (roles: other)

Mined pattern `fan-out-ij3d-roi-convert-to-rgb-other-2` (shape `fan-out`, support 30).

## Role sequence

| role  | node ids                                                             |
|-------|----------------------------------------------------------------------|
| other | `ij3d-roi-convert-to-rgb`                                            |
| other | `ij1-calibration-copy-physical-dimensions`, `ij1-color-blend-images` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id                                    | name                     |
|-----|--------------------------------------------|--------------------------|
| n0  | `ij3d-roi-convert-to-rgb`                  | Convert 3D ROI to RGB    |
| n1  | `ij1-color-blend-images`                   | Blend images             |
| n2  | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from                  | slot     | to           | slot      | data class                 |
|-----------------------|----------|--------------|-----------|----------------------------|
| Convert 3D ROI to RGB | `Output` | Blend images | `Magenta` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Convert IJ3D ROI to RGB fans out to 2 target ids (roles: other)`, menu path
`Patterns/ROIs/Convert IJ3D ROI to RGB fans out to 2 target ids (roles: other)` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-ij3d-roi-convert-to-rgb-other-2.json`.

