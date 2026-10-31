#!/usr/bin/env python3
"""Extract node descriptions from the JIPipe source tree (research/offline tool).

The mining pipeline's ``registry.json`` is structure-only (node id, name,
functions, slots) - it carries **no** node descriptions, which is why the step
(C) LLM had to infer what each node does from its short display name alone
(the jargon names, unqualified ROI names and thin descriptions the user
flagged in the (C) review). This script recovers the real descriptions from
the ``@SetJIPipeDocumentation(name=…, description=…)`` annotations on each
algorithm class.

Linking a node id to its class:

* plugins register nodes with
  ``registerNodeType("<id>", <Class>.class, …)`` (simple form) or
  ``registerNodeType(new JIPipeJavaNodeRegistrationTask("<id>", <Class>.class, …))``
  (task form). Both are scanned.
* the class's ``@SetJIPipeDocumentation`` (class-level, the first one in the
  file) holds the ``description``. Java string-literal concatenations that span
  lines are joined; the common escapes (``\\"`` ``\\\\`` ``\\n`` ``\\t``) are
  decoded (``\\n``/``\\t`` become spaces - descriptions are prose, not
  line-broken, so a single logical line is what the pipeline and the prompt
  want).

Output: a JSON map ``{node-info-id: description}`` (sorted, the nodes that
have a description; ~300 of the 326 node types the shipped patterns use -
the ~26 without are ``clij:plugin:*`` / ``jipipe2ai-*`` / auto-registered
nodes, which fall back to the node name in the pipeline).

Usage (from the repository root):

    python3 utils/extract-node-descriptions.py selftest
    python3 utils/extract-node-descriptions.py [--repo <root>] [--out <map.json>]
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_OUT = os.path.join(
    REPO_ROOT, "utils", "taxonomy-research", "node-descriptions.json")

# The source roots that hold algorithm classes + their registrations.
SOURCE_SUBDIRS = ("jipipe-core/src/main", "plugins")

_SKIP_PART = ("target", "build", "test", "Test")


# --------------------------------------------------------------------------- #
# Parsing helpers (unit-tested, no filesystem)
# --------------------------------------------------------------------------- #

def _decode_java_literal(raw: str) -> str:
    """Decode the escape sequences of a single Java string literal body."""
    out = []
    i = 0
    n = len(raw)
    while i < n:
        c = raw[i]
        if c == "\\" and i + 1 < n:
            nxt = raw[i + 1]
            if nxt == "u" and i + 5 < n:
                try:
                    out.append(chr(int(raw[i + 2:i + 6], 16)))
                    i += 6
                    continue
                except ValueError:
                    pass
            if nxt in ('"', "\\"):
                out.append(nxt)
                i += 2
                continue
            if nxt in ("n", "t", "r"):
                out.append(" ")  # whitespace in a description -> a single space
                i += 2
                continue
            out.append(nxt)
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def _read_concat_literals(s: str) -> str:
    """Read a Java string-literal concatenation starting at ``s`` (the value
    position, i.e. right after ``=``): ``"a" + "b" + "c"`` -> ``"abc"``.
    Stops at the first character that is not whitespace/``+``/``"``."""
    parts = []
    i = 0
    n = len(s)
    while i < n:
        while i < n and (s[i].isspace() or s[i] == "+"):
            i += 1
        if i >= n or s[i] != '"':
            break
        i += 1
        raw = []
        while i < n:
            c = s[i]
            if c == "\\" and i + 1 < n:
                raw.append(c)
                raw.append(s[i + 1])
                i += 2
                continue
            if c == '"':
                i += 1
                break
            raw.append(c)
            i += 1
        parts.append(_decode_java_literal("".join(raw)))
    return "".join(parts).strip()


def extract_description(text: str):
    """The first ``@SetJIPipeDocumentation`` description in ``text``, or None
    if absent/empty. Anchored on the ``@`` usage - the string also appears in
    the import line, and matching that (then finding the NEXT ``(``, which may
    sit in a javadoc) silently returns the wrong block or None."""
    m = re.search(r"@SetJIPipeDocumentation\s*\(", text)
    if not m:
        return None
    open_paren = m.end() - 1
    # find the matching close paren (annotations don't nest parens meaningfully,
    # but balance to be safe)
    depth = 0
    close_paren = -1
    for i in range(open_paren, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                close_paren = i
                break
    if close_paren == -1:
        return None
    block = text[open_paren + 1:close_paren]
    dm = re.search(r"^\s*description\s*=\s*", block, re.S)
    if not dm:
        # description might not be first; search anywhere in the block
        dm = re.search(r"\bdescription\s*=\s*", block)
        if not dm:
            return None
    value = _read_concat_literals(block[dm.end():])
    return value or None


def extract_id_class_map(text: str) -> dict:
    """Map node-info-id -> algorithm class name from the node-registration
    calls in ``text``. Both registration methods (``registerNodeType`` and
    ``registerAnnotationNodeType``, the latter for canvas-note nodes) and both
    call forms (simple id+class, and the new-task object) are matched."""
    out = {}
    for m in re.finditer(
            r"register(?:Annotation)?NodeType\(\s*(?:new\s+[A-Za-z0-9_]+\s*\(\s*)?"
            r'"([^"]+)"\s*,\s*([A-Za-z0-9_]+)\s*\.\s*class', text):
        out.setdefault(m.group(1), m.group(2))
    return out


_TOP_CLASS = re.compile(
    r"^(?:public\s+|final\s+|abstract\s+|sealed\s+)*"
    r"(?:class|interface|enum)\s+([A-Za-z0-9_]+)", re.M)


def top_level_class_name(text: str):
    m = _TOP_CLASS.search(text)
    return m.group(1) if m else None


# --------------------------------------------------------------------------- #
# Whole-tree extraction
# --------------------------------------------------------------------------- #

def iter_source_files(repo: str):
    for sub in SOURCE_SUBDIRS:
        base = os.path.join(repo, sub)
        if not os.path.isdir(base):
            continue
        for dirpath, dirnames, filenames in os.walk(base):
            dirnames[:] = [d for d in dirnames
                           if not any(s in d for s in _SKIP_PART)]
            for fn in filenames:
                if fn.endswith(".java") and "Test" not in fn:
                    yield os.path.join(dirpath, fn)


def extract_all(repo: str) -> dict:
    """{node-info-id: description} for the whole source tree under ``repo``."""
    id2class = {}
    class2desc = {}
    for path in iter_source_files(repo):
        try:
            with open(path, encoding="utf-8", errors="ignore") as fh:
                text = fh.read()
        except OSError:
            continue
        id2class.update(extract_id_class_map(text))
        cls = top_level_class_name(text)
        if cls:
            desc = extract_description(text)
            if desc:
                class2desc[cls] = desc
    result = {}
    for nid, cls in id2class.items():
        if cls in class2desc:
            result[nid] = class2desc[cls]
    return result


def selftest():
    tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "extract_node_descriptions_tests.py")
    old_argv = sys.argv
    sys.argv = [tests]
    try:
        import runpy
        runpy.run_path(tests, run_name="__main__")
        return 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 1
    finally:
        sys.argv = old_argv


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Extract node descriptions from the JIPipe source tree.")
    parser.add_argument("mode", nargs="?", default="run",
                        help="'run' (default) | 'selftest'")
    parser.add_argument("--repo", default=REPO_ROOT)
    parser.add_argument("--out", default=DEFAULT_OUT)
    args = parser.parse_args(argv)

    if args.mode == "selftest":
        return selftest()

    result = extract_all(args.repo)
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")
    print(f"extracted {len(result)} node descriptions -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
