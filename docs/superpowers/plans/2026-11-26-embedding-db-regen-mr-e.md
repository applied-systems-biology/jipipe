# Embedding DB Regeneration + Model Bundle for Distributions (MR E, #1410) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Release packages ship a freshly regenerated `ai/embeddings.db` (built at package time from the exact built registry + MR D's embedding text) and a pre-seeded embedding model bundle, so first AI search on a fresh install needs no recompute and no artifact download; stale user caches are invalidated.

**Architecture:** A new headless `JIPipeLauncher build-embedding-db` command (jipipe-4) computes embeddings for every node-database entry with a runner constructed directly from given ONNX paths and writes a JEMB file, reading it back before exiting. Runtime model resolution learns a well-known *embedding model bundle* location (jcef-bundle pattern: install dir -> shared dir -> user dir) that the AI service consults before any artifact download, and the `ai:embedding` capability probe reports it as an internal capability. The distribution chain (jipipe-distribution-files) commits the model via Git LFS, runs the command in step1 against the built classpath, injects the resulting DB into the packaged jipipe-core jar, and copies the bundle into the package. The JEMB `CURRENT_VERSION` bump to 3 makes the load gate accept only v3, invalidating every pre-existing v1/v2 user cache and the stale repo bundle in one move.

**Tech Stack:** Java 21 (jipipe-4), bash (distribution scripts), Git LFS (model payload), ONNX Runtime via the existing `JIPipeOnnxEmbeddingAIModelRunner`, Maven offline.

**Spec:** `docs/superpowers/specs/2026-11-25-node-search-1323-design.md` (wave 3, amended model-provisioning ruling of 2026-11-25) + the cache-invalidation action recorded on #1410 (2026-11-26).

## Global Constraints

- Two branches: `1410-embedding-db-regen` in jipipe-4 (from master 38aafc2c94, contains merged !219 + !223) and `1410-embedding-model-bundle` in jipipe-distribution-files (from origin/main d7e80d82). Every commit references `#1410`.
- No em dashes or en dashes anywhere (code, comments, docs, strings, shell).
- The golden gate (`NodeSearchGoldenSetTest` 94/94), `SearchDescriptionQualityTest`, core and launcher suites must stay green on every jipipe-4 task. Launcher runs need `-am clean`; `-pl jipipe-launcher` without `-am` resolves a stale core jar.
- Fail-open everywhere in the dist chain: no model, no command, no network -> loud warning, build continues (the design's "skip with a loud warning so local and minimal builds keep working").
- Fail-closed in the command: any per-entry embedding failure -> non-zero exit, no partial DB shipped.
- The model payload commits to LFS once (platform-independent), never duplicated per platform.
- The repo copy of `embeddings.db` is NOT regenerated or committed in this wave (design: stays as-is; the v3 gate makes it inert; the CI-regenerated DB ships instead).
- Deferred minors go to `.superpowers/sdd/2026-11-26-embedding-db-regen-mr-e/progress.md` (jipipe-4 workspace; the dist-repo work is ledgered there too, one wave one ledger).

## Review Focus

1. **Stale-vector serving (the bug this MR exists to fix).** A user with a pre-change disk cache must never receive old-text vectors after updating. Pin: Task 3's version-gate test (v2 file rejected, v3 accepted); Task 6's package smoke (the shipped jar's DB is v3 built from the new text).
2. **Package build fragility.** The dist phase runs a JVM against the freshly built classpath inside a packaging script - a failure mode that could brick CI. Pin: the phase is fail-open (command absent, model absent, non-zero exit -> warn + continue with the previous DB), verified by a local dry-run of step1/step2 on this machine (Task 6).
3. **Bundle resolution path safety.** The runtime resolution reads model files from user-influenced install locations. Pin: Task 2's tests (bundle absent -> artifact fallback unchanged; manifest model-id mismatch -> ignored with a warning; corrupted manifest -> ignored, never crash).
4. **Capability probe truthfulness.** The probe reports `ai:embedding` available only when a working model is actually resolvable (bundle files present AND hashes match, or artifact installed). A false-positive surfaces as "AI available" that fails on first search. Pin: Task 2's mismatch tests + the manifest-hash verification in the bundle class.
5. **LFS integrity of the model payload.** A truncated LFS object breaks every package build silently at embedding time (minutes in, fail-closed). Pin: Task 6's dist-CI integrity job (SHA256 manifest check, scoped LFS pull, before any build stage).

---

## Part A: jipipe-4 (branch `1410-embedding-db-regen`)

### Task 1: Headless `build-embedding-db` launcher command + JEMB round-trip test

**Files:**
- Create: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/BuildEmbeddingDbCommand.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/JIPipeLauncher.java` (dispatch case `build-embedding-db` + HelpCommand entry)
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/embeddings/JembFormatRoundTripTest.java`

**Interfaces:**
- Consumes: `JIPipeServiceInitializationSettings` + `JIPipeServiceMode.Headless` + `JIPipe.ensureInstance(settings)` (the `HeadlessRunCommand` boot pattern), `JIPipeNodeDatabase.getInstance()` (await entries bounded, the golden-gate's await pattern), `JIPipeOnnxEmbeddingAIModelRunner(Path modelPath, Path tokenizerPath, String modelId)` + `float[] embed(String)`, `JIPipeEmbeddingDatabase` (no-arg or `false`-persistent constructor, `setEmbeddingWithHash(modelId, nodeId, embedding, text)`, `saveToStream`, `loadFromStream`), `JIPipeEmbeddingDatabase.entryToId(entry)` / `entryToText(entry)`, `AIPlugin.DEFAULT_EMBEDDING_ARTIFACT` id resolution (`EmbeddingModelEnvironment.deriveModelId()` semantics - the command's `--model-id` default must equal what the desktop service uses for the default artifact; resolve via the environment with the default artifact query, not a hardcoded string).
- Produces: `BuildEmbeddingDbCommand.doBuildEmbeddingDb(List<String> argsList)`:
  - Args: `--model <path>` (required, the onnx/model.onnx file), `--tokenizer <path>` (required, tokenizer.json), `--output <path>` (required, .db destination), `--model-id <id>` (optional, default per above), `--threads <n>` (optional, default 1).
  - Flow: validate files exist (missing -> stderr + exit 1 before any boot); boot JIPipe headless; await node-database entries (bounded 300 s, the gate's await shape); iterate `getEntries()`, `entryToId` + `entryToText`, `runner.embed`, `db.setEmbeddingWithHash`; count failures (an entry whose embed throws or returns null -> recorded, at the end: any failure -> stderr summary + `System.exit(1)` WITHOUT saving the DB - fail-closed, no partial ship); on success `saveToStream` to the output path (write to `<output>.tmp` then atomic move), then the built-in read-back: `loadFromStream` from the written file into a fresh database, assert model id equal, entry count equal, and hash equality for a 10% sample (seeded) of entries - any mismatch -> exit 1; print a summary line (entries, model id, output path, elapsed) and exit 0.
  - The command never touches GUI classes, never starts the AI service (the runner is direct), and respects `--fast-init`-less full registry init (the DB must cover the full node set).
- Produces: `JembFormatRoundTripTest` (jipipe-core, no model needed): build a `JIPipeEmbeddingDatabase`, set 3 embeddings with hashes for a fixed model id (synthetic vectors), `saveToStream` to a temp file, `loadFromStream` into a fresh instance, assert model id, count, vectors (byte-equal), and hashes all survive; also assert a file written with a different model id fails the model-id equality (the read-back contract the command relies on).

- [ ] **Step 1: Write the failing round-trip test** (JembFormatRoundTripTest): FAIL (no test).
- [ ] **Step 2: Implement** the test's needs only (it exercises existing JEMB APIs - if the test reveals a missing access path, add the minimal accessor to JIPipeEmbeddingDatabase and note it).
- [ ] **Step 3: Run** `mvn -pl jipipe-core clean test -o -Dtest=JembFormatRoundTripTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS.
- [ ] **Step 4: Implement the command + dispatch + help entry.**
- [ ] **Step 5: Live run (the real proof):** `mvn -pl jipipe-launcher -am clean package -o -DskipTests -q` then run the command against the local model: `java -cp <launcher-classpath from mvn dependency:build-classpath or the assembled jars> org.hkijena.jipipe.launcher.JIPipeLauncher build-embedding-db --model /home/rgerst/.cache/jipipe/embedding-model/onnx/model.onnx --tokenizer /home/rgerst/.cache/jipipe/embedding-model/tokenizer.json --output /tmp/opencode/embeddings-test.db`. Expected: full run (a few minutes on CPU), exit 0, read-back summary, a JEMB v2 file. Record: entry count, elapsed, output size. (This run also proves the 106 KB model.onnx + 133 MB model.onnx_data external-data pair loads - note whether `--model` must point at model.onnx with model.onnx_data alongside: the ONNX loader resolves external data relative to the model file; document the requirement in the command javadoc and the dist script.)
- [ ] **Step 6: Negative runs:** missing model file -> exit 1 before boot; `--output` to a read-only dir -> exit 1 with a clear message.
- [ ] **Step 7: Verify** core + launcher suites green (gate included).
- [ ] **Step 8: Commit** `feat(#1410): headless build-embedding-db launcher command + JEMB round-trip test`

### Task 2: Runtime embedding model bundle + capability probe

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/ai/EmbeddingModelBundle.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/ai/EmbeddingModelBundleTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeAIServiceComponent.java` (bundle-first resolution for the default-artifact environment)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/ai/AIPlugin.java` (register the bundle capability provider)

**Interfaces:**
- Produces: `EmbeddingModelBundle` (final class):
  - `public static final String BUNDLE_DIR_NAME = "embedding-model-bundle"`; `public static final String MANIFEST_NAME = "manifest.json"`.
  - Manifest schema (documented in javadoc): `{"modelId": "<full artifact id, e.g. cn.ac.baai.bge_small_en:1.5-any_onnx>", "version": 1, "files": {"onnx/model.onnx": "<sha256>", "onnx/model.onnx_data": "<sha256>", "tokenizer.json": "<sha256>"}}` - files are the artifact layout (the `embedding/onnx/...` prefix is NOT part of bundle paths: the bundle dir IS the artifact root).
  - `public Path resolve(java.util.function.Supplier<Path> installDir, Supplier<Path> sharedDir, Supplier<Path> userDir)` - the jcef fallback chain (CefPlugin:72-89): first dir whose `<dir>/embedding-model-bundle` exists; the injectable suppliers keep it testable (production callers pass `PathUtils::getImageJDir` etc.).
  - `public static Optional<ResolvedBundle> load(Path bundleDir)` - parses the manifest (malformed -> empty Optional + debug log, never throws), verifies every listed file exists with matching SHA256 (mismatch -> empty + warn), returns `ResolvedBundle(Path modelOnnx, Path tokenizerJson, String modelId)` (record). Verification is the probe's truthfulness guarantee (Review Focus 4).
  - `public static boolean isUsableFor(ResolvedBundle bundle, String expectedModelIdPrefixOrFullId)` - the match test the service uses (exact model id equality).
- Modifies: `JIPipeAIServiceComponent` - in the path that resolves the embedding model for the DEFAULT artifact environment (the environment has no local files set, `loadFromArtifact` with the default query, or the default-constructed env): if `EmbeddingModelBundle.load(...)` resolves and its model id matches the default artifact's derived id, construct the runner from the bundle paths instead of the artifact (the artifact download path stays the fallback: no bundle -> existing behavior byte-identical; dev/source builds unchanged). The existing `hasEmbeddingModel()`/`isReady`/start machinery is untouched (the runner construction site is the only change). Find that construction site: where the service currently builds the `JIPipeOnnxEmbeddingAIModelRunner` from the environment's local files or the deployed artifact.
- Modifies: `AIPlugin.register` - a new `JIPipeCapabilityProvider` (kind INTERNAL, probe = bundle present AND hash-verified AND id matches the default) that satisfies `ai:embedding` alongside the existing artifact provider (check how multiple providers for one capability combine - the capability registry's availability should be OR of providers; verify and note in the report).
- Produces: `EmbeddingModelBundleTest` (jipipe-core): resolve chain order (install > shared > user, absent skips); malformed manifest -> empty; hash mismatch -> empty + the warn fires (capture via logger or observable state); id mismatch via `isUsableFor` -> false; a valid temp bundle -> all green. No Swing, no model, no JIPipe boot required (pure class).

- [ ] **Step 1: Write the failing bundle tests.**
- [ ] **Step 2: Implement** the bundle class; tests PASS.
- [ ] **Step 3: Wire** the service resolution + capability provider.
- [ ] **Step 4: Verify** core suite green + a manual smoke: place a copy of the local model at a temp "install dir" with a correct manifest, point the test-seam or a small main at it, and confirm the service constructs the runner from the bundle (log line proof); absent bundle -> artifact path (log proof). Record both in the report.
- [ ] **Step 5: Commit** `feat(#1410): embedding model bundle resolution and capability probe`

### Task 3: JEMB version 3 cache invalidation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/embeddings/JIPipeEmbeddingDatabase.java` (`CURRENT_VERSION` + the load gate)
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/embeddings/JembFormatRoundTripTest.java` (version-gate assertions)

**Interfaces:**
- `CURRENT_VERSION = 3` (format identical to v2 - the version int is the invalidation signal; javadoc: v3 marks the post-#1409 embedding-text restructure; v1/v2 files are rejected wholesale because their vectors encode the old entry text).
- The load gate (line ~948): accept `version == CURRENT_VERSION` ONLY; v1/v2 -> the existing warn path + empty database (no partial load). This invalidates: existing user disk caches (all v2) and the stale repo bundle resource (v2, loaded via the resource path - it now warns and starts empty, dev builds fill JIT - the intended inert state).
- Round-trip test gains: a v2-written file (hand-crafted header, one entry) loads as EMPTY with the warn; a v3 round trip is byte-complete (existing assertions now over v3).

- [ ] **Step 1: Extend the tests** (v2 rejection, v3 round trip): FAIL.
- [ ] **Step 2: Implement** the version change.
- [ ] **Step 3: Run** the round-trip test + full core suite: green. Note in the report: the repo `ai/embeddings.db` resource now loads empty with a warning on dev builds (expected, intended); the golden gate and description gate unaffected (they don't touch embeddings).
- [ ] **Step 4: Commit** `feat(#1410): JEMB version 3 invalidates pre-restructure embedding caches`

## Part B: jipipe-distribution-files (branch `1410-embedding-model-bundle`, from origin/main)

### Task 4: Model payload + manifest via Git LFS

**Files:**
- Create: `scripts/embedding-model-bundle/onnx/model.onnx`, `scripts/embedding-model-bundle/onnx/model.onnx_data`, `scripts/embedding-model-bundle/tokenizer.json`, `scripts/embedding-model-bundle/manifest.json` (all LFS-tracked except the manifest)
- Modify: `.gitattributes` (add `scripts/embedding-model-bundle/** filter=lfs diff=lfs merge=lfs -text` - the existing per-directory LFS pattern)

**Interfaces:**
- Source: the local artifact at `/home/rgerst/.cache/jipipe/embedding-model/` (exact artifact layout: `onnx/model.onnx` + `onnx/model.onnx_data` external data + `tokenizer.json` + the extra tokenizer files `vocab.txt`/`tokenizer_config.json`/`special_tokens_map.json` - copy the whole artifact directory contents, the runner may need the config siblings; verify what `JIPipeOnnxEmbeddingAIModelRunner` actually reads: if it reads ONLY tokenizer.json, ship only what's read + document; default: ship all files present, manifest lists all of them).
- Manifest: model id = the full default artifact id the service derives (resolve it from jipipe-4's `EmbeddingModelEnvironment`/`AIPlugin` - must equal Task 2's `isUsableFor` expectation; write the EXACT string the production code produces, verify by running jipipe-4 code or the command's default), `version: 1`, `files` = relative path -> lowercase hex SHA256 for every shipped file.
- LFS: `git lfs track` via the .gitattributes line, add, commit - the repo's LFS remote is the same GitLab instance; verify `git lfs ls-files` lists the payload after the commit (and note the push requirement: the dist repo push must upload LFS objects - record the command output).

- [ ] **Step 1: Copy** the artifact into `scripts/embedding-model-bundle/` (preserving layout), generate the manifest (python3 hashlib, the file list from the directory).
- [ ] **Step 2: Verify** the model id string against jipipe-4's resolution (run a one-liner or read `deriveModelId`/the environment default artifact's full id; the id must be the one the artifact registry reports for the default embedding artifact - if it is version-pinned, use the full id including version; record the decision).
- [ ] **Step 3: .gitattributes** + commit `feat(#1410): embedding model bundle payload (LFS) with integrity manifest`.
- [ ] **Step 4: Push** the dist branch (`git push -u origin 1410-embedding-model-bundle`), verify `git lfs ls-files` on the remote ref (a fresh clone into a temp dir + `git lfs pull --include="scripts/embedding-model-bundle/**"` + hash check = the integrity proof).

### Task 5: Packaging chain (step1 regen + inject, step2/3 bundle copy)

**Files:**
- Modify: `scripts/step1-publish-snapshot-ci.sh` (the regen phase)
- Modify: `scripts/step2-create-zip.sh` (bundle copy into the package)
- Modify: `scripts/step3-build-packages.sh` (bundle copy into the installer packages)
- Modify: `scripts/step1-publish-snapshot.sh` + `scripts/step1-publish-beta.sh` (the release-flow variants: same optional phase, per the design's "and the release-flow variants")

**Interfaces:**
- Step1 phase (after `mvn clean package`, before the OUT_DIR finalization - insert a clearly-marked block):
  - `EMBEDDING_BUNDLE="${EMBEDDING_MODEL_BUNDLE:-${SCRIPTS_DIR}/embedding-model-bundle}"`; model file = `${EMBEDDING_BUNDLE}/onnx/model.onnx`, tokenizer = `${EMBEDDING_BUNDLE}/tokenizer.json`.
  - Preconditions (each failure -> loud WARN, phase skipped, build continues): bundle dir exists; model + tokenizer exist; `git lfs` status sane (files are real, not pointer files - check the `version https://git-lfs.github.com/spec/v1` first line; the CI clone in jipipe-4's `distribution_package` job does a plain `git clone --depth 1` - IT MUST GAIN `git lfs install --local` + a scoped `git lfs pull --include="scripts/embedding-model-bundle/**"` first; that CI change belongs to jipipe-4's `.gitlab-ci.yml` - Task 6 carries it); the launcher classpath command exists: `java -cp "$CP" org.hkijena.jipipe.launcher.JIPipeLauncher help` output contains `build-embedding-db` (absent -> the jipipe-4 side is not merged yet -> warn + skip).
  - Classpath: the built modules in `${TARGET_DIR}` - `jipipe-launcher/target/jipipe-launcher-*.jar` + all plugin/core jars the launcher needs; the simplest robust construction: `mvn -q -f ${TARGET_DIR}/jipipe-launcher/pom.xml dependency:build-classpath -Dmdep.outputFile=/tmp/...` won't see the freshly built snapshot jars unless installed... Decision: run the command with `-cp` built from `TARGET_DIR/jipipe-launcher/target/*.jar` + the reactor jars via `find ${TARGET_DIR} -name "*-SNAPSHOT.jar" -path "*/target/*"` (the mvn package output; document the assumption and verify in the dry-run) - the launcher's `main` must be resolvable.
  - Command: `${JAVA_HOME}/bin/java -cp "$CP" org.hkijena.jipipe.launcher.JIPipeLauncher build-embedding-db --model ... --tokenizer ... --output "${OUT_DIR}/embeddings-regenerated.db"` - non-zero exit -> loud FAIL, do NOT inject (the previous DB ships), the phase is skipped with a clear error (fail-open per the design's local/minimal-build guarantee; the CI job then has an artifact showing the stale DB - a known limitation, documented in the MR description).
  - Injection: `jar uf` / `zip` update of `${TARGET_DIR}/jipipe-core/target/jipipe-core-*-SNAPSHOT.jar` entry `org/hkijena/jipipe/ai/embeddings.db` with the regenerated file (verify the jar tool available in the CI image: `jar` from the embedded JDK works; `zip` may be absent - prefer `jar uf`). Verify the injection (unzip -p | head comparison of the version int or just the entry size).
- Step2/step3: after the package tree is assembled (where the template root lands in `package/` / the installer staging - find the existing jcef-bundle flow: it rides in the template, so the bundle lands with the template copy; for the single-copy bundle, an explicit `cp -r "${SCRIPTS_DIR}/embedding-model-bundle" "<package-root>/embedding-model-bundle"` at the point where `package/plugins` etc. are populated; step3 for each platform variant it packages).
- All new script lines: `set -euo pipefail`-safe (the phase uses explicit `|| { warn; }` guards so a skip never kills the script).

- [ ] **Step 1: Implement** the step1 phase + step2/step3 copies + the variant scripts.
- [ ] **Step 2: Dry-run (the Review Focus 2 pin):** on this machine simulate the phase: export a source tar of the jipipe-4 branch (like step1 does), run the step1 phase logic against the built jars with the local bundle, confirm: command found, DB regenerated (v3, ~2400 entries), jar entry replaced, `jar tf` shows the new entry size; then the skip paths: bundle dir missing -> warn + continue (exit 0); command missing (point at an old jipipe tree) -> warn + continue. Record every line.
- [ ] **Step 3: Commit** `feat(#1410): regenerate embedding DB and seed the model bundle in the packaging chain`.

### Task 6: CI wiring + dist integrity job + both MRs

**Files:**
- Modify (jipipe-4): `.gitlab-ci.yml` `distribution_package` job (LFS enable + scoped pull in the cloned dist repo)
- Create (dist repo): `ci/model-bundle-integrity/run.sh` + a job in the dist `.gitlab-ci.yml` (stages: before the smoke tests - it is cheap and protects the payload): scoped `git lfs pull --include="scripts/embedding-model-bundle/**"`, verify every manifest SHA256, verify the manifest parses (python3), verify the model id is non-empty. Failure = job failure (this one is NOT fail-open: a broken payload must not silently ship; the fix is re-pushing the LFS object, not skipping the build).
- (Both repos) the MRs: jipipe-4 MR `feat(#1410): embedding DB regeneration for release distributions` (references #1410 + the dist MR url), dist MR (references #1410 + the jipipe-4 MR url), both target their defaults, remove_source_branch on.

**Interfaces:**
- jipipe-4 CI edit: in the `distribution_package` script, after the clone: `cd jipipe-distribution-files && git lfs install --local && git lfs pull --include="scripts/embedding-model-bundle/**" && cd scripts` (the job image must have git-lfs - verify the base image; if not, `apt-get install -y git-lfs` as the dist smoke jobs do - check what `.jipipe_ci` provides).
- Merge order: the jipipe-4 MR can merge first (the dist phase is command-aware and skips); the dist MR then enables the real regeneration. Cross-reference both in the descriptions. Completion note on #1410.

- [ ] **Step 1: jipipe-4 CI edit** + verify the job's script block reads correctly (shellcheck if available; at minimum bash -n on the extracted script).
- [ ] **Step 2: dist integrity job + script** + local run of `ci/model-bundle-integrity/run.sh` (must pass on the committed payload).
- [ ] **Step 3: Commit** both sides (`chore(#1410): CI LFS wiring for the embedding model bundle` in jipipe-4; a separate commit in the dist repo for the integrity job if not folded into Task 5's), **push** both branches.
- [ ] **Step 4: Open both MRs** (save_merge_request), cross-referenced; link the session; completion note on #1410 (both MR urls, the measured local dry-run numbers, the merge-order guidance).
- [ ] **Step 5: Suites** (jipipe-4 final head): core + launcher green.
- [ ] **Step 6: Package smoke check (spec test requirement, post-merge):** after BOTH MRs merge and the `distribution_package` CI job runs on master, from the CI artifacts: extract the zip package, assert `plugins/JIPipe/jipipe-core-*.jar` contains `org/hkijena/jipipe/ai/embeddings.db` as a v3 file (read the version int: 3) with ~2400 entries (parse with the JembFormatRoundTripTest-style reader or the command's read-back logic), and assert the package root contains `embedding-model-bundle/` with the manifest. Record the numbers on #1410. The "no full recompute on first AI search" property follows from v3-DB-new-text + v3-gate (old caches rejected, new DB matches MR D's text hashes) - note this reasoning in the note. If the CI job has not run yet when this step is reached, mark it pending in the ledger and run it when the pipeline lands (do not block the MRs on it).

## Scope rulings (plan-level, recorded)

- The repo copy of `ai/embeddings.db` is NOT touched: with the v3 gate it loads empty in dev (warning logged); the CI-injected v3 DB is what ships. A scheduled refresh of the repo copy remains an optional follow-up (design wave 3 item 4).
- The bundle lives at `scripts/embedding-model-bundle` (single copy, LFS) and is copied into packages by step2/step3 - NOT committed per platform template (the model is platform-independent; the jcef bundles are per platform because CEF binaries are).
- The step1 non-zero-exit path ships the previous DB with a loud CI-visible failure (fail-open per the design), not a broken package: a regenerated-DB regression is better served by the existing DB than by a missing one; the integrity job + MR pipeline catch persistent breakage.
- The capability provider reports INTERNAL-kind availability for the bundle; the existing artifact provider stays (OR-combination, verified in Task 2).
- `--model` points at `onnx/model.onnx` with `model.onnx_data` (ONNX external data) alongside it - the runner/loader resolves external data relative to the model file (verified in Task 1's live run; documented in the command javadoc and the dist script).
