#!/usr/bin/env python3
"""Tests for the node-description audit tool (utils/audit-node-descriptions.py).

The audit (the (C) review's decision 3) drafts improved descriptions for the
pattern-relevant nodes whose documentation is wrong/missing/thin and applies
them to the ``@SetJIPipeDocumentation`` annotations in the Java source. The
risky part is the source edit itself: descriptions are Java string-literal
concatenations that span lines, may be empty (``description = ""``), or absent
entirely. These tests pin the apply behavior; the round-trip through the
extractor (utils/extract-node-descriptions.py) is the acceptance criterion —
whatever we write must be re-readable.

Run standalone:

    python3 utils/audit_node_descriptions_tests.py
"""

import importlib.util
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, file))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


AUDIT = load("audit_node_descriptions", "audit-node-descriptions.py")
EX = load("extract_node_descriptions", "extract-node-descriptions.py")


class ApplyDescriptionTests(unittest.TestCase):
    """apply_description(text, new) -> patched Java source."""

    def test_replaces_single_line_literal(self):
        src = ('@SetJIPipeDocumentation(name = "X", description = "Old text.")\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, "New text.")
        self.assertIn('description = "New text."', out)
        self.assertNotIn("Old text", out)
        self.assertIn('name = "X"', out, "the name attribute must be untouched")
        self.assertEqual(EX.extract_description(out), "New text.",
                         "the patch must round-trip through the extractor")

    def test_replaces_multi_line_concatenation(self):
        src = ('@SetJIPipeDocumentation(name = "X", description = "First part. " +\n'
               '        "Second part.")\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, "One clean line.")
        self.assertEqual(EX.extract_description(out), "One clean line.")
        self.assertNotIn("First part", out)

    def test_fills_empty_description(self):
        src = ('@SetJIPipeDocumentation(name = "Tunnel entrance", description = "")\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, "Lets data pass across the graph.")
        self.assertEqual(EX.extract_description(out), "Lets data pass across the graph.")

    def test_inserts_missing_description_attribute(self):
        src = ('@SetJIPipeDocumentation(name = "X")\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, "Does the thing.")
        self.assertEqual(EX.extract_description(out), "Does the thing.")
        self.assertIn('name = "X"', out)

    def test_escapes_quotes_and_backslashes(self):
        new = 'Converts "ROIs" to C:\\data\\masks.'
        src = ('@SetJIPipeDocumentation(name = "X", description = "Old.")\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, new)
        self.assertEqual(EX.extract_description(out), new,
                         "escape round-trip must survive the extractor")
        # no unescaped quote may open the literal early
        self.assertNotIn('"ROIs"', out)

    def test_newlines_are_collapsed_to_spaces(self):
        out = AUDIT.apply_description(
            '@SetJIPipeDocumentation(name = "X", description = "Old.")\npublic class Foo { }',
            "Line one.\nLine two.\tTabbed.")
        self.assertEqual(EX.extract_description(out), "Line one. Line two. Tabbed.")
        # the patched annotation must stay on one line
        self.assertTrue(out.split("\n")[0].startswith('@SetJIPipeDocumentation'),
                        "a raw newline would be invalid Java inside a string literal")

    def test_is_idempotent(self):
        src = ('@SetJIPipeDocumentation(name = "X", description = "Old.")\n'
               'public class Foo { }')
        once = AUDIT.apply_description(src, "New.")
        twice = AUDIT.apply_description(once, "New.")
        self.assertEqual(once, twice)

    def test_only_the_class_level_annotation_is_touched(self):
        src = ('@SetJIPipeDocumentation(name = "Real", description = "Class text.")\n'
               'public class Foo {\n'
               '  @SetJIPipeDocumentation(name = "param", description = "a param.")\n'
               '  Object bar;\n'
               '}')
        out = AUDIT.apply_description(src, "Patched class text.")
        self.assertEqual(EX.extract_description(out), "Patched class text.")
        self.assertIn('description = "a param."', out,
                      "the parameter-level annotation must be untouched")

    def test_trailing_attribute_after_description_is_kept(self):
        # description is not always the last attribute
        src = ('@SetJIPipeDocumentation(name = "X", description = "Old.",\n'
               '        descriptionResourceURL = null)\n'
               'public class Foo { }')
        out = AUDIT.apply_description(src, "New.")
        self.assertEqual(EX.extract_description(out), "New.")
        self.assertIn("descriptionResourceURL = null", out)

    def test_raises_when_no_annotation_present(self):
        with self.assertRaises(ValueError):
            AUDIT.apply_description("public class Foo { }\n", "New.")


class AuditDraftingTests(unittest.TestCase):
    """The draft prompt must carry the defect context (wrong vs thin vs
    missing) — a node whose description is known-wrong gets a different
    instruction than one that is merely thin."""

    def _slots(self, *data_types):
        # the registry.json shape: {class (FQ), dataType}
        return [{"class": f"org.hkijena.jipipe.{dt}", "dataType": dt}
                for dt in data_types]

    def test_prompt_states_the_wrong_defect(self):
        p = AUDIT.build_draft_prompt(
            name="Convert 2D ROI to RGB", node_id="ij1-roi-to-rgb",
            current="Converts ROI lists to masks.", bucket="wrong",
            wrong_note="describes ij1-roi-to-mask",
            inputs=self._slots("imagej-imgplus", "imagej-roi-2d"),
            outputs=self._slots("imagej-imgplus-color-rgb"),
            source="public class RoiToRGBAlgorithm { void execute() { /* ... */ } }")
        self.assertIn("WRONG", p)
        self.assertIn("ij1-roi-to-mask", p)
        self.assertIn("an ImageJ image", p)
        self.assertIn("public class RoiToRGBAlgorithm", p)
        self.assertIn('{"description"', p)

    def test_prompt_shows_data_types_not_java_classes(self):
        # the (C) review follow-up: feeding raw Java class names as anchors made
        # the model copy them into the prose ("an ImagePlusData image"). The
        # prompt must present data types by user-facing display name and must
        # ban internal class names outright (the ban rule itself may quote the
        # class names as examples of what NOT to write).
        p = AUDIT.build_draft_prompt(
            name="Remove LUT", node_id="ij1-remove-lut",
            current="Removes the LUT.", bucket="thin", wrong_note=None,
            inputs=[{"class": "org.hkijena.jipipe...ImagePlusData",
                     "dataType": "imagej-imgplus"}],
            outputs=[{"class": "org.hkijena.jipipe...ImagePlusData",
                      "dataType": "imagej-imgplus"}],
            source="class X {}")
        self.assertIn("an ImageJ image", p)
        # no class name may appear OUTSIDE the ban rule (the rule quotes them)
        body = p.split("NEVER mention internal Java class names")[0]
        self.assertNotIn("ImagePlusData", body)
        self.assertIn("class name", p.lower())  # the ban rule is stated

    def test_parse_draft_rejects_internal_class_names(self):
        # fail-closed: a draft that leaks an internal class name must not pass
        import json
        leak = json.dumps({"description":
            "Takes an ImagePlusData image and returns a ResultsTableData table."})
        self.assertIsNone(AUDIT.parse_draft(leak))
        clean = json.dumps({"description":
            "Takes an ImageJ image and returns a results table."})
        self.assertEqual(AUDIT.parse_draft(clean),
                         "Takes an ImageJ image and returns a results table.")

    def test_prompt_states_the_thin_defect(self):
        p = AUDIT.build_draft_prompt(
            name="Import file list", node_id="import-file-list",
            current="Converts each provided path into file data.", bucket="thin",
            wrong_note=None, inputs=self._slots("ObjectListData"),
            outputs=self._slots("FileData"), source="class X {}")
        self.assertIn("thin", p.lower())
        self.assertNotIn("WRONG", p)

    def test_prompt_states_the_missing_defect(self):
        p = AUDIT.build_draft_prompt(
            name="Tunnel entrance", node_id="data-flow-tunnel-1-entrance",
            current="", bucket="missing", wrong_note=None,
            inputs=self._slots("JIPipeData"), outputs=[],
            source="class X {}")
        self.assertIn("missing", p.lower())

    def test_parse_draft_round_trip(self):
        import json
        self.assertEqual(
            AUDIT.parse_draft('Sure! Here you go:\n' + json.dumps(
                {"description": "Takes ROIs and makes an RGB image."})),
            "Takes ROIs and makes an RGB image.")
        self.assertIsNone(AUDIT.parse_draft("no json at all"))

    def test_parse_draft_caps_length(self):
        import json
        self.assertIsNone(AUDIT.parse_draft(
            json.dumps({"description": "x" * 500}), max_chars=400))
        self.assertEqual(
            AUDIT.parse_draft(json.dumps({"description": "ok short"}), max_chars=400),
            "ok short")


if __name__ == "__main__":
    unittest.main(verbosity=2)
