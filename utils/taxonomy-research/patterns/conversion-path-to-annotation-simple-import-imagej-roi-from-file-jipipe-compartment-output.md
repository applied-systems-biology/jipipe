---
audience: "tbd"
modality: "path -> file -> imagej-roi"
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
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Output","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Import ROI -> Compartment output"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"14cf279c-24cc-51db-ad81-dc3d1aa62305\",\"jipipe:node:name\":\"Add path to annotations\"},\"n1\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"14cf279c-24cc-51db-ad81-dc3d1aa62305\",\"jipipe:node:name\":\"ROI from file\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"14cf279c-24cc-51db-ad81-dc3d1aa62305\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Files\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Annotate with file/directory name -> Import ROI -> Compartment output

Mined pattern `conversion-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output` (shape `conversion`, support 32).

## Role sequence

| role | node ids |
|---|---|
| annotate | `path-to-annotation-simple` |
| import | `import-imagej-roi-from-file` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `path-to-annotation-simple` | Add path to annotations |
| n1 | `import-imagej-roi-from-file` | ROI from file |
| n2 | `jipipe:compartment-output` | Output |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add path to annotations | `Annotated paths` | ROI from file | `Files` | `path` |
| ROI from file | `Output` | Output | `ROI` | `imagej-roi` |

## Template

Proposed name `Annotate with file/directory name -> Import ROI -> Compartment output`, menu path `Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/conversion-path-to-annotation-simple-import-imagej-roi-from-file-jipipe-compartment-output.json`.

