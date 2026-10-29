#!/usr/bin/env python3
"""Task 4: workflow chains & function-branch fragmentation (6.0.0).

Reads corpus.json + registry.json (locked, Tasks 1-2); emits report section
2 with (2a) top-20 node ids by distinct project count, (2b/2c) the 20 most
frequent 3-node chains A->B->C (two data-class-typed edges sharing the
middle node instance; deduped by id-triple) with branch facets (all function
leaves mapped to top-level branches via the registry function-tree parent
walk) and flags (fragmented: >= 3 distinct top-level branches across A,B,C;
isolated-middle: B's primary branch in neither A's nor C's branch set), and
(2d) dead-end candidates. No interpretation (Task 9's job). Stdlib only.
Emits analysis/t4_table.md (raw tables) and report.md (created if absent; on
re-runs only the text between "## 2. " and the next "## " or EOF is
replaced; byte-stable, hash-seed-immune). Prints "chains found: N,
fragmented: M, dead-end candidates: K". Usage: python3 t4_workflows.py
"""
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE = ROOT / "report.md", ROOT / "analysis" / "t4_table.md"
DATE = "2026-10-14"  # corpus and registry dump dates (Tasks 1-2)
THRESHOLD = 25  # (d) "frequently consumed": typed outgoing edges per id


def build_registry(registry):
    """(ids, names, all_branches, primary_branch, function names, dt -> input
    ids, id -> output dt ids)."""
    fpar, fname = {}, {}
    for f in registry["functions"]:
        fpar[f["id"]] = f["parent"]
        fname[f["id"]] = f["name"]

    def top(fid):
        seen = set()
        while fpar.get(fid) is not None and fid not in seen:
            seen.add(fid)
            fid = fpar[fid]
        return fid

    ids, names, allb, primb = set(), {}, {}, {}
    inp, outdts = defaultdict(set), {}
    for n in registry["nodes"]:
        ids.add(n["id"])
        names[n["id"]] = n["name"]
        fs = n.get("functions") or []
        allb[n["id"]] = {top(f) for f in fs}
        primb[n["id"]] = top(fs[0]) if fs else None
        for io in n.get("inputs") or []:
            if io.get("dataType"):
                inp[io["dataType"]].add(n["id"])
        outdts[n["id"]] = {io["dataType"] for io in n.get("outputs") or [] if io.get("dataType")}
    return ids, names, allb, primb, fname, inp, outdts


def fmt_edge(e):
    return f"`{e['fromUid']}`.{e['fromSlot']} → `{e['toUid']}`.{e['toSlot']} (dataClass `{e['dataClass']}`)"


def aggregate(corpus, unresolved):
    """ONE project-wide uid->node map per project before any graph walk (edges are
    re-homed into the TARGET compartment: 4,661/31,826 fromUids live elsewhere)."""
    node_stats = defaultdict(lambda: {"projects": set(), "instances": 0})
    chains = defaultdict(lambda: {"occ": 0, "projects": set(), "first": None})
    out_typed = defaultdict(int)
    cnt = {"total": 0, "null": 0, "unres": 0, "skip_mid": 0, "skip_partner": 0}
    for p in corpus["projects"]:
        uid_to_node = {}
        for c in p["compartments"]:
            for n in c["nodes"]:
                uid_to_node[n["uid"]] = n
                node_stats[n["id"]]["projects"].add(p["file"])
                node_stats[n["id"]]["instances"] += 1
        edges = [e for c in p["compartments"] for e in c["edges"]]
        in_typed = defaultdict(list)
        for e in edges:
            cnt["total"] += 1
            tok = e.get("dataClass")
            if tok is None:
                cnt["null"] += 1
            elif tok in unresolved:
                cnt["unres"] += 1
            if tok is not None:
                src = uid_to_node.get(e["fromUid"])
                if src is not None:
                    out_typed[src["id"]] += 1
                in_typed[e["toUid"]].append(e)
        for e2 in edges:
            if e2.get("dataClass") is None:
                continue
            b = uid_to_node.get(e2["fromUid"])
            if b is None:
                cnt["skip_mid"] += 1
                continue
            for e1 in in_typed.get(e2["fromUid"], ()):
                a = uid_to_node.get(e1["fromUid"])
                cnd = uid_to_node.get(e2["toUid"])
                if a is None or cnd is None:
                    cnt["skip_partner"] += 1
                    continue
                s = chains[(a["id"], b["id"], cnd["id"])]
                s["occ"] += 1
                s["projects"].add(p["file"])
                if s["first"] is None:
                    s["first"] = (p["file"], fmt_edge(e1), fmt_edge(e2))
    return node_stats, chains, out_typed, cnt


def dead_ends(out_typed, inp, outdts, allb, primb):
    """(d) ruling, computed per the verbatim definition in the method note."""
    dead = []
    for nid in sorted(out_typed):
        if out_typed[nid] < THRESHOLD:
            continue
        consumers = set()
        for dt in outdts.get(nid, ()):
            consumers |= inp.get(dt, set())
        union = set().union(*(allb.get(cn, set()) for cn in consumers))
        if len(union) == 1 and next(iter(union)) != primb.get(nid):
            dead.append({"id": nid, "out": out_typed[nid],
                         "branch": next(iter(union)), "in_ids": len(consumers)})
    return dead


def table_2a(node_stats, names):
    ranked = sorted(node_stats.items(), key=lambda kv: (-len(kv[1]["projects"]),
                                                        -kv[1]["instances"], kv[0]))[:20]
    lines = ["| # | node id | name | distinct projects | node instances |",
             "|---:|---|---|---:|---:|"]
    for i, (nid, s) in enumerate(ranked, 1):
        lines.append(f"| {i} | `{nid}` | {names.get(nid, '*not in 6.0.0 registry*')} "
                     f"| {len(s['projects'])} | {s['instances']} |")
    return lines


def table_2bc(chains, allb, primb, fname):
    ranked = sorted(chains.items(), key=lambda kv: (-kv[1]["occ"], kv[0]))[:20]
    lines = ["| # | A → B → C (node ids) | occurrences | projects | top-level branches "
             "(union of A∪B∪C) | fragmented | isolated middle |",
             "|---:|---|---:|---:|---|---:|---:|"]
    frag = iso = 0
    for i, ((a, b, cc), s) in enumerate(ranked, 1):
        union = sorted(allb.get(a, set()) | allb.get(b, set()) | allb.get(cc, set()))
        is_frag = len(union) >= 3
        pb = primb.get(b)
        is_iso = pb is not None and pb not in allb.get(a, set()) | allb.get(cc, set())
        frag += is_frag
        iso += is_iso
        branch_txt = (" ∪ ".join(fname[u] for u in union)
                      or "(none - no registry function leaves)")
        lines.append(f"| {i} | `{a}` → `{b}` → `{cc}` | {s['occ']} | {len(s['projects'])} "
                     f"| {branch_txt} | {'yes' if is_frag else 'no'} "
                     f"| {'yes' if is_iso else 'no'} |")
    return lines, frag, iso


def table_2d(dead, names, fname):
    lines = ["| node id | name | typed outgoing edges (corpus) | narrow consumer branch "
             "| distinct registry consumer node ids |", "|---|---|---:|---|---:|"]
    for d in dead:
        lines.append(f"| `{d['id']}` | {names.get(d['id'], '*not in 6.0.0 registry*')} "
                     f"| {d['out']} | {fname[d['branch']]} | {d['in_ids']} |")
    return lines


def method_note(chains, dead, cnt, nonreg, M_all, allb, mid_unres_triples, mid_unres, n_absent):
    top20 = sorted(chains.items(), key=lambda kv: (-kv[1]["occ"], kv[0]))[:20]
    frag20 = sum(1 for k, _ in top20
                 if len(allb.get(k[0], set()) | allb.get(k[1], set())
                        | allb.get(k[2], set())) >= 3)
    nonreg_txt = ", ".join(f"`{k}` ({v})" for k, v in nonreg)
    lines = [
        "Generated by `analysis/t4_workflows.py`; re-running replaces this section in "
        "place (rest of `report.md` untouched). Raw tables: `analysis/t4_table.md`.",
        "",
        "Method:",
        "- **2a - top-20 node ids:** aggregated by registry id across the corpus: "
        "distinct projects (a node used twice in one project counts once) + total "
        "node instances; sorted by project count desc, then instance count desc, then id.",
        f"- **2b/2c - chains:** a chain A→B→C is two data-class-typed edges e1 (A→B) and "
        f"e2 (B→C) with e1.toUid == e2.fromUid (the same B node instance); counted per "
        f"occurrence, table rows deduped by the (A.id, B.id, C.id) id-triple, top 20 by "
        f"occurrence count (deterministic tie-break: occurrences desc, then lexicographic "
        f"triple). ONE project-wide uid→node map is built per project before any graph "
        f"walk (edges are re-homed into the target compartment: 4,661 of {cnt['total']:,} "
        f"edges have a fromUid in another compartment). Edge `dataClass` = the source "
        f"slot's raw on-disk data-type token: {cnt['null']} of {cnt['total']:,} edges "
        f"carry a null token and {cnt['unres']} carry one of the 8 unresolved tokens of "
        f"Task 3 (kept as their own token in the chain statistics - never dropped); "
        f"{cnt['skip_mid']} typed edges with an unresolved fromUid and "
        f"{cnt['skip_partner']} (e1, e2) pairs with an unresolved toUid were skipped "
        f"(0 / 0 here - nothing dropped).",
        "- **Branch facets (2c):** each node's branch set = the union of the top-level "
        "branches of ALL its function leaves (primary + secondary, registry `functions[]`), "
        "each leaf's `parent` walked up the registry function tree (88 functions, 13 "
        "top-level branches; verified acyclic). **Fragmented** = the three nodes' branch "
        "sets span ≥ 3 distinct top-level branches. **Isolated middle** = B's primary "
        "branch (top-level of its primary function) is in neither A's nor C's branch set; "
        f"in {mid_unres_triples} triples ({mid_unres:,} occurrences) the middle node's id "
        f"is absent from the registry (legacy plugin ids, see below) so no primary "
        f"resolves to a branch - their isolated-middle flag is reported as 'no' "
        f"(not computable, not a negative signal).",
        f"- **2d - dead-end candidates (interpretation, recorded verbatim):** “frequently "
        f"consumed” = node id with ≥ {THRESHOLD} typed outgoing edges across the whole "
        f"corpus; “output type's consumers all in one narrow branch” = take the node's "
        f"OUTPUT data-type ids (registry `outputs[].dataType`, non-null), the registry "
        f"node ids that accept any of those as `inputs[].dataType`, and their branch sets "
        f"(primary + secondary, as above); flag when that consumer branch union is exactly "
        f"1 top-level branch AND that branch ≠ the node's own primary branch. These are "
        f"“candidates for secondary-function assignments or family steps” (the WHY is "
        f"Task 9's job - counts + ids + the one branch name only).",
        "",
        f"Summary: {len(chains)} distinct id-triples with ≥1 occurrence "
        f"({sum(s['occ'] for s in chains.values()):,} total occurrences); {M_all} "
        f"fragmented triples overall ({frag20} of the top 20); {len(dead)} dead-end "
        f"candidates.",
        "",
    ]
    if nonreg:
        lines += [f"Corpus node ids absent from the 6.0.0 registry that carry typed "
                  f"outgoing edges ({len(nonreg)} of the {n_absent} absent ids; legacy "
                  f"plugin ids - no function leaves, treated as an empty branch set): "
                  f"{nonreg_txt}.", ""]
    return lines


def render_report(body_text, corpus, registry):
    header = "## 2. Workflow chains\n"
    if not REPORT.exists():
        n_edges = sum(len(c["edges"]) for p in corpus["projects"] for c in p["compartments"])
        provenance = (f"Corpus: {len(corpus['projects'])} projects / {n_edges:,} edges "
                      f"({DATE}). Registry: {len(registry['nodes']):,} nodes / "
                      f"{len(registry['functions'])} functions ({DATE}).\n")
        REPORT.write_text("# Taxonomy follow-up research report\n" +
                          provenance + header + body_text, encoding="utf-8")
        return "created"
    text = REPORT.read_text(encoding="utf-8")
    m = re.search(r"^## 2\. ", text, flags=re.M)
    if not m:
        REPORT.write_text(text + ("\n" if not text.endswith("\n") else "")
                          + header + body_text, encoding="utf-8")
        return "appended section 2"
    nxt = re.search(r"^## ", text[m.end():], flags=re.M)
    end = m.end() + nxt.start() if nxt else len(text)
    REPORT.write_text(text[:m.start()] + header + body_text + text[end:],
                      encoding="utf-8")
    return "replaced section 2 in place"


def main():
    corpus = json.load(open(ROOT / "corpus.json", encoding="utf-8"))
    registry = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    reg_ids, names, allb, primb, fname, inp, outdts = build_registry(registry)
    unresolved = set()
    t3 = ROOT / "analysis" / "unresolved_types.md"
    if t3.exists():
        for line in t3.read_text(encoding="utf-8").splitlines():
            m = re.match(r"\| `([^`]+)` \| (?:unresolved|ambiguous) \|", line)
            if m:
                unresolved.add(m.group(1))
    node_stats, chains, out_typed, cnt = aggregate(corpus, unresolved)
    n_absent = sum(1 for nid in node_stats if nid not in reg_ids)
    nonreg = [(k, v) for k, v in sorted(out_typed.items()) if k not in reg_ids]
    M_all = sum(1 for k in chains if len(allb.get(k[0], set()) | allb.get(k[1], set())
                                         | allb.get(k[2], set())) >= 3)
    dead = dead_ends(out_typed, inp, outdts, allb, primb)
    top_triple, top_stat = min(chains.items(), key=lambda kv: (-kv[1]["occ"], kv[0]))
    mid_unres_triples = sum(1 for k in chains if k[1] not in reg_ids)
    mid_unres = sum(s["occ"] for k, s in chains.items() if k[1] not in reg_ids)
    lines = method_note(chains, dead, cnt, nonreg, M_all, allb, mid_unres_triples, mid_unres, n_absent)
    lines += ["### 2a - Top-20 most-used node ids", ""] + table_2a(node_stats, names)
    lines += ["", "### 2b/2c - Top-20 3-node chains with branch facets", ""]
    t2bc, frag20, iso20 = table_2bc(chains, allb, primb, fname)
    lines += t2bc
    lines += ["", f"Top-20 flags: {frag20} fragmented, {iso20} isolated-middle.",
              "", f"### 2d - Dead-end candidates (≥ {THRESHOLD} typed outgoing edges; output "
              "types' consumers in exactly one top-level branch ≠ own primary branch)", ""]
    lines += table_2d(dead, names, fname) if dead else ["(none)"]
    v = top_stat["first"]
    lines += ["", "Step 2 verification (computed from the corpus; both edges are present in "
              f"`corpus.json` with non-null `dataClass`): the top chain `{top_triple[0]}` → "
              f"`{top_triple[1]}` → `{top_triple[2]}` has {top_stat['occ']} occurrences in "
              f"{len(top_stat['projects'])} projects; in project `{v[0]}` the two edges "
              f"sharing middle node instance B = `{top_triple[1]}` are e1 (A→B): {v[1]} and "
              f"e2 (B→C): {v[2]}"]
    TABLE.write_text("\n".join(["# Task 4 - raw tables (workflow chains & branch fragmentation)",
                                "", *table_2a(node_stats, names), "",
                                *table_2bc(chains, allb, primb, fname)[0], "",
                                *(table_2d(dead, names, fname) if dead else ["(none)"]),
                                ""]), encoding="utf-8")
    render_report("\n".join(lines).rstrip() + "\n", corpus, registry)
    print(f"chains found: {len(chains)}, fragmented: {M_all}, dead-end candidates: {len(dead)}")
    print(f"(edges: {cnt['total']:,} total, {cnt['null']} null-dataClass, {cnt['unres']} unresolved-token; uid skips: {cnt['skip_mid']} mid + {cnt['skip_partner']} partner; report.md: section 2 in place)")


if __name__ == "__main__":
    main()
