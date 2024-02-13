package org.hkijena.jipipe.extensions.ijfilaments.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdgeVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class EdgeMaskParameter extends AbstractJIPipeParameterCollection {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public EdgeMaskParameter() {
    }

    public EdgeMaskParameter(EdgeMaskParameter other) {

        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @JIPipeDocumentation(name = "Edge mask", description = "Apply the operation to a specific set of edges")
    @JIPipeParameter("mask")
    @JIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per edge")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("mask")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    public Set<FilamentEdge> filter(Filaments3DData graph, Set<FilamentEdge> edgeSet, JIPipeExpressionVariablesMap variables) {
        Set<FilamentEdge> matched = new HashSet<>();
        for (FilamentEdge filamentEdge : edgeSet) {
            FilamentEdgeVariablesInfo.writeToVariables(graph, filamentEdge, variables, "");
            if(filter.test(variables)) {
                matched.add(filamentEdge);
            }
        }
        return matched;
    }
}
