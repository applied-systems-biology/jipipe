---
audience: "tbd"
modality: "file -> imagej-ome -> imagej-imgplus"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 6
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/BloodVessels_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/Paul_Kerstin_CellTrack_13.jip"
    - "Example workflows/BloodVessels_04.jip"
  projectCount: 6
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "import-imagej-bioformats": "Bio-Formats importer"
      - "image-properties-to-annotation": "Annotate with image properties"
      - "jipipe:compartment-output": "Output"
    edges:
      - {"from":"Bio-Formats importer","fromSlot":"Output","to":"Annotate with image properties","toSlot":"Image","dataClass":"imagej-ome"}
      - {"from":"Annotate with image properties","fromSlot":"Annotated image","to":"Output","toSlot":"Raw","dataClass":"imagej-imgplus"}
  templateCandidate: true
template:
  name: "Bio-Formats importer -> Annotate with image properties -> core-io"
  menuPath: "Patterns/Data Sources/Bio-Formats importer -> Annotate with image properties -> core-io"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"6e8e0d6f-37e1-541b-9999-64a28159b2bc\",\"jipipe:node:name\":\"Bio-Formats importer\"},\"n1\":{\"jipipe:node-info-id\":\"image-properties-to-annotation\",\"jipipe:graph-compartment\":\"6e8e0d6f-37e1-541b-9999-64a28159b2bc\",\"jipipe:node:name\":\"Annotate with image properties\"},\"n2\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"6e8e0d6f-37e1-541b-9999-64a28159b2bc\",\"jipipe:node:name\":\"Output\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Image\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated image\",\"target-node\":\"n2\",\"target-slot\":\"Raw\"}]}"
---

# Pattern: Bio-Formats importer -> Annotate with image properties -> core-io

Mined pattern `annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-core-io` (shape
`annotation-sandwich`, support 6).

## Role sequence

| role     | node ids                         |
|----------|----------------------------------|
| import   | `import-imagej-bioformats`       |
| annotate | `image-properties-to-annotation` |
| core-io  | `jipipe:compartment-output`      |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                          | name                           |
|-----|----------------------------------|--------------------------------|
| n0  | `import-imagej-bioformats`       | Bio-Formats importer           |
| n1  | `image-properties-to-annotation` | Annotate with image properties |
| n2  | `jipipe:compartment-output`      | Output                         |

| from                           | slot              | to                             | slot    | data class       |
|--------------------------------|-------------------|--------------------------------|---------|------------------|
| Bio-Formats importer           | `Output`          | Annotate with image properties | `Image` | `imagej-ome`     |
| Annotate with image properties | `Annotated image` | Output                         | `Raw`   | `imagej-imgplus` |

## Template

Proposed name `Bio-Formats importer -> Annotate with image properties -> core-io`, menu path
`Patterns/Data Sources/Bio-Formats importer -> Annotate with image properties -> core-io` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/annotation-sandwich-import-imagej-bioformats-image-properties-to-annotation-core-io.json`.

