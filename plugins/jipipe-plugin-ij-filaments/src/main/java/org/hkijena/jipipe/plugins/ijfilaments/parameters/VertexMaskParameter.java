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

package org.hkijena.jipipe.plugins.ijfilaments.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class VertexMaskParameter extends AbstractJIPipeParameterCollection {

    private OptionalJIPipeExpressionParameter filter = new OptionalJIPipeExpressionParameter(false, "");

    public VertexMaskParameter() {
    }

    public VertexMaskParameter(VertexMaskParameter other) {

        this.filter = new OptionalJIPipeExpressionParameter(other.filter);
    }

    public static Set<FilamentVertex> filter(OptionalJIPipeExpressionParameter filter, Filaments3DGraphData graph, Set<FilamentVertex> vertexSet, JIPipeExpressionVariablesMap variables) {
        if (vertexSet == null) {
            vertexSet = graph.vertexSet();
        }
        if (filter.isEnabled()) {
            Set<FilamentVertex> matched = new HashSet<>();
            for (FilamentVertex vertex : vertexSet) {
                // Write variables
                FilamentVertexVariablesInfo.writeToVariables(graph, vertex, variables, "");
                if (filter.getContent().test(variables)) {
                    matched.add(vertex);
                }
            }

            return matched;
        } else {
            return vertexSet;
        }
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Apply the operation to a specific set of vertices")
    @JIPipeParameter("mask")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public OptionalJIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("mask")
    public void setFilter(OptionalJIPipeExpressionParameter filter) {
        this.filter = filter;
    }
}
