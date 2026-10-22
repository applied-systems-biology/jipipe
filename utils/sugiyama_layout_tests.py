#!/usr/bin/env python3
"""Unit tests for sugiyama_layout.py (the shared grid-cell layout for the
bundled System patterns). Run: python3 utils/sugiyama_layout_tests.py"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sugiyama_layout import layout  # noqa: E402


def chain3():
    return {
        "nodes": {"a": {}, "b": {}, "c": {}},
        "edges": [
            {"source-node": "a", "target-node": "b"},
            {"source-node": "b", "target-node": "c"},
        ],
    }


def test_linear_chain_width7():
    # The brief's canonical case: x = 1, 10, 19 (each column 7 wide + 2 gap),
    # all at y = 1 (single row per layer).
    pos = layout(chain3(), {"a": 7, "b": 7, "c": 7})
    assert pos == {"a": (1, 1), "b": (10, 1), "c": (19, 1)}, pos


def test_width_map_10_then_7():
    # A 10-cell-wide node pushes the next column to x = 1 + 10 + 2 = 13.
    pos = layout(chain3(), {"a": 10, "b": 7, "c": 7})
    assert pos == {"a": (1, 1), "b": (13, 1), "c": (22, 1)}, pos


def test_diamond():
    g = {
        "nodes": {"a": {}, "b": {}, "c": {}, "d": {}},
        "edges": [
            {"source-node": "a", "source-slot": "o", "target-node": "b", "target-slot": "i"},
            {"source-node": "a", "source-slot": "o", "target-node": "c", "target-slot": "i"},
            {"source-node": "b", "source-slot": "o", "target-node": "d", "target-slot": "i"},
            {"source-node": "c", "source-slot": "o", "target-node": "d", "target-slot": "i"},
        ],
    }
    pos = layout(g, {k: 7 for k in "abcd"})
    # a: layer 0. b, c: layer 1 (same layer, different y: 1 and 1+3+1=5).
    # d: layer 2, single row (index 0 -> y = 1).
    assert pos["a"] == (1, 1), pos
    assert pos["b"] == (10, 1), pos
    assert pos["c"] == (10, 5), pos
    assert pos["d"] == (19, 1), pos


def test_diamond_layer_order_by_node_key():
    # The within-layer index is the SORTED node-key order: a "z" node in layer
    # 1 must sort after a "b" node (b on top, z below), regardless of edge
    # order.
    g = {
        "nodes": {"a": {}, "b": {}, "z": {}},
        "edges": [
            {"source-node": "a", "target-node": "z"},
            {"source-node": "a", "target-node": "b"},
        ],
    }
    pos = layout(g, {k: 7 for k in "abz"})
    assert pos["b"] == (10, 1), pos
    assert pos["z"] == (10, 5), pos


def test_long_edge_virtual_handling():
    # a->b->c plus the long edge a->c: c's layer is 2 (longest path), so its
    # x steps TWICE (the a->c long edge spans a virtual position in layer 1,
    # which contributes no width). The long edge must NOT pull c up to layer 1
    # and must NOT add a phantom column.
    g = {
        "nodes": {"a": {}, "b": {}, "c": {}},
        "edges": [
            {"source-node": "a", "target-node": "b"},
            {"source-node": "b", "target-node": "c"},
            {"source-node": "a", "target-node": "c"},
        ],
    }
    pos = layout(g, {k: 7 for k in "abc"})
    assert pos == {"a": (1, 1), "b": (10, 1), "c": (19, 1)}, pos


def test_isolated_node_free_floating_stack():
    # An isolated node + a chain: the isolated node is placed in a single
    # column just past the last real column (x = 1 + sum of all real columns =
    # 1 + 9 + 9 = 19), stacked at y = 1 + 3*(index+1) = 4.
    g = {
        "nodes": {"a": {}, "b": {}, "iso": {}},
        "edges": [{"source-node": "a", "target-node": "b"}],
    }
    pos = layout(g, {k: 7 for k in "abiso"})
    assert pos["a"] == (1, 1), pos
    assert pos["b"] == (10, 1), pos
    assert pos["iso"] == (19, 4), pos


def test_multiple_isolated_stack():
    # Two isolated nodes stack vertically with a 3-cell pitch (y = 4 and 7).
    g = {"nodes": {"i1": {}, "i2": {}}, "edges": []}
    pos = layout(g, {})
    assert pos["i1"] == (1, 4), pos
    assert pos["i2"] == (1, 7), pos


def test_isolated_only_no_real_columns():
    # No edges at all: the free-floating column starts at x = 1 (1 + 0).
    g = {"nodes": {"solo": {}}, "edges": []}
    pos = layout(g, {"solo": 7})
    assert pos["solo"] == (1, 4), pos


def test_default_width_for_missing_key():
    # A node absent from the width map uses the 7-cell default.
    pos = layout(chain3(), {"a": 10})
    assert pos == {"a": (1, 1), "b": (13, 1), "c": (22, 1)}, pos


def test_empty_graph():
    assert layout({"nodes": {}, "edges": []}, {}) == {}


def test_determinism():
    # Same input twice -> identical output (dict equality).
    import json
    g = {
        "nodes": {"n%d" % i: {} for i in range(6)},
        "edges": [
            {"source-node": "n0", "target-node": "n1"},
            {"source-node": "n0", "target-node": "n2"},
            {"source-node": "n1", "target-node": "n3"},
            {"source-node": "n2", "target-node": "n3"},
            {"source-node": "n3", "target-node": "n4"},
            {"source-node": "n4", "target-node": "n5"},
        ],
    }
    widths = {k: (7 if i % 2 else 10) for i, k in enumerate(sorted(g["nodes"]))}
    # Rebuild the graph fresh from the same bytes to simulate a second run.
    g2 = json.loads(json.dumps(g))
    w2 = dict(widths)
    assert layout(g, widths) == layout(g2, w2)


def test_row_height_uses_node_height_not_width():
    # The vertical pitch is 3 (height) + 1 (gap) = 4 per preceding row, and is
    # independent of the (wider) node width.
    g = {
        "nodes": {"a": {}, "b": {}, "c": {}},
        "edges": [
            {"source-node": "a", "target-node": "b"},
            {"source-node": "a", "target-node": "c"},
        ],
    }
    pos = layout(g, {k: 10 for k in "abc"})
    assert pos["b"] == (13, 1), pos
    assert pos["c"] == (13, 5), pos  # 1 + (3 + 1), not width-based


if __name__ == "__main__":
    fns = [v for k, v in sorted(globals().items())
           if k.startswith("test_") and callable(v)]
    for fn in fns:
        fn()
        print(f"PASS {fn.__name__}")
    print(f"OK ({len(fns)} tests)")
