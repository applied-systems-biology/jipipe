#!/usr/bin/env python3
"""Generate the DAG system-template resources + sidecar entries (step D,
work item 1324).

Companion to generate-system-template-metadata.py, which owns the
LINEAR (node-sequence) patterns from the 185 research artifacts. Option C
curation (ruling 2026-19) ships only the 24 genuinely-linear-only workflows
(outcome names, no chain separator); the 32 node+edge sub-paths of already-
shipped DAG patterns and the 8 plumbing (comment/group) patterns are dropped.
This script ships the DAG patterns mined/derived by the taxonomy-research
pipeline:

  * 262 MINED patterns  — dag-judgments.json (ship=true), minus the 3
    dag-stale-id-plan.json exclusions, with the 3 stale-id remaps applied
    (verified 2026-10-19: same-algorithm successors, slot names preserved);
  * 160 DERIVED patterns — dag-derived.json (ship=true): 26 deterministic
    twin swaps + 134 (C)-gated what-next chains (incl. the 2026-10-19
    full-catalog re-sweep's 6 cLij2 GPU push->kernel->pull chains).

Total: 422 mined/derived DAG patterns, cleaned at generation (group I/O
wrapper nodes stripped, the {Parameters} external slot enabled where wired,
and the 16 item-3c drops: 5 JIPipe-scripting patterns, 2 loop-plumbing
tunnel patterns, 9 degenerate multi-component harvest artifacts; 34 further
multi-component patterns keep their largest connected component) -> 406
shipped, on top of the 24 linear ones (430 System templates).

Outputs (all in the bundled resource dir, next to the linear patterns):
  * one <key>.json per DAG pattern — the (A)-format graph,
    jipipe:node:name STRIPPED (the node display-name policy, 2026-18:
    the shipped graph never pins a label; display falls back to the live
    registry name), locations + compartment key kept (§3.4), group I/O
    wrapper nodes stripped, the item-3c drops removed and keep-largest
    components trimmed;
  * system-templates.json — the 24 linear entries preserved BYTE-STABLE
    plus the 406 surviving "dag-..." entries (name + description +
    functionId + file). Idempotent: re-running rebuilds exactly the dag-*
    entries (the item-3c drops are keyed in dag-pattern-decisions.json).

The "Wiring:" sidecar line is BFS-ordered for DAGs (plan §3.4) and uses
LIVE registry display names (never corpus labels).

Every guard below is fail-closed (process exits non-zero on any
violation); the launcher's SystemPatternTemplatesTest re-verifies the
bundled set end-to-end (deserialization, registry ids, top-level
functions, (name, graph) uniqueness, curation rules).
"""
import hashlib
import html
import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
OUT_DIR = os.path.join(REPO_ROOT,
    "jipipe-core", "src", "main", "resources",
    "org", "hkijena", "jipipe", "plugins", "nodetemplate", "patterns")
SIDECAR = os.path.join(OUT_DIR, "system-templates.json")

EXPECTED_MINED_SHIPPED = 265
EXPECTED_MINED_EXCLUDED = 3
EXPECTED_MINED_SHIP = 262          # 265 - 3 excludes (the 3 remaps ship)
EXPECTED_DERIVED_SHIP = 160        # 26 twins + 134 what-next
EXPECTED_LINEAR = 24  # Option C keep-set (was 64; 32 sub-path + 8 plumbing dropped)
EXPECTED_TOTAL = EXPECTED_MINED_SHIP + EXPECTED_DERIVED_SHIP  # 422 (pre item-3c drops)

# Stale-id remaps (dag-stale-id-plan.json "renamable", successors verified
# 2026-10-19 against the full 2072-node registry + git history):
#   ij1-transform-tile  -> ij1-transform-tile-2d-v2
#     (the -2d variant is @Deprecated @LabelAsJIPipeHidden; the -v2 is the
#      current TileImage2Dv2Algorithm with the same Input/Output slots)
#   import-ome-zarr-from-directory-as-imgplus
#       -> import-ome-zarr-from-zip-directory-as-imgplus
#     (commit fa85690da6 "unified OME ZARR ZIP/directory reader"; the
#      successor is a superset: directory OR zip; same Input/Output slots)
REMAP = {
    "ij1-transform-tile": "ij1-transform-tile-2d-v2",
    "import-ome-zarr-from-directory-as-imgplus":
        "import-ome-zarr-from-zip-directory-as-imgplus",
}

DAG_KEY_PREFIX = "dag-"

# Item 3c (rulings 2026-19/20): the general pattern policies + the per-case
# decisions. No shipped pattern may involve the (to-be-redesigned) JIPipe
# scripting system or loop-plumbing tunnels, and the multi-component harvest
# artifacts are kept only by explicit per-key decision.
PATTERN_DECISIONS = os.path.join(RESEARCH, "dag-pattern-decisions.json")
# Dedicated script-RUNNER node ids (the JIPipe scripting system). Deliberately
# NOT the "(Script)" filter variants (annotate-split-by-annotation-script,
# ij1-roi-filter-statistics-script, ...) — those are annotation/filter nodes,
# and external-imagej-macro is a separate integration (user: out of scope).
SCRIPT_RUNNER_PREFIXES = ("cpython-script", "python-script", "r-script")
SCRIPT_RUNNER_IDS = {
    "define-python-script", "define-r-script",
    "import-python-script", "import-r-script", "tables-from-script",
}
# Loop plumbing: a tunnel entrance/exit is a node-loop connector, not part of
# any pattern (an orphaned tunnel instantiates as a floating node).
TUNNEL_NODE_IDS = {"data-flow-tunnel-1-entrance", "data-flow-tunnel-1-exit"}


def is_script_runner(nid):
    return (nid or "").startswith(SCRIPT_RUNNER_PREFIXES) or nid in SCRIPT_RUNNER_IDS


def connected_components(graph):
    """Undirected connected components of the graph (list of node-key sets)."""
    nodes = graph["nodes"]
    adj = {k: set() for k in nodes}
    for e in graph.get("edges", []):
        s, t = e.get("source-node"), e.get("target-node")
        if s in adj and t in adj:
            adj[s].add(t)
            adj[t].add(s)
    seen = set()
    comps = []
    for n in nodes:
        if n in seen:
            continue
        stack = [n]
        comp = set()
        while stack:
            x = stack.pop()
            if x in comp:
                continue
            comp.add(x)
            seen.add(x)
            stack.extend(adj[x] - comp)
        comps.append(comp)
    return comps


def keep_largest_component(graph, name, key):
    """Trim the graph to its largest connected component (the disconnected
    strays are harvest noise — the mining region captured several unconnected
    node groups). Deterministic: tie-break by the sorted node ids."""
    comps = connected_components(graph)
    comps.sort(key=lambda c: (len(c), tuple(sorted(
        graph["nodes"][x]["jipipe:node-info-id"] for x in c))), reverse=True)
    largest = comps[0]
    graph["nodes"] = {k: v for k, v in graph["nodes"].items() if k in largest}
    graph["edges"] = [e for e in graph.get("edges", [])
                      if e.get("source-node") in largest and e.get("target-node") in largest]
    if not graph["nodes"]:
        fail(f"{name!r} ({key}): keep-largest-component left an empty graph")
    return graph


def apply_pattern_decisions(records, decisions):
    """Apply the item-3c policy + per-case decisions to the (cleaned) records.
    Fail-closed: a multi-component / script / tunnel pattern WITHOUT a
    decision entry means the harvest-artifact set changed — re-review it.
    Returns the surviving records (drops removed, keep-largest trimmed)."""
    kept = []
    dropped = 0
    for key, name, desc, fn, g in records:
        ids = [n["jipipe:node-info-id"] for n in g["nodes"].values()]
        script = [i for i in ids if is_script_runner(i)]
        tunnel = [i for i in ids if i in TUNNEL_NODE_IDS]
        multi = len(connected_components(g)) > 1
        if not (script or tunnel or multi):
            kept.append((key, name, desc, fn, g))
            continue
        d = decisions.get(key)
        if d is None:
            fail(f"{name!r} ({key}): multi-component/script/tunnel pattern "
                 f"without a decision in dag-pattern-decisions.json — the "
                 "harvest-artifact set changed; re-review it")
        if d["action"] == "drop":
            dropped += 1
            continue
        if script or tunnel:
            fail(f"{name!r} ({key}): decision says keep, but the pattern "
                 f"contains a script-runner ({script}) or tunnel ({tunnel}) "
                 "node — those are policy DROPS, not keep-largest")
        keep_largest_component(g, name, key)
        kept.append((key, name, desc, fn, g))
    return kept, dropped


def fail(msg):
    print("ERROR: " + msg, file=sys.stderr)
    sys.exit(1)


def load(name):
    with open(os.path.join(RESEARCH, name), encoding="utf-8") as fh:
        return json.load(fh)


def remap_graph(graph):
    g = json.loads(json.dumps(graph))  # deep copy
    for node in g["nodes"].values():
        nid = node["jipipe:node-info-id"]
        if nid in REMAP:
            node["jipipe:node-info-id"] = REMAP[nid]
    return g


def strip_name(graph):
    """Drop the serialized node display label (jipipe:node:name) — the
    node display-name policy: the shipped graph tracks the live registry
    name instead of pinning a corpus-era label."""
    for node in graph["nodes"].values():
        node.pop("jipipe:node:name", None)
    return graph


# Group I/O "plug" nodes (JIPipeDesktopGroupingPlugin): they only make sense
# INSIDE a group (they define the group's external in/out interfaces). Mined
# out of a group they arrive as top-level nodes and instantiate as floating,
# unusable interface nodes. Skip them (and their edges).
WRAPPER_IO_IDS = {"graph-wrapper:input", "graph-wrapper:output"}
# The flattened node key of the JIPipeParameterSlotAlgorithm "External
# parameters" toggle (verified 2026-10-19: setting it to true in the node
# JSON creates the {Parameters} input slot on the template-deserialization
# path — see the throwaway probe).
PARAM_SLOT_FLAG = "jipipe:parameter-slot-algorithm/has-parameter-slot"
PARAM_SLOT = "{Parameters}"


def clean_graph(graph):
    """Make a mined graph ship-ready (applied at generation, research
    artifacts untouched):
      (3a) strip group I/O wrapper nodes (graph-wrapper:input/output) and
           their edges — they are leftover group plumbing that instantiates
           as floating interfaces;
      (3b) enable the "External" parameter slot (has-parameter-slot=true) on
           every node that an edge targets at its {Parameters} slot — the
           corpus dumper is structure-only and dropped the per-node flag, so
           the slot is never created and the wiring dangles (the parameters
           never connect).
    Fail-closed: after (3a) no wrapper id may remain, and after (3b) no edge
    may still reference a {Parameters} slot whose target lacks the flag."""
    nodes = graph["nodes"]
    kept = {k: v for k, v in nodes.items()
            if v.get("jipipe:node-info-id") not in WRAPPER_IO_IDS}
    if len(kept) != len(nodes):
        graph["nodes"] = kept
        graph["edges"] = [e for e in graph["edges"]
                          if e.get("source-node") in kept and e.get("target-node") in kept]
    # (3b) the {Parameters} edge targets need the External slot enabled
    for e in graph["edges"]:
        if e.get("target-slot") == PARAM_SLOT:
            target = graph["nodes"].get(e.get("target-node"))
            if target is not None:
                target[PARAM_SLOT_FLAG] = True
    # fail-closed guards
    for v in graph["nodes"].values():
        if v.get("jipipe:node-info-id") in WRAPPER_IO_IDS:
            fail(f"wrapper I/O node survived clean_graph: "
                 f"{v.get('jipipe:node-info-id')}")
    for e in graph["edges"]:
        if e.get("target-slot") == PARAM_SLOT:
            target = graph["nodes"].get(e.get("target-node"))
            if target is None or target.get(PARAM_SLOT_FLAG) is not True:
                fail(f"{PARAM_SLOT} edge to {e.get('target-node')} without the "
                     f"External parameter-slot flag — the wiring would dangle")
    return graph


def topo_order(graph):
    """Deterministic BFS-from-sources node-key order (same rule as the
    (B) canonical text: alphabetical among ready nodes, tie-break by
    node-info-id; a cycle-break fallback — the (A) format guarantees
    DAGs)."""
    nodes = graph["nodes"]
    indeg = {k: 0 for k in nodes}
    succ = {k: [] for k in nodes}
    for e in graph.get("edges", []):
        s, t = e["source-node"], e["target-node"]
        if s in succ and t in indeg:
            succ[s].append(t)
            indeg[t] += 1
    order = []
    remaining = set(nodes)
    while remaining:
        ready = sorted(
            (k for k in remaining if indeg[k] == 0),
            key=lambda k: (nodes[k]["jipipe:node-info-id"], k))
        if not ready:
            ready = [max(remaining)]  # cycle break (unreachable for (A))
        for k in ready:
            order.append(k)
            remaining.discard(k)
            for s in succ[k]:
                indeg[s] -= 1
    return order


def wiring_line(graph, registry):
    """The plan §3.4 'Wiring:' sidecar line, BFS-ordered, LIVE registry
    display names (never corpus labels — the shipped graph carries no
    jipipe:node:name)."""
    pos = {k: i for i, k in enumerate(topo_order(graph))}
    im = {k: v["jipipe:node-info-id"] for k, v in graph["nodes"].items()}
    name_of = {
        v["jipipe:node-info-id"]:
            registry.get(v["jipipe:node-info-id"], {}).get("name")
            or v["jipipe:node-info-id"]
        for v in graph["nodes"].values()}
    inv = {v: k for k, v in im.items()}
    edges = sorted(
        ((im[e["source-node"]], e.get("source-slot", ""),
          im[e["target-node"]], e.get("target-slot", ""))
         for e in graph.get("edges", [])),
        key=lambda t: (pos[inv[t[0]]], pos[inv[t[2]]], t[1], t[3]))
    if not edges:
        return None
    wire = "; ".join(
        f"{name_of[s]}.{ss} \u2192 {name_of[t]}.{ts}" for s, ss, t, ts in edges)
    return "Wiring: " + wire + "."


def structural_key(graph):
    """Deterministic unique sidecar/file key material: the label-agnostic
    structural signature (node-info-id multiset + canonical edge
    multiset), hashed. Uniqueness across the 422 is asserted below."""
    node_ids = tuple(sorted(
        n["jipipe:node-info-id"] for n in graph["nodes"].values()))
    edges = tuple(sorted(
        (e["source-node"], e.get("source-slot", ""), e["target-node"],
         e.get("target-slot", ""))
        for e in graph.get("edges", [])))
    blob = json.dumps([list(node_ids), list(edges)], ensure_ascii=False)
    return hashlib.sha1(blob.encode("utf-8")).hexdigest()[:12]


def main():
    registry = {n["id"]: n for n in load("registry.json")["nodes"]}
    top_functions = {
        f["id"] for f in load("registry.json").get("functions", [])
        if f.get("parent") is None}
    judgments = load("dag-judgments.json")
    clusters = {c["clusterId"]: c
                for c in load("dag-clusters.json")["clusters"]}
    derived = load("dag-derived.json")
    plan = load("dag-stale-id-plan.json")
    excluded = set(plan["exclude"])
    if len(excluded) != EXPECTED_MINED_EXCLUDED:
        fail(f"expected {EXPECTED_MINED_EXCLUDED} excluded mined patterns, "
             f"the plan lists {len(excluded)}: {sorted(excluded)}")

    # ---- collect the 422 (name, description, functionId, graph, key) ----
    records = []  # (sidecar_key, name, description, functionId, graph)
    seen_keys = {}
    mined = [e for e in judgments["entries"] if e.get("ship")]
    if len(mined) != EXPECTED_MINED_SHIPPED:
        fail(f"expected {EXPECTED_MINED_SHIPPED} shipped mined patterns, "
             f"got {len(mined)}")
    for e in mined:
        cid = e["clusterId"]
        if cid in excluded:
            continue
        graph = remap_graph(
            clusters[cid]["representativeCandidate"]["graph"])
        key = f"{DAG_KEY_PREFIX}mined-{cid}"
        if key in seen_keys:
            fail(f"duplicate sidecar key {key}")
        seen_keys[key] = cid
        records.append((key, e["name"], e["description"], e["functionId"],
                        graph))
    if len(records) != EXPECTED_MINED_SHIP:
        fail(f"mined shippable count mismatch: {len(records)} (expected "
             f"{EXPECTED_MINED_SHIP} = {EXPECTED_MINED_SHIPPED} shipped - "
             f"{EXPECTED_MINED_EXCLUDED} excluded)")

    dship = [e for e in derived["entries"] if e.get("ship")]
    if len(dship) != EXPECTED_DERIVED_SHIP:
        fail(f"expected {EXPECTED_DERIVED_SHIP} shipped derived patterns, "
             f"got {len(dship)}")
    for e in dship:
        dcid = e.get("clusterId")
        if not dcid:
            fail("derived entry without a clusterId")
        # derived clusterIds are NOT unique (a source can yield several
        # what-next chains); the structural hash makes the key unique.
        key = f"{DAG_KEY_PREFIX}derived-{dcid}-{structural_key(e['state']['pattern']['graph'])}"
        if key in seen_keys:
            fail(f"duplicate sidecar key {key}")
        seen_keys[key] = dcid
        records.append((key, e["name"], e["description"], e["functionId"],
                        e["state"]["pattern"]["graph"]))
    if len(records) != EXPECTED_TOTAL:
        fail(f"expected {EXPECTED_TOTAL} DAG patterns, built {len(records)}")

    # ---- clean every mined graph for shipping (3a + 3b) ----
    # Applied to the in-memory records (research artifacts untouched, and
    # idempotent across runs). MUST run before the guards below so (i)–(vii)
    # validate the SHIPPED shape, and so the (viii)/(ix) backstops below can
    # fail closed on a regression of clean_graph itself.
    for i, (key, name, desc, fn, g) in enumerate(records):
        records[i] = (key, name, desc, fn, clean_graph(g))

    # ---- item 3c: the general pattern policies + per-case decisions ----
    # Runs AFTER clean_graph (the wrapper-strip changes connectivity, so the
    # multi-component classification is computed on the cleaned graph) and
    # BEFORE the guards (so (i)–(vii) validate the final shipped set, and the
    # identity check re-runs on the trimmed id-multisets). Fail-closed: a
    # script-runner / tunnel / multi-component pattern without a decision
    # entry means the harvest-artifact set changed — re-review it.
    with open(PATTERN_DECISIONS, encoding="utf-8") as fh:
        decisions = json.load(fh)["decisions"]
    records, decisions_dropped = apply_pattern_decisions(records, decisions)

    # The shipped DAG set is the 422 minus the 16 policy/artifact drops
    # (5 cpython-script + 2 tunnel + 9 degenerate multi-component). Pin the
    # number so a silent re-add of a dropped pattern fails the build.
    EXPECTED_DAG_SHIP = EXPECTED_TOTAL - 16
    if len(records) != EXPECTED_DAG_SHIP:
        fail(f"expected {EXPECTED_DAG_SHIP} shipped DAG patterns after the "
             f"item-3c drops ({EXPECTED_TOTAL} - 16), built {len(records)} — "
             "the drop set changed; re-review dag-pattern-decisions.json")

    # ---- fail-closed guards over the 422 ----
    # (i) every node id registered (in the FULL 2072-node registry; the
    #     remaps were applied above, the 3 excludes dropped)
    for key, name, _desc, _fn, g in records:
        for nid in (n["jipipe:node-info-id"] for n in g["nodes"].values()):
            if nid not in registry:
                fail(f"{name!r} ({key}): unregistered node id {nid!r}")
    # (ii) the stale ids are gone. (They are unregistered in the 2072
    #     registry, so guard (i) above would already fail if any survived —
    #     this is the explicit, self-documenting version of the same check:
    #     a missing remap would leave an unregistered stale id behind.)
    all_ids = [n["jipipe:node-info-id"]
               for _k, _n, _d, _f, g in records
               for n in g["nodes"].values()]
    for stale in REMAP:
        if stale in all_ids:
            fail(f"stale id {stale!r} survived the remap (clusters 0133/"
                 f"0242/0318 must reference the verified successors)")
    # (iii) functionId is a registered TOP-LEVEL branch (the test pins
    #     fn.getParent() == null; the icon/hue is derived from it)
    for key, name, _d, fn, _g in records:
        if fn not in top_functions:
            fail(f"{name!r} ({key}): functionId {fn!r} is not a "
                 f"registered top-level branch")
    # (iv) §3.4 locations: every node carries location/<its own
    #     compartment>/x AND /y
    for key, name, _d, _f, g in records:
        for k, n in g["nodes"].items():
            comp = n.get("jipipe:graph-compartment")
            md = n.get("jipipe:metadata-v1") or {}
            if not comp or f"location/{comp}/x" not in md \
                    or f"location/{comp}/y" not in md:
                fail(f"{name!r} ({key}): node {k} lacks its §3.4 location "
                     f"(compartment {comp!r}, metadata keys {sorted(md)})")
    # (v) names: non-empty; no collision with the 64 linear patterns;
    #     (name, node-info-id multiset) unique across the 422 (the test's
    #     identity key — 13 DAG names are shared by DIFFERENT graphs).
    #     Separate the linear (non-dag-*) entries from any prior dag-* run so
    #     the guard is idempotent: on every run the linear set is exactly 64.
    with open(SIDECAR, encoding="utf-8") as fh:
        sidecar = json.load(fh)
    linear = {k: v for k, v in sidecar.items()
              if not k.startswith(DAG_KEY_PREFIX)}
    if len(linear) != EXPECTED_LINEAR:
        fail(f"sidecar expected to hold the {EXPECTED_LINEAR} linear "
             f"patterns (non-dag-* entries) before the DAG merge, holds "
             f"{len(linear)}")
    linear_names = {v.get("name") for v in linear.values()}
    identity = {}
    for key, name, _d, _f, g in records:
        if not name or not name.strip():
            fail(f"{key}: empty pattern name")
        if name in linear_names:
            fail(f"DAG name {name!r} collides with a linear pattern name")
        ids = tuple(sorted(n["jipipe:node-info-id"]
                           for n in g["nodes"].values()))
        if (name, ids) in identity:
            fail(f"(name, node-ids) collision: {name!r} shared by "
                 f"{identity[(name, ids)]} and {key}")
        identity[(name, ids)] = key
    # (vi) curation rules (the test pins these for every template):
    #     no compartment sink, no statistical fan name
    for key, name, _d, _f, g in records:
        if any(n["jipipe:node-info-id"] == "jipipe:compartment-output"
               for n in g["nodes"].values()):
            fail(f"{name!r} ({key}): contains a compartment-output sink")
        if "fans in from" in name or "fans out to" in name:
            fail(f"{name!r} ({key}): statistical fan name")
    # (vii) edge endpoints exist in the graph
    for key, name, _d, _f, g in records:
        for e in g.get("edges", []):
            if e["source-node"] not in g["nodes"] \
                    or e["target-node"] not in g["nodes"]:
                fail(f"{name!r} ({key}): dangling edge endpoint")

    # ---- write the DAG graph resources (422 mined, minus the item-3c drops) ----
    # Sweep stale dag-* files from a previous run (idempotent rebuild);
    # the linear *.json are untouched.
    shipped_files = {key + ".json" for key, *_ in records}
    for existing in sorted(os.listdir(OUT_DIR)):
        if existing.endswith(".json") and existing != "system-templates.json" \
                and existing.startswith(DAG_KEY_PREFIX) \
                and existing not in shipped_files:
            os.remove(os.path.join(OUT_DIR, existing))
    for key, name, desc, _fn, g in records:
        g = strip_name(g)
        if any("jipipe:node:name" in n for n in g["nodes"].values()):
            fail(f"{name!r}: jipipe:node:name survived the strip")
        with open(os.path.join(OUT_DIR, key + ".json"), "w",
                  encoding="utf-8") as fh:
            fh.write(json.dumps(g, ensure_ascii=False,
                                separators=(",", ":")) + "\n")

    # ---- merge the sidecar (linear 64 byte-stable + 422 dag-*) ----
    # Reset to the linear entries only (any prior dag-* run is dropped), then
    # add the fresh 422. Idempotent: the linear subset is never touched.
    sidecar = dict(linear)
    for key, name, desc, fn, g in records:
        lines = []
        base = (desc or "").strip()
        if base:
            lines.append(base if base.endswith(".") else base + ".")
        wl = wiring_line(g, registry)
        if wl:
            lines.append(wl)
        sidecar[key] = {
            "description": "<br>".join(html.escape(l, quote=False)
                                       for l in lines),
            "file": key + ".json",
            "functionId": fn,
            "name": name,
        }
    with open(SIDECAR, "w", encoding="utf-8") as fh:
        json.dump(sidecar, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")

    # ---- summary ----
    from collections import Counter
    dist = Counter(r[3] for r in records)
    print("=== DAG system template generation (step D) ===")
    print(f"mined shipped    : {EXPECTED_MINED_SHIP} "
          f"({EXPECTED_MINED_SHIPPED} - {EXPECTED_MINED_EXCLUDED} excluded; "
          f"3 stale-id remaps applied)")
    print(f"derived shipped  : {EXPECTED_DERIVED_SHIP} "
          f"(26 twins + 134 what-next, incl. 6 cLij2 re-sweep chains)")
    print(f"DAG patterns     : {len(records)}  ->  {OUT_DIR}")
    print(f"sidecar entries  : {len(sidecar)} "
          f"({EXPECTED_LINEAR} linear + {len(records)} dag-*)  ->  {SIDECAR}")
    print("function distribution:")
    for fid, cnt in dist.most_common():
        print(f"   {fid}: {cnt}")
    print("OK")


if __name__ == "__main__":
    main()
