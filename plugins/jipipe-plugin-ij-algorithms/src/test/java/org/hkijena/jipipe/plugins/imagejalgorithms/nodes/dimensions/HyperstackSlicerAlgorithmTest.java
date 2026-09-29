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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMutableIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for issue 1329: nodes must forward input annotations as
 * expression variables so that ranges can be computed from them.
 */
class HyperstackSlicerAlgorithmTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * A 2x2 hyperstack with the given number of Z slices.
     */
    private static ImagePlus hyperstack(int sizeZ) {
        ij.ImageStack stack = new ij.ImageStack(2, 2);
        for (int i = 0; i < sizeZ; i++) {
            stack.addSlice(new ShortProcessor(2, 2));
        }
        ImagePlus image = new ImagePlus("test", stack);
        image.setDimensions(1, sizeZ, 1);
        return image;
    }

    /**
     * Populates the node input with one annotated image and returns a wired iteration step
     * that mirrors how the framework merges input annotations into the iteration step.
     */
    private static JIPipeSingleIterationStep wireAnnotatedInput(HyperstackSlicerAlgorithm node, ImagePlus image, String annotationName, String annotationValue) {
        JIPipeTextAnnotation annotation = new JIPipeTextAnnotation(annotationName, annotationValue);
        node.getInputSlot("Input").addData(new ImagePlusData(image), List.of(annotation), org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode.Merge, new JIPipeMutableDataContext(), new JIPipeProgressInfo());
        JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(node);
        iterationStep.setInputData(node.getInputSlot("Input"), 0);
        iterationStep.addMergedTextAnnotations(node.getInputSlot("Input").getTextAnnotations(0), org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode.Merge);
        return iterationStep;
    }

    /**
     * Issue 1329: the reported expression MAKE_SEQUENCE(0, TO_NUMBER($"Image Z slices") - 1) must
     * resolve the annotation "Image Z slices" as variable.
     */
    @Test
    void testAnnotationForwardedAsVariable() {
        HyperstackSlicerAlgorithm node = JIPipe.createNode(HyperstackSlicerAlgorithm.class);
        IntegerRange indicesZ = node.getIndicesZ();
        indicesZ.setUseExpression(true);
        indicesZ.getExpression().setExpression("MAKE_SEQUENCE(0, TO_NUMBER($\"Image Z slices\") - 1)");
        node.setIndicesZ(indicesZ);

        JIPipeSingleIterationStep iterationStep = wireAnnotatedInput(node, hyperstack(3), "Image Z slices", "3");
        node.runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());

        assertEquals(1, node.getOutputSlot("Output").getRowCount());
        // MAKE_SEQUENCE(0, 3 - 1) yields the indices 0,1 -> two Z slices in the output
        ImagePlus output = node.getOutputSlot("Output").getData(0, ImagePlusData.class, new JIPipeProgressInfo()).getImage();
        assertEquals(2, output.getNSlices());
    }

    /**
     * The same node must also resolve the standard variable range expression without annotations.
     */
    @Test
    void testMinAndMaxVariablesStillWork() {
        HyperstackSlicerAlgorithm node = JIPipe.createNode(HyperstackSlicerAlgorithm.class);
        IntegerRange indicesZ = node.getIndicesZ();
        indicesZ.setUseExpression(true);
        indicesZ.getExpression().setExpression("MAKE_SEQUENCE(min, max - 1)");
        node.setIndicesZ(indicesZ);

        JIPipeSingleIterationStep iterationStep = wireAnnotatedInput(node, hyperstack(3), "Image Z slices", "3");
        node.runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());

        assertEquals(1, node.getOutputSlot("Output").getRowCount());
        ImagePlus output = node.getOutputSlot("Output").getData(0, ImagePlusData.class, new JIPipeProgressInfo()).getImage();
        assertEquals(2, output.getNSlices());
    }
}
