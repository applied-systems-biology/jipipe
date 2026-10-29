---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-mask -> imagej-results-table"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 34
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 34
  exampleGraph:
    compartment: "ROI processing+Segmentation"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "jipipe:compartment-output": "ROIs"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap #1"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap #2"
    edges:
      - {"from":"ROIs","fromSlot":"Green spores","to":"Filter ROI by overlap #1","toSlot":"ROI 1","dataClass":"imagej-roi"}
      - {"from":"Filter ROI by overlap #1","fromSlot":"ROI 1","to":"Filter ROI by overlap #2","toSlot":"ROI 1","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by overlap (old) fans in from 7 source ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by overlap (old) fans in from 7 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"d71ba8f3-2d7e-5bff-ab56-50c34d67137f\",\"jipipe:node:name\":\"ROIs\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"d71ba8f3-2d7e-5bff-ab56-50c34d67137f\",\"jipipe:node:name\":\"Filter ROI by overlap #1\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"d71ba8f3-2d7e-5bff-ab56-50c34d67137f\",\"jipipe:node:name\":\"Filter ROI by overlap #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Green spores\",\"target-node\":\"n1\",\"target-slot\":\"ROI 1\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI 1\",\"target-node\":\"n2\",\"target-slot\":\"ROI 1\"}]}"
---

# Pattern: Filter 2D ROI by overlap (old) fans in from 7 source ids (roles: core-io, other)

Mined pattern `fan-in-ij1-roi-filter-by-overlap-core-io-1-other-1` (shape `fan-in`, support 34).

## Role sequence

| role    | node ids                                                                                                                                               |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| other   | `ij1-roi-filter-by-overlap`                                                                                                                            |
| core-io | `jipipe:compartment-output`                                                                                                                            |
| other   | `ij1-analyze-find-particles2d`, `ij1-roi-filter-by-overlap`, `ij1-roi-filter-statistics`, `ij1-roi-merge`, `ij1-roi-outline`, `ij1-roi-set-properties` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment
`ROI processing+Segmentation`.

| key | node id                     | name                     |
|-----|-----------------------------|--------------------------|
| n0  | `jipipe:compartment-output` | ROIs                     |
| n1  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap #1 |
| n2  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap #2 |

| from                     | slot           | to                       | slot    | data class   |
|--------------------------|----------------|--------------------------|---------|--------------|
| ROIs                     | `Green spores` | Filter ROI by overlap #1 | `ROI 1` | `imagej-roi` |
| Filter ROI by overlap #1 | `ROI 1`        | Filter ROI by overlap #2 | `ROI 1` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by overlap (old) fans in from 7 source ids (roles: core-io, other)`, menu path
`Patterns/ROIs/Filter 2D ROI by overlap (old) fans in from 7 source ids (roles: core-io, other)` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-ij1-roi-filter-by-overlap-core-io-1-other-1.json`.

