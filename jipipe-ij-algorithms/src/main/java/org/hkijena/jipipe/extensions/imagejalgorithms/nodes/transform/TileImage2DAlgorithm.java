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
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Tile image", description = "This node is deprecated. Re-add 'Tile image'.\n\n" +
        "Splits the image into tiles of a predefined size. If the image is not perfectly tileable, it is resized.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@Deprecated
@JIPipeHidden
public class TileImage2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int tileSizeX = 512;
    private int tileSizeY = 512;

    private int overlapX = 0;

    private int overlapY = 0;
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

    public TileImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TileImage2DAlgorithm(TileImage2DAlgorithm other) {
        super(other);
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
        this.overlapX = other.overlapX;
        this.overlapY = other.overlapY;
        this.borderMode = other.borderMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
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
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(tileImage), annotations, annotationMergeStrategy, tileProgress);
            }
        }
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

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        report.resolve("Tile width").checkIfWithin(this, tileSizeX, 0, Double.POSITIVE_INFINITY, false, false);
        report.resolve("Tile height").checkIfWithin(this, tileSizeY, 0, Double.POSITIVE_INFINITY, false, false);
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
    @JIPipeParameter("tile-x")
    public int getTileSizeX() {
        return tileSizeX;
    }

    @JIPipeParameter("tile-x")
    public void setTileSizeX(int tileSizeX) {
        this.tileSizeX = tileSizeX;
    }

    @JIPipeDocumentation(name = "Tile height", description = "The height of a tile")
    @JIPipeParameter("tile-y")
    public int getTileSizeY() {
        return tileSizeY;
    }

    @JIPipeParameter("tile-y")
    public void setTileSizeY(int tileSizeY) {
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

    @JIPipeDocumentation(name = "Overlap (Y)", description = "Sets the overlap of the tiles. Please note that the size of the tiles will increase.")
    @JIPipeParameter("overlap-y")
    public int getOverlapY() {
        return overlapY;
    }

    @JIPipeParameter("overlap-y")
    public void setOverlapY(int overlapY) {
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
