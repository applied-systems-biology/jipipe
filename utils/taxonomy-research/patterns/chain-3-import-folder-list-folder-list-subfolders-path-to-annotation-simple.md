---
audience: "tbd"
modality: "folder -> path"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 51
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/lecture_06.jip"
    - "AIAgentTests/01-BasicWorkshop/lecture_06_custom_dirs.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
  projectCount: 51
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "import-folder-list": "Folder list"
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Annotate with file/directory name"
    edges:
      - {"from":"Folder list","fromSlot":"Folder paths","to":"List subfolders","toSlot":"Folders","dataClass":"folder"}
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Annotate with file/directory name","toSlot":"Paths","dataClass":"folder"}
  templateCandidate: true
template:
  name: "Folder list -> List subfolders -> Annotate with file/directory name"
  menuPath: "Patterns/Data Sources/Folder list -> List subfolders -> Annotate with file/directory name"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-folder-list\",\"jipipe:graph-compartment\":\"a81573fb-1fe8-5edd-8ca3-5c3053692e7f\",\"jipipe:node:name\":\"Folder list\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"a81573fb-1fe8-5edd-8ca3-5c3053692e7f\",\"jipipe:node:name\":\"List subfolders\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"a81573fb-1fe8-5edd-8ca3-5c3053692e7f\",\"jipipe:node:name\":\"Annotate with file/directory name\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Folder paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Subfolders\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Folder list -> List subfolders -> Annotate with file/directory name

Mined pattern `chain-3-import-folder-list-folder-list-subfolders-path-to-annotation-simple` (shape `chain`, support 51).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `import-folder-list`        |
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                     | name                              |
|-----|-----------------------------|-----------------------------------|
| n0  | `import-folder-list`        | Folder list                       |
| n1  | `folder-list-subfolders`    | List subfolders                   |
| n2  | `path-to-annotation-simple` | Annotate with file/directory name |

| from            | slot           | to                                | slot      | data class |
|-----------------|----------------|-----------------------------------|-----------|------------|
| Folder list     | `Folder paths` | List subfolders                   | `Folders` | `folder`   |
| List subfolders | `Subfolders`   | Annotate with file/directory name | `Paths`   | `folder`   |

## Template

Proposed name `Folder list -> List subfolders -> Annotate with file/directory name`, menu path
`Patterns/Data Sources/Folder list -> List subfolders -> Annotate with file/directory name` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-import-folder-list-folder-list-subfolders-path-to-annotation-simple.json`.

