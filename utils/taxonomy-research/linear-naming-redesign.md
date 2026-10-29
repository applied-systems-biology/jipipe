# Linear Pattern Naming — Redesign (policy: name = what it does, no chain/arrows)

Work item 1324 · branch `1324-node-taxonomy-core` · 2026-10-19
Status: **decision report, awaiting ruling.** Nothing renamed yet.

## The policy (ruling, 2026-19)

> Template **names** must describe *what the workflow does* (the outcome).
> No `->`, no `→`, no `⇢`, **no chain separator of any kind** in the name.
> The mechanism (the node sequence) belongs in the **description**, not the name.

This is exactly the standard the **422 DAG names already meet** — they were
LLM-named on the outcome rules in `dag-naming-findings.md`:

- name the *outcome*, not the mechanism;
- no JIPipe-internal jargon ("annotated paths", "hyperstack", "slot");
- qualify `2D ROI` / `3D ROI`;
- two audiences (biologist + bioinformatician).

Examples of the target style (existing DAG names):
`Filter 3D ROIs by overlap`, `Load and denoise images from folders`,
`Segment cells into 2D ROIs`, `Detect 2D particles and render 2D ROIs`.

The **64 linear patterns** are the old round: their names are verbatim node
chains (`List files -> Annotate with file/directory name -> Import image`).
The fix is to give them outcome names in the DAG style. The **description**
already carries the mechanism (`Sequence: … → …` / `Wiring: …`) and is left as-is
— the `→` in the description is correct (it's the mechanism, where it belongs).

## Why a plain rename is not enough — the redundancy finding

The 64 linear patterns are not 64 distinct workflows. Measured against the
422 shipped DAG patterns (node-set + edge-set containment on the shipped graph
files):

| bucket                                                       | count  | meaning                                                                                                                     |
|--------------------------------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------|
| **true sub-path of a shipped DAG**                           | **32** | the linear chain's nodes AND edges are a strict subgraph of an already-shipped DAG pattern that already has an outcome name |
| **contains plumbing nodes** (`jipipe:comment`, `node-group`) | **8**  | workflow-specific organization, no general information (same class the original curation excluded for `compartment-output`) |
| **unique (neither)**                                         | **24** | genuinely linear-only workflows that need their own outcome name                                                            |
| (plumbing ∩ sub-path)                                        | 0      | the buckets are disjoint                                                                                                    |

### Consequence 1: renaming all 64 creates many duplicate names

Several chains describe the same outcome, so outcome-naming them produces
identical / near-identical names:

```
"List and filter files from a folder"        × 4
"Recursively list files from subfolders"     × 3
"Load images from a filtered folder"         × 3
"Import Bio-Formats and annotate with image properties" × 2
"Import 2D ROIs and clean the resulting masks"           × 2
```

The `noDuplicateNamesWithSameGraph` test *tolerates* same-name/different-graph,
so it would not fail — but shipping 4 templates all named "List and filter
files from a folder" is exactly the noise the policy is meant to remove.

### Consequence 2: the 32 sub-paths are already covered by a DAG

Each of the 32 is a literal fragment of a shipped DAG pattern. A biologist who
already has `Load and denoise images from folders` (the DAG) does not also need
`List subfolders -> Annotate with file/directory name -> List files` (the
3-node fragment of it). The DAG is the superset workflow and carries the good
name. Keeping both is redundant.

### Consequence 3: dropping sub-paths touches the flagship test

The flagship pattern pinned by `SystemPatternTemplatesTest` —
`chain-3-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file`
(`List files -> Annotate with file/directory name -> Import image`) — **is one of
the 32 sub-paths** (it is a fragment of the DAG
`Prepare cropped images and cleaned 2D ROI masks from folders`). Dropping it
would fail the `flagshipPresent` assertion (which requires a template with *exactly* those 3 nodes). The
`flagshipMinesNextSteps` relation would still hold (the superset DAG carries the same edge), but the exact-3-node pin
would need to
be re-pointed at a surviving pattern. Any option that drops sub-paths therefore
includes a small test edit.

## Scope options

| option | action                                             | linear kept | total | notes                                                                                                   |
|--------|----------------------------------------------------|-------------|-------|---------------------------------------------------------------------------------------------------------|
| **A**  | rename all 64, keep all                            | 64          | 486   | satisfies the naming policy directly; leaves duplicate names + 32 fragments that duplicate shipped DAGs |
| **B**  | rename all, **drop the 32 sub-paths**              | 32          | 454   | removes the redundant fragments; requires re-pointing the flagship test                                 |
| **C**  | rename all, **drop the 32 sub-paths + 8 plumbing** | 24          | 446   | most aggressive dedup; cleanest set; requires re-pointing the flagship test                             |

All options keep the **24 unique** patterns with fresh outcome names.
Option C's surviving 24 are the ones with no DAG equivalent and no plumbing —
the set that most clearly "is about what it does."

## Recommendation

**Option C** (keep 24, rename to outcome names). Rationale:

1. It fully satisfies the naming policy.
2. It removes the 32 fragments that are literally contained in already-named
   DAG patterns (redundancy) and the 8 plumbing patterns (noise) — which is what
   "names about what they do, not just chains" is really asking for.
3. The surviving 24 have distinct outcomes, so no duplicate names.
4. The cost is small and contained: a name map for 24 + a flagship re-point in
   one test + regenerating the sidecar (the DAG generator keeps the 64-linear
   subset count, so its `EXPECTED_LINEAR` pin moves 64 → 24).

If you prefer to change *only* names and keep the set intact for this pass (option A), that is also clean and
lower-risk; the dedup can follow as its own
curation step.

## Draft outcome names (all 64, for style review regardless of scope)

Format: `current chain name` → `proposed outcome name`  [bucket]

### File/folder loading (the `folder-list*` / `path-to-annotation-simple` family)

- `List files -> Annotate with file/directory name -> Import image` → **Load images from a folder** [sub-path ⚑FLAGSHIP]
- `List files -> Annotate with file/directory name -> Import ROI` → **Load 2D ROIs from a folder** [unique]
- `List files -> Annotate with file/directory name -> Filter paths` → **List and filter files from a folder** [sub-path]
- `List subfolders -> Annotate with file/directory name -> List files` → **Recursively list files from
  subfolders** [sub-path]
- `List subfolders -> Annotate with file/directory name -> Filter paths` → **Recursively list and filter files from
  subfolders** [sub-path]
- `Filter paths -> List files -> Annotate with file/directory name` → **List and filter files from a folder** [unique]
- `Annotate with file/directory name -> List files -> Annotate with file/directory name` → **List files and annotate
  their paths** [sub-path]
- `Annotate with file/directory name -> List subfolders -> Annotate with file/directory name` → **List subfolders and
  annotate their paths** [sub-path]
- `Annotate with file/directory name -> Filter paths -> List files` → **List and filter files from a folder** [sub-path]
- `Annotate with file/directory name -> Filter paths -> Import image` → **Load images from a filtered file
  list** [unique]
- `Folder list -> List subfolders -> Annotate with file/directory name` → **List subfolders from input
  folders** [sub-path]
- `File list -> Annotate with file/directory name -> Import Cellpose model from file` → **Import a Cellpose model from a
  file list** [unique]
- `List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask` → **Load 2D ROIs from a
  folder and convert them to masks** [unique]
- `List files -> Annotate with file/directory name -> Filter paths -> Import image` → **Load images from a filtered
  folder** [sub-path]
- `List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name` →
  **Recursively list files from subfolders** [sub-path]
- `List subfolders -> Annotate with file/directory name -> Filter paths -> List files` → **Recursively list and filter
  files from subfolders** [sub-path]
- `Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths` → **Recursively list and filter
  subfolders** [sub-path]
- `Filter paths -> List files -> Annotate with file/directory name -> Import image` → **Load images from a filtered file
  list** [sub-path]
- `Filter paths -> List files -> Annotate with file/directory name -> Import ROI` → **Load 2D ROIs from a filtered file
  list** [unique]
- `Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths` → **List,
  annotate, and filter files from a folder** [sub-path]
- `Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files` →
  **Recursively list files from subfolders** [sub-path]
- `Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name` → **List and
  filter files from a folder** [sub-path]
-
`List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths` →
**Recursively list, annotate, and filter files** [sub-path]
-
`List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name` →
**Recursively list and filter files from subfolders** [sub-path]
- `Folder list -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files` → **Recursively
  list and filter files from subfolders** [unique]
- `Filter paths -> List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask` → **Load 2D
  ROIs from a filtered folder and convert to masks** [unique]
-
`Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Import image` →
**Load images from a filtered folder** [unique]
-
`Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name` →
**Recursively list files and annotate their paths** [sub-path]
-
`Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import image` →
**Load images from a filtered folder** [sub-path]
- `Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import ROI` →
  **Load 2D ROIs from a filtered folder** [unique]

### Cell segmentation

- `Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB` → **Segment cells and render
  filtered 2D ROIs** [unique]
- `Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics` → **Segment
  with Cellpose 3.x and filter 2D ROIs** [sub-path]
- `Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to mask` → **Segment with
  Cellpose 3.x and create 2D ROI masks** [sub-path]
- `Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Convert 2D ROI to RGB` → **Segment with
  Cellpose 3.x and render 2D ROIs** [sub-path]

### Particle / mask processing

- `Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB` → **Detect 2D particles by thresholding and
  render them** [sub-path]
- `Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D` → **Threshold and clean
  binary masks** [sub-path]
- `Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D` → **Clean masks and detect 2D
  particles** [unique]
- `Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB` → **Detect 2D particles and render them in
  RGB** [unique]
- `Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)` → **Morphologically clean and
  randomly split data** [unique]
- `Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data` → **Convert 2D ROIs to masks, clean them,
  and measure** [unique]
- `Morphological hole filling 2D -> Morphological operation 2D -> Find particles 2D -> Convert 2D ROI to RGB` → **Clean
  masks, detect 2D particles, and render them** [sub-path]
- `Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)` →
  **Convert 2D ROIs to masks, clean, measure, and split** [unique]

### ROI import → mask

- `Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D` → **Import 2D ROIs and clean the resulting
  masks** [sub-path]
- `Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask` → **Import 2D ROIs and convert them to
  masks** [unique]
- `Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data` → **Import 2D ROIs, clean
  the masks, and measure** [unique]
- `Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D` → **Import
  2D ROIs and clean the resulting masks** [unique]
-
`List files -> Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D` →
**Load 2D ROIs from a folder and clean the masks** [unique]
-
`Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data -> Split data randomly (percentage)` →
**Import 2D ROIs, clean, measure, and split the data** [unique]
-
`Annotate with file/directory name -> Import ROI -> Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data` →
**Import 2D ROIs, clean the masks, and measure** [unique]

### 3D ROI visualization

- `Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Copy physical dimensions` → **Color 3D ROIs and copy physical
  dimensions** [sub-path]
- `Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images` → **Color 3D ROIs and blend onto the
  image** [sub-path]
- `Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions` → **Render 3D ROIs in color and copy
  dimensions** [sub-path]
- `Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB` → **Filter overlapping 3D
  ROIs and render them** [sub-path]
- `Set IJ3D ROI properties -> Convert IJ3D ROI to RGB -> Blend images -> Copy physical dimensions` → **Color and blend
  3D ROIs onto the image** [sub-path]

### Bio-Formats / microscopy import

- `Bio-Formats importer -> Annotate with image properties -> Split by dimension` → **Import Bio-Formats and split by
  dimension with image properties** [sub-path]
- `Import image -> Annotate with data -> Split data randomly (percentage)` → **Import images and split data
  randomly** [unique]
- `Group -> Bio-Formats importer -> Annotate with image properties` → **Import Bio-Formats and annotate with image
  properties** [plumbing]
- `Project user path -> Group -> Bio-Formats importer` → **Import Bio-Formats from the project directory** [plumbing]
- `Project user path -> Group -> Bio-Formats importer -> Annotate with image properties` → **Import Bio-Formats from the
  project and annotate** [plumbing]
- `Group -> Bio-Formats importer -> Annotate with image properties` (comment) — see below
- `Comment -> Group -> Bio-Formats importer` → **Import Bio-Formats images** [plumbing]
- `Group -> Bio-Formats importer -> Annotate with image properties` (group) → **Import Bio-Formats and annotate with
  image properties** [plumbing]
- `Comment -> Group -> Bio-Formats importer -> Annotate with image properties` → **Import Bio-Formats and annotate with
  image properties** [plumbing]

### Misc plumbing (comment/group) — no general information

- `Comment -> Annotate with file/directory name -> List subfolders` → **List subfolders from a folder** [plumbing]
- `Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name` → **List
  subfolders and annotate their paths** [plumbing]
- `Comment -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> List files` →
  **Recursively list files from subfolders** [plumbing]

> Note: the plumbing bucket (8) and several "unique" folder-listing chains
> produce names that collide across buckets — another reason option C
> (drop sub-path + plumbing) yields the cleanest, duplicate-free set.
