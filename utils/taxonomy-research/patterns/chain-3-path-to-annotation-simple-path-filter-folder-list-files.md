---
audience: "tbd"
modality: "path -> folder -> file"
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
      - "path-to-annotation-simple": "Add path to annotations"
      - "path-filter": "Filter paths"
      - "folder-list-files": "List TIFF or JPG"
    edges:
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"List TIFF or JPG","toSlot":"Folders","dataClass":"path"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Filter paths -> List files"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Filter paths -> List files"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"b8895ef9-517b-58b6-9a81-f1980fb9cc46\",\"jipipe:node:name\":\"Add path to annotations\"},\"n1\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"b8895ef9-517b-58b6-9a81-f1980fb9cc46\",\"jipipe:node:name\":\"Filter paths\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"b8895ef9-517b-58b6-9a81-f1980fb9cc46\",\"jipipe:node:name\":\"List TIFF or JPG\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"}]}"
---

# Pattern: Annotate with file/directory name -> Filter paths -> List files

Mined pattern `chain-3-path-to-annotation-simple-path-filter-folder-list-files` (shape `chain`, support 32).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| annotate | `path-to-annotation-simple` |
| import   | `path-filter`               |
| import   | `folder-list-files`         |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                     | name                    |
|-----|-----------------------------|-------------------------|
| n0  | `path-to-annotation-simple` | Add path to annotations |
| n1  | `path-filter`               | Filter paths            |
| n2  | `folder-list-files`         | List TIFF or JPG        |

| from                    | slot              | to               | slot      | data class |
|-------------------------|-------------------|------------------|-----------|------------|
| Add path to annotations | `Annotated paths` | Filter paths     | `Input`   | `path`     |
| Filter paths            | `Output`          | List TIFF or JPG | `Folders` | `path`     |

## Template

Proposed name `Annotate with file/directory name -> Filter paths -> List files`, menu path
`Patterns/Annotations/Annotate with file/directory name -> Filter paths -> List files` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-path-to-annotation-simple-path-filter-folder-list-files.json`.

