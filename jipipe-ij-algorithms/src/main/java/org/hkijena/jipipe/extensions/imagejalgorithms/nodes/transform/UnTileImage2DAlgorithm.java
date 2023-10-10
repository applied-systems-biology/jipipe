package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Un-Tile image 2D", description = "Merges/Assembles multiple image tiles back into one image. Utilizes annotations to determine the location of tiles.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class UnTileImage2DAlgorithm extends JIPipeMergingAlgorithm {

    private OptionalAnnotationNameParameter tileRealXAnnotation = new OptionalAnnotationNameParameter("Original X", true);

    private OptionalAnnotationNameParameter tileRealYAnnotation = new OptionalAnnotationNameParameter("Original Y", true);

    private OptionalAnnotationNameParameter tileInsetXAnnotation = new OptionalAnnotationNameParameter("Inset X", true);

    private OptionalAnnotationNameParameter tileInsetYAnnotation = new OptionalAnnotationNameParameter("Inset Y", true);

    private OptionalAnnotationNameParameter imageWidthAnnotation = new OptionalAnnotationNameParameter("Original width", true);

    private OptionalAnnotationNameParameter imageHeightAnnotation = new OptionalAnnotationNameParameter("Original height", true);

    public UnTileImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UnTileImage2DAlgorithm(UnTileImage2DAlgorithm other) {
        super(other);
        this.tileRealXAnnotation = new OptionalAnnotationNameParameter(other.tileRealXAnnotation);
        this.tileRealYAnnotation = new OptionalAnnotationNameParameter(other.tileRealYAnnotation);
        this.tileInsetXAnnotation = new OptionalAnnotationNameParameter(other.tileInsetXAnnotation);
        this.tileInsetYAnnotation = new OptionalAnnotationNameParameter(other.tileInsetYAnnotation);
        this.imageWidthAnnotation = new OptionalAnnotationNameParameter(other.imageWidthAnnotation);
        this.imageHeightAnnotation = new OptionalAnnotationNameParameter(other.imageHeightAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        int width = 0;
        int height = 0;
        int nSlices = 0;
        int nChannels = 0;
        int nFrames = 0;
        int bitDepth = 0;
        progressInfo.log("Analyzing annotations ...");
        Map<ImagePlus, Point> imageLocations = new IdentityHashMap<>();
        for (int row : dataBatch.getInputRows(getFirstInputSlot())) {
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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(mergedImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Use original X location", description = "If enabled, use the annotation that contains the original X location. Currently mandatory.")
    @JIPipeParameter("tile-real-x-annotation")
    public OptionalAnnotationNameParameter getTileRealXAnnotation() {
        return tileRealXAnnotation;
    }

    @JIPipeParameter("tile-real-x-annotation")
    public void setTileRealXAnnotation(OptionalAnnotationNameParameter tileRealXAnnotation) {
        this.tileRealXAnnotation = tileRealXAnnotation;
    }

    @JIPipeDocumentation(name = "Use original Y location", description = "If enabled, use the annotation that contains the original Y location. Currently mandatory.")
    @JIPipeParameter("tile-real-y-annotation")
    public OptionalAnnotationNameParameter getTileRealYAnnotation() {
        return tileRealYAnnotation;
    }

    @JIPipeParameter("tile-real-y-annotation")
    public void setTileRealYAnnotation(OptionalAnnotationNameParameter tileRealYAnnotation) {
        this.tileRealYAnnotation = tileRealYAnnotation;
    }

    @JIPipeDocumentation(name = "Use original width", description = "If enabled, use the original image width annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-width")
    public OptionalAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width")
    public void setImageWidthAnnotation(OptionalAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @JIPipeDocumentation(name = "Use original height", description = "If enabled, use the original image height annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-height")
    public OptionalAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height")
    public void setImageHeightAnnotation(OptionalAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }

    @JIPipeDocumentation(name = "Use inset X", description = "If enabled, use the inset annotation. Otherwise the inset is assumed to be zero.")
    @JIPipeParameter("tile-inset-x")
    public OptionalAnnotationNameParameter getTileInsetXAnnotation() {
        return tileInsetXAnnotation;
    }

    @JIPipeParameter("tile-inset-x")
    public void setTileInsetXAnnotation(OptionalAnnotationNameParameter tileInsetXAnnotation) {
        this.tileInsetXAnnotation = tileInsetXAnnotation;
    }

    @JIPipeDocumentation(name = "Use inset Y", description = "If enabled, use the inset annotation. Otherwise the inset is assumed to be zero.")
    @JIPipeParameter("tile-inset-y")
    public OptionalAnnotationNameParameter getTileInsetYAnnotation() {
        return tileInsetYAnnotation;
    }

    @JIPipeParameter("tile-inset-y")
    public void setTileInsetYAnnotation(OptionalAnnotationNameParameter tileInsetYAnnotation) {
        this.tileInsetYAnnotation = tileInsetYAnnotation;
    }
}
