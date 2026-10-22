#!/usr/bin/env python3
"""Generate the node-description audit before/after review document.

Reads ``node-description-audit-plan.json`` (the classified worklist with
pattern frequencies) + ``node-description-audit-proposals.json`` (the applied
before/after texts) and writes ``node-description-audit-review.md`` — the
human review table grouped wrong → missing → thin.

Every table row has exactly 4 cells (node | used in | was | now), one cell per
header column — an earlier inline version of this report glued the frequency
onto the node cell, shifting every row left so the "now" column rendered
empty. That failure mode is pinned by the selftest.

Usage:

    python3 utils/make-node-description-audit-review.py
    python3 utils/make-node-description-audit-review.py selftest
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import date

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
PLAN = os.path.join(RESEARCH, "node-description-audit-plan.json")
PROPOSALS = os.path.join(RESEARCH, "node-description-audit-proposals.json")
OUT = os.path.join(RESEARCH, "node-description-audit-review.md")

BUCKETS = (("wrong", "## Wrong (described a sibling node)"),
           ("missing", "## Missing (empty description)"),
           ("thin", "## Thin (<50 chars)"))

WAS_MAX = 110
NOW_MAX = 220


def _clip(s, n):
    s = (s or "").strip()
    return s if len(s) <= n else s[:n - 1] + "…"


def build_report(plan: dict, proposals: dict, today=None) -> str:
    today = today or date.today().isoformat()
    freq = {r["nodeId"]: r.get("freq", 0) for r in plan["worklist"]}
    counts = plan.get("counts", {})
    by_bucket = {}
    for p in proposals["proposals"]:
        by_bucket.setdefault(p["bucket"], []).append(p)
    L = []
    A = L.append
    A("# Node-Description Audit — Before/After (pattern-relevant slice, decision 3)")
    A("")
    A(f"Work item 1324 · {today} · {sum(counts.get(b, 0) for b in ('wrong', 'missing', 'thin'))}"
      f" editable node types of the {len(freq)} the shipped patterns use")
    A("")
    A("Classified by `classify-node-description-audit.py` (full worklist in")
    A("`node-description-audit-plan.json`). Drafted from each node's SOURCE CODE by the")
    A("LLM; applied to the `@SetJIPipeDocumentation` annotation; round-trip verified")
    A("(the extractor returns the proposed text verbatim); all touched modules compile")
    A("and the core/launcher test suites pass at their baselines.")
    A("")
    A("Buckets: **wrong** = the text described a sibling node (copy-paste), **missing**")
    A("= empty, **thin** = present but <50 chars. The "
      f"{counts.get('not_editable_here', 0)} `not_editable_here` (third-party CLIJ")
    A("kernels, runtime-dynamic plot/trackmate variants, the absent jipipe2ai plugin,")
    A("stale IDs that get remapped in (D)) have no JIPipe annotation to fix — the")
    A("remaining ~1440 registry nodes are the follow-up pass.")
    A("")
    for bucket, header in BUCKETS:
        rows = by_bucket.get(bucket, [])
        if not rows:
            continue
        rows.sort(key=lambda p: (-freq.get(p["nodeId"], 0), p["nodeId"]))
        A(header)
        A("")
        A("| node | used in | was | now |")
        A("|---|---|---|---|")
        for p in rows:
            nid = p["nodeId"]
            f = freq.get(nid, 0)
            was = _clip(p.get("current"), WAS_MAX) or "_(empty)_"
            now = _clip(p.get("proposed"), NOW_MAX)
            A(f"| `{nid}` | ×{f} | {was} | {now} |")
        A("")
    return "\n".join(L) + "\n"


def selftest() -> int:
    """The table invariant: every row has exactly one cell per header column."""
    plan = {"counts": {"wrong": 1, "missing": 0, "thin": 1,
                       "not_editable_here": 22, "ok": 228},
            "worklist": [
                {"nodeId": "n-b", "freq": 2, "bucket": "thin", "desc": "d"},
                {"nodeId": "n-w", "freq": 64, "bucket": "wrong", "desc": "d"},
            ]}
    proposals = {"proposals": [
        {"nodeId": "n-w", "bucket": "wrong", "current": "Wrong text.",
         "proposed": "Correct text."},
        {"nodeId": "n-b", "bucket": "thin", "current": "x",
         "proposed": "A longer correct text."},
    ]}
    report = build_report(plan, proposals, today="2026-10-17")
    table_rows = [l for l in report.split("\n")
                  if l.startswith("| `")]
    assert table_rows, "no table rows generated"
    for row in table_rows:
        assert row.count("|") == 5, f"row must have 4 cells: {row}"
    # the wrong bucket (freq 64) is documented and sorted first within its table
    assert "| `n-w` | ×64 | Wrong text. | Correct text. |" in report
    # wrong section precedes thin section
    assert report.index("## Wrong") < report.index("## Thin")
    print(f"selftest OK ({len(table_rows)} rows, all 4-cell)")
    return 0


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Generate the node-description "
                                             "audit review document.")
    ap.add_argument("mode", nargs="?", default="run",
                    choices=["run", "selftest"])
    a = ap.parse_args(argv)
    if a.mode == "selftest":
        return selftest()
    plan = json.load(open(PLAN, encoding="utf-8"))
    proposals = json.load(open(PROPOSALS, encoding="utf-8"))
    report = build_report(plan, proposals)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(report)
    print(f"wrote {OUT} ({report.count(chr(10))} lines)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
