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

import java.awt.*;
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
 *   <li>the white MASK, keyed by (style, seed, size bucket) - theme-independent;
 *       changing ink/opacity never invalidates it;</li>
 *   <li>the TINTED copy, keyed by (mask, ink, opacity) - built once per theme
 *       look, drawn per node with a plain alpha composite.</li>
 * </ul>
 * Nodes cluster into a handful of sizes; larger nodes scale the nearest
 * bucket up via drawImage.
 */
public final class NodePatternRenderer {
    /**
     * The canonical node body size the pattern is rendered at (the theme preview's node).
     */
    public static final int NODE_WIDTH = 150;
    public static final int NODE_HEIGHT = 75;
    private static final Map<String, BufferedImage> MASK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> TINT_CACHE = new ConcurrentHashMap<>();
    private static final int BUCKET = 8;
    private static final int MAX = 512;

    private NodePatternRenderer() {
    }

    /**
     * White-on-transparent mask for (style, seed, size), from the cache.
     */
    public static BufferedImage mask(String styleId, String seed, int width, int height) {
        NodePatternFamily fam = NodePatternStyles.family(styleId);
        if (fam == null) return null; // unknown/null style => no pattern (fail quiet)
        int w = clamp(bucket(width));
        int h = clamp(bucket(height));
        String key = styleId + "\u0000" + seed + "\u0000" + w + "\u0000" + h;
        return MASK_CACHE.computeIfAbsent(key, k -> render(fam, w, h, new Random(seed.hashCode())));
    }

    /**
     * Paints the mask into (x,y,w,h), tinted with {@code ink} at {@code opacity}.
     */
    public static void paint(Graphics2D g, int x, int y, int w, int h,
                             String styleId, String seed, Color ink, float opacity) {
        BufferedImage m = mask(styleId, seed, w, h);
        if (m == null) return;
        BufferedImage tinted = tint(styleId + "\u0000" + seed, m, ink, opacity);
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gg.drawImage(tinted, x, y, w, h, null);
        gg.dispose();
    }

    /**
     * Paints the pattern into (x,y,w,h) as a CROP of a node's background: a
     * canonical node body ({@value #NODE_WIDTH}×{@value #NODE_HEIGHT} - the size
     * the theme preview draws nodes at) is rendered exactly as a canvas node
     * renders its pattern (via {@link #paint}), and a centered (w×h) window of
     * it is copied into the target. The icon therefore shows the pattern at
     * exactly the same cell scale a canvas node does, pixel-for-pixel.
     * <p>
     * Use this for small regions (e.g. the 14×47 fill of the node list's
     * pattern icon): the plain {@link #paint} would stretch the mask to the
     * target's aspect (distorting cells), and rendering at the target's own
     * size would give cells several times denser than on a real node.
     */
    public static void paintNodeScaleCropped(Graphics2D g, int x, int y, int w, int h,
                                             String styleId, String seed, Color ink, float opacity) {
        BufferedImage nodeBackground = new BufferedImage(NODE_WIDTH, NODE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ng = nodeBackground.createGraphics();
        try {
            paint(ng, 0, 0, NODE_WIDTH, NODE_HEIGHT, styleId, seed, ink, opacity);
        } finally {
            ng.dispose();
        }
        Graphics2D gg = (Graphics2D) g.create();
        try {
            // Capture the caller's clip BEFORE replacing it. gg inherits g's
            // clip at creation; setClip would discard it. Intersect (not
            // replace) so callers that rely on their clip surviving - e.g. the
            // round-cornered list icon that clips to a rounded rect - are not
            // overdrawn by the square crop window.
            Shape callerClip = gg.getClip();
            gg.setClip(x, y, w, h);
            if (callerClip != null) {
                gg.clip(callerClip);
            }
            gg.drawImage(nodeBackground, x - (NODE_WIDTH - w) / 2, y - (NODE_HEIGHT - h) / 2, null);
        } finally {
            gg.dispose();
        }
    }

    /**
     * Clears both caches (test hook; not needed for theme changes - the mask is theme-independent).
     */
    public static void invalidateCache() {
        MASK_CACHE.clear();
        TINT_CACHE.clear();
    }

    // ---- internal ----

    /**
     * Reuses or builds the tinted copy of the white mask (ink + its alpha, at {@code opacity}).
     */
    private static BufferedImage tint(String maskId, BufferedImage mask, Color ink, float opacity) {
        int w = mask.getWidth(), h = mask.getHeight();
        float o = Math.max(0f, Math.min(1f, opacity));
        int inkArgb = (ink.getRed() << 16) | (ink.getGreen() << 8) | ink.getBlue();
        // maskId (style + seed) identifies WHICH white mask this tint belongs to -
        // two styles at the same size/ink/opacity must never share a tinted copy
        String key = maskId + "\u0000" + inkArgb + "\u0000" + Float.floatToIntBits(o) + "\u0000" + w + "\u0000" + h;
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

    private static int bucket(int v) {
        return Math.max(1, (v / BUCKET) * BUCKET);
    }

    private static int clamp(int v) {
        return Math.min(MAX, Math.max(1, v));
    }

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
