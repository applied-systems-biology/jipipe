package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;

import java.util.*;

/**
 * Result of a data batch generation run
 */
public class JIPipeDataBatchGenerationResult {
    private List<JIPipeMultiDataBatch> dataBatches = new ArrayList<>();
    private Set<String> referenceTextAnnotationColumns = new HashSet<>();

    public List<JIPipeMultiDataBatch> getDataBatches() {
        return dataBatches;
    }

    public void setDataBatches(List<JIPipeMultiDataBatch> dataBatches) {
        this.dataBatches = dataBatches;
    }

    public Set<String> getReferenceTextAnnotationColumns() {
        return referenceTextAnnotationColumns;
    }

    public void setReferenceTextAnnotationColumns(Set<String> referenceTextAnnotationColumns) {
        this.referenceTextAnnotationColumns = referenceTextAnnotationColumns;
    }

    public void setDataBatches(JIPipeMultiDataBatch... dataBatches) {
        this.dataBatches = new ArrayList<>();
        for (JIPipeMultiDataBatch dataBatch : dataBatches) {
            this.dataBatches.add(dataBatch);
        }
    }
}
