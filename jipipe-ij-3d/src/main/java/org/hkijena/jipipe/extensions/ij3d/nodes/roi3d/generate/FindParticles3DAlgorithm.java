package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.generate;

import ij.ImagePlus;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageFloat;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Find particles 3D", description = "Finds 3D particles within a mask image")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
public class FindParticles3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double minParticleSize = 0;
    private double maxParticleSize = Double.POSITIVE_INFINITY;
    private double minParticleSphericity = 0;
    private double maxParticleSphericity = 1;

    private Neighborhood3D neighborhood = Neighborhood3D.TwentySixConnected;

    private boolean measureInPhysicalUnits = true;

    private boolean excludeEdges = false;

    private boolean blackBackground = true;

    public FindParticles3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FindParticles3DAlgorithm(FindParticles3DAlgorithm other) {
        super(other);
        this.minParticleSize = other.minParticleSize;
        this.maxParticleSize = other.maxParticleSize;
        this.minParticleSphericity = other.minParticleSphericity;
        this.maxParticleSphericity = other.maxParticleSphericity;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.excludeEdges = other.excludeEdges;
        this.blackBackground = other.blackBackground;
        this.neighborhood = other.neighborhood;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus maskImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        if (!blackBackground) {
            maskImage = ImageJUtils.duplicate(maskImage);
            ImageJUtils.forEachIndexedZCTSlice(maskImage, (ip, index) -> ip.invert(), progressInfo.resolve("Invert mask"));
        }

        ROI3DListData roiList = new ROI3DListData();

        ImageJUtils.forEachIndexedCTStack(maskImage, (imp, index, stackProgress) -> {
            progressInfo.log("Detecting connected components ...");
            ImagePlus labels = ImageJAlgorithmUtils.connectedComponents3D(imp, neighborhood, 32);
            ImageFloat imageHandler = new ImageFloat(labels);

            progressInfo.log("Detecting 3D particles ...");
            Objects3DPopulation population = new Objects3DPopulation(imageHandler);

            // Filter phase
            progressInfo.log("Detected " + roiList.size() + " objects");
            List<Object3D> toRemove = new ArrayList<>();
            progressInfo.log("Applying filters ...");
            for (Object3D object3D : population.getObjectsList()) {
                if (excludeEdges && object3D.edgeImage(imageHandler, true, true)) {
                    toRemove.add(object3D);
                    continue;
                }
                if (minParticleSize > 0 || maxParticleSize < Double.POSITIVE_INFINITY) {
                    double area = measureInPhysicalUnits ? object3D.getAreaUnit() : object3D.getAreaPixels();
                    if (area < minParticleSize || area > maxParticleSize) {
                        toRemove.add(object3D);
                        continue;
                    }
                }
                if (minParticleSphericity > 0 || maxParticleSphericity != 1) {
                    double sphericity = object3D.getSphericity(measureInPhysicalUnits);
                    if (sphericity < minParticleSphericity || sphericity > maxParticleSphericity) {
                        toRemove.add(object3D);
                    }
                }
            }
            for (Object3D object3D : toRemove) {
                population.removeObject(object3D);
            }

            roiList.addFromPopulation(population, index.getC() + 1, index.getT() + 1);

        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), roiList, progressInfo);
    }

    @JIPipeDocumentation(name = "Neighborhood", description = "Determines which neighborhood is used to find connected components.")
    @JIPipeParameter("neighborhood")
    public Neighborhood3D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood3D neighborhood) {
        this.neighborhood = neighborhood;
    }

    @JIPipeParameter(value = "min-particle-size", uiOrder = -20)
    @JIPipeDocumentation(name = "Min particle size", description = "The minimum particle size in the specified unit of the input image. " +
            "If no unit is available, the unit is 'pixels'. If an object is not within the size range, it is removed from the results.")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @JIPipeParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;

    }

    @JIPipeParameter(value = "max-particle-size", uiOrder = -19)
    @JIPipeDocumentation(name = "Max particle size", description = "The maximum particle size in the specified unit of the input image. " +
            "If no unit is available, the unit is 'pixels'. If an object is not within the size range, it is removed from the results.")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @JIPipeParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;

    }

    @JIPipeParameter(value = "min-particle-sphericity", uiOrder = -18)
    @JIPipeDocumentation(name = "Min particle sphericity", description = "The minimum sphericity. " +
            "The value range is from 0-1. If an object is not within the sphericity range, it is removed from the results.")
    public double getMinParticleSphericity() {
        return minParticleSphericity;
    }

    /**
     * @param minParticleSphericity value from 0 to 1
     * @return if setting the value was successful
     */
    @JIPipeParameter("min-particle-sphericity")
    public boolean setMinParticleSphericity(double minParticleSphericity) {
        if (minParticleSphericity < 0 || minParticleSphericity > 1)
            return false;
        this.minParticleSphericity = minParticleSphericity;

        return true;
    }

    @JIPipeParameter(value = "max-particle-sphericity", uiOrder = -17)
    @JIPipeDocumentation(name = "Max particle sphericity", description = "The maximum sphericity. " +
            "The value range is from 0-1. If an object is not within the sphericity range, it is removed from the results.")
    public double getMaxParticleSphericity() {
        return maxParticleSphericity;
    }

    /**
     * @param maxParticleSphericity value from 0 to 1
     * @return if setting the value was successful
     */
    @JIPipeParameter("max-particle-sphericity")
    public boolean setMaxParticleSphericity(double maxParticleSphericity) {
        if (maxParticleSphericity < 0 || maxParticleSphericity > 1)
            return false;
        this.maxParticleSphericity = maxParticleSphericity;

        return true;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @JIPipeDocumentation(name = "Black background", description = "If enabled, the background is assumed to be black. If disabled, black pixels are extracted as ROI.")
    @JIPipeParameter("black-background")
    public boolean isBlackBackground() {
        return blackBackground;
    }

    @JIPipeParameter("black-background")
    public void setBlackBackground(boolean blackBackground) {
        this.blackBackground = blackBackground;
    }

    @JIPipeParameter("exclude-edges")
    @JIPipeDocumentation(name = "Exclude edges", description = "If enabled, objects that are connected to the image edges are removed.")
    public boolean isExcludeEdges() {
        return excludeEdges;
    }

    @JIPipeParameter("exclude-edges")
    public void setExcludeEdges(boolean excludeEdges) {
        this.excludeEdges = excludeEdges;

    }
}
