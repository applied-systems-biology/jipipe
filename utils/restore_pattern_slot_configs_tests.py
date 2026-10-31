#!/usr/bin/env python3
"""Unit tests for restore_pattern_slot_configs.py (the B1 corpus-restore pass).

Synthetic mini-corpus in a temp dir (2 projects, 3 node types: one
parameter-driven, one dynamic, one unmatched-static) + synthetic shipped
files; asserts the deterministic pick, the param-copy exclusions, the
dynamic generic fallback, the hard-fail on an unmatched STATIC type, the
analyze() extra counts, and idempotence (re-run = no-op).

Run: python3 utils/restore_pattern_slot_configs_tests.py
"""
import json
import os
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import restore_pattern_slot_configs as R  # noqa: E402


def _write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, ensure_ascii=False)


def make_registry():
    return {
        # parameter-driven: static Output; extra "Ch A"/"Ch B" are driven.
        "splitter": {
            "inputSlots": [{"name": "Input"}],
            "outputSlots": [{"name": "Output"}]},
        "src": {"inputSlots": [], "outputSlots": [{"name": "Out"}]},
        "sink": {"inputSlots": [{"name": "Input"}], "outputSlots": []},
        # dynamic (JIPipeIOSlotConfiguration): no static slots.
        "annotation-merge": {"inputSlots": [], "outputSlots": []},
        # unmatched STATIC: a real static "In" but the shipped edge wants a
        # slot no corpus instance can cover.
        "static-missing": {
            "inputSlots": [{"name": "In"}], "outputSlots": []},
    }


def write_corpus_splitter(root):
    """Two projects, each with a covering splitter instance; projA is the
    lex-smaller and MUST win the tie-break."""
    _write(os.path.join(root, "projA", "a.jip"), {
        "graph": {"nodes": {
            "u-a": {
                "jipipe:node-info-id": "splitter",
                "jipipe:graph-compartment": "compA",
                "jipipe:node:name": "MUST NOT BE COPIED",
                "channel-to-slot-assignments/Ch A": 0,
                "channel-to-slot-assignments/Ch B": 1,
                "jipipe:slot-configuration": {
                    "input": {"Input": R._slot_entry("Input", "imagej-imgplus", "Input")},
                    "output": {
                        "Output": R._slot_entry("Output", "imagej-imgplus", "Output"),
                        "Ch A": R._slot_entry("Ch A", "imagej-imgplus", "Output"),
                        "Ch B": R._slot_entry("Ch B", "imagej-imgplus", "Output"),
                    },
                },
            }}, "edges": []}})
    _write(os.path.join(root, "projB", "b.jip"), {
        "graph": {"nodes": {
            "u-b": {
                "jipipe:node-info-id": "splitter",
                "jipipe:graph-compartment": "compB",
                "channel-to-slot-assignments/Ch A": 99,
                "jipipe:slot-configuration": {
                    "input": {"Input": R._slot_entry("Input", "imagej-imgplus", "Input")},
                    "output": {
                        "Output": R._slot_entry("Output", "imagej-imgplus", "Output"),
                        "Ch A": R._slot_entry("Ch A", "imagej-imgplus", "Output"),
                        "Ch B": R._slot_entry("Ch B", "imagej-imgplus", "Output"),
                        "Ch C": R._slot_entry("Ch C", "imagej-imgplus", "Output"),
                    },
                },
            }}, "edges": []}})


def split_file():
    return {
        "nodes": {
            "s0": {"jipipe:node-info-id": "src",
                   "jipipe:graph-compartment": "uuid-s0"},
            "s1": {"jipipe:node-info-id": "splitter",
                   "jipipe:graph-compartment": "uuid-s1"},
            "s2": {"jipipe:node-info-id": "sink",
                   "jipipe:graph-compartment": "uuid-s2"},
        },
        "edges": [
            {"source-node": "s0", "source-slot": "Out", "target-node": "s1", "target-slot": "Input"},
            {"source-node": "s1", "source-slot": "Output", "target-node": "s2", "target-slot": "Input"},
            {"source-node": "s1", "source-slot": "Ch A", "target-node": "s2", "target-slot": "Input"},
            {"source-node": "s1", "source-slot": "Ch B", "target-node": "s2", "target-slot": "Input"},
        ],
    }


def test_deterministic_pick_and_param_copy():
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        write_corpus_splitter(root)
        _write(os.path.join(out, "split-file.json"), split_file())
        reg = make_registry()

        report = R.restore(out, root, reg)
        assert len(report) == 1, report
        f, k, nid, source, ei, eo, slots = report[0]
        assert k == "s1" and nid == "splitter", (f, k, nid)
        assert source.startswith("projA/a.jip:"), source  # lex-smallest wins

        node = json.load(open(os.path.join(out, "split-file.json")))["nodes"]["s1"]
        cfg = node["jipipe:slot-configuration"]
        assert set(cfg["output"]) == {"Output", "Ch A", "Ch B"}, cfg["output"]
        # projA's driving params copied verbatim (NOT projB's 99).
        assert node["channel-to-slot-assignments/Ch A"] == 0
        assert node["channel-to-slot-assignments/Ch B"] == 1
        # jipipe: keys (except the slot config itself) are NOT copied.
        assert "jipipe:node:name" not in node
        assert node["jipipe:graph-compartment"] == "uuid-s1"  # its own, not compA

        # Idempotence: re-run = no-op, byte-identical.
        before = open(os.path.join(out, "split-file.json"), "rb").read()
        report2 = R.restore(out, root, reg)
        assert report2 == [], report2
        after = open(os.path.join(out, "split-file.json"), "rb").read()
        assert before == after
    finally:
        shutil.rmtree(tmp)
    print("PASS test_deterministic_pick_and_param_copy")


def test_analyze_extras():
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        write_corpus_splitter(root)
        _write(os.path.join(out, "split-file.json"), split_file())
        reg = make_registry()
        res = R.analyze(out, root, reg)
        assert set(res) == {"split-file.json"}, res
        n = res["split-file.json"]["s1"]
        assert n["input"] == 0 and n["output"] == 2, n
        # src and sink are unaffected (their required set == their static set).
        assert "s0" not in res["split-file.json"]
        assert "s2" not in res["split-file.json"]
    finally:
        shutil.rmtree(tmp)
    print("PASS test_analyze_extras")


def test_dynamic_generic_fallback():
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        # A corpus instance that does NOT cover "Data 2" -> no match -> fallback.
        _write(os.path.join(root, "p", "q.jip"), {
            "graph": {"nodes": {
                "u": {"jipipe:node-info-id": "annotation-merge",
                      "jipipe:graph-compartment": "c",
                      "jipipe:slot-configuration": {
                          "input": {"X": R._slot_entry("X", "jipipe:data", "Input")},
                          "output": {"X": R._slot_entry("X", "jipipe:data", "Output")},
                      }}}}, "edges": []})
        g = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "src",
                       "jipipe:graph-compartment": "u0"},
                "n1": {"jipipe:node-info-id": "annotation-merge",
                       "jipipe:graph-compartment": "u1"},
            },
            # n1 references only "Data 2" -> the fallback must emit BOTH.
            "edges": [
                {"source-node": "n0", "source-slot": "Out",
                 "target-node": "n1", "target-slot": "Data 2"},
            ],
        }
        _write(os.path.join(out, "dyn.json"), g)
        reg = make_registry()
        report = R.restore(out, root, reg)
        assert len(report) == 1, report
        assert report[0][3] == "FALLBACK:generic-data", report[0]
        node = json.load(open(os.path.join(out, "dyn.json")))["nodes"]["n1"]
        cfg = node["jipipe:slot-configuration"]
        assert set(cfg["input"]) == {"Data", "Data 2"}, cfg["input"]
        for e in cfg["input"].values():
            assert e["slot-data-type"] == "jipipe:data"
    finally:
        shutil.rmtree(tmp)
    print("PASS test_dynamic_generic_fallback")


def test_static_unmatched_hard_fail():
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        _write(os.path.join(root, "p", "q.jip"), {"graph": {"nodes": {}, "edges": []}})
        g = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "src",
                       "jipipe:graph-compartment": "u0"},
                "n1": {"jipipe:node-info-id": "static-missing",
                       "jipipe:graph-compartment": "u1"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Out",
                 "target-node": "n1", "target-slot": "NotARealSlot"},
            ],
        }
        _write(os.path.join(out, "bad.json"), g)
        reg = make_registry()
        try:
            R.restore(out, root, reg)
        except SystemExit as e:
            assert e.code == 1, e.code
            print("PASS test_static_unmatched_hard_fail")
            return
        raise AssertionError("expected a hard-fail (SystemExit 1) for the "
                             "unmatched static type")
    finally:
        shutil.rmtree(tmp)


def test_exception_table():
    """The pre-made derived-0096 ruling: an explicit per-file exception emits
    a Roi2dListData input of type imagej-roi for THAT node only."""
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        _write(os.path.join(root, "p", "q.jip"), {"graph": {"nodes": {}, "edges": []}})
        fname = "dag-derived-derived-wn-cluster-0096-27-3250df34394f.json"
        g = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "src",
                       "jipipe:graph-compartment": "derived"},
                "n1": {"jipipe:node-info-id": "ij1-roi-count",
                       "jipipe:graph-compartment": "derived"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Out",
                 "target-node": "n1", "target-slot": "Roi2dListData"},
            ],
        }
        _write(os.path.join(out, fname), g)
        reg = make_registry()
        reg["ij1-roi-count"] = {
            "inputSlots": [{"name": ""}],  # empty name -> not a static slot
            "outputSlots": [{"name": "Counts"}],
        }
        report = R.restore(out, root, reg)
        assert len(report) == 1, report
        assert report[0][3].startswith("EXCEPTION:"), report[0]
        node = json.load(open(os.path.join(out, fname)))["nodes"]["n1"]
        cfg = node["jipipe:slot-configuration"]
        assert "Roi2dListData" in cfg["input"], cfg["input"]
        assert cfg["input"]["Roi2dListData"]["slot-data-type"] == "imagej-roi"
    finally:
        shutil.rmtree(tmp)
    print("PASS test_exception_table")


def test_fresh_slot_preference_over_registry():
    """The FRESH-node slot set (JVM-measured) is authoritative over the
    registry annotation list: a node type whose fresh outputs are a subset of
    its registry outputs (parameter-gated, e.g. cellpose) is affected when it
    requires an output the fresh node does NOT create - even though the
    registry lists it as static."""
    tmp = tempfile.mkdtemp()
    try:
        root = os.path.join(tmp, "corpus")
        out = os.path.join(tmp, "out")
        os.makedirs(out)
        # A corpus instance whose config covers {Labels, ROI, Probabilities}.
        _write(os.path.join(root, "p", "q.jip"), {
            "graph": {"nodes": {
                "u": {
                    "jipipe:node-info-id": "cellpose-3.x",
                    "jipipe:graph-compartment": "c",
                    "jipipe:slot-configuration": {
                        "input": {"Input": R._slot_entry("Input", "imagej-imgplus", "Input"),
                                  "Model": R._slot_entry("Model", "jipipe:data", "Input")},
                        "output": {
                            "Labels": R._slot_entry("Labels", "imagej-imgplus", "Output"),
                            "ROI": R._slot_entry("ROI", "imagej-roi", "Output"),
                            "Probabilities": R._slot_entry("Probabilities", "imagej-imgplus", "Output"),
                        },
                    },
                }}, "edges": []}})
        g = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "src", "jipipe:graph-compartment": "u0"},
                "n1": {"jipipe:node-info-id": "cellpose-3.x", "jipipe:graph-compartment": "u1"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Out", "target-node": "n1", "target-slot": "Input"},
                {"source-node": "n1", "source-slot": "ROI", "target-node": "n0", "target-slot": "Out"},
                {"source-node": "n1", "source-slot": "Probabilities", "target-node": "n0", "target-slot": "Out"},
            ],
        }
        _write(os.path.join(out, "cp.json"), g)
        reg = {"cellpose-3.x": {
                   # registry (annotation) lists ALL outputs - the over-report.
                   "inputSlots": [{"name": "Input"}, {"name": "Model"}],
                   "outputSlots": [{"name": "Labels"}, {"name": "Flows XY"},
                                   {"name": "Flows Z"}, {"name": "Flows d"},
                                   {"name": "Probabilities"}, {"name": "ROI"}]},
               # src is NOT affected: its required {Out} in/out are static.
               "src": {"inputSlots": [{"name": "Out"}],
                       "outputSlots": [{"name": "Out"}]}}
        # Simulate the committed JVM probe: the fresh node has ONLY Labels+ROI.
        R._FRESH_SLOTS = {"cellpose-3.x": {"inputs": ["Input", "Model"],
                                           "outputs": ["Labels", "ROI"]}}
        try:
            res = R.analyze(out, root, reg)
            assert "cp.json" in res, res
            n = res["cp.json"]["n1"]
            # extra = {Probabilities} (required out {ROI, Probabilities} minus
            # fresh out {Labels, ROI}). With the REGISTRY alone it would be 0.
            assert n["output"] == 1 and n["input"] == 0, n
            report = R.restore(out, root, reg)
            assert any(r[1] == "n1" and r[3].startswith("p/q.jip:") for r in report), report
        finally:
            R._FRESH_SLOTS = None  # reset the module cache
    finally:
        shutil.rmtree(tmp)
    print("PASS test_fresh_slot_preference_over_registry")


if __name__ == "__main__":
    test_deterministic_pick_and_param_copy()
    test_analyze_extras()
    test_dynamic_generic_fallback()
    test_static_unmatched_hard_fail()
    test_exception_table()
    test_fresh_slot_preference_over_registry()
    print("OK (6 tests)")
