---
audience: "tbd"
modality: "file -> imagej-roi -> imagej-imgplus -> imagej-imgplus-greyscale-mask -> jipipe:data"
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
      - "import-imagej-roi-from-file": "ROI from file"
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "ij1-morph-operation2d": "Morphological operation 2D"
      - "annotate-with-data": "Annotate with data"
    edges:
      - {"from":"ROI from file","fromSlot":"Output","to":"Convert ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
      - {"from":"Convert ROI to mask","fromSlot":"Output","to":"Morphological operation 2D","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Morphological operation 2D","fromSlot":"Output","to":"Annotate with data","toSlot":"Annotation","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data"
  menuPath: "Patterns/Data Sources/Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"838d7054-6226-55db-b7ac-c9fecb3f5d04\",\"jipipe:node:name\":\"ROI from file\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"838d7054-6226-55db-b7ac-c9fecb3f5d04\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"838d7054-6226-55db-b7ac-c9fecb3f5d04\",\"jipipe:node:name\":\"Morphological operation 2D\"},\"n3\":{\"jipipe:node-info-id\":\"annotate-with-data\",\"jipipe:graph-compartment\":\"838d7054-6226-55db-b7ac-c9fecb3f5d04\",\"jipipe:node:name\":\"Annotate with data\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Annotation\"}]}"
---

# Pattern: Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data

Mined pattern `chain-4-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d-annotate-with-data` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| import | `import-imagej-roi-from-file` |
| other | `ij1-roi-to-mask` |
| other | `ij1-morph-operation2d` |
| annotate | `annotate-with-data` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip` (relative to `/data/JIPipe`), compartment `Cellpose training`.

| key | node id | name |
|---|---|---|
| n0 | `import-imagej-roi-from-file` | ROI from file |
| n1 | `ij1-roi-to-mask` | Convert ROI to mask |
| n2 | `ij1-morph-operation2d` | Morphological operation 2D |
| n3 | `annotate-with-data` | Annotate with data |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| ROI from file | `Output` | Convert ROI to mask | `ROI` | `imagej-roi` |
| Convert ROI to mask | `Output` | Morphological operation 2D | `Input` | `imagej-imgplus-greyscale-mask` |
| Morphological operation 2D | `Output` | Annotate with data | `Annotation` | `imagej-imgplus` |

## Template

Proposed name `Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data`, menu path `Patterns/Data Sources/Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-import-imagej-roi-from-file-ij1-roi-to-mask-ij1-morph-operation2d-annotate-with-data.json`.

