#!/usr/bin/env python3
"""Generate the bundled System node-template patterns (task 8).

This script turns the corpus-mined pattern study artifacts into the JIPipe
bundled System node-template resources:

  * the deduplicated raw ``JIPipeGraph``-shaped graph files
    (``{"nodes": ..., "edges": ...}``) -- copied VERBATIM (cp, not move) from
    the research directory, which is a study artifact and is left untouched; and
  * a committed metadata sidecar (``system-templates.json``) that carries the
    per-template ``name`` / ``description`` / ``functionId`` / ``file``.

It is DETERMINISTIC: re-running it over an unchanged research directory
produces byte-identical output (sorted iteration throughout; stable JSON
serialization). It is meant to be (re-)run from the repository root:

    python3 utils/generate-system-template-metadata.py

Inputs (all under ``utils/taxonomy-research/`` -- read-only):
  * ``patterns/templates/*.json``  -- the 185 raw minimal ``JIPipeGraph`` JSONs.
  * ``patterns/*.md``              -- one per pattern: YAML frontmatter
    (``template.name``, ``corpus`` block, ``tags``). The frontmatter also
    carries the name/description/tags used by the sidecar. (The markdown's
    "## Role sequence" table is NOT used for the function rule -- it is
    truncated on 13 patterns; see the rule below.)
  * ``patterns/patterns.json``     -- the complete structured pattern data:
    a list of ``{"id", "nodes": [{"role", "nodeIds": […]}, …], …}``. The
    function rule's node set is the deduped set of all ``nodeIds`` per pattern.
  * ``registry.json``              -- the node + function registry the rule is
    computed against (``nodes[].functions[]``, ``functions[].parent``).

Dedup rule (R8 revised -- ship 145 of the 185):
  Group the 185 by graph signature = (sorted list of node-info-ids, sorted list
  of ``(sourceNodeInfoId, sourceSlot, targetNodeInfoId, targetSlot)`` edge
  tuples; instance node keys and compartment UUIDs are excluded). A group
  collapses to ONE shipped file -- the lexicographically-smallest filename/id
  -- ONLY IF every member shares the identical ``template.name`` AND the
  identical FUNCTIONAL graph content: all node fields and edges except the
  per-instance artifacts (node instance keys, ``jipipe:graph-compartment``
  UUIDs) and the cosmetic node display labels (``jipipe:node:name`` -- a
  user-chosen label, not execution-relevant configuration). A group that
  differs in name or in functional content keeps ALL of its members (the
  ``fan-in-*``/``fan-out-*`` pair is the only such case in the shipped
  corpus: an identical example subgraph but distinct pattern identities, so
  dropping one would lose a named pattern).

  Content-identity guard: the signature only covers node ids + edges, so two
  same-named members could in principle still differ in other functional
  content (parameters, wiring, structure). Collapsing such a group would
  drop that content silently, so any functional-content difference keeps
  ALL members instead of collapsing. The guard is a LIVE condition of the
  collapse (not a post-hoc assertion that can never fire): on any future
  corpus re-run it fails closed (keep all) rather than shipping a
  silently-wrong set. The shipped corpus exercises the guard: the
  ``path-filter`` group differs ONLY in a node display label, so it
  collapses but is reported as a label-only collapse.

Function rule (R8c revised -- each distinct node id counts ONCE):
  For each pattern, its ``taxonomyFunction`` id = the most common TOP-LEVEL
  ancestor (walk the function's ``parent`` chain to the root) of each node's
  primary function (``functions[0]`` in ``registry.json``), ties broken by the
  lexicographically-first id.
  The node set is the pattern's COMPLETE structured node list from
  ``patterns.json`` -- ``patterns[id]["nodes"]`` (a list of
  ``{"role": …, "nodeIds": […]}``) -- taking the SET of all nodeIds (roles may
  repeat an id; each id is counted ONCE).
  IMPORTANT: do NOT derive the node set from the markdown "## Role sequence"
  table -- 13 of the 185 tables are TRUNCATED with an ellipsis, and for 6
  shipped patterns the truncation flips the most-common-branch winner. The
  structured ``patterns.json`` list is complete.
  Validation (the rule is authoritative; these guards pin it):
   * the full distribution over the 185 template candidates MUST be exactly
     ``data 47, annotations 36, images 31, graph 31, rois 25, machine-learning
     9, export 4, tables 2`` (sum 185); any deviation FAILS the generator;
   * cross-check (a warning, never fatal): the computed primary function's NAME
     appears SOMEWHERE in the pattern's frontmatter ``tags`` block (spec §11:
     "The pattern docs' `tags` block is the source data"). Any mismatch is
     logged and counted; expect 0. (This check is deliberately NOT the first
     tags entry -- tags[0] is prose ordering and disagrees with the rule on
     88/185; the tags block lists ALL involved branches, so a wrong winner can
     still satisfy it, which is why the distribution assertion above is the
     real guard.)

Output (all under ``jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/``):
  * the 145 deduplicated graph JSON files (copied under their kept filenames);
  * ``system-templates.json`` -- a JSON object mapping pattern id ->
    ``{"name", "description", "functionId", "file"}``.

  ``name``        = the ``.md`` frontmatter ``template.name``.
  ``description`` = an HTMLText-encodable string built from the frontmatter
                    ``corpus`` block:
                      Mined from the HKI corpus -- support N across M projects.<br>
                      Sequence: A -> B -> C.<br>
                      Wiring: A.slot -> B.slot; ...
                    (the node-name sequence from ``corpus.exampleGraph.nodes``
                    and, per R8, the edge slot wiring; one ``<br>`` per line,
                    HTML-escaped).
  ``functionId``  = the R8c-computed top-level function id.
  ``file``        = the shipped graph JSON filename.

  Icon/hue are intentionally NOT stored here: they are derived from the LIVE
  function registry at registration time (see ``TaxonomyPlugin``).
"""

from __future__ import annotations

import html
import json
import os
import shutil
import sys
from collections import Counter
from datetime import datetime, timezone
from html import unescape

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
PATTERNS_DIR = os.path.join(RESEARCH, "patterns")
TEMPLATES_DIR = os.path.join(PATTERNS_DIR, "templates")
PATTERNS_JSON = os.path.join(PATTERNS_DIR, "patterns.json")
REGISTRY = os.path.join(RESEARCH, "registry.json")
OUT_DIR = os.path.join(
    REPO_ROOT,
    "jipipe-core", "src", "main", "resources",
    "org", "hkijena", "jipipe", "plugins", "nodetemplate", "patterns",
)
SIDECAR = os.path.join(OUT_DIR, "system-templates.json")


def fail(msg: str) -> None:
    print(f"GENERATOR FAILED: {msg}", file=sys.stderr)
    raise SystemExit(1)


# --------------------------------------------------------------------------- #
# Loading
# --------------------------------------------------------------------------- #

def load_registry():
    with open(REGISTRY, encoding="utf-8") as fh:
        reg = json.load(fh)
    funcs = {f["id"]: f for f in reg["functions"]}
    nodes = {n["id"]: n for n in reg["nodes"]}
    return funcs, nodes


def load_patterns():
    """Index ``patterns.json`` by pattern id. The value is the pattern's COMPLETE
    deduped set of node ids (the union of every ``nodeIds`` list; roles may repeat
    an id, so each id counts once). This is the function rule's node source -- the
    structured data, NOT the (possibly truncated) markdown role table."""
    with open(PATTERNS_JSON, encoding="utf-8") as fh:
        data = json.load(fh)
    result = {}
    for entry in data["patterns"]:
        ids = set()
        for group in entry["nodes"]:
            ids.update(group["nodeIds"])
        result[entry["id"]] = ids
    return result


def top_ancestor(func_id, funcs):
    """Walk a function's parent chain to its top-level ancestor id."""
    f = funcs.get(func_id)
    while f is not None and f.get("parent"):
        f = funcs.get(f["parent"])
        if f is None:
            return None
    return f["id"] if f is not None else None


def frontmatter(text: str) -> str:
    import re
    m = re.match(r"^---\n(.*?)\n---", text, re.S)
    return m.group(1) if m else ""


def template_block_name(fm: str) -> str:
    """The ``template:`` block's ``name:`` field (the pattern's display name)."""
    idx = fm.find("template:")
    if idx < 0:
        return ""
    for line in fm[idx + len("template:"):].splitlines():
        s = line.strip()
        if s.startswith("name:"):
            return s.split("name:", 1)[1].strip().strip('"').strip("'")
    return ""


def corpus_block(fm: str) -> dict:
    """Parse the ``corpus:`` frontmatter block (support, projects, exampleGraph).

    The block is the run of whitespace-indented lines immediately after the
    column-0 ``corpus:`` key, ending at the next column-0 key (``template:``).
    """
    import re
    lines = fm.splitlines()
    block = []
    in_corpus = False
    for ln in lines:
        if in_corpus and not ln.startswith(" "):
            break  # next column-0 key (e.g. "template:")
        if ln.startswith("corpus:"):
            in_corpus = True
            continue
        if in_corpus:
            block.append(ln)
    text = "\n".join(block)

    out = {}
    sm = re.search(r"support:\s*(\d+)", text)
    if sm:
        out["support"] = int(sm.group(1))
    pm = re.search(r"projects:\s*\n((?:\s*-\s*\"[^\"]*\"\s*\n?)+)", text)
    out["projects"] = re.findall(r'-\s*"([^"]*)"', pm.group(1)) if pm else []
    cm = re.search(r"projectCount:\s*(\d+)", text)
    out["projectCount"] = int(cm.group(1)) if cm else len(out["projects"])
    # exampleGraph.nodes -> ordered [(id, name)]
    nsm = re.search(r"nodes:\s*\n((?:\s*-\s*\"[^\"]+\":\s*\"[^\"]*\"\s*\n?)+)", text)
    out["exampleGraphNodes"] = re.findall(r'-\s*"([^"]+)":\s*"([^"]*)"', nsm.group(1)) if nsm else []
    return out


def tags(fm: str):
    import re
    m = re.search(r"^tags:\s*\[(.*?)\]", fm, re.M)
    return [t.strip() for t in m.group(1).split(",")] if m else []


def graph_signature(g: dict):
    """(sorted node-info ids, sorted (srcInfoId,srcSlot,tgtInfoId,tgtSlot) edges)."""
    im = {k: v["jipipe:node-info-id"] for k, v in g["nodes"].items()}
    ids = tuple(sorted(v["jipipe:node-info-id"] for v in g["nodes"].values()))
    edges = tuple(sorted((im[e["source-node"]], e["source-slot"],
                          im[e["target-node"]], e["target-slot"])
                         for e in g["edges"]))
    return ids, edges


# Instance-identity fields excluded from content identity: the instance node
# keys and the compartment UUIDs are per-project artifacts (already excluded
# from the signature), and jipipe:node:name is a cosmetic user label, not
# functional configuration (execution uses node type + parameters).
_NON_FUNCTIONAL_NODE_FIELDS = ("jipipe:graph-compartment", "jipipe:node:name")


def graph_content(g: dict):
    """Canonical FUNCTIONAL content of a pattern graph: every node field
    except the non-functional instance-identity fields above, plus the
    normalized edges. Members of a dedup group that differ only in those
    fields are the same pattern mined from different corpus projects and
    collapse safely; any other difference (node type, parameters, structure,
    wiring) keeps ALL members."""
    nodes = []
    for v in g["nodes"].values():
        body = {f: val for f, val in v.items() if f not in _NON_FUNCTIONAL_NODE_FIELDS}
        nodes.append((v["jipipe:node-info-id"],
                      json.dumps(body, sort_keys=True, ensure_ascii=False)))
    nodes.sort(key=lambda t: t[0])
    im = {k: v["jipipe:node-info-id"] for k, v in g["nodes"].items()}
    edges = sorted((im[e["source-node"]], e["source-slot"],
                    im[e["target-node"]], e["target-slot"]) for e in g["edges"])
    return json.dumps([nodes, edges], sort_keys=True, ensure_ascii=False)


def node_labels(g: dict):
    """The set of node display labels (for reporting label-only collapses)."""
    return frozenset(v.get("jipipe:node:name") for v in g["nodes"].values())


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #

def main() -> None:
    funcs, nodes = load_registry()
    pattern_nodes = load_patterns()

    md_files = sorted(f for f in os.listdir(PATTERNS_DIR) if f.endswith(".md"))
    if len(md_files) != 185:
        fail(f"expected 185 pattern .md files, found {len(md_files)}")
    tpl_files = sorted(f for f in os.listdir(TEMPLATES_DIR) if f.endswith(".json"))
    if len(tpl_files) != 185:
        fail(f"expected 185 template .json files, found {len(tpl_files)}")
    # every template file must have a matching md and vice versa
    if {os.path.splitext(f)[0] for f in tpl_files} != {os.path.splitext(f)[0] for f in md_files}:
        fail("template .json and pattern .md files are not 1:1 by stem")

    graphs = {}
    meta = {}
    content = {}   # stem -> normalized JSON (full content identity for dedup)
    for f in tpl_files:
        stem = os.path.splitext(f)[0]
        with open(os.path.join(TEMPLATES_DIR, f), encoding="utf-8") as fh:
            g = json.load(fh)
        if "nodes" not in g or "edges" not in g:
            fail(f"{f} is not JIPipeGraph-shaped (missing nodes/edges)")
        graphs[stem] = g
        content[stem] = graph_content(g)
        with open(os.path.join(PATTERNS_DIR, stem + ".md"), encoding="utf-8") as fh:
            text = fh.read()
        fm = frontmatter(text)
        name = template_block_name(fm)
        if not name:
            fail(f"{stem}.md has no template.name")
        meta[stem] = {
            "text": text,
            "fm": fm,
            "name": name,
            "tags": tags(fm),
            "corpus": corpus_block(fm),
        }

    # ---- function rule over ALL 185 (each distinct node id counted once) ----
    # Node set = the pattern's COMPLETE structured node list (patterns.json),
    # NOT the markdown role table (13/185 are truncated).
    chosen = {}          # stem -> chosen top-level function id
    func_names = {fid: funcs[fid]["name"] for fid in funcs}
    tag_mismatches = []
    unresolved = 0
    for stem in sorted(graphs):
        node_ids = pattern_nodes.get(stem)
        if not node_ids:
            fail(f"{stem}: not present in patterns.json (no complete node list)")
        per = Counter()
        for nid in node_ids:
            node = nodes.get(nid)
            tf = node.get("functions") if node else None
            t = top_ancestor(tf[0], funcs) if tf else None
            if t is None:
                unresolved += 1
            else:
                per[t] += 1
        if not per:
            fail(f"{stem}: no resolvable node function in its complete node set "
                 f"{sorted(node_ids)}")
        mx = max(per.values())
        winner = sorted(t for t, c in per.items() if c == mx)[0]
        chosen[stem] = winner
        # tags cross-check (warning only; tags list ALL involved branches, so this
        # does NOT pin the winner -- the distribution assertion below does)
        if func_names[winner] not in meta[stem]["tags"]:
            tag_mismatches.append((stem, func_names[winner], meta[stem]["tags"]))

    dist = Counter(chosen.values())

    # Distribution MUST be exactly the 8-branch target (sum 185). This is the
    # authoritative guard for the node-set source: a truncated/role-based set
    # produces 9 branches (with a stray filaments) and fails here.
    EXPECTED_DIST = {
        "jipipe:function:data": 47,
        "jipipe:function:annotations": 36,
        "jipipe:function:images": 31,
        "jipipe:function:graph": 31,
        "jipipe:function:rois": 25,
        "jipipe:function:machine-learning": 9,
        "jipipe:function:export": 4,
        "jipipe:function:tables": 2,
    }
    if dict(dist) != EXPECTED_DIST:
        fail("function-rule distribution differs from the expected 8-branch target: "
             f"got {dict(dist.most_common())}; expected "
             f"{ {k.replace('jipipe:function:',''): v for k, v in EXPECTED_DIST.items()} }")

    # ---- dedup (R8 revised) ----
    groups = {}
    for stem in sorted(graphs):
        groups.setdefault(graph_signature(graphs[stem]), []).append(stem)

    kept = {}   # stem -> record
    collapsed = 0
    kept_all_groups = []
    label_only_collapses = []
    for sig, members in groups.items():
        if len(members) == 1:
            kept[members[0]] = None
            continue
        names = {meta[m]["name"] for m in members}
        contents = {content[m] for m in members}
        if len(names) == 1 and len(contents) == 1:
            # collapse: keep lex-smallest. BOTH conditions are live safety
            # guards: the signature only covers node ids + edges, so two
            # same-named members could still differ in other functional
            # content (parameters, wiring, structure). Collapsing a group
            # that differs in that would drop the content silently, so any
            # such difference keeps ALL members instead.
            kept[min(members)] = None
            collapsed += 1
            # Informational: node display labels are a cosmetic (non-functional)
            # exclusion, so label-only differences still collapse; report them
            # for transparency of what "identical" meant.
            if len({node_labels(graphs[m]) for m in members}) > 1:
                label_only_collapses.append(members)
        else:
            kept_all_groups.append(members)
            for m in members:
                kept[m] = None
    kept_stems = sorted(kept)
    dropped = 185 - len(kept_stems)

    # ---- build sidecar ----
    sidecar = {}
    for stem in kept_stems:
        g = graphs[stem]
        m = meta[stem]
        c = m["corpus"]
        support = c.get("support", 0)
        projects = c.get("projectCount", 0)
        seq = m["corpus"].get("exampleGraphNodes", [])
        seq_names = [n for _, n in seq]
        lines = [
            f"Mined from the HKI corpus — support {support} across {projects} projects.",
        ]
        if seq_names:
            lines.append("Sequence: " + " → ".join(unescape(n) for n in seq_names) + ".")
        # wiring from the raw graph edges, ordered source -> target
        im = {k: v["jipipe:node-info-id"] for k, v in g["nodes"].items()}
        name_of = {v["jipipe:node-info-id"]: v["jipipe:node:name"] for v in g["nodes"].values()}
        edges = sorted(
            ((im[e["source-node"]], e["source-slot"], im[e["target-node"]], e["target-slot"])
             for e in g["edges"])
        )
        if edges:
            wire = "; ".join(f"{name_of[s]}.{ss} → {name_of[t]}.{ts}" for s, ss, t, ts in edges)
            lines.append("Wiring: " + wire + ".")
        desc = "<br>".join(html.escape(l, quote=False) for l in lines)

        sidecar[stem] = {
            "file": stem + ".json",
            "functionId": chosen[stem],
            "name": m["name"],
            "description": desc,
        }

    # ---- write outputs ----
    os.makedirs(OUT_DIR, exist_ok=True)
    for stem in kept_stems:
        src = os.path.join(TEMPLATES_DIR, stem + ".json")
        dst = os.path.join(OUT_DIR, stem + ".json")
        shutil.copyfile(src, dst)  # cp (verbatim); research dir untouched

    with open(SIDECAR, "w", encoding="utf-8") as fh:
        json.dump(sidecar, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")

    # ---- summary ----
    print("=== System template pattern generation ===")
    print(f"research dir : {RESEARCH} (read-only)")
    print(f"output dir   : {OUT_DIR}")
    print(f"total patterns   : 185")
    print(f"shipped (kept)   : {len(kept_stems)}")
    print(f"collapsed groups : {collapsed} (name + functional-content identity: PASS)")
    print(f"dropped files    : {dropped}")
    print(f"kept-all groups  : {len(kept_all_groups)}")
    for members in kept_all_groups:
        print(f"   kept-all (name or functional content differs): {members}")
    if label_only_collapses:
        print(f"label-only collapses (cosmetic jipipe:node:name differences, informational):")
        for members in label_only_collapses:
            print(f"   {members}")
    print(f"unresolved nodes : {unresolved}")
    print(f"tags cross-check : {len(tag_mismatches)} mismatch(es) (expect 0)")
    for stem, fn, tg in tag_mismatches[:10]:
        print(f"   MISMATCH {stem}: primary '{fn}' not in tags {tg}")
    print("function distribution (185, distinct node ids):")
    for fid, cnt in dist.most_common():
        print(f"   {fid}: {cnt}")
    print(f"sidecar entries  : {len(sidecar)}  ->  {SIDECAR}")
    print("OK")


if __name__ == "__main__":
    main()
