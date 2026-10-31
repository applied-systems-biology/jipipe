---
audience: "tbd"
modality: "imagej-results-table -> file"
tags: [Export, Graph / Project, Tables]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Manuel_08_3D.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Preprocessing and edges"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "table-merge-tables": "Merge table rows"
      - "jipipe:compartment-output": "Processed curves"
      - "iji-export-table-as-xlsx-v2": "Export table as XLSX: root widths summary empirical"
    edges:
      - {"from":"Merge table rows","fromSlot":"Output","to":"Processed curves","toSlot":"Mean empirical values","dataClass":"imagej-results-table"}
  templateCandidate: true
template:
  name: "Merge table rows fans out to 4 target ids (roles: core-io, export)"
  menuPath: "Patterns/Tables/Merge table rows fans out to 4 target ids (roles: core-io, export)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"table-merge-tables\",\"jipipe:graph-compartment\":\"0c145980-e5ae-53fb-8200-2d2078fb0ac2\",\"jipipe:node:name\":\"Merge table rows\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"0c145980-e5ae-53fb-8200-2d2078fb0ac2\",\"jipipe:node:name\":\"Processed curves\"},\"n2\":{\"jipipe:node-info-id\":\"iji-export-table-as-xlsx-v2\",\"jipipe:graph-compartment\":\"0c145980-e5ae-53fb-8200-2d2078fb0ac2\",\"jipipe:node:name\":\"Export table as XLSX: root widths summary empirical\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Mean empirical values\"}]}"
---

# Pattern: Merge table rows fans out to 4 target ids (roles: core-io, export)

Mined pattern `fan-out-table-merge-tables-core-io-1-export-1` (shape `fan-out`, support 28).

## Role sequence

| role    | node ids                                                                 |
|---------|--------------------------------------------------------------------------|
| other   | `table-merge-tables`                                                     |
| core-io | `jipipe:compartment-output`                                              |
| export  | `iji-export-table`, `iji-export-table-as-xlsx-v2`, `iji-export-table-v2` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing and edges`.

| key | node id                       | name                                                |
|-----|-------------------------------|-----------------------------------------------------|
| n0  | `table-merge-tables`          | Merge table rows                                    |
| n1  | `jipipe:compartment-output`   | Processed curves                                    |
| n2  | `iji-export-table-as-xlsx-v2` | Export table as XLSX: root widths summary empirical |

| from             | slot     | to               | slot                    | data class             |
|------------------|----------|------------------|-------------------------|------------------------|
| Merge table rows | `Output` | Processed curves | `Mean empirical values` | `imagej-results-table` |

## Template

Proposed name `Merge table rows fans out to 4 target ids (roles: core-io, export)`, menu path
`Patterns/Tables/Merge table rows fans out to 4 target ids (roles: core-io, export)` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-table-merge-tables-core-io-1-export-1.json`.

