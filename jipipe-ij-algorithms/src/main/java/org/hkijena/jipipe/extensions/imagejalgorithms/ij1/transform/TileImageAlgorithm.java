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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Tile image", description = "Splits the image into tiles of a predefined size. If the image is not perfectly tileable, it is resized.")
@JIPipeOrganization(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class TileImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TransformScale2DAlgorithm scale2DAlgorithm;
    private int tileX = 512;
    private int tileY = 512;
    private OptionalAnnotationNameParameter tileXAnnotation = new OptionalAnnotationNameParameter("Tile X", true);
    private OptionalAnnotationNameParameter tileYAnnotation = new OptionalAnnotationNameParameter("Tile Y", true);
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    public TileImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        scale2DAlgorithm = JIPipe.createNode(TransformScale2DAlgorithm.class);
        scale2DAlgorithm.setScaleMode(ScaleMode.Fit);
        registerSubParameter(scale2DAlgorithm);
    }

    public TileImageAlgorithm(TileImageAlgorithm other) {
        super(other);
        this.tileX = other.tileX;
        this.tileY = other.tileY;
        this.tileXAnnotation = new OptionalAnnotationNameParameter(other.tileXAnnotation);
        this.tileYAnnotation = new OptionalAnnotationNameParameter(other.tileYAnnotation);
        this.scale2DAlgorithm = new TransformScale2DAlgorithm(other.scale2DAlgorithm);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        registerSubParameter(scale2DAlgorithm);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        if(img.getWidth() % tileX != 0 || img.getHeight() % tileY != 0) {
            JIPipeProgressInfo scaleProgress = progressInfo.resolveAndLog("Scaling to " + tileX + " x " + tileY);
            scale2DAlgorithm.getxAxis().getContent().setExpression("CEIL(x / " + tileX + ") * " + tileX);
            scale2DAlgorithm.getyAxis().getContent().setExpression("CEIL(x / " + tileY + ") * " + tileY);
            scale2DAlgorithm.clearSlotData();
            scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(img), scaleProgress);
            scale2DAlgorithm.run(scaleProgress);
            img = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, scaleProgress).getImage();
            scale2DAlgorithm.clearSlotData();
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
                    tileStack.setProcessor(crop, index.getStackIndex(finalImg));
                }, tileProgress);

                ImagePlus tileImage = new ImagePlus("Tile " + x + ", " + y, tileStack);
                List<JIPipeAnnotation> annotations = new ArrayList<>();
                tileXAnnotation.addAnnotationIfEnabled(annotations, x + "");
                tileYAnnotation.addAnnotationIfEnabled(annotations, y + "");
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(tileImage), annotations, annotationMergeStrategy, tileProgress);
            }
        }
    }

    @JIPipeDocumentation(name = "Merge existing annotations", description = "Determines how existing annotations are merged")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Tile width").checkIfWithin(this, tileX, 0, Double.POSITIVE_INFINITY, false, false);
        report.forCategory("Tile height").checkIfWithin(this, tileY, 0, Double.POSITIVE_INFINITY, false, false);
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

    @JIPipeDocumentation(name = "Scaling", description = "The following settings determine how the image is scaled if it is not perfectly tileable.")
    @JIPipeParameter(value = "scale-algorithm", uiExcludeSubParameters = { "jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm" })
    public TransformScale2DAlgorithm getScale2DAlgorithm() {
        return scale2DAlgorithm;
    }

    @Override
    public JIPipeParameterVisibility getOverriddenUIParameterVisibility(JIPipeParameterAccess access, JIPipeParameterVisibility currentVisibility) {
        if(access.getSource() == scale2DAlgorithm && access.getKey().contains("axis"))
            return JIPipeParameterVisibility.Hidden;
        return super.getOverriddenUIParameterVisibility(access, currentVisibility);
    }
}
