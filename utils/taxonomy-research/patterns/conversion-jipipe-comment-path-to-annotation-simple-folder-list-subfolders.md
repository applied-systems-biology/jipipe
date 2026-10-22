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
      - "path-to-annotation-simple": "Add path to annotations"
      - "folder-list-subfolders": "List subfolders"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"Add path to annotations","toSlot":"Paths","dataClass":"jipipe:data"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"List subfolders","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "Comment -> Annotate with file/directory name -> List subfolders"
  menuPath: "Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"61f346b5-32b7-588c-9df8-90fe6165eb81\",\"jipipe:node:name\":\"Comment\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"61f346b5-32b7-588c-9df8-90fe6165eb81\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"61f346b5-32b7-588c-9df8-90fe6165eb81\",\"jipipe:node:name\":\"List subfolders\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Comment\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Comment -> Annotate with file/directory name -> List subfolders

Mined pattern `conversion-jipipe-comment-path-to-annotation-simple-folder-list-subfolders` (shape `conversion`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `jipipe:comment` |
| annotate | `path-to-annotation-simple` |
| import | `folder-list-subfolders` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:comment` | Comment |
| n1 | `path-to-annotation-simple` | Add path to annotations |
| n2 | `folder-list-subfolders` | List subfolders |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Comment | `Comment` | Add path to annotations | `Paths` | `jipipe:data` |
| Add path to annotations | `Annotated paths` | List subfolders | `Folders` | `path` |

## Template

Proposed name `Comment -> Annotate with file/directory name -> List subfolders`, menu path `Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-jipipe-comment-path-to-annotation-simple-folder-list-subfolders.json`.

