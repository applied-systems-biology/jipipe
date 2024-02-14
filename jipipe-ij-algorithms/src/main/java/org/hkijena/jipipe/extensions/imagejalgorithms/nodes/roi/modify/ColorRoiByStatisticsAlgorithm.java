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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Color ROI by statistics", description = "Sets the ROI item colors by measurements.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ColorRoiByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private MeasurementColumn fillMeasurement = MeasurementColumn.Area;
    private MeasurementColumn lineMeasurement = MeasurementColumn.Area;
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ColorRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(fillMeasurement.getNativeValue() | lineMeasurement.getNativeValue());

        // Continue with run
        super.run(runContext, progressInfo);
    }


    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData outputData = new ROIListData();

        ROIListData inputRois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputRois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(runContext, progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

        // Apply color map
        double fillMin = Double.POSITIVE_INFINITY;
        double fillMax = Double.NEGATIVE_INFINITY;
        double lineMin = Double.POSITIVE_INFINITY;
        double lineMax = Double.NEGATIVE_INFINITY;
        TDoubleList fillValues = new TDoubleArrayList();
        TDoubleList lineValues = new TDoubleArrayList();
        for (int row = 0; row < inputRois.size(); row++) {
            double fillValue = statistics.getTable().getValue(fillMeasurement.getColumnName(), row);
            double lineValue = statistics.getTable().getValue(fillMeasurement.getColumnName(), row);

            fillValues.add(fillValue);
            fillMin = Math.min(fillValue, fillMin);
            fillMax = Math.max(fillValue, fillMax);

            lineValues.add(lineValue);
            lineMin = Math.min(lineValue, lineMin);
            lineMax = Math.max(lineValue, lineMax);
        }
        for (int row = 0; row < inputRois.size(); row++) {
            double fillValue = fillValues.get(row);
            double relativeFill = (fillValue - fillMin) / (fillMax - fillMin);
            double lineValue = lineValues.get(row);
            double relativeLine = (lineValue - lineMin) / (lineMax - lineMin);
            Roi roi = (Roi) inputRois.get(row).clone();
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(relativeFill));
            }
            if (mapLineColor.isEnabled()) {
                roi.setStrokeColor(mapLineColor.getContent().apply(relativeLine));
            }
            outputData.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
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
