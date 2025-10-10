package org.hkijena.jipipe.api.nodes.iterationstep;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.*;

import java.util.*;

class JIPipeMultiIterationStepGeneratorTest {
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


}