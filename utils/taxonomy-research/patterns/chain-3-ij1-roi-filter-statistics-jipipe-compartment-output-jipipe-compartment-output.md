---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Apply Cellpose Model 2+ROI management"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "jipipe:compartment-output": "Output #1"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Output #1","toSlot":"ROIs","dataClass":"imagej-roi"}
      - {"from":"Output #1","fromSlot":"ROIs","to":"Output #2","toSlot":"Model2 ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter 2D ROI by statistics -> Compartment output -> Compartment output"
  menuPath: "Patterns/ROIs/Filter 2D ROI by statistics -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"69c14eda-aae3-51aa-9917-0b6366efbd58\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"69c14eda-aae3-51aa-9917-0b6366efbd58\",\"jipipe:node:name\":\"Output #1\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"69c14eda-aae3-51aa-9917-0b6366efbd58\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROIs\"},{\"source-node\":\"n1\",\"source-slot\":\"ROIs\",\"target-node\":\"n2\",\"target-slot\":\"Model2 ROI\"}]}"
---

# Pattern: Filter 2D ROI by statistics -> Compartment output -> Compartment output

Mined pattern `chain-3-ij1-roi-filter-statistics-jipipe-compartment-output-jipipe-compartment-output` (shape `chain`,
support 30).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| other   | `ij1-roi-filter-statistics` |
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Apply Cellpose Model 2+ROI management`.

| key | node id                     | name                     |
|-----|-----------------------------|--------------------------|
| n0  | `ij1-roi-filter-statistics` | Filter ROI by statistics |
| n1  | `jipipe:compartment-output` | Output #1                |
| n2  | `jipipe:compartment-output` | Output #2                |

| from                     | slot     | to        | slot         | data class   |
|--------------------------|----------|-----------|--------------|--------------|
| Filter ROI by statistics | `Output` | Output #1 | `ROIs`       | `imagej-roi` |
| Output #1                | `ROIs`   | Output #2 | `Model2 ROI` | `imagej-roi` |

## Template

Proposed name `Filter 2D ROI by statistics -> Compartment output -> Compartment output`, menu path
`Patterns/ROIs/Filter 2D ROI by statistics -> Compartment output -> Compartment output` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-roi-filter-statistics-jipipe-compartment-output-jipipe-compartment-output.json`.

