package org.hkijena.jipipe.extensions.utils.algorithms.datatable;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.utils.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Unpack data table", description = "Extracts data stored in the input slot into the output." +
        " If multiple tables are supplied, the rows are merged.")
@JIPipeInputSlot(value = JIPipeDataTable.class, slotName = "Table", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Data tables")
public class ExtractTableAlgorithm extends JIPipeParameterSlotAlgorithm {

    private boolean mergeAnnotations = true;
    private JIPipeTextAnnotationMergeMode mergeStrategy = JIPipeTextAnnotationMergeMode.Merge;

    public ExtractTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractTableAlgorithm(ExtractTableAlgorithm other) {
        super(other);
        this.mergeAnnotations = other.mergeAnnotations;
        this.mergeStrategy = other.mergeStrategy;
    }

    @Override
    protected boolean canPassThrough() {
        return false;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            JIPipeDataTable dataTable = getFirstInputSlot().getData(row, JIPipeDataTable.class,
                    progressInfo.resolve("Table", row, getFirstInputSlot().getRowCount()));

            for (int row2 = 0; row2 < dataTable.getRowCount(); row2++) {
                annotations.clear();
                annotations.addAll(dataTable.getTextAnnotations(row2));
                annotations.addAll(parameterAnnotations);
                if (mergeAnnotations) {
                    annotations.addAll(getFirstInputSlot().getTextAnnotations(row));
                }
                getFirstOutputSlot().addData(dataTable.getDataItemStore(row2),
                        annotations,
                        mergeStrategy,
                        dataTable.getDataAnnotations(row2),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        progressInfo);
            }

        }
    }

    @JIPipeDocumentation(name = "Merge outside annotations", description = "If enabled, table annotations are merged into the data.")
    @JIPipeParameter("merge-annotations")
    public boolean isMergeAnnotations() {
        return mergeAnnotations;
    }

    @JIPipeParameter("merge-annotations")
    public void setMergeAnnotations(boolean mergeAnnotations) {
        this.mergeAnnotations = mergeAnnotations;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how table annotations are merged into the data.")
    @JIPipeParameter("merge-strategy")
    public JIPipeTextAnnotationMergeMode getMergeStrategy() {
        return mergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setMergeStrategy(JIPipeTextAnnotationMergeMode mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }
}
