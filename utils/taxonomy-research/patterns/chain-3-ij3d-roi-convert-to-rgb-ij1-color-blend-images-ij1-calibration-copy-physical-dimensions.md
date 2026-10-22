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
      - {"from":"Blend images","fromSlot":"Output","to":"Copy physical dimensions","toSlot":"Target","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions"
  menuPath: "Patterns/ROIs/Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-roi-convert-to-rgb\",\"jipipe:graph-compartment\":\"6b5a7a7f-2e82-5afa-8dad-e38c187a634b\",\"jipipe:node:name\":\"Convert 3D ROI to RGB\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-color-blend-images\",\"jipipe:graph-compartment\":\"6b5a7a7f-2e82-5afa-8dad-e38c187a634b\",\"jipipe:node:name\":\"Blend images\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"6b5a7a7f-2e82-5afa-8dad-e38c187a634b\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Magenta\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Target\"}]}"
---

# Pattern: Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions

Mined pattern `chain-3-ij3d-roi-convert-to-rgb-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions` (shape `chain`, support 30).

## Role sequence

| role | node ids |
|---|---|
| other | `ij3d-roi-convert-to-rgb` |
| other | `ij1-color-blend-images` |
| other | `ij1-calibration-copy-physical-dimensions` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id | name |
|---|---|---|
| n0 | `ij3d-roi-convert-to-rgb` | Convert 3D ROI to RGB |
| n1 | `ij1-color-blend-images` | Blend images |
| n2 | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Convert 3D ROI to RGB | `Output` | Blend images | `Magenta` | `imagej-imgplus-color-rgb` |
| Blend images | `Output` | Copy physical dimensions | `Target` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions`, menu path `Patterns/ROIs/Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-ij3d-roi-convert-to-rgb-ij1-color-blend-images-ij1-calibration-copy-physical-dimensions.json`.

