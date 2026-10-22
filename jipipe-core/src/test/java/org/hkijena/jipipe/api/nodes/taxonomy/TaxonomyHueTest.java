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

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the deterministic hue auto-picker: {@link TaxonomyHue#autoHue(HueCategory, String)}
 * and the {@link JIPipeNodeTaxonomyFunction#getColorHue()} default path
 * (top-level + {@link HueCategory} → in-cell hue; no category → transparent;
 * explicit override wins; leaves always inherit).
 * <p>
 * The Voronoi cells are computed here from the enum's anchors with an
 * independent copy of the cell formula — not from {@link TaxonomyHue}'s
 * helpers — so containment and tiling genuinely cross-check the implementation.
 */
class TaxonomyHueTest {

    private static final List<String> CORPUS = List.of(
        "jipipe:function:x",
        "a",
        "b",
        "jipipe:function:rois",
        "jipipe:function:images",
        "some-plugin:my-function",
        "long-id-with-many-digits-1234567890",
        "Uebung-übung",
        "id",
        "x".repeat(40));

    private static final List<String> DISTINCT_CORPUS = List.of(
        "t-1", "t-2", "t-3", "t-4",
        "jipipe:function:m", "jipipe:function:n", "jipipe:function:o", "jipipe:function:p");

    // ---------------------------------------------------------------------
    // Cell formula (test-local copy; the implementation must not be called)
    // ---------------------------------------------------------------------

    /**
     * Computes the Voronoi cell of every {@link HueCategory} from the enum's
     * anchors: for anchor aᵢ with previous-anchor gap gᵢ₋₁ = (aᵢ − aᵢ₋₁) mod 360
     * and next-anchor gap gᵢ = (aᵢ₊₁ − aᵢ) mod 360, the cell is
     * [aᵢ − gᵢ₋₁/2, aᵢ + gᵢ/2), wrap-aware.
     *
     * @return start degrees (0..360) of each category's cell
     */
    private static double[] cellStartDeg() {
        HueCategory[] categories = HueCategory.values();
        int n = categories.length;
        double[] starts = new double[n];
        for (int i = 0; i < n; i++) {
            double anchor = categories[i].getAnchorDeg();
            double prevAnchor = categories[(i - 1 + n) % n].getAnchorDeg();
            double nextAnchor = categories[(i + 1) % n].getAnchorDeg();
            double prevGap = ((anchor - prevAnchor) % 360 + 360) % 360;
            double nextGap = ((nextAnchor - anchor) % 360 + 360) % 360;
            starts[i] = ((anchor - prevGap / 2) % 360 + 360) % 360;
        }
        return starts;
    }

    /** Cell width of the category at the given cell-start (deg). */
    private static double cellWidthDeg(int startSlot) {
        double[] starts = cellStartDeg();
        double next = starts[(startSlot + 1) % starts.length];
        double width = ((next - starts[startSlot]) % 360 + 360) % 360;
        return width == 0 ? 360 : width;
    }

    /** True if hueDeg lies inside the cell [start, start + width) on the circle. */
    private static boolean inCellDeg(double hueDeg, double start, double width) {
        double d = ((hueDeg - start) % 360 + 360) % 360;
        // 1e-4 tolerance covers the float arithmetic at the wrap boundary.
        return d < width + 1e-4 || d > 360 - 1e-4;
    }

    /** Normalized hue (0..1) in degrees, wrapping. */
    private static double hueDeg(Float hue) {
        return ((hue * 360) % 360 + 360) % 360;
    }

    // ---------------------------------------------------------------------
    // (1) determinism
    // ---------------------------------------------------------------------

    @Test
    void determinism() {
        // Same input → identical output, even across fresh calls.
        assertEquals(TaxonomyHue.autoHue(HueCategory.GEOMETRY, "jipipe:function:x"),
            TaxonomyHue.autoHue(HueCategory.GEOMETRY, "jipipe:function:x"));
        // Different stable ids in the same category → different hues.
        assertNotEquals(TaxonomyHue.autoHue(HueCategory.GEOMETRY, "a"),
            TaxonomyHue.autoHue(HueCategory.GEOMETRY, "b"));
    }

    // ---------------------------------------------------------------------
    // (2) cell containment
    // ---------------------------------------------------------------------

    @Test
    void cellContainment() {
        double[] starts = cellStartDeg();
        for (int i = 0; i < HueCategory.values().length; i++) {
            HueCategory category = HueCategory.values()[i];
            double width = cellWidthDeg(i);
            for (String id : CORPUS) {
                Float hue = TaxonomyHue.autoHue(category, id);
                assertNotNull(hue);
                assertTrue(hue >= 0f && hue <= 1f, "hue " + hue + " out of 0..1 for " + category + "/" + id);
                assertTrue(inCellDeg(hueDeg(hue), starts[i], width),
                    "autoHue(" + category + ", \"" + id + "\") = " + hue + " (" + hueDeg(hue) + "°) "
                        + "outside " + category + " cell [" + starts[i] + ", " + starts[i] + width + ")");
            }
        }
    }

    // ---------------------------------------------------------------------
    // (3) tiling
    // ---------------------------------------------------------------------

    @Test
    void cellsAreDisjointAndTileTheCircle() {
        double[] starts = cellStartDeg();
        int n = starts.length;
        // Pairwise disjoint: for two circular arcs [s, s+w) with w < 360°, a
        // positive-length intersection exists iff one arc's start point lies
        // strictly inside the other arc.
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double ai = starts[i], wi = cellWidthDeg(i);
                double aj = starts[j], wj = cellWidthDeg(j);
                assertTrue(!inArcStrict(aj, ai, wi) && !inArcStrict(ai, aj, wj),
                    "cells overlap: [" + ai + ", +" + wi + ") ∩ [" + aj + ", +" + wj + ")");
            }
        }
        // Total span is exactly 360°.
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += cellWidthDeg(i);
        }
        assertEquals(360, total, 1e-9);
    }

    /** True if point x lies strictly inside the circular arc [start, start + width). */
    private static boolean inArcStrict(double x, double start, double width) {
        return ((x - start) % 360 + 360) % 360 < width;
    }

    // ---------------------------------------------------------------------
    // (4) golden spread
    // ---------------------------------------------------------------------

    @Test
    void goldenSpreadProducesDistinctHues() {
        List<Float> hues = new ArrayList<>();
        for (String id : DISTINCT_CORPUS) {
            hues.add(TaxonomyHue.autoHue(HueCategory.ANALYSIS, id));
        }
        for (int i = 0; i < hues.size(); i++) {
            for (int j = i + 1; j < hues.size(); j++) {
                assertNotEquals(hues.get(i), hues.get(j),
                    "hues collide for ids " + DISTINCT_CORPUS.get(i) + " and " + DISTINCT_CORPUS.get(j));
            }
        }
    }

    // ---------------------------------------------------------------------
    // (5) explicit hue beats category
    // ---------------------------------------------------------------------

    @Test
    void explicitHueBeatsCategory() {
        TestFunction function = new TestFunction("jipipe:function:explicit", "Explicit", 0.99f, HueCategory.GEOMETRY, null);
        assertEquals(0.99f, function.getColorHue());
    }

    // ---------------------------------------------------------------------
    // (6) no category → transparent; leaves always inherit
    // ---------------------------------------------------------------------

    @Test
    void noCategoryNoHue() {
        // Top-level without a category: the default getColorHue() is null (transparent).
        TestFunction transparent = new TestFunction("jipipe:function:transparent", "Transparent", null, null, null);
        assertNull(transparent.getColorHue());
        // Leaf with a category: still null — leaves never own a hue, they inherit.
        TestFunction top = new TestFunction("jipipe:function:top", "Top", null, HueCategory.GEOMETRY, null);
        TestFunction leaf = new TestFunction("jipipe:function:leaf", "Leaf", null, HueCategory.GEOMETRY, top);
        assertNull(leaf.getColorHue());
    }

    // ---------------------------------------------------------------------
    // (7) duplicate hue is reported by validation
    // ---------------------------------------------------------------------

    @Test
    void duplicateHueIsReportedByValidation() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction a = new TestFunction("jipipe:function:dup-a", "Dup A", 0.5f, null, null);
        TestFunction b = new TestFunction("jipipe:function:dup-b", "Dup B", 0.5f, null, null);
        registry.register(a);
        registry.register(b);

        Set<String> errors = registry.validate();
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate top-level color hue")),
            "expected a 'Duplicate top-level color hue' error, got: " + errors);
    }

    // ---------------------------------------------------------------------
    // Test-local minimal functions (implement the interface's abstract
    // methods; the two default methods — getHueCategory() / getColorHue() —
    // are left to the interface unless the group under test pins one of them)
    // ---------------------------------------------------------------------

    private static class TestFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;
        private final String name;
        private final Float explicitHue;
        private final HueCategory explicitCategory;
        private final JIPipeNodeTaxonomyFunction parent;

        private TestFunction(String id, String name, Float explicitHue, HueCategory explicitCategory,
                             JIPipeNodeTaxonomyFunction parent) {
            this.id = id;
            this.name = name;
            this.explicitHue = explicitHue;
            this.explicitCategory = explicitCategory;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return parent;
        }

        /** Overrides getColorHue() only when this group pins an explicit hue. */
        @Override
        public Float getColorHue() {
            return explicitHue != null ? explicitHue : JIPipeNodeTaxonomyFunction.super.getColorHue();
        }

        /** Overrides getHueCategory() only when this group pins a category. */
        @Override
        public HueCategory getHueCategory() {
            return explicitCategory != null ? explicitCategory : JIPipeNodeTaxonomyFunction.super.getHueCategory();
        }

        @Override
        public String getDescription() {
            return id;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }
}
