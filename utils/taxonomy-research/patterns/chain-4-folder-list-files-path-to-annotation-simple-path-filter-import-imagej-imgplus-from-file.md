---
audience: "tbd"
modality: "folder -> file -> path -> imagej-imgplus"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 26
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "path-filter": "Filter paths"
      - "import-imagej-imgplus-from-file": "Import image"
    edges:
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name","toSlot":"Paths","dataClass":"file"}
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"Import image","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Filter paths -> Import image"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Filter paths -> Import image"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"01fc0c2a-031a-5a14-a605-001da38c5f5b\",\"jipipe:node:name\":\"List files\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"01fc0c2a-031a-5a14-a605-001da38c5f5b\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"01fc0c2a-031a-5a14-a605-001da38c5f5b\",\"jipipe:node:name\":\"Filter paths\"},\"n3\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"01fc0c2a-031a-5a14-a605-001da38c5f5b\",\"jipipe:node:name\":\"Import image\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Files\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Filter paths -> Import image

Mined pattern `chain-4-folder-list-files-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file` (shape
`chain`, support 26).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| import   | `folder-list-files`               |
| annotate | `path-to-annotation-simple`       |
| import   | `path-filter`                     |
| import   | `import-imagej-imgplus-from-file` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                           | name                              |
|-----|-----------------------------------|-----------------------------------|
| n0  | `folder-list-files`               | List files                        |
| n1  | `path-to-annotation-simple`       | Annotate with file/directory name |
| n2  | `path-filter`                     | Filter paths                      |
| n3  | `import-imagej-imgplus-from-file` | Import image                      |

| from                              | slot              | to                                | slot    | data class |
|-----------------------------------|-------------------|-----------------------------------|---------|------------|
| List files                        | `Files`           | Annotate with file/directory name | `Paths` | `file`     |
| Annotate with file/directory name | `Annotated paths` | Filter paths                      | `Input` | `path`     |
| Filter paths                      | `Output`          | Import image                      | `Files` | `path`     |

## Template

Proposed name `List files -> Annotate with file/directory name -> Filter paths -> Import image`, menu path
`Patterns/Data Sources/List files -> Annotate with file/directory name -> Filter paths -> Import image` (proposal only —
not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-folder-list-files-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file.json`.

