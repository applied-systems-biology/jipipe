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
      - "path-to-annotation-simple": "Add path to annotations"
      - "path-filter": "Filter paths"
      - "folder-list-files": "List TIFF or JPG"
    edges:
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"List TIFF or JPG","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "List subfolders -> Annotate with file/directory name -> Filter paths -> List files"
  menuPath: "Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"3dc7f3d9-e0db-5575-91d8-dfb3d75864ea\",\"jipipe:node:name\":\"List subfolders\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"3dc7f3d9-e0db-5575-91d8-dfb3d75864ea\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"3dc7f3d9-e0db-5575-91d8-dfb3d75864ea\",\"jipipe:node:name\":\"Filter paths\"},\"n3\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"3dc7f3d9-e0db-5575-91d8-dfb3d75864ea\",\"jipipe:node:name\":\"List TIFF or JPG\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Subfolders\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: List subfolders -> Annotate with file/directory name -> Filter paths -> List files

Mined pattern `chain-4-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files` (shape `chain`,
support 32).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |
| import   | `folder-list-files`         |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `folder-list-subfolders`    | List subfolders         |
| n1  | `path-to-annotation-simple` | Add path to annotations |
| n2  | `path-filter`               | Filter paths            |
| n3  | `folder-list-files`         | List TIFF or JPG        |

| from                    | slot              | to                      | slot      | data class |
|-------------------------|-------------------|-------------------------|-----------|------------|
| List subfolders         | `Subfolders`      | Add path to annotations | `Paths`   | `folder`   |
| Add path to annotations | `Annotated paths` | Filter paths            | `Input`   | `path`     |
| Filter paths            | `Output`          | List TIFF or JPG        | `Folders` | `path`     |

## Template

Proposed name `List subfolders -> Annotate with file/directory name -> Filter paths -> List files`, menu path
`Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths -> List files` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files.json`.

