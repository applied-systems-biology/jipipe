# JIPX Phase 1 — Corpus Verification Record (Task 6, #753)

**Date:** 2026-10-23
**Branch:** `753-jipx-project-format`
**Scope of proof:** the Phase 1 JIPX container (`.jipx` parts slice/assemble,
manifest, workspace, reader, writer + RO-Crate build, IO dispatch) is
**behavior-preserving** across the JIPX round-trip — load a legacy `.jip`
project, save it to a legacy fixpoint model (M1), write M1 to a `.jipx`
container, read the `.jipx` back (M2), and prove M1 ≡ M2 on the full
monolith serialization, with a valid Workflow RO-Crate — across the 367-file
local corpus.

---

## 1. Method

**Corpus:** `/data/JIPipe` — **367** `.jip` files (recursive; the corpus is
read-only for this verification).

**Instrument:** `JipxRoundTripFixpointTest`
(`jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/`), a
corpus-gated JUnit harness structured as a direct inheritance of the Phase 0
gate `CorpusRoundTripFixpointTest` (gating, sorted discovery, per-project
try/catch-continue, stage-tracked outcomes, pollution guard, deterministic
report, canonical comparison helper duplicated by design). Gated by
`-Djipipe.jipx.roundtrip.corpus=<dir>` (skipped green in CI without it);
optional `-Djipipe.jipx.roundtrip.limit` (smoke runs) and
`-Djipipe.jipx.roundtrip.report` (default `<corpus>/.roundtrip/jipx.json`).

**Per project** (each in its own try/catch — one file's failure never aborts
the run):
1. Copy the corpus file into a fresh, index-named `@TempDir` work dir as
   `project.jip` (the corpus is never touched; the pollution guard asserts
   the top-level `*.jip` count is unchanged before/after — it passed).
2. **Legacy cycle** (the Phase 0 inheritance): `M0 = loadProject(copy)` →
   save `S1` → `M1 = loadProject(S1)` → save `S2` → `legacyFixpoint =
   canonical(S1 tree) == canonical(S2 tree)`.
3. **JIPX cycle** (from M1, the legacy-fixpoint model):
   `writeWithViolations(M1, project.jipx, SILENT, violations)` →
   `M2 = loadProject(project.jipx)` (the read dispatch routes ZIP → JIPX
   reader — the dispatch seam itself is proven; the reader is never
   instantiated directly) → `jipxModelEquivalent =
   canonical(monolithTree(M1)) == canonical(monolithTree(M2))`, where
   `monolithTree(X) = readValue(writeValueAsString(X))` is the full monolith
   serialization. The canonical comparison erases the known array-order
   nondeterminism of the current save path (the Phase 0 ruling).
4. **Crate check** (`jipxCrateValid`, boolean only — the crate content is
   deliberately not byte-compared, `datePublished` is non-deterministic by
   design, per the Task 4 review carry-forward): open the `.jipx` with
   `ZipFile`; the `ro-crate-metadata.json` entry exists and parses, its root
   `./` entity has `mainEntity` (== `project/graph.json`, as an RO-Crate
   `{"@id": ...}` reference) and a `license` reference; the `jipx.json`
   entry exists and parses.

**Outcomes:** `OK` (steps 2–4 all ran; the three booleans + `loadedNodesM1`
+ `jipxViolations` recorded) | `PARSE_FAILED` | `LOAD_FAILED` (legacy cycle —
JIPX steps skipped) | `SAVE_FAILED` (legacy cycle) | `JIPX_SAVE_FAILED` |
`JIPX_LOAD_FAILED`. `error` is a one-line diagnostic (`Class: message`,
whitespace-collapsed, 500-char cap) — known to flap for JDK NPE
helpful-message forms, never part of any comparison.

**Build pre-step:** `mvn -pl jipipe-launcher -am install -DskipTests -q` was
run before any launcher test run (the Task 5 lesson — stale SNAPSHOT jars in
`~/.m2` cause spurious failures).

---

## 2. Outcome counts

| | OK | PARSE_FAILED | LOAD_FAILED | SAVE_FAILED | JIPX_SAVE_FAILED | JIPX_LOAD_FAILED | total |
|---|---|---|---|---|---|---|---|
| **Phase 1 JIPX run** | **358** | 0 | 9 | 0 | 0 | 0 | **367** |

**JIPX equivalence results (the Phase 1 done-gate):**

| metric | value |
|---|---|
| `jipxModelEquivalent = true` | **358 / 358** legacy-OK (100%) |
| `jipxModelEquivalent = false` | **0** |
| `jipxCrateValid = true` | **358 / 358** (100%) |
| `jipxCrateValid = false` | **0** |
| `jipxViolations > 0` | **0** entries (total across all 367 = **0**) |
| OK files with `loadedNodesM1 = 0` (vacuity guard) | **0** (range 1–492) |

**The JIPX round-trip is behavior-preserving across the entire loadable
corpus: every file that loads in the legacy cycle also round-trips through
the JIPX container with an identical monolith model, a valid Workflow
RO-Crate, and zero compliance violations.**

---

## 3. Legacy-cycle inheritance (Phase 0 parity)

`legacyFixpoint` (canonical S1-vs-S2 equality, the Phase 0 fixpoint
property):

- **284 / 356** of the files that are OK in *both* the Phase 0 baseline and
  this run are `legacyFixpoint = true` — a **byte-identical 0-mismatch
  parity** with the Phase 0 `s1vs2SemanticallyEqual` split (Phase 0: 284
  true / 72 false of its 356 OK).
- The 74 `legacyFixpoint = false` files in this run are exactly the Phase 0
  72 churn files **plus the 2 newly-loadable `IOInterface` files** (§4),
  both of which fall in the documented pre-existing churn buckets (env
  `artifact-query/query-v2` `null`→`"null"`, slot `custom-name`
  `null`→`""`, auto-doc drops `important:true`) — the Phase 0-deferred
  pre-existing `.jip` bugs, **unchanged by Phase 1**.

**The Phase 1 JIPX container introduces zero legacy fixpoint regression.**

> Note: `loadedNodesM1` (the M1/S1-reload model node count) is not a
> Phase 0 parity field — Phase 0 measured `loadedNodesS1` on the *original*
> P load, a different model. Node counts are therefore not compared
> cross-phase; only the fixpoint property and the JIPX equivalence are.

---

## 4. Triage — every non-OK and every deviation from the Phase 0 baseline

**The 9 `LOAD_FAILED` files** (all legacy-cycle, JIPX steps skipped):

| file | error (diagnostic) | bucket |
|---|---|---|
| `06072020a.jip` | NPE `jsonNode.has` null | missing sub-field |
| `14072020.json.jip` | NPE `jsonNode.has` null | missing sub-field |
| `AIAgentTests/06-CreatedProjects/EmptyProject/empty-1-compartment.jip` | NPE `jsonNode.get().asText()` null | missing sub-field |
| `AIAgentTests/06-CreatedProjects/EmptyProject/empty-3-compartments.jip` | NPE `jsonNode.get().asText()` null | missing sub-field |
| `Empty (1 compartment).jip` | NPE `jsonNode.get().asText()` null | missing sub-field |
| `Empty (3 compartments).jip` | NPE `jsonNode.get().asText()` null | missing sub-field |
| `ometest.jip` | NPE `jsonNode.get().asText()` null | missing sub-field |
| `testclij.jip` | NPE `jsonNode.has` null | missing sub-field |
| `testparameters.jip` | NPE `jsonNode.has` null | missing sub-field |

All 9 are **old-format `.jip` files that cannot be loaded by the current
(unrefactored) legacy reader** — the same failure class Phase 0 recorded.
These are pre-existing corpus defects, not Phase 1 bugs; the JIPX steps are
correctly skipped for them (no JIPX container can be written from a model
that never loaded).

**The 2-file deviation from the Phase 0 baseline — classified ENVIRONMENTAL,
not a Phase 1 bug:**

Phase 0's baseline had **11** `LOAD_FAILED`; this run has **9**. The 2
additional Phase 0 failures were
`ConfrontationAssay_Mohamed_Jaime_v17_forMohamedData_v1.jip` (two identical
copies, in `AIAgentTests/03-ExampleWorkflows/` and `Example workflows/`),
which Phase 0 recorded as `LOAD_FAILED` with
`IndexOutOfBoundsException: No node infos registered for class
org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm`. In this
run both load **OK** (358 and 364 nodes) and their JIPX round-trips are
**clean** (`jipxModelEquivalent=true`, `jipxCrateValid=true`,
`jipxViolations=0`).

**Evidence the flip is environmental, not a Phase 1 regression:**
1. **No Phase 1 commit touched the failing code.** `git diff` across the 6
   Phase 1 commits (`39578b7176..bbe7d7a5ed`) is empty for the node
   registry, `IOInterfaceAlgorithm`, `JIPipe.createNode`, `JIPipeProject`
   legacy load, `JIPipeGraph.nodesFromJson`, and the `UtilitiesPlugin`
   registration — the flip lives in the shared **legacy** load path (M0),
   which Phase 1 did not modify.
2. **The unchanged Phase 0 harness reproduces the flip.** Running
   `CorpusRoundTripFixpointTest` (verified **byte-identical** to the
   Phase 0 baseline commit `c7f5`, `git diff` empty) on a mini-corpus of
   just those 2 files, under the current Phase 1 code, yields `OK` / `OK` —
   the same flip, through the *identical* legacy load path.
3. **The Phase 0 baseline was captured in a contaminated environment.**
   The Phase 0 record (§5.1) documents a **live JIPipe GUI** (PIDs
   2016822/2019071) that "shared the `/home/ruman/.local/share/JIPipe`
   profile and rewrote the profile's node-db/settings state *during* the
   after-run," and that the baseline's dependency *membership* "can be
   warmup-sensitive in edge cases." That GUI has since been killed; the
   registry now resolves `io-interface` (registered by
   `UtilitiesPlugin.registerNodeType("io-interface", IOInterfaceAlgorithm.class, …)`)
   deterministically, so the 2 files load. The flip resolves *toward the
   files actually loading* — the opposite direction of a Phase 1 regression.
4. **The JIPX steps for both files are clean** — if the JIPX writer/reader
   had a bug surfacing on `io-interface` nodes (5 per file),
   `jipxModelEquivalent` / `jipxViolations` would be non-trivial; they are
   all true / 0.

**Verdict:** the 2-file `LOAD_FAILED`→`OK` flip is registry-state
environmental drift (Phase 0 baseline captured under a live-GUI profile
rewrite; now quiet). It is **not** a Phase 1 JIPX bug, and no `.jip`
behavior was changed to force it. Recorded as a concern (§6), not fixed.

**No other deviations.** Zero `jipxModelEquivalent=false`, zero
`jipxCrateValid=false`, zero `jipxViolations>0`, zero `PARSE_FAILED`,
zero `SAVE_FAILED`, zero `JIPX_SAVE_FAILED`, zero `JIPX_LOAD_FAILED`.

---

## 5. Fixes made during triage

**None to production code.** The only code change in this task is the
harness itself (`JipxRoundTripFixpointTest`), plus one **harness-internal**
correctness fix made *before* the full run: the crate check originally read
`mainEntity`/`license` as plain strings, but the RO-Crate library writes
them as `{"@id": …}` id references (verified against a live crate dump), so a
`resolveId` helper was added to resolve the reference form. This is a test-
only fix — it does not touch JIPX production behavior or the frozen `.jip`
path.

---

## 6. Environment note

- **JIPipe GUI running during this run:** **NO.** `ps aux | grep -i jipipe
  | grep -v grep` showed only the `konsole` terminals, the `mvn` build, and
  its `java` child — no JIPipe desktop/`JIPipeDesktopMain` process. The
  contaminating GUI from the Phase 0 window is gone, which is consistent
  with the 2-file flip resolving toward "loads OK" (§4).
- **JDK:** OpenJDK **25.0.4.1** (project targets Java 21 language level; the
  25 runtime is the machine default — same as the Phase 0 runs).
- **SNAPSHOT pre-step:** `mvn -pl jipipe-launcher -am install -DskipTests -q`
  completed (exit 0) before the smoke and full runs — the Task 5
  environmental-failure lesson was applied.
- **Full-run duration:** 21:53:51 → 21:59:37 (~**5.5 min**), 367 files.
- **Report location:** `/data/JIPipe/.roundtrip/jipx.json` (the only write
  into the corpus area; pollution guard passed).

---

## 7. Closing statement

**The JIPX Phase 1 container is verified behavior-preserving across the full
corpus.** All 367 files were round-tripped: **358** loaded and every one of
them satisfies `jipxModelEquivalent = true` (M1 ≡ M2 on the full canonical
monolith — the JIPX write→read is a faithful identity of the legacy-fixpoint
model), `jipxCrateValid = true` (valid Workflow RO-Crate with `mainEntity`
→ `project/graph.json` and a `license`), and `jipxViolations = 0`. The
**9** `LOAD_FAILED` files are pre-existing old-format `.jip` defects in the
*legacy* load path (JIPX correctly skipped); the **2-file** `LOAD_FAILED`
delta from the Phase 0 baseline is attributed to environmental registry-state
drift (the Phase 0 baseline was captured under a live-GUI profile rewrite,
now absent) — proven non-regressive by (a) no Phase 1 commit touching the
legacy load / node registry, (b) the byte-identical Phase 0 harness
reproducing the flip, and (c) the flip's clean JIPX round-trips.

**Open items (not blocking, not Phase 1 bugs):**
- The 9 legacy `LOAD_FAILED` files and the 74 `legacyFixpoint=false` churn
  files are Phase 0-deferred pre-existing `.jip` defects (HashSet ordering,
  `null`→`"null"`, custom-name, `important` drop) — tracked as separate work
  items, explicitly out of Phase 1 scope.
- To formally close the 2-file baseline delta, the Phase 0 baseline could be
  re-captured in the now-quiet environment (no live GUI) — expected to yield
  9 `LOAD_FAILED` matching this run.

**The `.jipx` format is ready to be gated as Phase 1 complete.**
