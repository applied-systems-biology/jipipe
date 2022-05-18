package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.awt.*;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Un-Tile image", description = "Merges/Assembles multiple image tiles back into one image. Utilizes annotations to determine the location of tiles.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class UnTileImageAlgorithm extends JIPipeMergingAlgorithm {

    private OptionalAnnotationNameParameter tileRealXAnnotation = new OptionalAnnotationNameParameter("Original X", true);

    private OptionalAnnotationNameParameter tileRealYAnnotation = new OptionalAnnotationNameParameter("Original Y", true);

    private OptionalAnnotationNameParameter imageWidthAnnotation = new OptionalAnnotationNameParameter("Original width", true);

    private OptionalAnnotationNameParameter imageHeightAnnotation = new OptionalAnnotationNameParameter("Original height", true);
    
    public UnTileImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UnTileImageAlgorithm(UnTileImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        int width = 0;
        int height = 0;
        progressInfo.log("Analyzing annotations ..."); // TODO: Data type?
        Map<ImagePlus, Point> imageLocations = new IdentityHashMap<>();
        for (int row : dataBatch.getInputRows(getFirstInputSlot())) {
            ImagePlus tile = getFirstInputSlot().getData(row, ImagePlusData.class, progressInfo).getImage();
            Map<String, String> annotations = JIPipeTextAnnotation.annotationListToMap(getFirstInputSlot().getTextAnnotations(row), JIPipeTextAnnotationMergeMode.OverwriteExisting);
            int x;
            int y;
            if(tileRealXAnnotation.isEnabled()) {
                x = NumberUtils.createInteger(annotations.get(tileRealXAnnotation.getContent()));
            }
            else {
                throw new RuntimeException("No real X location available!");
            }
            if(tileRealYAnnotation.isEnabled()) {
                y = NumberUtils.createInteger(annotations.get(tileRealYAnnotation.getContent()));
            }
            else {
                throw new RuntimeException("No real Y location available!");
            }
            if(imageWidthAnnotation.isEnabled() && annotations.containsKey(imageWidthAnnotation.getContent())) {
                int num = NumberUtils.createInteger(annotations.get(imageWidthAnnotation.getContent()));
                if(width != 0 && num != width) {
                    throw new RuntimeException("Image width was already determined as " + width + ", but row " + row + " suggested width=" + num);
                }
                width = num;
            }
            if(imageHeightAnnotation.isEnabled() && annotations.containsKey(imageHeightAnnotation.getContent())) {
                int num = NumberUtils.createInteger(annotations.get(imageHeightAnnotation.getContent()));
                if(height != 0 && num != height) {
                    throw new RuntimeException("Image height was already determined as " + height + ", but row " + row + " suggested height=" + num);
                }
                height = num;
            }
            imageLocations.put(tile, new Point(x,y));
        }

        // Determine width & height if not set
        if(width == 0) {
            for (Map.Entry<ImagePlus, Point> entry : imageLocations.entrySet()) {
                width = Math.max(width, entry.getKey().getWidth() + entry.getValue().x);
            }
        }
        if(height == 0) {
            for (Map.Entry<ImagePlus, Point> entry : imageLocations.entrySet()) {
                height = Math.max(height, entry.getKey().getHeight() + entry.getValue().y);
            }
        }

        // TODO: Data type! Create target image!
    }

    @JIPipeDocumentation(name = "Use original X location", description = "If true, use the annotation that contains the original X location. Currently mandatory.")
    @JIPipeParameter("tile-real-x-annotation")
    public OptionalAnnotationNameParameter getTileRealXAnnotation() {
        return tileRealXAnnotation;
    }

    @JIPipeParameter("tile-real-x-annotation")
    public void setTileRealXAnnotation(OptionalAnnotationNameParameter tileRealXAnnotation) {
        this.tileRealXAnnotation = tileRealXAnnotation;
    }

    @JIPipeDocumentation(name = "Use original Y location", description = "If true, use the annotation that contains the original Y location. Currently mandatory.")
    @JIPipeParameter("tile-real-y-annotation")
    public OptionalAnnotationNameParameter getTileRealYAnnotation() {
        return tileRealYAnnotation;
    }

    @JIPipeParameter("tile-real-y-annotation")
    public void setTileRealYAnnotation(OptionalAnnotationNameParameter tileRealYAnnotation) {
        this.tileRealYAnnotation = tileRealYAnnotation;
    }

    @JIPipeDocumentation(name = "Use original width", description = "If true, use the original image width annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-width")
    public OptionalAnnotationNameParameter getImageWidthAnnotation() {
        return imageWidthAnnotation;
    }

    @JIPipeParameter("tile-original-width")
    public void setImageWidthAnnotation(OptionalAnnotationNameParameter imageWidthAnnotation) {
        this.imageWidthAnnotation = imageWidthAnnotation;
    }

    @JIPipeDocumentation(name = "Use original height", description = "If true, use the original image height annotation. Otherwise, the output image size is calculated from the tiles.")
    @JIPipeParameter("tile-original-height")
    public OptionalAnnotationNameParameter getImageHeightAnnotation() {
        return imageHeightAnnotation;
    }

    @JIPipeParameter("tile-original-height")
    public void setImageHeightAnnotation(OptionalAnnotationNameParameter imageHeightAnnotation) {
        this.imageHeightAnnotation = imageHeightAnnotation;
    }
}
