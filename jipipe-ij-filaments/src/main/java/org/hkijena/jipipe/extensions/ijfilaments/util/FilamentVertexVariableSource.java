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
        VARIABLES.add(new ExpressionParameterVariable("Centroid X", "The X location of the centroid", "x"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid Y", "The Y location of the centroid", "y"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid Z", "The Z location of the centroid", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid channel", "The channel (c) location of the centroid", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid frame", "The frame (t) location of the centroid", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Thickness", "The thickness around the centroid", "thickness"));
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
