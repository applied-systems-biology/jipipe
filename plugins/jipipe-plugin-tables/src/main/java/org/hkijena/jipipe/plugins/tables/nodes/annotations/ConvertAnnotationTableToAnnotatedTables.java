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

package org.hkijena.jipipe.plugins.tables.nodes.annotations;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.util.HashSet;
import java.util.Map;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Annotation table to annotated table", description = "Extracts annotation columns from an annotation table and " +
        "converts them into data annotations. All non-annotation items are stored in the resulting tables.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = AnnotationTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ConvertAnnotationTableToAnnotatedTables extends JIPipeSimpleIteratingAlgorithm {

    private boolean keepAnnotationColumns = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ConvertAnnotationTableToAnnotatedTables(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvertAnnotationTableToAnnotatedTables(ConvertAnnotationTableToAnnotatedTables other) {
        super(other);
        this.keepAnnotationColumns = other.keepAnnotationColumns;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        AnnotationTableData inputData = iterationStep.getInputData(getFirstInputSlot(), AnnotationTableData.class, progressInfo);
        HashSet<String> annotationColumns = new HashSet<>(inputData.getColumnNames());
        TableColumn mergedColumn = inputData.getMergedColumn(annotationColumns, ", ", "=");
        for (Map.Entry<String, ResultsTableData> entry : inputData.splitBy(mergedColumn).entrySet()) {
            ResultsTableData data = entry.getValue();
            AnnotationTableData annotationTableData = new AnnotationTableData(data);
            if (!keepAnnotationColumns) {
                data.removeColumns(annotationColumns);
            }
            iterationStep.addOutputData(getFirstOutputSlot(), data, annotationTableData.getAnnotations(0), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Keep annotation columns", description = "If enabled, annotation columns are copied into the result tables.")
    @JIPipeParameter("keep-annotation-columns")
    public boolean isKeepAnnotationColumns() {
        return keepAnnotationColumns;
    }

    @JIPipeParameter("keep-annotation-columns")
    public void setKeepAnnotationColumns(boolean keepAnnotationColumns) {
        this.keepAnnotationColumns = keepAnnotationColumns;
    }
}
