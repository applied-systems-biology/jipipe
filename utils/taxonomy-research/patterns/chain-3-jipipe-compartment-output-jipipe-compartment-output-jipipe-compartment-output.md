---
audience: "tbd"
modality: "unknown"
tags: [Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Distances+Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "jipipe:compartment-output": "Mask"
      - "jipipe:compartment-output": "Output"
      - "jipipe:compartment-output": "Distances"
    edges:
      - {"from":"Mask","fromSlot":"3D cell mask","to":"Output","toSlot":"3D cell mask","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Output","fromSlot":"Green distances","to":"Distances","toSlot":"Green to green closest distances","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Compartment output -> Compartment output -> Compartment output"
  menuPath: "Patterns/Graph / Project/Compartment output -> Compartment output -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"bfb87ab7-5b17-56a8-a94c-f5f61b7c3016\",\"jipipe:node:name\":\"Mask\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"bfb87ab7-5b17-56a8-a94c-f5f61b7c3016\",\"jipipe:node:name\":\"Output\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"bfb87ab7-5b17-56a8-a94c-f5f61b7c3016\",\"jipipe:node:name\":\"Distances\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"3D cell mask\",\"target-node\":\"n1\",\"target-slot\":\"3D cell mask\"},{\"source-node\":\"n1\",\"source-slot\":\"Green distances\",\"target-node\":\"n2\",\"target-slot\":\"Green to green closest distances\"}]}"
---

# Pattern: Compartment output -> Compartment output -> Compartment output

Mined pattern `chain-3-jipipe-compartment-output-jipipe-compartment-output-jipipe-compartment-output` (shape `chain`, support 32).

## Role sequence

| role | node ids |
|---|---|
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Distances+Membrane+Particles`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:compartment-output` | Mask |
| n1 | `jipipe:compartment-output` | Output |
| n2 | `jipipe:compartment-output` | Distances |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Mask | `3D cell mask` | Output | `3D cell mask` | `imagej-imgplus-greyscale-mask` |
| Output | `Green distances` | Distances | `Green to green closest distances` | `imagej-results-table` |

## Template

Proposed name `Compartment output -> Compartment output -> Compartment output`, menu path `Patterns/Graph / Project/Compartment output -> Compartment output -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-jipipe-compartment-output-jipipe-compartment-output-jipipe-compartment-output.json`.

