#!/usr/bin/env python3
"""Mine the golden-set corpus candidates from the example workflows (#1408).

Counts node placements across the example workflows (every
``jipipe:node-info-id`` value, including the ones inside ``node-group``
nested graphs) and joins them with a registry dump (node id, name,
description plain text), then writes one candidate block per node id,
sorted by placement count:

    python3 utils/search-usage/build-golden-set-candidates.py \
        /tmp/opencode/registry-dump.json

The registry dump is the full-registry JSON of ``NodeRegistryDumpToolTest``
(``-Djipipe.dump.path=<path>``, object keyed by node id with ``name``,
``description``, ``framework``). The failing-only dump of
``SearchDescriptionQualityTest`` (``-Djipipe.desc-sweep.dump=<path>``) is
also accepted but covers only gate failures; the script warns when it
detects it because most candidates then keep ``name: null``.

Output ``utils/taxonomy-research/golden-set/candidates.json`` is the
authoring input of the golden set (task 1 of MR C): the most-placed ids
are what users demonstrably search for. Deterministic: identical inputs
produce byte-identical output. Node ids that the dump does not know keep
``inDump: false`` and are never silently dropped.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from collections import Counter

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT_DIR = os.path.join(REPO_ROOT, "utils", "taxonomy-research", "golden-set")
WORKFLOWS_DIR = "/data/JIPipe/Example workflows"
NODE_INFO_ID_KEY = "jipipe:node-info-id"


def collect_node_ids(obj, hits: Counter, path: str) -> None:
    """Recursively collect every node-info id under the given object."""
    if isinstance(obj, dict):
        for key, value in obj.items():
            if key == NODE_INFO_ID_KEY and isinstance(value, str):
                hits[value] += 1
            else:
                collect_node_ids(value, hits, path + "/" + str(key))
    elif isinstance(obj, list):
        for index, value in enumerate(obj):
            collect_node_ids(value, hits, path + f"[{index}]")


def top_graph_node_ids(document) -> Counter:
    """The node-info ids directly under the top-level graph.nodes (the
    counting the usage prior of #1404 uses; node-group contents excluded)."""
    hits: Counter = Counter()
    for node in document.get("graph", {}).get("nodes", {}).values():
        if isinstance(node, dict) and isinstance(node.get(NODE_INFO_ID_KEY), str):
            hits[node[NODE_INFO_ID_KEY]] += 1
    return hits


def workflow_node_ids(directory: str) -> tuple[dict[str, Counter], dict[str, Counter], list[str]]:
    """Return ({file: all node ids}, {file: top-graph node ids}, unreadable files).

    The first counter is recursive (includes node-group contents); the
    second counts only the nodes directly under the top-level graph.nodes
    (the counting the usage prior of #1404 uses).
    """
    per_file: dict[str, Counter] = {}
    top_file: dict[str, Counter] = {}
    unreadable: list[str] = []
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".jip"):
            continue
        path = os.path.join(directory, name)
        try:
            with open(path, encoding="utf-8") as fp:
                document = json.load(fp)
        except (OSError, json.JSONDecodeError) as error:
            unreadable.append(f"{name}: {error}")
            continue
        hits: Counter = Counter()
        collect_node_ids(document, hits, "")
        per_file[name] = hits
        top_file[name] = top_graph_node_ids(document)
    return per_file, top_file, unreadable


def load_registry(dump_path: str) -> tuple[dict[str, dict], bool]:
    """Load a registry dump. Returns (rows keyed by node id, is_full_registry).

    Accepts the full-registry dump (object keyed by node id, values with
    ``name``) and the failing-only gate dump (document with a ``failing``
    array). The full dump is the preferred join source.
    """
    with open(dump_path, encoding="utf-8") as fp:
        document = json.load(fp)
    if isinstance(document, dict) and isinstance(document.get("failing"), list):
        rows = {}
        for entry in document["failing"]:
            rows[entry["nodeId"]] = {
                "name": entry.get("name"),
                "description": entry.get("description"),
            }
        return rows, False
    rows = {}
    for node_id, value in document.items():
        if isinstance(value, dict) and "name" in value:
            rows[node_id] = value
    return rows, True


def build_candidates(per_file: dict[str, Counter], top_file: dict[str, Counter],
                     registry: dict[str, dict]) -> list[dict]:
    totals: Counter = Counter()
    top_totals: Counter = Counter()
    workflows_of: dict[str, list[str]] = {}
    for name, hits in per_file.items():
        for node_id, count in hits.items():
            totals[node_id] += count
            workflows_of.setdefault(node_id, []).append(name)
    for hits in top_file.values():
        top_totals.update(hits)
    candidates = []
    for node_id in sorted(totals, key=lambda n: (-totals[n], n)):
        row = registry.get(node_id)
        candidate = {
            "nodeId": node_id,
            "placements": totals[node_id],
            "topGraphPlacements": top_totals.get(node_id, 0),
            "workflows": workflows_of[node_id],
            "inDump": row is not None,
            "name": row.get("name") if row else None,
            "description": row.get("description") if row else None,
        }
        if row and row.get("framework") is not None:
            candidate["framework"] = row["framework"]
        candidates.append(candidate)
    return candidates


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("dump", help="registry dump JSON (NodeRegistryDumpToolTest -Djipipe.dump.path)")
    parser.add_argument("--workflows", default=WORKFLOWS_DIR,
                        help=f"directory with the example .jip workflows (default {WORKFLOWS_DIR})")
    parser.add_argument("--out", default=os.path.join(OUT_DIR, "candidates.json"))
    args = parser.parse_args()

    registry, is_full = load_registry(args.dump)
    if not is_full:
        print("WARNING: dump looks like the failing-only SearchDescriptionQualityTest "
              "output; use NodeRegistryDumpToolTest (-Djipipe.dump.path) for the full "
              "registry so every candidate gets name and description.")

    per_file, top_file, unreadable = workflow_node_ids(args.workflows)
    candidates = build_candidates(per_file, top_file, registry)
    total_placements = sum(c["placements"] for c in candidates)
    in_dump = sum(1 for c in candidates if c["inDump"])

    document = {
        "_note": ("Golden-set mining output (#1408, task 1): node placements across the "
                  "example workflows joined with the registry dump. Rerun with: "
                  "python3 utils/search-usage/build-golden-set-candidates.py <registry-dump>"),
        "workflowsScanned": len(per_file),
        "totalPlacements": total_placements,
        "distinctNodeIds": len(candidates),
        "registryRows": len(registry),
        "candidatesInDump": in_dump,
        "candidates": candidates,
    }
    if unreadable:
        document["unreadableWorkflows"] = unreadable

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fp:
        json.dump(document, fp, indent=2, sort_keys=True, ensure_ascii=False)
        fp.write("\n")

    print(f"{'nodeId':44s} {'places':>6s}  name")
    for candidate in candidates[:15]:
        name = candidate["name"] or "(not in dump)"
        print(f"{candidate['nodeId']:44s} {candidate['placements']:6d}  {name}")
    print(f"... {len(candidates)} distinct node ids, {total_placements} placements "
          f"in {len(per_file)} workflows; {in_dump} joined with the dump")
    if unreadable:
        print(f"UNREADABLE workflows ({len(unreadable)}):")
        for line in unreadable:
            print(f"  {line}")
    print(f"wrote {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
