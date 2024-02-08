package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

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
