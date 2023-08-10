/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Set single annotation", description = "Sets a single annotation")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class SetSingleAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression annotationValue = new StringQueryExpression();
    private StringQueryExpression annotationName = new StringQueryExpression();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * @param info the info
     */
    public SetSingleAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SetSingleAnnotation(SetSingleAnnotation other) {
        super(other);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.annotationName = new StringQueryExpression(other.annotationName);
        this.annotationValue = new StringQueryExpression(other.annotationValue);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());
        variableSet.set("data_string", getFirstInputSlot().getDataItemStore(dataBatch.getInputSlotRows().get(getFirstInputSlot())).getStringRepresentation());
        variableSet.set("data_type", JIPipe.getDataTypes().getIdOf(getFirstInputSlot().getDataItemStore(dataBatch.getInputSlotRows().get(getFirstInputSlot())).getDataClass()));
        variableSet.set("row", dataBatch.getInputSlotRows().get(getFirstInputSlot()));
        variableSet.set("num_rows", getFirstInputSlot().getRowCount());
        String name = StringUtils.nullToEmpty(annotationName.generate(variableSet));
        String value = StringUtils.nullToEmpty(annotationValue.generate(variableSet));
        dataBatch.addMergedTextAnnotation(new JIPipeTextAnnotation(name, value), annotationMergeStrategy);
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation value", description = "The value of the generated annotation. ")
    @JIPipeParameter("annotation-value")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Project directory", description = "The project directory (if available; will be the same as the data directory otherwise)", key = "project_dir")
    @ExpressionParameterSettingsVariable(name = "Project data directories", description = "The user-configured project data directories as map. Access entries by the key.", key = "project_data_dirs")
    public StringQueryExpression getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("annotation-value")
    public void setAnnotationValue(StringQueryExpression annotationValue) {
        this.annotationValue = annotationValue;
    }

    @JIPipeDocumentation(name = "Annotation name", description = "The name of the generated annotation. ")
    @JIPipeParameter("annotation-name")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public StringQueryExpression getAnnotationName() {
        return annotationName;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationName(StringQueryExpression annotationName) {
        this.annotationName = annotationName;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Data string",
                    "The data stored as string",
                    "data_string"));
            VARIABLES.add(new ExpressionParameterVariable("Data type ID",
                    "The data type ID",
                    "data_type"));
            VARIABLES.add(new ExpressionParameterVariable("Row",
                    "The row inside the data table",
                    "row"));
            VARIABLES.add(new ExpressionParameterVariable("Number of rows",
                    "The number of rows in the data table",
                    "num_rows"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
