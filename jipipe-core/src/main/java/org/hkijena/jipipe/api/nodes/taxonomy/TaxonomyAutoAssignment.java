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

import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * Auto-assignment of taxonomy functions and spatial dimensions for the
 * runtime-discovered (dynamic) nodes, i.e. the auto-imported ImageJ2 op nodes
 * and CLIJ command nodes.
 * <p>
 * The primary function is resolved by the taxonomy service: an exact
 * resource mapping to a stable function id first, then weighted name
 * inference ({@code JIPipeNodeTaxonomyServiceComponent#inferFunction}) as
 * the fallback. This class keeps the mapping/tag resources and the spatial
 * dimensions, which are <em>derived</em> from the node's input profile - an
 * unconstrained image input ({@link JIPipeNodeTaxonomyInputProfile#ANY})
 * yields both 2D and 3D. The framework is decided by the calling
 * {@link JIPipeNodeInfo} (ImageJ2 / CLIJ); this helper does not set it.
 * <p>
 * All methods are pure and static: they never throw and require no JIPipe
 * instance, so they are unit-testable without the ImageJ context.
 */
public final class TaxonomyAutoAssignment {

    private TaxonomyAutoAssignment() {
    }

    // ------------------------------------------------------------------
    // Resource-file mapping (JSON) for auto-discovered nodes
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Resource-file tag mapping (JSON) for auto-discovered nodes
    // ------------------------------------------------------------------

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

    /**
     * A mapping from auto-discovered node keys (an op name, a command name,
     * or an op namespace) to a taxonomy function id, loaded from a JSON
     * resource bundled with the integrating plugin (e.g.
     * {@code taxonomy/ij2-op-mapping.json}).
     * <p>
     * The mapping is loaded lazily once per resource and cached; a missing or
     * unreadable resource yields an <em>empty</em> mapping (never an
     * exception - auto-discovered registration must not crash the startup),
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
         * semantics without a leading slash - e.g.
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
     * ({@code TaxonomyResolver.enrichNodeInfos}) so every registered node -
     * not only the annotated ones - carries its audited tags.
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
}
