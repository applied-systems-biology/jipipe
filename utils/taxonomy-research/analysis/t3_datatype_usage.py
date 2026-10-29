#!/usr/bin/env python3
"""Task 3: data-type producer/consumer usage map over the real corpus (6.0.0).

Reads corpus.json + registry.json (locked, Tasks 1-2) and, for every data type
on a corpus edge, emits registry producers/consumers (capability) plus
corpus-observed producers/consumers (edge counts) and corpus edge/project
counts (sorted by edge count desc), plus the two flagged sub-tables (exactly
1-2 distinct producers/consumers over corpus occurrences). Tokens are resolved
against the registry: exact data-type id -> exact class FQN -> simple class
name (an ambiguous simple-name match is recorded, not guessed; unmatched ->
unresolved). No interpretation (Task 9's job). Stdlib only: json, re,
collections, pathlib.

Emits analysis/t3_table.md (the section-1 payload, verbatim),
analysis/unresolved_types.md, and report.md (created if absent; on re-runs only
the text between the "## 1." header and the next "## " header or EOF is
replaced, the rest of report.md untouched). All output byte-stable and
hash-seed-immune. Prints "<T> types, <X> flagged low-consumer, <Y> flagged
low-producer". Usage: python3 t3_datatype_usage.py
"""
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE = ROOT / "report.md", ROOT / "analysis" / "t3_table.md"
UNRESOLVED = ROOT / "analysis" / "unresolved_types.md"
DATE = "2026-10-14"  # corpus and registry dump dates (Tasks 1-2)


def build_registry_maps(registry):
    """(node_names, outputs_by_dt, inputs_by_dt, fqn_to_dts, simple_to_fqns,
    dts_to_fqns)."""
    names, outp, inp = {}, defaultdict(set), defaultdict(set)
    fqn_to_dts, simple_to_fqns, dts_to_fqns = (defaultdict(set) for _ in range(3))
    for n in registry["nodes"]:
        names[n["id"]] = n["name"]
        for io in n.get("outputs") or []:
            if io.get("dataType"):
                outp[io["dataType"]].add(n["id"])
        for io in n.get("inputs") or []:
            if io.get("dataType"):
                inp[io["dataType"]].add(n["id"])
        for io in list(n.get("outputs") or []) + list(n.get("inputs") or []):
            cls, dt = io.get("class"), io.get("dataType")
            if cls:
                simple_to_fqns[cls.rsplit(".", 1)[-1]].add(cls)
            if cls and dt:
                fqn_to_dts[cls].add(dt)
                dts_to_fqns[dt].add(cls)
    return names, outp, inp, fqn_to_dts, simple_to_fqns, dts_to_fqns


def resolve_token(token, dt_ids, fqn_to_dts, simple_to_fqns):
    """(kind, row_key); kind in datatype-id / fqn / simple-name / ambiguous /
    unresolved."""
    if token in dt_ids:
        return "datatype-id", token
    if token in fqn_to_dts:
        return "fqn", sorted(fqn_to_dts[token])[0]
    matches = set()
    for fqn in simple_to_fqns.get(token, ()):
        matches |= fqn_to_dts.get(fqn, set())
    if len(matches) == 1:
        return "simple-name", sorted(matches)[0]
    if len(matches) > 1:
        return "ambiguous", None
    return "unresolved", None


def aggregate_corpus(corpus):
    """Per raw edge token: edge count, project set, observed producer/consumer
    node-id -> edge-count maps. ONE project-wide uid->node map per project is
    built first: edges are re-homed into the TARGET compartment, so an edge's
    fromUid may reference a node in another compartment (4,661/31,826 do)."""
    stats = defaultdict(lambda: {"edges": 0, "projects": set(),
                                 "producers": defaultdict(int),
                                 "consumers": defaultdict(int)})
    null_edges = 0
    for p in corpus["projects"]:
        uid_to_id = {n["uid"]: n["id"] for c in p["compartments"] for n in c["nodes"]}
        for c in p["compartments"]:
            for e in c["edges"]:
                token = e.get("dataClass")
                if token is None:
                    null_edges += 1
                    continue
                s = stats[token]
                s["edges"] += 1
                s["projects"].add(p["file"])
                src, dst = uid_to_id.get(e["fromUid"]), uid_to_id.get(e["toUid"])
                if src is not None:
                    s["producers"][src] += 1
                if dst is not None:
                    s["consumers"][dst] += 1
    return stats, null_edges


def join_rows(stats, names, outp, inp, fqn_to_dts, simple):
    """One row per distinct raw token (the resolved key is the row identity),
    sorted by edge count desc, then raw token."""
    dt_ids = set(outp) | set(inp)
    rows = []
    for token, s in stats.items():
        kind, key = resolve_token(token, dt_ids, fqn_to_dts, simple)
        projects = sorted(s["projects"])
        rows.append({
            "raw": token, "kind": kind, "key": key,
            "edges": s["edges"], "projects": projects,
            "cap_p": sorted(outp.get(key, set())) if key else [],
            "cap_c": sorted(inp.get(key, set())) if key else [],
            "obs_p": sorted(s["producers"].items(), key=lambda kv: (-kv[1], kv[0])),
            "obs_c": sorted(s["consumers"].items(), key=lambda kv: (-kv[1], kv[0])),
            "example_project": projects[0] if projects else "",
        })
    rows.sort(key=lambda r: (-r["edges"], r["raw"]))
    return rows


def obs_count(row, side):
    """Distinct node ids observed producing/consuming the row's type (edges)."""
    key = "obs_p" if side == "producers" else "obs_c"
    return len({nid for nid, _ in row[key]})


def flagged(row, side):
    """Ruling: exactly 1-2 distinct producers/consumers over corpus occurrences."""
    return 1 <= obs_count(row, side) <= 2


def fmt_obs(id_counts, names):
    parts = [f"{nid} ({cnt})" if names.get(nid) in (None, "")
             else f"{nid} - {names.get(nid)} ({cnt})" for nid, cnt in id_counts]
    return ", ".join(parts) if parts else "(none - 0)"


def fmt_cap(ids):
    return ", ".join(f"`{i}`" for i in ids) if ids else "(none - 0)"


def table(rows, names):
    lines = [
        "| data-type id (row key) | raw corpus token | edges | projects | "
        "registry producers (capability) | registry consumers (capability) | "
        "corpus producers (node id - name, edge count) | "
        "corpus consumers (node id - name, edge count) |",
        "|---|---|---:|---:|---|---|---|---|",
    ]
    for r in rows:
        if r["key"]:
            key = f"`{r['key']}`"
            raw = f"`{r['raw']}`" if r["raw"] != r["key"] else "(= row key)"
        else:
            key, raw = f"`{r['raw']}` (**unresolved**)", "(= row key)"
        lines.append(f"| {key} | {raw} | {r['edges']} | {len(r['projects'])} | "
                     f"{fmt_cap(r['cap_p'])} | {fmt_cap(r['cap_c'])} | "
                     f"{fmt_obs(r['obs_p'], names)} | {fmt_obs(r['obs_c'], names)} |")
    return lines


def flagged_table(rows, side, names):
    obs_key = "obs_p" if side == "producers" else "obs_c"
    sub = [r for r in rows if flagged(r, side)]
    sub.sort(key=lambda r: (r["key"] is None, r["key"] or r["raw"]))
    lines = [
        f"| data-type id (row key) | distinct corpus {side} | "
        f"corpus {side} (node id - name, edge count) | edges | projects |",
        "|---|---:|---|---:|---:|",
    ]
    for r in sub:
        lines.append(f"| `{r['key'] or r['raw']}` | {obs_count(r, side)} | "
                     f"{fmt_obs(r[obs_key], names)} | {r['edges']} | "
                     f"{len(r['projects'])} |")
    return lines


def unresolved_md(rows):
    lines = [
        "# Task 3 - unresolved corpus data-class tokens", "",
        "Raw `slot-data-type` tokens on corpus edges that do not resolve against",
        "`registry.json` (no exact data-type id, no exact class FQN, no simple-name",
        "match; a simple-name match hitting more than one data type is listed as",
        "`ambiguous` instead of guessed). These are the corpus-side 'unresolvable'",
        "tokens of Review Focus #3 - their edges cannot be attributed to a",
        "registered data type (e.g. a legacy id that no longer exists in 6.0.0).",
        "",
    ]
    bad = [r for r in rows if r["kind"] in ("unresolved", "ambiguous")]
    if not bad:
        lines.append("(none - every raw edge token resolved)")
    else:
        lines += ["| raw token | kind | edges | projects | example project |",
                  "|---|---|---:|---:|---|"]
        for r in sorted(bad, key=lambda r: (-r["edges"], r["raw"])):
            lines.append(f"| `{r['raw']}` | {r['kind']} | {r['edges']} | "
                         f"{len(r['projects'])} | `{r['example_project']}` |")
    lines.append("")
    return "\n".join(lines)


def verification_lines(rows, dts_to_fqns, names):
    """Step 2, computed from the data (no interpretation): the trackmate
    'spots' data type and where the detector node ids sit in the chain."""
    by_key = {r["key"]: r for r in rows if r["key"]}
    spot, det = by_key.get("trackmate-spot-collection"), by_key.get("trackmate-spot-detector")
    if not spot or not det:
        return ["Step 2 verification: FAILED - expected trackmate types missing."]
    fq = lambda dt: ", ".join(f"`{c}`" for c in sorted(dts_to_fqns.get(dt, ())))
    present = "trackmate-tracking" in {n for n, _ in spot["obs_c"]}
    cap_det = [n for n in det["cap_p"] if n.startswith("trackmate-create-spot-detector-")]
    return [
        f"Step 2 verification (computed from the data): the trackmate **spots** "
        f"data type is `{spot['key']}` (class {fq(spot['key'])}); there is no "
        f"`SpotsCollectionData`-token or `trackmate-find-particles` node in the "
        f"corpus/registry. Registry capability: the {len(cap_det)} "
        f"`trackmate-create-spot-detector-*` detector nodes "
        f"({fmt_cap(cap_det)}) output `{det['key']}` (class {fq(det['key'])}); "
        f"in the corpus `{det['key']}` is produced by {fmt_obs(det['obs_p'], names)} "
        f"and consumed by {fmt_obs(det['obs_c'], names)}. Corpus producers of "
        f"`{spot['key']}`: {fmt_obs(spot['obs_p'], names)} (registry capability "
        f"{fmt_cap(spot['cap_p'])}); corpus consumers: {fmt_obs(spot['obs_c'], names)} "
        f"- `trackmate-tracking` is "
        f"{'present' if present else '**ABSENT**'} among them.",
    ]


def section_body(rows, names, dts_to_fqns):
    unres = [r for r in rows if r["kind"] == "unresolved"]
    ambig = [r for r in rows if r["kind"] == "ambiguous"]
    low_c = [r for r in rows if flagged(r, "consumers")]
    low_p = [r for r in rows if flagged(r, "producers")]
    only_out = [r for r in rows if not r["obs_c"]]
    only_in = [r for r in rows if not r["obs_p"]]
    n_res = len(rows) - len(unres) - len(ambig)
    out = [
        "Generated by `analysis/t3_datatype_usage.py`; re-running replaces this "
        "section in place (rest of `report.md` untouched). Raw tables: "
        "`analysis/t3_table.md`; unresolved tokens: `analysis/unresolved_types.md`.",
        "",
        "Method: each corpus edge carries the raw on-disk `slot-data-type` token "
        "of its source output slot (0 of 31,826 edges carry a null token). Tokens "
        "are resolved against the registry in the order exact data-type id → "
        "exact class FQN → simple class name (an ambiguous simple-name match is "
        "recorded, not guessed); unresolved tokens go to "
        "`analysis/unresolved_types.md`. Producer/consumer node ids are resolved "
        "project-wide (edges are re-homed into the target compartment; `fromUid` "
        "may point at a node in another compartment). “Registry … (capability)” = "
        "distinct registered node ids declaring that data type on an output/input "
        "slot; “corpus … (edge count)” = the same counted over corpus edges, i.e. "
        "what pipelines actually wire. All tables sorted by edge count descending.",
        "",
        f"### Table 1a - All data types observed on corpus edges ({len(rows)} "
        f"distinct raw tokens: {n_res} resolved"
        + (f", {len(unres)} unresolved" if unres else "")
        + (f", {len(ambig)} ambiguous" if ambig else "") + ")",
        "",
    ]
    out += table(rows, names)
    out += ["",
            f"### Table 1b - Flagged: exactly 1–2 distinct corpus producers "
            f"({len(low_p)} types)", ""]
    out += flagged_table(rows, "producers", names)
    out += ["",
            f"### Table 1c - Flagged: exactly 1–2 distinct corpus consumers "
            f"({len(low_c)} types)", ""]
    out += flagged_table(rows, "consumers", names)
    out.append("")
    if unres or ambig:
        lines = [
            "Flagged types that are **UNRESOLVED** (raw token not in the "
            "registry; no registry capability, no node attribution - these "
            "matter more than the registry-known flags above) vs "
            "**registry-known**:", "",
            "| flagged type | status |", "|---|---|",
        ]
        seen = set()
        for r in sorted(low_p + low_c,
                        key=lambda r: (r["key"] is None, r["key"] or r["raw"])):
            key = r["key"] or r["raw"]
            if key in seen:
                continue
            seen.add(key)
            status = {"unresolved": "UNRESOLVED - raw token not in registry",
                      "ambiguous": "AMBIGUOUS simple-name match"}.get(
                          r["kind"], "registry-known")
            lines.append(f"| `{key}` | {status} |")
        out += lines + [""]
    basis = ["Tables 1b/1c count distinct node ids over corpus occurrences "
             "(edges), not registry capability."]
    if only_out:
        basis.append("Types never consumed in the corpus: " +
                     ", ".join(f"`{r['key'] or r['raw']}`" for r in only_out) + ".")
    if only_in:
        basis.append("Types never produced in the corpus: " +
                     ", ".join(f"`{r['key'] or r['raw']}`" for r in only_in) + ".")
    elif not (only_out or only_in):
        basis.append("No type has 0 producers or 0 consumers in the corpus, so "
                     "the exactly 1–2 flag is equivalent to ≤2 here.")
    out += basis + [""]
    out += verification_lines(rows, dts_to_fqns, names)
    return "\n".join(out).rstrip() + "\n"


def render_report(rows, names, corpus, registry, dts_to_fqns):
    n_projects = len(corpus["projects"])
    n_comps = sum(len(p["compartments"]) for p in corpus["projects"])
    n_nodes = sum(len(c["nodes"]) for p in corpus["projects"] for c in p["compartments"])
    n_edges = sum(len(c["edges"]) for p in corpus["projects"] for c in p["compartments"])
    body = section_body(rows, names, dts_to_fqns)
    if not REPORT.exists():
        provenance = (f"Corpus: {n_projects} projects / {n_comps} compartments / "
                      f"{n_nodes:,} nodes / {n_edges:,} edges ({DATE}). Registry: "
                      f"{len(registry['nodes']):,} nodes / {len(dts_to_fqns)} "
                      f"distinct data-type ids ({DATE}).") + "\n"
        REPORT.write_text("# Taxonomy follow-up research report\n" + provenance +
                          "## 1. Data-type usage map\n" + body, encoding="utf-8")
        return "created"
    text = REPORT.read_text(encoding="utf-8")
    m = re.search(r"^## 1\. ", text, flags=re.M)
    if not m:
        raise SystemExit("report.md exists but has no '## 1. ' section")
    nxt = re.search(r"^## ", text[m.end():], flags=re.M)
    end = m.end() + nxt.start() if nxt else len(text)
    REPORT.write_text(text[:m.start()] + "## 1. Data-type usage map\n" + body +
                      text[end:], encoding="utf-8")
    return "replaced section 1 in place"


def main():
    corpus = json.load(open(ROOT / "corpus.json", encoding="utf-8"))
    registry = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    names, outp, inp, fqn_to_dts, simple, dts_to_fqns = build_registry_maps(registry)
    stats, null_edges = aggregate_corpus(corpus)
    rows = join_rows(stats, names, outp, inp, fqn_to_dts, simple)
    TABLE.parent.mkdir(parents=True, exist_ok=True)
    TABLE.write_text(section_body(rows, names, dts_to_fqns), encoding="utf-8")
    UNRESOLVED.write_text(unresolved_md(rows), encoding="utf-8")
    result = render_report(rows, names, corpus, registry, dts_to_fqns)
    low_c = sum(1 for r in rows if flagged(r, "consumers"))
    low_p = sum(1 for r in rows if flagged(r, "producers"))
    bad = sum(1 for r in rows if r["kind"] in ("unresolved", "ambiguous"))
    print(f"{len(rows)} types, {low_c} flagged low-consumer, {low_p} "
          f"flagged low-producer")
    print(f"(null-token edges: {null_edges}; unresolved/ambiguous raw tokens: "
          f"{bad}; report.md: {result})")


if __name__ == "__main__":
    main()
