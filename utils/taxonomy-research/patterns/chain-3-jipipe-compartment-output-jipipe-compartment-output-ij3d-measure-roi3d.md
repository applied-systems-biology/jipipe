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
    compartment: "Membrane+Particles+Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "jipipe:compartment-output": "Output"
      - "jipipe:compartment-output": "Mask"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Output","fromSlot":"Green trimmed","to":"Mask","toSlot":"Green trimmed","dataClass":"imagej-imgplus"}
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Extract ROI 3D statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Compartment output -> Compartment output -> Extract IJ3D ROI statistics"
  menuPath: "Patterns/Graph / Project/Compartment output -> Compartment output -> Extract IJ3D ROI statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"fe64fd2b-e861-51e8-b7d5-d9357e5c7d5b\",\"jipipe:node:name\":\"Output\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"fe64fd2b-e861-51e8-b7d5-d9357e5c7d5b\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"fe64fd2b-e861-51e8-b7d5-d9357e5c7d5b\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Green trimmed\",\"target-node\":\"n1\",\"target-slot\":\"Green trimmed\"},{\"source-node\":\"n1\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"}]}"
---

# Pattern: Compartment output -> Compartment output -> Extract IJ3D ROI statistics

Mined pattern `chain-3-jipipe-compartment-output-jipipe-compartment-output-ij3d-measure-roi3d` (shape `chain`, support
26).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |
| other   | `ij3d-measure-roi3d`        |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles+Preprocessing`.

| key | node id                     | name                      |
|-----|-----------------------------|---------------------------|
| n0  | `jipipe:compartment-output` | Output                    |
| n1  | `jipipe:compartment-output` | Mask                      |
| n2  | `ij3d-measure-roi3d`        | Extract ROI 3D statistics |

| from   | slot              | to                        | slot            | data class       |
|--------|-------------------|---------------------------|-----------------|------------------|
| Output | `Green trimmed`   | Mask                      | `Green trimmed` | `imagej-imgplus` |
| Mask   | `Magenta trimmed` | Extract ROI 3D statistics | `Reference`     | `imagej-imgplus` |

## Template

Proposed name `Compartment output -> Compartment output -> Extract IJ3D ROI statistics`, menu path
`Patterns/Graph / Project/Compartment output -> Compartment output -> Extract IJ3D ROI statistics` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-compartment-output-jipipe-compartment-output-ij3d-measure-roi3d.json`.

