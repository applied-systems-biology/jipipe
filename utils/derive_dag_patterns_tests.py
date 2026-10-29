#!/usr/bin/env python3
"""Tests for the pattern derivation step (utils/derive-dag-patterns.py).

The (C) review exposed a coverage gap: the mined set captures what users
DID build, not what a knowledgeable analyst WOULD build next (2D/3D twins,
plot-type variants, conversion chains like mask->filaments or
measure->annotate->use). The derivation step (C') generates those from the
shipped patterns, grounded in the real node catalog, and gates them through
the same naming rules. These tests pin the deterministic core:

* slot extraction (a node's real input/output slot names + classes, from
  multi-line @AddJIPipe(Input|Output)Slot annotations)
* 2D<->3D twin detection (slot-signature swap, not name guessing)
* the mechanical twin swap (a pure-2D pattern -> its 3D variant)
* fail-closed validation of LLM-proposed chains (unknown id, wrong slot,
  type mismatch, cycle, size)
* structural dedup against the mined set

Run standalone:

    python3 utils/derive_dag_patterns_tests.py
"""

import importlib.util
import json
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "derive-dag-patterns.py")


def load():
    spec = importlib.util.spec_from_file_location("derive_dag_patterns", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


D = load()


# the catalog node shape used throughout (== what build_catalog produces):
# {"id","name","inputs":{dataType:[slotName]}, "outputs":{dataType:[slotName]}}
def cat_node(nid, name, in_dts, out_dts):
    def conv(lst):
        out = {}
        for dt, names in lst:
            out[dt] = list(names)
        return out
    return {"id": nid, "name": name, "inputs": conv(in_dts),
            "outputs": conv(out_dts)}


# --------------------------------------------------------------------------- #

class SlotExtractionTests(unittest.TestCase):

    def test_extract_slot_names_and_classes(self):
        src = ('@SetJIPipeDocumentation(name = "X", description = "Y.")\n'
               '@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image")\n'
               '@AddJIPipeInputSlot(value = Roi2dListData.class, name = "ROI")\n'
               '@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output")\n'
               'public class FooAlgorithm extends JIPipeAlgorithm { }')
        slots = D.extract_slots(src)
        self.assertEqual({(n, c) for n, c in slots["inputs"]},
                         {("Image", "ImagePlusData"), ("ROI", "Roi2dListData")})
        self.assertEqual({(n, c) for n, c in slots["outputs"]},
                         {("Output", "Roi2dListData")})

    def test_slots_with_flags_and_multiline_annotations(self):
        src = ('@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image",\n'
               '        create = true, optional = true)\n'
               '@AddJIPipeOutputSlot(value = XData.class, name = "Out",\n'
               '        create = true)\n'
               'public class Foo { }')
        slots = D.extract_slots(src)
        self.assertEqual({n for n, _ in slots["inputs"]}, {"Image"})
        self.assertEqual({n for n, _ in slots["outputs"]}, {"Out"})

    def test_empty_source(self):
        self.assertEqual(D.extract_slots("public class Foo { }"),
                         {"inputs": [], "outputs": []})


class TwinDetectionTests(unittest.TestCase):
    """A 2D<->3D twin pair is verified by the SLOT DATA-TYPE signature being
    the dimension-swapped mirror plus the same name concept - never by id
    guessing."""

    def test_roi_pair_is_a_twin(self):
        a = cat_node("ij1-roi-filter-by-overlap", "Filter 2D ROI by overlap",
                     [("imagej-roi", ["In"])], [("imagej-roi", ["Out"])])
        b = cat_node("ij3d-roi-filter-by-overlap", "Filter IJ3D ROI by overlap",
                     [("roi-3d-list", ["In"])], [("roi-3d-list", ["Out"])])
        self.assertTrue(D.is_twin_pair(a, b))

    def test_image_blur_pair_is_a_twin(self):
        a = cat_node("ij1-blur-gaussian2d", "Gaussian blur 2D",
                     [("imagej-imgplus-2d", ["I"])], [("imagej-imgplus-2d", ["O"])])
        b = cat_node("ij1-blur-gaussian3d", "Gaussian blur 3D",
                     [("imagej-imgplus-3d", ["I"])], [("imagej-imgplus-3d", ["O"])])
        self.assertTrue(D.is_twin_pair(a, b))

    def test_same_dimension_is_not_a_twin(self):
        a = cat_node("ij1-roi-filter-by-overlap", "Filter 2D ROI by overlap",
                     [("imagej-roi", ["In"])], [("imagej-roi", ["Out"])])
        c = cat_node("ij1-roi-other", "Filter 2D ROI by overlap",
                     [("imagej-roi", ["In"])], [("imagej-roi", ["Out"])])
        self.assertFalse(D.is_twin_pair(a, c))

    def test_unrelated_is_not_a_twin(self):
        a = cat_node("ij1-roi-filter-by-overlap", "Filter 2D ROI by overlap",
                     [("imagej-roi", ["In"])], [("imagej-roi", ["Out"])])
        b = cat_node("ij3d-measure-roi3d", "Extract IJ3D ROI statistics",
                     [("roi-3d-list", ["In"]), ("imagej-imgplus", ["Ref"])],
                     [("imagej-results-table", ["Out"])])
        self.assertFalse(D.is_twin_pair(a, b))

    def test_node_dimension(self):
        self.assertEqual(D.node_dimension(
            cat_node("x", "Gaussian blur 2D", [("imagej-imgplus-2d", ["I"])],
                     [("imagej-imgplus-2d", ["O"])])), "2D")
        self.assertEqual(D.node_dimension(
            cat_node("x", "Filter 3D ROI", [("roi-3d-list", ["I"])],
                     [("roi-3d-list", ["O"])])), "3D")
        self.assertEqual(D.node_dimension(
            cat_node("x", "Remove column", [("imagej-results-table", ["I"])],
                     [("imagej-results-table", ["O"])])), None)

    def test_swap_datatype(self):
        self.assertEqual(D.swap_datatype("imagej-roi"), "roi-3d-list")
        self.assertEqual(D.swap_datatype("roi-3d-list"), "imagej-roi")
        self.assertEqual(D.swap_datatype("imagej-imgplus-2d"),
                         "imagej-imgplus-3d")
        self.assertEqual(D.swap_datatype("imagej-imgplus-3d"),
                         "imagej-imgplus-2d")
        self.assertEqual(D.swap_datatype("imagej-results-table"),
                         "imagej-results-table")


class MechanicalSwapTests(unittest.TestCase):

    def _catalog(self):
        return [
            cat_node("ij1-math-foo2d", "Foo 2D",
                     [("imagej-imgplus-2d", ["I"])], [("imagej-imgplus-2d", ["O"])]),
            cat_node("ij1-math-foo3d", "Foo 3D",
                     [("imagej-imgplus-3d", ["I"])], [("imagej-imgplus-3d", ["O"])]),
            cat_node("ij1-roi-to-mask", "Convert 2D ROI to mask",
                     [("imagej-roi", ["ROI"])], [("imagej-imgplus-2d", ["Out"])]),
            cat_node("ij3d-roi3d-to-mask", "Convert 3D ROI to mask",
                     [("roi-3d-list", ["ROI"])], [("imagej-imgplus-3d", ["Out"])]),
            cat_node("table-remove-columns", "Remove table column",
                     [("imagej-results-table", ["In"])],
                     [("imagej-results-table", ["Out"])]),
        ]

    def test_pure_2d_pattern_swaps_to_3d(self):
        graph = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "ij1-roi-to-mask"},
                "n1": {"jipipe:node-info-id": "ij1-math-foo2d"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Out",
                 "target-node": "n1", "target-slot": "I"},
            ],
        }
        out = D.mechanical_swap(graph, self._catalog(), direction="3D")
        self.assertEqual(out["nodes"]["n0"]["jipipe:node-info-id"],
                         "ij3d-roi3d-to-mask")
        self.assertEqual(out["nodes"]["n1"]["jipipe:node-info-id"],
                         "ij1-math-foo3d")
        self.assertEqual(out["edges"][0]["source-slot"], "Out")
        self.assertEqual(out["edges"][0]["target-node"], "n1")

    def test_source_endpoint_uses_twin_output_not_input(self):
        # regression: the slot lookup merged inputs+outputs (inputs first,
        # setdefault), so a SOURCE endpoint resolved to the twin's INPUT slot
        # name for the swapped data type instead of its OUTPUT slot name.
        # Only bites when the twin has both an input and an output of the same
        # dimension type with DIFFERENT names - the real ROI-statistics /
        # morph / scale families (input "ROI", output "Output"), 10 cases.
        catalog = self._catalog() + [
            cat_node("stat2d", "Stats 2D",
                     [("imagej-roi", ["ROI"])], [("imagej-roi", ["Output"])]),
            cat_node("stat3d", "Stats 3D",
                     [("roi-3d-list", ["ROI"])], [("roi-3d-list", ["Output"])]),
            cat_node("consumer", "Consume a results table (neutral sink)",
                     [("imagej-results-table", ["ROI"])],
                     [("imagej-results-table", ["Out"])]),
        ]
        graph = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "stat2d"},
                "n1": {"jipipe:node-info-id": "consumer"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Output",
                 "target-node": "n1", "target-slot": "ROI"},
            ],
        }
        out = D.mechanical_swap(graph, catalog, direction="3D")
        self.assertEqual(out["nodes"]["n0"]["jipipe:node-info-id"], "stat3d")
        e = out["edges"][0]
        # the source is stat3d's OUTPUT slot (roi-3d-list) = "Output",
        # NOT its input slot "ROI" (the old bug produced "ROI")
        self.assertEqual(e["source-slot"], "Output")
        self.assertEqual(e["target-slot"], "ROI")

    def test_renamed_slot_is_remapped_in_kebab_case(self):
        # regression: the real import-imagej-roi-from-file (2D, input slot
        # "Files", dt "file") vs import-roi-3d (3D, input slot "Input", dt
        # "file"). The remapped name must land in the KEBAB-CASE (A) keys
        # (source-slot/target-slot) - writing it to the snake_case keys left
        # the stale 2D name in the shipped edge (import-roi-3d "Files" does
        # not exist -> deserialization break at (D)).
        catalog = self._catalog() + [
            cat_node("path-annotation", "Path to annotation",
                     [], [("path", ["Paths"])]),
            cat_node("import-2d-roi", "Import 2D ROI file",
                     [("file", ["Files"])], [("imagej-roi", ["Output"])]),
            cat_node("import-3d-roi", "Import 3D ROI file",
                     [("file", ["Input"])], [("roi-3d-list", ["Output"])]),
        ]
        graph = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "path-annotation"},
                "n1": {"jipipe:node-info-id": "import-2d-roi"},
            },
            "edges": [
                {"source-node": "n0", "source-slot": "Paths",
                 "target-node": "n1", "target-slot": "Files"},
            ],
        }
        out = D.mechanical_swap(graph, catalog, direction="3D")
        self.assertEqual(out["nodes"]["n1"]["jipipe:node-info-id"],
                         "import-3d-roi")
        e = out["edges"][0]
        self.assertEqual(e["target-slot"], "Input")  # remapped, kebab-case
        self.assertEqual(e["source-slot"], "Paths")  # neutral, untouched
        self.assertNotIn("target_slot", e)  # no stray snake_case keys
        self.assertNotIn("source_slot", e)

    def test_pattern_with_no_dimensioned_nodes_is_not_swapped(self):
        graph = {
            "nodes": {"n0": {"jipipe:node-info-id": "table-remove-columns"}},
            "edges": [],
        }
        self.assertIsNone(D.mechanical_swap(graph, self._catalog(),
                                            direction="3D"))

    def test_mixed_dimension_pattern_is_not_swapped(self):
        graph = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "ij1-roi-to-mask"},    # 2D
                "n1": {"jipipe:node-info-id": "ij3d-roi3d-to-mask"},  # 3D
            },
            "edges": [],
        }
        self.assertIsNone(D.mechanical_swap(graph, self._catalog(),
                                            direction="2D"))

    def test_node_without_a_twin_blocks_the_swap(self):
        graph = {
            "nodes": {
                "n0": {"jipipe:node-info-id": "ij1-roi-to-mask"},
                "n1": {"jipipe:node-info-id": "table-remove-columns"},  # neutral:
            },
            "edges": [],
        }
        # neutral nodes are fine; add a 2D node with no 3D twin
        catalog = self._catalog() + [
            cat_node("ij1-unique-2d", "Unique 2D",
                     [("imagej-imgplus-2d", ["I"])],
                     [("imagej-imgplus-2d", ["O"])])]
        graph["nodes"]["n2"] = {"jipipe:node-info-id": "ij1-unique-2d"}
        self.assertIsNone(D.mechanical_swap(graph, catalog, direction="3D"))


class ValidationTests(unittest.TestCase):
    """LLM-proposed chains are fail-closed: every node id must exist, every
    edge must name real slots on both ends, types must be assignable, and
    the result must be a small DAG."""

    def _catalog(self):
        return {
            "ij1-roi-to-mask": cat_node(
                "ij1-roi-to-mask", "Convert 2D ROI to mask",
                [("imagej-roi", ["ROI"])], [("imagej-imgplus-2d", ["Out"])]),
            "ij1-math-foo2d": cat_node(
                "ij1-math-foo2d", "Foo 2D",
                [("imagej-imgplus-2d", ["I"])], [("imagej-imgplus-2d", ["O"])]),
        }

    def test_valid_chain_passes(self):
        proposal = {
            "name": "x", "description": "y",
            "nodes": [{"id": "ij1-roi-to-mask"}, {"id": "ij1-math-foo2d"}],
            "edges": [{"source": "ij1-roi-to-mask", "source_slot": "Out",
                       "target": "ij1-math-foo2d", "target_slot": "I"}],
        }
        self.assertIsNone(D.validate_proposal(proposal, self._catalog()))

    def test_unknown_node_id_fails(self):
        proposal = {"name": "x", "description": "y",
                    "nodes": [{"id": "does-not-exist"}], "edges": []}
        self.assertIsNotNone(D.validate_proposal(proposal, self._catalog()))

    def test_wrong_slot_name_fails(self):
        proposal = {
            "name": "x", "description": "y",
            "nodes": [{"id": "ij1-roi-to-mask"}, {"id": "ij1-math-foo2d"}],
            "edges": [{"source": "ij1-roi-to-mask", "source_slot": "Bogus",
                       "target": "ij1-math-foo2d", "target_slot": "I"}],
        }
        self.assertIsNotNone(D.validate_proposal(proposal, self._catalog()))

    def test_type_mismatch_fails(self):
        catalog = {
            "a": cat_node("a", "A", [("imagej-imgplus-2d", ["In"])],
                          [("imagej-imgplus-2d", ["Out"])]),
            "b": cat_node("b", "B", [("imagej-roi", ["In"])],
                          [("imagej-roi", ["Out"])]),
        }
        proposal = {
            "name": "x", "description": "y",
            "nodes": [{"id": "a"}, {"id": "b"}],
            "edges": [{"source": "a", "source_slot": "Out",
                       "target": "b", "target_slot": "In"}],
        }
        # a outputs imagej-imgplus-2d; b wants imagej-roi -> mismatch
        self.assertIsNotNone(D.validate_proposal(proposal, catalog))

    def test_specialized_type_fits_general_input(self):
        # imagej-imgplus-2d feeds an imagej-imgplus input (specialization)
        catalog = {
            "a": cat_node("a", "A", [("imagej-imgplus-2d", ["In"])],
                          [("imagej-imgplus-2d", ["Out"])]),
            "b": cat_node("b", "B", [("imagej-imgplus", ["In"])],
                          [("imagej-imgplus", ["Out"])]),
        }
        proposal = {
            "name": "x", "description": "y",
            "nodes": [{"id": "a"}, {"id": "b"}],
            "edges": [{"source": "a", "source_slot": "Out",
                       "target": "b", "target_slot": "In"}],
        }
        self.assertIsNone(D.validate_proposal(proposal, catalog))

    def test_cycle_fails(self):
        catalog = {
            "a": cat_node("a", "A", [("t", ["ROI"])], [("t", ["Out"])]),
            "b": cat_node("b", "B", [("t", ["I"])], [("t", ["Out"])]),
        }
        proposal = {
            "name": "x", "description": "y",
            "nodes": [{"id": "a"}, {"id": "b"}],
            "edges": [{"source": "a", "source_slot": "Out",
                       "target": "b", "target_slot": "I"},
                      {"source": "b", "source_slot": "Out",
                       "target": "a", "target_slot": "ROI"}],
        }
        self.assertIsNotNone(D.validate_proposal(proposal, catalog))

    def test_oversized_chain_fails(self):
        catalog = {}
        nodes = []
        for i in range(11):
            nid = f"n{i}"
            catalog[nid] = cat_node(nid, f"N{i}", [("t", ["In"])], [("t", ["Out"])])
            nodes.append({"id": nid})
        edges = [{"source": f"n{i}", "source_slot": "Out",
                  "target": f"n{i+1}", "target_slot": "In"} for i in range(10)]
        proposal = {"name": "x", "description": "y",
                    "nodes": nodes, "edges": edges}
        self.assertIsNotNone(D.validate_proposal(proposal, catalog))


class DedupTests(unittest.TestCase):

    def test_same_concept_relabelled_collides(self):
        # the dedup is label-agnostic: a derived pattern that is the same
        # structure as a mined one (or another derived one) collapses to the
        # same signature even though the node-instance labels differ
        g1 = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                        "n1": {"jipipe:node-info-id": "b"}},
              "edges": [{"source-node": "n0", "source-slot": "Out",
                         "target-node": "n1", "target-slot": "In"}]}
        g2 = {"nodes": {"x9": {"jipipe:node-info-id": "b"},
                        "x8": {"jipipe:node-info-id": "a"}},
              "edges": [{"source-node": "x8", "source-slot": "Out",
                         "target-node": "x9", "target-slot": "In"}]}
        self.assertEqual(D.structural_signature(g1),
                         D.structural_signature(g2))

    def test_identical_graphs_collide(self):
        g1 = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                        "n1": {"jipipe:node-info-id": "b"}},
              "edges": [{"source-node": "n0", "source-slot": "Out",
                         "target-node": "n1", "target-slot": "In"}]}
        self.assertEqual(D.structural_signature(g1),
                         D.structural_signature(g1))

    def test_different_wiring_is_different(self):
        g1 = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                        "n1": {"jipipe:node-info-id": "b"}},
              "edges": [{"source-node": "n0", "source-slot": "Out",
                         "target-node": "n1", "target-slot": "In"}]}
        g2 = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                        "n1": {"jipipe:node-info-id": "b"}},
              "edges": [{"source-node": "n0", "source-slot": "Out",
                         "target-node": "n1", "target-slot": "In2"}]}
        self.assertNotEqual(D.structural_signature(g1),
                            D.structural_signature(g2))

    def test_different_node_types_is_different(self):
        g1 = {"nodes": {"n0": {"jipipe:node-info-id": "a"}}, "edges": []}
        g2 = {"nodes": {"n0": {"jipipe:node-info-id": "z"}}, "edges": []}
        self.assertNotEqual(D.structural_signature(g1),
                            D.structural_signature(g2))

    def test_duplicate_node_ids_count(self):
        g = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                       "n1": {"jipipe:node-info-id": "a"}},
             "edges": []}
        self.assertEqual(D.structural_signature(g)[0], ("a", "a"))


class CatalogSelectionTests(unittest.TestCase):
    """For a pattern, the capability catalog = nodes that could plausibly
    FOLLOW it (consume one of the pattern's input or output data types) plus
    same-function nodes, capped. The LLM gets this, not the whole 1500-node
    registry."""

    def _node(self, nid, name, func, ins, outs):
        return {"id": nid, "name": name, "function": func,
                "inputs": {d: ["In"] for d in ins},
                "outputs": {d: ["Out"] for d in outs}}

    def test_consumers_of_pattern_outputs_are_selected(self):
        catalog = [
            self._node("roi-to-rgb", "Convert 2D ROI to RGB", "rois",
                       ["imagej-roi"], ["imagej-imgplus"]),
            self._node("unrelated-table", "Table thing", "tables",
                       ["imagej-results-table"], ["imagej-results-table"]),
        ]
        # pattern outputs imagej-roi
        sel = D.select_catalog(catalog, pattern_dts={"imagej-roi"},
                               function="rois", cap=50)
        ids = {n["id"] for n in sel}
        self.assertIn("roi-to-rgb", ids)

    def test_cap_is_respected(self):
        catalog = [self._node(f"n{i}", f"N {i}", "rois", ["imagej-roi"],
                              ["imagej-imgplus"]) for i in range(30)]
        sel = D.select_catalog(catalog, pattern_dts={"imagej-roi"},
                               function="rois", cap=10)
        self.assertLessEqual(len(sel), 10)

    def test_selection_is_deterministic(self):
        catalog = [self._node(f"n{i}", f"N {i}", "rois", ["imagej-roi"],
                              ["imagej-imgplus"]) for i in range(12)]
        a = [n["id"] for n in D.select_catalog(catalog,
                                               pattern_dts={"imagej-roi"},
                                               function="rois", cap=10)]
        b = [n["id"] for n in D.select_catalog(catalog,
                                               pattern_dts={"imagej-roi"},
                                               function="rois", cap=10)]
        self.assertEqual(a, b)


class WhatNextPromptTests(unittest.TestCase):

    def test_prompt_has_pattern_catalog_and_rules(self):
        state = {"pattern": {
            "nodes": [{"id": "a", "name": "Auto threshold 2D (8-bit)",
                       "description": "Thresholds the image."}],
            "nodeSequence": "Auto threshold", "wiring": "",
            "nodeCount": 1, "edgeCount": 0, "support": 5, "signal": "x",
            "observedTitles": []}}
        catalog = [{"id": "find-particles", "name": "Find particles 2D",
                    "inputs": {"imagej-imgplus": ["Mask"]},
                    "outputs": {"imagej-roi": ["Out"]}}]
        p = D.build_what_next_prompt(state, catalog)
        self.assertIn("Auto threshold", p)
        self.assertIn("Find particles 2D", p)
        self.assertIn("what", p.lower())  # the "what next" framing
        self.assertIn('"nodes"', p)       # the answer schema
        self.assertIn('"edges"', p)
        # must forbid inventing ids/slots
        self.assertIn("existing", p.lower())

    def test_prompt_carries_the_seed_families(self):
        # the user's four "what next" example families are prompt hints so the
        # model knows the KIND of derivation wanted (conversion chains, not
        # just single-node extensions)
        state = {"pattern": {
            "nodes": [{"id": "a", "name": "Auto threshold 2D (8-bit)",
                       "description": "Thresholds the image."}],
            "nodeSequence": "Auto threshold", "wiring": "",
            "nodeCount": 1, "edgeCount": 0, "support": 5, "signal": "x",
            "observedTitles": []}}
        p = D.build_what_next_prompt(state, [])
        self.assertIn("filament", p.lower())
        self.assertIn("annotat", p.lower())


class ProposalParsingTests(unittest.TestCase):

    def test_parses_a_valid_proposal_list(self):
        import json
        raw = json.dumps([{"name": "n1", "description": "d1",
                           "nodes": [{"id": "a"}], "edges": []}])
        props = D.parse_proposals(raw)
        self.assertEqual(len(props), 1)
        self.assertEqual(props[0]["nodes"], [{"id": "a"}])

    def test_caps_at_max_proposals(self):
        import json
        raw = json.dumps([{"name": f"n{i}", "description": "d",
                           "nodes": [{"id": "a"}], "edges": []}
                          for i in range(5)])
        props = D.parse_proposals(raw)
        self.assertLessEqual(len(props), D.MAX_PROPOSALS_PER_PATTERN)

    def test_rejects_empty_or_malformed(self):
        self.assertEqual(D.parse_proposals("no json"), [])
        self.assertEqual(D.parse_proposals("{}"), [])
        self.assertEqual(D.parse_proposals("[]"), [])


class BuildCatalogTests(unittest.TestCase):
    """The catalog joins registry.json (data types per slot) with the source
    @AddJIPipe(Input|Output)Slot annotations (slot names per class) - the LLM
    must see exact slot names to wire valid chains."""

    def _reg_node(self, nid, name, func, ins, outs, extra=None):
        def conv(lst):
            return [{"class": f"org.x.{c}", "dataType": dt} for c, dt in lst]
        def named(lst):
            return [{"class": f"org.x.{c}", "dataType": dt, "name": nm}
                    for c, dt, nm in lst]
        node = {"id": nid, "name": name, "functions": [func],
                "inputs": conv(ins), "outputs": conv(outs)}
        if extra:
            node.update(extra)
        return node

    def test_join_yields_slot_names_per_datatype(self):
        reg = [self._reg_node("ij1-roi-to-rgb", "Convert 2D ROI to RGB",
                              "jipipe:function:roi-filters",
                              [("Roi2dListData", "imagej-roi"),
                               ("ImagePlusData", "imagej-imgplus")],
                              [("ImagePlusColorRGBData",
                                "imagej-imgplus-color-rgb")])]
        source_slots = {"ij1-roi-to-rgb": {
            "inputs": [("ROI", "Roi2dListData"), ("Image", "ImagePlusData")],
            "outputs": [("Output", "ImagePlusColorRGBData")]}}
        cat = D.build_catalog(reg, source_slots)
        self.assertEqual(cat[0]["inputs"],
                         {"imagej-roi": ["ROI"], "imagej-imgplus": ["Image"]})
        self.assertEqual(cat[0]["outputs"],
                         {"imagej-imgplus-color-rgb": ["Output"]})

    def test_node_without_source_slots_falls_back_to_class_names(self):
        # a node whose class file was not found still gets a catalog entry,
        # using the class simple name as the slot label (best effort -
        # validation is fail-closed on the real slots)
        reg = [self._reg_node("x-node", "X node", "jipipe:function:misc",
                              [("SomeData", "some-dt")], [])]
        cat = D.build_catalog(reg, {})
        self.assertEqual(cat[0]["inputs"], {"some-dt": ["SomeData"]})

    def test_registry_slot_names_fallback_for_dynamic_nodes(self):
        # 2026-10-19 re-sweep: dynamically-registered nodes (cLij2/clijx
        # kernels) have NO source @AddJIPipe*Slot annotations, so the
        # source-slot scan cannot verify their slots. The registry dump
        # (inputSlots/outputSlots with the runtime slot name, 2072-node
        # full-launcher registry) is authoritative for them - a node absent
        # from the source scan but present with named slots in the registry
        # gets those slot names and slots_verified=True.
        reg = [self._reg_node("clij:plugin:net.x.Edof", "CLIJ2 Edof",
                              "jipipe:function:misc",
                              [("CLIJImageData", "clij2-image")],
                              [("CLIJImageData", "clij2-image")],
                              extra={
                                  "inputSlots": [
                                      {"class": "org.x.CLIJImageData",
                                       "dataType": "clij2-image",
                                       "name": "Source"}],
                                  "outputSlots": [
                                      {"class": "org.x.CLIJImageData",
                                       "dataType": "clij2-image",
                                       "name": "Destination"}],
                              })]
        cat = D.build_catalog(reg, {})
        self.assertEqual(cat[0]["inputs"], {"clij2-image": ["Source"]})
        self.assertEqual(cat[0]["outputs"], {"clij2-image": ["Destination"]})
        self.assertTrue(cat[0]["slots_verified"])

    def test_source_slots_still_take_precedence_over_registry_names(self):
        # for static (source-parseable) nodes the source annotation remains
        # the verification basis; the registry slot names are only a
        # fallback for nodes the source scan cannot see
        reg = [self._reg_node("ij1-roi-to-rgb", "Convert 2D ROI to RGB",
                              "jipipe:function:roi-filters",
                              [("Roi2dListData", "imagej-roi"),
                               ("ImagePlusData", "imagej-imgplus")],
                              [("ImagePlusColorRGBData",
                                "imagej-imgplus-color-rgb")],
                              extra={
                                  "inputSlots": [
                                      {"class": "org.x.Roi2dListData",
                                       "dataType": "imagej-roi",
                                       "name": "ROI"},
                                      {"class": "org.x.ImagePlusData",
                                       "dataType": "imagej-imgplus",
                                       "name": "Image"}],
                                  "outputSlots": [
                                      {"class": "org.x.ImagePlusColorRGBData",
                                       "dataType": "imagej-imgplus-color-rgb",
                                       "name": "Output"}],
                              })]
        source_slots = {"ij1-roi-to-rgb": {
            "inputs": [("ROI", "Roi2dListData"), ("Image", "ImagePlusData")],
            "outputs": [("Output", "ImagePlusColorRGBData")]}}
        cat = D.build_catalog(reg, source_slots)
        self.assertEqual(cat[0]["inputs"],
                         {"imagej-roi": ["ROI"], "imagej-imgplus": ["Image"]})
        self.assertEqual(cat[0]["outputs"],
                         {"imagej-imgplus-color-rgb": ["Output"]})
        self.assertTrue(cat[0]["slots_verified"])

    def test_registry_slot_names_without_any_name_stay_unverified(self):
        # a registry slot entry with an EMPTY name carries no usable slot
        # name - the fail-closed default (slots_verified=False) must hold
        reg = [self._reg_node("clij:plugin:net.x.Anon", "CLIJ2 Anon",
                              "jipipe:function:misc",
                              [("CLIJImageData", "clij2-image")],
                              [("CLIJImageData", "clij2-image")],
                              extra={
                                  "inputSlots": [
                                      {"class": "org.x.CLIJImageData",
                                       "dataType": "clij2-image",
                                       "name": ""}],
                                  "outputSlots": [
                                      {"class": "org.x.CLIJImageData",
                                       "dataType": "clij2-image",
                                       "name": ""}],
                              })]
        cat = D.build_catalog(reg, {})
        self.assertFalse(cat[0]["slots_verified"])

    def test_branch_of_function_uses_the_parent_chain(self):
        # the branch is the TOP node of the function tree, walked via the
        # parent chain - NOT a first-token guess (filament-data is under
        # "data", not "filaments")
        reg = json.load(open(os.path.join(
            HERE, "taxonomy-research", "registry.json")))
        bi = D.build_branch_index(reg["functions"])
        self.assertEqual(D.branch_of_function("jipipe:function:roi-filters", bi), "rois")
        self.assertEqual(D.branch_of_function("jipipe:function:filament-data", bi), "data")
        self.assertEqual(D.branch_of_function("jipipe:function:filament-quantification", bi), "filaments")
        self.assertEqual(D.branch_of_function("jipipe:function:plot-line", bi), "plotting")
        self.assertEqual(D.branch_of_function("jipipe:function:tracking-analysis", bi), "tracking")
        self.assertEqual(D.branch_of_function("jipipe:function:table-transform", bi), "tables")
        self.assertEqual(D.branch_of_function("jipipe:function:omero", bi), "data")
        self.assertEqual(D.branch_of_function("jipipe:function:annotations-edit", bi), "annotations")
        # without the index a flat top-level id maps to itself, others to misc
        self.assertEqual(D.branch_of_function("jipipe:function:annotations"), "annotations")
        self.assertEqual(D.branch_of_function("jipipe:function:some-leaf"), "misc")

    def test_catalog_carries_the_branch(self):
        reg = [self._reg_node("ij1-roi-to-rgb", "Convert 2D ROI to RGB",
                              "jipipe:function:roi-filters",
                              [("Roi2dListData", "imagej-roi")],
                              [("ImagePlusColorRGBData", "imagej-imgplus-color-rgb")])]
        source_slots = {"ij1-roi-to-rgb": {
            "inputs": [("ROI", "Roi2dListData")], "outputs": [("Output", "ImagePlusColorRGBData")]}}
        functions = [{"id": "jipipe:function:roi-filters", "name": "ROI filters",
                      "parent": "jipipe:function:rois"},
                     {"id": "jipipe:function:rois", "name": "ROIs", "parent": None}]
        cat = D.build_catalog(reg, source_slots, functions)
        self.assertEqual(cat[0]["function"], "rois")


class MergeShardsTests(unittest.TestCase):
    """The full what-next sweep is sharded (parallel jobs); the merge keeps
    one representative per structural signature, preferring a gate-SHIPPED
    entry over a gate-rejected one, and a deterministic twin over a
    what-next. Twins are deduped by clusterId (they are deterministic; a
    twin-only regeneration supersedes stale in-shard twins even when the
    slot remap changed the signature)."""

    def _entry(self, cid, mech, name="N"):
        return {"clusterId": cid, "support": 0, "ship": True, "gate": "general_pattern",
                "functionId": "jipipe:function:rois", "name": name, "description": "D",
                "error": None, "verdict": None,
                "provenance": {"mechanism": mech, "derivedFrom": "cluster-0001"},
                "state": {"pattern": {"graph": {
                    "nodes": {"n0": {"jipipe:node-info-id": f"node-{cid}"}},
                    "edges": []}}}}

    def test_mechanical_twins_taken_once(self):
        shard0 = {"entries": [self._entry("derived-mech-cluster-0003-3d", "twin-swap", "T3D"),
                              self._entry("derived-wn-cluster-0003-0", "what-next", "W0")]}
        shard1 = {"entries": [self._entry("derived-mech-cluster-0003-3d", "twin-swap", "T3D"),
                              self._entry("derived-wn-cluster-0004-0", "what-next", "W1")]}
        merged = D.merge_shards([shard0, shard1])
        ids = [e["clusterId"] for e in merged]
        self.assertEqual(ids.count("derived-mech-cluster-0003-3d"), 1,
                         "the deterministic twins must appear exactly once")
        self.assertEqual(sorted(ids),
                         ["derived-mech-cluster-0003-3d",
                          "derived-wn-cluster-0003-0", "derived-wn-cluster-0004-0"])

    def test_structural_dedup_across_shards(self):
        # two shards independently derived the same structure (same node id +
        # wiring) - the merge keeps one
        e1 = self._entry("derived-wn-cluster-0003-0", "what-next")
        e2 = self._entry("derived-wn-cluster-0004-0", "what-next")
        # force the same graph structure
        e2["state"]["pattern"]["graph"]["nodes"]["n0"]["jipipe:node-info-id"] = \
            e1["state"]["pattern"]["graph"]["nodes"]["n0"]["jipipe:node-info-id"]
        merged = D.merge_shards([{"entries": [e1]}, {"entries": [e2]}])
        self.assertEqual(len(merged), 1)

    def test_dedup_against_mined_signatures(self):
        e1 = self._entry("derived-wn-cluster-0003-0", "what-next")
        mined_sig = D.structural_signature(
            {"nodes": {"n0": {"jipipe:node-info-id":
                              e1["state"]["pattern"]["graph"]["nodes"]["n0"]
                              ["jipipe:node-info-id"]}},
             "edges": []})
        merged = D.merge_shards([{"entries": [e1]}],
                                mined_sigs={mined_sig})
        self.assertEqual(merged, [])

    def test_shipped_variant_beats_rejected_same_structure(self):
        # regression: the first merge kept the FIRST entry per signature, so
        # a gate-rejected what-next (same structure as a gate-PASSED one from
        # another source pattern) could collapse its shipped sibling - this
        # lost 3 real patterns incl. "Annotate data with image statistics"
        # (seed family 4). The shipped variant must win regardless of order.
        e_rej = self._entry("derived-wn-cluster-0001-0", "what-next", "Bad Name")
        e_rej.update({"ship": False, "gate": "noise"})
        e_ship = self._entry("derived-wn-cluster-0002-0", "what-next", "Good Name")
        # same graph structure as e_rej
        e_ship["state"]["pattern"]["graph"] = json.loads(
            json.dumps(e_rej["state"]["pattern"]["graph"]))
        for shards in ([{"entries": [e_rej]}, {"entries": [e_ship]}],
                       [{"entries": [e_ship]}, {"entries": [e_rej]}]):
            merged = D.merge_shards([s.copy() for s in shards])
            self.assertEqual(len(merged), 1)
            self.assertTrue(merged[0]["ship"],
                            "the gate-passed variant must survive the merge")
            self.assertEqual(merged[0]["name"], "Good Name")

    def test_order_is_deterministic(self):
        shard0 = {"entries": [self._entry("derived-wn-cluster-0003-0", "what-next", "W0")]}
        shard1 = {"entries": [self._entry("derived-wn-cluster-0004-0", "what-next", "W1")]}
        a = [e["clusterId"] for e in D.merge_shards([shard0, shard1])]
        b = [e["clusterId"] for e in D.merge_shards([shard1, shard0])]
        self.assertEqual(a, b)


class MechanicalTwinEntryTests(unittest.TestCase):
    """A verified twin of a SHIPPED pattern is a legitimate general pattern
    by construction - the LLM gate (which sees support 0) wrongly rejected
    16/26 of them in the pilot. The twin entry is built deterministically:
    inherit the source's ship + function, rename 2D<->3D in name/description."""

    def _catalog(self):
        return [
            cat_node("ij1-roi-filter-by-overlap", "Filter 2D ROI by overlap",
                     [("imagej-roi", ["In"])], [("imagej-roi", ["Out"])]),
            cat_node("ij3d-roi-filter-by-overlap", "Filter IJ3D ROI by overlap",
                     [("roi-3d-list", ["In"])], [("roi-3d-list", ["Out"])]),
        ]

    def _shipped_entry(self):
        return {"clusterId": "cluster-0003", "support": 34,
                "functionId": "jipipe:function:rois",
                "name": "Filter 2D ROIs by overlap",
                "description": "Keeps 2D ROIs that overlap another 2D ROI list.",
                "ship": True, "gate": "general_pattern"}

    def _graph(self):
        return {"nodes": {"n0": {"jipipe:node-info-id": "ij1-roi-filter-by-overlap",
                                 "jipipe:node:name": "Filter 2D ROI by overlap"}},
                "edges": []}

    def test_twin_entry_is_deterministic_and_shipped(self):
        e = D.mechanical_twin_entry(self._shipped_entry(), self._graph(),
                                    "3D", self._catalog())
        self.assertIsNotNone(e)
        self.assertTrue(e["ship"])
        self.assertEqual(e["gate"], "general_pattern")
        self.assertEqual(e["functionId"], "jipipe:function:rois")
        # the node was swapped to the verified 3D twin
        g = e["state"]["pattern"]["graph"]
        self.assertEqual(list(g["nodes"].values())[0]["jipipe:node-info-id"],
                         "ij3d-roi-filter-by-overlap")
        # name + description were dimension-renamed 2D -> 3D
        self.assertIn("3D", e["name"])
        self.assertNotIn("2D", e["name"])
        self.assertIn("3D", e["description"])
        # provenance recorded (artifact only), support 0
        self.assertEqual(e["provenance"]["mechanism"], "twin-swap")
        self.assertEqual(e["provenance"]["derivedFrom"], "cluster-0003")
        self.assertEqual(e["support"], 0)

    def test_unswappable_returns_none(self):
        # a pattern whose node has no 3D twin is not a mechanical twin
        graph = {"nodes": {"n0": {"jipipe:node-info-id": "table-remove-columns",
                                  "jipipe:node:name": "Remove table column"}},
                 "edges": []}
        catalog = [cat_node("table-remove-columns", "Remove table column",
                            [("imagej-results-table", ["In"])],
                            [("imagej-results-table", ["Out"])])]
        self.assertIsNone(D.mechanical_twin_entry(self._shipped_entry(),
                                                  graph, "3D", catalog))

    def test_dimension_rename_both_directions(self):
        self.assertEqual(D.dimension_rename("Filter 2D ROIs by overlap", "3D"),
                         "Filter 3D ROIs by overlap")
        self.assertEqual(D.dimension_rename("Filter 3D ROIs by overlap", "2D"),
                         "Filter 2D ROIs by overlap")
        # no dimension token -> unchanged
        self.assertEqual(D.dimension_rename("Export results table", "3D"),
                         "Export results table")
        self.assertEqual(D.dimension_rename(None, "3D"), None)

    def test_3d_to_2d_twin_of_a_3d_pattern(self):
        e = D.mechanical_twin_entry(
            {"clusterId": "cluster-0005", "support": 31,
             "functionId": "jipipe:function:rois",
             "name": "Attach 3D ROI measurements to data",
             "description": "Extracts 3D ROI statistics.", "ship": True,
             "gate": "general_pattern"},
            {"nodes": {"n0": {"jipipe:node-info-id": "ij3d-roi-filter-by-overlap",
                              "jipipe:node:name": "Filter IJ3D ROI by overlap"}},
             "edges": []},
            "2D", self._catalog())
        self.assertIsNotNone(e)
        self.assertEqual(list(e["state"]["pattern"]["graph"]["nodes"]
                               .values())[0]["jipipe:node-info-id"],
                         "ij1-roi-filter-by-overlap")
        self.assertIn("2D", e["name"])


class PatternGraphTests(unittest.TestCase):
    """Derived candidates must be emitted in the exact (A) minimal-graph
    format (D) consumes: jipipe:node-info-id, a graph compartment, and a
    location/<compartment>/x|y pair under jipipe:metadata-v1 for EVERY node
    (the plan's 3.4 guard)."""

    def test_to_pattern_graph_adds_compartment_and_locations(self):
        g = {"nodes": {"n0": {"jipipe:node-info-id": "a"},
                       "n1": {"jipipe:node-info-id": "b"}},
             "edges": [{"source-node": "n0", "source-slot": "Out",
                        "target-node": "n1", "target-slot": "In"}]}
        out = D.to_pattern_graph(g)
        comp = out["nodes"]["n0"]["jipipe:graph-compartment"]
        self.assertTrue(all(n["jipipe:graph-compartment"] == comp
                            for n in out["nodes"].values()))
        for n in out["nodes"].values():
            self.assertIn(f"location/{comp}/x", n["jipipe:metadata-v1"])
            self.assertIn(f"location/{comp}/y", n["jipipe:metadata-v1"])
        # longest-path layout: n1 (downstream) is to the right of n0
        x0 = out["nodes"]["n0"]["jipipe:metadata-v1"][f"location/{comp}/x"]
        x1 = out["nodes"]["n1"]["jipipe:metadata-v1"][f"location/{comp}/x"]
        self.assertGreater(x1, x0)

    def test_to_pattern_graph_is_deterministic(self):
        g = {"nodes": {"n0": {"jipipe:node-info-id": "a"}}, "edges": []}
        self.assertEqual(D.to_pattern_graph(g), D.to_pattern_graph(g))


class DerivedJudgeTests(unittest.TestCase):

    def test_general_pattern_with_name_ships(self):
        import json
        llm = lambda prompt: json.dumps(
            {"gate": "general_pattern", "function": "rois",
             "name": "N", "description": "D"})
        cluster = {"clusterId": "derived-0001", "support": 0,
                   "representativeCandidate": {"graph": {
                       "nodes": {"n0": {"jipipe:node-info-id": "a",
                                        "jipipe:node:name": "A"}},
                       "edges": [], "nodeCount": 1, "edgeCount": 0,
                       "signal": "derived"}}}
        j = D.judge_derived(cluster, llm)
        self.assertTrue(j["ship"])
        self.assertEqual(j["functionId"], "jipipe:function:rois")

    def test_noise_rejects(self):
        import json
        llm = lambda prompt: json.dumps(
            {"gate": "noise", "function": "misc", "name": "", "description": ""})
        cluster = {"clusterId": "derived-0002", "support": 0,
                   "representativeCandidate": {"graph": {
                       "nodes": {"n0": {"jipipe:node-info-id": "a",
                                        "jipipe:node:name": "A"}},
                       "edges": [], "nodeCount": 1, "edgeCount": 0,
                       "signal": "derived"}}}
        j = D.judge_derived(cluster, llm)
        self.assertFalse(j["ship"])

    def test_overlong_name_rejects_fail_closed(self):
        import json
        llm = lambda prompt: json.dumps(
            {"gate": "general_pattern", "function": "rois",
             "name": "x" * 200, "description": "D"})
        cluster = {"clusterId": "derived-0003", "support": 0,
                   "representativeCandidate": {"graph": {
                       "nodes": {"n0": {"jipipe:node-info-id": "a",
                                        "jipipe:node:name": "A"}},
                       "edges": [], "nodeCount": 1, "edgeCount": 0,
                       "signal": "derived"}}}
        j = D.judge_derived(cluster, llm)
        self.assertFalse(j["ship"])


class MergeDerivedOutputsTests(unittest.TestCase):
    """The merge RECOMPUTES the deterministic twins with the current code
    (the in-shard twins may have been produced by an older version - the
    real incident: the podman run predates the kebab-case slot-remap fix, so
    2 import-roi-3d twins shipped the stale 2D slot name "Files"). Only the
    LLM what-next entries come from the shards."""

    def _twin(self, cid, slot="Input"):
        return {"clusterId": cid, "support": 0, "ship": True,
                "gate": "general_pattern", "functionId": "jipipe:function:rois",
                "name": "Twin", "description": "D", "error": None, "verdict": None,
                "provenance": {"mechanism": "twin-swap", "derivedFrom": "c0"},
                "state": {"pattern": {"graph": {
                    "nodes": {"n9": {"jipipe:node-info-id": "path-annotation"},
                               "n0": {"jipipe:node-info-id": "import-3d-roi"}},
                    "edges": [{"source-node": "n9", "source-slot": "Paths",
                               "target-node": "n0", "target-slot": slot}]}}}}

    def _wn(self, cid, ship=True, node="export-data", twin_like=False):
        if twin_like:
            # the same structure as a twin (for the cross-collision tests)
            graph = {"nodes": {"n9": {"jipipe:node-info-id": "path-annotation"},
                                "n0": {"jipipe:node-info-id": "import-3d-roi"}},
                     "edges": [{"source-node": "n9", "source-slot": "Paths",
                                "target-node": "n0", "target-slot": "Input"}]}
        else:
            graph = {"nodes": {"n0": {"jipipe:node-info-id": node}}, "edges": []}
        return {"clusterId": cid, "support": 0, "ship": ship,
                "gate": "general_pattern" if ship else "noise",
                "functionId": "jipipe:function:export",
                "name": "WN" if ship else "bad", "description": "D",
                "error": None, "verdict": None,
                "provenance": {"mechanism": "what-next", "derivedFrom": "c1"},
                "state": {"pattern": {"graph": graph}}}

    def test_stale_in_shard_twin_is_replaced_by_recomputed(self):
        shards = [{"entries": [self._twin("derived-mech-c0-3d", slot="Files"),
                               self._wn("derived-wn-c1-0")]}]
        fresh = self._twin("derived-mech-c0-3d", slot="Input")
        merged = D.merge_derived_outputs(shards, [fresh], mined_sigs=set())
        twin = [e for e in merged if e["clusterId"] == "derived-mech-c0-3d"][0]
        self.assertEqual(twin["state"]["pattern"]["graph"]["edges"][0]
                         ["target-slot"], "Input",
                         "the recomputed (fixed) twin must win over the stale shard one")
        self.assertEqual(len(merged), 2)  # twin + what-next

    def test_whatnext_matching_a_twin_signature_is_dropped(self):
        twin = self._twin("derived-mech-c0-3d")
        wn = self._wn("derived-wn-c1-0", twin_like=True)
        merged = D.merge_derived_outputs([{"entries": [wn]}], [twin],
                                         mined_sigs=set())
        self.assertEqual([e["clusterId"] for e in merged],
                         ["derived-mech-c0-3d"])

    def test_mined_signature_still_drops_both(self):
        twin = self._twin("derived-mech-c0-3d")
        mined_sig = D.structural_signature(twin["state"]["pattern"]["graph"])
        wn = self._wn("derived-wn-c1-0", twin_like=True)  # same structure
        merged = D.merge_derived_outputs([{"entries": [wn]}], [twin],
                                         mined_sigs={mined_sig})
        self.assertEqual(merged, [])


class GatePromptTests(unittest.TestCase):

    def test_prompt_carries_the_naming_rules_and_derived_framing(self):
        state = {"pattern": {
            "nodes": [{"id": "a", "name": "A", "description": "does A"}],
            "nodeSequence": "A", "wiring": "", "nodeCount": 1, "edgeCount": 0,
            "support": 0, "signal": "derived", "observedTitles": []}}
        p = D.build_derived_gate_prompt(state)
        low = p.lower()
        self.assertTrue("2d roi" in low or "2d/3d" in low)
        self.assertIn("biologist", low)
        self.assertIn("annotated path", low)  # the jargon ban
        self.assertIn("derived", low)
        self.assertIn('{"gate"', p)


if __name__ == "__main__":
    unittest.main(verbosity=2)
