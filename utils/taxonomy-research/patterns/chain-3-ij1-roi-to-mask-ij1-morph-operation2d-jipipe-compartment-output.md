---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi -> imagej-imgplus-greyscale-mask"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 36
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
  projectCount: 36
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "ij1-morph-operation2d": "Morphological operation 2D"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Convert ROI to mask","fromSlot":"Output","to":"Morphological operation 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Morphological operation 2D","fromSlot":"Output","to":"Output","toSlot":"Mask","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Convert 2D ROI to mask -> Morphological operation 2D -> Compartment output"
  menuPath: "Patterns/ROIs/Convert 2D ROI to mask -> Morphological operation 2D -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"1a0c9f1b-a413-542e-ae21-33b77a44682e\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"1a0c9f1b-a413-542e-ae21-33b77a44682e\",\"jipipe:node:name\":\"Morphological operation 2D\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"1a0c9f1b-a413-542e-ae21-33b77a44682e\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Mask\"}]}"
---

# Pattern: Convert 2D ROI to mask -> Morphological operation 2D -> Compartment output

Mined pattern `chain-3-ij1-roi-to-mask-ij1-morph-operation2d-jipipe-compartment-output` (shape `chain`, support 36).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| other   | `ij1-roi-to-mask`           |
| other   | `ij1-morph-operation2d`     |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id                     | name                       |
|-----|-----------------------------|----------------------------|
| n0  | `ij1-roi-to-mask`           | Convert ROI to mask        |
| n1  | `ij1-morph-operation2d`     | Morphological operation 2D |
| n2  | `jipipe:compartment-output` | Output                     |

| from                       | slot     | to                         | slot    | data class                      |
|----------------------------|----------|----------------------------|---------|---------------------------------|
| Convert ROI to mask        | `Output` | Morphological operation 2D | `Input` | `imagej-imgplus-greyscale-mask` |
| Morphological operation 2D | `Output` | Output                     | `Mask`  | `imagej-imgplus`                |

## Template

Proposed name `Convert 2D ROI to mask -> Morphological operation 2D -> Compartment output`, menu path
`Patterns/ROIs/Convert 2D ROI to mask -> Morphological operation 2D -> Compartment output` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-roi-to-mask-ij1-morph-operation2d-jipipe-compartment-output.json`.

