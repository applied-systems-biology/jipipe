---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Florian_HolesInPolymer_CellPose_21.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Apply Cellpose Model 2+Quality check+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "jipipe:compartment-output": "Output #1"
      - "jipipe:compartment-output": "Output #2"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap"
    edges:
      - {"from":"Output #1","fromSlot":"ROIs","to":"Output #2","toSlot":"Model2 ROI","dataClass":"imagej-roi"}
      - {"from":"Output #2","fromSlot":"Merged ROIs","to":"Filter ROI by overlap","toSlot":"ROI 2","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Compartment output -> Compartment output -> Filter 2D ROI by overlap (old)"
  menuPath: "Patterns/Graph / Project/Compartment output -> Compartment output -> Filter 2D ROI by overlap (old)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a994671a-86e4-5e26-ad87-96e4194d1d06\",\"jipipe:node:name\":\"Output #1\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a994671a-86e4-5e26-ad87-96e4194d1d06\",\"jipipe:node:name\":\"Output #2\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"a994671a-86e4-5e26-ad87-96e4194d1d06\",\"jipipe:node:name\":\"Filter ROI by overlap\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROIs\",\"target-node\":\"n1\",\"target-slot\":\"Model2 ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Merged ROIs\",\"target-node\":\"n2\",\"target-slot\":\"ROI 2\"}]}"
---

# Pattern: Compartment output -> Compartment output -> Filter 2D ROI by overlap (old)

Mined pattern `chain-3-jipipe-compartment-output-jipipe-compartment-output-ij1-roi-filter-by-overlap` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-roi-filter-by-overlap` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 2+Quality check+ROI management`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:compartment-output` | Output #1 |
| n1 | `jipipe:compartment-output` | Output #2 |
| n2 | `ij1-roi-filter-by-overlap` | Filter ROI by overlap |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Output #1 | `ROIs` | Output #2 | `Model2 ROI` | `imagej-roi` |
| Output #2 | `Merged ROIs` | Filter ROI by overlap | `ROI 2` | `imagej-roi` |

## Template

Proposed name `Compartment output -> Compartment output -> Filter 2D ROI by overlap (old)`, menu path `Patterns/Graph / Project/Compartment output -> Compartment output -> Filter 2D ROI by overlap (old)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-jipipe-compartment-output-jipipe-compartment-output-ij1-roi-filter-by-overlap.json`.

