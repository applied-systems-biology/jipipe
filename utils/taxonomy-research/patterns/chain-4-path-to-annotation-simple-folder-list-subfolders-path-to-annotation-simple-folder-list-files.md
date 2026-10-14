---
audience: "tbd"
modality: "path -> folder -> file"
tags: [Annotations, Data Sources]
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
      - "path-to-annotation-simple": "Add path to annotations #1"
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Add path to annotations #2"
      - "folder-list-files": "List files"
    edges:
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"List subfolders","toSlot":"Folders","dataClass":"path"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations #2","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"50ca2aba-bea3-5fc3-9cf5-318f74c2bfb0\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"50ca2aba-bea3-5fc3-9cf5-318f74c2bfb0\",\"jipipe:node:name\":\"List subfolders\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"50ca2aba-bea3-5fc3-9cf5-318f74c2bfb0\",\"jipipe:node:name\":\"Add path to annotations #2\"},\"n3\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"50ca2aba-bea3-5fc3-9cf5-318f74c2bfb0\",\"jipipe:node:name\":\"List files\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Subfolders\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"},{\"source-node\":\"n2\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n3\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files

Mined pattern `chain-4-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple-folder-list-files` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| annotate | `path-to-annotation-simple` |
| import | `folder-list-subfolders` |
| annotate | `path-to-annotation-simple` |
| import | `folder-list-files` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id | name |
|---|---|---|
| n0 | `path-to-annotation-simple` | Add path to annotations #1 |
| n1 | `folder-list-subfolders` | List subfolders |
| n2 | `path-to-annotation-simple` | Add path to annotations #2 |
| n3 | `folder-list-files` | List files |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add path to annotations #1 | `Annotated paths` | List subfolders | `Folders` | `path` |
| List subfolders | `Subfolders` | Add path to annotations #2 | `Paths` | `folder` |
| Add path to annotations #2 | `Annotated paths` | List files | `Folders` | `path` |

## Template

Proposed name `Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files`, menu path `Patterns/Annotations/Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple-folder-list-files.json`.

