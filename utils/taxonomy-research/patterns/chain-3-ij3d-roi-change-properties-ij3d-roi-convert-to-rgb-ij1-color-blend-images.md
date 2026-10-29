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
      - "ij3d-roi-change-properties": "Set 3D ROI properties"
      - "ij3d-roi-convert-to-rgb": "Convert 3D ROI to RGB"
      - "ij1-color-blend-images": "Blend images"
    edges:
      - {"from":"Set 3D ROI properties","fromSlot":"Output","to":"Convert 3D ROI to RGB","toSlot":"ROI","dataClass":"roi-3d-list"}
      - {"from":"Convert 3D ROI to RGB","fromSlot":"Output","to":"Blend images","toSlot":"Magenta","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images"
  menuPath: "Patterns/ROIs/Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-roi-change-properties\",\"jipipe:graph-compartment\":\"d75b1b5b-fed5-55cb-bfb3-9c461224e4f3\",\"jipipe:node:name\":\"Set 3D ROI properties\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-roi-convert-to-rgb\",\"jipipe:graph-compartment\":\"d75b1b5b-fed5-55cb-bfb3-9c461224e4f3\",\"jipipe:node:name\":\"Convert 3D ROI to RGB\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"d75b1b5b-fed5-55cb-bfb3-9c461224e4f3\",\"jipipe:node:name\":\"Blend images\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Magenta\"}]}"
---

# Pattern: Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images

Mined pattern `chain-3-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-color-blend-images` (shape `chain`,
support 30).

## Role sequence

| role  | node ids                     |
|-------|------------------------------|
| other | `ij3d-roi-change-properties` |
| other | `ij3d-roi-convert-to-rgb`    |
| other | `ij1-color-blend-images`     |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id                      | name                  |
|-----|------------------------------|-----------------------|
| n0  | `ij3d-roi-change-properties` | Set 3D ROI properties |
| n1  | `ij3d-roi-convert-to-rgb`    | Convert 3D ROI to RGB |
| n2  | `ij1-color-blend-images`     | Blend images          |

| from                  | slot     | to                    | slot      | data class                 |
|-----------------------|----------|-----------------------|-----------|----------------------------|
| Set 3D ROI properties | `Output` | Convert 3D ROI to RGB | `ROI`     | `roi-3d-list`              |
| Convert 3D ROI to RGB | `Output` | Blend images          | `Magenta` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images`, menu path
`Patterns/ROIs/Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-color-blend-images.json`.

