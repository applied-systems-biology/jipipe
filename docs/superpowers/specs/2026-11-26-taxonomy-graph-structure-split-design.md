# Taxonomy: split the Graph branch - structural nodes vs user-facing debug nodes

Date: 2026-11-26
Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1403
Status: approved (user rulings recorded below)

## Intent and why

Work item 1403: "Comment nodes cannot be deleted." The 6.0.0 node taxonomy
conversion mapped the legacy `InternalNodeTypeCategory` behavior flags
(hidden in node browsers, not user-extractable, hence not user-deletable)
onto the `Graph > Debugging` leaf (`GraphDebugFunction`), and behavior flags
are read from a node's primary function (`TaxonomyView.behaviorFlags`).
The comment node was legacy `MiscellaneousNodeTypeCategory` (visible,
deletable) but was assigned to `GraphDebugFunction` by the conversion, so it
inherited internal behavior: it cannot be deleted, cannot be extracted, is
hidden from the add-nodes panel, and renders with the white Graph-branch
hue. `GetJIPipeSlotFolder`, `ImportJIPipeSlotFolder`, and `Crasher` carry the
same wrong assignment; `IOInterfaceAlgorithm` is wrongly treated as internal
too.

Root cause: the conversion conflated two genuinely different groups under
one leaf:

1. internally used graph structural nodes the user should never touch, and
2. user-facing debug/meta-run nodes.

## Decisions (user, 2026-11-26)

- `GraphDebugFunction` ceases and becomes unused (deprecated). The name was
  misleading: a proper debugging function already exists
  (`DebuggingFunction`, Misc > Debugging, holds Wait and LogHammer).
- The structural nodes move OUT into a NEW leaf that is hidden in the
  add-nodes panel and cannot be found.
- `IOInterfaceAlgorithm` is NOT structural: its pure form is actively used
  by users to structure graphs and acts as the copy-paste fallback for its
  descendants. It goes into `DataUtilitiesFunction` (Data utilities).
- User-facing nodes move into `DebuggingFunction`.
- `RunJIPipeProjectAlgorithm` (same regression class, legacy Misc, hidden
  via Wrappers flags) also goes to Misc > Debugging.
- Wrapper plugs (GraphWrapperAlgorithmInput/Output) move into the new
  structural leaf.
- `JIPipeProjectCompartmentOutput` moves into the new structural leaf (not
  the empty Compartment outputs leaf). Ensure no weird fallout in the
  Compartments tab's add-nodes panel.
- New leaf name: "Structure" (class `GraphStructuralFunction`).

## Node disposition

| Node | Legacy (pre-6.0) | Was | New function |
|------|------------------|-----|--------------|
| `JIPipeCommentNode` | Misc | Graph > Debugging | Misc > Debugging |
| `GetJIPipeSlotFolderAlgorithm` | Misc ("Meta run") | Graph > Debugging | Misc > Debugging |
| `ImportJIPipeSlotFolderAlgorithm` | Misc ("Meta run") | Graph > Debugging | Misc > Debugging |
| `CrasherAlgorithm` | Misc | Graph > Debugging | Misc > Debugging |
| `RunJIPipeProjectAlgorithm` | Misc ("Meta run") | Graph > Wrappers | Misc > Debugging |
| `LoopStartNode` | Misc (deprecated scaffold) | Graph > Debugging | Graph > Structure |
| `LoopEndNode` | Misc (deprecated scaffold) | Graph > Debugging | Graph > Structure |
| `GraphWrapperAlgorithmInput` | Internal | Graph > Wrappers | Graph > Structure |
| `GraphWrapperAlgorithmOutput` | Internal | Graph > Wrappers | Graph > Structure |
| `JIPipeProjectCompartmentOutput` | Internal | Graph > Wrappers | Graph > Structure |
| `IOInterfaceAlgorithm` | Misc | Graph > Debugging | Data utilities |

Unchanged (stay in Graph > Wrappers, UI-managed): `JIPipeNodeGroup`,
`JIPipeProjectCompartment`, `JIPipeDataFlowTunnel1Entrance`,
`JIPipeDataFlowTunnel1Exit`.

## Leaf-level changes

- **New** `GraphStructuralFunction` (Graph > Structure,
  `jipipe:function:graph-structural`): `isVisibleInPipeline=false`,
  `isVisibleInCompartments=false`, `canExtract=false` (the internal trio).
  Members: LoopStart, LoopEnd, wrapper plugs, compartment output.
- **`GraphDebugFunction`** (`jipipe:function:graph-debug`): unregistered
  from `TaxonomyPlugin`, class kept as `@Deprecated` with a javadoc
  redirect. Its ID literal is frozen (stable-ID rule; persisted in node
  database entries and UI state).
- **`GraphCompartmentOutputsFunction`** becomes empty; handled the same way
  as GraphDebugFunction if the taxonomy tree renders empty leaves (checked
  during implementation; otherwise left registered and noted).
- **`DebuggingFunction`** description widened to cover the meta-run helpers.
- **`DataUtilitiesFunction`** gains IOInterfaceAlgorithm.

No node registration IDs change; saved projects keep loading, and behavior
(hue, deletability, visibility) follows the new annotation at runtime.

## Approaches considered

1. **Rename `GraphDebugFunction` in place** (chosen direction initially,
   refined by the user into the split): keeps one leaf but cannot separate
   the two genuinely different node groups.
2. **Per-node behavior-flag overrides** in `@ConfigureJIPipeNode`: most
   flexible, but adds a parallel flag channel next to the function-carried
   flags; rejected as over-engineering for this split.
3. **Split into two leaves (chosen):** new structural leaf for the
   never-user-touched nodes; user-facing nodes into the existing proper
   debugging leaf; `GraphDebugFunction` deprecated.

## Contract tests

- `FunctionLeavesTest`: ID literal catalog updated (graph-debug entry
  removed, graph-structural added).
- `InternalFunctionFlagsTest`: internal trio updated to the new leaf set.
- `TokenTaxonomyTest`: search-token expectations updated in lockstep.
- New disposition test pinning, per affected node: function assignment and
  the resulting behavior flags (user-facing nodes extractable, structural
  nodes hidden and not extractable) - the 1403 regression contract.

## What is left to the builder

- Exact `DebuggingFunction` description wording (no parent-word repetition,
  bare-leaf-name rules from the 2026-11-25 name harmonization).
- Handling of the empty `GraphCompartmentOutputsFunction` leaf per the rule
  above.
- Verification that the Compartments tab add-nodes panel and the node
  database visibility facets stay consistent.
