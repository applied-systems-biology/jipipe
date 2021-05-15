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

package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;

import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Get all variable names", description = "Returns all variable names as array. The names are ordered.")
public class GetVariableKeysFunction extends ExpressionFunction {

    public GetVariableKeysFunction() {
        super("GET_ALL_VARIABLE_NAMES", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return variables.keySet().stream().sorted().collect(Collectors.toList());
    }
}
