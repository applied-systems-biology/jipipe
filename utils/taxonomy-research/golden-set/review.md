# Golden-set corpus review (#1408, MR C, task 1)

Status: authored, verified against a fresh registry dump and the untouched
scorer, ready to pin. The corpus lives at
`jipipe-launcher/src/test/resources/org/hkijena/jipipe/api/nodes/database/golden-set.json`
(93 cases). Pinning happens after the user ack; Task 2 builds the gate test.

## How the corpus was built

1. **Mining** (`utils/search-usage/build-golden-set-candidates.py`): node
   placements across the 59 example workflows at `/data/JIPipe/Example
   workflows/` (every `jipipe:node-info-id`, including `node-group` nested
   graphs), joined with the registry dump. Output:
   `utils/taxonomy-research/golden-set/candidates.json` (324 distinct node
   ids, 11254 placements, deterministic output).
2. **Registry dump**: `NodeRegistryDumpToolTest` with
   `-Djipipe.dump.path=/tmp/opencode/registry-dump.json` wrote all 1511
   registered node infos (id, name, plain-text description, framework). The
   description gate ran in the same invocation:
   `mvn -pl jipipe-launcher -am test -o -Dtest=SearchDescriptionQualityTest,NodeRegistryDumpToolTest
   -Djipipe.desc-sweep.dump=... -Djipipe.dump.path=...` - BUILD SUCCESS, the
   gate dumped **0 failing descriptions** (green after #1404), which is why
   the failing-only desc-sweep dump would have been an empty join source;
   the full-registry dump is the join input.
3. **Authoring**: every case's `expect`/`first` id was verified against the
   dump (no invented ids), then each candidate query was executed against
   the **untouched** `JIPipeEnhancedNodeDatabaseSearch` over the live
   registry (throwaway probe test, same call pattern the Task 2 gate uses;
   deleted after the run). Probe evidence is cited per case in the corpus.

## Placement counting note

The miner records two counts per node: `placements` (recursive, includes
`node-group` contents) and `topGraphPlacements` (direct children of the
top-level `graph.nodes`, the counting the #1404 usage prior uses). The plan's
expected numbers reproduce exactly as `topGraphPlacements`:
`ij1-roi-to-rgb` 298, `ij1-threshold-auto2d` 242, `ij1-morph-operation2d`
228. The recursive counts are slightly higher (303/246/233) because they
also count placements inside node groups; `evidence` strings in the corpus
cite the recursive counts. 9 mined ids are not in the current registry
(`inDump: false`, kept visible in candidates.json, never dropped): 5 clij
plugin ids the workflows reference but the registry no longer registers
(e.g. `ExtendedDepthOfFocusSobelProjection`, 91 placements),
`graph-annotation-group-box`, and 3 plotly ids.

## Corpus shape

93 cases. Per-tag counts (multi-tag cases count in every tag):

| tag | count | minimum |
|---|---|---|
| biologist-outcome | 21 | 15 |
| bioinfo-algorithm | 25 | 15 |
| imagej-vocab | 16 | 10 |
| typo | 12 | 10 |
| substring | 6 | 6 |
| ordering | 11 | 8 |
| dimensionality | 16 | 10 |

## Pre-fix status (measured, untouched scorer, 2026-11-25)

- **13 cases RED** - the fix backlog Task 2 will measure as
  `baseline-failures.json`:
  - 12 `typo` cases (`treshol`, `treshod`, `treshood`, `dspeckle`,
    `dseckle`, `prcentile`, `pcentile`, `ipaining`, `gusian`, `seltonize`,
    `wtrshed`, `atrshed`): all 0 results pre-fix. Each typo token is below
    the strong fuzzy tier against its target word: measured JW 0.80-0.85
    per token (`dseckle`/`despeckle` 0.805 is the lowest, `treshol`/
    `treshod`/`wtrshed` 0.848 the highest), all 12 inside the weak band
    [0.78, 0.85), none in the strong tier, so the weak tier (Task 4) is
    exactly the missing rung.
  - `dim-median-two-dimensional-05` (`median filter two dimensional`): 0
    results pre-fix; Task 6's `two dimensional` -> `2d` canon is the fix.
- **80 cases GREEN** - regression pins. They pass today and must keep
  passing through Tasks 3-7 (Review Focus 2). Every `first` assertion holds
  pre-fix.
- Boundary watch (green at rank 4-5 today; a template-set change could push
  them out): `bio-scale-bar-11` (4), `alg-watershed-split-04` (5),
  `alg-hough-11` (4), `ij-fill-holes-05` (5), `ij-variance-filter-08` (4).

## Covered-anyway cases (measured passers, kept out of the corpus)

These authoring candidates already pass the untouched scorer, so per the
task rule they are recorded here instead of the corpus:

| query | measured result | why it already works |
|---|---|---|
| `treshold` | `ij1-threshold-auto2d` rank 1 of 141 | JW(treshold, threshold) = 0.892, above the 0.85 strong tier |
| `thresold` | rank 1 of 150 | strong tier |
| `watersed` | watershed family ranks 1-3 | JW 0.985, strong tier |
| `cellpose` / `cellpos` | cellpose family rank 1 | exact token / substring rung |
| `tatistics`, `satistics` | `ij1-roi-statistics` rank 2 | substring rung (mid-word of "statistics") |
| `eorder` | `ij1-dimensions-reorder` rank 1 | substring rung (mid-word of "Reorder") |
| `vesselnes` | frangi vesselness rank 2 | substring rung |
| `xtended` | extended-depth-of-focus rank 5 | fuzzy tier |
| `anotation` | annotate family rank 1 | strong tier (JW 0.97) |
| `measurment` | measure family rank 1 | strong tier |
| `histogam` | `plot-table:plot-histogram` rank 1 | strong tier |
| `segmenation` | cellpose segmentation rank 1 | strong tier |
| `annotaion` | annotate family rank 1 | strong tier |
| `trackmat` | `trackmate-tracking` rank 1 | strong tier |

Consequence for Task 4: the brief's example typos (`treshold`, `cellpose`,
`watersed`) are already covered by the strong tier or the substring rung;
the corpus's 12 typo cases use verified weak-band variants instead. Task 4
Step 1's check ("all typo cases below the strong tier") holds for every
corpus typo case by construction.

## Corpus case probe cross-reference (sub-morphologic-06)

The `sub-morphologic-06` evidence string cites this file (`probe: see
review.md`): measured against the untouched scorer, query `morphologic`
returns `ij1-morph-operation2d` at rank 1 (1-based authoring-probe
numbering; rank 0 in the gate's 0-based numbering) of 25 results, i.e. the
case is GREEN pre-fix. The mechanism is the forward substring rung
(`morphological` contains `morphologic`; the query token is a word-start
prefix of the name word `Morphological`).

## Dropped case ideas (with reasons)

**Template wall.** System node templates whose names carry the query words
occupy the whole top 10 of several outcome queries (measured). No node id
can be asserted within 5 there, and Task 6's dimension bias cannot move
templates (they declare no spatial dimensions). Dropped as node cases,
recorded for wave 2 / UX:
`find the cells`, `segment the cells`, `segment cells 3d`, `segment cells
2d`, `denoise`, `find rois`, `detect rois`, `measure roi`, `roi area`,
`remove the background`, `overlay images`, `subtract images`, `intensity
statistics`, `overlay scale bar`, `denoise`. Two of the listed queries do
have an assertable node inside the wall (measured: `intensity statistics`
puts a node at rank 5 and `overlay scale bar` a node at rank 3); both were
conservatively dropped with the wall. The plan's constraint-query
example (`segment cells 3d`) is authored in node-name form instead
(`watershed 3d`, `connected components 3d`, `spots 3d`, `threshold 3d` -
all measured with the 3D nodes ranked over their 2D-only siblings), which
is the same dimension-constraint contract without the template wall.

**Vocabulary gaps** (0 results pre-fix is the group-level picture: the
queries do not surface their intended node; three measured per-entry
exceptions return a handful of irrelevant results instead of 0:
`analyze particles` 3, `erode` 9, `dilate` 8. No wave 1 mechanism can add
the missing words; embedding work / description scope of wave 2):
`color macrophages red`, `make a chart` (verb too rare; `bar chart` works
and is in the corpus), `make diagram` (no plot description says
"diagram"), `count the cells` / `count cells` / `cell count` (no counting
node mentions cells; `count rois` works), `rolling ball` (the node name
says "Find or subtract background"; `subtract background` works),
`random forest` (no node name carries it; ilastik/weka queries work),
`stack to rgb` (the node says "Combine channel slices to RGB"; `combine
rgb` works), `erode`, `dilate` (capability lives in a method parameter of
"Morphological operation"; only filament/label nodes carry the words),
`analyze particles`, `set scale`, `make binary` (`binarize the image`
works), `adaptive threshold` (only a smoothing filter carries "adaptive";
the local threshold family does not), `maximum entropy` / `li threshold` /
`huang threshold` / `default threshold` (method-level vocabulary of the
Auto Threshold parameter, only clij/ij2 auto-import nodes carry it),
`measure intensity` (`intensity statistics` is template-walled too),
`remove noise` / `noise removal`, `object classification`, `classify
cells`, `rigid registration` (`registration` works), `orthogonal`,
`save plot`, `8 bit` (templates only).

**Ordering failures measured but not authored** (the current order is
wrong for users, but no Task 3-7 mechanism changes it, so a `first`
assertion would stay red):
- `histogram` -> `ij1-contrast-histogram-enhancer` ranks 1 over
  `plot-table:plot-histogram` (rank 2); both get the same Task 7 bonus.
- `threshold` -> a template plus name-prefix entries
  ("Threshold by annotations", "Threshold/Value statistics 5D") wall out
  `ij1-threshold-auto2d` (rank 8); the name-prefix tier outranks all
  coverage-scored entries by design and `Auto threshold 2D (8-bit)` does
  not start with "threshold".
- `threshold manual` -> color variants rank above the plain manual
  threshold (word-order bonus asymmetry); Task 7's bonus applies to both.
- `merge the channels` -> HSB/LAB/RGB variants rank above the classic node
  (shorter-name Levenshtein tie-break); the stopword "the" blocks the
  Task 7 name-exact bonus for everyone.
- `kmeans` -> the fuzzy c-means variant ranks 1 over plain k-means (same
  fuzzy grade; Task 7's bonus applies to neither, "kmeans" is no name
  token).

These are recorded for a future wave (order-aware ranking or prominence
list); the corpus's ordering cases pin the 11 first-positions that are
correct today.

## Notes for the gate test (Task 2)

- All cases use the default `expectWithin` 5 (the field is omitted).
- `first` appears only on `ordering`-tagged cases.
- `evidence` strings are informational; the gate ignores them.
- The corpus expects node ids (`originNodeId`), including auto-import ids
  where the vocabulary genuinely points there (`ij2:op:filter.findEdges`).
