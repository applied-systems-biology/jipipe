# DAG-Aware Pattern Mining — Plan v2 (scheduled follow-up, #1324 / #1333)

Status: **plan only — not implemented.** Supersedes the v1 extraction
(induced-subgraph enumeration) — the extraction problem is better framed as
**harvesting the groupings humans already authored**, which the user mapped
to well-known human tasks:

1. sections drawn with canvas notes ("these nodes fulfill a function"),
2. group nodes (a human decided "this belongs together"),
3. small compartments (a compartment may fulfill a single function),
4. named nodes (rarely done, same class of signal),
5. classes → packages (programming: the named module is the unit).

The v1 insight survives at a different stage: the AI/embedding stage is not
for *discovering* structure (humans did) but for *merging, gating and naming*
what was harvested.

Date: 2026-10-16 (v2 same day) · Branch: `1324-node-taxonomy-core` · Work item: 1324 (patterns) / 1333 (repository seam)

---

## 1. What a pattern is (unchanged)

A pattern is a **reusable, general subgraph of a pipeline that has a proper
derivable name of what it does** — shape-agnostic (DAGs first-class), name =
the test (unnameable ⇒ noise), no workflow plumbing (compartment sinks).

## 2. The five human signals (all verified in the `.jip` JSON, measured)

The corpus is `/data/JIPipe`: **541 projects** (535 with named compartments,
148 with at least one authored grouping signal). Each signal is mechanically
harvestable — no structure discovery needed:

| # | Signal | Storage | Membership | Measured |
|---|--------|---------|------------|----------|
| 1 | **Canvas-note sections** (`graph-annotation-text-box`, + `graph-annotation-group-box`) | `text-title` + `grid-width/height` + `location/x,y` (grid rect) | **geometric**: nodes whose grid coords fall inside the rect | 1000 notes + 105 boxes; verified on the flagship: 4 titled sections cover 13/13 nodes, "Loading raw images and ROIs" = 8-node DAG with 8 intra edges |
| 2 | **Group nodes** (`node-group`) | `jipipe:node:name` + `contents: {nodes, edges}` | **explicit** (embedded subgraph) | 215 (all with contents; 200 in the 3–12 node band); names e.g. "READ: folders, subfolders and files" |
| 3 | **Named compartments** (`jipipe:project-compartment` in `compartments["compartment-graph"]`) | `jipipe:node:name` | **shared UUID**: pipeline nodes whose `jipipe:graph-compartment` equals the compartment node's key | 535 projects; 480 compartments in the 3–12 band; names e.g. "Analysis", "Main" |
| 3b | **Geometric clusters (unannotated)** — the user's addition: related nodes are placed together, distinct from other parts, even without a canvas note | node grid coordinates (`location/<comp>/x,y`) | **single-linkage clustering with an automatic gap cut**: the largest ratio between consecutive merge distances is the natural separation ("distinct from other parts"); fixed-epsilon DBSCAN was rejected as eps-sensitive (jaccard 0.67–1.0 across eps 4–12) | measured over the corpus: 41/78 checked projects (6–60 nodes) show clean auto-cutable clusters (ratios 1.3–2.5); **18 are unannotated** — e.g. `cellpose4-training.jip` (no notes) auto-cuts into its real blocks (9-node image/ROI loading + 5-node label loading + the 3.x/4.x training pairs); on the annotated flagship the auto-cut agrees with the human sections (jaccard 0.67–1.0) |
| 4 | **Named nodes** (`jipipe:node:name` ≠ default) | per node | n/a (naming signal, feeds the namer) | the corpus is full of them; default-name detection = name ≠ node-info-id display name |
| 5 | **Comment nodes** (`jipipe:comment`) | `jipipe:node:description` (HTML) | adjacency (they attach to a node via the Comment slot) | 385; same class as 4 — text + attachment, not grouping |

Signals 1–3b yield the candidate **patterns** (1–3 with a human-authored
name; 3b without — see the harvest stage);
signals 4–5 yield naming **evidence** (for the merge/namer stage), not
candidates.

## 3. Design — harvest → merge → gate/name → ship

Reuses the existing System-template machinery end to end (generator +
`system-templates.json` sidecar + `TaxonomyPlugin.registerSystemTemplates()`
+ the live guards from (69)); shipping stays mechanical.

### 3.1 Harvest (deterministic, no AI) — `utils/mine-dag-patterns.py`

For each `.jip` in the corpus (same exclusions: tmp dirs, Fiji, docs):

- **Group nodes**: the candidate = `contents` verbatim (it is already a
  `JIPipeGraph`-shaped subgraph); title = the group's `jipipe:node:name`
  (reject default "Group"); exported-parameters stay (they are part of the
  recipe).
- **Canvas-note sections**: candidate = the induced subgraph over the
  rect-contained nodes; title = `text-title`. Drop annotation nodes
  themselves and compartment sinks from the member set (they are plumbing),
  then keep only components with ≥2 remaining real nodes.
- **Named compartments**: candidate = the induced subgraph over the
  compartment's UUID-sharing nodes; title = the compartment name (reject
  "Analysis"/"Main"-class generic names via a small stop-list, or let the
  JEV gate reject them — see 3.3).
- **Geometric clusters (signal 3b)**: candidate = the induced subgraph over
  each auto-cut cluster (single-linkage + largest-merge-distance-ratio cut,
  capped at 8 clusters; clusters of 3–12 real nodes). No title — these
  candidates carry NO authored name, so they route differently: the merge
  stage (3.2) first tries to match them against a titled candidate
  (same-cluster ⇒ the title transfers); unmatched ones rely entirely on the
  JEV Q3 namer, and `none` ⇒ reject (no name, no ship — the §1 rule). The
  annotated and unannotated variants interact: where notes exist, the note
  rectangles are the ground truth and geometric clustering only fills the
  uncovered gaps (the flagship's 8-node section abuts its neighbor — pure
  geometry merges them; the note boundary separates them); where no notes
  exist, geometry is the only signal.
- Cross-cutting filters: 3–12 real nodes (band verified), exclude candidates
  containing a compartment sink, exclude pure chains already shipped (de-dup
  against the 64 by the functional-content signature from (69)).
- Output: `utils/taxonomy-research/dag-candidates.json` — candidate graph +
  signal provenance (which signal, source project, title) + support counts
  (a candidate harvested from N projects is itself a cross-project pattern).

### 3.2 Merge/dedup + name evidence (Qwen embedding, `asb-llm.hki-jena.de`)

Candidates from different projects/projects' wordings describe the same
pattern ("Load images and ROIs" vs "Loading raw images and ROIs"; cellpose3
vs cellpose4 variants). Deterministic signature dedup catches only exact
matches; merge the near-duplicates:

- Canonical text form per candidate (title + topologically/BFS-ordered node
  display names + wiring), embedded via the Qwen endpoint (OpenAI-compatible
  `/embeddings` — the same API `JIPipeAPIEmbeddingAIModelRunner` speaks).
- Agglomerative clustering (cosine, threshold sweep 0.1–0.4; literature and
  practice put useful cuts around 0.2–0.3). Cluster = one shipping
  candidate; representative = highest-support member; the cluster's titles
  are the candidate-name evidence.
- Signals 4–5 feed this stage too: named nodes and comment text inside a
  cluster are additional name evidence (vocabulary for 3.3's options).

### 3.3 Gate + name + classify (imajev, the JEV MCQ decision model)

imajev (UniJEV-4B + `decision_readout` codebook,
`/data/models/unijev/sidecar-imajev-4b/`) answers **multiple-choice**
questions ("inspect the evidence … choose unknown when the evidence is
insufficient … return only the single option code") — used only for bounded
decisions:

- **Q1 quality gate** — `general reusable pattern` / `workflow-specific
  plumbing` / `noise`. The enforceable form of §1; rejects generic-name
  compartments ("Analysis"), scratch sections, one-off groupings.
- **Q2 function** — the 13 top-level branches (options well under the 255
  cap). Replaces the most-common-ancestor heuristic for DAGs where it is
  ill-defined.
- **Q3 name** — options = the cluster's title evidence + deterministic
  candidates (BFS-ordered registry node names; "N: verb" for fan-ins) +
  `none` ⇒ reject. The human-authored titles dominate the option set; the
  model picks the best general name or admits none.

State = the candidate's canonical text form (title evidence + node names +
wiring). Answers recorded in the research artifact (audit trail), like the
(69) generator's guards.

### 3.4 Ship

Survivors feed the existing generator: the sidecar gains DAG entries (file +
functionId + name + description; the "Wiring:" line stays — BFS-ordered for
DAGs). All (69) guards apply unchanged; the distribution target is extended
at the first DAG ship and re-pinned in `SystemPatternTemplatesTest`.

**Node locations (the positioning fix, data-side).** The round-2 review
found that mined templates carried **no node locations**, so a double-click /
"Add to graph" drop landed every node on the same grid cell (a "bunch"). A
first attempt computed a layout at insert time (in `addToGraph`) and was
reverted on request — the real fix is to bake the positions into the mined
template *data*, so they travel with the shipped JSON like a user-authored
template's do. Contract for the harvest + generator:

- **Source of truth = the source project's layout.** When a candidate is
  induced from a real project (group `contents`, a canvas-note section, a
  named compartment, or a geometric cluster), copy each member node's
  `jipipe:metadata-v1` `location/<compartment>/x|y` grid coordinates verbatim
  into the mined template's node metadata, **normalized** so the candidate's
  bounding-box top-left is at the origin (subtract the min x / min y across
  members). This keeps the pattern compact and drop-anchored (the canvas
  re-anchors stored locations at the cursor on insert).
- **Normalize the compartment key.** The source node's location is stored
  under its *source project's* compartment UUID. In the mined template every
  node must instead store its location under the template's own (single,
  shared) `jipipe:graph-compartment` — the same compartment string the node
  already carries in the template. Insert then re-keys it to the canvas'
  compartment (see `rekeyTemplateGraphToCompartment`, (75)) and the stored
  location resolves.
- **Fallback for candidates with no usable source layout** (a group whose
  `contents` nodes carry no metadata, a purely geometric cluster): run a
  compact longest-path layering at *mine time* (same algorithm the reverted
  runtime used — topological column, stack rows), anchored at the origin.
  Prefer the real layout; the computed one is the fallback, not the default.
- **Guard:** the (69) dedup signature already ignores
  `jipipe:graph-compartment` and is location-agnostic, so two functionally
  identical patterns with different layouts still collapse correctly. A new
  generator assertion: every shipped DAG template's nodes carry a
  `location/<their-compartment>/x|y` pair (no bunch-on-drop possible).

## 4. First patterns to land (acceptance bar)

1. **Cellpose training setup** — flagship DAG from the flagship project's
   "Loading raw images and ROIs" (8 nodes) + "Training" (3 nodes) sections,
   or the group/compartment equivalents in the cellpose4 siblings. Name
   target: the human title or its JEV-picked generalization.
2. **Testing multiple thresholding methods** (comparison fan-out/in DAG).

## 5. Sequence (each a reviewable commit)

- **(A)** `utils/mine-dag-patterns.py` harvest (signals 1–3) + filters +
  support counts + **node locations** (verbatim from the source layout,
  normalized to the origin, re-keyed to the template's compartment — §3.4);
  commit `dag-candidates.json`. (Deterministic; no AI.)
- **(B)** embedding merge stage (Qwen) + name-evidence collection; commit
  clusters + threshold note.
- **(C)** JEV gate/name/classify (imajev); commit survivors + Q1–Q3 answers.
- **(D)** generator extension + sidecar + test re-pin + manual review.

## 6. Non-goals (unchanged)

No in-app mining (offline research pipeline, like the 185); no re-mining the
linear 64 (additive); no new relation kinds (the index already consumes DAG
template graphs; source nodes simply have empty `previous`); #1333 repository
stays out (Download tab seam).
