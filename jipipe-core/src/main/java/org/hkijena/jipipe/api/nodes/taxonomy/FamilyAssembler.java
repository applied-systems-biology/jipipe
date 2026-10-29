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

import java.util.*;

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
 * assembler resolves a class to node ids via an injectable class→node-ids
 * mapping (see {@link #resolveNodeIds(Class)}), applied <em>lazily at
 * query time</em> — a step stores the raw {@code after} classes and
 * resolves them against the mapping in effect when
 * {@link #graph}, {@link #followUps(String)}, {@link #siblingNodeIds(String)}
 * or {@link #validate(Set)} runs:
 * <ol>
 * <li>the ids registered with the assembler win — production callers build
 * the mapping from the real node type registry, where node ids are stable
 * string literals that are generally <em>not</em> the class simple name. A
 * class mapped to several node ids (a shared factory class behind
 * programmatically generated entries) contributes all of them;</li>
 * <li>otherwise the class's simple name is used, which is convenient for
 * tests with stub node classes whose ids equal their simple names.</li>
 * </ol>
 * Deferred resolution makes registration order irrelevant: a step whose
 * predecessor registers later still resolves once the mapping is current.
 * <p>
 * The assembler is not thread-safe; populate it during startup, then
 * query it.
 */
public class FamilyAssembler {

    /**
     * node class → registered node ids, in registration order
     */
    private final Map<Class<?>, List<String>> classToNodeIds;

    /**
     * family class → (node id → step), insertion-ordered
     */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>,
            Map<String, JIPipeNodeTaxonomyFamilyStep>> stepsByFamily = new LinkedHashMap<>();

    /**
     * family class → node ids in registration order (deterministic follow-up order)
     */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>, List<String>> stepOrder = new LinkedHashMap<>();

    /**
     * family class → family id used in step ids, as first registered
     */
    private final Map<Class<? extends JIPipeNodeTaxonomyFamily>, String> familyIdByClass = new LinkedHashMap<>();

    /**
     * Creates an assembler that resolves {@code after} classes by their
     * simple name only (the fallback rule, see {@link #resolveNodeIds(Class)}).
     */
    public FamilyAssembler() {
        this(Map.of());
    }

    /**
     * Creates an assembler that resolves {@code after} classes through
     * the supplied class→node-ids mapping first, falling back to the
     * class's simple name.
     *
     * @param classToNodeIds the caller's mapping from node classes to their
     *                       registered node ids (in registration order); may be empty
     */
    public FamilyAssembler(Map<Class<?>, List<String>> classToNodeIds) {
        this.classToNodeIds = new LinkedHashMap<>();
        classToNodeIds.forEach((clazz, ids) -> this.classToNodeIds.put(clazz, new ArrayList<>(ids)));
    }

    /**
     * @param familyId the family id
     * @param nodeId   the node id
     * @return the step id {@code familyId + "#" + nodeId}
     */
    public static String stepId(String familyId, String nodeId) {
        return familyId + "#" + nodeId;
    }

    /**
     * Registers a step: the node with id {@code nodeId} takes the role
     * described by {@code annotation} inside {@code annotation.family()},
     * using {@code familyId} as the family id part of the step id. The
     * family id is a stable string literal supplied by the caller and is
     * never derived from the family class; real families use ids of the
     * form {@code "jipipe:family:…"}.
     * <p>
     * The annotation's {@code after} classes are stored <em>unresolved</em>
     * and resolved lazily at query time (see the class Javadoc); a
     * predecessor that is not registered in the same family (or not
     * resolvable at all) is reported at {@link #validate(Set)} time, not
     * here, so that all steps can be added before the graph is checked.
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
        // The raw after classes are stored, not resolved step ids:
        // resolution happens lazily at query time against the mapping in
        // effect then (see the class Javadoc), so registration order and
        // late mapping updates do not break the edges.
        List<Class<?>> afterClasses = new ArrayList<>(Arrays.asList(annotation.after()));
        familySteps.put(nodeId, new JIPipeNodeTaxonomyFamilyStep(
                familyId, annotation.label(), nodeId, annotation.purpose(), afterClasses));
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
     * Resolves an {@code after} node class to the node ids that fill its
     * role: the registered class→node-ids mapping wins (a class behind
     * programmatically generated entries maps to all of their ids, in
     * registration order), otherwise the class's simple name is used (see
     * the class Javadoc for the rationale).
     * <p>
     * Resolution happens lazily at query time: the mapping in effect when
     * this is called determines the result, so a predecessor registered
     * after the step was added still resolves correctly.
     *
     * @param nodeClass the predecessor node class from an annotation
     * @return the node ids to use as the predecessor step ids; never empty
     */
    public List<String> resolveNodeIds(Class<?> nodeClass) {
        List<String> mapped = classToNodeIds.get(nodeClass);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped;
        }
        return List.of(nodeClass.getSimpleName());
    }

    /**
     * Replaces the class→node-ids mapping used to resolve {@code after}
     * classes. Already-registered steps are not affected structurally:
     * their predecessor ids are resolved lazily against this mapping at
     * query time.
     *
     * @param classToNodeIds node class → registered node ids (in
     *                       registration order); may be empty
     */
    public void updateClassToNodeId(Map<Class<?>, List<String>> classToNodeIds) {
        this.classToNodeIds.clear();
        if (classToNodeIds != null) {
            classToNodeIds.forEach((clazz, ids) -> this.classToNodeIds.put(clazz, new ArrayList<>(ids)));
        }
    }

    /**
     * Builds the directed workflow graph of a family: one vertex per step
     * id, one edge predecessor → successor for each resolved {@code after}
     * id.
     *
     * @param family the family
     * @return a {@link SimpleDirectedGraph} whose vertices are the
     * family's step ids; empty if the family has no steps
     */
    public Graph<String, DefaultEdge> graph(JIPipeNodeTaxonomyFamily family) {
        Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        for (JIPipeNodeTaxonomyFamilyStep step : steps(family)) {
            graph.addVertex(step.getStepId());
            for (String afterId : step.afterIds(this)) {
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
                if (successor != null && successor.afterIds(this).contains(stepId)) {
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
     * The ids of the families that have a registered step for the given
     * node id, in family registration order.
     * <p>
     * This is the inverse of {@link #siblingNodeIds(String)}: instead of
     * returning the node ids inside one family, it returns the family ids
     * that contain a step for {@code nodeId}. It lets a UI caller resolve
     * "which workflow family(ies) does this node belong to?" and then look
     * each family up (e.g. in the node registry's family map) to inspect
     * its {@link JIPipeNodeTaxonomyFamily#getTemplate() template} and
     * {@link JIPipeNodeTaxonomyFamily#getExample() example}.
     *
     * @param nodeId the node id
     * @return the family ids of every family that has a step for
     * {@code nodeId}, in family registration order; empty if the
     * node holds no steps
     */
    public Set<String> familyIdsOf(String nodeId) {
        Set<String> familyIds = new LinkedHashSet<>();
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
                Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            if (familyEntry.getValue().containsKey(nodeId)) {
                String familyId = familyIdByClass.get(familyEntry.getKey());
                if (familyId != null) {
                    familyIds.add(familyId);
                }
            }
        }
        return familyIds;
    }

    /**
     * Checks that every registered step refers to a node id the caller
     * has in its set of registered node ids, and that every step's
     * resolved {@code after} predecessors refer to node ids of steps that
     * actually exist in the same family (a dangling predecessor means the
     * edge would silently never fire — e.g. because the predecessor's
     * class does not map to a registered step).
     *
     * @param registeredNodeIds the node ids that exist in the node type
     *                          registry
     * @return one human-readable error per dangling step or dangling
     * predecessor, naming the family and the node id; empty if all steps
     * are resolvable
     */
    public Set<String> validate(Set<String> registeredNodeIds) {
        Set<String> errors = new LinkedHashSet<>();
        for (Map.Entry<Class<? extends JIPipeNodeTaxonomyFamily>,
                Map<String, JIPipeNodeTaxonomyFamilyStep>> familyEntry : stepsByFamily.entrySet()) {
            String familyId = familyIdByClass.get(familyEntry.getKey());
            for (JIPipeNodeTaxonomyFamilyStep step : familyEntry.getValue().values()) {
                if (!registeredNodeIds.contains(step.nodeId())) {
                    errors.add("Workflow family step in family '" + familyId
                            + "' references unregistered node id '" + step.nodeId() + "'");
                    continue;
                }
                for (String afterId : step.afterIds(this)) {
                    String predecessorNodeId = afterId.substring(familyId.length() + 1);
                    if (!familyEntry.getValue().containsKey(predecessorNodeId)) {
                        errors.add("Workflow family step '" + step.nodeId() + "' in family '" + familyId
                                + "' references a missing predecessor step for node id '" + predecessorNodeId + "'");
                    }
                }
            }
        }
        return errors;
    }
}
