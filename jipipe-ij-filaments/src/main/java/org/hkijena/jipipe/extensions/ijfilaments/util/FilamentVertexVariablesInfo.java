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

package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentVertexVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("uuid", "UUID", "The unique ID of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "X", "The X location"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Y", "The Y location"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Z", "The Z location"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Channel", "The channel (c) location"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Frame", "The frame (t) location"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("radius", "Radius", "The radius of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("value", "Value", "The value of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("degree", "Degree", "The degree (number of edges) of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("vsx", "Physical voxel size (X)", "Size of a voxel (X)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("vsy", "Physical voxel size (Y)", "Size of a voxel (Y)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("vsz", "Physical voxel size (Z)", "Size of a voxel (Z)"));
    }

    public static void writeToVariables(Filaments3DData graph, FilamentVertex vertex, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureVertex(vertex, variables, prefix);
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
