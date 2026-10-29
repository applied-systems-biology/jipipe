---
audience: "tbd"
modality: "folder -> file -> path -> imagej-imgplus"
tags: [Annotations, Data Sources]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 43
  projects:
    - "AIAgentTests/01-BasicWorkshop/lecture_06.jip"
    - "AIAgentTests/01-BasicWorkshop/lecture_06_custom_dirs.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_01_remapped.jip"
    - "AIAgentTests/02-AdvancedPipelines/Cryogels_02.jip"
  projectCount: 43
  exampleGraph:
    compartment: "Analysis"
    project: "AIAgentTests/01-BasicWorkshop/lecture_06.jip"
    nodes:
      - "folder-list-files": "List files"
      - "path-to-annotation-simple": "Annotate with file/directory name"
      - "import-imagej-imgplus-from-file": "Import image"
    edges:
      - {"from":"List files","fromSlot":"Files","to":"Annotate with file/directory name","toSlot":"Paths","dataClass":"file"}
      - {"from":"Annotate with file/directory name","fromSlot":"Annotated paths","to":"Import image","toSlot":"Files","dataClass":"path"}
  templateCandidate: true
template:
  name: "List files -> Annotate with file/directory name -> Import image"
  menuPath: "Patterns/Data Sources/List files -> Annotate with file/directory name -> Import image"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"folder-list-files\",\"jipipe:graph-compartment\":\"cbcb3961-864b-55fc-8174-91daed0b2a10\",\"jipipe:node:name\":\"List files\"},\"n1\":{\"jipipe:node-info-id\":\"path-to-annotation-simple\",\"jipipe:graph-compartment\":\"cbcb3961-864b-55fc-8174-91daed0b2a10\",\"jipipe:node:name\":\"Annotate with file/directory name\"},\"n2\":{\"jipipe:node-info-id\":\"import-imagej-imgplus-from-file\",\"jipipe:graph-compartment\":\"cbcb3961-864b-55fc-8174-91daed0b2a10\",\"jipipe:node:name\":\"Import image\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Files\",\"target-node\":\"n1\",\"target-slot\":\"Paths\"},{\"source-node\":\"n1\",\"source-slot\":\"Annotated paths\",\"target-node\":\"n2\",\"target-slot\":\"Files\"}]}"
---

# Pattern: List files -> Annotate with file/directory name -> Import image

Mined pattern `conversion-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file` (shape
`conversion`, support 43).

## Role sequence

| role     | node ids                          |
|----------|-----------------------------------|
| import   | `folder-list-files`               |
| annotate | `path-to-annotation-simple`       |
| import   | `import-imagej-imgplus-from-file` |

## Example graph

Project: `AIAgentTests/01-BasicWorkshop/lecture_06.jip` (relative to `/data/JIPipe`), compartment `Analysis`.

| key | node id                           | name                              |
|-----|-----------------------------------|-----------------------------------|
| n0  | `folder-list-files`               | List files                        |
| n1  | `path-to-annotation-simple`       | Annotate with file/directory name |
| n2  | `import-imagej-imgplus-from-file` | Import image                      |

| from                              | slot              | to                                | slot    | data class |
|-----------------------------------|-------------------|-----------------------------------|---------|------------|
| List files                        | `Files`           | Annotate with file/directory name | `Paths` | `file`     |
| Annotate with file/directory name | `Annotated paths` | Import image                      | `Files` | `path`     |

## Template

Proposed name `List files -> Annotate with file/directory name -> Import image`, menu path
`Patterns/Data Sources/List files -> Annotate with file/directory name -> Import image` (proposal only — not
registered). `data` = the minimal JIPipeGraph JSON; the full object is written to
`templates/conversion-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file.json`.

