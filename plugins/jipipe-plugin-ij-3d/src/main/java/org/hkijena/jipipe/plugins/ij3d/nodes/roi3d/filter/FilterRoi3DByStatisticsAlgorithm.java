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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter;

import com.google.common.primitives.Doubles;
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
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.AllROI3DMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Filter 3D ROI by statistics", description = "Filters the 3D ROI list elements via statistics.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class FilterRoi3DByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {
    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter();
    private ROI3DMeasurementSetParameter measurements = new ROI3DMeasurementSetParameter();
    private boolean outputEmptyLists = true;
    private boolean measureInPhysicalUnits = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoi3DByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterRoi3DByStatisticsAlgorithm(FilterRoi3DByStatisticsAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
        this.measurements = new ROI3DMeasurementSetParameter(other.measurements);
        this.outputEmptyLists = other.outputEmptyLists;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData inputRois = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap()
                .putAnnotations(iterationStep.getMergedTextAnnotations())
                .putCustomVariables(getDefaultCustomExpressionVariables());

        // Obtain statistics
        ResultsTableData statistics = inputRois.measure(IJ3DUtils.wrapImage(inputReference), measurements.getNativeValue(), measureInPhysicalUnits, "", progressInfo.resolve("Measuring ROIs"));

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumnData column = statistics.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
            } else {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
            }
        }
        variableSet.set("num_roi", inputRois.size());

        // Apply filter
        ROI3DListData outputData = new ROI3DListData();

        for (int row = 0; row < statistics.getRowCount(); row++) {
            ROI3D roi = inputRois.get(row);

            // Write metadata
            Map<String, String> roiProperties = roi.getMetadata();
            variableSet.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variableSet.set("metadata." + entry.getKey(), entry.getValue());
            }

            // Write statistics
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variableSet.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
            }

            // Filter
            if (filters.test(variableSet)) {
                outputData.add(roi);
            }
        }

        if (!outputData.isEmpty() || outputEmptyLists) {
            iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }

    @JIPipeParameter(value = "filter", important = true)
    @SetJIPipeDocumentation(name = "Only keep ROI if", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'f' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'." +
            "Annotations are available as variables.")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ROI3DMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DMeasurementSetParameter measurements) {
        this.measurements = measurements;
    }

    @SetJIPipeDocumentation(name = "Output empty lists", description = "If enabled, the node will also output empty lists.")
    @JIPipeParameter("output-empty-lists")
    public boolean isOutputEmptyLists() {
        return outputEmptyLists;
    }

    @JIPipeParameter("output-empty-lists")
    public void setOutputEmptyLists(boolean outputEmptyLists) {
        this.outputEmptyLists = outputEmptyLists;
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
