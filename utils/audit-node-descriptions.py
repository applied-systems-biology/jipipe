#!/usr/bin/env python3
"""Draft and apply improved node descriptions (the (C) review decision 3).

Full-audit worklist: ``utils/taxonomy-research/node-description-audit-plan.json``
(the ``wrong`` / ``missing`` / ``thin`` buckets — the editable, pattern-relevant
node types). For each one this tool

1. **draft** — sends the node's real evidence to the LLM (node name + id, the
   input/output data types, the current description, and the class source
   itself) with the defect made explicit (a ``wrong`` description — copy-pasted
   from a sibling — is a different instruction than ``thin`` or ``missing``) and
   the same audience rules the (C) re-run uses (two audiences: bioinformaticians
   and biologists; outcome first, then the technical specifics; no JIPipe
   data-model jargon; 2D/3D stated for ROI nodes). The response is a single
   ``{"description": "…"}`` JSON object, capped (fail-closed over-length).
   Drafts are cached by prompt sha-256 (gitignored) and written to
   ``node-description-audit-proposals.json`` for review.

2. **apply** — splices each reviewed proposal into the
   ``@SetJIPipeDocumentation`` annotation of the owning class. The edit targets
   only the class-level annotation's ``description`` value: it replaces a
   (possibly multi-line) string-literal concatenation, fills an empty
   ``description = ""``, or inserts a missing ``description`` attribute —
   leaving the ``name`` and any other attributes (e.g.
   ``descriptionResourceURL``) untouched. Java escapes (``\\`` ``\"``) are
   written correctly and newlines are collapsed to spaces (a raw newline inside
   a string literal is invalid Java).

Acceptance: after ``apply``, re-running
``utils/extract-node-descriptions.py`` must yield exactly the proposed text for
every node (the round-trip through the extractor is the test), and the touched
modules must compile.

Usage (from the repository root):

    python3 utils/audit-node-descriptions.py selftest
    python3 utils/audit-node-descriptions.py draft [--limit N]
    python3 utils/audit-node-descriptions.py apply   # after reviewing the proposals
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
PLAN = os.path.join(RESEARCH, "node-description-audit-plan.json")
PROPOSALS = os.path.join(RESEARCH, "node-description-audit-proposals.json")
CACHE = os.path.join(RESEARCH, "dag-llm-cache.json")
JUDGE_SCRIPT = os.path.join(REPO_ROOT, "utils", "judge-dag-patterns.py")

# fail-closed cap: a node description that long belongs in the (deferred)
# multi-section documentation system, not in the annotation
MAX_DESC_CHARS = 500
# the class source is evidence, not a dump: cap it for the prompt
MAX_SOURCE_CHARS = 12000

# the editable buckets (the rest of the plan is not_editable_here / ok)
EDITABLE_BUCKETS = ("wrong", "missing", "thin")


def _judge_module():
    spec = importlib.util.spec_from_file_location("judge_dag_patterns", JUDGE_SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


# --------------------------------------------------------------------------- #
# Source editing (unit-tested)
# --------------------------------------------------------------------------- #

def escape_java(text: str) -> str:
    """Make ``text`` safe inside a Java string literal (one line)."""
    out = []
    for ch in text:
        if ch == "\\":
            out.append("\\\\")
        elif ch == '"':
            out.append('\\"')
        elif ch in "\n\r\t":
            out.append(" ")
        elif ord(ch) < 0x20:
            out.append(" ")
        else:
            out.append(ch)
    return "".join(out)


def find_class_annotation(text: str):
    """(block_start, block_end, paren_open, paren_close) of the FIRST
    ``@SetJIPipeDocumentation(…)`` in ``text`` — the class-level one (it
    precedes any parameter-level annotations). None if absent."""
    m = re.search(r"@SetJIPipeDocumentation\s*\(", text)
    if not m:
        return None
    open_p = m.end() - 1
    depth = 0
    for i in range(open_p, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return m.start(), i + 1, open_p, i
    return None


def _scan_literal_run(s: str):
    """Read a Java string-literal concatenation at the value position (right
    after ``=``): returns (decoded_text, consumed_length). Stops at the first
    character that is not whitespace / ``+`` / ``\"``."""
    # same semantics as the extractor's reader, but returns the span length
    raw_parts = []
    i = 0
    n = len(s)
    start = i
    while i < n:
        while i < n and (s[i].isspace() or s[i] == "+"):
            i += 1
        if i >= n or s[i] != '"':
            break
        if start == i:
            start = i
        i += 1
        buf = []
        while i < n:
            c = s[i]
            if c == "\\" and i + 1 < n:
                buf.append(c)
                buf.append(s[i + 1])
                i += 2
                continue
            if c == '"':
                i += 1
                break
            buf.append(c)
            i += 1
        raw_parts.append("".join(buf))
    if not raw_parts:
        return None, (start if start < i else 0)
    decoded = "".join(_decode_literal(p) for p in raw_parts)
    return decoded, i - start


def _decode_literal(raw: str) -> str:
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
            out.append(nxt)
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def apply_description(text: str, new_desc: str) -> str:
    """Patch the class-level ``@SetJIPipeDocumentation`` description in the
    Java source ``text`` to ``new_desc``. Replaces an existing (multi-line
    concatenated) literal run, fills an empty one, or inserts the attribute if
    it is absent. Only the class-level annotation is touched."""
    found = find_class_annotation(text)
    if found is None:
        raise ValueError("no @SetJIPipeDocumentation annotation found")
    a_start, a_end, open_p, close_p = found
    block = text[open_p + 1:close_p]
    literal = f'"{escape_java(new_desc)}"'
    dm = re.search(r"\bdescription\s*=", block)
    if dm:
        value_pos = dm.end()
        run_text, consumed = _scan_literal_run(block[value_pos:])
        value_end = value_pos + consumed
        if run_text is None or consumed == 0:
            # description = <not a literal run> (e.g. a variable) — refuse
            raise ValueError("description value is not a string-literal run")
        # value_pos is right after '='; keep the separating space
        new_block = block[:value_pos] + " " + literal + block[value_end:]
    else:
        stripped = block.strip()
        if stripped:
            new_block = f"{stripped}, description = {literal}"
        else:
            new_block = f"description = {literal}"
    return text[:open_p + 1] + new_block + text[close_p:]


# --------------------------------------------------------------------------- #
# Drafting (unit-tested prompt shape + response parsing)
# --------------------------------------------------------------------------- #

def build_draft_prompt(*, name, node_id, current, bucket, wrong_note,
                       inputs, outputs, source) -> str:
    """The per-node drafting prompt. ``inputs``/``outputs`` are the registry
    slot lists ([{class, dataType}]); ``source`` is the (capped) class source."""
    def slots(lst):
        if not lst:
            return "  (none)"
        return "\n".join(f"  - {s['class'].rsplit('.', 1)[-1]}"
                         for s in lst)
    if bucket == "wrong":
        defect = (f"The CURRENT description is WRONG — it was copy-pasted from a "
                  f"sibling node ({wrong_note}). It does not describe THIS node. "
                  f"Discard it and write the correct one from the source below.")
        current_block = f"Current (WRONG, describes the sibling) description:\n  {current}\n"
    elif bucket == "thin":
        defect = ("The CURRENT description is too thin to tell a user what the "
                  "node does — extend it into a real description.")
        current_block = f"Current (thin) description:\n  {current}\n"
    else:
        defect = ("The node has NO description (missing) — write one from "
                  "scratch from the source below.")
        current_block = ""
    return f"""You are writing the user-facing documentation description of ONE JIPipe algorithm node.

Node: {name} (id: {node_id})

{defect}

{current_block}Input data types the node receives:
{slots(inputs)}

Output data types the node produces:
{slots(outputs)}

The node's own source code (ground truth of what it does):
```java
{source}
```

Write ONE description for this node.

Audience: TWO readers — bioinformaticians who know image-analysis tools, AND
biologists who do not. Requirements:
- State concretely what the node TAKES IN, what it DOES, and what it PRODUCES
  (the data types above are the anchors).
- Lead with the outcome in plain terms; the technical specifics follow in the
  same sentence or a second one.
- NEVER use JIPipe data-model jargon ("path", "annotated path", "slot", "graph",
  "compartment", "wrapper", "hyperstack"). Say what the analyst gets instead
  (e.g. "a list of image files", not "annotated paths").
- If the node operates on ROIs, say "2D ROI" or "3D ROI" — they are different
  data types and must not be conflated.
- 1-3 sentences, at most ~400 characters. Plain prose; a single inline <code>
  is allowed for a formula. No line breaks, no markdown.

Answer with ONE JSON object only:
{{"description": "the new description"}}
"""


def parse_draft(response, max_chars: int = MAX_DESC_CHARS):
    """The description string out of the model response, or None (malformed /
    over the cap — fail-closed). ``response`` is the parsed verdict dict (the
    model answers ``{"description": …}``) or raw text (defensively)."""
    if isinstance(response, dict):
        d = response.get("description")
    elif isinstance(response, str) and response:
        try:
            v = json.loads(response)
        except ValueError:
            m = re.search(r"\{.*\}", response, re.S)
            v = None
            if m:
                try:
                    v = json.loads(m.group(0))
                except ValueError:
                    v = None
        d = v.get("description") if isinstance(v, dict) else None
    else:
        d = None
    if not isinstance(d, str):
        return None
    d = re.sub(r"\s+", " ", d).strip()
    if not d or len(d) > max_chars:
        return None
    return d


# --------------------------------------------------------------------------- #
# CLI: draft / apply
# --------------------------------------------------------------------------- #

def load_worklist():
    plan = json.load(open(PLAN, encoding="utf-8"))
    rows = [r for r in plan["worklist"] if r["bucket"] in EDITABLE_BUCKETS]
    # highest impact first (the plan is already ranked that way)
    return rows


def resolve_sources(repo: str):
    """node_id -> (class_name, file_path, file_text) for the editable rows,
    reusing the extractor's registration scan."""
    spec = importlib.util.spec_from_file_location(
        "extract_node_descriptions",
        os.path.join(REPO_ROOT, "utils", "extract-node-descriptions.py"))
    EX = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(EX)
    id2class = {}
    class2file = {}
    for path in EX.iter_source_files(repo):
        try:
            txt = open(path, encoding="utf-8", errors="ignore").read()
        except OSError:
            continue
        id2class.update(EX.extract_id_class_map(txt))
        cls = EX.top_level_class_name(txt)
        if cls:
            class2file.setdefault(cls, (path, txt))
    out = {}
    for nid, cls in id2class.items():
        if cls in class2file:
            path, txt = class2file[cls]
            out[nid] = (cls, path, txt)
    return out


def capped_source(text: str) -> str:
    if len(text) <= MAX_SOURCE_CHARS:
        return text
    # keep the annotation header (slots + docs) and as much class body as fits
    m = re.search(r"@SetJIPipeDocumentation", text)
    if m and m.start() < MAX_SOURCE_CHARS:
        return text[m.start():m.start() + MAX_SOURCE_CHARS] + "\n// …truncated…\n"
    return text[:MAX_SOURCE_CHARS] + "\n// …truncated…\n"


def cmd_draft(limit=0):
    J = _judge_module()
    rows = load_worklist()
    if limit > 0:
        rows = rows[:limit]
    sources = resolve_sources(REPO_ROOT)
    reg = {n["id"]: n for n in
           json.load(open(os.path.join(RESEARCH, "registry.json"),
                          encoding="utf-8"))["nodes"]}
    cache = J.load_cache(CACHE)
    client = J.LLMClient.from_credentials()

    existing = {}
    if os.path.isfile(PROPOSALS):
        existing = {p["nodeId"]: p for p in
                    json.load(open(PROPOSALS, encoding="utf-8"))["proposals"]}

    proposals = []
    calls = 0
    for r in rows:
        nid = r["nodeId"]
        # incremental: a drafted proposal is reused (delete the proposals file
        # to force a full re-draft)
        if nid in existing and existing[nid].get("proposed"):
            proposals.append(existing[nid])
            continue
        cls_name, path, txt = sources.get(nid, (None, None, None))
        if txt is None:
            # the plan says editable, but no in-tree class resolved — record
            # it as unresolvable instead of silently skipping
            proposals.append({"nodeId": nid, "file": None, "class": None,
                              "bucket": r["bucket"], "current": r.get("desc"),
                              "proposed": None,
                              "note": "no in-tree class resolved"})
            continue
        node = reg.get(nid, {})
        prompt = build_draft_prompt(
            name=node.get("name", nid), node_id=nid,
            current=r.get("desc") or "", bucket=r["bucket"],
            wrong_note=(None if r["bucket"] != "wrong" else
                        _wrong_note(nid)),
            inputs=node.get("inputs", []), outputs=node.get("outputs", []),
            source=capped_source(txt))
        verdict = J.fetch_verdict(client.complete, cache, prompt)
        calls += 1
        # verdict is the model's {"description": …} dict (or None if the
        # endpoint failed / no JSON) — parse_draft caps + fail-closes on it
        desc = parse_draft(verdict)
        proposals.append({
            "nodeId": nid, "file": path, "class": cls_name,
            "bucket": r["bucket"], "current": r.get("desc"),
            "proposed": desc,
            "note": None if desc else "draft failed (malformed/over cap) — retry",
        })
    J.save_cache(CACHE, cache)
    with open(PROPOSALS, "w", encoding="utf-8") as fh:
        json.dump({"_note": ("LLM-drafted replacement descriptions for the "
                             "wrong/missing/thin pattern-relevant node types "
                             "(decision 3). Review, then: python3 "
                             "utils/audit-node-descriptions.py apply"),
                   "proposals": proposals},
                  fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    ok = sum(1 for p in proposals if p["proposed"])
    print(f"proposals: {ok}/{len(proposals)} drafted ({calls} endpoint calls)")
    bad = [p["nodeId"] for p in proposals if not p["proposed"]]
    if bad:
        print("failed:", bad)
    return 0


def _wrong_note(node_id: str) -> str:
    spec = importlib.util.spec_from_file_location(
        "classify_node_desc_audit",
        os.path.join(REPO_ROOT, "utils", "classify-node-description-audit.py"))
    C = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(C)
    return C.WRONG_COPY_PASTE.get(node_id, "a sibling node")


def cmd_apply():
    doc = json.load(open(PROPOSALS, encoding="utf-8"))
    by_node = {}
    changed = []
    for p in doc["proposals"]:
        if not p.get("proposed") or not p.get("file"):
            continue
        path = p["file"]
        if path in by_node:
            # two node ids share one class file — apply sequentially in memory
            by_node.setdefault(path, []).append(p)
        else:
            by_node[path] = [p]
    for path, ps in sorted(by_node.items()):
        txt = open(path, encoding="utf-8").read()
        for p in ps:
            txt = apply_description(txt, p["proposed"])
            changed.append((p["nodeId"], path))
        open(path, "w", encoding="utf-8").write(txt)
    print(f"applied {len(changed)} descriptions to {len(by_node)} files")
    for nid, path in changed:
        print(f"  {nid:44s} {path.replace(REPO_ROOT + '/', '')}")
    print("\nverify: python3 utils/extract-node-descriptions.py  (round-trip)")
    return 0


def selftest():
    tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "audit_node_descriptions_tests.py")
    old = sys.argv
    sys.argv = [tests]
    try:
        import runpy
        runpy.run_path(tests, run_name="__main__")
        return 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 1
    finally:
        sys.argv = old


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("mode", nargs="?", default="help",
                    choices=["help", "selftest", "draft", "apply"])
    ap.add_argument("--limit", type=int, default=0,
                    help="draft only the first N worklist rows (pilot)")
    args = ap.parse_args(argv)
    if args.mode == "selftest":
        return selftest()
    if args.mode == "draft":
        return cmd_draft(args.limit)
    if args.mode == "apply":
        return cmd_apply()
    ap.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
