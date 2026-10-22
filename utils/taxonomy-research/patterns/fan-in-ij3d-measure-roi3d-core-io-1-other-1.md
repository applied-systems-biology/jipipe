---
audience: "tbd"
modality: "roi-3d-list -> imagej-imgplus -> imagej-results-table -> imagej-imgplus-greyscale-mask -> imagej-imgplus-greyscale"
tags: [Graph / Project, Images, ROIs]
biologicalContext: "tbd"
prerequisiteKnowledge: "tbd"
corpus:
  support: 30
  projects:
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_12_forR3.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy.jip"
    - "AIAgentTests/03-ExampleWorkflows/Annamaria_16_ERphagy_withHanddrawnROIs.jip"
    - "AIAgentTests/03-ExampleWorkflows/BacteriaOnMicrospheresInStaticCulture_Kerstin_16.jip"
  projectCount: 30
  exampleGraph:
    compartment: "Membrane+Particles"
    project: "AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip"
    nodes:
      - "jipipe:compartment-output": "Mask"
      - "ij3d-filter-roi3d-by-statistics": "Filter 3D ROI by statistics"
      - "ij3d-measure-roi3d": "Extract ROI 3D statistics"
    edges:
      - {"from":"Mask","fromSlot":"Magenta trimmed","to":"Filter 3D ROI by statistics","toSlot":"Reference","dataClass":"imagej-imgplus"}
      - {"from":"Filter 3D ROI by statistics","fromSlot":"Output","to":"Extract ROI 3D statistics","toSlot":"ROI","dataClass":"roi-3d-list"}
  templateCandidate: true
template:
  name: "Extract IJ3D ROI statistics fans in from 6 source ids (roles: core-io, other)"
  menuPath: "Patterns/ROIs/Extract IJ3D ROI statistics fans in from 6 source ids (roles: core-io, other)"
  data: "{\"nodes\":{\"n0\":{\"jipipe:node-info-id\":\"jipipe:compartment-output\",\"jipipe:graph-compartment\":\"ad00f165-3a9c-54ea-8551-832c3ae7d1cd\",\"jipipe:node:name\":\"Mask\"},\"n1\":{\"jipipe:node-info-id\":\"ij3d-filter-roi3d-by-statistics\",\"jipipe:graph-compartment\":\"ad00f165-3a9c-54ea-8551-832c3ae7d1cd\",\"jipipe:node:name\":\"Filter 3D ROI by statistics\"},\"n2\":{\"jipipe:node-info-id\":\"ij3d-measure-roi3d\",\"jipipe:graph-compartment\":\"ad00f165-3a9c-54ea-8551-832c3ae7d1cd\",\"jipipe:node:name\":\"Extract ROI 3D statistics\"}},\"edges\":[{\"source-node\":\"n0\",\"source-slot\":\"Magenta trimmed\",\"target-node\":\"n1\",\"target-slot\":\"Reference\"},{\"source-node\":\"n1\",\"source-slot\":\"Output\",\"target-node\":\"n2\",\"target-slot\":\"ROI\"}]}"
---

# Pattern: Extract IJ3D ROI statistics fans in from 6 source ids (roles: core-io, other)

Mined pattern `fan-in-ij3d-measure-roi3d-core-io-1-other-1` (shape `fan-in`, support 30).

## Role sequence

| role | node ids |
|---|---|
| other | `ij3d-measure-roi3d` |
| core-io | `io-interface`, `jipipe:compartment-output` |
| other | `ij3d-analyze-find-particles`, `ij3d-filter-roi3d-by-statistics`, `ij3d-labels-to-roi`, `ij3d-roi-filter-by-overlap` |

## Example graph

Project: `AIAgentTests/03-ExampleWorkflows/Annamaria_12.jip` (relative to `/data/JIPipe`), compartment `Membrane+Particles`.

| key | node id | name |
|---|---|---|
| n0 | `jipipe:compartment-output` | Mask |
| n1 | `ij3d-filter-roi3d-by-statistics` | Filter 3D ROI by statistics |
| n2 | `ij3d-measure-roi3d` | Extract ROI 3D statistics |

| from | slot | to | slot | data class |
|---|---|---|---|---|
| Mask | `Magenta trimmed` | Filter 3D ROI by statistics | `Reference` | `imagej-imgplus` |
| Filter 3D ROI by statistics | `Output` | Extract ROI 3D statistics | `ROI` | `roi-3d-list` |

## Template

Proposed name `Extract IJ3D ROI statistics fans in from 6 source ids (roles: core-io, other)`, menu path `Patterns/ROIs/Extract IJ3D ROI statistics fans in from 6 source ids (roles: core-io, other)` (proposal only — not registered). `data` = the minimal JIPipeGraph JSON; the full object is written to `templates/fan-in-ij3d-measure-roi3d-core-io-1-other-1.json`.

