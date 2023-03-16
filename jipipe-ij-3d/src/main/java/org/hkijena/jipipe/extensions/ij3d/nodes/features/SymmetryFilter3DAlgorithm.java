package org.hkijena.jipipe.extensions.ij3d.nodes.features;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.CannyEdge3D;
import mcib3d.image3d.processing.SymmetryFilter;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "3D symmetry filter", description = "Compute the gradients of the image based on the Canny edge detector. " +
        "Then the symmetry filter will vote for the voxels inside the object based on the gradient vector direction.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Symmetry", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Symmetry smoothed", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Edges", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Bin", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "BinEdge", autoCreate = true)
@JIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Filters/3D-Edge-and-Symmetry-Filter/")
public class SymmetryFilter3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    boolean improved = true;
    private double alpha = 0.5;
    private int radius = 10;
    private double normalize = 10;
    private double scaling = 2;

    public SymmetryFilter3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SymmetryFilter3DAlgorithm(SymmetryFilter3DAlgorithm other) {
        super(other);
        this.alpha = other.alpha;
        this.radius = other.radius;
        this.normalize = other.normalize;
        this.scaling = other.scaling;
        this.improved = other.improved;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        Map<ImageSliceIndex, ImageProcessor> edgeMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> binMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> binEdgeMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> symmetryMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> symmetrySmoothedMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (ih, index, ctProgress) -> {
            CannyEdge3D edges = new CannyEdge3D(ih, alpha);
            ImageHandler[] gg = edges.getGradientsXYZ();
            ImageHandler ed = edges.getEdge();
            IJ3DUtils.putToMap(ed, index.getC(), index.getT(), edgeMap);

            SymmetryFilter sy = new SymmetryFilter(gg, radius, improved);
            // optional
            sy.setNormalize(normalize); // default 10
            sy.setScaling(scaling); // default 2
            sy.setImproved(improved); // default true

            IJ3DUtils.putToMap(sy.getIntermediates()[0], index.getC(), index.getT(), binMap);
            IJ3DUtils.putToMap(sy.getIntermediates()[1], index.getC(), index.getT(), binEdgeMap);

            ImageHandler sym = sy.getSymmetry(false);
            ImageHandler sym_ = sy.getSymmetry(true);

            IJ3DUtils.putToMap(sym, index.getC(), index.getT(), symmetryMap);
            IJ3DUtils.putToMap(sym_, index.getC(), index.getT(), symmetrySmoothedMap);

        }, progressInfo);
        ImagePlus bin = ImageJUtils.mergeMappedSlices(binMap);
        ImagePlus binEdge = ImageJUtils.mergeMappedSlices(binEdgeMap);
        ImagePlus symmetry = ImageJUtils.mergeMappedSlices(symmetryMap);
        ImagePlus symmetrySmoothed = ImageJUtils.mergeMappedSlices(symmetrySmoothedMap);
        ImagePlus edge = ImageJUtils.mergeMappedSlices(edgeMap);
        bin.copyScale(inputImage);
        binEdge.copyScale(inputImage);
        symmetry.copyScale(inputImage);
        symmetrySmoothed.copyScale(inputImage);
        edge.copyScale(inputImage);
        dataBatch.addOutputData("Edges", new ImagePlusGreyscaleData(edge), progressInfo);
        dataBatch.addOutputData("Symmetry", new ImagePlusGreyscaleData(symmetry), progressInfo);
        dataBatch.addOutputData("Symmetry smoothed", new ImagePlusGreyscaleData(symmetrySmoothed), progressInfo);
        dataBatch.addOutputData("Bin", new ImagePlusGreyscaleData(bin), progressInfo);
        dataBatch.addOutputData("BinEdge", new ImagePlusGreyscaleData(binEdge), progressInfo);
    }

    @JIPipeDocumentation(name = "Alpha", description = "The smoothing in canny edge detection, the smaller the value, the smoother the edges.")
    @JIPipeParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @JIPipeParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @JIPipeDocumentation(name = "Radius", description = "The radius of the object whose symmetry is to be detected.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @JIPipeDocumentation(name = "Normalize", description = "Internal values. See Gertych et al. for references.")
    @JIPipeParameter("normalize")
    public double getNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(double normalize) {
        this.normalize = normalize;
    }

    @JIPipeDocumentation(name = "Scaling", description = "Internal values. See Gertych et al. for references.")
    @JIPipeParameter("scaling")
    public double getScaling() {
        return scaling;
    }

    @JIPipeParameter("scaling")
    public void setScaling(double scaling) {
        this.scaling = scaling;
    }

    @JIPipeDocumentation(name = "Improved seed detection", description = "Modified implementation to better detect seeds inside objects rather than objects themselves.")
    @JIPipeParameter("improved-seed-detection")
    public boolean isImproved() {
        return improved;
    }

    @JIPipeParameter("improved-seed-detection")
    public void setImproved(boolean improved) {
        this.improved = improved;
    }
}
