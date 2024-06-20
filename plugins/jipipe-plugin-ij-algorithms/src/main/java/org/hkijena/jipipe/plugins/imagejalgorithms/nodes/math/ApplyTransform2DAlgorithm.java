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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Apply transformation", description = "Applies a mathematical function to each pixel. Supported operations are: ABS, INVERT, SQUARE, LOGARITHM, SQRT, and EXP")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nMath", aliasName = "Math operation (pixel transformation)")
public class ApplyTransform2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Transformation transformation = Transformation.Absolute;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyTransform2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ApplyTransform2DAlgorithm(ApplyTransform2DAlgorithm other) {
        super(other);
        this.transformation = other.transformation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ip -> {
            switch (transformation) {
                case Absolute:
                    ip.abs();
                    break;
                case Invert:
                    ip.invert();
                    break;
                case Square:
                    ip.sqr();
                    break;
                case Logarithm:
                    ip.ln();
                    break;
                case SquareRoot:
                    ip.sqrt();
                    break;
                case Exponential:
                    ip.exp();
                    break;
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel.")
    @JIPipeParameter("transformation-function")
    public Transformation getTransformation() {
        return transformation;
    }

    @JIPipeParameter("transformation-function")
    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;

    }

    /**
     * Available transformation functions
     */
    public enum Transformation {
        Absolute,
        Exponential,
        Invert,
        Logarithm,
        Square,
        SquareRoot
    }
}
