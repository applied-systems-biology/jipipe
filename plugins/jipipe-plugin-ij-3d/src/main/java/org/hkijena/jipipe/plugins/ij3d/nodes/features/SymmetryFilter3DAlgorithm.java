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

package org.hkijena.jipipe.plugins.ij3d.nodes.features;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.CannyEdge3D;
import mcib3d.image3d.processing.SymmetryFilter;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "3D symmetry filter", description = "Compute the gradients of the image based on the Canny edge detector. " +
        "Then the symmetry filter will vote for the voxels inside the object based on the gradient vector direction.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Symmetry", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Symmetry smoothed", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Edges", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Bin", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "BinEdge", create = true)
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Filters/3D-Edge-and-Symmetry-Filter/")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
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
        iterationStep.addOutputData("Edges", new ImagePlusGreyscaleData(edge), progressInfo);
        iterationStep.addOutputData("Symmetry", new ImagePlusGreyscaleData(symmetry), progressInfo);
        iterationStep.addOutputData("Symmetry smoothed", new ImagePlusGreyscaleData(symmetrySmoothed), progressInfo);
        iterationStep.addOutputData("Bin", new ImagePlusGreyscaleData(bin), progressInfo);
        iterationStep.addOutputData("BinEdge", new ImagePlusGreyscaleData(binEdge), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Alpha", description = "The smoothing in canny edge detection, the smaller the value, the smoother the edges.")
    @JIPipeParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @JIPipeParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "The radius of the object whose symmetry is to be detected.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Normalize", description = "Internal values. See Gertych et al. for references.")
    @JIPipeParameter("normalize")
    public double getNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(double normalize) {
        this.normalize = normalize;
    }

    @SetJIPipeDocumentation(name = "Scaling", description = "Internal values. See Gertych et al. for references.")
    @JIPipeParameter("scaling")
    public double getScaling() {
        return scaling;
    }

    @JIPipeParameter("scaling")
    public void setScaling(double scaling) {
        this.scaling = scaling;
    }

    @SetJIPipeDocumentation(name = "Improved seed detection", description = "Modified implementation to better detect seeds inside objects rather than objects themselves.")
    @JIPipeParameter("improved-seed-detection")
    public boolean isImproved() {
        return improved;
    }

    @JIPipeParameter("improved-seed-detection")
    public void setImproved(boolean improved) {
        this.improved = improved;
    }
}
