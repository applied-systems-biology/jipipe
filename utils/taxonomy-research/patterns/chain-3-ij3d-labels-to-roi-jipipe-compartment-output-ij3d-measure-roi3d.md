---
audience: "tbd"
modality: "imagej-imgplus-greyscale -> roi-3d-list -> imagej-imgplus -> imagej-results-table"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Analysis+Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    nodes:
      - "ij3d-labels-to-roi": "IJ3D Labels to 3D ROI"
      - "jipipe:compartment-output": "For analysis"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"IJ3D Labels to 3D ROI","fromSlot":"ROI","to":"For analysis","toSlot":"Bacteria from blue","dataClass":"roi-3d-list"}
      - {"from":"For analysis","fromSlot":"Bacteria from blue","to":"Extract ROI 3D statistics","toSlot":"ROI","dataClass":"roi-3d-list"}
  templateCandidate: true
template:
  name: "IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics"
  menuPath: "Patterns/Images/IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij3d-labels-to-roi\",\"jipipe:graph-compartment\":\"12fbdfd7-4a2e-5604-b945-b9956341bb24\",\"jipipe:node:name\":\"IJ3D Labels to 3D ROI\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"12fbdfd7-4a2e-5604-b945-b9956341bb24\",\"jipipe:node:name\":\"For analysis\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"12fbdfd7-4a2e-5604-b945-b9956341bb24\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"ROI\",\"target-node\":\"n1\",\"target-slot\":\"Bacteria from blue\"},{\"source-node\":\"n1\",\"source-slot\":\"Bacteria from blue\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics

Mined pattern `chain-3-ij3d-labels-to-roi-jipipe-compartment-output-ij3d-measure-roi3d` (shape `chain`, support 32).

## Role sequence

| role    | node ids                    |
|---------|-----------------------------|
| other   | `ij3d-labels-to-roi`        |
| core-io | `jipipe:compartment-output` |
| other   | `ij3d-measure-roi3d`        |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip` (relative to `/data/JIPipe`), compartment
`Analysis+Preprocessing`.

| key | node id                     | name                      |
|-----|-----------------------------|---------------------------|
| n0  | `ij3d-labels-to-roi`        | IJ3D Labels to 3D ROI     |
| n1  | `jipipe:compartment-output` | For analysis              |
| n2  | `ij3d-measure-roi3d`        | Extract ROI 3D statistics |

| from                  | slot                 | to                        | slot                 | data class    |
|-----------------------|----------------------|---------------------------|----------------------|---------------|
| IJ3D Labels to 3D ROI | `ROI`                | For analysis              | `Bacteria from blue` | `roi-3d-list` |
| For analysis          | `Bacteria from blue` | Extract ROI 3D statistics | `ROI`                | `roi-3d-list` |

## Template

Proposed name `IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics`, menu path
`Patterns/Images/IJ3D Labels to 3D ROI -> Compartment output -> Extract IJ3D ROI statistics` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij3d-labels-to-roi-jipipe-compartment-output-ij3d-measure-roi3d.json`.

