---
audience: "tbd"
modality: "path -> file -> imagej-roi -> imagej-imgplus -> imagej-imgplus-greyscale-mask"
tags: [Annotations, Data Sources, ROIs]
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
      - "ij1-roi-to-mask": "Convert ROI to mask"
    edges:
      - {"from":"Add path to annotations","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
      - {"from":"ROI from file","fromSlot":"Output","to":"Convert ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"83e9bd10-8f08-5d6e-83d3-d575f5c1a9c8\",\"jipipe:node:name\":\"Add path to annotations\"},\"n1\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"83e9bd10-8f08-5d6e-83d3-d575f5c1a9c8\",\"jipipe:node:name\":\"ROI from file\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"83e9bd10-8f08-5d6e-83d3-d575f5c1a9c8\",\"jipipe:node:name\":\"Convert ROI to mask\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Files\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask

Mined pattern `conversion-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask` (shape `conversion`,
support 32).

## Role sequence

| role     | node ids                      |
|----------|-------------------------------|
| annotate | `path-to-annotation-simple`   |
| import   | `import-imagej-roi-from-file` |
| other    | `ij1-roi-to-mask`             |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                       | name                    |
|-----|-------------------------------|-------------------------|
| n0  | `path-to-annotation-simple`   | Add path to annotations |
| n1  | `import-imagej-roi-from-file` | ROI from file           |
| n2  | `ij1-roi-to-mask`             | Convert ROI to mask     |

| from                    | slot              | to                  | slot    | data class   |
|-------------------------|-------------------|---------------------|---------|--------------|
| Add path to annotations | `Annotated paths` | ROI from file       | `Files` | `path`       |
| ROI from file           | `Output`          | Convert ROI to mask | `ROI`   | `imagej-roi` |

## Template

Proposed name `Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask`, menu path
`Patterns/Annotations/Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-path-to-annotation-simple-import-imagej-roi-from-file-ij1-roi-to-mask.json`.

