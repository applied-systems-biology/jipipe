#!/usr/bin/env python3
"""Tests for the DAG pattern harvest (plan step A, utils/mine-dag-patterns.py).

The harvest is deterministic and has no AI: it reads real ``.jip`` projects
and emits authored-grouping candidates (group nodes, canvas-note sections,
named compartments, geometric clusters) as minimal ``JIPipeGraph``-shaped
subgraphs with provenance, support counts and node locations.

Run standalone:

    python3 utils/mine_dag_patterns_tests.py

or via the harvester's selftest subcommand:

    python3 utils/mine-dag-patterns.py selftest

The fixtures are synthetic ``.jip`` files (flat-graph format 6.0.0) built in
a temp directory, so the suite is hermetic. One test (flagship) additionally
mines ``/data/JIPipe/Cellpose training example`` if it exists on this
machine and is skipped otherwise.
"""

import copy
import importlib.util
import json
import os
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "mine-dag-patterns.py")


def load_harvester():
    """Import the hyphenated harvester script as a module."""
    spec = importlib.util.spec_from_file_location("mine_dag_patterns", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


HARVEST = load_harvester()


# --------------------------------------------------------------------------- #
# Fixture builders (synthetic .jip projects, flat-graph format)
# --------------------------------------------------------------------------- #

COMP_A = "aaaaaaaa-0000-0000-0000-00000000000a"
COMP_B = "bbbbbbbb-0000-0000-0000-00000000000b"


def node_info(node_id, name, loc=None, comp=COMP_A, extra=None, legacy_loc=None,
              old_loc=None):
    """A main-graph node. ``loc`` -> jipipe:metadata-v1 location/<comp>;
    ``legacy_loc`` -> jipipe:ui-grid-location VerticalCompact (legacy format);
    ``old_loc`` -> an extra stale metadata-v1 location under a different
    compartment key (must NOT be used)."""
    n = {
        "jipipe:graph-compartment": comp,
        "jipipe:node-info-id": node_id,
        "jipipe:node:name": name,
        "jipipe:slot-configuration": {"input": {}, "output": {}},
    }
    meta = {}
    if loc is not None:
        meta[f"location/{comp}/x"] = loc[0]
        meta[f"location/{comp}/y"] = loc[1]
    if old_loc is not None:
        meta["location/cccccccc-0000-0000-0000-00000000000c/x"] = old_loc[0]
        meta["location/cccccccc-0000-0000-0000-00000000000c/y"] = old_loc[1]
    if meta:
        n["jipipe:metadata-v1"] = meta
    if legacy_loc is not None:
        n["jipipe:ui-grid-location"] = {
            "": {"VerticalCompact": {"x": legacy_loc[0], "y": legacy_loc[1]}}
        }
    if extra:
        n.update(extra)
    return n


def text_box(title, x, y, w, h, comp=COMP_A, uid_suffix=""):
    return node_info(
        "graph-annotation-text-box", "Text box canvas note" + uid_suffix,
        loc=(x, y), comp=comp,
        extra={"text-title": title, "grid-width": w, "grid-height": h},
    )


def group_node(name, inner_nodes, inner_edges, x, y, comp=COMP_A):
    """A node-group with an explicit contents subgraph (inner nodes carry
    their own (empty) compartment and may lack locations entirely)."""
    return {
        "jipipe:graph-compartment": comp,
        "jipipe:node-info-id": "node-group",
        "jipipe:node:name": name,
        "jipipe:slot-configuration": {"input": {}, "output": {}},
        "jipipe:metadata-v1": {
            f"location/{comp}/x": x, f"location/{comp}/y": y,
        },
        "contents": {"nodes": inner_nodes, "edges": inner_edges},
    }


def inner_node(node_id, name, uid=None):
    """A node inside a group's contents: empty compartment, no location
    (the common case) unless a location is added by the caller."""
    return {
        "jipipe:graph-compartment": "",
        "jipipe:node-info-id": node_id,
        "jipipe:node:name": name,
        "jipipe:slot-configuration": {"input": {}, "output": {}},
    }


def inner_node_loc(node_id, name, legacy_loc):
    n = inner_node(node_id, name)
    n["jipipe:ui-grid-location"] = {
        "": {"VerticalCompact": {"x": legacy_loc[0], "y": legacy_loc[1]}}
    }
    return n


def edge(src, src_slot, dst, dst_slot):
    return {"source-node": src, "source-slot": src_slot,
            "target-node": dst, "target-slot": dst_slot}


def project(nodes, edges, compartments=()):
    """A .jip dict. ``compartments``: (uuid, display_name) pairs."""
    p = {"jipipe:project-format-version": 1, "graph": {"nodes": {}, "edges": []}}
    for uid, n in nodes.items():
        p["graph"]["nodes"][uid] = n
    p["graph"]["edges"] = edges
    if compartments:
        p["compartments"] = {"compartment-graph": {"nodes": {
            uuid: {"jipipe:node-info-id": "jipipe:project-compartment",
                   "jipipe:node:name": name}
            for uuid, name in compartments}}}
    return p


def write_project(root, rel_path, p):
    path = os.path.join(root, rel_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(p, fh)
    return rel_path


# --------------------------------------------------------------------------- #
# Fixture 1: a titled group node (no inner locations -> fallback layout)
# --------------------------------------------------------------------------- #

def build_group_project(root):
    u1, u2, u3 = "u1", "u2", "u3"
    inner = {
        u1: inner_node("folder-list-files", "List files"),
        u2: inner_node("import-imagej-imgplus-from-file", "Import image"),
        u3: inner_node("ij1-roi-to-mask", "Convert ROI to mask"),
    }
    inner_edges = [
        edge(u1, "Files", u2, "Files"),
        edge(u2, "Output", u3, "Image"),
    ]
    nodes = {
        "g": group_node("My Group", inner, inner_edges, 0, 0),
    }
    return write_project(root, "projA.jip", project(nodes, [], [(COMP_A, "GroupComp")]))


# --------------------------------------------------------------------------- #
# Fixture 2: two titled canvas-note sections + an unannotated cluster region
# --------------------------------------------------------------------------- #

def build_sections_project(root):
    nodes = {
        # Section A ("Section A", rect (0,0) 10x10): 4 real nodes, 3 edges
        "g1": node_info("folder-list-files", "List TIFF", loc=(0, 0)),
        "g2": node_info("import-imagej-imgplus-from-file", "Import image", loc=(2, 1)),
        "g3": node_info("ij1-roi-to-mask", "Convert ROI to mask", loc=(4, 2)),
        "g4": node_info("annotate-with-data", "Annotate with data", loc=(4, 6)),
        # Section B ("Section B", rect (0,20) 10x10): 3 real nodes, 2 edges
        "s1": node_info("folder-list-files", "List raw", loc=(0, 20)),
        "s2": node_info("import-imagej-imgplus-from-file", "Import raw", loc=(2, 21)),
        "s3": node_info("ij1-morph-operation2d", "Morphology", loc=(4, 22)),
        # Unannotated region (rect (0,40) 10x10): 3 real nodes, 2 edges
        "c1": node_info("path-to-annotation-simple", "Add paths", loc=(0, 40)),
        "c2": node_info("distribute-data", "Split", loc=(2, 41)),
        "c3": node_info("annotate-data-with-table-values", "Annotate table", loc=(4, 42)),
        # Plumbing: a compartment sink inside section A's rect-neighborhood but
        # OUTSIDE it (x=30) -> must never appear in any candidate.
        "p1": node_info("jipipe:compartment-output", "Output", loc=(30, 0)),
        "na": text_box("Section A", 0, 0, 10, 10),
        "nb": text_box("Section B", 0, 20, 10, 10),
    }
    edges = [
        edge("g1", "Files", "g2", "Files"),
        edge("g2", "Output", "g3", "Image"),
        edge("g3", "Output", "g4", "Annotation"),
        edge("s1", "Files", "s2", "Files"),
        edge("s2", "Output", "s3", "Input"),
        edge("c1", "Annotated paths", "c2", "Input"),
        edge("c2", "Output", "c3", "Input"),
    ]
    # "Analysis" is stop-listed -> no compartment candidate from this project.
    return write_project(root, "projB.jip", project(nodes, edges, [(COMP_B, "Analysis")]))


# --------------------------------------------------------------------------- #
# Fixture 3: named compartments (one stop-listed)
# --------------------------------------------------------------------------- #

def build_compartment_project(root):
    comp1 = "dddddddd-0000-0000-0000-000000000001"
    comp2 = "dddddddd-0000-0000-0000-000000000002"
    nodes = {
        "t1": node_info("folder-list-files", "List", loc=(0, 0), comp=comp1),
        "t2": node_info("import-imagej-imgplus-from-file", "Import", loc=(2, 1), comp=comp1),
        "t3": node_info("ij1-roi-to-mask", "To mask", loc=(4, 2), comp=comp1),
        "t4": node_info("annotate-with-data", "Annotate", loc=(4, 6), comp=comp1),
        "po": node_info("jipipe:compartment-output", "Output", loc=(30, 0), comp=comp1),
        "x1": node_info("folder-list-files", "A1", loc=(0, 0), comp=comp2),
        "x2": node_info("ij1-roi-to-mask", "A2", loc=(2, 1), comp=comp2),
        "x3": node_info("distribute-data", "A3", loc=(4, 2), comp=comp2),
    }
    edges = [
        edge("t1", "Files", "t2", "Files"),
        edge("t2", "Output", "t3", "Image"),
        edge("t3", "Output", "t4", "Annotation"),
        edge("x1", "Files", "x2", "Files"),
    ]
    return write_project(root, "projC.jip", project(
        nodes, edges, [(comp1, "Import and preprocess"), (comp2, "Analysis")]))


# --------------------------------------------------------------------------- #
# Fixture 4: filters (band, sink inside a group, default group name,
# section with too few real nodes, 2-node compartment)
# --------------------------------------------------------------------------- #

def build_filters_project(root):
    comp = "eeeeeeee-0000-0000-0000-00000000000e"
    # below band: 2-node group
    small_inner = {
        "q1": inner_node("folder-list-files", "Q1"),
        "q2": inner_node("ij1-roi-to-mask", "Q2"),
    }
    small = group_node("Tiny Group", small_inner, [edge("q1", "Files", "q2", "Files")], 0, 0, comp=comp)
    # above band: 13-node group
    big_inner = {f"b{i}": inner_node("folder-list-files", f"B{i}") for i in range(13)}
    big_edges = [edge(f"b{i}", "Files", f"b{i+1}", "Files") for i in range(12)]
    big = group_node("Huge Group", big_inner, big_edges, 0, 20, comp=comp)
    # sink inside a group's contents -> reject the whole candidate
    sink_inner = {
        "k1": inner_node("folder-list-files", "K1"),
        "k2": inner_node("import-imagej-imgplus-from-file", "K2"),
        "k3": inner_node("ij1-roi-to-mask", "K3"),
        "k4": inner_node("jipipe:compartment-output", "Out"),
    }
    sink_edges = [edge("k1", "Files", "k2", "Files"),
                  edge("k2", "Output", "k3", "Image"),
                  edge("k3", "Output", "k4", "Raw")]
    sink = group_node("Sink Group", sink_inner, sink_edges, 0, 40, comp=comp)
    # default group name -> untitled (still harvested, title None)
    defn_inner = {
        "d1": inner_node("folder-list-files", "D1"),
        "d2": inner_node("import-imagej-imgplus-from-file", "D2"),
        "d3": inner_node("ij1-roi-to-mask", "D3"),
    }
    defn_edges = [edge("d1", "Files", "d2", "Files"), edge("d2", "Output", "d3", "Image")]
    defn = group_node("Group", defn_inner, defn_edges, 0, 60, comp=comp)
    # a section covering only 2 real nodes -> below band, dropped
    few1 = node_info("folder-list-files", "Few1", loc=(0, 90), comp=comp)
    few2 = node_info("ij1-roi-to-mask", "Few2", loc=(2, 91), comp=comp)
    few_note = text_box("Tiny Section", 0, 90, 10, 5, comp=comp)
    nodes = {
        "small": small, "big": big, "sink": sink, "defn": defn,
        "f1": few1, "f2": few2, "fn": few_note,
    }
    edges = [edge("f1", "Files", "f2", "Files")]
    # compartment "Import and preprocess" has exactly 2 real nodes (below band);
    # the stop-listed "Analysis" compartment would be 3 but is stop-listed.
    return write_project(root, "projD.jip", project(
        nodes, edges, [(comp, "Import and preprocess"),
                       ("ffffffff-0000-0000-0000-00000000000f", "Analysis")]))


# --------------------------------------------------------------------------- #
# Fixture 5: two projects with an identical titled section (support = 2)
# --------------------------------------------------------------------------- #

def identical_section_nodes(prefix, comp):
    return {
        prefix + "1": node_info("folder-list-files", "L1", loc=(0, 0), comp=comp),
        prefix + "2": node_info("import-imagej-imgplus-from-file", "L2", loc=(2, 1), comp=comp),
        prefix + "3": node_info("ij1-roi-to-mask", "L3", loc=(4, 2), comp=comp),
        prefix + "n": text_box("Same Section", 0, 0, 10, 10, comp=comp),
    }


def build_support_projects(root):
    comp1 = "11111111-0000-0000-0000-000000000001"
    comp2 = "22222222-0000-0000-0000-000000000002"
    n1 = identical_section_nodes("e", comp1)
    p1 = project(n1, [edge("e1", "Files", "e2", "Files"),
                      edge("e2", "Output", "e3", "Image")],
                 [(comp1, "Ecomp")])
    n2 = identical_section_nodes("f", comp2)
    p2 = project(n2, [edge("f1", "Files", "f2", "Files"),
                      edge("f2", "Output", "f3", "Image")],
                 [(comp2, "Fcomp")])
    write_project(root, "projE1.jip", p1)
    return write_project(root, "projE2.jip", p2)


# --------------------------------------------------------------------------- #
# Fixture 6: location formats (legacy ui-grid-location, stale extra key)
# --------------------------------------------------------------------------- #

def build_location_project(root):
    comp = "33333333-0000-0000-0000-000000000003"
    nodes = {
        # modern metadata-v1 location
        "m1": node_info("folder-list-files", "M1", loc=(10, 10), comp=comp),
        # legacy ui-grid-location only
        "m2": node_info("import-imagej-imgplus-from-file", "M2", comp=comp,
                        legacy_loc=(12, 11)),
        # modern location PLUS a stale location under another compartment key
        "m3": node_info("ij1-roi-to-mask", "M3", loc=(14, 12), comp=comp,
                        old_loc=(999, 999)),
        "mn": text_box("Loc Section", 0, 0, 30, 30, comp=comp),
    }
    edges = [edge("m1", "Files", "m2", "Files"), edge("m2", "Output", "m3", "Image")]
    return write_project(root, "projF.jip", project(nodes, edges, [(comp, "LocComp")]))


# --------------------------------------------------------------------------- #
# Helpers to find candidates in the mine result
# --------------------------------------------------------------------------- #

def find(result, signal=None, title=None, project=None):
    out = []
    for c in result["candidates"]:
        if signal is not None and c["signal"] != signal:
            continue
        if title is not None and c["title"] != title:
            continue
        if project is not None and c["project"] != project:
            continue
        out.append(c)
    return out


def node_ids(c):
    return {v["jipipe:node-info-id"] for v in c["graph"]["nodes"].values()}


def locations(c):
    """{node key: (x, y)} from the candidate graph's metadata-v1."""
    locs = {}
    for k, v in c["graph"]["nodes"].items():
        comp = v["jipipe:graph-compartment"]
        m = v.get("jipipe:metadata-v1", {})
        x = m.get(f"location/{comp}/x")
        y = m.get(f"location/{comp}/y")
        if x is not None and y is not None:
            locs[k] = (x, y)
    return locs


def run_mine(root, out_name="dag-candidates.json"):
    out = os.path.join(root, out_name)
    result = HARVEST.mine_corpus(root, out, shipped_dir=None)
    return result


class TempCorpus:
    def __init__(self, builders):
        self.root = tempfile.mkdtemp(prefix="dag-mine-test-")
        for b in builders:
            b(self.root)

    def cleanup(self):
        shutil.rmtree(self.root, ignore_errors=True)


class HarvestTests(unittest.TestCase):

    def test_group_node_harvest_fallback_layout(self):
        corpus = TempCorpus([build_group_project])
        try:
            result = run_mine(corpus.root)
            got = find(result, signal="group-node", title="My Group")
            self.assertEqual(len(got), 1,
                             f"expected 1 group candidate, got {result['candidates']}")
            c = got[0]
            self.assertEqual(c["nodeCount"], 3)
            self.assertEqual(c["edgeCount"], 2)
            self.assertEqual(node_ids(c),
                             {"folder-list-files",
                              "import-imagej-imgplus-from-file", "ij1-roi-to-mask"})
            # no source layout in the group contents -> fallback layering
            self.assertEqual(c["layoutSource"], "fallback")
            locs = locations(c)
            self.assertEqual(len(locs), 3)
            # longest-path layering: 3 columns, 0-based
            self.assertEqual(sorted(set(x for x, _ in locs.values())), [0, 1, 2])
            self.assertEqual(min(y for _, y in locs.values()), 0)
            # every node carries a location pair under the candidate's own
            # (single, shared) compartment
            comps = {v["jipipe:graph-compartment"] for v in c["graph"]["nodes"].values()}
            self.assertEqual(len(comps), 1)
            self.assertEqual(comps.pop(), c["compartmentUuid"])
            # provenance
            self.assertEqual(c["project"], "projA.jip")
            self.assertEqual(c["support"], 1)
            self.assertEqual(c["memberProjects"], ["projA.jip"])
        finally:
            corpus.cleanup()

    def test_canvas_note_sections(self):
        corpus = TempCorpus([build_sections_project])
        try:
            result = run_mine(corpus.root)
            a = find(result, signal="canvas-note-section", title="Section A")
            self.assertEqual(len(a), 1)
            a = a[0]
            self.assertEqual(a["nodeCount"], 4)
            self.assertEqual(a["edgeCount"], 3)
            self.assertEqual(a["layoutSource"], "source")
            # normalized to the bounding-box origin
            locs = locations(a)
            self.assertEqual(min(x for x, _ in locs.values()), 0)
            self.assertEqual(min(y for _, y in locs.values()), 0)
            # the compartment sink (x=30) must not be a member
            self.assertNotIn("jipipe:compartment-output", node_ids(a))
            # the annotation nodes themselves are not members
            self.assertNotIn("graph-annotation-text-box", node_ids(a))

            b = find(result, signal="canvas-note-section", title="Section B")
            self.assertEqual(len(b), 1)
            self.assertEqual(b[0]["nodeCount"], 3)
            self.assertEqual(b[0]["edgeCount"], 2)
        finally:
            corpus.cleanup()

    def test_geometric_clusters_unannotated(self):
        corpus = TempCorpus([build_sections_project])
        try:
            result = run_mine(corpus.root)
            clusters = find(result, signal="geometric-cluster")
            # the titled sections' members are ground truth (ruling A2) and
            # are excluded from clustering: only the UNANNOTATED c-region
            # forms a cluster
            self.assertEqual(len(clusters), 1,
                             f"expected 1 cluster, got {[c['title'] for c in clusters]}")
            c_regions = [c for c in clusters
                         if node_ids(c) == {"path-to-annotation-simple",
                                            "distribute-data",
                                            "annotate-data-with-table-values"}]
            self.assertEqual(len(c_regions), 1)
            self.assertEqual(c_regions[0]["nodeCount"], 3)
            self.assertEqual(c_regions[0]["edgeCount"], 2)
            self.assertEqual(c_regions[0]["layoutSource"], "source")
        finally:
            corpus.cleanup()

    def test_auto_cluster_gap_cut_unit(self):
        # two tight triples, far apart -> 2 clusters
        points = {f"a{i}": (i, 0) for i in range(3)}
        points.update({f"b{i}": (100 + i, 100) for i in range(3)})
        clusters = HARVEST.auto_cluster(points)
        self.assertEqual(len(clusters), 2)
        sizes = sorted(len(c) for c in clusters)
        self.assertEqual(sizes, [3, 3])
        # uniform spacing (no gap) -> a single cluster
        uniform = {f"u{i}": (i * 2, 0) for i in range(4)}
        clusters = HARVEST.auto_cluster(uniform)
        self.assertEqual(len(clusters), 1)
        self.assertEqual(clusters[0], set(uniform))

    def test_compartment_signal_and_stoplist(self):
        corpus = TempCorpus([build_compartment_project])
        try:
            result = run_mine(corpus.root)
            got = find(result, signal="compartment", title="Import and preprocess")
            self.assertEqual(len(got), 1)
            c = got[0]
            self.assertEqual(c["nodeCount"], 4)
            self.assertEqual(c["edgeCount"], 3)
            # the compartment-output proxy is not a member
            self.assertNotIn("jipipe:compartment-output", node_ids(c))
            # the stop-listed "Analysis" compartment yields no candidate
            self.assertEqual(len(find(result, signal="compartment",
                                      title="Analysis")), 0)
        finally:
            corpus.cleanup()

    def test_filters(self):
        corpus = TempCorpus([build_filters_project])
        try:
            result = run_mine(corpus.root)
            titles = {c["title"] for c in result["candidates"]}
            # below band (2 nodes)
            self.assertNotIn("Tiny Group", titles)
            self.assertNotIn("Tiny Section", titles)
            # above band (13 nodes)
            self.assertNotIn("Huge Group", titles)
            # sink inside the group contents rejects the whole candidate
            self.assertNotIn("Sink Group", titles)
            # default group name -> harvested but UNTITLED
            defn = find(result, signal="group-node", title=None)
            self.assertEqual(len(defn), 1)
            self.assertEqual(node_ids(defn[0]),
                             {"folder-list-files",
                              "import-imagej-imgplus-from-file", "ij1-roi-to-mask"})
            # the 2-real-node compartment is below band; stop-listed one rejected
            self.assertEqual(len(find(result, signal="compartment")), 0)
            # geometric clusters may still fire for the group-node region,
            # but no candidate may contain the sink
            for c in result["candidates"]:
                self.assertNotIn("jipipe:compartment-output", node_ids(c))
        finally:
            corpus.cleanup()

    def test_support_counts_across_projects(self):
        corpus = TempCorpus([build_support_projects])
        try:
            result = run_mine(corpus.root)
            got = find(result, signal="canvas-note-section", title="Same Section")
            self.assertEqual(len(got), 2, "both projects emit the section")
            for c in got:
                self.assertEqual(c["support"], 2)
                self.assertEqual(c["memberProjects"], ["projE1.jip", "projE2.jip"])
            # identical functional signature -> the two share it
            self.assertEqual(HARVEST.functional_signature(got[0]["graph"]),
                             HARVEST.functional_signature(got[1]["graph"]))
        finally:
            corpus.cleanup()

    def test_duplicate_of_shipped_is_dropped(self):
        # a candidate whose functional signature matches a shipped 64 pattern
        # (a 3-node linear chain folder-list-files -> import -> roi-to-mask is
        # NOT one of the shipped 64; instead fabricate the shipped set)
        corpus = TempCorpus([build_support_projects])
        shipped_dir = tempfile.mkdtemp(prefix="dag-mine-shipped-")
        try:
            # craft a shipped template identical to "Same Section"'s graph
            got_graph = None
            result0 = run_mine(corpus.root, "pre.json")
            for c in result0["candidates"]:
                if c["signal"] == "canvas-note-section" and c["title"] == "Same Section":
                    got_graph = c["graph"]
            self.assertIsNotNone(got_graph)
            with open(os.path.join(shipped_dir, "shipped-1.json"), "w",
                      encoding="utf-8") as fh:
                json.dump(got_graph, fh)
            result = HARVEST.mine_corpus(corpus.root, os.path.join(corpus.root, "out.json"),
                                         shipped_dir=shipped_dir)
            self.assertEqual(len(find(result, signal="canvas-note-section",
                                      title="Same Section")), 0,
                             "a shipped-identical candidate must be dropped")
        finally:
            corpus.cleanup()
            shutil.rmtree(shipped_dir, ignore_errors=True)

    def test_location_formats(self):
        corpus = TempCorpus([build_location_project])
        try:
            result = run_mine(corpus.root)
            got = find(result, signal="canvas-note-section", title="Loc Section")
            self.assertEqual(len(got), 1)
            c = got[0]
            self.assertEqual(c["nodeCount"], 3)
            self.assertEqual(c["layoutSource"], "source")
            locs = locations(c)
            self.assertEqual(len(locs), 3, "legacy + stale-key nodes must resolve")
            # normalized: min x = 0, min y = 0; the stale 999,999 key is unused
            self.assertEqual(min(x for x, _ in locs.values()), 0)
            self.assertEqual(min(y for _, y in locs.values()), 0)
            xs = sorted(x for x, _ in locs.values())
            self.assertEqual(xs, [0, 2, 4])
        finally:
            corpus.cleanup()

    def test_output_is_deterministic(self):
        corpus = TempCorpus([build_group_project, build_sections_project,
                             build_compartment_project, build_filters_project,
                             build_support_projects, build_location_project])
        try:
            out1 = os.path.join(corpus.root, "out1.json")
            out2 = os.path.join(corpus.root, "out2.json")
            # pin the generated timestamp so the two runs are byte-identical
            # (the harvest is deterministic; the wall-clock timestamp is the
            # only non-deterministic field and is caller-controlled)
            stamp = "2026-10-17T00:00:00Z"
            HARVEST.mine_corpus(corpus.root, out1, shipped_dir=None,
                                generated=stamp)
            HARVEST.mine_corpus(corpus.root, out2, shipped_dir=None,
                                generated=stamp)
            with open(out1, "rb") as f1, open(out2, "rb") as f2:
                self.assertEqual(f1.read(), f2.read(),
                                 "two runs must be byte-identical")
            # candidate ids are stable and sequential
            with open(out1, encoding="utf-8") as fh:
                result = json.load(fh)
            ids = [c["candidateId"] for c in result["candidates"]]
            self.assertEqual(len(ids), len(set(ids)))
            self.assertEqual(ids, sorted(ids))
            # support groups candidates by functional signature: every
            # candidate's memberProjects is the union over its signature
            bysig = {}
            for c in result["candidates"]:
                bysig.setdefault(c["signature"], set()).add(c["project"])
            for c in result["candidates"]:
                self.assertEqual(set(c["memberProjects"]),
                                 bysig[c["signature"]],
                                 "memberProjects must equal the signature's "
                                 "project set")
                self.assertEqual(c["support"], len(c["memberProjects"]))
        finally:
            corpus.cleanup()

    def test_flagship_cellpose4_sections(self):
        """If the real corpus flagship is on this machine, its two titled
        sections must harvest exactly as measured (8/8 and 3/3)."""
        flagship = "/data/JIPipe/Cellpose training example"
        if not os.path.isdir(flagship):
            self.skipTest("flagship corpus not present on this machine")
        out = tempfile.mktemp(prefix="dag-mine-flagship-", suffix=".json")
        try:
            result = HARVEST.mine_corpus(flagship, out, shipped_dir=None)
            loading = find(result, title="Loading raw images and ROIs")
            self.assertTrue(loading, "flagship 'Loading raw images and ROIs' missing")
            loading = loading[0]
            self.assertEqual(loading["nodeCount"], 8)
            self.assertEqual(loading["edgeCount"], 8)
            self.assertEqual(loading["layoutSource"], "source")
            training = find(result, title="Training")
            self.assertTrue(training, "flagship 'Training' missing")
            training = training[0]
            self.assertEqual(training["nodeCount"], 3)
            self.assertEqual(training["edgeCount"], 3)
        finally:
            if os.path.exists(out):
                os.remove(out)


if __name__ == "__main__":
    unittest.main(verbosity=2)
