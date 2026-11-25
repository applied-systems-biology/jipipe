# Node search finetuning and embedding search (#1323) - design

- Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1323
- Date: 2026-11-25
- Status: draft for approval
- Builds on: #1404 (MR !216 search ranking, MR !217 description sweep, both merged)

## Goal

Users find the node they mean from realistic queries. Two target audiences:

1. **Experimentalists** (biologists, microbiologists) query the visible outcome without
   technical terms: "find the cells", "color macrophages red", "make diagram",
   "summarize the results".
2. **Bioinformaticians** know image analysis and query the algorithm or the ImageJ
   vocabulary without knowing JIPipe's exact node name: "kmeans", "local Otsu
   threshold", "watershed split".

Failure classes to fix (from the work item):

- Misspellings fall through ("kmeans" vs haystack token "k-means" split into "k", "means").
- Term is a substring of the haystack but the node is not found.
- Ordering prefers specialized nodes over the simple one the user expects.
- Embedding ("AI") search is hit-and-miss; users query in outcome words or ImageJ terms.
- Release distributions ship a stale bundled embedding DB, forcing a full embedding
  recompute ("full embedding db refresh") on first AI search.

## Non-goals

- No LLM/cloud dependency added to the search path (local ONNX embedding model only).
- No rework of !216's usage-prior machinery beyond what the golden set demands.
- Auto-import families stay out of hand-curation scope (ruling A of #1404 continues:
  `ij2:op:*`, `clij:plugin:*`, `trackmate-create-spot-{tracker,detector}-*`).

## Ground truth: the golden set (reviewed deliverable)

A JSON corpus at jipipe-launcher test resources, plus a permanent gate test:

- Case shape: `{ id, query, tags, expectWithin: N, expect: [node ids], first: optional }`.
  A case passes when every `expect` id ranks within the first `N` results of the lexicon
  search (and `first`, when set, ranks first). Ordering of the remaining list is not
  asserted; that stays flexible for future ranking work.
- Tags encode the audience and failure class: `biologist-outcome`, `bioinfo-algorithm`,
  `imagej-vocab`, `typo`, `substring`, `ordering`, `dimensionality`. A case may carry
  several tags.
- Size target: 60 to 100 cases. Authoring sources, in order of weight:
  1. The real example workflows at `/data/JIPipe/Example workflows/` (61 files): 59
     contain graphs with 320 distinct node ids and 10,189 node placements; the
     most-placed ids (thresholding, morphology, ROI to RGB, exports, plots, annotation
     flows) are what users demonstrably search for and seed both audiences' queries.
  2. A dimensionality scan of a fresh node name and description dump (gate dump tool;
     the sweep batches hold pre-!217 text and serve as a format reference only).
  3. The docs and the ImageJ operation vocabulary, expanded with typo variants of real
     node vocabulary.
- Gate: `NodeSearchGoldenSetTest` in jipipe-launcher (live registry, same pattern as
  `SearchDescriptionQualityTest`). The corpus is authored first, reviewed by the user as
  a deliverable (it defines what "realistic" means), then pinned; scorer changes must
  keep it green.
- The same corpus feeds the embedding evaluation (wave 2) and can seed future corpus
  growth.

## Wave 1: lexicon search fixes (MR C, jipipe-4 only)

Scorer work in `JIPipeEnhancedNodeDatabaseSearch` / `NodeSearchText`, gated by the
golden set:

1. **Compound and hyphen bridging.** Comparison forms strip internal hyphens,
   underscores, and dots from tokens ("k-means" compares as "kmeans") on query and
   haystack side. Adjacent single-letter-plus-word haystack tokens ("k", "means") also
   compare as the joined unit against a joined query token. The raw token stream fed to
   existing rungs stays untouched (same principle as !217: additive comparison forms,
   bit-identical base behavior).
2. **Typo tier.** Below the existing 0.85 Jaro-Winkler rung, a weaker tier at
   approximately 0.78 with a minimum token length guard (>= 5) and at most one edit per
   word. No keyboard-adjacency modeling in this round; re-evaluate on golden-set typo
   cases only if the plain tier misses them.
3. **Substring diagnosis.** Enumerate the golden-set substring failures, classify
   word-start prefix vs mid-word, and fix the classes the corpus proves broken. Expected
   finding: word-start prefixes already match; mid-word substrings are covered by (1) or
   get a relaxed rung only where the corpus shows a real case.
4. **Simplicity preference.** Strengthen the weight of exact matches on display-name
   tokens (a query hitting the node name outranks description-only hits), on top of the
   !216 usage prior. A curated prominence list is added only if ordering cases still
   fail after this.
5. **Dimensionality terms.** Measured on the 447-row sweep dump: 147 node names mention
   "2D", 54 mention "3D" (descriptions: 171 and 97); related vocabulary is sparser
   (z-stack 1 name, time-lapse 2, hyperstack 5, stack 7, slice 7). Dimension tokens are
   therefore the highest-frequency distinguishing tokens in the catalog, and a bare
   "2d"/"3d" query is under-constrained while "segment cells 3d" must not lose the
   dimension constraint. Scorer handling, gated by `dimensionality` corpus cases:
   - Lowercase token matching already covers "2D" vs query "2d"; verify, do not assume.
   - Normalize compound forms to the dimension token ("3-d", "3 d", "three-dimensional",
     "3 dimensional" all compare as "3d").
   - Consume the structured `spatialDimensions` metadata of the 6.0.0 node taxonomy
     (2D/3D, a closed set) as a ranking signal: a query containing a dimension token
     boosts nodes whose spatial dimensions match and demotes those it excludes, on top
     of normal text scoring.
   - Related vocabulary (z-stack, time-lapse, hyperstack, slice) gets corpus cases; a
     synonym expansion into dimension tokens is added only where a case proves the
     miss (z-stack implies 3D spatial extent, time-lapse does not map cleanly onto the
     2D/3D set, so no blind expansion).

## Wave 2: AI-assisted ranking and embedding quality (MR D, jipipe-4 only)

1. **Merged ranking by default (ruling 2026-11-25).** The node browser drops the
   dedicated AI-search toggle button: one result list, blended ranking, always on
   unless AI is unavailable. The lexicon score and the embedding cosine are mapped
   onto a comparable scale and combined (usage prior applied to both); the AI
   contribution activates only when the general AI setting is enabled and the local
   model is available - resolved from the internal embedding model bundle first
   (wave 3), artifact download as fallback. When AI is disabled or the model is
   absent, the classic algorithm alone serves results - graceful degradation, no UI
   breakage, no empty states caused by a missing model. The `ai:embedding` capability
   probe learns the bundle location so distributions report the capability as
   available without any download.
2. **"Node search" settings page** in the general settings category: user control of
   the search system. Candidate controls (final set fixed in the MR D plan): AI
   contribution on/off, typo tolerance on/off, reset of the usage prior. The page is
   the single place where the search system is configured; no per-search mode
   switching in the browser.
3. **Embedded text restructuring** in the entry renderer used by
   `JIPipeEmbeddingDatabase` ("Name / Description / Menu / Inputs / Outputs" today):
   add the 6.0.0 function-taxonomy path, keep the name first, use the outcome-first
   !217 descriptions as the bulk of the signal, condense slot lists; respect the token
   bound. Changing the rendered text re-keys the vector cache through the content hash
   (existing invalidation path, by design).
4. **Evaluation harness**: a run-anywhere check that executes golden-set queries
   through the merged ranking and reports top-N hit rate per tag, with and without the
   AI contribution. Not a hard CI gate (requires the local ONNX model); runs locally
   and as an optional CI job when model files are present.
5. Model quality is re-visited only if the harness shows bge_small_en failing on
   outcome queries: the DB is keyed per model id, so switching or adding a model is a
   data change, not a code change.

## Wave 3: embedding DB regeneration for distributions (MR E)

**Facts.** The bundled DB is the classpath resource `ai/embeddings.db` (JEMB binary,
model `cn.ac.baai.bge_small_en:1.5-any_onnx`, entries loaded pre-verified). The repo
copy was last regenerated 2026-09-10, before the !217 description sweep, so released
packages hold embeddings of outdated text and stale node sets: first AI search on a
fresh install recomputes everything. The CI job `distribution_package` (jipipe-4
`.gitlab-ci.yml`) clones `jipipe-distribution-files` and runs `step1-publish-snapshot-ci.sh`
(export source, `mvn clean package`), `step2-create-zip.sh` (dist/zip), `step3-build-packages.sh`
(installers). `jipipe-cli` no longer exists; `jipipe-launcher` is the CLI entry point
(command pattern, e.g. `HeadlessRunCommand`).

**Changes.**

1. **jipipe-4**: a new headless `JIPipeLauncher` command (`build-embedding-db`):
   initializes the full registry and node database without a GUI, loads the ONNX model
   and tokenizer from given paths, computes embeddings for every current entry (same
   logic as the desktop build tool), writes the JEMB file to a given output path, exits
   non-zero on any entry failure (fail-closed).
2. **jipipe-distribution-files**: `step1-publish-snapshot-ci.sh` (and the release-flow
   variants) gain an optional phase between `mvn package` and the zip step: when model
   and tokenizer files are provided (env), run the command against the built jar
   classpath, write the DB, and inject it into the packaged jar so `step2`/`step3` pick
   it up unchanged. When no model is present: skip with a loud warning so local and
   minimal builds keep working.
3. **Model provisioning (ruling 2026-11-25, amended): the model ships with the
   distribution.** Model + tokenizer are committed to `jipipe-distribution-files` via
   Git LFS (the repo is already LFS-based with JDKs and templates), pinned and
   offline-available. They serve twice:
   - **At package build time**: the DB regeneration in step1 reads them.
   - **At runtime**: step2/step3 pre-seed an *embedding model bundle* into the package,
     mirroring the jcef bundle pattern (`CefPlugin`: a well-known bundle directory in
     the installation, resolved ImageJ dir -> shared dir -> user dir). The bundle holds
     the artifact layout (`onnx/model.onnx`, `tokenizer.json`) plus a manifest naming
     the model id, so one code path serves both.
   At runtime the AI service resolves the model from the internal bundle first; the
   existing artifact download remains the fallback for dev and source builds (and for
   users of minimal packages without the bundle). With the bundle present the
   `ai:embedding` capability is satisfied without the artifact server: the model
   becomes an internally managed system, always available in distributions.
4. **Repo copy** of `embeddings.db` stays as-is in this wave: stale bundled entries are
   inert (keyed by node id + content hash), dev builds fill gaps JIT. Optional
   follow-up: a scheduled pipeline that regenerates and commits a refreshed DB.
5. **Determinism**: the regenerated DB is a package-time artifact, not committed, so
   cross-run/cross-architecture byte differences do not matter. A read-back test
   verifies the written DB (model id, entry count, hash set) loads identically.

## Delivery

- MR C `1408-golden-set-lexicon` (#1408): golden set + wave 1 (jipipe-4)
- MR D `1409-ai-merged-ranking` (#1409): wave 2 (jipipe-4)
- MR E `1410-embedding-db-regen` (#1410): wave 3 (jipipe-4 launcher command + jipipe-distribution-files
  scripts, cross-referenced MRs)
- Parent item #1323 rewritten 2026-11-25 with the audiences, rulings, and child items.
- Order: C first (it defines "correct"); D and E can proceed in parallel after C.
- Branch naming per convention; deferred minors ledgered per MR in `.superpowers/sdd/`.
- Testing: `NodeSearchGoldenSetTest` green; existing gates stay green
  (`SearchDescriptionQualityTest`, core and launcher suites); JEMB read-back test;
  package smoke check after a CI dry run (DB present in artifact, no full recompute).

## Risks

- **Corpus bias**: cases authored by one head can overfit the scorer. Mitigated by user
  review of the corpus before it is pinned, and by tagging both audiences separately.
- **Embedding model quality on outcome queries**: bge-small may underperform on
  "make diagram" style queries; the harness makes this measurable before any model
  change is attempted.
- **CI cost**: embedding all entries with bge-small on CI CPU is on the order of
  minutes per package build; acceptable inside the package stage.
- **distribution-files is a separate repo**: its MRs cannot be validated by jipipe-4 CI;
  the phase is fail-open (skip when no model) and verified by a manual package run.
