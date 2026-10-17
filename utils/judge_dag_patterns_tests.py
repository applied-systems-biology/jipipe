#!/usr/bin/env python3
"""Tests for the DAG pattern gate/name/classify stage (plan step C,
utils/judge-dag-patterns.py).

Step (C) asks the LLM (``vllm/frontier`` via the asb-llm OpenAI-compatible
``/chat/completions`` endpoint — a reasoning model: the answer is in
``content`` after the ``reasoning_content``; fallback extraction follows the
data-analysis plugin's vlm_client) ONE structured-JSON question per cluster:

    {"gate": "general_pattern" | "workflow_plumbing" | "noise",
     "function": one of the 13 top-level branch keys,
     "name": "short general name" | "",
     "description": "1-2 general sentences" | ""}

(Replaces the imajev MCQ readout: its 128-char option cap and min-2-options
constraint fight the naming task, and it cannot emit a description. The LLM
produces gate + function + name + description in one call. The imajev-based
implementation was removed; its endpoint contract remains recorded in the
ledger.)

The endpoint is NOT called by these tests — they inject a deterministic
fake LLM function, so the suite is hermetic.

Run standalone:

    python3 utils/judge_dag_patterns_tests.py
"""

import importlib.util
import json
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "judge-dag-patterns.py")


def load_judge():
    spec = importlib.util.spec_from_file_location("judge_dag_patterns", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


JUDGE = load_judge()

# The 13 top-level branch keys (jipipe:function:<x>) — the function option set.
BRANCH_KEYS = ("annotations", "data", "export", "filaments", "graph",
               "images", "machine-learning", "misc", "plotting", "rois",
               "statistics", "tables", "tracking")
BRANCH_IDS = {f"jipipe:function:{k}" for k in BRANCH_KEYS}


def cluster(cid="cluster-0001", titles=None, node_names=None, support=2,
            rep_cid="dag-001"):
    node_names = node_names or ["Import image", "Threshold", "Convert ROI to mask"]
    rep = {
        "candidateId": rep_cid,
        "signal": "canvas-note-section",
        "project": f"p{support}.jip",
        "title": (titles[0] if titles else None),
        "support": support,
        "memberProjects": [f"p{i}.jip" for i in range(support)],
        "nodeCount": len(node_names),
        "edgeCount": len(node_names) - 1,
        "layoutSource": "source",
        "compartmentUuid": "u",
        "graph": {
            "nodes": {f"n{i}": {"jipipe:node-info-id": f"nid{i}",
                                "jipipe:node:name": nm,
                                "jipipe:graph-compartment": "u",
                                "jipipe:metadata-v1": {}}
                      for i, nm in enumerate(node_names)},
            "edges": [{"source-node": f"n{i}", "source-slot": "s",
                       "target-node": f"n{i+1}", "target-slot": "t"}
                      for i in range(len(node_names) - 1)],
        },
    }
    return {
        "clusterId": cid,
        "candidateIds": [rep_cid],
        "signatureGroups": ["sig"],
        "representative": rep_cid,
        "representativeCandidate": rep,
        "support": support,
        "memberProjects": rep["memberProjects"],
        "titleEvidence": titles or [],
        "titleTransferredTo": [],
        "nameEvidence": [],
        "text": "Title: x\nNodes: ...\nWiring: ...",
    }


def make_llm(gate="general_pattern", function="images", name="Load images and ROIs",
             description="Loads images and ROIs from files."):
    """A deterministic fake LLM: returns the fixed JSON verdict."""
    verdict = {"gate": gate, "function": function, "name": name,
               "description": description}
    def llm(prompt):
        return dict(verdict)
    return llm


class JudgeTests(unittest.TestCase):

    # -- state construction ------------------------------------------------ #

    def test_state_carries_titles_nodes_wiring_and_support(self):
        c = cluster(titles=["Load images"], support=5)
        state = JUDGE.build_state(c, None)
        flat = json.dumps(state)
        self.assertIn("Load images", flat)
        self.assertIn("5", flat)  # support
        self.assertIn("Import image", flat)

    def test_state_carries_node_descriptions(self):
        # the (C) review: the LLM must see each node's real description, not
        # just its short display name
        c = cluster(node_names=["Import image", "Convert ROI to mask"])
        descs = {"nid0": "Loads an image from a file.",
                 "nid1": "Converts ROI lists to masks."}
        state = JUDGE.build_state(c, None, node_descs=descs)
        self.assertIn("nodes", state["pattern"])
        by_id = {n["id"]: n for n in state["pattern"]["nodes"]}
        self.assertEqual(by_id["nid0"]["description"],
                         "Loads an image from a file.")
        self.assertEqual(by_id["nid1"]["description"],
                         "Converts ROI lists to masks.")

    def test_state_marks_roi_dimension(self):
        # the (C) review: 2D and 3D ROIs are different data types and must not
        # be conflated — the state must say which dimension each ROI node is
        # (detected from the node id: ij1-* = 2D, ij3d-* = 3D).
        c = cluster(node_names=["Convert 2D ROI to RGB", "Measure 3D ROI"])
        # node ids are n0..nK in the fixture -> nid0, nid1; override the graph
        c["representativeCandidate"]["graph"]["nodes"]["n0"]["jipipe:node-info-id"] = "ij1-roi-to-rgb"
        c["representativeCandidate"]["graph"]["nodes"]["n1"]["jipipe:node-info-id"] = "ij3d-measure-roi3d"
        state = JUDGE.build_state(c, None)
        by_id = {n["id"]: n for n in state["pattern"]["nodes"]}
        self.assertEqual(by_id["ij1-roi-to-rgb"]["dimension"], "2D")
        self.assertEqual(by_id["ij3d-measure-roi3d"]["dimension"], "3D")

    def test_prompt_names_the_13_branches(self):
        c = cluster(titles=["Load images"])
        prompt = JUDGE.build_prompt(c, JUDGE.build_state(c, None))
        for k in BRANCH_KEYS:
            self.assertIn(k, prompt)
        for g in ("general_pattern", "workflow_plumbing", "noise"):
            self.assertIn(g, prompt)

    def test_prompt_has_the_four_naming_rules(self):
        # the (C) review: the prompt must carry the four rules — name the
        # outcome (no jargon), qualify ROI 2D/3D, state concrete in->out, and
        # write for two audiences.
        c = cluster(titles=["Load images"])
        prompt = JUDGE.build_prompt(c, JUDGE.build_state(c, None))
        low = prompt.lower()
        self.assertIn("2d roi", low) or self.assertIn("2d/3d", low)
        self.assertTrue(any(t in low for t in
                            ("annotated path", "hyperstack", "jargon",
                             "internal term")), "no jargon-ban rule")
        self.assertIn("biologist", low) or self.assertIn("audience", low)

    # -- decision logic ---------------------------------------------------- #

    def test_ship_when_general_and_named(self):
        llm = make_llm()
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertTrue(j["ship"], j)
        self.assertEqual(j["functionId"], "jipipe:function:images")
        self.assertEqual(j["name"], "Load images and ROIs")
        self.assertEqual(j["description"], "Loads images and ROIs from files.")
        self.assertEqual(j["gate"], "general_pattern")

    def test_reject_when_plumbing(self):
        llm = make_llm(gate="workflow_plumbing")
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertFalse(j["ship"])
        self.assertEqual(j["gate"], "workflow_plumbing")

    def test_reject_when_noise(self):
        llm = make_llm(gate="noise")
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertFalse(j["ship"])

    def test_reject_when_name_empty(self):
        llm = make_llm(name="", description="")
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertFalse(j["ship"], "an empty name must reject")
        self.assertEqual(j["gate"], "general_pattern")

    def test_reject_when_function_invalid(self):
        llm = make_llm(function="not-a-branch")
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertFalse(j["ship"], "an invalid function key must reject")
        self.assertIsNone(j["functionId"])

    def test_gate_must_be_known(self):
        llm = make_llm(gate="maybe")
        j = JUDGE.judge_cluster(cluster(titles=["T"]), llm, None)
        self.assertFalse(j["ship"])
        self.assertEqual(j["gate"], "maybe")

    def test_verdict_extracted_from_json_content(self):
        # the LLM returns JSON text (possibly with surrounding whitespace or
        # a code fence); the judge must extract the JSON object
        verdict = ("```json\n" + json.dumps(
            {"gate": "general_pattern", "function": "rois",
             "name": "Manage ROIs", "description": "Filters ROIs."}) + "\n```")
        def llm(prompt):
            return verdict
        j = JUDGE.judge_cluster(cluster(titles=["ROI management"]), llm, None)
        self.assertTrue(j["ship"])
        self.assertEqual(j["functionId"], "jipipe:function:rois")
        self.assertEqual(j["name"], "Manage ROIs")

    def test_malformed_llm_output_rejects(self):
        def llm(prompt):
            return "I think this is a nice pattern but I refuse JSON."
        j = JUDGE.judge_cluster(cluster(titles=["T"]), llm, None)
        self.assertFalse(j["ship"])
        self.assertEqual(j["error"], "malformed_verdict")

    def test_verdict_recorded_as_audit_trail(self):
        llm = make_llm()
        j = JUDGE.judge_cluster(cluster(titles=["Load images and ROIs"]),
                                llm, None)
        self.assertEqual(j["verdict"]["gate"], "general_pattern")
        self.assertEqual(j["verdict"]["function"], "images")

    def test_untitled_cluster_ships_with_llm_name(self):
        llm = make_llm(name="Threshold images and find particles",
                       description="Thresholds and detects objects.")
        j = JUDGE.judge_cluster(cluster(titles=[]), llm, None)
        self.assertTrue(j["ship"])
        self.assertEqual(j["name"], "Threshold images and find particles")

    def test_judge_all_is_input_order_independent(self):
        cs = [cluster(f"cluster-{i:04d}", titles=[f"T{i}"],
                      node_names=[f"n{i}a", f"n{i}b", f"n{i}c"]) for i in range(4)]
        llm = make_llm()
        r1 = JUDGE.judge_clusters(cs, llm, None)
        r2 = JUDGE.judge_clusters(list(reversed(cs)), llm, None)
        self.assertEqual(json.dumps(r1, sort_keys=True),
                         json.dumps(r2, sort_keys=True))

    def test_name_length_capped_for_sidecar(self):
        # the sidecar name is a UI string; a runaway LLM name is rejected
        llm = make_llm(name="x" * 300)
        j = JUDGE.judge_cluster(cluster(titles=["T"]), llm, None)
        self.assertFalse(j["ship"], "a runaway name must reject")

    def test_description_length_capped(self):
        llm = make_llm(description="y" * 2000)
        j = JUDGE.judge_cluster(cluster(titles=["T"]), llm, None)
        self.assertFalse(j["ship"], "a runaway description must reject")


if __name__ == "__main__":
    unittest.main(verbosity=2)
