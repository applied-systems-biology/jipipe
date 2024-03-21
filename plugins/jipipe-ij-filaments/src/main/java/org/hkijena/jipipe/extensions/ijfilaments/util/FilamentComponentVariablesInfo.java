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

import java.util.HashSet;
import java.util.Set;

public class FilamentComponentVariablesInfo implements ExpressionParameterVariablesInfo {

    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Component", "Component", "The component ID of the filament"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVertices", "Number of vertices", "The number of vertices"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numEdges", "Number of edges", "The number of edges"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lengthPixels", "Length (pixels)", "The length (in pixels)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lengthUnit", "Length (unit)", "The length (in physical units)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lengthPixelsRadiusCorrected", "Length (pixels, radius-corrected)", "The length (in pixels). Radius is added at the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lengthUnitRadiusCorrected", "Length (unit, radius-corrected)", "The length (in physical units). Radius is added at the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("simplifiedLengthPixels", "Length (simplified graph, pixels)", "The length (in pixels) of a simplified filament that only contains the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("simplifiedLengthUnit", "Length (simplified graph, unit)", "The length (in physical units) of a simplified filament that only contains the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("simplifiedLengthPixelsRadiusCorrected", "Length (simplified graph, pixels, radius-corrected)", "The length (in pixels) of a simplified filament that only contains the end points. Radius is added at the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("simplifiedLengthUnitRadiusCorrected", "Length (simplified graph, unit, radius-corrected)", "The length (in physical units) of a simplified filament that only contains the end points. Radius is added at the end points."
        ));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("confinementRatio", "Confinement ratio", "The confinement ratio"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("confinementRatioRadiusCorrected", "Confinement ratio (radius-corrected)", "The confinement ratio. Radius is added at the end points during the calculation."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree0", "Number of vertices with degree 0", "The number of vertices with degree (number of edges) zero."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree1", "Number of vertices with degree 1", "The number of vertices with degree (number of edges) one."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree2", "Number of vertices with degree 2", "The number of vertices with degree (number of edges) two."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree3", "Number of vertices with degree 3", "The number of vertices with degree (number of edges) three."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree4", "Number of vertices with degree 4", "The number of vertices with degree (number of edges) four."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegree5", "Number of vertices with degree 5", "The number of vertices with degree (number of edges) five."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("numVerticesWithDegreeMoreThan5", "Number of vertices with degree > 5", "The number of vertices with degree (number of edges) more than five."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxDegree", "Max degree", "The maximum degree."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minDegree", "Min degree", "The minimum degree."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMinX", "Min X (center, pixels)", "Minimum center X (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMinY", "Min Y (center, pixels)", "Minimum center Y (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMinZ", "Min Z (center, pixels)", "Minimum center Z (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMaxX", "Max X (center, pixels)", "Maximum center X (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMaxY", "Max Y (center, pixels)", "Maximum center Y (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("centerMaxZ", "Max Z (center, pixels)", "Maximum center Z (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMinX", "Min X (sphere, pixels)", "Minimum X (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMinY", "Min Y (sphere, pixels)", "Minimum Y (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMinZ", "Min Z (sphere, pixels)", "Minimum Z (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMaxX", "Max X (sphere, pixels)", "Maximum X (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMaxY", "Max Y (sphere, pixels)", "Maximum Y (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("sphereMaxZ", "Max Z (sphere, pixels)", "Maximum Z (sphere around the center, in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minEdgeLengthPixels", "Min edge length (pixels)", "Minimum length of an edge (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minEdgeLengthUnit", "Min edge length (unit)", "Minimum length of an edge (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxEdgeLengthPixels", "Max edge length (pixels)", "Maximum length of an edge (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxEdgeLengthUnit", "Max edge length (unit)", "Maximum length of an edge (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("avgEdgeLengthPixels", "Average edge length (pixels)", "Average length of an edge (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("avgEdgeLengthUnit", "Average edge length (unit)", "Average length of an edge (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minVertexRadiusPixels", "Min vertex radius (pixels)", "Minimum vertex radius (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxVertexRadiusPixels", "Max vertex radius (pixels)", "Maximum vertex radius (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("avgVertexRadiusPixels", "Average vertex radius (pixels)", "Maximum vertex radius (in pixels)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minVertexRadiusUnit", "Min vertex radius (unit)", "Minimum vertex radius (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxVertexRadiusUnit", "Max vertex radius (unit)", "Maximum vertex radius (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("avgVertexRadiusUnit", "Average vertex radius (unit)", "Average vertex radius (in physical units)."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("minVertexValue", "Min vertex value", "Minimum vertex value."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("maxVertexValue", "Max vertex value", "Maximum vertex value."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("avgVertexValue", "Average vertex value", "Average vertex value."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("physicalSizeUnit", "Physical size unit", "Unit used in the values that have a physical size"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
