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

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.Map;

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

    private DefaultExpressionParameter filters = new DefaultExpressionParameter();
    private RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode("ij1-roi-statistics", RoiStatisticsAlgorithm.class);
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
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
        ROIListData allROIs = new ROIListData();
        ResultsTableData allStatistics = new ResultsTableData();

        roiStatisticsAlgorithm.setOverrideReferenceImage(true);

        for (Map.Entry<ImagePlusData, ROIListData> entry : getReferenceImage(dataBatch, progressInfo).entrySet()) {
            // Obtain statistics
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(entry.getValue(), progressInfo);
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(entry.getKey(), progressInfo);
            roiStatisticsAlgorithm.run(progressInfo);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            allROIs.addAll(entry.getValue());
            allStatistics.addRows(statistics);
        }

        // Apply filter
        ROIListData outputData = new ROIListData();
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        for (int row = 0; row < allStatistics.getRowCount(); row++) {
            for (int col = 0; col < allStatistics.getColumnCount(); col++) {
                variableSet.set(allStatistics.getColumnName(col), allStatistics.getValueAt(row, col));
            }
            if (filters.test(variableSet)) {
                outputData.add(allROIs.get(row));
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeParameter("filter")
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'X' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'")
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

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            setFilters(new DefaultExpressionParameter("Area > 100 AND Circ. >= 0.6"));
        }
    }
}
