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
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.extensions.parameters.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Convert quantity", description = "Converts a quantity string '[value] [unit]' to another unit. Will throw an error if the unit is unknown. Supported are length, time, and weight units (metric).")
public class QuantityConvertFunction extends ExpressionFunction {

    /**
     * Contains factors to the standard unit
     * Length: m
     * Weight: g
     * Time: s
     */
    public static final Map<String, Double> UNITS_FACTORS = new HashMap<>();

    static  {
        UNITS_FACTORS.put("nm", 1e-9);
        UNITS_FACTORS.put("µm", 1e-6);
        UNITS_FACTORS.put("micron", 1e-6);
        UNITS_FACTORS.put("microns", 1e-6);
        UNITS_FACTORS.put("um", 1e-6);
        UNITS_FACTORS.put("mm", 0.001);
        UNITS_FACTORS.put("cm", 0.01);
        UNITS_FACTORS.put("dm", 0.1);
        UNITS_FACTORS.put("m", 1.0);
        UNITS_FACTORS.put("km", 1000.0);

        UNITS_FACTORS.put("ng", 1e-9);
        UNITS_FACTORS.put("µg", 1e-6);
        UNITS_FACTORS.put("ug", 1e-6);
        UNITS_FACTORS.put("mg", 0.001);
        UNITS_FACTORS.put("g", 1.0);
        UNITS_FACTORS.put("kg", 1000.0);
        UNITS_FACTORS.put("t", 1000000.0);

        UNITS_FACTORS.put("ns", 1e-9);
        UNITS_FACTORS.put("µs", 1e-6);
        UNITS_FACTORS.put("us", 1e-6);
        UNITS_FACTORS.put("ms", 0.001);
        UNITS_FACTORS.put("s", 1.0);
        UNITS_FACTORS.put("min", 60.0);
        UNITS_FACTORS.put("h", 3600.0);
        UNITS_FACTORS.put("d", 86400.0);
        UNITS_FACTORS.put("a", 3.154e+7);
    }

    public QuantityConvertFunction() {
        super("QUANTITY_CONVERT", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("quantity", "Quantity string e.g., 1 µm or 10s");
        }
        else if(index == 1) {
            return new ParameterInfo("target unit", "The target unit");
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        Object value = parameters.get(0);
        Quantity quantity = Quantity.parse(value.toString());
        
        double sourceFactor = UNITS_FACTORS.get(quantity.getUnit());
        double targetFactor = UNITS_FACTORS.get(StringUtils.nullToEmpty(parameters.get(1)));

        return ((quantity.getValue() * sourceFactor) / targetFactor) + " " + parameters.get(1);
    }
}
