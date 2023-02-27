package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentVertexVariableSource implements ExpressionParameterVariableSource {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("UUID", "The unique ID of the vertex", "uuid"));
        VARIABLES.add(new ExpressionParameterVariable("X", "The X location", "x"));
        VARIABLES.add(new ExpressionParameterVariable("Y", "The Y location", "y"));
        VARIABLES.add(new ExpressionParameterVariable("Z", "The Z location", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Channel", "The channel (c) location", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Frame", "The frame (t) location", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Thickness", "The thickness of the vertex", "thickness"));
        VARIABLES.add(new ExpressionParameterVariable("Intensity", "The intensity of the vertex", "intensity"));
        VARIABLES.add(new ExpressionParameterVariable("Degree", "The degree (number of edges) of the vertex", "degree"));
    }

    public static void writeToVariables(Filaments3DData graph, FilamentVertex vertex, ExpressionVariables variables, String prefix) {
        graph.measureVertex(vertex, variables, prefix);
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
