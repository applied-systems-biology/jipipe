package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;

import java.util.HashSet;
import java.util.Set;

public class FilamentVertexVariableSource implements ExpressionParameterVariableSource {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Centroid X", "The X location of the centroid", "cx"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid Y", "The Y location of the centroid", "cy"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid Z", "The Z location of the centroid", "cz"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid channel", "The channel (c) location of the centroid", "cc"));
        VARIABLES.add(new ExpressionParameterVariable("Centroid frame", "The frame (t) location of the centroid", "ct"));
        VARIABLES.add(new ExpressionParameterVariable("Thickness", "The thickness around the centroid", "thickness"));
        VARIABLES.add(new ExpressionParameterVariable("Degree", "The degree (number of edges) of the vertex", "degree"));
    }

    public static void writeToVariables(FilamentsData graph, FilamentVertex vertex, ExpressionVariables variables, String prefix) {
        variables.set(prefix + "cx", vertex.getCentroid().getX());
        variables.set(prefix + "cy", vertex.getCentroid().getY());
        variables.set(prefix + "cz", vertex.getCentroid().getZ());
        variables.set(prefix + "cc", vertex.getCentroid().getC());
        variables.set(prefix + "ct", vertex.getCentroid().getT());
        variables.set(prefix + "thickness", vertex.getThickness());
        variables.set(prefix + "degree", graph.degreeOf(vertex));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
