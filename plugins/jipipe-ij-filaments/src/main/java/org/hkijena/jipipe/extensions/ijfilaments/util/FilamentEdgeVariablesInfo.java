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

public class FilamentEdgeVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("uuid", "UUID", "The unique ID of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("length", "Length (pixels)", "The length of the edge in pixels"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ulength", "Length (unit)", "The length of the edge in physical size (if available)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("unit", "Unit", "The unit of the length"));

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.uuid", "Source UUID", "The unique ID of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.x", "Source centroid X", "The X location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.y", "Source centroid Y", "The Y location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.z", "Source centroid Z", "The Z location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.c", "Source centroid channel", "The channel (c) location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.t", "Source centroid frame", "The frame (t) location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.thickness", "Source thickness", "The thickness around the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.degree", "Source degree", "The degree (number of edges) of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.vsx", "Source physical voxel size X", "The size of 1 voxel (X)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.vsy", "Source physical voxel size Y", "The size of 1 voxel (Y)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("source.vsz", "Source physical voxel size Z", "The size of 1 voxel (Z)"));

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.uuid", "Target UUID", "The unique ID of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.x", "Target centroid X", "The X location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.y", "Target centroid Y", "The Y location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.z", "Target centroid Z", "The Z location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.c", "Target centroid channel", "The channel (c) location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.t", "Target centroid frame", "The frame (t) location of the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.thickness", "Target thickness", "The thickness around the centroid"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.degree", "Target degree", "The degree (number of edges) of the vertex"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.vsx", "Target physical voxel size X", "The size of 1 voxel (X)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.vsy", "Target physical voxel size Y", "The size of 1 voxel (Y)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("target.vsz", "Target physical voxel size Z", "The size of 1 voxel (Z)"));
    }

    public static void writeToVariables(Filaments3DData graph, FilamentEdge edge, JIPipeExpressionVariablesMap variables, String prefix) {
        graph.measureEdge(edge, variables, prefix, graph.getConsensusPhysicalSizeUnit());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
