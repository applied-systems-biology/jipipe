package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Count ROI", description = "Outputs one table that contains the counts for each incoming ROI. " +
        "Each ROI list produces one column in the output table and contains all annotations of the incoming ROI data. " +
        "You can add multiple input slots. " +
        "All output table column is named according to the slot name of the incoming ROI list." +
        "This node can merge ROI lists according to their annotations. The sum of the counts within the same data batch are generated.")
@JIPipeInputSlot(value = ROIListData.class)
@JIPipeOutputSlot(value = AnnotationTableData.class, slotName = "Counts")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
public class CountROIAlgorithm extends JIPipeMergingAlgorithm {

    private AnnotationTableData currentResult;
    private boolean addAnnotations = true;

    public CountROIAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Counted ROI", "ROI to be counted", ROIListData.class)
                .addOutputSlot("Counts", "The counts", AnnotationTableData.class)
                .restrictInputTo(ROIListData.class)
                .sealOutput().build());
    }

    public CountROIAlgorithm(CountROIAlgorithm other) {
        super(other);
        this.addAnnotations = other.addAnnotations;
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new AnnotationTableData(), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        currentResult = new AnnotationTableData();
        super.runParameterSet(progressInfo, parameterAnnotations);

        // We do a custom merge into one final table
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
            for (int row = 0; row < inputSlot.getRowCount(); row++) {
                annotations.addAll(inputSlot.getTextAnnotations(row));
            }
        }
        getFirstOutputSlot().addData(currentResult, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        currentResult = null;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        // write into result
        int row = currentResult.addRow();
        for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
            List<ROIListData> rois = dataBatch.getInputData(inputSlot, ROIListData.class, progressInfo);
            long count = rois.stream().collect(Collectors.summarizingInt(ROIListData::size)).getSum();
            int col = currentResult.getOrCreateColumnIndex(inputSlot.getName(), false);
            currentResult.setValueAt(1.0 * count, row, col);
        }
        if (addAnnotations) {
            for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
                int col = currentResult.getOrCreateColumnIndex(annotation.getName(), false);
                currentResult.setValueAt(annotation.getValue(), row, col);
            }
        }
    }

    @JIPipeDocumentation(name = "Add annotations", description = "If enabled, incoming annotations are added into the output table.")
    @JIPipeParameter("add-annotations")
    public boolean isAddAnnotations() {
        return addAnnotations;
    }

    @JIPipeParameter("add-annotations")
    public void setAddAnnotations(boolean addAnnotations) {
        this.addAnnotations = addAnnotations;
    }
}