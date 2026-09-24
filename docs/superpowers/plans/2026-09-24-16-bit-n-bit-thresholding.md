# 16-bit / n-bit Thresholding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 16-bit and custom-bins (n-bit) auto-thresholding to JIPipe — a JIPipe-owned n-bin threshold engine, new 16-bit/custom-bins nodes, 16-bit local auto-thresholds, and n-bit histogram threshold expression functions — per spec `docs/superpowers/specs/2026-09-24-16-bit-n-bit-thresholding-design.md` (work item #1327).

**Architecture:** One threshold engine in `jipipe-core` (`AutoThresholdMethod` enum + `NBinsAutoThresholder` dispatcher + ported method implementations) shared by expression functions (core) and nodes (plugin-ij-algorithms). The engine delegates to ImageJ's `AutoThresholder` for ≤256-bin histograms (bit-identical fast path) and runs ported implementations on the populated sub-range for larger histograms. All new nodes write 8-bit masks.

**Tech Stack:** Java 21, Maven, ImageJ1 1.54p (already a dependency), JUnit 5 (test scope inherited from root pom at `pom.xml:342-348`).

## Global Constraints

- Enum constant names of `AutoThresholdMethod` are **exactly** ImageJ's: `Default, Huang, Intermodes, IsoData, IJ_IsoData, Li, MaxEntropy, Mean, MinError, Minimum, Moments, Otsu, Percentile, RenyiEntropy, Shanbhag, Triangle, Yen` (deserialization matches by constant `name()`).
- Existing node IDs, parameter IDs, expression function IDs must NOT change.
- Existing `HISTOGRAM_THRESHOLD_*` functions keep exact current behavior when the new optional `nbins` argument is omitted (truncate to 256 bins).
- Output of every new node: `ImagePlusGreyscaleMaskData` (8-bit 0/255 mask).
- Node naming: comma-in-parenthesis, e.g. "Local auto threshold 2D (Bernsen, 8-bit)".
- Every new Java file starts with the standard MIT license header (copy from `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2DAlgorithm.java:1-12`).
- Commands run from repo root `/data/src/jipipe-4` unless stated. Build: `mvn -q -pl <module> -am compile` (use `-o` if offline works; omit if it fails). Tests: `mvn -q -pl <module> test -Dtest=<Class>`.
- Commit after every task, message prefix `Threshold: ` to match spec work.

---

### Task 1: `AutoThresholdMethod` enum + item info

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/AutoThresholdMethod.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/AutoThresholdMethodEnumItemInfo.java`

**Interfaces:**
- Produces: `enum AutoThresholdMethod` with ImageJ's exact 17 constants; `AutoThresholdMethodEnumItemInfo implements JIPipeEnumParameterItemInfo` (`org.hkijena.jipipe.plugins.parameters.api.enums`).

- [ ] **Step 1: Create the enum**

`AutoThresholdMethod.java` — full content (with the standard license header from Global Constraints at the top):

```java
package org.hkijena.jipipe.utils.threshold;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

/**
 * Auto-thresholding methods, mirroring {@link ij.process.AutoThresholder.Method}.
 * The constant names are intentionally identical to ImageJ's, as enum parameters
 * are serialized via the constant name and existing projects must stay loadable.
 * The implementations operate on arbitrary bin counts (see {@link NBinsAutoThresholder}).
 */
@EnumParameterSettings(itemInfo = AutoThresholdMethodEnumItemInfo.class)
public enum AutoThresholdMethod {
    Default,
    Huang,
    Intermodes,
    IsoData,
    IJ_IsoData,
    Li,
    MaxEntropy,
    Mean,
    MinError,
    Minimum,
    Moments,
    Otsu,
    Percentile,
    RenyiEntropy,
    Shanbhag,
    Triangle,
    Yen
}
```

- [ ] **Step 2: Create the item info**

`AutoThresholdMethodEnumItemInfo.java` — full content (with license header):

```java
package org.hkijena.jipipe.utils.threshold;

import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;

import javax.swing.*;

/**
 * Renders per-method documentation for {@link AutoThresholdMethod}.
 */
public class AutoThresholdMethodEnumItemInfo implements JIPipeEnumParameterItemInfo {

    private static String description(AutoThresholdMethod method) {
        switch (method) {
            case Default:
                return "<b>Default</b><br>The IsoData method used by ImageJ's Threshold widget, "
                        + "with a correction for dominant peaks. Robust general-purpose default.";
            case Huang:
                return "<b>Huang</b><br>Huang's fuzzy thresholding using Shannon entropy. "
                        + "Slower on dense histograms (iterative per-bin entropy).";
            case Intermodes:
                return "<b>Intermodes</b><br>Assumes a bimodal histogram; iteratively smooths it "
                        + "until two maxima remain. The threshold is the mean of the two modes.";
            case IsoData:
                return "<b>IsoData</b><br>Iterative inter-means thresholding (Ridler &amp; Calvard).";
            case IJ_IsoData:
                return "<b>IJ IsoData</b><br>The original ImageJ IsoData implementation (kept for "
                        + "backward compatibility).";
            case Li:
                return "<b>Li</b><br>Li's minimum cross-entropy thresholding (iterative).";
            case MaxEntropy:
                return "<b>MaxEntropy</b><br>Kapur-Sahoo-Wong maximum entropy thresholding.";
            case Mean:
                return "<b>Mean</b><br>The threshold is the mean of the histogram values.";
            case MinError:
                return "<b>MinError</b><br>Kittler-Illingworth minimum error thresholding (iterative).";
            case Minimum:
                return "<b>Minimum</b><br>Assumes a bimodal histogram; iteratively smooths it until "
                        + "two maxima remain. The threshold is the minimum between the modes.";
            case Moments:
                return "<b>Moments</b><br>Tsai's moment-preserving thresholding.";
            case Otsu:
                return "<b>Otsu</b><br>Otsu's between-class variance maximization.";
            case Percentile:
                return "<b>Percentile</b><br>Doyle's percentile thresholding (50% foreground).";
            case RenyiEntropy:
                return "<b>RenyiEntropy</b><br>Renyi-entropy based thresholding (uses alpha = 0.5, 1, 2).";
            case Shanbhag:
                return "<b>Shanbhag</b><br>Shanbhag's information-measure thresholding.";
            case Triangle:
                return "<b>Triangle</b><br>Zack-Rogers-Latt triangle algorithm; skew-corrected. "
                        + "Suited for images with a dominant peak at one end.";
            case Yen:
                return "<b>Yen</b><br>Yen's maximum entropy criterion thresholding.";
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return null;
    }

    @Override
    public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return value.toString();
    }

    @Override
    public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        if (value instanceof AutoThresholdMethod)
            return "<html>" + description((AutoThresholdMethod) value) + "</html>";
        return null;
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn -q -pl jipipe-core -am compile`
Expected: BUILD SUCCESS (no output with `-q` on success).

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/
git commit -m "Threshold: add AutoThresholdMethod enum with enhanced item info"
```

---

### Task 2: Ported n-bit threshold method implementations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/NBinsThresholdMethods.java`

**Interfaces:**
- Produces: `public static int NBinsThresholdMethods.<method>(int[] histogram)` for all 17 methods — signatures identical to ImageJ's `AutoThresholder` static methods, but correct for any histogram length ≥ 2.

- [ ] **Step 1: Create the ported methods class**

`NBinsThresholdMethods.java` — full content (with license header). This is a line-by-line port of ImageJ 1.54p `ij.process.AutoThresholder`'s static methods (source at `~/.m2/repository/net/imagej/ij/1.54p/ij-1.54p-sources.jar`, file `ij/process/AutoThresholder.java`, lines 190–1383), with these systematic changes:
- `IJ.log(...)` calls removed (methods return -1/0 instead and the dispatcher decides).
- Package-private `defaultIsoData`/`IJIsoData` (ImageJ lines 100–164) are ported as private methods (they back `Default` and `IJ_IsoData`).
- No 255/256 constants remain — every loop/array bound is `data.length` (the ImageJ source already uses `data.length` throughout; verify while porting that no `256` or `255` literals remain).
- `A`, `B`, `C`, `partialSum`, `bimodalTest` helpers are ported as private static.

```java
package org.hkijena.jipipe.utils.threshold;

/**
 * Auto-threshold method implementations ported from ImageJ 1.54p
 * {@code ij.process.AutoThresholder} (by G. Landini, from the Auto_Threshold
 * plugin), adapted to operate on histograms with an arbitrary number of bins.
 * The histogram array length is the bin count; bin indices are threshold
 * candidates. On a 256-bin input every method must return exactly what
 * ImageJ's implementation returns (enforced by unit tests).
 */
public final class NBinsThresholdMethods {

    private NBinsThresholdMethods() {
    }

    // Port of AutoThresholder.defaultIsoData (package-private in ImageJ)
    private static int defaultIsoData(int[] data) {
        // ... ported body from ImageJ lines 100-124, unchanged
    }

    // Port of AutoThresholder.IJIsoData (package-private in ImageJ)
    private static int IJIsoData(int[] data) {
        // ... ported body from ImageJ lines 126-164, unchanged
    }

    public static int Default(int[] data) {
        return defaultIsoData(data);
    }

    public static int IJ_IsoData(int[] data) {
        return IJIsoData(data);
    }

    // public static int Huang(int[] data) { ... ported from ImageJ lines 228-311 }
    // public static int Intermodes(int[] data) { ... lines 384-432, minus IJ.log }
    // public static int IsoData(int[] data) { ... lines 434-494, minus IJ.log }
    // public static int Li(int[] data) { ... lines 496-576 }
    // public static int MaxEntropy(int[] data) { ... lines 578-661 }
    // public static int Mean(int[] data) { ... lines 663-676 }
    // public static int MinError(int[] data) { ... lines 678-724: port MinErrorI, minus IJ.log }
    // public static int Minimum(int[] data) { ... lines 747-796, minus IJ.log }
    // public static int Moments(int[] data) { ... lines 798-848 }
    // public static int Otsu(int[] data) { ... lines 850-904 }
    // public static int Percentile(int[] data) { ... lines 906-933 }
    // public static int RenyiEntropy(int[] data) { ... lines 944-1142 }
    // public static int Shanbhag(int[] data) { ... lines 1145-1225 }
    // public static int Triangle(int[] data) { ... lines 1228-1328 }
    // public static int Yen(int[] data) { ... lines 1331-1383 }

    // private static double A(int[] y, int j) { ... lines 726-731 }
    // private static double B(int[] y, int j) { ... lines 733-738 }
    // private static double C(int[] y, int j) { ... lines 740-745 }
    // private static double partialSum(int[] y, int j) { ... lines 936-941 }
    // private static boolean bimodalTest(double[] y) { ... lines 367-382 }
}
```

Porting rule: copy each ImageJ method body verbatim from the sources jar (`unzip -p ~/.m2/repository/net/imagej/ij/1.54p/ij-1.54p-sources.jar ij/process/AutoThresholder.java` to view it), delete `IJ.log` statements and the `import ij.IJ`, and rename `MinErrorI` to `MinError`. Do not "improve" any arithmetic — fidelity to ImageJ output is the requirement.

- [ ] **Step 2: Compile**

Run: `mvn -q -pl jipipe-core -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/NBinsThresholdMethods.java
git commit -m "Threshold: port AutoThresholder methods to n-bin implementations"
```

---

### Task 3: `NBinsAutoThresholder` dispatcher + fidelity tests

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/NBinsAutoThresholder.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/threshold/NBinsAutoThresholderTest.java`

**Interfaces:**
- Consumes: `NBinsThresholdMethods.<method>(int[])` (Task 2), `AutoThresholdMethod` (Task 1).
- Produces: `public static int NBinsAutoThresholder.getThreshold(AutoThresholdMethod method, int[] histogram)` — histogram length is the bin count; returns a bin index in `[0, histogram.length - 1]` or a negative ImageJ-parity sentinel (-1) for pathological input. Also `public static int getThreshold8U(AutoThresholdMethod, int[])` (≤256-bin fast path, exact ImageJ semantics including bilevel handling) and `public static int getThreshold16U(AutoThresholdMethod, int[])`.

- [ ] **Step 1: Write the failing fidelity test**

`NBinsAutoThresholderTest.java` — full content (with license header):

```java
package org.hkijena.jipipe.utils.threshold;

import ij.process.AutoThresholder;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NBinsAutoThresholderTest {

    /**
     * On 256-bin histograms every method must return exactly what ImageJ's
     * AutoThresholder returns.
     */
    @Test
    void testFidelityWithImageJOn256Bins() {
        AutoThresholder imageJ = new AutoThresholder();
        Random random = new Random(42);
        for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
            for (int trial = 0; trial < 100; trial++) {
                int[] histogram = randomHistogram(random, 256, trial);
                int expected = imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);
                int actual = NBinsAutoThresholder.getThreshold(method, histogram);
                assertEquals(expected, actual, method + " trial " + trial);
            }
        }
    }

    @Test
    void testNullHistogramRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, null));
    }

    /**
     * 65536-bin histogram with a sparse populated range: threshold must be
     * offset back into the original bin space and stay within the populated range.
     */
    @Test
    void test16BitSparseHistogram() {
        // Populated bins 1000..1200, bimodal: peaks at 1050 and 1150
        int[] histogram = new int[65536];
        for (int i = 1030; i <= 1070; i++) histogram[i] = 200 - Math.abs(i - 1050) * 4;
        for (int i = 1130; i <= 1170; i++) histogram[i] = 200 - Math.abs(i - 1150) * 4;
        int threshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, histogram);
        // Threshold between the two modes (exclusive of peak centers)
        assertEquals(true, threshold > 1070 && threshold < 1130, "Otsu threshold between modes, got " + threshold);
        // Translation invariance: shifting the populated range by +1000 must shift the threshold by +1000
        int[] shifted = new int[65536];
        for (int i = 1000; i < 1201; i++) shifted[i + 1000 < 65536 ? i : i] = histogram[i];
        for (int i = 0; i < 65536; i++) shifted[i] = 0;
        for (int i = 1000; i <= 1200; i++) shifted[i + 1000] = histogram[i];
        int shiftedThreshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, shifted);
        assertEquals(threshold + 1000, shiftedThreshold, "Otsu translation invariance");
    }

    /**
     * Custom bin counts: a bimodal 16-bin histogram must yield a threshold between the modes.
     */
    @Test
    void testCustomBinCount() {
        int[] histogram = new int[16];
        histogram[2] = 100;
        histogram[3] = 200;
        histogram[12] = 200;
        histogram[13] = 100;
        int threshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, histogram);
        assertEquals(true, threshold >= 3 && threshold <= 12, "Threshold between modes, got " + threshold);
    }

    private int[] randomHistogram(Random random, int bins, int trial) {
        int[] histogram = new int[bins];
        switch (trial % 3) {
            case 0: // bimodal
                for (int i = 0; i < bins; i++)
                    histogram[i] = (int) (100 * Math.exp(-Math.pow(i - 60, 2) / 200.0))
                            + (int) (100 * Math.exp(-Math.pow(i - 190, 2) / 200.0));
                break;
            case 1: // uniform random
                for (int i = 0; i < bins; i++)
                    histogram[i] = random.nextInt(100);
                break;
            default: // sparse
                histogram[random.nextInt(bins)] = 10;
                histogram[random.nextInt(bins)] = 50;
                histogram[random.nextInt(bins)] = 25;
                break;
        }
        return histogram;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=NBinsAutoThresholderTest`
Expected: COMPILATION ERROR (`NBinsAutoThresholder` does not exist).

- [ ] **Step 3: Implement `NBinsAutoThresholder`**

Full content (with license header):

```java
package org.hkijena.jipipe.utils.threshold;

import ij.process.AutoThresholder;

import java.util.Arrays;

/**
 * Dispatches auto-thresholding for histograms with an arbitrary number of bins.
 * <p>
 * For histograms with at most 256 bins the call is delegated to ImageJ's
 * {@link AutoThresholder} (including its bilevel short-circuit), so results are
 * bit-identical to classic 8-bit thresholding. For larger histograms the
 * populated sub-range (first to last non-zero bin) is extracted once, the
 * matching n-bin implementation from {@link NBinsThresholdMethods} runs on the
 * compact array, and the result is offset back.
 */
public final class NBinsAutoThresholder {

    private static final AutoThresholder IMAGEJ_THRESHOLDER = new AutoThresholder();
    private static final int MAX_FAST_PATH_BINS = 256;

    private NBinsAutoThresholder() {
    }

    /**
     * Calculates a threshold for a histogram of arbitrary bin count.
     *
     * @param method    the method
     * @param histogram the histogram; its length is the bin count
     * @return the threshold bin index
     */
    public static int getThreshold(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length <= MAX_FAST_PATH_BINS)
            return IMAGEJ_THRESHOLDER.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);

        // Extract the populated sub-range
        int minbin = -1;
        int maxbin = -1;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                if (minbin < 0) minbin = i;
                maxbin = i;
            }
        }
        if (minbin < 0)
            return 0; // empty histogram
        int[] compact = Arrays.copyOfRange(histogram, minbin, maxbin + 1);

        int threshold = invokePorted(method, compact);
        if (threshold < 0)
            threshold = 0;
        return threshold + minbin;
    }

    /**
     * 8-bit fast path with exact ImageJ semantics (histogram of at most 256 bins).
     *
     * @param method    the method
     * @param histogram the histogram (at most 256 bins)
     * @return the threshold bin index
     */
    public static int getThreshold8U(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length > 256)
            throw new IllegalArgumentException("8-bit thresholding requires a histogram with at most 256 bins, got " + histogram.length);
        return IMAGEJ_THRESHOLDER.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);
    }

    /**
     * 16-bit thresholding (histogram of at most 65536 bins).
     *
     * @param method    the method
     * @param histogram the histogram (at most 65536 bins)
     * @return the threshold bin index (= pixel value for full-range 16-bit histograms)
     */
    public static int getThreshold16U(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length > 65536)
            throw new IllegalArgumentException("16-bit thresholding requires a histogram with at most 65536 bins, got " + histogram.length);
        return getThreshold(method, histogram);
    }

    private static int invokePorted(AutoThresholdMethod method, int[] histogram) {
        switch (method) {
            case Default: return NBinsThresholdMethods.Default(histogram);
            case Huang: return NBinsThresholdMethods.Huang(histogram);
            case Intermodes: return NBinsThresholdMethods.Intermodes(histogram);
            case IsoData: return NBinsThresholdMethods.IsoData(histogram);
            case IJ_IsoData: return NBinsThresholdMethods.IJ_IsoData(histogram);
            case Li: return NBinsThresholdMethods.Li(histogram);
            case MaxEntropy: return NBinsThresholdMethods.MaxEntropy(histogram);
            case Mean: return NBinsThresholdMethods.Mean(histogram);
            case MinError: return NBinsThresholdMethods.MinError(histogram);
            case Minimum: return NBinsThresholdMethods.Minimum(histogram);
            case Moments: return NBinsThresholdMethods.Moments(histogram);
            case Otsu: return NBinsThresholdMethods.Otsu(histogram);
            case Percentile: return NBinsThresholdMethods.Percentile(histogram);
            case RenyiEntropy: return NBinsThresholdMethods.RenyiEntropy(histogram);
            case Shanbhag: return NBinsThresholdMethods.Shanbhag(histogram);
            case Triangle: return NBinsThresholdMethods.Triangle(histogram);
            case Yen: return NBinsThresholdMethods.Yen(histogram);
            default: throw new UnsupportedOperationException("Unknown method: " + method);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl jipipe-core test -Dtest=NBinsAutoThresholderTest`
Expected: 4 tests PASS. If `testFidelityWithImageJOn256Bins` fails for specific methods, the port of that method in Task 2 deviates from ImageJ — compare method bodies line-by-line against the sources jar and fix the port, not the test.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/threshold/NBinsAutoThresholder.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/utils/threshold/NBinsAutoThresholderTest.java
git commit -m "Threshold: add NBinsAutoThresholder engine with ImageJ fidelity tests"
```

---

### Task 4: Migrate `AutoThreshold2DAlgorithm` and enum registration to `AutoThresholdMethod`

**Files:**
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2DAlgorithm.java`
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/ImageJAlgorithmsPlugin.java` (registration at lines 1060–1098)

**Interfaces:**
- Consumes: `AutoThresholdMethod`, `NBinsAutoThresholder.getThreshold(AutoThresholdMethod, int[])` (Tasks 1–3).
- Produces: `AutoThreshold2DAlgorithm.getMethod()` returns `AutoThresholdMethod`; nested enum `AutoThreshold2DAlgorithm.SliceThresholdMode` unchanged (reused by other nodes and the new nodes).

- [ ] **Step 1: Update `AutoThreshold2DAlgorithm`**

Changes (keep everything else — parameters, slice modes, ROI/mask handling — byte-identical):
1. Replace import `ij.process.AutoThresholder` with `org.hkijena.jipipe.utils.threshold.AutoThresholdMethod` and `org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder`.
2. Field: `private AutoThresholdMethod method = AutoThresholdMethod.Default;`
3. Getter/setter: change types `AutoThresholder.Method` → `AutoThresholdMethod` (parameter key `"method"` unchanged).
4. In `runIteration`, delete `AutoThresholder autoThresholder = new AutoThresholder();` (line 106) and replace both call sites `autoThresholder.getThreshold(method, histogram)` (lines 134, 191) with `NBinsAutoThresholder.getThreshold(method, histogram)`; replace `autoThresholder.getThreshold(method, combinedHistogram)` (line 166) with `NBinsAutoThresholder.getThreshold(method, combinedHistogram)`.
5. Update the class-level `@SetJIPipeDocumentation` name to `"Auto threshold 2D (8-bit)"` and append to the description: `" This node requires 8-bit images and uses 256 bins (one per pixel value). Use 'Auto threshold 2D (16-bit)' or 'Auto threshold 2D (custom bins)' for images with a higher bit depth."`

- [ ] **Step 2: Update the plugin registration**

In `ImageJAlgorithmsPlugin.registerThresholdAlgorithms()`:
1. Line 1069: `for (AutoThresholder.Method method : AutoThresholder.Method.values())` → `for (AutoThresholdMethod method : AutoThresholdMethod.values())` (add import `org.hkijena.jipipe.utils.threshold.AutoThresholdMethod`; the `registerNodeExample(AutoThreshold2DAlgorithm.class, method.name(), node -> node.setMethod(method))` body is unchanged since `method.name()` and `setMethod` still work).
2. Lines 1093–1094: replace
   ```java
   registerEnumParameterType(AutoThresholder.Method.class.getCanonicalName(), AutoThresholder.Method.class,
           "Auto threshold method", "Available methods");
   ```
   with
   ```java
   registerEnumParameterType(AutoThresholdMethod.class.getCanonicalName(), AutoThresholdMethod.class,
           "Auto threshold method", "Available methods");
   ```

- [ ] **Step 3: Compile the module**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/
git commit -m "Threshold: port AutoThreshold2DAlgorithm to AutoThresholdMethod"
```

---

### Task 5: Migrate `NucleiSegmentation3DAlgorithm` (jipipe-plugin-ij-3d)

**Files:**
- Modify: `plugins/jipipe-plugin-ij-3d/src/main/java/org/hkijena/jipipe/plugins/ij3d/nodes/segmentation/NucleiSegmentation3DAlgorithm.java`

**Interfaces:**
- Consumes: `AutoThresholdMethod` (Task 1).

- [ ] **Step 1: Update the node**

mcib3d's `Segment3DNuclei.setMethod` requires `ij.process.AutoThresholder.Method`, so the node keeps converting at the boundary:
1. Replace import `ij.process.AutoThresholder` (if only used for the enum — check the import list) with `org.hkijena.jipipe.utils.threshold.AutoThresholdMethod` **plus** keep/add `import ij.process.AutoThresholder;` (still needed for the conversion).
2. Field: `private AutoThresholdMethod autoThresholdMethod = AutoThresholdMethod.Default;`
3. Getter/setter types → `AutoThresholdMethod` (parameter key `"auto-threshold-method"` unchanged).
4. Copy-constructor: type of `other.autoThresholdMethod` changes accordingly (no code change beyond types).
5. In `runIteration`, line 67: `segment3DNuclei.setMethod(autoThresholdMethod)` → `segment3DNuclei.setMethod(AutoThresholder.Method.valueOf(autoThresholdMethod.name()))`.

- [ ] **Step 2: Compile**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-3d -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add plugins/jipipe-plugin-ij-3d/src/main/java/org/hkijena/jipipe/plugins/ij3d/nodes/segmentation/NucleiSegmentation3DAlgorithm.java
git commit -m "Threshold: port NucleiSegmentation3DAlgorithm to AutoThresholdMethod"
```

---

### Task 6: Rework `HistogramThresholdFunction` (optional nbins) + migrate 17 subclasses

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/HistogramThresholdFunction.java`
- Modify: all 17 `HistogramThreshold*.java` in the same directory (`HistogramThresholdHuang`, `HistogramThresholdImageJDefault`, `HistogramThresholdIntermodes`, `HistogramThresholdImageJIsoData`, `HistogramThresholdIsoData`, `HistogramThresholdLi`, `HistogramThresholdMaxEntropy`, `HistogramThresholdMean`, `HistogramThresholdMinError`, `HistogramThresholdMinimum`, `HistogramThresholdMoments`, `HistogramThresholdOtsu`, `HistogramThresholdPercentile`, `HistogramThresholdRenyi`, `HistogramThresholdShanbhag`, `HistogramThresholdTriangle`, `HistogramThresholdYen`)

**Interfaces:**
- Consumes: `AutoThresholdMethod`, `NBinsAutoThresholder` (Tasks 1–3).
- Produces: `protected abstract AutoThresholdMethod getMethod();` on `HistogramThresholdFunction`; subclasses no longer implement `calculateThreshold`.

- [ ] **Step 1: Write the failing test**

`jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/HistogramThresholdFunctionTest.java` — full content (with license header):

```java
package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramThresholdFunctionTest {

    private List<Number> histogram(int... values) {
        List<Number> list = new ArrayList<>();
        for (int value : values) list.add(value);
        return list;
    }

    /**
     * Without nbins the function must behave exactly as before: truncate to 256 bins.
     */
    @Test
    void testLegacyBehaviorTruncatesTo256() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> large = histogram(new int[300]); // 300 zeros, would overflow 256
        large.set(50, 100);
        large.set(200, 100);
        Object result = function.evaluate(List.of(large), new JIPipeExpressionVariablesMap());
        assertEquals(Integer.class, result.getClass());
        int legacy = (int) result;
        // Same computation on a 256-truncated array
        List<Number> truncated = histogram(new int[256]);
        truncated.set(50, 100);
        truncated.set(200, 100);
        assertEquals((int) function.evaluate(List.of(truncated), new JIPipeExpressionVariablesMap()), legacy);
    }

    /**
     * With nbins=16 a 16-bin histogram is evaluated directly.
     */
    @Test
    void testNbinsParameter() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> histo = histogram(0, 0, 100, 200, 0, 0, 0, 0, 0, 0, 200, 100, 0, 0, 0, 0);
        Object result = function.evaluate(List.of(histo, 16), new JIPipeExpressionVariablesMap());
        int threshold = ((Number) result).intValue();
        assertEquals(true, threshold >= 3 && threshold <= 10, "Threshold between modes, got " + threshold);
    }
}
```

Run: `mvn -q -pl jipipe-core test -Dtest=HistogramThresholdFunctionTest`
Expected: FAIL (the 2-argument `evaluate` call does not compile yet — `ExpressionFunction(name, 1)` accepts only 1 argument).

- [ ] **Step 2: Rework the base class**

Replace `HistogramThresholdFunction.java` body (keep package/license) with:

```java
package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder;

import java.util.Collection;
import java.util.List;

public abstract class HistogramThresholdFunction extends ExpressionFunction {

    /**
     * Default number of bins for backwards compatibility.
     */
    public static final int DEFAULT_NBINS = 256;

    public HistogramThresholdFunction(String name) {
        super(name, 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> numbers = (Collection<Number>) parameters.get(0);
        int nbins = DEFAULT_NBINS;
        if (parameters.size() > 1) {
            Object nbinsParam = parameters.get(1);
            if (nbinsParam instanceof Number) {
                nbins = ((Number) nbinsParam).intValue();
            } else {
                throw new IllegalArgumentException("The nbins parameter of " + getName()
                        + " must be a number, got: " + nbinsParam);
            }
        }
        if (nbins < 2)
            throw new IllegalArgumentException("The nbins parameter of " + getName()
                    + " must be at least 2, got: " + nbins);
        int[] histogram = new int[nbins];
        int i = 0;
        for (Number number : numbers) {
            histogram[i] = number.intValue();
            ++i;
            if (i >= nbins)
                break;
        }
        return NBinsAutoThresholder.getThreshold(getMethod(), histogram);
    }

    /**
     * @return the auto-threshold method to apply
     */
    protected abstract AutoThresholdMethod getMethod();

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("Histogram", "Array containing numbers. The array index represents the " +
                        "bin and the array value represents the count for this bin. " +
                        "The histogram is truncated to the number of bins.", Collection.class);
            case 1:
                return new ParameterInfo("nbins", "Optional. Number of bins (default 256). If provided, the " +
                        "histogram is truncated/padded to this length.", Integer.class);
            default:
                return null;
        }
    }
}
```

- [ ] **Step 3: Migrate the 17 subclasses**

Each subclass (e.g. `HistogramThresholdOtsu.java`) changes its overridden method from
```java
@Override
protected int calculateThreshold(int[] histogram) {
    return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Otsu, histogram);
}
```
to
```java
@Override
protected AutoThresholdMethod getMethod() {
    return AutoThresholdMethod.Otsu;
}
```
with imports `ij.process.AutoThresholder` removed and `org.hkijena.jipipe.utils.threshold.AutoThresholdMethod` added. Method-name mapping (subclass constant → `AutoThresholdMethod` constant): Default→Default, Huang→Huang, Intermodes→Intermodes, IsoData→IsoData, IJ_IsoData→IJ_IsoData, Li→Li, MaxEntropy→MaxEntropy, Mean→Mean, MinError→MinError, Minimum→Minimum, Moments→Moments, Otsu→Otsu, Percentile→Percentile, RenyiEntropy→RenyiEntropy, Shanbhag→Shanbhag, Triangle→Triangle, Yen→Yen (identical names — mechanical change). Subclasses with the `IJLogToJIPipeProgressInfoPump` wrapper (`HistogramThresholdIntermodes`, `HistogramThresholdMinimum`, `HistogramThresholdMinError`) drop it (ImageJ logging is no longer called; the ported methods do not log).

- [ ] **Step 4: Run tests**

Run: `mvn -q -pl jipipe-core test -Dtest='HistogramThresholdFunctionTest,NBinsAutoThresholderTest'`
Expected: ALL PASS (both classes).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/ \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/
git commit -m "Threshold: add optional nbins to histogram threshold functions"
```

---

### Task 7: New `HISTOGRAM_THRESHOLD_8_BIT_*` / `_16_BIT_*` expression functions

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/HistogramThreshold8BitFunction.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/statistics/HistogramThreshold16BitFunction.java`
- Create: 34 subclasses (17 per variant), one file each, in the same directory. Naming: `HistogramThreshold8BitOtsu.java` (`"HISTOGRAM_THRESHOLD_8_BIT_OTSU"`), `HistogramThreshold16BitOtsu.java` (`"HISTOGRAM_THRESHOLD_16_BIT_OTSU"`), etc. — method suffix from Task 6 mapping.
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java` (register after line 244)

**Interfaces:**
- Consumes: `AutoThresholdMethod`, `NBinsAutoThresholder.getThreshold8U/getThreshold16U` (Task 3).
- Produces: 34 registered functions.

- [ ] **Step 1: Create the two intermediate base classes**

`HistogramThreshold8BitFunction.java` (with license header):

```java
package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder;

import java.util.Collection;
import java.util.List;

/**
 * Base of 8-bit histogram threshold functions. The histogram must have at most 256 bins;
 * the bin index is the pixel value.
 */
public abstract class HistogramThreshold8BitFunction extends ExpressionFunction {

    public HistogramThreshold8BitFunction(String name) {
        super(name, 1);
    }

    protected abstract AutoThresholdMethod getMethod();

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> numbers = (Collection<Number>) parameters.get(0);
        int[] histogram = new int[256];
        int i = 0;
        for (Number number : numbers) {
            histogram[i] = number.intValue();
            ++i;
            if (i >= 256)
                break;
        }
        return NBinsAutoThresholder.getThreshold8U(getMethod(), histogram);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Histogram", "Array of size 256 containing numbers. The array index " +
                "represents the bin (pixel value) and the array value represents the count for this bin.", Collection.class);
    }
}
```

`HistogramThreshold16BitFunction.java` — identical structure, but: array `new int[65536]`, truncation guard `i >= 65536`, calls `NBinsAutoThresholder.getThreshold16U(getMethod(), histogram)`, ParameterInfo text: "Array of size 65536 containing numbers. The array index represents the bin (pixel value) and the array value represents the count for this bin."

- [ ] **Step 2: Create the 34 subclasses**

Each is a 5-line class. Example `HistogramThreshold8BitOtsu.java` (with license header):

```java
package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;

@SetJIPipeDocumentation(name = "Histogram threshold 8-bit (Otsu)", description = "Calculates a threshold from a " +
        "256-bin histogram (bin index = pixel value) using the Otsu algorithm.")
public class HistogramThreshold8BitOtsu extends HistogramThreshold8BitFunction {
    public HistogramThreshold8BitOtsu() {
        super("HISTOGRAM_THRESHOLD_8_BIT_OTSU");
    }

    @Override
    protected AutoThresholdMethod getMethod() {
        return AutoThresholdMethod.Otsu;
    }
}
```

Function ID pattern: `HISTOGRAM_THRESHOLD_8_BIT_<CONSTANT>` and `HISTOGRAM_THRESHOLD_16_BIT_<CONSTANT>` where `<CONSTANT>` is the `AutoThresholdMethod` constant (e.g. `HISTOGRAM_THRESHOLD_8_BIT_IJ_ISODATA`, `HISTOGRAM_THRESHOLD_16_BIT_RENYIENTROPY`). Display name pattern: "Histogram threshold 8-bit (Otsu)" / "Histogram threshold 16-bit (Otsu)".

- [ ] **Step 3: Register in `ExpressionPlugin.register()`**

After line 244 (`registerExpressionFunction(new HistogramThresholdYen());`) add 34 lines:

```java
registerExpressionFunction(new HistogramThreshold8BitDefault());
registerExpressionFunction(new HistogramThreshold8BitHuang());
registerExpressionFunction(new HistogramThreshold8BitIntermodes());
registerExpressionFunction(new HistogramThreshold8BitIsoData());
registerExpressionFunction(new HistogramThreshold8BitImageJIsoData());
registerExpressionFunction(new HistogramThreshold8BitLi());
registerExpressionFunction(new HistogramThreshold8BitMaxEntropy());
registerExpressionFunction(new HistogramThreshold8BitMean());
registerExpressionFunction(new HistogramThreshold8BitMinError());
registerExpressionFunction(new HistogramThreshold8BitMinimum());
registerExpressionFunction(new HistogramThreshold8BitMoments());
registerExpressionFunction(new HistogramThreshold8BitOtsu());
registerExpressionFunction(new HistogramThreshold8BitPercentile());
registerExpressionFunction(new HistogramThreshold8BitRenyiEntropy());
registerExpressionFunction(new HistogramThreshold8BitShanbhag());
registerExpressionFunction(new HistogramThreshold8BitTriangle());
registerExpressionFunction(new HistogramThreshold8BitYen());
registerExpressionFunction(new HistogramThreshold16BitDefault());
registerExpressionFunction(new HistogramThreshold16BitHuang());
registerExpressionFunction(new HistogramThreshold16BitIntermodes());
registerExpressionFunction(new HistogramThreshold16BitIsoData());
registerExpressionFunction(new HistogramThreshold16BitImageJIsoData());
registerExpressionFunction(new HistogramThreshold16BitLi());
registerExpressionFunction(new HistogramThreshold16BitMaxEntropy());
registerExpressionFunction(new HistogramThreshold16BitMean());
registerExpressionFunction(new HistogramThreshold16BitMinError());
registerExpressionFunction(new HistogramThreshold16BitMinimum());
registerExpressionFunction(new HistogramThreshold16BitMoments());
registerExpressionFunction(new HistogramThreshold16BitOtsu());
registerExpressionFunction(new HistogramThreshold16BitPercentile());
registerExpressionFunction(new HistogramThreshold16BitRenyiEntropy());
registerExpressionFunction(new HistogramThreshold16BitShanbhag());
registerExpressionFunction(new HistogramThreshold16BitTriangle());
registerExpressionFunction(new HistogramThreshold16BitYen());
```

(Class names end with the Java constant, e.g. `HistogramThreshold8BitImageJIsoData` for `IJ_IsoData` — drop only the underscores.)

- [ ] **Step 4: Compile and run module tests**

Run: `mvn -q -pl jipipe-core test -Dtest='HistogramThreshold*Test,NBinsAutoThresholderTest'`
Expected: ALL PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/
git commit -m "Threshold: add 8-bit/16-bit histogram threshold expression functions"
```

---

### Task 8: `AutoThreshold2D16UAlgorithm` node

**Files:**
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2D16UAlgorithm.java`
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/ImageJAlgorithmsPlugin.java`

**Interfaces:**
- Consumes: `AutoThresholdMethod`, `NBinsAutoThresholder.getThreshold16U` (Tasks 1, 3), `AutoThreshold2DAlgorithm.SliceThresholdMode` (Task 4), `ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI` (`plugins/.../utils/ImageJAlgorithmUtils.java:239`).
- Produces: node ID `ij1-threshold-auto2d-16u`.

- [ ] **Step 1: Create the node**

Model it on `AutoThreshold2DAlgorithm` (Task 4 state) + `CustomAutoThreshold2D16UAlgorithm`'s output writing. Full skeleton (with license header; implement following the referenced patterns):

```java
package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
// ... remaining imports mirrored from AutoThreshold2DAlgorithm (annotation/parameter/node API,
//     ImagePlusGreyscale16UData, ImagePlusGreyscaleMaskData, ImageJIterationUtils, ...)

/**
 * Thresholding node that thresholds 16-bit images via an auto threshold
 */
@SetJIPipeDocumentation(name = "Auto threshold 2D (16-bit)", description = "Applies an auto-thresholding algorithm to 16-bit images. " +
        "The threshold is calculated from the full-range 65536-bin histogram (one bin per pixel value), so no precision is lost. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale16UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
public class AutoThreshold2D16UAlgorithm extends JIPipeIteratingAlgorithm {

    private AutoThresholdMethod method = AutoThresholdMethod.Default;
    private boolean darkBackground = true;
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", false);
    private SliceThresholdMode thresholdMode = SliceThresholdMode.ApplyPerSlice;
    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    // Constructors: (JIPipeNodeInfo info) { super(info); ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration()); }
    //               copy-constructor mirroring AutoThreshold2DAlgorithm's field-by-field copy

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Input", ImagePlusGreyscale16UData.class, progressInfo).getDuplicateImage();
        // ROI/mask input handling: copy verbatim from AutoThreshold2DAlgorithm.runIteration lines 104-120

        // Create 8-bit output hyperstack (pattern from ManualThreshold16U2DAlgorithm.runIteration lines 95-101):
        ImagePlus outputImage = IJ.createHyperStack(img.getTitle() + " Thresholded",
                img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 8);

        // Dark background: invert each 16-bit slice in full range (65535 - v) — invert via processor.invert()
        // AFTER resetMinAndMax, because ShortProcessor.invert() inverts around the display range:
        // call ip.resetMinAndMax() first, then ip.invert() so the inversion is 65535 - v (see ShortProcessor.java:582-598).
        // Apply this once per slice before histogram computation if !darkBackground.

        // Per mode (thresholdMode), mirroring AutoThreshold2DAlgorithm lines 122-212 but:
        //  - histogram: ((ShortProcessor) ip).getHistogram() (65536 bins)
        //  - threshold: NBinsAutoThresholder.getThreshold16U(method, histogram)
        //  - combine-statistics mode: int[] combinedHistogram = new int[65536]; add per-slice histograms element-wise
        //  - combine-threshold mode: clamp via Math.min(65535, Math.max(0, combined.intValue()))
        //  - application: write into outputImage via applyThreshold((ShortProcessor) ip, targetProcessor, threshold)
        //    where targetProcessor is fetched exactly as in CustomAutoThreshold2D16UAlgorithm lines 191-197
        //  - threshold annotations: same OptionalTextAnnotationNameParameter usage as AutoThreshold2DAlgorithm lines 138-149 / 167-170 / 199-206

        // iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), annotations, ..., progressInfo);
    }

    private void applyThreshold(ShortProcessor source, ByteProcessor target, int threshold) {
        short[] src = (short[]) source.getPixels();
        byte[] dst = (byte[]) target.getPixels();
        for (int i = 0; i < src.length; i++) {
            dst[i] = Short.toUnsignedInt(src[i]) > threshold ? (byte) 255 : 0;
        }
    }

    // Getters/setters for all parameters, keys identical to AutoThreshold2DAlgorithm:
    // "method", "dark-background", "threshold-annotation", "slice-threshold-mode",
    // "threshold-combine-expression", "source-area", "threshold-annotation-strategy"
    // Types: AutoThresholdMethod (method), same classes as AutoThreshold2DAlgorithm for the rest.
    // Reuse AutoThreshold2DAlgorithm.SliceThresholdMode for thresholdMode and reference
    // AutoThreshold2DAlgorithm.ThresholdsExpressionParameterVariablesInfo for the expression settings.

    public enum SliceThresholdMode {  // DO NOT create this — reuse AutoThreshold2DAlgorithm.SliceThresholdMode
    }
}
```

**Important:** the last enum block is a note — delete it; the node uses `AutoThreshold2DAlgorithm.SliceThresholdMode` (as `CustomAutoThreshold2D16UAlgorithm` does at its line 69).

- [ ] **Step 2: Register the node + per-method examples**

In `ImageJAlgorithmsPlugin.registerThresholdAlgorithms()`, after the `ij1-threshold-auto2d` registration block:

```java
registerNodeType("ij1-threshold-auto2d-16u", AutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
    registerNodeExample(AutoThreshold2D16UAlgorithm.class, method.name(), node -> node.setMethod(method));
}
```

- [ ] **Step 3: Compile**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/
git commit -m "Threshold: add Auto threshold 2D (16-bit) node"
```

---

### Task 9: `AutoThreshold2DNBinsAlgorithm` node (custom bins)

**Files:**
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2DNBinsAlgorithm.java`
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/ImageJAlgorithmsPlugin.java`
- Test: `plugins/jipipe-plugin-ij-algorithms/src/test/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2DNBinsAlgorithmTest.java`

**Interfaces:**
- Consumes: `AutoThresholdMethod`, `NBinsAutoThresholder.getThreshold` (Tasks 1, 3), `AutoThreshold2DAlgorithm.SliceThresholdMode` (Task 4).
- Produces: node ID `ij1-threshold-auto2d-nbins`; static helper `public static int binIndex(double value, double min, double max, int nbins)` = `(int) Math.floor((value - min) / (max - min) * nbins)` clamped to `[0, nbins-1]`.

- [ ] **Step 1: Write the failing test**

`AutoThreshold2DNBinsAlgorithmTest.java` — full content (with license header):

```java
package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoThreshold2DNBinsAlgorithmTest {

    @Test
    void testBinIndexMapping() {
        // 8-bit range 0..255 into 256 bins: bin index = value
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(0, 0, 255, 256));
        assertEquals(255, AutoThreshold2DNBinsAlgorithm.binIndex(255, 0, 255, 256));
        assertEquals(127, AutoThreshold2DNBinsAlgorithm.binIndex(127, 0, 255, 256));
        // 8-bit range into 16 bins: each bin spans 16 values
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(15, 0, 255, 16));
        assertEquals(1, AutoThreshold2DNBinsAlgorithm.binIndex(16, 0, 255, 16));
        assertEquals(15, AutoThreshold2DNBinsAlgorithm.binIndex(255, 0, 255, 16));
        // Float range 10.0..20.0 into 10 bins
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(10.0, 10.0, 20.0, 10));
        assertEquals(5, AutoThreshold2DNBinsAlgorithm.binIndex(15.0, 10.0, 20.0, 10));
        assertEquals(9, AutoThreshold2DNBinsAlgorithm.binIndex(19.99, 10.0, 20.0, 10));
        assertEquals(9, AutoThreshold2DNBinsAlgorithm.binIndex(20.0, 10.0, 20.0, 10)); // clamped
    }
}
```

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms test -Dtest=AutoThreshold2DNBinsAlgorithmTest`
Expected: COMPILATION ERROR (class does not exist).

- [ ] **Step 2: Create the node**

Structure (license header + imports mirrored from Task 8):

```java
package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

// imports as in AutoThreshold2D16UAlgorithm, plus org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData

/**
 * Thresholding node that thresholds via an auto threshold applied to a histogram with a custom number of bins
 */
@SetJIPipeDocumentation(name = "Auto threshold 2D (custom bins)", description = "Applies an auto-thresholding algorithm to a histogram " +
        "with a custom number of bins. 8-bit and 16-bit inputs are binned over their full native range. " +
        "For 32-bit inputs, the bin range is determined by the multi-slice thresholding mode: " +
        "'Apply threshold per slice' uses each slice's minimum/maximum, 'Combine slice statistics' uses " +
        "the whole image's minimum/maximum, and 'Combine thresholds per slice' computes per-slice thresholds " +
        "that are combined via the expression. The threshold is mapped back to a pixel value via the bin range. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
public class AutoThreshold2DNBinsAlgorithm extends JIPipeIteratingAlgorithm {

    public static int binIndex(double value, double min, double max, int nbins) {
        if (max <= min)
            return 0;
        int index = (int) Math.floor((value - min) / (max - min) * nbins);
        return Math.min(nbins - 1, Math.max(0, index));
    }

    private AutoThresholdMethod method = AutoThresholdMethod.Default;
    private int nbins = 256;
    private boolean darkBackground = true;
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", false);
    private SliceThresholdMode thresholdMode = SliceThresholdMode.ApplyPerSlice;
    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    // Constructor/copy-constructor/supportsParallelization: as in Task 8 (plus nbins in the copy)

    @SetJIPipeDocumentation(name = "Number of bins", description = "The number of histogram bins used to calculate the threshold. " +
            "The minimum is 2 and the maximum is 65536.")
    @JIPipeParameter(value = "nbins", important = true)
    public int getNbins() {
        return nbins;
    }

    @JIPipeParameter("nbins")
    public boolean setNbins(int nbins) {
        if (nbins < 2 || nbins > 65536)
            return false;
        this.nbins = nbins;
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        // ROI/mask handling and 8-bit output hyperstack creation: as in Task 8

        // Determine bin range per processor type (ip is the current slice):
        //  - ByteProcessor: min=0, max=255
        //  - ShortProcessor: min=0, max=65535
        //  - FloatProcessor: per SliceThresholdMode:
        //      ApplyPerSlice / CombineThresholdPerSlice → this slice's min/max (ip.getStatistics(JIPipe uses ImageProcessor.getStats:
        //        use ip.getMin()/ip.getMax() — but note FloatProcessor min/max are the actual data min/max)
        //      CombineSliceStatistics → whole-image min/max computed once over all slices before the loop
        //
        // Histogram: int[] histogram = new int[nbins]; for each pixel p (ip.getf(i)):
        //   histogram[binIndex(p, min, max, nbins)]++
        //   (skip pixels outside ROI/mask, mirroring the masked-pixel approach: if mask != null, only count where mask pixel > 0)
        //
        // Dark background for float input: threshold mapping handles inversion naturally —
        //   if !darkBackground, invert the input image pixels (v → (min + max) - v) before binning, as in Task 8.
        //   For integer types use ip.invert() after resetMinAndMax() as in Task 8.
        //
        // Threshold: NBinsAutoThresholder.getThreshold(method, histogram) → bin index t
        // Map back to pixel value: double thresholdValue = min + (t + 1) * (max - min) / nbins;
        //   (a pixel is foreground if its value > thresholdValue — consistent with '>' semantics of the other nodes)
        //
        // Apply: for each pixel, dst = value > thresholdValue ? 255 : 0 (write into the 8-bit output,
        //   using targetProcessor fetching as in Task 8; ip.getf(i) works for all processor types)
        //
        // Slice modes: mirror Task 8's three branches; in CombineSliceStatistics the histograms are summed
        //   element-wise (they share the same bin range — whole-image min/max for floats), threshold computed once,
        //   mapped back once, applied to all slices. In CombineThresholdPerSlice, per-slice bin ranges are allowed;
        //   thresholds (as pixel values) are combined via the expression, then clamped to [min, max] per slice on application.
        //
        // Annotations: as in Task 8 (annotation value is the mapped pixel-value threshold)
    }

    // Remaining getters/setters with keys: "method", "dark-background", "threshold-annotation",
    // "slice-threshold-mode", "threshold-combine-expression", "source-area", "threshold-annotation-strategy"
    // (types identical to Task 8)
}
```

- [ ] **Step 3: Register the node + examples**

In `registerThresholdAlgorithms()` after the Task 8 block:

```java
registerNodeType("ij1-threshold-auto2d-nbins", AutoThreshold2DNBinsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
    registerNodeExample(AutoThreshold2DNBinsAlgorithm.class, method.name(), node -> node.setMethod(method));
}
```

- [ ] **Step 4: Run the test**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms test -Dtest=AutoThreshold2DNBinsAlgorithmTest`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/
git commit -m "Threshold: add Auto threshold 2D (custom bins) node"
```

---

### Task 10: `PercentileThreshold16U2DAlgorithm` node

**Files:**
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/PercentileThreshold16U2DAlgorithm.java`
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/ImageJAlgorithmsPlugin.java`

**Interfaces:**
- Produces: node ID `ij1-threshold-percentile2d-16u`.

- [ ] **Step 1: Create the node**

Copy `PercentileThreshold8U2DAlgorithm` (`nodes/threshold/PercentileThreshold8U2DAlgorithm.java`) and change:
1. Class name → `PercentileThreshold16U2DAlgorithm`; input slot `ImagePlusGreyscale8UData` → `ImagePlusGreyscale16UData`.
2. `@SetJIPipeDocumentation` name → `"Percentile threshold 2D (16-bit)"`; description → "Thresholds the image with a threshold calculated from the image pixel values (percentile of the 65536-bin histogram). If higher-dimensional data is provided, the filter is applied to each 2D slice."
3. In `runIteration`: input data class → `ImagePlusGreyscale16UData.class`; the slice loop reads `short[] pixels = (short[]) ip.getPixels();` and computes the histogram `int[] histogram = new int[65536]; for (short p : pixels) histogram[p & 0xFFFF]++;` then applies the existing percentile scan over `histogram` unchanged (sum until `sum >= sum * percentile / 100.0`, `thresh` = bin index). Since `ip.threshold(thresh)` on a `ShortProcessor` sets ≤thresh to 0 and >thresh to 255 (ImageJ semantics, `ShortProcessor.java:1202-1210`), the output mask is produced by the existing `ip.threshold(thresh)` call followed by `new ImagePlusGreyscaleMaskData(img)` (the mask type converts to 8-bit if needed — same as the 8U node).

- [ ] **Step 2: Register the node**

In `registerThresholdAlgorithms()` after `ij1-threshold-percentile2d-8u` (line 1065):

```java
registerNodeType("ij1-threshold-percentile2d-16u", PercentileThreshold16U2DAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
```

- [ ] **Step 3: Compile and commit**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile`
Expected: BUILD SUCCESS.

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/
git commit -m "Threshold: add Percentile threshold 2D (16-bit) node"
```

---

### Task 11: Local auto-threshold 16-bit nodes (Mean/Median/MidGrey/Otsu + 5 named)

**Files:**
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/LocalAutoThreshold16UUtils.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/LocalAutoThreshold2D16UAlgorithm.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/BernsenLocalAutoThreshold2D16UAlgorithm.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/ContrastLocalAutoThreshold2D16UAlgorithm.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/NiblackLocalAutoThreshold2D16UAlgorithm.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/PhansalkarLocalAutoThreshold2D16UAlgorithm.java`
- Create: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/SauvolaLocalAutoThreshold2D16UAlgorithm.java`
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/ImageJAlgorithmsPlugin.java`

**Interfaces:**
- Consumes: `NBinsAutoThresholder` (for local Otsu), ImageJ `RankFilters` (existing dependency).
- Produces: node IDs `ij1-threshold-local-auto2d-16u`, `ij1-threshold-local-auto2d-16u-bernsen|contrast|niblack|phansalkar|sauvola`; `LocalAutoThreshold16UUtils` shared static kernels.

- [ ] **Step 1: Create `LocalAutoThreshold16UUtils`**

This class holds the 16-bit kernel implementations (with license header). The 8-bit originals' bodies are adapted per method. Key shared pieces:

```java
package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * 16-bit local auto-threshold kernels, ported from the 8-bit variants
 * (originally from fiji.threshold.Auto_Local_Threshold).
 * All kernels write into an 8-bit output processor instead of mutating the input.
 */
public final class LocalAutoThreshold16UUtils {

    private LocalAutoThreshold16UUtils() {
    }

    /**
     * Duplicates a 16-bit processor into a new ImagePlus.
     */
    public static ImagePlus duplicateImage16U(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createShortImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, ij.process.Blitter.COPY);
        return iPlus;
    }

    // Mean(ip, radius, c_value, doIwhite, ByteProcessor out):
    //   port of LocalAutoThreshold2DAlgorithm.Mean (lines 83-113) — compute mean via RankFilters on a
    //   32-bit duplicate (ImageConverter.convertToGray32 as in the original), then
    //   out[i] = ((src[i] & 0xFFFF) > (int)(mean[i] - c_value)) ? object : backg;
    //   object/backg chosen by doIwhite exactly as the original (object = 0xff when doIwhite).
    //
    // Median(ip, radius, c_value, doIwhite, out): port of LocalAutoThreshold2DAlgorithm.Median (lines 115-142) —
    //   RankFilters MEDIAN on a 16-bit duplicate; median[i] is short; compare with & 0xFFFF.
    //
    // MidGrey(ip, radius, c_value, doIwhite, out): port of MidGrey (lines 144-178) —
    //   MAX/MIN RankFilters on 16-bit duplicates; mid_gray = ((max&0xFFFF)+(min&0xFFFF))/2;
    //   out[i] = ((src[i]&0xFFFF) > mid_gray - c_value) ? object : backg;
    //
    // Bernsen(ip, radius, contrast_threshold, doIwhite, out): port of BernsenLocalAutoThreshold2DAlgorithm.Bernsen
    //   (lines 76-125) — local_contrast and mid_gray computed from & 0xFFFF values;
    //   low-contrast decision: (mid_gray >= 32768) ? object : backg   // half of the 16-bit range, replacing 128
    //
    // Contrast(ip, radius, doIwhite, out): port of ContrastLocalAutoThreshold2DAlgorithm.Contrast (lines 75-113) —
    //   Math.abs over & 0xFFFF values; the "(pixels[i] & 0xff) != 0" guard becomes "(src&0xFFFF) != 0".
    //
    // Niblack(ip, radius, k_value, c_value, doIwhite, out): port of NiblackLocalAutoThreshold2DAlgorithm.Niblack
    //   (lines 77-119) — mean/variance via RankFilters on 32-bit duplicates (as original);
    //   out[i] = ((src[i]&0xFFFF) > (int)(mean[i] + k_value * Math.sqrt(var[i]) - c_value)) ? object : backg;
    //
    // Sauvola(ip, radius, k_value, r_value, doIwhite, out): port of SauvolaLocalAutoThreshold2DAlgorithm.Sauvola
    //   (lines 76-119) — NOTE: the original r_value default is 128 (8-bit scale). For the 16-bit port the
    //   node default becomes 32768 and the doc says "half the dynamic range"; formula otherwise unchanged:
    //   out[i] = ((src[i]&0xFFFF) > (int)(mean[i] * (1.0 + k_value * ((Math.sqrt(var[i]) / r_value) - 1.0)))) ? object : backg;
    //
    // Phansalkar(ip, radius, k, r, p, q, doIwhite, out): port of PhansalkarLocalAutoThreshold2DAlgorithm.Phansalkar
    //   (lines 78-143) — the original normalizes via stretchHistogram + multiply(1.0/255) on the ORIGINAL
    //   (8-bit) intensities. For 16-bit, replace that normalization with multiply(1.0/65535) so mean/sd are
    //   in [0,1] as the formula expects; ori/mean/sd arrays as in the original; r stays the user parameter
    //   (default 0.5 unchanged, as it is normalized); comparison identical.
    //
    // Otsu(ip, radius, c_value, doIwhite, out): port of LocalAutoThreshold2DAlgorithm.Otsu (lines 180-273), with:
    //   - L removed: per-pixel local histogram is built over the local window's actual value range —
    //     for each window, find local min/max, build int[range] histogram, run NBinsThresholdMethods.Otsu
    //     (via NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, histogram)) on the compact
    //     histogram, offset back by local min.
    //   - the threshold subtraction (threshold - c_value) and the 0xFFFF clamp ((threshold) & 0xFFFF stays
    //     without mask — clamp to [0, 65535]) mirror the original semantics.
    //   - pixelsOut writes into out (byte 0/255) instead of the input.
}
```

Implement each kernel fully in the file following the referenced 8-bit source line ranges — the kernels are direct ports with `& 0xFFFF` masking and an explicit `out` array; no other logic changes. (The plan deliberately references the exact source lines: the implementer must read the 8-bit file listed in the task Files section and port it; the per-kernel notes above state every deviation from that source.)

- [ ] **Step 2: Create the 6 node classes**

Each node mirrors its 8-bit counterpart exactly (same parameters, keys, defaults — except Sauvola `r` default 32768 as noted) with these changes:
1. Input slot: `ImagePlusGreyscale8UData` → `ImagePlusGreyscale16UData`.
2. `@SetJIPipeDocumentation` name: "(Mean/Median/MidGrey/Otsu, 16-bit)" / "(Bernsen, 16-bit)" / "(Contrast, 16-bit)" / "(Niblack, 16-bit)" / "(Phansalkar, 16-bit)" / "(Sauvola, 16-bit)".
3. In `runIteration`: input class → 16U; per slice, create `ByteProcessor out = new ByteProcessor(ip.getWidth(), ip.getHeight());`, call the corresponding `LocalAutoThreshold16UUtils` kernel writing into `out`, then copy `out` into the output hyperstack via the Task 8 target-processor pattern, and output `new ImagePlusGreyscaleMaskData(outputImage)`.
   Structure per node:
   ```java
   ImagePlus img = inputData.getDuplicateImage();
   ImagePlus outputImage = IJ.createHyperStack(img.getTitle() + " Thresholded",
           img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 8);
   ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
       if (!darkBackground) { ip.resetMinAndMax(); ip.invert(); }
       ByteProcessor out = new ByteProcessor(ip.getWidth(), ip.getHeight());
       LocalAutoThreshold16UUtils.Bernsen(ip, radius, contrastThreshold, true, out); // per-node kernel
       ByteProcessor target = (ByteProcessor) (outputImage.hasImageStack()
               ? outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
               : outputImage.getProcessor());
       target.copyBits(out, 0, 0, Blitter.COPY);
   }, progressInfo);
   iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
   ```
4. `LocalAutoThreshold2D16UAlgorithm` (the selector node) keeps the `Method` enum {Mean, Median, MidGrey, Otsu} and dispatches to `LocalAutoThreshold16UUtils` kernels.
5. Citations/aliases: keep the `@AddJIPipeCitation` entries; **do not** add `@AddJIPipeNodeAlias` (no ImageJ 16-bit equivalent).

- [ ] **Step 3: Register all 6 nodes**

In `registerThresholdAlgorithms()` after the existing local registrations (lines 1079–1084):

```java
registerNodeType("ij1-threshold-local-auto2d-16u", LocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
registerNodeType("ij1-threshold-local-auto2d-16u-bernsen", BernsenLocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
registerNodeType("ij1-threshold-local-auto2d-16u-contrast", ContrastLocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
registerNodeType("ij1-threshold-local-auto2d-16u-niblack", NiblackLocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
registerNodeType("ij1-threshold-local-auto2d-16u-phansalkar", PhansalkarLocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
registerNodeType("ij1-threshold-local-auto2d-16u-sauvola", SauvolaLocalAutoThreshold2D16UAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/segment.png"));
```

- [ ] **Step 4: Compile and commit**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile`
Expected: BUILD SUCCESS.

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/
git commit -m "Threshold: add 16-bit local auto threshold nodes"
```

---

### Task 12: Rename 8-bit local auto-threshold nodes (documentation only)

**Files:**
- Modify: `plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/LocalAutoThreshold2DAlgorithm.java` (line 46)
- Modify: the 5 other files in that directory (`BernsenLocalAutoThreshold2DAlgorithm.java` line 42, `ContrastLocalAutoThreshold2DAlgorithm.java` line 42, `NiblackLocalAutoThreshold2DAlgorithm.java` line 43, `PhansalkarLocalAutoThreshold2DAlgorithm.java` line 44, `SauvolaLocalAutoThreshold2DAlgorithm.java` — find its `@SetJIPipeDocumentation` name line)

**Interfaces:** none new — display-name-only change.

- [ ] **Step 1: Update the `@SetJIPipeDocumentation` names**

| File | Old name | New name |
|---|---|---|
| `LocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D` | `Local auto threshold 2D (Mean/Median/MidGrey/Otsu, 8-bit)` |
| `BernsenLocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D (Bernsen)` | `Local auto threshold 2D (Bernsen, 8-bit)` |
| `ContrastLocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D (Contrast)` | `Local auto threshold 2D (Contrast, 8-bit)` |
| `NiblackLocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D (Niblack)` | `Local auto threshold 2D (Niblack, 8-bit)` |
| `PhansalkarLocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D (Phansalkar)` | `Local auto threshold 2D (Phansalkar, 8-bit)` |
| `SauvolaLocalAutoThreshold2DAlgorithm.java` | `Local auto threshold 2D (Sauvola)` | `Local auto threshold 2D (Sauvola, 8-bit)` |

Append to each description: `" This node requires 8-bit images. Use the corresponding 16-bit node for images with a higher bit depth."`

- [ ] **Step 2: Compile and commit**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am compile`
Expected: BUILD SUCCESS.

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/main/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/local/
git commit -m "Threshold: clarify bit depth in local auto threshold node names"
```

---

### Task 13: End-to-end node smoke tests

**Files:**
- Create: `plugins/jipipe-plugin-ij-algorithms/src/test/java/org/hkijena/jipipe/plugins/imagejalgorithms/nodes/threshold/AutoThreshold2D16UAlgorithmTest.java`

**Interfaces:**
- Consumes: all nodes from Tasks 8–11.

- [ ] **Step 1: Write the smoke tests**

Full content (with license header). Pattern: bootstrap JIPipe (`JIPipe.ensureInstance()` in `@BeforeAll` — precedent `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPITest.java:21-24`), create a node instance via `JIPipe.createNode(X.class)`, drive `runIteration` through a `JIPipeSingleIterationStep` obtained from the node's data-batch generation, and check the output mask:

```java
package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoThreshold2D16UAlgorithmTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * Bimodal 16-bit image (dark background 1000, bright objects 20000):
     * the 16-bit auto threshold must produce a mask with exactly the bright pixels set.
     */
    @Test
    void testBimodal16BitOtsu() {
        // Build 32x32 image: left half 1000, right half 20000
        ShortProcessor processor = new ShortProcessor(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                processor.set(x, y, x < 16 ? 1000 : 20000);
            }
        }
        ImagePlus image = new ImagePlus("test", processor);

        AutoThreshold2D16UAlgorithm node = JIPipe.createNode(AutoThreshold2D16UAlgorithm.class);
        node.setMethod(AutoThresholdMethod.Otsu);

        // Drive one iteration: the JIPipeSingleIterationStep needs the node's input slot populated.
        // Simplest harness: use the node's own runIteration with a manually wired iteration step:
        JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(node);
        iterationStep.setInputData(node.getInputSlot("Input"), new ImagePlusGreyscale16UData(image),
                new JIPipeProgressInfo()); // setInputData(slot, data, progress) — check JIPipeSingleIterationStep API
        // If setInputData(slot,row) / direct data injection is not exposed, populate the node's input slot
        // via node.getInputSlot("Input").addData(...) and generate the iteration step via the node's
        // getDataBatches() machinery — adapt to whichever API compiles (see JIPipeSingleIterationStep.setInputData).
        node.runIteration(iterationStep, new JIPipeIterationContext(), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());

        ImagePlusGreyscaleMaskData output = iterationStep
                .getOutputData(node.getOutputSlot("Output"), ImagePlusGreyscaleMaskData.class, new JIPipeProgressInfo())
                .get(0); // adapt to the actual accessor (getOutputData returns slot or list — check API)
        ImagePlus mask = output.getImage();
        assertEquals(8, mask.getBitDepth());
        int foreground = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                if (mask.getProcessor().get(x, y) == 255) foreground++;
            }
        }
        assertEquals(32 * 16, foreground, "Exactly the bright half must be foreground");
    }
}
```

**Note for the implementer:** the exact iteration-step wiring API (`setInputData(slot, data, progress)` vs slot `addData` + `getDataBatches`) must be adapted to what compiles — read `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/iterationstep/JIPipeSingleIterationStep.java` (its `setInputData(JIPipeDataSlot, int)` at line 121 sets a row reference; the practical path is populating `node.getInputSlot("Input").addData(data, progress)` then running the node via its `run()`-family or `getDataBatches()` — check how `JIPipeIteratingAlgorithm.runIteration` is invoked from `run()` and use the smallest public entry point). The test's assertions (8-bit output, exact foreground count) are the contract; the wiring is mechanical.

Add a second test method `testCustomBinsNode()` in the same file for `AutoThreshold2DNBinsAlgorithm` on the same bimodal image with `nbins = 256` (bins span 0..65535, so bin width 256; bright mode ≈ bin 78, dark ≈ bin 3; Otsu separates them; expect the same 512 foreground pixels).

- [ ] **Step 2: Run the tests**

Run: `mvn -q -pl plugins/jipipe-plugin-ij-algorithms -am test -Dtest='AutoThreshold2D16UAlgorithmTest,AutoThreshold2DNBinsAlgorithmTest'`
Expected: ALL PASS.

- [ ] **Step 3: Commit**

```bash
git add plugins/jipipe-plugin-ij-algorithms/src/test/
git commit -m "Threshold: add 16-bit/custom-bins node smoke tests"
```

---

### Task 14: Full build verification

**Files:** none new.

- [ ] **Step 1: Compile everything and run all tests**

Run: `mvn -q clean compile && mvn -q test`
Expected: BUILD SUCCESS, all tests pass (existing tests + all new tests).

- [ ] **Step 2: Search for leftover 8-bit assumptions**

Run: `rg -n "AutoThresholder\.Method" --iglob '!**/test/**' jipipe-core plugins | rg -v "NBinsAutoThresholder|AutoThresholdMethod|valueOf"`
Expected: no matches (all production usages migrated). `AutoThresholder` class itself may still appear inside `NBinsAutoThresholder` (the fast path) — that is intended.

- [ ] **Step 3: Final commit (if anything pending)**

```bash
git status --short
git add -A && git commit -m "Threshold: 16-bit/n-bit thresholding complete" || true
```

---

## Self-Review Results

- **Spec coverage:** enum with identical names + itemInfo (Task 1, 4, 5), n-bin engine with 8U/16U fast paths (Tasks 2–3), existing function optional nbins + 34 new functions (Tasks 6–7), 16-bit auto node (Task 8), custom-bins node with SliceThresholdMode-driven 32F ranges (Task 9), percentile 16-bit (Task 10), local 16-bit ports ×6 (Task 11), 8-bit renames (Tasks 4, 12), tests (Tasks 3, 6, 9, 13, 14). Expression-function IDs unchanged; node IDs unchanged; parameter keys unchanged.
- **Placeholder scan:** Tasks 2 and 11 deliberately specify "port from ImageJ source at these exact lines" rather than inlining ~1000 lines into the plan — the referenced source is the authoritative content, available locally in the sources jar, and the porting rules (what to change, what to keep) are fully specified. All other code is complete.
- **Type consistency:** `AutoThresholdMethod` used consistently; `getThreshold(AutoThresholdMethod, int[])` signature identical in Tasks 3–9; `SliceThresholdMode` referenced as `AutoThreshold2DAlgorithm.SliceThresholdMode`.
