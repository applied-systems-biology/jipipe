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

package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.filter;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.AllMeasurement3DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.utils.Measurement3DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.utils.Measurements3DSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameterSettings;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter 3D ROI by statistics", description = "Filters the 3D ROI list elements via statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class FilterRoi3DByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {
    private final CustomExpressionVariablesParameter customFilterVariables;
    private DefaultExpressionParameter filters = new DefaultExpressionParameter();
    private Measurements3DSetParameter measurements = new Measurements3DSetParameter();
    private boolean outputEmptyLists = true;
    private boolean measureInPhysicalUnits = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoi3DByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customFilterVariables = new CustomExpressionVariablesParameter(this);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterRoi3DByStatisticsAlgorithm(FilterRoi3DByStatisticsAlgorithm other) {
        super(other);
        this.filters = new DefaultExpressionParameter(other.filters);
        this.measurements = new Measurements3DSetParameter(other.measurements);
        this.outputEmptyLists = other.outputEmptyLists;
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData inputRois = dataBatch.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(dataBatch.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(variableSet, true, "custom.", true, "custom");

        // Obtain statistics
        ResultsTableData statistics = inputRois.measure(IJ3DUtils.wrapImage(inputReference), measurements.getNativeValue(), measureInPhysicalUnits, progressInfo.resolve("Measuring ROIs"));

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumn column = statistics.getColumnReference(col);
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
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }

    @JIPipeParameter(value = "filter", important = true)
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'f' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'." +
            "Annotations are available as variables.")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public Measurements3DSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(Measurements3DSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Output empty lists", description = "If enabled, the node will also output empty lists.")
    @JIPipeParameter("output-empty-lists")
    public boolean isOutputEmptyLists() {
        return outputEmptyLists;
    }

    @JIPipeParameter("output-empty-lists")
    public void setOutputEmptyLists(boolean outputEmptyLists) {
        this.outputEmptyLists = outputEmptyLists;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomFilterVariables() {
        return customFilterVariables;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
