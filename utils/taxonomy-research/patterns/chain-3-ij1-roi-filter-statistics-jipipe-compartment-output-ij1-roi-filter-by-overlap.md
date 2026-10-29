---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Apply Cellpose Model 2+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "jipipe:compartment-output": "Output"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap"
    edges:
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Output","toSlot":"ROIs","dataClass":"imagej-roi"}
      - {"from":"Output","fromSlot":"ROIs","to":"Filter ROI by overlap","toSlot":"ROI 2","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics -> Compartment output -> Filter 2D ROI by overlap (old)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics -> Compartment output -> Filter 2D ROI by overlap (old)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"bbf124be-5129-5349-8d11-f4f1f45bb3a6\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"bbf124be-5129-5349-8d11-f4f1f45bb3a6\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"bbf124be-5129-5349-8d11-f4f1f45bb3a6\",\"jipipe:node:name\":\"Filter ROI by overlap\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROIs\"},{\"source-node\":\"n1\",\"source-slot\":\"ROIs\",\"target-node\":\"n2\",\"target-slot\":\"ROI 2\"}]}"
---

# Pattern: Filter 2D ROI by statistics -> Compartment output -> Filter 2D ROI by overlap (old)

Mined pattern `chain-3-ij1-roi-filter-statistics-jipipe-compartment-output-ij1-roi-filter-by-overlap` (shape `chain`,
support 28).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| other   | `ij1-roi-filter-statistics` |
| core-io | `jipipe:compartment-output` |
| other   | `ij1-roi-filter-by-overlap` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Apply Cellpose Model 2+ROI management`.

| key | node id                     | name                     |
|-----|-----------------------------|--------------------------|
| n0  | `ij1-roi-filter-statistics` | Filter ROI by statistics |
| n1  | `jipipe:compartment-output` | Output                   |
| n2  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap    |

| from                     | slot     | to                    | slot    | data class   |
|--------------------------|----------|-----------------------|---------|--------------|
| Filter ROI by statistics | `Output` | Output                | `ROIs`  | `imagej-roi` |
| Output                   | `ROIs`   | Filter ROI by overlap | `ROI 2` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by statistics -> Compartment output -> Filter 2D ROI by overlap (old)`, menu path
`Patterns/ROIs/Filter 2D ROI by statistics -> Compartment output -> Filter 2D ROI by overlap (old)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-roi-filter-statistics-jipipe-compartment-output-ij1-roi-filter-by-overlap.json`.

