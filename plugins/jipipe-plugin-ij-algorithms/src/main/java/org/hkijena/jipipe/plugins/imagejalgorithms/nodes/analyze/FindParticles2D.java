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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a mask to ROI and ROI measurements
 */
// Algorithm metadata
@SetJIPipeDocumentation(name = "Find particles 2D", description = "Converts mask images into ROI by applying a connected components algorithm and generates measurements. " +
        "Please note that this algorithm will always trace the external object boundaries and convert them into polygonal ROIs. This means that holes will be closed. " +
        "This node requires a thresholded image as input and will extract measurements from the thresholded image. " +
        "If you already have ROI available and want measurements, use 'Extract ROI statistics'." +
        "If higher-dimensional data is provided, the results are generated for each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")

@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", description = "The mask where particles are extracted from. White pixels are foreground.", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI", description = "The extracted ROI", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true, description = "The measurements of the ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Analyze Particles...")
public class FindParticles2D extends JIPipeSimpleIteratingAlgorithm {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.POSITIVE_INFINITY;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;
    private boolean includeHoles = true;

    private boolean compositeROI = false;
    private boolean splitSlices = false;
    private boolean blackBackground = true;
    private OptionalStringParameter annotationType = new OptionalStringParameter();
    private ImageStatisticsSetParameter statisticsParameters = new ImageStatisticsSetParameter();
    private Neighborhood2D neighborhood = Neighborhood2D.EightConnected;

    private boolean measureInPhysicalUnits = true;

    /**
     * @param info algorithm info
     */
    public FindParticles2D(JIPipeNodeInfo info) {
        super(info);
        this.annotationType.setContent("Image index");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FindParticles2D(FindParticles2D other) {
        super(other);
        this.minParticleSize = other.minParticleSize;
        this.maxParticleSize = other.maxParticleSize;
        this.minParticleCircularity = other.minParticleCircularity;
        this.maxParticleCircularity = other.maxParticleCircularity;
        this.excludeEdges = other.excludeEdges;
        this.splitSlices = other.splitSlices;
        this.annotationType = other.annotationType;
        this.blackBackground = other.blackBackground;
        this.includeHoles = other.includeHoles;
        this.neighborhood = other.neighborhood;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.compositeROI = other.compositeROI;
        this.statisticsParameters = new ImageStatisticsSetParameter(other.statisticsParameters);
    }

    @SetJIPipeDocumentation(name = "Composite ROI", description = "If enabled, generate composite ROI that can contain holes. Please note " +
            "that not all operations can appropriately handle composite ROI.")
    @JIPipeParameter("composite-roi")
    public boolean isCompositeROI() {
        return compositeROI;
    }

    @JIPipeParameter("composite-roi")
    public void setCompositeROI(boolean compositeROI) {
        this.compositeROI = compositeROI;
    }

    @SetJIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns. <br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getStatisticsParameters() {
        return statisticsParameters;
    }

    @JIPipeParameter("measurements")
    public void setStatisticsParameters(ImageStatisticsSetParameter statisticsParameters) {
        this.statisticsParameters = statisticsParameters;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscaleMaskData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);

        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            // Update the analyzer to extract the measurements we want
            statisticsParameters.updateAnalyzer();

            // Otherwise we might get issues
            Prefs.blackBackground = this.blackBackground;

            int options = 0;
            if (includeHoles) {
                options |= ParticleAnalyzer.INCLUDE_HOLES;
            }
            if (excludeEdges) {
                options |= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
            }
            if (neighborhood == Neighborhood2D.FourConnected) {
                options |= ParticleAnalyzer.FOUR_CONNECTED;
            }
            if (compositeROI) {
                options |= ParticleAnalyzer.COMPOSITE_ROIS;
            }

            if (splitSlices) {
                int finalOptions = options;
                ImageJIterationUtils.forEachIndexedZCTSlice(inputData.getImage(), (ip, index) -> {
                    RoiManager manager = new RoiManager(true);
                    ResultsTable table = new ResultsTable();
                    ParticleAnalyzer.setRoiManager(manager);
                    ParticleAnalyzer.setResultsTable(table);
                    ParticleAnalyzer analyzer = new ParticleAnalyzer(finalOptions,
                            0,
                            table,
                            minParticleSize,
                            maxParticleSize,
                            minParticleCircularity,
                            maxParticleCircularity);
                    ImagePlus sliceImage = new ImagePlus(inputData.getImage().getTitle() + "_" + index, ip);
                    // Copy calibration
                    {
                        sliceImage.copyScale(inputData.getImage());
                    }
                    analyzer.analyze(sliceImage, ip);

                    // Override for "Slice"
                    if (statisticsParameters.getValues().contains(Measurement.StackPosition)) {
                        for (int i = 0; i < table.getCounter(); i++) {
                            table.setValue("Slice", i, index.zeroSliceIndexToOneStackIndex(inputData.getImage()));
                            table.setValue("SliceZ", i, index.getZ());
                            table.setValue("SliceC", i, index.getC());
                            table.setValue("SliceT", i, index.getT());
                        }
                    }

                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    if (annotationType.isEnabled() && !StringUtils.isNullOrEmpty(annotationType.getContent())) {
                        annotations.add(new JIPipeTextAnnotation(annotationType.getContent(), "" + index));
                    }
                    ROI2DListData rois = new ROI2DListData(Arrays.asList(manager.getRoisAsArray()));
                    ImagePlus roiReferenceImage = new ImagePlus(inputData.getImage().getTitle(), ip.duplicate());
                    for (Roi roi : rois) {
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                        roi.setImage(roiReferenceImage);
                    }

                    // Use JIPipe-enhanced measurements
                    ResultsTableData measurements = rois.measure(roiReferenceImage, statisticsParameters, true, measureInPhysicalUnits);

                    iterationStep.addOutputData("ROI", rois, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                    iterationStep.addOutputData("Measurements", measurements, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }, progressInfo);
            } else {
                ResultsTableData mergedResultsTable = new ResultsTableData(new ResultsTable());
                ROI2DListData mergedROI = new ROI2DListData(new ArrayList<>());
                ImagePlus roiReferenceImage = inputData.getDuplicateImage();

                int finalOptions = options;
                ImageJIterationUtils.forEachIndexedZCTSlice(inputData.getImage(), (ip, index) -> {
                    RoiManager manager = new RoiManager(true);
                    ResultsTable table = new ResultsTable();
                    ParticleAnalyzer.setRoiManager(manager);
                    ParticleAnalyzer.setResultsTable(table);
                    ParticleAnalyzer analyzer = new ParticleAnalyzer(finalOptions,
                            0,
                            table,
                            minParticleSize,
                            maxParticleSize,
                            minParticleCircularity,
                            maxParticleCircularity);
                    ImagePlus sliceImage = new ImagePlus(inputData.getImage().getTitle() + "_" + index, ip);
                    if (measureInPhysicalUnits) {
                        sliceImage.copyScale(inputData.getImage());
                    }
                    analyzer.analyze(sliceImage, ip);

                    // Override for "Slice"
                    if (statisticsParameters.getValues().contains(Measurement.StackPosition)) {
                        for (int i = 0; i < table.getCounter(); i++) {
                            table.setValue("Slice", i, index.zeroSliceIndexToOneStackIndex(inputData.getImage()));
                            table.setValue("SliceZ", i, index.getZ());
                            table.setValue("SliceC", i, index.getC());
                            table.setValue("SliceT", i, index.getT());
                        }
                    }
                    ROI2DListData rois = new ROI2DListData(Arrays.asList(manager.getRoisAsArray()));
                    for (Roi roi : rois) {
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                        roi.setImage(roiReferenceImage);
                    }

                    // Use JIPipe-enhanced measurements
                    ResultsTableData measurements = rois.measure(roiReferenceImage, statisticsParameters, true, measureInPhysicalUnits);

                    // Merge into one result
                    mergedResultsTable.addRows(measurements);
                    mergedROI.mergeWith(rois);
                }, progressInfo);

                iterationStep.addOutputData("ROI", mergedROI, progressInfo);
                iterationStep.addOutputData("Measurements", mergedResultsTable, progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @JIPipeParameter(value = "min-particle-size", uiOrder = -20)
    @SetJIPipeDocumentation(name = "Min particle size", description = "The minimum particle size in the specified unit of the input image. " +
            "If no unit is available, the unit is 'pixels'. If an object is not within the size range, it is removed from the results.")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @JIPipeParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;

    }

    @JIPipeParameter(value = "max-particle-size", uiOrder = -19)
    @SetJIPipeDocumentation(name = "Max particle size", description = "The maximum particle size in the specified unit of the input image. " +
            "If no unit is available, the unit is 'pixels'. If an object is not within the size range, it is removed from the results.")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @JIPipeParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;

    }

    @JIPipeParameter(value = "min-particle-circularity", uiOrder = -18)
    @SetJIPipeDocumentation(name = "Min particle circularity", description = "The minimum circularity (circularity = 4pi(area/perimeter^2)). " +
            "The value range is from 0-1. If an object is not within the circularity range, it is removed from the results.")
    public double getMinParticleCircularity() {
        return minParticleCircularity;
    }

    /**
     * @param minParticleCircularity value from 0 to 1
     * @return if setting the value was successful
     */
    @JIPipeParameter("min-particle-circularity")
    public boolean setMinParticleCircularity(double minParticleCircularity) {
        if (minParticleCircularity < 0 || minParticleCircularity > 1)
            return false;
        this.minParticleCircularity = minParticleCircularity;

        return true;
    }

    @JIPipeParameter(value = "max-particle-circularity", uiOrder = -17)
    @SetJIPipeDocumentation(name = "Max particle circularity", description = "The maximum circularity (circularity = 4pi(area/perimeter^2)). " +
            "The value range is from 0-1. If an object is not within the circularity range, it is removed from the results.")
    public double getMaxParticleCircularity() {
        return maxParticleCircularity;
    }

    /**
     * @param maxParticleCircularity value from 0 to 1
     * @return if setting the value was successful
     */
    @JIPipeParameter("max-particle-circularity")
    public boolean setMaxParticleCircularity(double maxParticleCircularity) {
        if (maxParticleCircularity < 0 || maxParticleCircularity > 1)
            return false;
        this.maxParticleCircularity = maxParticleCircularity;

        return true;
    }

    @JIPipeParameter("exclude-edges")
    @SetJIPipeDocumentation(name = "Exclude edges", description = "If enabled, objects that are connected to the image edges are removed.")
    public boolean isExcludeEdges() {
        return excludeEdges;
    }

    @JIPipeParameter("exclude-edges")
    public void setExcludeEdges(boolean excludeEdges) {
        this.excludeEdges = excludeEdges;

    }

    @SetJIPipeDocumentation(name = "Split slices", description = "If enabled, results are generated for each 2D slice. Otherwise all results are merged into one table and ROI.")
    @JIPipeParameter("split-slices")
    public boolean isSplitSlices() {
        return splitSlices;
    }

    @JIPipeParameter("split-slices")
    public void setSplitSlices(boolean splitSlices) {
        this.splitSlices = splitSlices;

    }

    @SetJIPipeDocumentation(name = "Split slices annotation", description = "The annotation type generated by 'Split slices'. You can select no annotation type to disable this feature.")
    @JIPipeParameter("annotation-type")
    public OptionalStringParameter getAnnotationType() {
        return annotationType;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationType(OptionalStringParameter annotationType) {
        this.annotationType = annotationType;

    }

    @SetJIPipeDocumentation(name = "Black background", description = "If enabled, the background is assumed to be black. If disabled, black pixels are extracted as ROI.")
    @JIPipeParameter("black-background")
    public boolean isBlackBackground() {
        return blackBackground;
    }

    @JIPipeParameter("black-background")
    public void setBlackBackground(boolean blackBackground) {
        this.blackBackground = blackBackground;
    }

    @SetJIPipeDocumentation(name = "Fill interior holes (flood-fill)", description = "If enabled, interior holes are filled.")
    @JIPipeParameter("include-holes")
    public boolean isIncludeHoles() {
        return includeHoles;
    }

    @JIPipeParameter("include-holes")
    public void setIncludeHoles(boolean includeHoles) {
        this.includeHoles = includeHoles;
    }

    @SetJIPipeDocumentation(name = "Neighborhood", description = "Determines which neighborhood is used to find connected components.")
    @JIPipeParameter("neighborhood")
    public Neighborhood2D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood2D neighborhood) {
        this.neighborhood = neighborhood;
    }
}
