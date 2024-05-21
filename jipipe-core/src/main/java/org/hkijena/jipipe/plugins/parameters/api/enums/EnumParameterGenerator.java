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

package org.hkijena.jipipe.plugins.parameters.api.enums;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.DefaultJIPipeParameterGenerator;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.primitives.DynamicStringSetParameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EnumParameterGenerator extends DefaultJIPipeParameterGenerator {

    private DynamicStringSetParameter generatedValues = new DynamicStringSetParameter();

    @Override
    public <T> List<T> generate(JIPipeDesktopWorkbench workbench, Component parent, Class<T> klass) {
        // Set to enum properties before generating
        T[] enumConstants = klass.getEnumConstants();
        List<String> allowedValues = new ArrayList<>();
        for (T enumConstant : enumConstants) {
            allowedValues.add(enumConstant + "");
        }
        generatedValues.setAllowedValues(allowedValues);
        generatedValues.setValues(new HashSet<>(allowedValues));

        return super.generate(workbench, parent, klass);
    }

    @Override
    public <T> List<T> generateAfterDialog(JIPipeWorkbench workbench, Component parent, Class<T> klass) {
        List<T> result = new ArrayList<>();
        for (String value : generatedValues.getValues()) {
            Object enumValue = Enum.valueOf((Class<Enum>) klass, value);
            result.add((T) enumValue);
        }
        return result;
    }

    @SetJIPipeDocumentation(name = "Values", description = "The values that should be generated")
    @JIPipeParameter("generated-values")
    public DynamicStringSetParameter getGeneratedValues() {
        return generatedValues;
    }

    @JIPipeParameter("generated-values")
    public void setGeneratedValues(DynamicStringSetParameter generatedValues) {
        this.generatedValues = generatedValues;
    }

    @Override
    public String getName() {
        return "Enum values";
    }

    @Override
    public String getDescription() {
        return "Generates values that ";
    }
}
