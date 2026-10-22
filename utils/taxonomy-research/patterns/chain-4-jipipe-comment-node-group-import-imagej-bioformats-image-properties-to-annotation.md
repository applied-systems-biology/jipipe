---
audience: "tbd"
modality: "jipipe:data -> file -> imagej-ome -> imagej-imgplus"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 28
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
  projectCount: 28
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    nodes:
      - "jipipe:comment": "Comment"
      - "node-group": "READ: folders, subfolders and files"
      - "import-imagej-bioformats": "Bio-Formats importer"
      - "image-properties-to-annotation": "Annotate with image properties"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"READ: folders, subfolders and files","toSlot":"Input folder","dataClass":"jipipe:data"}
      - {"from":"READ: folders, subfolders and files","fromSlot":"Annotated paths","to":"Bio-Formats importer","toSlot":"Input","dataClass":"path"}
      - {"from":"Bio-Formats importer","fromSlot":"Output","to":"Annotate with image properties","toSlot":"Image","dataClass":"imagej-ome"}
  templateCandidate: true
template:
  name: "Comment -> Group -> Bio-Formats importer -> Annotate with image properties"
  menuPath: "Patterns/Graph / Project/Comment -> Group -> Bio-Formats importer -> Annotate with image properties"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"cc0195f0-47ee-5f87-8f3f-668b0c9b0edb\",\"jipipe:node:name\":\"Comment\"},\"n1\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"cc0195f0-47ee-5f87-8f3f-668b0c9b0edb\",\"jipipe:node:name\":\"READ: folders, subfolders and files\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"cc0195f0-47ee-5f87-8f3f-668b0c9b0edb\",\"jipipe:node:name\":\"Bio-Formats importer\"},\"n3\":{\"jipipe:node-info-id\":\"image-properties-to-annotation\",\"jipipe:graph-compartment\":\"cc0195f0-47ee-5f87-8f3f-668b0c9b0edb\",\"jipipe:node:name\":\"Annotate with image properties\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Comment\",\"target-node\":\"n1\",\"target-slot\":\"Input folder\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Comment -> Group -> Bio-Formats importer -> Annotate with image properties

Mined pattern `chain-4-jipipe-comment-node-group-import-imagej-bioformats-image-properties-to-annotation` (shape `chain`, support 28).

## Role sequence

| role | node ids |
|---|---|
| other | `jipipe:comment` |
| other | `node-group` |
| import | `import-imagej-bioformats` |
| annotate | `image-properties-to-annotation` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` (relative to `/data/JIPipe`), compartment `Preprocessing`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:comment` | Comment |
| n1 | `node-group` | READ: folders, subfolders and files |
| n2 | `import-imagej-bioformats` | Bio-Formats importer |
| n3 | `image-properties-to-annotation` | Annotate with image properties |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Comment | `Comment` | READ: folders, subfolders and files | `Input folder` | `jipipe:data` |
| READ: folders, subfolders and files | `Annotated paths` | Bio-Formats importer | `Input` | `path` |
| Bio-Formats importer | `Output` | Annotate with image properties | `Image` | `imagej-ome` |

## Template

Proposed name `Comment -> Group -> Bio-Formats importer -> Annotate with image properties`, menu path `Patterns/Graph / Project/Comment -> Group -> Bio-Formats importer -> Annotate with image properties` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/chain-4-jipipe-comment-node-group-import-imagej-bioformats-image-properties-to-annotation.json`.

