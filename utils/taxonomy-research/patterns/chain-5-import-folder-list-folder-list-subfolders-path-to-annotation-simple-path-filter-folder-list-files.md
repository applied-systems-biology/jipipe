---
audience: "tbd"
modality: "folder -> path -> file"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "import-folder-list": "Folder list"
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Add path to annotations"
      - "path-filter": "Filter paths"
      - "folder-list-files": "List TIFF or JPG"
    edges:
      - {"from":"Folder list","fromSlot":"Folder paths","to":"List subfolders","toSlot":"Folders","dataClass":"folder"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"List TIFF or JPG","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files"
  menuPath: "Patterns/Data Sources/Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-folder-list\",\"jipipe:graph-compartment\":\"4f4767eb-4e1f-5f74-9cee-156ebfdb2bf5\",\"jipipe:node:name\":\"Folder list\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"4f4767eb-4e1f-5f74-9cee-156ebfdb2bf5\",\"jipipe:node:name\":\"List subfolders\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"4f4767eb-4e1f-5f74-9cee-156ebfdb2bf5\",\"jipipe:node:name\":\"Add path to annotations\"},\"n3\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"4f4767eb-4e1f-5f74-9cee-156ebfdb2bf5\",\"jipipe:node:name\":\"Filter paths\"},\"n4\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"4f4767eb-4e1f-5f74-9cee-156ebfdb2bf5\",\"jipipe:node:name\":\"List TIFF or JPG\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Folder paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Subfolders\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"},{\"source-node\":\"n2\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n3\",\"target-slot\":\"Input\"},{\"source-node\":\"n3\",\"source-slot\":\"Output\",\"target-node\":\"n4\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files

Mined pattern
`chain-5-import-folder-list-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files` (shape
`chain`, support 30).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `import-folder-list`        |
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |
| import   | `folder-list-files`         |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `import-folder-list`        | Folder list             |
| n1  | `folder-list-subfolders`    | List subfolders         |
| n2  | `path-to-annotation-simple` | Add path to annotations |
| n3  | `path-filter`               | Filter paths            |
| n4  | `folder-list-files`         | List TIFF or JPG        |

| from                    | slot              | to                      | slot      | data class |
|-------------------------|-------------------|-------------------------|-----------|------------|
| Folder list             | `Folder paths`    | List subfolders         | `Folders` | `folder`   |
| List subfolders         | `Subfolders`      | Add path to annotations | `Paths`   | `folder`   |
| Add path to annotations | `Annotated paths` | Filter paths            | `Input`   | `path`     |
| Filter paths            | `Output`          | List TIFF or JPG        | `Folders` | `path`     |

## Template

Proposed name `Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files`, menu
path
`Patterns/Data Sources/Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files`
(proposal only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-5-import-folder-list-folder-list-subfolders-path-to-annotation-simple-path-filter-folder-list-files.json`.

