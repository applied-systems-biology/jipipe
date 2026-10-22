---
audience: "tbd"
modality: "path -> folder"
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
    edges:
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"List subfolders","toSlot":"Folders","dataClass":"path"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"folder"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> List subfolders -> Annotate with file/directory name"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> List subfolders -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"83080a08-bccd-5f53-ac97-b02156abc62f\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"83080a08-bccd-5f53-ac97-b02156abc62f\",\"jipipe:node:name\":\"List subfolders\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"83080a08-bccd-5f53-ac97-b02156abc62f\",\"jipipe:node:name\":\"Add path to annotations #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Subfolders\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Annotate with file/directory name -> List subfolders -> Annotate with file/directory name

Mined pattern `conversion-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple` (shape `conversion`, support 26).

## Role sequence

| role | node ids |
|---|---|
| annotate | `path-to-annotation-simple` |
| import | `folder-list-subfolders` |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id | name |
|---|---|---|
| n0 | `path-to-annotation-simple` | Add path to annotations #1 |
| n1 | `folder-list-subfolders` | List subfolders |
| n2 | `path-to-annotation-simple` | Add path to annotations #2 |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add path to annotations #1 | `Annotated paths` | List subfolders | `Folders` | `path` |
| List subfolders | `Subfolders` | Add path to annotations #2 | `Paths` | `folder` |

## Template

Proposed name `Annotate with file/directory name -> List subfolders -> Annotate with file/directory name`, menu path `Patterns/Annotations/Annotate with file/directory name -> List subfolders -> Annotate with file/directory name` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-path-to-annotation-simple-folder-list-subfolders-path-to-annotation-simple.json`.

