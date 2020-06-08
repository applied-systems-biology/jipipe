package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.colors.ColorMapEnumItemInfo;
import org.hkijena.acaq5.extensions.parameters.colors.OptionalColorMapParameter;
import org.hkijena.acaq5.extensions.parameters.editors.EnumParameterSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Color ROI by statistics (unreferenced)", description = "Sets the ROI item colors by measurements. " +
        "This algorithm does not require a  reference image and will instead use the output of the unreferenced 'ROI to Mask' algorithm as reference.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class UnreferencedColorRoiByStatisticsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private MeasurementColumn measurement = MeasurementColumn.Area;
    private UnreferencedRoiStatisticsAlgorithm roiStatisticsAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-statistics-unreferenced");
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public UnreferencedColorRoiByStatisticsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        this.mapFillColor.setEnabled(true);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public UnreferencedColorRoiByStatisticsAlgorithm(UnreferencedColorRoiByStatisticsAlgorithm other) {
        super(other);
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
        this.mapLineColor = new OptionalColorMapParameter(other.mapLineColor);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(measurement.getNativeValue());

        // Continue with run
        super.run(subProgress, algorithmProgress, isCancelled);
    }


    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataInterface.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getFirstInputSlot().addData(data);
        roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);

        // Apply color map
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        TDoubleList values = new TDoubleArrayList();
        for (int row = 0; row < data.size(); row++) {
            double value = statistics.getTable().getValue(measurement.getColumnName(), row);
            values.add(value);
            min = Math.min(value, min);
            max = Math.max(value, max);
        }
        for (int row = 0; row < data.size(); row++) {
            double value = values.get(row);
            double relative = (value - min) / (max - min);
            Roi roi = data.get(row);
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(relative));
            }
            if (mapLineColor.isEnabled()) {
                roi.setFillColor(mapLineColor.getContent().apply(relative));
            }
        }

        dataInterface.addOutputData(getFirstOutputSlot(), data);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Map fill color", description = "Allows you to map the ROI fill color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @ACAQParameter("map-fill-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapFillColor() {
        return mapFillColor;
    }

    @ACAQParameter("map-fill-color")
    public void setMapFillColor(OptionalColorMapParameter mapFillColor) {
        this.mapFillColor = mapFillColor;
    }

    @ACAQDocumentation(name = "Map line color", description = "Allows you to map the ROI line color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @ACAQParameter("map-line-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapLineColor() {
        return mapLineColor;
    }

    @ACAQParameter("map-line-color")
    public void setMapLineColor(OptionalColorMapParameter mapLineColor) {
        this.mapLineColor = mapLineColor;
    }

    @ACAQDocumentation(name = "Measurement", description = "The measurement to extract.")
    @ACAQParameter("measurement")
    public MeasurementColumn getMeasurement() {
        return measurement;
    }

    @ACAQParameter("measurement")
    public void setMeasurement(MeasurementColumn measurement) {
        this.measurement = measurement;
    }
}
