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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.misc.MiscellaneousUnassignedFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile.*;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TaxonomyAutoAssignment}: the input-profile-derived spatial
 * dimensions and the JSON resource mappings for the runtime-discovered
 * IJ2/CLIJ nodes, plus the behavior-parity corpus of the weighted name
 * inference (now served by the taxonomy service: the display names of the
 * retired {@code functionForName} tests are inferred on the live registry,
 * whose fallback is {@link MiscellaneousUnassignedFunction} - the deliberate
 * 6.0.0 behavior change from the old Miscellaneous default).
 */
class TaxonomyAutoAssignmentTest {

    @BeforeAll
    static void boot() {
        // The name-inference tests run against the LIVE taxonomy service
        // (populated by TaxonomyPlugin, which also sets the inference
        // fallback). The launcher listener pre-patches ImageJ, so booting
        // here is safe.
        JIPipe.ensureInstance();
    }

    @Test
    void gaussianFilterNameMatchesFiltering() {
        // "filter"(8) + "gaussian"(1) both score filtering
        assertEquals(FilteringFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("3D Gaussian filter").getClass());
    }

    @Test
    void medianFilterNameMatchesFiltering() {
        // "filter"(8) + "median"(1) both score filtering
        assertEquals(FilteringFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("Median filter").getClass());
    }

    @Test
    void segmentNameMatchesSegmentation() {
        assertEquals(SegmentationFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("Segment cells").getClass());
    }

    @Test
    void unknownNameFallsBackToMiscellaneousUnassignedWithoutThrowing() {
        JIPipeNodeTaxonomyFunction function =
                JIPipe.getNodeTaxonomyService().inferFunction("Some totally unknown operation xyz");
        assertEquals(MiscellaneousUnassignedFunction.class, function.getClass());
    }

    @Test
    void resampleNameMatchesResampling() {
        assertEquals(ResamplingFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("Resample").getClass());
    }

    @Test
    void nameNotInPatternsFallsBackToMiscellaneousUnassigned() {
        // "watershed" is deliberately NOT in any function's patterns:
        // asserts the fallback branch (the 6.0.0 behavior change)
        assertEquals(MiscellaneousUnassignedFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("Watershed").getClass());
    }

    @Test
    void colorNameMatchesColorStack() {
        assertEquals(ColorStackFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction("Color convert").getClass());
    }

    @Test
    void nullNameFallsBackToMiscellaneousUnassignedWithoutThrowing() {
        assertEquals(MiscellaneousUnassignedFunction.class,
                JIPipe.getNodeTaxonomyService().inferFunction(null).getClass());
    }

    @Test
    void anyProfileMapsToBothSpatialDimensions() {
        assertEquals(Set.of(SPATIAL_2D, SPATIAL_3D), TaxonomyAutoAssignment.spatialFor(ANY));
    }

    @Test
    void plane2dProfileMapsToSpatial2dOnly() {
        assertEquals(Set.of(SPATIAL_2D), TaxonomyAutoAssignment.spatialFor(PLANE_2D));
    }

    @Test
    void volume3dProfileMapsToSpatial3dOnly() {
        assertEquals(Set.of(SPATIAL_3D), TaxonomyAutoAssignment.spatialFor(VOLUME_3D));
    }

    @Test
    void naProfileMapsToEmptySet() {
        assertTrue(TaxonomyAutoAssignment.spatialFor(NA).isEmpty());
    }

    @Test
    void nullProfileMapsToEmptySetWithoutThrowing() {
        assertTrue(TaxonomyAutoAssignment.spatialFor(null).isEmpty());
    }

    @Test
    void firstImageInputAfterNonImageInputIsUsed() {
        // The NA first class (non-image input) is skipped; the ANY image class decides.
        Function<Class<?>, JIPipeNodeTaxonomyInputProfile> byClass = cls ->
                cls == String.class ? NA : ANY;
        assertEquals(Set.of(SPATIAL_2D, SPATIAL_3D),
                TaxonomyAutoAssignment.spatialForFirstImageInput(List.of(String.class, Integer.class), byClass));
    }

    @Test
    void onlyNonImageInputsYieldEmptySpatial() {
        Function<Class<?>, JIPipeNodeTaxonomyInputProfile> byClass = cls -> NA;
        assertTrue(TaxonomyAutoAssignment.spatialForFirstImageInput(List.of(String.class), byClass).isEmpty());
    }

    @Test
    void emptyInputListYieldsEmptySpatial() {
        assertTrue(TaxonomyAutoAssignment.spatialForFirstImageInput(Collections.emptyList(), cls -> ANY).isEmpty());
    }

    @Test
    void plane2dFirstImageInputYieldsSpatial2d() {
        Function<Class<?>, JIPipeNodeTaxonomyInputProfile> byClass = cls -> PLANE_2D;
        assertEquals(Set.of(SPATIAL_2D),
                TaxonomyAutoAssignment.spatialForFirstImageInput(List.of(String.class), byClass));
    }

    @Test
    void nullInputListYieldsEmptySpatialWithoutThrowing() {
        assertTrue(TaxonomyAutoAssignment.spatialForFirstImageInput(null, cls -> ANY).isEmpty());
    }

    @Test
    void throwingLookupIsTreatedAsNaAndSkipped() {
        Function<Class<?>, JIPipeNodeTaxonomyInputProfile> throwing = cls -> {
            throw new IllegalStateException("boom");
        };
        assertTrue(TaxonomyAutoAssignment.spatialForFirstImageInput(List.of(String.class), throwing).isEmpty());
    }

    // ------------------------------------------------------------------
    // Resource-file mapping (full-registry classification audit)
    // ------------------------------------------------------------------

    @Test
    void missingResourceYieldsAnEmptyMapping() {
        TaxonomyAutoAssignment.ResourceMapping mapping =
                TaxonomyAutoAssignment.ResourceMapping.load(TaxonomyAutoAssignmentTest.class,
                        "taxonomy/does-not-exist.json");
        assertTrue(mapping.isEmpty());
        assertNull(mapping.functionIdOf("anything"));
    }

    @Test
    void tagMappingLoadsAndLooksUpTags() {
        // Tag audit resource loader: key -> tags, lowercase + trimmed
        TaxonomyAutoAssignment.TagMapping mapping =
                TaxonomyAutoAssignment.TagMapping.load(TaxonomyAutoAssignmentTest.class,
                        "taxonomy/test-tag-mapping.json");
        assertFalse(mapping.isEmpty());
        assertEquals(Set.of("segmentation", "watershed", "labeling"),
                mapping.tagsOf("CLIJ2 Watershed"));
        assertEquals(Set.of("filtering", "median"), mapping.tagsOf("CLIJ2 Median"));
        // Unmapped / null keys yield the empty set (never null)
        assertTrue(mapping.tagsOf("CLIJ2 Unmapped").isEmpty());
        assertTrue(mapping.tagsOf(null).isEmpty());
    }

    @Test
    void missingResourceYieldsAnEmptyTagMapping() {
        TaxonomyAutoAssignment.TagMapping mapping =
                TaxonomyAutoAssignment.TagMapping.load(TaxonomyAutoAssignmentTest.class,
                        "taxonomy/does-not-exist.json");
        assertTrue(mapping.isEmpty());
        assertTrue(mapping.tagsOf("anything").isEmpty());
    }
}
