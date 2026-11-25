#!/usr/bin/env python3
"""Structural pre-check for description-sweep proposals (#1404).

Verifies what does not need the Java gate: every proposal has a non-empty
``proposed`` text within the 500-character draft cap, carries no em/en
dashes, and points at an existing file. The word-coverage judgment itself
is the gate's (``SearchDescriptionQualityTest``) - this script is only the
fast pre-filter that saves an apply cycle.

    python3 utils/search-usage/check-description-proposals.py \
        utils/taxonomy-research/description-sweep/proposals-batch-01.json [more files...]
"""

from __future__ import annotations

import json
import os
import sys

MAX_DESC_CHARS = 500  # the apply tool's draft cap


def check_file(path: str) -> list[str]:
    problems = []
    document = json.load(open(path, encoding="utf-8"))
    for proposal in document.get("proposals", []):
        node_id = proposal.get("nodeId", "<no id>")
        proposed = proposal.get("proposed") or ""
        if not proposed.strip():
            problems.append(f"{node_id}: empty proposed text")
        if len(proposed) > MAX_DESC_CHARS:
            problems.append(f"{node_id}: proposed text is {len(proposed)} chars (cap {MAX_DESC_CHARS})")
        if "\u2014" in proposed or "\u2013" in proposed:
            problems.append(f"{node_id}: proposed text contains an em/en dash")
        file_path = proposal.get("file")
        if not file_path or not os.path.isfile(file_path):
            problems.append(f"{node_id}: file missing or not found: {file_path}")
    return problems


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    all_problems = []
    for path in sys.argv[1:]:
        all_problems.extend(check_file(path))
    if all_problems:
        for problem in all_problems:
            print("PROBLEM:", problem)
        return 1
    print("all proposals pass the structural pre-check")
    return 0


if __name__ == "__main__":
    sys.exit(main())
