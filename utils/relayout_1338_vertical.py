#!/usr/bin/env python3
"""1338 scoped re-layout: re-lay out the bundled linear + derived patterns
VERTICALLY (layers top-to-bottom, the app's flow direction) in place.

Why a scoped transform instead of re-running the generators:
  * The full pipeline (linear gen -> DAG gen -> B1 restore) is not
    reproducible on this machine: the B1 restore pass needs corpus matches
    at /data/JIPipe and hard-fails on dag-mined-cluster-0276 (a converter
    whose corpus instance no longer covers it).
  * The DAG generator rebuilds MINED files from the research artifacts, and
    the B1 restore pass re-adds the corpus-faithful slot configs. Re-running
    it would strip/rewrite 51 mined files that are byte-stable in the repo.
  * The layout only needs the width map, which is file-LOCAL
    (restore_mod.extras_for_graph + width_map_for_graph over the committed
    graph's own jipipe:slot-configuration + the static registry) - exactly
    what the generators compute at layout time (Ruling 12).

Therefore: read each committed linear (chain-*/annotation-sandwich-*) and
derived (dag-derived-*) file, re-lay it out with the transposed
sugiyama_layout.relocate_graph (1338), and write it back with the generator
serialization. Mined (dag-mined-*) files are left byte-identical.

Run: python3 utils/relayout_1338_vertical.py
"""
import glob
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import restore_pattern_slot_configs as restore_mod  # noqa: E402
from sugiyama_layout import relocate_graph  # noqa: E402

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(
    REPO_ROOT, "jipipe-core", "src", "main", "resources",
    "org", "hkijena", "jipipe", "plugins", "nodetemplate", "patterns")
REGISTRY_FILE = os.path.join(REPO_ROOT, "utils", "taxonomy-research", "registry.json")

LINEAR_PREFIXES = ("chain-", "annotation-sandwich-")
DERIVED_PREFIX = "dag-derived-"
MINED_PREFIX = "dag-mined-"


def main():
    with io.open(REGISTRY_FILE, encoding="utf-8") as fh:
        registry = {n["id"]: n for n in json.load(fh)["nodes"]}

    files = sorted(glob.glob(os.path.join(OUT_DIR, "*.json")))
    touched = 0
    skipped_mined = 0
    for path in files:
        name = os.path.basename(path)
        if name == "system-templates.json":
            continue
        if name.startswith(MINED_PREFIX):
            skipped_mined += 1
            continue  # mined: keep verbatim (mined invariance)
        if not (name.startswith(LINEAR_PREFIXES) or name.startswith(DERIVED_PREFIX)):
            print(f"UNEXPECTED FILE (not linear/derived/mined): {name}", file=sys.stderr)
            sys.exit(1)

        with io.open(path, encoding="utf-8") as fh:
            g = json.load(fh)
        # Same width rule the generators apply at layout time (Ruling 12 /
        # Global-Constraints rule 5): static registry slots + required
        # extras (file-local), plus the restored slot config's non-static
        # slots (width_map_for_graph adds them).
        extras = restore_mod.extras_for_graph(g, registry)
        widths = restore_mod.width_map_for_graph(g, extras, registry)
        relocate_graph(g, widths, rekey_legacy=None)  # compartments already final
        with io.open(path, "w", encoding="utf-8") as fh:
            fh.write(json.dumps(g, ensure_ascii=False,
                                separators=(",", ":")) + "\n")
        touched += 1

    print(f"re-laid out (vertical): {touched}")
    print(f"left verbatim (mined) : {skipped_mined}")
    assert touched == 178, f"expected 178 linear+derived files, touched {touched}"
    assert skipped_mined == 242, f"expected 242 mined files, saw {skipped_mined}"
    print("OK")


if __name__ == "__main__":
    main()
