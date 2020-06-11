package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumnSortOrder;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.colors.ColorMapEnumItemInfo;
import org.hkijena.acaq5.extensions.parameters.colors.OptionalColorMapParameter;
import org.hkijena.acaq5.extensions.parameters.editors.EnumParameterSettings;
import org.hkijena.acaq5.extensions.parameters.roi.IntModificationParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Sort and extract ROI by statistics", description = "Sorts the ROI list elements via statistics and allows to you extract the n top values. " +
        "Optionally, line and fill colors of the output rows can be colored according to the output order. " + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class SortAndExtractRoiByStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private MeasurementColumnSortOrder.List sortOrderList = new MeasurementColumnSortOrder.List();
    private IntModificationParameter selection = new IntModificationParameter();
    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-statistics");
    private boolean autoClamp = true;
    private OptionalColorMapParameter mapFillColor = new OptionalColorMapParameter();
    private OptionalColorMapParameter mapLineColor = new OptionalColorMapParameter();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public SortAndExtractRoiByStatisticsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ROIListData.class, "Output");
        selection.setUseExactValue(true);
        sortOrderList.addNewInstance();
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public SortAndExtractRoiByStatisticsAlgorithm(SortAndExtractRoiByStatisticsAlgorithm other) {
        super(other);
        this.sortOrderList = new MeasurementColumnSortOrder.List(other.sortOrderList);
        this.selection = new IntModificationParameter(other.selection);
        this.autoClamp = other.autoClamp;
        this.mapFillColor = new OptionalColorMapParameter(other.mapFillColor);
        this.mapLineColor = new OptionalColorMapParameter(other.mapLineColor);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.getMeasurements().setNativeValue(sortOrderList.getNativeMeasurementEnumValue());

        // Continue with run
        super.run(subProgress, algorithmProgress, isCancelled);
    }


    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = dataInterface.getInputData("ROI", ROIListData.class);
        ImagePlusData referenceImageData = new ImagePlusData(getReferenceImage(dataInterface, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled));

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(data);
        roiStatisticsAlgorithm.getInputSlot("Reference").addData(referenceImageData);
        roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);

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

        int topN = selection.apply(data.size());
        if (autoClamp) {
            topN = Math.min(topN, data.size());
        }

        ROIListData outputData = new ROIListData();
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

        dataInterface.addOutputData(getFirstOutputSlot(), outputData);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Sort order", description = "Allows you to determine by which measurement columns to sort by. You can order by multiple columns " +
            "where the order within this list. If you for example order by 'Area' and then 'Perimeter', the ROI will be ordered by area and if the area is the same by perimeter.")
    @ACAQParameter("sort-orders")
    public MeasurementColumnSortOrder.List getSortOrderList() {
        return sortOrderList;
    }

    @ACAQParameter("sort-orders")
    public void setSortOrderList(MeasurementColumnSortOrder.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @ACAQDocumentation(name = "Selection (top n)", description = "Allows you to select an exact amount or a fraction of ROI. The top N items will be selected.")
    @ACAQParameter("selection")
    public IntModificationParameter getSelection() {
        return selection;
    }

    @ACAQParameter("selection")
    public void setSelection(IntModificationParameter selection) {
        this.selection = selection;
    }

    @ACAQDocumentation(name = "Ignore missing items", description = "If enabled, there will be not error if you select too many items (e.g. if the list only contains " +
            "10 items, but you select the top 20)")
    @ACAQParameter("auto-clamp")
    public boolean isAutoClamp() {
        return autoClamp;
    }

    @ACAQParameter("auto-clamp")
    public void setAutoClamp(boolean autoClamp) {
        this.autoClamp = autoClamp;
    }

    @ACAQDocumentation(name = "Map fill color", description = "Allows you to map the ROI fill color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @ACAQParameter("map-fill-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapFillColor() {
        return mapFillColor;
    }

    @ACAQParameter("map-fill-color")
    public void setMapFillColor(OptionalColorMapParameter mapFillColor) {
        this.mapFillColor = mapFillColor;
    }

    @ACAQDocumentation(name = "Map line color", description = "Allows you to map the ROI line color to the order generated by the sorting. " +
            "The color is rendered when converting into a RGB visualization.")
    @ACAQParameter("map-line-color")
    @EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class)
    public OptionalColorMapParameter getMapLineColor() {
        return mapLineColor;
    }

    @ACAQParameter("map-line-color")
    public void setMapLineColor(OptionalColorMapParameter mapLineColor) {
        this.mapLineColor = mapLineColor;
    }
}
