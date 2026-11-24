#!/usr/bin/env python3
"""Generate the shipped node-usage prior resource (#1404).

Mines a corpus of real-world JIPipe projects (``*.jip`` files) and writes
the committed usage-prior resource the enhanced node search ranks with:

    python3 utils/search-usage/generate-usage-prior.py \
        --corpus "/data/JIPipe/Example workflows" \
        --registry-dump /tmp/opencode/node-registry-dump.json \
        --out jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json

- ``--corpus``        directory of ``*.jip`` projects (never hard-coded;
                      the committed artifact is the generated resource)
- ``--registry-dump`` JSON object keyed by registered node id (produced by
                      ``NodeRegistryDumpToolTest`` with
                      ``-Djipipe.dump.path=...``); ids absent from the dump
                      are dropped (ids that are not node types in the
                      current build, e.g. from other plugin versions).
                      Canvas notes, comments, groups, and compartment I/O
                      ARE registered node types and stay in the prior
- ``--out``           output path (the committed resource)

Deterministic: identical inputs produce a byte-identical file (sorted
keys, stable ``generated`` date format, corpus hash over sorted
``(filename, bytes)`` pairs).
"""

from __future__ import annotations

import argparse
import datetime
import glob
import hashlib
import json
import os
import sys


def mine_corpus(corpus_dir: str) -> tuple[dict[str, dict[str, int]], int]:
    """Returns ({node_id: {instances, pipelines}}, file_count)."""
    counts: dict[str, dict[str, int]] = {}
    files = sorted(glob.glob(os.path.join(corpus_dir, "*.jip")))
    for path in files:
        with open(path, encoding="utf-8") as fp:
            project = json.load(fp)
        nodes = (project.get("graph") or {}).get("nodes") or {}
        ids_in_file = set()
        for node in nodes.values():
            node_id = node.get("jipipe:node-info-id")
            if not node_id:
                continue
            entry = counts.setdefault(node_id, {"instances": 0, "pipelines": 0})
            entry["instances"] += 1
            ids_in_file.add(node_id)
        for node_id in ids_in_file:
            counts[node_id]["pipelines"] += 1
    return counts, len(files)


def corpus_sha256(corpus_dir: str) -> str:
    digest = hashlib.sha256()
    files = sorted(glob.glob(os.path.join(corpus_dir, "*.jip")))
    for path in files:
        rel = os.path.relpath(path, corpus_dir).replace(os.sep, "/")
        with open(path, "rb") as fp:
            digest.update(rel.encode("utf-8"))
            digest.update(b"\0")
            digest.update(fp.read())
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--corpus", required=True, help="directory of *.jip projects")
    parser.add_argument("--registry-dump", required=True,
                        help="JSON object keyed by registered node id (NodeRegistryDumpToolTest)")
    parser.add_argument("--out", required=True, help="output JSON path")
    args = parser.parse_args()

    with open(args.registry_dump, encoding="utf-8") as fp:
        registered = set(json.load(fp).keys())

    counts, file_count = mine_corpus(args.corpus)
    # Only ids that are node types in the current build enter the prior:
    # the registry filter drops ids from other plugin versions. Canvas
    # notes, comments, groups, and compartment I/O are registered node
    # types and stay (Ruling, Task 2).
    filtered = {node_id: entry for node_id, entry in counts.items() if node_id in registered}

    document = {
        "source": "mined from real-world JIPipe example workflows (jipipe:node-info-id occurrences)",
        "generated": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d"),
        "corpus": {
            "files": file_count,
            "sha256": corpus_sha256(args.corpus),
        },
        "counts": filtered,
    }

    os.makedirs(os.path.dirname(os.path.abspath(args.out)) or ".", exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fp:
        json.dump(document, fp, indent=2, sort_keys=True, ensure_ascii=False)
        fp.write("\n")

    print(f"wrote {len(filtered)} entries ({sum(e['instances'] for e in filtered.values())} instances "
          f"from {file_count} projects) to {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
