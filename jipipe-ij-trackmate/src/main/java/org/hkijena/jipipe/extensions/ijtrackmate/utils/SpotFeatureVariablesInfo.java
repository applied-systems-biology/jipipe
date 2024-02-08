package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpotFeatureVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final BiMap<String, String> KEY_TO_VARIABLE_MAP = HashBiMap.create();
    private static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    public static void initializeVariablesIfNeeded() {
        if (VARIABLES == null) {
            VARIABLES = new HashSet<>();
            for (Map.Entry<String, String> entry : SpotFeature.VALUE_LABELS.entrySet()) {
                String key = entry.getKey();
                String name = entry.getValue();
                String variableName = key.toLowerCase();
                VARIABLES.add(new JIPipeExpressionParameterVariableInfo(variableName, name, "The TrackMate " + key + " spot feature"));
                VARIABLES.add(new JIPipeExpressionParameterVariableInfo("all." + variableName, "All " + name, "All values of TrackMate " + key + " spot feature"));
                KEY_TO_VARIABLE_MAP.put(key, variableName);
            }
        }
    }

    public static String keyToVariable(String key) {
        initializeVariablesIfNeeded();
        return KEY_TO_VARIABLE_MAP.get(key);
    }

    public static String variableToKey(String variableName) {
        initializeVariablesIfNeeded();
        return KEY_TO_VARIABLE_MAP.inverse().get(variableName);
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        initializeVariablesIfNeeded();
        return VARIABLES;
    }
}
