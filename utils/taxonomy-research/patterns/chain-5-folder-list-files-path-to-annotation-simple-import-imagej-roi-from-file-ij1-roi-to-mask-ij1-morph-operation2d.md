---
audience: "tbd"
modality: "folder -> file -> path -> imagej-roi -> imagej-imgplus -> imagej-imgplus-greyscale-mask"
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
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations"
      - "import-imagej-roi-from-file": "ROI from file"
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "ij1-morph-operation2d": "Morphological operation 2D"
    edges:
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Convert ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Convert ROI to mask","fromSlot":"Output","to":"Morphological operation 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"722182bd-ccf1-5a42-8995-c50a8862905e\",\"jipipe:node:name\":\"List ZIP\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"722182bd-ccf1-5a42-8995-c50a8862905e\",\"jipipe:node:name\":\"Add path to annotations\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"722182bd-ccf1-5a42-8995-c50a8862905e\",\"jipipe:node:name\":\"ROI from file\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"722182bd-ccf1-5a42-8995-c50a8862905e\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n4\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"722182bd-ccf1-5a42-8995-c50a8862905e\",\"jipipe:node:name\":\"Morphological operation 2D\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Files\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"ROI\"},{\"source-node\":\"n3\",\"source-slot\":\"Output\",\"target-node\":\"n4\",\"target-slot\":\"Input\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D

Mined pattern
`chain-5-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d`
(shape `chain`, support 26).

## Role sequence

| role     | node ids                      |
|----------|-------------------------------|
| import   | `folder-list-files`           |
| annotate | `path-to-annotation-simple`   |
| import   | `import-imagej-roi-from-file` |
| other    | `ij1-roi-to-mask`             |
| other    | `ij1-morph-operation2d`       |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment
`Cellpose training`.

| key | node id                       | name                       |
|-----|-------------------------------|----------------------------|
| n0  | `folder-list-files`           | List ZIP                   |
| n1  | `path-to-annotation-simple`   | Add path to annotations    |
| n2  | `import-imagej-roi-from-file` | ROI from file              |
| n3  | `ij1-roi-to-mask`             | Convert ROI to mask        |
| n4  | `ij1-morph-operation2d`       | Morphological operation 2D |

| from                    | slot              | to                         | slot    | data class                      |
|-------------------------|-------------------|----------------------------|---------|---------------------------------|
| List ZIP                | `Files`           | Add path to annotations    | `Paths` | `file`                          |
| Add path to annotations | `Annotated paths` | ROI from file              | `Files` | `path`                          |
| ROI from file           | `Output`          | Convert ROI to mask        | `ROI`   | `imagej-roi`                    |
| Convert ROI to mask     | `Output`          | Morphological operation 2D | `Input` | `imagej-imgplus-greyscale-mask` |

## Template

Proposed name
`List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D`,
menu path
`Patterns/Data Sources/List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-5-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d.json`.

