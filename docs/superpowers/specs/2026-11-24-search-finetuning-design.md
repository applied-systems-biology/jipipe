# Search & description finetuning (#1404 follow-up)

Date: 2026-11-24
Status: draft for approval
Follows: MR !214 (branch `1404-search-ux-enhancements`) - the structural first round: single-pane find list, name-prefix ladder, multi-node system patterns hidden by default.

## Intent and why

Work item #1404, issue 2: "A deeper finetuning of both search and node
descriptions must be done." The first round made the ranking structurally
sound; this round makes it *usable in the long tail*, using the two signals
the maintainer pointed at:

1. **Usage** - which nodes people actually use. Today the ranking has zero
   usage signal: `confirmQuery` (the built-in usage hook) is never called
   by any UI, and the `-freq` sort key it feeds is not in the comparator.
   The real-world corpus at `/data/JIPipe/Example workflows/` (59 pipelines,
   10,189 node instances, 320 distinct types) is an ideal prior.
2. **Descriptions** - 796 of 1511 registered nodes (53%) have descriptions
   that never contain their own name's content words ("Convert 2D ROI to
   RGB" never says "convert"; "Split channels" never says "channels"). The
   blended field coverage structurally favors nodes whose descriptions
   repeat their names - the exact mechanism that made "IJ3D Find particles
   3D" outrank "Find particles 2D" in the first round.

Decisions already made with the maintainer (2026-11-24):

- Usage sources: **both** - a shipped prior mined from the corpus **and**
  runtime-recorded per-user counts.
- Usage effect: **small bounded score bonus** (never overrides text-match
  quality).
- Description scope: **all failing nodes** (not only the top-used ones).

## Goals

- G1: A committed, reproducible usage prior: generator script + generated
  resource, mined from the real-world corpus.
- G2: Runtime usage recording: inserting a node (node browser, node finder
  dialog) increments a per-user counter, persisted in the settings registry.
- G3: Usage enters the enhanced ranking as a small bounded bonus; the
  comparator tie-breaks by usage after the primary score.
- G4: The coverage-bucket scale no longer punishes a full name+token match
  for a thin description (rescale, not threshold change).
- G5: Every registered node's description contains its own name's content
  words, verified by a registry-wide gate test.
- G6: All changes are offline-safe, deterministic, and test-pinned.

## Non-goals

- No changes to the AI (embedding) search.
- No changes to the legacy search implementation.
- No per-query learning beyond usage counts (no click models, no
  cross-user telemetry - counts stay local to this installation).
- No restructuring of the node database builder.

## Anti-goals (failure modes)

- Usage must never bury a well-named obscure node below a badly-named
  popular one when the text match clearly favors the former (bounded bonus
  small enough for that).
- Generated descriptions must read naturally (no "This node converts..."
  boilerplate stamped onto 800 nodes verbatim); they are documentation
  first, search fodder second.
- The usage prior must not encode anything beyond node-type id counts
  (no user-identifying data - it is derived from the maintainers' local
  example corpus).
- **Ruling (2026-11-24, Task 2):** canvas notes, comments, groups, and
  compartment I/O are real registered node types (with descriptions) and
  STAY in the prior; the builder-check premise that they "carry no
  description" was factually wrong, and excluding them would need a
  hard-coded blocklist against the project's no-node-id-heuristics style.

## Constraints

- The example-workflow corpus lives outside the repository
  (`/data/JIPipe/Example workflows/`); the generator takes its path as an
  argument and the *generated resource* is committed.
- Registered IDs are stable string literals (project rule) - the prior and
  the recorded counts key on those ids; unknown ids (plugins removed,
  renamed) are ignored at load.
- The gate test runs in jipipe-launcher (only module with the full
  registry), mirroring `TaxonomyFullValidationTest`.
- No em/en dashes anywhere (project rule).

## Approaches (considered and chosen)

### Usage prior source

1. **Shipped prior + runtime recording (chosen)** - the prior makes every
   fresh install instantly better; runtime counts personalize on top.
2. Shipped prior only - rejected: loses the personalization the maintainer
   explicitly chose.
3. Runtime only - rejected: cold-start useless; the corpus prior is the
   single highest-value data source available.

### Usage effect on ranking

1. **Small bounded, saturating bonus (chosen)** - `bonus = W * f/(f+K)`
   subtracted from the fused score, `W = 0.05`, `K = 10` (half weight at 10
   uses). Flips near-ties ("2D" variant used 115x vs an unused "3D"
   variant) while never overriding coverage/ladder differences that matter.
2. Tie-break only - rejected: too weak to matter (identical scores are
   rare with float scoring).
3. Strong boost / popularity buckets - rejected: buries obscure-but-well-
   named nodes; anti-goal.

**Ladder interaction (builder check):** the name-match ladder is a
text-quality tier and stays strictly above usage. The bonus applies ONLY
inside the coverage-scored tier (tier 2 of the comparator); exact and
name-prefix entries keep their sentinel scores and order (exact by usage
via the existing `-freq` sort key, prefix by name length then usage).
Usage can never promote an entry across ladder tiers. The "2D used 115x
vs unused 3D" example applies to non-prefix near-ties (e.g. query
"particles"), which is the common case; a prefix query is already won by
the ladder regardless of usage. Tests assert the relative-order property
with injected synthetic priors (see "Prior injection").

### Persistence of runtime counts

1. **Settings registry (chosen)** - same mechanism as the pinned node ids
   and the node-browser kinds (one JSON registry location, loaded lazily,
   written on change). No new file format.
2. Own file in the JIPipe directory - rejected: a second persistence
   mechanism for one map.

**Storage shape (builder check):** a static, lazily loaded in-memory map
(the `NodeEntryKind` pattern - no `SettingsBinding`, which would leak a
path listener per record call); records write through with the settings
service's debounced `saveLater()`. Recorded on any thread, so the map is
a concurrent map; the registry round-trip is the whole map
(`putIntoRegistry` with a `Map<String, Long>`, read tolerant of missing
or corrupt entries, which reset the map to empty).

### What counts as an insertion (builder check)

Recorded (a deliberate "add new node(s) from the search" choice):
`JIPipeDesktopNodeBrowser.insertAtCursor` (double-click, Enter, context
menu, follow-up strip all funnel there), the node finder dialog's insert
actions, and the drag-and-drop drop path
(`JIPipeDesktopAddNodeTransferHandler`). Not recorded: paste, duplicate,
undo, project load (no search choice expressed).

Semantics: for a plain node entry, one increment of its origin node id.
For templates/patterns and examples, one increment per inserted node
instance's info id (the signal is "which node types people actually
use"; a 3-node pattern records its 3 member types).

**Recording path (builder check):** the UIs call `NodeUsage.record`
directly - NOT via `database.confirmQuery`, which dispatches to whatever
search implementation is active (Legacy: a no-op; AI: its own separate
in-memory map). Recording must not depend on the search implementation;
`confirmQuery` stays untouched. The prior keys only REAL node types:
the generator filters corpus ids against the live registry dump, dropping
ids that are not registered node types at all. Canvas notes, comments,
groups, and compartment I/O ARE registered node types (with descriptions,
searchable and insertable) and STAY in the prior (Ruling, Task 2 - the
builder-check premise that they are "infrastructure, not searchable node
types" was factually wrong). The corpus directory is a generator argument
(never hard-coded); the resource's README records corpus provenance
(file count + content hash of the contributing directory), so CI never
needs the corpus.

### Combining prior + user counts (builder check)

Both are "times used" and add: `effective = prior.instances + userCount`.
The prior uses **instances** (not pipelines) - the same unit the runtime
counter accumulates. A personal count on a never-priored node yields a
small bonus (5 uses -> about 0.017); on a saturated popular node it adds
almost nothing - accepted, popular nodes are already ranked well.

### Description sweep shape

1. **All failing nodes, batched by plugin, spec-driven (chosen)** - a
   JSON patch spec (node id -> new opening sentence) is authored per batch,
   reviewed, then applied by script (the only sane way to edit ~100 files
   without manual churn); every edit preserves the existing description
   body and only rewrites/prepends the opening sentence so the node's own
   words appear naturally.
2. Top-used only - rejected by the maintainer.
3. Mechanical template stamping - rejected (anti-goal: boilerplate).

**Reuse the existing audit tooling (builder check):** the sweep does NOT
write a fresh apply script. `utils/audit-node-descriptions.py` already
implements reviewed-proposal application (splice into
`@SetJIPipeDocumentation`, Java string escaping, empty-description fill,
round-trip verification via `utils/extract-node-descriptions.py`) - the
sweep authors its proposals in that format and applies with that script.
`utils/extract-node-descriptions.py` (source-based) is the analysis
source of truth for names/descriptions; the launcher-side registry dump
is used only to cross-check derived names.

**Scope facts (measured 2026-11-24, post-MR-A; user decision A):** the
fresh gate count is 780 failing + 1 empty (`annotation-table-from-file`).
They split into 397 JIPipe-authored nodes (+ the empty one, + 2 nodes
resolving their description from a resource URL, fixed in their resource
files by hand) and 381 auto-imported nodes (282 ImageJ2 ops `ij2:op:*`
whose third-party metadata carries no description, 99 CLIJ kernels
`clij:plugin:*` whose text comes from CLIJ's own documentation, and the
dynamically-registered TrackMate factory wrappers
`trackmate-create-spot-{tracker,detector}-*` whose text comes from
TrackMate's own `getInfoText()` - identified during the sweep's Final
Task). The
auto-imported families are NOT editable by annotation splicing - the
earlier "the sweep covers all failures" premise was wrong for them
(user decision A, 2026-11-24): the gate structurally scopes to
JIPipe-authored nodes (ids under the two auto-import families are out of
the gate by rule, not by an exemption list), and the sweep fixes the
authored set. The search already ranks auto-imported nodes via name and
taxonomy tokens; a missing description echo only lowers their ceiling
slightly. Gate body = the description the
search sees (`info.getDescription().toPlainText()`); the tokenizer and
containment rule are the shared Java helper (`NodeSearchText.contentWords`
+ `coveredBy`), so gate and search agree. Gate corpus ordering uses the
usage-prior resource.

### Coverage-bucket scale

1. **Rescale by the name+token maximum (chosen)** - bucket on
   `coverage / 0.95` (0.7 + 0.25 field weights), with the bucket-0
   boundary at the maximum itself (rescaled >= 1.0): a node matching the
   query fully in name+tokens lands in the top bucket whether or not its
   description repeats the words (Ruling, Task 4: the design's earlier
   "0.98" boundary would leave such nodes in bucket 1).
   One line; keeps the floor and all other thresholds.
2. Lower the top-bucket threshold to 0.95 - rejected: conflates "full
   name+token match" with "extra-field also matched" (a description repeat
   would still outrank, just inside the same bucket).

## Components

1. **Generator** `utils/search-usage/generate-usage-prior.py` (+ README):
   reads the corpus directory (argument), counts per node-info id
   (instances + distinct pipelines), writes the resource JSON with sorted
   keys and a generated-at stamp. Deterministic byte-stable output.
2. **Resource** `jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json`:
   `{"source": "...", "generated": "...", "counts": {"<node-id>": {"instances": N, "pipelines": M}}}`.
   Single-source ship set: a test pins format, sortedness, and a few
   entries exactly (e.g. `ij1-threshold-auto2d`, `ij1-analyze-find-particles2d`).
3. **Usage service** (new, `api/nodes/database/NodeUsage.java`, small):
   - static access to `usage(nodeId)` = prior + persisted user count;
   - `record(nodeId)` increments the user count and writes the registry
     (debounced or on-save, matching the settings service patterns);
   - unknown ids are ignored; corrupted values reset to empty.
4. **Search changes** (`JIPipeEnhancedNodeDatabaseSearch`):
   - usage bonus per G3/approach above; comparator tie-break by usage after
     the primary score;
   - bucket rescale per approach above.
5. **UI wiring**: the node browser's `insertAtCursor` and the node finder
   dialog's two insert actions call `NodeUsage.record` DIRECTLY (builder
   check finding #4: `confirmQuery` is never called by any UI and would
   dispatch to whichever search implementation is active) for each
   inserted node's id (template member ids, one increment each).
6. **Description sweep**: per failing node, an authored opening sentence
   containing the missing name words; applied by script from the reviewed
   spec; batches ordered by corpus usage, then plugin. The analysis dump
   uses the live registry (jipipe-launcher), same as the gate.
7. **Gate test** `SearchDescriptionQualityTest` (jipipe-launcher): every
   registered node's description must contain its name's content words
   (same tokenizer as the search: word split + camelCase + stopwords).
   Ships green after the sweep; stays as the regression gate.

## Delivery order

1. **MR A (ranking + usage)**: components 1-5 + tests. Independent of the
   sweep; immediately useful.
2. **MR B (description sweep)**: components 6-7, batched (usage-ordered),
   possibly several waves in one MR.

## What is left to the builder

- Exact bonus constant tuning (bounded by the approach's cap) with a test
  pinning the ordering property, not the magic number.
- The stopword list (single source: a small Java helper in
  `api/nodes/database`, mirrored in the Python generator and the gate
  test).
- Description edit wording (the maintainer reviews the spec batches).

**Prior injection (builder check):** `NodeUsage` reads the prior through
an injectable supplier - production wires the classpath resource, tests
inject synthetic maps. Ranking/ordering tests (jipipe-core, next to
`EnhancedNodeDatabaseSearchRankingTest`) therefore assert relative order
under synthetic priors and never depend on the corpus; a separate small
test pins the SHIPPED resource's format, sortedness, and a few entries
exactly (it is a single-source ship set), reading only the resource.
