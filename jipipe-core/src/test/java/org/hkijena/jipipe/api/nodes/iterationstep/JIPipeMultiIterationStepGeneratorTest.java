package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JIPipeMultiIterationStepGeneratorTest {
    /**
     * A common case where we receive from two inputs data that should line up by their annotations
     * The iteration step generator should match the correct rows.
     * We are testing an inversion (can be any case in real data, but algorithm should not make assumptions)
     */
    @Test
    public void testTwoInputSimple() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("#Dataset", "C", "B", "A"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
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
        ), build(generator));
    }

    /**
     * A common case where the non-presence of a reference column is seen as "ANY"/Wildcard
     */
    @Test
    public void testTwoInputsDistributing() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("NoReference", "A")) // No # -> not a reference column
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
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
        ), build(generator));
    }

    /**
     * A case where data is missing; the main algorithm should still work, although downstream algorithms may complain.
     */
    @Test
    public void testTwoInputsMissing() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("#Dataset", "A"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
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
        ), build(generator));
    }

    /**
     * The algorithm should fall back to creating a single step if no references can be found
     */
    @Test
    public void testDegenerate1() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("NoReference", "A", "B", "C")), // Not starting with #
                createDummyTable("Input 2", column("NoReferenceToo", "A"))  // Not starting with #
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true); // Merging will yield single step
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 iteration step
        assertEquals(1, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0, 1, 2),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * The algorithm should fall back to creating a single step if no references can be found, but merging is disabled,
     * so the resolver will create pairwise steps
     */
    @Test
    public void testDegenerate2() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("NoReference", "A", "B", "C")), // Not starting with #
                createDummyTable("Input 2", column("NoReferenceToo", "A", "B"))  // Not starting with #
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false); // NO merging!
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 iteration step
        assertEquals(6, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
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
        ), build(generator));
    }

    /**
     * Test with forceFlowGraphSolver = true
     * Should use flow graph solver even when dictionary solver could be used
     */
    @Test
    public void testForceFlowGraphSolver() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A", "B", "C")),
                createDummyTable("Input 2", column("#Dataset", "A", "B", "C"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        generator.setSolverPreference(JIPipeIterationStepSolverPreference.ForceFlowGraph);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 2)
                )
        ), build(generator));
    }

    /**
     * Test scenario that definitely requires flow graph solver
     * Multiple reference columns, custom matching, etc.
     */
    @Test
    public void testComplexFlowGraphScenario() {
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
        generator.setSolverPreference(JIPipeIterationStepSolverPreference.ForceFlowGraph);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * Test the flow graph solver's orphan connection logic
     * Create scenarios with nodes that have no compatible matches
     */
    @Test
    public void testOrphanHandling() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "C")),
                createDummyTable("Input 2", column("#Group", "A", "B")),
                createDummyTable("Input 3", column("#Group", "A", "B"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        generator.setSolverPreference(JIPipeIterationStepSolverPreference.ForceFlowGraph);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0),
                        slot("Input 3", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1),
                        slot("Input 3", 1)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2"),
                        slot("Input 3")
                )
        ), build(generator));
    }

    /**
     * Test with forceNAIsAny = true
     * Empty reference values should match with all other values
     */
    @Test
    public void testForceNAIsAny() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "")),
                createDummyTable("Input 2", column("#Group", "A", "B", "B"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true); // forceNAIsAny makes really no sense without merging
        generator.setForceNAIsAny(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps
        assertEquals(2, result.size());

        // Expect the following layout (empty value matches with all)
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0, 2), // 2 because it matches also to A
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1, 2),
                        slot("Input 2", 1, 2)
                )
        ), build(generator));
    }

    /**
     * Test progress tracking and cancellation
     * Verify that cancellation is properly handled
     */
    @Test
    public void testProgressCancellation() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.cancel();

        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);

        List<JIPipeMultiIterationStep> result = generator.build(progressInfo);

        // Expect null result due to cancellation
        assertNull(result);
    }

    /**
     * Test with empty slot list
     * Should handle gracefully
     */
    @Test
    public void testEmptySlotList() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of());
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 0 iteration steps
        assertEquals(0, result.size());
    }

    /**
     * Test complex scenarios with mixed reference/non-reference columns
     * Across multiple inputs
     */
    @Test
    public void testMixedReferenceAndNonReference() {
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
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on #Group only
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                )
        ), build(generator));
    }

    /**
     * Test with inconsistent data types in reference columns
     * Should handle type mismatches gracefully
     */
    @Test
    public void testInconsistentDataTypes() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "123", "true")),
                createDummyTable("Input 2", column("#Group", "A", "123", "true"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 2)
                )
        ), build(generator));
    }

    /**
     * A basic test that applies a simple iteration case (1 input of N items = N iteration steps)
     * Uses the default selection method (use # to denote reference columns)
     */
    @Test
    public void testSimpleIteration() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0)),
                step(slot("Input", 1)),
                step(slot("Input", 2))
        ), build(generator));
    }

    /**
     * A basic test that applies a simple iteration case with non-reference columns
     * Because only # columns are used as reference, all other columns are ignored during creation of iteration steps
     */
    @Test
    public void testSimpleIterationWithNonReference() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input",
                column("#Dataset", "A", "B", "C"),
                column("Ignored", "1", "2", "3"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0)),
                step(slot("Input", 1)),
                step(slot("Input", 2))
        ), build(generator));
    }

    /**
     * A basic test that applies a simple merging case, but all reference column values are different
     * Uses the default selection method (use # to denote important columns)
     */
    @Test
    public void testSimpleMergingIteration1() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0)),
                step(slot("Input", 1)),
                step(slot("Input", 2))
        ), build(generator));
    }

    /**
     * A basic test that applies a simple merging case, where we should receive (A A) and (B)
     * Uses the default selection method (use # to denote important columns)
     */
    @Test
    public void testSimpleMergingIteration2() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "A", "B"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0, 1)),
                step(slot("Input", 2))
        ), build(generator));
    }

    /**
     * Test the all into one batch setting
     */
    @Test
    public void testAllIntoOne() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.MergeAll, new StringQueryExpression());
        generator.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 step
        assertEquals(1, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0, 1, 2))
        ), build(generator));
    }

    /**
     * Test Union strategy with multiple inputs
     * Should use all columns present in any input
     */
    @Test
    public void testUnionStrategy() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("Group", "A", "B"), column("ID", "1", "2")),
                createDummyTable("Input 2", column("Group", "B", "A"), column("ID", "2", "1"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.Union, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on #Group column (union of all columns)
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * Test Intersection strategy
     * Should only use columns present in all inputs
     */
    @Test
    public void testIntersectionStrategy() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B"), column("#ID", "1", "2")),
                createDummyTable("Input 2", column("#Group", "B", "A"), column("#Other", "X", "Y"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.Intersection, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 iteration step based on #Group column (intersection only has #Group)
        assertEquals(2, result.size());

        // Expect the following layout (only B matches)
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * Test PrefixHashIntersection strategy
     * Should use # columns present in all inputs
     */
    @Test
    public void testPrefixHashIntersection() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B"), column("#ID", "1", "2"), column("Other", "X", "Y")),
                createDummyTable("Input 2", column("#Group", "B", "A"), column("#Other", "X", "Y"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashIntersection, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 steps
        assertEquals(2, result.size());

        // Expect the following layout (only B matches)
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 1)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * Test the all into one batch setting but with merging disabled
     * (this will be ignored by the algorithm, which then cause expected issues with {@link JIPipeSingleIterationStep} creation.
     */
    @Test
    public void testAllIntoOne2() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.MergeAll, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 step
        assertEquals(1, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(slot("Input", 0, 1, 2))
        ), build(generator));
    }

}