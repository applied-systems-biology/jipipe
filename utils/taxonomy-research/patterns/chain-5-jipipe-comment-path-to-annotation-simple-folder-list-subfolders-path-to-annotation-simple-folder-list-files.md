---
audience: "tbd"
modality: "jipipe:data -> path -> folder -> file"
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
      - "folder-list-files": "List files"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"Add path to annotations #1","toSlot":"Paths","dataClass":"jipipe:data"}
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"List subfolders","toSlot":"Folders","dataClass":"path"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations #2","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files"
  menuPath: "Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"e9dea934-0b60-5e61-98f9-5391a361bb9f\",\"jipipe:node:name\":\"Comment\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"e9dea934-0b60-5e61-98f9-5391a361bb9f\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"e9dea934-0b60-5e61-98f9-5391a361bb9f\",\"jipipe:node:name\":\"List subfolders\"},\"n3\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"e9dea934-0b60-5e61-98f9-5391a361bb9f\",\"jipipe:node:name\":\"Add path to annotations #2\"},\"n4\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"e9dea934-0b60-5e61-98f9-5391a361bb9f\",\"jipipe:node:name\":\"List files\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Comment\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"},{\"source-node\":\"n2\",\"source-slot\":\"Subfolders\",\"target-node\":\"n3\",\"target-slot\":\"Paths\"},{\"source-node\":\"n3\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n4\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files

Mined pattern `chain-5-jipipe-comment-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple-folder-list-files` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| other | `jipipe:comment` |
| annotate | `path-to-annotation-simple` |
| import | `folder-list-subfolders` |
| annotate | `path-to-annotation-simple` |
| import | `folder-list-files` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:comment` | Comment |
| n1 | `path-to-annotation-simple` | Add path to annotations #1 |
| n2 | `folder-list-subfolders` | List subfolders |
| n3 | `path-to-annotation-simple` | Add path to annotations #2 |
| n4 | `folder-list-files` | List files |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Comment | `Comment` | Add path to annotations #1 | `Paths` | `jipipe:data` |
| Add path to annotations #1 | `Annotated paths` | List subfolders | `Folders` | `path` |
| List subfolders | `Subfolders` | Add path to annotations #2 | `Paths` | `folder` |
| Add path to annotations #2 | `Annotated paths` | List files | `Folders` | `path` |

## Template

Proposed name `Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files`, menu path `Patterns/Graph / Project/Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-5-jipipe-comment-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple-folder-list-files.json`.

