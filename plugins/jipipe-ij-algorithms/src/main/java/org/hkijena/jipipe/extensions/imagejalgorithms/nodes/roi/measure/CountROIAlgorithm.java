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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Count ROI", description = "Outputs one table that contains the counts for each incoming ROI. " +
        "Each ROI list produces one column in the output table and contains all annotations of the incoming ROI data. " +
        "You can add multiple input slots. " +
        "All output table column is named according to the slot name of the incoming ROI list." +
        "This node can merge ROI lists according to their annotations. The sum of the counts within the same data batch are generated.")
@AddJIPipeInputSlot(value = ROIListData.class)
@AddJIPipeOutputSlot(value = AnnotationTableData.class, slotName = "Counts")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
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
    public boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(new AnnotationTableData(), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        currentResult = new AnnotationTableData();
        super.runParameterSet(runContext, progressInfo, parameterAnnotations);

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
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // write into result
        int row = currentResult.addRow();
        for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
            List<ROIListData> rois = iterationStep.getInputData(inputSlot, ROIListData.class, progressInfo);
            long count = rois.stream().collect(Collectors.summarizingInt(ROIListData::size)).getSum();
            int col = currentResult.getOrCreateColumnIndex(inputSlot.getName(), false);
            currentResult.setValueAt(1.0 * count, row, col);
        }
        if (addAnnotations) {
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                int col = currentResult.getOrCreateColumnIndex(annotation.getName(), false);
                currentResult.setValueAt(annotation.getValue(), row, col);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Add annotations", description = "If enabled, incoming annotations are added into the output table.")
    @JIPipeParameter("add-annotations")
    public boolean isAddAnnotations() {
        return addAnnotations;
    }

    @JIPipeParameter("add-annotations")
    public void setAddAnnotations(boolean addAnnotations) {
        this.addAnnotations = addAnnotations;
    }
}
