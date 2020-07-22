package org.hkijena.jipipe.extensions.annotation.algorithms;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that merges the annotations of all inputs and outputs the data with the shared annotations
 */
@JIPipeDocumentation(name = "Annotate by annotation table", description = "Merges matching annotations from an annotation table into the data set.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = AnnotationTableData.class, slotName = "Annotations", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", autoCreate = true, inheritedSlot = "Data")
public class AnnotateWithAnnotationTable extends JIPipeIteratingAlgorithm {

    private boolean discardExistingAnnotations = false;

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public AnnotateWithAnnotationTable(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AnnotateWithAnnotationTable(AnnotateWithAnnotationTable other) {
        super(other);
        this.discardExistingAnnotations = other.discardExistingAnnotations;
    }

    @Override
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }

        Map<String, JIPipeDataSlot> slotMap = new HashMap<>();
        JIPipeDataSlot dataInputSlot = getInputSlot("Data");
        slotMap.put("Data", dataInputSlot);

        // Create a dummy slot where we put the annotations
        JIPipeDataSlot dummy = new JIPipeDataSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "dummy", null), this);
        slotMap.put("dummy", dummy);
        JIPipeDataSlot annotationSlot = getInputSlot("Annotations");
        for (int i = 0; i < annotationSlot.getRowCount(); i++) {
            AnnotationTableData data = annotationSlot.getData(i, AnnotationTableData.class);
            for (int j = 0; j < data.getRowCount(); j++) {
                List<JIPipeAnnotation> annotations = data.getAnnotations(j);
                dummy.addData(data, annotations);
            }
        }

        // Group the data by annotations
        Set<Map.Entry<JIPipeDataBatchKey, Map<String, TIntSet>>> byDataBatch = groupDataByMetadata(slotMap).entrySet();
        for (Map.Entry<JIPipeDataBatchKey, Map<String, TIntSet>> entry : byDataBatch) {
            TIntSet dataRows = entry.getValue().getOrDefault("Data", null);
            if (dataRows == null)
                continue;
            TIntSet metadataRows = entry.getValue().getOrDefault("dummy", new TIntHashSet());

            Map<String, JIPipeAnnotation> newAnnotations = new HashMap<>();
            for (TIntIterator it = metadataRows.iterator(); it.hasNext(); ) {
                int row = it.next();
                for (JIPipeAnnotation annotation : dummy.getAnnotations(row)) {
                    JIPipeAnnotation existing = newAnnotations.getOrDefault(annotation.getName(), null);
                    if (existing != null) {
                        String value = getDataBatchGenerationSettings().getAnnotationMergeStrategy().merge(existing.getValue(), annotation.getValue());
                        existing = new JIPipeAnnotation(existing.getName(), value);
                    } else {
                        existing = annotation;
                    }
                    newAnnotations.put(annotation.getName(), existing);
                }
            }

            for (TIntIterator it = dataRows.iterator(); it.hasNext(); ) {
                int row = it.next();
                Map<String, JIPipeAnnotation> annotationMap = new HashMap<>();

                // Fetch existing annotations
                if (!discardExistingAnnotations) {
                    for (JIPipeAnnotation annotation : dataInputSlot.getAnnotations(row)) {
                        annotationMap.put(annotation.getName(), annotation);
                    }
                }

                // Merge new annotations
                for (JIPipeAnnotation annotation : newAnnotations.values()) {
                    JIPipeAnnotation existing = annotationMap.getOrDefault(annotation.getName(), null);
                    if (existing != null) {
                        String value = getDataBatchGenerationSettings().getAnnotationMergeStrategy().merge(existing.getValue(), annotation.getValue());
                        existing = new JIPipeAnnotation(existing.getName(), value);
                    } else {
                        existing = annotation;
                    }
                    annotationMap.put(annotation.getName(), existing);
                }

                // Add data to output
                getFirstOutputSlot().addData(dataInputSlot.getData(row, JIPipeData.class), new ArrayList<>(annotationMap.values()));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Non-functional
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough() {
        getFirstOutputSlot().copyFrom(getInputSlot("Data"));
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
