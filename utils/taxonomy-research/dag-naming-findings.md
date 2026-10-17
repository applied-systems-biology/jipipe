# DAG Pattern Mining — Naming & Description Findings

Work item 1324 · branch `1324-node-taxonomy-core` · 2026-10-17
Status: **findings + proposed fix, awaiting review.** Not re-run yet.

You flagged four problems in the step (C) output. I measured each against the
270 shipped patterns and traced them to a common root cause. This report is
the evidence; the end is the fix I propose and the two decisions it needs
from you.

---

## Root cause (all four share it)

**The step (C) LLM never saw any node *descriptions*.** It only got each node
's short display name (e.g. "Add path to annotations", "Convert 2D ROI to
RGB") plus the slot wiring. The research `registry.json` I built for the
mining pipeline is *structure-only* (id, name, functions, slots) — it carries
**0** of the 1510 node descriptions, even though 300 of the 326 node types
the 270 patterns actually use **do have real descriptions in the source**.

So the model was forced to infer what each step does from its label and the
JIPipe slot vocabulary. That is exactly why:
- names describe JIPipe *mechanics* ("annotated paths") instead of the
  *outcome* a biologist gets,
- "ROI" is used unqualified (the model can't see that the 2D and 3D ROI nodes
  are different data types from the name alone), and
- descriptions are thin and sometimes don't say what a node actually does.

The fix is therefore two-part and cheap: **(1)** feed the real node
descriptions into the (C) prompt, and **(2)** tighten the naming rules. No
change to (A) or (B) — the candidates and clusters are fine; only the (C)
verdicts (name + description + gate) need regenerating.

Evidence that the descriptions exist and are extractable:

```
distinct node types used by the 270 shipped patterns : 326
types with an extractable source description         : 300 (92%)
  ij1-roi-to-rgb            Converts ROI lists to masks. The line and fill color is
                            stored within the ROI themselves. This algorithm needs a
                            reference image that provides the output sizes. …
  ij1-threshold-auto2d      Applies an auto-thresholding algorithm. If higher-dimensional
                            data is provided, the filter is applied to each 2D slice. …
  path-to-annotation-simple Creates an annotation for each path based on its name or its
                            full path.
  import-imagej-imgplus…    Loads an image using the best-matching method.
  ij1-analyze-find-particles2d
                            Converts mask images into ROI by applying a connected
                            components algorithm and generates measurements. …
```
(the 26 without a source description are mostly `clij:plugin:*`, `jipipe2ai-*`
and a few auto-registered nodes — a fallback to the node name is fine for those.)

---

## Issue 1 — Names use JIPipe-internal terms ("annotated paths", "hyperstack")

Measured: 8 of 270 names contain a JIPipe-internal or image-analysis-jargon term
a biologist wouldn't map to an outcome. The flagship is the worst (support 92,
so it's the most visible):

| cluster | support | current name | problem |
|---|---|---|---|
| 0001 | **92** | *List folder contents with **annotated paths*** | "annotated paths" is JIPipe's `path`-data-with-annotations mechanic, not what the biologist gets (a list of files/ROIs) |
| 0010 | 20 | *Cellpose segmentation to cleaned mask **hyperstack*** | "hyperstack" is a data-format term, not an outcome |
| 0015 | 14 | *Folder-based image import with **path annotations*** | same "path annotation" mechanic |
| 0023 | 10 | *Preprocess imported image **hyperstacks*** | "hyperstack" |
| 0025 | 10 | *Import images with source **path annotation*** | "path annotation" |
| 0101 | 4  | *Import images from folders with **path annotations*** | "path annotation" |
| 0193 | 2  | *ROI mask **hyperstack*** | "hyperstack" |
| 0233 | 1  | *Import **file paths** as annotation table* | "paths" (mild) |

The deeper pattern (your example): the name describes **how JIPipe moves the
data** (path → annotate → …) rather than **what the analyst achieves**.

**Proposed fix (prompt rules, applied on re-run):**
1. Name the *outcome*, not the mechanism. Ask "what does a user end up with?"
   — "Load images and ROIs from a folder", not "List folder contents with
   annotated paths".
2. Forbid / replace a short list of JIPipe-internal terms in names: `annotated
   path(s)`, `path annotation`, `hyperstack`, `slot`, `graph`, `compartment`,
   `wrapper`, `raw` (unless it means "uncropped", which it usually doesn't
   here). Provide the plain-language equivalent (`files/folders`, `multichannel
   stack`, …).
3. The description already has room to mention the mechanism; the *name* must
   not.

---

## Issue 2 — 2D and 3D ROIs are conflated

Measured across all 71 ROI-named patterns (node-type is the ground truth:
`ij1-roi*` = 2D ROI, `ij3d-roi3d*` = 3D ROI — different data types):

| category | count |
|---|---|
| properly qualified "2D ROI"/"3D ROI" in the name | 14 |
| bare "ROI", graph is **one** type, name doesn't say which (43 are 2D, 10 are 3D) | **56** |
| bare "ROI", graph **genuinely mixes** 2D + 3D ROI nodes | **1** |
| bare "ROI", no ROI-typed node identifiable by id | 3 |

The 56 single-type-but-unqualified ones are exactly your "the other type is
usually derivable" case — the name silently implies the pattern works for both
dimensions, which misleads a biologist into dropping it onto the wrong data.
Worst offenders (high support, most trusted):

```
0003  support 34  actually 2D  'Filter ROIs by overlap'
0004  support 31  actually 2D  'Cellpose segmentation to filtered RGB ROIs'
0017  support 13  actually 2D  'Image and ROI preprocessing'
0005  support 31  actually 3D  'Annote data with ROI statistics'
0008  support 22  actually 3D  'Color ROIs by statistics and render as RGB'
0027  support 8   actually 3D  'Segment objects and extract ROI statistics'
```

The **one genuine mix** is `0104 support 4 'Cellpose segmentation to ROIs'` —
its graph runs `ij1-roi-to-mask` / `ij1-roi-to-rgb` (2D) **and**
`ij3d-labels-to-roi` (3D) in the same pipeline. That's either a legitimate
2D→3D conversion (the name must say so) or a mining artifact that merged two
dimensionalities — needs a manual look.

**Proposed fix (prompt rule + a data-side note):**
- Rule: whenever the pattern touches ROI nodes, the name **must** say `2D ROI`
  or `3D ROI` (or `2D/3D` only if it truly operates on both, with the
  description explaining the conversion). The LLM is given each node's
  type/dimension (2D/3D is in the node id and will be in the description).
- The single mixed graph (`0104`) is called out in the report's flags section
  for you to decide: rename to state the 2D→3D step, or drop it.
- Optional, your point that the other dimension is "derivable": I would *not*
  auto-generate a mirror 3D pattern (that's a content decision with real
  downstream cost), but I can tag each single-type ROI pattern so a future
  "available in 2D / 3D" affordance is possible. Noting for you, not doing it.

---

## Issue 3 — Descriptions must say what the node actually does

Measured: for the 12 annotation-function patterns, **9 of 12 never state what
is being annotated** (the concrete input → concrete output). Example:

```
0030 'Annotate data and produce cleaned annotation table'
     "Annotates data with values from a table, converts the resulting
      annotations into a table, and tidies the table by removing, merging, …"
     → which values? from where? onto what? (a biologist can't tell)
0032 'Set multiple annotations'
     "Applies a sequence of expression-based annotation setters to data and
      merges the resulting annotations…"
     → sets *what*? "expression-based" is an implementation detail.
```

This is the same root cause: the model had the node *names* but not the node
*descriptions*, so it paraphrased the names instead of stating function.

**Proposed fix (the main one):**
1. Extend the structure-only `registry.json` to carry each node's source
   `description` (a one-off change to the `RegistryDumperTest`-style dump, or
   a direct extract from the `@SetJIPipeDocumentation` annotations — 92%
   coverage shown above). This is genuinely useful beyond mining: it makes the
   registry searchable by what a node does, which feeds node search and the
   user.
2. Re-run (C) with the node descriptions in the state, and add a prompt rule:
   *"For the description, state concretely what the pattern takes in and what
   it produces, using the real node functions (not 'annotates data' but
   'attaches each ROI's area and intensity to the image')."*
3. **Multi-section documentation API (your note):** the current viewer
   (`JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation`) flattens
   name + one description blob + slot table + parameters into one concatenated
   markdown string. A *structured* doc type (named sections — e.g. "What it
   does", "Inputs", "Outputs", "Parameters", "Examples/See-also") is the right
   API so the viewer can render sections independently. This is a separate,
   larger workstream than the mining step; the descriptions we generate here
   are the content that will eventually populate those sections. I'd treat it
   as its own follow-up task (design the `SectionedDocumentation` type +
   viewer) rather than block the mining on it — but I'll note it and can
   draft the design if you want.

---

## Issue 4 — Two audiences: bioinformaticians and biologists

This is a rule, not a bug, but it's the lens the other three are seen through.
A name/description that only an image-analysis programmer understands
("annotated paths", "expression-based annotation setters") fails half the
audience; one that over-simplifies fails the other.

**Proposed fix (prompt rule):**
- *Name*: plain outcome, short, usable by a biologist *and* precise enough for
  a bioinformatician. Prefer the biological object ("cells", "objects",
  "particles") over the data type where it's the same thing to a biologist.
- *Description*: lead with the outcome in plain terms; add the technical
  specifics (data types, node functions) as a second sentence. Two audiences,
  one description, ordered general → specific.
- Worked example (re-run target), flagship 0001:
  - *before*: "List folder contents with annotated paths" /
    "Enumerates subfolders and files under an input folder, annotates each
    path with file/directory names, and outputs filtered annotated paths…"
  - *after* (illustrative of the rule, not the final string): "Load images and
    ROIs from a folder" / "Recursively lists the images and ROI files in a
    folder and turns each into an image or ROI, ready for analysis. (JIPipe:
    folder list → path-to-annotation → importers.)"

---

## What this means for the pipeline

| step | status |
|---|---|
| (A) harvest | unchanged — candidates and their source layouts are correct |
| (B) merge | unchanged — the 324 families are correct (they're structural) |
| (C) gate/name/classify | **re-run** with (i) node descriptions in the state and (ii) the four prompt rules. Gate decisions will shift a little (the model now has real function evidence), names/descriptions will change a lot. |
| (D) ship | **blocked** on the (C) re-run + your decisions below |

The (C) re-run is ~324 LLM calls again (~30–60 min; the descriptions make the
prompts longer but well within context). It regenerates `dag-judgments.json`
and I update this review report from it. Nothing is committed until you're
happy with the new names.

---

## Decisions I need from you

1. **Support floor** (carried over from the last report): which set does (D)
   ship? — all 270 (plan literal) / ≥2 (208) / ≥3 (133) / ≥5 (65)? The
   re-run will change these counts slightly.
2. **Node descriptions in the registry**: OK to extend the registry dump to
   carry descriptions (the 92%-coverage fix)? This is the main enabler for
   issues 1/3/4 and also improves node search.
3. **Multi-section documentation API**: treat as a **separate follow-up task**
   (design now, implement after mining ships), or do you want me to design it
   now and fold it in before (D)?
4. **The one mixed 2D/3D graph** (`0104 'Cellpose segmentation to ROIs'`):
   rename to state the 2D→3D conversion, or drop it from the set?

Once you answer 1–4 I'll do the (C) re-run, regenerate the report, and you
review the new names/descriptions before (D).
