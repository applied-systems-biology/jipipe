package org.hkijena.jipipe.extensions.annotation.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.*;

/**
 * Algorithm that merges the annotations of all inputs and outputs the data with the shared annotations
 */
@JIPipeDocumentation(name = "Annotate by annotation table", description = "Merges matching annotations from an annotation table into the data set.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = AnnotationTableData.class, slotName = "Annotations", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", autoCreate = true, inheritedSlot = "Data")
public class AnnotateWithAnnotationTable extends JIPipeParameterSlotAlgorithm {

    private JIPipeIteratingAlgorithmDataBatchGenerationSettings tableMergeSettings = new JIPipeIteratingAlgorithmDataBatchGenerationSettings();
    private boolean discardExistingAnnotations = false;

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public AnnotateWithAnnotationTable(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(tableMergeSettings);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AnnotateWithAnnotationTable(AnnotateWithAnnotationTable other) {
        super(other);
        this.discardExistingAnnotations = other.discardExistingAnnotations;
        this.tableMergeSettings = new JIPipeIteratingAlgorithmDataBatchGenerationSettings(other.tableMergeSettings);
        registerSubParameter(tableMergeSettings);
    }

    @JIPipeDocumentation(name = "Table row matching", description = "The following settings determine how rows are matched up between " +
            "the annotation table and the incoming data table.")
    @JIPipeParameter(value = "table-merge-settings",
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/connector-orthogonal.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/connector-orthogonal.png")
    public JIPipeIteratingAlgorithmDataBatchGenerationSettings getTableMergeSettings() {
        return tableMergeSettings;
    }

    private List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setNode(this);
        builder.setApplyMerging(false);
        builder.setSlots(slots);
        builder.setAnnotationMergeStrategy(tableMergeSettings.getAnnotationMergeStrategy());
        builder.setReferenceColumns(tableMergeSettings.getColumnMatching(),
                tableMergeSettings.getCustomColumns());
        builder.setCustomAnnotationMatching(tableMergeSettings.getCustomAnnotationMatching());
        builder.setAnnotationMatchingMethod(tableMergeSettings.getAnnotationMatchingMethod());
        List<JIPipeMergingDataBatch> dataBatches = builder.build(progressInfo);
        dataBatches.sort(Comparator.naturalOrder());
        boolean withLimit = tableMergeSettings.getLimit().isEnabled();
        IntegerRange limit = tableMergeSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, dataBatches.size())) : null;
        if (withLimit) {
            List<JIPipeMergingDataBatch> limitedBatches = new ArrayList<>();
            for (int i = 0; i < dataBatches.size(); i++) {
                if (allowedIndices.contains(i)) {
                    limitedBatches.add(dataBatches.get(i));
                }
            }
            dataBatches = limitedBatches;
        }
        if (tableMergeSettings.isSkipIncompleteDataSets()) {
            dataBatches.removeIf(JIPipeMergingDataBatch::isIncomplete);
        }
        return dataBatches;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
            return;
        }

        JIPipeDataSlot dataInputSlot = getInputSlot("Data");

        // Create a dummy slot where we put the annotations
        JIPipeDataSlot dummy = new JIPipeDataSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "dummy", null), this);
        JIPipeDataSlot annotationSlot = getInputSlot("Annotations");
        for (int i = 0; i < annotationSlot.getRowCount(); i++) {
            AnnotationTableData data = annotationSlot.getData(i, AnnotationTableData.class, progressInfo);
            for (int j = 0; j < data.getRowCount(); j++) {
                List<JIPipeTextAnnotation> annotations = data.getAnnotations(j);
                dummy.addData(data, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        }

        // Group the data by annotations
        List<JIPipeMergingDataBatch> mergingDataBatches = generateDataBatchesDryRun(Arrays.asList(dataInputSlot, dummy), progressInfo);
        for (JIPipeMergingDataBatch dataBatch : mergingDataBatches) {
            Set<Integer> dataRows = dataBatch.getInputRows("Data");
            if (dataRows == null)
                continue;
            Set<Integer> metadataRows = dataBatch.getInputRows(dummy);

            Map<String, JIPipeTextAnnotation> newAnnotations = new HashMap<>();
            for (int row : metadataRows) {
                for (JIPipeTextAnnotation annotation : dummy.getAnnotations(row)) {
                    JIPipeTextAnnotation existing = newAnnotations.getOrDefault(annotation.getName(), null);
                    if (existing != null) {
                        String value = getTableMergeSettings().getAnnotationMergeStrategy().merge(existing.getValue(), annotation.getValue());
                        existing = new JIPipeTextAnnotation(existing.getName(), value);
                    } else {
                        existing = annotation;
                    }
                    newAnnotations.put(annotation.getName(), existing);
                }
            }

            for (int row : dataRows) {
                Map<String, JIPipeTextAnnotation> annotationMap = new HashMap<>();

                // Fetch existing annotations
                if (!discardExistingAnnotations) {
                    for (JIPipeTextAnnotation annotation : dataInputSlot.getAnnotations(row)) {
                        annotationMap.put(annotation.getName(), annotation);
                    }
                }

                // Merge new annotations
                for (JIPipeTextAnnotation annotation : newAnnotations.values()) {
                    JIPipeTextAnnotation existing = annotationMap.getOrDefault(annotation.getName(), null);
                    if (existing != null) {
                        String value = getTableMergeSettings().getAnnotationMergeStrategy().merge(existing.getValue(), annotation.getValue());
                        existing = new JIPipeTextAnnotation(existing.getName(), value);
                    } else {
                        existing = annotation;
                    }
                    annotationMap.put(annotation.getName(), existing);
                }

                // Add data to output
                getFirstOutputSlot().addData(dataInputSlot.getData(row, JIPipeData.class, progressInfo), new ArrayList<>(annotationMap.values()), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        }
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(getInputSlot("Data"), progressInfo);
    }

    @JIPipeDocumentation(name = "Replace all existing annotations", description = "If enabled, existing annotations will not be carried over into the output.")
    @JIPipeParameter("discard-existing-annotations")
    public boolean isDiscardExistingAnnotations() {
        return discardExistingAnnotations;
    }

    @JIPipeParameter("discard-existing-annotations")
    public void setDiscardExistingAnnotations(boolean discardExistingAnnotations) {
        this.discardExistingAnnotations = discardExistingAnnotations;
    }
}
