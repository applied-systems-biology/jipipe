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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.parameters.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Color ROI by statistics", description = "Sets the ROI item colors by measurements." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ColorRoiByStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private MeasurementColumn fillMeasurement = MeasurementColumn.Area;
    private MeasurementColumn lineMeasurement = MeasurementColumn.Area;
    private RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode("ij1-roi-statistics", RoiStatisticsAlgorithm.class);
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ColorRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
        this.mapFillColor.setEnabled(true);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ColorRoiByStatisticsAlgorithm(ColorRoiByStatisticsAlgorithm other) {
        super(other);
        this.fillMeasurement = other.fillMeasurement;
        this.lineMeasurement = other.lineMeasurement;
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
        this.mapLineColor = new OptionalColorMapParameter(other.mapLineColor);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(fillMeasurement.getNativeValue() | lineMeasurement.getNativeValue());

        // Continue with run
        super.run(subProgress, algorithmProgress, isCancelled);
    }


    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData outputData = new ROIListData();

        roiStatisticsAlgorithm.setOverrideReferenceImage(true);

        for (Map.Entry<ImagePlusData, ROIListData> entry : getReferenceImage(dataBatch, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled).entrySet()) {
            ROIListData data = (ROIListData) entry.getValue().duplicate();

            // Obtain statistics
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(data);
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(entry.getKey());
            roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);

            // Apply color map
            double fillMin = Double.POSITIVE_INFINITY;
            double fillMax = Double.NEGATIVE_INFINITY;
            double lineMin = Double.POSITIVE_INFINITY;
            double lineMax = Double.NEGATIVE_INFINITY;
            TDoubleList fillValues = new TDoubleArrayList();
            TDoubleList lineValues = new TDoubleArrayList();
            for (int row = 0; row < data.size(); row++) {
                double fillValue = statistics.getTable().getValue(fillMeasurement.getColumnName(), row);
                double lineValue = statistics.getTable().getValue(fillMeasurement.getColumnName(), row);

                fillValues.add(fillValue);
                fillMin = Math.min(fillValue, fillMin);
                fillMax = Math.max(fillValue, fillMax);

                lineValues.add(lineValue);
                lineMin = Math.min(lineValue, lineMin);
                lineMax = Math.max(lineValue, lineMax);
            }
            for (int row = 0; row < data.size(); row++) {
                double fillValue = fillValues.get(row);
                double relativeFill = (fillValue - fillMin) / (fillMax - fillMin);
                double lineValue = lineValues.get(row);
                double relativeLine = (lineValue - lineMin) / (lineMax - lineMin);
                Roi roi = data.get(row);
                if (mapFillColor.isEnabled()) {
                    roi.setFillColor(mapFillColor.getContent().apply(relativeFill));
                }
                if (mapLineColor.isEnabled()) {
                    roi.setStrokeColor(mapLineColor.getContent().apply(relativeLine));
                }
            }

            outputData.addAll(data);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData);
    }

    @JIPipeDocumentation(name = "Map fill color", description = "Allows you to map the ROI fill color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @JIPipeParameter("map-fill-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapFillColor() {
        return mapFillColor;
    }

    @JIPipeParameter("map-fill-color")
    public void setMapFillColor(OptionalColorMapParameter mapFillColor) {
        this.mapFillColor = mapFillColor;
    }

    @JIPipeDocumentation(name = "Map line color", description = "Allows you to map the ROI line color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @JIPipeParameter("map-line-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapLineColor() {
        return mapLineColor;
    }

    @JIPipeParameter("map-line-color")
    public void setMapLineColor(OptionalColorMapParameter mapLineColor) {
        this.mapLineColor = mapLineColor;
    }

    @JIPipeDocumentation(name = "Fill measurement", description = "The measurement to extract for the filling color.")
    @JIPipeParameter("fill-measurement")
    public MeasurementColumn getFillMeasurement() {
        return fillMeasurement;
    }

    @JIPipeParameter("fill-measurement")
    public void setFillMeasurement(MeasurementColumn fillMeasurement) {
        this.fillMeasurement = fillMeasurement;
    }

    @JIPipeDocumentation(name = "Line measurement", description = "The measurement to extract for the line color.")
    @JIPipeParameter("line-measurement")
    public MeasurementColumn getLineMeasurement() {
        return lineMeasurement;
    }

    @JIPipeParameter("line-measurement")
    public void setLineMeasurement(MeasurementColumn lineMeasurement) {
        this.lineMeasurement = lineMeasurement;
    }
}
