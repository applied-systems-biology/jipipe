---
audience: "tbd"
modality: "path -> folder -> file -> imagej-imgplus"
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
      - "folder-list-files": "List TIFF or JPG"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-imgplus-from-file": "Import image"
    edges:
      - {"from":"Filter paths","fromSlot":"Output","to":"List TIFF or JPG","toSlot":"Folders","dataClass":"path"}
      - {"from":"List TIFF or JPG","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Import image","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "Filter paths -> List files -> Annotate with file/directory name -> Import image"
  menuPath: "Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name -> Import image"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"4c482fa9-7107-5c28-bf8c-31bd4324090a\",\"jipipe:node:name\":\"Filter paths\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"4c482fa9-7107-5c28-bf8c-31bd4324090a\",\"jipipe:node:name\":\"List TIFF or JPG\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"4c482fa9-7107-5c28-bf8c-31bd4324090a\",\"jipipe:node:name\":\"Add path to annotations\"},\"n3\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"4c482fa9-7107-5c28-bf8c-31bd4324090a\",\"jipipe:node:name\":\"Import image\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Files\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"},{\"source-node\":\"n2\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n3\",\"target-slot\":\"Files\"}]}"
---

# Pattern: Filter paths -> List files -> Annotate with file/directory name -> Import image

Mined pattern `chain-4-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file` (shape
`chain`, support 32).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| import   | `path-filter`                     |
| import   | `folder-list-files`               |
| annotate | `path-to-annotation-simple`       |
| import   | `import-imagej-imgplus-from-file` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                           | name                    |
|-----|-----------------------------------|-------------------------|
| n0  | `path-filter`                     | Filter paths            |
| n1  | `folder-list-files`               | List TIFF or JPG        |
| n2  | `path-to-annotation-simple`       | Add path to annotations |
| n3  | `import-imagej-imgplus-from-file` | Import image            |

| from                    | slot              | to                      | slot      | data class |
|-------------------------|-------------------|-------------------------|-----------|------------|
| Filter paths            | `Output`          | List TIFF or JPG        | `Folders` | `path`     |
| List TIFF or JPG        | `Files`           | Add path to annotations | `Paths`   | `file`     |
| Add path to annotations | `Annotated paths` | Import image            | `Files`   | `path`     |

## Template

Proposed name `Filter paths -> List files -> Annotate with file/directory name -> Import image`, menu path
`Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name -> Import image` (proposal only —
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file.json`.

