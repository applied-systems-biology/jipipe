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

package org.hkijena.jipipe.api.nodes.taxonomy.functions;

import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the 13 top-level {@link JIPipeNodeTaxonomyFunction} branches:
 * instantiation, stable literal ids, table names, table hues (converted to
 * {@code 0..1}, {@code null} for the transparent Graph branch), pairwise hue
 * uniqueness among the colored branches, top-level-ness, and a clean
 * {@link FunctionRegistry#validate()} after registering all of them.
 */
class TopLevelBranchesTest {

    /**
     * The 13 top-level branch instances, one per class.
     */
    private static List<JIPipeNodeTaxonomyFunction> all() {
        return List.of(
            new DataSourcesFunction(),
            new ImagesFunction(),
            new RoisFunction(),
            new FilamentsFunction(),
            new TablesFunction(),
            new StatisticsFunction(),
            new TrackingFunction(),
            new AnnotationsFunction(),
            new MachineLearningFunction(),
            new PlottingFunction(),
            new ExportFunction(),
            new GraphFunction(),
            new MiscellaneousFunction()
        );
    }

    @Test
    void allThirteenInstantiate() {
        assertEquals(13, all().size());
        for (JIPipeNodeTaxonomyFunction function : all()) {
            assertTrue(function instanceof JIPipeNodeTaxonomyFunction);
        }
    }

    @Test
    void idsAreStableLiterals() {
        assertEquals("jipipe:function:data", new DataSourcesFunction().getId());
        assertEquals("jipipe:function:images", new ImagesFunction().getId());
        assertEquals("jipipe:function:rois", new RoisFunction().getId());
        assertEquals("jipipe:function:filaments", new FilamentsFunction().getId());
        assertEquals("jipipe:function:tables", new TablesFunction().getId());
        assertEquals("jipipe:function:statistics", new StatisticsFunction().getId());
        assertEquals("jipipe:function:tracking", new TrackingFunction().getId());
        assertEquals("jipipe:function:annotations", new AnnotationsFunction().getId());
        assertEquals("jipipe:function:machine-learning", new MachineLearningFunction().getId());
        assertEquals("jipipe:function:plotting", new PlottingFunction().getId());
        assertEquals("jipipe:function:export", new ExportFunction().getId());
        assertEquals("jipipe:function:graph", new GraphFunction().getId());
        assertEquals("jipipe:function:misc", new MiscellaneousFunction().getId());
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = all().stream()
            .map(JIPipeNodeTaxonomyFunction::getId)
            .collect(Collectors.toSet());
        assertEquals(13, ids.size());
    }

    @Test
    void namesMatchTable() {
        assertEquals("Data Sources", new DataSourcesFunction().getName());
        assertEquals("Images", new ImagesFunction().getName());
        assertEquals("ROIs", new RoisFunction().getName());
        assertEquals("Filaments", new FilamentsFunction().getName());
        assertEquals("Tables", new TablesFunction().getName());
        assertEquals("Statistics", new StatisticsFunction().getName());
        assertEquals("Tracking", new TrackingFunction().getName());
        assertEquals("Annotations", new AnnotationsFunction().getName());
        assertEquals("Machine learning", new MachineLearningFunction().getName());
        assertEquals("Plotting", new PlottingFunction().getName());
        assertEquals("Export", new ExportFunction().getName());
        assertEquals("Graph / Project", new GraphFunction().getName());
        assertEquals("Miscellaneous", new MiscellaneousFunction().getName());
    }

    @Test
    void huesMatchTable() {
        assertEquals(136.0f / 360.0f, new DataSourcesFunction().getColorHue());
        assertEquals(186.0f / 360.0f, new ImagesFunction().getColorHue());
        assertEquals(0.0f / 360.0f, new RoisFunction().getColorHue());
        assertEquals(40.0f / 360.0f, new FilamentsFunction().getColorHue());
        assertEquals(216.0f / 360.0f, new TablesFunction().getColorHue());
        assertEquals(60.0f / 360.0f, new StatisticsFunction().getColorHue());
        assertEquals(165.0f / 360.0f, new TrackingFunction().getColorHue());
        assertEquals(264.0f / 360.0f, new AnnotationsFunction().getColorHue());
        assertEquals(200.0f / 360.0f, new MachineLearningFunction().getColorHue());
        assertEquals(155.0f / 360.0f, new PlottingFunction().getColorHue());
        assertEquals(22.0f / 360.0f, new ExportFunction().getColorHue());
        assertNull(new GraphFunction().getColorHue());
        assertEquals(50.0f / 360.0f, new MiscellaneousFunction().getColorHue());
    }

    @Test
    void coloredHuesArePairwiseUnique() {
        // The 12 colored branches must not share a hue (Review Focus #3);
        // Graph is transparent (null) and excluded.
        Set<Float> hues = all().stream()
            .map(JIPipeNodeTaxonomyFunction::getColorHue)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        assertEquals(12, hues.size());
    }

    @Test
    void allAreTopLevel() {
        for (JIPipeNodeTaxonomyFunction function : all()) {
            assertNull(function.getParent(),
                "Expected " + function.getClass().getSimpleName() + " to be top-level (null parent)");
        }
    }

    @Test
    void registryValidateReturnsEmpty() {
        FunctionRegistry registry = new FunctionRegistry();
        for (JIPipeNodeTaxonomyFunction function : all()) {
            registry.register(function);
        }
        Set<String> errors = registry.validate();
        assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
    }
}
