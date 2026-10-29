---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-results-table -> jipipe:data"
tags: [Annotations, Graph / Project, ROIs]
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
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "jipipe:compartment-output": "Mask"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
      - "annotate-data-with-table-values": "Annotate data with table values"
    edges:
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Extract ROI 3D statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
      - {"from":"Extract ROI 3D statistics","fromSlot":"Measurements","to":"Annotate data with table values","toSlot":"Table","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values"
  menuPath: "Patterns/Graph / Project/Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"920e0f39-ec3f-5fcb-9d33-2160668f8bbf\",\"jipipe:node:name\":\"Mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"920e0f39-ec3f-5fcb-9d33-2160668f8bbf\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"},\"n2\":{\"jipipe:node-info-id\":\"annotate-data-with-table-values\",\"jipipe:graph-compartment\":\"920e0f39-ec3f-5fcb-9d33-2160668f8bbf\",\"jipipe:node:name\":\"Annotate data with table values\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n1\",\"target-slot\":\"Reference\"},{\"source-node\":\"n1\",\"source-slot\":\"Measurements\",\"target-node\":\"n2\",\"target-slot\":\"Table\"}]}"
---

# Pattern: Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values

Mined pattern `conversion-jipipe-compartment-output-ij3d-measure-roi3d-annotate-data-with-table-values` (shape
`conversion`, support 28).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| core-io  | `jipipe:compartment-output`       |
| other    | `ij3d-measure-roi3d`              |
| annotate | `annotate-data-with-table-values` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment
`Membrane+Particles`.

| key | node id                           | name                            |
|-----|-----------------------------------|---------------------------------|
| n0  | `jipipe:compartment-output`       | Mask                            |
| n1  | `ij3d-measure-roi3d`              | Extract ROI 3D statistics       |
| n2  | `annotate-data-with-table-values` | Annotate data with table values |

| from                      | slot              | to                              | slot        | data class             |
|---------------------------|-------------------|---------------------------------|-------------|------------------------|
| Mask                      | `Magenta trimmed` | Extract ROI 3D statistics       | `Reference` | `imagej-imgplus`       |
| Extract ROI 3D statistics | `Measurements`    | Annotate data with table values | `Table`     | `imagej-results-table` |

## Template

Proposed name `Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values`, menu path
`Patterns/Graph / Project/Compartment output -> Extract IJ3D ROI statistics -> Annotate data with table values`
(proposal only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-jipipe-compartment-output-ij3d-measure-roi3d-annotate-data-with-table-values.json`.

