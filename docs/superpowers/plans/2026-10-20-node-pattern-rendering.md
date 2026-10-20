# Node Pattern Rendering — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every framework's canvas nodes a shared, color-neutral, plane-filling background signature (pattern) plus an abbreviation badge, so related nodes are visually distinct per framework.

**Architecture:** A pure-Java2D formula engine in `jipipe-core` (`desktop/commons/nodepatterns`). Each of 17 styles is a small `NodePatternFamily.paint(Graphics2D,w,h,Random)` that draws **white ink on transparent**, antialiased, filling the frame. A `NodePatternRenderer` renders/caches the white mask per (style, seed, size-bucket) and draws it tinted at the theme's ink+opacity. The node UI paints the mask over the body and an abbreviation chip in the title band. Settings live in the existing theme style (4 fields). No new dependencies, no contrib module, no SVG/Batik.

**Tech Stack:** Java 21, `java.awt`/`Graphics2D`, `BufferedImage`, Jackson (theme JSON), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-20-node-pattern-rendering-design.md` (read it first — it is the source of truth for the 17-family catalog, the mapping table, the 4 contract tests, and the theme setting names/defaults).

**Reference formulas:** `docs/superpowers/plans/assets/PatternForge-reference.java` — the validated prototype. The 17 family bodies are ported from its lambdas (see the per-family line ranges below). **Keep the tuned constants byte-for-byte**: the plane-filling, determinism and golden tests depend on them. `assets/node-wall-preview.png` and `assets/patterns-sheet-preview.png` are the approved visual target.

## Global Constraints

- Package for all engine code: `org.hkijena.jipipe.desktop.commons.nodepatterns`.
- All family ids are **stable string literals** (never code-derived), per the project's registered-ID rule.
- Every family draws **only** with the current paint (white) onto a transparent canvas; no color, no `Math.random`, no wall clock — a pure function of `(w, h, rng)`.
- Antialiasing ON + `STROKE_CONTROL_PURE` (set by the renderer before a family paints).
- Seeded by **framework id**: `new Random(seed.hashCode())`. Same framework ⇒ identical pattern on every node, every run.
- Theme defaults: `showNodeFrameworkPattern=true`, `nodePatternInk=white`, `nodePatternOpacity=0.15f`, `showNodeFrameworkBadge=true`. All-off ⇒ today's look.
- `NoFramework` ⇒ **null** style id ⇒ no pattern, no badge (deliberate).
- Tests are headless (`BufferedImage` only). **No Swing interaction/UI tests** (the "do not test trivial things" policy) — appearance is verified by the user in the app.
- Java 21. Build offline: `mvn -o -pl jipipe-core …`.

## Review Focus

Five input classes the spec implies but no unit test exercises — the behaviors a user would expect. Each is pinned to the task that owns the code:

1. **A framework registered with an unknown/null style id** (e.g. a plugin added later with a style we don't ship). Expected: node paints with **no pattern and no badge, no exception**. → Task 4 (registry test: every shipped framework resolves to a known id; the renderer's null-guard is verified by the engine test that an unknown id yields an empty/absent mask).
2. **`NoFramework` nodes** (the large majority of custom/legacy nodes). Expected: **no pattern, no badge** — they stay plain. → Task 4 (registry test asserts `NoFramework` style id is null) + Task 5 (paint guard).
3. **A node smaller than one mask bucket** (zoomed way out). Expected: the smallest bucket is scaled down, no crash, still tinted correctly. → Task 1 (renderer clamps to the smallest bucket; engine test renders a 16×16 mask without error).
4. **The disabled/pass-through hatch must cover the pattern** (a disabled node should read as disabled, pattern suppressed). Expected: hatch paints over the mask. → Task 5 (paint order: mask before hatch).
5. **Theme reload while the mask cache is warm** (user switches ink/opacity). Expected: the mask (theme-independent white) is reused; only the tint changes — no stale colored tile. → Task 3 (the mask cache key is (style,seed,size), never the ink; the tint is applied at draw time, so changing the theme needs no cache invalidation — asserted by an engine test that two `paint` calls with different inks over the same mask differ only in color).

---

## File Structure

Created (engine, `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/nodepatterns/`):
- `NodePatternFamily.java` — the `paint(Graphics2D,int,int,Random)` interface + its contract in the javadoc.
- `NodePatternStyles.java` — registry: id → family instance; `family(String)`, `ids()`.
- `NodePatternRenderer.java` — mask cache + `mask(...)` + `paint(...)` + `invalidateCache()`.
- 17 family classes: `StripesFamily`, `NetFamily`, `CellularFamily`, `CarpetFamily`, `HilbertFamily`, `LandscapeFamily` (Task 1); `WavesFamily`, `WeaveFamily`, `PatchworkFamily`, `LoopsFamily`, `DiscoFamily`, `TrianglesFamily`, `BlobsFamily`, `DotsFamily`, `HoneycombFamily`, `RippleFamily`, `FlowFamily` (Task 2).
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/nodepatterns/NodePatternEngineTest.java` (Task 1, extended in Task 2).
- Test resource: `jipipe-core/src/test/resources/org/hkijena/jipipe/desktop/commons/nodepatterns/golden-net-128.png` (Task 1).

Modified:
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java` (Task 3).
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopThemeEditorDocument.java` (Task 3).
- 16 framework classes in `…/api/nodes/taxonomy/frameworks/` + `…/api/nodes/taxonomy/NoFramework.java` (Task 4).
- `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/FrameworksTest.java` (Task 4).
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java` (Task 5).
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphCanvasUI.java` (Task 5 — the node context menu is assembled in `createContextMenu()`, ~line 925).

---

### Task 1: Pattern engine core + first 6 families + contract tests

**Files:**
- Create: `…/desktop/commons/nodepatterns/NodePatternFamily.java`
- Create: `…/desktop/commons/nodepatterns/NodePatternStyles.java`
- Create: `…/desktop/commons/nodepatterns/NodePatternRenderer.java`
- Create: `…/desktop/commons/nodepatterns/{StripesFamily,NetFamily,CellularFamily,CarpetFamily,HilbertFamily,LandscapeFamily}.java`
- Test: `…/test/…/desktop/commons/nodepatterns/NodePatternEngineTest.java`
- Test resource: `…/test/resources/…/nodepatterns/golden-net-128.png`

**Interfaces:**
- Produces:
  - `interface NodePatternFamily { void paint(Graphics2D g, int w, int h, Random rng); }`
  - `final class NodePatternStyles { static NodePatternFamily family(String id); static Collection<String> ids(); }`
  - `final class NodePatternRenderer { static BufferedImage mask(String styleId, String seed, int width, int height); static void paint(Graphics2D g, int x, int y, int w, int h, String styleId, String seed, Color ink, float opacity); static void invalidateCache(); }`
- Consumes: nothing from earlier tasks (this is the foundation).

The 6 families in this task are the algorithmic representatives: `stripes` (bands), `net` (graph), `cellular` (voronoi membranes), `carpet` (fractal fill), `hilbert` (space-filling curve), `landscape` (layered fill). Port each from `PatternForge-reference.java` using the line ranges below; keep constants identical.

- [ ] **Step 1: Write the failing engine test**

Create `NodePatternEngineTest.java` (no JIPipe boot needed — pure `BufferedImage`):

```java
package org.hkijena.jipipe.desktop.commons.nodepatterns;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

class NodePatternEngineTest {

    private static final String[] SIZES = { "64", "128", "200" };

    private static BufferedImage render(String style, String seed, int w, int h) {
        return NodePatternRenderer.mask(style, seed, w, h);
    }

    // 1. DETERMINISM — same (style, seed, size) ⇒ byte-identical mask, all styles, 3 sizes.
    //    invalidateCache() between the calls so the second mask() truly re-renders
    //    (otherwise the cache hands back the same object and the test proves nothing).
    @Test
    void maskIsDeterministicAcrossInvocations() throws Exception {
        for (String id : NodePatternStyles.ids()) {
            for (String s : SIZES) {
                int n = Integer.parseInt(s);
                byte[] a = toPng(NodePatternRenderer.mask(id, "seed-x", n, n));
                NodePatternRenderer.invalidateCache();
                byte[] b = toPng(NodePatternRenderer.mask(id, "seed-x", n, n));
                assertArrayEquals(a, b, "mask not deterministic for " + id + "@" + n);
            }
        }
    }

    // 1b. GOLDEN — net@128, seed = TrackMate id, pixel-exact vs the checked-in reference
    @Test
    void netMaskMatchesGoldenReference() throws Exception {
        BufferedImage live = NodePatternRenderer.mask("net", "jipipe:framework:trackmate", 128, 128);
        BufferedImage ref = ImageIO.read(NodePatternEngineTest.class.getResourceAsStream("golden-net-128.png"));
        assertNotNull(ref, "golden reference missing");
        assertSameSize(live, ref);
        for (int y = 0; y < ref.getHeight(); y++)
            for (int x = 0; x < ref.getWidth(); x++)
                assertEquals(ref.getRGB(x, y), live.getRGB(x, y), "golden pixel mismatch at " + x + "," + y);
    }

    // 2. PLANE-FILLING — ink in the 6px band of ALL four borders, >= 4%, every family, 3 sizes x 2 seeds
    @Test
    void everyFamilyFillsAllFourBorders() {
        for (String id : NodePatternStyles.ids()) {
            for (String s : SIZES) {
                int n = Integer.parseInt(s);
                for (String seed : new String[]{"seed-a", "seed-b"}) {
                    double[] c = edgeCoverage(NodePatternRenderer.mask(id, seed, n, n));
                    double min = Math.min(Math.min(c[0], c[1]), Math.min(c[2], c[3]));
                    assertTrue(min >= 0.04, id + " fails plane-filling (min edge " + min + ")");
                }
            }
        }
    }

    // 3. COLOR-NEUTRAL — every non-transparent pixel has white channels (alpha may vary)
    @Test
    void everyMaskIsWhiteInkOnly() {
        for (String id : NodePatternStyles.ids()) {
            BufferedImage m = NodePatternRenderer.mask(id, "seed-c", 128, 128);
            for (int y = 0; y < m.getHeight(); y++)
                for (int x = 0; x < m.getWidth(); x++) {
                    int argb = m.getRGB(x, y);
                    if (((argb >>> 24) & 0xFF) == 0) continue;
                    assertEquals(255, (argb >>> 16) & 0xFF, id + " non-white R at " + x + "," + y);
                    assertEquals(255, (argb >>> 8) & 0xFF, id + " non-white G");
                    assertEquals(255, argb & 0xFF, id + " non-white B");
                }
        }
    }

    // Registry: every registered id resolves to a non-null family
    @Test
    void registryResolvesEveryId() {
        assertFalse(NodePatternStyles.ids().isEmpty());
        for (String id : NodePatternStyles.ids())
            assertNotNull(NodePatternStyles.family(id), "unresolvable style " + id);
        assertNull(NodePatternStyles.family("does-not-exist"));
    }

    // Review Focus #3: tiny nodes clamp to the smallest bucket without error
    @Test
    void tinyNodeRendersWithoutError() {
        assertNotNull(NodePatternRenderer.mask("net", "seed", 16, 16));
    }

    // Review Focus #5: same mask, two inks => tinted draws differ only in color
    @Test
    void inkIsAppliedAtDrawTimeNotBakedIn() {
        BufferedImage base = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = base.createGraphics();
        NodePatternRenderer.paint(g, 0, 0, 128, 128, "net", "seed", new Color(0xFF0000), 0.5f);
        g.dispose();
        long reds = 0, blues = 0, transparent = 0, total = 128L * 128;
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            int a = base.getRGB(x, y);
            if (((a >>> 24) & 0xFF) == 0) { transparent++; continue; }
            if ((a & 0xFF) > ((a >>> 16) & 0xFF)) reds++; else blues++;
        }
        assertTrue(reds > blues, "red ink should dominate the tinted mask");
        // the tint must follow the mask's shape, not fill the whole rect
        assertTrue(transparent > total / 2,
                "tinted draw is a solid rectangle (" + transparent + "/" + total + " transparent) — tinting ignores the mask alpha");
    }

    // ---- helpers ----
    static byte[] toPng(BufferedImage img) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "png", b);
        return b.toByteArray();
    }
    static void assertSameSize(BufferedImage a, BufferedImage b) {
        assertEquals(b.getWidth(), a.getWidth());
        assertEquals(b.getHeight(), a.getHeight());
    }
    static double[] edgeCoverage(BufferedImage img) {
        int S = img.getWidth(); int edgeW = 6; double[] acc = new double[4];
        for (int i = 0; i < edgeW; i++) {
            for (int j = 0; j < S; j++) { if (ink(img.getRGB(j, i))) acc[0]++; if (ink(img.getRGB(i, j))) acc[1]++; }
            for (int j = 0; j < S; j++) { if (ink(img.getRGB(j, S - 1 - i))) acc[2]++; if (ink(img.getRGB(S - 1 - i, j))) acc[3]++; }
        }
        double n = edgeW * S;
        return new double[]{acc[0] / n, acc[1] / n, acc[2] / n, acc[3] / n};
    }
    static boolean ink(int argb) { return ((argb >>> 24) & 0xFF) > 40; }
}
```

Note: `toPng` is only called inside tests that already `throws Exception`; add `throws Exception` to `maskIsDeterministicAcrossInvocations` (adjust its signature) or wrap in try/catch. Keep it simple: declare the method `throws Exception`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -o -q -pl jipipe-core test -Dtest=NodePatternEngineTest`
Expected: COMPILE FAILURE — `NodePatternRenderer`/`NodePatternStyles`/families do not exist yet.

- [ ] **Step 3: Create the interface and registry**

`NodePatternFamily.java`:

```java
package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.awt.Graphics2D;
import java.util.Random;

/**
 * One framework pattern style: draws white ink on a transparent canvas,
 * antialiased, filling the whole frame edge-to-edge.
 * <p>
 * Contract (enforced by NodePatternEngineTest):
 * <ul>
 *   <li>draws only with the current paint (white) — no color, no alpha channel
 *       selection of its own; the renderer supplies a white pen/brush;</li>
 *   <li>reaches all four borders (overshoot the frame on grid-based geometry
 *       so the motif is clipped by the node body like an infinite texture);</li>
 *   <li>is a pure function of {@code (w, h, rng)} — no Math.random, no clock.</li>
 * </ul>
 * The renderer sets {@code KEY_ANTIALIASING=ON} and {@code KEY_STROKE_CONTROL=PURE}
 * before calling.
 */
public interface NodePatternFamily {
    void paint(Graphics2D g, int w, int h, Random rng);
}
```

`NodePatternStyles.java` — registry over the 17 ids (this task registers 6; Task 2 adds the rest). Use stable literal ids:

```java
package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of bundled node pattern styles. Ids are stable string literals. */
public final class NodePatternStyles {
    private static final Map<String, NodePatternFamily> STYLES = new LinkedHashMap<>();

    static {
        // Task 1 (algorithmic representatives)
        STYLES.put("stripes", new StripesFamily());
        STYLES.put("net", new NetFamily());
        STYLES.put("cellular", new CellularFamily());
        STYLES.put("carpet", new CarpetFamily());
        STYLES.put("hilbert", new HilbertFamily());
        STYLES.put("landscape", new LandscapeFamily());
        // Task 2 adds: waves, weave, patchwork, loops, disco, triangles, blobs,
        //              dots, honeycomb, ripple, flow
    }

    private NodePatternStyles() {}

    /** @return the family for {@code id}, or {@code null} for an unknown id */
    public static NodePatternFamily family(String id) {
        return id == null ? null : STYLES.get(id);
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableCollection(STYLES.keySet());
    }
}
```

- [ ] **Step 4: Create the renderer (mask cache + tinted draw)**

`NodePatternRenderer.java`:

```java
package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders and caches color-neutral (white-on-transparent) pattern masks and
 * draws them tinted onto nodes.
 * <p>
 * Two cache levels:
 * <ul>
 *   <li>the white MASK, keyed by (style, seed, size bucket) — theme-independent;
 *       changing ink/opacity never invalidates it;</li>
 *   <li>the TINTED copy, keyed by (mask, ink, opacity) — built once per theme
 *       look, drawn per node with a plain alpha composite.</li>
 * </ul>
 * Nodes cluster into a handful of sizes; larger nodes scale the nearest
 * bucket up via drawImage.
 */
public final class NodePatternRenderer {
    private static final Map<String, BufferedImage> MASK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> TINT_CACHE = new ConcurrentHashMap<>();
    private static final int BUCKET = 8;
    private static final int MAX = 512;

    private NodePatternRenderer() {}

    /** White-on-transparent mask for (style, seed, size), from the cache. */
    public static BufferedImage mask(String styleId, String seed, int width, int height) {
        NodePatternFamily fam = NodePatternStyles.family(styleId);
        if (fam == null) return null; // unknown/null style => no pattern (fail quiet)
        int w = clamp(bucket(width));
        int h = clamp(bucket(height));
        String key = styleId + "\u0000" + seed + "\u0000" + w + "\u0000" + h;
        return MASK_CACHE.computeIfAbsent(key, k -> render(fam, w, h, new Random(seed.hashCode())));
    }

    /** Paints the mask into (x,y,w,h), tinted with {@code ink} at {@code opacity}. */
    public static void paint(Graphics2D g, int x, int y, int w, int h,
                             String styleId, String seed, Color ink, float opacity) {
        BufferedImage m = mask(styleId, seed, w, h);
        if (m == null) return;
        BufferedImage tinted = tint(m, ink, opacity);
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gg.drawImage(tinted, x, y, w, h, null);
        gg.dispose();
    }

    /** Clears both caches (test hook; not needed for theme changes — the mask is theme-independent). */
    public static void invalidateCache() {
        MASK_CACHE.clear();
        TINT_CACHE.clear();
    }

    // ---- internal ----

    /** Reuses or builds the tinted copy of the white mask (ink + its alpha, at {@code opacity}). */
    private static BufferedImage tint(BufferedImage mask, Color ink, float opacity) {
        int w = mask.getWidth(), h = mask.getHeight();
        float o = Math.max(0f, Math.min(1f, opacity));
        int inkArgb = (ink.getRed() << 16) | (ink.getGreen() << 8) | ink.getBlue();
        String key = inkArgb + "\u0000" + Float.floatToIntBits(o) + "\u0000" + w + "\u0000" + h;
        return TINT_CACHE.computeIfAbsent(key, k -> {
            int[] src = mask.getRGB(0, 0, w, h, null, 0, w);
            int[] dst = new int[src.length];
            for (int i = 0; i < src.length; i++) {
                int a = (src[i] >>> 24) & 0xFF;
                if (a == 0) continue;
                int alpha = (int) Math.round(a * o);
                dst[i] = (alpha << 24) | inkArgb;
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            out.setRGB(0, 0, w, h, dst, 0, w);
            return out;
        });
    }

    private static int bucket(int v) { return Math.max(1, (v / BUCKET) * BUCKET); }
    private static int clamp(int v) { return Math.min(MAX, Math.max(1, v)); }

    private static BufferedImage render(NodePatternFamily fam, int w, int h, Random rng) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        fam.paint(g, w, h, rng);
        g.dispose();
        return img;
    }
}
```

The tint technique (mask alpha × ink color, per-pixel, cached) is the non-obvious part of this task; the test `inkIsAppliedAtDrawTimeNotBakedIn` pins it, including the guard that the tint follows the mask's shape rather than filling the rectangle. Do NOT tint with `SRC_IN` on the node's graphics object — that compositing mode multiplies the ink into everything already painted (it would erase the node body).

- [ ] **Step 5: Port the 6 families**

Create one class per family, `final`, no-arg constructor, implementing `NodePatternFamily`. Port the `paint` body from `PatternForge-reference.java` (the `families()` method), keeping the exact constants. Line ranges in the reference:

| Class | id | Reference lambda (PatternForge-reference.java) |
|---|---|---|
| `StripesFamily` | `stripes` | `F.put("stripes", …)` (bands; frame-adjacent straight) |
| `NetFamily` | `net` | `F.put("net", …)` (jittered-grid kNN, k=3) |
| `CellularFamily` | `cellular` | `F.put("cellular", …)` (voronoi membranes + border sites) |
| `CarpetFamily` | `carpet` | `F.put("carpet", …)` + helper `carpet(...)` (Sierpinski carpet) |
| `HilbertFamily` | `hilbert` | `F.put("hilbert", …)` + helper `d2xy(...)` (space-filling curve, order 6) |
| `LandscapeFamily` | `landscape` | `F.put("landscape", …)` + helper `curveY(...)` (strata bands) |

Family classes that need a private helper (`CarpetFamily.carpet`, `HilbertFamily.d2xy`, `LandscapeFamily.curveY`) keep that helper as a private static in the same class. The reference's `render`/edge-audit/`families()` harness is NOT ported (that's the test/preview, already in NodePatternEngineTest + the reference file).

- [ ] **Step 6: Generate the golden reference PNG**

Generate `golden-net-128.png` from the freshly built engine (same JVM ⇒ same render):

Run:
```bash
cd /data/src/jipipe-4 && mvn -o -q -pl jipipe-core test-compile -Dmaven.test.skip=false 2>&1 | tail -3
```
Then a tiny throwaway generator (write to `/tmp`, run with the jipipe-core test classpath):
```bash
cd /data/src/jipipe-4
cat > /tmp/GoldenGen.java <<'EOF'
import org.hkijena.jipipe.desktop.commons.nodepatterns.*;
import java.awt.image.BufferedImage; import java.io.File;
import javax.imageio.ImageIO;
public class GoldenGen {
    public static void main(String[] a) throws Exception {
        BufferedImage m = NodePatternRenderer.mask("net", "jipipe:framework:trackmate", 128, 128);
        ImageIO.write(m, "png", new File("jipipe-core/src/test/resources/org/hkijena/jipipe/desktop/commons/nodepatterns/golden-net-128.png"));
        System.out.println("golden written");
    }
}
EOF
CP=$(mvn -o -q -pl jipipe-core dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null | tail -1)
javac -cp "jipipe-core/target/classes:$CP" -d /tmp/gg /tmp/GoldenGen.java
java -Djava.awt.headless=true -cp "/tmp/gg:jipipe-core/target/classes:$CP" GoldenGen
```
Expected: `golden written` and the PNG exists at the test-resource path.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `mvn -o -q -pl jipipe-core test -Dtest=NodePatternEngineTest`
Expected: PASS (5-6 tests: determinism, golden, plane-filling, color-neutral, registry, tiny-node, ink-at-draw-time). If a family fails plane-filling, its port drifted from the reference — re-copy that lambda's constants. If `inkIsAppliedAtDrawTimeNotBakedIn` fails, fix the renderer's tinting (Step 4 note) — do NOT weaken the test.

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/nodepatterns/ \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/nodepatterns/ \
        jipipe-core/src/test/resources/org/hkijena/jipipe/desktop/commons/nodepatterns/
git commit -m "feat: node pattern engine (family interface, registry, seeded mask renderer, 6 families, contract tests)

Foundation for framework pattern rendering (Phase 2). Pure-Java2D:
white-on-transparent alpha masks, seeded by framework id, tinted per
theme at draw time. Contract tests pin determinism, plane-filling
(all four borders), color-neutrality, and a golden mask. (6.0.0)"
```

---

### Task 2: Remaining 11 families (complete the 17)

**Files:**
- Create: `…/desktop/commons/nodepatterns/{WavesFamily,WeaveFamily,PatchworkFamily,LoopsFamily,DiscoFamily,TrianglesFamily,BlobsFamily,DotsFamily,HoneycombFamily,RippleFamily,FlowFamily}.java`
- Modify: `…/desktop/commons/nodepatterns/NodePatternStyles.java` (register the 11)

**Interfaces:**
- Consumes: `NodePatternFamily` (Task 1).
- Produces: the full 17-id registry (so the Task 1 tests now sweep all 17 automatically).

- [ ] **Step 1: Add the 11 families**

Port each from `PatternForge-reference.java` (same rule as Task 1: exact constants). Families needing a private helper keep it in-class: `WavesFamily` (uses the reference's `f(...)` field + `crossSegs(...)` marching-squares helpers), `PatchworkFamily` (`subdivide(...)`), `BlobsFamily` (none), `DotsFamily` (none), `HoneycombFamily` (none), `RippleFamily` (none), `FlowFamily` (none), `WeaveFamily` (`fillBandH(...)`), `LoopsFamily` (none), `DiscoFamily` (none), `TrianglesFamily` (none).

Reference line anchors: `F.put("waves",…)` (the SECOND waves lambda — the proper marching-squares one, not the first placeholder), `F.put("weave",…)`, `F.put("patchwork",…)`, `F.put("loops",…)`, `F.put("disco",…)`, `F.put("triangles",…)`, `F.put("blobs",…)`, `F.put("dots",…)`, `F.put("honeycomb",…)`, `F.put("ripple",…)`, `F.put("flow",…)`.

- [ ] **Step 2: Register them in `NodePatternStyles`**

Add to the static block (after the 6 from Task 1):
```java
        STYLES.put("waves", new WavesFamily());
        STYLES.put("weave", new WeaveFamily());
        STYLES.put("patchwork", new PatchworkFamily());
        STYLES.put("loops", new LoopsFamily());
        STYLES.put("disco", new DiscoFamily());
        STYLES.put("triangles", new TrianglesFamily());
        STYLES.put("blobs", new BlobsFamily());
        STYLES.put("dots", new DotsFamily());
        STYLES.put("honeycomb", new HoneycombFamily());
        STYLES.put("ripple", new RippleFamily());
        STYLES.put("flow", new FlowFamily());
```

- [ ] **Step 3: Run the full engine sweep (now covers all 17)**

Run: `mvn -o -q -pl jipipe-core test -Dtest=NodePatternEngineTest`
Expected: PASS — the plane-filling, determinism, color-neutral and registry tests now iterate all 17 ids. If any new family fails plane-filling or color-neutral, its port drifted — re-copy the constants (do not lower the test thresholds).

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/nodepatterns/
git commit -m "feat: complete the 17 node pattern families (waves, weave, patchwork, loops, disco, triangles, blobs, dots, honeycomb, ripple, flow)

Engine contract tests now sweep the full catalog. (6.0.0)"
```

---

### Task 3: Theme settings (style fields + editor document + persistence)

**Files:**
- Modify: `…/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java` (4 fields + copy ctor + accessors)
- Modify: `…/desktop/app/customizer/JIPipeDesktopThemeEditorDocument.java` (CategoryGraphEditor fields + annotated accessors + 2 sync blocks)
- Test: `…/test/…/desktop/commons/theme/NodePatternThemeSettingsTest.java`

**Interfaces:**
- Produces (on `JIPipeDesktopModernThemeStyle`): `isShowNodeFrameworkPattern()`, `setShowNodeFrameworkPattern(boolean)`, `getNodePatternInk()`, `setNodePatternInk(Color)`, `getNodePatternOpacity()`, `setNodePatternOpacity(float)`, `isShowNodeFrameworkBadge()`, `setShowNodeFrameworkBadge(boolean)`.
- Consumes: Task 1/2 (not directly — this is the settings layer).

Exact names/defaults (from the spec): `showNodeFrameworkPattern` (boolean, `true`), `nodePatternInk` (Color, `Color.WHITE`), `nodePatternOpacity` (float, `0.15f`), `showNodeFrameworkBadge` (boolean, `true`). `@JsonProperty`/`@JIPipeParameter` keys (kebab-case, matching the file's existing `node-fill-saturation`-style keys): `show-node-framework-pattern`, `node-pattern-ink`, `node-pattern-opacity`, `show-node-framework-badge`.

- [ ] **Step 1: Write the failing persistence test**

```java
package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class NodePatternThemeSettingsTest {

    // Defaults on a fresh style (old theme files must load with these)
    @Test
    void defaultsMatchSpec() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        assertTrue(s.isShowNodeFrameworkPattern());
        assertEquals(Color.WHITE, s.getNodePatternInk());
        assertEquals(0.15f, s.getNodePatternOpacity(), 1e-6);
        assertTrue(s.isShowNodeFrameworkBadge());
    }

    // Copy constructor preserves the new fields (style duplication path)
    @Test
    void copyConstructorPreservesPatternSettings() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        s.setShowNodeFrameworkPattern(false);
        s.setNodePatternInk(new Color(0x123456));
        s.setNodePatternOpacity(0.4f);
        s.setShowNodeFrameworkBadge(false);
        JIPipeDesktopModernThemeStyle c = new JIPipeDesktopModernThemeStyle(s);
        assertFalse(c.isShowNodeFrameworkPattern());
        assertEquals(new Color(0x123456), c.getNodePatternInk());
        assertEquals(0.4f, c.getNodePatternOpacity(), 1e-6);
        assertFalse(c.isShowNodeFrameworkBadge());
    }

    // Jackson round-trip: serialize -> deserialize preserves the fields
    @Test
    void jacksonRoundTripPreservesPatternSettings() throws Exception {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        s.setNodePatternOpacity(0.22f);
        s.setNodePatternInk(new Color(0xFF8800));
        byte[] bytes = JsonUtils.getObjectMapper().writeValueAsBytes(s);
        JIPipeDesktopModernThemeStyle back = JsonUtils.getObjectMapper().readValue(bytes, JIPipeDesktopModernThemeStyle.class);
        assertEquals(0.22f, back.getNodePatternOpacity(), 1e-6);
        assertEquals(new Color(0xFF8800), back.getNodePatternInk());
    }

    // Back-compat: a theme JSON without the new keys deserializes to the defaults
    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        // "brightness" is the JIPipeDesktopUIThemeBrightness enum (values: Light, Dark)
        byte[] bytes = "{\"name\":\"test\",\"brightness\":\"Light\"}".getBytes();
        JIPipeDesktopModernThemeStyle back = JsonUtils.getObjectMapper().readValue(bytes, JIPipeDesktopModernThemeStyle.class);
        assertTrue(back.isShowNodeFrameworkPattern());
        assertEquals(0.15f, back.getNodePatternOpacity(), 1e-6);
    }
}
```

Use `org.hkijena.jipipe.utils.json.JsonUtils` (accessor: `getObjectMapper()` — the same one `ThemeUtils` uses to load styles).

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -o -q -pl jipipe-core test -Dtest=NodePatternThemeSettingsTest`
Expected: COMPILE FAILURE — the accessors don't exist yet.

- [ ] **Step 3: Add the 4 fields to `JIPipeDesktopModernThemeStyle`**

Next to the other node-appearance fields (after `nodeBorderBrightness`, ~line 182). Add `@JsonProperty`-annotated fields with the exact JSON keys + defaults from the step above; extend the **copy constructor** (after the `nodeBorderBrightness` copy, ~line 256) with the 4 assignments; add the 8 accessors (boolean getters as `isShow…`, Color/float getters as `getNodePattern…`) following the file's existing accessor style.

- [ ] **Step 4: Wire the theme editor document (annotation-driven, no form code)**

In `JIPipeDesktopThemeEditorDocument`:
1. In `CategoryGraphEditor` (the node-appearance parameter collection, ~line 775+), add the same 4 fields with `@JIPipeParameter("<json-key>")` + `@SetJIPipeDocumentation(name=…, description=…)` on getters/setters, mirroring `nodeFillSaturation`'s existing annotations. Display names: "Show framework pattern", "Pattern ink", "Pattern opacity", "Show framework badge".
2. In the **style→category** load block (after `categoryGraphEditor.nodeBorderBrightness = style.getNodeBorderBrightness();`, ~line 121): add the 4 `categoryGraphEditor.x = style.getX();` lines.
3. In the **category→style** save block (after `result.setNodeBorderBrightness(categoryGraphEditor.getNodeBorderBrightness());`, ~line 183): add the 4 `result.setX(categoryGraphEditor.getX());` lines.

The theme editor renders `CategoryGraphEditor` generically from its annotations, so no additional UI code is needed.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -o -q -pl jipipe-core test -Dtest=NodePatternThemeSettingsTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopThemeEditorDocument.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/theme/NodePatternThemeSettingsTest.java
git commit -m "feat: theme settings for node framework pattern (show/ink/opacity/badge)

4 fields on JIPipeDesktopModernThemeStyle (defaults: on, white, 0.15, on)
wired into the annotation-driven theme editor's Graph Editor category.
Persistence tests: defaults, copy-ctor, Jackson round-trip, missing-key
fallback. (6.0.0)"
```

---

### Task 4: Framework → style mapping

**Files:**
- Modify: 16 framework classes in `…/api/nodes/taxonomy/frameworks/*.java` + `…/api/nodes/taxonomy/NoFramework.java`
- Test: `…/test/…/api/nodes/taxonomy/frameworks/FrameworksTest.java` (update old style-id pins + add mapping test)
- Test: `…/test/…/desktop/commons/nodepatterns/NodePatternEngineTest.java` (add the registry-mapping assertion)

**Interfaces:**
- Consumes: `NodePatternStyles.family/ids` (Tasks 1-2); `JIPipeNodeTaxonomyFramework.getPatternStyleId()`.
- Produces: the shipped mapping (15 framework→style pairs + `NoFramework`→null).

Mapping (from the spec table — this is the ship contract):

| Framework class | style id |
|---|---|
| `Cellpose2Framework`, `Cellpose3Framework`, `Cellpose4Framework` | `blobs` |
| `TrackMateFramework`, `CometFramework` | `net` |
| `ClijFramework` | `honeycomb` |
| `ImageJ1Framework`, `ImageJ2Framework` | `triangles` |
| `JFreeChartFramework` | `waves` |
| `CoreFramework` | `patchwork` |
| `OpenCVFramework` | `weave` |
| `PythonFramework` | `stripes` |
| `OmniposeFramework` | `cellular` |
| `IlastikFramework` | `dots` |
| `RFramework` | `landscape` |
| `JIPipeDLFramework` | `carpet` |
| `NoFramework` | **null** |

- [ ] **Step 1: Write the failing mapping test**

Two tests live in `FrameworksTest.java` (api test — it may import api classes but NOT the desktop `NodePatternStyles`):

(a) The existing `namesAndPatternStyleIdsMatchTheSeedTable` (lines ~105-129) pins the OLD ids — replace its pattern-style assertions with the new mapping (keep the name assertions):
```java
        assertEquals("patchwork", new CoreFramework().getPatternStyleId());
        assertEquals("blobs", new Cellpose2Framework().getPatternStyleId());
        assertEquals("blobs", new Cellpose3Framework().getPatternStyleId());
        assertEquals("blobs", new Cellpose4Framework().getPatternStyleId());
        assertEquals("net", new TrackMateFramework().getPatternStyleId());
        assertEquals("net", new CometFramework().getPatternStyleId());
        assertEquals("carpet", new JIPipeDLFramework().getPatternStyleId());
        assertEquals("waves", new JFreeChartFramework().getPatternStyleId());
        assertEquals("weave", new OpenCVFramework().getPatternStyleId());
        assertEquals("honeycomb", new ClijFramework().getPatternStyleId());
        assertEquals("triangles", new ImageJ1Framework().getPatternStyleId());
        assertEquals("triangles", new ImageJ2Framework().getPatternStyleId());
        assertEquals("dots", new IlastikFramework().getPatternStyleId());
        assertEquals("cellular", new OmniposeFramework().getPatternStyleId());
        assertEquals("stripes", new PythonFramework().getPatternStyleId());
        assertEquals("landscape", new RFramework().getPatternStyleId());
```
(b) New:
```java
    @Test
    void noFrameworkHasNoPatternStyle() {
        assertNull(new NoFramework().getPatternStyleId(), "NoFramework must carry no pattern");
    }
```
(The existing `patternStyleIdsAreNonEmpty` stays — `allFrameworks()` lists the 16 frameworks and excludes `NoFramework`, so it still passes once the mapping is in.)

The registry-resolution half lives in `NodePatternEngineTest` (desktop test — importing the api framework classes is fine; desktop depends on api). Add:
```java
    @Test
    void everyFrameworkResolvesToAShippedStyle() {
        org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JIPipeNodeTaxonomyFramework[] fws = new org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework[] {
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CoreFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose2Framework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose3Framework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose4Framework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.TrackMateFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CometFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JIPipeDLFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JFreeChartFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OpenCVFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ClijFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ2Framework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IlastikFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OmniposeFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.PythonFramework(),
            new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.RFramework()
        };
        for (var fw : fws) {
            assertNotNull(fw.getPatternStyleId(), fw.getId() + " has no style");
            assertNotNull(NodePatternStyles.family(fw.getPatternStyleId()),
                    fw.getId() + " -> unknown style " + fw.getPatternStyleId());
        }
        assertNull(NodePatternStyles.family(
                new org.hkijena.jipipe.api.nodes.taxonomy.NoFramework().getPatternStyleId()));
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -o -q -pl jipipe-core test -Dtest=FrameworksTest,NodePatternEngineTest`
Expected: FAIL — the framework classes still return the old ids (`constellation`, `shape-grid`, `initials`, `weave`, …) and `NoFramework` still returns `"initials"`.

- [ ] **Step 3: Update the 16 framework classes + NoFramework**

Edit each `getPatternStyleId()` to the table value. `NoFramework.getPatternStyleId()` returns `null`. Keep `getId()`, `getName()`, `getAbbreviation()` unchanged. (These are presentation strings, not persisted project data — safe per the spec's migration note.)

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -o -q -pl jipipe-core test -Dtest=FrameworksTest,NodePatternEngineTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/ \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/NoFramework.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/frameworks/FrameworksTest.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/nodepatterns/NodePatternEngineTest.java
git commit -m "feat: map frameworks to node pattern styles (NoFramework -> none)

Cellpose->blobs, TrackMate/Comet->net, cLij->honeycomb, ImageJ1/2->triangles,
JFreeChart->waves, Core->patchwork, OpenCV->weave, Python->stripes,
Omnipose->cellular, Ilastik->dots, R->landscape, JIPipeDL->carpet.
NoFramework carries no pattern. Mapping is the ship contract (tested). (6.0.0)"
```

---

### Task 5: Node UI integration (pattern + badge + minimap + context menu)

**Files:**
- Modify: `…/desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java`
- Modify: `…/desktop/app/grapheditor/commons/JIPipeDesktopGraphCanvasUI.java`

**Interfaces:**
- Consumes: `NodePatternRenderer.paint(...)` (Task 1), the theme accessors (Task 3), `node.getInfo().getFramework()` (defaults `NoFramework`), `fw.getPatternStyleId()`, `fw.getId()`, `fw.getAbbreviation()`, `fw.getName()`, `fw.getColor()`.
- Produces: the on-canvas pattern + badge. **No tests** (Swing UI — verified by the user in the app, per the test policy). Gate = compiles + full suite stays green + visual check.

- [ ] **Step 1: Paint the pattern in `paintNode`**

In `JIPipeDesktopGraphNodeUI.paintNode` (the body-fill block, ~line 848), AFTER the `g2.fillRect(0,0,w,h)` body fill and BEFORE the disabled/pass-through hatch (`if (getNode() instanceof JIPipeAlgorithm …)` ~line 852), insert:

```java
        // Framework signature pattern over the whole body (spec: hatch
        // paints over it, so disabled/pass-through nodes read as disabled).
        JIPipeNodeTaxonomyFramework fw = getNode().getInfo().getFramework();
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        if (style != null && style.isShowNodeFrameworkPattern()
                && fw != null && !NoFramework.ID.equals(fw.getId())) {
            NodePatternRenderer.paint(g2, 0, 0, getWidth(), getHeight(),
                    fw.getPatternStyleId(), fw.getId(),
                    style.getNodePatternInk(), style.getNodePatternOpacity());
        }
```

Add the needed imports (`NodePatternRenderer`, `JIPipeDesktopModernThemeStyle`, `NoFramework`, `JIPipeNodeTaxonomyFramework`, `ThemeUtils` — several are already imported).

- [ ] **Step 2: Add the badge in the title band**

In `paintNodeControls` (after the node name is drawn, ~line 1205+), when the badge is enabled and the framework is not `NoFramework`, draw a rounded chip: abbreviation text, fill = `fw.getColor()`, border = the node border color, using `zoomedTertiaryFont` (or a derived small font). Compute the chip width with the same `FontMetrics` the name uses and **add it to the title-band centering math** — extend the `centerNativeWidth` expression (the `nameWidth` sum, ~line 1215) by the chip width (a small gap + the chip) when the badge is shown, so the whole run-button/partition/icon/name/chip group stays centered.

Guard: `style.isShowNodeFrameworkBadge() && fw != null && !NoFramework.ID.equals(fw.getId())`. Cache the framework on the node UI instance (next to `nodeFillColor`, ~line 92) so it is read once per node, not per paint.

- [ ] **Step 3: Reuse the mask in the minimap**

In `paintMinimap` (~line 1549), after the body fill (`graphics2D.fillRect(x, y, width, height)`, ~line 1552) and before the runtime-partition/outline, draw the same pattern scaled to the minimap rect at the theme opacity (same guard as Step 1). Use `NodePatternRenderer.paint(graphics2D, x, y, width, height, …)`.

- [ ] **Step 4: Framework name in the node context menu**

NOTE: `JIPipeDesktopGraphNodeUINodeContextMenu` is only the **run-node submenu**. The node's main context menu (Disable/Delete/Rename/…) is assembled by `JIPipeDesktopGraphCanvasUI.createContextMenu()` (~line 925), which already contains selection-based node sections (the "Node partitioning menus" block, ~line 953). There, when the selection contains exactly one node UI (`selectionManager.getSelectionByType(JIPipeDesktopGraphNodeUI.class)` of size 1) whose framework is not `NoFramework`, add at the very top of that block: `menu.add` a disabled `JMenuItem("Framework: " + fw.getName())` (a leading separator only if the menu is non-empty). The canvas has no hover-tooltip, so this line is the discoverability surface for the full framework name — the abbreviation chip alone is not self-explanatory. Guard on the same framework check.

- [ ] **Step 5: Compile**

Run: `mvn -o -q -pl jipipe-core compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Full core suite (no regressions) + real-store check**

Run: `mvn -o -pl jipipe-core test 2>&1 | grep -E "Tests run: [0-9]+, Fail|BUILD" | tail -3`
Expected: BUILD SUCCESS, 0 failures. Then `ls ~/.local/share/JIPipe/profiles/6.0.0/node-templates | wc -l` → `0` (this feature must not touch the template store).

- [ ] **Step 7: Visual verification artifact (throwaway, not shipped)**

Regenerate a node-wall preview from the SHIPPED families (not the prototype) so the user sees the exact in-app look. Write a small throwaway to `/tmp` that, for each of the 16 frameworks, calls `NodePatternRenderer.paint` over a colored rounded rect with a title band + badge chip (mirroring Step 1-2), and saves a grid PNG. Run it headless. Hand the PNG path to the user for the in-app-equivalent eyeball. (The reference `assets/node-wall-preview.png` is the target to match.)

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphCanvasUI.java
git commit -m "feat: paint framework pattern + abbreviation badge on canvas nodes, minimap reuse, context-menu framework line

Pattern over the whole body (hatch paints over it), abbreviation chip in
the title band (centering math extended), same mask in the minimap, and a
disabled 'Framework: <name>' line in the node context menu (the canvas has
no hover tooltip). Driven by the Task-3 theme settings; NoFramework and
unknown styles render plain. (6.0.0)"
```

---

### Task 6: Final verification + close-out

**Files:** none new (verification only).

**Interfaces:** consumes everything from Tasks 1-5.

- [ ] **Step 1: Full core + launcher suites**

Run:
```bash
cd /data/src/jipipe-4 && mvn -o -pl jipipe-core test 2>&1 | grep -E "Tests run: [0-9]+, Fail|BUILD" | tail -3
mvn -o -pl jipipe-launcher test 2>&1 | grep -E "Tests run: [0-9]+, Fail|BUILD" | tail -3
```
Expected: both BUILD SUCCESS, 0 failures. Record the exact counts.

- [ ] **Step 2: Real template store untouched**

Run: `ls ~/.local/share/JIPipe/profiles/6.0.0/node-templates 2>/dev/null | wc -l`
Expected: `0`.

- [ ] **Step 3: Registry oracle sanity (no taxonomy drift)**

The feature touches no node ids or registered IDs — confirm no shipped resource changed: `git status --short` should show only the source/test files from Tasks 1-5 (no `src/main/resources` or pattern-JSON changes).

- [ ] **Step 4: Commit any remaining + report**

If Steps 1-3 surface nothing, there is nothing new to commit. Report to the user: test counts (core/launcher), store=0, the list of new/modified files, and the visual-preview path from Task 5 Step 7. Note explicitly that in-app appearance is the user's to verify (no Swing UI tests, per policy).

---

## Self-Review

**Spec coverage:**
- §Architecture (interface/registry/renderer, seed, cache) → Task 1. ✔
- §Style catalog (17 families, 12 mapped, 5 spare) → Tasks 1-2. ✔
- §Framework mapping (15 pairs + NoFramework null) → Task 4. ✔
- §Node UI (pattern paint order, badge, minimap, tooltip→context-menu) → Task 5. ✔ (tooltip adjusted to context menu — see note below.)
- §Theme settings (4 fields, defaults, editor wiring) → Task 3. ✔
- §Error handling (unknown/NoFramework/tiny-node/hatch-over-pattern/cache-reuse) → Tasks 1 (tiny-node, unknown→null, tint-at-draw), 4 (NoFramework), 5 (hatch order, guards). ✔
- §Testing (determinism+golden, plane-filling, color-neutral, registry) → Tasks 1-2 + 4. ✔
- §Migration (no project impact; FrameworksTest pins update) → Task 4. ✔

**Step scan:** each step names one file, one action, and a checkable result. Family bodies are exact-copies pointing at the reference file (not transcribed). The one genuinely non-obvious algorithm (the renderer's tinting) is called out with the technique and the test that pins it. No "handle edge cases" / "TBD" lines.

**Type consistency:** `mask(styleId, seed, width, height)` and `paint(g,x,y,w,h,styleId,seed,ink,opacity)` are used identically in Tasks 1, 3, 5. Theme accessor names (`isShowNodeFrameworkPattern`, `getNodePatternInk`, `getNodePatternOpacity`, `isShowNodeFrameworkBadge`) are identical in Task 3 (definition) and Task 5 (use). Family class names match the registry ids in Tasks 1-2.

**Review Focus:** all 5 lines are pinned to a task (1→T4, 2→T4+T5, 3→T1, 4→T5, 5→T3+T1). None orphaned.

**Proportion:** the plan is longer than the spec, but the excess is the porting pointers (line ranges into the reference file) and the exact test code — both are decisions the implementer cannot make alone (which constants, which assertions). No family body is transcribed.

**One deliberate deviation from the spec (flagged for the user):** the spec's §Node UI says "tooltip shows the full framework name", but this codebase has **no node hover-tooltip** (the canvas owns mouse handling; only toolbar/menu items have tooltips). The full framework name is therefore surfaced as a disabled `Framework: <name>` line in the node **context menu** (Task 5 Step 4) — the discoverability surface that actually exists. The abbreviation chip + this line together satisfy "full framework name is reachable".
