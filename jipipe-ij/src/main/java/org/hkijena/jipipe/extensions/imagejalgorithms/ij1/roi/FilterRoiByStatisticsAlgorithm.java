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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.measure.MeasurementFilter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter ROI by statistics", description = "Filters the ROI list elements via statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private MeasurementFilter.List measurementFilters = new MeasurementFilter.List();
    private boolean invert = false;
    private LogicalOperation betweenMeasurementOperation = LogicalOperation.LogicalAnd;
    private LogicalOperation sameMeasurementOperation = LogicalOperation.LogicalAnd;
    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipeAlgorithm.newInstance("ij1-roi-statistics");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
        measurementFilters.addNewInstance();
    }

    /**
     * Instantiates a new node type.
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
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(measurementFilters.getNativeMeasurementEnumValue());

        // Continue with run
        super.run(subProgress, algorithmProgress, isCancelled);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData allROIs = new ROIListData();
        ResultsTableData allStatistics = new ResultsTableData();

        for (Map.Entry<ImagePlusData, ROIListData> entry : getReferenceImage(dataBatch, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled).entrySet()) {
            // Obtain statistics
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(entry.getValue());
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(entry.getKey());
            roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);
            allROIs.addAll(entry.getValue());
            allStatistics.mergeWith(statistics);
        }

        // Apply filter
        Multimap<MeasurementColumn, MeasurementFilter> filtersPerColumn = HashMultimap.create();
        for (MeasurementFilter measurementFilter : measurementFilters) {
            filtersPerColumn.put(measurementFilter.getKey(), measurementFilter);
        }

        ROIListData outputData = new ROIListData();
        for (int row = 0; row < allStatistics.getRowCount(); row++) {
            List<Boolean> betweenMeasurements = new ArrayList<>();
            for (MeasurementColumn measurementColumn : filtersPerColumn.keySet()) {
                double value = allStatistics.getTable().getValue(measurementColumn.getColumnName(), row);
                List<Boolean> withinMeasurement = new ArrayList<>();
                for (MeasurementFilter measurementFilter : filtersPerColumn.get(measurementColumn)) {
                    withinMeasurement.add(measurementFilter.getValue().test(value));
                }
                betweenMeasurements.add(sameMeasurementOperation.apply(withinMeasurement));
            }
            boolean rowResult = betweenMeasurementOperation.apply(betweenMeasurements);
            if (rowResult == !invert) {
                outputData.add(allROIs.get(row));
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeParameter("invert")
    @JIPipeDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }

    @JIPipeParameter("filter")
    @JIPipeDocumentation(name = "Filter", description = "The set of filters to apply")
    public MeasurementFilter.List getMeasurementFilters() {
        return measurementFilters;
    }

    @JIPipeParameter("filter")
    public void setMeasurementFilters(MeasurementFilter.List measurementFilters) {
        this.measurementFilters = measurementFilters;
    }

    @JIPipeParameter("same-measurement-operation")
    @JIPipeDocumentation(name = "Connect same measurements by", description = "The logical operation to apply between filters that filter the same measurement column")
    public LogicalOperation getSameMeasurementOperation() {
        return sameMeasurementOperation;
    }

    @JIPipeParameter("same-measurement-operation")
    public void setSameMeasurementOperation(LogicalOperation sameMeasurementOperation) {
        this.sameMeasurementOperation = sameMeasurementOperation;
    }

    @JIPipeParameter("between-measurement-operation")
    @JIPipeDocumentation(name = "Connect different measurements by", description = "The logical operation to apply between different measurement columns")
    public LogicalOperation getBetweenMeasurementOperation() {
        return betweenMeasurementOperation;
    }

    @JIPipeParameter("between-measurement-operation")
    public void setBetweenMeasurementOperation(LogicalOperation betweenMeasurementOperation) {
        this.betweenMeasurementOperation = betweenMeasurementOperation;
    }
}
