---
audience: "tbd"
modality: "file -> imagej-ome -> imagej-imgplus"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 44
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
  projectCount: 44
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "node-group": "READ: folders, subfolders and files"
      - "import-imagej-bioformats": "Bio-Formats importer"
      - "image-properties-to-annotation": "Annotate with image properties"
    edges:
      - {"from":"READ: folders, subfolders and files","fromSlot":"Annotated paths","to":"Bio-Formats importer","toSlot":"Input","dataClass":"path"}
      - {"from":"Bio-Formats importer","fromSlot":"Output","to":"Annotate with image properties","toSlot":"Image","dataClass":"imagej-ome"}
  templateCandidate: true
template:
  name: "Group -> Bio-Formats importer -> Annotate with image properties"
  menuPath: "Patterns/Graph / Project/Group -> Bio-Formats importer -> Annotate with image properties"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"add8b443-f5f6-56c2-8428-c6df0285ab4b\",\"jipipe:node:name\":\"READ: folders, subfolders and files\"},\"n1\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"add8b443-f5f6-56c2-8428-c6df0285ab4b\",\"jipipe:node:name\":\"Bio-Formats importer\"},\"n2\":{\"jipipe:node-info-id\":\"image-properties-to-annotation\",\"jipipe:graph-compartment\":\"add8b443-f5f6-56c2-8428-c6df0285ab4b\",\"jipipe:node:name\":\"Annotate with image properties\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n1\",\"target-slot\":\"Input\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Group -> Bio-Formats importer -> Annotate with image properties

Mined pattern `chain-3-node-group-import-imagej-bioformats-image-properties-to-annotation` (shape `chain`, support 44).

## Role sequence

| role | node ids |
|---|---|
| other | `node-group` |
| import | `import-imagej-bioformats` |
| annotate | `image-properties-to-annotation` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `node-group` | READ: folders, subfolders and files |
| n1 | `import-imagej-bioformats` | Bio-Formats importer |
| n2 | `image-properties-to-annotation` | Annotate with image properties |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| READ: folders, subfolders and files | `Annotated paths` | Bio-Formats importer | `Input` | `path` |
| Bio-Formats importer | `Output` | Annotate with image properties | `Image` | `imagej-ome` |

## Template

Proposed name `Group -> Bio-Formats importer -> Annotate with image properties`, menu path `Patterns/Graph / Project/Group -> Bio-Formats importer -> Annotate with image properties` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-3-node-group-import-imagej-bioformats-image-properties-to-annotation.json`.

