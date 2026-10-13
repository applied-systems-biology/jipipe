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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageArithmeticFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageComparisonFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageLabelingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageMomentsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageNeighborMeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImagePixelManipulationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageProjectionsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageStatisticsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageTransformationsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ImageTypeConversionFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.PreprocessingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.SegmentationFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ThresholdingFunction;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    // ------------------------------------------------------------------
    // Resource-file mapping (JSON) for auto-discovered nodes
    // ------------------------------------------------------------------

    /**
     * A mapping from auto-discovered node keys (an op name, a command name,
     * or an op namespace) to a taxonomy function id, loaded from a JSON
     * resource bundled with the integrating plugin (e.g.
     * {@code taxonomy/ij2-op-mapping.json}).
     * <p>
     * The mapping is loaded lazily once per resource and cached; a missing or
     * unreadable resource yields an <em>empty</em> mapping (never an
     * exception — auto-discovered registration must not crash the startup),
     * and lookups then simply miss.
     * <p>
     * The values are function <em>ids</em> (stable string literals, the
     * registry's {@code jipipe:function:...} keys), not classes, so the
     * mapping resources can be regenerated by tooling (the classification
     * audit) without recompiling, and the plugin does not need to know the
     * concrete function classes.
     */
    public static final class ResourceMapping {

        private final Map<String, String> byKey = new HashMap<>();

        private ResourceMapping() {
        }

        /**
         * Loads a flat {@code key -> function-id} mapping from the given JSON
         * resource. Missing or invalid resources yield an empty mapping.
         *
         * @param resourceManager the plugin's resource manager
         * @param internalPath    the resource path (relative to the manager's base)
         * @return the loaded mapping (empty on any failure)
         */
        public static ResourceMapping load(JIPipeResourceManager resourceManager, String internalPath) {
            try (java.io.InputStream stream = resourceManager.getResourceAsStream(internalPath)) {
                return fromStream(stream);
            } catch (IOException | RuntimeException e) {
                return new ResourceMapping();
            }
        }

        /**
         * Loads a flat {@code key -> function-id} mapping from the JSON
         * resource at the given location, resolved <em>relative to the
         * supplied class's package</em> (Java's {@link Class#getResource}
         * semantics without a leading slash — e.g.
         * {@code ImageJ2Plugin.class} with {@code "taxonomy/ij2-op-ids.json"}
         * loads {@code .../imagej2/taxonomy/ij2-op-ids.json}). Missing or
         * invalid resources yield an empty mapping.
         *
         * @param resourceClass the class whose package holds the resource
         * @param resourcePath  the resource path relative to the class's package
         * @return the loaded mapping (empty on any failure)
         */
        public static ResourceMapping load(Class<?> resourceClass, String resourcePath) {
            try (java.io.InputStream stream = resourceClass.getResourceAsStream(resourcePath)) {
                return fromStream(stream);
            } catch (IOException | RuntimeException e) {
                return new ResourceMapping();
            }
        }

        private static ResourceMapping fromStream(java.io.InputStream stream) throws IOException {
            ResourceMapping mapping = new ResourceMapping();
            if (stream == null) {
                return mapping;
            }
            Map<String, String> raw = JsonUtils.getObjectMapper().readValue(stream, Map.class);
            mapping.byKey.putAll(raw);
            return mapping;
        }

        /**
         * @param key the mapping key (exact match)
         * @return the mapped function id, or {@code null} if not mapped
         */
        public String functionIdOf(String key) {
            return key == null ? null : byKey.get(key);
        }

        /**
         * @return whether this mapping is empty (nothing loaded)
         */
        public boolean isEmpty() {
            return byKey.isEmpty();
        }

        /**
         * @return an unmodifiable view of the raw key → function-id entries
         */
        public Map<String, String> entries() {
            return Collections.unmodifiableMap(byKey);
        }
    }

    // ------------------------------------------------------------------
    // Resource-file tag mapping (JSON) for auto-discovered nodes
    // ------------------------------------------------------------------

    /**
     * A mapping from auto-discovered node keys (an op name or a command
     * name) to a set of taxonomy tags, loaded from a JSON resource bundled
     * with the integrating plugin (e.g. {@code taxonomy/ij2-op-tags.json}:
     * {@code {"stats.sum": ["statistics", "projection"]}}).
     * <p>
     * Same crash-proof contract as {@link ResourceMapping}: a missing or
     * unreadable resource yields an empty mapping. The tags come from the
     * tag-audit pipeline (per-node generation + embedding-based compression
     * into a canonical vocabulary) and are surfaced by overriding
     * {@code JIPipeNodeInfo#getMergedTags()} at read time.
     */
    public static final class TagMapping {

        private final Map<String, Set<String>> byKey = new HashMap<>();

        private TagMapping() {
        }

        /**
         * Loads a flat {@code key -> tag list} mapping from the JSON
         * resource at the given location, resolved relative to the supplied
         * class's package (Java {@link Class#getResource} semantics).
         *
         * @param resourceClass the class whose package holds the resource
         * @param resourcePath  the resource path relative to the class's package
         * @return the loaded mapping (empty on any failure)
         */
        public static TagMapping load(Class<?> resourceClass, String resourcePath) {
            TagMapping mapping = new TagMapping();
            try (java.io.InputStream stream = resourceClass.getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    return mapping;
                }
                Map<String, List<String>> raw = JsonUtils.getObjectMapper()
                    .readValue(stream, JsonUtils.getObjectMapper().getTypeFactory()
                        .constructMapType(HashMap.class, String.class, List.class));
                for (Map.Entry<String, List<String>> e : raw.entrySet()) {
                    if (e.getValue() == null) {
                        continue;
                    }
                    Set<String> tags = new java.util.LinkedHashSet<>();
                    for (String tag : e.getValue()) {
                        if (tag != null && !tag.isBlank()) {
                            tags.add(tag.trim().toLowerCase(Locale.ROOT));
                        }
                    }
                    if (!tags.isEmpty()) {
                        mapping.byKey.put(e.getKey(), Collections.unmodifiableSet(tags));
                    }
                }
            } catch (IOException | RuntimeException e) {
                // Crash-proof: an unreadable resource simply yields no tags
                return new TagMapping();
            }
            return mapping;
        }

        /**
         * @param key the mapping key (exact match)
         * @return the mapped tags (possibly empty, never {@code null})
         */
        public Set<String> tagsOf(String key) {
            return key == null ? Set.of() : byKey.getOrDefault(key, Set.of());
        }

        /**
         * @return whether this mapping is empty (nothing loaded)
         */
        public boolean isEmpty() {
            return byKey.isEmpty();
        }
    }

    /**
     * Tag audit resource for the <em>curated</em> (Java-registered) nodes:
     * node id → tags, loaded once from the jipipe-core resource
     * {@code org/hkijena/jipipe/api/nodes/taxonomy/tags/curated-node-tags.json}
     * (generated by the tag-audit pipeline: per-node tag generation +
     * embedding-based compression into a canonical vocabulary).
     * <p>
     * The tags are merged in by the tag enrichment
     * ({@code TaxonomyResolver.enrichNodeInfos}) so every registered node —
     * not only the annotated ones — carries its audited tags.
     */
    public static final class CuratedNodeTags {

        private static final TagMapping MAPPING =
            TagMapping.load(CuratedNodeTags.class, "tags/curated-node-tags.json");

        private CuratedNodeTags() {
        }

        /**
         * @param nodeId the node type id
         * @return the audited tags (possibly empty, never {@code null})
         */
        public static Set<String> tagsOf(String nodeId) {
            return MAPPING.tagsOf(nodeId);
        }
    }

    /**
     * Resolves the primary taxonomy function for an auto-discovered node:
     * (1) the exact key in the plugin's JSON mapping resource, then
     * (2) the name-keyword heuristic, then
     * (3) Miscellaneous.
     *
     * @param mapping the plugin's resource mapping (may be empty, never null)
     * @param key     the mapping key (e.g. the ImageJ2 op name or the CLIJ
     *                command name)
     * @param name    the node's display name (the heuristic input)
     * @return the primary function, never {@code null}
     */
    public static JIPipeNodeTaxonomyFunction functionForResource(ResourceMapping mapping, String key, String name) {
        String id = mapping.functionIdOf(key);
        if (id != null) {
            JIPipeNodeTaxonomyFunction function = functionForId(id);
            if (function != null) {
                return function;
            }
        }
        return functionForName(name);
    }

    /**
     * Instantiates a taxonomy function from its stable registry id. Only the
     * functions reachable for auto-discovered nodes are supported (the seed
     * set plus the audit leaves); unknown ids yield {@code null} so callers
     * fall back to the heuristic.
     *
     * @param id the function id (e.g. {@code jipipe:function:image-arithmetic})
     * @return a new function instance, or {@code null} if the id is unknown
     */
    public static JIPipeNodeTaxonomyFunction functionForId(String id) {
        if (id == null) {
            return null;
        }
        switch (id) {
            case ImageArithmeticFunction.ID -> { return new ImageArithmeticFunction(); }
            case ImageComparisonFunction.ID -> { return new ImageComparisonFunction(); }
            case ImageProjectionsFunction.ID -> { return new ImageProjectionsFunction(); }
            case ImageTypeConversionFunction.ID -> { return new ImageTypeConversionFunction(); }
            case ImageLabelingFunction.ID -> { return new ImageLabelingFunction(); }
            case ImageStatisticsFunction.ID -> { return new ImageStatisticsFunction(); }
            case ImageMomentsFunction.ID -> { return new ImageMomentsFunction(); }
            case ImageTransformationsFunction.ID -> { return new ImageTransformationsFunction(); }
            case ImagePixelManipulationFunction.ID -> { return new ImagePixelManipulationFunction(); }
            case ImageNeighborMeasurementsFunction.ID -> { return new ImageNeighborMeasurementsFunction(); }
            case FilteringFunction.ID -> { return new FilteringFunction(); }
            case ThresholdingFunction.ID -> { return new ThresholdingFunction(); }
            case SegmentationFunction.ID -> { return new SegmentationFunction(); }
            case MeasurementsFunction.ID -> { return new MeasurementsFunction(); }
            case ResamplingFunction.ID -> { return new ResamplingFunction(); }
            case MergingSplittingFunction.ID -> { return new MergingSplittingFunction(); }
            case ColorStackFunction.ID -> { return new ColorStackFunction(); }
            case PreprocessingFunction.ID -> { return new PreprocessingFunction(); }
            case "jipipe:function:misc" -> { return new MiscellaneousFunction(); }
            default -> { return null; }
        }
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
