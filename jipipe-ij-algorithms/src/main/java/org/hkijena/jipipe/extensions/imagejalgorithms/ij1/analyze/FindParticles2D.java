/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a mask to ROI and ROI measurements
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Find particles 2D", description = "Converts mask images into ROI by applying a connected components algorithm and generates measurements. " +
        "Please note that this algorithm will always trace the external object boundaries and convert them into polygonal ROIs. This means that holes will be closed. " +
        "This node requires a thresholded image as input and will extract measurements from the thresholded image. " +
        "If you already have ROI available and want measurements, use 'Extract ROI statistics'." +
        "If higher-dimensional data is provided, the results are generated for each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")

@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Analyze Particles...")
public class FindParticles2D extends JIPipeSimpleIteratingAlgorithm {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.POSITIVE_INFINITY;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;
    private boolean includeHoles = true;
    private boolean splitSlices = false;
    private boolean blackBackground = true;
    private OptionalStringParameter annotationType = new OptionalStringParameter();
    private ImageStatisticsSetParameter statisticsParameters = new ImageStatisticsSetParameter();
    private Neighborhood2D neighborhood = Neighborhood2D.EightConnected;

    /**
     * @param info algorithm info
     */
    public FindParticles2D(JIPipeNodeInfo info) {
        super(info,
                JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Mask", "The mask where particles are extracted from. White pixels are foreground.", ImagePlusGreyscaleMaskData.class)
                        .addOutputSlot("ROI", "The extracted ROI", ROIListData.class, null)
                        .addOutputSlot("Measurements", "The measurements of the ROI", ResultsTableData.class, null)
                        .seal()
                        .build());
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
        this.statisticsParameters = new ImageStatisticsSetParameter(other.statisticsParameters);
    }

    @JIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns. <br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getStatisticsParameters() {
        return statisticsParameters;
    }

    @JIPipeParameter("measurements")
    public void setStatisticsParameters(ImageStatisticsSetParameter statisticsParameters) {
        this.statisticsParameters = statisticsParameters;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscaleMaskData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);

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

            if (splitSlices) {
                int finalOptions = options;
                ImageJUtils.forEachIndexedZCTSlice(inputData.getImage(), (ip, index) -> {
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
                    ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
                    ImagePlus roiReferenceImage = new ImagePlus(inputData.getImage().getTitle(), ip.duplicate());
                    for (Roi roi : rois) {
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                        roi.setImage(roiReferenceImage);
                    }

                    dataBatch.addOutputData("ROI", rois, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                    dataBatch.addOutputData("Measurements", new ResultsTableData(table), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }, progressInfo);
            } else {
                ResultsTableData mergedResultsTable = new ResultsTableData(new ResultsTable());
                ROIListData mergedROI = new ROIListData(new ArrayList<>());
                ImagePlus roiReferenceImage = inputData.getDuplicateImage();

                int finalOptions = options;
                ImageJUtils.forEachIndexedZCTSlice(inputData.getImage(), (ip, index) -> {
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
                    ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
                    for (Roi roi : rois) {
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                        roi.setImage(roiReferenceImage);
                    }

                    // Merge into one result
                    mergedResultsTable.addRows(new ResultsTableData(table));
                    mergedROI.mergeWith(rois);
                }, progressInfo);

                dataBatch.addOutputData("ROI", mergedROI, progressInfo);
                dataBatch.addOutputData("Measurements", mergedResultsTable, progressInfo);
            }
        }
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

    @JIPipeParameter(value = "min-particle-circularity", uiOrder = -18)
    @JIPipeDocumentation(name = "Min particle circularity", description = "The minimum circularity (circularity = 4pi(area/perimeter^2)). " +
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
    @JIPipeDocumentation(name = "Max particle circularity", description = "The maximum circularity (circularity = 4pi(area/perimeter^2)). " +
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
    @JIPipeDocumentation(name = "Exclude edges", description = "If enabled, objects that are connected to the image edges are removed.")
    public boolean isExcludeEdges() {
        return excludeEdges;
    }

    @JIPipeParameter("exclude-edges")
    public void setExcludeEdges(boolean excludeEdges) {
        this.excludeEdges = excludeEdges;

    }

    @JIPipeDocumentation(name = "Split slices", description = "If enabled, results are generated for each 2D slice. Otherwise all results are merged into one table and ROI.")
    @JIPipeParameter("split-slices")
    public boolean isSplitSlices() {
        return splitSlices;
    }

    @JIPipeParameter("split-slices")
    public void setSplitSlices(boolean splitSlices) {
        this.splitSlices = splitSlices;

    }

    @JIPipeDocumentation(name = "Split slices annotation", description = "The annotation type generated by 'Split slices'. You can select no annotation type to disable this feature.")
    @JIPipeParameter("annotation-type")
    public OptionalStringParameter getAnnotationType() {
        return annotationType;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationType(OptionalStringParameter annotationType) {
        this.annotationType = annotationType;

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

    @JIPipeDocumentation(name = "Include holes", description = "If enabled, holes are not filled.")
    @JIPipeParameter("include-holes")
    public boolean isIncludeHoles() {
        return includeHoles;
    }

    @JIPipeParameter("include-holes")
    public void setIncludeHoles(boolean includeHoles) {
        this.includeHoles = includeHoles;
    }

    @JIPipeDocumentation(name = "Neighborhood", description = "Determines which neighborhood is used to find connected components.")
    @JIPipeParameter("neighborhood")
    public Neighborhood2D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood2D neighborhood) {
        this.neighborhood = neighborhood;
    }
}
