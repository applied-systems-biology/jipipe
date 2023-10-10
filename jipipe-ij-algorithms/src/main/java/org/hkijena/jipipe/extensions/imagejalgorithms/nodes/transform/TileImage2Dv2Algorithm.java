/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Tile image 2D", description = "Splits the image into tiles of a predefined size. If the image is not perfectly tileable, it is resized.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class TileImage2Dv2Algorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;
    private DefaultExpressionParameter tileSizeX = new DefaultExpressionParameter("512");
    private DefaultExpressionParameter tileSizeY = new DefaultExpressionParameter("512");
    private DefaultExpressionParameter overlapX = new DefaultExpressionParameter("0");
    private DefaultExpressionParameter overlapY = new DefaultExpressionParameter("0");
    private OptionalAnnotationNameParameter tileXAnnotation = new OptionalAnnotationNameParameter("Tile X", true);
    private OptionalAnnotationNameParameter tileYAnnotation = new OptionalAnnotationNameParameter("Tile Y", true);
    private OptionalAnnotationNameParameter numTilesX = new OptionalAnnotationNameParameter("Num Tiles X", true);
    private OptionalAnnotationNameParameter numTilesY = new OptionalAnnotationNameParameter("Num Tiles Y", true);
    private BorderMode borderMode = BorderMode.Constant;
    private OptionalAnnotationNameParameter tileRealXAnnotation = new OptionalAnnotationNameParameter("Original X", true);
    private OptionalAnnotationNameParameter tileRealYAnnotation = new OptionalAnnotationNameParameter("Original Y", true);
    private OptionalAnnotationNameParameter imageWidthAnnotation = new OptionalAnnotationNameParameter("Original width", true);
    private OptionalAnnotationNameParameter imageHeightAnnotation = new OptionalAnnotationNameParameter("Original height", true);
    private OptionalAnnotationNameParameter tileInsetXAnnotation = new OptionalAnnotationNameParameter("Inset X", true);
    private OptionalAnnotationNameParameter tileInsetYAnnotation = new OptionalAnnotationNameParameter("Inset Y", true);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public TileImage2Dv2Algorithm(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public TileImage2Dv2Algorithm(TileImage2Dv2Algorithm other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.tileSizeX = other.tileSizeX;
        this.tileSizeY = other.tileSizeY;
        this.tileXAnnotation = new OptionalAnnotationNameParameter(other.tileXAnnotation);
        this.tileYAnnotation = new OptionalAnnotationNameParameter(other.tileYAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.tileRealXAnnotation = new OptionalAnnotationNameParameter(other.tileRealXAnnotation);
        this.tileRealYAnnotation = new OptionalAnnotationNameParameter(other.tileRealYAnnotation);
        this.imageWidthAnnotation = new OptionalAnnotationNameParameter(other.imageWidthAnnotation);
        this.imageHeightAnnotation = new OptionalAnnotationNameParameter(other.imageHeightAnnotation);
        this.tileInsetXAnnotation = new OptionalAnnotationNameParameter(other.tileInsetXAnnotation);
        this.tileInsetYAnnotation = new OptionalAnnotationNameParameter(other.tileInsetYAnnotation);
        this.numTilesX = new OptionalAnnotationNameParameter(other.numTilesX);
        this.numTilesY = new OptionalAnnotationNameParameter(other.numTilesY);
        this.overlapX = other.overlapX;
        this.overlapY = other.overlapY;
        this.borderMode = other.borderMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus originalImg = img;

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");
        variables.set("width", img.getWidth());
        variables.set("height", img.getHeight());
        variables.set("num_c", img.getNChannels());
        variables.set("num_z", img.getNSlices());
        variables.set("num_t", img.getNFrames());
        variables.set("num_d", img.getNDimensions());

        final int tileSizeX_ = tileSizeX.evaluateToInteger(variables);
        final int tileSizeY_ = tileSizeY.evaluateToInteger(variables);
        final int overlapX_ = overlapX.evaluateToInteger(variables);
        final int overlapY_ = overlapY.evaluateToInteger(variables);

        final int realTileSizeX = tileSizeX_ + 2 * overlapX_;
        final int realTileSizeY = tileSizeY_ + 2 * overlapY_;
        final int nTilesX = (int) Math.ceil(1.0 * img.getWidth() / tileSizeX_);
        final int nTilesY = (int) Math.ceil(1.0 * img.getHeight() / tileSizeY_);

        // Add border to image
        {
            int width = img.getWidth();
            int height = img.getHeight();

            // First correct the tile size assuming no overlap
            if ((width % tileSizeX_) != 0) {
                width = (int) (Math.ceil(1.0 * width / tileSizeX_) * tileSizeX_);
            }
            if ((height % tileSizeY_) != 0) {
                height = (int) (Math.ceil(1.0 * height / tileSizeY_) * tileSizeY_);
            }

            // Insert overlap
            width += 2 * overlapX_;
            height += 2 * overlapY_;

            int left = overlapX_;
            int top = overlapY_;
            int right = width - originalImg.getWidth() - left;
            int bottom = height - originalImg.getHeight() - top;
            img = AddBorder2DAlgorithm.addBorder(img, left, top, right, bottom, borderMode, 0, Color.BLACK, progressInfo.resolve("Adding border due to overlap"));
        }

        for (int y = 0; y < nTilesY; y++) {
            for (int x = 0; x < nTilesX; x++) {
                Rectangle roi = new Rectangle(x * tileSizeX_, y * tileSizeY_, realTileSizeX, realTileSizeY);
                JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Tile", x + y * nTilesX, nTilesX * nTilesY);
                ImageStack tileStack = new ImageStack(realTileSizeX, realTileSizeY, img.getStackSize());
                ImagePlus finalImg = img;
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    ip.setRoi(roi);
                    ImageProcessor crop = ip.crop();
                    ip.setRoi((Roi) null);
                    tileStack.setProcessor(crop, index.zeroSliceIndexToOneStackIndex(finalImg));
                }, tileProgress);

                ImagePlus tileImage = new ImagePlus("Tile " + x + ", " + y, tileStack);
                tileImage.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
                tileImage.copyScale(img);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                tileXAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(x));
                tileYAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(y));
                numTilesX.addAnnotationIfEnabled(annotations, String.valueOf(nTilesX));
                numTilesY.addAnnotationIfEnabled(annotations, String.valueOf(nTilesY));
                imageWidthAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(originalImg.getWidth()));
                imageHeightAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(originalImg.getHeight()));
                tileRealXAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(x * tileSizeX_));
                tileRealYAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(y * tileSizeY_));
                tileInsetXAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(overlapX_));
                tileInsetYAnnotation.addAnnotationIfEnabled(annotations, String.valueOf(overlapY_));
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(tileImage), annotations, annotationMergeStrategy, tileProgress);
            }
        }
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }

    @JIPipeDocumentation(name = "Merge existing annotations", description = "Determines how existing annotations are merged")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Annotate with tile X", description = "If true, annotate each tile with its X location (in tile coordinates)")
    @JIPipeParameter("tile-x-annotation")
    public OptionalAnnotationNameParameter getTileXAnnotation() {
        return tileXAnnotation;
    }

    @JIPipeParameter("tile-x-annotation")
    public void setTileXAnnotation(OptionalAnnotationNameParameter tileXAnnotation) {
        this.tileXAnnotation = tileXAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with tile Y", description = "If true, annotate each tile with its Y location (in tile coordinates)")
    @JIPipeParameter("tile-y-annotation")
    public OptionalAnnotationNameParameter getTileYAnnotation() {
        return tileYAnnotation;
    }

    @JIPipeParameter("tile-y-annotation")
    public void setTileYAnnotation(OptionalAnnotationNameParameter tileYAnnotation) {
        this.tileYAnnotation = tileYAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with number of X tiles", description = "If true, annotate each tile with the number of tiles in the image")
    @JIPipeParameter("num-tile-x-annotation")
    public OptionalAnnotationNameParameter getNumTilesX() {
        return numTilesX;
    }

    @JIPipeParameter("num-tile-x-annotation")
    public void setNumTilesX(OptionalAnnotationNameParameter numTilesX) {
        this.numTilesX = numTilesX;
    }

    @JIPipeDocumentation(name = "Annotate with number of Y tiles", description = "If true, annotate each tile with the number of tiles in the image")
    @JIPipeParameter("num-tile-y-annotation")
    public OptionalAnnotationNameParameter getNumTilesY() {
        return numTilesY;
    }

    @JIPipeParameter("num-tile-y-annotation")
    public void setNumTilesY(OptionalAnnotationNameParameter numTilesY) {
        this.numTilesY = numTilesY;
    }

    @JIPipeDocumentation(name = "Tile width", description = "The width of a tile")
    @JIPipeParameter(value = "tile-x", important = true)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getTileSizeX() {
        return tileSizeX;
    }

    @JIPipeParameter("tile-x")
    public void setTileSizeX(DefaultExpressionParameter tileSizeX) {
        this.tileSizeX = tileSizeX;
    }

    @JIPipeDocumentation(name = "Tile height", description = "The height of a tile")
    @JIPipeParameter(value = "tile-y", important = true)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getTileSizeY() {
        return tileSizeY;
    }

    @JIPipeParameter("tile-y")
    public void setTileSizeY(DefaultExpressionParameter tileSizeY) {
        this.tileSizeY = tileSizeY;
    }

    @JIPipeDocumentation(name = "Annotate with original X location", description = "If true, annotate the tile with its location within the original image. Only works if the scaling anchor is top left")
    @JIPipeParameter("tile-real-x-annotation")
    public OptionalAnnotationNameParameter getTileRealXAnnotation() {
        return tileRealXAnnotation;
    }

    @JIPipeParameter("tile-real-x-annotation")
    public void setTileRealXAnnotation(OptionalAnnotationNameParameter tileRealXAnnotation) {
        this.tileRealXAnnotation = tileRealXAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with original Y location", description = "If true, annotate the tile with its location within the original image. Only works if the scaling anchor is top left")
    @JIPipeParameter("tile-real-y-annotation")
    public OptionalAnnotationNameParameter getTileRealYAnnotation() {
        return tileRealYAnnotation;
    }

    @JIPipeParameter("tile-real-y-annotation")
    public void setTileRealYAnnotation(OptionalAnnotationNameParameter tileRealYAnnotation) {
        this.tileRealYAnnotation = tileRealYAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with original width", description = "If true, annotate the tile with the width of the original image")
    @JIPipeParameter("tile-original-width-annotation")
    public OptionalAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width-annotation")
    public void setImageWidthAnnotation(OptionalAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with original height", description = "If true, annotate the tile with the height of the original image")
    @JIPipeParameter("tile-original-height-annotation")
    public OptionalAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height-annotation")
    public void setImageHeightAnnotation(OptionalAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }

    @JIPipeDocumentation(name = "Overlap (X)", description = "Sets the overlap of the tiles. Please note that the size of the tiles will increase.")
    @JIPipeParameter("overlap-x")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getOverlapX() {
        return overlapX;
    }

    @JIPipeParameter("overlap-x")
    public void setOverlapX(DefaultExpressionParameter overlapX) {
        this.overlapX = overlapX;
    }

    @JIPipeDocumentation(name = "Overlap (Y)", description = "Sets the overlap of the tiles. Please note that the size of the tiles will increase.")
    @JIPipeParameter("overlap-y")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getOverlapY() {
        return overlapY;
    }

    @JIPipeParameter("overlap-y")
    public void setOverlapY(DefaultExpressionParameter overlapY) {
        this.overlapY = overlapY;
    }

    @JIPipeDocumentation(name = "Annotate with tile inset (X)", description = "If enabled, each tile is annotated with its inset, meaning the overlap in the X direction. This value can be utilized to remove the overlap at a later point.")
    @JIPipeParameter("tile-inset-x-annotation")
    public OptionalAnnotationNameParameter getTileInsetXAnnotation() {
        return tileInsetXAnnotation;
    }

    @JIPipeParameter("tile-inset-x-annotation")
    public void setTileInsetXAnnotation(OptionalAnnotationNameParameter tileInsetXAnnotation) {
        this.tileInsetXAnnotation = tileInsetXAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with tile inset (Y)", description = "If enabled, each tile is annotated with its inset, meaning the overlap in the Y direction. This value can be utilized to remove the overlap at a later point.")
    @JIPipeParameter("tile-inset-y-annotation")
    public OptionalAnnotationNameParameter getTileInsetYAnnotation() {
        return tileInsetYAnnotation;
    }

    @JIPipeParameter("tile-inset-y-annotation")
    public void setTileInsetYAnnotation(OptionalAnnotationNameParameter tileInsetYAnnotation) {
        this.tileInsetYAnnotation = tileInsetYAnnotation;
    }

    @JIPipeDocumentation(name = "Border mode", description = "Determines how the image is expanded with borders. Only applicable if an overlap is set.")
    @JIPipeParameter("border-mode")
    public BorderMode getBorderMode() {
        return borderMode;
    }

    @JIPipeParameter("border-mode")
    public void setBorderMode(BorderMode borderMode) {
        this.borderMode = borderMode;
    }
}
