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

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s into the
 * directed workflow graph of each {@link JIPipeNodeTaxonomyFamily}.
 * <p>
 * A step id is {@code familyId + "#" + nodeId}; the same node can hold
 * steps in several families, one per repeated annotation. Vertices of a
 * family's graph are its step ids; a directed edge points from a
 * predecessor step to this step, i.e. {@code after = {A, B}} on step S
 * produces the edges A → S and B → S (the "after" relationship).
 * <p>
 * <b>Family ids.</b> The {@code familyId} part of a step id is always a
 * stable string literal supplied by the caller via
 * {@link #addStep(String, String, JIPipeNodeTaxonomyFamilyStepAnnotation)};
 * it is never derived from the family class. Per the project's stable-ID
 * rule, real families use ids of the form {@code "jipipe:family:…"}.
 * Steps are keyed by the family class object, so two classes with the
 * same simple name in different packages do not collide; all
 * registrations for one family class must use the same family id.
 * <p>
 * <b>Resolution of {@code after} node classes.</b> Annotation values are
 * node <em>classes</em>, but steps are keyed by node <em>ids</em>. The
 * assembler resolves a class to a node id via an injectable class→node-id
 * mapping (see {@link #resolveNodeId(Class)}):
 * <ol>
 * <li>the mapping registered with the assembler wins — production
 * callers build it from the real node type registry, where node ids are
 * stable string literals that are generally <em>not</em> the class
 * simple name;</li>
 * <li>otherwise the class's simple name is used, which is convenient for
 * tests with stub node classes whose ids equal their simple names.</li>
 * </ol>
 * <p>
 * The assembler is not thread-safe; populate it during startup, then
 * query it.
 */
public class FamilyAssembler {

    /** node class → node id mapping registered by the caller */
    private final Map<Class<?>, String> classToNodeId;

    /** family class → (node id → step), insertion-ordered */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>,
        Map<String, JIPipeNodeTaxonomyFamilyStep>> stepsByFamily = new LinkedHashMap<>();

    /** family class → node ids in registration order (deterministic follow-up order) */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>, List<String>> stepOrder = new LinkedHashMap<>();

    /** family class → family id used in step ids, as first registered */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>, String> familyIdByClass = new LinkedHashMap<>();

    /**
     * Creates an assembler that resolves {@code after} classes by their
     * simple name only (the fallback rule, see {@link #resolveNodeId(Class)}).
     */
    public FamilyAssembler() {
        this(Map.of());
    }

    /**
     * Creates an assembler that resolves {@code after} classes through
     * the supplied class→node-id mapping first, falling back to the
     * class's simple name.
     *
     * @param classToNodeId the caller's mapping from node classes to their
     *                      registered node ids; may be empty
     */
    public FamilyAssembler(Map<Class<?>, String> classToNodeId) {
        this.classToNodeId = new LinkedHashMap<>(classToNodeId);
    }

    /**
     * Registers a step: the node with id {@code nodeId} takes the role
     * described by {@code annotation} inside {@code annotation.family()},
     * using {@code familyId} as the family id part of the step id. The
     * family id is a stable string literal supplied by the caller and is
     * never derived from the family class; real families use ids of the
     * form {@code "jipipe:family:…"}.
     * <p>
     * The annotation's {@code after} classes are resolved via
     * {@link #resolveNodeId(Class)}; a predecessor that is not registered
     * in the same family (or not resolvable at all) is reported at
     * {@link #validate(Set)} time, not here, so that all steps can be
     * added before the graph is checked.
     * <p>
     * All registrations for one family class must agree on the family id;
     * the first one wins and a conflicting later one is rejected.
     *
     * @param nodeId     the node id the step refers to
     * @param familyId   the stable id literal to use as the family id part
     *                   of the step ids (e.g. {@code "jipipe:family:trackmate"});
     *                   never derived from the family class
     * @param annotation the step annotation; its {@code family()} selects
     *                   the family
     * @throws IllegalArgumentException if the same node already holds a
     *                                  step for the same family, or if
     *                                  the family class was already
     *                                  registered with a different id
     */
    public void addStep(String nodeId, String familyId,
                        JIPipeNodeTaxonomyFamilyStepAnnotation annotation) {
        Class<? extends JIPipeNodeTaxonomyFamily> familyClass = annotation.family();
        String previous = familyIdByClass.putIfAbsent(familyClass, familyId);
        if (previous != null && !previous.equals(familyId)) {
            throw new IllegalArgumentException(
                "Conflicting family id for " + familyClass.getName() + ": '" + previous + "' and '" + familyId + "'");
        }
        Map<String, JIPipeNodeTaxonomyFamilyStep> familySteps =
            stepsByFamily.computeIfAbsent(familyClass, id -> new LinkedHashMap<>());
        if (familySteps.containsKey(nodeId)) {
            throw new IllegalArgumentException(
                "Duplicate step for node id '" + nodeId + "' in family '" + familyId + "'");
        }
        List<String> afterIds = new ArrayList<>();
        for (Class<?> predecessor : annotation.after()) {
            String predecessorNodeId = resolveNodeId(predecessor);
            afterIds.add(stepId(familyId, predecessorNodeId));
        }
        familySteps.put(nodeId, new JIPipeNodeTaxonomyFamilyStep(
            familyId, annotation.label(), nodeId, annotation.purpose(), afterIds));
        stepOrder.computeIfAbsent(familyClass, id -> new ArrayList<>()).add(nodeId);
    }

    /**
     * Removes a previously registered step. Does nothing if no step with
     * the given node id exists in the given family (identified by the
     * family id literal supplied to {@link #addStep(String, String, JIPipeNodeTaxonomyFamilyStepAnnotation)}).
     *
     * @param nodeId   the node id of the step to remove
     * @param familyId the family id literal of the family the step belongs to
     */
    public void unregisterStep(String nodeId, String familyId) {
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
            Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            String registeredFamilyId = familyIdByClass.get(familyEntry.getKey());
            if (!familyId.equals(registeredFamilyId)) {
                continue;
            }
            familyEntry.getValue().remove(nodeId);
            List<String> order = stepOrder.get(familyEntry.getKey());
            if (order != null) {
                order.remove(nodeId);
            }
            if (familyEntry.getValue().isEmpty()) {
                stepsByFamily.remove(familyEntry.getKey());
                stepOrder.remove(familyEntry.getKey());
            }
            return;
        }
    }

    /**
     * Resolves an {@code after} node class to a node id: the registered
     * class→node-id mapping wins, otherwise the class's simple name is
     * used (see the class Javadoc for the rationale).
     *
     * @param nodeClass the predecessor node class from an annotation
     * @return the node id to use as the predecessor step id
     */
    public String resolveNodeId(Class<?> nodeClass) {
        String mapped = classToNodeId.get(nodeClass);
        if (mapped != null) {
            return mapped;
        }
        return nodeClass.getSimpleName();
    }

    /**
     * Replaces the class→node-id mapping used to resolve {@code after}
     * classes in future {@link #addStep} calls. Already-registered steps
     * are not affected: their predecessor step ids were resolved when
     * they were added.
     *
     * @param classToNodeId node class → registered node id; may be empty
     */
    public void updateClassToNodeId(Map<Class<?>, String> classToNodeId) {
        this.classToNodeId.clear();
        if (classToNodeId != null) {
            this.classToNodeId.putAll(classToNodeId);
        }
    }

    /**
     * Builds the directed workflow graph of a family: one vertex per step
     * id, one edge predecessor → successor for each {@code after} entry.
     *
     * @param family the family
     * @return a {@link SimpleDirectedGraph} whose vertices are the
     * family's step ids; empty if the family has no steps
     */
    public Graph<String, DefaultEdge> graph(JIPipeNodeTaxonomyFamily family) {
        Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        for (JIPipeNodeTaxonomyFamilyStep step : steps(family)) {
            graph.addVertex(step.getStepId());
            for (String afterId : step.afterIds()) {
                graph.addVertex(afterId);
                graph.addEdge(afterId, step.getStepId());
            }
        }
        return graph;
    }

    /**
     * @param family the family
     * @return all steps of the family, in registration order
     */
    public Set<JIPipeNodeTaxonomyFamilyStep> steps(JIPipeNodeTaxonomyFamily family) {
        Map<String, JIPipeNodeTaxonomyFamilyStep> familySteps =
            stepsByFamily.getOrDefault(family.getClass(), Map.of());
        Set<JIPipeNodeTaxonomyFamilyStep> result = new LinkedHashSet<>();
        for (String nodeId : stepOrder.getOrDefault(family.getClass(), List.of())) {
            JIPipeNodeTaxonomyFamilyStep step = familySteps.get(nodeId);
            if (step != null) {
                result.add(step);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * @param nodeId the node id
     * @return the node ids of the direct successors of {@code nodeId}
     * (the targets of its outgoing workflow edges), in step registration
     * order, deduplicated across the families the node belongs to; empty
     * if the node has no steps or is terminal in every family it
     * belongs to
     */
    public List<String> followUps(String nodeId) {
        Set<String> seen = new LinkedHashSet<>();
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
            Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            String stepId = stepId(familyIdByClass.get(familyEntry.getKey()), nodeId);
            for (String successorNodeId :
                stepOrder.getOrDefault(familyEntry.getKey(), List.of())) {
                JIPipeNodeTaxonomyFamilyStep successor = familyEntry.getValue().get(successorNodeId);
                if (successor != null && successor.afterIds().contains(stepId)) {
                    seen.add(successorNodeId);
                }
            }
        }
        return new ArrayList<>(seen);
    }

    /**
     * @param nodeId the node id
     * @return the node ids of the <em>other</em> steps of every family
     * that has a registered step for {@code nodeId} (its workflow
     * siblings), in family registration order, then step registration
     * order; {@code nodeId} itself is excluded and the result is empty
     * if the node holds no steps. The ids of a node's follow-ups are a
     * subset of the returned ids (a follow-up is a sibling); callers
     * that must exclude follow-ups do so with
     * {@link #followUps(String)}.
     */
    public Set<String> siblingNodeIds(String nodeId) {
        Set<String> siblings = new LinkedHashSet<>();
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
            Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            if (!familyEntry.getValue().containsKey(nodeId)) {
                continue;
            }
            for (String otherNodeId : stepOrder.getOrDefault(familyEntry.getKey(), List.of())) {
                if (!otherNodeId.equals(nodeId) && familyEntry.getValue().containsKey(otherNodeId)) {
                    siblings.add(otherNodeId);
                }
            }
        }
        return siblings;
    }

    /**
     * Checks that every registered step refers to a node id the caller
     * has in its set of registered node ids.
     *
     * @param registeredNodeIds the node ids that exist in the node type
     *                          registry
     * @return one human-readable error per dangling step (a step whose
     * node id is not in {@code registeredNodeIds}), naming the family
     * and the node id; empty if all steps are resolvable
     */
    public Set<String> validate(Set<String> registeredNodeIds) {
        Set<String> errors = new LinkedHashSet<>();
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
            Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            for (JIPipeNodeTaxonomyFamilyStep step : familyEntry.getValue().values()) {
                if (!registeredNodeIds.contains(step.nodeId())) {
                    errors.add("Workflow family step in family '" + familyIdByClass.get(familyEntry.getKey())
                        + "' references unregistered node id '" + step.nodeId() + "'");
                }
            }
        }
        return errors;
    }

    /**
     * @param familyId the family id
     * @param nodeId   the node id
     * @return the step id {@code familyId + "#" + nodeId}
     */
    private static String stepId(String familyId, String nodeId) {
        return familyId + "#" + nodeId;
    }
}
