# Description Sweep (MR B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every JIPipe-authored node's description contains its name's content words, enforced by a permanent registry-wide gate test.

**Architecture:** A launcher-side gate test (`SearchDescriptionQualityTest`) defines the rule with the search's own tokenizer (`NodeSearchText`); the same test run dumps the failing worklist (opt-in system property). A Python builder merges the dump with source-file resolution (from the existing audit tooling) and the usage prior into a batched, usage-ordered worklist. Batches of authored opening sentences are applied with the existing `utils/audit-node-descriptions.py` apply machinery (extended with a `--proposals` path), verified by extractor round-trip and the gate itself.

**Tech Stack:** Java 21 (jipipe-launcher tests, live registry), Python 3 (worklist builder, apply reuse), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-24-search-finetuning-design.md` (sections "Description sweep shape", components 6-7). Supplement measured facts below.

## Global Constraints

- Scope ruling (user, 2026-11-24, option A): the gate structurally skips the two auto-import families - node ids starting with `ij2:op:` and `clij:plugin:`; their descriptions are third-party metadata (ImageJ2 op registry, CLIJ kernels), not JIPipe-authored text. This is a scope rule, not an exemption list. All other registered nodes are in scope; there are no exemptions.
- Gate predicate (single source): for every registered node info, every word of `NodeSearchText.contentWords(info.getName())` must satisfy `NodeSearchText.coveredBy(word, NodeSearchText.wordSet(plainText))` where `plainText = info.getDescription().toPlainText()`. Empty descriptions fail (the word set is empty). The gate never re-implements the tokenizer in test code.
- Authored sentences must contain ALL missing content words, preserve the existing description body (rewrite or prepend only the opening sentence), keep the whole description at most 500 characters (the apply tool's `MAX_DESC_CHARS`), use no em/en dashes (project rule), read naturally (outcome first, no JIPipe data-model jargon, 2D/3D stated for ROI nodes), and avoid boilerplate stamping (the design's anti-goal).
- The sweep does NOT write a fresh apply script: `utils/audit-node-descriptions.py` is extended minimally (`--proposals` path argument) and used for every edit.
- Batches are deterministic: rows ordered by corpus usage (instances from the shipped prior, descending; zero-usage rows after, alphabetically by node id), grouped so a batch stays within one source module, target 30-50 rows per batch (large modules split at usage-order boundaries).
- Intermediate commits on the branch may leave the gate RED (it goes green in the final task); the branch is pushed once, after the gate is green.
- Tests must not write the real user settings; the gate dump goes to a path given via `-Djipipe.desc-sweep.dump=` (opt-in, `NodeRegistryDumpToolTest` precedent).
- No Swing-structure tests; the gate is a registry/ship-set contract (sanctioned test class).

## Review Focus

- Abbreviation names (`RGB`, `LAB`, `HSB`, `LUT`, `EVF`, `OME`, `ROI`): the sentence must genuinely use the term in context, not stuff the missing word - pinned by the per-batch review checklist and the gate (which only checks containment, so quality is the author's duty).
- Two node ids sharing one class file (inherited documentation): a batch applying two proposals to one file must not clobber the first edit - pinned by the apply tool's in-memory sequential apply (already implemented) plus the extractor round-trip asserting BOTH texts land.
- HTML-bearing descriptions (`<ul>`, `<br/>`, `<code>`): the new opening sentence goes before the markup, and the gate judges `toPlainText` - pinned by Task 1's gate body choice and the round-trip.
- The 500-character cap against long existing bodies (e.g. the TSOAX, Cellpose, TurboReg descriptions): the sentence must be short or the body condensed - pinned by the worklist builder flagging rows whose current body exceeds 460 characters (they need condensation, noted in the batch data).
- Auto-import look-alikes: ids with colons that ARE JIPipe-authored (`jipipe:comment`, `jipipe:compartment-output`, ...) must NOT be skipped by the scope rule - pinned by a gate test assertion that the scope filter only matches the two exact prefixes, and by the execution-time check of the 2 remaining colon-id failures.

---

### Task 1: Gate test `SearchDescriptionQualityTest` (RED) + worklist dump

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/SearchDescriptionQualityTest.java`
- Test: same file

**Interfaces:**
- Consumes: `NodeSearchText.contentWords(String)`, `NodeSearchText.wordSet(String)`, `NodeSearchText.coveredBy(String, Set<String>)`, `JIPipe.ensureInstance()`, `JIPipe.getNodes().getRegisteredNodeInfos()`, `JIPipeNodeInfo.getName()`, `JIPipeNodeInfo.getDescription().toPlainText()`.
- Produces: the gate (permanent regression test) and, with `-Djipipe.desc-sweep.dump=<path>`, a JSON file `{"generated": "...", "failing": [{"nodeId", "name", "missing": [...], "description"}]}` ordered by node id. Later tasks consume the dump only.

- [ ] **Step 1: Write the gate test**

Mirror `TaxonomyFullValidationTest`'s boot (`@BeforeAll` calls `JIPipe.ensureInstance()`). One test method, `everyJIPipeAuthoredNodeDescriptionContainsItsNameWords`: iterate `JIPipe.getNodes().getRegisteredNodeInfos()`, skip ids starting with `ij2:op:` or `clij:plugin:` (scope rule in a small static helper `isInSweepScope(String nodeId)` so the filter is testable), collect failures as `nodeId + " (" + name + "): missing " + missing`, and assert the failure list is empty with the joined report as the assertion message. When `jipipe.desc-sweep.dump` is set, write the dump JSON (sorted by node id) before asserting. A second test method, `scopeFilterOnlySkipsTheAutoImportFamilies`, asserts `isInSweepScope` is false only for the two exact prefixes and true for `jipipe:comment`, `ij1-threshold-auto2d`, `annotation-table-from-file`.

- [ ] **Step 2: Run it and record the RED state**

Run: `mvn -q -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Djipipe.desc-sweep.dump=/tmp/opencode/desc-sweep-dump.json -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with ~398 failures (397 non-empty + 1 empty, planning count; the live registry decides), and the dump file written.

- [ ] **Step 3: Commit (the gate stays RED until the final task)**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/SearchDescriptionQualityTest.java
git commit -m "test(#1404): description quality gate (name content words in description, JIPipe-authored scope)"
```

### Task 2: Worklist builder + `--proposals` apply argument

**Files:**
- Create: `utils/search-usage/build-description-sweep-worklist.py`
- Modify: `utils/audit-node-descriptions.py` (add `--proposals PATH` argument, default the existing `node-description-audit-proposals.json`; thread it through `cmd_apply`)
- Test: existing `utils/audit_node_descriptions_tests.py` (selftest still passes); builder verified by its output

**Interfaces:**
- Consumes: the Task 1 dump; `audit.resolve_sources(".")` (node id -> class name, file path, source text); `jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json` (`counts[id].instances`).
- Produces: `utils/taxonomy-research/node-description-sweep-worklist.json` - `{"generated", "batches": [{"id", "module", "rows": [{"nodeId", "name", "file", "class", "missing", "description", "needsCondensation"}]}]}`. Rows ordered by usage then id, grouped into module-honoring batches of 30-50. `needsCondensation` is true when the current plain-text description (HTML stripped) exceeds 460 characters.

- [ ] **Step 1: Add `--proposals` to the audit tool**

New argparse argument on `apply` (and `draft` for symmetry): `--proposals`, default `PROPOSALS`. `cmd_apply` takes the path as a parameter. No behavior change otherwise.

- [ ] **Step 2: Run the selftest**

Run: `python3 utils/audit-node-descriptions.py selftest`
Expected: all pass.

- [ ] **Step 3: Write the builder and generate the worklist**

The builder reads the three inputs, resolves files (rows whose class cannot be resolved are listed in a top-level `"unresolved"` array with their ids - Task N investigates them, none are silently dropped), and writes the batched worklist. Deterministic output (sorted keys).

Run: `python3 utils/search-usage/build-description-sweep-worklist.py --dump /tmp/opencode/desc-sweep-dump.json --out utils/taxonomy-research/node-description-sweep-worklist.json`
Expected: ~397 resolvable rows in ~10 batches, 1-2 unresolved ids listed.

- [ ] **Step 4: Commit**

```bash
git add utils/search-usage/build-description-sweep-worklist.py utils/audit-node-descriptions.py utils/taxonomy-research/node-description-sweep-worklist.json
git commit -m "feat(#1404): description sweep worklist builder; --proposals argument for the apply tool"
```

### Tasks 3-N: Batch authoring and application (one task per batch, in worklist order)

**Files (per batch):**
- Modify: the batch rows' source files (via the apply tool only)
- Create (per batch): `utils/taxonomy-research/node-description-sweep-proposals-batch-<n>.json`

**Interfaces:**
- Consumes: the Task 2 worklist batch; the apply tool (`apply --proposals <batch file>`); the extractor (`utils/extract-node-descriptions.py --out <path>`); the Task 1 gate.
- Produces: applied descriptions in source; per-batch proposal files kept in the repository as the review record.

- [ ] **Step 1: Author the batch proposals**

For each row: write an opening sentence containing ALL `missing` words, naturally continuing (or replacing the weak opening of) the existing description body. Keep the total plain-text length at most 500 characters; rows flagged `needsCondensation` get their body condensed. Ground the sentence in what the node does (the class source is available via `audit.resolve_sources` when the description is unclear; do not invent behavior). Respect the audience rules and the no-dash rule. Record each as a proposal object `{nodeId, file, class, bucket: "gate", current, proposed}` in the batch file.

- [ ] **Step 2: Mechanical pre-check**

Run: a short Python check over the batch file that every `proposed` text, HTML-stripped, contains each of the row's `missing` words case-insensitively as a prefix/stem/substring match, and is at most 500 characters.
Expected: all rows pass (fix the sentence, not the check).

- [ ] **Step 3: Apply and round-trip**

Run: `python3 utils/audit-node-descriptions.py apply --proposals utils/taxonomy-research/node-description-sweep-proposals-batch-<n>.json` then `python3 utils/extract-node-descriptions.py --out /tmp/opencode/desc-extract-after.json`
Expected: apply reports the batch count; the extractor output equals the proposed text for every touched node.

- [ ] **Step 4: Gate delta**

Run: `mvn -q -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -c "missing"` (failure lines before vs after)
Expected: the failure count drops by the batch size.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs(#1404): description sweep batch <n> (<module>, <count> nodes)"
```

Batch map (module: approx rows): ij-algorithms 184 (split into 4 batches by usage order), ij-3d 62 (2 batches), ij-filaments 26, filesystem+filesystem-adjacent 15, tables 14, ij 13, utils 12, omero+annotations 18, core+python+strings+forms 20, trackmate+cellpose+scene-3d+clij+opencv+weka+ij2+remaining 26. The worklist file's batch ids are authoritative; the map only sizes the effort.

### Final Task: Gate GREEN, stragglers, verification, MR

**Files:**
- Modify: the 2 resource-backed descriptions' resource files (hand edit); the unresolved row's source (investigate); the old pilot proposal if unapplied
- Test: the gate (goes GREEN)

- [ ] **Step 1: Resolve the stragglers**

Investigate the worklist's `unresolved` ids (expected: `trackmate-create-spot-simple-sparse-lap-tracker` - find its class or registration) and the 2 non-auto-import colon-id failures from Task 1's RED run (extend the scope rule with evidence if they turn out to be auto-imported). Fix the 2 resource-backed nodes in their resource files. Check whether the old pilot proposal (`ij1-roi-to-rgb` in `node-description-audit-proposals.json`) is still unapplied and fold it in.

- [ ] **Step 2: Gate GREEN + full verification**

Run: `mvn -q -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Dsurefire.failIfNoSpecifiedTests=false` (expected: green), then `mvn -pl jipipe-core clean test -o` (expected: all green), then `mvn -q -pl jipipe-launcher -am test -o -Dtest='TaxonomyFullValidationTest,NodeRegistryDumpToolTest' -Dsurefire.failIfNoSpecifiedTests=false` (expected: green).

- [ ] **Step 3: Ledger + fresh whole-branch review**

Append the final counts to the SDD ledger, generate the review package (`review-package` script), and dispatch a fresh reviewer subagent over the whole branch (gate test + builder + all description diffs, checking sentence quality against the review checklist). Fix Critical/Important findings; ledger the rest.

- [ ] **Step 4: Push and open the MR**

Push `1404-description-sweep`. Create the MR (target `master` if !216 has merged, else `1404-search-finetuning`); description states the scope ruling (option A), the applied counts, the 2 hand-fixed resource files, and any deferred minors. Link the MR to this session.
