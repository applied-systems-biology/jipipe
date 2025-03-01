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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import com.google.common.primitives.Doubles;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipePercentageProgressInfo;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.AllMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Enlarge/shrink 2D ROI", description = "Enlarges or shrinks each input ROI")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class EnlargeShrinkRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter pixels = new JIPipeExpressionParameter("5");
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    public EnlargeShrinkRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public EnlargeShrinkRoiAlgorithm(EnlargeShrinkRoiAlgorithm other) {
        super(other);
        this.pixels = new JIPipeExpressionParameter(other.pixels);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData input = iterationStep.getInputData("Input", ROI2DListData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));

        RoiStatisticsAlgorithm roiStatisticsAlgorithm =
                JIPipe.createNode(RoiStatisticsAlgorithm.class);
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(input, progressInfo);
        if (reference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(new ImagePlusData(reference), progressInfo);
        }
        roiStatisticsAlgorithm.run(runContext, progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumnData column = statistics.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
            } else {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
            }
        }

        ROI2DListData output = new ROI2DListData();
        JIPipePercentageProgressInfo processingRoiProgress = progressInfo.percentage("Processing ROI");
        for (int i = 0; i < input.size(); i++) {
            Roi roi = input.get(i);
            processingRoiProgress.logPercentage(i, input.size());
            Map<String, String> roiProperties = ImageJUtils.getRoiProperties(roi);
            variableSet.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variableSet.set("metadata." + entry.getKey(), entry.getValue());
            }
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variableSet.set(statistics.getColumnName(col), statistics.getValueAt(i, col));
            }

            double change = pixels.evaluateToDouble(variableSet);
            if(change != 0) {
                roi = RoiEnlarger.enlarge(roi, change);
            }
            output.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Enlarge/shrink (px)", description = "The pixels that determine by how much the ROI is enlarged. Return negative values to shrink the ROI.")
    @JIPipeParameter(value = "pixels", important = true)
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AllMeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getPixels() {
        return pixels;
    }

    @JIPipeParameter("pixels")
    public void setPixels(JIPipeExpressionParameter pixels) {
        this.pixels = pixels;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate." + "<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter(value = "measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
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
