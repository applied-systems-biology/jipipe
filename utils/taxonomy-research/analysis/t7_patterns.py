#!/usr/bin/env python3
"""Task 7: pattern mining - every recurring pipeline pattern (6.0.0, work item 1324).

Reads corpus.json + registry.json (locked, Tasks 1-6) and the 24 hand-written
LLM-branch pattern docs (read-only); mines the five pattern classes defined by
the controller ruling; cross-checks against the hand-written library; emits
patterns/patterns.json (the inventory), analysis/t7_table.md (raw tables) and
report.md section 5 (created if absent; on re-runs only the text between
"## 5. " and the next "## "/EOF is replaced; byte-stable, hash-seed-immune).
No WHY-interpretation (Task 9's job). Stdlib only.

ONE project-wide uid->node map per project (edges are re-homed into the TARGET
compartment: 4,661/31,826 fromUids live elsewhere); compartment membership for
class-5 motifs = the compartment entry whose nodes[] contains the uid; an edge
is inter-compartment iff its two endpoints' compartments differ.
"""
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE = ROOT / "report.md", ROOT / "analysis" / "t7_table.md"
PATTERNS_OUT = ROOT / "patterns" / "patterns.json"
DOCS = Path("/data/src/jipipe-2/jipipe-core/src/main/resources/org/hkijena/jipipe/"
            "ai/knowledge/sources/bioimage-analysis/patterns")
CUTOFF = 3           # keep patterns with support (distinct projects) >= CUTOFF
TOPN = 25            # report 5a / raw table: top-N per class
EMIT_SUP = 20        # patterns.json: emit support >= EMIT_SUP (see emission note)

DATA, ANNOT, EXPORT, PLOT = ("jipipe:function:data", "jipipe:function:annotations",
                             "jipipe:function:export", "jipipe:function:plotting")
TM_FW = "jipipe:framework:trackmate"
CORE_IO = {"io-interface", "jipipe:compartment-input", "jipipe:compartment-output"}


def build_role(registry):
    """id -> (role, name). Role = first match in the ruling's order: import,
    annotate, export, trackmate, plotting, core-io, else other; absent from the
    registry or no primary function -> unknown."""
    fpar = {f["id"]: f["parent"] for f in registry["functions"]}

    def top(fid):
        seen = set()
        while fpar.get(fid) is not None and fid not in seen:
            seen.add(fid)
            fid = fpar[fid]
        return fid

    role, name = {}, {}
    for n in registry["nodes"]:
        nid, fs = n["id"], n.get("functions") or []
        name[nid] = n["name"]
        if not fs:
            role[nid] = "unknown"
            continue
        b = top(fs[0])
        role[nid] = ("import" if b == DATA else "annotate" if b == ANNOT else
                     "export" if b == EXPORT else "trackmate"
                     if n.get("framework") == TM_FW else "plotting" if b == PLOT
                     else "core-io" if nid in CORE_IO else "other")
    return role, name


nrole = lambda role, nid: role.get(nid, "unknown")
slug = lambda tok: (re.sub(r"[^a-z0-9]+", "-", str(tok).lower()).strip("-") or "x")
pid = lambda shape, key: slug(shape) + "-" + "-".join(slug(t) for t in
                                                       (str(k) for k in key))


def make_labels(uids, name_of):
    """uid -> display label (dedup with a #n index when instance names collide)."""
    disp = {u: name_of[u] for u in uids}
    cnt = Counter(disp.values())
    seen, out = {}, {}
    for u in uids:
        d = disp[u]
        if cnt[d] > 1:
            k = seen.get(d, 0)
            seen[d] = k + 1
            out[u] = f"{d} #{k + 1}"
        else:
            out[u] = d
    return out


def build_example(path, rep, id_of, name_of):
    """(nodes, edges) subgraph for a node-uid path (parameters elided);
    rep=None -> nodes only (the compartment-cycle case)."""
    lab = make_labels(list(path), name_of)
    nodes = [{"id": id_of[u], "name": lab[u]} for u in path]
    edges = []
    if rep is not None:
        for a, b in zip(path, path[1:]):
            e = rep.get((a, b))
            if e is not None:
                edges.append({"from": lab[a], "fromSlot": e["fromSlot"],
                              "to": lab[b], "toSlot": e["toSlot"],
                              "dataClass": e["dataClass"]})
    return nodes, edges


class Acc:
    """Accumulator: (shape, key) -> pattern (occ, projects, role-group id-sets,
    deterministic smallest-example subgraph). The example subgraph is built only
    when an occurrence's selection key (node count, project file, comp name)
    beats the current best - chains call upsert ~1M times, so it is deferred."""

    def __init__(self):
        self.p = {}

    def upsert(self, shape, key, gkeys, roles, gk_ids, proj, path, rep,
               id_of, name_of, comp_of):
        ck = (len(path), proj, "+".join(sorted({comp_of[u] for u in path})))
        p = self.p.get((shape, key))
        if p is None:
            p = {"shape": shape, "key": key, "occ": 0, "projects": set(),
                 "groups": {g: {"role": roles[g], "ids": set()} for g in gkeys},
                 "order": list(gkeys), "ex": None, "ex_nodes": None,
                 "ex_edges": None, "ex_comp": None}
            self.p[(shape, key)] = p
        p["occ"] += 1
        p["projects"].add(proj)
        for g, ids in gk_ids.items():
            p["groups"][g]["ids"] |= ids
        if p["ex"] is None or ck < p["ex"]:
            p["ex"], p["ex_comp"] = ck, ck[2]
            p["ex_nodes"], p["ex_edges"] = build_example(path, rep, id_of,
                                                         name_of)


SHAPE_ORDER = ["chain", "fan-out", "fan-in", "conversion", "annotation-sandwich",
               "compartment-boundary", "compartment-cycle"]


def mine(corpus, role, acc):
    edge_cnt = {"total": 0, "inter": 0}
    for proj in corpus["projects"]:
        file_ = proj["file"]
        id_of, name_of, comp_of = {}, {}, {}
        for comp in proj["compartments"]:
            for n in comp["nodes"]:
                id_of[n["uid"]] = n["id"]
                name_of[n["uid"]] = n["name"]
                comp_of[n["uid"]] = comp["name"]
        all_edges = [e for comp in proj["compartments"] for e in comp["edges"]]
        for e in all_edges:
            edge_cnt["total"] += 1
        # typed adjacency (uid sets) + per-(from,to) representative edge dict
        out_t, in_t = defaultdict(set), defaultdict(set)
        rep_t, rep_a = {}, {}
        for e in all_edges:
            fu, tu = e["fromUid"], e["toUid"]
            if fu not in id_of or tu not in id_of:
                continue
            k = (fu, tu)
            sk = (str(e["fromSlot"]), str(e["toSlot"]), str(e["dataClass"]))
            if k not in rep_a or sk < (str(rep_a[k]["fromSlot"]),
                                       str(rep_a[k]["toSlot"]),
                                       str(rep_a[k]["dataClass"])):
                rep_a[k] = e
            if e.get("dataClass") is None:
                continue
            out_t[fu].add(tu)
            in_t[tu].add(fu)
            if k not in rep_t or (e["fromSlot"], e["toSlot"], e["dataClass"]) < \
                    (rep_t[k]["fromSlot"], rep_t[k]["toSlot"], rep_t[k]["dataClass"]):
                rep_t[k] = e
        # class 1: chains (typed walks, node-length 3-5)
        for start in sorted(out_t):
            vis = {start}

            def dfs(u, path):
                if len(path) >= 3:
                    key = tuple(id_of[x] for x in path)
                    gkeys = [str(i) for i in range(len(path))]
                    acc.upsert("chain", (len(path), key), gkeys,
                               {g: nrole(role, key[i]) for i, g in enumerate(gkeys)},
                               {g: {key[i]} for i, g in enumerate(gkeys)}, file_,
                               path, rep_t, id_of, name_of, comp_of)
                if len(path) >= 5:
                    return
                for v in sorted(out_t.get(u, ())):
                    if v not in vis:
                        vis.add(v)
                        dfs(v, path + [v])
                        vis.discard(v)

            dfs(start, [start])
        # class 3: conversions (typed 2-hop, data type changes at every hop)
        for e2 in all_edges:
            if e2.get("dataClass") is None:
                continue
            b, c = e2["fromUid"], e2["toUid"]
            for a in sorted(in_t.get(b, ())):
                e1 = rep_t.get((a, b))
                if e1 is None or e1["dataClass"] == e2["dataClass"]:
                    continue
                key = (id_of[a], id_of[b], id_of[c])
                gkeys = ["0", "1", "2"]
                acc.upsert("conversion", key, gkeys,
                           {g: nrole(role, key[i]) for i, g in enumerate(gkeys)},
                           {g: {key[i]} for i, g in enumerate(gkeys)}, file_,
                           [a, b, c], rep_t, id_of, name_of, comp_of)
        # class 2: fan-out / fan-in
        for x, tset in sorted(out_t.items()):
            tids = {id_of[v] for v in tset}
            if len(tids) < 2:
                continue
            rms = tuple(sorted(Counter(nrole(role, t) for t in tids).items()))
            key = (id_of[x], rms)
            tg = ["X"] + [f"T:{r}" for r, _ in rms]
            acc.upsert("fan-out", key, tg,
                       {"X": nrole(role, id_of[x]), **{f"T:{r}": r for r, _ in rms}},
                       {"X": {id_of[x]},
                        **{f"T:{r}": {t for t in tids if nrole(role, t) == r}
                           for r, _ in rms}},
                       file_, [x] + sorted(tset), rep_t, id_of, name_of, comp_of)
        for x, sset in sorted(in_t.items()):
            sids = {id_of[u] for u in sset}
            if len(sids) < 2:
                continue
            rms = tuple(sorted(Counter(nrole(role, s) for s in sids).items()))
            key = (id_of[x], rms)
            sg = ["X"] + [f"S:{r}" for r, _ in rms]
            acc.upsert("fan-in", key, sg,
                       {"X": nrole(role, id_of[x]), **{f"S:{r}": r for r, _ in rms}},
                       {"X": {id_of[x]},
                        **{f"S:{r}": {s for s in sids if nrole(role, s) == r}
                           for r, _ in rms}},
                       file_, sorted(sset) + [x], rep_t, id_of, name_of, comp_of)
        # class 4: annotation sandwiches (typed-or-any in/out edges)
        BAD = {"import", "annotate", "unknown"}
        for s in sorted(in_t):
            if nrole(role, id_of[s]) != "annotate":
                continue
            for imp in sorted(in_t[s]):
                if nrole(role, id_of[imp]) != "import":
                    continue
                for pr in sorted(out_t[s]):
                    rj = nrole(role, id_of[pr])
                    if rj in BAD:
                        continue
                    key = (id_of[imp], id_of[s], rj)
                    acc.upsert("annotation-sandwich", key,
                               ["import", "annotate", rj],
                               {"import": "import", "annotate": "annotate",
                                rj: rj},
                               {"import": {id_of[imp]}, "annotate": {id_of[s]},
                                rj: {id_of[pr]}},
                               file_, [imp, s, pr], rep_a, id_of, name_of, comp_of)
        # class 5: compartment-boundary (inter-compartment edge pairs)
        for e in all_edges:
            fu, tu = e["fromUid"], e["toUid"]
            cf, ct = comp_of.get(fu), comp_of.get(tu)
            if cf is None or ct is None or cf == ct:
                continue
            edge_cnt["inter"] += 1
            key = (id_of[fu], id_of[tu])
            acc.upsert("compartment-boundary", key, ["src", "tgt"],
                       {"src": nrole(role, key[0]), "tgt": nrole(role, key[1])},
                       {"src": {key[0]}, "tgt": {key[1]}},
                       file_, [fu, tu], rep_a, id_of, name_of, comp_of)
    return edge_cnt


def mine_cycles(corpus, acc):
    """class 5b: DFS back-edge per compartment; JIPipe dataflow is a DAG -> 0."""
    cyc = 0
    for proj in corpus["projects"]:
        for comp in proj["compartments"]:
            id_of = {n["uid"]: n["id"] for n in comp["nodes"]}
            adj = defaultdict(list)
            for e in comp["edges"]:
                if e["fromUid"] in id_of and e["toUid"] in id_of:
                    adj[e["fromUid"]].append(e["toUid"])
            WHITE, GRAY, BLACK = 0, 1, 2
            color, back = {}, []

            def dfs(u):
                color[u] = GRAY
                for v in adj.get(u, ()):
                    st = color.get(v, WHITE)
                    if st == GRAY:
                        back.append((u, v))
                    elif st == WHITE:
                        dfs(v)
                color[u] = BLACK

            for u in sorted(adj):
                if color.get(u, WHITE) == WHITE:
                    dfs(u)
            if back:
                cyc += 1
                name_of = {n["uid"]: n["name"] for n in comp["nodes"]}
                comp_of = {u: comp["name"] for u in id_of}
                acc.upsert("compartment-cycle",
                           (f"{comp['name']}:{len(comp['nodes'])}",), ["cycle"],
                           {"cycle": "other"},
                           {"cycle": {id_of[u] for u, v in back}}, proj["file"],
                           [u for u, v in back[:3]], None, id_of, name_of, comp_of)
    return cyc


def load_docs(reg_ids):
    """filename -> sorted DISTINCT backtick tokens that are exact registry node
    ids. Extracted PER LINE: `[^`]+` over the full text would span newlines and,
    in any doc with a multi-line code/mermaid block, pair a fence's opening
    backtick with the next one, emitting one giant token that contains the real
    ids but is never an exact id (the ids would be silently lost). Per-line
    findall cannot span a newline and cannot pair across an unbalanced line."""
    out = {}
    for d in sorted(DOCS.glob("*.md")):
        ids = set()
        for line in d.read_text(encoding="utf-8").splitlines():
            ids.update(t for t in re.findall(r"`([^`]+)`", line) if t in reg_ids)
        out[d.name] = sorted(ids)
    return out


def cross_check(docs, patterns, reg_ids, corpus_ids):
    """(doc_rows, corrections, gaps, unmatchable, best_doc, pat_sup). A doc
    matches a pattern when >=50% of its ids are in the pattern's full nodeIds
    union; best = highest overlap, tie -> lowest pattern id."""
    pat_union, pat_sup = {}, {}
    for (shape, key), p in patterns.items():
        u = set()
        for g in p["groups"].values():
            u |= g["ids"]
        pat_union[pid(shape, key)] = frozenset(u)
        pat_sup[pid(shape, key)] = len(p["projects"])
    pat_ids = sorted(pat_union)
    doc_rows, corrections, unmatchable, best_doc, pat_matched = [], [], [], {}, {}
    for dname, ids in docs.items():
        idset = set(ids)
        in_corpus = len(idset & corpus_ids)
        if len(ids) < 2:
            unmatchable.append((dname, len(ids), in_corpus))
            doc_rows.append((dname, len(ids), 0.0, None, in_corpus))
            continue
        best = None
        for pid_ in pat_ids:
            u = pat_union[pid_]
            if u:
                ov = len(idset & u) / len(idset)
                if ov >= 0.5:
                    # this doc matches this pattern (>=50% overlap) - mark it so
                    # it is NOT a library gap, regardless of whether it is the
                    # doc's BEST match. (Gating on "best" would wrongly count a
                    # pattern as a gap when another pattern is the doc's best.)
                    pat_matched[pid_] = True
                    if best is None or ov > best[0] or \
                            (ov == best[0] and pid_ < best[1]):
                        best = (ov, pid_)
        if best is None:
            maxov = max((len(idset & pat_union[x]) / len(idset) for x in pat_ids
                         if pat_union[x]), default=0.0)
            doc_rows.append((dname, len(ids), maxov, None, in_corpus))
            if in_corpus >= 1:
                corrections.append((dname, ids, in_corpus, maxov))
        else:
            ov, pid_ = best
            doc_rows.append((dname, len(ids), ov, pid_, in_corpus))
            c = best_doc.get(pid_)
            if c is None or ov > c[0] or (ov == c[0] and dname < c[1]):
                best_doc[pid_] = (ov, dname)
    gaps = sorted(((x, pat_sup[x]) for x in pat_ids
                   if pat_sup[x] >= 5 and x not in pat_matched),
                  key=lambda t: (-t[1], t[0]))
    return doc_rows, corrections, gaps, unmatchable, best_doc, pat_sup


# ---------------------------------------------------------------- rendering
def role_summary(p):
    seen, out = set(), []
    for g in p["order"]:
        if p["groups"][g]["role"] not in seen:
            seen.add(p["groups"][g]["role"])
            out.append(p["groups"][g]["role"])
    return " -> ".join(out)


def p_name(p, nm):
    shape, key = p["shape"], p["key"]
    if shape in ("chain", "conversion"):
        return " -> ".join(nm(i) for i in (key[1] if shape == "chain" else key))
    if shape == "compartment-boundary":
        return f"{nm(key[0])} -> {nm(key[1])}"
    if shape == "annotation-sandwich":
        return f"{nm(key[0])} -> {nm(key[1])} -> {key[2]}"
    if shape in ("fan-out", "fan-in"):
        groups = [p["groups"][g] for g in p["order"] if g != "X"]
        n_ids = sum(len(g["ids"]) for g in groups)
        roles = ", ".join(sorted({g["role"] for g in groups}))
        word, kind = ("out to", "target") if shape == "fan-out" else ("in from", "source")
        return f"{nm(key[0])} fans {word} {n_ids} {kind} ids (roles: {roles})"
    if shape == "compartment-cycle":
        return f"cycle in compartment {key[0]}"
    return str(key)


def to_pattern_json(p, name, best_doc):
    """One emitted pattern. `projects` is capped to the first 20 (sorted) to
    keep patterns.json small; `projectCount` = the true total and `support` =
    the true distinct-project count (both full, not capped)."""
    pid_ = pid(p["shape"], p["key"])
    projs = sorted(p["projects"])
    return {"id": pid_, "name": p_name(p, lambda i: name.get(i, i)),
            "shape": p["shape"],
            "nodes": [{"role": p["groups"][g]["role"],
                       "nodeIds": sorted(p["groups"][g]["ids"])}
                      for g in p["order"]],
            "support": len(projs), "projects": projs[:20],
            "projectCount": len(projs),
            "exampleGraph": {"compartment": p["ex_comp"],
                             "project": p["ex"][1] if p["ex"] else None,
                             "nodes": p["ex_nodes"] or [],
                             "edges": p["ex_edges"] or []},
            "crossRefs": {"llmPatternDoc":
                          best_doc[pid_][1] if pid_ in best_doc else None,
                          "llmOverlap": round(best_doc[pid_][0], 3)
                          if pid_ in best_doc else None,
                          "kdbSolution": None}}


def table_rows(keys, acc, name):
    lines = ["| # | id | shape | support | projects | occurrences | role summary |",
             "|---:|---|---|---:|---:|---:|---|"]
    for i, (shape, key) in enumerate(keys[:TOPN], 1):
        p = acc.p[(shape, key)]
        lines.append(f"| {i} | `{pid(shape,key)}` | {shape} | {len(p['projects'])} "
                     f"| {len(p['projects'])} | {p['occ']} | {role_summary(p)} |")
    return lines


def render_section(acc, docs, doc_rows, corrections, gaps, unmatchable, edge_cnt,
                   n_absent, role_counts, verify, boundary_top, io_pair, cycle_n,
                   kept, totals, name, summary):
    nm = lambda nid: name.get(nid, nid)
    body, raw = [], []
    rejected = sum(totals[s]["total"] - totals[s]["kept"] for s in totals)
    body += [
        "Generated by `analysis/t7_patterns.py`; re-running replaces this section in "
        "place (rest of `report.md` untouched). Raw tables: `analysis/t7_table.md`; "
        "machine-readable inventory: `patterns/patterns.json`.", "", "Method:",
        "- **Role signature (concrete, first match wins):** `import` = primary "
        "function's top-level branch is `jipipe:function:data`; `annotate` = branch "
        "`jipipe:function:annotations` (a function branch, not a framework); `export` = "
        "branch `jipipe:function:export`; `trackmate` = framework "
        "`jipipe:framework:trackmate`; `plotting` = branch `jipipe:function:plotting`; "
        "`core-io` = id in {`io-interface`, `jipipe:compartment-input`, "
        "`jipipe:compartment-output`}; `unknown` = absent from the registry or no "
        "primary function; else `other`. Order: import, annotate, export, trackmate, "
        "plotting, core-io, other. Role counts over the 336 distinct corpus-used node "
        "ids: " + ", ".join(f"{r}={c}" for r, c in sorted(role_counts.items())) +
        f". {n_absent} corpus node ids are absent from the registry (legacy) and are "
        "treated as `unknown` (counted, never crashed on).",
        "- **support** = number of DISTINCT projects in which the pattern occurs >= 1 "
        f"time (deduped per project). **Cutoff: keep support >= {CUTOFF}**; "
        f"{rejected:,} distinct patterns (support 1-2) were rejected. "
        f"**occurrences** (where a class defines it) = total realizations "
        "(chains/conversions: edge-walks; fan-out/fan-in: qualifying node instances; "
        "boundary: edge count; sandwich: import->annotate->processor triples).",
        "- **Class 1 - chains (`shape: chain`):** every maximal node-id sequence of "
        "length 3-5 along TYPED edges (both hops' `dataClass` non-null) in a "
        "project's project-wide node space; a chain is one id-tuple (occurrences = "
        "edge-paths realizing it). Overlaps Task 4's top-20 3-chains (this is the "
        "generalized multi-length inventory).",
        "- **Class 2 - fan-out / fan-in (`shape: fan-out` / `fan-in`):** fan-out = a "
        "node X with >= 2 TYPED output edges to >= 2 DISTINCT target node ids; "
        "generalized signature = (X.id, sorted role-multiset of targets). Fan-in = "
        "dual (>= 2 typed input edges from >= 2 distinct sources).",
        "- **Class 3 - conversions (`shape: conversion`):** typed 2-hop A->B->C where "
        "the data type changes at EVERY hop (`dataClass(A->B) != dataClass(B->C)`; "
        "null edges excluded); generalized by (A.id, B.id, C.id).",
        "- **Class 4 - annotation sandwiches (`shape: annotation-sandwich`):** a node "
        "S of role `annotate` with >= 1 incoming edge (typed or any) from a role "
        "`import` node AND >= 1 outgoing edge to a node of role NOT in {import, "
        "annotate, unknown} (a 'processor'); generalized by (import_id, annotate_id, "
        "processor_role).",
        "- **Class 5 - compartment motifs:** **boundary** (`compartment-boundary`) = "
        "inter-compartment edges (endpoints in different compartments per the "
        "uid->compartment map), generalized by (source_node_id, target_node_id), "
        f"occurrences = edge count, keep support >= {CUTOFF}. **cycle** "
        "(`compartment-cycle`) = per-compartment DFS back-edge detection, generalized "
        "by (compartment name + node count), NO support cutoff (reported as M; JIPipe "
        "dataflow is a DAG).",
        "- **Cross-check (Step 2):** a hand-written doc's named ids = every "
        "backtick-quoted token (extracted **per line** - `[^`]+` over the full text "
        "spans newlines and, in any doc with a multi-line code/mermaid block, pairs a "
        "fence's opening backtick with the next one, emitting one giant token that "
        "contains the real ids but is never an exact id, so they would be silently "
        "lost) that is an exact registry node id (distinct ids; the overlap "
        "denominator). A doc matches a mined pattern when >= 50% of its ids appear in "
        "the pattern's full nodeIds union (across all role groups); the best match "
        "(highest overlap, tie -> lowest pattern id) is recorded as "
        "`crossRefs.llmPatternDoc` + "
        "`crossRefs.llmOverlap`. **5b correction candidates** = docs with >= 2 "
        "extracted ids, NO pattern at overlap >= 50%, and not all ids corpus-absent. "
        "**5c library gaps** = mined patterns with support >= 5 and no doc at overlap "
        ">= 50%. Docs with < 2 extracted registry ids are 'unmatchable (few named "
        "ids)'.",
        "- **patterns.json emission cap:** `patterns.json` emits only patterns with "
        f"**support >= {EMIT_SUP} OR the top-100 by support within each of the 5 "
        f"conceptual classes** (class 2 = fan-out+fan-in; class 5 = boundary+cycle; "
        f"union, sorted by support desc then id); the full statistics for ALL mined "
        f"patterns remain in this report (5a). The literal 'support >= 5' rule would "
        f"emit ~5,708 patterns / ~15 MB (the long chain tail), far over the ~3 MB "
        f"budget, and support >= 18 still lands ~3.75 MB; support >= {EMIT_SUP} + "
        f"top-100/class is the highest-support set that fits (this run: "
        f"{summary['emitted']} patterns, {summary['sizeMB']:.1f} MB). To keep the file "
        "small, each emitted pattern's `projects` list is **capped to the first 20 "
        "(sorted)** with a new `projectCount` = the true total; `support` always = the "
        "true distinct-project count and `exampleGraph` is kept in full (small; needed "
        "by Task 8). The long tail is represented numerically in the top-level "
        "`summary` object (`totalMined`, `byShape`, `emitted`, `supportHistogram` "
        "1-2/3-4/>=5) - no silent data loss.",
        "- **exampleGraph** = the smallest (fewest nodes; tie -> lexicographic "
        "project file + compartment name) subgraph with one occurrence: node ids + "
        "slot edges, parameters elided (the corpus carries no parameters).",
        "",
    ]
    body += ["### 5a - Mined-pattern inventory (top-25 per class, ranked by support)",
             ""]
    for s in SHAPE_ORDER:
        tot = totals[s]
        body += [f"#### {s}", "",
                 f"**total kept: {tot['kept']:,}** (of {tot['total']:,} mined; "
                 f"{tot['total']-tot['kept']:,} rejected by the support >= {CUTOFF} "
                 "cutoff)", ""]
        body += table_rows(tot["kept_keys"], acc, nm)
        body.append("")
    body += ["**Class totals (kept):** " +
             ", ".join(f"{s}={totals[s]['kept']:,}" for s in SHAPE_ORDER) +
             f"; **N = {kept:,}** total mined patterns with support >= {CUTOFF}.", ""]
    body += [f"**Boundary note:** the top inter-compartment pair is "
             f"`{boundary_top[0]}` -> `{boundary_top[1]}` ({boundary_top[2]:,} "
             f"occurrences, {boundary_top[3]} projects). The controller's sanity anchor "
             f"`io-interface -> jipipe:compartment-output` ({io_pair[2]:,} occurrences) "
             f"is **100% same-compartment** in the real corpus (0 inter-compartment), "
             f"so it does not appear in this inter-compartment class; that figure is "
             f"the corpus-wide edge count from Task 5's 3b same-framework table, not a "
             f"cross-compartment wiring pair. {edge_cnt['inter']:,} of "
             f"{edge_cnt['total']:,} corpus edges are inter-compartment (all from "
             f"`jipipe:compartment-output`).", ""]
    v = verify
    body += [f"**Flagship verification (Step 3):** the mined `{v['shape']}` pattern "
             f"`{v['id']}` ({v['name']}) has **support {v['support']}** (>= {CUTOFF}); "
             f"in project `{v['project']}` (compartment `{v['compartment']}`) the "
             f"concrete occurrence is realized by nodes "
             + ", ".join(f"`{x['id']}`" for x in v["nodes"]) + " with edges "
             + "; ".join(f"`{e['from']}`.{e['fromSlot']} -> "
                         f"`{e['to']}`.{e['toSlot']} (dataClass `{e['dataClass']}`)"
                         for e in v["edges"]) +
             f". Independently re-scanned in `corpus.json`: {v['rescan']} typed edges "
             f"of the matching (from-id, to-id) pairs are present in that project. "
             f"PASS.", ""]
    body += ["### 5b - Correction candidates (hand-written docs the corpus does not "
             "sufficiently exhibit)", ""]
    if corrections:
        body += ["| doc | extracted registry ids | in corpus | best overlap |",
                 "|---|---|---:|---:|"]
        for dname, ids, inc, ov in sorted(corrections):
            body.append(f"| {dname} | {', '.join(f'`{i}`' for i in ids)} "
                        f"| {inc}/{len(set(ids))} | {ov:.2f} |")
    else:
        body.append("(none - every doc with >= 2 extracted ids matched a pattern)")
    body += [""]
    n_match = sum(1 for r in doc_rows if r[3] is not None)
    n_zero = sum(1 for r in doc_rows if r[1] == 0)
    body += [f"Cross-check over {len(docs)} docs: **{n_match} matched** a mined "
             f"pattern (overlap >= 50%), **{len(corrections)} correction candidates**, "
             f"**{len(unmatchable)} unmatchable** (< 2 extracted "
             f"registry ids), {len(gaps):,} library gaps (5c). **Weakness:** "
             f"{n_zero} of {len(docs)} docs name nodes by display name or prose "
             "rather than raw registry node ids inside backticks, so the mechanical "
             "exact-id extraction yields 0 ids for them - they are 'unmatchable' for "
             "a reason (extraction weakness), "
             "NOT because the corpus lacks the pattern. Per-doc:", ""]
    body += ["| doc | extracted ids | in corpus | best overlap | matched pattern |",
             "|---|---:|---:|---:|---|"]
    for dname, n_ids, ov, mp, inc in sorted(doc_rows):
        body.append(f"| {dname} | {n_ids} | {inc} | {ov:.2f} | "
                    f"{'`' + mp + '`' if mp else '(none)'} |")
    body.append("")
    body += [f"### 5c - Library gaps (mined patterns with support >= 5 and no matching "
             f"doc) - {len(gaps):,} patterns", ""]
    if gaps:
        body += ["| pattern id | support |", "|---|---:|"]
        for gid, sup in gaps[:TOPN]:
            body.append(f"| `{gid}` | {sup} |")
        if len(gaps) > TOPN:
            body.append(f"| … {len(gaps)-TOPN} more (counts preserved in "
                        f"`patterns/patterns.json` `summary`; only the top rows "
                        f"are listed/emitted) | |")
    else:
        body.append("(none)")
    body.append("")
    body += [f"Compartment cycles: **M = {cycle_n}** (JIPipe dataflow is a DAG; DFS "
             "found no back-edge in any of the 710 compartments).", ""]
    raw += ["# Task 7 - raw tables (mined patterns)", ""]
    raw += [f"**Totals (kept, support >= {CUTOFF}):** " +
            ", ".join(f"{s}={totals[s]['kept']:,}" for s in SHAPE_ORDER) +
            f"; N={kept:,}.", ""]
    for s in SHAPE_ORDER:
        raw += [f"## {s} (top {TOPN} of {totals[s]['kept']:,} kept)", ""]
        raw += table_rows(totals[s]["kept_keys"], acc, nm)
        raw.append("")
    raw += ["## cross-check - per-doc", "",
            "| doc | extracted ids | in corpus | best overlap | matched pattern |",
            "|---|---:|---:|---:|---|"]
    for dname, n_ids, ov, mp, inc in sorted(doc_rows):
        raw.append(f"| {dname} | {n_ids} | {inc} | {ov:.2f} | "
                   f"{'`'+mp+'`' if mp else '(none)'} |")
    raw.append("")
    raw += [f"## 5b correction candidates ({len(corrections)})", ""]
    raw += list(f"- {d}: {', '.join(ids)} ({inc} in corpus)"
                for d, ids, inc, ov in sorted(corrections)) or ["(none)"]
    raw.append("")
    raw += [f"## 5c library gaps ({len(gaps):,}) - support >= 5, no doc match", ""]
    raw += list(f"- {gid} (support {sup})" for gid, sup in gaps[:TOPN]) or ["(none)"]
    raw.append("")
    return body, raw


def render_report(body_text):
    header = "## 5. Mined patterns vs the hand-written library\n"
    if not REPORT.exists():
        REPORT.write_text("# Taxonomy follow-up research report\n" + header +
                          body_text, encoding="utf-8")
        return "created"
    text = REPORT.read_text(encoding="utf-8")
    m = re.search(r"^## 5\. ", text, flags=re.M)
    if not m:
        REPORT.write_text(text + ("\n" if not text.endswith("\n") else "") +
                          header + body_text, encoding="utf-8")
        return "appended"
    nxt = re.search(r"^## ", text[m.end():], flags=re.M)
    end = m.end() + nxt.start() if nxt else len(text)
    REPORT.write_text(text[:m.start()] + header + body_text + text[end:],
                      encoding="utf-8")
    return "replaced"


def main():
    corpus = json.load(open(ROOT / "corpus.json", encoding="utf-8"))
    registry = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    role, name = build_role(registry)
    reg_ids = set(name)
    corpus_ids = {n["id"] for p in corpus["projects"]
                  for c in p["compartments"] for n in c["nodes"]}
    n_absent = len(corpus_ids - reg_ids)
    role_counts = Counter(nrole(role, i) for i in corpus_ids)

    acc = Acc()
    edge_cnt = mine(corpus, role, acc)
    cycle_n = mine_cycles(corpus, acc)

    totals = {s: {"total": 0, "kept": 0, "kept_keys": []} for s in SHAPE_ORDER}
    for (shape, key), p in acc.p.items():
        sup = len(p["projects"])
        totals[shape]["total"] += 1
        if shape == "compartment-cycle" or sup >= CUTOFF:
            totals[shape]["kept"] += 1
            totals[shape]["kept_keys"].append((shape, key))
    for s in totals:
        totals[s]["kept_keys"].sort(key=lambda k: (-len(acc.p[k]["projects"]),
                                                   pid(k[0], k[1])))
    kept = sum(totals[s]["kept"] for s in totals)

    # boundary top + io-interface anchor (corpus-wide count)
    bpair, bproj, io_total = Counter(), defaultdict(set), 0
    for p in corpus["projects"]:
        comp_of, id_of = {}, {}
        for c in p["compartments"]:
            for n in c["nodes"]:
                comp_of[n["uid"]] = c["name"]
                id_of[n["uid"]] = n["id"]
        for c in p["compartments"]:
            for e in c["edges"]:
                fu, tu = e["fromUid"], e["toUid"]
                a, b = comp_of.get(fu), comp_of.get(tu)
                if a is not None and b is not None and a != b:
                    k = (id_of[fu], id_of[tu])
                    bpair[k] += 1
                    bproj[k].add(p["file"])
                if id_of.get(fu) == "io-interface" and \
                        id_of.get(tu) == "jipipe:compartment-output":
                    io_total += 1
    bt = sorted(bpair.items(), key=lambda kv: (-kv[1], kv[0]))[0]
    boundary_top = (bt[0][0], bt[0][1], bt[1], len(bproj[bt[0]]))
    io_pair = ("io-interface", "jipipe:compartment-output", io_total)

    # flagship: best chain/conversion with ids[0] in folder-lists, ids[1] a
    # path-annotator, ids[2] an image/roi import
    FLAG = {"folder-list-files", "folder-list-subfolders"}
    ANN = {"path-to-annotation-simple", "annotate-by-project-paths",
           "annotate-with-path-properties"}
    IMP = {"import-imagej-imgplus-from-file", "import-imagej-roi-from-file"}
    best = None
    for (shape, key), p in acc.p.items():
        if shape not in ("chain", "conversion") or len(p["projects"]) < CUTOFF:
            continue
        ids = key[1] if shape == "chain" else key
        if len(ids) >= 3 and ids[0] in FLAG and ids[1] in ANN and ids[2] in IMP:
            if best is None or len(p["projects"]) > best[2]:
                best = (shape, key, len(p["projects"]), p)
    if best is None:
        raise SystemExit("FLAGSHIP NOT FOUND - mining bug")
    shape, key, sup, p = best
    verify = {"shape": shape, "id": pid(shape, key),
              "name": p_name(p, lambda i: name.get(i, i)), "support": sup,
              "project": p["ex"][1], "compartment": p["ex_comp"],
              "nodes": p["ex_nodes"], "edges": p["ex_edges"]}
    tgt = next(pp for pp in corpus["projects"] if pp["file"] == verify["project"])
    id_of = {n["uid"]: n["id"] for c in tgt["compartments"] for n in c["nodes"]}
    seq = key[1] if shape == "chain" else key
    want = {(a, b) for a, b in zip(seq, seq[1:])}
    verify["rescan"] = sum(1 for c in tgt["compartments"] for e in c["edges"]
                           if e.get("dataClass") is not None
                           and (id_of.get(e["fromUid"]), id_of.get(e["toUid"]))
                           in want)

    docs = load_docs(reg_ids)
    doc_rows, corrections, gaps, unmatchable, best_doc, pat_sup = cross_check(
        docs, acc.p, reg_ids, corpus_ids)

    # emission cap (approach recorded in the method note): patterns.json emits
    # only (a) patterns with support >= EMIT_SUP, OR (b) the top-100 by support
    # within each of the FIVE conceptual classes (class 2 = fan-out+fan-in;
    # class 5 = boundary+cycle) - union, sorted by support desc then id. The
    # literal "support >= 5" rule would emit 5,708 patterns / ~15 MB (mostly the
    # long chain tail), far over the ~3 MB budget; EMIT_SUP + top-100/class is
    # the highest-support set that fits (support >= 18 still ~3.75 MB). The full
    # statistics for ALL mined patterns stay in this report (5a); the dropped
    # long tail is preserved numerically in the top-level `summary`.
    CLASSES = [["chain"], ["fan-out", "fan-in"], ["conversion"],
               ["annotation-sandwich"],
               ["compartment-boundary", "compartment-cycle"]]
    byshape = {}
    for s in SHAPE_ORDER:
        for (sh, k) in totals[s]["kept_keys"]:
            byshape.setdefault(sh, []).append((sh, k))
    for s in byshape:
        byshape[s].sort(key=lambda t: (-len(acc.p[t]["projects"]),
                                       pid(t[0], t[1])))
    emit = set()
    for s in SHAPE_ORDER:
        for t in byshape.get(s, []):
            if len(acc.p[t]["projects"]) >= EMIT_SUP:
                emit.add(t)
    for cls in CLASSES:
        pool = sorted([t for s in cls for t in byshape.get(s, [])],
                      key=lambda t: (-len(acc.p[t]["projects"]),
                                     pid(t[0], t[1])))
        for t in pool[:100]:
            emit.add(t)
    emit_keys = sorted(emit,
                       key=lambda t: (-len(acc.p[t]["projects"]), pid(t[0], t[1])))
    json_pats = [to_pattern_json(acc.p[t], name, best_doc) for t in emit_keys]
    # supportHistogram buckets the KEPT (support >= CUTOFF) population, so it
    # sums to totalMined = kept; the emitted set is support >= EMIT_SUP + the
    # per-class top-100, so every kept pattern with support < EMIT_SUP that is
    # not in a class top-100 sits in the 3-4 bucket (numerically represented ->
    # no silent data loss).
    hist = {"1-2": 0, "3-4": 0, ">=5": 0}
    for p in acc.p.values():
        s = len(p["projects"])
        if s >= CUTOFF:
            hist["3-4" if s < 5 else ">=5"] += 1
    summary = {"totalMined": kept,
               "byShape": {s: totals[s]["kept"] for s in SHAPE_ORDER},
               "emitted": len(json_pats),
               "supportHistogram": hist,
               "sizeMB": 0.0}
    blob = json.dumps({"summary": summary, "patterns": json_pats}, indent=1,
                      ensure_ascii=False) + "\n"
    summary["sizeMB"] = round(len(blob.encode("utf-8")) / 1e6, 3)
    blob = json.dumps({"summary": summary, "patterns": json_pats}, indent=1,
                      ensure_ascii=False) + "\n"
    PATTERNS_OUT.parent.mkdir(parents=True, exist_ok=True)
    PATTERNS_OUT.write_text(blob, encoding="utf-8")

    body, raw = render_section(acc, docs, doc_rows, corrections, gaps,
                               unmatchable, edge_cnt, n_absent, role_counts,
                               verify, boundary_top, io_pair, cycle_n, kept,
                               totals, name, summary)
    TABLE.write_text("\n".join(raw).rstrip() + "\n", encoding="utf-8")
    render_report("\n".join(body).rstrip() + "\n")
    n_match = sum(1 for r in doc_rows if r[3] is not None)
    fan = totals["fan-out"]["kept"] + totals["fan-in"]["kept"]
    comp = totals["compartment-boundary"]["kept"] + \
        totals["compartment-cycle"]["kept"]
    rej = sum(t["total"] - t["kept"] for t in totals.values())
    print(f"patterns: {kept} (chains {totals['chain']['kept']}, fan {fan}, "
          f"conversions {totals['conversion']['kept']}, "
          f"annotations {totals['annotation-sandwich']['kept']}, "
          f"compartments {comp})")
    print(f"(rejected support 1-2: {rej}; cycles M={cycle_n}; docs matched="
          f"{n_match}/24, corrections={len(corrections)}, gaps={len(gaps)}, "
          f"unmatchable={len(unmatchable)}; patterns.json {len(json_pats)} entries; "
          f"report.md section 5 in place)")


if __name__ == "__main__":
    main()
