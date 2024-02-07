package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.TrackFeature;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrackSpotFeatureVariablesInfo implements ExpressionParameterVariablesInfo {

    private static Set<ExpressionParameterVariable> VARIABLES;
    private static BiMap<String, String> SPOT_KEY_TO_VARIABLE_MAP = HashBiMap.create();
    private static BiMap<String, String> TRACK_KEY_TO_VARIABLE_MAP = HashBiMap.create();

    public static void initializeVariablesIfNeeded() {
        if (VARIABLES == null) {
            VARIABLES = new HashSet<>();
            for (Map.Entry<String, String> entry : TrackFeature.VALUE_LABELS.entrySet()) {
                String key = entry.getKey();
                String name = entry.getValue();
                String variableName = "track." + key.toLowerCase();
                VARIABLES.add(new ExpressionParameterVariable(name, "The TrackMate " + key + " track feature", variableName));
                TRACK_KEY_TO_VARIABLE_MAP.put(key, variableName);
            }
            for (Map.Entry<String, String> entry : SpotFeature.VALUE_LABELS.entrySet()) {
                String key = entry.getKey();
                String name = entry.getValue();
                String variableName = "spot." + key.toLowerCase();
                VARIABLES.add(new ExpressionParameterVariable(name, "The TrackMate " + key + " spot feature", variableName));
                SPOT_KEY_TO_VARIABLE_MAP.put(key, variableName);
            }
        }
    }

    public static String spotKeyToVariable(String key) {
        initializeVariablesIfNeeded();
        return SPOT_KEY_TO_VARIABLE_MAP.get(key);
    }

    public static String spotVariableToKey(String variableName) {
        initializeVariablesIfNeeded();
        return SPOT_KEY_TO_VARIABLE_MAP.inverse().get(variableName);
    }

    public static String trackKeyToVariable(String key) {
        initializeVariablesIfNeeded();
        return TRACK_KEY_TO_VARIABLE_MAP.get(key);
    }

    public static String trackVariableToKey(String variableName) {
        initializeVariablesIfNeeded();
        return TRACK_KEY_TO_VARIABLE_MAP.inverse().get(variableName);
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        initializeVariablesIfNeeded();
        return VARIABLES;
    }
}
