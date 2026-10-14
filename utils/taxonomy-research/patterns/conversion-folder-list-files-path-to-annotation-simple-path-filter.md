---
audience: "tbd"
modality: "folder -> file -> path"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 38
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_07_Kidney_Table_Annotation_Processing/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_08_ROI_Labels_Filaments/Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_09_Misc/Kidney/kidney.jip"
  projectCount: 38
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "path-filter": "Filter paths"
    edges:
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name","toSlot":"Paths","dataClass":"file"}
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Filter paths"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Filter paths"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"f4157daf-fc0d-57df-be0a-9efdf0c45139\",\"jipipe:node:name\":\"List files\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"f4157daf-fc0d-57df-be0a-9efdf0c45139\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"f4157daf-fc0d-57df-be0a-9efdf0c45139\",\"jipipe:node:name\":\"Filter paths\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Filter paths

Mined pattern `conversion-folder-list-files-path-to-annotation-simple-path-filter` (shape `conversion`, support 38).

## Role sequence

| role | node ids |
|---|---|
| import | `folder-list-files` |
| annotate | `path-to-annotation-simple` |
| import | `path-filter` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment `File Handling`.

| key | node id | name |
|---|---|---|
| n0 | `folder-list-files` | List files |
| n1 | `path-to-annotation-simple` | Annotate with file/directory name |
| n2 | `path-filter` | Filter paths |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| List files | `Files` | Annotate with file/directory name | `Paths` | `file` |
| Annotate with file/directory name | `Annotated paths` | Filter paths | `Input` | `path` |

## Template

Proposed name `List files -> Annotate with file/directory name -> Filter paths`, menu path `Patterns/Data Sources/List files -> Annotate with file/directory name -> Filter paths` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-folder-list-files-path-to-annotation-simple-path-filter.json`.

