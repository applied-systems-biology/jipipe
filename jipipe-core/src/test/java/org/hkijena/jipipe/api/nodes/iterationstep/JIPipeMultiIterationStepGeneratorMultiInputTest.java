package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeTextAnnotationMatchingMethod;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JIPipeMultiIterationStepGeneratorMultiInputTest {
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
     * Test custom expression-based matching instead of exact match
     * Use JIPipeTextAnnotationMatchingMethod.CustomExpression
     * Test with complex expressions
     */
    @Test
    public void testCustomExpressionMatching() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Dataset", "A1", "B2", "C3")),
                createDummyTable("Input 2", column("#Dataset", "A", "B", "C"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setAnnotationMatchingMethod(JIPipeTextAnnotationMatchingMethod.CustomExpression);
        generator.setCustomAnnotationMatching(new JIPipeExpressionParameter("value.substring(0, 1)"));
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 3 iteration steps
        assertEquals(3, result.size());

        // Expect the following layout (matching based on first character)
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
     * Test JIPipeIterationStepTextAnnotationColumMatching.Custom
     * Test with custom column expressions
     */
    @Test
    public void testCustomColumnSelection() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "C"), column("#ID", "1", "2", "3")),
                createDummyTable("Input 2", column("#Group", "A", "B"), column("#ID", "1", "2"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.Custom, new StringQueryExpression("#Group"));
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps
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
     * Test Union strategy with multiple inputs
     * Should use all columns present in any input
     */
    @Test
    public void testUnionStrategy() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B"), column("#ID", "1", "2")),
                createDummyTable("Input 2", column("#Group", "B", "C"), column("#Other", "X", "Y"))
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
                        slot("Input 2", 1)  // C doesn't exist in Input 1, B doesn't exist in Input 2 for row 1
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
                createDummyTable("Input 2", column("#Group", "B", "C"), column("#Other", "X", "Y"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.Intersection, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 iteration step based on #Group column (intersection only has #Group)
        assertEquals(1, result.size());

        // Expect the following layout (only B matches)
        assertEquals(expectedResult(
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
                createDummyTable("Input 2", column("#Group", "B", "C"), column("#Other", "X", "Y"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashIntersection, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 1 iteration step based on #Group column (only #Group is common # column)
        assertEquals(1, result.size());

        // Expect the following layout (only B matches)
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 0)
                )
        ), build(generator));
    }

    /**
     * Test with three inputs, each with different reference columns
     * Some columns should match across all inputs, some only partially
     */
    @Test
    public void testThreeInputComplexMatching() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "C"), column("#ID", "1", "2", "3")),
                createDummyTable("Input 2", column("#Group", "A", "B"), column("#Other", "X", "Y")),
                createDummyTable("Input 3", column("#Group", "B", "C"), column("#ID", "2", "3"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on #Group column
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1),
                        slot("Input 3", 0)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2"),
                        slot("Input 3", 1)
                )
        ), build(generator));
    }

    /**
     * Test slots with multiple reference columns
     * Test different combinations of matching columns
     */
    @Test
    public void testMultipleReferenceColumns() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B"), column("#ID", "1", "2")),
                createDummyTable("Input 2", column("#Group", "A", "B"), column("#ID", "1", "3"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on combination of #Group and #ID
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
     * Test scenarios where only some inputs match on reference columns
     * Should handle partial matches gracefully
     */
    @Test
    public void testPartialMatching() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B", "C")),
                createDummyTable("Input 2", column("#Group", "A", "B")),
                createDummyTable("Input 3", column("#Group", "B", "D"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on #Group column
        assertEquals(2, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0),
                        slot("Input 3")
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 1),
                        slot("Input 3", 0)
                )
        ), build(generator));
    }

    /**
     * Test different JIPipeTextAnnotationMergeMode values
     * Overwrite, Merge, KeepFirst, KeepLast
     */
    @Test
    public void testDifferentAnnotationMergeStrategies() {
        // Test OverwriteExisting
        JIPipeMultiIterationStepGenerator generator1 = new JIPipeMultiIterationStepGenerator();
        generator1.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator1.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator1.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.OverwriteExisting);
        generator1.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result1 = generator1.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result1.size());

        // Test Merge
        JIPipeMultiIterationStepGenerator generator2 = new JIPipeMultiIterationStepGenerator();
        generator2.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator2.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator2.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Merge);
        generator2.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result2 = generator2.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result2.size());

        // Test KeepFirst
        JIPipeMultiIterationStepGenerator generator3 = new JIPipeMultiIterationStepGenerator();
        generator3.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator3.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator3.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.SkipExisting);
        generator3.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result3 = generator3.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result3.size());

        // Test KeepLast (Discard)
        JIPipeMultiIterationStepGenerator generator4 = new JIPipeMultiIterationStepGenerator();
        generator4.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator4.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator4.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Discard);
        generator4.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result4 = generator4.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result4.size());
    }

    /**
     * Test different JIPipeDataAnnotationMergeMode strategies
     * Test with actual data annotations, not just text
     */
    @Test
    public void testDataAnnotationMerging() {
        // Test OverwriteExisting
        JIPipeMultiIterationStepGenerator generator1 = new JIPipeMultiIterationStepGenerator();
        generator1.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator1.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator1.setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode.OverwriteExisting);
        generator1.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result1 = generator1.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result1.size());

        // Test MergeTables
        JIPipeMultiIterationStepGenerator generator2 = new JIPipeMultiIterationStepGenerator();
        generator2.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B"))
        ));
        generator2.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator2.setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode.MergeTables);
        generator2.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result2 = generator2.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result2.size());
    }

    /**
     * Test slots with no annotations
     * Test mixed scenarios with some annotated, some not
     */
    @Test
    public void testEmptyAnnotations() {
        // Test with no reference columns
        JIPipeMultiIterationStepGenerator generator1 = new JIPipeMultiIterationStepGenerator();
        generator1.setSlots(List.of(
                createDummyTable("Input 1", column("NoRef", "A", "B")),
                createDummyTable("Input 2", column("NoRef", "A", "B"))
        ));
        generator1.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator1.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result1 = generator1.build(JIPipeProgressInfo.SILENT);
        assertEquals(1, result1.size());

        // Test mixed - one with reference, one without
        JIPipeMultiIterationStepGenerator generator2 = new JIPipeMultiIterationStepGenerator();
        generator2.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "B")),
                createDummyTable("Input 2", column("NoRef", "A", "B"))
        ));
        generator2.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator2.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result2 = generator2.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result2.size());
    }

    /**
     * Test with many unique reference values
     * Test performance and correctness
     */
    @Test
    public void testLargeValueSets() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();

        // Create columns with many unique values
        String[] values1 = new String[100];
        String[] values2 = new String[100];
        for (int i = 0; i < 100; i++) {
            values1[i] = "Value" + i;
            values2[i] = "Value" + i;
        }

        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#ID", values1)),
                createDummyTable("Input 2", column("#ID", values2))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 100 iteration steps
        assertEquals(100, result.size());

        // Verify each step has the correct matching using the test utility
        Set<Map<String, Set<Integer>>> actualResult = build(generator);
        assertEquals(100, actualResult.size());

        int stepIndex = 0;
        for (Map<String, Set<Integer>> step : actualResult) {
            assertEquals(2, step.size()); // Two inputs
            for (Set<Integer> rows : step.values()) {
                assertEquals(1, rows.size());
                assertTrue(rows.contains(stepIndex));
            }
            stepIndex++;
        }
    }

    /**
     * Test with different data types in annotation values
     * Numbers, strings, booleans, etc.
     */
    @Test
    public void testMixedDataTypes() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Mixed", "String", "123", "true", "3.14")),
                createDummyTable("Input 2", column("#Mixed", "String", "123", "true", "3.14"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 4 iteration steps
        assertEquals(4, result.size());

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
                ),
                step(
                        slot("Input 1", 3),
                        slot("Input 2", 3)
                )
        ), build(generator));
    }

    /**
     * Test handling of null annotation values
     * Test various combinations of empty/missing values
     */
    @Test
    public void testNullAndEmptyValues() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1", column("#Group", "A", "", "null", "B")),
                createDummyTable("Input 2", column("#Group", "A", "B", "C", ""))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 4 iteration steps
        assertEquals(4, result.size());

        // Expect the following layout
        assertEquals(expectedResult(
                step(
                        slot("Input 1", 0),
                        slot("Input 2", 0)
                ),
                step(
                        slot("Input 1", 1),
                        slot("Input 2", 3)
                ),
                step(
                        slot("Input 1", 2),
                        slot("Input 2", 2)
                ),
                step(
                        slot("Input 1", 3),
                        slot("Input 2", 1)
                )
        ), build(generator));
    }

    /**
     * Test with nested or complex annotation patterns
     * Multiple levels of annotations, hierarchical data
     */
    @Test
    public void testComplexAnnotationPatterns() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(
                createDummyTable("Input 1",
                        column("#Group.Level1", "A", "B"),
                        column("#Group.Level2", "X", "Y"),
                        column("#ID", "1", "2")),
                createDummyTable("Input 2",
                        column("#Group.Level1", "A", "B"),
                        column("#Group.Level2", "X", "Y"),
                        column("#ID", "1", "2"))
        ));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);

        // Expect 2 iteration steps based on hierarchical matching
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