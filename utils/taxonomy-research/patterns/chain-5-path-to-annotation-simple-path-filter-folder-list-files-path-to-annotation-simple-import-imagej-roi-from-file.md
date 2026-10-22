---
audience: "tbd"
modality: "path -> folder -> file -> imagej-roi"
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
      - "path-to-annotation-simple": "Add path to annotations #1"
      - "path-filter": "Filter paths"
      - "folder-list-files": "List ZIP"
      - "path-to-annotation-simple": "Add path to annotations #2"
      - "import-imagej-roi-from-file": "ROI from file"
    edges:
      - {"from":"Add path to annotations #1","fromSlot":"Annotated paths","to":"Filter paths","toSlot":"Input","dataClass":"path"}
      - {"from":"Filter paths","fromSlot":"Output","to":"List ZIP","toSlot":"Folders","dataClass":"path"}
      - {"from":"List ZIP","fromSlot":"Files","to":"Add path to annotations #2","toSlot":"Paths","dataClass":"file"}
      - {"from":"Add path to annotations #2","fromSlot":"Annotated paths","to":"ROI from file","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI"
  menuPath: "Patterns/Annotations/Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"88adb327-906f-5448-93c6-a80fe53ad65f\",\"jipipe:node:name\":\"Add path to annotations #1\"},\"n1\":{\"jipipe:node-info-id\":\"path-filter\",\"jipipe:graph-compartment\":\"88adb327-906f-5448-93c6-a80fe53ad65f\",\"jipipe:node:name\":\"Filter paths\"},\"n2\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"88adb327-906f-5448-93c6-a80fe53ad65f\",\"jipipe:node:name\":\"List ZIP\"},\"n3\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"88adb327-906f-5448-93c6-a80fe53ad65f\",\"jipipe:node:name\":\"Add path to annotations #2\"},\"n4\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"88adb327-906f-5448-93c6-a80fe53ad65f\",\"jipipe:node:name\":\"ROI from file\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Folders\"},{\"source-node\":\"n2\",\"source-slot\":\"Files\",\"target-node\":\"n3\",\"target-slot\":\"Paths\"},{\"source-node\":\"n3\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n4\",\"target-slot\":\"Files\"}]}"
---

# Pattern: Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI

Mined pattern `chain-5-path-to-annotation-simple-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file` (shape `chain`, support 32).

## Role sequence

| role | node ids |
|---|---|
| annotate | `path-to-annotation-simple` |
| import | `path-filter` |
| import | `folder-list-files` |
| annotate | `path-to-annotation-simple` |
| import | `import-imagej-roi-from-file` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `path-to-annotation-simple` | Add path to annotations #1 |
| n1 | `path-filter` | Filter paths |
| n2 | `folder-list-files` | List ZIP |
| n3 | `path-to-annotation-simple` | Add path to annotations #2 |
| n4 | `import-imagej-roi-from-file` | ROI from file |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Add path to annotations #1 | `Annotated paths` | Filter paths | `Input` | `path` |
| Filter paths | `Output` | List ZIP | `Folders` | `path` |
| List ZIP | `Files` | Add path to annotations #2 | `Paths` | `file` |
| Add path to annotations #2 | `Annotated paths` | ROI from file | `Files` | `path` |

## Template

Proposed name `Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI`, menu path `Patterns/Annotations/Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-5-path-to-annotation-simple-path-filter-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file.json`.

