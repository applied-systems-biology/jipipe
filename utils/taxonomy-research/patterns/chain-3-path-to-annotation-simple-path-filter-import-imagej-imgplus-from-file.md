---
audience: "tbd"
modality: "path -> file -> imagej-imgplus"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_feet.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_08_skirt.jip"
    - "AIAgentTests/03-ExampleWorkflows/Clara_09.jip"
  projectCount: 26
  exampleGraph:
    compartment: "File Handling"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
    nodes:
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "path-filter": "Filter paths"
      - "import-imagej-imgplus-from-file": "Import image"
    edges:
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"Import image","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Filter paths -> Import image"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Filter paths -> Import image"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"aee03a11-0562-5770-a023-7a1fd1a5adb2\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n1\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"aee03a11-0562-5770-a023-7a1fd1a5adb2\",\"jipipe:node:name\":\"Filter paths\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"aee03a11-0562-5770-a023-7a1fd1a5adb2\",\"jipipe:node:name\":\"Import image\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Files\"}]}"
---

# Pattern: Annotate with file/directory name -> Filter paths -> Import image

Mined pattern `chain-3-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file` (shape `chain`, support
26).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| annotate | `path-to-annotation-simple`       |
| import   | `path-filter`                     |
| import   | `import-imagej-imgplus-from-file` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip` (relative to `/data/JIPipe`), compartment
`File Handling`.

| key | node id                           | name                              |
|-----|-----------------------------------|-----------------------------------|
| n0  | `path-to-annotation-simple`       | Annotate with file/directory name |
| n1  | `path-filter`                     | Filter paths                      |
| n2  | `import-imagej-imgplus-from-file` | Import image                      |

| from                              | slot              | to           | slot    | data class |
|-----------------------------------|-------------------|--------------|---------|------------|
| Annotate with file/directory name | `Annotated paths` | Filter paths | `Input` | `path`     |
| Filter paths                      | `Output`          | Import image | `Files` | `path`     |

## Template

Proposed name `Annotate with file/directory name -> Filter paths -> Import image`, menu path
`Patterns/Annotations/Annotate with file/directory name -> Filter paths -> Import image` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-path-to-annotation-simple-path-filter-import-imagej-imgplus-from-file.json`.

