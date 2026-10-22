---
audience: "tbd"
modality: "imagej-results-table"
tags: [Graph / Project, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 38
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 38
  exampleGraph:
    compartment: "Colocalization"
    project: "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    nodes:
      - "table-add-annotation-columns": "Add annotations as columns"
      - "table-merge-tables": "Merge table rows"
      - "jipipe:compartment-output": "Colocalization per component"
    edges:
      - {"from":"Add annotations as columns","fromSlot":"Output","to":"Merge table rows","toSlot":"Input","dataClass":"imagej-results-table"}
      - {"from":"Merge table rows","fromSlot":"Output","to":"Colocalization per component","toSlot":"Green vs. Magenta, STED","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Add annotations as columns -> Merge table rows -> Compartment output"
  menuPath: "Patterns/Tables/Add annotations as columns -> Merge table rows -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"table-add-annotation-columns\",\"jipipe:graph-compartment\":\"a50f16bd-a634-53b7-98c0-87f8bb5b0e65\",\"jipipe:node:name\":\"Add annotations as columns\"},\"n1\":{\"jipipe:node-info-id\":\"table-merge-tables\",\"jipipe:graph-compartment\":\"a50f16bd-a634-53b7-98c0-87f8bb5b0e65\",\"jipipe:node:name\":\"Merge table rows\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a50f16bd-a634-53b7-98c0-87f8bb5b0e65\",\"jipipe:node:name\":\"Colocalization per component\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Green vs. Magenta, STED\"}]}"
---

# Pattern: Add annotations as columns -> Merge table rows -> Compartment output

Mined pattern `chain-3-table-add-annotation-columns-table-merge-tables-jipipe-compartment-output` (shape `chain`, support 38).

## Role sequence

| role | node ids |
|---|---|
| other | `table-add-annotation-columns` |
| other | `table-merge-tables` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` (relative to `/data/JIPipe`), compartment `Colocalization`.

| key | node id | name |
|---|---|---|
| n0 | `table-add-annotation-columns` | Add annotations as columns |
| n1 | `table-merge-tables` | Merge table rows |
| n2 | `jipipe:compartment-output` | Colocalization per component |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add annotations as columns | `Output` | Merge table rows | `Input` | `imagej-results-table` |
| Merge table rows | `Output` | Colocalization per component | `Green vs. Magenta, STED` | `imagej-results-table` |

## Template

Proposed name `Add annotations as columns -> Merge table rows -> Compartment output`, menu path `Patterns/Tables/Add annotations as columns -> Merge table rows -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-table-add-annotation-columns-table-merge-tables-jipipe-compartment-output.json`.

