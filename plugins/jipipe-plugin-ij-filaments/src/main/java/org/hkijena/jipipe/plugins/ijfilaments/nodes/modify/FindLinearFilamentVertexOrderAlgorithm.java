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
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEndpointsVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.scijava.vecmath.Vector3d;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Find linear filament vertex order/distance", description = "For all vertex components that are linear (all vertices have a degree less than 3), find the order each vertex in that line. " +
        "Also can calculate the distance (in pixels) traveled from the start to the end point." +
        "The result is stored in each vertex' metadata field.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class FindLinearFilamentVertexOrderAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter startPointSelectorFunction = new JIPipeExpressionParameter("IF_ELSE(first.x < second.x, \"first\", \"second\")");
    private OptionalStringParameter orderMetadataKey = new OptionalStringParameter("line_order", true);
    private OptionalStringParameter distanceMetadataKey = new OptionalStringParameter("line_distance", true);
    private OptionalStringParameter maxDistanceMetadataKey = new OptionalStringParameter("line_length", true);

    public FindLinearFilamentVertexOrderAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FindLinearFilamentVertexOrderAlgorithm(FindLinearFilamentVertexOrderAlgorithm other) {
        super(other);
        this.startPointSelectorFunction = new JIPipeExpressionParameter(other.startPointSelectorFunction);
        this.orderMetadataKey = new OptionalStringParameter(other.orderMetadataKey);
        this.distanceMetadataKey = new OptionalStringParameter(other.distanceMetadataKey);
        this.maxDistanceMetadataKey = new OptionalStringParameter(other.maxDistanceMetadataKey);
    }

    @SetJIPipeDocumentation(name = "Set order metadata", description = "If enabled, sets the order (1 at the starting point) of the vertex within the line")
    @JIPipeParameter("order-metadata-key")
    public OptionalStringParameter getOrderMetadataKey() {
        return orderMetadataKey;
    }

    @JIPipeParameter("order-metadata-key")
    public void setOrderMetadataKey(OptionalStringParameter orderMetadataKey) {
        this.orderMetadataKey = orderMetadataKey;
    }

    @SetJIPipeDocumentation(name = "Set distance from start metadata", description = "If enabled, sets the traveled distance from the start point (in pixels)")
    @JIPipeParameter("distance-metadata-key")
    public OptionalStringParameter getDistanceMetadataKey() {
        return distanceMetadataKey;
    }

    @JIPipeParameter("distance-metadata-key")
    public void setDistanceMetadataKey(OptionalStringParameter distanceMetadataKey) {
        this.distanceMetadataKey = distanceMetadataKey;
    }

    @SetJIPipeDocumentation(name = "Set line length metadata", description = "If enabled, sets the full line length (in pixels)")
    @JIPipeParameter("max-distance-metadata-key")
    public OptionalStringParameter getMaxDistanceMetadataKey() {
        return maxDistanceMetadataKey;
    }

    @JIPipeParameter("max-distance-metadata-key")
    public void setMaxDistanceMetadataKey(OptionalStringParameter maxDistanceMetadataKey) {
        this.maxDistanceMetadataKey = maxDistanceMetadataKey;
    }

    @SetJIPipeDocumentation(name = "Start point selector", description = "Applied per linear component. Given are the two end points (first and second). The function should return \"first\" or \"second\" depending on which of the end points should be ")
    @JIPipeParameter("selector-function")
    @JIPipeExpressionParameterSettings(hint = "per endpoint pair")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentEndpointsVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "first.direction", name = "Source direction", description = "Vector that contains the direction of the first vertex")
    @AddJIPipeExpressionParameterVariable(key = "second.direction", name = "Target direction", description = "Vector that contains the direction of the second vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @AddJIPipeExpressionParameterVariable(key = "path_length", name = "Path length", description = "Existing path length in number of edges between the vertices (NaN if there is no path)")
    @AddJIPipeExpressionParameterVariable(key = "dot_product", name = "Vertices direction dot product", description = "The dot product of source and target directions. " +
            "-1 if the directions are opposite and 1 if they point at the same direction (NaN if not available)")
    @AddJIPipeExpressionParameterVariable(key = "angle", name = "Angle", description = "The angle between the source and target directions (NaN if not available).")
    public JIPipeExpressionParameter getStartPointSelectorFunction() {
        return startPointSelectorFunction;
    }

    @JIPipeParameter("selector-function")
    public void setStartPointSelectorFunction(JIPipeExpressionParameter startPointSelectorFunction) {
        this.startPointSelectorFunction = startPointSelectorFunction;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData filaments = (Filaments3DGraphData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo).duplicate(progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = new ConnectivityInspector<>(filaments);
        DijkstraShortestPath<FilamentVertex, FilamentEdge> shortestPath = new DijkstraShortestPath<>(filaments);

        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            if (connectedSet.stream().anyMatch(vertex -> filaments.degreeOf(vertex) > 2)) {
                continue;
            }
            if (connectedSet.size() == 1) {
                // trivial solution
                for (FilamentVertex vertex : connectedSet) {
                    if (distanceMetadataKey.isEnabled()) {
                        vertex.setMetadata(distanceMetadataKey.getContent(), 0);
                    }
                    if (maxDistanceMetadataKey.isEnabled()) {
                        vertex.setMetadata(maxDistanceMetadataKey.getContent(), 0);
                    }
                    if (orderMetadataKey.isEnabled()) {
                        vertex.setMetadata(orderMetadataKey.getContent(), 0);
                    }
                }
            } else if (connectedSet.size() >= 2) {
                List<FilamentVertex> endPoints = connectedSet.stream().filter(vertex -> filaments.degreeOf(vertex) == 1).collect(Collectors.toList());

                if (endPoints.size() != 2) {
                    // Not a line
                    continue;
                }

                FilamentVertex first = endPoints.get(0);
                FilamentVertex second = endPoints.get(1);
                Vector3d firstDirection = findDirection(first, filaments);
                Vector3d secondDirection = findDirection(second, filaments);

                FilamentEndpointsVariablesInfo.writeToVariables(filaments, first, second, variables, "");

                GraphPath<FilamentVertex, FilamentEdge> path = shortestPath.getPath(first, second);
                variables.set("angle", Math.toDegrees(firstDirection.angle(secondDirection)));
                variables.set("dot_product", firstDirection.dot(secondDirection));
                variables.set("path_length", path.getLength());

                Object candidate = startPointSelectorFunction.evaluate(variables);
                FilamentVertex start;
                FilamentVertex end;
                if (candidate == Boolean.TRUE) {
                    start = first;
                    end = second;
                } else if (candidate.equals("first")) {
                    start = first;
                    end = second;
                } else if (candidate.equals("second")) {
                    start = second;
                    end = first;
                } else {
                    throw new IllegalArgumentException("Unknown selector response: " + candidate);
                }

                path = shortestPath.getPath(start, end);
                List<FilamentVertex> vertexList = path.getVertexList();
                double maxDistancePixels = 0;
                double currentDistancePixels = 0;
                for (int i = 1; i < vertexList.size(); i++) {
                    FilamentVertex last = vertexList.get(i - 1);
                    FilamentVertex current = vertexList.get(i);
                    maxDistancePixels += current.getSpatialLocation().distanceTo(last.getSpatialLocation());
                }
                for (int i = 0; i < vertexList.size(); i++) {
                    FilamentVertex vertex = vertexList.get(i);
                    if (i > 0) {
                        currentDistancePixels += vertex.getSpatialLocation().distanceTo(vertexList.get(i - 1).getSpatialLocation());
                    }
                    if (distanceMetadataKey.isEnabled()) {
                        vertex.setMetadata(distanceMetadataKey.getContent(), currentDistancePixels);
                    }
                    if (maxDistanceMetadataKey.isEnabled()) {
                        vertex.setMetadata(maxDistanceMetadataKey.getContent(), maxDistancePixels);
                    }
                    if (orderMetadataKey.isEnabled()) {
                        vertex.setMetadata(orderMetadataKey.getContent(), i + 1);
                    }
                }

            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }

    private Vector3d findDirection(FilamentVertex here, Filaments3DGraphData filaments) {
        Set<FilamentVertex> neighbors = Graphs.neighborSetOf(filaments, here);
        Vector3d currentV1 = here.getSpatialLocation().toSciJavaVector3d();
        Vector3d currentV2 = neighbors.iterator().next().getSpatialLocation().toSciJavaVector3d();

        Vector3d currentDirection = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
        currentDirection.normalize();
        return currentDirection;
    }
}
