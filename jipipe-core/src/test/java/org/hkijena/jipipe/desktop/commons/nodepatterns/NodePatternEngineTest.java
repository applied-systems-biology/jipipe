/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.nodepatterns;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NodePatternEngineTest {

    private static final String[] SIZES = {"64", "128", "200"};

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
        int S = img.getWidth();
        int edgeW = 6;
        double[] acc = new double[4];
        for (int i = 0; i < edgeW; i++) {
            for (int j = 0; j < S; j++) {
                if (ink(img.getRGB(j, i))) acc[0]++;
                if (ink(img.getRGB(i, j))) acc[1]++;
            }
            for (int j = 0; j < S; j++) {
                if (ink(img.getRGB(j, S - 1 - i))) acc[2]++;
                if (ink(img.getRGB(S - 1 - i, j))) acc[3]++;
            }
        }
        double n = edgeW * S;
        return new double[]{acc[0] / n, acc[1] / n, acc[2] / n, acc[3] / n};
    }

    static boolean ink(int argb) {
        return ((argb >>> 24) & 0xFF) > 40;
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

    // Registry mapping: every shipped framework resolves to a shipped style; NoFramework carries none
    @Test
    void everyFrameworkResolvesToAShippedStyle() {
        org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework[] fws = new org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework[]{
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CoreFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose2Framework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose3Framework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Cellpose4Framework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.TrackMateFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JIPipeDLFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.JFreeChartFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OpenCVFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ClijFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ2Framework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IlastikFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OmniposeFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.PythonFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.RFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.WekaFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.FilamentsFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IJ3DFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.OmeroFramework(),
                new org.hkijena.jipipe.api.nodes.taxonomy.frameworks.Scene3DFramework()
        };
        for (var fw : fws) {
            assertNotNull(fw.getPatternStyleId(), fw.getId() + " has no style");
            assertNotNull(NodePatternStyles.family(fw.getPatternStyleId()),
                    fw.getId() + " -> unknown style " + fw.getPatternStyleId());
        }
        assertNull(NodePatternStyles.family(
                new org.hkijena.jipipe.api.nodes.taxonomy.NoFramework().getPatternStyleId()));
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
        for (int y = 0; y < 128; y++)
            for (int x = 0; x < 128; x++) {
                int a = base.getRGB(x, y);
                if (((a >>> 24) & 0xFF) == 0) {
                    transparent++;
                    continue;
                }
                if (((a >>> 16) & 0xFF) > (a & 0xFF)) reds++;
                else blues++;
            }
        assertTrue(reds > blues, "red ink should dominate the tinted mask");
        // the tint must follow the mask's shape, not fill the whole rect
        assertTrue(transparent > total / 2,
                "tinted draw is a solid rectangle (" + transparent + "/" + total + " transparent) — tinting ignores the mask alpha");
    }

    // Review round 2 (ruling 2026-18): the tint cache must be keyed by mask identity —
    // two DIFFERENT styles at the same size + ink + opacity must not share a tinted copy.
    // Fails against a tint cache keyed by (ink, opacity, w, h) only.
    @Test
    void differentStylesAtSameSizeDoNotShareTint() {
        BufferedImage a = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ga = a.createGraphics();
        NodePatternRenderer.paint(ga, 0, 0, 128, 128, "stripes", "seed", new Color(0x3366CC), 0.5f);
        ga.dispose();
        BufferedImage b = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gb = b.createGraphics();
        NodePatternRenderer.paint(gb, 0, 0, 128, 128, "net", "seed", new Color(0x3366CC), 0.5f);
        gb.dispose();
        long differing = 0;
        for (int y = 0; y < 128; y++)
            for (int x = 0; x < 128; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                if (pa != pb && ((((pa >>> 24) | (pb >>> 24)) & 0xFF) > 0)) differing++;
            }
        assertTrue(differing > 0,
                "two different styles at the same size/ink/opacity drew pixel-identical images — tint cache key omits mask identity");
    }
}
