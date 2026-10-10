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
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView.BehaviorFlags;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Minimal {@link JIPipeNodeTypeCategory} stub with a controllable id,
     * name, hue and the four behavior flags.
     */
    private static class TestCategory implements JIPipeNodeTypeCategory {
        private final String id;
        private final String name;
        private final float hue;
        private final boolean pipeline;
        private final boolean compartments;
        private final boolean extract;
        private final boolean runnable;

        private TestCategory(String id, String name, float hue,
                             boolean pipeline, boolean compartments,
                             boolean extract, boolean runnable) {
            this.id = id;
            this.name = name;
            this.hue = hue;
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
        public String getDescription() {
            return id;
        }

        @Override
        public int getUIOrder() {
            return 0;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public float getColorHue() {
            return hue;
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
     * Minimal {@link JIPipeNodeInfo} stub. All taxonomy-relevant accessors
     * are injected; the unrelated accessors return harmless defaults so the
     * class never triggers JIPipe service lookups.
     */
    private static class TestInfo implements JIPipeNodeInfo {
        private final String name;
        private final JIPipeNodeTaxonomyFunction primary;
        private final Set<JIPipeNodeTaxonomyFunction> secondary;
        private final JIPipeNodeTypeCategory category;
        private final String menuPath;

        private TestInfo(String name, JIPipeNodeTaxonomyFunction primary,
                         Set<JIPipeNodeTaxonomyFunction> secondary,
                         JIPipeNodeTypeCategory category, String menuPath) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
            this.category = category;
            this.menuPath = menuPath;
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
        public String getMenuPath() {
            return menuPath;
        }

        @Override
        public JIPipeNodeTypeCategory getCategory() {
            return category;
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
        // The converted node still carries a legacy category (an
        // InternalNodeTypeCategory after 1b conversions), but the helper must
        // prefer the function. We give it a non-null legacy category on
        // purpose to prove the function branch wins.
        TestCategory legacy = new TestCategory("org.hkijena.jipipe:internal", "Internal", -1f,
                false, false, false, false);
        return new TestInfo("Gaussian Blur", primary, Set.of(secondary), legacy, "");
    }

    /** An unconverted info: no primary function, a legacy category + menu path. */
    private static JIPipeNodeInfo unconvertedInfo() {
        // Distinct flag values from the converted primary to prove the
        // category branch is read.
        TestCategory category = new TestCategory("org.hkijena.jipipe:images", "Images", 0.3f,
                false, true, false, true);
        return new TestInfo("My Node", null, Collections.emptySet(), category, "Filtering");
    }

    // ---------------------------------------------------------------------
    // functionDisplayName
    // ---------------------------------------------------------------------

    @Test
    void functionDisplayNameConvertedUsesPrimaryFunctionName() {
        assertEquals("Filtering", TaxonomyView.functionDisplayName(convertedInfo()));
    }

    @Test
    void functionDisplayNameUnconvertedUsesCategoryName() {
        assertEquals("Images", TaxonomyView.functionDisplayName(unconvertedInfo()));
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
    void locationPathUnconvertedIsCategoryMenuPathPlusNodeName() {
        assertEquals("Images > Filtering > My Node",
                TaxonomyView.locationPath(unconvertedInfo()));
    }

    @Test
    void locationPathUnconvertedOmitsEmptyMenuPathSegment() {
        TestCategory category = new TestCategory("org.hkijena.jipipe:images", "Images", 0.3f,
                false, true, false, true);
        JIPipeNodeInfo info = new TestInfo("My Node", null, Collections.emptySet(), category, "");
        assertEquals("Images > My Node", TaxonomyView.locationPath(info));
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
    void behaviorFlagsUnconvertedReadCategoryFlags() {
        BehaviorFlags flags = TaxonomyView.behaviorFlags(unconvertedInfo());
        assertEquals(false, flags.isVisibleInPipeline());
        assertEquals(true, flags.isVisibleInCompartments());
        assertEquals(false, flags.canExtract());
        assertEquals(true, flags.isRunnable());
    }

    @Test
    void behaviorFlagsNullCategoryAndFunctionFallsBackToSafeDefaults() {
        JIPipeNodeInfo info = new TestInfo("Lonely", null, Collections.emptySet(), null, "");
        BehaviorFlags flags = TaxonomyView.behaviorFlags(info);
        // canExtract / isRunnable mirror the JIPipeNodeTypeCategory interface
        // defaults (both true); the two visibility flags have no interface
        // default and default to the permissive true.
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
    void searchIdsUnconvertedIsExactlyLegacyCategoryId() {
        // The legacy transition shape: the raw category id, exactly the form
        // the node database builder historically stored (that categoryIds
        // facet was removed in task 25 — searchIds keeps the shape until the
        // legacy fallback itself goes away in tasks 26/27).
        Set<String> ids = TaxonomyView.searchIds(unconvertedInfo());
        assertEquals(Set.of("org.hkijena.jipipe:images"), ids);
    }

    @Test
    void searchIdsConvertedWithNoSecondaryIsOnlyPrimaryId() {
        TestFunction root = new TestFunction("jipipe:function:images", "Images", 186f / 360f,
                null, true, true, true, true);
        TestFunction primary = new TestFunction("jipipe:function:images:filtering", "Filtering", null,
                root, true, false, true, false);
        JIPipeNodeInfo info = new TestInfo("Solo", primary, Collections.emptySet(), null, "");
        assertEquals(Set.of("jipipe:function:images:filtering"), TaxonomyView.searchIds(info));
    }
}
