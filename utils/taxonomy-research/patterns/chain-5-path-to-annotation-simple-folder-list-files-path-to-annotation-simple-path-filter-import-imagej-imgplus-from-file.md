---
audience: "tbd"
modality: "path -> folder -> file -> imagej-imgplus"
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
      - "path-to-annotation-simple": "Annotate with file/directory name #1"
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name #2"
      - "path-filter": "Filter paths"
      - "import-imagej-imgplus-from-file": "Import image"
    edges:
      - {"from":"Annotate with file/directory name #1","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name #2","toSlot":"Paths","dataClass":"file"}
      - {"from":"Annotate with file/directory name #2","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"Import image","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"c0302e31-6fac-55c1-a3b2-049b5ac9b4d0\",\"jipipe:node:name\":\"Annotate with file/directory name #1\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"c0302e31-6fac-55c1-a3b2-049b5ac9b4d0\",\"jipipe:node:name\":\"List files\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"c0302e31-6fac-55c1-a3b2-049b5ac9b4d0\",\"jipipe:node:name\":\"Annotate with file/directory name #2\"},\"n3\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"c0302e31-6fac-55c1-a3b2-049b5ac9b4d0\",\"jipipe:node:name\":\"Filter paths\"},\"n4\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"c0302e31-6fac-55c1-a3b2-049b5ac9b4d0\",\"jipipe:node:name\":\"Import image\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Files\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"},{\"source-node\":\"n2\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n3\",\"target-slot\":\"Input\"},{\"source-node\":\"n3\",\"source-slot\":\"Output\",\"target-node\":\"n4\",\"target-slot\":\"Files\"}]}"
---

# Pattern: Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image

Mined pattern
`chain-5-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file`
(shape `chain`, support 26).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| annotate | `path-to-annotation-simple`       |
| import   | `folder-list-files`               |
| annotate | `path-to-annotation-simple`       |
| import   | `path-filter`                     |
| import   | `import-imagej-imgplus-from-file` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                           | name                                 |
|-----|-----------------------------------|--------------------------------------|
| n0  | `path-to-annotation-simple`       | Annotate with file/directory name #1 |
| n1  | `folder-list-files`               | List files                           |
| n2  | `path-to-annotation-simple`       | Annotate with file/directory name #2 |
| n3  | `path-filter`                     | Filter paths                         |
| n4  | `import-imagej-imgplus-from-file` | Import image                         |

| from                                 | slot              | to                                   | slot      | data class |
|--------------------------------------|-------------------|--------------------------------------|-----------|------------|
| Annotate with file/directory name #1 | `Annotated paths` | List files                           | `Folders` | `path`     |
| List files                           | `Files`           | Annotate with file/directory name #2 | `Paths`   | `file`     |
| Annotate with file/directory name #2 | `Annotated paths` | Filter paths                         | `Input`   | `path`     |
| Filter paths                         | `Output`          | Import image                         | `Files`   | `path`     |

## Template

Proposed name
`Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image`,
menu path
`Patterns/Annotations/Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image`
(proposal only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-5-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file.json`.

