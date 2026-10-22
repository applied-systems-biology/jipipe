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
    compartment: "Apply Cellpose Model 1+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-to-rgb": "Convert ROI to RGB: Cellpose results"
      - "jipipe:compartment-output": "Output"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap"
    edges:
      - {"from":"Convert ROI to RGB: Cellpose results","fromSlot":"Output","to":"Output","toSlot":"Ovedrlay combined ROIs","dataClass":"imagej-imgplus-color-rgb"}
      - {"from":"Output","fromSlot":"Image with ridges","to":"Filter ROI by overlap","toSlot":"Reference","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old)"
  menuPath: "Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"9d7092aa-46bc-51fd-a215-b693ebbd8862\",\"jipipe:node:name\":\"Convert ROI to RGB: Cellpose results\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"9d7092aa-46bc-51fd-a215-b693ebbd8862\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"9d7092aa-46bc-51fd-a215-b693ebbd8862\",\"jipipe:node:name\":\"Filter ROI by overlap\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Ovedrlay combined ROIs\"},{\"source-node\":\"n1\",\"source-slot\":\"Image with ridges\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old)

Mined pattern `chain-3-ij1-roi-to-rgb-jipipe-compartment-output-ij1-roi-filter-by-overlap` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-to-rgb` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-roi-filter-by-overlap` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Apply Cellpose Model 1+ROI management`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-to-rgb` | Convert ROI to RGB: Cellpose results |
| n1 | `jipipe:compartment-output` | Output |
| n2 | `ij1-roi-filter-by-overlap` | Filter ROI by overlap |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Convert ROI to RGB: Cellpose results | `Output` | Output | `Ovedrlay combined ROIs` | `imagej-imgplus-color-rgb` |
| Output | `Image with ridges` | Filter ROI by overlap | `Reference` | `imagej-imgplus` |

## Template

Proposed name `Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old)`, menu path `Patterns/ROIs/Convert 2D ROI to RGB -> Compartment output -> Filter 2D ROI by overlap (old)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-ij1-roi-to-rgb-jipipe-compartment-output-ij1-roi-filter-by-overlap.json`.

