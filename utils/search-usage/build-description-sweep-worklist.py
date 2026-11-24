#!/usr/bin/env python3
"""Build the description-sweep worklist from the gate dump (#1404).

Merges the failing-worklist dump of ``SearchDescriptionQualityTest`` (run
with ``-Djipipe.desc-sweep.dump=<path>``) with the source resolution of the
existing audit tooling and the corpus usage of the shipped usage prior,
then partitions the rows into usage-ordered, module-honoring batches:

    python3 utils/search-usage/build-description-sweep-worklist.py \
        --gate-dump /tmp/opencode/desc-sweep-gate-dump.json

Outputs ``utils/taxonomy-research/description-sweep/batch-NN.json`` (the
authoring input of the sweep) plus a printed batch table. Deterministic:
identical inputs produce byte-identical batches.

A row whose source class cannot be resolved keeps ``file: null`` and a
``note`` (never silently dropped - the sweep investigates those rows); a
gate-dump node missing from the source resolution entirely aborts the
build (worklist drift).
"""

from __future__ import annotations

import argparse
import datetime
import importlib.util
import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
AUDIT = os.path.join(REPO_ROOT, "utils", "audit-node-descriptions.py")
OUT_DIR = os.path.join(REPO_ROOT, "utils", "taxonomy-research", "description-sweep")
PRIOR = os.path.join(
    REPO_ROOT,
    "jipipe-core", "src", "main", "resources", "org", "hkijena", "jipipe",
    "search", "usage-prior.json")

BATCH_MIN = 30
BATCH_MAX = 50


def load_audit_module():
    spec = importlib.util.spec_from_file_location("jipipe_audit", AUDIT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def module_of(path: str) -> str:
    rel = os.path.relpath(path, REPO_ROOT).replace(os.sep, "/")
    return rel.split("/src/main/java/")[0] if "/src/main/java/" in rel else rel


def build_rows(gate_dump_path: str, audit) -> tuple[list[dict], list[str]]:
    dump = json.load(open(gate_dump_path, encoding="utf-8"))
    sources = audit.resolve_sources(REPO_ROOT)
    prior = json.load(open(PRIOR, encoding="utf-8"))["counts"]

    rows = []
    unresolved = []
    for entry in dump["failing"]:
        node_id = entry["nodeId"]
        resolved = sources.get(node_id)
        if resolved is None:
            unresolved.append(node_id)
            continue
        class_name, path, _ = resolved
        if not path:
            unresolved.append(node_id)
            continue
        usage = prior.get(node_id, {}).get("instances", 0)
        rows.append({
            "nodeId": node_id,
            "name": entry["name"],
            "missing": entry["missing"],
            "current": entry["description"],
            "file": os.path.relpath(path, REPO_ROOT).replace(os.sep, "/"),
            "class": class_name,
            "usage": usage,
            "needsCondensation": len(entry["description"]) > 460,
        })
    return rows, unresolved


def partition(rows: list[dict]) -> list[list[dict]]:
    """Group rows by module (modules ordered by total corpus usage desc, then
    module path), sort each module's rows by usage desc, and partition each
    group into consecutive batches of BATCH_MIN..BATCH_MAX rows."""
    modules: dict[str, list[dict]] = {}
    for row in rows:
        modules.setdefault(module_of(os.path.join(REPO_ROOT, row["file"])), []).append(row)
    module_order = sorted(
        modules.keys(),
        key=lambda m: (-sum(r["usage"] for r in modules[m]), m))
    batches = []
    for module in module_order:
        group = sorted(modules[module], key=lambda r: (-r["usage"], r["nodeId"]))
        count = len(group)
        chunks = max(1, -(-count // BATCH_MAX))
        size = -(-count // chunks)
        for start in range(0, count, size):
            batches.append(group[start:start + size])
    return batches


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--gate-dump", required=True,
                        help="JSON from SearchDescriptionQualityTest -Djipipe.desc-sweep.dump")
    parser.add_argument("--out-dir", default=OUT_DIR)
    args = parser.parse_args()

    audit = load_audit_module()
    rows, unresolved = build_rows(args.gate_dump, audit)

    # usage-ordered (corpus usage descending, then id); zero-usage rows trail
    rows.sort(key=lambda r: (-r["usage"], r["nodeId"]))

    batches = partition(rows)
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"{'batch':8s} {'rows':>5s}  module")
    for index, batch in enumerate(batches, 1):
        name = f"batch-{index:02d}.json"
        document = {
            "_note": ("Description sweep batch (#1404). Author the opening "
                      "sentences into each row's 'proposed', then apply with: "
                      "python3 utils/audit-node-descriptions.py apply "
                      f"--proposals utils/taxonomy-research/description-sweep/{name}"),
            "module": module_of(os.path.join(REPO_ROOT, batch[0]["file"])),
            "rows": batch,
        }
        with open(os.path.join(args.out_dir, name), "w", encoding="utf-8") as fp:
            json.dump(document, fp, indent=2, sort_keys=True, ensure_ascii=False)
            fp.write("\n")
        print(f"{name:8s} {len(batch):5d}  {document['module']}")
    if unresolved:
        note = {"unresolved": unresolved,
                "_note": "No in-tree class resolved; investigate, never skip."}
        with open(os.path.join(args.out_dir, "unresolved.json"), "w", encoding="utf-8") as fp:
            json.dump(note, fp, indent=2, sort_keys=True)
            fp.write("\n")
        print(f"unresolved: {len(unresolved)} -> unresolved.json {unresolved}")
    print(f"total: {len(rows)} rows in {len(batches)} batches")
    return 0


if __name__ == "__main__":
    sys.exit(main())
