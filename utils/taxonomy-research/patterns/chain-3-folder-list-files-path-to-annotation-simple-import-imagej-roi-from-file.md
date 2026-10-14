---
audience: "tbd"
modality: "folder -> file -> path -> imagej-roi"
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
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
    edges:
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Import ROI"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"5bf674df-e195-527e-a56c-ad320a921613\",\"jipipe:node:name\":\"List ZIP\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"5bf674df-e195-527e-a56c-ad320a921613\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"5bf674df-e195-527e-a56c-ad320a921613\",\"jipipe:node:name\":\"ROI from file\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Files\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Import ROI

Mined pattern `chain-3-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file` (shape `chain`, support 32).

## Role sequence

| role | node ids |
|---|---|
| import | `folder-list-files` |
| annotate | `path-to-annotation-simple` |
| import | `import-imagej-roi-from-file` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `folder-list-files` | List ZIP |
| n1 | `path-to-annotation-simple` | Add path to annotations |
| n2 | `import-imagej-roi-from-file` | ROI from file |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| List ZIP | `Files` | Add path to annotations | `Paths` | `file` |
| Add path to annotations | `Annotated paths` | ROI from file | `Files` | `path` |

## Template

Proposed name `List files -> Annotate with file/directory name -> Import ROI`, menu path `Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file.json`.

