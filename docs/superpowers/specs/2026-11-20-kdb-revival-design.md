# KDB Revival: Structured Corpus and Retrieval Service - Design Spec

Date: 2026-11-20
Status: Draft (approved approach: B, pending document review)
Charter: #1383 charter addition "KDB revival decision", resolved 2026-11-20: revive as a redesign.
Tracking child work item: #1394.

## 1. Intent and why

The JIPipe Knowledge Database (KDB) is the project's curated domain canon for AI agents. A first
version exists on the unmerged branch `1294-image-analysis-kdb` (46 markdown documents plus a
cosine-only search service over markdown chunks). That branch was never merged and is planned to
die; this spec replaces it with a structured redesign, mining the old branch for content, not code.

Three reasons to revive it now:

1. **Small models need it.** The JIPipe-specific agent harness (handoff project, new team member)
   targets tool-calling models of the Qwen 3.5 4B class with a domain-knowledge calibration of
   0.38 (2B) to 0.86 (4B) on JIPipe material. Without a KDB, these models hallucinate node names,
   parameters, and image-analysis procedures. The harness spec (`1290-ai-llm-integration`, also
   planned to die, also mined not merged) already prescribes the consumer contract:
   `search_knowledge`, `get_knowledge_entry`, `list_knowledge_domains`, plus a calibration-scaled
   system-prompt nudge.
2. **The oikb lesson.** The institute's OpenWebUI knowledge base (oikb) experience showed that
   naive similarity or text lookup fails on larger and related items: retrieving one relevant page
   does not ensure the agent finds the page's linked passage or the related entry. Embeddings alone
   do not fix that; the retrieval interface must make next steps explicit.
3. **Markdown rots.** A hand-written prose corpus referencing nodes by name drifts silently: node
   ids get deprecated, hidden, or replaced, and nothing fails when it does.

External agents (large models over MCP) are the second consumer class; the same service feeds both.

## 2. Goals, non-goals, anti-goals

### Goals

1. A structured, hand-authored corpus as repository data files: one YAML file per entry,
   reviewable in merge requests, versioned in git. Git history IS the change ledger.
2. Deterministic retrieval: lexical, vector, and relation lanes fused with reciprocal rank fusion
   and a deterministic re-rank. The only ML in the ranking path is the embedding lane.
3. Node facts are never duplicated into the corpus. Entries reference stable node registration
   ids; display names, deprecation state, successors, replacements, and empirical workflow relations
   (`previous`/`next`/`similar`) join from the live registry and `JIPipeNodeDatabase` at render time.
   Node lifecycle facts are registry-sourced, which requires the registry to actually carry them:
   this project adds a successor API to `JIPipeNodeInfo` (5.3.1), and that API also drives
   user-facing auto-generated node documentation ("Use that instead").
4. Verification in `jipipe-launcher` tests: referential integrity, schema conformance, a closed
   finding-code list, coverage warnings, and golden relevance tests.
5. A protocol-neutral Java service consumed by the harness LLM functions AND exposed as read-only
   MCP automation operations for external agents.
6. Rendering at the boundary: agent-facing text is assembled from entries plus live registries,
   with resolved node names, deprecation badges, relation evidence, and explicit next-step pointers.

### Non-goals

- Claims machinery (append-only claims with validity windows, runtime contradiction scanner):
  StoryForge carries this for multi-editor concurrent fiction; the KDB corpus changes only through
  reviewed merge requests, so git history already is the claim ledger. Version qualifiers exist as
  plain optional entry fields, not per-claim windows.
- PostgreSQL, graph databases, external services, an embedding server at runtime: the corpus is
  hundreds of entries, the app is an offline desktop application; in-memory indexes and the
  existing JEMB machinery suffice.
- Runtime corpus mutation by agents. (If ever added later: suggestion-never-mutation.)
- A corpus-side tombstone ledger: node existence and successors are registry facts (ruling
  2026-11-20).
- A KDB browser UI (future work, ledgered out).
- SCIP or compiler-aware code indexing. Node knowledge comes from the registry, not source scans.
- Autonomous multi-hop research loops (`trace_question`): traversal is bounded to one hop in the
  service; anything longer is the harness's job, not the KDB's.

### Anti-goals (failure even if everything technically works)

- A corpus that can drift from the registry silently (the markdown rot this design exists to kill).
- Retrieval that returns raw chunk text with no provenance, no relations, and no next steps
  (the oikb failure mode).
- Node display names, parameter lists, or slot signatures stored inside corpus entries
  (double-bookkeeping against the registry).

## 3. Constraints

- Java 21, Maven, offline at runtime.
- Dependencies already present: `jackson-dataformat-yaml` + snakeyaml (currently transitive via
  commons-text; jipipe-core must declare it explicitly), JEMB embedding database,
  `JIPipeAIServiceComponent.embedNow/tryEmbed` (bge-small-class embedding model, 512-token window).
  No Lucene and none needed (hand-rolled inverted index + BM25 over hundreds of entries).
- House rules: no em/en dashes anywhere; registered ids are stable string literals; the test
  policy applies (no trivial tests; single-source ship sets may pin exact counts, and the corpus
  is one, but coverage is a warning, not a gate).
- `jipipe-launcher` depends on jipipe-core plus all plugins: full-registry gates live there
  (the `TaxonomyFullValidationTest` precedent).
- The harness is developed fresh by a new team member. The service API is the handoff artifact;
  LLM function registration, the calibration nudge, and bundle assembly belong to the harness side.
- Branches `1290-ai-llm-integration` and `1294-image-analysis-kdb` die. Their content is mined
  into this design and the corpus; neither branch is merged.

## 4. Approaches considered

| | A. StoryForge-full | B. Structured canon + registry relations (picked) | C. Registry-derived + thin overlay |
|---|---|---|---|
| Core idea | Adapt the whole StoryForge architecture: kinds, slot schemas as code, append-only claims with version windows, runtime conflict scanner, aliases, deterministic lanes | Hand-authored structured entries with typed slot schemas as code; node facts join from the registry at render time; deterministic 3-lane retrieval; launcher gates | Generate most entries from the node registry/docs; hand-author only patterns and mistakes |
| Strength | Rigor: machine-checked contradiction detection, per-claim version history | Kills the drift class at the source; retrieval contract fixes the oikb failure; corpus stays reviewable | Minimal authoring cost, perfect registry sync |
| Weakness | The claims machinery solves multi-editor concurrency the KDB does not have; git MR history already is the ledger; runtime windows duplicate it | Requires distilling the existing corpus into entries (content work) | Registries do not know how to think: explainers, patterns, troubleshooting narratives, and the empirical parameter distributions are exactly what the small model cannot invent; discards the corpus's real value |

B absorbs from A what is cheap and load-bearing: typed slot schemas as code, stable human-readable
entry keys, a closed finding-code list with doc-coverage tests, deterministic lanes before any ML,
the reject/warn split (structural breakage fails the build, semantic trouble warns), the escape-
hatch `properties` map with shadow warnings, one canonical home per inverse relation. B absorbs
from C its central principle: node facts come from the registry at render time, never copied.

### Recorded prior-art results (so nobody re-derives them)

- **oikb (our own negative result):** pure similarity / text lookup misses related items; one
  relevant page does not carry its linked passage. Retrieval must return typed relations and
  explicit next steps.
- **APS-RAG (arXiv 2607.24663, deployed at a photon source facility):** hybrid dense+sparse+graph
  with a corrective agentic loop. Their honest reading: the graph channel's recall contribution was
  not statistically significant, the corrective loop mostly bought faithfulness, and their own
  recommendation is to prioritize reranking before agentic components. Consequence for this design:
  the relation lane is one bounded hop and the deterministic re-rank matters more than any
  autonomous traversal.
- **DocuSearch (arXiv 2609.01617):** hybrid lexical+vector+graph with RRF and grounding checks cut
  hallucination from 21.4% to 6.8% over dense-only. Lexical search for exact identifiers is
  non-negotiable: embeddings dilute node ids, error strings, and parameter names.
- **Anthropic contextual retrieval:** chunks embedded without document context underperform;
  the rendered text we embed carries the title, domain, and breadcrumb for exactly this reason.
- **StoryForge (our own project, AI writing):** typed structure makes consistency deterministic;
  schema-as-code gates correctness; conflicts-as-findings with a reject/warn split; deterministic
  relevance lanes before ML; closed finding-code lists guarded by two tests. Its claims-append-only
  machinery does NOT transfer (see Non-goals).

## 5. The design

### 5.1 Entry model

```java
public final class KdbEntry {
    private final String entryKey;      // stable, human-readable, unique, e.g.
                                        // "bioimage-analysis/patterns/cellpose-segmentation"
    private final UUID id;              // machine id
    private final KdbEntryKind kind;    // closed enum, schema-as-code
    private final String title;
    private final Map<String, List<Object>> slotValues;  // keyed by SlotDef name, schema-typed
    private final String body;          // markdown prose (the genuinely prose part)
    private final List<String> aliases; // match surface for lexical search (not mention highlighter)
    private final String sinceVersion;  // optional: compared against the pinned corpus version
    private final String untilVersion;  // optional: compared against the pinned corpus version
    private final List<String> sourceRefs; // provenance: markdown source path, workflow analysis
                                           // report section, node database evidence
    private final Map<String, Object> properties; // escape hatch, untyped, shadow-warned
}
```

Slot schemas are code, never serialized:

```java
public record SlotDef(String name, KdbValueType type, KdbCardinality cardinality) {}
public enum KdbValueType { TEXT, NODE_REF, ENTRY_REF, NUMBER, VERSION }
public enum KdbCardinality { SINGLE, MULTI }
```

Seed kinds (closed enum `KdbEntryKind`, each with an ordered slot list; extendable only through a
code change, which is the point):

- **concept** - explains a JIPipe or image-analysis concept (the "what/when" layer). Slots:
  `prerequisites` (ENTRY_REF, MULTI), `relatedConcepts` (ENTRY_REF, MULTI),
  `nodes` (NODE_REF, MULTI).
- **pattern** - a workflow recipe: which nodes in what order with which observed parameter values
  (the "how" layer; distilled from the 60-workflow analysis). Slots:
  `problem` (TEXT, SINGLE), `nodeChain` (NODE_REF, MULTI, ORDERED), `parameterEvidence` (TEXT,
  MULTI), `elaborates` (ENTRY_REF, MULTI: canonical home of the pattern-to-concept relation),
  `crossRefs` (ENTRY_REF, MULTI).
- **mistake** - a common error with symptom/cause/fix. Slots: `symptom` (TEXT, SINGLE),
  `cause` (TEXT, SINGLE), `fix` (TEXT, SINGLE), `nodes` (NODE_REF, MULTI), `crossRefs`
  (ENTRY_REF, MULTI).
- **decision-tree** - how to choose between options. Slots: `question` (TEXT, SINGLE),
  `options` (ENTRY_REF, MULTI), `nodes` (NODE_REF, MULTI).
- **node-note** - knowledge about specific nodes the registry cannot carry ("use X before Y",
  ordering constraints, gotchas). Slots: `nodes` (NODE_REF, MULTI), `caveat` (TEXT, SINGLE).

Inverse relations have one canonical home: a pattern declares `elaborates` (concept); the concept
does not list patterns. Rendering derives the inverse direction when needed.

The 11 domains from the dormant branch's `KnowledgeDomain` carry over as a **closed enum**
(core-semantics, parameters, expressions, data-management, file-formats, bioimage-analysis,
integration, project-management, running-pipelines, plugin-dev, tables). `domain` is an entry
attribute derived from the file's folder, not a slot (a slot that restates the folder would be the
shadow the `properties` rule warns about). The entryKey is
`<domain>/<kind-directory>/<entry-name>`, enforced by the integrity gate
(`kind-directory` pluralizes the kind: `concepts`, `patterns`, `mistakes`, `decision-trees`,
`node-notes`). `searchKnowledge`'s domain filter is an exact match against the enum id.

### 5.2 Storage format

One YAML file per entry:

```
jipipe-core/src/main/resources/org/hkijena/jipipe/ai/kdb/entries/<domain>/<kind-directory>/<entry-name>.yaml
```

```yaml
# bioimage-analysis/patterns/cellpose-segmentation.yaml
kind: pattern
title: "Cellpose segmentation of 2D fluorescence data"
aliases: [cellpose, segment nuclei with cellpose]
slots:
  problem: "Segment nuclei or cells in 2D fluorescence images"
  nodeChain: [ij3-cellpose-import, ij3-cellpose-segment, ij2-roi-to-rgb]   # NODE_REF
  parameterEvidence:
    - "Cellpose 2/3 (segmode=cyto) observed in 41 of 60 workflows"
  elaborates: [bioimage-analysis/concepts/segmentation]                     # ENTRY_REF
body: |
  ... prose ...
sourceRefs:
  - "docs/knowledge/sources/bioimage-analysis/patterns/cellpose-segmentation.md"
  - "workflow-pattern-analysis.md#section-7.2"
```

`domain` is not a field: it is read from the folder path, and `kind` must agree with the
kind-directory segment of the path.

Loading parses with Jackson `YAMLFactory` into a wire map, then reconstructs typed values through
the kind's slot schema at the boundary (the StoryForge round-trip lesson: reconstruction is a
narrowing, never a guess; a malformed value produces a clean KDB finding, not an uncaught
exception). Unknown kind, unknown slot, wrong type, or cardinality violation is a structural error
and fails the build.

### 5.3 Node references and the registry contract

`NODE_REF` values are node registration ids (stable string literals). Node lifecycle facts are
NODE-REGISTRY sourced, never corpus sourced (ruling 2026-11-20). The contract:

- **The registry is the canonical home of node lifecycle facts.** Every NODE_REF must resolve
  against the live node registry. There is no corpus tombstone ledger: a node that leaves the
  registry leaves through the registry's own retirement path (see 5.3.1), and a corpus entry that
  still references it fails the integrity gate with the registry's own message. The corpus owns no
  second copy of node existence.
- A registry node id that appears in no entry is a coverage warning (5.8), never an error.
- Rendering resolves live: display name, deprecation badge, successor pointer. For pattern chains,
  the node database contributes `previous`/`next`/`similar` evidence with support counts (5.5, lane
  3), restricted to system templates for reproducibility.
- The corpus never stores display names, parameter lists, slot signatures, or lifecycle state. The
  exception is observed parameter VALUES and counts (corpus content, because registries carry no
  observations).

This is the drift kill: when node `X` is deprecated or gains a successor, every entry that
references it renders the badge and the "use that instead" pointer automatically, with zero corpus
edits.

### 5.3.1 Registry-side successor API (new work in this project)

Today `JIPipeNodeInfo` carries `isDeprecated()` / `isUnstable()` only; neither says what to use
instead. The only deprecation path in the registry is `@Deprecated` on the node class, read by
`JIPipeJavaNodeInfo` into the boolean. The tracked common user complaint ("deprecated node does not
tell me what to use instead") has no data to answer from. This project adds that data:

- **`@JIPipeNodeSuccessor`** (new annotation in `org.hkijena.jipipe.api.nodes`): a single String
  array of successor node registration ids, ordered by preference, plus an optional `reason`. Applied
  to a node class alongside `@Deprecated`. Zero or more successors.
- `JIPipeNodeInfo.getSuccessorNodeIds()` (default empty) and `getSuccessorReason()` (default null);
  `JIPipeJavaNodeInfo` reads the annotation the same way it reads `@Deprecated`;
  `JIPipeMutableNodeInfo` carries setters for plugin-registered nodes.
- A registered id in the annotation that does not resolve is a registry validation error (the same
  fail-closed stance as the taxonomy gate).
- The corpus and the KDB renderer consume this API; they never state it.

Consumers (each is separately useful, none of them depends on the KDB):

1. **Auto-generated node documentation** (the user-facing win): the documentation block shows
   "Deprecated. Use `X` instead." with the same sentence shape and id resolution as everywhere else,
   so a node renamed or replaced has one place to correct and every doc surface follows.
2. **KDB rendering** (5.6): entries referencing a deprecated or superseded node render the badge and
   the successor line.
3. **Node search and relations** (existing surfaces): the `previous`/`next` evidence can point at
   the successor instead of a dead end.

The migration of existing deprecated nodes into `@JIPipeNodeSuccessor` starts with the deprecated node
classes discovered during the pilot: the hidden loop helper nodes (`LoopStartNode`, `LoopEndNode`) and
the hidden rectangular ROI node (`DefineRectangularRoiAlgorithm`, `ij1-roi-from-rectangles`). These
have no successor and are marked as such explicitly with an empty successor list and a factual reason,
rather than left to default. The remaining legacy deprecated node classes (roughly two dozen hidden
helpers across the plugins: ROI append and export variants, legacy plot and table helpers, the legacy
Cellpose 2 inference, and others) are left to the migration phase: the successor annotation is the
mechanism, and each class needs a domain decision about its replacement before one is declared.

### 5.4 Relations

- `ENTRY_REF` links entries by entryKey. One hop maximum in the service, cycle-guarded. The node
  database's `next`/`previous` expansion named below is a later phase (see the lane 3 amendment in
  section 5.5); the shipped relation lane is ENTRY_REF-only.
- Node-anchored lookup: a query containing a node id, alias, or error-string fragment resolves
  through the lexical lane first; the relation lane then pulls the entry's ENTRY_REF neighbors and,
  for pattern entries, the node database's `next`/`previous` neighbors of the referenced nodes.

### 5.5 Retrieval service (protocol-neutral)

New package `org.hkijena.jipipe.api.knowledge` (replacing the dormant classes wholesale):

```java
public final class KnowledgeService {
    public List<KnowledgeResult> searchKnowledge(String query, String domainFilter,
                                                 String kindFilter, int limit);
    public KnowledgeEntryPayload getKnowledgeEntry(String entryKey);   // structured + rendered
    public List<DomainInfo> listKnowledgeDomains();
    public List<RelatedEntry> relatedEntries(String entryKey);          // one hop, labeled
}
```

Lanes, fused with reciprocal rank fusion (k = 60), then deterministic re-rank:

1. **Lexical:** in-memory inverted index with BM25 over entryKey, aliases, title, TEXT slot
   values, node ids, and body. Handles exact node ids, parameter names, error strings. No new
   dependency. This lane never fails.
2. **Vector:** JEMB-backed, bge-small via `JIPipeAIServiceComponent`. Embeds the RENDERED text
   (title + domain + breadcrumb + slot facts + body), not raw markdown. Bundled embedding database
   ships in resources (the dormant branch's `knowledge-embeddings.db` pattern), user cache per
   model id, JIT computation as fallback. Graceful when no embedding model is available: the other
   two lanes still serve.
3. **Relation:** one hop through ENTRY_REFs from lanes 1-2 hits, plus node-database-anchored
   expansion (5.4). Relation evidence is mined from **system templates only** (ruling
   2026-11-20): support counts ("observed in N system templates") are then reproducible across
   machines, so the golden relevance test can assert them, and a global/no-project MCP call has a
   stable answer. User-authored templates do not enter KDB evidence (they are the user's own
   arrangement, not canon).

**Shipped state (final review amendment):** the shipped relation lane is ENTRY_REF-only (the
one-hop `KdbEntryGraph` expansion). The node-database expansion (`previous`/`next`/`similar`
evidence mined from system templates) and the deprecated-node re-rank penalty are a later phase,
ledgered as follow-up work: the next content phase must not assume they exist.

Deterministic re-rank, in order: entries outside their version window are hard-dropped from search
(`sinceVersion` above the **pinned corpus version constant** or `untilVersion` below it; not the
running application version, so the golden test is time-stable) - still resolvable by key for
provenance; deprecated-node-heavy entries take a fixed penalty (see the lane 3 amendment above: the
penalty is a later phase); domain/kind filters apply; stable tiebreak on entryKey. No learned weights.

The **vector lane is not part of the automated golden test** (ruling 2026-11-20): a headless JUnit
JVM has no running embedding model, and the project will not build CI embedding infrastructure for
it. The golden test is golden on the lexical and relation lanes only; a separate unit test pins the
vector lane's wiring (fusion weight, merge, and attribution) with a stubbed embedding. End-to-end
vector behaviour is validated by the user by eye (and, after the MCP charter closes, by the
follow-up real-world test round).

### 5.5.1 Lifecycle

`KnowledgeService` is a process-wide singleton loaded lazily on first use (search, entry lookup, or
domain list), and its corpus index (parsed entries + inverted index + entry graph) is **immutable
once loaded**. Only the render-time joins are live: node display names, deprecation and successor
state, and node-database relations are read from the registry and node database per render, never
cached inside the corpus index. That split is why there is no invalidation API: the corpus is
build-time constant, and everything that changes at runtime is resolved at render time by
construction. Rebuilding the index is a restart-level event (a corpus jar changes only by shipping
a new build); tests construct their own service instance with an injected corpus directory.

### 5.6 Rendering

`renderForAgent(entry, options)` assembles the agent-facing text: title, kind, domain, resolved
node lines with ids (`Cellpose import (ij3-cellpose-import) - deprecated, use ij3-cellpose-import-v2`),
slot facts, body, and a Next steps block: related entries by key with the relation label
("elaborates: segmentation - use get_knowledge_entry"), node-database next/previous evidence,
and the domain list pointer. Rendering is bounded (default ~1200 tokens, configurable; the harness's
mode-based sizing picks the actual value). The rendered text is also what the vector lane embeds,
which keeps the embedding and the retrieval result consistent by construction.

### 5.7 Consumers

- **Harness LLM functions** (new team member's side): `search_knowledge(query, domain?, limit?)`,
  `get_knowledge_entry(entry_key)`, `list_knowledge_domains()`. Calibration-scaled nudge text is
  harness-side.
- **MCP automation operations** (this project): `search_knowledge`, `get_knowledge_entry`,
  `list_knowledge_domains` registered in `AutomationPlugin` as read-only operations
  (`isReadOnly() = true`, so they default to Allow), each with a JSON schema mirroring its parse,
  `additionalProperties: false`. External agents get the same canon as the local agent.
  Tool scope is **`INSTANCE_GLOBAL`** (the KDB is global canon, not project data: any live window
  answers, no project argument is injected into the schema). Aggregation was considered and
  rejected: every instance serves the same corpus, so deduplicating identical answers across
  windows buys nothing and would rewrite ids.
- The service is protocol-neutral: no MCP, Swing, or LLM types leak into `KnowledgeService`.

### 5.7.1 Contract authority

**This spec is the contract authority** (ruling 2026-11-20). The dormant harness spec
(`1290-ai-llm-integration`) is being retired; where the two disagree - the resource path
(`ai/kdb` here, `ai/knowledge` there), the absence of a keyword lane there, the entry format - this
spec wins. The new harness is far enough out that it will adapt to this contract when it is built,
not the other way round. `relatedEntries` stays inside the service and inside rendered next-steps;
it is **not** a fourth MCP operation (traversal is one bounded hop, and the render already carries
the hop's results).

### 5.8 Verification gates (jipipe-launcher)

- **`KdbCorpusIntegrityTest`** (fail): every entry parses; schema conformance (closed kinds, slots,
  types, cardinality); unique entryKeys; entryKey equals the path-derived
  `<domain>/<kind-directory>/<name>`; every NODE_REF resolves in the node registry (fail, with the
  registry's own message for a retired id); every ENTRY_REF resolves; `properties` keys shadowing
  schema slots produce warnings.
- **`KdbFindingCodesTest`** (fail): the finding-code list is closed and every code is named in the
  docs table with its severity (the two-test rule; spec and code cannot drift).
- **`KdbCoverageTest`** (warn, visible in CI output): registered node ids and taxonomy leaf
  functions (48 leaves) with no corpus coverage are reported. Per-leaf granularity keeps the
  report usable; per-node would be noise. Coverage grows incrementally, hence warn-not-fail.
- **`KdbRelevanceGoldenTest`** (fail): 15-30 pinned queries with expected entryKey sets and lane
  attribution, deliberately including the oikb failure modes (related-item queries, exact node-id
  queries, error-string queries, cross-domain queries). Golden on the lexical and relation lanes
  only (5.5); the relation lane runs against a deterministically seeded node database (the
  `NodeDatabaseTaxonomyTest` `setEntries(List)` precedent) so evidence is reproducible.
- **`NodeSuccessorRegistryTest`** (fail, not KDB-specific): every `@JIPipeNodeSuccessor` id resolves,
  the annotated node is deprecated, and no successor chain contains the node itself. Guards the new
  registry API (5.3.1) on its own, independent of the corpus.

### 5.9 Phasing

1. **Registry successor API:** `@JIPipeNodeSuccessor`, `JIPipeNodeInfo`/`JIPipeJavaNodeInfo`/
   `JIPipeMutableNodeInfo` support, the `NodeSuccessorRegistryTest` gate, migration of the existing
   deprecated node classes, and the auto-generated documentation "Use that instead" line. This ships
   on its own value first: it fixes the tracked user complaint without a corpus.
2. **Corpus machinery + pilot corpus:** entry model, YAML load, gates, service + lanes, MCP
   operations; distill the bioimage-analysis patterns and decision trees from the 60-workflow
   analysis report (`/data/JIPipe/Example workflows/workflow-pattern-analysis.md`, present and
   available) into pilot entries. The 46 markdown documents are rescued from the dying branch to
   `docs/knowledge/sources/` (not shipped in jars; `sourceRefs` point there).
3. **Content batches:** remaining domains distilled in reviewed batches (core-semantics,
   data-management, parameters, running-pipelines, integration, project-management, tables,
   plugin-dev), each batch riding with an updated coverage report.
4. **Harness integration (handoff, later):** LLM functions, calibration nudge, evidence bundles for
   sub-sessions; the service API is the contract, adapted to the new harness when it exists.

## 6. What transfers from StoryForge, and what does not

| Principle | Transfer? | Why |
|---|---|---|
| Typed-id / human-key duality (entryKey + UUID) | Yes | Identical need: stable in-text references, stable machine identity |
| Slot schemas as code | Yes | The whole drift-kill depends on schema-gated correctness |
| Claims append-only + disjointness invariant | **No** | Solves multi-editor concurrent fiction; git MR history is the KDB's ledger |
| Conflicts as findings, reject/warn split | Yes (at build time) | Structural breakage fails the build; semantic trouble warns |
| Closed finding-code list + doc-coverage tests | Yes | Spec and code cannot drift |
| Escape-hatch properties + shadow warnings | Yes | Future-proofing without free-form rot |
| One canonical home per inverse relation | Yes | Applied hardest to node facts: the registry is the canonical home, and it gains a successor API to be able to carry them |
| Deterministic relevance lanes before ML | Yes | The oikb fix; APS-RAG independently confirms |
| Story-order window anchoring | **No** | Replaced by optional since/until version qualifiers compared against a pinned corpus version constant, read alongside live deprecation and successor state |
| Prose mention highlighting, alias matcher traps | **No** | No canvas highlighting use case; aliases are a search surface only |
| Pin contested rules as named test vectors + shared fixtures | Yes | The golden relevance test IS this |
| Ratified design note with transferred/not-transferred table | Yes | This section |

## 7. Left to the builder

- BM25 constants and tokenizer (word-boundary, lowercase, split on non-alphanumerics; node ids and
  error strings must survive tokenization as units).
- Token estimation for the render bound (jtokkit if present on master, else a whitespace heuristic;
  the embedding model's own limit is the hard ceiling).
- Exact YAML databind structure (wire maps vs direct binding; the reconstruction-at-boundary rule
  binds either way).
- The bundled embedding database build step (menu tool carried over from the dormant branch, minus
  its markdown-specific parts).
- Migration mechanics for mining the 46 markdown documents (pilot batch is in scope for phase 1;
  the rest is phase 2 content work).
