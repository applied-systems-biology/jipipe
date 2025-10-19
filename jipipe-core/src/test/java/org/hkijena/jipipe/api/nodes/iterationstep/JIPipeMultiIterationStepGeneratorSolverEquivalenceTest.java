package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JIPipeMultiIterationStepGeneratorSolverEquivalenceTest {
    /**
     * A common case where we receive from two inputs data that should line up by their annotations
     * The iteration step generator should match the correct rows.
     * We are testing an inversion (can be any case in real data, but algorithm should not make assumptions)
     */
    @Test
    public void testTwoInputSimpleEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("#Dataset", "C", "B", "A"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 2)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 0)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * A common case where the non-presence of a reference column is seen as "ANY"/Wildcard
     */
    @Test
    public void testTwoInputsDistributingEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("NoReference", "A")) // No # -> not a reference column
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        // Expect the following layout
        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 0)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * A case where data is missing; the main algorithm should still work, although downstream algorithms may complain.
     */
    @Test
    public void testTwoInputsMissingEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("#Dataset", "A"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        // Expect the following layout
        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2")
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2")
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * The algorithm should fall back to creating a single step if no references can be found
     */
    @Test
    public void testDegenerate1Equivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("NoReference", "A", "B", "C")), // Not starting with #
                createDummyTable("Input 2", column("NoReferenceToo", "A"))  // Not starting with #
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true); // Merging will yield single step

        var expected = expectedResult(
                step(
                        slot("Input 1", 0, 1, 2),
                        slot("Input 2", 0)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * The algorithm should fall back to creating a single step if no references can be found, but merging is disabled,
     * so the resolver will create pairwise steps
     */
    @Test
    public void testDegenerate2Equivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("NoReference", "A", "B", "C")), // Not starting with #
                createDummyTable("Input 2", column("NoReferenceToo", "A", "B"))  // Not starting with #
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false); // NO merging!

        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 1)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    @Test
    public void testComplexFlowGraphScenarioEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1",
                        column("#Group", "A", "B"),
                        column("#ID", "1", "2")),
                createDummyTable("Input 2",
                        column("#Group", "B", "A"),
                        column("#ID", "2", "1"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        // Expect the following layout
        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * Test the flow graph solver's orphan connection logic
     * Create scenarios with nodes that have no compatible matches
     */
    @Test
    public void testOrphanHandlingEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "C")),
                createDummyTable("Input 2", column("#Group", "A", "B")),
                createDummyTable("Input 3", column("#Group", "A", "B"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        // Expect the following layout
        var expected  = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0),
                        slot("Input 3",0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1),
                        slot("Input 3", 1 )
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2"),
                        slot("Input 3")
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }

    /**
     * Test complex scenarios with mixed reference/non-reference columns
     * Across multiple inputs
     */
    @Test
    public void testMixedReferenceAndNonReferenceEquivalence() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1",
                        column("#Group", "A", "B"),
                        column("NonRef", "X", "Y")),
                createDummyTable("Input 2",
                        column("#Group", "A", "B"),
                        column("NonRef", "Z", "W"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        // Expect the following layout
        var expected = expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                )
        );

        // Test all solvers
        for (JIPipeIterationStepSolverPreference preference : JIPipeIterationStepSolverPreference.values()) {
            generator.setSolverPreference(preference);
            assertEquals(expected, build(generator));
        }
    }
}
