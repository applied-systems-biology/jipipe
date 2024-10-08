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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementColumn;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Color 3D ROI by statistics", description = "Sets the 3D ROI item colors by measurements.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class ColorRoi3DByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {
    private ROI3DMeasurementColumn fillMeasurement = ROI3DMeasurementColumn.Index;
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();

    private boolean measureInPhysicalUnits = true;

    public ColorRoi3DByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.mapFillColor.setEnabled(true);
    }

    public ColorRoi3DByStatisticsAlgorithm(ColorRoi3DByStatisticsAlgorithm other) {
        super(other);
        this.fillMeasurement = other.fillMeasurement;
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputData = new ROI3DListData();

        ROI3DListData inputRois = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Obtain statistics
        ResultsTableData statistics = inputRois.measure(IJ3DUtils.wrapImage(inputReference),
                fillMeasurement.getNativeValue(),
                measureInPhysicalUnits,
                "",
                progressInfo.resolve("Measure ROI"));

        // Apply color map
        double fillMin = Double.POSITIVE_INFINITY;
        double fillMax = Double.NEGATIVE_INFINITY;
        TDoubleList fillValues = new TDoubleArrayList();
        for (int row = 0; row < inputRois.size(); row++) {
            Object measurement = statistics.getValueAt(row, statistics.getColumnIndex(fillMeasurement.getColumnName()));
            double fillValue;
            if (measurement instanceof Number) {
                fillValue = ((Number) measurement).doubleValue();
            } else {
                fillValue = measurement.hashCode();
            }

            fillValues.add(fillValue);
            fillMin = Math.min(fillValue, fillMin);
            fillMax = Math.max(fillValue, fillMax);
        }
        for (int row = 0; row < inputRois.size(); row++) {
            double fillValue = fillValues.get(row);
            double relativeFill = (fillValue - fillMin) / (fillMax - fillMin);
            ROI3D roi = new ROI3D(inputRois.get(row));
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(relativeFill));
            }
            outputData.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Map fill color", description = "Allows you to map the ROI fill color to the order generated by the sorting. " +
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

    @SetJIPipeDocumentation(name = "Fill measurement", description = "The measurement to extract for the filling color.")
    @JIPipeParameter("fill-measurement")
    public ROI3DMeasurementColumn getFillMeasurement() {
        return fillMeasurement;
    }

    @JIPipeParameter("fill-measurement")
    public void setFillMeasurement(ROI3DMeasurementColumn fillMeasurement) {
        this.fillMeasurement = fillMeasurement;
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
}
