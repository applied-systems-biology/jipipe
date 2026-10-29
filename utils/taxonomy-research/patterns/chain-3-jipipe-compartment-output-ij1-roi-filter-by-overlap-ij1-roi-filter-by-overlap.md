---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
  projectCount: 28
  exampleGraph:
    compartment: "ROI processing+Segmentation"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "jipipe:compartment-output": "ROIs"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap #1"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap #2"
    edges:
      - {"from":"ROIs","fromSlot":"Blue spores","to":"Filter ROI by overlap #1","toSlot":"ROI 2","dataClass":"imagej-roi"}
      - {"from":"Filter ROI by overlap #1","fromSlot":"ROI 1","to":"Filter ROI by overlap #2","toSlot":"ROI 2","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Compartment output -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old)"
  menuPath: "Patterns/Graph / Project/Compartment output -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"1d717a23-f8c0-5179-9b0b-1bf06de911f5\",\"jipipe:node:name\":\"ROIs\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"1d717a23-f8c0-5179-9b0b-1bf06de911f5\",\"jipipe:node:name\":\"Filter ROI by overlap #1\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"1d717a23-f8c0-5179-9b0b-1bf06de911f5\",\"jipipe:node:name\":\"Filter ROI by overlap #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Blue spores\",\"target-node\":\"n1\",\"target-slot\":\"ROI 2\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI 1\",\"target-node\":\"n2\",\"target-slot\":\"ROI 2\"}]}"
---

# Pattern: Compartment output -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old)

Mined pattern `chain-3-jipipe-compartment-output-ij1-roi-filter-by-overlap-ij1-roi-filter-by-overlap` (shape `chain`,
support 28).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| core-io | `jipipe:compartment-output` |
| other   | `ij1-roi-filter-by-overlap` |
| other   | `ij1-roi-filter-by-overlap` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment
`ROI processing+Segmentation`.

| key | node id                     | name                     |
|-----|-----------------------------|--------------------------|
| n0  | `jipipe:compartment-output` | ROIs                     |
| n1  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap #1 |
| n2  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap #2 |

| from                     | slot          | to                       | slot    | data class   |
|--------------------------|---------------|--------------------------|---------|--------------|
| ROIs                     | `Blue spores` | Filter ROI by overlap #1 | `ROI 2` | `imagej-roi` |
| Filter ROI by overlap #1 | `ROI 1`       | Filter ROI by overlap #2 | `ROI 2` | `imagej-roi` |

## Template

Proposed name `Compartment output -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old)`, menu path
`Patterns/Graph / Project/Compartment output -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old)`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-ij1-roi-filter-by-overlap-ij1-roi-filter-by-overlap.json`.

