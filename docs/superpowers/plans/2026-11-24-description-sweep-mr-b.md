# Description Sweep (MR B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every JIPipe-authored node's description contains its name's content words, enforced by a permanent gate test, with the texts authored per node (no boilerplate) and applied through the existing audit tooling.

**Architecture:** A launcher-side gate test (`SearchDescriptionQualityTest`) defines the rule with the search's own tokenizer (`NodeSearchText`) and doubles as the worklist dump tool. A small Python builder merges the gate dump with source-file resolution and corpus usage into usage-ordered batches. Each batch's opening sentences are authored against the node's evidence, mechanically pre-checked, peer-reviewed, then applied with `utils/audit-node-descriptions.py apply` and verified by the extractor round-trip and the gate re-run.

**Tech Stack:** Java 21 / JUnit 5 (jipipe-launcher boots the full plugin registry), Python 3 (tooling), the existing `utils/audit-node-descriptions.py` apply machinery.

**Spec:** `docs/superpowers/specs/2026-11-24-search-finetuning-design.md` (components 6-7, "Description sweep shape", "Scope facts (measured 2026-11-24, post-MR-A; user decision A)"). MR A is !216 (`1404-search-finetuning`, auto-merge armed).

## Global Constraints

- Gate predicate (single source, Java only): every word of `NodeSearchText.contentWords(info.getName())` must satisfy `NodeSearchText.coveredBy(word, NodeSearchText.wordSet(plain))` where `plain = info.getDescription() == null ? "" : info.getDescription().toPlainText()`. No Python mirror of the rule is authoritative; a Python re-check is a pre-filter only.
- Gate scope (user decision A, 2026-11-24): node ids starting with `ij2:op:` or `clij:plugin:` are OUT of the gate by structural rule (auto-import families; their descriptions are third-party metadata, not JIPipe-authored text). Documented in the test's javadoc; NOT an exemption list.
- Every edit preserves the existing description body; only the opening sentence is rewritten or prepended (empty descriptions are authored whole). Composed text stays within the tool's 500-character draft cap where the body allows; bodies already longer stay intact (the cap is a draft-mode guard, not a sweep guard).
- Authoring quality rules (the audit tool's audience rules): outcome first, then technical specifics; two audiences (bioinformaticians, biologists); no JIPipe data-model jargon; 2D/3D stated for ROI nodes; no template boilerplate ("This node..."-stamping is the anti-goal).
- The sweep reuses `utils/audit-node-descriptions.py` for application (the only change allowed there: an optional `--proposals PATH` argument, default unchanged) and `utils/extract-node-descriptions.py` for round-trip verification. No fresh apply script.
- Batch order: corpus usage descending (instances from the shipped usage prior; 0 for unlisted), then plugin/module path; 30-50 rows per batch; a batch stays within one module.
- No em/en dashes anywhere (project rule); stable string literals for the dump system property; tests never write the real user settings; no Swing-structure tests.
- Branch `1404-description-sweep` off the current `1404-search-finetuning` HEAD; push and MR only at the end (intermediate commits stay local).

## Review Focus

- HTML-bearing descriptions: the gate must judge the PLAIN text (`toPlainText`), never raw markup - a word living only inside a tag or attribute must not pass. Exercised by the live registry itself (many descriptions carry HTML lists); the gate run and the batch round-trips are the pin.
- Inherited documentation: nodes sharing a parent class description fail together and must clear together - if a batch's applied count drops the gate by fewer failures than the batch has rows, an inherited-sharing case is the first suspect (per-batch gate re-run pins this).
- Deprecated nodes: their descriptions open with a deprecation notice - the sweep keeps that notice (as body) and the authored sentence follows it; the batch review pass checks this.
- Unresolvable rows: a failing node with no in-tree class is recorded with a note and investigated in the final task - never silently skipped (the builder carries the audit tool's `note` mechanism forward).
- Worklist drift between the gate dump and the source resolution (node renamed, plugin moved): the builder fails loudly on a node that vanished from the extract resolution rather than applying to a stale file path.

---

### Task 1: Gate test + worklist dump (RED)

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/SearchDescriptionQualityTest.java`

**Interfaces:**
- Consumes: `JIPipe.ensureInstance()`, `JIPipe.getNodes().getRegisteredNodeInfos()`, `NodeSearchText.contentWords/wordSet/coveredBy` (all existing).
- Produces: the gate (permanent regression test) and, behind `-Djipipe.desc-sweep.dump=<path>`, a JSON worklist array `[{nodeId, name, missing, description}]` (sorted by nodeId) that Task 2 consumes.

- [ ] **Step 1: Create the SDD ledger and branch**

Create branch `1404-description-sweep` off the current HEAD. Create the ledger `.superpowers/sdd/2026-11-24-description-sweep-mr-b/progress.md` (first line names this plan file).

- [ ] **Step 2: Write the gate test**

Mirror `TaxonomyFullValidationTest`'s boot (`@BeforeAll JIPipe.ensureInstance()`, idempotent). One `@Test` that iterates `JIPipe.getNodes().getRegisteredNodeInfos()`, skips ids starting with `ij2:op:` / `clij:plugin:` (constant `AUTO_IMPORT_ID_PREFIXES` with the user-decision-A javadoc), computes the missing words per the Global Constraint predicate, collects every failure (id, name, missing words) into one assertion message, and - when `jipipe.desc-sweep.dump` is set - writes the JSON worklist to that path before asserting. The assertion message must be actionable: `"<id> (<name>): missing [word, ...]"`.

- [ ] **Step 3: Run to verify RED and capture the worklist**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Djipipe.desc-sweep.dump=/tmp/opencode/desc-sweep-gate-dump.json -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with roughly 398 failures (planning count 397 + 1 empty; the exact number is recorded in the ledger). The dump file exists and is valid JSON.

- [ ] **Step 4: Commit (RED is expected and documented)**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/SearchDescriptionQualityTest.java
git commit -m "test(#1404): description quality gate (RED: the sweep fixes the failures)"
```

### Task 2: Worklist builder + apply extension

**Files:**
- Modify: `utils/audit-node-descriptions.py` (`apply` gains an optional `--proposals PATH`; default unchanged)
- Create: `utils/search-usage/build-description-sweep-worklist.py`
- Test: `utils/audit_node_descriptions_tests.py` (selftest extended if it exercises `apply`)

**Interfaces:**
- Consumes: Task 1's gate dump; `audit-node-descriptions.resolve_sources` (imported as a module); the usage prior resource `jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json`.
- Produces: batch files `utils/taxonomy-research/description-sweep/batch-NN.json` (`{"_note": ..., "rows": [{nodeId, file, class, missing, current, usage}]}`, deterministic), and `apply --proposals <path>`.

- [ ] **Step 1: Extend the apply tool with `--proposals PATH`**

`cmd_apply` reads the path from the argument (default: the existing `PROPOSALS` constant). The proposals format is unchanged. Run the selftest.

- [ ] **Step 2: Write the builder script**

`build-description-sweep-worklist.py --gate-dump <path> [--out-dir utils/taxonomy-research/description-sweep]`: for each dump row, resolve `file`/`class` via `resolve_sources` (unresolvable rows keep `file: null` + a `note`, never dropped; a node missing from the resolution entirely aborts with an error - worklist drift), attach corpus `usage` (prior instances, 0 default), sort by (usage desc, module path, nodeId), partition into batches of 30-50 that stay within one module, write `batch-NN.json` deterministically, print the batch table.

- [ ] **Step 3: Run the builder and commit**

Run it against the Task 1 dump; record the batch table (counts + modules) in the ledger. Commit builder + batches.

### Task 3: Batch authoring + application (one pass per batch, in order)

**Files:**
- Create per batch: `utils/taxonomy-research/description-sweep/proposals-batch-NN.json` (the authored proposals, same format the apply tool reads)
- Modify: the class files the proposals point to (via the apply tool only)

**Interfaces:**
- Consumes: `batch-NN.json` rows; the audit tool's audience rules text (`build_draft_prompt`) as the authoring brief.
- Produces: applied descriptions; after each batch the gate failure count drops by exactly the number of resolvable rows in the batch.

Per batch, in order:

- [ ] **Step 1: Author the proposals**

Dispatch one subagent per batch with the batch rows and the audience rules; the subagent returns the proposals JSON where `proposed` = authored opening sentence (containing ALL the row's `missing` words naturally) + the preserved body (first sentence replaced when it is the one being improved). Whole-authored texts only for the empty description. No boilerplate, no JIPipe jargon, outcome first, 2D/3D for ROI nodes, composed length within 500 chars where the body allows.

- [ ] **Step 2: Mechanical pre-check (pre-filter only)**

A Python check (the planning mirror of the gate rule) verifies every `proposed` covers its row's `missing` words and length stays within the cap where required. Failures go back to the authoring step. The authoritative check is the gate re-run in step 5.

- [ ] **Step 3: Review pass**

A fresh subagent reviews the batch: does each sentence read naturally with the preserved body, carry no boilerplate or jargon, state 2D/3D for ROI nodes, keep deprecation notices (as body), and stay factually consistent with the node's name and current body? Rejected rows return to step 1.

- [ ] **Step 4: Apply**

`python3 utils/audit-node-descriptions.py apply --proposals utils/taxonomy-research/description-sweep/proposals-batch-NN.json`

- [ ] **Step 5: Round-trip + gate delta**

`python3 utils/extract-node-descriptions.py` must yield exactly the proposed text for every touched node; re-run the gate test and confirm the failure count dropped by the batch's resolvable-row count (fewer dropped = an inherited-documentation case; investigate before continuing). Commit the batch.

### Task 4: Leftovers, gate GREEN, verification, MR

**Files:**
- Modify: the classes/resources of the leftover rows
- Modify: `docs/superpowers/specs/2026-11-24-search-finetuning-design.md` (only if execution uncovered another spec-fact correction)

**Interfaces:**
- Consumes: the last gate dump (the failing set must be exactly the known leftovers).
- Produces: the gate GREEN; the sweep complete.

- [ ] **Step 1: Fix the known leftovers**

Investigate the one node with no in-tree class (`trackmate-create-spot-tracker-simple-sparse-lap-tracker`): locate its registration, fix its description in place or record why it is not JIPipe-authored (then it belongs to the auto-import scope rule - extend the rule's javadoc with the third family, do not add an exemption). Fix the 2 resource-backed nodes in their resource files. The empty description (`annotation-table-from-file`) is handled in its batch (Task 3).

- [ ] **Step 2: Gate GREEN**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS with zero failures.

- [ ] **Step 3: Full verification**

`mvn -pl jipipe-core clean test -o` (background, ~10 min) and the launcher module's test run; both green.

- [ ] **Step 4: Ledger, push, MR**

Record the final counts in the SDD ledger; dispatch the final fresh whole-branch reviewer (same protocol as MR A); fix Critical/Important findings, ledger the rest; push `1404-description-sweep`; open the MR (target master; if !216 has not merged yet, target it and retarget after it merges). The MR description lists: scope decision A, the batch table, the leftovers' outcomes, and the deferred minors.
