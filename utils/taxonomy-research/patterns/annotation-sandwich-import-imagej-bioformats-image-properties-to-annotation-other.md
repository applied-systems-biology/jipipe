---
audience: "tbd"
modality: "file -> imagej-ome -> imagej-imgplus -> path -> imagej-roi -> imagej-results-table -> imagej-imgplus-greyscale"
tags: [Annotations, Data Sources, Images, Miscellaneous]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 16
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnAgarPad_Kerstin_26.jip"
  projectCount: 16
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "import-imagej-bioformats": "Bio-Formats importer"
      - "image-properties-to-annotation": "Annotate with image properties"
      - "ij1-dimensions-hyper-stacksplitter": "Split by dimension"
    edges:
      - {"from":"Bio-Formats importer","fromSlot":"Output","to":"Annotate with image properties","toSlot":"Image","dataClass":"imagej-ome"}
      - {"from":"Annotate with image properties","fromSlot":"Annotated image","to":"Split by dimension","toSlot":"Input","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Bio-Formats importer -> Annotate with image properties -> other"
  menuPath: "Patterns/Data Sources/Bio-Formats importer -> Annotate with image properties -> other"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"be0aa7fa-f1a5-5586-ad62-dba951a249b8\",\"jipipe:node:name\":\"Bio-Formats importer\"},\"n1\":{\"jipipe:node-info-id\":\"image-properties-to-annotation\",\"jipipe:graph-compartment\":\"be0aa7fa-f1a5-5586-ad62-dba951a249b8\",\"jipipe:node:name\":\"Annotate with image properties\"},\"n2\":{\"jipipe:node-info-id\":\"ij1-dimensions-hyper-stacksplitter\",\"jipipe:graph-compartment\":\"be0aa7fa-f1a5-5586-ad62-dba951a249b8\",\"jipipe:node:name\":\"Split by dimension\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Image\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated image\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Bio-Formats importer -> Annotate with image properties -> other

Mined pattern `annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-other` (shape
`annotation-sandwich`, support 16).

## Role sequence

| role     | node ids                                                                                                                               |
|----------|----------------------------------------------------------------------------------------------------------------------------------------|
| import   | `import-imagej-bioformats`                                                                                                             |
| annotate | `image-properties-to-annotation`                                                                                                       |
| other    | `external-imagej-macro`, `ij1-blur-median2d`, `ij1-color-split-channels-2`, `ij1-dimensions-hyper-stacksplitter`, `ij1-fft-bandpass2d` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                              | name                           |
|-----|--------------------------------------|--------------------------------|
| n0  | `import-imagej-bioformats`           | Bio-Formats importer           |
| n1  | `image-properties-to-annotation`     | Annotate with image properties |
| n2  | `ij1-dimensions-hyper-stacksplitter` | Split by dimension             |

| from                           | slot              | to                             | slot    | data class       |
|--------------------------------|-------------------|--------------------------------|---------|------------------|
| Bio-Formats importer           | `Output`          | Annotate with image properties | `Image` | `imagej-ome`     |
| Annotate with image properties | `Annotated image` | Split by dimension             | `Input` | `imagej-imgplus` |

## Template

Proposed name `Bio-Formats importer -> Annotate with image properties -> other`, menu path
`Patterns/Data Sources/Bio-Formats importer -> Annotate with image properties -> other` (proposal only - not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-other.json`.

