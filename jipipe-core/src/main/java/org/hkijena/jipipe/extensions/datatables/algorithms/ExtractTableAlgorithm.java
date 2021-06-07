package org.hkijena.jipipe.extensions.datatables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTableData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Extract data table", description = "Extracts data stored in the input slot into the output." +
        " If multiple tables are supplied, the rows are merged.")
@JIPipeInputSlot(value = JIPipeDataTableData.class, slotName = "Table", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ExtractTableAlgorithm extends JIPipeParameterSlotAlgorithm {

    private boolean mergeAnnotations = true;
    private JIPipeAnnotationMergeStrategy mergeStrategy = JIPipeAnnotationMergeStrategy.Merge;

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
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            JIPipeDataTableData dataTableData = getFirstInputSlot().getData(row, JIPipeDataTableData.class,
                    progressInfo.resolve("Table", row, getFirstInputSlot().getRowCount()));

            for (int row2 = 0; row2 < dataTableData.getDataSlot().getRowCount(); row2++) {
                annotations.clear();
                annotations.addAll(dataTableData.getDataSlot().getAnnotations(row2));
                annotations.addAll(parameterAnnotations);
                if (mergeAnnotations) {
                    annotations.addAll(getFirstInputSlot().getAnnotations(row));
                }
                getFirstOutputSlot().addData(dataTableData.getDataSlot().getVirtualData(row2),
                        annotations,
                        mergeStrategy);
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
    public JIPipeAnnotationMergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setMergeStrategy(JIPipeAnnotationMergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }
}
