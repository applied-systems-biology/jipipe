# Taxonomy research — real-pipeline corpus study (6.0.0, work item 1324)

Research artifacts for mining the real JIPipe pipeline corpus for node-taxonomy
findings. Part of the taxonomy follow-up research plan
(`.superpowers/sdd/2026-10-14-taxonomy-followup-research/`).

## Corpus provenance

- `/data/JIPipe/Example workflows/` — user example workflows (read-only)
- `/data/JIPipe/AIAgentTests/` — AI-agent test projects (read-only)
- Date: 2026-10-14

File counts (actual, 2026-10-14):

| Source | Count |
|---|---|
| `Example workflows/*.jip` (top-level files) | **59** |
| `AIAgentTests/**/*.jip` (recursive) | **102** |
| **Total `.jip` files dumped** | **161** |

**Discrepancy vs. the brief (61 + 102 = 163):** the brief's expected count
`61` comes from `ls "/data/JIPipe/Example workflows/" | wc -l`, which counts
directory entries, not `.jip` files — the directory also contains one subdirectory
(`JIPipe.tmp.dir`) and one non-project file (`workflow-pattern-analysis.md`).
The recursive `*.jip` count is 59. AIAgentTests matches (102). The dumper
processes every `*.jip` it finds under both roots (161 files total).

## Coverage

`N loaded / M total, K failed (see loadError)`:

**161 loaded / 161 total, 0 failed (see loadError)**

Extracted totals: 710 compartments, 22,557 nodes, 31,826 edges.

## Registry provenance

The registry-side twin of the corpus above, dumped from the fully-booted
JIPipe instance (`JIPipe.ensureInstance()`, all `jipipe-plugin-*` on the
jipipe-launcher test classpath) by
`jipipe-launcher/…/taxonomy/RegistryDumperTest`. The two files together are
the locked interface for the later analysis tasks: `corpus.json` (real
pipelines) joins to `registry.json` (the registry) on each slot's
`dataType`.

- Date: 2026-10-14
- Source: the live node registry at boot (not a static file read)

`registry: N nodes, F functions, M families, K family-graph edges` (K = sum
over families of declared resolved edges; F = registered taxonomy functions):

**registry: 1510 nodes, 88 functions, 2 families (22 steps, 20 after-edges),
20 family-graph edges**

**Families actually registered: `jipipe:family:trackmate` (21 step nodes)
and `jipipe:family:plotting` (1 step node), 22 steps in total.** A family is
only present in the registry if at least one *registered* node carries its
`@JIPipeNodeTaxonomyFamilyStepAnnotation`; `Cellpose4Family` and
`MachineLearningFamily` are defined in source but no registered node is
annotated with them, so they do not appear. (The dispatch brief estimated
~72 families; the live registry is authoritative — 2.) Note the 21 trackmate
steps come from 15 annotated *classes*, not 21: `CreateSpotDetectorNode` is
the shared instance class behind the 7 dynamically-registered
`trackmate-create-spot-detector-*` nodes, so it contributes 7 steps.

**Function tree:** the 88 registered taxonomy functions are dumped as the
top-level `functions` array — one `{id, name, parent}` entry per function,
`parent` being the parent function's id or `null` for the 13 top-level
branches (`getParent() == null`, the `FunctionRegistry.topLevel()`
definition). Added so the workflow-chain / branch analyses can resolve which
top-level branch each node's function leaf id lives under.

**Family after-edges:** the declared workflow edges of each registered family
are dumped as the top-level `families` array — per family, `steps[]` lists
every step as `{nodeId, label, after}`, where `after` = the step's resolved
after **node ids** (sorted; `step.afterIds(assembler)` with the family step-id
prefix stripped). The 20 after-edges across the 22 steps equal the reported
K. Added so the family-coverage-vs-usage analysis can compare the declared
"which step runs after which step" against the real corpus chains. Note the
fan-out: `trackmate-tracking`'s `after` lists 8 node ids — `trackmate-detector`
plus all 7 `trackmate-create-spot-detector-*` nodes, because the annotation's
`after = {SpotDetectorNode.class, CreateSpotDetectorNode.class}` resolves the
shared detector class to every dynamically-registered detector node.

## Files

- `corpus.json` — generated corpus (structure only, no embedded data payloads;
  ~11.8 MB). Locked shape consumed verbatim by the later analysis tasks:
  `projects[]` → `file` (path relative to `/data/JIPipe`), `loadable`,
  `loadError`, `compartments[]` → `name`, `nodes[]` (`uid`, `id`, `name`,
  `slots.input[]`/`slots.output[]` with `name` + raw on-disk `dataClass` token),
  `edges[]` (`fromUid`, `fromSlot`, `dataClass`, `toUid`, `toSlot`).
- `registry.json` — generated registry (~716 KB), byte-stable across runs
  (nodes sorted by id; the `functions` array sorted by function id; the
  `families` array sorted by family id, its `steps` by node id, and each
  `after` array lexicographically; every registry set/array sorted
  lexicographically). Locked shape: `nodes[]` (`id`, `name`, `framework`,
  `functions[]` primary first then secondaries, `tags[]`, `familySteps[]`
  (`family`, `label`), `followUps[]`, `inputs[]`/`outputs[]` each
  `{"class": <FQN>, "dataType": <data-type id or null>}`) plus a top-level
  `functions[]` (the taxonomy function tree: `id`, `name`, `parent` = parent
  id or `null` for top-level branches) and a top-level `families[]` (the
  declared family workflow edges: `id`, `steps[]` = `{nodeId, label, after[]}`
  with `after` = resolved after node ids). `nodes` is joined to `corpus.json`
  on `dataType`; the `functions` block resolves each node's function leaf id
  to its top-level branch; the `families` block feeds the family-coverage
  analysis.
- `report.md` — the combined research report. **Section ownership:** section N
  (1–6) is owned by `analysis/t{N+1}_*.py` and is regenerated IN PLACE on
  re-run (each script replaces only its own `^## N. ` → next `^## `/EOF span;
  the rest of the file is untouched); section 7 + the executive summary are
  hand-written (native Task 9). Re-run order does not matter.
- `analysis/t3_datatype_usage.py` — Task 3: data-type producer/consumer usage
  map (report §1). Re-run: `python3
  utils/taxonomy-research/analysis/t3_datatype_usage.py`.
- `analysis/t4_workflows.py` — Task 4: workflow chains + function-branch
  fragmentation (report §2). Re-run: `python3
  utils/taxonomy-research/analysis/t4_workflows.py`.
- `analysis/t5_families.py` — Task 5: family coverage vs real usage (report
  §3). Re-run: `python3
  utils/taxonomy-research/analysis/t5_families.py`.
- `analysis/t6_functions.py` — Task 6: function-tree fit + secondary-function
  value (report §4). Re-run: `python3
  utils/taxonomy-research/analysis/t6_functions.py`.
- `analysis/t7_patterns.py` — Task 7: pattern mining + LLM-library
  cross-check (report §5; also writes `patterns/patterns.json`). Re-run:
  `python3 utils/taxonomy-research/analysis/t7_patterns.py`.
- `analysis/t8_pattern_docs.py` — Task 8: pattern docs + templates + report
  §6 (also writes `patterns/*.md` + `patterns/templates/*.json`). Re-run:
  `python3 utils/taxonomy-research/analysis/t8_pattern_docs.py`.
- `analysis/t{3..8}_table.md` — the raw per-task tables (also embedded in
  their report sections).
- `analysis/unresolved_types.md` — corpus data-class tokens that do not
  resolve against the registry (raw token, edge/project counts, example
  project).
- `patterns/` — Task 7/8 outputs: `patterns.json` (mined patterns, emission-
  capped, with a `summary` object preserving the long tail numerically),
  `<pattern-id>.md` (185 curated pattern docs), `templates/<pattern-id>.json`
  (185 JIPipeGraph-shaped template data files).
- `extract/ProjectCorpusDumper.java` — standalone dumper (Jackson only, no
  jipipe-core dependency, not a Maven module).
- `extract/classes/` — `javac` build scratch (git-ignored).

## Build & run

Compile against the Jackson jars in `~/.m2` (databind, core, annotations):

```bash
cd /data/src/jipipe-4
J=~/.m2/repository/com/fasterxml/jackson/core
CP="$J/jackson-databind/2.19.2/jackson-databind-2.19.2.jar:$J/jackson-core/2.19.2/jackson-core-2.19.2.jar:$J/jackson-annotations/2.19.2/jackson-annotations-2.19.2.jar"
javac -cp "$CP" -d utils/taxonomy-research/extract/classes utils/taxonomy-research/extract/ProjectCorpusDumper.java

# selftest (its "test": asserts loadable, node count > 0, edge uid/slot
# consistency on two corpus files, and bogus-path error capture; exit 0 = pass)
java -cp utils/taxonomy-research/extract/classes:$CP ProjectCorpusDumper selftest

# full corpus dump
java -Xmx4g -cp utils/taxonomy-research/extract/classes:$CP ProjectCorpusDumper \
    "/data/JIPipe/Example workflows" "/data/JIPipe/AIAgentTests" utils/taxonomy-research/corpus.json
```

Regenerate `registry.json` (runs the full JIPipe registry boot in
jipipe-launcher, the only module with all plugins on its classpath):

```bash
mvn -o -pl jipipe-launcher -am test -Dtest='RegistryDumperTest' -Dsurefire.failIfNoSpecifiedTests=false
```

The corpus dumper's walk is fully recursive (both corpus roots and all
subdirectories; benign here — no nested `.jip` files exist in either root).

## On-disk format notes (JIPipe 6.0.0, flat-graph project format)

The `.jip` JSON format is not a per-compartment nesting of nodes/edges (as the
brief's assumed shape suggested) — the dumper adapts to what is real:

- `graph` is the **flat main graph**: `nodes` is a map keyed by node UUID,
  `edges` a list with `source-node`/`source-slot`/`target-node`/`target-slot`.
  Every main-graph node carries `jipipe:graph-compartment` = UUID of its owning
  compartment (empty/absent for root-level nodes).
- `compartments.compartment-graph` is a **separate graph of compartment container
  nodes** (`jipipe:node-info-id: jipipe:project-compartment`). It provides
  compartment display names; its own nodes/edges are not duplicated into the
  output.
- The main graph also holds per-compartment `jipipe:compartment-output` **proxy
  nodes** (one per compartment) that declare the compartment's input/output slot
  interface. All 31,826 main-graph edges are re-homed into compartments by the
  **target** node's compartment. Consequently, an edge whose source is another
  compartment's output proxy appears in the target's compartment, and its
  `fromUid` references a node of the source compartment (the selftest's edge
  check resolves source uids project-wide for this reason).
  *Known limitation:* the two empty template projects
  `AIAgentTests/06-CreatedProjects/EmptyProject/*.jip` store their
  inter-compartment wiring only as alias-based compartment-graph edges and have
  zero main-graph edges, so that wiring is absent from `corpus.json` (the
  empty compartments themselves are present). No real pipelines are affected.
- 12 root-level nodes (no owning compartment) are kept in a synthetic
  `"(root)"` compartment so nothing is lost.
- Slot data classes are the **raw on-disk `slot-data-type` tokens**, recorded
  verbatim (51 distinct tokens); normalization against the registry happens in
  Task 3. Edge `dataClass` is the source output slot's token (the on-disk edge
  format carries no data class of its own).
- Structure only: node `data` fields / embedded payloads are never copied;
  verified: 0 base64-looking blobs in `corpus.json`, longest string 91 chars.

## Pattern library (Task 8)

`analysis/t8_pattern_docs.py` **owns doc generation** (the task brief allowed
folding it into `t7_patterns.py --emit-docs`; a separate script was chosen —
t7 is reviewed and frozen). Re-run: `python3
utils/taxonomy-research/analysis/t8_pattern_docs.py`. Stdlib only; reads
`patterns/patterns.json` (locked) + `registry.json` + `report.md` section 5,
writes:

- `patterns/<pattern-id>.md` — one doc per CURATED pattern (**185 docs**:
  graph-shaped `chain`/`conversion`/`fan-out`/`fan-in` with support >= 25, OR
  any `annotation-sandwich`; the controller's R8 rule overrides the plan's
  "one doc per pattern, support >= 3" — 13,122 docs would be a data dump).
  Frontmatter keeps the LLM-branch shape (`audience`, `modality`, `tags`,
  `biologicalContext`, `prerequisiteKnowledge`) but fills only what is
  mechanically derivable: `modality` (registry data types flowing through the
  pattern) and `tags` (top-level function branches, deduped + sorted); the
  other three keys are the literal `tbd` (domain framing cannot honestly be
  derived from generic algorithm node ids). Plus a `corpus` block (support,
  top-5 projects, example subgraph) and a `template` block.
- `patterns/templates/<pattern-id>.json` — the minimal JIPipeGraph-shaped
  `data` per doc (R8b contract in report section 6 method note).
- `report.md` section 6 (method note + tables 6a/6b/6c; in-place
  architecture — only the `^## 6. ` → next `^## `/EOF span is replaced,
  sections 1–5 untouched, byte-stable).
- `analysis/t8_table.md` — the raw 6a rows (all 416, not just top-30).

End-to-end check of one template `data` (candidate
`fan-in-ij1-roi-to-rgb-core-io-1-other-1`, the highest-support pattern):
`jipipe-launcher/src/test/java/.../taxonomy/TemplateDataDeserializationTest.java`
(deserializes via `JsonUtils.readFromString(data, JIPipeGraph.class)`, the
exact `JIPipeNodeTemplate.getGraph()` path) — PASS (3 nodes, 1 connection).
Run: `mvn -o -pl jipipe-launcher -am test
-Dtest='TemplateDataDeserializationTest' -Dsurefire.failIfNoSpecifiedTests=false`.

## Verification

- Selftest output (2026-10-14): `SELFTEST: PASS` (exit 0).
- t8 re-run (2026-10-14): the 185 `.md` + 185 `templates/*.json` files,
  `report.md` section 6 and `analysis/t8_table.md` are byte-identical across
  two runs; sections 1–5 of `report.md` byte-identical to the committed state.
- Full dump (2026-10-14): dumper exit 0; `corpus.json` 11,847,569 bytes.
- Independent Python cross-check of `corpus.json` against the raw `.jip` files:
  per-file node/edge counts match for all 161 projects (0 mismatches), and the
  locked JSON shape holds for every project/compartment/node/slot/edge.
- Read-only guarantee: nothing was written under `/data/JIPipe`.
