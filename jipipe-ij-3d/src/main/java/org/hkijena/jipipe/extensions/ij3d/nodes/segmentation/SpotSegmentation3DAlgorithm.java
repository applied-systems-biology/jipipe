package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.FastFilters3D;
import mcib3d.image3d.segment.*;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

public abstract class SpotSegmentation3DAlgorithm extends JIPipeIteratingAlgorithm {

    private final SeedSegmentationSettings seedSegmentationSettings;
    private final SpotSegmentationSettings spotSegmentationSettings;
    private int seedsThreshold = 15;
    private boolean output32bit = false;
    private int minVolumePixels = 1;
    private int maxVolumePixels = 1000000;
    private boolean enableWatershed = true;

    public SpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.seedSegmentationSettings = new SeedSegmentationSettings();
        this.spotSegmentationSettings = new SpotSegmentationSettings();
        registerSubParameter(seedSegmentationSettings);
        registerSubParameter(spotSegmentationSettings);
    }

    public SpotSegmentation3DAlgorithm(SpotSegmentation3DAlgorithm other) {
        super(other);
        this.seedSegmentationSettings = new SeedSegmentationSettings(other.seedSegmentationSettings);
        this.spotSegmentationSettings = new SpotSegmentationSettings(other.spotSegmentationSettings);
        registerSubParameter(seedSegmentationSettings);
        registerSubParameter(spotSegmentationSettings);

        this.minVolumePixels = other.minVolumePixels;
        this.maxVolumePixels = other.maxVolumePixels;
        this.enableWatershed = other.enableWatershed;
        this.output32bit = other.output32bit;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus spotsImage = iterationStep.getInputData("Spots", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus seedsImage = ImageJUtils.unwrap(iterationStep.getInputData("Seeds", ImagePlusGreyscaleMaskData.class, progressInfo));

        Map<ImageSliceIndex, ImageProcessor> labelMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(spotsImage, (spots3D, index, ctProgress) -> {

            // Generate/extract seed
            ImageHandler seed3DImage;
            if (seedsImage != null) {
                seed3DImage = ImageHandler.wrap(ImageJUtils.extractCTStack(seedsImage, index.getC(), index.getT()));
            } else {
                ctProgress.log("Detecting seeds ...");
                seed3DImage = ImageHandler.wrap(FastFilters3D.filterIntImageStack(spots3D.getImageStack(),
                        FastFilters3D.MAXLOCAL,
                        seedSegmentationSettings.getRadius(),
                        seedSegmentationSettings.getRadius(),
                        seedSegmentationSettings.getRadius(),
                        0,
                        false));
            }

            Segment3DSpots seg = new Segment3DSpots(spots3D, seed3DImage);
            seg.setSeedsThreshold(seedsThreshold);
            seg.setBigLabel(output32bit);
            seg.setUseWatershed(enableWatershed);
            seg.setVolumeMin(minVolumePixels);
            seg.setVolumeMax(maxVolumePixels);

            LocalThresholder localThresholder = createThresholder();
            SpotSegmenter spotSegmenter;
            switch (spotSegmentationSettings.getSegmentationMethod()) {
                case Maximum:
                    spotSegmenter = new SpotSegmenterMax();
                    break;
                case Block:
                    spotSegmenter = new SpotSegmenterBlock();
                    break;
                default:
                    spotSegmenter = new SpotSegmenterClassical();
                    break;
            }

            seg.setLocalThresholder(localThresholder);
            seg.setSpotSegmenter(spotSegmenter);
            seg.segmentAll();

            int size = seg.getNbObjects();
            ctProgress.log("Number of labelled objects: " + size);

            IJ3DUtils.putToMap(seg.getLabeledImage(), index.getC(), index.getT(), labelMap);
        }, progressInfo);

        ImagePlus outputLabels = ImageJUtils.mergeMappedSlices(labelMap);
        outputLabels.copyScale(spotsImage);
        iterationStep.addOutputData("Labels", new ImagePlusGreyscaleData(outputLabels), progressInfo);
    }

    protected abstract LocalThresholder createThresholder();

    @SetJIPipeDocumentation(name = "Output 32-bit labels", description = "If enabled, generate 32-bit labels (support more than 32767 objects)")
    @JIPipeParameter("output-32-bit")
    public boolean isOutput32bit() {
        return output32bit;
    }

    @JIPipeParameter("output-32-bit")
    public void setOutput32bit(boolean output32bit) {
        this.output32bit = output32bit;
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

    @SetJIPipeDocumentation(name = "Seed segmentation", description = "The following settings are utilized if no seed image is provided and " +
            "an automated algorithm is applied for the seed detection")
    @JIPipeParameter("seed-segmentation-settings")
    public SeedSegmentationSettings getSeedSegmentationSettings() {
        return seedSegmentationSettings;
    }

    @SetJIPipeDocumentation(name = "Spot segmentation", description = "Settings related to the segmentation of the spots")
    @JIPipeParameter("spot-segmentation-settings")
    public SpotSegmentationSettings getSpotSegmentationSettings() {
        return spotSegmentationSettings;
    }

    @SetJIPipeDocumentation(name = "Minimum volume (pixels)", description = "The minimum volume of objects (pixel units)")
    @JIPipeParameter("min-volume-pixels")
    public int getMinVolumePixels() {
        return minVolumePixels;
    }

    @JIPipeParameter("min-volume-pixels")
    public void setMinVolumePixels(int minVolumePixels) {
        this.minVolumePixels = minVolumePixels;
    }

    @SetJIPipeDocumentation(name = "Maximum volume (pixels)", description = "The maximum volume of objects (pixel units)")
    @JIPipeParameter("max-volume-pixels")
    public int getMaxVolumePixels() {
        return maxVolumePixels;
    }

    @JIPipeParameter("max-volume-pixels")
    public void setMaxVolumePixels(int maxVolumePixels) {
        this.maxVolumePixels = maxVolumePixels;
    }

    @SetJIPipeDocumentation(name = "Enable watershed", description = "If enabled, close spots will be separated")
    @JIPipeParameter("enable-watershed")
    public boolean isEnableWatershed() {
        return enableWatershed;
    }

    @JIPipeParameter("enable-watershed")
    public void setEnableWatershed(boolean enableWatershed) {
        this.enableWatershed = enableWatershed;
    }

    public enum SpotSegmentationMethod {
        Classical,
        Maximum,
        Block
    }

    public static class SpotSegmentationSettings extends AbstractJIPipeParameterCollection {
        private SpotSegmentationMethod segmentationMethod = SpotSegmentationMethod.Classical;

        public SpotSegmentationSettings() {
        }

        public SpotSegmentationSettings(SpotSegmentationSettings other) {
            this.segmentationMethod = other.segmentationMethod;
        }

        @SetJIPipeDocumentation(name = "Method", description = "When the threshold value has been determined, the plugin offers several\n" +
                "methods of voxel clustering that segment the object. The segmentation\n" +
                "proceeds by successive examination of voxels, starting from the seed.\n" +
                "\n" +
                "* Classical : All neighbouring voxels with intensity value higher than the\n" +
                "threshold value (Local Background) are clustered to form the object.\n" +
                "* Maximum : This mode operates similarly as the previous one, but with one\n" +
                "additional constraint : the voxels are clustered in the object if their\n" +
                "intensity value is higher than the threshold and lower than the intensity\n" +
                "value of the previously clustered voxels. It can prevent to cluster pixels\n" +
                "from another close spot.\n" +
                "* Block : The same Maximum procedure is applied, but instead of\n" +
                "considering single voxels, block of voxel (all neighbors with value greater\n" +
                "than the local threshold) are examined. All voxels must satisfy the\n" +
                "constraint to be added to the cluster, else no voxel are added.")
        @JIPipeParameter("segmentation-method")
        public SpotSegmentationMethod getSegmentationMethod() {
            return segmentationMethod;
        }

        @JIPipeParameter("segmentation-method")
        public void setSegmentationMethod(SpotSegmentationMethod segmentationMethod) {
            this.segmentationMethod = segmentationMethod;
        }
    }

}
