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

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory registry of {@link JIPipeNodeTaxonomyFunction}s, forming the
 * function tree of the node taxonomy.
 * <p>
 * The registry is deliberately plain and theme-independent: hue resolution
 * ({@link #baseHue(JIPipeNodeTaxonomyFunction)}, {@link #shadeDepth(JIPipeNodeTaxonomyFunction)})
 * and shading ({@link #shadeHsb(float, float, float, int)}) are pure functions
 * over the function tree, so the desktop layer can compose the result with
 * the current theme style's saturation/brightness at display time.
 * <p>
 * The registry is not thread-safe; it is expected to be fully populated
 * during startup before being exposed to the UI.
 */
public class FunctionRegistry {

    private final Map<String, JIPipeNodeTaxonomyFunction> byId = new HashMap<>();

    /**
     * Monotonic counter bumped on every structural mutation; used by the
     * taxonomy service's inference index to detect direct-registry mutations.
     */
    private final AtomicLong version = new AtomicLong();

    /**
     * The fixed HSB shading rule for taxonomy tree depth. The hue is unchanged;
     * each level of depth below the hue owner raises brightness by {@code 0.06}
     * (capped at {@code 0.95}) and lowers saturation by {@code 0.04}
     * (floored at {@code 0.05}).
     * <p>
     * This is a pure function so that the registry stays theme-independent:
     * callers compose it with the current theme style's saturation/brightness.
     * With {@code depth = 0} the rule is the identity: hue, saturation and
     * brightness are returned unchanged.
     *
     * @param hue   the base hue, unchanged
     * @param sat   the base saturation
     * @param bri   the base brightness
     * @param depth the depth below the hue owner; {@code 0} is the identity
     * @return {@code {hue, shadedSaturation, shadedBrightness}}
     */
    public static float[] shadeHsb(float hue, float sat, float bri, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be >= 0, got " + depth);
        }
        float shadedSat = Math.max(0.05f, sat - 0.04f * depth);
        float shadedBri = Math.min(0.95f, bri + 0.06f * depth);
        return new float[]{hue, shadedSat, shadedBri};
    }

    /**
     * Extracts the ids that form a cycle: the suffix of {@code path} from the
     * first occurrence of {@code repeatedId} to the end, in walk order.
     */
    private static String cycleIds(Set<String> path, String repeatedId) {
        List<String> cycle = new ArrayList<>();
        boolean inCycle = false;
        for (String id : path) {
            if (id.equals(repeatedId)) {
                inCycle = true;
            }
            if (inCycle) {
                cycle.add(id);
            }
        }
        return String.join(" → ", cycle);
    }

    /**
     * Registers a function. Re-registering an existing id replaces the previous function.
     *
     * @param function the function to register
     */
    public void register(JIPipeNodeTaxonomyFunction function) {
        byId.put(function.getId(), function);
        version.incrementAndGet();
    }

    /**
     * Unregisters a function. Does nothing if the id is not registered.
     *
     * @param id the id of the function to remove
     * @return the removed function, or {@code null} if none was registered under that id
     */
    @Nullable
    public JIPipeNodeTaxonomyFunction unregister(String id) {
        version.incrementAndGet();
        return byId.remove(id);
    }

    /**
     * @return the current mutation version (see {@link #version}); the value
     * increases monotonically with every {@link #register} / {@link #unregister}
     */
    public long mutationVersion() {
        return version.get();
    }

    /**
     * @param id the id of the function to look up
     * @return the registered function with that id, or {@code null} if none is registered
     */
    @Nullable
    public JIPipeNodeTaxonomyFunction byId(String id) {
        return byId.get(id);
    }

    /**
     * @return an unmodifiable view of all registered functions
     */
    public Set<JIPipeNodeTaxonomyFunction> all() {
        return new LinkedHashSet<>(byId.values());
    }

    /**
     * @return an unmodifiable set of all registered top-level functions (those with no parent)
     */
    public Set<JIPipeNodeTaxonomyFunction> topLevel() {
        Set<JIPipeNodeTaxonomyFunction> result = new LinkedHashSet<>();
        for (JIPipeNodeTaxonomyFunction function : byId.values()) {
            if (function.getParent() == null) {
                result.add(function);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * @param parent the parent function
     * @return the registered direct children of the given function, ordered by
     * {@link JIPipeNodeTaxonomyFunction#getName()} for deterministic iteration
     * (the underlying {@link HashMap} does not guarantee a stable order)
     */
    public List<JIPipeNodeTaxonomyFunction> children(JIPipeNodeTaxonomyFunction parent) {
        List<JIPipeNodeTaxonomyFunction> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        // Match on the parent's id, not object identity. Functions are value
        // objects: a child's getParent() typically returns a *fresh* instance
        // (e.g. {@code new DataSourcesFunction()} inside ImageDataFunction),
        // which is equal by id to the registered top-level but not the same
        // reference. Reference equality would therefore report every branch
        // as childless, flattening the tree (see the node browser's tree).
        String parentId = parent.getId();
        for (JIPipeNodeTaxonomyFunction function : byId.values()) {
            JIPipeNodeTaxonomyFunction parentFunction = function.getParent();
            if (parentFunction != null && parentId.equals(parentFunction.getId())) {
                result.add(function);
            }
        }
        result.sort(Comparator.comparing(JIPipeNodeTaxonomyFunction::getName));
        return result;
    }

    /**
     * Resolves the base hue of a function: the nearest ancestor (including the
     * function itself) whose {@link JIPipeNodeTaxonomyFunction#getColorHue()} is
     * non-null.
     *
     * @param function the function
     * @return the nearest non-null hue in the parent chain
     * @throws IllegalStateException if the chain contains a cycle or ends without a hue
     */
    public float baseHue(JIPipeNodeTaxonomyFunction function) {
        return hueOwnerChain(function).owner.getColorHue();
    }

    /**
     * Resolves how far below the hue owner a function is, in number of tree edges.
     *
     * @param function the function
     * @return the number of edges below the nearest hue-owning ancestor;
     * 0 for the hue owner itself
     * @throws IllegalStateException if the chain contains a cycle or ends without a hue
     */
    public int shadeDepth(JIPipeNodeTaxonomyFunction function) {
        return hueOwnerChain(function).depth;
    }

    private HueOwnerChain hueOwnerChain(JIPipeNodeTaxonomyFunction function) {
        Set<String> visited = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction current = function;
        int depth = 0;
        while (current.getColorHue() == null) {
            // A valid parent chain visits each id at most once; a repeat means a cycle.
            if (!visited.add(current.getId())) {
                throw new IllegalStateException(
                        "Cycle in the parent chain of taxonomy function " + current.getId() + ": " + cycleIds(visited, current.getId()));
            }
            JIPipeNodeTaxonomyFunction parent = current.getParent();
            if (parent == null) {
                throw new IllegalStateException(
                        "No color hue defined in the parent chain of taxonomy function " + current.getId());
            }
            current = parent;
            depth++;
        }
        return new HueOwnerChain(current, depth);
    }

    /**
     * Validates the registered function tree. Cheap enough to run at startup
     * for the full function set: each check walks parent chains with a visited
     * set, so the total cost is O(n²) in the number of functions, which is
     * negligible for a few dozen entries.
     * <p>
     * Reported errors:
     * <ul>
     * <li>cycles in a parent chain (listing the ids that form the cycle),</li>
     * <li>functions that reference a parent that is not registered (unreachable leaves),</li>
     * <li>top-level functions that share a color hue.</li>
     * </ul>
     * <p>
     * A top-level function whose {@link JIPipeNodeTaxonomyFunction#getColorHue() hue}
     * is {@code null} is a <em>transparent</em> branch (rendered with the theme's
     * panel background, matching the legacy negative-hue case). It is valid and
     * does not participate in the hue-uniqueness check; the hue is resolved at
     * display time by the consumer.
     *
     * @return human-readable error strings; empty if the registry is valid
     */
    public Set<String> validate() {
        Set<String> errors = new LinkedHashSet<>();
        for (JIPipeNodeTaxonomyFunction function : byId.values()) {
            Set<String> path = new LinkedHashSet<>();
            JIPipeNodeTaxonomyFunction current = function;
            while (current != null) {
                if (!path.add(current.getId())) {
                    errors.add("Cycle in taxonomy function parent chain: " + cycleIds(path, current.getId()));
                    break;
                }
                JIPipeNodeTaxonomyFunction parent = current.getParent();
                if (parent == null) {
                    break;
                }
                if (!byId.containsKey(parent.getId())) {
                    errors.add("Taxonomy function " + current.getId() + " references unregistered parent " + parent.getId());
                    break;
                }
                current = parent;
            }
        }

        // Among the <em>colored</em> top-level functions, no two may share a hue.
        // A transparent top-level (hue {@code null}) is valid and skipped here;
        // consumers render it with the theme's panel background.
        Map<Float, String> hueOwners = new HashMap<>();
        for (JIPipeNodeTaxonomyFunction function : byId.values()) {
            if (function.getParent() != null) {
                continue;
            }
            Float hue = function.getColorHue();
            if (hue == null) {
                continue;
            }
            String previous = hueOwners.putIfAbsent(hue, function.getId());
            if (previous != null) {
                errors.add("Duplicate top-level color hue " + hue + " in taxonomy functions " + previous + " and " + function.getId());
            }
        }
        return errors;
    }

    /**
     * The hue-owning ancestor and the number of tree edges from the original
     * function to it.
     */
    private record HueOwnerChain(JIPipeNodeTaxonomyFunction owner, int depth) {
    }
}
