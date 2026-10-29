---
audience: "tbd"
modality: "folder -> file -> imagej-ome"
tags: [Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 54
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Paco_06.jip"
  projectCount: 54
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "project-user-directory": "Project user directory"
      - "node-group": "READ: folders, subfolders and files"
      - "import-imagej-bioformats": "Bio-Formats importer"
    edges:
      - {"from":"Project user directory","fromSlot":"Output","to":"READ: folders, subfolders and files","toSlot":"Input folder","dataClass":"folder"}
      - {"from":"READ: folders, subfolders and files","fromSlot":"Annotated paths","to":"Bio-Formats importer","toSlot":"Input","dataClass":"path"}
  templateCandidate: true
template:
  name: "Project user path -> Group -> Bio-Formats importer"
  menuPath: "Patterns/Data Sources/Project user path -> Group -> Bio-Formats importer"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"project-user-directory\",\"jipipe:graph-compartment\":\"e50ecfd6-bb21-5614-863b-521672292254\",\"jipipe:node:name\":\"Project user directory\"},\"n1\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"e50ecfd6-bb21-5614-863b-521672292254\",\"jipipe:node:name\":\"READ: folders, subfolders and files\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"e50ecfd6-bb21-5614-863b-521672292254\",\"jipipe:node:name\":\"Bio-Formats importer\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input folder\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"}]}"
---

# Pattern: Project user path -> Group -> Bio-Formats importer

Mined pattern `chain-3-project-user-directory-node-group-import-imagej-bioformats` (shape `chain`, support 54).

## Role sequence

| role   | node ids                   |
|--------|----------------------------|
| import | `project-user-directory`   |
| other  | `node-group`               |
| import | `import-imagej-bioformats` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                    | name                                |
|-----|----------------------------|-------------------------------------|
| n0  | `project-user-directory`   | Project user directory              |
| n1  | `node-group`               | READ: folders, subfolders and files |
| n2  | `import-imagej-bioformats` | Bio-Formats importer                |

| from                                | slot              | to                                  | slot           | data class |
|-------------------------------------|-------------------|-------------------------------------|----------------|------------|
| Project user directory              | `Output`          | READ: folders, subfolders and files | `Input folder` | `folder`   |
| READ: folders, subfolders and files | `Annotated paths` | Bio-Formats importer                | `Input`        | `path`     |

## Template

Proposed name `Project user path -> Group -> Bio-Formats importer`, menu path
`Patterns/Data Sources/Project user path -> Group -> Bio-Formats importer` (proposal only — not registered). `data` =
the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-3-project-user-directory-node-group-import-imagej-bioformats.json`.

