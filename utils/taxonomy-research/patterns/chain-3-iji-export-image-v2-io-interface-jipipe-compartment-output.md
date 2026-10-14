---
audience: "tbd"
modality: "imagej-imgplus -> file"
tags: [Export, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 26
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    - "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
  projectCount: 26
  exampleGraph:
    compartment: "Nodules"
    project: "AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip"
    nodes:
      - "iji-export-image-v2": "Export image"
      - "io-interface": "IO Interface: save HSB histograms per image and per condition"
      - "jipipe:compartment-output": "Raw nodules from direct ROI medium size"
    edges:
      - {"from":"Export image","fromSlot":"Exported file","to":"IO Interface: save HSB histograms per image and per condition","toSlot":"Data 6","dataClass":"file"}
      - {"from":"IO Interface: save HSB histograms per image and per condition","fromSlot":"Data","to":"Raw nodules from direct ROI medium size","toSlot":"Save color histograms","dataClass":"file"}
  templateCandidate: true
template:
  name: "Export image -> IO Interface -> Compartment output"
  menuPath: "Patterns/Export/Export image -> IO Interface -> Compartment output"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"iji-export-image-v2\",\"jipipe:graph-compartment\":\"4c500761-8f6e-5ec8-aa3d-0cc41728301d\",\"jipipe:node:name\":\"Export image\"},\"n1\":{\"jipipe:node-info-id\":\"io-interface\",\"jipipe:graph-compartment\":\"4c500761-8f6e-5ec8-aa3d-0cc41728301d\",\"jipipe:node:name\":\"IO Interface: save HSB histograms per image and per condition\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"4c500761-8f6e-5ec8-aa3d-0cc41728301d\",\"jipipe:node:name\":\"Raw nodules from direct ROI medium size\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Exported file\",\"target-node\":\"n1\",\"target-slot\":\"Data 6\"},{\"source-node\":\"n1\",\"source-slot\":\"Data\",\"target-node\":\"n2\",\"target-slot\":\"Save color histograms\"}]}"
---

# Pattern: Export image -> IO Interface -> Compartment output

Mined pattern `chain-3-iji-export-image-v2-io-interface-jipipe-compartment-output` (shape `chain`, support 26).

## Role sequence

| role | node ids |
|---|---|
| export | `iji-export-image-v2` |
| core-io | `io-interface` |
| core-io | `jipipe:compartment-output` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Angela_Roots_38.jip` (relative to `/data/JIPipe`), compartment `Nodules`.

| key | node id | name |
|---|---|---|
| n0 | `iji-export-image-v2` | Export image |
| n1 | `io-interface` | IO Interface: save HSB histograms per image and per condition |
| n2 | `jipipe:compartment-output` | Raw nodules from direct ROI medium size |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Export image | `Exported file` | IO Interface: save HSB histograms per image and per condition | `Data 6` | `file` |
| IO Interface: save HSB histograms per image and per condition | `Data` | Raw nodules from direct ROI medium size | `Save color histograms` | `file` |

## Template

Proposed name `Export image -> IO Interface -> Compartment output`, menu path `Patterns/Export/Export image -> IO Interface -> Compartment output` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-iji-export-image-v2-io-interface-jipipe-compartment-output.json`.

