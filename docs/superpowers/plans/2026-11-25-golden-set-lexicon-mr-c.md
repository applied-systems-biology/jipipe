# Golden Set + Lexicon Search Fixes (#1408, MR C) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pin node-search quality with a reviewed golden-set gate test and fix the lexicon scorer against it: compound/hyphen bridging, typo tier, substring rungs, dimensionality handling, and simplicity preference.

**Architecture:** A JSON corpus of query -> expected-node cases lives in jipipe-launcher test resources; `NodeSearchGoldenSetTest` runs every case through `JIPipeEnhancedNodeDatabaseSearch` over the live registry (the `SearchDescriptionQualityTest` pattern) and fails with a dumpable worklist. Scorer fixes are additive comparison forms and one new weak rung in the existing ladder; the raw token stream stays untouched. A mining script turns the real example workflows and a registry dump into corpus candidates.

**Tech Stack:** Java 21, JUnit 5 (jipipe-launcher test module), Jackson (corpus parsing), Maven offline builds.

**Spec:** `docs/superpowers/specs/2026-11-25-node-search-1323-design.md` (wave 1 + golden set sections; rulings of 2026-11-25).

## Global Constraints

- Branch `1408-golden-set-lexicon`; every commit message references `#1408`.
- No em dashes or en dashes anywhere (code, comments, docs, strings).
- `NodeSearchText.splitWords`, `SPLIT_WORD_PATTERN`, `contentWords`, `coveredBy` stay byte-identical: `SearchDescriptionQualityTest` must stay green after every task. All tokenizer work is NEW additive methods.
- The search token stream fed to existing rungs stays unchanged; fixes only add comparison forms, rungs, and bias terms.
- No Swing UI tests (test policy 2026-10-20); the gate test is the deliverable test.
- Builds run offline: `mvn -o`; the core suite needs `clean` (`mvn -pl jipipe-core clean test -o`, about 10 min, run in background).
- Deferred minors go to `.superpowers/sdd/2026-11-25-golden-set-lexicon-mr-c/progress.md`.
- Corpus assertion strengths are never weakened without a recorded corpus-edit justification (see Review Focus 1).

## Review Focus

1. **Silent corpus weakening.** When a case fails after a scorer change, the temptation is editing the case to pass. Every corpus edit during fix rounds must be recorded in `utils/taxonomy-research/golden-set/review.md` with the reason; a case is withdrawn only when it was wrong as authored (wrong expected node, unbuildable query), never because the scorer could not be fixed. Owner: Task 1 corpus; enforced by review record in every fix task.
2. **Threshold regressions against !216.** The ladder precedence (exact name > name prefix > coverage), the usage bonus bound (0.05, coverage-scored only), and `bucketFor` are tuned behavior. No existing constant changes value; new constants are additive. Owner: Tasks 3-7; each task's gate run must show previously green cases staying green.
3. **IDF sensitivity to registry growth.** Cases assert "within N" (default N=5), never exact position; only an explicit `first` case asserts position 1. New plugins must not break the gate. Owner: Task 2 harness + corpus integrity test.
4. **Shared-tokenizer drift.** `NodeSearchText` is consumed by `SearchDescriptionQualityTest`; any change to its existing methods silently moves the description gate. Owner: Tasks 3-6; `SearchDescriptionQualityTest` green in every task's verification.
5. **Corpus non-determinism.** Duplicate case ids, empty expects, or tag minimums drifting during edits. Owner: Task 2 `corpusIntegrity` test pins uniqueness, non-empty expects, and per-tag minimum counts.

---

### Task 1: Corpus mining and golden-set authoring

**Files:**
- Create: `utils/search-usage/build-golden-set-candidates.py`
- Create: `jipipe-launcher/src/test/resources/org/hkijena/jipipe/api/nodes/database/golden-set.json`
- Create: `utils/taxonomy-research/golden-set/review.md`

**Interfaces:**
- Consumes: `/data/JIPipe/Example workflows/*.jip` (graph node ids under `jipipe:node-info-id`), a fresh registry dump via the existing dump property `mvn -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Djipipe.desc-sweep.dump=<path>`.
- Produces: `golden-set.json`, an array of cases `{ "id": "<stable-kebab-id>", "query": "<user query>", "tags": ["..."], "expectWithin": 5, "expect": ["<node-id>", ...], "first": "<node-id>" }`. `first` and `expectWithin` optional (default N=5). Tag vocabulary exactly: `biologist-outcome`, `bioinfo-algorithm`, `imagej-vocab`, `typo`, `substring`, `ordering`, `dimensionality`.

- [ ] **Step 1: Write the mining script**

`build-golden-set-candidates.py`: reads the example workflows (JSON, key `jipipe:node-info-id` under `graph.nodes`), aggregates node id frequency, joins with the registry dump (name, description plain text), and writes `utils/taxonomy-research/golden-set/candidates.json` sorted by placement count with one candidate block per node id. Reuse the JSON handling style of `utils/search-usage/build-description-sweep-worklist.py`.

- [ ] **Step 2: Produce the fresh registry dump and run the script**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest -Djipipe.desc-sweep.dump=/tmp/opencode/registry-dump.json -Dsurefire.failIfNoSpecifiedTests=false`
Then: `python3 utils/search-usage/build-golden-set-candidates.py /tmp/opencode/registry-dump.json`
Expected: `candidates.json` exists with placement counts (top placements: `ij1-roi-to-rgb` 298, `ij1-threshold-auto2d` 242, `ij1-morph-operation2d` 228, ...).

- [ ] **Step 3: Author `golden-set.json`**

60 to 100 cases. Per-tag minimum counts: `biologist-outcome` >= 15, `bioinfo-algorithm` >= 15, `imagej-vocab` >= 10, `typo` >= 10, `substring` >= 6, `ordering` >= 8, `dimensionality` >= 10. Every case's `expect` ids are verified against the dump (no invented ids). Dimensionality cases cover: lowercase `2d`/`3d` queries against dimension-led names, compound forms (`3-d`, `3 d`, `three dimensional`), and constraint queries (`segment cells 3d` must rank a 3D node within N over 2D-only equivalents; `2d` analog). Typo cases are one-edit variants of real node vocabulary (`treshold`, `cellpose`, `watersed`); a typo case's `expect` node must NOT be findable by the untouched scorer (verify with the dump's word sets before authoring, drop cases the current scorer already passes into a `covered-anyway` note in `review.md`). Ordering cases pin the simpler node first (`first`) where the design's simplicity ruling applies. Every case gets an `evidence` string (workflow-mined node id, doc section, or vocabulary source) - stored in the case as `"evidence"`; the gate ignores it.

- [ ] **Step 4: User review checkpoint**

Post the corpus to the user as a reviewable summary (per-tag counts, 10 representative cases, the full file path). State that pinning happens after their ack. Do not start Task 2 before the ack arrives.

- [ ] **Step 5: Commit**

```bash
git add utils/search-usage/build-golden-set-candidates.py utils/taxonomy-research/golden-set/ jipipe-launcher/src/test/resources/org/hkijena/jipipe/api/nodes/database/golden-set.json
git commit -m "feat(#1408): golden-set corpus from example workflows + registry dump"
```

### Task 2: Golden-set gate test (expected RED)

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchGoldenSetTest.java`

**Interfaces:**
- Consumes: `JIPipe.ensureInstance()`, `JIPipeNodeDatabase.getInstance()`, `new JIPipeEnhancedNodeDatabaseSearch(database)` (direct instance: pins the enhanced scorer, no settings dependency), `JIPipeNodeDatabasePipelineVisibility.Pipeline`, `JIPipeNodeDatabaseEntry.getId()`.
- Produces: `NodeSearchGoldenSetTest` with two tests, `goldenSet()` and `corpusIntegrity()`; dump property `jipipe.golden-set.dump` writes the failure worklist JSON (`{case, missing, actualTop10}` per failing case) before asserting - the `SearchDescriptionQualityTest` dump pattern.

- [ ] **Step 1: Write the test**

`goldenSet()`: load the corpus from the classpath resource, run each case, collect result ids via `getId()`, assert every `expect` id is within the first `expectWithin` ids and `first` (when set) equals id 0. Unknown ids in `expect` or `first` fail with the case id named. `corpusIntegrity()`: unique case ids, non-empty `expect`, `expectWithin >= 1`, per-tag minimum counts as pinned in Task 1 Step 3 (assert the exact numbers, they are the contract).

- [ ] **Step 2: Run and record the failure worklist as the fix backlog**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest -Djipipe.golden-set.dump=/tmp/opencode/golden-set-failures.json`
Expected: FAIL with a nonzero case count (the pre-fix state). Copy the dump to `utils/taxonomy-research/golden-set/baseline-failures.json` and commit it - it is the measured justification for Tasks 3-7.

- [ ] **Step 3: Commit**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchGoldenSetTest.java utils/taxonomy-research/golden-set/baseline-failures.json
git commit -m "feat(#1408): NodeSearchGoldenSetTest gate + baseline failure worklist"
```

### Task 3: Compound and hyphen bridging

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeSearchText.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`

**Interfaces:**
- Produces: `public static List<String> NodeSearchText.compoundJoinedTokens(String text)` - the lowercase joined forms of separator-containing compound words ("k-means" yields "kmeans"); splits on whitespace and camelCase only, contributes a form only when the group contains `-`, `_`, or `/`, never mutates existing method behavior.
- `CoverageScore coverageAgainst(QueryRep q, List<String> candTokens, List<String> candStems, List<String> candJoined, Idf idf)` gains the fourth list; `QueryRep` gains adjacent-bigram concatenations (`parts[i] + parts[i+1]`, lowercase); `CandidateView` gains per-field joined-token lists (name, tokens, extra) from `compoundJoinedTokens`.

- [ ] **Step 1: Identify the bridging cases in the baseline dump** (`grep -l kmeans`-style over `baseline-failures.json`); they are the acceptance set.

- [ ] **Step 2: Implement** `compoundJoinedTokens` and thread the joined lists through `CandidateView.from` (additive: `covName`/`covTokens`/`covExtra` each also compare against their field's joined list).

- [ ] **Step 3: Ladder rung** in `coverageAgainst`, after the prefix rung and before the stem rung: joined exact (`candJoined.contains(qt)`) grades 0.92; bigram concatenation of the query (`j = q.parts[i] + q.parts[i+1]`) matched against `candTokens` or `candJoined` grades 0.92 and covers both i and i+1 (skip i+1 on the next iteration). Ladder order becomes: exact 1.00 > prefix 0.92 > joined 0.92 > stem 0.88 > substring 0.80 > strong fuzzy > weak fuzzy.

- [ ] **Step 4: Verify**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest`
Expected: the Task 3 cases flip green; no previously green case regresses.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeSearchText.java jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java
git commit -m "feat(#1408): compound and hyphen bridging in the coverage ladder"
```

### Task 4: Typo tier

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`

**Interfaces:**
- Produces: constants `MIN_WEAK_FUZZY_SIM = 0.78`, `WEAK_FUZZY_SCORE = 0.62`; a weak-fuzzy rung after the strong-fuzzy rung: `JW.compare(qt, tok) >= 0.78` with BOTH token lengths >= 5 grades a flat 0.62. `MIN_FUZZY_SIM` (0.85) and its gradient stay untouched.

- [ ] **Step 1: Verify the typo cases in the baseline dump are all below the strong tier** (they must be; a case above 0.85 already passing belongs in `review.md` as covered-anyway).

- [ ] **Step 2: Implement the weak rung** in the fuzzy branch of `coverageAgainst` (the single `else` after the substring rung: compute max JW once, grade from the two bands).

- [ ] **Step 3: Verify** the Task 4 cases flip green; the recall floor (`MIN_COVERAGE = 0.60`) keeps pruning: a weak-only single-token query sits at coverage 0.62 and survives - verify one such case in the dump output.

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest`

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java
git commit -m "feat(#1408): weak fuzzy tier for typos (JW 0.78, length guard 5)"
```

### Task 5: Substring and reverse-prefix rung

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`

**Interfaces:**
- Produces: the substring rung condition widens to `tok.contains(qt) || (qt.startsWith(tok) && qt.length() >= 3)` - the query-longer prefix direction the gate's `coveredBy` already accepts but the scorer's ladder did not. Grade stays 0.80. The 3-char guard keeps 1-2 char haystack tokens from prefix-matching everything.

- [ ] **Step 1: Verify the substring cases in the baseline dump fail through the missing reverse direction** (query token longer than every candidate token). Cases failing for other reasons stay for Task 3/4.

- [ ] **Step 2: Implement** the widened condition in the substring branch.

- [ ] **Step 3: Verify**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest`
Expected: Task 5 cases green, no regressions.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java
git commit -m "feat(#1408): reverse-prefix acceptance in the substring rung"
```

### Task 6: Dimensionality terms

**Files:**
- Modify: `jipipe-core/src/main/resources/org/hkijena/jipipe/search/phrases.txt`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`

**Interfaces:**
- Produces: phrase-canon variants appended to the existing canonical groups (keep the file's alphabetical order): the `2d` group gains `two dimensional`, `2 dimensional`; the `3d` group gains `three dimensional`, `3 dimensional`; a `4d` group (`4d, 4 d, 4-d, four dimensional, 4 dimensional`) if absent; the `5d` group gains `five dimensional`, `5 dimensional`. No `1d` group (no measured demand, YAGNI).
- Produces: `CandidateView` gains `Set<String> spatialDimensions` - the canonical lowercase ids of the node's declared `JIPipeNodeTaxonomySpatialDimension` set, resolved through the node type registry from `entry.getOriginNodeId()`; empty when the entry wraps no node type or declares none. Query-side mapping (post-canonicalization token -> spatial dim): `2d`->2D, `2d+t`->2D, `3d`->3D, `3d+t`->3D, `4d`->3D, `5d`->3D; `1d` maps to nothing.
- Produces: dimension bias in the coverage-scored path (after `fused` is computed, alongside `typeBias`): when the query carries a mapped dimension D - declared D: `finalScore -= 0.03` (constant `DIMENSION_MATCH_BONUS`); declares spatial dimensions but not D: `finalScore += 0.10` (constant `DIMENSION_MISMATCH_PENALTY`); declares nothing: no bias. The dimension token still scores normally in the coverage ladder.

- [ ] **Step 1: Verify what already works** - the corpus's plain `2d`/`3d` exact-token cases and the `3 d`/`3-d` compound cases (phrase canon covers those already). Only the failing dimensionality cases are acceptance set: word forms and constraint queries.

- [ ] **Step 2: Add the phrases.txt variants** (alphabetical position, comma-separated, canonical first).

- [ ] **Step 3: Implement the spatial-dimension facet and bias** (resolve the enum's id strings once per candidate view; compare against the mapped query dimension).

- [ ] **Step 4: Verify**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest`
Expected: all dimensionality cases green, no regressions.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/resources/org/hkijena/jipipe/search/phrases.txt jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java
git commit -m "feat(#1408): dimensionality word forms + taxonomy spatial-dimension bias"
```

### Task 7: Simplicity preference (name-exact bonus)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java`

**Interfaces:**
- Produces: constant `NAME_EXACT_BONUS = 0.05`; after the field blend and before the recall floor: when every query token appears as an exact token of the candidate's name (`v.nameTokens.containsAll(Q.parts)`), `coverage = Math.min(1.0, coverage + NAME_EXACT_BONUS)`. Exact-name and name-prefix ladder entries are unaffected (they skip scoring).

- [ ] **Step 1: Implement** the bonus at the pinned spot in `internalQuery`.

- [ ] **Step 2: Verify**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest`
Expected: the `ordering` cases' `first` assertions pass; full gate green.

- [ ] **Step 3: Full gate + description gate green**

Run: `mvn -pl jipipe-launcher -am test -o -Dtest=NodeSearchGoldenSetTest,SearchDescriptionQualityTest`
Expected: PASS (2/2 test classes).

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java
git commit -m "feat(#1408): name-exact bonus for simplicity preference"
```

### Task 8: Full verification, ledger, MR

- [ ] **Step 1: Core suite** (background, ~10 min): `mvn -pl jipipe-core clean test -o` - Expected: 0 failures, 0 errors.

- [ ] **Step 2: Launcher suite**: `mvn -pl jipipe-launcher -am test -o` - Expected: 0 failures.

- [ ] **Step 3: Deferred-minors sweep**: resolve or ledger every minor found during fix rounds in `.superpowers/sdd/2026-11-25-golden-set-lexicon-mr-c/progress.md`; nothing unrecorded.

- [ ] **Step 4: Push and open MR**

```bash
git push -u origin 1408-golden-set-lexicon
```
MR via `save_merge_request`: title `feat(#1408): golden-set gate + lexicon search fixes`, target `master`, description referencing #1408 and #1323, `remove_source_branch: true`, link the session to it.

- [ ] **Step 5: Post a completion note** on #1408 (MR reference, gate counts, corpus size per tag).
