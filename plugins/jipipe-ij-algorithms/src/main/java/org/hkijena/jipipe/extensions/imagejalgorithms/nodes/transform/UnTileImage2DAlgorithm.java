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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Un-Tile image 2D", description = "Merges/Assembles multiple image tiles back into one image. Utilizes annotations to determine the location of tiles.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class UnTileImage2DAlgorithm extends JIPipeMergingAlgorithm {

    private OptionalTextAnnotationNameParameter tileRealXAnnotation = new OptionalTextAnnotationNameParameter("Original X", true);

    private OptionalTextAnnotationNameParameter tileRealYAnnotation = new OptionalTextAnnotationNameParameter("Original Y", true);

    private OptionalTextAnnotationNameParameter tileInsetXAnnotation = new OptionalTextAnnotationNameParameter("Inset X", true);

    private OptionalTextAnnotationNameParameter tileInsetYAnnotation = new OptionalTextAnnotationNameParameter("Inset Y", true);

    private OptionalTextAnnotationNameParameter imageWidthAnnotation = new OptionalTextAnnotationNameParameter("Original width", true);

    private OptionalTextAnnotationNameParameter imageHeightAnnotation = new OptionalTextAnnotationNameParameter("Original height", true);

    public UnTileImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UnTileImage2DAlgorithm(UnTileImage2DAlgorithm other) {
        super(other);
        this.tileRealXAnnotation = new OptionalTextAnnotationNameParameter(other.tileRealXAnnotation);
        this.tileRealYAnnotation = new OptionalTextAnnotationNameParameter(other.tileRealYAnnotation);
        this.tileInsetXAnnotation = new OptionalTextAnnotationNameParameter(other.tileInsetXAnnotation);
        this.tileInsetYAnnotation = new OptionalTextAnnotationNameParameter(other.tileInsetYAnnotation);
        this.imageWidthAnnotation = new OptionalTextAnnotationNameParameter(other.imageWidthAnnotation);
        this.imageHeightAnnotation = new OptionalTextAnnotationNameParameter(other.imageHeightAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        int width = 0;
        int height = 0;
        int nSlices = 0;
        int nChannels = 0;
        int nFrames = 0;
        int bitDepth = 0;
        progressInfo.log("Analyzing annotations ...");
        Map<ImagePlus, Point> imageLocations = new IdentityHashMap<>();
        for (int row : iterationStep.getInputRows(getFirstInputSlot())) {
            Map<String, String> annotations = JIPipeTextAnnotation.annotationListToMap(getFirstInputSlot().getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting);
            ImagePlus tile = getFirstInputSlot().getData(row, ImagePlusData.class, progressInfo).getImage();

            // Crop inset
            int insetX = 0;
            int insetY = 0;

            if (tileInsetXAnnotation.isEnabled()) {
                insetX = NumberUtils.createInteger(annotations.get(tileInsetXAnnotation.getContent()));
            }
            if (tileInsetYAnnotation.isEnabled()) {
                insetY = NumberUtils.createInteger(annotations.get(tileInsetYAnnotation.getContent()));
            }

            if (insetX != 0 || insetY != 0) {
                tile = TransformCrop2DAlgorithm.crop(progressInfo, tile, new Rectangle(insetX, insetY, tile.getWidth() - insetX * 2, tile.getHeight() - insetY * 2));
            }

            // Calculate dimensions
            if (bitDepth != 24)
                bitDepth = Math.max(tile.getBitDepth(), bitDepth);
            nChannels = Math.max(nChannels, tile.getNChannels());
            nFrames = Math.max(nFrames, tile.getNFrames());
            nSlices = Math.max(nSlices, tile.getNSlices());

            int x;
            int y;
            if (tileRealXAnnotation.isEnabled()) {
                x = NumberUtils.createInteger(annotations.getOrDefault(tileRealXAnnotation.getContent(), "0"));
            } else {
                throw new RuntimeException("No real X location available!");
            }
            if (tileRealYAnnotation.isEnabled()) {
                y = NumberUtils.createInteger(annotations.getOrDefault(tileRealYAnnotation.getContent(), "0"));
            } else {
                throw new RuntimeException("No real Y location available!");
            }
            if (imageWidthAnnotation.isEnabled() && annotations.containsKey(imageWidthAnnotation.getContent())) {
                int num = NumberUtils.createInteger(annotations.get(imageWidthAnnotation.getContent()));
                if (width != 0 && num != width) {
                    throw new RuntimeException("Image width was already determined as " + width + ", but row " + row + " suggested width=" + num);
                }
                width = num;
            }
            if (imageHeightAnnotation.isEnabled() && annotations.containsKey(imageHeightAnnotation.getContent())) {
                int num = NumberUtils.createInteger(annotations.get(imageHeightAnnotation.getContent()));
                if (height != 0 && num != height) {
                    throw new RuntimeException("Image height was already determined as " + height + ", but row " + row + " suggested height=" + num);
                }
                height = num;
            }
            imageLocations.put(tile, new Point(x, y));
        }

        // Determine width & height if not set
        if (width == 0) {
            for (Map.Entry<ImagePlus, Point> entry : imageLocations.entrySet()) {
                width = Math.max(width, entry.getKey().getWidth() + entry.getValue().x);
            }
        }
        if (height == 0) {
            for (Map.Entry<ImagePlus, Point> entry : imageLocations.entrySet()) {
                height = Math.max(height, entry.getKey().getHeight() + entry.getValue().y);
            }
        }

        // Create initial image
        ImagePlus mergedImage = IJ.createHyperStack("Un-tiled", width, height, nChannels, nSlices, nFrames, bitDepth);
        progressInfo.log("Merged image: " + mergedImage);

        for (Map.Entry<ImagePlus, Point> entry : imageLocations.entrySet()) {
            ImagePlus tile = ImageJUtils.convertToSameTypeIfNeeded(entry.getKey(), mergedImage, true);
            JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Writing tile " + entry.getKey() + " [" + tile + "] " + " to " + entry.getValue());
            ImageJUtils.forEachIndexedZCTSlice(tile, (sourceIp, index) -> {
                ImageProcessor targetIp = ImageJUtils.getSliceZero(mergedImage, index);
                targetIp.insert(sourceIp, entry.getValue().x, entry.getValue().y);
            }, tileProgress);
            mergedImage.copyScale(tile);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(mergedImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Use original X location", description = "If enabled, use the annotation that contains the original X location. Currently mandatory.")
    @JIPipeParameter("tile-real-x-annotation")
    public OptionalTextAnnotationNameParameter getTileRealXAnnotation() {
        return tileRealXAnnotation;
    }

    @JIPipeParameter("tile-real-x-annotation")
    public void setTileRealXAnnotation(OptionalTextAnnotationNameParameter tileRealXAnnotation) {
        this.tileRealXAnnotation = tileRealXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Use original Y location", description = "If enabled, use the annotation that contains the original Y location. Currently mandatory.")
    @JIPipeParameter("tile-real-y-annotation")
    public OptionalTextAnnotationNameParameter getTileRealYAnnotation() {
        return tileRealYAnnotation;
    }

    @JIPipeParameter("tile-real-y-annotation")
    public void setTileRealYAnnotation(OptionalTextAnnotationNameParameter tileRealYAnnotation) {
        this.tileRealYAnnotation = tileRealYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Use original width", description = "If enabled, use the original image width annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-width")
    public OptionalTextAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width")
    public void setImageWidthAnnotation(OptionalTextAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Use original height", description = "If enabled, use the original image height annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-height")
    public OptionalTextAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height")
    public void setImageHeightAnnotation(OptionalTextAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }

    @SetJIPipeDocumentation(name = "Use inset X", description = "If enabled, use the inset annotation. Otherwise the inset is assumed to be zero.")
    @JIPipeParameter("tile-inset-x")
    public OptionalTextAnnotationNameParameter getTileInsetXAnnotation() {
        return tileInsetXAnnotation;
    }

    @JIPipeParameter("tile-inset-x")
    public void setTileInsetXAnnotation(OptionalTextAnnotationNameParameter tileInsetXAnnotation) {
        this.tileInsetXAnnotation = tileInsetXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Use inset Y", description = "If enabled, use the inset annotation. Otherwise the inset is assumed to be zero.")
    @JIPipeParameter("tile-inset-y")
    public OptionalTextAnnotationNameParameter getTileInsetYAnnotation() {
        return tileInsetYAnnotation;
    }

    @JIPipeParameter("tile-inset-y")
    public void setTileInsetYAnnotation(OptionalTextAnnotationNameParameter tileInsetYAnnotation) {
        this.tileInsetYAnnotation = tileInsetYAnnotation;
    }
}
