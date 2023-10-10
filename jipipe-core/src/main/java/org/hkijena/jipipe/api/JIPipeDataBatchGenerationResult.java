package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;

import java.util.*;

/**
 * Result of a data batch generation run
 */
public class JIPipeDataBatchGenerationResult {
    private List<JIPipeMergingDataBatch> dataBatches = new ArrayList<>();
    private Set<String> referenceTextAnnotationColumns = new HashSet<>();

    public List<JIPipeMergingDataBatch> getDataBatches() {
        return dataBatches;
    }

    public void setDataBatches(List<JIPipeMergingDataBatch> dataBatches) {
        this.dataBatches = dataBatches;
    }

    public Set<String> getReferenceTextAnnotationColumns() {
        return referenceTextAnnotationColumns;
    }

    public void setReferenceTextAnnotationColumns(Set<String> referenceTextAnnotationColumns) {
        this.referenceTextAnnotationColumns = referenceTextAnnotationColumns;
    }

    public void setDataBatches(JIPipeMergingDataBatch... dataBatches) {
        this.dataBatches = new ArrayList<>();
        for (JIPipeMergingDataBatch dataBatch : dataBatches) {
            this.dataBatches.add(dataBatch);
        }
    }
}
