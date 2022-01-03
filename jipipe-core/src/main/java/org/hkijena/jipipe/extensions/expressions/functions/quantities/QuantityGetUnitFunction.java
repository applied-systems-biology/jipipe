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

package org.hkijena.jipipe.extensions.expressions.functions.quantities;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.extensions.parameters.quantities.Quantity;

import java.util.List;

@JIPipeDocumentation(name = "Get quantity unit", description = "Parses a quantity string '[value] [unit]' and extracts the unit as string.")
public class QuantityGetUnitFunction extends ExpressionFunction {

    public QuantityGetUnitFunction() {
        super("QUANTITY_GET_UNIT", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("quantity", "Quantity string e.g., 1 µm or 10s");
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        Object value = parameters.get(0);
        Quantity quantity = Quantity.parse(value.toString());
        return quantity.getUnit();
    }
}
