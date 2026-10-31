---
audience: "tbd"
modality: "path -> folder -> file"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 50
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_09_Misc/Kidney/kidney.jip"
  projectCount: 50
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "path-to-annotation-simple": "Annotate with file/directory name #1"
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name #2"
    edges:
      - {"from":"Annotate with file/directory name #1","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name #2","toSlot":"Paths","dataClass":"file"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> List files -> Annotate with file/directory name"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> List files -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"6592f5ca-7d68-5692-8c98-c6f8e8bc375c\",\"jipipe:node:name\":\"Annotate with file/directory name #1\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"6592f5ca-7d68-5692-8c98-c6f8e8bc375c\",\"jipipe:node:name\":\"List files\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"6592f5ca-7d68-5692-8c98-c6f8e8bc375c\",\"jipipe:node:name\":\"Annotate with file/directory name #2\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Files\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Annotate with file/directory name -> List files -> Annotate with file/directory name

Mined pattern `conversion-path-to-annotation-simple-folder-list-files-path-to-annotation-simple` (shape `conversion`,
support 50).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| annotate | `path-to-annotation-simple` |
| import   | `folder-list-files`         |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                     | name                                 |
|-----|-----------------------------|--------------------------------------|
| n0  | `path-to-annotation-simple` | Annotate with file/directory name #1 |
| n1  | `folder-list-files`         | List files                           |
| n2  | `path-to-annotation-simple` | Annotate with file/directory name #2 |

| from                                 | slot              | to                                   | slot      | data class |
|--------------------------------------|-------------------|--------------------------------------|-----------|------------|
| Annotate with file/directory name #1 | `Annotated paths` | List files                           | `Folders` | `path`     |
| List files                           | `Files`           | Annotate with file/directory name #2 | `Paths`   | `file`     |

## Template

Proposed name `Annotate with file/directory name -> List files -> Annotate with file/directory name`, menu path
`Patterns/Annotations/Annotate with file/directory name -> List files -> Annotate with file/directory name` (proposal
only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-path-to-annotation-simple-folder-list-files-path-to-annotation-simple.json`.

