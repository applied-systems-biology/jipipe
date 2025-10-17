package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.build;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.column;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.createDummyTable;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.expectedResult;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.slot;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JIPipeMultiIterationStepGeneratorAdditionalMultiInputTest {
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
        generator.setForceFlowGraphSolver(true);
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
        generator.setForceFlowGraphSolver(true);
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
        generator.setForceFlowGraphSolver(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
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
}
