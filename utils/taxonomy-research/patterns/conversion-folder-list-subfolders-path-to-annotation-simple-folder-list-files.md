---
audience: "tbd"
modality: "folder -> path -> file"
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
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "folder-list-files": "List files"
    edges:
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Annotate with file/directory name","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"List files","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "List subfolders -> Annotate with file/directory name -> List files"
  menuPath: "Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"e2953391-6a8c-5e63-adcc-01e580513b8e\",\"jipipe:node:name\":\"List subfolders\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"e2953391-6a8c-5e63-adcc-01e580513b8e\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"e2953391-6a8c-5e63-adcc-01e580513b8e\",\"jipipe:node:name\":\"List files\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Subfolders\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: List subfolders -> Annotate with file/directory name -> List files

Mined pattern `conversion-folder-list-subfolders-path-to-annotation-simple-folder-list-files` (shape `conversion`, support 50).

## Role sequence

| role | node ids |
|---|---|
| import | `folder-list-subfolders` |
| annotate | `path-to-annotation-simple` |
| import | `folder-list-files` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment `File Handling`.

| key | node id | name |
|---|---|---|
| n0 | `folder-list-subfolders` | List subfolders |
| n1 | `path-to-annotation-simple` | Annotate with file/directory name |
| n2 | `folder-list-files` | List files |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| List subfolders | `Subfolders` | Annotate with file/directory name | `Paths` | `folder` |
| Annotate with file/directory name | `Annotated paths` | List files | `Folders` | `path` |

## Template

Proposed name `List subfolders -> Annotate with file/directory name -> List files`, menu path `Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> List files` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-folder-list-subfolders-path-to-annotation-simple-folder-list-files.json`.

