---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale -> imagej-imgplus-greyscale-mask -> imagej-results-table -> imagej-imgplus-greyscale-32f -> imagej-imgplus-color-rgb"
tags: [Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Anna_Slavena_11.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Franzi_Lara_06.jip"
  projectCount: 26
  exampleGraph:
    compartment: "New green "
    project: "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter 2D ROI by statistics"
      - "ij1-roi-statistics": "Extract 2D ROI statistics"
      - "ij1-roi-to-rgb": "Convert 2D ROI to RGB"
    edges:
      - {"from":"Filter 2D ROI by statistics","fromSlot":"Output","to":"Extract 2D ROI statistics","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics fans out to 5 target ids (roles: other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics fans out to 5 target ids (roles: other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"c09838e7-0f9c-5870-a593-bd9239fbc9c6\",\"jipipe:node:name\":\"Filter 2D ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-statistics\",\"jipipe:graph-compartment\":\"c09838e7-0f9c-5870-a593-bd9239fbc9c6\",\"jipipe:node:name\":\"Extract 2D ROI statistics\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"c09838e7-0f9c-5870-a593-bd9239fbc9c6\",\"jipipe:node:name\":\"Convert 2D ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Filter 2D ROI by statistics fans out to 5 target ids (roles: other)

Mined pattern `fan-out-ij1-roi-filter-statistics-other-2` (shape `fan-out`, support 26).

## Role sequence

| role  | node ids                                                                                                                                 |
|-------|------------------------------------------------------------------------------------------------------------------------------------------|
| other | `ij1-roi-filter-statistics`                                                                                                              |
| other | `ij1-colocalization-coloc2`, `ij1-roi-set-properties-from-expressions`, `ij1-roi-statistics`, `ij1-roi-to-labels-name`, `ij1-roi-to-rgb` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip` (relative to `/data/JIPipe`), compartment
`New green `.

| key | node id                     | name                        |
|-----|-----------------------------|-----------------------------|
| n0  | `ij1-roi-filter-statistics` | Filter 2D ROI by statistics |
| n1  | `ij1-roi-statistics`        | Extract 2D ROI statistics   |
| n2  | `ij1-roi-to-rgb`            | Convert 2D ROI to RGB       |

| from                        | slot     | to                        | slot  | data class   |
|-----------------------------|----------|---------------------------|-------|--------------|
| Filter 2D ROI by statistics | `Output` | Extract 2D ROI statistics | `ROI` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by statistics fans out to 5 target ids (roles: other)`, menu path
`Patterns/ROIs/Filter 2D ROI by statistics fans out to 5 target ids (roles: other)` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-ij1-roi-filter-statistics-other-2.json`.

