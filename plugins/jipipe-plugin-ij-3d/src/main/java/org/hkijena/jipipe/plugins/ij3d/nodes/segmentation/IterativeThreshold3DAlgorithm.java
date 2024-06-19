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

package org.hkijena.jipipe.plugins.ij3d.nodes.segmentation;

import ij.ImagePlus;
import ij.ImageStack;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Point3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.IterativeThresholding.TrackThreshold;
import mcib3d.image3d.processing.FastFilters3D;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.ArrayList;

@SetJIPipeDocumentation(name = "3D iterative thresholding", description = "Tests all thresholds and detect objects for all thresholds, it will then try to build a lineage of the objects detected, " +
        "linking them from one threshold to the next threshold, taking possible splits into account.")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/3D-Iterative-Segmentation/")
@AddJIPipeCitation("Gul-Mohammed, J., Arganda-Carreras, I., Andrey, P., Galy, V., & Boudier, T. (2014). A generic classification-based method for segmentation of nuclei in 3D images of early embryos. BMC bioinformatics, 15(1), 1-12.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Markers", optional = true, description = "Optional seeds for the objects", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class IterativeThreshold3DAlgorithm extends JIPipeIteratingAlgorithm {

    private int minVolumePixels = 100;
    private int maxVolumePixels = 1000;
    private int minThreshold = 0;
    private int minContrastExp = 0;

    private int valueMethod = 10;

    private boolean startsAtMean = false;

    private boolean enableFiltering = false;

    private CriteriaMethod criteriaMethod = CriteriaMethod.Elongation;

    private ThresholdMethod thresholdMethod = ThresholdMethod.Step;

    private SegmentResultsMethod segmentResultsMethod = SegmentResultsMethod.All;


    public IterativeThreshold3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public IterativeThreshold3DAlgorithm(IterativeThreshold3DAlgorithm other) {
        super(other);
        this.minVolumePixels = other.minVolumePixels;
        this.maxVolumePixels = other.maxVolumePixels;
        this.minThreshold = other.minThreshold;
        this.minContrastExp = other.minContrastExp;
        this.valueMethod = other.valueMethod;
        this.startsAtMean = other.startsAtMean;
        this.enableFiltering = other.enableFiltering;
        this.criteriaMethod = other.criteriaMethod;
        this.thresholdMethod = other.thresholdMethod;
        this.segmentResultsMethod = other.segmentResultsMethod;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus markersImage = ImageJUtils.unwrap(iterationStep.getInputData("Markers", ImagePlusGreyscaleMaskData.class, progressInfo));
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> {
            ArrayList<Point3D> point3Ds = null;
            if (markersImage != null) {
                ImagePlus seeds = ImageJUtils.extractCTStack(markersImage, index.getC(), index.getT());
                point3Ds = computeMarkers(ImageInt.wrap(seeds));
            }
            ImagePlus duplicate = ImageJUtils.duplicate(ih.getImagePlus());
            if (enableFiltering) {
                applyFilter(duplicate, ctProgress);
            }
            ctProgress.log("Threshold method " + thresholdMethod);
            ctProgress.log("Criteria method " + criteriaMethod);
            int thmin = minThreshold;
            // is starts at mean selected, use mean, maybe remove in new version
            if (startsAtMean) {
                thmin = (int) ImageHandler.wrap(duplicate).getMean();
                ctProgress.log("Mean=" + thmin);
            }

            TrackThreshold TT = new TrackThreshold(minVolumePixels, maxVolumePixels, minContrastExp, valueMethod, valueMethod, thmin);
            TT.setMarkers(point3Ds);

            if (duplicate.getBitDepth() == 8) {
                ctProgress.log("Threshold method was set to 'Step' (8-bit input)");
                TT.setMethodThreshold(ThresholdMethod.Step.nativeValue);
            } else {
                TT.setMethodThreshold(thresholdMethod.nativeValue);
            }
            TT.setCriteriaMethod(criteriaMethod.nativeValue);

            ImagePlus res;
            if (segmentResultsMethod == SegmentResultsMethod.All)
                res = TT.segment(duplicate, true);
            else
                res = TT.segmentBest(duplicate, true);

            return ImageHandler.wrap(res);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    private void applyFilter(ImagePlus duplicate, JIPipeProgressInfo ctProgress) {
        int radX = (int) Math.floor(Math.pow((minVolumePixels * 3.0) / (4.0 * Math.PI), 1.0 / 3.0));
        if (radX > 10) {
            radX = 10;
        }
        if (radX < 1) {
            radX = 1;
        }
        int radZ = radX; // use calibration ?
        ctProgress.log("Filtering with radius " + radX);
        ImageStack res = FastFilters3D.filterIntImageStack(duplicate.getStack(), FastFilters3D.MEDIAN, radX, radX, radZ, 0, true);
        duplicate.setStack(res);
    }

    private ArrayList<Point3D> computeMarkers(ImageInt markImage) {
        if (markImage.isBinary()) {
            ImageLabeller labeler = new ImageLabeller();
            markImage = labeler.getLabels(markImage);
        }
        ArrayList<Point3D> point3Ds = new ArrayList<>();
        Objects3DPopulation objects3DPopulation = new Objects3DPopulation(markImage);
        for (Object3D object3D : objects3DPopulation.getObjectsList()) {
            point3Ds.add(object3D.getCenterAsPoint());
        }
        return point3Ds;
    }

    @SetJIPipeDocumentation(name = "Min volume (pixels)", description = "The minimum volume of the detected objects")
    @JIPipeParameter("min-volume-pixels")
    public int getMinVolumePixels() {
        return minVolumePixels;
    }

    @JIPipeParameter("min-volume-pixels")
    public void setMinVolumePixels(int minVolumePixels) {
        this.minVolumePixels = minVolumePixels;
    }

    @SetJIPipeDocumentation(name = "Max volume (pixels)", description = "The maximum volume of the detected objects")
    @JIPipeParameter("max-volume-pixels")
    public int getMaxVolumePixels() {
        return maxVolumePixels;
    }

    @JIPipeParameter("max-volume-pixels")
    public void setMaxVolumePixels(int maxVolumePixels) {
        this.maxVolumePixels = maxVolumePixels;
    }

    @SetJIPipeDocumentation(name = "Minimum threshold", description = "The minimum threshold")
    @JIPipeParameter("min-threshold")
    public int getMinThreshold() {
        return minThreshold;
    }

    @JIPipeParameter("min-threshold")
    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;
    }

    @SetJIPipeDocumentation(name = "Minimum contrast (Exp)", description = "Allows to exclude regions from the calculation that have a low contrast")
    @JIPipeParameter("min-contrast-exp")
    public int getMinContrastExp() {
        return minContrastExp;
    }

    @JIPipeParameter("min-contrast-exp")
    public void setMinContrastExp(int minContrastExp) {
        this.minContrastExp = minContrastExp;
    }

    @SetJIPipeDocumentation(name = "Threshold method: value", description = "Value associated to the thresholding method. Please read the documentation of the 'Threshold method' parameter.")
    @JIPipeParameter("value-method")
    public int getValueMethod() {
        return valueMethod;
    }

    @JIPipeParameter("value-method")
    public void setValueMethod(int valueMethod) {
        this.valueMethod = valueMethod;
    }

    @SetJIPipeDocumentation(name = "Start at mean", description = "In order not to test low thresholds you can specify to start with the mean value of the image as the lowest threshold or specify manually the lowest threshold to start with. ")
    @JIPipeParameter("start-at-mean")
    public boolean isStartsAtMean() {
        return startsAtMean;
    }

    @JIPipeParameter("start-at-mean")
    public void setStartsAtMean(boolean startsAtMean) {
        this.startsAtMean = startsAtMean;
    }

    @SetJIPipeDocumentation(name = "Filter before thresholding", description = "Filter the image before thresholding with a 3D median filter with radii proportional to the minimal volume.")
    @JIPipeParameter("enable-filtering")
    public boolean isEnableFiltering() {
        return enableFiltering;
    }

    @JIPipeParameter("enable-filtering")
    public void setEnableFiltering(boolean enableFiltering) {
        this.enableFiltering = enableFiltering;
    }

    @SetJIPipeDocumentation(name = "Criteria method", description = "<p>criteria to pick the best threshold :</p>\n" +
            "<ul>\n" +
            "<li>Elongation : the threshold leading to the most round object is chosen (minimal elongation).</li>\n" +
            "<li>Volume : the threshold leading to the largest object.</li>\n" +
            "<li>MSER : the threshold where the volume of the object is most stable (minimal variation).</li>\n" +
            "<li>Edges : the threshold where the objects has maximal edges.</li>\n" +
            "</ul>\n")
    @JIPipeParameter("criteria-method")
    public CriteriaMethod getCriteriaMethod() {
        return criteriaMethod;
    }

    @JIPipeParameter("criteria-method")
    public void setCriteriaMethod(CriteriaMethod criteriaMethod) {
        this.criteriaMethod = criteriaMethod;
    }

    @SetJIPipeDocumentation(name = "Threshold method", description = "<p>The thresholds tested can be tuned with 3 options with the value parameter :</p>\n" +
            "<ul>\n" +
            "<li>Step : threshold are tested each step value.</li>\n" +
            "<li>Kmeans : histogram is analysed and clustered into value classes using a KMeans algorithm.</li>\n" +
            "<li>Volume : a constant number of pixels between two thresholds using value thresholds.</li>\n" +
            "</ul>\n" +
            "<p>For 8-bits images it is recommended to use the method Step with value between 1 and 5. For 16-bits images try Step with values between 5 and 100 depending on the dynamic of your data. Note than the more threshold tested the more memory used.</p>\n")
    @JIPipeParameter("threshold-method")
    public ThresholdMethod getThresholdMethod() {
        return thresholdMethod;
    }

    @JIPipeParameter("threshold-method")
    public void setThresholdMethod(ThresholdMethod thresholdMethod) {
        this.thresholdMethod = thresholdMethod;
    }

    @SetJIPipeDocumentation(name = "Segment results")
    @JIPipeParameter("segment-results-method")
    public SegmentResultsMethod getSegmentResultsMethod() {
        return segmentResultsMethod;
    }

    @JIPipeParameter("segment-results-method")
    public void setSegmentResultsMethod(SegmentResultsMethod segmentResultsMethod) {
        this.segmentResultsMethod = segmentResultsMethod;
    }

    public enum CriteriaMethod {
        Elongation(1),
        Compactness(2),
        Volume(3),
        Mser(4),
        Edges(5);

        private final int nativeValue;

        CriteriaMethod(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }

    public enum ThresholdMethod {
        Step(1),
        KMeans(2),
        Volume(3);

        private final int nativeValue;

        ThresholdMethod(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }

    public enum SegmentResultsMethod {
        All,
        Best
    }
}
