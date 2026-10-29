# JIPX Phase 0 — `.jip` IO Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract a dedicated reader/writer layer for the existing `.jip` format behind a format-dispatch API, and prove
with a corpus round-trip harness that the extraction is behavior-preserving — the verified foundation the `.jipx` format
(Phase 1) builds on.

**Architecture:** Pure extraction refactor: new package `org.hkijena.jipipe.api.project.io` with `JIPipeProjectIO` ←
`JIPipeProjectReader`/`JIPipeProjectWriter` interfaces, `JIPipeLegacyProjectReader`/`JIPipeLegacyProjectWriter` holding
the exact current load/save logic, and a static `JIPipeProjectIOs` dispatcher (content sniff for reads, extension for
writes) plus a `peek` helper for the GUI's pre-open dependency/metadata checks. `JIPipeProject.loadProject/saveProject`
become thin delegates. Verification is **baseline parity**: a system-property-gated harness round-trips the 367-project
local corpus *before* the refactor to capture a baseline report, then again *after* — the two reports must match
exactly.

**Tech Stack:** Java 21, Maven, Jackson (`JsonUtils.getObjectMapper()`), JUnit 5 (jipipe-launcher test module).

**Spec:** `docs/superpowers/specs/2026-10-23-jipx-project-format-design.md` (§3 Phase 0; §3.2 verification harness; §3.1
IO interfaces)

## Global Constraints

- Java 21, Maven. Module tests: `mvn -pl jipipe-launcher test` / `mvn -pl jipipe-core test` (run from repo root;
  jipipe-launcher depends on jipipe-core).
- All new production code in package `org.hkijena.jipipe.api.project.io`, directory
  `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/`.
- All new tests in `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/` — the launcher is the only module
  with all plugin modules on the classpath (`nodesFromJson` silently skips unknown node ids on jipipe-core's classpath,
  making load tests vacuous there).
- Every new file carries the standard MIT copyright header (copy verbatim from
  `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java:1-12`).
- **Zero behavior change** to `.jip` read/write semantics — this is a pure extraction. Proof = corpus baseline parity
  (Task 6), not code review.
- Harness gated on `-Djipipe.jip.roundtrip.corpus=<dir>`; must skip (assumeTrue) when absent so CI stays green. Default
  corpus on this machine: `/data/JIPipe` (367 `.jip` files, recursive; versions 1.0.0–6.0.0 including `Development`
  builds, 39 without a core dependency).
- **Never write into the corpus directory** except the sanctioned report location `/data/JIPipe/.roundtrip/`. The
  harness round-trips temp *copies*, never corpus files in place.
- IO format ids are stable string literals: `"org.hkijena.jipipe.core:jip"`.
- No Swing-structure tests, no trivial getter/setter tests (project test policy). The harness and contract tests below
  are the warranted non-trivial kind (serialization round-trips, contracts).
- Tests calling `JIPipe.ensureInstance()` are safe in jipipe-launcher: `ImageJLegacyPatcherLauncherSessionListener`
  preinits the ImageJ1 patcher. Do not remove it or add `--add-opens` workarounds.
- Work on branch `753-jipx-project-format` (worktree `.worktrees/753-jipx-project-format`), per
  `<issue-id>-<kebab-name>` convention. The spec commits already on master stay there.
- Do not touch: `FixtureProjectsLoadTest`, `ImageJLegacyPatcherLauncherSessionListener`, the instrumentation system, the
  RO-Crate publisher.

## Review Focus

1. **Silent node skipping** — `JIPipeGraph.nodesFromJson` skips unknown node ids without throwing; a load that drops
   nodes looks successful. *Pinned by:* the fixpoint invariant (raw S1 ≡ raw S2, Task 1) — a node lost in load (S1) is
   absent from the in-memory model, never written to S2, and the raw comparison fails; plus non-vacuity node counts in
   Tasks 2–3 contract tests (fixture with a known node id).
2. **Corpus pollution** — loading/saving must not create files inside `/data/JIPipe` (user paths, temp dirs,
   `JIPipe.tmp.dir`). *Pinned by:* Task 1 harness snapshots the corpus's top-level `*.jip` list before/after and asserts
   it unchanged; round-trip happens on temp copies.
3. **One bad file must not abort the corpus run** — unparseable/ancient files (129 lack a version field; versions back
   to 1.0.0) must be recorded as an outcome, not crash the harness. *Pinned by:* Task 1 per-project try/catch with
   `PARSE_FAILED`/`LOAD_FAILED`/`SAVE_FAILED` outcomes; the full run completing with any mix of outcomes is the test.
4. **Save-path side-effect regressions** — `saveProject(Path, false)` must leave `getProjectFile()` unchanged,
   `(Path, true)` must update it; the authors merge happens **only** in the `Path` variant (not `Writer`/
   `OutputStream` — current behavior). *Pinned by:* Task 3 contract test.
5. **Save non-determinism** — any field regenerated per save (timestamp, random id) would break the fixpoint on
   *current* code. *Pinned by:* Task 1 full-corpus run — a `FIXPOINT_VIOLATION` on current code is a finding to
   investigate and report to the user before Phase 0 proceeds, never something to silently baseline.

---

### Task 1: Corpus round-trip fixpoint harness (the verification instrument)

Runs against the **current, unrefactored** `JIPipeProject.loadProject/saveProject` to capture the baseline.

**Files:**

- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/CorpusRoundTripFixpointTest.java`

**Interfaces:**

- Consumes:
  `JIPipeProject.loadProject(Path, JIPipeValidationReportContext, JIPipeValidationReport, JIPipeNotificationInbox, JIPipeProgressInfo)`,
  `JIPipeProject.saveProject(Path, boolean)` — unchanged, as they exist today.
- Produces: the baseline report `/data/JIPipe/.roundtrip/baseline.json` (schema below — Task 6 compares against it);
  harness behavior contract for Task 6's re-run.

- [ ] **Step 1: Write the gated harness test**

`CorpusRoundTripFixpointTest` (single `@Test` method; `@BeforeAll` calls `JIPipe.ensureInstance()`):

- `assumeTrue` on system property `jipipe.jip.roundtrip.corpus` (a directory); optional `jipipe.jip.roundtrip.report`
  (report output path, default `<corpus>/.roundtrip/report.json`); optional `jipipe.jip.roundtrip.limit` (int, process
  only the first N files — smoke runs).
- Discover inputs: `Files.walk` over the corpus dir, filter filenames ending `.jip` (case-insensitive), sorted. Snapshot
  the top-level `*.jip` count of the corpus root before the run.
- Per project `P` (each in its own try/catch; **continue on any failure**):
    1. Copy `P` to a fresh `@TempDir`-based working dir as `project.jip` (never touch the corpus file).
    2. Cycle 1: `loadProject(copy)` → `saveProject(s1Path, false)` → Cycle 2: `loadProject(s1Path)` →
       `saveProject(s2Path, false)`.
    3. Fixpoint check: `JsonUtils.getObjectMapper().readTree(s1)` `.equals(` `readTree(s2)` `)` — Jackson
       `ObjectNode.equals` is key-order-insensitive, so this is a semantic comparison. Record `s1vs2SemanticallyEqual`.
    4. P→S1 diff (diagnostic only, never fails the run): for each top-level key in either tree, record
       added/removed/changed → `pvs1TopLevelChangedKeys` (sorted list).
    5. Diagnostics: `loadedNodesS1`/`loadedNodesS2` = `p.getGraph().traverse().size()`.
    6. Outcome per project: `OK` | `PARSE_FAILED` | `LOAD_FAILED` | `SAVE_FAILED` (with a one-line `error` string for
       failures).
- After all projects: assert the corpus root's top-level `*.jip` count is unchanged.
- Write the report as a single JSON document (no timestamps, no timings — Task 6 compares two reports for exact
  equality):

```json
{
  "corpus": "<corpus dir>",
  "totalFiles": 367,
  "outcomes": {"OK": 0, "PARSE_FAILED": 0, "LOAD_FAILED": 0, "SAVE_FAILED": 0},
  "entries": [
    {"file": "<absolute corpus path>", "outcome": "OK",
     "s1vs2SemanticallyEqual": true,
     "pvs1TopLevelChangedKeys": ["jipipe:project-jipipe-version"],
     "loadedNodesS1": 0, "loadedNodesS2": 0,
     "error": null}
  ]
}
```

- [ ] **Step 2: Smoke-run the harness on 3 files**

Run:
`mvn -pl jipipe-launcher test -Dtest=CorpusRoundTripFixpointTest -Djipipe.jip.roundtrip.corpus=/data/JIPipe -Djipipe.jip.roundtrip.limit=3 -Djipipe.jip.roundtrip.report=/tmp/opencode/jipx-smoke.json`
Expected: PASS; report contains 3 entries; no new non-report files in `/data/JIPipe`.

- [ ] **Step 3: Full corpus baseline run**

Run:
`mvn -pl jipipe-launcher test -Dtest=CorpusRoundTripFixpointTest -Djipipe.jip.roundtrip.corpus=/data/JIPipe -Djipipe.jip.roundtrip.report=/data/JIPipe/.roundtrip/baseline.json`
(long-running — up to ~1 h; use a long or no timeout).
Expected: PASS (the harness itself only fails on corpus pollution or a harness bug). The interesting output is the
report.

- [ ] **Step 4: Triage the baseline**

Review `baseline.json`: every `FIXPOINT_VIOLATION` (i.e. `s1vs2SemanticallyEqual: false`) or failure outcome gets a
cause classification. **Unexplained fixpoint violations are pre-existing current-code bugs — report them to the user
before proceeding with Tasks 2–5** (they may be in-scope to fix, they may not be silently baselined). Expected and
acceptable: version-field upgrades in `pvs1TopLevelChangedKeys` (`jipipe:project-jipipe-version`,
`jipipe:doc:parameters`, `external-environments` — regenerated caches).

- [ ] **Step 5: Commit**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/CorpusRoundTripFixpointTest.java
git commit -m "test(io): corpus round-trip fixpoint harness for .jip (baseline instrument, #753)"
```

### Task 2: Reader interfaces + `JIPipeLegacyProjectReader` + delegation

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectIO.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectReader.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeLegacyProjectReader.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java:196-222` (both `loadProject`
  overloads)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/LegacyProjectReaderContractTest.java`

**Interfaces:**

- Consumes: existing public `JIPipeProject` members `fromJson`, `setWorkDirectory`, `validateUserPaths`,
  `fixBrokenEnvironments` (all already public).
- Produces:
    - `interface JIPipeProjectIO { String getId(); List<String> getFileExtensions(); boolean supports(Path path); }`
    -
    `interface JIPipeProjectReader extends JIPipeProjectIO { JIPipeProject read(Path path, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException; }`
    - `class JIPipeLegacyProjectReader implements JIPipeProjectReader` — `getId()` → `"org.hkijena.jipipe.core:jip"`,
      `getFileExtensions()` → `["jip"]`, `supports(Path)` → regular file and (extension `jip`, case-insensitive).

- [ ] **Step 1: Write the failing contract test**

`LegacyProjectReaderContractTest` (`@BeforeAll JIPipe.ensureInstance()`): read the existing fixture
`basic-images.jipipe` (from `/projects/5.3.0-taxonomy-fixtures/`, same loading pattern as `FixtureProjectsLoadTest`) via
`new JIPipeLegacyProjectReader().read(path, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT)`;
assert: `getProjectFile().equals(path)`, `getWorkDirectory().equals(path.getParent())`,
`getGraph().traverse().size() == 1` (non-vacuity), and the single node's `getInfo().getId()` equals
`ij1-blur-gaussian2d`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-launcher test -Dtest=LegacyProjectReaderContractTest`
Expected: compilation failure — `JIPipeLegacyProjectReader` does not exist.

- [ ] **Step 3: Implement the interfaces and the reader**

`JIPipeLegacyProjectReader.read` = the **exact body** of the current 5-arg `loadProject` (JIPipeProject.java:211-222):
readValue → `new JIPipeProject()` → `fromJson` → `setWorkDirectory(path.getParent())` →
`validateUserPaths(notifications)` → setting-gated `fixBrokenEnvironments` → set `projectFile` (via `setProjectFile`) →
return. Both `JIPipeProject.loadProject` overloads become one-line delegates to the reader.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-launcher test -Dtest=LegacyProjectReaderContractTest`
Expected: PASS.

- [ ] **Step 5: Regression run**

Run: `mvn -pl jipipe-launcher test`
Expected: PASS (in particular `FixtureProjectsLoadTest` — it exercises the delegated `loadProject`).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/ jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/LegacyProjectReaderContractTest.java
git commit -m "refactor(io): extract JIPipeLegacyProjectReader behind JIPipeProjectReader, #753"
```

### Task 3: Writer interface + `JIPipeLegacyProjectWriter` + delegation

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectWriter.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeLegacyProjectWriter.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java:402-487` (all three `saveProject`
  overloads; widen `addAuthorsFromGlobalList` to public)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/LegacyProjectWriterContractTest.java`

**Interfaces:**

- Consumes: `JIPipeProjectIO` from Task 2; `JIPipeProject.addAuthorsFromGlobalList()` (widened to public in this task).
- Produces:
    -
    `interface JIPipeProjectWriter extends JIPipeProjectIO { void write(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo) throws IOException; void write(JIPipeProject project, Writer writer) throws IOException; void write(JIPipeProject project, OutputStream outputStream) throws IOException; }`
    - `class JIPipeLegacyProjectWriter implements JIPipeProjectWriter` — same id/extensions/`supports` as the reader.

- [ ] **Step 1: Write the failing contract test**

`LegacyProjectWriterContractTest`: load fixture `basic-images.jipipe` (via `JIPipeProject.loadProject`); then:

1. `new JIPipeLegacyProjectWriter().write(p, out, JIPipeProgressInfo.SILENT)` → parse `out` as JsonNode:
   `jipipe:project-type` == `"project"`; reload via `JIPipeLegacyProjectReader` → identical uuid→node-info-id mapping
   (reuse the `nodeIdMapping` pattern from `FixtureProjectsLoadTest`).
2. Side-effect contract on the wrapper: `p.saveProject(target, false)` leaves `p.getProjectFile()` unchanged;
   `p.saveProject(target2, true)` sets it to `target2`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-launcher test -Dtest=LegacyProjectWriterContractTest`
Expected: compilation failure — writer does not exist.

- [ ] **Step 3: Implement the writer and delegation**

`JIPipeLegacyProjectWriter.write(project, target, progressInfo)` = exact body of current `saveProject(Path, boolean)`
minus the `updateSavePath` side effect: authors merge
(`if (project.getMetadata().isAutoAddAuthors()) project.addAuthorsFromGlobalList()`) → pretty-print via
`JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(target.toFile(), project)`. The `Writer`/
`OutputStream` overloads = current `saveProject(Writer)`/`saveProject(OutputStream)` bodies — **no authors merge**
(current asymmetry, preserved exactly). `JIPipeProject.saveProject(Path, boolean)` delegates then applies
`if (updateSavePath) projectFile = fileName;` itself; the other two overloads delegate directly. Widen
`addAuthorsFromGlobalList` to public (Javadoc: used by the legacy writer).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-launcher test -Dtest=LegacyProjectWriterContractTest`
Expected: PASS.

- [ ] **Step 5: Regression run**

Run: `mvn -pl jipipe-launcher test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/ jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/LegacyProjectWriterContractTest.java
git commit -m "refactor(io): extract JIPipeLegacyProjectWriter behind JIPipeProjectWriter, #753"
```

### Task 4: `JIPipeProjectIOs` dispatcher + `peek`

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectIOs.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectPeek.java`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/ProjectIOsDispatchTest.java`

**Interfaces:**

- Consumes: `JIPipeLegacyProjectReader`/`JIPipeLegacyProjectWriter` (Tasks 2–3), `ArchiveUtils.isZipFile(Path)`,
  `JIPipeProject.loadDependenciesFromJson(JsonNode)` / `loadMetadataFromJson(JsonNode)` (existing statics, unchanged).
- Produces:
    - `final class JIPipeProjectIOs` (private ctor):
      `static JIPipeProjectReader readerFromPath(Path path) throws IOException`;
      `static JIPipeProjectWriter writerFromPath(Path path)`;
      `static JIPipeProjectPeek peek(Path path) throws IOException`.
    - `final class JIPipeProjectPeek`: `Set<JIPipeDependency> getDependencies()`, `JIPipeProjectMetadata getMetadata()`.

- [ ] **Step 1: Write the failing dispatch test**

`ProjectIOsDispatchTest` decision table (fixtures written to `@TempDir`; ZIP fixture = a few bytes with `PK\x03\x04`
magic prefix written to a file):

1. pretty-printed minimal project JSON in a `.jip` file → `readerFromPath` returns `JIPipeLegacyProjectReader`.
2. same JSON in a `.json` file → still `JIPipeLegacyProjectReader` (content wins over extension).
3. ZIP-magic file named `x.jip` → `readerFromPath` throws `UnsupportedOperationException` containing
   `"not yet supported"` (`.jipx` arrives in Phase 1).
4. `writerFromPath(path("a.jip"))` → `JIPipeLegacyProjectWriter`; `writerFromPath(path("a.jipx"))` →
   `UnsupportedOperationException` containing `"not yet supported"`.
5. `peek` on a hand-written temp `.jip` (with `metadata: {"name": "Roundtrip"}` and an empty `dependencies` array) →
   `getMetadata().getName().equals("Roundtrip")`, `getDependencies().isEmpty()`; `peek` on `/data/JIPipe/06072020a.jip`
   (a real corpus file with 7 dependencies — the module's test fixtures have zero) → `getDependencies().size()` is 7;
   `peek` on the ZIP-magic file → `UnsupportedOperationException`. Note: `/data/JIPipe` is the sanctioned corpus (Task
   1) — reading one file from it is fine, writing never is.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl jipipe-launcher test -Dtest=ProjectIOsDispatchTest`
Expected: compilation failure — `JIPipeProjectIOs` does not exist.

- [ ] **Step 3: Implement the dispatcher and peek**

`readerFromPath`: if `ArchiveUtils.isZipFile(path)` → throw
`UnsupportedOperationException("ZIP-based project formats are not yet supported (the .jipx format arrives with JIPipe 6.0 phase 1): " + path)`;
otherwise return `new JIPipeLegacyProjectReader()` (the reader itself surfaces parse errors). `writerFromPath`:
extension switch — `jip` → legacy writer; `jipx` → throw
`UnsupportedOperationException(".jipx is not yet supported: " + path)`; anything else → throw
`IllegalArgumentException("Cannot determine project writer for: " + path)`. `peek`: same ZIP guard, then `readValue` +
the two existing static loaders, wrapped in a `JIPipeProjectPeek` record-like class.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl jipipe-launcher test -Dtest=ProjectIOsDispatchTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/ jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/ProjectIOsDispatchTest.java
git commit -m "feat(io): JIPipeProjectIOs dispatcher and project peek, #753"
```

### Task 5: GUI peeks behind the dispatcher

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWindow.java:331-335` (file
  branch of `openProject`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWindow.java:432-435` (directory
  branch)

**Interfaces:**

- Consumes: `JIPipeProjectIOs.peek(Path)` (Task 4).
- Produces: nothing (behavior-identical rewiring; the inline load at lines 369-381 stays as-is in Phase 0 — dispatcher
  adoption of the load path itself is Phase 1).

- [ ] **Step 1: Rewire the two peek sites**

File branch: replace the `readValue` + `loadDependenciesFromJson` + `loadMetadataFromJson` block with
`JIPipeProjectPeek peek = JIPipeProjectIOs.peek(path);` then `dependencySet = peek.getDependencies();` /
`metadata = peek.getMetadata();` (drop the `metadata` local if the compiler shows it unused — its only live use was the
commented-out update-site block). Directory branch: replace `readValue` + `loadDependenciesFromJson` with
`peek(projectPath).getDependencies()`. No other changes — the missing-dependency dialog flow is untouched.

- [ ] **Step 2: Compile and regression-run**

Run: `mvn -pl jipipe-core compile -q && mvn -pl jipipe-launcher test`
Expected: compile clean; tests PASS. (No new UI tests — Swing behavior verified by eye in the app per project policy.)

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWindow.java
git commit -m "refactor(ui): route project-open peeks through JIPipeProjectIOs, #753"
```

### Task 6: Corpus verification gate (baseline parity)

The Phase 0 done-gate: the refactored code must produce **exactly** the baseline behavior.

**Files:**

- Create: `docs/superpowers/plans/2026-10-23-jipx-phase0-verification.md` (verification record)

**Interfaces:**

- Consumes: the harness (Task 1) and the baseline report `/data/JIPipe/.roundtrip/baseline.json`.

- [ ] **Step 1: Re-run the harness on the refactored code**

Run:
`mvn -pl jipipe-launcher test -Dtest=CorpusRoundTripFixpointTest -Djipipe.jip.roundtrip.corpus=/data/JIPipe -Djipipe.jip.roundtrip.report=/data/JIPipe/.roundtrip/after.json`
(same long-timeout note as Task 1).
Expected: PASS; report written.

- [ ] **Step 2: Compare against the baseline (exact parity)**

Run:
`python3 -c "import json,sys; a=json.load(open('/data/JIPipe/.roundtrip/baseline.json')); b=json.load(open('/data/JIPipe/.roundtrip/after.json')); sys.exit(0 if a==b else (print('DIFFERENCES:', [ (x['file'],x['outcome'],y['outcome']) for x,y in zip(a['entries'],b['entries']) if x!=y ]) or 1))"`
Expected: exit 0 (no output). Any difference is a Phase 0 regression in the extraction — fix before proceeding; do not
weaken the comparison.

- [ ] **Step 3: Write the verification record**

`2026-10-23-jipx-phase0-verification.md`: corpus size, outcome counts, notable `pvs1TopLevelChangedKeys` buckets
(expected: version fields and regenerated caches), fixpoint violations found and their triage (from Task 1 Step 4), and
the parity result. State explicitly: `.jip` read/write verified behavior-preserving; Phase 1 (`.jipx` reader/writer) may
proceed.

- [ ] **Step 4: Full module regression**

Run: `mvn -pl jipipe-core,jipipe-launcher test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-10-23-jipx-phase0-verification.md
git commit -m "docs: JIPX phase 0 corpus verification record, #753"
```
