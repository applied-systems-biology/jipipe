# AI-Assisted Merged Ranking + Node Search Settings (MR D, #1409) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One blended node-search result list by default (lexicon + embedding, usage prior on both), the dedicated AI-search toggle removed everywhere, and a new "Node search" settings page as the single place configuring the search system.

**Architecture:** `JIPipeNodeDatabase.query` (the pinned-ids overload) fuses the configured classic search's per-entry scores with the AI search's per-entry cosine similarities through a pure, unit-testable ranking function in a new class; when AI is disabled, unavailable, or the classic implementation is Legacy, the classic result is returned unchanged (graceful degradation). The flag `FLAG_USE_AI_SEARCH` and both UI toggle buttons are removed. The embedding entry text is restructured (taxonomy function path, condensed slots, token bound) and re-keys the vector caches through the existing content-hash invalidation. An assumption-gated harness test reports golden-set hit rates through the merged path with and without the AI contribution.

**Tech Stack:** Java 21, JUnit 5 (jipipe-launcher test module), Swing (toggle removal only - no new UI tests per policy), Maven offline builds.

**Spec:** `docs/superpowers/specs/2026-11-25-node-search-1323-design.md` (wave 2 section; rulings of 2026-11-25: merged ranking default, "Node search" settings page in the general category, bundled embedding model).

## Global Constraints

- Branch `1409-ai-merged-ranking` (from origin/master AFTER !219 merges); every commit message references `#1409`.
- No em dashes or en dashes anywhere (code, comments, docs, strings).
- The golden-set gate (`NodeSearchGoldenSetTest`, 94/94) and `SearchDescriptionQualityTest` must stay green on every task: the gate pins the enhanced scorer via a direct `new JIPipeEnhancedNodeDatabaseSearch(database)` instance, so merged-ranking and settings changes must not alter the scorer's default behavior.
- `NodeSearchText.java` is NOT touched in this MR (no tokenizer changes).
- No Swing interaction tests (test policy 2026-10-20); toggle-button removal is verified by compilation + the user's eye.
- Builds run offline: `mvn -o`; launcher-suite runs need `clean` (stale probe-class hazard, MR C discovery); a `-pl jipipe-launcher` run without `-am` silently resolves a stale jipipe-core jar from ~/.m2.
- The AI search blocks its caller up to 300 s while the model works (existing `SEARCH_TIMEOUT_SECONDS` behavior, inherited unchanged).
- Deferred minors go to `.superpowers/sdd/2026-11-26-ai-merged-ranking-mr-d/progress.md`.

## Review Focus

1. **Golden-gate blindness to the live path.** The gate tests the enhanced scorer directly; the merged path could break ranking for real users while the gate stays green. Pin: the harness (Task 7) runs the corpus through the merged path when the model is present; the fusion function itself is unit-tested (Task 5) on synthetic inputs including the degradation arms.
2. **Graceful degradation.** A reasonable person expects: AI disabled in settings, model absent, model failed, or AI query timeout, and the browser still works with the classic ranking alone, no empty results, no exception. Pin: Task 5's unit tests cover the no-AI / null-AI-result arms; Task 6's UI sweep confirms no toggle remains that could route around it.
3. **Settings migration.** The `search-implementation-v2` value moves sheets (`org.hkijena.jipipe:general-ui` to the new sheet id). A user who chose Legacy must keep Legacy after the update. Pin: Task 1's compat read + its test.
4. **Embedding text re-key.** Changing `entryToText` must not strand stale vectors: every consumer verifies the text hash (`getVerifiedEmbedding`), so changed text recomputes JIT. A regression here silently serves outdated similarity. Pin: Task 6 verifies both tiers' verify paths take the rendered text as the key (code inspection + harness numbers move only from recompute, not staleness).
5. **Usage prior correctness.** `NodeUsage` keys by origin node id; the AI search today keys its private map by namespaced entry id. Fusing with mismatched keys makes the prior inert for AI-ranked entries. Pin: Task 3's test records and reads through origin ids.

---

### Task 1: "Node search" settings page

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeNodeSearchApplicationSettings.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/StandardSettingsPlugin.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettings.java` (remove the searchImplementation field, its `@JIPipeParameter("search-implementation-v2")` accessors, and its import)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeNodeDatabase.java:459-464` (`getSearchImplementation()` reads the new sheet)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchSettingsMigrationTest.java`

**Interfaces:**
- Produces: `JIPipeNodeSearchApplicationSettings` with `ID = "org.hkijena.jipipe:node-search"`, `getDefaultCategory() = JIPipeDefaultApplicationSettingsSheetCategory.General`, `getInstance() = JIPipe.getSettings().getById(ID, JIPipeNodeSearchApplicationSettings.class)`, and:
  - `JIPipeNodeDatabaseSearchImplementation getSearchImplementation()` / setter, parameter key `search-implementation-v2`, default `Enhanced`, documentation "The method responsible for handling the node search" (moved verbatim from JIPipeGeneralUIApplicationSettings).
  - `boolean isAiContribution()` / setter, parameter key `ai-contribution`, default `true`, documentation: "If enabled, the node search blends the classic ranking with the AI (embedding) contribution when the AI model is available. Disable to always use the classic ranking alone."
  - `boolean isTypoTolerance()` / setter, parameter key `typo-tolerance`, default `true`, documentation: "If enabled, the node search tolerates typos in query words (fuzzy matching). Disable for strictly literal matching."
  - Compat migration in the constructor: if the raw settings node `org.hkijena.jipipe:general-ui` → `search-implementation-v2` is present (pattern of `JIPipeGeneralUIApplicationSettings.isReducedMotionFromRaw()`, via `JIPipeApplicationSettingsServiceComponent.getRawSheetsNode()`), and the new sheet's own stored value is absent, adopt the old value (Legacy stays Legacy).
  - (The usage-reset button is NOT in this task - it lands in Task 3 next to the `NodeUsage.resetUserCounts()` it calls.)
- Modifies: `JIPipeNodeDatabase.getSearchImplementation()` returns `JIPipeNodeSearchApplicationSettings.getInstance().getSearchImplementation()` when `JIPipe.isInstantiated()`, else `Legacy` (unchanged fallback).

- [ ] **Step 1: Write the failing migration test**

`NodeSearchSettingsMigrationTest`: with `JIPipe.ensureInstance()`, assert `JIPipeNodeSearchApplicationSettings.getInstance()` resolves (non-null), `isAiContribution()` defaults true, `isTypoTolerance()` defaults true, `getSearchImplementation()` defaults `Enhanced`; assert `JIPipeGeneralUIApplicationSettings.getInstance()` no longer exposes a search-implementation parameter (`getInstanceServiceComponent` parameter check or reflection: no field `searchImplementation`); assert `JIPipeNodeDatabase.getInstance().getSearchImplementation()` does not throw and returns a non-null implementation.

- [ ] **Step 2: Run it** (`mvn -pl jipipe-launcher -am clean test -o -Dtest=NodeSearchSettingsMigrationTest -Dsurefire.failIfNoSpecifiedTests=false`): FAIL (class does not exist).

- [ ] **Step 3: Implement** the sheet, the reset parameter type + editor UI (button labeled "Reset usage data", tooltip "Clears the recorded personal node usage counts; the shipped community prior stays active", calls `NodeUsage.resetUserCounts()`), registration in `StandardSettingsPlugin.register()` (`registerApplicationSettingsSheet(new JIPipeNodeSearchApplicationSettings())` + `registerParameterEditor(NodeUsageResetParameter.class, NodeUsageResetDesktopParameterEditorUI.class)`), the compat read, and the `JIPipeNodeDatabase.getSearchImplementation()` rewiring. Follow the annotation pattern of `MCPApplicationSettings` and the editor-registration pattern of `AutomationPlugin` (line 67).

- [ ] **Step 4: Run it**: PASS. Also run `NodeSearchGoldenSetTest,SearchDescriptionQualityTest` (same flags): 94/94 + green (the gate reads `getSearchImplementation` indirectly through `getSearch()` - defaults unchanged).

- [ ] **Step 5: Commit** `feat(#1409): Node search settings page (AI contribution, typo tolerance, usage reset, migrated implementation)`

### Task 2: Typo tolerance wiring

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java` (weak fuzzy band only)

**Interfaces:**
- Produces: private helper `private static boolean isTypoToleranceEnabled()` - `JIPipe.isInstantiated()` ? `JIPipeNodeSearchApplicationSettings.getInstance().isTypoTolerance()` : true (fail-open: tests and headless runs keep current behavior).
- Behavior: in the weak-band condition (`JW >= MIN_WEAK_FUZZY_SIM`, flat `WEAK_FUZZY_SCORE`), when `!isTypoToleranceEnabled()` no weak match fires. The strong band (`MIN_FUZZY_SIM` 0.85 gradient) is untouched and always active.

- [ ] **Step 1: Implement** the helper + condition (one guard clause in the weak band).

- [ ] **Step 2: Verify** `NodeSearchGoldenSetTest,SearchDescriptionQualityTest`: 94/94 + green (default on). No new test (a test asserting "set false, assert weaker" would restate the guard; the gate pins the default-on path, and the setting is user-visible).

- [ ] **Step 3: Commit** `feat(#1409): typo tolerance setting gates the weak fuzzy band`

### Task 3: Shared usage prior in the AI search + reset

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/NodeUsage.java` (add reset)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeAINodeDatabaseSearch.java` (switch to NodeUsage)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/NodeUsageResetParameter.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/NodeUsageResetDesktopParameterEditorUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeNodeSearchApplicationSettings.java` (add the reset button parameter) + `StandardSettingsPlugin.java` (register its editor)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeUsageResetTest.java`

**Interfaces:**
- Produces: `NodeUsage.resetUserCounts()` - clears the user-count map and persists the empty map (use the existing persister); documented as the settings-page reset target.
- Modifies: `JIPipeAINodeDatabaseSearch` - delete `USAGE_COUNTS` and its map writes; `updateUsageCount(userSelected)` records via `NodeUsage.record(nodeId)` where `nodeId = userSelected.getOriginNodeId() != null ? userSelected.getOriginNodeId() : userSelected.getId()` (the prior is keyed by origin node ids); both sort comparators read `NodeUsage.usage(...)` with the same id derivation.

- [ ] **Step 1: Write the failing test**: `NodeUsageResetTest` (jipipe-core) - with the injectable persister/supplier seam (`setUserCountsPersister`, `setPriorSupplier`): record two ids, assert `usage(id) == prior + recorded`, call `resetUserCounts()`, assert recorded part is 0 while the prior part survives; restore the injectables in a finally block.

- [ ] **Step 2: Run it**: FAIL (no `resetUserCounts`).

- [ ] **Step 3: Implement** `resetUserCounts` + the AI search rewiring + the settings-page reset button (parameter `usage-reset` of type `NodeUsageResetParameter` on the Node search sheet, custom editor `NodeUsageResetDesktopParameterEditorUI` with a button labeled "Reset usage data", tooltip "Clears the recorded personal node usage counts; the shipped community prior stays active", calling `NodeUsage.resetUserCounts()`; register via `registerParameterEditor` in `StandardSettingsPlugin.register()`, the pattern of `AutomationPlugin` line 67).

- [ ] **Step 4: Run it**: PASS. Plus `NodeSearchGoldenSetTest,SearchDescriptionQualityTest`: 94/94 + green.

- [ ] **Step 5: Commit** `feat(#1409): AI search reads the shared usage prior; usage reset on the Node search page`

### Task 4: Score exposure on both searches

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeEnhancedNodeDatabaseSearch.java` (expose scores; behavior-neutral refactor)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeAINodeDatabaseSearch.java` (expose scores)

**Interfaces:**
- Produces (new file `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/ScoredNodeDatabaseEntry.java`): record `ScoredNodeDatabaseEntry(JIPipeNodeDatabaseEntry entry, double score, boolean pinned)` - the shared scored-result shape of both searches.
- Produces: `JIPipeEnhancedNodeDatabaseSearch` gains `public List<ScoredNodeDatabaseEntry> queryScored(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds)` - the internalQuery path with the per-entry `finalScore` retained (exact-name/name-prefix ladder entries keep their sentinel score, whatever `internalQuery` assigns them today; the scored list covers the same candidate set as `query()` returns, in the same order). `query()` becomes a thin map over `queryScored()` (order identical). The slot-type overload is NOT exposed scored (scope ruling below).
- Produces: `JIPipeAINodeDatabaseSearch` gains `public CompletableFuture<List<ScoredNodeDatabaseEntry>> queryScoredAsync(...)` over the same signature - `SearchRun` completes with `List<ScoredNodeDatabaseEntry>` (similarity after the AutoImport 0.85 / EdgeCase 0.7 classification penalties, exactly what it sorts by today); `query()` maps it. The internal `ScoredEntry` class is replaced by the shared record.

- [ ] **Step 1: Refactor** the enhanced search: keep every score computation; return scored; verify byte-identical behavior by the gate dump (below).
- [ ] **Step 2: Refactor** the AI search: `SearchRun` future type change; both `query()` overloads map identically to today.
- [ ] **Step 3: Verify**: `mvn -pl jipipe-launcher -am clean test -o -Dtest=NodeSearchGoldenSetTest,SearchDescriptionQualityTest -Dsurefire.failIfNoSpecifiedTests=false -Djipipe.golden-set.dump=/tmp/opencode/golden-set-t4d.json` - 94/94 + green, dump `failing: []`; core suite `mvn -pl jipipe-core clean test -o` green (the refactor touches the hot path).
- [ ] **Step 4: Commit** `refactor(#1409): expose per-entry scores from the enhanced and AI searches`

### Task 5: Merged ranking + flag and toggle removal

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeMergedNodeSearchRanking.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/JIPipeMergedNodeSearchRankingTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeNodeDatabase.java` (both query overloads; remove FLAG + containsFlag)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java` (remove toggle: fields at lines 162/189, toolbar block 493-511, `ReloadTask` flag arg 1738/1751)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java` (remove static AI_SEARCH line 95, button field 121, toolbar block 547-570, flag args 901/915)

**Interfaces:**
- Produces: `JIPipeMergedNodeSearchRanking` (final class, static methods, pure):
  - `public static boolean isAiContributionActive()` - true iff `JIPipe.isInstantiated()` && `JIPipeNodeSearchApplicationSettings.getInstance().isAiContribution()` && `AIApplicationSettings.getInstance().isEnableAI()` && `JIPipe.getInstance().getAiService().hasConfiguredAndReadyEmbeddingModel()` (exception-safe: any throw = false).
  - `public static List<JIPipeNodeDatabaseEntry> fuse(List<ScoredNodeDatabaseEntry> classic, List<ScoredNodeDatabaseEntry> ai, Set<String> pinnedIds)`:
    - Normalize each side by its own maximum positive score (`score / max`, max <= 0 → side contributes 0); map entries by identity (`JIPipeNodeDatabaseEntry` reference - both lists carry the same entry objects from the same database).
    - Combined = `FUSION_CLASSIC_WEIGHT * classicN + FUSION_AI_WEIGHT * aiN` with `FUSION_CLASSIC_WEIGHT = 0.7`, `FUSION_AI_WEIGHT = 0.3` (constants documented: a strong classic match cannot be outranked by AI alone; an AI-only semantic match beats a weak classic match). An entry present in only one list gets 0 for the missing side.
    - Sort: pinned first, then combined descending, then `NodeUsage.usage(originId, fallback entry id)` descending, then name (case-insensitive) - the comparator shape both searches use today.
    - Null or empty `ai` → return classic order unchanged (degradation arm).
  - `JIPipeNodeDatabase.query` (pinned-ids overload): `classic = getSearch().query(...)`; when `isAiContributionActive()` and `getSearchImplementation() == Enhanced`: `ai = aiSearch.query(...)` (null-safe), return `fuse(...)`. Else return classic. The slot-type overload (line 494-500) loses the AI branch entirely and always returns `getSearch().query(...)` (scope ruling: no caller passes the flag there today; type-distance dominates those queries).
  - `FLAG_USE_AI_SEARCH`, `containsFlag`, and the `aiSearch` branch are removed; `getAiSearch()` and `aiSearch.buildIndex()` in `buildIndex()` stay (the merged path and `BuildAIEmbeddingIndexTool` use them). The flag parameter stays in the interface signatures (callers stop passing it; the varargs is ignored).
- Unit tests (`JIPipeMergedNodeSearchRankingTest`, jipipe-core, pure - fake entries are NOT needed: `fuse` takes `ScoredNodeDatabaseEntry` which wraps real entries; construct minimal anonymous `JIPipeNodeDatabaseEntry` subclasses or use the test-side seam if entries are hard to build - in that case make `fuse` take `(List<ScoredNodeDatabaseEntry>, List<ScoredNodeDatabaseEntry>, Set<String>)` as specified and test with the lightest constructible entry kind):
  1. classic-only (null ai) preserves classic order exactly.
  2. strong classic match + strong ai match of the same entry stays first.
  3. AI-only semantic match (classic 0) outranks a weak classic match (classic 0.1 of max) but not a strong one (0.9 of max) - the 0.7/0.3 property.
  4. an entry in both lists with max-normalized equal scores beats an entry in only one.
  5. usage tiebreak: equal combined scores order by `NodeUsage.usage` (inject a prior supplier), then name.
  6. pinned entries come first regardless of scores.
  7. empty classic list → empty result; empty both → empty.

- [ ] **Step 1: Write the failing fusion tests** (the 7 cases above).
- [ ] **Step 2: Run them**: FAIL (class missing).
- [ ] **Step 3: Implement** `JIPipeMergedNodeSearchRanking` + the `JIPipeNodeDatabase` rewiring + both UI toggle removals.
- [ ] **Step 4: Run**: fusion tests PASS; `NodeSearchGoldenSetTest,SearchDescriptionQualityTest` 94/94 + green; full launcher reactor `-am clean test -o` green; core suite green.
- [ ] **Step 5: Commit** `feat(#1409): merged ranking by default; AI search toggle removed`

### Task 6: Embedded text restructuring

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/embeddings/JIPipeEmbeddingDatabase.java` (`entryToText` only)

**Interfaces:**
- Produces: `entryToText` renders, in order, with a 1800-character total bound (constant `MAX_EMBEDDING_TEXT_LENGTH = 1800`, rationale: bge_small_en's 512-token window is roughly 2000 English characters; the bound keeps the whole text embeddable):
  1. `Name: <name>` (unchanged, always first, never truncated).
  2. `Functions: <path1 > path2>` - the entry's `getFunctionIds()` resolved to display names via the node taxonomy function registry (`FunctionRegistry.byId(id)` through the nodes service component's `getFunctions()`, `JIPipeNodeTaxonomyFunction.getName()`; unresolved ids render raw). Omitted when empty or the registry is unavailable.
  3. `Description: <plain text>` (the outcome-first !217 descriptions are the bulk signal - already outcome-first, no rewrite).
  4. `Inputs: <slot (DataTypeName), ...>` / `Outputs: ...` - condensed: drop the per-slot `dataInfo.getDescription()`, keep `slotName (DataTypeName)`.
  - Truncation: apply to the description first (cut at the bound minus the other sections' lengths); never truncate name or functions.
- The change re-keys vector caches through the content-hash verification (`getVerifiedEmbedding(modelId, nodeId, entryText)`) - verify by code reading that BOTH the global tier and the local tier hash the rendered text (Task 6 verification includes this inspection, recorded in the report).

- [ ] **Step 1: Implement** the restructured `entryToText` (one method; helper for the functions line).
- [ ] **Step 2: Verify**: `NodeSearchGoldenSetTest,SearchDescriptionQualityTest` 94/94 + green (embedding text is outside both gates); compile + core suite green.
- [ ] **Step 3: Commit** `feat(#1409): restructure embedding entry text (function path, condensed slots, token bound)`

### Task 7: Merged-ranking evaluation harness

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/NodeSearchMergedRankingHarnessTest.java`

**Interfaces:**
- Consumes: the golden-set corpus (`golden-set.json`, 94 cases, JsonNode tree per the gate's loader), `JIPipeEnhancedNodeDatabaseSearch.queryScored`, `JIPipeAINodeDatabaseSearch.queryScoredAsync`, `JIPipeMergedNodeSearchRanking.fuse`.
- Produces: a report-only harness test `mergedRankingHarness()`:
  - Assumption-abort (`Assumptions.assumeTrue`) when `JIPipeMergedNodeSearchRanking.isAiContributionActive()` is false on this machine (no model) - the test then reports "skipped: model unavailable" in CI-safe silence.
  - When active: run all 94 cases through (a) classic-only scores and (b) `fuse(classic, ai, Set.of())`; write a JSON report via dump property `jipipe.merged-harness.dump` (`{perTag: {tag: {classicTop5, mergedTop5}}, cases: [{id, classicRank, mergedRank}]}`) and log a per-tag summary; assert NOTHING about the numbers (report-only per the spec: "Not a hard CI gate") except that the fused list is non-empty for every case that has a non-empty classic list (structural sanity).
- [ ] **Step 1: Implement** the harness.
- [ ] **Step 2: Verify**: run with the model if this machine has it configured (check `isAiContributionActive` in a probe); record per-tag numbers in the report. Without the model: verify the assumption-abort path (test skips, build green). Either way: `mvn -pl jipipe-launcher -am clean test -o -Dtest=NodeSearchMergedRankingHarnessTest -Dsurefire.failIfNoSpecifiedTests=false` green.
- [ ] **Step 3: Commit** `feat(#1409): merged-ranking evaluation harness over the golden set`

### Task 8: Full verification, ledger, MR

- [ ] **Step 1: Core suite** (background, ~10 min): `mvn -pl jipipe-core clean test -o` - 0 failures.
- [ ] **Step 2: Launcher suite**: `mvn -pl jipipe-launcher -am clean test -o` - 0 failures.
- [ ] **Step 3: Deferred-minors sweep** (ledger `.superpowers/sdd/2026-11-26-ai-merged-ranking-mr-d/progress.md`).
- [ ] **Step 4: Push and open MR** (`save_merge_request`: title `feat(#1409): merged node search ranking + Node search settings page`, target master, references #1409/#1323, `remove_source_branch: true`), link the session, completion note on #1409.

## Scope rulings (plan-level, recorded)

- The slot-type query overload keeps classic-only behavior (the removed flag had no caller there; type distance dominates those queries). Recorded as a conscious scope decision in the MR description.
- Fusion covers the pinned-ids overload only - the two surfaces the removed toggles served (node browser, node finder).
- `Legacy` implementation users get no AI contribution (the legacy search exposes no scores; Legacy is the explicit opt-out from the modern system).
- The final settings control set (per the design's "final set fixed in the MR D plan"): search implementation (moved here), AI contribution on/off, typo tolerance on/off, usage-data reset button.
- The design's line "the ai:embedding capability probe learns the bundle location so distributions report the capability as available without any download" ships with MR E (#1410): the probe can only learn a location the bundle (#1410) creates. Nothing in this MR touches the capability probe; recorded here so the wave-2 sentence is not a silent drop.
