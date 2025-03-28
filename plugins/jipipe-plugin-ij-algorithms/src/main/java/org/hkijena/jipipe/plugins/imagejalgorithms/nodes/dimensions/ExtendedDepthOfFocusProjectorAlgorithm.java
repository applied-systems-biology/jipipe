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
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;


@SetJIPipeDocumentation(name = "Custom extended depth of focus (Z/C/T)", description = "Performs a Z, C, or T-Projection ")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Score", create = true, description = "Optional image that scores each pixel. Must have the exact same size as the input. If not provided, the fallback method is used.", optional = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ExtendedDepthOfFocusProjectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ScoringMethod fallbackScoringMethod = ScoringMethod.Tenengrad;
    private SelectionMethod selectionMethod = SelectionMethod.Maximum;
    private HyperstackDimension projectedAxis = HyperstackDimension.Depth;

    public ExtendedDepthOfFocusProjectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtendedDepthOfFocusProjectorAlgorithm(ExtendedDepthOfFocusProjectorAlgorithm other) {
        super(other);
        this.projectedAxis = other.projectedAxis;
        this.selectionMethod = other.selectionMethod;
        this.fallbackScoringMethod = other.fallbackScoringMethod;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus score = ImageJUtils.unwrap(iterationStep.getInputData("Score", ImagePlusGreyscaleData.class, progressInfo));
        ImagePlus img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getImage();

        // Rotate the image if needed
        if(projectedAxis == HyperstackDimension.Channel) {
            img = ImageJUtils.duplicate(img);
            img.setDimensions();
        }

        if(score == null) {

        }

//        ImagePlus result;
//        if (img.getStackSize() > 1) {
//            switch (projectedAxis) {
//                case Channel: {
//                    result = processChannel(img, variables, progressInfo);
//                }
//                break;
//                case Depth: {
//                    result = processDepth(img, variables, progressInfo);
//                }
//                break;
//                case Frame: {
//                    result = processFrame(img, variables, progressInfo);
//                }
//                break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//        } else {
//            result = img;
//        }

//        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }


    @SetJIPipeDocumentation(name = "Fallback scoring method", description = "The scoring method that is applied per 2D plane if no custom score image is provided. Variance applies a local variance filter, while Tenengrad calculates the Sobel gradient.")
    @JIPipeParameter("fallback-scoring-method")
    public ScoringMethod getFallbackScoringMethod() {
        return fallbackScoringMethod;
    }

    @JIPipeParameter("fallback-scoring-method")
    public void setFallbackScoringMethod(ScoringMethod fallbackScoringMethod) {
        this.fallbackScoringMethod = fallbackScoringMethod;
    }

    @SetJIPipeDocumentation(name = "Pixel selection method", description = "Determines if the pixel with the minimum or the maximum score is chosen")
    @JIPipeParameter("selection-method")
    public SelectionMethod getSelectionMethod() {
        return selectionMethod;
    }

    @JIPipeParameter("selection-method")
    public void setSelectionMethod(SelectionMethod selectionMethod) {
        this.selectionMethod = selectionMethod;
    }

    @SetJIPipeDocumentation(name = "Projected axis", description = "The axis that should be projected")
    @JIPipeParameter("projected-axis")
    public HyperstackDimension getProjectedAxis() {
        return projectedAxis;
    }

    @JIPipeParameter("projected-axis")
    public void setProjectedAxis(HyperstackDimension projectedAxis) {
        this.projectedAxis = projectedAxis;
    }

    public enum SelectionMethod {
        Minimum,
        Maximum
    }

    public enum ScoringMethod {
        Variance,
        Tenengrad
    }
}
