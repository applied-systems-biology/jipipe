#!/usr/bin/env python3
"""Tests for the DAG pattern merge stage (plan step B, utils/merge-dag-patterns.py).

Step (B) embeds each candidate's canonical text form via the Qwen embedding
endpoint (OpenAI-compatible /embeddings on asb-llm.hki-jena.de), merges
near-duplicate candidates by agglomerative clustering (cosine), and collects
the name evidence (human-authored titles + named nodes + comment text) that
step (C)'s JEV namer will pick from.

Deterministic given the embeddings: the same vectors always produce the same
clusters. The endpoint itself is NOT called by these tests — they run against
fixture embeddings (a fake vector function) so the suite is hermetic. One
optional live probe (skipped unless --live is passed / env DAG_MERGE_LIVE=1)
validates the endpoint contract.

Run standalone:

    python3 utils/merge_dag_patterns_tests.py [--live]
"""

import importlib.util
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "merge-dag-patterns.py")


def load_merger():
    spec = importlib.util.spec_from_file_location("merge_dag_patterns", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


MERGE = load_merger()


def cand(cid, signal="canvas-note-section", title=None, support=1,
         node_ids=None, edges=None, project=None, names=None,
         memberProjects=None):
    node_ids = node_ids or ["folder-list-files",
                            "import-imagej-imgplus-from-file",
                            "ij1-roi-to-mask"]
    names = names or ["List files", "Import image", "Convert ROI to mask"]
    nodes = {}
    for i, (nid, nm) in enumerate(zip(node_ids, names)):
        nodes[f"n{i}"] = {
            "jipipe:node-info-id": nid,
            "jipipe:node:name": nm,
            "jipipe:graph-compartment": "u",
            "jipipe:metadata-v1": {"location/u/x": i, "location/u/y": 0},
        }
    edges = edges or [
        {"source-node": "n0", "source-slot": "Files",
         "target-node": "n1", "target-slot": "Files"},
        {"source-node": "n1", "source-slot": "Output",
         "target-node": "n2", "target-slot": "Image"},
    ]
    project = project or f"proj-{cid}.jip"
    return {
        "candidateId": cid,
        "signal": signal,
        "project": project,
        "title": title,
        "support": support,
        "memberProjects": memberProjects or [project],
        "nodeCount": len(node_ids),
        "edgeCount": len(edges),
        "layoutSource": "source",
        "compartmentUuid": "u",
        "graph": {"nodes": nodes, "edges": edges},
        "signature": f"sig-{cid}",
    }


def make_candidates_file(path, candidates, extra=None):
    doc = {"version": 1, "generated": "2026-10-17T00:00:00Z",
           "corpusRoot": "/x", "projectsScanned": 1,
           "candidates": candidates}
    if extra:
        doc.update(extra)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(doc, fh)
    return path


class MergeTests(unittest.TestCase):

    # -- canonical text form ------------------------------------------------ #

    def test_canonical_text_contains_title_nodes_and_wiring(self):
        c = cand("dag-001", title="Load images and ROIs",
                 names=["List TIFF", "Import image", "Convert ROI to mask"])
        text = MERGE.canonical_text(c)
        self.assertIn("Load images and ROIs", text)
        self.assertIn("List TIFF", text)
        self.assertIn("Import image", text)
        self.assertIn("Convert ROI to mask", text)
        # wiring with slots
        self.assertIn("Files", text)
        self.assertIn("Image", text)
        # no HTML/JSON punctuation noise
        self.assertNotIn("{", text)

    def test_canonical_text_is_deterministic(self):
        c = cand("dag-001", title="T")
        self.assertEqual(MERGE.canonical_text(c), MERGE.canonical_text(c))

    def test_node_display_names_use_registry_names_fallback(self):
        # a node with an empty jipipe:node:name falls back to the info id
        c = cand("dag-001", names=["", "Import image", "Convert ROI to mask"])
        text = MERGE.canonical_text(c)
        self.assertIn("folder-list-files", text)

    # -- clustering ----------------------------------------------------------#

    def test_agglomerative_merge_near_duplicates(self):
        # two same-shape candidates with near-identical text -> one cluster
        c1 = cand("dag-001", title="Load images and ROIs",
                  names=["List TIFF", "Import image", "Convert ROI to mask"])
        c2 = cand("dag-002", title="Loading raw images and ROIs",
                  names=["List TIFF", "Import image", "Convert ROI to mask"])
        c3 = cand("dag-003", title="Train a cellpose model",
                  names=["Split data", "Import model", "Train cellpose"],
                  node_ids=["distribute-data",
                            "import-cellpose-4.x-pretrained-segmentation-model",
                            "cellpose-segmentation-training-4.x"])
        # fake embeddings: c1/c2 nearly identical, c3 orthogonal (keyed on
        # the title, which the canonical text always carries in tests)
        def embed(texts):
            v = {"Load images": [1.0, 0.0, 0.0],
                 "Loading raw": [0.99, 0.05, 0.0],
                 "Train a": [0.0, 0.0, 1.0]}
            out = []
            for t in texts:
                key = next(k for k in v if k in t)
                out.append(v[key])
            return out
        result = MERGE.merge_candidates([c1, c2, c3], embed, threshold=0.2)
        self.assertEqual(len(result), 2, "c1+c2 merge, c3 stays separate")
        # the cluster that contains c1 also contains c2
        cluster_ids = [set(m["candidateIds"]) for m in result]
        self.assertTrue(any({"dag-001", "dag-002"} <= ids for ids in cluster_ids))
        # member data preserved
        merged = next(m for m in result if "dag-001" in m["candidateIds"])
        self.assertEqual(merged["support"], 2)  # 2 projects total
        self.assertEqual(set(merged["titleEvidence"]), {"Load images and ROIs",
                                                        "Loading raw images and ROIs"})
        # representative = highest support member
        self.assertEqual(merged["representative"], "dag-001")

    def test_representative_is_highest_support(self):
        # support = DISTINCT PROJECTS; dag-002 spans two, dag-001 one
        c1 = cand("dag-001", title="A", project="p1.jip",
                  memberProjects=["p1.jip"])
        c2 = cand("dag-002", title="A variant", project="p2.jip",
                  memberProjects=["p1.jip", "p2.jip"])
        def embed(texts):
            return [[1.0, 0.0]] * len(texts)
        result = MERGE.merge_candidates([c1, c2], embed, threshold=0.5)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["representative"], "dag-002")
        # the cluster's support = distinct projects across members
        self.assertEqual(result[0]["support"], 2)

    def test_support_is_distinct_projects(self):
        # two candidates from the SAME project with the same signature must
        # not double-count the project
        c1 = cand("dag-001", title="A", project="same.jip", support=1,
                  memberProjects=["same.jip"])
        c2 = cand("dag-002", title="A2", project="same.jip", support=1,
                  memberProjects=["same.jip"])
        def embed(texts):
            return [[1.0, 0.0]] * len(texts)
        result = MERGE.merge_candidates([c1, c2], embed, threshold=0.5)
        self.assertEqual(result[0]["support"], 1)
        self.assertEqual(result[0]["memberProjects"], ["same.jip"])

    def test_threshold_zero_keeps_all_separate(self):
        c1 = cand("dag-001", title="A")
        c2 = cand("dag-002", title="A2")
        def embed(texts):
            return [[1.0, 0.0]] * len(texts)
        # threshold is a cosine DISTANCE: 0.0 merges only zero-distance
        # pairs; two identical vectors are zero-distance, so this pair DOES
        # merge. Use orthogonal vectors + threshold 0 to keep all separate.
        def embed_orthogonal(texts):
            return [[1.0, 0.0], [0.0, 1.0]]
        result = MERGE.merge_candidates([c1, c2], embed_orthogonal,
                                        threshold=0.0)
        self.assertEqual(len(result), 2)

    def test_signature_premerge_collapses_identical(self):
        # identical functional signature -> merged BEFORE embedding (one
        # embedding call, one cluster)
        c1 = cand("dag-001", title="A", support=1)
        c2 = cand("dag-002", title="A", support=1)
        c2["signature"] = c1["signature"]
        calls = []
        def embed(texts):
            calls.extend(texts)
            return [[1.0, 0.0]] * len(texts)
        result = MERGE.merge_candidates([c1, c2], embed, threshold=0.0)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["support"], 2)
        self.assertEqual(len(calls), 1, "one embedding per signature group")

    def test_untitled_cluster_gets_title_transfer(self):
        # an untitled candidate (geometric cluster) whose embedding matches a
        # titled cluster inherits the title evidence (plan §3.1 signal 3b)
        c1 = cand("dag-001", title="Load images and ROIs")
        c2 = cand("dag-002", signal="geometric-cluster", title=None,
                  names=["List TIFF", "Import image", "Convert ROI to mask"])
        def embed(texts):
            return [[1.0, 0.0]] * len(texts)
        result = MERGE.merge_candidates([c1, c2], embed, threshold=0.5)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["titleEvidence"], ["Load images and ROIs"])
        self.assertEqual(result[0]["titleTransferredTo"], ["dag-002"])

    def test_deterministic_ordering(self):
        cs = [cand(f"dag-{i:03d}", title=f"T{i}") for i in range(1, 6)]
        def embed(texts):
            out = []
            for t in texts:
                # the title is the last line of "Title: T<i>" in these fakes
                import re
                m = re.search(r"Title: T(\d+)", t)
                i = int(m.group(1))
                out.append([1.0 if i % 2 == 1 else 0.0,
                            0.0 if i % 2 == 1 else 1.0])
            return out
        r1 = MERGE.merge_candidates(cs, embed, threshold=0.3)
        r2 = MERGE.merge_candidates(list(reversed(cs)), embed, threshold=0.3)
        self.assertEqual(json.dumps(r1, sort_keys=True),
                         json.dumps(r2, sort_keys=True),
                         "merge must be input-order independent")

    # -- name evidence (signals 4/5) ---------------------------------------- #

    def test_named_node_evidence_collected(self):
        # named nodes inside a cluster are extra name evidence: a node whose
        # jipipe:node:name differs from its registry display name
        c = cand("dag-001", title="A",
                 node_ids=["folder-list-files", "import-imagej-imgplus-from-file",
                           "ij1-roi-to-mask"],
                 names=["My custom loading step", "Import image", "To mask"])
        names = {"folder-list-files": "List files in folder",
                 "import-imagej-imgplus-from-file": "Import image",
                 "ij1-roi-to-mask": "Convert ROI to mask"}
        ev = MERGE.collect_name_evidence(c, registry_names=names)
        self.assertIn("My custom loading step", ev)
        self.assertIn("To mask", ev)
        self.assertNotIn("Import image", ev)  # equals the registry name
        self.assertNotIn("List files in folder", ev)  # registry name, not user name

    def test_named_node_evidence_excludes_the_corpus_dominant_label(self):
        # the 2026-10-19 audit: "label != registry name" conflates USER
        # renaming with a node TYPE that the developers renamed — the type's
        # old default differs from the current registry name across the whole
        # corpus and is not a naming signal. A label that equals the type's
        # DOMINANT corpus label (its de-facto default) is excluded, in
        # addition to the current registry name.
        c = cand("dag-001", title="A",
                 node_ids=["renamed-type", "user-named"],
                 names=["Old default name", "My special step"])
        names = {"renamed-type": "New current name",
                 "user-named": "Convert ROI to mask"}
        census = {"renamed-type": {"Old default name": 990,
                                   "New current name": 3},
                  "user-named": {"Convert ROI to mask": 40}}
        ev = MERGE.collect_name_evidence(c, registry_names=names,
                                         census=census)
        self.assertNotIn("Old default name", ev,
                         "the stale default is not a naming signal")
        self.assertIn("My special step", ev,
                      "genuine user naming survives")

    def test_dominant_label_deterministic_tie_break(self):
        census = {"x": {"B label": 5, "A label": 5}}
        self.assertEqual(MERGE.dominant_label(census, "x"), "A label")
        self.assertIsNone(MERGE.dominant_label(census, "absent"))

    def test_named_node_evidence_requires_registry_names(self):
        c = cand("dag-001", title="A", names=["My step", "Import image", "To mask"])
        ev = MERGE.collect_name_evidence(c, registry_names=None)
        self.assertEqual(ev, [], "no registry names -> no named-node evidence")


if __name__ == "__main__":
    live = "--live" in sys.argv
    if live:
        os.environ["DAG_MERGE_LIVE"] = "1"
        sys.argv.remove("--live")
    unittest.main(verbosity=2)
