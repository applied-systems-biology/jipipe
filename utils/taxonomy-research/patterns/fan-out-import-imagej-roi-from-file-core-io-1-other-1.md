---
audience: "tbd"
modality: "file -> imagej-roi -> imagej-imgplus -> imagej-imgplus-greyscale-mask"
tags: [Data Sources, Graph / Project, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 34
  projects:
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
  projectCount: 34
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    nodes:
      - "import-imagej-roi-from-file": "ROI from file"
      - "ij1-roi-to-mask": "Convert ROI to mask"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"ROI from file","fromSlot":"Output","to":"Convert ROI to mask","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Import ROI fans out to 2 target ids (roles: core-io, other)"
  menuPath: "Patterns/Data Sources/Import ROI fans out to 2 target ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-imagej-roi-from-file\",\"jipipe:graph-compartment\":\"a2a8d965-b568-5e75-9468-9084ce2b861d\",\"jipipe:node:name\":\"ROI from file\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-roi-to-mask\",\"jipipe:graph-compartment\":\"a2a8d965-b568-5e75-9468-9084ce2b861d\",\"jipipe:node:name\":\"Convert ROI to mask\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"a2a8d965-b568-5e75-9468-9084ce2b861d\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Import ROI fans out to 2 target ids (roles: core-io, other)

Mined pattern `fan-out-import-imagej-roi-from-file-core-io-1-other-1` (shape `fan-out`, support 34).

## Role sequence

| role    | node ids                      |
|---------|-------------------------------|
| import  | `import-imagej-roi-from-file` |
| core-io | `jipipe:compartment-output`   |
| other   | `ij1-roi-to-mask`             |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id                       | name                |
|-----|-------------------------------|---------------------|
| n0  | `import-imagej-roi-from-file` | ROI from file       |
| n1  | `ij1-roi-to-mask`             | Convert ROI to mask |
| n2  | `jipipe:compartment-output`   | Output              |

| from          | slot     | to                  | slot  | data class   |
|---------------|----------|---------------------|-------|--------------|
| ROI from file | `Output` | Convert ROI to mask | `ROI` | `imagej-roi` |

## Template

Proposed name `Import ROI fans out to 2 target ids (roles: core-io, other)`, menu path
`Patterns/Data Sources/Import ROI fans out to 2 target ids (roles: core-io, other)` (proposal only — not registered).
`data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/fan-out-import-imagej-roi-from-file-core-io-1-other-1.json`.

