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

import com.fathzer.soft.javaluator.Operator;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.utils.DocumentationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizes {@link Operator} instances and puts them together with a name and description
 */
public class ExpressionOperatorEntry {
    private final String name;
    private final String description;
    private final Operator operator;

    public ExpressionOperatorEntry(Operator operator) {
        this.operator = operator;
        JIPipeDocumentation documentation = operator.getClass().getAnnotation(JIPipeDocumentation.class);
        if (documentation != null) {
            name = documentation.name();
            description = DocumentationUtils.getDocumentationDescription(documentation);
        } else {
            name = "Undefined";
            description = "";
        }
    }

    public ExpressionOperatorEntry(String name, String description, Operator operator) {
        this.name = name;
        this.description = description;
        this.operator = operator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Operator getOperator() {
        return operator;
    }

    public ParameterInfo getParameterInfo(int index) {
        if (operator instanceof ExpressionOperator)
            return ((ExpressionOperator) operator).getParameterInfo(index);
        else {
            switch (index) {
                case 0:
                    return new ParameterInfo("x", "");
                case 1:
                    return new ParameterInfo("y", "");
                default:
                    return null;
            }
        }
    }

    public String getSignature() {
        if (operator.getOperandCount() == 2) {
            return "x " + operator.getSymbol() + " y";
        } else if (operator.getAssociativity() == Operator.Associativity.LEFT) {
            return "x " + operator.getSymbol();
        } else {
            return operator.getSymbol() + " x";
        }
    }

    public String getTemplate() {
        if (operator.getOperandCount() == 2) {
            return "() " + operator.getSymbol() + " ()";
        } else {
            return operator.getSymbol() + " ()";
        }
    }

    /**
     * Extracts operators from an evaluator
     *
     * @param evaluator               the evaluator
     * @param onlyExpressionOperators if true, operators must inherit from {@link ExpressionOperator}
     * @return the list
     */
    public static List<ExpressionOperatorEntry> fromEvaluator(ExpressionEvaluator evaluator, boolean onlyExpressionOperators) {
        List<ExpressionOperatorEntry> result = new ArrayList<>();
        for (Operator operator : evaluator.getOperators()) {
            if (onlyExpressionOperators && !(operator instanceof ExpressionOperator))
                continue;
            result.add(new ExpressionOperatorEntry(operator));
        }
        return result;
    }
}
