# JIPX — New JIPipe project format (work item #753)

**Status:** Approved design · **Date:** 2026-10-23 · **Target milestone:** 6.0.0
**Issue:** https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/753
**Related:** #1147 (Interactive JIPipe — multiple graphs), #1332 (history system)

## 1. Motivation

The current `.jip` project format is a single JSON document. It has been
extended repeatedly (e.g. node/parameter documentation embedded as
`jipipe:doc:parameters`) and grows large over time. The single-file design
cannot carry:

- multiple unrelated graphs (needed by #1147: small custom pipelines within
  interactive systems),
- project history information (needed by #1332),
- data carried with the project (RO-Crate export covers publication, but is
  an export format, not a project format).

The new format `.jipx` is a ZIP-based container (in the spirit of Office
Open XML) that doubles as an RO-Crate conforming to the Workflow RO-Crate
profile. JIPipe auto-fills information the user did not provide and runs a
compliance check on save. The legacy `.jip` format remains readable for
backward compatibility; users are encouraged to save `.jipx` (Office-style
prompt on save, `.jipx` default, new projects always `.jipx`).

Implementation phasing (agreed): **Phase 0** extracts a dedicated
reader/writer for the *existing* `.jip` format and verifies it against the
local project corpus; **Phase 1** builds the `.jipx` reader/writer on top.
This ensures `.jip` remains readable for compatibility.

## 2. Current state (facts established during investigation)

- `JIPipeProject.toJson/fromJson` (jipipe-core,
  `api/project/JIPipeProject.java`) is the whole format. Top-level JSON
  fields: `jipipe:project-type` ("project"), `jipipe:project-format-version`
  (write-only, always 1, never read), `jipipe:project-jipipe-version`,
  `metadata`, `dependencies`, `runtime-partitions`, `run-sets`,
  `jipipe:doc:parameters`, `settings`, `external-environments`,
  `additional-metadata`, `graph`, `compartments.compartment-graph`.
- The real migration mechanism is per-app-version: `jipipe:project-jipipe-version`
  → `applyProjectUpgrade` fans out to `JIPipeProjectUpgradable` implementors,
  plus in-`fromJson` legacy fixes (compartment ID remap, ghost-node removal,
  compartment-output re-attachment).
- `jipipe:doc:parameters` and `external-environments` are write-only caches:
  regenerated on save, never read back.
- Work directory is derived from the project file's parent directory; paths
  in the JSON are normalized strings resolved against it. Nothing in the
  file is absolute-path-dependent by design.
- IO entry points today: `JIPipeProject.loadProject(Path)` (read) and
  `saveProject(Path/Writer/OutputStream)` (write); the GUI window does
  dependency/metadata *peek* reads before opening, plus save-time
  self-verification (write to `.part`, re-read, atomic move).
- A "ZIP with `project.jip` at root" convention already exists in four
  places (legacy archive ZIP, save-and-cache ZIP, project template ZIPs,
  RO-Crate export). Primitives exist: `JIPipeZIPReadDataStorage`,
  `JIPipeZIPWriteDataStorage`, `ArchiveUtils.isZipFile/decompressZipFile`.
- RO-Crate export exists today (`plugins/publish` → `CreateROCrateRun`,
  `.crate.zip`) using the vendored library `contrib/jipipe-ro-crate-java-2.1.0`
  (`RoCrateBuilder`, `Writers.newZipPathWriter()`, validator, unused
  `reader` package). The import path currently ignores crate metadata and
  just opens the inner `project.jip`.
- `jipx` appears nowhere in the codebase; the extension is free.
- Load sites that must keep working: GUI open, open-on-startup (file/URL),
  CLI `run` / `create-ro-crate`, 5 node-based loaders in plugins, template
  loader (`.jip` and `.zip`), backups (create/restore/prune/collect),
  output-folder convention (`project.jip` + `log.txt`), post-run re-save in
  `JIPipeGraphRun`.

## 3. Phase 0 — dedicated `.jip` reader/writer + corpus verification

New package `org.hkijena.jipipe.api.project.io` in jipipe-core.

### 3.1 IO interfaces and dispatcher

```java
public interface JIPipeProjectIO {
    String getId();                    // stable string literal, e.g. "org.hkijena.jipipe.core:jip", "…:jipx"
    List<String> getFileExtensions();  // e.g. ["jip"], ["jipx"]
    boolean supports(Path path);       // extension check + content sniff
}

public interface JIPipeProjectReader extends JIPipeProjectIO {
    JIPipeProject read(Path path, JIPipeValidationReportContext context,
                       JIPipeValidationReport report, JIPipeNotificationInbox notifications,
                       JIPipeProgressInfo progressInfo) throws IOException;
}

public interface JIPipeProjectWriter extends JIPipeProjectIO {
    void write(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo) throws IOException;
}
```

- **`JIPipeLegacyProjectReader`** — the exact logic of the current
  `JIPipeProject.loadProject(Path,…)`: `JsonUtils.getObjectMapper().readValue`
  → `fromJson` → `setWorkDirectory(parent)` → `validateUserPaths` →
  setting-gated `fixBrokenEnvironments` → `projectFile = path`.
- **`JIPipeLegacyProjectWriter`** — the exact logic of the current
  `saveProject`: authors merge (`addAuthorsFromGlobalList`) → pretty-print
  via the global mapper. Writer/OutputStream variants stay as overloads.
- **`JIPipeProjectIOs`** (static dispatcher, utils convention):
  - `readerFromPath(Path)` — **content first**: ZIP magic bytes
    (`ArchiveUtils.isZipFile`) → JIPX/crate reader in Phase 1; otherwise
    JSON → legacy reader. Extension is a hint, not the gate (a `.jip` with
    ZIP content is a mislabeled crate and is treated as such).
  - `writerFromPath(Path)` — extension-driven: `.jipx` → JIPX writer,
    `.jip` → legacy writer.
  - `readerFromURI(URI)` / `writerFromURI(URI)` — later: download to temp →
    `readerFromPath` (covers the GUI `importURL` flow); remote writers later.
- `JIPipeProject.loadProject/saveProject` become thin delegates to the
  legacy pair (all ~15 external call sites keep working unchanged; they
  migrate to the dispatcher gradually).
- GUI peek reads (dependency/metadata check before open) move behind
  `JIPipeProjectIOs.peek(Path)` helpers — parse only what is needed, no full
  load, one place that knows all formats.

Design intent: folder-based IO (`supports(dir)` with `project.jip` /
`jipx.json` inside) and remote IO are future `JIPipeProjectIO`
implementations that slot in without touching call sites.

### 3.2 Verification harness (fixpoint stability)

`fromJson` deliberately applies legacy conversions, so original → first
re-save can legitimately differ. The invariant is:

1. `load(P) → save → S1`, then `load(S1) → save → S2` — **S1 and S2 must be
   semantically identical** (canonical JSON tree comparison: Jackson
   `ObjectNode` with sorted keys; byte-level equality is not required).
2. `P → S1` diffs are bucketed: expected upgrades (known legacy fixes) vs.
   unexpected changes (data loss). Unexpected ⇒ extraction bug.

Form: a JUnit 5 test in jipipe-core gated on a system property
`-Djipipe.jip.roundtrip.corpus=<dir>` (assumeTrue; skipped when absent, CI
stays green). **Default corpus on this machine: `/data/JIPipe/` (367 real
projects)**; run locally over the full corpus. The same harness doubles as
the JIPX verification in Phase 1: `load(.jip) → write(.jipx) → read(.jipx)`
must equal the JIP fixpoint.

## 4. Phase 1 — JIPX container layout

```
project.jipx                              (ZIP; ro-crate-metadata.json at root ⇒ the ZIP is the RO-Crate)
├── ro-crate-metadata.json                RO-Crate metadata (auto-generated on save)
├── jipx.json                             JIPX manifest (mechanical: versions, part table, main graph)
├── README.md                             generated from project metadata (profile SHOULD)
├── project/                              split project parts — each is the JSON the monolith had as one top-level field
│   ├── metadata.json                     ← "metadata"
│   ├── dependencies.json                 ← "dependencies"
│   ├── runtime-partitions.json           ← "runtime-partitions"
│   ├── run-sets.json                     ← "run-sets"
│   ├── settings.json                     ← "settings" (unknown sheets preserved verbatim)
│   ├── additional-metadata.json          ← "additional-metadata" (incl. GUI tab state)
│   ├── graph.json                        ← "graph"  ← MAIN GRAPH
│   ├── compartment-graph.json            ← "compartments"."compartment-graph"
│   └── doc-parameters.json               ← "jipipe:doc:parameters" (write-only cache)
├── data/                                 reserved: carried data (#753 motivation; empty in v1)
├── graphs/                               reserved: additional graphs (#1147; empty in v1)
└── history/                              reserved: history snapshots (#1332; empty in v1)
```

Decisions:

- **Split parts** (not a single `project.jip` in the ZIP): each part is
  independently readable/patchable, addressing the JSON-growth complaint and
  enabling partial updates (history #1332 saves only a changed graph).
- **`jipx.json` manifest**: `format-version: 1`, `jipipe-version`,
  `main-graph: "project/graph.json"`, part table `{path, role, version}`.
  Per-part versions start at 1. Unknown parts are preserved verbatim on
  round-trip (forward compatibility for plugins and the reserved slots).
- **Manifest and crate metadata are separate**: `ro-crate-metadata.json` is
  semantic (auto-regenerated); `jipx.json` is structural (what parts exist,
  in which version). RO-Crate tooling ignores the manifest; JIPipe reads it
  first.
- `external-environments` is **dropped** from JIPX — write-only cache, fully
  derivable from node parameters, never read.
- **Load semantics unchanged**: the reader reassembles part files into the
  exact tree `fromJson` consumes today; the entire upgrade machinery
  (`JIPipeProjectUpgradable` fan-out + in-`fromJson` legacy fixes) works
  without modification. Version discipline: bump manifest `format-version`
  only when the *container* changes; per-project app-version upgrades
  continue through the existing mechanism.
- Reserved dirs exist in the ZIP only when they have content; they are slots
  named in the manifest until then.

## 5. JIPX save/load layer

### 5.1 Write path (`JIPipeXProjectWriter`)

1. Refresh `project.getProjectFile()` + authors merge (as legacy save does).
2. Stage into a fresh temp directory under JIPipe's global tmp
   (`PathUtils.createTempDirectory`): write `project/*.json` parts by
   delegating to per-part serialization extracted from the monolith
   `toJson` (each part's JSON output is identical to the monolith's
   corresponding top-level field — the extraction is a code refactor, not a
   format change); write
   `jipx.json`; generate `README.md`; build `ro-crate-metadata.json` with
   the contrib RO-Crate library (`RoCrateBuilder`).
3. **Auto-fill + validate on save**: fill required-but-missing crate fields
   (license from a **new** `JIPipeProjectIOApplicationSettings` default-license
   setting, `programmingLanguage`,
   `mainEntity` → `project/graph.json`, name/description from project
   metadata, keywords), run crate validation, collect violations.
4. Zip the staging dir to `<target>.part`, self-verify by re-reading it,
   atomic move onto the target (mirrors the existing `.jip` self-verify
   flow). Non-fixable validation violations surface as a non-blocking
   warning (list + "Save anyway").

### 5.2 Read path (`JIPipeXProjectReader`)

1. Extract to a temp dir (`ArchiveUtils.decompressZipFile` /
   `JIPipeZIPReadDataStorage`).
2. Read `jipx.json`, reassemble the part tree, feed the existing
   `fromJson` (upgrades + legacy fixes apply unchanged).
3. Assign workdir = ZIP's parent; run the reader-contract steps
   (`validateUserPaths`, setting-gated env fix, `projectFile = path`).
   Extraction-dir cleanup piggybacks on the existing temp-dir lifecycle
   (`JIPipeProject.close`).

### 5.3 GUI threading

Save runs in a Swing worker (existing background-runner pattern) with a
progress overlay during zip — no EDT hang. Non-GUI call sites (CLI run,
node-based loaders, archive runs, templates, backups) call the same
reader/writer synchronously.

## 6. RO-Crate conformance ("JIPipe Workflow RO-Crate")

v1 conforms to base RO-Crate 1.1-DRAFT + Workflow RO-Crate profile 1.0
(https://w3id.org/workflowhub/workflow-ro-crate/1.0):

- Root `./` Dataset: `name`, `description` (from project metadata),
  `license` (**auto-filled if missing** — the profile's only hard MUST),
  `keywords` (`["JIPipe"]` + metadata categories), `conformsTo` → base
  RO-Crate + workflow-ro-crate/1.0.
- `mainEntity` → `project/graph.json` as
  `["File","SoftwareSourceCode","ComputationalWorkflow"]` with
  `programmingLanguage` (JIPipe version contextual entity) and Bioschemas
  `conformsTo`.
- `README.md` at root, `about: ./`, `text/markdown`, generated from project
  metadata.
- `workflow.cwl` generation stays in the RO-Crate **publisher** (publish-time
  enrichment for execution crates, Docker settings, ORCID, `inputs/`
  staging) — the everyday save stays fast and does not carry CWL.
- Reserved dirs become `hasPart` Dataset entities when non-empty.
- Validation = contrib library validator + the profile checks above;
  failures surface in the save-time warning.
- **Prepared follow-up** (not in v1 code): the derived profile
  "JIPipe Workflow RO-Crate" gets the permalink
  `https://jipipe.org/ro-crate/jipipe-workflow/1.0` (jipipe.org redirects to
  jipipe.hki-jena.de; hosting to be set up). Publishing the profile page +
  Profile Crate is a separate work item.

## 7. Migration & compatibility matrix (6.0.0)

| Surface | Behavior |
|---|---|
| New project | Always `.jipx` (Save As filter order: jipx, jip) |
| Save existing `.jip` (Ctrl+S / menu) | Office-style prompt **on save**: "Save as `.jipx` (recommended) / keep `.jip` / cancel" — per-save choice; choosing `.jipx` writes `<same-dir>/<same-name>.jipx`, updates the project's tracked file to it, and **leaves the old `.jip` untouched** on disk (no deletion); `.jip` keeps round-tripping |
| Open | `readerFromPath` content sniff: `.jip` → legacy; `.jipx`/crate ZIP → JIPX; older projects keep opening |
| CLI `run`, node loaders, templates | Accept both; template ZIPs (`.jip` inside) keep working |
| Backups | New backups of `.jipx` projects are `.jipx` |
| Post-run re-save (`JIPipeGraphRun`), save-and-cache | Re-save in the project's current format |
| Legacy archive ZIP / `.crate.zip` publisher | Unchanged; "Open output folder" extended to find `jipx.json` next to `project.jip` |
| Conversion | Open `.jip` → Save As `.jipx`; no auto-conversion |

## 8. Testing strategy

- **Phase 0 harness**: corpus round-trip fixpoint test (system-property
  gated, §3.2) — the primary safety net for "jip still reads".
- **Unit tests (non-trivial contracts only)**: part reassembly equality
  (monolith JSON ⇄ split parts, canonical-tree comparison); manifest
  unknown-part preservation; ZIP self-verify failure path; reader sniff
  dispatch (`supports` decision table: jip / jipx / crate.zip / mislabeled);
  auto-fill of required crate fields; compliance-check fail-closed
  behavior (warning carries violations, save still possible).
- **No Swing-structure tests** (project policy); GUI save prompt verified by
  eye in the app.
- The RO-Crate publisher's own tests (contrib module) continue to cover the
  crate library.

## 9. Out of scope (v1)

- Multi-graph payloads in `graphs/` (#1147) — slot reserved only.
- History snapshots in `history/` (#1332) — slot reserved only.
- Carried data in `data/` — slot reserved only.
- Formal published profile page/Profile Crate at jipipe.org (follow-up work
  item; URI reserved here).
- Remote/URI writers; folder-format readers/writers (interfaces accommodate
  them later).
- Removing or deprecating `.jip` support.

## 10. Implementation phases (summary)

1. **Phase 0** — extract `JIPipeLegacyProjectReader/Writer`, add
   `JIPipeProjectIOs`, delegate `loadProject/saveProject`, move GUI peeks
   behind the dispatcher, build the corpus round-trip harness, run it over
   the local corpus until the fixpoint holds for every project.
2. **Phase 1** — JIPX container (`JIPipeXProjectReader/Writer`, manifest,
   part (de)serialization, staging + self-verify, Swing-worker save),
   RO-Crate generation + auto-fill + validation, save prompt + migration
   UX, extension-point adoption in call sites (open/open-on-load, CLI,
   templates, backups, post-run re-save).
