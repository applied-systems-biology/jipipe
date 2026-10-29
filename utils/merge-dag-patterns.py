#!/usr/bin/env python3
"""Merge near-duplicate DAG pattern candidates by embedding (plan step B,
#1324 / #1333).

Consumes step (A)'s ``utils/taxonomy-research/dag-candidates.json`` and
produces ``utils/taxonomy-research/dag-clusters.json``:

* **pre-merge**: candidates sharing a functional signature are already
  exact duplicates - collapsed BEFORE embedding (one embedding per
  signature group; the (A) `support`/`memberProjects` are the union).
* **embed**: each group's canonical text form (title evidence + node
  display names + wiring) is embedded via the OpenAI-compatible
  ``/embeddings`` endpoint on ``asb-llm.hki-jena.de`` (model
  ``vllm/embedding``, 4096-dim - the same API
  ``JIPipeAPIEmbeddingAIModelRunner`` speaks).
* **merge**: agglomerative clustering (cosine distance, average linkage).
  A cluster = one shipping candidate for step (C); representative = the
  highest-support member (ties: the lowest candidateId); the cluster's
  titles are the name evidence; an untitled geometric-cluster member that
  lands in a titled cluster inherits the title (plan §3.1 signal 3b title
  transfer).
* **name evidence (signals 4/5)**: named nodes (a node whose
  ``jipipe:node:name`` differs from BOTH its current registry display name
  AND its dominant corpus label - the 2026-10-19 audit: comparing only
  against the registry name counted a dev-renamed type's stale old default
  as "user naming", 29% of the old field) and comment text attached
  inside the candidate are additional vocabulary for step (C)'s namer.

Deterministic given the embeddings: the same vectors always produce the
same clusters, in the same order. Embeddings are cached to
``utils/taxonomy-research/dag-embeddings.json`` (keyed by the SHA-256 of
the canonical text) so re-runs and threshold sweeps do not re-call the
endpoint.

Usage (from the repository root):

    python3 utils/merge-dag-patterns.py selftest
    python3 utils/merge-dag-patterns.py [--threshold 0.25] [--sweep]
        [--candidates <dag-candidates.json>] [--out <dag-clusters.json>]
        [--cache <dag-embeddings.json>] [--credentials <file>]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import sys
import time
import urllib.request
from datetime import datetime, timezone

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESEARCH = os.path.join(REPO_ROOT, "utils", "taxonomy-research")
DEFAULT_CANDIDATES = os.path.join(RESEARCH, "dag-candidates.json")
DEFAULT_OUT = os.path.join(RESEARCH, "dag-clusters.json")
DEFAULT_CACHE = os.path.join(RESEARCH, "dag-embeddings.json")
DEFAULT_CREDENTIALS = ("/home/rgerst/.config/opencode-data-analysis/"
                       "credentials.json")

EMBED_URL = "https://asb-llm.hki-jena.de/v1/embeddings"
EMBED_MODEL = "vllm/embedding"
EMBED_DIM = 4096
CHUNK = 40  # texts per request (the audit script's chunk size)

# Plan §3.2: "agglomerative clustering (cosine, threshold sweep 0.1–0.4;
# literature and practice put useful cuts around 0.2–0.3)". The sweep over
# THIS corpus (ruling B8) shows that cut is wrong here: this embedding
# compresses distances high (median cosine distance 0.408; even
# "hello"/"Hello" is 0.13 apart), so the "0.2–0.3" band over-merges - at
# 0.20 a single cluster holds 220 members / 40 distinct graphs (chaining),
# and it only grows with threshold (110 -> 276 -> 714 -> 1101). The usable
# cut is lower: 0.15 stops exactly where chaining begins, where each
# multi-member cluster is one coherent workflow family (1–9 distinct graphs,
# many projects). The committed artifact is produced at 0.15.
DEFAULT_THRESHOLD = 0.15
SWEEP = (0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40)


# --------------------------------------------------------------------------- #
# Canonical text form
# --------------------------------------------------------------------------- #

def canonical_text(candidate: dict) -> str:
    """The canonical text form of a candidate (plan §3.2: "title +
    topologically/BFS-ordered node display names + wiring").

    A node's display name falls back to its node-info-id when the
    ``jipipe:node:name`` label is empty. Deterministic; no JSON punctuation
    (plain sentences for the embedding model).
    """
    g = candidate["graph"]
    nodes = g["nodes"]
    edges = g["edges"]
    # BFS from sources (deterministic: alphabetical among ready nodes)
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
            ready = [max(remaining)]  # cycle break (A guarantees DAGs)
        for k in ready:
            order.append(k)
            remaining.discard(k)
            for s in succ[k]:
                indeg[s] -= 1
    name_of = {k: (nodes[k].get("jipipe:node:name")
                   or nodes[k]["jipipe:node-info-id"])
               for k in nodes}
    parts = []
    title = candidate.get("title")
    if title:
        parts.append(f"Title: {title}")
    parts.append("Nodes: " + "; ".join(name_of[k] for k in order))
    wiring = "; ".join(
        f"{name_of[e['source-node']]} {e.get('source-slot', '')} -> "
        f"{name_of[e['target-node']]} {e.get('target-slot', '')}"
        for e in sorted(edges, key=lambda e: (e["source-node"],
                                              e.get("source-slot", ""),
                                              e["target-node"],
                                              e.get("target-slot", ""))))
    if wiring:
        parts.append("Wiring: " + wiring)
    return "\n".join(parts)


# --------------------------------------------------------------------------- #
# Name evidence (signals 4/5)
# --------------------------------------------------------------------------- #

def build_corpus_label_census(candidates) -> dict:
    """node-id -> {label: count} over the harvested candidates (the (A)
    population). The 2026-10-19 audit: "label != registry name" as the
    signal-4 test conflates USER renaming with node TYPES the developers
    renamed - the type's old default differs from the current registry name
    across the whole corpus (29% of the nameEvidence field was exactly that).
    The census is the baseline for "what is this type's default label"
    (dominant_label); it is rebuilt from dag-candidates.json on every (B)
    run, so it needs no separate artifact."""
    census = {}
    for c in candidates:
        for v in c["graph"]["nodes"].values():
            nid = v.get("jipipe:node-info-id")
            label = (v.get("jipipe:node:name") or "").strip()
            if not nid or not label:
                continue
            census.setdefault(nid, {})
            census[nid][label] = census[nid].get(label, 0) + 1
    return census


def dominant_label(census: dict, node_id: str):
    """The type's de-facto default label = the most frequent label in the
    census (lexicographic tie-break, deterministic). None when the type is
    absent from the census. This is the registry name for un-renamed types
    and the STALE old default for types the developers renamed - excluding
    it (below) is what separates user naming from a rename."""
    counts = census.get(node_id)
    if not counts:
        return None
    return sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]


def collect_name_evidence(candidate: dict, registry_names=None,
                          census=None) -> list:
    """Named-node evidence (signal 4): the ``jipipe:node:name`` labels that
    are a genuine USER choice - not the node type's default. A label is
    excluded when it equals the type's CURRENT registry name OR its
    DOMINANT corpus label (census): a node type the developers renamed
    carries its stale old default across the corpus (measured 60–99% of
    occurrences, e.g. "Add path to annotations" for
    path-to-annotation-simple, now "Annotate with file/directory name") and
    that is not naming evidence. Without ``registry_names`` there is no
    evidence at all; the census is an optional refinement (a missing type in
    the census is not excluded by the dominant rule)."""
    if not registry_names:
        return []
    out = []
    for v in candidate["graph"]["nodes"].values():
        label = (v.get("jipipe:node:name") or "").strip()
        if not label:
            continue
        reg = registry_names.get(v["jipipe:node-info-id"])
        if reg and label == reg:
            continue  # the current registry name is not a user name
        if census is not None and label == dominant_label(census,
                                                          v["jipipe:node-info-id"]):
            continue  # the type's (stale or current) default label
        out.append(label)
    return sorted(set(out))


def load_registry_names(path=None):
    """{node id: display name} from the registry.json research artifact."""
    path = path or os.path.join(RESEARCH, "registry.json")
    if not os.path.isfile(path):
        return None
    with open(path, encoding="utf-8") as fh:
        reg = json.load(fh)
    return {n["id"]: n["name"] for n in reg.get("nodes", [])}


# --------------------------------------------------------------------------- #
# Embedding endpoint
# --------------------------------------------------------------------------- #

class EndpointEmbedder:
    """The live OpenAI-compatible /embeddings endpoint (asb-llm). Batched,
    retried, cached by the caller (merge_candidates does the caching)."""

    def __init__(self, credentials_path=DEFAULT_CREDENTIALS,
                 url=EMBED_URL, model=EMBED_MODEL):
        with open(credentials_path, encoding="utf-8") as fh:
            creds = json.load(fh)
        self.key = creds["vlm_api_key"]
        self.url = url
        self.model = model
        self.calls = 0

    def __call__(self, texts):
        out = []
        for i in range(0, len(texts), CHUNK):
            chunk = texts[i:i + CHUNK]
            body = json.dumps({"model": self.model, "input": chunk}).encode()
            req = urllib.request.Request(
                self.url, data=body,
                headers={"Authorization": f"Bearer {self.key}",
                         "Content-Type": "application/json"})
            data = None
            for attempt in range(3):
                try:
                    with urllib.request.urlopen(req, timeout=120) as r:
                        data = json.load(r)
                    break
                except Exception as e:
                    if attempt == 2:
                        raise
                    time.sleep(2 * (attempt + 1))
            self.calls += 1
            out.extend(v["embedding"] for v in
                       sorted(data["data"], key=lambda x: x["index"]))
        return out


# --------------------------------------------------------------------------- #
# Clustering
# --------------------------------------------------------------------------- #

# scipy is the production path (C-speed average linkage; the research
# scripts in this repo already use it). It is OPTIONAL - without it the
# pure-Python fallback below is used (correct, but O(n^4); fine for the
# small hermetic test inputs, too slow for the ~624-vector corpus run).
try:
    import numpy as np  # type: ignore
    from scipy.cluster.hierarchy import linkage, fcluster  # type: ignore
    HAS_SCIPY = True
except ImportError:  # pragma: no cover
    HAS_SCIPY = False


def _cosine_distance(a, b):
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(x * x for x in b))
    if na == 0 or nb == 0:
        return 1.0
    return 1.0 - dot / (na * nb)


def average_linkage_clusters(vectors, threshold):
    """Deterministic agglomerative clustering (average linkage over cosine
    distances); merges while the closest pair's average-linkage distance is
    <= threshold. Returns a list of lists of indices, in deterministic
    (min-index-sorted) order.

    ``threshold`` is a COSINE DISTANCE (0 = identical, 1 = orthogonal,
    2 = opposite). With scipy, ``fcluster(criterion='distance')`` cuts the
    linkage tree at exactly this scale; the pure-Python fallback implements
    the same semantics.
    """
    n = len(vectors)
    if n == 0:
        return []
    if HAS_SCIPY and n > 1:
        z = linkage(np.asarray(vectors, dtype=np.float64),
                    method="average", metric="cosine")
        labels = fcluster(z, t=threshold, criterion="distance")
        by_label = {}
        for idx, lab in enumerate(labels):
            by_label.setdefault(int(lab), []).append(idx)
        clusters = [sorted(members) for members in by_label.values()]
        clusters.sort(key=min)
        return clusters
    return _average_linkage_pure(vectors, threshold)


def _average_linkage_pure(vectors, threshold):
    """Pure-Python average linkage (the fallback path; O(n^4))."""
    n = len(vectors)
    clusters = [[i] for i in range(n)]
    if n <= 1:
        return clusters
    # pairwise distance matrix (small inputs only)
    pd = [[_cosine_distance(vectors[i], vectors[j]) if i != j else 0.0
           for j in range(n)] for i in range(n)]

    def linkage_dist(a, b):
        return (sum(pd[x][y] for x in a for y in b)
                / (len(a) * len(b)))

    while len(clusters) > 1:
        best = None
        for i in range(len(clusters)):
            for j in range(i + 1, len(clusters)):
                key = (linkage_dist(clusters[i], clusters[j]),
                       min(clusters[i]), min(clusters[j]))
                if best is None or key < best:
                    best = key
        if best[0] > threshold:
            break
        _, lo, hi = best
        # lo/hi are min-index markers, unique per cluster: find the clusters
        # that contain them and merge (marker-index identity, immune to the
        # list re-ordering below)
        a = next(c for c in clusters if lo in c)
        b = next(c for c in clusters if hi in c)
        a.extend(b)
        clusters = [c for c in clusters if c is not a and c is not b]
        clusters.append(a)
        clusters.sort(key=min)
    return clusters


# --------------------------------------------------------------------------- #
# Merge
# --------------------------------------------------------------------------- #

def merge_candidates(candidates, embed, threshold=DEFAULT_THRESHOLD,
                     registry_names=None, cache=None, cache_path=None,
                     census=None):
    """Pre-merge by signature, embed one text per signature group, cluster,
    and build the shipping-candidate records.

    ``embed``: callable(texts) -> list of vectors (hermetic tests pass a
    fake; the CLI passes the live endpoint with the disk cache).
    ``cache``: {sha256(canonical_text): vector} - consulted first; new
    embeddings are written back to ``cache_path`` when given.
    Returns the list of cluster records (deterministic order).
    """
    if cache is None:
        cache = {}
    # -- pre-merge by functional signature --------------------------------- #
    groups = {}  # signature -> {candidates: [...], ...}
    order_keys = []
    for c in sorted(candidates, key=lambda c: c["candidateId"]):
        sig = c["signature"]
        if sig not in groups:
            groups[sig] = []
            order_keys.append(sig)
        groups[sig].append(c)

    sig_records = []
    need_texts = []
    need_sigs = []
    texts_by_sig = {}
    for sig in order_keys:
        members = groups[sig]
        # union support across members (distinct projects)
        projects = set()
        for m in members:
            projects.update(m.get("memberProjects") or [m["project"]])
        rep = sorted(members,
                     key=lambda m: (-len(m.get("memberProjects")
                                          or [m["project"]]),
                                    m["candidateId"]))[0]
        text = canonical_text(rep)
        texts_by_sig[sig] = text
        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        rec = {
            "signature": sig,
            "candidates": [m["candidateId"] for m in members],
            "representative": rep["candidateId"],
            "projects": sorted(projects),
            "support": len(projects),
            "titles": sorted({m["title"] for m in members if m["title"]}),
            "titleless": [m["candidateId"] for m in members if not m["title"]],
            "signal": rep["signal"],
            "text": text,
            "digest": digest,
        }
        sig_records.append(rec)
        if digest not in cache:
            need_texts.append(text)
            need_sigs.append(rec)
    # -- embed (batched) ---------------------------------------------------- #
    if need_texts:
        vectors = embed(need_texts)
        if len(vectors) != len(need_texts):
            raise RuntimeError(f"embedder returned {len(vectors)} vectors "
                               f"for {len(need_texts)} texts")
        for rec, vec in zip(need_sigs, vectors):
            cache[rec["digest"]] = vec

    vecs = [cache[r["digest"]] for r in sig_records]

    # -- cluster ------------------------------------------------------------ #
    clusters = average_linkage_clusters(vecs, threshold)
    out = []
    for members in clusters:
        recs = [sig_records[i] for i in members]
        # union across the cluster
        projects = set()
        titles = []
        titleless = []
        cand_ids = []
        rep = None
        for r in sorted(recs, key=lambda r: r["representative"]):
            projects.update(r["projects"])
            titles.extend(t for t in r["titles"] if t not in titles)
            titleless.extend(r["titleless"])
            cand_ids.extend(r["candidates"])
            # representative = highest support; ties -> the lowest
            # representative candidateId (deterministic). recs iterate in
            # ascending representative order, so >= keeps the EARLIER one on
            # ties.
            if rep is None or r["support"] > rep["support"]:
                rep = r
        rep_cand = next((c for c in candidates
                         if c["candidateId"] == rep["representative"]), None)
        # title transfer: an untitled member in a titled cluster (plan §3.1
        # signal 3b - "the merge stage first tries to match them against a
        # titled candidate (same-cluster ⇒ the title transfers)")
        transferred = []
        if titles:
            for r in recs:
                if r["titleless"] and r["titles"]:
                    continue
                if r["titleless"] and titles:
                    transferred.extend(r["titleless"])
        out.append({
            "clusterId": None,  # assigned by the caller (stable numbering)
            "candidateIds": sorted(cand_ids),
            "signatureGroups": [r["signature"] for r in
                                sorted(recs, key=lambda r: r["representative"])],
            "representative": rep["representative"],
            "representativeCandidate": rep_cand,
            "support": len(projects),
            "memberProjects": sorted(projects),
            "titleEvidence": titles,
            "titleTransferredTo": sorted(set(transferred)),
            "nameEvidence": sorted(set(
                ev for r in recs
                for ev in collect_name_evidence_candidates(r, candidates,
                                                           registry_names,
                                                           census))),
            "text": canonical_text(rep_cand) if rep_cand else rep["text"],
        })
    out.sort(key=lambda m: (-m["support"], m["candidateIds"][0]))
    for i, m in enumerate(out):
        m["clusterId"] = f"cluster-{i + 1:04d}"
    return out


def collect_name_evidence_candidates(sig_record, candidates, registry_names,
                                     census=None):
    """Named-node evidence across a signature group's member candidates."""
    if not registry_names:
        return []
    by_id = {c["candidateId"]: c for c in candidates}
    out = []
    for cid in sig_record["candidates"]:
        c = by_id.get(cid)
        if c:
            out.extend(collect_name_evidence(c, registry_names, census))
    return out


# --------------------------------------------------------------------------- #
# Disk cache + CLI
# --------------------------------------------------------------------------- #

def load_cache(path):
    if not os.path.isfile(path):
        return {}
    with open(path, encoding="utf-8") as fh:
        raw = json.load(fh)
    # stored as {digest: [vector]}
    return {k: v for k, v in raw.items()}


def save_cache(path, cache):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(cache, fh)


def selftest():
    tests = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "merge_dag_patterns_tests.py")
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
        description="Merge near-duplicate DAG pattern candidates by "
                    "embedding (plan step B).")
    parser.add_argument("mode", nargs="?", default="run",
                        help="'run' (default) | 'selftest'")
    parser.add_argument("--candidates", default=DEFAULT_CANDIDATES)
    parser.add_argument("--out", default=DEFAULT_OUT)
    parser.add_argument("--cache", default=DEFAULT_CACHE)
    parser.add_argument("--credentials", default=DEFAULT_CREDENTIALS)
    parser.add_argument("--threshold", type=float, default=DEFAULT_THRESHOLD,
                        help="cosine distance merge threshold "
                             "(default 0.25; plan sweep 0.1-0.4)")
    parser.add_argument("--sweep", action="store_true",
                        help="report cluster counts over the sweep instead "
                             "of writing the output")
    args = parser.parse_args(argv)

    if args.mode == "selftest":
        return selftest()

    with open(args.candidates, encoding="utf-8") as fh:
        doc = json.load(fh)
    candidates = doc["candidates"]
    cache = load_cache(args.cache)
    registry_names = load_registry_names()
    census = build_corpus_label_census(candidates)
    embedder = EndpointEmbedder(args.credentials)
    # wrap the endpoint with the disk cache
    def embed_with_cache(texts):
        return embedder(texts)
    if args.sweep:
        # ensure all vectors are cached first (one pass over the endpoint;
        # the per-threshold re-clustering below is then purely local)
        merge_candidates(candidates, embed_with_cache,
                         threshold=1.0, registry_names=registry_names,
                         cache=cache, cache_path=args.cache, census=census)
        save_cache(args.cache, cache)
        def cached_embed(texts):
            # all texts are cached now - fail loudly on a cache miss instead
            # of silently calling the endpoint again
            key = lambda t: hashlib.sha256(t.encode("utf-8")).hexdigest()
            missing = [t for t in texts if key(t) not in cache]
            if missing:
                raise RuntimeError(f"sweep: {len(missing)} uncached text(s) "
                                   "(cache should be complete after the "
                                   "first pass)")
            return [cache[key(t)] for t in texts]
        print("threshold  clusters  multi-clusters  largest")
        for t in SWEEP:
            result = merge_candidates(candidates, cached_embed,
                                      threshold=t, registry_names=registry_names,
                                      cache=cache, census=census)
            multi = [m for m in result if len(m["candidateIds"]) > 1]
            largest = max((len(m["candidateIds"]) for m in result), default=0)
            print(f"{t:9.2f}  {len(result):8d}  {len(multi):14d}  {largest:7d}")
        return 0

    result = merge_candidates(candidates, embed_with_cache,
                              threshold=args.threshold,
                              registry_names=registry_names,
                              cache=cache, cache_path=args.cache, census=census)
    save_cache(args.cache, cache)
    doc_out = {
        "version": 1,
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "threshold": args.threshold,
        "candidatesIn": len(candidates),
        "signatureGroups": None,
        "clusters": result,
    }
    doc_out["signatureGroups"] = len({c["signature"] for c in candidates})
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(doc_out, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write("\n")
    multi = [m for m in result if len(m["candidateIds"]) > 1]
    titled = [m for m in result if m["titleEvidence"]]
    print("=== DAG pattern merge (step B) ===")
    print(f"candidates in : {len(candidates)} "
          f"({doc_out['signatureGroups']} signature groups)")
    print(f"threshold     : {args.threshold:.2f} (cosine, average linkage)")
    print(f"clusters out  : {len(result)} ({len(multi)} multi-member, "
          f"{len(titled)} with title evidence)")
    print(f"endpoint      : {EMBED_URL} ({EMBED_MODEL}, {EMBED_DIM}-dim); "
          f"{embedder.calls} request(s)")
    print(f"cache entries : {len(cache)}")
    print("top by support:")
    for m in result[:10]:
        titles = m["titleEvidence"][:2]
        print(f"   {m['clusterId']} support={m['support']:3d} "
              f"members={len(m['candidateIds']):3d} titles={titles}")
    print(f"output        : {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
