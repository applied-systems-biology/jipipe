#!/usr/bin/env python3
"""Gate / name / classify DAG pattern clusters with the LLM (plan step C,
#1324 / #1333).

Consumes step (B)'s ``utils/taxonomy-research/dag-clusters.json`` and asks
the LLM (``vllm/frontier`` via the asb-llm OpenAI-compatible
``/chat/completions`` endpoint; a reasoning model — the answer is the
``content`` after ``reasoning_content``, with the plugin's
``content or reasoning_content`` fallback) ONE structured-JSON question per
cluster:

    {"gate": "general_pattern" | "workflow_plumbing" | "noise",
     "function": one of the 13 top-level branch keys,
     "name": "short general name" | "",
     "description": "1-2 general sentences" | ""}

* **gate** — the enforceable form of plan §1 (a pattern is a reusable,
  general subgraph with a proper derivable name): high support (authored in
  many independent projects) and a non-generic task-describing title are
  strong evidence for ``general_pattern``; bespoke one-project arrangements
  and generic titles ("Analysis", "File handling for X") are plumbing.
* **function** — the 13 top-level taxonomy branches (replaces the
  most-common-ancestor heuristic for DAGs where it is ill-defined).
* **name + description** — the sidecar fields step (D) ships; an empty name
  ⇒ reject (the §1 rule: unnameable ⇒ noise).

Design history: step (C) was first built on the imajev MCQ readout (the
plan §3.3 text). That was replaced on the user's direction: the MCQ's hard
wire constraints (128-char option values, min-2 options, no free-text
output) fight the naming task, and it cannot emit a description. The LLM
produces gate + function + name + description in one ~5 s call at
``temperature=0``; the verdicts are recorded verbatim as the audit trail.

Decisions are cached to ``utils/taxonomy-research/dag-llm-cache.json``
(keyed by SHA-256 of the prompt; gitignored like the (B) cache) so re-runs
and prompt tweaks do not re-call the model.

Usage (from the repository root):

    python3 utils/judge-dag-patterns.py selftest
    python3 utils/judge-dag-patterns.py [--clusters <dag-clusters.json>]
        [--out <dag-judgments.json>] [--cache <dag-llm-cache.json>]
        [--limit N] [--credentials <file>] [--endpoint <url>] [--model <id>]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
import urllib.request
from datetime import datetime, timezone

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
DEFAULT_CLUSTERS = os.path.join(RESEARCH, "dag-clusters.json")
DEFAULT_OUT = os.path.join(RESEARCH, "dag-judgments.json")
DEFAULT_CACHE = os.path.join(RESEARCH, "dag-llm-cache.json")
DEFAULT_CREDENTIALS = ("/home/rgerst/.config/opencode-data-analysis/"
                       "credentials.json")
DEFAULT_BASE_URL = "https://asb-llm.hki-jena.de/v1"
DEFAULT_MODEL = "vllm/frontier"

GATE_OPTIONS = ("general_pattern", "workflow_plumbing", "noise")

# The 13 top-level taxonomy branches (registry.json). Keys are stable
# machine codes; the prompt explains each in one phrase.
BRANCH_HINTS = {
    "annotations": "Annotations (marking data with annotations)",
    "data": "Data sources (importing files, folder lists, project data)",
    "export": "Export (writing data/files out)",
    "filaments": "Filaments (line-shaped objects)",
    "graph": "Graph / project (JIPipe graph and project structure)",
    "images": "Images (image processing and conversion)",
    "machine-learning": "Machine learning (models, training, inference)",
    "misc": "Miscellaneous (other)",
    "plotting": "Plotting (charts and plots)",
    "rois": "ROIs (region-of-interest processing)",
    "statistics": "Statistics (numeric analysis of data)",
    "tables": "Tables (tabular data operations)",
    "tracking": "Tracking (object tracking over time)",
}

# Sidecar sanity caps (the shipped 64 use names <= ~120 chars and short
# descriptions; a runaway LLM string is a parse failure, not a pattern).
MAX_NAME_LEN = 128
MAX_DESCRIPTION_LEN = 600

# The model is a reasoning model: reasoning_content + content share the
# completion budget. 2000 tokens truncated ~25% of verdicts mid-reasoning
# (malformed_verdict with pure reasoning text); 8000 covers the observed
# worst cases (flagship: ~1700 tokens total) with a wide margin.
MAX_TOKENS = 8000
TEMPERATURE = 0.0


# --------------------------------------------------------------------------- #
# State + prompt
# --------------------------------------------------------------------------- #

def _bfs_order(graph: dict):
    """Deterministic BFS-from-sources node order (same as (B)'s canonical
    text; alphabetical among ready nodes)."""
    nodes = graph["nodes"]
    edges = graph["edges"]
    indeg = {k: 0 for k in nodes}
    succ = {k: [] for k in nodes}
    for e in edges:
        if e["source-node"] in succ and e["target-node"] in indeg:
            succ[e["source-node"]].append(e["target-node"])
            indeg[e["target-node"]] += 1
    order = []
    remaining = set(nodes)
    while remaining:
        ready = sorted(k for k in remaining if indeg[k] == 0)
        if not ready:
            ready = [max(remaining)]  # cycle break ((A) guarantees DAGs)
        for k in ready:
            order.append(k)
            remaining.discard(k)
            for s in succ[k]:
                indeg[s] -= 1
    return order


def node_name_of(v: dict, registry_names):
    """Registry display name when known, else the node's label, else the
    node-info-id (the (B) fallback rule)."""
    if registry_names:
        reg = registry_names.get(v["jipipe:node-info-id"])
        if reg:
            return reg
    return (v.get("jipipe:node:name") or v["jipipe:node-info-id"])


def build_state(cluster: dict, registry_names=None) -> dict:
    """The state record sent to the LLM: the cluster's canonical form (what
    (B) merged, what (C) judges)."""
    c = cluster["representativeCandidate"]
    g = c["graph"]
    order = _bfs_order(g)
    names = [node_name_of(g["nodes"][k], registry_names) for k in order]
    name_of = {k: node_name_of(g["nodes"][k], registry_names)
               for k in g["nodes"]}
    wiring = "; ".join(
        f"{name_of[e['source-node']]} {e.get('source-slot', '')} -> "
        f"{name_of[e['target-node']]} {e.get('target-slot', '')}"
        for e in sorted(g["edges"], key=lambda e: (e["source-node"],
                                                   e.get("source-slot", ""),
                                                   e["target-node"],
                                                   e.get("target-slot", ""))))
    titles = list(cluster.get("titleEvidence") or [])
    state = {
        "pattern": {
            "nodeSequence": " -> ".join(names),
            "wiring": wiring,
            "nodeCount": c["nodeCount"],
            "edgeCount": c["edgeCount"],
            "support": cluster["support"],
            "signal": c["signal"],
            "observedTitles": titles,
        },
    }
    if titles:
        state["pattern"]["primaryTitle"] = titles[0]
    return state


def build_prompt(cluster: dict, state: dict) -> str:
    branches = ", ".join(f'"{k}" ({v})' for k, v in sorted(BRANCH_HINTS.items()))
    return f"""You are curating a library of reusable image-analysis pipeline patterns mined from real projects (JIPipe).

For the pattern described in the JSON below, answer with ONE JSON object only:
{{"gate": "general_pattern" | "workflow_plumbing" | "noise",
 "function": one of [{", ".join(sorted(BRANCH_HINTS))}],
 "name": "a short general name of what this pattern does" | "",
 "description": "1-2 sentences describing what this pattern does, in general terms" | ""}}

Rules:
- gate "general_pattern" only if the subgraph is a small self-contained reusable unit doing one clear GENERALIZABLE thing; high support (authored in many independent projects) and a non-generic task-describing title are strong evidence. Wiring data together is not by itself plumbing.
- gate "workflow_plumbing" if it is a bespoke arrangement of one specific project's files/data (generic or project-specific titles like "Analysis", "File handling for X").
- gate "noise" if scratch/ad-hoc with no clear purpose.
- function: the primary purpose of the pattern. Options: {branches}.
- name must be a GENERAL name usable in another project; use the observed titles as inspiration but GENERALIZE them (drop project-specific terms). Empty string if you cannot name it generally.
- description: what it does, in general terms, for a user browsing a pattern library. Empty string when the gate is not "general_pattern".

Pattern JSON:
"""


# --------------------------------------------------------------------------- #
# Verdict parsing + judgment
# --------------------------------------------------------------------------- #

def parse_verdict(text: str):
    """Extract the JSON verdict object from the LLM output (tolerates code
    fences and surrounding prose). Returns the dict or None."""
    if not text:
        return None
    # try direct parse first
    try:
        v = json.loads(text)
        if isinstance(v, dict):
            return v
    except (ValueError, TypeError):
        pass
    # strip code fences / find the outermost {...}
    m = re.search(r"\{.*\}", text, re.S)
    if not m:
        return None
    try:
        v = json.loads(m.group(0))
        return v if isinstance(v, dict) else None
    except (ValueError, TypeError):
        return None


def judge_cluster(cluster: dict, llm, registry_names=None) -> dict:
    """Ask the LLM about one cluster and derive the decision.

    ``llm``: callable(prompt) -> the parsed JSON verdict dict (tests inject a
    deterministic fake; the CLI passes the endpoint client). Returns the
    judgment record with the verdict as the audit trail.
    """
    state = build_state(cluster, registry_names)
    prompt = build_prompt(cluster, state)
    verdict = llm(prompt + json.dumps(state, ensure_ascii=False))
    if isinstance(verdict, str):
        # the raw LLM output (tests + the endpoint client return text) —
        # extract the JSON verdict object from it
        verdict = parse_verdict(verdict)

    gate = verdict.get("gate") if isinstance(verdict, dict) else None
    function_key = verdict.get("function") if isinstance(verdict, dict) else None
    name = (verdict.get("name") or "").strip() if isinstance(verdict, dict) else ""
    description = ((verdict.get("description") or "").strip()
                   if isinstance(verdict, dict) else "")

    function_id = ("jipipe:function:" + function_key
                   if function_key in BRANCH_HINTS else None)

    error = None
    if not isinstance(verdict, dict):
        error = "malformed_verdict"
    elif gate not in GATE_OPTIONS:
        error = "gate_not_recognized"
    elif function_id is None:
        error = "function_not_recognized"
    elif len(name) > MAX_NAME_LEN:
        error = "name_too_long"
    elif len(description) > MAX_DESCRIPTION_LEN:
        error = "description_too_long"

    ship = (error is None
            and gate == "general_pattern"
            and bool(name))

    return {
        "clusterId": cluster["clusterId"],
        "ship": ship,
        "error": error,
        "gate": gate if gate in GATE_OPTIONS else gate,
        "functionId": function_id,
        "name": name if ship else None,
        "description": description if ship else None,
        "support": cluster["support"],
        "representative": cluster["representative"],
        "state": state,
        "verdict": verdict if isinstance(verdict, dict) else None,
    }


def judge_clusters(clusters, llm, registry_names=None) -> list:
    """Judge every cluster, in deterministic (clusterId) order."""
    results = []
    for cluster in sorted(clusters, key=lambda c: c["clusterId"]):
        results.append(judge_cluster(cluster, llm, registry_names))
    return results


# --------------------------------------------------------------------------- #
# Endpoint client
# --------------------------------------------------------------------------- #

class LLMClient:
    """Minimal OpenAI-compatible chat-completions client (urllib; the same
    endpoint/model the data-analysis plugin's vlm_client uses). The model is
    a reasoning model: the answer is ``content`` after ``reasoning_content``
    (fallback: ``content or reasoning_content``, as the plugin does)."""

    def __init__(self, base_url=DEFAULT_BASE_URL, model=DEFAULT_MODEL,
                 api_key=None, timeout=300.0):
        self.url = base_url.rstrip("/") + "/chat/completions"
        self.model = model
        self.api_key = api_key
        self.timeout = timeout
        self.calls = 0

    @classmethod
    def from_credentials(cls, path=DEFAULT_CREDENTIALS,
                         base_url=None, model=None):
        with open(path, encoding="utf-8") as fh:
            creds = json.load(fh)
        return cls(base_url or creds.get("vlm_base_url") or DEFAULT_BASE_URL,
                   model or creds.get("vlm_model") or DEFAULT_MODEL,
                   creds.get("vlm_api_key"))

    def complete(self, prompt):
        body = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": MAX_TOKENS,
            "temperature": TEMPERATURE,
        }
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        req = urllib.request.Request(self.url, data=json.dumps(body).encode(),
                                     headers=headers)
        last = None
        for attempt in range(3):
            try:
                with urllib.request.urlopen(req, timeout=self.timeout) as r:
                    data = json.load(r)
                self.calls += 1
                msg = data["choices"][0]["message"]
                return (msg.get("content") or msg.get("reasoning_content")
                        or "")
            except Exception as e:  # noqa: BLE001 — retried below
                last = e
                if attempt == 2:
                    break
                time.sleep(2 * (attempt + 1))
        raise RuntimeError(f"LLM request failed: {last}")


def load_registry_names(path=None):
    path = path or os.path.join(RESEARCH, "registry.json")
    if not os.path.isfile(path):
        return None
    with open(path, encoding="utf-8") as fh:
        reg = json.load(fh)
    return {n["id"]: n["name"] for n in reg.get("nodes", [])}


def load_cache(path):
    if not os.path.isfile(path):
        return {}
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def save_cache(path, cache):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(cache, fh, ensure_ascii=False)


def selftest():
    tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "judge_dag_patterns_tests.py")
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
        description="Gate/name/classify DAG pattern clusters with the LLM "
                    "(plan step C).")
    parser.add_argument("mode", nargs="?", default="run",
                        help="'run' (default) | 'selftest'")
    parser.add_argument("--clusters", default=DEFAULT_CLUSTERS)
    parser.add_argument("--out", default=DEFAULT_OUT)
    parser.add_argument("--cache", default=DEFAULT_CACHE)
    parser.add_argument("--credentials", default=DEFAULT_CREDENTIALS)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--limit", type=int, default=0,
                        help="judge only the first N clusters (pilot runs)")
    args = parser.parse_args(argv)

    if args.mode == "selftest":
        return selftest()

    with open(args.clusters, encoding="utf-8") as fh:
        doc = json.load(fh)
    clusters = doc["clusters"]
    if args.limit > 0:
        clusters = clusters[:args.limit]
    registry_names = load_registry_names()
    client = LLMClient.from_credentials(args.credentials, args.base_url,
                                        args.model)
    cache = load_cache(args.cache)

    def llm_cached(prompt):
        digest = hashlib.sha256(prompt.encode("utf-8")).hexdigest()
        if digest in cache:
            return parse_verdict(cache[digest])
        text = client.complete(prompt)
        cache[digest] = text
        return parse_verdict(text)

    results = judge_clusters(clusters, llm_cached, registry_names)
    save_cache(args.cache, cache)

    from collections import Counter
    rejected = Counter()
    for r in results:
        if r["ship"]:
            continue
        if r["error"]:
            rejected[r["error"]] += 1
        else:
            rejected[f"gate_{r['gate']}"] += 1
    shipped = [r for r in results if r["ship"]]
    func_dist = Counter(r["functionId"] for r in shipped)

    out_doc = {
        "version": 1,
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "model": args.model,
        "clustersIn": len(clusters),
        "shipped": len(shipped),
        "rejected": dict(rejected),
        "functionDistribution": dict(sorted(func_dist.items())),
        "entries": results,
    }
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(out_doc, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")

    print("=== DAG pattern gate/name/classify (step C, LLM) ===")
    print(f"clusters in   : {len(clusters)}")
    print(f"shipped       : {len(shipped)}")
    print(f"rejected      : {dict(rejected)}")
    print(f"endpoint calls: {client.calls} (cached: {len(cache)})")
    print("shipped by function:")
    for fid, cnt in func_dist.most_common():
        print(f"   {fid}: {cnt}")
    print("top shipped by support:")
    for r in sorted(shipped, key=lambda r: -r["support"])[:12]:
        print(f"   {r['clusterId']} support={r['support']:3d} "
              f"{r['functionId'].replace('jipipe:function:', ''):18s} "
              f"{r['name']!r}")
    print(f"output        : {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
