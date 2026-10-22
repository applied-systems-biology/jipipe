---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Colocalization"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij3d-roi-filter-by-overlap": "Filter 3D ROI by overlap"
      - "ij3d-roi-change-properties": "Set 3D ROI properties"
      - "ij3d-roi-convert-to-rgb": "Convert 3D ROI to RGB"
    edges:
      - {"from":"Filter 3D ROI by overlap","fromSlot":"ROI 2","to":"Set 3D ROI properties","toSlot":"Input","dataClass":"roi-3d-list"}
      - {"from":"Set 3D ROI properties","fromSlot":"Output","to":"Convert 3D ROI to RGB","toSlot":"ROI","dataClass":"roi-3d-list"}
  templateCandidate: true
template:
  name: "Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB"
  menuPath: "Patterns/ROIs/Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"81b784a0-606e-513a-a096-58594661377d\",\"jipipe:node:name\":\"Filter 3D ROI by overlap\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-roi-change-properties\",\"jipipe:graph-compartment\":\"81b784a0-606e-513a-a096-58594661377d\",\"jipipe:node:name\":\"Set 3D ROI properties\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-roi-convert-to-rgb\",\"jipipe:graph-compartment\":\"81b784a0-606e-513a-a096-58594661377d\",\"jipipe:node:name\":\"Convert 3D ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI 2\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB

Mined pattern `chain-3-ij3d-roi-filter-by-overlap-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij3d-roi-filter-by-overlap` |
| other | `ij3d-roi-change-properties` |
| other | `ij3d-roi-convert-to-rgb` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id | name |
|---|---|---|
| n0 | `ij3d-roi-filter-by-overlap` | Filter 3D ROI by overlap |
| n1 | `ij3d-roi-change-properties` | Set 3D ROI properties |
| n2 | `ij3d-roi-convert-to-rgb` | Convert 3D ROI to RGB |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Filter 3D ROI by overlap | `ROI 2` | Set 3D ROI properties | `Input` | `roi-3d-list` |
| Set 3D ROI properties | `Output` | Convert 3D ROI to RGB | `ROI` | `roi-3d-list` |

## Template

Proposed name `Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB`, menu path `Patterns/ROIs/Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-ij3d-roi-filter-by-overlap-ij3d-roi-change-properties-ij3d-roi-convert-to-rgb.json`.

