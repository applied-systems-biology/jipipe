package org.hkijena.jipipe.extensions.ijfilaments.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class VertexMaskParameter extends AbstractJIPipeParameterCollection {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public VertexMaskParameter() {
    }

    public VertexMaskParameter(VertexMaskParameter other) {

        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @JIPipeDocumentation(name = "Vertex mask", description = "Apply the operation to a specific set of vertices")
    @JIPipeParameter("mask")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("mask")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    public Set<FilamentVertex> filter(Filaments3DData graph, Set<FilamentVertex> vertexSet, JIPipeExpressionVariablesMap variables) {
        Set<FilamentVertex> matched = new HashSet<>();
        for (FilamentVertex vertex : vertexSet) {
            // Write variables
            FilamentVertexVariablesInfo.writeToVariables(graph, vertex, variables, "");
            if (filter.test(variables)) {
                matched.add(vertex);
            }
        }

        return matched;
    }
}
