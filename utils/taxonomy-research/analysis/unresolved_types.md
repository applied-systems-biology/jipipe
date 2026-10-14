# Task 3 — unresolved corpus data-class tokens

Raw `slot-data-type` tokens on corpus edges that do not resolve against
`registry.json` (no exact data-type id, no exact class FQN, no simple-name
match; a simple-name match hitting more than one data type is listed as
`ambiguous` instead of guessed). These are the corpus-side 'unresolvable'
tokens of Review Focus #3 — their edges cannot be attributed to a
registered data type (e.g. a legacy id that no longer exists in 6.0.0).

| raw token | kind | edges | projects | example project |
|---|---|---:|---:|---|
| `plot-histogram` | unresolved | 623 | 63 | `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` |
| `plot-xy-scatter` | unresolved | 192 | 14 | `AIAgentTests/02-AdvancedPipelines/JohannesBrendel_Supramolecular_17.jip` |
| `imagej-imgplus-2d-greyscale-16u` | unresolved | 48 | 4 | `AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip` |
| `plot-xy-line` | unresolved | 44 | 14 | `AIAgentTests/02-AdvancedPipelines/CellTracking_01.jip` |
| `plot-pie-2d` | unresolved | 12 | 2 | `AIAgentTests/03-ExampleWorkflows/Franziska_Stephanie_Luminescence_08.jip` |
| `plot-category-statistical-bar` | unresolved | 4 | 4 | `AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip` |
| `plot-category-statistical-line` | unresolved | 4 | 4 | `AIAgentTests/03-ExampleWorkflows/ConfrontationAssay_Mohamed_Jaime_v17.jip` |
| `plotly-histogram` | unresolved | 2 | 2 | `AIAgentTests/03-ExampleWorkflows/Delgir_06.jip` |
