package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * A parameter collection that allows the creation of custom expression variables
 * Please note that the {@link org.hkijena.jipipe.api.parameters.JIPipeParameter} annotation should have {@link org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence} set to 'NestedCollection'. Otherwise, user-defined parameters will not be saved!
 */
public class CustomExpressionVariablesParameter extends JIPipeDynamicParameterCollection {

    public CustomExpressionVariablesParameter() {
        super(true);
    }

    public CustomExpressionVariablesParameter(JIPipeGraphNode target) {
        super(true);
        target.registerSubParameter(this);
    }

    public CustomExpressionVariablesParameter(CustomExpressionVariablesParameter other) {
        super(other);
    }

    public CustomExpressionVariablesParameter(CustomExpressionVariablesParameter other, JIPipeGraphNode target) {
        super(other);
        target.registerSubParameter(this);
    }

    /**
     * Writes the parameter values to variables
     * @param variables the target
     * @param asVariables write as variables (accessible via the key)
     * @param variablesPrefix prefix for the variables
     * @param toMap if a map should be created (accessible via the key)
     * @param mapName the name of the map
     */
    public void writeToVariables(ExpressionVariables variables, boolean asVariables, String variablesPrefix, boolean toMap, String mapName) {
        if(asVariables) {
            for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
                variables.set(variablesPrefix + entry.getKey(), entry.getValue().get(Object.class));
            }
        }
        if(toMap) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
                map.put(entry.getKey(), entry.getValue().get(Object.class));
            }
            variables.set(mapName, map);
        }
    }
}
