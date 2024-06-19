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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.sort;

import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementColumnSortOrder;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Sort and extract ROI by statistics", description = "Sorts the ROI list elements via statistics and allows to you extract the n top values. " +
        "Optionally, line and fill colors of the output rows can be colored according to the output order. ")
@LabelAsJIPipeHidden
@Deprecated
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class SortAndExtractRoiByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipe.createNode("ij1-roi-statistics"
    );
    private MeasurementColumnSortOrder.List sortOrderList = new MeasurementColumnSortOrder.List();
    private NumericFunctionExpression selection = new NumericFunctionExpression();
    private boolean autoClamp = true;
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SortAndExtractRoiByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        selection.ensureExactValue(true);
        sortOrderList.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public SortAndExtractRoiByStatisticsAlgorithm(SortAndExtractRoiByStatisticsAlgorithm other) {
        super(other);
        this.sortOrderList = new MeasurementColumnSortOrder.List(other.sortOrderList);
        this.selection = new NumericFunctionExpression(other.selection);
        this.autoClamp = other.autoClamp;
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
        this.mapLineColor = new OptionalColorMapParameter(other.mapLineColor);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(sortOrderList.getNativeMeasurementEnumValue());

        // Continue with run
        super.run(runContext, progressInfo);
    }


    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        ROIListData outputData = new ROIListData();

        ROIListData data = inputRois;
        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(data, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(runContext, progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

        // Apply sorting
        if (!sortOrderList.isEmpty()) {
            Comparator<Integer> comparator = sortOrderList.get(0).getRowComparator(statistics);
            for (int i = 1; i < sortOrderList.size(); i++) {
                comparator = comparator.thenComparing(sortOrderList.get(i).getRowComparator(statistics));
            }
            List<Integer> rowIndices = new ArrayList<>(statistics.getRowCount());
            for (int i = 0; i < statistics.getRowCount(); i++) {
                rowIndices.add(i);
            }
            rowIndices.sort(comparator);

            ROIListData sortedData = new ROIListData();
            for (int i = 0; i < data.size(); i++) {
                sortedData.add(null);
            }
            for (int i = 0; i < data.size(); i++) {
                sortedData.set(i, data.get(rowIndices.get(i)));
            }
            data = sortedData;
        }

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        int topN = (int) selection.apply(data.size(), variables);
        if (autoClamp) {
            topN = Math.min(topN, data.size());
        }

        for (int i = 0; i < topN; i++) {
            Roi roi = data.get(i);
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(i * 1.0 / topN));
            }
            if (mapLineColor.isEnabled()) {
                roi.setStrokeColor(mapLineColor.getContent().apply(i * 1.0 / topN));
            }
            outputData.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Sort order", description = "Allows you to determine by which measurement columns to sort by. You can order by multiple columns " +
            "where the order within this list. If you for example order by 'Area' and then 'Perimeter', the ROI will be ordered by area and if the area is the same by perimeter.")
    @JIPipeParameter("sort-orders")
    public MeasurementColumnSortOrder.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-orders")
    public void setSortOrderList(MeasurementColumnSortOrder.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @SetJIPipeDocumentation(name = "Selection (top n)", description = "Allows you to select an exact amount or a fraction of ROI. The top N items will be selected.")
    @JIPipeParameter("selection")
    public NumericFunctionExpression getSelection() {
        return selection;
    }

    @JIPipeParameter("selection")
    public void setSelection(NumericFunctionExpression selection) {
        this.selection = selection;
    }

    @SetJIPipeDocumentation(name = "Ignore missing items", description = "If enabled, there will be not error if you select too many items (e.g. if the list only contains " +
            "10 items, but you select the top 20)")
    @JIPipeParameter("auto-clamp")
    public boolean isAutoClamp() {
        return autoClamp;
    }

    @JIPipeParameter("auto-clamp")
    public void setAutoClamp(boolean autoClamp) {
        this.autoClamp = autoClamp;
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

    @SetJIPipeDocumentation(name = "Map line color", description = "Allows you to map the ROI line color to the order generated by the sorting. " +
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
}
