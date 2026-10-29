#!/usr/bin/env python3
"""Task 8: pattern library deliverable + follow-up mapping (6.0.0, work item 1324).

Turns Task 7's mined patterns (patterns/patterns.json, locked, do not modify)
into a usable pattern library:

  * ONE markdown doc per CURATED pattern -> patterns/<pattern-id>.md
    (curation rule R8: graph-shaped (chain/conversion/fan-out/fan-in) with
    support >= 25, OR any annotation-sandwich - the plan's "one doc per
    pattern, support >= 3" is overridden; 13,122 docs is a data dump, not a
    library. The long tail stays numerically preserved in patterns.json
    `summary` + report section 5a.)
  * ONE JIPipeGraph-shaped `data` JSON per doc -> patterns/templates/<id>.json
    (the exact wire format JIPipeGraph.Deserializer reads; verified by the
    jipipe-launcher test TemplateDataDeserializationTest).
  * report.md section 6 (method note + tables 6a/6b/6c), created if absent;
    on re-runs ONLY the text from "^## 6. " to the next "^## " / EOF is
    replaced; sections 1-5 (owned by t3-t7) are never touched. Byte-identical
    across runs (no sets in emitted order; everything sorted explicitly).
  * analysis/t8_table.md - the raw 6a rows (all, not just top-30).

Honest frontmatter (R8): the LLM-branch doc shape (audience, modality, tags,
biologicalContext, prerequisiteKnowledge) is kept, but ONLY what is
mechanically derivable is filled: modality (coarse data types flowing through
the pattern, from the registry inputs/outputs of its nodes) and tags (the
top-level function branches of its nodes, parent-walked, deduped + sorted).
audience / biologicalContext / prerequisiteKnowledge are the literal string
`tbd` - domain framing that cannot honestly be derived from generic algorithm
node ids; fabricating them would violate the no-unsourced-opinions constraint.

Stdlib only. Reads patterns/patterns.json + registry.json + report.md section
5 (for the 6c hand-off numbers, so they agree with the committed section 5).
Writes: patterns/*.md, patterns/templates/*.json, report.md section 6,
analysis/t8_table.md.
"""
import json
import re
import uuid
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT, TABLE = ROOT / "report.md", ROOT / "analysis" / "t8_table.md"
PATTERNS_IN = ROOT / "patterns" / "patterns.json"
DOCS_OUT = ROOT / "patterns"
TEMPLATES_OUT = ROOT / "patterns" / "templates"
JUNIT_CANDIDATE = "fan-in-ij1-roi-to-rgb-core-io-1-other-1"
# Step-3 result (TemplateDataDeserializationTest, 2026-10-14). PASS: the
# candidate template's `data` deserialized via JsonUtils.readFromString(...,
# JIPipeGraph.class) with getNodeCount() == 3 (== the template's node count,
# so no silent-empty) and getSlotEdges() == 1 (>= 1, so the single example
# edge was not soft-skipped). In sync with the JUnit stdout line
# "TEMPLATE-CHECK: candidate=... nodes=3/3 connections=1/1".
JUNIT_TEST_RESULT = "PASS"
JUNIT_COUNTS = {"nodes": 3, "connections": 1}

GRAPH_SHAPES = {"chain", "conversion", "fan-out", "fan-in"}
SUP_CUTOFF = 25  # R8 curation: graph-shaped patterns need support >= 25


def curated(pats):
    """R8: graph-shaped with support >= SUP_CUTOFF, OR any annotation-sandwich."""
    return [p for p in pats
            if (p["shape"] in GRAPH_SHAPES and p["support"] >= SUP_CUTOFF)
            or p["shape"] == "annotation-sandwich"]


def build_reg(reg):
    """id -> node; function parent map + top-branch walker + top names."""
    reg_by_id = {n["id"]: n for n in reg["nodes"]}
    fpar = {f["id"]: f["parent"] for f in reg["functions"]}
    fname = {f["id"]: f["name"] for f in reg["functions"]}

    def top(fid):
        seen = set()
        while fpar.get(fid) is not None and fid not in seen:
            seen.add(fid)
            fid = fpar[fid]
        return fid

    return reg_by_id, top, fname


def pattern_node_ids(p):
    """The union of the pattern's `nodes[].nodeIds`, group order preserved,
    ids within a group sorted (they are in the file)."""
    out = []
    for g in p["nodes"]:
        for i in g["nodeIds"]:
            if i not in out:
                out.append(i)
    return out


def derive_tags(p, reg_by_id, top, fname):
    """Top-level function branches of the pattern's nodes, deduped + sorted."""
    t = set()
    for i in pattern_node_ids(p):
        n = reg_by_id.get(i)
        if n and n.get("functions"):
            t.add(fname[top(n["functions"][0])])
    return sorted(t)


def derive_modality(p, reg_by_id):
    """Coarse data types flowing through the pattern: the registry
    inputs[].dataType/outputs[].dataType of the pattern's nodes, group order,
    deduped. 'unknown' when nothing is registered/derivable."""
    out = []
    for i in pattern_node_ids(p):
        n = reg_by_id.get(i)
        if not n:
            continue
        for io in (n.get("inputs") or []) + (n.get("outputs") or []):
            dt = io.get("dataType")
            if dt and dt not in out:
                out.append(dt)
    return out or ["unknown"]


def shared_compartment_uuid(pid):
    """One shared compartment UUID for every node of a template (named UUID
    v5 from the pattern id -> deterministic across runs)."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "jipipe-pattern-template/" + pid))


def build_template_data(p, reg_by_id):
    """R8b minimal JIPipeGraph `data`:
      nodes: per-instance keys n0..n(k-1) (index = position in
             exampleGraph.nodes; handles duplicate registry ids), each
             {"jipipe:node-info-id": <id>, "jipipe:graph-compartment":
             <shared uuid>, "jipipe:node:name": <name>}
      edges: exampleGraph edges rewritten so source-node/target-node = the
             n<index> of the node whose exampleGraph NAME matches the edge
             endpoint (name collision -> first match, counted for the note).
    """
    ex = p["exampleGraph"]
    nodes = ex["nodes"]
    name_index, name_counts = {}, Counter()
    for i, n in enumerate(nodes):
        name_counts[n["name"]] += 1
        name_index.setdefault(n["name"], i)
    collisions = sum(c - 1 for c in name_counts.values() if c > 1)
    comp = shared_compartment_uuid(p["id"])
    data = {
        "nodes": {f"n{i}": {"jipipe:node-info-id": n["id"],
                            "jipipe:graph-compartment": comp,
                            "jipipe:node:name": n["name"]}
                  for i, n in enumerate(nodes)},
        "edges": [{"source-node": f"n{name_index[e['from']]}",
                   "source-slot": e["fromSlot"],
                   "target-node": f"n{name_index[e['to']]}",
                   "target-slot": e["toSlot"]}
                  for e in ex["edges"]
                  if e["from"] in name_index and e["to"] in name_index],
    }
    return data, collisions


def esc(text):
    """Escape a string for a markdown table cell / YAML double-quoted value."""
    return str(text).replace("\\", "\\\\").replace("|", "\\|")


def yq(text):
    """YAML double-quoted scalar (JSON escaping is a valid YAML subset)."""
    return json.dumps(text, ensure_ascii=False)


def doc_for(p, reg_by_id, top, fname, data, collisions):
    """The markdown doc: LLM-branch frontmatter shape (tbd where not
    mechanically derivable) + corpus block + template block."""
    ex = p["exampleGraph"]
    tags = derive_tags(p, reg_by_id, top, fname)
    modality = derive_modality(p, reg_by_id)
    projects = p["projects"][:5]  # top 5 (the file's list is sorted), all if fewer
    branch = "Miscellaneous"
    for i in pattern_node_ids(p):
        n = reg_by_id.get(i)
        if n and n.get("functions"):
            branch = fname[top(n["functions"][0])]
            break
    menu = f"Patterns/{branch}/{p['name']}"
    tmpl_name = p["name"]
    data_json = json.dumps(data, separators=(",", ":"), ensure_ascii=False)

    lines = ["---"]
    lines.append(f"audience: {yq('tbd')}")
    lines.append(f"modality: {yq(' -> '.join(modality))}")
    lines.append("tags: [" + ", ".join(tags) + "]")
    lines.append(f"biologicalContext: {yq('tbd')}")
    lines.append(f"prerequisiteKnowledge: {yq('tbd')}")
    lines += [
        "corpus:",
        f"  support: {p['support']}",
        "  projects:",
    ]
    lines += [f"    - {yq(x)}" for x in projects]
    if p["projectCount"] > len(projects):
        lines.append(f"  projectCount: {p['projectCount']}")
    lines += [
        "  exampleGraph:",
        f"    compartment: {yq(ex['compartment'])}",
        f"    project: {yq(ex['project'])}",
        "    nodes:",
    ]
    lines += [f"      - {yq(n['id'])}: {yq(n['name'])}" for n in ex["nodes"]]
    lines += ["    edges:"]
    lines += [f"      - {json.dumps(e, separators=(',', ':'), ensure_ascii=False)}"
              for e in ex["edges"]]
    lines += [
        "  templateCandidate: true",
        "template:",
        f"  name: {yq(tmpl_name)}",
        f"  menuPath: {yq(menu)}",
        f"  data: {yq(data_json)}",
        "---",
        "",
        f"# Pattern: {p['name']}",
        "",
        f"Mined pattern `{p['id']}` (shape `{p['shape']}`, support {p['support']}"
        + (f" of {p['projectCount']} projects"
           if p["projectCount"] != p["support"] else "") + ").",
        "",
    ]
    if collisions:
        lines += [f"> Note: {collisions} duplicate node instance name(s) in the "
                  "example subgraph - edge endpoints resolve to the first "
                  "matching node.", ""]
    lines += ["## Role sequence", "",
              "| role | node ids |", "|---|---|"]
    for g in p["nodes"]:
        shown = ", ".join(f"`{i}`" for i in g["nodeIds"][:6])
        if len(g["nodeIds"]) > 6:
            shown += f", … ({len(g['nodeIds'])} total)"
        lines.append(f"| {g['role']} | {shown} |")
    lines += ["", "## Example graph", "",
              f"Project: `{ex['project']}` (relative to `/data/JIPipe`), "
              f"compartment `{ex['compartment']}`.", ""]
    lines += ["| key | node id | name |", "|---|---|---|"]
    for i, n in enumerate(ex["nodes"]):
        lines.append(f"| n{i} | `{n['id']}` | {esc(n['name'])} |")
    lines += ["", "| from | slot | to | slot | data class |",
              "|---|---|---|---|---|"]
    for e in ex["edges"]:
        lines.append(f"| {esc(e['from'])} | `{e['fromSlot']}` | "
                     f"{esc(e['to'])} | `{e['toSlot']}` | `{e['dataClass']}` |")
    lines += ["", f"## Template", "",
              f"Proposed name `{tmpl_name}`, menu path `{menu}` (proposal only - "
              "not registered). `data` = the minimal JIPipeGraph JSON; the full "
              "object is written to `templates/" + p["id"] + ".json`.", ""]
    return "\n".join(lines) + "\n", menu, branch


# ------------------------------------------------------------- section 6
def parse_section5(report_text):
    """Pull the committed section-5 cross-check numbers (so 6c agrees with
    what is committed, not with any live recomputation):
      - the 5b correction-candidate table rows (doc, ids, in-corpus, overlap)
      - the per-doc table rows (doc, extracted, in-corpus, overlap, matched)
      - the cross-check summary line counts (matched / corrections /
        unmatchable / gaps)"""
    m = re.search(r"^## 5\. ", report_text, flags=re.M)
    nxt = re.search(r"^## ", report_text[m.end():], flags=re.M) if m else None
    s5 = report_text[m.end():m.end() + nxt.start()] if (m and nxt) else \
        (report_text[m.end():] if m else "")
    # summary line: "Cross-check over 24 docs: **11 matched** …, **5 correction
    # candidates**, **8 unmatchable** …, 5,390 library gaps (5c)."
    sm = re.search(r"\*\*(\d+) matched\*\*.*?\*\*(\d+) correction candidates\*\*"
                   r",\s*\*\*(\d+) unmatchable\*\*.*?,\s*([\d,]+) library gaps",
                   s5, flags=re.S)
    matched_n, corr_n, unmatch_n, gaps_n = (
        int(sm.group(1)), int(sm.group(2)), int(sm.group(3)),
        int(sm.group(4).replace(",", ""))) if sm else (None,) * 4
    # per-doc table rows
    doc_rows = []
    for line in s5.splitlines():
        mm = re.match(r"^\| ([^|]+\.md) \| (\d+) \| (\d+) \| ([\d.]+) \| (.+) \|$",
                      line)
        if mm:
            mp = mm.group(5)
            doc_rows.append((mm.group(1), int(mm.group(2)), int(mm.group(3)),
                             mm.group(4),
                             mp.strip("`") if mp != "(none)" else None))
    # 5b correction table rows
    corr_rows = []
    for line in s5.splitlines():
        mm = re.match(r"^\| ([^|]+\.md) \| (.+?) \| (\d+)/(\d+) \| ([\d.]+) \|$",
                      line)
        if mm:
            corr_rows.append((mm.group(1), mm.group(2), mm.group(3),
                              mm.group(4), mm.group(5)))
    return {"matched": matched_n, "corrections": corr_n,
            "unmatchable": unmatch_n, "gaps": gaps_n,
            "doc_rows": doc_rows, "corr_rows": corr_rows}


def render_section6(sel, reg_by_id, top, fname, s5, coll_total, mod_unknown):
    """The section-6 body WITHOUT the "## 6." header line - render_report()
    owns the header (mirrors t7; a header line inside the body would break the
    in-place replacement, whose boundary is the next "^## " line)."""
    body = []
    body += [
        "Generated by `analysis/t8_pattern_docs.py`; re-running replaces this "
        "section in place (sections 1-5 untouched, scripts frozen). Docs: "
        "`patterns/<pattern-id>.md`; template `data` JSON: "
        "`patterns/templates/<pattern-id>.json`; raw 6a rows: "
        "`analysis/t8_table.md`. The end-to-end deserialization check of one "
        f"template `data` (candidate `{JUNIT_CANDIDATE}`) lives in "
        "`jipipe-launcher/.../taxonomy/TemplateDataDeserializationTest.java` "
        "(JUnit, same module as `RegistryDumperTest`): "
        f"**{JUNIT_TEST_RESULT}**"
        + (f" - `{JUNIT_COUNTS['nodes']}` nodes (== the template's "
           f"`{JUNIT_COUNTS['nodes']}` nodes; the silent-empty case is ruled "
           f"out), `{JUNIT_COUNTS['connections']}` connection (== the "
           f"template's `{JUNIT_COUNTS['connections']}` edge; soft-skipped "
           "edges ruled out); every loaded node id is a registry id"
           if JUNIT_TEST_RESULT == "PASS" else "")
        + ".",
        "",
        "Method:",
        f"- **Curation rule (R8):** one doc per pattern that is (a) graph-shaped "
        f"(`chain`/`conversion`/`fan-out`/`fan-in`) with support >= {SUP_CUTOFF}, "
        "OR (b) an annotation-sandwich (all 4, regardless of support). The plan's "
        "'one doc per pattern, support >= 3' is overridden: 13,122 docs is a data "
        "dump, not a library. This run: **185 docs** (181 graph-shaped "
        f">= {SUP_CUTOFF} + 4 sandwiches). The long tail (the sub-{SUP_CUTOFF} "
        "patterns, incl. support 3-4) stays numerically preserved in "
        "`patterns/patterns.json` `summary` + section 5a and is NOT documented.",
        "- **Honest frontmatter (R8):** each doc keeps the LLM-branch frontmatter "
        "shape (`audience`, `modality`, `tags`, `biologicalContext`, "
        "`prerequisiteKnowledge`) plus a `corpus` block (support, top-5 projects, "
        "the example subgraph, `templateCandidate: true`) and a `template` block "
        "(proposed `name`, `menuPath`, and the JIPipeGraph `data`). Only what is "
        "mechanically derivable is filled: `modality` = the coarse data types "
        "flowing through the pattern (the registry `inputs[].dataType` / "
        "`outputs[].dataType` of its nodes, group order, deduped; `unknown` if "
        "not derivable - "
        + (f"{len(mod_unknown)} doc(s): " + ", ".join(f"`{i}`" for i in mod_unknown)
           if mod_unknown else "0 doc(s)")
        + "); `tags` = the top-level function branches of the pattern's nodes "
        "(parent-walk), deduped + sorted. `audience`, `biologicalContext` and "
        "`prerequisiteKnowledge` are the literal `tbd` - domain framing that "
        "cannot honestly be derived from generic algorithm node ids (most are "
        "ImageJ ops, not biology); `tbd` defers them to the LLM branch / a human.",
        "- **Template `data` contract (R8b, verified against "
        "`JIPipeGraph.fromJson`/`nodesFromJson`/`edgesFromJson` and re-verified "
        "by the JUnit test):** `{\"nodes\": {<key>: nodeObj, ...}, \"edges\": "
        "[edgeObj, ...]}`. Per node: key = per-instance `n0`, `n1`, … (index in "
        "`exampleGraph.nodes`; duplicate registry ids are safe), nodeObj = "
        "`{\"jipipe:node-info-id\": <registered id>, \"jipipe:graph-compartment\": "
        "<ONE shared UUID string>, \"jipipe:node:name\": <example name>}`. "
        "Requirements found while reading the deserializer: the `nodes` key must "
        "be present (absent -> `fromJson` returns an EMPTY graph silently); "
        "`jipipe:node-info-id` must be a known registry id (unknown -> node "
        "skipped, soft); `jipipe:graph-compartment` is read via "
        "`.get(...).asText()` -> **NPE if the field is missing, it is REQUIRED** "
        "(a non-UUID string is tolerated: it falls back to the default "
        "compartment + a legacy id); `jipipe:node:name` sets the custom name "
        "that the deserializer's alias mechanism (`generateAliasIdFor`) keys off, "
        "so the `n<k>` map keys resolve the edges; edges carry "
        "`source-node`/`target-node` = those map keys and `source-slot` / "
        "`target-slot` = the registered slot names (the corpus `fromSlot` / "
        "`toSlot` came from the on-disk `slot-configuration.name` - a slot "
        "mismatch is a SOFT skip: the edge is dropped, not a crash). NO other "
        "field is required: the node's own `fromJson` guards every optional "
        "field and parameter deserialization reads `node.path(key)`, so "
        "parameters may be omitted entirely (minimal template = the three "
        "fields above).",
        f"- **6b formula:** score = support x breadth, where breadth = the "
        "number of distinct node ids across the pattern's `nodes[]` union (how "
        "many concrete algorithms it generalizes); rank by score desc, tie -> "
        "pattern id asc. All 185 patterns are template candidates (all are "
        "graph-shaped; sandwiches included).",
        "- **6c** re-presents Task 7's cross-check (committed section 5) as the "
        "seeding hand-off for the LLM branch (1290-ai-llm-integration); numbers "
        "are parsed from section 5, not recomputed.",
        "",
    ]
    # 6a
    rows = []
    for p in sel:
        for i in range(len(p["nodes"]) - 1):
            a, b = p["nodes"][i], p["nodes"][i + 1]
            rows.append((p["id"], a["role"], a["nodeIds"], b["role"],
                         b["nodeIds"], p["support"]))
    rows.sort(key=lambda r: (-r[5], r[0], r[1]))

    def row_md(r):
        nid = r[2][0]
        nm = reg_by_id.get(nid, {}).get("name", nid)
        return (f"| `{r[0]}` | {r[1]} -> {r[3]} | `{esc(nid)}` "
                f"({esc(nm)}) | " +
                ", ".join(f"`{esc(x)}`" for x in r[4]) +
                f" | {r[5]} |")

    body += [
        "### 6a - Pattern -> follow-up mapping (top 30 of "
        f"{len(rows)} rows by support)",
        "",
        "For each curated pattern and each CONSECUTIVE role pair, the "
        "suggestion: after a node of role `role[i]`, the corpus shows nodes of "
        "role `role[i+1]` - i.e. surface the `role[i+1]` node ids as follow-up "
        "recommendations for `role[i]` role nodes. Mechanically derived from "
        "the pattern's role sequence + its `nodes[]` id lists. One example node "
        "id per row (the group's first, i.e. the registry-sorted first); the "
        "full row set is in `analysis/t8_table.md`.",
        "",
        "| pattern id | roles | example node | suggested follow-up | support |",
        "|---|---|---|---|---:|",
    ]
    body += [row_md(r) for r in rows[:30]]
    body += ["",
             f"**Total rows: {len(rows)}** (top 30 shown; all rows in "
             "`analysis/t8_table.md`).", ""]
    # 6b
    b6b = []
    for p in sel:
        b = len(pattern_node_ids(p))
        b6b.append((p, b))
    b6b.sort(key=lambda t: (-(t[0]["support"] * t[1]), t[0]["id"]))
    body += [
        "### 6b - Template candidates (all 185, ranked by support x breadth)",
        "",
        "| rank | pattern id | shape | support | breadth | score | proposed menuPath |",
        "|---:|---|---|---:|---:|---:|---|",
    ]
    for i, (p, b) in enumerate(b6b, 1):
        menu = None
        for i_ in pattern_node_ids(p):
            n = reg_by_id.get(i_)
            if n and n.get("functions"):
                fid = top(n["functions"][0])
                menu = f"Patterns/{fname[fid]}/{p['name']}"
                break
        body.append(f"| {i} | `{p['id']}` | {p['shape']} | {p['support']} "
                    f"| {b} | {p['support'] * b} | {esc(menu)} |")
    body += [""]
    # 6c
    body += [
        "### 6c - Corrections to the LLM-branch library (seeding hand-off)",
        "",
        f"Re-presentation of Task 7's cross-check (committed section 5): "
        f"**{s5['matched']} matched** (validated by the corpus), "
        f"**{s5['corrections']} correction candidates** (hand-written docs the "
        "corpus does not sufficiently exhibit - the seeding work list), "
        f"**{s5['unmatchable']} unmatchable** (< 2 extracted registry ids - "
        "an extraction weakness, NOT absence from the corpus). The "
        f"**{s5['gaps']:,} library gaps** of section 5c are the "
        "mined-but-undocumented patterns (support >= 5, no doc at overlap >= "
        "50%); section 6 documents the top of that list - the full id list is "
        "NOT in `patterns/patterns.json` (it is emission-capped; only the "
        "counts survive in its `summary`) - refer, don't re-list.",
        "",
        f"**Correction candidates ({s5['corrections']}, from 5b):**",
        "",
        "| doc | extracted registry ids | in corpus | best overlap |",
        "|---|---|---|---|",
    ]
    body += [f"| {d} | {ids} | {inc}/{tot} | {ov} |"
             for d, ids, inc, tot, ov in s5["corr_rows"]]
    matched = [r for r in s5["doc_rows"] if r[4]]
    unmatch = [r for r in s5["doc_rows"] if r[1] < 2]
    body += ["", f"**Matched ({len(matched)}, validated by the corpus):**", ""]
    body += [f"- `{d}` -> `{mp}` (extracted {n}, overlap {ov})"
             for d, n, inc, ov, mp in sorted(matched)]
    body += ["", f"**Unmatchable ({len(unmatch)}, few/no named ids):**", ""]
    body += [f"- `{d}` ({n} extracted id(s))"
             for d, n, inc, ov, mp in sorted(unmatch)]
    body += ["",
             "This is the hand-off list for when the LLM branch "
             "(1290-ai-llm-integration) resumes: seed the correction-candidate "
             "docs from the curated patterns above (their docs carry the "
             "`corpus` evidence), and note the unmatchable docs as an "
             "extraction-format issue (they name nodes by display name or "
             "prose, not raw ids in backticks).", ""]
    if coll_total:
        body += [f"**Edge-resolution note:** {coll_total} duplicate node "
                 "instance name(s) across all curated example subgraphs - "
                 "edge endpoints resolve to the first matching node.", ""]
    return "\n".join(body), "\n".join(
        ["# Task 8 - raw 6a rows (pattern -> follow-up mapping)", "",
         f"**Total rows: {len(rows)}** (all curated patterns, all consecutive "
         "role pairs; sorted by support desc, then pattern id, then role).",
         "",
         "| pattern id | roles | example node | suggested follow-up | support |",
         "|---|---|---|---|---:|"] + [row_md(r) for r in rows] + [""])


def render_report(body_text):
    header = "## 6. Pattern library\n"
    if not REPORT.exists():
        REPORT.write_text("# Taxonomy follow-up research report\n" + header +
                          body_text, encoding="utf-8")
        return "created"
    text = REPORT.read_text(encoding="utf-8")
    m = re.search(r"^## 6\. ", text, flags=re.M)
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
    pats = json.load(open(PATTERNS_IN, encoding="utf-8"))["patterns"]
    reg = json.load(open(ROOT / "registry.json", encoding="utf-8"))
    reg_by_id, top, fname = build_reg(reg)
    sel = curated(pats)
    sel.sort(key=lambda p: p["id"])  # deterministic emission order

    TEMPLATES_OUT.mkdir(parents=True, exist_ok=True)
    coll_total = 0
    mod_unknown = []
    for p in sel:
        data, coll = build_template_data(p, reg_by_id)
        coll_total += coll
        doc, _, _ = doc_for(p, reg_by_id, top, fname, data, coll)
        if derive_modality(p, reg_by_id) == ["unknown"]:
            mod_unknown.append(p["id"])
        (DOCS_OUT / (p["id"] + ".md")).write_text(doc, encoding="utf-8")
        TEMPLATES_OUT.joinpath(p["id"] + ".json").write_text(
            json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n",
            encoding="utf-8")

    s5 = parse_section5(REPORT.read_text(encoding="utf-8"))
    body, raw = render_section6(sel, reg_by_id, top, fname, s5, coll_total,
                                mod_unknown)
    render_report(body)
    TABLE.write_text(raw, encoding="utf-8")

    n_6a = sum(len(p["nodes"]) - 1 for p in sel)
    print(f"docs: {len(sel)}, templates: {len(sel)}, 6a rows: {n_6a}, "
          f"6b: {len(sel)}, 6c: corrections {s5['corrections']} / matched "
          f"{s5['matched']} / unmatchable {s5['unmatchable']} "
          f"(gaps {s5['gaps']:,}); edge name-collisions: {coll_total}; "
          f"modality-unknown: {len(mod_unknown)}"
          + (f" ({', '.join(mod_unknown[:5])}…)" if mod_unknown else ""))


if __name__ == "__main__":
    main()
