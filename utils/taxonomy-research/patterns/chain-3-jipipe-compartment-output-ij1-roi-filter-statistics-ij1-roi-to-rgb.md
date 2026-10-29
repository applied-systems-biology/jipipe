---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-color-rgb"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Anna_Slavena_11.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Nodules+Preprocessing and edges"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "jipipe:compartment-output": "Raw and masked images"
      - "ij1-roi-filter-statistics": "Filter ROI by statistics"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
    edges:
      - {"from":"Raw and masked images","fromSlot":"Masked raw image","to":"Filter ROI by statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
      - {"from":"Filter ROI by statistics","fromSlot":"Output","to":"Convert ROI to RGB","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Compartment output -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Graph / Project/Compartment output -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"092bfba6-bcf2-5d8f-af69-01129901ded8\",\"jipipe:node:name\":\"Raw and masked images\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-statistics\",\"jipipe:graph-compartment\":\"092bfba6-bcf2-5d8f-af69-01129901ded8\",\"jipipe:node:name\":\"Filter ROI by statistics\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"092bfba6-bcf2-5d8f-af69-01129901ded8\",\"jipipe:node:name\":\"Convert ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Masked raw image\",\"target-node\":\"n1\",\"target-slot\":\"Reference\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Compartment output -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB

Mined pattern `chain-3-jipipe-compartment-output-ij1-roi-filter-statistics-ij1-roi-to-rgb` (shape `chain`, support 28).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| core-io | `jipipe:compartment-output` |
| other   | `ij1-roi-filter-statistics` |
| other   | `ij1-roi-to-rgb`            |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Nodules+Preprocessing and edges`.

| key | node id                     | name                     |
|-----|-----------------------------|--------------------------|
| n0  | `jipipe:compartment-output` | Raw and masked images    |
| n1  | `ij1-roi-filter-statistics` | Filter ROI by statistics |
| n2  | `ij1-roi-to-rgb`            | Convert ROI to RGB       |

| from                     | slot               | to                       | slot        | data class       |
|--------------------------|--------------------|--------------------------|-------------|------------------|
| Raw and masked images    | `Masked raw image` | Filter ROI by statistics | `Reference` | `imagej-imgplus` |
| Filter ROI by statistics | `Output`           | Convert ROI to RGB       | `ROI`       | `imagej-roi`     |

## Template

Proposed name `Compartment output -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB`, menu path
`Patterns/Graph / Project/Compartment output -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB` (proposal only —
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-ij1-roi-filter-statistics-ij1-roi-to-rgb.json`.

