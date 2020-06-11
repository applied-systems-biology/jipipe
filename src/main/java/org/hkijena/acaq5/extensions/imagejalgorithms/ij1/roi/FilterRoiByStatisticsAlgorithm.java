package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.LogicalOperation;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementFilter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementFilterList;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Filter ROI by statistics", description = "Filters the ROI list elements via statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private MeasurementFilterList measurementFilters = new MeasurementFilterList();
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
        this.measurementFilters = new MeasurementFilterList(other.measurementFilters);
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
            List<Boolean> betweenMeasurements = new ArrayList<>();
            for (MeasurementColumn measurementColumn : filtersPerColumn.keySet()) {
                double value = statistics.getTable().getValue(measurementColumn.getColumnName(), row);
                List<Boolean> withinMeasurement = new ArrayList<>();
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
        getEventBus().post(new ParameterChangedEvent(this, "invert"));
    }

    @ACAQParameter("filter")
    @ACAQDocumentation(name = "Filter", description = "The set of filters to apply")
    public MeasurementFilterList getMeasurementFilters() {
        return measurementFilters;
    }

    @ACAQParameter("filter")
    public void setMeasurementFilters(MeasurementFilterList measurementFilters) {
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
