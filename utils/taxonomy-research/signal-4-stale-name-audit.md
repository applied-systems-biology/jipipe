# Signal 4 (named nodes) — stale-name audit & fix (2026-10-19)

**Question (user, during the C′ review):** "Because of how names are
serialized, candidates found by section 2 signal 4 need to be carefully
investigated if it's even a signal or just an old name."

**Verdict: confirmed — partially.** Signal 4 as defined in the plan (`label ≠ current registry name`) conflates two
events that serialize
identically into `jipipe:node:name`:

1. a **user** renames a node instance (genuine naming signal), and
2. a **developer** renames the node *type* — every project saved before the
   rename keeps the old default name, which now differs from the current
   registry name (pure staleness, not a signal).

This is the same staleness mechanism the (101) fix closed for the shipped
linear patterns, applied to the *harvest side*.

---

## 1. Scope correction: signal 4 finds no candidates

Per plan §2, signals 1–3b yield candidate *patterns*; signals 4–5 yield
naming *evidence* for the merge/namer stage. Verified from source and
artifact, not just the plan text:

- `(A)` `mine-dag-patterns.py` has no named-node candidate branch; the only
  `signal` values it assigns are the four grouping signals.
- The artifact agrees: all 2029 candidates carry exactly
  `canvas-note-section` (814), `geometric-cluster` (780), `compartment`
  (235), `group-node` (200). **Zero candidates exist that "signal 4 found."**

So the contamination below cannot have created, dropped, or titled any
pattern. It affects one field: `nameEvidence` in `dag-clusters.json`.

## 2. Measurement (full corpus + artifact)

Corpus: 1,966 `.jip`, 123,122 node instances (top-level + compartment
graphs). 100% carry a non-empty `jipipe:node:name` (JIPipe serializes the
resolved name at save time, default or custom).

- 52,588 instances (42.7%) have a label ≠ **current** registry name — the
  raw signal-4 hit rate.
- Of the 874 `nameEvidence` entries actually written across the 324
  clusters: **251 (29%) are uniform stale defaults** — the same label on
  ≥80% of that type's corpus occurrences and ≠ the current name, i.e. the
  signature of a dev rename:

  | type | stale default (share) | current registry name |
    |---|---|---|
  | `path-to-annotation-simple` | "Add path to annotations" (93%) | Annotate with file/directory name |
  | `ij1-roi-set-overlay` | "Set overlay" (99%) | Set 2D overlay |
  | `ij1-threshold-auto2d` | "Auto threshold 2D" (98%) | Auto threshold 2D (8-bit) |
  | `ij1-blur-median2d` | "Median blur 2D" (90%) | Median filter 2D |
  | `ij1-roi-extract-metadata` | "Extract ROI metadata as table" (100%) | Extract 2D ROI metadata as table |

  **47 clusters had nameEvidence that was 100% dev-rename noise.**
- The other **623 (71%) are genuine user naming** — variants and suffixes
  deviating from the type's norm ("Annotate with file names", "Auto
  threshold 2D: cadherin mask", "Convert ROI to RGB: blue overlay").
  The discriminator: a dev rename is **uniform per type** across the whole
  corpus; user naming **varies per instance**.

## 3. Blast radius: contained, nothing shipped is affected

Traced every consumer of `nameEvidence`:

- **Zero downstream consumers.** The only reference in code is the (B)
  writer (`merge-dag-patterns.py`); the (C) judge and the review-report
  generator never read it. It was *intended* as namer vocabulary (plan
  §3.2) but (C)'s `build_state` never included it — (C) saw
  `titleEvidence` (human-authored group/note/compartment titles) + **registry** node names only.
- The **(C) namer** therefore produced all 265 mined + 154 derived names
  from registry names + authored titles — **no shipped name or description
  carries a stale node name.** (Verified in the shipped states, e.g.
  "Annotate with file/directory name", not "Add path to annotations".)
- **(B) clustering** embedded the raw labels (canonical text). A dev rename
  is uniform per type, so it acted as a *consistent* token in the
  embedding space — same old name on every instance of a type — and did not
  distort grouping. (Recorded as a known limitation; (B) is frozen and
  reviewed, not re-run.)

**Net: no re-mine, no (B)/ (C) re-run, no re-ship.** The defect was in a
write-only research field.

## 4. The fix (implemented this commit)

`collect_name_evidence` now has **two non-evidence baselines** instead of
one:

- the type's **current registry name** (a post-rename project serializes
  the new default), and
- the type's **dominant census label** — the most frequent label for that
  type over the candidate population (rebuilt from `dag-candidates.json`
  on every (B) run, deterministic lexicographic tie-break, no new artifact).
  For an un-renamed type the dominant *is* the registry name (behavior
  unchanged); for a renamed type the dominant is the **stale old default**,
  which is exactly what the old detector miscounted as user naming.

A label is evidence only if it deviates from **both** — i.e. it is a
genuine per-instance user choice.

**Regenerated `dag-clusters.json`:** the (B) re-run was 100% cache-only (all 624 representative texts already embedded —
verified before running,
zero endpoint calls). The diff proves the clusters are **byte-identical
except `nameEvidence`** (and the `generated` timestamp):

- entries 874 → **472** (402 stale-default labels dropped);
- re-audit: 0 remaining entries equal their type's dominant label, with one
  documented residual — "Z-Project" in cluster-0236, a count-1 instance of
  `ij1-dimensions-zproject` (census: "Z-Project (classic)" 4 / "Z-Project"
  1). The label equals the *sibling* type's current registry name; the
  mode rule cannot resolve a 20%-share label (stale type-1 default vs.
  genuine user rename). Known limitation for low-mode-share types; 1 entry
  in a write-only field.

**Tests:** `merge_dag_patterns_tests.py` 14/14 (2 new: dominant-label
exclusion keeps genuine user naming; deterministic tie-break).

## 5. Remaining (accepted, documented)

- `(B)` canonical text still embeds corpus labels (uniform per type; (B)
  frozen and reviewed). A future re-mine would get the same clusters.
- The residual low-mode-share ambiguity (one entry, above) and types whose
  mode itself is a popular user label — at most one label per type can be
  miskept/misdropped; the field is advisory.
- The plan §2 row for signal 4 is amended to the corrected definition.
