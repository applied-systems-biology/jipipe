package org.hkijena.jipipe.extensions.parameters.api.enums;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.DefaultJIPipeParameterGenerator;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringSetParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EnumParameterGenerator extends DefaultJIPipeParameterGenerator {

    private DynamicStringSetParameter generatedValues = new DynamicStringSetParameter();

    @Override
    public <T> List<T> generate(JIPipeWorkbench workbench, Component parent, Class<T> klass) {
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
            Object enumValue = Enum.valueOf((Class<Enum>)klass, value);
            result.add((T) enumValue);
        }
        return result;
    }

    @JIPipeDocumentation(name = "Values", description = "The values that should be generated")
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
