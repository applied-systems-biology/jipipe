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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate data with table values", description = "Annotates the incoming data with values from the input table")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", description = "The data that should be annotated", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Table", description = "The table that contains the data", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Annotated data", description = "The annotated data", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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

    @SetJIPipeDocumentation(name = "Annotation merge mode", description = "Determines how generated annotations are merged with existing annotations")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "List of annotations to be created. Each is provided with two expressions: one for generating the name, and one for generating the value.\n\n" +
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

        @SetJIPipeDocumentation(name = "Name")
        @JIPipeParameter("name")
        @AddJIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @AddJIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }

        @SetJIPipeDocumentation(name = "Value")
        @JIPipeParameter("value")
        @AddJIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the specified column as list", key = "")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @AddJIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public JIPipeExpressionParameter getAnnotationValue() {
            return annotationValue;
        }

        @JIPipeParameter("value")
        public void setAnnotationValue(JIPipeExpressionParameter annotationValue) {
            this.annotationValue = annotationValue;
        }
    }
}
