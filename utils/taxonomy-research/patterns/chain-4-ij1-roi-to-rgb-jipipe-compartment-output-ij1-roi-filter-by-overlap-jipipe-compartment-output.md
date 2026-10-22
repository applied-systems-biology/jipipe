---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Quality check+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
      - "jipipe:compartment-output": "Output #1"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Convert ROI to RGB","fromSlot":"Output","to":"Output #1","toSlot":"Merged ROI overlay","dataClass":"imagej-imgplus-color-rgb"}
      - {"from":"Output #1","fromSlot":"Merged ROIs","to":"Filter ROI by overlap","toSlot":"ROI 2","dataClass":"imagej-roi"}
      - {"from":"Filter ROI by overlap","fromSlot":"ROI 1","to":"Output #2","toSlot":"False negatives ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output"
  menuPath: "Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"4ced51c8-5c33-5507-9f03-1b655713b4a4\",\"jipipe:node:name\":\"Convert ROI to RGB\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"4ced51c8-5c33-5507-9f03-1b655713b4a4\",\"jipipe:node:name\":\"Output #1\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"4ced51c8-5c33-5507-9f03-1b655713b4a4\",\"jipipe:node:name\":\"Filter ROI by overlap\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"4ced51c8-5c33-5507-9f03-1b655713b4a4\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Merged ROI overlay\"},{\"source-node\":\"n1\",\"source-slot\":\"Merged ROIs\",\"target-node\":\"n2\",\"target-slot\":\"ROI 2\"},{\"source-node\":\"n2\",\"source-slot\":\"ROI 1\",\"target-node\":\"n3\",\"target-slot\":\"False negatives ROI\"}]}"
---

# Pattern: Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output

Mined pattern `chain-4-ij1-roi-to-rgb-jipipe-compartment-output-ij1-roi-filter-by-overlap-jipipe-compartment-output` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-to-rgb` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-roi-filter-by-overlap` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Quality check+ROI management`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-to-rgb` | Convert ROI to RGB |
| n1 | `jipipe:compartment-output` | Output #1 |
| n2 | `ij1-roi-filter-by-overlap` | Filter ROI by overlap |
| n3 | `jipipe:compartment-output` | Output #2 |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Convert ROI to RGB | `Output` | Output #1 | `Merged ROI overlay` | `imagej-imgplus-color-rgb` |
| Output #1 | `Merged ROIs` | Filter ROI by overlap | `ROI 2` | `imagej-roi` |
| Filter ROI by overlap | `ROI 1` | Output #2 | `False negatives ROI` | `imagej-roi` |

## Template

Proposed name `Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output`, menu path `Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-ij1-roi-to-rgb-jipipe-compartment-output-ij1-roi-filter-by-overlap-jipipe-compartment-output.json`.

