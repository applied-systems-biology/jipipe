---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 34
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 34
  exampleGraph:
    compartment: "Colocalization"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij3d-roi-change-properties": "Set 3D ROI properties"
      - "ij3d-roi-convert-to-rgb": "Convert 3D ROI to RGB"
      - "ij1-calibration-copy-physical-dimensions": "Copy physical dimensions"
    edges:
      - {"from":"Set 3D ROI properties","fromSlot":"Output","to":"Convert 3D ROI to RGB","toSlot":"ROI","dataClass":"roi-3d-list"}
      - {"from":"Convert 3D ROI to RGB","fromSlot":"Output","to":"Copy physical dimensions","toSlot":"Target","dataClass":"imagej-imgplus-color-rgb"}
  templateCandidate: true
template:
  name: "Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions"
  menuPath: "Patterns/ROIs/Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-roi-change-properties\",\"jipipe:graph-compartment\":\"84eec347-1cbd-5f76-8c25-d2a6d2337e61\",\"jipipe:node:name\":\"Set 3D ROI properties\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-roi-convert-to-rgb\",\"jipipe:graph-compartment\":\"84eec347-1cbd-5f76-8c25-d2a6d2337e61\",\"jipipe:node:name\":\"Convert 3D ROI to RGB\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-calibration-copy-physical-dimensions\",\"jipipe:graph-compartment\":\"84eec347-1cbd-5f76-8c25-d2a6d2337e61\",\"jipipe:node:name\":\"Copy physical dimensions\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Target\"}]}"
---

# Pattern: Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions

Mined pattern `conversion-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-calibration-copy-physical-dimensions` (shape `conversion`, support 34).

## Role sequence

| role | node ids |
|---|---|
| other | `ij3d-roi-change-properties` |
| other | `ij3d-roi-convert-to-rgb` |
| other | `ij1-calibration-copy-physical-dimensions` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id | name |
|---|---|---|
| n0 | `ij3d-roi-change-properties` | Set 3D ROI properties |
| n1 | `ij3d-roi-convert-to-rgb` | Convert 3D ROI to RGB |
| n2 | `ij1-calibration-copy-physical-dimensions` | Copy physical dimensions |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Set 3D ROI properties | `Output` | Convert 3D ROI to RGB | `ROI` | `roi-3d-list` |
| Convert 3D ROI to RGB | `Output` | Copy physical dimensions | `Target` | `imagej-imgplus-color-rgb` |

## Template

Proposed name `Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions`, menu path `Patterns/ROIs/Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb-ij1-calibration-copy-physical-dimensions.json`.

