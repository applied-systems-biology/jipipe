---
audience: "tbd"
modality: "imagej-results-table -> file"
tags: [Export, Graph / Project, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Phagocytic meassures"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "table-apply-expression-per-row-v2": "Apply expression per row"
      - "iji-export-table-v2": "Export table: per image spores"
      - "jipipe:compartment-output": "Phagocytic measures"
    edges:
      - {"from":"Apply expression per row","fromSlot":"Output","to":"Export table: per image spores","toSlot":"Input","dataClass":"imagej-results-table"}
      - {"from":"Export table: per image spores","fromSlot":"Exported file","to":"Phagocytic measures","toSlot":"Results from JIPipe native","dataClass":"file"}
  templateCandidate: true
template:
  name: "Apply expression per row fans out to 3 target ids (roles: core-io, export)"
  menuPath: "Patterns/Tables/Apply expression per row fans out to 3 target ids (roles: core-io, export)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"table-apply-expression-per-row-v2\",\"jipipe:graph-compartment\":\"355d2109-128c-5a07-b25b-5d03050e9231\",\"jipipe:node:name\":\"Apply expression per row\"},\"n1\":{\"jipipe:node-info-id\":\"iji-export-table-v2\",\"jipipe:graph-compartment\":\"355d2109-128c-5a07-b25b-5d03050e9231\",\"jipipe:node:name\":\"Export table: per image spores\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"355d2109-128c-5a07-b25b-5d03050e9231\",\"jipipe:node:name\":\"Phagocytic measures\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Exported file\",\"target-node\":\"n2\",\"target-slot\":\"Results from JIPipe native\"}]}"
---

# Pattern: Apply expression per row fans out to 3 target ids (roles: core-io, export)

Mined pattern `fan-out-table-apply-expression-per-row-v2-core-io-1-export-1` (shape `fan-out`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `table-apply-expression-per-row-v2` |
| core-io | `jipipe:compartment-output` |
| export | `iji-export-table-as-xlsx-v2`, `iji-export-table-v2` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `Phagocytic meassures`.

| key | node id | name |
|---|---|---|
| n0 | `table-apply-expression-per-row-v2` | Apply expression per row |
| n1 | `iji-export-table-v2` | Export table: per image spores |
| n2 | `jipipe:compartment-output` | Phagocytic measures |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Apply expression per row | `Output` | Export table: per image spores | `Input` | `imagej-results-table` |
| Export table: per image spores | `Exported file` | Phagocytic measures | `Results from JIPipe native` | `file` |

## Template

Proposed name `Apply expression per row fans out to 3 target ids (roles: core-io, export)`, menu path `Patterns/Tables/Apply expression per row fans out to 3 target ids (roles: core-io, export)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-out-table-apply-expression-per-row-v2-core-io-1-export-1.json`.

