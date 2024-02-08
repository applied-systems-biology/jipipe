package org.hkijena.jipipe.extensions.tables.nodes.annotations;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate data with table values", description = "Annotates the incoming data with values from the input table")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", description = "The data that should be annotated", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Table", description = "The table that contains the data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", description = "The annotated data", autoCreate = true)
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
public class AnnotateDataWithTableValues extends JIPipeIteratingAlgorithm {

    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;

    private ParameterCollectionList generatedAnnotations = ParameterCollectionList.containingCollection(AnnotationSettings.class);

    public AnnotateDataWithTableValues(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateDataWithTableValues(AnnotateDataWithTableValues other) {
        super(other);
        this.annotationMergeMode = other.annotationMergeMode;
        this.generatedAnnotations = new ParameterCollectionList(other.generatedAnnotations);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JIPipeData data = iterationStep.getInputData("Data", JIPipeData.class, progressInfo);
        ResultsTableData tableData = iterationStep.getInputData("Table", ResultsTableData.class, progressInfo);

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (int col = 0; col < tableData.getColumnCount(); col++) {
            variables.set(tableData.getColumnName(col), tableData.getColumnReference(col).getDataAsObjectList());
        }

        for (AnnotationSettings annotationSettings : generatedAnnotations.mapToCollection(AnnotationSettings.class)) {
            String name = annotationSettings.annotationName.evaluateToString(variables);
            String value = annotationSettings.annotationValue.evaluateToString(variables);
            annotationList.add(new JIPipeTextAnnotation(name, value));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation merge mode", description = "Determines how generated annotations are merged with existing annotations")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "List of annotations to be created. Each is provided with two expressions: one for generating the name, and one for generating the value.\n\n" +
            "Both the name and value expressions have access to annotations and variables that correspond to the table columns of the input table.")
    @JIPipeParameter(value = "generated-annotations", important = true)
    @ParameterCollectionListTemplate(AnnotationSettings.class)
    public ParameterCollectionList getGeneratedAnnotations() {
        return generatedAnnotations;
    }

    @JIPipeParameter("generated-annotations")
    public void setGeneratedAnnotations(ParameterCollectionList generatedAnnotations) {
        this.generatedAnnotations = generatedAnnotations;
    }

    public static class AnnotationSettings extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation\"");
        private JIPipeExpressionParameter annotationValue = new JIPipeExpressionParameter("");

        @JIPipeDocumentation(name = "Name")
        @JIPipeParameter("name")
        @JIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @JIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }

        @JIPipeDocumentation(name = "Value")
        @JIPipeParameter("value")
        @JIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @JIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public JIPipeExpressionParameter getAnnotationValue() {
            return annotationValue;
        }

        @JIPipeParameter("value")
        public void setAnnotationValue(JIPipeExpressionParameter annotationValue) {
            this.annotationValue = annotationValue;
        }
    }
}
