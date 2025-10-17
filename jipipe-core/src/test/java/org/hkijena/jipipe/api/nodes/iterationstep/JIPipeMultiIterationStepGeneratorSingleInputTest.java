package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.*;

import java.util.*;

class JIPipeMultiIterationStepGeneratorSingleInputTest {
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
                        slot("Input 1", 0,1,2),
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

}