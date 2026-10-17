#!/usr/bin/env python3
"""Tests for utils/classify-node-description-audit.py.

The (C) review's decision (3) is a full audit of the node descriptions the
shipped patterns use. My first scoping pass used a length proxy (missing or
<50 chars) and produced "93 targets" — but that proxy MISSES the most damaging
defect: a description that is long yet describes the WRONG node (a copy-paste
from a sibling), e.g. ij1-roi-to-rgb (the most-used pattern node) whose text
says "Converts ROI lists to masks" (that is ij1-roi-to-mask's description).

This classifier buckets every pattern-relevant node type by its ACTUAL defect
so the audit scope is correct and reproducible. Run standalone:

    python3 utils/classify-node-description-audit_tests.py
"""

import importlib.util
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "classify-node-description-audit.py")


def load_cls():
    spec = importlib.util.spec_from_file_location("classify_node_desc_audit", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


C = load_cls()


class BucketTests(unittest.TestCase):
    """classify() on synthetic nodes — one defect each."""

    def _cls(self, desc, nid="some-node", freq=5):
        return C.classify(nid, desc, freq=freq)

    def test_missing(self):
        self.assertEqual(self._cls(None), "missing")
        self.assertEqual(self._cls(""), "missing")

    def test_thin(self):
        self.assertEqual(self._cls("Renames columns"), "thin")
        self.assertEqual(self._cls("x" * 49), "thin")
        self.assertNotEqual(self._cls("x" * 50), "thin")

    def test_ok(self):
        self.assertEqual(self._cls("A " + "real description " * 5), "ok")

    def test_clij_auto_registered_is_not_editable(self):
        self.assertEqual(
            self._cls("long enough description text here to pass",
                      "clij:plugin:net.x.y.Mask"), "not_editable_here")

    def test_absent_plugin_is_not_editable(self):
        for nid in ("jipipe2ai-predict-image", "deep-learning-train-image-model",
                    "create-jipipe2ai-segmentation-model"):
            self.assertEqual(
                self._cls("long enough description text here to pass", nid),
                "not_editable_here")

    def test_stale_id_is_not_editable(self):
        # the corpus id was renamed in 6.0.0; the pattern is remapped to its
        # successor (which has its own description), so there is nothing to fix
        self.assertEqual(
            self._cls("long enough description text here to pass",
                      "ij1-transform-tile"), "not_editable_here")

    def test_curated_wrong_overrides_length(self):
        # long, but describes the sibling node — must be flagged wrong, not ok
        self.assertEqual(
            C.classify("ij1-roi-to-rgb",
                       "Converts ROI lists to masks. This algorithm needs a "
                       "reference image that provides the output sizes."),
            "wrong")

    def test_wrong_set_is_the_high_value_set(self):
        # the verified copy-paste errors are all in the curated wrong set
        for nid in ("ij1-roi-to-rgb", "filaments-convert-to-mask",
                    "trackmate-filter-tracks"):
            self.assertIn(nid, C.WRONG_COPY_PASTE)


class FrequencyOrderingTests(unittest.TestCase):
    """The worklist must be ordered so the highest-impact nodes are fixed first."""

    def test_higher_frequency_ranks_first_within_a_bucket(self):
        rows = [
            {"nodeId": "a", "desc": "short", "freq": 1},
            {"nodeId": "b", "desc": "short", "freq": 44},
        ]
        ranked = C.rank_worklist(rows)
        thin = [r for r in ranked if r["bucket"] == "thin"]
        self.assertEqual(thin[0]["nodeId"], "b",
                         "the most-used thin node must come first")


if __name__ == "__main__":
    unittest.main(verbosity=2)
