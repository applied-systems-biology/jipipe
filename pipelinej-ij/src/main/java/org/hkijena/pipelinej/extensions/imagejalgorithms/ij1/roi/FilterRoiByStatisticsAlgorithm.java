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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.roi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.parameters.util.LogicalOperation;
import org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.measure.MeasurementFilter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Filter ROI by statistics", description = "Filters the ROI list elements via statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private MeasurementFilter.List measurementFilters = new MeasurementFilter.List();
    private boolean invert = false;
    private LogicalOperation betweenMeasurementOperation = LogicalOperation.LogicalAnd;
    private LogicalOperation sameMeasurementOperation = LogicalOperation.LogicalAnd;
    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-statistics");

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public FilterRoiByStatisticsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ROIListData.class, "Output");
        measurementFilters.addNewInstance();
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public FilterRoiByStatisticsAlgorithm(FilterRoiByStatisticsAlgorithm other) {
        super(other);
        this.invert = other.invert;
        this.sameMeasurementOperation = other.sameMeasurementOperation;
        this.measurementFilters = new MeasurementFilter.List(other.measurementFilters);
        this.betweenMeasurementOperation = other.betweenMeasurementOperation;
        this.sameMeasurementOperation = other.sameMeasurementOperation;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(measurementFilters.getNativeMeasurementEnumValue());

        // Continue with run
        super.run(subProgress, algorithmProgress, isCancelled);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataInterface.getInputData("ROI", ROIListData.class);
        ImagePlusData referenceImageData = new ImagePlusData(getReferenceImage(dataInterface, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled));

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputData);
        roiStatisticsAlgorithm.getInputSlot("Reference").addData(referenceImageData);
        roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);

        // Apply filter
        Multimap<MeasurementColumn, MeasurementFilter> filtersPerColumn = HashMultimap.create();
        for (MeasurementFilter measurementFilter : measurementFilters) {
            filtersPerColumn.put(measurementFilter.getKey(), measurementFilter);
        }

        ROIListData outputData = new ROIListData();
        for (int row = 0; row < statistics.getRowCount(); row++) {
            java.util.List betweenMeasurements = new ArrayList<>();
            for (MeasurementColumn measurementColumn : filtersPerColumn.keySet()) {
                double value = statistics.getTable().getValue(measurementColumn.getColumnName(), row);
                java.util.List withinMeasurement = new ArrayList<>();
                for (MeasurementFilter measurementFilter : filtersPerColumn.get(measurementColumn)) {
                    withinMeasurement.add(measurementFilter.getValue().test(value));
                }
                betweenMeasurements.add(sameMeasurementOperation.apply(withinMeasurement));
            }
            boolean rowResult = betweenMeasurementOperation.apply(betweenMeasurements);
            if (rowResult == !invert) {
                outputData.add(inputData.get(row));
            }
        }

        dataInterface.addOutputData(getFirstOutputSlot(), outputData);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQParameter("invert")
    @ACAQDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }

    @ACAQParameter("filter")
    @ACAQDocumentation(name = "Filter", description = "The set of filters to apply")
    public MeasurementFilter.List getMeasurementFilters() {
        return measurementFilters;
    }

    @ACAQParameter("filter")
    public void setMeasurementFilters(MeasurementFilter.List measurementFilters) {
        this.measurementFilters = measurementFilters;
    }

    @ACAQParameter("same-measurement-operation")
    @ACAQDocumentation(name = "Connect same measurements by", description = "The logical operation to apply between filters that filter the same measurement column")
    public LogicalOperation getSameMeasurementOperation() {
        return sameMeasurementOperation;
    }

    @ACAQParameter("same-measurement-operation")
    public void setSameMeasurementOperation(LogicalOperation sameMeasurementOperation) {
        this.sameMeasurementOperation = sameMeasurementOperation;
    }

    @ACAQParameter("between-measurement-operation")
    @ACAQDocumentation(name = "Connect different measurements by", description = "The logical operation to apply between different measurement columns")
    public LogicalOperation getBetweenMeasurementOperation() {
        return betweenMeasurementOperation;
    }

    @ACAQParameter("between-measurement-operation")
    public void setBetweenMeasurementOperation(LogicalOperation betweenMeasurementOperation) {
        this.betweenMeasurementOperation = betweenMeasurementOperation;
    }
}
