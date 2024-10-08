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

package org.hkijena.jipipe.plugins.expressions.custom;

import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

import java.util.HashMap;
import java.util.Map;

/**
 * A parameter collection that allows the creation of custom expression variables
 * Please note that the {@link org.hkijena.jipipe.api.parameters.JIPipeParameter} annotation should have {@link JIPipeParameterSerializationMode} set to 'NestedCollection'. Otherwise, user-defined parameters will not be saved!
 */
public class JIPipeCustomExpressionVariablesParameter extends JIPipeDynamicParameterCollection {

    public JIPipeCustomExpressionVariablesParameter() {
        super(true);
    }

    public JIPipeCustomExpressionVariablesParameter(AbstractJIPipeParameterCollection target) {
        super(true);
        target.registerSubParameter(this);
    }

    public JIPipeCustomExpressionVariablesParameter(JIPipeCustomExpressionVariablesParameter other) {
        super(other);
    }

    public JIPipeCustomExpressionVariablesParameter(JIPipeCustomExpressionVariablesParameter other, AbstractJIPipeParameterCollection target) {
        super(other);
        target.registerSubParameter(this);
    }

    /**
     * Writes the parameter values to variables
     *
     * @param variables       the target
     * @param asVariables     write as variables (accessible via the key)
     * @param variablesPrefix prefix for the variables
     * @param toMap           if a map should be created (accessible via the key)
     * @param mapName         the name of the map
     */
    public void writeToVariables(JIPipeExpressionVariablesMap variables, boolean asVariables, String variablesPrefix, boolean toMap, String mapName) {
        if (asVariables) {
            for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
                variables.set(variablesPrefix + entry.getKey(), entry.getValue().get(Object.class));
            }
        }
        if (toMap) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
                map.put(entry.getKey(), entry.getValue().get(Object.class));
            }
            variables.set(mapName, map);
        }
    }

    /**
     * Writes the parameter values to variables with default configuration
     *
     * @param variables the target
     */
    public void writeToVariables(JIPipeExpressionVariablesMap variables) {
        writeToVariables(variables, true, "custom.", true, "custom");
    }
}
