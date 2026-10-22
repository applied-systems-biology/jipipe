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

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView.BehaviorFlags;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure tests for {@link TaxonomyView}: display name, location path,
 * behavior flags and search ids, for both converted (primary function
 * present) and unconverted (legacy category only) node infos.
 * <p>
 * No {@code JIPipe} instance is created; everything is driven by in-test
 * stub objects, so the test runs without any registry/service state.
 */
class TaxonomyViewTest {

    // ---------------------------------------------------------------------
    // Stubs
    // ---------------------------------------------------------------------

    /**
     * Minimal {@link JIPipeNodeTaxonomyFunction} stub with controllable
     * id, name, hue, parent chain and the four behavior flags.
     */
    private static class TestFunction implements JIPipeNodeTaxonomyFunction {
        private final String id;
        private final String name;
        private final Float hue;
        private final JIPipeNodeTaxonomyFunction parent;
        private final boolean pipeline;
        private final boolean compartments;
        private final boolean extract;
        private final boolean runnable;

        private TestFunction(String id, String name, Float hue,
                             JIPipeNodeTaxonomyFunction parent,
                             boolean pipeline, boolean compartments,
                             boolean extract, boolean runnable) {
            this.id = id;
            this.name = name;
            this.hue = hue;
            this.parent = parent;
            this.pipeline = pipeline;
            this.compartments = compartments;
            this.extract = extract;
            this.runnable = runnable;
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
            return pipeline;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return compartments;
        }

        @Override
        public boolean canExtract() {
            return extract;
        }

        @Override
        public boolean isRunnable() {
            return runnable;
        }
    }

    /**
     * Minimal {@link JIPipeNodeInfo} stub. Only the taxonomy-relevant
     * accessors are meaningful; the unrelated accessors return harmless
     * defaults so the class never triggers JIPipe service lookups.
     */
    private static class TestInfo implements JIPipeNodeInfo {
        private final String name;
        private final JIPipeNodeTaxonomyFunction primary;
        private final Set<JIPipeNodeTaxonomyFunction> secondary;

        private TestInfo(String name, JIPipeNodeTaxonomyFunction primary,
                         Set<JIPipeNodeTaxonomyFunction> secondary) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public String getId() {
            return "stub:" + name;
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
            return name;
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
            return primary;
        }

        @Override
        public Set<JIPipeNodeTaxonomyFunction> getSecondaryFunctions() {
            return secondary;
        }
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    /** A converted info: primary leaf "Filtering" under root "Images" + one secondary. */
    private static JIPipeNodeInfo convertedInfo() {
        TestFunction root = new TestFunction("jipipe:function:images", "Images", 186f / 360f,
                null, true, true, true, true);
        // Distinct flag values so the assertions are meaningful.
        TestFunction primary = new TestFunction("jipipe:function:images:filtering", "Filtering", null,
                root, true, false, true, false);
        TestFunction secondary = new TestFunction("jipipe:function:statistics", "Statistics", 300f / 360f,
                null, false, false, false, true);
        return new TestInfo("Gaussian Blur", primary, Set.of(secondary));
    }

    /** An unconverted info: no primary function (fn-null defensive state). */
    private static JIPipeNodeInfo unconvertedInfo() {
        return new TestInfo("My Node", null, Collections.emptySet());
    }

    // ---------------------------------------------------------------------
    // functionDisplayName
    // ---------------------------------------------------------------------

    @Test
    void functionDisplayNameConvertedUsesPrimaryFunctionName() {
        assertEquals("Filtering", TaxonomyView.functionDisplayName(convertedInfo()));
    }

    @Test
    void functionDisplayNameUnconvertedFallsBackToInternal() {
        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): the display name is the constant
        // "Internal", not a legacy category name.
        assertEquals("Internal", TaxonomyView.functionDisplayName(unconvertedInfo()));
    }

    // ---------------------------------------------------------------------
    // locationPath
    // ---------------------------------------------------------------------

    @Test
    void locationPathConvertedIsFunctionChainPlusNodeName() {
        assertEquals("Images > Filtering > Gaussian Blur",
                TaxonomyView.locationPath(convertedInfo()));
    }

    @Test
    void locationPathUnconvertedIsNodeNameOnly() {
        // fn-null defensive state: the location path is just the node name
        // (no category / menu path branch prefix).
        assertEquals("My Node", TaxonomyView.locationPath(unconvertedInfo()));
    }

    // ---------------------------------------------------------------------
    // behaviorFlags
    // ---------------------------------------------------------------------

    @Test
    void behaviorFlagsConvertedReadPrimaryFunctionFlags() {
        BehaviorFlags flags = TaxonomyView.behaviorFlags(convertedInfo());
        assertEquals(true, flags.isVisibleInPipeline());
        assertEquals(false, flags.isVisibleInCompartments());
        assertEquals(true, flags.canExtract());
        assertEquals(false, flags.isRunnable());
    }

    @Test
    void behaviorFlagsUnconvertedFallsBackToSafeDefaults() {
        // fn-null defensive state: the safe defaults (all true) — the
        // 21a null-category default. canExtract / isRunnable mirror the
        // taxonomy defaults; the two visibility flags are permissive true.
        BehaviorFlags flags = TaxonomyView.behaviorFlags(unconvertedInfo());
        assertEquals(true, flags.isVisibleInPipeline());
        assertEquals(true, flags.isVisibleInCompartments());
        assertEquals(true, flags.canExtract());
        assertEquals(true, flags.isRunnable());
    }

    // ---------------------------------------------------------------------
    // searchIds
    // ---------------------------------------------------------------------

    @Test
    void searchIdsConvertedIsPrimaryPlusSecondaryFunctionIds() {
        Set<String> ids = TaxonomyView.searchIds(convertedInfo());
        assertEquals(Set.of("jipipe:function:images:filtering", "jipipe:function:statistics"), ids);
    }

    @Test
    void searchIdsUnconvertedIsEmpty() {
        // fn-null defensive state: no legacy category id — the search ids are
        // empty (the new defensive default).
        Set<String> ids = TaxonomyView.searchIds(unconvertedInfo());
        assertTrue(ids.isEmpty());
    }

    @Test
    void searchIdsConvertedWithNoSecondaryIsOnlyPrimaryId() {
        TestFunction root = new TestFunction("jipipe:function:images", "Images", 186f / 360f,
                null, true, true, true, true);
        TestFunction primary = new TestFunction("jipipe:function:images:filtering", "Filtering", null,
                root, true, false, true, false);
        JIPipeNodeInfo info = new TestInfo("Solo", primary, Collections.emptySet());
        assertEquals(Set.of("jipipe:function:images:filtering"), TaxonomyView.searchIds(info));
    }
}
