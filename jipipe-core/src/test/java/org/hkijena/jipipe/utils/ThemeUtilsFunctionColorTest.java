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

package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure tests for the {@link ThemeUtils} function-hue dual-read.
 * <p>
 * The color math lives in the package-private pure core
 * {@code functionNodeFillColor}/{@code functionNodeBorderColor}, which is
 * exercised directly here (same package) with an in-test style + an empty
 * {@link FunctionRegistry} ({@code baseHue}/{@code shadeDepth} walk the
 * function's own parent chain and read no registry state).
 * <p>
 * One test exercises the <em>public</em> facade for an unconverted node to
 * prove the legacy fallback path is byte-identical and never touches
 * {@code JIPipe.getNodes()}. No {@code JIPipe} instance is created.
 */
class ThemeUtilsFunctionColorTest {

    private static final float TEST_HUE = 186f / 360f;

    // ---------------------------------------------------------------------
    // Minimal taxonomy-function stub (id/name/hue/parent, default flags)
    // ---------------------------------------------------------------------
    private static class TestFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;
        private final String name;
        private final Float hue;
        private JIPipeNodeTaxonomyFunction parent;

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
            return false;
        }
    }

    /**
     * Minimal {@link JIPipeNodeInfo} stub whose {@code getPrimaryFunction()}
     * is {@code null} (the fn-null defensive state, only reachable via
     * {@code JIPipeEmptyNodeInfo}, never a registered node).
     */
    private static class TestInfo implements JIPipeNodeInfo {
        private TestInfo() {
        }

        @Override
        public String getId() {
            return "stub";
        }

        @Override
        public Class<? extends JIPipeGraphNode> getInstanceClass() {
            return JIPipeGraphNode.class;
        }

        @Override
        public JIPipeGraphNode newInstance() {
            return null;
        }

        @Override
        public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
            return null;
        }

        @Override
        public String getName() {
            return "Stub Node";
        }

        @Override
        public HTMLText getDescription() {
            return null;
        }

        @Override
        public List<AddJIPipeInputSlot> getInputSlots() {
            return List.of();
        }

        @Override
        public List<AddJIPipeOutputSlot> getOutputSlots() {
            return List.of();
        }

        @Override
        public Set<Class<? extends JIPipeEnvironment>> getEnvironments() {
            return Set.of();
        }

        @Override
        public Set<JIPipeDependency> getDependencies() {
            return Set.of();
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public JIPipeNodeTaxonomyFunction getPrimaryFunction() {
            return null;
        }

        @Override
        public Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
            return Collections.emptySet();
        }
    }

    // ---------------------------------------------------------------------
    // (a) top-level hue-186/360 → depth-0 identity
    // ---------------------------------------------------------------------
    @Test
    void topLevelFunctionFillUsesUnshadedHue() {
        JIPipeDesktopModernThemeStyle style = new JIPipeDesktopModernThemeStyle();
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("top", "Top", TEST_HUE);

        Color fill = ThemeUtils.functionNodeFillColor(top, style, registry);
        Color expected = Color.getHSBColor(TEST_HUE,
                style.getNodeFillSaturation(), style.getNodeFillBrightness());
        assertEquals(expected, fill);

        Color border = ThemeUtils.functionNodeBorderColor(top, style, registry);
        Color expectedBorder = Color.getHSBColor(TEST_HUE,
                style.getNodeBorderSaturation(), style.getNodeBorderBrightness());
        assertEquals(expectedBorder, border);
    }

    // ---------------------------------------------------------------------
    // (b) child at depth 1 → shaded by the fixed HSB rule
    // ---------------------------------------------------------------------
    @Test
    void childFunctionFillIsShadedByDepthOne() {
        JIPipeDesktopModernThemeStyle style = new JIPipeDesktopModernThemeStyle();
        FunctionRegistry registry = new FunctionRegistry();
        TestFunction top = new TestFunction("top", "Top", TEST_HUE);
        TestFunction child = new TestFunction("child", "Child", null).withParent(top);

        float fillSat = style.getNodeFillSaturation();
        float fillBri = style.getNodeFillBrightness();
        // The Task-2 shade rule (identical to FunctionRegistry.shadeHsb at depth 1):
        // saturation -0.04 floored at 0.05, brightness +0.06 capped at 0.95.
        Color expectedFill = Color.getHSBColor(TEST_HUE,
                Math.max(0.05f, fillSat - 0.04f), Math.min(0.95f, fillBri + 0.06f));
        assertEquals(expectedFill, ThemeUtils.functionNodeFillColor(child, style, registry));

        float borderSat = style.getNodeBorderSaturation();
        float borderBri = style.getNodeBorderBrightness();
        Color expectedBorder = Color.getHSBColor(TEST_HUE,
                Math.max(0.05f, borderSat - 0.04f), Math.min(0.95f, borderBri + 0.06f));
        assertEquals(expectedBorder, ThemeUtils.functionNodeBorderColor(child, style, registry));

        // Cross-check the core against the registry's own pure shading rule.
        float[] hsb = FunctionRegistry.shadeHsb(TEST_HUE, fillSat, fillBri, 1);
        assertEquals(Color.getHSBColor(hsb[0], hsb[1], hsb[2]),
                ThemeUtils.functionNodeFillColor(child, style, registry));
    }

    // ---------------------------------------------------------------------
    // (c) transparent branch (hue-null root) → panel background / highlight border
    // ---------------------------------------------------------------------
    @Test
    void transparentBranchUsesPanelBackgroundAndHighlightBorder() {
        JIPipeDesktopModernThemeStyle style = new JIPipeDesktopModernThemeStyle();
        FunctionRegistry registry = new FunctionRegistry();
        // Root with a null hue (transparent) and a child beneath it.
        TestFunction transparentRoot = new TestFunction("transparent", "Transparent", null);
        TestFunction child = new TestFunction("tchild", "TChild", null).withParent(transparentRoot);

        assertEquals(style.getPanelBackground(),
                ThemeUtils.functionNodeFillColor(child, style, registry));
        assertEquals(style.getNodeHighlightBorder(),
                ThemeUtils.functionNodeBorderColor(child, style, registry));

        // The transparent root itself is also rendered with the panel background.
        assertEquals(style.getPanelBackground(),
                ThemeUtils.functionNodeFillColor(transparentRoot, style, registry));
        assertEquals(style.getNodeHighlightBorder(),
                ThemeUtils.functionNodeBorderColor(transparentRoot, style, registry));
    }

    // ---------------------------------------------------------------------
    // (e-cycle, optional) broken/cycle chain → base hue 0.5, depth 0 (identity)
    // ---------------------------------------------------------------------
    @Test
    void brokenCycleFallsBackToNeutralHalfHue() {
        JIPipeDesktopModernThemeStyle style = new JIPipeDesktopModernThemeStyle();
        FunctionRegistry registry = new FunctionRegistry();
        // a -> b -> a, both hue-null: baseHue throws a cycle ISE.
        TestFunction a = new TestFunction("a", "A", null);
        TestFunction b = new TestFunction("b", "B", null).withParent(a);
        a.withParent(b);

        Color expectedFill = Color.getHSBColor(0.5f,
                style.getNodeFillSaturation(), style.getNodeFillBrightness());
        assertEquals(expectedFill, ThemeUtils.functionNodeFillColor(a, style, registry));

        Color expectedBorder = Color.getHSBColor(0.5f,
                style.getNodeBorderSaturation(), style.getNodeBorderBrightness());
        assertEquals(expectedBorder, ThemeUtils.functionNodeBorderColor(a, style, registry));
    }

    // ---------------------------------------------------------------------
    // (d) unconverted node through the PUBLIC facade → byte-identical legacy color
    // ---------------------------------------------------------------------
    @Test
    void fnNullInfoPublicFacadeUsesDefensiveDefaults() {
        // The fn-null defensive state (primary function null; only reachable
        // via JIPipeEmptyNodeInfo, never a registered node): the facades
        // resolve to the legacy negative-hue transparent channels — the panel
        // background fill and the highlight border. This path never calls
        // JIPipe.getNodes(), so the test stays pure.
        TestInfo info = new TestInfo();
        JIPipeDesktopModernThemeStyle currentStyle = ThemeUtils.getCurrentStyle();
        assertEquals(currentStyle.getPanelBackground(), ThemeUtils.getNodeFillColor(info));
        assertEquals(currentStyle.getNodeHighlightBorder(), ThemeUtils.getNodeBorderColor(info));
    }
}
