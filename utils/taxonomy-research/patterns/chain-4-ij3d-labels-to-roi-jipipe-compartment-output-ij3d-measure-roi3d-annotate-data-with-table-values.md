---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> roi-3d-list -> imagej-imgplus -> imagej-results-table -> jipipe:data"
tags: [Annotations, Graph / Project, Images, ROIs]
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
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "ij3d-labels-to-roi": "Labels to 3D ROI"
      - "jipipe:compartment-output": "Mask"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
      - "annotate-data-with-table-values": "Annotate data with table values"
    edges:
      - {"from":"Labels to 3D ROI","fromSlot":"ROI","to":"Mask","toSlot":"3D cell membranes","dataClass":"roi-3d-list"}
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Extract ROI 3D statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
      - {"from":"Extract ROI 3D statistics","fromSlot":"Measurements","to":"Annotate data with table values","toSlot":"Table","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values"
  menuPath: "Patterns/Images/IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-labels-to-roi\",\"jipipe:graph-compartment\":\"510967ff-3969-57c9-b2ba-eb855bdfcd1b\",\"jipipe:node:name\":\"Labels to 3D ROI\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"510967ff-3969-57c9-b2ba-eb855bdfcd1b\",\"jipipe:node:name\":\"Mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"510967ff-3969-57c9-b2ba-eb855bdfcd1b\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"},\"n3\":{\"jipipe:node-info-id\":\"annotate-data-with-table-values\",\"jipipe:graph-compartment\":\"510967ff-3969-57c9-b2ba-eb855bdfcd1b\",\"jipipe:node:name\":\"Annotate data with table values\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"3D cell membranes\"},{\"source-node\":\"n1\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n2\",\"target-slot\":\"Reference\"},{\"source-node\":\"n2\",\"source-slot\":\"Measurements\",\"target-node\":\"n3\",\"target-slot\":\"Table\"}]}"
---

# Pattern: IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values

Mined pattern `chain-4-ij3d-labels-to-roi-jipipe-compartment-output-ij3d-measure-roi3d-annotate-data-with-table-values`
(shape `chain`, support 26).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| other    | `ij3d-labels-to-roi`              |
| core-io  | `jipipe:compartment-output`       |
| other    | `ij3d-measure-roi3d`              |
| annotate | `annotate-data-with-table-values` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                           | name                            |
|-----|-----------------------------------|---------------------------------|
| n0  | `ij3d-labels-to-roi`              | Labels to 3D ROI                |
| n1  | `jipipe:compartment-output`       | Mask                            |
| n2  | `ij3d-measure-roi3d`              | Extract ROI 3D statistics       |
| n3  | `annotate-data-with-table-values` | Annotate data with table values |

| from                      | slot              | to                              | slot                | data class             |
|---------------------------|-------------------|---------------------------------|---------------------|------------------------|
| Labels to 3D ROI          | `ROI`             | Mask                            | `3D cell membranes` | `roi-3d-list`          |
| Mask                      | `Magenta trimmed` | Extract ROI 3D statistics       | `Reference`         | `imagej-imgplus`       |
| Extract ROI 3D statistics | `Measurements`    | Annotate data with table values | `Table`             | `imagej-results-table` |

## Template

Proposed name
`IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values`, menu
path
`Patterns/Images/IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-ij3d-labels-to-roi-jipipe-compartment-output-ij3d-measure-roi3d-annotate-data-with-table-values.json`.

