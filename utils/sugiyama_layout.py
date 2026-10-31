#!/usr/bin/env python3
"""Shared deterministic grid-cell layout for the bundled System patterns.

A pure, UI-free re-implementation of the arithmetic of
``SugiyamaGraphAutoLayoutImplementation.rearrangeSugiyamaVertical`` (the app's
Sugiyama auto-layout - layers flow TOP TO BOTTOM, the app's flow direction,
1338), over the RAW edge list of a minimal graph
(``{"nodes": {key: {...}}, "edges": [...]}``). For the shipped patterns it is
deterministic and, per the global-constraints rule, identical in spacing to
the Java impl's output.

Why a pure port: the Java impl is coupled to the Swing node-UI (it reads each
node's pixel width/height from the live canvas and walks the UI set in
nondeterministic order). A port over the raw edge list with a caller-supplied
width map is byte-stable and reproducible, which the generators need.

Spacing rules (the global-constraints rule, the binding spec):
  1. Layer = longest-path depth (Kahn topological order; sources = 0). Long
     edges (spanning > 1 layer) need no explicit virtual node for *positioning*:
     the longest-path layering already puts the target at the right depth, and
     in the Java impl a virtual-only column/row contributes 0 width/height, so
     a real node's position depends only on the real nodes in earlier layers /
     earlier indices.
  2. Within a layer: index = node-key sort order (deterministic; the Java impl
     uses set order, which is irrelevant for equality of the spacing rules).
  3. y(layer) = 1 + sum over l' < layer of (NODE_HEIGHT_CELLS + 1) -
     every real node is 3 grid rows tall, so each layer advances the row by
     3 + 1 cells (the Java impl's per-row max of node heights + GRID_HEIGHT).
  4. x(index) = 1 + sum over i' < index of (nodeWidthCells(i') + 2), where
     nodeWidthCells comes from the width map (cells; the caller supplies it,
     already ceil(estimate/25) - see node_width_cells below).
  5. The width map (node key -> cells) is the caller's: 7 cells for most
     nodes, 10 cells for nodes whose FINAL slot count (restored
     jipipe:slot-configuration if present, else registry inputSlots+outputSlots)
     is >= 4. It is computed AFTER the B1 restore pass, so the layout sees the
     final slot counts. This function NEVER touches the registry.
  6. Free-floating nodes (no edge at all in the pattern): y = 1 + sum of all
     real rows (the y just past the last layer), x = 1 + 7*(index+1)
     (DEFAULT_WIDTH_CELLS pitch), mirroring the Java impl's free-floating
     stack (right of the content, down from the top). Only reachable if a
     pattern with an isolated node ships (curation should prevent it); the
     rule exists so the function is total.

Coordinates are GRID CELLS (the editor stores locations in cells), origin at
(1, 1).
"""

from __future__ import annotations

import uuid
from collections import defaultdict

#: A node occupies 3 grid rows (the Java impl's nodeHeight + spacing basis).
NODE_HEIGHT_CELLS = 3
#: Horizontal gap between same-layer (parallel) nodes, in grid cells (2
#: cells, mirroring the Java impl's 2 * GRID_WIDTH column padding).
LAYER_GAP_CELLS = 2
#: Vertical gap between rows, in grid cells (1 cell, mirroring the Java impl's
#: 1 * GRID_HEIGHT padding).
ROW_GAP_CELLS = 1
#: Default width (cells) for a node absent from the caller's width map.
DEFAULT_WIDTH_CELLS = 7


def _longest_path_layering(nodes, edges):
    """Longest-path layering: {node key: layer}. A node's layer is its longest
    distance (in edges) from any source; sources (no incoming edge) are 0.
    Deterministic Kahn order (alphabetical among ready nodes). A cycle-break
    fallback (unreachable for the (A)-format DAGs) keeps it total."""
    pred = {k: [] for k in nodes}
    for e in edges:
        s, t = e.get("source-node"), e.get("target-node")
        if s is not None and t is not None and s != t \
                and s in pred and t in pred:
            pred[t].append(s)
    layer = {}
    done = set()
    remaining = set(nodes)
    while remaining:
        ready = [k for k in sorted(remaining) if all(p in done for p in pred[k])]
        if not ready:
            ready = [max(remaining)]  # deterministic cycle break (unreachable)
        for k in ready:
            layer[k] = max((layer[p] + 1 for p in pred[k] if p in layer),
                           default=0)
            done.add(k)
            remaining.discard(k)
    return layer


def layout(graph, node_width_cells):
    """Lay out ``graph`` on the grid.

    Args:
      graph: ``{"nodes": {key: {...}}, "edges": [...]}`` - the minimal
        (A)-format graph. Node keys are the dict keys (not the UUIDs); edges
        reference ``source-node`` / ``target-node``.
      node_width_cells: ``{node key: int}`` - the FINAL width of each node in
        grid cells (7 or 10; see rule 5). Nodes absent from the map use
        :data:`DEFAULT_WIDTH_CELLS`.

    Returns:
      ``{node key: (x, y)}`` - deterministic grid-cell coordinates, origin
      (1, 1). Every node in ``graph`` gets a placement (total).
    """
    nodes = graph.get("nodes", {})
    edges = graph.get("edges", [])

    layer = _longest_path_layering(nodes, edges)

    # Nodes touched by at least one (valid, in-graph) edge are NOT free-floating.
    has_edge = set()
    for e in edges:
        s, t = e.get("source-node"), e.get("target-node")
        if s is not None and s in nodes:
            has_edge.add(s)
        if t is not None and t in nodes:
            has_edge.add(t)

    # Real (non-free-floating) nodes grouped by layer, sorted by node key
    # (rule 2: deterministic index order).
    by_layer = defaultdict(list)
    for k in sorted(layer):
        if k in has_edge:
            by_layer[layer[k]].append(k)
    for l in by_layer:
        by_layer[l].sort()

    def width(k):
        return node_width_cells.get(k, DEFAULT_WIDTH_CELLS)

    # y per layer (rule 3): start at 1, advance by (NODE_HEIGHT_CELLS + 1).
    # Every real node is 3 grid rows tall, so a layer's row height is always
    # NODE_HEIGHT_CELLS (the Java impl's per-row max over node heights).
    max_layer = max(by_layer) if by_layer else -1
    y_of_layer = {}
    y = 1
    for l in range(0, max_layer + 1):
        y_of_layer[l] = y
        y += NODE_HEIGHT_CELLS + ROW_GAP_CELLS

    result = {}
    for l in sorted(by_layer):
        keys = by_layer[l]
        # rule 4: x = 1 + sum over i' < idx of (width(i') + LAYER_GAP)
        x = 1
        for i, k in enumerate(keys):
            result[k] = (x, y_of_layer[l])
            x += width(k) + LAYER_GAP_CELLS

    # Free-floating nodes (rule 6): a single row just past the last real
    # row (down from the top), laid out right of the content with a
    # 7-cell (default width) pitch.
    free = [k for k in sorted(nodes) if k not in has_edge]
    if free:
        ff_y = y  # == 1 + sum of all real rows (the y after the last layer)
        for i, k in enumerate(free):
            result[k] = (1 + DEFAULT_WIDTH_CELLS * (i + 1), ff_y)

    return result


def is_uuid(value):
    """True iff ``value`` parses as a UUID string."""
    if not isinstance(value, str):
        return False
    try:
        uuid.UUID(value)
        return True
    except Exception:
        return False


def node_location_valid(node):
    """A node's location is VALID iff its ``jipipe:graph-compartment`` is a
    UUID (or empty / ``"DEFAULT"`` - both resolve to the default compartment)
    AND ``jipipe:metadata-v1`` carries ``location/<that key>/x`` and ``/y``.
    (The legacy-name "derived" + pixel-era files and the 24 location-less
    linear files are the 179 invalid ones.)"""
    comp = node.get("jipipe:graph-compartment", "")
    md = node.get("jipipe:metadata-v1") or {}
    if not (is_uuid(comp) or comp in ("DEFAULT", "")):
        return False
    return (f"location/{comp}/x" in md) and (f"location/{comp}/y" in md)


def relocate_graph(graph, node_width_cells, rekey_legacy="derived"):
    """Re-lay out the WHOLE graph with :func:`layout` and rewrite every node's
    ``jipipe:metadata-v1`` to ``{"location/<comp>/x": x,
    "location/<comp>/y": y}`` where ``<comp>`` is the node's existing
    compartment value.

    If ``rekey_legacy`` is not None, nodes whose compartment equals that
    legacy name (e.g. ``"derived"``) also get their
    ``jipipe:graph-compartment`` rewritten to ``"DEFAULT"`` (the resolvable
    default-compartment marker - ``JIPipeGraph.nodesFromJson`` keeps
    default-compartment nodes as-is and the existing "DEFAULT" location
    migration promotes the key) and their location is keyed under
    ``"DEFAULT"``. Mined nodes (UUID compartments) keep their compartment and
    are keyed under their own UUID.

    Returns the number of nodes relocated. Mutates ``graph`` in place.
    """
    pos = layout(graph, node_width_cells)
    moved = 0
    for k, node in graph["nodes"].items():
        x, y = pos[k]
        comp = node.get("jipipe:graph-compartment", "")
        if rekey_legacy is not None and comp == rekey_legacy:
            comp = "DEFAULT"
            node["jipipe:graph-compartment"] = comp
        node["jipipe:metadata-v1"] = {
            f"location/{comp}/x": x,
            f"location/{comp}/y": y,
        }
        moved += 1
    return moved


def width_cells_for_slot_count(slot_count):
    """The node width (grid cells) for a node's FINAL slot count: 10 cells
    for >= 4 slots (long slot names push the UI past 7 cells), else 7. (The
    Global-Constraints width rule 5.)"""
    return 10 if slot_count >= 4 else DEFAULT_WIDTH_CELLS


if __name__ == "__main__":
    # Smoke test: a 3-node chain, all nodes width 7.
    g = {
        "nodes": {"a": {}, "b": {}, "c": {}},
        "edges": [
            {"source-node": "a", "target-node": "b"},
            {"source-node": "b", "target-node": "c"},
        ],
    }
    print(layout(g, {"a": 7, "b": 7, "c": 7}))
