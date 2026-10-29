---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-results-table"
tags: [Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Colocalization+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "jipipe:compartment-output": "Output"
      - "ij3d-roi-filter-by-overlap": "Filter 3D ROI by overlap"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Output","fromSlot":"Green ROIs","to":"Filter 3D ROI by overlap","toSlot":"ROI 1","dataClass":"roi-3d-list"}
      - {"from":"Filter 3D ROI by overlap","fromSlot":"ROI 2","to":"Extract ROI 3D statistics","toSlot":"ROI","dataClass":"roi-3d-list"}
  templateCandidate: true
template:
  name: "Compartment output -> Filter IJ3D ROI by overlap (old) -> Extract IJ3D ROI statistics"
  menuPath: "Patterns/Graph / Project/Compartment output -> Filter IJ3D ROI by overlap (old) -> Extract IJ3D ROI statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"e630be7d-4fad-53f1-bc1b-6ada8af9c23c\",\"jipipe:node:name\":\"Output\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-roi-filter-by-overlap\",\"jipipe:graph-compartment\":\"e630be7d-4fad-53f1-bc1b-6ada8af9c23c\",\"jipipe:node:name\":\"Filter 3D ROI by overlap\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"e630be7d-4fad-53f1-bc1b-6ada8af9c23c\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Green ROIs\",\"target-node\":\"n1\",\"target-slot\":\"ROI 1\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI 2\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Compartment output -> Filter IJ3D ROI by overlap (old) -> Extract IJ3D ROI statistics

Mined pattern `chain-3-jipipe-compartment-output-ij3d-roi-filter-by-overlap-ij3d-measure-roi3d` (shape `chain`, support
26).

## Role sequence

| role    | node ids                     |
|---------|------------------------------|
| core-io | `jipipe:compartment-output`  |
| other   | `ij3d-roi-filter-by-overlap` |
| other   | `ij3d-measure-roi3d`         |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Colocalization+Particles`.

| key | node id                      | name                      |
|-----|------------------------------|---------------------------|
| n0  | `jipipe:compartment-output`  | Output                    |
| n1  | `ij3d-roi-filter-by-overlap` | Filter 3D ROI by overlap  |
| n2  | `ij3d-measure-roi3d`         | Extract ROI 3D statistics |

| from                     | slot         | to                        | slot    | data class    |
|--------------------------|--------------|---------------------------|---------|---------------|
| Output                   | `Green ROIs` | Filter 3D ROI by overlap  | `ROI 1` | `roi-3d-list` |
| Filter 3D ROI by overlap | `ROI 2`      | Extract ROI 3D statistics | `ROI`   | `roi-3d-list` |

## Template

Proposed name `Compartment output -> Filter IJ3D ROI by overlap (old) -> Extract IJ3D ROI statistics`, menu path
`Patterns/Graph / Project/Compartment output -> Filter IJ3D ROI by overlap (old) -> Extract IJ3D ROI statistics`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-ij3d-roi-filter-by-overlap-ij3d-measure-roi3d.json`.

