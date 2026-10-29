---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> roi-3d-list -> imagej-imgplus -> imagej-results-table -> imagej-imgplus-color-rgb"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Membrane"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij3d-labels-to-roi": "Labels to 3D ROI"
      - "jipipe:compartment-output": "Mask"
      - "ij3d-roi-split-explode": "Split into individual 3D ROI lists"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Labels to 3D ROI","fromSlot":"ROI","to":"Mask","toSlot":"3D cell membranes","dataClass":"roi-3d-list"}
  templateCandidate: true
template:
  name: "IJ3D Labels to 3D ROI fans out to 7 target ids (roles: core-io, other)"
  menuPath: "Patterns/Images/IJ3D Labels to 3D ROI fans out to 7 target ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-labels-to-roi\",\"jipipe:graph-compartment\":\"4e0edf3e-199e-59b3-8b28-477d5c993c77\",\"jipipe:node:name\":\"Labels to 3D ROI\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"4e0edf3e-199e-59b3-8b28-477d5c993c77\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-roi-split-explode\",\"jipipe:graph-compartment\":\"4e0edf3e-199e-59b3-8b28-477d5c993c77\",\"jipipe:node:name\":\"Split into individual 3D ROI lists\"},\"n3\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"4e0edf3e-199e-59b3-8b28-477d5c993c77\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"3D cell membranes\"}]}"
---

# Pattern: IJ3D Labels to 3D ROI fans out to 7 target ids (roles: core-io, other)

Mined pattern `fan-out-ij3d-labels-to-roi-core-io-1-other-2` (shape `fan-out`, support 28).

## Role sequence

| role    | node ids                                                                                                                                                                 |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| other   | `ij3d-labels-to-roi`                                                                                                                                                     |
| core-io | `jipipe:compartment-output`                                                                                                                                              |
| other   | `ij3d-filter-roi3d-by-statistics`, `ij3d-measure-roi3d`, `ij3d-roi-change-properties`, `ij3d-roi-convert-to-rgb`, `ij3d-roi-filter-by-overlap`, `ij3d-roi-split-explode` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Membrane`.

| key | node id                     | name                               |
|-----|-----------------------------|------------------------------------|
| n0  | `ij3d-labels-to-roi`        | Labels to 3D ROI                   |
| n1  | `jipipe:compartment-output` | Mask                               |
| n2  | `ij3d-roi-split-explode`    | Split into individual 3D ROI lists |
| n3  | `ij3d-measure-roi3d`        | Extract ROI 3D statistics          |

| from             | slot  | to   | slot                | data class    |
|------------------|-------|------|---------------------|---------------|
| Labels to 3D ROI | `ROI` | Mask | `3D cell membranes` | `roi-3d-list` |

## Template

Proposed name `IJ3D Labels to 3D ROI fans out to 7 target ids (roles: core-io, other)`, menu path
`Patterns/Images/IJ3D Labels to 3D ROI fans out to 7 target ids (roles: core-io, other)` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-ij3d-labels-to-roi-core-io-1-other-2.json`.

