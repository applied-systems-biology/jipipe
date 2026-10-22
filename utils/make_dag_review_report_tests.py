#!/usr/bin/env python3
"""Tests for the DAG review-report generator's pure functions
(utils/make-dag-review-report.py): the (C)-review issue detectors and the
stale-id renamable/exclude split (the (D) shippable-after math).

Run standalone:

    python3 utils/make_dag_review_report_tests.py
"""

import importlib.util
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "make-dag-review-report.py")


def load_report():
    spec = importlib.util.spec_from_file_location("make_dag_review_report", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


R = load_report()


def entry(cid, name, desc, support=2):
    return {"clusterId": cid, "ship": True, "gate": "general_pattern",
            "functionId": "jipipe:function:images", "name": name,
            "description": desc, "support": support,
            "state": {"pattern": {"nodeSequence": "a", "wiring": "",
                                  "nodeCount": 3, "edgeCount": 2,
                                  "support": support, "signal": "s",
                                  "observedTitles": []}}}


class DetectorTests(unittest.TestCase):

    def test_bare_roi(self):
        es = [entry("c1", "Filter ROIs by overlap", "d"),
              entry("c2", "Filter 2D ROIs by overlap", "d"),
              entry("c3", "3D ROI measurements", "d")]
        self.assertEqual(R.bare_roi_count(es), 1)

    def test_jargon(self):
        es = [entry("c1", "List files with annotated paths", "d"),
              entry("c2", "Segmentation to mask hyperstack", "d"),
              entry("c3", "Segment cells into ROIs", "d")]
        self.assertEqual(R.jargon_count(es), 2)

    def test_thin_description(self):
        es = [entry("c1", "N", "Takes images and produces masks."),
              entry("c2", "N", "Annotates data with values.")]
        self.assertEqual(R.thin_desc_count(es), 1)

    def test_stale_ids_split(self):
        cluster_by_id = {
            "c1": {"representativeCandidate": {"graph": {"nodes": {
                "n0": {"jipipe:node-info-id": "ij1-transform-tile"}}}}},
            "c2": {"representativeCandidate": {"graph": {"nodes": {
                "n0": {"jipipe:node-info-id": "clij:plugin:x.Y"}}}}},
            "c3": {"representativeCandidate": {"graph": {"nodes": {
                "n0": {"jipipe:node-info-id": "ij1-threshold-auto2d"}}}}},
        }
        shipped = [entry("c1", "a", "d"), entry("c2", "b", "d"),
                   entry("c3", "c", "d")]
        reg_ids = {"ij1-threshold-auto2d"}
        stale = R.stale_ids(shipped, cluster_by_id, reg_ids)
        self.assertEqual(set(stale), {"c1", "c2"})
        renamable = {cid: bad for cid, bad in stale.items()
                     if set(bad) <= R.STALE_RENAMES}
        exclude = {cid: bad for cid, bad in stale.items()
                   if not (set(bad) <= R.STALE_RENAMES)}
        self.assertEqual(set(renamable), {"c1"})
        self.assertEqual(set(exclude), {"c2"})
        # shippable-after = shipped - exclude (renamable ones ship remapped)
        self.assertEqual(len(shipped) - len(exclude), 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
