# 16-bit / n-bit thresholding — Design

Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1327

## Problem

All auto-thresholding nodes are built around ImageJ's 8-bit `AutoThresholder`
(256-bin histograms, in-place `ip.threshold()`). Modern microscopy commonly
produces 16-bit or 32-bit images, which current auto-threshold nodes either
reject (8U-only input slots) or handle via lossy 8-bit conversions. ImageJ's
`AutoThresholder` itself is documented as limited to 256-bin histograms.

## Requirements (from work item + user decisions)

1. Clear naming of all (auto)thresholding nodes indicating bit depth and binning.
2. 16-bit thresholding nodes covering the full feature set of the 8-bit variants
   (auto threshold, percentile threshold, local auto thresholds — manual and
   expression-based 16-bit nodes already exist).
3. Thresholding with custom number of bins via parameter.
4. Same for expression functions.
5. Backwards compatibility: node names/descriptions may change; expression
   function names may NOT change; adding optional parameters to existing
   expression functions is allowed.

## Design decisions (user-approved)

- **Preserve existing nodes exactly** — only their displayed name/description
  changes so users understand limitations. No input-slot widening.
- **Port threshold methods to a JIPipe-owned n-bit engine** rather than
  rescaling to 256 bins around ImageJ's implementation.
- **Multiple engine implementations** rather than one class, so the common
  cases (8-bit, 16-bit) get optimal performance.
- New node set: a 16-bit auto-threshold node + a custom-bins auto-threshold
  node; 16-bit variants of percentile and all local auto-threshold algorithms.
- Output of all new nodes: 8-bit binary mask (`ImagePlusGreyscaleMaskData`),
  matching existing 16-bit/32-bit manual threshold nodes.
- Expression functions: existing 17 `HISTOGRAM_THRESHOLD_*` gain an optional
  `nbins` parameter (default **256**, preserving current truncation behavior
  when omitted); new `HISTOGRAM_THRESHOLD_8_BIT_*` and
  `HISTOGRAM_THRESHOLD_16_BIT_*` variants are added. Return value stays the
  bin index (current behavior).
- 32-bit float bin range in the custom-bins node is governed by the existing
  `AutoThreshold2DAlgorithm.SliceThresholdMode` parameter.
- Naming convention: comma-in-parenthesis, e.g. "Local auto threshold 2D
  (Bernsen, 8-bit)".

## Architecture

### 1. Threshold engine (new, in `jipipe-core`)

Location: `org.hkijena.jipipe.utils.ImageJThresholdUtils` package
(`jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/`). Core already
depends on ImageJ1, and existing expression functions already import
`ij.process.AutoThresholder`, so both expression functions (core) and nodes
(plugin-ij-algorithms → depends on core) share one engine.

Components:

- **`AutoThresholdMethod`** — enum mirroring `ij.process.AutoThresholder.Method`'s
  17 values. **The enum constant names must be exactly identical to ImageJ's**
  (`Default, Huang, Intermodes, IsoData, IJ_IsoData, Li, MaxEntropy, Mean,
  MinError, Minimum, Moments, Otsu, Percentile, RenyiEntropy, Shanbhag,
  Triangle, Yen`): enum parameters are deserialized via Jackson
  `readerFor(fieldClass)`, which matches by constant `name()` — renamed
  constants would break deserialization of existing projects.
  The code base is ported to the new enum: all existing usages of
  `AutoThresholder.Method` as a parameter type (e.g.
  `AutoThreshold2DAlgorithm.getMethod()`,
  `NucleiSegmentation3DAlgorithm.autoThresholdMethod`) are switched to
  `AutoThresholdMethod`, and the existing enum parameter registration
  `registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), ...)`
  in `ImageJAlgorithmsPlugin.registerThresholdAlgorithms()` is changed to
  register the new class (same registration pattern).
  The enum leverages JIPipe's enum item rendering for enhanced documentation:
  `@EnumParameterSettings(itemInfo = AutoThresholdMethodEnumItemInfo.class)`
  with a `JIPipeEnumParameterItemInfo` implementation providing per-method
  labels (`getLabel`) and per-method documentation tooltips (`getTooltip`),
  each describing the method's algorithm and its binning behavior.


- **`NBinsAutoThresholder`** — public entry point. Static method
  `getThreshold(AutoThresholdMethod method, int[] histogram)` where histogram
  length is the bin count. Dispatches:

  - **8-bit fast path:** if `histogram.length <= 256`, delegate directly to
    ImageJ's `AutoThresholder` static method implementations (bit-identical,
    zero porting risk, optimal performance for the common case). The legacy
    ImageJ `>256` sub-range-extraction semantics are NOT used.
  - **16-bit fast path:** for 65536-bin histograms (and generally when the
    populated range exceeds 256), extract the populated sub-range (first→last
    non-zero bin) ONCE into a compact array, run the ported method on it, and
    offset the result back. Most methods are translation-invariant to the
    array bounds and then run on a typically small array. Methods whose
    constants are normalized against array length (Triangle, Percentile,
    entropy-based methods) use the actual sub-range length so the semantics
    match the compacted histogram.
  - **General n-bit path:** ported method implementations that operate on
    arbitrary-length histograms (used for custom bins and as the engine behind
    the 16-bit path).

- **Ported method implementations** — one static method per threshold method,
  ported from ImageJ 1.54p `ij.process.AutoThresholder` (public static
  methods), with every 255/256 hardcoding replaced by `histogram.length - 1`
  scaling. **Fidelity rule:** on 256-bin input every ported method must return
  exactly what ImageJ's implementation returns (unit-tested against it).

Rationale for sub-range extraction: a 65536-bin histogram is sparse for most
microscopy images (12-bit camera data populates ≤4096 bins). Running O(n) or
O(n·iterative) methods on the compact array keeps 16-bit performance on par
with 8-bit, satisfying the optimal-performance requirement for common cases.

### 2. Nodes (jipipe-plugin-ij-algorithms)

All in `nodes/threshold/` (+ `local/`), registered in
`ImageJAlgorithmsPlugin.registerThresholdAlgorithms()`.

**New nodes:**

| Node ID | Name | Input | Notes |
|---|---|---|---|
| `ij1-threshold-auto2d-16u` | Auto threshold 2D (16-bit) | `ImagePlusGreyscale16UData` | Full mirror of `AutoThreshold2DAlgorithm`: all 17 methods, 3 slice modes, dark background, threshold annotation + combination expression, ROI/mask source area |
| `ij1-threshold-auto2d-nbins` | Auto threshold 2D (custom bins) | `ImagePlusGreyscaleData` | `nbins` parameter (default 256, min 2, max 65536); reuses `SliceThresholdMode` |
| `ij1-threshold-percentile2d-16u` | Percentile threshold 2D (16-bit) | `ImagePlusGreyscale16UData` | Mirror of the 8U percentile node with 65536-bin histogram |
| `ij1-threshold-local-auto2d-16u` | Local auto threshold 2D (16-bit) | `ImagePlusGreyscale16UData` | Mean/Median/MidGrey/Otsu selector node, 16-bit |
| `ij1-threshold-local-auto2d-16u-bernsen` | Local auto threshold 2D (Bernsen, 16-bit) | `ImagePlusGreyscale16UData` | |
| `ij1-threshold-local-auto2d-16u-contrast` | Local auto threshold 2D (Contrast, 16-bit) | `ImagePlusGreyscale16UData` | |
| `ij1-threshold-local-auto2d-16u-niblack` | Local auto threshold 2D (Niblack, 16-bit) | `ImagePlusGreyscale16UData` | |
| `ij1-threshold-local-auto2d-16u-phansalkar` | Local auto threshold 2D (Phansalkar, 16-bit) | `ImagePlusGreyscale16UData` | |
| `ij1-threshold-local-auto2d-16u-sauvola` | Local auto threshold 2D (Sauvola, 16-bit) | `ImagePlusGreyscale16UData` | |

All outputs: `ImagePlusGreyscaleMaskData` (8-bit 0/255 mask).

Implementation notes:

- `AutoThreshold2D16UAlgorithm`: histogram via `ShortProcessor.getHistogram()`
  (65536 bins; bin index = pixel value). Threshold is applied per-pixel into a
  fresh 8-bit output hyperstack (pattern from `ManualThreshold16U2DAlgorithm`),
  NOT via `ip.threshold()`. The dark-background inversion happens on a 32-bit
  duplicate or on the histogram (bin reversal) to avoid mutating 16-bit
  original semantics — the simple approach: invert the 16-bit processor copy
  (`255 - v` for 8-bit becomes `65535 - v`) exactly as the 8-bit node inverts,
  then treat as dark background.
- `AutoThreshold2DNBinsAlgorithm`: input `ImagePlusGreyscaleData`; reads pixels
  as 32F floats for uniform per-pixel access, or dispatches on processor type.
  For 8U/16U integer input, bins span the full native range (0–255 /
  0–65535) with `nbins` equal-width bins; bin index → threshold pixel value
  mapping is `binIndex * (range+1) / nbins`. For 32F input the bin range
  follows `SliceThresholdMode`: `ApplyPerSlice` → per-slice min/max;
  `CombineSliceStatistics` → whole-image min/max with histograms summed;
  `CombineThresholdPerSlice` → per-slice ranges, per-slice thresholds combined
  via expression, combined threshold mapped back per-slice. Threshold applied
  per-pixel into 8-bit output mask.
- Local 16-bit nodes: the 8-bit originals already compute rank-filter
  statistics on 32-bit duplicates; the ports keep that structure and replace
  the `(byte[]) ip.getPixels()` loops with short-array loops (`& 0xFFFF`
  masks) writing into a new 8-bit mask. The local Otsu port uses the compact
  sub-range array technique (L becomes the local sub-range length, not 256).
  The 8-bit local nodes remain untouched.
- Per-method node examples registered for both new auto-threshold nodes
  (mirroring the existing `registerNodeExample` loop).

**Renamed (documentation-only changes):**

| Node | Old name | New name |
|---|---|---|
| `ij1-threshold-auto2d` | Auto threshold 2D | Auto threshold 2D (8-bit) |
| `ij1-threshold-manual2d-8u` | Manual threshold 2D (8-bit) | unchanged |
| `ij1-threshold-percentile2d-8u` | Percentile threshold 2D (8-bit) | unchanged |
| `ij1-threshold-local-auto2d` | Local auto threshold 2D | Local auto threshold 2D (Mean/Median/MidGrey/Otsu, 8-bit) |
| `ij1-threshold-local-auto2d-bernsen` | Local auto threshold 2D (Bernsen) | Local auto threshold 2D (Bernsen, 8-bit) |
| `ij1-threshold-local-auto2d-contrast` | Local auto threshold 2D (Contrast) | Local auto threshold 2D (Contrast, 8-bit) |
| `ij1-threshold-local-auto2d-niblack` | Local auto threshold 2D (Niblack) | Local auto threshold 2D (Niblack, 8-bit) |
| `ij1-threshold-local-auto2d-phansalkar` | Local auto threshold 2D (Phansalkar) | Local auto threshold 2D (Phansalkar, 8-bit) |
| `ij1-threshold-local-auto2d-sauvola` | Local auto threshold 2D (Sauvola) | Local auto threshold 2D (Sauvola, 8-bit) |

Descriptions additionally document bit-depth limitations and point to the
16-bit/custom-bins counterparts. Node IDs and `AddJIPipeNodeAlias` entries are
NOT touched. New nodes get no ImageJ alias (no ImageJ equivalent exists).

### 3. Expression functions (jipipe-core)

- **Existing 17 `HISTOGRAM_THRESHOLD_*`** (e.g. `HISTOGRAM_THRESHOLD_OTSU`):
  constructor becomes `ExpressionFunction(name, 1, 2)`; optional second
  parameter `nbins` (default 256). When omitted: exact current behavior
  (truncate to 256 bins). When provided: histogram truncated/padded to
  `nbins` and evaluated by the n-bit engine. Return: bin index (integer).
  Function IDs unchanged.
- **New variants** (34 functions, same 17 methods each):
  - `HISTOGRAM_THRESHOLD_8_BIT_<METHOD>` — validates histogram length ≤ 256,
    evaluates via the ImageJ fast path, returns bin index 0..255.
  - `HISTOGRAM_THRESHOLD_16_BIT_<METHOD>` — validates histogram length ≤
    65536, evaluates via the 16-bit engine path, returns bin index 0..65535.
- Implementation: `HistogramThresholdFunction` base class reworked to delegate
  to `NBinsAutoThresholder`; the 17 existing per-method subclasses keep their
  IDs and gain the optional parameter; 34 new subclasses are added.
  Registered in `ExpressionPlugin.register()`.
- Function help/description documents the nbins semantics (bin index return,
  histogram length = bin count).

### 4. Testing

jipipe-plugin-ij-algorithms currently has no test setup; add JUnit 5 test
dependencies (pattern from `contrib/jipipe-ro-crate-java-2.1.0/pom.xml`) and
create `src/test/java`. Engine tests live in jipipe-core's test setup if
present, otherwise alongside (same pattern).

Tests:

1. **Port fidelity:** all 17 methods × ~100 random 256-bin histograms →
   ported engine output must equal `ij.process.AutoThresholder` output exactly.
2. **Sub-range extraction:** 65536-bin histograms with known populated ranges
   → threshold equals the threshold of the compacted histogram + offset for
   translation-invariant methods; hand-computed expectations for
   length-normalized methods (Triangle, Percentile).
3. **Custom bins:** nbins ∈ {2, 16, 256, 4096, 65536} on synthetic images →
   threshold within valid range; monotonic sanity checks (bimodal histogram →
   threshold between modes).
4. **Expression functions:** with and without `nbins`; 8-bit/16-bit variants
   on valid and over-length histograms (expect validation errors).
5. **Node smoke tests:** 16-bit auto-threshold node on synthetic 2-slice 16-bit
   image produces an 8-bit mask with expected foreground ratio for a bimodal
   input; custom-bins node on 8U/16U/32F inputs.

## Error handling

- `nbins` parameter: min 2, max 65536 (setter validation, `@JIPipeParameter`).
- Expression functions: histogram longer than the declared/validated bin
  count → descriptive `IllegalArgumentException` naming the function, the
  expected bin count, and the actual array length.
- Engine: empty histogram (all zero) → ImageJ parity behavior (methods return
  0 or their empty-input convention); documented in the fidelity tests.
- Nodes: no new failure modes beyond existing nodes' behavior (input slot
  types already enforce depth).

## Out of scope

- 3D thresholding nodes (jipipe-plugin-ij-3d) — may adopt the engine later.
- Color thresholding (HSB/RGB/LAB nodes) — 8-bit per-channel by design.
- Renaming expression functions (forbidden by requirements).
- Widening existing node input slots (preserved per user decision).

## Migration / compatibility

- Old projects: all existing node IDs, parameter IDs, and function IDs
  unchanged; serialized projects load identically. Only display names change.
- **Enum migration:** all production usages of `AutoThresholder.Method` are
  ported to the new `AutoThresholdMethod` enum:
  - `AutoThreshold2DAlgorithm` (field, getter/setter, `runIteration`)
  - `NucleiSegmentation3DAlgorithm` (jipipe-plugin-ij-3d; field
    `autoThresholdMethod`, getter/setter)
  - `ImageJAlgorithmsPlugin.registerThresholdAlgorithms()` — the
    `registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), ...)`
    registration is changed to register `AutoThresholdMethod` (new ID, e.g.
    its canonical name); the per-method `registerNodeExample` loop follows.
  - The 17 `HistogramThreshold*` expression function subclasses switch from
    `AutoThresholder.Method.X` to `AutoThresholdMethod.X`.
  Because the constant `name()`s are identical, stored values like
  `"method": "Otsu"` deserialize identically after the type switch
  (deserialization goes through
  `JsonUtils.getObjectMapper().readerFor(fieldClass)` at ParameterUtils.java:114,
  which matches by constant name).
- Existing expressions calling `HISTOGRAM_THRESHOLD_X(h)` behave identically
  (nbins defaults to 256 with truncation).
- New nodes/functions are additive.
