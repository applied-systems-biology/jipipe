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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.filter;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter ROI by statistics", description = "Filters the ROI list elements via statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class FilterRoiByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private DefaultExpressionParameter filters = new DefaultExpressionParameter();
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean outputEmptyLists = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterRoiByStatisticsAlgorithm(FilterRoiByStatisticsAlgorithm other) {
        super(other);
        roiStatisticsAlgorithm.setAllSlotsVirtual(false, false, null);
        this.filters = new DefaultExpressionParameter(other.filters);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        roiStatisticsAlgorithm.setAllSlotsVirtual(false, false, null);
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.setMeasurements(measurements);

        // Continue with run
        super.run(progressInfo);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputRois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputRois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

        // Apply filter
        ROIListData outputData = new ROIListData();
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        for (int row = 0; row < statistics.getRowCount(); row++) {
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variableSet.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
            }
            if (filters.test(variableSet)) {
                outputData.add(inputRois.get(row));
            }
        }

        if (!outputData.isEmpty() || outputEmptyLists) {
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
    }

    private ImagePlus getReferenceImage(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = null;
        {
            ImagePlusData data = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
            if (data != null) {
                reference = data.getDuplicateImage();
            }
        }
        return reference;
    }

    @JIPipeParameter(value = "filter", important = true)
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'f' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'." +
            "Annotations are available as variables.")
    @ExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
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
}
