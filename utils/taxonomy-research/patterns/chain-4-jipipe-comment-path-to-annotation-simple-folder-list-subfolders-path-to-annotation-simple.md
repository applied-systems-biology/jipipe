---
audience: "tbd"
modality: "jipipe:data -> path -> folder"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
  projectCount: 26
  exampleGraph:
    compartment: "File handler"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "jipipe:comment": "Comment"
      - "path-to-annotation-simple": "Add path to annotations #1"
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Add path to annotations #2"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"Add path to annotations #1","toSlot":"Paths","dataClass":"jipipe:data"}
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"List subfolders","toSlot":"Folders","dataClass":"path"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"folder"}
  templateCandidate: true
template:
  name: "Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name"
  menuPath: "Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"593aab36-6d3a-5536-b697-e8d81c296cb6\",\"jipipe:node:name\":\"Comment\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"593aab36-6d3a-5536-b697-e8d81c296cb6\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"593aab36-6d3a-5536-b697-e8d81c296cb6\",\"jipipe:node:name\":\"List subfolders\"},\"n3\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"593aab36-6d3a-5536-b697-e8d81c296cb6\",\"jipipe:node:name\":\"Add path to annotations #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Comment\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"},{\"source-node\":\"n2\",\"source-slot\":\"Subfolders\",\"target-node\":\"n3\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name

Mined pattern `chain-4-jipipe-comment-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple` (shape
`chain`, support 26).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| other    | `jipipe:comment`            |
| annotate | `path-to-annotation-simple` |
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id                     | name                       |
|-----|-----------------------------|----------------------------|
| n0  | `jipipe:comment`            | Comment                    |
| n1  | `path-to-annotation-simple` | Add path to annotations #1 |
| n2  | `folder-list-subfolders`    | List subfolders            |
| n3  | `path-to-annotation-simple` | Add path to annotations #2 |

| from                       | slot              | to                         | slot      | data class    |
|----------------------------|-------------------|----------------------------|-----------|---------------|
| Comment                    | `Comment`         | Add path to annotations #1 | `Paths`   | `jipipe:data` |
| Add path to annotations #1 | `Annotated paths` | List subfolders            | `Folders` | `path`        |
| List subfolders            | `Subfolders`      | Add path to annotations #2 | `Paths`   | `folder`      |

## Template

Proposed name `Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name`,
menu path
`Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-jipipe-comment-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple.json`.

