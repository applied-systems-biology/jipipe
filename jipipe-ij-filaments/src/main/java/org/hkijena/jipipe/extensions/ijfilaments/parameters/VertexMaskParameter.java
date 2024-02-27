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

package org.hkijena.jipipe.extensions.ijfilaments.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
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

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Apply the operation to a specific set of vertices")
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
