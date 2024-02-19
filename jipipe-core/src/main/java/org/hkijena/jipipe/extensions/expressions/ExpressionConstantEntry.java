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

import com.fathzer.soft.javaluator.Constant;
import com.fathzer.soft.javaluator.Operator;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.utils.DocumentationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizes {@link Operator} instances and puts them together with a name and description
 */
public class ExpressionConstantEntry {
    private final String name;
    private final String description;
    private final Constant constant;

    public ExpressionConstantEntry(Constant constant) {
        this.constant = constant;
        SetJIPipeDocumentation documentation = constant.getClass().getAnnotation(SetJIPipeDocumentation.class);
        if (documentation != null) {
            name = documentation.name();
            description = DocumentationUtils.getDocumentationDescription(documentation);
        } else {
            name = "Undefined";
            description = "";
        }
    }

    public ExpressionConstantEntry(String name, String description, Constant constant) {
        this.name = name;
        this.description = description;
        this.constant = constant;
    }

    /**
     * Extracts operators from an evaluator
     *
     * @param evaluator               the evaluator
     * @param onlyExpressionOperators if true, operators must inherit from {@link ExpressionOperator}
     * @return the list
     */
    public static List<ExpressionConstantEntry> fromEvaluator(ExpressionEvaluator evaluator, boolean onlyExpressionOperators) {
        List<ExpressionConstantEntry> result = new ArrayList<>();
        for (Constant constant : evaluator.getConstants()) {
            if (onlyExpressionOperators && !(constant instanceof ExpressionConstant))
                continue;
            result.add(new ExpressionConstantEntry(constant));
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Constant getConstant() {
        return constant;
    }
}
