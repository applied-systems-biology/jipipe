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

import org.hkijena.jipipe.api.nodes.taxonomy.functions.MiscellaneousFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile.ANY;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile.NA;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile.PLANE_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile.VOLUME_3D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure tests for {@link TaxonomyAutoAssignment} (Task 20): the name heuristic
 * for the primary function and the input-profile-derived spatial dimensions
 * of the runtime-discovered IJ2/CLIJ nodes. No JIPipe instance is required.
 */
class TaxonomyAutoAssignmentTest {

    @Test
    void gaussianFilterNameMatchesFiltering() {
        // "filter" (rule 2) wins before "gaussian" (rule 9)
        assertEquals(FilteringFunction.class, TaxonomyAutoAssignment.functionForName("3D Gaussian filter").getClass());
    }

    @Test
    void medianFilterNameMatchesFiltering() {
        // "filter" (rule 2) is reached before "median" (rule 9)
        assertEquals(FilteringFunction.class, TaxonomyAutoAssignment.functionForName("Median filter").getClass());
    }

    @Test
    void segmentNameMatchesSegmentation() {
        assertEquals(SegmentationFunction.class, TaxonomyAutoAssignment.functionForName("Segment cells").getClass());
    }

    @Test
    void unknownNameFallsBackToMiscellaneousWithoutThrowing() {
        JIPipeNodeTaxonomyFunction function = TaxonomyAutoAssignment.functionForName("Some totally unknown operation xyz");
        assertEquals(MiscellaneousFunction.class, function.getClass());
    }

    @Test
    void resampleNameMatchesResampling() {
        assertEquals(ResamplingFunction.class, TaxonomyAutoAssignment.functionForName("Resample").getClass());
    }

    @Test
    void nameNotInTableFallsBackToMiscellaneous() {
        // "watershed" is deliberately NOT in the table: asserts the default branch
        assertEquals(MiscellaneousFunction.class, TaxonomyAutoAssignment.functionForName("Watershed").getClass());
    }

    @Test
    void colorNameMatchesColorStack() {
        assertEquals(ColorStackFunction.class, TaxonomyAutoAssignment.functionForName("Color convert").getClass());
    }

    @Test
    void nullNameFallsBackToMiscellaneousWithoutThrowing() {
        assertEquals(MiscellaneousFunction.class, TaxonomyAutoAssignment.functionForName(null).getClass());
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
}
