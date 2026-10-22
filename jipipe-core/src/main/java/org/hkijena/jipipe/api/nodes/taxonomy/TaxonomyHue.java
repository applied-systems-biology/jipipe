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

package org.hkijena.jipipe.api.nodes.taxonomy;

/**
 * Deterministic hue auto-picking for taxonomy functions.
 * <p>
 * {@link #autoHue(HueCategory, String)} is a pure function of the
 * {@link HueCategory} and the function's stable id: the same input yields the
 * same hue on every install, in every plugin configuration and registration
 * order. It maps every id into its category's Voronoi cell, so conceptually
 * similar functions are visually adjacent by construction.
 * <p>
 * Hue is never persisted (not in settings, node database or project JSON) —
 * it is computed live from the function tree.
 */
public final class TaxonomyHue {

    /** FNV-1a 64-bit offset basis. */
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;

    /** FNV-1a 64-bit prime. */
    private static final long FNV_PRIME = 0x100000001b3L;

    /** Number of hue slots per Voronoi cell. */
    private static final int SLOTS = 24;

    /**
     * Golden-angle fraction (137.508°/360°): consecutive slots are maximally
     * spread inside the cell.
     */
    private static final float GOLDEN_FRACTION = 0.381966011250105f;

    private TaxonomyHue() {
    }

    /**
     * Deterministically picks a hue inside {@code category}'s Voronoi cell
     * for the function with the given stable id.
     * <p>
     * Algorithm: FNV-1a 64 over {@code category.name() + "\u0000" + stableId}
     * → {@code slot = floorMod(hash, 24)} → {@code spread = (slot * goldenFraction) % 1}
     * → {@code hue = (cellStartDeg + spread * cellWidthDeg) / 360},
     * wrap-normalized to {@code 0..1}.
     *
     * @param category the hue family
     * @param stableId the function's stable id
     * @return the normalized hue in {@code 0..1}; never {@code null}
     */
    public static Float autoHue(HueCategory category, String stableId) {
        long h = fnv1a64((category.name() + "\u0000" + stableId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        int slot = (int) Math.floorMod(h, SLOTS);
        float spread = (slot * GOLDEN_FRACTION) % 1f;
        float deg = cellStartDeg(category) + spread * cellWidthDeg(category);
        float normalized = ((deg % 360f) + 360f) % 360f;
        return normalized / 360f;
    }

    /**
     * Start of the category's Voronoi cell in degrees ({@code 0..360}).
     * The cell runs from the midpoint between the previous anchor and this
     * anchor to the midpoint between this anchor and the next one, wrapping
     * around the hue circle.
     *
     * @param category the hue family
     * @return the cell start in degrees
     */
    public static float cellStartDeg(HueCategory category) {
        HueCategory[] categories = HueCategory.values();
        int index = java.util.Arrays.asList(categories).indexOf(category);
        int n = categories.length;
        float anchor = categories[index].getAnchorDeg();
        float prevAnchor = categories[(index - 1 + n) % n].getAnchorDeg();
        float prevGap = mod360(anchor - prevAnchor);
        return mod360(anchor - prevGap / 2f);
    }

    /**
     * Width of the category's Voronoi cell in degrees.
     *
     * @param category the hue family
     * @return the cell width in degrees
     */
    public static float cellWidthDeg(HueCategory category) {
        HueCategory[] categories = HueCategory.values();
        int index = java.util.Arrays.asList(categories).indexOf(category);
        int n = categories.length;
        float anchor = categories[index].getAnchorDeg();
        float prevAnchor = categories[(index - 1 + n) % n].getAnchorDeg();
        float nextAnchor = categories[(index + 1) % n].getAnchorDeg();
        float prevGap = mod360(anchor - prevAnchor);
        float nextGap = mod360(nextAnchor - anchor);
        return (prevGap + nextGap) / 2f;
    }

    /**
     * FNV-1a 64-bit hash of the given bytes (offset basis
     * {@code 0xcbf29ce484222325L}, prime {@code 0x100000001b3L}).
     */
    private static long fnv1a64(byte[] bytes) {
        long h = FNV_OFFSET_BASIS;
        for (byte b : bytes) {
            h ^= (b & 0xffL);
            h *= FNV_PRIME;
        }
        return h;
    }

    /**
     * Positive modulo into {@code 0..359} (deg).
     */
    private static float mod360(float degrees) {
        return ((degrees % 360f) + 360f) % 360f;
    }
}
