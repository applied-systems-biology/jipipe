package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentUnconnectedEdgeVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Length", "The length of the edge", "length"));

        FilamentVertexVariablesInfo source = new FilamentVertexVariablesInfo();
        for (ExpressionParameterVariable variable : source.getVariables(null, null)) {
            VARIABLES.add(new ExpressionParameterVariable("Source " + variable.getName(), variable.getDescription(), "source." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("Target " + variable.getName(), variable.getDescription(), "target." + variable.getKey()));
        }
    }

    public static void writeToVariables(Filaments3DData graph, FilamentVertex source, FilamentVertex target, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureVertex(source, variables, prefix + "source.");
        graph.measureVertex(target, variables, prefix + "target.");
        variables.set("length", source.getSpatialLocation().distanceTo(target.getSpatialLocation()));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
