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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;

import java.util.HashSet;
import java.util.Set;

public class FilamentEndpointsVariablesInfo implements JIPipeExpressionVariablesInfo {

    public static void writeToVariables(Filaments3DGraphData graph, FilamentVertex source, FilamentVertex target, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureVertex(source, variables, prefix + "first.");
        graph.measureVertex(target, variables, prefix + "second.");

        variables.set("distance", source.getSpatialLocation().distanceTo(target.getSpatialLocation()));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("distance", "Distance", "The distance between the points coordinates"));

        FilamentVertexVariablesInfo source = new FilamentVertexVariablesInfo();
        for (JIPipeExpressionParameterVariableInfo variable : source.getVariables(workbench, null, null)) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("first." + variable.getKey(), "First " + variable.getName(), variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("second." + variable.getKey(), "Second " + variable.getName(), variable.getDescription()));
        }

        return VARIABLES;
    }
}
