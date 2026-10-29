---
audience: "tbd"
modality: "imagej-imgplus-greyscale-mask -> imagej-imgplus -> imagej-roi -> imagej-results-table -> imagej-imgplus-color-rgb"
tags: [Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 34
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney_annotated.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
  projectCount: 34
  exampleGraph:
    compartment: "Analysis"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip"
    nodes:
      - "ij1-morph-binary-fillholes2d": "Morphological hole filling 2D"
      - "ij1-morph-operation2d": "Morphological operation 2D: Erode"
      - "ij1-analyze-find-particles2d": "Find particles 2D"
      - "ij1-roi-to-rgb": "Convert ROI to RGB"
    edges:
      - {"from":"Morphological hole filling 2D","fromSlot":"Output","to":"Morphological operation 2D: Erode","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Morphological operation 2D: Erode","fromSlot":"Output","to":"Find particles 2D","toSlot":"Mask","dataClass":"imagej-imgplus"}
      - {"from":"Find particles 2D","fromSlot":"ROI","to":"Convert ROI to RGB","toSlot":"ROI","dataClass":"imagej-roi"}
  templateCandidate: true
template:
  name: "Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB"
  menuPath: "Patterns/Images/Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-morph-binary-fillholes2d\",\"jipipe:graph-compartment\":\"54a1eca1-98a3-56b1-834a-d5561e346d15\",\"jipipe:node:name\":\"Morphological hole filling 2D\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"54a1eca1-98a3-56b1-834a-d5561e346d15\",\"jipipe:node:name\":\"Morphological operation 2D: Erode\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-analyze-find-particles2d\",\"jipipe:graph-compartment\":\"54a1eca1-98a3-56b1-834a-d5561e346d15\",\"jipipe:node:name\":\"Find particles 2D\"},\"n3\":{\"jipipe:node-info-id\":\"ij1-roi-to-rgb\",\"jipipe:graph-compartment\":\"54a1eca1-98a3-56b1-834a-d5561e346d15\",\"jipipe:node:name\":\"Convert ROI to RGB\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Mask\"},{\"source-node\":\"n2\",\"source-slot\":\"ROI\",\"target-node\":\"n3\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB

Mined pattern `chain-4-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb`
(shape `chain`, support 34).

## Role sequence

| role  | node ids                       |
|-------|--------------------------------|
| other | `ij1-morph-binary-fillholes2d` |
| other | `ij1-morph-operation2d`        |
| other | `ij1-analyze-find-particles2d` |
| other | `ij1-roi-to-rgb`               |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip` (relative to `/data/JIPipe`), compartment
`Analysis`.

| key | node id                        | name                              |
|-----|--------------------------------|-----------------------------------|
| n0  | `ij1-morph-binary-fillholes2d` | Morphological hole filling 2D     |
| n1  | `ij1-morph-operation2d`        | Morphological operation 2D: Erode |
| n2  | `ij1-analyze-find-particles2d` | Find particles 2D                 |
| n3  | `ij1-roi-to-rgb`               | Convert ROI to RGB                |

| from                              | slot     | to                                | slot    | data class                      |
|-----------------------------------|----------|-----------------------------------|---------|---------------------------------|
| Morphological hole filling 2D     | `Output` | Morphological operation 2D: Erode | `Input` | `imagej-imgplus-greyscale-mask` |
| Morphological operation 2D: Erode | `Output` | Find particles 2D                 | `Mask`  | `imagej-imgplus`                |
| Find particles 2D                 | `ROI`    | Convert ROI to RGB                | `ROI`   | `imagej-roi`                    |

## Template

Proposed name
`Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB`, menu path
`Patterns/Images/Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB`
(proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-ij1-morph-binary-fillholes2d-ij1-morph-operation2d-ij1-analyze-find-particles2d-ij1-roi-to-rgb.json`.

