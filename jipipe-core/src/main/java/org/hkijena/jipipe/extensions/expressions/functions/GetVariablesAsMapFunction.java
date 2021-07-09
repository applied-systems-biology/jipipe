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
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.HashMap;
import java.util.List;

@JIPipeDocumentation(name = "Get all variables as map", description = "Returns all variables at map.")
public class GetVariablesAsMapFunction extends ExpressionFunction {

    public GetVariablesAsMapFunction() {
        super("GET_VARIABLES_MAP", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return new HashMap<>(variables);
    }
}
