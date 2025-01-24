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
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
    private OptionalTextAnnotationNameParameter annotationX = new OptionalTextAnnotationNameParameter("X", false);
    private OptionalTextAnnotationNameParameter annotationY = new OptionalTextAnnotationNameParameter("Y", false);
    private OptionalTextAnnotationNameParameter annotationBoundingWidth = new OptionalTextAnnotationNameParameter("Width", false);
    private OptionalTextAnnotationNameParameter annotationBoundingHeight = new OptionalTextAnnotationNameParameter("Height", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

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
        this.annotationX = new OptionalTextAnnotationNameParameter(other.annotationX);
        this.annotationY = new OptionalTextAnnotationNameParameter(other.annotationY);
        this.annotationBoundingWidth = new OptionalTextAnnotationNameParameter(other.annotationBoundingWidth);
        this.annotationBoundingHeight = new OptionalTextAnnotationNameParameter(other.annotationBoundingHeight);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Annotate with X location", description = "If enabled, the generated image is annotated with the top-left X coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-x", uiOrder = -50)
    public OptionalTextAnnotationNameParameter getAnnotationX() {
        return annotationX;
    }

    @JIPipeParameter("annotation-x")
    public void setAnnotationX(OptionalTextAnnotationNameParameter annotationX) {
        this.annotationX = annotationX;
    }

    @SetJIPipeDocumentation(name = "Annotate with Y location", description = "If enabled, the generated image is annotated with the top-left Y coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-y", uiOrder = -45)
    public OptionalTextAnnotationNameParameter getAnnotationY() {
        return annotationY;
    }

    @JIPipeParameter("annotation-y")
    public void setAnnotationY(OptionalTextAnnotationNameParameter annotationY) {
        this.annotationY = annotationY;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI width", description = "If enabled, the generated image is annotated with the width of the ROI")
    @JIPipeParameter("annotation-width")
    public OptionalTextAnnotationNameParameter getAnnotationBoundingWidth() {
        return annotationBoundingWidth;
    }

    @JIPipeParameter("annotation-width")
    public void setAnnotationBoundingWidth(OptionalTextAnnotationNameParameter annotationBoundingWidth) {
        this.annotationBoundingWidth = annotationBoundingWidth;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI height", description = "If enabled, the generated image is annotated with the height of the ROI")
    @JIPipeParameter("annotation-height")
    public OptionalTextAnnotationNameParameter getAnnotationBoundingHeight() {
        return annotationBoundingHeight;
    }

    @JIPipeParameter("annotation-height")
    public void setAnnotationBoundingHeight(OptionalTextAnnotationNameParameter annotationBoundingHeight) {
        this.annotationBoundingHeight = annotationBoundingHeight;
    }

    @SetJIPipeDocumentation(name = "Annotation merging", description = "Determines how generated annotations are merged with existing annotations")
    @JIPipeParameter("annotation-merging")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merging")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
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
        Image5DExpressionParameterVariablesInfo.writeToVariables(img, variables);
        Rectangle bounds = roi.getInsideArea(imageArea, variables);
        if (bounds == null || bounds.width == 0 || bounds.height == 0) {
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

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        annotationX.addAnnotationIfEnabled(annotations, String.valueOf(bounds.x));
        annotationY.addAnnotationIfEnabled(annotations, String.valueOf(bounds.y));
        annotationBoundingWidth.addAnnotationIfEnabled(annotations, String.valueOf(bounds.width));
        annotationBoundingHeight.addAnnotationIfEnabled(annotations, String.valueOf(bounds.height));

        ImagePlus croppedImg = ImageJUtils.cropLegacy(img, bounds, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(croppedImg), annotations, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI", description = "Defines the area to crop.")
    @JIPipeParameter("roi")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public Margin getRoi() {
        return roi;
    }

    @JIPipeParameter("roi")
    public void setRoi(Margin roi) {
        this.roi = roi;

    }
}
