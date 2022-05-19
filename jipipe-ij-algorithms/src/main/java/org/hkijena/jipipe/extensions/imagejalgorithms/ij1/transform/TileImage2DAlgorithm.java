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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Tile image", description = "Splits the image into tiles of a predefined size. If the image is not perfectly tileable, it is resized.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class TileImage2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private CanvasEqualizer canvasEqualizer;
    private int tileX = 512;
    private int tileY = 512;
    private OptionalAnnotationNameParameter tileXAnnotation = new OptionalAnnotationNameParameter("Tile X", true);
    private OptionalAnnotationNameParameter tileYAnnotation = new OptionalAnnotationNameParameter("Tile Y", true);
    private OptionalAnnotationNameParameter numTilesX = new OptionalAnnotationNameParameter("Num Tiles X", true);
    private OptionalAnnotationNameParameter numTilesY = new OptionalAnnotationNameParameter("Num Tiles Y", true);

    private OptionalAnnotationNameParameter tileRealXAnnotation = new OptionalAnnotationNameParameter("Original X", true);

    private OptionalAnnotationNameParameter tileRealYAnnotation = new OptionalAnnotationNameParameter("Original Y", true);

    private OptionalAnnotationNameParameter imageWidthAnnotation = new OptionalAnnotationNameParameter("Original width", true);

    private OptionalAnnotationNameParameter imageHeightAnnotation = new OptionalAnnotationNameParameter("Original height", true);

    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public TileImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.canvasEqualizer = new CanvasEqualizer();
        canvasEqualizer.setxAxis(new DefaultExpressionParameter(""));
        canvasEqualizer.setyAxis(new DefaultExpressionParameter(""));
        canvasEqualizer.setAnchor(Anchor.TopLeft);
        registerSubParameter(canvasEqualizer);
    }

    public TileImage2DAlgorithm(TileImage2DAlgorithm other) {
        super(other);
        this.tileX = other.tileX;
        this.tileY = other.tileY;
        this.tileXAnnotation = new OptionalAnnotationNameParameter(other.tileXAnnotation);
        this.tileYAnnotation = new OptionalAnnotationNameParameter(other.tileYAnnotation);
        this.canvasEqualizer = new CanvasEqualizer(other.canvasEqualizer);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.tileRealXAnnotation = new OptionalAnnotationNameParameter(other.tileRealXAnnotation);
        this.tileRealYAnnotation = new OptionalAnnotationNameParameter(other.tileRealYAnnotation);
        this.imageWidthAnnotation = new OptionalAnnotationNameParameter(other.imageWidthAnnotation);
        this.imageHeightAnnotation = new OptionalAnnotationNameParameter(other.imageHeightAnnotation);
        registerSubParameter(canvasEqualizer);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus originalImg = img;
        if (img.getWidth() % tileX != 0 || img.getHeight() % tileY != 0) {
            ExpressionVariables variables = new ExpressionVariables();
            JIPipeProgressInfo scaleProgress = progressInfo.resolveAndLog("Expanding canvas to fit tiles of " + tileX + " x " + tileY);
            CanvasEqualizer equalizer = new CanvasEqualizer(canvasEqualizer);
            if(equalizer.getxAxis().isEmpty())
                equalizer.getxAxis().setExpression("CEIL(width / " + tileX + ") * " + tileX);
            if(equalizer.getyAxis().isEmpty())
                equalizer.getyAxis().setExpression("CEIL(height / " + tileY + ") * " + tileY);
            img = equalizer.equalize(Collections.singletonList(img), variables).get(0);
        }
        final int nTilesX = img.getWidth() / tileX;
        final int nTilesY = img.getHeight() / tileY;
        for (int y = 0; y < nTilesY; y++) {
            for (int x = 0; x < nTilesX; x++) {
                Rectangle roi = new Rectangle(x * tileX, y * tileY, tileX, tileY);
                JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Tile", x + y * nTilesX, nTilesX * nTilesY);
                ImageStack tileStack = new ImageStack(tileX, tileY, img.getStackSize());
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
                if(canvasEqualizer.getAnchor() == Anchor.TopLeft) {
                    tileRealXAnnotation.addAnnotationIfEnabled(annotations, x * tileX + "");
                    tileRealYAnnotation.addAnnotationIfEnabled(annotations, y * tileY + "");
                }
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
        report.resolve("Tile width").checkIfWithin(this, tileX, 0, Double.POSITIVE_INFINITY, false, false);
        report.resolve("Tile height").checkIfWithin(this, tileY, 0, Double.POSITIVE_INFINITY, false, false);
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
    public int getTileX() {
        return tileX;
    }

    @JIPipeParameter("tile-x")
    public void setTileX(int tileX) {
        this.tileX = tileX;
    }

    @JIPipeDocumentation(name = "Tile height", description = "The height of a tile")
    @JIPipeParameter("tile-y")
    public int getTileY() {
        return tileY;
    }

    @JIPipeParameter("tile-y")
    public void setTileY(int tileY) {
        this.tileY = tileY;
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
    @JIPipeParameter("tile-original-width")
    public OptionalAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width")
    public void setImageWidthAnnotation(OptionalAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with original height", description = "If true, annotate the tile with the height of the original image")
    @JIPipeParameter("tile-original-height")
    public OptionalAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height")
    public void setImageHeightAnnotation(OptionalAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }

    @JIPipeDocumentation(name = "Canvas expansion", description = "The following parameters allow you to set how the canvas is expanded if the tiles do not fit.")
    @JIPipeParameter(value = "canvas-equalizer")
    public CanvasEqualizer getCanvasEqualizer() {
        return canvasEqualizer;
    }

//    @Override
//    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
//        if ("x-axis".equals(access.getKey()) && access.getSource() == getScale2DAlgorithm()) {
//            return false;
//        }
//        if ("y-axis".equals(access.getKey()) && access.getSource() == getScale2DAlgorithm()) {
//            return false;
//        }
//        return super.isParameterUIVisible(tree, access);
//    }
}
