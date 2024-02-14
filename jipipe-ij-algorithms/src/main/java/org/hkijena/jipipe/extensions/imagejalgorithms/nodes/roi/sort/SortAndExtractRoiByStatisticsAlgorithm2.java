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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.sort;

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
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumnSortOrder;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMapEnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorMapParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Sort and extract ROI by statistics", description = "Sorts the ROI list elements via statistics and allows to you extract the n top values. " +
        "Optionally, line and fill colors of the output rows can be colored according to the output order. ")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class SortAndExtractRoiByStatisticsAlgorithm2 extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipe.createNode("ij1-roi-statistics"
    );
    private MeasurementColumnSortOrder.List sortOrderList = new MeasurementColumnSortOrder.List();
    private OptionalIntegerRange selectedIndices = new OptionalIntegerRange(new IntegerRange("0"), false);
    private boolean autoClamp = true;
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SortAndExtractRoiByStatisticsAlgorithm2(JIPipeNodeInfo info) {
        super(info);
        sortOrderList.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public SortAndExtractRoiByStatisticsAlgorithm2(SortAndExtractRoiByStatisticsAlgorithm2 other) {
        super(other);
        this.sortOrderList = new MeasurementColumnSortOrder.List(other.sortOrderList);
        this.selectedIndices = new OptionalIntegerRange(other.selectedIndices);
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
        roiStatisticsAlgorithm.clearSlotData();
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

        List<Integer> indices;
        if (selectedIndices.isEnabled()) {
            indices = selectedIndices.getContent().getIntegers(0, data.size() - 1, variables);
        } else {
            indices = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                indices.add(i);
            }
        }
        int nSelected = indices.size();

        for (int roiIndex : indices) {
            if (autoClamp && (roiIndex < 0 || roiIndex >= data.size())) {
                continue;
            }
            Roi roi = data.get(roiIndex);
            if (mapFillColor.isEnabled()) {
                roi.setFillColor(mapFillColor.getContent().apply(roiIndex * 1.0 / nSelected));
            }
            if (mapLineColor.isEnabled()) {
                roi.setStrokeColor(mapLineColor.getContent().apply(roiIndex * 1.0 / nSelected));
            }
            outputData.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Sort order", description = "Allows you to determine by which measurement columns to sort by. You can order by multiple columns " +
            "where the order within this list. If you for example order by 'Area' and then 'Perimeter', the ROI will be ordered by area and if the area is the same by perimeter.")
    @JIPipeParameter("sort-orders")
    public MeasurementColumnSortOrder.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-orders")
    public void setSortOrderList(MeasurementColumnSortOrder.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @JIPipeDocumentation(name = "Selected indices", description = "If enabled, the output will only contain the ROI indices specified in by the value. For example, the value '0' returns the first ROI. A value of '0-4' returns the top 5 items.")
    @JIPipeParameter("selected-indices")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public OptionalIntegerRange getSelectedIndices() {
        return selectedIndices;
    }

    @JIPipeParameter("selected-indices")
    public void setSelectedIndices(OptionalIntegerRange selectedIndices) {
        this.selectedIndices = selectedIndices;
    }

    @JIPipeDocumentation(name = "Ignore missing items", description = "If enabled, there will be not error if you select too many items (e.g. if the list only contains " +
            "10 items, but you select the top 20)")
    @JIPipeParameter("auto-clamp")
    public boolean isAutoClamp() {
        return autoClamp;
    }

    @JIPipeParameter("auto-clamp")
    public void setAutoClamp(boolean autoClamp) {
        this.autoClamp = autoClamp;
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
}
