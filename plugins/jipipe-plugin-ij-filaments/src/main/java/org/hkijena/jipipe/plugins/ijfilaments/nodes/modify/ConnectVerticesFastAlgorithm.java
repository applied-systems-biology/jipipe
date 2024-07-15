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

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2dParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.util.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.joml.Vector2i;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Connect filament vertices (Fast)", description = "Connect existing vertices based on distance.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", optional = true, create = true)
public class ConnectVerticesFastAlgorithm extends JIPipeIteratingAlgorithm {
    private final VertexMaskParameter vertexMask;
    private boolean connectAcrossC = false;
    private boolean connectAcrossT = false;
    private boolean enable3D = true;
    private OptionalColorParameter newEdgeColor = new OptionalColorParameter(Color.GREEN, true);
    private OptionalIntegerParameter limitConnections = new OptionalIntegerParameter(false, 1);
    private boolean enforceEdgesWithinMask = true;
    private boolean ignoreIfHasPath = true;
    private Vector2dParameter lengthRange = new Vector2dParameter(0, 10);
    private Vector2iParameter candidateVertexDegreeLimit = new Vector2iParameter(0, 1);
    private Vector2dParameter candidateVertexValueLimit = new Vector2dParameter(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    private SortOrder limitConnectionsLengthSortOrder = SortOrder.Ascending;

    public ConnectVerticesFastAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ConnectVerticesFastAlgorithm(ConnectVerticesFastAlgorithm other) {
        super(other);
        this.connectAcrossC = other.connectAcrossC;
        this.connectAcrossT = other.connectAcrossT;
        this.enable3D = other.enable3D;
        this.ignoreIfHasPath = other.ignoreIfHasPath;
        this.newEdgeColor = new OptionalColorParameter(other.newEdgeColor);
        this.enforceEdgesWithinMask = other.enforceEdgesWithinMask;
        this.limitConnections = new OptionalIntegerParameter(other.limitConnections);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        this.lengthRange = new Vector2dParameter(other.lengthRange);
        this.candidateVertexDegreeLimit = new Vector2iParameter(other.candidateVertexDegreeLimit);
        this.candidateVertexValueLimit = new Vector2dParameter(other.candidateVertexValueLimit);
        this.limitConnectionsLengthSortOrder = other.limitConnectionsLengthSortOrder;
        registerSubParameter(vertexMask);
    }

    @SetJIPipeDocumentation(name = "Candidate vertex value limit", description = "Limits candidate vertices by their value")
    @JIPipeParameter("candidate-vertex-value-limit")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2dParameter getCandidateVertexValueLimit() {
        return candidateVertexValueLimit;
    }

    @JIPipeParameter("candidate-vertex-value-limit")
    public void setCandidateVertexValueLimit(Vector2dParameter candidateVertexValueLimit) {
        this.candidateVertexValueLimit = candidateVertexValueLimit;
    }

    @SetJIPipeDocumentation(name = "Candidate vertex degree limit", description = "Limits candidate vertices by their degree")
    @JIPipeParameter("candidate-vertex-degree-limit")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2iParameter getCandidateVertexDegreeLimit() {
        return candidateVertexDegreeLimit;
    }

    @JIPipeParameter("candidate-vertex-degree-limit")
    public void setCandidateVertexDegreeLimit(Vector2iParameter candidateVertexDegreeLimit) {
        this.candidateVertexDegreeLimit = candidateVertexDegreeLimit;
    }

    @SetJIPipeDocumentation(name = "Edge sort order (length)", description = "The sort order of edges by their length if 'Limit created edges' is active.")
    @JIPipeParameter("limit-connections-length-sort-order")
    public SortOrder getLimitConnectionsLengthSortOrder() {
        return limitConnectionsLengthSortOrder;
    }

    @JIPipeParameter("limit-connections-length-sort-order")
    public void setLimitConnectionsLengthSortOrder(SortOrder limitConnectionsLengthSortOrder) {
        this.limitConnectionsLengthSortOrder = limitConnectionsLengthSortOrder;
    }

    @SetJIPipeDocumentation(name = "Limit created edges", description = "If enabled, limit the number of created edges per vertex. Edges are sorted by their length.")
    @JIPipeParameter("limit-connections")
    public OptionalIntegerParameter getLimitConnections() {
        return limitConnections;
    }

    @JIPipeParameter("limit-connections")
    public void setLimitConnections(OptionalIntegerParameter limitConnections) {
        this.limitConnections = limitConnections;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Ignore if path exists", description = "If enabled, do not consider edges between vertices that already have a path. Impacts performance.")
    @JIPipeParameter("ignore-if-has-path")
    public boolean isIgnoreIfHasPath() {
        return ignoreIfHasPath;
    }

    @JIPipeParameter("ignore-if-has-path")
    public void setIgnoreIfHasPath(boolean ignoreIfHasPath) {
        this.ignoreIfHasPath = ignoreIfHasPath;
    }

    @SetJIPipeDocumentation(name = "Edge length", description = "Determines the minimum and maximum edge length")
    @JIPipeParameter("edge-length")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2dParameter getLengthRange() {
        return lengthRange;
    }

    @JIPipeParameter("edge-length")
    public void setLengthRange(Vector2dParameter lengthRange) {
        this.lengthRange = lengthRange;
    }

    @SetJIPipeDocumentation(name = "Prevent edges outside mask", description = "If enabled and a mask is available, check if edges crosses outside the mask boundaries and exclude those from being marked as candidate edge")
    @JIPipeParameter("enforce-edges-within-mask")
    public boolean isEnforceEdgesWithinMask() {
        return enforceEdgesWithinMask;
    }

    @JIPipeParameter("enforce-edges-within-mask")
    public void setEnforceEdgesWithinMask(boolean enforceEdgesWithinMask) {
        this.enforceEdgesWithinMask = enforceEdgesWithinMask;
    }

    @SetJIPipeDocumentation(name = "Color new edges", description = "Allows to color newly made edges")
    @JIPipeParameter("new-edge-color")
    public OptionalColorParameter getNewEdgeColor() {
        return newEdgeColor;
    }

    @JIPipeParameter("new-edge-color")
    public void setNewEdgeColor(OptionalColorParameter newEdgeColor) {
        this.newEdgeColor = newEdgeColor;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Additional filter applied to the vertices prior to candidate edge selection.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @SetJIPipeDocumentation(name = "Connect across channels", description = "If enabled, the algorithm considers also endpoints between different channel planes.")
    @JIPipeParameter("connect-across-c")
    public boolean isConnectAcrossC() {
        return connectAcrossC;
    }

    @JIPipeParameter("connect-across-c")
    public void setConnectAcrossC(boolean connectAcrossC) {
        this.connectAcrossC = connectAcrossC;
    }

    @SetJIPipeDocumentation(name = "Connect across frames", description = "If enabled, the algorithm considers also endpoints between different frame planes.")
    @JIPipeParameter("connect-across-t")
    public boolean isConnectAcrossT() {
        return connectAcrossT;
    }

    @JIPipeParameter("connect-across-t")
    public void setConnectAcrossT(boolean connectAcrossT) {
        this.connectAcrossT = connectAcrossT;
    }

    @SetJIPipeDocumentation(name = "Connect in 3D", description = "If enabled, the algorithm will look for endpoints in other Z planes.")
    @JIPipeParameter("enable-3d")
    public boolean isEnable3D() {
        return enable3D;
    }

    @JIPipeParameter("enable-3d")
    public void setEnable3D(boolean enable3D) {
        this.enable3D = enable3D;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData("Input", Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        ImagePlus mask;
        if (enforceEdgesWithinMask) {
            ImagePlusGreyscaleMaskData maskData = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
            if (maskData != null) {
                mask = maskData.getImage();
            } else {
                mask = null;
            }
        } else {
            mask = null;
        }

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);


        HashSet<EdgeCandidate> candidates = new HashSet<>();
        DijkstraShortestPath<FilamentVertex, FilamentEdge> outputDataInspector = ignoreIfHasPath ? new DijkstraShortestPath<>(outputData) : null;
        for (FilamentVertex current : outputData.vertexSet()) {

            int currentDegree = outputData.degreeOf(current);
            if(currentDegree < candidateVertexDegreeLimit.getX() || currentDegree > candidateVertexDegreeLimit.getY()) {
                continue;
            }
            double currentValue = current.getValue();
            if(currentValue < candidateVertexValueLimit.getX() || currentValue > candidateVertexValueLimit.getY()) {
                continue;
            }

            Vector3d currentV1 = current.getSpatialLocation().toSciJavaVector3d();

            outer:
            for (FilamentVertex other : outputData.vertexSet()) {
                if (other != current) {
                    if (!enable3D && current.getSpatialLocation().getZ() != other.getSpatialLocation().getZ()) {
                        continue;
                    }
                    if (!connectAcrossC && current.getNonSpatialLocation().getChannel() != other.getNonSpatialLocation().getChannel()) {
                        continue;
                    }
                    if (!connectAcrossT && current.getNonSpatialLocation().getFrame() != other.getNonSpatialLocation().getFrame()) {
                        continue;
                    }
                    if(outputData.containsEdge(current, other)) {
                        continue;
                    }
                    int otherDegree = outputData.degreeOf(other);
                    if(otherDegree < candidateVertexDegreeLimit.getX() || otherDegree > candidateVertexDegreeLimit.getY()) {
                        continue;
                    }
                    double otherValue = other.getValue();
                    if(otherValue < candidateVertexValueLimit.getX() || otherValue > candidateVertexValueLimit.getY()) {
                        continue;
                    }
                    if (progressInfo.isCancelled()) {
                        return;
                    }

                    if (mask != null) {
                        // Just test at vertex location
                        Vector3d testLocation = new Vector3d(currentV1.x,
                                currentV1.y,
                                currentV1.z);
                        int x = Math.max(0, Math.min(mask.getWidth() - 1, (int) Math.round(testLocation.x)));
                        int y = (int) Math.round(testLocation.y);
                        int z = Math.max(0, (int) Math.round(testLocation.z));

                        ImageProcessor ip = ImageJUtils.getSliceZero(mask, 0, z, 0);
                        if (ip.get(x, y) == 0) {
                            continue;
                        }
                    }

                    if (progressInfo.isCancelled()) {
                        return;
                    }

                    // Calculate other direction
                    Vector3d otherV1 = other.getSpatialLocation().toSciJavaVector3d();

                    if (!enable3D) {
                        otherV1.z = 0;
                    }

                    // Check length limits
                    double distance = current.getSpatialLocation().distanceTo(other.getSpatialLocation());
                    if(distance < this.lengthRange.getX() || distance > this.lengthRange.getY()) {
                        continue;
                    }

                    // Check path exists
                    if(ignoreIfHasPath && outputDataInspector != null) {
                        GraphPath<FilamentVertex, FilamentEdge> path = outputDataInspector.getPath(current, other);
                        if(path != null) {
                            continue;
                        }
                    }

                    // Add as candidate
                    double score = this.limitConnectionsLengthSortOrder == SortOrder.Ascending ? distance : -distance;
                    EdgeCandidate candidate = new EdgeCandidate(current, other, score);
                    candidates.add(candidate);

                }
            }
        }

        // Apply connect
        int successes = 0;
        Map<FilamentVertex, Integer> connectionCount = new HashMap<>();
        for (EdgeCandidate candidate : candidates.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {

            if (progressInfo.isCancelled()) {
                return;
            }

            if (limitConnections.isEnabled()) {
                int c1 = connectionCount.getOrDefault(candidate.source, 0);
                int c2 = connectionCount.getOrDefault(candidate.target, 0);
                if (c1 < limitConnections.getContent() && c2 < limitConnections.getContent()) {
                    connectionCount.put(candidate.source, c1 + 1);
                    connectionCount.put(candidate.target, c2 + 1);
                } else {
                    continue;
                }
            }

            // Connect
            FilamentEdge edge = outputData.addEdge(candidate.source, candidate.target);
            if (edge != null) {
                if (newEdgeColor.isEnabled())
                    edge.setColor(newEdgeColor.getContent());
            }
            ++successes;
        }

        progressInfo.log("Successfully created " + successes + " edges.");

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    public static class EdgeCandidate implements Comparable<EdgeCandidate> {
        private final FilamentVertex source;
        private final FilamentVertex target;
        private final double score;

        public EdgeCandidate(FilamentVertex source, FilamentVertex target, double score) {
            this.source = source;
            this.target = target;
            this.score = score;
        }

        public FilamentVertex getSource() {
            return source;
        }

        public FilamentVertex getTarget() {
            return target;
        }

        public double getScore() {
            return score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeCandidate that = (EdgeCandidate) o;
            return Double.compare(score, that.score) == 0 && Objects.equals(source, that.source) && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target, score);
        }

        @Override
        public int compareTo(@NotNull EdgeCandidate o) {
            return Double.compare(score, o.score);
        }
    }

}
