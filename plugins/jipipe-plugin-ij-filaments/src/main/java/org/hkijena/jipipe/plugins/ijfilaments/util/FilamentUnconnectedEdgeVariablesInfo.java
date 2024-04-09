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

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentUnconnectedEdgeVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("length", "Length", "The length of the edge"));

        FilamentVertexVariablesInfo source = new FilamentVertexVariablesInfo();
        for (JIPipeExpressionParameterVariableInfo variable : source.getVariables(null, null)) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source." + variable.getKey(), "Source " + variable.getName(), variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target." + variable.getKey(), "Target " + variable.getName(), variable.getDescription()));
        }
    }

    public static void writeToVariables(Filaments3DData graph, FilamentVertex source, FilamentVertex target, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureVertex(source, variables, prefix + "source.");
        graph.measureVertex(target, variables, prefix + "target.");
        variables.set("length", source.getSpatialLocation().distanceTo(target.getSpatialLocation()));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
