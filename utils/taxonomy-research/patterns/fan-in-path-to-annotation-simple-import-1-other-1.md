---
audience: "tbd"
modality: "path -> folder -> jipipe:data"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip"
  projectCount: 26
  exampleGraph:
    compartment: "File handler"
    project: "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    nodes:
      - "project-user-directory": "Project user directory"
      - "jipipe:comment": "Comment"
      - "path-to-annotation-simple": "Add path to annotations"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"Add path to annotations","toSlot":"Paths","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name fans in from 3 source ids (roles: import, other)"
  menuPath: "Patterns/Annotations/Annotate with file/directory name fans in from 3 source ids (roles: import, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"project-user-directory\",\"jipipe:graph-compartment\":\"218e6663-69ac-5a07-a4d6-7dd058fc266c\",\"jipipe:node:name\":\"Project user directory\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"218e6663-69ac-5a07-a4d6-7dd058fc266c\",\"jipipe:node:name\":\"Comment\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"218e6663-69ac-5a07-a4d6-7dd058fc266c\",\"jipipe:node:name\":\"Add path to annotations\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Comment\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"}]}"
---

# Pattern: Annotate with file/directory name fans in from 3 source ids (roles: import, other)

Mined pattern `fan-in-path-to-annotation-simple-import-1-other-1` (shape `fan-in`, support 26).

## Role sequence

| role     | node ids                                       |
|----------|------------------------------------------------|
| annotate | `path-to-annotation-simple`                    |
| import   | `import-folder-list`, `project-user-directory` |
| other    | `jipipe:comment`                               |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `project-user-directory`    | Project user directory  |
| n1  | `jipipe:comment`            | Comment                 |
| n2  | `path-to-annotation-simple` | Add path to annotations |

| from    | slot      | to                      | slot    | data class    |
|---------|-----------|-------------------------|---------|---------------|
| Comment | `Comment` | Add path to annotations | `Paths` | `jipipe:data` |

## Template

Proposed name `Annotate with file/directory name fans in from 3 source ids (roles: import, other)`, menu path
`Patterns/Annotations/Annotate with file/directory name fans in from 3 source ids (roles: import, other)` (proposal
only - not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-path-to-annotation-simple-import-1-other-1.json`.

