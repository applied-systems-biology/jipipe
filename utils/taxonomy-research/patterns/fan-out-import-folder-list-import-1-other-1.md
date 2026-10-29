---
audience: "tbd"
modality: "folder -> jipipe:data"
tags: [Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "import-folder-list": "Folder list"
      - "folder-list-subfolders": "List subfolders"
      - "jipipe:comment": "Comment"
    edges:
      - {"from":"Folder list","fromSlot":"Folder paths","to":"List subfolders","toSlot":"Folders","dataClass":"folder"}
  templateCandidate: true
template:
  name: "Folder list fans out to 2 target ids (roles: import, other)"
  menuPath: "Patterns/Data Sources/Folder list fans out to 2 target ids (roles: import, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-folder-list\",\"jipipe:graph-compartment\":\"b9ae5049-19cf-571b-9af0-47c7499162ca\",\"jipipe:node:name\":\"Folder list\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"b9ae5049-19cf-571b-9af0-47c7499162ca\",\"jipipe:node:name\":\"List subfolders\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"b9ae5049-19cf-571b-9af0-47c7499162ca\",\"jipipe:node:name\":\"Comment\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Folder paths\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Folder list fans out to 2 target ids (roles: import, other)

Mined pattern `fan-out-import-folder-list-import-1-other-1` (shape `fan-out`, support 26).

## Role sequence

| role   | node ids                 |
|--------|--------------------------|
| import | `import-folder-list`     |
| import | `folder-list-subfolders` |
| other  | `jipipe:comment`         |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id                  | name            |
|-----|--------------------------|-----------------|
| n0  | `import-folder-list`     | Folder list     |
| n1  | `folder-list-subfolders` | List subfolders |
| n2  | `jipipe:comment`         | Comment         |

| from        | slot           | to              | slot      | data class |
|-------------|----------------|-----------------|-----------|------------|
| Folder list | `Folder paths` | List subfolders | `Folders` | `folder`   |

## Template

Proposed name `Folder list fans out to 2 target ids (roles: import, other)`, menu path
`Patterns/Data Sources/Folder list fans out to 2 target ids (roles: import, other)` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-import-folder-list-import-1-other-1.json`.

