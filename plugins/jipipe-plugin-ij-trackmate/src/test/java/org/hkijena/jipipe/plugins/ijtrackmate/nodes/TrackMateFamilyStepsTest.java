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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes;

import org.hkijena.jipipe.api.nodes.taxonomy.FamilyAssembler;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation;
import org.hkijena.jipipe.api.nodes.taxonomy.families.TrackMateFamily;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters.ConvertSpotsToRoiNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters.ConvertTracksToRoiNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.detectors.CreateSpotDetectorNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.MeasureSpotsNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.SpotDetectorNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.SpotFilterNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.SpotsToRGBNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the {@link TrackMateFamily} workflow graph assembled from the
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s on the TrackMate node
 * classes.
 * <p>
 * This test lives in the trackmate plugin module because the annotated node
 * classes are plugin classes; jipipe-core (which hosts the family classes)
 * must not depend on its plugins. The class-level metadata of the four seed
 * families is tested in core's {@code SeedFamiliesTest}.
 * <p>
 * Steps are keyed by the node class's <em>simple name</em> - the
 * {@link FamilyAssembler} fallback rule for an empty class→node-id mapping.
 * In production the registry provides the real stable node ids, and the
 * {@code after} classes resolve lazily against that mapping at query time.
 * The generated detector entries (DoG, LoG, …) share the
 * {@link CreateSpotDetectorNode} class, which carries the "Detect spots"
 * step: in production its class→node-ids mapping fans the "Track spots"
 * edge out to every generated detector id.
 * <p>
 * The annotated workflow: the main chain
 * Detect spots → Track spots → Filter tracks → Measure tracks, with
 * branches for spot-level analysis (Filter/Measure spots, Convert spots to
 * RGB/ROI), track-level analysis (Measure branches/edges, Visualize tracked
 * spots, Convert tracks to RGB/ROI), and visualization (Render track scheme).
 */
class TrackMateFamilyStepsTest {

    /**
     * The TrackMate node classes annotated as family steps.
     */
    private static final List<Class<?>> ANNOTATED_NODES = List.of(
            SpotDetectorNode.class,
            TrackingNode.class,
            TrackFilterNode.class,
            MeasureTracksNode.class,
            SpotFilterNode.class,
            MeasureSpotsNode.class,
            SpotsToRGBNode.class,
            ConvertSpotsToRoiNode.class,
            MeasureBranchesNode.class,
            MeasureEdgesNode.class,
            FollowSpotsPerTrackNode.class,
            TracksToRGBNode.class,
            ConvertTracksToRoiNode.class,
            TrackSchemeRendererNode.class,
            // The shared factory class of the generated detector entries (DoG,
            // LoG, …) - each generated node id becomes a "Detect spots" step,
            // and "Track spots" follows all of them (deferred, multi-id
            // resolution in production; the simple-name fallback here).
            CreateSpotDetectorNode.class
    );

    /**
     * Builds an assembler with all annotated TrackMate steps registered.
     */
    private static FamilyAssembler assembleSteps() {
        FamilyAssembler assembler = new FamilyAssembler();
        for (Class<?> nodeClass : ANNOTATED_NODES) {
            for (JIPipeNodeTaxonomyFamilyStepAnnotation annotation :
                    nodeClass.getAnnotationsByType(JIPipeNodeTaxonomyFamilyStepAnnotation.class)) {
                if (annotation.family() != TrackMateFamily.class) {
                    throw new AssertionError(nodeClass.getSimpleName()
                            + " has a TrackMate step annotation for family " + annotation.family().getName());
                }
                assembler.addStep(nodeClass.getSimpleName(),
                        TrackMateFamily.ID, annotation);
            }
        }
        return assembler;
    }

    /**
     * Kahn's topological sort over the edge set: returns {@code true} if the
     * directed graph has no cycle (every vertex is processed). Builds its own
     * adjacency from the edge set so it depends on no jgrapht algorithm API
     * (which lives in {@code jgrapht-alg}, a dependency this module does not
     * have).
     */
    private static boolean isAcyclic(Graph<String, DefaultEdge> graph) {
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String vertex : graph.vertexSet()) {
            adjacency.put(vertex, new ArrayList<>());
            inDegree.put(vertex, 0);
        }
        for (DefaultEdge edge : graph.edgeSet()) {
            String source = graph.getEdgeSource(edge);
            String target = graph.getEdgeTarget(edge);
            adjacency.get(source).add(target);
            inDegree.merge(target, 1, Integer::sum);
        }
        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            String vertex = queue.poll();
            processed++;
            for (String successor : adjacency.get(vertex)) {
                int remaining = inDegree.merge(successor, -1, Integer::sum);
                if (remaining == 0) {
                    queue.add(successor);
                }
            }
        }
        return processed == graph.vertexSet().size();
    }

    /**
     * Every annotated node class carries exactly one TrackMate family step
     * annotation, and every {@code after} class is one of the annotated
     * nodes (no dangling predecessor edges).
     */
    @Test
    void everyAnnotatedNodeHasExactlyOneTrackMateStep() {
        Set<Class<?>> annotated = ANNOTATED_NODES.stream().collect(Collectors.toSet());
        for (Class<?> nodeClass : ANNOTATED_NODES) {
            List<JIPipeNodeTaxonomyFamilyStepAnnotation> annotations =
                    List.of(nodeClass.getAnnotationsByType(JIPipeNodeTaxonomyFamilyStepAnnotation.class));
            assertEquals(1, annotations.size(),
                    "expected exactly one TrackMate step annotation on " + nodeClass.getSimpleName()
                            + ", got " + annotations.size());
            JIPipeNodeTaxonomyFamilyStepAnnotation annotation = annotations.get(0);
            assertEquals(TrackMateFamily.class, annotation.family(),
                    "wrong family on " + nodeClass.getSimpleName());
            assertFalse(annotation.label().isBlank(), "blank label on " + nodeClass.getSimpleName());
            assertFalse(annotation.purpose().isBlank(), "blank purpose on " + nodeClass.getSimpleName());
            for (Class<?> after : annotation.after()) {
                assertTrue(annotated.contains(after),
                        nodeClass.getSimpleName() + "'s after class " + after.getSimpleName()
                                + " is not an annotated TrackMate step node");
            }
        }
    }

    /**
     * The assembled family graph is a directed acyclic graph with every
     * annotated step present as a vertex.
     */
    @Test
    void familyGraphIsAcyclicAndComplete() {
        FamilyAssembler assembler = assembleSteps();
        TrackMateFamily family = new TrackMateFamily();

        Graph<String, DefaultEdge> graph = assembler.graph(family);
        assertEquals(ANNOTATED_NODES.size(), graph.vertexSet().size(),
                "expected one vertex per annotated node, got " + graph.vertexSet());
        for (Class<?> nodeClass : ANNOTATED_NODES) {
            assertTrue(graph.containsVertex(TrackMateFamily.ID + "#" + nodeClass.getSimpleName()),
                    "missing step vertex for " + nodeClass.getSimpleName());
        }
        assertTrue(isAcyclic(graph), "family workflow graph must be acyclic");
        // Sanity: the graph must contain at least the main chain's edges.
        assertTrue(graph.edgeSet().size() >= 7, "expected at least 7 edges, got " + graph.edgeSet().size());

        Set<String> errors = assembler.validate(ANNOTATED_NODES.stream()
                .map(Class::getSimpleName).collect(Collectors.toSet()));
        assertTrue(errors.isEmpty(), "dangling steps: " + errors);
    }

    /**
     * The main chain resolves in order:
     * Detect spots → Track spots → Filter tracks → Measure tracks.
     */
    @Test
    void mainChainFollowUps() {
        FamilyAssembler assembler = assembleSteps();

        List<String> afterDetect = assembler.followUps(SpotDetectorNode.class.getSimpleName());
        assertTrue(afterDetect.contains(TrackingNode.class.getSimpleName()),
                "tracking must be a follow-up of detect spots, got: " + afterDetect);
        assertTrue(afterDetect.contains(SpotFilterNode.class.getSimpleName()),
                "filter spots must be a follow-up of detect spots, got: " + afterDetect);

        List<String> afterTrack = assembler.followUps(TrackingNode.class.getSimpleName());
        assertTrue(afterTrack.contains(TrackFilterNode.class.getSimpleName()),
                "filter tracks must be a follow-up of track spots, got: " + afterTrack);
        assertTrue(afterTrack.contains(MeasureBranchesNode.class.getSimpleName()),
                "branch analysis must be a follow-up of track spots, got: " + afterTrack);
        assertTrue(afterTrack.contains(MeasureEdgesNode.class.getSimpleName()),
                "measure edges must be a follow-up of track spots, got: " + afterTrack);

        List<String> afterFilter = assembler.followUps(TrackFilterNode.class.getSimpleName());
        assertTrue(afterFilter.contains(MeasureTracksNode.class.getSimpleName()),
                "measure tracks must be a follow-up of filter tracks, got: " + afterFilter);

        // The chain order is preserved: spot filtering comes before tracking,
        // tracking before track filtering, track filtering before measuring.
        List<String> order = assembler.steps(new TrackMateFamily()).stream()
                .map(s -> s.nodeId()).collect(Collectors.toList());
        assertEquals(0, order.indexOf(SpotDetectorNode.class.getSimpleName()),
                "detect spots must be the first registered step");
        int detect = order.indexOf(SpotDetectorNode.class.getSimpleName());
        int track = order.indexOf(TrackingNode.class.getSimpleName());
        int filterTracks = order.indexOf(TrackFilterNode.class.getSimpleName());
        int measureTracks = order.indexOf(MeasureTracksNode.class.getSimpleName());
        assertTrue(detect < track && track < filterTracks && filterTracks < measureTracks,
                "main chain must be registered in workflow order, got: " + order);
    }

    /**
     * The branch steps hang off the right predecessors.
     */
    @Test
    void branchFollowUps() {
        FamilyAssembler assembler = assembleSteps();

        assertTrue(assembler.followUps(MeasureSpotsNode.class.getSimpleName())
                        .contains(ConvertSpotsToRoiNode.class.getSimpleName()),
                "convert spots to ROI must follow measure spots");
        assertTrue(assembler.followUps(MeasureTracksNode.class.getSimpleName())
                        .contains(ConvertTracksToRoiNode.class.getSimpleName()),
                "convert tracks to ROI must follow measure tracks");
        assertTrue(assembler.followUps(TrackingNode.class.getSimpleName())
                        .contains(FollowSpotsPerTrackNode.class.getSimpleName()),
                "visualize tracked spots must follow track spots");
        assertTrue(assembler.followUps(MeasureTracksNode.class.getSimpleName())
                        .contains(TracksToRGBNode.class.getSimpleName()),
                "convert tracks to RGB must follow measure tracks");
        assertTrue(assembler.followUps(MeasureTracksNode.class.getSimpleName())
                        .contains(TrackSchemeRendererNode.class.getSimpleName()),
                "render track scheme must follow measure tracks");

        // Terminal steps have no successors.
        assertTrue(assembler.followUps(TrackSchemeRendererNode.class.getSimpleName()).isEmpty(),
                "render track scheme is terminal");

        // The generated detector entries (shared CreateSpotDetectorNode
        // class, simple-name fallback here) link into "Track spots" - this
        // is the "DoG detector → Track spots" follow-up the finder must
        // show for every generated detector node id in production.
        assertTrue(assembler.followUps(CreateSpotDetectorNode.class.getSimpleName())
                        .contains(TrackingNode.class.getSimpleName()),
                "tracking must be a follow-up of the created spot detectors");
    }

    /**
     * The family class itself is the seed family: final, with a stable
     * string-literal id.
     */
    @Test
    void familyClassIsFinalWithStableId() {
        assertTrue(Modifier.isFinal(TrackMateFamily.class.getModifiers()),
                "TrackMateFamily must be final");
        assertEquals("jipipe:family:trackmate", new TrackMateFamily().getId());
        assertNotNull(new TrackMateFamily().getName());
        assertNotNull(new TrackMateFamily().getDescription());
    }
}
