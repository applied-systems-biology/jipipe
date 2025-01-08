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
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Equalize hyperstack dimensions (maximum)", description = "Makes the input image have the same dimensions as the maximum of all images. You can choose to make them equal in width/height, and " +
        "hyperstack dimensions (Z, C, T). Optionally annotates each output with information about its original size. " +
        "This node allows multiple inputs and outputs (IO-configuration). The maximum is calculated over all inputs and then returned via the corresponding output.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Canvas Size... (multiple images hyperstack, maximum)")
public class TransformEqualizeDimensionsToMaxAlgorithm extends JIPipeMergingAlgorithm {


    private boolean equalWidthAndHeight = false;
    private boolean equalHyperstackDimensions = true;
    private boolean copySlices = true;
    private boolean restoreOriginalAnnotations = true;
    private TransformScale2DAlgorithm scale2DAlgorithm;

    private OptionalTextAnnotationNameParameter originalWidthAnnotation = new OptionalTextAnnotationNameParameter("Original width", false);
    private OptionalTextAnnotationNameParameter originalHeightAnnotation = new OptionalTextAnnotationNameParameter("Original height", false);
    private OptionalTextAnnotationNameParameter originalSizeCAnnotation = new OptionalTextAnnotationNameParameter("Original channels", false);
    private OptionalTextAnnotationNameParameter originalSizeZAnnotation = new OptionalTextAnnotationNameParameter("Original depth", false);
    private OptionalTextAnnotationNameParameter originalSizeTAnnotation = new OptionalTextAnnotationNameParameter("Original frames", false);

    public TransformEqualizeDimensionsToMaxAlgorithm(JIPipeNodeInfo info) {
        super(info, createSlotConfiguration());
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setAnchor(Anchor.TopLeft);
        scale2DAlgorithm.setScaleMode(ScaleMode.Paste);
        registerSubParameter(scale2DAlgorithm);
    }

    public TransformEqualizeDimensionsToMaxAlgorithm(TransformEqualizeDimensionsToMaxAlgorithm other) {
        super(other);
        this.equalHyperstackDimensions = other.equalHyperstackDimensions;
        this.equalWidthAndHeight = other.equalWidthAndHeight;
        this.copySlices = other.copySlices;
        this.restoreOriginalAnnotations = other.restoreOriginalAnnotations;
        this.originalWidthAnnotation = new OptionalTextAnnotationNameParameter(other.originalWidthAnnotation);
        this.originalHeightAnnotation = new OptionalTextAnnotationNameParameter(other.originalHeightAnnotation);
        this.originalSizeCAnnotation = new OptionalTextAnnotationNameParameter(other.originalSizeCAnnotation);
        this.originalSizeZAnnotation = new OptionalTextAnnotationNameParameter(other.originalSizeZAnnotation);
        this.originalSizeTAnnotation = new OptionalTextAnnotationNameParameter(other.originalSizeTAnnotation);
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        registerSubParameter(scale2DAlgorithm);
    }

    private static JIPipeIOSlotConfiguration createSlotConfiguration() {
        JIPipeIOSlotConfiguration slotConfiguration = new JIPipeIOSlotConfiguration();
        slotConfiguration.setAllowedInputSlotTypes(Collections.singleton(ImagePlusData.class));
        slotConfiguration.setAllowedOutputSlotTypes(Collections.singleton(ImagePlusData.class));

        slotConfiguration.addInputSlot("Data", "The data", ImagePlusData.class, true);

        return slotConfiguration;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        progressInfo.log("Collecting images ...");

        int maxWidth = 0;
        int maxHeight = 0;
        int maxSizeC = 0;
        int maxSizeZ = 0;
        int maxSizeT = 0;

        for (JIPipeInputDataSlot inputSlot : getDataInputSlots()) {
            for (int row : iterationStep.getInputRows(inputSlot)) {
                if (progressInfo.isCancelled()) {
                    return;
                }
                ImagePlus imp = inputSlot.getData(row, ImagePlusData.class, progressInfo).getImage();
                maxWidth = Math.max(maxWidth, imp.getWidth());
                maxHeight = Math.max(maxHeight, imp.getHeight());
                maxSizeC = Math.max(maxSizeC, imp.getNChannels());
                maxSizeZ = Math.max(maxSizeZ, imp.getNSlices());
                maxSizeT = Math.max(maxSizeT, imp.getNFrames());
            }
        }

        progressInfo.log("Output dimensions are " + maxWidth + "x" + maxHeight + "x" + maxSizeC + "x" + maxSizeZ + "x" + maxSizeT);
        if (maxWidth == 0 || maxHeight == 0 || maxSizeC == 0 || maxSizeZ == 0 || maxSizeT == 0) {
            progressInfo.log("Nothing to do");
            return;
        }

        for (JIPipeInputDataSlot inputSlot : getDataInputSlots()) {
            for (int row : iterationStep.getInputRows(inputSlot)) {
                if (progressInfo.isCancelled()) {
                    return;
                }
                progressInfo.log("Processing row " + row + " in " + inputSlot.getName());
                ImagePlus image = inputSlot.getData(row, ImagePlusData.class, progressInfo).getImage();
                ImagePlus originalImage = image;

                if (equalWidthAndHeight) {
                    scale2DAlgorithm.clearSlotData(false, progressInfo);
                    scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(image), progressInfo);
                    scale2DAlgorithm.setxAxis(new OptionalJIPipeExpressionParameter(true, Integer.toString(maxWidth)));
                    scale2DAlgorithm.setyAxis(new OptionalJIPipeExpressionParameter(true, Integer.toString(maxHeight)));
                    scale2DAlgorithm.run(runContext, progressInfo.resolve("2D scaling"));
                    image = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
                    scale2DAlgorithm.clearSlotData(false, progressInfo);
                }
                if (equalHyperstackDimensions) {
                    image = ImageJUtils.ensureSize(image, maxSizeC, maxSizeZ, maxSizeT, copySlices);
                }

                List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>();
                List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();

                if(restoreOriginalAnnotations) {
                    textAnnotations = inputSlot.getTextAnnotations(row);
                    dataAnnotations = inputSlot.getDataAnnotations(row);
                }

                originalWidthAnnotation.addAnnotationIfEnabled(textAnnotations, String.valueOf(originalImage.getWidth()));
                originalHeightAnnotation.addAnnotationIfEnabled(textAnnotations, String.valueOf(originalImage.getHeight()));
                originalSizeCAnnotation.addAnnotationIfEnabled(textAnnotations, String.valueOf(originalImage.getNChannels()));
                originalSizeZAnnotation.addAnnotationIfEnabled(textAnnotations, String.valueOf(originalImage.getNSlices()));
                originalSizeTAnnotation.addAnnotationIfEnabled(textAnnotations, String.valueOf(originalImage.getNFrames()));

                iterationStep.addOutputData(inputSlot.getName(),
                        new ImagePlusData(image),
                        textAnnotations,
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        dataAnnotations,
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with original width", description = "If enabled, add the original width as annotation")
    @JIPipeParameter("original-width-annotation")
    public OptionalTextAnnotationNameParameter getOriginalWidthAnnotation() {
        return originalWidthAnnotation;
    }

    @JIPipeParameter("original-width-annotation")
    public void setOriginalWidthAnnotation(OptionalTextAnnotationNameParameter originalWidthAnnotation) {
        this.originalWidthAnnotation = originalWidthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original height", description = "If enabled, add the original height as annotation")
    @JIPipeParameter("original-height-annotation")
    public OptionalTextAnnotationNameParameter getOriginalHeightAnnotation() {
        return originalHeightAnnotation;
    }

    @JIPipeParameter("original-height-annotation")
    public void setOriginalHeightAnnotation(OptionalTextAnnotationNameParameter originalHeightAnnotation) {
        this.originalHeightAnnotation = originalHeightAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original channel number", description = "If enabled, add the original number of channels as annotation")
    @JIPipeParameter("original-size-c-annotation")
    public OptionalTextAnnotationNameParameter getOriginalSizeCAnnotation() {
        return originalSizeCAnnotation;
    }

    @JIPipeParameter("original-size-c-annotation")
    public void setOriginalSizeCAnnotation(OptionalTextAnnotationNameParameter originalSizeCAnnotation) {
        this.originalSizeCAnnotation = originalSizeCAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original depth", description = "If enabled, add the original depth (Z) as annotation")
    @JIPipeParameter("original-size-z-annotation")
    public OptionalTextAnnotationNameParameter getOriginalSizeZAnnotation() {
        return originalSizeZAnnotation;
    }

    @JIPipeParameter("original-size-z-annotation")
    public void setOriginalSizeZAnnotation(OptionalTextAnnotationNameParameter originalSizeZAnnotation) {
        this.originalSizeZAnnotation = originalSizeZAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original frame number", description = "If enabled, add the original number of frames as annotation")
    @JIPipeParameter("original-size-t-annotation")
    public OptionalTextAnnotationNameParameter getOriginalSizeTAnnotation() {
        return originalSizeTAnnotation;
    }

    @JIPipeParameter("original-size-t-annotation")
    public void setOriginalSizeTAnnotation(OptionalTextAnnotationNameParameter originalSizeTAnnotation) {
        this.originalSizeTAnnotation = originalSizeTAnnotation;
    }

    @SetJIPipeDocumentation(name = "Restore original annotations", description = "If enabled, restore the original annotations of the data")
    @JIPipeParameter("restore-original-annotations")
    public boolean isRestoreOriginalAnnotations() {
        return restoreOriginalAnnotations;
    }

    @JIPipeParameter("restore-original-annotations")
    public void setRestoreOriginalAnnotations(boolean restoreOriginalAnnotations) {
        this.restoreOriginalAnnotations = restoreOriginalAnnotations;
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
        if (!equalWidthAndHeight && subParameter == scale2DAlgorithm) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == scale2DAlgorithm) {
            if ("x-axis".equals(access.getKey()) || "y-axis".equals(access.getKey())) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }
}
