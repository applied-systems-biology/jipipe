---
audience: "tbd"
modality: "jipipe:data -> file -> imagej-ome"
tags: [Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 36
  projects:
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_06.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
  projectCount: 36
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    nodes:
      - "jipipe:comment": "Comment"
      - "node-group": "READ: folders, subfolders and files"
      - "import-imagej-bioformats": "Bio-Formats importer"
    edges:
      - {"from":"Comment","fromSlot":"Comment","to":"READ: folders, subfolders and files","toSlot":"Input folder","dataClass":"jipipe:data"}
      - {"from":"READ: folders, subfolders and files","fromSlot":"Annotated paths","to":"Bio-Formats importer","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "Comment -> Group -> Bio-Formats importer"
  menuPath: "Patterns/Graph / Project/Comment -> Group -> Bio-Formats importer"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:comment\",\"jipipe:graph-compartment\":\"96f0e760-8674-555f-abac-737985dc9c83\",\"jipipe:node:name\":\"Comment\"},\"n1\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"96f0e760-8674-555f-abac-737985dc9c83\",\"jipipe:node:name\":\"READ: folders, subfolders and files\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"96f0e760-8674-555f-abac-737985dc9c83\",\"jipipe:node:name\":\"Bio-Formats importer\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Comment\",\"target-node\":\"n1\",\"target-slot\":\"Input folder\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Comment -> Group -> Bio-Formats importer

Mined pattern `chain-3-jipipe-comment-node-group-import-imagej-bioformats` (shape `chain`, support 36).

## Role sequence

| role   | node ids                   |
|--------|----------------------------|
| other  | `jipipe:comment`           |
| other  | `node-group`               |
| import | `import-imagej-bioformats` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` (relative to `/data/JIPipe`),
compartment `Preprocessing`.

| key | node id                    | name                                |
|-----|----------------------------|-------------------------------------|
| n0  | `jipipe:comment`           | Comment                             |
| n1  | `node-group`               | READ: folders, subfolders and files |
| n2  | `import-imagej-bioformats` | Bio-Formats importer                |

| from                                | slot              | to                                  | slot           | data class    |
|-------------------------------------|-------------------|-------------------------------------|----------------|---------------|
| Comment                             | `Comment`         | READ: folders, subfolders and files | `Input folder` | `jipipe:data` |
| READ: folders, subfolders and files | `Annotated paths` | Bio-Formats importer                | `Input`        | `path`        |

## Template

Proposed name `Comment -> Group -> Bio-Formats importer`, menu path
`Patterns/Graph / Project/Comment -> Group -> Bio-Formats importer` (proposal only - not registered). `data` = the
minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-jipipe-comment-node-group-import-imagej-bioformats.json`.

