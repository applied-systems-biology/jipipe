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
import ij.ImageStack;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Crop 2D image", description = "Crops a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image", aliasName = "Crop")
public class TransformCrop2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin roi = new Margin();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformCrop2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public TransformCrop2DAlgorithm(TransformCrop2DAlgorithm other) {
        super(other);
        this.roi = new Margin(other.roi);
    }

    public static ImagePlus crop(JIPipeProgressInfo progressInfo, ImagePlus img, Rectangle cropped) {
        ImagePlus croppedImg;
        if (img.hasImageStack()) {
            ImageStack result = new ImageStack(cropped.width, cropped.height, img.getStackSize());
            ImageJUtils.forEachIndexedZCTSlice(img, (imp, index) -> {
                imp.setRoi(cropped);
                ImageProcessor croppedImage = imp.crop();
                imp.resetRoi();
                result.setProcessor(croppedImage, index.zeroSliceIndexToOneStackIndex(img));
            }, progressInfo);
            croppedImg = new ImagePlus("Cropped", result);
            croppedImg.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
            croppedImg.copyScale(img);
        } else {
            ImageProcessor imp = img.getProcessor();
            imp.setRoi(cropped);
            ImageProcessor croppedImage = imp.crop();
            imp.resetRoi();
            croppedImg = new ImagePlus("Cropped", croppedImage);
            croppedImg.copyScale(img);
        }
        return croppedImg;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        Rectangle imageArea = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        Rectangle cropped = roi.getInsideArea(imageArea, variables);
        if (cropped == null || cropped.width == 0 || cropped.height == 0) {
            throw new JIPipeValidationRuntimeException(new NullPointerException("Cropped rectangle is null or empty!"),
                    "Cropped rectangle is empty!",
                    "The input for the cropping operator was an image of size w=" + img.getWidth() + ", h=" + img.getHeight() + ". The resulting ROI is empty.",
                    "Please check the parameters and ensure that a non-empty area is cropped out.");
        }
//        if(!imageArea.inter(cropped)) {
//            throw new UserFriendlyRuntimeException(new NullPointerException("Cropped rectangle is outside of image!"),
//                    "Cropped rectangle is outside of the image!",
//                    "Algorithm '" +getName() + "'",
//                    "The input for the cropping operator was an image of size w=" + img.getWidth() + ", h=" + img.getHeight() + ". The resulting ROI is a rectangle with following properties: " +
//                            cropped + ". The rectangle is outside of the image dimensions.",
//                    "Please check the parameters and ensure that you only crop ");
//        }

        ImagePlus croppedImg = crop(progressInfo, img, cropped);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(croppedImg), progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI", description = "Defines the area to crop.")
    @JIPipeParameter("roi")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public Margin getRoi() {
        return roi;
    }

    @JIPipeParameter("roi")
    public void setRoi(Margin roi) {
        this.roi = roi;

    }
}
