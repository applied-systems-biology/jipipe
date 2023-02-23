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
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;

import java.util.List;

@JIPipeDocumentation(name = "Convert quantity", description = "Converts a quantity string '[value] [unit]' to another unit. Will throw an error if the unit is unknown. Supported are length, time, and weight units (metric).")
public class QuantityConvertFunction extends ExpressionFunction {


    public QuantityConvertFunction() {
        super("QUANTITY_CONVERT", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("quantity", "Quantity string e.g., 1 µm or 10s");
        } else if (index == 1) {
            return new ParameterInfo("target unit", "The target unit");
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        Object value = parameters.get(0);
        Quantity quantity = Quantity.parse(value.toString());

        if (quantity == null) {
            throw new RuntimeException("Invalid quantity: '" + value + "'. Must be [number] [unit]");
        }

        Quantity outputQuantity = quantity.convertTo(parameters.get(1) + "");
        return outputQuantity.getValue() + " " + outputQuantity.getUnit();
    }
}
