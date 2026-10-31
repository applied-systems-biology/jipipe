#!/usr/bin/env python3
"""Task 5: family coverage & follow-up quality against real corpus usage (6.0.0).

Reads corpus.json + registry.json (locked, Tasks 1-4); emits report section 3:
(3a) per family, each declared after-edge's corpus wiring (any dataClass) + status
plus real member edges NOT declared; (3b) same-framework corpus edges (A.id !=
B.id) with no declared family relation in either direction (top 30 + per-framework
pair counts); (3c) the low-consumer recompute (registry data types with exactly
1-2 distinct corpus consumers), cross-checked against the committed t3_table.md
sub-table, split by whether any producer framework has families. No interpretation
(Task 9's job); stdlib only; ONE project-wide uid->node map per project (edges
re-homed into the target compartment). Emits analysis/t5_table.md and report.md
(created if absent; on re-runs only "## 3. " -> next "## "/EOF replaced; byte-
stable, hash-seed-immune). Usage: python3 t5_families.py
"""

import json, re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE, T3 = (ROOT / "report.md", ROOT / "analysis" / "t5_table.md",
                     ROOT / "analysis" / "t3_table.md")
NONE_FW = "jipipe:framework:none"  # explicit "no framework" value, not a real one


def build_registry(registry):
    """(fw, outdts_all, fqn_to_dts, simple_to_fqns, resolvable)"""
    fw, outdts_all = {}, set()
    fqn_to_dts, simple_to_fqns = defaultdict(set), defaultdict(set)
    for n in registry["nodes"]:
        fw[n["id"]] = n.get("framework")
        outdts_all |= {io["dataType"] for io in n.get("outputs") or [] if io.get("dataType")}
        for io in list(n.get("outputs") or []) + list(n.get("inputs") or []):
            cls, dt = io.get("class"), io.get("dataType")
            if cls:
                simple_to_fqns[cls.rsplit(".", 1)[-1]].add(cls)
            if cls and dt:
                fqn_to_dts[cls].add(dt)
    resolvable = set(outdts_all) | {d for s in fqn_to_dts.values() for d in s}
    return fw, outdts_all, fqn_to_dts, simple_to_fqns, resolvable


def resolve_token(token, resolvable, fqn_to_dts, simple_to_fqns):
    """t3's order: exact dt id -> exact class FQN -> unique simple name; None = not one type."""
    if token in resolvable:
        return token
    m = set(fqn_to_dts.get(token, ())) | {d for fqn in sorted(simple_to_fqns.get(token, ()))
                                          for d in fqn_to_dts.get(fqn, set())}
    return next(iter(m)) if len(m) == 1 else None


def load_t3_flagged(path):
    """Row keys of the committed t3 Table 1c flagged sub-table, in order; stops at
    the first non-table, non-empty line (its header row is a | row)."""
    rows, in1c = [], False
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("### Table 1c"):
            in1c = True
        elif in1c and line.strip() and not line.strip().startswith("|"):
            break
        elif in1c and (m := re.match(r"^\| `([^`]+)` \|", line)):
            rows.append(m.group(1))
    return rows


def verify_trackmate(families):
    tm = next(f for f in families if f["id"] == "jipipe:family:trackmate")
    pl = next(f for f in families if f["id"] == "jipipe:family:plotting")
    ids = {s["nodeId"] for s in tm["steps"]}
    det_after = {a for s in tm["steps"] if s["nodeId"] == "trackmate-tracking" for a in s["after"]}
    n_after = sum(len(s["after"]) for s in tm["steps"])
    n_pl_after = sum(len(s["after"]) for s in pl["steps"])
    n_det = sum(1 for i in ids if i.startswith("trackmate-create-spot-detector-"))
    ok = (len(tm["steps"]) == 21 and len(ids) == 21 and n_det == 7 and len(det_after) == 8
          and n_after == 20 and len(pl["steps"]) == 1 and n_pl_after == 0)
    return ("Step 2 verification (computed from `registry.json`): the trackmate family has "
            f"{len(tm['steps'])} steps with {len(ids)} distinct member node ids "
            "(expected 21; the 7 `trackmate-create-spot-detector-*` nodes each have their own "
            f"step) and {n_after} declared after-edges (expected 20); `trackmate-tracking.after` = "
            f"the 7 detector ids + `trackmate-detector` ({len(det_after)} ids, expected 8); the "
            f"plotting family has {len(pl['steps'])} step and {n_pl_after} declared after-edges "
            f"(expected 1 step, 0 edges). {'PASS' if ok else 'FAIL'}")


def aggregate(corpus, fw, families, members, resolvable, fqn_to_dts, simple_to_fqns):
    """(declared, undeclared, pair_stats, type_stats, cnt) over all corpus edges:
    declared[F][(B,A)]/undeclared[F][(A,B)] per-edge stats; 3b pairs; 3c id sets."""
    fam_of = {nid: fid for fid, mem in members.items() for nid in mem}
    declared_in = {f["id"]: {s["nodeId"]: set(s["after"]) for s in f["steps"]} for f in families}
    stat = lambda: {"occ": 0, "projects": set()}
    declared = {f["id"]: defaultdict(stat) for f in families}
    undeclared = {f["id"]: defaultdict(stat) for f in families}
    pair_stats = defaultdict(stat)
    type_stats = defaultdict(lambda: {"cons": set(), "prod": set()})
    cnt = {k: 0 for k in ("total", "null", "resolvable", "unresolvable", "in_family", "declared",
                          "undeclared", "cross_family", "cross_declared", "pair", "pair_samefam",
                          "pair_alt", "occ_alt", "no_fw", "fw_mismatch")}
    for p in corpus["projects"]:
        uidmap = {n["uid"]: n["id"] for c in p["compartments"] for n in c["nodes"]}
        for c in p["compartments"]:
            for e in c["edges"]:
                cnt["total"] += 1
                tok = e.get("dataClass")
                a, b = uidmap.get(e["fromUid"]), uidmap.get(e["toUid"])
                if tok is None:
                    cnt["null"] += 1
                else:
                    t = resolve_token(tok, resolvable, fqn_to_dts, simple_to_fqns)
                    if t is None:
                        cnt["unresolvable"] += 1
                    elif a is not None and b is not None:
                        cnt["resolvable"] += 1
                        type_stats[t]["cons"].add(b)
                        type_stats[t]["prod"].add(a)
                if a is None or b is None:
                    continue
                fa, fb = fam_of.get(a), fam_of.get(b)
                # 3a: within-family edges split declared vs undeclared
                if fa is not None and fa == fb:
                    cnt["in_family"] += 1
                    if a in declared_in[fa].get(b, ()):
                        cnt["declared"] += 1
                        s = declared[fa][(b, a)]
                    else:
                        cnt["undeclared"] += 1
                        s = undeclared[fa][(a, b)]
                    s["occ"] += 1
                    s["projects"].add(p["file"])
                else:
                    cnt["cross_family"] += 1
                # 3b (ruling): SAME-FRAMEWORK (non-null, `none` sentinel excluded) AND
                # NOT a declared after-edge in ANY family, in either direction. Computed
                # over ALL edges (incl. same-family undeclared, which also appear in 3a).
                if a != b:
                    fwa, fwb = fw.get(a), fw.get(b)
                    if fwa is None or fwa == NONE_FW:
                        cnt["no_fw"] += 1
                    elif fwb != fwa:
                        cnt["fw_mismatch"] += 1
                    elif ((fb is not None and a in declared_in[fb].get(b, ())) or
                            (fa is not None and b in declared_in[fa].get(a, ()))):
                        cnt["cross_declared"] += 1
                    else:
                        cnt["pair"] += 1
                        s = pair_stats[(fwa, a, b)]
                        s["occ"] += 1
                        s["projects"].add(p["file"])
                        if fa is not None and fa == fb:
                            cnt["pair_samefam"] += 1
    # alternative reading: 3b restricted to pairs where NEITHER endpoint is a
    # family member (same walk, `a in fam_of and b in fam_of` skip)
    alt = [k for k in pair_stats if k[1] not in fam_of and k[2] not in fam_of]
    cnt["pair_alt"] = len(alt)
    cnt["occ_alt"] = sum(pair_stats[k]["occ"] for k in alt)
    return declared, undeclared, pair_stats, type_stats, cnt


def fam_stats(f, declared, undeclared):
    fid, d = f["id"], declared[f["id"]]
    edges = [(s["nodeId"], a) for s in f["steps"] for a in s["after"]]
    und, n_wired = undeclared[fid], sum(1 for b, a in edges if d[(b, a)]["occ"] > 0)
    return {"fid": fid, "n_steps": len(f["steps"]),
            "n_members": len({s["nodeId"] for s in f["steps"]}),
            "declared": {(b, a): d[(b, a)] for b, a in edges}, "n_declared": len(edges),
            "n_wired": n_wired, "n_never": len(edges) - n_wired, "undeclared": und,
            "n_undec_pairs": len(und), "undec_occ": sum(s["occ"] for s in und.values())}


def render_section(fam, families, pair_stats, per_fw, low, cross_check, cnt, step2_line, cross_check_lines):
    """(section-body-lines, raw-table-lines) sharing one build path."""
    TOPN = 20
    body, raw = [], []
    body += [
        "Generated by `analysis/t5_families.py`; re-running replaces this section in place "
        "(rest of `report.md` untouched). Raw tables: `analysis/t5_table.md`.",
        "", "Method:",
        "- **Definitions:** a family's **member set** = the set of `nodeId` values across its "
        "`registry.json` `families[].steps[]` (trackmate: 21 distinct node ids from 21 steps; "
        "plotting: 1). A **declared after-edge** of family F = (F, B, A) where B is a step's "
        "`nodeId` and A is in B's `after[]` (the resolved predecessor node ids; trackmate: 20 "
        "edges, plotting: 0). **ONE project-wide uid→node map is built per project before any "
        "walk** (edges are re-homed into the target compartment). Of {t:,} corpus edges: {i:,} "
        "are between members of the same family ({d:,} wiring a declared after-edge, {u:,} "
        "wiring an undeclared member pair), {x:,} are between non-member or cross-family nodes; "
        "the typed tokens are {r:,} resolvable to a registry data type and {nr:,} not (null: {n})."
        .format(t=cnt["total"], i=cnt["in_family"], d=cnt["declared"], u=cnt["undeclared"],
                x=cnt["cross_family"], r=cnt["resolvable"], nr=cnt["unresolvable"], n=cnt["null"]),
        "- **3a - declared vs real:** a declared after-edge (B, A) is **wired** when ≥ 1 "
        "corpus edge has source node id A and target node id B (ANY `dataClass` - a real "
        "connection is a real connection; occurrences and distinct projects counted per "
        "family). Otherwise **declared-but-never-wired**. **Undeclared-real** edges: corpus "
        "edges (A→B) whose two node ids are both in F's member set but where B's `after[]` "
        "does not contain A - real workflow edges the family graph is missing (top {n} by "
        "occurrence; all listed in `analysis/t5_table.md`)." .format(n=TOPN),
        "- **3b - family candidates:** corpus edges (A→B) with A.id != B.id, "
        "node(A).framework == node(B).framework (non-null; "
        f"`{NONE_FW}` excluded - {cnt['no_fw']:,} such edges) where the pair is NOT "
        "connected by any declared after-edge in ANY family, in either direction "
        f"({cnt['cross_declared']:,} same-framework edges excluded for this reason). Computed "
        "over ALL corpus edges (per the ruling's mechanical rule, not restricted to "
        f"cross-family edges): {cnt['pair_samefam']:,} of the qualifying occurrences are "
        "same-family edges that are also reported as undeclared-real in 3a (the "
        "trackmate `mask-detector→detector` chain) - the overlap is intentional, not an "
        f"error (reading 3b restricted to pairs where NEITHER endpoint is a family "
        f"member: {cnt['pair_alt']:,} pairs / {cnt['occ_alt']:,} occurrences). "
        "Frameworks absent from the corpus are absent from the results by "
        f"construction. {cnt['fw_mismatch']:,} edges had different frameworks and never qualify.",
        "- **3c - specialized data types:** the **low-consumer set is RECOMPUTED** here "
        "(t3's markdown not parsed for the set, t3 not re-run): a registry data type T "
        "(a non-null `outputs[].dataType`) with exactly 1-2 DISTINCT consumer node ids, where "
        "consumer = a registry node id observed as the target of a corpus edge whose resolved "
        "token is T (t3's counting rule, mirrored). " + cross_check + " For each T, the "
        "**producer framework(s)** = the distinct `framework` of registry nodes that "
        "OUTPUT T (registry capability, unlike the corpus-observed consumers); a framework "
        "**HAS families** when ≥ 1 node with that framework is in any family's member set. "
        "T is (i) **with-family** when any producer framework HAS families (the trackmate "
        "case) or (ii) **without-family** otherwise (the siblings - narrow-consumer "
        "specialized types with no family to anchor them).",
        "", *step2_line, ""]
    for st in fam:
        f = next(x for x in families if x["id"] == st["fid"])
        label = {s["nodeId"]: s["label"] for s in f["steps"]}
        decl_rows = [f"| `{a}` → `{b}` | {label[b]} | {s['occ']} | {len(s['projects'])} | "
                     f"{'wired' if s['occ'] else 'declared-but-never-wired'} |"
                     for (b, a), s in sorted(st["declared"].items(),
                                             key=lambda kv: (-kv[1]["occ"], kv[0][1], kv[0][0]))]
        und_rows = [f"| `{a}` → `{b}` | {s['occ']} | {len(s['projects'])} |"
                    for (a, b), s in sorted(st["undeclared"].items(),
                                            key=lambda kv: (-kv[1]["occ"], kv[0]))]
        body += [f"### 3a - `{st['fid']}` ({st['n_steps']} steps, {st['n_members']} members, "
                 f"{st['n_declared']} declared after-edges, {st['n_wired']} wired, {st['n_never']} "
                 f"declared-but-never-wired, {st['n_undec_pairs']} undeclared-real pairs)", "",
                 "| declared after-edge (A → B) | B label | wired occurrences | wired projects | "
                 "status |", "|---|---|---:|---:|---|"]
        body += decl_rows or ["| - | - | - | - | (no declared after-edges) |"]
        body += ["", f"Undeclared-real edges between `{st['fid']}` members "
                     f"(top {min(TOPN, len(und_rows))} of {st['n_undec_pairs']} "
                     f"pairs, {st['undec_occ']:,} occurrences):", ""]
        body += (["| A → B (member node ids) | occurrences | projects |",
                  "|---|---:|---:|"] + und_rows[:TOPN]) if und_rows else \
            ["(none - every real member→member edge is declared)"]
        body.append("")
        raw += [f"### `{st['fid']}` declared after-edges", ""]
        raw += ["| A → B | B label | wired occurrences | wired projects | status |",
                "|---|---|---:|---:|---|"] + (decl_rows or ["(none)"])
        raw += ["", f"### `{st['fid']}` undeclared-real member edges "
                    f"({st['n_undec_pairs']} pairs)", ""]
        raw += ["| A → B | occurrences | projects |", "|---|---:|---:|"] + (und_rows or ["(none)"])
        raw.append("")
    all_sorted = sorted(pair_stats.items(), key=lambda kv: (-kv[1]["occ"], kv[0]))
    pair_row = lambda k, s: f"| `{k[0]}` | `{k[1]}` → `{k[2]}` | {s['occ']} | {len(s['projects'])} |"
    p30 = ["| # | framework | A → B | occurrences | projects |",
           "|---:|---|---|---:|---:|"] + \
          [f"| {i} | " + pair_row(k, s)[2:] for i, (k, s) in enumerate(all_sorted[:30], 1)]
    perfw = ["| framework | distinct pairs | occurrences |", "|---|---:|---:|"] + \
            [f"| `{fwi}` | {np_} | {no:,} |" for fwi, (np_, no) in per_fw]
    body += [f"### 3b - Same-framework real edges with no declared family relation (top "
             f"{len(all_sorted[:30])} of {len(pair_stats)} pairs)", ""]
    body += p30 + ["", "Per-framework distinct (A, B) pair counts (frameworks present in the "
                   "corpus with ≥ 1 qualifying pair):", ""] + perfw + [""]
    raw += ["## 3b - same-framework real edges, no declared family relation (all pairs)", ""]
    raw += ["| framework | A → B | occurrences | projects |", "|---|---|---:|---:|"] + \
        [pair_row(k, s) for k, s in all_sorted]
    raw += ["", "## 3b - per-framework distinct (A, B) pair counts", ""] + perfw + [""]
    p = sum(1 for t in low if t["cls"] == "with-family")
    q = sum(1 for t in low if t["cls"] == "without-family")
    fwj = lambda t: ", ".join(f"`{f}`" for f in sorted(t["pfws"])) or "(none)"
    l3c = ["| data type T | distinct corpus consumers | producer frameworks "
           "(registry) | classification | example producer node id |", "|---|---:|---|---|---|"] + \
          [f"| `{t['T']}` | {len(t['cons'])} | {fwj(t)} | {t['cls']} | `{t['ex']}` |" for t in low]
    al3c = ["| data type T | distinct corpus consumers | consumer node ids | "
            "producer node ids (registry capability) | producer frameworks | classification |",
            "|---|---:|---|---|---|---|"] + \
           [f"| `{t['T']}` | {len(t['cons'])} | "
            f"{', '.join(f'`{c}`' for c in sorted(t['cons']))} | "
            f"{', '.join(f'`{x}`' for x in sorted(t['prod']))} | {fwj(t)} | {t['cls']} |" for t in low]
    body += [f"### 3c - Low-consumer types by producer-family status "
             f"({len(low)} recomputed: {p} with-family, {q} without-family)", ""]
    body += l3c + [""]
    raw += ["## 3c - low-consumer recompute (all types)", ""]
    raw += al3c + ["", *cross_check_lines, ""]
    return body, raw


def main():
    corpus = json.load(open(ROOT / "corpus.json", encoding="utf-8"))
    registry = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    fw, outdts_all, fqn_to_dts, simple, resolvable = build_registry(registry)
    families = registry["families"]
    members = {f["id"]: {s["nodeId"] for s in f["steps"]} for f in families}
    declared, undeclared, pair_stats, type_stats, cnt = aggregate(
        corpus, fw, families, members, resolvable, fqn_to_dts, simple)
    fam = [fam_stats(f, declared, undeclared) for f in sorted(families, key=lambda f: f["id"])]
    per_fw = defaultdict(lambda: [0, 0])
    for (fwi, a, b), s in pair_stats.items():
        per_fw[fwi][0] += 1
        per_fw[fwi][1] += s["occ"]
    per_fw = sorted(per_fw.items(), key=lambda kv: (-kv[1][0], kv[0]))
    has_fam_fws = {fw.get(n) for m in members.values() for n in m if fw.get(n)}
    outp = defaultdict(set)  # T -> registry node ids that OUTPUT T (capability, ruling 3c)
    for n in registry["nodes"]:
        for dt in (io["dataType"] for io in n.get("outputs") or [] if io.get("dataType")):
            outp[dt].add(n["id"])
    low = []
    for t, ts in sorted(type_stats.items()):
        if t in outdts_all and 1 <= len(ts["cons"]) <= 2:
            cap, pfws = outp[t], sorted({fw.get(x) for x in outp[t] if fw.get(x)})
            low.append({"T": t, "cons": ts["cons"], "prod": cap, "pfws": pfws,
                        "cls": ("no-registry-producer-framework" if not pfws else "with-family"
                                if has_fam_fws & set(pfws) else "without-family"),
                        "ex": sorted(cap)[0]})
    t3_flagged = load_t3_flagged(T3)
    t3_raw = [t for t in t3_flagged if t not in outdts_all]
    n_reg = len(t3_flagged) - len(t3_raw)
    rec, t3s = {t["T"] for t in low}, {t for t in t3_flagged if t in outdts_all}
    agree = rec == t3s
    cc = ("**Cross-check: AGREE** - the recomputed set ({r} types) equals the "
          "registry-known rows of the committed `analysis/t3_table.md` Table 1c "
          "flagged sub-table ({n} of its {n2} rows; {u} rows are unresolved raw tokens, "
          "not registry data types, so the recompute rule - `T` is a non-null "
          "`outputs[].dataType` - excludes them by definition)."
          .format(r=len(rec), n=n_reg, n2=len(t3_flagged), u=len(t3_raw))
          if agree else
          "**Cross-check: DISAGREE** - recomputed-only: "
          + (", ".join(f"`{x}`" for x in sorted(rec - t3s)) or "(none)")
          + "; t3-only (registry-known): "
          + (", ".join(f"`{x}`" for x in sorted(t3s - rec)) or "(none)")
          + ". Reported as a concern.")
    ccl = [f"Cross-check vs `analysis/t3_table.md` Table 1c: "
           f"{'AGREE' if agree else 'DISAGREE'} (recomputed: {len(rec)} types; t3 table rows: "
           f"{len(t3_flagged)}; registry-known rows: {n_reg}"
           + (f"; {len(t3_raw)} rows are unresolved raw tokens: "
              + ", ".join(f"`{t}`" for t in sorted(t3_raw)) if t3_raw else "") + ")"]
    body, raw = render_section(fam, families, pair_stats, per_fw, low, cc, cnt, [verify_trackmate(families)], ccl)
    TABLE.write_text("\n".join(["# Task 5 - raw tables (family coverage & follow-up "
                                "quality)", "", "## 3a - declared after-edges vs corpus "
                                "wiring", ""] + raw), encoding="utf-8")
    text = "## 3. Families vs reality\n" + "\n".join(body).rstrip() + "\n"
    if not REPORT.exists():
        n_edges = sum(len(c["edges"]) for p in corpus["projects"] for c in p["compartments"])
        provenance = (f"Corpus: {len(corpus['projects'])} projects / {n_edges:,} edges "
                      f"(2026-10-14). Registry: {len(registry['nodes']):,} nodes / "
                      f"{len(registry['functions'])} functions (2026-10-14).\n")
        REPORT.write_text("# Taxonomy follow-up research report\n" + provenance + text, encoding="utf-8")
    else:
        t = REPORT.read_text(encoding="utf-8")
        m = re.search(r"^## 3\. ", t, flags=re.M)
        if not m:
            REPORT.write_text(t + ("\n" if not t.endswith("\n") else "") + text,
                              encoding="utf-8")
        else:
            nxt = re.search(r"^## ", t[m.end():], flags=re.M)
            end = m.end() + nxt.start() if nxt else len(t)
            REPORT.write_text(t[:m.start()] + text + t[end:], encoding="utf-8")
    for st in fam:
        print(f"{st['fid']}: {st['n_steps']} steps, {st['n_members']} members, {st['n_declared']} "
              f"declared edges, {st['n_wired']} wired, {st['n_never']} declared-but-never-wired, "
              f"{st['n_undec_pairs']} undeclared-real pairs ({st['undec_occ']:,} occurrences)")
    p = sum(1 for t in low if t["cls"] == "with-family")
    q = sum(1 for t in low if t["cls"] == "without-family")
    r = sum(1 for t in low if t["cls"] == "no-registry-producer-framework")
    print(f"family-candidate pairs: {len(pair_stats)} ({cnt['pair']:,} occurrences); "
          f"low-consumer types: {len(low)} ({p} with-family, {q} without-family"
          + (f", {r} no-registry-producer-framework" if r else "") + ")")
    print(f"(corpus: {cnt['total']:,} edges; {cnt['in_family']:,} in-family "
          f"(declared {cnt['declared']:,}, undeclared {cnt['undeclared']:,}), "
          f"{cnt['cross_family']:,} cross-family; typed tokens: {cnt['resolvable']:,} resolvable / "
          f"{cnt['unresolvable']:,} not; 3c cross-check: {'AGREE' if agree else 'DISAGREE'})")


if __name__ == "__main__":
    main()
