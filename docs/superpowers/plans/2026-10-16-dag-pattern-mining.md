# DAG-Aware Pattern Mining — Plan (scheduled follow-up, #1324 / #1333)

Status: **plan only — not implemented.** The immediate curation (the 64 shipped
linear-chain patterns) is done; this is the next step that adds the
DAG-shaped patterns (e.g. a cellpose training setup) to the System pattern
library.

Date: 2026-10-16 · Branch: `1324-node-taxonomy-core` · Work item: 1324 (patterns) / 1333 (repository seam)

---

## 1. What a pattern is (the definition all miners must satisfy)

A pattern is a **reusable, general subgraph of a pipeline that has a proper
derivable name of what it does**. Three consequences:

- **Shape-agnostic.** Linear chains are only the easy-to-name *subset*; DAGs
  are first-class (a cellpose training setup: import images + import ROIs →
  split → annotate → train, with a fan-in at the training node). The first
  miner (chain/fan) could only express chains and statistical fans — which is
  why DAGs never appeared and the fans were pure noise.
- **Name = the test.** If a subgraph cannot be named from what it does
  (topologically-ordered node display names, or "N: verb" for a DAG), it is
  noise and must not ship. Names like "X fans in from 11 source ids" or
  "... -> other" fail this test and were excluded from the shipped set.
- **Not workflow plumbing.** Subgraphs that exist only to connect a
  compartment (a `jipipe:compartment-output` sink, compartment-specific
  variable definitions) carry no general information and are excluded.

## 2. Corpus (measured, this machine)

- `/data/JIPipe`: **541 `.jip` projects**, ~1 main compartment each, **~43
  compartments with DAG shape** (≥5 nodes + a fan-in). Focused test pipelines
  include the canonical DAG examples: `Cellpose training example/
  cellpose3_training.jip` (18 nodes, 3 fan-in + 3 fan-out),
  `cellpose4_training.jip`, plus trackmate/filaments/3d/glomeruli tests.
- The existing research corpus (`utils/taxonomy-research/`, 185 mined chain/
  fan candidates) stays as the *linear* source; the DAG miner is additive.

## 3. Design — extract → embed+cluster → JEV gate/name/classify → ship

The pipeline reuses the **existing System-template machinery** end to end
(`utils/generate-system-template-metadata.py` style generator +
`system-templates.json` sidecar + `TaxonomyPlugin.registerSystemTemplates()`
+ the live dedup/content-identity guards), so shipping is mechanical once
candidates exist.

### 3.1 Extraction (deterministic, no AI)

A `utils/mine-dag-patterns.py` script over the `.jip` corpus:

- Per compartment graph: enumerate **connected induced subgraphs** of
  3–12 nodes (candidate size cap — patterns that are whole pipelines are not
  reusable units), with in/out degree > 1 allowed (DAGs), acyclic.
- **Support counting**: count projects in which each candidate occurs
  (isomorphic on node-type + slot signature, the same functional-identity
  notion the current generator uses — instance keys/UUIDs/labels excluded).
- Filter: support ≥ 2 (or ≥ 1 for the *known flagship DAGs* — see the
  scheduling note), exclude candidates containing a compartment sink,
  exclude candidates whose every node has a unique type AND whose only
  structure is a chain (already covered by the linear set — de-dup against
  the shipped 64).
- Output: `utils/taxonomy-research/dag-candidates.json` (candidate graphs +
  support + source projects), research-artifact only (read-only convention,
  like the 185).

Practical note: full k-subgraph enumeration is combinatorial; on 43 DAG
compartments of ≤~25 nodes this is tractable with a meet-in-the-middle /
support-pruned gSpan-style expansion, or — simpler and sufficient for this
corpus size — enumerate **sinks of interest** (train/infer/measure nodes) and
grow their in/out neighborhoods up to the size cap. Prefer the latter
(starting from the semantically important nodes) — it naturally produces the
"setup" shape of the cellpose patterns.

### 3.2 Embedding + clustering (Qwen embedding endpoint, `asb-llm.hki-jena.de`)

- Each candidate cluster → a canonical text form:
  `"<node name>.<slot> → <node name>.<slot>; …" + " nodes: A, B, C"`,
  embedded via the Qwen endpoint (the same OpenAI-compatible
  `/embeddings` API the app's `JIPipeAPIEmbeddingAIModelRunner` speaks, so
  the miner can reuse that code path verbatim).
- Agglomerative clustering (cosine) to (a) **dedup** the same DAG mined from
  different projects/wordings and (b) detect **recurring units** (e.g. the
  "import images + import ROI masks → train" block recurring across
  cellpose3/cellpose4/CellposeTraining). Threshold sweep 0.1–0.4 (the
  published workflow-graph-clustering literature finds ~0.2–0.3; GraphMind
  et al. use exactly this embedding-clustering step at a fraction of LLM cost).
- Keep the **cluster representatives** (max-support member) as the candidates
  for the JEV stage. This is the cheap, scalable stage — it can run over the
  whole corpus.

### 3.3 JEV quality gate + classification + naming (imajev endpoint)

imajev (UniJEV-4B + the `decision_readout` codebook,
`/data/models/unijev/sidecar-imajev-4b/imajev-unijev.json`) is a
**multiple-choice verifier** ("inspect the evidence… choose unknown when the
evidence is insufficient… return only the single option code"), so it is used
for exactly the bounded decisions it is good at — NOT open-ended generation:

- **Q1 — quality gate** (options: `general reusable pattern` /
  `workflow-specific plumbing` / `noise`). Reject anything not `general
  reusable pattern`; this is the enforceable form of the §1 definition.
- **Q2 — function** (options: the 13 top-level branch names, ≤8 questions
  fits comfortably under the 255-option limit). Replaces the
  most-common-ancestor heuristic for DAGs (where it is ill-defined: a
  training DAG touches import + annotations + ml).
- **Q3 — name selection** (options: 3–5 candidate names generated
  deterministically from the structure — topological/BFS node-name ordering,
  "N: verb" prefixes for fan-ins, the cluster's recurring-unit label — plus
  `none`). A name the model can't pick = `none` = reject (no name, no ship).

Each question takes the candidate's canonical text form (node names + wiring)
as "state". Temperature from the sidecar (1.3) — but for a *gate* we want the
argmax code, so request greedy decoding where the endpoint allows it.

### 3.4 Shipping

Candidates that survive Q1–Q3 feed the existing generator flow with one
extension: the sidecar gains DAG entries (file + functionId + name +
description). The description keeps the Sequence/Wiring lines (BFS-ordered
for DAGs) — the "Wiring:" line is what disambiguates same-name patterns and
stays. The dedup/content-identity/role-name/distribution guards all apply
unchanged; the distribution target is extended to include the DAG
candidates' functions (re-pin at first DAG ship).

## 4. First DAGs to land (the scheduling)

The user's examples define the acceptance bar:

1. **Cellpose training setup** — from `Cellpose training example/
   cellpose3_training.jip` (and the cellpose4 sibling): the
   import-images + import-ROI-masks → (morph) → split → annotate → train
   block. Name target: "Import images and ROI masks → Split data → Annotate
   with data → Train Cellpose (3.x)" (or JEV-selected variant).
2. **Testing multiple thresholding methods** — a DAG fanning out one image
   into several thresholding branches and fanning the results back in
   (comparison pattern). Source: the `/data/JIPipe` threshold tests.

Suggested sequence (each a reviewable commit):

- **(A)** `utils/mine-dag-patterns.py` extraction + support counting +
  de-dup-against-64; run over the corpus; commit the
  `dag-candidates.json` research artifact. (Deterministic; no AI.)
- **(B)** embedding clustering stage (Qwen endpoint); commit the clustered
  representatives + the threshold choice with a short note.
- **(C)** JEV gate/name/classify stage (imajev endpoint); commit the
  surviving candidates with their Q1–Q3 answers (audit trail).
- **(D)** generator extension + sidecar + `SystemPatternTemplatesTest`
  re-pin (count + the two flagship DAG patterns) + the manual review.

Estimated size: A is the bulk of the work (one script, corpus-sized run);
B/C are thin HTTP clients around the existing runners; D reuses (69)'s
generator.

## 5. Explicit non-goals (this round)

- No mining inside the app (it stays an offline research pipeline, like the
  185; the app only ships results).
- No re-mining of the linear set (the 64 stand; the DAG set is additive).
- No new relation kinds (the Similar/Previous/Next index already consumes
  every template graph, DAGs included — nothing to change there; a DAG
  pattern's source nodes simply have empty `previous`, as spec §13 noted).
- #1333 repository: still out of scope; the Download tab remains the seam
  (mined DAG patterns ship as System; sharing is later).
