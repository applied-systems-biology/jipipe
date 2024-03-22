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

package org.hkijena.jipipe.extensions.tables.nodes.annotations;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with table properties", description = "Annotates tables with information about the table")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For tables")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class AnnotateByTablePropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private OptionalTextAnnotationNameParameter rowCountAnnotation = new OptionalTextAnnotationNameParameter("Num Rows", true);
    private OptionalTextAnnotationNameParameter columnCountAnnotation = new OptionalTextAnnotationNameParameter("Num Columns", true);
    private OptionalTextAnnotationNameParameter columnNamesAnnotation = new OptionalTextAnnotationNameParameter("Column names", false);

    public AnnotateByTablePropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateByTablePropertiesAlgorithm(AnnotateByTablePropertiesAlgorithm other) {
        super(other);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.rowCountAnnotation = new OptionalTextAnnotationNameParameter(other.rowCountAnnotation);
        this.columnCountAnnotation = new OptionalTextAnnotationNameParameter(other.columnCountAnnotation);
        this.columnNamesAnnotation = new OptionalTextAnnotationNameParameter(other.columnNamesAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        rowCountAnnotation.addAnnotationIfEnabled(annotationList, "" + data.getRowCount());
        columnCountAnnotation.addAnnotationIfEnabled(annotationList, "" + data.getColumnCount());
        if (columnNamesAnnotation.isEnabled())
            columnNamesAnnotation.addAnnotationIfEnabled(annotationList, JsonUtils.toJsonString(data.getColumnNames()));

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how the newly generated annotations are merged with existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Annotate with row count", description = "If enabled, annotate with the row count")
    @JIPipeParameter("row-count-annotation")
    public OptionalTextAnnotationNameParameter getRowCountAnnotation() {
        return rowCountAnnotation;
    }

    @JIPipeParameter("row-count-annotation")
    public void setRowCountAnnotation(OptionalTextAnnotationNameParameter rowCountAnnotation) {
        this.rowCountAnnotation = rowCountAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with column count", description = "If enabled, annotate with the column count")
    @JIPipeParameter("column-count-annotation")
    public OptionalTextAnnotationNameParameter getColumnCountAnnotation() {
        return columnCountAnnotation;
    }

    @JIPipeParameter("column-count-annotation")
    public void setColumnCountAnnotation(OptionalTextAnnotationNameParameter columnCountAnnotation) {
        this.columnCountAnnotation = columnCountAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with column names", description = "If enabled, annotate with the column names as JSON string")
    @JIPipeParameter("column-names-annotation")
    public OptionalTextAnnotationNameParameter getColumnNamesAnnotation() {
        return columnNamesAnnotation;
    }

    @JIPipeParameter("column-names-annotation")
    public void setColumnNamesAnnotation(OptionalTextAnnotationNameParameter columnNamesAnnotation) {
        this.columnNamesAnnotation = columnNamesAnnotation;
    }
}
