package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentEdgeVariableSource implements ExpressionParameterVariableSource {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("UUID", "The unique ID of the vertex", "uuid"));
        VARIABLES.add(new ExpressionParameterVariable("Length", "The length of the edge", "length"));

        VARIABLES.add(new ExpressionParameterVariable("Source UUID", "The unique ID of the vertex", "source.uuid"));
        VARIABLES.add(new ExpressionParameterVariable("Source centroid X", "The X location of the centroid", "source.x"));
        VARIABLES.add(new ExpressionParameterVariable("Source centroid Y", "The Y location of the centroid", "source.y"));
        VARIABLES.add(new ExpressionParameterVariable("Source centroid Z", "The Z location of the centroid", "source.z"));
        VARIABLES.add(new ExpressionParameterVariable("Source centroid channel", "The channel (c) location of the centroid", "source.c"));
        VARIABLES.add(new ExpressionParameterVariable("Source centroid frame", "The frame (t) location of the centroid", "source.t"));
        VARIABLES.add(new ExpressionParameterVariable("Source thickness", "The thickness around the centroid", "source.thickness"));
        VARIABLES.add(new ExpressionParameterVariable("Source degree", "The degree (number of edges) of the vertex", "source.degree"));

        VARIABLES.add(new ExpressionParameterVariable("Target UUID", "The unique ID of the vertex", "target.uuid"));
        VARIABLES.add(new ExpressionParameterVariable("Target centroid X", "The X location of the centroid", "target.x"));
        VARIABLES.add(new ExpressionParameterVariable("Target centroid Y", "The Y location of the centroid", "target.y"));
        VARIABLES.add(new ExpressionParameterVariable("Target centroid Z", "The Z location of the centroid", "target.z"));
        VARIABLES.add(new ExpressionParameterVariable("Target centroid channel", "The channel (c) location of the centroid", "target.c"));
        VARIABLES.add(new ExpressionParameterVariable("Target centroid frame", "The frame (t) location of the centroid", "target.t"));
        VARIABLES.add(new ExpressionParameterVariable("Target thickness", "The thickness around the centroid", "target.thickness"));
        VARIABLES.add(new ExpressionParameterVariable("Target degree", "The degree (number of edges) of the vertex", "target.degree"));
    }

    public static void writeToVariables(Filaments3DData graph, FilamentEdge edge, ExpressionVariables variables, String prefix) {
        graph.measureEdge(edge, variables, prefix);
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
