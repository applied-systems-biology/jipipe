# JIPX Phase 1 — `.jipx` Container Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `.jipx` project format (ZIP container that doubles as a Workflow RO-Crate), building on the verified Phase 0 IO foundation: split JSON parts, manifest, always-on project workspace, only-compress saves, RO-Crate auto-fill + save-time compliance check, Office-style save prompt, and adoption across all open/save/load call sites.

**Architecture:** Phase 0 delivered `JIPipeProjectIO` ← `JIPipeProjectReader`/`JIPipeProjectWriter` + `JIPipeLegacyProject*` + the `JIPipeProjectIOs` static dispatcher (whose `.jipx`/ZIP paths currently throw `UnsupportedOperationException` — those stubs are the exact seams). Phase 1 adds: (1) a `JIPipeProjectParts` (de)serializer that slices/reassembles the monolith top-level JSON into part files (slice = exact monolith field, so part output is byte-for-byte the existing serialization); (2) `JIPipeXManifest` (`jipx.json`) + `JIPipeProjectWorkspace` (a directory under the project's existing locked temp base; created for BOTH formats on open); (3) `JIPipeXProjectReader`/`JIPipeXProjectWriter` implementing the Phase 0 interfaces; (4) `JIPipeProjectIOApplicationSettings` (default license); (5) dispatcher seam activation + GUI migration UX (save prompt, new-project default, filters, open dispatch) + call-site adoption (JIPipeGraphRun re-save, backups follow format via the `saveProject` dispatch); (6) JIPX round-trip verification (corpus-gated harness extension + unit contracts).

**Tech Stack:** Java 21, Maven, Jackson (`JsonUtils.getObjectMapper()`), zip4j (transitively via `contrib/jipipe-ro-crate-java-2.1.0`), the contrib RO-Crate library (`org.hkijena.jipipe.contrib.ro_crate`), JUnit 5 (jipipe-launcher tests).

**Spec:** `docs/superpowers/specs/2026-10-23-jipx-project-format-design.md` (§4 container layout, §5 save/load layer, §6 RO-Crate conformance, §7 migration matrix, §8 testing). Phase 0 plan: `docs/superpowers/plans/2026-10-23-jipx-phase0-jip-io-foundation.md` (foundation already merged on this branch).

## Global Constraints

- Java 21, Maven. All Phase 1 production code in `jipipe-core`; new packages `org.hkijena.jipipe.api.project.jipx` (parts, manifest, workspace, reader, writer, crate) and additions to the existing `org.hkijena.jipipe.api.project.io` (dispatcher seams). Tests in `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/`.
- Build note (Phase 0 lesson): after jipipe-core changes, `mvn -pl jipipe-core install -DskipTests -q` before launcher tests. Revert `utils/fresh-node-slots.json` / `utils/taxonomy-research/registry.json` test side effects before committing.
- New production file: standard MIT copyright header (copy from `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java:1-12`).
- Format id stable string literal: `"org.hkijena.jipipe.core:jipx"`. Registration IDs never code-derived.
- The `.jip` format and its behavior are FROZEN (Phase 0 gate passed): no changes to legacy reader/writer semantics, no `jip` file-format changes.
- Part serialization = exact slice of the monolith `toJson` output (see Task 1) — do not re-implement per-part serializers.
- `external-environments` is DROPPED from JIPX (write-only cache, never read — spec §4). `jipipe:doc:parameters` IS carried (write-only but cheap + the spec's part list includes it).
- Workspace: `<project temporary base directory>/jipx-workspace` (the existing `JIPipeProject.getTemporaryBaseDirectory()` + `FileLocker` + `close()` cleanup lifecycle). `workspace.json` state file at workspace root (origin path, origin format, last-save timestamp ISO-8601, dirty flag; clean close removes the workspace via the existing temp-dir cleanup).
- RO-Crate: v1 conforms to base RO-Crate 1.1 + `https://w3id.org/workflowhub/workflow-ro-crate/1.0` (spec §6). Auto-fill on save: license from `JIPipeProjectIOApplicationSettings` default (empty → MIT per `CreateROCrateRun.toSpdxLicenseUri` convention: `https://spdx.org/licenses/MIT`), `programmingLanguage` = JIPipe contextual entity, `mainEntity` → `project/graph.json`, name/description from project metadata, keywords `["JIPipe"]` + metadata categories. Validation failures surface as non-blocking warnings ("Save anyway" stays default).
- `workflow.cwl` / Docker / ORCID enrichment stays in the RO-Crate PUBLISHER (`CreateROCrateRun`) — everyday saves carry no CWL (spec §6).
- No Swing-structure tests (project policy): the save prompt is verified by eye; its non-UI decision logic (which format a save targets) IS unit-tested.
- Corpus `/data/JIPipe` (367 `.jip` files) read-only except `/data/JIPipe/.roundtrip/`. Gated harnesses: `-Djipipe.jip.roundtrip.corpus=<dir>` (Phase 0), `-Djipipe.jipx.roundtrip.corpus=<dir>` (Phase 1).
- JIPipe GUI may be running concurrently (shared profile!) — long corpus runs can drift registry state; verification records must note environment state, and the controller runs control experiments when parity wobbles (Phase 0 precedent).
- Branch: `753-jipx-project-format` (already on it, in main checkout `/fastdata/src/jipipe-3`). Commit per task. No MR until the user says the feature is finished.

## Review Focus

1. **Part-slice fidelity** — a part file must equal the monolith's corresponding top-level field for every project; reassembled parts must feed `fromJson` with identical semantics (upgrades, legacy fixes). *Pinned by:* Task 1 unit test (slice⇄monolith equality on a fixture with all fields + on the corpus via Task 6's gated harness) and Task 3's reader contract (loaded model ≡ legacy-loaded model).
2. **ZIP content safety** — malicious/corrupt `.jipx`: zip-slip, missing manifest, manifest listing parts that don't exist, part files with malformed JSON, a `.jip` file that is actually a ZIP (mislabeled crate). *Pinned by:* Task 3 tests (missing manifest → clear exception; unknown manifest parts preserved; corrupt part → IOException surfaced, project not half-opened) + the existing `ArchiveUtils.decompressZipFile` zip-slip guard.
3. **Save atomicity** — a crashed/failed save must not leave the target `.jipx` half-written; the workspace must never be the artifact. *Pinned by:* Task 4 tests (self-verify failure → target file absent/unchanged, `.part` cleaned up; atomic move).
4. **Format dispatch round-trips** — every open/save surface must respect the file's actual format: `.jip` stays `.jip` (frozen), `.jipx` stays `.jipx`, `saveProject` dispatches on the TARGET extension, backups/GraphRun follow the project's current format. *Pinned by:* Task 4 writer-dispatch test + Task 5 call-site tests (GraphRun re-save name choice; backup target suffix) + Task 6 harness entry `jipxModelEquivalent` (a `.jip` whose workspace materialization or JIPX write silently degrades shows up as false with the diff bucket).
5. **RO-Crate auto-fill + compliance** — a project with empty license/metadata must still produce a conformant crate on save (license MUST is auto-filled); non-conformant cases produce a non-blocking warning, never a failed save; `RoCrateBuilder.build()` validation runs. *Pinned by:* Task 4 tests (auto-fill on bare fixture; validation violation collection shape).

---

### Task 1: `JIPipeProjectParts` — part (de)serialization

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectParts.java`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectPartsTest.java`

**Interfaces:**
- Consumes: `JIPipeProject` (monolith `toJson` via `JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(project)` → parse tree; `fromJson` for reassembly), the Phase 0 reader/writer (fixture loads).
- Produces:
  - `public static List<String> PART_KEYS` — the 9 part keys in write order: `metadata`, `dependencies`, `runtime-partitions`, `run-sets`, `settings`, `additional-metadata`, `graph`, `compartments` (value = `{"compartment-graph": <graph>}`), `jipipe:doc:parameters`.
  - `public static Map<String, JsonNode> sliceParts(JIPipeProject project)` — serialize the whole project to its monolith tree (via the global mapper, exactly as the legacy writer does), return a map key → the corresponding top-level node: `metadata`, `dependencies`, `runtime-partitions`, `run-sets`, `settings`, `additional-metadata`, `graph`, `compartments` (the object containing `compartment-graph`), `jipipe:doc:parameters` (may be missing for bare projects — absent keys are simply absent from the map). **Never** slices `external-environments`, `jipipe:project-type`, `jipipe:project-format-version`, `jipipe:project-jipipe-version` (dropped/derived).
  - `public static JsonNode assembleParts(Map<String, JsonNode> parts, String jipipeVersion)` — build the exact tree `JIPipeProject.fromJson` consumes: `jipipe:project-type` = `"project"`, `jipipe:project-format-version` = `1`, `jipipe:project-jipipe-version` = the passed version, plus every part key mapped to `parts` values (missing part keys omitted, as monoliths legitimately omit them). No other fields.

- [ ] **Step 1: Write the failing test**

`JIPipeProjectPartsTest` (`@BeforeAll JIPipe.ensureInstance()`), fixture = `/projects/5.3.0-taxonomy-fixtures/tables.jipipe` (1 node `table-add-columns-generate`):
1. `load = JIPipeProject.loadProject(fixture, ctx, report, new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT)`; `parts = sliceParts(load)`; `monolith = JsonUtils.getObjectMapper().readValue(JsonUtils.getObjectMapper().writeValueAsString(load), JsonNode.class)`.
2. Assert: for every key in `PART_KEYS` present in `monolith`, `parts.get(key).equals(monolith.get(key))` (exact monolith-field equality — the fidelity contract); `parts` has no key `external-environments` / `jipipe:project-type` / `jipipe:project-jipipe-version`.
3. Round-trip: `tree = assembleParts(parts, JIPipe.getJIPipeVersion())`; `p2 = new JIPipeProject(); p2.fromJson(tree, ctx, report2, inbox, SILENT)`; assert uuid→node-info-id mapping of `p2.getGraph()` equals the one of `load` (non-vacuity: `tables.jipipe` has 1 node + the mapping pattern from `FixtureProjectsLoadTest`).

- [ ] **Step 2: Run test, verify it fails** (compile failure — class missing). Run: `mvn -pl jipipe-launcher test -Dtest=JIPipeProjectPartsTest`
- [ ] **Step 3: Implement `JIPipeProjectParts`** per the Interfaces block. Javadoc: the slice-is-the-monolith-field fidelity contract and the dropped-fields decision (spec §4).
- [ ] **Step 4: Run test, verify it passes.**
- [ ] **Step 5: Commit** `feat(jipx): project parts slice/assemble, #753`

### Task 2: `JIPipeXManifest` + `JIPipeProjectWorkspace`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXManifest.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectWorkspace.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeLocalProjectWorkspace.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java` (add `workspace` field + `getWorkspace()` lazy accessor; remove workspace in `close()`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeLegacyProjectReader.java` (materialize workspace on legacy open)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeXManifestTest.java`

**Interfaces:**
- Consumes: `JIPipeProjectParts` (Task 1), `JIPipeProject.getTemporaryBaseDirectory()` (existing, locked lifecycle), `ArchiveUtils` (later tasks), Jackson.
- Produces:
  - `JIPipeXManifest` — JSON-mapped (Jackson, `@JsonProperty`), fields: `format-version` (int, = `1` constant `FORMAT_VERSION`), `jipipe-version` (String), `main-graph` (String, = `"project/graph.json"` constant), `parts` (Map<String,String>: zip entry path → part key, e.g. `"project/metadata.json" → "metadata"`), `unknown` (Map<String,String>: other zip entry paths preserved verbatim on round-trip — reserved slots `data/`, `graphs/`, `history/` appear here when non-empty). `static JIPipeXManifest fromJsonNode(JsonNode)` / `JsonNode toJsonNode()`; `static JIPipeXManifest defaultManifest(String jipipeVersion)` (parts table = the 9 default part paths `project/<key>.json`, key = part key with `:` replaced by `-` in the FILE name — e.g. `jipipe:doc:parameters` → `project/jipipe-doc-parameters.json`; the manifest part VALUE keeps the real key).
  - `JIPipeProjectWorkspace` (INTERFACE, spec §5.4 — remote workspaces later implement the same seam) + `JIPipeLocalProjectWorkspace` (v1 implementation, directory-based) — ctor `JIPipeLocalProjectWorkspace(Path baseDirectory, JIPipeProject project)`; interface methods: `Path getBaseDirectory()`; `void materializeFromModel(JIPipeProgressInfo)` (writes each part file via `JIPipeProjectParts.sliceParts` + `JIPipeXManifest.defaultManifest` + `workspace.json`; idempotent: re-materializing updates parts in place, never deletes unknown/payload entries); `void extractFromZip(Path zipFile, JIPipeProgressInfo)` (unzip via `ArchiveUtils.decompressZipFile` into the base dir); `Map<String, JsonNode> readParts()` (reads manifest + every part entry as a JsonNode; unknown entries pass through); `void markSaved(Path originPath)`; `void markDirty()`; `JIPipeXManifest getManifest()` (lazy read of jipx.json). `workspace.json` state: `origin-path`, `origin-format` `"jip"|"jipx"`, `last-save` ISO-8601, `dirty` boolean (the §5.5 recovery hooks).
  - `JIPipeProject.getWorkspace()` — returns the lazy workspace (`getTemporaryBaseDirectory().resolve("jipx-workspace")`), null when the project is disposed or no temp base exists (never force-creates on getters).

- [ ] **Step 1: Write the failing manifest test**

`JIPipeXManifestTest`: default manifest round-trips through `toJsonNode`/`fromJsonNode` (9 parts, format-version 1, main-graph constant); unknown-part preservation: add `"data/foo.tiff" → "data/foo.tiff"` to `unknown`, round-trip, still present; part FILE naming: `jipipe:doc:parameters` maps to file `project/jipipe-doc-parameters.json` but value stays `jipipe:doc:parameters`.

- [ ] **Step 2: Run, verify it fails** (compile failure).
- [ ] **Step 3: Implement manifest + workspace + the `JIPipeProject` hook.** `getWorkspace()`: create the directory lazily on first call (mkdirs), construct the workspace object once. `close(JIPipeProgressInfo)`: dispose the workspace reference (the existing temp-dir deletion already removes the directory — no extra delete code; only null the field). `JIPipeLegacyProjectReader.read`: after the existing load sequence (workDirectory set), call `project.getWorkspace().materializeFromModel(progressInfo)` wrapped in try/catch that logs to progressInfo (workspace materialization must NEVER fail a legacy load — it's derived).
- [ ] **Step 4: Run the manifest test (pass) + `mvn -pl jipipe-launcher test` (regression, incl. FixtureProjectsLoadTest — legacy loads now materialize workspaces).**
- [ ] **Step 5: Commit** `feat(jipx): manifest + project workspace, #753`

### Task 3: `JIPipeXProjectReader` + dispatcher activation (reads)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectReader.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectIOs.java` (`readerFromPath` ZIP branch + `peek` ZIP branch → JIPX)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java` (`loadProject` overloads → dispatch through `JIPipeProjectIOs.readerFromPath`)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectReaderTest.java`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/io/ProjectIOsDispatchTest.java` (amend the two ZIP cases from "not yet supported" to JIPX behavior — see Step 1)

**Interfaces:**
- Consumes: `JIPipeProjectWorkspace` (Task 2), `JIPipeProjectParts` (Task 1), `ArchiveUtils.isZipFile/decompressZipFile`.
- Produces:
  - `JIPipeXProjectReader implements JIPipeProjectReader` — id `"org.hkijena.jipipe.core:jipx"`, extensions `List.of("jipx")`, `supports(Path)` = regular file, `.jipx` extension (case-insensitive), AND `ArchiveUtils.isZipFile`. `read(...)`: (a) `new JIPipeProject()` + `setWorkDirectory(path.getParent())` (BEFORE the workspace — the workspace needs the temp base; workdir is just a field, `fromJson` does not require it — legacy reader's order is preserved for legacy, JIPX needs this order, document it); (b) `workspace.extractFromZip(path, progress)`; (c) `manifest = read manifest; parts = workspace.readParts()`; **missing `jipx.json` → throw `JIPipeValidationRuntimeException`** "not a JIPipe 6 project (missing jipx.json)"; **a part listed in the manifest but absent → throw** (corrupt container); (d) `project.fromJson(JIPipeProjectParts.assembleParts(parts, manifest.getJipipeVersion()), ctx, report, notifications, progress)` (the full upgrade machinery applies); (e) `validateUserPaths`, setting-gated `fixBrokenEnvironments`, `setProjectFile(path)` — same reader contract as legacy.
  - `JIPipeProjectIOs.readerFromPath`: `isZipFile(path)` → `new JIPipeXProjectReader()` (replaces the UOE); the reader's `read()` itself rejects non-`.jipx` ZIPs with a clear message (crate.zip without `jipx.json` → the manifest-missing exception tells the user it's not a JIPX). `peek`: ZIP → parse from zip entries: read `jipx.json` + `project/metadata.json` (via `java.util.zip.ZipFile` streaming, no extraction) → `JIPipeProjectPeek` (dependencies from `project/dependencies.json`, metadata from `project/metadata.json`); non-`.jipx`-looking ZIP peek → UnsupportedOperationException (GUI importROCrate path unchanged).

- [ ] **Step 1: Write the failing tests**

(a) Amend `ProjectIOsDispatchTest`: the ZIP-magic case (3) now expects `instanceof JIPipeXProjectReader` from `readerFromPath`; `peek` on a ZIP-magic file WITHOUT jipx.json → UnsupportedOperationException; add: `writerFromPath` `.jipx` → UnsupportedOperationException (writer lands in Task 4 — assert the current stub behavior so the test documents the seam, flip in Task 4).
(b) `JIPipeXProjectReaderTest`: build a throwaway `.jipx` with `JIPipeProjectParts` + `JIPipeXManifest` + zip (a private `buildJipx(Path, JIPipeProject)` helper using zip4j `ZipFile.createZipFile` writing the part files under `project/`, `jipx.json` at root — Task 4's writer reuses nothing from this helper; it's test-local): load fixture `basic-images.jipipe` → buildJipx → `new JIPipeXProjectReader().read(...)` → assert projectFile, workDirectory, node count 1, node id `ij1-blur-gaussian2d` (non-vacuity); model equivalence: uuid→info-id mapping equals the legacy load of the same fixture; **missing manifest**: zip with only `project/graph.json` → exception message contains "jipx.json"; **corrupt part**: manifest lists `project/metadata.json` but the zip omits it → exception.

- [ ] **Step 2: Run, verify failures** (dispatch: instanceof fails; reader: class missing).
- [ ] **Step 3: Implement** per the Interfaces block.
- [ ] **Step 4: Run both tests (pass) + full `mvn -pl jipipe-launcher test`** (regression: `loadProject` now dispatches — every existing load site goes through the reader; the 11 corpus LOAD_FAILED files must still behave identically, i.e. the legacy path is untouched for non-ZIP).
- [ ] **Step 5: Commit** `feat(jipx): project reader + dispatcher read activation, #753`

### Task 4: `JIPipeXProjectWriter` + RO-Crate build + `JIPipeProjectIOApplicationSettings` + dispatcher activation (writes)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectWriter.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrate.java` (the RO-Crate entity builder — separate file, one responsibility)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeProjectIOApplicationSettings.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/StandardSettingsPlugin.java` (register the settings sheet)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/io/JIPipeProjectIOs.java` (`writerFromPath` `.jipx` → `JIPipeXProjectWriter`, replacing the UOE)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java` (`saveProject(Path, boolean)` dispatches via `writerFromPath` — the single seam through which backups/GraphRun/archive runs pick up format awareness)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/PathUtils.java` (add `EXTENSION_FILTER_JIPX`)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectWriterTest.java`

**Interfaces:**
- Consumes: Phase 0 writer interface (3 methods — Path implemented, Writer/OutputStream throw `UnsupportedOperationException(".jipx is a ZIP container; stream writes are not supported")`), Task 1 parts, Task 2 workspace/manifest, `JIPipeProjectIOs.writerFromPath` (Task 3 amended the reader side), the RO-Crate library (`RoCrate.RoCrateBuilder`, `WorkflowEntity`, `FileEntity`, `PersonEntity`, `OrganizationEntity`, `ContextualEntity`, `JsonDescriptor`, `CrateVersion`), `net.lingala.zip4j` (unwrapped zip: `ZipFile.createZipFile(target, password, ZipParameters)` — entry order: `ro-crate-metadata.json` first (Workflow RO-Crate root convention), then `jipx.json`, then `project/*`, then any payload entries), `ArchiveUtils` (self-verify via `ZipFile`/`decompressZipFile`), `CreateROCrateRun` (reference for entity-building idioms — do NOT modify it).
- Produces:
  - `JIPipeXProjectWriter implements JIPipeProjectWriter` — id `"org.hkijena.jipipe.core:jipx"`, extensions `List.of("jipx")`, `supports` = `.jipx` extension (case-insensitive). `write(project, target, progress)`:
    1. Authors merge (same gate as legacy: `project.getMetadata().isAutoAddAuthors()` → `addAuthorsFromGlobalList()`).
    2. `workspace = project.getWorkspace(); workspace.materializeFromModel(progress)` (updates part files in place — payload/unknown entries survive).
    3. `JIPipeProjectCrate.buildCrate(project, workspace, violations)` → write `ro-crate-metadata.json` (`crate.getJsonMetadata()`) + generated `README.md` (project metadata name/description as markdown; `about: ./`) into the workspace.
    4. Write `jipx.json` (manifest with the parts table + current unknown entries, format-version 1, jipipe-version, main-graph).
    5. Zip workspace → `target.resolveSibling(target fileName + ".part")` with zip4j, NO wrapper root.
    6. **Self-verify**: open the `.part` (zip4j `ZipFile.isZipFile` + read `jipx.json`, parse every part entry as JSON, parse `ro-crate-metadata.json` as JSON); on failure → delete `.part`, throw `JIPipeValidationRuntimeException` (with the first violation).
    7. Atomic replace: delete existing target (if any) + move `.part` → target; `workspace.markSaved(project.getProjectFile() or target)`.
  - `JIPipeProjectCrate` — `static RoCrate buildCrate(JIPipeProject project, JIPipeProjectWorkspace workspace, List<String> violationsOut)` following `CreateROCrateRun` idioms: `RoCrateBuilder(name, description, datePublished, toSpdxLicenseUri(metadata.getLicense()))` (auto-fill: empty license → `https://spdx.org/licenses/MIT`, the settings default via `JIPipeProjectIOApplicationSettings`); root `keywords` = `["JIPipe"]` + metadata categories; `mainEntity` = `WorkflowEntity` id `project/graph.json` types File/SoftwareSourceCode/ComputationalWorkflow + `programmingLanguage` = contextual entity `https://jipipe.org/jipipe` (type `ComputerLanguage`, name "JIPipe", version = `JIPipe.getJIPipeVersion()`); `README.md` FileEntity (`text/markdown`, `about ./`); Person/Organization entities from metadata authors+affiliations (mirror `CreateROCrateRun.addROCrateAuthors/Organizations`); conformsTo descriptor: `JsonDescriptor.Builder` (default RO-Crate 1.1) + the workflow-ro-crate/1.0 contextual descriptor (mirror `createROCrateBuilder` L424); `builder.build()` (validates — collect thrown/printed violations into `violationsOut`: the library prints to System.err and returns boolean-less; wrap `build()` and also run the spec's profile checklist (license present, mainEntity present, README present, programmingLanguage present) ourselves into `violationsOut` — the checklist IS the "compliance check" of the spec; auto-fill happens BEFORE it so a well-formed project yields zero violations).
  - `JIPipeProjectIOApplicationSettings` — id `"org.hkijena.jipipe:project-io"`, extends `JIPipeDefaultApplicationsSettingsSheet`, `@JIPipeParameter("default-ro-crate-license")` `String getDefaultRoCrateLicense()` (empty default = MIT), category `General`; registered in `StandardSettingsPlugin.register` via `registerApplicationSettingsSheet(new JIPipeProjectIOApplicationSettings())`.

- [ ] **Step 1: Write the failing tests**

`JIPipeXProjectWriterTest` (fixture `basic-images.jipipe` loaded via `loadProject`):
1. **Happy path + round-trip:** `writer.write(project, tmp/"p.jipx", SILENT)` → file exists, zip4j `isZipFile`, entries contain `jipx.json` + `project/graph.json` + `ro-crate-metadata.json` + `README.md`, NO `external-environments.json`; load back with `new JIPipeXProjectReader().read(...)` → uuid→info-id mapping equals the original (non-vacuity: 1 node); the crate metadata parses as JSON, root entity has `conformsTo` containing the workflow-ro-crate/1.0 URI, `mainEntity` = `project/graph.json`, `license` present.
2. **Auto-fill:** fixture project has empty license → saved crate `license` = `https://spdx.org/licenses/MIT`; violations list empty.
3. **Atomicity:** make the target directory read-only after creating the `.part`... (simpler deterministic version): corrupt a part file in a pre-populated workspace is not reachable — instead test the self-verify guard by calling the private verify path via a public seam: `JIPipeXProjectWriter` exposes `static void selfVerifyZip(Path zip)` (used by the write path) → test: build a bad zip (entry `jipx.json` present but a listed part missing) → `selfVerifyZip` throws; and in a failed write (simulate by passing a `target` inside a read-only dir → IOException) the target is absent and no `.part` remains.
4. **Dispatcher:** `JIPipeProjectIOs.writerFromPath(x.jipx)` → `instanceof JIPipeXProjectWriter`; `JIPipeProject.saveProject(jipxPath, true)` on the loaded fixture → file created AND `getProjectFile()` == jipxPath (the updateSavePath seam) — the format-dispatch contract through the legacy-looking API.

- [ ] **Step 2: Run, verify failures** (compile: classes missing; dispatcher: UOE).
- [ ] **Step 3: Implement** per the Interfaces block. Authors-merge resolution (the one behavioral subtlety): the branch's `saveProject(Path, boolean)` is merge-free and BOTH writers' Path variants do the authors merge self-contained (legacy: verified `JIPipeLegacyProjectWriter.write(Path…)`; JIPX: Task 4 step 1). Keep it that way — the JIPX writer merges in its Path variant, the delegate stays trivial.
- [ ] **Step 4: Run tests (pass) + full launcher suite** (regression: `saveProject` now dispatches — `FixtureProjectsLoadTest` round-trip still `.jip`; corpus harness must not be affected).
- [ ] **Step 5: Commit** `feat(jipx): project writer, RO-Crate build, io settings, write dispatch, #753`

### Task 5: GUI migration UX + call-site adoption

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWindow.java` — (a) `saveProjectAs`: the Office-style prompt; (b) file-branch open: adopt `JIPipeProjectIOs.readerFromPath(path).read(path, ...)` in the background run (replaces inline `fromJson` + the double parse — the Phase 0 deferral resolved now that the load path must support `.jipx`); (c) open/save choosers: add `EXTENSION_FILTER_JIPX` (save: jipx FIRST, then jip; open: jip, jipx, crate.zip); (d) `importURL` + `tryAddOpenProjectOnLoad`-adjacent classification: `.jipx` → project open (before the ZIP-magic→crate routing); (e) directory branch: also find `jipx.json` (output-folder convention: `project.jipx`).
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/JIPipeDesktop.java` — `doOpenProjectOnLoad`: `.jipx` extension → `window.openProject(file, true)` BEFORE the `.crate.zip`/ZIP-magic checks; `tryAddOpenProjectOnLoad`: accept `.jipx` files.
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/run/JIPipeGraphRun.java:273-281` — re-save name = `"project." + (project.getProjectFile() != null && fileName ends with ".jipx" ? "jipx" : "jip")`.
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeProjectBackupServiceComponent.java` — `CreateBackupRun.run()`: backup target suffix follows the project's current format: `window.getProjectSavePath()` ends with `.jipx` → `baseName + ".jipx"`, else `.jip` (the `saveProject` dispatch then writes the right container).
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/backups/JIPipeDesktopBackupManagerPanel.java` — restore chooser filter: add `EXTENSION_FILTER_JIPX` after `EXTENSION_FILTER_JIP`.
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/core/CorePlugin.java:186` — `registerFileChooserKnownFileType("JIPipe project 6 file", "apps/jipipe.png", ".jipx")`.
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/SaveFormatDecisionTest.java` (the NON-UI decision logic, extracted as a pure static for testability: `JIPipeDesktopProjectWindow.determineSaveTarget(Path currentSavePath)` → returns an enum `{JIPX_PROMPT, LEGACY_DIRECT}`: `.jip` current file → JIPX_PROMPT; `.jipx` or null/untitled → LEGACY_DIRECT (chooser decides)). The prompt itself (Swing) is verified by eye per policy.

- [ ] **Step 1: Write the failing decision test** (`determineSaveTarget` does not exist yet).
- [ ] **Step 2: Run, verify failure.**
- [ ] **Step 3: Implement** the decision static + wire it into `saveProjectAs`: on JIPX_PROMPT → `JOptionPane.showOptionDialog` (labels: `"Save as JIPipe 6 project (*.jipx) — recommended"`, `"Keep legacy JIPipe project (*.jip)"`, `"Cancel"`, default = first) → jipx: `savePath = PathUtils.ensureExtension(currentSavePath, "jipx", "jip")` (same directory, same base name; the old `.jip` file stays untouched on disk — the save writes the `.jipx` sibling and `projectSavePath`/`projectFile` update to it); keep: unchanged legacy path; cancel: abort. Then the existing `.part` + self-verify + move flow runs with the chosen target (self-verify via `loadProject` now auto-dispatches). Untitled (null savePath) → chooser with jipx-first filter, no prompt. File branch of `openProject`: if the file ends with `.crate.zip` → delegate to `importROCrate(file, …)` (existing flow — the open chooser now offers crate.zip, and peek refuses non-JIPX ZIPs, so this routing must happen BEFORE the peek).
- [ ] **Step 4: Implement the remaining adoption** (open dispatch, importURL, doOpenProjectOnLoad, GraphRun, backups, CorePlugin). No UI tests (policy). Full launcher suite regression + `mvn -pl jipipe-core compile`.
- [ ] **Step 5: Commit** `feat(jipx): save prompt, format dispatch in open flows, call-site adoption, #753`

### Task 6: JIPX round-trip verification (corpus-gated harness + record)

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JipxRoundTripFixpointTest.java`
- Create: `docs/superpowers/plans/2026-10-23-jipx-phase1-verification.md`

**Interfaces:**
- Consumes: everything from Tasks 1–5; the Phase 0 harness pattern (`CorpusRoundTripFixpointTest`) as the structural template.
- Produces: gated harness `-Djipipe.jipx.roundtrip.corpus=<dir>` (+ optional `.limit`, `.report`), report schema mirroring Phase 0 (`corpus`, `totalFiles`, `outcomes{OK, PARSE_FAILED, LOAD_FAILED, SAVE_FAILED, JIPX_LOAD_FAILED, JIPX_SAVE_FAILED}`, entries with `file`, `outcome`, `legacyFixpoint` (s1vs2 canonical — inherited from the Phase 0 cycle), `jipxModelEquivalent` (load .jip → write .jipx → read .jipx → serialize both models to monolith trees → canonical equality), `jipxCrateValid` (crate metadata parseable + root has mainEntity/license), `loadedNodes` (legacy S1 load), `error`). Per project: copy → legacy cycle (S1/S2 as Phase 0) → `writerFromPath` .jipx write → JIPX read → model equivalence → crate checks. Full run against `/data/JIPipe` (~3 min), report to `/data/JIPipe/.roundtrip/jipx.json`.

- [ ] **Step 1: Write the harness** (structure = Phase 0 harness: assumeTrue gate, per-project try/catch continue, pollution guard, deterministic report, canonical comparison helpers REUSED by copy from the Phase 0 test — note: two test files sharing the canonical helper is acceptable for tests; if the implementer prefers, extract to a small package-private `JsonCanonicalizer` utility in the test tree and have Phase 0's test use it too — allowed as a test-only refactor, keeps the Phase 0 file's behavior byte-identical).
- [ ] **Step 2: Smoke run** (limit=3) → verify report shape + the 2 LOAD_FAILED files behave (legacy cycle records their failures, JIPX steps skipped for them, outcome LOAD_FAILED).
- [ ] **Step 3: Full corpus run** → `/data/JIPipe/.roundtrip/jipx.json`.
- [ ] **Step 4: Triage** — every non-OK outcome classified (expected: the 11 legacy LOAD_FAILED; anything else is a Phase 1 bug → fix or report). Record in the report file.
- [ ] **Step 5: Verification record** `2026-10-23-jipx-phase1-verification.md`: corpus size, outcome counts, the equivalence result (X/356 legacy-OK files with jipxModelEquivalent=true — any false is a bug with the exact diff bucket), crate validity counts, environment note (GUI running or not). Commit `docs: JIPX phase 1 corpus verification record, #753`.
- [ ] **Step 6: Full regression** `mvn -pl jipipe-core,jipipe-launcher test` → green. (Both modules — the GUI adoption touched core.)
- [ ] **Step 7: Commit** if any test-only changes remain (the JsonCanonicalizer refactor if done).

---

## Out of scope (this plan; spec §9)

- Multi-graph (`graphs/`), history (`history/`), carried data (`data/`) payloads — reserved manifest slots only.
- Formal published profile page / Profile Crate at jipipe.org (follow-up work item; URI `https://jipipe.org/jipipe` contextual entity is used as a stable placeholder).
- Remote/URI writers, folder-format readers, remote workspaces.
- The crash-recovery assistant (workspace.json hooks exist; restore UX is a follow-up with #1332).
- Fixing the Phase 0-deferred pre-existing bugs (HashSet ordering / null→"null" / custom-name / important drop) — separate work items.
