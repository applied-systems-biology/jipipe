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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link FamilyAssembler}: graph building from
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}s, successor lookup,
 * dangling-step validation, multi-family annotation of a single node,
 * and the injectable {@code after} class→node-id mapping.
 * <p>
 * The stub node classes bridge to short node ids ("detect", "track")
 * through the assembler's injectable class→node-id mapping; the
 * fallback rule (class simple name) is pinned by
 * {@link #afterFallsBackToSimpleName()}.
 * <p>
 * Family ids embedded in step ids are always the explicit literals
 * {@link #TM_FAMILY_ID} and {@link #SM_FAMILY_ID}, mirroring the
 * stable {@code "jipipe:family:…"} id rule for real families.
 */
class FamilyAssemblerTest {

    /** Test literal for the TrackMate stub family's stable id. */
    private static final String TM_FAMILY_ID = "jipipe:family:trackmate";

    /** Test literal for the simple-model stub family's stable id. */
    private static final String SM_FAMILY_ID = "jipipe:family:simple-model";

    // ------------------------------------------------------------------
    // Stub node types
    // ------------------------------------------------------------------

    /** Stub node with node id "detect" (equals its simple name). */
    static class DetectSpots {
    }

    /** Stub node with node id "track" (equals its simple name). */
    static class TrackSpots {
    }

    // ------------------------------------------------------------------
    // Stub taxonomy families
    // ------------------------------------------------------------------

    private static class Tm implements JIPipeNodeTaxonomyFamily {
        @Override
        public String getId() {
            return "trackmate";
        }

        @Override
        public String getName() {
            return "TrackMate";
        }

        @Override
        public String getDescription() {
            return "TrackMate workflow";
        }
    }

    private static class Sm implements JIPipeNodeTaxonomyFamily {
        @Override
        public String getId() {
            return "simple-model";
        }

        @Override
        public String getName() {
            return "Simple model";
        }

        @Override
        public String getDescription() {
            return "Simple model workflow";
        }
    }

    /**
     * An assembler for the two-step TrackMate scenario: detect → track,
     * with the stub classes bridged to short node ids via the injected
     * class→node-id mapping.
     */
    private static FamilyAssembler twoStepAssembler() {
        FamilyAssembler assembler = new FamilyAssembler(Map.of(
            DetectSpots.class, "detect",
            TrackSpots.class, "track"));
        assembler.addStep("detect", TM_FAMILY_ID, step(Tm.class, "Detect spots"));
        assembler.addStep("track", TM_FAMILY_ID, step(Tm.class, "Track spots", DetectSpots.class));
        return assembler;
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    /**
     * graphBuiltFromAnnotations: 2 steps + 1 edge, direction
     * detect → track (predecessor → successor).
     */
    @Test
    void graphBuiltFromAnnotations() {
        FamilyAssembler assembler = twoStepAssembler();
        Tm tm = new Tm();

        var graph = assembler.graph(tm);
        assertEquals(2, graph.vertexSet().size(), "expected the two step ids as vertices");
        assertTrue(graph.containsVertex("jipipe:family:trackmate#detect"), "expected step id familyId#nodeId for detect");
        assertTrue(graph.containsVertex("jipipe:family:trackmate#track"), "expected step id familyId#nodeId for track");
        assertEquals(1, graph.edgeSet().size(), "expected exactly one predecessor→successor edge");

        // Edge direction: predecessor (detect) → successor (track).
        var edge = graph.edgeSet().iterator().next();
        assertNotNull(edge, "expected edge from detect to track");
        assertEquals("jipipe:family:trackmate#detect", graph.getEdgeSource(edge));
        assertEquals("jipipe:family:trackmate#track", graph.getEdgeTarget(edge));

        assertEquals(2, assembler.steps(tm).size());
        JIPipeNodeTaxonomyFamilyStep track = step(assembler.steps(tm), "track");
        assertEquals(List.of("jipipe:family:trackmate#detect"), track.afterIds());
    }

    /**
     * followUpsOrdered: {@code followUps("detect")} = {@code ["track"]}
     * (successor node ids, in registration order).
     */
    @Test
    void followUpsOrdered() {
        FamilyAssembler assembler = twoStepAssembler();

        assertEquals(List.of("track"), assembler.followUps("detect"));
        assertTrue(assembler.followUps("track").isEmpty(), "terminal step has no follow-ups");
    }

    /**
     * danglingStepRejected: a step whose node id is not registered
     * produces a validate() error naming that node id.
     */
    @Test
    void danglingStepRejected() {
        FamilyAssembler assembler = twoStepAssembler();

        // "track" is registered, "detect" is not → dangling step.
        Set<String> errors = assembler.validate(Set.of("track"));
        assertFalse(errors.isEmpty(), "expected at least one dangling-step error");
        assertTrue(errors.stream().anyMatch(e -> e.contains("detect")),
            "error must name the dangling node id 'detect', got: " + errors);

        // With all steps' node ids registered, validation passes.
        assertTrue(assembler.validate(Set.of("detect", "track")).isEmpty());
    }

    /**
     * multiFamily: one node annotated for two families appears in both
     * families' step sets and graphs.
     */
    @Test
    void multiFamily() {
        FamilyAssembler assembler = new FamilyAssembler();
        Tm tm = new Tm();
        Sm sm = new Sm();

        // The "filter" node carries steps in two different families
        // (one repeated annotation per family, as on a real node class).
        assembler.addStep("filter", TM_FAMILY_ID, step(Tm.class, "Filter (TrackMate)"));
        assembler.addStep("filter", SM_FAMILY_ID, step(Sm.class, "Filter (simple model)"));

        assertTrue(assembler.steps(tm).stream().anyMatch(s -> s.nodeId().equals("filter")),
            "filter must appear in the TrackMate family");
        assertTrue(assembler.steps(sm).stream().anyMatch(s -> s.nodeId().equals("filter")),
            "filter must appear in the simple-model family");
        assertTrue(assembler.graph(tm).containsVertex("jipipe:family:trackmate#filter"));
        assertTrue(assembler.graph(sm).containsVertex("jipipe:family:simple-model#filter"));
    }

    /**
     * The annotation must be {@code @Repeatable}: a node type annotated
     * with steps for two families compiles and exposes both annotations.
     */
    @Test
    void annotationIsRepeatable() {
        JIPipeNodeTaxonomyFamilyStepAnnotation[] annotations =
            RepeatableHost.class.getAnnotationsByType(JIPipeNodeTaxonomyFamilyStepAnnotation.class);
        assertEquals(2, annotations.length, "expected both family annotations on one type");
        assertTrue(Arrays.stream(annotations).anyMatch(a -> a.family() == Tm.class));
        assertTrue(Arrays.stream(annotations).anyMatch(a -> a.family() == Sm.class));

        // A node class without any family step annotations contributes nothing.
        FamilyAssembler assembler = new FamilyAssembler();
        for (JIPipeNodeTaxonomyFamilyStepAnnotation a :
            NoSteps.class.getAnnotationsByType(JIPipeNodeTaxonomyFamilyStepAnnotation.class)) {
            assembler.addStep("none", "jipipe:family:dummy", a);
        }
        assertEquals(0, assembler.steps(new Tm()).size());
        assertEquals(0, assembler.steps(new Sm()).size());
    }

    /**
     * {@code after} resolution uses the injected class→node-id map, which
     * overrides the simple-name fallback. Here the stub classes map to
     * realistic kebab-case node ids ("tm-detect", "tm-track") that differ
     * from their simple names.
     */
    @Test
    void afterResolvedByInjectedMapping() {
        FamilyAssembler assembler = new FamilyAssembler(Map.of(
            DetectSpots.class, "tm-detect",
            TrackSpots.class, "tm-track"));
        assembler.addStep("tm-detect", TM_FAMILY_ID, step(Tm.class, "Detect spots"));
        assembler.addStep("tm-track", TM_FAMILY_ID, step(Tm.class, "Track spots", DetectSpots.class));
        Tm tm = new Tm();

        assertEquals(List.of("tm-track"), assembler.followUps("tm-detect"));
        JIPipeNodeTaxonomyFamilyStep track = step(assembler.steps(tm), "tm-track");
        assertEquals(List.of("jipipe:family:trackmate#tm-detect"), track.afterIds());
        assertTrue(assembler.graph(tm).containsVertex("jipipe:family:trackmate#tm-detect"));
        assertTrue(assembler.graph(tm).containsVertex("jipipe:family:trackmate#tm-track"));
    }

    /**
     * The caller-supplied family id literal is used verbatim in step
     * ids, and a conflicting id for the same family class is rejected.
     */
    @Test
    void explicitFamilyIdInStepIds() {
        FamilyAssembler assembler = new FamilyAssembler(Map.of(
            DetectSpots.class, "detect",
            TrackSpots.class, "track"));
        assembler.addStep("detect", TM_FAMILY_ID, step(Tm.class, "Detect spots"));
        assembler.addStep("track", TM_FAMILY_ID, step(Tm.class, "Track spots", DetectSpots.class));
        Tm tm = new Tm();

        assertTrue(assembler.graph(tm).containsVertex("jipipe:family:trackmate#detect"));
        assertTrue(assembler.graph(tm).containsVertex("jipipe:family:trackmate#track"));
        assertEquals(List.of("jipipe:family:trackmate#detect"), step(assembler.steps(tm), "track").afterIds());
        assertEquals(List.of("track"), assembler.followUps("detect"));

        // A conflicting family id for the same family class is rejected.
        assertThrows(IllegalArgumentException.class,
            () -> assembler.addStep("detect", "jipipe:family:other", step(Tm.class, "Detect spots")));
    }

    /**
     * Without an injected mapping, an {@code after} class resolves to its
     * own simple name — the documented fallback rule.
     */
    @Test
    void afterFallsBackToSimpleName() {
        FamilyAssembler assembler = new FamilyAssembler();
        assembler.addStep("DetectSpots", TM_FAMILY_ID, step(Tm.class, "Detect spots"));
        assembler.addStep("TrackSpots", TM_FAMILY_ID, step(Tm.class, "Track spots", DetectSpots.class));

        JIPipeNodeTaxonomyFamilyStep track = step(assembler.steps(new Tm()), "TrackSpots");
        assertEquals(List.of("jipipe:family:trackmate#DetectSpots"), track.afterIds());
        assertEquals(List.of("TrackSpots"), assembler.followUps("DetectSpots"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JIPipeNodeTaxonomyFamilyStep step(Set<JIPipeNodeTaxonomyFamilyStep> steps, String nodeId) {
        return steps.stream().filter(s -> s.nodeId().equals(nodeId)).findFirst().orElseThrow();
    }

    /** Builds an annotation instance with the given family, label and successors. */
    private static JIPipeNodeTaxonomyFamilyStepAnnotation step(
        Class<? extends JIPipeNodeTaxonomyFamily> family, String label, Class<?>... after) {
        return new JIPipeNodeTaxonomyFamilyStepAnnotation() {
            @Override
            public Class<? extends JIPipeNodeTaxonomyFamily> family() {
                return family;
            }

            @Override
            public String label() {
                return label;
            }

            @Override
            public Class<?>[] after() {
                return after;
            }

            @Override
            public String purpose() {
                return "";
            }

            @Override
            public Class<? extends JIPipeNodeTaxonomyFamilyStepAnnotation> annotationType() {
                return JIPipeNodeTaxonomyFamilyStepAnnotation.class;
            }
        };
    }

    /** A node type annotated with steps for two families (@Repeatable). */
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = Tm.class, label = "Filter (TrackMate)", purpose = "Filter spots")
    @JIPipeNodeTaxonomyFamilyStepAnnotation(family = Sm.class, label = "Filter (simple model)", purpose = "Filter spots")
    private static class RepeatableHost {
    }

    /** A node type without any family step annotations. */
    private static class NoSteps {
    }
}
