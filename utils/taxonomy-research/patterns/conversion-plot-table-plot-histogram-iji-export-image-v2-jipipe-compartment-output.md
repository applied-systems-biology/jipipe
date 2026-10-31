---
audience: "tbd"
modality: "imagej-results-table -> plot -> imagej-imgplus -> file"
tags: [Export, Graph / Project, Plotting]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Cartography"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "plot-table:plot-histogram": "Histogram plot"
      - "iji-export-image-v2": "Export image"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Histogram plot","fromSlot":"Output","to":"Export image","toSlot":"Input","dataClass":"plot-histogram"}
      - {"from":"Export image","fromSlot":"Exported file","to":"Output","toSlot":"Histogram distances","dataClass":"file"}
  templateCandidate: true
template:
  name: "JFreeChart Histogram plot -> Export image -> Compartment output"
  menuPath: "Patterns/Plotting/JFreeChart Histogram plot -> Export image -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"plot-table:plot-histogram\",\"jipipe:graph-compartment\":\"40b44560-d813-5986-be51-0ec00002b186\",\"jipipe:node:name\":\"Histogram plot\"},\"n1\":{\"jipipe:node-info-id\":\"iji-export-image-v2\",\"jipipe:graph-compartment\":\"40b44560-d813-5986-be51-0ec00002b186\",\"jipipe:node:name\":\"Export image\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"40b44560-d813-5986-be51-0ec00002b186\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Exported file\",\"target-node\":\"n2\",\"target-slot\":\"Histogram distances\"}]}"
---

# Pattern: JFreeChart Histogram plot -> Export image -> Compartment output

Mined pattern `conversion-plot-table-plot-histogram-iji-export-image-v2-jipipe-compartment-output` (shape `conversion`,
support 26).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| plotting | `plot-table:plot-histogram` |
| export   | `iji-export-image-v2`       |
| core-io  | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Cartography`.

| key | node id                     | name           |
|-----|-----------------------------|----------------|
| n0  | `plot-table:plot-histogram` | Histogram plot |
| n1  | `iji-export-image-v2`       | Export image   |
| n2  | `jipipe:compartment-output` | Output         |

| from           | slot            | to           | slot                  | data class       |
|----------------|-----------------|--------------|-----------------------|------------------|
| Histogram plot | `Output`        | Export image | `Input`               | `plot-histogram` |
| Export image   | `Exported file` | Output       | `Histogram distances` | `file`           |

## Template

Proposed name `JFreeChart Histogram plot -> Export image -> Compartment output`, menu path
`Patterns/Plotting/JFreeChart Histogram plot -> Export image -> Compartment output` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-plot-table-plot-histogram-iji-export-image-v2-jipipe-compartment-output.json`.

