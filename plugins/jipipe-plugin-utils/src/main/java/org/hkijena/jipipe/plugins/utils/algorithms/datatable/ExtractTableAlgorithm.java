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

package org.hkijena.jipipe.plugins.utils.algorithms.datatable;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Unpack data table", description = "Extracts data stored in the input slot into the output." +
        " If multiple tables are supplied, the rows are merged.")
@AddJIPipeInputSlot(value = JIPipeDataTable.class, name = "Table", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Data", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Data tables")
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
    public boolean canPassThrough() {
        return false;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
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
                        dataTable.getDataContext(row2).branch(this),
                        progressInfo);
            }

        }
    }

    @SetJIPipeDocumentation(name = "Merge outside annotations", description = "If enabled, table annotations are merged into the data.")
    @JIPipeParameter("merge-annotations")
    public boolean isMergeAnnotations() {
        return mergeAnnotations;
    }

    @JIPipeParameter("merge-annotations")
    public void setMergeAnnotations(boolean mergeAnnotations) {
        this.mergeAnnotations = mergeAnnotations;
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how table annotations are merged into the data.")
    @JIPipeParameter("merge-strategy")
    public JIPipeTextAnnotationMergeMode getMergeStrategy() {
        return mergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setMergeStrategy(JIPipeTextAnnotationMergeMode mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }
}
