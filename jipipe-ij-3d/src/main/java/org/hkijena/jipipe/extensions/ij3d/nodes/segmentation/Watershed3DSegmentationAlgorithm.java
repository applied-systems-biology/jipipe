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

package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.FastFilters3D;
import mcib3d.image3d.regionGrowing.Watershed3D;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Watershed 3D segmentation", description = "The 3D Watershed operation works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D filters), the other image containing signal data. " +
        "A first threshold1 is used for seeds (only seeds with value > threshold1 will be used). " +
        "A second threshold is used to cluster voxels with values > threshold2. In this implementation voxels are clustered to seeds in descending order of voxel values.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Segment")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Dams", create = true)
public class Watershed3DSegmentationAlgorithm extends JIPipeIteratingAlgorithm {

    private final SeedSegmentationSettings seedSegmentationSettings;
    private int seedsThreshold = 70; // Default is 7000?!
    private int voxelsThreshold = 0;

    public Watershed3DSegmentationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        seedSegmentationSettings = new SeedSegmentationSettings();
        registerSubParameter(seedSegmentationSettings);
    }

    public Watershed3DSegmentationAlgorithm(Watershed3DSegmentationAlgorithm other) {
        super(other);
        this.seedSegmentationSettings = new SeedSegmentationSettings(other.seedSegmentationSettings);
        this.seedsThreshold = other.seedsThreshold;
        this.voxelsThreshold = other.voxelsThreshold;
        registerSubParameter(seedSegmentationSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus seedsImage = ImageJUtils.unwrap(iterationStep.getInputData("Seeds", ImagePlusGreyscaleMaskData.class, progressInfo));

        Map<ImageSliceIndex, ImageProcessor> labelMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> damMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (spots3D, index, ctProgress) -> {

            // Generate/extract seed
            ImageHandler seed3DImage;
            if (seedsImage != null) {
                seed3DImage = ImageHandler.wrap(ImageJUtils.extractCTStack(seedsImage, index.getC(), index.getT()));
            } else {
                ctProgress.log("Detecting seeds ...");
                if (spots3D.getImageStack().getBitDepth() < 32) {
                    seed3DImage = ImageHandler.wrap(FastFilters3D.filterIntImageStack(spots3D.getImageStack(),
                            FastFilters3D.MAXLOCAL,
                            seedSegmentationSettings.getRadius(),
                            seedSegmentationSettings.getRadius(),
                            seedSegmentationSettings.getRadius(),
                            0,
                            false));
                } else {
                    seed3DImage = ImageHandler.wrap(FastFilters3D.filterFloatImageStack(spots3D.getImageStack(),
                            FastFilters3D.MAXLOCAL,
                            seedSegmentationSettings.getRadius(),
                            seedSegmentationSettings.getRadius(),
                            seedSegmentationSettings.getRadius(),
                            0,
                            false));
                }
            }

            ctProgress.log("Computing watershed ...");
            Watershed3D water = new Watershed3D(spots3D, seed3DImage, voxelsThreshold, seedsThreshold);
            water.setLabelSeeds(true);
            water.setAnim(false);

            IJ3DUtils.putToMap(water.getWatershedImage3D(), index.getC(), index.getT(), labelMap);
            IJ3DUtils.putToMap(water.getDamImage(), index.getC(), index.getT(), damMap);
        }, progressInfo);

        ImagePlus outputLabels = ImageJUtils.mergeMappedSlices(labelMap);
        outputLabels.copyScale(inputImage);
        iterationStep.addOutputData("Labels", new ImagePlusGreyscaleData(outputLabels), progressInfo);
        ImagePlus outputDams = ImageJUtils.mergeMappedSlices(damMap);
        outputDams.copyScale(inputImage);
        iterationStep.addOutputData("Dams", new ImagePlusGreyscaleData(outputDams), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Seed segmentation", description = "The following settings are utilized if no seed image is provided and " +
            "an automated algorithm is applied for the seed detection")
    @JIPipeParameter("seed-segmentation-settings")
    public SeedSegmentationSettings getSeedSegmentationSettings() {
        return seedSegmentationSettings;
    }

    @SetJIPipeDocumentation(name = "Seeds threshold", description = "Determines which seeds will be used")
    @JIPipeParameter("seeds-threshold")
    public int getSeedsThreshold() {
        return seedsThreshold;
    }

    @JIPipeParameter("seeds-threshold")
    public void setSeedsThreshold(int seedsThreshold) {
        this.seedsThreshold = seedsThreshold;
    }

    @SetJIPipeDocumentation(name = "Image threshold", description = "Clusters the voxels")
    @JIPipeParameter("image-threshold")
    public int getVoxelsThreshold() {
        return voxelsThreshold;
    }

    @JIPipeParameter("image-threshold")
    public void setVoxelsThreshold(int voxelsThreshold) {
        this.voxelsThreshold = voxelsThreshold;
    }
}
