---
audience: "tbd"
modality: "path -> file -> imagej-imgplus"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 25
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/CellposeTraining.jip"
    - "AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip"
  projectCount: 25
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    nodes:
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "import-imagej-imgplus-from-file": "Import image"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"Import image","toSlot":"Files","dataClass":"path"}
      - {"from":"Import image","fromSlot":"Image","to":"Output","toSlot":"Raw","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Import image -> Compartment output"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Import image -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"18a2fcb6-c5e1-5315-87c7-b8031ddfe0c9\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n1\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"18a2fcb6-c5e1-5315-87c7-b8031ddfe0c9\",\"jipipe:node:name\":\"Import image\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"18a2fcb6-c5e1-5315-87c7-b8031ddfe0c9\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Files\"},{\"source-node\":\"n1\",\"source-slot\":\"Image\",\"target-node\":\"n2\",\"target-slot\":\"Raw\"}]}"
---

# Pattern: Annotate with file/directory name -> Import image -> Compartment output

Mined pattern `conversion-path-to-annotation-simple-import-imagej-imgplus-from-file-jipipe-compartment-output` (shape
`conversion`, support 25).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| annotate | `path-to-annotation-simple`       |
| import   | `import-imagej-imgplus-from-file` |
| core-io  | `jipipe:compartment-output`       |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip` (relative to `/data/JIPipe`),
compartment `File Handling`.

| key | node id                           | name                              |
|-----|-----------------------------------|-----------------------------------|
| n0  | `path-to-annotation-simple`       | Annotate with file/directory name |
| n1  | `import-imagej-imgplus-from-file` | Import image                      |
| n2  | `jipipe:compartment-output`       | Output                            |

| from                              | slot              | to           | slot    | data class       |
|-----------------------------------|-------------------|--------------|---------|------------------|
| Annotate with file/directory name | `Annotated paths` | Import image | `Files` | `path`           |
| Import image                      | `Image`           | Output       | `Raw`   | `imagej-imgplus` |

## Template

Proposed name `Annotate with file/directory name -> Import image -> Compartment output`, menu path
`Patterns/Annotations/Annotate with file/directory name -> Import image -> Compartment output` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-path-to-annotation-simple-import-imagej-imgplus-from-file-jipipe-compartment-output.json`.

