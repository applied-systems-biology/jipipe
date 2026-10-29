---
audience: "tbd"
modality: "folder -> path"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 32
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
  projectCount: 32
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "folder-list-subfolders": "List subfolders"
      - "path-to-annotation-simple": "Add path to annotations"
      - "path-filter": "Filter paths"
    edges:
      - {"from":"List subfolders","fromSlot":"Subfolders","to":"Add path to annotations","toSlot":"Paths","dataClass":"folder"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "List subfolders -> Annotate with file/directory name -> Filter paths"
  menuPath: "Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-subfolders\",\"jipipe:graph-compartment\":\"c2d1e935-9529-56df-b998-c2be567e1076\",\"jipipe:node:name\":\"List subfolders\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"c2d1e935-9529-56df-b998-c2be567e1076\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"c2d1e935-9529-56df-b998-c2be567e1076\",\"jipipe:node:name\":\"Filter paths\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Subfolders\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: List subfolders -> Annotate with file/directory name -> Filter paths

Mined pattern `chain-3-folder-list-subfolders-path-to-annotation-simple-path-filter` (shape `chain`, support 32).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `folder-list-subfolders`    |
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `folder-list-subfolders`    | List subfolders         |
| n1  | `path-to-annotation-simple` | Add path to annotations |
| n2  | `path-filter`               | Filter paths            |

| from                    | slot              | to                      | slot    | data class |
|-------------------------|-------------------|-------------------------|---------|------------|
| List subfolders         | `Subfolders`      | Add path to annotations | `Paths` | `folder`   |
| Add path to annotations | `Annotated paths` | Filter paths            | `Input` | `path`     |

## Template

Proposed name `List subfolders -> Annotate with file/directory name -> Filter paths`, menu path
`Patterns/Data Sources/List subfolders -> Annotate with file/directory name -> Filter paths` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-folder-list-subfolders-path-to-annotation-simple-path-filter.json`.

