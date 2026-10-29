#!/usr/bin/env python3
"""Tests for the node-description extractor (utils/extract-node-descriptions.py).

The extractor builds a node-info-id -> description map by reading
``@SetJIPipeDocumentation(name=…, description=…)`` off each algorithm class,
linking classes to ids via the plugin ``registerNodeType("<id>", <Class>.class,
…)`` calls. Descriptions are Java string-literal concatenations that span
lines, so the parser must join them. It is a research/offline tool (no Maven),
mirroring the other ``utils/*.py`` mining scripts and their selftest pattern.

Run standalone:

    python3 utils/extract_node_descriptions_tests.py
"""

import importlib.util
import os
import textwrap
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "extract-node-descriptions.py")


def load_extractor():
    spec = importlib.util.spec_from_file_location("extract_node_descriptions", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


EX = load_extractor()


class ExtractUnitTests(unittest.TestCase):
    """Unit tests on the parsing helpers (no filesystem)."""

    def test_description_literal_is_extracted(self):
        txt = ('@SetJIPipeDocumentation(name = "Convert 2D ROI to RGB", '
               'description = "Converts ROI lists to masks. The line and fill '
               'color is stored within the ROI themselves.")')
        self.assertEqual(EX.extract_description(txt),
                         "Converts ROI lists to masks. The line and fill color "
                         "is stored within the ROI themselves.")

    def test_multi_line_concatenation_is_joined(self):
        txt = ('@SetJIPipeDocumentation(name = "X", description = "First part. " +\n'
               '        "Second part. " +\n'
               '        "Third part.")')
        self.assertEqual(EX.extract_description(txt),
                         "First part. Second part. Third part.")

    def test_escaped_quote_in_description(self):
        txt = ('@SetJIPipeDocumentation(name = "X", description = "Says \\"hi\\" and more.")')
        self.assertEqual(EX.extract_description(txt), 'Says "hi" and more.')

    def test_no_description_returns_none(self):
        self.assertIsNone(EX.extract_description('@SetJIPipeDocumentation(name = "X")'))
        self.assertIsNone(EX.extract_description("class Foo { }"))

    def test_only_first_annotation_block_considered(self):
        # a file may have several annotations; the class-level one is first
        txt = ('@SetJIPipeDocumentation(name = "Real", description = "The real one.")\n'
               'class Foo {\n'
               '  @SetJIPipeDocumentation(name = "param", description = "a param.")\n'
               '}')
        self.assertEqual(EX.extract_description(txt), "The real one.")

    def test_id_to_class_map_from_registration(self):
        txt = ('registerNodeType("ij1-roi-to-rgb", RoiToRGBAlgorithm.class, icon);\n'
               'registerNodeType("import-imagej-imgplus-from-file", '
               'ImportImageJImgPlusFromFileAlgorithm.class, null);')
        m = EX.extract_id_class_map(txt)
        self.assertEqual(m["ij1-roi-to-rgb"], "RoiToRGBAlgorithm")
        self.assertEqual(m["import-imagej-imgplus-from-file"],
                         "ImportImageJImgPlusFromFileAlgorithm")

    def test_registration_with_new_task_object_ignored(self):
        # registerNodeType(new JIPipeJavaNodeRegistrationTask("id", Class.class, …))
        txt = ('registerNodeType(new JIPipeJavaNodeRegistrationTask('
               '"my-id", MyNode.class, this, icon));')
        m = EX.extract_id_class_map(txt)
        # the simple form is not present; the new-task form is captured too
        self.assertIn("my-id", m)
        self.assertEqual(m["my-id"], "MyNode")

    def test_annotation_node_type_registration(self):
        # canvas-note nodes register via registerAnnotationNodeType - they
        # carry @SetJIPipeDocumentation too and must be linked the same way
        txt = ('registerAnnotationNodeType("graph-annotation-image", '
               'ImageBoxGraphCanvasNote.class, ImageBoxNoteGraphNodeTool.class, icon);')
        m = EX.extract_id_class_map(txt)
        self.assertEqual(m["graph-annotation-image"], "ImageBoxGraphCanvasNote")


class ExtractIntegrationTests(unittest.TestCase):
    """End-to-end against the real source tree (skipped if the repo sources
    are not present, so the suite stays hermetic elsewhere)."""

    REPO = os.path.dirname(HERE)  # utils/.. = repo root

    def test_repo_sources_present(self):
        self.assertTrue(os.path.isdir(os.path.join(self.REPO, "jipipe-core")),
                        "repo sources not found; skipping would be wrong here")

    def test_known_descriptions_are_present(self):
        m = EX.extract_all(self.REPO)
        # the flagship pattern's node types must have real descriptions
        for nid in ("ij1-roi-to-rgb", "ij1-threshold-auto2d",
                    "path-to-annotation-simple", "import-imagej-imgplus-from-file"):
            self.assertIn(nid, m, f"{nid} has no extracted description")
            self.assertGreater(len(m[nid]), 10, f"{nid} description too short")

    def test_coverage_is_high(self):
        m = EX.extract_all(self.REPO)
        self.assertGreater(len(m), 300, f"only {len(m)} descriptions extracted")


if __name__ == "__main__":
    unittest.main(verbosity=2)
