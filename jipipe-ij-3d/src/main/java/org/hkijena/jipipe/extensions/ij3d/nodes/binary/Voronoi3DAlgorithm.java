package org.hkijena.jipipe.extensions.ij3d.nodes.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.regionGrowing.Watershed3DVoronoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Voronoi 3D", description = "The Voronoi algorithm will draw lines between objects at equal distances from the boundaries of" +
        " the different objects, then compute zones around objects based on these lines. This can also be seen as the splitting of the background.\n" +
        "\n" +
        "Neighbouring objects can then be computed as objects having a line in common. ")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class Voronoi3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float maxRadius = 0;

    public Voronoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Voronoi3DAlgorithm(Voronoi3DAlgorithm other) {
        super(other);
        this.maxRadius = other.maxRadius;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Input", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        Map<ImageSliceIndex, ImageProcessor> labelMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (mask3D, index, ctProgress) -> {

            Watershed3DVoronoi watershed3DVoronoi = new Watershed3DVoronoi(mask3D, maxRadius);
            ImageHandler voronoiZones = watershed3DVoronoi.getVoronoiZones(false);

            IJ3DUtils.putToMap(voronoiZones, index.getC(), index.getT(), labelMap);

        }, progressInfo);

        ImagePlus outputLabels = ImageJUtils.mergeMappedSlices(labelMap);
        outputLabels.copyScale(inputImage);
        dataBatch.addOutputData("Labels", new ImagePlusGreyscaleData(outputLabels), progressInfo);
    }

    @JIPipeDocumentation(name = "Max radius", description = "The maximum radius (0 for no maximum radius)")
    @JIPipeParameter("max-radius")
    public float getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
    }
}
