---
audience: "tbd"
modality: "file -> path -> cellpose-model-v2"
tags: [Annotations, Data Sources, Machine learning]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 4
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "Example workflows/Angela_Roots_38.jip"
    - "Example workflows/Angela_Roots_38_withHanddrawnROIs.jip"
  exampleGraph:
    compartment: "Nodules"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "import-file-list": "File list"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-cellpose-model-v2": "Import Cellpose model from file"
    edges:
      - {"from":"File list","fromSlot":"Filenames","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"Import Cellpose model from file","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "File list -> Annotate with file/directory name -> other"
  menuPath: "Patterns/Data Sources/File list -> Annotate with file/directory name -> other"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-file-list\",\"jipipe:graph-compartment\":\"bf359a57-6b14-510e-92c4-78d2ff3c53cd\",\"jipipe:node:name\":\"File list\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"bf359a57-6b14-510e-92c4-78d2ff3c53cd\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"import-cellpose-model-v2\",\"jipipe:graph-compartment\":\"bf359a57-6b14-510e-92c4-78d2ff3c53cd\",\"jipipe:node:name\":\"Import Cellpose model from file\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Filenames\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: File list -> Annotate with file/directory name -> other

Mined pattern `annotation-sandwich-import-file-list-path-to-annotation-simple-other` (shape `annotation-sandwich`,
support 4).

## Role sequence

| role     | node ids                    |
|----------|-----------------------------|
| import   | `import-file-list`          |
| annotate | `path-to-annotation-simple` |
| other    | `import-cellpose-model-v2`  |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Nodules`.

| key | node id                     | name                            |
|-----|-----------------------------|---------------------------------|
| n0  | `import-file-list`          | File list                       |
| n1  | `path-to-annotation-simple` | Add path to annotations         |
| n2  | `import-cellpose-model-v2`  | Import Cellpose model from file |

| from                    | slot              | to                              | slot    | data class |
|-------------------------|-------------------|---------------------------------|---------|------------|
| File list               | `Filenames`       | Add path to annotations         | `Paths` | `file`     |
| Add path to annotations | `Annotated paths` | Import Cellpose model from file | `Input` | `path`     |

## Template

Proposed name `File list -> Annotate with file/directory name -> other`, menu path
`Patterns/Data Sources/File list -> Annotate with file/directory name -> other` (proposal only - not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/annotation-sandwich-import-file-list-path-to-annotation-simple-other.json`.

