---
audience: "tbd"
modality: "path -> jipipe:data"
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
      - "path-to-annotation-simple": "Add path to annotations"
      - "jipipe:comment": "Comment"
      - "path-filter": "Filter paths"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"Filter paths","toSlot":"Input","dataClass":"jipipe:data"}
  templateCandidate: true
template:
  name: "Filter paths fans in from 3 source ids (roles: annotate, other)"
  menuPath: "Patterns/Data Sources/Filter paths fans in from 3 source ids (roles: annotate, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"c617865e-777a-525a-bbe6-f4f4aaf3a622\",\"jipipe:node:name\":\"Add path to annotations\"},\"n1\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"c617865e-777a-525a-bbe6-f4f4aaf3a622\",\"jipipe:node:name\":\"Comment\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"c617865e-777a-525a-bbe6-f4f4aaf3a622\",\"jipipe:node:name\":\"Filter paths\"}},\"edges\":[{\"source-node\":\"n1\",\"source-slot\":\"Comment\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Filter paths fans in from 3 source ids (roles: annotate, other)

Mined pattern `fan-in-path-filter-annotate-1-other-1` (shape `fan-in`, support 26).

## Role sequence

| role     | node ids                                    |
|----------|---------------------------------------------|
| import   | `path-filter`                               |
| annotate | `annotate-set`, `path-to-annotation-simple` |
| other    | `jipipe:comment`                            |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip` (relative to `/data/JIPipe`), compartment `File handler`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `path-to-annotation-simple` | Add path to annotations |
| n1  | `jipipe:comment`            | Comment                 |
| n2  | `path-filter`               | Filter paths            |

| from    | slot      | to           | slot    | data class    |
|---------|-----------|--------------|---------|---------------|
| Comment | `Comment` | Filter paths | `Input` | `jipipe:data` |

## Template

Proposed name `Filter paths fans in from 3 source ids (roles: annotate, other)`, menu path
`Patterns/Data Sources/Filter paths fans in from 3 source ids (roles: annotate, other)` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-in-path-filter-annotate-1-other-1.json`.

