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

package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInferencePattern;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.MiscellaneousFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.*;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.misc.MiscellaneousUnassignedFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-static tests of the taxonomy name-inference scorer: no JIPipe boot,
 * no live registry - the index and the id lookup are handed in, so these pin
 * the scoring rules (summation, tie-break, soft-fail) on their own.
 */
class NodeTaxonomyInferenceTest {

    private static final List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> INDEX = buildIndex();
    // (the brief sketched this as a static method, but scoreName takes a
    // Function parameter - a bare method name is not a valid expression, so
    // it is a static final field; every call site is unchanged)
    private static final Function<String, JIPipeNodeTaxonomyFunction> lookup = id ->
            switch (id) {
                case "jipipe:function:segmentation" -> new SegmentationFunction();
                case "jipipe:function:filtering" -> new FilteringFunction();
                case "jipipe:function:color-stack" -> new ColorStackFunction();
                case "jipipe:function:merging-splitting" -> new MergingSplittingFunction();
                case "jipipe:function:misc" -> new MiscellaneousFunction();
                default -> null; // unregistered id -> soft fail
            };
    private JIPipeNodeTaxonomyServiceComponent freshComponent;

    private static List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> buildIndex() {
        // a slice of the real aggregate index: enough to exercise every rule
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> list = new ArrayList<>();
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("segment", "jipipe:function:segmentation", 9f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("filter", "jipipe:function:filtering", 8f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("median", "jipipe:function:filtering", 1f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("color", "jipipe:function:color-stack", 2f));
        list.add(new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("stack", "jipipe:function:merging-splitting", 3f));
        return list;
    }

    @Test
    void singleKeywordWins() {
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("segment cells", INDEX, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:segmentation", fn.getId());
    }

    @Test
    void summedWeightsOfOneFunctionBeatNothingElse() {
        // "median filter" matches median(1) + filter(8) = filtering 9; no other
        // keyword matches, so filtering wins on the summed score
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("median filter", INDEX, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:filtering", fn.getId());
    }

    @Test
    void noMatchYieldsNull() {
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("some totally unknown operation xyz", INDEX, lookup));
    }

    @Test
    void nullAndBlankNamesYieldNull() {
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName(null, INDEX, lookup));
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("   ", INDEX, lookup));
    }

    @Test
    void tieBreaksToLexicographicallySmallestId() {
        // "color stack" matches color->color-stack(2) and stack->merging-splitting(3)?
        // No: 3 > 2, so build an explicit tie instead:
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> tieIndex = List.of(
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("x", "jipipe:function:merging-splitting", 5f),
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("x", "jipipe:function:color-stack", 5f));
        JIPipeNodeTaxonomyFunction fn =
                JIPipeNodeTaxonomyServiceComponent.scoreName("xx", tieIndex, lookup);
        assertNotNull(fn);
        assertEquals("jipipe:function:color-stack", fn.getId(), "tie -> smallest id");
    }

    // ------------------------------------------------------------------
    // Instance-level inference on a fresh, un-booted component
    // (new JIPipeNodeTaxonomyServiceComponent(null) is legal for these
    // paths: the service reference is only used for progress logging,
    // which register/unregister/inferFunction do not touch)
    // ------------------------------------------------------------------

    @Test
    void unregisteredFunctionInIndexIsSkippedNotThrown() {
        List<JIPipeNodeTaxonomyServiceComponent.IndexedKeyword> ghostIndex = List.of(
                new JIPipeNodeTaxonomyServiceComponent.IndexedKeyword("segment", "jipipe:function:ghost", 9f));
        assertNull(JIPipeNodeTaxonomyServiceComponent.scoreName("segment", ghostIndex, lookup));
    }

    @BeforeEach
    void createFreshComponent() {
        freshComponent = new JIPipeNodeTaxonomyServiceComponent(null);
        // The 8 pattern-bearing functions + the two miscellaneous functions,
        // registered flat (childrenOf/validation are not exercised here).
        for (JIPipeNodeTaxonomyFunction function : new JIPipeNodeTaxonomyFunction[]{
                new SegmentationFunction(),
                new FilteringFunction(),
                new ThresholdingFunction(),
                new ResamplingFunction(),
                new PreprocessingFunction(),
                new MeasurementsFunction(),
                new MergingSplittingFunction(),
                new ColorStackFunction(),
                new MiscellaneousFunction(),
                new MiscellaneousUnassignedFunction()
        }) {
            freshComponent.registerFunction(function);
        }
    }

    @AfterEach
    void disposeFreshComponent() {
        freshComponent = null;
    }

    @Test
    void parityThreeDGaussianFilterIsFiltering() {
        assertEquals(FilteringFunction.class,
                freshComponent.inferFunction("3D Gaussian filter").getClass());
    }

    // (a) parity corpus - the exact names of the retired functionForName tests

    @Test
    void parityMedianFilterIsFiltering() {
        assertEquals(FilteringFunction.class,
                freshComponent.inferFunction("Median filter").getClass());
    }

    @Test
    void paritySegmentCellsIsSegmentation() {
        assertEquals(SegmentationFunction.class,
                freshComponent.inferFunction("Segment cells").getClass());
    }

    @Test
    void parityResampleIsResampling() {
        assertEquals(ResamplingFunction.class,
                freshComponent.inferFunction("Resample").getClass());
    }

    @Test
    void parityColorConvertIsColorStack() {
        assertEquals(ColorStackFunction.class,
                freshComponent.inferFunction("Color convert").getClass());
    }

    @Test
    void unknownNameFallsBackToMiscellaneousUnassigned() {
        freshComponent.setInferenceFallback(new MiscellaneousUnassignedFunction());
        assertEquals(MiscellaneousUnassignedFunction.class,
                freshComponent.inferFunction("Some totally unknown operation xyz").getClass());
    }

    // (b) THE BEHAVIOR CHANGE - the fallback is MiscellaneousUnassignedFunction

    @Test
    void nameNotInPatternsFallsBackToMiscellaneousUnassigned() {
        // "watershed" is deliberately NOT in any function's patterns
        freshComponent.setInferenceFallback(new MiscellaneousUnassignedFunction());
        assertEquals(MiscellaneousUnassignedFunction.class,
                freshComponent.inferFunction("Watershed").getClass());
    }

    @Test
    void nullNameFallsBackToMiscellaneousUnassignedWithoutThrowing() {
        freshComponent.setInferenceFallback(new MiscellaneousUnassignedFunction());
        assertEquals(MiscellaneousUnassignedFunction.class,
                freshComponent.inferFunction(null).getClass());
    }

    @Test
    void unregisteredFallbackYieldsNullWithoutThrowing() {
        // set the fallback's id, then unregister its function: soft fail
        freshComponent.setInferenceFallback(new MiscellaneousUnassignedFunction());
        freshComponent.unregisterFunction(MiscellaneousUnassignedFunction.ID);
        assertNull(freshComponent.inferFunction("Some totally unknown operation xyz"));
    }

    @Test
    void registeredFunctionIsInferredAndResolvable() {
        freshComponent.registerFunction(new ClusteryFunction());
        assertEquals("test:function:clustery",
                freshComponent.inferFunction("clustery thing").getId());
        assertNotNull(freshComponent.functionById("test:function:clustery"));
    }

    // (c) extensibility - a "second plugin" registers a new function at runtime

    @Test
    void unregisteredFunctionContributesNothingToInference() {
        freshComponent.registerFunction(new ClusteryFunction());
        freshComponent.unregisterFunction("test:function:clustery");
        // index rebuilt without the stale contribution: no registered function
        // matches "clustery" and no fallback is set -> null (no exception)
        assertNull(freshComponent.inferFunction("clustery thing"));
        assertNull(freshComponent.functionById("test:function:clustery"));
    }

    /**
     * A "second plugin" function: test-local class, declared patterns only.
     */
    private static final class ClusteryFunction implements JIPipeNodeTaxonomyFunction {
        @Override
        public String getId() {
            return "test:function:clustery";
        }

        @Override
        public String getName() {
            return "Clustery";
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
            return "Test-only extensibility function";
        }

        @Override
        public javax.swing.Icon getIcon() {
            return null;
        }

        @Override
        public java.util.Set<String> getTags() {
            return java.util.Set.of();
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return false;
        }

        @Override
        public List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns() {
            return List.of(new JIPipeNodeTaxonomyInferencePattern("clustery", 9f));
        }
    }
}
