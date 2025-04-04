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

package org.hkijena.jipipe.plugins.ijtrackmate.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.TrackFeature;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TrackFeatureVariablesInfo implements JIPipeExpressionVariablesInfo {

    private static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;
    private static BiMap<String, String> KEY_TO_VARIABLE_MAP = HashBiMap.create();

    public static void initializeVariablesIfNeeded() {
        if (VARIABLES == null) {
            VARIABLES = new HashSet<>();
            for (Map.Entry<String, String> entry : TrackFeature.VALUE_LABELS.entrySet()) {
                String key = entry.getKey();
                String name = entry.getValue();
                String variableName = key.toLowerCase(Locale.ROOT);
                VARIABLES.add(new JIPipeExpressionParameterVariableInfo(variableName, name, "The TrackMate " + key + " track feature"));
                VARIABLES.add(new JIPipeExpressionParameterVariableInfo("all." + variableName, "All " + name, "All values of TrackMate " + key + " track feature"));
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
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        initializeVariablesIfNeeded();
        return VARIABLES;
    }
}
