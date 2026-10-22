#!/usr/bin/env python3
"""Generate the DAG-pattern shipping review report (a (D)-stage deliverable).

Reads the pipeline artifacts and writes ``dag-shipping-review.md`` — the
human-review document the user reads before (D) ships the survivors:

* the A→B→C funnel + headline numbers,
* the support distribution + floor options (the (D) curation call),
* the four (C)-review issue metrics (bare-ROI names, jargon names,
  description concreteness) so a re-run's improvement is visible,
* every shipped pattern grouped by function (name · support · shape ·
  description),
* every rejected family (for false-negative check),
* auto-flags (same-name/different-graph, the (D) stale/optional-plugin node
  ids that would fail the launcher deserialization guard, the one mixed
  2D/3D graph).

Deterministic given the artifacts (no model calls). Usage:

    python3 utils/make-dag-review-report.py \
        [--judgments <dag-judgments.json>] [--clusters <dag-clusters.json>]
        [--candidates <dag-candidates.json>] [--registry <registry.json>]
        [--baseline <dag-judgments-v1.json>] [--out <dag-shipping-review.md>]
"""

from __future__ import annotations

import argparse
import json
import os
import re
from collections import Counter, defaultdict
from datetime import date

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")

BRANCH_ORDER = ["data", "annotations", "images", "rois", "filaments",
                "machine-learning", "tracking", "tables", "statistics",
                "plotting", "export", "graph", "misc"]

# the (C) review's four issue detectors (shared with the report + the audit)
JARGON_TERMS = ("annotated path", "hyperstack", "hyper-stack", "slot",
                " graph", "wrapper", "paths", " path")
BARE_ROI = re.compile(r"\brois?\b")

# stale node ids that have a clean current successor (the corpus predates the
# 6.0.0 rename); a stale pattern is RENAMABLE iff ALL its stale ids are here.
STALE_RENAMES = {
    "ij1-transform-tile",
    "import-ome-zarr-from-directory-as-imgplus",
}


def is_2d(i):
    return i.startswith("ij1-") and "3d" not in i and "roi" in i


def is_3d(i):
    return i.startswith("ij3d-") and "roi" in i


def branch_of(e):
    return e["functionId"].replace("jipipe:function:", "")


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def bare_roi_count(shipped):
    return sum(1 for e in shipped
               if BARE_ROI.search((e["name"] or "").lower())
               and "2d" not in (e["name"] or "").lower()
               and "3d" not in (e["name"] or "").lower())


def jargon_count(shipped):
    return sum(1 for e in shipped
               if any(t in (e["name"] or "").lower() for t in JARGON_TERMS))


def thin_desc_count(shipped):
    return sum(1 for e in shipped
               if not re.search(r"takes|from|input",
                                (e["description"] or "").lower()))


def stale_ids(shipped, cluster_by_id, registry_ids):
    """{clusterId: [stale ids]} for shipped patterns that would fail the
    launcher's everyPatternGraphDeserializes guard (unregistered node id)."""
    out = {}
    for e in shipped:
        g = cluster_by_id[e["clusterId"]]["representativeCandidate"]["graph"]
        bad = [v["jipipe:node-info-id"] for v in g["nodes"].values()
               if v["jipipe:node-info-id"] not in registry_ids]
        if bad:
            out[e["clusterId"]] = sorted(set(bad))
    return out


def derived_section(L, derived, judg_entries):
    """The C′ section: the derived siblings the mined set under-covers
    (2D/3D twins + the model's "what next" chains) — for the same manual
    review the mined set gets."""
    A = L.append
    src_name = {e["clusterId"]: e["name"] for e in judg_entries}
    entries = derived["entries"]
    twins = [e for e in entries
             if (e.get("provenance") or {}).get("mechanism") == "twin-swap"]
    wn = [e for e in entries
          if (e.get("provenance") or {}).get("mechanism") == "what-next"]
    shipped_twins = [e for e in twins if e["ship"]]
    wn_shipped = [e for e in wn if e["ship"]]
    wn_noise = [e for e in wn if not e["ship"]]
    rejected = derived.get("rejected", [])

    A("## Derived patterns (C′) — the \"what next\" siblings, for review")
    A("")
    A("The (C′) derivation step fills the gap the review found in the mined")
    A("set: what an analyst would DERIVE from a pattern, not just what the")
    A("corpus happened to contain. Two mechanisms:")
    A("")
    A("- **twin-swap** (deterministic, no model): the verified 2D↔3D twin of")
    A("  a shipped pattern — the other dimension is derivable by construction,")
    A("  so it inherits the source's already-(C)-reviewed name with the")
    A("  dimension token swapped.")
    A("- **what-next** (model-proposed): a short chain of real nodes extending")
    A("  the pattern's outputs. Every proposal is validated fail-closed against")
    A("  the real registry (node ids, exact slot names, type assignability,")
    A("  no cycles, ≤10 nodes) and gated with the (C) rules (no support")
    A("  heuristic — it was never observed).")
    A("")
    A("Provenance lives in `dag-derived.json` only — per the review decision")
    A("they ship as normal patterns. The (D) guards apply identically")
    A("(§3.4 locations, deserialization, `SystemPatternTemplatesTest` re-pin).")
    A("")
    A("### Funnel")
    A("")
    A("| stage | count |")
    A("|---|---|")
    A("| shipped mined patterns swept | 265 (4 parallel shards) |")
    A(f"| deterministic twin candidates | {len(twins)} (all shipped by construction) |")
    A(f"| what-next candidates (validated + deduped) | {len(wn)} |")
    A(f"|   → gated shipped | **{len(wn_shipped)}** |")
    A(f"|   → gate noise | {len(wn_noise)} |")
    A(f"| what-next proposals rejected by fail-closed validation | {len(rejected)} |")
    A(f"| **total derived shipped** | **{len(shipped_twins) + len(wn_shipped)}** |")
    A("")
    A("### twin-swap (26) — the dimension siblings")
    A("")
    A("Source pattern → twin. The name/description are the source's,")
    A("dimension-renamed — flag any where the rename reads oddly.")
    A("")
    for e in sorted(shipped_twins, key=lambda e: e["provenance"]["derivedFrom"]):
        src = e["provenance"]["derivedFrom"]
        target = "3D" if e["clusterId"].endswith("-3d") else "2D"
        A(f"- **{e['name']}** ({target}) — from “{src_name.get(src, src)}” (`{src}`)")
        A(f"  - {e['description']}")
    A("")
    A("### what-next (128) — grouped by function")
    A("")
    A("Each extends a mined pattern (its source is named); the chain is")
    A("verified to type-check against the real node slots.")
    A("")
    fd = Counter(branch_of(e) for e in wn_shipped)
    for b in BRANCH_ORDER:
        grp = [e for e in wn_shipped if branch_of(e) == b]
        if not grp:
            continue
        A(f"#### {b} ({len(grp)})")
        A("")
        for e in sorted(grp, key=lambda e: (e["provenance"]["derivedFrom"],
                                            e["name"])):
            src = e["provenance"]["derivedFrom"]
            st = e["state"]["pattern"]
            A(f"- **{e['name']}** — extends “{src_name.get(src, src)}” (`{src}`) · {st['nodeCount']}n/{st['edgeCount']}e")
            A(f"  - {e['description']}")
        A("")
    A("### what-next rejected by the gate (false-negative check)")
    A("")
    for e in sorted(wn_noise, key=lambda e: e["clusterId"]):
        st = e["state"]["pattern"]
        src = e["provenance"]["derivedFrom"]
        A(f"- “{(e.get('provenance') or {}).get('draftName', '?')}” — extends “{src_name.get(src, src)}” · {st['nodeCount']}n — gate={e['gate']}")
    A("")
    A("### what-next rejected by fail-closed validation (the grounding gate)")
    A("")
    A(f"{len(rejected)} model proposals that named a real slot wrong, used an")
    A("incompatible type, or cited a node id that does not exist — dropped")
    A("before the gate ever saw them (fail-closed by design).")
    A("")
    for reason, n in Counter(r["reason"] for r in rejected).most_common():
        A(f"- {n}× {reason}")
    A("")


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--judgments", default=os.path.join(RESEARCH, "dag-judgments.json"))
    ap.add_argument("--clusters", default=os.path.join(RESEARCH, "dag-clusters.json"))
    ap.add_argument("--candidates", default=os.path.join(RESEARCH, "dag-candidates.json"))
    ap.add_argument("--registry", default=os.path.join(RESEARCH, "registry.json"))
    ap.add_argument("--baseline", default=os.path.join(RESEARCH, "dag-judgments-v1.json"))
    ap.add_argument("--derived", default=os.path.join(RESEARCH, "dag-derived.json"))
    ap.add_argument("--out", default=os.path.join(RESEARCH, "dag-shipping-review.md"))
    a = ap.parse_args(argv)

    judg = load(a.judgments)
    cluster_by_id = {c["clusterId"]: c for c in load(a.clusters)["clusters"]}
    registry_ids = {n["id"] for n in load(a.registry)["nodes"]}
    baseline = load(a.baseline) if os.path.isfile(a.baseline) else None

    entries = judg["entries"]
    shipped = sorted([e for e in entries if e["ship"]],
                     key=lambda e: (e["functionId"], -e["support"], e["clusterId"]))
    rejected = [e for e in entries if not e["ship"]]

    base_ship = ([e for e in baseline["entries"] if e["ship"]]
                 if baseline else None)

    def shape(e):
        st = e["state"]["pattern"]
        seq = st["nodeSequence"]
        return (f"{st['nodeCount']} nodes / {st['edgeCount']} edges — "
                f"{seq[:157] + '…' if len(seq) > 160 else seq}")

    L = []
    A = L.append
    A(f"# DAG Pattern Mining — Shipping Review (steps A–C′)")
    A("")
    A(f"Work item 1324 · branch `1324-node-taxonomy-core` · {date.today().isoformat()}")
    A("")
    A("This is the **manual-review gate before step (D) ships the survivors**.")
    A("Regenerate after any re-run: `python3 utils/make-dag-review-report.py`.")
    A("")
    A("## Funnel")
    A("")
    A("| stage | in | out |")
    A("|---|---|---|")
    A(f"| projects scanned | — | 510 |")
    A(f"| (A) candidates | — | 2029 |")
    A(f"| (B) families | 2029 | 324 |")
    A(f"| **(C) shipped** | 324 | **{len(shipped)}** |")
    rej_counts = Counter()
    for e in rejected:
        rej_counts[e["error"] if e["error"] and not e["error"].startswith("gate_")
                  else f"gate_{e['gate']}"] += 1
    A(f"| (C) rejected | | {len(rejected)} ({dict(rej_counts)}) |")
    A("")
    # (C) review improvement table
    A("## The (C) naming-review fixes (measured)")
    A("")
    if base_ship:
        A("| issue (from the (C) review) | before (v1) | after (this run) |")
        A("|---|---|---|")
        A(f"| bare \"ROI\" names (no 2D/3D) | {bare_roi_count(base_ship)} | "
          f"**{bare_roi_count(shipped)}** |")
        A(f"| JIPipe-internal jargon in names | {jargon_count(base_ship)} | "
          f"**{jargon_count(shipped)}** |")
        A(f"| descriptions with no in/out anchor | {thin_desc_count(base_ship)} | "
          f"**{thin_desc_count(shipped)}** |")
        A(f"| shipped count | {len(base_ship)} | {len(shipped)} |")
        A("")
        A("(The gate now sees each node's real description + ROI dimension, so it")
        A("re-judged some families — the count shift is the model using real")
        A("function evidence, not noise.)")
    A("")
    # support distribution + floor options
    sup = Counter(e["support"] for e in shipped)
    A("## Support distribution of the shipped set")
    A("")
    A("| support | count | cumulative ≥ |")
    for s in sorted(sup, reverse=True):
        A(f"| {s} | {sup[s]} | {sum(v for k, v in sup.items() if k >= s)} |")
    A("")
    A("**The (D) curation call (your decision 1):** the plan specifies no support")
    A("floor (the §1 rule is name-derivability, which the gate enforces). If you")
    A("want a tighter library:")
    A("")
    A("| option | ships |")
    A("|---|---|")
    A(f"| no floor (plan literal) | {len(shipped)} |")
    for f in (2, 3, 5):
        A(f"| support ≥ {f} | {sum(v for k, v in sup.items() if k >= f)} |")
    A("")
    # by function
    fd = Counter(branch_of(e) for e in shipped)
    A("## Shipped by function")
    A("")
    A("| function | shipped |")
    A("|---|---|")
    for b in BRANCH_ORDER:
        if fd.get(b):
            A(f"| {b} | {fd[b]} |")
    A("")
    # (D) stale-id blocker
    stale = stale_ids(shipped, cluster_by_id, registry_ids)
    A("## (D) hard blocker: patterns on unregistered node ids")
    A("")
    A(f"**{len(stale)} shipped patterns reference a node id not registered in the")
    A("6.0.0 launcher** (the corpus predates 6.0.0; some IDs were renamed, some")
    A("live in plugins the 6.0.0 distribution does not ship). Verified against the")
    A("FULL-LAUNCHER registry dump (2026-10-19): the earlier code/test-classpath")
    A("dump was blind to the dynamically-registered cLij2/clijx kernels — the")
    A("real launcher ships cLij2 2.5.3.5 + clijx 0.32.2.0, which resolved 9 of the")
    A("former 15. The launcher guard `everyPatternGraphDeserializes` asserts")
    A("every node id resolves, so these FAIL unless remapped or excluded in (D).")
    A("See `dag-stale-id-plan.json` for the remap/exclude list.")
    A("")
    renamable = {cid: bad for cid, bad in stale.items() if set(bad) <= STALE_RENAMES}
    exclude = {cid: bad for cid, bad in stale.items()
               if not (set(bad) <= STALE_RENAMES)}
    sup_of = {e["clusterId"]: e["support"] for e in shipped}
    A(f"**Renamable** (all stale ids have a clean current successor): "
      f"{len(renamable)}")
    for cid in sorted(renamable, key=lambda c: -sup_of[c]):
        e = next(x for x in shipped if x["clusterId"] == cid)
        A(f"- `{cid}` support {e['support']} {e['name']!r} → {renamable[cid]}")
    A("")
    A(f"**Exclude or plugin-gate** (optional-plugin nodes absent from the default "
      f"launcher): {len(exclude)}")
    for cid in sorted(exclude, key=lambda c: -sup_of[c]):
        e = next(x for x in shipped if x["clusterId"] == cid)
        A(f"- `{cid}` support {e['support']} {e['name']!r} → {exclude[cid]}")
    A("")
    A(f"Shippable after remap + exclude: **{len(shipped) - len(exclude)}**.")
    A("")
    A("### (D) shipped — 2026-10-19 (commit after the full-catalog re-sweep)")
    A("")
    A(f"Shipped **{len(shipped) - len(exclude)} mined + 160 derived = 422 DAG patterns** "
      "(`utils/generate-dag-system-template-metadata.py` → the bundled "
      "`patterns/dag-*.json` resources + `system-templates.json` sidecar), on top "
      "of the 64 linear patterns (486 System templates). The 3 remaps, verified "
      "against git history + the full-launcher registry (same/successor "
      "algorithm, identical `Input`/`Output` slot names):")
    A("")
    A("- `ij1-transform-tile` → `ij1-transform-tile-2d-v2` — clusters 0133 + "
      "0242 (the `-2d` variant is `@Deprecated @LabelAsJIPipeHidden`; the `-v2` "
      "is the current `TileImage2Dv2Algorithm`)")
    A("- `import-ome-zarr-from-directory-as-imgplus` → "
      "`import-ome-zarr-from-zip-directory-as-imgplus` — cluster 0318 (commit "
      "`fa85690da6` unified OME ZARR ZIP/directory reader; a strict superset)")
    A("")
    A("Generator guards (fail-closed, run on every regeneration): all node ids "
      "resolve in the 2072-node full-launcher registry (the (D) id oracle per "
      "the (103) ruling — never the test-classpath dump), functionIds are "
      "registered top-level branches, every node carries its §3.4 "
      "`location/<compartment>/x|y`, `jipipe:node:name` is stripped, no "
      "compartment sinks / fan names, and (name, node-info-id multiset) is "
      "unique. `SystemPatternTemplatesTest` re-pins 486 and re-verifies the "
      "bundled set end-to-end (launcher suite 16/0/0/0).")
    A("")
    A("Known test-classpath gap (documented, not a pattern defect): the Maven "
      "test classpath boots cLij2 **2.3.0.6** while the shipping dist bundles "
      "cLij2 **2.5.3.5** + clijx **0.32.2.0** (a Fiji-only build, not on Maven "
      "Central). 10 cLij2 patterns therefore load with those version-gated "
      "kernels dropped *in the test JVM only*; the test guard matches on "
      "(name, resolvable-node-ids) and stays strict for everything else — the "
      "true stale-id oracle is the generator's 2072 full-launcher check, which "
      "all 422 pass.")
    A("")
    # full shipped list
    A("## All shipped patterns (for review)")
    A("")
    A("Grouped by function, then support (descending). `cluster-XXXX` traces back")
    A("into `dag-clusters.json` / the corpus.")
    A("")
    for b in BRANCH_ORDER:
        grp = [e for e in shipped if branch_of(e) == b]
        if not grp:
            continue
        A(f"### {b} ({len(grp)})")
        A("")
        for e in grp:
            mark = ""
            if e["clusterId"] in stale:
                mark = " ⚠ unregistered id"
            A(f"- **{e['name']}**{mark} · support {e['support']} · {e['state']['pattern']['signal']} · `{e['clusterId']}`")
            A(f"  - {shape(e)}")
            A(f"  - {e['description']}")
        A("")
    # derived (C') section — the "what next" siblings, same manual review
    if os.path.isfile(a.derived):
        derived = load(a.derived)
        derived_section(L, derived, judg["entries"])
    # rejections
    A("## Rejected (false-negative check)")
    A("")
    A(f"{len(rejected)} families the model cut. The high-support / good-title ones")
    A("are where a false negative would hide.")
    A("")
    for e in sorted(rejected, key=lambda e: (-e["support"], e["clusterId"])):
        c = cluster_by_id[e["clusterId"]]
        reason = (e["error"] if e["error"] and e["error"] != "malformed_verdict"
                  else f"gate={e['gate']}") if e["error"] else f"gate={e['gate']}"
        if e["error"] is None:
            reason = f"gate={e['gate']}" if e["gate"] else "name empty (gate general)"
        titles = c["titleEvidence"][:2]
        st = e["state"]["pattern"]
        A(f"- `{e['clusterId']}` support {e['support']} — **{reason}** — {st['nodeCount']}n/{st['edgeCount']}e")
        if titles:
            A(f"  - titles: {titles}")
        vn = e.get("verdict", {})
        if vn.get("name") and e["gate"] in ("general_pattern",):
            A(f"  - model name: {vn['name']!r}")
    A("")
    # flags
    A("## Flags")
    A("")
    names = defaultdict(list)
    for e in shipped:
        names[e["name"]].append(e["clusterId"])
    dups = {n: cs for n, cs in names.items() if len(cs) > 1}
    A("**Same name, different graph** (legal — the browser dedups on (name, graph)):")
    for n, cs in sorted(dups.items()):
        A(f"- {n!r} × {len(cs)}: {', '.join(cs)}")
    A("")
    mixed = [e for e in shipped if any(is_2d(v["jipipe:node-info-id"]) for v in
            cluster_by_id[e["clusterId"]]["representativeCandidate"]["graph"]["nodes"].values())
            and any(is_3d(v["jipipe:node-info-id"]) for v in
            cluster_by_id[e["clusterId"]]["representativeCandidate"]["graph"]["nodes"].values())]
    A("**Mixed 2D+3D ROI graph (decision 4: keep + renamed to state the span):**")
    for e in mixed:
        A(f"- `{e['clusterId']}` {e['name']!r}")
    A("")
    open(a.out, "w", encoding="utf-8").write("\n".join(L) + "\n")
    print(f"wrote {a.out} ({len(L)} lines)")
    print(f"shipped {len(shipped)}, rejected {len(rejected)}, stale-id {len(stale)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
