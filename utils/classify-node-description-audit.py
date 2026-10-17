#!/usr/bin/env python3
"""Classify the node descriptions the shipped DAG patterns depend on.

The (C) review's decision (3) is a full audit of the node descriptions the
patterns use. A length-only scoping pass (missing or <50 chars) undercounts:
the most damaging defect is a description that is long yet describes the WRONG
node (copy-pasted from a sibling) — e.g. ``ij1-roi-to-rgb``, the single most
used pattern node, whose text says "Converts ROI lists to masks" (that is
``ij1-roi-to-mask``'s description).

For every node type that appears in the shipped patterns this classifies the
description into a defect bucket and orders the worklist by pattern frequency:

* ``wrong``              — long but copy-pasted from a sibling node (curated
                           list, verified against the source). Highest value.
* ``missing``            — no ``@SetJIPipeDocumentation`` description in-tree.
* ``thin``               — present but < 50 chars (too short to say what it does).
* ``not_editable_here``  — auto-registered / dynamic / third-party / stale id:
                           the text is not in a JIPipe annotation, or the id no
                           longer exists (the pattern is remapped to a
                           successor that has its own description).
* ``ok``                 — present, long enough, not flagged.

Usage (from the repository root):

    python3 utils/classify-node-description-audit.py
"""

from __future__ import annotations

import json
import os
from collections import Counter

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")

THIN_CHARS = 50

# Verified copy-paste errors: the description text belongs to the SIBLING node,
# not this one (checked against the source). Auditing these is the whole point
# of "the name alone does not carry enough info to infer what it does" — a
# wrong description is worse than a thin one because it confidently misleads.
WRONG_COPY_PASTE = {
    # "Converts ROI lists to masks…" is ij1-roi-to-mask's description; the node
    # converts 2D ROIs to an RGB image (color stored in the ROIs).
    "ij1-roi-to-rgb": "describes ij1-roi-to-mask (says 'converts ROI lists to masks')",
    # "Converts filaments into a 3D ROI" is filaments-convert-to-roi3d's text.
    "filaments-convert-to-mask": "describes filaments-convert-to-roi3d (says 'into a 3D ROI')",
    # "Filter TrackMate spots via expressions" is trackmate-filter-spots' text.
    "trackmate-filter-tracks": "describes trackmate-filter-spots (says 'spots')",
}

# ids that no longer exist in 6.0.0 — the pattern is remapped to the successor
# (which carries its own description), so there is nothing to fix on this id.
STALE_IDS = {
    "ij1-transform-tile",
    "import-ome-zarr-from-directory-as-imgplus",
}

# prefix families whose descriptions are NOT in a @SetJIPipeDocumentation
# annotation (auto-registered third-party kernels, runtime-dynamic variants of a
# single programmatic JIPipeNodeInfo, or a plugin absent from this tree/launcher)
NOT_EDITABLE_PREFIXES = (
    "clij:plugin:",
    "plot-table:",
    "trackmate-create-spot-",
    "jipipe2ai-",
    "create-jipipe2ai-",
    "deep-learning-train-image-model",
)


def is_not_editable_here(node_id: str) -> bool:
    return (any(node_id.startswith(p) for p in NOT_EDITABLE_PREFIXES)
            or node_id in STALE_IDS)


def classify(node_id: str, desc, *, freq: int = 0) -> str:
    """The defect bucket for one node type's description."""
    if is_not_editable_here(node_id):
        return "not_editable_here"
    if node_id in WRONG_COPY_PASTE:
        return "wrong"
    if not desc or not desc.strip():
        return "missing"
    if len(desc) < THIN_CHARS:
        return "thin"
    return "ok"


def rank_worklist(rows: list) -> list:
    """rows: [{nodeId, desc, freq}] -> rows + bucket, highest-impact first.

    Within a bucket: frequency (pattern instances) descending, then id, so the
    output is deterministic and the most-seen broken node is fixed first.
    """
    out = []
    for r in rows:
        row = dict(r)
        row["bucket"] = classify(row["nodeId"], row.get("desc"), freq=row.get("freq", 0))
        out.append(row)
    bucket_rank = {"wrong": 0, "missing": 1, "thin": 2, "not_editable_here": 3,
                   "ok": 4}
    out.sort(key=lambda r: (bucket_rank.get(r["bucket"], 9),
                            -r.get("freq", 0), r["nodeId"]))
    return out


def pattern_node_frequency(judgments_path, clusters_path) -> Counter:
    """node-info-id -> how many shipped patterns' graphs contain it."""
    judg = json.load(open(judgments_path, encoding="utf-8"))
    clusters = {c["clusterId"]: c
                for c in json.load(open(clusters_path, encoding="utf-8"))["clusters"]}
    freq = Counter()
    for e in judg["entries"]:
        if not e["ship"]:
            continue
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        for v in g["nodes"].values():
            freq[v["jipipe:node-info-id"]] += 1
    return freq


def main(argv=None) -> int:
    freq = pattern_node_frequency(
        os.path.join(RESEARCH, "dag-judgments.json"),
        os.path.join(RESEARCH, "dag-clusters.json"))
    descs = json.load(open(os.path.join(RESEARCH, "node-descriptions.json"),
                           encoding="utf-8"))
    rows = [{"nodeId": nid, "desc": descs.get(nid), "freq": c}
            for nid, c in freq.items()]
    ranked = rank_worklist(rows)
    out = {"_note": ("Pattern-relevant node types classified by description "
                     "defect (the (C) review decision 3). wrong/missing/thin "
                     "are the editable audit worklist; not_editable_here has no "
                     "JIPipe annotation to fix (dynamic/3rd-party/stale); ok "
                     "needs no work. See utils/classify-node-description-audit.py."),
           "counts": {},
           "worklist": ranked}
    for r in ranked:
        out["counts"][r["bucket"]] = out["counts"].get(r["bucket"], 0) + 1
    dest = os.path.join(RESEARCH, "node-description-audit-plan.json")
    with open(dest, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print(f"pattern-relevant node types: {len(ranked)}")
    for b in ("wrong", "missing", "thin", "ok", "not_editable_here"):
        if out["counts"].get(b):
            print(f"  {b:20s} {out['counts'][b]}")
    print(f"-> {dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
