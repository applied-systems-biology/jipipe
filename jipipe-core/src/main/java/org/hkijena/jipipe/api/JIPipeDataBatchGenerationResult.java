package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JIPipeDataBatchGenerationResult {
    private List<JIPipeMergingDataBatch> dataBatches = Collections.emptyList();
    private Set<String> referenceTextAnnotationColumns = Collections.emptySet();

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
}
