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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.analyze;

import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.ImageStatisticsParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.tables.ResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts a mask to ROI and ROI measurements
 */
// Algorithm metadata
@ACAQDocumentation(name = "Find particles 2D", description = "Converts mask images into ROI and generates measurements. " +
        "If higher-dimensional data is provided, the results are generated for each 2D slice.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Analysis)

// Algorithm data flow
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Measurements")

// Algorithm traits
public class FindParticles2D extends ACAQSimpleIteratingAlgorithm {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.POSITIVE_INFINITY;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;
    private boolean splitSlices = true;
    private String annotationType = "Image index";
    private ImageStatisticsParameters statisticsParameters = new ImageStatisticsParameters();

    /**
     * @param declaration algorithm declaration
     */
    public FindParticles2D(ACAQAlgorithmDeclaration declaration) {
        super(declaration,
                ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Mask", ImagePlusGreyscaleMaskData.class)
                        .addOutputSlot("ROI", ROIListData.class, null)
                        .addOutputSlot("Measurements", ResultsTableData.class, null)
                        .seal()
                        .build());
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
        this.statisticsParameters = new ImageStatisticsParameters(other.statisticsParameters);
    }

    @ACAQDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns. Please refer to the " +
            "individual measurement documentations for the column names.")
    @ACAQParameter("measurements")
    public ImageStatisticsParameters getStatisticsParameters() {
        return statisticsParameters;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscaleMaskData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class);

        // Update the analyzer to extract the measurements we want
        statisticsParameters.updateAnalyzer();

        if (splitSlices) {
            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (ip, index) -> {
                RoiManager manager = new RoiManager(true);
                ResultsTable table = new ResultsTable();
                ParticleAnalyzer.setRoiManager(manager);
                ParticleAnalyzer.setResultsTable(table);
                ParticleAnalyzer analyzer = new ParticleAnalyzer(0,
                        0,
                        table,
                        minParticleSize,
                        maxParticleSize,
                        minParticleCircularity,
                        maxParticleCircularity);
                analyzer.analyze(inputData.getImage(), ip);

                // Override for "Slice"
                if (statisticsParameters.isOutputStackPosition()) {
                    for (int i = 0; i < table.getCounter(); i++) {
                        table.setValue("Slice", i, index + 1);
                    }
                }

                List<ACAQAnnotation> traits = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(annotationType)) {
                    traits.add(new ACAQAnnotation(annotationType, "slice=" + index));
                }
                ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
                for (Roi roi : rois) {
                    roi.setPosition(index + 1);
                }

                dataInterface.addOutputData("ROI", rois, traits);
                dataInterface.addOutputData("Measurements", new ResultsTableData(table), traits);
            });
        } else {
            ResultsTableData mergedResultsTable = new ResultsTableData(new ResultsTable());
            ROIListData mergedROI = new ROIListData(new ArrayList<>());

            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (ip, index) -> {
                RoiManager manager = new RoiManager(true);
                ResultsTable table = new ResultsTable();
                ParticleAnalyzer.setRoiManager(manager);
                ParticleAnalyzer.setResultsTable(table);
                ParticleAnalyzer analyzer = new ParticleAnalyzer(0,
                        0,
                        table,
                        minParticleSize,
                        maxParticleSize,
                        minParticleCircularity,
                        maxParticleCircularity);
                analyzer.analyze(inputData.getImage(), ip);

                // Override for "Slice"
                if (statisticsParameters.isOutputStackPosition()) {
                    for (int i = 0; i < table.getCounter(); i++) {
                        table.setValue("Slice", i, index + 1);
                    }
                }
                ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
                for (Roi roi : rois) {
                    roi.setPosition(index + 1);
                }

                // Merge into one result
                mergedResultsTable.mergeWith(new ResultsTableData(table));
                mergedROI.mergeWith(rois);
            });

            dataInterface.addOutputData("ROI", mergedROI);
            dataInterface.addOutputData("Measurements", mergedResultsTable);
        }


    }

    @ACAQParameter("min-particle-size")
    @ACAQDocumentation(name = "Min particle size")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @ACAQParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;

    }

    @ACAQParameter("max-particle-size")
    @ACAQDocumentation(name = "Max particle size")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @ACAQParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;

    }

    @ACAQParameter("min-particle-circularity")
    @ACAQDocumentation(name = "Min particle circularity")
    public double getMinParticleCircularity() {
        return minParticleCircularity;
    }

    /**
     * @param minParticleCircularity value from 0 to 1
     * @return if setting the value was successful
     */
    @ACAQParameter("min-particle-circularity")
    public boolean setMinParticleCircularity(double minParticleCircularity) {
        if (minParticleCircularity < 0 || minParticleCircularity > 1)
            return false;
        this.minParticleCircularity = minParticleCircularity;

        return true;
    }

    @ACAQParameter("max-particle-circularity")
    @ACAQDocumentation(name = "Max particle circularity")
    public double getMaxParticleCircularity() {
        return maxParticleCircularity;
    }

    /**
     * @param maxParticleCircularity value from 0 to 1
     * @return if setting the value was successful
     */
    @ACAQParameter("max-particle-circularity")
    public boolean setMaxParticleCircularity(double maxParticleCircularity) {
        if (maxParticleCircularity < 0 || maxParticleCircularity > 1)
            return false;
        this.maxParticleCircularity = maxParticleCircularity;

        return true;
    }

    @ACAQParameter("exclude-edges")
    @ACAQDocumentation(name = "Exclude edges")
    public boolean isExcludeEdges() {
        return excludeEdges;
    }

    @ACAQParameter("exclude-edges")
    public void setExcludeEdges(boolean excludeEdges) {
        this.excludeEdges = excludeEdges;

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQDocumentation(name = "Split slices", description = "If enabled, results are generated for each 2D slice. Otherwise all results are merged into one table and ROI.")
    @ACAQParameter("split-slices")
    public boolean isSplitSlices() {
        return splitSlices;
    }

    @ACAQParameter("split-slices")
    public void setSplitSlices(boolean splitSlices) {
        this.splitSlices = splitSlices;

    }

    @ACAQDocumentation(name = "Split slices annotation", description = "The annotation type generated by 'Split slices'. You can select no annotation type to disable this feature.")
    @ACAQParameter("annotation-type")
    public String getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;

    }
}
