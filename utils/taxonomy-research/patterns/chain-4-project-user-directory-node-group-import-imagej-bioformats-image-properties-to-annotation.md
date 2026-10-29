---
audience: "tbd"
modality: "folder -> file -> imagej-ome -> imagej-imgplus"
tags: [Annotations, Data Sources, Graph / Project]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 40
  projects:
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/CellTracking_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip"
    - "AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17_remapped.jip"
    - "AIAgentTests/03-ExampleWorkflows/AdrianFeile_VibrioOnChip_04.jip"
  projectCount: 40
  exampleGraph:
    compartment: "Preprocessing"
    project: "AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip"
    nodes:
      - "project-user-directory": "Project user directory"
      - "node-group": "READ: folders, subfolders and files"
      - "import-imagej-bioformats": "Bio-Formats importer"
      - "image-properties-to-annotation": "Annotate with image properties"
    edges:
      - {"from":"Project user directory","fromSlot":"Output","to":"READ: folders, subfolders and files","toSlot":"Input folder","dataClass":"folder"}
      - {"from":"READ: folders, subfolders and files","fromSlot":"Annotated paths","to":"Bio-Formats importer","toSlot":"Input","dataClass":"path"}
      - {"from":"Bio-Formats importer","fromSlot":"Output","to":"Annotate with image properties","toSlot":"Image","dataClass":"imagej-ome"}
  templateCandidate: true
template:
  name: "Project user path -> Group -> Bio-Formats importer -> Annotate with image properties"
  menuPath: "Patterns/Data Sources/Project user path -> Group -> Bio-Formats importer -> Annotate with image properties"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"project-user-directory\",\"jipipe:graph-compartment\":\"7618ab1a-4aa3-5528-9668-1eee21b7df22\",\"jipipe:node:name\":\"Project user directory\"},\"n1\":{\"jipipe:node-info-id\":\"node-group\",\"jipipe:graph-compartment\":\"7618ab1a-4aa3-5528-9668-1eee21b7df22\",\"jipipe:node:name\":\"READ: folders, subfolders and files\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-bioformats\",\"jipipe:graph-compartment\":\"7618ab1a-4aa3-5528-9668-1eee21b7df22\",\"jipipe:node:name\":\"Bio-Formats importer\"},\"n3\":{\"jipipe:node-info-id\":\"image-properties-to-annotation\",\"jipipe:graph-compartment\":\"7618ab1a-4aa3-5528-9668-1eee21b7df22\",\"jipipe:node:name\":\"Annotate with image properties\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Output\",\"target-node\":\"n1\",\"target-slot\":\"Input folder\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Input\"},{\"source-node\":\"n2\",\"source-slot\":\"Output\",\"target-node\":\"n3\",\"target-slot\":\"Image\"}]}"
---

# Pattern: Project user path -> Group -> Bio-Formats importer -> Annotate with image properties

Mined pattern `chain-4-project-user-directory-node-group-import-imagej-bioformats-image-properties-to-annotation` (shape
`chain`, support 40).

## Role sequence

| role     | node ids                         |
|----------|----------------------------------|
| import   | `project-user-directory`         |
| other    | `node-group`                     |
| import   | `import-imagej-bioformats`       |
| annotate | `image-properties-to-annotation` |

## Example graph

Project: `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` (relative to `/data/JIPipe`), compartment
`Preprocessing`.

| key | node id                          | name                                |
|-----|----------------------------------|-------------------------------------|
| n0  | `project-user-directory`         | Project user directory              |
| n1  | `node-group`                     | READ: folders, subfolders and files |
| n2  | `import-imagej-bioformats`       | Bio-Formats importer                |
| n3  | `image-properties-to-annotation` | Annotate with image properties      |

| from                                | slot              | to                                  | slot           | data class   |
|-------------------------------------|-------------------|-------------------------------------|----------------|--------------|
| Project user directory              | `Output`          | READ: folders, subfolders and files | `Input folder` | `folder`     |
| READ: folders, subfolders and files | `Annotated paths` | Bio-Formats importer                | `Input`        | `path`       |
| Bio-Formats importer                | `Output`          | Annotate with image properties      | `Image`        | `imagej-ome` |

## Template

Proposed name `Project user path -> Group -> Bio-Formats importer -> Annotate with image properties`, menu path
`Patterns/Data Sources/Project user path -> Group -> Bio-Formats importer -> Annotate with image properties` (proposal
only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/chain-4-project-user-directory-node-group-import-imagej-bioformats-image-properties-to-annotation.json`.

