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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a mask to ROI and ROI measurements
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Find particles 2D", description = "Converts mask images into ROI by applying a connected components algorithm and generates measurements. Please note that this algorithm will always trace the external object boundaries and convert them into polygonal ROIs. This means that holes will be closed. " +
        "If higher-dimensional data is provided, the results are generated for each 2D slice.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")

// Algorithm data flow
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements")

// Algorithm traits
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
                JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Mask", ImagePlusGreyscaleMaskData.class)
                        .addOutputSlot("ROI", ROIListData.class, null)
                        .addOutputSlot("Measurements", ResultsTableData.class, null)
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
                        table.setValue("Slice", i, index.getStackIndex(inputData.getImage()));
                        table.setValue("SliceZ", i, index.getZ());
                        table.setValue("SliceC", i, index.getC());
                        table.setValue("SliceT", i, index.getT());
                    }
                }

                List<JIPipeAnnotation> traits = new ArrayList<>();
                if (annotationType.isEnabled() && !StringUtils.isNullOrEmpty(annotationType.getContent())) {
                    traits.add(new JIPipeAnnotation(annotationType.getContent(), "" + index));
                }
                ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
                ImagePlus roiReferenceImage = new ImagePlus(inputData.getImage().getTitle(), ip.duplicate());
                for (Roi roi : rois) {
                    roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                    roi.setImage(roiReferenceImage);
                }

                dataBatch.addOutputData("ROI", rois, traits, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
                dataBatch.addOutputData("Measurements", new ResultsTableData(table), traits, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
            });
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
                        table.setValue("Slice", i, index.getStackIndex(inputData.getImage()));
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
                mergedResultsTable.mergeWith(new ResultsTableData(table));
                mergedROI.mergeWith(rois);
            });

            dataBatch.addOutputData("ROI", mergedROI, progressInfo);
            dataBatch.addOutputData("Measurements", mergedResultsTable, progressInfo);
        }
    }

    @JIPipeParameter("min-particle-size")
    @JIPipeDocumentation(name = "Min particle size")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @JIPipeParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;

    }

    @JIPipeParameter("max-particle-size")
    @JIPipeDocumentation(name = "Max particle size")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @JIPipeParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;

    }

    @JIPipeParameter("min-particle-circularity")
    @JIPipeDocumentation(name = "Min particle circularity")
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

    @JIPipeParameter("max-particle-circularity")
    @JIPipeDocumentation(name = "Max particle circularity")
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
    @JIPipeDocumentation(name = "Exclude edges")
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
