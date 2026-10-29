---
audience: "tbd"
modality: "folder -> path -> file"
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
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Add path to annotations #1"
      - "path-filter": "Filter paths"
      - "folder-list-files": "List TIFF or JPG"
      - "path-to-annotation-simple": "Add path to annotations #2"
    edges:
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations #1","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"List TIFF or JPG","toSlot":"Folders","dataClass":"path"}
      - {"from":"List TIFF or JPG","fromSlot":"Files","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"file"}
  templateCandidate: true
template:
  name: "List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name"
  menuPath: "Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"d9c41e20-df61-598b-be85-06f12fc591c4\",\"jipipe:node:name\":\"List subfolders\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"d9c41e20-df61-598b-be85-06f12fc591c4\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"d9c41e20-df61-598b-be85-06f12fc591c4\",\"jipipe:node:name\":\"Filter paths\"},\"n3\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"d9c41e20-df61-598b-be85-06f12fc591c4\",\"jipipe:node:name\":\"List TIFF or JPG\"},\"n4\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"d9c41e20-df61-598b-be85-06f12fc591c4\",\"jipipe:node:name\":\"Add path to annotations #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Subfolders\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Folders\"},{\"source-node\":\"n3\",\"source-slot\":\"Files\",\"target-node\":\"n4\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name

Mined pattern
`chain-5-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files-path-to-annotation-simple`
(shape `chain`, support 32).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |
| import   | `folder-list-files`         |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                       |
|-----|-----------------------------|----------------------------|
| n0  | `folder-list-subfolders`    | List subfolders            |
| n1  | `path-to-annotation-simple` | Add path to annotations #1 |
| n2  | `path-filter`               | Filter paths               |
| n3  | `folder-list-files`         | List TIFF or JPG           |
| n4  | `path-to-annotation-simple` | Add path to annotations #2 |

| from                       | slot              | to                         | slot      | data class |
|----------------------------|-------------------|----------------------------|-----------|------------|
| List subfolders            | `Subfolders`      | Add path to annotations #1 | `Paths`   | `folder`   |
| Add path to annotations #1 | `Annotated paths` | Filter paths               | `Input`   | `path`     |
| Filter paths               | `Output`          | List TIFF or JPG           | `Folders` | `path`     |
| List TIFF or JPG           | `Files`           | Add path to annotations #2 | `Paths`   | `file`     |

## Template

Proposed name
`List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name`,
menu path
`Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-5-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files-path-to-annotation-simple.json`.

