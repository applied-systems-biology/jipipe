---
audience: "tbd"
modality: "folder -> path -> file"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 36
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_09_Misc/Kidney/kidney.jip"
  projectCount: 36
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Annotate with file/directory name #1"
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name #2"
      - "path-filter": "Filter paths"
    edges:
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Annotate with file/directory name #1","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Annotate with file/directory name #1","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name #2","toSlot":"Paths","dataClass":"file"}
      - {"from":"Annotate with file/directory name #2","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths"
  menuPath: "Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"fd4b2173-2977-5710-8d7d-d30e914adfe8\",\"jipipe:node:name\":\"List subfolders\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"fd4b2173-2977-5710-8d7d-d30e914adfe8\",\"jipipe:node:name\":\"Annotate with file/directory name #1\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"fd4b2173-2977-5710-8d7d-d30e914adfe8\",\"jipipe:node:name\":\"List files\"},\"n3\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"fd4b2173-2977-5710-8d7d-d30e914adfe8\",\"jipipe:node:name\":\"Annotate with file/directory name #2\"},\"n4\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"fd4b2173-2977-5710-8d7d-d30e914adfe8\",\"jipipe:node:name\":\"Filter paths\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Subfolders\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"},{\"source-node\":\"n2\",\"source-slot\":\"Files\",\"target-node\":\"n3\",\"target-slot\":\"Paths\"},{\"source-node\":\"n3\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n4\",\"target-slot\":\"Input\"}]}"
---

# Pattern: List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths

Mined pattern
`chain-5-folder-list-subfolders-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter`
(shape `chain`, support 36).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |
| import   | `folder-list-files`         |
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                     | name                                 |
|-----|-----------------------------|--------------------------------------|
| n0  | `folder-list-subfolders`    | List subfolders                      |
| n1  | `path-to-annotation-simple` | Annotate with file/directory name #1 |
| n2  | `folder-list-files`         | List files                           |
| n3  | `path-to-annotation-simple` | Annotate with file/directory name #2 |
| n4  | `path-filter`               | Filter paths                         |

| from                                 | slot              | to                                   | slot      | data class |
|--------------------------------------|-------------------|--------------------------------------|-----------|------------|
| List subfolders                      | `Subfolders`      | Annotate with file/directory name #1 | `Paths`   | `folder`   |
| Annotate with file/directory name #1 | `Annotated paths` | List files                           | `Folders` | `path`     |
| List files                           | `Files`           | Annotate with file/directory name #2 | `Paths`   | `file`     |
| Annotate with file/directory name #2 | `Annotated paths` | Filter paths                         | `Input`   | `path`     |

## Template

Proposed name
`List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths`,
menu path
`Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-5-folder-list-subfolders-path-to-annotation-simple-folder-list-files-path-to-annotation-simple-path-filter.json`.

