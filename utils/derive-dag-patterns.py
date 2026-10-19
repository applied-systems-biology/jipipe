#!/usr/bin/env python3
"""Step C′ — pattern derivation: generate the sibling patterns the corpus
under-covers (the (C) review's "what next" gap), grounded in the real node
catalog, and gate them through the same naming rules as the mined set.

Two generators:

1. **Mechanical 2D<->3D twin swap** — a shipped pattern whose dimensioned
   nodes are ALL in one dimension and each have a verified twin node is
   swappable into the other dimension. A twin pair is verified by
   ``is_twin_pair``: the two nodes' slot DATA-TYPE signatures are exact
   dimension-swapped mirrors (``imagej-roi`` <-> ``roi-3d-list``,
   ``*-2d`` <-> ``*-3d``) and their names normalize to the same concept.
   Never by id guessing. Neutral (no-dimension) nodes pass through.

2. **LLM "what next" sweep** — for each shipped pattern the model gets the
   pattern (name + description + nodes with their audited descriptions) and a
   capability catalog of nodes that could follow it, each with exact slot
   names + data types (extracted from the ``@AddJIPipeInputSlot`` /
   ``@AddJIPipeOutputSlot`` annotations in source). It proposes up to 2 new
   patterns; ``validate_proposal`` is fail-closed: every node id must exist,
   every edge must name real slots on both ends, types must be assignable
   (equal or specialization), the result must be a DAG of <=10 nodes.

Survivors are gated by a (C)-style judge prompt (no support heuristic, same
four naming rules) and deduped structurally against the mined set and each
other. Output: ``dag-derived.json`` in the same entry shape as
``dag-judgments.json`` so (D) merges shipped entries from both files.
Provenance (``derivedFrom``, ``mechanism``) is recorded in the artifact only —
per the review decision, derived patterns ship as normal patterns.

Usage (from the repository root):

    python3 utils/derive-dag-patterns.py selftest
    python3 utils/derive-dag-patterns.py run [--limit N]
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import re
import sys
from collections import Counter

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
JUDGE_SCRIPT = os.path.join(REPO_ROOT, "utils", "judge-dag-patterns.py")
CACHE = os.path.join(RESEARCH, "dag-llm-cache.json")
OUT = os.path.join(RESEARCH, "dag-derived.json")

MAX_NODES = 10
MAX_PROPOSALS_PER_PATTERN = 2

# --------------------------------------------------------------------------- #
# Slot extraction (from the @AddJIPipe(Input|Output)Slot annotations in the
# algorithm-class source; multi-line annotation values are joined)
# --------------------------------------------------------------------------- #

_SLOT = re.compile(
    r"@AddJIPipe(Input|Output)Slot\s*\(\s*(.*?)\)\s*(?=@|\n\s*(?:public|class|private))",
    re.S)


def _attr(attrs: str, key: str):
    m = re.search(key + r'\s*=\s*"([^"]+)"', attrs)
    return m.group(1) if m else None


def _attr_class(attrs: str):
    m = re.search(r"value\s*=\s*([A-Za-z0-9_.]+)\.class", attrs)
    return m.group(1).rsplit(".", 1)[-1] if m else None


def extract_slots(source: str) -> dict:
    """{(slot name, class simple name)} per direction, in declaration order."""
    out = {"inputs": [], "outputs": []}
    for m in _SLOT.finditer(source):
        direction = "inputs" if m.group(1) == "Input" else "outputs"
        name = _attr(m.group(2), "name")
        cls = _attr_class(m.group(2))
        if name and cls:
            out[direction].append((name, cls))
    return out


# --------------------------------------------------------------------------- #
# 2D<->3D twins (verified by slot-signature mirror + name concept)
# --------------------------------------------------------------------------- #

def swap_datatype(dt: str) -> str:
    if dt == "imagej-roi":
        return "roi-3d-list"
    if dt == "roi-3d-list":
        return "imagej-roi"
    if "-2d" in dt:
        return dt.replace("-2d", "-3d")
    if "-3d" in dt:
        return dt.replace("-3d", "-2d")
    return dt


def _dt_sequence(node: dict, direction: str) -> list:
    dts = []
    for dt, names in node.get(direction, {}).items():
        dts.extend([dt] * len(names))
    return sorted(dts)


def node_dimension(node: dict) -> str:
    """'2D' / '3D' / None from the node's data types, id and name."""
    dts = (_dt_sequence(node, "inputs") + _dt_sequence(node, "outputs"))
    has2 = any(dt == "imagej-roi" or "-2d" in dt for dt in dts)
    has3 = any(dt == "roi-3d-list" or "-3d" in dt for dt in dts)
    if has2 and has3:
        return "MIXED"
    nid = node.get("id", "")
    name = (node.get("name") or "").lower()
    if has3 or nid.startswith("ij3d-") or "3d" in name:
        return "3D"
    if has2 or "2d" in name:
        return "2D"
    if nid.startswith("ij1-") and "roi" in nid:
        return "2D"
    return None


def _name_concept(name: str) -> str:
    """Dimension-agnostic concept key of a node name (the ij1/ij3d ROI pairs
    differ only in the dimension token — strip "ij1"/"ij3d" and bare "2d"/
    "3d" tokens before digit removal so both sides normalize identically)."""
    s = (name or "").lower()
    s = s.replace("ij3d", "").replace("ij1", "")
    s = re.sub(r"2d|3d", "", s)
    s = re.sub(r"[0-9]", "", s)
    s = re.sub(r"[^a-z]", "", s)
    return s


def is_twin_pair(a: dict, b: dict) -> bool:
    """a and b are verified 2D<->3D twins: mirrored data-type signatures and
    the same name concept."""
    if {node_dimension(a), node_dimension(b)} != {"2D", "3D"}:
        return False
    if _dt_sequence(a, "inputs") != [swap_datatype(d) for d in
                                     _dt_sequence(b, "inputs")]:
        return False
    if _dt_sequence(a, "outputs") != [swap_datatype(d) for d in
                                      _dt_sequence(b, "outputs")]:
        return False
    return _name_concept(a.get("name")) == _name_concept(b.get("name"))


def build_twin_map(catalog: list) -> dict:
    """node id -> {"2D": partner_id, "3D": partner_id} (verified pairs only).
    Pre-groups by name concept so only same-concept pairs are compared
    (the O(n²) full scan over ~1500 nodes is not practical)."""
    out = {}
    by = {c["id"]: c for c in catalog}
    by_concept = {}
    for c in catalog:
        by_concept.setdefault(_name_concept(c.get("name")), []).append(c)
    for grp in by_concept.values():
        if len(grp) < 2:
            continue
        for i in range(len(grp)):
            for j in range(i + 1, len(grp)):
                a, b = grp[i], grp[j]
                if is_twin_pair(a, b):
                    da = node_dimension(a)
                    db = node_dimension(b)
                    out.setdefault(a["id"], {})[db] = b["id"]
                    out.setdefault(b["id"], {})[da] = a["id"]
    return out


def mechanical_swap(graph: dict, catalog: list, direction: str,
                    twins: dict = None) -> dict:
    """Swap a pure-2D (or pure-3D) pattern to ``direction`` via verified
    twins. Returns the swapped graph, or None if the pattern is not purely
    one dimension or a dimensioned node has no twin. ``twins`` is a prebuilt
    ``build_twin_map(catalog)`` (pass it to avoid the O(n²) rebuild per
    pattern)."""
    source_dim = "2D" if direction == "3D" else "3D"
    by = {c["id"]: c for c in catalog}
    if twins is None:
        twins = build_twin_map(catalog)
    node_ids = [v["jipipe:node-info-id"] for v in graph["nodes"].values()]
    dims = [node_dimension(by[nid]) if nid in by else None for nid in node_ids]
    if not any(d == source_dim for d in dims):
        return None  # not a pattern of the source dimension
    if any(d not in (None, source_dim) for d in dims):
        return None  # mixed / foreign dimension
    # slot name lookup per (node id, data type), SEPARATE by direction — a
    # source endpoint must resolve to the twin's OUTPUT slot for the swapped
    # data type and a target to its INPUT slot. Merging both directions
    # (inputs first, setdefault) made a source resolve to the twin's input
    # name whenever the twin has an input and an output of the same dimension
    # type with different names (the real ROI-statistics/morph/scale
    # families: input "ROI", output "Output") — 10 broken edges in the first
    # full run.
    slot_by_dt_out = {}
    slot_by_dt_in = {}
    for c in catalog:
        for dt, names in c.get("outputs", {}).items():
            slot_by_dt_out.setdefault((c["id"], dt), names)
        for dt, names in c.get("inputs", {}).items():
            slot_by_dt_in.setdefault((c["id"], dt), names)
    new = {"nodes": {}, "edges": []}
    dim_by_key = {k: d for k, d in
                  zip(graph["nodes"].keys(), dims)}
    for k, v in graph["nodes"].items():
        nid = v["jipipe:node-info-id"]
        if dim_by_key[k] is None:
            new["nodes"][k] = dict(v)
            continue
        twin = twins.get(nid, {}).get(direction)
        if twin is None:
            return None
        new["nodes"][k] = dict(v, **{"jipipe:node-info-id": twin})
        by_twin = by.get(twin)
        if by_twin:
            new["nodes"][k]["jipipe:node:name"] = by_twin.get(
                "jipipe:node:name") or by_twin.get("name")
    for e in graph["edges"]:
        s, t = e["source-node"], e["target-node"]
        s_id, t_id = graph["nodes"][s]["jipipe:node-info-id"], \
            graph["nodes"][t]["jipipe:node-info-id"]
        s_new, t_new = new["nodes"][s]["jipipe:node-info-id"], \
            new["nodes"][t]["jipipe:node-info-id"]
        s_slot = e.get("source-slot", "")
        t_slot = e.get("target-slot", "")
        s_dt = None
        for dt, names in by.get(s_id, {}).get("outputs", {}).items():
            if s_slot in names:
                s_dt = dt
                break
        t_dt = None
        for dt, names in by.get(t_id, {}).get("inputs", {}).items():
            if t_slot in names:
                t_dt = dt
                break
        if s_dt:
            names = slot_by_dt_out.get((s_new, swap_datatype(s_dt)), [])
            if names:
                s_slot = names[0]
        if t_dt:
            names = slot_by_dt_in.get((t_new, swap_datatype(t_dt)), [])
            if names:
                t_slot = names[0]
        # (A) format edge keys are KEBAB-case (source-slot/target-slot) —
        # the mined graphs and the (D) deserialization read those. Writing
        # the remap to the snake_case keys silently kept the stale (2D) slot
        # names in the shipped edge (verified: import-roi-3d "Files").
        new_edge = dict(e)
        if "source-slot" in new_edge:
            new_edge["source-slot"] = s_slot
        if "target-slot" in new_edge:
            new_edge["target-slot"] = t_slot
        new["edges"].append(new_edge)
    # fail-closed post-check: every slot name on a SWAPPED node endpoint must
    # exist on the twin in the swapped graph. The remap only ever touches
    # swapped endpoints (neutral endpoints carry dimension-agnostic data
    # types, so the swap is the identity and the mined names are preserved
    # verbatim — including on dynamic plot-table:* nodes whose real slot
    # names are not source-verifiable, exactly as in the shipped source
    # patterns). A lookup miss on a swapped endpoint would otherwise ship a
    # stale name (verified: import-roi-3d "Files", stat nodes "ROI").
    swapped_keys = {k for k, d in dim_by_key.items() if d is not None}
    for e in new["edges"]:
        for end, slotkey, dkey in (("source-node", "source-slot", "outputs"),
                                   ("target-node", "target-slot", "inputs")):
            if e[end] not in swapped_keys:
                continue
            c = by.get(new["nodes"][e[end]]["jipipe:node-info-id"])
            if c is None:
                return None
            valid = {n for l in c.get(dkey, {}).values() for n in l}
            if e.get(slotkey) not in valid:
                return None
    return new


# --------------------------------------------------------------------------- #
# Fail-closed validation of LLM-proposed chains
# --------------------------------------------------------------------------- #

def _slot_dt(node: dict, direction: str, slot_name: str):
    for dt, names in node.get(direction, {}).items():
        if slot_name in names:
            return dt
    return None


def _assignable(src_dt: str, tgt_dt: str) -> bool:
    """A slot of data type src_dt can feed a slot expecting tgt_dt when they
    are equal or src_dt is a specialization of tgt_dt (imagej-imgplus-2d
    feeds an imagej-imgplus input)."""
    return src_dt == tgt_dt or src_dt.startswith(tgt_dt + "-")


def validate_proposal(proposal: dict, catalog: dict) -> str:
    """None if the proposed chain is structurally sound, else the reason.
    ``catalog``: node id -> node record {id, name, inputs:{dt:[names]},
    outputs:{dt:[names]}}."""
    nodes = proposal.get("nodes") or []
    if not nodes:
        return "empty"
    if len(nodes) > MAX_NODES:
        return f"too many nodes ({len(nodes)} > {MAX_NODES})"
    ids = [n["id"] for n in nodes]
    if len(set(ids)) != len(ids):
        return "duplicate node ids"
    for nid in ids:
        if nid not in catalog:
            return f"unknown node id {nid}"
    indeg = Counter()
    for e in proposal.get("edges", []):
        s, t = e["source"], e["target"]
        if s not in ids or t not in ids:
            return f"edge endpoint not in nodes ({s} -> {t})"
        s_dt = _slot_dt(catalog[s], "outputs", e["source_slot"])
        if s_dt is None:
            return f"{s} has no output slot named {e['source_slot']}"
        t_dt = _slot_dt(catalog[t], "inputs", e["target_slot"])
        if t_dt is None:
            return f"{t} has no input slot named {e['target_slot']}"
        if not _assignable(s_dt, t_dt):
            return f"type mismatch {s_dt} !-> {t_dt} on {s} -> {t}"
        indeg[t] += 1
    # cycle check (Kahn)
    remaining = set(ids)
    progress = True
    while remaining and progress:
        progress = False
        for nid in list(remaining):
            if indeg[nid] == 0:
                remaining.discard(nid)
                progress = True
                for e in proposal.get("edges", []):
                    if e["source"] == nid:
                        indeg[e["target"]] -= 1
    if remaining:
        return "cycle in the proposed chain"
    return None


# --------------------------------------------------------------------------- #
# Structural dedup (node-id multiset + edge multiset, instance-label-keyed)
# --------------------------------------------------------------------------- #

def structural_signature(graph: dict) -> tuple:
    """(node-id multiset, canonical edge multiset) — LABEL-AGNOSTIC: node
    instance labels are canonicalized by BFS from the sources (ties broken by
    node-info-id), so a derived pattern that is structurally the same as a
    mined one (or another derived one) collapses to the same signature even
    though the labels differ."""
    nodes = graph["nodes"]
    edges = graph.get("edges", [])
    indeg = {k: 0 for k in nodes}
    adj = {k: [] for k in nodes}
    for e in edges:
        s, t = e["source-node"], e["target-node"]
        if s in indeg and t in indeg:
            indeg[t] += 1
            adj[s].append(t)
    order = sorted([k for k in nodes if indeg[k] == 0],
                   key=lambda k: (nodes[k]["jipipe:node-info-id"], k))
    canon = {}
    for idx, k in enumerate(order):
        canon[k] = idx
    head = 0
    while head < len(order):
        k = order[head]
        head += 1
        for t in sorted(adj[k], key=lambda x: (nodes[x]["jipipe:node-info-id"], x)):
            if t not in canon:
                canon[t] = len(order)
                order.append(t)
    for k in nodes:
        canon.setdefault(k, len(order))
    node_ids = tuple(sorted(v["jipipe:node-info-id"] for v in nodes.values()))
    edge_t = []
    for e in edges:
        s, t = e["source-node"], e["target-node"]
        if s in canon and t in canon:
            edge_t.append((canon[s], e.get("source-slot", ""),
                           canon[t], e.get("target-slot", "")))
    return (node_ids, tuple(sorted(edge_t)))


# --------------------------------------------------------------------------- #
# Catalog assembly (registry data types + source slot names + branch)
# --------------------------------------------------------------------------- #

# the (C) judge's function branches == the 13 top-level function tree nodes
# (their short ids); a leaf's branch is found by walking the function tree's
# parent chain (registry.json "functions").
_TOP_FUNC_SHORT = {"annotations", "data", "export", "filaments", "graph",
                   "images", "machine-learning", "misc", "plotting", "rois",
                   "statistics", "tables", "tracking"}


def build_branch_index(functions: list) -> dict:
    """function id -> top-level (branch) function id, via the parent chain."""
    by_id = {f["id"]: f for f in functions}
    out = {}
    for fid in by_id:
        cur = by_id[fid]
        seen = {fid}
        while cur.get("parent") and cur["parent"] in by_id and cur["parent"] not in seen:
            seen.add(cur["id"])
            cur = by_id[cur["parent"]]
        top = cur["id"].replace("jipipe:function:", "")
        out[fid] = top if top in _TOP_FUNC_SHORT else "misc"
    return out


def branch_of_function(function_id: str, branch_index: dict = None) -> str:
    """The (C) branch key for a registry function id (parent-chain lookup;
    a flat short id maps to itself when it is a top-level branch)."""
    if branch_index is not None and function_id in branch_index:
        return branch_index[function_id]
    short = (function_id or "").replace("jipipe:function:", "")
    return short if short in _TOP_FUNC_SHORT else "misc"


def build_catalog(registry: list, source_slots: dict, functions: list = None) -> list:
    """Join registry.json nodes (slot data types) with slot names + a
    function branch. Slot names come from two trusted sources, in order:
    (1) the source ``@AddJIPipe*Slot`` annotations (``source_slots``: node id
    -> {inputs: [(name, class)], outputs: [...]}) — authoritative for
    statically-registered nodes; (2) the registry dump's per-slot names
    (``inputSlots``/``outputSlots`` with a ``name`` field) — authoritative
    for DYNAMICALLY-registered nodes (cLij2/clijx kernels), which have no
    source annotation to parse and cannot be dumped from code (the
    2026-10-19 re-sweep). A node with neither falls back to the class simple
    name and is marked slots_verified=False (the orchestrator fail-closes
    patterns that depend on unverified slot names)."""
    branch_index = build_branch_index(functions or [])
    cat = []
    for n in registry:
        def join(direction):
            src = source_slots.get(n["id"], {}).get(direction, [])
            by_dt = {}
            for s in n.get(direction, []):
                dt = s.get("dataType")
                if not dt:
                    continue
                cls_simple = s["class"].rsplit(".", 1)[-1]
                names = [nm for nm, cls in src if cls == cls_simple]
                by_dt.setdefault(dt, names or [cls_simple])
            return by_dt

        def join_registry(direction):
            # Fallback for dynamically-registered nodes: slot names straight
            # from the booted-launcher registry dump (inputSlots/outputSlots).
            regkey = "inputSlots" if direction == "inputs" else "outputSlots"
            name_by_cls = {}
            for s in n.get(regkey, []):
                nm = s.get("name") or ""
                cls_simple = s["class"].rsplit(".", 1)[-1]
                name_by_cls.setdefault(cls_simple, []).append(nm)
            by_dt = {}
            for s in n.get(direction, []):
                dt = s.get("dataType")
                if not dt:
                    continue
                cls_simple = s["class"].rsplit(".", 1)[-1]
                names = [nm for nm in name_by_cls.get(cls_simple, []) if nm]
                by_dt.setdefault(dt, names or [cls_simple])
            return by_dt

        if n["id"] in source_slots:
            # source-verified (static node) — unchanged behavior
            in_map, out_map = join("inputs"), join("outputs")
            verified = True
        else:
            # dynamically-registered (cLij2/clijx) — registry slot names
            in_map, out_map = join_registry("inputs"), join_registry("outputs")
            verified = any((s.get("name") or "")
                           for s in (n.get("inputSlots", [])
                                     + n.get("outputSlots", [])))
        funcs = n.get("functions") or []
        cat.append({
            "id": n["id"],
            "name": n.get("name", n["id"]),
            "function": branch_of_function(funcs[0], branch_index) if funcs else "misc",
            "inputs": in_map,
            "outputs": out_map,
            "slots_verified": verified,
        })
    return cat


# --------------------------------------------------------------------------- #
# Capability catalog for the "what next" sweep
# --------------------------------------------------------------------------- #

def select_catalog(catalog: list, pattern_dts, function: str,
                   cap: int = 50) -> list:
    """The nodes a 'what next' proposer may use: nodes that consume one of
    the pattern's data types (the direct followers) plus same-function nodes
    (the alternatives). Deterministic order: consumers first, then id."""
    def is_consumer(n):
        return any(dt in pattern_dts for dt in n.get("inputs", {}))
    consumers = [n for n in catalog if is_consumer(n)]
    same_func = [n for n in catalog
                 if n.get("function") == function and not is_consumer(n)]
    def key(n):
        return (0 if is_consumer(n) else 1, n["id"])
    sel = consumers + same_func
    sel.sort(key=key)
    return sel[:cap]


def _slots_line(n: dict, direction: str) -> str:
    parts = []
    for dt, names in sorted(n.get(direction, {}).items()):
        parts.append(dt + " (" + ", ".join(names) + ")")
    return ", ".join(parts) if parts else "(none)"


def build_what_next_prompt(state: dict, catalog: list) -> str:
    p = state["pattern"]
    nodes = "\n".join(
        f"- {n['name']}: {n.get('description', '')}" for n in p["nodes"])
    cat_lines = []
    for n in catalog:
        cat_lines.append(f"- {n['id']} — {n['name']}")
        cat_lines.append("    inputs:  " + _slots_line(n, "inputs"))
        cat_lines.append("    outputs: " + _slots_line(n, "outputs"))
    cat = "\n".join(cat_lines)
    return f"""You are extending a library of reusable image-analysis pipeline patterns (JIPipe).

The pattern below is one a user already has:

Pattern: {p['nodeSequence']}
Nodes (what each does):
{nodes}

Given this pattern, what would an image analyst or biologist want to do NEXT
with its output (or with similar data)? Propose up to {MAX_PROPOSALS_PER_PATTERN} NEW
patterns — short, coherent chains of EXISTING nodes from the catalog below
that extend or reuse this one.

Good derivations look like:
- converting the output to another useful form (e.g. a mask or label image to
  filaments, ROIs to filaments, filaments edited and converted back to ROIs,
  a mask to 2D or 3D filaments);
- measuring the result and then ANNOTATING the data with the measurements —
  plus a companion pattern that USES such annotations (filter, plot, split);
- the same concept with a different plot type or a different bit depth where
  the catalog offers it;
- a 2D variant of a 3D pattern or vice versa, when the catalog has the twin.

Catalog (node id — name, with exact input/output data types and slot names):
{cat}

Rules:
- Use ONLY node ids and slot names exactly as listed in the catalog. Never
  invent an id or a slot.
- Each new pattern: 2 to {MAX_NODES} nodes, wired edge-to-edge (an edge is
  source -> target with exact slot names; the source's output data type must
  fit the target's input data type).
- The new pattern must be a coherent, generalizable unit a user would want to
  run — not an arbitrary chain.
- "name": plain outcome, two audiences (bioinformaticians AND biologists), no
  JIPipe internals. If it operates on ROIs, say "2D ROI" or "3D ROI".
- "description": 1-2 sentences, what it takes in / does / produces.

Answer with ONE JSON object only:
{{"proposals": [{{"name": "...", "description": "...",
   "nodes": [{{"id": "<catalog node id>"}}, ...],
   "edges": [{{"source": "<id>", "source_slot": "<slot>",
               "target": "<id>", "target_slot": "<slot>"}}, ...]}}]}}
Empty list if there is nothing sensible to derive.
"""


def parse_proposals(raw) -> list:
    """The proposal list out of the model response (fail-closed: malformed
    shapes are dropped, the list is capped at MAX_PROPOSALS_PER_PATTERN)."""
    if isinstance(raw, str):
        try:
            raw = json.loads(raw)
        except ValueError:
            m = re.search(r"\{.*\}", raw or "", re.S)
            if not m:
                return []
            try:
                raw = json.loads(m.group(0))
            except ValueError:
                return []
    if isinstance(raw, dict):
        raw = raw.get("proposals")
    if not isinstance(raw, list):
        return []
    out = []
    for prop in raw[:MAX_PROPOSALS_PER_PATTERN]:
        if not isinstance(prop, dict):
            continue
        nodes = prop.get("nodes")
        edges = prop.get("edges")
        if (isinstance(prop.get("name"), str) and prop["name"].strip()
                and isinstance(prop.get("description"), str)
                and isinstance(nodes, list) and nodes
                and all(isinstance(n, dict) and isinstance(n.get("id"), str)
                        for n in nodes)
                and isinstance(edges, list)):
            out.append({"name": prop["name"].strip(),
                        "description": prop["description"].strip(),
                        "nodes": nodes, "edges": edges})
    return out


# --------------------------------------------------------------------------- #
# The (C)-style gate prompt for derived patterns
# --------------------------------------------------------------------------- #

def build_derived_gate_prompt(state: dict) -> str:
    p = state["pattern"]
    nodes = "\n".join(
        f"- {n['name']}: {n.get('description', '')}" for n in p["nodes"])
    return f"""You are curating a library of reusable image-analysis pipeline patterns (JIPipe).

The pattern described below was NOT observed in a user project — it was
DERIVED by a model that, thinking like an image analyst using the related
patterns, proposed this chain of real, existing nodes. Judge it on the same
bar as observed patterns.

Pattern (nodes with what each does):
{nodes}

Sequence: {p['nodeSequence']}
Wiring: {p.get('wiring', '')}

Answer with ONE JSON object only:
{{"gate": "general_pattern" | "noise",
 "function": one of ["annotations","data","export","filaments","graph","images","machine-learning","misc","plotting","rois","statistics","tables","tracking"],
 "name": "a short general name of what this pattern does" | "",
 "description": "1-2 sentences describing what this pattern does, in general terms" | ""}}

Rules:
- gate "general_pattern" only if the chain is a coherent, reusable,
  generalizable unit doing one clear thing a user would want to run. A chain
  that merely converts data type A to data type B with no analysis in between
  is fine ONLY if that conversion is itself a common need; arbitrary chains
  are "noise".
- NAMING: two audiences — bioinformaticians AND biologists. Name the OUTCOME
  in plain terms, never JIPipe internals ("annotated path", "slot", "graph",
  "compartment", "hyperstack").
- If the pattern operates on ROIs, the name MUST say "2D ROI" or "3D ROI"
  (2D/3D ROIs are different data types, never a bare "ROI").
- description: what it takes in, what it does, what it produces — plain first,
  technical second. Empty string if gate is "noise".
"""


def dimension_rename(text: str, direction: str) -> str:
    """Swap the 2D/3D tokens of user-facing text (a twin's name/description).
    Text without the token is unchanged."""
    if not text:
        return text
    if direction == "3D":
        return re.sub(r"\b2D\b", "3D", text)
    return re.sub(r"\b3D\b", "2D", text)


def mechanical_twin_entry(source_entry: dict, graph: dict, direction: str,
                          catalog: list, node_descs: dict = None,
                          twins: dict = None) -> dict:
    """The deterministic derived entry for the verified twin of a shipped
    pattern. The LLM gate does NOT judge twins: they are general by
    construction (a structural equivalent of a pattern already judged
    general), and the pilot showed the gate (which sees support 0) rejecting
    16/26 verified twins while shipping others. The name/description inherit
    the source's (already (C)-reviewed, dimension-qualified) text with the
    2D/3D tokens swapped. All node names are the canonical registry display
    names (a derived pattern is assembled fresh — the source pattern's canvas
    labels do not belong in it). Returns None if not swappable."""
    node_descs = node_descs or {}
    if twins is None:
        twins = build_twin_map(catalog)
    swapped = mechanical_swap(graph, catalog, direction, twins)
    if swapped is None:
        return None
    by = {c["id"]: c for c in catalog}
    for v in swapped["nodes"].values():
        c = by.get(v["jipipe:node-info-id"])
        if c:
            v["jipipe:node:name"] = c["name"]
    out_graph = to_pattern_graph(swapped)
    J = _judge_module()
    order = J._bfs_order(swapped)
    name_of = {k: swapped["nodes"][k].get("jipipe:node:name",
                                          swapped["nodes"][k]["jipipe:node-info-id"])
               for k in swapped["nodes"]}
    state = {"pattern": {
        "nodes": [{"id": swapped["nodes"][k]["jipipe:node-info-id"],
                    "name": name_of[k],
                    "description": node_descs.get(
                        swapped["nodes"][k]["jipipe:node-info-id"], "")}
                   for k in order],
        "nodeSequence": " -> ".join(name_of[k] for k in order),
        "wiring": "; ".join(f"{name_of[e['source-node']]} {e.get('source-slot', '')} -> "
                            f"{name_of[e['target-node']]} {e.get('target-slot', '')}"
                            for e in swapped.get("edges", [])),
        "nodeCount": len(swapped["nodes"]),
        "edgeCount": len(swapped.get("edges", [])),
        "support": 0, "signal": "derived", "observedTitles": [],
        "graph": out_graph,
    }}
    return {
        "clusterId": f"derived-mech-{source_entry['clusterId']}-{direction.lower()}",
        "support": 0,
        "ship": bool(source_entry.get("ship")),
        "gate": source_entry.get("gate"),
        "functionId": source_entry.get("functionId"),
        "name": dimension_rename(source_entry.get("name"), direction),
        "description": dimension_rename(source_entry.get("description"), direction),
        "error": None,
        "verdict": None,
        "provenance": {"mechanism": "twin-swap",
                       "derivedFrom": source_entry["clusterId"],
                       "direction": f"{source_entry.get('name')} -> {direction}"},
        "state": state,
    }


# --------------------------------------------------------------------------- #
# Graph assembly (the (A) minimal-graph format, with 3.4 layout locations)
# --------------------------------------------------------------------------- #

def bfs_depth(graph: dict) -> dict:
    """node key -> longest-path depth from the sources (the 3.4 layout rule)."""
    indeg = {k: 0 for k in graph["nodes"]}
    adj = {k: [] for k in graph["nodes"]}
    for e in graph.get("edges", []):
        if e["source-node"] in indeg and e["target-node"] in indeg:
            indeg[e["target-node"]] += 1
            adj[e["source-node"]].append(e["target-node"])
    depth = {}
    queue = [k for k, d in indeg.items() if d == 0]
    for k in queue:
        depth[k] = 0
    while queue:
        k = queue.pop(0)
        for t in adj[k]:
            depth[t] = max(depth.get(t, 0), depth[k] + 1)
            indeg[t] -= 1
            if indeg[t] == 0:
                queue.append(t)
    for k in graph["nodes"]:  # cycles / unreachable: fall back to 0
        depth.setdefault(k, 0)
    return depth


def to_pattern_graph(graph: dict, comp: str = "derived") -> dict:
    """Wrap a {nodes:{k:{jipipe:node-info-id}}, edges:[...]} graph into the
    (A) minimal-graph format every shipped pattern carries: a
    jipipe:graph-compartment per node and a location/<compartment>/x|y pair
    under jipipe:metadata-v1 (plan 3.4 — (D) guards its presence). x is the
    longest-path depth, y the per-depth order, so the layout is
    deterministic."""
    depth = bfs_depth(graph)
    by_depth = {}
    for k in graph["nodes"]:
        by_depth.setdefault(depth[k], []).append(k)
    pos = {}
    for d, keys in by_depth.items():
        for i, k in enumerate(sorted(keys)):
            pos[k] = (d * 250 + 100, i * 150 + 100)
    out = {"nodes": {}, "edges": []}
    for k, v in graph["nodes"].items():
        x, y = pos[k]
        out["nodes"][k] = dict(v)
        out["nodes"][k]["jipipe:graph-compartment"] = comp
        out["nodes"][k]["jipipe:metadata-v1"] = {
            f"location/{comp}/x": x,
            f"location/{comp}/y": y,
        }
    out["edges"] = [dict(e) for e in graph.get("edges", [])]
    return out


# --------------------------------------------------------------------------- #
# The (C)-style judge for derived patterns
# --------------------------------------------------------------------------- #

def judge_derived(cluster: dict, llm, node_descs=None) -> dict:
    """Gate/name/classify one derived pseudo-cluster with the (C) rules
    (no support heuristic). Same sanity caps as (C) (name <= 128,
    description <= 600, fail-closed). Returns a dag-judgments-shaped entry.
    ``node_descs``: node-info-id -> audited description (the gate sees what
    each node does, as in the mined (C) re-runs)."""
    J = _judge_module()
    g = cluster["representativeCandidate"]["graph"]
    node_descs = node_descs or {}
    state = {"pattern": {
        "nodes": [{"id": v["jipipe:node-info-id"],
                   "name": v.get("jipipe:node:name", v["jipipe:node-info-id"]),
                   "description": node_descs.get(v["jipipe:node-info-id"], "")}
                  for v in g["nodes"].values()],
        "nodeSequence": " -> ".join(v.get("jipipe:node:name", "")
                                    for v in g["nodes"].values()),
        "wiring": "; ".join(f"{e['source-node']} {e.get('source-slot', '')} -> "
                            f"{e['target-node']} {e.get('target-slot', '')}"
                            for e in g.get("edges", [])),
        "nodeCount": len(g["nodes"]), "edgeCount": len(g.get("edges", [])),
        "support": 0, "signal": "derived", "observedTitles": [],
    }}
    prompt = build_derived_gate_prompt(state)
    state["pattern"]["graph"] = to_pattern_graph(g)
    verdict = llm(prompt)
    if isinstance(verdict, str):
        verdict = J.parse_verdict(verdict)
    entry = {"clusterId": cluster["clusterId"], "support": 0,
             "ship": False, "gate": None, "functionId": None, "name": None,
             "description": None, "error": None, "provenance": cluster.get("provenance"),
             "state": state}
    if not isinstance(verdict, dict):
        entry["error"] = "malformed_verdict"
        return entry
    gate = verdict.get("gate")
    func = verdict.get("function")
    name = (verdict.get("name") or "").strip()
    desc = (verdict.get("description") or "").strip()
    entry["verdict"] = verdict
    entry["gate"] = gate
    if gate != "general_pattern":
        return entry
    if func not in J.BRANCH_HINTS:
        entry["error"] = "unknown_function"
        return entry
    if not name:
        entry["error"] = "empty_name"
        return entry
    if len(name) > J.MAX_NAME_LEN:
        entry["error"] = "name_too_long"
        return entry
    if len(desc) > J.MAX_DESCRIPTION_LEN:
        entry["error"] = "description_too_long"
        return entry
    entry["functionId"] = f"jipipe:function:{func}"
    entry["name"] = name
    entry["description"] = desc
    entry["ship"] = True
    return entry


def merge_derived_outputs(shards: list, fresh_twins: list,
                          mined_sigs: set = None) -> list:
    """Combine the LLM what-next entries from the shards with a FRESH
    recomputation of the deterministic twins (the in-shard twins may have
    been produced by an older code version — the real incident: the podman
    run predates the kebab-case slot-remap fix, so 2 import-roi-3d twins
    carried the stale 2D slot name). One representative per structural
    signature: a mined match is dropped; otherwise prefer a gate-SHIPPED
    entry over a gate-rejected one, then a twin over a what-next, then
    clusterId. Deterministic output order (clusterId)."""
    mined_sigs = mined_sigs or set()
    entries = list(fresh_twins)
    for shard in shards:
        for e in shard.get("entries", []):
            if (e.get("provenance") or {}).get("mechanism") == "twin-swap":
                continue  # superseded by the fresh recomputation
            entries.append(e)
    groups = {}
    for e in entries:
        sig = structural_signature(e["state"]["pattern"]["graph"])
        if sig in mined_sigs:
            continue  # already a mined pattern
        groups.setdefault(sig, []).append(e)

    def pick(g):
        return sorted(g, key=lambda e: (
            0 if e.get("ship") else 1,
            0 if (e.get("provenance") or {}).get("mechanism") == "twin-swap"
            else 1,
            e["clusterId"]))[0]

    out = [pick(v) for v in groups.values()]
    out.sort(key=lambda e: e["clusterId"])
    return out


def recompute_twins(shipped: list, clusters: dict, catalog: list,
                    node_descs: dict, twins: dict) -> list:
    """The deterministic twin entries for all shipped patterns, both
    directions (pure function — no LLM — so it is safe to re-run at merge
    time with the current code)."""
    out = []
    seen = set()
    for e in shipped:
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        for direction in ("3D", "2D"):
            entry = mechanical_twin_entry(e, g, direction, catalog,
                                          node_descs, twins)
            if entry is None:
                continue
            sig = structural_signature(entry["state"]["pattern"]["graph"])
            if sig in seen:
                continue
            seen.add(sig)
            out.append(entry)
    return out


def merge_shards(shards: list, mined_sigs: set = None) -> list:
    """Merge sharded what-next runs (the full sweep is split across parallel
    jobs). Entries are grouped by structural signature: a signature matching
    a MINED pattern is dropped entirely; otherwise one representative per
    signature is kept, preferring (1) a gate-SHIPPED entry over a
    gate-rejected one of the same structure (the same chain, one naming
    passed the gate), then (2) a deterministic twin over a what-next (the
    twin's name is the dimension-rename of an already (C)-reviewed name),
    then clusterId. Order is deterministic (clusterId)."""
    mined_sigs = mined_sigs or set()
    groups = {}
    for shard in shards:
        for e in shard.get("entries", []):
            sig = structural_signature(e["state"]["pattern"]["graph"])
            if sig in mined_sigs:
                continue  # already a mined pattern
            groups.setdefault(sig, []).append(e)

    def pick(entries):
        return sorted(entries, key=lambda e: (
            0 if e.get("ship") else 1,
            0 if (e.get("provenance") or {}).get("mechanism") == "twin-swap"
            else 1,
            e["clusterId"]))[0]

    out = [pick(v) for v in groups.values()]
    out.sort(key=lambda e: e["clusterId"])
    return out


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #

def _judge_module():
    spec = importlib.util.spec_from_file_location("judge_dag_patterns",
                                                  JUDGE_SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def build_source_slots(repo: str) -> dict:
    """node id -> {inputs: [(slot name, class simple name)], outputs: [...]}.
    One pass over the source tree (the extractor's registration scan + the
    slot annotations off each algorithm class)."""
    spec = importlib.util.spec_from_file_location(
        "extract_node_descriptions",
        os.path.join(REPO_ROOT, "utils", "extract-node-descriptions.py"))
    EX = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(EX)
    id2class = {}
    class2text = {}
    for path in EX.iter_source_files(repo):
        try:
            txt = open(path, encoding="utf-8", errors="ignore").read()
        except OSError:
            continue
        id2class.update(EX.extract_id_class_map(txt))
        cls = EX.top_level_class_name(txt)
        if cls:
            class2text.setdefault(cls, txt)
    out = {}
    for nid, cls in id2class.items():
        if cls in class2text:
            out[nid] = extract_slots(class2text[cls])
    return out


def _pattern_state(e, graph, node_descs):
    """The (C)-style state for a pattern (used by both what-next and the gate)."""
    J = _judge_module()
    order = J._bfs_order(graph) if hasattr(J, "_bfs_order") else list(graph["nodes"])
    name_of = {k: (graph["nodes"][k].get("jipipe:node:name")
                   or graph["nodes"][k]["jipipe:node-info-id"])
               for k in graph["nodes"]}
    descs = node_descs or {}
    nodes = []
    for k in order:
        nid = graph["nodes"][k]["jipipe:node-info-id"]
        nodes.append({"id": nid, "name": name_of[k],
                      "description": descs.get(nid, "")})
    wiring = "; ".join(
        f"{name_of[e['source-node']]} {e.get('source-slot', '')} -> "
        f"{name_of[e['target-node']]} {e.get('target-slot', '')}"
        for e in sorted(graph.get("edges", []),
                        key=lambda e: (e["source-node"], e["target-node"])))
    return {"pattern": {
        "nodes": nodes,
        "nodeSequence": " -> ".join(name_of[k] for k in order),
        "wiring": wiring,
        "nodeCount": len(graph["nodes"]),
        "edgeCount": len(graph.get("edges", [])),
        "support": e["support"], "signal": "mined",
        "observedTitles": [],
    }}


def _proposal_to_graph(proposal, cat_by_id: dict = None):
    """Map an LLM proposal (nodes by id, edges by id) to the {nodes, edges}
    graph shape (node-instance labels n0..n(k-1) in proposal order). Node
    display names come from the catalog (the raw graph carries no names)."""
    cat_by_id = cat_by_id or {}
    label = {n["id"]: f"n{i}" for i, n in enumerate(proposal["nodes"])}
    graph = {"nodes": {}, "edges": []}
    for nid, k in label.items():
        node = {"jipipe:node-info-id": nid}
        c = cat_by_id.get(nid)
        if c:
            node["jipipe:node:name"] = c["name"]
        graph["nodes"][k] = node
    for e in proposal["edges"]:
        if e["source"] not in label or e["target"] not in label:
            return None
        graph["edges"].append({
            "source-node": label[e["source"]],
            "source-slot": e["source_slot"],
            "target-node": label[e["target"]],
            "target-slot": e["target_slot"],
        })
    return graph


def _pattern_data_types(graph, by_id):
    """The data types flowing OUT of the pattern's nodes (for catalog
    consumer selection) — plus the ones flowing in, as context."""
    dts = set()
    for v in graph["nodes"].values():
        n = by_id.get(v["jipipe:node-info-id"])
        if not n:
            continue
        dts.update(n.get("outputs", {}).keys())
    return dts


def cmd_run(limit: int, start: int = 0, count: int = 0,
            out_path: str = None, cache_path: str = None) -> int:
    J = _judge_module()
    reg = json.load(open(os.path.join(RESEARCH, "registry.json"),
                         encoding="utf-8"))
    reg_nodes = reg["nodes"]
    reg_functions = reg.get("functions", [])
    judgments = json.load(open(os.path.join(RESEARCH, "dag-judgments.json"),
                               encoding="utf-8"))
    clusters = {c["clusterId"]: c for c in
                json.load(open(os.path.join(RESEARCH, "dag-clusters.json"),
                               encoding="utf-8"))["clusters"]}
    node_descs = json.load(open(os.path.join(RESEARCH,
                                             "node-descriptions.json"),
                                encoding="utf-8"))
    by_id = {n["id"]: n for n in reg_nodes}

    print("building catalog (source slots + data types + branch)…")
    source_slots = build_source_slots(REPO_ROOT)
    catalog = build_catalog(reg_nodes, source_slots, reg_functions)
    cat_by_id = {c["id"]: c for c in catalog}
    print(f"  {len(catalog)} nodes, {sum(1 for c in catalog if c['slots_verified'])} with verified slots")
    twins = build_twin_map(catalog)
    n_pairs = len([v for v in twins.values() if v.get("3D")])
    print(f"  {n_pairs} verified 2D<->3D twin pairs")

    shipped = [e for e in judgments["entries"] if e["ship"]]
    mined_sigs = set()
    for e in shipped:
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        mined_sigs.add(structural_signature(g))
    print(f"mined patterns: {len(shipped)}")

    client = J.LLMClient.from_credentials()
    cache = J.load_cache(cache_path or CACHE)
    calls_made = [0]

    def llm(prompt):
        v = J.fetch_verdict(client.complete, cache, prompt)
        calls_made[0] += 1
        # periodic save: a killed shard must not lose its LLM calls (the
        # cache is content-keyed, so re-runs resume for free)
        if calls_made[0] % 5 == 0:
            J.save_cache(cache_path or CACHE, cache)
        return v

    candidates = []  # (cluster-like dict, provenance) — what-next only
    rejected = []
    results = []
    seen_sigs = set(mined_sigs)

    def add_candidate(graph, provenance, cluster_id):
        if structural_signature(graph) in seen_sigs:
            return  # already a mined pattern (or an earlier derived one)
        seen_sigs.add(structural_signature(graph))
        cand = {"clusterId": cluster_id, "support": 0,
                "representativeCandidate": {"graph": to_pattern_graph(graph),
                                            "nodeCount": len(graph["nodes"]),
                                            "edgeCount": len(graph["edges"]),
                                            "signal": "derived"}}
        cand["provenance"] = provenance
        candidates.append(cand)

    # ---- 1. mechanical twin swap (deterministic — the source pattern was
    #         already judged general, so its verified twin ships by
    #         construction; no LLM re-gate, no 16/26 false "noise") ----
    n_mech = 0
    for e in shipped:
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        for direction in ("3D", "2D"):
            entry = mechanical_twin_entry(e, g, direction, catalog,
                                          node_descs, twins)
            if entry is None:
                continue
            sig = structural_signature(entry["state"]["pattern"]["graph"])
            if sig in seen_sigs:
                continue
            seen_sigs.add(sig)
            n_mech += 1
            results.append(entry)

    # ---- 2. LLM what-next sweep (sharded: patterns[start:start+count],
    #         --limit further caps the shard) ----
    if count > 0:
        sweep = shipped[start:start + count]
    else:
        sweep = shipped[start:]
    if limit > 0:
        sweep = sweep[:limit]
    for i, e in enumerate(sweep):
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        state = _pattern_state(e, g, node_descs)
        dts = _pattern_data_types(g, cat_by_id)
        branch = e["functionId"].replace("jipipe:function:", "")
        sel = select_catalog(catalog, dts, branch, cap=50)
        prompt = build_what_next_prompt(state, sel)
        raw = llm(prompt)
        props = parse_proposals(raw)
        for p in props:
            graph = _proposal_to_graph(p)
            if graph is None:
                rejected.append({"derivedFrom": e["clusterId"], "name": p["name"],
                                 "reason": "edge endpoint not in nodes"})
                continue
            reason = validate_proposal(p, cat_by_id)
            if reason:
                rejected.append({"derivedFrom": e["clusterId"], "name": p["name"],
                                 "reason": reason})
                continue
            # fail-closed: every node used must have verified source slots
            if any(not cat_by_id[n["jipipe:node-info-id"]].get("slots_verified")
                   for n in graph["nodes"].values()):
                rejected.append({"derivedFrom": e["clusterId"], "name": p["name"],
                                 "reason": "unverified slot names"})
                continue
            add_candidate(graph,
                          {"mechanism": "what-next", "derivedFrom": e["clusterId"],
                           "draftName": p["name"], "draftDescription": p["description"]},
                          f"derived-wn-{e['clusterId']}-{i}")
        if (i + 1) % 25 == 0:
            print(f"  what-next {i+1}/{len(sweep)} (candidates so far: {len(candidates)})")

    J.save_cache(cache_path or CACHE, cache)

    # ---- gate the what-next survivors with the (C) rules ----
    for c in candidates:
        entry = judge_derived(c, llm, node_descs)
        results.append(entry)

    out = {"_note": ("Derived patterns (C'): the 'what next' siblings the mined "
                     "set under-covers. mechanism=twin-swap entries are "
                     "deterministic (verified structural equivalent of a shipped "
                     "pattern — no LLM re-gate); mechanism=what-next entries "
                     "went through the (C) gate. provenance is recorded here "
                     "only; they ship as normal patterns in (D). support is 0 "
                     "(derived, not observed). The graph in state.pattern is "
                     "in the (A) minimal format with 3.4 locations."),
           "rejected": rejected,
           "entries": results}
    dest = out_path if out_path is not None else OUT
    with open(dest, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    shipped_derived = [r for r in results if r["ship"]]
    from collections import Counter
    mech_shipped = sum(1 for r in results
                       if (r.get("provenance") or {}).get("mechanism")
                       == "twin-swap" and r["ship"])
    wn_shipped = sum(1 for r in results
                     if (r.get("provenance") or {}).get("mechanism")
                     == "what-next" and r["ship"])
    print(f"\nmechanical twins: {mech_shipped} shipped (deterministic)")
    print(f"what-next: {len(candidates)} candidates -> {wn_shipped} shipped "
          f"(gated), {len(rejected)} validation-rejected")
    print(f"total derived shipped: {len(shipped_derived)}")
    by_branch = Counter(r["functionId"].replace("jipipe:function:", "")
                        for r in shipped_derived)
    for b, c in by_branch.most_common():
        print(f"   {b:16s} {c}")
    print(f"output: {dest}")
    return 0


def selftest():
    tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "derive_dag_patterns_tests.py")
    old = sys.argv
    sys.argv = [tests]
    try:
        import runpy
        runpy.run_path(tests, run_name="__main__")
        return 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 1
    finally:
        sys.argv = old


def cmd_merge(shard_paths: list) -> int:
    """Merge sharded run outputs into dag-derived.json. The deterministic
    mechanical twins are RECOMPUTED with the current code (the in-shard
    twins may predate a fix — incident: the podman run predates the
    kebab-case slot-remap fix, so 2 import-roi-3d twins carried the stale 2D
    slot name "Files"); the LLM what-next entries come from the shards.
    One representative per structural signature, shipped preferred."""
    judgments = json.load(open(os.path.join(RESEARCH, "dag-judgments.json"),
                               encoding="utf-8"))
    clusters = {c["clusterId"]: c for c in
                json.load(open(os.path.join(RESEARCH, "dag-clusters.json"),
                               encoding="utf-8"))["clusters"]}
    mined_sigs = set()
    shipped_judg = [e for e in judgments["entries"] if e["ship"]]
    for e in shipped_judg:
        g = clusters[e["clusterId"]]["representativeCandidate"]["graph"]
        mined_sigs.add(structural_signature(g))
    shards = [json.load(open(p, encoding="utf-8")) for p in shard_paths]

    print("recomputing the deterministic twins with the current code…")
    reg = json.load(open(os.path.join(RESEARCH, "registry.json"),
                         encoding="utf-8"))
    node_descs = json.load(open(os.path.join(RESEARCH,
                                             "node-descriptions.json"),
                                encoding="utf-8"))
    source_slots = build_source_slots(REPO_ROOT)
    catalog = build_catalog(reg["nodes"], source_slots,
                            reg.get("functions", []))
    twins = build_twin_map(catalog)
    fresh_twins = recompute_twins(shipped_judg, clusters, catalog,
                                  node_descs, twins)
    print(f"  {len(fresh_twins)} twins (recomputed; in-shard twins discarded)")

    merged = merge_derived_outputs(shards, fresh_twins, mined_sigs)
    rejected = []
    for s in shards:
        rejected.extend(s.get("rejected", []))
    out = {"_note": ("Derived patterns (C'): the 'what next' siblings the mined "
                     "set under-covers. mechanism=twin-swap entries are "
                     "deterministic (verified structural equivalent of a shipped "
                     "pattern — no LLM re-gate); mechanism=what-next entries "
                     "went through the (C) gate. provenance is recorded here "
                     "only; they ship as normal patterns in (D). support is 0 "
                     "(derived, not observed). The graph in state.pattern is "
                     "in the (A) minimal format with 3.4 locations. Built by "
                     "merging sharded runs."),
           "rejected": rejected,
           "entries": merged}
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    shipped = [e for e in merged if e["ship"]]
    print(f"merged {len(shards)} shards -> {len(merged)} entries "
          f"({len(shipped)} shipped, {len(rejected)} validation-rejected)")
    from collections import Counter
    mech = sum(1 for e in shipped
               if (e.get("provenance") or {}).get("mechanism") == "twin-swap")
    wn = len(shipped) - mech
    print(f"  mechanical twins: {mech}, what-next: {wn}")
    by_branch = Counter(e["functionId"].replace("jipipe:function:", "")
                        for e in shipped)
    for b, c in by_branch.most_common():
        print(f"   {b:16s} {c}")
    print(f"output: {OUT}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("mode", nargs="?", default="help",
                    choices=["help", "selftest", "run", "merge"])
    ap.add_argument("--limit", type=int, default=0,
                    help="run: sweep at most N shipped patterns")
    ap.add_argument("--start", type=int, default=0,
                    help="run: start index into the shipped list (sharding)")
    ap.add_argument("--count", type=int, default=0,
                    help="run: number of shipped patterns in this shard")
    ap.add_argument("--out", default=None,
                    help="run: output path (shard file)")
    ap.add_argument("--cache", default=None,
                    help="run: LLM cache path (per-shard when parallel)")
    ap.add_argument("shards", nargs="*",
                    help="merge: the shard output paths")
    args = ap.parse_args(argv)
    if args.mode == "selftest":
        return selftest()
    if args.mode == "merge":
        if not args.shards:
            ap.error("merge needs the shard paths")
        return cmd_merge(args.shards)
    if args.mode == "run":
        return cmd_run(args.limit, args.start, args.count, args.out, args.cache)
    ap.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
