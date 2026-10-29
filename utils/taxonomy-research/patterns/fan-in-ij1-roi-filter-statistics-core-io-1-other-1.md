---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-32f -> imagej-imgplus-greyscale -> cellpose-model-v2 -> imagej-imgplus-greyscale-mask -> imagej-results-table"
tags: [Graph / Project, Images, Machine learning, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 37
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Anna_Slavena_11.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
  projectCount: 37
  exampleGraph:
    compartment: "File Handling+Glomeruli"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip"
    nodes:
      - "jipipe:compartment-output": "Output"
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
    edges:
      - {"from":"Find particles 2D","fromSlot":"ROI","to":"Filter ROI by statistics","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics fans in from 10 source ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics fans in from 10 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"fdafb20b-2aed-578d-a29f-b81955233742\",\"jipipe:node:name\":\"Output\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"fdafb20b-2aed-578d-a29f-b81955233742\",\"jipipe:node:name\":\"Find particles 2D\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"fdafb20b-2aed-578d-a29f-b81955233742\",\"jipipe:node:name\":\"Filter ROI by statistics\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Filter 2D ROI by statistics fans in from 10 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-roi-filter-statistics-core-io-1-other-1` (shape `fan-in`, support 37).

## Role sequence

| role    | node ids                                                                                                                                                                    |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| other   | `ij1-roi-filter-statistics`                                                                                                                                                 |
| core-io | `jipipe:compartment-output`                                                                                                                                                 |
| other   | `cellpose-2`, `cellpose-inference-2.x`, `cellpose-segmentation-inference-3.x`, `ij1-analyze-find-particles2d`, `ij1-math-divide-by-maximum`, `ij1-roi-combine`, … (9 total) |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip` (relative to
`/data/JIPipe`), compartment `File Handling+Glomeruli`.

| key | node id                        | name                     |
|-----|--------------------------------|--------------------------|
| n0  | `jipipe:compartment-output`    | Output                   |
| n1  | `ij1-analyze-find-particles2d` | Find particles 2D        |
| n2  | `ij1-roi-filter-statistics`    | Filter ROI by statistics |

| from              | slot  | to                       | slot  | data class   |
|-------------------|-------|--------------------------|-------|--------------|
| Find particles 2D | `ROI` | Filter ROI by statistics | `ROI` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by statistics fans in from 10 source ids (roles: core-io, other)`, menu path
`Patterns/ROIs/Filter 2D ROI by statistics fans in from 10 source ids (roles: core-io, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-ij1-roi-filter-statistics-core-io-1-other-1.json`.

