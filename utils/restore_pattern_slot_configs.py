#!/usr/bin/env python3
"""B1 slot-configuration restore pass (work item 1324, global-constraints B1).

The minimal bundled graph format stripped every node's
``jipipe:slot-configuration`` AND the slot-driving parameters, so ~66
patterns reference slots that do not exist on a fresh node (their edges drop
on load and the nodes float). This pass restores FULL FIDELITY from the
source corpus (the user's ruling): for each shipped DAG/linear file in
``OUT_DIR``, for each node whose edge-referenced (REQUIRED) slots exceed its
registry-declared STATIC slots, find the deterministic corpus instance of the
same node type whose ``jipipe:slot-configuration`` covers the required set and
copy that config + its slot-driving parameters VERBATIM.

Why a dedicated pass: the DAG/linear generators work from research artifacts
that do NOT carry slot configs; the corpus (``/data/JIPipe``, READ-ONLY) is
reachable only here.

The public API (required by the global-constraints "Restore-script API" bullet
and the Ruling 10 note - both are used by the generators' width maps):

  * ``analyze(out_dir, corpus_root, registry) -> dict``  - PURE (no writes).
    Returns, per file, per node key, the EXTRA input/output slot counts the
    restore would add (the node's required set minus its registry STATIC slots
    and any slots its existing ``jipipe:slot-configuration`` already covers).
    The generators build their width maps from this.
  * ``restore(out_dir, corpus_root, registry)``  - the patching pass. ``main``
    runs it. Idempotent: a node whose required set is already covered by its
    existing config / static slots is skipped, so re-running on already-patched
    files is a no-op.

Matching rule (deterministic, the global-constraints B1 rule):
  * A node's REQUIRED set = every slot name on a shipped edge endpoint for that
    node (source-end slots, target-end slots), EXCLUDING the ``{Parameters}
    external-parameter slot (that slot is created by the generator's existing
    (3b) ``has-parameter-slot`` flag on load, not by a slot config - it is the
    (3b) pass's job, per the copy exclusions below).
  * A node's STATIC set = the registry ``inputSlots``/``outputSlots`` names
    (generator-side approximation; the launcher edge-survival test is the
    authoritative oracle). The proxy is deliberately conservative (it may
    over-flag - restoring a config whose slots the JVM would create anyway is
    harmless); it never under-flags a needed restore.
  * A node is AFFECTED iff REQUIRED - STATIC - <existing config slots> is
    non-empty on either side.
  * MATCH = a corpus instance (any project under ``corpusRoot``, indexed by
    node type) whose ``jipipe:slot-configuration`` covers the required set
    (source-end required slots ⊆ the instance's ``output`` map; target-end
    required slots ⊆ its ``input`` map). Match by (type, required-slot-set) -
    NEVER by the shipped compartment UUID (the mined compartment UUIDs are
    synthetic).
  * TIE-BREAK (determinism) = lexicographically-smallest (corpus-relative
    project path, compartment key, node UUID).

Copy (full fidelity): the instance's full ``jipipe:slot-configuration`` (both
sides - real data types), plus the instance's top-level non-``jipipe:``
parameter keys VERBATIM (the slot-driving parameters; without them the
restored slots are never created). EXCLUDED from the copy: everything under
``jipipe:`` EXCEPT ``jipipe:slot-configuration`` itself.

Fallback (only when no corpus instance matches):
  * dynamic-slot node types (the ``JIPipeIOSlotConfiguration`` ids) → emit
    ``jipipe:data`` mirror slots for the required names (contiguous ``Data 1..N``
    fill for the "Data k" pattern - a node referencing only "Data 2" gets BOTH
    "Data 1" and "Data 2");
  * static types with no corpus match → ``fail(...)`` the build (never ship a
    dangling edge silently). The one pre-verified case is handled by the
    documented per-file EXCEPTION TABLE below, not a silent skip.

Idempotent + deterministic; prints a per-pattern change report.
"""

from __future__ import annotations

import json
import os
import sys

from sugiyama_layout import width_cells_for_slot_count

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
CANDIDATES = os.path.join(RESEARCH, "dag-candidates.json")
REGISTRY_FILE = os.path.join(RESEARCH, "registry.json")
#: The JVM-measured FRESH-node slot set for every shipped node type
#: (produced by jipipe-launcher's FreshNodeSlotProbeTest, committed here).
#: The registry.json inputSlots/outputSlots are the @AddJIPipe*Slot
#: ANNOTATION slots, but a node whose constructor / parameter-gating logic
#: adds or removes slots (e.g. the cellpose inference nodes keep only the
#: parameter-enabled outputs - default "ROI") has a DIFFERENT slot set on a
#: fresh instance. The restore pass must match the required slot set against
#: the FRESH-node set (the launcher edge test's world view), so the effective
#: static slots prefer this measurement over the annotation list.
FRESH_SLOTS_FILE = os.path.join(REPO_ROOT, "utils", "fresh-node-slots.json")
DEFAULT_OUT_DIR = os.path.join(
    REPO_ROOT, "jipipe-core", "src", "main", "resources",
    "org", "hkijena", "jipipe", "plugins", "nodetemplate", "patterns")

#: Mirror of the mining pipeline's excluded corpus prefixes (read-only).
_EXCLUDED_PREFIXES = ("fiji", "jipipe-release", "tmp", "temp", "documentation")

#: The ``{Parameters}`` external-parameter slot: created by the generator's
#: existing (3b) has-parameter-slot flag on load, NOT by a slot config.
PARAM_SLOT = "{Parameters}"

#: The ``JIPipeIOSlotConfiguration`` node-type ids (the DYNAMIC slot types -
#: these create user-defined slots, so with no corpus match they fall back to
#: generic ``jipipe:data`` slots rather than failing the build). Verified
#: 2026-10-21 against the registry (the 3 iteration-checker nodes carry
#: static slots in the registry, so they are NOT here and would hard-fail if
#: ever affected).
DYNAMIC_SLOT_TYPE_IDS = frozenset({
    "annotation-merge",
    "ij1-transform-equalize-dimensions-max-io",
    "io-interface",
    "graph-wrapper:input",
    "graph-wrapper:output",
    "loop:start",
    "loop:end",
    "jipipe:compartment-output",
})

#: Documented per-file EXCEPTION TABLE for the one pre-verified restore
#: hard-fail (NOT a silent skip). Ruling: ``dag-derived-...-0096...json``'s
#: ``ij1-roi-count`` node has a required INPUT slot named ``Roi2dListData``
#: (a class name) that no corpus instance covers, because the node's input is
#: declared in code with an EMPTY slot name (registry: inputSlots [{class:
#: Roi2dListData, name: ""}]). The edge references the class name, so the only
#: faithful restore is to emit a ``jipipe:slot-configuration`` input named
#: ``Roi2dListData`` of type ``imagej-roi`` (the registry ``dataType`` for
#: ``Roi2dListData``) for THIS node only. Keyed by (file, node-type).
EXCEPTIONS = {
    ("dag-derived-derived-wn-cluster-0096-27-3250df34394f.json",
     "ij1-roi-count"): {
        "input": {"Roi2dListData": "imagej-roi"},
        "output": {},
    },
}

#: The generic data type id (for the dynamic fallback's ``jipipe:data`` slots).
GENERIC_DATA_TYPE = "jipipe:data"


def fail(msg):
    print("ERROR: " + msg, file=sys.stderr)
    sys.exit(1)


def _slot_entry(name, data_type, slot_type):
    """A generic ``JIPipeDataSlotInfo``-shaped entry for a fallback slot."""
    return {
        "slot-data-type": data_type,
        "slot-type": slot_type,
        "name": name,
        "custom-name": None,
        "description": "",
        "is-virtual": False,
        "save-outputs": True,
        "is-optional": False,
    }


def _required_sets(graph):
    """{node key: (required_input_set, required_output_set)} from the file's
    edges. ``{Parameters}`` is excluded (it is the (3b) flag's slot)."""
    nodes = graph["nodes"]
    req_in = {}
    req_out = {}
    for e in graph.get("edges", []):
        s, t = e.get("source-node"), e.get("target-node")
        ss = e.get("source-slot", "")
        ts = e.get("target-slot", "")
        if s in nodes and ss and ss != PARAM_SLOT:
            req_out.setdefault(s, set()).add(ss)
        if t in nodes and ts and ts != PARAM_SLOT:
            req_in.setdefault(t, set()).add(ts)
    return {k: (req_in.get(k, set()), req_out.get(k, set())) for k in nodes}


_FRESH_SLOTS = None


def _load_fresh_slots():
    """The committed JVM-measured fresh-node slot set (node id ->
    {"inputs": [...], "outputs": [...]}) or None if the artifact is absent
    (in which case the registry annotation list is used)."""
    global _FRESH_SLOTS
    if _FRESH_SLOTS is None:
        if os.path.exists(FRESH_SLOTS_FILE):
            with open(FRESH_SLOTS_FILE, encoding="utf-8") as fh:
                _FRESH_SLOTS = json.load(fh)
        else:
            _FRESH_SLOTS = {}
    return _FRESH_SLOTS


def _static_slots(nid, registry):
    """The node type's STATIC input/output slot NAME sets. The FRESH-node
    slot set (JVM-measured via the exact load path, from
    ``fresh-node-slots.json``) is AUTHORITATIVE when present: it is exactly
    the slot set a fresh instance creates on load, which is the world the
    launcher edge test sees. Types marked ``uninstantiable`` (the
    version-gated cLij2 kernels, dropped on load - their edges are never
    counted by the edge test) and any type absent from the artifact fall
    back to the registry annotation list (``inputSlots``/``outputSlots``),
    which can differ from the fresh set for parameter-gated nodes. Excludes
    the ``{Parameters}`` flag slot and empty-name class slots."""
    fresh = _load_fresh_slots().get(nid)
    if isinstance(fresh, dict) and not fresh.get("uninstantiable"):
        ins = {s for s in fresh.get("inputs", []) if s}
        outs = {s for s in fresh.get("outputs", []) if s}
        return ins - {PARAM_SLOT}, outs - {PARAM_SLOT}
    info = registry.get(nid) or {}
    ins = {s.get("name", "") for s in info.get("inputSlots", [])
           if s.get("name", "")}
    outs = {s.get("name", "") for s in info.get("outputSlots", [])
            if s.get("name", "")}
    return ins - {PARAM_SLOT}, outs - {PARAM_SLOT}


def _existing_config_slots(node):
    """The node's existing ``jipipe:slot-configuration`` input/output names
    (empty if absent)."""
    sc = node.get("jipipe:slot-configuration")
    if not isinstance(sc, dict):
        return set(), set()
    return (set((sc.get("input") or {}).keys()),
            set((sc.get("output") or {}).keys()))


def _is_affected(nid, req_in, req_out, registry):
    """True iff the node's required set exceeds its STATIC slots on either
    side (the existing config is NOT considered here - this is the
    proxy/analyze notion, which drives the width map)."""
    s_in, s_out = _static_slots(nid, registry)
    return bool((req_in - s_in) or (req_out - s_out))


def _covered_by(node, req_in, req_out, registry):
    """True iff the node's required set is fully covered by its existing
    ``jipipe:slot-configuration`` (if present) OR its static slots - the
    restore pass's idempotence predicate."""
    sc = node.get("jipipe:slot-configuration")
    if isinstance(sc, dict):
        cfg_in = set((sc.get("input") or {}).keys())
        cfg_out = set((sc.get("output") or {}).keys())
        s_in, s_out = _static_slots(
            node["jipipe:node-info-id"], registry)
        return (req_in <= (cfg_in | s_in)) and (req_out <= (cfg_out | s_out))
    s_in, s_out = _static_slots(node["jipipe:node-info-id"], registry)
    return (req_in <= s_in) and (req_out <= s_out)


def _corpus_files(corpus_root):
    for dirpath, dirnames, filenames in os.walk(corpus_root):
        dirnames[:] = sorted(
            d for d in dirnames
            if not d.lower().startswith((".git",) + _EXCLUDED_PREFIXES))
        for fn in sorted(filenames):
            if fn.endswith(".jip"):
                yield os.path.relpath(os.path.join(dirpath, fn), corpus_root)


def build_corpus_index(corpus_root):
    """Index the READ-ONLY corpus by node type ->
    [(project, compartment, uuid, slot_config, node)] for every node that
    carries a ``jipipe:slot-configuration``. Sorted for the deterministic
    tie-break (lex-smallest project, compartment, uuid)."""
    index = {}
    for rel in _corpus_files(corpus_root):
        path = os.path.join(corpus_root, rel)
        try:
            with open(path, encoding="utf-8") as fh:
                project = json.load(fh)
        except Exception:
            continue
        for uuid, node in sorted(
                (project.get("graph") or {}).get("nodes", {}).items()):
            sc = node.get("jipipe:slot-configuration")
            if not isinstance(sc, dict):
                continue
            nid = node.get("jipipe:node-info-id")
            if not nid:
                continue
            index.setdefault(nid, []).append(
                (rel, node.get("jipipe:graph-compartment") or "", uuid,
                 sc, node))
    for nid in index:
        index[nid].sort(key=lambda t: (t[0], t[1], t[2]))
    return index


def _match_instance(nid, req_in, req_out, corpus_index):
    """The deterministic corpus instance whose slot config covers the required
    set, or None."""
    for proj, comp, uuid, sc, node in corpus_index.get(nid, []):
        out_names = set((sc.get("output") or {}).keys())
        in_names = set((sc.get("input") or {}).keys())
        if req_out <= out_names and req_in <= in_names:
            return proj, comp, uuid, sc, node
    return None


def _drive_params(node):
    """The instance's top-level non-``jipipe:`` parameter keys (the
    slot-driving parameters) - copied VERBATIM. Everything under ``jipipe:``
    except the slot config itself is excluded (the (3b) ``{Parameters}`` flag
    is the generator's existing pass's job)."""
    return {k: v for k, v in node.items()
            if not k.startswith("jipipe:")}


def _data_fill(required):
    """Contiguous ``Data 1..N`` fill for a required set of "Data k" names."""
    nums = []
    for name in required:
        if name == "Data":
            nums.append(1)
        elif name.startswith("Data ") and name[5:].isdigit():
            nums.append(int(name[5:]))
    if not nums:
        return list(required)
    n = max(nums)
    return (["Data"] + ["Data %d" % i for i in range(2, n + 1)]
            if n >= 2 else ["Data"])


def _fallback_config(nid, req_in, req_out):
    """The generic ``jipipe:data`` fallback for a dynamic type with no corpus
    match (contiguous Data 1..N fill)."""
    cfg_in = {name: _slot_entry(name, GENERIC_DATA_TYPE, "Input")
              for name in sorted(_data_fill(req_in))}
    cfg_out = {name: _slot_entry(name, GENERIC_DATA_TYPE, "Output")
               for name in sorted(_data_fill(req_out))}
    return {"input": cfg_in, "output": cfg_out}, {}


def _shipped_files(out_dir):
    return sorted(f for f in os.listdir(out_dir)
                  if f.endswith(".json") and f != "system-templates.json")


def extras_for_graph(graph, registry):
    """The per-node EXTRA input/output slot counts (required minus STATIC
    slots - the existing config is NOT subtracted, so the result is stable
    for the width map regardless of whether the file has been patched).
    Returns ``{node key: {"input": int, "output": int, "node-info-id": str}}``
    for AFFECTED nodes only. This is the file-local core of :func:`analyze`
    (the Ruling 10 "Restore-script API"); the generators build their width
    maps from it (via :func:`width_map_for_graph`)."""
    req = _required_sets(graph)
    per_node = {}
    for k, node in graph["nodes"].items():
        nid = node["jipipe:node-info-id"]
        req_in, req_out = req[k]
        if not _is_affected(nid, req_in, req_out, registry):
            continue
        s_in, s_out = _static_slots(nid, registry)
        per_node[k] = {
            "input": len(req_in - s_in),
            "output": len(req_out - s_out),
            "node-info-id": nid,
        }
    return per_node


def analyze(out_dir, corpus_root, registry):
    """PURE (no writes): per file, per node key, the EXTRA input/output slot
    counts the restore would add. See :func:`extras_for_graph` for the
    per-node rule.

    Returns ``{file: {node key: {"input": int, "output": int,
    "node-info-id": str}}}`` for AFFECTED nodes only."""
    if not os.path.isdir(corpus_root):
        fail(f"corpus root does not exist: {corpus_root}")
    result = {}
    for f in _shipped_files(out_dir):
        with open(os.path.join(out_dir, f), encoding="utf-8") as fh:
            graph = json.load(fh)
        per_node = extras_for_graph(graph, registry)
        if per_node:
            result[f] = per_node
    return result


def width_map_for_graph(graph, node_extras, registry):
    """The FINAL per-node width map (node key -> grid cells) for laying out
    ``graph`` (Ruling 12 / Global-Constraints width rule 5):

        static_slots(registry) + analyze-reported extras  ->  width_cells.

    ``node_extras`` is the per-node mapping for ONE file exactly as
    :func:`analyze` returns it (``{node key: {"input": int, "output": int}}``)
    - typically ``analyze(out_dir, corpus_root, registry)[this_file]``. Nodes
    with no entry are un-restored and use their static slot count. A node
    that already carries a ``jipipe:slot-configuration`` counts that config's
    NON-static slots (static ones are already in the registry count).
    """
    widths = {}
    for k, node in graph["nodes"].items():
        static_in, static_out = _static_slots(
            node["jipipe:node-info-id"], registry)
        extra = node_extras.get(k, {})
        total = (len(static_in) + extra.get("input", 0)) \
            + (len(static_out) + extra.get("output", 0))
        sc = node.get("jipipe:slot-configuration")
        if isinstance(sc, dict):
            cfg_in = (sc.get("input") or {})
            cfg_out = (sc.get("output") or {})
            total += len(set(cfg_in) - static_in)
            total += len(set(cfg_out) - static_out)
        widths[k] = width_cells_for_slot_count(total)
    return widths


def restore(out_dir, corpus_root, registry):
    """The patching pass. Returns the per-pattern change report
    ``[(file, node key, node-type, source, extra_in, extra_out, slots_added)]``
    (sorted). Idempotent: an already-covered node is skipped."""
    corpus_index = build_corpus_index(corpus_root)
    report = []
    for f in _shipped_files(out_dir):
        path = os.path.join(out_dir, f)
        with open(path, encoding="utf-8") as fh:
            graph = json.load(fh)
        req = _required_sets(graph)
        changed = False
        for k in sorted(graph["nodes"]):
            node = graph["nodes"][k]
            nid = node["jipipe:node-info-id"]
            req_in, req_out = req[k]
            if _covered_by(node, req_in, req_out, registry):
                continue  # already covered (idempotent) or never affected
            m = _match_instance(nid, req_in, req_out, corpus_index)
            if m is not None:
                proj, comp, uuid, sc, src_node = m
                config = json.loads(json.dumps(sc))
                params = _drive_params(src_node)
                source = f"{proj}:{comp}:{uuid}"
            elif (f, nid) in EXCEPTIONS:
                spec = EXCEPTIONS[(f, nid)]
                config = {
                    "input": {name: _slot_entry(name, dt, "Input")
                              for name, dt in sorted(spec["input"].items())},
                    "output": {name: _slot_entry(name, dt, "Output")
                               for name, dt in sorted(spec["output"].items())},
                }
                params = {}
                source = f"EXCEPTION:{f}:{nid}"
            elif nid in DYNAMIC_SLOT_TYPE_IDS:
                config, params = _fallback_config(nid, req_in, req_out)
                source = "FALLBACK:generic-data"
            else:
                fail(f"{f}: node {k} ({nid}) needs restore for slots "
                     f"in={sorted(req_in)} out={sorted(req_out)} but NO "
                     "corpus instance covers it and it is a STATIC type - "
                     "refusing to ship a dangling edge (add a documented "
                     "EXCEPTIONS entry or a corpus match)")
            node["jipipe:slot-configuration"] = config
            for pk, pv in params.items():
                node[pk] = pv
            s_in, s_out = _static_slots(nid, registry)
            report.append((f, k, nid, source,
                           len(req_in - s_in), len(req_out - s_out),
                           sorted((req_in - s_in) | (req_out - s_out))))
            changed = True
        if changed:
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(json.dumps(graph, ensure_ascii=False,
                                    separators=(",", ":")) + "\n")
    return report


def main(argv=None):
    argv = argv if argv is not None else sys.argv[1:]
    out_dir = DEFAULT_OUT_DIR
    corpus_root = None
    i = 0
    while i < len(argv):
        a = argv[i]
        if a in ("--out",):
            out_dir = os.path.abspath(argv[i + 1])
            i += 2
            continue
        if a in ("--corpus",):
            corpus_root = os.path.abspath(argv[i + 1])
            i += 2
            continue
        i += 1
    if corpus_root is None:
        with open(CANDIDATES, encoding="utf-8") as fh:
            corpus_root = os.path.abspath(json.load(fh)["corpusRoot"])
    with open(REGISTRY_FILE, encoding="utf-8") as fh:
        registry = {n["id"]: n for n in json.load(fh)["nodes"]}
    report = restore(out_dir, corpus_root, registry)
    print("=== B1 slot-configuration restore ===")
    print(f"out_dir  : {out_dir}")
    print(f"corpus   : {corpus_root} (read-only)")
    files = sorted({r[0] for r in report})
    print(f"nodes restored : {len(report)}")
    print(f"files touched  : {len(files)}")
    for f, k, nid, source, ei, eo, slots in report:
        print(f"   {f} :: {k} [{nid}] <- {source} "
              f"(+{ei}in/+{eo}out: {', '.join(slots)})")
    print("OK" if report else "OK (0 changes - idempotent no-op)")


if __name__ == "__main__":
    main()
