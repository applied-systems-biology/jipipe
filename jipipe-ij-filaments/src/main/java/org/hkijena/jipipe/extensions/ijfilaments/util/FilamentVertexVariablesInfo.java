package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentVertexVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("UUID", "The unique ID of the vertex", "uuid"));
        VARIABLES.add(new ExpressionParameterVariable("X", "The X location", "x"));
        VARIABLES.add(new ExpressionParameterVariable("Y", "The Y location", "y"));
        VARIABLES.add(new ExpressionParameterVariable("Z", "The Z location", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Channel", "The channel (c) location", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Frame", "The frame (t) location", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Radius", "The radius of the vertex", "radius"));
        VARIABLES.add(new ExpressionParameterVariable("Value", "The value of the vertex", "value"));
        VARIABLES.add(new ExpressionParameterVariable("Degree", "The degree (number of edges) of the vertex", "degree"));
        VARIABLES.add(new ExpressionParameterVariable("Physical voxel size (X)", "Size of a voxel (X)", "vsx"));
        VARIABLES.add(new ExpressionParameterVariable("Physical voxel size (Y)", "Size of a voxel (Y)", "vsy"));
        VARIABLES.add(new ExpressionParameterVariable("Physical voxel size (Z)", "Size of a voxel (Z)", "vsz"));
    }

    public static void writeToVariables(Filaments3DData graph, FilamentVertex vertex, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureVertex(vertex, variables, prefix);
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
