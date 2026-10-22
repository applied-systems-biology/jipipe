---
audience: "tbd"
modality: "file -> imagej-imgplus -> jipipe:data"
tags: [Annotations, Data Sources, Miscellaneous]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 18
  projects:
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
    - "AIAgentTests/03-ExampleWorkflows/Phagocytosis_Simran_07.jip"
    - "AIAgentTests/03-ExampleWorkflows/Phagocytosis_Simran_07_Asp.jip"
  projectCount: 18
  exampleGraph:
    compartment: "Cellpose training"
    project: "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    nodes:
      - "import-imagej-imgplus-from-file": "Import image"
      - "annotate-with-data": "Annotate with data"
      - "distribute-data": "Split data randomly (percentage)"
    edges:
      - {"from":"Import image","fromSlot":"Image","to":"Annotate with data","toSlot":"Input","dataClass":"imagej-imgplus"}
      - {"from":"Annotate with data","fromSlot":"Output","to":"Split data randomly (percentage)","toSlot":"Input","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Import image -> Annotate with data -> other"
  menuPath: "Patterns/Data Sources/Import image -> Annotate with data -> other"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"94f932f1-a942-59ff-90b9-20b27bd90c30\",\"jipipe:node:name\":\"Import image\"},\"n1\":{\"jipipe:node-info-id\":\"annotate-with-data\",\"jipipe:graph-compartment\":\"94f932f1-a942-59ff-90b9-20b27bd90c30\",\"jipipe:node:name\":\"Annotate with data\"},\"n2\":{\"jipipe:node-info-id\":\"distribute-data\",\"jipipe:graph-compartment\":\"94f932f1-a942-59ff-90b9-20b27bd90c30\",\"jipipe:node:name\":\"Split data randomly (percentage)\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Image\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Import image -> Annotate with data -> other

Mined pattern `annotation-sandwich-import-imagej-imgplus-from-file-annotate-with-data-other` (shape `annotation-sandwich`, support 18).

## Role sequence

| role | node ids |
|---|---|
| import | `import-imagej-imgplus-from-file` |
| annotate | `annotate-with-data` |
| other | `distribute-data` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment `Cellpose training`.

| key | node id | name |
|---|---|---|
| n0 | `import-imagej-imgplus-from-file` | Import image |
| n1 | `annotate-with-data` | Annotate with data |
| n2 | `distribute-data` | Split data randomly (percentage) |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Import image | `Image` | Annotate with data | `Input` | `imagej-imgplus` |
| Annotate with data | `Output` | Split data randomly (percentage) | `Input` | `jipipe:data` |

## Template

Proposed name `Import image -> Annotate with data -> other`, menu path `Patterns/Data Sources/Import image -> Annotate with data -> other` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/annotation-sandwich-import-imagej-imgplus-from-file-annotate-with-data-other.json`.

