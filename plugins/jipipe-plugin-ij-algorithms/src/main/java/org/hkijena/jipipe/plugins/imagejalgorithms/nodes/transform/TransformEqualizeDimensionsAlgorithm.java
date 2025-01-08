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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;

@SetJIPipeDocumentation(name = "Equalize hyperstack dimensions", description = "Makes the input image have the same dimensions as the reference image. You can choose to make them equal in width/height, and " +
        "hyperstack dimensions (Z, C, T)")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Canvas Size... (multiple images hyperstack)")
public class TransformEqualizeDimensionsAlgorithm extends JIPipeIteratingAlgorithm {


    private boolean equalWidthAndHeight = false;
    private boolean equalHyperstackDimensions = true;
    private boolean copySlices = true;
    private TransformScale2DAlgorithm scale2DAlgorithm;

    public TransformEqualizeDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        registerSubParameter(scale2DAlgorithm);
    }

    public TransformEqualizeDimensionsAlgorithm(TransformEqualizeDimensionsAlgorithm other) {
        super(other);
        this.equalHyperstackDimensions = other.equalHyperstackDimensions;
        this.equalWidthAndHeight = other.equalWidthAndHeight;
        this.copySlices = other.copySlices;
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        registerSubParameter(scale2DAlgorithm);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getImage();
        ImagePlus referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();

        if (equalWidthAndHeight) {
            scale2DAlgorithm.clearSlotData(false, progressInfo);
            scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(image), progressInfo);
            scale2DAlgorithm.setxAxis(new OptionalJIPipeExpressionParameter(true, Integer.toString(referenceImage.getWidth())));
            scale2DAlgorithm.setyAxis(new OptionalJIPipeExpressionParameter(true, Integer.toString(referenceImage.getHeight())));
            scale2DAlgorithm.run(runContext, progressInfo.resolve("2D scaling"));
            image = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
            scale2DAlgorithm.clearSlotData(false, progressInfo);
        }
        if (equalHyperstackDimensions) {
            image = ImageJUtils.ensureEqualSize(image, referenceImage, copySlices);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Equalize width and height", description = "If enabled, the width and height are equalized")
    @JIPipeParameter("equal-width-and-height")
    public boolean isEqualWidthAndHeight() {
        return equalWidthAndHeight;
    }

    @JIPipeParameter("equal-width-and-height")
    public void setEqualWidthAndHeight(boolean equalWidthAndHeight) {
        this.equalWidthAndHeight = equalWidthAndHeight;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Equalize hyperstack dimensions", description = "If enabled, the hyperstack dimensions are equalized")
    @JIPipeParameter("equal-hyperstack-dimensions")
    public boolean isEqualHyperstackDimensions() {
        return equalHyperstackDimensions;
    }

    @JIPipeParameter("equal-hyperstack-dimensions")
    public void setEqualHyperstackDimensions(boolean equalHyperstackDimensions) {
        this.equalHyperstackDimensions = equalHyperstackDimensions;
    }

    @SetJIPipeDocumentation(name = "Newly generated slices", description = "Determines how new slices are generated, if needed. You can either repeat the last available slice or make new slices zero/black.")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Repeat last available", falseLabel = "Create empty slice")
    @JIPipeParameter("copy-slices")
    public boolean isCopySlices() {
        return copySlices;
    }

    @JIPipeParameter("copy-slices")
    public void setCopySlices(boolean copySlices) {
        this.copySlices = copySlices;
    }

    @SetJIPipeDocumentation(name = "Scaling", description = "The following settings determine how the image is scaled if it is not perfectly tileable.")
    @JIPipeParameter(value = "scale-algorithm")
    public TransformScale2DAlgorithm getScale2DAlgorithm() {
        return scale2DAlgorithm;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if(!equalWidthAndHeight && subParameter == scale2DAlgorithm) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if(access.getSource() == scale2DAlgorithm) {
            if("x-axis".equals(access.getKey()) || "y-axis".equals(access.getKey())) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }
}
