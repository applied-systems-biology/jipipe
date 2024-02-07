package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.util.Point3d;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.scijava.vecmath.Vector3d;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Grow end vertices (direction)", description = "Iteratively adds vertices to end points (degree 1) and grows them in the direction determined by the neighbor")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class GrowEndVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int iterations = 1;
    private final VertexMaskParameter vertexMask;
    private final CustomExpressionVariablesParameter customExpressionVariables;
    private JIPipeExpressionParameter azimuthExpression = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter attitudeExpression = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter distanceExpression = new JIPipeExpressionParameter("5");

    public GrowEndVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public GrowEndVerticesAlgorithm(GrowEndVerticesAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.iterations = other.iterations;
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        this.attitudeExpression = new JIPipeExpressionParameter(other.attitudeExpression);
        this.azimuthExpression = new JIPipeExpressionParameter(other.azimuthExpression);
        this.distanceExpression = new JIPipeExpressionParameter(other.distanceExpression);
        registerSubParameter(vertexMask);
    }

    private double[] cartesianToSphericalCoordinates(Vector3D point) {
        double rho = point.getNorm();
        double phi = Math.acos(point.getZ() / rho);  // inclination (attitude)
        double theta = Math.atan2(point.getY(), point.getX());  // azimuth
        return new double[]{rho, theta, phi};
    }

    private Vector3D sphericalToCartesianCoordinates(double[] sphericalCoords) {
        double rho = sphericalCoords[0];
        double theta = sphericalCoords[1];
        double phi = sphericalCoords[2];

        double x = rho * Math.sin(phi) * Math.cos(theta);
        double y = rho * Math.sin(phi) * Math.sin(theta);
        double z = rho * Math.cos(phi);

        return new Vector3D(x, y, z);
    }

    private Vector3D predictNewPoint(Vector3D A, Vector3D B, double d, double azimuthOffset, double attitudeOffset) {
        Vector3D AB = B.subtract(A).normalize();
        Vector3D C = B.add(AB.scalarMultiply(d));

        // Calculate spherical coordinates of C relative to B
        double[] sphericalCoordsC = cartesianToSphericalCoordinates(C.subtract(B));

        // Modify azimuth and attitude based on offsets
        double azimuth = sphericalCoordsC[1] + azimuthOffset;
        double attitude = sphericalCoordsC[2] + attitudeOffset;

        // Convert back to Cartesian coordinates

        return sphericalToCartesianCoordinates(new double[]{sphericalCoordsC[0], azimuth, attitude}).add(B);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData graph = (Filaments3DData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo).duplicate(progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        for (int i = 0; i < iterations; i++) {
            JIPipeProgressInfo iterationProgress = progressInfo.resolveAndLog("Iteration", i, iterations);
            Set<FilamentVertex> toProcess = new HashSet<>();
            for (FilamentVertex vertex : vertexMask.filter(graph, graph.vertexSet(), variables)) {
                if(graph.degreeOf(vertex) == 1) {
                   toProcess.add(vertex);
                }
            }

            for (FilamentVertex vertex : toProcess) {
                // Calculate distance
                FilamentVertexVariablesInfo.writeToVariables(graph, vertex, variables, "");
                double distance = distanceExpression.evaluateToDouble(variables);

                // Find source and target
                FilamentVertex source;
                FilamentVertex target;
                FilamentEdge edge = graph.edgesOf(vertex).iterator().next();
                source = graph.getEdgeSource(edge);
                target = graph.getEdgeTarget(edge);
                if(source == vertex) {
                    source = target;
                    target = vertex;
                }

                // Calculate new position
                Vector3D a = source.getSpatialLocation().toApacheVector3d();
                Vector3D b = target.getSpatialLocation().toApacheVector3d();
                Vector3D ab = b.subtract(a).normalize();
                Vector3D c = b.add(ab.scalarMultiply(distance));

                // Calculate spherical coordinates of C relative to B
                double[] sphericalCoordsC = cartesianToSphericalCoordinates(c.subtract(b));

                // Modify azimuth and attitude
                variables.set("default", sphericalCoordsC[1]);
                double azimuth = azimuthExpression.evaluateToDouble(variables);

                variables.set("default", sphericalCoordsC[2]);
                double attitude = attitudeExpression.evaluateToDouble(variables);

                // Create new coordinate
                c = sphericalToCartesianCoordinates(new double[]{sphericalCoordsC[0], azimuth, attitude}).add(b);

                // Insert new vertex at c
                FilamentVertex newVertex = new FilamentVertex(vertex);
                newVertex.setSpatialLocation(new Point3d(c.getX(), c.getY(), c.getZ()));
                graph.addVertex(newVertex);
                graph.addEdge(vertex, newVertex);

            }

            iterationProgress.log("Processed " + toProcess.size() + " vertices");
        }

        iterationStep.addOutputData(getFirstOutputSlot(), graph, progressInfo);
    }

    @JIPipeDocumentation(name = "Azimuth", description = "Expression that determines the azimuth (angle in the xy-plane) for the new vertex")
    @JIPipeParameter("azimuth")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node (radians)")
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    public JIPipeExpressionParameter getAzimuthExpression() {
        return azimuthExpression;
    }

    @JIPipeParameter("azimuth")
    public void setAzimuthExpression(JIPipeExpressionParameter azimuthExpression) {
        this.azimuthExpression = azimuthExpression;
    }

    @JIPipeDocumentation(name = "Attitude", description = "Expression that determines the attitude (angle in z) for the new vertex")
    @JIPipeParameter("attitude")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node (radians)")
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    public JIPipeExpressionParameter getAttitudeExpression() {
        return attitudeExpression;
    }

    @JIPipeParameter("attitude")
    public void setAttitudeExpression(JIPipeExpressionParameter attitudeExpression) {
        this.attitudeExpression = attitudeExpression;
    }

    @JIPipeDocumentation(name = "Distance", description = "Expression that determines the distance between the origin and new vertex")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node")
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeParameter("distance")
    public JIPipeExpressionParameter getDistanceExpression() {
        return distanceExpression;
    }

    @JIPipeParameter("distance")
    public void setDistanceExpression(JIPipeExpressionParameter distanceExpression) {
        this.distanceExpression = distanceExpression;
    }

    @JIPipeDocumentation(name = "Iterations", description = "The number of growth iterations")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @JIPipeDocumentation(name = "Vertex mask", description = "Additional mask applied to the vertices. If the vertex mask returns FALSE, the vertex is not eroded.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. " +
            "Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-expression-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
