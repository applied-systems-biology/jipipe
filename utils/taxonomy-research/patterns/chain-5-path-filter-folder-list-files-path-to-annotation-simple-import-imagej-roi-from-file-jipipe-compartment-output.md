---
audience: "tbd"
modality: "path -> folder -> file -> imagej-roi"
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
      - "path-filter": "Filter paths"
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Filter paths","fromSlot":"Output","to":"List ZIP","toSlot":"Folders","dataClass":"path"}
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Output","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Compartment output"
  menuPath: "Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"a7ddd1c6-fa70-5614-bac5-fcb971373225\",\"jipipe:node:name\":\"Filter paths\"},\"n1\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"a7ddd1c6-fa70-5614-bac5-fcb971373225\",\"jipipe:node:name\":\"List ZIP\"},\"n2\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"a7ddd1c6-fa70-5614-bac5-fcb971373225\",\"jipipe:node:name\":\"Add path to annotations\"},\"n3\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"a7ddd1c6-fa70-5614-bac5-fcb971373225\",\"jipipe:node:name\":\"ROI from file\"},\"n4\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a7ddd1c6-fa70-5614-bac5-fcb971373225\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Folders\"},{\"source-node\":\"n1\",\"source-slot\":\"Files\",\"target-node\":\"n2\",\"target-slot\":\"Paths\"},{\"source-node\":\"n2\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n3\",\"target-slot\":\"Files\"},{\"source-node\":\"n3\",\"source-slot\":\"Output\",\"target-node\":\"n4\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Compartment output

Mined pattern `chain-5-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output` (shape `chain`, support 32).

## Role sequence

| role | node ids |
|---|---|
| import | `path-filter` |
| import | `folder-list-files` |
| annotate | `path-to-annotation-simple` |
| import | `import-imagej-roi-from-file` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `path-filter` | Filter paths |
| n1 | `folder-list-files` | List ZIP |
| n2 | `path-to-annotation-simple` | Add path to annotations |
| n3 | `import-imagej-roi-from-file` | ROI from file |
| n4 | `jipipe:compartment-output` | Output |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Filter paths | `Output` | List ZIP | `Folders` | `path` |
| List ZIP | `Files` | Add path to annotations | `Paths` | `file` |
| Add path to annotations | `Annotated paths` | ROI from file | `Files` | `path` |
| ROI from file | `Output` | Output | `ROI` | `imagej-roi` |

## Template

Proposed name `Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Compartment output`, menu path `Patterns/Data Sources/Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-5-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output.json`.

