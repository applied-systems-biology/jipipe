package org.hkijena.jipipe.extensions.ijfilaments.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.util.HashSet;
import java.util.Set;

public class FilamentComponentVariableSource implements ExpressionParameterVariableSource {

    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Component", "The component ID of the filament", "Component"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices", "The number of vertices", "numVertices"));
        VARIABLES.add(new ExpressionParameterVariable("Number of edges", "The number of edges", "numEdges"));
        VARIABLES.add(new ExpressionParameterVariable("Length (pixels)", "The length (in pixels)", "lengthPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Length (unit)", "The length (in physical units)", "lengthUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Length (pixels, radius-corrected)", "The length (in pixels). Radius is added at the end points.",
                "lengthPixelsRadiusCorrected"));
        VARIABLES.add(new ExpressionParameterVariable("Length (unit, radius-corrected)", "The length (in physical units). Radius is added at the end points.",
                "lengthUnitRadiusCorrected"));
        VARIABLES.add(new ExpressionParameterVariable("Length (simplified graph, pixels)", "The length (in pixels) of a simplified filament that only contains the end points.",
                "simplifiedLengthPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Length (simplified graph, unit)", "The length (in physical units) of a simplified filament that only contains the end points.",
                "simplifiedLengthUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Length (simplified graph, pixels, radius-corrected)", "The length (in pixels) of a simplified filament that only contains the end points. Radius is added at the end points.",
                "simplifiedLengthPixelsRadiusCorrected"));
        VARIABLES.add(new ExpressionParameterVariable("Length (simplified graph, unit, radius-corrected)", "The length (in physical units) of a simplified filament that only contains the end points. Radius is added at the end points.",
                "simplifiedLengthUnitRadiusCorrected"));
        VARIABLES.add(new ExpressionParameterVariable("Confinement ratio", "The confinement ratio", "confinementRatio"));
        VARIABLES.add(new ExpressionParameterVariable("Confinement ratio (radius-corrected)", "The confinement ratio. Radius is added at the end points during the calculation.", "confinementRatioRadiusCorrected"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 0", "The number of vertices with degree (number of edges) zero.", "numVerticesWithDegree0"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 1", "The number of vertices with degree (number of edges) one.", "numVerticesWithDegree1"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 2", "The number of vertices with degree (number of edges) two.", "numVerticesWithDegree2"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 3", "The number of vertices with degree (number of edges) three.", "numVerticesWithDegree3"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 4", "The number of vertices with degree (number of edges) four.", "numVerticesWithDegree4"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree 5", "The number of vertices with degree (number of edges) five.", "numVerticesWithDegree5"));
        VARIABLES.add(new ExpressionParameterVariable("Number of vertices with degree > 5", "The number of vertices with degree (number of edges) more than five.", "numVerticesWithDegreeMoreThan5"));
        VARIABLES.add(new ExpressionParameterVariable("Min X (center, pixels)", "Minimum center X (in pixels).", "centerMinX"));
        VARIABLES.add(new ExpressionParameterVariable("Min Y (center, pixels)", "Minimum center Y (in pixels).", "centerMinY"));
        VARIABLES.add(new ExpressionParameterVariable("Min Z (center, pixels)", "Minimum center Z (in pixels).", "centerMinZ"));
        VARIABLES.add(new ExpressionParameterVariable("Max X (center, pixels)", "Maximum center X (in pixels).", "centerMaxX"));
        VARIABLES.add(new ExpressionParameterVariable("Max Y (center, pixels)", "Maximum center Y (in pixels).", "centerMaxY"));
        VARIABLES.add(new ExpressionParameterVariable("Max Z (center, pixels)", "Maximum center Z (in pixels).", "centerMaxZ"));
        VARIABLES.add(new ExpressionParameterVariable("Min X (sphere, pixels)", "Minimum X (sphere around the center, in pixels).", "sphereMinX"));
        VARIABLES.add(new ExpressionParameterVariable("Min Y (sphere, pixels)", "Minimum Y (sphere around the center, in pixels).", "sphereMinY"));
        VARIABLES.add(new ExpressionParameterVariable("Min Z (sphere, pixels)", "Minimum Z (sphere around the center, in pixels).", "sphereMinZ"));
        VARIABLES.add(new ExpressionParameterVariable("Max X (sphere, pixels)", "Maximum X (sphere around the center, in pixels).", "sphereMaxX"));
        VARIABLES.add(new ExpressionParameterVariable("Max Y (sphere, pixels)", "Maximum Y (sphere around the center, in pixels).", "sphereMaxY"));
        VARIABLES.add(new ExpressionParameterVariable("Max Z (sphere, pixels)", "Maximum Z (sphere around the center, in pixels).", "sphereMaxZ"));
        VARIABLES.add(new ExpressionParameterVariable("Min edge length (pixels)", "Minimum length of an edge (in pixels).", "minEdgeLengthPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Min edge length (unit)", "Minimum length of an edge (in physical units).", "minEdgeLengthUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Max edge length (pixels)", "Maximum length of an edge (in pixels).", "maxEdgeLengthPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Max edge length (unit)", "Maximum length of an edge (in physical units).", "maxEdgeLengthUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Average edge length (pixels)", "Average length of an edge (in pixels).", "avgEdgeLengthPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Average edge length (unit)", "Average length of an edge (in physical units).", "avgEdgeLengthUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Min vertex radius (pixels)", "Minimum vertex radius (in pixels).", "minVertexRadiusPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Max vertex radius (pixels)", "Maximum vertex radius (in pixels).", "maxVertexRadiusPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Average vertex radius (pixels)", "Maximum vertex radius (in pixels).", "avgVertexRadiusPixels"));
        VARIABLES.add(new ExpressionParameterVariable("Min vertex radius (unit)", "Minimum vertex radius (in physical units).", "minVertexRadiusUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Max vertex radius (unit)", "Maximum vertex radius (in physical units).", "maxVertexRadiusUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Average vertex radius (unit)", "Average vertex radius (in physical units).", "avgVertexRadiusUnit"));
        VARIABLES.add(new ExpressionParameterVariable("Min vertex value", "Minimum vertex value.", "minVertexValue"));
        VARIABLES.add(new ExpressionParameterVariable("Max vertex value", "Maximum vertex value.", "maxVertexValue"));
        VARIABLES.add(new ExpressionParameterVariable("Average vertex value", "Average vertex value.", "avgVertexValue"));
        VARIABLES.add(new ExpressionParameterVariable("Physical size unit", "Unit used in the values that have a physical size", "physicalSizeUnit"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
