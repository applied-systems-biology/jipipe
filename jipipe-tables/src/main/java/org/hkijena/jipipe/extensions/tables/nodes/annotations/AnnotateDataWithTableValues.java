package org.hkijena.jipipe.extensions.tables.nodes.annotations;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Annotate data with table values", description = "Annotates the incoming data with values from the input table")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", description = "The data that should be annotated", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Table", description = "The table that contains the data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", description = "The annotated data", inheritedSlot = "Data", autoCreate = true)
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataBatch.getInputData("Data", JIPipeData.class, progressInfo);
        ResultsTableData tableData = dataBatch.getInputData("Table", ResultsTableData.class, progressInfo);

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (int col = 0; col < tableData.getColumnCount(); col++) {
            variables.set(tableData.getColumnName(col), tableData.getColumnReference(col).getDataAsObjectList());
        }

        for (AnnotationSettings annotationSettings : generatedAnnotations.mapToCollection(AnnotationSettings.class)) {
            String name = annotationSettings.annotationName.evaluateToString(variables);
            String value = annotationSettings.annotationValue.evaluateToString(variables);
            annotationList.add(new JIPipeTextAnnotation(name, value));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
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
    public ParameterCollectionList getGeneratedAnnotations() {
        return generatedAnnotations;
    }

    @JIPipeParameter("generated-annotations")
    public void setGeneratedAnnotations(ParameterCollectionList generatedAnnotations) {
        this.generatedAnnotations = generatedAnnotations;
    }

    public static class AnnotationSettings extends AbstractJIPipeParameterCollection {
        private DefaultExpressionParameter annotationName = new DefaultExpressionParameter("\"Annotation\"");
        private DefaultExpressionParameter annotationValue = new DefaultExpressionParameter("");

        @JIPipeDocumentation(name = "Name")
        @JIPipeParameter("name")
        @ExpressionParameterSettingsVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @ExpressionParameterSettingsVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public DefaultExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("name")
        public void setAnnotationName(DefaultExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }

        @JIPipeDocumentation(name = "Value")
        @JIPipeParameter("value")
        @ExpressionParameterSettingsVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @ExpressionParameterSettingsVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public DefaultExpressionParameter getAnnotationValue() {
            return annotationValue;
        }

        @JIPipeParameter("value")
        public void setAnnotationValue(DefaultExpressionParameter annotationValue) {
            this.annotationValue = annotationValue;
        }
    }
}
