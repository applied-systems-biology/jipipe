---
audience: "tbd"
modality: "folder -> jipipe:data"
tags: [Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 40
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 40
  exampleGraph:
    compartment: "Preprocessing and edges"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "project-user-directory": "Project user directory"
      - "jipipe:comment": "Comment"
      - "node-group": "READ: folders, subfolders and files, with subfolder output"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"READ: folders, subfolders and files, with subfolder output","toSlot":"Input folder","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Group fans in from 3 source ids (roles: import, other)"
  menuPath: "Patterns/Graph / Project/Group fans in from 3 source ids (roles: import, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"project-user-directory\",\"jipipe:graph-compartment\":\"8dff7c49-873f-5d3b-b437-7b17412a7f60\",\"jipipe:node:name\":\"Project user directory\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"8dff7c49-873f-5d3b-b437-7b17412a7f60\",\"jipipe:node:name\":\"Comment\"},\"n2\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"8dff7c49-873f-5d3b-b437-7b17412a7f60\",\"jipipe:node:name\":\"READ: folders, subfolders and files, with subfolder output\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Comment\",\"target-node\":\"n2\",\"target-slot\":\"Input folder\"}]}"
---

# Pattern: Group fans in from 3 source ids (roles: import, other)

Mined pattern `fan-in-node-group-import-1-other-1` (shape `fan-in`, support 40).

## Role sequence

| role   | node ids                                       |
|--------|------------------------------------------------|
| other  | `node-group`                                   |
| import | `import-folder-list`, `project-user-directory` |
| other  | `jipipe:comment`                               |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing and edges`.

| key | node id                  | name                                                       |
|-----|--------------------------|------------------------------------------------------------|
| n0  | `project-user-directory` | Project user directory                                     |
| n1  | `jipipe:comment`         | Comment                                                    |
| n2  | `node-group`             | READ: folders, subfolders and files, with subfolder output |

| from    | slot      | to                                                         | slot           | data class    |
|---------|-----------|------------------------------------------------------------|----------------|---------------|
| Comment | `Comment` | READ: folders, subfolders and files, with subfolder output | `Input folder` | `jipipe:data` |

## Template

Proposed name `Group fans in from 3 source ids (roles: import, other)`, menu path
`Patterns/Graph / Project/Group fans in from 3 source ids (roles: import, other)` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-node-group-import-1-other-1.json`.

