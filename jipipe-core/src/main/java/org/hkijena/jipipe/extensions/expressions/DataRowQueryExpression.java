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

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Expression for selecting rows inside a data table
 */
@ExpressionParameterSettings(variableSource = DataRowQueryExpression.VariableSource.class)
@JIPipeDocumentationDescription(description = "This parameter allows you to select a subset of data via annotations. If you leave the " +
        "expression empty, all data will match. Otherwise, you can utilize variables that are named after the annotation columns. To test " +
        "for the existence of an annotation (i.e.., NA values), you can use the EXISTS operator.")
public class DataRowQueryExpression extends DefaultExpressionParameter {

    public DataRowQueryExpression() {
    }

    public DataRowQueryExpression(String expression) {
        super(expression);
    }

    public DataRowQueryExpression(DataRowQueryExpression other) {
        super(other);
    }

    /**
     * Returns true if the data contained in this slot matches the expression
     *
     * @param slot the data slot
     * @param row  data row index
     * @return if the expression matches
     */
    public boolean test(JIPipeDataSlot slot, int row) {
        return test(slot.getVirtualData(row), slot.getTextAnnotations(row));
    }

    /**
     * Returns matching rows
     *
     * @param slot the data slot
     * @return matching rows
     */
    public java.util.List<Integer> query(JIPipeDataSlot slot) {
        java.util.List<Integer> result = new ArrayList<>();
        for (int row = 0; row < slot.getRowCount(); row++) {
            if (test(slot, row))
                result.add(row);
        }
        return result;
    }

    /**
     * Returns matching rows
     *
     * @param slot the data slot
     * @param rows rows to be checked
     * @return matching rows
     */
    public java.util.List<Integer> query(JIPipeDataSlot slot, Collection<Integer> rows) {
        java.util.List<Integer> result = new ArrayList<>();
        for (int row : rows) {
            if (test(slot, row))
                result.add(row);
        }
        return result;
    }

    /**
     * Returns true if the data matches the expression
     *
     * @param data        the data
     * @param annotations the annotations
     * @return if the expression matches
     */
    private boolean test(JIPipeDataItemStore data, java.util.List<JIPipeTextAnnotation> annotations) {
        ExpressionVariables variables = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : annotations) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("data_string", data.getStringRepresentation());

        return test(variables);
    }

    /**
     * Returns true if the data matches the expression
     *
     * @param data        the data
     * @param annotations the annotations
     * @return if the expression matches
     */
    private boolean test(JIPipeData data, java.util.List<JIPipeTextAnnotation> annotations) {
        ExpressionVariables variables = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : annotations) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("data_string", data.toString());

        return test(variables);
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Data string", "The string representation of the data", "data_string"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
