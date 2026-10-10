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

package org.hkijena.jipipe.api.nodes.infos;

import org.hkijena.jipipe.api.JIPipeNodeTaxonomyTags;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link JIPipeJavaNodeInfo} extracts the taxonomy fields
 * (functions, framework, spatial dimensions, node-level tags) from
 * {@link ConfigureJIPipeNode} / {@link JIPipeNodeTaxonomyTags} into the
 * new getters on the node info, and that unconverted nodes (no taxonomy
 * fields) report a null primary function, no secondary functions, and a
 * {@link NoFramework} framework.
 */
class JIPipeJavaNodeInfoTaxonomyTest {

    /**
     * Minimal test implementation of {@link JIPipeNodeTaxonomyFunction}.
     */
    static class StubFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;

        public StubFunction() {
            this.id = "stub-function";
        }

        public StubFunction(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return null;
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

    /**
     * Minimal test implementation of {@link JIPipeNodeTaxonomyFramework}.
     */
    static class StubFramework implements JIPipeNodeTaxonomyFramework {
        public StubFramework() {
        }

        @Override
        public String getId() {
            return "stub-framework";
        }

        @Override
        public String getName() {
            return "Stub Framework";
        }

        @Override
        public String getAbbreviation() {
            return "SF";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /**
     * Stub primary function.
     */
    static class StubFuncA extends StubFunction {
        public StubFuncA() {
            super("func-a");
        }
    }

    /**
     * Stub secondary function.
     */
    static class StubFuncB extends StubFunction {
        public StubFuncB() {
            super("func-b");
        }
    }

    /**
     * A node that has been converted to the new taxonomy: it declares
     * functions (first = primary), spatial dimensions, a framework,
     * and node-level taxonomy tags.
     */
    @ConfigureJIPipeNode(function = {StubFuncA.class, StubFuncB.class}, spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D, JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D}, framework = StubFramework.class)
    @JIPipeNodeTaxonomyTags({"x"})
    private static class ConvertedStub extends JIPipeGraphNode {
        public ConvertedStub(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * A node that is NOT yet converted: it declares no taxonomy fields at
     * all (since the legacy node-type-category system was removed in Task
     * 27, "unconverted" is modeled by an empty {@code @ConfigureJIPipeNode}).
     */
    @ConfigureJIPipeNode
    private static class UnconvertedStub extends JIPipeGraphNode {
        public UnconvertedStub(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    @Test
    void convertedNodeExtractsTaxonomy() {
        JIPipeJavaNodeInfo info = new JIPipeJavaNodeInfo("test-node", ConvertedStub.class);

        // Primary function = first declared function
        assertNotNull(info.getPrimaryFunction());
        assertEquals("func-a", info.getPrimaryFunction().getId());

        // Secondary functions = the rest
        Set<JIPipeNodeTaxonomyFunction> secondary = info.getSecondaryFunctions();
        assertEquals(1, secondary.size());
        assertEquals("func-b", secondary.iterator().next().getId());

        // Framework
        assertTrue(info.getFramework() instanceof StubFramework,
            "expected StubFramework, got: " + info.getFramework().getClass());
        assertEquals("stub-framework", info.getFramework().getId());

        // Spatial dimensions
        assertEquals(Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D, JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D),
            info.getSpatialDimensions());

        // Node-level taxonomy tags
        assertEquals(Set.of("x"), info.getTaxonomyTags());

        // Merged tags are only computed at registration (Task 7)
        assertTrue(info.getMergedTags().isEmpty());
    }

    @Test
    void unconvertedNodeHasNoFunction() {
        JIPipeJavaNodeInfo info = new JIPipeJavaNodeInfo("legacy-node", UnconvertedStub.class);

        // Unconverted: no primary function
        assertNull(info.getPrimaryFunction());
        assertTrue(info.getSecondaryFunctions().isEmpty());

        // Framework defaults to NoFramework
        assertNotNull(info.getFramework());
        assertTrue(info.getFramework() instanceof NoFramework,
            "expected NoFramework, got: " + info.getFramework().getClass());

        // No spatial dimensions, no node-level tags
        assertTrue(info.getSpatialDimensions().isEmpty());
        assertTrue(info.getTaxonomyTags().isEmpty());
    }
}
