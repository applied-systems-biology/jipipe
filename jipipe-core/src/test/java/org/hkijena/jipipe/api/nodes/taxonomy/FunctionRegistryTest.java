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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link FunctionRegistry} registration, lookup, hue resolution,
 * the fixed HSB shading rule, and startup validation.
 */
class FunctionRegistryTest {

    /**
     * Minimal test implementation of {@link JIPipeNodeTaxonomyFunction}.
     * {@code withParent()} links the function into a chain.
     */
    private static class TestFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;
        private final String name;
        private JIPipeNodeTaxonomyFunction parent;
        private final Float hue;

        private TestFunction(String id, String name, Float hue) {
            this.id = id;
            this.name = name;
            this.hue = hue;
        }

        private TestFunction withParent(JIPipeNodeTaxonomyFunction parent) {
            this.parent = parent;
            return this;
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

        @Override
        public Float getColorHue() {
            return hue;
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

    /** Builds a 3-node chain: top ("seg", hue 0.5) → cell → leaf. */
    private static JIPipeNodeTaxonomyFunction chain() {
        TestFunction top = new TestFunction("seg", "Segmentation", 0.5f);
        TestFunction cell = new TestFunction("cell", "Cells", null).withParent(top);
        TestFunction leaf = new TestFunction("leaf", "Leaf", null).withParent(cell);
        return leaf;
    }

    @Test
    void registerAndLookup() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("seg", "Segmentation", 0.5f);
        TestFunction cell = new TestFunction("cell", "Cells", null).withParent(top);
        TestFunction leaf = new TestFunction("leaf", "Leaf", null).withParent(cell);
        registry.register(top);
        registry.register(cell);
        registry.register(leaf);

        assertEquals(3, registry.all().size());
        assertEquals(top, registry.byId("seg"));
        assertEquals(leaf, registry.byId("leaf"));
        assertTrue(registry.topLevel().contains(top));
        assertEquals(1, registry.topLevel().size());

        List<JIPipeNodeTaxonomyFunction> topChildren = registry.children(top);
        assertEquals(List.of(cell), topChildren);
        assertEquals(List.of(leaf), registry.children(cell));
        assertTrue(registry.children(leaf).isEmpty());
    }

    @Test
    void cycleRejected() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction a = new TestFunction("a", "A", null);
        TestFunction b = new TestFunction("b", "B", null).withParent(a);
        a = a.withParent(b); // a → b → a
        registry.register(a);
        registry.register(b);

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("a") && e.contains("b")),
            "cycle must be reported with the offending ids, got: " + errors);
    }

    @Test
    void unreachableLeafRejected() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("top", "Top", 0.5f);
        TestFunction mid = new TestFunction("mid", "Mid", null).withParent(top);
        TestFunction orphanedLeaf = new TestFunction("leaf", "Leaf", null).withParent(mid);
        // Register only two of the three chain nodes: the leaf's parent "mid"
        // is not registered, so the chain does not reach a registered top-level.
        registry.register(top);
        registry.register(orphanedLeaf);

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("leaf") && e.contains("mid")),
            "unreachable leaf must be reported with the offending ids, got: " + errors);
    }

    @Test
    void baseHueAndDepthTopLevel() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("seg", "Segmentation", 0.5f);
        registry.register(top);

        assertEquals(0.5f, registry.baseHue(top));
        assertEquals(0, registry.shadeDepth(top));
    }

    @Test
    void shadingByDepth() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("seg", "Segmentation", 0.5f);
        TestFunction cell = new TestFunction("cell", "Cells", null).withParent(top);
        TestFunction leaf = new TestFunction("leaf", "Leaf", null).withParent(cell);
        registry.register(top);
        registry.register(cell);
        registry.register(leaf);

        assertEquals(0.5f, registry.baseHue(cell));
        assertEquals(1, registry.shadeDepth(cell));
        assertEquals(0.5f, registry.baseHue(leaf));
        assertEquals(2, registry.shadeDepth(leaf));

        // Depth 0 is the identity: hue, saturation and brightness unchanged.
        float[] identity = FunctionRegistry.shadeHsb(0.5f, 0.5f, 0.5f, 0);
        assertEquals(0.5f, identity[0]);
        assertEquals(0.5f, identity[1]);
        assertEquals(0.5f, identity[2]);

        // Depths 1 and 2 differ (monotonic brightness: +0.06 per level,
        // saturation: −0.04 per level).
        float[] d1 = FunctionRegistry.shadeHsb(0.5f, 0.5f, 0.5f, 1);
        float[] d2 = FunctionRegistry.shadeHsb(0.5f, 0.5f, 0.5f, 2);
        assertEquals(0.5f, d1[0]);
        assertEquals(0.5f, d2[0]);
        // delta covers single-ulp float rounding of the rule sat - 0.04f*depth
        // and bri + 0.06f*depth
        assertEquals(0.46f, d1[1], 1e-6f);
        assertEquals(0.42f, d2[1], 1e-6f);
        assertEquals(0.56f, d1[2], 1e-6f);
        assertEquals(0.62f, d2[2], 1e-6f);
        assertNotEquals(d1[1], d2[1]);
        assertNotEquals(d1[2], d2[2]);
        // monotonic: deeper = brighter, less saturated
        assertTrue(d1[2] < d2[2]);
        assertTrue(d1[1] > d2[1]);

        // Deterministic: calling twice yields identical results.
        float[] d1Again = FunctionRegistry.shadeHsb(0.5f, 0.5f, 0.5f, 1);
        assertArrayEquals(d1, d1Again);
    }

    @Test
    void duplicateTopHuesReported() {
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction a = new TestFunction("top-a", "Top A", 0.5f);
        TestFunction b = new TestFunction("top-b", "Top B", 0.5f);
        registry.register(a);
        registry.register(b);

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("top-a") && e.contains("top-b")),
            "duplicate top-level hues must be reported with the offending ids, got: " + errors);
    }
}
