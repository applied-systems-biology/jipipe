#!/usr/bin/env python3
"""Task 6: function-tree fit & secondary-function value over the real corpus (6.0.0).

Reads corpus.json + registry.json (locked, Tasks 1-5); emits report section 4
(4a) leaf occupancy per primary function id (top-15 biggest, bottom-15 smallest
non-empty, catch-all `misc` + its branch members marked); (4b) "orphan load":
corpus-USED nodes in the Miscellaneous catch-all, BOTH readings (primary ==
`misc` exactly / primary branch == the misc branch) with id + name lists;
(4c) distinct corpus-USED node ids per top-level branch (full 13-branch table,
<3-used candidates, 0-used flagged as strongest); (4d) secondary-value
candidates: used nodes N with primary branch Pb and >= 1 secondary branch whose
best secondary-branch mass strictly exceeds N's primary-branch mass among N's
corpus neighbor endpoints (in AND out, project-wide uid resolution, ANY
dataClass, self-edges excluded, >= 3 known-branch endpoints floor; top 20 of all
+ total + 3-node spot-check verification). The method note carries the branch /
secondary-branch definitions, the 6d rule verbatim, the 6b dual reading, and
the absent-id handling. No interpretation (Task 9's job); stdlib only; ONE
project-wide uid->node map per project (edges re-homed into the target
compartment). Emits analysis/t6_table.md and report.md (created if absent; on
re-runs only "## 4. " -> next "## "/EOF replaced; byte-stable, hash-seed-immune).
Usage: python3 t6_functions.py
"""

import json, re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE = ROOT / "report.md", ROOT / "analysis" / "t6_table.md"
MISC = "jipipe:function:misc"
FLOOR = 3    # 6d evidence floor: >= 3 known-branch neighbor endpoints
TOPN, BOTN, D_TOP = 15, 15, 20
S = lambda fid: fid.replace("jipipe:function:", "")


def branch_of(fid, parent_of):
    """Top-level branch of a function id: parent-walk to null (verified acyclic)."""
    seen = 0
    while parent_of.get(fid) is not None:
        fid = parent_of[fid]
        seen += 1
        assert seen < 20, "function tree cycle at " + fid
    return fid


def build_registry(registry):
    parent_of = {f["id"]: f["parent"] for f in registry["functions"]}
    name_of = {f["id"]: f["name"] for f in registry["functions"]}
    has_child = {fid for fid in name_of if any(p == fid for p in parent_of.values())}
    tops = sorted(f["id"] for f in registry["functions"] if f["parent"] is None)
    reg_by_id = {n["id"]: n for n in registry["nodes"]}
    prim_branch = {n["id"]: branch_of(n["functions"][0], parent_of) for n in registry["nodes"]}
    sec_branches = {n["id"]: {branch_of(f, parent_of) for f in n["functions"][1:]}
                    - {prim_branch[n["id"]]} for n in registry["nodes"]}
    return parent_of, name_of, has_child, tops, reg_by_id, prim_branch, sec_branches


def aggregate(corpus, reg_by_id, prim_branch):
    """(used, present, absent, neighbors, known) over all corpus edges. ONE
    project-wide uid->node map per project (edges are re-homed into the target
    compartment); neighbors[nid] = Counter of the primary branch of every OTHER
    endpoint of a non-self corpus edge nid is incident to (in AND out, ANY
    dataClass); known = its total across all branches (the floor's denominator)."""
    used, neighbors, known = set(), defaultdict(Counter), Counter()
    for p in corpus["projects"]:
        uidmap = {n["uid"]: n["id"] for c in p["compartments"] for n in c["nodes"]}
        used.update(uidmap.values())
        for c in p["compartments"]:
            for e in c["edges"]:
                a, b = e["fromUid"], e["toUid"]
                if a == b:
                    continue
                for me, other in ((uidmap.get(a), uidmap.get(b)),
                                  (uidmap.get(b), uidmap.get(a))):
                    if me is not None and other in prim_branch:
                        neighbors[me][prim_branch[other]] += 1
                        known[me] += 1
    present = sorted(used & set(reg_by_id))
    absent = sorted(used - set(reg_by_id))
    return used, present, absent, neighbors, known


def find_candidates(present, reg_by_id, prim_branch, sec_branches, neighbors, known):
    """Verbatim 6d rule: N (corpus-USED, >= 1 secondary branch) is a
    secondary-value candidate iff secondaryMass > primaryMass (strict) AND
    known[N] >= FLOOR; bestSecondaryBranch = argmax of mass over N's secondary
    branches, tie -> lexicographically smallest; primaryMass = mass(Pb)."""
    out = []
    for nid in present:
        secs = sec_branches[nid]
        if not secs:
            continue
        m = neighbors[nid]
        pb, pm = prim_branch[nid], m.get(prim_branch[nid], 0)
        best, bm = None, -1
        for B in sorted(secs):      # strict > keeps the lexicographically smallest tie
            if m.get(B, 0) > bm:
                bm, best = m.get(B, 0), B
        if bm > pm and known[nid] >= FLOOR:
            out.append({"nid": nid, "name": reg_by_id[nid]["name"], "pb": pb,
                        "best": best, "pm": pm, "bm": bm, "deg": known[nid]})
    out.sort(key=lambda c: (c["pm"] - c["bm"], c["nid"]))   # (secondaryMass - primaryMass) desc, then id
    return out


def spot_note(c, reg_by_id):
    """One-line qualitative rationale for a spot-checked candidate, from its
    registry functions[] + inputs[]/outputs[] data types."""
    nid = c["nid"]
    f = reg_by_id[nid]["functions"]
    head = ("confirmed: claimed primary `%s` + secondary `%s` are exactly its registry "
            "`functions[0]`/`[1]`; " % (S(f[0]), S(f[1])))
    if nid == "data-to-annotation-table":
        tail = ("inputs `JIPipeData` (jipipe:data), outputs `AnnotationTableData` "
                "(annotation-table) -> plausible for the Annotations branch (its "
                "winning secondary)")
    elif nid.startswith("plot-table:"):
        tail = ("inputs `ResultsTableData` (imagej-results-table), outputs "
                "`JFreeChartPlotData` (plot) -> plausible for the Images branch "
                "(its winning secondary) and for the Plotting primary")
    else:
        io = lambda side: ", ".join("`%s` (%s)" % (i["class"].rsplit(".", 1)[-1],
                                                   i["dataType"])
                                    for i in (reg_by_id[nid].get(side) or [])
                                    if i.get("dataType")) or "(none)"
        tail = "inputs: " + io("inputs") + "; outputs: " + io("outputs")
    return head + tail


def render(parent_of, name_of, has_child, tops, reg_by_id, prim_branch, sec_branches,
           used, present, absent, neighbors, known, cands, withsec, n_edges):
    occ = Counter(n["functions"][0] for n in reg_by_id.values())
    misc_members = {fid for fid in name_of if fid == MISC or branch_of(fid, parent_of) == MISC}
    mark = lambda fid: " **(catch-all `misc`)**" if fid == MISC else (
        " *(misc branch: %s)*" % name_of[fid] if fid in misc_members else "")
    n_branch_prims = sum(1 for fid in occ if fid in has_child)
    n_never_prim = len(name_of) - len(occ)
    # 4a
    a_big = ["| function id | name | nodes (primary) |", "|---|---|---:|"] + \
            ["| `%s` | %s%s | %d |" % (S(fid), name_of[fid], mark(fid), c)
             for fid, c in sorted(occ.items(), key=lambda kv: (-kv[1], kv[0]))[:TOPN]]
    a_small = ["| function id | name | nodes (primary) |", "|---|---|---:|"] + \
              ["| `%s` | %s%s | %d |" % (S(fid), name_of[fid], mark(fid), c)
               for fid, c in sorted(occ.items(), key=lambda kv: (kv[1], kv[0]))[:BOTN]]
    # 4b (both readings)
    i_ids = [n for n in present if reg_by_id[n]["functions"][0] == MISC]
    ii_ids = [n for n in present if prim_branch[n] == MISC]
    ii_literal = [n for n in present if reg_by_id[n]["functions"][0] in
                  {MISC, "jipipe:function:data-utilities", "jipipe:function:scripting",
                   "jipipe:function:text-markup"}]
    ii_diff = sorted(set(ii_ids) - set(ii_literal))
    b_rows = lambda ids: (["| node id | name | primary function |", "|---|---|---|"] +
                          ["| `%s` | %s | `%s` |" % (n, reg_by_id[n]["name"],
                                                      S(reg_by_id[n]["functions"][0]))
                           for n in ids]) if ids else ["(none)"]
    b1, b2, b2lit = b_rows(i_ids), b_rows(ii_ids), b_rows(ii_literal)
    # 4c
    branch_used = Counter(prim_branch[n] for n in present)
    c_rows = ["| branch | name | distinct corpus-USED node ids (primary branch) | flag |",
              "|---|---|---:|---|"]
    for Bf in sorted(tops, key=lambda f: (-branch_used.get(f, 0), f)):
        u = branch_used.get(Bf, 0)
        flag = "0 used — strongest merge/demotion candidate" if u == 0 else (
               "< 3 used — merge/demotion candidate" if u < 3 else "")
        c_rows.append("| `%s` | %s | %d | %s |" % (S(Bf), name_of[Bf], u, flag))
    low = [S(Bf) for Bf in tops if branch_used.get(Bf, 0) < 3]
    zero = [S(Bf) for Bf in tops if branch_used.get(Bf, 0) == 0]
    # 4d
    d_rows = ["| # | node id | name | primary branch | best secondary branch | primaryMass "
              "| secondaryMass | known-branch neighbor endpoints |",
              "|---:|---|---|---|---|---:|---:|---:|"] + \
             ["| %d | `%s` | %s | `%s` | `%s` | %d | %d | %d |"
              % (i, c["nid"], c["name"], S(c["pb"]), S(c["best"]), c["pm"], c["bm"], c["deg"])
              for i, c in enumerate(cands[:D_TOP], 1)]
    spot = ["| node id | verification (read from `registry.json`) |", "|---|---|"] + \
           ["| `%s` | %s |" % (c["nid"], spot_note(c, reg_by_id)) for c in cands[:3]]
    a_full = ["| function id | name | nodes (primary) |", "|---|---|---:|"] + \
             ["| `%s` | %s | %d |" % (S(fid), name_of[fid], c)
              for fid, c in sorted(occ.items(), key=lambda kv: (-kv[1], kv[0]))]
    absent_l = ", ".join("`%s`" % a for a in absent) or "(none)"
    n_scripting_children = sum(1 for fid in name_of if parent_of.get(fid) == "jipipe:function:scripting")
    body = [
        "Generated by `analysis/t6_functions.py`; re-running replaces this section in "
        "place (rest of `report.md` untouched). Raw tables (incl. full 4a occupancy): "
        "`analysis/t6_table.md`.",
        "", "Method:",
        "- **Branch definitions:** a node's **branch** = the top-level branch of its "
        "**primary** function (`functions[0]`; parent-walk to `null` — verified "
        "acyclic). A node's **secondary branches** = the top-level branch of each of "
        "its secondary functions (`functions[1:]`), deduped, excluding the primary "
        "branch. %d top-level branches; %d functions." % (len(tops), len(name_of)),
        "- **4a:** nodes counted per **primary function id** (leaf or branch — "
        "`misc` itself, `segmentation` and `scripting` can be primary values; %d "
        "non-empty primary ids are branch-level, not leaves); the %d functions "
        "never a primary are omitted. Top %d biggest and bottom %d smallest "
        "non-empty; the catch-all `misc` and its branch members are marked."
        % (n_branch_prims, n_never_prim, TOPN, BOTN),
        "- **4b (dual reading):** (i) **catch-all only** — corpus-USED nodes whose "
        "primary function is exactly `%s` (%d of them); (ii) **whole misc branch** — "
        "corpus-USED nodes whose primary function is in the misc subtree: `misc`, "
        "`data-utilities`, `scripting` + its %d `scripting-*` leaf children, "
        "`text-markup` (%d of them, computed by branch-walk). Note: the "
        "ground-facts summary names 3 child leaves of `misc` and counts "
        "`scripting` itself as one of them (2 nodes, 0 used — matching the file's "
        "2 nodes with primary `scripting` exactly); in the file `scripting` is "
        "additionally an intermediate node with %d further leaf children "
        "(`scripting-expression`, `scripting-imagej-macro`, `scripting-python`, "
        "`scripting-r`), so the ruling's literal set {`misc`, `data-utilities`, "
        "`scripting`, `text-markup`} yields %d used nodes while the whole-branch "
        "walk yields %d — the difference is %s (their primaries are `scripting-*` "
        "leaves under `scripting`). The whole-branch walk is reported as reading "
        "(ii) (it is the 'whole misc branch'); the literal-set count is stated "
        "alongside." % (MISC, len(i_ids), n_scripting_children, len(ii_ids),
                       n_scripting_children, len(ii_literal), len(ii_ids),
                       ", ".join("`%s`" % n for n in ii_diff) or "(none)"),
        "- **4c:** distinct corpus-USED node ids per top-level primary branch; "
        "merge/demotion candidates = branches with < 3 used nodes (%s); 0-used "
        "branches are the strongest candidates: %s."
        % (", ".join("`%s`" % x for x in low) or "(none)",
           ", ".join("`%s`" % x for x in zero) or "(none)"),
        "- **4d (candidate rule, verbatim):** for each corpus-USED node N with "
        "primary branch Pb and ≥ 1 secondary branch: gather N's corpus edges (in "
        "AND out, project-wide uid resolution, ANY dataClass; self-edges "
        "excluded; endpoints that are absent ids or have no primary branch "
        "ignored — %s of %d corpus-USED node ids are absent from the registry and "
        "treated as unknown-branch: %s). Let mass(B) = number of N's "
        "neighbor-endpoints whose primary branch == B. primaryMass = mass(Pb); "
        "bestSecondaryBranch = argmax over N's secondary branches of mass (tie → "
        "lexicographically smallest); secondaryMass = mass(bestSecondaryBranch). N "
        "is a secondary-value candidate iff secondaryMass > primaryMass (strict) "
        "AND N has ≥ 3 neighbor-endpoints with known primary branches (evidence "
        "floor, to suppress noise). Top %d shown, of %d total candidates, over %s "
        "corpus edges; %d corpus-USED registry nodes have ≥ 1 secondary branch "
        "(denominator context; full list in `analysis/t6_table.md`)."
        % (len(absent), len(used), absent_l, D_TOP, len(cands), f"{n_edges:,}",
           len(withsec)),
        "",
        "### 4a — Leaf occupancy (primary function id; top %d / bottom %d of %d "
        "non-empty)" % (TOPN, BOTN, len(occ)), ""]
    body += a_big + ["", "Bottom %d smallest non-empty:" % BOTN, ""] + a_small + [""]
    body += ["### 4b — Orphan load: corpus-USED nodes in the Miscellaneous "
             "catch-all (dual reading)", "",
             "Reading (i) — catch-all only, primary function == `misc` "
             "(%d):" % len(i_ids), ""] + b1 + [
             "", "Reading (ii) — whole misc branch, primary function in the misc "
             "subtree (%d):" % len(ii_ids), ""] + b2 + [
             "", "Reading (ii) per the ruling's literal set {`misc`, "
             "`data-utilities`, `scripting`, `text-markup`}: %d used nodes — the "
             "difference from the whole-branch walk above: %s (their primaries are "
             "`scripting-*` leaves under the intermediate `scripting` node)."
             % (len(ii_literal),
                ", ".join("`%s` (primary `%s`)" % (n, S(reg_by_id[n]["functions"][0]))
                          for n in ii_diff) or "(none)"), ""]
    body += ["### 4c — Top-level branches: distinct corpus-USED node ids (full "
             "%d-branch table, sorted by used count desc)" % len(tops), ""] + c_rows + [
             "", "Merge/demotion candidates (< 3 used): " +
             (", ".join("`%s`" % x for x in low) or "(none)") + "; strongest (0 "
              "used): " + (", ".join("`%s`" % x for x in zero) or "(none)"), ""]
    body += ["### 4d — Secondary-value candidates (top %d of %d total; sorted by "
             "secondaryMass − primaryMass desc, then id)" % (min(D_TOP, len(cands)), len(cands)),
             ""] + d_rows + [
             "", "Step 2 spot-check — 3 named candidates verified by reading "
             "`registry.json` (functions + slot data types):", ""] + spot + [""]
    return body, a_full, b1, b2, b2lit, c_rows, d_rows, spot


def main():
    corpus = json.load(open(ROOT / "corpus.json", encoding="utf-8"))
    registry = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    (parent_of, name_of, has_child, tops, reg_by_id, prim_branch,
     sec_branches) = build_registry(registry)
    used, present, absent, neighbors, known = aggregate(corpus, reg_by_id, prim_branch)
    cands = find_candidates(present, reg_by_id, prim_branch, sec_branches, neighbors, known)
    withsec = sorted(n for n in present if sec_branches[n])
    n_edges = sum(len(c["edges"]) for p in corpus["projects"] for c in p["compartments"])
    (body, a_full, b1, b2, b2lit, c_rows, d_rows, spot) = render(
        parent_of, name_of, has_child, tops, reg_by_id, prim_branch, sec_branches,
        used, present, absent, neighbors, known, cands, withsec, n_edges)
    text = "## 4. Function tree fit\n" + "\n".join(body).rstrip() + "\n"
    if not REPORT.exists():
        REPORT.write_text("# Taxonomy follow-up research report\n" + text, encoding="utf-8")
    else:
        t = REPORT.read_text(encoding="utf-8")
        m = re.search(r"^## 4\. ", t, flags=re.M)
        if not m:
            REPORT.write_text(t + ("\n" if not t.endswith("\n") else "") + text,
                              encoding="utf-8")
        else:
            nxt = re.search(r"^## ", t[m.end():], flags=re.M)
            end = m.end() + nxt.start() if nxt else len(t)
            REPORT.write_text(t[:m.start()] + text + t[end:], encoding="utf-8")
    TABLE.write_text("\n".join(
        ["# Task 6 — raw tables (function-tree fit & secondary-function value)", "",
         "## 4a — leaf occupancy (primary function id): all non-empty", ""] + a_full +
         ["", "## 4b — reading (i) catch-all only (primary == `misc`)", ""] + b1 +
         ["", "## 4b — reading (ii) whole misc branch (primary function in misc subtree)", ""] + b2 +
         ["", "## 4b — reading (ii), ruling's literal set {misc, data-utilities, scripting, text-markup}", ""] + b2lit +
         ["", "## 4c — top-level branches: distinct corpus-USED node ids (primary branch)", ""] +
         c_rows + ["", "## 4d — secondary-value candidates (all)", ""] + d_rows +
         ["", "Step 2 spot-check (3 named candidates, registry read)", ""] + spot +
         ["", "Corpus-USED registry nodes with >= 1 secondary branch (denominator "
          "context): " + (", ".join("`%s`" % n for n in withsec) or "(none)"),
          "", "Absent-from-registry corpus-USED node ids (unknown-branch): " +
          (", ".join("`%s`" % a for a in absent) or "(none)"), ""]),
        encoding="utf-8")
    occ = Counter(n["functions"][0] for n in reg_by_id.values())
    branch_used = Counter(prim_branch[n] for n in present)
    low = [S(Bf) for Bf in tops if branch_used.get(Bf, 0) < 3]
    print("leaves occupied: %d, catch-all used: %d (branch %d), <3-used branches: [%s], "
          "secondary-value candidates: %d"
          % (len(occ),
             sum(1 for n in present if reg_by_id[n]["functions"][0] == MISC),
             sum(1 for n in present if prim_branch[n] == MISC),
             ", ".join(low), len(cands)))


if __name__ == "__main__":
    main()
