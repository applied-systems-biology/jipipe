---
audience: "tbd"
modality: "path -> folder -> file"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "path-filter": "Filter paths"
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations"
    edges:
      - {"from":"Filter paths","fromSlot":"Output","to":"List ZIP","toSlot":"Folders","dataClass":"path"}
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
  templateCandidate: true
template:
  name: "Filter paths -> List files -> Annotate with file/directory name"
  menuPath: "Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"0059e97a-6cb9-5e3f-9c31-33d9fbfcf092\",\"jipipe:node:name\":\"Filter paths\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"0059e97a-6cb9-5e3f-9c31-33d9fbfcf092\",\"jipipe:node:name\":\"List ZIP\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"0059e97a-6cb9-5e3f-9c31-33d9fbfcf092\",\"jipipe:node:name\":\"Add path to annotations\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Files\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Filter paths -> List files -> Annotate with file/directory name

Mined pattern `conversion-path-filter-folder-list-files-path-to-annotation-simple` (shape `conversion`, support 32).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `path-filter`               |
| import   | `folder-list-files`         |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `path-filter`               | Filter paths            |
| n1  | `folder-list-files`         | List ZIP                |
| n2  | `path-to-annotation-simple` | Add path to annotations |

| from         | slot     | to                      | slot      | data class |
|--------------|----------|-------------------------|-----------|------------|
| Filter paths | `Output` | List ZIP                | `Folders` | `path`     |
| List ZIP     | `Files`  | Add path to annotations | `Paths`   | `file`     |

## Template

Proposed name `Filter paths -> List files -> Annotate with file/directory name`, menu path
`Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-path-filter-folder-list-files-path-to-annotation-simple.json`.

