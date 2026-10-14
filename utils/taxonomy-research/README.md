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

`registry: N nodes, M families, K family-graph edges` (K = sum over families
of declared resolved edges):

**registry: 1510 nodes, 2 families, 20 family-graph edges**

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

## Files

- `corpus.json` — generated corpus (structure only, no embedded data payloads;
  ~11.8 MB). Locked shape consumed verbatim by the later analysis tasks:
  `projects[]` → `file` (path relative to `/data/JIPipe`), `loadable`,
  `loadError`, `compartments[]` → `name`, `nodes[]` (`uid`, `id`, `name`,
  `slots.input[]`/`slots.output[]` with `name` + raw on-disk `dataClass` token),
  `edges[]` (`fromUid`, `fromSlot`, `dataClass`, `toUid`, `toSlot`).
- `registry.json` — generated registry (~705 KB), byte-stable across runs
  (nodes sorted by id; every registry set/array sorted lexicographically).
  Locked shape: `nodes[]` (`id`, `name`, `framework`, `functions[]` primary
  first then secondaries, `tags[]`, `familySteps[]` (`family`, `label`),
  `followUps[]`, `inputs[]`/`outputs[]` each `{"class": <FQN>, "dataType":
  <data-type id or null>}`). Joined to `corpus.json` on `dataType`.
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

## Verification

- Selftest output (2026-10-14): `SELFTEST: PASS` (exit 0).
- Full dump (2026-10-14): dumper exit 0; `corpus.json` 11,847,569 bytes.
- Independent Python cross-check of `corpus.json` against the raw `.jip` files:
  per-file node/edge counts match for all 161 projects (0 mismatches), and the
  locked JSON shape holds for every project/compartment/node/slot/edge.
- Read-only guarantee: nothing was written under `/data/JIPipe`.
