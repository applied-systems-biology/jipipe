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

package org.hkijena.jipipe.api.service.components.nodes;

import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeMutableNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.*;

/**
 * Resolves the taxonomy views of the node service component:
 * <ul>
 * <li>the {@link FunctionRegistry} (the curated function tree, populated
 * once by the taxonomy plugin),</li>
 * <li>the {@link FrameworkRegistry} (populated once by the taxonomy
 * plugin),</li>
 * <li>the family step graph, assembled from the nodes'
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s via a
 * {@link FamilyAssembler},</li>
 * <li>the input profile providers used to derive each node's
 * {@link JIPipeNodeTaxonomyInputProfile}.</li>
 * </ul>
 * <p>
 * Node registrations are recorded as node→function / node→framework
 * membership; the functions and frameworks themselves are NOT registered
 * here. The curated tree is registered once by the taxonomy plugin, and
 * {@link #validateTaxonomy()} flags any node that points at a function or
 * framework that is absent from those registries.
 * <p>
 * The class is deliberately plain (no Swing, no SciJava) so it can be
 * exercised without a full JIPipe instance; it is not thread-safe and is
 * expected to be populated during startup, mirroring the registries.
 */
public class TaxonomyResolver {

    private final FunctionRegistry functionRegistry = new FunctionRegistry();
    private final FrameworkRegistry frameworkRegistry = new FrameworkRegistry();
    private final List<InputProfileProvider> inputProfileProviders = new ArrayList<>();
    private final Map<String, JIPipeNodeTaxonomyFamily> familiesById = new LinkedHashMap<>();

    /** nodeId → the node's primary and secondary taxonomy functions */
    private final Map<String, Set<JIPipeNodeTaxonomyFunction>> nodeFunctions = new HashMap<>();

    /** nodeId → the node's taxonomy framework */
    private final Map<String, JIPipeNodeTaxonomyFramework> nodeFrameworks = new HashMap<>();

    /** Class → node ids (registration order), lazily rebuilt from the node registry */
    private Map<Class<?>, List<String>> classToNodeIds;

    private FamilyAssembler familyAssembler;

    /**
     * Creates a resolver with no node classes known yet; the
     * {@link FamilyAssembler} is created lazily on first use.
     */
    public TaxonomyResolver() {
    }

    /**
     * Creates a resolver that resolves family {@code after} classes
     * through the given class→node-ids mapping.
     *
     * @param classToNodeIds node class → registered node ids (registration order)
     */
    public TaxonomyResolver(Map<Class<?>, List<String>> classToNodeIds) {
        this.classToNodeIds = classToNodeIds == null ? null : new LinkedHashMap<>(classToNodeIds);
    }

    /**
     * @return the function registry (the curated function tree)
     */
    public FunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }

    /**
     * @return the framework registry
     */
    public FrameworkRegistry getFrameworkRegistry() {
        return frameworkRegistry;
    }

    /**
     * @return the family assembler; created on first use. The
     * class→node-ids mapping is taken from the supplied map (if any)
     * before the assembler is created, so {@code after} classes resolve
     * to real node ids wherever possible.
     */
    public FamilyAssembler getFamilyAssembler() {
        if (familyAssembler == null) {
            familyAssembler = new FamilyAssembler(classToNodeIds == null ? Map.of() : classToNodeIds);
        }
        return familyAssembler;
    }

    /**
     * Rebuilds the class→node-ids mapping used by the
     * {@link FamilyAssembler} to resolve {@code after} classes. A class
     * behind several registered node infos (e.g. a shared factory class
     * of programmatically generated node entries) maps to <em>all</em> of
     * their ids, in registration order.
     *
     * @param nodeClasses node class → the node infos that use the class
     */
    public void refreshClassToNodeIdMapping(Multimap<Class<? extends JIPipeGraphNode>, JIPipeNodeInfo> nodeClasses) {
        Map<Class<?>, List<String>> mapping = new LinkedHashMap<>();
        for (Map.Entry<Class<? extends JIPipeGraphNode>, JIPipeNodeInfo> entry : nodeClasses.entries()) {
            mapping.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(entry.getValue().getId());
        }
        this.classToNodeIds = mapping;
        // Keep the existing assembler (its steps survive); only its
        // class→node-ids mapping is updated — the steps resolve their
        // predecessors lazily against it at query time.
        if (familyAssembler != null) {
            familyAssembler.updateClassToNodeId(mapping);
        }
    }

    /**
     * Registers a node's taxonomy membership: the node's primary and
     * secondary functions and its framework are recorded for
     * {@link #validateTaxonomy()}; each
     * {@link JIPipeNodeTaxonomyFamilyStepAnnotation} on the node's class
     * is assembled into its family.
     * <p>
     * The functions and frameworks are NOT registered into the registries
     * here; they are curated and registered once by the taxonomy plugin.
     *
     * @param info the node info
     */
    public void recordNodeMembership(JIPipeNodeInfo info) {
        Set<JIPipeNodeTaxonomyFunction> functions = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            functions.add(primary);
        }
        functions.addAll(info.getSecondaryFunctions());
        boolean reRegistration = nodeFunctions.containsKey(info.getId());
        nodeFunctions.put(info.getId(), functions);
        JIPipeNodeTaxonomyFramework framework = info.getFramework();
        if (framework != null) {
            nodeFrameworks.put(info.getId(), framework);
        }

        // Steps were already assembled on the first registration of this
        // node; re-registering the same node must not add them twice
        // (addStep rejects duplicate node ids per family).
        if (reRegistration) {
            return;
        }
        for (JIPipeNodeTaxonomyFamilyStepAnnotation stepAnnotation :
            ReflectionUtils.getAllAnnotations(info.getInstanceClass(), JIPipeNodeTaxonomyFamilyStepAnnotation.class)) {
            JIPipeNodeTaxonomyFamily family =
                (JIPipeNodeTaxonomyFamily) ReflectionUtils.newInstance(stepAnnotation.family());
            String familyId = family.getId();
            familiesById.putIfAbsent(familyId, family);
            getFamilyAssembler().addStep(info.getId(), familyId, stepAnnotation);
        }
    }

    /**
     * Removes a node's taxonomy membership (inverse of
     * {@link #recordNodeMembership(JIPipeNodeInfo)}).
     *
     * @param nodeId the node id
     */
    public void removeNodeMembership(String nodeId) {
        nodeFunctions.remove(nodeId);
        nodeFrameworks.remove(nodeId);
    }

    /**
     * @param nodeId the node id
     * @return the recorded functions of the node (primary + secondary),
     * or an empty set if the node has none
     */
    public Set<JIPipeNodeTaxonomyFunction> getFunctionsOf(String nodeId) {
        return nodeFunctions.getOrDefault(nodeId, Set.of());
    }

    /**
     * @param nodeId the node id
     * @return the recorded framework of the node, or {@code null}
     */
    public JIPipeNodeTaxonomyFramework getFrameworkOf(String nodeId) {
        return nodeFrameworks.get(nodeId);
    }

    /**
     * @return an unmodifiable id → family view of the families first seen
     * through a node's step annotations, in discovery order
     */
    public Map<String, JIPipeNodeTaxonomyFamily> getFamilies() {
        return Collections.unmodifiableMap(familiesById);
    }

    /**
     * Removes a previously seen family from the family registry.
     * <p>
     * Families are only ever added (on first node registration) and have no
     * natural inverse of {@link #recordNodeMembership(JIPipeNodeInfo)}, so
     * this explicit removal is required to keep the shared registry clean
     * between tests that register synthetic nodes carrying a
     * {@code @JIPipeNodeTaxonomyFamilyStepAnnotation}. Does nothing if the
     * id is not present.
     *
     * @param familyId the family id to remove
     * @return the removed family, or {@code null} if none was registered under that id
     */
    public JIPipeNodeTaxonomyFamily unregisterFamily(String familyId) {
        return familiesById.remove(familyId);
    }

    /**
     * Registers an input profile provider. Providers are consulted in
     * registration order by {@link #inputProfileFor(Class)}.
     *
     * @param provider the provider
     */
    public void registerInputProfileProvider(InputProfileProvider provider) {
        inputProfileProviders.add(provider);
    }

    /**
     * Removes previously registered input profile providers.
     *
     * @param providers the providers to remove
     */
    public void unregisterInputProfileProviders(Iterable<InputProfileProvider> providers) {
        for (InputProfileProvider provider : providers) {
            inputProfileProviders.remove(provider);
        }
    }

    /**
     * Derives the input profile of a data class by consulting the
     * registered providers in order.
     * <p>
     * The derivation is total: a provider that throws or returns
     * {@code null} is skipped, and if no provider yields a non-
     * {@link JIPipeNodeTaxonomyInputProfile#NA} profile, {@code NA} is
     * returned. This method never throws.
     *
     * @param dataClass the data class
     * @return the first non-{@code NA} profile among the providers, or
     * {@code NA} if none yields one
     */
    public JIPipeNodeTaxonomyInputProfile inputProfileFor(Class<?> dataClass) {
        if (dataClass == null) {
            // No input slot ⇒ no derivable profile
            return JIPipeNodeTaxonomyInputProfile.NA;
        }
        for (InputProfileProvider provider : inputProfileProviders) {
            try {
                JIPipeNodeTaxonomyInputProfile profile = provider.getProfile(dataClass);
                if (profile != null && profile != JIPipeNodeTaxonomyInputProfile.NA) {
                    return profile;
                }
            } catch (RuntimeException ignored) {
                // A failing provider must not break derivation: skip it.
            }
        }
        return JIPipeNodeTaxonomyInputProfile.NA;
    }

    /**
     * Idempotently enriches the given node infos: the input profile is
     * derived from the first input slot's data class (no input slots ⇒
     * {@code NA}), and the merged tags (node-level tags plus the tags of
     * every function on the node's paths, leaf → root, primary and
     * secondary) are recomputed.
     * <p>
     * Only mutable infos ({@code JIPipeMutableNodeInfo}) are enriched;
     * others are left untouched.
     *
     * @param nodeInfos the node infos to enrich
     */
    public void enrichNodeInfos(Collection<? extends JIPipeNodeInfo> nodeInfos) {
        for (JIPipeNodeInfo info : nodeInfos) {
            if (!(info instanceof JIPipeMutableNodeInfo mutable)) {
                continue;
            }
            mutable.setInputProfile(inputProfileFor(firstInputSlotDataClass(info)));
            Set<JIPipeNodeTaxonomyFunction> functions = currentFunctionsOf(info);
            if (!functions.isEmpty()) {
                mutable.setMergedTags(mergedTags(info, functions));
            }
        }
    }

    /**
     * The node's current taxonomy functions: its primary function
     * (if any) plus its secondary functions.
     *
     * @param info the node info
     * @return the functions, possibly empty
     */
    private static Set<JIPipeNodeTaxonomyFunction> currentFunctionsOf(JIPipeNodeInfo info) {
        Set<JIPipeNodeTaxonomyFunction> functions = new LinkedHashSet<>();
        if (info.getPrimaryFunction() != null) {
            functions.add(info.getPrimaryFunction());
        }
        functions.addAll(info.getSecondaryFunctions());
        return functions;
    }

    /**
     * Validates the taxonomy state:
     * <ul>
     * <li>the function tree ({@link FunctionRegistry#validate()}),</li>
     * <li>the frameworks ({@link FrameworkRegistry#validate()}),</li>
     * <li>the family steps ({@link FamilyAssembler#validate(Set)} against
     * the registered node ids), and</li>
     * <li>the node→function/framework membership: every node with a
     * non-null primary function must have that function and its
     * framework present in the registries; an absent function is
     * reported naming the node id and the function id.</li>
     * </ul>
     * Nodes whose primary function is {@code null} are unconverted and
     * are not errors (they are reported separately, see Task 8).
     *
     * @param registeredNodeInfos nodeId → node info, the current node
     *                            registry
     * @return human-readable error strings; empty if the taxonomy is valid
     */
    public Set<String> validateTaxonomy(Map<String, ? extends JIPipeNodeInfo> registeredNodeInfos) {
        Set<String> errors = new LinkedHashSet<>();
        errors.addAll(functionRegistry.validate());
        errors.addAll(frameworkRegistry.validate());
        if (familyAssembler != null) {
            errors.addAll(familyAssembler.validate(registeredNodeInfos.keySet()));
        }
        for (Map.Entry<String, ? extends JIPipeNodeInfo> entry : registeredNodeInfos.entrySet()) {
            JIPipeNodeInfo info = entry.getValue();
            JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
            if (primary == null) {
                // Unconverted node: reported separately, not an error.
                continue;
            }
            if (functionRegistry.byId(primary.getId()) == null) {
                errors.add("Node '" + info.getId() + "' points at taxonomy function '"
                    + primary.getId() + "' that is not registered in the function registry");
            }
            JIPipeNodeTaxonomyFramework framework = info.getFramework();
            if (framework != null && !(framework instanceof NoFramework)
                && frameworkRegistry.byId(framework.getId()) == null) {
                errors.add("Node '" + info.getId() + "' points at taxonomy framework '"
                    + framework.getId() + "' that is not registered in the framework registry");
            }
        }
        return errors;
    }

    private static Class<?> firstInputSlotDataClass(JIPipeNodeInfo info) {
        List<AddJIPipeInputSlot> inputSlots = info.getInputSlots();
        if (inputSlots.isEmpty()) {
            return null;
        }
        return inputSlots.get(0).value();
    }

    private static Set<String> mergedTags(JIPipeNodeInfo info, Set<JIPipeNodeTaxonomyFunction> functions) {
        Set<String> tags = new LinkedHashSet<>(info.getTaxonomyTags());
        // Tag-audit resource (jipipe-core): per-node tags for the curated
        // nodes, merged next to the annotation-declared tags
        tags.addAll(TaxonomyAutoAssignment.CuratedNodeTags.tagsOf(info.getId()));
        for (JIPipeNodeTaxonomyFunction function : functions) {
            collectFunctionPathTags(function, tags, new HashSet<>());
        }
        return tags;
    }

    /**
     * Adds the tags of the function and every ancestor on its path
     * (leaf → root) to the given set.
     *
     * @param function the function to start from
     * @param target   the tag set to fill
     * @param visited  visited function ids (cycle guard)
     */
    private static void collectFunctionPathTags(JIPipeNodeTaxonomyFunction function, Set<String> target, Set<String> visited) {
        JIPipeNodeTaxonomyFunction current = function;
        while (current != null && visited.add(current.getId())) {
            target.addAll(current.getTags());
            current = current.getParent();
        }
    }
}
