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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.PreprocessingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ThresholdingFunction;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Auto-assignment of taxonomy functions and spatial dimensions for the
 * runtime-discovered (dynamic) nodes, i.e. the auto-imported ImageJ2 op nodes
 * and CLIJ command nodes.
 * <p>
 * This refines the flat defaults introduced in 19.11b (every discovered node
 * got {@code MiscellaneousFunction} and no spatial dimensions): the primary
 * function is derived from a small, best-effort <em>name heuristic</em>
 * (default {@code Miscellaneous}), while the spatial dimensions are
 * <em>derived</em> from the node's input profile — an unconstrained image
 * input ({@link JIPipeNodeTaxonomyInputProfile#ANY}) yields both 2D and 3D.
 * The framework is decided by the calling {@link JIPipeNodeInfo}
 * (ImageJ2 / CLIJ); this helper does not set it.
 * <p>
 * All methods are pure and static: they never throw and require no JIPipe
 * instance, so they are unit-testable without the ImageJ context.
 */
public final class TaxonomyAutoAssignment {

    private TaxonomyAutoAssignment() {
    }

    /**
     * Heuristically assigns a primary taxonomy function from the display name
     * of a discovered node. The name is lowercased and matched against the
     * keyword table below; the <em>first</em> hit in priority order wins.
     * Names without a hit (and {@code null}/empty names) fall back to
     * {@link MiscellaneousFunction}. Never throws.
     * <p>
     * The ordering is deliberate: {@code "filter"} precedes the generic
     * morphological keywords ({@code "gaussian"}, {@code "median"}, ...) so
     * that e.g. "3D Gaussian filter" is classified as filtering, and
     * {@code "segment"} is first so that "Segment cells" is classified as
     * segmentation.
     *
     * @param name the node's display name, may be {@code null}
     * @return the assigned function; never {@code null}
     */
    public static JIPipeNodeTaxonomyFunction functionForName(String name) {
        if (name == null) {
            return new MiscellaneousFunction();
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("segment")) {
            return new SegmentationFunction();
        }
        if (lower.contains("filter")) {
            return new FilteringFunction();
        }
        if (lower.contains("threshold")) {
            return new ThresholdingFunction();
        }
        if (lower.contains("resample") || lower.contains("interpolat") || lower.contains("resize")
                || lower.contains("rescale") || lower.contains("upscale") || lower.contains("downscale")) {
            return new ResamplingFunction();
        }
        if (lower.contains("inpaint") || lower.contains("restore") || lower.contains("denoise")
                || lower.contains("deconvolv") || lower.contains("preprocess")
                || lower.contains("background") || lower.contains("flatten")) {
            return new PreprocessingFunction();
        }
        if (lower.contains("measure") || lower.contains("analyz") || lower.contains("profile")) {
            return new MeasurementsFunction();
        }
        if (lower.contains("merge") || lower.contains("split") || lower.contains("stack")
                || lower.contains("concatenat") || lower.contains("combine") || lower.contains("montage")) {
            return new MergingSplittingFunction();
        }
        if (lower.contains("color") || lower.contains("rgb") || lower.contains("hsl")
                || lower.contains("lut") || lower.contains("colormap")) {
            return new ColorStackFunction();
        }
        if (lower.contains("blur") || lower.contains("smooth") || lower.contains("erode")
                || lower.contains("dilate") || lower.contains("morpholog")
                || lower.contains("gaussian") || lower.contains("median")) {
            return new FilteringFunction();
        }
        return new MiscellaneousFunction();
    }

    /**
     * Maps an input profile to the spatial dimensions it implies:
     * {@code ANY} (unconstrained image data) ⇒ both 2D and 3D,
     * {@code PLANE_2D} ⇒ 2D, {@code VOLUME_3D} ⇒ 3D, {@code NA} ⇒ none.
     * Never throws ({@code null} ⇒ empty set).
     *
     * @param profile the derived input profile, may be {@code null}
     * @return the implied spatial dimensions; never {@code null}
     */
    public static Set<JIPipeNodeTaxonomySpatialDimension> spatialFor(JIPipeNodeTaxonomyInputProfile profile) {
        if (profile == null) {
            return Collections.emptySet();
        }
        switch (profile) {
            case ANY:
                return Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D, JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D);
            case PLANE_2D:
                return Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D);
            case VOLUME_3D:
                return Set.of(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D);
            default: // NA
                return Collections.emptySet();
        }
    }

    /**
     * Derives the spatial dimensions from the data classes of a node's input
     * slots, in declaration order: the <em>first</em> slot whose class maps
     * to an image profile ({@code ANY}, {@code PLANE_2D}, or {@code VOLUME_3D})
     * decides the node's spatial dimensions. Slots whose class maps to
     * {@code NA} (non-image inputs such as tables, ROIs, or files) are
     * skipped and confer no spatial dimension. If no input class is
     * image-profiled (or the list is {@code null}/empty), the result is empty.
     * <p>
     * The lookup is passed as a parameter so this method stays pure and
     * testable without a JIPipe instance; at the call sites it is the
     * registered providers of the node service component
     * ({@code JIPipe.getNodes()::inputProfileFor}), keeping the spatial
     * dimensions derived rather than heuristic. A lookup that throws for a
     * given class is treated as {@code NA} (skipped); this method never
     * throws.
     *
     * @param inputClasses  the data classes of the node's input slots, in order, may be {@code null}
     * @param profileLookup the function mapping a data class to its input profile
     * @return the spatial dimensions implied by the first image input slot; never {@code null}
     */
    public static Set<JIPipeNodeTaxonomySpatialDimension> spatialForFirstImageInput(List<Class<?>> inputClasses,
                                                                                      Function<Class<?>, JIPipeNodeTaxonomyInputProfile> profileLookup) {
        if (inputClasses == null || profileLookup == null) {
            return Collections.emptySet();
        }
        for (Class<?> inputClass : inputClasses) {
            JIPipeNodeTaxonomyInputProfile profile;
            try {
                profile = profileLookup.apply(inputClass);
            } catch (RuntimeException e) {
                // A failing lookup is treated as NA: the slot is skipped.
                continue;
            }
            if (profile == JIPipeNodeTaxonomyInputProfile.NA || profile == null) {
                continue;
            }
            Set<JIPipeNodeTaxonomySpatialDimension> spatial = spatialFor(profile);
            if (!spatial.isEmpty()) {
                return spatial;
            }
        }
        return Collections.emptySet();
    }
}
