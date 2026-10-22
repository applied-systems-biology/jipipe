---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-mask -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Segmentation"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter 2D ROI by statistics: Filter ROI by area and circularity"
      - "jipipe:compartment-output": "ROIs"
      - "ij1-roi-to-rgb": "Convert ROI to RGB: TL overlay flows"
    edges:
      - {"from":"Filter 2D ROI by statistics: Filter ROI by area and circularity","fromSlot":"Output","to":"ROIs","toSlot":"Green spores","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics fans out to 3 target ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics fans out to 3 target ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"261e6763-eb66-5214-b16b-92788c4fa56c\",\"jipipe:node:name\":\"Filter 2D ROI by statistics: Filter ROI by area and circularity\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"261e6763-eb66-5214-b16b-92788c4fa56c\",\"jipipe:node:name\":\"ROIs\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"261e6763-eb66-5214-b16b-92788c4fa56c\",\"jipipe:node:name\":\"Convert ROI to RGB: TL overlay flows\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Green spores\"}]}"
---

# Pattern: Filter 2D ROI by statistics fans out to 3 target ids (roles: core-io, other)

Mined pattern `fan-out-ij1-roi-filter-statistics-core-io-1-other-1` (shape `fan-out`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-roi-filter-statistics` |
| core-io | `jipipe:compartment-output` |
| other | `ij1-roi-to-mask`, `ij1-roi-to-rgb` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `Segmentation`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-roi-filter-statistics` | Filter 2D ROI by statistics: Filter ROI by area and circularity |
| n1 | `jipipe:compartment-output` | ROIs |
| n2 | `ij1-roi-to-rgb` | Convert ROI to RGB: TL overlay flows |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Filter 2D ROI by statistics: Filter ROI by area and circularity | `Output` | ROIs | `Green spores` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by statistics fans out to 3 target ids (roles: core-io, other)`, menu path `Patterns/ROIs/Filter 2D ROI by statistics fans out to 3 target ids (roles: core-io, other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-out-ij1-roi-filter-statistics-core-io-1-other-1.json`.

