#!/usr/bin/env python3
"""Harvest DAG pattern candidates from the real JIPipe corpus (plan step A,
#1324 / #1333).

Part of the DAG-aware pattern mining plan
(``docs/superpowers/plans/2026-10-16-dag-pattern-mining.md``). This is the
**deterministic harvest stage — no AI**. It walks the corpus's real ``.jip``
projects and emits the groupings HUMANS already authored as candidate
patterns:

* **signal 1 — canvas-note sections** (``graph-annotation-text-box`` /
  ``graph-annotation-group-box``): the rect-contained real nodes form the
  candidate; title = the note's ``text-title``.
* **signal 2 — group nodes** (``node-group``): the group's embedded
  ``contents`` subgraph is the candidate verbatim; title = the group's
  ``jipipe:node:name`` (the default name "Group" is not a title).
* **signal 3 — named compartments** (``jipipe:project-compartment``): the
  UUID-sharing real nodes form the candidate; title = the compartment's
  display name (a small stop-list rejects generic names — the JEV gate in
  step (C) remains the real authority).
* **signal 3b — geometric clusters** (unannotated): related nodes are placed
  together even without a note (plan v2 addition, commit (72)). Single-linkage
  clustering over grid coordinates with an automatic gap cut (the largest
  ratio between consecutive merge distances). No title — these route
  through the merge stage (title transfer) and the JEV namer in step (C);
  a candidate that ends up nameless there is rejected there.

Cross-cutting filters: 3–12 real nodes (the measured band), no compartment
sinks / annotation / plumbing nodes in the member set, and no duplicate of
an already-shipped pattern (functional signature vs the committed System
template resources).

**Node locations (plan §3.4, the data-side positioning fix):** every shipped
candidate's nodes must carry a grid location pair so a template drop does not
bunch on one cell. The source of truth is the SOURCE PROJECT's layout,
copied verbatim and **normalized** so the candidate's bounding-box top-left
sits at the origin. Locations are re-keyed to the candidate's own single
``jipipe:graph-compartment`` UUID (the insert path,
``rekeyTemplateGraphToCompartment`` (75), then re-keys it to the canvas at
drop time). Candidates whose source layout is unusable (most group
``contents`` carry no or only legacy locations) fall back to a compact
longest-path layering computed at mine time.

Output: ``utils/taxonomy-research/dag-candidates.json`` —

.. code-block:: json

    {
      "version": 1,
      "generated": "<UTC ISO-8601>",
      "corpusRoot": "...",
      "projectsScanned": 541,
      "candidates": [
        {
          "candidateId": "dag-001",
          "signal": "canvas-note-section",
          "project": "Cellpose training example/cellpose4_training.jip",
          "title": "Loading raw images and ROIs",
          "support": 2,
          "memberProjects": ["..."],
          "nodeCount": 8,
          "edgeCount": 8,
          "layoutSource": "source",
          "compartmentUuid": "1234...",
          "graph": {"nodes": {...}, "edges": [...]},
          "signature": "..."
        }
      ]
    }

The script is **deterministic except for the ``generated`` timestamp**
(sorted iteration throughout; stable JSON serialization; candidate ids are
stable). Tests: ``utils/mine_dag_patterns_tests.py`` (run via
``mine-dag-patterns.py selftest``).

Usage (from the repository root):

    python3 utils/mine-dag-patterns.py selftest
    python3 utils/mine-dag-patterns.py /data/JIPipe
    python3 utils/mine-dag-patterns.py /data/JIPipe \
        --shipped-dir <dir-with-shipped-pattern-jsons> \
        --out <dag-candidates.json>
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import uuid
from collections import Counter
from datetime import datetime, timezone

VERSION = 1

# Node types that are plumbing, never real members of a pattern (plan §3.1:
# "Drop annotation nodes themselves and compartment sinks from the member
# set"). jipipe:comment attaches to a node and carries no wiring.
PLUMBING_IDS = {
    "graph-annotation-text-box",
    "graph-annotation-group-box",
    "jipipe:compartment-output",
    "jipipe:compartment-input",
    "jipipe:project-compartment",
    "jipipe:comment",
    "node-group",
}

# Compartment sinks are rejected ANYWHERE in a candidate (even inside a
# group's contents) — they are workflow-specific plumbing (the (69) rule).
SINK_IDS = {"jipipe:compartment-output", "jipipe:compartment-input"}

# Generic, non-derivable compartment names (ruling A6): harvesting them would
# produce candidates whose only name evidence is a stop-word. The JEV gate
# (step C) would reject them anyway; this keeps the harvest lean.
COMPARTMENT_STOP_LIST = {
    "analysis", "main", "pipeline", "processing", "default", "empty", "new",
    "test", "tests", "output", "input", "untitled", "group", "section",
    "general", "other",
}

DEFAULT_GROUP_NAME = "group"

# The measured node band (plan §2 / §3.1), real nodes only (ruling A10).
MIN_NODES = 3
MAX_NODES = 12

# Geometric clustering (signal 3b, ruling A2): single-linkage over grid
# coordinates, cut at the largest ratio between consecutive merge distances,
# at most this many clusters per compartment.
MAX_CLUSTERS = 8
# A gap ratio below this is not a "distinct from other parts" separation
# (the (72) measurement saw clean cuts at 1.3–2.5; uniform spacing gives
# ratios near 1.0 and must NOT cut).
MIN_GAP_RATIO = 1.25

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_OUT = os.path.join(REPO_ROOT, "utils", "taxonomy-research",
                           "dag-candidates.json")
DEFAULT_SHIPPED_DIR = os.path.join(
    REPO_ROOT, "jipipe-core", "src", "main", "resources",
    "org", "hkijena", "jipipe", "plugins", "nodetemplate", "patterns")

# Directories that are not user corpus (build/tooling/vendor/docs trees).
# "fiji" = the ImageJ distribution; "jipipe-release" = a vendored source build
# tree (its target/classes are exact copies of its own src/main/resources, so
# including it would double-count); "tmp"/"temp" = scratch; "documentation" =
# the docs tree (the plan's "tmp dirs, Fiji, docs" exclusions, plus the
# build-tree that the plan's count was not intended to include).
EXCLUDED_PART_PREFIXES = ("fiji", "jipipe-release", "tmp", "temp", "documentation")


# --------------------------------------------------------------------------- #
# Location resolution
# --------------------------------------------------------------------------- #

def node_grid_location(node: dict, compartment: str):
    """The node's grid location (x, y) within ``compartment``, or None.

    Resolution order (mirrors ``JIPipeGraphNode.fromJson`` +
    ``getNodeUILocationWithin``):

    1. modern ``jipipe:metadata-v1`` ``location/<compartment>/x|y`` — the
       canonical format (the legacy format is migrated INTO this store at
       load time);
    2. legacy ``jipipe:ui-grid-location``: the entry whose compartment key
       equals ``compartment`` ("" for root) OR, if the node has exactly one
       compartment key and it is not the requested one, that single entry
       (stale multi-compartment keys are NEVER used — the modern store's
       own keys win and they are compartment-qualified);
    3. the legacy ``""`` entry as a last resort.

    Only the "VerticalCompact" visual mode is considered (the one Java
    migrates).
    """
    meta = node.get("jipipe:metadata-v1")
    if isinstance(meta, dict):
        x = meta.get(f"location/{compartment}/x")
        y = meta.get(f"location/{compartment}/y")
        if isinstance(x, (int, float)) and isinstance(y, (int, float)):
            return (x, y)
    ugl = node.get("jipipe:ui-grid-location")
    if isinstance(ugl, dict):
        def pick(key):
            modes = ugl.get(key)
            if not isinstance(modes, dict):
                return None
            vc = modes.get("VerticalCompact")
            if not isinstance(vc, dict):
                return None
            x, y = vc.get("x"), vc.get("y")
            if isinstance(x, (int, float)) and isinstance(y, (int, float)):
                return (x, y)
            return None
        loc = pick(compartment)
        if loc is not None:
            return loc
        keys = [k for k, v in ugl.items() if isinstance(v, dict)]
        if len(keys) == 1 and keys[0] != compartment:
            loc = pick(keys[0])
            if loc is not None:
                return loc
        return pick("")
    return None


# --------------------------------------------------------------------------- #
# Fallback layout (recovered from the reverted (73) runtime layout)
# --------------------------------------------------------------------------- #

def topo_layering(graph: dict):
    """Longest-path layering: returns {node key: layer} where a node's layer
    is its longest distance (in edges) from any source. Inputs before their
    consumers (Kahn order); a node with no slots gets layer 0. Cycles are
    broken deterministically (the remaining node in the highest alphabetical
    order is layered after its already-layered predecessors)."""
    nodes = graph["nodes"]
    edges = graph["edges"]
    pred = {k: [] for k in nodes}
    for e in edges:
        if e["source-node"] in pred and e["target-node"] in pred:
            pred[e["target-node"]].append(e["source-node"])
    layer = {}
    done = set()
    remaining = set(nodes)
    while remaining:
        ready = [k for k in sorted(remaining)
                 if all(p in done for p in pred[k])]
        if not ready:
            # deterministic cycle break
            ready = [max(remaining)]
        for k in ready:
            layer[k] = max((layer[p] + 1 for p in pred[k] if p in layer), default=0)
            done.add(k)
            remaining.discard(k)
    return layer


def fallback_layout(graph: dict):
    """Compute grid locations for a candidate whose source layout is unusable:
    column = longest-path layer (x), row = position within the layer, top to
    bottom in topological (Kahn) order with ~2 rows of headroom per node (y).
    Returns {node key: (x, y)} — already origin-anchored."""
    layer = topo_layering(graph)
    # Kahn order with alphabetical tie-break: stable row order
    nodes = graph["nodes"]
    edges = graph["edges"]
    indeg = {k: 0 for k in nodes}
    succ = {k: [] for k in nodes}
    for e in edges:
        if e["source-node"] in indeg and e["target-node"] in indeg:
            succ[e["source-node"]].append(e["target-node"])
            indeg[e["target-node"]] += 1
    order = []
    remaining = set(nodes)
    while remaining:
        ready = sorted(k for k in remaining if indeg[k] == 0)
        if not ready:
            ready = [max(remaining)]
        for k in ready:
            order.append(k)
            remaining.discard(k)
            for s in succ[k]:
                indeg[s] -= 1
    row = Counter()
    locs = {}
    for k in order:
        l = layer[k]
        locs[k] = (l, row[l] * 2)
        row[l] += 1
    return locs


# --------------------------------------------------------------------------- #
# Geometric clustering (signal 3b, ruling A2)
# --------------------------------------------------------------------------- #

def auto_cluster(points: dict):
    """Single-linkage clustering over grid coordinates with an automatic gap
    cut (plan §3.1 signal 3b, commit (72)): the largest RATIO between
    consecutive merge distances is the natural separation between "related
    nodes placed together" and "distinct from other parts".

    Full single-linkage runs to one cluster, recording each merge distance.
    If the largest ratio d[i+1]/d[i] between consecutive merge distances is
    below :data:`MIN_GAP_RATIO`, there is no natural gap and the points form
    ONE cluster. Otherwise the linkage is re-run and stopped after the merge
    that precedes the largest ratio, yielding ``n - (split_idx + 1)``
    clusters.

    Returns a list of frozensets of point keys, in deterministic merge order.
    Points with no coordinates are excluded by the caller.
    """
    items = sorted(points.items())
    n = len(items)
    if n == 0:
        return []
    if n <= 1:
        return [frozenset(k for k, _ in items)]
    merge_dists = _linkage_with_dists(items)
    ratios = [merge_dists[i + 1] / merge_dists[i]
              for i in range(len(merge_dists) - 1) if merge_dists[i] > 0]
    if not ratios or max(ratios) < MIN_GAP_RATIO:
        return [frozenset(k for k, _ in items)]
    split_idx = max(range(len(ratios)), key=lambda i: ratios[i])
    target = max(1, n - (split_idx + 1))
    return _run_linkage(items, target)


def _dist(a, b):
    return ((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2) ** 0.5


def _all_pairs(items):
    n = len(items)
    pd = [[0.0] * n for _ in range(n)]
    for i in range(n):
        for j in range(i + 1, n):
            d = _dist(items[i][1], items[j][1])
            pd[i][j] = pd[j][i] = d
    return pd


def _closest_pair(clusters, pd):
    best = None
    for i in range(len(clusters)):
        for j in range(i + 1, len(clusters)):
            d = min(pd[x][y] for x in clusters[i] for y in clusters[j])
            key = (d, min(clusters[i]), min(clusters[j]))
            if best is None or key < best[0]:
                best = (key, i, j)
    return best[1], best[2], best[0][0]


def _merge(clusters, i, j):
    a, b = clusters[i], clusters[j]
    if min(a) > min(b):
        a, b = b, a
    a.extend(b)
    clusters = [c for k, c in enumerate(clusters) if k not in (i, j)] + [a]
    clusters.sort(key=min)
    return clusters


def _run_linkage(items, stop_clusters):
    """Deterministic single-linkage (alphabetical point order, the cluster
    containing the alphabetically-first point survives a merge) stopped when
    ``stop_clusters`` clusters remain."""
    pd = _all_pairs(items)
    clusters = [[i] for i in range(len(items))]
    while len(clusters) > stop_clusters:
        i, j, _d = _closest_pair(clusters, pd)
        clusters = _merge(clusters, i, j)
    return [frozenset(items[idx][0] for idx in c) for c in clusters]


def _linkage_with_dists(items):
    """Deterministic single-linkage to one cluster; returns the list of
    merge distances (one per merge, in merge order)."""
    pd = _all_pairs(items)
    clusters = [[i] for i in range(len(items))]
    dists = []
    while len(clusters) > 1:
        i, j, d = _closest_pair(clusters, pd)
        dists.append(d)
        clusters = _merge(clusters, i, j)
    return dists


# --------------------------------------------------------------------------- #
# Functional signature + dedup
# --------------------------------------------------------------------------- #

def functional_signature(graph: dict):
    """The signature used for dedup (ruling A5): the sorted node-info ids +
    the sorted (srcInfoId, srcSlot, tgtInfoId, tgtSlot) edge tuples. Instance
    keys, compartment UUIDs, node labels and locations are ignored — the
    same rule class as the (69) generator's dedup, so a candidate that is
    functionally one of the shipped 64 is a duplicate whether or not it was
    mined with different labels/layout."""
    im = {k: v["jipipe:node-info-id"] for k, v in graph["nodes"].items()}
    ids = tuple(sorted(v["jipipe:node-info-id"] for v in graph["nodes"].values()))
    edges = tuple(sorted(
        (im[e["source-node"]], e["source-slot"], im[e["target-node"]], e["target-slot"])
        for e in graph["edges"]))
    return json.dumps([list(ids), [list(e) for e in edges]],
                      sort_keys=True, ensure_ascii=False)


def load_shipped_signatures(shipped_dir):
    if not shipped_dir or not os.path.isdir(shipped_dir):
        return set()
    sigs = set()
    for f in sorted(os.listdir(shipped_dir)):
        if not f.endswith(".json") or f == "system-templates.json":
            continue
        with open(os.path.join(shipped_dir, f), encoding="utf-8") as fh:
            g = json.load(fh)
        if "nodes" in g and "edges" in g:
            sigs.add(functional_signature(g))
    return sigs


# --------------------------------------------------------------------------- #
# Candidate construction
# --------------------------------------------------------------------------- #

def _candidate_compartment_uuid(candidate_id_seed: str) -> str:
    """One fresh deterministic UUID per candidate (ruling A4)."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL,
                          "jipipe-dag-candidate:" + candidate_id_seed))


def _minimal_node(node: dict, info_id: str, name: str, compartment: str):
    return {
        "jipipe:node-info-id": info_id,
        "jipipe:graph-compartment": compartment,
        "jipipe:node:name": name,
    }


def build_candidate_graph(member_nodes, member_edges, source_locations,
                          compartment_uuid, require_source_layout):
    """Assemble a candidate's minimal ``JIPipeGraph`` (ruling A3) with its
    locations (plan §3.4).

    ``member_nodes``: {original key: node dict} — already plumbing-free.
    ``member_edges``: edges with both endpoints in the member set.
    ``source_locations``: {original key: (x, y)} as resolved from the source
        project (only members, only nodes that HAVE a location).
    ``require_source_layout``: when True (section/compartment/cluster —
        induced from the canvas), a candidate is layout-"source" iff EVERY
        member has a resolved location; otherwise it falls back. When False
        (group contents — an embedded subgraph), even partial source
        locations are not usable (inner nodes sit in the group's local
        coordinate space, not the canvas) → fallback unless every member
        has one, in which case "source" (normalized like the canvas case).

    Returns (graph, layout_source). ``compartment_uuid`` may be "" (the
    default compartment); locations are then stored under the canonical
    ``location/_`` key (Java's ``StringUtils.orElse(compartment, "_")``).
    """
    comp_key = compartment_uuid or "_"
    if source_locations and len(source_locations) == len(member_nodes):
        xs = [x for x, _ in source_locations.values()]
        ys = [y for _, y in source_locations.values()]
        min_x, min_y = min(xs), min(ys)
        layout = "source"
    else:
        # Fallback: the longest-path layering of the candidate's OWN wiring
        probe = {"nodes": {k: n for k, n in member_nodes.items()},
                 "edges": member_edges}
        locs = fallback_layout(probe)
        min_x = min((x for x, _ in locs.values()), default=0)
        min_y = min((y for _, y in locs.values()), default=0)
        layout = "fallback"
    # re-key member nodes to fresh n0..nK keys (alphabetical by original key
    # for determinism)
    keys = sorted(member_nodes)
    keymap = {k: f"n{i}" for i, k in enumerate(keys)}
    nodes = {}
    for k in keys:
        n = member_nodes[k]
        info_id = n.get("jipipe:node-info-id")
        if layout == "source":
            sx, sy = source_locations[k]
            x, y = int(round(sx - min_x)), int(round(sy - min_y))
        else:
            x, y = int(round(locs[k][0] - min_x)), int(round(locs[k][1] - min_y))
        node = _minimal_node(n, info_id, n.get("jipipe:node:name") or "",
                             compartment_uuid)
        node["jipipe:metadata-v1"] = {
            f"location/{comp_key}/x": x,
            f"location/{comp_key}/y": y,
        }
        nodes[keymap[k]] = node
    edges = []
    for e in sorted(member_edges, key=lambda e: (keymap.get(e["source-node"], ""),
                                                 e.get("source-slot", ""),
                                                 keymap.get(e["target-node"], ""),
                                                 e.get("target-slot", ""))):
        if e["source-node"] not in keymap or e["target-node"] not in keymap:
            continue
        edges.append({
            "source-node": keymap[e["source-node"]],
            "source-slot": e.get("source-slot", ""),
            "target-node": keymap[e["target-node"]],
            "target-slot": e.get("target-slot", ""),
        })
    return {"nodes": nodes, "edges": edges}, layout


def real_nodes_in(nodes: dict):
    """{key: node} for nodes whose info id is not plumbing (ruling A8)."""
    return {k: n for k, n in nodes.items()
            if n.get("jipipe:node-info-id") not in PLUMBING_IDS}


def induced_edges(edges, member_keys):
    m = set(member_keys)
    return [e for e in edges
            if e.get("source-node") in m and e.get("target-node") in m]


def connected_components(graph: dict):
    """The graph's connected components (undirected view), each a dict of
    {node: node} / edges, deterministically ordered (component containing
    the alphabetically-first node first; nodes sorted)."""
    nodes = graph["nodes"]
    adj = {k: set() for k in nodes}
    for e in graph["edges"]:
        s, t = e.get("source-node"), e.get("target-node")
        if s in adj and t in adj:
            adj[s].add(t)
            adj[t].add(s)
    seen = set()
    comps = []
    for k in sorted(nodes):
        if k in seen:
            continue
        stack = [k]
        comp = set()
        while stack:
            cur = stack.pop()
            if cur in seen:
                continue
            seen.add(cur)
            comp.add(cur)
            stack.extend(adj[cur])
        members = {c: nodes[c] for c in sorted(comp)}
        comps.append({"nodes": members,
                      "edges": induced_edges(graph["edges"], comp)})
    return comps


def in_rect(loc, rx, ry, w, h):
    return rx <= loc[0] < rx + w and ry <= loc[1] < ry + h


# --------------------------------------------------------------------------- #
# Per-project harvest
# --------------------------------------------------------------------------- #

def harvest_project(rel_path, project, stop_list=COMPARTMENT_STOP_LIST):
    """Harvest all four candidate signals from one parsed .jip project.

    Returns (candidates, claimed) where each candidate is a dict WITHOUT the
    candidateId/signature/support (assigned corpus-wide) and ``claimed`` is
    the set of (compartment, node key) pairs already used by a TITLED
    candidate — geometric clustering only fills the uncovered gaps (ruling
    A2: "where notes exist, the note rectangles are the ground truth and
    geometric clustering only fills the uncovered gaps").
    """
    out = []
    graph = project.get("graph") or {}
    nodes = graph.get("nodes") or {}
    edges = graph.get("edges") or []
    if not nodes:
        return out, set()

    # compartment display names (container nodes)
    comp_names = {}
    for ck, cn in ((project.get("compartments") or {})
                   .get("compartment-graph", {}).get("nodes", {}) or {}).items():
        if cn.get("jipipe:node-info-id") == "jipipe:project-compartment":
            name = (cn.get("jipipe:node:name") or "").strip()
            if name:
                comp_names[ck] = name

    claimed = set()
    candidates_raw = []  # (signal, title_or_None, member_nodes, member_edges,
                          # source_locations)

    def locations_for(member_nodes):
        locs = {}
        for k, n in member_nodes.items():
            comp = n.get("jipipe:graph-compartment") or ""
            loc = node_grid_location(n, comp)
            if loc is not None:
                locs[k] = loc
        return locs

    # --- signal 2: group nodes (embedded contents, verbatim) ------------- #
    for gk in sorted(nodes):
        g = nodes[gk]
        if g.get("jipipe:node-info-id") != "node-group":
            continue
        contents = g.get("contents") or {}
        inner = contents.get("nodes") or {}
        inner_edges = contents.get("edges") or []
        # the group's own local compartment (inner nodes share it; usually "")
        inner_comp = next(iter(n.get("jipipe:graph-compartment") or "" for n in inner.values())) if inner else ""
        members = real_nodes_in(inner)
        inner_edges = induced_edges(inner_edges, members)
        if not (MIN_NODES <= len(members) <= MAX_NODES):
            continue
        # a group's contents ARE the authored recipe: one that hands data to
        # a compartment sink is workflow plumbing, not a pattern (the (69)
        # rule) — rejected on the RAW inner set (sinks are plumbing-filtered
        # out of `members` above and would never be seen there)
        if any(n.get("jipipe:node-info-id") in SINK_IDS for n in inner.values()):
            continue
        name = (g.get("jipipe:node:name") or "").strip()
        title = None if name.lower() == DEFAULT_GROUP_NAME else (name or None)
        locs = locations_for(members)
        # group contents: inner locations are in the group's LOCAL coordinate
        # space (relative to the group's own origin), so they are a usable
        # source layout iff every member has one (they are already
        # origin-relative); otherwise the fallback.
        candidates_raw.append(("group-node", title, members, inner_edges, locs))
        if title is not None:
            for k in members:
                claimed.add((inner_comp, k))

    # --- signal 1: canvas-note sections (geometric containment) ---------- #
    for ak in sorted(nodes):
        a = nodes[ak]
        info = a.get("jipipe:node-info-id")
        if info not in ("graph-annotation-text-box", "graph-annotation-group-box"):
            continue
        title = (a.get("text-title") or "").strip() or None
        comp = a.get("jipipe:graph-compartment") or ""
        al = node_grid_location(a, comp)
        w = a.get("grid-width")
        h = a.get("grid-height")
        if al is None or not isinstance(w, (int, float)) or not isinstance(h, (int, float)):
            continue
        rx, ry = al
        real = real_nodes_in({k: n for k, n in nodes.items()
                              if (n.get("jipipe:graph-compartment") or "") == comp})
        member_keys = {k for k, n in real.items()
                       if (loc := node_grid_location(n, comp)) is not None
                       and in_rect(loc, rx, ry, w, h)}
        members = {k: real[k] for k in sorted(member_keys)}
        # a titled section's members are ground truth: claim them so the
        # geometric signal does not re-cluster them
        if title:
            for k in members:
                claimed.add((comp, k))
        if len(members) < 2:
            continue
        comp_edges = induced_edges(edges, member_keys)
        # split into connected components; keep those in band (ruling A8)
        for comp_graph in connected_components({"nodes": members, "edges": comp_edges}):
            if MIN_NODES <= len(comp_graph["nodes"]) <= MAX_NODES:
                candidates_raw.append(("canvas-note-section", title,
                                       comp_graph["nodes"], comp_graph["edges"],
                                       locations_for(comp_graph["nodes"])))

    # --- signal 3: named compartments (UUID-sharing members) -------------- #
    for comp_uid in sorted(set((n.get("jipipe:graph-compartment") or "")
                               for n in nodes.values()) | set(comp_names)):
        name = comp_names.get(comp_uid)
        if not name or name.lower() in stop_list:
            continue
        real = real_nodes_in({k: n for k, n in nodes.items()
                              if (n.get("jipipe:graph-compartment") or "") == comp_uid})
        if not (MIN_NODES <= len(real) <= MAX_NODES):
            continue
        # the whole compartment is ONE authored unit; keep it as a single
        # candidate (all components) if in band — no splitting here, the
        # compartment is the boundary
        candidates_raw.append(("compartment", name, real,
                               induced_edges(edges, real),
                               locations_for(real)))
        for k in real:
            claimed.add((comp_uid, k))

    # --- signal 3b: geometric clusters (unannotated) ---------------------- #
    # per compartment, over the real nodes NOT claimed by any titled
    # candidate and that have a location
    per_comp = {}
    for k, n in nodes.items():
        if n.get("jipipe:node-info-id") in PLUMBING_IDS:
            continue
        comp = n.get("jipipe:graph-compartment") or ""
        if (comp, k) in claimed:
            continue
        loc = node_grid_location(n, comp)
        if loc is None:
            continue
        per_comp.setdefault(comp, {})[k] = loc
    for comp in sorted(per_comp):
        points = per_comp[comp]
        if len(points) < MIN_NODES:
            continue
        clusters = auto_cluster(points)
        clusters = [c for c in clusters
                    if MIN_NODES <= len(c) <= MAX_NODES]
        if len(clusters) > MAX_CLUSTERS:
            clusters = clusters[:MAX_CLUSTERS]
        for cluster in clusters:
            members = {k: nodes[k] for k in sorted(cluster)}
            candidates_raw.append(("geometric-cluster", None, members,
                                   induced_edges(edges, members),
                                   {k: points[k] for k in cluster}))

    # --- assemble candidates (band + sink filter, graph + layout) --------- #
    for signal, title, members, member_edges, locs in candidates_raw:
        if not (MIN_NODES <= len(members) <= MAX_NODES):
            continue
        # (sinks cannot reach here: they are plumbing-filtered out of the
        # member sets at every signal; group-level sink recipes are rejected
        # at the group signal itself)
        # band + sink filter; the compartment UUID is assigned corpus-wide in
        # mine_corpus (derived from the unique candidateId, ruling A4)
        graph_out, layout = build_candidate_graph(members, member_edges, locs,
                                                  "",
                                                  require_source_layout=(
                                                      signal != "group-node"))
        out.append({
            "signal": signal,
            "project": rel_path,
            "title": title,
            "nodeCount": len(members),
            "edgeCount": len(graph_out["edges"]),
            "layoutSource": layout,
            "graph": graph_out,
        })
    return out, claimed


# --------------------------------------------------------------------------- #
# Corpus walk
# --------------------------------------------------------------------------- #

def iter_corpus_files(root):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames
                             if not d.lower().startswith(EXCLUDED_PART_PREFIXES))
        for fn in sorted(filenames):
            if fn.endswith(".jip"):
                yield os.path.relpath(os.path.join(dirpath, fn), root)


def mine_corpus(root, out_path, shipped_dir=None, generated=None):
    """Run the harvest over ``root`` and write the candidates file.

    Returns the result dict (also written to ``out_path``).
    ``shipped_dir=None`` → the committed System template resources dir.
    ``generated=None`` → the current UTC timestamp; pass a fixed string for
    byte-identical (re-)runs — the harvest itself is fully deterministic.
    """
    if shipped_dir is None:
        shipped_dir = DEFAULT_SHIPPED_DIR
    if generated is None:
        generated = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    shipped_sigs = load_shipped_signatures(shipped_dir)

    projects = []
    for rel in iter_corpus_files(root):
        try:
            with open(os.path.join(root, rel), encoding="utf-8") as fh:
                p = json.load(fh)
        except Exception:
            continue
        if not isinstance(p, dict):
            continue
        if not (p.get("graph") or {}).get("nodes"):
            continue
        projects.append((rel, p))
    projects.sort(key=lambda t: t[0])

    raw = []  # (project, candidate)
    for rel, p in projects:
        cands, _ = harvest_project(rel, p)
        raw.extend((rel, c) for c in cands)

    # support counts (ruling A9): distinct projects per functional signature
    sig_projects = {}
    for rel, c in raw:
        sig = functional_signature(c["graph"])
        sig_projects.setdefault(sig, set()).add(rel)

    # shipped-duplicate filter (ruling A5)
    kept = []
    dropped_shipped = 0
    for rel, c in raw:
        sig = functional_signature(c["graph"])
        if sig in shipped_sigs:
            dropped_shipped += 1
            continue
        kept.append((rel, c))

    # deterministic candidate ids (ruling A11): project-sorted, signal order
    signal_order = {"group-node": 0, "canvas-note-section": 1,
                    "compartment": 2, "geometric-cluster": 3}
    kept.sort(key=lambda t: (t[0], signal_order[t[1]["signal"]],
                             json.dumps(t[1]["graph"], sort_keys=True)))
    candidates = []
    for i, (rel, c) in enumerate(kept):
        candidate_id = f"dag-{i + 1:03d}"
        # one deterministic UUID per candidate (ruling A4), derived from the
        # unique candidateId so it is stable across runs and collision-free
        cand_uuid = _candidate_compartment_uuid(candidate_id)
        # re-key the graph's compartment to this UUID (it was built with "")
        graph = {
            "nodes": {k: {**v, "jipipe:graph-compartment": cand_uuid,
                          "jipipe:metadata-v1": {
                              f"location/{cand_uuid}/x":
                                  v["jipipe:metadata-v1"]["location/_/x"],
                              f"location/{cand_uuid}/y":
                                  v["jipipe:metadata-v1"]["location/_/y"],
                          }}
                      for k, v in c["graph"]["nodes"].items()},
            "edges": c["graph"]["edges"],
        }
        sig = functional_signature(graph)
        rec = {
            "candidateId": candidate_id,
            "signal": c["signal"],
            "project": rel,
            "title": c["title"],
            "support": len(sig_projects[sig]),
            "memberProjects": sorted(sig_projects[sig]),
            "nodeCount": c["nodeCount"],
            "edgeCount": c["edgeCount"],
            "layoutSource": c["layoutSource"],
            "compartmentUuid": cand_uuid,
            "graph": graph,
            "signature": sig,
        }
        candidates.append(rec)

    result = {
        "version": VERSION,
        "generated": generated,
        "corpusRoot": os.path.abspath(root),
        "projectsScanned": len(projects),
        "shippedDuplicatesDropped": dropped_shipped,
        "candidates": candidates,
    }
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")
    return result


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #

def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Harvest DAG pattern candidates from the JIPipe corpus "
                    "(plan step A — deterministic, no AI).")
    parser.add_argument("corpus", nargs="?", default="/data/JIPipe",
                        help="corpus root (default: /data/JIPipe)")
    parser.add_argument("--out", default=DEFAULT_OUT,
                        help=f"output file (default: {DEFAULT_OUT})")
    parser.add_argument("--shipped-dir", default=DEFAULT_SHIPPED_DIR,
                        help="dir of the shipped System pattern JSONs used "
                             "for the duplicate filter")
    args = parser.parse_args(argv)

    if args.corpus == "selftest":
        import runpy
        tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                             "mine_dag_patterns_tests.py")
        old_argv = sys.argv
        sys.argv = [tests]
        try:
            runpy.run_path(tests, run_name="__main__")
            return 0
        except SystemExit as e:
            return e.code if isinstance(e.code, int) else 1
        finally:
            sys.argv = old_argv

    if not os.path.isdir(args.corpus):
        print(f"corpus root not found: {args.corpus}", file=sys.stderr)
        return 2
    result = mine_corpus(args.corpus, args.out,
                         shipped_dir=args.shipped_dir or None)
    by_signal = Counter(c["signal"] for c in result["candidates"])
    by_title = sum(1 for c in result["candidates"] if c["title"])
    src = Counter(c["layoutSource"] for c in result["candidates"])
    print(f"=== DAG pattern harvest (step A) ===")
    print(f"corpus root : {args.corpus}")
    print(f"projects    : {result['projectsScanned']} scanned")
    print(f"candidates  : {len(result['candidates'])} "
          f"({by_title} titled, {len(result['candidates']) - by_title} untitled)")
    for sig, cnt in sorted(by_signal.items()):
        print(f"   {sig}: {cnt}")
    print(f"layouts     : source {src.get('source', 0)} / "
          f"fallback {src.get('fallback', 0)}")
    print(f"shipped dups dropped: {result['shippedDuplicatesDropped']}")
    top = sorted(result["candidates"], key=lambda c: (-c["support"], c["candidateId"]))[:10]
    print("top by support:")
    for c in top:
        print(f"   {c['candidateId']} support={c['support']} "
              f"{c['signal']} {c['title']!r} nodes={c['nodeCount']} "
              f"edges={c['edgeCount']} [{c['layoutSource']}]")
    print(f"output      : {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
