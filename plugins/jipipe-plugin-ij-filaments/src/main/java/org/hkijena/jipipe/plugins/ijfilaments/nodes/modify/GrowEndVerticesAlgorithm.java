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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.Point3d;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Grow end vertices (direction)", description = "Iteratively adds vertices to end points (degree 1) and grows them in the direction determined by the neighbor")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
public class GrowEndVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private int iterations = 1;
    private JIPipeExpressionParameter azimuthExpression = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter attitudeExpression = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter distanceExpression = new JIPipeExpressionParameter("5");

    public GrowEndVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public GrowEndVerticesAlgorithm(GrowEndVerticesAlgorithm other) {
        super(other);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData graph = (Filaments3DData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        for (int i = 0; i < iterations; i++) {
            JIPipeProgressInfo iterationProgress = progressInfo.resolveAndLog("Iteration", i, iterations);
            Set<FilamentVertex> toProcess = new HashSet<>();
            for (FilamentVertex vertex : vertexMask.filter(graph, graph.vertexSet(), variables)) {
                if (graph.degreeOf(vertex) == 1) {
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
                if (source == vertex) {
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

    @SetJIPipeDocumentation(name = "Azimuth", description = "Expression that determines the azimuth (angle in the xy-plane) for the new vertex")
    @JIPipeParameter("azimuth")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node (radians)")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    public JIPipeExpressionParameter getAzimuthExpression() {
        return azimuthExpression;
    }

    @JIPipeParameter("azimuth")
    public void setAzimuthExpression(JIPipeExpressionParameter azimuthExpression) {
        this.azimuthExpression = azimuthExpression;
    }

    @SetJIPipeDocumentation(name = "Attitude", description = "Expression that determines the attitude (angle in z) for the new vertex")
    @JIPipeParameter("attitude")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node (radians)")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    public JIPipeExpressionParameter getAttitudeExpression() {
        return attitudeExpression;
    }

    @JIPipeParameter("attitude")
    public void setAttitudeExpression(JIPipeExpressionParameter attitudeExpression) {
        this.attitudeExpression = attitudeExpression;
    }

    @SetJIPipeDocumentation(name = "Distance", description = "Expression that determines the distance between the origin and new vertex")
    @JIPipeExpressionParameterSettings(hint = "per origin vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Default", key = "default", description = "The default value calculated from the direction of the neighbor node")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeParameter("distance")
    public JIPipeExpressionParameter getDistanceExpression() {
        return distanceExpression;
    }

    @JIPipeParameter("distance")
    public void setDistanceExpression(JIPipeExpressionParameter distanceExpression) {
        this.distanceExpression = distanceExpression;
    }

    @SetJIPipeDocumentation(name = "Iterations", description = "The number of growth iterations")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Allows to limit the calculations to a specific set of vertices.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
