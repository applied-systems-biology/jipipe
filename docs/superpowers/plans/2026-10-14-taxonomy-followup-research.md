# Taxonomy Follow-up Research Plan (work item 1324)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Answer, with measured data from the real pipeline corpus, whether the 6.0.0 taxonomy (function tree +
framework + family follow-ups) matches how users actually build and navigate
pipelines - **and extract every recurring pipeline pattern the corpus exhibits** (e.g.
"File → Annotate with path → Import image"), mapping each to its consumer: family follow-ups,
the future pattern-template feature (reusing the existing `JIPipeNodeTemplate` API), and the
LLM-integration pattern knowledge - producing a prioritized list of concrete taxonomy changes
plus the pattern library as data.

**Architecture:** One corpus-extraction pipeline (Java, offline, jipipe-core test classpath +
Jackson + JGraphT, no Swing, no ImageJ) dumps every project in the corpus to a single
interchange JSON (nodes by id, edges by input-slot data type + convertibility, node usage
frequencies, function/framework/family facets from the live registry). Analysis tasks run
offline Python over that JSON + the registry dump, each producing one numbered report. The
pattern-mining tasks extract recurring subgraph patterns (chains, fan-out/fan-in motifs,
conversion chains, compartment organization) and cross-check them against the hand-written
pattern library of the halted LLM-integration branch (`/data/src/jipipe-2`,
`1290-ai-llm-integration`) as a seeding point - measured evidence validates, corrects and
extends those hand-written docs. The reports feed the next implementation round (taxonomy
content corrections, family coverage, follow-up quality, pattern templates); no production
code changes in this plan except the extractor itself.

**Tech Stack:** Java 21 (extractor), Maven offline (`mvn -o`), Jackson (`.jip` format 1),
JGraphT, Python 3 (analysis, stdlib only - no data deps), the taxonomy registries as built by
commits (32)– (34).

**Spec:** `docs/superpowers/specs/2026-10-08-node-taxonomy-redesign-design.md` (the taxonomy
redesign; this plan researches its Phase-3 "refined content" inputs) + the round-9 feedback
items: (a) the "specialized data types with few input uses (like trackmate)" pattern, (b)
real-world pipeline analysis from `/data/JIPipe/Example workflows` and
`/data/JIPipe/AIAgentTests`, (c) **extraction of all observable pipeline patterns** (e.g.
"File → Annotate with path → Import image") - feeding the follow-ups UI, the future
pattern-template feature (reusing the existing node-template API), and the LLM-integration
pattern knowledge (see `Seeding inputs`, below).

## Seeding inputs (read-only references; cite, don't copy blindly)

1. **The hand-written pattern library** of the halted LLM-integration branch:
   `/data/src/jipipe-2` @ `1290-ai-llm-integration`, directory
   `jipipe-core/src/main/resources/org/hkijena/jipipe/ai/knowledge/sources/bioimage-analysis/patterns/`
   (24 markdown pattern docs with YAML frontmatter: `audience`, `modality`, `tags`,
   `biologicalContext`, `prerequisiteKnowledge`; each names its key node ids). Branch
   status: on halt (JEV deployment investigation). **Expected to hit master after this
   branch.** The mining results are the *measured* check on these hand-written docs: keep
   what the corpus confirms, correct what it contradicts, add what it reveals.
2. **KDB solution docs** (same branch): `.../ai/solutions/*.md` (frontmatter `id`,
   `description`, `keywords`) - shorter task-shaped patterns; cross-check the pattern ids
   mined in Task 7 against these ids.
3. **The node-template API** (this repo, already on the branch):
   `org.hkijena.jipipe.api.JIPipeNodeTemplate` - name, description, menuPath, icon, hue,
   `data` (a serialized `JIPipeGraph`, via `setData`/`setGraph`) - the designated carrier
   for the future pattern-template feature (multi-node insertable patterns). Task 7's
   output is designed to map 1:1 onto this API (graph JSON + name + description + tags).

## Global Constraints

- Branch: `1324-node-taxonomy-core` (MR !168). Commits: single-line
  `type: description (6.0.0) (N)`; next number after (34) is **(35)**.
- All taxonomy IDs stay stable string literals; this plan changes NO registry content, NO
  annotations, NO production behavior - only the extractor tool + reports.
- The corpus is **read-only** (never write into `/data/JIPipe/`); all derived artifacts go to
  `utils/taxonomy-research/`.
- Extraction must run offline (`-o`) and headless: no Swing, no ImageJ/Fiji, no SciJava
  context - if a node registry is needed, build it the same way `TaxonomyFullValidationTest`
  does, but the extractor only needs the registries, not the full validation.
- A finding is only recorded with its numbers + example node ids / project paths; no
  unsourced opinions in the reports.

## Review Focus

- **Unreadable / corrupted / legacy-version `.jip` files** - must be counted and listed (with the parse error), never
  crash the dump; the corpus stats stay honest about coverage.
- **Duplicate node ids across projects** - same id appearing in two projects with different
  configs must not break the usage aggregation (aggregate by id, keep both examples).
- **Nodes from plugins that failed to register** in the headless extractor - such node ids in
  a project must be reported as "unresolvable" (and their edges excluded), not silently dropped.
- **Slot-level edges vs node-level edges** - a node can be wired to the same data type through
  many slots; the edge model must be (sourceNode, sourceSlot, targetNode) with the data type,
  so "type usage" counts are per-slot, not per-node.
- **Function-leaf ambiguity from secondary functions** - a node can hold several function
  leaves; every "which leaf should own this workflow" finding must cite the node's primary
  vs secondary leaves, not just one.

---

## File Structure (locked)

| Path                                                                                          | Responsibility                                                                                                                                                                                                                                                                                                                                                                        |
|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `utils/taxonomy-research/extract/ProjectCorpusDumper.java`                                    | One main(): load `.jip` files → corpus JSON. Pure Jackson, no jipipe-core dependency (slot data classes recorded as the raw on-disk tokens; Task 3 normalizes). The only standalone Java in this plan.                                                                                                                                                                                |
| `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/RegistryDumperTest.java` | JUnit test (boots the JIPipe instance exactly like the sibling `TaxonomyFullValidationTest`) that dumps the live node registry (id → name, functions, framework, tags, family steps, follow-ups, input/output data types) to `utils/taxonomy-research/registry.json`. A test, not a main: it reuses the proven headless bootstrap and the existing suite's ImageJ-patcher protection. |
| `utils/taxonomy-research/corpus.json`                                                         | Generated: all projects, nodes, edges (Task 1).                                                                                                                                                                                                                                                                                                                                       |
| `utils/taxonomy-research/registry.json`                                                       | Generated: taxonomy facets per node id (Task 2).                                                                                                                                                                                                                                                                                                                                      |
| `utils/taxonomy-research/analysis/{t3..t8}.py`                                                | One script per analysis task (Tasks 3–8); each also emits `analysis/t{N}_table.md` (raw tables cited by report.md).                                                                                                                                                                                                                                                                   |
| `utils/taxonomy-research/patterns/`                                                           | Task 7 output: the mined pattern library - `patterns.json` (the machine-readable inventory) + one `*.md` per confirmed pattern (Task 8; the same frontmatter shape as the LLM-branch pattern docs, plus a `corpus` evidence block and a `template` block for template candidates).                                                                                                    |
| `utils/taxonomy-research/report.md`                                                           | The findings document (Tasks 3–9); sections 1–7 map to Tasks 3–9 outputs (1: data-type usage, 2: workflow chains, 3: families, 4: function-tree fit, 5: pattern mining, 6: pattern library, 7: recommendations).                                                                                                                                                                      |
| `utils/taxonomy-research/README.md`                                                           | How to regenerate everything (commands, corpus provenance, versions).                                                                                                                                                                                                                                                                                                                 |

## Task 1: Corpus dumper → `corpus.json`

**Files:**

- Create: `utils/taxonomy-research/extract/ProjectCorpusDumper.java`
- Create: `utils/taxonomy-research/corpus.json` (generated)

**Interfaces:**

- Consumes: `.jip` project format (Jackson; `JIPipeProject`-compatible on-disk format, read
  via Jackson directly - no JIPipe instance needed).
- Produces: `corpus.json` with exactly this shape (later tasks depend on it):

```json
{
  "projects": [
    {
      "file": "Example workflows/AdrianFeile_VibrioOnChip_04.jip",
      "loadable": true,
      "loadError": null,
      "compartments": [
        {
          "name": "…",
          "nodes": [
            {
              "uid": "…", "id": "…", "name": "…",
              "slots": {
                "input":  [{"name": "…", "dataClass": "…"}],
                "output": [{"name": "…", "dataClass": "…"}]
              }
            }
          ],
          "edges": [
            {"fromUid": "…", "fromSlot": "…", "dataClass": "…", "toUid": "…", "toSlot": "…"}
          ]
        }
      ]
    }
  ]
}
```

- [ ] **Step 1: Inventory the corpus.**

Run:

```bash
ls "/data/JIPipe/Example workflows/" | wc -l
find /data/JIPipe/AIAgentTests -name '*.jip' | wc -l
```

Expected: 61 + 102. Record both numbers in `utils/taxonomy-research/README.md` (corpus
provenance line: paths, file counts, date).

- [ ] **Step 2: Write the selftest.**

`ProjectCorpusDumper` gets a `selftest` mode (`java ... selftest`) - the extractor is a
standalone program (compiled with `javac` against the Jackson jars in `~/.m2`, NOT a Maven
module; no test framework): asserts against the two smallest corpus files
(`AIAgentTests/01-BasicWorkshop/lecture_02.jip` and
`AIAgentTests/05-Homework-JSC2025/Homework_JSC_2025/Alternative1/hyphae_single.jip`) that (d) `loadable == true`, (e)
node count > 0, (f) every edge references existing uids and slot
names, (g) a deliberately bogus path yields `loadable == false` with a non-null `loadError`
instead of an exception. Exit code = pass/fail.

- [ ] **Step 3: Implement `ProjectCorpusDumper.main(String[] args)`.**

`args[0]` = corpus root (s), `args[1]` = output path. Walk both roots for `*.jip` (recursive
for AIAgentTests, top-level for Example workflows - record which files came from where in the
`file` field). Parse each project with Jackson (`ObjectMapper`), extract compartments →
nodes (uid, `node-type`/id, name, declared input/output slots with their data class names) and
connections (input-slot level: `fromUid`, `fromSlot`, `toUid`, `toSlot`, `dataClass`).
Catch every per-file exception into `{loadable:false, loadError}` (Review Focus #1).
Data classes are recorded as the RAW on-disk tokens (no jipipe-core dependency in this
dumper); Task 3 normalizes them against `registry.json` (FQN match, then simple-name
match; unmatched → "unresolved" list, Review Focus #3).

- [ ] **Step 4: Run the selftest, then the dumper over the whole corpus.**

Run:

```bash
cd /data/src/jipipe-4 && javac -cp <jackson-jars-in-~/.m2> -d utils/taxonomy-research/extract/classes utils/taxonomy-research/extract/ProjectCorpusDumper.java
java -cp utils/taxonomy-research/extract/classes:<jackson-jars> ProjectCorpusDumper selftest
java -cp utils/taxonomy-research/extract/classes:<jackson-jars> ProjectCorpusDumper "/data/JIPipe" utils/taxonomy-research/corpus.json
```

Expected: selftest exit 0; dumper exit 0; `corpus.json` present; the README gets a
"coverage" line: `N loaded / M total, K failed (see loadError)`.

- [ ] **Step 5: Commit.**

```bash
git add utils/taxonomy-research && git commit -m "research: corpus dumper for the real-pipeline taxonomy study (6.0.0) (35)"
```

## Task 2: Registry dumper → `registry.json`

**Files:**

- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/RegistryDumperTest.java`
- Create: `utils/taxonomy-research/registry.json` (generated by running the test)

**Interfaces:**

- Consumes: the live registries exactly as `TaxonomyFullValidationTest` builds them (`JIPipe.getNodes()`,
  `getFunctions()`, `getFamilyAssembler()`); commits (32)– (34) code.
  Runs under the jipipe-launcher test setup (JUnit 5 + the ImageJ legacy-patcher
  `LauncherSessionListener` protection - do not add `--add-opens` or manual
  `LegacyInjector.preinit()` calls; see AGENTS.md).
- Produces: `utils/taxonomy-research/registry.json`:

```json
{
  "nodes": [
    {
      "id": "trackmate-create-spot-detector-dog",
      "name": "…",
      "framework": "jipipe:framework:trackmate",
      "functions": ["…primary…", "…secondary…"],
      "tags": ["…"],
      "familySteps": [{"family": "jipipe:family:trackmate", "label": "Detect spots"}],
      "followUps": ["trackmate-tracking"],
      "inputs": ["…dataClass…"], "outputs": ["…dataClass…"]
    }
  ]
}
```

- [ ] **Step 1: Write the dumper test with its assertions inline.**

`RegistryDumperTest` (one `@Test`, mirroring `TaxonomyFullValidationTest`'s bootstrap):
build the registries, write `utils/taxonomy-research/registry.json` (resolved relative to
the repo root - the test runs from `jipipe-launcher/`, so use
`Paths.get("..", "utils", "taxonomy-research", "registry.json").toAbsolutePath().normalize()`),
then assert: (d) the dump contains the node id `trackmate-create-spot-detector-dog`, (e) that node's `followUps`
contains `trackmate-tracking` (the round-9 fix is in place), (f) `trackmate-detector`'s followUps contain
`trackmate-tracking`, (g) every
`familySteps[].family` value is present in the assembler's family set, (h) total node count
matches `JIPipe.getNodes().getRegisteredNodeInfos().size()`.

- [ ] **Step 2: Run it.**

Run: `mvn -o -pl jipipe-launcher -am test -Dtest='RegistryDumperTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS; `registry.json` written. Record in README:
`registry: N nodes, M families, K family-graph edges` (K = sum over families of declared
resolved edges).

- [ ] **Step 3: Commit.**

```bash
git add utils/taxonomy-research jipipe-launcher/src/test && git commit -m "research: taxonomy registry dumper (functions/frameworks/families/follow-ups) (6.0.0) (36)"
```

## Task 3: Data-type usage map (the "trackmate pattern" - general)

**Files:**

- Create: `utils/taxonomy-research/analysis/t3_datatype_usage.py`
- Modify: `utils/taxonomy-research/report.md` (create with section 1)

**Interfaces:**

- Consumes: `corpus.json` (edges + slot data classes), `registry.json` (which node ids
  produce/consume each type).
- Produces: report section **1. Data-type usage map** - for every data class appearing in any
  corpus edge: producers (distinct node ids that output it, with counts), consumers (distinct
  node ids that accept it as input, with counts), corpus occurrences (edge count, project
  count). Sorted by occurrence count desc. Plus a flagged sub-table: **types with exactly
  1–2 distinct consumers** and **types with exactly 1–2 distinct producers**.

- [ ] **Step 1: Write the analysis script.**

`t3_datatype_usage.py`: read both JSONs; **normalize the corpus's raw data-class tokens**
against `registry.json` (FQN match, then simple-name match; unmatched tokens →
`analysis/unresolved_types.md`, Review Focus #3); build the tables above; emit
`utils/taxonomy-research/analysis/t3_table.md` (the raw table) and print a summary
(`T types, X flagged low-consumer, Y flagged low-producer`). No heuristics beyond the
count thresholds (1–2) - interpretation belongs in Task 9.

- [ ] **Step 2: Run it; sanity-check against one known type.**

Run: `python3 utils/taxonomy-research/analysis/t3_datatype_usage.py`
Expected: exit 0. Manually verify `SpotsCollectionData` (trackmate) appears with producers
including the `trackmate-*` detector ids and consumers including `trackmate-tracking`
(cross-check the ids against `registry.json`). Record the verification line in the report.

- [ ] **Step 3: Commit** (table + script + README note):

```bash
git add utils/taxonomy-research && git commit -m "research: data-type producer/consumer usage map over the real corpus (6.0.0) (37)"
```

## Task 4: Workflow chains & function-branch fragmentation

**Files:**

- Create: `utils/taxonomy-research/analysis/t4_workflows.py`
- Modify: `utils/taxonomy-research/report.md` (section 2)

**Interfaces:**

- Consumes: `corpus.json` (per-compartment edges), `registry.json` (function facets).
- Produces: report section **2. Workflow chains** - (a) top-20 most-used node ids across the corpus (with project
  counts - aggregate by id,
  counting distinct projects, so the same id used in several projects or twice in one is
  counted correctly, Review Focus #2); (b) the 20 most frequent 3-node chains `A→B→C` (by data-class-typed edges,
  deduped by id); (c) for each of those chains, the function leaves each node lives in - **primary and
  secondary leaves both** (Review Focus #5) - flag chains where the three nodes span **≥ 3 distinct top-level branches**
  (fragmentation signal) and chains where the middle
  node's primary branch is shared by no chain partner; (d) "dead-ends": node ids that are frequently consumed but whose
  output type's consumers (from `registry.json`) are all in one narrow branch - candidates for secondary-function
  assignments or family steps.

- [ ] **Step 1: Write the script** (emits `t4_table.md` + prints `chains found: N,
  fragmented: M, dead-end candidates: K`).

- [ ] **Step 2: Run; verify one chain end-to-end** by hand against one named project (cite
  the project path in the report).

- [ ] **Step 3: Commit** (6.0.0) (38).

## Task 5: Family coverage & follow-up quality against real usage

**Files:**

- Create: `utils/taxonomy-research/analysis/t5_families.py`
- Modify: `utils/taxonomy-research/report.md` (section 3)

**Interfaces:**

- Consumes: `registry.json` (family steps, follow-ups), `corpus.json` (actual edges).
- Produces: report section **3. Families vs reality** - (a) every family: its steps, and how often the corpus actually
  uses each declared
  `after`-edge as a real connection (declared-but-never-wired edges; corpus-edges between
  family members that are NOT declared); (b) corpus-edges between nodes that share a framework but have NO family
  relation (family
  candidates - e.g. other TrackMate-adjacent nodes, or a second seed family); (c) the "specialized data types"
  cross-check: from Task 3's low-consumer types, which ones
  belong to a framework whose nodes already have families vs. not - the trackmate case and
  its siblings (cellpose, ilastik, opencv, omnipose, weka, clij).

- [ ] **Step 1: Write the script** (emits `t5_table.md`; prints per-family counts).

- [ ] **Step 2: Run; verify the trackmate family numbers** against
  `TrackMateFamilyStepsTest` expectations (14 annotated classes + the detector fan-out).

- [ ] **Step 3: Commit** (6.0.0) (39).

## Task 6: Navigation ergonomics - do people use the tree or the search?

**Files:**

- Create: `utils/taxonomy-research/analysis/t6_functions.py`
- Modify: `utils/taxonomy-research/report.md` (section 4)

**Interfaces:**

- Consumes: `registry.json` (function tree + per-node leaves), `corpus.json` (used node ids).
- Produces: report section **4. Function tree fit** - (a) leaf occupancy: nodes per function leaf (top-15 biggest,
  bottom-15 smallest non-empty); (b) "orphan load": how many of the corpus's used nodes sit in `Miscellaneous*`-type
  leaves (the conversion's last-resort rule) - with the list; (c) depth/branch check: top-level branches with < 3 used
  nodes in the corpus (candidates
  for merging or demoting); (d) secondary-function value: nodes whose corpus usage (what they connect to/from) matches
  a secondary leaf better than their primary - the strongest case for secondary assignments
  or tag changes.

- [ ] **Step 1: Write the script** (emits `t6_table.md`).

- [ ] **Step 2: Run; spot-check 3 named nodes** from (d) by reading their slot types in
  `registry.json`.

- [ ] **Step 3: Commit** (6.0.0) (40).

## Task 7: Pattern mining - every recurring pipeline pattern

The round-9 feedback's core ask: "extract all patterns that can be observed", e.g.
"File → Annotate with path → Import image". These become (a) follow-up evidence, (b)
pattern templates (node-template API), (c) the measured correction of the LLM branch's
hand-written pattern library.

**Files:**

- Create: `utils/taxonomy-research/analysis/t7_patterns.py`
- Create: `utils/taxonomy-research/patterns/patterns.json` (generated)
- Modify: `utils/taxonomy-research/report.md` (section 5, tables 5a–5c: the mined-pattern inventory, the LLM-library
  cross-check, library gaps)

**Interfaces:**

- Consumes: `corpus.json` (per-compartment graphs: nodes + slot-typed edges),
  `registry.json` (function/framework/family facets per node id), the LLM-branch pattern
  docs (Seeding input 1; read-only).
- Produces: `patterns/patterns.json` - the inventory of mined patterns:

```json
{
  "patterns": [
    {
      "id": "import-annotate-image",
      "name": "File → Annotate with path → Import image",
      "shape": "chain",
      "nodes": [
        {"role": "import", "nodeIds": ["import-file", "import-folder", "list-folder-contents"]},
        {"role": "annotate", "nodeIds": ["annotate-set-path-as-annotation", "annotate-data-with-table-values"]},
        {"role": "to-image", "nodeIds": ["import-image"]}
      ],
      "support": 17,
      "projects": ["Example workflows/…", "AIAgentTests/…"],
      "exampleGraph": { …the smallest compartment subgraph JSON (node ids + edges, parameters elided)… },
      "crossRefs": {"llmPatternDoc": "import-split-annotate.md", "kdbSolution": null}
    }
  ]
}
```

- Pattern classes to mine (all as node-id-generalized subgraphs - roles abstract over the
  concrete node ids, `support` = number of distinct occurrences across the corpus,
  deduplicated per project):
    1. **Chains** `A→B→C` (length 3–5): the import/annotate/preprocess/measure/export spine.
    2. **Fan-out** (one output → ≥2 consumers) and **fan-in** (≥2 sources → one input): e.g.
       one image source feeding threshold + measurement; multiple annotations merging.
    3. **Conversion chains** (edges whose data type changes at every hop, length ≥ 2): the
       mask ↔ ROI ↔ labels family and its siblings.
    4. **Annotation sandwiches**: any node of the annotation framework between an import and
       a processor (the "Annotate with path" pattern and variants).
    5. **Compartment-boundary motifs**: export-at-compartment-end → import-at-next-start (the
       compartment-organization pattern) and compartment-wide cycles (loops).
- [ ] **Step 1: Write the mining script.**

`t7_patterns.py`: read `corpus.json` + `registry.json`; for each pattern class, extract
generalized subgraphs (roles = node ids with the same function leaf or the same "role
signature" - import/annotation/export/… - from `registry.json` facets: data-source leaves,
annotation-framework nodes, export leaves); aggregate support across projects; rank by
support desc; keep patterns with support ≥ 3 (record the cutoff and the rejected count in
the report). For each pattern, capture the smallest example subgraph (node ids + slot
edges only - parameters elided) as `exampleGraph`. Emit `patterns/patterns.json` +
`analysis/t7_table.md` (id, shape, support, project count, roles) and print
`patterns: N (chains C, fan F, conversions V, annotations A, compartments M)`.

- [ ] **Step 2: Cross-check against the LLM-branch pattern docs.**

For each of the 24 hand-written pattern docs (Seeding input 1): match by the doc's named
node ids (a doc matches a mined pattern when ≥ 50% of its named ids appear in the pattern's
roles) and record `crossRefs.llmPatternDoc`. A hand-written doc with **no matching mined
pattern** and **no corpus evidence** is a correction candidate (hand-written pattern the
real corpus does not exhibit); a mined pattern with high support and **no doc** is a
library-gap candidate. Both lists go into the report (section 5, tables 5b/5c).

- [ ] **Step 3: Run and verify the flagship pattern.**

Run: `python3 utils/taxonomy-research/analysis/t7_patterns.py`
Expected: exit 0. Verify "File → Annotate with path → Import image" (or its closest mined
form) is found with support ≥ 3, by manually opening one cited project and confirming the
chain (record the check in the report).

- [ ] **Step 4: Commit.**

```bash
git add utils/taxonomy-research && git commit -m "research: mine all recurring pipeline patterns from the corpus, cross-checked against the LLM-branch pattern library (6.0.0) (41)"
```

## Task 8: Pattern library deliverable + follow-up mapping

**Files:**

- Create: `utils/taxonomy-research/patterns/*.md` (one per confirmed pattern)
- Modify: `utils/taxonomy-research/report.md` (section 6)

**Interfaces:**

- Consumes: `patterns/patterns.json` (Task 7), the node-template API shape (Seeding
  input 3: name, description, menuPath, icon, hue, `data` = serialized `JIPipeGraph`).
- Produces:
    - One markdown doc per pattern with support ≥ 3, in the **same frontmatter shape as the
      LLM-branch docs** (`audience`, `modality`, `tags`, `biologicalContext`,
      `prerequisiteKnowledge`) plus a `corpus` block: `support`, `projects` (top 5),
      `exampleGraph` (the subgraph), `templateCandidate` (true/false). For template
      candidates also a `template` block: proposed `name`, `menuPath`, and the
      `JIPipeGraph`-shaped `data` (built from `exampleGraph` - node ids + edges, no
      parameters), mapping 1:1 onto `JIPipeNodeTemplate.setData`.
    - Report section **6. Pattern library** - table 6a: pattern → follow-up mapping (which
      mined pattern each family/step should surface as follow-ups for its role nodes; e.g.
      every "import" role node gains "Annotate with path" as a follow-up); table 6b:
      pattern → template candidates (ranked by support × breadth); table 6c: corrections to
      the LLM-branch library (from Task 7's step 2) - the seeding hand-off for when that
      branch resumes.
- [ ] **Step 1: Generate the pattern docs** from `patterns.json` (script
  `t8_pattern_docs.py` or fold into `t7_patterns.py --emit-docs` - pick one, note in
  README).

- [ ] **Step 2: Write section 6** (the three tables).

- [ ] **Step 3: Verify one template candidate end-to-end**: take the highest-support
  template candidate, assemble its `data` JSON, and verify it deserializes as a
  `JIPipeGraph` (a one-off check in the Task 2 dumper test's module or a documented
  snippet - node ids must exist in `registry.json`). Record the check in the report.

- [ ] **Step 4: Commit.**

```bash
git add utils/taxonomy-research && git commit -m "research: pattern library + follow-up and template mappings (LLM-branch cross-check) (6.0.0) (42)"
```

## Task 9: Findings → prioritized taxonomy change list

**Files:**

- Modify: `utils/taxonomy-research/report.md` (section 7 + executive summary at top)

**Interfaces:**

- Consumes: sections 1–6 (Tasks 3–8).
- Produces: section **7. Recommendations** - a prioritized table, each row: change (new family / family step / secondary
  function / tag / leaf merge or rename candidate /
  pattern follow-up wiring / pattern template), evidence (which table + numbers), risk (stable-ID rule impact - renames
  are 6.0.0-only and must flag the conversion-map
  update), and suggested round. Plus the executive summary (≤ 10 lines) for the
  work-item comment. This task changes NO code.

- [ ] **Step 1: Write section 7 + executive summary** from the tables (no new data).

- [ ] **Step 2: Self-review against Global Constraints** - every row cites numbers; the
  stable-ID rule is respected (any rename row is flagged); Review-Focus items are all
  answered (coverage numbers present, unresolvable nodes listed).

- [ ] **Step 3: Commit** (6.0.0) (43).

- [ ] **Step 4: Post the executive summary as a work-item comment** on 1324 (GraphQL `createNote`, noteableId
  `gid://gitlab/WorkItem/1784`, project 131 - the
  established API pattern), linking the report path.

## Execution notes

- Tasks 1–2 are sequential (Tasks 3+ consume both). Tasks 3–7 are independent of each
  other once 1–2 land. Task 8 depends on 7 (and 2, for the template verification).
  Task 9 is last (consumes 3–8).
- Recommended execution: **subagent-driven** for Tasks 3–7 (independent, table-shaped
  deliverables, cheap to review), native for 1–2, 8 and 9 (the extractors need careful
  registry bootstrapping; 8–9 need the whole picture).
- The LLM-branch (`/data/src/jipipe-2` @ `1290-ai-llm-integration`) is read-only
  reference material in every task that cites it; nothing is merged from it, and its
  pattern docs are only *validated* against the corpus - corrections are recorded in
  this repo's report (table 6c) as the hand-off for when that branch resumes.
