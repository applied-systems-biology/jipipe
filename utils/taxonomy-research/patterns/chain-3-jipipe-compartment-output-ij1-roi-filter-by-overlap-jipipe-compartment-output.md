---
audience: "tbd"
modality: "imagej-imgplus -> imagej-roi"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 36
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
  projectCount: 36
  exampleGraph:
    compartment: "Preprocessing+Quality check"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "jipipe:compartment-output": "Output #1"
      - "ij1-roi-filter-by-overlap": "Filter ROI by overlap"
      - "jipipe:compartment-output": "Output #2"
    edges:
      - {"from":"Output #1","fromSlot":"ROI","to":"Filter ROI by overlap","toSlot":"ROI 1","dataClass":"imagej-roi"}
      - {"from":"Filter ROI by overlap","fromSlot":"ROI 1","to":"Output #2","toSlot":"False negatives ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output"
  menuPath: "Patterns/Graph / Project/Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b258d048-8393-5ec2-b699-8fd18b5f5dea\",\"jipipe:node:name\":\"Output #1\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"b258d048-8393-5ec2-b699-8fd18b5f5dea\",\"jipipe:node:name\":\"Filter ROI by overlap\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"b258d048-8393-5ec2-b699-8fd18b5f5dea\",\"jipipe:node:name\":\"Output #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"ROI 1\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI 1\",\"target-node\":\"n2\",\"target-slot\":\"False negatives ROI\"}]}"
---

# Pattern: Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output

Mined pattern `chain-3-jipipe-compartment-output-ij1-roi-filter-by-overlap-jipipe-compartment-output` (shape `chain`,
support 36).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| core-io | `jipipe:compartment-output` |
| other   | `ij1-roi-filter-by-overlap` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing+Quality check`.

| key | node id                     | name                  |
|-----|-----------------------------|-----------------------|
| n0  | `jipipe:compartment-output` | Output #1             |
| n1  | `ij1-roi-filter-by-overlap` | Filter ROI by overlap |
| n2  | `jipipe:compartment-output` | Output #2             |

| from                  | slot    | to                    | slot                  | data class   |
|-----------------------|---------|-----------------------|-----------------------|--------------|
| Output #1             | `ROI`   | Filter ROI by overlap | `ROI 1`               | `imagej-roi` |
| Filter ROI by overlap | `ROI 1` | Output #2             | `False negatives ROI` | `imagej-roi` |

## Template

Proposed name `Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output`, menu path
`Patterns/Graph / Project/Compartment output -> Filter 2D ROI by overlap (old) -> Compartment output` (proposal only —
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-ij1-roi-filter-by-overlap-jipipe-compartment-output.json`.

