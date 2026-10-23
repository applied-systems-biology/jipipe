# JIPX Phase 0 — Corpus Verification Record (Task 6, #753)

**Date:** 2026-10-23
**Branch:** `753-jipx-project-format` (worktree `.worktrees/753-jipx-project-format`)
**Scope of proof:** the Tasks 2–5 extraction of `.jip` read/write behind
`JIPipeProjectReader`/`JIPipeProjectWriter`/`JIPipeProjectIOs` must be
behavior-preserving against the baseline captured on the unrefactored code (Task 1, fix round 1).

---

## 1. Method

**Corpus:** `/data/JIPipe` — **367** `.jip` files (142 top-level, recursive; versions
1.0.0–6.0.0 incl. Development builds). The corpus is read-only for this verification:
the harness round-trips temp *copies* (Task 1's pollution guard asserts the top-level
`*.jip` count is unchanged; the after-run passed it — `Tests run: 1, Failures: 0`).

**Instrument:** `CorpusRoundTripFixpointTest` (jipipe-launcher), **unchanged since the
baseline capture** (`git diff c7f5cbc7b2..7aac99317b -- <harness>` is empty). Per file:
load → save S1 → load S1 → save S2; records `outcome`, `s1vs2SemanticallyEqual`
(canonical JSON equality — recursively sorted objects/arrays, per the Task 1 canonical
ruling), `pvs1TopLevelChangedKeys` (top-level keys whose canonical value differs between
the original P and S1; diagnostic), `loadedNodesS1/S2`, `error` (diagnostic only).

**Reports:**
- Baseline (unrefactored code `c7f5`, captured 17:02): `/data/JIPipe/.roundtrip/baseline.json`
- After (refactored code `7aac99317b`, run 18:01–18:04): `/data/JIPipe/.roundtrip/after.json`
- Control (unrefactored code `c7f5`, run 18:16–18:19, current environment): `/tmp/opencode/jipx-task6-control-unref.json`
- After reproducibility (refactored, 18:10–18:13): `/tmp/opencode/jipx-task6-after2.json`

**Comparison scope (amended per controller rulings):** global `totalFiles` and `outcomes`
counts; per entry (matched by `file`, same order): `outcome`,
`s1vs2SemanticallyEqual`, `pvs1TopLevelChangedKeys`, `loadedNodesS1`, `loadedNodesS2`.
The `error` diagnostic field is **redacted** from the comparison (JDK NPE
helpful-message elision flaps across JVMs — proven JDK-side in Task 1).
Comparison script: `compare-reports.py` in the SDD workspace (working artifact, not
committed).

---

## 2. Outcome counts (identical across baseline, after, control)

| | OK | PARSE_FAILED | LOAD_FAILED | SAVE_FAILED | total |
|---|---|---|---|---|---|
| baseline (unrefactored, 17:02) | 356 | 0 | 11 | 0 | 367 |
| after (refactored, 18:04) | 356 | 0 | 11 | 0 | 367 |
| control (unrefactored, 18:19) | 356 | 0 | 11 | 0 | 367 |

`totalFiles` = 367 in all three. `s1vs2SemanticallyEqual` = true on 284 / false on 72 of
the 356 OK files in **all** reports. `loadedNodesS1/S2` identical for all 367 files in
**all** reports. Every LOAD_FAILED file is the same file with the same stage mapping.

## 3. `pvs1TopLevelChangedKeys` buckets (baseline)

Non-empty for all 356 OK files. Key frequency across OK files:

| key | files | meaning |
|---|---|---|
| `jipipe:doc:parameters` | 354 | regenerated parameter-doc cache |
| `graph` | 353 | graph re-serialization (incl. doc cache, order-normalized away) |
| `dependencies` | 350 | see §5 — membership-sensitive in a handful of files |
| `metadata` | 335 | authors merge / default metadata fields |
| `settings` | 287 | settings sheet regenerated with current defaults |
| `compartments` | 286 | regenerated compartments array |
| `jipipe:project-jipipe-version` | 286 | version upgrade stamp (old → current 6.0.0) |
| `runtime-partitions` | 285 | regenerated runtime-partition settings |
| `external-environments` | 160 | env definitions re-serialized |
| `run-sets` | 155 | regenerated run-set entries |
| `additional-metadata` | 129 | metadata additions on old-format files |
| `jipipe:project-format-version` | 18 | format-version stamp on old files |

These are the expected version-upgrade + regenerated-cache buckets (plan expectation
confirmed).

## 4. Fixpoint violations found in Task 1 and their triage (carried forward)

- **~280 ordering-only** S1≠S2 files: `HashSet<JIPipeMutableDependency>` iteration order
  (no value-based `equals`/`hashCode`; fresh set per save) — **erased by the canonical
  comparison** (Task 1 ruling 1); no longer violations (the 284 true count includes them).
- **72 real-content-churn files** (deterministic, stable across all 8 runs): three
  pre-existing bugs, unchanged by the extraction:
  1. env `artifact-query/query-v2` `null` → `"null"` (~67 files, env-overrides churn);
  2. slot `custom-name` `null` → `""` (5 files);
  3. auto-generated parameter doc drops `important: true` (1 file, tutorial-project-test.jip).
- **11 LOAD_FAILED** old-format files (4 legacy `algorithm-graph`, 5 missing required
  sub-field, 2 unregistered `IOInterfaceAlgorithm`) — expected; same set in all runs.
- **Warmup-sensitive dependency membership** — flagged in Task 1 ("one early outlier saw
  9 vs 10 … membership can be warmup-sensitive in edge cases"). **This is what §5
  proves is still live.**

## 5. Parity result

**Comparison script output (after-run vs. baseline):**

```
PARITY FAILED: 6 differences across 367 entries:
  /data/JIPipe/blobs-labkit-example.jip :: pvs1TopLevelChangedKeys
    baseline: ['dependencies']
    after:    []
  /data/JIPipe/blobs-multi.jip :: pvs1TopLevelChangedKeys
    baseline: ['dependencies', 'graph', 'jipipe:doc:parameters', 'jipipe:project-jipipe-version', 'settings']
    after:    ['graph', 'jipipe:doc:parameters', 'jipipe:project-jipipe-version', 'settings']
  /data/JIPipe/labkit-example.crate/project.jip :: pvs1TopLevelChangedKeys
    baseline: ['dependencies']
    after:    []
  /data/JIPipe/library_test.jip :: pvs1TopLevelChangedKeys
    baseline: ['graph', 'jipipe:doc:parameters', 'jipipe:project-jipipe-version', 'runtime-partitions', 'settings']
    after:    ['dependencies', 'graph', 'jipipe:doc:parameters', 'jipipe:project-jipipe-version', 'runtime-partitions', 'settings']
  /data/JIPipe/table-merging-test.jip :: pvs1TopLevelChangedKeys
    baseline: ['dependencies', 'graph', 'jipipe:doc:parameters']
    after:    ['graph', 'jipipe:doc:parameters']
  /data/JIPipe/test-archiving.crate-1/project.jip :: pvs1TopLevelChangedKeys
    baseline: ['graph', 'jipipe:doc:parameters']
    after:    ['dependencies', 'graph', 'jipipe:doc:parameters']
```

Exit code 1. **All 6 differences are in the single field
`pvs1TopLevelChangedKeys`, all in the membership of the `dependencies` key**
(4 files: key present in baseline, absent after; 2 files: the reverse — mixed
direction). No other ruling-scoped field differs anywhere: `outcome`,
`s1vs2SemanticallyEqual` (incl. the 72-churn set), `loadedNodesS1/S2`, `totalFiles`,
`outcomes` are byte-identical across all three reports.

**Reproducibility:** refactored-code run 2 (after2) is field-identical to run 1:
`PARITY: 367 entries, 0 differences` (exit 0) — the 6-difference set is stable across
refactored runs in this environment.

**Control experiment (decisive):** the **unrefactored** harness + **unrefactored** core
(run 18:16–18:19, after the baseline's environment shifted) diverges from the baseline
in the **same field and mechanism** on a **disjoint file set (9 files**:
`Lecture_Data/JSC_2025_05_Batch_Processing_Blobs/blobs.jip`, `blobs-test.jip`,
`blobs-tunnel.jip`, `cellpose-test.jip`, `cellpose.jip`,
`jsc2025_example_blobs_batch_processing.crate/project.jip`, `kidney-test.jip`,
`project-dirs-with-files.jip`, `test123456.crate/project.jip` — all with
`dependencies` added to the changed set). Zero overlap with the refactored run's set.

**Interpretation:** the `dependencies` entry of `pvs1TopLevelChangedKeys` reflects
whether the *computed* minimal dependency **membership** equals the stored one.
Membership is derived per node from the registered plugin/data-type source maps
(`JIPipe.getNodes().getSourceOf(id)` / `JIPipe.getDataTypes().getSourceOf(type)`), i.e. it
is a function of the JVM's registry state, which Task 1 established as warmup-sensitive
in edge cases. The environment shifted between the baseline capture (17:02) and the
after/control runs (18:0x): a live JIPipe GUI instance (started 18:01) shares the
`/home/ruman/.local/share/JIPipe` profile and rewrote the profile's node-db/settings
state at 18:02:23 *during* the after-run, and concurrent `mvn install` activity in the
main worktree touched the shared `~/.m2` snapshot artifacts. The refactored and
unrefactored codes landed in different-but-each-its-own membership states — hence the
disjoint, mixed-direction flip sets. **The extraction itself is not the cause:** the
identical difference class is produced by the unrefactored code under the same
environment drift.

**Error-field differences (redacted, recorded per ruling 2):** exactly 1 —
`ometest.jip`: baseline `java.lang.NullPointerException: java.lang.NullPointerException`
vs. after `java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.asText()" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null` — the documented JDK-21 helpful-message elision flap; outcome and stage identical.

**Verdict on the gate as run today:** the after-run does **not** satisfy exact parity —
6 ruling-scoped-field entries differ. Per the Task 6 constraint this is reported, not
weakened. However, the control experiment shows the gate's *baseline* (captured 17:02 in
a quiet environment) is not a valid reference in the current environment: **the same
unrefactored code also fails it (9 entries).** The failure is environmental, not
regressive.

### What is verified vs. what is not

**Verified (evidence holds regardless of the membership confound):**
- `outcome` parity: 356 OK / 11 LOAD_FAILED, identical file sets and stage mappings.
- Fixpoint parity under canonical semantics: identical 284/72 split, identical 72-file
  churn set.
- Load parity: identical `loadedNodesS1/S2` for all 367 files (no silent node loss or
  gain in the extraction — the review-focus #1 contract).
- No corpus pollution (built-in guard passed in every run).
- All 6+9+0 flip entries differ *only* in the `dependencies` membership diagnostic; in
  none of them does any other key, value, node count, or outcome change.

**Not yet verified (blocked by environment, not by the code):**
- The `pvs1TopLevelChangedKeys` field for the `dependencies` entry of the ~15 affected
  files cannot be parity-judged against the 17:02 baseline because the baseline's
  membership state is unreproducible in the current environment (proven by the control
  run). A **re-capture of the baseline in a controlled environment** (no live GUI
  instance writing the shared profile; no concurrent `mvn install` of jipipe-core
  into the shared `~/.m2`) — followed by a fresh after-run under the same conditions —
  is required to close the gate formally. Given the control evidence, the expected
  outcome of that re-run is parity on all ruling-scoped fields.

## 6. Full module regression

`mvn -pl jipipe-core,jipipe-launcher test` (refactored core `7aac99317b` reinstalled to
`~/.m2` before the run; harness skipped by gate): see §7 for the result.

### 5.1 Controlled re-capture (controller follow-up, 2026-10-23)

Requested as the decisive closure evidence (fresh unrefactored baseline + fresh
refactored after-run back-to-back in a controlled environment). **Not performed:**
the live JIPipe GUI instance (PIDs 2016822/2019071, the same one that contaminated
the 18:0x window) was still running, and per the standing instruction it was not
killed. A re-capture while it is active would not be controlled (shared profile
`/home/ruman/.local/share/JIPipe`), so the run was deferred, not faked. Everything
is staged for an ~8-minute run once the GUI is closed (commands in the task-6
report); the environment is otherwise verified ready (unrefactored commit
`c7f5cbc7b2` carries the canonical harness; `~/.m2` provenance md5-verified per
run). Note for the controller: JIPipe honors `JIPIPE_OVERRIDE_USER_DIR_BASE`
(`PathUtils.getJIPipeUserDirBase`), which would isolate the harness from the GUI's
profile if a GUI-free window is not available.

## 7. Regression result

`mvn -pl jipipe-core,jipipe-launcher test` — **BUILD SUCCESS**, 2026-10-23 18:25 (log:
`/tmp/opencode/jipx-task6-regression.log`):

| module | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| jipipe-core | 507 | 0 | 0 | 1 |
| jipipe-launcher | 28 | 0 | 0 | 1 |

Both skips are pre-existing gated tests (jipipe-launcher's skip is
`CorpusRoundTripFixpointTest` itself — `assumeTrue` on the corpus property, skipped by
design outside a gate run; verified identical to the Tasks 2–5 baseline behavior).
Run against the refactored core `7aac99317b` (reinstalled to `~/.m2` immediately before
the run; md5-verified). The full IO suite is green: `ProjectIOsDispatchTest` 5/5,
`LegacyProjectReaderContractTest` 1/1, `LegacyProjectWriterContractTest` 2/2,
`FixtureProjectsLoadTest` 3/3, `TaxonomyFullValidationTest` 1/1,
`SystemPatternTemplatesTest` 8/8.

---

## Closing statement

**The `.jip` read/write extraction (Tasks 2–5) is verified behavior-preserving on every
stable, semantically meaningful dimension: load outcomes, node counts, the canonical
fixpoint (incl. all 72 pre-existing churn files), and save outcomes are identical across
367 real corpus files before and after the refactor. The gate as executed today
reports 6 differences, all in one environment-sensitive diagnostic field
(`pvs1TopLevelChangedKeys` → `dependencies` membership); a control run proves the
identical difference class on the unrefactored code under the same environment drift
(GUI + shared profile + shared `~/.m2` contended during the run window), so the
extraction is exonerated and the remaining gap is a re-capture under controlled
conditions, not a code fix. That re-capture was requested as follow-up but is
**blocked pending the live GUI instance being closed** (see §5.1 — not performed,
not faked); it is staged to complete in one ~8-minute paired run. On the basis of
this record, `.jip` read/write is confirmed behavior-preserving on every stable,
semantically meaningful dimension, and Phase 1 (`.jipx` reader/writer) may proceed
with the §5.1 re-capture as the outstanding formal gate item.**
