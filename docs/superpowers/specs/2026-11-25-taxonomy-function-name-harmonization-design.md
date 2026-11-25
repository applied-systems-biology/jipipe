# Taxonomy function names: harmonization design

Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1406
Date: 2026-11-25
Status: approved approach "A: bare leaf names"; name map below is the normative list.

## Intent and why

The node taxonomy's sub-function display names mix at least six naming styles for
the same concepts. The work item cites "Filament filtering" vs "ROI filters" vs
"Tracking: filtering" as three different ways to describe one idea. The names
should be harmonized so the same concept is spelled the same way in every
branch.

## Goals

- One consistent convention for all sub-function display names.
- The same concept (filtering, measurements, ...) uses the same word in every
  branch.
- No display name repeats its own parent branch's word.

## Non-goals

- No changes to registry IDs (`jipipe:function:*` stay exactly as they are).
- No moves of functions between branches, no new or removed functions.
- No changes to descriptions, tags, hues, icons, or visibility flags.
- No UI code changes.

## Anti-goals

- Renaming IDs (would break every saved project that persists function IDs).
- Colon-style names ("Tracking: filtering"), which is the style the work item
  complains about.

## Constraints

- Display names are not persisted in projects; only IDs are. Renaming display
  names is migration-free.
- The node search scores the full ancestor path (leaf at full weight,
  ancestors at decreasing weight), so a bare leaf name still matches queries
  that name the parent (e.g. "roi filter" finds ROIs > Filtering).
- No test pins the current display names; only the source files themselves
  contain them (`utils/taxonomy-research/registry.json` is a research
  snapshot, not a gate).

## Approaches considered

- **A: bare leaf names (picked).** Every sub-function names only the concept;
  the tree supplies the domain. Grammar rule: nominal for groupings
  ("Filtering", "Morphology"), verb-first for actions on the branch's object
  ("Filter & modify", "Export to file"). A leaf keeps a domain word only when
  dropping it would collide with a top-level branch or lose meaning ("Filament
  data" under Data Sources; "Bar plot" under Plotting, where "Bar" alone does
  not read). Cleanest tree, no redundancy, search unaffected. Weakness: a name
  read outside the tree (template-scope tooltip) has no parent context.
- **B: self-contained qualified names.** "Domain + role" everywhere
  ("ROI filtering", "Track detection"). Every name works anywhere, but the
  tree becomes redundant ("Tracking > Track detection") and multi-word
  operations get clumsy ("Table join & split"). Rejected: the taxonomy is
  consumed through the tree, where the qualification is noise.
- **C: A plus breadcrumbs in flat contexts.** Best result but adds UI changes
  to tooltip and compendium code. Rejected for this task: out of scope for a
  name sweep; can follow later if the flat contexts prove confusing.

## The naming rule

1. A sub-function name never repeats its parent branch's word.
2. Groupings are nominal and parallel within the branch ("Detection, Linking,
   Filtering, Analysis").
3. Actions on the branch's object are verb-first, object dropped
   ("Filter & modify", "Join & split", "Export to file").
4. A domain word is kept when dropping it would collide with a top-level
   branch name or lose meaning ("Filament data", "Bar plot", "Color stack").

## Name map (normative)

Top-level names and their spelling stay unchanged. Only sub-functions listed
below change.

| Path | Old name | New name |
|------|----------|----------|
| Annotations | Convert annotations | Convert |
| Annotations | Edit annotations | Edit |
| Annotations | Graph annotations | Canvas |
| Export | ROI | ROIs |
| Filaments | Filament filtering | Filtering |
| Filaments | Filament quantification | Quantification |
| Filaments | Filament smoothing | Smoothing |
| Graph / Project | Graph debugging | Debugging |
| Graph / Project | Graph wrappers | Wrappers |
| Images | Image arithmetic | Arithmetic |
| Images | Image comparison | Comparison |
| Images | Image labeling | Labeling |
| Images | Image moments | Moments |
| Images | Image neighbor measurements | Neighbor measurements |
| Images | Image pixel manipulation | Pixel manipulation |
| Images | Image projections | Projections |
| Images | Image properties | Properties |
| Images | Image statistics | Statistics |
| Images | Image transformations | Transformations |
| Images | Image type conversion | Type conversion |
| Images / Segmentation | Cell-based segmentation | Cell-based |
| Machine learning | Machine learning inference | Inference |
| Machine learning | Machine learning models | Models |
| Machine learning | Machine learning training | Training |
| Miscellaneous | Miscellaneous unassigned | Unassigned |
| ROIs | ROI filters | Filtering |
| ROIs | ROI intersections | Intersections |
| ROIs | ROI measurements | Measurements |
| ROIs | ROI morphology | Morphology |
| ROIs | ROI properties | Properties |
| Statistics | Descriptive statistics | Descriptive |
| Statistics | Statistical testing | Testing |
| Tables | Aggregate tables | Aggregate |
| Tables | Export tables to file | Export to file |
| Tables | Filter & modify tables | Filter & modify |
| Tables | Join & split tables | Join & split |
| Tables | Transform tables | Transform |
| Tracking | Tracking: analysis | Analysis |
| Tracking | Tracking: detection | Detection |
| Tracking | Tracking: filtering | Filtering |
| Tracking | Tracking: linking | Linking |

Deliberately unchanged sub-functions (bare already, or domain word carries the
meaning): Data Sources children ("Filament data", "Image data", "ROI data",
"Table data", "Scene 3D data", "Misc. data", "File system", "Forms", "OMERO"),
Miscellaneous children ("Data utilities", "Debugging", "Templates",
"Text & markup"), Export children ("Data", "Images", "Tables", "3D scenes"),
Images ("Color stack", "Filtering", "Measurements", "Merging & splitting",
"Preprocessing", "Resampling", "Segmentation", "Thresholding"), Graph / Project
("Compartment outputs"), Plotting children ("Bar plot", "Heatmap",
"Line plot", "Other plots", "Scatter plot"), Scripting children ("Expressions",
"ImageJ macro", "Python", "R"), Statistics ("Estimation").

## What is left to the builder

- Apply the map to the `getName()` string literals of the listed function
  classes (display names only; IDs, descriptions, and everything else stay).
- Grep for the old names as string literals elsewhere (docs, research notes)
  and update decision records where they document the taxonomy (e.g.
  `utils/taxonomy-conversion/conversion-map.md` uses old names in prose; update
  the affected rows).
- Run the taxonomy validation gates (`TaxonomyFullValidationTest`) and the
  node browser/database taxonomy tests.
