---
audience: "tbd"
modality: "imagej-imgplus-greyscale-8u -> imagej-imgplus-greyscale-mask -> imagej-imgplus"
tags: [Images]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_04_Kidney_Compartments/kidney_annotated.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_05_Batch_Processing_Kidney/kidney.jip"
    - "AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Analysis"
    project: "AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip"
    nodes:
      - "ij1-threshold-auto2d": "Auto threshold 2D"
      - "ij1-morph-operation2d": "Morphological operation 2D: Dilate"
      - "ij1-morph-binary-fillholes2d": "Morphological hole filling 2D"
    edges:
      - {"from":"Auto threshold 2D","fromSlot":"Output","to":"Morphological operation 2D: Dilate","toSlot":"Input","dataClass":"imagej-imgplus-greyscale-mask"}
      - {"from":"Morphological operation 2D: Dilate","fromSlot":"Output","to":"Morphological hole filling 2D","toSlot":"Input","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D"
  menuPath: "Patterns/Images/Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"ij1-threshold-auto2d\",\"jipipe:graph-compartment\":\"bea5256b-5df7-58b8-be72-5228e3d12f2a\",\"jipipe:node:name\":\"Auto threshold 2D\"},\"n1\":{\"jipipe:node-info-id\":\"ij1-morph-operation2d\",\"jipipe:graph-compartment\":\"bea5256b-5df7-58b8-be72-5228e3d12f2a\",\"jipipe:node:name\":\"Morphological operation 2D: Dilate\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-morph-binary-fillholes2d\",\"jipipe:graph-compartment\":\"bea5256b-5df7-58b8-be72-5228e3d12f2a\",\"jipipe:node:name\":\"Morphological hole filling 2D\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D

Mined pattern `chain-3-ij1-threshold-auto2d-ij1-morph-operation2d-ij1-morph-binary-fillholes2d` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `ij1-threshold-auto2d` |
| other | `ij1-morph-operation2d` |
| other | `ij1-morph-binary-fillholes2d` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/JSC_2025_03_Kidney_Basic/kidney.jip` (relative to `/data/JIPipe`), compartment `Analysis`.

| key | node id | name |
|---|---|---|
| n0 | `ij1-threshold-auto2d` | Auto threshold 2D |
| n1 | `ij1-morph-operation2d` | Morphological operation 2D: Dilate |
| n2 | `ij1-morph-binary-fillholes2d` | Morphological hole filling 2D |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Auto threshold 2D | `Output` | Morphological operation 2D: Dilate | `Input` | `imagej-imgplus-greyscale-mask` |
| Morphological operation 2D: Dilate | `Output` | Morphological hole filling 2D | `Input` | `imagej-imgplus` |

## Template

Proposed name `Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D`, menu path `Patterns/Images/Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-ij1-threshold-auto2d-ij1-morph-operation2d-ij1-morph-binary-fillholes2d.json`.

