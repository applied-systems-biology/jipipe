# Taxonomy function name harmonization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename 41 taxonomy function display names so the same concept is spelled the same way in every branch (work item 1406).

**Architecture:** Pure display-name sweep over the `getName()` string literals of the taxonomy function classes in `jipipe-core/.../api/nodes/taxonomy/functions/`, plus lockstep updates to the one test that asserts on name words (`TokenTaxonomyTest`), three code comments that quote a renamed name, and two rows of the conversion decision record. No IDs, no UI code, no data changes.

**Tech Stack:** Java 21, Maven, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-25-taxonomy-function-name-harmonization-design.md` (the name map there is normative; this plan argues from it).

## Global Constraints

- Registry IDs (`public static final String ID = "jipipe:function:..."`) must NOT change (stable-ID rule; saved projects persist them).
- Only `getName()` return values (and Javadoc/comments that quote the old display name) change. Descriptions, tags, hues, icons, parents, visibility flags stay.
- No em dashes or en dashes anywhere (AGENTS.md rule); the new names contain none.
- Branch: `1406-taxonomy-function-names` off current `master`. Commit style follows recent history: `fix(#1406): ...`.
- `utils/taxonomy-research/**` and historical spec docs (`docs/superpowers/specs/2026-10-*.md`) are dated research/spec records: leave them untouched.

## Review Focus

- **Missed rename inside the functions package:** every old name literal must be gone from `api/nodes/taxonomy/functions/`; the final grep in Task 2 pins this.
- **Sibling name collision after renaming to bare words:** two children of the same parent must not end up with the same name; the duplicate check in Task 2 pins this.
- **Search-token regression:** `TokenTaxonomyTest` pins which words land at which weight; its hardcoded expectations must change in lockstep with the renames (Task 1 steps 5-7).
- **ID drift:** `git diff` must show no changed `ID =` line; the Task 2 verification step pins this.

No new tests are written: the change is a data rename, and the existing taxonomy gates (presence-based, plugin-aware per the 2026-10-22 ruling) already run over the registry. A naming-convention test over the live registry would constrain plugin-contributed functions and was deliberately not added.

---

### Task 1: Apply the renames and adapt dependents

**Files:**
- Modify (41, `getName()` literal + any Javadoc quoting the old name):
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/annotations/AnnotationsConvertFunction.java` ("Convert annotations" -> "Convert")
  - `.../functions/annotations/AnnotationsEditFunction.java` ("Edit annotations" -> "Edit")
  - `.../functions/annotations/GraphAnnotationsFunction.java` ("Graph annotations" -> "Canvas")
  - `.../functions/export/ExportRoiFunction.java` ("ROI" -> "ROIs")
  - `.../functions/filaments/FilamentFilteringFunction.java` ("Filament filtering" -> "Filtering")
  - `.../functions/filaments/FilamentQuantificationFunction.java` ("Filament quantification" -> "Quantification")
  - `.../functions/filaments/FilamentSmoothingFunction.java` ("Filament smoothing" -> "Smoothing")
  - `.../functions/graph/GraphDebugFunction.java` ("Graph debugging" -> "Debugging")
  - `.../functions/graph/GraphWrappersFunction.java` ("Graph wrappers" -> "Wrappers")
  - `.../functions/images/ImageArithmeticFunction.java` ("Image arithmetic" -> "Arithmetic")
  - `.../functions/images/ImageComparisonFunction.java` ("Image comparison" -> "Comparison")
  - `.../functions/images/ImageLabelingFunction.java` ("Image labeling" -> "Labeling")
  - `.../functions/images/ImageMomentsFunction.java` ("Image moments" -> "Moments")
  - `.../functions/images/ImageNeighborMeasurementsFunction.java` ("Image neighbor measurements" -> "Neighbor measurements")
  - `.../functions/images/ImagePixelManipulationFunction.java` ("Image pixel manipulation" -> "Pixel manipulation")
  - `.../functions/images/ImageProjectionsFunction.java` ("Image projections" -> "Projections")
  - `.../functions/images/ImagePropertiesFunction.java` ("Image properties" -> "Properties")
  - `.../functions/images/ImageStatisticsFunction.java` ("Image statistics" -> "Statistics")
  - `.../functions/images/ImageTransformationsFunction.java` ("Image transformations" -> "Transformations")
  - `.../functions/images/ImageTypeConversionFunction.java` ("Image type conversion" -> "Type conversion")
  - `.../functions/images/SegmentationCellBasedFunction.java` ("Cell-based segmentation" -> "Cell-based")
  - `.../functions/machinelearning/MachineLearningInferenceFunction.java` ("Machine learning inference" -> "Inference")
  - `.../functions/machinelearning/MachineLearningModelsFunction.java` ("Machine learning models" -> "Models")
  - `.../functions/machinelearning/MachineLearningTrainingFunction.java` ("Machine learning training" -> "Training")
  - `.../functions/misc/MiscellaneousUnassignedFunction.java` ("Miscellaneous unassigned" -> "Unassigned")
  - `.../functions/rois/RoiFiltersFunction.java` ("ROI filters" -> "Filtering")
  - `.../functions/rois/RoiIntersectionsFunction.java` ("ROI intersections" -> "Intersections")
  - `.../functions/rois/RoiMeasurementsFunction.java` ("ROI measurements" -> "Measurements")
  - `.../functions/rois/RoiMorphologyFunction.java` ("ROI morphology" -> "Morphology")
  - `.../functions/rois/RoiPropertiesFunction.java` ("ROI properties" -> "Properties")
  - `.../functions/statistics/StatisticsDescriptiveFunction.java` ("Descriptive statistics" -> "Descriptive")
  - `.../functions/statistics/StatisticsTestingFunction.java` ("Statistical testing" -> "Testing")
  - `.../functions/tables/TableAggregateFunction.java` ("Aggregate tables" -> "Aggregate")
  - `.../functions/tables/TableExportFileFunction.java` ("Export tables to file" -> "Export to file")
  - `.../functions/tables/TableFilterModifyFunction.java` ("Filter & modify tables" -> "Filter & modify")
  - `.../functions/tables/TableJoinSplitFunction.java` ("Join & split tables" -> "Join & split")
  - `.../functions/tables/TableTransformFunction.java` ("Transform tables" -> "Transform")
  - `.../functions/tracking/TrackingAnalysisFunction.java` ("Tracking: analysis" -> "Analysis")
  - `.../functions/tracking/TrackingDetectionFunction.java` ("Tracking: detection" -> "Detection")
  - `.../functions/tracking/TrackingFilteringFunction.java` ("Tracking: filtering" -> "Filtering")
  - `.../functions/tracking/TrackingLinkingFunction.java` ("Tracking: linking" -> "Linking")
  (all under `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/`)
- Modify (comments quoting a renamed function name):
  - `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java:371` (comment "Miscellaneous unassigned")
  - `plugins/jipipe-plugin-clij/src/main/java/org/hkijena/jipipe/plugins/clij2/CLIJCommandNodeInfo.java:285` (comment)
  - `plugins/jipipe-plugin-ij2/src/main/java/org/hkijena/jipipe/plugins/imagej2/ImageJ2OpNodeInfo.java:205` (comment)
- Modify (decision record rows using old names):
  - `utils/taxonomy-conversion/conversion-map.md:1942` ("Edit annotations") and `:2391` ("Tracking: detection")
- Modify (test expectations):
  - `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/TokenTaxonomyTest.java`
- Commit (already written, currently untracked): `docs/superpowers/specs/2026-11-25-taxonomy-function-name-harmonization-design.md`

**Interfaces:**
- Consumes: nothing (data change).
- Produces: the display names from the spec's name map. `TokenTaxonomyTest`'s `taxonomyWords()` derives its word set dynamically from `getName()`, so only its hardcoded word assertions need updating (steps 5-7).

- [ ] **Step 1: Create the branch**

```bash
git checkout -b 1406-taxonomy-function-names
```

- [ ] **Step 2: Apply the 41 `getName()` renames**

Edit each file's `getName()` return literal per the map above. Only the display name changes; leave `ID`, `getDescription()`, `getTags()`, parents, and every other member untouched. If the class Javadoc quotes the old display name, update that quote too.

- [ ] **Step 3: Update the three comments that quote renamed names**

`TaxonomyPlugin.java` (line ~371), `CLIJCommandNodeInfo.java` (line ~285), `ImageJ2OpNodeInfo.java` (line ~205): change the quoted "Miscellaneous unassigned" to the new name "Unassigned", keeping the comment's meaning.

- [ ] **Step 4: Update the two conversion-map rows**

`utils/taxonomy-conversion/conversion-map.md`: line 1942 `"Edit annotations"` -> `"Edit"`, line 2391 `"Tracking: detection"` -> `"Detection"` (only the quoted function names; the node-class names stay).

- [ ] **Step 5: Run TokenTaxonomyTest and confirm the expected failures**

Run: `mvn test -pl jipipe-core -Dtest=TokenTaxonomyTest`
Expected: FAIL, and only because of the renamed words: (a) assertion on `"roi"` at `WEIGHT_FUNCTION_LEAF` (secondary leaf is now "Measurements", so "roi" is no longer a token), (b) assertion that `"segmentation"` sits at leaf(6) AND parent(4) (leaf is now "Cell-based", so "segmentation" is parent(4) only).

- [ ] **Step 6: Update TokenTaxonomyTest expectations**

In `TokenTaxonomyTest.java`:
- Class Javadoc word list: primary path becomes `Images > Segmentation > Cell-based`: "cell-based" => {6}; "segmentation" (the parent "Segmentation"'s only word) => {4}; "images" => {2}. Secondary leaf: "Measurements" (parent "ROIs", top none): "measurements" (leaf word) => {6}; ancestors still not walked ("rois" absent).
- Delete the `"roi"` leaf-weight assertion (the token no longer exists); keep `assertFalse(tokens.contains("rois"))`.
- Change the `"segmentation"` assertion to `Set.of(WeightedTokens.WEIGHT_FUNCTION_PARENT)` with a comment that the leaf word coincidence case no longer occurs in this fixture (word-weight accumulation stays covered by the "cells" name+tag assertion).
- Update the two prose comments quoting "Cell-based segmentation" / "ROI measurements" (lines ~68, ~186, ~219) to the new names.

- [ ] **Step 7: Run TokenTaxonomyTest to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=TokenTaxonomyTest`
Expected: PASS.

- [ ] **Step 8: Verify the rename is complete and collision-free**

```bash
grep -rn "Convert annotations\|Edit annotations\|Graph annotations\|Filament filtering\|Filament quantification\|Filament smoothing\|Graph debugging\|Graph wrappers\|Image arithmetic\|Image comparison\|Image labeling\|Image moments\|Image neighbor measurements\|Image pixel manipulation\|Image projections\|Image properties\|Image statistics\|Image transformations\|Image type conversion\|Cell-based segmentation\|Machine learning inference\|Machine learning models\|Machine learning training\|Miscellaneous unassigned\|ROI filters\|ROI intersections\|ROI measurements\|ROI morphology\|ROI properties\|Descriptive statistics\|Statistical testing\|Aggregate tables\|Export tables to file\|Filter & modify tables\|Join & split tables\|Transform tables\|Tracking: analysis\|Tracking: detection\|Tracking: filtering\|Tracking: linking" \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/
```
Expected: no output (the `utils/taxonomy-research/` snapshots and historical specs keep their old names by design).

```bash
python3 - <<'EOF'
import re, glob, os, collections
base = 'jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions'
cls2name, cls2parent = {}, {}
for f in glob.glob(base + '/**/*.java', recursive=True):
    src = open(f).read()
    cls = os.path.basename(f)[:-5]
    m = re.search(r'public String getName\(\)\s*\{[^}]*?return "([^"]*)"', src, re.S)
    p = re.search(r'return new (\w+)\(\);', src)
    cls2name[cls] = m.group(1); cls2parent[cls] = p.group(1) if p else None
seen = collections.defaultdict(list)
for cls, name in cls2name.items():
    seen[(cls2parent.get(cls), name)].append(cls)
dupes = {k: v for k, v in seen.items() if len(v) > 1}
print('DUPLICATES:', dupes if dupes else 'none')
assert not dupes
EOF
```
Expected: `DUPLICATES: none`.

- [ ] **Step 9: Verify no ID drifted**

```bash
git diff --stat; git diff | grep -c 'ID = ' || true
```
Expected: 41 modified function files + 6 dependent files; zero changed lines containing `ID = `.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "fix(#1406): harmonize taxonomy function display names

Bare leaf names: a sub-function never repeats its parent branch's word;
the same concept is spelled the same way in every branch (Tracking:
filtering / Filament filtering / ROI filters are all Filtering now).
Registry IDs, descriptions, tags, hues, and placements are unchanged."
```

---

### Task 2: Run the taxonomy gates and the core test suite

**Files:** none (verification only).

**Interfaces:**
- Consumes: Task 1's renamed display names.
- Produces: green validation gates for the whole registry.

- [ ] **Step 1: Run the full taxonomy validation gate**

Run: `mvn test -pl jipipe-launcher -Dtest=TaxonomyFullValidationTest -am -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (production validation over the entire registry; catches parent-chain, hue, and registration problems if the renames had broken anything).

- [ ] **Step 2: Run the node database / browser taxonomy tests in jipipe-core**

Run: `mvn test -pl jipipe-core -Dtest='TokenTaxonomyTest,NodeDatabaseTaxonomyTest,JIPipeJavaNodeInfoTaxonomyTest,TaxonomyResolverTest,NodeTaxonomyInferenceTest'`
Expected: PASS.

- [ ] **Step 3: Sanity-check that system-templates.json references survived**

`jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/system-templates.json` contains the strings "Edit annotations", "Image statistics", "ROI measurements", "ROI properties" only as template/node display names; template-to-function links use `functionId`, never the display name. Confirm:

```bash
grep -n '"functionId"' jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/system-templates.json | head
```
Expected: all `functionId` values are `jipipe:function:*` IDs (unchanged), so no template edit is needed.

- [ ] **Step 4: Report the result**

If all green, the branch is ready for review/MR. Any failure here: fix before proceeding, do not exclude tests (ImageJ patcher ordering rule).

---

## Self-review notes

- Spec coverage: all 41 rows of the spec's name map appear in Task 1's file list with exact old -> new values; the "deliberately unchanged" list needs no task.
- The two-track risk (rename vs test) is ordered rename-first so the test failure observed in step 5 is the expected one.
- `CanvasNotesPlugin.getName()` ("Graph annotations") is the plugin's own name, not the taxonomy function: out of scope, untouched.
- `jipipe-core/target/**` matches are build output: ignored, never committed.
