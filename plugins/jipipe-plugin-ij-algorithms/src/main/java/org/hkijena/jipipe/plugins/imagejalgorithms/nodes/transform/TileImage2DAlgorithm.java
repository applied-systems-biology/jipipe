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
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Tile image", description =
        "Splits the image into tiles of a predefined size. If the image is not perfectly tileable, it is resized.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@Deprecated
@LabelAsJIPipeHidden
public class TileImage2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int tileSizeX = 512;
    private int tileSizeY = 512;

    private int overlapX = 0;

    private int overlapY = 0;
    private OptionalTextAnnotationNameParameter tileXAnnotation = new OptionalTextAnnotationNameParameter("Tile X", true);
    private OptionalTextAnnotationNameParameter tileYAnnotation = new OptionalTextAnnotationNameParameter("Tile Y", true);
    private OptionalTextAnnotationNameParameter numTilesX = new OptionalTextAnnotationNameParameter("Num Tiles X", true);
    private OptionalTextAnnotationNameParameter numTilesY = new OptionalTextAnnotationNameParameter("Num Tiles Y", true);

    private BorderMode borderMode = BorderMode.Constant;

    private OptionalTextAnnotationNameParameter tileRealXAnnotation = new OptionalTextAnnotationNameParameter("Original X", true);

    private OptionalTextAnnotationNameParameter tileRealYAnnotation = new OptionalTextAnnotationNameParameter("Original Y", true);

    private OptionalTextAnnotationNameParameter imageWidthAnnotation = new OptionalTextAnnotationNameParameter("Original width", true);

    private OptionalTextAnnotationNameParameter imageHeightAnnotation = new OptionalTextAnnotationNameParameter("Original height", true);

    private OptionalTextAnnotationNameParameter tileInsetXAnnotation = new OptionalTextAnnotationNameParameter("Inset X", true);

    private OptionalTextAnnotationNameParameter tileInsetYAnnotation = new OptionalTextAnnotationNameParameter("Inset Y", true);

    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public TileImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TileImage2DAlgorithm(TileImage2DAlgorithm other) {
        super(other);
        this.tileSizeX = other.tileSizeX;
        this.tileSizeY = other.tileSizeY;
        this.tileXAnnotation = new OptionalTextAnnotationNameParameter(other.tileXAnnotation);
        this.tileYAnnotation = new OptionalTextAnnotationNameParameter(other.tileYAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.tileRealXAnnotation = new OptionalTextAnnotationNameParameter(other.tileRealXAnnotation);
        this.tileRealYAnnotation = new OptionalTextAnnotationNameParameter(other.tileRealYAnnotation);
        this.imageWidthAnnotation = new OptionalTextAnnotationNameParameter(other.imageWidthAnnotation);
        this.imageHeightAnnotation = new OptionalTextAnnotationNameParameter(other.imageHeightAnnotation);
        this.tileInsetXAnnotation = new OptionalTextAnnotationNameParameter(other.tileInsetXAnnotation);
        this.tileInsetYAnnotation = new OptionalTextAnnotationNameParameter(other.tileInsetYAnnotation);
        this.overlapX = other.overlapX;
        this.overlapY = other.overlapY;
        this.borderMode = other.borderMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus originalImg = img;

        final int realTileSizeX = tileSizeX + 2 * overlapX;
        final int realTileSizeY = tileSizeY + 2 * overlapY;
        final int nTilesX = (int) Math.ceil(1.0 * img.getWidth() / tileSizeX);
        final int nTilesY = (int) Math.ceil(1.0 * img.getHeight() / tileSizeY);

        // Add border to image
        {
            int width = img.getWidth();
            int height = img.getHeight();

            // First correct the tile size assuming no overlap
            if ((width % tileSizeX) != 0) {
                width = (int) (Math.ceil(1.0 * width / tileSizeX) * tileSizeX);
            }
            if ((height % tileSizeY) != 0) {
                height = (int) (Math.ceil(1.0 * height / tileSizeY) * tileSizeY);
            }

            // Insert overlap
            width += 2 * overlapX;
            height += 2 * overlapY;

            int left = overlapX;
            int top = overlapY;
            int right = width - originalImg.getWidth() - left;
            int bottom = height - originalImg.getHeight() - top;
            img = AddBorder2DAlgorithm.addBorder(img, left, top, right, bottom, borderMode, 0, Color.BLACK, progressInfo.resolve("Adding border due to overlap"));
        }

        for (int y = 0; y < nTilesY; y++) {
            for (int x = 0; x < nTilesX; x++) {
                Rectangle roi = new Rectangle(x * tileSizeX, y * tileSizeY, realTileSizeX, realTileSizeY);
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
                tileXAnnotation.addAnnotationIfEnabled(annotations, x + "");
                tileYAnnotation.addAnnotationIfEnabled(annotations, y + "");
                numTilesX.addAnnotationIfEnabled(annotations, nTilesX + "");
                numTilesY.addAnnotationIfEnabled(annotations, nTilesY + "");
                imageWidthAnnotation.addAnnotationIfEnabled(annotations, originalImg.getWidth() + "");
                imageHeightAnnotation.addAnnotationIfEnabled(annotations, originalImg.getHeight() + "");
                tileRealXAnnotation.addAnnotationIfEnabled(annotations, x * tileSizeX + "");
                tileRealYAnnotation.addAnnotationIfEnabled(annotations, y * tileSizeY + "");
                tileInsetXAnnotation.addAnnotationIfEnabled(annotations, overlapX + "");
                tileInsetYAnnotation.addAnnotationIfEnabled(annotations, overlapY + "");
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(tileImage), annotations, annotationMergeStrategy, tileProgress);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Merge existing annotations", description = "Determines how existing annotations are merged")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Annotate with tile X", description = "If true, annotate each tile with its X location (in tile coordinates)")
    @JIPipeParameter("tile-x-annotation")
    public OptionalTextAnnotationNameParameter getTileXAnnotation() {
        return tileXAnnotation;
    }

    @JIPipeParameter("tile-x-annotation")
    public void setTileXAnnotation(OptionalTextAnnotationNameParameter tileXAnnotation) {
        this.tileXAnnotation = tileXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with tile Y", description = "If true, annotate each tile with its Y location (in tile coordinates)")
    @JIPipeParameter("tile-y-annotation")
    public OptionalTextAnnotationNameParameter getTileYAnnotation() {
        return tileYAnnotation;
    }

    @JIPipeParameter("tile-y-annotation")
    public void setTileYAnnotation(OptionalTextAnnotationNameParameter tileYAnnotation) {
        this.tileYAnnotation = tileYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of X tiles", description = "If true, annotate each tile with the number of tiles in the image")
    @JIPipeParameter("num-tile-x-annotation")
    public OptionalTextAnnotationNameParameter getNumTilesX() {
        return numTilesX;
    }

    @JIPipeParameter("num-tile-x-annotation")
    public void setNumTilesX(OptionalTextAnnotationNameParameter numTilesX) {
        this.numTilesX = numTilesX;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of Y tiles", description = "If true, annotate each tile with the number of tiles in the image")
    @JIPipeParameter("num-tile-y-annotation")
    public OptionalTextAnnotationNameParameter getNumTilesY() {
        return numTilesY;
    }

    @JIPipeParameter("num-tile-y-annotation")
    public void setNumTilesY(OptionalTextAnnotationNameParameter numTilesY) {
        this.numTilesY = numTilesY;
    }

    @SetJIPipeDocumentation(name = "Tile width", description = "The width of a tile")
    @JIPipeParameter("tile-x")
    public int getTileSizeX() {
        return tileSizeX;
    }

    @JIPipeParameter("tile-x")
    public void setTileSizeX(int tileSizeX) {
        this.tileSizeX = tileSizeX;
    }

    @SetJIPipeDocumentation(name = "Tile height", description = "The height of a tile")
    @JIPipeParameter("tile-y")
    public int getTileSizeY() {
        return tileSizeY;
    }

    @JIPipeParameter("tile-y")
    public void setTileSizeY(int tileSizeY) {
        this.tileSizeY = tileSizeY;
    }

    @SetJIPipeDocumentation(name = "Annotate with original X location", description = "If true, annotate the tile with its location within the original image. Only works if the scaling anchor is top left")
    @JIPipeParameter("tile-real-x-annotation")
    public OptionalTextAnnotationNameParameter getTileRealXAnnotation() {
        return tileRealXAnnotation;
    }

    @JIPipeParameter("tile-real-x-annotation")
    public void setTileRealXAnnotation(OptionalTextAnnotationNameParameter tileRealXAnnotation) {
        this.tileRealXAnnotation = tileRealXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original Y location", description = "If true, annotate the tile with its location within the original image. Only works if the scaling anchor is top left")
    @JIPipeParameter("tile-real-y-annotation")
    public OptionalTextAnnotationNameParameter getTileRealYAnnotation() {
        return tileRealYAnnotation;
    }

    @JIPipeParameter("tile-real-y-annotation")
    public void setTileRealYAnnotation(OptionalTextAnnotationNameParameter tileRealYAnnotation) {
        this.tileRealYAnnotation = tileRealYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original width", description = "If true, annotate the tile with the width of the original image")
    @JIPipeParameter("tile-original-width-annotation")
    public OptionalTextAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width-annotation")
    public void setImageWidthAnnotation(OptionalTextAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with original height", description = "If true, annotate the tile with the height of the original image")
    @JIPipeParameter("tile-original-height-annotation")
    public OptionalTextAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height-annotation")
    public void setImageHeightAnnotation(OptionalTextAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }

    @SetJIPipeDocumentation(name = "Overlap (X)", description = "Sets the overlap of the tiles. Please note that the size of the tiles will increase.")
    @JIPipeParameter("overlap-x")
    public int getOverlapX() {
        return overlapX;
    }

    @JIPipeParameter("overlap-x")
    public boolean setOverlapX(int overlapX) {
        if (overlapX < 0)
            return false;
        this.overlapX = overlapX;
        return true;
    }

    @SetJIPipeDocumentation(name = "Overlap (Y)", description = "Sets the overlap of the tiles. Please note that the size of the tiles will increase.")
    @JIPipeParameter("overlap-y")
    public int getOverlapY() {
        return overlapY;
    }

    @JIPipeParameter("overlap-y")
    public void setOverlapY(int overlapY) {
        this.overlapY = overlapY;
    }

    @SetJIPipeDocumentation(name = "Annotate with tile inset (X)", description = "If enabled, each tile is annotated with its inset, meaning the overlap in the X direction. This value can be utilized to remove the overlap at a later point.")
    @JIPipeParameter("tile-inset-x-annotation")
    public OptionalTextAnnotationNameParameter getTileInsetXAnnotation() {
        return tileInsetXAnnotation;
    }

    @JIPipeParameter("tile-inset-x-annotation")
    public void setTileInsetXAnnotation(OptionalTextAnnotationNameParameter tileInsetXAnnotation) {
        this.tileInsetXAnnotation = tileInsetXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with tile inset (Y)", description = "If enabled, each tile is annotated with its inset, meaning the overlap in the Y direction. This value can be utilized to remove the overlap at a later point.")
    @JIPipeParameter("tile-inset-y-annotation")
    public OptionalTextAnnotationNameParameter getTileInsetYAnnotation() {
        return tileInsetYAnnotation;
    }

    @JIPipeParameter("tile-inset-y-annotation")
    public void setTileInsetYAnnotation(OptionalTextAnnotationNameParameter tileInsetYAnnotation) {
        this.tileInsetYAnnotation = tileInsetYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Border mode", description = "Determines how the image is expanded with borders. Only applicable if an overlap is set.")
    @JIPipeParameter("border-mode")
    public BorderMode getBorderMode() {
        return borderMode;
    }

    @JIPipeParameter("border-mode")
    public void setBorderMode(BorderMode borderMode) {
        this.borderMode = borderMode;
    }
}
