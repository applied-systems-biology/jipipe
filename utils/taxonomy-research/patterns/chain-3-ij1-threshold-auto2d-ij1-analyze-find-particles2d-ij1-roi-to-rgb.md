---
audience: "tbd"
modality: "imagej-imgplus-greyscale-8u -> imagej-imgplus-greyscale-mask -> imagej-roi -> imagej-results-table -> imagej-imgplus -> imagej-imgplus-color-rgb"
tags: [Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_02_Blobs_Single/blobs.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney_annotated.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Blobs/blobs.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Analysis"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_02_Blobs_Single/blobs.jip"
    nodes:
      - "ij1-threshold-auto2d": "Auto threshold 2D"
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
    edges:
      - {"from":"Auto threshold 2D","fromSlot":"Output","to":"Find particles 2D","toSlot":"Mask","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Find particles 2D","fromSlot":"ROI","to":"Convert ROI to RGB","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Images/Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-threshold-auto2d\",\"jipipe:graph-compartment\":\"7231adcd-3a21-5f06-a7ab-a51752aba94a\",\"jipipe:node:name\":\"Auto threshold 2D\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"7231adcd-3a21-5f06-a7ab-a51752aba94a\",\"jipipe:node:name\":\"Find particles 2D\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"7231adcd-3a21-5f06-a7ab-a51752aba94a\",\"jipipe:node:name\":\"Convert ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Mask\"},{\"source-node\":\"n1\",\"source-slot\":\"ROI\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB

Mined pattern `chain-3-ij1-threshold-auto2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb` (shape `chain`, support 30).

## Role sequence

| role  | node ids                       |
|-------|--------------------------------|
| other | `ij1-threshold-auto2d`         |
| other | `ij1-analyze-find-particles2d` |
| other | `ij1-roi-to-rgb`               |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_02_Blobs_Single/blobs.jip` (relative to `/data/JIPipe`), compartment
`Analysis`.

| key | node id                        | name               |
|-----|--------------------------------|--------------------|
| n0  | `ij1-threshold-auto2d`         | Auto threshold 2D  |
| n1  | `ij1-analyze-find-particles2d` | Find particles 2D  |
| n2  | `ij1-roi-to-rgb`               | Convert ROI to RGB |

| from              | slot     | to                 | slot   | data class                      |
|-------------------|----------|--------------------|--------|---------------------------------|
| Auto threshold 2D | `Output` | Find particles 2D  | `Mask` | `imagej-imgplus-greyscale-mask` |
| Find particles 2D | `ROI`    | Convert ROI to RGB | `ROI`  | `imagej-roi`                    |

## Template

Proposed name `Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB`, menu path
`Patterns/Images/Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-ij1-threshold-auto2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb.json`.

