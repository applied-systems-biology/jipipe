---
audience: "tbd"
modality: "folder -> file -> path -> imagej-roi"
tags: [Annotations, Data Sources, Graph / Project]
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
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Output","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Import ROI -> Compartment output"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"5e0f555d-ed7d-59a1-8fbd-a828f8cd2799\",\"jipipe:node:name\":\"List ZIP\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"5e0f555d-ed7d-59a1-8fbd-a828f8cd2799\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"5e0f555d-ed7d-59a1-8fbd-a828f8cd2799\",\"jipipe:node:name\":\"ROI from file\"},\"n3\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"5e0f555d-ed7d-59a1-8fbd-a828f8cd2799\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Files\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Import ROI -> Compartment output

Mined pattern
`chain-4-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output` (shape
`chain`, support 32).

## Role sequence

| role     | node ids                      |
|----------|-------------------------------|
| import   | `folder-list-files`           |
| annotate | `path-to-annotation-simple`   |
| import   | `import-imagej-roi-from-file` |
| core-io  | `jipipe:compartment-output`   |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                       | name                    |
|-----|-------------------------------|-------------------------|
| n0  | `folder-list-files`           | List ZIP                |
| n1  | `path-to-annotation-simple`   | Add path to annotations |
| n2  | `import-imagej-roi-from-file` | ROI from file           |
| n3  | `jipipe:compartment-output`   | Output                  |

| from                    | slot              | to                      | slot    | data class   |
|-------------------------|-------------------|-------------------------|---------|--------------|
| List ZIP                | `Files`           | Add path to annotations | `Paths` | `file`       |
| Add path to annotations | `Annotated paths` | ROI from file           | `Files` | `path`       |
| ROI from file           | `Output`          | Output                  | `ROI`   | `imagej-roi` |

## Template

Proposed name `List files -> Annotate with file/directory name -> Import ROI -> Compartment output`, menu path
`Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI -> Compartment output` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output.json`.

