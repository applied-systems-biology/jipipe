---
audience: "tbd"
modality: "path -> file -> imagej-roi -> imagej-imgplus -> imagej-imgplus-greyscale-mask"
tags: [Annotations, Data Sources, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Cellpose training"
    project: "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    nodes:
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "ij1-morph-operation2d": "Morphological operation 2D"
    edges:
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Convert ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Convert ROI to mask","fromSlot":"Output","to":"Morphological operation 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"d5b721ff-d8bb-557b-a338-6948da34d099\",\"jipipe:node:name\":\"Add path to annotations\"},\"n1\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"d5b721ff-d8bb-557b-a338-6948da34d099\",\"jipipe:node:name\":\"ROI from file\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"d5b721ff-d8bb-557b-a338-6948da34d099\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"d5b721ff-d8bb-557b-a338-6948da34d099\",\"jipipe:node:name\":\"Morphological operation 2D\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Files\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D

Mined pattern `chain-4-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| annotate | `path-to-annotation-simple` |
| import | `import-imagej-roi-from-file` |
| other | `ij1-roi-to-mask` |
| other | `ij1-morph-operation2d` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment `Cellpose training`.

| key | node id | name |
|---|---|---|
| n0 | `path-to-annotation-simple` | Add path to annotations |
| n1 | `import-imagej-roi-from-file` | ROI from file |
| n2 | `ij1-roi-to-mask` | Convert ROI to mask |
| n3 | `ij1-morph-operation2d` | Morphological operation 2D |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add path to annotations | `Annotated paths` | ROI from file | `Files` | `path` |
| ROI from file | `Output` | Convert ROI to mask | `ROI` | `imagej-roi` |
| Convert ROI to mask | `Output` | Morphological operation 2D | `Input` | `imagej-imgplus-greyscale-mask` |

## Template

Proposed name `Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D`, menu path `Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d.json`.

